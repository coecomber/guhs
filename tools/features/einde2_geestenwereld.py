"""
einde2 (the Guheinde expansion), slice geestenwereld: De Geestenwereld (DESIGN_EINDE2 5 "De Geestenwereld" + "Uitwerking"),
a small dimension of its own, guhs:geestenwereld. Java: nl.juiced.guhs.feature.einde2.geestenwereld (E2GeestenwereldFeature).

  - the dimension: no sun, no day and night (fixed time), a sky of slow pastel northern lights (E2GeestenwereldSky draws
    textures/environment/geestenwereld_*.png), lila mist, floating lights in the air, its own quiet sound (a loop and a
    whisper now and then, einde2_geestenwereld_geluid.py writes the OGGs);
  - the void with floating islands in the mist, 608 x 608 blocks around 0,0: ONE hand-made build
    (einde2_geestenwereld_bouw.py) with seven zones, cut in 32 x 32 tiles (data/guhs/structure/geestenwereld/stuk_<i>_<j>)
    and put down chunk by chunk by the feature guhs:einde2_geestenwereld_stukken (the only feature of the one biome), always
    at the same spot (template 0,0,0 = world -304, 32, -304); its markers are the sidecar data/guhs/einde2/plekken/
    geestenwereld.json ("soort": "vast", with "dimensie" and "oorsprong"); data/guhs/einde2/geestenwereld_versie.json holds
    a number made from the build: a chunk that was made from an older build is put down again when it loads;
  - the blocks with "turned around" colours (einde2_geestenwereld_tex.py): geestengras, geestenaarde, geestenhout,
    schaduwhout and their planks, gloeibladeren (cyan, pink), maansteen (+ tiles, stairs, slab), nevelwater (falls up), the
    spirit mushroom, the lotus leaf, the spirit lantern, the twelve masks of Koh's cave, and three blocks that do something:
    the meditatiesteen (right-click: back to your body), the gedenksteen (right-click: its lore) and the lotuspuzzeltegel
    (right-click: it turns; the puzzle of the White Lotus gate);
  - texts (einde2_geestenwereld_tekst.py: names, the lore of every stone, messages), sounds, the test templates.
"""
import hashlib
import json
import os

import features.einde2_kit as kit
from features import einde2_geestenwereld_bouw as bouw
from features import einde2_geestenwereld_tekst as tekst
from features import einde2_geestenwereld_tex as tex

S = "einde2_geestenwereld"
DIM = "geestenwereld"
FEATURE = f"{S}_stukken"
MUZIEK = "minecraft:music.overworld.cherry_grove"

# id -> (light, map colour hint for the wiki) ; every block has an item and a name
STEEN = ["maansteen", "maansteen_tegels", "maansteen_trap", "maansteen_plaat", "geestenaarde", "geestengras", "meditatiesteen", "gedenksteen",
         "lotuspuzzeltegel"]
HOUT = ["geestenhout", "geestenplanken", "schaduwhout", "schaduwplanken", "geestpaddenstoel_steel"]
ZACHT = ["gloeibladeren_cyaan", "gloeibladeren_roze", "schaduwbladeren", "geestpaddenstoel_hoed", "lotusblad", "nevelwater", "geestlantaarn", "geestenmasker"]
BLOKKEN = STEEN + HOUT + ZACHT

