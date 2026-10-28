"""
1.8.0 Guhdromenland, the finishing touches ("droomspul"). Java: feature/droomspul (DroomspulFeature, DroomBewoners).

  build(h)    the three other boxes a guh is tucked into (verhuisdoos, schoenendoos, pizzadoos; their tag membership is in
              dromen.py), the tuck-in items zeepje / sterlampje / fotootje (+ a little chest loot), the recipes of the
              droombrievenbusjes and the droomwekker (droomplein.py makes the blocks), the islanders' lists
              data/guhs/dromen/bewoners/<eiland>.json (read from the island templates: a sleeping guh in every open nestje,
              a Blub in every holletje of the Slaapliedjesheuvels, the Postguh in the Droomposthuisje) and the texts (Dutch in
              nl_nl, English in tools/lang/en/c214_droomspul.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/droomspul.py
"""
import gzip
import io
import json
import os
import random
import struct
import sys

from PIL import Image, ImageDraw

from features import dromen   # (its texture and model helpers: karton, _el, _facing)

DOZEN = ["verhuisdoos", "schoenendoos", "pizzadoos"]          # (DroomspulFeature.DOZEN: add at the end, never reorder)
ITEMS = ["zeepje", "sterlampje", "fotootje"]
BEWONERS_VERSIE = 1
EILANDEN = ["plein", "liedjes", "sterren", "herinnering", "badje", "molentje"]
# the Postguh behind the sorting table of the Droomposthuisje (r01_droomplein.py: S.guh(26.5, T + 1, 18.5, 180); C, T = 42, 30)
POSTGUH = {"soort": "postguh", "plek": [26.5 - 42, 1, 18.5 - 42], "yaw": 180}
# the islands where a covered nestje is a Blub-holletje (the open ones get a sleeping guh)
HOLLETJES = {"liedjes"}
# chest loot: item -> (chest table, chance)
LOOT = {"fotootje": ("guhs:chests/guh_picnic", 0.33), "sterlampje": ("guhs:chests/guh_sterrenwacht", 0.25),
        "zeepje": ("guhs:chests/onderwol/knuffelslaapzaal", 0.25)}

P = "gui.guhs.droomspul."
TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.verhuisdoos": ("Verhuisdoos", "Moving Box"),
    "block.guhs.verhuisdoos.lore": ("'Breekbaar: guh'. Rechtsklik 's avonds: stop je guh erin in.",
                                    "'Fragile: guh'. Right-click in the evening: tuck your guh in."),
    "block.guhs.schoenendoos": ("Schoenendoos", "Shoe Box"),
    "block.guhs.schoenendoos.lore": ("De schoenen zijn weg, de guh is erin. Rechtsklik 's avonds: stop je guh erin in.",
                                     "The shoes are gone, the guh is in. Right-click in the evening: tuck your guh in."),
    "block.guhs.pizzadoos": ("Pizzadoos", "Pizza Box"),
    "block.guhs.pizzadoos.lore": ("Nog warm en het ruikt naar kaas. Welke guh kan daar nou nee tegen zeggen?",
                                  "Still warm and it smells of cheese. Which guh could ever say no to that?"),
    "item.guhs.zeepje": ("Zeepje", "Little Soap"),
    "item.guhs.zeepje.lore": ("Geef het mee bij het instoppen: je droomt van het Wolkenbadje.",
                              "Give it along when you tuck your guh in: you dream of the Cloud Bath."),
    "item.guhs.sterlampje": ("Sterlampje", "Star Lamp"),
    "item.guhs.sterlampje.lore": ("Geef het mee bij het instoppen: je droomt van het Sterrenkussenveld.",
                                  "Give it along when you tuck your guh in: you dream of the Star Cushion Field."),
    "item.guhs.fotootje": ("Fotootje", "Little Photo"),
    "item.guhs.fotootje.lore": ("Een oud fotootje van een familiedag. Geef het mee bij het instoppen: je droomt van het Herinneringseiland.",
                                "An old little photo of a family day. Give it along when you tuck your guh in: you dream of Memory Island."),
    "entity.guhs.droompostguh": ("Droompostguh", "Dream Post Guh"),
    P + "postguh.0": ("Post voor jou? Njeg... alleen een droom. Die bezorg ik wel gratis.",
                      "Mail for you? Njeg... only a dream. I deliver those for free."),
    P + "postguh.1": ("Een busje per eiland. Maar alleen naar eilanden waar je al was, anders weet ik je adres niet!",
                      "One mailbox per island. But only to islands you've been to, otherwise I don't know your address!"),
    P + "postguh.2": ("Postzegels? Hier betaal je met gaapjes. Ik heb er al drie gehad vandaag.",
                      "Stamps? Here you pay with yawns. I've had three already today."),
    P + "postguh.3": ("Gisteren bezorgde ik een knuffel. Hij was een beetje platgedrukt, maar nog heel lief.",
                      "Yesterday I delivered a hug. It was a bit squashed, but still very sweet."),
}


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(base, var, seed, size=16):
    return dromen._ruis(base, var, seed, size)


