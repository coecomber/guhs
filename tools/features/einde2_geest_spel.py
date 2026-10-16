"""
einde2 phase B/C (CONTRACT_E2_B 7.9), slice geest_spel: the play of the Geestenwereld (zone access, the five trials, the Pai
Sho tiles and the Oude Reiger, the lost lanterns, Hei Bai-guh's acorn, Koh's riddles, the chakra path, fast travel and the
mirror ponds, the rare sightings, footprints, the Geestenklok, the swamp vision, Wan-guh and the eighth island).
Java: nl.juiced.guhs.feature.einde2.geestspel (E2GeestSpelFeature).

  lang      gui.guhs.einde2_geest_spel.*                     every message (einde2_geest_spel_tekst.TEKST)
            gui.guhs.verhalen.geest_*                        the seven lines (verhaal_motor.verhaallijn)
            scene.guhs.einde2_geest_spel_*                   the five scenes (verhaal_motor.scene)
            item.guhs.pai_sho_tegel_*, item.guhs.einde2_geest_spel_*, block.guhs.einde2_geest_spel_mediteerkussen (+ .lore)
  assets    textures/item/<id>.png (drawn here), models/item, the cushion's block model + blockstate
  data      advancement/quest/geest_*_stap_<i> (the lines), advancement/quest/einde2_geest_spel_* (granted from code),
            recipe/einde2_geest_spel_knabbelkoor_eikel, recipe/einde2_geest_spel_mediteerkussen, loot of the cushion
  FTB       the section "De Geestenwereld" in the chapter guhs_guheinde (16 quests, ftb(fq))

Chunk c97_e2_geest_spel; English: tools/lang/en/c97_e2_geest_spel.json; wiki texts: einde2_geest_spel_wiki.py.
"""
import math

from PIL import Image, ImageDraw

import features.einde2_kit as kit
from features import verhaal_motor
from features import einde2_geest_spel_tekst as tekst

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTIES = [("einde2_geest_spel", tekst.SECTIE, "item:guhs:pai_sho_tegel_witte_lotus", None)]

TEGELS = ["bumi", "pakguh", "piandao", "jeong_jeong", "miroh", "witte_lotus"]
ITEMS = ["einde2_geest_spel_knabbelkoor_eikel", "einde2_geest_spel_koh_masker", "einde2_geest_spel_veer", "einde2_geest_spel_gouden_rol",
         "einde2_geest_spel_koh_schub", "einde2_geest_spel_maandruppel", "einde2_geest_spel_eerste_knabbel"]
KUSSEN = "einde2_geest_spel_mediteerkussen"


def build(h):
    teksten(h)
    lijnen(h)
    for naam in tekst.VERBORGEN:
        kit.verborgen(h, naam)
    tegels(h)
    voorwerpen(h)
    kussen(h)
    recepten(h)
    selfcheck(h)


# =====================================================================================================================
# texts
# =====================================================================================================================
def teksten(h):
    for k, v in tekst.TEKST.items():
        kit.lang(h, tekst.P + k, v)
    for key, (naam, lore) in tekst.ITEMS.items():
        kit.lang(h, key, naam)
        kit.lang(h, key + ".lore", lore)
    key, naam, lore = tekst.BLOK
    kit.lang(h, key, naam)
    kit.lang(h, key + ".lore", lore)
    for sid, (titel, regels, namen) in tekst.SCENES.items():
        verhaal_motor.scene(h, sid, titel, regels, namen)


def lijnen(h):
    for lid, (naam, uitleg, stappen, klaar) in tekst.LIJNEN.items():
        verhaal_motor.verhaallijn(h, lid, naam, uitleg, stappen, klaar=klaar)


