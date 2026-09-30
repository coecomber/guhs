"""
De Guhkamer in je Guhmaag + de Guhbel (2.10 "Lieve vadsjes van elkaar", slice speelgoed; CONTRACT_210 par. 5.5).

  - guhkamer_deur   the door to (and in) the Guhkamer: a cherry-wood arch with guh ears on top, a little name board and a
                    soft pink curtain with a heart that you walk through (two halves, placed by the Guhkamer itself)
  - guhbel          a golden bell with a pink bow and guh ears: send a guh to your Guhkamer, call a guest back
Plus textures, the bell's icon and recipe, sounds, texts (screen, signs, messages, wist-je-datjes), advancements, the
game test room and the FTB section "De Guhkamer in je Guhmaag".
"""
import os

import numpy as np
from PIL import Image, ImageDraw

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
VACHT = (246, 168, 200)


def _ruis(size, basis, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _hout():
    """Pink-ish cherry wood for the arch."""
    img = Image.fromarray(_ruis(16, (232, 160, 162), 5, 5101))
    d = ImageDraw.Draw(img)
    for x in (0, 8):
        d.line([(x, 0), (x, 15)], fill=(200, 122, 130, 255))
    for y in (4, 11):
        img.putpixel((3, y), (214, 138, 144, 255))
        img.putpixel((12, y + 2), (214, 138, 144, 255))
    return img


def _gordijn(hart):
    """A soft pink velvet curtain with folds (and, on the upper half, a heart)."""
    a = _ruis(16, (242, 128, 178), 3, 5102 if hart else 5103)
    for x in range(16):
        k = [0, -18, -8, 10, 18, 8, -6, -18, -8, 10, 18, 8, -6, -18, -8, 6][x]
        a[:, x, :3] = np.clip(a[:, x, :3].astype(int) + k, 0, 255).astype(np.uint8)
    img = Image.fromarray(a)
    if hart:
        heart = [(5, 5), (6, 5), (9, 5), (10, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (4, 7), (5, 7), (6, 7), (7, 7),
                 (8, 7), (9, 7), (10, 7), (11, 7), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10)]
        for p in heart:
            img.putpixel(p, (255, 232, 242, 255))
        img.putpixel((5, 6), (255, 255, 255, 255))
    else:   # a golden tassel rope at the bottom
        d = ImageDraw.Draw(img)
        d.line([(0, 13), (15, 13)], fill=(255, 214, 92, 255))
        for x in range(1, 16, 3):
            img.putpixel((x, 14), (228, 170, 50, 255))
    return img


def _bordje():
    """The little name board over the door: cream with pink 'letters' (Guhkamer)."""
    img = Image.new("RGBA", (16, 16), (255, 244, 226, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(200, 122, 130, 255))
    for x, w in ((2, 2), (5, 1), (7, 2), (10, 1), (12, 2)):
        d.rectangle([x, 6, x + w - 1, 9], fill=(214, 86, 138, 255))
    return img


def _bel_icoon():
    rows = [
        "..kk........kk..",
        ".kpok......kopk.",
        ".kpokkkkkkkkopk.",
        "..kkkbbkkbbkkk..",
        "....kbBbbBbk....",
        ".....kkyykk.....",
        "....kyyWyyyk....",
        "...kyyWyyyyyk...",
        "...kyWyyyyyyk...",
        "..kyyWyyyyyyyk..",
        "..kyyyyyyyyyYk..",
        ".kyyyyyyyyyyYYk.",
        ".kYYYYYYYYYYYYk.",
        "..kkkkkkkkkkkk..",
        "......kbbk......",
        ".......kk.......",
    ]
    pal = {"k": (122, 48, 88, 255), "p": VACHT + (255,), "o": (255, 150, 196, 255), "b": (236, 70, 120, 255), "B": (255, 150, 190, 255),
           "y": (255, 214, 92, 255), "Y": (228, 170, 50, 255), "W": (255, 246, 200, 255)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def textures(h):
    h.save(_hout(), "block", "guhkamer_hout.png")
    h.save(_gordijn(False), "block", "guhkamer_gordijn.png")
    h.save(_gordijn(True), "block", "guhkamer_gordijn_hart.png")
    h.save(_bordje(), "block", "guhkamer_bordje.png")
    h.save(_bel_icoon(), "item", "guhbel.png")


# =====================================================================================================================
# models
# =====================================================================================================================
def _el(frm, to, tex, faces="all"):
    names = ["north", "east", "south", "west", "up", "down"] if faces == "all" else faces
    return {"from": [float(v) for v in frm], "to": [float(v) for v in to],
            "faces": {n: {"uv": [0, 0, 16, 16], "texture": f"#{tex}"} for n in names}}


TEX = {"hout": "guhs:block/guhkamer_hout", "gordijn": "guhs:block/guhkamer_gordijn", "hart": "guhs:block/guhkamer_gordijn_hart",
       "bordje": "guhs:block/guhkamer_bordje", "vacht": "guhs:block/speelgoed_vacht", "oor": "guhs:block/speelgoed_oor"}


def _model(els):
    return {"render_type": "minecraft:cutout", "ambientocclusion": False, "textures": dict(TEX, particle=TEX["gordijn"]), "elements": els}


def deur_onder():
    return _model([_el([0, 0, 6], [2, 16, 10], "hout"), _el([14, 0, 6], [16, 16, 10], "hout"),
                   _el([2, 0, 7.5], [14, 16, 8.5], "gordijn", ["north", "south"]),
                   _el([-0.5, 0, 5], [16.5, 1, 11], "hout")])


def deur_boven():
    els = [_el([0, 0, 6], [2, 12, 10], "hout"), _el([14, 0, 6], [16, 12, 10], "hout"), _el([0, 12, 6], [16, 16, 10], "hout"),
           _el([2, 10, 6], [4, 12, 10], "hout"), _el([12, 10, 6], [14, 12, 10], "hout"),
           _el([2, 0, 7.5], [14, 10, 8.5], "hart", ["north", "south"]), _el([4, 10, 7.5], [12, 12, 8.5], "gordijn", ["north", "south"]),
           _el([5, 12.5, 5.5], [11, 15.5, 6], "bordje", ["north"]), _el([5, 12.5, 10], [11, 15.5, 10.5], "bordje", ["south"])]
    for x0 in (1, 11):   # guh ears on top of the arch, pink on both sides
        els.append(_el([x0, 16, 6.5], [x0 + 4, 20, 9.5], "vacht"))
        els.append(_el([x0 + 0.8, 16.6, 6.4], [x0 + 3.2, 19.3, 6.5], "oor", ["north"]))
        els.append(_el([x0 + 0.8, 16.6, 9.5], [x0 + 3.2, 19.3, 9.6], "oor", ["south"]))
    return _model(els)


def blocks_and_items(h):
    A = h.A
    h.w(f"{A}/models/block/guhkamer_deur_onder.json", deur_onder())
    h.w(f"{A}/models/block/guhkamer_deur_boven.json", deur_boven())
    variants = {}
    for f, r in ROT.items():
        for half, model in (("lower", "guhkamer_deur_onder"), ("upper", "guhkamer_deur_boven")):
            v = {"model": f"guhs:block/{model}"}
            if r:
                v["y"] = r
            variants[f"facing={f},half={half}"] = v
    h.w(f"{A}/blockstates/guhkamer_deur.json", {"variants": variants})
    h.item_model("guhbel")
    h.shaped("guhbel", [" W ", "GKG", "G G"], {"W": "minecraft:pink_wool", "G": "minecraft:gold_ingot", "K": "guhs:kaas_knabbels"}, "guhs:guhbel")


# =====================================================================================================================
# sounds, advancements, texts
# =====================================================================================================================
SOUNDS = {
    "guhkamer.bel": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6},
                     {"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.4}],
    "guhkamer.weg": [{"name": "minecraft:entity.fox.teleport", "type": "event", "pitch": 1.5},
                     {"name": "guhs:guh_ambient7", "pitch": 1.3}],
    "guhkamer.terug": [{"name": "minecraft:entity.fox.teleport", "type": "event", "pitch": 1.2},
                       {"name": "guhs:guh_ambient2", "pitch": 1.2}],
    "guhkamer.deur": [{"name": "minecraft:item.armor.equip_leather", "type": "event", "pitch": 1.3},
                      {"name": "minecraft:block.wool.step", "type": "event", "pitch": 0.8}],
    "guhkamer.groei": [{"name": "minecraft:entity.player.levelup", "type": "event", "pitch": 1.4},
                       {"name": "minecraft:block.amethyst_block.chime", "type": "event", "pitch": 1.0}],
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    from features import band
    band.visible(h, "guhkamer_bel", "root", "guhs:guhbel", "task", "Tingeling!",
                 "Stuur een guh met de Guhbel naar de Guhkamer in je Guhmaag")
    band.visible(h, "guhkamer_binnen", "guhkamer_bel", "guhs:guhbel", "task", "Logeerpartijtje",
                 "Stap door de deur in je Guhmaag je eigen Guhkamer in")
    band.visible(h, "guhkamer_groei", "guhkamer_binnen", "minecraft:nether_star", "goal", "Een kamer met uitzicht",
                 "Je Guhkamer is gegroeid, dankzij een zielsguh bff 5evr <3")
    band.visible(h, "guhkamer_bezoek", "guhkamer_binnen", "minecraft:cake", "task", "Op visite, njeg",
                 "Bezoek de Guhkamer van iemand anders (zijn maag moet openbaar zijn)")


TEXTS = {
    "block.guhs.guhkamer_deur": "Deur van de Guhkamer",
    "item.guhs.guhbel": "Guhbel",
    "item.guhs.guhbel.lore": "Tingeling! Een gouden belletje met guhoortjes.",
    "item.guhs.guhbel.uitleg": "Rechtsklik: stuur je guhs naar de Guhkamer in je Guhmaag, of roep ze terug naar jou.",
    # signs
    "sign.guhs.guhkamer.maag1": "Guhkamer",
    "sign.guhs.guhkamer.maag2": "logeerkamer voor",
    "sign.guhs.guhkamer.maag3": "vadsige guhs",
    "sign.guhs.guhkamer.kamer1": "Welkom in de",
    "sign.guhs.guhkamer.kamer2": "Guhkamer!",
    "sign.guhs.guhkamer.kamer3": "(deur = terug)",
    # messages
    "gui.guhs.guhkamer.gegroeid": "Je Guhkamer is gegroeid! Nu %s x %s blokken, plek voor %s logeerguhs. Dank je, zielsguh!",
    "gui.guhs.guhkamer.groeit_straks": "Een nieuwe zielsguh! Je Guhkamer groeit zodra je er weer binnenstapt.",
    "gui.guhs.guhkamer.even_geduld": "%s ligt nog diep te slapen in de Guhkamer... probeer het zo nog eens, njeg.",
    "gui.guhs.guhkamer.geroepen": "Tingeling! %s komt aangehuppeld.",
    "gui.guhs.guhkamer.gestuurd": "%s gaat lekker logeren in je Guhkamer. Doei doei!",
    "gui.guhs.guhkamer.vol": "Je Guhkamer zit vol! Hij groeit met elke zielsguh bff 5evr <3.",
    "gui.guhs.guhkamer.geen_maag": "Je hebt nog geen Guhmaag, njeg! Doe eerst de maagquest.",
    "gui.guhs.guhkamer.niet_jouw": "Alleen je eigen tamme guhs mogen in je Guhkamer logeren.",
    "gui.guhs.guhkamer.prive": "De maag van %s is niet openbaar: de Guhkamer blijft dicht.",
    "gui.guhs.guhkamer.welkom_thuis": "Welkom in je Guhkamer! Er logeren hier %s guhs.",
    "gui.guhs.guhkamer.welkom_bezoek": "Welkom in de Guhkamer van %s! Kijken mag, aaien niet.",
    # the Guh menu (2.10.1): in and out of the logeerkamer
    "gui.guhs.menu.guhkamer.logeren": "Logeren in de Guhkamer",
    "gui.guhs.menu.guhkamer.logeren.tooltip": "Ga maar lekker logeren! Je guh gaat naar de Guhkamer in je Guhmaag, net als met de Guhbel. "
                                              "Is de kamer vol? Dan blijft hij gezellig bij jou.",
    "gui.guhs.menu.guhkamer.uit": "Uit de logeerkamer",
    "gui.guhs.menu.guhkamer.uit.tooltip": "Klaar met logeren! Je guh komt naar je toe en gaat weer met je mee, net als roepen met de Guhbel. Vahoeg!",
    # the Guhbel screen
    "gui.guhs.guhkamer.bel.titel": "Guhbel",
    "gui.guhs.guhkamer.bel.kamer": "Guhkamer %s x %s · %s/%s logeerguhs",
    "gui.guhs.guhkamer.bel.groei": "Je Guhkamer groeit met elke guh die zielsguh bff 5evr <3 is (nu %s). Er komt dan meer plek bij!",
    "gui.guhs.guhkamer.bel.kop.bij": "Bij jou",
    "gui.guhs.guhkamer.bel.kop.gasten": "In de Guhkamer",
    "gui.guhs.guhkamer.bel.stuur": "Klik: %s gaat logeren in de Guhkamer",
    "gui.guhs.guhkamer.bel.roep": "Klik: tingeling, %s komt naar je toe",
    "gui.guhs.guhkamer.bel.niemand_bij": "Geen eigen guhs in de buurt. Ze moeten binnen 24 blokken zijn.",
    "gui.guhs.guhkamer.bel.niemand_logeert": "Nog niemand logeert in je Guhkamer. Stuur er een guh heen!",
    "gui.guhs.guhkamer.bel.geen_maag": "Je hebt nog geen Guhmaag. De Guhkamer zit in je maag: doe eerst de maagquest!",
    "gui.guhs.guhkamer.bel.woont": "woont in %s",
    # sounds
    "subtitles.guhs.guhkamer.bel": "Guhbel: tingeling",
    "subtitles.guhs.guhkamer.weg": "Guh gaat logeren",
    "subtitles.guhs.guhkamer.terug": "Guh komt terug",
    "subtitles.guhs.guhkamer.deur": "Gordijntje van de Guhkamer",
    "subtitles.guhs.guhkamer.groei": "De Guhkamer groeit",
    # wist-je-datjes
    "gui.guhs.wistjedat.guhkamer.eerste": "Vandaag mocht ik voor het eerst logeren in de Guhkamer. In een maag! Het is er warm en roze en er ligt een kleedje. Heel gezellig, njeg.",
    "gui.guhs.wistjedat.guhkamer.logeren_1": "Ik logeer weer in de Guhkamer. Ik mis mijn baasje een beetje. Een heel klein beetje. Oké, best veel.",
    "gui.guhs.wistjedat.guhkamer.logeren_2": "In de Guhkamer heb ik een dutje gedaan op het roze kleedje. Vahoeg zacht!",
    "gui.guhs.wistjedat.guhkamer.logeren_3": "Soms hoor ik de maag rommelen als ik in de Guhkamer lig. Dat is gewoon een slaapliedje, toch?",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def test_templates(h):
    t = h.Structure((30, 16, 30))
    for x in range(30):
        for z in range(30):
            t.set(x, 0, z, "minecraft:smooth_stone")
    t.save("guhkamer_test_kamer")


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for p in (f"{A}/blockstates/guhkamer_deur.json", f"{A}/models/block/guhkamer_deur_onder.json", f"{A}/models/block/guhkamer_deur_boven.json",
              f"{A}/models/item/guhbel.json", f"{D}/recipe/guhbel.json", os.path.join(h.TEX, "item", "guhbel.png")):
        if not os.path.exists(p):
            missing.append(p)
    for k in ("item.guhs.guhbel", "block.guhs.guhkamer_deur", "gui.guhs.guhkamer.bel.titel"):
        if k not in h.NL:
            missing.append(k)
    for model in (deur_onder(), deur_boven()):
        for e in model["elements"]:
            if min(e["from"]) < -16 or max(e["to"]) > 32:
                missing.append(f"element out of range: {e['from']} {e['to']}")
    if missing:
        raise SystemExit(f"guhkamer assets missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "De Guhkamer in je Guhmaag")
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("guhkamer_guhbel", "De Guhbel", "Maak een &dGuhbel&r: goud, roze wol en een kaasknabbel. Rechtsklik ermee: je ziet je guhs in de buurt en "
      "de logeerguhs in je &dGuhkamer&r. Tingeling!", "guhs:guhbel", [item("guhs:guhbel")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle")
    q("guhkamer_logeren", "Ga maar lekker logeren", "Stuur een guh met de Guhbel naar de &dGuhkamer&r: een logeerkamer in je Guhmaag. Daar "
      "wacht hij veilig op je als hij niet mee kan. Met de Guhbel roep je hem weer terug, waar je ook bent.", "guhs:guhbel",
      [adv("guhs:lieve_vadsjes/guhkamer_bel")], rewards=(("guhs:kaas_knabbels", 12),))
    q("guhkamer_binnen", "Logeerpartijtje", "In je Guhmaag staat een deur met een roze gordijntje en guhoortjes: loop erdoor en je staat in je "
      "&dGuhkamer&r. Zet er Guhhuisjes en speelgoed neer: dat werkt daar net als buiten.", "guhs:guhhuisje_klein",
      [adv("guhs:lieve_vadsjes/guhkamer_binnen")], rewards=(("guhs:pluizige_tunnel", 4),))
    q("guhkamer_groei", "Een kamer met uitzicht", "Je Guhkamer groeit met elke guh die &6zielsguh bff 5evr <3&r is: groter, hoger en plek "
      "voor meer logeerguhs.", "minecraft:nether_star", [adv("guhs:lieve_vadsjes/guhkamer_groei")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), shape="gear", xp=300)
    q("guhkamer_bezoek", "Op visite", "Is de maag van een vriend openbaar? Dan mag je ook door zijn Guhkamer-deur en zijn logeerguhs "
      "bewonderen. (Aaien en bouwen mag alleen de eigenaar.)", "minecraft:cake", [adv("guhs:lieve_vadsjes/guhkamer_bezoek")],
      rewards=(("guhs:kaas_knabbels", 8),))
