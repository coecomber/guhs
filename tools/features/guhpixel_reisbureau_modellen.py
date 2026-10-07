"""
Reisbureau "De Vadsvakantie": the models of the 32 souvenirs, the Gouden koffertje, the koffertje and the Reisbalie.

Every model is a handful of coloured boxes. A box is (x0, y0, z0, x1, y1, z1, colour[, options]); the colours of one model
are painted as 4 x 4 swatches on its own 16 x 16 texture (block/reisbureau_<id>.png) and every face of a box shows the swatch
of its colour. Options: "gloed" (a full-bright box), "tex" + "uv" (another texture on the faces in "op", default all),
"rot" (a model rotation). All models face north (the blockstate turns them); wall things hang against z 14..16.

SOUVENIRS: id -> (kind, boxes, extra textures, shape). kind "vloer" / "muur"; shape = the bounding box Java uses as the
block's shape (feature/guhpixel/reisbureau/ReisbureauSlice.java reads the same numbers from VORMEN below through the
generated data/guhs/reisbureau/vormen.json: a game test compares the registered blocks with it).
"""
import numpy as np
from PIL import Image

from features import guhpixel_reisbureau_tex as tex

N = "reisbureau"
ALL = ("down", "up", "north", "south", "west", "east")
GLOED = {"neoforge_data": {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}}

KLEUR = {
    "hout": (150, 104, 62), "hout_donker": (104, 68, 40), "hout_licht": (206, 164, 112), "kurk": (196, 150, 98),
    "goud": (250, 208, 70), "goud_donker": (206, 150, 36), "goud_licht": (255, 238, 150),
    "kaas": (250, 206, 84), "kaas_oranje": (244, 170, 60), "kaas_gat": (214, 158, 46), "was_rood": (212, 58, 60),
    "mos": (96, 150, 70), "mos_licht": (134, 186, 94), "groen": (70, 140, 74), "groen_donker": (46, 100, 56),
    "roze": (255, 150, 196), "roze_licht": (255, 198, 224), "roze_donker": (222, 98, 150), "paars": (164, 108, 214),
    "wit": (246, 244, 240), "wol": (236, 232, 222), "creme": (244, 228, 196), "grijs": (150, 150, 158), "grijs_donker": (96, 96, 106),
    "ijzer": (82, 86, 98), "zwart": (40, 36, 46), "donker": (58, 28, 60),
    "zand": (232, 208, 150), "zand_donker": (206, 176, 116), "water": (92, 170, 226), "water_licht": (150, 210, 244),
    "rood": (214, 62, 58), "rood_donker": (160, 40, 44), "oranje": (244, 140, 50), "geel": (255, 226, 90),
    "blauw": (70, 110, 210), "blauw_licht": (130, 170, 240), "delfts": (236, 240, 250), "delfts_blauw": (70, 96, 190),
    "vlam": (255, 150, 40), "vlam_kern": (255, 232, 120), "glim": (220, 255, 140), "licht": (255, 244, 170),
    "huid": (250, 214, 190), "steen": (128, 124, 130), "bruin": (130, 84, 52), "bruin_licht": (170, 118, 74),
}


def b(x0, y0, z0, x1, y1, z1, kleur, **opt):
    return (x0, y0, z0, x1, y1, z1, kleur, opt)


def _schilderij(lijst, hoek, beeld):
    """A framed picture on the wall: the frame, little corner blocks, the picture (texture #beeld) on the front."""
    return [
        b(1, 1, 15, 15, 15, 16, lijst),
        b(1, 1, 14.4, 15, 2.5, 15, lijst), b(1, 13.5, 14.4, 15, 15, 15, lijst),
        b(1, 2.5, 14.4, 2.5, 13.5, 15, lijst), b(13.5, 2.5, 14.4, 15, 13.5, 15, lijst),
        b(0.6, 0.6, 14.2, 2.2, 2.2, 15.2, hoek), b(13.8, 0.6, 14.2, 15.4, 2.2, 15.2, hoek),
        b(0.6, 13.8, 14.2, 2.2, 15.4, 15.2, hoek), b(13.8, 13.8, 14.2, 15.4, 15.4, 15.2, hoek),
        b(2.5, 2.5, 14.7, 13.5, 13.5, 15, "wit", tex="#beeld", uv=[0, 0, 16, 16], op=("north",)),
    ]


def _toren(kleur, lichtjes):
    out = [
        b(3, 0, 3, 5.5, 4, 5.5, kleur), b(10.5, 0, 3, 13, 4, 5.5, kleur), b(3, 0, 10.5, 5.5, 4, 13, kleur), b(10.5, 0, 10.5, 13, 4, 13, kleur),
        b(3.5, 4, 3.5, 12.5, 5, 12.5, "kaas_oranje"), b(5.5, 5, 5.5, 10.5, 9, 10.5, kleur), b(5, 9, 5, 11, 9.8, 11, "kaas_oranje"),
        b(7, 9.8, 7, 9, 14, 9, kleur), b(7.6, 14, 7.6, 8.4, 16, 8.4, "kaas_oranje"),
    ]
    if lichtjes:
        for (x, y, z) in ((3.2, 4.2, 3.2), (12, 4.2, 3.2), (3.2, 4.2, 12), (12, 4.2, 12), (4.8, 9.1, 4.8), (10.4, 9.1, 4.8), (4.8, 9.1, 10.4),
                          (10.4, 9.1, 10.4), (7.6, 15.6, 7.6)):
            out.append(b(x, y, z, x + 0.8, y + 0.8, z + 0.8, "licht", gloed=True))
        out += [b(6.4, 6, 5.3, 7.4, 7, 5.5, "licht", gloed=True), b(8.6, 6, 5.3, 9.6, 7, 5.5, "licht", gloed=True),
                b(6.4, 6, 10.5, 7.4, 7, 10.7, "licht", gloed=True), b(8.6, 6, 10.5, 9.6, 7, 10.7, "licht", gloed=True)]
    return out


