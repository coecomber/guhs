"""
De Guhoven (1.2.5; the Java side is feature/guhoven).

A furnace that needs no fuel: it bakes on vadskracht (bbq2: it is a consumer in a vadskracht net, features/vadskracht.py).
Put it next to a Guhrad with a guh in it (any of its 3x3 blocks) or join it to one with Guhdraad and it smelts exactly like a
vanilla furnace (same recipes, same speed, XP, hoppers, comparator). Ordinary redstone (levers, torches, dust, a redstone
block) does nothing: it really wants a running guh. Pink stone with a guh face: two ears at the top, two eyes, a little snoet,
and the oven mouth glows when it bakes. Like every guh machine the face shows how it is doing: asleep without vadskracht,
awake with it, surprised when the result slot is full. This module makes:
  - block guh_oven (front asleep/awake/lit/full, side, top; orientable model with 4 facings x lit x snoet, item model, loot
    table: drops itself, recipe: 8 stone (the furnace tag) around 1 kaasknabbel, mineable/pickaxe, the tag guhs:vadskracht)
  - the screen's guh wheel icon (instead of the fuel slot: off and on)
  - the texts (name, lore, screen) and the FTB quest (next to the Guhrad in "Lekker eten & gezellig thuis")
"""
import math
import os

import numpy as np
from PIL import Image

from features import vadskracht

NAME = "guh_oven"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

STEEN = (222, 158, 182)          # pink kaasknabbel stone
STEEN_DONKER = (176, 108, 138)   # mortar / frame
STEEN_LICHT = (240, 190, 208)
OOR = (255, 186, 214)
OOR_BINNEN = (255, 140, 182)
OOG = (58, 28, 60)
SNOET = (226, 98, 150)
BLOS = (255, 130, 172)


def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _zet(a, x, y, kleur):
    a[y, x, :3] = kleur[:3]
    a[y, x, 3] = 255


def _rand(a):
    """The dark frame around a face (like the vanilla furnace's stone rim)."""
    a[0, :, :3] = STEEN_DONKER
    a[15, :, :3] = STEEN_DONKER
    a[:, 0, :3] = STEEN_DONKER
    a[:, 15, :3] = STEEN_DONKER


# =====================================================================================================================
# textures
# =====================================================================================================================
def _voor(aan, staat="werkt"):
    """The front: the oven mouth (dark, or glowing when it bakes) under the guh face of this state (vadskracht.snoet)."""
    a = _ruis(STEEN, 5, 5301)
    _rand(a)
    # two little ears in the top corners (rounded, a pinker inside)
    for ox in (1, 11):
        for y, xs in ((1, (1, 2)), (2, (0, 1, 2, 3)), (3, (0, 1, 2, 3))):
            for x in xs:
                _zet(a, ox + x, y, OOR)
        for x, y in ((1, 2), (2, 2), (1, 3), (2, 3)):
            _zet(a, ox + x, y, OOR_BINNEN)
    # (the face comes last: eyes with a white shine, blush and a snoet, the standard face of every guh machine)
    # the oven mouth (rows 9..13, columns 4..11, round corners): dark when off, glowing when it bakes
    for y in range(9, 14):
        for x in range(4, 12):
            if (x in (4, 11)) and (y in (9, 13)):
                continue
            if aan:
                t = (y - 9) / 4                           # top: deep pink glow, bottom: hot yellow
                c = (255, int(120 + 110 * t), int(150 - 110 * t))
                if (x + y) % 3 == 0 and y < 12:
                    c = (255, 236, 150)                   # little flame tips
            else:
                c = (52, 26, 42) if y < 12 else (78, 44, 60)
            _zet(a, x, y, c)
    if not aan:
        for x in range(5, 11, 2):                         # a grate in the dark mouth
            _zet(a, x, 12, (110, 74, 92))
    # a lip around the mouth
    for x in range(5, 11):
        _zet(a, x, 8, STEEN_DONKER)
        _zet(a, x, 14, STEEN_DONKER)
    for y in range(10, 13):
        _zet(a, 3, y, STEEN_DONKER)
        _zet(a, 12, y, STEEN_DONKER)
    return vadskracht.snoet(Image.fromarray(a), 2, 5, staat)


