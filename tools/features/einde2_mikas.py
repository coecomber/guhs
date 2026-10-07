"""
einde2 (the Guheinde expansion), slice mikas: the Ender-Mika, the Kaasmijt, the Schuilguh and the Schuildoos (DESIGN_EINDE2 §3).
Java: nl.juiced.guhs.feature.einde2.mikas (E2MikasFeature, EnderMikaEntity, KaasmijtEntity + Kaasmijten + HikEffect,
SchuilguhEntity, SchuildoosBlock + its block entity, menu and item).

  build(h)   the models/animations/textures (einde2_mikas_modellen), the item icons, the Schuildoos (textures, models, loot that
             keeps the contents on the item, recipe), the flits particle and the hik's icon, sounds, the Ender-Mika's spawn
             (biome modifier einde2_mikas_ender_mika) and its loot, the tags, the Guhdex pages, the texts (Dutch in both
             files), the two hidden advancements, the game test room (einde2_mikas_test_kamer), and a self-check
  ftb(fq)    the quests of the section "Ender-Mika's & Schuilguhs" (see / find / make; nothing locked)

The creatures: ender_mika (the outer islands, by itself: weight 2; shy, teleports, steals ONE knabbel and always gives it
back; drops 0-1 flitskruimel), kaasmijt (never by itself: a knabbelkoorvrucht, an egg; tickles = the hik; gone after two
minutes), schuilguh (never by itself: buildings and eggs; a kusje = you float; a knabbel = a doosschil, once a day).
English: tools/lang/en/c53_e2_mikas.json; wiki texts: einde2_mikas_wiki.py.
"""
import os

import numpy as np
from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import einde2_mikas_modellen as modellen

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "Ender-Mika's & Schuilguhs"
FTB_PORTRAIT = "geo:schuilguh:schuilguh"

DIEREN = ["ender_mika", "kaasmijt", "schuilguh"]
EIEREN = {"ender_mika": ("Ender-Mika-spawnei", 0x1A0F2A, 0xEE80FF), "kaasmijt": ("Kaasmijt-spawnei", 0xFAD660, 0xD684FF),
          "schuilguh": ("Schuilguh-spawnei", 0xE2BE82, 0xD6B2DE)}

# =====================================================================================================================
# texts (Dutch in both files)
# =====================================================================================================================
DEX = {  # page -> (name, rarity, info)
    "ender_mika": ("Ender-Mika", "Zeldzaam, op de buiteneilanden van het Guheinde",
                   "Een Mika die zo lang in de leegte heeft staan staren dat hij helemaal is uitgerekt: lang, dun en paarszwart, met "
                   "gloeiende oogjes. Hij flitst van eiland naar eiland en is dol op kaasknabbels: heb je er een in je hand, dan sluipt "
                   "hij achter je langs en jat er precies één. Hij is vreselijk verlegen: kijk hem twee tellen strak aan en hij laat "
                   "je knabbel van schrik weer vallen. Mep je hem, dan duwt hij je om en flitst hij weg. Loopt er een Kaasmijt rond, "
                   "dan vergeet hij alles en sjokt hij er verliefd achteraan. NJEG."),
    "kaasmijt": ("Kaasmijt", "Soms uit een knabbelkoorvrucht",
                 "Een piepklein beestje van kaaskruimels, met zes pootjes en twee gloeiende voelsprietjes. Af en toe springt er een uit "
                 "een knabbelkoorvrucht waar je net een hap van nam. Het trippelt naar je toe en kietelt je tenen, en daar krijg je de "
                 "hik van. Hik! Na een paar minuutjes is het weer weg. Ender-Mika's vinden Kaasmijten het mooiste wat er bestaat en "
                 "lopen er dromerig achteraan: met een mijtje in de buurt zijn je knabbels dus veilig."),
    "schuilguh": ("Schuilguh", "Op de muren van een Mika-vesting",
                  "Een lieve, verlegen guh die in een rond kaasdoosje woont, vastgeplakt aan een muur, het plafond of gewoon de vloer. "
                  "Kom je rustig dichterbij, dan gluurt hij onder zijn dekseltje vandaan. Met een lege hand krijg je een kusje: daar ga "
                  "je even van zweven (je landt zacht, het blijft een guh). Geef je hem een kaasknabbel, dan schuift hij één keer per "
                  "dag een doosschil naar buiten. Schrikt hij, dan klapt zijn deksel dicht. Toe!"),
}

