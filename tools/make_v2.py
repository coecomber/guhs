"""
Resources for Guhs 2.0.0 (guh stomach, sled, crystals, bees, deco, ...): JSON, textures, structures and lang.

Run from the project root AFTER make_resources.py (it adds its lang keys to the lang files that script writes):
    python tools/make_resources.py && python tools/make_v2.py
"""
import colorsys
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import make_structures as ms  # noqa: E402
from make_structures import Structure, mc, Byte, Short, Float, Double, floats, compounds, chest  # noqa: E402

R = os.path.join("src", "main", "resources")
A = os.path.join(R, "assets", "guhs")
D = os.path.join(R, "data", "guhs")
TEX = os.path.join(A, "textures")

EN, NL = {}, {}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def lang(key, en, nl):
    EN[key] = en
    NL[key] = nl


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def grid(rows, palette):
    img = Image.new("RGBA", (len(rows[0]), len(rows)))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), palette.get(ch, (0, 0, 0, 0)))
    return img


def noise_tex(base, var, seed, size=16, spots=None):
    """A block texture: base colour with per-pixel noise, plus optional spots [(colour, chance)]."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            c = base
            for colour, chance in spots or []:
                if rng.random() < chance:
                    c = colour
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c[:3]) + (255,))
    return img


def simple_block(name, tex=None, render_type=None):
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": tex or f"guhs:block/{name}"}}
    if render_type:
        model["render_type"] = render_type
    w(f"{A}/models/block/{name}.json", model)
    w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
    w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})


def self_drop(name, item=None):
    w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{item or name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def item_model(name, tex=None, parent="minecraft:item/generated"):
    w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex or f"guhs:item/{name}"}})


def shaped(name, pattern, key, result, count=1):
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                  "key": dict(key),  # 1.21.2+: ingredients are plain "item" / "#tag" strings
                                  "result": {"id": result, "count": count}})


def shapeless(name, ingredients, result, count=1):
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                  "ingredients": list(ingredients),
                                  "result": {"id": result, "count": count}})


def count_fn(lo, hi):
    return [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]


def add_loot(table, entries, rolls=1, chance=None):
    """Adds a pool to one of our chest loot tables (written by make_resources.py)."""
    path = f"{D}/loot_table/chests/{table}.json"
    data = json.load(open(path, encoding="utf-8"))
    pool = {"rolls": rolls, "entries": entries}
    if chance is not None:
        pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    if pool not in data["pools"]:  # safe to run this script twice
        data["pools"].append(pool)
    w(path, data)


def recolour(img, hue=None, sat=None, val=1.0, only=None):
    """HSV recolour of an RGBA image (numpy). only(h, s, v) -> bool mask picks the pixels to change."""
    a = np.asarray(img.convert("RGBA")).astype(np.float32) / 255
    rgb = a[..., :3]
    mx, mn = rgb.max(-1), rgb.min(-1)
    v = mx
    s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) / 6
    mask = np.ones(h.shape, bool) if only is None else only(h, s, v)
    nh = np.where(mask, hue if hue is not None else h, h)
    ns = np.where(mask, np.clip(s * (sat if sat is not None else 1), 0, 1), s)
    nv = np.where(mask, np.clip(v * val, 0, 1), v)
    i = np.floor(nh * 6).astype(int) % 6
    f = nh * 6 - np.floor(nh * 6)
    p, q, t = nv * (1 - ns), nv * (1 - f * ns), nv * (1 - (1 - f) * ns)
    out = np.zeros_like(rgb)
    for k, (x, y, z) in enumerate([(nv, t, p), (q, nv, p), (p, nv, t), (p, q, nv), (t, p, nv), (nv, p, q)]):
        sel = i == k
        out[..., 0][sel], out[..., 1][sel], out[..., 2][sel] = x[sel], y[sel], z[sel]
    res = np.concatenate([out, a[..., 3:]], -1)
    return Image.fromarray((res * 255).astype(np.uint8))


def pinkish(h, s, v):
    return ((h > 0.85) | (h < 0.05)) & (s > 0.12)


# =====================================================================================================================
# Phase 2: the guh stomach (guhmaag dimension) and the questline "De ontvoerde guh"
# =====================================================================================================================
def guhmaag():
    w(f"{D}/dimension_type/guhmaag.json", {
        "ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False,
        "bed_works": False, "has_raids": False, "has_skylight": False, "has_ceiling": True,
        "coordinate_scale": 1.0, "ambient_light": 0.35, "logical_height": 256, "min_y": 0, "height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:the_nether", "fixed_time": 18000,
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0})
    w(f"{D}/dimension/guhmaag.json", {"type": "guhs:guhmaag", "generator": {
        "type": "minecraft:flat", "settings": {"biome": "guhs:guhmaag", "layers": [], "lakes": False,
                                               "features": False, "structure_overrides": []}}})
    w(f"{D}/worldgen/biome/guhmaag.json", {
        "has_precipitation": False, "temperature": 0.9, "downfall": 0.0,
        "effects": {"sky_color": 0x8C2A4A, "fog_color": 0x6A1A30, "water_color": 0xB8E03A, "water_fog_color": 0x9AC020,
                    "ambient_sound": "minecraft:ambient.basalt_deltas.loop",
                    "particle": {"options": {"type": "minecraft:dust", "color": [1.0, 0.55, 0.7], "scale": 1.0},
                                 "probability": 0.004},
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8,
                                   "offset": 2.0}},
        "spawners": {}, "spawn_costs": {}, "carvers": {"air": []}, "features": []})
    lang("biome.guhs.guhmaag", "Guh Stomach", "Guhmaag")

    # blocks (the stomach and mouth; unbreakable)
    save(noise_tex((214, 84, 118), 14, 1, spots=[((236, 120, 150), 0.12), ((170, 52, 88), 0.1)]), "block", "maagwand.png")
    save(noise_tex((150, 40, 64), 10, 2, spots=[((186, 70, 96), 0.2)]), "block", "maagbodem.png")
    tong = noise_tex((226, 104, 128), 10, 3)
    for (x, y) in [(2, 3), (7, 2), (12, 4), (4, 9), (10, 10), (14, 13), (1, 14), (8, 13)]:
        tong.putpixel((x, y), (246, 150, 170, 255))
    save(tong, "block", "tong.png")
    tand = noise_tex((240, 238, 226), 6, 4)
    for i in range(16):
        tand.putpixel((i, 15), (200, 196, 180, 255))
        tand.putpixel((0, i), (214, 210, 196, 255))
    save(tand, "block", "tand.png")
    save(noise_tex((212, 170, 70), 18, 5, spots=[((170, 130, 40), 0.25), ((150, 190, 60), 0.1), ((0, 0, 0), 0.0)]),
         "block", "verteerde_kaasknabbels.png")
    for b in ("maagwand", "maagbodem", "tong", "tand", "verteerde_kaasknabbels"):
        simple_block(b)
    self_drop("verteerde_kaasknabbels")
    # stomach-portal: an animated red/pink swirl
    frames = []
    for f in range(16):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                a = math.atan2(dy, dx) + f * math.pi / 8 + math.hypot(dx, dy) * 0.45
                t = 0.5 + 0.5 * math.sin(a * 3)
                img.putpixel((x, y), (int(170 + 70 * t), int(30 + 60 * t), int(70 + 60 * t), 200))
        frames.append(img)
    strip = Image.new("RGBA", (16, 16 * 16))
    for i, img in enumerate(frames):
        strip.paste(img, (0, 16 * i))
    save(strip, "block", "maag_portal.png")
    with open(os.path.join(TEX, "block", "maag_portal.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 2}}, f)
    w(f"{A}/models/block/maag_portal.json", {"parent": "minecraft:block/nether_portal_ns",
                                               "textures": {"particle": "guhs:block/maag_portal", "portal": "guhs:block/maag_portal"}})
    w(f"{A}/blockstates/maag_portal.json", {"variants": {f"kind={k}": {"model": "guhs:block/maag_portal"}
                                                        for k in ("mond", "darm", "maag", "exit")}})
    # stomach acid (fluid)
    w(f"{A}/blockstates/maagzuur.json", {"variants": {"": {"model": "guhs:block/maagzuur"}}})
    w(f"{A}/models/block/maagzuur.json", {"textures": {"particle": "minecraft:block/water_still"}})
    bucket = Image.open(os.path.join(TEX, "item", "kaas_saus_bucket.png"))
    save(recolour(bucket, hue=0.22, sat=1.2, only=lambda h, s, v: (h > 0.05) & (h < 0.2) & (s > 0.3)), "item", "maagzuur_bucket.png")
    item_model("maagzuur_bucket")

    for key, en, nl in [
        ("block.guhs.maagwand", "Stomach Wall", "Maagwand"), ("block.guhs.maagbodem", "Stomach Floor", "Maagbodem"),
        ("block.guhs.tong", "Tongue", "Tong"), ("block.guhs.tand", "Tooth", "Tand"),
        ("block.guhs.verteerde_kaasknabbels", "Digested Kaasknabbels", "Verteerde kaasknabbels"),
        ("block.guhs.maag_portal", "Stomach Portal", "Maagportaal"),
        ("block.guhs.maagzuur", "Stomach Acid", "Maagzuur"), ("fluid_type.guhs.maagzuur", "Stomach Acid", "Maagzuur"),
        ("item.guhs.maagzuur_bucket", "Bucket of Stomach Acid", "Emmer maagzuur"),
    ]:
        lang(key, en, nl)

    quest_items()
    npc_textures()
    phase2_lang()
    heiligdom()
    mika_kamp()


ITEM_PAL = {
    '.': (0, 0, 0, 0), 'k': (40, 20, 30, 255), 'p': (240, 140, 180, 255), 'P': (255, 190, 215, 255),
    'm': (200, 70, 130, 255), 'w': (255, 255, 255, 255), 'g': (250, 200, 60, 255), 'G': (200, 140, 30, 255),
    'b': (120, 70, 40, 255), 'B': (170, 110, 60, 255), 'c': (170, 235, 255, 255), 'C': (90, 190, 240, 255),
    'r': (220, 50, 60, 255), 'y': (255, 236, 120, 255), 's': (190, 190, 200, 255), 'S': (130, 130, 145, 255),
    'l': (140, 220, 255, 180), 'v': (160, 90, 200, 255), 'o': (250, 160, 60, 255), 'e': (90, 60, 40, 255),
}

ITEMS = {
    "guh_buikfluitje": [
        "................", "................", "................", "......kkkk......", ".....kppppk.....",
        "kkkkkpPPppmk....", "kPPPPppppppmk...", "kpppppp.kppmk...", "kmmmmmmkkppmk...", "kkkkkkmppppmk...",
        ".....kmmmmmk....", "......kkkkk.....", "................", "................", "................",
        "................"],
    "verloren_guh_taart": [
        "................", "................", ".......r........", ".......k........", "....pPpPpPpP....",
        "...pPPPPPPPPp...", "..kyyyyyyyyyyk..", "..kpppppppppk...", "..kPPPPPPPPPPk..", "..kbbbbbbbbbbk..",
        "..kyyyyyyyyyyk..", "..kppppppppppk..", "..kbbbbbbbbbbk..", "...kkkkkkkkkk...", "................",
        "................"],
    "guh_ballon": [
        "......kkkk......", ".....kpPPpk.....", "....kpPwPppk....", "....kpPPpppk....", "....kppppppk....",
        "....kppppppk....", "....kmpppppk....", ".....kmmppk.....", "......kmmk......", ".......kk.......",
        "........k.......", ".......k........", "........k.......", ".......k........", "........k.......",
        "................"],
    "guh_kristal": [
        "................", ".......k........", "......kck.......", "......kcCk......", ".....kcwCk......",
        ".....kcwCCk.....", "....kccwCCk.....", "....kPccCCmk....", "...kPPcCCmmk....", "...kPPPcmmmk....",
        "...kpPPmmmk.....", "....kppmmk......", ".....kpmk.......", "......kk........", "................",
        "................"],
    "guh_kristal_verrekijker": [
        "................", "............kk..", "...........kcck.", "..........kPPck.", ".........kSPPk..",
        "........kSSkk...", ".......kgSSk....", "......kGgGk.....", ".....kgGgk......", "....kGgGk.......",
        "...kgGgk........", "..kSSSk.........", ".kSccSk.........", ".kSccSk.........", "..kkkk..........",
        "................"],
    "guhdex": [
        "................", "..kkkkkkkkkkk...", "..kmpppppppmk...", "..kmpPwPPppmkk..", "..kmpwkPPppmkw..",
        "..kmpPPPPppmkw..", "..kmppppppppkw..", "..kmpyyyyyppkw..", "..kmppppppppkw..", "..kmpgGgGgppkw..",
        "..kmppppppppkw..", "..kmppppppppkw..", "..kmmmmmmmmmkw..", "..kkkkkkkkkkkw..", "....wwwwwwwwww..",
        "................"],
    "sleeglijder": [
        "................", "................", "................", "................", "..kk............",
        ".kssk...........", ".kSsk...........", "..kssk..........", "...kssssssssssk.", "....kSSSSSSSSSk.",
        ".....kkkkkkkkkk.", "................", "................", "................", "................",
        "................"],
    "guh_belletje": [
        "................", "................", ".......kk.......", "......kmmk......", "......kppk......",
        ".....kgyygk.....", "....kgyyyGGk....", "....kgyyyGGk....", "...kgyyyyGGGk...", "...kgyyyyyGGk...",
        "..kGgggggggGGk..", "..kkkkkkkkkkkk..", "......kGGk......", ".......kk.......", "................",
        "................"],
    "roze_lint": [
        "................", "................", "...kk.....kk....", "..kppk...kppk...", "..kpPpk.kpPpk...",
        "..kppPpkpPppk...", "...kppPmPppk....", "....kkmmmkk.....", "....kpkmkpk.....", "...kpk.k.kpk....",
        "...kpk...kpk....", "..kpk.....kpk...", "..kpk.....kpk...", "..kk.......kk...", "................",
        "................"],
    "sleebouwersboek": [
        "................", "..kkkkkkkkkkk...", "..kbcccccccck...", "..kbcwcccwccck..", "..kbcccccccckw..",
        "..kbccskkscckw..", "..kbcsssssscckw.", "..kbcckkkkkcckw.", "..kbcccccccckw..", "..kbccPpPpccckw.",
        "..kbccccccccckw.", "..kbbbbbbbbbkw..", "..kkkkkkkkkkkw..", "....wwwwwwwww...", "................",
        "................"],
}


def quest_items():
    for name, rows in ITEMS.items():
        save(grid(rows, ITEM_PAL), "item", f"{name}.png")
        item_model(name)
    # the two new compasses: recoloured guh cave compass frames
    for compass, hue in (("mika_spoorkompas", 0.0), ("taartkruimels", 0.12)):
        overrides = []
        for i in range(32):
            src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
            img = recolour(src, hue=hue, sat=1.1, val=0.85 if compass == "mika_spoorkompas" else 1.0, only=pinkish)
            frame = f"{compass}_{i:02d}"
            save(img, "item", f"{frame}.png")
            item_model(frame)
            overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
        overrides.append({"predicate": {"angle": 0.984375}, "model": f"guhs:item/{compass}_00"})
        w(f"{A}/models/item/{compass}.json", {"parent": "minecraft:item/generated",
                                               "textures": {"layer0": f"guhs:item/{compass}_16"}, "overrides": overrides})
    # the sled is drawn by make_v2's sled section; a placeholder until then
    for key, en, nl in [
        ("item.guhs.guh_buikfluitje", "Guh Belly Whistle", "Guh-buikfluitje"),
        ("item.guhs.guh_buikfluitje.lore", "Blow it (or press G) to travel to your guh stomach and back",
         "Blaas erop (of druk op G) om naar je guhmaag te reizen en terug"),
        ("item.guhs.mika_spoorkompas", "Mika Trail Compass", "Mika-spoorkompas"),
        ("item.guhs.mika_spoorkompas.lore", "Points to the nearest Mika camp", "Wijst naar het dichtstbijzijnde Mika-kamp"),
        ("item.guhs.taartkruimels", "Cake Crumbs", "Taartkruimels"),
        ("item.guhs.taartkruimels.lore", "A trail of crumbs... to the nearest guh picnic",
         "Een spoor van kruimels... naar de dichtstbijzijnde guh-picknick"),
        ("item.guhs.verloren_guh_taart", "Lost Guh Cake", "Verloren guh-taart"),
        ("item.guhs.verloren_guh_taart.lore", "Mother Vadsig's cake. Don't eat it!", "De taart van Moeder Vadsig. Niet opeten!"),
        ("item.guhs.guh_ballon", "Guh Balloon", "Guh-ballon"),
        ("item.guhs.guh_ballon.lore", "For a guh party", "Voor een guhfeestje"),
        ("item.guhs.guh_kristal", "Guh Crystal", "Guhkristal"),
        ("item.guhs.guh_kristal_verrekijker", "Guh Crystal Spyglass", "Guh-kristalverrekijker"),
        ("item.guhs.guh_kristal_verrekijker.lore", "Use it: rare guhs within 64 blocks start to glow",
         "Gebruik hem: zeldzame guhs binnen 64 blokken gaan gloeien"),
        ("item.guhs.guh_kristal_verrekijker.found", "%s rare guh(s) nearby are glowing!", "%s zeldzame guh(s) in de buurt gloeien op!"),
        ("item.guhs.guhdex", "Guhdex", "Guhdex"),
        ("item.guhs.guhdex.lore", "Every guh you've seen and tamed", "Alle guhs die je hebt gezien en getemd"),
        ("item.guhs.sleeglijder", "Sled Runner", "Sleeglijder"),
        ("item.guhs.sleeglijder.lore", "Part of the broken sled (guh caves)", "Onderdeel van de kapotte slee (guhgrotten)"),
        ("item.guhs.guh_belletje", "Guh Bell", "Guh-belletje"),
        ("item.guhs.guh_belletje.lore", "Part of the broken sled (guh villages)", "Onderdeel van de kapotte slee (guhdorpen)"),
        ("item.guhs.roze_lint", "Pink Ribbon", "Roze lint"),
        ("item.guhs.roze_lint.lore", "Part of the broken sled (kleermaker)", "Onderdeel van de kapotte slee (kleermaker)"),
        ("item.guhs.sleebouwersboek", "Sled Builder's Book", "Sleebouwersboek"),
        ("item.guhs.sleebouwersboek.lore", "Needed (not used up) to craft sled rails",
         "Nodig (niet opgebruikt) om sleerails te maken"),
    ]:
        lang(key, en, nl)
    shaped("guh_ballon", [" W ", " W ", " S "], {"W": "minecraft:pink_wool", "S": "minecraft:string"}, "guhs:guh_ballon", 2)
    shapeless("guhdex", ["minecraft:book", "guhs:kaas_knabbels", "minecraft:pink_dye"], "guhs:guhdex")
    shaped("guh_kristal_verrekijker", ["C", "S"], {"C": "guhs:guh_kristal", "S": "minecraft:spyglass"}, "guhs:guh_kristal_verrekijker")
    # balloons are found at guh picnics too (you need 3 for the party)
    add_loot("guh_picnic", [{"type": "minecraft:item", "name": "guhs:guh_ballon", "functions": count_fn(1, 3)}])


NPC_COLOURS = {  # kind: (hue, saturation factor, value factor) for the pink parts of the sitting guh
    "moeder_vadsig": (0.90, 1.35, 0.95),
    "tandarts": (0.47, 0.55, 1.08),
    "maagenzym": (0.22, 1.1, 0.95),
    "slee_guh": (0.57, 0.7, 1.05),
}


def npc_textures():
    src = Image.open(os.path.join(TEX, "entity", "guh_sitting.png"))
    for kind, (hue, sat, val) in NPC_COLOURS.items():
        save(recolour(src, hue=hue, sat=sat, val=val, only=pinkish), "entity", f"npc_{kind}.png")
    lang("entity.guhs.guh_npc", "Guh", "Guh")
    lang("entity.guhs.guh_npc.moeder_vadsig", "Mother Vadsig", "Moeder Vadsig")
    lang("entity.guhs.guh_npc.tandarts", "Dentist Guh", "Tandarts-guh")
    lang("entity.guhs.guh_npc.maagenzym", "Stomach Enzyme Guh", "Maagenzym-guh")
    lang("entity.guhs.guh_npc.slee_guh", "Sled Guh", "Slee-guh")
    lang("entity.guhs.mika_baas", "Mika Boss", "Mika-baas")


VARIANT_DEX = {  # id: (rarity en, rarity nl, info en, info nl)
    "normal": ("Common", "Gewoon", "The good old guh. Found everywhere in the Guhmension and in guh caves.",
               "De goede oude guh. Overal in de Guhmensie en in guhgrotten."),
    "mint": ("Uncommon", "Ongewoon", "A fresh mint-green guh. Smells like toothpaste.",
             "Een frisse mintgroene guh. Ruikt naar tandpasta."),
    "choco": ("Uncommon", "Ongewoon", "Chocolate brown and extra snuggly.", "Chocoladebruin en extra knuffelig."),
    "snow": ("Uncommon", "Ongewoon", "A white guh that loves the cold peaks.", "Een witte guh die van de koude toppen houdt."),
    "brontosaurus": ("Rare", "Zeldzaam", "A guh with a very long neck. Guh-saurus!", "Een guh met een heel lange nek. Guh-saurus!"),
    "teckel": ("Rare", "Zeldzaam", "A sausage guh with six legs. Very long, very happy.",
               "Een worstguh met zes pootjes. Heel lang, heel blij."),
    "ghost": ("Rare (night only)", "Zeldzaam (alleen 's nachts)", "A see-through guh that only shows up at night. Boo-guh!",
              "Een doorzichtige guh die alleen 's nachts verschijnt. Boe-guh!"),
    "starry": ("Very rare", "Heel zeldzaam", "Its fur is a night sky full of glowing stars.",
               "Zijn vacht is een nachthemel vol gloeiende sterren."),
    "rainbow": ("Very rare", "Heel zeldzaam", "Changes colour all the time. Nobody knows how.",
                "Verandert steeds van kleur. Niemand weet hoe."),
    "ender": ("Rare (Guh Peaks only)", "Zeldzaam (alleen op de Guhpieken)",
              "Enderguh: black and purple, with dragon wings. Tame it (1 in 8 knabbels, or 1 fried knabbel), saddle it and fly!",
              "Enderguh: zwart-paars met drakenvleugels. Tem hem (1 op 8 knabbels, of 1 gefrituurde knabbel), zadel hem en vlieg!"),
    "koning": ("Unique (guh castle)", "Uniek (guhkasteel)",
               "The Koningguh on his throne in the legendary guh castle. Royal purple, a white mane, a moustache. Tame him with kaas knabbels for his outfit!",
               "De Koningguh op zijn troon in het legendarische guhkasteel. Koningspaars, witte manen, een snorretje. Tem hem met kaasknabbels voor zijn pakje!"),
    "reisguh": ("Common (by every portal and in big places)", "Gewoon (bij elk portaal en in grote plekken)",
                "The Reisguh: a guh conductor. Right-click to discover it; after that you travel between all Reisguhs you know. "
                "One sits by every guh portal, at the guh castle and in the big places: Guhwarden, Knuffeldal, the Guhkermis, "
                "Guhland, the Ballonfestival and the Guhcircuit.",
                "De Reisguh: een guh-conducteur. Rechtsklik om hem te ontdekken; daarna reis je tussen alle Reisguhs die je kent. "
                "Er zit er een bij elk guhportaal, bij het guhkasteel en in de grote plekken: Guhwarden, Knuffeldal, de Guhkermis, "
                "Guhland, het Ballonfestival en het Guhcircuit."),
    "poortwachter": ("Unique (guh castle)", "Uniek (guhkasteel)",
                     "The gate guards of the guh castle. They only let friends of the guhs in...",
                     "De poortwachters van het guhkasteel. Ze laten alleen guhvrienden binnen..."),
    # the guh characters of the 2.4 minigames and rare places (one in each building)
    "showguh": ("Rare (beauty theatre)", "Zeldzaam (beautytheater)",
                "The Showguh runs the Vads Contest: dress a model guh for the theme and let the jury judge. Rosettes buy the Showster set.",
                "De Showguh presenteert de Vads-wedstrijd: kleed een modelguh aan voor het thema en laat de jury oordelen. Met rozetten koop je het Showster-pakje."),
    "raceguh": ("Rare (race track)", "Zeldzaam (racebaan)",
                "The Raceguh lends you a race guh: three laps through the rings, VAHOEG pads and your own ghost guh.",
                "De Raceguh leent je een raceguh: drie rondjes door de ringen, over VAHOEG-platen en tegen je eigen spookguh."),
    "mepguh": ("Rare (whack hall)", "Zeldzaam (mephal)",
               "The Mepguh hands you a mepper: whack the Mikas on the 4x4 board, but never the guh!",
               "De Mepguh geeft je een mepper: mep de Mika's op het 4x4-bord, maar nooit de guh!"),
    "djguh": ("Rare (guh disco)", "Zeldzaam (guhdisco)",
              "The DJ-guh plays the colours: dance them back in the right order. One wrong step and the music stops.",
              "De DJ-guh speelt de kleuren voor: dans ze in de goede volgorde na. Eén verkeerde stap en de muziek stopt."),
    "golfguh": ("Rare (golf course)", "Zeldzaam (golfbaan)",
                "The Golfguh lends you a club for 9 holes of guh minigolf. Hold right-click, let go, VAHOEG!",
                "De Golfguh leent je een club voor 9 holes guhminigolf. Rechtsklik vasthouden, loslaten, VAHOEG!"),
    "smulguh": ("Rare (food festival)", "Zeldzaam (eetfestijn)",
                "The Smulguh lends you her big bowl: catch the falling food for a minute, and stay away from Mika-vet.",
                "De Smulguh leent je haar grote schaal: vang een minuut lang het vallende eten, en blijf weg van Mika-vet."),
    "visguh": ("Rare (fishing pond)", "Zeldzaam (visvijver)",
               "The Visguh lends you a rod for the fishing contest. The special guh fish are yours to keep!",
               "De Visguh leent je een hengel voor de viswedstrijd. De bijzondere guhvissen mag je houden!"),
    "mijnguh": ("Very rare (cheese mine)", "Heel zeldzaam (kaasmijn)",
                "The Mijnguh lends you a pickaxe for the cheese veins, which are found nowhere else.",
                "De Mijnguh leent je een houweel voor de kaasaders, die je nergens anders vindt."),
    "bibliothecaris": ("Very rare (guh library)", "Heel zeldzaam (guhbibliotheek)",
                       "The librarian of the guh library knows every guh story. Read them all on the lecterns!",
                       "De Bibliothecaris van de guhbibliotheek kent alle guhverhalen. Lees ze allemaal op de lessenaars!"),
    "zeemeerguh": ("Rare (guh seas)", "Zeldzaam (Guhzee, Diepe Guhzee)",
                   "The Zeemeerguh: a guh with a fish tail! Swims super fast. Tame it, saddle it and ride it under water.",
                   "De Zeemeerguh: een guh met een vissenstaart! Zwemt supersnel. Tem hem, zadel hem en rijd onder water."),
    "golden": ("Legendary", "Legendarisch", "A shiny golden guh. Extremely rare!", "Een glimmende gouden guh. Extreem zeldzaam!"),
    "brococolief": ("Unique", "Uniek", "The guh of the guh village. Wears pink, of course.",
                    "De guh van het guhdorp. Draagt natuurlijk roze."),
}


def phase2_lang():
    for v, (ren, rnl, ien, inl) in VARIANT_DEX.items():
        lang(f"gui.guhs.guhdex.rarity.{v}", "Rarity: " + ren, "Zeldzaamheid: " + rnl)
        lang(f"gui.guhs.guhdex.info.{v}", ien, inl)
    for key, en, nl in [
        ("key.categories.guhs", "Guhs", "Guhs"),
        ("key.category.guhs.guhs", "Guhs", "Guhs"),  # 1.1.0 (26.1): KeyMapping.Category guhs:guhs
        ("key.guhs.maag", "Guh stomach (there and back)", "Guhmaag (heen en terug)"),
        # stomach
        ("gui.guhs.maag.locked", "You don't have a guh stomach yet... (Mother Vadsig's quest)",
         "Je hebt nog geen guhmaag... (quest van Moeder Vadsig)"),
        ("gui.guhs.maag.private", "The stomach of %s is private", "De maag van %s is privé"),
        ("gui.guhs.maag.no_build", "You may not build in this stomach", "Je mag niet bouwen in deze maag"),
        ("gui.guhs.maag.settings", "Stomach settings", "Maaginstellingen"),
        ("gui.guhs.maag.access.public", "Public", "Openbaar"),
        ("gui.guhs.maag.access.private", "Private", "Privé"),
        ("gui.guhs.maag.access.whitelist", "Whitelist", "Whitelist"),
        ("gui.guhs.maag.build_on", "Build: yes", "Bouwen: ja"),
        ("gui.guhs.maag.build_off", "Build: no", "Bouwen: nee"),
        ("gui.guhs.maag.add_name", "Player name", "Spelernaam"),
        ("gui.guhs.maag.add", "Add", "Toevoegen"),
        ("gui.guhs.maag.size", "Your stomach: %s x %s blocks", "Jouw maag: %s x %s blokken"),
        ("gui.guhs.maag.grow", "Bigger (%s x %s)? Do the Dentist Guh's jobs in the mouth.",
         "Groter (%s x %s)? Doe de klusjes van de Tandarts-guh in de mond."),
        ("gui.guhs.maag.max", "Your stomach is as big as it gets!", "Je maag is zo groot als hij kan worden!"),
        ("gui.guhs.maag.whitelist_empty", "Nobody on the whitelist yet", "Nog niemand op de whitelist"),
        ("gui.guhs.maag.whitelist_off", "(the whitelist only counts in whitelist mode)", "(de whitelist telt alleen in whitelist-stand)"),
        ("gui.guhs.maag.unknown_player", "Unknown player: %s", "Onbekende speler: %s"),
        ("sign.guhs.maag.to_mouth", "to the mouth", "naar de mond"),
        ("sign.guhs.maag.to_intestines", "to the intestines", "naar de darmen"),
        ("sign.guhs.maag.to_world", "back to the world", "terug naar de wereld"),
        ("sign.guhs.maag.lobby1", "Welcome in", "Welkom in"),
        ("sign.guhs.maag.lobby2", "the guh mouth!", "de guhmond!"),
        # Moeder Vadsig
        ("quest.guhs.vadsig.start", "Guh... guhhh... my little Guhbert has been kidnapped by the Mikas! Follow this compass to their camp. Please, bring him back!",
         "Guh... guhhh... mijn kleine Guhbert is ontvoerd door de Mika's! Volg dit kompas naar hun kamp. Alsjeblieft, breng hem terug!"),
        ("quest.guhs.vadsig.camp", "The compass points to the Mika camp. Beat the Mika Boss and free Guhbert!",
         "Het kompas wijst naar het Mika-kamp. Versla de Mika-baas en bevrijd Guhbert!"),
        ("quest.guhs.vadsig.cake", "Guhbert is back! But... my cake for the welcome party is gone. Follow the crumbs to the guh picnic!",
         "Guhbert is terug! Maar... mijn taart voor het welkomstfeest is weg. Volg de kruimels naar de guh-picknick!"),
        ("quest.guhs.vadsig.need", "For the party I need the cake (%s) and %s/%s guh balloons.",
         "Voor het feest heb ik de taart (%s) en %s/%s guh-ballonnen nodig."),
        ("quest.guhs.have", "found", "gevonden"),
        ("quest.guhs.missing", "still missing", "nog kwijt"),
        ("quest.guhs.vadsig.party", "PARTY! Thank you, thank you! Here, my belly whistle. From now on you have your own guh stomach!",
         "FEEST! Dankjewel, dankjewel! Hier, mijn buikfluitje. Vanaf nu heb je je eigen guhmaag!"),
        ("quest.guhs.vadsig.unlocked", "Guh stomach unlocked! Blow the belly whistle or press G.",
         "Guhmaag ontgrendeld! Blaas op het buikfluitje of druk op G."),
        ("quest.guhs.vadsig.done", "Guh guh! Guhbert and I are so happy. Enjoy your stomach!",
         "Guh guh! Guhbert en ik zijn zo blij. Veel plezier met je maag!"),
        ("quest.guhs.cake.found", "You found the lost cake at the picnic! Bring it to Mother Vadsig.",
         "Je hebt de verloren taart gevonden bij de picknick! Breng hem naar Moeder Vadsig."),
        # Mika-baas
        ("quest.guhs.mika.challenge", "NJEG! Want the little guh? Beat me at rock-paper-scissors-VADS. Three times in a row!",
         "NJEG! Wil je de kleine guh? Versla me met steen-papier-schaar-VADS. Drie keer op rij!"),
        ("quest.guhs.mika.play", "NJEG! Again? Fine.", "NJEG! Nog een keer? Prima."),
        ("quest.guhs.mika.won", "NJEG HEHE! I picked %s. Back to zero!", "NJEG HEHE! Ik koos %s. Terug naar nul!"),
        ("quest.guhs.mika.again", "Grrr... again!", "Grrr... nog een keer!"),
        ("quest.guhs.mika.vadsed", "NJEG... I'VE BEEN VADSED!", "NJEG... IK BEN GEVADST!"),
        ("quest.guhs.guhbert.free", "Guh! Guh guh! (Guhbert is free and follows you)", "Guh! Guh guh! (Guhbert is vrij en volgt jou)"),
        ("gui.guhs.rps.title", "Rock-Paper-Scissors-VADS", "Steen-Papier-Schaar-VADS"),
        ("gui.guhs.rps.rules", "Win 3 times in a row. VADS beats everything...", "Win 3 keer op rij. VADS wint van alles..."),
        ("gui.guhs.rps.steen", "Rock", "Steen"),
        ("gui.guhs.rps.papier", "Paper", "Papier"),
        ("gui.guhs.rps.schaar", "Scissors", "Schaar"),
        ("gui.guhs.rps.vads", "VADS", "VADS"),
        ("gui.guhs.rps.win", "You win this round!", "Jij wint deze ronde!"),
        ("gui.guhs.rps.lose", "Mika picked %s... you lose!", "Mika koos %s... verloren!"),
        # Tandarts / enzyme
        ("quest.guhs.dentist.no_maag", "Open wide! Oh, you don't have a stomach yet. Ask Mother Vadsig.",
         "Wijd open! O, je hebt nog geen maag. Vraag het Moeder Vadsig."),
        ("quest.guhs.dentist.max", "Your stomach is already %s wide. Bigger isn't healthy!", "Je maag is al %s breed. Groter is niet gezond!"),
        ("quest.guhs.dentist.job", "Your stomach is %s wide. For %s, bring me:", "Je maag is %s breed. Voor %s moet je me dit brengen:"),
        ("quest.guhs.need.knabbels", "%s/%s kaas knabbels", "%s/%s kaas knabbels"),
        ("quest.guhs.need.ingots", "%s/%s vahoege vads ingots", "%s/%s vahoege-vadsstaven"),
        ("quest.guhs.need.crystals", "%s/%s guh crystals", "%s/%s guhkristallen"),
        ("quest.guhs.need.big_mika", "  - defeat a Big Mika", "  - versla een Grote Mika"),
        ("quest.guhs.need.golden", "  - bring a golden guh along", "  - neem een gouden guh mee"),
        ("quest.guhs.dentist.done", "Stretch... stretch... done! Your stomach is now %s wide.",
         "Rekken... rekken... klaar! Je maag is nu %s breed."),
        ("quest.guhs.enzyme.not_yours", "Blub. I only listen to %s.", "Blub. Ik luister alleen naar %s."),
        # Guhdex
        ("gui.guhs.guhdex.title", "Guhdex", "Guhdex"),
        ("gui.guhs.guhdex.tamed", "Tamed", "Getemd"),
        ("gui.guhs.guhdex.unknown", "Not seen yet", "Nog niet gezien"),
        ("gui.guhs.guhdex.rewards", "Rewards", "Beloningen"),
        ("gui.guhs.guhdex.milestone", "%s seen: %s", "%s gezien: %s"),
        ("gui.guhs.guhdex.milestone_tamed", "%s seen + %s tamed: %s", "%s gezien + %s getemd: %s"),
        ("gui.guhs.guhdex.claim", "Claim", "Ophalen"),
        ("gui.guhs.guhdex.claimed", "Claimed", "Opgehaald"),
        ("gui.guhs.guhdex.new", "New in your Guhdex: %s", "Nieuw in je Guhdex: %s"),
        ("gui.guhs.guhdex.welcome", "Welcome to the Guhmension! Here's a Guhdex: fill it with every guh you meet. Njeg!",
         "Welkom in de Guhmensie! Hier, een Guhdex: vul hem met alle guhs die je tegenkomt. Njeg!"),
        # Guhdex: the Highscores tab
        ("gui.guhs.highscores.tab", "Highscores", "Highscores"),
        ("gui.guhs.highscores.to_dex", "Back to the guhs", "Terug naar de guhs"),
        ("gui.guhs.highscores.never", "Never played, njeg!", "Nog nooit gespeeld, njeg!"),
        ("gui.guhs.highscores.record", "Server record: %s by %s", "Serverrecord: %s door %s"),
        ("gui.guhs.highscores.record_you", "Server record: %s - that's YOU! VAHOEG!", "Serverrecord: %s - dat ben JIJ! VAHOEG!"),
        ("gui.guhs.highscores.no_record", "No server record yet: grab it, vahoeg!", "Nog geen serverrecord: pak hem, vahoeg!"),
        ("gui.guhs.highscores.lower", "(faster/fewer is better)", "(sneller/minder is beter)"),
        ("gui.guhs.highscores.higher", "(more is better)", "(meer is beter)"),
        ("gui.guhs.highscores.game.beauty", "Vads Contest", "Vads-wedstrijd"),
        ("gui.guhs.highscores.game.race", "Guhrace", "Guhrace"),
        ("gui.guhs.highscores.game.race_lap", "Guhrace: fastest lap", "Guhrace: snelste ronde"),
        ("gui.guhs.highscores.game.meppen", "Mika meppen", "Mika meppen"),
        ("gui.guhs.highscores.game.disco", "Guhdisco", "Guhdisco"),
        ("gui.guhs.highscores.game.golf", "Guh golf (9 holes)", "Guhgolf (9 holes)"),
        ("gui.guhs.highscores.game.smul", "Vadsig eetfestijn", "Vadsig eetfestijn"),
        ("gui.guhs.highscores.game.vissen", "Guhfish contest", "Guhvis-wedstrijd"),
        ("gui.guhs.highscores.game.vissen_zwaarste", "Heaviest fish", "Zwaarste vis"),
        ("gui.guhs.highscores.game.verstop_makkelijk", "Verstopguh: easy", "Verstopguh: makkelijk"),
        ("gui.guhs.highscores.game.verstop_medium", "Verstopguh: medium", "Verstopguh: medium"),
        ("gui.guhs.highscores.game.verstop_moeilijk", "Verstopguh: hard", "Verstopguh: moeilijk"),
        # sled quest
        ("quest.guhs.sled.start", "Guh... my sled is broken! I need a sled runner (guh caves), a guh bell (guh villages) and a pink ribbon (the kleermaker).",
         "Guh... mijn slee is kapot! Ik heb een sleeglijder (guhgrotten), een guh-belletje (guhdorpen) en een roze lint (de kleermaker) nodig."),
        ("quest.guhs.sled.parts", "Still missing parts for the sled:", "Nog missende onderdelen voor de slee:"),
        ("quest.guhs.sled.thanks", "Wheee! It works again! Take this sled and my sled builder's book - build your own track!",
         "Wiee! Hij doet het weer! Neem deze slee en mijn sleebouwersboek - bouw je eigen baan!"),
        ("quest.guhs.sled.unlocked", "You can now craft sled rails (the book stays in the grid).",
         "Je kunt nu sleerails maken (het boek blijft in het rooster)."),
        ("quest.guhs.sled.done", "Ding ding! Have fun on the rails!", "Tingeling! Veel plezier op de rails!"),
    ]:
        lang(key, en, nl)


def heiligdom():
    """Vadsig-heiligdom: a round pink shrine on pillars, Mother Vadsig sitting on a huge cushion in the middle."""
    W = 21
    s = Structure((W, 14, W))
    c = W // 2
    fp = []
    for x in range(W):
        for z in range(W):
            d = math.dist((x, z), (c, c))
            if d <= 10.2:
                fp.append((x, z))
                s.set(x, 0, z, mc("pink_concrete") if d > 8.6 else mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
                if 9.2 < d <= 10.2:
                    s.set(x, 0, z, mc("magenta_glazed_terracotta"), {"facing": "north"})
    for i in range(8):                                   # pillars with kaasknabbel blocks on top
        a = i * math.pi / 4
        x, z = c + round(8 * math.cos(a)), c + round(8 * math.sin(a))
        s.fill(x, 1, z, x, 6, z, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, 7, z, "guhs:block_of_kaasknabbels")
    for x in range(W):                                   # a pink dome of stained glass
        for z in range(W):
            for y in range(7, 14):
                d = math.dist((x, y * 1.3 - 7 * 1.3, z), (c, 0, c))
                if 8.6 <= d <= 9.4:
                    s.set(x, y, z, mc("pink_stained_glass"))
    for x in range(W):                                   # the big cushion
        for z in range(W):
            d = math.dist((x, z), (c, c))
            if d <= 3.6:
                s.set(x, 1, z, mc("magenta_wool"))
            if d <= 2.6:
                s.set(x, 2, z, mc("pink_wool"))
            if 3.6 < d <= 4.2:
                s.set(x, 1, z, mc("magenta_carpet"))
    for (x, z) in ((c - 6, c + 6), (c + 6, c + 6), (c - 6, c - 6), (c + 6, c - 6)):
        s.set(x, 1, z, mc("potted_pink_tulip"))
    s.set(c - 2, 1, c + 6, "guhs:block_of_kaasknabbels")
    s.set(c + 2, 1, c + 6, mc("cake"))
    s.entity(c + 0.5, 3.0, c + 0.5, {"id": "guhs:guh_npc", "Kind": "moeder_vadsig", "PersistenceRequired": Byte(1),
                                      "Rotation": floats(0.0, 0.0)})
    s.clear_above(fp, 1)
    s.save("vadsig_heiligdom")


def mika_kamp():
    """Mika camp: black tents, a soul campfire, and Guhbert in a cage. The Mika Boss guards it."""
    W = 23
    s = Structure((W, 10, W))
    rng = random.Random(7)
    c = W // 2
    fp = []
    for x in range(W):
        for z in range(W):
            if math.dist((x, z), (c, c)) <= 11:
                fp.append((x, z))
                s.set(x, 0, z, mc(rng.choice(["blackstone", "coarse_dirt", "soul_soil", "blackstone", "crying_obsidian"])
                                  if rng.random() < 0.8 else "gravel"))
    s.set(c, 1, c, mc("soul_campfire"), {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        s.set(c + dx, 1, c + dz, mc("polished_blackstone_slab"), {"type": "bottom", "waterlogged": "false"})

    def tent(x0, z0):
        for i in range(4):
            s.fill(x0 + i, 1 + i, z0, x0 + i, 1 + i, z0 + 4, mc("black_wool"))
            s.fill(x0 + 6 - i, 1 + i, z0, x0 + 6 - i, 1 + i, z0 + 4, mc("black_wool"))
        s.fill(x0 + 3, 4, z0, x0 + 3, 4, z0 + 4, mc("red_wool"))
        s.set(x0 + 3, 1, z0 + 2, mc("red_bed"), {"facing": "south", "part": "foot", "occupied": "false"})
    tent(2, 2)
    tent(14, 3)
    # Guhbert's cage
    cx, cz = c - 1, c + 5
    for x in range(cx - 1, cx + 3):
        for z in range(cz - 1, cz + 3):
            s.set(x, 4, z, mc("polished_blackstone"))
            for y in range(1, 4):
                if x in (cx - 1, cx + 2) or z in (cz - 1, cz + 2):
                    s.set(x, y, z, mc("iron_bars"), {"east": "true", "west": "true", "north": "true", "south": "true",
                                                     "waterlogged": "false"})
    s.set(cx + 1, 1, cz + 1, mc("magenta_carpet"))
    s.entity(cx + 1.0, 1.0, cz + 1.0, ms.guh_nbt(0.6, CustomName='{"text":"Guhbert"}', NoAI=Byte(1),
                                                 Invulnerable=Byte(1), Age=-1000000000,
                                                 Tags=ms.NbtList(8, ["guhs_caged_guhbert"])))
    # the Mika Boss next to the fire, two normal Mikas around
    s.entity(c + 3.5, 1.0, c + 0.5, {"id": "guhs:mika_baas", "PersistenceRequired": Byte(1), "Rotation": floats(90.0, 0.0)})
    for (x, z) in ((c - 5, c - 2), (c + 5, c - 5)):
        s.entity(x + 0.5, 1.0, z + 0.5, ms.mika_nbt())
    chest(s, 17, 1, 12, "west", "guhs:chests/evil_mika_home")
    for (x, z) in ((c - 7, c + 7), (c + 7, c + 7), (c, c - 9)):
        s.fill(x, 1, z, x, 2, z, mc("polished_blackstone_wall"), {"up": "true", "north": "none", "south": "none",
                                                               "east": "none", "west": "none", "waterlogged": "false"})
        s.set(x, 3, z, mc("soul_lantern"), {"hanging": "false", "waterlogged": "false"})
    s.clear_above(fp, 1)
    s.save("mika_kamp")


TEMPLATE_SIZES = {  # largest side of each surface structure's template (for the flatness check)
    "hamster_house": 32, "hamster_house_medium": 46, "hamster_house_large": 72, "hamster_house_extra_extra_large": 128,
    "evil_mika_home": 22, "guh_picnic": 13, "cheese_fountain": 17, "grand_cheese_fountain": 31, "guh_statue": 50,
    "guhramid": 41, "guh_village": 64, "mini_picnic": 5, "quartz_statue": 4, "block_guh": 8, "guh_fossil": 18,
    "giant_kaasknabbel": 25, "giant_cake": 17, "kaasknabbel_arch": 41, "vadsig_heiligdom": 21, "mika_kamp": 23, "sleehut": 15}


KEEP_CLEAR = {"guh_kasteel": 136}  # (a first keep_clear; bouwruimte() at the end sets the real reach of every structure)
FLATNESS = {"sleehut": 18, "hamster_house_extra_extra_large": 40}  # (the sled hut is on the steep Guh Peaks)


def structure(name, biomes, spacing, separation, salt, spawn_overrides=None, start_y=0, reach=80, centre=None):
    jigsaw = {"type": "minecraft:jigsaw", "biomes": f"#guhs:has_structure/{name}", "step": "surface_structures",
              "spawn_overrides": spawn_overrides or {}, "terrain_adaptation": "beard_box",
              "start_pool": f"guhs:{name}/start", "size": 1, "start_height": {"absolute": start_y},
              "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": reach, "use_expansion_hack": False}
    if centre:
        # the piece is placed with this jigsaw block (in its middle) on the start: pieces only reach 8 chunks from there
        jigsaw["start_jigsaw_name"] = centre
    if reach > 116:
        # vanilla allows at most 128 including the terrain adaptation: the blending comes from the outer structure below
        jigsaw["terrain_adaptation"] = "none"
    # only on flat enough ground (guhs:flat_jigsaw samples the surface around the start): no half mountains, no floating
    radius = TEMPLATE_SIZES.get(name, 16)
    w(f"{D}/worldgen/structure/{name}.json", {
        "type": "guhs:flat_jigsaw", "biomes": jigsaw["biomes"], "step": jigsaw["step"], "spawn_overrides": jigsaw["spawn_overrides"],
        "terrain_adaptation": "beard_box", "check_radius": radius, "max_height_difference": FLATNESS.get(name, 6 + radius // 4),
        "keep_clear": KEEP_CLEAR.get(name, (radius + 1) // 2),
        "jigsaw": jigsaw})
    w(f"{D}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{name}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    w(f"{D}/worldgen/structure_set/{name}.json", {
        "structures": [{"structure": f"guhs:{name}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    w(f"{D}/tags/worldgen/biome/has_structure/{name}.json",
      {"values": [b if ":" in b else f"guhs:{b}" for b in biomes]})


GUHMENSION_LAND = ["guh_fields", "knabbel_crumbs", "pink_puffs", "kaas_flats", "guh_meadows"]


def phase2_structures():
    structure("vadsig_heiligdom", GUHMENSION_LAND, spacing=56, separation=20, salt=20200001)
    structure("mika_kamp", GUHMENSION_LAND + ["mikas_biome"], spacing=24, separation=8, salt=20200002,
              spawn_overrides={"monster": {"bounding_box": "piece", "spawns": [
                  {"type": "guhs:mika", "weight": 1, "minCount": 1, "maxCount": 1}]}})


# =====================================================================================================================
# Phase 3: the guh sled and its rails
# =====================================================================================================================
SWATCH = {  # 8x8 swatches in a 64x64 texture: index -> colour
    0: (238, 141, 173), 1: (200, 90, 130), 2: (250, 246, 240), 3: (190, 196, 210), 4: (250, 200, 60),
    5: (190, 140, 90), 6: (220, 50, 70), 7: (255, 205, 222), 8: (130, 90, 60), 9: (60, 40, 50),
}


def swatch_uv(i):
    return [(i % 8) * 8 + 1, (i // 8) * 8 + 1]


def geo_cube(origin, size, colour, pivot=None, rotation=None, faces=None):
    faces = faces or {}
    uv = {f: {"uv": swatch_uv(faces.get(f, colour)), "uv_size": [6, 6]} for f in ("north", "south", "east", "west", "up", "down")}
    cube = {"origin": origin, "size": size, "uv": uv}
    if rotation:
        cube["pivot"], cube["rotation"] = pivot, rotation
    return cube


def swatch_texture(path, swatches, seed=1, var=10):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i, c in swatches.items():
        for y in range(8):
            for x in range(8):
                v = rng.randint(-var, var)
                img.putpixel(((i % 8) * 8 + x, (i // 8) * 8 + y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    save(img, *path)


def sled_model():
    """The guh sled (front = -z): silver runners curling up at the front, a pink deck, a backrest with guh ears."""
    cubes = []
    for x in (-7, 6):                                                    # runners with a curl
        cubes.append(geo_cube([x, 0, -11], [1, 1, 21], 3))
        cubes.append(geo_cube([x, 1, -13], [1, 4, 1], 3))
        cubes.append(geo_cube([x, 5, -13], [1, 1, 3], 3))
        cubes.append(geo_cube([x, 0, -12], [1, 2, 1], 3))
        for z in (-8, -1, 6):                                           # struts
            cubes.append(geo_cube([x, 1, z], [1, 2, 1], 8))
    cubes.append(geo_cube([-7, 3, -10], [14, 1, 19], 0, faces={"up": 7}))  # deck
    for x in (-8, 7):                                                    # side rails
        cubes.append(geo_cube([x, 3, -10], [1, 3, 19], 1))
    cubes.append(geo_cube([-8, 3, -11], [16, 3, 1], 1))                  # front board
    cubes.append(geo_cube([-7, 4, 8], [14, 8, 1], 2, faces={"south": 0, "north": 7}))  # backrest
    cubes.append(geo_cube([-7, 12, 8], [14, 1, 1], 0))
    for x in (-6, 3):                                                    # guh ears
        cubes.append(geo_cube([x, 13, 8], [3, 3, 1], 0, faces={"north": 7}))
    cubes.append(geo_cube([-1, 3, -13], [2, 2, 2], 4))                   # the bell
    cubes.append(geo_cube([-4, 5, -12], [8, 1, 1], 6))                   # ribbon on the front
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.guh_slee", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 3, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": [{"name": "sled", "pivot": [0, 0, 0], "cubes": cubes}]}]}
    w(f"{A}/geckolib/models/entity/guh_slee.geo.json", geo)
    w(f"{A}/geckolib/animations/entity/guh_slee.animation.json", {"format_version": "1.8.0", "animations": {}})
    swatch_texture(("entity", "guh_slee.png"), SWATCH, seed=3, var=8)


RAIL_ICONS = {
    "guh_slee": [
        "................", "................", "................", "............kk..", "...........kppk.",
        "...........kwpk.", ".kk.......kwppk.", "kppkkkkkkkkppk..", "kPPPPPPPPPPPpk..", "kmmmmmmmmmmmmk..",
        ".kkkkkkkkkkkk...", "..ks..ks..ks....", "kssssssssssssk..", ".kSSSSSSSSSSSSk.", "..kkkkkkkkkkkk..",
        "................"],
    "sleerail_recht": [
        "................", "..kp........kp..", ".kBBBBBBBBBBBBk.", "..kw........kw..", "..kp........kp..",
        ".kBBBBBBBBBBBBk.", "..kw........kw..", "..kp........kp..", ".kBBBBBBBBBBBBk.", "..kw........kw..",
        "..kp........kp..", ".kBBBBBBBBBBBBk.", "..kw........kw..", "..kp........kp..", "................",
        "................"],
    "sleerail_bocht": [
        "................", "................", "...........kpk..", "..........kBwk..", ".........kBBpk..",
        "........kpkBw...", ".......kwk.Bp...", "......kpk..kBk..", ".....kwk..kwkk..", "....kpkBBkpk....",
        "...kwkBBkwk.....", "..kpkBkpk.......", ".kwkkwk.........", ".kpwpk..........", "..kk............",
        "................"],
    "sleerail_helling": [
        "................", "................", "............kpk.", "...........kwBk.", "..........kpBk..",
        ".........kwBk...", "........kpBk....", ".......kwBk.....", "......kpBk......", ".....kwBk.......",
        "....kpBk........", "...kwBk.........", "..kpBk..........", ".kwBk...........", "kkkkkkkkkkkkkkkk",
        "................"],
}


def sled():
    sled_model()
    for name, rows in RAIL_ICONS.items():
        save(grid(rows, ITEM_PAL), "item", f"{name}.png")
        item_model(name)
    # rail texture for the block entity renderer: pink | white / wood | dark wood
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = [(238, 120, 160), (250, 246, 240), (176, 126, 80), (130, 90, 60)][(x // 8) + 2 * (y // 8)]
            img.putpixel((x, y), c + (255,))
    save(img, "block", "slee_rail.png")
    # blockstates: the anchor shows nothing itself (block entity), the parts are invisible
    w(f"{A}/models/block/slee_rail.json", {"textures": {"particle": "guhs:block/slee_rail"}})
    w(f"{A}/blockstates/slee_rail.json", {"variants": {"": {"model": "guhs:block/slee_rail"}}})
    w(f"{A}/blockstates/slee_rail_part.json", {"variants": {"": {"model": "guhs:block/slee_rail"}}})
    # breaking a piece drops its item
    w(f"{D}/loot_table/blocks/slee_rail.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:sleerail_bocht", "conditions": [
                {"condition": "minecraft:block_state_property", "block": "guhs:slee_rail", "properties": {"shape": sh}}]}
            for sh in ("curve_left", "curve_right")] + [
            {"type": "minecraft:item", "name": "guhs:sleerail_helling", "conditions": [
                {"condition": "minecraft:block_state_property", "block": "guhs:slee_rail", "properties": {"shape": "slope"}}]},
            {"type": "minecraft:item", "name": "guhs:sleerail_recht"}]}]}]})
    # recipes: the sled builder's book stays in the grid
    B = "guhs:sleebouwersboek"
    rail_key = {"I": "minecraft:iron_ingot", "W": "minecraft:pink_wool", "B": B, "S": "minecraft:stick"}
    shaped("sleerail_recht", ["I I", "WBW", "S S"], rail_key, "guhs:sleerail_recht", 4)
    shaped("sleerail_bocht", ["II ", "WBW", " SS"], rail_key, "guhs:sleerail_bocht", 2)
    shaped("sleerail_helling", ["  I", " BW", "ISS"], rail_key, "guhs:sleerail_helling", 2)
    shaped("guh_slee", ["  I", "WWW", "IBI"], {"I": "minecraft:iron_ingot", "W": "minecraft:pink_wool", "B": B}, "guhs:guh_slee")
    # the three parts of the broken sled, and new chest tables for guh caves and villages
    w(f"{D}/loot_table/chests/guh_caves.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": "guhs:chests/hamster_house"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:sleeglijder"}],
         "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guh_kristal", "functions": count_fn(1, 3)}],
         "conditions": [{"condition": "minecraft:random_chance", "chance": 0.3}]}]})
    w(f"{D}/loot_table/chests/guh_village.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": "guhs:chests/hamster_house"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guh_belletje"}],
         "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}]}]})
    for key, en, nl in [
        ("entity.guhs.guh_slee", "Guh Sled", "Guh-slee"),
        ("entity.guhs.guh_slee.locked", "This sled belongs to Guhland", "Deze slee hoort bij Guhland"),
        ("item.guhs.guh_slee", "Guh Sled", "Guh-slee"),
        ("item.guhs.guh_slee.lore", "Put it on a sled rail. Right-click while riding: the control panel",
         "Zet hem op een sleerail. Rechtsklik tijdens het rijden: het bedieningspaneel"),
        ("item.guhs.guh_slee.needs_rail", "A sled goes on a sled rail", "Een slee hoort op een sleerail"),
        ("item.guhs.sleerail_recht", "Sled Rail (Straight)", "Sleerail (recht)"),
        ("item.guhs.sleerail_bocht", "Sled Rail (Curve)", "Sleerail (bocht)"),
        ("item.guhs.sleerail_helling", "Sled Rail (Slope)", "Sleerail (helling)"),
        ("item.guhs.sleerail.lore", "4x4 blocks. Click the end of a rail to add on to it",
         "4x4 blokken. Klik op het eind van een rail om erop aan te sluiten"),
        ("item.guhs.sleerail.curve.lore", "Turns right; sneak for left", "Gaat naar rechts; sluip voor links"),
        ("item.guhs.sleerail.slope.lore", "Goes 4 up; sneak (on the end of a rail) to go down",
         "Gaat 4 omhoog; sluip (op het eind van een rail) om te dalen"),
        ("item.guhs.sleerail.blocked", "There is no room for this rail piece", "Er is geen plek voor dit stuk rail"),
        ("block.guhs.slee_rail", "Sled Rail", "Sleerail"),
        ("block.guhs.slee_rail_part", "Sled Rail", "Sleerail"),
        ("gui.guhs.sled.title", "Guh Sled", "Guh-slee"),
        ("gui.guhs.sled.start", "Start", "Start"),
        ("gui.guhs.sled.stop", "Stop", "Stop"),
        ("gui.guhs.sled.reverse", "Turn around", "Omkeren"),
        ("gui.guhs.sled.speed", "Speed", "Snelheid"),
        ("gui.guhs.sled.speed.1", "Slow", "Langzaam"),
        ("gui.guhs.sled.speed.2", "Normal", "Normaal"),
        ("gui.guhs.sled.speed.3", "WHEEE", "WIEEE"),
    ]:
        lang(key, en, nl)
    sleehut()


def sleehut():
    """The Sleehut on the Guh Peaks: a snowy log cabin, the Slee-guh outside next to her broken sled."""
    s = Structure((15, 10, 15))
    fp = [(x, z) for x in range(15) for z in range(15)]
    for x, z in fp:
        s.set(x, 0, z, mc("snow_block") if (x * 7 + z * 3) % 5 else mc("packed_ice"))
    s.fill(3, 1, 3, 11, 1, 9, mc("spruce_planks"))
    for x in range(3, 12):
        for z in range(3, 10):
            if x in (3, 11) or z in (3, 9):
                corner = x in (3, 11) and z in (3, 9)
                s.fill(x, 1, z, x, 4, z, mc("spruce_log" if corner else "stripped_spruce_log"), {"axis": "y"})
    stairs = {"half": "bottom", "shape": "straight", "waterlogged": "false"}
    for i in range(4):                                                   # roof with snow on top
        s.fill(2, 5 + i, 2 + i, 12, 5 + i, 2 + i, mc("spruce_stairs"), {"facing": "south", **stairs})
        s.fill(2, 5 + i, 10 - i, 12, 5 + i, 10 - i, mc("spruce_stairs"), {"facing": "north", **stairs})
    s.fill(2, 8, 6, 12, 8, 6, mc("snow_block"))
    s.fill(4, 5, 4, 10, 5, 8, mc("spruce_planks"))
    door = {"facing": "south", "hinge": "left", "open": "false", "powered": "false"}
    s.set(7, 1, 9, mc("spruce_door"), {"half": "lower", **door})
    s.set(7, 2, 9, mc("spruce_door"), {"half": "upper", **door})
    for (x, z) in ((5, 9), (9, 9), (3, 6), (11, 6)):
        s.set(x, 2, z, mc("glass"))
    s.set(4, 2, 4, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    s.fill(9, 2, 4, 10, 2, 4, mc("pink_wool"))
    chest(s, 5, 2, 4, "south", "guhs:chests/guh_picnic")
    s.set(10, 2, 7, mc("campfire"), {"lit": "true", "facing": "west", "signal_fire": "false", "waterlogged": "false"})
    # the broken sled (in blocks) and the Slee-guh
    s.fill(2, 1, 12, 6, 1, 12, mc("pink_wool"))
    s.set(4, 1, 13, mc("stripped_spruce_log"), {"axis": "x"})
    s.entity(9.5, 1.0, 12.5, {"id": "guhs:guh_npc", "Kind": "slee_guh", "PersistenceRequired": Byte(1),
                              "Rotation": floats(0.0, 0.0)})
    blossom = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for (x, z) in ((12, 12), (1, 1), (13, 2)):                         # little guh blossom trees
        s.fill(x, 1, z, x, 4, z, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (4, 5, 6):
                    if abs(dx) + abs(dz) + (dy - 5) * (dy - 5) <= (3 if dy == 5 else 1) + (dy == 4):
                        s.set(x + dx, dy, z + dz, "guhs:guhbloesem_leaves", blossom) if (x + dx, dy, z + dz) not in s.blocks else None
    s.clear_above(fp, 1)
    s.save("sleehut")


def phase3_structures():
    structure("sleehut", ["guh_peaks"], spacing=28, separation=9, salt=20300001)
    Structure((24, 8, 26)).save("sled_room")  # for the GameTests


# =====================================================================================================================
# Phase 4: pink moon and stars, the guh sea, the crystal mines
# =====================================================================================================================
def patch_json(path, fn):
    data = json.load(open(path, encoding="utf-8"))
    fn(data)
    w(path, data)


def add_tag(path, values):
    full = f"{R}/data/{path}.json"
    data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    w(full, data)


def pink_moon():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(9)
    craters = [(rng.uniform(14, 50), rng.uniform(14, 50), rng.uniform(2, 6)) for _ in range(9)]
    for y in range(64):
        for x in range(64):
            d = math.dist((x + 0.5, y + 0.5), (32, 32))
            if d > 29:
                continue
            c = [255, 170, 205]
            for cx, cy, cr in craters:
                if math.dist((x, y), (cx, cy)) < cr:
                    c = [236, 130, 175]
            shade = 1 - 0.25 * max(0, (d - 20) / 9)
            a = 255 if d < 28 else int(255 * (29 - d))
            img.putpixel((x, y), tuple(int(v * shade) for v in c) + (a,))
    save(img, "environment", "pink_moon.png")


FISH_TEX = {0: (240, 140, 180), 1: (255, 205, 222), 2: (214, 90, 140), 3: (250, 246, 240)}


def guh_vis_model():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(4)
    for i, c in FISH_TEX.items():
        for y in range(8):
            for x in range(8):
                v = rng.randint(-8, 8)
                img.putpixel(((i % 4) * 8 + x, (i // 4) * 8 + y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    # the face (6x5 at 0,8): pink with two big guh eyes
    face = ["pppppp", "wbpwbp", "bkpbkp", "pppppp", "ppmmpp"]
    col = {"p": (240, 140, 180), "w": (255, 255, 255), "b": (90, 170, 235), "k": (30, 30, 60), "m": (200, 70, 120)}
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            img.putpixel((x, 8 + y), col[ch] + (255,))
    save(img, "entity", "guh_vis.png")

    def uv(i):
        return {"uv": [(i % 4) * 8 + 1, (i // 4) * 8 + 1], "uv_size": [6, 6]}
    body_faces = {f: uv(0) for f in ("south", "east", "west", "up")}
    body_faces["down"] = uv(1)
    body_faces["north"] = {"uv": [0, 8], "uv_size": [6, 5]}
    fin = {f: uv(2) for f in ("north", "south", "east", "west", "up", "down")}
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.guh_vis", "texture_width": 32, "texture_height": 32,
                        "visible_bounds_width": 2, "visible_bounds_height": 1, "visible_bounds_offset": [0, 0.25, 0]},
        "bones": [
            {"name": "body", "pivot": [0, 0, 0], "cubes": [
                {"origin": [-3, 0, -4], "size": [6, 5, 9], "uv": body_faces},
                {"origin": [0, 5, -2], "size": [0, 2, 4], "uv": fin},
                {"origin": [-2.5, 5, -3.5], "size": [1, 1, 1], "uv": {f: uv(0) for f in ("north", "south", "east", "west", "up", "down")}},
                {"origin": [1.5, 5, -3.5], "size": [1, 1, 1], "uv": {f: uv(0) for f in ("north", "south", "east", "west", "up", "down")}}]},
            {"name": "tail", "parent": "body", "pivot": [0, 2.5, 5], "cubes": [
                {"origin": [0, 0, 5], "size": [0, 5, 4], "uv": fin}]}]}]}
    w(f"{A}/geckolib/models/entity/guh_vis.geo.json", geo)
    w(f"{A}/geckolib/animations/entity/guh_vis.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.guh_vis.swim": {"loop": True, "animation_length": 0.6, "bones": {
            "tail": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 25, 0], "0.3": [0, 0, 0], "0.45": [0, -25, 0], "0.6": [0, 0, 0]}},
            "body": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, -4, 0], "0.3": [0, 0, 0], "0.45": [0, 4, 0], "0.6": [0, 0, 0]}}}}}})


FISH_ICONS = {
    "guh_vis": [
        "................", "................", "................", "....kkkkk.......", "...kpppppk...kk.",
        "..kpwbppppk.kmk.", ".kppbkpppppkmmk.", ".kpppppppppppmk.", ".kmpppPPPPpppmk.", "..kmpPPPPPppkmk.",
        "...kkpPPPPpk.kk.", ".....kkkkkk.....", "................", "................", "................",
        "................"],
    "gebakken_guh_vis": [
        "................", "................", "................", "....kkkkk.......", "...kgggggk...kk.",
        "..kgwbGgggk.kGk.", ".kggbkggGggkGGk.", ".kgggGgggggggGk.", ".kGgggyyyygggGk.", "..kGgyyyyygGkGk.",
        "...kkgyyyygk.kk.", ".....kkkkkk.....", "................", "................", "................",
        "................"],
}


def guh_sea_and_crystals():
    pink_moon()
    guh_vis_model()
    # the Guhmension uses our sky effects (pink moon + stars)
    patch_json(f"{D}/dimension_type/guhmension.json", lambda d: d.update({"effects": "guhs:guhmension"}))

    # --- guh fish items ---
    for name, rows in FISH_ICONS.items():
        save(grid(rows, ITEM_PAL), "item", f"{name}.png")
        item_model(name)
    bucket = Image.open(os.path.join(TEX, "item", "kaas_saus_bucket.png")).convert("RGBA")
    fish = grid(FISH_ICONS["guh_vis"], ITEM_PAL).resize((10, 10), Image.NEAREST)
    b2 = recolour(bucket, hue=0.58, sat=1.1, only=lambda h, s, v: (h > 0.05) & (h < 0.2) & (s > 0.3))
    b2.alpha_composite(fish, (3, 4))
    save(b2, "item", "guh_vis_bucket.png")
    item_model("guh_vis_bucket")
    w(f"{A}/models/item/guh_vis_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    for kind, typ, t in (("smelting", "minecraft:smelting", 200), ("smoking", "minecraft:smoking", 100),
                         ("campfire_cooking", "minecraft:campfire_cooking", 600)):
        w(f"{D}/recipe/gebakken_guh_vis_{kind}.json", {"type": typ, "category": "food", "ingredient": {"item": "guhs:guh_vis"},
                                                        "result": {"id": "guhs:gebakken_guh_vis"}, "experience": 0.35, "cookingtime": t})
    # fishing in the Guhmension also brings up guh fish
    # (26.1: NeoForge loads every file in data/*/loot_modifiers itself; the global_loot_modifiers.json list is gone)
    w(f"{D}/loot_modifiers/guh_vis_fishing.json", {
        "type": "neoforge:add_table", "table": "guhs:gameplay/guh_vis_fishing", "conditions": [
            {"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:gameplay/fishing/fish"},
            {"condition": "minecraft:location_check", "predicate": {"dimension": "guhs:guhmension"}}]})
    w(f"{D}/loot_table/gameplay/guh_vis_fishing.json", {"type": "minecraft:fishing", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guh_vis"}]}]})
    w(f"{D}/loot_table/entities/guh_vis.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guh_vis", "functions": [
            {"function": "minecraft:furnace_smelt", "conditions": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                     "predicate": {"flags": {"is_on_fire": True}}}]}]}]}]})

    # --- lily pad ---
    lelie = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.dist((x + 0.5, y + 0.5), (8, 8))
            ang = math.degrees(math.atan2(y - 8, x - 8)) % 360
            if d < 7.3 and not (d > 1.5 and 300 < ang < 330):
                c = (236, 120, 170) if (int(d) + x) % 5 else (250, 170, 205)
                if d < 1.6:
                    c = (255, 230, 110)
                lelie.putpixel((x, y), c + (255,))
    save(lelie, "block", "guh_waterlelie.png")
    w(f"{A}/models/block/guh_waterlelie.json", {"parent": "minecraft:block/lily_pad", "render_type": "minecraft:cutout",
                                                  "textures": {"texture": "guhs:block/guh_waterlelie", "particle": "guhs:block/guh_waterlelie"}})
    w(f"{A}/blockstates/guh_waterlelie.json", {"variants": {
        "": [{"model": "guhs:block/guh_waterlelie"}, {"model": "guhs:block/guh_waterlelie", "y": 90},
             {"model": "guhs:block/guh_waterlelie", "y": 180}, {"model": "guhs:block/guh_waterlelie", "y": 270}]}})
    item_model("guh_waterlelie", "guhs:block/guh_waterlelie")
    self_drop("guh_waterlelie")

    # --- crystals ---
    cl = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x0, h, w_) in ((3, 9, 3), (7, 13, 3), (11, 8, 2), (5, 6, 2), (9, 10, 2)):
        for y in range(16 - h, 16):
            for x in range(x0, x0 + w_):
                t = (y - (16 - h)) / h
                c = (255, int(170 + 60 * (1 - t)), int(215 + 30 * (1 - t))) if x == x0 else (236, 120, 190)
                cl.putpixel((x, y), c + (255,))
        cl.putpixel((x0, 16 - h), (255, 255, 255, 255))
    save(cl, "block", "guh_kristal_cluster.png")
    w(f"{A}/models/block/guh_kristal_cluster.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                       "textures": {"cross": "guhs:block/guh_kristal_cluster"}})
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
           "west": {"x": 90, "y": 270}}
    w(f"{A}/blockstates/guh_kristal_cluster.json", {"variants": {
        f"facing={f}": {"model": "guhs:block/guh_kristal_cluster", **r} for f, r in rot.items()}})
    item_model("guh_kristal_cluster", "guhs:block/guh_kristal_cluster")
    blok = noise_tex((236, 130, 190), 12, 21, spots=[((255, 200, 230), 0.15), ((200, 80, 150), 0.12)])
    for i in range(16):
        blok.putpixel((i, i), (255, 225, 240, 255))
        blok.putpixel((15 - i, i // 2), (255, 210, 235, 255))
    save(blok, "block", "guh_kristal_blok.png")
    lamp = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            d = math.dist((x + 0.5, y + 0.5), (8, 8))
            c = (200, 170, 190) if edge else (255, int(200 + 50 * max(0, 1 - d / 7)), int(225 + 30 * max(0, 1 - d / 7)))
            lamp.putpixel((x, y), c + (255,))
    save(lamp, "block", "guh_kristal_lamp.png")
    save(noise_tex((214, 170, 180), 10, 22, spots=[((236, 190, 200), 0.2), ((180, 140, 150), 0.1), ((240, 150, 200), 0.03)]),
         "block", "guh_kristalsteen.png")
    for b in ("guh_kristal_blok", "guh_kristal_lamp", "guh_kristalsteen"):
        simple_block(b)
        self_drop(b)
    save(grid(ITEMS["guh_kristal"], ITEM_PAL), "item", "guh_kristal.png")
    item_model("guh_kristal")
    w(f"{D}/loot_table/blocks/guh_kristal_cluster.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:guh_kristal_cluster", "conditions": [
                {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "functions": count_fn(1, 3) + [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}]}]}]}]})
    add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guh_kristal_cluster", "guhs:guh_kristal_blok", "guhs:guh_kristalsteen"])
    shaped("guh_kristal_blok", ["KK", "KK"], {"K": "guhs:guh_kristal"}, "guhs:guh_kristal_blok")
    shaped("guh_kristal_lamp", ["NGN", "GKG", "NGN"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass", "K": "guhs:guh_kristal"},
           "guhs:guh_kristal_lamp", 2)
    # singing crystals: the guh sounds, played high
    snd_path = f"{A}/sounds.json"
    sounds = json.load(open(snd_path, encoding="utf-8"))
    ambient = [s if isinstance(s, str) else s["name"] for s in sounds["entity.guh.ambient"]["sounds"]]
    sounds["block.guh_kristal.sing"] = {"subtitle": "subtitles.guhs.block.guh_kristal.sing",
                                        "sounds": [{"name": n, "pitch": 1.1, "volume": 0.6} for n in ambient[:6]]}
    w(snd_path, sounds)

    # --- worldgen: guh sea (flat, with pink water pools) and the crystal mines (underground, rare) ---
    w(f"{D}/worldgen/configured_feature/guh_pool.json", {"type": "guhs:guh_pool", "config": {}})
    w(f"{D}/worldgen/placed_feature/guh_pool.json", {"feature": "guhs:guh_pool", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(f"{D}/worldgen/configured_feature/guh_crystal_cave.json", {"type": "guhs:guh_crystal_cave", "config": {}})
    w(f"{D}/worldgen/placed_feature/guh_crystal_cave.json", {"feature": "guhs:guh_crystal_cave", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 36},
                                                      "max_inclusive": {"absolute": 54}}},
        {"type": "minecraft:biome"}]})
    ores = ["guhs:guhmension_kaasknabbel_veins", "guhs:vahoege_vads_ore"]
    base = json.load(open(f"{D}/worldgen/biome/guh_meadows.json", encoding="utf-8"))
    sea = json.loads(json.dumps(base))
    sea["effects"].update({"sky_color": 0xF4B8DC, "fog_color": 0xFFDDEE, "water_color": 0xFF8FC8, "water_fog_color": 0xF070A8})
    sea["spawners"]["water_ambient"] = [{"type": "guhs:guh_vis", "weight": 20, "minCount": 3, "maxCount": 5}]
    sea["features"] = [[], ["guhs:guh_pool"], [], [], [], [], ores, [], [], [], []]
    w(f"{D}/worldgen/biome/guh_sea.json", sea)
    mine = json.loads(json.dumps(base))
    mine["effects"].update({"fog_color": 0xE890C8, "ambient_sound": "minecraft:ambient.cave",
                            "particle": {"options": {"type": "minecraft:dust", "color": [1.0, 0.6, 0.85], "scale": 0.8},
                                         "probability": 0.006}})
    mine["spawners"] = {k: [] for k in base["spawners"]}
    mine["features"] = [[], [], [], ["guhs:guh_crystal_cave"], [], [], ores, [], [], [], []]
    w(f"{D}/worldgen/biome/guh_kristalmijn.json", mine)

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] not in ("guhs:guh_sea", "guhs:guh_kristalmijn")]
        for e in entries:  # the surface biomes: any depth / crystal noise (so they win everywhere the mines don't)
            e["parameters"]["depth"] = [-1.0, 1.0]
            e["parameters"]["continentalness"] = [-1.0, 1.0]
        full = [-1.0, 1.0]
        entries.append({"biome": "guhs:guh_sea", "parameters": {"temperature": 0.05, "humidity": -0.5, "continentalness": full,
                                                                "erosion": [-1.0, -0.25], "weirdness": 0.0, "depth": full, "offset": 0.0}})
        entries.append({"biome": "guhs:guh_kristalmijn", "parameters": {"temperature": full, "humidity": full, "continentalness": [0.55, 1.0],
                                                                        "erosion": full, "weirdness": 0.0, "depth": [0.5, 1.0], "offset": 0.0}})
        d["generator"]["biome_source"]["biomes"] = entries
    patch_json(f"{D}/dimension/guhmension.json", biome_source)
    w(f"{D}/worldgen/noise/guhmension_crystal.json", {"firstOctave": -6, "amplitudes": [1.0, 0.5]})

    def router(d):
        # continents: where the crystal mines are; depth: 1 below y58, -1 above y62 (so the mines stay underground)
        d["noise_router"]["continents"] = {"type": "minecraft:noise", "noise": "guhs:guhmension_crystal", "xz_scale": 1.0, "y_scale": 0.0}
        d["noise_router"]["depth"] = {"type": "minecraft:y_clamped_gradient", "from_y": 58, "to_y": 62, "from_value": 1.0, "to_value": -1.0}
        rules = d["surface_rule"]["sequence"]
        sea_rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": ["guhs:guh_sea"]},
                    "then_run": {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0,
                                 "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
                                 "then_run": {"type": "minecraft:sequence", "sequence": [
                                     {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold",
                                      "noise": "guhs:guhmension_patches", "min_threshold": 0.35, "max_threshold": 10.0},
                                      "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:pink_concrete_powder"}}},
                                     {"type": "minecraft:block", "result_state": {"Name": "minecraft:pink_wool"}}]}}}
        if sea_rule not in rules:
            rules.append(sea_rule)
    patch_json(f"{D}/worldgen/noise_settings/guhmension.json", router)

    for key, en, nl in [
        ("biome.guhs.guh_sea", "Guh Sea", "Guhzee"),
        ("biome.guhs.guh_kristalmijn", "Guh Crystal Mine", "Guhkristalmijn"),
        ("entity.guhs.guh_vis", "Guh Fish", "Guhvis"),
        ("item.guhs.guh_vis", "Guh Fish", "Guhvis"),
        ("item.guhs.gebakken_guh_vis", "Fried Guh Fish", "Gebakken guhvis"),
        ("item.guhs.guh_vis_bucket", "Bucket of Guh Fish", "Emmer met guhvis"),
        ("item.guhs.guh_vis_spawn_egg", "Guh Fish Spawn Egg", "Guhvis-spawnei"),
        ("block.guhs.guh_waterlelie", "Guh Lily Pad", "Guhwaterlelie"),
        ("block.guhs.guh_kristal_cluster", "Guh Crystal Cluster", "Guhkristalcluster"),
        ("block.guhs.guh_kristal_blok", "Block of Guh Crystal", "Guhkristalblok"),
        ("block.guhs.guh_kristal_lamp", "Guh Crystal Lamp", "Guhkristallamp"),
        ("block.guhs.guh_kristalsteen", "Guh Crystal Stone", "Guhkristalsteen"),
        ("subtitles.guhs.block.guh_kristal.sing", "Guh crystal sings", "Guhkristal zingt"),
    ]:
        lang(key, en, nl)


# =====================================================================================================================
# Phase 5: guh bees + knabbelkorf, pink guh slimes, Nether Mikas
# =====================================================================================================================
GUHMENSION_BIOMES = ["guh_fields", "knabbel_crumbs", "pink_puffs", "kaas_flats", "guh_meadows", "guh_peaks", "vads_cliffs",
                     "mikas_biome", "guh_sea"]


def paint(img, x0, y0, rows, col):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in col:
                img.putpixel((x0 + x, y0 + y), col[ch])


def guh_bee_model():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(12)
    pink, yellow = (244, 150, 190), (255, 214, 80)

    def fuzz(c):
        v = rng.randint(-10, 10)
        return tuple(max(0, min(255, ch + v)) for ch in c) + (255,)
    for y in range(8):                       # (0,0): side stripes along u, (8,0): top stripes along v, (16,0): plain pink
        for x in range(8):
            img.putpixel((x, y), fuzz(yellow if (x // 2) % 2 else pink))
            img.putpixel((8 + x, y), fuzz(yellow if (y // 2) % 2 else pink))
            img.putpixel((16 + x, y), fuzz(pink))
            img.putpixel((24 + x, y), (225, 240, 255, 200))             # wings
            img.putpixel((32 + x, y), fuzz((60, 40, 50)))               # antennae / legs
    face = ["ppppppp", "pwbpwbp", "pbkpbkp", "ppppppp", "ppmmmpp", "ppppppp", "ppppppp"]
    paint(img, 0, 8, face, {"p": (244, 150, 190, 255), "w": (255, 255, 255, 255), "b": (90, 170, 235, 255),
                            "k": (30, 30, 60, 255), "m": (200, 70, 120, 255)})
    save(img, "entity", "guh_bee.png")

    def sw(u, v, w_=6, h=6):
        return {"uv": [u + 1, v + 1], "uv_size": [w_, h]}
    body = {"north": {"uv": [0, 8], "uv_size": [7, 7]}, "south": sw(16, 0), "east": sw(0, 0), "west": sw(0, 0),
            "up": sw(8, 0), "down": sw(16, 0)}
    wing = {f: sw(24, 0) for f in ("north", "south", "east", "west", "up", "down")}
    dark = {f: sw(32, 0) for f in ("north", "south", "east", "west", "up", "down")}
    pinkf = {f: sw(16, 0) for f in ("north", "south", "east", "west", "up", "down")}
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.guh_bee", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": [
            {"name": "body", "pivot": [0, 5, 0], "cubes": [
                {"origin": [-3.5, 2, -5], "size": [7, 7, 10], "uv": body},
                {"origin": [-3, 9, -3.5], "size": [1, 1, 1], "uv": pinkf},      # guh ears
                {"origin": [2, 9, -3.5], "size": [1, 1, 1], "uv": pinkf},
                {"origin": [-2, 7, -8], "size": [1, 0, 3], "uv": dark},         # antennae
                {"origin": [1, 7, -8], "size": [1, 0, 3], "uv": dark},
                {"origin": [-2, 0, -2], "size": [1, 2, 0], "uv": dark},         # legs
                {"origin": [1, 0, -2], "size": [1, 2, 0], "uv": dark},
                {"origin": [-2, 0, 1], "size": [1, 2, 0], "uv": dark},
                {"origin": [1, 0, 1], "size": [1, 2, 0], "uv": dark}]},
            {"name": "wing_left", "parent": "body", "pivot": [1.5, 9, -2], "cubes": [
                {"origin": [1.5, 9, -2], "size": [7, 0, 5], "uv": wing}]},
            {"name": "wing_right", "parent": "body", "pivot": [-1.5, 9, -2], "cubes": [
                {"origin": [-8.5, 9, -2], "size": [7, 0, 5], "uv": wing}]}]}]}
    w(f"{A}/geckolib/models/entity/guh_bee.geo.json", geo)
    w(f"{A}/geckolib/animations/entity/guh_bee.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.guh_bee.fly": {"loop": True, "animation_length": 0.2, "bones": {
            "wing_left": {"rotation": {"0.0": [0, 0, -15], "0.1": [0, 0, 35], "0.2": [0, 0, -15]}},
            "wing_right": {"rotation": {"0.0": [0, 0, 15], "0.1": [0, 0, -35], "0.2": [0, 0, 15]}},
            "body": {"position": {"0.0": [0, 0, 0], "0.1": [0, 0.4, 0], "0.2": [0, 0, 0]}}}}}})


def slime_texture():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rng = random.Random(5)
    for y in range(16):                                   # outer, see-through layer
        for x in range(32):
            if 8 <= x < 24 and y < 8 or y >= 8:
                light = 1.15 if 8 <= x < 16 and y < 8 else 1.0
                c = tuple(min(255, int(ch * light)) for ch in (240, 130, 185))
                img.putpixel((x, y), c + (130 + rng.randint(-10, 10),))
    for y in range(16, 28):                               # inner core
        for x in range(24):
            if 6 <= x < 18 and y < 22 or y >= 22:
                v = rng.randint(-10, 10)
                img.putpixel((x, y), (min(255, 225 + v), 110 + v, 165 + v, 255))
    for (ex, ey) in ((32, 0), (32, 4)):                    # eyes: guh-blue with a white shine
        for y in range(4):
            for x in range(8):
                img.putpixel((ex + x, ey + y), (40, 40, 70, 255))
        img.putpixel((ex + 2, ey + 2), (90, 170, 235, 255))
        img.putpixel((ex + 3, ey + 2), (255, 255, 255, 255))
        img.putpixel((ex + 2, ey + 3), (60, 120, 200, 255))
        img.putpixel((ex + 3, ey + 3), (90, 170, 235, 255))
    for y in range(8, 10):                                # mouth
        for x in range(32, 36):
            img.putpixel((x, y), (170, 50, 100, 255))
    save(img, "entity", "guh_slime.png")


def bees_slimes_mikas():
    guh_bee_model()
    slime_texture()
    mika = Image.open(os.path.join(TEX, "entity", "mika.png")).convert("RGBA")
    nm = recolour(mika, hue=0.02, sat=1.2, val=0.55)
    a = np.asarray(nm).copy()
    rng = np.random.default_rng(3)
    ember = (rng.random(a.shape[:2]) < 0.035) & (a[..., 3] > 0)
    a[ember] = [255, 120, 30, 255]
    save(Image.fromarray(a), "entity", "nether_mika.png")

    # knabbelkorf
    side = noise_tex((226, 176, 90), 12, 31)
    for y in (3, 8, 13):
        for x in range(16):
            side.putpixel((x, y), (186, 130, 60, 255))
    save(side, "block", "knabbelkorf_side.png")
    top = noise_tex((216, 166, 84), 10, 32)
    for i in range(16):
        top.putpixel((i, 0), (170, 120, 60, 255)); top.putpixel((i, 15), (170, 120, 60, 255))
        top.putpixel((0, i), (170, 120, 60, 255)); top.putpixel((15, i), (170, 120, 60, 255))
    save(top, "block", "knabbelkorf_top.png")
    for honey in (False, True):
        front = side.copy()
        for y in range(6, 11):
            for x in range(5, 11):
                front.putpixel((x, y), (60, 34, 20, 255))
        if honey:
            for (x, y) in ((6, 5), (9, 5), (4, 7), (11, 8), (7, 11), (8, 12), (10, 11)):
                front.putpixel((x, y), (255, 220, 60, 255))
            for x in range(5, 11):
                front.putpixel((x, 10), (250, 190, 40, 255))
        save(front, "block", "knabbelkorf_front_honey.png" if honey else "knabbelkorf_front.png")
    for suffix in ("", "_honey"):
        w(f"{A}/models/block/knabbelkorf{suffix}.json", {"parent": "minecraft:block/orientable_with_bottom", "textures": {
            "front": f"guhs:block/knabbelkorf_front{suffix}", "side": "guhs:block/knabbelkorf_side",
            "top": "guhs:block/knabbelkorf_top", "bottom": "guhs:block/knabbelkorf_top", "particle": "guhs:block/knabbelkorf_side"}})
    variants = {}
    for f, yrot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lvl in range(6):
            v = {"model": "guhs:block/knabbelkorf_honey" if lvl == 5 else "guhs:block/knabbelkorf"}
            if yrot:
                v["y"] = yrot
            variants[f"facing={f},honey_level={lvl}"] = v
    w(f"{A}/blockstates/knabbelkorf.json", {"variants": variants})
    w(f"{A}/models/item/knabbelkorf.json", {"parent": "guhs:block/knabbelkorf"})
    self_drop("knabbelkorf")
    add_tag("minecraft/tags/point_of_interest_type/bee_home", ["guhs:knabbelkorf"])
    add_tag("minecraft/tags/block/beehives", ["guhs:knabbelkorf"])
    add_tag("minecraft/tags/block/mineable/axe", ["guhs:knabbelkorf"])
    shaped("knabbelkorf", ["PPP", "KKK", "PPP"], {"P": "#minecraft:planks", "K": "guhs:kaas_knabbels"}, "guhs:knabbelkorf")

    # pink slime block
    sb = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner = 3 <= x <= 12 and 3 <= y <= 12
            c = (250, 170, 210, 220) if edge else (236, 120, 180, 235) if inner else (244, 146, 196, 170)
            sb.putpixel((x, y), c)
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        sb.putpixel((x, y), (255, 225, 240, 240))
    save(sb, "block", "roze_slijmblok.png")
    w(f"{A}/models/block/roze_slijmblok.json", {"parent": "minecraft:block/slime_block", "render_type": "minecraft:translucent",
                                                  "textures": {"particle": "guhs:block/roze_slijmblok", "texture": "guhs:block/roze_slijmblok"}})
    w(f"{A}/blockstates/roze_slijmblok.json", {"variants": {"": {"model": "guhs:block/roze_slijmblok"}}})
    w(f"{A}/models/item/roze_slijmblok.json", {"parent": "guhs:block/roze_slijmblok"})
    self_drop("roze_slijmblok")
    shaped("roze_slijmblok", ["SSS", "SSS", "SSS"], {"S": "guhs:guh_slimeball"}, "guhs:roze_slijmblok")
    shapeless("guh_slimeball_from_block", ["guhs:roze_slijmblok"], "guhs:guh_slimeball", 9)
    shaped("sticky_piston_from_guh_slimeball", ["S", "P"], {"S": "guhs:guh_slimeball", "P": "minecraft:piston"}, "minecraft:sticky_piston")
    shaped("lead_from_guh_slimeball", ["SS ", "SB ", "  S"], {"S": "minecraft:string", "B": "guhs:guh_slimeball"}, "minecraft:lead", 2)

    # items
    icons = {
        "kaashoning": [
            "................", "......kkkk......", "......kbbk......", ".......kk.......", "......kllk......",
            ".....klyylk.....", "....klyyyyyk....", "....kyyYyyyk....", "....kyYyyyyk....", "....kyyyyGyk....",
            "....kyyyyyGk....", "....kyyyyGGk....", ".....kGGGGk.....", "......kkkk......", "................",
            "................"],
        "guh_slimeball": [
            "................", "................", "................", "......kkkk......", "....kkPPppkk....",
            "...kPPwPpppmk...", "...kPwPppppmk...", "..kpPPpppppmmk..", "..kppppppppmmk..", "..kpppppppmmmk..",
            "...kppppmmmmk...", "...kkmmmmmmkk...", ".....kkkkkk.....", "................", "................",
            "................"],
    }
    pal = dict(ITEM_PAL)
    pal.update({"y": (255, 200, 60, 255), "Y": (255, 236, 150, 255), "G": (220, 150, 30, 255), "l": (230, 240, 250, 200),
                "b": (150, 110, 70, 255)})
    for name, rows in icons.items():
        save(grid(rows, pal), "item", f"{name}.png")
        item_model(name)
    for egg in ("guh_bee_spawn_egg", "guh_slime_spawn_egg", "nether_mika_spawn_egg"):
        w(f"{A}/models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})

    # loot
    w(f"{D}/loot_table/entities/guh_slime.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:guh_slimeball", "functions": count_fn(0, 2) + [
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}],
        "conditions": [{"condition": "minecraft:entity_properties", "entity": "this",
                        "predicate": {"type_specific": {"type": "minecraft:slime", "size": 1}}}]}]})
    mika_loot = json.load(open(f"{D}/loot_table/entities/mika.json", encoding="utf-8"))
    mika_loot["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:magma_cream"}],
                               "conditions": [{"condition": "minecraft:random_chance", "chance": 0.3}]})
    w(f"{D}/loot_table/entities/nether_mika.json", mika_loot)

    # spawns: bees + slimes in the Guhmension (and bees/slimes in pretty overworld biomes), Nether Mikas in the Nether
    for b in GUHMENSION_BIOMES:
        def add(d, b=b):
            creature = [e for e in d["spawners"].get("creature", []) if e["type"] not in ("guhs:guh_bee", "guhs:guh_slime")]
            if b != "mikas_biome":
                creature.append({"type": "guhs:guh_bee", "weight": 12, "minCount": 1, "maxCount": 3})
                creature.append({"type": "guhs:guh_slime", "weight": 8, "minCount": 1, "maxCount": 2})
            d["spawners"]["creature"] = creature
        patch_json(f"{D}/worldgen/biome/{b}.json", add)
    w(f"{D}/neoforge/biome_modifier/guh_bees_overworld.json", {
        "type": "neoforge:add_spawns", "biomes": ["minecraft:flower_forest", "minecraft:meadow", "minecraft:sunflower_plains",
                                                  "minecraft:plains", "minecraft:cherry_grove"],
        "spawners": [{"type": "guhs:guh_bee", "weight": 4, "minCount": 1, "maxCount": 2}]})
    w(f"{D}/neoforge/biome_modifier/guh_slimes_overworld.json", {
        "type": "neoforge:add_spawns", "biomes": ["minecraft:cherry_grove", "minecraft:flower_forest", "minecraft:meadow"],
        "spawners": [{"type": "guhs:guh_slime", "weight": 3, "minCount": 1, "maxCount": 2}]})
    w(f"{D}/neoforge/biome_modifier/nether_mikas.json", {
        "type": "neoforge:add_spawns", "biomes": "#minecraft:is_nether",
        "spawners": [{"type": "guhs:nether_mika", "weight": 6, "minCount": 1, "maxCount": 1}]})

    for key, en, nl in [
        ("entity.guhs.guh_bee", "Guh Bee", "Guhbij"),
        ("entity.guhs.guh_slime", "Guh Slime", "Guhslijm"),
        ("entity.guhs.nether_mika", "Nether Mika", "Nether-Mika"),
        ("item.guhs.guh_bee_spawn_egg", "Guh Bee Spawn Egg", "Guhbij-spawnei"),
        ("item.guhs.guh_slime_spawn_egg", "Guh Slime Spawn Egg", "Guhslijm-spawnei"),
        ("item.guhs.nether_mika_spawn_egg", "Nether Mika Spawn Egg", "Nether-Mika-spawnei"),
        ("item.guhs.kaashoning", "Cheese Honey", "Kaashoning"),
        ("item.guhs.guh_slimeball", "Guh Slimeball", "Guhslijmbal"),
        ("block.guhs.knabbelkorf", "Knabbelkorf", "Knabbelkorf"),
        ("block.guhs.roze_slijmblok", "Pink Slime Block", "Roze slijmblok"),
    ]:
        lang(key, en, nl)


# =====================================================================================================================
# Phase 6: food, furniture and deco, flowers, the knabbelboer and his kaasknabbel plants
# =====================================================================================================================
import io  # noqa: E402
import zipfile  # noqa: E402

_JAR = None


def vanilla(path):
    global _JAR
    if _JAR is None:
        _JAR = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    return Image.open(io.BytesIO(_JAR.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def rehue(img, hue, sat=1.0, val=1.0, keep_grey=True):
    only = (lambda h, s, v: s > 0.12) if keep_grey else None
    return recolour(img, hue=hue, sat=sat, val=val, only=only)


def ramp(img, dark, light):
    """A vanilla texture's light and dark, painted from one colour to another."""
    a = np.asarray(img).astype(np.float32)
    lum = a[..., 0] * .3 + a[..., 1] * .59 + a[..., 2] * .11
    lo, hi = np.percentile(lum[a[..., 3] > 0], 3), np.percentile(lum[a[..., 3] > 0], 97)
    t = np.clip((lum - lo) / (hi - lo), 0, 1)[..., None]
    out = np.array(dark, np.float32) * (1 - t) + np.array(light, np.float32) * t
    return Image.fromarray(np.concatenate([out, a[..., 3:]], axis=2).astype(np.uint8))