def _zij():
    a = _ruis(STEEN, 6, 5302)
    for y in (3, 7, 11):                                  # pink bricks
        a[y, :, :3] = STEEN_DONKER
    for rij, y0 in enumerate((0, 4, 8, 12)):
        for x in ((5, 13) if rij % 2 == 0 else (1, 9)):
            for y in range(y0, min(y0 + 3, 16)):
                a[y, x, :3] = STEEN_DONKER
    _rand(a)
    # a little heart on the side
    for y, xs in ((5, (6, 9)), (6, (5, 6, 7, 8, 9, 10)), (7, (6, 7, 8, 9)), (8, (7, 8))):
        for x in xs:
            a[y, x, :3] = SNOET
            a[y, x, 3] = 255
    return Image.fromarray(a)


def _boven():
    a = _ruis(STEEN_LICHT, 4, 5303)
    _rand(a)
    for ox in (2, 10):                                    # the ears seen from above (at the front edge)
        for x in range(ox, ox + 4):
            for y in (1, 2):
                _zet(a, x, y, OOR)
        _zet(a, ox + 1, 2, OOR_BINNEN)
        _zet(a, ox + 2, 2, OOR_BINNEN)
    return Image.fromarray(a)


def _wiel(aan, size=24):
    """The screen icon where a furnace has its fuel slot: a little guh wheel (grey-pink when off, bright when it runs)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    rand = (255, 92, 184, 255) if aan else (150, 120, 134, 255)
    spaak = (255, 160, 205, 255) if aan else (176, 156, 166, 255)
    for y in range(size):
        for x in range(size):
            d = math.dist((x, y), (c, c))
            if 9.2 <= d <= 11.4:
                px[x, y] = rand
    for k in range(6):                                    # six spokes
        ang = k * math.pi / 3 + math.pi / 6
        for r in np.arange(1.5, 9.4, 0.25):
            px[int(round(c + r * math.cos(ang))), int(round(c + r * math.sin(ang)))] = spaak
    for y in range(size):
        for x in range(size):
            d = math.dist((x, y), (c, c))
            if d <= 1.6:
                px[x, y] = rand
    # a little guh running at the bottom of the wheel
    lijf = (255, 186, 214, 255) if aan else (190, 170, 180, 255)
    for y in range(13, 19):
        for x in range(8, 16):
            if math.dist((x, y), (11.5, 16)) <= 3.6:
                px[x, y] = lijf
    oog = (58, 28, 60, 255)
    px[10, 15] = oog
    px[13, 15] = oog
    px[9, 12] = px[14, 12] = lijf                         # ears
    if aan:
        for x, y in ((3, 3), (20, 4), (21, 19), (2, 20)):  # sparkles
            px[x, y] = (255, 240, 250, 255)
    return img


def textures(h):
    h.save(_voor(False), "block", f"{NAME}_front.png")
    h.save(_voor(True), "block", f"{NAME}_front_on.png")
    h.save(_voor(False, "slaapt"), "block", f"{NAME}_front_slaapt.png")
    h.save(_voor(False, "vol"), "block", f"{NAME}_front_vol.png")
    h.save(_zij(), "block", f"{NAME}_side.png")
    h.save(_boven(), "block", f"{NAME}_top.png")
    h.save(_wiel(False), "gui", f"{NAME}_wiel.png")
    h.save(_wiel(True), "gui", f"{NAME}_wiel_aan.png")


# =====================================================================================================================
# model, blockstate, item, loot, recipe, tags
# =====================================================================================================================
def blocks_and_items(h):
    A = h.A
    t = lambda k: f"guhs:block/{NAME}_{k}"
    for model, front in ((NAME, "front"), (f"{NAME}_on", "front_on"), (f"{NAME}_slaapt", "front_slaapt"), (f"{NAME}_vol", "front_vol")):
        h.w(f"{A}/models/block/{model}.json", {"parent": "minecraft:block/orientable",
                                                "textures": {"front": t(front), "side": t("side"), "top": t("top")}})
    # lit (it bakes) wins; else the face says it: asleep without vadskracht, surprised when the result slot is full
    achter = lambda lit, snoet: "_on" if lit == "true" else {"slaapt": "_slaapt", "werkt": "", "vol": "_vol"}[snoet]
    h.w(f"{A}/blockstates/{NAME}.json", {"variants": {
        f"facing={f},lit={lit},snoet={snoet}": {"model": f"guhs:block/{NAME}{achter(lit, snoet)}", **({"y": r} if r else {})}
        for f, r in ROT.items() for lit in ("false", "true") for snoet in vadskracht.STATEN}})
    h.w(f"{A}/models/item/{NAME}.json", {"parent": f"guhs:block/{NAME}"})
    h.self_drop(NAME)
    # as cheap as a furnace: 8 stone (the same tag as the furnace: cobblestone, blackstone, cobbled deepslate) around a kaasknabbel
    h.shaped(NAME, ["SSS", "SKS", "SSS"], {"S": "#minecraft:stone_crafting_materials", "K": "guhs:kaas_knabbels"}, f"guhs:{NAME}", 1)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{NAME}"])


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    f"block.guhs.{NAME}": "Guhoven",
    f"block.guhs.{NAME}.lore": "Geen kolen of houtskool nodig: hij bakt op vadskracht! Zet hem naast een Guhrad waar een guh in rent, "
                               "of sluit hem aan met Guhdraad. Gewone redstone vindt hij niks.",
    f"block.guhs.{NAME}.lore.lief": "Het guhtje rent in het rad, en de oven bakt de knabbels. Samen vadsig, njeg!",
    f"gui.guhs.{NAME}.aan": "Vadskracht! De oven bakt.",
    f"gui.guhs.{NAME}.uit": "De oven bakt niet. Hij heeft vadskracht nodig: zet hem naast een Guhrad waar een guh in rent, of sluit "
                            "hem aan met Guhdraad (en doe er iets in om te bakken). Kijk naar de oven en je leest of je opstelling "
                            "genoeg vadskracht heeft.",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def selfcheck(h):
    A, D = h.A, h.D
    missing = [p for p in (f"{A}/blockstates/{NAME}.json", f"{A}/models/block/{NAME}.json", f"{A}/models/block/{NAME}_on.json",
                           f"{A}/models/block/{NAME}_slaapt.json", f"{A}/models/block/{NAME}_vol.json",
                           f"{A}/models/item/{NAME}.json", f"{D}/loot_table/blocks/{NAME}.json", f"{D}/recipe/{NAME}.json")
               if not os.path.exists(p)]
    missing += [k for k in ("block/guh_oven_front", "block/guh_oven_front_on", "block/guh_oven_front_slaapt", "block/guh_oven_front_vol",
                            "block/guh_oven_side", "block/guh_oven_top",
                            "gui/guh_oven_wiel", "gui/guh_oven_wiel_aan")
                if not os.path.exists(os.path.join(h.TEX, *k.split("/")) + ".png")]
    missing += [k for k in TEXTS if k not in h.NL]
    if missing:
        raise SystemExit(f"guhoven: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quest: in the section "Lekker eten & gezellig thuis" of Guhs & basis, right after the Guhrad
# =====================================================================================================================
def ftb(fq):
    fq.q("guh_oven", "Bakken op vadskracht", "Een &dGuhoven&r (8 keisteen om een kaasknabbel) heeft geen kolen nodig: hij bakt op "
         "&dvadskracht&r! Zet hem naast een &dGuhrad&r waar een guh in rent, of sluit hem aan met &dGuhdraad&r. Kijk naar de oven "
         "en je leest hoeveel vadskracht je opstelling gebruikt: vraagt die meer dan je guhs bij elkaar rennen, dan staat alles "
         "stil. Gewone redstone vindt hij niks. Het guhtje rent, de oven bakt. Njeg!",
         f"guhs:{NAME}", [fq.item(f"guhs:{NAME}")], rewards=(("guhs:kaas_knabbels", 8),), deps=["wheel"], xp=50)
