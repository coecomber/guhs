"""
Het Ballonfestival (2.8.0 "Knuffeldal", slice "buiten"; see guhs_work28/KNUFFEL_CONTRACT.md): a festival field full of
guh-shaped hot-air balloons on the Guhvelden and the Roze pluisjes, where Kapitein Wolkje takes you on a guided round
flight (no steering) past two of eight viewpoints; after landing: their stamps on your ballonstempelkaart.

  - the loose structure ballonfestival (ballon_bouw.py: 72 x 40 x 72, geometry self-check)
  - the entity guh_luchtballon (ballon_model.py: GeckoLib model, animation, four colours)
  - blocks ballonsteiger and mini_luchtballon, the coin ballonmunt
  - Kapitein Wolkje (BALLONGUH): his own model (pilot's cap with goggles, a flying scarf) and texture
  - the clothes ballonpet (outfit_ballonpet*, with goggles on its rim) and ballonbril (outfit_ballonbril*)
  - the Knus section "ballon" (ballonstempels: 8 viewpoints), advancements, FTB row y = 93.5, lang (all Dutch)
"""
import math
import random

import numpy as np
from PIL import Image

from features import ballon_bouw as bouw
from features import ballon_model as model
from features import sterrenwacht_hulp as hulp

NAME = bouw.NAME
CLOTHES = ["ballonpet", "ballonbril"]
FTB_Y = 93.5
LEER = (132, 84, 52)
MESSING = (214, 168, 70)

_H = [0, 6, -2]   # head pivot
BONES = {
    # the ballonpet: a leather pilot's cap over the head (the ears poke through), side flaps, flying goggles on its rim
    "outfit_ballonpet": ("head", _H, "ballonpet", [([-7.2, 13.2, -12.4], [14.4, 2.4, 12.8], 0), ([-6.4, 15.6, -11.4], [12.8, 1.0, 10.6], 0),
                                                   ([-7.7, 8.4, -8.8], [0.6, 5.2, 5.2], 0), ([7.1, 8.4, -8.8], [0.6, 5.2, 5.2], 0),
                                                   ([-7.3, 12.6, -12.8], [14.6, 1.0, 0.6], 0)]),
    "outfit_ballonpet_bril": ("head", _H, "ballonpet_bril", [([-5.6, 13.0, -13.3], [4.2, 2.8, 0.9], 0), ([1.4, 13.0, -13.3], [4.2, 2.8, 0.9], 0)]),
    # the ballonbril: big round flying goggles over the eyes, on a leather strap
    "outfit_ballonbril": ("head", _H, "ballonbril", [([0.6, 6.8, -13.1], [6.8, 6.2, 1.2], 0), ([-7.4, 6.8, -13.1], [6.8, 6.2, 1.2], 0)]),
    "outfit_ballonbril_band": ("head", _H, "ballonbril_band", [([7.25, 9.0, -12.5], [0.6, 1.5, 12.6], 0), ([-7.85, 9.0, -12.5], [0.6, 1.5, 12.6], 0),
                                                               ([-7.85, 9.0, 0.1], [15.7, 1.5, 0.6], 0), ([-1.2, 9.4, -13.0], [2.4, 1.0, 0.8], 0)]),
}


def clothes(rng, v):
    def leer():
        a = v.fabric(LEER, rng, 10)
        a[:, 15:17] = (104, 64, 40)                     # a seam over the top
        return np.clip(a, 0, 255)

    def bril(open_glas):
        def p():
            a = np.zeros((32, 32, 4), np.float64)
            for y in range(32):
                for x in range(32):
                    d = math.dist((x + 0.5, y + 0.5), (16, 16))
                    if d > 10:
                        a[y, x] = MESSING + (255,)
                    elif open_glas:
                        a[y, x] = (0, 0, 0, 0)          # see-through: the guh's own eyes show
                    else:
                        a[y, x] = (150, 214, 246, 255)
            a[8:11, 9:12] = (255, 255, 255, 255)          # a glint
            return a
        return p

    def band():
        return v.fabric((96, 60, 38), rng, 6)
    return {"ballonpet": {"ballonpet": leer, "ballonpet_bril": bril(False)},
            "ballonbril": {"ballonbril": bril(True), "ballonbril_band": band}}


