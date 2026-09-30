"""
De Guhboerderij (2.8.0 "Knuffeldal", slice boerderij): the farm of Boerin Hooibaal in the guhweides and the kaasvlakte
(see guhs_work28/KNUFFEL_CONTRACT.md, par. 4.5).

  - the animals guhschaapje, knabbelkippetje and guhkoe: GeckoLib models, animations and textures
    (boerderij_dieren.py), spawn eggs, a rare natural spawn in guh_meadows / kaas_flats (a biome modifier), loot
  - Boerin Hooibaal (NPC kind BOERINNEGUH): her own model (straw hat, a straw, an apron), dialogue, daily chores, shop
  - items pluiswol, knabbelei, kaasmelk, guhborstel, knabbelvoer; blocks pluiswolblok, guh_voerbak, kippennestje
    (boerderij_tex.py); particles wolplukje, melkdruppel; sounds boerderij.*; the Knus tags pluiswol/knabbelei/kaasmelk
  - clothes boerderij_hoedje, boerderij_zakdoek
  - the structure guhboerderij (boerderij_bouw.py: three stalls that are giant guh animals, a yard, a moestuin, a
    windmill, an entrance arch) with its geometry self-check
  - advancements (tab guhs:knuffeldal: boerderij_gevonden, boerderij_verzorgd, boerderij_alle_producten), the Guhdex
    pages, the Knus section texts, FTB quests (row y = 89), the game test room
"""
import os

from features import boerderij_bouw as bouw
from features import boerderij_dieren as dieren
from features import boerderij_tex as tex

FTB_Y = 89
CLOTHES = ["boerderij_hoedje", "boerderij_zakdoek"]
ANIMALS = ["guhschaapje", "knabbelkippetje", "guhkoe"]
SOUNDS = {
    "boerderij.schaapje_bleh": [{"name": "minecraft:entity.sheep.ambient", "type": "event", "pitch": 1.45, "volume": 0.8}],
    "boerderij.kippetje_tok": [{"name": "minecraft:entity.chicken.ambient", "type": "event", "pitch": 1.35, "volume": 0.8}],
    "boerderij.koe_moeh": [{"name": "minecraft:entity.cow.ambient", "type": "event", "pitch": 1.3, "volume": 0.8}],
}

# =====================================================================================================================
# clothes (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    "outfit_boerderij_hoedje": ("head", _H, "boerderij_hoed", [([-7, 15.2, -12], [14, 0.6, 11], 0), ([-4, 15.8, -9.5], [8, 3, 6.5], 0),
                                                              ([-3.4, 18.8, -8.9], [6.8, 0.6, 5.3], 0)]),
    "outfit_boerderij_hoedje_band": ("head", _H, "boerderij_band", [([-4, 15.8, -9.5], [8, 1, 6.5], 0.12)]),
}