# =====================================================================================================================
# pictures (16 x 16, drawn by hand in code)
# =====================================================================================================================
def _leeg():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _tegel(symbool, kleur, rand=(196, 170, 120), vlak=(244, 234, 210)):
    """A round Pai Sho tile: a cream disc with a dark rim, a symbol in the middle and two tiny guh ears on top."""
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.ellipse((1, 2, 14, 15), fill=rand + (255,))
    d.ellipse((2, 3, 13, 14), fill=vlak + (255,))
    d.point([(5, 2), (10, 2)], fill=rand + (255,))     # (the guh ears on the rim)
    d.point([(5, 1), (10, 1)], fill=(232, 150, 170, 255))
    k = kleur + (255,)
    if symbool == "berg":           # an upside-down mountain (Bumi-guh's joke)
        d.polygon([(4, 6), (11, 6), (7, 12)], fill=k)
        d.point([(7, 7), (6, 7), (8, 7)], fill=(255, 255, 255, 255))
    elif symbool == "golf":
        for x in range(4, 12):
            y = 9 + round(1.5 * math.sin((x - 4) * 0.9))
            d.point([(x, y), (x, y + 1)], fill=k)
        d.point([(6, 6), (7, 6), (8, 6), (9, 6)], fill=(90, 70, 50, 255))   # (a moustache, very small)
    elif symbool == "zwaard":
        d.line([(5, 12), (10, 5)], fill=k)
        d.line([(6, 12), (11, 5)], fill=(200, 205, 215, 255))
        d.line([(5, 9), (8, 12)], fill=(120, 80, 40, 255))
    elif symbool == "vlam":
        d.polygon([(7, 4), (10, 9), (9, 12), (6, 12), (5, 9)], fill=k)
        d.polygon([(7, 7), (8, 10), (7, 12), (6, 10)], fill=(255, 220, 90, 255))
    elif symbool == "blad":
        d.ellipse((5, 6, 10, 12), fill=k)
        d.line([(5, 12), (10, 6)], fill=(60, 110, 50, 255))
        d.point([(11, 5), (12, 4)], fill=(210, 210, 200, 255))   # (a wisp of steam)
    elif symbool == "lotus":
        wit = (255, 255, 255, 255)
        d.polygon([(7, 4), (9, 8), (7, 12), (5, 8)], fill=wit)
        d.polygon([(3, 8), (7, 12), (4, 12)], fill=wit)
        d.polygon([(12, 8), (8, 12), (11, 12)], fill=wit)
        d.point([(7, 8), (7, 9)], fill=(250, 210, 120, 255))
        d.ellipse((3, 11, 12, 13), outline=k)
    return img


def tegels(h):
    soorten = {"bumi": ("berg", (90, 150, 70)), "pakguh": ("golf", (60, 120, 200)), "piandao": ("zwaard", (70, 70, 80)),
               "jeong_jeong": ("vlam", (210, 70, 40)), "miroh": ("blad", (120, 170, 80)), "witte_lotus": ("lotus", (150, 190, 220))}
    for m in TEGELS:
        symbool, kleur = soorten[m]
        rand = (150, 160, 190) if m == "witte_lotus" else (196, 170, 120)
        vlak = (200, 216, 236) if m == "witte_lotus" else (244, 234, 210)
        h.save(_tegel(symbool, kleur, rand, vlak), "item", f"pai_sho_tegel_{m}.png")
        h.item_model(f"pai_sho_tegel_{m}")


