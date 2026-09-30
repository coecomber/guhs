"""
De Knuffelcreche (2.8.0 "Knuffeldal", phase 2, slice creche): the plein slot "creche" of the Knuffeldal town - a giant
baby guh with a pacifier (creche_bouw.py) - where Juf Knuffel looks after the babyguhtjes (see
src/main/java/nl/juiced/guhs/feature/creche and guhs_work28/KNUFFEL_CONTRACT.md).

  - blocks: guh_wiegje (a crib: baby leeg/wakker/ingestopt, and the wish bubble of the care round), speelkleed,
    feestslingers (the creche's feesttaakje for the Grote Knusfeest)
  - items: speenmunt (the coin), babyflesje, schone_luier, knuffeldekentje (recipe with #guhs:knus/pluiswol)
  - the prop entity creche_babyguh (a tiny guh with a pacifier), Juf Knuffel's own model and texture
  - clothes: babymutsje, rompertje, speenkettinkje (Juf Knuffel's shop)
  - the particle slaapsterretje, sounds creche.slaapliedje / creche.babygiechel, advancements (tab guhs:knuffeldal),
    lang (Dutch in both files), the Knus tab texts, the game test room, FTB quests (row y = 84.5)
"""
import os

import numpy as np
from PIL import Image

from features import creche_bouw as bouw
from features import creche_tex as tex
from features import knuffeldal_npcs as kn

CLOTHES = ["babymutsje", "rompertje", "speenkettinkje"]
FTB_Y = 84.5
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# =====================================================================================================================
# guh clothes (make_guh_variants.py / make_clothes_icons.py)
# =====================================================================================================================
_H = [0, 6, -2]   # the guh's head pivot
BONES = {
    # the babymutsje: a frilly bonnet over the top, sides and back of the head, with a frill in front and ribbons
    "outfit_babymutsje": ("head", _H, "babymuts", [([-7.2, 13.0, -10.8], [14.4, 2.6, 10.6], 0), ([-6.8, 5.5, -0.9], [13.6, 9.5, 1.2], 0),
                                                  ([-8.7, 6.0, -10.2], [0.9, 8.5, 9.6], 0), ([7.8, 6.0, -10.2], [0.9, 8.5, 9.6], 0)]),
    "outfit_babymutsje_rand": ("head", _H, "babymuts_rand", [([-7.6, 12.6, -11.6], [15.2, 1.8, 1.0], 0.1),
                                                             ([-8.9, 2.5, -7.8], [0.6, 3.6, 0.6], 0), ([8.3, 2.5, -7.8], [0.6, 3.6, 0.6], 0)]),
    # the speenkettinkje: a little chain round the neck with a pacifier on it
    "outfit_speenkettinkje": ("head", _H, "speenketting", [([-4.5, 1.6, -11.6], [9, 0.5, 0.5], 0), ([-0.3, 0.3, -11.7], [0.6, 1.4, 0.6], 0)]),
    "outfit_speenkettinkje_speen": ("head", _H, "speen", [([-1.5, -1.6, -12.0], [3.0, 1.9, 0.6], 0), ([-0.8, -2.7, -12.4], [1.6, 1.1, 0.4], 0)]),
}


