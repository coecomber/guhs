"""
1.7.0 "Onder de Wol", rounds 32-37 (toveren; Java: feature/toveren): the Glinsteraltaar (the guh enchanting table, model
B "Glinsteraltaar" of mijnen-ontwerp/villagers/guh_enchanttafel_voorstellen.png), its power block the Knuffelboekenkast,
the Spreukjesboekje's look and the eight guh enchantments.

  - blocks: models (the altar is a little element model: a foot and a top of guhkristalsteen, four pillars, a glowing
    crystal core with a face, four crystal tips), blockstates, loot, mineable tags, recipes;
  - the textures (16x16 pixel style; the floating book is vanilla's 64x32 book layout recoloured lilac with hearts);
  - the Spreukjesboekje: an item model definition guhs:spreukjesboekje (no item of its own: the Java side puts it on an
    enchanted book through the item_model component, see Spreukjesboekje.java);
  - the particles glinsterhartje (hearts flying from the shelves) and pluispoef (Pluisval's fluff);
  - data/guhs/enchantment/*.json and the tags: #guhs:glinsteraltaar (the altar's pool), #guhs:spreukjes/alle,
    #guhs:spreukjes/boekje (the book-only spells, for the villager trades and loot), #guhs:duurzaamheid, the block tags
    of Pluisval and Vadsvlijt, the item tag guhs:enchantable/mika_mepper (weapons minus the Mika-mepper), and a NeoForge
    "remove" on minecraft:enchantable/durability: no unbreaking/mending for the unbreakable vads gear and Mika-mepper;
  - the texts (nl_nl + en_us, merged).

Standalone and idempotent (the way the slices since 1.5 run), from the project root:
    PYTHONPATH=tools python tools/features/toveren.py
"""
import io
import json
import os
import sys
import zipfile

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from features.onderwol_tex import MC_JAR, rng, vlak, leeg, ruis, zet, img, arr, mix   # noqa: E402

ALTAAR, KAST, BOEKJE = "glinsteraltaar", "knuffelboekenkast", "spreukjesboekje"

# =====================================================================================================================
# textures
# =====================================================================================================================
HOUT, HOUT_D, HOUT_L = (226, 160, 186), (184, 112, 146), (244, 196, 214)
ACHTER = (112, 64, 100)
BOEKEN = [(150, 196, 246), (246, 140, 196), (190, 160, 246), (252, 214, 120), (140, 214, 196), (255, 246, 236), (236, 110, 170)]
PLUCHE, PLUCHE_D = (232, 206, 240), (204, 168, 220)
OOG, BLOS, MOND = (36, 52, 120), (255, 160, 200), (120, 50, 110)
KRISTAL = [(170, 70, 140), (214, 104, 176), (246, 150, 210), (255, 200, 236), (255, 238, 250)]


def _vanilla(pad):
    with zipfile.ZipFile(MC_JAR) as z:
        return arr(Image.open(io.BytesIO(z.read(pad))).convert("RGBA"))


def _ramp(f, kleuren):
    k = np.array(kleuren, np.float32)
    x = np.clip(f, 0, 1) * (len(k) - 1)
    i = np.clip(np.floor(x).astype(int), 0, len(k) - 2)
    w = (x - i)[..., None]
    return k[i] * (1 - w) + k[i + 1] * w


def _helder(t):
    return t[..., :3] @ np.array([0.3, 0.5, 0.2], np.float32) / 255.0


def _pluche_guhtje(t, x, y):
    """A little plush guh between the books: 5 wide, 6 high (ears on top), standing on the shelf at y + 5."""
    zet(t, x, y, PLUCHE)
    zet(t, x + 4, y, PLUCHE)
    for yy in range(y + 1, y + 6):
        for xx in range(x, x + 5):
            zet(t, xx, yy, PLUCHE if yy < y + 5 else PLUCHE_D)
    zet(t, x + 1, y + 2, OOG)
    zet(t, x + 3, y + 2, OOG)
    zet(t, x, y + 3, BLOS)
    zet(t, x + 4, y + 3, BLOS)
    zet(t, x + 2, y + 3, MOND)