LANG = {
    "item.guhs.flitskruimel": "Flitskruimel",
    "item.guhs.flitskruimel.lore": "Een kruimel die nog naflitst van een Ender-Mika. Knipper met je ogen en hij ligt een stukje verder. Njeg.",
    "item.guhs.doosschil": "Doosschil",
    "item.guhs.doosschil.lore": "Een rond stuk kaasdoos, door een Schuilguh naar buiten geschoven voor een knabbel. Twee om een kist: een Schuildoos.",
    "block.guhs.schuildoos": "Schuildoos",
    "block.guhs.schuildoos.tooltip": "Houdt alles vast wat erin zit, ook als je hem oppakt. Past niet in een andere Schuildoos (een guh heeft "
                                     "het geprobeerd).",
    "effect.guhs.hik": "Hik",
    # what happens (overlay lines)
    "gui.guhs.einde2_mikas.gejat": "Hé! Een Ender-Mika jat een knabbel uit je hand. Kijk hem strak aan, dan laat hij hem vallen. NJEG!",
    "gui.guhs.einde2_mikas.laat_vallen": "De Ender-Mika laat je knabbel van schrik vallen. Gauw oprapen!",
    "gui.guhs.einde2_mikas.terug": "De Ender-Mika durfde hem toch niet op te eten: je knabbel zit weer in je zak.",
    "gui.guhs.einde2_mikas.mijt_uit_vrucht": "Plop! Er springt een Kaasmijt uit je vrucht. Pas op je tenen...",
    "gui.guhs.einde2_mikas.gekieteld": "Kiele kiele! De Kaasmijt kietelt je tenen. Hik!",
    "gui.guhs.einde2_mikas.kusje": "Smak! Een kusje van de Schuilguh. Je zweeft er even van...",
    "gui.guhs.einde2_mikas.schil": "Krak! De Schuilguh schuift een doosschil naar buiten. Dank je wel, guh!",
    "gui.guhs.einde2_mikas.schil_morgen": "Deze Schuilguh gaf vandaag al een doosschil. Morgen is er weer een aangegroeid, njeg.",
    "gui.guhs.einde2_mikas.schuilguh_dicht": "Toe! De Schuilguh zit even in zijn doosje. Wacht tot hij weer gluurt.",
    # sounds
    "subtitles.guhs.einde2_mikas.ender_mika.flits": "Ender-Mika flitst weg",
    "subtitles.guhs.einde2_mikas.ender_mika.jat": "Ender-Mika jat een knabbel",
    "subtitles.guhs.einde2_mikas.ender_mika.betrapt": "Ender-Mika is betrapt",
    "subtitles.guhs.einde2_mikas.ender_mika.zwijmel": "Ender-Mika zwijmelt",
    "subtitles.guhs.einde2_mikas.kaasmijt.piep": "Kaasmijt piept",
    "subtitles.guhs.einde2_mikas.kaasmijt.kietel": "Kaasmijt kietelt",
    "subtitles.guhs.einde2_mikas.kaasmijt.plop": "Kaasmijt plopt",
    "subtitles.guhs.einde2_mikas.hik": "Hik!",
    "subtitles.guhs.einde2_mikas.schuilguh.open": "Schuilguh gluurt",
    "subtitles.guhs.einde2_mikas.schuilguh.dicht": "Schuilguh klapt dicht",
    "subtitles.guhs.einde2_mikas.schuilguh.kusje": "Schuilguh geeft een kusje",
    "subtitles.guhs.einde2_mikas.schuilguh.schil": "Doosschil kraakt los",
    "subtitles.guhs.einde2_mikas.schuildoos.open": "Schuildoos gaat open",
    "subtitles.guhs.einde2_mikas.schuildoos.dicht": "Schuildoos gaat dicht",
}

