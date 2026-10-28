"""
1.8.0 Vadskracht (design: MineScapeStory/ontwerp-guhs180-vadskracht.md V4-V6; Java: feature/raket): the Kaas-ruimtevaart.

  lanceerplatform     the launch pad: hand in romp, motor, brandstof, bemanning (RaketStappen); the rocket builds itself above
                      it (RaketBouw); the launch scene raket_lancering; afterwards a click flies you to the Kaasmaan
  terugkeercapsule    the moon base's way home (unbreakable)
  kaasmaankristal_erts + kaasmaankristal   the moon's rare ore (in the craters round the base)
  maanvlaggetje       a little guh flag to plant on the moon (or buy from astronaut Kaasje)
  astronautenpakje / astronautenhelm   guh clothes for the crew (suit + circuit helmet bones, our own textures)
  the dimension guhs:kaasmaan (flat cheese: bedrock 1, kaaskorst 50, gatenkaas 4; the Guheinde's sky box), its biome
  advancements quest/raket_*, quest/kaasmaan_* (the FTB quests' tasks), recipes, loot, the FTB section "De Kaasraket"
  in guhs_techniek (FTB_SECTIES, ftb).

From the project root:  PYTHONPATH=tools python tools/features/raket.py
English: tools/lang/en/c217_raket.json (the FTB quests: c217_raket_ftb.json).
"""
import json
import os

import numpy as np
from PIL import Image, ImageDraw

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
EN_OVERLAY = os.path.join("tools", "lang", "en", "c217_raket.json")
EN_FTB = os.path.join("tools", "lang", "en", "c217_raket_ftb.json")
rng = np.random.default_rng(18_0217)

TEKSTEN = {}  # key -> (nl, en)


