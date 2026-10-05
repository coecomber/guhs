"""
Het Bleekwoud (1.2.8): the mod's own take on the Pale Garden, in the Guhmension. A rare, silent forest where all the pink
has drained away (Java: nl.juiced.guhs.feature.bleekwoud).

  - the biome guhs:bleekwoud: a region of its own in the Guhmension (the low noise guhs:bleekwoud moves the multi_noise
    temperature; about 1% of the surface in patches of a few hectares, measured by the game test bleekwoudAandeel), no
    music, dim grey sky and fog, pale grass and leaves, NO spawns at all
  - the wood set bleekhout_* (stam, gestript, gezicht, planken, trap, plaat, hek, poort, deur, luik, bord + wandbord,
    bladeren, zaailing), bleekmos (block, tapijt, bleek_hangmos)
  - krakend_guhhartje / verzuurd_guhhartje (the hearts), kaashars (clump), kaashars_blok, harssteen (item), harsstenen
    (+ trap, plaat, muur, gebeitelde_harsstenen), oogbloempje / open_oogbloempje (+ in a pot)
  - the Kraakguh and the Kraak-Mika (models, textures, animations: bleekwoud_tex.py), their sounds (vanilla's creaking
    and eyeblossom sounds under our own events and subtitles)
  - trees (bleekhout_boom, bleekhout_boom_hart with a heart, bleekhout_boompje), moss patches, oogbloempjes
  - the structures bleke_open_plek and houthakkershutje (bleekwoud_bouw.py), their loot, the woodcutter's diary
  - advancements (tab guhmension), Guhdex pages, FTB quests (chapter guhs_extra27), lang (Dutch; English: tools/lang/en)
"""
import json
import os
import zipfile

from features import bleekwoud_bouw as bouw
from features import bleekwoud_tex as tex

BIOME = "bleekwoud"
# The biome is a REGION of its own, like the Sneeuwguhtoendra (features/verhaal_wereld.py): the Guhmension's temperature and
# humidity noises are small (64 blocks), so a rare point in them only gives specks of a few blocks. The low noise
# guhs:bleekwoud (512 blocks) gives real patches of forest: where it is above TERM (and away from the deep seas, the
# Knuffeldal, the Guhpolder, the tundra and Guhwai'i) the multi-noise TEMPERATURE drops and the HUMIDITY rises by SHIFT, to
# the Bleekwoud's own entry in the coldest, wettest corner (TEMP, HUMID; OFFSET keeps it out of the rest of the world, where
# the two small noises never get that far together). No other region moves those two, so nothing crosses it.
# The terrain stays what it is. Below y 40-48 the term fades out, so the cave biomes under the forest stay.
# TERM is tuned with the game test BleekwoudGameTests.bleekwoudAandeel (about 1% of the surface).
NOISE = "bleekwoud"
NOISE_OCTAVE = -9
TERM = (0.650, 0.656)
SHIFT = 12.0
TEMP, HUMID, OFFSET = -2.0, 2.0, 1.0
GUHWAII_OFF = (0.34, 0.40)          # never next to a Guhwai'i (its region starts at guhwaii noise 0.42)
KELDER_Y = (40, 48)

WOOD = {"bleekhout_trap": "oak_stairs", "bleekhout_plaat": "oak_slab", "bleekhout_hek": "oak_fence",
        "bleekhout_poort": "oak_fence_gate", "bleekhout_deur": "oak_door", "bleekhout_luik": "oak_trapdoor"}
STEEN = {"harsstenen_trap": "brick_stairs", "harsstenen_plaat": "brick_slab", "harsstenen_muur": "brick_wall"}
LOGS = ["bleekhout_stam", "bleekhout_gestript", "bleekhout_gezicht"]
HARTEN = ["krakend_guhhartje", "verzuurd_guhhartje"]
BLOCKS = [*LOGS, "bleekhout_planken", *WOOD, "bleekhout_bord", "bleekhout_wandbord", "bleekhout_bladeren", "bleekhout_zaailing", "bleekmos",
          "bleekmos_tapijt", "bleek_hangmos", *HARTEN, "kaashars", "kaashars_blok", "harsstenen", *STEEN, "gebeitelde_harsstenen",
          "oogbloempje", "open_oogbloempje", "potted_oogbloempje", "potted_open_oogbloempje"]
NO_ITEM = {"bleekhout_wandbord", "potted_oogbloempje", "potted_open_oogbloempje"}
FTB_PORTRAIT = "geo:kraakguh:kraakguh"

