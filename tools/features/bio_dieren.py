"""
biomes3 slice "dieren" (Java: feature/bio/dieren; English: tools/lang/en/c84_bio_dieren.json; contract: CONTRACT_BIO.md).

  - koi                a calm school fish in guh style, five colours; koivoer (recipe) makes them come up and eat, a water
                       bucket scoops one up (koi_emmer) to live in your own pond
  - wolkenschaapje     a little sheep that is mostly cloud and floats above the Wolkenweide; shears give wolkenpluis
  - kikkerguh          the existing one, now also by the lake and the valley ponds (it sits on lily pads there)
  - bloesemguh, tanukiguh   the wild guhs of the Bloesemmeertje and the Klaterdal (variant bones + furs), and the wolkguh
                       wild in the Wolkenweide (Java only)

build(h) writes the models/animations/textures (bio_dieren_modellen), the item icons and models, sounds.json (the OGGs are
made once by bio_dieren_geluid.py and committed), the spawns (biome modifiers), the koivoer recipe, the hidden quest
advancements (the proofs for the systemen slice), the game test templates and all texts (Dutch). No ftb(fq): systemen's.
BONES / variants are for tools/make_guh_variants.py.
"""
import os

from PIL import Image

from features import bio_dieren_modellen as modellen
from features import bio_lib as lib

BONES = modellen.BONES
CLOTHES = []

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
SOUNDS = {
    "koi.hap": ["koi_hap1", "koi_hap2", "koi_hap3"],
    "koi.strooi": ["koi_strooi1", "koi_strooi2"],
    "wolkenschaapje.bleh": ["schaapje_bleh1", "schaapje_bleh2", "schaapje_bleh3"],
    "wolkenschaapje.pluis": ["schaapje_pluis1", "schaapje_pluis2"],
}
# name: (biomes, entity, weight, min, max). The Java spawn rules add the caps (DierenRegels) and "near water".
SPAWNS = {
    "koi_bloesemmeertje": (["guhs:bloesemmeertje"], "guhs:koi", 12, 3, 5),
    "koi_klaterdal": (["guhs:klaterdal"], "guhs:koi", 10, 2, 4),
    "biodieren_kikkerguh_bloesemmeertje": (["guhs:bloesemmeertje"], "guhs:kikkerguh", 20, 1, 3),
    "biodieren_kikkerguh_klaterdal": (["guhs:klaterdal"], "guhs:kikkerguh", 10, 1, 2),
    "wolkenschaapje_wolkenweide": (["guhs:wolkenweide"], "guhs:wolkenschaapje", 30, 2, 3),
}
# the hidden advancements (granted from Java, or by a vanilla trigger): the proofs the systemen slice hangs its quests on
QUEST = ["koi_gevoerd", "koi_kleintje", "wolkenschaapje_geschoren", "wolkenschaapje_gehouden"]
PAGINAS = ["koi", "wolkenschaapje", "bloesemguh", "tanukiguh"]


def variants(rng, v):
    # (rng depends on this module's place in FEATURES: not used; the painters take theirs from bio_lib.rng)
    return modellen.variants(v)


# =====================================================================================================================
# item icons
# =====================================================================================================================
def koivoer_icon(h):
    """A little paper bag of pellets with a koi on it."""
    pal = {"a": (150, 104, 70, 255), "b": (214, 172, 124, 255), "c": (236, 204, 160, 255), "d": (120, 82, 54, 255),
           "r": (234, 84, 48, 255), "w": (252, 248, 242, 255), "k": (30, 26, 40, 255), "p": (214, 140, 60, 255), "q": (176, 106, 44, 255),
           "s": (226, 92, 120, 255)}
    rows = ["................",
            "....pq.p.qp.....",
            "...qpppqppqp....",
            "...dddddddddd...",
            "..abccccccccba..",
            "..abccssssccba..",
            "..abccccccccba..",
            "..abcwrrwwccba..",
            "..abwrrrwwkwba..",
            "..abwwrwwwwwba..",
            "..abcwwwrrwcba..",
            "..abccccccccba..",
            "..abccccccccba..",
            "..abbbbbbbbbba..",
            "...aaaaaaaaaa...",
            "................"]
    return h.grid(rows, pal)