def t(key, nl, en):
    TEKSTEN[key] = (nl, en)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def save(img, *deel):
    p = os.path.join(A, "textures", *deel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# =====================================================================================================================
# textures
# =====================================================================================================================
WIT, WIT_D, ROZE, ROZE_D = (246, 244, 250), (206, 204, 214), (248, 150, 196), (210, 104, 156)
GEEL, GEEL_D, GEEL_L = (255, 214, 92), (226, 168, 52), (255, 238, 160)
BLAUW, NACHT, METAAL, METAAL_D = (150, 214, 248), (40, 36, 70), (190, 190, 204), (130, 130, 146)


def stof(kleur, ruis=7, size=16):
    a = np.zeros((size, size, 4), np.int32)
    a[..., :3] = kleur
    a[..., :3] += rng.integers(-ruis, ruis + 1, (size, size, 1))
    a[..., 3] = 255
    return a


def img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def platform_top():
    a = stof(WIT)
    for y in range(16):
        for x in range(16):
            if ((x // 4) + (y // 4)) % 2 == 0:
                a[y, x, :3] = ROZE
    im = img(a)
    d = ImageDraw.Draw(im)
    d.ellipse([3, 3, 12, 12], outline=GEEL + (255,))
    d.ellipse([6, 6, 9, 9], fill=GEEL + (255,))
    d.rectangle([0, 0, 15, 15], outline=ROZE_D + (255,))
    return im


def platform_zij():
    a = stof(METAAL, 5)
    a[0:2, :, :3] = ROZE
    a[14:16, :, :3] = ROZE_D
    for x in range(0, 16, 4):
        a[2:14, x, :3] = METAAL_D
    im = img(a)
    d = ImageDraw.Draw(im)
    d.point([(5, 7), (10, 7)], fill=NACHT + (255,))           # a little guh face on the side
    d.point([(4, 9), (11, 9)], fill=ROZE + (255,))
    d.line([6, 10, 9, 10], fill=NACHT + (255,))
    return im


def capsule():
    a = stof(WIT, 4)
    a[0:2, :, :3] = ROZE
    im = img(a)
    d = ImageDraw.Draw(im)
    d.ellipse([4, 4, 11, 11], fill=BLAUW + (255,), outline=METAAL_D + (255,))
    d.point([(6, 6), (7, 6)], fill=(255, 255, 255, 255))
    return im


def erts():
    a = stof(GEEL, 10)
    for _ in range(9):                                        # cheese holes
        x, y = rng.integers(1, 14, 2)
        a[y:y + 2, x:x + 2, :3] = GEEL_D
    for (x, y) in ((4, 5), (10, 3), (7, 10), (12, 11), (3, 12)):
        a[y - 1:y + 2, x, :3] = (190, 236, 255)
        a[y, x - 1:x + 2, :3] = (190, 236, 255)
        a[y, x, :3] = (255, 255, 255)
    return img(a)


def kristal():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (13, 6), (11, 14), (5, 14), (3, 6)], fill=(176, 228, 255, 255), outline=(90, 150, 210, 255))
    d.polygon([(8, 1), (10, 6), (8, 14), (6, 6)], fill=(226, 246, 255, 255))
    d.point([(7, 4), (9, 9)], fill=GEEL + (255,))
    return im


def vlag():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([4, 1, 4, 15], fill=METAAL_D + (255,))
    d.rectangle([5, 2, 13, 8], fill=ROZE + (255,), outline=ROZE_D + (255,))
    d.point([(7, 4), (11, 4)], fill=NACHT + (255,))           # the guh on the flag
    d.line([8, 6, 10, 6], fill=NACHT + (255,))
    d.point([(6, 2), (12, 2)], fill=WIT + (255,))              # its ears
    return im


def vlag_doek():
    a = stof(ROZE, 5)
    im = img(a)
    d = ImageDraw.Draw(im)
    d.ellipse([4, 3, 11, 10], fill=WIT + (255,))
    d.point([(6, 6), (9, 6)], fill=NACHT + (255,))
    d.line([7, 8, 8, 8], fill=NACHT + (255,))
    d.point([(4, 2), (11, 2)], fill=WIT + (255,))
    return im


# --- the guh clothes: one 8x8 swatch per bone on the 128x128 clothes uv, textures 512x512 (32x32 per swatch) ----------
def sw_pak():
    a = stof(WIT, 6, 32)
    a[:, 0:3, :3] = WIT_D
    a[20:26, 4:12, :3] = GEEL                               # a cheese patch
    a[21:23, 6:8, :3] = GEEL_D
    a[6:14, 18:28, :3] = ROZE                               # a little pink guh flag on the chest
    a[8:10, 20:22, :3] = NACHT
    a[8:10, 24:26, :3] = NACHT
    return a


def sw_helm():
    a = stof(WIT, 5, 32)
    a[26:32, :, :3] = ROZE
    return a


def sw_vizier():
    a = stof(BLAUW, 6, 32)
    a[4:8, 4:12, :3] = (230, 246, 255)
    return a


def sw_streep():
    return stof(ROZE, 5, 32)


KLEDING = {  # id -> [(bone prefix, swatch)]
    "astronautenpakje": [("outfit_suit", sw_pak)],
    "astronautenhelm": [("outfit_circuit_helm_vizier", sw_vizier), ("outfit_circuit_helm_streep", sw_streep), ("outfit_circuit_helm", sw_helm)],
}


def kleding_textures():
    geo = json.load(open(os.path.join(A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    bones = geo["minecraft:geometry"][0]["bones"]
    for naam, delen in KLEDING.items():
        a = np.zeros((512, 512, 4), np.int32)
        for b in bones:
            schilder = next((s for (pre, s) in delen if b["name"].startswith(pre)), None)
            if schilder is None:
                continue
            for c in b.get("cubes", []):
                uv = c["uv"]["north"]["uv"] if isinstance(c.get("uv"), dict) else c["uv"]
                u, v = int(uv[0]) * 4, int(uv[1]) * 4
                if a[v:v + 32, u:u + 32, 3].max() == 0:
                    a[v:v + 32, u:u + 32] = schilder()
        save(img(a), "entity", "guh_clothes", f"{naam}.png")


ICONEN = {
    "astronautenpakje": ["................", "....wwwwwwww....", "...wwwwwwwwww...", "..wwwwrrwwwwww..", "..wwwrnrnwwwww..",
                         "..wwwwrrwwwwww..", "..wwwwwwwwwwww..", "..wwyyyywwwwww..", "..wwyGyywwwwww..", "..wwwwwwwwwwww..",
                         "..ww.wwwwww.ww..", "..rr.rr..rr.rr.."],
    "astronautenhelm": ["................", ".....wwwwww.....", "...wwwwwwwwww...", "..wwwbbbbbbwww..", "..wwbbBbbbbbww..",
                        "..wwbbbbbbbbww..", "..wwbbbbbbbbww..", "..wwwbbbbbbwww..", "...wwwwwwwwww...", "...rrrrrrrrrr...",
                        "................"],
}
ICOON_KLEUR = {"w": WIT, "r": ROZE, "n": NACHT, "y": GEEL, "G": GEEL_D, "b": BLAUW, "B": (236, 248, 255)}


def iconen():
    for naam, rijen in ICONEN.items():
        ic = np.zeros((16, 16, 4), np.uint8)
        top = (16 - len(rijen)) // 2
        for y, rij in enumerate(rijen):
            for x, ch in enumerate(rij[:16]):
                if ch in ICOON_KLEUR:
                    ic[top + y, x, :3] = ICOON_KLEUR[ch]
                    ic[top + y, x, 3] = 255
        Image.fromarray(ic).save(os.path.join(A, "textures", "item", f"{naam}.png"))
        item_generated(naam)


def item_generated(naam):
    w(os.path.join(A, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{naam}"}})
    w(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


# =====================================================================================================================
# blocks, items, recipes, loot, tags
# =====================================================================================================================
def blok_item(naam):
    w(os.path.join(A, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:block/{naam}"}})


def loot_zelf(naam):
    w(os.path.join(D, "loot_table", "blocks", f"{naam}.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def blokken():
    save(platform_top(), "block", "lanceerplatform_top.png")
    save(platform_zij(), "block", "lanceerplatform_zij.png")
    w(os.path.join(A, "blockstates", "lanceerplatform.json"), {"variants": {"": {"model": "guhs:block/lanceerplatform"}}})
    w(os.path.join(A, "models", "block", "lanceerplatform.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/lanceerplatform_top", "bottom": "guhs:block/lanceerplatform_zij", "side": "guhs:block/lanceerplatform_zij"}})
    blok_item("lanceerplatform")
    loot_zelf("lanceerplatform")

    save(capsule(), "block", "terugkeercapsule.png")
    w(os.path.join(A, "blockstates", "terugkeercapsule.json"), {"variants": {"": {"model": "guhs:block/terugkeercapsule"}}})
    w(os.path.join(A, "models", "block", "terugkeercapsule.json"), {
        "parent": "minecraft:block/block", "textures": {"all": "guhs:block/terugkeercapsule", "particle": "guhs:block/terugkeercapsule"},
        "elements": [{"from": [2, 0, 2], "to": [14, 13, 14], "faces": {f: {"texture": "#all"} for f in
                                                                         ("north", "south", "east", "west", "up", "down")}},
                     {"from": [4, 13, 4], "to": [12, 15, 12], "faces": {f: {"texture": "#all"} for f in
                                                                          ("north", "south", "east", "west", "up")}}]})
    blok_item("terugkeercapsule")

    save(erts(), "block", "kaasmaankristal_erts.png")
    w(os.path.join(A, "blockstates", "kaasmaankristal_erts.json"), {"variants": {"": {"model": "guhs:block/kaasmaankristal_erts"}}})
    w(os.path.join(A, "models", "block", "kaasmaankristal_erts.json"), {"parent": "minecraft:block/cube_all",
                                                                        "textures": {"all": "guhs:block/kaasmaankristal_erts"}})
    blok_item("kaasmaankristal_erts")
    w(os.path.join(D, "loot_table", "blocks", "kaasmaankristal_erts.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:kaasmaankristal_erts", "conditions": [{"condition": "minecraft:match_tool",
             "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch",
                                                                      "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": "guhs:kaasmaankristal", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})

    save(kristal(), "item", "kaasmaankristal.png")
    item_generated("kaasmaankristal")

    save(vlag(), "item", "maanvlaggetje.png")
    save(vlag_doek(), "block", "maanvlaggetje_doek.png")
    w(os.path.join(A, "models", "block", "maanvlaggetje.json"), {
        "parent": "minecraft:block/block", "textures": {"paal": "minecraft:block/iron_block", "doek": "guhs:block/maanvlaggetje_doek",
                                                        "particle": "guhs:block/maanvlaggetje_doek"},
        "elements": [{"from": [7.5, 0, 7.5], "to": [8.5, 16, 8.5], "faces": {f: {"texture": "#paal"} for f in
                                                                             ("north", "south", "east", "west", "up", "down")}},
                     {"from": [8.5, 9, 7.75], "to": [16, 15, 8.25], "faces": {
                         "north": {"uv": [0, 2, 16, 14], "texture": "#doek"}, "south": {"uv": [16, 2, 0, 14], "texture": "#doek"},
                         "up": {"uv": [0, 0, 16, 1], "texture": "#doek"}, "down": {"uv": [0, 0, 16, 1], "texture": "#doek"},
                         "east": {"uv": [0, 0, 1, 12], "texture": "#doek"}}}]})
    w(os.path.join(A, "blockstates", "maanvlaggetje.json"), {"variants": {
        f"facing={f}": {"model": "guhs:block/maanvlaggetje", **({"y": r} if r else {})}
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(os.path.join(A, "items", "maanvlaggetje.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/maanvlaggetje"}})
    w(os.path.join(A, "models", "item", "maanvlaggetje.json"), {"parent": "minecraft:item/generated",
                                                                 "textures": {"layer0": "guhs:item/maanvlaggetje"}})
    loot_zelf("maanvlaggetje")


def recepten():
    w(os.path.join(D, "recipe", "lanceerplatform.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IKI", "KBK", "IKI"],
        "key": {"I": "minecraft:iron_block", "K": "guhs:knuffelsteen", "B": "guhs:knabbelbatterij"},
        "result": {"id": "guhs:lanceerplatform", "count": 1}})
    w(os.path.join(D, "recipe", "astronautenpakje.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["W W", "WGW", "WWW"],
        "key": {"W": "minecraft:white_wool", "G": "minecraft:glass_pane"},
        "result": {"id": "guhs:astronautenpakje", "count": 1}})
    w(os.path.join(D, "recipe", "astronautenhelm.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["WWW", "WGW"],
        "key": {"W": "minecraft:white_wool", "G": "minecraft:light_blue_stained_glass_pane"},
        "result": {"id": "guhs:astronautenhelm", "count": 1}})
    w(os.path.join(D, "recipe", "maanvlaggetje.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SP", "S ", "S "],
        "key": {"S": "minecraft:iron_ingot", "P": "minecraft:pink_wool"},
        "result": {"id": "guhs:maanvlaggetje", "count": 1}})


def tags():
    p = os.path.join(R, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
    d = json.load(open(p, encoding="utf-8"))
    for b in ("guhs:lanceerplatform", "guhs:kaasmaankristal_erts"):
        if b not in d["values"]:
            d["values"].append(b)
    w(p, d)


def advancements():
    for naam in ("raket_romp", "raket_motor", "raket_brandstof", "raket_bemanning", "raket_gelanceerd", "kaasmaan_bezocht",
                 "kaasmaan_ruil", "kaasmaan_vlag"):
        w(os.path.join(D, "advancement", "quest", f"{naam}.json"), {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# the dimension guhs:kaasmaan
# =====================================================================================================================
def dimensie():
    w(os.path.join(D, "dimension", "kaasmaan.json"), {"type": "guhs:kaasmaan", "generator": {"type": "minecraft:flat", "settings": {
        "biome": "guhs:kaasmaan", "lakes": False, "features": False, "structure_overrides": [],
        "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "guhs:kaaskorst", "height": 50},
                   {"block": "guhs:gatenkaas", "height": 4}]}}})
    muziek = {"sound": "guhs:hemel.muziek", "min_delay": 1200, "max_delay": 3600, "replace_current_music": True}
    w(os.path.join(D, "dimension_type", "kaasmaan.json"), {
        "attributes": {
            "minecraft:audio/background_music": {"default": muziek, "creative": muziek},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#3a3448",
            "minecraft:visual/fog_color": "#2a2438",
            "minecraft:visual/sky_color": "#05030c",
            "neoforge:custom_skybox": "guhs:guheinde"},
        "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False, "coordinate_scale": 1.0, "ambient_light": 0.35,
        "min_y": 0, "height": 256, "logical_height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "has_fixed_time": True,
        "timelines": "#minecraft:universal", "skybox": "end"})       # (the Guheinde's sky box draws on the End's box)
    w(os.path.join(D, "worldgen", "biome", "kaasmaan.json"), {
        "attributes": {"minecraft:visual/fog_color": "#2a2438", "minecraft:visual/sky_color": "#05030c",
                       "minecraft:visual/water_fog_color": "#ffd860"},
        "has_precipitation": False, "temperature": 0.5, "downfall": 0.0,
        "effects": {"water_color": "#ffd860", "foliage_color": "#f2d070", "grass_color": "#f2d070"},
        "spawners": {}, "spawn_costs": {}, "carvers": [], "features": []})


# =====================================================================================================================
# texts
# =====================================================================================================================
G = "gui.guhs.raket."
K = "gui.guhs.kaasmaan."
S = "scene.guhs.raket_lancering."


def teksten():
    t("block.guhs.lanceerplatform", "Lanceerplatform", "Launch Pad")
    t("block.guhs.lanceerplatform.lore", "Bouw hier je eigen Kaasraket, in vier stappen. Klik erop met de spullen bij je; sluip-klik: "
      "wat er nog nodig is.", "Build your own Cheese Rocket here, in four steps. Click it with the things on you; sneak-click: what "
      "is still needed.")
    t("block.guhs.terugkeercapsule", "Terugkeercapsule", "Return Capsule")
    t("block.guhs.terugkeercapsule.lore", "Klik erop en je bent zo weer thuis. Pak je kaasmaankristallen in!",
      "Click it and you're home in a jiffy. Pack your cheese moon crystals!")
    t("block.guhs.kaasmaankristal_erts", "Kaasmaankristal-erts", "Cheese Moon Crystal Ore")
    t("block.guhs.kaasmaankristal_erts.lore", "Zit alleen in de kraters van de Kaasmaan. Glinstert als een guh die een knabbel ziet.",
      "Only in the craters of the Cheese Moon. Sparkles like a guh that spots a nibble.")
    t("item.guhs.kaasmaankristal", "Kaasmaankristal", "Cheese Moon Crystal")
    t("item.guhs.kaasmaankristal.lore", "Astronaut Kaasje ruilt ze graag voor souvenirs.", "Astronaut Kaasje gladly swaps them for souvenirs.")
    t("block.guhs.maanvlaggetje", "Maanvlaggetje", "Little Moon Flag")
    t("block.guhs.maanvlaggetje.lore", "Plant hem op de Kaasmaan: één kleine stap voor een guh, één grote knabbel voor guhkind.",
      "Plant it on the Cheese Moon: one small step for a guh, one giant nibble for guhkind.")
    t("biome.guhs.kaasmaan", "De Kaasmaan", "The Cheese Moon")
    t("item.guhs.astronautenpakje", "Astronautenpakje", "Astronaut Suit")
    t("item.guhs.astronautenhelm", "Astronautenhelm", "Astronaut Helmet")
    t("gui.guhs.kledingbron.raket", "De Kaasraket (zelf maken)", "The Cheese Rocket (craft it)")
    t("gui.guhs.kleding.prijs.hint.raket", "Zelf maken: wol en glas", "Craft it: wool and glass")

    t(G + "stap.romp", "De romp", "The hull")
    t(G + "stap.motor", "De motor", "The engine")
    t(G + "stap.brandstof", "De brandstof", "The fuel")
    t(G + "stap.bemanning", "De bemanning", "The crew")
    t(G + "stap.kop", "Kaasraket, stap %s van %s: %s", "Cheese Rocket, step %s of %s: %s")
    t(G + "stap.klaar", "%s is klaar! De raket groeit, njeg!", "%s is done! The rocket grows, njeg!")
    t(G + "volgende", "Volgende stap: %s. Sluip-klik op het platform om te zien wat er nodig is.",
      "Next step: %s. Sneak-click the pad to see what's needed.")
    t(G + "erin", "%s spulletjes in de raket gestopt.", "Put %s things into the rocket.")
    t(G + "bemanning.uitleg", "Neem %s van je eigen guhs in een astronautenpakje mee naar het platform en klik erop: dan stappen ze in.",
      "Bring %s of your own guhs in an astronaut suit to the pad and click it: then they climb in.")
    t(G + "bemanning.nodig", "Er staan %s astronauten-guhs klaar, er zijn er %s nodig. (Eigen guhs, in een astronautenpakje, dichtbij.)",
      "%s astronaut guhs are ready, %s are needed. (Your own guhs, in an astronaut suit, close by.)")
    t(G + "klaar_voor_lancering", "De Kaasraket is klaar! Klik op het platform: 5... 4... 3...",
      "The Cheese Rocket is ready! Click the pad: 5... 4... 3...")
    t(G + "naar_maan", "Klik op het platform en de raket brengt je naar de Kaasmaan.", "Click the pad and the rocket takes you to the Cheese Moon.")
    t(G + "gelanceerd", "Lancering geslaagd! Volgende halte: de Kaasmaan.", "Launch successful! Next stop: the Cheese Moon.")
    t(G + "cmd.geen", "Geen Lanceerplatform binnen 16 blokken.", "No Launch Pad within 16 blocks.")
    t(G + "cmd.stap", "Het platform staat nu op %s stappen klaar.", "The pad now has %s steps done.")
    t(G + "cmd.spullen", "Alle raketspullen zitten in je tas.", "All rocket things are in your bag.")
    t(G + "cmd.bemanning", "Drie astronauten-guhs staan voor je klaar.", "Three astronaut guhs are ready for you.")
    t(G + "cmd.reset", "Het platform is weer leeg.", "The pad is empty again.")

    t(K + "aankomst", "Welkom op de Kaasmaan! Het ruikt hier... heel erg naar kaas. Spring maar eens, alles is hier lichter.",
      "Welcome to the Cheese Moon! It smells... very much of cheese here. Go on, jump: everything is lighter here.")
    t(K + "terug", "De capsule zet je zachtjes thuis neer. Tot de volgende keer, Kaasmaan!",
      "The capsule sets you down gently at home. See you next time, Cheese Moon!")
    t(K + "kaasje", "Astronaut Kaasje", "Astronaut Kaasje")
    t(K + "kaasje.praat.0", "Kaasje: \"Hallo aardling! Of... guhling? Pas op voor de kraters, daar zitten de mooiste kristallen.\"",
      "Kaasje: \"Hello earthling! Or... guhling? Mind the craters, that's where the prettiest crystals are.\"")
    t(K + "kaasje.praat.1", "Kaasje: \"Ik woon hier al heel lang. De knabbels zijn hier gratis, je hoeft alleen maar te happen.\"",
      "Kaasje: \"I've lived here a long time. The nibbles are free here, you just have to take a bite.\"")
    t(K + "kaasje.praat.2", "Kaasje: \"Vahoeg, een bezoeker! Heb je kaasmaankristallen voor me? Dan heb ik souvenirs voor jou.\"",
      "Kaasje: \"Vahoeg, a visitor! Got cheese moon crystals for me? Then I've got souvenirs for you.\"")
    t(K + "kaasje.ruil", "Ruilen: klik met kaasmaankristallen in je hand. %s = maanvlaggetje, %s = maansteen van kaas, %s = mini-raket. "
      "(Sluip-klik: altijd het vlaggetje.)",
      "Swapping: click with cheese moon crystals in your hand. %s = little moon flag, %s = cheese moon rock, %s = mini rocket. "
      "(Sneak-click: always the flag.)")
    t(K + "kaasje.gekocht", "Kaasje geeft je: %s. \"Goede reis, en vergeet niet te zwaaien!\"",
      "Kaasje gives you: %s. \"Safe travels, and don't forget to wave!\"")
    t(K + "vlag", "Je hebt een vlaggetje op de Kaasmaan geplant! Eén kleine stap voor een guh...",
      "You planted a flag on the Cheese Moon! One small step for a guh...")

    t(S + "titel", "De lancering van de Kaasraket", "The launch of the Cheese Rocket")
    t(S + "klaar", "Alle guhs aan boord? Riemen vast, oortjes plat!", "All guhs aboard? Seatbelts on, ears down!")
    t(S + "aftel5", "Vijf...", "Five...")
    t(S + "aftel4", "Vier...", "Four...")
    t(S + "aftel3", "Drie...", "Three...")
    t(S + "aftel2", "Twee...", "Two...")
    t(S + "aftel1", "Eén...", "One...")
    t(S + "vuur", "VAHOEG! Motoren aan!", "VAHOEG! Engines on!")
    t(S + "weg", "Daar gaat ze, recht naar de Kaasmaan!", "There she goes, straight to the Cheese Moon!")

    for naam, (nl_t, en_t, nl_d, en_d) in {
        "raket_romp": ("Een romp met oortjes", "A hull with ears", "Lever de romp in", "Hand in the hull"),
        "raket_motor": ("Brommm", "Vroom", "Lever de motor in", "Hand in the engine"),
        "raket_brandstof": ("Volgetankt", "Tanked up", "Lever de brandstof in", "Hand in the fuel"),
        "raket_bemanning": ("Bemanning aan boord", "Crew aboard", "Drie astronauten-guhs stappen in", "Three astronaut guhs climb in"),
        "raket_gelanceerd": ("Lancering!", "Liftoff!", "Lanceer de Kaasraket", "Launch the Cheese Rocket"),
        "kaasmaan_bezocht": ("Op de Kaasmaan", "On the Cheese Moon", "Zet voet op de Kaasmaan", "Set foot on the Cheese Moon"),
        "kaasmaan_ruil": ("Souvenirjager", "Souvenir hunter", "Ruil kristallen bij Kaasje", "Swap crystals with Kaasje"),
        "kaasmaan_vlag": ("Eén kleine stap", "One small step", "Plant een maanvlaggetje", "Plant a little moon flag"),
    }.items():
        t(f"advancements.guhs.quest.{naam}.title", nl_t, en_t)
        t(f"advancements.guhs.quest.{naam}.description", nl_d, en_d)


def blokuitleg():
    """The wiki's "Zo werkt het" entries (tools/wiki_site/blokuitleg.py): appended once, in a marked block."""
    p = os.path.join("tools", "wiki_site", "blokuitleg.py")
    s = open(p, encoding="utf-8").read()
    if "# <raket>" in s:
        return
    s += '''
# <raket> 1.8.0 Vadskracht: the Kaasraket (tools/features/raket.py)
UITLEG["lanceerplatform"] = dict(
    nl="Het platform van de Kaasraket. Bouw de raket in vier stappen: de romp, de motor, de brandstof en de bemanning.\\n"
       "\\n"
       "- Klik erop met de spullen bij je: alles van de huidige stap gaat erin, ook in porties. Sluip-klik: wat er nog nodig is.\\n"
       "- Bij elke klare stap bouwt de raket zichzelf een stukje verder boven het platform. De volgende stap twinkelt als spook.\\n"
       "- De bemanning: drie van je eigen guhs in een astronautenpakje, dichtbij. Ze stappen in de cockpit.\\n"
       "- Alles klaar: klik voor de lancering. Daarna brengt een klik je naar de Kaasmaan.",
    en="The pad of the Cheese Rocket. Build the rocket in four steps: the hull, the engine, the fuel and the crew.\\n"
       "\\n"
       "- Click it with the things on you: everything of the current step goes in, in portions too. Sneak-click: what's still needed.\\n"
       "- With every finished step the rocket builds itself a bit further above the pad. The next step twinkles as a ghost.\\n"
       "- The crew: three of your own guhs in an astronaut suit, close by. They climb into the cockpit.\\n"
       "- All done: click for the launch. After that a click takes you to the Cheese Moon.",
    meer=[])
UITLEG["terugkeercapsule"] = dict(
    nl="Staat in de maanbasis op de Kaasmaan. Klik erop en je gaat terug naar waar je vandaan kwam (naast je Lanceerplatform).",
    en="Stands in the moon base on the Cheese Moon. Click it and you go back to where you came from (next to your Launch Pad).",
    meer=[])
# </raket>
'''
    open(p, "w", encoding="utf-8", newline="\n").write(s)


# =====================================================================================================================
# FTB (make_ftbquests.py reads FTB_CHAPTER, FTB_SECTIES and ftb)
# =====================================================================================================================
FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("raket", "De Kaasraket", "item:guhs:lanceerplatform", None)]
FTB_EN = {}


def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    quests = [
        ("raket_platform", "Een plek om te lanceren", "Maak een &dLanceerplatform&r (ijzerblokken, knuffelsteen en een knabbelbatterij) "
         "en zet het op een open plek: de raket wordt groot!", "guhs:lanceerplatform", [item("guhs:lanceerplatform")],
         "A place to launch", "Make a &dLaunch Pad&r (iron blocks, knuffelsteen and a nibble battery) and put it somewhere open: the "
         "rocket gets big!"),
        ("raket_romp", "De romp", "Klik op het platform met &d64 knuffelsteen&r, &d24 ijzer&r en &d16 glas&r (mag in porties).",
         "guhs:knuffelsteen", [adv("raket_romp")], "The hull", "Click the pad with &d64 knuffelsteen&r, &d24 iron&r and &d16 glass&r "
         "(in portions is fine)."),
        ("raket_motor", "De motor", "Twee &dknabbelbatterijen&r, een &dGuhoven&r, &d4 ijzerblokken&r en &d16 redstone&r. Brommm!",
         "guhs:knabbelbatterij", [adv("raket_motor")], "The engine", "Two &dnibble batteries&r, a &dGuh Oven&r, &d4 iron blocks&r and "
         "&d16 redstone&r. Vroom!"),
        ("raket_brandstof", "De brandstof", "&d24 blokken kaasknabbels&r en &d4 emmers kaassaus&r. Raketten rijden op kaas, dat weet "
         "iedereen.", "guhs:block_of_kaasknabbels", [adv("raket_brandstof")], "The fuel", "&d24 blocks of cheese nibbles&r and "
         "&d4 buckets of cheese sauce&r. Rockets run on cheese, everybody knows that."),
        ("raket_bemanning", "De bemanning", "Trek drie van je guhs een &dastronautenpakje&r aan (wol en glas), neem ze mee naar het "
         "platform en klik erop.", "guhs:astronautenpakje", [adv("raket_bemanning")], "The crew", "Dress three of your guhs in an "
         "&dastronaut suit&r (wool and glass), take them to the pad and click it."),
        ("raket_gelanceerd", "Lancering!", "Klik op het platform: aftellen, motoren aan, en weg is de raket!", "guhs:lanceerplatform",
         [adv("raket_gelanceerd")], "Liftoff!", "Click the pad: countdown, engines on, and off goes the rocket!"),
        ("kaasmaan_vlag", "Eén kleine stap", "Plant een &dmaanvlaggetje&r op de Kaasmaan. Hak in de kraters &dkaasmaankristallen&r en ruil "
         "ze bij &dAstronaut Kaasje&r voor souvenirs.", "guhs:maanvlaggetje", [adv("kaasmaan_vlag")], "One small step",
         "Plant a &dlittle moon flag&r on the Cheese Moon. Chip &dcheese moon crystals&r in the craters and swap them with "
         "&dAstronaut Kaasje&r for souvenirs."),
    ]
    vorige = None
    for (key, titel, desc, icoon, taken, en_t, en_d) in quests:
        q(key, titel, desc, icoon, taken, deps=[vorige] if vorige else (), rewards=(("guhs:kaas_knabbels", 16),))
        FTB_EN[f"ftb.guhs_techniek.q.{key}.title"] = en_t
        FTB_EN[f"ftb.guhs_techniek.q.{key}.desc"] = en_d
        vorige = key
    FTB_EN["ftb.guhs_techniek.section.raket"] = "The Cheese Rocket"
    w(EN_FTB, FTB_EN)


# =====================================================================================================================
def opslaan():
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        d = json.load(open(p, encoding="utf-8"))
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        w(p, d)
    w(EN_OVERLAY, {k: en for k, (nl, en) in sorted(TEKSTEN.items())})


def build(h=None):
    kleding_textures()
    iconen()
    blokken()
    recepten()
    tags()
    advancements()
    dimensie()
    teksten()
    blokuitleg()
    if h is not None and hasattr(h, "lang"):
        for k, (nl, en) in TEKSTEN.items():
            h.lang(k, en, nl)


if __name__ == "__main__":
    build()
    opslaan()
    print(f"raket: lanceerplatform, kaasmaan, 2 clothes, {len(TEKSTEN)} texts")
