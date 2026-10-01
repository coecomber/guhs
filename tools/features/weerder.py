"""
De Wilde-guhweerder (1.2.0; the Java side is feature/weerder).

A little pink sign on a wooden post, with two fluffy guh ears and a sleeping guh face on it ("hier woont al een vadsje"). Put it
in your house or base: no WILD guhs spawn in the area around it (8/16/24/32/48 blocks, chosen with right-click; the screen can
show the area as the same blue dome as the Guhhuisje's klus-area). Still lief: the wild guhs know a vadsje already lives here,
so they go cuddle somewhere else. This module makes:
  - block wilde_guhweerder (model with 4 facings, textures, item model, loot table: drops itself, recipe, mineable/axe)
  - the texts (name, lore, screen) and the FTB quest (in the section "Het Guhhuisje", added from huisje.ftb)
"""
import os

import numpy as np
from PIL import Image, ImageDraw

NAME = "wilde_guhweerder"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

ROZE = (246, 168, 200)
ROZE_DONKER = (214, 120, 162)
LICHT = (255, 222, 236)
HOUT = (150, 96, 70)
OOG = (58, 28, 60, 255)


def _ruis(basis, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    n = rng.normal(0, var, (size, size))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return a


# =====================================================================================================================
# textures (the board face uses rows 4..11, columns 1..14: the model maps uv [1, 4, 15, 12] onto the 14 x 8 board)
# =====================================================================================================================
def _bord(voor):
    img = Image.fromarray(_ruis(LICHT, 4, 4201 if voor else 4202))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 4, 14, 11], outline=ROZE_DONKER + (255,))
    if voor:
        # a sleeping guh: closed eyes (little arcs), a pink snoet, blush
        for ex in (3, 9):
            for x, y in ((ex, 7), (ex + 1, 8), (ex + 2, 8), (ex + 3, 7)):
                img.putpixel((x, y), OOG)
        img.putpixel((7, 9), (226, 98, 150, 255))
        img.putpixel((8, 9), (226, 98, 150, 255))
        for x in (3, 12):
            img.putpixel((x, 9), (255, 160, 190, 255))
    else:
        # the back: a little heart in the middle
        hart = (226, 98, 150, 255)
        for y, xs in ((6, (5, 6, 9, 10)), (7, range(5, 11)), (8, range(6, 10)), (9, (7, 8))):
            for x in xs:
                img.putpixel((x, y), hart)
        img.putpixel((6, 6), (255, 196, 220, 255))
    return img


def _rand():
    a = _ruis(ROZE, 6, 4203)
    for y in (0, 15):
        a[y, :, :3] = ROZE_DONKER
    return Image.fromarray(a)


def _paal():
    a = _ruis(HOUT, 7, 4204)
    for x in (4, 11):
        a[:, x, :3] = np.clip(a[:, x, :3].astype(int) - 20, 0, 255)
    return Image.fromarray(a)


def _oor():
    a = _ruis(ROZE, 5, 4205)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 5, 11, 15], fill=(255, 176, 206, 255))
    d.ellipse([5, 7, 10, 15], fill=(255, 196, 220, 255))
    return img


def _voet():
    a = _ruis((118, 168, 92), 9, 4206)
    rng = np.random.default_rng(4207)
    for _ in range(18):
        x, y = rng.integers(0, 16), rng.integers(0, 16)
        a[y, x, :3] = (255, 186, 214)   # little pink flowers in the grass tuft
    return Image.fromarray(a)


def textures(h):
    h.save(_bord(True), "block", f"{NAME}_bord.png")
    h.save(_bord(False), "block", f"{NAME}_achter.png")
    h.save(_rand(), "block", f"{NAME}_rand.png")
    h.save(_paal(), "block", f"{NAME}_paal.png")
    h.save(_oor(), "block", f"{NAME}_oor.png")
    h.save(_voet(), "block", f"{NAME}_voet.png")


# =====================================================================================================================
# model, blockstate, item, loot, recipe, tags
# =====================================================================================================================
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    return {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for f in faces}}


def model():
    bord = {"from": [1, 6, 7], "to": [15, 14, 9], "faces": {
        "north": {"texture": "#bord", "uv": [1, 4, 15, 12]}, "south": {"texture": "#achter", "uv": [1, 4, 15, 12]},
        "east": {"texture": "#rand", "uv": [7, 0, 9, 8]}, "west": {"texture": "#rand", "uv": [7, 0, 9, 8]},
        "up": {"texture": "#rand", "uv": [1, 7, 15, 9]}, "down": {"texture": "#rand", "uv": [1, 7, 15, 9]}}}
    els = [
        _el([5, 0, 5], [11, 1, 11], "#voet"),                                                          # a grass tuft with flowers
        _el([7, 1, 7], [9, 6, 9], "#paal", faces=("north", "south", "east", "west")),                  # the post
        bord,
        _el([2.5, 14, 7.5], [5.5, 16, 8.5], "#oor", faces=("north", "south", "east", "west", "up"), uv=[4, 5, 11, 15]),
        _el([10.5, 14, 7.5], [13.5, 16, 8.5], "#oor", faces=("north", "south", "east", "west", "up"), uv=[4, 5, 11, 15]),
    ]
    t = lambda k: f"guhs:block/{NAME}_{k}"
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": t("rand"), "bord": t("bord"), "achter": t("achter"), "rand": t("rand"), "paal": t("paal"),
                         "oor": t("oor"), "voet": t("voet")},
            "elements": els}