def colorize(img, rgb):
    """A grey (biome-tinted) vanilla texture, coloured in."""
    a = np.asarray(img).astype(np.float32)
    out = a.copy()
    for i in range(3):
        out[..., i] = a[..., i] * rgb[i] / 255
    return Image.fromarray(out.astype(np.uint8))


def el(frm, to, tex, rot=None, faces=None):
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    return e


def facing_states(name, model=None, extra=""):
    return {f"facing={f}{extra}": ({"model": model or f"guhs:block/{name}", **({"y": y} if y else {})})
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}


def furniture_model(name, elements, textures):
    w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": textures["particle"], **textures},
                                         "elements": elements})
    w(f"{A}/blockstates/{name}.json", {"variants": facing_states(name)})
    w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
    self_drop(name)


FOOD_ICONS = {
    "guh_cupcake": [
        "................", "......kkkk......", ".....kPwPPk.....", "....kPPPPPPk....", "...kppPPpPppk...", "...kppppppppk...",
        "..kmppppppppmk..", "..kkkkkkkkkkkk..", "...kBbBbBbBbk...", "...kbBbBbBbBk...", "....kBbBbBbk....", "....kbBbBbBk....",
        ".....kkkkkk.....", "................", "................", "................"],
    "macaron": [
        "................", "................", "................", "................", "....kkkkkkkk....", "...kPPPPPPPPk...",
        "..kPPwPPPPPPPk..", "..kppppppppppk..", "..kwwwwwwwwwwk..", "..kppppppppppk..", "..kmppppppppmk..", "...kmmmmmmmmk...",
        "....kkkkkkkk....", "................", "................", "................"],
    "kaasfondue": [
        "................", "................", "....g.....s.....", ".....g...s......", "......g.s.......", "...kkkkkkkkkk...",
        "..kyyyyYyyyyyk..", "..kyYyyyyyyYyk..", "..kbyyyyyyyybk..", "...kbbbbbbbbk...", "....kbBBBBbk....", ".....kkkkkk.....",
        "................", "................", "................", "................"],
    "kaasknabbel_milkshake": [
        "................", ".........kk.....", "........kr......", ".......kr.......", ".....kkkkkk.....", "....kPPwPPPk....",
        "....kPwPPPPk....", "....klyyyyyk....", "....klyyyyyk....", "....klpppppk....", ".....klppplk....", ".....klpppk.....",
        ".....kllllk.....", "......kkkk......", "................", "................"],
    "kaasknabbelzaadjes": [
        "................", "................", "................", "....kk....kk....", "...kyyk..kyyk...", "...kyGk..kyGk...",
        "....kk.kk.kk....", "......kyyk......", "......kyGk......", ".......kk.......", "...kk.....kk....", "..kyyk...kyyk...",
        "..kyGk...kyGk...", "...kk.....kk....", "................", "................"],
    "guh_taart_item": [
        "................", "................", "................", "....kkkkkkkk....", "...kPwPPwPPPk...", "..kPPPPPPPPPPk..",
        "..kpPPpPPpPPpk..", "..kwbkwwwbkwwk..", "..kppppppppppk..", "..kppppmmppppk..", "..kPPPPPPPPPPk..", "..kppppppppppk..",
        "..kkkkkkkkkkkk..", "................", "................", "................"],
}

