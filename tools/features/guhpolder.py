"""
De Guhpolder (2.9 "De Grote Guhspelen", slice guhpolder of the Elf-Guhjestocht team): a rare, cold, FLAT biome of the
Guhmensie where the Elf-Guhjestocht is held (the elftocht module places it on the peak of the polder noise).

  - the biome guhs:guhpolder: its own noise guhmension_polder, flattened towards the middle (guhpolder_wereld.py; the
    numbers for the Elf-Guhjestocht: PEAK_MIN, TOCHT_RING, TOCHT_RADIUS there); cold, snowing, frost glitter in the air;
    guhs spawn (a third of them become a Pinguh), never Mika's; Knabbelkelders may lie under it
  - blocks: rijpgras, rijpsprietjes, guh_ijsbloempje (light blue dye), polderijs (never melts; no snow settles on it),
    knotwilg_stam + knotwilg_bladeren (snow caps), ijspegelguh_kristal (light), guh_molentje (turning sails, a guh face:
    grinds #guhs:knus/knabbelgraan into knabbelmeel, tag guhs:knus/knabbelmeel; the knabbeloven bakes twice as much with it)
  - worldgen (Java: GuhpolderWorldgen): knotwilgen, frozen sloten with rows of knotwilgen, vennetjes, sneeuwguh-heuveltjes,
    snow patches, loose molentjes, plants and ice crystals
  - the Pinguh (GuhVariant PINGUH): bones pinguh_vleugel_* (flippers), three looks (guhpolder_tex.py: klassiek, keizer and
    the grey fluffy chick), Guhdex texts; advancements in the tab De Grote Guhspelen; FTB quests (section "De Guhpolder")
Textures: guhpolder_tex.py. Self-check: selfcheck(h) (blocks/items have their assets, the lang is complete).
"""
import os

from features import guhpolder_tex as tex
from features import guhpolder_wereld as wereld

FTB_SECTION = "De Guhpolder"
CLOTHES = []
BLOCKS = ["rijpgras", "rijpsprietjes", "guh_ijsbloempje", "polderijs", "knotwilg_stam", "knotwilg_bladeren", "ijspegelguh_kristal", "guh_molentje"]
ITEMS = ["knabbelmeel"]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# read by tools/features/elftocht.py (the Elf-Guhjestocht: one per polder, near the peak of the RAW polder noise):
# the peak at least PEAK_MIN; two peaks are in the same polder while the noise between them stays >= the biome's edge
# (TERM_FROM). The tour then goes on the best spot near the peak where its 256 x 256 square lies on dead-flat polder
# ("vlak": this router's own rule - the polder noise >= FLAT_TO, the deep sea's noise <= SEA_OFF[0], the knuffel noise
# <= KNUFFEL_OFF[0]; at least min_flat of the 7 x 7 grid points over the square), and only when the real ground there
# has at most max_ongelijk grid points under water or more than 10 lower / 12 higher (ElftochtStructure).
NOISE = wereld.NOISE
BIOME = wereld.BIOME
ELFTOCHT_PIEK = {"min_value": wereld.PEAK_MIN, "dal_value": wereld.TERM_FROM, "flat_value": wereld.TOCHT_RING, "flat_radius": 0,
                 "vlak": {"flat_value": wereld.FLAT_TO, "sea_noise": f"guhs:{wereld.kw.SEA_NOISE}", "sea_max": wereld.kw.SEA_OFF[0],
                          "knuffel_noise": f"guhs:{wereld.kw.NOISE}", "knuffel_max": wereld.KNUFFEL_OFF[0], "min_flat": 40},
                 "max_ongelijk": 2}

# the Pinguh's flippers (shown only on the Pinguh: GuhVariant PINGUH, bone prefix "pinguh")
BONES = {
    "pinguh_vleugel_links": ("body", [6.9, 9, 4], "pinguh_vleugel", [([6.5, 3, 1], [0.8, 6, 6], 0)]),
    "pinguh_vleugel_rechts": ("body", [-6.9, 9, 4], "pinguh_vleugel", [([-7.3, 3, 1], [0.8, 6, 6], 0)]),
}