def knuffelboekenkast():
    """Like vanilla's bookshelf: two rows of pastel books on pink blossom wood, a plush guh in every row."""
    r = rng(3201)
    t = ruis(vlak(HOUT), 4, 3202)
    for x in range(16):
        zet(t, x, 0, HOUT_L)
        zet(t, x, 7, HOUT_D)
        zet(t, x, 8, HOUT_L)
        zet(t, x, 15, HOUT_D)
    for (boven, onder, guh_x) in ((1, 6, 9), (9, 14, 2)):
        for y in range(boven, onder + 1):
            for x in range(1, 15):
                zet(t, x, y, ACHTER)
        x = 1
        while x < 15:
            if x == guh_x:
                _pluche_guhtje(t, guh_x, boven)
                x += 5
                continue
            bw = int(min(r.integers(1, 3), 15 - x, max(1, guh_x - x) if x < guh_x else 15 - x))
            hoog = int(r.integers(4, 7))
            c = BOEKEN[int(r.integers(0, len(BOEKEN)))]
            for xx in range(x, x + bw):
                for y in range(onder - hoog + 1, onder + 1):
                    zet(t, xx, y, mix(c, (255, 255, 255), 0.25) if xx == x else c)
                zet(t, xx, onder - hoog + 3, (255, 236, 150) if r.random() < 0.5 else mix(c, (0, 0, 0), 0.2))
            x += bw
        zet(t, 0, boven, HOUT_D)
        zet(t, 15, boven, HOUT_D)
    for y in range(16):
        zet(t, 0, y, mix(t[y, 0, :3], HOUT_D, 0.5))
        zet(t, 15, y, mix(t[y, 15, :3], HOUT_D, 0.5))
    return t


def kristal(seed):
    """Glowing pink crystal: facets in bands, light top-left."""
    r = rng(seed)
    t = leeg()
    for y in range(16):
        for x in range(16):
            f = 0.55 + 0.25 * np.sin((x * 0.9 + y * 1.7) * 0.6) - (x + y) * 0.012 + r.normal(0, 0.06)
            t[y, x, :3] = _ramp(np.array(f), KRISTAL)
            t[y, x, 3] = 255
    for _ in range(6):   # a few glints
        x, y = r.integers(1, 15, 2)
        zet(t, x, y, KRISTAL[-1])
    return t


def altaar_kern():
    """The crystal core (the model shows its middle band, y 5-11, on every side) with a little face."""
    t = kristal(3211)
    for (x, y) in ((5, 7), (6, 7), (5, 8), (6, 8), (9, 7), (10, 7), (9, 8), (10, 8)):
        zet(t, x, y, (90, 40, 110))
    zet(t, 5, 7, (255, 255, 255))
    zet(t, 9, 7, (255, 255, 255))
    zet(t, 4, 9, (255, 120, 190))
    zet(t, 11, 9, (255, 120, 190))
    for (x, y) in ((6, 9), (7, 10), (8, 10), (9, 9)):
        zet(t, x, y, (120, 50, 120))
    return t


def altaar_top(steen):
    """The top: a frame of guhkristalsteen around a glowing pink crystal mosaic with a heart."""
    t = steen.copy()
    k = kristal(3221)
    t[2:14, 2:14] = k[2:14, 2:14]
    for x in range(2, 14):   # a lighter rim inside the frame
        zet(t, x, 2, mix(t[2, x, :3], (255, 255, 255), 0.3))
    hart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
    for dy, rij in enumerate(hart):
        for dx, c in enumerate(rij):
            if c == "1":
                zet(t, 4 + dx, 5 + dy, (255, 120, 190) if dy > 0 or dx in (1, 4) else (255, 200, 230))
    zet(t, 5, 6, (255, 236, 246))
    return t