MACARON_COLOURS = {"roze": ((255, 170, 205), (240, 120, 170)), "mint": ((170, 240, 200), (110, 200, 160)),
                   "citroen": ((255, 240, 130), (230, 200, 60)), "choco": ((170, 110, 80), (120, 70, 50))}


def deco_and_food():
    pal = dict(ITEM_PAL)
    pal.update({"y": (255, 210, 70, 255), "Y": (255, 236, 150, 255), "G": (220, 150, 30, 255), "l": (230, 240, 250, 200),
                "b": (150, 110, 70, 255), "B": (190, 140, 90, 255), "g": (200, 200, 200, 255), "s": (210, 210, 220, 255),
                "r": (230, 60, 90, 255)})
    for name, rows in FOOD_ICONS.items():
        if name == "macaron":
            for colour, (light, dark) in MACARON_COLOURS.items():
                p2 = dict(pal)
                p2.update({"P": light + (255,), "p": tuple(int((a + b) / 2) for a, b in zip(light, dark)) + (255,), "m": dark + (255,)})
                save(grid(rows, p2), "item", f"macaron_{colour}.png")
                item_model(f"macaron_{colour}")
            continue
        save(grid(rows, pal), "item", f"{name}.png")
        if name != "guh_taart_item":
            item_model(name)
    item_model("guh_taart", "guhs:item/guh_taart_item")

    # --- the guh cake (block): the vanilla cake models with pink guh textures ---
    for part in ("top", "side", "bottom", "inner"):
        img = vanilla(f"block/cake_{part}")
        img = rehue(img, 0.92, 1.4, 1.0)
        if part == "side":                              # guh eyes on the side
            for (x, y) in ((4, 9), (10, 9)):
                img.putpixel((x, y), (255, 255, 255, 255)); img.putpixel((x + 1, y), (90, 170, 235, 255))
                img.putpixel((x, y + 1), (90, 170, 235, 255)); img.putpixel((x + 1, y + 1), (30, 30, 60, 255))
        save(img, "block", f"guh_taart_{part}.png")
    tex = {"particle": "guhs:block/guh_taart_side", "bottom": "guhs:block/guh_taart_bottom", "top": "guhs:block/guh_taart_top",
           "side": "guhs:block/guh_taart_side", "inside": "guhs:block/guh_taart_inner"}
    w(f"{A}/models/block/guh_taart.json", {"parent": "minecraft:block/cake", "textures": tex})
    for i in range(1, 7):
        w(f"{A}/models/block/guh_taart_slice{i}.json", {"parent": f"minecraft:block/cake_slice{i}", "textures": tex})
    w(f"{A}/blockstates/guh_taart.json", {"variants": {f"bites={i}": {"model": "guhs:block/guh_taart" + (f"_slice{i}" if i else "")}
                                                      for i in range(7)}})

    # --- furniture (cherry wood + pink wool) ---
    P, Wp, Ww = "#wood", "#pink", "#white"
    ft = {"particle": "minecraft:block/cherry_planks", "wood": "minecraft:block/cherry_planks", "pink": "minecraft:block/pink_wool",
          "white": "minecraft:block/white_wool"}
    legs = lambda h, a=2, b=4, c=12, d=14: [el([a, 0, a], [b, h, b], P), el([c, 0, a], [d, h, b], P), el([a, 0, c], [b, h, d], P), el([c, 0, c], [d, h, d], P)]
    furniture_model("guh_stoel", legs(7) + [el([2, 7, 2], [14, 9, 14], P), el([3, 9, 3], [13, 10, 12], Wp),
                                           el([2, 9, 12], [14, 20, 14], P), el([3, 11, 11.9], [13, 18, 12], Wp, faces=("north",)),
                                           el([3, 20, 12], [6, 23, 14], Wp), el([10, 20, 12], [13, 23, 14], Wp)], ft)
    furniture_model("guh_tafel", legs(13, 1, 4, 12, 15) + [el([0, 13, 0], [16, 16, 16], P), el([1, 16, 1], [15, 16.2, 15], Ww)], ft)
    furniture_model("guh_bank", [el([0, 0, 1], [16, 8, 15], P), el([1, 8, 1], [15, 10, 11], Wp), el([0, 8, 11], [16, 16, 15], Wp),
                                 el([0, 8, 1], [2, 12, 11], P), el([14, 8, 1], [16, 12, 11], P),
                                 el([2, 16, 12], [5, 18, 14], Wp), el([11, 16, 12], [14, 18, 14], Wp)], ft)
    # the cupboard: a barrel with a guh-pink door front
    base = rehue(vanilla("block/cherry_planks"), 0.93, 1.2)
    save(base, "block", "guh_kast_side.png")
    for opened in (False, True):
        img = base.copy()
        for y in range(16):
            img.putpixel((0, y), (120, 60, 80, 255)); img.putpixel((15, y), (120, 60, 80, 255)); img.putpixel((8, y), (120, 60, 80, 255))
        for x in range(16):
            img.putpixel((x, 0), (120, 60, 80, 255)); img.putpixel((x, 15), (120, 60, 80, 255))
        if opened:
            for y in range(1, 15):
                for x in range(1, 15):
                    if x != 8:
                        img.putpixel((x, y), (60, 30, 45, 255))
        else:
            for (x, y) in ((6, 7), (9, 7)):                    # guh-eye door knobs
                img.putpixel((x, y), (255, 255, 255, 255)); img.putpixel((x, y + 1), (90, 170, 235, 255))
        save(img, "block", "guh_kast_front_open.png" if opened else "guh_kast_front.png")
    for opened in ("", "_open"):
        w(f"{A}/models/block/guh_kast{opened}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"guhs:block/guh_kast_front{opened}", "side": "guhs:block/guh_kast_side", "top": "guhs:block/guh_kast_side"}})
    kv = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270), ("up", 0), ("down", 0)):
        for o in ("false", "true"):
            kv[f"facing={f},open={o}"] = {"model": "guhs:block/guh_kast" + ("_open" if o == "true" else ""), **({"y": y} if y else {})}
    w(f"{A}/blockstates/guh_kast.json", {"variants": kv})
    w(f"{A}/models/item/guh_kast.json", {"parent": "guhs:block/guh_kast"})
    self_drop("guh_kast")

    # --- paper lanterns ---
    for colour, rgb in (("roze", (255, 150, 200)), ("geel", (255, 220, 110)), ("mint", (150, 235, 190))):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                rib = y % 3 == 0
                c = tuple(int(v * (0.8 if rib else 1.0)) for v in rgb)
                img.putpixel((x, y), c + (255,))
        save(img, "block", f"lampion_{colour}.png")
        for hanging in (False, True):
            dy = 3 if hanging else 0
            els = [el([4, 1 + dy, 4], [12, 10 + dy, 12], "#paper"), el([5, 10 + dy, 5], [11, 11 + dy, 11], "#cap"),
                   el([5, dy, 5], [11, 1 + dy, 11], "#cap")]
            if hanging:
                els.append(el([7.5, 14, 7.5], [8.5, 16, 8.5], "#cap"))
            name = f"lampion_{colour}" + ("_hanging" if hanging else "")
            w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {
                "particle": f"guhs:block/lampion_{colour}", "paper": f"guhs:block/lampion_{colour}", "cap": "minecraft:block/dark_oak_planks"},
                "elements": els})
        w(f"{A}/blockstates/lampion_{colour}.json", {"variants": {
            "hanging=false": {"model": f"guhs:block/lampion_{colour}"}, "hanging=true": {"model": f"guhs:block/lampion_{colour}_hanging"}}})
        w(f"{A}/models/item/lampion_{colour}.json", {"parent": f"guhs:block/lampion_{colour}"})
        self_drop(f"lampion_{colour}")
        dye = {"roze": "pink_dye", "geel": "yellow_dye", "mint": "lime_dye"}[colour]
        shapeless(f"lampion_{colour}", ["minecraft:paper", "minecraft:paper", "minecraft:torch", "minecraft:string", f"minecraft:{dye}"],
                  f"guhs:lampion_{colour}", 2)

    # --- bunting ---
    flags = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, rgb in enumerate(((255, 120, 180), (255, 255, 255), (255, 210, 70), (140, 220, 190))):
        for y in range(4):
            for x in range(4 * i + y // 2, 4 * i + 4 - y // 2):
                flags.putpixel((x, y), rgb + (255,))
    for x in range(16):
        flags.putpixel((x, 8), (240, 240, 240, 255))
    save(flags, "block", "vlaggetjes.png")
    els = [el([0, 14.5, 7.9], [16, 15, 8.1], "#flags")]
    els[0]["faces"] = {f: {"texture": "#flags", "uv": [0, 8, 16, 9]} for f in ("north", "south", "up", "down")}
    for i, x in enumerate((1, 6, 11)):
        e = {"from": [x, 10.5, 8], "to": [x + 4, 14.5, 8], "faces": {f: {"texture": "#flags", "uv": [4 * (i % 4), 0, 4 * (i % 4) + 4, 4]}
                                                                    for f in ("north", "south")}}
        els.append(e)
    w(f"{A}/models/block/vlaggetjes.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                             "textures": {"particle": "guhs:block/vlaggetjes", "flags": "guhs:block/vlaggetjes"},
                                             "elements": els})
    w(f"{A}/blockstates/vlaggetjes.json", {"variants": {"axis=x": {"model": "guhs:block/vlaggetjes"},
                                                         "axis=z": {"model": "guhs:block/vlaggetjes", "y": 90}}})
    item_model("vlaggetjes", "guhs:block/vlaggetjes")
    self_drop("vlaggetjes")
    shaped("vlaggetjes", ["SSS", "PWY"], {"S": "minecraft:string", "P": "minecraft:pink_wool", "W": "minecraft:white_wool",
                                          "Y": "minecraft:yellow_wool"}, "guhs:vlaggetjes", 6)

    # --- bean bags and cushions in every wool colour ---
    w(f"{A}/models/block/zitzak.json", {"parent": "minecraft:block/block", "textures": {"particle": "#wool"}, "elements": [
        el([1, 0, 1], [15, 6, 15], "#wool"), el([2, 6, 2], [14, 8, 9], "#wool"), el([2, 6, 9], [14, 13, 15], "#wool"),
        el([3, 13, 10], [13, 14, 14], "#wool")]})
    w(f"{A}/models/block/kussen.json", {"parent": "minecraft:block/block", "textures": {"particle": "#wool"}, "elements": [
        el([2, 0, 2], [14, 3, 14], "#wool"), el([3, 3, 3], [13, 4, 13], "#wool"),
        el([1, 0, 1], [3, 1, 3], "#wool"), el([13, 0, 1], [15, 1, 3], "#wool"), el([1, 0, 13], [3, 1, 15], "#wool"), el([13, 0, 13], [15, 1, 15], "#wool")]})
    colours = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
               "brown", "green", "red", "black"]
    NL_COLOURS = {"white": "Witte", "orange": "Oranje", "magenta": "Magenta", "light_blue": "Lichtblauwe", "yellow": "Gele",
                  "lime": "Lichtgroene", "pink": "Roze", "gray": "Grijze", "light_gray": "Lichtgrijze", "cyan": "Cyaan",
                  "purple": "Paarse", "blue": "Blauwe", "brown": "Bruine", "green": "Groene", "red": "Rode", "black": "Zwarte"}
    for c in colours:
        for kind, en, nl in (("zitzak", "Bean Bag", "vadszak"), ("kussen", "Cushion", "guhkussen")):
            name = f"{c}_{kind}"
            w(f"{A}/models/block/{name}.json", {"parent": f"guhs:block/{kind}", "textures": {"wool": f"minecraft:block/{c}_wool"}})
            w(f"{A}/blockstates/{name}.json", {"variants": facing_states(name)})
            w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
            self_drop(name)
            lang(f"block.guhs.{name}", f"{c.replace('_', ' ').title()} {en}", f"{NL_COLOURS[c]} {nl}")
        shaped(f"{c}_zitzak", ["W W", "WWW"], {"W": f"minecraft:{c}_wool"}, f"guhs:{c}_zitzak")
        shaped(f"{c}_kussen", ["WFW"], {"W": f"minecraft:{c}_wool", "F": "minecraft:feather"}, f"guhs:{c}_kussen", 2)

    # --- guh flowers ---
    save(rehue(vanilla("block/dandelion"), 0.11, 1.2), "block", "kaasbloem.png")
    save(rehue(vanilla("block/allium"), 0.93, 1.1), "block", "roze_guhbloem.png")
    save(rehue(vanilla("block/poppy"), 0.07, 1.1), "block", "knabbelroos.png")
    oor = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(8, 16):
        oor.putpixel((8, y), (90, 160, 80, 255))
    for (x, y) in ((9, 11), (10, 10), (7, 13), (6, 12)):
        oor.putpixel((x, y), (110, 180, 90, 255))
    for cx in (5.5, 10.5):                                   # two big guh ears
        for y in range(1, 9):
            for x in range(16):
                if abs(x - cx) <= 2.2 - abs(y - 4.5) * 0.3:
                    inner = abs(x - cx) <= 1 and 2 <= y <= 7
                    oor.putpixel((x, y), (250, 150, 190, 255) if inner else (245, 200, 220, 255))
    save(oor, "block", "guhoortjes.png")
    for f in ("kaasbloem", "guhoortjes", "roze_guhbloem", "knabbelroos"):
        w(f"{A}/models/block/{f}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"guhs:block/{f}"}})
        w(f"{A}/blockstates/{f}.json", {"variants": {"": {"model": f"guhs:block/{f}"}}})
        item_model(f, f"guhs:block/{f}")
        self_drop(f)
        w(f"{A}/models/block/potted_{f}.json", {"parent": "minecraft:block/flower_pot_cross", "render_type": "minecraft:cutout",
                                                  "textures": {"plant": f"guhs:block/{f}"}})
        w(f"{A}/blockstates/potted_{f}.json", {"variants": {"": {"model": f"guhs:block/potted_{f}"}}})
        w(f"{D}/loot_table/blocks/potted_{f}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{f}"}]}]})
    for f, dye in (("kaasbloem", "yellow"), ("guhoortjes", "pink"), ("roze_guhbloem", "magenta"), ("knabbelroos", "orange")):
        shapeless(f"{dye}_dye_from_{f}", [f"guhs:{f}"], f"minecraft:{dye}_dye")
    flowers = ["guhs:kaasbloem", "guhs:guhoortjes", "guhs:roze_guhbloem", "guhs:knabbelroos"]
    add_tag("minecraft/tags/block/small_flowers", flowers)
    add_tag("minecraft/tags/item/small_flowers", flowers)
    add_tag("minecraft/tags/block/flower_pots", [f.replace("guhs:", "guhs:potted_") for f in flowers])

    # --- pink grass, the kaasknabbel plant and the knabbelboer ---
    save(colorize(vanilla("block/short_grass"), (250, 150, 200)), "block", "roze_gras.png")
    w(f"{A}/models/block/roze_gras.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": "guhs:block/roze_gras"}})
    w(f"{A}/blockstates/roze_gras.json", {"variants": {"": {"model": "guhs:block/roze_gras"}}})
    item_model("roze_gras", "guhs:block/roze_gras")
    w(f"{D}/loot_table/blocks/roze_gras.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "guhs:roze_gras", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}]},
        {"type": "minecraft:item", "name": "guhs:kaasknabbelzaadjes", "conditions": [{"condition": "minecraft:random_chance", "chance": 0.15}]}]}]}]})
    for age in range(8):
        img = rehue(vanilla(f"block/wheat_stage{age}"), 0.13 if age < 7 else 0.1, 1.2)
        if age == 7:                                            # ripe: kaas knabbels hanging in it
            for (x, y) in ((3, 3), (7, 2), (11, 4), (5, 6), (10, 7)):
                for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                    img.putpixel((x + dx, y + dy), (236, 150, 40, 255))
                img.putpixel((x, y), (250, 200, 90, 255))
        save(img, "block", f"kaasknabbelplant_stage{age}.png")
        w(f"{A}/models/block/kaasknabbelplant_stage{age}.json", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
                                                                   "textures": {"crop": f"guhs:block/kaasknabbelplant_stage{age}"}})
    w(f"{A}/blockstates/kaasknabbelplant.json", {"variants": {f"age={a}": {"model": f"guhs:block/kaasknabbelplant_stage{a}"} for a in range(8)}})
    ripe = [{"condition": "minecraft:block_state_property", "block": "guhs:kaasknabbelplant", "properties": {"age": "7"}}]
    w(f"{D}/loot_table/blocks/kaasknabbelplant.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "conditions": ripe, "functions": count_fn(2, 4)},
            {"type": "minecraft:item", "name": "guhs:kaasknabbelzaadjes"}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaasknabbelzaadjes", "functions": [
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:binomial_with_bonus_count",
             "parameters": {"extra": 3, "probability": 0.5714286}}]}], "conditions": ripe}]})
    add_tag("minecraft/tags/block/crops", ["guhs:kaasknabbelplant"])
    add_tag("minecraft/tags/block/bee_growables", ["guhs:kaasknabbelplant"])
    add_tag("minecraft/tags/block/maintains_farmland", ["guhs:kaasknabbelplant"])
    add_tag("minecraft/tags/item/villager_plantable_seeds", ["guhs:kaasknabbelzaadjes"])
    add_tag("minecraft/tags/block/mineable/axe", ["guhs:guh_stoel", "guhs:guh_tafel", "guhs:guh_bank", "guhs:guh_kast", "guhs:zaadbak"])
    add_tag("minecraft/tags/point_of_interest_type/acquirable_job_site", ["guhs:zaadbak"])
    # the zaadbak (workstation): a composter, pink, full of seeds
    for part, src in (("top", "composter_top"), ("front", "composter_side"), ("side", "composter_side")):
        a = np.asarray(rehue(vanilla(f"block/{src}"), 0.93, 1.2)).copy()
        if part == "top":
            a[a[..., 3] < 255] = (90, 60, 40, 255)
            for (x, y) in ((4, 5), (7, 4), (10, 6), (5, 9), (9, 10), (11, 9), (7, 7), (3, 11), (12, 4)):
                a[y, x] = (255, 210, 70, 255)
        save(Image.fromarray(a), "block", f"zaadbak_{part}.png")
    w(f"{A}/models/block/zaadbak.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": "guhs:block/zaadbak_top", "front": "guhs:block/zaadbak_front", "side": "guhs:block/zaadbak_side"}})
    w(f"{A}/blockstates/zaadbak.json", {"variants": facing_states("zaadbak")})
    w(f"{A}/models/item/zaadbak.json", {"parent": "guhs:block/zaadbak"})
    self_drop("zaadbak")
    shaped("zaadbak", ["SSS", "PCP"], {"S": "guhs:kaasknabbelzaadjes", "P": "minecraft:pink_wool", "C": "minecraft:composter"}, "guhs:zaadbak")
    robe = rehue(vanilla("entity/villager/profession/farmer"), 0.1, 1.2)
    save(robe, "entity", "villager", "profession", "knabbelboer.png")
    save(rehue(robe, 0.1, 0.8, 0.85), "entity", "zombie_villager", "profession", "knabbelboer.png")

    # --- recipes: food and furniture ---
    shaped("guh_taart", ["MMM", "SKS", "WPW"], {"M": "minecraft:milk_bucket", "S": "minecraft:sugar", "K": "guhs:kaas_knabbels",
                                                "W": "minecraft:wheat", "P": "minecraft:pink_dye"}, "guhs:guh_taart")
    shapeless("guh_cupcake", ["minecraft:wheat", "minecraft:sugar", "minecraft:egg", "minecraft:pink_dye"], "guhs:guh_cupcake", 3)
    for colour, dye in (("roze", "pink_dye"), ("mint", "lime_dye"), ("citroen", "yellow_dye"), ("choco", "cocoa_beans")):
        shapeless(f"macaron_{colour}", ["minecraft:sugar", "minecraft:egg", f"minecraft:{dye}"], f"guhs:macaron_{colour}", 4)
    shapeless("kaasfondue", ["minecraft:bowl", "guhs:kaas_knabbels", "guhs:kaas_knabbels", "guhs:kaas_knabbels", "minecraft:milk_bucket"],
              "guhs:kaasfondue")
    shapeless("kaasknabbel_milkshake", ["minecraft:milk_bucket", "guhs:kaas_knabbels", "minecraft:sugar", "minecraft:glass_bottle",
                                        "minecraft:glass_bottle", "minecraft:glass_bottle"], "guhs:kaasknabbel_milkshake", 3)
    shaped("guh_stoel", ["P  ", "PWP", "S S"], {"P": "minecraft:cherry_planks", "W": "minecraft:pink_wool", "S": "minecraft:stick"}, "guhs:guh_stoel", 2)
    shaped("guh_tafel", ["PPP", "S S", "S S"], {"P": "minecraft:cherry_planks", "S": "minecraft:stick"}, "guhs:guh_tafel")
    shaped("guh_bank", ["  W", "WWW", "PPP"], {"P": "minecraft:cherry_planks", "W": "minecraft:pink_wool"}, "guhs:guh_bank")
    shaped("guh_kast", ["PPP", "PCP", "PPP"], {"P": "minecraft:cherry_planks", "C": "minecraft:chest"}, "guhs:guh_kast")

    # --- worldgen: guh flowers and pink grass all over the Guhmension ---
    def patch_feature(name, states, tries, spread, count):
        w(f"{D}/worldgen/configured_feature/{name}.json", {"type": "minecraft:random_patch", "config": {
            "tries": tries, "xz_spread": spread, "y_spread": 2, "feature": {
                "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:weighted_state_provider",
                            "entries": [{"weight": wgt, "data": {"Name": s}} for s, wgt in states]}}},
                "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    {"type": "minecraft:would_survive", "state": {"Name": states[0][0]}}]}}]}}})
        w(f"{D}/worldgen/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]})
    patch_feature("guh_flowers", [("guhs:kaasbloem", 3), ("guhs:guhoortjes", 2), ("guhs:roze_guhbloem", 3), ("guhs:knabbelroos", 2)], 24, 6, 1)
    patch_feature("roze_gras", [("guhs:roze_gras", 1)], 32, 7, 2)
    for b in GUHMENSION_BIOMES:
        def veg(d, b=b):
            step = d["features"][9]
            for f in ("guhs:guh_flowers", "guhs:roze_gras"):
                if f not in step and not (b == "mikas_biome" and f == "guhs:guh_flowers"):
                    step.append(f)
        patch_json(f"{D}/worldgen/biome/{b}.json", veg)
    # guh villagers also in the new biomes
    patch_json(f"{R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
               lambda d: d["values"].update({"guhs:guh_sea": {"villager_type": "guhs:guh"}, "guhs:guh_kristalmijn": {"villager_type": "guhs:guh"}}))
    # picnics now have guh food
    add_loot("guh_picnic", [{"type": "minecraft:item", "name": "guhs:guh_cupcake", "weight": 3, "functions": count_fn(1, 4)},
                            {"type": "minecraft:item", "name": "guhs:macaron_roze", "weight": 2, "functions": count_fn(2, 6)},
                            {"type": "minecraft:item", "name": "guhs:macaron_mint", "weight": 2, "functions": count_fn(2, 6)},
                            {"type": "minecraft:item", "name": "guhs:kaasknabbel_milkshake", "weight": 2},
                            {"type": "minecraft:item", "name": "guhs:kaasfondue", "weight": 1}], rolls={"type": "minecraft:uniform", "min": 1, "max": 3})

    for key, en, nl in [
        ("block.guhs.guh_taart", "Guh Cake", "Guh-taart"), ("item.guhs.guh_taart", "Guh Cake", "Guh-taart"),
        ("item.guhs.guh_cupcake", "Guh Cupcake", "Guh-cupcake"),
        ("item.guhs.macaron_roze", "Pink Macaron", "Roze macaron"), ("item.guhs.macaron_mint", "Mint Macaron", "Mint-macaron"),
        ("item.guhs.macaron_citroen", "Lemon Macaron", "Citroenmacaron"), ("item.guhs.macaron_choco", "Chocolate Macaron", "Chocolademacaron"),
        ("item.guhs.kaasfondue", "Cheese Fondue", "Kaasfondue"),
        ("item.guhs.kaasknabbel_milkshake", "Kaasknabbel Milkshake", "Kaasknabbel-milkshake"),
        ("item.guhs.kaasknabbelzaadjes", "Kaasknabbel Seeds", "Kaasknabbelzaadjes"),
        ("block.guhs.kaasknabbelplant", "Kaasknabbel Plant", "Kaasknabbelplant"),
        ("block.guhs.roze_gras", "Pink Grass", "Roze gras"),
        ("block.guhs.zaadbak", "Seed Tray", "Zaadbak"),
        ("entity.minecraft.villager.guhs.knabbelboer", "Knabbelboer", "Knabbelboer"),
        ("block.guhs.guh_stoel", "Guh Chair", "Guh-stoel"), ("block.guhs.guh_tafel", "Guh Table", "Guh-tafel"),
        ("block.guhs.guh_bank", "Guh Sofa", "Guh-bank"), ("block.guhs.guh_kast", "Guh Cupboard", "Guh-kast"),
        ("block.guhs.lampion_roze", "Pink Paper Lantern", "Roze lampion"), ("block.guhs.lampion_geel", "Yellow Paper Lantern", "Gele lampion"),
        ("block.guhs.lampion_mint", "Mint Paper Lantern", "Mintgroene lampion"),
        ("block.guhs.vlaggetjes", "Bunting", "Vlaggetjes"),
        ("block.guhs.kaasbloem", "Cheese Flower", "Kaasbloem"), ("block.guhs.guhoortjes", "Guh Ears", "Guhoortjes"),
        ("block.guhs.roze_guhbloem", "Pink Guh Flower", "Roze guhbloem"), ("block.guhs.knabbelroos", "Knabbel Rose", "Knabbelroos"),
        ("block.guhs.potted_kaasbloem", "Potted Cheese Flower", "Kaasbloem in pot"),
        ("block.guhs.potted_guhoortjes", "Potted Guh Ears", "Guhoortjes in pot"),
        ("block.guhs.potted_roze_guhbloem", "Potted Pink Guh Flower", "Roze guhbloem in pot"),
        ("block.guhs.potted_knabbelroos", "Potted Knabbel Rose", "Knabbelroos in pot"),
    ]:
        lang(key, en, nl)


# =====================================================================================================================
# 2.0.1: lava-thick kaas saus, guh-headed furniture and lily pads, lampgions, Dutch njeg-dialogue
# =====================================================================================================================
DIALOGUE = {  # quest dialogue: Dutch in every language (with plenty of njeg, vads and vahoeg)
    "quest.guhs.vadsig.start": "Guh... guhhh... NJEG! Die vadsige Mika's hebben mijn kleine Guhbert ontvoerd! Volg dit Mika-spoorkompas naar hun kamp en breng hem terug. Alsjeblieft, vahoege held!",
    "quest.guhs.vadsig.camp": "Njeg, njeg... Het kompas wijst naar het Mika-kamp. Versla de Mika-baas en bevrijd mijn Guhbert! Vads op!",
    "quest.guhs.vadsig.cake": "GUHBERT! Vahoeg! Maar... NJEG, mijn taart voor het welkomstfeest is weg! Volg de taartkruimels naar een guh-picknick. En neem 3 guh-ballonnen mee, anders is het geen echt vadsig feestje!",
    "quest.guhs.vadsig.need": "Voor het feest heb ik de verloren guh-taart (%s) en %s/%s guh-ballonnen nodig. Ballonnen vind je in picknickkisten, of maak ze van roze wol en touw. Njeg, wat wordt dat een vadsig feestje!",
    "quest.guhs.have": "gevonden",
    "quest.guhs.missing": "nog kwijt",
    "quest.guhs.vadsig.party": "VAHOEGE VADS! FEEEEST! Dankjewel, dankjewel! Hier, mijn buikfluitje: vanaf nu heb je je eigen guhmaag. Njeg njeg!",
    "quest.guhs.vadsig.unlocked": "Guhmaag ontgrendeld! Blaas op het buikfluitje of druk op G.",
    "quest.guhs.vadsig.done": "Guh guh! Guhbert en ik zijn zo vadsig blij. Veel plezier in je maag, vahoege vriend! Njeg!",
    "quest.guhs.cake.found": "Je hebt de verloren guh-taart gevonden! Breng hem met 3 guh-ballonnen naar Moeder Vadsig.",
    "quest.guhs.mika.challenge": "NJEG! Wil je die kleine guh terug? Versla me dan met steen-papier-schaar. Drie keer op rij! Njeg njeg njeg!",
    "quest.guhs.mika.play": "NJEG! Nog een keer? Vads jij maar op.",
    "quest.guhs.mika.won": "NJEG HEHE! Ik koos %s. Terug naar nul, vadsig ding!",
    "quest.guhs.mika.vads_reveal": "Njeg hehe... zolang jij het geheime VADS-gebaar niet kent, win je NOOIT van mij. Oeps. Njeg. Dat had ik niet mogen zeggen...",
    "quest.guhs.mika.again": "Grrr... njeg! Nog een keer!",
    "quest.guhs.mika.vadsed": "NJEG... IK BEN GEVADST!",
    "quest.guhs.guhbert.free": "Guh! Guh guh! Vahoeg! (Guhbert is vrij en volgt jou)",
    "quest.guhs.dentist.no_maag": "Wijd open, njeg! O, je hebt nog geen maag. Vraag het Moeder Vadsig.",
    "quest.guhs.dentist.max": "Je maag is al %s breed. Nog vadsiger is niet gezond, njeg!",
    "quest.guhs.dentist.job": "Je maag is %s breed. Voor %s moet je me dit brengen, vahoeg:",
    "quest.guhs.dentist.done": "Rekken... rekken... VADS! Je maag is nu %s breed.",
    "quest.guhs.need.knabbels": "%s/%s kaasknabbels",
    "quest.guhs.need.ingots": "%s/%s vahoege-vadsstaven",
    "quest.guhs.need.crystals": "%s/%s guhkristallen",
    "quest.guhs.need.big_mika": "  - versla een Grote Mika",
    "quest.guhs.need.golden": "  - neem een Gouden Guh mee",
    "quest.guhs.enzyme.not_yours": "Blub. Njeg. Ik luister alleen naar %s.",
    "quest.guhs.sled.start": "Guh... njeg, mijn slee is kapot! Ik heb een sleeglijder (guhgrotten), een guh-belletje (guhdorpen) en een roze lint (de kleermaker) nodig.",
    "quest.guhs.sled.parts": "Nog missende onderdelen voor de slee, vahoeg:",
    "quest.guhs.sled.thanks": "Wiee, VADS! Hij doet het weer! Neem deze slee en mijn sleebouwersboek - bouw je eigen vahoege baan!",
    "quest.guhs.sled.unlocked": "Je kunt nu sleerails maken (het boek blijft in het rooster).",
    "quest.guhs.sled.done": "Tingeling! Njeg njeg, veel plezier op de rails!",
}


def fixes_2_0_1():
    # --- kaas saus: thick and bubbly like lava, but cheese (and it doesn't burn) ---
    for part in ("still", "flow"):
        lava = vanilla(f"block/lava_{part}")
        a = np.asarray(lava).astype(np.float32)
        lum = a[..., :3].mean(-1, keepdims=True) / 255
        dark, light = np.array([226, 146, 28]), np.array([255, 238, 140])
        rgb = dark + (light - dark) * np.clip((lum - 0.35) / 0.6, 0, 1)
        out = np.concatenate([rgb, a[..., 3:]], -1).astype(np.uint8)
        save(Image.fromarray(out), "block", f"kaas_saus_{part}.png")
        meta = json.loads(_JAR.read(f"assets/minecraft/textures/block/lava_{part}.png.mcmeta"))
        with open(os.path.join(TEX, "block", f"kaas_saus_{part}.png.mcmeta"), "w") as f:
            json.dump(meta, f)
    w(f"{A}/models/block/kaas_saus.json", {"textures": {"particle": "guhs:block/kaas_saus_still"}})

    # --- guh textures for the furniture: the real guh face, fur and ear from the guh's own texture ---
    guh = Image.open(os.path.join(TEX, "entity", "guh.png")).convert("RGBA")
    fur = guh.crop((178, 2, 226, 34)).resize((64, 64), Image.NEAREST)  # (the body's top: plain fur)
    save(fur, "block", "guh_vacht.png")
    face = fur.copy()
    face.paste(guh.crop((0, 196, 56, 238)), (4, 12))
    save(face, "block", "guh_gezicht.png")
    ear = fur.copy()
    for y in range(64):
        for x in range(64):
            if ((x - 32) / 18) ** 2 + ((y - 34) / 24) ** 2 <= 1:
                ear.putpixel((x, y), (214, 150, 200, 255))
    save(ear, "block", "guh_oor.png")

    F, V, O = "#face", "#fur", "#ear"
    gt = {"particle": "guhs:block/guh_vacht", "face": "guhs:block/guh_gezicht", "fur": "guhs:block/guh_vacht", "ear": "guhs:block/guh_oor",
          "wood": "minecraft:block/cherry_planks", "pink": "minecraft:block/pink_wool", "white": "minecraft:block/white_wool"}

    def full_uv(e):
        for f in e["faces"].values():
            f["uv"] = [0, 0, 16, 16]
        return e

    def head(frm, to):
        """A guh head: the face on the front (north), fur all around."""
        e = el(frm, to, V)
        e["faces"]["north"] = {"texture": F}
        return full_uv(e)

    def ears(y, z0, z1, xs=((2, 5), (11, 14)), h=4):
        out = []
        for x0, x1 in xs:
            e = el([x0, y, z0], [x1, y + h, z1], V)
            e["faces"]["north"] = {"texture": O}
            out.append(full_uv(e))
        return out
    legs = lambda hgt, a=2, b=4, c=12, d=14: [el([a, 0, a], [b, hgt, b], "#wood"), el([c, 0, a], [d, hgt, b], "#wood"),
                                              el([a, 0, c], [b, hgt, d], "#wood"), el([c, 0, c], [d, hgt, d], "#wood")]
    furniture_model("guh_stoel", legs(7) + [el([2, 7, 2], [14, 9, 14], "#wood"), el([3, 9, 3], [13, 10, 11], "#pink"),
                                           head([2, 9, 11], [14, 20, 14])] + ears(20, 11.5, 13), gt)
    furniture_model("guh_bank", [el([0, 0, 1], [16, 8, 15], "#wood"), el([1, 8, 1], [15, 10, 11], "#pink"),
                                 head([0, 8, 11], [16, 18, 15]), el([0, 8, 1], [2, 12, 11], V), el([14, 8, 1], [16, 12, 11], V)]
                    + ears(18, 12, 14, xs=((1, 4), (12, 15))), gt)
    top = el([0, 13, 0], [16, 16, 16], "#wood")
    top["faces"]["up"] = {"texture": F}
    furniture_model("guh_tafel", legs(13, 1, 4, 12, 15) + [top], gt)
    for opened in ("", "_open"):
        cube = el([0, 0, 0], [16, 16, 16], V)
        cube["faces"]["north"] = {"texture": "#front"}
        w(f"{A}/models/block/guh_kast{opened}.json", {"parent": "minecraft:block/block", "textures": {
            **gt, "front": "guhs:block/guh_gezicht" if not opened else "guhs:block/guh_kast_front_open"},
            "elements": [cube] + ears(16, 6.5, 8.5, xs=((1, 5), (11, 15)), h=5)})
    # bean bags and cushions get little guh ears too
    w(f"{A}/models/block/zitzak.json", {"parent": "minecraft:block/block", "textures": {"particle": "#wool"}, "elements": [
        el([1, 0, 1], [15, 6, 15], "#wool"), el([2, 6, 2], [14, 8, 9], "#wool"), el([2, 6, 9], [14, 13, 15], "#wool"),
        el([3, 13, 10], [13, 14, 14], "#wool"), el([3, 13, 11], [6, 16, 13], "#wool"), el([10, 13, 11], [13, 16, 13], "#wool")]})
    w(f"{A}/models/block/kussen.json", {"parent": "minecraft:block/block", "textures": {"particle": "#wool"}, "elements": [
        el([2, 0, 2], [14, 3, 14], "#wool"), el([3, 3, 3], [13, 4, 13], "#wool"),
        el([3, 3, 11], [5, 6, 12.5], "#wool"), el([11, 3, 11], [13, 6, 12.5], "#wool"),
        el([1, 0, 1], [3, 1, 3], "#wool"), el([13, 0, 1], [15, 1, 3], "#wool"), el([1, 0, 13], [3, 1, 15], "#wool"), el([13, 0, 13], [15, 1, 15], "#wool")]})

    # --- guh lily pads: a guh head seen from above ---
    pad = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body, dark = (236, 150, 190), (206, 110, 160)
    for y in range(16):
        for x in range(16):
            inhead = ((x - 7.5) / 6.6) ** 4 + ((y - 9.5) / 5.6) ** 4 <= 1
            inear = any(math.dist((x, y), c) <= 2.4 for c in ((3.2, 3.6), (11.8, 3.6)))
            if inhead or inear:
                pad.putpixel((x, y), body + (255,))
    for c in ((3.2, 3.6), (11.8, 3.6)):                   # inner ears
        for y in range(16):
            for x in range(16):
                if math.dist((x, y), c) <= 1.2:
                    pad.putpixel((x, y), (250, 190, 215, 255))
    for ex in (4, 10):                                     # big guh eyes
        for (dx, dy), col in (((0, 0), (20, 20, 40)), ((1, 0), (20, 20, 40)), ((0, 1), (90, 170, 235)), ((1, 1), (60, 120, 200)),
                              ((0, 2), (20, 20, 40)), ((1, 2), (20, 20, 40))):
            pad.putpixel((ex + dx, 8 + dy), col + (255,))
        pad.putpixel((ex + 1, 8), (255, 255, 255, 255))
    for x in (7, 8):
        pad.putpixel((x, 12), (180, 90, 140, 255))
    save(pad, "block", "guh_waterlelie.png")

    # --- names ---
    for colour, en, nl in (("roze", "Pink", "Roze"), ("geel", "Yellow", "Gele"), ("mint", "Mint", "Mintgroene")):
        lang(f"block.guhs.lampion_{colour}", f"{en} Lampgion", f"{nl} lampgion")
    lang("item.guhs.guh_compass.how", "Always points to the nearest one in this dimension; hold it to see how far",
         "Wijst altijd naar de dichtstbijzijnde in deze dimensie; houd hem vast om te zien hoe ver")
    lang("gui.guhs.rps.rules", "Win 3 times in a row.", "Win 3 keer op rij.")
    lang("gui.guhs.menu.wander", "Rondvadsen: %s", "Rondvadsen: %s")
    lang("gui.guhs.wardrobe.armor", "Pantser", "Pantser")
    lang("gui.guhs.menu.wardrobe", "Kleding, pantser & rugzak", "Kleding, pantser & rugzak")
    lang("item.guhs.guh_compass.distance", "About %s blocks away", "Ongeveer %s blokken ver")
    lang("item.guhs.guh_compass.none", "None nearby", "Geen in de buurt")
    lang("item.guhs.taartkruimels.lore", "A trail of crumbs... to the nearest guh picnic (also in the overworld)",
         "Een spoor van kruimels... naar de dichtstbijzijnde guh-picknick (ook in de overworld)")
    for key, text in DIALOGUE.items():
        lang(key, text, text)


# =====================================================================================================================
# 2.0.1: the guh blossom tree (guhbloesem): tiny guhs drift down from its leaves
# =====================================================================================================================
def guhbloesem():
    # guh-fur bark: the cherry bark in the guh's own mauve fur colours, and now and then a block with a little guh face
    dark, light = (120, 70, 118), (214, 172, 218)
    bark = ramp(vanilla("block/cherry_log"), dark, light)
    save(bark, "block", "guhbloesem_log.png")
    save(ramp(vanilla("block/cherry_log_top"), dark, light), "block", "guhbloesem_log_top.png")
    face = bark.copy()
    paint(face, 0, 6, ["....ew....ew....",
                       "....kk....kk....",
                       "....kk....kk....",
                       "...b........b...",
                       "..bb..m..m..bb..",
                       ".......mm......."],
          {"e": (255, 255, 255, 255), "w": (40, 50, 100, 255), "k": (40, 50, 100, 255), "b": (240, 150, 190, 255), "m": (60, 20, 50, 255)})
    save(face, "block", "guhbloesem_log_gezicht.png")
    save(rehue(vanilla("block/cherry_planks"), 0.84, 0.9, 1.08), "block", "guhbloesem_planks.png")
    leaves = rehue(vanilla("block/cherry_leaves"), 0.87, 1.0, 1.02)
    save(leaves, "block", "guhbloesem_leaves.png")
    save(rehue(vanilla("block/cherry_sapling"), 0.87, 1.0, 1.0), "block", "guhbloesem_sapling.png")
    # the falling tiny guhs: four little guh heads
    for i, (body, inner) in enumerate((((236, 160, 205), (250, 200, 225)), ((200, 160, 225), (230, 200, 240)),
                                       ((250, 180, 210), (255, 220, 235)), ((220, 140, 190), (245, 190, 215)))):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        head = ["........", ".xx..xx.", ".xixxix.", ".xxxxxx.", "xxxxxxxx", "xekxxkex", "xxxxxxxx", ".xxmmxx."]
        paint(img, 0, 0, head, {"x": body + (255,), "i": inner + (255,), "e": (255, 255, 255, 255), "k": (40, 60, 120, 255),
                                "m": (190, 90, 140, 255)})
        save(img, "particle", f"guh_blaadje_{i}.png")
    w(f"{A}/particles/guh_blaadje.json", {"textures": [f"guhs:guh_blaadje_{i}" for i in range(4)]})

    # models, blockstates, items
    w(f"{A}/models/block/guhbloesem_log.json", {"parent": "minecraft:block/cube_column",
                                                  "textures": {"end": "guhs:block/guhbloesem_log_top", "side": "guhs:block/guhbloesem_log"}})
    w(f"{A}/models/block/guhbloesem_log_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                             "textures": {"end": "guhs:block/guhbloesem_log_top", "side": "guhs:block/guhbloesem_log"}})
    w(f"{A}/models/block/guhbloesem_log_gezicht.json", {"parent": "minecraft:block/cube_column",
                                                          "textures": {"end": "guhs:block/guhbloesem_log_top", "side": "guhs:block/guhbloesem_log_gezicht"}})
    w(f"{A}/blockstates/guhbloesem_log.json", {"variants": {
        "axis=y": [{"model": "guhs:block/guhbloesem_log", "weight": 4}, {"model": "guhs:block/guhbloesem_log", "y": 90, "weight": 4},
                   {"model": "guhs:block/guhbloesem_log_gezicht", "weight": 1}, {"model": "guhs:block/guhbloesem_log_gezicht", "y": 180, "weight": 1}],
        "axis=z": {"model": "guhs:block/guhbloesem_log_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/guhbloesem_log_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/guhbloesem_log.json", {"parent": "guhs:block/guhbloesem_log"})
    simple_block("guhbloesem_planks")
    simple_block("guhbloesem_leaves", render_type="minecraft:cutout_mipped")
    w(f"{A}/models/block/guhbloesem_sapling.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                      "textures": {"cross": "guhs:block/guhbloesem_sapling"}})
    w(f"{A}/blockstates/guhbloesem_sapling.json", {"variants": {"": {"model": "guhs:block/guhbloesem_sapling"}}})
    item_model("guhbloesem_sapling", "guhs:block/guhbloesem_sapling")
    for b in ("guhbloesem_log", "guhbloesem_planks", "guhbloesem_sapling"):
        self_drop(b)
    silk_or_shears = {"condition": "minecraft:any_of", "terms": [
        {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
        {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
    w(f"{D}/loot_table/blocks/guhbloesem_leaves.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:guhbloesem_leaves", "conditions": [silk_or_shears]},
            {"type": "minecraft:item", "name": "guhs:guhbloesem_sapling", "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "functions": count_fn(1, 2)}],
         "conditions": [{"condition": "minecraft:inverted", "term": silk_or_shears},
                        {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                         "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}]})
    add_tag("minecraft/tags/block/logs_that_burn", ["guhs:guhbloesem_log"])
    add_tag("minecraft/tags/item/logs_that_burn", ["guhs:guhbloesem_log"])
    add_tag("minecraft/tags/block/planks", ["guhs:guhbloesem_planks"])
    add_tag("minecraft/tags/item/planks", ["guhs:guhbloesem_planks"])
    add_tag("minecraft/tags/block/leaves", ["guhs:guhbloesem_leaves"])
    add_tag("minecraft/tags/item/leaves", ["guhs:guhbloesem_leaves"])
    add_tag("minecraft/tags/block/saplings", ["guhs:guhbloesem_sapling"])
    add_tag("minecraft/tags/item/saplings", ["guhs:guhbloesem_sapling"])
    add_tag("minecraft/tags/block/mineable/axe", ["guhs:guhbloesem_log", "guhs:guhbloesem_planks"])
    add_tag("minecraft/tags/block/mineable/hoe", ["guhs:guhbloesem_leaves"])
    shapeless("guhbloesem_planks", ["guhs:guhbloesem_log"], "guhs:guhbloesem_planks", 4)
    # Dutch names everywhere, with a guh, vads or VAHOEG tucked in where it fits
    for key, name in [
        ("block.guhs.guhbloesem_log", "Guhbloesemstam"), ("block.guhs.guhbloesem_planks", "Vadsplanken"),
        ("block.guhs.guhbloesem_leaves", "Guhbloesem"), ("block.guhs.guhbloesem_sapling", "Guhbloesemboompje"),
        ("item.guhs.macaron_roze", "Roze guhcaron"), ("item.guhs.macaron_mint", "Mint-guhcaron"),
        ("item.guhs.macaron_citroen", "Citroenguhcaron"), ("item.guhs.macaron_choco", "Chocoguhcaron"),
        ("item.guhs.kaasfondue", "Kaasfondguh"), ("item.guhs.kaasknabbel_milkshake", "Vahoege kaasknabbelshake"),
        ("block.guhs.roze_slijmblok", "Roze guhslijmblok"), ("block.guhs.guh_stoel", "Guhstoel"), ("block.guhs.guh_bank", "Vadszetel"),
        ("block.guhs.guh_tafel", "Guhtafel"), ("block.guhs.guh_kast", "Vadskast"), ("block.guhs.vlaggetjes", "Guhvlaggetjes"),
        ("block.guhs.zaadbak", "Vadszaadbak"), ("item.guhs.sleebouwersboek", "Vahoege sleebouwersboek"),
        ("block.guhs.guh_waterlelie", "Guhlelie"), ("block.guhs.roze_gras", "Roze guhgras"),
        ("item.guhs.gebakken_guh_vis", "Vadsig gebakken guhvis"), ("block.guhs.guh_taart", "Guhtaart"), ("item.guhs.guh_taart", "Guhtaart"),
        ("item.guhs.guh_cupcake", "Guhcupcake"), ("item.guhs.guh_slee", "Guhslee"), ("entity.guhs.guh_slee", "Guhslee"),
        ("block.guhs.knabbelkorf", "Knabbelkorf"), ("item.guhs.kaashoning", "Kaashoning"),
    ]:
        lang(key, name, name)

    # the tree itself: vanilla cherry shape, our blocks, growing on pink wool too
    tree = json.loads(_JAR.read("data/minecraft/worldgen/configured_feature/cherry.json"))
    cfg = tree["config"]
    cfg["trunk_provider"] = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:guhbloesem_log", "Properties": {"axis": "y"}}}
    cfg["foliage_provider"] = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:guhbloesem_leaves",
                               "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}}
    cfg["dirt_provider"] = {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:pink_wool"}}
    w(f"{D}/worldgen/configured_feature/guhbloesem.json", tree)
    w(f"{D}/worldgen/placed_feature/guhbloesem_rare.json", {"feature": "guhs:guhbloesem", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 14}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                     "state": {"Name": "guhs:guhbloesem_sapling", "Properties": {"stage": "0"}}}},
        {"type": "minecraft:biome"}]})
    for b in GUHMENSION_BIOMES:
        def trees(d, b=b):
            if "guhs:guhbloesem_rare" not in d["features"][9]:
                d["features"][9].insert(0, "guhs:guhbloesem_rare")
        patch_json(f"{D}/worldgen/biome/{b}.json", trees)


# =====================================================================================================================
# 2.0.1: cuter guh crystals: little see-through crystal guh heads, pastel mine walls
# =====================================================================================================================
def crystal_face(img, x0, y0, scale=1):
    """Two big shiny guh eyes and a little smile on a 16x16 crystal texture."""
    for ex in (4, 10):
        for dx in range(2):
            for dy in range(3):
                img.putpixel((x0 + ex + dx, y0 + 7 + dy), (60, 40, 90, 255))
        img.putpixel((x0 + ex + 1, y0 + 7), (255, 255, 255, 255))
        img.putpixel((x0 + ex, y0 + 9), (120, 170, 240, 255))
    for x in (7, 8):
        img.putpixel((x0 + x, y0 + 12), (200, 90, 150, 255))
    img.putpixel((x0 + 6, y0 + 11), (200, 90, 150, 255))
    img.putpixel((x0 + 9, y0 + 11), (200, 90, 150, 255))


def crystals_v2():
    def gem(seed):
        rng = random.Random(seed)
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                t = (x + y) / 30
                c = (int(255 - 25 * t), int(190 - 40 * t), int(225 + 20 * t))
                if (x - y) % 7 == 0:
                    c = (255, 235, 248)                          # facet shine lines
                a = 205 if 0 < x < 15 and 0 < y < 15 else 235
                img.putpixel((x, y), c + (a,))
        for _ in range(5):                                     # sparkles
            img.putpixel((rng.randrange(1, 15), rng.randrange(1, 15)), (255, 255, 255, 255))
        return img
    body = gem(1)
    save(body, "block", "guh_kristal_gem.png")
    face = gem(2)
    crystal_face(face, 0, 0)
    save(face, "block", "guh_kristal_gem_face.png")
    # the cluster: a little crystal guh head with ears, growing from the surface it's on
    def e(frm, to, tex, north=None):
        el_ = {"from": frm, "to": to, "faces": {f: {"texture": tex, "uv": [0, 0, 16, 16]} for f in ("down", "up", "north", "south", "west", "east")}}
        if north:
            el_["faces"]["north"]["texture"] = north
        return el_
    w(f"{A}/models/block/guh_kristal_cluster.json", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
        "textures": {"particle": "guhs:block/guh_kristal_gem", "gem": "guhs:block/guh_kristal_gem", "face": "guhs:block/guh_kristal_gem_face"},
        "elements": [e([3.5, 0, 3.5], [12.5, 7, 12.5], "#gem", "#face"), e([4, 7, 5.5], [6.5, 9.5, 8], "#gem"),
                     e([9.5, 7, 5.5], [12, 9.5, 8], "#gem")]})
    w(f"{A}/models/item/guh_kristal_cluster.json", {"parent": "guhs:block/guh_kristal_cluster"})
    # pastel mine walls, crystal block and lamp
    save(colorize(vanilla("block/calcite"), (250, 205, 225)), "block", "guh_kristalsteen.png")
    save(rehue(vanilla("block/amethyst_block"), 0.9, 0.8, 1.15), "block", "guh_kristal_blok.png")
    lamp = rehue(vanilla("block/amethyst_block"), 0.92, 0.5, 1.3)
    crystal_face(lamp, 0, -1)
    save(lamp, "block", "guh_kristal_lamp.png")
    save(grid(ITEMS["guh_kristal"], ITEM_PAL), "item", "guh_kristal.png")


# =====================================================================================================================
# 2.0.1: the stomach story (you're inside Moeder Vadsig), quest advancements for FTB Quests, the shrine compass
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["vadsig_met", "guhbert_free", "cake_found", "maag_unlocked", "maag_64", "maag_80", "maag_96", "maag_112",
                      "maag_128", "sled_repaired", "launched"] + [f"seen_{v}" for v in VARIANT_DEX] + \
                     [f"found_{c}" for c in ("guh_bee", "guh_slime", "guh_vis", "nether_mika", "mika")]


def stomach_story():
    # hidden advancements (no display): granted by the mod, checked by the FTB Quests chapter
    for name in QUEST_ADVANCEMENTS:
        w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for v in VARIANT_DEX:
        w(f"{D}/advancement/quest/tamed_{v}.json", {"criteria": {"done": {"trigger": "minecraft:tame_animal", "conditions": {
            "entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                        "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"%s\"}" % v}}]}}}})
    w(f"{D}/advancement/quest/ride_ender.json", {"criteria": {"done": {"trigger": "minecraft:started_riding", "conditions": {
        "player": [{"condition": "minecraft:entity_properties", "entity": "this",
                    "predicate": {"vehicle": {"type": "guhs:guh", "nbt": "{Variant:\"ender\"}"}}}]}}}})
    w(f"{D}/advancement/quest/ride_sled.json", {"criteria": {"done": {"trigger": "minecraft:started_riding", "conditions": {
        "player": [{"condition": "minecraft:entity_properties", "entity": "this",
                    "predicate": {"vehicle": {"type": "guhs:guh_slee"}}}]}}}})

    # the shrine compass: gold frames
    overrides = []
    for i in range(32):
        src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
        frame = f"heiligdom_kompas_{i:02d}"
        save(recolour(src, hue=0.13, sat=1.3, val=1.05, only=pinkish), "item", f"{frame}.png")
        item_model(frame)
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": "guhs:item/heiligdom_kompas_00"})
    w(f"{A}/models/item/heiligdom_kompas.json", {"parent": "minecraft:item/generated",
                                                  "textures": {"layer0": "guhs:item/heiligdom_kompas_16"}, "overrides": overrides})

    texts = {
        "item.guhs.heiligdom_kompas": "Heiligdomkompas",
        "item.guhs.heiligdom_kompas.lore": "Wijst naar het Vadsig-heiligdom van Moeder Vadsig (Guhmensie). Te koop bij de vadstemmer.",
        "quest.guhs.next": "Volgende stap: %s",
        "quest.guhs.next.camp": "volg het Mika-spoorkompas naar het Mika-kamp in de Guhmensie (houd het vast voor de afstand)",
        "quest.guhs.next.back_to_vadsig": "breng Guhbert terug naar Moeder Vadsig in het Vadsig-heiligdom",
        "quest.guhs.next.picnic": "volg de taartkruimels naar een guh-picknick en loop over het kleed om de taart te vinden",
        "quest.guhs.next.balloons": "neem 3 guh-ballonnen (picknickkisten, of maak ze van roze wol + touw) en de taart mee naar Moeder Vadsig",
        "quest.guhs.vadsig.swallow": "Kom eens hier, vahoege held... NJEG... *HAP!*",
        "quest.guhs.swallowed": "Moeder Vadsig heeft je ingeslikt! Dit is jouw plekje in haar buik: je eigen guhmaag. Blaas op het buikfluitje of druk op G om terug te gaan.",
        "sign.guhs.maag.lobby1": "Welkom in de",
        "sign.guhs.maag.lobby2": "mond van Moeder Vadsig!",
        "quest.guhs.need.item": "%s/%s %s",
        "quest.guhs.need.guhdex": "  - %s/%s guhsoorten in je Guhdex",
        # the dentist chapters: stories inside her belly
        "quest.guhs.dentist.story.krampjes": "Njeg... hoor je dat gerommel? Moeder Vadsig heeft maagkrampjes van al die vadsige hapjes. Als ik haar maag wat oprek, heb jij meer ruimte en zij minder pijn. Vahoeg!",
        "quest.guhs.dentist.done.krampjes": "Rekken... VADS! De krampjes zijn weg en je maag is nu %s breed. Moeder Vadsig zucht tevreden.",
        "quest.guhs.dentist.story.mika": "Njeg! Er is een Mika-geur in haar buik... Een Grote Mika heeft ooit haar lievelingsknuffel ingepikt en nu durft ze niet goed te eten. Versla een Grote Mika, dan kan ik verder rekken.",
        "quest.guhs.dentist.done.mika": "Die Mika-geur is weg, vahoeg! Moeder Vadsig durft weer te smullen: je maag is nu %s breed.",
        "quest.guhs.dentist.story.goud": "Moeder Vadsig mist haar gouden neefje... Een Gouden Guh op visite zou haar zo blij maken dat haar buik nog verder uitzet. Neem er een mee (tam, in de buurt).",
        "quest.guhs.dentist.done.goud": "GOUD! Ze is zo vadsig blij dat haar buik zomaar groeit: je maag is nu %s breed.",
        "quest.guhs.dentist.story.zacht": "Haar maagwand is een beetje stug geworden, njeg. Guhslijm en kaashoning maken hem weer lekker zacht en rekbaar.",
        "quest.guhs.dentist.done.zacht": "Zacht als een kussen! Je maag is nu %s breed. Vahoege vads!",
        "quest.guhs.dentist.story.bloesem": "De laatste en grootste rek, njeg! Moeder Vadsig wil een bloesemtuin in haar buik. Breng guhbloesemboompjes en kristallen, en laat zien dat je elke guhsoort kent (Guhdex).",
        "quest.guhs.dentist.done.bloesem": "VAHOEGE VADS! Moeder Vadsig heeft nu een hele tuin in haar buik: je maag is %s breed. Groter kan echt niet meer!",
        "quest.guhs.dentist.max": "Je maag is al %s breed. Nog vadsiger past niet in Moeder Vadsig, njeg!",
    }
    belly = ["Njeg... wat kriebelt er toch in mijn buik? O, jij bent het!", "Hmm, ik heb wel trek in kaasknabbels... niet opeten wat daar ligt hoor!",
             "Guh guh... *rommel rommel*... sorry, dat was mijn buik.", "Zit je lekker daarbinnen, vahoege vriend?",
             "Niet te hard springen, njeg, dat kietelt!", "Guhbert zegt hoi! Hij zwaait naar mijn buik.",
             "Soms voel ik je bouwen... wat maak je daar, iets vadsigs?", "*hik* ...oeps. Njeg. Alles goed daarbinnen?"]
    for i, line in enumerate(belly):
        texts[f"quest.guhs.vadsig.belly.{i}"] = line
    for k, v in texts.items():
        lang(k, v, v)


# =====================================================================================================================
# lang: merged into the files make_resources.py wrote
# =====================================================================================================================
def write_lang():
    nl = json.load(open(f"{A}/lang/nl_nl.json", encoding="utf-8"))
    nl.update(NL)
    en = json.load(open(f"{A}/lang/en_us.json", encoding="utf-8"))
    en.update(EN)
    en.update(nl)  # everything in Dutch, whatever language the game is set to
    w(f"{A}/lang/nl_nl.json", nl)
    w(f"{A}/lang/en_us.json", en)
    print(f"lang: {len(EN)} keys")


# =====================================================================================================================
# 2.1.0: the guh kermis (a rare fair with a real sled coaster), coaster rail pieces, the kermis outfit, menu tooltips
# =====================================================================================================================
KERMIS_ICONS = {
    "sleerail_drop": [
        "................", "...........kpk..", "..........kwBk..", "..........kpBk..", ".........kwBk...",
        ".........kpBk...", "........kwBk....", "........kpBk....", ".......kwBk.....", "......kpBk......",
        ".....kwBk.......", "...kkpBk........", ".kkpwBk.........", "kpwBBk..........", "kkkkkkkkkkkkkkkk",
        "................"],
    "sleerail_kurkentrekker": [
        "................", ".....kkkkkk.....", "...kkpwpwpwkk...", "..kpk......kpk..", "..kwk......kwk..",
        "...kkpwpwpwkk...", ".....kkkkkk.....", "...kkpwpwpwkk...", "..kpk......kpk..", "..kwk......kwk..",
        "...kkpwpwpwkk...", ".....kkkkkk.....", "......kBBk......", "......kBBk......", ".....kkkkkk.....",
        "................"],
    "sleerail_schans": [
        "................", "................", "................", "..........w.w...", "...........w....",
        "................", ".............kpk", "............kwBk", "...........kpBk.", "..........kwBk..",
        "........kkpBk...", "kkkkkkkpwBBk....", "kpwpwpwBBBk.....", "kBBBBBBBBk..rrr.", "kkkkkkkkkk.rrrrr",
        "................"],
    "kermisbon": [
        "................", "................", "................", "..kkkkkkkkkkkk..", ".kyyyyyyyyyyyyk.",
        ".kyrryyyyyyrryk.", ".ky.pppppppp.yk.", ".ky.pwkppkwp.yk.", ".ky.pppppppp.yk.", ".ky.ppmmmmpp.yk.",
        ".ky.pppppppp.yk.", ".kyrryyyyyyrryk.", ".kyyyyyyyyyyyyk.", "..kkkkkkkkkkkk..", "................",
        "................"],
}


def kermis():
    import slee_track as st

    # --- the coaster rail items, recipes and what a broken piece drops ---
    for name, rows in KERMIS_ICONS.items():
        save(grid(rows, ITEM_PAL), "item", f"{name}.png")
        item_model(name)
    B = "guhs:sleebouwersboek"
    rail_key = {"I": "minecraft:iron_ingot", "W": "minecraft:pink_wool", "B": B, "S": "minecraft:stick", "K": "guhs:guh_kristal"}
    plain_key = {k: v for k, v in rail_key.items() if k != "K"}
    shaped("sleerail_drop", [" WI", "WBI", "ISS"], plain_key, "guhs:sleerail_drop", 1)
    shaped("sleerail_kurkentrekker", ["IWI", "WBW", "ISI"], plain_key, "guhs:sleerail_kurkentrekker", 1)
    shaped("sleerail_schans", ["  K", " BW", "ISS"], rail_key, "guhs:sleerail_schans", 1)
    drops = {"curve_left": "sleerail_bocht", "curve_right": "sleerail_bocht", "slope": "sleerail_helling", "drop": "sleerail_drop",
             "spiral_left": "sleerail_kurkentrekker", "spiral_right": "sleerail_kurkentrekker", "jump": "sleerail_schans"}
    w(f"{D}/loot_table/blocks/slee_rail.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"guhs:{item}", "conditions": [
                {"condition": "minecraft:block_state_property", "block": "guhs:slee_rail", "properties": {"shape": sh}}]}
            for sh, item in drops.items()] + [{"type": "minecraft:item", "name": "guhs:sleerail_recht"}]}]}]})

    # --- the kermis compass: red and yellow frames ---
    overrides = []
    for i in range(32):
        src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
        frame = f"kermiskompas_{i:02d}"
        save(recolour(src, hue=0.01, sat=1.35, val=1.0, only=pinkish), "item", f"{frame}.png")
        item_model(frame)
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": "guhs:item/kermiskompas_00"})
    w(f"{A}/models/item/kermiskompas.json", {"parent": "minecraft:item/generated",
                                              "textures": {"layer0": "guhs:item/kermiskompas_16"}, "overrides": overrides})

    # --- the Kermis-guh: a red and gold sitting guh ---
    src = Image.open(os.path.join(TEX, "entity", "guh_sitting.png"))
    save(recolour(src, hue=0.02, sat=1.25, val=1.0, only=pinkish), "entity", "npc_kermis_guh.png")

    # --- the sled's dashboard: three buttons at the rider's feet that blink while the sled stands still ---
    sled_knopjes()

    # --- the structure ---
    TEMPLATE_SIZES["guh_kermis"] = 44
    structure("guh_kermis", GUHMENSION_LAND, spacing=44, separation=16, salt=20400001)
    kermis_structure(st)
    Structure((24, 14, 52)).save("coaster_room")  # for the GameTests

    # --- advancements (shown): find it, first lap, the whole outfit ---
    for name, parent, icon, frame, crit, title, desc in [
        ("find_guh_kermis", "enter_guhmension", "guhs:kermiskompas", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:guh_kermis"]}}}},
         "Kermis!", "Vind de zeldzame Guhkermis"),
        ("kermis_rit", "find_guh_kermis", "guhs:guh_slee", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:kermisbon"}]}},
         "WIEEEEE!", "Rij een rondje in de achtbaan van de Guhkermis"),
        ("kermis_outfit", "kermis_rit", "guhs:kermis_hoed", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:kermis_hoed"}, {"items": "guhs:kermis_jasje"}, {"items": "guhs:kermis_strik"}]}},
         "Vahoeg op de kermis", "Koop het hele kermispakje bij de Kermis-guh"),
    ]:
        w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)

    # --- texts (all Dutch) ---
    for key, text in {
        "item.guhs.sleerail_drop": "Vadsdrop (steile sleerail)",
        "item.guhs.sleerail_kurkentrekker": "Guhkurkentrekker (sleerail)",
        "item.guhs.sleerail_schans": "Vahoegschans (sleerail)",
        "item.guhs.sleerail.drop.lore": "8 omhoog over 4 blokken: een echte achtbaanhelling. Sluip (op het eind van een rail) om naar beneden te gaan",
        "item.guhs.sleerail.spiral.lore": "Halve draai, 4 omhoog: je komt 3 blokken verderop terug. Sluip voor links; kijk omlaag om te dalen",
        "item.guhs.sleerail.jump.lore": "De slee vliegt 10 blokken door de lucht. Klik op het eind: het volgende stuk komt vanzelf op de landingsplek",
        "item.guhs.kermisbon": "Guhkermisbon",
        "item.guhs.kermiskompas": "Kermiskompas",
        "item.guhs.kermiskompas.lore": "Wijst naar de Guhkermis in de Guhmensie. Te koop bij de vadstemmer.",
        "item.guhs.kermis_hoed": "Vahoege kermishoed",
        "item.guhs.kermis_jasje": "Kermisjasje",
        "item.guhs.kermis_strik": "Guhkermisstrik",
        "entity.guhs.guh_npc.kermis_guh": "Kermis-guh",
        "entity.guhs.guh_slee.hint": "Rechtermuisklik: de stuurknopjes (start, snelheid, omkeren)",
        "quest.guhs.kermis.hello": "NJEG! Welkom op de Guhkermis! Elk rondje in de achtbaan = een kermisbon. Kijk eens wat ik voor bonnen heb... vahoeg!",
        "quest.guhs.kermis.first": "WIEEEE! Je eerste rondje in de Guhkermis-achtbaan! Een prijzenzakje voor jou: ballonnen, kaashoning, kristallen en 2 extra kermisbonnen. Vads!",
        "quest.guhs.kermis.lap": "Rondje! +1 kermisbon",
        "entity.guhs.guh.ender": "Enderguh",
        # the guh menu: shorter labels, and what every button does on hover
        "gui.guhs.menu.teleport": "Teleporteren: %s",
        "gui.guhs.menu.sit.tooltip": "Laat je guh op zijn plek zitten, of weer opstaan en je volgen",
        "gui.guhs.menu.teleport.tooltip": "Aan: je guh teleporteert naar je toe als je te ver weg bent",
        "gui.guhs.menu.gravity.tooltip": "Aan: zwaar en platgedrukt, kan niet springen, valt sneller",
        "gui.guhs.menu.wander.tooltip": "Aan: je guh vadst zelf rond. Uit: hij blijft braaf staan waar hij is",
        "gui.guhs.menu.sounds.tooltip": "Guhgeluidjes aan of uit, en hoe vaak",
        "gui.guhs.menu.radius.tooltip": "Hoe ver weg een agressieve guh mobs aanvalt",
        "gui.guhs.menu.name.tooltip": "Geef je guh een naam (Enter of Hernoem); geen naamkaartje nodig",
        "gui.guhs.menu.wardrobe.tooltip": "Kleertjes, pantser en de rugzak van je guh, met je eigen inventaris erbij",
        "gui.guhs.menu.sounds.ambient.tooltip": "Maakt je guh uit zichzelf geluidjes?",
        "gui.guhs.menu.sounds.frequency.tooltip": "Hoe vaak je guh uit zichzelf een geluidje maakt",
        "gui.guhs.sled.start.tooltip": "De guhs gaan trekken (of stoppen)",
        "gui.guhs.sled.reverse.tooltip": "Draai om en rij de andere kant op",
        "gui.guhs.sled.speed.tooltip": "Hoe hard de guhs trekken. Bergaf gaat het vanzelf harder, bergop langzamer",
    }.items():
        lang(key, text, text)


