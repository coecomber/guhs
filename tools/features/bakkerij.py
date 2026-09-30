"""
De Knabbelbakkerij (2.8, plein slot "bakkerij" of the Knuffeldal town; Java: nl.juiced.guhs.feature.bakkerij).

  - the building knuffeldal_stadje/bakkerij (a giant kaasknabbel bread with a guh face in the crust, chimneys puffing
    knabbelwolkjes, a cosy bakery inside) and the GameTest bakery bakkerij_test: tools/features/bakkerij_gebouw.py
  - the pixel art (pastries, feesttaart, bakmunt, knabbeloven, chimney pot, particles, button sheet, order bubble):
    tools/features/bakkerij_tex.py
  - here: Bakker Korstje's look (a toque with a kaasknabbel, an apron), block models, particles, sounds, tags, recipes,
    advancements, lang (Dutch in both files), the baker's outfit (BONES / clothes / icons / CLOTHES), FTB quests (row 83)
    and a self-check of the assets.
"""
import os

import numpy as np
from PIL import Image

from features import bakkerij_gebouw as gebouw
from features import bakkerij_tex as tex
from features import knuffeldal_npcs as npcs

RECEPTEN = ["knabbelbroodje", "kaaskrakeling", "vadsvlaai", "guhcroissant", "knabbelkoekje", "kaasbolletje", "pluismuffin", "theetaartje",
            "knabbeltompouce", "vadsdonut", "guhwafel", "sterrenkoekje"]
ITEMS = RECEPTEN + ["feesttaart", "bakmunt"]
FTB_Y = 83

# ======================================================================================================================
# guh clothes: the baker's outfit (Bakker Korstje's shop)
# ======================================================================================================================
_H = [0, 6, -2]
BONES = {
    # a round, puffy baker's cap (lower than a chef's toque) with a little kaasknabbel pinned on the front
    "outfit_bakkersmutsje": ("head", _H, "bakkersmutsje", [([-3.8, 15, -8.8], [7.6, 1.4, 5.6], 0), ([-4.6, 16.4, -9.6], [9.2, 2.6, 7.2], 0),
                                                          ([-3.6, 19.0, -8.6], [7.2, 0.6, 5.2], 0)]),
    "outfit_bakkersmutsje_knabbel": ("head", _H, "knabbeltje", [([-1.1, 16.9, -10.1], [2.2, 1.6, 0.6], 0)]),
}
CLOTHES = ["bakkersmutsje", "bakkersschortje", "meelstrikje"]