def boek():
    """The floating book (64 x 32, vanilla's layout): the covers a fluffy lilac with a pink heart, the pages pastel."""
    v = _vanilla("assets/minecraft/textures/entity/enchantment/enchanting_table_book.png")
    t = v.copy()
    r = rng(3231)
    h = _helder(v)
    lila = [(110, 72, 140), (156, 112, 190), (196, 160, 226), (226, 200, 246), (248, 232, 255)]
    for y in range(32):
        for x in range(64):
            if v[y, x, 3] == 0:
                continue
            if y < 10:   # covers and spine: lilac fluff (noise for the fur), the gold corners become pink
                gold = v[y, x, 0] > 200 and v[y, x, 2] < 120
                if gold:
                    t[y, x, :3] = (255, 150, 206)
                else:
                    t[y, x, :3] = _ramp(np.array(h[y, x] * 1.3 + r.normal(0, 0.06)), lila)
            else:        # pages: cream -> a soft pink white
                t[y, x, :3] = mix(v[y, x, :3], (255, 226, 240), 0.35)
    hart = ["0101", "1111", "0110"]
    for x0 in (1, 7, 17, 23):   # a heart on each cover face (the lids are 6 wide)
        for dy, rij in enumerate(hart):
            for dx, c in enumerate(rij):
                if c == "1":
                    t[4 + dy, x0 + dx, :3] = (255, 120, 190)
                    t[4 + dy, x0 + dx, 3] = 255
    return t


def spreukjesboekje():
    """The item: vanilla's enchanted book recoloured: a pink fluffy cover, a lilac ribbon, two little guh ears."""
    v = _vanilla("assets/minecraft/textures/item/enchanted_book.png")
    t = v.copy()
    h = _helder(v)
    roze = [(110, 40, 80), (176, 76, 130), (226, 120, 172), (246, 170, 206), (255, 218, 236)]
    lila = [(90, 50, 130), (150, 110, 200), (200, 170, 240)]
    for y in range(16):
        for x in range(16):
            if v[y, x, 3] == 0:
                continue
            rr, gg, bb = v[y, x, :3]
            if rr > 180 and gg > 180 and bb > 180:
                t[y, x, :3] = mix((rr, gg, bb), (255, 230, 244), 0.4)       # pages
            elif rr > 150 and gg < 80:
                t[y, x, :3] = _ramp(np.array(h[y, x] * 1.8), lila)          # the red ribbon -> lilac
            elif rr > 200 and gg > 140 and bb < 100:
                t[y, x, :3] = (255, 236, 150)                              # the clasp stays golden
            else:
                t[y, x, :3] = _ramp(np.array(h[y, x] * 2.2), roze)          # the brown cover -> pink
    # two little ears on the top edge of the cover
    for (x, y) in ((6, 1), (10, 0)):
        zet(t, x, y, (246, 170, 206))
        zet(t, x + 1, y, (226, 120, 172))
    return t


def deeltje_hart(groot):
    t = np.zeros((8, 8, 4), np.float32)
    vorm = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"] if groot else ["01010", "11111", "01110", "00100"]
    ox, oy = (0, 1) if groot else (1, 2)
    for dy, rij in enumerate(vorm):
        for dx, c in enumerate(rij):
            if c == "1":
                t[oy + dy, ox + dx] = (255, 130, 196, 255)
    t[oy + 1, ox + 1] = (255, 230, 244, 255)
    return t


def deeltje_glinster():
    t = np.zeros((8, 8, 4), np.float32)
    for i in range(1, 6):
        t[3, i] = t[i, 3] = (255, 190, 226, 255)
    t[3, 3] = (255, 255, 255, 255)
    return t


def deeltje_pluis(seed):
    r = rng(seed)
    t = np.zeros((8, 8, 4), np.float32)
    for y in range(8):
        for x in range(8):
            d = np.hypot(x - 3.5, y - 3.5) + r.normal(0, 0.2)
            if d < 3.2:
                t[y, x] = (*mix((255, 240, 248), (246, 186, 214), d / 3.2), 255)
    return t