def voorwerpen(h):
    # the knabbelkoor acorn: a brown nut under a purple knabbelkoor cap
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.ellipse((4, 6, 11, 14), fill=(176, 120, 70, 255))
    d.ellipse((5, 7, 8, 11), fill=(206, 152, 96, 255))
    d.ellipse((3, 3, 12, 8), fill=(132, 70, 170, 255))
    d.point([(5, 4), (7, 4), (9, 5), (6, 6), (10, 4)], fill=(186, 120, 220, 255))
    d.line([(7, 1), (8, 3)], fill=(90, 60, 40, 255))
    h.save(img, "item", "einde2_geest_spel_knabbelkoor_eikel.png")
    # Koh's mask: a pale face without any feeling, two ear points
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.ellipse((2, 2, 13, 15), fill=(236, 232, 226, 255), outline=(150, 140, 140, 255))
    d.polygon([(3, 4), (4, 0), (6, 3)], fill=(236, 232, 226, 255))
    d.polygon([(12, 4), (11, 0), (9, 3)], fill=(236, 232, 226, 255))
    d.line([(4, 7), (6, 7)], fill=(40, 30, 40, 255))
    d.line([(9, 7), (11, 7)], fill=(40, 30, 40, 255))
    d.line([(6, 11), (9, 11)], fill=(140, 60, 70, 255))
    d.point([(3, 9), (12, 9)], fill=(214, 150, 160, 255))
    h.save(img, "item", "einde2_geest_spel_koh_masker.png")
    # the white heron's feather
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.line([(3, 14), (12, 2)], fill=(170, 170, 180, 255))
    for i in range(9):
        x, y = 4 + i, 12 - round(i * 1.25)
        d.line([(x, y), (x - 2, y - 2)], fill=(250, 250, 255, 255))
        d.line([(x, y), (x + 2, y + 1)], fill=(232, 238, 250, 255))
    h.save(img, "item", "einde2_geest_spel_veer.png")
    # the golden scroll
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.rectangle((3, 5, 12, 11), fill=(250, 220, 110, 255))
    d.ellipse((1, 4, 4, 12), fill=(220, 170, 50, 255))
    d.ellipse((11, 4, 14, 12), fill=(220, 170, 50, 255))
    for x in range(5, 11, 2):
        d.point([(x, 7), (x + 1, 9)], fill=(160, 110, 40, 255))
    h.save(img, "item", "einde2_geest_spel_gouden_rol.png")
    # a scale of Koh: dark, glossy, a pale shine
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 2), (13, 7), (11, 14), (5, 14), (3, 7)], fill=(40, 34, 52, 255))
    d.line([(6, 5), (8, 3)], fill=(150, 140, 190, 255))
    d.line([(5, 9), (11, 9)], fill=(70, 60, 90, 255))
    h.save(img, "item", "einde2_geest_spel_koh_schub.png")
    # a moon drop: a silver drop with a little moon in it
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (12, 8), (8, 14), (4, 8)], fill=(206, 214, 236, 255))
    d.ellipse((4, 6, 12, 14), fill=(206, 214, 236, 255))
    d.ellipse((6, 7, 10, 11), fill=(255, 255, 255, 255))
    d.ellipse((7, 7, 11, 10), fill=(206, 214, 236, 255))
    h.save(img, "item", "einde2_geest_spel_maandruppel.png")
    # the first knabbel: a little stone cheese knabbel with a warm glow
    img = _leeg()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 12), (13, 12), (12, 5), (4, 3)], fill=(150, 146, 136, 255))
    d.polygon([(2, 12), (13, 12), (13, 14), (2, 14)], fill=(110, 106, 100, 255))
    for (x, y) in [(6, 7), (9, 9), (5, 10), (10, 6)]:
        d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(96, 92, 86, 255))
    d.point([(4, 4), (3, 6)], fill=(255, 226, 150, 255))
    h.save(img, "item", "einde2_geest_spel_eerste_knabbel.png")
    for i in ITEMS:
        h.item_model(i)


def kussen(h):
    """The meditation cushion: a lilac cushion with a gold rim, a white lotus stitched on top and four tassels."""
    tex = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(tex)
    d.rectangle((0, 0, 15, 15), fill=(150, 112, 200, 255))
    for x in range(16):
        for y in range(16):
            if (x * 7 + y * 3) % 11 == 0:
                tex.putpixel((x, y), (138, 100, 188, 255))
    d.rectangle((0, 0, 15, 0), fill=(226, 190, 90, 255))
    d.rectangle((0, 15, 15, 15), fill=(226, 190, 90, 255))
    d.rectangle((0, 0, 0, 15), fill=(226, 190, 90, 255))
    d.rectangle((15, 0, 15, 15), fill=(226, 190, 90, 255))
    d.polygon([(8, 4), (10, 8), (8, 12), (6, 8)], fill=(250, 250, 255, 255))
    d.polygon([(4, 8), (8, 12), (5, 12)], fill=(236, 236, 248, 255))
    d.polygon([(12, 8), (8, 12), (11, 12)], fill=(236, 236, 248, 255))
    h.save(tex, "block", f"{KUSSEN}.png")
    zijkant = Image.new("RGBA", (16, 16), (128, 92, 176, 255))
    ImageDraw.Draw(zijkant).rectangle((0, 0, 15, 1), fill=(226, 190, 90, 255))
    h.save(zijkant, "block", f"{KUSSEN}_zij.png")
    kwast = Image.new("RGBA", (16, 16), (226, 190, 90, 255))
    h.save(kwast, "block", f"{KUSSEN}_kwast.png")
    t = f"guhs:block/{KUSSEN}"
    elementen = [
        h.el([1, 0, 1], [15, 4, 15], "#zij", faces=("down", "north", "south", "west", "east")),
        h.el([1, 4, 1], [15, 4.01, 15], "#boven", faces=("up",)),
        h.el([0, 2, 0], [2, 3, 2], "#kwast"), h.el([14, 2, 0], [16, 3, 2], "#kwast"),
        h.el([0, 2, 14], [2, 3, 16], "#kwast"), h.el([14, 2, 14], [16, 3, 16], "#kwast"),
    ]
    h.furniture_model(KUSSEN, elementen, {"particle": t, "boven": t, "zij": t + "_zij", "kwast": t + "_kwast"})