def icons(ic):
    pet = ic.icon(ic.pad(["....aaaaaaa.....", "...abbbbbbba....", "..abbbbbbbbba...", "..accdccdcca....", ".abbbbbbbbbbba..", ".ab.........ba..",
                          ".aa.........aa.."]),
                  {"a": (80, 48, 28), "b": (132, 84, 52), "c": (214, 168, 70), "d": (150, 214, 246)})
    return {"ballonpet": pet, "ballonbril": ic.shaped("glasses", (170, 120, 40), (150, 214, 246))}


# =====================================================================================================================
# textures, blocks, items
# =====================================================================================================================
def textures(h):
    rng = random.Random(28703)
    save = h.save
    planks = h.ramp(h.vanilla("block/spruce_planks"), (110, 72, 44), (206, 150, 100))
    top = planks.copy()
    tp = top.load()
    for i in range(16):
        for j in (0, 15):
            tp[i, j] = (246, 150, 196, 255) if (i // 2) % 2 else (255, 244, 248, 255)
            tp[j, i] = (246, 150, 196, 255) if (i // 2) % 2 else (255, 244, 248, 255)
    for x in range(5, 11):
        for y in range(5, 11):
            if math.dist((x + 0.5, y + 0.5), (8, 8)) < 3.2:
                tp[x, y] = (255, 214, 232, 255)
    tp[6, 7] = tp[9, 7] = (34, 24, 52, 255)
    tp[7, 9] = tp[8, 9] = (232, 112, 164, 255)
    save(top, "block", "ballonsteiger_top.png")
    side = planks.copy()
    sp = side.load()
    for x in range(16):
        for y in (0, 1):
            sp[x, y] = (246, 150, 196, 255) if (x // 2) % 2 else (255, 244, 248, 255)
    save(side, "block", "ballonsteiger_zijkant.png")
    # the mini luchtballon: stripes, a face, wicker
    stof = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            stof.putpixel((x, y), (246, 150, 196, 255) if (x // 2) % 2 else (255, 244, 248, 255))
    save(stof, "block", "mini_luchtballon_stof.png")
    gezicht = hulp.noisy((255, 206, 226), 4, rng)
    hulp.paint_face(gezicht, (0, 0, 16, 16), 0)
    save(gezicht, "block", "mini_luchtballon_gezicht.png")
    mand = hulp.noisy((176, 124, 70), 10, rng)
    mp = mand.load()
    for x in range(16):
        for y in range(16):
            if (y // 2 + x // 3) % 2 == 0:
                c = mp[x, y]
                mp[x, y] = (max(0, c[0] - 30), max(0, c[1] - 30), max(0, c[2] - 30), 255)
    save(mand, "block", "mini_luchtballon_mand.png")
    oor = hulp.noisy((246, 150, 196), 5, rng)
    for x in range(5, 11):
        for y in range(5, 11):
            oor.putpixel((x, y), (214, 90, 170, 255))
    save(oor, "block", "mini_luchtballon_oor.png")
    # the ballonmunt: a golden coin with a little guh balloon on it
    munt = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mp = munt.load()
    for x in range(16):
        for y in range(16):
            d = math.dist((x + 0.5, y + 0.5), (8, 8))
            if d <= 7:
                mp[x, y] = (236, 186, 70, 255) if d > 6 else (250, 214, 100, 255)
    for x in range(5, 11):
        for y in range(3, 9):
            if math.dist((x + 0.5, y + 0.5), (8, 6)) <= 3:
                mp[x, y] = (246, 150, 196, 255)
    mp[6, 3] = mp[9, 3] = (246, 150, 196, 255)
    mp[7, 6] = mp[8, 6] = (60, 30, 50, 255)
    for (x, y) in ((6, 9), (9, 9), (6, 10), (9, 10)):
        mp[x, y] = (150, 100, 40, 255)
    for x in range(6, 10):
        mp[x, 11] = mp[x, 12] = (176, 124, 70, 255)
    save(munt, "item", "ballonmunt.png")
    # the cloud puff particle
    frames = []
    for i in range(3):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        p = img.load()
        prng = random.Random(28710 + i)
        blobs = [(8 + prng.uniform(-3, 3), 8 + prng.uniform(-2, 2), prng.uniform(3, 5)) for _ in range(4)]
        for x in range(16):
            for y in range(16):
                a = 0.0
                for (bx, by, r) in blobs:
                    a = max(a, 1 - math.dist((x + 0.5, y + 0.5), (bx, by)) / r)
                if a > 0:
                    shade = 235 + int(20 * a)
                    p[x, y] = (shade, shade, min(255, shade + 6), int(255 * min(1.0, a * 1.8)))
        frames.append(img)
    hulp.particle_frames(h, "ballonwolkje", frames)


def blocks_and_items(h):
    A, D, w = h.A, h.D, h.w
    el = hulp.el
    w(f"{A}/models/block/ballonsteiger.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/ballonsteiger_top", "bottom": "guhs:block/ballonsteiger_zijkant", "side": "guhs:block/ballonsteiger_zijkant"}})
    w(f"{A}/blockstates/ballonsteiger.json", {"variants": {"": {"model": "guhs:block/ballonsteiger"}}})
    w(f"{A}/models/item/ballonsteiger.json", {"parent": "guhs:block/ballonsteiger"})
    t = {"particle": "guhs:block/mini_luchtballon_stof", "stof": "guhs:block/mini_luchtballon_stof", "gezicht": "guhs:block/mini_luchtballon_gezicht",
         "mand": "guhs:block/mini_luchtballon_mand", "oor": "guhs:block/mini_luchtballon_oor", "touw": "minecraft:block/white_wool"}
    ballon = {"from": [3, 5, 3], "to": [13, 14, 13], "faces": {"north": {"texture": "#gezicht"}, "south": {"texture": "#gezicht"},
                                                               "east": {"texture": "#stof"}, "west": {"texture": "#stof"},
                                                               "up": {"texture": "#stof"}, "down": {"texture": "#stof"}}}
    elements = [el([6, 0, 6], [10, 3, 10], "#mand"), ballon,
                el([4, 13.5, 7], [6.5, 16, 9], "#oor"), el([9.5, 13.5, 7], [12, 16, 9], "#oor"),
                el([4, 4, 4], [12, 5, 12], "#stof")]
    for (x, z) in ((6, 6), (9.5, 6), (6, 9.5), (9.5, 9.5)):
        elements.append(el([x, 3, z], [x + 0.5, 4, z + 0.5], "#touw"))
    w(f"{A}/models/block/mini_luchtballon.json", {"parent": "minecraft:block/block", "textures": t, "elements": elements})
    w(f"{A}/blockstates/mini_luchtballon.json", {"variants": {"": {"model": "guhs:block/mini_luchtballon"}}})
    w(f"{A}/models/item/mini_luchtballon.json", {"parent": "guhs:block/mini_luchtballon"})
    h.item_model("ballonmunt")
    for b in ("ballonsteiger", "mini_luchtballon"):
        h.self_drop(b)
    h.shaped("mini_luchtballon", [" W ", "WPW", " S "], {"W": "#minecraft:wool", "P": "minecraft:paper", "S": "minecraft:string"},
             "guhs:mini_luchtballon", 1)
    h.shaped("ballonsteiger", ["WWW", "PPP"], {"W": "#minecraft:wool", "P": "#minecraft:planks"}, "guhs:ballonsteiger", 4)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:ballonsteiger"])
    w(f"{D}/loot_table/chests/{NAME}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:ballonmunt", "functions": h.count_fn(1, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:mini_luchtballon", "functions": h.count_fn(1, 2)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaas_knabbels", "weight": 5, "functions": h.count_fn(6, 14)},
            {"type": "minecraft:item", "name": "guhs:guh_ballon", "weight": 3, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:string", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:paper", "weight": 2, "functions": h.count_fn(2, 5)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 2, "functions": h.count_fn(1, 3)}]}]})