def alle_texturen(h):
    steen = arr(Image.open(os.path.join(h.A, "textures", "block", "guh_kristalsteen.png")).convert("RGBA"))
    return {
        ("block", KAST): knuffelboekenkast(),
        ("block", f"{ALTAAR}_kern"): altaar_kern(),
        ("block", f"{ALTAAR}_top"): altaar_top(steen),
        ("block", f"{ALTAAR}_kristal"): kristal(3241),
        ("block", f"{ALTAAR}_boek"): boek(),
        ("item", BOEKJE): spreukjesboekje(),
        ("particle", "glinsterhartje_0"): deeltje_hart(True),
        ("particle", "glinsterhartje_1"): deeltje_hart(False),
        ("particle", "glinsterhartje_2"): deeltje_glinster(),
        ("particle", "pluispoef_0"): deeltje_pluis(3251),
        ("particle", "pluispoef_1"): deeltje_pluis(3252),
    }


def textures(h):
    for (soort, naam), t in alle_texturen(h).items():
        p = os.path.join(h.A, "textures", soort, f"{naam}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img(t).save(p)


# =====================================================================================================================
# models, blockstates, items, particles
# =====================================================================================================================
def _el(van, tot, tex, uv=None, faces=("north", "south", "east", "west", "up", "down")):
    """A model element with the same texture on the given faces (uv: one rectangle for the sides, or None = auto)."""
    f = {}
    for k in faces:
        e = {"texture": tex}
        if uv is not None and k not in ("up", "down"):
            e["uv"] = uv
        f[k] = e
    return {"from": van, "to": tot, "faces": f}


def models(h):
    A, w = h.A, h.w
    steen, kern, top, kris = "#steen", "#kern", "#top", "#kristal"
    elementen = [
        _el([0, 0, 0], [16, 3, 16], steen),                                     # foot
        _el([2, 3, 2], [14, 9, 14], kern, uv=[2, 5, 14, 11], faces=("north", "south", "east", "west")),   # crystal core + face
    ]
    for (x, z) in ((0.5, 0.5), (11.5, 0.5), (0.5, 11.5), (11.5, 11.5)):        # pillars
        elementen.append(_el([x, 3, z], [x + 4, 9, z + 4], steen, faces=("north", "south", "east", "west")))
    blad = _el([0, 9, 0], [16, 12, 16], steen)
    blad["faces"]["up"] = {"texture": top}
    elementen.append(blad)
    for (x, z) in ((0.5, 0.5), (13.5, 0.5), (0.5, 13.5), (13.5, 13.5)):        # crystal tips on the corners
        elementen.append(_el([x, 12, z], [x + 2, 15, z + 2], kris, uv=[6, 4, 8, 7]))
        elementen.append(_el([x + 0.5, 15, z + 0.5], [x + 1.5, 16, z + 1.5], kris, uv=[10, 2, 11, 3]))
    w(f"{A}/models/block/{ALTAAR}.json", {
        "parent": "minecraft:block/block", "ambientocclusion": False,
        "textures": {"particle": "guhs:block/guh_kristalsteen", "steen": "guhs:block/guh_kristalsteen", "kern": f"guhs:block/{ALTAAR}_kern",
                     "top": f"guhs:block/{ALTAAR}_top", "kristal": f"guhs:block/{ALTAAR}_kristal"},
        "elements": elementen})
    w(f"{A}/blockstates/{ALTAAR}.json", {"variants": {"": {"model": f"guhs:block/{ALTAAR}"}}})
    w(f"{A}/models/item/{ALTAAR}.json", {"parent": f"guhs:block/{ALTAAR}"})

    w(f"{A}/models/block/{KAST}.json", {"parent": "minecraft:block/cube_column",
                                        "textures": {"side": f"guhs:block/{KAST}", "end": "guhs:block/guhbloesem_planks"}})
    w(f"{A}/blockstates/{KAST}.json", {"variants": {"": {"model": f"guhs:block/{KAST}"}}})
    w(f"{A}/models/item/{KAST}.json", {"parent": f"guhs:block/{KAST}"})

    h.item_model(BOEKJE)
    for i in (ALTAAR, KAST, BOEKJE):
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{i}"}})

    w(f"{A}/particles/glinsterhartje.json", {"textures": [f"guhs:glinsterhartje_{i}" for i in range(3)]})
    w(f"{A}/particles/pluispoef.json", {"textures": ["guhs:pluispoef_0", "guhs:pluispoef_1"]})


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
def loot(h):
    D, w = h.D, h.w
    w(f"{D}/loot_table/blocks/{ALTAAR}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"guhs:{ALTAAR}", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name"]}]}]}],
        "random_sequence": f"guhs:blocks/{ALTAAR}"})
    h.self_drop(KAST)