def verhuis_zij():
    img = dromen.karton(21)
    d = ImageDraw.Draw(img)
    d.rectangle([7, 0, 8, 15], fill=(214, 196, 150, 255))               # the tape over the middle
    for (x, y) in ((3, 6), (3, 5), (2, 6), (4, 6), (3, 7), (3, 8), (3, 9)):   # an arrow: this side up
        d.point((x, y), fill=(60, 44, 34, 255))
    d.rectangle([10, 8, 13, 11], fill=(250, 238, 222, 255))             # a label with a little guh face
    d.point([(11, 9), (12, 9)], fill=(40, 30, 40, 255))
    return img


def schoen_zij():
    img = _ruis((246, 214, 226), 5, 22)
    d = ImageDraw.Draw(img)
    d.line([0, 4, 15, 4], fill=(226, 170, 196, 255))                   # the lid's edge
    d.rectangle([5, 7, 10, 10], fill=(255, 255, 255, 255))             # the brand: a guh ear logo
    d.point([(6, 8), (9, 8)], fill=(240, 140, 170, 255))
    return img


def schoen_binnen():
    img = _ruis((236, 226, 214), 4, 23)
    d = ImageDraw.Draw(img)
    for y in (4, 9, 13):                                               # tissue paper folds
        d.line([1, y, 14, y + 1], fill=(250, 246, 240, 255))
    return img


def pizza_binnen():
    img = _ruis((236, 196, 120), 6, 24)                                # the crust
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 14, 14], fill=(244, 206, 96, 255))                 # cheese
    for (x, y) in ((4, 4), (10, 5), (6, 10), (11, 11), (8, 7)):        # kaasknabbels as toppings
        d.rectangle([x, y, x + 1, y + 1], fill=(250, 170, 60, 255))
    d.point([(7, 4), (5, 7)], fill=(255, 236, 160, 255))
    return img


def pizza_deksel():
    img = _ruis((236, 230, 220), 4, 25)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 4, 12, 11], fill=(232, 74, 84, 255))               # a red label: "Guhzza"
    d.rectangle([5, 6, 10, 9], fill=(255, 236, 160, 255))
    d.point([(6, 7), (9, 7)], fill=(40, 30, 40, 255))
    return img


def zeepje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 5, 13, 12], radius=3, fill=(255, 182, 213, 255), outline=(226, 128, 172, 255))
    d.line([4, 7, 9, 7], fill=(255, 226, 238, 255))                    # shine
    for (x, y, r) in ((11, 2, 2), (6, 1, 1), (13, 5, 1)):              # bubbles
        d.ellipse([x - r, y - r, x + r, y + r], outline=(190, 230, 255, 255))
    return img


def sterlampje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([7, 0, 8, 2], fill=(120, 110, 140, 255))               # the little hook
    d.rounded_rectangle([4, 3, 11, 13], radius=2, fill=(70, 78, 140, 220), outline=(196, 170, 90, 255))
    ster = [(7, 5), (8, 5), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (6, 10), (9, 10), (7, 9), (8, 9), (7, 6), (8, 6)]
    for (x, y) in ster:
        d.point((x, y), fill=(255, 228, 110, 255))
    d.line([4, 14, 11, 14], fill=(196, 170, 90, 255))
    return img


def fotootje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 14], fill=(250, 246, 236, 255), outline=(200, 190, 170, 255))   # a polaroid
    d.rectangle([3, 3, 12, 10], fill=(196, 170, 136, 255))             # sepia
    d.rectangle([3, 8, 12, 10], fill=(150, 180, 120, 255))             # grass
    for (x, c) in ((5, (255, 182, 213)), (8, (220, 210, 200)), (10, (255, 200, 150))):   # three guhs at the party
        d.rectangle([x, 6, x + 1, 8], fill=c + (255,))
        d.point((x, 5), fill=c + (255,))
    return img


