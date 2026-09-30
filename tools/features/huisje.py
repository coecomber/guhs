"""
Het Guhhuisje (2.10 "Lieve vadsjes van elkaar", fundament; see guhs_work210/CONTRACT_210.md par. 4.10, 5.1).

A little house shaped like a guh HEAD (NOT a hamster house): the fluffy ears are the roof, the two windows are its big
shiny eyes, the snoet is the door (with a little heart knob) and the nose sits right above it. Three sizes:
  - guhhuisje_klein   2x2x2 blocks, 3 residents
  - guhhuisje_medium  3x3x3 blocks, 5 residents (flower boxes under the eyes)
  - guhhuisje_groot   4x4x4 blocks, 8 residents (flower boxes, a pink chimney and a bow on its ear)
One model per size (models/block/guhhuisje_<maat>_model.json, 32 model pixels wide) that HuisjeRenderer scales up to the
footprint; the blocks themselves only carry a particle texture (the controller is drawn by its renderer, the invisible
guhhuisje_deel parts by nothing). Plus textures, item icons, recipes, loot, sounds, texts, advancements, FTB quests and
the game test room.
"""
import os
import random

import numpy as np
from PIL import Image, ImageDraw

MATEN = ["klein", "medium", "groot"]
PLEKKEN = {"klein": 3, "medium": 5, "groot": 8}
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# =====================================================================================================================
# textures
# =====================================================================================================================
VACHT = (246, 168, 200)
VACHT_DONKER = (226, 138, 176)
LICHT = (255, 214, 230)
OOR = (255, 150, 196)
NEUS = (214, 86, 138)