def recipes(h):
    # the altar: like vanilla's table (a book on diamonds and obsidian), the guh way: a book on two Vahoegiet around a guh
    # crystal, on guhkristalsteen
    h.shaped(ALTAAR, [" B ", "VKV", "SSS"],
             {"B": "minecraft:book", "V": "guhs:vahoegiet", "K": "guhs:guh_kristal", "S": "guhs:guh_kristalsteen"}, f"guhs:{ALTAAR}")
    # the shelf: like vanilla's bookshelf (6 planks + 3 books), with blossom wood and a tuft of pink wool for the plush guh
    h.shaped(KAST, ["PWP", "BBB", "PPP"], {"P": "guhs:guhbloesem_planks", "W": "minecraft:pink_wool", "B": "minecraft:book"}, f"guhs:{KAST}")


VILTFAMILIE = ["viltsteen", "gepolijste_viltsteen", "viltsteen_bakstenen", "viltsteen_tegels", "gebeitelde_viltsteen",
               "viltsteen_trap", "viltsteen_plaat", "viltsteen_muur", "gepolijste_viltsteen_trap", "gepolijste_viltsteen_plaat",
               "viltsteen_bakstenen_trap", "viltsteen_bakstenen_plaat", "viltsteen_bakstenen_muur", "viltsteen_tegels_trap",
               "viltsteen_tegels_plaat", "viltkiezel"]
PLUIS = ["pluiskussen", "pluiswolblok", "pluisdak", "pluisdak_plaat", "pluisdak_trap", "golfbaan_vilt"]
KAASKNABBEL = ["block_of_kaasknabbels", "kaasknabbel_stone", "kaasknabbel_cobblestone", "kaasknabbel_deepslate", "kaasknabbel_dirt",
               "kaasknabbelerts_vilt", "kaasknabbelerts_knuffel", "kaasknabbelerts_kristal", "houtskoolsteen_kaasknabbelerts"]
VANILLA_POOL = ["sharpness", "protection", "efficiency", "unbreaking", "power", "fortune", "looting", "silk_touch", "feather_falling",
                "respiration", "depth_strider", "swift_sneak", "mending"]
TAFEL_SPREUKEN = ["mika_mepper", "pluisval", "glitterzicht", "vadsvlijt", "bakvads"]
BOEK_SPREUKEN = ["spaarguhtje", "aaizacht", "kaasknabbelgeur"]
ONBREEKBAAR = ["guhs:vahoege_vads_helmet", "guhs:vahoege_vads_chestplate", "guhs:vahoege_vads_leggings", "guhs:vahoege_vads_boots",
               "guhs:vahoege_vads_sword", "guhs:vahoege_vads_pickaxe", "guhs:vahoege_vads_axe", "guhs:vahoege_vads_shovel",
               "guhs:vahoege_vads_hoe", "guhs:vahoege_vads_paxel", "guhs:vahoege_vads_shears", "guhs:mika_mepper"]


def _opt(ids):
    """Tag entries that may be missing (a block of another slice that is renamed one day must not break the tag)."""
    return [{"id": i, "required": False} for i in ids]