def clothes(rng, v):
    def muts():
        a = v.fabric((255, 244, 250), rng, 6)
        for y in range(2, 32, 8):
            for x in range((y // 8) % 2 * 4, 32, 8):
                a[y:y + 2, x:x + 2] = (246, 150, 196)          # little pink dots
        return a

    def rand():
        a = v.fabric((190, 225, 250), rng, 6)
        a[::4, :] = (255, 255, 255)
        return a

    def romper():
        a = v.fabric((190, 225, 250), rng, 6)
        for y in range(3, 32, 7):
            for x in range((y // 7) % 2 * 3, 32, 6):
                a[y:y + 2, x:x + 2] = (255, 255, 255)
        a[12:16, 13:15] = (246, 110, 170)                       # a little heart
        a[12:14, 12:16] = (246, 110, 170)
        return a

    def ketting():
        return v.fabric((236, 236, 244), rng, 8)

    def speen():
        a = v.fabric((140, 200, 240), rng, 6)
        a[24:, :] = (246, 200, 70)
        return a

    return {"babymutsje": {"babymuts": muts, "babymuts_rand": rand},
            "rompertje": {"suit": romper},
            "speenkettinkje": {"speenketting": ketting, "speen": speen}}


def icons(ic):
    muts = ic.icon(ic.pad(["....aaaaaa......", "..aabbbbbbaa....", ".abbcbbbbcbba...", ".abbbbbbbbbba...", "abbbbcbbbbbbba..",
                           "abbbbbbbbbbbba..", "addddddddddda...", ".d..........d...", ".d..........d..."]),
                   {"a": (200, 180, 200), "b": (255, 244, 250), "c": (246, 150, 196), "d": (140, 200, 240)})
    ketting = ic.icon(ic.pad(["..a.........a...", "...a.......a....", "....a.....a.....", ".....aaaaa......", ".......a........",
                              ".....bbbbb......", ".....bbcbb......", ".....bbbbb......", "......ddd.......", "......d.d.......",
                              "......ddd......."]),
                      {"a": (200, 200, 215), "b": (140, 200, 240), "c": (246, 150, 196), "d": (246, 200, 70)})
    return {"babymutsje": muts, "rompertje": ic.shirt((190, 225, 250), (120, 170, 220), (255, 255, 255), "buttons"),
            "speenkettinkje": ketting}


# =====================================================================================================================
# blocks and items
# =====================================================================================================================
def el(frm, to, texture, faces=None, rot=None, uv=None):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": texture, "uv": uv} if uv else {"texture": texture})
                                          for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    return e


WIEG_TEX = {"particle": "guhs:block/guh_wiegje_hout", "hout": "guhs:block/guh_wiegje_hout", "laken": "guhs:block/guh_wiegje_laken",
            "hoofdbord": "guhs:block/guh_wiegje_hoofdbord", "baby": "guhs:block/guh_wiegje_baby", "slaapt": "guhs:block/guh_wiegje_baby_slaapt",
            "vacht": "minecraft:block/pink_wool", "dekentje": "guhs:block/guh_wiegje_dekentje"}


def wieg_base():
    # the front (the foot end, where the baby looks) is north; the headboard with its guh face is south
    return [el([1.5, 0, 1.5], [3, 2, 3], "#hout"), el([13, 0, 1.5], [14.5, 2, 3], "#hout"), el([1.5, 0, 13], [3, 2, 14.5], "#hout"),
            el([13, 0, 13], [14.5, 2, 14.5], "#hout"),
            el([1, 2, 1], [15, 3, 15], "#hout"),
            el([2, 3, 2], [14, 4.5, 14], "#laken"),
            el([1, 3, 1], [2, 10, 15], "#hout"), el([14, 3, 1], [15, 10, 15], "#hout"),
            el([2, 3, 1], [14, 9, 2], "#hout"),
            {"from": [1, 3, 14], "to": [15, 14, 15.5], "faces": {"north": {"texture": "#hoofdbord"}, "south": {"texture": "#hoofdbord"},
                                                                  "up": {"texture": "#hout"}, "west": {"texture": "#hout"}, "east": {"texture": "#hout"},
                                                                  "down": {"texture": "#hout"}}}]


def wieg_wakker():
    return [{"from": [4.5, 4.5, 7.5], "to": [11.5, 10.5, 13], "faces": {"north": {"texture": "#baby"}, "south": {"texture": "#vacht"},
                                                                     "up": {"texture": "#vacht"}, "west": {"texture": "#vacht"},
                                                                     "east": {"texture": "#vacht"}, "down": {"texture": "#vacht"}}},
            el([4, 10, 10], [6, 13, 11.5], "#vacht"), el([10, 10, 10], [12, 13, 11.5], "#vacht"),
            el([5.5, 4.5, 4.5], [10.5, 7.5, 7.5], "#vacht")]


def wieg_ingestopt():
    return [{"from": [4.5, 4.5, 8.5], "to": [11.5, 9.5, 13.5], "faces": {"north": {"texture": "#slaapt"}, "south": {"texture": "#vacht"},
                                                                      "up": {"texture": "#vacht"}, "west": {"texture": "#vacht"},
                                                                      "east": {"texture": "#vacht"}, "down": {"texture": "#vacht"}}},
            el([4, 9, 10.5], [6, 11.5, 12], "#vacht"), el([10, 9, 10.5], [12, 11.5, 12], "#vacht"),
            el([2.5, 4.5, 2.5], [13.5, 6.5, 9], "#dekentje")]


def wens_model(w):
    faces = {"north": {"texture": "#wens", "uv": [0, 0, 16, 16]}, "south": {"texture": "#wens", "uv": [0, 0, 16, 16]}}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": f"guhs:block/guh_wiegje_wens_{w}", "wens": f"guhs:block/guh_wiegje_wens_{w}"},
            "elements": [{"from": [3, 15, 8], "to": [13, 25, 8], "shade": False, "rotation": {"origin": [8, 20, 8], "axis": "y", "angle": 45},
                          "faces": faces},
                         {"from": [3, 15, 8], "to": [13, 25, 8], "shade": False, "rotation": {"origin": [8, 20, 8], "axis": "y", "angle": -45},
                          "faces": faces}]}


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    block = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": WIEG_TEX}
    w(f"{A}/models/block/guh_wiegje.json", {**block, "elements": wieg_base()})
    w(f"{A}/models/block/guh_wiegje_baby_wakker.json", {**block, "elements": wieg_wakker()})
    w(f"{A}/models/block/guh_wiegje_baby_ingestopt.json", {**block, "elements": wieg_ingestopt()})
    w(f"{A}/models/block/guh_wiegje_item.json", {**block, "elements": wieg_base() + wieg_ingestopt()})
    for wens in tex.WENSEN:
        w(f"{A}/models/block/guh_wiegje_wens_{wens}.json", wens_model(wens))
    parts = []
    for f, r in ROT.items():
        rot = {"y": r} if r else {}
        parts.append({"when": {"facing": f}, "apply": {"model": "guhs:block/guh_wiegje", **rot}})
        parts.append({"when": {"facing": f, "baby": "wakker"}, "apply": {"model": "guhs:block/guh_wiegje_baby_wakker", **rot}})
        parts.append({"when": {"facing": f, "baby": "ingestopt"}, "apply": {"model": "guhs:block/guh_wiegje_baby_ingestopt", **rot}})
    for wens in tex.WENSEN:
        parts.append({"when": {"wens": wens}, "apply": {"model": f"guhs:block/guh_wiegje_wens_{wens}"}})
    w(f"{A}/blockstates/guh_wiegje.json", {"multipart": parts})
    w(f"{A}/models/item/guh_wiegje.json", {"parent": "guhs:block/guh_wiegje_item"})
    # the play mat
    w(f"{A}/models/block/speelkleed.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/speelkleed"}})
    w(f"{A}/blockstates/speelkleed.json", {"variants": {"": {"model": "guhs:block/speelkleed"}}})
    w(f"{A}/models/item/speelkleed.json", {"parent": "guhs:block/speelkleed"})
    # the garland
    w(f"{A}/models/block/feestslingers.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                               "textures": {"particle": "guhs:block/feestslingers", "vlag": "guhs:block/feestslingers"},
                                               "elements": [{"from": [0, 4, 8], "to": [16, 16, 8], "shade": False, "faces": {
                                                   "north": {"texture": "#vlag", "uv": [0, 0, 16, 12]},
                                                   "south": {"texture": "#vlag", "uv": [0, 0, 16, 12]}}}]})
    w(f"{A}/blockstates/feestslingers.json", {"variants": {"axis=x": {"model": "guhs:block/feestslingers"},
                                                           "axis=z": {"model": "guhs:block/feestslingers", "y": 90}}})
    h.item_model("feestslingers", "guhs:block/feestslingers")
    for i in ("speenmunt", "babyflesje", "schone_luier", "knuffeldekentje"):
        h.item_model(i)
    # loot
    for blk in ("guh_wiegje", "speelkleed", "feestslingers"):
        h.self_drop(blk)
    # recipes
    h.shaped("guh_wiegje", ["P P", "PWP"], {"P": "#minecraft:planks", "W": "minecraft:pink_wool"}, "guhs:guh_wiegje", 1)
    h.shaped("speelkleed", ["PB"], {"P": "minecraft:pink_carpet", "B": "minecraft:light_blue_carpet"}, "guhs:speelkleed", 2)
    h.shapeless("babyflesje", ["minecraft:glass_bottle", "#guhs:knus/kaasmelk"], "guhs:babyflesje", 1)
    h.shapeless("babyflesje_melk", ["minecraft:glass_bottle", "minecraft:milk_bucket"], "guhs:babyflesje", 1)
    h.shapeless("schone_luier", ["minecraft:white_wool", "minecraft:string"], "guhs:schone_luier", 2)
    h.shaped("knuffeldekentje", ["PPP", "PWP"], {"P": "#guhs:knus/pluiswol", "W": "minecraft:pink_wool"}, "guhs:knuffeldekentje", 1)
    # tags
    add = h.add_tag
    add("minecraft/tags/block/mineable/axe", ["guhs:guh_wiegje"])
    add("guhs/tags/item/knus/feestslingers", ["guhs:feestslingers"])
    add("guhs/tags/item/knus/grijptickets", ["guhs:speenmunt"])