def koi_emmer_icon(h):
    """A water bucket with a rood-witte koi peeking over the rim: its red cap, guh eyes and round ears."""
    img = h.vanilla("item/water_bucket").copy()
    px = img.load()
    wit, wit2, rood, donker, oog = (252, 248, 242, 255), (226, 220, 214, 255), (234, 84, 48, 255), (150, 132, 126, 255), (18, 18, 34, 255)
    for y in range(2, 7):
        for x in range(4, 12):
            px[x, y] = wit2 if x in (4, 11) or y == 6 else wit
    for x in range(5, 11):
        px[x, 1] = donker
    for x in range(6, 10):                                     # the red cap
        px[x, 2] = rood
    px[7, 3] = px[8, 3] = rood
    for (x, y) in ((4, 1), (11, 1)):                           # two round ears
        px[x, y] = donker
    px[4, 0] = px[11, 0] = (244, 150, 170, 255)
    for ex in (5, 9):                                          # the glossy guh eyes
        px[ex, 3] = oog
        px[ex + 1, 3] = (255, 255, 255, 255)
        px[ex, 4] = (40, 100, 200, 255)
        px[ex + 1, 4] = (70, 150, 220, 255)
    px[5, 5] = px[10, 5] = (255, 140, 180, 255)                # blush
    px[7, 5] = px[8, 5] = (240, 132, 168, 255)                 # lips
    return img


def items(h):
    import mc26
    h.save(koivoer_icon(h), "item", "koivoer.png")
    h.item_model("koivoer")
    h.save(koi_emmer_icon(h), "item", "koi_emmer.png")
    h.item_model("koi_emmer")
    # spawn eggs (baked like mc26.spawn_eggs does for the eggs it knows)
    for name, (base, spots) in (("koi_spawn_egg", (0xFAF6F0, 0xEA5430)), ("wolkenschaapje_spawn_egg", (0xFAFCFF, 0xF6A0BE))):
        egg = Image.alpha_composite(mc26.tint(mc26.vanilla_121("item/spawn_egg"), base), mc26.tint(mc26.vanilla_121("item/spawn_egg_overlay"), spots))
        h.save(egg, "item", f"{name}.png")
        h.item_model(name)
    h.shapeless("koivoer", ["guhs:kaas_knabbels", "minecraft:wheat_seeds"], "guhs:koivoer", 6)


def sounds(h):
    def patch(d):
        for event, files in SOUNDS.items():
            d[event] = {"sounds": [f"guhs:bio_dieren/{f}" for f in files], "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def spawns(h):
    for name, (biomes, entity, weight, lo, hi) in SPAWNS.items():
        h.w(f"{h.D}/neoforge/biome_modifier/{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
                                                           "spawners": [{"type": entity, "weight": weight, "minCount": lo, "maxCount": hi}]})


def advancements(h):
    D = h.D
    for name in QUEST:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    for p in PAGINAS:                                           # (the Guhdex grants guhs:quest/seen_<page id>)
        h.w(f"{D}/advancement/quest/seen_{p}.json", {"criteria": IMPOSSIBLE})
    h.w(f"{D}/advancement/quest/koi_emmer.json", {"criteria": {"done": {
        "trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:koi_emmer"}]}}}})
    for variant in ("bloesemguh", "tanukiguh"):
        h.w(f"{D}/advancement/quest/{variant}_getemd.json", {"criteria": {"done": {"trigger": "minecraft:tame_animal", "conditions": {"entity": [
            {"condition": "minecraft:entity_properties", "entity": "this",
             "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"%s\"}" % variant}}]}}}})