def clothes(rng, v):
    def muts():
        a = v.fabric((252, 250, 244), rng, 5)
        for y in range(0, a.shape[0], 5):
            a[y, :] = a[y, :] * 0.93
        return a

    def schort():
        a = v.fabric((252, 244, 226), rng, 6)                  # a cream apron...
        px = a.shape[0]
        for y in range(2, px, 8):                              # ...printed with little kaasknabbels
            for x in range((y // 8) % 2 * 4 + 2, px, 8):
                a[y:y + 2, x:x + 3] = (238, 150, 50)
                a[y, x + 1] = (252, 206, 72)
        for _ in range(px):                                    # and dusted with flour
            y, x = int(rng.integers(px)), int(rng.integers(px))
            a[y, x] = (255, 255, 255)
        return a

    def strik():
        return v.dots((252, 250, 246), (244, 138, 184), rng, every=4, size=1)

    return {
        "bakkersmutsje": {"bakkersmutsje": muts, "knabbeltje": lambda: v.fabric((236, 150, 52), rng, 10)},
        "bakkersschortje": {"suit": schort},
        "meelstrikje": {"bowtie": strik},
    }


def icons(ic):
    muts = ["....aaaaaaa.....", "..aabbbbbbbaa...", ".abbbbbbbbbbba..", ".abbbbbbbbbbba..", ".abbbbccbbbbba..", "..abbbccbbbba...",
            "...aaaaaaaaa....", "...addddddda....", "...aaaaaaaaa...."]
    schort = ["....a......a....", "....a......a....", "....abbbbbba....", "....abcbbcba....", "....abbbbbba....", "aaaaabbbbbbaaaaa",
              "...abbcbbbcbba..", "..abbbbbbbbbbba.", "..abbcbbbbcbbba.", "..abbbbbbbbbbba.", ".abbbbcbbbbcbbba", ".aaaaaaaaaaaaaaa"]
    strik = ["aa..........aa..", "abaa......aaba..", "abbba.aa.abbba..", "abcbbabbabbcba..", "abbbbabbabbbba..", "abcbbabbabbcba..",
             "abbba.aa.abbba..", "abaa......aaba..", "aa..........aa.."]
    return {
        "bakkersmutsje": ic.icon(ic.pad(muts), {"a": (150, 130, 110), "b": (252, 250, 244), "c": (236, 150, 52), "d": (226, 222, 212)}),
        "bakkersschortje": ic.icon(ic.pad(schort), {"a": (150, 120, 90), "b": (252, 244, 226), "c": (238, 150, 50)}),
        "meelstrikje": ic.icon(ic.pad(strik), {"a": (160, 90, 120), "b": (252, 250, 246), "c": (244, 138, 184)}),
    }


# ======================================================================================================================
# Bakker Korstje: a golden-crust guh with a tall toque (with a kaasknabbel) and a floury apron
# ======================================================================================================================
def korstje(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_bakkerguh"
    sw = npcs._swatches(geo, ["muts", "band", "knabbel", "schort"])
    geo["bones"].append({"name": "korstje_muts", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        npcs._cube([-4.0, 25.8, -4.6], [8, 1.6, 7], sw["band"]),
        npcs._cube([-4.8, 27.4, -5.4], [9.6, 3.2, 8.6], sw["muts"]),
        npcs._cube([-4.2, 30.6, -4.8], [8.4, 2.2, 7.4], sw["muts"]),
        npcs._cube([-3.0, 32.8, -3.6], [6.0, 1.0, 5.0], sw["muts"]),
        npcs._cube([-1.2, 27.8, -5.9], [2.4, 1.8, 0.6], sw["knabbel"])]})
    geo["bones"].append({"name": "korstje_schort", "parent": "body", "pivot": [0, 10, -4], "cubes": [
        npcs._cube([-3.6, 3.0, -5.05], [7.2, 7.4, 0.4], sw["schort"]),
        npcs._cube([-3.6, 10.4, -4.8], [0.6, 1.8, 0.4], sw["schort"]),
        npcs._cube([3.0, 10.4, -4.8], [0.6, 1.8, 0.4], sw["schort"])]})
    npcs._save_geo(h, "guh_npc_bakkerguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.085, sat=0.62, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2820)
    npcs._paint(a, sw["muts"], (252, 250, 244), rng, 5)
    npcs._paint(a, sw["band"], (244, 138, 184), rng, 6)
    npcs._paint(a, sw["knabbel"], (238, 150, 50), rng, 10)

    def schort(block):
        block[..., :3] = (252, 244, 226)
        for y in range(3, 32, 9):
            for x in range(3 + (y // 9) % 2 * 4, 32, 9):
                block[y:y + 3, x:x + 4, :3] = (238, 150, 50)
        block[26:30, 13:19, :3] = (244, 138, 184)             # a little pink pocket
    npcs._paint(a, sw["schort"], (252, 244, 226), rng, 4, schort)
    # flour on his cheeks and paws
    opaque = np.argwhere(a[..., 3] > 0)
    prng = np.random.default_rng(2821)
    for i in prng.choice(len(opaque), 60, replace=False):
        y, x = opaque[i]
        a[y, x, :3] = np.minimum(255, a[y, x, :3].astype(int) + 60)
    h.save(Image.fromarray(a), "entity", "npc_bakkerguh.png")


# ======================================================================================================================
# blocks, items, particles, sounds, tags, recipes
# ======================================================================================================================
def blocks_and_items(h):
    A, D = h.A, h.D
    # the knabbeloven: a round pink brick oven with a face, a chimney pipe and two little ears
    for lit in (False, True):
        name = "knabbeloven_aan" if lit else "knabbeloven"
        front = "guhs:block/knabbeloven_voor_aan" if lit else "guhs:block/knabbeloven_voor"
        h.w(f"{A}/models/block/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/knabbeloven_zijkant", "voor": front, "zij": "guhs:block/knabbeloven_zijkant",
                         "boven": "guhs:block/knabbeloven_boven", "pijp": "guhs:block/knabbeloven_pijp", "oor": "guhs:block/knabbeloven_oor"},
            "elements": [
                {"from": [1, 0, 1], "to": [15, 13, 15], "faces": {
                    "north": {"uv": [1, 3, 15, 16], "texture": "#voor"}, "south": {"uv": [1, 3, 15, 16], "texture": "#zij"},
                    "east": {"uv": [1, 3, 15, 16], "texture": "#zij"}, "west": {"uv": [1, 3, 15, 16], "texture": "#zij"},
                    "up": {"uv": [1, 1, 15, 15], "texture": "#boven"}, "down": {"uv": [1, 1, 15, 15], "texture": "#boven", "cullface": "down"}}},
                {"from": [10, 13, 10], "to": [13, 16, 13], "faces": {f: {"uv": [0, 0, 3, 3], "texture": "#pijp"}
                                                                     for f in ("north", "south", "east", "west", "up")}},
                {"from": [2, 13, 3], "to": [5, 15, 4], "faces": {f: {"uv": [4, 4, 7, 6], "texture": "#oor"} for f in ("north", "south", "east", "west", "up")}},
                {"from": [11, 13, 3], "to": [14, 15, 4], "faces": {f: {"uv": [4, 4, 7, 6], "texture": "#oor"} for f in ("north", "south", "east", "west", "up")}},
            ]})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    h.w(f"{A}/blockstates/knabbeloven.json", {"variants": {
        f"facing={f},lit={str(l).lower()}": ({"model": "guhs:block/knabbeloven_aan" if l else "guhs:block/knabbeloven", "y": r} if r else
                                              {"model": "guhs:block/knabbeloven_aan" if l else "guhs:block/knabbeloven"})
        for f, r in rot.items() for l in (False, True)}})
    h.w(f"{A}/models/item/knabbeloven.json", {"parent": "guhs:block/knabbeloven"})
    # the chimney pot
    h.w(f"{A}/models/block/bakkerij_schoorsteen.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/bakkerij_schoorsteen_zijkant", "zij": "guhs:block/bakkerij_schoorsteen_zijkant",
                     "boven": "guhs:block/bakkerij_schoorsteen_boven"},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 11, 12], "faces": {**{f: {"uv": [4, 5, 12, 16], "texture": "#zij"} for f in ("north", "south", "east", "west")},
                                                              "down": {"uv": [4, 4, 12, 12], "texture": "#boven", "cullface": "down"}}},
            {"from": [3, 11, 3], "to": [13, 14, 13], "faces": {**{f: {"uv": [3, 2, 13, 5], "texture": "#zij"} for f in ("north", "south", "east", "west")},
                                                               "up": {"uv": [3, 3, 13, 13], "texture": "#boven"},
                                                               "down": {"uv": [3, 3, 13, 13], "texture": "#boven"}}}]})
    h.w(f"{A}/blockstates/bakkerij_schoorsteen.json", {"variants": {"": {"model": "guhs:block/bakkerij_schoorsteen"}}})
    h.w(f"{A}/models/item/bakkerij_schoorsteen.json", {"parent": "guhs:block/bakkerij_schoorsteen"})
    # the invisible markers
    for marker in ("bakkerij_klantplek", "bakkerij_ingang"):
        h.w(f"{A}/models/block/{marker}.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
        h.w(f"{A}/blockstates/{marker}.json", {"variants": {"": {"model": f"guhs:block/{marker}"}}})
    for item in ITEMS:
        h.item_model(item)
    for b in ("knabbeloven", "bakkerij_schoorsteen"):
        h.self_drop(b)
    # particles
    h.w(f"{A}/particles/knabbelwolkje.json", {"textures": [f"guhs:knabbelwolkje_{i}" for i in range(3)]})
    h.w(f"{A}/particles/meelstofje.json", {"textures": [f"guhs:meelstofje_{i}" for i in range(2)]})
    # recipes
    h.shaped("knabbeloven", ["BKB", "BFB", "BPB"], {"B": "minecraft:bricks", "K": "guhs:kaas_knabbels", "F": "minecraft:furnace",
                                                    "P": "minecraft:pink_dye"}, "guhs:knabbeloven")
    h.shaped("bakkerij_schoorsteen", ["B B", "BKB"], {"B": "minecraft:brick", "K": "guhs:kaas_knabbels"}, "guhs:bakkerij_schoorsteen")
    # tags: the pastries are gebak (theehuis, the feestbuffet, lekkernij for the Kruimel-Mika's), the feesttaart is the task
    # item, a bakmunt pays for the grijpmachine
    h.add_tag("guhs/tags/item/knus/gebak", [f"guhs:{r}" for r in RECEPTEN] + ["guhs:feesttaart"])
    h.add_tag("guhs/tags/item/knus/feesttaart", ["guhs:feesttaart"])
    h.add_tag("guhs/tags/item/knus/grijptickets", ["guhs:bakmunt"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:knabbeloven", "guhs:bakkerij_schoorsteen"])

    def patch(d):
        d["bakkerij.oven_ding"] = {"sounds": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.4, "volume": 0.8}],
                                   "subtitle": "subtitles.guhs.bakkerij.oven_ding"}
        d["bakkerij.bestelling"] = {"sounds": [{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.2, "volume": 0.7}],
                                    "subtitle": "subtitles.guhs.bakkerij.bestelling"}
    h.patch_json(f"{A}/sounds.json", patch)


# ======================================================================================================================
# advancements
# ======================================================================================================================
QUEST = ["bakkerij_gespeeld", "bakkerij_eerste_bestelling", "bakkerij_combo", "bakkerij_100", "bakkerij_250", "bakkerij_perfect", "bakkerij_oven",
         "bakkerij_highscore", "bakkerij_receptenboek_vol", "seen_bakkerguh"]


def advancements(h):
    D = h.D
    for name in QUEST:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.w(f"{D}/advancement/quest/bakkerij_pakje.json", {"criteria": {
        c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in CLOTHES}})
    impossible = {"done": {"trigger": "minecraft:impossible"}}
    for name, parent, icon, frame in (("bakkerij_eerste_bestelling", "stadje", "guhs:bakmunt", "task"),
                                      ("bakkerij_combo", "bakkerij_eerste_bestelling", "guhs:vadsdonut", "goal"),
                                      ("bakkerij_receptenboek_vol", "bakkerij_eerste_bestelling", "guhs:knabbeloven", "challenge")):
        h.w(f"{D}/advancement/knuffeldal/{name}.json", {
            "parent": f"guhs:knuffeldal/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                        "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": impossible})


# ======================================================================================================================
# lang (Dutch in both files)
# ======================================================================================================================
LANG = {
    # names
    "entity.guhs.guh_npc.bakkerguh": "Bakker Korstje",
    "gui.guhs.guhdex.rarity.bakkerguh": "Zeldzaamheid: Uniek (Knabbelbakkerij, Knuffeldal)",
    "gui.guhs.guhdex.info.bakkerguh": "Bakker Korstje bakt al zijn hele leven kaasknabbelbrood, en hij is er zelf een beetje op gaan lijken: goudbruin en "
                                      "altijd onder het meel. Help hem met de bestellingen, dan worden alle guhs in het dal weer lekker VAHOEG.",
    "entity.guhs.bakkerij_klant": "Klantje",
    "entity.guhs.bakkerij_klant.feest": "Feestklantje",
    "block.guhs.knabbeloven": "Knabbeloven",
    "block.guhs.bakkerij_schoorsteen": "Knabbelschoorsteentje",
    "block.guhs.bakkerij_klantplek": "Bakkerij-klantplek",
    "block.guhs.bakkerij_ingang": "Bakkerij-ingang",
    "item.guhs.bakmunt": "Bakmunt",
    "item.guhs.feesttaart": "Feesttaart",
    "item.guhs.feesttaart.lore": "Drie lagen vadsig geluk, in een roze doos met een strik",
    "item.guhs.feesttaart.lore2": "Voor Burgemeester Vadsema en het Grote Knusfeest. Niet onderweg opeten, njeg!",
    "item.guhs.bakkersmutsje": "Bakkersmutsje",
    "item.guhs.bakkersschortje": "Bakkersschortje",
    "item.guhs.meelstrikje": "Meelstrikje",
    "structure.guhs.knabbelbakkerij": "Knabbelbakkerij",
    "subtitles.guhs.bakkerij.oven_ding": "Oven: ding!",
    "subtitles.guhs.bakkerij.bestelling": "Klantje komt binnen",
    # the pastries: name, a line on the tooltip, what you say when you eat it
    "item.guhs.knabbelbroodje": "Knabbelbroodje",
    "item.guhs.knabbelbroodje.lore": "Knapperig broodje met knabbelkruimels. Maakt je snel!",
    "item.guhs.knabbelbroodje.njam": "Njam! Knabbelvaart: je rent als een hongerige guh!",
    "item.guhs.kaaskrakeling": "Kaaskrakeling",
    "item.guhs.kaaskrakeling.lore": "Een gevlochten krakeling van kaasdeeg met suiker. Handjes vlug!",
    "item.guhs.kaaskrakeling.njam": "Knapperdeknap! Je handjes gaan ineens vlug.",
    "item.guhs.vadsvlaai": "Vadsvlaai",
    "item.guhs.vadsvlaai.lore": "Een hele kaasvlaai. Vadsig? Nee hoor: gewoon VAHOEG.",
    "item.guhs.vadsvlaai.njam": "Mmm, vadsvlaai! Een extra laagje knuffelvet voor als het tegenzit.",
    "item.guhs.guhcroissant": "Guhcroissant",
    "item.guhs.guhcroissant.lore": "Een halvemaan van bladerdeeg met kaas. Hij kijkt je lief aan.",
    "item.guhs.guhcroissant.njam": "Boing! Van een guhcroissant word je hupsig.",
    "item.guhs.knabbelkoekje": "Knabbelkoekje",
    "item.guhs.knabbelkoekje.lore": "Een klein koekje voor tussendoor. Snel op, snel weg!",
    "item.guhs.knabbelkoekje.njam": "Kruimel kruimel... en hup, vol gas!",
    "item.guhs.kaasbolletje": "Kaasbolletje",
    "item.guhs.kaasbolletje.lore": "Een zacht bolletje met gesmolten kaas. Maakt je weer heel.",
    "item.guhs.kaasbolletje.njam": "Warm en kaasig... je voelt je meteen beter, njeg.",
    "item.guhs.pluismuffin": "Pluismuffin",
    "item.guhs.pluismuffin.lore": "Zo pluizig dat je er zachtjes van zweeft.",
    "item.guhs.pluismuffin.njam": "Pluis! Je voelt je licht als een wolkje.",
    "item.guhs.theetaartje": "Theetaartje",
    "item.guhs.theetaartje.lore": "Met een roze hartje en poedersuiker. Brengt geluk (en zin in thee).",
    "item.guhs.theetaartje.njam": "Wat een lief taartje. Vandaag wordt een geluksdag!",
    "item.guhs.knabbeltompouce": "Knabbeltompouce",
    "item.guhs.knabbeltompouce.lore": "Roze glazuur, gele room, twee knapperige laagjes. Stevig spul!",
    "item.guhs.knabbeltompouce.njam": "Plets, room op je snoet! Je voelt je sterk als een guhkasteel.",
    "item.guhs.vadsdonut": "Vadsdonut",
    "item.guhs.vadsdonut.lore": "Een ringetje met roze glazuur en spikkels. Stuiter stuiter!",
    "item.guhs.vadsdonut.njam": "Spikkels! Je benen willen springen!",
    "item.guhs.guhwafel": "Guhwafel",
    "item.guhs.guhwafel.lore": "Een ronde wafel met suiker. Onder water blijf je er lang van ademen.",
    "item.guhs.guhwafel.njam": "Wafel-bubbels! Nu kun je lang onder water knabbelen.",
    "item.guhs.sterrenkoekje": "Sterrenkoekje",
    "item.guhs.sterrenkoekje.lore": "Een sterretje met roze glazuur: je ziet er 's nachts alles mee.",
    "item.guhs.sterrenkoekje.njam": "Twinkel twinkel! Je ogen zien in het donker.",
    # the dough / shape / topping choices, the qualities
    "gui.guhs.bakkerij.deeg.knabbeldeeg": "Knabbeldeeg", "gui.guhs.bakkerij.deeg.kaasdeeg": "Kaasdeeg",
    "gui.guhs.bakkerij.deeg.zoetdeeg": "Zoet deeg", "gui.guhs.bakkerij.deeg.bladerdeeg": "Bladerdeeg",
    "gui.guhs.bakkerij.vorm.bolletje": "Bolletje", "gui.guhs.bakkerij.vorm.vlechtje": "Vlechtje", "gui.guhs.bakkerij.vorm.taartje": "Taartje",
    "gui.guhs.bakkerij.vorm.maantje": "Maantje", "gui.guhs.bakkerij.vorm.plaatje": "Plaatje", "gui.guhs.bakkerij.vorm.ringetje": "Ringetje",
    "gui.guhs.bakkerij.vorm.sterretje": "Sterretje",
    "gui.guhs.bakkerij.topping.kaas": "Kaas", "gui.guhs.bakkerij.topping.suiker": "Poedersuiker",
    "gui.guhs.bakkerij.topping.glazuur": "Roze glazuur", "gui.guhs.bakkerij.topping.kruimels": "Knabbelkruimels",
    "gui.guhs.bakkerij.kwaliteit.rauw": "Een tikje rauw", "gui.guhs.bakkerij.kwaliteit.goed": "Goed gebakken",
    "gui.guhs.bakkerij.kwaliteit.perfect": "PERFECT", "gui.guhs.bakkerij.kwaliteit.aangebrand": "Aangebrand",
    "gui.guhs.bakkerij.recept.regel": "%s + %s + %s",
    "gui.guhs.bakkerij.voor_klant": "Dat is voor een klantje! Rechtsklik op het klantje om het te geven.",
    "gui.guhs.bakkerij.voor_klant.tooltip": "Voor een klantje van Bakker Korstje",
    "gui.guhs.bakkerij.guh_eet": "%s smult van je %s. VAHOEG!",
    # the ingredients
    "gui.guhs.bakkerij.nodig.graan": "Knabbelgraan (of tarwe)",
    "gui.guhs.bakkerij.nodig.kaasmelk": "Kaasmelk (of melk)",
    "gui.guhs.bakkerij.nodig.ei": "Knabbelei (of ei)",
    "gui.guhs.bakkerij.nodig.knabbels": "Kaasknabbels",
    "gui.guhs.bakkerij.nodig.suiker": "Suiker",
    "gui.guhs.bakkerij.nodig.roze": "Roze kleurstof",
    "gui.guhs.bakkerij.nodig.kruimels": "Brood of koekje (kruimels)",
    "gui.guhs.bakkerij.vers": "Met verse spullen uit het Knuffeldal (boerderij en tuintjes) bak je er eentje extra!",
    # the baking screen
    "gui.guhs.bakkerij.scherm.oven": "Knabbeloven",
    "gui.guhs.bakkerij.scherm.spel": "Bakken voor de klantjes!",
    "gui.guhs.bakkerij.kies.deeg": "Deeg", "gui.guhs.bakkerij.kies.vorm": "Vorm", "gui.guhs.bakkerij.kies.topping": "Topping",
    "gui.guhs.bakkerij.wordt": "Dit wordt: %s",
    "gui.guhs.bakkerij.wordt.niets": "Dat is geen recept... njeg.",
    "gui.guhs.bakkerij.wordt.nieuw": "Een nieuw recept?! Probeer maar!",
    "gui.guhs.bakkerij.wordt.feesttaart": "Dit wordt: de FEESTTAART!",
    "gui.guhs.bakkerij.wordt.geheim": "Hmm... dat lijkt Korstjes geheime recept.",
    "gui.guhs.bakkerij.geheim": "Het feesttaart-recept bakt alleen Bakker Korstje, voor het Grote Knusfeest.",
    "gui.guhs.bakkerij.in_de_oven": "In de oven!",
    "gui.guhs.bakkerij.eruit": "ERUIT!",
    "gui.guhs.bakkerij.spatie": "Spatie of ERUIT! als het wijzertje in het oranje staat",
    "gui.guhs.bakkerij.uitleg": "Kies deeg, vorm en topping en schuif het in de oven",
    "gui.guhs.bakkerij.bakt": "%s zit in de oven... let op het wijzertje!",
    "gui.guhs.bakkerij.uit.spel": "%s is klaar (%s)! Snel naar het klantje!",
    "gui.guhs.bakkerij.uit.oven": "%s x %s: %s!",
    "gui.guhs.bakkerij.uit.aangebrand": "Oei, je %s is aangebrand... njeg.",
    "gui.guhs.bakkerij.bestellingen": "Bestellingen",
    "gui.guhs.bakkerij.geen_bestellingen": "Nog geen klantjes... ze komen zo!",
    "gui.guhs.bakkerij.score": "%s punten, combo x%s",
    "gui.guhs.bakkerij.nodig": "Nodig",
    "gui.guhs.bakkerij.te_ver": "Je staat te ver van de oven.",
    "gui.guhs.bakkerij.al_bezig": "Er zit al iets in de oven!",
    "gui.guhs.bakkerij.geen_recept": "Dat is geen recept... njeg.",
    "gui.guhs.bakkerij.te_weinig": "Je hebt niet genoeg spulletjes voor dit recept.",
    "gui.guhs.bakkerij.andere_oven": "Gebruik de ovens van de bakkerij waar je speelt!",
    "gui.guhs.bakkerij.vergeten": "Je was de oven vergeten... aangebrand, njeg!",
    # Korstje's screen
    "gui.guhs.bakkerij.play": "Bakken!",
    "gui.guhs.bakkerij.play.tooltip": "Twee minuten lang ben jij de bakker: help de klantjes aan hun bestelling",
    "gui.guhs.bakkerij.shop": "Winkeltje",
    "gui.guhs.bakkerij.shop.tooltip": "Bakkerspakje, een eigen knabbeloven en bakspulletjes voor bakmunten",
    "gui.guhs.bakkerij.rules": "Klantjes komen binnen met een wolkje boven hun hoofd: dat willen ze. Klik op een knabbeloven, kies deeg, vorm en "
                               "topping (of klik op een bestelling), schuif het in de oven en haal het eruit als het wijzertje in het oranje staat. "
                               "Rechtsklik dan op het klantje! Drie blije klantjes op rij: combo! Van bakmunten koop je leuke dingen.",
    "gui.guhs.bakkerij.busy": "%s bakt nu (nog %s seconden, %s punten). Kijk maar mee!",
    "gui.guhs.bakkerij.first": "De eerste keer krijg je extra bakmunten. VAHOEG!",
    "gui.guhs.bakkerij.free": "De ovens zijn warm. Klaar om te bakken?",
    "gui.guhs.bakkerij.feest": "Knusfeest! Haal %s punten, dan komt er een feestklantje voor de FEESTTAART.",
    "gui.guhs.bakkerij.record": "Record: %s punten · %s keer gebakken · %s/12 recepten",
    "gui.guhs.scorebord.bakkerij": "Knabbelbakkerij",
    "gui.guhs.scorebord.bakkerij.punten": "Beste bakkers",
    # Korstje talks
    "quest.guhs.bakkerij.hello": "Goeiemorgen! Ik ben Bakker Korstje. De Mika's hebben mijn kaasknabbels weer gepikt, en nu zijn de guhs in het dal "
                                 "niet VAHOEG genoeg meer... Help je me met de bestellingen?",
    "quest.guhs.bakkerij.hello.feest": "Het Grote Knusfeest! De burgemeester wil een feesttaart, en die bak ik alleen voor een echte bakker. "
                                       "Laat eens zien wat je kan!",
    "quest.guhs.bakkerij.busy": "Sssst, er wordt gebakken! Straks mag jij.",
    "quest.guhs.bakkerij.busy_you": "Je bent aan het bakken! De klantjes wachten, vads!",
    "quest.guhs.bakkerij.elsewhere": "Je bakt al ergens anders. Eén bakkerij tegelijk!",
    "quest.guhs.bakkerij.broken": "Njeg... mijn bakkerij is in de war, ik kan mijn ovens niet vinden.",
    "quest.guhs.bakkerij.shop": "Mijn winkeltje! Het bakkerspakje verkoop ik alleen hier. Njam!",
    "quest.guhs.bakkerij.go": "Schort voor, handjes gewassen? Klik op een knabbeloven om te bakken, en geef het klantje wat het wil. Klaar...?",
    "quest.guhs.bakkerij.go.feest": "Schort voor! Haal eerst %s punten, dan komt het feestklantje voor de feesttaart. Klaar...?",
    "quest.guhs.bakkerij.invite1": "Warme broodjes! Kom je helpen bakken?",
    "quest.guhs.bakkerij.invite2": "De klantjes staan te trappelen... njeg njeg!",
    "quest.guhs.bakkerij.invite3": "Wie wil er bakker zijn? Praat maar met mij!",
    "quest.guhs.bakkerij.ready": "De ovens worden warm...",
    "quest.guhs.bakkerij.title.go": "BAKKEN!",
    "quest.guhs.bakkerij.title.go.sub": "Klik op een knabbeloven",
    "quest.guhs.bakkerij.cheer.start": "Daar komen de klantjes! Vads vads vads!",
    "quest.guhs.bakkerij.cheer.half": "Nog één minuut! Doorbakken, kleine bakker!",
    "quest.guhs.bakkerij.cheer.ten": "Nog tien seconden! Snel snel snel!",
    "quest.guhs.bakkerij.cheer.feest": "Kijk, het feestklantje is er! Bak de FEESTTAART: zoet deeg, taartje, roze glazuur!",
    "quest.guhs.bakkerij.bar": "%s punten · nog %s s · combo x%s · %s klantjes blij",
    "quest.guhs.bakkerij.gebakken": "%s: %s!",
    "quest.guhs.bakkerij.vol": "Je zakken zitten vol! Geef eerst wat aan de klantjes.",
    "quest.guhs.bakkerij.aangebrand": "Aangebrand! Je combo is weg... njeg.",
    "quest.guhs.bakkerij.verkeerd": "Dat hadden ze niet besteld! Combo weg.",
    "quest.guhs.bakkerij.gemist": "Een klantje wachtte te lang. Combo weg.",
    "quest.guhs.bakkerij.combo_weg": "Oei, je combo is weg!",
    "quest.guhs.bakkerij.combo": "COMBO x%s!",
    "quest.guhs.bakkerij.geserveerd": "+%s! (%s punten, combo x%s)",
    "quest.guhs.bakkerij.feesttaart": "Bakker Korstje pakt een echte feesttaart voor je in: breng hem naar Burgemeester Vadsema! Pas op voor Kruimel-Mika's...",
    "quest.guhs.bakkerij.title.feesttaart": "FEESTTAART!",
    "quest.guhs.bakkerij.title.feesttaart.sub": "Breng hem naar de burgemeester",
    "quest.guhs.bakkerij.done": "Klaar! %s punten, %s blije klantjes, %s perfect gebakken, beste combo %s.",
    "quest.guhs.bakkerij.munten": "Je verdient %s bakmunten!",
    "quest.guhs.bakkerij.first": "Je eerste keer bakken! Hier, %s bakmunten extra. VAHOEG!",
    "quest.guhs.bakkerij.record": "Nieuw record: %s punten!",
    "quest.guhs.bakkerij.best": "Je record is %s punten.",
    "quest.guhs.bakkerij.title.record": "NIEUW RECORD!",
    "quest.guhs.bakkerij.title.end": "Winkel dicht!",
    "quest.guhs.bakkerij.title.points": "%s punten",
    "quest.guhs.bakkerij.end.super": "Wat een bakker! Het hele dal is weer VAHOEG. Kom je morgen weer?",
    "quest.guhs.bakkerij.end.good": "Lekker gebakken, vads! De klantjes smullen.",
    "quest.guhs.bakkerij.end.ok": "Dat ging al best goed! Volgende keer nog sneller, njeg.",
    "quest.guhs.bakkerij.stopped": "Bakken gestopt (%s punten). De klantjes gaan naar huis.",
    # the customers
    "quest.guhs.bakkerij.klant.wens": "Mag ik een %s? (%s, %s, %s) Njeg!",
    "quest.guhs.bakkerij.klant.feestwens": "Ik kom de %s halen! Zoet deeg, taartje, roze glazuur... voor het Knusfeest!",
    "quest.guhs.bakkerij.klant.verkeerd": "Njeg... ik wilde een %s.",
    "quest.guhs.bakkerij.klant.blij1": "Mijn %s! Dankjewel! VAHOEG!",
    "quest.guhs.bakkerij.klant.blij2": "Njam njam, een %s! Nu word ik weer VAHOEG.",
    "quest.guhs.bakkerij.klant.blij3": "Precies wat ik wilde, een %s! Njeg!",
    "quest.guhs.bakkerij.klant.blij4": "Een %s, helemaal voor mij! Wat vadsig lief!",
    "quest.guhs.bakkerij.klant.weg": "Ik moet naar huis... misschien morgen, njeg.",
    "quest.guhs.bakkerij.klant.niet_jij": "Ik wacht op de bakker van Korstje, njeg.",
    # the Knus tab
    "gui.guhs.knus.mijlpaal.bakkerij_eerste": "Je eerste blije klantje",
    "gui.guhs.knus.mijlpaal.bakkerij_bestellingen": "50 blije klantjes",
    "gui.guhs.knus.mijlpaal.bakkerij_combo": "Combo van 9 op rij",
    "gui.guhs.knus.mijlpaal.bakkerij_highscore": "250 punten in één spel",
    "gui.guhs.knus.mijlpaal.bakkerij_perfect": "25 keer perfect gebakken",
    "gui.guhs.knus.mijlpaal.bakkerij_zelfgebakken": "30 lekkernijen uit je eigen oven",
    "gui.guhs.knus.mijlpaal.bakkerij_recepten": "Het receptenboek vol",
    "gui.guhs.knus.mijlpaal.bakkerij_feesttaart": "De feesttaart gebakken",
    "gui.guhs.knus.verzameling.receptenboek": "Receptenboek",
    # advancements
    "advancements.guhs.knuffeldal.bakkerij_eerste_bestelling.title": "Warme broodjes!",
    "advancements.guhs.knuffeldal.bakkerij_eerste_bestelling.description": "Help een klantje in de Knabbelbakkerij aan zijn bestelling",
    "advancements.guhs.knuffeldal.bakkerij_combo.title": "Bakken aan de lopende band",
    "advancements.guhs.knuffeldal.bakkerij_combo.description": "Maak zes klantjes op rij blij bij Bakker Korstje",
    "advancements.guhs.knuffeldal.bakkerij_receptenboek_vol.title": "Meesterbakker",
    "advancements.guhs.knuffeldal.bakkerij_receptenboek_vol.description": "Bak alle twaalf recepten van het receptenboek",
}

RECEPT_INFO = {
    "knabbelbroodje": ("Knabbeldeeg", "Bolletje", "Knabbelkruimels", "Het allereerste recept van Bakker Korstje: 's ochtends vroeg ruikt het hele dal ernaar."),
    "kaaskrakeling": ("Kaasdeeg", "Vlechtje", "Poedersuiker", "Korstje vlecht ze met zijn oren. Echt waar, njeg."),
    "vadsvlaai": ("Zoet deeg", "Taartje", "Kaas", "Voor als je niet VAHOEG genoeg bent: een hele vlaai vol kaas."),
    "guhcroissant": ("Bladerdeeg", "Maantje", "Kaas", "Hij kijkt je zo lief aan dat je hem bijna niet durft op te eten. Bijna."),
    "knabbelkoekje": ("Knabbeldeeg", "Plaatje", "Poedersuiker", "Klein en snel: een koekje voor onderweg."),
    "kaasbolletje": ("Kaasdeeg", "Bolletje", "Kaas", "Zacht, warm en kaasig. Guhs noemen het 'een knuffel om op te eten'."),
    "pluismuffin": ("Zoet deeg", "Bolletje", "Roze glazuur", "Met twee oortjes van glazuur. Zo pluizig dat je ervan zweeft."),
    "theetaartje": ("Bladerdeeg", "Taartje", "Poedersuiker", "Mevrouw Theelepel bestelt er elke dag drie, voor bij de thee."),
    "knabbeltompouce": ("Bladerdeeg", "Plaatje", "Roze glazuur", "Roze glazuur, gele room: niemand eet hem zonder knoeien."),
    "vadsdonut": ("Zoet deeg", "Ringetje", "Roze glazuur", "Met regenboogspikkels. De favoriet van alle guhbaby's."),
    "guhwafel": ("Zoet deeg", "Plaatje", "Poedersuiker", "Een ronde wafel. Zeemeerguhs nemen hem mee naar de bodem van de zee."),
    "sterrenkoekje": ("Knabbeldeeg", "Sterretje", "Roze glazuur", "Gebakken bij sterrenlicht. Professor Sterretje zweert erbij."),
}


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for r, (d, v, t, info) in RECEPT_INFO.items():
        naam = LANG[f"item.guhs.{r}"]
        h.lang(f"gui.guhs.knus.receptenboek.{r}", naam, naam)
        txt = f"{d} + {v} + {t}. {info}"
        h.lang(f"gui.guhs.knus.receptenboek.{r}.info", txt, txt)


# ======================================================================================================================
# the self-check of our own assets (check_assets.py only knows the registry classes)
# ======================================================================================================================
def selfcheck(h):
    A = h.A
    missing = []
    for b in ("knabbeloven", "bakkerij_schoorsteen", "bakkerij_klantplek", "bakkerij_ingang"):
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL or f"block.guhs.{b}" not in h.EN:
            missing.append(f"lang block {b}")
    for i in ITEMS + ["knabbeloven", "bakkerij_schoorsteen"]:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
    for i in ITEMS:
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item {i}")
    for r in RECEPTEN:
        for k in (f"item.guhs.{r}.lore", f"item.guhs.{r}.njam", f"gui.guhs.knus.receptenboek.{r}", f"gui.guhs.knus.receptenboek.{r}.info"):
            if k not in h.NL:
                missing.append(f"lang {k}")
    for root, _dirs, files in os.walk(f"{A}/models"):
        for f in files:
            if f.startswith(("knabbeloven", "bakkerij_")) or f[:-5] in ITEMS:
                import json
                model = json.load(open(os.path.join(root, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    for t in ("entity/npc_bakkerguh", "entity/bakkerij_bubbel", "gui/bakkerij_knoppen", "particle/knabbelwolkje_0", "particle/meelstofje_0"):
        if not os.path.exists(f"{A}/textures/{t}.png"):
            missing.append(f"texture {t}")
    if not os.path.exists(f"{A}/geckolib/models/entity/guh_npc_bakkerguh.geo.json"):
        missing.append("geo guh_npc_bakkerguh")
    if missing:
        raise SystemExit(f"bakkerij assets missing: {missing}")


def build(h):
    tex.build(h)
    korstje(h)
    blocks_and_items(h)
    advancements(h)
    texts(h)
    # the building (the plein slot) and the GameTest bakery, both checked
    b = gebouw.bouw(h)
    problems = gebouw.st.check(b, gebouw.check_bakkerij)
    if problems:
        raise SystemExit("knuffeldal_stadje/bakkerij geometry check failed:\n  " + "\n  ".join(problems[:60]))
    gebouw.zet_markers(b)
    b.s.save("knuffeldal_stadje/bakkerij")
    gebouw.test_bakkerij(h)
    h.lang("gui.guhs.knus.slot.bakkerij", "Knabbelbakkerij", "Knabbelbakkerij")
    selfcheck(h)
    print(f"bakkerij: building ok ({len(b.s.blocks)} blocks, {b.walkable} walkable, {b.gezichten} + {b.grote_gezichten} faces)")


# ======================================================================================================================
# FTB quests (row y = 83), no dependencies
# ======================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("bakkerij_korstje", "Bakker Korstje", "Aan de noordkant van het plein in het Knuffeldal staat een reusachtig kaasknabbelbrood met een guhgezicht: "
      "de &6Knabbelbakkerij&r! Zeg hallo tegen &6Bakker Korstje&r (hij staat ook in je Guhdex).",
      "guhs:knabbeloven", [fq.adv("seen_bakkerguh")], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=100)
    q("bakkerij_spelen", "Schort voor!", "Praat met Bakker Korstje en druk op &6Bakken!&r Twee minuten lang ben jij de bakker: klantjes komen binnen "
      "met een wolkje boven hun hoofd, en dat willen ze hebben.", "guhs:bakmunt", [fq.adv("bakkerij_gespeeld")],
      rewards=(("guhs:bakmunt", 3),), x=-6.5, y=y, xp=100)
    q("bakkerij_eerste", "Warme broodjes!", "Klik op een knabbeloven, kies deeg, vorm en topping, schuif het in de oven en haal het eruit als het "
      "wijzertje in het oranje staat. Rechtsklik dan op het klantje. VAHOEG!", "guhs:knabbelbroodje", [fq.adv("bakkerij_eerste_bestelling")],
      rewards=(("guhs:kaas_knabbels", 12),), x=-5, y=y, xp=100)
    q("bakkerij_combo", "Combo!", "Maak zes klantjes op rij blij zonder iets te laten aanbranden of iemand te laten wachten.",
      "guhs:vadsdonut", [fq.adv("bakkerij_combo")], rewards=(("guhs:bakmunt", 4),), x=-3.5, y=y, xp=150)
    q("bakkerij_100", "Kleine bakker", "Haal 100 punten in één keer bakken.", "guhs:kaasbolletje", [fq.adv("bakkerij_100")],
      rewards=(("guhs:bakmunt", 3),), x=-2, y=y, xp=150)
    q("bakkerij_250", "Grote bakker", "Haal 250 punten in één keer bakken. Je staat dan vast in de Highscores van je Guhdex!",
      "guhs:vadsvlaai", [fq.adv("bakkerij_250")], rewards=(("guhs:bakmunt", 6),), x=-0.5, y=y, shape="gear", xp=300)
    q("bakkerij_perfect", "Perfect!", "Haal iets uit de oven precies als het wijzertje midden in het oranje staat.",
      "guhs:sterrenkoekje", [fq.adv("bakkerij_perfect")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y, xp=100)
    q("bakkerij_oven", "Je eigen knabbeloven", "Koop een &6knabbeloven&r bij Korstje (of maak er een) en bak zelf: graan of tarwe, melk, eieren, "
      "suiker... Met spullen uit de Guhboerderij en je tuintjes bak je er eentje extra!", "guhs:knabbeloven", [fq.adv("bakkerij_oven")],
      rewards=(("minecraft:sugar", 8),), x=2.5, y=y, xp=150)
    q("bakkerij_recepten", "Het receptenboek", "Bak alle twaalf recepten goed of perfect: ze komen in het receptenboek in je Guhdex (tabblad Knus).",
      "guhs:guhcroissant", [fq.adv("bakkerij_receptenboek_vol")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=4, y=y, shape="gear", xp=400)
    q("bakkerij_pakje", "Helemaal bakker", "Koop het bakkersmutsje, het bakkersschortje en het meelstrikje in Korstjes winkeltje.",
      "guhs:bakkersmutsje", [fq.adv("bakkerij_pakje")], rewards=(("guhs:bakmunt", 3),), x=5.5, y=y, xp=150)
    q("bakkerij_munten", "Bakmuntjes", "Spaar 10 bakmunten. Je kunt er ook een potje mee spelen bij de grijpmachine!",
      "guhs:bakmunt", [fq.item("guhs:bakmunt", 10)], rewards=(("guhs:kaas_knabbels", 8),), x=7, y=y, xp=100)