def _ruis(size, basis, var, seed, strepen=None):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    base = np.array(basis, float)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(base[c] + n, 0, 255)
    a[..., 3] = 255
    if strepen:
        kleur, kans = strepen
        for _ in range(int(size * size * kans)):
            x, y = rng.integers(0, size), rng.integers(0, size - 2)
            for d in range(rng.integers(2, 4)):
                if y + d < size:
                    a[y + d, (x + d // 2) % size, :3] = kleur
    return a


def _vacht():
    return Image.fromarray(_ruis(32, VACHT, 7, 2101, (VACHT_DONKER, 0.06)))


def _vacht_licht():
    return Image.fromarray(_ruis(32, LICHT, 5, 2102, ((244, 196, 214), 0.04)))


def _oor():
    a = _ruis(16, OOR, 4, 2103)
    ys = np.arange(16)[:, None]
    a[..., :3] = np.clip(a[..., :3].astype(int) - (15 - ys) * 1.2, 0, 255).astype(np.uint8)
    return Image.fromarray(a)


def _pluis():
    a = _ruis(16, (252, 196, 220), 9, 2104, ((255, 232, 242), 0.25))
    return Image.fromarray(a)


def _oog():
    """A window that is a big shiny guh eye: dark glass with two highlights, a faint window cross, rounded corners."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, 15, 15], fill=(44, 22, 44, 255))
    d.ellipse([1, 1, 14, 14], fill=(70, 34, 68, 255))
    d.ellipse([3, 4, 14, 15], fill=(58, 28, 60, 255))
    d.line([(8, 1), (8, 14)], fill=(96, 60, 96, 255))
    d.line([(1, 8), (14, 8)], fill=(96, 60, 96, 255))
    d.ellipse([3, 2, 7, 6], fill=(255, 255, 255, 255))
    d.rectangle([10, 10, 11, 11], fill=(236, 226, 255, 255))
    d.point((12, 4), fill=(200, 180, 230, 255))
    return img


def _deur():
    """The snoet with the door: light pink, an arched rosewood door with a heart knob and a little heart window."""
    a = _ruis(16, LICHT, 4, 2105)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.rectangle([4, 7, 11, 15], fill=(176, 92, 110, 255))
    d.ellipse([4, 3, 11, 10], fill=(176, 92, 110, 255))
    d.rectangle([5, 7, 10, 15], fill=(200, 116, 132, 255))
    d.ellipse([5, 4, 10, 9], fill=(200, 116, 132, 255))
    for y in (9, 12):
        d.line([(5, y), (10, y)], fill=(180, 98, 116, 255))
    for x, y in ((6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7)):       # a little heart window
        img.putpixel((x, y), (255, 214, 236, 255))
    img.putpixel((7, 5), (255, 214, 236, 255))
    img.putpixel((9, 5), (255, 214, 236, 255))
    img.putpixel((6, 5), (255, 214, 236, 255))
    img.putpixel((8, 5), (200, 116, 132, 255))
    for x, y in ((9, 11), (10, 11), (9, 12)):                               # the heart knob
        img.putpixel((x, y), (255, 226, 120, 255))
    return img


def _neus():
    a = _ruis(16, NEUS, 5, 2106)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 7, 6], fill=(240, 150, 190, 255))
    return img


def _blos():
    a = _ruis(16, (250, 128, 170), 6, 2107)
    return Image.fromarray(a)


def _bloembak():
    a = _ruis(16, (190, 120, 90), 6, 2108)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 5], fill=(98, 176, 96, 255))
    rng = random.Random(2108)
    for x in range(1, 15, 3):
        c = rng.choice([(255, 236, 120), (255, 150, 200), (255, 255, 255), (200, 160, 255)])
        d.rectangle([x, 1, x + 1, 2], fill=c + (255,))
    for y in (8, 12):
        d.line([(0, y), (15, y)], fill=(170, 100, 76, 255))
    return img


def _schoorsteen():
    a = _ruis(16, (222, 120, 150), 6, 2109)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        d.line([(0, y), (15, y)], fill=(250, 214, 226, 255))
        off = 0 if (y // 4) % 2 == 0 else 4
        for x in range(off, 16, 8):
            d.line([(x, y), (x, y + 3)], fill=(250, 214, 226, 255))
    return img


def _strik():
    a = _ruis(16, (236, 70, 120), 6, 2110)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.rectangle([6, 0, 9, 15], fill=(255, 150, 190, 255))
    return img


def _icoon(maat):
    """The item / quest picture: a pink guh head with ears, eye windows and the snoet door (16x16)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rand = (150, 60, 100, 255)
    # ears
    for x0 in (1, 10):
        d.ellipse([x0, 0, x0 + 5, 6], fill=rand)
        d.ellipse([x0 + 1, 1, x0 + 4, 5], fill=VACHT + (255,))
        d.ellipse([x0 + 2, 2, x0 + 3, 4], fill=OOR + (255,))
    # head
    d.ellipse([0, 3, 15, 15], fill=rand)
    d.ellipse([1, 4, 14, 14], fill=VACHT + (255,))
    d.rectangle([1, 9, 14, 14], fill=VACHT + (255,))
    d.line([(1, 15), (14, 15)], fill=rand)
    # eyes (windows)
    for x0 in (3, 10):
        d.rectangle([x0, 7, x0 + 2, 9], fill=(52, 26, 52, 255))
        img.putpixel((x0, 7), (255, 255, 255, 255))
    # snoet + door
    d.rectangle([5, 10, 10, 14], fill=LICHT + (255,))
    d.rectangle([6, 11, 9, 14], fill=(200, 116, 132, 255))
    img.putpixel((7, 10), NEUS + (255,))
    img.putpixel((8, 10), NEUS + (255,))
    img.putpixel((9, 13), (255, 226, 120, 255))
    # blush
    img.putpixel((2, 11), (250, 120, 166, 255))
    img.putpixel((13, 11), (250, 120, 166, 255))
    if maat in ("medium", "groot"):
        for x in (2, 3, 4, 11, 12, 13):
            img.putpixel((x, 10), (98, 176, 96, 255))
        img.putpixel((3, 10), (255, 236, 120, 255))
        img.putpixel((12, 10), (255, 150, 200, 255))
    if maat == "groot":
        d.rectangle([12, 0, 13, 3], fill=(222, 120, 150, 255))
        img.putpixel((2, 1), (236, 70, 120, 255))
        img.putpixel((4, 1), (236, 70, 120, 255))
        img.putpixel((3, 2), (255, 150, 190, 255))
    return img


TEXTURES = {"vacht": _vacht, "vacht_licht": _vacht_licht, "oor": _oor, "pluis": _pluis, "oog": _oog, "deur": _deur, "neus": _neus,
            "blos": _blos, "bloembak": _bloembak, "schoorsteen": _schoorsteen, "strik": _strik}


def textures(h):
    for name, paint in TEXTURES.items():
        h.save(paint(), "block", f"guhhuisje_{name}.png")
    for maat in MATEN:
        h.save(_icoon(maat), "item", f"guhhuisje_{maat}.png")


# =====================================================================================================================
# the guh-head models (model pixels; the front, with the snoet door, faces north = low z)
# =====================================================================================================================
def _el(frm, to, tex, faces="all", front=None):
    """An element with every face uv 0..16 (the textures stretch over the face). front: another texture for the north face."""
    names = ["north", "east", "south", "west", "up", "down"] if faces == "all" else faces
    f = {}
    for n in names:
        f[n] = {"uv": [0, 0, 16, 16], "texture": f"#{front if (front and n == 'north') else tex}"}
    return {"from": [float(v) for v in frm], "to": [float(v) for v in to], "faces": f}


def _model(maat):
    els = [
        # the head (pink fur), rounded: two crossed cores, the cheeks bulging out, a narrower top layer and a dome
        _el([-6, 0, -2], [22, 20, 20], "vacht"),
        _el([-4, 0, -4], [20, 20, 22], "vacht"),
        _el([-7, 2, 0], [23, 17, 18], "vacht", ["east", "west", "up", "down"]),
        _el([-5, 20, -2], [21, 22, 20], "vacht"),
        _el([-3, 20, -3], [19, 22, 21], "vacht", ["north", "south", "up"]),
        _el([-2, 22, 0], [18, 24.5, 18], "vacht"),
        # the ears: the roof (big and round, soft pink inside at the front)
        _el([-6, 21.5, 4], [4, 30, 12], "vacht"),
        _el([-5, 30, 5], [3, 32, 11], "vacht"),
        _el([-4.5, 23, 3.5], [2.5, 29.5, 4], "oor", ["north"]),
        _el([12, 21.5, 4], [22, 30, 12], "vacht"),
        _el([13, 30, 5], [21, 32, 11], "vacht"),
        _el([13.5, 23, 3.5], [20.5, 29.5, 4], "oor", ["north"]),
        # the pluis between the ears
        _el([4, 24.5, 3], [12, 27, 12], "pluis"),
        _el([6, 27, 5], [10, 28.5, 10], "pluis"),
        # the eyes: two big round windows
        _el([-1, 9, -4.6], [7, 18, -4], "oog", ["north"]),
        _el([9, 9, -4.6], [17, 18, -4], "oog", ["north"]),
        # the snoet with the door, the nose on top
        _el([2, 0, -7], [14, 8.5, -4], "vacht_licht", ["east", "west", "up", "north"], front="deur"),
        _el([6, 8.5, -6.2], [10, 11, -4], "neus", ["north", "east", "west", "up"]),
        # blush on the cheeks
        _el([-3.5, 5, -4.3], [0.5, 7.5, -4], "blos", ["north"]),
        _el([15.5, 5, -4.3], [19.5, 7.5, -4], "blos", ["north"]),
    ]
    if maat in ("medium", "groot"):   # flower boxes under the eyes
        els.append(_el([-2, 6.5, -6.5], [7, 8.5, -4], "bloembak", ["north", "east", "west", "up"]))
        els.append(_el([9, 6.5, -6.5], [18, 8.5, -4], "bloembak", ["north", "east", "west", "up"]))
    if maat == "groot":               # a pink chimney at the back and a bow on the left ear
        els.append(_el([13, 22, 14], [18, 30, 19], "schoorsteen"))
        els.append(_el([-7, 26.5, 2.5], [5, 29.5, 3.5], "strik", ["north", "south", "east", "west", "up"]))
    tex = {k: f"guhs:block/guhhuisje_{k}" for k in TEXTURES}
    tex["particle"] = "guhs:block/guhhuisje_vacht"
    return {"render_type": "minecraft:cutout", "ambientocclusion": False, "textures": tex, "elements": els,
            "display": {
                "gui": {"rotation": [25, 200, 0], "translation": [0, -1.5, 0], "scale": [0.34, 0.34, 0.34]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.18, 0.18, 0.18]},
                "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]},
                "head": {"rotation": [0, 180, 0], "translation": [0, 10, 0], "scale": [0.4, 0.4, 0.4]},
                "thirdperson_righthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
                "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
                "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
            }}


def blocks_and_items(h):
    A, D = h.A, h.D
    deeltje = {"textures": {"particle": "guhs:block/guhhuisje_vacht"}}
    for maat in MATEN:
        name = f"guhhuisje_{maat}"
        h.w(f"{A}/models/block/{name}_model.json", _model(maat))
        h.w(f"{A}/models/block/{name}.json", deeltje)
        h.w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}"} for f in ROT}})
        # in your hand, on the ground and in item frames the little 3D guh head; in the inventory its pixel picture
        h.w(f"{A}/models/item/{name}.json", {"loader": "neoforge:separate_transforms",
                                              "base": {"parent": f"guhs:block/{name}_model"},
                                              "perspectives": {"gui": {"parent": "minecraft:item/generated",
                                                                       "textures": {"layer0": f"guhs:item/{name}"}}}})
        h.self_drop(name)
    h.w(f"{A}/models/block/guhhuisje_deel.json", deeltje)
    h.w(f"{A}/blockstates/guhhuisje_deel.json", {"variants": {"": {"model": "guhs:block/guhhuisje_deel"}}})
    # recipes: every size needs the one before it; 3.0 (timmerguh): and the Timmerguh's bouwboekje (it stays in the grid), so
    # crafting huisjes comes after his questline "Samen een huisje bouwen" (huisjes that were placed before keep working)
    boek = "guhs:timmerguh_bouwboekje"
    h.shaped("guhhuisje_klein", ["WTW", "WKW", "PDP"], {"W": "minecraft:pink_wool", "K": "guhs:kaas_knabbels", "P": "#minecraft:planks",
                                                          "D": "#minecraft:wooden_doors", "T": boek}, "guhs:guhhuisje_klein")
    h.shaped("guhhuisje_medium", ["WTW", "WHW", "PBP"], {"W": "minecraft:pink_wool", "H": "guhs:guhhuisje_klein", "P": "#minecraft:planks",
                                                           "B": "guhs:block_of_kaasknabbels", "T": boek}, "guhs:guhhuisje_medium")
    h.shaped("guhhuisje_groot", ["TGW", "WHW", "PBP"], {"W": "minecraft:pink_wool", "G": "guhs:guh_kristal", "H": "guhs:guhhuisje_medium",
                                                          "P": "#minecraft:planks", "B": "guhs:block_of_kaasknabbels", "T": boek}, "guhs:guhhuisje_groot")
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:guhhuisje_{m}" for m in MATEN] + ["guhs:guhhuisje_deel"])