# =====================================================================================================================
# the characters' looks: Juf Knuffel (a nurse cap with a heart, an apron) and the babyguhtje (a tiny guh with a pacifier)
# =====================================================================================================================
def juf_knuffel(h):
    geo_file = kn._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_juf_knuffel"
    sw = kn._swatches(geo, ["muts", "hart", "schort", "schortrand", "fles"])
    geo["bones"].append({"name": "juf_mutsje", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        kn._cube([-4.5, 25.6, -5.2], [9, 1.4, 7], sw["muts"]),
        kn._cube([-4.8, 25.4, -6.1], [9.6, 3.2, 0.9], sw["muts"]),
        kn._cube([-3.5, 27.0, -4.2], [7, 1.2, 5], sw["muts"])]})
    geo["bones"].append({"name": "juf_hartje", "parent": "juf_mutsje", "pivot": [0, 27, -6.3], "cubes": [
        kn._cube([-1.2, 26.4, -6.5], [1.1, 1.5, 0.4], sw["hart"]), kn._cube([0.1, 26.4, -6.5], [1.1, 1.5, 0.4], sw["hart"]),
        kn._cube([-0.6, 25.9, -6.5], [1.2, 0.8, 0.4], sw["hart"])]})
    geo["bones"].append({"name": "juf_schortje", "parent": "body", "pivot": [0, 8, -4], "cubes": [
        kn._cube([-3.4, 3.0, -4.7], [6.8, 7.4, 0.5], sw["schort"]),
        kn._cube([-3.6, 10.2, -4.8], [7.2, 0.6, 0.5], sw["schortrand"]),
        kn._cube([-3.6, 2.8, -4.8], [7.2, 0.6, 0.5], sw["schortrand"]),
        kn._cube([1.0, 4.2, -5.3], [1.4, 2.6, 0.8], sw["fles"])]})
    kn._save_geo(h, "guh_npc_juf_knuffel.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.93, sat=0.55, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2831)
    kn._paint(a, sw["muts"], (252, 252, 255), rng, 4)
    kn._paint(a, sw["hart"], (238, 90, 150), rng, 4)

    def schort(block):
        for yy in range(0, 32, 6):
            for xx in range(2, 32, 8):
                block[yy:yy + 2, xx:xx + 2, :3] = (190, 225, 250)
    kn._paint(a, sw["schort"], (255, 250, 252), rng, 4, schort)
    kn._paint(a, sw["schortrand"], (246, 150, 196), rng, 4)
    kn._paint(a, sw["fles"], (236, 244, 250), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_juf_knuffel.png")


def babyguh(h):
    geo_file = kn._load(h, "guh.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.creche_babyguh"
    keep = {"root", "body", "tail", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"}
    geo["bones"] = [b for b in geo["bones"] if b["name"] in keep]
    sw = kn._swatches(geo, ["speen", "ring", "lok"])
    geo["bones"].append({"name": "baby_speen", "parent": "head", "pivot": [0, 4, -12], "cubes": [
        kn._cube([-2.2, 3.2, -13.1], [4.4, 2.1, 0.7], sw["speen"]), kn._cube([-1.3, 1.8, -13.6], [2.6, 1.6, 0.5], sw["ring"])]})
    geo["bones"].append({"name": "baby_lok", "parent": "head", "pivot": [0, 15, -6], "cubes": [
        kn._cube([-0.6, 15, -6.8], [1.2, 1.4, 1.2], sw["lok"]), kn._cube([0.2, 16.2, -6.8], [1.4, 1.0, 1.2], sw["lok"])]})
    kn._save_geo(h, "creche_babyguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    img = h.recolour(src, hue=0.94, sat=0.45, val=1.06, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2832)
    kn._paint(a, sw["speen"], (140, 200, 240), rng, 4)
    kn._paint(a, sw["ring"], (246, 200, 70), rng, 6)
    kn._paint(a, sw["lok"], (250, 176, 206), rng, 8)
    h.save(Image.fromarray(a), "entity", "creche_babyguh.png")


# =====================================================================================================================
# sounds, advancements, texts
# =====================================================================================================================
SOUNDS = {
    "creche.slaapliedje": [{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 0.8, "volume": 0.7},
                           {"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 0.9, "volume": 0.5}],
    "creche.babygiechel": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.9, "volume": 0.8}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


QUEST_ADVANCEMENTS = ["creche_juf", "creche_ingestopt", "creche_terugbrengen", "creche_alle_babys_terug", "creche_slaapliedjes", "seen_juf_knuffel"]
TAB = [  # name, parent, icon, frame, title, description
    ("creche_ingestopt", "stadje", "guhs:guh_wiegje", "task", "Stil maar, babyguhtje",
     "Geef drie babyguhtjes in de Knuffelcreche een flesje, een schone luier, stop ze in en zing een slaapliedje"),
    ("creche_alle_babys_terug", "creche_ingestopt", "guhs:speenmunt", "goal", "Niemand ontsnapt!",
     "Breng in één spelletje minstens 8 babyguhtjes terug naar hun wiegje, zonder dat Juf Knuffel er eentje zelf moet halen"),
    ("creche_slaapliedjes", "creche_ingestopt", "guhs:knuffeldekentje", "goal", "Het hele liedjesboek",
     "Zing alle vier de slaapliedjes van Juf Knuffel. Zzz... VAHOEG! (zachtjes)"),
]


def advancements(h):
    D = h.D
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, title, desc in TAB:
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
        h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


LIEDJES = {
    "sterretjes": ("Slaap, guhtje, slaap", "Daar buiten loopt een kaasknabbel... nee wacht, daar buiten twinkelt een sterretje. Het "
                   "allereerste liedje dat Juf Knuffel je leert: langzaam wiegen, heen en weer."),
    "maantje": ("Daar is het maantje", "Een zacht walsje over het maantje dat over de Guhmensie kijkt. De babyguhtjes worden er "
                "helemaal vadsig slaperig van."),
    "knabbeltje": ("Knabbeltje onder je kussen", "Een huppelliedje over een kaasknabbel onder je kussen, voor als je morgen wakker wordt. "
                   "Maar niet opeten in bed, njeg!"),
    "wolkje": ("Wolkje, wolkje", "Langzaam naar beneden, als een roze wolkje dat in slaap zakt. Het laatste liedje: daarna slaapt "
               "echt iedereen. Zzz."),
}

LANG = {
    # blocks and items
    "block.guhs.guh_wiegje": "Guh-wiegje",
    "block.guhs.guh_wiegje.lore": "Een wiegje met een guhgezichtje. Rechtsklik om te wiegen: babyguhs in de buurt groeien een beetje.",
    "block.guhs.speelkleed": "Speelkleed",
    "block.guhs.speelkleed.lore": "Een zacht speelkleed met een lachende guh. Om op te kruipen!",
    "block.guhs.feestslingers": "Feestslingers",
    "block.guhs.feestslingers.lore": "Geknutseld door de babyguhtjes van de Knuffelcreche. Voor het Grote Knusfeest!",
    "item.guhs.speenmunt": "Speenmunt",
    "item.guhs.speenmunt.lore": "De munt van de Knuffelcreche. Te besteden bij Juf Knuffel (en in de grijpmachine).",
    "item.guhs.babyflesje": "Babyflesje",
    "item.guhs.babyflesje.lore": "Warme kaasmelk voor babyguhtjes. Ook je eigen babyguh groeit er een beetje van. Slurp!",
    "item.guhs.schone_luier": "Schone luier",
    "item.guhs.schone_luier.lore": "Fris en zacht. Babyguhtjes worden daar zo vahoeg van!",
    "item.guhs.knuffeldekentje": "Knuffeldekentje",
    "item.guhs.knuffeldekentje.lore": "Een zacht dekentje van pluiswol. Stop een babyguhtje ermee in: zzz...",
    "item.guhs.babymutsje": "Babymutsje", "item.guhs.rompertje": "Rompertje", "item.guhs.speenkettinkje": "Speenkettinkje",
    "entity.guhs.creche_babyguh": "Babyguhtje",
    "subtitles.guhs.creche.slaapliedje": "Een slaapliedje",
    "subtitles.guhs.creche.babygiechel": "Babyguhtje giechelt",
    # Juf Knuffel (NPC name + Guhdex page)
    "entity.guhs.guh_npc.juf_knuffel": "Juf Knuffel",
    "gui.guhs.guhdex.rarity.juf_knuffel": "Zeldzaamheid: Uniek (Knuffelcreche, Knuffeldal)",
    "gui.guhs.guhdex.info.juf_knuffel": "Past op alle babyguhtjes van het Knuffeldal. Ze kent vier slaapliedjes en heeft altijd een "
                                        "flesje kaasmelk in haar schortje. Babyguhtjes kruipen overal heen: help jij ze terug in hun wiegje?",
    # her lines
    "quest.guhs.juf_knuffel.hoi.0": "Sst, sst... hallo! De babyguhtjes slapen. Nou ja, bijna allemaal. Njeg.",
    "quest.guhs.juf_knuffel.hoi.1": "Welkom in de Knuffelcreche! Wil je me helpen? Babyguhtjes zijn heel lief, maar ook heel kruiperig.",
    "quest.guhs.juf_knuffel.hoi.2": "Hoi! Weet je dat babyguhtjes nooit te vads kunnen zijn? Ze moeten juist VAHOEG worden. Daar help ik ze bij.",
    "quest.guhs.juf_knuffel.bezig": "Even geduld, ik help nu iemand anders met de babyguhtjes. Zzz... sst!",
    "quest.guhs.juf_knuffel.bezig_jij": "Je bent al bezig! Kijk maar naar de wiegjes: ze laten zien wat de babyguhtjes willen.",
    "quest.guhs.juf_knuffel.geen_wiegjes": "Njeg, waar zijn mijn wiegjes gebleven? Zonder wiegjes kunnen we niks.",
    "quest.guhs.juf_knuffel.verzorgen": "Kijk, drie babyguhtjes zijn wakker! Boven hun wiegje zie je wat ze willen: eerst een flesje, "
                                        "dan een schone luier, dan instoppen met een dekentje en dan een slaapliedje. Hier, pak aan!",
    "quest.guhs.juf_knuffel.verzorgen_feest": "Het Grote Knusfeest! De babyguhtjes willen feestslingers knutselen. Geef ze eerst papier, "
                                              "daarna een flesje, een schone luier, het dekentje en een slaapliedje. Hier, pak aan!",
    "quest.guhs.juf_knuffel.liedje_vandaag": "Vandaag zingen we: %s. Rechtsklik het wiegje als het babyguhtje een muzieknootje wil.",
    "quest.guhs.juf_knuffel.liedje_opnieuw": "Bijna! %s van de %s noten goed. Nog een keertje, rustig... rechtsklik het wiegje maar.",
    "quest.guhs.juf_knuffel.liedje_geleerd": "Oooh, dat klonk mooi! Je kent nu het liedje %s. Het staat in je Guhdex (Knus).",
    "quest.guhs.juf_knuffel.verzorgd": "Ze slapen alle drie! Zzz... wat knus. Hier, %s speenmunten. Dankjewel!",
    "quest.guhs.juf_knuffel.slingers": "En kijk wat de babyguhtjes voor het feest geknutseld hebben: feestslingers! Breng ze naar Burgemeester Vadsema.",
    "quest.guhs.juf_knuffel.terugbrengen": "Oei, de babyguhtjes worden wakker en kruipen overal heen! Rechtsklik een babyguhtje om het op te "
                                           "pakken en breng het terug naar een leeg wiegje. Snel, maar zachtjes!",
    "quest.guhs.juf_knuffel.eerste_spel": "Wat goed voor de eerste keer! Hier nog %s speenmunten extra, als welkom.",
    "quest.guhs.juf_knuffel.gestopt": "Oké, dan leg ik ze zelf wel terug. Zzz...",
    "quest.guhs.juf_knuffel.te_lang": "Njeg, het duurde zo lang dat ik ze zelf maar heb ingestopt. Kom je later terug?",
    # her screen
    "gui.guhs.creche.knop.verzorgen": "Verzorgronde",
    "gui.guhs.creche.knop.verzorgen.tooltip": "Rustig: geef drie babyguhtjes een flesje, een luier, stop ze in en zing een slaapliedje",
    "gui.guhs.creche.knop.knutselen": "Slingers knutselen",
    "gui.guhs.creche.knop.knutselen.tooltip": "Voor het Grote Knusfeest: de babyguhtjes knutselen feestslingers, daarna de verzorgronde",
    "gui.guhs.creche.knop.terugbrengen": "Babyguhtjes terugbrengen",
    "gui.guhs.creche.knop.terugbrengen.tooltip": "Het spelletje: de babyguhtjes kruipen weg, breng ze snel terug naar hun wiegje!",
    "gui.guhs.creche.knop.stop": "Stoppen",
    "gui.guhs.creche.knop.stop.tooltip": "Juf Knuffel neemt het over (bij het spelletje krijg je je punten nog)",
    "gui.guhs.creche.knop.winkel": "Winkel",
    "gui.guhs.creche.knop.winkel.tooltip": "Babykleertjes, wiegjes en dekentjes voor speenmunten",
    "gui.guhs.creche.knop.doei": "Doei!",
    "gui.guhs.creche.scherm.uitleg": "Dit is de Knuffelcreche! Help je me? Rustig verzorgen: flesje, luier, dekentje en een slaapliedje. "
                                     "Of het spelletje: de babyguhtjes kruipen weg, breng ze terug naar hun wiegje!",
    "gui.guhs.creche.scherm.feest": "Het Grote Knusfeest! De babyguhtjes willen feestslingers knutselen voor de burgemeester. Doe je mee "
                                    "met de verzorgronde? Dan knutselen ze eerst.",
    "gui.guhs.creche.scherm.bezig": "%s helpt me nu met de babyguhtjes. Even wachten, sst!",
    "gui.guhs.creche.scherm.jij_verzorgen": "Je bent bezig met de verzorgronde. Kijk boven de wiegjes wat de babyguhtjes willen.",
    "gui.guhs.creche.scherm.jij_terugbrengen": "Je bent bezig met terugbrengen! Snel, daar kruipt er eentje!",
    "gui.guhs.creche.scherm.best": "Jouw record: %s punten",
    "gui.guhs.creche.scherm.geen_best": "Nog geen record. Probeer het spelletje!",
    "gui.guhs.creche.scherm.munten": "Speenmunten: %s   Slaapliedjes: %s/4",
    # during the games
    "gui.guhs.creche.wens.geen": "niks", "gui.guhs.creche.wens.knutselen": "papier om te knutselen", "gui.guhs.creche.wens.honger": "een babyflesje",
    "gui.guhs.creche.wens.luier": "een schone luier", "gui.guhs.creche.wens.slaap": "ingestopt worden met een knuffeldekentje",
    "gui.guhs.creche.wens.liedje": "een slaapliedje (rechtsklik het wiegje)",
    "gui.guhs.creche.wil": "Njeg! Dit babyguhtje wil %s.",
    "gui.guhs.creche.nu": "Goed zo! Nu wil het %s.",
    "gui.guhs.creche.slaapt": "Zzz... slaapt! (%s van de %s)",
    "gui.guhs.creche.slaapt_al": "Sst! Dit babyguhtje slaapt al.",
    "gui.guhs.creche.deze_slaapt": "Dit babyguhtje slaapt gewoon door. Kijk bij de wakkere!",
    "gui.guhs.creche.niet_jouw_spel": "Njeg, iemand anders helpt Juf Knuffel nu.",
    "gui.guhs.creche.handen_vol": "Je handen zijn vol! (%s babyguhtjes tegelijk)",
    "gui.guhs.creche.terug": "+%s punten! Terug in het wiegje (reeks: %s)",
    "gui.guhs.creche.ontsnapt": "Njeg! Juf Knuffel moest er eentje zelf terugbrengen.",
    "gui.guhs.creche.bar": "Terug: %s  ·  Punten: %s  ·  Nog %s s  ·  Reeks: %s",
    "gui.guhs.creche.klaar": "Klaar! %s babyguhtjes teruggebracht, %s punten: %s speenmunten.",
    "gui.guhs.creche.record": "Nieuw record: %s punten (was %s)! +%s speenmunten. VAHOEG!",
    "gui.guhs.creche.record_eerste": "Je eerste record: %s punten!",
    "gui.guhs.creche.best": "Jouw record blijft %s punten.",
    "gui.guhs.creche.weggelopen": "Je liep weg uit de Knuffelcreche. Juf Knuffel neemt het over.",
    "gui.guhs.creche.gewiegd": "Wiegewiege... %s babyguh(s) groeien een beetje. Zzz...",
    "gui.guhs.creche.alleen_babys": "Njeg, dat is voor babyguhtjes!",
    "gui.guhs.creche.titel.klaar": "Klaar voor de start?",
    "gui.guhs.creche.titel.klaar.sub": "Zo meteen kruipen ze weg!",
    "gui.guhs.creche.titel.go": "Daar gaan ze!",
    "gui.guhs.creche.titel.go.sub": "Rechtsklik een babyguhtje en breng het terug",
    "gui.guhs.creche.titel.klaar_spel": "Allemaal in bed!",
    "gui.guhs.creche.titel.klaar_spel.sub": "%s punten",
    # the lullaby screen
    "gui.guhs.creche.liedje.titel": "Slaapliedje",
    "gui.guhs.creche.liedje.kop": "Slaapliedje: %s",
    "gui.guhs.creche.liedje.uitleg": "Druk op spatie (of klik) als een sterretje in het maantje is. Rustig meewiegen...",
    "gui.guhs.creche.liedje.raak": "Goed: %s / %s",
    "gui.guhs.creche.liedje.gelukt": "Zzz... het babyguhtje slaapt! Welterusten.",
    "gui.guhs.creche.liedje.mislukt": "Het babyguhtje is nog wakker. Nog een keertje?",
    # the scoreboard
    "gui.guhs.scorebord.creche": "Knuffelcreche",
    "gui.guhs.scorebord.creche.punten": "Babyguhtjes terugbrengen",
    # the Knus tab
    "gui.guhs.knus.mijlpaal.creche_eerste_ronde": "Je eerste verzorgronde",
    "gui.guhs.knus.mijlpaal.creche_ingestopt": "15 babyguhtjes ingestopt",
    "gui.guhs.knus.mijlpaal.creche_spelletje": "Babyguhtjes teruggebracht (spelletje)",
    "gui.guhs.knus.mijlpaal.creche_terug": "40 babyguhtjes teruggebracht",
    "gui.guhs.knus.mijlpaal.creche_record": "200 punten in één spelletje",
    "gui.guhs.knus.mijlpaal.creche_liedjes": "Alle 4 slaapliedjes",
    "gui.guhs.knus.verzameling.slaapliedjes": "Slaapliedjes",
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for lid, (naam, info) in LIEDJES.items():
        h.lang(f"gui.guhs.knus.slaapliedjes.{lid}", naam, naam)
        h.lang(f"gui.guhs.knus.slaapliedjes.{lid}.info", info, info)


# =====================================================================================================================
# game test room, self-check
# =====================================================================================================================
TEST_WIEGJES = [((3, 3), "south"), ((7, 3), "south"), ((11, 3), "south"), ((3, 11), "north"), ((7, 11), "north"), ((11, 11), "north")]


def test_templates(h):
    """creche_test_kamer (15 x 6 x 15): a floor with six cribs (babies tucked in) and room in the middle."""
    s = h.Structure((15, 6, 15))
    for x in range(15):
        for z in range(15):
            s.set(x, 0, z, "guhs:knuffelklinkers")
    for (x, z), f in TEST_WIEGJES:
        s.set(x, 1, z, "guhs:guh_wiegje", {"facing": f, "baby": "ingestopt", "wens": "geen"})
    s.save("creche_test_kamer")


def selfcheck_assets(h):
    A = h.A
    missing = []
    for b in ("guh_wiegje", "speelkleed", "feestslingers"):
        if not os.path.exists(f"{A}/blockstates/{b}.json") or not os.path.exists(f"{A}/models/item/{b}.json") or f"block.guhs.{b}" not in h.NL:
            missing.append(f"block {b}")
    for i in ("speenmunt", "babyflesje", "schone_luier", "knuffeldekentje"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for f in ("guh_npc_juf_knuffel.geo.json", "creche_babyguh.geo.json"):
        if not os.path.exists(f"{A}/geckolib/models/entity/{f}"):
            missing.append(f"geo {f}")
    for t in ("npc_juf_knuffel", "creche_babyguh"):
        if not os.path.exists(f"{A}/textures/entity/{t}.png"):
            missing.append(f"texture {t}")
    if missing:
        raise SystemExit(f"creche assets missing: {missing}")


def build(h):
    tex.textures(h)
    blocks_and_items(h)
    juf_knuffel(h)
    babyguh(h)
    sounds(h)
    advancements(h)
    texts(h)
    b = bouw.build_and_check(h)
    test_templates(h)
    selfcheck_assets(h)
    print(f"creche: building ok ({len(b.s.blocks)} blocks, {len(b.wiegjes)} cribs, {b.gezichten} small + {b.grote_gezichten} big guh faces)")


# =====================================================================================================================
# FTB quests (row y = 84.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("creche_juf", "Juf Knuffel", "Aan de westkant van het plein van het Knuffeldal zit een reuzenbabyguh met een speen: de &dKnuffelcreche&r. "
      "Loop tussen zijn voetjes door naar binnen en praat met &dJuf Knuffel&r.", "guhs:speenmunt", [fq.adv("creche_juf")],
      rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=100)
    q("creche_verzorgen", "Stil maar, babyguhtje", "Doe de verzorgronde: geef drie babyguhtjes een babyflesje, een schone luier, stop ze in "
      "met een knuffeldekentje en zing een slaapliedje (tik mee op de maat!).", "guhs:babyflesje", [fq.adv("creche_ingestopt")],
      rewards=(("guhs:speenmunt", 3),), x=-6.5, y=y, xp=150)
    q("creche_terugbrengen", "Kruip kruip kruip!", "Speel het spelletje: de babyguhtjes kruipen uit hun wiegje. Rechtsklik er een om hem "
      "op te pakken en breng hem terug naar een leeg wiegje. Hoe sneller, hoe meer punten!", "guhs:guh_wiegje",
      [fq.adv("creche_terugbrengen")], rewards=(("guhs:speenmunt", 3),), x=-5, y=y, xp=150)
    q("creche_alle_terug", "Niemand ontsnapt!", "Breng in één spelletje minstens 8 babyguhtjes terug zonder dat Juf Knuffel er eentje "
      "zelf moet halen.", "guhs:speenmunt", [fq.adv("creche_alle_babys_terug")], rewards=(("guhs:speenmunt", 6),), x=-3.5, y=y, shape="gear", xp=250)
    q("creche_slaapliedjes", "Het hele liedjesboek", "Leer alle vier de slaapliedjes van Juf Knuffel (elke verzorgronde leert ze je een "
      "nieuw liedje). Ze staan in je Guhdex, tab Knus.", "guhs:knuffeldekentje", [fq.adv("creche_slaapliedjes")],
      rewards=(("guhs:speenmunt", 5),), x=-2, y=y, shape="gear", xp=250)
    q("creche_babykleertjes", "Babykleertjes", "Koop bij Juf Knuffel een rompertje voor je tamme guh. Zo schattig, VAHOEG!",
      "guhs:rompertje", [fq.item("guhs:rompertje")], rewards=(("guhs:kaas_knabbels", 8),), x=-0.5, y=y, xp=100)
    q("creche_wiegje", "Een wiegje voor thuis", "Maak of koop een guh-wiegje. Wieg het (rechtsklik) en je babyguhs worden een beetje groter.",
      "guhs:guh_wiegje", [fq.item("guhs:guh_wiegje")], rewards=(("guhs:babyflesje", 3),), x=1, y=y, xp=100)
    q("creche_guhdex", "Juf Knuffel in je Guhdex", "Ga vlak bij Juf Knuffel staan, dan staat ze in je Guhdex.", "guhs:guhdex",
      [fq.adv("seen_juf_knuffel")], rewards=(("guhs:kaas_knabbels", 4),), x=2.5, y=y, shape="rsquare", xp=100)
