"""
Het Kaasmoeras (slice 4 of Guhs 2.7.0): a misty, bubbling swamp biome on the surface of the Guhmension.

  - the biome guhs:kaasmoeras (multi_noise point temperature 0.15, humidity 0.55, erosion -1..-0.25): olive-yellow
    grass and leaves, murky cheese-green water, a hazy yellow-green sky and fog (thicker mist: KaasmoerasClient),
    floating firefly-dust, swamp music, rain; soggy modderig kaasgras on kaasmodder, swamp oaks, kaasriet, moerasgras
    and mushrooms, and lots of pools (KaasmoerasPoelFeature): water pools with lily pads, bubbling borrelplassen of
    borrelende kaassaus (it bounces you up!) and now and then a knabbelvlotje (a raft with a barrel of loot)
  - blocks: borrelende_kaassaus, modderig_kaasgras, kaasmodder, kaasriet (tall), moerasgras, motknabbel (3 colours)
  - items: moeraskaas (a brewing ingredient, from the Moerasheks-Mika), vadsverdrijvend_drankje (throwable)
  - creatures (GeckoLib models + animations made here): the kikkerguh (roze / mint / geel), the kaasmot and the
    Moerasheks-Mika; the guh variant Kaasmoerasguh (its texture is made here too: green-yellow with cheese spots)
  - the structure moerasheks_hut: a big paalhut on stilts over a pond, shaped like the head of the Moerasheks-Mika
    herself under a giant witch hat, with a porch, a loft, a jetty with a knabbelvlotje and a borrelplas
  - loot tables, recipes, tags, advancements, the Guhdex pages, FTB quests (rows y=74/75.5) and all texts (Dutch; en_us
    gets the same Dutch text)

build(h) makes everything (h = make_v2); check(hut) is the geometry self-check of the template (SystemExit on problems).
"""
import json
import math
import os
import random
from collections import deque

import numpy as np
from PIL import Image, ImageDraw

NAME = "moerasheks_hut"
BIOME = "kaasmoeras"
FTB_Y = 74

# --- colours -------------------------------------------------------------------------------------------------------------
CHEESE = (250, 200, 70)
CHEESE_LIGHT = (255, 232, 140)
CHEESE_DARK = (214, 150, 40)
OLIVE = (150, 166, 62)
OLIVE_DARK = (104, 120, 40)
MUD = (122, 98, 52)
MUD_DARK = (84, 64, 32)
KIKKER = {  # colour: (fur, belly, spots)
    "roze": ((240, 140, 180), (255, 214, 230), (212, 96, 148)),
    "mint": ((140, 214, 178), (222, 250, 236), (84, 168, 132)),
    "geel": ((246, 206, 84), (255, 242, 196), (214, 162, 46)),
}
MOTKNABBEL = {"roze": ((200, 90, 140), (255, 205, 225)), "mint": ((70, 160, 120), (205, 255, 225)), "geel": ((210, 150, 30), (255, 244, 170))}


def rng_img(seed):
    return random.Random(seed)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def noisy(base, var, rng):
    v = rng.randint(-var, var)
    return clamp((base[0] + v, base[1] + v, base[2] + v)) + (255,)


# =====================================================================================================================
# block textures
# =====================================================================================================================
def borrel_frames(n=16):
    """Bubbling cheese sauce: a slowly swirling golden surface with bubbles that grow and pop (n frames)."""
    rng = random.Random(41)
    bubbles = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(1.2, 2.6), rng.randrange(n)) for _ in range(7)]
    frames = []
    for f in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        t = f / n * 2 * math.pi
        for y in range(16):
            for x in range(16):
                swirl = math.sin((x + y) * 0.55 + t) * 0.5 + math.sin((x - y) * 0.4 - t * 2) * 0.5
                c = [CHEESE[i] + (CHEESE_LIGHT[i] - CHEESE[i]) * max(0, swirl) * 0.8 + (CHEESE_DARK[i] - CHEESE[i]) * max(0, -swirl) * 0.7
                     for i in range(3)]
                px[x, y] = clamp(c) + (255,)
        for (bx, by, r, start) in bubbles:
            age = (f - start) % n
            life = n // 2
            if age >= life:
                continue
            rr = r * (0.35 + 0.65 * age / life)
            popping = age == life - 1
            for y in range(16):
                for x in range(16):
                    dx = min(abs(x - bx), 16 - abs(x - bx))    # (wraps around: the texture tiles)
                    dy = min(abs(y - by), 16 - abs(y - by))
                    d = math.hypot(dx, dy)
                    if popping and rr - 1 <= d <= rr + 0.4:
                        px[x, y] = (255, 250, 215, 255)
                    elif not popping and d <= rr:
                        edge = d > rr - 0.9
                        shine = (x - bx) + (y - by) < -rr * 0.6
                        px[x, y] = (255, 246, 196, 255) if shine else (CHEESE_DARK + (255,) if edge else (252, 216, 100, 255))
        frames.append(img)
    sheet = Image.new("RGBA", (16, 16 * n))
    for i, fr in enumerate(frames):
        sheet.paste(fr, (0, 16 * i))
    return sheet


def kaasmodder_tex(h):
    img = h.ramp(h.vanilla("block/mud"), MUD_DARK, (170, 142, 78)).copy()
    rng = rng_img(42)
    px = img.load()
    for _ in range(7):                                         # crumbs of cheese in the mud
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = CHEESE + (255,)
        if rng.random() < 0.5 and x < 15:
            px[x + 1, y] = CHEESE_DARK + (255,)
    return img


def kaasgras_top():
    rng = rng_img(43)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = OLIVE if rng.random() > 0.18 else OLIVE_DARK
            if rng.random() < 0.06:
                c = (196, 190, 70)                            # yellowish blades
            px[x, y] = noisy(c, 10, rng)
    for (cx, cy) in ((4, 5), (11, 10), (12, 3)):               # soggy mud patches and cheese flecks
        for y in range(cy - 1, cy + 2):
            for x in range(cx - 1, cx + 2):
                if rng.random() < 0.7:
                    px[x % 16, y % 16] = noisy(MUD, 8, rng)
    for _ in range(5):
        px[rng.randrange(16), rng.randrange(16)] = CHEESE_LIGHT + (255,)
    return img


def kaasgras_side(modder):
    rng = rng_img(44)
    img = modder.copy()
    px = img.load()
    for x in range(16):
        depth = 3 + (1 if rng.random() < 0.5 else 0) + (1 if rng.random() < 0.25 else 0)
        for y in range(depth):
            c = OLIVE if y < depth - 1 else OLIVE_DARK
            px[x, y] = noisy(c, 10, rng)
    px[3, 4] = CHEESE + (255,)
    px[12, 5] = CHEESE + (255,)
    return img


def kaasriet_tex():
    """Two textures (bottom and top) of a clump of reeds with cheese-cob cattails."""
    rng = rng_img(45)
    bottom, top = Image.new("RGBA", (16, 16), (0, 0, 0, 0)), Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    stems = [(2, 0.2), (5, -0.15), (8, 0.1), (11, -0.2), (14, 0.15)]
    heights = [26, 30, 22, 28, 24]                             # (out of 32: the whole plant is two blocks)
    for (x0, lean), hgt in zip(stems, heights):
        for yy in range(32 - hgt, 32):
            x = int(round(x0 + lean * (32 - yy) / 4))
            if not 0 <= x < 16:
                continue
            img, y = (top, yy) if yy < 16 else (bottom, yy - 16)
            shade = OLIVE_DARK if (yy + x) % 5 == 0 else (128, 150, 52)
            img.putpixel((x, y), noisy(shade, 8, rng))
        # the cheese cob: a fat yellow-brown sausage near the top
        tip = 32 - hgt
        for yy in range(tip + 2, tip + 7):
            x = int(round(x0 + lean * (32 - yy) / 4))
            for dx in (0, 1) if x < 15 else (0,):
                img, y = (top, yy) if yy < 16 else (bottom, yy - 16)
                c = CHEESE_DARK if dx else CHEESE
                if yy == tip + 2:
                    c = (200, 130, 40)
                img.putpixel((x + dx, y), c + (255,))
    # a few leaves at the bottom
    for x in range(0, 16, 3):
        for y in range(10, 16):
            if rng.random() < 0.35:
                bottom.putpixel((x, y), noisy(OLIVE, 10, rng))
    return bottom, top


def moerasgras_tex():
    rng = rng_img(46)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x0 in range(1, 16, 2):
        hgt = rng.randint(6, 12)
        lean = rng.uniform(-0.4, 0.4)
        for i in range(hgt):
            x = int(round(x0 + lean * i / 3))
            if 0 <= x < 16:
                img.putpixel((x, 15 - i), noisy(OLIVE if i < hgt - 2 else (176, 180, 72), 10, rng))
        if rng.random() < 0.4:                              # little cheese seeds
            x = int(round(x0 + lean * hgt / 3))
            if 0 <= x < 16:
                img.putpixel((x, 15 - hgt), CHEESE + (255,))
    return img


def motknabbel_tex(h, kleur):
    dark, light = MOTKNABBEL[kleur]
    top = h.ramp(h.vanilla("block/ochre_froglight_top"), dark, light).copy()
    side = h.ramp(h.vanilla("block/ochre_froglight_side"), dark, light).copy()
    # little dark specks: bits of kaasmot wing
    for img, spots in ((top, ((5, 6), (10, 9))), (side, ((4, 4), (11, 11)))):
        for (x, y) in spots:
            img.putpixel((x, y), clamp(tuple(c * 0.55 for c in dark)) + (255,))
    return top, side


# =====================================================================================================================
# item textures
# =====================================================================================================================
ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": (52, 44, 20, 255), "y": (214, 206, 92, 255), "Y": (238, 232, 140, 255), "d": (160, 150, 50, 255),
    "g": (110, 150, 50, 255), "G": (150, 190, 70, 255), "w": (255, 255, 255, 255), "s": (190, 210, 220, 255), "S": (140, 160, 175, 255),
    "l": (170, 196, 70, 255), "L": (206, 222, 110, 255), "c": (120, 84, 50, 255), "p": (240, 140, 180, 255), "r": (200, 40, 50, 255),
    "v": (120, 60, 140, 255), "o": (230, 160, 40, 255),
}
ICONS = {
    # a runny wedge of greenish swamp cheese with holes and mouldy spots (and a little stink cloud)
    "moeraskaas": ["..........g.....", ".........g.g....", "..........g.....", "..........kk....", "........kkYYk...",
                   "......kkYYYYYk..", "....kkYYgYYYYyk.", "..kkYyyYYYYgyyk.", ".kyyyydyyyyyyyk.", ".kyyydddyygyydyk",
                   ".kyygydyyyyddyk.", ".kyyyyyyyyyyyyk.", ".kddyyyydyyyydk.", "..kkddgddddgkk..", "....kkkkkkkk....", "................"],
    # a round bottle of murky yellow-green brew with a cork and a little Mika face on the label
    "vadsverdrijvend_drankje": ["................", "......kkkk......", "......kcck......", ".......ss.......", "......kssk......",
                                ".....ksLLsk.....", "....ksLLLLlsk...", "...kslLLLlllsk..", "...kslpppplllk..", "...kslprrprlgk..",
                                "...ksllppplglk..", "...kslllgglllk..", "....ksggllggk...", ".....kkkkkkk....", "................", "................"],
}