def sled_knopjes():
    """Adds the dashboard (a 'knopjes' bone) to the sled model, a green swatch, and the blink animation."""
    path = f"{A}/geckolib/models/entity/guh_slee.geo.json"
    geo = json.load(open(path, encoding="utf-8"))
    bones = geo["minecraft:geometry"][0]["bones"]
    bones[:] = [b for b in bones if b["name"] not in ("dashboard", "knopjes")]
    bones.append({"name": "dashboard", "parent": "sled", "pivot": [0, 4, -9],
                  "cubes": [geo_cube([-4, 4, -10], [8, 2, 2], 9), geo_cube([-4, 6, -10], [8, 1, 1], 1)]})
    bones.append({"name": "knopjes", "parent": "dashboard", "pivot": [0, 6, -9], "cubes": [
        geo_cube([-3.5, 6, -9], [2, 1, 1], 10), geo_cube([-1, 6, -9], [2, 1, 1], 4), geo_cube([1.5, 6, -9], [2, 1, 1], 6)]})
    w(path, geo)
    w(f"{A}/geckolib/animations/entity/guh_slee.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.guh_slee.knopjes": {"loop": True, "animation_length": 1.2, "bones": {
            "knopjes": {"scale": {"0.0": [1, 1, 1], "0.3": [1.25, 1.8, 1.25], "0.6": [1, 1, 1], "1.2": [1, 1, 1]},
                        "position": {"0.0": [0, 0, 0], "0.3": [0, 0.4, 0], "0.6": [0, 0, 0]}}}}}})
    SWATCH[10] = (90, 210, 110)
    swatch_texture(("entity", "guh_slee.png"), SWATCH, seed=3, var=8)