SOUNDS = {
    "einde2_mikas.ender_mika.flits": [{"name": "minecraft:entity.enderman.teleport", "type": "event", "pitch": 1.6, "volume": 0.7}],
    "einde2_mikas.ender_mika.jat": [{"name": "guhs:entity.mika.ambient", "type": "event", "pitch": 1.7},
                                    {"name": "minecraft:entity.item.pickup", "type": "event", "pitch": 0.7}],
    "einde2_mikas.ender_mika.betrapt": [{"name": "guhs:entity.mika.hurt", "type": "event", "pitch": 1.8, "volume": 0.7}],
    "einde2_mikas.ender_mika.zwijmel": [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 0.7, "volume": 0.7}],
    "einde2_mikas.kaasmijt.piep": [{"name": "minecraft:entity.endermite.ambient", "type": "event", "pitch": 1.8, "volume": 0.5}],
    "einde2_mikas.kaasmijt.kietel": [{"name": "minecraft:entity.silverfish.ambient", "type": "event", "pitch": 2.0, "volume": 0.6},
                                     {"name": "minecraft:entity.endermite.hurt", "type": "event", "pitch": 2.0, "volume": 0.6}],
    "einde2_mikas.kaasmijt.plop": [{"name": "minecraft:entity.chicken.egg", "type": "event", "pitch": 1.5}],
    "einde2_mikas.hik": [{"name": "minecraft:entity.player.burp", "type": "event", "pitch": 1.9, "volume": 0.6}],
    "einde2_mikas.schuilguh.open": [{"name": "minecraft:entity.shulker.open", "type": "event", "pitch": 1.5, "volume": 0.6}],
    "einde2_mikas.schuilguh.dicht": [{"name": "minecraft:entity.shulker.close", "type": "event", "pitch": 1.5, "volume": 0.6}],
    "einde2_mikas.schuilguh.kusje": [{"name": "guhs:entity.guh.happy", "type": "event", "pitch": 1.4}],
    "einde2_mikas.schuilguh.schil": [{"name": "minecraft:block.bamboo_wood.break", "type": "event", "pitch": 1.2}],
    "einde2_mikas.schuildoos.open": [{"name": "minecraft:block.barrel.open", "type": "event", "pitch": 1.3}],
    "einde2_mikas.schuildoos.dicht": [{"name": "minecraft:block.barrel.close", "type": "event", "pitch": 1.3}],
}

VERBORGEN = ["einde2_mikas_gejat", "einde2_mikas_kusje"]      # (granted from code: GuhAdvancements.grant)


def texts(h):
    for key, text in LANG.items():
        kit.lang(h, key, text)
    for page, (naam, zeldzaamheid, info) in DEX.items():
        kit.pagina(h, page, naam, zeldzaamheid, info)
    for name in VERBORGEN:
        kit.verborgen(h, name)