def effect_icon():
    """The Onvahoeg effect: a sad, deflated kaasknabbel with a green stink cloud (18x18)."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((3, 6, 14, 15), fill=CHEESE + (255,), outline=(150, 100, 20, 255))
    d.ellipse((5, 9, 7, 11), fill=CHEESE_DARK + (255,))
    d.ellipse((10, 11, 12, 13), fill=CHEESE_DARK + (255,))
    for x in (6, 11):                                        # droopy eyes
        img.putpixel((x, 9), (40, 30, 20, 255))
    for x in range(7, 11):                                   # a frown
        img.putpixel((x, 13 if x in (7, 10) else 12), (120, 60, 20, 255))
    for (x, y) in ((4, 2), (5, 1), (6, 2), (9, 3), (10, 2), (11, 3), (13, 1), (14, 2)):
        img.putpixel((x, y), (150, 190, 70, 255))
    return img


# =====================================================================================================================
# models, blockstates, loot, tags, recipes
# =====================================================================================================================
def blocks(h):
    A, D = h.A, h.D
    sheet = borrel_frames()
    h.save(sheet, "block", "borrelende_kaassaus.png")
    with open(os.path.join(h.TEX, "block", "borrelende_kaassaus.png.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f)
    h.w(f"{A}/models/block/borrelende_kaassaus.json", {"parent": "minecraft:block/block", "textures": {
        "all": "guhs:block/borrelende_kaassaus", "particle": "guhs:block/borrelende_kaassaus"}, "elements": [
        {"from": [0, 0, 0], "to": [16, 12, 16], "faces": {
            "down": {"texture": "#all", "cullface": "down"}, "up": {"texture": "#all"},
            **{f: {"texture": "#all", "uv": [0, 4, 16, 16], "cullface": f} for f in ("north", "south", "west", "east")}}}]})
    h.w(f"{A}/blockstates/borrelende_kaassaus.json", {"variants": {"": {"model": "guhs:block/borrelende_kaassaus"}}})
    h.w(f"{A}/models/item/borrelende_kaassaus.json", {"parent": "guhs:block/borrelende_kaassaus"})

    modder = kaasmodder_tex(h)
    h.save(modder, "block", "kaasmodder.png")
    h.simple_block("kaasmodder")
    h.save(kaasgras_top(), "block", "modderig_kaasgras_top.png")
    h.save(kaasgras_side(modder), "block", "modderig_kaasgras_side.png")
    h.w(f"{A}/models/block/modderig_kaasgras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/modderig_kaasgras_top", "bottom": "guhs:block/kaasmodder", "side": "guhs:block/modderig_kaasgras_side"}})
    h.w(f"{A}/blockstates/modderig_kaasgras.json", {"variants": {"": [
        {"model": "guhs:block/modderig_kaasgras", **({"y": r} if r else {})} for r in (0, 90, 180, 270)]}})
    h.w(f"{A}/models/item/modderig_kaasgras.json", {"parent": "guhs:block/modderig_kaasgras"})

    bottom, top = kaasriet_tex()
    h.save(bottom, "block", "kaasriet_bottom.png")
    h.save(top, "block", "kaasriet_top.png")
    for half in ("bottom", "top"):
        h.w(f"{A}/models/block/kaasriet_{half}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                       "textures": {"cross": f"guhs:block/kaasriet_{half}"}})
    h.w(f"{A}/blockstates/kaasriet.json", {"variants": {"half=lower": {"model": "guhs:block/kaasriet_bottom"},
                                                         "half=upper": {"model": "guhs:block/kaasriet_top"}}})
    h.item_model("kaasriet", "guhs:block/kaasriet_top")
    h.save(moerasgras_tex(), "block", "moerasgras.png")
    h.w(f"{A}/models/block/moerasgras.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                              "textures": {"cross": "guhs:block/moerasgras"}})
    h.w(f"{A}/blockstates/moerasgras.json", {"variants": {"": {"model": "guhs:block/moerasgras"}}})
    h.item_model("moerasgras", "guhs:block/moerasgras")

    for kleur in MOTKNABBEL:
        t, s = motknabbel_tex(h, kleur)
        h.save(t, "block", f"motknabbel_{kleur}_top.png")
        h.save(s, "block", f"motknabbel_{kleur}_side.png")
        h.w(f"{A}/models/block/motknabbel_{kleur}.json", {"parent": "minecraft:block/cube_column", "textures": {
            "end": f"guhs:block/motknabbel_{kleur}_top", "side": f"guhs:block/motknabbel_{kleur}_side"}})
    h.w(f"{A}/blockstates/motknabbel.json", {"variants": {f"kleur={k}": {"model": f"guhs:block/motknabbel_{k}"} for k in MOTKNABBEL}})
    h.w(f"{A}/models/item/motknabbel.json", {"parent": "guhs:block/motknabbel_roze", "overrides": [
        {"predicate": {"guhs:kleur": 0.5}, "model": "guhs:block/motknabbel_mint"},
        {"predicate": {"guhs:kleur": 1.0}, "model": "guhs:block/motknabbel_geel"}]})

    # --- loot ---
    h.self_drop("borrelende_kaassaus")
    h.self_drop("kaasmodder")
    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    h.w(f"{D}/loot_table/blocks/modderig_kaasgras.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:modderig_kaasgras", "conditions": [silk]},
            {"type": "minecraft:item", "name": "guhs:kaasmodder", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]})
    h.w(f"{D}/loot_table/blocks/kaasriet.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:kaasriet", "conditions": [
            {"condition": "minecraft:block_state_property", "block": "guhs:kaasriet", "properties": {"half": "lower"}}]}]}]})
    h.w(f"{D}/loot_table/blocks/moerasgras.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:moerasgras", "conditions": [{"condition": "minecraft:any_of", "terms": [silk, shears]}]},
            {"type": "minecraft:item", "name": "guhs:kaasknabbelzaadjes", "conditions": [
                {"condition": "minecraft:random_chance", "chance": 0.125}]}]}]}]})
    h.w(f"{D}/loot_table/blocks/motknabbel.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:motknabbel", "functions": [
            {"function": "minecraft:copy_state", "block": "guhs:motknabbel", "properties": ["kleur"]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    # --- tags ---
    h.add_tag("minecraft/tags/block/dirt", ["guhs:kaasmodder", "guhs:modderig_kaasgras"])
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:kaasmodder", "guhs:modderig_kaasgras", "guhs:borrelende_kaassaus"])
    h.add_tag("minecraft/tags/block/replaceable_by_trees", ["guhs:kaasriet", "guhs:moerasgras"])
    h.add_tag("minecraft/tags/block/sword_efficient", ["guhs:kaasriet", "guhs:moerasgras"])
    h.add_tag("minecraft/tags/block/enderman_holdable", ["guhs:kaasmodder", "guhs:modderig_kaasgras"])
    h.add_tag("guhs/tags/block/kaasmot_lokkers", ["guhs:block_of_kaasknabbels", "guhs:borrelende_kaassaus", "guhs:motknabbel",
                                                  "guhs:lampion_geel", "guhs:lampion_roze", "guhs:lampion_mint", "guhs:kaasknabbelplant",
                                                  "minecraft:lantern", "minecraft:ochre_froglight", "minecraft:pearlescent_froglight",
                                                  "minecraft:verdant_froglight", "minecraft:shroomlight"])

    # --- recipes ---
    h.shapeless("borrelende_kaassaus", ["guhs:moeraskaas", "guhs:guh_slimeball", "guhs:kaas_knabbels", "minecraft:mud"],
                "guhs:borrelende_kaassaus", 2)
    h.shapeless("vadsverdrijvend_drankje", ["minecraft:glass_bottle", "guhs:moeraskaas", "minecraft:fermented_spider_eye"],
                "guhs:vadsverdrijvend_drankje", 2)
    h.shapeless("kaasmodder", ["minecraft:mud", "guhs:kaas_knabbels"], "guhs:kaasmodder", 1)
    h.shapeless("kaasknabbels_uit_moeraskaas", ["guhs:moeraskaas"], "guhs:kaas_knabbels", 3)


def items(h):
    A = h.A
    for name, rows in ICONS.items():
        h.save(h.grid(rows, ITEM_PAL), "item", f"{name}.png")
        h.item_model(name)
    for egg in ("kikkerguh_spawn_egg", "kaasmot_spawn_egg", "moerasheks_mika_spawn_egg"):
        h.w(f"{A}/models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})
    img = effect_icon()
    full = os.path.join(h.TEX, "mob_effect", "onvahoeg.png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def loot(h):
    D = h.D
    cnt = h.count_fn

    def motknabbel(kleur, weight):
        return {"type": "minecraft:item", "name": "guhs:motknabbel", "weight": weight, "functions": cnt(1, 3) + [
            {"function": "minecraft:set_components", "components": {"minecraft:block_state": {"kleur": kleur}}}]}
    h.w(f"{D}/loot_table/chests/knabbelvlotje.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 10, "functions": cnt(4, 12)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": cnt(2, 5)},
            {"type": "minecraft:item", "name": "guhs:moeraskaas", "weight": 3, "functions": cnt(1, 2)},
            {"type": "minecraft:item", "name": "guhs:guh_vis", "weight": 5, "functions": cnt(1, 4)},
            {"type": "minecraft:item", "name": "guhs:guh_waterlelie", "weight": 4, "functions": cnt(1, 3)},
            {"type": "minecraft:item", "name": "guhs:kaasriet", "weight": 4, "functions": cnt(1, 4)},
            {"type": "minecraft:item", "name": "minecraft:fishing_rod", "weight": 2},
            {"type": "minecraft:item", "name": "minecraft:string", "weight": 3, "functions": cnt(2, 6)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 2, "functions": cnt(1, 4)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 1},
            motknabbel("roze", 1), motknabbel("mint", 1), motknabbel("geel", 1)]}]})
    h.w(f"{D}/loot_table/chests/moerasheks_hut.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:moeraskaas", "functions": cnt(2, 5)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:vadsverdrijvend_drankje", "functions": cnt(2, 4)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 8, "functions": cnt(8, 20)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": cnt(3, 8)},
            {"type": "minecraft:item", "name": "guhs:kaashoning", "weight": 2, "functions": cnt(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:glowstone_dust", "weight": 3, "functions": cnt(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:redstone", "weight": 3, "functions": cnt(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:spider_eye", "weight": 3, "functions": cnt(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:sugar", "weight": 3, "functions": cnt(2, 5)},
            {"type": "minecraft:item", "name": "minecraft:glass_bottle", "weight": 3, "functions": cnt(2, 5)},
            {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 2, "functions": cnt(1, 4)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 3, "functions": cnt(2, 6)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 1, "functions": cnt(1, 2)},
            motknabbel("roze", 1), motknabbel("mint", 1), motknabbel("geel", 1)]}]})
    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"min": 0, "max": 1}}
    h.w(f"{D}/loot_table/entities/moerasheks_mika.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:moeraskaas", "functions": cnt(1, 2) + [looting]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": cnt(2, 5)}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}],
         "entries": [{"type": "minecraft:item", "name": "guhs:vadsverdrijvend_drankje"}]}]})
    h.w(f"{D}/loot_table/entities/kaasmot.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.5}],
         "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels"}]}]})


# =====================================================================================================================
# worldgen: the biome, its pools and plants, where it goes in the Guhmension, its ground
# =====================================================================================================================
SKY, FOG = 0xCBD89C, 0xD9DDA6                 # hazy yellow-green
WATER, WATER_FOG = 0x9AA846, 0x6B7A2C         # murky cheese-green water
GRASS, FOLIAGE = 0xA3B04A, 0x8FA23C
BIOME_POINT = {"temperature": 0.15, "humidity": 0.55, "continentalness": [-1.0, 1.0], "erosion": [-1.0, -0.25],
               "weirdness": 0.0, "depth": [-1.0, 1.0], "offset": 0.0}


def random_patch(block, tries, spread, props=None):
    state = {"Name": block, **({"Properties": props} if props else {})}
    preds = [{"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}, {"type": "minecraft:would_survive", "state": state}]
    return {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": 2, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]}}}


def placed(h, name, placement):
    h.w(f"{h.D}/worldgen/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": placement})


def worldgen(h):
    D = h.D
    surface = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    # the pools (lakes step): about three per chunk, some of them borrelplassen or with a knabbelvlotje
    h.w(f"{D}/worldgen/configured_feature/kaasmoeras_poel.json", {"type": "guhs:kaasmoeras_poel", "config": {}})
    placed(h, "kaasmoeras_poel", [{"type": "minecraft:count", "count": 3}] + surface)
    h.w(f"{D}/worldgen/configured_feature/kaasmoeras_kaasriet.json", random_patch("guhs:kaasriet", 28, 5, {"half": "lower"}))
    placed(h, "kaasmoeras_kaasriet", [{"type": "minecraft:count", "count": 3}] + surface)
    h.w(f"{D}/worldgen/configured_feature/kaasmoeras_moerasgras.json", random_patch("guhs:moerasgras", 40, 7))
    placed(h, "kaasmoeras_moerasgras", [{"type": "minecraft:count", "count": 5}] + surface)
    # a few borrelende kaassaus puddles sunk into the grass (little bounce pads)
    h.w(f"{D}/worldgen/configured_feature/kaasmoeras_borrelplekje.json", {"type": "minecraft:random_patch", "config": {
        "tries": 6, "xz_spread": 2, "y_spread": 0, "feature": {
            "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                                                                  "state": {"Name": "guhs:borrelende_kaassaus"}}}},
            "placement": [{"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1},
                          {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                              {"type": "minecraft:matching_blocks", "blocks": ["guhs:modderig_kaasgras", "guhs:kaasmodder"]},
                              {"type": "minecraft:matching_blocks", "offset": [0, 1, 0], "blocks": "minecraft:air"}]}}]}}})
    placed(h, "kaasmoeras_borrelplekje", [{"type": "minecraft:rarity_filter", "chance": 3}] + surface)

    ores = ["guhs:guhmension_kaasknabbel_veins", "guhs:vahoege_vads_ore"]
    biome = {
        "has_precipitation": True, "temperature": 0.8, "downfall": 0.9, "creature_spawn_probability": 0.2,
        "effects": {
            "sky_color": SKY, "fog_color": FOG, "water_color": WATER, "water_fog_color": WATER_FOG,
            "grass_color": GRASS, "foliage_color": FOLIAGE,
            "particle": {"options": {"type": "minecraft:dust", "color": [0.86, 0.94, 0.36], "scale": 0.7}, "probability": 0.0035},
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "music": {"sound": "minecraft:music.overworld.swamp", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {
            "monster": [{"type": "guhs:moerasheks_mika", "weight": 2, "minCount": 1, "maxCount": 1}],
            "creature": [{"type": "guhs:guh", "weight": 70, "minCount": 2, "maxCount": 4},
                         {"type": "guhs:kikkerguh", "weight": 45, "minCount": 2, "maxCount": 4}],
            "ambient": [{"type": "guhs:kaasmot", "weight": 30, "minCount": 2, "maxCount": 4}],
            "water_ambient": [{"type": "guhs:guh_vis", "weight": 6, "minCount": 1, "maxCount": 3}],
            "axolotls": [], "underground_water_creature": [], "water_creature": [], "misc": []},
        "spawn_costs": {}, "carvers": {"air": []},
        "features": [[], ["guhs:kaasmoeras_poel"], [], [], [], [], ores, [], [], [
            "minecraft:trees_swamp", "guhs:kaasmoeras_kaasriet", "guhs:kaasmoeras_moerasgras", "guhs:kaasmoeras_borrelplekje",
            "minecraft:patch_waterlily", "minecraft:brown_mushroom_swamp", "minecraft:red_mushroom_swamp"], []],
    }
    h.w(f"{D}/worldgen/biome/{BIOME}.json", biome)

    # --- where it goes: one more point in the Guhmension's multi_noise (additive, like make_v2's guh sea) ---
    def biome_source(d):
        entries = d["generator"]["biome_source"]["biomes"]
        if not any(e["biome"] == f"guhs:{BIOME}" for e in entries):
            entries.append({"biome": f"guhs:{BIOME}", "parameters": dict(BIOME_POINT)})
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    # --- the ground: soggy kaasgras (and patches of kaasmodder) on kaasmodder ---
    rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [f"guhs:{BIOME}"]},
            "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                            "secondary_depth_range": 0, "surface_type": "floor"},
                 "then_run": {"type": "minecraft:sequence", "sequence": [
                     {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "guhs:guhmension_patches",
                                                                 "min_threshold": 0.42, "max_threshold": 10.0},
                      "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:kaasmodder"}}},
                     {"type": "minecraft:block", "result_state": {"Name": "guhs:modderig_kaasgras"}}]}},
                {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 3, "add_surface_depth": False,
                                                            "secondary_depth_range": 0, "surface_type": "floor"},
                 "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:kaasmodder"}}}]}}

    def surface_rule(d):
        rules = d["surface_rule"]["sequence"]
        if rule not in rules:
            rules.append(rule)
    h.patch_json(f"{D}/worldgen/noise_settings/guhmension.json", surface_rule)


# =====================================================================================================================
# creatures: GeckoLib models, animations and textures
# =====================================================================================================================
def face(u, v, w_, h_):
    return {"uv": [u, v], "uv_size": [w_, h_]}


def sw(i, size=6):
    """A plain 8x8 swatch (index i in an 8-wide grid), sampled 6x6 so the noise shows but the edges don't bleed."""
    return face((i % 8) * 8 + 1, (i // 8) * 8 + 1, size, size)


def cube(origin, size, faces, pivot=None, rotation=None, inflate=None):
    c = {"origin": origin, "size": size, "uv": {f: faces.get(f, faces.get("*")) for f in ("north", "south", "east", "west", "up", "down")}}
    if rotation:
        c["pivot"], c["rotation"] = pivot, rotation
    if inflate:
        c["inflate"] = inflate
    return c


def bone(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def geo(identifier, tw, th, bones, width=1.5, height=1.5):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": tw, "texture_height": th,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def swatches(img, colours, rng, var=8):
    px = img.load()
    for i, c in colours.items():
        for y in range(8):
            for x in range(8):
                px[(i % 8) * 8 + x, (i // 8) * 8 + y] = noisy(c, var, rng) if len(c) == 3 else c


def paint(img, x0, y0, rows, pal):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x0 + x, y0 + y), pal[ch])


# --- the kikkerguh ------------------------------------------------------------------------------------------------------
# 2.8: "een guh die een kikker is". The head IS the guh's head (the blocky guh head with its two big round ears, the big
# glossy guh eyes, the tiny snoet and a blush), shrunk onto a frog: squat frog body with back spots, frog legs, throat
# pouch, tongue. The guh parts keep their own UVs on a copy of guh.png recoloured to the frog's colour; the frog parts
# sample swatches painted into free cells of that sheet (the Sheet of spiesburcht_modellen).
KIKKER_HEAD_SCALE = 0.6
KIKKER_HEAD_CENTRE = (0, 1, -7)          # (the bottom middle of the guh's head: it's scaled around this point)
KIKKER_HEAD_SHIFT = (0, 3, 4)            # then put on the frog's front


def kikker_sheet(h, kleur):
    """guh.png in the frog's colour (fur along a ramp around it, inner ears a bit darker; eyes, nose and mouth kept),
    with a blush on the cheeks."""
    from . import spiesburcht_modellen as sm
    fc, belly, spots = KIKKER[kleur]
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    # the guh's fur in the frog's colour, keeping its shading (relative to the plain fur; a bit stronger, so the inner
    # ears come out clearly darker); the eyes (their shine and blue rings), nose and mouth stay as they are
    a = np.asarray(base).astype(np.float32)
    rgb = a[..., :3] / 255
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    lum = rgb @ np.array((0.3, 0.59, 0.11))
    fur = (a[..., 3] > 0) & (lum > 0.45) & (sat < 0.45) & ~(a[..., :3].min(-1) > 235) & ~(a[..., 2] > a[..., 0] + 30)
    shade = np.clip((lum / np.median(lum[fur])) ** 1.8, 0.5, 1.2)[..., None]
    a[..., :3] = np.where(fur[..., None], np.clip(np.array(fc, np.float32) * shade, 0, 255), a[..., :3])
    # the snoet: the guh's little nose cube stays pink (every face of it)
    nose = [c for c in sm.load_geo(h, "guh")["head"]["cubes"] if c["size"] == [4, 1.5, 1]][0]
    for f in nose["uv"].values():
        (u, v), (w_, h_) = f["uv"], f["uv_size"]
        u0, u1 = sorted((u * 4, (u + w_) * 4))
        v0, v1 = sorted((v * 4, (v + h_) * 4))
        a[int(v0):int(math.ceil(v1)), int(u0):int(math.ceil(u1)), :3] = (236, 128, 166)
        a[int(v0), int(u0):int(math.ceil(u1)), :3] = (250, 170, 196)
    # the blush under the eyes (on the front of the head: the cube with uv (0, 49), 14x11 units)
    for cx in (12, 44):
        for y in range(49 * 4 + 25, 49 * 4 + 32):
            for x in range(cx - 7, cx + 7):
                e = ((x - cx) / 7.0) ** 2 + ((y - (49 * 4 + 28.5)) / 3.5) ** 2
                if e < 1:
                    a[y, x, :3] = a[y, x, :3] * (0.35 + 0.5 * e) + np.array((255, 108, 150), np.float32) * (0.65 - 0.5 * e)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def kikkerguh(h):
    from . import spiesburcht_modellen as sm
    A = h.A
    guh = sm.load_geo(h, "guh")
    head = sm.moved(guh["head"], "head", "body", scale=KIKKER_HEAD_SCALE, shift=KIKKER_HEAD_SHIFT, centre=KIKKER_HEAD_CENTRE)
    ears = [sm.moved(guh[e], e, "head", scale=KIKKER_HEAD_SCALE, shift=KIKKER_HEAD_SHIFT, centre=KIKKER_HEAD_CENTRE)
            for e in ("ear_left", "ear_right")]
    front = min(c["origin"][2] for c in head["cubes"])                          # the tip of the snoet
    face_z = min(c["origin"][2] for c in head["cubes"] if c["size"][0] > 4)      # the face itself
    bottom = min(c["origin"][1] for c in head["cubes"])
    nose_y = [c for c in head["cubes"] if c["size"][0] <= 4][0]["origin"][1]
    bones = None
    for kleur, (fc, belly, spots) in KIKKER.items():
        sheet = sm.Sheet(kikker_sheet(h, kleur), [head] + ears)
        rng = np.random.default_rng({"roze": 281, "mint": 282, "geel": 283}[kleur])
        light = tuple(min(255, int(c + (255 - c) * 0.35)) for c in fc)

        def fur(base=fc):
            def paint():
                p = sm.noise(base, 7, rng)
                sm.blobs(p, light, rng, n=6, rmin=1, rmax=3, alpha=0.5)           # soft guh pluisjes
                return p
            return paint

        def back():
            p = fur()()
            for (x, y, r) in ((8, 8, 4), (23, 6, 3), (15, 18, 4), (5, 25, 3), (26, 24, 4), (14, 30, 2)):   # frog spots
                yy, xx = np.ogrid[:32, :32]
                m = (xx - x) ** 2 + (yy - y) ** 2 <= r * r
                p[m, :3] = np.array(spots, np.float32)
            return p
        sheet.swatch("vacht", fur())
        sheet.swatch("rug", back)
        sheet.swatch("buik", lambda: sm.noise(belly, 5, rng))
        sheet.swatch("poot", fur(tuple(int(c * 0.9) for c in fc)))
        sheet.swatch("tong", lambda: sm.noise((236, 80, 110), 6, rng))
        sheet.swatch("keel", lambda: sm.blobs(sm.noise(belly, 5, rng), (255, 190, 214), rng, n=4, rmin=2, rmax=5, alpha=0.4))

        def c(swatch, origin, size, faces=None, **kw):
            cube = sm.cube(sheet, swatch, origin, size, **kw)
            for f, sw_ in (faces or {}).items():
                cube["uv"][f] = sheet.faces(sw_)[f]
            return cube
        body = bone("body", "root", [0, 3, 1], [c("vacht", [-4.5, 1, -3.5], [9, 5, 9], {"up": "rug", "down": "buik"}),
                                               c("buik", [-3.5, 0.5, -3], [7, 1, 7])])
        throat = bone("throat", "head", [0, bottom, face_z + 2], [c("keel", [-2.5, bottom - 1.2, face_z + 0.4], [5, 1.4, 3])])
        # (the tongue waits inside the mouth; the tongue animation stretches it out forwards from the face)
        tongue = bone("tongue", "head", [0, nose_y - 0.6, face_z + 0.6], [c("tong", [-0.7, nose_y - 0.9, face_z + 0.05], [1.4, 0.5, 0.55])])
        leg = "poot"
        bones = [
            bone("root", None, [0, 0, 0], []),
            body, head] + ears + [throat, tongue,
            bone("leg_front_left", "body", [3.5, 2, -2.5], [c(leg, [2.5, 0, -3.5], [2, 2.5, 2])]),
            bone("leg_front_right", "body", [-3.5, 2, -2.5], [c(leg, [-4.5, 0, -3.5], [2, 2.5, 2])]),
            bone("leg_back_left", "body", [4.5, 2, 3], [c("vacht", [3.5, 0, 1.5], [2.5, 2.5, 4.5], {"up": "rug"}),
                                                       c(leg, [4, 0, -0.5], [3, 0.5, 2])]),
            bone("leg_back_right", "body", [-4.5, 2, 3], [c("vacht", [-6, 0, 1.5], [2.5, 2.5, 4.5], {"up": "rug"}),
                                                         c(leg, [-7, 0, -0.5], [3, 0.5, 2])]),
            bone("tail", "body", [0, 3, 5.5], [c("vacht", [-0.75, 2.5, 5.5], [1.5, 1.5, 1.5])]),
        ]
        sheet.save(h, f"kikkerguh_{kleur}")
    h.w(f"{A}/geckolib/models/entity/kikkerguh.geo.json", geo("kikkerguh", 128, 128, bones, 1.2, 1.2))
    h.w(f"{A}/geckolib/animations/entity/kikkerguh.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.kikkerguh.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "throat": {"scale": {"0.0": [1, 1, 1], "0.75": [1.08, 1.3, 1.08], "1.5": [1, 1, 1], "2.25": [1.08, 1.3, 1.08], "3.0": [1, 1, 1]}},
            "body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.02, 0.98, 1.02], "3.0": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 4], "2.0": [0, 0, -4], "3.0": [0, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.5": [0, 0, 0], "2.65": [0, 0, -14], "2.8": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "2.5": [0, 0, 0], "2.65": [0, 0, 14], "2.8": [0, 0, 0]}}}},
        "animation.kikkerguh.hop": {"loop": True, "animation_length": 0.5, "bones": {
            "body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2.5, 0], "0.3": [0, 2, 0], "0.45": [0, 0, 0]},
                     "rotation": {"0.0": [0, 0, 0], "0.1": [-12, 0, 0], "0.3": [6, 0, 0], "0.45": [0, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, 16], "0.35": [0, 0, -6], "0.45": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, -16], "0.35": [0, 0, 6], "0.45": [0, 0, 0]}},
            "leg_back_left": {"rotation": {"0.0": [0, 0, 0], "0.1": [50, 0, 0], "0.3": [20, 0, 0], "0.45": [0, 0, 0]}},
            "leg_back_right": {"rotation": {"0.0": [0, 0, 0], "0.1": [50, 0, 0], "0.3": [20, 0, 0], "0.45": [0, 0, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [-35, 0, 0], "0.4": [0, 0, 0]}},
            "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [-35, 0, 0], "0.4": [0, 0, 0]}}}},
        "animation.kikkerguh.swim": {"loop": True, "animation_length": 0.8, "bones": {
            "body": {"rotation": {"0.0": [8, 0, 0]}},
            "leg_back_left": {"rotation": {"0.0": [60, -20, 0], "0.4": [0, 20, 0], "0.8": [60, -20, 0]}},
            "leg_back_right": {"rotation": {"0.0": [60, 20, 0], "0.4": [0, -20, 0], "0.8": [60, 20, 0]}},
            "leg_front_left": {"rotation": {"0.0": [-40, 0, 0], "0.4": [-10, 0, 0], "0.8": [-40, 0, 0]}},
            "leg_front_right": {"rotation": {"0.0": [-40, 0, 0], "0.4": [-10, 0, 0], "0.8": [-40, 0, 0]}}}},
        "animation.kikkerguh.croak": {"loop": False, "animation_length": 0.9, "bones": {
            "throat": {"scale": {"0.0": [1, 1, 1], "0.15": [1.5, 2.2, 1.6], "0.35": [1, 1, 1], "0.5": [1.5, 2.2, 1.6], "0.75": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-10, 0, 0], "0.6": [-8, 0, 0], "0.9": [0, 0, 0]}}}},
        "animation.kikkerguh.tongue": {"loop": False, "animation_length": 0.45, "bones": {
            "tongue": {"scale": {"0.0": [1, 1, 1], "0.1": [1.2, 1.2, 40], "0.2": [1.2, 1.2, 30], "0.35": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.08": [-12, 0, 0], "0.35": [0, 0, 0]}}}},
    }})
    kikkerguh_check(h, bones, front)


def kikkerguh_check(h, bones, front):
    """A guh that is a frog: the guh head with both round ears and the snoet in front, the glossy guh eyes (dark pupil,
    blue iris, white shine) on the face in every colour, a blush; and still a frog: 4 legs, throat, tongue, spots."""
    names = {b["name"] for b in bones}
    p = [f"kikkerguh: no {n}" for n in ("head", "ear_left", "ear_right", "throat", "tongue", "body", "tail", "leg_front_left",
                                        "leg_front_right", "leg_back_left", "leg_back_right") if n not in names]
    by = {b["name"]: b for b in bones}
    for b in bones:
        if b.get("parent") and b["parent"] not in names:
            p.append(f"kikkerguh: {b['name']} hangs on a missing {b['parent']}")
    body_front = min(c["origin"][2] for c in by["body"]["cubes"])
    if front > body_front - 1:
        p.append(f"kikkerguh: the guh head doesn't stick out in front ({front} vs {body_front})")
    top = max(c["origin"][1] + c["size"][1] for b in bones for c in b.get("cubes", []))
    if top > 16:
        p.append(f"kikkerguh: {top} units tall, too big for its hitbox")
    for kleur in KIKKER:
        a = np.asarray(Image.open(os.path.join(h.TEX, "entity", f"kikkerguh_{kleur}.png")).convert("RGBA")).astype(np.int32)
        face = a[49 * 4:60 * 4, 0:14 * 4]
        blue = int(((face[..., 2] > face[..., 0] + 60) & (face[..., 3] > 0)).sum())
        white = int((face[..., :3].min(-1) > 240).sum())
        dark = int((face[..., :3].max(-1) < 60).sum())
        if blue < 100 or white < 30 or dark < 150:
            p.append(f"kikkerguh_{kleur}: no glossy guh eyes on the face (blue {blue}, shine {white}, pupil {dark})")
        redness = lambda px: int(px[0]) - int(px[1])
        if min(redness(face[28, 12]), redness(face[28, 44])) < redness(face[42, 28]) + 15:
            p.append(f"kikkerguh_{kleur}: no blush on the cheeks")
    if p:
        raise SystemExit("; ".join(p))


# --- the kaasmot ------------------------------------------------------------------------------------------------------------
def kaasmot(h):
    A = h.A
    # swatches: 0 purple fuzz, 1 cheese stripes, 2 Mika pink, 3 dark red; custom: wings (with cheese holes), the face
    WING, HIND, FACE = (0, 16, 6, 5), (8, 16, 4, 3), (14, 16, 3, 3)
    wing = {"*": face(*WING)}
    hind = {"*": face(*HIND)}
    bones = [
        bone("body", None, [0, 4, 0], [cube([-1, 3, -1], [2, 2, 2.5], {"*": sw(0, 4)}), cube([-0.8, 2.7, 1.5], [1.6, 1.6, 2.8], {"*": sw(1, 4)})]),
        bone("head", "body", [0, 4, -1], [cube([-1.25, 3, -3], [2.5, 2.5, 2], {"*": sw(2, 4), "north": face(*FACE)}),
                                          cube([-1.1, 5.5, -2.4], [0.6, 0.8, 0.6], {"*": sw(3, 2)}),
                                          cube([0.5, 5.5, -2.4], [0.6, 0.8, 0.6], {"*": sw(3, 2)}),
                                          cube([-0.9, 5.3, -3.2], [0.3, 2, 0.3], {"*": sw(0, 2)}, [-0.75, 5.3, -3], [20, 0, 10]),
                                          cube([0.6, 5.3, -3.2], [0.3, 2, 0.3], {"*": sw(0, 2)}, [0.75, 5.3, -3], [20, 0, -10])]),
        bone("wing_left", "body", [1, 5, 0], [cube([1, 5, -1.5], [6, 0, 5], wing), cube([1, 4.95, 1.8], [4, 0, 3], hind)]),
        bone("wing_right", "body", [-1, 5, 0], [cube([-7, 5, -1.5], [6, 0, 5], wing), cube([-5, 4.95, 1.8], [4, 0, 3], hind)]),
        bone("tail", "body", [0, 3.5, 4], [cube([-0.25, 3.2, 4.2], [0.5, 0.5, 1.8], {"*": sw(3, 2)}),
                                           cube([-0.6, 3.1, 5.9], [1.2, 0.7, 0.8], {"*": sw(3, 2)})]),
    ]
    h.w(f"{A}/geckolib/models/entity/kaasmot.geo.json", geo("kaasmot", 32, 32, bones, 1.0, 0.8))
    rng = random.Random(47)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    swatches(img, {0: (86, 50, 92), 2: (236, 150, 186), 3: (170, 30, 50)}, rng)
    px = img.load()
    for y in range(8):                                         # the abdomen: cheese stripes
        for x in range(8):
            px[8 + x, y] = (CHEESE if (y // 2) % 2 else (96, 56, 96)) + (255,)
    # wings: cheese yellow, darker rim, and round holes (see-through: they're cheese wings!)
    for (x0, y0, w_, h_) in (WING, HIND):
        for y in range(h_):
            for x in range(w_):
                edge = x in (0, w_ - 1) or y in (0, h_ - 1)
                px[x0 + x, y0 + y] = (CHEESE_DARK if edge else CHEESE_LIGHT if (x + y) % 3 else CHEESE) + (255,)
    for (x, y) in ((2, 17), (4, 19), (1, 19), (10, 17)):
        px[x, y] = (0, 0, 0, 0)
    paint(img, *FACE[:2], ["prp", "ppp", "pkp"], {"p": (236, 150, 186, 255), "r": (230, 30, 40, 255), "k": (80, 20, 40, 255)})
    px[FACE[0], FACE[1]] = (230, 30, 40, 255)
    px[FACE[0] + 2, FACE[1]] = (230, 30, 40, 255)
    px[FACE[0] + 1, FACE[1]] = (236, 150, 186, 255)
    h.save(img, "entity", "kaasmot.png")
    h.w(f"{A}/geckolib/animations/entity/kaasmot.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.kaasmot.fly": {"loop": True, "animation_length": 0.24, "bones": {
            "wing_left": {"rotation": {"0.0": [0, 0, 20], "0.12": [0, 0, -55], "0.24": [0, 0, 20]}},
            "wing_right": {"rotation": {"0.0": [0, 0, -20], "0.12": [0, 0, 55], "0.24": [0, 0, -20]}},
            "body": {"position": {"0.0": [0, 0, 0], "0.12": [0, 0.4, 0], "0.24": [0, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, 10, 0], "0.12": [0, -10, 0], "0.24": [0, 10, 0]}}}}}})


# --- the Moerasheks-Mika ----------------------------------------------------------------------------------------------------
def moerasheks(h):
    A = h.A
    src = json.load(open(f"{A}/geckolib/models/entity/mika.geo.json", encoding="utf-8"))
    g = json.loads(json.dumps(src))
    geom = g["minecraft:geometry"][0]
    geom["description"]["identifier"] = "geometry.moerasheks_mika"
    geom["description"]["visible_bounds_height"] = 3
    # new materials: 8x8 (uv) swatches in the free bottom quarter of the (128 x 128 uv) Mika texture
    mats = {"hat": 0, "band": 1, "gold": 2, "nose": 3, "wart": 4, "shawl": 5, "star": 6, "brim": 7}

    def m(name):
        u, v = mats[name] * 8 + 4, 100
        return {f: {"uv": [u, v], "uv_size": [6, 6]} for f in ("north", "south", "east", "west", "up", "down")}
    geom["bones"] += [
        {"name": "hat", "parent": "head", "pivot": [0, 13.5, -7], "cubes": [
            {"origin": [-9, 13.4, -15.5], "size": [18, 0.8, 17], "uv": m("brim")},
            {"origin": [-5.3, 14.2, -12.3], "size": [10.6, 1.4, 10.6], "uv": m("band")},
            {"origin": [-5, 14.2, -12], "size": [10, 3, 10], "uv": m("hat")},
            {"origin": [-3.8, 17.2, -10.8], "size": [7.6, 3, 7.6], "uv": m("hat")},
            {"origin": [-2.7, 20.2, -9.7], "size": [5.4, 2.6, 5.4], "uv": m("hat")},
            {"origin": [-1.2, 14.0, -12.6], "size": [2.4, 1.8, 0.4], "uv": m("gold")}]},
        {"name": "hat_tip", "parent": "hat", "pivot": [0, 22.8, -7], "rotation": [0, 0, -22], "cubes": [
            {"origin": [-1.8, 22.8, -8.8], "size": [3.6, 2.4, 3.6], "uv": m("hat")}]},
        {"name": "hat_tip2", "parent": "hat_tip", "pivot": [0, 25.2, -7], "rotation": [0, 0, -35], "cubes": [
            {"origin": [-1.1, 25.2, -8.1], "size": [2.2, 2.2, 2.2], "uv": m("hat")},
            {"origin": [-0.6, 27.3, -7.6], "size": [1.2, 1.2, 1.2], "uv": m("star")}]},
        {"name": "nose", "parent": "head", "pivot": [0, 6, -12], "cubes": [
            {"origin": [-1, 4.4, -14.4], "size": [2, 2.8, 2.6], "uv": m("nose")},
            {"origin": [-0.7, 3.6, -15.2], "size": [1.4, 1.4, 1.2], "uv": m("nose")},
            {"origin": [0.6, 5.8, -14.8], "size": [0.9, 0.9, 0.9], "uv": m("wart")}]},
        {"name": "shawl", "parent": "body", "pivot": [0, 5, 5], "cubes": [
            {"origin": [-7.2, 0.6, -2.8], "size": [14.4, 9.8, 1.8], "uv": m("shawl"), "inflate": 0.1}]},
    ]
    h.w(f"{A}/geckolib/models/entity/moerasheks_mika.geo.json", g)

    tex = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    # the pink Mika fur turns swampy green (not the red eyes, not the dark lines)
    tex = h.recolour(tex, hue=0.26, sat=2.1, val=0.72, only=lambda hh, s, v: ((hh > 0.85) | (hh < 0.05)) & (s > 0.12) & (s < 0.55)).copy()
    rng = random.Random(48)
    px = tex.load()
    colours = {"hat": (70, 40, 100), "band": CHEESE, "gold": (250, 214, 70), "nose": (110, 150, 60), "wart": (120, 80, 50),
               "shawl": (90, 60, 120), "star": (255, 240, 130), "brim": (52, 30, 76)}
    for name, i in mats.items():
        u0, v0 = (i * 8 + 4) * 4, 100 * 4
        for y in range(6 * 4):
            for x in range(6 * 4):
                c = colours[name]
                if name == "hat" and (x // 4 + y // 4) % 5 == 0:
                    c = (86, 52, 120)                          # a stitched patch pattern
                if name == "shawl" and (y // 4) % 2 == 0 and (x // 4) % 2 == 0:
                    c = (120, 160, 70)                         # a green check on the shawl
                px[u0 + x, v0 + y] = noisy(c, 7, rng)
    # warts on the green cheeks (a few darker spots on the fur)
    tex.save(os.path.join(h.TEX, "entity", "moerasheks_mika.png"))

    anims = json.load(open(f"{A}/geckolib/animations/entity/guh.animation.json", encoding="utf-8"))["animations"]
    idle = json.loads(json.dumps(anims["animation.guh.idle"]))
    walk = json.loads(json.dumps(anims["animation.guh.walk"]))
    idle.setdefault("bones", {})["hat_tip"] = {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 6], "2.0": [0, 0, 0]}}
    idle["bones"]["hat_tip2"] = {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 10], "2.0": [0, 0, 0]}}
    if "animation_length" not in idle:
        idle["animation_length"] = 2.0
    walk.setdefault("bones", {})["hat_tip"] = {"rotation": {"0.0": [0, 0, -6], "0.25": [0, 0, 6], "0.5": [0, 0, -6]}}
    h.w(f"{A}/geckolib/animations/entity/moerasheks_mika.animation.json", {"format_version": "1.8.0", "animations": {
        "animation.moerasheks_mika.idle": idle,
        "animation.moerasheks_mika.walk": walk,
        "animation.moerasheks_mika.throw": {"loop": False, "animation_length": 0.6, "bones": {
            "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-15, 0, 0], "0.35": [12, 0, 0], "0.6": [0, 0, 0]}},
            "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [-80, 0, 0], "0.35": [-20, 0, 0], "0.6": [0, 0, 0]}},
            "hat_tip": {"rotation": {"0.0": [0, 0, 0], "0.35": [0, 0, 18], "0.6": [0, 0, 0]}}}},
        "animation.moerasheks_mika.nibble": {"loop": False, "animation_length": 1.0, "bones": {
            "head": {"rotation": {"0.0": [0, 0, 0], "0.2": [15, 0, 0], "0.35": [5, 0, 0], "0.5": [15, 0, 0], "0.65": [5, 0, 0],
                                  "0.8": [15, 0, 0], "1.0": [0, 0, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.2": [-50, 0, 0], "0.8": [-50, 0, 0], "1.0": [0, 0, 0]}}}},
    }})


# --- the Kaasmoerasguh: the guh texture in swamp colours with cheese spots -------------------------------------------------
GUH_FUR = (195, 160, 205)


def kaasmoerasguh(h):
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    a = np.asarray(base).astype(np.int32)
    diff = a[..., :3] - np.array(GUH_FUR)
    fur = (np.abs(diff).sum(-1) < 40) & (a[..., 3] > 0)
    colour = np.array((168, 186, 86))
    out = a.copy()
    out[..., :3] = np.where(fur[..., None], np.clip(colour + diff, 0, 255), a[..., :3])
    rng = np.random.default_rng(49)
    hgt, wid = fur.shape
    yy, xx = np.mgrid[0:hgt, 0:wid]
    for _ in range(170):                                       # green-yellow spots: cheese yellow and dark swamp green
        y, x = rng.integers(0, hgt), rng.integers(0, wid)
        if not fur[y, x]:
            continue
        r = rng.uniform(3, 8)
        spot = ((yy - y) ** 2 + (xx - x) ** 2 <= r * r) & fur
        c = (238, 206, 84) if rng.random() < 0.55 else (104, 132, 46)
        shade = rng.integers(-10, 10)
        out[spot, 0], out[spot, 1], out[spot, 2] = c[0] + shade, c[1] + shade, c[2] + shade
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(os.path.join(h.TEX, "entity", "guh_kaasmoerasguh.png"))


def creatures(h):
    kikkerguh(h)
    kaasmot(h)
    moerasheks(h)
    kaasmoerasguh(h)


# =====================================================================================================================
# advancements (the Guhmension tab) and the hidden quest advancements
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["kaasmoeras_stuiter", "kaasmoeras_vlotje", "kaasmoeras_motknabbels", "kaasmoeras_onvahoeg",
                      "seen_kaasmoerasguh", "seen_kikkerguh", "seen_kaasmot", "seen_moerasheks_mika"]
IMPOSSIBLE = {"trigger": "minecraft:impossible"}


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": IMPOSSIBLE}})
    tame = {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                           "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"kaasmoerasguh\"}"}}]}}
    h.w(f"{D}/advancement/quest/tamed_kaasmoerasguh.json", {"criteria": {"done": tame}})
    kill = {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "guhs:moerasheks_mika"}}]}}
    h.w(f"{D}/advancement/quest/moerasheks_verslagen.json", {"criteria": {"done": kill}})
    for name, parent, icon, frame, crit, title, desc in [
        ("find_kaasmoeras", "enter_guhmension", "guhs:modderig_kaasgras", "task",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{BIOME}"}}}},
         "Tot je knieën in de kaas", "Stap in het mistige Kaasmoeras"),
        ("kaasmoeras_stuiter", "find_kaasmoeras", "guhs:borrelende_kaassaus", "task", IMPOSSIBLE,
         "Boing, njeg!", "Stuiter omhoog op borrelende kaassaus"),
        ("kaasmoeras_vlotje", "find_kaasmoeras", "minecraft:barrel", "task", IMPOSSIBLE,
         "Ahoy, knabbels!", "Open de ton op een knabbelvlotje"),
        ("kaasmoeras_motknabbel", "find_kaasmoeras", "guhs:motknabbel", "task",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:motknabbel"}]}},
         "Hap, slik, licht!", "Krijg een motknabbel: een kikkerguh die een kaasmot ophapt"),
        ("kaasmoeras_regenboog", "kaasmoeras_motknabbel", "minecraft:ochre_froglight", "challenge", IMPOSSIBLE,
         "Alle kikkerkleurtjes", "Heb een roze, een mint en een gele motknabbel tegelijk op zak"),
        ("kaasmoeras_kikkerguhtje", "kaasmoeras_motknabbel", "guhs:kikkerguh_spawn_egg", "task",
         {"trigger": "minecraft:bred_animals", "conditions": {"child": [
             {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "guhs:kikkerguh"}}]}},
         "Kwaak-vahoeg!", "Laat twee kikkerguhs met kaasknabbels een kikkerguhtje krijgen"),
        ("kaasmoerasguh_getemd", "find_kaasmoeras", "guhs:kaas_knabbels", "goal", tame,
         "Modderguhtje", "Tem een groen-geel gevlekte Kaasmoerasguh"),
        ("find_moerasheks_hut", "find_kaasmoeras", "guhs:kaasmodder", "task",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{NAME}"}}}},
         "Een hut op pootjes", "Vind de paalhut van de Moerasheks-Mika"),
        ("moerasheks_verslagen", "find_moerasheks_hut", "guhs:vadsverdrijvend_drankje", "goal", kill,
         "Weg met die heks!", "Versla de Moerasheks-Mika"),
        ("moeraskaas", "moerasheks_verslagen", "guhs:moeraskaas", "task",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:moeraskaas"}]}},
         "Stinkend lekker", "Krijg moeraskaas: goed voor in de guhbrouwketel"),
    ]:
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", title, title)
        h.lang(f"advancements.guhs.guhmension.{name}.description", desc, desc)