def kermis_structure(st):
    """The guh kermis: a sled coaster around a fairground (tools/slee_track.py; found with a search so the loop closes)."""
    S = ["straight"]
    seq = (S * 2 + ["spiral_right", "spiral_right"] + S * 2 + ["curve_right", "down_drop"] + S + ["jump"] + S + ["curve_right"]
           + S + ["drop"] + S + ["down_drop", "curve_right"] + S * 2 + ["slope", "straight", "down"] + S + ["curve_right"])
    W, H, D = 40, 26, 32
    s = Structure((W, H, D))
    pieces = st.build((5, 2, 23), "north", seq)
    end, _ = st.end_of(pieces[-1])
    start, _ = st.start_of(pieces[0])
    assert max(abs(end[i] - start[i]) for i in range(3)) < 0.01, (end, start)
    # ground: grass with pink paths
    fp = [(x, z) for x in range(W) for z in range(D)]
    for x, z in fp:
        s.set(x, 0, z, mc("dirt"))
        s.set(x, 1, z, mc("grass_block"), {"snowy": "false"})
    track_cols = {}
    for i, p in enumerate(pieces):
        for (x, y, z) in st.blocks(*p[:3]):
            track_cols.setdefault((x, z), []).append(y)
    # the coaster, the station piece is the finish line
    st.place(s, pieces, finish=(0,))
    # pillars under the high parts (not where a lower track runs)
    for (x, z), ys in track_cols.items():
        low = min(ys)
        if low > 3 and x % 3 == 0 and z % 3 == 0:
            s.fill(x, 2, z, x, low - 1, z, mc("white_concrete"))
            s.set(x, low - 1, z, mc("pink_concrete"))
    # the jump flies over a kaas saus pool
    jump = next(p for p in pieces if p[2] == "jump")
    ja = jump[0]
    for x in range(ja[0] + 3, ja[0] + 11):
        for z in range(ja[2] - 1, ja[2] + 3):
            edge = x in (ja[0] + 3, ja[0] + 10) or z in (ja[2] - 1, ja[2] + 2)
            s.set(x, 1, z, mc("pink_concrete") if edge else "guhs:kaas_saus", None if edge else {"level": "0"})
    # the station along the first two pieces (west side): platform, striped roof, fence and two sleds
    s.fill(1, 1, 18, 3, 1, 24, mc("birch_planks"))
    for z in range(18, 25):
        s.set(1, 2, z, mc("birch_fence"))
    for x, z in ((1, 18), (1, 24), (3, 18), (3, 24)):
        s.fill(x, 2, z, x, 6, z, mc("birch_fence"))
    for z in range(18, 25):
        for x in range(1, 9):
            s.set(x, 7, z, mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
    for z in (18, 24):
        s.set(8, 6, z, mc("birch_fence"))
        s.fill(8, 2, z, 8, 6, z, mc("birch_fence"))
    s.set(2, 6, 20, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    s.set(6, 6, 20, "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})
    for z in (22.0, 18.0):
        s.entity(6.0, 2.2, z, {"id": "guhs:guh_slee", "Locked": Byte(1), "Speed": 2, "Rotation": floats(180.0, 0.0)})
    # the fairground in the middle: a small carousel, the Kermis-guh's stall, tents and lampgion poles
    inner = [(x, z) for x in range(11, 33) for z in range(9, 23) if (x, z) not in track_cols]
    for x, z in inner:
        if (x * 3 + z * 5) % 11 == 0:
            s.set(x, 1, z, mc("pink_concrete_powder"))
    ms.carousel(s, 25, 15, r=4)
    # the stall: counter, striped awning, prices on a sign
    s.fill(13, 2, 18, 18, 2, 18, mc("stripped_birch_log"), {"axis": "x"})
    for x in (13, 18):
        s.fill(x, 2, 21, x, 5, 21, mc("birch_fence"))
        s.fill(x, 3, 18, x, 5, 18, mc("birch_fence"))
    for x in range(12, 20):
        for z in range(17, 23):
            s.set(x, 6, z, mc("red_wool") if x % 2 else mc("yellow_wool"))
    s.set(15, 3, 18, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    s.set(16, 3, 18, mc("cake"), {"bites": "0"})
    s.entity(15.5, 2.0, 20.0, {"id": "guhs:guh_npc", "Kind": "kermis_guh", "PersistenceRequired": Byte(1),
                               "Rotation": floats(180.0, 0.0)})
    # 2.10.1: a Reisguh on the station platform (a conductor at a station!), looking at the sleds: the kermis's waypoint
    from features import reisguh_plek
    reisguh_plek.zet(s, [(2, 2, 21), (3, 2, 21), (2, 2, 22), (3, 2, 22), (2, 2, 20), (3, 2, 20)], "Guhkermis", -90.0, Byte, floats,
                     "(guh_kermis)")
    # 2.8 (wereldleven): a grijpmachine next to the stall (paid with kermisbonnen; its first free spot)
    for (gx, gz, facing) in ((20, 20, "west"), (20, 18, "west"), (11, 20, "east"), (11, 17, "east")):
        if (gx, gz) not in track_cols and all(s.get(gx, y, gz) in (None, "minecraft:air") for y in (2, 3)):
            s.set(gx, 2, gz, "guhs:grijpmachine", {"facing": facing, "half": "lower"})
            s.set(gx, 3, gz, "guhs:grijpmachine", {"facing": facing, "half": "upper"})
            break
    else:
        raise SystemExit("guh_kermis: no room for the grijpmachine")
    # tents
    for (tx, tz, c) in ((13, 11, "magenta_wool"),):
        for dy in range(4):
            r = 3 - dy
            for x in range(tx - r, tx + r + 1):
                for z in range(tz - r, tz + r + 1):
                    if max(abs(x - tx), abs(z - tz)) == r and (x, z) not in track_cols:
                        s.set(x, 2 + dy, z, mc(c) if (x + z) % 2 else mc("white_wool"))
        s.set(tx, 6, tz, mc("pearlescent_froglight"))
        chest(s, tx, 2, tz, "south", "guhs:chests/hamster_house")
    # lampgion poles around the fairground
    for i, (x, z) in enumerate(((11, 9), (21, 9), (32, 9), (11, 22), (22, 22), (32, 15))):
        if (x, z) in track_cols:
            continue
        s.fill(x, 2, z, x, 4, z, mc("birch_fence"))
        s.set(x, 5, z, ("guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint")[i % 3], {"hanging": "false", "waterlogged": "false"})
    s.clear_above(fp, 2)
    s.save("guh_kermis")


# =====================================================================================================================
# 2.2.0: verstopguh (hide-and-seek) in the verstopguh house: a giant dollhouse under a roof of one-way glass
# =====================================================================================================================
VERSTOP_ICONS = {
    "verstopguhticket": [
        "................", "................", "................", "..kkkkkkkkkkkk..", ".kCCCCCCCCCCCCk.",
        ".kCwwCCCCCCwwCk.", ".kC.pppppppp.Ck.", ".kC.pkkppkkp.Ck.", ".kC.pwkppwkp.Ck.", ".kC.pppmmppp.Ck.",
        ".kC.pppppppp.Ck.", ".kCwwCCCCCCwwCk.", ".kCCCCCCCCCCCCk.", "..kkkkkkkkkkkk..", "................",
        "................"],
}
FACING = ("north", "east", "south", "west")


def verstop():
    # --- blocks: one-way glass, the invisible markers, the way out ---
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (255, 150, 200, 200) if edge else (255, 190, 225, 60))
    for i in range(3, 8):                                    # a little shine
        img.putpixel((i, 11 - i), (255, 255, 255, 150))
    save(img, "block", "eenrichtingsglas.png")
    save(noise_tex((236, 150, 190), 8, 41), "block", "eenrichtingsglas_onder.png")
    faces = {f: {"texture": "#glass", "cullface": f} for f in ("north", "south", "east", "west", "up")}
    faces["down"] = {"texture": "#ceiling", "cullface": "down"}
    w(f"{A}/models/block/eenrichtingsglas.json", {"render_type": "minecraft:translucent", "textures": {
        "particle": "guhs:block/eenrichtingsglas", "glass": "guhs:block/eenrichtingsglas", "ceiling": "guhs:block/eenrichtingsglas_onder"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]})
    w(f"{A}/blockstates/eenrichtingsglas.json", {"variants": {"": {"model": "guhs:block/eenrichtingsglas"}}})
    w(f"{A}/models/item/eenrichtingsglas.json", {"parent": "guhs:block/eenrichtingsglas"})
    self_drop("eenrichtingsglas")
    shaped("eenrichtingsglas", ["GGG", "GPG", "GGG"], {"G": "minecraft:pink_stained_glass", "P": "minecraft:pink_wool"},
           "guhs:eenrichtingsglas", 8)
    for marker in ("verstopplek", "verstopstart"):
        w(f"{A}/models/block/{marker}.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
        w(f"{A}/blockstates/{marker}.json", {"variants": {"": {"model": f"guhs:block/{marker}"}}})
    w(f"{A}/models/block/verstopuitgang.json", {"render_type": "minecraft:translucent", "textures": {
        "particle": "guhs:block/guh_portal", "top": "guhs:block/guh_portal"},
        "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#top"}}}]})
    w(f"{A}/blockstates/verstopuitgang.json", {"variants": {"": {"model": "guhs:block/verstopuitgang"}}})

    # --- a lampgion that went out (hint: no guhs left in this room) ---
    lamp = np.asarray(Image.open(os.path.join(TEX, "block", "lampion_roze.png")).convert("RGBA")).astype(np.float32)
    grey = lamp[..., :3].mean(-1, keepdims=True)
    lamp[..., :3] = (lamp[..., :3] * 0.35 + grey * 0.65) * 0.45
    save(Image.fromarray(lamp.astype(np.uint8)), "block", "lampion_uit.png")
    for suffix in ("", "_hanging"):
        m = json.load(open(f"{A}/models/block/lampion_roze{suffix}.json", encoding="utf-8"))
        m["textures"].update({"particle": "guhs:block/lampion_uit", "paper": "guhs:block/lampion_uit"})
        w(f"{A}/models/block/lampion_uit{suffix}.json", m)
    w(f"{A}/blockstates/lampion_uit.json", {"variants": {"hanging=false": {"model": "guhs:block/lampion_uit"},
                                                          "hanging=true": {"model": "guhs:block/lampion_uit_hanging"}}})
    lang("block.guhs.lampion_uit", "Lampgion (off)", "Lampgion (uit)")

    # --- items: the ticket and the compass ---
    for name, rows in VERSTOP_ICONS.items():
        save(grid(rows, ITEM_PAL), "item", f"{name}.png")
        item_model(name)
    overrides = []
    for i in range(32):
        src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
        frame = f"verstopkompas_{i:02d}"
        save(recolour(src, hue=0.55, sat=1.1, val=1.0, only=pinkish), "item", f"{frame}.png")
        item_model(frame)
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": "guhs:item/verstopkompas_00"})
    w(f"{A}/models/item/verstopkompas.json", {"parent": "minecraft:item/generated",
                                               "textures": {"layer0": "guhs:item/verstopkompas_16"}, "overrides": overrides})

    # --- Verstopguhtje: a mint-and-lilac sitting guh ---
    src = Image.open(os.path.join(TEX, "entity", "guh_sitting.png"))
    save(recolour(src, hue=0.78, sat=0.9, val=1.05, only=pinkish), "entity", "npc_verstopguhtje.png")
    save(recolour(src, hue=0.14, sat=1.2, val=1.05, only=pinkish), "entity", "npc_tipguh.png")      # the Tipguh: sunny yellow
    save(recolour(src, hue=0.58, sat=0.35, val=0.95, only=pinkish), "entity", "npc_poortwachter.png")  # the gate guards: steel
    reisguh_conducteur(src)                                                                            # the Reisguh: sky blue, cap and whistle

    # --- the house: rare, big, on flat ground; nothing spawns inside (wild guhs would mix with the hidden ones) ---
    TEMPLATE_SIZES["verstopguh_huis"] = 80
    FLATNESS["verstopguh_huis"] = 30
    none = {"bounding_box": "full", "spawns": []}
    structure("verstopguh_huis", GUHMENSION_LAND, spacing=48, separation=18, salt=20500001,
              spawn_overrides={"creature": none, "monster": none, "ambient": none})
    verstop_structure()

    # --- advancements ---
    for name, parent, icon, frame, crit, title, desc in [
        ("find_verstopguh_huis", "enter_guhmension", "guhs:verstopkompas", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:verstopguh_huis"]}}}},
         "Wie verstopt zich daar?", "Vind het zeldzame verstopguhhuis"),
        ("verstop_gewonnen", "find_verstopguh_huis", "guhs:verstopguhticket", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:verstopguhticket"}]}},
         "Gevonden!", "Vind alle verstopte guhs in het verstopguhhuis"),
        ("verstop_detective", "verstop_gewonnen", "guhs:detective_pet", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:detective_pet"}, {"items": "guhs:detective_vergrootglas"}, {"items": "guhs:detective_jas"}]}},
         "Guhlock Holmes", "Koop het hele detectivepakje bij Verstopguhtje"),
    ]:
        w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)
    for lvl in ("makkelijk", "medium", "moeilijk"):
        w(f"{D}/advancement/quest/verstop_{lvl}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    w(f"{D}/advancement/quest/guhvriend.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    w(f"{D}/advancement/quest/reisguhs.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    save(grid(["................", "................", "......kkkk......", ".....kCCCCk.....", "....kCccccCk....", "kkkkkCccccCk....",
               "kCCCCccccccCk...", "kccccccc.kccCk..", "kCCCCCCCkkcccCk.", "kkkkkkCcccccCk..", ".....kCCCCCCk...", "......kkkkkk....",
               "................", "......w..w......", ".......ww.......", "................"], ITEM_PAL), "item", "reisguh_fluitje.png")
    item_model("reisguh_fluitje")
    shapeless("reisguh_fluitje", ["minecraft:ender_pearl", "guhs:kaas_knabbels", "minecraft:light_blue_dye"], "guhs:reisguh_fluitje", 2)

    for key, text in {
        "block.guhs.eenrichtingsglas": "Eenrichtingsvadsglas",
        "block.guhs.verstopplek": "Verstopplekje",
        "block.guhs.verstopstart": "Verstopstart",
        "block.guhs.verstopuitgang": "Uitgang van het verstopguhhuis",
        "item.guhs.verstopguhticket": "Verstopguhticket",
        "item.guhs.verstopkompas": "Verstopkompas",
        "item.guhs.verstopkompas.lore": "Wijst naar het verstopguhhuis in de Guhmensie. Te koop bij de vadstemmer.",
        "item.guhs.detective_pet": "Guhlock-pet",
        "item.guhs.detective_vergrootglas": "Vergroot-vadsglas",
        "item.guhs.detective_jas": "Detectivejas",
        "entity.guhs.guh_npc.verstopguhtje": "Verstopguhtje",
        "quest.guhs.verstop.hello": "NJEG! Zin in verstopguh? Mijn vriendjes verstoppen zich in het huis onder ons. Makkelijk, medium of moeilijk? Vahoeg!",
        "quest.guhs.verstop.running": "Sssst... er wordt al gezocht! Wil je meehelpen?",
        "quest.guhs.verstop.broken": "Njeg... mijn huis is kapot, ik kan geen verstopplekjes meer vinden.",
        "quest.guhs.verstop.go": "Er zijn %s guhs verstopt (%s). Rechtermuisklik als je er een vindt. Succes! De roze uitgang brengt je terug.",
        "quest.guhs.verstop.joined": "%s zoekt mee!",
        "quest.guhs.verstop.found": "%s vond een guh! (%s / %s)",
        "quest.guhs.verstop.not_playing": "Deze guh is verstopt... vraag Verstopguhtje op het dak of je mee mag doen!",
        "quest.guhs.verstop.won": "VAHOEG! Alle guhs gevonden in %s! Je krijgt %s verstopguhticket(s).",
        "quest.guhs.verstop.record": "Nieuw record: %s!",
        "gui.guhs.scorebord.verstop": "Top 3 snelste zoekers",
        "gui.guhs.scorebord.empty": "nog niemand: jij eerst!",
        "gui.guhs.scorebord.place": "Je staat op plek %s van het scorebord! VAHOEG!",
        "quest.guhs.verstop.stopped": "Je stopt met zoeken (%s van %s gevonden). Tot de volgende keer!",
        "quest.guhs.verstop.bar": "Verstopguh (%s): %s / %s gevonden - %s",
        "gui.guhs.verstop.question": "Wil je verstopguh spelen? Kies hoe moeilijk. Hoe moeilijker, hoe kleiner de guhs en hoe meer tickets.",
        "gui.guhs.verstop.running": "Er wordt al gezocht (%s): %s van %s gevonden, %s speler(s). Zoek mee! (Samen zoeken telt niet voor records.)",
        "gui.guhs.verstop.makkelijk": "Makkelijk",
        "gui.guhs.verstop.medium": "Medium",
        "gui.guhs.verstop.moeilijk": "Moeilijk",
        "gui.guhs.verstop.level.tooltip": "%s guhs van %s blok lang. Alles gevonden: %s tickets, plus %s als je snel bent",
        "gui.guhs.verstop.join": "Meezoeken",
        "gui.guhs.verstop.join.tooltip": "Je gaat het huis in en zoekt mee; iedereen krijgt de tickets aan het eind",
        "gui.guhs.verstop.shop": "Winkeltje",
        "gui.guhs.verstop.shop.tooltip": "Het detectivepakje voor je guh, voor verstopguhtickets",
        "gui.guhs.verstop.best": "Jouw beste tijden (alleen gespeeld):",
        "quest.guhs.verstop.hint.floor": "Hint: er zit nog een guh op de %s, in het %s van het huis.",
        "quest.guhs.verstop.hint.lights": "Hint: in een kamer gingen de lampjes uit en werden de ramen donker. Daar zit geen guh meer!",
        "quest.guhs.verstop.hint.sniff": "Hint: hoor je dat? Een van de guhs moet steeds snuffelen...",
        "quest.guhs.verstop.hint.beneden": "benedenverdieping",
        "quest.guhs.verstop.hint.boven": "bovenverdieping",
        "quest.guhs.verstop.hint.noord": "noorden",
        "quest.guhs.verstop.hint.zuid": "zuiden",
        "quest.guhs.verstop.hint.oost": "oosten",
        "quest.guhs.verstop.hint.west": "westen",
        "quest.guhs.verstop.hint.noordoost": "noordoosten",
        "quest.guhs.verstop.hint.noordwest": "noordwesten",
        "quest.guhs.verstop.hint.zuidoost": "zuidoosten",
        "quest.guhs.verstop.hint.zuidwest": "zuidwesten",
        "quest.guhs.verstop.hint.midden": "midden",
        "entity.guhs.guh_npc.tipguh": "Tipguh",
        "quest.guhs.verstop.tip.confirm": "Psst... wil je een tip? Dan laat ik een guh even oplichten. Maar let op: dan telt je tijd niet meer voor je record (je tickets krijg je wel). Klik nog een keer als je het zeker weet!",
        "quest.guhs.verstop.tip.given": "Tipguh laat een guh oplichten voor %s. Kijk snel!",
        "quest.guhs.verstop.tip.wait": "Njeg, even geduld... over %s seconden weet ik weer een tip.",
        "quest.guhs.verstop.tip.nogame": "Hoi! Ik ben de Tipguh. Tijdens verstopguh kun je mij elke minuut om een tip vragen. Vraag Verstopguhtje op het dak om te spelen!",
        "quest.guhs.verstop.tip.norecord": "Je hebt een tip gebruikt, dus deze tijd telt niet voor je record.",
        "gui.guhs.verstop.no_build": "Njeg! Het verstopguhhuis is heilig: hier mag je niks slopen of bouwen.",
    }.items():
        lang(key, text, text)


# =====================================================================================================================
# 2.8: the Reisguh as a real guh-conductor: a conducteurspetje and a fluitje (its own NPC model), and his "tuut"
# =====================================================================================================================
REIS_MODEL = "guh_npc_reisguh"        # guh_sitting + the cap (bone reis_pet on the head) + the whistle on a cord
REIS_SWATCHES = {                      # name: (base colour, noise); painted in free 8x8 cells of npc_reisguh.png
    "pet": ((34, 52, 112), 6), "klep": ((20, 24, 40), 3), "band": ((236, 186, 60), 8), "embleem": ((244, 204, 84), 0),
    "fluit": ((240, 194, 64), 6), "koord": ((226, 60, 80), 6)}


def reisguh_conducteur(src):
    """npc_reisguh.png (the sky-blue sitting guh plus swatches for the cap and whistle), the model guh_npc_reisguh and
    the whistle's sound. The NPC renderer uses this model for Kind.REISGUH (feature/reisguh/client); the whistle
    animation is done in code (ReisguhFluitClient: the fluitje goes up to his mouth, a paw waves, "tuut!")."""
    from features import spiesburcht_modellen as sm
    tex = recolour(src, hue=0.55, sat=0.85, val=1.05, only=pinkish)        # the Reisguh: sky blue (as before)
    model = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh_sitting.geo.json"), encoding="utf-8"))
    geo = model["minecraft:geometry"][0]
    sheet = sm.Sheet(tex, geo["bones"])
    rng = np.random.default_rng(2808)

    def swatch(name):
        base, var = REIS_SWATCHES[name]
        p = sm.noise(base, var, rng)
        if name == "pet":                                   # a little sheen on the cap's cloth
            p[:6] = np.clip(p[:6] * 1.18, 0, 255)
        elif name == "klep":                                # the shiny peak
            p[4:10, 4:28] = (96, 108, 150)
            p[10:13, 8:24] = (60, 70, 110)
        elif name == "band":
            p[:3] = p[-3:] = (196, 146, 40)
        elif name == "embleem":                             # the badge: a little guh face on gold
            p[:, :] = (244, 204, 84)
            p[0:2, :] = p[-2:, :] = p[:, 0:2] = p[:, -2:] = (190, 140, 36)
            for (x0, x1) in ((6, 12), (20, 26)):            # round ears
                p[5:10, x0:x1] = (46, 64, 140)
            p[8:26, 8:24] = (46, 64, 140)                   # head
            p[13:18, 11:14] = p[13:18, 18:21] = (250, 250, 255)   # eyes
            p[15:17, 12:14] = p[15:17, 19:21] = (20, 20, 30)
            p[20:22, 14:18] = (240, 130, 170)               # snoet
        elif name == "fluit":                               # shiny gold with a bright line
            p[6:9, :] = (255, 244, 190)
            p[22:26, :] = (176, 128, 30)
        return p
    for name in REIS_SWATCHES:
        sheet.swatch(name, lambda n=name: swatch(n))
    cube = sm.cube
    klep = cube(sheet, "klep", [-4.1, 25.4, -9.2], [8.2, 0.6, 3.9], pivot=[0, 25.7, -5.4], rotation=[16, 0, 0])
    pet = {"name": "reis_pet", "parent": "head", "pivot": [0, 26, 0], "cubes": [
        cube(sheet, "pet", [-4.2, 25.3, -5.5], [8.4, 4.2, 9.2]),                      # the crown, between the ears
        cube(sheet, "pet", [-4.45, 29.3, -5.75], [8.9, 0.9, 9.7]),                    # the flat top
        cube(sheet, "band", [-4.35, 25.9, -5.65], [8.7, 1.1, 9.5]),                   # the golden band
        cube(sheet, "embleem", [-1.2, 27.2, -5.95], [2.4, 2.2, 0.4]),                 # the badge
        klep]}
    koord = {"name": "reis_koord", "parent": "body", "pivot": [0, 11.4, -5.3], "cubes": [
        cube(sheet, "koord", [0.1, 11.2, -5.5], [3.3, 0.45, 0.45], pivot=[0.1, 11.4, -5.3], rotation=[0, 0, 16]),
        cube(sheet, "koord", [-3.4, 11.2, -5.5], [3.3, 0.45, 0.45], pivot=[-0.1, 11.4, -5.3], rotation=[0, 0, -16])]}
    fluit = {"name": "reis_fluitje", "parent": "body", "pivot": [0, 9.2, -5.35], "cubes": [
        cube(sheet, "fluit", [-1.4, 8.2, -6.3], [2.8, 1.9, 1.9]),                     # the barrel
        cube(sheet, "fluit", [-0.45, 10.1, -5.8], [0.9, 1.2, 0.9]),                   # the mouthpiece (up, on the cord)
        cube(sheet, "koord", [-0.25, 11.1, -5.6], [0.5, 0.5, 0.5])]}
    geo["description"]["identifier"] = f"geometry.{REIS_MODEL}"
    geo["bones"] = [b for b in geo["bones"] if not b["name"].startswith("reis_")] + [pet, koord, fluit]
    w(os.path.join(A, "geckolib", "models", "entity", f"{REIS_MODEL}.geo.json"), model)
    img = Image.fromarray(np.clip(sheet.img, 0, 255).astype(np.uint8))
    save(img, "entity", "npc_reisguh.png")
    reisguh_tuut()
    lang("subtitles.guhs.reisguh.tuut", "Reisguh fluit: tuut tuut!", "Reisguh fluit: tuut tuut!")
    problems = reisguh_check(geo, img)
    if problems:
        raise SystemExit("reisguh: " + "; ".join(problems))


def reisguh_check(geo, img):
    """The conductor really has his cap on his head (on top, between the ears, peak over the face) and his whistle on
    his chest; every swatch is painted in a spot no other cube uses."""
    p = []
    bones = {b["name"]: b for b in geo["bones"]}
    head = bones["head"]
    top = max(c["origin"][1] + c["size"][1] for c in head["cubes"])
    front = min(c["origin"][2] for c in head["cubes"])
    ear_x = min(c["origin"][0] for c in bones["ear_left"]["cubes"])
    pet = bones.get("reis_pet")
    if not pet or pet.get("parent") != "head":
        p.append("no cap on the head")
    else:
        crown = pet["cubes"][0]
        if not (top - 1.5 <= crown["origin"][1] <= top and crown["origin"][0] + crown["size"][0] <= ear_x + 0.01):
            p.append(f"the cap doesn't sit on the head between the ears: {crown}")
        klep = pet["cubes"][-1]
        if klep["origin"][2] > front:
            p.append("the peak doesn't stick out over the face")
    fl = bones.get("reis_fluitje")
    if not fl or fl.get("parent") != "body":
        p.append("no whistle on the chest")
    a = np.asarray(img.convert("RGBA"))
    if a.shape[:2] != (512, 512):
        p.append(f"texture size {a.shape[:2]}")
    if not os.path.exists(os.path.join(A, "sounds", "reisguh_tuut.ogg")):
        p.append("no sounds/reisguh_tuut.ogg")
    return p


def reisguh_tuut():
    """sounds/reisguh_tuut.ogg (a pea whistle: tuut-tuuut, with its little rattle) and its sounds.json entry. Needs the
    python package soundfile to (re)make the .ogg; without it the file that's there stays."""
    path = os.path.join(A, "sounds", "reisguh_tuut.ogg")
    try:
        import soundfile
    except ImportError:
        soundfile = None
    if soundfile is not None:
        sr = 44100
        rng = np.random.default_rng(2808)
        out = []
        for dur, f0 in ((0.26, 1560), (0.08, 0), (0.5, 1600)):
            n = int(dur * sr)
            t = np.arange(n) / sr
            if not f0:
                out.append(np.zeros(n))
                continue
            f = f0 * (1 + 0.04 * np.exp(-t * 30)) * (1 + 0.004 * np.sin(2 * np.pi * 5 * t))
            ph = 2 * np.pi * np.cumsum(f) / sr
            tone = np.sin(ph) + 0.28 * np.sin(2 * ph) + 0.08 * np.sin(3 * ph)
            trill = 1 - 0.35 * (0.5 + 0.5 * np.sin(2 * np.pi * 34 * t))                  # the pea rattling
            breath = np.convolve(rng.normal(0, 1, n), np.ones(6) / 6, mode="same") * 0.12
            env = np.minimum(1, t / 0.015) * np.minimum(1, (dur - t) / 0.05)
            out.append((tone * trill + breath) * env)
        x = np.concatenate(out)
        x = 0.7 * x / np.abs(x).max()
        os.makedirs(os.path.dirname(path), exist_ok=True)
        soundfile.write(path, x.astype(np.float32), sr, format="OGG", subtype="VORBIS")
    snd_path = f"{A}/sounds.json"
    sounds = json.load(open(snd_path, encoding="utf-8"))
    sounds["reisguh.tuut"] = {"subtitle": "subtitles.guhs.reisguh.tuut", "sounds": [{"name": "guhs:reisguh_tuut", "volume": 0.8}]}
    w(snd_path, sounds)