def _vaas(band, tulpen):
    out = [b(5.5, 0, 5.5, 10.5, 6, 10.5, "delfts"), b(5.4, 2, 5.4, 10.6, 3.2, 10.6, band), b(6.5, 6, 6.5, 9.5, 7, 9.5, "delfts"),
           b(6.2, 7, 6.2, 9.8, 7.6, 9.8, band)]
    plekken = ((6.6, 12.5, 6.6), (8.8, 13.5, 6.8), (7.7, 14.5, 7.9), (6.4, 13, 8.9), (9, 12, 9), (7.7, 11.5, 5.9))
    for i, (x, top, z) in enumerate(plekken):
        out.append(b(x + 0.35, 7.6, z + 0.35, x + 0.75, top - 1.8, z + 0.75, "groen"))
        out.append(b(x - 0.2, top - 1.8, z - 0.2, x + 1.3, top, z + 1.3, tulpen[i % len(tulpen)]))
    out.append(b(5.2, 9, 7.6, 6.8, 9.5, 8.4, "groen_donker"))
    out.append(b(9.3, 10, 7.6, 10.9, 10.5, 8.4, "groen_donker"))
    return out


def _sneeuwbol(voet, koepel):
    return [
        b(3, 0, 3, 13, 2.5, 13, voet), b(3.5, 2.5, 3.5, 12.5, 3.2, 12.5, "goud_donker" if voet == "goud" else "hout_donker"),
        b(4.5, 3.2, 4.5, 11.5, 4.2, 11.5, "wit"),
        b(6, 4.2, 6.2, 9, 6.6, 9.2, "rood"), b(5.6, 6.6, 5.8, 9.4, 7.4, 9.6, "wit"), b(6.6, 7.4, 6.8, 8.4, 8, 8.6, "wit"),
        b(7.1, 4.2, 6.1, 7.9, 5.6, 6.2, "hout_donker"),
        b(10, 4.2, 9.2, 11, 5, 10.2, "hout"), b(9.5, 5, 8.7, 11.5, 6.4, 10.7, "groen"), b(10, 6.4, 9.2, 11, 7.6, 10.2, "groen"),
        b(3.6, 3.2, 3.6, 12.4, 12, 12.4, "wit", tex=koepel, uv=[0, 0, 16, 16]),
    ]


def _wereldbol(goud):
    if goud:
        bol = [b(4, 4, 4, 12, 12, 12, "goud", tex="#draai", uv=[0, 0, 16, 16], op=("north", "south", "west", "east"))]
        voet, as_ = "zwart", "goud_licht"
    else:
        bol = [b(4, 4, 4, 12, 12, 12, "water"),
               b(5, 7, 3.9, 8, 11, 4, "groen"), b(9, 5, 3.9, 11, 8, 4, "groen"), b(12, 6, 5, 12.1, 10, 8, "groen"), b(12, 8, 9, 12.1, 11, 11, "mos_licht"),
               b(5, 5, 12, 9, 9, 12.1, "groen"), b(9, 9, 12, 11, 11, 12.1, "mos_licht"), b(3.9, 6, 6, 4, 10, 10, "groen"),
               b(6, 12, 6, 10, 12.1, 10, "wit"), b(6, 3.9, 6, 10, 4, 10, "wit")]
        voet, as_ = "hout_donker", "goud"
    return [b(5, 0, 5, 11, 1, 11, voet), b(6, 1, 6, 10, 1.6, 10, voet), b(7.5, 1.6, 7.5, 8.5, 4, 8.5, as_),
            b(7.5, 12, 7.5, 8.5, 13.2, 8.5, as_), b(3, 3.2, 7.4, 3.6, 12.8, 8.6, as_), b(3, 12.6, 7.4, 8.5, 13.2, 8.6, as_),
            b(3, 3.2, 7.4, 8.5, 3.8, 8.6, as_)] + bol


def _koffer(lijf, band, slot, stickers):
    out = [b(3, 0, 5, 13, 7, 11, lijf), b(5, 0, 4.9, 6, 7.1, 11.1, band), b(10, 0, 4.9, 11, 7.1, 11.1, band),
           b(6.5, 7, 7.5, 7.2, 8.6, 8.5, band), b(8.8, 7, 7.5, 9.5, 8.6, 8.5, band), b(6.5, 8.6, 7.5, 9.5, 9.2, 8.5, band),
           b(5.1, 5.2, 4.7, 5.9, 6.2, 4.9, slot), b(10.1, 5.2, 4.7, 10.9, 6.2, 4.9, slot),
           b(2.8, 0, 4.8, 3.6, 0.8, 5.6, band), b(12.4, 0, 4.8, 13.2, 0.8, 5.6, band), b(2.8, 0, 10.4, 3.6, 0.8, 11.2, band),
           b(12.4, 0, 10.4, 13.2, 0.8, 11.2, band)]
    for (x, y, w, hgt, kleur) in stickers:
        out.append(b(x, y, 4.85, x + w, y + hgt, 4.95, kleur))
    return out