# =====================================================================================================================
# sounds, advancements, texts
# =====================================================================================================================
SOUNDS = {
    "huisje.deur": [{"name": "minecraft:block.wooden_door.open", "type": "event", "pitch": 1.45, "volume": 0.7}],
    "huisje.snurk": [{"name": "minecraft:entity.fox.sleep", "type": "event", "pitch": 1.3, "volume": 0.6},
                     {"name": "minecraft:entity.cat.purr", "type": "event", "pitch": 0.8, "volume": 0.5}],
}
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}
QUEST_ADVANCEMENTS = ["huisje_bewoner", "huisje_slapen"]


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    from features import band
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": IMPOSSIBLE})
    band.visible(h, "huisje_gebouwd", "root", "guhs:guhhuisje_klein", "task", "Een huisje met oortjes",
                 "Bouw een Guhhuisje: een huisje in de vorm van een guhhoofd")
    band.visible(h, "huisje_vol", "huisje_gebouwd", "guhs:guhhuisje_medium", "goal", "Vol is vol, njeg!",
                 "Laat een Guhhuisje helemaal vol wonen met guhs en maatjes")
    band.visible(h, "huisje_groot", "huisje_gebouwd", "guhs:guhhuisje_groot", "goal", "Villa Vahoeg",
                 "Bouw het grootste Guhhuisje: plek voor acht vadsige bewoners")


