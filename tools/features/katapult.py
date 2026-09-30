"""
De Knabbelkatapult (2.9, De Grote Guhspelen; slice sjoelkatapult): a guh castle with a catapult on the Vadskliffen,
opposite a crooked Mika fort. Kapitein Floepguh lends you pluisballen; knock 12 Mika forts to bits and free the knabbels.

  build(h)    the blocks (werper, fortplek, Mika figure, knabbelkist, mikaplank), the pluisbal entity texture, the
              katapultster coin and the loaned pluisballen, Kapitein Floepguh's model and texture, all texts (Dutch in both
              languages), the advancements (tab De Grote Guhspelen), the 12 fort templates (katapult_forten.py) and the
              knabbelkatapult structure (katapult_bouw.py), each with a self-check
  ftb(fq)     the katapult quests (section "katapult" of guhs_minigames)
  BONES / clothes / CLOTHES / icons: the Kapitein's outfit: het katapulthelmpje (a round helmet with a pink feather), de
              katapultriem (a belt with a buckle and a pouch with a pluisbal) and de pluisbal-oorbelletjes (OREN)

Java side: nl.juiced.guhs.feature.katapult (KatapultGame, KatapultFort, PluisbalEntity, KatapultBrokjeEntity, ...).
"""
import random

import numpy as np
from PIL import Image

from features import katapult_bouw as bouw
from features import katapult_forten as forten
from features import spelen
from features import sterrenwacht_hulp as hulp

NAME = "knabbelkatapult"
PINK = (250, 170, 206)
FLUFF = (252, 188, 218)
WOOD = (132, 92, 58)
MIKAPURPLE = (104, 62, 112)

# ---------------------------------------------------------------------------------------------------------------------
# the outfit
# ---------------------------------------------------------------------------------------------------------------------
_H = [0, 6, -2]
_B = [0, 6, 6]
BONES = {
    # a round helmet (a bowl on the head with a rim) and a tall pink feather on the side
    "outfit_katapulthelm": ("head", _H, "katapulthelm", [([-5.2, 14.6, -11.2], [10.4, 2.4, 9.6], 0), ([-4.2, 17.0, -10.2], [8.4, 1.0, 7.6], 0),
                                                       ([-5.8, 14.6, -12.4], [11.6, 0.5, 11.4], 0)]),
    "outfit_katapulthelm_veer": ("head", _H, "katapulthelm_veer", [([4.2, 16.0, -6.0], [0.7, 5.5, 0.7], 0), ([4.0, 20.0, -5.2], [1.1, 2.5, 2.2], 0),
                                                                 ([4.1, 21.8, -4.0], [0.9, 1.6, 1.6], 0)]),
    # a leather belt round the belly with a golden buckle and a pouch (with a pluisbal peeking out) on the side
    "outfit_katapultriem": ("body", _B, "katapultriem", [([-6.9, 10.6, 4], [13.8, 0.8, 2.2], 0), ([6.3, 1.2, 4], [0.8, 9.6, 2.2], 0),
                                                        ([-7.1, 1.2, 4], [0.8, 9.6, 2.2], 0), ([-6.9, 0.6, 4], [13.8, 0.8, 2.2], 0),
                                                        ([-8.4, 3.0, 3.4], [1.4, 3.6, 3.4], 0)]),
    "outfit_katapultriem_gesp": ("body", _B, "katapultriem_gesp", [([7.0, 4.4, 4.2], [0.6, 2.6, 1.8], 0)]),
    "outfit_katapultriem_bal": ("body", _B, "katapultriem_bal", [([-8.3, 6.4, 4.0], [1.3, 1.3, 1.3], 0.15)]),
}
# pluisbal earrings: a stud under each ear, a tiny chain and a fluffy pink ball
BONES.update(spelen.oren("outfit_oren_oorbel", "oren_oorbel", cubes=[([9.2, 10.5, -5.95], [0.7, 0.6, 0.6], 0), ([9.35, 9.3, -5.85], [0.4, 1.2, 0.4], 0),
                                                                     ([8.75, 7.7, -6.3], [1.6, 1.6, 1.6], 0.1)]))
CLOTHES = ["katapult_helmpje", "katapult_riem", "katapult_oorbelletjes"]


def _fluff(rng, v, base):
    a = v.fabric(base, rng, 16)
    px = v.SWATCH * 4
    r = np.random.default_rng(int(rng.random() * 1e6) if hasattr(rng, "random") else 1)
    for _ in range(90):
        x, y = r.integers(0, px, 2)
        a[y, x] = tuple(min(255, c + 22) for c in base)
    return a