# =====================================================================================================================
# Kapitein Wolkje: sky blue and white, a pilot's cap with goggles, a flying scarf
# =====================================================================================================================
def npc(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_ballonguh")
    sw = hulp.swatches(geo, ["pet", "bril", "sjaal", "sjaalpunt"])
    c = hulp.cube
    geo["bones"].append({"name": "kapitein_pet", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.8, 24.2, -7.4], [13.6, 2.4, 13.0], sw["pet"]), c([-6.0, 26.6, -6.4], [12, 1.0, 11], sw["pet"]),
        c([-7.3, 19.0, -3.4], [0.6, 5.4, 5], sw["pet"]), c([6.7, 19.0, -3.4], [0.6, 5.4, 5], sw["pet"]),
        c([-5.4, 23.8, -7.9], [4.2, 3.0, 1.0], sw["bril"]), c([1.2, 23.8, -7.9], [4.2, 3.0, 1.0], sw["bril"])]})
    geo["bones"].append({"name": "kapitein_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.2, 11.4, -4.6], [10.4, 2.0, 8.4], sw["sjaal"], inflate=0.1)]})
    geo["bones"].append({"name": "kapitein_sjaalpunt", "parent": "kapitein_sjaal", "pivot": [3.5, 12.4, 3.8], "cubes": [
        c([2.8, 7.4, 3.6], [2.2, 5.0, 0.8], sw["sjaal"]), c([3.0, 6.4, 3.7], [1.8, 1.0, 0.6], sw["sjaalpunt"])]})
    hulp.save_geo(h, "guh_npc_ballonguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.56, sat=2.0, val=1.0)
    rng = np.random.default_rng(28704)
    hulp.paint_swatch(a, sw["pet"], LEER, rng, 10)

    def bril(block):
        for y in range(32):
            for x in range(32):
                if math.dist((x + 0.5, y + 0.5), (16, 16)) < 10:
                    block[y, x, :3] = (150, 214, 246)
        block[8:12, 9:13, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["bril"], MESSING, rng, 6, bril)

    def streep(block):
        for x in range(32):
            if (x // 6) % 2:
                block[:, x, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["sjaal"], (236, 90, 110), rng, 6, streep)
    hulp.paint_swatch(a, sw["sjaalpunt"], (255, 255, 255), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_ballonguh.png")


# =====================================================================================================================
# sounds, advancements, lang
# =====================================================================================================================
SOUNDS = {
    "ballon.brander": [{"name": "minecraft:item.firecharge.use", "type": "event", "pitch": 0.6, "volume": 0.7},
                       {"name": "minecraft:block.fire.ambient", "type": "event", "volume": 1.0}],
    "ballon.wind": [{"name": "minecraft:item.elytra.flying", "type": "event", "volume": 0.25, "pitch": 1.3}],
}
QUEST_ADVANCEMENTS = ["seen_ballonguh", "ballon_wolkje", "ballon_eerste_vlucht", "ballon_alle_stempels", "ballon_stempelkaart_vol"]

ROUTES = {"pluisjesronde": "De Pluisjesronde", "hoge_vads": "De Hoge Vads", "knabbelkring": "De Knabbelkring", "zonnetjesroute": "De Zonnetjesroute"}
UITZICHTEN = [
    ("guhgezichtveld", "Het Guhgezicht-bloemenveld",
     "Kijk naar beneden! Daar ligt een reuzenguhgezicht van bloemen in het gras. Van hierboven lacht het naar je, njeg!",
     "Daar komt het bloemenveld, kijk maar naar beneden...",
     "Een guhgezicht van bloemen, zo groot dat je het alleen vanuit de lucht ziet. De festivalguhs planten het elk jaar opnieuw."),
    ("wolkenpoort", "De Wolkenpoort",
     "We vliegen DOOR een wolk! Alles is even wit en zacht, net een guhknuffel. Pfff, VAHOEG!",
     "Hou je vast, daar voor ons hangt een dikke wolk...",
     "Een wolk om doorheen te vliegen. Binnenin ruikt het naar kaasknabbels, zegt Kapitein Wolkje. (Dat is niet waar.)"),
    ("hoogste_puntje", "Het Hoogste Puntje",
     "Hoger kan niet! Dit is het hoogste puntje van de Hoge Vads. VAHOEG! Zie je hoe klein alles daar beneden is?",
     "Brander vol open... we gaan nog hoger, en hoger...",
     "Het allerhoogste puntje van alle ballonvluchten. Van hier zie je de hele Guhmensie, en een beetje van morgen."),
    ("regenboogbocht", "De Regenboogbocht",
     "Kijk opzij! Een regenboog, speciaal voor ons! Dat gebeurt alleen als je vahoeg genoeg vliegt.",
     "Kijk goed opzij, hier komt de Regenboogbocht...",
     "Een regenboog die opduikt in de bocht. Kapitein Wolkje zegt dat hij hem zelf heeft geverfd. Njeg."),
    ("reuzenguh", "De Reuzenguh",
     "Daar ligt de Reuzenguh! Een ballon zo groot dat hij nooit helemaal opgeblazen raakt. Zwaai maar, hij ziet je!",
     "Kijk links naar beneden, daar ligt iemand heel groots te luieren...",
     "De allergrootste guhballon van het festival. Hij ligt altijd half opgeblazen in het gras, want hij is een beetje vads."),
    ("hartjeswolkje", "Het Hartjeswolkje",
     "Een wolkje in de vorm van een hart! Dat betekent dat er vandaag iemand extra geknuffeld wordt. Misschien jij!",
     "Ik zie iets roze voor ons... wacht maar...",
     "Een hartjeswolkje: wie het ziet, krijgt die dag extra knuffels. Het is wetenschap."),
    ("brandersprong", "De Brandersprong",
     "BRANDER VOL OPEN! Hup, omhoog! Dat kriebelt in je buik, hè? VAHOEG!",
     "Klaar voor een sprongetje? Hou je hoedje vast...",
     "Een flinke plof van de brander en de ballon springt omhoog. Het lievelingsstukje van elke babyguh."),
    ("guhmensie_uitzicht", "Het Guhmensie-uitzicht",
     "En nu... stil maar even. Kijk om je heen: de roze velden, de bergen, de zee in de verte. Is de Guhmensie niet prachtig?",
     "Nog even en dan zie je het mooiste uitzicht van allemaal...",
     "Het mooiste uitzicht over de Guhmensie. Kapitein Wolkje wordt er altijd een beetje stil van."),
]

LANG = {
    "block.guhs.ballonsteiger": "Ballonsteiger",
    "block.guhs.ballonsteiger.lore": "Hier wacht de luchtballon. Praat met Kapitein Wolkje om in te stappen!",
    "block.guhs.mini_luchtballon": "Mini-luchtballon",
    "block.guhs.mini_luchtballon.lore": "Een klein guhballonnetje met een piepklein brandertje. Zweeft niet echt, maar bijna",
    "item.guhs.ballonmunt": "Ballonmunt",
    "item.guhs.ballonmunt.lore": "Voor elke ballonvlucht. Te besteden bij Kapitein Wolkje",
    "item.guhs.ballonpet": "Ballonpet", "item.guhs.ballonbril": "Ballonbril",
    "entity.guhs.guh_luchtballon": "Guh-luchtballon",
    "entity.guhs.guh_npc.ballonguh": "Kapitein Wolkje",
    "structure.guhs.ballonfestival": "Ballonfestival",
    "structure.guhs.ballonfestival.tooltip": "Kapitein Wolkje neemt je mee in zijn guh-luchtballon, langs de mooiste uitzichten",
    "gui.guhs.guhdex.rarity.ballonguh": "Zeldzaamheid: Zeldzaam (Ballonfestival, Guhvelden en Roze pluisjes)",
    "gui.guhs.guhdex.info.ballonguh": "Vliegt met zijn guh-luchtballon over de Guhmensie en vertelt over alles wat je ziet. Hoog, hoger, VAHOEG! "
                                      "Na elke vlucht een stempel op je ballonstempelkaart.",
    "subtitles.guhs.ballon.brander": "Brander blaast",
    "subtitles.guhs.ballon.wind": "Wind suist",
    "gui.guhs.ballon.beschermd": "Njeg! Het Ballonfestival is van Kapitein Wolkje. Hier niks slopen of bouwen!",
    "gui.guhs.ballon.praat_met_wolkje": "Praat met Kapitein Wolkje om mee te vliegen!",
    "gui.guhs.ballon.blijf_zitten": "Njeg! Niet springen, we zijn hoog! Blijf lekker in het mandje.",
    "gui.guhs.ballon.route": "Vlucht: %s",
    "gui.guhs.ballon.stempel_onderweg": "Uitzicht: %s (stempel na de landing!)",
    "quest.guhs.ballon.hallo": "Ahoi, grondguh! Kapitein Wolkje hier. Ik vlieg met mijn guh-luchtballon langs de mooiste plekjes van de Guhmensie. "
                               "Je hoeft niks te sturen, alleen maar te kijken. Na elke vlucht krijg je een stempel. VAHOEG, instappen!",
    "quest.guhs.ballon.instappen": "Instappen maar! Vandaag vliegen we %s. Hou je pootjes binnen het mandje, njeg!",
    "quest.guhs.ballon.al_in_de_lucht": "Mijn ballon is al in de lucht! Wacht maar even, hij komt zo terug.",
    "quest.guhs.ballon.geen_ballon": "Oei, waar is mijn ballon gebleven? Zonder ballonsteiger kan ik er ook geen nieuwe halen. Njeg...",
    "quest.guhs.ballon.winkel": "Mijn winkeltje! Voor ballonmunten: een ballonpet, een ballonbril en mini-luchtballonnetjes.",
    "quest.guhs.ballon.landen": "Daar is de ballonsteiger weer. Zachtjes landen... bijna... plof!",
    "quest.guhs.ballon.geland": "Geland! Deze uitzichten had je al gestempeld, maar het blijft mooi, hè? %2$s ballonmunten voor jou.",
    "quest.guhs.ballon.geland_stempels": "Geland! %1$s nieuwe stempel(s) op je kaart (%3$s/%4$s) en %2$s ballonmunten. VAHOEG gevlogen!",
    "quest.guhs.ballon.afgebroken": "O jee, de vlucht is afgebroken. We zijn weer veilig beneden. De volgende keer beter, njeg!",
    # take-off and the lines on the way, per route
    "quest.guhs.ballon.vertrek.pluisjesronde": "Brander aan! We vliegen de Pluisjesronde: eerst over het bloemenveld, dan dwars door een wolk!",
    "quest.guhs.ballon.onderweg.pluisjesronde.0": "Voel je hoe zacht we stijgen? Zo zacht als een pluisje. Daarom heet het de Pluisjesronde.",
    "quest.guhs.ballon.onderweg.pluisjesronde.1": "Wist je dat de Mika's ook een ballon wilden? Maar hij was te zwaar van al die gestolen kaasknabbels. Plof!",
    "quest.guhs.ballon.vertrek.hoge_vads": "Brander aan! Vandaag de Hoge Vads: zo hoog als een ballon maar kan. Niet naar beneden kijken... of juist wel!",
    "quest.guhs.ballon.onderweg.hoge_vads.0": "Hoger en hoger... mijn oren flapperen al. Die van jou ook?",
    "quest.guhs.ballon.onderweg.hoge_vads.1": "Een echte ballonvaarder is nooit bang, alleen een beetje vads. Njeg.",
    "quest.guhs.ballon.vertrek.knabbelkring": "Brander aan! De Knabbelkring gaat langs de Reuzenguh en een heel bijzonder wolkje.",
    "quest.guhs.ballon.onderweg.knabbelkring.0": "Kijk, daar beneden zwaaien de festivalguhs! Zwaai maar terug.",
    "quest.guhs.ballon.onderweg.knabbelkring.1": "Deze route heet de Knabbelkring omdat ik onderweg altijd een kaasknabbel eet. *knabbel*",
    "quest.guhs.ballon.vertrek.zonnetjesroute": "Brander aan! De Zonnetjesroute: met een sprongetje en het mooiste uitzicht van allemaal.",
    "quest.guhs.ballon.onderweg.zonnetjesroute.0": "Ruik je dat? Frisse lucht met een vleugje kaassaus. Heerlijk.",
    "quest.guhs.ballon.onderweg.zonnetjesroute.1": "Soms zie ik vanaf hier Professor Sterretje zwaaien, bij zijn sterrenwacht op de bergen.",
    # the Knus tab
    "gui.guhs.knus.verzameling.ballonstempels": "Ballonstempelkaart",
    "gui.guhs.knus.mijlpaal.ballon_gevonden": "Praat met Kapitein Wolkje",
    "gui.guhs.knus.mijlpaal.ballon_eerste_vlucht": "Je eerste ballonvlucht",
    "gui.guhs.knus.mijlpaal.ballon_vier_vluchten": "4 ballonvluchten",
    "gui.guhs.knus.mijlpaal.ballon_alle_stempels": "Een volle stempelkaart (8)",
    "gui.guhs.knus.mijlpaal.ballon_tien_vluchten": "10 ballonvluchten",
}


def advancements(h):
    hulp.quest_advancements(h, QUEST_ADVANCEMENTS)
    adv = hulp.display_advancement
    adv(h, "ballon_gevonden", "root", "guhs:mini_luchtballon", "task", hulp.in_structure(NAME),
        "Ballonnen in de lucht!", "Vind het Ballonfestival op de Guhvelden of de Roze pluisjes")
    adv(h, "ballon_eerste_vlucht", "ballon_gevonden", "guhs:ballonmunt", "goal", hulp.IMPOSSIBLE,
        "Hoog, hoger, VAHOEG!", "Maak een ballonvlucht met Kapitein Wolkje")
    adv(h, "ballon_alle_stempels", "ballon_eerste_vlucht", "guhs:ballonpet", "challenge", hulp.IMPOSSIBLE,
        "Volle stempelkaart", "Verzamel alle acht ballonstempels: vlieg alle vier de routes")


def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    for rid, naam in ROUTES.items():
        h.lang(f"gui.guhs.ballon.route.{rid}", naam, naam)
    for uid, naam, zin, komt, info in UITZICHTEN:
        h.lang(f"quest.guhs.ballon.uitzicht.{uid}", zin, zin)
        h.lang(f"quest.guhs.ballon.uitzicht.{uid}.komt", komt, komt)
        h.lang(f"gui.guhs.knus.ballonstempels.{uid}", naam, naam)
        h.lang(f"gui.guhs.knus.ballonstempels.{uid}.info", info, info)


def selfcheck(h):
    import os
    missing = []
    for b in ("ballonsteiger", "mini_luchtballon"):
        if not os.path.exists(f"{h.A}/blockstates/{b}.json") or f"block.guhs.{b}" not in h.NL:
            missing.append(b)
    for f in ("geo/entity/guh_luchtballon.geo.json", "animations/entity/guh_luchtballon.animation.json", "geo/entity/guh_npc_ballonguh.geo.json"):
        if not os.path.exists(f"{h.A}/{f}"):
            missing.append(f)
    for k in model.KLEUREN:
        if not os.path.exists(f"{h.A}/textures/entity/guh_luchtballon_{k}.png"):
            missing.append(k)
    if len(UITZICHTEN) != 8:
        missing.append("8 viewpoints")
    if missing:
        raise SystemExit(f"ballon assets missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    model.build(h)
    npc(h)
    hulp.sounds(h, SOUNDS)
    advancements(h)
    texts(h)
    s, info = bouw.build(h)
    n = bouw.check(s, info)
    h.TEMPLATE_SIZES[NAME] = 36
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["guh_fields", "pink_puffs"], spacing=26, separation=9, salt=20280602, start_y=-bouw.G, reach=60, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none})
    s.save(NAME)
    selfcheck(h)
    print(f"ballon: geometry check ok ({n} walkable spots, {info['gezichten']} guh faces)")


# =====================================================================================================================
# FTB quests (row y = 93.5), no dependencies
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, FTB_Y
    q("ballon_gevonden", "Het Ballonfestival", "Op de &dGuhvelden&r en de &dRoze pluisjes&r is soms een Ballonfestival, vol luchtballonnen in de vorm "
      "van guhkoppen. Het superkompas (categorie Knus) wijst de weg.", "guhs:mini_luchtballon", [fq.structure(NAME)],
      rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=y, shape="circle", xp=150)
    q("ballon_wolkje", "Kapitein Wolkje", "Praat met &dKapitein Wolkje&r bij zijn kiosk naast de ballonsteiger. Hij neemt je meteen mee de lucht in!",
      "guhs:ballonpet", [fq.adv("ballon_wolkje")], rewards=(("guhs:kaas_knabbels", 8),), x=-6.5, y=y, xp=100)
    q("ballon_eerste_vlucht", "Hoog, hoger, VAHOEG!", "Maak een ballonvlucht. Je hoeft niks te sturen: kijk rond en luister naar de Kapitein. "
      "Na de landing krijg je stempels en ballonmunten.", "guhs:ballonmunt", [fq.adv("ballon_eerste_vlucht")],
      rewards=(("guhs:ballonmunt", 1),), x=-5, y=y, xp=150)
    q("ballon_stempelkaart", "Volle stempelkaart", "Er zijn vier routes met elk twee uitzichten. Verzamel alle acht stempels op je ballonstempelkaart "
      "(Guhdex, tab Knus).", "minecraft:map", [fq.adv("ballon_alle_stempels")], rewards=(("guhs:mini_luchtballon", 2),), x=-3.5, y=y,
      shape="gear", xp=400)
    q("ballon_pakje", "Klaar voor de lucht", "Koop de ballonpet en de ballonbril bij Kapitein Wolkje (sluip + praten: zijn winkeltje) "
      "en trek ze je guh aan.", "guhs:ballonbril", [fq.item("guhs:ballonpet"), fq.item("guhs:ballonbril")],
      rewards=(("guhs:kaas_knabbels", 12),), x=-2, y=y, xp=150)
    q("ballon_mini", "Een ballonnetje voor thuis", "Een mini-luchtballon voor in je huis: koop er een bij de Kapitein of maak hem zelf van wol, "
      "papier en touw.", "guhs:mini_luchtballon", [fq.item("guhs:mini_luchtballon")], rewards=(("guhs:kaas_knabbels", 6),), x=-0.5, y=y, xp=100)
    q("ballon_guhdex", "Een luchtig gezicht", "Zet Kapitein Wolkje in je Guhdex (kom dichtbij genoeg).",
      "guhs:guhdex", [fq.adv("seen_ballonguh")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=y, shape="rsquare", xp=100)
