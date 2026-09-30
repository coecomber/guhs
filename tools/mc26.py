"""
Minecraft 26.1.2 resource formats (Guhs 1.1.0).

The generators (make_resources.py -> make_v2.py -> features/*) still build the resources in the shapes they always used; this
module turns the whole resource tree into the 26.1.2 formats. make_resources.py runs it as its last step, and it can be run
on its own (it only touches files that are still in an old shape, so running it twice changes nothing):

    python tools/mc26.py            (from the project root)

What it does (see MIGRATION_NOTES.md "Behaviour changes" for the parts that are not 1:1):
  * GeckoLib 5 asset folders: assets/guhs/geo -> assets/guhs/geckolib/models, animations -> geckolib/animations
  * client item definitions assets/guhs/items/<item>.json for every item model (1.21.4), model "overrides" -> range_dispatch /
    select / condition item models, spawn eggs get a baked texture (26.1 has no spawn egg tints any more)
  * block models: "render_type" is gone (26.1 picks the layer from the texture); translucent -> "force_translucent" textures
  * equipment assets for the humanoid armour (assets/guhs/equipment + textures/entity/equipment)
  * recipes: plain-string ingredients (1.21.2)
  * loot tables / advancements: text components as JSON objects (1.21.5), advancement backgrounds as texture ids
  * biomes: "effects" colours/sounds/music/particles -> environment attributes (1.21.11), carvers as a list
  * dimension types: attributes, skybox, cardinal light, timelines and clocks (1.21.11 / 26.1)
  * random_patch configured features (removed in 26.1) -> the inner feature + count/random_offset/filter placements
  * renamed vanilla tags
  * villager trades as data (26.1 trade_set / villager_trade registries)
"""
import copy
import glob
import io
import json
import os
import shutil
import sys
import zipfile

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")

changed = []