# =====================================================================================================================
# texts (Dutch; English later in tools/lang/en/c84_bio_dieren.json)
# =====================================================================================================================
TEXTS = {
    "entity.guhs.koi": "Koi",
    "entity.guhs.wolkenschaapje": "Wolkenschaapje",
    "entity.guhs.guh.bloesemguh": "Bloesemguh",
    "entity.guhs.guh.tanukiguh": "Tanukiguh",
    "item.guhs.koi_spawn_egg": "Koi-spawnei",
    "item.guhs.wolkenschaapje_spawn_egg": "Wolkenschaapje-spawnei",
    "item.guhs.koivoer": "Koivoer",
    "item.guhs.koivoer.tooltip": "Houd het vast bij het water: de koi komen happen. Rechtsklik op het water om een handje te strooien, njeg!",
    "item.guhs.koi_emmer": "Koi-emmer",
    "item.guhs.koi_emmer.kleur": "Een %s koi",
    "item.guhs.koi_emmer.kleintje": "Nog een kleintje",
    "item.guhs.koi_emmer.tooltip": "Leeg de emmer in je eigen vijver: daar blijft je koi voorgoed wonen",
    "gui.guhs.koi.kleur.roodwit": "rood-witte",
    "gui.guhs.koi.kleur.driekleur": "driekleurige",
    "gui.guhs.koi.kleur.roze": "roze",
    "gui.guhs.koi.kleur.blauw": "blauwe",
    "gui.guhs.koi.kleur.goud": "gouden",
    "gui.guhs.wolkenschaapje.gehouden": "Dit wolkenschaapje blijft nu bij jou. Bleh-njeg!",
    # sounds
    "subtitles.guhs.koi.hap": "Koi hapt",
    "subtitles.guhs.koi.strooi": "Koivoer plonst",
    "subtitles.guhs.wolkenschaapje.bleh": "Wolkenschaapje blaat zachtjes",
    "subtitles.guhs.wolkenschaapje.pluis": "Wolkenpluis laat los",
    # the Guhdex pages (bonus pages)
    "gui.guhs.guhdex.rarity.koi": "Zeldzaamheid: Vaak (vijvers van het Klaterdal en het Bloesemmeertje)",
    "gui.guhs.guhdex.info.koi": "Een dikke, rustige vijvervis met guhoogjes, blosjes en twee ronde guhoortjes als vinnetjes. Er zijn rood-witte, driekleurige, roze, blauwe en gouden koi. Houd koivoer vast en ze komen naar boven om te happen. Schep er een op met een emmer water en hij woont voorgoed in jouw vijver. Vahoeg!",
    "gui.guhs.guhdex.rarity.wolkenschaapje": "Zeldzaamheid: Alleen in de Wolkenweide",
    "gui.guhs.guhdex.info.wolkenschaapje": "Een schaapje dat bijna helemaal wolk is. Het zweeft een handje boven het gras en dwarrelt zachtjes rond. Knip het met een schaar voor wolkenpluis: na een poosje is het weer net zo pluizig. Geef het knabbelvoer of neem het aan een leidtouw mee, dan blijft het bij je. Bleh-njeg!",
    "gui.guhs.guhdex.rarity.bloesemguh": "Zeldzaamheid: Alleen bij het Bloesemmeertje",
    "gui.guhs.guhdex.info.bloesemguh": "Een bloesemwitte guh met roze blaadjes in zijn vacht en een bloesempje bij zijn oor. Hij ligt het liefst de hele dag onder de guhbloesembomen aan het water en doet 's avonds een dutje aan de oever. Tem hem met kaasknabbels!",
    "gui.guhs.guhdex.rarity.tanukiguh": "Zeldzaamheid: Alleen in het Klaterdal",
    "gui.guhs.guhdex.info.tanukiguh": "Een grijsbruine guh met een donker maskertje, een dikke geringde pluimstaart en een blaadje op zijn kop. Niemand weet waarom dat blaadje er nooit af valt. Woont in het Klaterdal en dut 's avonds bij het kabbelende water. Tem hem met kaasknabbels, njeg!",
}


# =====================================================================================================================
# game test templates (BioDierenGameTests)
# =====================================================================================================================
def test_templates(h):
    # a pond: 12 x 12, grass round a 8 x 8 pool two deep, two lily pads on it (the water surface is y 2, the bank y 3)
    t = h.Structure((12, 7, 12))
    for x in range(12):
        for z in range(12):
            vijver = 2 <= x <= 9 and 2 <= z <= 9
            t.set(x, 0, z, "minecraft:sand" if vijver else "minecraft:dirt")
            for y in (1, 2):
                if vijver:
                    t.set(x, y, z, "minecraft:water", {"level": "0"})
                else:
                    t.set(x, y, z, "minecraft:grass_block" if y == 2 else "minecraft:dirt", {"snowy": "false"} if y == 2 else {})
    for (x, z) in ((4, 4), (7, 6)):
        t.set(x, 3, z, "minecraft:lily_pad")
    t.save("biodieren_test_vijver")
    # a meadow: 12 x 12 grass
    t = h.Structure((12, 6, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("biodieren_test_wei")


def selfcheck(h):
    problems = modellen.check(h)
    for key, text in TEXTS.items():
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    for p in PAGINAS:
        naam = f"entity.guhs.{p}" if p in ("koi", "wolkenschaapje") else f"entity.guhs.guh.{p}"
        for k in (naam, f"gui.guhs.guhdex.rarity.{p}", f"gui.guhs.guhdex.info.{p}"):
            if k not in TEXTS:
                problems.append(f"missing text {k}")
    for k in modellen.KOI:
        if f"gui.guhs.koi.kleur.{k}" not in TEXTS:
            problems.append(f"missing text gui.guhs.koi.kleur.{k}")
    for event, files in SOUNDS.items():
        if f"subtitles.guhs.{event}" not in TEXTS:
            problems.append(f"missing subtitle {event}")
        for f in files:
            if not os.path.exists(os.path.join(h.A, "sounds", "bio_dieren", f + ".ogg")):
                problems.append(f"missing sound bio_dieren/{f}.ogg (run tools/features/bio_dieren_geluid.py)")
    for i in ("koivoer", "koi_emmer", "koi_spawn_egg", "wolkenschaapje_spawn_egg"):
        if not os.path.exists(os.path.join(h.TEX, "item", f"{i}.png")) or not os.path.exists(os.path.join(h.A, "models", "item", f"{i}.json")):
            problems.append(f"missing item texture or model {i}")
    if problems:
        raise SystemExit("bio_dieren self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    items(h)
    sounds(h)
    spawns(h)
    advancements(h)
    lib.teksten(h, TEXTS)
    test_templates(h)
    selfcheck(h)