def clothes(rng, v):
    return {
        "katapult_helmpje": {"katapulthelm": lambda: v.band((120, 140, 170), (250, 210, 90), rng, (26, 27)),
                             "katapulthelm_veer": lambda: v.fabric((246, 110, 170), rng, 14)},
        "katapult_riem": {"katapultriem": lambda: v.fabric((120, 76, 44), rng, 10),
                          "katapultriem_gesp": lambda: v.metal((246, 200, 70), rng),
                          "katapultriem_bal": lambda: _fluff(rng, v, FLUFF)},
        "katapult_oorbelletjes": {"oren_oorbel": lambda: _fluff(rng, v, (250, 150, 200))},
    }


RIEM_ICON = ["................", "................", "................", "................", "..........ppp...",
             "..........pwp...", "aaaaaaaaaaabbba.", "cccccccggcccccc.", "cccccccgyccccccb", "aaaaaaaggaaabbba",
             "..........bbbb..", "..........bbbb..", "................", "................", "................", "................"]
OORBEL_ICON = ["................", "................", "...gg......gg...", "...gg......gg...", "....k......k....", "....k......k....",
               "...ppp....ppp...", "..pwppp..pwppp..", "..ppppp..ppppp..", "..ppppp..ppppp..", "...ppp....ppp...",
               "................", "................", "................", "................", "................"]


def icons(ic):
    return {
        "katapult_helmpje": ic.shaped("helmet", (70, 80, 110), (120, 140, 170), (246, 110, 170)),
        "katapult_riem": ic.icon(RIEM_ICON, {"a": (80, 50, 30), "c": (120, 76, 44), "g": (246, 200, 70), "y": (255, 240, 150),
                                             "b": (150, 100, 60), "p": FLUFF, "w": (255, 240, 250)}),
        "katapult_oorbelletjes": ic.icon(OORBEL_ICON, {"g": (246, 200, 70), "k": (200, 170, 90), "p": (250, 150, 200), "w": (255, 235, 245)}),
    }


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
def _noise(base, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            img.putpixel((x, y), tuple(max(0, min(255, c + rng.randint(-var, var))) for c in base) + (255,))
    return img


def textures(h):
    rng = random.Random(29301)
    # the Mika's crooked planks: purple-brown boards that don't quite line up
    plank = Image.new("RGBA", (16, 16))
    p = plank.load()
    offsets = [0, 5, 2, 7]
    for y in range(16):
        row = y // 4
        for x in range(16):
            c = MIKAPURPLE
            if y % 4 == 3 or (x + offsets[row]) % 8 == 0:
                c = tuple(max(0, v - 34) for v in MIKAPURPLE)
            p[x, y] = tuple(max(0, min(255, v + rng.randint(-7, 7))) for v in c) + (255,)
    for (x, y) in ((3, 1), (11, 5), (6, 9), (13, 13)):              # nails
        p[x, y] = (190, 190, 200, 255)
    h.save(plank, "block", "katapult_mikaplank.png")
    # the bucket (spruce wood with iron bands) and the fluff of the pluisbal
    bucket = _noise(WOOD, 8, 2931)
    b = bucket.load()
    for x in range(16):
        for y in (2, 12):
            b[x, y] = (90, 90, 100, 255)
    h.save(bucket, "block", "katapult_werper_hout.png")
    fluff = _noise(FLUFF, 14, 2932)
    f = fluff.load()
    for (x, y) in ((4, 6), (11, 6)):
        f[x, y] = (40, 28, 50, 255)
        f[x, y + 1] = (40, 28, 50, 255)
    f[7, 9] = (232, 112, 164, 255)
    f[8, 9] = (232, 112, 164, 255)
    h.save(fluff, "block", "katapult_pluis.png")
    # the fortplek: dark stone with a pink target ring
    plek = _noise((46, 40, 54), 6, 2933)
    q = plek.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.5 < d < 7 or d < 2:
                q[x, y] = (236, 110, 170, 255)
    h.save(plek, "block", "katapult_fortplek.png")
    h.save(_noise((46, 40, 54), 6, 2934), "block", "katapult_fortplek_zij.png")
    # the knabbelkist: a crate with dark bands and a heart
    kist = _noise((176, 128, 74), 8, 2935)
    k = kist.load()
    for i in range(16):
        k[i, 0] = k[i, 15] = k[0, i] = k[15, i] = (100, 66, 36, 255)
        k[i, i] = (130, 90, 50, 255)
    for (x, y) in ((6, 6), (7, 6), (9, 6), (10, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8),
                   (7, 9), (8, 9), (9, 9), (8, 10)):
        k[x, y] = (238, 110, 160, 255)
    h.save(kist, "block", "katapult_knabbelkist.png")
    # the pluisbal entity (64x32: an 8x8x8 core, 3 tufts, 2 ears)
    img = Image.new("RGBA", (64, 32))
    e = img.load()
    for x in range(64):
        for y in range(32):
            e[x, y] = tuple(max(0, min(255, c + rng.randint(-16, 16))) for c in FLUFF) + (255,)
    for (x, y) in ((10, 11), (13, 11)):                            # a sleepy little guh face on the core's front (north face)
        e[x, y] = (40, 28, 50, 255)
    for (x, y) in ((9, 13), (14, 13)):
        e[x, y] = (238, 120, 170, 255)
    e[11, 13] = (150, 72, 116, 255)
    e[12, 13] = (150, 72, 116, 255)
    for x in range(32, 40):                                        # the ears: a darker pink
        for y in range(0, 4):
            e[x, y] = (232, 120, 168, 255)
    h.save(img, "entity", "pluisbal.png")
    # items: the katapultster (a golden star with a pink guh face) and the loaned pluisballen
    h.save(h.grid(["................", ".......yy.......", ".......yy.......", "......yyyy......", "yyyyyyyyyyyyyyyy", ".yyyyyyyyyyyyyy.",
                   "..yyykyyyykyyy..", "...yyyyyyyyyy...", "...yypyyyypyy...", "....yyymmyyy....", "....yyyyyyyy....",
                   "...yyyyyyyyyy...", "...yyyy..yyyy...", "..yyy......yyy..", "..yy........yy..", "................"],
                  {"y": (250, 208, 70, 255), "k": (40, 28, 50, 255), "p": (238, 120, 170, 255), "m": (150, 72, 116, 255)}),
           "item", "katapultster.png")
    h.save(h.grid(["................", "................", ".....pp..pp.....", "....pppppppp....", "...pwppppppppp..", "...pppppppppppp.",
                   "...pppppppppppp.", "....pppppppppp..", "...pp.pppppp....", "..pwpp.pppp.....", "..pppppp.pp.pp..",
                   "..pppppp..pwppp.", "...pppp...ppppp.", "...........ppp..", "................", "................"],
                  {"p": FLUFF + (255,), "w": (255, 240, 250, 255)}), "item", "katapult_pluisballen.png")


# ---------------------------------------------------------------------------------------------------------------------
# block models
# ---------------------------------------------------------------------------------------------------------------------
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def _facing(h, name, model):
    h.w(f"{h.A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": model, **({"y": r} if r else {})} for f, r in ROT.items()}})