# =====================================================================================================================
# the boxes
# =====================================================================================================================
def dozen(h):
    A, w, _el = h.A, h.w, dromen._el
    h.save(verhuis_zij(), "block", "verhuisdoos_zij.png")
    h.save(dromen.karton(26), "block", "verhuisdoos_karton.png")
    h.save(schoen_zij(), "block", "schoenendoos_zij.png")
    h.save(schoen_binnen(), "block", "schoenendoos_binnen.png")
    h.save(pizza_binnen(), "block", "pizzadoos_binnen.png")
    h.save(pizza_deksel(), "block", "pizzadoos_deksel.png")
    h.save(_ruis((226, 218, 204), 4, 27), "block", "pizzadoos_karton.png")
    modellen = {
        # the verhuisdoos: like the doos met knabbels, but a bit higher and both flaps open
        "verhuisdoos": ({"karton": "guhs:block/verhuisdoos_karton", "zij": "guhs:block/verhuisdoos_zij",
                         "binnen": "guhs:block/doos_knabbels_binnen", "particle": "guhs:block/verhuisdoos_karton"},
                        [_el([1, 0, 1], [15, 1, 15], "#binnen"),
                         _el([1, 1, 1], [15, 10, 2], "#zij"), _el([1, 1, 14], [15, 10, 15], "#zij"),
                         _el([1, 1, 2], [2, 10, 14], "#karton"), _el([14, 1, 2], [15, 10, 14], "#karton"),
                         _el([1, 10, 0], [15, 10.5, 1], "#karton"), _el([1, 10, 15], [15, 10.5, 16], "#karton")]),
        # the schoenendoos: low, with its lid leaning against the back
        "schoenendoos": ({"zij": "guhs:block/schoenendoos_zij", "binnen": "guhs:block/schoenendoos_binnen",
                          "particle": "guhs:block/schoenendoos_zij"},
                         [_el([2, 0, 3], [14, 1, 13], "#binnen"),
                          _el([2, 1, 3], [14, 6, 4], "#zij"), _el([2, 1, 12], [14, 6, 13], "#zij"),
                          _el([2, 1, 4], [3, 6, 12], "#zij"), _el([13, 1, 4], [14, 6, 12], "#zij"),
                          _el([1, 0, 13], [15, 12, 14], "#zij")]),
        # the pizzadoos: flat, the lid standing open at the back, a cheesy guhzza inside
        "pizzadoos": ({"karton": "guhs:block/pizzadoos_karton", "binnen": "guhs:block/pizzadoos_binnen",
                       "deksel": "guhs:block/pizzadoos_deksel", "particle": "guhs:block/pizzadoos_karton"},
                      [_el([1, 0, 1], [15, 1, 15], "#binnen"),
                       _el([1, 1, 1], [15, 3, 2], "#karton"), _el([1, 1, 2], [2, 3, 15], "#karton"),
                       _el([14, 1, 2], [15, 3, 15], "#karton"),
                       _el([1, 1, 15], [15, 15, 16], "#deksel")]),
    }
    for doos, (tex, els) in modellen.items():
        w(f"{A}/models/block/{doos}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": els})
        w(f"{A}/blockstates/{doos}.json", dromen._facing(doos))
        w(f"{A}/models/item/{doos}.json", {"parent": f"guhs:block/{doos}"})
        h.self_drop(doos)
    h.shapeless("verhuisdoos", ["guhs:doos_knabbels", "minecraft:string"], "guhs:verhuisdoos")
    h.shapeless("schoenendoos", ["guhs:doos_knabbels", "minecraft:pink_dye"], "guhs:schoenendoos")
    h.shapeless("pizzadoos", ["guhs:doos_knabbels", "minecraft:bread"], "guhs:pizzadoos")


# =====================================================================================================================
# the tuck-in items, their chest loot, the recipes of the plein's blocks
# =====================================================================================================================
def spullen(h):
    D = h.D
    for naam, maak in (("zeepje", zeepje), ("sterlampje", sterlampje), ("fotootje", fotootje)):
        h.save(maak(), "item", f"{naam}.png")
        h.item_model(naam)
    h.shapeless("zeepje", ["minecraft:slime_ball", "minecraft:pink_dye", "minecraft:sugar"], "guhs:zeepje", 2)
    h.shapeless("sterlampje", ["minecraft:glowstone_dust", "minecraft:gold_nugget", "minecraft:glass_pane"], "guhs:sterlampje")
    h.shapeless("fotootje", ["minecraft:paper", "minecraft:ink_sac", "minecraft:pink_dye"], "guhs:fotootje")
    for naam, (tafel, kans) in LOOT.items():
        h.w(f"{D}/loot_table/droomspul/{naam}.json", {"type": "minecraft:chest", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:random_chance", "chance": kans}],
            "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}"}]}]})
        kort = tafel.split("/")[-1]
        h.w(f"{D}/loot_modifiers/droomspul_{naam}__{kort}.json", {
            "type": "neoforge:add_table", "table": f"guhs:droomspul/{naam}",
            "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": tafel}]})
    # the droombrievenbusjes (a wool per island, as on the plein's paths) and the droomwekker
    wol = {"plein": "minecraft:pink_wool", "liedjes": "minecraft:purple_wool", "sterren": "minecraft:yellow_wool",
           "badje": "minecraft:light_blue_wool", "herinnering": "minecraft:brown_wool", "molentje": "minecraft:light_gray_wool"}
    for eiland, w in wol.items():
        h.shaped(f"droombrievenbus_{eiland}", ["WWW", "WZW", " S "],
                 {"W": w, "Z": "guhs:droomzand", "S": "minecraft:stick"}, f"guhs:droombrievenbus_{eiland}")
    h.shapeless("droomwekker", ["minecraft:clock", "minecraft:pink_dye", "guhs:droomzand"], "guhs:droomwekker")