# =====================================================================================================================
# texts (Dutch, also in en_us)
# =====================================================================================================================
TEXTS = {
    "biome.guhs.kaasmoeras": "Kaasmoeras",
    "block.guhs.borrelende_kaassaus": "Borrelende kaassaus",
    "block.guhs.modderig_kaasgras": "Modderig kaasgras",
    "block.guhs.kaasmodder": "Kaasmodder",
    "block.guhs.kaasriet": "Kaasriet",
    "block.guhs.moerasgras": "Moerasgras",
    "block.guhs.motknabbel": "Motknabbel",
    "block.guhs.motknabbel.roze": "Roze motknabbel",
    "block.guhs.motknabbel.mint": "Mint motknabbel",
    "block.guhs.motknabbel.geel": "Gele motknabbel",
    "block.guhs.motknabbel.lore": "Een gloeiende kaasmot-knabbel, uitgespuugd door een kikkerguh",
    "item.guhs.moeraskaas": "Moeraskaas",
    "item.guhs.vadsverdrijvend_drankje": "Vadsverdrijvend drankje",
    "item.guhs.vadsverdrijvend_drankje.lore": "Gooi het! Wie het raakt, voelt zich even onvahoeg",
    "item.guhs.kikkerguh_spawn_egg": "Kikkerguh-spawnei",
    "item.guhs.kaasmot_spawn_egg": "Kaasmot-spawnei",
    "item.guhs.moerasheks_mika_spawn_egg": "Moerasheks-Mika-spawnei",
    "entity.guhs.kikkerguh": "Kikkerguh",
    "entity.guhs.kaasmot": "Kaasmot",
    "entity.guhs.moerasheks_mika": "Moerasheks-Mika",
    "entity.guhs.vadsverdrijvend_drankje": "Vadsverdrijvend drankje",
    "entity.guhs.guh.kaasmoerasguh": "Kaasmoerasguh",
    "effect.guhs.onvahoeg": "Onvahoeg",
    "structure.guhs.moerasheks_hut": "Paalhut van de Moerasheks",
    "structure.guhs.moerasheks_hut.tooltip": "In het Kaasmoeras: een hut op palen, met de Moerasheks-Mika erin (en haar moeraskaas)",
    # the Guhdex pages
    "gui.guhs.guhdex.rarity.kaasmoerasguh": "Zeldzaamheid: Alleen in het Kaasmoeras",
    "gui.guhs.guhdex.info.kaasmoerasguh": "Groen-geel gevlekt, alsof hij door de modder heeft gerold (heeft hij ook). Woont alleen in het Kaasmoeras. Tem hem met kaasknabbels: dan wordt hij nog vadsiger!",
    "gui.guhs.guhdex.rarity.kikkerguh": "Zeldzaamheid: Vaak (Kaasmoeras)",
    "gui.guhs.guhdex.info.kikkerguh": "Kwaak-guh! Hupt rond in het Kaasmoeras, in roze, mint of geel. Hapt kaasmotten weg en spuugt dan een gloeiende motknabbel uit. Lust ook kaasknabbels.",
    "gui.guhs.guhdex.rarity.kaasmot": "Zeldzaamheid: Vaak (rond kaas en lampjes)",
    "gui.guhs.guhdex.info.kaasmot": "Een piepklein Mika-motje met gatenkaas-vleugeltjes. Fladdert rond kaas en lampjes en pikt kaasknabbels van de grond. Njeg! Kikkerguhs vinden ze heerlijk.",
    "gui.guhs.guhdex.rarity.moerasheks_mika": "Zeldzaamheid: Zeldzaam (paalhut in het Kaasmoeras)",
    "gui.guhs.guhdex.info.moerasheks_mika": "Een Mika met een punthoed. Gooit vadsverdrijvende drankjes (dan ben je even onvahoeg) en knabbelt haar eigen moeraskaas als ze pijn heeft. Laat moeraskaas vallen!",
    # messages
    "quest.guhs.kaasmoeras.onvahoeg": "Bleh... je voelt je even een stuk minder vahoeg.",
    "quest.guhs.kaasmoeras.heks0": "Moerasheks-Mika: Njeg! Blijf van mijn moeraskaas af, vadsig ding!",
    "quest.guhs.kaasmoeras.heks1": "Moerasheks-Mika: Hihihi! Een drankje van vadsverdrijving, speciaal voor jou!",
    "quest.guhs.kaasmoeras.heks2": "Moerasheks-Mika: Jij bent veel te vahoeg voor mijn moeras. Dat lossen we even op, njeg!",
    "quest.guhs.kaasmoeras.heks3": "Moerasheks-Mika: Mijn kaasmotten hebben al jouw knabbels ingepikt, hihi!",
    # signs
    "sign.guhs.kaasmoeras.heks1": "MOERASHEKS",
    "sign.guhs.kaasmoeras.heks2": "Verboden voor",
    "sign.guhs.kaasmoeras.heks3": "vahoege guhs!",
    "sign.guhs.kaasmoeras.heks4": "Njeg!",
    "sign.guhs.kaasmoeras.borrel1": "Pas op:",
    "sign.guhs.kaasmoeras.borrel2": "borrelende",
    "sign.guhs.kaasmoeras.borrel3": "kaassaus!",
    "sign.guhs.kaasmoeras.borrel4": "BOING!",
    "sign.guhs.kaasmoeras.vlot1": "Knabbelvlotje",
    "sign.guhs.kaasmoeras.vlot2": "(gestolen)",
    "sign.guhs.kaasmoeras.voorraad1": "Gestolen",
    "sign.guhs.kaasmoeras.voorraad2": "knabbels",
    "sign.guhs.kaasmoeras.voorraad3": "AFBLIJVEN",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# FTB quests (rows y=74 and y=75.5, no dependencies)
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    y = FTB_Y
    q("kaasmoeras_vind", "Het Kaasmoeras",
      "Ergens in de Guhmensie hangt een gele mist boven een drassig moeras vol borrelende kaassaus: het &eKaasmoeras&r. Stap erin (laarzen aan!).",
      "guhs:modderig_kaasgras", [fq.biome(BIOME)], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=y, shape="octagon", xp=100)
    q("kaasmoeras_stuiter", "Boing, njeg!",
      "Stap op &6borrelende kaassaus&r en je stuitert omhoog! Sluip om er rustig doorheen te waden. Vallen doet er nooit pijn.",
      "guhs:borrelende_kaassaus", [adv("kaasmoeras_stuiter")], x=-6, y=y)
    q("kaasmoeras_vlotje", "Ahoy, knabbels!",
      "Op grote moerasplassen dobbert soms een &eknabbelvlotje&r met een roze vlaggetje. Klim erop en open de ton!",
      "minecraft:barrel", [adv("kaasmoeras_vlotje")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-4, y=y, xp=100)
    q("kaasmoeras_kikkerguh", "Kwaak-guh!",
      "Sta vlak naast een &akikkerguh&r voor je Guhdex. Ze zijn er in roze, mint en geel!",
      "guhs:kikkerguh_spawn_egg", [adv("seen_kikkerguh")], x=-2, y=y)
    q("kaasmoeras_kaasmot", "Mika-motjes",
      "'s Avonds fladderen &dkaasmotten&r rond kaas en lampjes, en ze pikken kaasknabbels die op de grond liggen. Zoek er een op voor je Guhdex.",
      "guhs:kaasmot_spawn_egg", [adv("seen_kaasmot")], x=0, y=y)
    q("kaasmoeras_motknabbel", "Hap, slik, licht!",
      "Een kikkerguh hapt kaasmotten weg met zijn lange tong en spuugt dan een gloeiende &emotknabbel&r uit, in zijn eigen kleur.",
      "guhs:motknabbel", [item("guhs:motknabbel")], rewards=(("guhs:kaas_knabbels", 16),), x=2, y=y)
    q("kaasmoeras_regenboog", "Alle kikkerkleurtjes",
      "Heb een roze, een mint en een gele motknabbel tegelijk op zak.",
      "minecraft:ochre_froglight", [adv("kaasmoeras_motknabbels")], rewards=(("guhs:guh_kristal", 8),), x=4, y=y, shape="gear", xp=200)
    q("kaasmoeras_moerasguh", "Modderguhtje",
      "Wilde guhs in het Kaasmoeras zijn vaak groen-geel gevlekt: de &aKaasmoerasguh&r! Zoek er een en tem hem met kaasknabbels.",
      "guhs:guhdex", [adv("tamed_kaasmoerasguh")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=6, y=y, shape="rsquare", xp=200)
    y += 1.5
    q("kaasmoeras_hut", "Een hut op pootjes",
      "Midden in het moeras staat een paalhut met een reuzenheksenhoed: het hoofd van de &5Moerasheks-Mika&r zelf! Het superkompas (Avontuur) wijst de weg.",
      "guhs:kaasmodder", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=y, shape="hexagon", xp=100)
    q("kaasmoeras_onvahoeg", "Bleh, onvahoeg",
      "De Moerasheks-Mika gooit &2vadsverdrijvende drankjes&r. Word er eens door geraakt (het is maar even, njeg).",
      "guhs:vadsverdrijvend_drankje", [adv("kaasmoeras_onvahoeg")], x=-4, y=y)
    q("kaasmoeras_heks", "Weg met die heks!",
      "Versla de Moerasheks-Mika. Ze knabbelt haar eigen moeraskaas als ze pijn heeft, dus wees snel!",
      "guhs:vadsverdrijvend_drankje", [adv("moerasheks_verslagen")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-2, y=y, xp=200)
    q("kaasmoeras_moeraskaas", "Stinkend lekker",
      "Verzamel &emoeraskaas&r van de Moerasheks-Mika (of uit haar kist). Goed voor de guhbrouwketel!",
      "guhs:moeraskaas", [item("guhs:moeraskaas", 3)], rewards=(("guhs:vahoege_vads_ingot", 1),), x=0, y=y)
    q("kaasmoeras_heks_dex", "Heksenpagina",
      "Sta vlak naast de Moerasheks-Mika (durf je?) voor haar Guhdex-pagina.",
      "guhs:guhdex", [adv("seen_moerasheks_mika")], x=2, y=y)


# =====================================================================================================================
# own asset self-check (check_assets.py only knows the registry classes)
# =====================================================================================================================
def selfcheck_assets(h):
    A = h.A
    missing = []
    for b in ("borrelende_kaassaus", "modderig_kaasgras", "kaasmodder", "kaasriet", "moerasgras", "motknabbel"):
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{h.D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("moeraskaas", "vadsverdrijvend_drankje", "kikkerguh_spawn_egg", "kaasmot_spawn_egg", "moerasheks_mika_spawn_egg"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for e, textures in (("kikkerguh", [f"kikkerguh_{k}" for k in KIKKER]), ("kaasmot", ["kaasmot"]), ("moerasheks_mika", ["moerasheks_mika"])):
        for p in [f"{A}/geckolib/models/entity/{e}.geo.json", f"{A}/geckolib/animations/entity/{e}.animation.json"] + \
                 [os.path.join(h.TEX, "entity", f"{t}.png") for t in textures]:
            if not os.path.exists(p):
                missing.append(p)
        if f"entity.guhs.{e}" not in h.NL:
            missing.append(f"lang entity.guhs.{e}")
    for p in (os.path.join(h.TEX, "entity", "guh_kaasmoerasguh.png"), os.path.join(h.TEX, "mob_effect", "onvahoeg.png")):
        if not os.path.exists(p):
            missing.append(p)
    # every texture our models use exists
    for f in os.listdir(f"{A}/models/block"):
        if f.startswith(("borrelende", "modderig", "kaasmodder", "kaasriet", "moerasgras", "motknabbel")):
            for t in json.load(open(f"{A}/models/block/{f}", encoding="utf-8")).get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(os.path.join(h.TEX, t[5:] + ".png")):
                    missing.append(f"texture {t} of {f}")
    # the GeckoLib animations the entities play exist
    for e, names in (("kikkerguh", ["idle", "hop", "swim", "croak", "tongue"]), ("kaasmot", ["fly"]),
                     ("moerasheks_mika", ["idle", "walk", "throw", "nibble"])):
        anims = json.load(open(f"{A}/geckolib/animations/entity/{e}.animation.json", encoding="utf-8"))["animations"]
        for n in names:
            if f"animation.{e}.{n}" not in anims:
                missing.append(f"animation.{e}.{n}")
    if missing:
        raise SystemExit(f"kaasmoeras assets missing: {missing}")


# =====================================================================================================================
# the structure: the paalhut of the Moerasheks-Mika
# =====================================================================================================================
W, H, DP = 45, 46, 45
G = 6                           # the ground layer (the pond's water is level with it)
P = G + 6                       # the platform floor
CX, CZ = 22, 20                 # the middle of the Mika head (the hut)
RX, RZ = 10.0, 8.5
CY, RY_TOP, RY_BOT = P + 4.5, 8.0, 4.5
PX0, PX1, PZ0, PZ1 = 9, 35, 8, 34                     # the platform (a porch in front: the head ends at z ~28)
STAIRS = (21, 22, 23)
LOFT_Y = P + 6
LOFT_EDGE = CZ - 2              # the loft covers z <= LOFT_EDGE
LADDER = (CX - 4, CZ - 1)
POND = ((22, 22, 17.0, 15.0), (7, 29, 5.5, 5.5))       # (cx, cz, rx, rz): the pond under the hut and its west lobe
BORREL = (38, 38, 3.2)                                  # the borrelplas on the land
JETTY_Z = (25, 26)
VLOT = (4, 28)                  # the knabbelvlotje: x 4..7, z 28..30
STILTS = sorted({(x, z) for x in (9, 14, 18, 22, 26, 30, 35) for z in (PZ0, PZ1)} |
                {(x, z) for x in (PX0, PX1) for z in (8, 13, 17, 21, 25, 30, 34)} |
                {(x, z) for x in (14, 22, 30) for z in (13, 21, 28)})

PASSABLE_WORDS = ("air", "water", "kaasriet", "moerasgras", "carpet", "candle", "sign", "ladder", "_door", "vlaggetjes", "chain",
                  "hanging_roots", "lily_pad", "waterlelie", "rail", "button", "banner")
OBSTACLE_WORDS = ("fence", "_wall", "pane", "iron_bars", "lantern", "flower_pot", "potted_", "brewing_stand", "campfire", "lampion",
                  "composter", "cauldron")
LIGHTS = {"minecraft:lantern": 15, "minecraft:soul_lantern": 10, "minecraft:campfire": 15, "guhs:motknabbel": 15, "guhs:lampion_geel": 15,
          "guhs:lampion_roze": 15, "guhs:lampion_mint": 15, "minecraft:smoker": 13, "minecraft:green_candle": 9, "minecraft:lime_candle": 9,
          "guhs:borrelende_kaassaus": 6, "minecraft:shroomlight": 15}


def mcn(n):
    return n if ":" in n else f"minecraft:{n}"


class Hut:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, DP))
        self.rng = random.Random(20270404)
        self.pond = set()           # (x, z) columns with water
        self.head_inner, self.head_shell = set(), set()
        self.door = None
        self.chest = None

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mcn(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def props(self, x, y, z):
        b = self.s.blocks.get((x, y, z))
        return b[1] if b else {}


def pond_d(x, z):
    """How far into the pond (x, z) is: < 1 is water (the smallest of the two lobes, with a little wobble)."""
    best = 9.0
    for (cx, cz, rx, rz) in POND:
        a = math.atan2(z - cz, x - cx)
        wob = 1 + 0.07 * math.sin(3 * a + 1.3) + 0.05 * math.sin(5 * a)
        best = min(best, math.hypot((x - cx) / rx, (z - cz) / rz) / wob)
    return best


def head_inside(x, y, z):
    ry = RY_TOP if y >= CY else RY_BOT
    return abs(x - CX) ** 3 / RX ** 3 + abs(y - CY) ** 3 / ry ** 3 + abs(z - CZ) ** 3 / RZ ** 3 <= 1


def ground(hut):
    """The ground: kaasgras on kaasmodder, the pond (1-2 deep, kaasmodder bottom and rim) and the borrelplas."""
    for x in range(W):
        for z in range(DP):
            d = pond_d(x, z)
            for y in range(G - 3, G):
                hut.set(x, y, z, "guhs:kaasmodder")
            if d < 1:
                hut.pond.add((x, z))
                hut.set(x, G, z, "water", {"level": "0"})
                if d < 0.8:
                    hut.set(x, G - 1, z, "water", {"level": "0"})
            elif d < 1.12:
                hut.set(x, G, z, "guhs:kaasmodder")
            else:
                hut.set(x, G, z, "guhs:modderig_kaasgras")
    bx, bz, br = BORREL
    for x in range(bx - 4, bx + 5):
        for z in range(bz - 4, bz + 5):
            d = math.hypot(x - bx, z - bz)
            if d <= br:
                hut.set(x, G, z, "guhs:borrelende_kaassaus")
            elif d <= br + 1.1:
                hut.set(x, G, z, "guhs:kaasmodder")
    # the path from the stairs to the jetty and to the borrelplas
    for x in range(0, 40):
        for z in (41, 42):
            if (x, z) not in hut.pond and hut.get(x, G, z) == "guhs:modderig_kaasgras":
                hut.set(x, G, z, "packed_mud" if hut.rng.random() < 0.7 else "mud_bricks")


def stilts(hut):
    rng = hut.rng
    for (x, z) in STILTS:
        for y in range(G - 2, P):
            hut.set(x, y, z, "mangrove_log", {"axis": "y"})
        wet = (x, z) in hut.pond
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):      # mangrove roots flaring out at the waterline
            nx, nz = x + dx, z + dz
            if (nx, nz) in STILTS or not (0 <= nx < W and 0 <= nz < DP) or rng.random() < 0.3:
                continue
            y = G if (nx, nz) in hut.pond else G + 1
            hut.set(nx, y, nz, "mangrove_roots", {"waterlogged": "true" if (nx, nz) in hut.pond else "false"})
        if not wet:
            continue
    # beams under the platform: a frame along the edges and three under the hut
    for x in range(PX0, PX1 + 1):
        for z in (PZ0, PZ1, 13, 21, 28):
            if (x, z) not in STILTS:
                hut.set(x, P - 1, z, "stripped_mangrove_log", {"axis": "x"})
    for z in range(PZ0, PZ1 + 1):
        for x in (PX0, PX1):
            if (x, z) not in STILTS:
                hut.set(x, P - 1, z, "stripped_mangrove_log", {"axis": "z"})
    # roots dangling from the edge beams
    for x in range(PX0, PX1 + 1):
        for z in range(PZ0, PZ1 + 1):
            edge = x in (PX0, PX1) or z in (PZ0, PZ1)
            if edge and (x, z) not in STILTS and hut.get(x, P - 1, z) and rng.random() < 0.3:
                hut.set(x, P - 2, z, "hanging_roots", {"waterlogged": "false"})


def platform(hut):
    rng = hut.rng
    for x in range(PX0, PX1 + 1):
        for z in range(PZ0, PZ1 + 1):
            edge = x in (PX0, PX1) or z in (PZ0, PZ1)
            if edge:
                hut.set(x, P, z, "stripped_mangrove_log", {"axis": "x" if z in (PZ0, PZ1) else "z"})
            else:
                hut.set(x, P, z, "spruce_planks" if rng.random() < 0.12 else "mangrove_planks")
    # the railing (with a gap for the stairs), tall posts with lanterns on the corners, lampions along the sides
    for x in range(PX0, PX1 + 1):
        for z in range(PZ0, PZ1 + 1):
            if not (x in (PX0, PX1) or z in (PZ0, PZ1)) or (z == PZ1 and x in STAIRS):
                continue
            hut.set(x, P + 1, z, "mangrove_fence")
    for (x, z) in ((PX0, PZ0), (PX1, PZ0), (PX0, PZ1), (PX1, PZ1)):
        hut.set(x, P + 2, z, "mangrove_fence")
        hut.set(x, P + 3, z, "lantern", {"hanging": "false", "waterlogged": "false"})
    for i, (x, z) in enumerate([(PX0, 14), (PX0, 26), (PX1, 14), (PX1, 26), (14, PZ0), (30, PZ0), (15, PZ1), (29, PZ1)]):
        hut.set(x, P + 2, z, f"guhs:lampion_{('roze', 'geel', 'mint')[i % 3]}", {"hanging": "false", "waterlogged": "false"})
    # little flag garlands along the porch, from post to post
    for x in range(PX0 + 1, PX1):
        hut.set(x, P + 3, PZ1, "guhs:vlaggetjes", {"axis": "x"})
    for z in range(PZ0 + 1, PZ1):
        if z % 2 == 0:
            hut.set(PX0, P + 3, z, "guhs:vlaggetjes", {"axis": "z"})
            hut.set(PX1, P + 3, z, "guhs:vlaggetjes", {"axis": "z"})
    for z in range(PZ0 + 1, PZ1):
        if z % 2 == 1:
            hut.set(PX0, P + 3, z, "guhs:vlaggetjes", {"axis": "z"})
            hut.set(PX1, P + 3, z, "guhs:vlaggetjes", {"axis": "z"})


def stairs(hut):
    for k in range(0, P - G):
        y, z = P - k, PZ1 + 1 + k
        for x in STAIRS:
            hut.set(x, y, z, "mangrove_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for yy in range(y + 1, y + 4):
                hut.set(x, yy, z, "air")
        if k % 2 == 1 and k < P - G - 1:
            for x in (STAIRS[0], STAIRS[-1]):
                for yy in range(G - 1 if (x, z) in hut.pond else G + 1, y):
                    wet = (x, z) in hut.pond and yy <= G
                    hut.set(x, yy, z, "mangrove_fence", {"waterlogged": "true" if wet else "false"})
    # a sign at the foot of the stairs: this is the Moerasheks' home
    sign(hut, STAIRS[-1] + 2, G + 1, PZ1 + (P - G) + 1, "south", ["kaasmoeras.heks1", "kaasmoeras.heks2", "kaasmoeras.heks3", "kaasmoeras.heks4"])


def sign(hut, x, y, z, facing, keys, wall=False):
    h = hut.h
    msgs = [json.dumps({"translate": f"sign.guhs.{k}"}) if k else '""' for k in keys] + ['""'] * (4 - len(keys))
    text = {"messages": h.ms.NbtList(8, msgs), "color": "black", "has_glowing_text": h.Byte(1)}
    empty = {"messages": h.ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": h.Byte(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": h.Byte(1), "front_text": text, "back_text": empty}
    if wall:
        hut.set(x, y, z, "mangrove_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        hut.set(x, y, z, "mangrove_sign", {"rotation": rot, "waterlogged": "false"}, nbt)


# --- the hut: the head of the Moerasheks-Mika ---------------------------------------------------------------------------
def face_block(u, v):
    """The Mika face on the front of the head: u = x - CX, v = height above the platform. None = just skin."""
    for eu in (-4.5, 4.5):                                      # evil red eyes (windows)
        d = math.hypot((u - eu) / 2.4, (v - 7.5) / 2.2)
        if d <= 1:
            if d <= 0.62:
                return "white_concrete" if (u - eu) < -0.4 and (v - 7.5) > 0.4 else "red_stained_glass"
            return "black_concrete"
    if 1.5 <= abs(u) <= 7.5 and abs(v - (10.4 + (abs(u) - 1.5) * 0.33)) < 0.55:   # angry eyebrows
        return "black_concrete"
    if math.hypot((abs(u) - 7.5) / 1.7, (v - 4.5) / 1.2) <= 1:                     # pink Mika cheeks
        return "pink_concrete"
    grin = 2.4 + 0.07 * u * u
    if abs(u) <= 6 and abs(v - grin) < 0.55:                                        # a big grin...
        return "black_concrete"
    if abs(u) == 2 and abs(v - (grin - 1)) < 0.55:                                  # ...with two little fangs
        return "white_concrete"
    return None


def skin(x, y, z):
    k = (x * 7 + y * 13 + z * 5) % 23
    return "moss_block" if k == 0 else "green_terracotta" if k in (3, 11, 17) else "lime_terracotta"


def head(hut):
    inner = set()
    for x in range(CX - int(RX) - 1, CX + int(RX) + 2):
        for z in range(CZ - int(RZ) - 1, CZ + int(RZ) + 2):
            for y in range(P + 1, P + 14):
                if head_inside(x, y, z):
                    inner.add((x, y, z))
    shell = {c for c in inner if any((c[0] + dx, c[1] + dy, c[2] + dz) not in inner for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                                     for dz in (-1, 0, 1) if c[1] + dy > P)}
    hut.head_inner, hut.head_shell = inner, shell
    front_z = {}
    for (x, y, z) in inner:
        front_z[(x, y)] = max(front_z.get((x, y), -1), z)
    for c in inner - shell:
        hut.set(*c, "air")
    for (x, y, z) in shell:
        u, v = x - CX, y - P
        name = skin(x, y, z)
        if z == front_z[(x, y)] and z > CZ + 2:
            name = face_block(u, v) or name
        elif abs(u) >= RX - 2.5 and v in (5, 6) and (z - CZ) in (-3, -2, 2, 3):
            name = "yellow_stained_glass"                      # round windows in the sides
        elif z < CZ - 5 and abs(u) <= 1 and v in (5, 6):
            name = "yellow_stained_glass"                      # and one in the back
        hut.set(x, y, z, name)
    # the door in the mouth
    dz = max(front_z[(CX, P + 1)], front_z[(CX, P + 2)])
    hut.door = (CX, P + 1, dz)
    for v, half in ((1, "lower"), (2, "upper")):
        for z in range(dz - 3, dz):                             # a short passage through the thick bottom of the head
            if (CX, P + v, z) in shell:
                hut.set(CX, P + v, z, "air")
        hut.set(CX, P + v, dz, "mangrove_door", {"facing": "north", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    for u in (-1, 1):                                           # the dark inside of the mouth around the door
        for v in (1, 2, 3):
            for z in range(dz - 3, dz + 1):
                if (CX + u, P + v, z) in shell or (CX + u, P + v, z) not in inner and z <= dz and v < 3:
                    hut.set(CX + u, P + v, z, "black_concrete")
        if (CX, P + 3, dz) not in inner:
            hut.set(CX, P + 3, dz, "black_concrete")
    # the big hooked witch nose with a wart
    for u in (-1, 0, 1):
        for v in (5, 6):
            hut.set(CX + u, P + v, front_z[(CX + u, P + v)] + 1, "green_concrete")
    hut.set(CX, P + 5, front_z[(CX, P + 5)] + 2, "green_concrete")
    hut.set(CX, P + 4, front_z[(CX, P + 4)] + 2, "green_concrete")
    hut.set(CX + 1, P + 6, front_z[(CX + 1, P + 6)] + 2, "brown_terracotta")
    # the ears poke out of the hat: skin outside, pink inside
    for side in (-1, 1):
        ex, ey = CX + side * 11, P + 11
        for x in range(ex - 4, ex + 5):
            for y in range(ey - 4, ey + 5):
                d = math.hypot(x - ex, (y - ey) * 1.1)
                if d > 3.3:
                    continue
                for z in (CZ - 1, CZ, CZ + 1):
                    if (x, y, z) in inner:
                        continue
                    hut.set(x, y, z, "pink_terracotta" if d <= 2.0 and z == CZ + 1 else "lime_terracotta")
    # the devil tail out of the back of the head, curling up to a spade
    back = min(z for (x, y, z) in inner if x == CX and y == P + 3)
    path = [(0, 3, -1), (0, 3, -2), (0, 4, -2), (0, 4, -3), (0, 5, -3), (0, 5, -4), (0, 6, -4), (0, 7, -4), (0, 7, -3), (0, 8, -3)]
    for (dx, dy, dz) in path:
        hut.set(CX + dx, P + dy, back + dz, "red_concrete")
    for (dx, dy) in ((-1, 9), (0, 9), (1, 9), (0, 10), (-1, 8), (1, 8)):
        hut.set(CX + dx, P + dy, back - 3, "red_concrete")
    hut.set(CX, P + 11, back - 3, "red_concrete")


def hat(hut):
    top = max(y for (x, y, z) in hut.head_inner)
    by = top + 1
    rng = hut.rng
    for x in range(CX - 15, CX + 16):
        for z in range(CZ - 13, CZ + 14):
            if math.hypot((x - CX) / 13.5, (z - CZ) / 11.5) <= 1 and hut.get(x, by, z) is None:
                hut.set(x, by, z, "purple_terracotta")
    height = 15
    tip = None
    for i in range(height):
        y = by + 1 + i
        t = i / height
        r = 8.2 * (1 - t) ** 0.9 + 0.6
        sx = max(0.0, i - 8) ** 1.6 * 0.33
        cxs, czs = CX + sx, CZ
        cells = [(x, z) for x in range(int(cxs - r) - 1, int(cxs + r) + 2) for z in range(int(czs - r) - 1, int(czs + r) + 2)
                 if math.hypot(x - cxs, z - czs) <= r]
        if not cells:
            break
        front = max(z for (x, z) in cells)
        for (x, z) in cells:
            name = "purple_concrete" if (x + z + y) % 9 else "purple_terracotta"
            if i < 2:
                name = "yellow_concrete"                           # the cheese-yellow hat band...
                if z == front and abs(x - CX) <= 1:
                    name = "gold_block"                             # ...with a golden buckle
            elif 5 <= i <= 6 and x < cxs - r + 2.2 and abs(z - CZ) <= 1:
                name = "magenta_terracotta"                         # a patch on the side
            hut.set(x, y, z, name)
        tip = (round(cxs), y + 1, CZ)
    hut.set(*tip, "guhs:motknabbel", {"kleur": "geel"})           # a glowing star on the tip
    # the chimney outside the back wall, through the brim, smoking
    chx, chz = CX + 5, CZ - 9
    hut.set(chx, P + 1, chz, "smoker", {"facing": "south", "lit": "true"})
    for y in range(P + 2, by + 3):
        hut.set(chx, y, chz, "mud_bricks")
    hut.set(chx, by + 3, chz, "campfire", {"lit": "true", "signal_fire": "false", "facing": "north", "waterlogged": "false"})
    rng.random()


def interior(hut):
    rng = hut.rng
    air = hut.head_inner - hut.head_shell
    floor_cells = [(x, z) for (x, y, z) in air if y == P + 1]
    # --- the loft (sleeping place) at the back, with a shelf wall under its edge and a ladder ---
    for (x, y, z) in air:
        if y == LOFT_Y and z <= LOFT_EDGE:
            hut.set(x, y, z, "spruce_planks")
    lx, lz = LADDER
    for (x, y, z) in air:
        if z == LOFT_EDGE and P + 1 <= y < LOFT_Y and abs(x - CX) >= 2:
            hut.set(x, y, z, "bookshelf")
    for y in range(P + 1, LOFT_Y + 1):
        hut.set(lx, y, lz, "ladder", {"facing": "south", "waterlogged": "false"})
    for (x, y, z) in air:
        if y == LOFT_Y + 1 and z == LOFT_EDGE and x != lx and (x, y - 1, z) in air:
            hut.set(x, y, z, "spruce_fence")
    bx = CX + 2
    hut.set(bx, LOFT_Y + 1, CZ - 4, "red_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    hut.set(bx, LOFT_Y + 1, CZ - 5, "red_bed", {"facing": "north", "part": "head", "occupied": "false"})
    for (x, z) in ((bx - 1, CZ - 4), (bx - 1, CZ - 5), (bx + 1, CZ - 4)):
        if (x, LOFT_Y + 1, z) in air:
            hut.set(x, LOFT_Y + 1, z, "purple_carpet")
    hut.set(CX - 2, LOFT_Y + 1, CZ - 5, "lantern", {"hanging": "false", "waterlogged": "false"})
    hut.set(CX - 1, LOFT_Y + 1, CZ - 6, "barrel", {"facing": "up", "open": "false"})
    hut.set(bx + 2, LOFT_Y + 1, CZ - 6, "potted_red_mushroom")
    # --- the storeroom under the loft: the stolen kaasknabbels and the Moerasheks' chest ---
    back_cells = sorted([(x, z) for (x, z) in floor_cells if z < LOFT_EDGE - 1], key=lambda c: (c[1], c[0]))
    back_z = min(z for (x, z) in back_cells)
    for (x, z) in back_cells:
        if z <= back_z + 1 and CX + 2 <= x <= CX + 5:
            for y in (P + 1, P + 2):
                if (x, y, z) in air:
                    hut.set(x, y, z, "guhs:block_of_kaasknabbels")
    ch = (CX - 3, P + 1, back_z)
    if ch[:1] and (ch[0], ch[1], ch[2]) in air:
        hut.chest = ch
    else:
        cands = [(x, P + 1, z) for (x, z) in back_cells if x < CX]
        hut.chest = cands[0]
    hut.set(*hut.chest, "chest", {"facing": "south", "type": "single", "waterlogged": "false"},
            {"id": "minecraft:chest", "LootTable": f"guhs:chests/{NAME}"})
    sign(hut, CX + 3, P + 3, back_z, "south", ["kaasmoeras.voorraad1", "kaasmoeras.voorraad2", "kaasmoeras.voorraad3"], wall=True) \
        if hut.get(CX + 3, P + 3, back_z - 1) not in (None, "minecraft:air") else None
    for x in (CX - 5, CX, CX + 5):                              # lanterns hanging under the loft
        if (x, LOFT_Y - 1, CZ - 4) in air and hut.get(x, LOFT_Y, CZ - 4) == "minecraft:spruce_planks":
            hut.set(x, LOFT_Y - 1, CZ - 4, "lantern", {"hanging": "true", "waterlogged": "false"})
    # --- the witch's kitchen: a big cauldron with candles, two brewing stands, shelves and barrels along the walls ---
    hut.set(CX, P + 1, CZ + 2, "water_cauldron", {"level": "3"})
    for (x, z) in ((CX - 1, CZ + 2), (CX + 1, CZ + 2)):
        hut.set(x, P + 1, z, "green_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    for x in (CX - 4, CX + 4):
        hut.set(x, P + 1, CZ + 4, "brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    for (x, z) in ((CX - 1, CZ + 4), (CX, CZ + 4), (CX + 1, CZ + 4), (CX - 1, CZ + 5), (CX, CZ + 5), (CX + 1, CZ + 5)):
        hut.set(x, P + 1, z, "lime_carpet")
    wall_cycle = ["bookshelf", "barrel", "crafting_table", "bookshelf", "composter", "barrel", "bookshelf"]
    i = 0
    front = max(z for (x, z) in floor_cells)
    for (x, z) in sorted(floor_cells):
        if z <= LOFT_EDGE or hut.get(x, P + 1, z) not in ("minecraft:air", None) or z >= front - 1 and abs(x - CX) <= 2:
            continue
        if not any((x + dx, P + 1, z + dz) in hut.head_shell for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        if (x, z) == LADDER or abs(x - LADDER[0]) <= 1 and abs(z - LADDER[1]) <= 1:
            continue
        name = wall_cycle[i % len(wall_cycle)]
        i += 1
        props = {"facing": "up", "open": "false"} if name == "barrel" else {"level": "0"} if name == "composter" else None
        hut.set(x, P + 1, z, name, props)
        if name == "bookshelf" and (x, P + 2, z) in air:
            hut.set(x, P + 2, z, rng.choice(["potted_brown_mushroom", "potted_red_mushroom", "potted_dead_bush", "potted_fern"]))
    # lanterns on chains from the ceiling
    for (x, z) in ((CX - 4, CZ + 2), (CX + 4, CZ + 2), (CX, CZ + 5), (CX - 5, CZ - 5), (CX + 5, CZ - 5)):
        ys = [y for (xx, y, zz) in air if xx == x and zz == z]
        if not ys:
            continue
        ceiling = max(ys)
        low = LOFT_Y + 2 if z <= LOFT_EDGE else P + 5
        if ceiling < low:
            continue
        for y in range(low + 1, ceiling + 1):
            hut.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
        hut.set(x, low, z, "lantern", {"hanging": "true", "waterlogged": "false"})
    # the Moerasheks-Mika herself, in her kitchen
    hut.s.entity(CX + 2.5, P + 1.0, CZ + 3.5, {"id": "guhs:moerasheks_mika", "PersistenceRequired": hut.h.Byte(1),
                                               "Rotation": hut.h.floats(180.0, 0.0)})


def deck(hut):
    rng = hut.rng
    # the porch: two water cauldrons, barrels, benches, pots, a composter; the side decks: more pots and barrels
    for (x, z, name, props) in [
        (PX0 + 2, PZ1 - 1, "water_cauldron", {"level": "3"}), (PX1 - 2, PZ1 - 1, "water_cauldron", {"level": "2"}),
        (PX0 + 1, PZ1 - 1, "barrel", {"facing": "up", "open": "false"}), (PX1 - 1, PZ1 - 1, "barrel", {"facing": "up", "open": "false"}),
        (PX0 + 1, PZ1 - 2, "potted_brown_mushroom", None), (PX1 - 1, PZ1 - 2, "potted_red_mushroom", None),
        (15, PZ1 - 1, "mangrove_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
        (16, PZ1 - 1, "mangrove_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
        (28, PZ1 - 1, "mangrove_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
        (29, PZ1 - 1, "mangrove_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
        (PX0 + 1, 12, "composter", {"level": "3"}), (PX0 + 1, 18, "barrel", {"facing": "up", "open": "false"}),
        (PX0 + 1, 19, "potted_fern", None), (PX1 - 1, 18, "barrel", {"facing": "up", "open": "false"}),
        (PX1 - 1, 12, "potted_dead_bush", None), (PX1 - 1, 24, "cauldron", None),
    ]:
        if hut.get(x, P + 1, z) is None:
            hut.set(x, P + 1, z, name, props)
    # a little moss here and there on the deck
    for x in range(PX0 + 1, PX1):
        for z in range(PZ0 + 1, PZ1):
            if hut.get(x, P + 1, z) is None and (x, P + 1, z) not in hut.head_inner and rng.random() < 0.05:
                hut.set(x, P + 1, z, "moss_carpet")


def frog_faces(hut):
    """Kikkerguh faces hanging under the deck on three sides (pink, mint and yellow): cute from every side."""
    pattern = ["fwkfffwkf", "fffffffff", "fcmmmmmcf", ".fffffff."]
    for (plane, fixed, centre, colour) in (("x", PX0 - 1, 21, "pink_concrete"), ("x", PX1 + 1, 21, "yellow_concrete"),
                                           ("z", PZ0 - 1, 22, "lime_concrete")):
        for j, row in enumerate(pattern):
            y = P - 2 - j
            for i, ch in enumerate(row):
                if ch == ".":
                    continue
                name = {"f": colour, "w": "white_concrete", "k": "black_concrete", "c": "magenta_concrete", "m": "black_concrete"}[ch]
                a = centre - 4 + i
                if plane == "x":
                    hut.set(fixed, y, a, name)
                else:
                    hut.set(a, y, fixed, name)


def surroundings(hut):
    rng = hut.rng
    # the jetty on the west lobe with the knabbelvlotje moored at its end
    for x in range(0, 9):
        for z in JETTY_Z:
            hut.set(x, G + 1, z, "mangrove_planks")
            hut.set(x, G + 2, z, "air")
            hut.set(x, G + 3, z, "air")
        if x in (3, 6):
            for z in JETTY_Z:
                for y in range(G - 1, G + 1):
                    if (x, z) in hut.pond:
                        hut.set(x, y, z, "mangrove_fence", {"waterlogged": "true"})
    sign(hut, 0, G + 2, JETTY_Z[0], "east", ["kaasmoeras.vlot1", "kaasmoeras.vlot2"])
    vx, vz = VLOT
    for x in range(vx, vx + 4):
        for z in range(vz, vz + 3):
            hut.set(x, G, z, "spruce_slab", {"type": "top", "waterlogged": "true"})
    hut.set(vx + 1, G + 1, vz + 1, "spruce_fence")
    hut.set(vx + 1, G + 2, vz + 1, "spruce_fence")
    hut.set(vx + 1, G + 3, vz + 1, "spruce_fence")
    hut.set(vx + 1, G + 4, vz + 1, "pink_banner", {"rotation": "4"})
    hut.set(vx + 2, G + 2, vz + 1, "pink_wool")
    hut.set(vx + 2, G + 3, vz + 1, "white_wool")
    hut.set(vx + 3, G + 1, vz + 2, "spruce_fence")
    hut.set(vx + 3, G + 2, vz + 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    hut.set(vx, G + 1, vz, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel", "LootTable": "guhs:chests/knabbelvlotje"})
    hut.set(vx + 3, G + 1, vz, "guhs:block_of_kaasknabbels")
    # the borrelplas sign and three motknabbel lamps on posts along the path
    bx, bz, br = BORREL
    sign(hut, bx - 5, G + 1, bz + 2, "west", ["kaasmoeras.borrel1", "kaasmoeras.borrel2", "kaasmoeras.borrel3", "kaasmoeras.borrel4"])
    for (x, kleur) in ((12, "roze"), (30, "mint"), (36, "geel")):
        hut.set(x, G + 1, 43, "mangrove_fence")
        hut.set(x, G + 2, 43, "mangrove_fence")
        hut.set(x, G + 3, 43, "guhs:motknabbel", {"kleur": kleur})
    # plants: kaasriet on the rim, moerasgras on the grass, lily pads and bubbling blobs on the pond
    for x in range(W):
        for z in range(DP):
            top = hut.get(x, G, z)
            if hut.get(x, G + 1, z) is not None:
                continue
            r = rng.random()
            under = PX0 - 1 <= x <= PX1 + 1 and PZ0 - 1 <= z <= PZ1 + 1
            if top == "guhs:kaasmodder" and r < 0.35 and hut.get(x, G + 2, z) is None:
                hut.set(x, G + 1, z, "guhs:kaasriet", {"half": "lower"})
                hut.set(x, G + 2, z, "guhs:kaasriet", {"half": "upper"})
            elif top == "guhs:modderig_kaasgras" and r < 0.2:
                hut.set(x, G + 1, z, "guhs:moerasgras")
            elif top == "minecraft:water" and not under and r < 0.1:
                hut.set(x, G + 1, z, "guhs:guh_waterlelie" if r < 0.03 else "lily_pad")
            elif top == "minecraft:water" and not under and r > 0.985 and hut.get(x, G - 1, z) == "minecraft:water":
                hut.set(x, G, z, "guhs:borrelende_kaassaus")
    # kikkerguhs in all three colours around the pond, and a Kaasmoerasguh grazing by the path
    for (x, z, kleur) in ((2.5, 25.5, "mint"), (39.5, 30.5, "roze"), (16.5, 40.5, "geel")):
        hut.s.entity(x, G + (2.0 if z == 25.5 else 1.0), z, {"id": "guhs:kikkerguh", "Kleur": kleur, "PersistenceRequired": hut.h.Byte(1)})
    hut.s.entity(33.5, G + 1.0, 42.5, hut.h.ms.guh_nbt(1.0, Variant="kaasmoerasguh"))


def connect_fences(hut):
    for (x, y, z), (b, props, nbt) in list(hut.s.blocks.items()):
        if not b.endswith("_fence"):
            continue
        new = dict(props)
        new.setdefault("waterlogged", "false")
        for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("west", (-1, 0)), ("east", (1, 0))):
            n = hut.get(x + dx, y, z + dz)
            new[d] = "true" if n and (n.endswith("_fence") or (solid(n) and "stairs" not in n)) else "false"
        hut.s.blocks[(x, y, z)] = (b, new, nbt)


def build_hut(h):
    hut = Hut(h)
    ground(hut)
    stilts(hut)
    platform(hut)
    stairs(hut)
    head(hut)
    hat(hut)
    interior(hut)
    deck(hut)
    frog_faces(hut)
    surroundings(hut)
    fp = [(x, z) for x in range(W) for z in range(DP)]
    hut.s.clear_above(fp, G + 1)
    connect_fences(hut)
    # the anchor: the structure is placed around it (it becomes kaasmodder again)
    hut.set(CX, G - 3, CZ, "jigsaw", {"orientation": "up_north"},
            {"id": "minecraft:jigsaw", "name": "guhs:moerasheks_hut_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
             "final_state": "guhs:kaasmodder", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    return hut


# --- the geometry self-check ------------------------------------------------------------------------------------------------
def passable(b):
    return b is None or any(w in b for w in PASSABLE_WORDS) and not b.endswith("_stairs")


def obstacle(b):
    return b is not None and any(w in b for w in OBSTACLE_WORDS)


def solid(b):
    """Something you can stand on (fences and pots are in the way but you can't step onto them)."""
    return b is not None and not passable(b) and not obstacle(b)


def light_map(hut):
    """Block light spread from every lamp through the non-opaque cells (a rough version of the game's)."""
    level = {}
    todo = deque()
    for (x, y, z), (b, props, _) in hut.s.blocks.items():
        lv = LIGHTS.get(b, 0)
        if b.endswith("candle") and props.get("lit") != "true" or b in ("minecraft:campfire", "minecraft:smoker") and props.get("lit") != "true":
            lv = 0
        if lv:
            level[(x, y, z)] = lv
            todo.append((x, y, z))

    def clear(c):
        b = hut.get(*c)
        return b is None or not solid(b) or "glass" in b or "slab" in b or "stairs" in b
    while todo:
        c = todo.popleft()
        lv = level[c] - 1
        if lv <= 0:
            continue
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (c[0] + d[0], c[1] + d[1], c[2] + d[2])
            if 0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < DP and clear(n) and level.get(n, 0) < lv:
                level[n] = lv
                todo.append(n)
    return level


def walk(hut, start):
    """Every feet position reachable from start: walking (1 up, 3 down), climbing ladders and swimming."""
    get = hut.get

    def fluid(c):
        b = get(*c)
        return b == "minecraft:water"

    def standable(c):
        x, y, z = c
        if not (0 <= x < W and 0 <= z < DP and 1 <= y < H - 2):
            return False
        here, head_ = get(x, y, z), get(x, y + 1, z)
        if not passable(here) or not passable(head_):
            return False
        below = get(x, y - 1, z)
        return solid(below) or fluid(c) or (here and "ladder" in here) or (below and "ladder" in below)

    seen = {start} if standable(start) else set()
    todo = deque(seen)
    while todo:
        x, y, z = todo.popleft()
        here = get(x, y, z) or ""
        nexts = []
        if "ladder" in here or fluid((x, y, z)):
            nexts += [(x, y + 1, z), (x, y - 1, z)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if passable(get(x, y + 2, z)):
                nexts.append((x + dx, y + 1, z + dz))
            for dy in (0, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if all(passable(get(n[0], n[1] + k, n[2])) for k in range(0, -dy + 2)):
                    nexts.append(n)
                    if standable(n):
                        break
                else:
                    break
        for n in nexts:
            if n not in seen and standable(n):
                seen.add(n)
                todo.append(n)
    return seen


def check(hut):
    s, get = hut.s, hut.get
    problems = []
    # 1. walking: from the foot of the stairs up to the deck, into the hut, up the ladder into the loft, to the jetty
    start = (STAIRS[1], G + 1, PZ1 + (P - G) + 2)
    reach = walk(hut, start)
    targets = {"the porch": (CX, P + 1, PZ1 - 2), "the kitchen": (CX, P + 1, CZ + 3), "the storeroom": (CX, P + 1, LOFT_EDGE - 2),
               "the loft": (CX + 1, LOFT_Y + 1, CZ - 3), "the jetty end": (8, G + 2, JETTY_Z[0]), "the back deck": (CX, P + 1, PZ0 + 1),
               "the borrelplas": (BORREL[0] - 4, G + 1, BORREL[1])}
    for name, p in targets.items():
        if p not in reach:
            problems.append(f"{name} {p} can't be reached on foot ({get(*p)} / below {get(p[0], p[1] - 1, p[2])})")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b in ("minecraft:chest", "minecraft:barrel") and not any((x + dx, y + dy, z + dz) in reach
                                                                     for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) for dy in (0, -1)):
            problems.append(f"{b} at {(x, y, z)} can't be reached")
    # 2. the creatures stand on something, the Moerasheks is inside her hut, there's one chest with her loot
    for (ex, ey, ez, nbt) in s.entities:
        p = (int(math.floor(ex)), int(round(ey)), int(math.floor(ez)))
        below = get(p[0], p[1] - 1, p[2])
        if not passable(get(*p)) or not (solid(below) or below == "minecraft:water"):
            problems.append(f"{nbt['id']} at {p} isn't standing free on solid ground ({get(*p)} on {below})")
    heks = [e for e in s.entities if e[3]["id"] == "guhs:moerasheks_mika"]
    if len(heks) != 1 or (int(heks[0][0]), int(heks[0][1]), int(heks[0][2])) not in hut.head_inner:
        problems.append("the Moerasheks-Mika should be in her hut (exactly one)")
    chests = [b for b in s.blocks.values() if b[0] == "minecraft:chest"]
    if len(chests) != 1 or chests[0][2].get("LootTable") != f"guhs:chests/{NAME}":
        problems.append(f"the hut should have one chest with its loot ({len(chests)})")
    # 3. the water stays in: every water cell sits on something and has no open side
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b != "minecraft:water" and props.get("waterlogged") != "true":
            continue
        below = get(x, y - 1, z)
        if below in (None, "minecraft:air") and y > 0:
            problems.append(f"water at {(x, y, z)} over {below}")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = get(x + dx, y, z + dz)
            if not (0 <= x + dx < W and 0 <= z + dz < DP):
                problems.append(f"water at the edge {(x, y, z)}")
            elif n in (None, "minecraft:air") or n and any(w in n for w in ("kaasriet", "moerasgras", "carpet", "sign")):
                problems.append(f"water leaks at {(x, y, z)} towards {n}")
    # 4. things that need support have it
    for (x, y, z), (b, props, _) in s.blocks.items():
        below, above = get(x, y - 1, z), get(x, y + 1, z)
        if b in ("minecraft:lily_pad", "guhs:guh_waterlelie") and below != "minecraft:water":
            problems.append(f"lily pad at {(x, y, z)} on {below}")
        if b == "guhs:kaasriet" and props.get("half") == "lower" and below not in ("guhs:kaasmodder", "guhs:modderig_kaasgras"):
            problems.append(f"kaasriet at {(x, y, z)} on {below}")
        if b == "guhs:kaasriet" and props.get("half") == "lower" and above != "guhs:kaasriet":
            problems.append(f"kaasriet at {(x, y, z)} without a top")
        if b == "guhs:moerasgras" and below not in ("guhs:kaasmodder", "guhs:modderig_kaasgras"):
            problems.append(f"moerasgras at {(x, y, z)} on {below}")
        if b in ("minecraft:lantern", "minecraft:soul_lantern") or b.startswith("guhs:lampion"):
            if props.get("hanging") == "true":
                if not (solid(above) or above == "minecraft:chain"):
                    problems.append(f"hanging {b} at {(x, y, z)} hangs from {above}")
            elif not (solid(below) or (below or "").endswith("_fence")):
                problems.append(f"{b} at {(x, y, z)} stands on {below}")
        if b == "minecraft:hanging_roots" and not solid(above):
            problems.append(f"hanging roots at {(x, y, z)} under {above}")
        if "candle" in b and not solid(below):
            problems.append(f"candle at {(x, y, z)} on {below}")
        if b.endswith("_sign") and "wall" not in b and not (solid(below) or (below or "").endswith("_fence")):
            problems.append(f"sign at {(x, y, z)} on {below}")
        if b == "minecraft:ladder":
            bx, bz = {"south": (0, -1), "north": (0, 1), "east": (-1, 0), "west": (1, 0)}[props["facing"]]
            if not solid(get(x + bx, y, z + bz)):
                problems.append(f"ladder at {(x, y, z)} has no wall")
        if b.endswith("_door") and props.get("half") == "lower" and (not solid(below) or not (above or "").endswith("_door")):
            problems.append(f"door at {(x, y, z)} is broken")
        if b.endswith("_banner") and not (solid(below) or (below or "").endswith("_fence")):
            problems.append(f"banner at {(x, y, z)} on {below}")
    # 5. nothing floats: every group of blocks touches the ground
    real = {p for p, (b, _, _) in s.blocks.items() if b not in ("minecraft:air",)}
    seen = set()
    for p in real:
        if p in seen:
            continue
        comp, todo = [], [p]
        seen.add(p)
        while todo:
            q = todo.pop()
            comp.append(q)
            x, y, z = q
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if n in real and n not in seen:
                    seen.add(n)
                    todo.append(n)
        if all(y > G - 3 for (_, y, _) in comp):
            problems.append(f"floating blocks: {len(comp)} at {sorted(comp)[:3]}...")
    # 6. no dark corners in the hut (no monsters spawning in the Moerasheks' kitchen)
    light = light_map(hut)
    dark = [p for p in reach if p in hut.head_inner and light.get(p, 0) < 6]
    if dark:
        problems.append(f"{len(dark)} dark spots in the hut, e.g. {sorted(dark)[:4]}")
    # 7. the door opens onto the porch
    dx, dy, dz = hut.door
    if not (passable(get(dx, dy, dz + 1)) and solid(get(dx, dy - 1, dz + 1))):
        problems.append("the door doesn't open onto the porch")
    hut.reach = reach
    return problems


def structure(h):
    h.TEMPLATE_SIZES[NAME] = 24             # (the anchor is in the middle: the flatness check samples +-24)
    h.FLATNESS[NAME] = 8
    h.structure(NAME, [BIOME], spacing=18, separation=6, salt=20270404, start_y=-G, reach=40,
                centre="guhs:moerasheks_hut_midden", spawn_overrides={"monster": {"bounding_box": "piece", "spawns": [
                    {"type": "guhs:moerasheks_mika", "weight": 1, "minCount": 1, "maxCount": 1}]}})
    hut = build_hut(h)
    problems = check(hut)
    if problems:
        raise SystemExit("moerasheks_hut geometry check failed:\n  " + "\n  ".join(problems[:40]))
    print(f"moerasheks_hut: geometry check ok ({len(hut.reach)} walkable spots, {len(hut.s.blocks)} blocks)")
    hut.s.save(NAME)
    # a flat bit of kaasmoeras for the GameTests of the pools (KaasmoerasGameTests)
    t = h.Structure((26, 8, 26))
    for x in range(26):
        for z in range(26):
            for y in range(2):
                t.set(x, y, z, "guhs:kaasmodder")
            t.set(x, 2, z, "guhs:modderig_kaasgras")
    t.save("kaasmoeras_poeltest")


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    h.ms = __import__("make_structures")
    blocks(h)
    items(h)
    loot(h)
    creatures(h)
    worldgen(h)
    advancements(h)
    texts(h)
    structure(h)
    selfcheck_assets(h)