def blocks_and_items(h):
    A = h.A
    h.w(f"{A}/models/block/{NAME}.json", model())
    h.w(f"{A}/blockstates/{NAME}.json", {"variants": {
        f"facing={f}": {"model": f"guhs:block/{NAME}", **({"y": r} if r else {})} for f, r in ROT.items()}})
    h.w(f"{A}/models/item/{NAME}.json", {"parent": f"guhs:block/{NAME}"})
    h.self_drop(NAME)
    h.shaped(NAME, ["WWW", "WKW", " S "], {"W": "minecraft:pink_wool", "K": "guhs:kaas_knabbels", "S": "minecraft:stick"}, f"guhs:{NAME}", 1)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{NAME}"])


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {
    f"block.guhs.{NAME}": "Wilde-guhweerder",
    f"block.guhs.{NAME}.lore": "Zet hem in je huis of basis: in de area eromheen komen geen wilde guhs meer tevoorschijn. "
                               "Rechtsklik: kies hoe groot de area is en laat hem zien als een blauwe koepel.",
    f"block.guhs.{NAME}.lore.lief": "Heel lief hoor, njeg! Wilde guhs zien het bordje en weten: hier woont al een vadsje. "
                                    "Dan gaan ze gewoon ergens anders knuffelen.",
    "gui.guhs.weerder.uitleg": "Binnen deze area komen geen wilde guhs tevoorschijn. Ze zien het bordje en weten: hier woont al een "
                               "vadsje! Je eigen guhs, baby'tjes en guhs uit een spawn-ei mogen gewoon komen, en guhs die er al zijn "
                               "blijven lekker zitten.",
    "gui.guhs.weerder.straal": "Area: %s blokken om het bordje heen",
    "gui.guhs.weerder.straal.tooltip": "Geen wilde guhs binnen %s blokken van het bordje (ook een stukje erboven en eronder).",
    "gui.guhs.weerder.lief": "Lief hoor: de wilde guhs gaan gewoon ergens anders knuffelen. Njeg!",
    "gui.guhs.weerder.koepel": "Laat de area zien: %s",
    "gui.guhs.weerder.koepel.tooltip": "Een doorzichtige blauwe koepel over de area waar geen wilde guhs tevoorschijn komen. "
                                       "Alleen jij ziet hem.",
    "gui.guhs.weerder.van_wie": "Dit is de Wilde-guhweerder van %s",
    "gui.guhs.weerder.geplaatst": "Bordje staat! Wilde guhs weten nu dat hier al een vadsje woont (%s blokken om het bordje heen). "
                                  "Rechtsklik om de area te kiezen.",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def selfcheck(h):
    A, D = h.A, h.D
    missing = [p for p in (f"{A}/blockstates/{NAME}.json", f"{A}/models/block/{NAME}.json", f"{A}/models/item/{NAME}.json",
                           f"{D}/loot_table/blocks/{NAME}.json", f"{D}/recipe/{NAME}.json") if not os.path.exists(p)]
    missing += [k for k in ("bord", "achter", "rand", "paal", "oor", "voet")
                if not os.path.exists(os.path.join(h.TEX, "block", f"{NAME}_{k}.png"))]
    missing += [k for k in TEXTS if k not in h.NL]
    if missing:
        raise SystemExit(f"weerder: missing {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    texts(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quest: in the section "Het Guhhuisje" (huisje.ftb calls this, so it lands next to the huisje quests)
# =====================================================================================================================
def ftb_huisje(fq):
    fq.q("wilde_guhweerder", "Hier woont al een vadsje", "Zet een &dWilde-guhweerder&r in je huis of basis (roze wol, een kaasknabbel "
         "en een stokje). Wilde guhs zien het bordje en weten: hier woont al een vadsje! Dan gaan ze lief ergens anders knuffelen. "
         "Rechtsklik het bordje om de area te kiezen en hem als &9blauwe koepel&r te laten zien. Je eigen guhs mogen gewoon blijven.",
         f"guhs:{NAME}", [fq.item(f"guhs:{NAME}")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