TEXTS = {
    "block.guhs.guhhuisje_klein": "Guhhuisje (klein)",
    "block.guhs.guhhuisje_medium": "Guhhuisje (medium)",
    "block.guhs.guhhuisje_groot": "Guhhuisje (groot)",
    "block.guhs.guhhuisje_deel": "Guhhuisje",
    "block.guhs.guhhuisje.lore": "Plek voor %s bewoners: guhs, muisjes, Schilly en Poepschilly",
    "block.guhs.guhhuisje_klein.lore": "Een knus guhhoofd: de oortjes zijn het dak, de ogen de raampjes, de snoet de deur.",
    "block.guhs.guhhuisje_medium.lore": "Met bloembakjes onder de oogjes. Extra gezellig!",
    "block.guhs.guhhuisje_groot.lore": "Met schoorsteentje en een strik op het oor. Villa Vahoeg!",
    "subtitles.guhs.huisje.deur": "Huisjesdeurtje",
    "subtitles.guhs.huisje.snurk": "Guh snurkt in zijn huisje",
    # messages
    "gui.guhs.huisje.gebouwd": "Je Guhhuisje heet %s! Rechtsklik erop om bewoners en klusjes te kiezen.",
    "gui.guhs.huisje.trekt_in": "%s woont nu in Guhhuisje %s. Welkom thuis, njeg!",
    "gui.guhs.huisje.vol": "Guhhuisje %s zit vol! Bouw er nog een, of een grotere.",
    "gui.guhs.huisje.niet_jouw_guh": "Daar mogen alleen je eigen tamme guhs en maatjes wonen, njeg.",
    "gui.guhs.huisje.niet_van_jou": "Guhhuisje %s is van iemand anders. Niet naar binnen gluren!",
    "gui.guhs.huisje.naam_bezet": "Die naam is leeg, te lang of al van een ander huisje. Verzin iets anders, njeg!",
    # the screen
    "gui.guhs.huisje.titel": "Guhhuisje",
    "gui.guhs.huisje.titel_maat": "%s",
    "gui.guhs.huisje.naam": "Naam van het huisje",
    "gui.guhs.huisje.hernoem": "Hernoem",
    "gui.guhs.huisje.bewoners": "Bewoners: %s",
    "gui.guhs.huisje.opslag.bank": "Spulletjes van klusjes gaan naar de Bank Guh (gesorteerd!)",
    "gui.guhs.huisje.opslag.kist": "Spulletjes van klusjes gaan in de kist naast het huisje",
    "gui.guhs.huisje.opslag.deur": "Zet een kist naast het huisje! Nu komen de spulletjes voor de deur te liggen.",
    "gui.guhs.huisje.kop.bewoners": "Bewoners",
    "gui.guhs.huisje.kop.klusjes": "Klusjes",
    "gui.guhs.huisje.kop.klusjes_van": "Klusjes van %s",
    "gui.guhs.huisje.kop.nieuw": "Wie mag erin wonen?",
    "gui.guhs.huisje.leeg": "Nog niemand woont hier. Klik op Nieuwe bewoner, of rechtsklik het huisje met een opgepakte guh of een maatje.",
    "gui.guhs.huisje.slaapt": "slaapt",
    "gui.guhs.huisje.nieuwe_bewoner": "Nieuwe bewoner",
    "gui.guhs.huisje.nieuwe_bewoner.tooltip": "Je eigen tamme guhs en maatjes die vlakbij zijn (16 blokken) kunnen hier komen wonen.",
    "gui.guhs.huisje.terug_klusjes": "< Klusjes",
    "gui.guhs.huisje.uit_huis": "Uit huis",
    "gui.guhs.huisje.koepel": "Klus-area: %s",
    "gui.guhs.huisje.koepel.tooltip": "Laat klus-area zien: een blauwe koepel over het gebied waar de bewoners rondlopen, klusjes doen en spelen.",
    "gui.guhs.huisje.geen_klusjes": "Nog geen klusjes bekend, njeg! Je bewoners wonen hier gezellig, slapen hier en spelen in de buurt.",
    "gui.guhs.huisje.kies_bewoner": "Kies links een bewoner om zijn klusjes aan of uit te zetten. Dit zijn alle klusjes (en wat ze nodig hebben):",
    "gui.guhs.huisje.baby": "Baby's doen nog geen klusjes: die zijn nog te klein, ze spelen alleen lekker.",
    "gui.guhs.huisje.kan_niet": "kan niet",
    "gui.guhs.huisje.aan": "aan",
    "gui.guhs.huisje.uit": "uit",
    "gui.guhs.huisje.klus_aan": "Klik: dit klusje aan zetten",
    "gui.guhs.huisje.klus_uit": "Klik: dit klusje uit zetten",
    "gui.guhs.huisje.geen_kandidaten": "Er zijn geen eigen tamme guhs of maatjes in de buurt die hier nog niet wonen.",
    "gui.guhs.huisje.trek_in": "Klik: kom hier wonen!",
    "gui.guhs.huisje.verhuis": "Woont nu in %s. Klik: verhuizen!",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# the game test room, the self-check
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((24, 8, 24))
    for x in range(24):
        for z in range(24):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("huisje_test_tuin")


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for maat in MATEN:
        name = f"guhhuisje_{maat}"
        for p in (f"{A}/blockstates/{name}.json", f"{A}/models/item/{name}.json", f"{A}/models/block/{name}_model.json",
                  f"{D}/loot_table/blocks/{name}.json", f"{D}/recipe/{name}.json", os.path.join(h.TEX, "item", f"{name}.png")):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{name}" not in h.NL:
            missing.append(f"lang block.guhs.{name}")
    for k in TEXTURES:
        if not os.path.exists(os.path.join(h.TEX, "block", f"guhhuisje_{k}.png")):
            missing.append(k)
    for maat in MATEN:   # every element inside the model limits
        for e in _model(maat)["elements"]:
            if min(e["from"]) < -16 or max(e["to"]) > 32:
                missing.append(f"element out of range in {maat}: {e['from']} {e['to']}")
    if missing:
        raise SystemExit(f"huisje assets missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "Het Guhhuisje")
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("huisje_bouwen", "Een huisje met oortjes", "Een &dGuhhuisje&r is een huisje in de vorm van een guhhoofd: de oortjes zijn het dak, de "
      "oogjes de raampjes en de snoet is de deur! Je eerste krijg je van de &6Timmerguh&r (op de bouwplaats aan de rand van elk "
      "Knuffeldal-stadje). Met zijn &6bouwboekje&r in je werkbank maak je er zelf meer: roze wol, een kaasknabbel, planken en een deur.",
      "guhs:guhhuisje_klein", [item("guhs:guhhuisje_klein")], rewards=(("guhs:kaas_knabbels", 12),), shape="circle", xp=100)
    q("huisje_bewoner", "Welkom thuis!", "Rechtsklik je huisje en kies &dNieuwe bewoner&r (of rechtsklik het huisje met een opgepakte guh of een "
      "maatje). Guhs, pieppiepmuisjes, Schilly en Poepschilly mogen er allemaal wonen. Het huisje is nu hun thuis: ze blijven in de buurt.",
      "guhs:picked_up_guh", [adv("huisje_bewoner")], rewards=(("guhs:kaas_knabbels", 8),))
    q("huisje_slapen", "Welterusten, guh", "'s Nachts lopen je bewoners door de snoetdeur naar binnen en slapen ze in hun huisje (kijk maar naar "
      "de zzz bij de raampjes). 's Ochtends komen ze gapend weer naar buiten. Wacht een nachtje af!",
      "minecraft:red_bed", [adv("huisje_slapen")], rewards=(("guhs:marshmallow_knabbel", 3),))
    q("huisje_koepel", "Laat klus-area zien", "In het huisje-scherm kun je &9laat klus-area zien&r aanzetten: een blauwe koepel over het gebied "
      "waar je bewoners rondlopen, klusjes doen en spelen. Zo weet je precies waar je tuintjes en kisten moeten staan.",
      "minecraft:light_blue_stained_glass", [{"type": "checkmark"}], rewards=(("guhs:kaas_knabbels", 4),))
    q("huisje_medium", "Een huisje met bloembakjes", "Maak van een klein huisje een &dmedium&r Guhhuisje (met een blok kaasknabbels en het "
      "bouwboekje van de Timmerguh): plek voor "
      "vijf bewoners, en bloembakjes onder de oogjes!", "guhs:guhhuisje_medium", [item("guhs:guhhuisje_medium")],
      rewards=(("guhs:kaas_knabbels", 16),), xp=150)
    q("huisje_groot", "Villa Vahoeg", "Het grootste &dGuhhuisje&r: plek voor acht vadsige bewoners, met een schoorsteentje en een strik op het "
      "oor. Er is een guhkristal voor nodig (en het bouwboekje van de Timmerguh).", "guhs:guhhuisje_groot", [adv("guhs:lieve_vadsjes/huisje_groot")],
      rewards=(("guhs:gefrituurde_kaasknabbels", 4),), shape="gear", xp=300)
    q("huisje_vol", "Vol is vol, njeg!", "Laat een Guhhuisje helemaal vol wonen. Zoveel lieve vadsjes onder een dak!",
      "guhs:guhhuisje_medium", [adv("guhs:lieve_vadsjes/huisje_vol")], rewards=(("guhs:kaas_knabbels", 16),), xp=150)