def recepten(h):
    h.shapeless("einde2_geest_spel_knabbelkoor_eikel", ["guhs:knabbelkoorvrucht", "guhs:kaas_knabbels"], "guhs:einde2_geest_spel_knabbelkoor_eikel")
    h.shaped(KUSSEN, [" A ", "WWW"], {"A": "minecraft:amethyst_shard", "W": "minecraft:purple_wool"}, f"guhs:{KUSSEN}")


# =====================================================================================================================
# FTB: the section "De Geestenwereld" (chapter guhs_guheinde, right after the Guhvatar sections)
# =====================================================================================================================
def ftb(fq):
    plek = {"binnen": (0, 0), "lantaarns": (2, -1.5), "hei_bai": (2, 0), "koh": (2, 1.5), "poort": (4, 0), "proeven": (6, 0), "leden": (8, 0),
            "reiger": (10, 0), "archief": (12, 0), "chakra": (4, -2), "verschijningen": (6, -2), "klok": (4, 2), "voetstappen": (6, 2),
            "spiegel": (8, 2), "kussen": (8, -2), "wan": (10, -2)}
    na = {"lantaarns": "binnen", "hei_bai": "binnen", "koh": "binnen", "poort": "binnen", "proeven": "poort", "leden": "proeven", "reiger": "leden",
          "archief": "reiger", "chakra": "binnen", "verschijningen": "binnen", "klok": "binnen", "voetstappen": "binnen", "spiegel": "binnen",
          "kussen": "binnen", "wan": "binnen"}
    groot = {"binnen", "archief", "wan", "chakra"}
    for key, titel, tekst_, icoon, taak in tekst.FTB:
        soort, arg = taak
        t = fq.adv(arg) if soort == "adv" else fq.dim(arg)
        x, y = plek[key]
        deps = [f"einde2_geest_spel_{na[key]}"] if key in na else []
        fq.q(f"einde2_geest_spel_{key}", titel, tekst_, icoon, [t], rewards=(("guhs:kaas_knabbels", 16 if key in groot else 8),), deps=deps,
             x=x, y=y, shape="gear" if key in groot else None, xp=150 if key in groot else 75)


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    import os
    problems = []
    for lid, (_, _, stappen, _) in tekst.LIJNEN.items():
        for i in range(1, len(stappen) + 1):
            if not os.path.exists(f"{h.D}/advancement/quest/{lid}_stap_{i}.json"):
                problems.append(f"no advancement {lid}_stap_{i}")
    stappen = {"geest_witte_lotus": 6, "geest_lantaarns": 8, "geest_hei_bai": 2, "geest_koh": 3, "geest_chakra": 7, "geest_verschijningen": 4,
               "geest_wan": 3}
    for lid, n in stappen.items():
        if len(tekst.LIJNEN[lid][2]) != n:
            problems.append(f"{lid}: {len(tekst.LIJNEN[lid][2])} steps, Java has {n}")
    for _, _, _, _, (soort, arg) in tekst.FTB:
        if soort == "adv" and not os.path.exists(f"{h.D}/advancement/quest/{arg}.json"):
            problems.append(f"FTB task without advancement: {arg}")
    for m in TEGELS:
        if f"item.guhs.pai_sho_tegel_{m}" not in h.NL:
            problems.append(f"no name for the tile {m}")
    if not 12 <= len(tekst.FTB) <= 16:
        problems.append(f"{len(tekst.FTB)} FTB quests (12-16)")
    if problems:
        raise SystemExit("einde2_geest_spel self-check: " + "; ".join(problems))
