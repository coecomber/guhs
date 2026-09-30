"""
Het guhleven (2.8.0 "Knuffeldal", slice wereldleven): the world feels alive (see guhs_work28/KNUFFEL_CONTRACT.md, par. 4.8).

  - Dagritme: guhs of guh villages and the Knuffeldal and free-roaming tamed guhs yawn in the morning, wave during the day,
    nap in a nestje in the afternoon, sit round the campfire with marshmallow knabbels in the evening, and sleep at night
    (all Java: feature/wereldleven/Dagritme.java). Here: the marshmallow_knabbel, the nap nestje is the Vadswoud guhnestje.
  - IJscoguh Tingeling (entity ijscoguh, Guhdex page): the ice-cream bike model (wereldleven_modellen.ijscoguh), the seven
    kaasijsjes (three always, four seasonal), the effects blosjes / zweverig, the guh's ice-cream hat and blushing cheeks
    (extra guh bones outfit_wl_*, textures guh_clothes/wereldleven_*.png), the IJscopetje.
  - Het koortje: the guh-xylofoon, the guh-fluitje, the liedjesboekje (6 songs), the koorstrikje.
  - De grijpmachine: the block (two halves + the claw model), the 22 plushies (knuffel_<variant> + knuffel_glitter), the
    grijpmachine piece of the Knuffeldal plein (knuffeldal_stadje/grijpmachine) and one at the guh kermis (make_v2).
  - the Knus tab (knuffelkast, liedjesboek, ijsjes, 8 milestones), advancements, FTB quests (rows y = 98 and 99.5), lang,
    the game test rooms.
Textures: wereldleven_tex.py; models: wereldleven_modellen.py; texts: wereldleven_tekst.py.
"""
import json
import os
import random

from features import wereldleven_modellen as modellen
from features import wereldleven_tekst as tekst
from features import wereldleven_tex as tex

FTB_Y = (98, 99.5)
KNUFFEL_IDS = ["normal", "mint", "choco", "snow", "brontosaurus", "golden", "rainbow", "starry", "ghost", "teckel", "brococolief", "ender",
               "koning", "wolk", "zeemeerguh", "mager", "vahoege_ender", "kaasmoerasguh", "asguh", "pluisguh", "glitter",
               "pinguh"]   # (2.9, fundament: the Pinguh variant; last here so the older plushies keep their random seeds)
SMAKEN = ["roze", "mint", "choco", "bloesem", "zonnetje", "appeltaart", "sneeuw"]
CLOTHES = ["koorstrikje", "ijscopetje"]
PARTICLES = {"zangnootje": (2, tex.particle_noot), "ijsjeshartje": (3, tex.particle_hartje), "fluitstoom": (3, tex.particle_stoom)}
SOUNDS = {
    "wereldleven.ijscobel": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.5},
                             {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.3}],
    "wereldleven.xylofoon": [{"name": "minecraft:block.note_block.xylophone", "type": "event"}],
    "wereldleven.fluitje": [{"name": "minecraft:block.note_block.flute", "type": "event"}],
    "wereldleven.grijpklauw": [{"name": "minecraft:block.chain.place", "type": "event"},
                               {"name": "minecraft:block.piston.contract", "type": "event", "volume": 0.4, "pitch": 1.4}],
}

# =====================================================================================================================
# the guh's ice-cream hat and blushing cheeks (hidden bones, drawn by WereldlevenLagen), and the clothes
# =====================================================================================================================
_H = [0, 6, -2]   # the head pivot
BONES = {
    # an ice cream standing on its point on the head: the cone widening upwards, a scoop (tinted with the flavour), a cherry
    "outfit_wl_hoorntje": ("head", _H, "wl_hoorntje", [([-0.75, 15, -7.25], [1.5, 1.2, 1.5], 0), ([-1.5, 16.2, -8], [3, 1.3, 3], 0),
                                                       ([-2.25, 17.5, -8.75], [4.5, 1.3, 4.5], 0)]),
    "outfit_wl_bolletje": ("head", _H, "wl_bolletje", [([-3, 18.8, -9.5], [6, 2.6, 6], 0), ([-2.25, 21.4, -8.75], [4.5, 1, 4.5], 0),
                                                       ([-3.2, 18.5, -9.7], [6.4, 0.6, 6.4], 0)]),
    "outfit_wl_kersje": ("head", _H, "wl_kersje", [([-0.6, 22.4, -6.85], [1.2, 1.2, 1.2], 0)]),
    # blushing cheeks under the eyes, left and right of the snoet
    "outfit_wl_blosjes": ("head", _H, "wl_blosjes", [([3.6, 5.2, -12.2], [2.8, 1.4, 0.2], 0), ([-6.4, 5.2, -12.2], [2.8, 1.4, 0.2], 0)]),
}