def _el(frm, to, tex, faces=("down", "up", "north", "south", "west", "east"), uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for f in faces}}


def models(h):
    A = h.A
    h.simple_block("katapult_mikaplank")
    h.self_drop("katapult_mikaplank")
    h.shapeless("katapult_mikaplank", ["minecraft:dark_oak_planks", "minecraft:purple_dye"], "guhs:katapult_mikaplank", 1)
    # the bucket with a pluisbal in it
    h.w(f"{A}/models/block/katapult_werper.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/katapult_werper_hout", "hout": "guhs:block/katapult_werper_hout", "pluis": "guhs:block/katapult_pluis"},
        "elements": [_el([1, 0, 1], [15, 2, 15], "#hout"), _el([1, 2, 1], [15, 9, 3], "#hout"), _el([1, 2, 13], [15, 9, 15], "#hout"),
                     _el([1, 2, 3], [3, 9, 13], "#hout"), _el([13, 2, 3], [15, 9, 13], "#hout"),
                     _el([4, 2, 4], [12, 12, 12], "#pluis"), _el([-1, 6, 7], [17, 8, 9], "#hout", uv=[0, 0, 16, 2])]})
    _facing(h, "katapult_werper", "guhs:block/katapult_werper")
    h.w(f"{A}/models/item/katapult_werper.json", {"parent": "guhs:block/katapult_werper"})
    # the fortplek
    h.w(f"{A}/models/block/katapult_fortplek.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/katapult_fortplek", "side": "guhs:block/katapult_fortplek_zij", "bottom": "guhs:block/katapult_fortplek_zij"}})
    _facing(h, "katapult_fortplek", "guhs:block/katapult_fortplek")
    h.w(f"{A}/models/item/katapult_fortplek.json", {"parent": "guhs:block/katapult_fortplek"})
    # a Mika figure: a pink body, a Mika head (the mephal's Mika face), little ears, holding a stolen kaasknabbel
    h.w(f"{A}/models/block/katapult_mika.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/mika_mep_mika_side", "face": "guhs:block/mika_mep_mika_face", "side": "guhs:block/mika_mep_mika_side",
        "top": "guhs:block/mika_mep_mika_top", "ear": "guhs:block/mika_mep_mika_ear", "knabbel": "guhs:block/block_of_kaasknabbels"},
        "elements": [_el([4, 0, 5], [12, 6, 12], "#side"),
                     {"from": [3, 6, 3], "to": [13, 15, 13], "faces": {"north": {"texture": "#face"}, "south": {"texture": "#side"},
                                                                     "east": {"texture": "#side"}, "west": {"texture": "#side"},
                                                                     "up": {"texture": "#top"}, "down": {"texture": "#side"}}},
                     _el([3.5, 15, 6.5], [5.5, 17, 8.5], "#ear"), _el([10.5, 15, 6.5], [12.5, 17, 8.5], "#ear"),
                     _el([5.5, 3, 2], [10.5, 7, 5], "#knabbel")]})
    _facing(h, "katapult_mika", "guhs:block/katapult_mika")
    h.w(f"{A}/models/item/katapult_mika.json", {"parent": "guhs:block/katapult_mika"})
    # a crate of kaasknabbels, piled up to the top
    h.w(f"{A}/models/block/katapult_knabbelkist.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/katapult_knabbelkist", "kist": "guhs:block/katapult_knabbelkist", "knabbel": "guhs:block/block_of_kaasknabbels"},
        "elements": [_el([1, 0, 1], [15, 11, 15], "#kist"), _el([3, 11, 3], [13, 13, 13], "#knabbel"), _el([5, 13, 5], [11, 14, 10], "#knabbel")]})
    _facing(h, "katapult_knabbelkist", "guhs:block/katapult_knabbelkist")
    h.w(f"{A}/models/item/katapult_knabbelkist.json", {"parent": "guhs:block/katapult_knabbelkist"})
    h.item_model("katapultster")
    h.item_model("katapult_pluisballen")
    h.add_tag("guhs/tags/item/loaned", ["guhs:katapult_pluisballen"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:katapult_mikaplank"])


# ---------------------------------------------------------------------------------------------------------------------
# Kapitein Floepguh: a sunny peach guh with a navy captain's bicorne (gold trim, a pink feather that wiggles), a heart
# eye patch and a striped scarf
# ---------------------------------------------------------------------------------------------------------------------
def npc(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_katapultguh")
    sw = hulp.swatches(geo, ["hoed", "goud", "veer", "lapje", "sjaal"])
    c = hulp.cube
    geo["bones"].append({"name": "kapitein_hoed", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-7.4, 25.4, -3.2], [14.8, 2.4, 5.4], sw["hoed"]), c([-5.2, 27.8, -2.6], [10.4, 2.0, 4.2], sw["hoed"]),
        c([-3.0, 29.8, -2.0], [6.0, 1.0, 3.0], sw["hoed"]), c([-7.5, 25.2, -3.4], [15.0, 0.6, 5.8], sw["goud"]),
        c([-1.0, 27.0, -3.5], [2.0, 2.0, 0.6], sw["goud"])]})
    geo["bones"].append({"name": "kapitein_veer", "parent": "kapitein_hoed", "pivot": [4.5, 28.5, -0.5], "cubes": [
        c([4.2, 28.2, -1.0], [1.0, 5.0, 1.0], sw["veer"]), c([4.0, 32.6, -2.2], [1.4, 1.6, 2.6], sw["veer"]),
        c([4.1, 30.4, -1.6], [1.2, 1.8, 1.8], sw["veer"])]})
    geo["bones"].append({"name": "kapitein_lapje", "parent": "head", "pivot": [0, 20, -7], "cubes": [
        c([1.2, 18.0, -7.7], [4.4, 4.0, 0.5], sw["lapje"]), c([-8.3, 21.5, -7.4], [16.6, 0.6, 0.5], sw["hoed"])]})
    geo["bones"].append({"name": "kapitein_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.4, 11.2, -4.6], [10.8, 2.2, 8.6], sw["sjaal"], inflate=0.1), c([2.6, 6.6, -5.0], [2.4, 4.8, 0.8], sw["sjaal"])]})
    hulp.save_geo(h, "guh_npc_katapultguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.03, sat=0.62, val=1.05)
    rng = np.random.default_rng(29302)
    hulp.paint_swatch(a, sw["hoed"], (40, 50, 96), rng, 6)
    hulp.paint_swatch(a, sw["goud"], (246, 200, 70), rng, 10)
    hulp.paint_swatch(a, sw["veer"], (246, 110, 170), rng, 14)

    def hartje(block):
        block[..., :3] = (40, 30, 50)
        for y in range(32):
            for x in range(32):
                dx, dy = (x - 15.5) / 9, (y - 13) / 9
                if (dx * dx + dy * dy - 0.35) ** 3 - dx * dx * (-dy) ** 3 <= 0:
                    block[y, x, :3] = (238, 90, 150)
    hulp.paint_swatch(a, sw["lapje"], (40, 30, 50), rng, 2, hartje)

    def streep(block):
        for x in range(32):
            if (x // 5) % 2:
                block[:, x, :3] = (250, 250, 250)
    hulp.paint_swatch(a, sw["sjaal"], (70, 150, 220), rng, 6, streep)
    h.save(Image.fromarray(a), "entity", "npc_katapultguh.png")


# ---------------------------------------------------------------------------------------------------------------------
# advancements and texts
# ---------------------------------------------------------------------------------------------------------------------
ADVANCEMENTS = [
    ("katapult_gevonden", "root", "guhs:katapult_werper", "task",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:knabbelkatapult"}}}}},
     "Floep!", "Vind de Knabbelkatapult op de Vadskliffen: een guhkasteel tegenover een scheef Mika-fort"),
    ("katapult_gespeeld", "katapult_gevonden", "guhs:katapultster", "task", {"done": {"trigger": "minecraft:impossible"}},
     "Twaalf forten", "Schiet alle twaalf Mika-forten van één ronde plat bij Kapitein Floepguh"),
    ("katapult_drie_sterren", "katapult_gespeeld", "guhs:katapult_knabbelkist", "goal", {"done": {"trigger": "minecraft:impossible"}},
     "Drie sterren!", "Haal drie sterren bij een fort: alle Mika's weg, alle kisten open en nog een pluisbal over"),
    ("katapult_lastig", "katapult_drie_sterren", "guhs:katapult_pluisballen", "challenge", {"done": {"trigger": "minecraft:impossible"}},
     "Tegen de wind in", "Speel een hele ronde op lastig: drie pluisballen per fort, en het waait!"),
    ("katapult_alle_sterren", "katapult_lastig", "guhs:katapult_mika", "challenge", {"done": {"trigger": "minecraft:impossible"}},
     "Zesendertig sterren", "Haal in één ronde alle 36 sterren. De Mika's zijn helemaal van slag!"),
    ("katapult_kleding", "katapult_gespeeld", "guhs:katapult_helmpje", "goal",
     {"items": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
         {"items": "guhs:katapult_helmpje"}, {"items": "guhs:katapult_riem"}, {"items": "guhs:katapult_oorbelletjes"}]}},
      "code": {"trigger": "minecraft:impossible"}},
     "Klaar om te floepen", "Verzamel het hele katapultpakje: het katapulthelmpje, de katapultriem en de pluisbal-oorbelletjes"),
]


def advancements(h):
    for name, parent, icon, frame, crit, title, desc in ADVANCEMENTS:
        adv = {"parent": f"guhs:grote_guhspelen/{parent}",
               "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                           "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
               "criteria": crit}
        if len(crit) > 1:
            adv["requirements"] = [list(crit)]
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", adv)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)
    hulp.quest_advancements(h, ["katapult_gespeeld", "katapult_drie_sterren", "katapult_lastig", "katapult_alle_sterren",
                                "katapult_kleding", "seen_katapultguh"])


TEXTS = {
    "block.guhs.katapult_werper": "Katapultbakje",
    "block.guhs.katapult_fortplek": "Mika-fortplek",
    "block.guhs.katapult_mika": "Mika (in een fort)",
    "block.guhs.katapult_knabbelkist": "Kist met gestolen kaasknabbels",
    "block.guhs.katapult_mikaplank": "Scheve Mikaplank",
    "item.guhs.katapultster": "Katapultster",
    "item.guhs.katapult_pluisballen": "Pluisballen (geleend)",
    "item.guhs.katapult_pluisballen.lore": "Sta bij de katapult, kijk waar hij heen moet, houd rechtsklik ingedrukt om het elastiek aan te trekken en laat los: FLOEP!",
    "item.guhs.katapult_pluisballen.loan": "Van Kapitein Floepguh geleend: zacht als een guh, en na je ronde gaan ze terug.",
    "item.guhs.katapult_helmpje": "Katapulthelmpje",
    "item.guhs.katapult_riem": "Katapultriem",
    "item.guhs.katapult_oorbelletjes": "Pluisbal-oorbelletjes",
    "entity.guhs.pluisbal": "Pluisbal",
    "entity.guhs.katapult_brokje": "Fortbrokje",
    "entity.guhs.guh_npc.katapultguh": "Kapitein Floepguh",
    "gui.guhs.guhdex.rarity.katapultguh": "Zeldzaamheid: uniek (op de muur van de Knabbelkatapult, Vadskliffen)",
    "gui.guhs.guhdex.info.katapultguh": "Kapitein Floepguh bewaakt de Knabbelkatapult. Aan de overkant van de kloof hebben de Mika's hun forten gebouwd, vol gestolen kaasknabbels. Hij schiet er nooit iets hards op: alleen pluisballen, zo zacht als een guh. \"FLOEP! En daar gaat het fort. Niemand heeft pijn, de Mika's rennen giechelend weg en de knabbels zijn weer vrij. VAHOEG!\" Zijn ooglapje is eigenlijk een hartje. Njeg.",
    "structure.guhs.knabbelkatapult": "De Knabbelkatapult",
    "structure.guhs.knabbelkatapult.tooltip": "Minigame: pluisballen op twaalf Mika-forten met Kapitein Floepguh, katapultsterren en een katapultpakje (Vadskliffen)",
    # the Kapitein talks
    "quest.guhs.katapult.hello1": "Ahoi, guhtje! Zie je dat scheve fort daar aan de overkant? Vol gestolen kaasknabbels! Tijd voor een pluisbal. FLOEP!",
    "quest.guhs.katapult.hello2": "Welkom op de Knabbelkatapult! Twaalf Mika-forten, en jij krijgt de pluisballen. Die doen niemand pijn, hoor: het zijn net guhs.",
    "quest.guhs.katapult.hello3": "Njeg! De Mika's hebben wéér een fort gebouwd. Ze giechelen de hele dag naar me. Laten we ze eens wegfloepen!",
    "quest.guhs.katapult.hello4": "Mijn ooglapje? Dat is een hartje, voor de mooiheid. Ik kan prima mikken hoor! Zin in een rondje?",
    "quest.guhs.katapult.playing": "Je bent aan het floepen! Ga maar achter de katapult staan. Wil je stoppen? Dat kan hier ook.",
    "quest.guhs.katapult.busy": "Wacht even, %s is aan het floepen. Kijk lekker mee vanaf de muur!",
    "quest.guhs.katapult.elsewhere": "Je bent al bij een andere katapult bezig!",
    "quest.guhs.katapult.broken": "Njeg... ik kan mijn katapult of het fortveldje niet vinden. Is er iets kapot?",
    "quest.guhs.katapult.full": "Je zakken zitten vol! Maak één plekje vrij voor de pluisballen.",
    "quest.guhs.katapult.start": "Op je plaats achter de katapult! Niveau: %s. Het eerste fort wordt gebouwd... 3... 2... 1...",
    "quest.guhs.katapult.how.makkelijk": "Kijk waar de pluisbal heen moet en houd rechtsklik ingedrukt: hoe langer, hoe harder (tot 100%%). Laat los: FLOEP! Makkelijk: 5 pluisballen per fort, en de stippellijn laat zien waar hij heen vliegt.",
    "quest.guhs.katapult.how.medium": "Kijk waar de pluisbal heen moet en houd rechtsklik ingedrukt: hoe langer, hoe harder (tot 100%%). Laat los: FLOEP! Medium: 4 pluisballen per fort, zonder stippellijn.",
    "quest.guhs.katapult.how.lastig": "Kijk waar de pluisbal heen moet en houd rechtsklik ingedrukt: hoe langer, hoe harder (tot 100%%). Laat los: FLOEP! Lastig: 3 pluisballen per fort, en het waait: let op de pijltjes!",
    "quest.guhs.katapult.mika1": "(van de overkant) Hihihi! Deze knabbels zijn van ons!",
    "quest.guhs.katapult.mika2": "(van de overkant) Njeh njeh, jij raakt ons toch niet!",
    "quest.guhs.katapult.mika3": "(van de overkant) Hihi, ons fort is het allersterkste fort van de hele Guhmensie!",
    "quest.guhs.katapult.mika4": "(van de overkant) Pluisballen? Hahaha, die kietelen alleen maar!",
    "quest.guhs.katapult.mika5": "(van de overkant) Kom maar op, guhtje! Hihihihi!",
    "quest.guhs.katapult.stopped": "Je stopt met floepen. De pluisballen gaan terug in de mand. Tot de volgende keer, matroos!",
    "quest.guhs.katapult.walked_away": "Je liep weg van de katapult. Kapitein Floepguh pakt zijn pluisballen weer in.",
    "quest.guhs.katapult.idle": "Njeg, ben je in slaap gevallen? De Kapitein ruimt de pluisballen op.",
    "quest.guhs.katapult.fort_klaar": "Fort %s (%s): %s  %s punten | Mika's %s/%s | kisten %s/%s | +%s voor je pluisballen",
    "quest.guhs.katapult.run_klaar": "=== Alle twaalf forten! (%s) ===",
    "quest.guhs.katapult.run_sterren": "Sterren per fort: %s  (%s van de %s)",
    "quest.guhs.katapult.run_punten": "Totaal: %s punten",
    "quest.guhs.katapult.record": "Nieuw record op dit niveau: %s punten! (was %s)",
    "quest.guhs.katapult.first_record": "Je eerste ronde op dit niveau: %s punten. Dat is meteen je record!",
    "quest.guhs.katapult.no_record": "Je record op dit niveau blijft %s punten. Nog een rondje?",
    "quest.guhs.katapult.munten": "+%s katapultsterren (1 voor je ronde, 1 per 9 sterren, 1 voor een nieuw record; lastig +50%%)",
    "quest.guhs.katapult.knabbels": "Je hebt %s kaasknabbels bevrijd: ze zijn voor jou! Lekker vahoeg worden.",
    "quest.guhs.katapult.first": "Je allereerste ronde! Een cadeautje van de Kapitein: gefrituurde kaasknabbels. VAHOEG!",
    "quest.guhs.katapult.kasteel_record": "KASTEELRECORD! Jouw naam zweeft nu bovenaan boven de katapult én boven het Mika-fort. Floep floep hoera!",
    "quest.guhs.katapult.einde_top": "Wat een schutter! De Mika's rennen nog steeds giechelend weg. Jij bent de vahoegste katapultguh ooit!",
    "quest.guhs.katapult.einde_goed": "Goed geschoten, matroos! Nog een paar sterren en je bent kapitein.",
    "quest.guhs.katapult.einde_oefenen": "Njeg, die Mika's bouwen ook zo stevig! Tip: raak de paaltjes onderaan, dan valt alles vanzelf om.",
    # on screen
    "gui.guhs.katapult.countdown_sub": "Niveau: %s",
    "gui.guhs.katapult.fort_title": "Fort %s: %s",
    "gui.guhs.katapult.fort_sub": "%s Mika's, %s kisten met knabbels",
    "gui.guhs.katapult.fort_sub_wind": "%s Mika's, %s kisten | %s",
    "gui.guhs.katapult.fort_gevallen": "Alle Mika's weg! %s punten",
    "gui.guhs.katapult.fort_staat": "Het fort staat nog... %s punten",
    "gui.guhs.katapult.run_title": "%s sterren!",
    "gui.guhs.katapult.run_sub": "%s punten, +%s katapultsterren",
    "gui.guhs.katapult.bar": "Fort %s/%s | %s | Mika's %s/%s | kisten %s/%s | %s punten %s",
    "gui.guhs.katapult.wind": "| wind %s %s",
    "gui.guhs.katapult.wind_stil": "| geen wind",
    "gui.guhs.katapult.mika_weg": "Hihi! Daar rent een Mika weg! (%s/%s)",
    "gui.guhs.katapult.mika_laatste": "De laatste Mika rent giechelend weg! (%s/%s)",
    "gui.guhs.katapult.mis": "Hihihi, mis! (giechelen de Mika's)",
    "gui.guhs.katapult.wait_ball": "Wacht tot de pluisbal geland is!",
    "gui.guhs.katapult.wait_fort": "Wacht even, het fort wordt gebouwd!",
    "gui.guhs.katapult.too_far": "Ga achter de katapult staan om te schieten",
    "gui.guhs.katapult.look": "Kijk naar het Mika-fort aan de overkant!",
    "gui.guhs.katapult.vahoeg": "VAHOEG! Volle kracht!",
    "gui.guhs.katapult.power": "Elastiek: %s%%",
    "gui.guhs.scorebord.katapult": "Knabbelkatapult top 3",
    "gui.guhs.katapult.scorebord_niveau": "%s (%s pluisballen per fort)",
    "gui.guhs.katapult.no_build": "Njeg! De Knabbelkatapult is van de Kapitein: hier mag je niks slopen of bouwen. Floepen mag wel!",
    "gui.guhs.katapult.question": "Twaalf Mika-forten vol gestolen kaasknabbels. Kies een niveau en floep ze plat met pluisballen!",
    "gui.guhs.katapult.mine": "Je bent aan het floepen (fort %s van de 12). Stoppen mag altijd.",
    "gui.guhs.katapult.busy": "%s is aan het floepen (fort %s). Kijk mee of wacht even!",
    "gui.guhs.katapult.play.makkelijk": "5 pluisballen per fort, met een stippellijn die laat zien waar hij heen vliegt",
    "gui.guhs.katapult.play.medium": "4 pluisballen per fort, zonder stippellijn",
    "gui.guhs.katapult.play.lastig": "3 pluisballen per fort, en het waait! (+50% katapultsterren)",
    "gui.guhs.katapult.play": "Floepen maar!",
    "gui.guhs.katapult.play.tooltip": "Twaalf Mika-forten op het gekozen niveau. Kies hierboven makkelijk, medium of lastig. VAHOEG!",
    "gui.guhs.katapult.stop": "Stoppen",
    "gui.guhs.katapult.stop.tooltip": "Je ronde stopt; je houdt wat je al hebt",
    "gui.guhs.katapult.shop": "Winkeltje",
    "gui.guhs.katapult.shop.tooltip": "Het katapultpakje voor je guh, voor katapultsterren",
    "gui.guhs.katapult.rules": "Blokje 10, Mika 500, kist 300, elke pluisbal over 1000. Sterren: alle Mika's weg, alle kisten open, een pluisbal over.",
    "gui.guhs.katapult.records": "Jouw records (%s rondes):",
    "gui.guhs.katapult.best": "%s: %s  |  %s",
    "gui.guhs.katapult.kasteel": "kasteelrecord %s (%s)",
    "gui.guhs.katapult.kasteel_none": "nog geen kasteelrecord",
}


def texts(h):
    for key, text in TEXTS.items():
        h.lang(key, text, text)
    for i, name in enumerate(forten.NAMES, 1):
        h.lang(f"gui.guhs.katapult.fort.{i}", name, name)


# ---------------------------------------------------------------------------------------------------------------------
def build(h):
    textures(h)
    models(h)
    npc(h)
    advancements(h)
    texts(h)
    forten.build(h)
    s, info = bouw.build(h)
    n = bouw.check(s)
    h.TEMPLATE_SIZES[NAME] = 64
    h.FLATNESS[NAME] = 56                      # (the Vadskliffen are steep: the castle brings its own cliffs, 12 blocks deep)
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["vads_cliffs"], spacing=32, separation=11, salt=20290301, start_y=-bouw.G, reach=80, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    s.save(NAME)
    selfcheck(h)
    print(f"katapult: 12 forts ok, knabbelkatapult geometry check ok ({n} walkable spots, {info['gezichten']} faces)")


def selfcheck(h):
    import os
    missing = [f for f in ("blockstates/katapult_mika.json", "models/block/katapult_werper.json", "geo/entity/guh_npc_katapultguh.geo.json",
                           "textures/entity/pluisbal.png", "textures/entity/npc_katapultguh.png", "textures/item/katapultster.png")
               if not os.path.exists(f"{h.A}/{f}")]
    for i in range(1, 13):
        if not os.path.exists(f"{h.D}/structure/katapult/fort_{i:02d}.nbt"):
            missing.append(f"fort_{i:02d}")
        if f"gui.guhs.katapult.fort.{i}" not in h.NL:
            missing.append(f"name of fort {i}")
    if missing:
        raise SystemExit(f"katapult assets missing: {missing}")


# ---------------------------------------------------------------------------------------------------------------------
def ftb(fq):
    q = fq.q
    q("katapult_kasteel", "De Knabbelkatapult", "Op de &dVadskliffen&r staat soms een vrolijk guhkasteel met een katapult op de muur, "
      "tegenover een scheef Mika-fort (superkompas: Minigames). Op de muur staat &6Kapitein Floepguh&r.", "guhs:katapult_werper",
      [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=0, shape="circle", xp=100)
    q("katapult_ronde", "Twaalf forten", "Kies een niveau bij de Kapitein en schiet twaalf Mika-forten plat. Kijk, houd rechtsklik ingedrukt "
      "en laat los: FLOEP! Mika's rennen giechelend weg (ze doen niemand pijn) en de gestolen kaasknabbels zijn weer vrij.",
      "guhs:katapultster", [fq.adv("katapult_gespeeld")], rewards=(("guhs:katapultster", 2),), x=-6.5, y=0, xp=150)
    q("katapult_sterren", "Drie sterren!", "Haal drie sterren bij één fort: alle Mika's weg, alle knabbelkisten open, en nog een pluisbal "
      "over. Tip: raak de dunne paaltjes onderaan!", "guhs:katapult_knabbelkist", [fq.adv("katapult_drie_sterren")],
      rewards=(("guhs:katapultster", 2),), x=-5, y=0, xp=200)
    q("katapult_lastig", "Tegen de wind in", "Speel een hele ronde op &clastig&r: drie pluisballen per fort, en het waait. Let op de "
      "windpijltjes onderin je scherm!", "guhs:katapult_pluisballen", [fq.adv("katapult_lastig")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-3.5, y=0, shape="hexagon", xp=400)
    q("katapult_alle_sterren", "Zesendertig sterren", "Haal in één ronde alle 36 sterren. Dan is Kapitein Floepguh helemaal sprakeloos.",
      "guhs:katapult_mika", [fq.adv("katapult_alle_sterren")], rewards=(("guhs:vahoege_vads_ingot", 1),), x=-2, y=0, shape="hexagon", xp=500)
    q("katapult_pakje", "Klaar om te floepen", "Verzamel het hele katapultpakje bij de Kapitein: het katapulthelmpje, de katapultriem en de "
      "pluisbal-oorbelletjes.", "guhs:katapult_helmpje", [fq.adv("katapult_kleding")], rewards=(("guhs:katapultster", 3),),
      x=-0.5, y=0, shape="gear", xp=300)
    q("katapult_kapitein", "Ahoi, Kapitein!", "Zet Kapitein Floepguh in je Guhdex (kom dichtbij genoeg).", "guhs:guhdex",
      [fq.adv("seen_katapultguh")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=0, shape="rsquare", xp=100)