# id -> (kind, boxes, extra textures {name: texture id}, licht (0..15), effect ("" / stoom / vuur / glim / sneeuw / muziek / glinster))
SOUVENIRS = {
    # ---- 1 hour ----------------------------------------------------------------------------------------------------------
    "souvenir_lingsesdijk": ("muur", _schilderij("hout", "hout_donker", "#beeld"), {"beeld": f"guhs:block/{N}_beeld_lingsesdijk"}, 0, ""),
    "zeldzaam_lingsesdijk": ("muur", _schilderij("goud", "goud_licht", "#beeld") + [
        b(7, 15, 14.2, 9, 15.8, 15.2, "goud_licht"), b(7, 0.2, 14.2, 9, 1, 15.2, "goud_licht")],
        {"beeld": f"guhs:block/{N}_beeld_lingsesdijk"}, 0, ""),
    "souvenir_kaasmarkt": ("vloer", [
        b(3, 0, 3, 13, 3, 13, "kaas"), b(3.5, 3, 4.5, 12.5, 6, 13.5, "kaas_oranje"), b(4.5, 6, 3.5, 11.5, 9, 10.5, "kaas"),
        b(2.9, 1, 2.9, 13.1, 2, 13.1, "was_rood"), b(4.4, 7, 3.4, 11.6, 8, 10.6, "was_rood"),
        b(7.7, 9, 6.7, 8.3, 13, 7.3, "hout"), b(8.3, 11, 6.9, 11, 13, 7.1, "roze")], {}, 0, ""),
    "zeldzaam_kaasmarkt": ("vloer", [
        b(3, 0, 3, 13, 1.5, 13, "hout_donker"), b(4, 1.5, 4, 12, 2.2, 12, "rood"),
        b(5, 2.2, 5, 11, 7.2, 11, "goud"), b(6, 7.2, 6, 10, 8, 10, "goud_licht"),
        b(6, 3.5, 4.9, 7.5, 5, 5, "goud_donker"), b(8.8, 5, 4.9, 10, 6.2, 5, "goud_donker"), b(11, 3, 6.5, 11.1, 4.5, 8, "goud_donker"),
        b(4.9, 4.5, 8, 5, 6, 9.5, "goud_donker"), b(7, 3, 11, 8.5, 4.5, 11.1, "goud_donker"), b(7, 8, 7, 8.4, 8.1, 8.4, "goud_donker")],
        {}, 0, "glinster"),
    "souvenir_vadswoud": ("vloer", [
        b(2, 0, 2, 14, 3.5, 14, "mos"), b(3, 3.5, 3, 13, 4.6, 13, "mos_licht"), b(1.6, 1, 4, 2, 2.6, 12, "mos_licht"),
        b(14, 1, 4, 14.4, 2.6, 12, "mos_licht"), b(5, 4.6, 5, 6, 5.4, 6, "roze"), b(10, 4.6, 9, 11, 5.4, 10, "wit"),
        b(7.5, 4.6, 10.5, 8.3, 5.2, 11.3, "geel"), b(7.4, 4.6, 7.4, 8.6, 4.9, 8.6, "mos")], {}, 0, ""),
    "zeldzaam_vadswoud": ("vloer", [
        b(5, 0, 5, 11, 1, 11, "ijzer"), b(5.5, 1, 5.5, 10.5, 8, 10.5, "wit", tex="#glas", uv=[0, 0, 16, 16]),
        b(5.2, 1, 5.2, 5.8, 8, 5.8, "ijzer"), b(10.2, 1, 5.2, 10.8, 8, 5.8, "ijzer"), b(5.2, 1, 10.2, 5.8, 8, 10.8, "ijzer"),
        b(10.2, 1, 10.2, 10.8, 8, 10.8, "ijzer"), b(5, 8, 5, 11, 9, 11, "ijzer"), b(6, 9, 6, 10, 10, 10, "ijzer"),
        b(7, 10, 7.5, 7.6, 12, 8.5, "ijzer"), b(8.4, 10, 7.5, 9, 12, 8.5, "ijzer"), b(7, 12, 7.5, 9, 12.6, 8.5, "ijzer"),
        b(6.6, 2.4, 6.8, 7.4, 3.2, 7.6, "glim", gloed=True), b(8.6, 4.4, 8.2, 9.4, 5.2, 9, "glim", gloed=True),
        b(7.4, 6, 6.6, 8.2, 6.8, 7.4, "glim", gloed=True), b(6.6, 4.8, 9, 7.2, 5.4, 9.6, "glim", gloed=True)],
        {"glas": f"guhs:block/{N}_glas"}, 10, "glim"),
    "souvenir_knuffeldal": ("vloer", [
        b(4, 3, 5, 12, 9, 13.5, "wol"), b(4.6, 9, 6, 11.4, 10, 12.5, "wol"), b(5.5, 6, 1.8, 10.5, 10.5, 5.2, "huid"),
        b(5.2, 9.5, 2.6, 10.8, 11.2, 5.4, "wol"), b(4.3, 8.4, 3, 5.5, 9.4, 4.2, "huid"), b(10.5, 8.4, 3, 11.7, 9.4, 4.2, "huid"),
        b(6.4, 8, 1.7, 7.2, 8.8, 1.8, "donker"), b(8.8, 8, 1.7, 9.6, 8.8, 1.8, "donker"), b(7.5, 6.6, 1.7, 8.5, 7.2, 1.8, "roze"),
        b(4.8, 0, 5.8, 6.4, 3, 7.4, "donker"), b(9.6, 0, 5.8, 11.2, 3, 7.4, "donker"), b(4.8, 0, 11, 6.4, 3, 12.6, "donker"),
        b(9.6, 0, 11, 11.2, 3, 12.6, "donker"), b(7.2, 6, 13.5, 8.8, 7.6, 14.4, "wol")], {}, 0, ""),
    "zeldzaam_knuffeldal": ("vloer", [
        b(4, 0.6, 4, 12, 1.8, 12, "goud_donker"), b(5, 1.8, 5, 11, 7.5, 11, "goud"), b(6, 7.5, 6, 10, 9.5, 10, "goud"),
        b(7, 9.5, 7.6, 7.6, 11.6, 8.4, "goud_donker"), b(8.4, 9.5, 7.6, 9, 11.6, 8.4, "goud_donker"), b(7, 11.6, 7.6, 9, 12.2, 8.4, "goud_donker"),
        b(7.4, 0, 7.4, 8.6, 0.6, 8.6, "ijzer"), b(4.9, 5, 4.9, 11.1, 5.6, 11.1, "goud_licht"),
        b(6.2, 12.2, 7.7, 9.8, 13.6, 8.3, "rood"), b(5.4, 12.6, 7.7, 6.2, 14.4, 8.3, "rood"), b(9.8, 12.6, 7.7, 10.6, 14.4, 8.3, "rood")],
        {}, 0, "glinster"),
    # ---- 2 hours ---------------------------------------------------------------------------------------------------------
    "souvenir_guhwaii": ("vloer", [
        b(2, 0, 2, 14, 2.5, 14, "zand"), b(4, 2.5, 4, 12, 6.5, 12, "zand"), b(2, 2.5, 2, 5, 8, 5, "zand_donker"), b(11, 2.5, 2, 14, 8, 5, "zand_donker"),
        b(2, 2.5, 11, 5, 8, 14, "zand_donker"), b(11, 2.5, 11, 14, 8, 14, "zand_donker"), b(2, 8, 2, 3, 9, 3, "zand"), b(4, 8, 2, 5, 9, 3, "zand"),
        b(11, 8, 2, 12, 9, 3, "zand"), b(13, 8, 2, 14, 9, 3, "zand"), b(2, 8, 13, 3, 9, 14, "zand"), b(13, 8, 13, 14, 9, 14, "zand"),
        b(6.8, 2.5, 3.9, 9.2, 5.2, 4, "donker"), b(6, 6.5, 6, 10, 8.5, 10, "zand"), b(7.7, 8.5, 7.7, 8.3, 13, 8.3, "hout"),
        b(8.3, 10.8, 7.9, 11.4, 13, 8.1, "roze"), b(12, 0, 6, 13.4, 3.1, 7.4, "water_licht")], {}, 0, ""),
    "zeldzaam_guhwaii": ("vloer", [
        b(3, 0, 5, 13, 1, 11, "zand"), b(5, 1, 6.5, 11, 1.8, 9.5, "zand_donker"),
        b(6, 1.8, 7.2, 10, 14, 8.6, "goud"), b(6.8, 14, 7.2, 9.2, 15.2, 8.6, "goud"), b(7.4, 15.2, 7.2, 8.6, 16, 8.6, "goud_licht"),
        b(7.5, 2.5, 7.1, 8.5, 13.5, 7.2, "roze"), b(6, 6, 7.1, 10, 7, 7.2, "wit"), b(7.6, 1.8, 8.6, 8.4, 4, 9.6, "goud_donker")],
        {}, 0, "glinster"),
    "souvenir_barbecuether": ("vloer", [
        b(5, 0, 5, 11, 6, 11, "hout"), b(4.8, 1, 4.8, 11.2, 1.8, 11.2, "ijzer"), b(4.8, 4, 4.8, 11.2, 4.8, 11.2, "ijzer"),
        b(5.6, 5.6, 5.6, 10.4, 6.1, 10.4, "water"), b(4.4, 5, 7.4, 5, 9, 8.6, "hout_donker"), b(11, 5, 7.4, 11.6, 9, 8.6, "hout_donker"),
        b(4.4, 9, 7.4, 11.6, 9.6, 8.6, "hout_donker"), b(8, 5.8, 6.4, 14.5, 6.6, 7.2, "hout_licht"), b(7, 5.2, 6, 9, 6.4, 7.6, "hout_licht"),
        b(12.4, 0, 11.4, 14.6, 1.6, 13.6, "steen"), b(1.6, 0, 2.4, 3.6, 1.4, 4.4, "steen"), b(13, 0, 3, 14.4, 1.2, 4.4, "grijs_donker")], {}, 0, ""),
    "zeldzaam_barbecuether": ("vloer", [
        b(2, 2, 4, 14, 7, 12, "wit"), b(1.6, 6.4, 3.6, 14.4, 7.4, 12.4, "wit"), b(3, 6.6, 5, 13, 7.5, 11, "water_licht"),
        b(2.5, 0, 4.5, 4, 2, 6, "goud"), b(12, 0, 4.5, 13.5, 2, 6, "goud"), b(2.5, 0, 10, 4, 2, 11.5, "goud"), b(12, 0, 10, 13.5, 2, 11.5, "goud"),
        b(13, 7.4, 7.5, 14, 9.6, 8.5, "goud"), b(11.6, 9, 7.5, 14, 9.8, 8.5, "goud"), b(4, 7.5, 7, 6.4, 9.2, 9.4, "roze"),
        b(4.4, 9.2, 7.2, 5, 10, 7.8, "roze"), b(5.4, 9.2, 7.2, 6, 10, 7.8, "roze"), b(8, 7.5, 6, 9, 8, 7, "wit"), b(9.6, 7.5, 9, 10.4, 7.9, 9.8, "wit")],
        {}, 0, "stoom"),
    "souvenir_efteguh": ("vloer", [
        b(3, 0, 3, 13, 1, 13, "steen"), b(3.5, 1, 4, 12.5, 9, 13, "roze"), b(4, 7.5, 2.6, 12, 14, 10, "roze"),
        b(4.2, 14, 5, 6.2, 16, 6.6, "roze_donker"), b(9.8, 14, 5, 11.8, 16, 6.6, "roze_donker"),
        b(5.6, 8.4, 2.4, 10.4, 11.4, 2.6, "donker"), b(6.2, 8.4, 2.3, 9.8, 9, 2.5, "roze_donker"),
        b(5.4, 12, 2.5, 6.6, 13, 2.6, "donker"), b(9.4, 12, 2.5, 10.6, 13, 2.6, "donker"), b(7.5, 11.4, 2.4, 8.5, 12, 2.6, "roze_donker"),
        b(2.4, 5, 5, 3.5, 7, 8, "roze"), b(12.5, 5, 5, 13.6, 7, 8, "roze"), b(5, 3, 3.6, 11, 6, 4, "creme"),
        b(6.4, 9, 1.6, 8.4, 10.4, 2.4, "wit")], {}, 0, ""),
    "zeldzaam_efteguh": ("vloer", [
        b(6, 0, 6, 10, 6, 10, "creme"), b(2, 6, 2, 14, 9, 14, "rood"), b(3, 9, 3, 13, 11, 13, "rood"), b(5, 11, 5, 11, 12.4, 11, "rood"),
        b(2.5, 5.4, 2.5, 13.5, 6, 13.5, "creme"),
        b(4, 11, 4, 5.6, 11.2, 5.6, "wit"), b(10, 11, 9, 11.6, 11.2, 10.6, "wit"), b(7, 12.4, 7, 9, 12.6, 9, "wit"), b(5.5, 9, 10.5, 7, 11.1, 12, "wit"),
        b(9.5, 9, 3, 11.5, 11.1, 4.5, "wit"),
        b(4, 6.8, 1.9, 5.6, 8.2, 2, "wit"), b(9, 7, 1.9, 10.4, 8.4, 2, "wit"), b(14, 7, 5, 14.1, 8.4, 6.6, "wit"), b(14, 6.6, 10, 14.1, 8, 11.4, "wit"),
        b(1.9, 7, 8, 2, 8.4, 9.6, "wit"), b(6, 6.8, 14, 7.6, 8.2, 14.1, "wit"), b(10.4, 7, 14, 12, 8.4, 14.1, "wit"),
        b(7.2, 0, 5.8, 8.8, 3, 6, "hout_donker"), b(8.3, 1.4, 5.7, 8.6, 1.7, 5.8, "goud"), b(9.2, 3.4, 5.9, 9.9, 4.6, 6, "licht", gloed=True)],
        {}, 4, "muziek"),
    "souvenir_guhkenhof": ("vloer", _vaas("delfts_blauw", ("roze", "roze_donker", "roze_licht")), {}, 0, ""),
    "zeldzaam_guhkenhof": ("vloer", _vaas("goud", ("rood", "oranje", "geel", "groen", "blauw", "paars")), {}, 0, "glinster"),
    # ---- 8 hours ---------------------------------------------------------------------------------------------------------
    "souvenir_nomguh": ("vloer", _sneeuwbol("hout", "#glas"), {"glas": f"guhs:block/{N}_glas"}, 0, ""),
    "zeldzaam_nomguh": ("vloer", _sneeuwbol("goud", "#sneeuw"), {"sneeuw": f"guhs:block/{N}_sneeuw"}, 0, "sneeuw"),
    "souvenir_guhrijs": ("vloer", _toren("kaas", False), {}, 0, ""),
    "zeldzaam_guhrijs": ("vloer", _toren("kaas", True), {}, 12, "glinster"),
    "souvenir_camping": ("vloer", [
        b(1.5, 0, 2, 14.5, 0.5, 14, "mos"), b(2, 0.5, 3, 14, 2.5, 13, "oranje"), b(3.5, 2.5, 3, 12.5, 4.5, 13, "oranje"),
        b(5, 4.5, 3, 11, 6.5, 13, "oranje"), b(6.5, 6.5, 3, 9.5, 8.2, 13, "oranje"), b(7.4, 8.2, 2.5, 8.6, 8.9, 13.5, "hout_donker"),
        b(6.4, 0.5, 2.9, 9.6, 4.6, 3, "donker"), b(7.2, 4.6, 2.9, 8.8, 6.2, 3, "donker"), b(7.9, 0.5, 2.8, 8.1, 6.2, 2.9, "creme"),
        b(1.6, 0.5, 2.4, 2.1, 1.6, 2.9, "hout"), b(13.9, 0.5, 2.4, 14.4, 1.6, 2.9, "hout"), b(1.6, 0.5, 13.1, 2.1, 1.6, 13.6, "hout"),
        b(13.9, 0.5, 13.1, 14.4, 1.6, 13.6, "hout"), b(2, 1.4, 12.9, 14, 2, 13.1, "geel")], {}, 0, ""),
    "zeldzaam_camping": ("vloer", [
        b(4, 0, 7, 12, 1.5, 9, "hout"), b(7, 0, 4, 9, 1.5, 12, "hout_donker"), b(5, 1.5, 5.5, 7, 2.8, 10.5, "hout"),
        b(3, 0, 4, 4.6, 1.4, 5.6, "steen"), b(11.4, 0, 4, 13, 1.4, 5.6, "grijs_donker"), b(3, 0, 10.4, 4.6, 1.4, 12, "grijs_donker"),
        b(11.4, 0, 10.4, 13, 1.4, 12, "steen"), b(2.4, 0, 7.2, 3.8, 1.2, 8.8, "steen"), b(12.2, 0, 7.2, 13.6, 1.2, 8.8, "steen"),
        b(6.4, 1.5, 6.4, 9.6, 4.6, 9.6, "vlam", gloed=True), b(7, 4.6, 7, 9, 6.8, 9, "vlam", gloed=True),
        b(7.4, 1.6, 7.4, 8.6, 8.2, 8.6, "vlam_kern", gloed=True), b(6.6, 4.6, 8.6, 7.4, 5.8, 9.4, "vlam_kern", gloed=True),
        b(10.4, 5.6, 7.7, 15.6, 6.2, 8.3, "hout_licht"), b(9, 5.1, 7.2, 10.6, 6.7, 8.8, "wit"), b(9, 5.1, 7.2, 9.3, 6.7, 8.8, "creme")],
        {}, 13, "vuur"),
    "souvenir_guhnetie": ("vloer", [
        b(1, 0, 4, 15, 1, 12, "water"), b(2, 1, 5, 6, 1.2, 6, "water_licht"), b(10, 1, 10, 13.6, 1.2, 10.8, "water_licht"),
        b(3, 1, 6.4, 13, 3, 9.6, "zwart"), b(1.6, 2, 7, 3, 4.6, 9, "zwart"), b(1, 4.6, 7.5, 2, 7, 8.5, "zwart"), b(0.8, 7, 7.4, 2.2, 7.8, 8.6, "goud"),
        b(13, 2, 7, 14.4, 4.4, 9, "zwart"), b(14, 4.4, 7.5, 14.8, 5.6, 8.5, "zwart"), b(3.6, 3, 6.9, 12.4, 3.2, 9.1, "hout_donker"),
        b(6.6, 3.2, 7, 9.6, 3.9, 9, "rood"), b(9.6, 3.2, 7, 10.2, 5.2, 9, "rood"), b(11.4, 3.2, 8.4, 12, 11.5, 9, "hout_licht"),
        b(2.9, 2.2, 6.3, 13.1, 2.5, 6.4, "goud"), b(2.9, 2.2, 9.6, 13.1, 2.5, 9.7, "goud")], {}, 0, ""),
    "zeldzaam_guhnetie": ("muur", [
        b(3.5, 4.5, 14.4, 12.5, 10.5, 16, "wit"), b(4.5, 3.4, 14.6, 11.5, 4.5, 16, "wit"), b(3.5, 10.5, 14.3, 12.5, 11.5, 16, "goud"),
        b(4.8, 7, 14.3, 7, 8.6, 14.4, "donker"), b(9, 7, 14.3, 11.2, 8.6, 14.4, "donker"), b(7.5, 5.2, 13.6, 8.5, 7.4, 14.4, "wit"),
        b(4.5, 8.8, 14.3, 7.3, 9.2, 14.4, "goud"), b(8.7, 8.8, 14.3, 11.5, 9.2, 14.4, "goud"), b(6.6, 4.1, 14.5, 9.4, 4.5, 14.6, "rood"),
        b(3.2, 5.5, 14.4, 3.5, 9.5, 14.8, "goud"), b(12.5, 5.5, 14.4, 12.8, 9.5, 14.8, "goud"),
        b(4.4, 11.5, 14.8, 5.6, 15, 15.6, "paars"), b(6, 11.5, 14.8, 7.2, 16, 15.6, "roze"), b(7.4, 11.5, 14.6, 8.6, 15.4, 15.4, "goud_licht"),
        b(8.8, 11.5, 14.8, 10, 16, 15.6, "blauw_licht"), b(10.4, 11.5, 14.8, 11.6, 15, 15.6, "paars"), b(7.5, 10.6, 14.1, 8.5, 11.4, 14.3, "rood")],
        {}, 0, ""),
    # ---- 24 hours --------------------------------------------------------------------------------------------------------
    "souvenir_kaasmaan": ("vloer", [
        b(3, 0, 3, 13, 1, 13, "zwart"), b(4, 1, 4, 12, 5.5, 12, "kaas"), b(5, 5.5, 5, 11.5, 8.5, 10, "kaas"), b(6, 8.5, 6, 9.4, 10.4, 9, "kaas"),
        b(5, 2, 3.9, 6.6, 3.6, 4, "kaas_gat"), b(9, 3, 3.9, 10.8, 4.8, 4, "kaas_gat"), b(12, 2, 6, 12.1, 3.4, 7.6, "kaas_gat"),
        b(12, 3.4, 9.4, 12.1, 4.6, 10.6, "kaas_gat"), b(3.9, 2.4, 8, 4, 4, 9.6, "kaas_gat"), b(6, 2, 12, 7.8, 3.8, 12.1, "kaas_gat"),
        b(6.4, 6.4, 4.9, 7.8, 7.6, 5, "kaas_gat"), b(8.5, 8.5, 7.5, 9.5, 8.6, 8.5, "kaas_gat"), b(10, 5.5, 10, 11, 5.6, 11, "kaas_gat"),
        b(5.4, 0.2, 2.9, 10.6, 0.8, 3, "goud")], {}, 0, ""),
    "zeldzaam_kaasmaan": ("vloer", [
        b(4, 0, 4, 12, 1, 12, "grijs_donker"), b(7, 1, 7, 9, 2.4, 9, "grijs"), b(6, 2.4, 6, 10, 11, 10, "wit"),
        b(6.5, 11, 6.5, 9.5, 13, 9.5, "rood"), b(7.2, 13, 7.2, 8.8, 14.8, 8.8, "rood"), b(7.7, 14.8, 7.7, 8.3, 16, 8.3, "rood_donker"),
        b(4, 1, 7.5, 6, 5.5, 8.5, "rood"), b(10, 1, 7.5, 12, 5.5, 8.5, "rood"), b(7.5, 1, 4, 8.5, 5.5, 6, "rood"), b(7.5, 1, 10, 8.5, 5.5, 12, "rood"),
        b(7, 7.4, 5.8, 9, 9.4, 6, "blauw_licht"), b(6.8, 7.2, 5.9, 9.2, 9.6, 6, "grijs"), b(5.9, 4.6, 5.9, 10.1, 5.4, 10.1, "rood"),
        b(7.4, 0.2, 7.4, 8.6, 1, 8.6, "vlam", gloed=True)], {}, 0, "glinster"),
    "souvenir_wereldreis": ("vloer", _wereldbol(False), {}, 0, ""),
    "zeldzaam_wereldreis": ("vloer", _wereldbol(True), {"draai": f"guhs:block/{N}_draai"}, 0, "glinster"),
    "souvenir_cruise": ("vloer", [
        b(3, 0, 6, 5, 2.2, 10, "hout_donker"), b(10, 0, 6, 12, 2.2, 10, "hout_donker"),
        b(5, 3.2, 7.2, 10, 4.3, 8.8, "hout"), b(4.4, 3.8, 7.5, 5, 4.6, 8.5, "hout"), b(7.3, 4.3, 7.8, 7.7, 7.2, 8.2, "hout_donker"),
        b(7.7, 4.8, 7.9, 9.6, 6.9, 8.1, "wit"), b(5.6, 4.9, 7.9, 7.3, 6.4, 8.1, "creme"), b(7.4, 7.2, 7.9, 8.4, 7.7, 8.1, "rood"),
        b(2, 2.2, 5, 13, 8, 11, "wit", tex="#glas", uv=[0, 0, 16, 16]), b(13, 4, 7, 15.4, 6, 9, "wit", tex="#glas", uv=[0, 0, 16, 16]),
        b(15.4, 4.2, 7.2, 16, 5.8, 8.8, "kurk"), b(3, 2.4, 6, 12, 3.2, 10, "water")], {"glas": f"guhs:block/{N}_glas"}, 0, ""),
    "zeldzaam_cruise": ("muur", [
        b(5, 11.5, 14, 11, 14.5, 16, "wit"), b(5, 1.5, 14, 11, 4.5, 16, "wit"), b(2, 5, 14, 5, 11, 16, "rood"), b(11, 5, 14, 14, 11, 16, "rood"),
        b(3, 10, 14, 6, 13.5, 16, "rood"), b(10, 10, 14, 13, 13.5, 16, "rood"), b(3, 2.5, 14, 6, 6, 16, "wit"), b(10, 2.5, 14, 13, 6, 16, "wit"),
        b(7.5, 5, 14.4, 8.5, 10.6, 15.2, "goud"), b(6, 9.4, 14.4, 10, 10.2, 15.2, "goud"), b(7.2, 10.6, 14.4, 8.8, 11.4, 15.2, "goud_licht"),
        b(5.6, 5, 14.4, 10.4, 5.9, 15.2, "goud"), b(5.6, 5.9, 14.4, 6.4, 7.2, 15.2, "goud"), b(9.6, 5.9, 14.4, 10.4, 7.2, 15.2, "goud"),
        b(1.4, 7.4, 14.6, 2, 8.6, 15.4, "creme"), b(14, 7.4, 14.6, 14.6, 8.6, 15.4, "creme"), b(7.4, 14.5, 14.6, 8.6, 15.1, 15.4, "creme")],
        {}, 0, ""),
    "souvenir_balkonie": ("muur", _schilderij("hout_licht", "hout", "#beeld"), {"beeld": f"guhs:block/{N}_beeld_balkonie"}, 0, ""),
    "zeldzaam_balkonie": ("muur", [
        b(2, 5, 15.2, 14, 11, 16, "hout_licht"), b(2.5, 5.5, 15, 13.5, 10.5, 15.2, "wit", tex="#beeld", uv=[0, 0, 16, 8], op=("north",)),
        b(1.6, 4.6, 15, 14.4, 5.5, 16, "goud"), b(1.6, 10.5, 15, 14.4, 11.4, 16, "goud"), b(1.6, 5.5, 15, 2.5, 10.5, 16, "goud"),
        b(13.5, 5.5, 15, 14.4, 10.5, 16, "goud"), b(3, 11.4, 15.4, 3.6, 13.6, 16, "ijzer"), b(12.4, 11.4, 15.4, 13, 13.6, 16, "ijzer"),
        b(3, 13.6, 15.4, 13, 14.2, 16, "ijzer")], {"beeld": f"guhs:block/{N}_bordje_balkonie"}, 0, ""),
}