def clothes(rng, v):
    np = __import__("numpy")

    def hoed():
        a = v.straw(rng)
        return a

    def band():
        a = v.fabric((212, 50, 60), rng, 6)
        a[::8, ::8] = (255, 250, 250)
        return a

    def zakdoek():
        a = v.fabric((212, 50, 60), rng, 6)
        for y in range(2, 32, 7):
            for x in range(2 + (y // 7 % 2) * 3, 32, 7):
                a[y:y + 2, x:x + 2] = (255, 250, 250)
        return np.clip(a, 0, 255)

    return {"boerderij_hoedje": {"boerderij_hoed": hoed, "boerderij_band": band},
            "boerderij_zakdoek": {"scarf": zakdoek}}


def icons(ic):
    return {"boerderij_hoedje": ic.shaped("rim_hat", (170, 130, 50), (236, 204, 112), (212, 50, 60)),
            "boerderij_zakdoek": ic.shaped("scarf", (150, 30, 40), (212, 50, 60), (255, 250, 250))}


# =====================================================================================================================
# blocks, items, loot, recipes, tags
# =====================================================================================================================
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def el(frm, to, tex_, faces=None, uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": tex_, "uv": uv} if uv else {"texture": tex_})
                                              for f in (faces or ("down", "up", "north", "south", "west", "east"))}}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    h.simple_block("pluiswolblok")
    # the voerbak: a trough with a guh face on the front (north in the model), feed in it by level
    t = {"particle": "guhs:block/guh_voerbak", "hout": "guhs:block/guh_voerbak", "voor": "guhs:block/guh_voerbak_voorkant",
         "voer": "guhs:block/guh_voerbak_voer"}
    for voer in range(5):
        els = [el([1, 0, 1], [15, 2, 15], "#hout"),
               {"from": [1, 2, 1], "to": [15, 8, 2.5], "faces": {"north": {"texture": "#voor", "uv": [0, 4, 16, 13]},
                                                                 "south": {"texture": "#hout"}, "up": {"texture": "#hout"},
                                                                 "west": {"texture": "#hout"}, "east": {"texture": "#hout"}}},
               el([1, 2, 13.5], [15, 8, 15], "#hout"), el([1, 2, 2.5], [2.5, 8, 13.5], "#hout"), el([13.5, 2, 2.5], [15, 8, 13.5], "#hout"),
               el([1.5, 0, 1.5], [3, 1, 3], "#hout")]
        if voer:
            els.append(el([2.5, 2, 2.5], [13.5, 2 + 1.4 * voer, 13.5], "#voer", faces=("up", "north", "south", "west", "east")))
        w(f"{A}/models/block/guh_voerbak_{voer}.json", {"parent": "minecraft:block/block", "textures": t, "elements": els})
    w(f"{A}/blockstates/guh_voerbak.json", {"variants": {
        f"facing={f},voer={v}": {"model": f"guhs:block/guh_voerbak_{v}", **({"y": r} if r else {})} for f, r in ROT.items() for v in range(5)}})
    w(f"{A}/models/item/guh_voerbak.json", {"parent": "guhs:block/guh_voerbak_3"})
    # the kippennestje: a straw ring with eggs in it
    tn = {"particle": "guhs:block/kippennestje", "stro": "guhs:block/kippennestje", "ei": "guhs:block/kippennestje_ei"}
    eggs = [([4, 1, 4], [7, 4.5, 7]), ([9, 1, 7], [12, 4.5, 10]), ([5, 1, 9.5], [8, 4, 12.5])]
    for n in range(4):
        els = [el([1, 0, 1], [15, 1, 15], "#stro"), el([1, 1, 1], [15, 4, 3], "#stro"), el([1, 1, 13], [15, 4, 15], "#stro"),
               el([1, 1, 3], [3, 4, 13], "#stro"), el([13, 1, 3], [15, 4, 13], "#stro")]
        for (frm, to) in eggs[:n]:
            els.append(el(frm, to, "#ei"))
        w(f"{A}/models/block/kippennestje_{n}.json", {"parent": "minecraft:block/block", "textures": tn, "elements": els})
    w(f"{A}/blockstates/kippennestje.json", {"variants": {f"eieren={n}": {"model": f"guhs:block/kippennestje_{n}"} for n in range(4)}})
    w(f"{A}/models/item/kippennestje.json", {"parent": "guhs:block/kippennestje_2"})
    for i in ("pluiswol", "knabbelei", "kaasmelk", "guhborstel", "knabbelvoer"):
        h.item_model(i)
    h.item_model("guhborstel", parent="minecraft:item/handheld")
    for a in ANIMALS:
        w(f"{A}/models/item/{a}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # loot
    for b in ("pluiswolblok", "guh_voerbak", "kippennestje"):
        h.self_drop(b)
    for a, item, lo, hi in (("guhschaapje", "guhs:pluiswol", 1, 2), ("knabbelkippetje", "minecraft:feather", 0, 1), ("guhkoe", "guhs:kaas_knabbels", 0, 2)):
        w(f"{D}/loot_table/entities/{a}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": item, "functions": h.count_fn(lo, hi)}]}]})
    w(f"{D}/loot_table/chests/guhboerderij.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            {"type": "minecraft:item", "name": "guhs:knabbelvoer", "weight": 6, "functions": h.count_fn(3, 8)},
            {"type": "minecraft:item", "name": "guhs:pluiswol", "weight": 4, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:knabbelei", "weight": 4, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:kaasmelk", "weight": 2, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "guhs:knabbelzaadjes", "weight": 4, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:theekruidzaadjes", "weight": 3, "functions": h.count_fn(2, 4)},
            {"type": "minecraft:item", "name": "guhs:guhbloemzaadjes", "weight": 3, "functions": h.count_fn(2, 4)},
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(4, 10)},
            {"type": "minecraft:item", "name": "minecraft:hay_block", "weight": 3, "functions": h.count_fn(1, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:guhborstel", "weight": 2},
                                 {"type": "minecraft:item", "name": "guhs:guh_gieter", "weight": 2},
                                 {"type": "minecraft:item", "name": "guhs:guh_bloempot", "weight": 3, "functions": h.count_fn(1, 2)},
                                 {"type": "minecraft:item", "name": "guhs:boerderij_zakdoek", "weight": 1},
                                 {"type": "minecraft:empty", "weight": 3}]}]})
    # recipes
    h.shaped("pluiswolblok", ["WW", "WW"], {"W": "guhs:pluiswol"}, "guhs:pluiswolblok")
    h.shapeless("pluiswol_uit_blok", ["guhs:pluiswolblok"], "guhs:pluiswol", 4)
    h.shaped("guh_voerbak", ["PDP", "PPP"], {"P": "#minecraft:planks", "D": "minecraft:pink_dye"}, "guhs:guh_voerbak")
    h.shaped("kippennestje", ["W W", "WWW"], {"W": "minecraft:wheat"}, "guhs:kippennestje")
    h.shaped("guhborstel", [" W", "S "], {"W": "#minecraft:wool", "S": "minecraft:stick"}, "guhs:guhborstel")
    h.shapeless("knabbelvoer", ["minecraft:wheat_seeds", "minecraft:wheat_seeds", "guhs:kaas_knabbels"], "guhs:knabbelvoer", 4)
    h.shapeless("knabbelvoer_uit_graan", ["#guhs:knus/knabbelgraan", "guhs:kaas_knabbels"], "guhs:knabbelvoer", 6)
    # tags
    add = h.add_tag
    add("guhs/tags/item/knus/pluiswol", ["guhs:pluiswol"])
    add("guhs/tags/item/knus/knabbelei", ["guhs:knabbelei"])
    add("guhs/tags/item/knus/kaasmelk", ["guhs:kaasmelk"])
    add("minecraft/tags/block/mineable/axe", ["guhs:guh_voerbak"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:kippennestje"])
    add("minecraft/tags/block/wool", ["guhs:pluiswolblok"])
    add("minecraft/tags/item/wool", ["guhs:pluiswolblok"])
    # the animals now and then also live wild in the guhweides and the kaasvlakte (rare)
    w(f"{D}/neoforge/biome_modifier/boerderij_dieren.json", {
        "type": "neoforge:add_spawns", "biomes": ["guhs:guh_meadows", "guhs:kaas_flats"],
        "spawners": [{"type": "guhs:guhschaapje", "weight": 2, "minCount": 2, "maxCount": 3},
                     {"type": "guhs:knabbelkippetje", "weight": 2, "minCount": 2, "maxCount": 4},
                     {"type": "guhs:guhkoe", "weight": 1, "minCount": 1, "maxCount": 2}]})


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the structure and the game test room
# =====================================================================================================================
def structure(h):
    h.TEMPLATE_SIZES[bouw.NAME] = 30
    h.FLATNESS[bouw.NAME] = 10
    creatures = {"bounding_box": "piece", "spawns": [
        {"type": "guhs:guhschaapje", "weight": 3, "minCount": 1, "maxCount": 2},
        {"type": "guhs:knabbelkippetje", "weight": 3, "minCount": 1, "maxCount": 2},
        {"type": "guhs:guhkoe", "weight": 1, "minCount": 1, "maxCount": 1}]}
    h.structure(bouw.NAME, ["guh_meadows", "kaas_flats"], spacing=26, separation=9, salt=20280501, start_y=-bouw.G, reach=48,
                centre=bouw.ANCHOR, spawn_overrides={"monster": {"bounding_box": "piece", "spawns": []}, "creature": creatures})
    b = bouw.build(h)
    problems = bouw.check(b)
    if problems:
        raise SystemExit("guhboerderij geometry check failed:\n  " + "\n  ".join(problems[:40]))
    b.s.save(bouw.NAME)
    print(f"guhboerderij: geometry check ok ({b.walkable} walkable spots, {len(b.s.blocks)} blocks, {b.gezichten} guh faces, {b.dieren})")
    # the game test room: a little fenced meadow
    t = h.Structure((12, 6, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("boerderij_test_wei")


# =====================================================================================================================
# advancements and texts
# =====================================================================================================================
QUEST_ADVANCEMENTS = ["boerderij_hooibaal", "boerderij_verzorgd", "boerderij_alle_producten", "boerderij_klusje", "boerderij_klusjes"]
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS + [f"seen_{p}" for p in ("boerinneguh", *ANIMALS)]:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    for name, parent, icon, frame, crit, title, desc in [
        ("boerderij_gevonden", "root", "guhs:pluiswolblok", "task",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:guhboerderij"}}}}},
         "Boer zoekt Guh", "Vind de Guhboerderij van Boerin Hooibaal, in de guhweides of de kaasvlakte"),
        ("boerderij_verzorgd", "boerderij_gevonden", "guhs:guhborstel", "task", IMPOSSIBLE,
         "Blij beestje", "Maak een boerderijdier blij: aaien, borstelen en voeren (twee van de drie op een dag)"),
        ("boerderij_alle_producten", "boerderij_verzorgd", "guhs:kaasmelk", "goal", IMPOSSIBLE,
         "Wol, ei en melk", "Krijg pluiswol, een knabbelei en kaasmelk van je blije boerderijdieren. VAHOEG!"),
    ]:
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


KLUSJES = {
    "aaien": ("Njeg, vandaag wil ik dat je %s dieren aait. Gewoon met je hand, lief over hun kopje. Ze worden er zo blij van!",
              "Nog even aaien hoor: %s van de %s dieren geaaid. Njeg!"),
    "borstelen": ("Klusje van vandaag: borstel %s dieren met een guhborstel. Pluizig = gelukkig, dat weet iedereen!",
                  "Borstelen, borstelen! %s van de %s gedaan. Heb je wel een guhborstel? Ik verkoop ze, hoor."),
    "voeren": ("Vandaag: geef %s dieren knabbelvoer. Uit je hand, dat vinden ze het allerleukst. VAHOEG!",
               "Nog wat voeren: %s van de %s dieren hebben knabbelvoer gehad."),
    "voerbak": ("Mijn voerbakken zijn leeg, njeg! Doe er %s keer knabbelvoer in, dan eten de dieren vanzelf.",
                "De voerbakken: %s van de %s scheppen gedaan. Rechtsklik met knabbelvoer op een voerbak!"),
    "wol": ("Kun je me %s pluiswol brengen? Blije guhschaapjes laten hun wol vanzelf los. Voor de creche, die breit dekentjes!",
            "Ik wacht nog op pluiswol: je hebt er %s van de %s bij je."),
    "eieren": ("Breng me %s knabbeleitjes, lieverd. Blije knabbelkippetjes leggen ze in de kippennestjes. Voor de bakker!",
               "Knabbeleitjes: %s van de %s. Kijk eens in de kippennestjes?"),
    "melk": ("Vandaag wil ik %s flesjes kaasmelk. Een blije guhkoe geeft kaasmelk als je er een leeg flesje bij houdt. Moeh!",
             "Kaasmelk: %s van de %s flesjes. De guhkoe moet wel eerst blij zijn, njeg."),
    "oogst": ("Breng me %s dingen uit je tuintje: knabbelgraan, theekruid of guhbloemetjes. Zelf gekweekt is extra vahoeg!",
              "Oogst uit je tuintje: %s van de %s. Een gieter helpt, en tamme guhs helpen ook!"),
}

TEXTS = {
    "block.guhs.pluiswolblok": "Pluiswolblok",
    "block.guhs.guh_voerbak": "Guhvoerbak",
    "block.guhs.guh_voerbak.lore": "Doe er knabbelvoer in: de boerderijdieren eten er vanzelf uit",
    "block.guhs.kippennestje": "Kippennestje",
    "block.guhs.kippennestje.lore": "Blije knabbelkippetjes leggen hier hun knabbelei in",
    "item.guhs.pluiswol": "Pluiswol",
    "item.guhs.pluiswol.lore": "Superzacht, van een blij guhschaapje",
    "item.guhs.knabbelei": "Knabbelei",
    "item.guhs.knabbelei.lore": "Een eitje van een blij knabbelkippetje, knapperig als een kaasknabbel",
    "item.guhs.kaasmelk": "Kaasmelk",
    "item.guhs.kaasmelk.lore": "Romig en een beetje kazig. Van een blije guhkoe. Moeh!",
    "item.guhs.guhborstel": "Guhborstel",
    "item.guhs.guhborstel.lore": "Borstel de boerderijdieren: pluizig is gelukkig",
    "item.guhs.knabbelvoer": "Knabbelvoer",
    "item.guhs.knabbelvoer.lore": "Lekker voor schaapjes, kippetjes en koeien (en stiekem ook voor guhs)",
    "item.guhs.guhschaapje_spawn_egg": "Guhschaapje-spawnei",
    "item.guhs.knabbelkippetje_spawn_egg": "Knabbelkippetje-spawnei",
    "item.guhs.guhkoe_spawn_egg": "Guhkoe-spawnei",
    "item.guhs.boerderij_hoedje": "Boerderijhoedje",
    "item.guhs.boerderij_zakdoek": "Boerderijzakdoek",
    "entity.guhs.guhschaapje": "Guhschaapje",
    "entity.guhs.knabbelkippetje": "Knabbelkippetje",
    "entity.guhs.guhkoe": "Guhkoe",
    "entity.guhs.guh_npc.boerinneguh": "Boerin Hooibaal",
    "structure.guhs.guhboerderij": "Guhboerderij",
    "structure.guhs.guhboerderij.tooltip": "De boerderij van Boerin Hooibaal: guhschaapjes, knabbelkippetjes, guhkoeien en een moestuin",
    "subtitles.guhs.boerderij.schaapje_bleh": "Guhschaapje blaat: bleh!",
    "subtitles.guhs.boerderij.kippetje_tok": "Knabbelkippetje: tok tok",
    "subtitles.guhs.boerderij.koe_moeh": "Guhkoe: moeh!",
    # the Guhdex pages
    "gui.guhs.guhdex.rarity.boerinneguh": "Zeldzaamheid: Zeldzaam (Guhboerderij)",
    "gui.guhs.guhdex.info.boerinneguh": "Boerin Hooibaal zorgt voor alle boerderijdieren, met een strootje in haar mond. Elke dag heeft ze een klusje voor je, en ze verkoopt borstels, voer, zaadjes en gieters. Njeg, werk aan de winkel!",
    "gui.guhs.guhdex.rarity.guhschaapje": "Zeldzaamheid: Zeldzaam (Guhboerderij, soms in de guhweides)",
    "gui.guhs.guhdex.info.guhschaapje": "Een wolkje pluiswol met een guhkopje en ronde guhoortjes. Aai, borstel en voer het: een blij schaapje laat een plukje pluiswol los. Bleh!",
    "gui.guhs.guhdex.rarity.knabbelkippetje": "Zeldzaamheid: Zeldzaam (Guhboerderij, soms in de kaasvlakte)",
    "gui.guhs.guhdex.info.knabbelkippetje": "Een rond kaasgeel kippetje met grote guhogen. Als het blij is, legt het een knabbelei in een kippennestje. Tok tok, vahoeg!",
    "gui.guhs.guhdex.rarity.guhkoe": "Zeldzaamheid: Zeldzaam (Guhboerderij)",
    "gui.guhs.guhdex.info.guhkoe": "Een vadsige koe met kaasvlekken (met gaatjes!), een roze guhsnoet en twee hoorntjes. Maak haar blij en houd er een leeg flesje bij: kaasmelk! Moeh!",
    # messages
    "gui.guhs.boerderij.blij.guhschaapje": "Het guhschaapje is helemaal blij! Bleh-bleh!",
    "gui.guhs.boerderij.blij.knabbelkippetje": "Het knabbelkippetje is helemaal blij! Tok tok tok!",
    "gui.guhs.boerderij.blij.guhkoe": "De guhkoe is helemaal blij! Houd er een leeg flesje bij voor kaasmelk. Moeh!",
    "gui.guhs.boerderij.koe.niet_blij": "De guhkoe is nog niet blij genoeg... Aai, borstel of voer haar eerst. Njeg.",
    "gui.guhs.boerderij.koe.al_gemolken": "Deze guhkoe heeft vandaag al kaasmelk gegeven. Morgen weer, moeh!",
    "gui.guhs.boerderij.voerbak.vol": "De voerbak zit al tjokvol knabbelvoer!",
    "gui.guhs.boerderij.klus_klaar": "Klusje klaar! Ga terug naar Boerin Hooibaal. VAHOEG!",
    "gui.guhs.boerderij.klus_stand": "Klusje: %s / %s",
    "gui.guhs.boerderij.beschermd": "Njeg! Dit is de boerderij van Boerin Hooibaal: hier breek of bouw je niks.",
    "quest.guhs.boerderij.hallo": "Njeg, hallo daar! Ik ben Boerin Hooibaal. Welkom op de Guhboerderij! Mijn schaapjes, kippetjes en koeien worden pas blij als je ze aait, borstelt en voert. En blije dieren geven de lekkerste dingen: pluiswol, knabbeleitjes en kaasmelk. VAHOEG!",
    "quest.guhs.boerderij.al_klaar": "Je hebt je klusje van vandaag al gedaan, lieverd. Morgen heb ik weer iets voor je! Kijk gerust even in mijn winkeltje.",
    "quest.guhs.boerderij.bedankt0": "Njeg, wat goed! Hier, voor jou: kaasknabbels, knabbelvoer en een zakje zaadjes. VAHOEG!",
    "quest.guhs.boerderij.bedankt1": "Klusje gedaan? Dan ben jij de vahoegste boerenhulp van de Guhmensie! Een beloninkje voor jou.",
    "quest.guhs.boerderij.bedankt2": "Kijk die blije dieren eens! Dat heb jij gedaan. Hier, wat knabbels en zaadjes voor je tuintje.",
    "quest.guhs.boerderij.bedankt3": "Dankjewel! Als die Mika's nou eens van mijn kaasknabbels afbleven, dan werden we allemaal nog vahoeger... Hier, pak aan!",
    # the Knus tab
    "gui.guhs.knus.mijlpaal.boerderij_aaien": "Tien dieren geaaid",
    "gui.guhs.knus.mijlpaal.boerderij_borstelen": "Tien dieren geborsteld",
    "gui.guhs.knus.mijlpaal.boerderij_voeren": "Tien dieren gevoerd",
    "gui.guhs.knus.mijlpaal.boerderij_blij": "Vijf blije boerderijdieren",
    "gui.guhs.knus.mijlpaal.boerderij_producten": "25 boerderijproducten",
    "gui.guhs.knus.mijlpaal.boerderij_alle_producten": "Wol, ei en melk",
    "gui.guhs.knus.mijlpaal.boerderij_klusjes": "Zeven klusjes voor Boerin Hooibaal",
}
for _id, (_start, _nog) in KLUSJES.items():
    TEXTS[f"quest.guhs.boerderij.klus.{_id}"] = _start
    TEXTS[f"quest.guhs.boerderij.nog.{_id}"] = _nog


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def selfcheck_assets(h):
    A, D = h.A, h.D
    missing = []
    for b in ("pluiswolblok", "guh_voerbak", "kippennestje"):
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("pluiswol", "knabbelei", "kaasmelk", "guhborstel", "knabbelvoer", *[f"{a}_spawn_egg" for a in ANIMALS], *CLOTHES):
        if not os.path.exists(f"{A}/models/item/{i}.json") and i not in CLOTHES or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for a in ANIMALS:
        for p in (f"{A}/geckolib/models/entity/{a}.geo.json", f"{A}/geckolib/animations/entity/{a}.animation.json", os.path.join(h.TEX, "entity", f"{a}.png")):
            if not os.path.exists(p):
                missing.append(p)
        for k in (f"entity.guhs.{a}", f"gui.guhs.guhdex.rarity.{a}", f"gui.guhs.guhdex.info.{a}"):
            if k not in h.NL:
                missing.append(k)
    for p in (f"{A}/geckolib/models/entity/guh_npc_boerinneguh.geo.json", os.path.join(h.TEX, "entity", "npc_boerinneguh.png")):
        if not os.path.exists(p):
            missing.append(p)
    for f in os.listdir(f"{A}/models/block"):
        if f.startswith(("pluiswolblok", "guh_voerbak", "kippennestje")):
            import json
            for t in json.load(open(f"{A}/models/block/{f}", encoding="utf-8")).get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(os.path.join(h.TEX, t[5:] + ".png")):
                    missing.append(f"texture {t} of {f}")
    missing += dieren.check(h)
    if missing:
        raise SystemExit(f"boerderij assets missing: {missing}")


def build(h):
    h.ms = __import__("make_structures")
    tex.textures(h)
    dieren.build(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    structure(h)
    selfcheck_assets(h)


# =====================================================================================================================
# FTB quests (row y = 89, no dependencies)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, y = fq.q, fq.item, fq.adv, FTB_Y
    q("boerderij_vind", "De Guhboerderij",
      "Ergens in de guhweides of de kaasvlakte staat een boerderij met stallen in de vorm van reuzendieren: de &dGuhboerderij&r. "
      "Het superkompas (categorie Knus) wijst de weg.", "guhs:pluiswolblok", [fq.structure("guhboerderij")],
      rewards=(("guhs:knabbelvoer", 8),), x=-8, y=y, shape="circle", xp=100)
    q("boerderij_hooibaal", "Boerin Hooibaal", "Praat met &dBoerin Hooibaal&r in de koeienschuur (de reuzenkoe!). Ze heeft elke dag een klusje voor je.",
      "guhs:guhborstel", [adv("boerderij_hooibaal")], rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y, xp=100)
    q("boerderij_dieren", "Boerderijvriendjes", "Zet het guhschaapje, het knabbelkippetje, de guhkoe en Boerin Hooibaal in je Guhdex: ga er vlak naast staan.",
      "guhs:guhdex", [adv("seen_guhschaapje"), adv("seen_knabbelkippetje"), adv("seen_guhkoe"), adv("seen_boerinneguh")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-5, y=y, shape="rsquare", xp=150)
    q("boerderij_verzorgd", "Blij beestje", "Aai een boerderijdier (lege hand), borstel het (guhborstel) of voer het (knabbelvoer). "
      "Twee van de drie op een dag en het is &dhelemaal blij&r!", "guhs:guhborstel", [adv("boerderij_verzorgd")],
      rewards=(("guhs:knabbelvoer", 8),), x=-3.5, y=y, xp=100)
    q("boerderij_pluiswol", "Pluiswol", "Een blij guhschaapje laat een plukje &fpluiswol&r los.", "guhs:pluiswol", [item("guhs:pluiswol", 4)],
      rewards=(("guhs:kaas_knabbels", 8),), x=-2, y=y)
    q("boerderij_knabbelei", "Knabbelei", "Een blij knabbelkippetje legt een &eknabbelei&r in het dichtstbijzijnde kippennestje. Pak het eruit!",
      "guhs:knabbelei", [item("guhs:knabbelei", 3)], rewards=(("guhs:kaas_knabbels", 8),), x=-0.5, y=y)
    q("boerderij_kaasmelk", "Kaasmelk", "Maak een guhkoe blij en houd er een leeg flesje bij: &ekaasmelk&r! Moeh!", "guhs:kaasmelk",
      [item("guhs:kaasmelk", 2)], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y)
    q("boerderij_alles", "Wol, ei en melk", "Krijg alle drie de boerderijproducten van je eigen blije dieren. De bakker en het theehuis zijn er dol op!",
      "guhs:kaasmelk", [adv("boerderij_alle_producten")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=2.5, y=y, shape="gear", xp=200)
    q("boerderij_voerbak", "Vanzelf voeren", "Maak een &dguhvoerbak&r en doe er knabbelvoer in. Hongerige dieren in de buurt eten er dan vanzelf uit.",
      "guhs:guh_voerbak", [item("guhs:guh_voerbak")], rewards=(("guhs:knabbelvoer", 16),), x=4, y=y)
    q("boerderij_klusje", "Boerenhulp", "Doe het klusje van de dag voor Boerin Hooibaal.", "guhs:knabbelvoer", [adv("boerderij_klusje")],
      rewards=(("guhs:kaas_knabbels", 12),), x=5.5, y=y, xp=100)
    q("boerderij_klusjes", "Vaste boerenhulp", "Doe zeven klusjes voor Boerin Hooibaal (een per dag). Dan krijg je haar boerderijhoedje in je Guhdex (Knus)!",
      "guhs:boerderij_hoedje", [adv("boerderij_klusjes")], rewards=(("guhs:vahoege_vads_ingot", 1),), x=7, y=y, shape="gear", xp=300)