def tags(h):
    R, w = h.R, h.w
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{ALTAAR}"])
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{KAST}"])
    w(f"{R}/data/guhs/tags/block/toveren/zacht.json", {"replace": False, "values": [
        "#minecraft:wool", "#minecraft:wool_carpets", *_opt([f"guhs:{b}" for b in VILTFAMILIE + PLUIS])]})
    w(f"{R}/data/guhs/tags/block/toveren/vadsvlijt.json", {"replace": False, "values": [
        "#minecraft:wool", "#minecraft:wool_carpets", *_opt([f"guhs:{b}" for b in VILTFAMILIE + PLUIS + KAASKNABBEL])]})
    E = f"{R}/data/guhs/tags/enchantment"
    w(f"{E}/glinsteraltaar.json", {"replace": False, "values": [f"minecraft:{e}" for e in VANILLA_POOL] + [f"guhs:{e}" for e in TAFEL_SPREUKEN]})
    w(f"{E}/spreukjes/alle.json", {"replace": False, "values": [f"guhs:{e}" for e in TAFEL_SPREUKEN + BOEK_SPREUKEN]})
    w(f"{E}/spreukjes/boekje.json", {"replace": False, "values": [f"guhs:{e}" for e in BOEK_SPREUKEN]})
    w(f"{E}/duurzaamheid.json", {"replace": False, "values": ["minecraft:unbreaking", "minecraft:mending"]})
    # every weapon but the Mika-mepper itself (NeoForge "remove")
    w(f"{R}/data/guhs/tags/item/enchantable/mika_mepper.json", {"replace": False, "values": ["#minecraft:enchantable/weapon"],
                                                                 "remove": ["guhs:mika_mepper"]})
    # no unbreaking/mending for unbreakable gear: out of the vanilla durability tag (table, anvil, books, everything)
    w(f"{R}/data/minecraft/tags/item/enchantable/durability.json", {"replace": False, "values": [], "remove": ONBREEKBAAR})


# =====================================================================================================================
# the enchantments
# =====================================================================================================================
def _spreuk(naam, ondersteund, slots, gewicht, max_level, min_cost, max_cost, anvil, effects=None, exclusief=None):
    d = {"anvil_cost": anvil, "description": {"translate": f"enchantment.guhs.{naam}"}, "effects": effects or {},
         "max_cost": {"base": max_cost[0], "per_level_above_first": max_cost[1]}, "max_level": max_level,
         "min_cost": {"base": min_cost[0], "per_level_above_first": min_cost[1]}, "slots": slots,
         "supported_items": ondersteund, "weight": gewicht}
    if exclusief:
        d["exclusive_set"] = exclusief
    return d


def enchantments(h):
    D, w = h.D, h.w
    mika = {"minecraft:damage": [{"effect": {"type": "minecraft:multiply", "factor": 2.0},
                                  "requirements": {"condition": "minecraft:entity_properties", "entity": "this",
                                                   "predicate": {"type": "#guhs:mikas"}}}]}
    alles = {
        "mika_mepper": _spreuk("mika_mepper", "#guhs:enchantable/mika_mepper", ["mainhand"], 5, 1, (10, 0), (50, 0), 2, mika),
        "pluisval": _spreuk("pluisval", "#minecraft:enchantable/foot_armor", ["feet"], 5, 1, (5, 0), (45, 0), 2),
        "glitterzicht": _spreuk("glitterzicht", "#minecraft:enchantable/head_armor", ["head"], 2, 1, (10, 0), (50, 0), 4),
        "vadsvlijt": _spreuk("vadsvlijt", "#minecraft:enchantable/mining", ["mainhand"], 5, 3, (5, 10), (55, 10), 2),
        "bakvads": _spreuk("bakvads", "#minecraft:enchantable/mining_loot", ["mainhand"], 2, 1, (15, 0), (65, 0), 4,
                           exclusief=["minecraft:silk_touch"]),
        "spaarguhtje": _spreuk("spaarguhtje", "#minecraft:enchantable/mining_loot", ["mainhand"], 1, 1, (15, 0), (65, 0), 8),
        "aaizacht": _spreuk("aaizacht", "#minecraft:enchantable/weapon", ["mainhand"], 2, 1, (5, 0), (55, 0), 2),
        "kaasknabbelgeur": _spreuk("kaasknabbelgeur", "#minecraft:enchantable/armor", ["armor"], 1, 1, (10, 0), (60, 0), 4),
    }
    for naam, d in alles.items():
        w(f"{D}/enchantment/{naam}.json", d)