EXTRA = {
    "gouden_koffertje": ("vloer", _koffer("goud", "goud_donker", "goud_licht", ((6.6, 2, 2.8, 2, "goud_licht"),)), {}, 0, "glinster"),
    "koffertje": ("vloer", _koffer("bruin", "hout_donker", "goud", (
        (3.6, 4, 1.2, 1.6, "roze"), (6.4, 1, 1.8, 1.4, "geel"), (8.6, 3.6, 1.2, 1.8, "water"), (11.4, 1.2, 1.2, 1.2, "groen"),
        (6.6, 4.4, 1.4, 1, "wit"), (11.6, 4.4, 1, 1.4, "rood"))), {}, 0, ""),
}

BALIE = [
    b(0, 0, 3, 16, 11, 13, "hout_licht"), b(0, 11, 2, 16, 12, 14, "roze"), b(0, 0, 2.6, 16, 1, 3, "hout_donker"),
    b(0, 0, 2.6, 0.8, 11, 3, "hout"), b(15.2, 0, 2.6, 16, 11, 3, "hout"),
    b(4, 3, 2.7, 12, 9, 3, "wit", tex="#logo", uv=[0, 0, 16, 12], op=("north",)),
    b(3.6, 2.6, 2.8, 12.4, 9.4, 3, "roze_donker"),
    b(2.6, 12, 5.6, 5, 12.5, 8, "goud_donker"), b(3, 12.5, 6, 4.6, 13.6, 7.6, "goud"), b(3.5, 13.6, 6.5, 4.1, 14.2, 7.1, "goud_licht"),
    b(10, 12, 8, 14.4, 12.6, 11, "hout"), b(10.2, 12.6, 9.4, 11.4, 15.4, 10.6, "water"), b(11.6, 12.6, 9.4, 12.8, 15, 10.6, "geel"),
    b(13, 12.6, 9.4, 14.2, 15.6, 10.6, "roze"), b(10.2, 12.6, 8.2, 14.2, 13.6, 9.2, "hout"),
    b(6.4, 12, 4.4, 9, 12.3, 7.6, "creme"), b(6.8, 12.3, 5, 7.8, 12.9, 5.8, "rood"),
]


