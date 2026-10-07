"""
bbq2 (ring-h1) - the looks of chapter 1 of the Knabbelring: the four quest props of Guhdalf's camp (block models from boxes,
two small textures of their own, the rest borrowed from blocks that exist) and the drawn map of the narrator card.

  ringh1_vuurwerkkist     a crate with Guhdalf's rune, rockets sticking out of it
  ringh1_feesttafel       a laid table: a cloth, the cake with a candle, a heap of knabbels
  ringh1_proviand         an open crate, soort 0 worst / 1 kaas / 2 knabbels
  ringh1_schoorsteentje   a chimney pot (Java lets it smoke)
"""
from PIL import Image, ImageDraw

BLOKKEN = ["ringh1_vuurwerkkist", "ringh1_feesttafel", "ringh1_proviand", "ringh1_schoorsteentje"]


def _krat(h, band=None, rune=False):
    """A crate side: spruce planks with dark slats, optionally a coloured band and Guhdalf's rune."""
    img = h.vanilla("block/spruce_planks").copy()
    d = ImageDraw.Draw(img)
    donker, licht = (74, 52, 30, 255), (148, 110, 66, 255)
    d.rectangle([0, 0, 15, 1], fill=donker)
    d.rectangle([0, 14, 15, 15], fill=donker)
    d.rectangle([0, 0, 1, 15], fill=donker)
    d.rectangle([14, 0, 15, 15], fill=donker)
    d.line([2, 2, 13, 2], fill=licht)
    if band:
        d.rectangle([2, 6, 13, 10], fill=band)
    if rune:
        wit = (250, 244, 224, 255)
        for (x, y) in ((6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (6, 9), (7, 9), (8, 9), (9, 9), (9, 8), (8, 8)):
            img.putpixel((x, y), wit)       # a little "G"
        img.putpixel((7, 8), band)
    return img


def textures(h):
    h.save(_krat(h, band=(176, 46, 52, 255), rune=True), "block", "ringh1_vuurwerkkist.png")
    h.save(_krat(h), "block", "ringh1_krat.png")


def _model(h, name, elements, textures):
    h.w(f"{h.A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})


def modellen(h):
    el = h.el
    # the fireworks crate
    kist = "guhs:block/ringh1_vuurwerkkist"
    _model(h, "ringh1_vuurwerkkist", [
        el([1, 0, 1], [15, 10, 15], "#kist"),
        el([2, 10, 2], [14, 10.5, 14], "#stro", faces=("up",)),
        el([3, 10, 3], [5, 16, 5], "#rood"), el([3, 15, 3], [5, 16, 5], "#wit", faces=("up",)),
        el([7, 10, 9], [9, 15, 11], "#blauw"), el([7, 14, 9], [9, 15, 11], "#wit", faces=("up",)),
        el([11, 10, 4], [13, 16, 6], "#geel"), el([11, 15, 4], [13, 16, 6], "#wit", faces=("up",)),
        el([10, 10, 11], [12, 14, 13], "#rood"), el([4, 10, 10], [6, 13, 12], "#geel"),
    ], {"particle": kist, "kist": kist, "stro": "minecraft:block/hay_block_top", "rood": "minecraft:block/red_concrete",
        "blauw": "minecraft:block/light_blue_concrete", "geel": "minecraft:block/yellow_concrete", "wit": "minecraft:block/white_concrete"})
    h.w(f"{h.A}/blockstates/ringh1_vuurwerkkist.json", {"variants": {"": {"model": "guhs:block/ringh1_vuurwerkkist"}}})
    # the party table
    _model(h, "ringh1_feesttafel", [
        el([0, 12, 0], [16, 14, 16], "#hout"),
        el([1, 0, 1], [3, 12, 3], "#hout"), el([13, 0, 1], [15, 12, 3], "#hout"), el([1, 0, 13], [3, 12, 15], "#hout"), el([13, 0, 13], [15, 12, 15], "#hout"),
        el([0.5, 14, 0.5], [15.5, 14.25, 15.5], "#kleed", faces=("up", "north", "south", "east", "west")),
        el([4, 14.25, 4], [12, 19, 12], "#taart_zij", faces=("north", "south", "east", "west")),
        el([4, 14.25, 4], [12, 19, 12], "#taart", faces=("up",)),
        el([7.5, 19, 7.5], [8.5, 22, 8.5], "#kaars"),
        el([1, 14.25, 11], [4, 16, 14], "#knabbels"), el([12, 14.25, 1], [15, 15.5, 4], "#knabbels"),
    ], {"particle": "minecraft:block/spruce_planks", "hout": "minecraft:block/spruce_planks", "kleed": "minecraft:block/pink_wool",
        "taart_zij": "minecraft:block/cake_side", "taart": "minecraft:block/cake_top", "kaars": "minecraft:block/red_concrete",
        "knabbels": "guhs:block/block_of_kaasknabbels"})
    h.w(f"{h.A}/blockstates/ringh1_feesttafel.json", {"variants": {"": {"model": "guhs:block/ringh1_feesttafel"}}})
    # the provisions: an open crate with what is in it
    krat = "guhs:block/ringh1_krat"
    inhoud = {
        0: ([el([2, 8, 3], [14, 12, 6], "#a"), el([2, 8, 7], [14, 11, 10], "#a"), el([3, 8, 11], [13, 12, 13], "#a")],
            {"a": "guhs:block/worst_stam"}),
        1: ([el([3, 8, 3], [13, 13, 13], "#a"), el([5, 13, 5], [11, 14, 11], "#a", faces=("up", "north", "south", "east", "west"))],
            {"a": "guhs:block/belegen_kaas_tegels"}),
        2: ([el([2, 8, 2], [14, 11, 14], "#a"), el([4, 11, 4], [12, 13, 12], "#a"), el([6, 13, 6], [10, 14, 10], "#a")],
            {"a": "guhs:block/block_of_kaasknabbels"}),
    }
    for soort, (elementen, tex) in inhoud.items():
        _model(h, f"ringh1_proviand_{soort}", [
            el([1, 0, 1], [15, 1, 15], "#krat"),
            el([1, 1, 1], [15, 9, 2], "#krat"), el([1, 1, 14], [15, 9, 15], "#krat"), el([1, 1, 2], [2, 9, 14], "#krat"), el([14, 1, 2], [15, 9, 14], "#krat"),
        ] + elementen, {"particle": krat, "krat": krat, **tex})
    h.w(f"{h.A}/blockstates/ringh1_proviand.json", {"variants": {f"soort={s}": {"model": f"guhs:block/ringh1_proviand_{s}"} for s in inhoud}})
    # the chimney pot
    _model(h, "ringh1_schoorsteentje", [
        el([5, 0, 5], [11, 10, 11], "#steen"),
        el([4, 10, 4], [12, 12, 12], "#pot"),
        el([6, 11.9, 6], [10, 12, 10], "#roet", faces=("up",)),
    ], {"particle": "minecraft:block/bricks", "steen": "minecraft:block/bricks", "pot": "minecraft:block/terracotta", "roet": "minecraft:block/coal_block"})
    h.w(f"{h.A}/blockstates/ringh1_schoorsteentje.json", {"variants": {"": {"model": "guhs:block/ringh1_schoorsteentje"}}})


def kaart():
    """The drawn map of the narrator card: the hills of the Knabbelgouw around the feestwei, the big barbecueput south of it,
    the path Guhdalf's cart came by, a cross where the party is."""
    from features import verhaal_motor
    k = verhaal_motor.Kaart(seed=16)
    k.land([(14, 150), (10, 60), (40, 22), (110, 12), (190, 16), (240, 50), (246, 120), (200, 150), (100, 154)])
    k.rivier([(246, 96), (214, 104), (196, 128), (170, 152)], breed=3)
    for (x, y, hoog) in ((70, 54, 16), (124, 40, 22), (180, 54, 16), (44, 92, 14), (212, 88, 14)):
        k.berg(x, y, hoog)
    for (x, y) in ((70, 60), (124, 48), (180, 60), (44, 98), (212, 94)):
        k.huisje(x, y)
    k.bos(22, 112, 30, 26, 7)
    k.bos(204, 22, 30, 18, 5)
    k.boom(118, 82)
    k.pad([(4, 132), (40, 126), (86, 112), (124, 96), (128, 82)])
    k.pad([(128, 82), (128, 116)])
    k.kruis(128, 86)
    k.toren(128, 126, 10)
    k.tekst(92, 136, "barbecueput")
    k.tekst(98, 66, "Knabbelgouw")
    k.kompasroos(226, 132)
    return k


def build(h):
    textures(h)
    modellen(h)