# =====================================================================================================================
# item icons, the particle, the effect icon (drawn)
# =====================================================================================================================
def _img(size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def icon_flitskruimel():
    """A jagged crumb of cheese, shot through with ender purple, and two little sparks next to it."""
    img, d = _img()
    d.polygon([(4, 6), (7, 3), (11, 4), (13, 8), (11, 12), (6, 13), (3, 10)], fill=(250, 214, 96, 255), outline=(190, 128, 36, 255))
    d.line([(6, 5), (8, 8), (7, 9), (10, 11)], fill=(206, 120, 248, 255))          # the flash that is still in it
    d.point((8, 8), fill=(255, 236, 255, 255))
    d.point((10, 6), fill=(226, 172, 52, 255))
    d.point((5, 9), fill=(226, 172, 52, 255))
    d.point((6, 7), fill=(255, 240, 170, 255))
    for (x, y) in ((13, 3), (2, 13)):                                             # sparks
        d.point((x, y), fill=(255, 236, 255, 255))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            d.point((x + dx, y + dy), fill=(206, 120, 248, 255))
    return img


def icon_doosschil():
    """A curved shell of box wood (a quarter of the hoop, seen from the side): the checked band along its top edge, a staple."""
    img, d = _img()
    rij = ["................",
           "......rwrwr.....",
           "....wrHHHHHwr...",
           "...rHHhhhhHHHw..",
           "..wHHhhhhhhhHHr.",
           "..rHhhhdddhhhHD.",
           ".wHhhhd...dhhhD.",
           ".rHhhd.....dhHD.",
           ".HHhsd......dDD.",
           ".HhhsD.......D..",
           ".HhhhD..........",
           ".HHhhD..........",
           "..HhhD..........",
           "..DDDD..........",
           "................",
           "................"]
    kleur = {"H": (226, 190, 130), "h": (240, 208, 150), "d": (186, 146, 90), "D": (150, 112, 66), "r": (206, 52, 60), "w": (250, 244, 232),
             "s": (120, 120, 126)}
    for y, regel in enumerate(rij):
        for x, c in enumerate(regel):
            if c in kleur:
                img.putpixel((x, y), kleur[c] + (255,))
    return img


def flits_sprites():
    """Three little sparks: purple, cheese yellow, and a white-hot one."""
    out = []
    for kern, rand in (((255, 236, 255), (206, 120, 248)), ((255, 250, 200), (250, 214, 96)), ((255, 255, 255), (238, 128, 255))):
        img, d = _img(8)
        for x, y in ((3, 1), (3, 2), (3, 5), (3, 6), (1, 3), (2, 3), (5, 3), (6, 3), (4, 1), (4, 6), (1, 4), (6, 4)):
            d.point((x, y), fill=rand + (220,))
        d.rectangle([3, 3, 4, 4], fill=kern + (255,))
        d.point((2, 2), fill=rand + (140,))
        d.point((5, 5), fill=rand + (140,))
        out.append(img)
    return out


def icon_hik():
    """The hik's effect icon (18 x 18): a little open mouth that goes "hik" with two sparks above it."""
    img, d = _img(18)
    d.ellipse([2, 5, 15, 16], fill=(214, 178, 222, 255), outline=(150, 110, 170, 255))      # a guh-pink face
    d.ellipse([6, 9, 11, 14], fill=(120, 40, 70, 255))                                      # the mouth, wide open
    d.rectangle([7, 12, 10, 13], fill=(236, 110, 150, 255))
    d.point((4, 9), fill=(30, 18, 40, 255))
    d.point((13, 9), fill=(30, 18, 40, 255))
    for (x, y) in ((4, 2), (9, 1), (14, 3)):                                               # hik! hik!
        d.point((x, y), fill=(250, 214, 96, 255))
        d.point((x, y + 1), fill=(255, 240, 170, 255))
    return img


def schuildoos_textures(h):
    """The Schuildoos block: the same round kaasdoos the Schuilguh lives in (the painters of einde2_mikas_modellen), 32 px."""
    rng = kit.rng("schuildoos")
    zij = np.zeros((32, 32, 4), np.uint8)
    zij[0:8] = modellen.doos_zij(lid=True)(32, 8, rng)           # the lid's checked band (uv 0-4)
    zij[8:24] = modellen.doos_zij()(32, 16, rng)                 # the hoop of the box (uv 4-12)
    zij[24:32] = modellen.hout()(32, 8, rng)
    h.save(Image.fromarray(zij), "block", "schuildoos_zij.png")
    h.save(Image.fromarray(modellen.deksel_boven(32, 32, rng)), "block", "schuildoos_boven.png")
    h.save(Image.fromarray(modellen.doos_boven(32, 32, rng)), "block", "schuildoos_binnen.png")
    h.save(Image.fromarray(modellen.hout(donker=0.86)(32, 32, rng)), "block", "schuildoos_onder.png")


def textures(h):
    h.save(icon_flitskruimel(), "item", "flitskruimel.png")
    h.save(icon_doosschil(), "item", "doosschil.png")
    for i, img in enumerate(flits_sprites()):
        h.save(img, "particle", f"einde2_mikas_flits_{i}.png")
    h.save(icon_hik(), "mob_effect", "hik.png")
    schuildoos_textures(h)


# =====================================================================================================================
# blocks, items, loot, recipe, sounds, spawns, tags
# =====================================================================================================================
def _zijden(tex, uv):
    return {f: {"texture": tex, "uv": uv} for f in ("north", "south", "east", "west")}


def schuildoos_model(open_):
    """The box (1 px in from the block's sides, 8 high) and its wider lid; open: the lid lifted at the front, on its hinge
    at the back."""
    doos = {"from": [1, 0, 1], "to": [15, 8, 15], "faces": {
        **_zijden("#zij", [1, 4, 15, 12]), "up": {"texture": "#binnen", "uv": [1, 1, 15, 15]},
        "down": {"texture": "#onder", "uv": [1, 1, 15, 15], "cullface": "down"}}}
    deksel = {"from": [0.5, 8, 0.5], "to": [15.5, 12, 15.5], "faces": {
        **_zijden("#zij", [0.5, 0, 15.5, 4]), "up": {"texture": "#boven", "uv": [0, 0, 16, 16]},
        "down": {"texture": "#onder", "uv": [0.5, 0.5, 15.5, 15.5]}}}
    if open_:
        deksel["rotation"] = {"origin": [8, 8, 15.5], "axis": "x", "angle": 22.5}
    return {"parent": "minecraft:block/block", "textures": {
        "zij": "guhs:block/schuildoos_zij", "boven": "guhs:block/schuildoos_boven", "binnen": "guhs:block/schuildoos_binnen",
        "onder": "guhs:block/schuildoos_onder", "particle": "guhs:block/schuildoos_onder"}, "elements": [doos, deksel]}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    h.item_model("flitskruimel")
    h.item_model("doosschil")
    for e, (naam, basis, vlek) in EIEREN.items():
        kit.spawn_ei(h, e, naam, basis, vlek)
    # the Schuildoos: four ways round, lid down or lifted
    w(f"{A}/models/block/schuildoos.json", schuildoos_model(False))
    w(f"{A}/models/block/schuildoos_open.json", schuildoos_model(True))
    states = {}
    for open_ in ("false", "true"):
        states.update(h.facing_states("schuildoos", model="guhs:block/schuildoos" + ("_open" if open_ == "true" else ""), extra=f",open={open_}"))
    w(f"{A}/blockstates/schuildoos.json", {"variants": states})
    w(f"{A}/models/item/schuildoos.json", {"parent": "guhs:block/schuildoos"})
    # breaking it keeps what is inside: the item gets the block entity's contents (as a shulker box)
    w(f"{D}/loot_table/blocks/schuildoos.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": "guhs:schuildoos", "functions": [{
            "function": "minecraft:copy_components", "source": "block_entity",
            "include": ["minecraft:custom_name", "minecraft:container", "minecraft:lock", "minecraft:container_loot"]}]}]}]})
    h.shaped("schuildoos", ["S", "K", "S"], {"S": "guhs:doosschil", "K": "minecraft:chest"}, "guhs:schuildoos")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:schuildoos"])
    # the particle and the creatures
    w(f"{A}/particles/einde2_mikas_flits.json", {"textures": [f"guhs:einde2_mikas_flits_{i}" for i in range(3)]})
    # the Ender-Mika now and then leaves a Flitskruimel (modest: 0-1, looting may add one); the two others drop nothing
    w(f"{D}/loot_table/entities/ender_mika.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": "guhs:flitskruimel", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}, "add": False},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
             "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})
    for e in ("kaasmijt", "schuilguh"):
        w(f"{D}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "pools": []})
    h.add_tag("guhs/tags/entity_type/mikas", ["guhs:ender_mika"])           # a Mika like the others (the Mika-mepper, the Knabbelkroon)
    # the Ender-Mika comes by itself on the outer islands; old chunks keep the biome guhs:guheinde and get none
    w(f"{D}/neoforge/biome_modifier/einde2_mikas_ender_mika.json", {
        "type": "neoforge:add_spawns", "biomes": kit.ALLE_ZES,
        "spawners": [{"type": "guhs:ender_mika", "weight": 2, "minCount": 1, "maxCount": 1}]})


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# the game test room
# =====================================================================================================================
def test_templates(h):
    """einde2_mikas_test_kamer: a floor of 25 x 25 kaaskorst (the Ender-Mika needs room to flit: the tests keep it within 10
    blocks of the middle), with a little wall and ceiling in one corner for the Schuilguhs to hold on to."""
    t = h.Structure((25, 6, 25))
    for x in range(25):
        for z in range(25):
            t.set(x, 0, z, "guhs:kaaskorst")
    for x in range(1, 5):
        for y in range(1, 5):
            t.set(x, y, 1, "guhs:kaaskorst_stenen")
    for x in range(1, 5):
        for z in range(2, 5):
            t.set(x, 4, z, "guhs:kaaskorst_stenen")
    t.save("einde2_mikas_test_kamer")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    problems = modellen.check(h)
    for e in DIEREN:
        for key in (f"entity.guhs.{e}", f"item.guhs.{e}_spawn_egg", f"gui.guhs.guhdex.rarity.{e}", f"gui.guhs.guhdex.info.{e}"):
            if key not in h.NL:
                problems.append(f"missing lang {key}")
        for p in (f"{h.A}/geckolib/models/entity/{e}.geo.json", f"{h.A}/geckolib/animations/entity/{e}.animation.json",
                  f"{h.TEX}/entity/{e}.png", f"{h.TEX}/item/{e}_spawn_egg.png", f"{h.D}/advancement/quest/seen_{e}.json",
                  f"{h.D}/loot_table/entities/{e}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for p in (f"{h.TEX}/item/flitskruimel.png", f"{h.TEX}/item/doosschil.png", f"{h.TEX}/mob_effect/hik.png",
              f"{h.TEX}/block/schuildoos_zij.png", f"{h.A}/blockstates/schuildoos.json", f"{h.D}/recipe/schuildoos.json",
              f"{h.D}/loot_table/blocks/schuildoos.json", f"{h.A}/particles/einde2_mikas_flits.json",
              f"{h.D}/neoforge/biome_modifier/einde2_mikas_ender_mika.json", f"{h.D}/structure/einde2_mikas_test_kamer.nbt"):
        if not os.path.exists(p):
            problems.append(f"missing {p}")
    for event in SOUNDS:
        if f"subtitles.guhs.{event}" not in h.NL:
            problems.append(f"missing subtitle {event}")
    for name in VERBORGEN:
        if not os.path.exists(f"{h.D}/advancement/quest/{name}.json"):
            problems.append(f"missing advancement quest/{name}")
    # (the build contract's open ends: nothing of einde2 calls itself the last or the end of anything)
    for key, text in list(LANG.items()) + [(k, " ".join(v)) for k, v in DEX.items()]:
        low = text.lower()
        if "laatste" in low or "allersterkste" in low or "te vads" in low:
            problems.append(f"lore: {key}")
    if problems:
        raise SystemExit("einde2_mikas self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    modellen.build(h)
    textures(h)
    blocks_and_items(h)
    sounds(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests: "Ender-Mika's & Schuilguhs" (see / find / make; nothing locked, no dependencies)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("einde2_mikas_ender_mika", "Ender-Mika", "Op de &5buiteneilanden&r van het Guheinde staan lange, dunne, paarszwarte Mika's naar de "
      "leegte te staren. Kijk je ze aan, dan &dflitsen&r ze verlegen weg. Kom er (heel even) dichtbij voor je Guhdex. En let op je "
      "&6kaasknabbels&r: heb je er een in je hand, dan jat hij er eentje. Twee tellen strak aankijken en je krijgt hem terug!",
      "guhs:ender_mika_spawn_egg", [adv("seen_ender_mika")], rewards=(("guhs:kaas_knabbels", 8),), shape="gear", xp=100)
    q("einde2_mikas_flitskruimel", "Flitskruimel", "Een Ender-Mika is een echte Mika: mep hem en hij duwt terug. Soms laat hij een "
      "&dFlitskruimel&r achter, een kruimel die nog een beetje naflitst. Bewaar hem goed, njeg.",
      "guhs:flitskruimel", [item("guhs:flitskruimel")], rewards=(("guhs:kaas_knabbels", 12),), xp=100)
    q("einde2_mikas_kaasmijt", "Kaasmijt", "Eet een &5knabbelkoorvrucht&r en soms... plop! Er springt een &ekaasmijt&r uit, een "
      "piepklein kruimelbeestje dat je tenen kietelt. Daar krijg je de &ehik&r van. Ender-Mika's lopen er dromerig achteraan en "
      "vergeten dan je knabbels.", "guhs:kaasmijt_spawn_egg", [adv("seen_kaasmijt")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("einde2_mikas_schuilguh", "Schuilguh", "Aan de muren van een &5Mika-vesting&r hangen ronde kaasdoosjes. Kom rustig dichterbij en "
      "er gluurt een &dSchuilguh&r onder het dekseltje vandaan. Een lege hand: je krijgt een &dkusje&r en zweeft even. Geen zorgen, "
      "je landt zacht.", "guhs:schuilguh_spawn_egg", [adv("seen_schuilguh")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("einde2_mikas_doosschil", "Doosschil", "Geef een Schuilguh een &6kaasknabbel&r: hij schuift een &edoosschil&r naar buiten. Dat "
      "doet hij één keer per dag, dus zoek er meer dan één (of kom morgen terug).",
      "guhs:doosschil", [item("guhs:doosschil")], rewards=(("guhs:kaas_knabbels", 8),), xp=100)
    q("einde2_mikas_schuildoos", "Schuildoos", "Een doosschil, een &6kist&r en nog een doosschil onder elkaar: een &dSchuildoos&r. "
      "Hij houdt alles vast wat erin zit, ook als je hem oppakt. Een hele kist vol in één vakje. VAHOEG!",
      "guhs:schuildoos", [item("guhs:schuildoos")], rewards=(("guhs:kaas_knabbels", 16),), shape="gear", xp=150)