def verstop_structure():
    """The verstopguh house: 80x80, two floors of dollhouse rooms full of hiding spots, a roof of one-way glass with
    Verstopguhtje in the middle and stairs up the side. The rooms are 23x23; every room gets furniture along its walls
    with little gaps (nooks), things on top of tall furniture, and corners behind furniture: those are the hiding spots."""
    W, H, D = 80, 22, 80
    s = Structure((W, H, D))
    rng = random.Random(2202)
    X0, X1 = 4, 75                     # outer walls
    F1, F2, ROOF = 1, 7, 13           # floor 1, floor 2 and the roof (block y)
    cols = [(5, 27), (29, 51), (53, 74)]
    spots = []

    fp = [(x, z) for x in range(W) for z in range(D)]
    for x, z in fp:
        s.set(x, 0, z, mc("dirt"))
    # garden around the house: grass, a pink path, hedges and flowers
    for x, z in fp:
        if not (X0 <= x <= X1 and X0 <= z <= X1):
            ring = min(x, z, W - 1 - x, D - 1 - z)
            s.set(x, 0, z, mc("pink_concrete_powder") if ring == 2 else mc("grass_block"), {"snowy": "false"} if ring != 2 else None)
            if ring == 0 and (x + z) % 3:
                s.set(x, 1, z, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
            elif ring == 1 and (x * 7 + z * 3) % 5 == 0:
                s.set(x, 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
    # the shell: walls with windows, two floors, the one-way glass roof
    for y in range(F1, ROOF + 1):
        for i in range(X0, X1 + 1):
            for (x, z) in ((i, X0), (i, X1), (X0, i), (X1, i)):
                corner = x in (X0, X1) and z in (X0, X1)
                window = (y in (F1 + 3, F1 + 4, F2 + 3, F2 + 4)) and not corner and (i - X0) % 8 in (3, 4)
                if window:
                    s.set(x, y, z, mc("pink_stained_glass_pane"), pane(x, z, X0, X1))
                elif y in (F2, ROOF):
                    s.set(x, y, z, mc("white_concrete"))
                else:
                    s.set(x, y, z, mc("pink_terracotta") if corner else mc("pink_concrete") if (y + i) % 9 else mc("magenta_concrete"))
    for x in range(X0 + 1, X1):
        for z in range(X0 + 1, X1):
            s.set(x, F2, z, mc("stripped_cherry_wood"), {"axis": "y"})
            s.set(x, ROOF, z, "guhs:eenrichtingsglas")
    # a fence around the roof, the stairs up the east side, Verstopguhtje in the middle
    for i in range(X0, X1 + 1):
        for (x, z) in ((i, X0), (i, X1), (X0, i), (X1, i)):
            if not (x == X1 and 46 <= z <= 49):
                s.set(x, ROOF + 1, z, mc("cherry_fence"), fence(x, z, X0, X1))
    ns_fence = {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"}
    for k in range(1, ROOF + 1):                                  # stairs along the east wall going up northwards
        z = 49 + (ROOF - k)
        for x in (X1 + 1, X1 + 2):
            s.set(x, k, z, mc("cherry_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for y in range(1, k):
                s.set(x, y, z, mc("cherry_planks"))
        s.set(X1 + 3, k + 1, z, mc("cherry_fence"), ns_fence)
    for z in (46, 47, 48):
        for x in (X1 + 1, X1 + 2):
            s.set(x, ROOF, z, mc("cherry_planks"))
        s.set(X1 + 3, ROOF + 1, z, mc("cherry_fence"), ns_fence)
    for x in (X1 + 1, X1 + 2, X1 + 3):
        s.set(x, ROOF + 1, 45, mc("cherry_fence"), {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    s.set(40, ROOF + 1, 40, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    s.entity(40.5, ROOF + 1.0, 42.5, {"id": "guhs:guh_npc", "Kind": "verstopguhtje", "PersistenceRequired": Byte(1),
                                      "Rotation": floats(0.0, 0.0)})
    for (x, z) in ((20, 20), (60, 20), (20, 60), (60, 60)):
        s.set(x, ROOF + 1, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})

    # rooms: inner walls with doorways, then the furniture of each room
    for fy in (F1, F2):
        for wall in (28, 52):
            for i in range(X0 + 1, X1):
                s.set(wall, fy, i, mc("stripped_cherry_wood"), {"axis": "y"})   # the floor runs on under the walls and doorways
                s.set(i, fy, wall, mc("stripped_cherry_wood"), {"axis": "y"})
                for y in range(fy + 1, fy + 6):
                    s.set(wall, y, i, mc("white_concrete") if (i // 4) % 2 else mc("pink_concrete"))
                    s.set(i, y, wall, mc("white_concrete") if (i // 4) % 2 else mc("pink_concrete"))
        for wall in (28, 52):                                        # doorways in the middle of every wall piece
            for a, b in cols:
                mid = (a + b) // 2
                for d in (mid, mid + 1):
                    for y in (fy + 1, fy + 2, fy + 3):
                        s.set(wall, y, d, mc("air"))
                        s.set(d, y, wall, mc("air"))
    ROOMS = {F1: [["keuken", "eetkamer", "woonkamer"], ["bibliotheek", "hal", "speelkamer"], ["badkamer", "berging", "tuinkas"]],
             F2: [["slaapkamer", "kinderkamer", "slaapkamer2"], ["knuffelkamer", "overloop", "muziekkamer"], ["atelier", "zolder", "naaikamer"]]}
    for fy, grid_ in ROOMS.items():
        for r, row in enumerate(grid_):
            for c, kind in enumerate(row):
                (x0, x1), (z0, z1) = cols[c], cols[r]
                furnish(s, rng, kind, x0, z0, x1, z1, fy, spots)
    # the hall: the start, the way out, the stairs up (and the hole in the floor above them)
    for i, z in enumerate(range(45, 39, -1)):                      # 6 steps up to the landing
        for x in (39, 40):
            s.set(x, F1 + 1 + i, z, mc("cherry_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for y in range(F1 + 1, F1 + 1 + i):
                s.set(x, y, z, mc("cherry_planks"))
    for x in (39, 40):
        for z in range(41, 46):
            s.set(x, F2, z, mc("air"))
            s.set(x, F2 + 1, z, mc("air"))
    for z in range(41, 46):
        for x in (38, 41):
            s.set(x, F2 + 1, z, mc("cherry_fence"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(36, F1 + 1, 48, "guhs:verstopstart")
    s.entity(38.5, F1 + 1.0, 50.5, {"id": "guhs:guh_npc", "Kind": "tipguh", "PersistenceRequired": Byte(1), "Rotation": floats(180.0, 0.0)})
    for x in (33, 34):
        for z in (47, 48):
            s.set(x, F1 + 1, z, "guhs:verstopuitgang")
    # every spot a marker (kept only where there is room for a guh)
    kept = 0
    for (x, y, z) in spots:
        if s.get(x, y, z) in (None, "minecraft:air"):
            s.set(x, y, z, "guhs:verstopplek")
            kept += 1
    print(f"verstopguh house: {kept} hiding spots")
    s.clear_above(fp, 1)
    s.save("verstopguh_huis")


def pane(x, z, x0, x1):
    ns = x in (x0, x1)
    return {"north": str(ns).lower(), "south": str(ns).lower(), "east": str(not ns).lower(), "west": str(not ns).lower(), "waterlogged": "false"}


def fence(x, z, x0, x1):
    ns = x in (x0, x1)
    return {"north": str(ns).lower(), "south": str(ns).lower(), "east": str(not ns).lower(), "west": str(not ns).lower(), "waterlogged": "false"}


ROOM_STYLE = {  # floor, rug colour, wall pieces [(block, props, height)], centre piece
    "keuken": ("smooth_quartz", None, [("white_terracotta", 1), ("smoker", 1), ("cauldron", 1), ("barrel", 1), ("white_concrete", 2)], "table"),
    "eetkamer": ("cherry_planks", "pink", [("guh_kast", 2), ("barrel", 1), ("potted", 1)], "long_table"),
    "woonkamer": ("cherry_planks", "magenta", [("guh_bank", 1), ("bookshelf", 3), ("zitzak", 1), ("potted", 1), ("guh_kast", 2)], "sofa"),
    "bibliotheek": ("dark_oak_planks", "red", [("bookshelf", 3), ("bookshelf", 3), ("lectern", 1), ("chiseled_bookshelf", 3)], "shelves"),
    "hal": ("polished_andesite", "pink", [("potted", 1), ("guh_kast", 2)], None),
    "speelkamer": ("birch_planks", "light_blue", [("toys", 1), ("zitzak", 1), ("toys", 2), ("kussen", 1)], "ballpit"),
    "badkamer": ("white_concrete", None, [("cauldron", 1), ("quartz_block", 1), ("barrel", 1)], "bath"),
    "berging": ("spruce_planks", None, [("barrel", 1), ("hay_block", 2), ("chest", 1), ("barrel", 2)], "boxes"),
    "tuinkas": ("moss_block", None, [("bush", 1), ("potted", 1), ("bush", 2)], "garden"),
    "slaapkamer": ("cherry_planks", "pink", [("guh_kast", 2), ("barrel", 1), ("kussen", 1)], "bed"),
    "slaapkamer2": ("birch_planks", "lime", [("guh_kast", 2), ("barrel", 1), ("zitzak", 1)], "bed"),
    "kinderkamer": ("birch_planks", "yellow", [("toys", 1), ("toys", 2), ("kussen", 1), ("guh_kast", 2)], "bed"),
    "knuffelkamer": ("pink_wool", None, [("kussen", 1), ("zitzak", 1), ("plush", 2)], "plushpile"),
    "overloop": ("cherry_planks", "pink", [("potted", 1), ("bookshelf", 3)], None),
    "muziekkamer": ("dark_oak_planks", "purple", [("note_block", 1), ("jukebox", 1), ("piano", 1), ("bookshelf", 3)], "sofa"),
    "atelier": ("oak_planks", "orange", [("loom", 1), ("cartography_table", 1), ("potted", 1), ("barrel", 1)], "easels"),
    "zolder": ("spruce_planks", None, [("chest", 1), ("barrel", 2), ("hay_block", 1), ("barrel", 1)], "boxes"),
    "naaikamer": ("cherry_planks", "magenta", [("loom", 1), ("wool", 2), ("guh_kast", 2)], "table"),
}
WOOL = ["pink", "white", "magenta", "light_blue", "yellow", "lime", "purple", "orange"]


def furnish(s, rng, kind, x0, z0, x1, z1, fy, spots):
    floor, rug, pieces, centre = ROOM_STYLE[kind]
    y = fy + 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, fy, z, mc(floor) if floor != "moss_block" or (x + z) % 4 else mc("grass_block"), {"snowy": "false"} if floor == "moss_block" and not (x + z) % 4 else None)
    if rug:
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                s.set(x, y, z, mc(f"{rug}_carpet"))
    # a lamp hanging from the ceiling in each quarter
    for (x, z) in ((x0 + 5, z0 + 5), (x1 - 5, z0 + 5), (x0 + 5, z1 - 5), (x1 - 5, z1 - 5)):
        s.set(x, fy + 5, z, "guhs:lampion_" + rng.choice(["roze", "geel", "mint"]), {"hanging": "true", "waterlogged": "false"})
    # furniture along the walls, with gaps (nooks) and corners behind things
    mid_x, mid_z = (x0 + x1) // 2, (z0 + z1) // 2
    doorway = lambda x, z: abs(x - mid_x - 0.5) < 3 or abs(z - mid_z - 0.5) < 3
    sides = [[(x, z0, "south") for x in range(x0, x1 + 1)], [(x, z1, "north") for x in range(x0, x1 + 1)],
             [(x0, z, "east") for z in range(z0 + 1, z1)], [(x1, z, "west") for z in range(z0 + 1, z1)]]
    for side in sides:
        i = 1
        while i < len(side) - 1:
            x, z, facing = side[i]
            if doorway(x, z):
                i += 1
                continue
            block, height = rng.choice(pieces)
            place_piece(s, rng, block, x, y, z, facing, height, spots)
            gap = rng.random() < 0.35
            if gap and i + 1 < len(side) - 1 and not doorway(*side[i + 1][:2]):
                spots.append((side[i + 1][0], y, side[i + 1][1]))          # a nook between two pieces
                i += 1
            i += 1
        # the corners: something tall standing one block off the wall, a spot behind it
        x, z, facing = side[0]
        spots.append((x, y, z))
    place_centre(s, rng, centre, x0, z0, x1, z1, y, spots)
    # little islands of furniture in the open parts of the room, each with a spot next to it
    for _ in range(5):
        for _try in range(20):
            ix, iz = rng.randint(x0 + 3, x1 - 3), rng.randint(z0 + 3, z1 - 3)
            if abs(ix - mid_x) > 6 or abs(iz - mid_z) > 6:
                if not doorway(ix, iz) and all(s.get(ix + dx, y, iz + dz) in (None, "minecraft:air") for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
                    island(s, rng, kind, ix, y, iz, spots)
                    break


def island(s, rng, kind, x, y, z, spots):
    leaves = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    choice = rng.choice(["plant", "zitzakken", "tafeltje", "knuffels", "kast"] if kind != "tuinkas" else ["plant", "plant", "bush"])
    if choice == "plant":
        s.set(x, y, z, mc("barrel"), {"facing": "up", "open": "false"})
        s.set(x, y + 1, z, rng.choice(["guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "minecraft:potted_flowering_azalea_bush"]))
        spots.append((x + 1, y, z))
    elif choice == "bush":
        s.set(x, y, z, mc("flowering_azalea_leaves"), leaves)
        s.set(x + 1, y, z, mc("azalea_leaves"), leaves)
        spots.append((x, y, z + 1))
    elif choice == "zitzakken":
        colour = rng.choice(WOOL)
        s.set(x, y, z, f"guhs:{colour}_zitzak", {"facing": "south"})
        s.set(x + 1, y, z, f"guhs:{rng.choice(WOOL)}_zitzak", {"facing": "south"})
        spots.append((x, y, z - 1))
    elif choice == "tafeltje":
        s.set(x, y, z, "guhs:guh_tafel", {"facing": "north"})
        s.set(x, y, z + 1, "guhs:guh_stoel", {"facing": "north"})
        s.set(x, y + 1, z, "guhs:potted_kaasbloem")
        spots.append((x + 1, y, z))
    elif choice == "knuffels":
        for dx, dz, h in ((0, 0, 2), (1, 0, 1), (0, 1, 1)):
            for k in range(h):
                s.set(x + dx, y + k, z + dz, mc(rng.choice(["pink", "white", "magenta"]) + "_wool"))
        spots += [(x + 1, y, z + 1), (x, y + 2, z)]
    else:
        s.set(x, y, z, "guhs:guh_kast", {"facing": "south", "open": "false"})
        s.set(x, y + 1, z, "guhs:guh_kast", {"facing": "south", "open": "false"})
        spots += [(x, y + 2, z), (x, y, z - 1)]


def place_piece(s, rng, block, x, y, z, facing, height, spots):
    props = None
    name = block
    if block in ("guh_kast", "guh_bank", "zitzak", "kussen"):
        colour = rng.choice(WOOL)
        name = {"guh_kast": "guhs:guh_kast", "guh_bank": "guhs:guh_bank", "zitzak": f"guhs:{colour}_zitzak", "kussen": f"guhs:{colour}_kussen"}[block]
        props = {"facing": facing, "open": "false"} if block == "guh_kast" else {"facing": facing}
    elif block in ("smoker", "lectern", "loom"):
        name, props = mc(block), {"facing": facing, **({"lit": "false"} if block == "smoker" else {}),
                                  **({"has_book": "false", "powered": "false"} if block == "lectern" else {})}
    elif block == "barrel":
        name, props = mc("barrel"), {"facing": "up", "open": "false"}
    elif block == "chest":
        name, props = mc("chest"), {"facing": facing, "type": "single", "waterlogged": "false"}
    elif block == "cauldron":
        name = mc("water_cauldron") if rng.random() < 0.5 else mc("cauldron")
        props = {"level": "3"} if name.endswith("water_cauldron") else None
    elif block == "potted":
        name = rng.choice(["guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "guhs:potted_kaasbloem", "minecraft:potted_azalea_bush",
                           "minecraft:potted_flowering_azalea_bush"])
    elif block == "bush":
        name, props = mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"}
    elif block in ("toys", "plush", "wool"):
        for h in range(height):
            s.set(x, y + h, z, mc(rng.choice(WOOL) + ("_concrete" if block == "toys" else "_wool")))
        if height >= 2:
            spots.append((x, y + height, z))
        return
    elif block == "piano":
        s.set(x, y, z, mc("black_concrete"))
        s.set(x, y + 1, z, mc("white_carpet"))
        return
    elif block == "note_block":
        name, props = mc("note_block"), {"instrument": "harp", "note": "0", "powered": "false"}
    elif block == "jukebox":
        name, props = mc("jukebox"), {"has_record": "false"}
    elif block == "chiseled_bookshelf":
        name, props = mc("chiseled_bookshelf"), {"facing": facing, **{f"slot_{k}_occupied": "true" if (k + x + z) % 3 else "false" for k in range(6)}}
    elif block == "hay_block":
        name, props = mc("hay_block"), {"axis": "y"}
    else:
        name = mc(block)
    for h in range(height):
        s.set(x, y + h, z, name, props)
    if height >= 2:
        spots.append((x, y + height, z))                               # on top of the tall thing


def place_centre(s, rng, centre, x0, z0, x1, z1, y, spots):
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    if centre == "table":
        for dx in (-1, 0, 1):
            s.set(cx + dx, y, cz, "guhs:guh_tafel", {"facing": "north"})
        for dx in (-1, 1):
            s.set(cx + dx, y, cz - 1, "guhs:guh_stoel", {"facing": "south"})
            s.set(cx + dx, y, cz + 1, "guhs:guh_stoel", {"facing": "north"})
        spots.append((cx, y, cz + 1))
    elif centre == "long_table":
        for dz in range(-4, 5):
            s.set(cx, y, cz + dz, "guhs:guh_tafel", {"facing": "east"})
            if dz % 2 == 0:
                s.set(cx - 1, y, cz + dz, "guhs:guh_stoel", {"facing": "east"})
                s.set(cx + 1, y, cz + dz, "guhs:guh_stoel", {"facing": "west"})
        spots += [(cx - 1, y, cz - 3), (cx + 1, y, cz + 1)]
    elif centre == "sofa":
        for dx in range(-2, 3):
            s.set(cx + dx, y, cz + 2, "guhs:guh_bank", {"facing": "north"})
        s.set(cx, y, cz - 2, mc("red_wool"))
        spots.append((cx + rng.choice([-1, 1]), y, cz + 3))            # behind the sofa
    elif centre == "shelves":
        for row in (-4, 0, 4):
            for dx in range(-6, 7):
                if dx not in (-1, 0):                                   # an aisle through the middle
                    for h in range(3):
                        s.set(cx + dx, y + h, cz + row, mc("bookshelf"))
            s.set(cx + rng.choice([-5, 3]), y, cz + row, mc("air"))    # a hole in the shelf
            spots.append((cx + rng.choice([-5, 3]), y, cz + row))
            spots.append((cx + rng.choice([-6, 6]), y + 3, cz + row))
    elif centre == "ballpit":
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                edge = max(abs(dx), abs(dz)) == 3
                s.set(cx + dx, y, cz + dz, mc("white_concrete") if edge else mc(rng.choice(["pink", "yellow", "light_blue", "lime"]) + "_concrete_powder"))
        spots += [(cx, y + 1, cz), (cx + 2, y + 1, cz - 1)]
    elif centre == "bath":
        for dx in range(-2, 3):
            for dz in range(-1, 2):
                edge = abs(dx) == 2 or abs(dz) == 1
                s.set(cx + dx, y, cz + dz, mc("quartz_block") if edge else mc("water"), None if edge else {"level": "0"})
        spots.append((cx, y + 1, cz))
    elif centre == "boxes":
        for _ in range(9):
            bx, bz = cx + rng.randint(-5, 5), cz + rng.randint(-5, 5)
            h = rng.randint(1, 3)
            for k in range(h):
                s.set(bx, y + k, bz, mc("barrel"), {"facing": "up", "open": "false"})
            spots.append((bx + 1, y, bz))
    elif centre == "garden":
        for dx in range(-6, 7, 3):
            for dz in range(-6, 7, 3):
                s.set(cx + dx, y, cz + dz, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
                s.set(cx + dx, y + 1, cz + dz, mc("azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
                if rng.random() < 0.5:
                    spots.append((cx + dx + 1, y, cz + dz))
    elif centre == "bed":
        for dx in (-3, 2):
            colour = rng.choice(WOOL)
            s.set(cx + dx, y, cz, mc(f"{colour}_bed"), {"facing": "south", "part": "head", "occupied": "false"})
            s.set(cx + dx, y, cz - 1, mc(f"{colour}_bed"), {"facing": "south", "part": "foot", "occupied": "false"})
            s.set(cx + dx + 1, y, cz + 1, mc("barrel"), {"facing": "up", "open": "false"})
        spots += [(cx - 2, y, cz), (cx + 1, y, cz - 1)]
    elif centre == "plushpile":
        for _ in range(14):
            bx, bz = cx + rng.randint(-5, 5), cz + rng.randint(-5, 5)
            for k in range(rng.randint(1, 2)):
                s.set(bx, y + k, bz, mc(rng.choice(["pink", "white", "magenta"]) + "_wool"))
        spots += [(cx + rng.randint(-5, 5), y, cz + rng.randint(-5, 5)) for _ in range(3)]
    elif centre == "easels":
        for dx in (-3, 0, 3):
            s.set(cx + dx, y, cz, mc("oak_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            s.set(cx + dx, y + 1, cz, mc(rng.choice(WOOL) + "_wool"))
        spots.append((cx + 1, y, cz + 1))


# =====================================================================================================================
# 2.3.0: the guh castle (legendary rare, 256x256): the Koningguh on his throne, a garden, a maze, a dungeon
# =====================================================================================================================
def kasteel():
    # --- the royal throne: a seat with a guh head for a backrest ---
    gold, velvet, wood = "minecraft:block/gold_block", "minecraft:block/purple_wool", "minecraft:block/stripped_cherry_log"
    els = [el([1, 0, 1], [15, 7, 15], "#wood"), el([1, 7, 1], [15, 9, 13], "#velvet"),               # seat
           el([1, 7, 12], [15, 26, 16], "#velvet"), el([0, 7, 11], [16, 9, 16], "#gold"),            # back
           el([0, 9, 2], [2, 13, 12], "#gold"), el([14, 9, 2], [16, 13, 12], "#gold"),              # armrests
           el([-1, 24, 11], [17, 27, 16], "#gold"),                                                 # top of the back
           el([-1, 27, 12], [4, 32, 15], "#velvet"), el([12, 27, 12], [17, 32, 15], "#velvet"),      # guh ears
           el([4, 16, 11.5], [6, 19, 12], "#dark"), el([10, 16, 11.5], [12, 19, 12], "#dark")]     # guh eyes
    w(f"{A}/models/block/koningstroon.json", {"parent": "minecraft:block/block", "textures": {
        "particle": gold, "gold": gold, "velvet": velvet, "wood": wood, "dark": "minecraft:block/black_wool"}, "elements": els})
    w(f"{A}/blockstates/koningstroon.json", {"variants": {
        f"facing={f},royal={r}": {"model": "guhs:block/koningstroon", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for r in ("false", "true")}})
    w(f"{A}/models/item/koningstroon.json", {"parent": "guhs:block/koningstroon"})
    self_drop("koningstroon")

    # --- the king's compass: royal purple frames ---
    overrides = []
    for i in range(32):
        src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
        frame = f"koningskompas_{i:02d}"
        save(recolour(src, hue=0.77, sat=1.3, val=0.9, only=pinkish), "item", f"{frame}.png")
        item_model(frame)
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": "guhs:item/koningskompas_00"})
    w(f"{A}/models/item/koningskompas.json", {"parent": "minecraft:item/generated",
                                               "textures": {"layer0": "guhs:item/koningskompas_16"}, "overrides": overrides})

    # --- loot: the treasure room and the rest of the castle ---
    w(f"{D}/loot_table/chests/guh_kasteel_schat.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:koningstroon"}]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 3, "functions": count_fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 4, "functions": count_fn(8, 24)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": count_fn(8, 16)},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 2, "functions": count_fn(2, 5)},
            {"type": "minecraft:item", "name": "minecraft:gold_block", "weight": 2, "functions": count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:diamond_guh_armor", "weight": 1}]},
        # 2.9 (kleding): the Koningguh's own outfit is only found here (every piece has one source)
        {"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "guhs:koning_kroon", "weight": 1},
            {"type": "minecraft:item", "name": "guhs:koning_mantel", "weight": 1},
            {"type": "minecraft:item", "name": "guhs:koning_ketting", "weight": 1}]}]})
    w(f"{D}/loot_table/chests/guh_kasteel.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": "guhs:chests/hamster_house"}]},
        {"rolls": 2, "entries": [
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "functions": count_fn(2, 6)},
            {"type": "minecraft:item", "name": "guhs:kaashoning", "functions": count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:knight_helmet", "weight": 1},
            {"type": "minecraft:item", "name": "guhs:knight_armour", "weight": 1}]}]})

    # --- the structure: legendary rare ---
    TEMPLATE_SIZES["guh_kasteel"] = 128        # (the flatness check samples up to this far from the middle)
    FLATNESS["guh_kasteel"] = 48
    structure("guh_kasteel", GUHMENSION_LAND, spacing=120, separation=40, salt=20600001, start_y=0, reach=128,
              centre="guhs:kasteel_midden")
    kasteel_structure()

    for name, parent, icon, frame, crit, title, desc in [
        ("find_guh_kasteel", "enter_guhmension", "guhs:koningskompas", "challenge",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:guh_kasteel"]}}}},
         "Lang leve de Koning!", "Vind het legendarische guhkasteel"),
        ("koning_pakje", "find_guh_kasteel", "guhs:koning_kroon", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:koning_kroon"}, {"items": "guhs:koning_mantel"}, {"items": "guhs:koning_ketting"}]}},
         "Vahoege Majesteit", "Tem de Koningguh en krijg zijn hele koningspakje"),
    ]:
        w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)

    guh_paintings()
    for key, text in {
        "entity.guhs.guh_npc.reisguh": "Reisguh",
        "item.guhs.reisguh_fluitje": "Reisguh-fluitje",
        "item.guhs.reisguh_fluitje.lore": "Fluit op een plek in de Guhmensie: daar komt een Reisguh zitten, je eigen reispunt",
        "quest.guhs.reis.discovered": "Tuut tuut! Je hebt Reisguh '%s' ontdekt. Rechtsklik op me om te reizen naar alle Reisguhs die je kent. Vahoeg!",
        "quest.guhs.reis.only_guhmension": "Njeg... Reisguhs reizen alleen binnen de Guhmensie.",
        "quest.guhs.reis.renamed": "Deze Reisguh heet nu '%s'",
        "quest.guhs.reis.arrived": "Aangekomen bij %s. Tuut tuut!",
        "quest.guhs.reis.portal_name": "Guhportaal (%s, %s)",
        "quest.guhs.reis.own_name": "Reisguh van %s",
        "gui.guhs.reis.name": "Naam",
        "gui.guhs.reis.rename.tooltip": "Geef deze Reisguh een nieuwe naam (voor iedereen)",
        "gui.guhs.reis.where": "Reizen naar:",
        "gui.guhs.reis.to": "%s  (%s blokken)",
        "gui.guhs.reis.to.tooltip": "Klik om erheen te reizen",
        "gui.guhs.reis.none": "Je kent nog geen andere Reisguhs. Zoek ze op (bij elk guhportaal staat er een, en in grote plekken zoals Guhwarden, Knuffeldal en de Guhkermis) en rechtsklik ze om ze te ontdekken!",
        "entity.guhs.guh_npc.poortwachter": "Poortwachter",
        "quest.guhs.poort.halt": "HALT! Wie daar? Alleen vrienden van de guhs mogen het kasteel van de Koning in. Bewijs maar eens dat je een echte guhvriend bent!",
        "quest.guhs.poort.hint1": "Psst... echte guhvrienden zijn heel goed in vadsen. Lekker lui zijn, zeg maar. Zie je dat bankje daar?",
        "quest.guhs.poort.hint2": "Een echte guhvriend kent het geheime guhwoord. Het begint met een N... en klinkt als een tevreden guh: N-j-e-...",
        "quest.guhs.poort.hint3": "Ga eens een halve minuut lekker vadsig op het bankje zitten. Of zeg het geheime woord dat elke guh zegt als hij blij is (in de chat)!",
        "quest.guhs.poort.friend": "Welkom terug, guhvriend! De Koning verwacht je. NJEG!",
        "quest.guhs.poort.welcome_lazy": "Zo vadsig heb ik nog nooit iemand zien zitten... Jij bent echt een guhvriend! Ga maar naar binnen. VAHOEG!",
        "quest.guhs.poort.welcome_word": "NJEG! Dat is het geheime guhwoord! Jij bent een echte guhvriend. Welkom in het kasteel van de Koning!",
        "quest.guhs.poort.pushed": "Ho ho! Eerst bewijzen dat je een guhvriend bent (praat met de poortwachters).",
        "quest.guhs.poort.sitting": "Vadsig uitrusten... %s / %s seconden",
        "entity.guhs.guh.koning": "Koningguh",
        "block.guhs.koningstroon": "Koninklijke guhtroon",
        "item.guhs.koningskompas": "Koningskompas",
        "item.guhs.koningskompas.lore": "Wijst naar het legendarische guhkasteel (Guhmensie). Het kan heel ver weg zijn!",
        "item.guhs.koning_kroon": "Vahoege guhkoningskroon",
        "item.guhs.koning_mantel": "Hermelijnen koningsmantel",
        "item.guhs.koning_ketting": "Gouden guhmedaillon",
    }.items():
        lang(key, text, text)


def guh_paintings():
    """Guh paintings (like vanilla paintings, 16 pixels a block). The guh pile: 9x9, from a photo, made pixelated."""
    src = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "inspiration", "guh_stapel.png")
    if os.path.exists(src):
        from PIL import ImageEnhance
        im = Image.open(src).convert("RGB")
        w_, h_ = im.size
        side = min(w_, h_)
        im = im.crop(((w_ - side) // 2 + 10, 0, (w_ - side) // 2 + 10 + side, side))
        im = ImageEnhance.Contrast(ImageEnhance.Color(im).enhance(1.25)).enhance(1.1)
        small = im.resize((144, 144), Image.LANCZOS).quantize(colors=40, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGBA")
        px = small.load()
        for i in range(144):                                   # a thin dark wooden frame, like vanilla paintings
            for c in ((i, 0), (i, 143), (0, i), (143, i), (i, 1), (i, 142), (1, i), (142, i)):
                px[c] = (74, 52, 34, 255)
        save(small, "painting", "guh_stapel.png")
    w(f"{D}/painting_variant/guh_stapel.json", {"asset_id": "guhs:guh_stapel", "width": 9, "height": 9})
    w(f"{R}/data/minecraft/tags/painting_variant/placeable.json", {"replace": False, "values": ["guhs:guh_stapel"]})
    lang("painting.guhs.guh_stapel.title", "De Guhstapel", "De Guhstapel")
    lang("painting.guhs.guh_stapel.author", "De guhs", "De guhs")


KASTEEL_G = 12          # ground level in the template (the dungeon is below it)


def kasteel_structure():
    """The guh castle, gothic edition: 256x256. A moat of kaas saus with a drawbridge and a twin-tower gatehouse (two gate
    guards: only friends of the guhs get in), buttressed curtain walls with wall towers, and in the middle the guhdraal:
    a cathedral-castle with a tall nave (the throne room), transepts (feast hall and library), flying buttresses with
    pinnacles, a rose window between two spired towers and a crossing tower with the giant guh head on top. South of it the
    royal garden in pink and green: a pergola avenue with two Koningguh statues, a fountain plaza, parterres, a rose garden,
    a hedge maze and a pond. A dungeon with Mikas east of the nave (stairs down from the aisle chapel and from the kitchen)."""
    import make_structures as _ms
    W, H, D = 256, 172, 256
    G = KASTEEL_G
    s = Structure((W, H, D))
    rng = random.Random(2304)
    Q, QB, QP, PK, MG, GO = mc("quartz_bricks"), mc("quartz_block"), mc("quartz_pillar"), mc("pink_concrete"), mc("magenta_concrete"), mc("gold_block")
    PT = mc("pink_terracotta")
    GLASS = [mc("magenta_stained_glass"), mc("pink_stained_glass"), mc("purple_stained_glass"), mc("light_blue_stained_glass")]
    LEAVES = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    lamp = lambda hanging: ("guhs:lampion_" + rng.choice(["roze", "geel", "mint"]), {"hanging": hanging, "waterlogged": "false"})

    def stairs(block, facing, half="bottom"):
        return mc(block), {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}

    def fill(x0, y0, z0, x1, y1, z1, name, props=None):
        s.fill(x0, y0, z0, x1, y1, z1, name, props)

    def air(x0, y0, z0, x1, y1, z1):
        fill(x0, y0, z0, x1, y1, z1, mc("air"))

    def arch_top(i, width, rise):
        """Height of a pointed (gothic) arch above its springing line at column i (0..width-1)."""
        d = abs(i - (width - 1) / 2) / (width / 2)
        return int(round((1 - d ** 1.4) * rise))

    def pointed_opening(axis, fixed, a0, width, y0, straight, rise, block="air", props=None):
        """A pointed arch opening (or window) in a wall: axis 'x' = a wall along x at z=fixed, 'z' = along z at x=fixed."""
        for i in range(width):
            top = y0 + straight + arch_top(i, width, rise)
            for y in range(y0, top):
                pos = (a0 + i, y, fixed) if axis == "x" else (fixed, y, a0 + i)
                s.set(*pos, mc(block) if ":" not in block else block, props)

    def spire(cx, cz, y0, r, height, a=MG, b=PK):
        """A pointed spire: a square pyramid narrowing from radius r to a point, striped, with a golden tip."""
        for k in range(height):
            rr = r * (1 - k / height)
            for x in range(int(cx - rr - 1), int(cx + rr + 2)):
                for z in range(int(cz - rr - 1), int(cz + rr + 2)):
                    if max(abs(x - cx), abs(z - cz)) <= rr and max(abs(x - cx), abs(z - cz)) > rr - 1.3:
                        s.set(x, y0 + k, z, a if (k // 3) % 2 else b)
        s.set(int(cx), y0 + height, int(cz), GO)
        for y in range(y0 + height + 1, y0 + height + 4):
            s.set(int(cx), y, int(cz), mc("end_rod"), {"facing": "up"})

    def pinnacle(x, z, y0, h=7):
        fill(x - 1, y0, z - 1, x + 1, y0 + 2, z + 1, Q)
        spire(x, z, y0 + 3, 1.6, h, MG, QB)

    def buttress(x, z, y0, height, out_dx, out_dz, reach=6, arch_y=None):
        """A pier standing out from a wall, with a pinnacle; with arch_y a flying buttress arches over to the wall."""
        px, pz = x + out_dx * reach, z + out_dz * reach
        fill(px - 1, y0, pz - 1, px + 1, y0 + height, pz + 1, Q)
        for y in range(y0, y0 + height, 6):
            fill(px - 1, y, pz - 1, px + 1, y, pz + 1, PK)
        pinnacle(px, pz, y0 + height + 1)
        if arch_y is not None:
            for k in range(1, reach):
                ax, az = px - out_dx * k, pz - out_dz * k
                y = y0 + height + int(round((arch_y - (y0 + height)) * (k / reach) ** 0.8))
                for yy in (y, y - 1):
                    s.set(ax, yy, az, Q)
                s.set(ax, y - 2, az, *stairs("quartz_stairs", {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(out_dx, out_dz)], "top"))

    def round_tower2(cx, cz, r, y0, h, spire_h=None, beds=False):
        """A round wall tower: quartz and pink bands, arrow-slit windows, floors every 8, a spiral of ladders, a spire."""
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.dist((x, z), (cx, cz))
                if d > r + 0.5:
                    continue
                for y in range(y0, y0 + h):
                    if d > r - 1.5:
                        ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                        slit = (y - y0) % 8 in (3, 4, 5) and int(ang) % 60 < 8
                        s.set(x, y, z, mc("purple_stained_glass_pane") if slit else (Q if (y - y0) % 7 else PK))
                    elif (y - y0) % 8 == 0 and (x, z) != (cx + r - 2, cz):
                        s.set(x, y, z, mc("stripped_cherry_wood"), {"axis": "y"})
                    else:
                        s.set(x, y, z, mc("air"))
                s.set(x, y0 + h, z, QB)
                if d > r - 0.5 and (x + z) % 2 == 0:                   # battlements
                    s.set(x, y0 + h + 1, z, QB)
                if abs(d - (r + 0.6)) < 0.6:                            # a corbelled ring under the battlements
                    pass
        for y in range(y0 + 1, y0 + h):                                 # ladders up, one shaft
            s.set(cx + r - 2, y, cz, mc("ladder"), {"facing": "west", "waterlogged": "false"})
        if beds:
            for fy in range(y0 + 8, y0 + h - 4, 8):
                s.set(cx - 1, fy + 1, cz, mc(rng.choice(WOOL) + "_bed"), {"facing": "north", "part": "head", "occupied": "false"})
                s.set(cx - 1, fy + 1, cz + 1, s.get(cx - 1, fy + 1, cz), {"facing": "north", "part": "foot", "occupied": "false"})
                s.set(cx + 1, fy + 1, cz - 2, mc("barrel"), {"facing": "up", "open": "false"})
                s.set(cx, fy + 5, cz, *lamp("true"))
        if spire_h:
            for k in range(spire_h):                                   # a round cone roof
                rr = (r + 1.5) * (1 - k / spire_h)
                for x in range(cx - r - 2, cx + r + 3):
                    for z in range(cz - r - 2, cz + r + 3):
                        d = math.dist((x, z), (cx, cz))
                        if rr - 1.3 < d <= rr:
                            ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                            s.set(x, y0 + h + 2 + k, z, MG if int(ang / 30) % 2 else PK)
            top = y0 + h + 2 + spire_h
            s.set(cx, top, cz, GO)
            for y in range(top + 1, top + 5):
                s.set(cx, y, cz, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            for dx in range(1, 4):
                for y in (top + 3, top + 4):
                    s.set(cx + dx, y, cz, mc("magenta_wool") if dx % 2 else mc("pink_wool"))

    # ================================ the ground ==============================================================================
    for x in range(W):
        for z in range(D):
            s.set(x, G - 1, z, mc("dirt"))
            s.set(x, G, z, mc("grass_block"), {"snowy": "false"})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=G + 6)

    # ================================ moat, curtain wall, towers, gatehouse ===================================================
    CX0, CX1, CZ0, CZ1 = 36, 219, 10, 143                              # the curtain wall
    for x in range(CX0 - 7, CX1 + 8):                                   # the moat of kaas saus, 5 wide, with a stone edge
        for z in range(CZ0 - 7, CZ1 + 8):
            out = max(CX0 - x, x - CX1, CZ0 - z, z - CZ1)
            if 1 <= out <= 5:
                for y in range(G - 3, G + 1):
                    s.set(x, y, z, "guhs:kaas_saus" if y > G - 3 else mc("pink_terracotta"), {"level": "0"} if y > G - 3 else None)
            elif out == 6 or out == 0:
                s.set(x, G, z, mc("polished_andesite"))
    # the courtyard: air up to the wall top, a lawn with pink paths
    for x in range(CX0 + 4, CX1 - 3):
        for z in range(CZ0 + 4, CZ1 - 3):
            air(x, G + 1, z, x, G + 26, z)
            if (x - 127) ** 2 < 16 or ((x - 78) ** 2 < 4 and z > 70) or ((x - 177) ** 2 < 4 and z > 70):
                s.set(x, G, z, mc("pink_concrete_powder"))
            elif rng.random() < 0.08:
                s.set(x, G + 1, z, mc("short_grass") if rng.random() < 0.7 else "guhs:roze_gras")
    WALL_H = 24
    for x in range(CX0, CX1 + 1):
        for z in range(CZ0, CZ1 + 1):
            edge = min(x - CX0, CX1 - x, z - CZ0, CZ1 - z)
            if edge >= 4:
                continue
            for y in range(G, G + WALL_H):
                s.set(x, y, z, Q if (y // 5) % 2 else (PK if edge == 0 else Q))
            s.set(x, G + WALL_H, z, QB)
            if edge == 0:
                if (x + z) % 3 != 0:                                  # battlements with gaps
                    fill(x, G + WALL_H + 1, z, x, G + WALL_H + 2, z, QB)
                s.set(x, G + WALL_H - 1, z, *stairs("quartz_stairs", "north" if z == CZ0 else "south" if z == CZ1 else "west" if x == CX0 else "east", "top"))
            if edge == 3 and (x + z) % 14 == 0:
                s.set(x, G + WALL_H + 1, z, *lamp("false"))
    # buttresses along the outside of the wall (pointing into the moat), every 12 blocks
    for x in range(CX0 + 8, CX1 - 6, 12):
        buttress(x, CZ0, G, WALL_H - 6, 0, -1, reach=2)
    for z in range(CZ0 + 8, CZ1 - 6, 12):
        buttress(CX0, z, G, WALL_H - 6, -1, 0, reach=2)
        buttress(CX1, z, G, WALL_H - 6, 1, 0, reach=2)
    # pointed windows in the wall's inside face? no: arrow slits every 6 blocks, high up
    for x in range(CX0 + 3, CX1 - 2, 6):
        for z in (CZ0, CZ1):
            fill(x, G + 14, z, x, G + 17, z, mc("purple_stained_glass_pane"), {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    # corner towers (big, with beds inside) and towers in the middle of each wall
    for (tx, tz) in ((CX0, CZ0), (CX1, CZ0), (CX0, CZ1), (CX1, CZ1)):
        round_tower2(tx, tz, 10, G, 44, spire_h=22, beds=True)
    for (tx, tz) in ((CX0, 76), (CX1, 76), (84, CZ0), (171, CZ0), (84, CZ1), (171, CZ1)):
        round_tower2(tx, tz, 6, G, 34, spire_h=14)
    # the gatehouse: two big towers, a pointed gate, a raised portcullis, a drawbridge over the moat, a guh head above
    GX = 127
    for x in range(GX - 13, GX + 14):
        for z in range(CZ1 - 5, CZ1 + 1):
            fill(x, G, z, x, G + WALL_H + 8, z, Q if (x + z) % 7 else PK)
    for x in range(GX - 13, GX + 14):
        for z in (CZ1 - 5, CZ1):
            if (x + z) % 2:
                s.set(x, G + WALL_H + 9, z, QB)
    pointed_opening("x", CZ1, GX - 5, 11, G + 1, 12, 7)
    for z in range(CZ1 - 5, CZ1 + 1):                                 # the gate passage
        pointed_opening("x", z, GX - 5, 11, G + 1, 12, 7)
        for x in range(GX - 5, GX + 6):
            s.set(x, G, z, mc("polished_andesite"))
    for x in range(GX - 5, GX + 6):                                   # the raised portcullis (just its bottom shows)
        for y in (G + 15, G + 16, G + 17):
            if s.get(x, y, CZ1 - 3) == "minecraft:air":
                s.set(x, y, CZ1 - 3, mc("iron_bars"), {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    for tx in (GX - 16, GX + 16):
        round_tower2(tx, CZ1 - 2, 7, G, 42, spire_h=18)
    for z in range(CZ1 + 1, CZ1 + 8):                                 # the drawbridge
        for x in range(GX - 5, GX + 6):
            s.set(x, G, z, mc("stripped_spruce_log") if x in (GX - 5, GX + 5) else mc("spruce_planks"), {"axis": "z"} if x in (GX - 5, GX + 5) else None)
        for x in (GX - 6, GX + 6):
            s.set(x, G + 1, z, mc("spruce_fence"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    for x in (GX - 6, GX + 6):                                        # the drawbridge chains
        for k in range(8):
            s.set(x, G + 2 + k, CZ1 + 7 - k, mc("chain"), {"axis": "y", "waterlogged": "false"})
    _ms.voxel_guh(s, GX - 7, G + WALL_H - 2, CZ1 + 1, scale=0.55, bones=("head", "ear_left", "ear_right"), face_south=True)
    for x in range(GX - 4, GX + 5):
        s.set(x, G + 13, CZ1 + 2, "guhs:vlaggetjes", {"axis": "x"})
    # the two gate guards (outside the gate, looking out) and the bench for a vadsig rest
    for dx in (-8, 8):
        s.entity(GX + dx + 0.5, G + 1.0, CZ1 + 9.5, {"id": "guhs:guh_npc", "Kind": "poortwachter", "PersistenceRequired": Byte(1),
                                                      "Rotation": floats(0.0, 0.0)})
        fill(GX + dx, G, CZ1 + 9, GX + dx, G, CZ1 + 9, QB)
    for x in range(GX + 11, GX + 14):
        s.set(x, G + 1, CZ1 + 12, "guhs:guh_bank", {"facing": "south"})
    s.set(GX + 14, G + 1, CZ1 + 12, *lamp("false"))
    s.set(GX + 10, G + 1, CZ1 + 12, *lamp("false"))
    for dx in (-3, 3):
        guard(s, GX + dx + 0.5, G + 1, CZ1 - 8.5, 180.0)             # knights inside the gate

    # ================================ the guhdraal: a cathedral with a tall nave, lower aisles, transepts and an apse ============
    NX0, NX1 = 106, 149                                               # the outer walls of the aisles
    MX0, MX1 = 116, 139                                               # the high nave between the arcades
    NZ0, NZ1 = 40, 118                                                # the apse starts at NZ0; the west front at NZ1 (south)
    TZ0, TZ1 = 60, 79                                                 # the transepts (feast hall west, library east)
    TX0, TX1 = 76, 179
    NAVE_H, AISLE_H, TRANS_H = 44, 22, 32
    BAYS = list(range(NZ0 + 4, NZ1 - 10, 8))                          # where the pillars stand
    crossing = lambda z: TZ0 - 1 <= z <= TZ1 + 1

    def hall(x0, z0, x1, z1, height, floor_block, windows=True):
        """A hall: walls of quartz with pink bands, pointed windows, a floor, air inside, a flat top."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                wall = x in (x0, x1) or z in (z0, z1)
                for y in range(G, G + height):
                    if wall:
                        s.set(x, y, z, PK if (y - G) % 9 == 0 else Q)
                    elif y == G:
                        s.set(x, y, z, floor_block if (x + z) % 2 else mc("polished_diorite"))
                    else:
                        s.set(x, y, z, mc("air"))
                s.set(x, G + height, z, QB)
        if windows:
            for x in range(x0 + 4, x1 - 3, 6):
                for z in (z0, z1):
                    pointed_opening("x", z, x, 3, G + 5, height - 12, 3, GLASS[(x // 6) % 4])
            for z in range(z0 + 4, z1 - 3, 6):
                for x in (x0, x1):
                    pointed_opening("z", x, z, 3, G + 5, height - 12, 3, GLASS[(z // 6) % 4])

    def roof(x0, z0, x1, z1, y0, along):
        """A steep gable roof (ridge along x or z) in magenta and pink stripes, with a gold ridge and closed gable ends."""
        span = (z1 - z0) if along == "x" else (x1 - x0)
        half = span // 2 + 1
        for k in range(half + 1):
            for a in range((x0 if along == "x" else z0) - 1, (x1 if along == "x" else z1) + 2):
                for side in (0, 1):
                    b = (z0 - 1 + k if side == 0 else z1 + 1 - k) if along == "x" else (x0 - 1 + k if side == 0 else x1 + 1 - k)
                    x, z = (a, b) if along == "x" else (b, a)
                    for dy in (0, 1):
                        s.set(x, y0 + k * 2 + dy, z, MG if (a // 3) % 2 else PK)
            for end in ((x0 if along == "x" else z0), (x1 if along == "x" else z1)):   # the gable ends, filled in
                for b in range((z0 if along == "x" else x0) + k, (z1 if along == "x" else x1) + 1 - k):
                    x, z = (end, b) if along == "x" else (b, end)
                    for dy in (0, 1):
                        s.set(x, y0 + k * 2 + dy, z, Q)
        mid = (z0 + z1) // 2 if along == "x" else (x0 + x1) // 2
        for a in range((x0 if along == "x" else z0) - 1, (x1 if along == "x" else z1) + 2):
            x, z = (a, mid) if along == "x" else (mid, a)
            s.set(x, y0 + half * 2 + 1, z, GO)

    # 1. the transepts (first: the nave cuts through them)
    hall(TX0, TZ0, TX1, TZ1, TRANS_H, mc("smooth_quartz"))
    roof(TX0, TZ0, TX1, TZ1, G + TRANS_H + 1, "x")
    # 2. the aisles: the whole width, low
    hall(NX0, NZ0, NX1, NZ1, AISLE_H, mc("pink_glazed_terracotta"))
    # 3. the high nave: clerestory walls on the arcades, tall windows, a flat vault
    for x in range(MX0, MX1 + 1):
        for z in range(NZ0, NZ1 + 1):
            wall = x in (MX0, MX1) or z in (NZ0, NZ1)
            for y in range(G + 1, G + NAVE_H):
                if not wall:
                    s.set(x, y, z, mc("air"))
                elif y >= G + AISLE_H or z in (NZ0, NZ1):
                    s.set(x, y, z, PK if (y - G) % 9 == 0 else Q)
            s.set(x, G + NAVE_H, z, QB)
    for z in range(NZ0 + 6, NZ1 - 4, 8):                                 # clerestory windows
        if not crossing(z):
            for x in (MX0, MX1):
                pointed_opening("z", x, z, 3, G + AISLE_H + 9, 7, 3, GLASS[(z // 8) % 4])
    # the arcades: pillars with pointed arches between them, open to the aisles
    for x in (MX0, MX1):
        for z in range(NZ0 + 1, NZ1):
            for y in range(G + 1, G + AISLE_H):
                s.set(x, y, z, mc("air"))
        for i, z in enumerate(BAYS):
            fill(x - 1, G + 1, z - 1, x + 1, G + AISLE_H - 1, z + 1, QP, {"axis": "y"})
            fill(x - 1, G + AISLE_H - 4, z - 1, x + 1, G + AISLE_H - 4, z + 1, GO)
            if z + 8 < NZ1 - 4:
                for j in range(5):
                    top = G + AISLE_H - 8 + arch_top(j, 5, 5)
                    fill(x, top, z + 2 + j, x, G + AISLE_H - 1, z + 2 + j, Q)
    # ribs of the vault across the nave, from pillar to pillar
    for z in BAYS:
        if crossing(z):
            continue
        w = MX1 - MX0 - 1
        for j in range(w):
            y = G + NAVE_H - 9 + arch_top(j, w, 8)
            s.set(MX0 + 1 + j, y, z, Q)
            s.set(MX0 + 1 + j, min(y + 1, G + NAVE_H - 1), z, Q)
    # 4. roofs: a lean-to over each aisle (pink stairs), the steep main roof over the nave
    for k in range(MX0 - NX0 + 1):
        y = G + AISLE_H + 1 + k * 7 // (MX0 - NX0)
        for z in range(NZ0 - 1, NZ1 + 2):
            if crossing(z):
                continue
            s.set(NX0 - 1 + k, y, z, *stairs("purpur_stairs" if (z // 3) % 2 else "quartz_stairs", "east"))
            s.set(NX1 + 1 - k, y, z, *stairs("purpur_stairs" if (z // 3) % 2 else "quartz_stairs", "west"))
            for yy in range(G + AISLE_H + 1, y):
                s.set(NX0 - 1 + k, yy, z, Q)
                s.set(NX1 + 1 - k, yy, z, Q)
    roof(MX0, NZ0, MX1, NZ1, G + NAVE_H + 1, "z")
    # 5. the crossing: open through the aisles into both transepts, high up to the nave's vault
    for x in range(NX0, NX1 + 1):
        for z in range(TZ0 + 1, TZ1):
            top = G + (NAVE_H - 1 if MX0 < x < MX1 else TRANS_H - 1)
            for y in range(G + 1, top + 1):
                s.set(x, y, z, mc("air"))
            s.set(x, G, z, mc("smooth_quartz") if (x + z) % 2 else mc("pink_glazed_terracotta"))
    for x in (MX0, MX1):                                                 # the crossing piers
        for z in (TZ0, TZ1):
            fill(x - 1, G + 1, z - 1, x + 1, G + NAVE_H - 1, z + 1, QP, {"axis": "y"})
    # 6. the apse: a half round end behind the throne, tall windows, a half dome
    AR, ACZ = 12, NZ0
    for x in range(128 - AR - 1, 128 + AR + 1):
        for z in range(ACZ - AR - 1, ACZ + 1):
            d = math.dist((x + 0.5, z + 0.5), (128, ACZ + 0.5))
            if d <= AR:
                for y in range(G, G + NAVE_H - 4):
                    if d > AR - 1.2:
                        ang = math.degrees(math.atan2(z - ACZ, x - 127.5)) % 360
                        win = 6 <= y - G <= 30 and int(ang) % 30 < 5
                        s.set(x, y, z, GLASS[int(ang / 30) % 4] if win else (PK if (y - G) % 9 == 0 else Q))
                    elif y == G:
                        s.set(x, y, z, mc("smooth_quartz"))
                    elif z < ACZ:
                        s.set(x, y, z, mc("air"))
                for k in range(0, 12):
                    if AR * (1 - k / 12) - 1.6 < d <= AR * (1 - k / 12):
                        s.set(x, G + NAVE_H - 4 + k, z, MG if k % 2 else PK)
    for x in range(MX0 + 1, MX1):                                        # the nave opens into the apse
        air(x, G + 1, NZ0, x, G + NAVE_H - 6, NZ0)
    # 7. inside: a red carpet all the way, banners and lamps
    for z in range(NZ0 + 8, NZ1):
        for x in range(124, 132):
            s.set(x, G + 1, z, mc("red_carpet") if 125 <= x <= 130 else mc("yellow_carpet"))
    for x in range(MX0 + 4, MX1 - 2, 7):
        for z in range(NZ0 + 6, NZ1 - 3, 10):
            s.set(x, G + NAVE_H - 3, z, *lamp("true"))
    for z in range(NZ0 + 6, NZ1 - 12, 10):
        for x in (NX0 + 3, NX1 - 3):
            if not crossing(z):
                s.set(x, G + AISLE_H - 2, z, *lamp("true"))
        for x in (NX0 + 1, NX1 - 1):
            if not crossing(z):
                fill(x, G + 8, z, x, G + 16, z, mc("purple_wool"))
                s.set(x, G + 17, z, GO)
    # the dais and the throne, the Koningguh, four knights
    for x in range(119, 137):
        for z in range(NZ0 - 6, NZ0 + 7):
            if s.get(x, G + 1, z) in (None, "minecraft:air", "minecraft:red_carpet", "minecraft:yellow_carpet"):
                s.set(x, G + 1, z, QB)
                s.set(x, G + 2, z, mc("purple_carpet") if abs(x - 127.5) < 7 else mc("magenta_carpet"))
    for x in range(121, 135):
        s.set(x, G + 1, NZ0 + 7, *stairs("quartz_stairs", "north"))
    for x in range(123, 133):
        for z in range(NZ0 - 4, NZ0 + 3):
            s.set(x, G + 2, z, QB)
            s.set(x, G + 3, z, mc("purple_carpet"))
    for x in range(124, 132):
        s.set(x, G + 2, NZ0 + 3, *stairs("quartz_stairs", "north"))
    s.set(127, G + 3, NZ0 - 1, "guhs:koningstroon", {"facing": "south", "royal": "true"})
    # behind the throne a screen with the great painting of the guh pile (9x9), in a golden frame
    for x in range(121, 134):
        for y in range(G + 3, G + 19):
            s.set(x, y, NZ0 - 7, GO if x in (121, 133) or y in (G + 5, G + 18) else Q)
    for x in range(121, 134):
        s.set(x, G + 19, NZ0 - 7, *stairs("quartz_stairs", "south"))
    pinnacle(121, NZ0 - 7, G + 19, 4)
    pinnacle(133, NZ0 - 7, G + 19, 4)
    s.entity(127.5, G + 11.5, NZ0 - 5.5, {"id": "minecraft:painting", "variant": "guhs:guh_stapel", "facing": Byte(0),
                                          "TileX": 127, "TileY": G + 11, "TileZ": NZ0 - 6})
    s.entity(127.5, G + 3.3, NZ0 - 0.5, koning_nbt())
    for x in (121, 134):
        fill(x, G + 3, NZ0 - 5, x, G + 16, NZ0 - 5, mc("purple_wool"))
        s.set(x, G + 17, NZ0 - 5, GO)
    for (x, z) in ((122.5, NZ0 + 9.5), (133.5, NZ0 + 9.5), (122.5, NZ0 + 22.5), (133.5, NZ0 + 22.5)):
        guard(s, x, G + 1, z, 90.0 if x < 127 else 270.0)
    # the treasure room: behind the apse, a door behind the throne
    TRZ = ACZ - AR - 7
    for x in range(123, 133):
        for z in range(TRZ, ACZ - AR + 1):
            for y in range(G, G + 8):
                s.set(x, y, z, Q if x in (123, 132) or z == TRZ or y in (G, G + 7) else mc("air"))
    air(127, G + 1, ACZ - AR - 1, 128, G + 4, ACZ - AR + 1)
    chest(s, 124, G + 1, TRZ + 1, "south", "guhs:chests/guh_kasteel_schat")
    chest(s, 131, G + 1, TRZ + 1, "south", "guhs:chests/guh_kasteel_schat")
    s.set(127, G + 6, TRZ + 3, *lamp("true"))
    # the feast hall (west transept) and the library (east transept), with a balcony
    for x in range(TX0 + 4, NX0 - 3):
        s.set(x, G + 1, 69, "guhs:guh_tafel", {"facing": "north"})
        s.set(x, G + 1, 70, "guhs:guh_tafel", {"facing": "north"})
        if x % 2 == 0:
            s.set(x, G + 1, 67, "guhs:guh_stoel", {"facing": "south"})
            s.set(x, G + 1, 72, "guhs:guh_stoel", {"facing": "north"})
        if x % 5 == 0:
            s.set(x, G + 2, 69, mc("cake"), {"bites": "0"})
            s.set(x, G + 12, 69, *lamp("true"))
    for x in range(NX1 + 3, TX1 - 1):
        for z in (TZ0 + 1, TZ1 - 1):
            fill(x, G + 1, z, x, G + 5, z, mc("bookshelf"))
            fill(x, G + 9, z, x, G + 13, z, mc("bookshelf"))
        for z in (TZ0 + 2, TZ0 + 3, TZ1 - 3, TZ1 - 2):
            s.set(x, G + 8, z, mc("stripped_cherry_wood"), {"axis": "y"})
        if x % 4 == 0:
            s.set(x, G + 1, 69, mc("lectern"), {"facing": "north", "has_book": "false", "powered": "false"})
    for y in range(G + 1, G + 9):
        s.set(TX1 - 2, y, TZ0 + 3, mc("ladder"), {"facing": "west", "waterlogged": "false"})
    # 8. the west front: two towers with tall spires over the aisles, a great portal, a rose window, a gable
    for tx in (NX0, MX1):
        for x in range(tx, tx + 11):
            for z in range(NZ1 - 10, NZ1 + 1):
                wall = x in (tx, tx + 10) or z in (NZ1 - 10, NZ1)
                for y in range(G, G + 80):
                    if wall:
                        s.set(x, y, z, PK if (y - G) % 9 == 0 else Q)
                    elif (y - G) % 13 == 0:
                        s.set(x, y, z, mc("stripped_cherry_wood") if y > G else mc("polished_diorite"), {"axis": "y"} if y > G else None)
                    else:
                        s.set(x, y, z, mc("air"))
        for y0 in range(G + 10, G + 72, 13):
            pointed_opening("x", NZ1, tx + 4, 3, y0, 5, 2, GLASS[(y0 // 13) % 4])
            pointed_opening("z", tx if tx == NX0 else tx + 10, NZ1 - 6, 3, y0, 5, 2, GLASS[(y0 // 13 + 1) % 4])
        for x in range(tx, tx + 11):
            for z in range(NZ1 - 10, NZ1 + 1):
                if (x in (tx, tx + 10) or z in (NZ1 - 10, NZ1)) and (x + z) % 2:
                    s.set(x, G + 81, z, QB)
                s.set(x, G + 80, z, QB)
        for (px, pz) in ((tx, NZ1 - 10), (tx + 10, NZ1 - 10), (tx, NZ1), (tx + 10, NZ1)):
            pinnacle(px, pz, G + 81, 8)
        spire(tx + 5, NZ1 - 5, G + 81, 5.5, 38)
        pointed_opening("z", tx + 10 if tx == NX0 else tx, NZ1 - 6, 3, G + 1, 4, 2)     # a door from the nave into the tower
        for y in range(G + 1, G + 80):                                  # and a ladder up
            s.set(tx + 5, y, NZ1 - 9, mc("ladder"), {"facing": "south", "waterlogged": "false"})
    RW, RY = 8, G + 36                                                   # the rose window
    for x in range(128 - RW - 1, 128 + RW + 1):
        for y in range(RY - RW - 1, RY + RW + 1):
            d = math.dist((x + 0.5, y + 0.5), (128, RY + 0.5))
            if d <= RW:
                ang = math.degrees(math.atan2(y + 0.5 - RY, x + 0.5 - 128)) % 360
                ring = int(d / 2.4)
                s.set(x, y, NZ1, GO if d > RW - 0.8 or (int(ang) % 30 < 4 and d > 2) else GLASS[(ring + int(ang / 30)) % 4])
    for i, x in enumerate(range(121, 135)):                              # the great portal: open, with a golden arch
        top = G + 1 + 15 + arch_top(i, 14, 9)
        for z in (NZ1, NZ1 + 1):
            air(x, G + 1, z, x, top - 1, z)
            s.set(x, top, NZ1 + 1, GO)
    for x in range(119, 137):                                            # a forecourt, level with the nave floor
        for z in range(NZ1 + 1, NZ1 + 8):
            s.set(x, G, z, QB if (x + z) % 2 else mc("pink_concrete"))
    for x in (119, 136):                                                 # statues of knights by the portal
        guard(s, x + 0.5, G + 1, NZ1 + 3.5, 0.0)
    # 9. flying buttresses over the aisles, with pinnacles
    for z in BAYS:
        if crossing(z) or z > NZ1 - 12:
            continue
        buttress(MX0, z, G, 26, -1, 0, reach=15, arch_y=G + 38)
        buttress(MX1, z, G, 26, 1, 0, reach=15, arch_y=G + 38)
    for z in range(NZ0 + 2, NZ1 - 12, 8):                                 # pinnacles along the aisle walls
        if not crossing(z):
            pinnacle(NX0, z, G + AISLE_H + 1, 5)
            pinnacle(NX1, z, G + AISLE_H + 1, 5)
    # 10. the crossing tower, with the giant guh head on top
    CX, CZ = 128, 69
    for x in range(CX - 12, CX + 12):
        for z in range(CZ - 12, CZ + 12):
            wall = x in (CX - 12, CX + 11) or z in (CZ - 12, CZ + 11)
            for y in range(G + NAVE_H, G + 92):
                s.set(x, y, z, (PK if (y - G) % 9 == 0 else Q) if wall else mc("air"))
            if (x + z) % 2 and wall:
                s.set(x, G + 93, z, QB)
            s.set(x, G + 92, z, QB)
    for y0 in range(G + 52, G + 87, 12):
        for a in range(CX - 9, CX + 9, 6):
            pointed_opening("x", CZ - 12, a, 3, y0, 5, 2, GLASS[(y0 // 12) % 4])
            pointed_opening("x", CZ + 11, a, 3, y0, 5, 2, GLASS[(y0 // 12 + 1) % 4])
            pointed_opening("z", CX - 12, a - CX + CZ, 3, y0, 5, 2, GLASS[(y0 // 12 + 2) % 4])
            pointed_opening("z", CX + 11, a - CX + CZ, 3, y0, 5, 2, GLASS[(y0 // 12 + 3) % 4])
    for (px, pz) in ((CX - 12, CZ - 12), (CX + 11, CZ - 12), (CX - 12, CZ + 11), (CX + 11, CZ + 11)):
        pinnacle(px, pz, G + 94, 12)
    nx, ny, nz = _ms.voxel_guh(s, CX - 20, G + 93, CZ - 11, scale=1.75, bones=("head", "ear_left", "ear_right"), face_south=True)
    print(f"guh castle: the guh head on the crossing tower is {nx}x{ny}x{nz}, its top at y {G + 93 + ny}")

    # ================================ the kitchen annex, the dungeon ===========================================================
    for x in range(184, 206):                                            # the kitchen: a lower building in the east courtyard
        for z in range(96, 120):
            wall = x in (184, 205) or z in (96, 119)
            for y in range(G, G + 10):
                s.set(x, y, z, (Q if (y - G) % 5 else PK) if wall else (mc("spruce_planks") if y == G else mc("air")))
    roof(184, 96, 205, 119, G + 11, "z")
    pointed_opening("z", 184, 106, 3, G + 1, 3, 2)
    for z in range(98, 118, 3):
        s.set(204, G + 1, z, mc("smoker"), {"facing": "west", "lit": "false"})
        s.set(204, G + 2, z, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(186, G + 1, 98, mc("water_cauldron"), {"level": "3"})
    chest(s, 186, G + 1, 117, "east", "guhs:chests/guh_kasteel")
    DY = 2
    DX0, DX1, DZ0, DZ1 = 150, 206, 108, 128
    for x in range(DX0, DX1 + 1):
        for z in range(DZ0, DZ1 + 1):
            for y in range(DY, DY + 6):
                corridor = DZ0 + 8 <= z <= DZ0 + 11
                cell = (z < DZ0 + 6 or z > DZ0 + 13) and (x - DX0) % 8 != 0 and DX0 < x < DX1
                s.set(x, y, z, mc("air") if (corridor or cell) and DY < y and DX0 < x < DX1 and DZ0 < z < DZ1 else mc("deepslate_bricks"))
            s.set(x, DY, z, mc("deepslate_tiles"))
        if (x - DX0) % 8 == 4:
            for y in range(DY + 1, DY + 4):
                for zz in (DZ0 + 6, DZ0 + 7, DZ0 + 12, DZ0 + 13):
                    s.set(x, y, zz, mc("iron_bars"), {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    for x in (195, 196):                                                 # where the stairs come in
        air(x, DY + 1, DZ0, x, DY + 5, DZ0 + 11)
    for x in (160, 186):
        s.set(x, DY + 1, DZ0 + 9, mc("spawner"), None, {"id": "minecraft:mob_spawner", "SpawnData": {"entity": {"id": "guhs:mika"}},
                                                        "MinSpawnDelay": Short(300), "MaxSpawnDelay": Short(900), "SpawnCount": Short(1),
                                                        "MaxNearbyEntities": Short(3)})
    chest(s, DX0 + 2, DY + 1, DZ0 + 16, "north", "guhs:chests/guh_kasteel_schat")
    for x in (DX0 + 6, DX0 + 22, DX0 + 38):
        s.entity(x + 0.5, DY + 1.0, DZ0 + 2.5, {"id": "guhs:mika", "PersistenceRequired": Byte(1)})
    for x in range(DX0 + 4, DX1, 10):
        s.set(x, DY + 5, DZ0 + 9, mc("soul_lantern"), {"hanging": "true", "waterlogged": "false"})
    fence = lambda **sides: {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false", **sides}

    def way_down(cells, facing, top_hole):
        """A walkable stair (2 wide) from the ground floor down to the dungeon floor: cells = the ten (x, z) columns per
        step lane, top first; every step 1 lower, the last one on the dungeon floor, with head room all the way."""
        for i, lane in enumerate(cells):
            y = G - 1 - i
            for x, z in lane:
                s.set(x, y, z, *stairs("quartz_stairs", facing))
                air(x, y + 1, z, x, max(y + 3, G - 1) if i < top_hole else y + 3, z)
    # from the kitchen (east courtyard): down to the south, into the corridor's east end
    way_down([[(x, 100 + i) for x in (195, 196)] for i in range(10)], "north", 3)
    for z in (100, 101, 102):                                           # a railing round the opening in the kitchen floor
        s.set(194, G + 1, z, mc("spruce_fence"), fence(north="true", south="true"))
        s.set(197, G + 1, z, mc("spruce_fence"), fence(north="true", south="true"))
    for x in (194, 195, 196, 197):
        s.set(x, G + 1, 103, mc("spruce_fence"), fence(east="true", west="true"))
    # from the guhdraal (the chapel in the east aisle): down to the east, through the dungeon's west wall into the corridor
    way_down([[(140 + i, z) for z in (DZ0 + 8, DZ0 + 9)] for i in range(10)], "west", 3)
    air(DX0, DY + 1, DZ0 + 8, DX0, DY + 3, DZ0 + 9)
    for x in (141, 142, 143):                                           # a railing round the stairwell in the chapel floor
        s.set(x, G + 1, DZ0 + 7, mc("dark_oak_fence"), fence(east="true", west="true"))
    for z in (DZ0 + 8, DZ0 + 9):
        s.set(143, G + 1, z, mc("dark_oak_fence"), fence(north="true", south="true"))
    s.set(146, DY + 7, DZ0 + 8, mc("soul_lantern"), {"hanging": "true", "waterlogged": "false"})
    s.set(146, DY + 8, DZ0 + 8, mc("deepslate_bricks"))

    # ================================ around the walls: lamps, a tournament field (west), a farm and a windmill (east) ========
    for x in range(CX0 - 7, CX1 + 8):
        for z in range(CZ0 - 7, CZ1 + 8):
            if max(CX0 - x, x - CX1, CZ0 - z, z - CZ1) == 6 and (x + z) % 10 == 0 and z < CZ1:
                fill(x, G + 1, z, x, G + 2, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
                s.set(x, G + 3, z, *lamp("false"))
    fence_ns = {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"}
    fence_ew = {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"}
    # the tournament field: lists with a tilt barrier, stands, banners, three striped tents
    for z in range(40, 129):
        s.set(4, G + 1, z, mc("cherry_fence"), fence_ns)
        s.set(24, G + 1, z, mc("cherry_fence"), fence_ns)
        if 48 <= z <= 120:
            s.set(14, G + 1, z, mc("cherry_fence"), fence_ns)
            s.set(14, G + 2, z, mc("pink_carpet") if (z // 2) % 2 else mc("white_carpet"))
        for x in range(5, 24):
            s.set(x, G, z, mc("coarse_dirt") if (x + z) % 3 else mc("dirt_path"))
    for x in range(4, 25):
        s.set(x, G + 1, 40, mc("cherry_fence"), fence_ew)
        s.set(x, G + 1, 128, mc("cherry_fence"), fence_ew)
    for z in range(46, 124, 12):                                        # banners on poles along the lists
        for x in (4, 24):
            fill(x, G + 2, z, x, G + 6, z, mc("cherry_fence"), fence_ns)
            fill(x, G + 4, z + 1, x, G + 6, z + 1, mc("magenta_wool") if (z // 12) % 2 else mc("pink_wool"))
    for k in range(3):                                                   # stands for the audience, outside the east fence
        for z in range(50, 118):
            s.set(25 + k, G + 1 + k, z, *stairs("cherry_stairs", "west"))
            for y in range(G + 1, G + 1 + k):
                s.set(25 + k, y, z, mc("cherry_planks"))
    for i, (tz, c) in enumerate(((12, "magenta_wool"), (22, "light_blue_wool"), (32, "yellow_wool"))):
        tx = 12 + (i % 2) * 6
        for dy in range(5):
            r = 4 - dy
            for x in range(tx - r, tx + r + 1):
                for z in range(tz - r, tz + r + 1):
                    if max(abs(x - tx), abs(z - tz)) == r:
                        s.set(x, G + 1 + dy, z, mc(c) if (x + z) % 2 else mc("white_wool"))
        air(tx, G + 1, tz + 4, tx, G + 2, tz + 4)
        fill(tx, G + 6, tz, tx, G + 8, tz, mc("cherry_fence"), fence_ns)
        s.set(tx + 1, G + 8, tz, mc(c))
        chest(s, tx, G + 1, tz, "south", "guhs:chests/guh_kasteel")
    for (x, z) in ((10.5, 60.5), (18.5, 108.5)):
        guard(s, x, G + 1, z, 90.0 if x < 14 else 270.0)
    # the farm: fields of kaasknabbels with water, a scarecrow guh, knabbelkorven with guh bees, a windmill
    for x in range(230, 253):
        for z in range(10, 72):
            if (x - 230) % 6 == 3:
                s.set(x, G, z, mc("water"), {"level": "0"})
            else:
                s.set(x, G, z, mc("farmland"), {"moisture": "7"})
                s.set(x, G + 1, z, "guhs:kaasknabbelplant", {"age": str(rng.randint(3, 7))})
    for x in range(229, 254):
        for z in (9, 72):
            s.set(x, G + 1, z, mc("cherry_fence"), fence_ew)
    for z in range(9, 73):
        for x in (229, 253):
            s.set(x, G + 1, z, mc("cherry_fence"), fence_ns)
    fill(241, G + 1, 40, 241, G + 2, 40, mc("cherry_fence"), fence_ns)
    s.set(241, G + 3, 40, mc("hay_block"), {"axis": "y"})
    s.set(241, G + 4, 40, mc("carved_pumpkin"), {"facing": "south"})
    fill(240, G + 3, 40, 242, G + 3, 40, mc("cherry_fence"), fence_ew)
    WX, WZ = 241, 94
    for x in range(WX - 5, WX + 6):
        for z in range(WZ - 5, WZ + 6):
            d = math.dist((x, z), (WX, WZ))
            if d <= 4.5:
                for y in range(G + 1, G + 17):
                    s.set(x, y, z, (mc("stripped_cherry_log") if (y - G) % 4 else PK) if d > 3.3 else mc("air"), {"axis": "y"} if d > 3.3 and (y - G) % 4 else None)
                s.set(x, G, z, mc("spruce_planks"))
    for k in range(6):
        for x in range(WX - 5, WX + 6):
            for z in range(WZ - 5, WZ + 6):
                if 4.8 - k - 1.2 < math.dist((x, z), (WX, WZ)) <= 4.8 - k:
                    s.set(x, G + 17 + k, z, MG if k % 2 else PK)
    s.set(WX, G + 23, WZ, GO)
    air(WX, G + 1, WZ + 4, WX, G + 3, WZ + 4)
    for k in range(-8, 9):                                               # the sails, facing south
        for (sx, sy) in ((k, 0), (0, k)):
            if k:
                s.set(WX + sx, G + 12 + sy, WZ + 5, mc("white_wool") if abs(k) > 2 else mc("stripped_cherry_log"), None if abs(k) > 2 else {"axis": "x" if sy == 0 else "y"})
    s.set(WX, G + 12, WZ + 5, GO)
    for x in range(232, 252, 4):                                         # knabbelkorven among flowers
        for z in (112, 124):
            s.set(x, G + 1, z, "guhs:knabbelkorf", {"facing": "south", "honey_level": str(rng.randint(0, 5))})
            for dx in (-1, 1):
                s.set(x + dx, G + 1, z + 1, rng.choice(["guhs:roze_guhbloem", "guhs:kaasbloem", "minecraft:allium", "minecraft:pink_tulip"]))
    for i in range(4):
        s.entity(236.5 + i * 4, G + 3.0, 118.5, {"id": "guhs:guh_bee", "PersistenceRequired": Byte(1)})
    for (x, z) in ((10, 136), (20, 6), (250, 80), (236, 140), (8, 140)):
        blossom_tree(s, rng, x, G + 1, z)

    # ================================ the royal garden ========================================================================
    AX = 127
    GZ0 = CZ1 + 8                                                       # the garden starts past the moat
    garden = [(x, z) for x in range(W) for z in range(GZ0, D)]
    for (x, z) in garden:                                               # lawns in two greens and pink
        s.set(x, G, z, mc("moss_block") if (x // 6 + z // 6) % 2 else mc("grass_block"), None if (x // 6 + z // 6) % 2 else {"snowy": "false"})
    for z in range(GZ0, D):                                             # the avenue, with a pergola of guh blossom
        for x in range(AX - 6, AX + 7):
            s.set(x, G, z, QB if abs(x - AX) == 6 else mc("pink_concrete_powder") if ((z // 2) + abs(x - AX) // 2) % 2 else mc("white_concrete_powder"))
        if z % 6 == 0 and z < D - 8:
            for x in (AX - 7, AX + 7):
                fill(x, G + 1, z, x, G + 5, z, mc("stripped_cherry_log"), {"axis": "y"})
            for x in range(AX - 7, AX + 8):
                s.set(x, G + 6, z, mc("stripped_cherry_log"), {"axis": "x"})
            for x in range(AX - 8, AX + 9):
                for dz in (-1, 0, 1):
                    if rng.random() < 0.75:
                        s.set(x, G + 7, z + dz, "guhs:guhbloesem_leaves", LEAVES)
    # two big Koningguh statues on either side of the avenue, near the drawbridge
    king_bones = _ms.STATUE_BONES + ("koning_manen", "koning_snor", "koning_sok_fl", "koning_sok_fr", "koning_sok_bl", "koning_sok_br")
    outfit = {"guh_clothes/koning_kroon.png": ("outfit_grand_crown",), "guh_clothes/koning_mantel.png": ("outfit_long_cape", "outfit_long_cape_collar"),
              "guh_clothes/koning_ketting.png": ("outfit_chain",)}
    frame = king_bones + tuple(b for bones in outfit.values() for b in bones)
    for side in (-1, 1):
        bx, bz = AX + side * 22, GZ0 + 14
        fill(bx - 9, G + 1, bz - 9, bx + 9, G + 2, bz + 9, QB)
        fill(bx - 8, G + 3, bz - 8, bx + 8, G + 3, bz + 8, PK)
        for (px, pz) in ((bx - 9, bz - 9), (bx + 9, bz - 9), (bx - 9, bz + 9), (bx + 9, bz + 9)):
            pinnacle(px, pz, G + 3, 5)
        _ms.voxel_guh(s, bx - 11, G + 4, bz - 11, scale=0.95, bones=king_bones, texture="guh_koning.png", face_south=True, bounds_bones=frame)
        for tex, bones in outfit.items():
            _ms.voxel_guh(s, bx - 11, G + 4, bz - 11, scale=0.95, bones=bones, texture=tex, face_south=True, bounds_bones=frame)
    # a round fountain plaza half way
    PZ = 205
    for x in range(AX - 18, AX + 19):
        for z in range(PZ - 18, PZ + 19):
            d = math.dist((x, z), (AX, PZ))
            if d <= 18:
                s.set(x, G, z, mc("smooth_quartz") if int(d) % 4 else mc("pink_concrete"))
    for x in range(AX - 9, AX + 10):
        for z in range(PZ - 9, PZ + 10):
            d = math.dist((x, z), (AX, PZ))
            if d <= 9:
                s.set(x, G, z, QB)
                s.set(x, G + 1, z, QB if d > 8 else "guhs:kaas_saus", None if d > 8 else {"level": "0"})
    fill(AX - 1, G + 1, PZ - 1, AX + 1, G + 6, PZ + 1, QP, {"axis": "y"})
    fill(AX - 2, G + 7, PZ - 2, AX + 2, G + 7, PZ + 2, GO)
    _ms.voxel_guh(s, AX - 3, G + 8, PZ - 3, scale=0.3, texture="guh_golden.png")
    for a in range(0, 360, 45):
        x, z = int(AX + 14 * math.cos(math.radians(a))), int(PZ + 14 * math.sin(math.radians(a)))
        s.set(x, G + 1, z, "guhs:guh_bank", {"facing": "north"})
        s.set(x, G + 2, z, mc("air"))
    for a in range(22, 360, 45):
        x, z = int(AX + 16 * math.cos(math.radians(a))), int(PZ + 16 * math.sin(math.radians(a)))
        blossom_tree(s, rng, x, G + 1, z)
    # parterres: box hedges around beds of flowers (pink and green), with topiary balls
    flowers = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes", "minecraft:pink_tulip", "minecraft:allium",
               "minecraft:peony", "minecraft:lilac"]
    for (x0, z0) in ((AX - 60, GZ0 + 34), (AX + 22, GZ0 + 34), (AX - 60, GZ0 + 78), (AX + 22, GZ0 + 78)):
        for x in range(x0, x0 + 38):
            for z in range(z0, z0 + 20):
                edge = x in (x0, x0 + 37) or z in (z0, z0 + 19)
                inner = (x - x0) % 8 == 0 or (z - z0) % 5 == 0
                if edge:
                    s.set(x, G + 1, z, mc("azalea_leaves"), LEAVES)
                elif inner:
                    s.set(x, G, z, mc("pink_concrete_powder"))
                else:
                    s.set(x, G, z, mc("grass_block"), {"snowy": "false"})
                    if rng.random() < 0.7:
                        f = rng.choice(flowers)
                        if f in ("minecraft:peony", "minecraft:lilac"):
                            s.set(x, G + 1, z, f, {"half": "lower"})
                            s.set(x, G + 2, z, f, {"half": "upper"})
                        else:
                            s.set(x, G + 1, z, f)
        for (tx, tz) in ((x0, z0), (x0 + 37, z0), (x0, z0 + 19), (x0 + 37, z0 + 19)):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in (1, 2, 3):
                        if abs(dx) + abs(dz) + abs(dy - 2) <= 2:
                            s.set(tx + dx, G + dy, tz + dz, mc("flowering_azalea_leaves"), LEAVES)
    # the hedge maze (far west), the pond and more fountains (far east), blossom trees and lamps all around
    hedge_maze(s, rng, 6, GZ0 + 30, 11, G)
    pond(s, rng, 222, GZ0 + 70, G)
    for (fx, fz) in ((220, GZ0 + 32), (34, D - 16), (220, D - 16)):
        fountain(s, fx, fz, G)
    taken = lambda x, z: abs(x - AX) < 26 and z < GZ0 + 30 or abs(x - AX) < 9 or math.dist((x, z), (AX, PZ)) < 21 \
        or (4 <= x <= 52 and GZ0 + 28 <= z <= GZ0 + 78) or math.dist((x, z), (222, GZ0 + 70)) < 18 or z > D - 5 or x < 4 or x > W - 5 \
        or any(x0 - 2 <= x <= x0 + 40 and z0 - 2 <= z <= z0 + 22 for (x0, z0) in ((AX - 60, GZ0 + 34), (AX + 22, GZ0 + 34), (AX - 60, GZ0 + 78), (AX + 22, GZ0 + 78)))
    for _ in range(70):
        x, z = rng.randint(4, W - 5), rng.randint(GZ0 + 4, D - 5)
        if not taken(x, z):
            blossom_tree(s, rng, x, G + 1, z)
    for z in range(GZ0 + 4, D - 6, 8):
        for side in (-1, 1):
            s.set(AX + side * 9, G + 1, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            s.set(AX + side * 9, G + 2, z, *lamp("false"))
    # a hedge around the garden and the garden gate at the south
    for x in range(W):
        if abs(x - AX) > 7:
            fill(x, G + 1, D - 1, x, G + 3, D - 1, mc("flowering_azalea_leaves"), LEAVES)
    for z in range(GZ0, D):
        for x in (0, W - 1):
            fill(x, G + 1, z, x, G + 3, z, mc("flowering_azalea_leaves"), LEAVES)
    for side in (-1, 1):
        px = AX + side * 9
        fill(px - 1, G + 1, D - 3, px + 1, G + 10, D - 1, Q)
        pinnacle(px, D - 2, G + 11, 6)
    for x in range(AX - 8, AX + 9):
        s.set(x, G + 11, D - 2, GO if x % 2 else QB)
    # a Reisguh by the garden gate: the castle's waypoint
    s.entity(AX + 4.5, G + 1.0, D - 7.5, {"id": "guhs:guh_npc", "Kind": "reisguh", "ReisName": "Guhkasteel", "PersistenceRequired": Byte(1),
                                         "Rotation": floats(0.0, 0.0)})
    # the anchor in the middle of the ground (becomes grass): the whole castle is placed around it
    s.set(128, G, 128, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:kasteel_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "minecraft:grass_block[snowy=false]", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.save("guh_kasteel")


def koning_nbt():
    return {"id": "guhs:guh", "Variant": "koning", "Sitting": Byte(1), "PersistenceRequired": Byte(1), "Personality": "brave",
            "ClothesHead": "koning_kroon", "ClothesBody": "koning_mantel", "ClothesNeck": "koning_ketting",
            "Rotation": floats(0.0, 0.0),
            "attributes": compounds([{"id": "minecraft:scale", "base": Double(1.72)}])}


def guard(s, x, y, z, yaw):
    """A guh guard in knight's armour: stands still at its post (until someone tames it)."""
    s.entity(x, y, z, {"id": "guhs:guh", "NoAI": Byte(1), "PersistenceRequired": Byte(1), "ClothesHead": "knight_helmet",
                       "ClothesBody": "knight_armour", "Rotation": floats(yaw, 0.0),
                       "attributes": compounds([{"id": "minecraft:scale", "base": Double(1.0)}])})


def round_tower(s, rng, cx, cz, r, y0, h, roof=True):
    """A round tower of quartz and pink bands, windows, a cone roof in stripes with a golden tip and a flag."""
    Q, PK, GO = mc("quartz_bricks"), mc("pink_concrete"), mc("gold_block")
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.dist((x, z), (cx, cz))
            if d <= r + 0.5:
                for y in range(y0, y0 + h):
                    shell = d > r - 1.5
                    if shell:
                        window = (y - y0) % 7 in (3, 4) and int(math.degrees(math.atan2(z - cz, x - cx)) + 360) % 90 < 12
                        s.set(x, y, z, mc("pink_stained_glass") if window else (Q if (y - y0) % 6 else PK))
                    elif (y - y0) % 12 == 0:
                        s.set(x, y, z, mc("stripped_cherry_wood"), {"axis": "y"})
                    else:
                        s.set(x, y, z, mc("air"))
                s.set(x, y0 + h, z, mc("quartz_block"))
                if d > r - 0.5 and (x + z) % 2 == 0:
                    s.set(x, y0 + h + 1, z, mc("quartz_block"))
    for y in range(y0 + 1, y0 + h):                        # a ladder inside, up to the top
        s.set(cx, y, cz + r - 2, mc("ladder"), {"facing": "north", "waterlogged": "false"})
    for y in range(y0 + 12, y0 + h, 12):
        s.set(cx, y, cz + r - 2, mc("ladder"), {"facing": "north", "waterlogged": "false"})
    if roof:
        for k in range(r + 3):
            rr = r + 1.5 - k
            for x in range(cx - r - 2, cx + r + 3):
                for z in range(cz - r - 2, cz + r + 3):
                    d = math.dist((x, z), (cx, cz))
                    if rr - 1.2 < d <= rr:
                        ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                        s.set(x, y0 + h + 2 + k * 2, z, mc("magenta_concrete") if int(ang / 30) % 2 else mc("pink_concrete"))
                        s.set(x, y0 + h + 3 + k * 2, z, mc("magenta_concrete") if int(ang / 30) % 2 else mc("pink_concrete"))
        top = y0 + h + 2 + (r + 3) * 2
        s.set(cx, top, cz, GO)
        for y in range(top + 1, top + 5):
            s.set(cx, y, cz, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        for dx in range(1, 4):
            for y in (top + 3, top + 4):
                s.set(cx + dx, y, cz, mc("magenta_wool") if dx % 2 else mc("pink_wool"))


def blossom_tree(s, rng, x, y, z):
    h = rng.randint(5, 7)
    for k in range(h):
        s.set(x, y + k, z, "guhs:guhbloesem_log", {"axis": "y"})
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-1, 3):
                if dx * dx + dz * dz + (dy * 1.6) ** 2 <= 11 and rng.random() < 0.9 and (dx or dz or dy > 0):
                    s.set(x + dx, y + h + dy, z + dz, "guhs:guhbloesem_leaves", {"persistent": "true", "distance": "7", "waterlogged": "false"})


def fountain(s, cx, cz, G):
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.dist((x, z), (cx, cz))
            if d <= 5.5:
                s.set(x, G, z, mc("quartz_block"))
                if d > 4.5:
                    s.set(x, G + 1, z, mc("quartz_block"))
                else:
                    s.set(x, G + 1, z, "guhs:kaas_saus", {"level": "0"})
    for y in range(G + 1, G + 5):
        s.set(cx, y, cz, mc("quartz_pillar"), {"axis": "y"})
    s.set(cx, G + 5, cz, mc("gold_block"))
    s.set(cx, G + 6, cz, "guhs:kaas_saus", {"level": "0"})


def pond(s, rng, cx, cz, G):
    for x in range(cx - 14, cx + 15):
        for z in range(cz - 10, cz + 11):
            d = ((x - cx) / 14) ** 2 + ((z - cz) / 10) ** 2
            if d <= 1:
                depth = 1 if d > 0.6 else 2
                s.set(x, G - depth, z, mc("sand"))
                for y in range(G - depth + 1, G + 1):
                    s.set(x, y, z, mc("water"), {"level": "0"})
                if d < 0.8 and rng.random() < 0.06:
                    s.set(x, G + 1, z, "guhs:guh_waterlelie")
            elif d <= 1.25:
                s.set(x, G, z, mc("pink_concrete_powder"))
    for _ in range(5):
        s.entity(cx + rng.uniform(-8, 8), G - 0.5, cz + rng.uniform(-5, 5), {"id": "guhs:guh_vis", "PersistenceRequired": Byte(1)})


def hedge_maze(s, rng, x0, z0, cells, G):
    """A hedge maze (3 high), cells x cells, each cell 3x3 with 1-thick hedges; a fountain and a chest in the middle."""
    size = cells * 4 + 1
    leaves = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    for x in range(size):
        for z in range(size):
            for y in range(G + 1, G + 4):
                s.set(x0 + x, y, z0 + z, mc("azalea_leaves"), leaves)
            s.set(x0 + x, G, z0 + z, mc("grass_block"), {"snowy": "false"})
    seen = {(0, 0)}
    stack = [(0, 0)]

    def carve(cx, cz):
        for x in range(cx * 4 + 1, cx * 4 + 4):
            for z in range(cz * 4 + 1, cz * 4 + 4):
                for y in range(G + 1, G + 4):
                    s.set(x0 + x, y, z0 + z, mc("air"))
    carve(0, 0)
    while stack:
        cx, cz = stack[-1]
        options = [(cx + dx, cz + dz, dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                   if 0 <= cx + dx < cells and 0 <= cz + dz < cells and (cx + dx, cz + dz) not in seen]
        if not options:
            stack.pop()
            continue
        nx, nz, dx, dz = rng.choice(options)
        carve(nx, nz)
        wx, wz = cx * 4 + 2 + dx * 2, cz * 4 + 2 + dz * 2   # break the hedge between
        for a in (-1, 0, 1):
            for y in range(G + 1, G + 4):
                s.set(x0 + wx + (a if dz else 0), y, z0 + wz + (a if dx else 0), mc("air"))
        seen.add((nx, nz))
        stack.append((nx, nz))
    for (ex, ez) in ((size - 1, size // 2 - 1), (size - 1, size // 2), (size - 1, size // 2 + 1)):   # entrance on the east side
        for y in range(G + 1, G + 4):
            s.set(x0 + ex, y, z0 + ez, mc("air"))
    for y in range(G + 1, G + 4):
        s.set(x0 + size - 1, y, z0 + cells // 2 * 4 + 2, mc("air"))
        s.set(x0 + size - 2, y, z0 + cells // 2 * 4 + 2, mc("air"))
    mid = cells // 2 * 4 + 2
    s.set(x0 + mid, G + 1, z0 + mid, mc("chest"), {"facing": "south", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/guh_kasteel"})


# =====================================================================================================================
# the Guhmension super compass: one compass for (almost) every guh structure; the old single-purpose ones retire
# =====================================================================================================================
SUPER_STRUCTURES = {  # structure: (name, what's there)
    "guh_caves": ("Guhgrotten", "Hamsterbuizen diep onder de grond, met een grote centrale kamer"),
    "challenging_guh_caves": ("Uitdagende guhgrotten", "Zwart-rode buizen, Mika-spawners en Grote Mika met twee schatkisten"),
    "evil_mika_home": ("Huis van Boze Mika", "Het donkere huis van de Mika's, met Mika's vet in de kist"),
    "vadsig_heiligdom": ("Vadsig-heiligdom", "Hier begint de quest van Moeder Vadsig (je eigen guhmaag)"),
    "mika_kamp": ("Mika-kamp", "Quest: bevrijd Guhbert met steen-papier-schaar-VADS"),
    "guh_picnic": ("Guh-picknick", "Quest: de verloren guhtaart; ook de Hongerige Guh (bankguh)"),
    "sleehut": ("Sleehut", "Quest: de kapotte slee van de Slee-guh (Guhpieken)"),
    "verstopguh_huis": ("Verstopguhhuis", "Minigame: verstopguh met Verstopguhtje, tickets en een detectivepakje"),
    "guh_kermis": ("Guhkermis", "Minigame: de achtbaan, kermisbonnen en het kermispakje"),
    "guh_kasteel": ("Guhkasteel", "Legendarisch: de Koningguh, de poortwachters en een schatkamer (heel ver!)"),
    "hamster_house_extra_extra_large": ("Guhland", "Het zeer zeldzame guh-pretpark met reuzenrad en sleebaan"),
    "guhramid": ("Guhramide", "Zeldzame roze piramide met een Gouden Guh in een geheime kamer"),
    "guh_statue": ("Guhstandbeeld", "Een reuzenguh van blokken"),
    "grand_cheese_fountain": ("Grote kaasfontein", "Drie schalen kaassaus en een vadsbol"),
    "cheese_fountain": ("Kaasfontein", "Een fontein van kaassaus"),
    "guh_village": ("Guhdorp", "Guhdorpelingen met guhberoepen: handelen!"),
    "hamster_house": ("Hamsterhuis", "Klein hamsterhuis met een guh-spawner"),
    "hamster_house_medium": ("Groot hamsterhuis", "Hamsterhuis met een grote guh"),
    "hamster_house_large": ("Hamsterstad", "Een hele hamsterspeelstad met een megaguh"),
}
SUPER_CATEGORIES = {  # (same order as SuperkompasItem.CATEGORIES)
    "avontuur": ("Avontuur", "Grotten en Mika's: spannend!"),
    "quests": ("Quests", "Plekken waar een guh-opdracht begint"),
    "minigames": ("Minigames", "Spelletjes met tickets en bonnen"),
    "wonderen": ("Wonderen", "Grote en zeldzame bezienswaardigheden"),
    "wonen": ("Wonen", "Waar de guhs wonen"),
}
OLD_COMPASSES = ("guh_cave_compass", "challenge_compass", "kermiskompas", "verstopkompas", "koningskompas")


def superkompas():
    overrides = []
    for i in range(32):
        src = Image.open(os.path.join(TEX, "item", f"guh_cave_compass_{i:02d}.png"))
        frame = f"guhmensie_superkompas_{i:02d}"
        img = recolour(src, hue=0.47, sat=1.2, val=1.05, only=pinkish).convert("RGBA")
        px = img.load()
        for (x, y) in ((7, 1), (8, 1), (1, 7), (1, 8), (14, 7), (14, 8), (7, 14), (8, 14)):   # golden studs on the case
            if px[x, y][3]:
                px[x, y] = (250, 205, 60, 255)
        save(img, "item", f"{frame}.png")
        item_model(frame)
        overrides.append({"predicate": {"angle": (i - 0.5) / 32 if i else 0.0}, "model": f"guhs:item/{frame}"})
    overrides.append({"predicate": {"angle": 0.984375}, "model": "guhs:item/guhmensie_superkompas_00"})
    w(f"{A}/models/item/guhmensie_superkompas.json", {"parent": "minecraft:item/generated",
                                                       "textures": {"layer0": "guhs:item/guhmensie_superkompas_16"}, "overrides": overrides})
    shaped("guhmensie_superkompas", [" K ", "KCK", " V "], {"K": "guhs:guh_kristal", "C": "minecraft:compass", "V": "guhs:vahoege_vads_ingot"},
           "guhs:guhmensie_superkompas")
    # the old compasses: out of every chest, and the advancements show the super compass
    for folder in (f"{D}/loot_table", f"{D}/advancement"):
        for root, _dirs, files in os.walk(folder):
            for f in files:
                path = os.path.join(root, f)
                text = open(path, encoding="utf-8").read()
                new = text
                for old in OLD_COMPASSES:
                    new = new.replace(f'"guhs:{old}"', '"guhs:guhmensie_superkompas"')
                if new != text:
                    open(path, "w", encoding="utf-8").write(new)
    for key, text in {
        "item.guhs.guhmensie_superkompas": "Guhmensie-superkompas",
        "item.guhs.guhmensie_superkompas.named": "Superkompas: %s",
        "item.guhs.guhmensie_superkompas.lore": "Rechtsklik: kies wat je zoekt. Hij wijst naar de dichtstbijzijnde (in deze dimensie).",
        "item.guhs.guhmensie_superkompas.choose": "Rechtsklik om te kiezen wat je zoekt",
        "item.guhs.guhmensie_superkompas.chosen": "Het superkompas zoekt nu: %s",
        "item.guhs.guh_compass.old": "Oud kompas: vervangen door het Guhmensie-superkompas",
        "gui.guhs.superkompas.pick": "Kies wat je wilt zoeken",
        "quest.guhs.reis.picked_up": "Je hebt Reisguh '%s' opgepakt. Zet hem ergens in de Guhmensie neer: daar is dan zijn nieuwe reispunt.",
        "quest.guhs.reis.wild_name": "Reisguh (%s, %s)",
    }.items():
        lang(key, text, text)
    for cid, (name, tip) in SUPER_CATEGORIES.items():
        lang(f"gui.guhs.superkompas.{cid}", name, name)
        lang(f"gui.guhs.superkompas.{cid}.tooltip", tip, tip)
    for sid, (name, tip) in SUPER_STRUCTURES.items():
        lang(f"structure.guhs.{sid}", name, name)
        lang(f"structure.guhs.{sid}.tooltip", tip, tip)



def fix_compass_needles():
    """Vanilla's compass shows frame 16 at angle 0 (the needle pointing ahead): the frames of every guh compass are
    matched up the same way (they used to start at frame 0, so the needles pointed exactly the wrong way)."""
    import re
    folder = f"{A}/models/item"
    for f in os.listdir(folder):
        path = os.path.join(folder, f)
        model = json.load(open(path, encoding="utf-8"))
        overrides = model.get("overrides", [])
        if not overrides or "angle" not in overrides[0].get("predicate", {}):
            continue
        name = f[:-5]
        for i, o in enumerate(overrides):
            frame = (min(i, 32) + 16) % 32
            o["model"] = f"guhs:item/{name}_{frame:02d}"
        w(path, model)


def shared_2_4():
    """What the 2.4 minigames share: one game at a time, a busy shop, fire near the guh buildings, loaned items."""
    lang("quest.guhs.minigame.busy", "Njeg, you're still busy with another game! Finish that one first, then you can play here. Vahoeg!",
         "Njeg, jij bent nog met een ander spelletje bezig! Maak dat eerst af, dan mag je hier meedoen. Vahoeg!")
    lang("quest.guhs.minigame.dropped", "Your pockets are full: %sx %s dropped right in front of you!",
         "Je zakken zitten vol: %sx %s ligt voor je neus op de grond!")
    lang("quest.guhs.shop.busy", "Just a moment, guh! I'm still helping someone else. You're next!",
         "Even geduld, guh! Ik help nog een andere klant. Zo ben jij aan de beurt!")
    lang("gui.guhs.protected.no_fire", "No fire or buckets next to a guh building, njeg!",
         "Geen vuur of emmers naast een guhgebouw, njeg!")
    # things a minigame only lends you: they stay out of backpacks, the Bank Guh, item frames and chests
    add_tag("guhs/tags/item/loaned", ["guhs:guhgolfclub", "guhs:guhvis_hengel", "guhs:smulschaal", "guhs:mika_mep_hamer", "guhs:leenhouweel"])


def muziek():
    """Guh music discs (the sounds: tools/import_sound.py; sounds.json: make_resources) and their jukebox songs."""
    discs = {  # id: (song name, length in seconds, label colour, comparator output)
        "ze_hangen": ("Guh - Ze hangen aan me veh", 25, (240, 110, 170), 7),
    }
    for sid, (title, seconds, label, comparator) in discs.items():
        w(f"{D}/jukebox_song/{sid}.json", {"sound_event": f"guhs:music_disc.{sid}", "description": {"translate": f"jukebox_song.guhs.{sid}"},
                                            "length_in_seconds": float(seconds), "comparator_output": comparator})
        lang(f"jukebox_song.guhs.{sid}", title, title)
        lang(f"item.guhs.music_disc_{sid}", "Guh Music Disc", "Guh-muziekplaat")
        # the record: black vinyl with grooves, a round label with a little guh face
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d <= 7.6:
                    px[x, y] = (30, 26, 34, 255) if int(d) % 2 else (48, 42, 54, 255)
                if d <= 3.6:
                    px[x, y] = label + (255,)
        for x, y in ((6, 6), (9, 6)):                                        # eyes
            px[x, y] = (20, 20, 30, 255)
        for x in (6, 7, 8, 9):                                               # a little smile
            px[x, 9 if x in (7, 8) else 8] = (120, 30, 70, 255)
        px[3, 4] = px[4, 3] = (110, 100, 120, 255)                           # shine on the vinyl
        save(img, "item", f"music_disc_{sid}.png")
        item_model(f"music_disc_{sid}")
    w(f"{R}/data/c/tags/item/music_discs.json", {"replace": False, "values": [f"guhs:music_disc_{sid}" for sid in discs]})


def run_features():
    """The 2.4 features (tools/features/*.py) make their own resources, with all of make_v2 at hand."""
    import types
    import features
    h = types.SimpleNamespace(**globals())
    for module in features.modules():
        module.build(h)


# =====================================================================================================================
# Room between the buildings (after everything else wrote its structures)
# =====================================================================================================================
def read_nbt(path):
    """A small reader for (gzipped) structure .nbt files: returns the root compound as dicts/lists/numbers."""
    import gzip
    import struct
    data = gzip.open(path).read()
    pos = 0

    def take(n):
        nonlocal pos
        pos += n
        return data[pos - n:pos]

    def string():
        (n,) = struct.unpack(">H", take(2))
        return take(n).decode("utf-8", "replace")

    def payload(t):
        if t == 1:
            return struct.unpack(">b", take(1))[0]
        if t == 2:
            return struct.unpack(">h", take(2))[0]
        if t == 3:
            return struct.unpack(">i", take(4))[0]
        if t == 4:
            return struct.unpack(">q", take(8))[0]
        if t == 5:
            return struct.unpack(">f", take(4))[0]
        if t == 6:
            return struct.unpack(">d", take(8))[0]
        if t == 7:
            (n,) = struct.unpack(">i", take(4))
            return take(n)
        if t == 8:
            return string()
        if t == 9:
            et = take(1)[0]
            (n,) = struct.unpack(">i", take(4))
            return [payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                tt = take(1)[0]
                if tt == 0:
                    return out
                name = string()
                out[name] = payload(tt)
        if t == 11:
            (n,) = struct.unpack(">i", take(4))
            return list(struct.unpack(f">{n}i", take(4 * n)))
        if t == 12:
            (n,) = struct.unpack(">i", take(4))
            return list(struct.unpack(f">{n}q", take(8 * n)))
        raise ValueError(f"nbt tag {t}")

    take(1)
    string()
    return payload(10)


# who goes first when two buildings would touch (default: the one that reaches furthest). The Knabbelkelders (the way
# to the Guheinde, only 4 per world) never give way; the cave tubes give way to every building, the smallest first
VOORRANG = {"knabbelkelder": 1000, "onderwater": 900, "guh_caves": 1, "challenging_guh_caves": 2, "gatenkaas_mijnschacht": 3}
VOORRANG["knuffeldal_stadje"] = 800   # 2.8: the one town of every Knuffeldal goes before the other buildings
VOORRANG["elfguhjestocht"] = 900   # 2.9: the one Elf-Guhjestocht of every Guhpolder goes before the other buildings

# 3.0 (Guhverhalen): generic post-pass hooks for new structure types, so feature modules never edit this file.
#   RUIMTE_HOOKS[type](structure_json, jigsaw_reach) -> keep_clear (the reach from the middle of the start chunk), or None
#   GROND_HOOKS[type](structure_json) -> the ground_level_delta of its start pool (1 + the template y of the ground), or None
# (features.verhaal_wereld registers the branches for guhs:regio_jigsaw)
RUIMTE_HOOKS = {}
GROND_HOOKS = {}


def bouwruimte():
    """Every guhs structure claims the room of its pieces (keep_clear = how far they reach from the middle of the start
    chunk, per axis; see BouwRuimte.java): plain jigsaw ones are wrapped in guhs:flat_jigsaw (check_radius 0: no
    flatness check) so the underground ones take part too. /guhs bouwcheck checks that the reach is right.
    All of them are placed late, just before the plants (see below)."""
    sizes = {}

    def template(loc):
        if loc not in sizes:
            ns, path = loc.split(":")
            nbt = read_nbt(f"{R}/data/{ns}/structure/{path}.nbt")
            palette = nbt["palette"] if "palette" in nbt else nbt["palettes"][0]
            jigsaws = []
            for b in nbt["blocks"]:
                if palette[b["state"]]["Name"] == "minecraft:jigsaw":
                    tag = b.get("nbt", {})
                    jigsaws.append((b["pos"], tag.get("name", ""), tag.get("pool", "minecraft:empty")))
            sizes[loc] = (nbt["size"], jigsaws)
        return sizes[loc]

    def jigsaw_reach(jig, pool_centre):
        """Reach of a jigsaw structure from the middle of its chunk (the start sits on the chunk's corner, or with
        pool_centre on the middle)."""
        corner = 0 if pool_centre else 8
        pool = json.load(open(f"{D}/worldgen/template_pool/{jig['start_pool'].split(':')[1]}.json", encoding="utf-8"))
        reach = 0
        for e in pool["elements"]:
            size, jigsaws = template(e["element"]["location"])
            if jig.get("size", 1) > 1 and any(p != "minecraft:empty" for _, _, p in jigsaws):
                return jig["max_distance_from_center"] + 8 + 16  # (a piece may stick out a little past the limit)
            name = jig.get("start_jigsaw_name")
            anchors = [p for p, n, _ in jigsaws if n == name] if name else [[0, 0, 0]]
            for a in anchors:
                for axis in (0, 2):
                    if name:
                        reach = max(reach, a[axis], size[axis] - 1 - a[axis])
                    else:
                        reach = max(reach, size[axis] - 1)
        return reach + corner + 1

    for file in sorted(os.listdir(f"{D}/worldgen/structure")):
        path = f"{D}/worldgen/structure/{file}"
        name = file[:-5]
        s = json.load(open(path, encoding="utf-8"))
        kind = s["type"]
        if kind == "minecraft:jigsaw":
            inner = dict(s)
            s = {"type": "guhs:flat_jigsaw", "biomes": inner["biomes"], "step": inner["step"],
                 "spawn_overrides": inner["spawn_overrides"], "terrain_adaptation": inner["terrain_adaptation"],
                 "check_radius": 0, "max_height_difference": 0, "jigsaw": inner}
            kind = s["type"]
        if kind in ("guhs:flat_jigsaw", "guhs:guheinde_eiland"):
            reach = jigsaw_reach(s["jigsaw"], False)
        elif kind == "guhs:barbecueput":
            reach = jigsaw_reach(s, True)
        elif kind == "guhs:guhbubbel":
            # (the dome sits on the peak of its sea, anywhere in its cell of cell_chunks x cell_chunks around the start chunk)
            reach = jigsaw_reach(s, True) + s["cell_chunks"] * 16
        elif kind == "guhs:knuffeldal_stadje":
            # (2.8) the town: the reach of its pieces from the anchor (tools/features/knuffeldal_stadje.py works it out and
            # writes it as keep_clear) plus a cell: the anchor sits on the peak of its dal, anywhere in its cell
            import features.knuffeldal_stadje as _stadje
            _overlap, town_reach = _stadje.check_stukken()
            reach = town_reach + 1 + s["cell_chunks"] * 16
        elif kind == "guhs:elfguhjestocht":
            # (2.9) the Elf-Guhjestocht: one per Guhpolder, on the peak of its noise like the Knuffeldal town: the reach of
            # its pieces from the anchor (tools/features/elftocht.py REACH, the elftocht slice fills in the real number)
            # plus a cell (the anchor sits anywhere in its cell)
            import features.elftocht as _elftocht
            reach = _elftocht.REACH + 1 + s["cell_chunks"] * 16
        elif kind == "guhs:burcht":
            span = s["tiles_x"] * s["tile_size"], s["tiles_z"] * s["tile_size"]
            ax, _, az = s["anchor"]
            reach = max(ax, span[0] - 1 - ax, az, span[1] - 1 - az) + 1
        elif kind in RUIMTE_HOOKS:
            reach = RUIMTE_HOOKS[kind](s, jigsaw_reach)   # (3.0: e.g. guhs:regio_jigsaw, features/verhaal_wereld.py)
            if reach is None:
                continue
        else:
            continue
        s["keep_clear"] = reach
        # placed after everything that changes the rock (ores, the Barbecuether's roosterijzer blobs and sauce springs):
        # those eat the rock the buildings are made of (pink wool, gatenkaas, houtskoolsteen...); only the plants
        # come after them (and they stay off the buildings: buiten_gebouwen())
        if s["step"] in ("surface_structures", "underground_structures", "underground_decoration"):
            s["step"] = "fluid_springs"
            if "jigsaw" in s:
                s["jigsaw"]["step"] = "fluid_springs"
        if name in VOORRANG:
            s["voorrang"] = VOORRANG[name]
        else:
            s.pop("voorrang", None)
        w(path, s)


def grond():
    """2.10 (DESIGN 9.1, "het omgekeerde trapje"): the land meets every sunk building at its real floor.
    Vanilla takes template y = 1 as the ground of a jigsaw piece: the jigsaw placement puts minY + 1 on the surface, and the
    Beardifier smooths the terrain around the piece towards minY + 1. Our surface buildings are sunk (start_height -G: the
    ground's top block at template y = G, foundations/ponds/roots below it), so the land around them was dug down to
    template y = 1, a ring that stepped down 3-2-1 into a moat. This post-pass (after bouwruimte(), so it sees the final
    JSON) gives the start pool of every sunk structure a guhs:grond_single_pool_element (GrondPoolElement.java) with
    ground_level_delta = the layer you walk in (G + 1 = 1 - start_height), and sets start_height to 0: the building lands on
    exactly the same spot as before, but the terrain now meets its floor with the normal smooth beard.
    The two custom starts without heightmap projection (the Knuffeldal town, the Elf-Guhjestocht) ask Grond.startY() for
    their start y; their delta = the template y of the ground at the anchor + 1 (the town: read from the plein's anchor;
    the Elf-Guhjestocht: features.elftocht.GROND_Y, default 4). Idempotent: already rewritten structures are left alone."""
    def rewrite_pool(pool_id, delta):
        path = f"{D}/worldgen/template_pool/{pool_id.split(':')[1]}.json"
        pool = json.load(open(path, encoding="utf-8"))
        changed = False
        for e in pool["elements"]:
            el = e["element"]
            grond_el = el["element_type"] == "guhs:grond_single_pool_element"
            if (el["element_type"] == "minecraft:single_pool_element" or grond_el) and el.get("ground_level_delta") != delta:
                new = {"element_type": "guhs:grond_single_pool_element"}
                new.update({k: v for k, v in el.items() if k != "element_type"})
                new["ground_level_delta"] = delta
                e["element"] = new
                changed = True
        if changed:
            w(path, pool)

    def anchor_y(pool_id, name):
        pool = json.load(open(f"{D}/worldgen/template_pool/{pool_id.split(':')[1]}.json", encoding="utf-8"))
        ys = set()
        for e in pool["elements"]:
            ns, path = e["element"]["location"].split(":")
            nbt = read_nbt(f"{R}/data/{ns}/structure/{path}.nbt")
            palette = nbt["palette"] if "palette" in nbt else nbt["palettes"][0]
            ys.update(b["pos"][1] for b in nbt["blocks"]
                      if palette[b["state"]]["Name"] == "minecraft:jigsaw" and b.get("nbt", {}).get("name") == name)
        assert len(ys) == 1, f"grond: anchor {name} of {pool_id}: {ys}"
        return ys.pop()

    done = []
    for file in sorted(os.listdir(f"{D}/worldgen/structure")):
        path = f"{D}/worldgen/structure/{file}"
        name = file[:-5]
        s = json.load(open(path, encoding="utf-8"))
        if s.get("terrain_adaptation", "none") == "none":
            continue   # (the outer structure's adaptation is what the Beardifier uses; none = no beard, nothing to fix)
        if s["type"] == "guhs:flat_jigsaw":
            jig = s["jigsaw"]
            height = jig.get("start_height", {})
            if not jig.get("project_start_to_heightmap") or "absolute" not in height or height["absolute"] >= 0:
                continue   # (not sunk, or already rewritten)
            delta = 1 - height["absolute"]
            rewrite_pool(jig["start_pool"], delta)
            jig["start_height"] = {"absolute": 0}
            w(path, s)
        elif s["type"] == "guhs:knuffeldal_stadje":
            delta = anchor_y(s["start_pool"], s["start_jigsaw_name"]) + 1
            rewrite_pool(s["start_pool"], delta)
        elif s["type"] == "guhs:elfguhjestocht":
            import features.elftocht as _elftocht
            delta = getattr(_elftocht, "GROND_Y", 4) + 1
            rewrite_pool(s["start_pool"], delta)
        elif s["type"] in GROND_HOOKS:
            delta = GROND_HOOKS[s["type"]](s)   # (3.0: e.g. guhs:regio_jigsaw, features/verhaal_wereld.py)
            if delta is None:
                continue
            rewrite_pool(s["start_pool"], delta)
        else:
            continue
        done.append(f"{name} {delta}")
    print(f"grond: {len(done)} sunk structures meet the land at their floor ({', '.join(done)})")


def buiten_gebouwen():
    """No plants, bushes or trees on the roofs (or inside) of buildings, and no ore blobs, pillars or springs in them: the
    decoration of our biomes (from the surface structures step on; the features of a neighbouring chunk can run after a
    building is placed) gets the placement filter guhs:buiten_gebouwen (BuitenGebouwenFilter.java), and so do the
    patches inside our random_patch features (their tries spread around the spot). (The holes, pools and crystal caves
    of the earlier steps keep away from buildings themselves.)"""
    filt = {"type": "guhs:buiten_gebouwen"}
    placed = set()
    for file in sorted(os.listdir(f"{D}/worldgen/biome")):
        biome = json.load(open(f"{D}/worldgen/biome/{file}", encoding="utf-8"))
        for step in biome["features"][4:]:
            placed.update(f for f in step if f.startswith("guhs:"))
    for name in sorted(placed):
        path = f"{D}/worldgen/placed_feature/{name[5:]}.json"
        pf = json.load(open(path, encoding="utf-8"))
        if filt not in pf["placement"]:
            pf["placement"].append(filt)
            w(path, pf)
        feature = pf["feature"]
        if isinstance(feature, str) and feature.startswith("guhs:"):
            cpath = f"{D}/worldgen/configured_feature/{feature[5:]}.json"
            cf = json.load(open(cpath, encoding="utf-8"))
            inner = cf.get("config", {}).get("feature") if cf["type"] in ("minecraft:random_patch", "minecraft:flower") else None
            if isinstance(inner, dict) and "placement" in inner and filt not in inner["placement"]:
                inner["placement"].append(filt)
                w(cpath, cf)


def knus_2_8():
    """2.8 (Knuffeldal): the Knabbelkelders (the way to the Guheinde) may also lie under the Knuffeldal."""
    add_tag("guhs/tags/worldgen/biome/has_structure/knabbelkelder", ["guhs:knuffeldal"])


if __name__ == "__main__":
    guhmaag()
    phase2_structures()
    sled()
    phase3_structures()
    guh_sea_and_crystals()
    bees_slimes_mikas()
    deco_and_food()
    fixes_2_0_1()
    guhbloesem()
    crystals_v2()
    stomach_story()
    kermis()
    verstop()
    kasteel()
    superkompas()
    fix_compass_needles()
    shared_2_4()
    muziek()
    run_features()
    knus_2_8()
    bouwruimte()
    grond()
    buiten_gebouwen()
    write_lang()