def _vorm(dozen, soort):
    x0 = min(d[0] for d in dozen)
    y0 = min(d[1] for d in dozen)
    z0 = min(d[2] for d in dozen)
    x1 = max(d[3] for d in dozen)
    y1 = max(d[4] for d in dozen)
    z1 = max(d[5] for d in dozen)
    r = lambda v: max(0.0, min(16.0, round(v * 2) / 2))
    if soort == "muur":
        z1 = 16
    return [r(x0), r(y0), r(z0), r(x1), r(y1), r(z1)]


def _swatches(kleuren, seed):
    """16 x 16: every colour a 4 x 4 swatch, a little lighter at the top and darker at the bottom, with soft noise."""
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., 3] = 255
    a[..., :3] = (255, 0, 255)
    for i, naam in enumerate(kleuren):
        cx, cy = (i % 4) * 4, (i // 4) * 4
        basis = np.array(KLEUR[naam], float)
        for y in range(4):
            for x in range(4):
                f = (1.07, 1.02, 0.97, 0.9)[y] + rng.normal(0, 0.018)
                a[cy + y, cx + x, :3] = np.clip(basis * f, 0, 255)
    return Image.fromarray(a)


def elementen(dozen):
    """(elements, colours in swatch order) of a model."""
    kleuren = []
    for d in dozen:
        if d[6] not in kleuren:
            kleuren.append(d[6])
    if len(kleuren) > 16:
        raise SystemExit(f"reisbureau model: more than 16 colours: {kleuren}")
    els = []
    for (x0, y0, z0, x1, y1, z1, kleur, opt) in dozen:
        i = kleuren.index(kleur)
        cx, cy = (i % 4) * 4, (i // 4) * 4
        uv = [cx + 0.25, cy + 0.25, cx + 3.75, cy + 3.75]
        faces = {}
        andere = opt.get("op", ALL) if "tex" in opt else ()
        for f in ALL:
            if "tex" in opt and "op" in opt and f not in andere:
                face = {"texture": "#k", "uv": uv}
            elif "tex" in opt:
                face = {"texture": opt["tex"], "uv": opt.get("uv", [0, 0, 16, 16])}
            elif "op" in opt and f not in opt["op"]:
                continue
            else:
                face = {"texture": "#k", "uv": uv}
            if opt.get("gloed"):
                face.update(GLOED)
            faces[f] = face
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
        if opt.get("gloed"):
            e["shade"] = False
        if "rot" in opt:
            e["rotation"] = opt["rot"]
        els.append(e)
    return els, kleuren


def _display(vorm, soort):
    x0, y0, z0, x1, y1, z1 = vorm
    groot = max(x1 - x0, y1 - y0, (z1 - z0) if soort == "vloer" else 0, 4)
    s = round(min(1.25, 0.625 * 15 / groot), 3)
    cy = (y0 + y1) / 2
    if soort == "muur":
        return {"gui": {"rotation": [0, 180, 0], "translation": [0, round((8 - cy) * 1.0, 2), 0], "scale": [1, 1, 1]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, -7], "scale": [1, 1, 1]},
                "ground": {"rotation": [0, 180, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 2.5, -5], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 2, -6], "scale": [0.5, 0.5, 0.5]}}
    return {"gui": {"rotation": [30, 225, 0], "translation": [0, round((8 - cy) * s * 0.82, 2), 0], "scale": [s, s, s]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, round((8 - cy) * 0.6, 2), 0], "scale": [0.75, 0.75, 0.75]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]}}