SOUNDS = {  # our event -> (vanilla events it plays, subtitle)
    "bleekwoud.hart.klop": ([{"name": "minecraft:block.creaking_heart.idle", "type": "event"}], "Guhhartje klopt"),
    "bleekwoud.hart.wakker": ([{"name": "minecraft:block.creaking_heart.spawn", "type": "event"}], "Guhhartje wordt wakker"),
    "bleekwoud.hart.au": ([{"name": "minecraft:block.creaking_heart.hurt", "type": "event"}], "Guhhartje kraakt"),
    "bleekwoud.kraakguh.kraak": ([{"name": "minecraft:entity.creaking.ambient", "type": "event", "pitch": 1.3},
                                  {"name": "minecraft:entity.creaking.unfreeze", "type": "event", "pitch": 1.3}], "Krak... krak..."),
    "bleekwoud.kraakguh.bevries": ([{"name": "minecraft:entity.creaking.freeze", "type": "event", "pitch": 1.2}], "Houten guh staat stil"),
    "bleekwoud.kraakguh.au": ([{"name": "minecraft:entity.creaking.sway", "type": "event", "pitch": 1.2}], "Houten guh wiebelt"),
    "bleekwoud.kraakguh.knuffel": ([{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 0.8},
                                    {"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 0.8}], "Houten knuffel. Krak!"),
    "bleekwoud.kraakguh.verkruimel": ([{"name": "minecraft:entity.creaking.death", "type": "event", "pitch": 1.2}], "Houten guh verkruimelt"),
    "bleekwoud.oogbloempje.open": ([{"name": "minecraft:block.eyeblossom.open", "type": "event"}], "Oogbloempje gaat open"),
    "bleekwoud.oogbloempje.dicht": ([{"name": "minecraft:block.eyeblossom.close", "type": "event"}], "Oogbloempje gaat dicht"),
}

DAGBOEK = [
    "Dagboek van de Houthakkerguh.\n\nDag 1. Vahoeg! Ik heb een bos gevonden vol bleke bomen. Mooi wit hout, en zo lekker stil. Geen guh "
    "te zien. Ik bouw een hutje en ga hakken. Njeg!",
    "Dag 2. Vannacht kraakte er iets buiten. Krak... krak... Vast de wind. Maar het waait hier nooit. Ik heb mijn dekentje over mijn "
    "oortjes getrokken.",
    "Dag 3. Er staat een houten guh tussen de bomen. Die stond er gisteren niet! Hij beweegt niet. Hij kijkt alleen maar. Met oranje "
    "oogjes.",
    "Dag 4. Ik keek even naar mijn bijl. Toen ik weer opkeek, stond de houten guh DICHTERBIJ. Ik keek weg. Krak. Nog dichterbij! "
    "Zolang ik kijk, staat hij stil.",
    "Dag 5. Ik heb de hele nacht naar hem gestaard. Mijn oogjes prikken. Toen de zon opkwam, verkruimelde hij zomaar. En in die "
    "dikke boom klopt iets, net een hartje. Boem-boem.",
    "Dag 6. Ik moest niezen. Hatsjoe! Oogjes dicht. En toen... een knuffel. Een houten knuffel! Krak! Ik kon even bijna niet lopen. "
    "Hij wilde alleen maar knuffelen. Best lief, eigenlijk.",
    "Dag 7. Lief of niet: ik wil weer eens slapen. Ik verhuis naar Knuffeldal, daar kraakt niks. Wie dit leest: blijf kijken, of laat "
    "je knuffelen. Maar pas op voor de zure hartjes: daar komt een Mika uit. NJEG! Dag hutje.",
]
PAGE_MAX = 245

DEX = {  # page -> (name, where to find it, info)
    "kraakguh": ("Kraakguh", "Zeldzaam: 's nachts in het Bleekwoud, bij een boom met een Krakend Guhhartje",
                 "Een guh van bleek hout, met mos op zijn bol. Hij hoort bij een Krakend Guhhartje en komt alleen 's nachts. Hij "
                 "beweegt alleen als niemand kijkt: kijk je naar hem, dan staat hij doodstil. Kijk je weg... krak! Hij wil je alleen "
                 "maar een houten knuffel geven (daar word je even traag van). Slaan helpt niet: dan druipt er kaashars uit zijn "
                 "boom. Bij zonsopgang verkruimelt hij, en ook als je zijn hartje uit de boom hakt."),
    "kraak_mika": ("Kraak-Mika", "Heel zeldzaam: 's nachts in het Bleekwoud, bij een boom met een Verzuurd Guhhartje",
                   "Een Mika van bleek hout met geelgroene oogjes. Hij komt uit een verzuurd hartje. Net als de Kraakguh beweegt hij "
                   "alleen als niemand kijkt, maar hij komt niet knuffelen: hij komt duwen. NJEG! Pijn doet het niet. Na een "
                   "paar flinke meppen heeft hij er genoeg van en kruipt hij tot de volgende nacht terug in zijn boom."),
}

LANG = {
    "biome.guhs.bleekwoud": "Bleekwoud",
    "block.guhs.bleekhout_stam": "Bleekhoutstam", "block.guhs.bleekhout_gestript": "Gestripte bleekhoutstam",
    "block.guhs.bleekhout_gezicht": "Slapend guhgezichtje in de schors",
    "block.guhs.bleekhout_planken": "Bleekhoutplanken", "block.guhs.bleekhout_trap": "Bleekhouttrap", "block.guhs.bleekhout_plaat": "Bleekhoutplaat",
    "block.guhs.bleekhout_hek": "Bleekhouthek", "block.guhs.bleekhout_poort": "Bleekhoutpoort", "block.guhs.bleekhout_deur": "Bleekhoutdeur",
    "block.guhs.bleekhout_luik": "Bleekhoutluik", "block.guhs.bleekhout_bord": "Bleekhoutbord", "block.guhs.bleekhout_wandbord": "Bleekhoutbord",
    "block.guhs.bleekhout_bladeren": "Bleekhoutbladeren", "block.guhs.bleekhout_zaailing": "Bleekhoutzaailing",
    "block.guhs.bleekmos": "Bleekmos", "block.guhs.bleekmos_tapijt": "Bleekmostapijt", "block.guhs.bleek_hangmos": "Bleek hangmos",
    "block.guhs.krakend_guhhartje": "Krakend Guhhartje", "block.guhs.verzuurd_guhhartje": "Verzuurd Guhhartje",
    "block.guhs.kaashars": "Kaashars", "item.guhs.kaashars": "Kaashars", "block.guhs.kaashars_blok": "Blok kaashars",
    "item.guhs.harssteen": "Harssteen",
    "block.guhs.harsstenen": "Harsstenen", "block.guhs.harsstenen_trap": "Harsstenen trap", "block.guhs.harsstenen_plaat": "Harsstenen plaat",
    "block.guhs.harsstenen_muur": "Harsstenen muur", "block.guhs.gebeitelde_harsstenen": "Gebeitelde harsstenen",
    "block.guhs.oogbloempje": "Dicht oogbloempje", "block.guhs.open_oogbloempje": "Open oogbloempje",
    "block.guhs.potted_oogbloempje": "Dicht oogbloempje in een pot", "block.guhs.potted_open_oogbloempje": "Open oogbloempje in een pot",
    "item.guhs.kraakguh_spawn_egg": "Kraakguh-spawnei", "item.guhs.kraak_mika_spawn_egg": "Kraak-Mika-spawnei",
    "structure.guhs.bleke_open_plek": "Bleke open plek",
    "structure.guhs.bleke_open_plek.tooltip": "Een ronde open plek in het Bleekwoud vol oogbloempjes, met een oeroude boom waar een guhhartje in klopt",
    "structure.guhs.houthakkershutje": "Houthakkershutje",
    "structure.guhs.houthakkershutje.tooltip": "Het verlaten hutje van de Houthakkerguh in het Bleekwoud, met zijn dagboek",
    "gui.guhs.bleekwoud.knuffel": "De Kraakguh geeft je een houten knuffel. Krak!",
    "gui.guhs.bleekwoud.geen_naam": "Hij hoort bij zijn hartje: een naamplaatje wil hij niet.",
    "gui.guhs.bleekwoud.dagboek_kopie": "Je schrijft het dagboek van de Houthakkerguh snel over: nu heb je een eigen exemplaar!",
    "book.guhs.bleekwoud.dagboek.title": "Het dagboek van de Houthakkerguh",
    "book.guhs.bleekwoud.dagboek.door": "door de Houthakkerguh",
}

ADV = [  # name, parent, icon, frame, criteria (None: granted by code), title, description
    ("bleekwoud_bezocht", "enter_guhmension", "guhs:bleekhout_zaailing", "task",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{BIOME}"}}}}},
     "Waar is al het roze?", "Vind het Bleekwoud: een stil, bleek bos, heel zeldzaam in de Guhmensie"),
    ("bleekwoud_oogje", "bleekwoud_bezocht", "guhs:open_oogbloempje", "task",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:open_oogbloempje"}]}}},
     "Oogje open", "Pluk 's nachts een oogbloempje dat zijn oogje open heeft"),
    ("bleekwoud_knuffel", "bleekwoud_bezocht", "guhs:krakend_guhhartje", "goal", None,
     "Krak! Een knuffel", "Laat je 's nachts knuffelen door een Kraakguh (hij komt alleen als je niet kijkt...)"),
    ("bleekwoud_kaashars", "bleekwoud_knuffel", "guhs:kaashars", "task",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:kaashars"}]}}},
     "Plakkerige kaas", "Verzamel kaashars: het druipt uit de boom als je een Kraakguh een tikje geeft"),
    ("bleekwoud_hartje", "bleekwoud_knuffel", "minecraft:iron_axe", "goal", None,
     "Hartje gebroken", "Hak een Krakend Guhhartje uit zijn boom: zijn Kraakguh verkruimelt"),
    ("bleekwoud_geduwd", "bleekwoud_knuffel", "guhs:verzuurd_guhhartje", "goal", None,
     "Zuur hartje, zure Mika", "Word geduwd door een Kraak-Mika (die komt uit een Verzuurd Guhhartje). NJEG!"),
    ("bleekwoud_open_plek", "bleekwoud_bezocht", "guhs:oogbloempje", "goal",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{bouw.PLEK}"}}}}},
     "De Bleke Open Plek", "Vind de open plek in het Bleekwoud met de oeroude boom"),
    ("bleekwoud_hutje", "bleekwoud_bezocht", "guhs:bleekhout_deur", "goal",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{bouw.HUT}"}}}}},
     "Niemand thuis", "Vind het verlaten Houthakkershutje in het Bleekwoud"),
    ("bleekwoud_dagboek", "bleekwoud_hutje", "minecraft:written_book", "task", None,
     "Krak... krak...", "Lees het dagboek van de Houthakkerguh op de lessenaar in zijn hutje"),
]


def _jar():
    return zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))


# =====================================================================================================================
# blockstates, models, item models
# =====================================================================================================================
def copy_set(h, z, names, mapping, tex, group, planks_item, base_item):
    """Vanilla's blockstates, models, loot and recipes of `mapping` (ours -> vanilla's), with our textures and items."""
    A, D, w = h.A, h.D, h.w
    for ours, van in mapping.items():
        state = json.dumps(json.loads(z.read(f"assets/minecraft/blockstates/{van}.json")))
        for old, new in tex.items():
            state = state.replace(f'"{old}"', f'"{new}"')
        state = state.replace(f"minecraft:block/{van}", f"guhs:block/{ours}")
        w(f"{A}/blockstates/{ours}.json", json.loads(state))
        for path in names:
            if not path.startswith(f"assets/minecraft/models/block/{van}") or not path.endswith(".json"):
                continue
            rest = path[len(f"assets/minecraft/models/block/{van}"):-5]
            if van == "oak_fence" and rest.startswith("_gate"):
                continue
            model = json.loads(z.read(path))
            model["textures"] = {k: tex.get(v, v) for k, v in model.get("textures", {}).items()}
            if van in ("oak_door", "oak_trapdoor"):
                model["render_type"] = "minecraft:cutout"
            w(f"{A}/models/block/{ours}{rest}.json", model)
        loot = json.loads(z.read(f"data/minecraft/loot_table/blocks/{van}.json"))
        w(f"{D}/loot_table/blocks/{ours}.json", json.loads(json.dumps(loot).replace(f"minecraft:{van}", f"guhs:{ours}")))
        recipe = json.dumps(json.loads(z.read(f"data/minecraft/recipe/{van}.json")))
        recipe = recipe.replace(f'"item": "minecraft:{base_item}"', f'"item": "{planks_item}"')
        recipe = recipe.replace(f'"id": "minecraft:{van}"', f'"id": "guhs:{ours}"').replace('"group": "wooden_', f'"group": "{group}_')
        w(f"{D}/recipe/{ours}.json", json.loads(recipe))


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # --- logs, the face, planks, leaves, sapling, moss ---
    for name in ("bleekhout_stam", "bleekhout_gestript"):
        t = {"end": f"guhs:block/{name}_top", "side": f"guhs:block/{name}"}
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_column", "textures": t})
        w(f"{A}/models/block/{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
        w(f"{A}/blockstates/{name}.json", {"variants": {
            "axis=y": {"model": f"guhs:block/{name}"},
            "axis=z": {"model": f"guhs:block/{name}_horizontal", "x": 90},
            "axis=x": {"model": f"guhs:block/{name}_horizontal", "x": 90, "y": 90}}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    w(f"{A}/models/block/bleekhout_gezicht.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": "guhs:block/bleekhout_stam_top", "front": "guhs:block/bleekhout_gezicht", "side": "guhs:block/bleekhout_stam"}})
    w(f"{A}/blockstates/bleekhout_gezicht.json", {"variants": h.facing_states("bleekhout_gezicht")})
    w(f"{A}/models/item/bleekhout_gezicht.json", {"parent": "guhs:block/bleekhout_gezicht"})
    h.simple_block("bleekhout_planken")
    h.simple_block("bleekhout_bladeren", render_type="minecraft:cutout_mipped")
    h.simple_block("bleekmos")
    w(f"{A}/models/block/bleekhout_zaailing.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                    "textures": {"cross": "guhs:block/bleekhout_zaailing"}})
    w(f"{A}/blockstates/bleekhout_zaailing.json", {"variants": {"": {"model": "guhs:block/bleekhout_zaailing"}}})
    h.item_model("bleekhout_zaailing", "guhs:block/bleekhout_zaailing")
    # --- the sign (the block entity renderer draws it; the model only gives the break particles) ---
    for b in ("bleekhout_bord", "bleekhout_wandbord"):
        w(f"{A}/models/block/{b}.json", {"textures": {"particle": "guhs:block/bleekhout_planken"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
    h.item_model("bleekhout_bord")
    # --- the moss carpet (tufts climb the blocks next to it) and the hanging moss ---
    w(f"{A}/models/block/bleekmos_tapijt.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/bleekmos"}})
    for side in ("laag", "hoog"):
        w(f"{A}/models/block/bleekmos_tapijt_{side}.json", {"parent": "minecraft:block/mossy_carpet_side", "render_type": "minecraft:cutout",
                                                           "textures": {"side": f"guhs:block/bleekmos_tapijt_{side}"}})
    none = {"east": "none", "north": "none", "south": "none", "west": "none"}
    parts = [{"apply": {"model": "guhs:block/bleekmos_tapijt"}, "when": {"bottom": "true"}},
             {"apply": {"model": "guhs:block/bleekmos_tapijt"}, "when": {"bottom": "false", **none}}]
    for d, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        r = {"uvlock": True, "y": rot} if rot else {}
        parts.append({"apply": {"model": "guhs:block/bleekmos_tapijt_hoog", **r}, "when": {d: "tall"}})
        parts.append({"apply": {"model": "guhs:block/bleekmos_tapijt_laag", **r}, "when": {d: "low"}})
        parts.append({"apply": {"model": "guhs:block/bleekmos_tapijt_hoog", **r}, "when": {"bottom": "false", **none}})
    w(f"{A}/blockstates/bleekmos_tapijt.json", {"multipart": parts})
    w(f"{A}/models/item/bleekmos_tapijt.json", {"parent": "guhs:block/bleekmos_tapijt"})
    for name, t in (("bleek_hangmos", "bleek_hangmos"), ("bleek_hangmos_punt", "bleek_hangmos_punt")):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{t}"}})
    w(f"{A}/blockstates/bleek_hangmos.json", {"variants": {"tip=false": {"model": "guhs:block/bleek_hangmos"},
                                                           "tip=true": {"model": "guhs:block/bleek_hangmos_punt"}}})
    h.item_model("bleek_hangmos")
    # --- the hearts: uprooted, asleep, awake; upright and lying ---
    for name in HARTEN:
        variants = {}
        for state, suffix in (("uprooted", ""), ("dormant", "_slaapt"), ("awake", "_wakker")):
            t = {"end": f"guhs:block/{name}{suffix}_top", "side": f"guhs:block/{name}{suffix}"}
            w(f"{A}/models/block/{name}{suffix}.json", {"parent": "minecraft:block/cube_column", "textures": t})
            w(f"{A}/models/block/{name}{suffix}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
            for natural in ("true", "false"):
                variants[f"axis=y,creaking_heart_state={state},natural={natural}"] = {"model": f"guhs:block/{name}{suffix}"}
                variants[f"axis=z,creaking_heart_state={state},natural={natural}"] = {"model": f"guhs:block/{name}{suffix}_horizontal", "x": 90}
                variants[f"axis=x,creaking_heart_state={state},natural={natural}"] = {"model": f"guhs:block/{name}{suffix}_horizontal", "x": 90, "y": 90}
        w(f"{A}/blockstates/{name}.json", {"variants": variants})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_slaapt"})
    # --- kaashars ---
    h.simple_block("kaashars_blok")
    h.simple_block("harsstenen")
    h.simple_block("gebeitelde_harsstenen")
    h.item_model("kaashars")
    h.item_model("harssteen")
    # --- the oogbloempje and its pots ---
    for b in ("oogbloempje", "open_oogbloempje"):
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
        h.item_model(b, f"guhs:block/{b}")
        w(f"{A}/models/block/potted_{b}.json", {"parent": "minecraft:block/flower_pot_cross", "render_type": "minecraft:cutout",
                                                "textures": {"plant": f"guhs:block/{b}"}})
        w(f"{A}/blockstates/potted_{b}.json", {"variants": {"": {"model": f"guhs:block/potted_{b}"}}})
        w(f"{D}/loot_table/blocks/potted_{b}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}], "conditions": [{"condition": "minecraft:survives_explosion"}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        h.self_drop(b)
    for egg in ("kraakguh_spawn_egg", "kraak_mika_spawn_egg"):
        h.item_model(egg)

    # --- the vanilla-shaped sets: the wood (oak's), the bricks (brick's), the clump (glow lichen's), the sign's recipe ---
    with _jar() as z:
        names = z.namelist()
        wood_tex = {"minecraft:block/oak_planks": "guhs:block/bleekhout_planken", "minecraft:block/oak_door_top": "guhs:block/bleekhout_deur_top",
                    "minecraft:block/oak_door_bottom": "guhs:block/bleekhout_deur_bottom", "minecraft:block/oak_trapdoor": "guhs:block/bleekhout_luik"}
        copy_set(h, z, names, WOOD, wood_tex, "bleekhout", "guhs:bleekhout_planken", "oak_planks")
        steen_tex = {"minecraft:block/bricks": "guhs:block/harsstenen"}
        copy_set(h, z, names, STEEN, steen_tex, "harsstenen", "guhs:harsstenen", "bricks")
        lichen = json.dumps(json.loads(z.read("assets/minecraft/blockstates/glow_lichen.json"))).replace("minecraft:block/glow_lichen", "guhs:block/kaashars")
        w(f"{A}/blockstates/kaashars.json", json.loads(lichen))
        model = json.loads(z.read("assets/minecraft/models/block/glow_lichen.json"))
        model["textures"] = {k: "guhs:block/kaashars" for k in model["textures"]}
        model["render_type"] = "minecraft:cutout"
        w(f"{A}/models/block/kaashars.json", model)
        sign = json.dumps(json.loads(z.read("data/minecraft/recipe/oak_sign.json")))
        sign = sign.replace('"item": "minecraft:oak_planks"', '"item": "guhs:bleekhout_planken"').replace('"id": "minecraft:oak_sign"', '"id": "guhs:bleekhout_bord"')
        w(f"{D}/recipe/bleekhout_bord.json", json.loads(sign.replace('"group": "wooden_', '"group": "bleekhout_')))
        leaves = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/dark_oak_leaves.json")))
        leaves = json.loads(leaves.replace("minecraft:dark_oak_leaves", "guhs:bleekhout_bladeren").replace("minecraft:dark_oak_sapling", "guhs:bleekhout_zaailing"))
        leaves["pools"] = [p for p in leaves["pools"] if "minecraft:apple" not in json.dumps(p)]      # (no apples in a bleekhout tree)
        w(f"{D}/loot_table/blocks/bleekhout_bladeren.json", leaves)
    w(f"{A}/models/item/bleekhout_trap.json", {"parent": "guhs:block/bleekhout_trap"})
    w(f"{A}/models/item/bleekhout_plaat.json", {"parent": "guhs:block/bleekhout_plaat"})
    w(f"{A}/models/item/bleekhout_hek.json", {"parent": "guhs:block/bleekhout_hek_inventory"})
    w(f"{A}/models/item/bleekhout_poort.json", {"parent": "guhs:block/bleekhout_poort"})
    w(f"{A}/models/item/bleekhout_luik.json", {"parent": "guhs:block/bleekhout_luik_bottom"})
    h.item_model("bleekhout_deur")
    w(f"{A}/models/item/harsstenen_trap.json", {"parent": "guhs:block/harsstenen_trap"})
    w(f"{A}/models/item/harsstenen_plaat.json", {"parent": "guhs:block/harsstenen_plaat"})
    w(f"{A}/models/item/harsstenen_muur.json", {"parent": "guhs:block/harsstenen_muur_inventory"})


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
def loot_recipes_tags(h):
    D, w = h.D, h.w
    for b in ("bleekhout_stam", "bleekhout_gestript", "bleekhout_gezicht", "bleekhout_planken", "bleekhout_zaailing", "bleekhout_bord", "bleekmos",
              "bleekmos_tapijt", "bleek_hangmos", "kaashars_blok", "harsstenen", "gebeitelde_harsstenen"):
        h.self_drop(b)
    h.self_drop("bleekhout_wandbord", "bleekhout_bord")
    # the clump: one item per face it sticks to (vanilla's resin clump)
    faces = ("down", "east", "north", "south", "up", "west")
    w(f"{D}/loot_table/blocks/kaashars.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": "guhs:kaashars", "functions": [
            *[{"function": "minecraft:set_count", "add": True, "count": 1, "conditions": [{
                "condition": "minecraft:block_state_property", "block": "guhs:kaashars", "properties": {f: "true"}}]} for f in faces],
            {"function": "minecraft:set_count", "add": True, "count": -1},
            {"function": "minecraft:explosion_decay"}]}]}]})
    # a heart: with silk touch the heart itself, else 1-3 kaashars (more with fortune)
    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    for name in HARTEN:
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"guhs:{name}", "conditions": [silk]},
                {"type": "minecraft:item", "name": "guhs:kaashars", "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}, "add": False},
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                     "parameters": {"bonusMultiplier": 1}},
                    {"function": "minecraft:limit_count", "limit": {"max": 9}},
                    {"function": "minecraft:explosion_decay"}]}]}]}]})
    for e in ("kraakguh", "kraak_mika"):
        w(f"{D}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": []})
    # the chests: modest (a few things of the forest itself)
    c = h.count_fn
    w(f"{D}/loot_table/chests/{bouw.PLEK}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:bleekhout_zaailing", "functions": c(2, 4)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaashars", "functions": c(2, 5)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": c(3, 8)},
            {"type": "minecraft:item", "name": "guhs:oogbloempje", "weight": 4, "functions": c(2, 4)},
            {"type": "minecraft:item", "name": "guhs:bleekmos", "weight": 3, "functions": c(2, 4)},
            {"type": "minecraft:item", "name": "guhs:bleek_hangmos", "weight": 2, "functions": c(1, 3)},
            {"type": "minecraft:item", "name": "guhs:bleekhout_bord", "weight": 1}]}]})
    w(f"{D}/loot_table/chests/{bouw.HUT}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_axe"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:bleekhout_planken", "functions": c(8, 16)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:bleekhout_stam", "functions": c(4, 8)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": c(4, 8)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaashars", "functions": c(1, 3)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:stick", "weight": 3, "functions": c(2, 6)},
            {"type": "minecraft:item", "name": "guhs:bleekhout_zaailing", "weight": 3, "functions": c(1, 2)},
            {"type": "minecraft:item", "name": "minecraft:lantern", "weight": 1}]}]})

    # --- recipes ---
    for kind in ("block", "item"):
        h.add_tag(f"guhs/tags/{kind}/bleekhout_stammen", [f"guhs:{b}" for b in LOGS])
    h.shapeless("bleekhout_planken", ["#guhs:bleekhout_stammen"], "guhs:bleekhout_planken", 4)
    h.shapeless("bleekhout_gezicht", ["guhs:bleekhout_stam", "guhs:kaas_knabbels"], "guhs:bleekhout_gezicht", 1)
    h.shaped("bleekmos_tapijt", ["MM"], {"M": "guhs:bleekmos"}, "guhs:bleekmos_tapijt", 3)
    h.shaped("kaashars_blok", ["HHH", "HHH", "HHH"], {"H": "guhs:kaashars"}, "guhs:kaashars_blok", 1)
    h.shapeless("kaashars_uit_blok", ["guhs:kaashars_blok"], "guhs:kaashars", 9)
    w(f"{D}/recipe/harssteen.json", {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": "guhs:kaashars"},
                                     "result": {"id": "guhs:harssteen"}, "experience": 0.1, "cookingtime": 200})
    h.shaped("harsstenen", ["HH", "HH"], {"H": "guhs:harssteen"}, "guhs:harsstenen", 1)
    h.shaped("gebeitelde_harsstenen", ["P", "P"], {"P": "guhs:harsstenen_plaat"}, "guhs:gebeitelde_harsstenen", 1)
    for result, n in (("harsstenen_trap", 1), ("harsstenen_plaat", 2), ("harsstenen_muur", 1), ("gebeitelde_harsstenen", 1)):
        w(f"{D}/recipe/{result}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": "guhs:harsstenen"},
                                                   "result": {"id": f"guhs:{result}", "count": n}})
    h.shaped("krakend_guhhartje", ["S", "H", "S"], {"S": "guhs:bleekhout_stam", "H": "guhs:kaashars_blok"}, "guhs:krakend_guhhartje", 1)
    h.shapeless("verzuurd_guhhartje", ["guhs:krakend_guhhartje", "minecraft:fermented_spider_eye"], "guhs:verzuurd_guhhartje", 1)
    h.shapeless("oogbloempje_grijze_kleurstof", ["guhs:oogbloempje"], "minecraft:gray_dye", 1)
    h.shapeless("oogbloempje_oranje_kleurstof", ["guhs:open_oogbloempje"], "minecraft:orange_dye", 1)

    # --- tags ---
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", [f"guhs:{b}" for b in LOGS])
        add(f"minecraft/tags/{kind}/planks", ["guhs:bleekhout_planken"])
        add(f"minecraft/tags/{kind}/wooden_stairs", ["guhs:bleekhout_trap"])
        add(f"minecraft/tags/{kind}/wooden_slabs", ["guhs:bleekhout_plaat"])
        add(f"minecraft/tags/{kind}/wooden_fences", ["guhs:bleekhout_hek"])
        add(f"minecraft/tags/{kind}/fence_gates", ["guhs:bleekhout_poort"])
        add(f"minecraft/tags/{kind}/wooden_doors", ["guhs:bleekhout_deur"])
        add(f"minecraft/tags/{kind}/wooden_trapdoors", ["guhs:bleekhout_luik"])
        add(f"minecraft/tags/{kind}/leaves", ["guhs:bleekhout_bladeren"])
        add(f"minecraft/tags/{kind}/saplings", ["guhs:bleekhout_zaailing"])
        add(f"minecraft/tags/{kind}/small_flowers", ["guhs:oogbloempje", "guhs:open_oogbloempje"])
        add(f"minecraft/tags/{kind}/stairs", ["guhs:harsstenen_trap"])
        add(f"minecraft/tags/{kind}/slabs", ["guhs:harsstenen_plaat"])
        add(f"minecraft/tags/{kind}/walls", ["guhs:harsstenen_muur"])
    add("minecraft/tags/block/standing_signs", ["guhs:bleekhout_bord"])
    add("minecraft/tags/block/wall_signs", ["guhs:bleekhout_wandbord"])
    add("minecraft/tags/item/signs", ["guhs:bleekhout_bord"])
    add("minecraft/tags/block/flower_pots", ["guhs:potted_oogbloempje", "guhs:potted_open_oogbloempje"])
    add("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in LOGS + ["bleekhout_planken", *WOOD, "bleekhout_bord", "bleekhout_wandbord", *HARTEN]])
    add("minecraft/tags/block/mineable/hoe", ["guhs:bleekhout_bladeren", "guhs:bleekmos", "guhs:bleekmos_tapijt", "guhs:bleek_hangmos"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:harsstenen", *[f"guhs:{b}" for b in STEEN], "guhs:gebeitelde_harsstenen"])
    add("minecraft/tags/block/dirt", ["guhs:bleekmos"])
    add("minecraft/tags/block/sword_efficient", ["guhs:bleekmos_tapijt", "guhs:bleek_hangmos"])
    # what a bleekmos patch may turn into moss (bonemeal, and around the trees): the Guhmension's wool ground too
    add("guhs/tags/block/bleekmos_vervangbaar", ["#minecraft:moss_replaceable", "minecraft:pink_wool", "guhs:vadsmos", "guhs:bleekmos"])
    # every kind of Mika (1.2.8: another slice fills this tag with the others)
    add("guhs/tags/entity_type/mikas", ["guhs:kraak_mika"])


# =====================================================================================================================
# worldgen
# =====================================================================================================================
def worldgen(h):
    D, w = h.D, h.w
    wg = f"{D}/worldgen"
    log = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:bleekhout_stam", "Properties": {"axis": "y"}}}
    leaf = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:bleekhout_bladeren",
                                                                 "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}}
    hang = {"type": "guhs:bleek_hangmos", "leaves_probability": 0.15, "trunk_probability": 0.4, "ground_probability": 0.8}

    def big(decorators):
        return {"type": "minecraft:tree", "config": {
            "trunk_provider": log, "foliage_provider": leaf,
            "trunk_placer": {"type": "minecraft:dark_oak_trunk_placer", "base_height": 6, "height_rand_a": 2, "height_rand_b": 1},
            "foliage_placer": {"type": "minecraft:dark_oak_foliage_placer", "radius": 0, "offset": 0},
            "minimum_size": {"type": "minecraft:three_layers_feature_size", "limit": 1, "upper_limit": 1, "lower_size": 0, "middle_size": 1,
                             "upper_size": 2},
            "decorators": decorators, "ignore_vines": True, "force_dirt": False}}
    w(f"{wg}/configured_feature/bleekhout_boom.json", big([hang]))
    # the heart tree (placed apart, see below); one heart in five is soured
    w(f"{wg}/configured_feature/bleekhout_boom_hart.json", big([hang, {"type": "guhs:krakend_guhhartje", "probability": 1.0, "soured": 0.2}]))
    w(f"{wg}/configured_feature/bleekhout_boompje.json", {"type": "minecraft:tree", "config": {
        "trunk_provider": log, "foliage_provider": leaf,
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 2, "height_rand_b": 0},
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 3},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "decorators": [{"type": "guhs:bleek_hangmos", "leaves_probability": 0.12, "trunk_probability": 0.0, "ground_probability": 0.0}],
        "ignore_vines": True, "force_dirt": False}})
    survive = {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                           "state": {"Name": "guhs:bleekhout_zaailing", "Properties": {"stage": "0"}}}}
    # a dense canopy of big trees; a heart tree is tried in one chunk in two (measured in a generated forest: about one
    # awake heart within the 32 blocks around you at night, a handful per forest; one in five of them soured)
    for name, first in (("bleekhout_boom_hart", [{"type": "minecraft:rarity_filter", "chance": 2}]),
                        ("bleekhout_boom", [{"type": "minecraft:count", "count": 14}])):
        w(f"{wg}/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": [
            *first, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, survive, {"type": "minecraft:biome"}]})

    # moss patches (vanilla's pale moss patch with our blocks): around the trees and from bonemeal
    carpet = {"Name": "guhs:bleekmos_tapijt", "Properties": {"bottom": "true", "north": "none", "east": "none", "south": "none", "west": "none"}}
    groei = {"feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:weighted_state_provider", "entries": [
        {"weight": 22, "data": carpet}, {"weight": 3, "data": {"Name": "guhs:oogbloempje"}}]}}}, "placement": []}

    def patch(chance, lo, hi):
        return {"type": "minecraft:vegetation_patch", "config": {
            "replaceable": "#guhs:bleekmos_vervangbaar", "ground_state": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:bleekmos"}},
            "vegetation_feature": groei, "surface": "floor", "depth": 1, "extra_bottom_block_chance": 0.0, "vertical_range": 5,
            "vegetation_chance": chance, "xz_radius": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi},
            "extra_edge_column_chance": 0.75}}
    w(f"{wg}/configured_feature/bleekmos_plek.json", patch(0.3, 2, 4))
    w(f"{wg}/configured_feature/bleekmos_bonemeal.json", patch(0.6, 1, 2))
    w(f"{wg}/placed_feature/bleekmos_plekken.json", {"feature": "guhs:bleekmos_plek", "placement": [
        {"type": "minecraft:count", "count": 5}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})
    w(f"{wg}/configured_feature/oogbloempjes.json", {"type": "minecraft:random_patch", "config": {
        "tries": 24, "xz_spread": 5, "y_spread": 2, "feature": {"feature": {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:oogbloempje"}}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ["guhs:bleekmos", "minecraft:pink_wool"]}]}}]}}})
    w(f"{wg}/placed_feature/oogbloempjes.json", {"feature": "guhs:oogbloempjes", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})

    # the biome (already in the 26.1 shape: mc26.biome leaves it alone): silent, dim, pale, and nothing spawns
    base = json.load(open(f"{wg}/biome/pink_puffs.json", encoding="utf-8"))
    ores = base["features"][6]
    w(f"{wg}/biome/{BIOME}.json", {
        "attributes": {"minecraft:audio/background_music": {}, "minecraft:audio/music_volume": 0.0,
                       "minecraft:visual/fog_color": "#8a8280", "minecraft:visual/sky_color": "#b4aeb0",
                       "minecraft:visual/water_fog_color": "#5a6470"},
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.8,
        "effects": {"water_color": "#8a96a2", "foliage_color": "#b9a7ab", "dry_foliage_color": "#a8a0a0", "grass_color": "#a9a2a0"},
        "creature_spawn_probability": 0.0,
        "spawners": {k: [] for k in base["spawners"]},
        "spawn_costs": {}, "carvers": [],
        "features": [[], [], [], [], [], [], ores, [], [], ["guhs:bleekhout_boom_hart", "guhs:bleekhout_boom", "guhs:bleekmos_plekken",
                                                            "guhs:oogbloempjes"], []]})

    # the region: the noise, the router term on the temperature, the biome's own entry
    from features import knuffeldal_wereld as kw
    from features import verhaal_wereld as vw
    w(f"{wg}/noise/{NOISE}.json", {"firstOctave": NOISE_OCTAVE, "amplitudes": [1.0]})

    def term():
        buiten = kw.mul(kw.mul(kw.geen_zee(), vw.geen_knuffel()), kw.mul(vw.geen_polder(), kw.mul(
            vw.geen_toendra(), kw.spline(vw.GUHWAII_NOISE, [(GUHWAII_OFF[0], 1.0), (GUHWAII_OFF[1], 0.0)]))))
        vlak = {"type": "minecraft:cache_2d", "argument": kw.mul(kw.spline(NOISE, [(TERM[0], 0.0), (TERM[1], 1.0)]), buiten)}
        return kw.mul(vlak, kw.gradient(KELDER_Y[0], KELDER_Y[1], 0.0, 1.0))

    def router(d):
        r = d["noise_router"]
        if f"guhs:{NOISE}" not in json.dumps(r["temperature"]):      # (idempotent: the file is written fresh every full run)
            r["temperature"] = kw.add(r["temperature"], kw.mul(term(), -SHIFT))
            r["vegetation"] = kw.add(r["vegetation"], kw.mul(term(), SHIFT))
    h.patch_json(f"{wg}/noise_settings/guhmension.json", router)

    def biome_source(d):
        anything = [-2.0, 2.0]
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != f"guhs:{BIOME}"]
        entries.append({"biome": f"guhs:{BIOME}", "parameters": {"temperature": TEMP, "humidity": HUMID, "continentalness": [-1.0, 1.0],
                                                                 "erosion": anything, "weirdness": 0.0, "depth": [-1.0, 1.0], "offset": OFFSET}})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    def surface(d):
        rules = d["surface_rule"]["sequence"]
        rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
                "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0,
                             "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
                             "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:bleekmos"}}}}
        d["surface_rule"]["sequence"] = [r for r in rules if r.get("if_true", {}).get("biome_is") != [f"guhs:{BIOME}"]] + [rule]
    h.patch_json(f"{wg}/noise_settings/guhmension.json", surface)
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({f"guhs:{BIOME}": {"villager_type": "guhs:guh"}}))
    # the Knabbelkelders (the way to the Guheinde, on fixed rings around spawn) may lie under the Bleekwoud too
    h.add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", [f"guhs:{BIOME}"])