# =====================================================================================================================
# the islanders, read from the island templates
# =====================================================================================================================
def _nbt(pad):
    """A small NBT reader (gzip): enough for structure templates."""
    f = io.BytesIO(gzip.open(pad).read())

    def naam():
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode()

    def lees(t):
        if t == 1:
            return struct.unpack(">b", f.read(1))[0]
        if t == 2:
            return struct.unpack(">h", f.read(2))[0]
        if t == 3:
            return struct.unpack(">i", f.read(4))[0]
        if t == 4:
            return struct.unpack(">q", f.read(8))[0]
        if t == 5:
            return struct.unpack(">f", f.read(4))[0]
        if t == 6:
            return struct.unpack(">d", f.read(8))[0]
        if t == 7:
            n = struct.unpack(">i", f.read(4))[0]
            return f.read(n)
        if t == 8:
            return naam()
        if t == 9:
            et = f.read(1)[0]
            n = struct.unpack(">i", f.read(4))[0]
            return [lees(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = f.read(1)[0]
                if tt == 0:
                    return d
                k = naam()
                d[k] = lees(tt)
        if t == 11:
            n = struct.unpack(">i", f.read(4))[0]
            return list(struct.unpack(f">{n}i", f.read(4 * n)))
        if t == 12:
            n = struct.unpack(">i", f.read(4))[0]
            return list(struct.unpack(f">{n}q", f.read(8 * n)))
        raise ValueError(f"NBT tag {t}?")

    t = f.read(1)[0]
    naam()
    return lees(t)


def bewoners(h):
    D = h.D
    gemaakt = {}
    for eiland in EILANDEN:
        tpl = f"{D}/structure/dromen/eiland_{eiland}.nbt"
        js = f"{D}/dromen/eiland/{eiland}.json"
        if not (os.path.exists(tpl) and os.path.exists(js)):
            continue
        with open(js, encoding="utf-8") as f:
            mx, my, mz = json.load(f)["midden"]
        d = _nbt(tpl)
        pal = [p["Name"] for p in d["palette"]]
        blok = {tuple(b["pos"]): pal[b["state"]] for b in d["blocks"]}
        rng = random.Random(eiland)
        lijst = [dict(POSTGUH)] if eiland == "plein" else []
        for (x, y, z), naam in sorted(blok.items()):
            if naam != "guhs:guhnestje":
                continue
            boven = [blok.get((x, y + k, z), "minecraft:air") for k in (1, 2, 3)]
            gedekt = any(b != "minecraft:air" for b in boven)
            if eiland in HOLLETJES and gedekt:
                lijst.append({"soort": "blub", "plek": [x + 0.5 - mx, y - my + 0.3, z + 0.5 - mz], "yaw": rng.randrange(0, 360, 15)})
            elif not gedekt:
                lijst.append({"soort": "guh_slaapt", "plek": [x + 0.5 - mx, y - my + 0.25, z + 0.5 - mz],
                              "yaw": rng.randrange(0, 360, 15)})
        if lijst:
            h.w(f"{D}/dromen/bewoners/{eiland}.json", {"versie": BEWONERS_VERSIE, "bewoners": lijst})
            gemaakt[eiland] = len(lijst)
    return gemaakt


# =====================================================================================================================
def build(h):
    dozen(h)
    spullen(h)
    gemaakt = bewoners(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)
    return gemaakt


def selfcheck(h):
    A, D = h.A, h.D
    nodig = [f"{A}/models/item/{n}.json" for n in DOZEN + ITEMS] + [f"{A}/blockstates/{n}.json" for n in DOZEN]
    weg = [p for p in nodig if not os.path.exists(p)]
    weg += [k for k, (nl, en) in TEKSTEN.items() if nl.count("%") != en.count("%")]
    if weg:
        raise SystemExit(f"droomspul self-check failed: {weg}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    gemaakt = build(h)
    en_pad = os.path.join("tools", "lang", "en", "c214_droomspul.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in DOZEN + ITEMS:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")
    print(f"droomspul: {len(DOZEN)} boxes, {len(ITEMS)} tuck-in items, islanders {gemaakt}, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