def rd(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            if f.read().replace("\r\n", "\n") == text:
                return
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    changed.append(path)


def hexrgb(v):
    if isinstance(v, str):
        return v
    return "#%06x" % (v & 0xFFFFFF)


# ---------------------------------------------------------------------------------------------------------------------
# GeckoLib 5: assets/<ns>/geckolib/models|animations
# ---------------------------------------------------------------------------------------------------------------------

def geckolib_folders():
    moves = [(os.path.join(A, "geo"), os.path.join(A, "geckolib", "models")),
             (os.path.join(A, "animations"), os.path.join(A, "geckolib", "animations"))]
    for old, new in moves:
        if not os.path.isdir(old):
            continue
        for src in glob.glob(os.path.join(old, "**", "*.json"), recursive=True):
            dst = os.path.join(new, os.path.relpath(src, old))
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            if os.path.exists(dst):
                os.remove(dst)
            shutil.move(src, dst)
            changed.append(dst)
        shutil.rmtree(old)
    # GeckoLib 5 refuses animation files without animations in a dev environment: give them one empty idle animation
    for path in glob.glob(os.path.join(A, "geckolib", "animations", "**", "*.json"), recursive=True):
        data = rd(path)
        if not data.get("animations"):
            data["animations"] = {"animation.guhs.leeg": {"loop": True, "animation_length": 1.0, "bones": {}}}
            w(path, data)


# ---------------------------------------------------------------------------------------------------------------------
# Text components (1.21.5): JSON objects instead of JSON strings
# ---------------------------------------------------------------------------------------------------------------------

def text(v):
    """A text component that may still be a JSON string ("{\"translate\": ...}") -> the object."""
    if isinstance(v, str):
        s = v.strip()
        if s.startswith("{") or s.startswith("["):
            try:
                v = json.loads(s)
            except ValueError:
                return v
        elif s.startswith('"') and s.endswith('"'):
            try:
                return json.loads(s)
            except ValueError:
                return v
    return events(v)


def events(v):
    """clickEvent/hoverEvent -> click_event/hover_event (1.21.5)."""
    if isinstance(v, list):
        return [events(x) for x in v]
    if not isinstance(v, dict):
        return v
    out = {}
    for k, x in v.items():
        if k == "clickEvent":
            ev = dict(x)
            action = ev.get("action")
            value = ev.pop("value", None)
            if value is not None:
                key = {"open_url": "url", "run_command": "command", "suggest_command": "command", "change_page": "page",
                       "copy_to_clipboard": "value", "open_file": "path"}.get(action, "value")
                ev[key] = int(value) if key == "page" else value
            out["click_event"] = ev
        elif k == "hoverEvent":
            ev = dict(x)
            if "contents" in ev:
                c = ev.pop("contents")
                if ev.get("action") == "show_text":
                    ev["value"] = text(c)
                elif isinstance(c, dict):
                    ev.update(c)
            out["hover_event"] = ev
        elif k in ("extra", "with"):
            out[k] = [text(e) for e in x]
        else:
            out[k] = x
    return out


TEXT_COMPONENTS = ("minecraft:custom_name", "minecraft:item_name")


def item_components(comps):
    for k in TEXT_COMPONENTS:
        if k in comps:
            comps[k] = text(comps[k])
    if "minecraft:lore" in comps:
        comps["minecraft:lore"] = [text(x) for x in comps["minecraft:lore"]]
    book = comps.get("minecraft:written_book_content")
    if isinstance(book, dict) and "pages" in book:
        book["pages"] = [page(p) for p in book["pages"]]
    return comps


def page(p):
    if isinstance(p, dict) and ("raw" in p or "filtered" in p):
        return {k: text(x) for k, x in p.items()}
    return text(p)


def walk_components(o):
    """Every item stack/predicate "components" map and every loot function that holds text."""
    if isinstance(o, list):
        for x in o:
            walk_components(x)
        return
    if not isinstance(o, dict):
        return
    fn = o.get("function")
    if fn == "minecraft:set_written_book_pages" and "pages" in o:
        o["pages"] = [page(p) for p in o["pages"]]
    elif fn == "minecraft:set_name" and "name" in o:
        o["name"] = text(o["name"])
    elif fn == "minecraft:set_lore" and "lore" in o:
        o["lore"] = [text(x) for x in o["lore"]]
    for k, v in o.items():
        if k == "components" and isinstance(v, dict):
            item_components(v)
        walk_components(v)


# ---------------------------------------------------------------------------------------------------------------------
# Recipes (1.21.2): ingredients are "item", "#tag" or ["item", ...]
# ---------------------------------------------------------------------------------------------------------------------

def ingredient(v):
    if isinstance(v, str):
        return v
    if isinstance(v, dict):
        if "item" in v and len(v) == 1:
            return v["item"]
        if "tag" in v and len(v) == 1:
            return "#" + v["tag"]
        return v  # a NeoForge custom ingredient
    if isinstance(v, list):
        parts = [ingredient(x) for x in v]
        if len(parts) == 1:
            return parts[0]
        if all(isinstance(p, str) and not p.startswith("#") for p in parts):
            return parts
        return {"neoforge:ingredient_type": "neoforge:compound", "children": parts}
    return v


def recipe(d):
    t = d.get("type", "")
    if "key" in d:
        d["key"] = {k: ingredient(v) for k, v in d["key"].items()}
    if "ingredients" in d:
        d["ingredients"] = [ingredient(v) for v in d["ingredients"]]
    for k in ("ingredient", "template", "base", "addition"):
        if k in d:
            d[k] = ingredient(d[k])
    res = d.get("result")
    if isinstance(res, dict) and "item" in res and "id" not in res:
        res["id"] = res.pop("item")
    if isinstance(res, str) and t != "minecraft:stonecutting":
        d["result"] = {"id": res}
    if isinstance(res, dict) and res.get("count") == 1:
        del res["count"]
    return d


# ---------------------------------------------------------------------------------------------------------------------
# Advancements
# ---------------------------------------------------------------------------------------------------------------------

def advancement(d):
    disp = d.get("display")
    if disp:
        bg = disp.get("background")
        if isinstance(bg, str) and bg.endswith(".png"):
            ns, path = bg.split(":", 1) if ":" in bg else ("minecraft", bg)
            if path.startswith("textures/"):
                path = path[len("textures/"):]
            disp["background"] = f"{ns}:{path[:-4]}"
        for k in ("title", "description"):
            if k in disp:
                disp[k] = text(disp[k])
    walk_components(d)
    return d


# ---------------------------------------------------------------------------------------------------------------------
# Biomes (1.21.11): colours, sounds, music and particles are environment attributes
# ---------------------------------------------------------------------------------------------------------------------

GAME = {"sound": "minecraft:music.game", "min_delay": 12000, "max_delay": 24000}
CREATIVE = {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000}
UNDER_WATER = {"sound": "minecraft:music.under_water", "min_delay": 12000, "max_delay": 24000}


def music_entry(m):
    e = {"sound": m["sound"], "min_delay": m.get("min_delay", 12000), "max_delay": m.get("max_delay", 24000)}
    if m.get("replace_current_music"):
        e["replace_current_music"] = True
    return e


def background_music(music=None, underwater=False):
    """1.21.1 Minecraft#getSituationalMusic in a non-vanilla dimension: creative flying -> creative music, under water in a
    plays_underwater_music biome -> under water music, else the biome's music or the game music."""
    bm = {"default": music_entry(music) if music else dict(GAME), "creative": dict(CREATIVE)}
    if underwater:
        bm["underwater"] = dict(UNDER_WATER)
    return bm


def sound_id(s):
    return s["sound_id"] if isinstance(s, dict) and "sound_id" in s else s


def biome(d, underwater=False):
    eff = d.get("effects", {})
    attrs = d.get("attributes", {})
    if "fog_color" in eff or "sky_color" in eff or "mood_sound" in eff or "music" in eff or "particle" in eff \
            or "minecraft:audio/background_music" not in attrs:
        new_eff = {}
        for k in ("water_color", "foliage_color", "dry_foliage_color", "grass_color"):
            if k in eff:
                new_eff[k] = hexrgb(eff[k])
        if "grass_color_modifier" in eff and eff["grass_color_modifier"] != "none":
            new_eff["grass_color_modifier"] = eff["grass_color_modifier"]
        if "water_color" not in new_eff:
            new_eff["water_color"] = "#3f76e4"
        if "fog_color" in eff:
            attrs["minecraft:visual/fog_color"] = hexrgb(eff["fog_color"])
        if "sky_color" in eff:
            attrs["minecraft:visual/sky_color"] = hexrgb(eff["sky_color"])
        if "water_fog_color" in eff:
            attrs["minecraft:visual/water_fog_color"] = hexrgb(eff["water_fog_color"])
        if "particle" in eff:
            p = eff["particle"]
            attrs["minecraft:visual/ambient_particles"] = [{"particle": p["options"], "probability": p["probability"]}]
        sounds = {}
        if "ambient_sound" in eff:
            sounds["loop"] = sound_id(eff["ambient_sound"])
        if "mood_sound" in eff:
            m = dict(eff["mood_sound"])
            m["sound"] = sound_id(m["sound"])
            sounds["mood"] = m
        if "additions_sound" in eff:
            a = dict(eff["additions_sound"])
            a["sound"] = sound_id(a["sound"])
            sounds["additions"] = a
        if sounds:
            attrs["minecraft:audio/ambient_sounds"] = sounds
        music = eff.get("music")
        if isinstance(music, list):  # weighted list: take the heaviest
            music = max(music, key=lambda e: e.get("weight", 1))["data"]
        attrs["minecraft:audio/background_music"] = background_music(music, underwater)
        d["effects"] = new_eff
    d["attributes"] = dict(sorted(attrs.items()))
    carvers = d.get("carvers")
    if isinstance(carvers, dict):
        d["carvers"] = [c for step in carvers.values() for c in (step if isinstance(step, list) else [step])]
    # the vanilla files list the keys alphabetically; keep ours readable: attributes, climate, effects, the rest
    order = ["attributes", "has_precipitation", "temperature", "temperature_modifier", "downfall", "effects"]
    return {k: d[k] for k in order if k in d} | {k: v for k, v in d.items() if k not in order}


# ---------------------------------------------------------------------------------------------------------------------
# Dimension types (1.21.11 + 26.1)
# ---------------------------------------------------------------------------------------------------------------------

# the 26.1.2 vanilla overworld/nether/end lighting and fog, which is what the 1.21.1 "effects" of those dimensions did
OVERWORLD_VISUAL = {"minecraft:visual/ambient_light_color": "#0a0a0a", "minecraft:visual/cloud_color": "#ccffffff",
                    "minecraft:visual/cloud_height": 192.33, "minecraft:visual/fog_color": "#c0d8ff",
                    "minecraft:visual/sky_color": "#78a7ff"}
NETHER_VISUAL = {"minecraft:visual/ambient_light_color": "#302821", "minecraft:visual/fog_start_distance": 10.0,
                 "minecraft:visual/fog_end_distance": 96.0, "minecraft:visual/sky_light_color": "#7a7aff",
                 "minecraft:visual/sky_light_factor": 0.0}
END_VISUAL = {"minecraft:visual/ambient_light_color": "#3f473f", "minecraft:visual/sky_color": "#000000",
              "minecraft:visual/sky_light_color": "#ac60cd", "minecraft:visual/sky_light_factor": 0.0}
OVERWORLD_BED = {"can_set_spawn": "always", "can_sleep": "when_dark", "error_message": {"translate": "block.minecraft.bed.no_sleep"}}
EXPLODING_BED = {"can_set_spawn": "never", "can_sleep": "never", "explodes": True}

# our DimensionSpecialEffects of 1.21.1 (client code): which sky they drew and their fog colour
EFFECTS = {
    # GuhmensionSky: overworld sky with a pink moon and pink stars (drawn by the guhs:guhmension skybox renderer)
    "guhs:guhmension": {"skybox": "overworld", "visual": OVERWORLD_VISUAL, "custom_skybox": "guhs:guhmension"},
    # GuheindeSky: an end-like sky (drawn by the guhs:guheinde skybox renderer), constant dark-purple fog
    "guhs:guheinde": {"skybox": "end", "visual": dict(END_VISUAL, **{"minecraft:visual/fog_color": "#3d1f33"}),
                      "custom_skybox": "guhs:guheinde"},
    # BarbecuetherSky: no sky, nether lighting, thick fog everywhere (colour per biome)
    "guhs:barbecuether": {"skybox": "none", "cardinal_light": "nether", "visual": NETHER_VISUAL},
    "minecraft:the_nether": {"skybox": "none", "cardinal_light": "nether", "visual": NETHER_VISUAL},
    "minecraft:overworld": {"skybox": "overworld", "visual": OVERWORLD_VISUAL},
    "minecraft:the_end": {"skybox": "end", "visual": END_VISUAL},
}


def sky_light_level(fixed_time):
    """1.21.1 Level#updateSkyBrightness for a fixed time of day (no rain): 15 - skyDarken."""
    import math
    d0 = (fixed_time / 24000.0 - 0.25) % 1.0
    d1 = 0.5 - math.cos(d0 * math.pi) / 2.0
    time_of_day = (d0 * 2.0 + d1) / 3.0                                   # DimensionType#timeOfDay
    d2 = 0.5 + 2.0 * min(max(math.cos(time_of_day * math.pi * 2), -0.25), 0.25)
    return float(15 - int((1.0 - d2) * 11.0))


def dimension_type(d):
    if "attributes" in d and "ultrawarm" not in d:
        return d  # already 26.1
    eff = EFFECTS[d.get("effects", "minecraft:overworld")]
    fixed = d.get("fixed_time")
    attrs = dict(eff["visual"])
    attrs["minecraft:audio/background_music"] = background_music()
    if d.get("bed_works", True):
        # 1.21.1: sleeping needs a "natural" dimension (no spawn point either), the bed only explodes when bed_works is false
        attrs["minecraft:gameplay/bed_rule"] = OVERWORLD_BED if d.get("natural", True) else {"can_set_spawn": "never", "can_sleep": "never"}
    else:
        attrs["minecraft:gameplay/bed_rule"] = EXPLODING_BED
    attrs["minecraft:gameplay/respawn_anchor_works"] = bool(d.get("respawn_anchor_works", False))
    if not d.get("has_raids", True):
        attrs["minecraft:gameplay/can_start_raid"] = False
    if d.get("piglin_safe", False):
        attrs["minecraft:gameplay/piglins_zombify"] = False
    if d.get("natural", False):
        attrs["minecraft:gameplay/nether_portal_spawns_piglin"] = True
    if d.get("ultrawarm", False):
        attrs["minecraft:gameplay/water_evaporates"] = True
        attrs["minecraft:gameplay/fast_lava"] = True
        attrs["minecraft:visual/default_dripstone_particle"] = {"type": "minecraft:dripping_dripstone_lava"}
    if fixed is not None:
        level = sky_light_level(fixed)
        if level != 15.0:
            attrs["minecraft:gameplay/sky_light_level"] = level
    if "custom_skybox" in eff:
        attrs["neoforge:custom_skybox"] = eff["custom_skybox"]
    out = {
        "attributes": dict(sorted(attrs.items())),
        "has_skylight": d["has_skylight"],
        "has_ceiling": d["has_ceiling"],
        "has_ender_dragon_fight": False,
        "coordinate_scale": d["coordinate_scale"],
        "ambient_light": d["ambient_light"],
        "min_y": d["min_y"],
        "height": d["height"],
        "logical_height": d["logical_height"],
        "infiniburn": d["infiniburn"],
        "monster_spawn_light_level": d["monster_spawn_light_level"],
        "monster_spawn_block_light_limit": d["monster_spawn_block_light_limit"],
    }
    if eff["skybox"] != "overworld":
        out["skybox"] = eff["skybox"]
    if "cardinal_light" in eff:
        out["cardinal_light"] = eff["cardinal_light"]
    if fixed is not None:
        out["has_fixed_time"] = True
        out["timelines"] = "#minecraft:universal"
    else:
        # the 1.21.1 day time of a dimension without fixed_time was the overworld's
        out["default_clock"] = "minecraft:overworld"
        out["timelines"] = "#minecraft:in_overworld"
    return out


# ---------------------------------------------------------------------------------------------------------------------
# random_patch (removed in 26.1) -> the inner feature; its tries/spread/filter move into the placed features
# ---------------------------------------------------------------------------------------------------------------------

def spread(n):
    return {"type": "minecraft:trapezoid", "min": -n, "max": n, "plateau": 0} if n else 0


def patch_placements(cfg):
    """random_patch placed `tries` copies of its feature at pos + (rand(xz+1)-rand(xz+1), rand(y+1)-rand(y+1), ...)."""
    mods = [{"type": "minecraft:count", "count": cfg.get("tries", 128)}]
    xz, y = cfg.get("xz_spread", 7), cfg.get("y_spread", 3)
    if xz or y:
        mods.append({"type": "minecraft:random_offset", "xz_spread": spread(xz), "y_spread": spread(y)})
    return mods + list(cfg["feature"].get("placement", []))


def random_patches(data_root):
    """Rewrites every guhs random_patch (and flower) configured feature + the placed features that use it."""
    cf_dir = os.path.join(data_root, "worldgen", "configured_feature")
    pf_dir = os.path.join(data_root, "worldgen", "placed_feature")
    marker_dir = os.path.join(data_root, "worldgen", "placed_feature")
    patches = {}
    for path in glob.glob(os.path.join(cf_dir, "**", "*.json"), recursive=True):
        d = rd(path)
        if d.get("type") in ("minecraft:random_patch", "minecraft:flower", "minecraft:no_bonemeal_flower"):
            name = "guhs:" + os.path.relpath(path, cf_dir)[:-5].replace(os.sep, "/")
            cfg = d["config"]
            inner = cfg["feature"]["feature"]
            if isinstance(inner, str):
                raise SystemExit(f"{path}: random_patch around a feature reference is not supported")
            patches[name] = patch_placements(cfg)
            w(path, inner)
    if not patches:
        return
    for path in glob.glob(os.path.join(pf_dir, "**", "*.json"), recursive=True):
        d = rd(path)
        if d.get("feature") in patches:
            d["placement"] = list(d.get("placement", [])) + patches[d["feature"]]
            w(path, d)
    del marker_dir


# ---------------------------------------------------------------------------------------------------------------------
# Other worldgen codec changes up to 26.1
# ---------------------------------------------------------------------------------------------------------------------

def noise_settings(d):
    """26.1: the noise router's initial_density_without_jaggedness is replaced by preliminary_surface_level (a
    find_top_surface function). 1.21.1 NoiseChunk#computePreliminarySurfaceLevel scanned from min_y + height down in
    cell_height steps for the first initial density > 0.390625; this is the same scan as a density function."""
    router = d.get("noise_router", {})
    if "initial_density_without_jaggedness" in router:
        noise = d["noise"]
        initial = router.pop("initial_density_without_jaggedness")
        router["preliminary_surface_level"] = {
            "type": "minecraft:find_top_surface",
            "density": {"type": "minecraft:add", "argument1": -0.390625, "argument2": initial},
            "upper_bound": float(noise["min_y"] + noise["height"]),
            "lower_bound": noise["min_y"],
            "cell_height": noise["size_vertical"] * 4,
        }
        d["noise_router"] = dict(sorted(router.items()))
    return d


def feature_configs(o):
    """RuleBasedBlockStateProvider is a normal (typed) state provider since 1.21.2; huge mushrooms name their ground."""
    if isinstance(o, list):
        return [feature_configs(x) for x in o]
    if not isinstance(o, dict):
        return o
    if set(o) == {"fallback", "rules"}:
        o = {"type": "minecraft:rule_based_state_provider", **o}
    t = o.get("type")
    if t in ("minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom") and "can_place_on" not in o.get("config", {}):
        # 1.21.1 AbstractHugeMushroomFeature: dirt or mushroom_grow_block; 26.1 vanilla has this tag for it
        o["config"]["can_place_on"] = {"type": "minecraft:matching_block_tag", "tag": t + "_can_place_on"}
    return {k: feature_configs(v) for k, v in o.items()}


def worldgen(ns_dir):
    for path in glob.glob(os.path.join(ns_dir, "worldgen", "noise_settings", "**", "*.json"), recursive=True):
        w(path, noise_settings(rd(path)))
    for folder in ("configured_feature", "placed_feature"):
        for path in glob.glob(os.path.join(ns_dir, "worldgen", folder, "**", "*.json"), recursive=True):
            w(path, feature_configs(rd(path)))


# ---------------------------------------------------------------------------------------------------------------------
# Tags renamed in 1.21.2 ... 26.1
# ---------------------------------------------------------------------------------------------------------------------

TAG_RENAMES = {
    ("block", "snow_layer_cannot_survive_on"): "cannot_support_snow_layer",
    ("block", "snow_layer_can_survive_on"): "support_override_snow_layer",
}


def tags():
    root = os.path.join(R, "data", "minecraft", "tags")
    for (kind, old), new in TAG_RENAMES.items():
        src = os.path.join(root, kind, old + ".json")
        if os.path.exists(src):
            dst = os.path.join(root, kind, new + ".json")
            data = rd(src)
            if os.path.exists(dst):
                other = rd(dst)
                data["values"] = other["values"] + [v for v in data["values"] if v not in other["values"]]
            w(dst, data)
            os.remove(src)
            changed.append(src)


# ---------------------------------------------------------------------------------------------------------------------
# Models: render_type (gone in 26.1), spawn eggs (no tints any more)
# ---------------------------------------------------------------------------------------------------------------------

def model_path(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    return os.path.join(A, "models", *path.split("/")) + ".json" if ns == "guhs" else None


def effective_textures(ref, seen=None):
    p = model_path(ref)
    if not p or not os.path.exists(p):
        return {}
    d = rd(p)
    tex = effective_textures(d["parent"]) if "parent" in d else {}
    tex.update(d.get("textures", {}))
    return tex


def models():
    for path in glob.glob(os.path.join(A, "models", "**", "*.json"), recursive=True):
        d = rd(path)
        rt = d.pop("render_type", None)
        if rt is None:
            continue
        if rt.split(":")[-1] == "translucent":
            tex = effective_textures(d["parent"]) if "parent" in d else {}
            tex.update(d.get("textures", {}))
            new = {}
            for k, v in tex.items():
                if isinstance(v, str) and not v.startswith("#"):
                    new[k] = {"sprite": v, "force_translucent": True}
                else:
                    new[k] = v
            d["textures"] = new
        w(path, d)


# 1.21.1 DeferredSpawnEggItem colours (base, spots); the textures are baked like the old tinted template_spawn_egg
SPAWN_EGGS = {
    "guh_spawn_egg": (0xF7CCDA, 0x78C4EB), "mika_spawn_egg": (0xE2AABE, 0x8C1428), "guh_vis_spawn_egg": (0xF7A8C8, 0x78C4EB),
    "guh_bee_spawn_egg": (0xF7B6CB, 0xFFD24A), "guh_slime_spawn_egg": (0xF090C0, 0xFFD0E4), "nether_mika_spawn_egg": (0x5A2A30, 0xFF7A20),
    "guhschaapje_spawn_egg": (0xFFF0F6, 0xF08CB4), "knabbelkippetje_spawn_egg": (0xF7C83C, 0xF08CB4), "guhkoe_spawn_egg": (0xFFF4DC, 0xE8A93A),
    "vadswaker_spawn_egg": (0x6B4A2E, 0xF2C94C), "mika_larfje_spawn_egg": (0xE8C25A, 0x8C1428), "kikkerguh_spawn_egg": (0xF08CB4, 0x9CC84A),
    "kaasmot_spawn_egg": (0x4A2A48, 0xF7C83C), "moerasheks_mika_spawn_egg": (0x7FA046, 0x4B2A6A), "kruimel_mika_spawn_egg": (0xE9B478, 0x9A3A5A),
    "pluisegeltje_spawn_egg": (0xF2D2C4, 0xB77A6A), "guh_konijntje_spawn_egg": (0xF8C6D8, 0xFFF4F8),
    "pluiseekhoorntje_spawn_egg": (0xE08A4E, 0xFFE6C8), "shuckle_spawn_egg": (0xD8323A, 0xF6D64A), "mew_spawn_egg": (0xFAB2D2, 0x4682DE),
    "pieppiepmuisje_spawn_egg": (0x2A2233, 0xECE4D0), "poepschilly_spawn_egg": (0x70803E, 0xEEE4D0), "schilly_spawn_egg": (0x86A04A, 0x6A5E50),
    "boze_kaasknabbel_spawn_egg": (0xF5A93A, 0x5C2A10), "boze_oppernabbel_spawn_egg": (0xEC8428, 0xFACE48),
    "rookguh_spawn_egg": (0xF2F0F4, 0xF08CB4), "vonk_mika_spawn_egg": (0xE8742A, 0x5A2A1E), "knekel_mika_spawn_egg": (0x2A2426, 0xD23C3C),
    "aangebrande_mika_spawn_egg": (0x1C1718, 0xFF8A2A), "pluisvinkje_spawn_egg": (0xFFE2EE, 0xF696BE),
    "kaasmeesje_spawn_egg": (0xFAD654, 0xB896D6), "guh_uiltje_spawn_egg": (0xC48E88, 0xFFE27A), "zeemeeuwtje_spawn_egg": (0xFCFCFA, 0xB2BCCC),
    "guhxolotl_spawn_egg": (0xF8B2D0, 0xD63A84), "guh_eendje_spawn_egg": (0xFFF8EC, 0xFA963C),
    "knabbelvlindertje_spawn_egg": (0xFCD656, 0xC4A8F0), "glimguhtje_spawn_egg": (0xFAE2EC, 0xD8F070),
    "lieveheersbeestje_spawn_egg": (0xDE2834, 0x1E1822), "ijscoguh_spawn_egg": (0xFFF4F8, 0xF08CB8),
    # no item any more (the quest guh egg was removed long ago), but its model is still generated: give it the guh's colours
    "quest_guh_spawn_egg": (0xF7CCDA, 0x78C4EB),
}

_JAR = None


def vanilla_121(path):
    """A texture of Minecraft 1.21.1 (the spawn egg template is gone in 26.1)."""
    global _JAR
    from PIL import Image
    if _JAR is None:
        name = "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"
        for base in (os.path.join("build", "moddev", "artifacts"), os.path.join("..", "guhs", "build", "moddev", "artifacts")):
            if os.path.exists(os.path.join(base, name)):
                _JAR = zipfile.ZipFile(os.path.join(base, name))
                break
        else:
            raise SystemExit(f"{name} not found (copy it from the 1.21.1 tree's build/moddev/artifacts)")
    return Image.open(io.BytesIO(_JAR.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def tint(img, rgb):
    r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
    px = img.load()
    out = img.copy()
    po = out.load()
    for y in range(img.height):
        for x in range(img.width):
            pr, pg, pb, pa = px[x, y]
            po[x, y] = (pr * r // 255, pg * g // 255, pb * b // 255, pa)
    return out


def spawn_eggs():
    from PIL import Image
    for name, (base, spots) in SPAWN_EGGS.items():
        model = os.path.join(A, "models", "item", name + ".json")
        if not os.path.exists(model):
            continue
        d = rd(model)
        tex_path = os.path.join(A, "textures", "item", name + ".png")
        if d.get("parent") == "minecraft:item/template_spawn_egg" or not os.path.exists(tex_path):
            egg = Image.alpha_composite(tint(vanilla_121("item/spawn_egg"), base), tint(vanilla_121("item/spawn_egg_overlay"), spots))
            os.makedirs(os.path.dirname(tex_path), exist_ok=True)
            egg.save(tex_path)
            changed.append(tex_path)
            w(model, {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})


# ---------------------------------------------------------------------------------------------------------------------
# Client item definitions (1.21.4): assets/guhs/items/<item>.json
# ---------------------------------------------------------------------------------------------------------------------

def plain(ref):
    return {"type": "minecraft:model", "model": ref}


def from_overrides(base_ref, overrides):
    """The 1.21.1 item model overrides of our items -> 26.1 item models (same looks, vanilla properties where possible)."""
    keys = {k for o in overrides for k in o["predicate"]}
    if keys == {"angle"}:
        # the guh compasses: the vanilla needle pointing at the lodestone tracker target (spins without one / elsewhere)
        entries = [{"threshold": o["predicate"]["angle"] * 32.0, "model": plain(o["model"])} for o in overrides]
        return {"type": "minecraft:range_dispatch", "property": "minecraft:compass", "target": "lodestone", "scale": 32.0,
                "entries": entries, "fallback": plain(base_ref)}
    if keys == {"cast"}:
        cast = next(o["model"] for o in overrides if o["predicate"]["cast"] >= 1)
        return {"type": "minecraft:condition", "property": "minecraft:fishing_rod/cast", "on_true": plain(cast), "on_false": plain(base_ref)}
    if keys == {"custom_model_data"}:
        # TankonderdeelItem sets CustomModelData floats [n] (26.1: a list of floats instead of one int)
        entries = [{"threshold": float(o["predicate"]["custom_model_data"]), "model": plain(o["model"])} for o in overrides]
        return {"type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data", "index": 0,
                "entries": entries, "fallback": plain(base_ref)}
    if keys == {"guhs:kleur"} and base_ref.startswith("guhs:block/motknabbel"):
        # the motknabbel item keeps its colour in the block_state component
        cases = [{"when": "roze", "model": plain("guhs:block/motknabbel_roze")},
                 {"when": "mint", "model": plain("guhs:block/motknabbel_mint")},
                 {"when": "geel", "model": plain("guhs:block/motknabbel_geel")}]
        return {"type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "kleur",
                "cases": cases, "fallback": plain(base_ref)}
    if keys == {"guhs:kleur"}:
        # the guhxolotl emmertje: colour from its custom data -> the select property guhs:guhxolotl_kleur (client code)
        colours = ["roze", "mint", "choco", "wit", "goud"]
        cases = [{"when": c, "model": plain(f"guhs:item/guhxolotl_emmertje_{c}" if c != "roze" else base_ref)} for c in colours]
        for o in overrides:
            c = colours[round(o["predicate"]["guhs:kleur"] * 4)]
            cases[colours.index(c)]["model"] = plain(o["model"])
        return {"type": "minecraft:select", "property": "guhs:guhxolotl_kleur", "cases": cases, "fallback": plain(base_ref)}
    raise SystemExit(f"{base_ref}: unknown override predicates {keys}")


OVERRIDE_BASES = {}  # item name -> (base model ref, overrides), filled from the 1.21.1 shapes before they are dropped


def item_definitions():
    item_dir = os.path.join(A, "models", "item")
    defs_dir = os.path.join(A, "items")
    sub_models = set()
    specials = {}
    for path in glob.glob(os.path.join(item_dir, "*.json")):
        name = os.path.basename(path)[:-5]
        d = rd(path)
        if "overrides" in d:
            overrides = d.pop("overrides")
            base = f"guhs:item/{name}"
            if set(d) <= {"parent"} and d.get("parent", "").startswith("guhs:block/"):
                base = d["parent"]  # a model that only points at a block model (motknabbel)
            specials[name] = from_overrides(base, overrides)
            w(path, d)
            for o in overrides:
                if o["model"].startswith("guhs:item/") and o["model"] != f"guhs:item/{name}":
                    sub_models.add(o["model"][len("guhs:item/"):])
    # overrides converted in an earlier run: their definitions are already written, keep them and their sub-models
    for path in glob.glob(os.path.join(defs_dir, "*.json")):
        name = os.path.basename(path)[:-5]
        d = rd(path).get("model", {})
        if d.get("type") != "minecraft:model":
            specials.setdefault(name, None)
            for ref in json.dumps(d).split('"'):
                if ref.startswith("guhs:item/") and ref != f"guhs:item/{name}":
                    sub_models.add(ref[len("guhs:item/"):])
    for path in glob.glob(os.path.join(item_dir, "*.json")):
        name = os.path.basename(path)[:-5]
        if name in sub_models and name not in specials:
            stale = os.path.join(defs_dir, name + ".json")
            if os.path.exists(stale):
                os.remove(stale)
            continue
        model = specials.get(name)
        if name in specials and model is None:
            continue
        w(os.path.join(defs_dir, name + ".json"), {"model": model or plain(f"guhs:item/{name}")})


# ---------------------------------------------------------------------------------------------------------------------
# Equipment assets (1.21.2): humanoid armour textures
# ---------------------------------------------------------------------------------------------------------------------

ARMOUR = ["vahoege_vads", "duikhelm", "knabbelkroon"]


def equipment():
    old = os.path.join(A, "textures", "models", "armor")
    for name in ARMOUR:
        for layer, kind in (("1", "humanoid"), ("2", "humanoid_leggings")):
            src = os.path.join(old, f"{name}_layer_{layer}.png")
            dst = os.path.join(A, "textures", "entity", "equipment", kind, f"{name}.png")
            if os.path.exists(src):
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                if os.path.exists(dst):
                    os.remove(dst)
                shutil.move(src, dst)
                changed.append(dst)
        w(os.path.join(A, "equipment", f"{name}.json"), {"layers": {
            "humanoid": [{"texture": f"guhs:{name}"}],
            "humanoid_leggings": [{"texture": f"guhs:{name}"}]}})
    if os.path.isdir(old) and not os.listdir(old):
        os.rmdir(old)
        parent = os.path.dirname(old)
        if not os.listdir(parent):
            os.rmdir(parent)


# ---------------------------------------------------------------------------------------------------------------------
# Villager trades (26.1: data-driven; the professions in ModVillagers point at guhs:<profession>/level_<n>)
# ---------------------------------------------------------------------------------------------------------------------

def sell(item, count, emeralds, max_uses, xp):
    return {"wants": {"id": "minecraft:emerald", "count": emeralds}, "gives": {"id": item, "count": count},
            "max_uses": max_uses, "xp": xp, "reputation_discount": 0.05}


def buy(item, count, emeralds, max_uses, xp):
    return {"wants": {"id": item, "count": count}, "gives": {"id": "minecraft:emerald", "count": emeralds},
            "max_uses": max_uses, "xp": xp, "reputation_discount": 0.05}


# KledingBronLijst prices
PRIJS_STRIK, PRIJS_ZONNEBRIL, PRIJS_REGENHOED, PRIJS_OORSTRIKJE, PRIJS_TRUI, PRIJS_REGENJAS = 4, 5, 6, 4, 10, 10

TRADES = {  # 1.0.0 ModVillagers.onTrades, level -> [(name, trade)]
    "vads_temmer": {
        1: [("buy_kaas_knabbels", buy("guhs:kaas_knabbels", 16, 1, 16, 2)), ("sell_guh_spawn_egg", sell("guhs:guh_spawn_egg", 1, 8, 6, 5))],
        2: [("sell_saddle", sell("minecraft:saddle", 1, 6, 6, 10)), ("buy_block_of_kaasknabbels", buy("guhs:block_of_kaasknabbels", 2, 1, 12, 10)),
            ("sell_heiligdom_kompas", sell("guhs:heiligdom_kompas", 1, 12, 3, 10)),
            ("sell_guhmensie_superkompas", sell("guhs:guhmensie_superkompas", 1, 10, 3, 10))],
        3: [("sell_iron_guh_armor", sell("guhs:iron_guh_armor", 1, 12, 3, 15)),
            ("sell_gefrituurde_kaasknabbels", sell("guhs:gefrituurde_kaasknabbels", 4, 3, 12, 10))],
        4: [("sell_diamond_guh_armor", sell("guhs:diamond_guh_armor", 1, 28, 3, 20)), ("sell_guh_spawn_eggs", sell("guhs:guh_spawn_egg", 3, 20, 4, 20))],
        5: [("sell_netherite_guh_armor", sell("guhs:netherite_guh_armor", 1, 48, 2, 30))],
    },
    # 2.9: the kleermaker's everyday set (ModVillagers.kleermakerAanbod; KledingKleermaker keeps his full offer)
    "guh_kleermaker": {
        1: [("sell_red_bowtie", sell("guhs:red_bowtie", 1, PRIJS_STRIK, 12, 5)),
            ("sell_black_bowtie", sell("guhs:black_bowtie", 1, PRIJS_STRIK, 12, 5)),
            ("sell_sunglasses", sell("guhs:sunglasses", 1, PRIJS_ZONNEBRIL, 12, 5)),
            ("sell_rain_hat", sell("guhs:rain_hat", 1, PRIJS_REGENHOED, 12, 10)),
            ("sell_striped_sweater", sell("guhs:striped_sweater", 1, PRIJS_TRUI, 12, 15)),
            ("sell_raincoat", sell("guhs:raincoat", 1, PRIJS_REGENJAS, 12, 15)),
            ("sell_oorstrikje_roze", sell("guhs:oorstrikje_roze", 1, PRIJS_OORSTRIKJE, 12, 5)),
            ("sell_oorstrikje_mint", sell("guhs:oorstrikje_mint", 1, PRIJS_OORSTRIKJE, 12, 5)),
            ("sell_oorstrikje_geel", sell("guhs:oorstrikje_geel", 1, PRIJS_OORSTRIKJE, 12, 5)),
            ("sell_roze_lint", sell("guhs:roze_lint", 1, 3, 12, 5)),
            ("buy_pink_wool", buy("minecraft:pink_wool", 12, 1, 16, 2)),
            ("buy_string", buy("minecraft:string", 14, 1, 16, 2))],
    },
    "vadssmid": {
        1: [("buy_vahoege_vads", buy("guhs:vahoege_vads", 2, 3, 12, 5)), ("sell_vahoege_vads_ingot", sell("guhs:vahoege_vads_ingot", 1, 12, 6, 5))],
        2: [("sell_vahoege_vads_shovel", sell("guhs:vahoege_vads_shovel", 1, 18, 3, 10)),
            ("sell_vahoege_vads_hoe", sell("guhs:vahoege_vads_hoe", 1, 18, 3, 10))],
        3: [("sell_vahoege_vads_pickaxe", sell("guhs:vahoege_vads_pickaxe", 1, 36, 3, 15)),
            ("sell_vahoege_vads_axe", sell("guhs:vahoege_vads_axe", 1, 36, 3, 15)),
            ("sell_vahoege_vads_boots", sell("guhs:vahoege_vads_boots", 1, 30, 3, 15))],
        4: [("sell_vahoege_vads_sword", sell("guhs:vahoege_vads_sword", 1, 30, 3, 20)),
            ("sell_vahoege_vads_helmet", sell("guhs:vahoege_vads_helmet", 1, 36, 3, 20)),
            ("sell_vahoege_vads_leggings", sell("guhs:vahoege_vads_leggings", 1, 48, 3, 20))],
        5: [("sell_vahoege_vads_chestplate", sell("guhs:vahoege_vads_chestplate", 1, 58, 2, 30)),
            ("sell_vahoege_vads_paxel", sell("guhs:vahoege_vads_paxel", 1, 64, 2, 30))],
    },
    "hamsterbouwer": {
        1: [("buy_yellow_dye", buy("minecraft:yellow_dye", 12, 1, 16, 2)), ("sell_yellow_stained_glass", sell("minecraft:yellow_stained_glass", 8, 1, 16, 2))],
        2: [("sell_guh_wire", sell("guhs:guh_wire", 8, 3, 12, 10)), ("buy_redstone", buy("minecraft:redstone", 12, 1, 16, 10))],
        3: [("sell_guh_wheel", sell("guhs:guh_wheel", 1, 10, 4, 15)), ("sell_pink_stained_glass", sell("minecraft:pink_stained_glass", 8, 1, 16, 10))],
        4: [("sell_guh_wire_bundle", sell("guhs:guh_wire", 24, 7, 8, 20)), ("sell_frying_pan", sell("guhs:frying_pan", 1, 8, 4, 20))],
        5: [("sell_guh_spawner", sell("guhs:guh_spawner", 1, 60, 1, 30))],
    },
    "knabbelboer": {
        1: [("buy_kaas_knabbels", buy("guhs:kaas_knabbels", 20, 1, 16, 2)), ("sell_kaasknabbelzaadjes", sell("guhs:kaasknabbelzaadjes", 4, 1, 16, 2))],
        2: [("sell_guh_cupcake", sell("guhs:guh_cupcake", 3, 2, 12, 10)), ("buy_sugar", buy("minecraft:sugar", 16, 1, 16, 10)),
            ("sell_macaron_roze", sell("guhs:macaron_roze", 4, 2, 12, 10)), ("sell_macaron_mint", sell("guhs:macaron_mint", 4, 2, 12, 10))],
        3: [("sell_kaasknabbel_milkshake", sell("guhs:kaasknabbel_milkshake", 1, 3, 8, 15)),
            ("sell_macaron_citroen", sell("guhs:macaron_citroen", 4, 2, 12, 15)), ("sell_macaron_choco", sell("guhs:macaron_choco", 4, 2, 12, 15))],
        4: [("sell_kaasfondue", sell("guhs:kaasfondue", 1, 5, 6, 20)), ("sell_knabbelkorf", sell("guhs:knabbelkorf", 1, 8, 4, 20))],
        5: [("sell_guh_taart", sell("guhs:guh_taart", 1, 12, 4, 30)), ("sell_kaashoning", sell("guhs:kaashoning", 2, 6, 6, 30))],
    },
    "mika_jager": {
        1: [("buy_mika_vet", buy("guhs:mika_vet", 2, 1, 16, 2)), ("sell_arrow", sell("minecraft:arrow", 16, 1, 12, 2))],
        2: [("sell_mika_mepper", sell("guhs:mika_mepper", 1, 12, 3, 10)), ("sell_shield", sell("minecraft:shield", 1, 5, 6, 10))],
        3: [("sell_guhmensie_superkompas", sell("guhs:guhmensie_superkompas", 1, 12, 3, 15)), ("buy_mika_vet_single", buy("guhs:mika_vet", 1, 1, 16, 10))],
        4: [("sell_golden_apple", sell("minecraft:golden_apple", 1, 8, 4, 20))],
        5: [("sell_totem_of_undying", sell("minecraft:totem_of_undying", 1, 40, 1, 30))],
    },
}


def villager_trades():
    for prof, levels in TRADES.items():
        for level in range(1, 6):
            trades = levels.get(level, [])
            ids = []
            for name, trade in trades:
                t = copy.deepcopy(trade)
                for side in ("wants", "gives"):
                    if t[side].get("count") == 1:
                        del t[side]["count"]
                w(os.path.join(D, "villager_trade", prof, str(level), name + ".json"), t)
                ids.append(f"guhs:{prof}/{level}/{name}")
            w(os.path.join(D, "tags", "villager_trade", prof, f"level_{level}.json"), {"values": ids})
            # 1.21.1: the villager picked 2 different offers of the level's list (VillagerTrades#addOffersFromItemListings)
            w(os.path.join(D, "trade_set", prof, f"level_{level}.json"),
              {"trades": f"#guhs:{prof}/level_{level}", "amount": 2, "random_sequence": f"guhs:trade_set/{prof}/level_{level}"})


# ---------------------------------------------------------------------------------------------------------------------
# The data files, per folder
# ---------------------------------------------------------------------------------------------------------------------

def data_files():
    for ns_dir in glob.glob(os.path.join(R, "data", "*")):
        for path in glob.glob(os.path.join(ns_dir, "recipe", "**", "*.json"), recursive=True):
            d = rd(path)
            w(path, recipe(copy.deepcopy(d)))
        for folder in ("loot_table", "loot_modifiers"):
            for path in glob.glob(os.path.join(ns_dir, folder, "**", "*.json"), recursive=True):
                d = rd(path)
                walk_components(d)
                w(path, d)
        for path in glob.glob(os.path.join(ns_dir, "advancement", "**", "*.json"), recursive=True):
            w(path, advancement(rd(path)))
        underwater = set()
        tag = os.path.join(R, "data", "minecraft", "tags", "worldgen", "biome", "plays_underwater_music.json")
        if os.path.exists(tag):
            underwater = set(rd(tag)["values"])
        for path in glob.glob(os.path.join(ns_dir, "worldgen", "biome", "**", "*.json"), recursive=True):
            name = os.path.basename(ns_dir) + ":" + os.path.relpath(path, os.path.join(ns_dir, "worldgen", "biome"))[:-5].replace(os.sep, "/")
            w(path, biome(rd(path), name in underwater))
        for path in glob.glob(os.path.join(ns_dir, "dimension_type", "**", "*.json"), recursive=True):
            w(path, dimension_type(rd(path)))
        random_patches(ns_dir)
        worldgen(ns_dir)


def run():
    del changed[:]
    geckolib_folders()
    data_files()
    tags()
    villager_trades()
    models()
    spawn_eggs()
    item_definitions()
    equipment()
    print(f"mc26: {len(changed)} files written for Minecraft 26.1.2")


if __name__ == "__main__":
    if not os.path.isdir(R):
        sys.exit("run from the project root: python tools/mc26.py")
    run()