# =====================================================================================================================
# texts
# =====================================================================================================================
TEKSTEN = {
    f"block.guhs.{ALTAAR}": ("Glitter Altar", "Glinsteraltaar"),
    f"block.guhs.{KAST}": ("Cuddle Bookshelf", "Knuffelboekenkast"),
    f"item.guhs.{BOEKJE}": ("Little Spell Book", "Spreukjesboekje"),
    "enchantment.guhs.mika_mepper": ("Mika Whacker", "Mika-mepper"),
    "enchantment.guhs.pluisval": ("Fluff Fall", "Pluisval"),
    "enchantment.guhs.glitterzicht": ("Glitter Sight", "Glitterzicht"),
    "enchantment.guhs.spaarguhtje": ("Piggy Guh", "Spaarguhtje"),
    "enchantment.guhs.bakvads": ("Baking Vads", "Bakvads"),
    "enchantment.guhs.aaizacht": ("Gentle Pat", "Aaizacht"),
    "enchantment.guhs.vadsvlijt": ("Vads Diligence", "Vadsvlijt"),
    "enchantment.guhs.kaasknabbelgeur": ("Cheese Nibble Scent", "Kaasknabbelgeur"),
    # (the descriptions: shown by enchantment-description mods and the wiki)
    "enchantment.guhs.mika_mepper.desc": ("Double damage against Mikas. Not for the Mika Whacker itself: it already whacks hard enough!",
                                          "Dubbele schade tegen Mika's. Niet voor de Mika-mepper zelf: die mept al hard genoeg!"),
    "enchantment.guhs.pluisval.desc": ("Land on wool, felt or fluff without any fall damage. Poef!",
                                       "Land zonder valschade op wol, vilt of pluis. Poef!"),
    "enchantment.guhs.glitterzicht.desc": ("A soft glittery night vision, but only underground.",
                                           "Een zacht glinsterend nachtzicht, maar alleen onder de grond."),
    "enchantment.guhs.spaarguhtje.desc": ("What you mine and pick up goes straight into the Bank Guh of the Bank Key in your inventory. Kling!",
                                          "Wat je hakt en oppakt gaat meteen naar de Bankguh van de Banksleutel in je inventaris. Klingel!"),
    "enchantment.guhs.bakvads.desc": ("What you mine comes out of the ground already smelted. Toasty!",
                                      "Wat je hakt komt al gesmolten uit de grond. Lekker warm!"),
    "enchantment.guhs.aaizacht.desc": ("Your hits never hurt your own pets or villagers: they just get a pat.",
                                       "Je klappen raken nooit je eigen huisdieren of villagers: die krijgen gewoon een aai."),
    "enchantment.guhs.vadsvlijt.desc": ("Mine much faster through wool, felt and cheese nibble blocks.",
                                        "Hak veel sneller door wol, vilt en kaasknabbelblokken."),
    "enchantment.guhs.kaasknabbelgeur.desc": ("You smell of cheese nibbles: every wild guh nearby comes to you and follows you around.",
                                              "Je ruikt naar kaasknabbels: alle wilde guhs in de buurt komen naar je toe en lopen je achterna."),
}


def texts(h):
    for k, (en, nl) in TEKSTEN.items():
        h.lang(k, en, nl)


def selfcheck(h):
    problemen = []
    for (soort, naam) in alle_texturen(h):
        if not os.path.exists(os.path.join(h.A, "textures", soort, f"{naam}.png")):
            problemen.append(f"texture missing: {soort}/{naam}")
    for e in TAFEL_SPREUKEN + BOEK_SPREUKEN:
        if not os.path.exists(f"{h.D}/enchantment/{e}.json"):
            problemen.append(f"enchantment missing: {e}")
    if problemen:
        raise SystemExit("toveren self-check failed:\n  " + "\n  ".join(problemen))


def build(h):
    textures(h)
    models(h)
    loot(h)
    recipes(h)
    tags(h)
    enchantments(h)
    texts(h)
    selfcheck(h)


def _direct():
    import types
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
        w(full, data)

    def item_model(name, tex_=None, parent="minecraft:item/generated"):
        w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex_ or f"guhs:item/{name}"}})

    def self_drop(name, item=None):
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{item or name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    def shaped(name, pattern, key, result, count=1):
        w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": dict(key),
                                      "result": {"id": result, "count": count}})

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, w=w, add_tag=add_tag, item_model=item_model, self_drop=self_drop, shaped=shaped, lang=lang)
    build(h)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"toveren: {len(NL)} texts")


if __name__ == "__main__":
    _direct()