SOUNDS = {
    f"{S}.sfeer": ["sfeer"],
    f"{S}.gefluister": ["gefluister1", "gefluister2", "gefluister3"],
    f"{S}.meditatie": ["meditatie"],
    f"{S}.tegel": ["tegel"],
}


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def dimensie(h):
    D = h.D
    # (one invisible layer of structure_void at y 0: a flat world of only air counts as "void" and then runs no biome
    # features, and the build comes from the biome's one feature)
    h.w(f"{D}/dimension/{DIM}.json", {"type": f"guhs:{DIM}", "generator": {
        "type": "minecraft:flat", "settings": {"biome": f"guhs:{DIM}", "layers": [{"block": "minecraft:structure_void", "height": 1}],
                                               "lakes": False, "features": True, "structure_overrides": []}}})
    # written in the 26.1 shape (like the Guhpixel dimension): a fixed soft light, no beds, the northern lights sky
    h.w(f"{D}/dimension_type/{DIM}.json", {
        "attributes": {
            "minecraft:audio/background_music": {
                "default": {"sound": MUZIEK, "min_delay": 6000, "max_delay": 18000},
                "creative": {"sound": MUZIEK, "min_delay": 6000, "max_delay": 18000}},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/sky_light_level": 12.0,
            "minecraft:visual/ambient_light_color": "#2c2440",
            "minecraft:visual/cloud_color": "#00ffffff",
            "minecraft:visual/fog_color": "#c4b2ea",
            "minecraft:visual/sky_color": "#7c68c0",
            "neoforge:custom_skybox": f"guhs:{DIM}",
        },
        "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False, "coordinate_scale": 1.0,
        "ambient_light": 0.12, "min_y": 0, "height": 256, "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "has_fixed_time": True, "timelines": "#minecraft:universal"})
    steps = [[] for _ in range(11)]
    steps[0] = [f"guhs:{FEATURE}"]          # (raw generation: the flat generator skips the structure steps)
    h.w(f"{D}/worldgen/biome/{DIM}.json", {
        "has_precipitation": False, "temperature": 0.6, "downfall": 0.0,
        "attributes": {
            # floating lights: cyan glow and a few white sparks drifting through the mist
            "minecraft:visual/ambient_particles": [
                {"particle": {"type": "minecraft:glow"}, "probability": 0.0035},
                {"particle": {"type": "minecraft:end_rod"}, "probability": 0.0006}],
            "minecraft:visual/fog_start_distance": 24.0,
            "minecraft:visual/fog_end_distance": 170.0,
        },
        "effects": {"sky_color": 0x7C68C0, "fog_color": 0xC4B2EA, "water_color": 0xA8ECF4, "water_fog_color": 0x9AB8F0,
                    "grass_color": 0xC4A4EC, "foliage_color": 0x9CE8E4,
                    "ambient_sound": f"guhs:{S}.sfeer",
                    "additions_sound": {"sound": f"guhs:{S}.gefluister", "tick_chance": 0.0016},
                    "music": {"sound": MUZIEK, "min_delay": 6000, "max_delay": 18000, "replace_current_music": False}},
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": steps})
    h.w(f"{D}/worldgen/configured_feature/{FEATURE}.json", {"type": f"guhs:{FEATURE}", "config": {}})
    h.w(f"{D}/worldgen/placed_feature/{FEATURE}.json", {"feature": f"guhs:{FEATURE}", "placement": []})


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def _model(h, naam, model):
    h.w(f"{h.A}/models/block/{naam}.json", model)


def _een(h, naam, model_naam=None):
    h.w(f"{h.A}/blockstates/{naam}.json", {"variants": {"": {"model": f"guhs:block/{model_naam or naam}"}}})


def _item(h, naam, model=None):
    h.w(f"{h.A}/models/item/{naam}.json", {"parent": model or f"guhs:block/{naam}"})


def _paal(h, naam, zij, top):
    _model(h, naam, {"parent": "minecraft:block/cube_column", "textures": {"side": zij, "end": top}})
    _model(h, f"{naam}_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"side": zij, "end": top}})
    h.w(f"{h.A}/blockstates/{naam}.json", {"variants": {
        "axis=y": {"model": f"guhs:block/{naam}"},
        "axis=z": {"model": f"guhs:block/{naam}_horizontal", "x": 90},
        "axis=x": {"model": f"guhs:block/{naam}_horizontal", "x": 90, "y": 90}}})
    _item(h, naam)


def blocks_and_items(h):
    A, g = h.A, "guhs:block/"
    for naam in ("geestenaarde", "geestenplanken", "schaduwplanken", "maansteen", "maansteen_tegels", "geestpaddenstoel_hoed"):
        h.simple_block(naam)
    for naam in ("gloeibladeren_cyaan", "gloeibladeren_roze", "schaduwbladeren"):
        h.simple_block(naam, render_type="minecraft:cutout_mipped")
    h.simple_block("nevelwater", render_type="minecraft:translucent")
    h.w(f"{h.TEX}/block/nevelwater.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})
    _model(h, "geestengras", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": g + "geestengras_top", "bottom": g + "geestenaarde", "side": g + "geestengras_zij"}})
    _een(h, "geestengras")
    _item(h, "geestengras")
    _paal(h, "geestenhout", g + "geestenhout", g + "geestenhout_top")
    _paal(h, "schaduwhout", g + "schaduwhout", g + "schaduwhout_top")
    _paal(h, "geestpaddenstoel_steel", g + "geestpaddenstoel_steel", g + "geestpaddenstoel_steel")
    # maansteen stairs and slab (from the tiles), like the Guheinde's own
    t = g + "maansteen_tegels"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        _model(h, f"maansteen_trap{suffix}", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
    from features.guheinde import stair_states
    h.w(f"{A}/blockstates/maansteen_trap.json", {"variants": stair_states("guhs:block/maansteen_trap")})
    _item(h, "maansteen_trap")
    _model(h, "maansteen_plaat", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    _model(h, "maansteen_plaat_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/blockstates/maansteen_plaat.json", {"variants": {"type=bottom": {"model": "guhs:block/maansteen_plaat"},
                                                              "type=top": {"model": "guhs:block/maansteen_plaat_top"},
                                                              "type=double": {"model": "guhs:block/maansteen_tegels"}}})
    _item(h, "maansteen_plaat")
    # the lotus leaf: half a block high, see-through between its round edges
    _model(h, "lotusblad", {"parent": "minecraft:block/slab", "render_type": "minecraft:cutout",
                            "textures": {"bottom": g + "lotusblad_zij", "top": g + "lotusblad", "side": g + "lotusblad_zij"}})
    _een(h, "lotusblad")
    _item(h, "lotusblad")
    # the meditation stone and the memorial stone (its carved text faces the way it was put)
    _model(h, "meditatiesteen", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": g + "meditatiesteen_top", "bottom": g + "maansteen", "side": g + "meditatiesteen"}})
    _een(h, "meditatiesteen")
    _item(h, "meditatiesteen")
    _model(h, "gedenksteen", {"parent": "minecraft:block/orientable", "textures": {
        "top": g + "gedenksteen", "front": g + "gedenksteen_voor", "side": g + "gedenksteen"}})
    h.w(f"{A}/blockstates/gedenksteen.json", {"variants": h.facing_states("gedenksteen")})
    _item(h, "gedenksteen")
    # the lotus puzzle tile: one piece of the big lotus on top (stuk), turned a quarter per click (draai)
    for stuk in ("hoek", "rand", "midden"):
        _model(h, f"lotuspuzzeltegel_{stuk}", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": g + f"lotuspuzzeltegel_{stuk}", "bottom": g + "lotuspuzzeltegel_zij", "side": g + "lotuspuzzeltegel_zij"}})
    h.w(f"{A}/blockstates/lotuspuzzeltegel.json", {"variants": {
        f"draai={d},stuk={stuk}": ({"model": f"guhs:block/lotuspuzzeltegel_{stuk}", "y": 90 * d} if d else {"model": f"guhs:block/lotuspuzzeltegel_{stuk}"})
        for stuk in ("hoek", "rand", "midden") for d in range(4)}})
    _item(h, "lotuspuzzeltegel", "guhs:block/lotuspuzzeltegel_midden")
    # the masks of Koh's cave: a thin plate on the wall behind it, the face looking the block's facing
    for i, m in enumerate(tex.MASKERS):
        _model(h, f"geestenmasker_{m}", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                          "textures": {"particle": g + f"geestenmasker_{m}", "gezicht": g + f"geestenmasker_{m}"},
                                          "elements": [{"from": [1, 1, 15], "to": [15, 15, 16], "faces": {
                                              "north": {"uv": [1, 1, 15, 15], "texture": "#gezicht"},
                                              "south": {"uv": [1, 1, 15, 15], "texture": "#gezicht"},
                                              "east": {"uv": [0, 1, 1, 15], "texture": "#gezicht"}, "west": {"uv": [15, 1, 16, 15], "texture": "#gezicht"},
                                              "up": {"uv": [1, 0, 15, 1], "texture": "#gezicht"}, "down": {"uv": [1, 15, 15, 16], "texture": "#gezicht"}}}]})
    h.w(f"{A}/blockstates/geestenmasker.json", {"variants": {
        f"facing={f},gezicht={i}": ({"model": f"guhs:block/geestenmasker_{m}", "y": y} if y else {"model": f"guhs:block/geestenmasker_{m}"})
        for i, m in enumerate(tex.MASKERS) for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _item(h, "geestenmasker", "guhs:block/geestenmasker_noh")
    # the spirit lantern: the vanilla lantern shapes with the pale cyan texture
    _model(h, "geestlantaarn", {"parent": "minecraft:block/template_lantern", "textures": {"lantern": g + "geestlantaarn"}})
    _model(h, "geestlantaarn_hangend", {"parent": "minecraft:block/template_hanging_lantern", "textures": {"lantern": g + "geestlantaarn"}})
    h.w(f"{A}/blockstates/geestlantaarn.json", {"variants": {"hanging=false": {"model": "guhs:block/geestlantaarn"},
                                                            "hanging=true": {"model": "guhs:block/geestlantaarn_hangend"}}})
    h.item_model("geestlantaarn", tex="guhs:block/geestlantaarn")
    # loot: each drops itself (in the Geestenwereld nothing can be broken; elsewhere a creative player may place them)
    for naam in BLOKKEN:
        if naam != "maansteen_plaat":
            h.self_drop(naam)
    h.w(f"{h.D}/loot_table/blocks/maansteen_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "guhs:maansteen_plaat", "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": "guhs:maansteen_plaat", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    gg = lambda names: [f"guhs:{n}" for n in names]  # noqa: E731
    h.add_tag("minecraft/tags/block/mineable/pickaxe", gg(STEEN))
    h.add_tag("minecraft/tags/block/mineable/axe", gg(HOUT))
    h.add_tag("minecraft/tags/block/mineable/hoe", gg(["gloeibladeren_cyaan", "gloeibladeren_roze", "schaduwbladeren", "geestpaddenstoel_hoed", "lotusblad"]))
    h.add_tag("minecraft/tags/block/stairs", ["guhs:maansteen_trap"])
    h.add_tag("minecraft/tags/item/stairs", ["guhs:maansteen_trap"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:maansteen_plaat"])
    h.add_tag("minecraft/tags/item/slabs", ["guhs:maansteen_plaat"])


# =====================================================================================================================
# sounds, texts, the build
# =====================================================================================================================
def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            entry = {"sounds": [{"name": f"guhs:{S}/{f}", "stream": f == "sfeer"} for f in files], "subtitle": f"subtitles.guhs.{event}"}
            d[event] = entry
    h.patch_json(f"{h.A}/sounds.json", patch)


def texts(h):
    for key, text in tekst.TEXTS.items():
        kit.lang(h, key, text)


def wereld(h):
    """The build: self-check, tiles, sidecar, version."""
    b = bouw.maak(h)
    problems = bouw.controleer(b)
    if problems:
        raise SystemExit(f"{DIM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    nx, nz = b.save_tiles()
    blad = b.blad("vast")
    blad["dimensie"] = f"guhs:{DIM}"
    blad["oorsprong"] = list(bouw.OORSPRONG)
    blad["tiles"] = [nx, nz]
    blad["zones"] = list(bouw.ZONES)
    h.w(f"{h.D}/einde2/plekken/{DIM}.json", blad)
    # the version: a number from the blocks themselves (the same build = the same number, also on another machine)
    hsh = hashlib.sha256()
    for (x, y, z), (n, props, nbt) in sorted(b.s.blocks.items()):
        hsh.update(f"{x},{y},{z},{n},{sorted(props.items())},{json.dumps(nbt, sort_keys=True, default=str) if nbt else ''};".encode())
    versie = int.from_bytes(hsh.digest()[:4], "big") & 0x7FFFFFFF
    h.w(f"{h.D}/einde2/{DIM}_versie.json", {"versie": max(1, versie)})
    print(f"{DIM}: self-check ok {b.stats}, {nx} x {nz} tiles ({len(b.tiles[2])} saved), version {versie}")
    return b


def test_templates(h):
    """einde2_geestenwereld_test_kamer: a floor of maansteen 9 x 9 with a meditation stone, a memorial stone (lore id
    test) and a lotus puzzle tile."""
    t = h.Structure((9, 5, 9))
    t.fill(0, 0, 0, 8, 0, 8, "guhs:maansteen_tegels")
    t.set(2, 1, 2, "guhs:meditatiesteen")
    t.set(6, 1, 2, "guhs:gedenksteen", {"facing": "south"})
    t.set(4, 0, 6, "guhs:lotuspuzzeltegel", {"stuk": "hoek", "draai": "1"})
    t.save(f"{S}_test_kamer")


def selfcheck(h):
    A, D = h.A, h.D
    missing = [k for k in tekst.TEXTS if k not in h.NL]
    for naam in BLOKKEN:
        if not os.path.exists(f"{A}/blockstates/{naam}.json") or not os.path.exists(f"{A}/models/item/{naam}.json"):
            missing.append(f"block/item json {naam}")
        if f"block.guhs.{naam}" not in tekst.TEXTS:
            missing.append(f"name of {naam}")
    for t in tex.TEXTURES:
        if not os.path.exists(f"{A}/textures/block/{t}.png"):
            missing.append(f"texture {t}")
    for t in ("geestenwereld_lucht", "geestenwereld_noorderlicht"):
        if not os.path.exists(f"{A}/textures/environment/{t}.png"):
            missing.append(f"sky {t}")
    for f in (f"dimension/{DIM}.json", f"dimension_type/{DIM}.json", f"worldgen/biome/{DIM}.json", f"worldgen/configured_feature/{FEATURE}.json",
              f"worldgen/placed_feature/{FEATURE}.json", f"einde2/plekken/{DIM}.json", f"einde2/{DIM}_versie.json", f"structure/{S}_test_kamer.nbt"):
        if not os.path.exists(f"{D}/{f}"):
            missing.append(f)
    for files in SOUNDS.values():
        for f in files:
            if not os.path.exists(f"{A}/sounds/{S}/{f}.ogg"):
                missing.append(f"sound {f} (run python tools/features/einde2_geestenwereld_geluid.py)")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in tekst.TEXTS:
            missing.append(f"subtitle {event}")
    if not missing:
        blad = json.load(open(f"{D}/einde2/plekken/{DIM}.json", encoding="utf-8"))
        for zone in bouw.ZONES:
            for m in (f"zone_{zone}", f"plek_{zone}", f"meditatiesteen_{zone}"):
                if m not in blad["plekken"]:
                    missing.append(f"marker {m}")
        for lore in tekst.LORE:
            if f"lore_{lore}" not in blad["plekken"]:
                missing.append(f"lore stone {lore}")
    if missing:
        raise SystemExit(f"{S}: missing {missing}")


def build(h):
    dimensie(h)
    tex.build(h)
    blocks_and_items(h)
    sounds(h)
    texts(h)
    wereld(h)
    test_templates(h)
    selfcheck(h)