def variants(rng, v):
    # (make_guh_variants writes a first guh_pinguh.png; build() then paints the real three looks over it from guh.png)
    return {"pinguh": ((34, 38, 52), {"pinguh_vleugel": lambda: v.fabric((34, 38, 52), rng, 6)})}


# =====================================================================================================================
# texts (Dutch, also in en_us)
# =====================================================================================================================
LANG = {
    "biome.guhs.guhpolder": "Guhpolder",
    "block.guhs.rijpgras": "Rijpgras",
    "block.guhs.rijpsprietjes": "Rijpsprietjes",
    "block.guhs.rijpsprietjes.lore": "Knisperende sprietjes vol rijp. Knip ze met een schaar!",
    "block.guhs.guh_ijsbloempje": "Guh-ijsbloempje",
    "block.guhs.guh_ijsbloempje.lore": "Een doorzichtig ijsblauw bloempje met een piepklein guhgezichtje. Lichtblauwe kleurstof!",
    "block.guhs.polderijs": "Polderijs",
    "block.guhs.knotwilg_stam": "Knotwilgstam",
    "block.guhs.knotwilg_bladeren": "Knotwilgtakjes",
    "block.guhs.ijspegelguh_kristal": "IJspegelguh-kristal",
    "block.guhs.ijspegelguh_kristal.lore": "Een gloeiend ijskristal in de vorm van een guhoor. Luistert het mee? Njeg!",
    "block.guhs.guh_molentje": "Guh-molentje",
    "block.guhs.guh_molentje.lore": "Maalt knabbelgraan tot knabbelmeel. Sneller als het sneeuwt, heel vahoeg als het stormt!",
    "item.guhs.knabbelmeel": "Knabbelmeel",
    "item.guhs.knabbelmeel.lore": "Vers gemalen in een guh-molentje. In de knabbeloven bak je er dubbel zoveel mee!",
    "subtitles.guhs.guhpolder.molentje_maal": "Guh-molentje maalt",
    "subtitles.guhs.guhpolder.pinguh_glij": "Pinguh glijdt: wiiiie!",
    "gui.guhs.guhpolder.molentje.gestort": "%s knabbelgraan in het molentje gestort. Draaien maar!",
    "gui.guhs.guhpolder.molentje.vol": "Het molentje zit vol, njeg! Haal eerst het knabbelmeel eruit.",
    "gui.guhs.guhpolder.molentje.meel": "%s knabbelmeel, vers gemalen! VAHOEG!",
    "gui.guhs.guhpolder.molentje.leeg": "Het molentje draait lekker rond, maar er zit geen knabbelgraan in.",
    "gui.guhs.guhpolder.molentje.maalt": "Het molentje maalt nog %s knabbelgraan... (sneller als het sneeuwt!)",
    "gui.guhs.guhpolder.meel_dubbel": "Gebakken met knabbelmeel: dubbel zoveel, VAHOEG!",
    "entity.guhs.guh.pinguh": "Pinguh",
    "gui.guhs.guhdex.rarity.pinguh": "Zeldzaamheid: Ongewoon (alleen in de Guhpolder)",
    "gui.guhs.guhdex.info.pinguh": "Een guh in een pinguinpakje! Een derde van de wilde guhs in de ijskoude Guhpolder is een Pinguh. "
                                   "Er zijn klassieke Pinguhs (zwart met een wit buikje en een oranje snaveltje), deftige Keizerpinguhs "
                                   "(een beetje groter, met gouden wangetjes) en heel soms een grijs pluizig Pinguh-kuikentje: alle "
                                   "baby's zijn zo! Waggelt vadsig rond en glijdt op zijn buik over het ijs. Tem hem met kaasknabbels, "
                                   "dan glijdt hij tijdens de Elf-Guhjestocht gezellig met je mee. VAHOEG!",
}