def alle():
    """{block id (without the namespace prefix): (kind, boxes, textures, licht, effect)} of everything this module draws."""
    out = {}
    for sid, spec in SOUVENIRS.items():
        out[f"{N}_{sid}"] = spec
    for sid, spec in EXTRA.items():
        out[f"{N}_{sid}"] = spec
    return out


def vormen():
    """{block id: {"muur", "vorm" [x0, y0, z0, x1, y1, z1], "licht", "effect"}}: written as data/guhs/reisbureau/vormen.json; the
    Java table (reisbureau/Souvenirs.java) has the same numbers and a game test compares the two."""
    return {bid: {"muur": soort == "muur", "vorm": _vorm(dozen, soort), "licht": licht, "effect": effect}
            for bid, (soort, dozen, _t, licht, effect) in alle().items()}


def build(h, lib, namen):
    """Models, textures, blockstates, item models, loot and names. namen: {block id: (name, lore)}."""
    tex.build(h)
    for seed, (bid, (soort, dozen, textures, _licht, _effect)) in enumerate(alle().items()):
        els, kleuren = elementen(dozen)
        h.save(_swatches(kleuren, 8600 + seed), "block", f"{bid}.png")
        t = {"particle": f"guhs:block/{bid}", "k": f"guhs:block/{bid}", **textures}
        naam, lore = namen[bid]
        vorm = _vorm(dozen, soort)
        (lib.muurdeco if soort == "muur" else lib.deco)(h, bid, els, t, naam, lore, display=_display(vorm, soort))
    # the Reisbalie (its blockstate has the extra property vast: the counter of the Reisbureau itself cannot be broken)
    els, kleuren = elementen(BALIE)
    bid = f"{N}_balie"
    h.save(_swatches(kleuren, 8590), "block", f"{bid}.png")
    h.w(f"{h.A}/models/block/{bid}.json", {"parent": "minecraft:block/block",
                                           "textures": {"particle": f"guhs:block/{bid}", "k": f"guhs:block/{bid}", "logo": f"guhs:block/{N}_logo"},
                                           "elements": els})
    h.w(f"{h.A}/blockstates/{bid}.json", {"variants": {**h.facing_states(bid, extra=",vast=false"), **h.facing_states(bid, extra=",vast=true")}})
    h.w(f"{h.A}/models/item/{bid}.json", {"parent": f"guhs:block/{bid}"})
    h.self_drop(bid)
    h.w(f"{h.D}/{N}/vormen.json", vormen())