def structures(h):
    bouw.build(h)
    none = {"bounding_box": "piece", "spawns": []}
    over = {"monster": none, "creature": none, "ambient": none}
    h.TEMPLATE_SIZES[bouw.PLEK] = 17
    h.FLATNESS[bouw.PLEK] = 12
    # (the forests are small, 1 to 13 hectares: a tight grid, so that most forests have one of each)
    h.structure(bouw.PLEK, [BIOME], spacing=6, separation=3, salt=20281201, start_y=-bouw.G, reach=40, centre=bouw.PLEK_ANCHOR,
                spawn_overrides=over)
    h.TEMPLATE_SIZES[bouw.HUT] = 9
    h.FLATNESS[bouw.HUT] = 9
    h.structure(bouw.HUT, [BIOME], spacing=5, separation=2, salt=20281202, start_y=-bouw.G, reach=40, centre=bouw.HUT_ANCHOR,
                spawn_overrides=over)


# =====================================================================================================================
# sounds, advancements, texts
# =====================================================================================================================
def sounds(h):
    def patch(d):
        for event, (entries, _sub) in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for event, (_entries, sub) in SOUNDS.items():
        h.lang(f"subtitles.guhs.{event}", sub, sub)


def advancements(h):
    D = h.D
    for page in DEX:
        h.w(f"{D}/advancement/quest/seen_{page}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, title, desc in ADV:
        crit = crit or {"done": {"trigger": "minecraft:impossible"}}
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit})
        h.lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        h.lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for page, (name, rarity, info) in DEX.items():
        h.lang(f"entity.guhs.{page}", name, name)
        h.lang(f"gui.guhs.guhdex.rarity.{page}", "Zeldzaamheid: " + rarity, "Zeldzaamheid: " + rarity)
        h.lang(f"gui.guhs.guhdex.info.{page}", info, info)
    assert len(DAGBOEK) == bouw.DAGBOEK_PAGINAS
    for i, page in enumerate(DAGBOEK):
        assert len(page) <= PAGE_MAX, f"dagboek page {i} is {len(page)} long"
        h.lang(f"book.guhs.bleekwoud.dagboek.{i}", page, page)