def clothes(rng, v):
    return {"wereldleven_ijshoedje": {"wl_hoorntje": lambda: tex.swatch_hoorntje(v, rng), "wl_bolletje": lambda: tex.swatch_bolletje(v, rng),
                                      "wl_kersje": lambda: tex.swatch_kersje(v, rng)},
            "wereldleven_blosjes": {"wl_blosjes": lambda: tex.swatch_blosjes(v, rng)},
            "koorstrikje": {"bowtie": lambda: tex.swatch_koorstrikje(v, rng)},
            "ijscopetje": {"cap": lambda: tex.swatch_ijscopet(v, rng)}}


def icons(ic):
    strik = ic.icon(ic.pad(["aa..........aa..", "abba......abba..", "abbbaaaaaabbba..", "abbbbbccbbbbba..", "abbbaaaaaabbba..",
                            "abba......abba..", "aa..........aa.."]), {"a": (80, 40, 150), "b": (140, 90, 220), "c": (255, 216, 78)})
    return {"koorstrikje": strik, "ijscopetje": ic.shaped("cap", (200, 90, 140), (255, 250, 252), (246, 136, 184))}


# =====================================================================================================================
# textures
# =====================================================================================================================
def textures(h):
    rng = random.Random(28800)
    save = h.save
    for vid in KNUFFEL_IDS:
        save(tex.knuffel(vid, random.Random(28800 + KNUFFEL_IDS.index(vid))), "block", f"knuffel_{vid}.png")
    save(tex.xylofoon_hout(rng), "block", "guh_xylofoon_hout.png")
    save(tex.xylofoon_gezicht(rng), "block", "guh_xylofoon_gezicht.png")
    save(tex.xylofoon_hamer(), "block", "guh_xylofoon_hamer.png")
    for i in range(8):
        save(tex.xylofoon_staaf(i, rng), "block", f"guh_xylofoon_staaf_{i}.png")
    save(tex.grijp_kast(rng), "block", "grijpmachine_kast.png")
    save(tex.grijp_voorkant(rng), "block", "grijpmachine_voorkant.png")
    save(tex.grijp_paneel(rng), "block", "grijpmachine_paneel.png")
    save(tex.grijp_glas(), "block", "grijpmachine_glas.png")
    save(tex.grijp_frame(rng), "block", "grijpmachine_frame.png")
    save(tex.grijp_bord(rng), "block", "grijpmachine_bord.png")
    save(tex.grijp_klauw(rng), "block", "grijpmachine_klauw.png")
    save(tex.rood(rng), "block", "grijpmachine_rood.png")
    for s in SMAKEN:
        save(tex.kaasijsje(s), "item", f"kaasijsje_{s}.png")
    save(tex.fluitje(), "item", "guh_fluitje.png")
    save(tex.marshmallow(), "item", "marshmallow_knabbel.png")
    save(tex.liedjesboekje(), "item", "wereldleven_liedjesboekje.png")
    save(tex.grijpmachine_icoon(), "item", "grijpmachine.png")
    for name, (n, painter) in PARTICLES.items():
        for i in range(n):
            save(painter(i), "particle", f"{name}_{i}.png")
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})