ADVANCEMENTS = [  # name, parent, icon, frame, title, description, criteria
    ("polder", "root", "guhs:rijpgras", "task", "Brrr, de Guhpolder!", "Vind de ijskoude, platte Guhpolder",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                            "predicate": {"location": {"biomes": "guhs:guhpolder"}}}]}}}),
    ("pinguh", "polder", "guhs:polderijs", "goal", "Een vadsige Pinguh", "Tem een Pinguh: een guh in een pinguinpakje!",
     {"done": {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                              "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"pinguh\"}"}}]}}}),
    ("knabbelmeel", "polder", "guhs:guh_molentje", "task", "Vers gemalen", "Maal knabbelgraan tot knabbelmeel in een guh-molentje",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:knabbelmeel"}]}}}),
    ("ijsbloempje", "polder", "guhs:guh_ijsbloempje", "task", "Een bloemetje van ijs", "Pluk een guh-ijsbloempje in de Guhpolder",
     {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:guh_ijsbloempje"}]}}}),
]


# =====================================================================================================================
# models, blockstates, loot, recipes, tags
# =====================================================================================================================
def el(frm, to, tex_, faces=None, uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": tex_, "uv": uv} if uv else {"texture": tex_})
                                             for f in (faces or ("down", "up", "north", "south", "west", "east"))}}


def molentje_elements():
    """The little mill (facing north): a white tower with the guh face in front, a pink cap with two guh ears, the axle."""
    body = {"from": [4, 0, 4], "to": [12, 10, 12], "faces": {
        "north": {"texture": "#voor", "uv": [0, 0, 16, 16]}, "south": {"texture": "#zijkant", "uv": [0, 0, 16, 16]},
        "east": {"texture": "#zijkant", "uv": [0, 0, 16, 16]}, "west": {"texture": "#zijkant", "uv": [0, 0, 16, 16]},
        "down": {"texture": "#hout"}}}
    return [body,
            el([3.5, 10, 3.5], [12.5, 12, 12.5], "#dak"),
            el([5, 12, 5], [11, 14, 11], "#dak"),
            el([6.5, 14, 6.5], [9.5, 15, 9.5], "#dak"),
            el([4.2, 12, 7], [5.7, 15.5, 8.5], "#oor"), el([10.3, 12, 7], [11.8, 15.5, 8.5], "#oor"),
            el([7.5, 9.5, 2.5], [8.5, 10.5, 4], "#hout")]


def wieken_elements():
    """The sails around the hub (8, 10, 2.5): two crossed arms and four cloth sails, pinwheel style."""
    cloth = ("north", "south")
    return [el([7, 9, 1.5], [9, 11, 3], "#hout"),
            el([7.5, 2, 2.1], [8.5, 18, 2.7], "#hout"),
            el([0, 9.5, 2.1], [16, 10.5, 2.7], "#hout"),
            el([8.5, 11, 2.3], [11, 17.5, 2.5], "#wiek", faces=cloth, uv=[0, 0, 16, 16]),
            el([9, 7, 2.3], [15.5, 9.5, 2.5], "#wiek", faces=cloth, uv=[0, 0, 16, 16]),
            el([5, 2.5, 2.3], [7.5, 9, 2.5], "#wiek", faces=cloth, uv=[0, 0, 16, 16]),
            el([0.5, 10.5, 2.3], [7, 13, 2.5], "#wiek", faces=cloth, uv=[0, 0, 16, 16])]


MOLEN_TEX = {"particle": "guhs:block/guh_molentje_zijkant", "voor": "guhs:block/guh_molentje_voor", "zijkant": "guhs:block/guh_molentje_zijkant",
             "dak": "guhs:block/guh_molentje_dak", "oor": "minecraft:block/pink_wool", "hout": "guhs:block/guh_molentje_hout",
             "wiek": "guhs:block/guh_molentje_wiek"}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    # rijpgras (snowy sides under snow)
    w(f"{A}/models/block/rijpgras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/rijpgras_top", "bottom": "minecraft:block/pink_wool", "side": "guhs:block/rijpgras_side"}})
    w(f"{A}/models/block/rijpgras_snowy.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "minecraft:block/snow", "bottom": "minecraft:block/pink_wool", "side": "guhs:block/rijpgras_side_snowy"}})
    w(f"{A}/blockstates/rijpgras.json", {"variants": {
        "snowy=false": [{"model": "guhs:block/rijpgras", **({"y": r} if r else {})} for r in (0, 90, 180, 270)],
        "snowy=true": {"model": "guhs:block/rijpgras_snowy"}}})
    w(f"{A}/models/item/rijpgras.json", {"parent": "guhs:block/rijpgras"})
    # crosses
    for name, rt in (("rijpsprietjes", "minecraft:cutout"), ("guh_ijsbloempje", "minecraft:translucent")):
        w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": rt, "textures": {"cross": f"guhs:block/{name}"}})
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
        h.item_model(name, f"guhs:block/{name}")
    # polderijs
    w(f"{A}/models/block/polderijs.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/polderijs"}})
    w(f"{A}/blockstates/polderijs.json", {"variants": {"": [{"model": "guhs:block/polderijs", **({"y": r} if r else {})} for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/polderijs.json", {"parent": "guhs:block/polderijs"})
    # the knotwilg
    w(f"{A}/models/block/knotwilg_stam.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "guhs:block/knotwilg_stam_top", "side": "guhs:block/knotwilg_stam"}})
    w(f"{A}/models/block/knotwilg_stam_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "end": "guhs:block/knotwilg_stam_top", "side": "guhs:block/knotwilg_stam"}})
    w(f"{A}/blockstates/knotwilg_stam.json", {"variants": {
        "axis=y": {"model": "guhs:block/knotwilg_stam"},
        "axis=z": {"model": "guhs:block/knotwilg_stam_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/knotwilg_stam_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/knotwilg_stam.json", {"parent": "guhs:block/knotwilg_stam"})
    w(f"{A}/models/block/knotwilg_bladeren.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout_mipped",
                                                    "textures": {"all": "guhs:block/knotwilg_bladeren"}})
    w(f"{A}/models/block/knotwilg_bladeren_sneeuw.json", {"parent": "minecraft:block/cube_bottom_top", "render_type": "minecraft:cutout_mipped",
                                                           "textures": {"top": "guhs:block/knotwilg_bladeren_sneeuw_top",
                                                                        "side": "guhs:block/knotwilg_bladeren_sneeuw",
                                                                        "bottom": "guhs:block/knotwilg_bladeren"}})
    w(f"{A}/blockstates/knotwilg_bladeren.json", {"variants": {"sneeuw=false": {"model": "guhs:block/knotwilg_bladeren"},
                                                               "sneeuw=true": {"model": "guhs:block/knotwilg_bladeren_sneeuw"}}})
    w(f"{A}/models/item/knotwilg_bladeren.json", {"parent": "guhs:block/knotwilg_bladeren_sneeuw"})
    # the ice crystal (like an amethyst cluster: grows on every side)
    w(f"{A}/models/block/ijspegelguh_kristal.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:translucent",
                                                      "textures": {"cross": "guhs:block/ijspegelguh_kristal"}})
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
    w(f"{A}/blockstates/ijspegelguh_kristal.json", {"variants": {f"facing={f}": {"model": "guhs:block/ijspegelguh_kristal", **r}
                                                                 for f, r in rot.items()}})
    h.item_model("ijspegelguh_kristal", "guhs:block/ijspegelguh_kristal")
    # the guh-molentje (the sails are a separate model, turned by MolentjeRenderer)
    w(f"{A}/models/block/guh_molentje.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                               "textures": MOLEN_TEX, "elements": molentje_elements()})
    w(f"{A}/models/block/guh_molentje_wieken.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                      "textures": MOLEN_TEX, "elements": wieken_elements()})
    w(f"{A}/blockstates/guh_molentje.json", {"variants": {f"facing={f}": {"model": "guhs:block/guh_molentje", **({"y": r} if r else {})}
                                                          for f, r in ROT.items()}})
    w(f"{A}/models/item/guh_molentje.json", {"parent": "minecraft:block/block", "textures": MOLEN_TEX,
                                              "elements": molentje_elements() + wieken_elements(),
                                              "display": {"gui": {"rotation": [30, 200, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}})
    h.item_model("knabbelmeel")

    # --- loot ---
    for blk in ("rijpgras", "guh_ijsbloempje", "polderijs", "knotwilg_stam", "ijspegelguh_kristal", "guh_molentje"):
        h.self_drop(blk)
    shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    w(f"{D}/loot_table/blocks/rijpsprietjes.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:rijpsprietjes", "conditions": [shears]}]}]})
    silk_or_shears = {"condition": "minecraft:any_of", "terms": [shears, {"condition": "minecraft:match_tool", "predicate": {
        "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
    w(f"{D}/loot_table/blocks/knotwilg_bladeren.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:knotwilg_bladeren", "conditions": [silk_or_shears]},
            {"type": "minecraft:item", "name": "minecraft:stick", "conditions": [{"condition": "minecraft:random_chance", "chance": 0.25}],
             "functions": h.count_fn(1, 2)}]}]}]})

    # --- recipes ---
    h.shapeless("guh_ijsbloempje_kleurstof", ["guhs:guh_ijsbloempje"], "minecraft:light_blue_dye", 1)
    h.shaped("guh_molentje", ["W W", "SKS", "PPP"], {"W": "minecraft:white_wool", "S": "minecraft:stick", "K": "guhs:kaas_knabbels",
                                                      "P": "#minecraft:planks"}, "guhs:guh_molentje", 1)
    h.shaped("polderijs", ["IS", "SI"], {"I": "minecraft:packed_ice", "S": "minecraft:snowball"}, "guhs:polderijs", 2)
    h.shapeless("knotwilg_planken", ["guhs:knotwilg_stam"], "minecraft:spruce_planks", 4)

    # --- tags ---
    add = h.add_tag
    add("minecraft/tags/block/dirt", ["guhs:rijpgras"])
    add("minecraft/tags/block/mineable/shovel", ["guhs:rijpgras"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:polderijs", "guhs:ijspegelguh_kristal"])
    add("minecraft/tags/block/mineable/axe", ["guhs:knotwilg_stam", "guhs:guh_molentje"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:knotwilg_bladeren"])
    add("minecraft/tags/block/replaceable", ["guhs:rijpsprietjes"])
    add("minecraft/tags/block/sword_efficient", ["guhs:rijpsprietjes", "guhs:guh_ijsbloempje"])
    add("minecraft/tags/block/snow_layer_cannot_survive_on", ["guhs:polderijs"])   # (the canal stays clear for skating)
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/leaves", ["guhs:knotwilg_bladeren"])
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:knotwilg_stam"])
        add(f"minecraft/tags/{kind}/small_flowers", ["guhs:guh_ijsbloempje"])
    add("guhs/tags/block/guhpolder/glijijs", ["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:frosted_ice", "guhs:polderijs"])
    add("guhs/tags/item/knus/knabbelmeel", ["guhs:knabbelmeel"])


def sounds(h):
    entries = {"guhpolder.molentje_maal": [{"name": "minecraft:block.grindstone.use", "type": "event", "volume": 0.5, "pitch": 1.2}],
               "guhpolder.pinguh_glij": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.6, "volume": 0.8}]}

    def patch(d):
        for event, snd in entries.items():
            d[event] = {"sounds": snd, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    for name, parent, icon, frame, title, desc, crit in ADVANCEMENTS:
        adv = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.guhpolder_{name}.title"},
                           "description": {"translate": f"advancements.guhs.grote_guhspelen.guhpolder_{name}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
               "criteria": crit,
               "parent": "guhs:grote_guhspelen/root" if parent == "root" else f"guhs:grote_guhspelen/guhpolder_{parent}"}
        h.w(f"{h.D}/advancement/grote_guhspelen/guhpolder_{name}.json", adv)
        h.lang(f"advancements.guhs.grote_guhspelen.guhpolder_{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.guhpolder_{name}.description", desc, desc)
    h.w(f"{h.D}/advancement/quest/seen_pinguh.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def test_templates(h):
    """The test room: a little ice rink (polderijs, the east half) next to rijpgras."""
    s = h.Structure((16, 12, 16))
    for x in range(16):
        for z in range(16):
            if x >= 8:
                s.set(x, 0, z, "guhs:polderijs")
            else:
                s.set(x, 0, z, "guhs:rijpgras", {"snowy": "false"})
    s.save("guhpolder_test_ijsbaan")


def selfcheck(h):
    problems = []
    for b in BLOCKS:
        if not os.path.exists(f"{h.A}/blockstates/{b}.json"):
            problems.append(f"block {b}: no blockstate")
        if f"block.guhs.{b}" not in h.NL or f"block.guhs.{b}" not in h.EN:
            problems.append(f"block {b}: no name")
    for i in BLOCKS + ITEMS:
        if not os.path.exists(f"{h.A}/models/item/{i}.json"):
            problems.append(f"item {i}: no item model")
    for i in ITEMS:
        if f"item.guhs.{i}" not in h.NL:
            problems.append(f"item {i}: no name")
    for t in ("entity/guh_pinguh", "entity/guh_pinguh_keizer", "entity/guh_pinguh_pluis", "item/knabbelmeel", "particle/guhpolder_glinster_0",
              "particle/guh_sneeuw_0", "particle/guh_sneeuw_3", "particle/guh_sneeuw_guhkop"):
        if not os.path.exists(f"{h.TEX}/{t}.png"):
            problems.append(f"texture {t} missing")
    if problems:
        raise SystemExit("guhpolder self-check: " + "; ".join(problems))


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    wereld.worldgen(h)
    sounds(h)
    advancements(h)
    for key, text in LANG.items():
        h.lang(key, text, text)
    test_templates(h)
    selfcheck(h)


def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("guhpolder_vind", "De Guhpolder", "Ergens in de Guhmensie ligt een ijskoude, platte &bGuhpolder&r: rijpgras, knotwilgen, bevroren "
      "sloten en sneeuwguhtjes. Hier wordt de &bElf-Guhjestocht&r geschaatst! Neem een warme sjaal mee, njeg.", "guhs:rijpgras",
      [fq.biome("guhpolder")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("guhpolder_pinguh", "Een vadsige Pinguh", "Een derde van de wilde guhs in de polder is een &bPinguh&r: klassiek, keizer of een "
      "grijs pluizig kuikentje. Tem er een met kaasknabbels. Hij glijdt op zijn buik over het ijs, en tijdens de Elf-Guhjestocht "
      "glijdt hij met je mee!", "guhs:polderijs", [adv("guhs:grote_guhspelen/guhpolder_pinguh")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=["guhpolder_vind"], xp=150)
    q("guhpolder_ijsbloempje", "IJsbloempjes", "Pluk drie &bguh-ijsbloempjes&r: doorzichtige blauwe bloempjes met een piepklein "
      "guhgezichtje. Er komt lichtblauwe kleurstof uit!", "guhs:guh_ijsbloempje", [item("guhs:guh_ijsbloempje", 3)],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["guhpolder_vind"])
    q("guhpolder_molentje", "Een guh-molentje", "Vind een &bguh-molentje&r in de polder of maak er zelf een (wol, stokjes, een "
      "kaasknabbel en planken). Zijn wieken draaien de hele dag, en nog sneller als het sneeuwt!", "guhs:guh_molentje",
      [item("guhs:guh_molentje")], rewards=(("guhs:knabbelgraan", 8),), deps=["guhpolder_vind"])
    q("guhpolder_meel", "Vers gemalen", "Stop knabbelgraan in een guh-molentje (rechtsklik) en haal er &eknabbelmeel&r uit. In de "
      "knabbeloven van de bakker bak je met knabbelmeel dubbel zoveel. VAHOEG!", "guhs:knabbelmeel", [item("guhs:knabbelmeel", 8)],
      rewards=(("guhs:kaas_knabbels", 12),), deps=["guhpolder_molentje"], xp=100)