def selfcheck(h):
    A = h.A
    missing = []
    for b in BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if b not in NO_ITEM and not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
        if not os.path.exists(f"{h.D}/loot_table/blocks/{b}.json"):
            missing.append(f"loot {b}")
    for i in ("harssteen", "kaashars", "kraakguh_spawn_egg", "kraak_mika_spawn_egg"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
    prefixes = ("bleekhout", "bleekmos", "bleek_hangmos", "krakend_guhhartje", "verzuurd_guhhartje", "kaashars", "harsstenen", "gebeitelde_harsstenen",
                "oogbloempje", "open_oogbloempje", "potted_oogbloempje", "potted_open_oogbloempje")
    for f in os.listdir(f"{A}/models/block"):
        if f.startswith(prefixes):
            model = json.load(open(os.path.join(A, "models", "block", f), encoding="utf-8"))
            for t in model.get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    missing.append(f"texture {t} ({f})")
    for e in DEX:
        for p in (f"{A}/geckolib/models/entity/{e}.geo.json", f"{A}/textures/entity/{e}.png", f"{A}/textures/entity/{e}_glowmask.png"):
            if not os.path.exists(p):
                missing.append(p)
    if missing:
        raise SystemExit(f"bleekwoud assets missing: {missing}")


def build(h):
    tex.build(h)
    blocks_and_items(h)
    loot_recipes_tags(h)
    worldgen(h)
    structures(h)
    sounds(h)
    advancements(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB Quests: the section "Het Bleekwoud" in the chapter guhs_extra27 (nothing locked: every player can do every quest)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    y = 72
    q("bleekwoud_bezoek", "Het Bleekwoud", "Heel zeldzaam, ergens ver weg in de Guhmensie, ligt een bos waar al het roze uit is "
      "weggelopen: het &7Bleekwoud&r. Witte stammen, grijs mos, en het is er muisstil: er woont geen enkele guh. Zoek het op!",
      "guhs:bleekhout_zaailing", [fq.biome(BIOME)], rewards=(("guhs:kaas_knabbels", 12),), x=-8, y=y, shape="circle", xp=100)
    q("bleekwoud_hout", "Bleekhout", "Hak een bleke boom om en maak &7bleekhoutplanken&r, een bleekhoutdeur en een bleekhoutbord. "
      "Eén zaailing wordt een klein boompje, vier in een vierkant een dikke boom.",
      "guhs:bleekhout_planken", [item("guhs:bleekhout_planken", 16), item("guhs:bleekhout_deur"), item("guhs:bleekhout_bord")],
      rewards=(("guhs:bleekhout_zaailing", 4),), x=-6, y=y, xp=50)
    q("bleekwoud_oogje", "Oogje open", "Overdag slapen de &7oogbloempjes&r. 's Nachts doen ze een voor een hun oogje open en worden ze "
      "&6oranje&r. Pluk er een terwijl hij open is!", "guhs:open_oogbloempje", [item("guhs:open_oogbloempje")],
      rewards=(("minecraft:flower_pot", 1),), x=-4, y=y, xp=50)
    q("bleekwoud_kraakguh", "Krak... krak...", "In sommige bomen zit een &6Krakend Guhhartje&r. 's Nachts wordt het wakker en roept "
      "het zijn &7Kraakguh&r: een guh van hout. Hij beweegt alleen als je NIET kijkt. Zoek er een op (binnen 8 blokjes) voor je Guhdex.",
      "guhs:kraakguh_spawn_egg", [adv("seen_kraakguh")], rewards=(("guhs:kaas_knabbels", 12),), x=-2, y=y, shape="gear", xp=100)
    q("bleekwoud_knuffel", "Een houten knuffel", "De Kraakguh is een guh, dus hij is lief: hij wil je alleen maar knuffelen. Draai je "
      "om, wacht even... &6Krak!&r Je wordt er een paar tellen traag van, meer niet.",
      "guhs:krakend_guhhartje", [adv("guhs:guhmension/bleekwoud_knuffel")], rewards=(("guhs:gefrituurde_kaasknabbels", 2),), x=0, y=y, xp=150)
    q("bleekwoud_kaashars", "Kaashars", "Geef een Kraakguh een tikje: hij voelt er niks van, maar uit de stam rond zijn hartje druipt "
      "&6kaashars&r. Schraap het van de boom. Negen maken een blok, en gesmolten wordt het een &6harssteen&r.",
      "guhs:kaashars", [item("guhs:kaashars", 8)], rewards=(("guhs:kaas_knabbels", 8),), x=2, y=y, xp=100)
    q("bleekwoud_harsstenen", "Harsstenen", "Smelt kaashars in een oven tot harsstenen en bouw er &6harsstenen&r mee: warm kaasgeel, "
      "met trappen, platen en muurtjes.", "guhs:harsstenen", [item("guhs:harsstenen", 8)], rewards=(("guhs:kaashars", 4),), x=4, y=y, xp=100)
    q("bleekwoud_hartje", "Hartje gebroken", "Wil je van een Kraakguh af? Zoek de boom waar het sprankelspoor naartoe gaat en hak "
      "het &6Krakend Guhhartje&r eruit. Met Zijden aanraking kun je het hartje meenemen en thuis tussen twee bleekhoutstammen zetten.",
      "minecraft:iron_axe", [adv("guhs:guhmension/bleekwoud_hartje")], rewards=(("guhs:kaashars", 3),), x=6, y=y, xp=150)
    y = 74
    q("bleekwoud_open_plek", "De Bleke Open Plek", "Midden in het Bleekwoud ligt een ronde open plek vol oogbloempjes, met een "
      "oeroude boom. Hoog in zijn stam klopt een hartje... Het superkompas (Avontuur) wijst de weg.",
      "guhs:oogbloempje", [fq.structure(bouw.PLEK)], rewards=(("guhs:kaas_knabbels", 12),), x=-8, y=y, shape="gear", xp=150)
    q("bleekwoud_hutje", "Het Houthakkershutje", "Iemand heeft hier gewoond: de &7Houthakkerguh&r. Zijn bijl zit nog in de stronk. "
      "Waar is hij gebleven? Het superkompas (Avontuur) wijst de weg.",
      "guhs:bleekhout_deur", [fq.structure(bouw.HUT)], rewards=(("guhs:kaas_knabbels", 12),), x=-6, y=y, shape="gear", xp=150)
    q("bleekwoud_dagboek", "Het dagboek", "Op de lessenaar in het hutje ligt het &ddagboek van de Houthakkerguh&r. Klik op de "
      "lessenaar: iedereen krijgt een eigen exemplaar.", "minecraft:written_book", [adv("guhs:guhmension/bleekwoud_dagboek")],
      rewards=(("guhs:bleekhout_zaailing", 2),), x=-4, y=y, xp=100)
    q("bleekwoud_kraak_mika", "Zuur hartje, zure Mika", "Een op de vijf hartjes is &averzuurd&r (geelgroen). Daar komt geen Kraakguh "
      "uit, maar een &cKraak-Mika&r: die komt je duwen als je niet kijkt. NJEG! Na een paar meppen kruipt hij terug in zijn boom.",
      "guhs:kraak_mika_spawn_egg", [adv("seen_kraak_mika")], rewards=(("guhs:mika_vet", 1),), x=-2, y=y, shape="gear", xp=150)