# =====================================================================================================================
# items, loot, recipes, tags, sounds
# =====================================================================================================================
def items_and_data(h):
    A, D, w = h.A, h.D, h.w
    for s in SMAKEN:
        h.item_model(f"kaasijsje_{s}")
    for i in ("guh_fluitje", "marshmallow_knabbel", "wereldleven_liedjesboekje"):
        h.item_model(i)
    w(f"{A}/models/item/ijscoguh_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # loot: the plushies and the xylofoon drop themselves, the grijpmachine only from its lower half
    for b in ["guh_xylofoon"] + [f"knuffel_{k}" for k in KNUFFEL_IDS]:
        h.self_drop(b)
    w(f"{D}/loot_table/blocks/grijpmachine.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": "guhs:grijpmachine"}], "conditions": [
        {"condition": "minecraft:block_state_property", "block": "guhs:grijpmachine", "properties": {"half": "lower"}},
        {"condition": "minecraft:survives_explosion"}]}]})
    # recipes (the plushies only come out of the grijpmachine; the ice creams only from IJscoguh Tingeling)
    h.shaped("guh_xylofoon", ["RYG", "BPL", "S S"], {"R": "minecraft:red_dye", "Y": "minecraft:yellow_dye", "G": "minecraft:lime_dye",
                                                     "B": "minecraft:light_blue_dye", "P": "#minecraft:planks", "L": "minecraft:purple_dye",
                                                     "S": "minecraft:stick"}, "guhs:guh_xylofoon", 1)
    h.shaped("guh_fluitje", [" I ", "SIP", " I "], {"I": "minecraft:iron_nugget", "S": "minecraft:string", "P": "minecraft:pink_dye"},
             "guhs:guh_fluitje", 1)
    h.shapeless("wereldleven_liedjesboekje", ["minecraft:book", "minecraft:note_block", "guhs:kaas_knabbels"], "guhs:wereldleven_liedjesboekje", 1)
    h.shapeless("marshmallow_knabbel", ["minecraft:stick", "minecraft:sugar", "guhs:kaas_knabbels"], "guhs:marshmallow_knabbel", 2)
    h.shaped("grijpmachine", ["GGG", "GKG", "PRP"], {"G": "minecraft:glass", "K": "minecraft:chain", "P": "minecraft:pink_concrete",
                                                     "R": "minecraft:redstone"}, "guhs:grijpmachine", 1)
    # tags
    add = h.add_tag
    add("guhs/tags/item/knus/marshmallow", ["guhs:marshmallow_knabbel"])
    add("guhs/tags/block/knus/knuffels", [f"guhs:knuffel_{k}" for k in KNUFFEL_IDS])
    add("minecraft/tags/block/mineable/axe", ["guhs:guh_xylofoon"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:grijpmachine"])
    # sounds
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{A}/sounds.json", patch)


# =====================================================================================================================
# advancements (tab guhs:knuffeldal) and the hidden quest ones
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["wereldleven_ijscoguh", "wereldleven_ijsje", "wereldleven_alle_ijsjes", "wereldleven_koortje", "wereldleven_liedjesboek",
                      "wereldleven_fluitje", "wereldleven_grijpmachine", "wereldleven_knuffelkast_vol", "wereldleven_knuffel", "wereldleven_kampvuur",
                      "wereldleven_gapen", "wereldleven_gezwaaid", "wereldleven_dutje", "seen_ijscoguh"]


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    tab = [("wereldleven_ijsje", "root", "guhs:kaasijsje_roze", "task"),
           ("wereldleven_koortje", "wereldleven_ijsje", "guhs:guh_xylofoon", "task"),
           ("wereldleven_liedjesboek", "wereldleven_koortje", "guhs:wereldleven_liedjesboekje", "goal"),
           ("wereldleven_grijpmachine", "wereldleven_ijsje", "guhs:grijpmachine", "task"),
           ("wereldleven_knuffelkast_vol", "wereldleven_grijpmachine", "guhs:knuffel_glitter", "challenge")]
    for name, parent, icon, frame in tab:
        title, desc = tekst.ADVANCEMENTS[name]
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": impossible})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


# =====================================================================================================================
# the game test rooms
# =====================================================================================================================
def test_templates(h):
    # a square with a xylofoon and a grijpmachine, far enough apart
    s = h.Structure((21, 6, 21))
    for x in range(21):
        for z in range(21):
            s.set(x, 0, z, "guhs:knuffelklinkers" if (x + z) % 4 else "minecraft:pink_wool")
    # (the campfire and the guh nest are put down by the tests themselves: then they are points of interest right away)
    s.set(10, 1, 10, "guhs:guh_xylofoon", {"facing": "south"})
    s.set(16, 1, 16, "guhs:grijpmachine", {"facing": "north", "half": "lower"})
    s.set(16, 2, 16, "guhs:grijpmachine", {"facing": "north", "half": "upper"})
    s.save("wereldleven_test_plein")
    # an empty floor without any nest or fire (naps on the spot)
    e = h.Structure((13, 5, 13))
    for x in range(13):
        for z in range(13):
            e.set(x, 0, z, "guhs:knuffelklinkers")
    e.save("wereldleven_test_leeg")
    # a long street: a little resident's house at one end (door towards the street), the campfire far away at the
    # other end (put down by the test): more than VUUR_ZOEK = 16 blocks, so only a resident finds it
    L, W = 40, 11
    st = h.Structure((L, 6, W))
    for x in range(L):
        for z in range(W):
            st.set(x, 0, z, "guhs:knuffelklinkers" if (x + z) % 5 else "minecraft:pink_wool")
    for x in range(1, 8):
        for z in range(2, 9):
            wand = x in (1, 7) or z in (2, 8)
            deur = x == 7 and z == 5
            for y in range(1, 4):
                if wand and not (deur and y < 3):
                    st.set(x, y, z, "minecraft:pink_terracotta" if y != 2 else "minecraft:white_wool")
            st.set(x, 4, z, "minecraft:pink_wool")
    st.save("wereldleven_test_lang")
    # self-check: the house has its door, and the street to the fire is open
    if st.get(7, 1, 5) is not None or st.get(7, 2, 5) is not None or st.get(7, 3, 5) is None:
        raise SystemExit("wereldleven_test_lang: the house needs a door (7, 1-2, 5)")
    if any(st.get(x, y, 5) is not None for x in range(8, L) for y in (1, 2)):
        raise SystemExit("wereldleven_test_lang: the street to the campfire must be open")


def selfcheck_assets(h):
    """check_assets.py only reads the registry classes: this checks our own blocks, items and the IJscoguh."""
    A = h.A
    blocks = ["guh_xylofoon", "grijpmachine"] + [f"knuffel_{k}" for k in KNUFFEL_IDS]
    items = ["guh_fluitje", "marshmallow_knabbel", "wereldleven_liedjesboekje", "ijscoguh_spawn_egg"] + [f"kaasijsje_{s}" for s in SMAKEN]
    missing = []
    for b in blocks:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in items:
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for f in ("geckolib/models/entity/ijscoguh.geo.json", "geckolib/animations/entity/ijscoguh.animation.json", "textures/entity/ijscoguh.png",
              "models/block/grijpmachine_klauw.json", "textures/entity/guh_clothes/wereldleven_ijshoedje.png"):
        if not os.path.exists(f"{A}/{f}") and "guh_clothes" not in f:
            missing.append(f)
    for root, _dirs, files in os.walk(f"{A}/models/block"):
        for f in files:
            if f.startswith(("knuffel_", "guh_xylofoon", "grijpmachine")):
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    if missing:
        raise SystemExit(f"wereldleven assets missing: {missing}")


def build(h):
    textures(h)
    modellen.knuffel_models(h, KNUFFEL_IDS)
    modellen.xylofoon_model(h)
    modellen.grijpmachine_models(h)
    swatches = modellen.ijscoguh(h)
    modellen.grijpmachine_stuk(h)
    items_and_data(h)
    advancements(h)
    for key, text in tekst.LANG.items():
        h.lang(key, text, text)
    test_templates(h)
    selfcheck_assets(h)
    print(f"wereldleven: {len(KNUFFEL_IDS)} plushies, ijscoguh swatches {len(swatches)}")


# =====================================================================================================================
# FTB quests (rows y = 98 and 99.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y1, y2 = fq.q, FTB_Y[0], FTB_Y[1]
    q("wereldleven_gapen", "Goedemorgen, guh!", "In de guhdorpen en het Knuffeldal leven de guhs een echte guhdag. 's Ochtends: een hele grote gaap en "
      "een rekje-strekje. Kijk maar eens 's morgens vroeg!", "minecraft:clock", [fq.adv("wereldleven_gapen")],
      rewards=(("guhs:kaas_knabbels", 4),), x=-8, y=y1, shape="circle", xp=50)
    q("wereldleven_gezwaaid", "Hoi hoi!", "Overdag zwaaien guhs naar je als je langskomt, ook je eigen tamme guhs (als ze vrij rondlopen).",
      "guhs:guh_spawn_egg", [fq.adv("wereldleven_gezwaaid")], rewards=(("guhs:kaas_knabbels", 4),), x=-6.5, y=y1, xp=50)
    q("wereldleven_dutje", "Middagdutje", "'s Middags doen de guhs een dutje: in een guhnestje, of opgekruld op de plek in een klein nestje. Zzz... "
      "niet wakker maken, njeg!", "guhs:guhnestje", [fq.adv("wereldleven_dutje")], rewards=(("guhs:kaas_knabbels", 6),), x=-5, y=y1, xp=50)
    q("wereldleven_kampvuur", "Marshmallows bij het kampvuur", "'s Avonds zitten de guhs rond het kampvuur. Geef er een een "
      "&dmarshmallowknabbel&r (stokje + suiker + kaasknabbel): hij roostert hem en smult hem op!", "guhs:marshmallow_knabbel",
      [fq.adv("wereldleven_kampvuur")], rewards=(("guhs:marshmallow_knabbel", 4),), x=-3.5, y=y1, xp=100)
    q("wereldleven_ijscoguh", "Tingeling!", "Hoor je een belletje? Dat is &dIJscoguh Tingeling&r op zijn ijscofiets! Hij komt af en toe langs in de "
      "Guhmensie (ook op het plein van het Knuffeldal) en de guhs rennen hem achterna.", "guhs:ijscoguh_spawn_egg",
      [fq.adv("wereldleven_ijscoguh")], rewards=(("guhs:kaas_knabbels", 8),), x=-2, y=y1, shape="gear", xp=100)
    q("wereldleven_ijsje", "Een kaasijsje!", "Koop een kaasijsje bij IJscoguh Tingeling en eet het op, of geef het aan een guh: die krijgt een ijshoedje, "
      "blosjes of gaat zweven!", "guhs:kaasijsje_roze", [fq.adv("wereldleven_ijsje")], rewards=(("guhs:kaas_knabbels", 6),), x=-0.5, y=y1, xp=100)
    q("wereldleven_alle_ijsjes", "Alle smaken", "Proef alle zeven kaasijsjes. Vier ervan zijn er alleen in hun eigen seizoen: bloesem (lente), "
      "zonnetje (zomer), appeltaart (herfst) en sneeuw (winter).", "guhs:kaasijsje_sneeuw", [fq.adv("wereldleven_alle_ijsjes")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 6),), x=1, y=y1, shape="gear", xp=300)
    q("wereldleven_guhdex", "IJscoguh in de Guhdex", "Ga dicht bij IJscoguh Tingeling staan: dan komt hij in je Guhdex.", "guhs:guhdex",
      [fq.adv("seen_ijscoguh")], rewards=(("guhs:kaas_knabbels", 4),), x=2.5, y=y1, shape="rsquare", xp=50)
    q("wereldleven_xylofoon", "Guh-xylofoon", "Maak een &dguh-xylofoon&r: acht gekleurde staafjes, van do tot do.", "guhs:guh_xylofoon",
      [fq.item("guhs:guh_xylofoon")], rewards=(("guhs:kaas_knabbels", 4),), x=-8, y=y2, shape="circle", xp=50)
    q("wereldleven_koortje", "Het koortje", "Speel een liedje uit het liedjesboekje op de guh-xylofoon terwijl er guhs in de buurt zijn: ze zingen "
      "allemaal mee! (Tuintjes in de buurt groeien er ook van.)", "guhs:wereldleven_liedjesboekje", [fq.adv("wereldleven_koortje")],
      rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y2, xp=100)
    q("wereldleven_fluitje", "Fluitje erbij", "Met het &dguh-fluitje&r fluit je je laatste liedje waar je maar wilt, en de guhs zingen mee.",
      "guhs:guh_fluitje", [fq.adv("wereldleven_fluitje")], rewards=(("guhs:kaas_knabbels", 4),), x=-5, y=y2, xp=50)
    q("wereldleven_liedjesboek", "Het hele liedjesboekje", "Speel alle zes de liedjes: dan krijg je het koorstrikje! VAHOEG!",
      "guhs:koorstrikje", [fq.adv("wereldleven_liedjesboek")], rewards=(("guhs:gefrituurde_kaasknabbels", 6),), x=-3.5, y=y2, shape="gear", xp=300)
    q("wereldleven_grijpmachine", "Grijpen maar!", "Op de guhkermis en onder de arcade van het Knuffeldal staat een &dgrijpmachine&r. Eén kaartje "
      "(een kermisbon of een munt van een minigame), en je stuurt zelf de klauw! Elke knuffel geeft ook een kermisbon terug.",
      "guhs:grijpmachine", [fq.adv("wereldleven_grijpmachine")], rewards=(("guhs:kermisbon", 3),), x=-2, y=y2, xp=100)
    q("wereldleven_knuffel", "Knuffeltijd", "Zet een guhknuffel neer: je tamme guhs (die vrij rondlopen) komen hem af en toe een dikke knuffel geven.",
      "guhs:knuffel_normal", [fq.adv("wereldleven_knuffel")], rewards=(("guhs:kaas_knabbels", 6),), x=-0.5, y=y2, xp=100)
    q("wereldleven_knuffelkast", "De volle knuffelkast", "Verzamel alle 22 guhknuffels, ook de zeldzame glitterknuffel. Je knuffelkast staat in je "
      "Guhdex (tab Knus).", "guhs:knuffel_glitter", [fq.adv("wereldleven_knuffelkast_vol")], rewards=(("guhs:vahoege_vads_ingot", 2),),
      x=1, y=y2, shape="gear", xp=500)
