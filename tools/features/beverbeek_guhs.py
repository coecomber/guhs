"""
1.6.2: the guhs of the Beverbeek (Java: nl.juiced.guhs.feature.beverbeek.BeverGuhs).

  - de Beverguh (GuhVariant.BEVERGUH, bones "bever_*"): chestnut-brown fur, a lighter muzzle, two orange buck teeth and a
    flat scaly paddle tail. Like the biomes3 guhs its bones add NO swatch of their own: they sample swatches that already
    exist for other variants (SWATCH), and only guh_beverguh.png paints them.
  - the knaagdier outfit (GuhClothes.KNAAGDIER_MUTS / KNAAGDIER_STAART, bones "outfit_knaagdier_*"): a brown beaver hood
    with round ears and buck teeth, and a flat beaver tail. Beverbeek guhs wear it sometimes; also craftable.

make_guh_variants.py picks up BONES, variants() and clothes(); make_clothes_icons.py picks up icons(); build(h) writes the
texts, the item models, the recipes and the Beverguh's sleeping texture (after make_guh_variants made guh_beverguh.png).
English: tools/lang/en/c188_beverbeek_guhs.json.
"""
import os

import numpy as np
from PIL import Image

_H = [0, 6, -2]            # the guh's head pivot
_STAART = [0, 1.5, 12]     # its tail pivot
SWATCH = {"bever_staart": "koning_manen", "bever_tanden": "koning_goud"}

_PEDDEL = [([-2.6, 2.6, 13.0], [5.2, 0.9, 6.4], 0), ([-2.1, 2.6, 19.4], [4.2, 0.9, 1.0], 0)]
_TANDEN = [([-1.15, 4.05, -12.55], [1.05, 1.5, 0.45], 0), ([0.1, 4.05, -12.55], [1.05, 1.5, 0.45], 0)]

BONES = {
    # the Beverguh (variant bones: hidden on every other guh)
    "bever_staart": ("tail", _STAART, SWATCH["bever_staart"], _PEDDEL),
    "bever_tanden": ("head", _H, SWATCH["bever_tanden"], _TANDEN),
    # the knaagdier outfit (own swatches)
    "outfit_knaagdier_muts": ("head", _H, "knaagdier_muts", [([-7.3, 12.9, -12.3], [14.6, 1.5, 12.4], 0.05),
                                                             ([-7.3, 4.0, -0.4], [14.6, 9.0, 0.9], 0.05)]),
    "outfit_knaagdier_oren": ("head", _H, "knaagdier_oren", [([2.6, 14.3, -7.4], [2.8, 2.4, 1.4], 0), ([-5.4, 14.3, -7.4], [2.8, 2.4, 1.4], 0)]),
    "outfit_knaagdier_tandjes": ("head", _H, "knaagdier_tandjes", [([-1.3, 3.9, -12.7], [1.2, 1.7, 0.5], 0), ([0.1, 3.9, -12.7], [1.2, 1.7, 0.5], 0)]),
    "outfit_knaagdier_staart": ("tail", _STAART, "knaagdier_staart", [([-2.7, 2.5, 12.6], [5.4, 1.0, 6.8], 0.05),
                                                                      ([-2.2, 2.5, 19.4], [4.4, 1.0, 1.0], 0.05)]),
}
BEVER_VACHT = (128, 84, 52)
BEVER_DONKER = (74, 56, 46)
TAND = (240, 150, 60)
CLOTHES = ["knaagdier_muts", "knaagdier_staart"]


def _schubben(v, rng, kleur):
    """The scaly paddle: dark leathery squares in rows, offset like a beaver's tail."""
    a = v.fabric(kleur, rng, 6)
    px = a.shape[0]
    for y in range(0, px, 4):
        a[y, :] = a[y, :] * 0.78
        for x in range((y // 4 % 2) * 2, px, 4):
            a[y:y + 4, x] = a[y:y + 4, x] * 0.82
    return np.clip(a, 0, 255)


def _tand(v, rng):
    a = v.fabric(TAND, rng, 5)
    px = a.shape[0]
    a[:, px // 2 - 1:px // 2 + 1] = a[:, px // 2 - 1:px // 2 + 1] * 0.85    # the gap between the two teeth
    a[:2, :] = (252, 196, 120)                                                  # a light tip
    return np.clip(a, 0, 255)


def variants(rng, v):
    """The Beverguh's first texture: the fur and the swatches its bones sample (make_guh_variants)."""
    return {"beverguh": (BEVER_VACHT, {SWATCH["bever_staart"]: lambda: _schubben(v, rng, BEVER_DONKER),
                                      SWATCH["bever_tanden"]: lambda: _tand(v, rng)})}


def clothes(rng, v):
    def muts():
        a = v.fabric((140, 96, 60), rng, 12)
        px = a.shape[0]
        for _ in range(40):                                   # fluffy strands
            x, y = rng.integers(0, px - 1), rng.integers(0, px - 3)
            a[y:y + 3, x] = np.clip(a[y:y + 3, x] + 16, 0, 255)
        return a

    def oren():
        a = v.fabric((140, 96, 60), rng, 10)
        px = a.shape[0]
        a[px // 4:3 * px // 4, px // 4:3 * px // 4] = (196, 150, 120)   # the inside of the ear
        return a

    return {
        "knaagdier_muts": {"knaagdier_muts": muts, "knaagdier_oren": oren, "knaagdier_tandjes": lambda: _tand(v, rng)},
        "knaagdier_staart": {"knaagdier_staart": lambda: _schubben(v, rng, BEVER_DONKER)},
    }


def icons(ic):
    muts = ["...aa....aa.....", "..abba..abba....", "..aaaaaaaaaa....", ".abbbbbbbbbba...", ".abbbbbbbbbba...",
            ".abbbbbbbbbba...", ".abbbbccbbbba...", "..abbbccbbba....", "...aaaccaaa.....", "......cc........"]
    staart = ["................", "....aaaaaa......", "...abdbdbdba....", "..abdbdbdbdba...", "..abbdbdbdbda...",
              "..abdbdbdbdba...", "...abdbdbdba....", "....aaaaaa......", ".....abba.......", ".....abba......."]
    return {
        "knaagdier_muts": ic.icon(ic.pad(muts), {"a": (82, 54, 34), "b": (140, 96, 60), "c": TAND}),
        "knaagdier_staart": ic.icon(ic.pad(staart), {"a": (46, 34, 28), "b": BEVER_DONKER, "d": (96, 74, 60)}),
    }


TEKSTEN = {
    "entity.guhs.guh.beverguh": "Beverguh",
    "gui.guhs.guhdex.rarity.beverguh": "Zeldzaamheid: Alleen in de Beverbeek",
    "gui.guhs.guhdex.info.beverguh": "Een kastanjebruine guh met twee oranje knaagtandjes en een platte geschubde staart. "
                                     "Knabbelt aan alles wat van hout is (ook aan je deur, sorry vads). Woont in de Beverbeek "
                                     "tussen de bevers. Tem hem met kaasknabbels, njeg!",
    "item.guhs.knaagdier_muts": "Knaagdiermuts",
    "item.guhs.knaagdier_staart": "Beverstaart",
}


def beverguh(base, g, v):
    """The Beverguh painted on the guh sheet (the way the Tanukiguh is): chestnut fur, a lighter belly and muzzle, darker
    paws, ears and thin tail; its paddle and teeth bones get their swatches."""
    from features import guhpolder_tex as gt
    from features import bio_dieren_modellen as bm
    licht = (186, 140, 100)
    p = gt.Painter(base, BEVER_VACHT, fluff=0.05)
    for c in gt._faces(g, "body"):
        p.rect(c["down"], licht)
        p.rect(c["north"], licht, rows=(0.4, 1.0))
    head = gt._faces(g, "head")
    front = min((c for c in head if c["_size"][0] >= 10), key=lambda c: c["_origin"][2])
    bm._ellips(p, front["north"], 0.5, 0.88, 0.34, 0.24, licht)          # the lighter muzzle
    for ear in ("ear_left", "ear_right"):
        for c in gt._faces(g, ear):
            for k, fr in c.items():
                if not k.startswith("_"):
                    p.rect(fr, BEVER_DONKER)
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right", "tail"):
        for c in gt._faces(g, leg):
            for k, fr in c.items():
                if not k.startswith("_"):
                    p.rect(fr, BEVER_DONKER, anything=True)
    a = p.a
    rng = np.random.default_rng(16200)
    for bone_name, swatch in (("bever_staart", _schubben(v, rng, BEVER_DONKER)), ("bever_tanden", _tand(v, rng))):
        rect = bm._swatch_rect(g, bone_name)
        if rect:
            bm._plak(a, rect, swatch)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def _textuur(h):
    """guh_beverguh.png painted for real (make_guh_variants only made a first, plain one)."""
    import make_guh_variants as v
    from features import bio_dieren_modellen as bm
    g = bm._guh_geo(h)
    if not all(b in {x["name"] for x in g["bones"]} for b in ("bever_staart", "bever_tanden")):
        print("beverbeek_guhs: run tools/make_guh_variants.py first (the Beverguh's bones are not in guh.geo.json yet)")
        return False
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    h.save(beverguh(base, g, v), "entity", "guh_beverguh.png")
    return True


def _slaap(h):
    """guh_slaap/guh_beverguh.png: the Beverguh with closed eyes (the way the biomes3 guhs get theirs)."""
    from features import bio_dieren_modellen as bm
    pad = os.path.join(h.TEX, "entity", "guh_beverguh.png")
    if not os.path.exists(pad):
        print("beverbeek_guhs: run tools/make_guh_variants.py first (no guh_beverguh.png yet)")
        return False
    img = Image.open(pad).convert("RGBA")
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    h.save(bm.slaap(img, base, (226, 196, 160, 255)), "entity", "guh_slaap", "guh_beverguh.png")
    return True


def build(h):
    for k, v in TEKSTEN.items():
        h.NL[k] = v
    for c in CLOTHES:
        h.item_model(c)
    h.shapeless("knaagdier_muts", ["minecraft:brown_wool", "minecraft:brown_wool", "minecraft:brown_wool", "minecraft:orange_dye"],
                "guhs:knaagdier_muts")
    h.shapeless("knaagdier_staart", ["minecraft:brown_wool", "minecraft:brown_wool", "minecraft:leather"], "guhs:knaagdier_staart")
    return _textuur(h) and _slaap(h)
