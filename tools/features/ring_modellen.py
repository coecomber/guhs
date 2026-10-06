"""
bbq2 (ring-kern) - the models, textures and animation files of the cast of "In de ban van de Knabbelring".

  NPC kinds (SittingGuhRenderers.NPC_MODELEN, registered in feature/ring/client/RingClient):
    guhdalf     the sitting guh as a wizard: a tall pointy hat with a bent tip, a long beard and bushy eyebrows, a cloak and a
                staff with a knabbel crystal. The hat, the cloak and the crystal exist twice (bones grijs_* and wit_*):
                Guhdalf de Grijze and, after the mine, Guhdalf de Witte (the client shows one set).
    araguh      the ranger-king: a weather-beaten hood and cloak, a stubbly chin, a sword on his back, and (bone
                araguh_kroon, shown at the end) the knabbel crown.
    leguhlas    the elf: long blond hair, pointy ear tips, a bow and a quiver.
    gimguh      the dwarf: a horned iron helmet, a huge braided red beard, an axe.
    merrie, pippguh   two young guhs with curly tufts and scarves; Merrie holds a knabbel, Pippguh a little bucket.
    guhrond     the elf lord: long dark hair, a silver circlet with a gem, a wine-red mantle.
    guhladriel  the lady of the tree city: long golden hair, a star circlet, a white gown.
    boromika    a Mika (the Mika's model) who truly wants to help: a round shield on his back, the horn at his side, a fur
                collar; own animation file for the Mika's bones.
    smikagol    a thin, pale Mika with huge sad eyes, three hairs and a loincloth; the same model is the walking entity
                guhs:smikagol (geo smikagol + its own animations: idle, loop, kruip, vis, grijp).
  Entity guhs:knekel_ruiter: one of the Nine: the Knekel-Mika's skeleton in a tattered hood and cloak on a smoke-black
  steed with ember eyes (geo knekel_ruiter + animations idle, draf, verblind).
  Sam-guh's variant bones and the four reward outfits are in ring.py (BONES / variants / clothes), drawn by make_guh_variants.

build(h) writes geckolib/models/entity/*.geo.json, geckolib/animations/entity/*.animation.json and textures/entity/*.png.
preview(out) renders every model from two sides (python tools/features/ring_modellen.py <out>).
"""
import copy
import json
import math
import os
import sys

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as kn

GEO = ("geckolib", "models", "entity")
ANIM = ("geckolib", "animations", "entity")


# =====================================================================================================================
# helpers
# =====================================================================================================================
def _bone(geo, name):
    for b in geo["bones"]:
        if b["name"] == name:
            return b
    raise KeyError(name)


def _src(h, name):
    return Image.open(os.path.join(h.TEX, "entity", name)).convert("RGBA")


def _spiegel(cubes):
    """The same cubes on the other side (x -> -x)."""
    out = []
    for o, s, sw, *rest in cubes:
        out.append(([-(o[0] + s[0]), o[1], o[2]], list(s), sw, *rest))
    return out


def _cubes(sw, cubes):
    return [kn._cube(list(o), list(s), sw[name], *(rest[:1] if rest else [])) for o, s, name, *rest in cubes]


def _deel(geo, sw, name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "parent": parent, "pivot": pivot, "cubes": _cubes(sw, cubes)}
    if rotation:
        b["rotation"] = rotation
    geo["bones"].append(b)


def strepen(donker=0.82, stap=4):
    def p(block):
        for y in range(0, 32, stap):
            block[y, :, :3] = (block[y, :, :3] * donker).astype(np.uint8)
    return p


def haar(donker=0.8):
    """Hair / beard: fine vertical strands."""
    def p(block):
        for x in range(0, 32, 3):
            block[:, x, :3] = (block[:, x, :3] * donker).astype(np.uint8)
        for x in range(1, 32, 7):
            block[:, x, :3] = np.clip(block[:, x, :3].astype(np.int32) + 16, 0, 255).astype(np.uint8)
    return p


def rand(kleur, breed=3):
    def p(block):
        block[:breed, :, :3] = kleur
        block[-breed:, :, :3] = kleur
        block[:, :breed, :3] = kleur
        block[:, -breed:, :3] = kleur
    return p


def steen(kleur, glans=(255, 255, 255)):
    """A cut gem / crystal: a bright diamond in the middle."""
    def p(block):
        for y in range(32):
            for x in range(32):
                d = abs(x - 15.5) + abs(y - 15.5)
                if d < 9:
                    block[y, x, :3] = glans if d < 3 else kleur
    return p


def houtnerf(donker=0.85):
    def p(block):
        for x in range(0, 32, 5):
            block[:, x:x + 1, :3] = (block[:, x:x + 1, :3] * donker).astype(np.uint8)
        block[14:16, :, :3] = (block[14:16, :, :3] * 0.9).astype(np.uint8)
    return p


def metaal(licht=30):
    def p(block):
        for i in range(32):
            j = (i * 2) % 32
            block[i, max(0, j - 2):j + 2, :3] = np.clip(block[i, max(0, j - 2):j + 2, :3].astype(np.int32) + licht, 0, 255).astype(np.uint8)
        block[:2, :, :3] = np.clip(block[:2, :, :3].astype(np.int32) + licht, 0, 255).astype(np.uint8)
        block[-2:, :, :3] = (block[-2:, :, :3] * 0.75).astype(np.uint8)
    return p


def rafels(block):
    """A tattered hem: see-through bites out of the lower edge."""
    for x in range(0, 32, 4):
        diep = 3 + (x * 7) % 6
        block[32 - diep:, x:x + 2, 3] = 0


def sterren(kleur=(255, 250, 200)):
    def p(block):
        for (x, y) in ((6, 6), (22, 9), (13, 20), (26, 25), (4, 26)):
            block[y, x - 1:x + 2, :3] = kleur
            block[y - 1:y + 2, x, :3] = kleur
    return p


def boompje(block):
    """The white tree of Guhndor (a knabbel tree): on the shield."""
    wit = (244, 240, 226)
    block[10:27, 15:17, :3] = wit
    for y, w in ((6, 7), (8, 9), (10, 8), (12, 6), (14, 4)):
        block[y:y + 2, 16 - w:16 + w, :3] = wit
    block[26:28, 11:21, :3] = wit
    for (x, y) in ((6, 4), (25, 4), (3, 12), (28, 12)):
        block[y:y + 2, x:x + 2, :3] = (250, 214, 90)


def _schilder(h, bron, vacht, sw, kleuren, seed, ogen=None):
    """The texture: the base sheet with its pink fur turned into `vacht` (hue, sat, val), then every swatch painted."""
    img = _src(h, bron)
    if vacht is not None:
        img = h.recolour(img, hue=vacht[0], sat=vacht[1], val=vacht[2], only=h.pinkish)
    a = np.asarray(img.convert("RGBA")).copy()
    if ogen:
        ogen(a)
    rng = np.random.default_rng(seed)
    for name, spec in kleuren.items():
        kleur, var, patroon = spec
        kn._paint(a, sw[name], kleur, rng, var, patroon)
    return a


def _zittend(h, kind):
    geo_file = kn._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
    geo["description"]["visible_bounds_width"] = 4
    geo["description"]["visible_bounds_height"] = 3.5
    geo["description"]["visible_bounds_offset"] = [0, 1.5, 0]
    return geo_file, geo


def _bewaar(h, kind, geo_file, a, tex=None):
    kn._save_geo(h, f"guh_npc_{kind}.geo.json", geo_file)
    h.save(Image.fromarray(a), "entity", f"{tex or 'npc_' + kind}.png")


HOOFD = [0, 13, 0]          # the sitting guh's head pivot
LIJF = [0, 2, 0]
ARM_R = [-3.5, 10, -4]
ARM_L = [3.5, 10, -4]


def _elfenoren(geo, sw, name):
    """Pointy tips on the guh ears."""
    links = [([9.4, 28.6, -1.6], [2.6, 2.4, 1.1], name), ([10.6, 30.6, -1.5], [1.6, 2.0, 0.9], name)]
    _bone(geo, "ear_left")["cubes"] += _cubes(sw, links)
    _bone(geo, "ear_right")["cubes"] += _cubes(sw, _spiegel(links))


def _lang_haar(sw, name, tot=4.0, breed=7.6):
    """Long hair: a cap on the crown, a curtain down the back of the head to y = tot, and two locks along the cheeks."""
    return [([-breed, 24.6, -5.6], [breed * 2, 1.9, 12.2], name), ([-breed + 0.6, 26.2, -4.4], [breed * 2 - 1.2, 0.8, 9.6], name),
            ([-breed, tot + 6, 5.8], [breed * 2, 24.8 - tot - 6, 1.4], name), ([-breed + 1, tot, 6.2], [breed * 2 - 2, 6.4, 1.2], name),
            ([breed - 0.4, 12.5, -4.8], [1.2, 12.4, 3.2], name), ([-breed - 0.8, 12.5, -4.8], [1.2, 12.4, 3.2], name)]


# =====================================================================================================================
# the sitting cast
# =====================================================================================================================
def guhdalf(h):
    geo_file, geo = _zittend(h, "guhdalf")
    sw = kn._swatches(geo, ["baard", "wenkbrauw", "staf", "hoed_grijs", "band_grijs", "mantel_grijs", "knop_grijs",
                            "hoed_wit", "band_wit", "mantel_wit", "knop_wit"])
    _deel(geo, sw, "guhdalf_baard", "head", HOOFD, [
        ([-3.8, 14.3, -8.3], [7.6, 1.3, 0.9], "baard"),                 # the moustache, under the nose
        ([-4.6, 9.0, -8.2], [9.2, 5.4, 1.5], "baard"), ([-3.6, 5.2, -8.0], [7.2, 3.9, 1.3], "baard"),
        ([-2.4, 2.4, -7.8], [4.8, 2.9, 1.1], "baard"), ([-1.2, 0.6, -7.6], [2.4, 1.9, 0.9], "baard"),
        ([-7.4, 21.4, -7.5], [5.2, 1.4, 0.8], "wenkbrauw"), ([2.2, 21.4, -7.5], [5.2, 1.4, 0.8], "wenkbrauw")])
    _deel(geo, sw, "guhdalf_staf", "arm_right", ARM_R, [([-5.7, 0.5, -7.6], [1.2, 27.5, 1.2], "staf"),
                                                        ([-6.3, 26.6, -8.2], [2.4, 0.8, 2.4], "staf")])
    for look in ("grijs", "wit"):
        _deel(geo, sw, f"{look}_hoed", "head", HOOFD, [
            ([-9.5, 25.4, -9.0], [19, 0.9, 17], f"hoed_{look}"), ([-5.6, 26.3, -5.8], [11.2, 3.4, 10.6], f"hoed_{look}"),
            ([-5.7, 26.3, -5.9], [11.4, 1.5, 10.8], f"band_{look}"),
            ([-4.2, 29.7, -4.2], [8.4, 3.4, 7.8], f"hoed_{look}"), ([-3.0, 33.1, -2.6], [6.0, 3.2, 5.4], f"hoed_{look}"),
            ([-1.9, 36.3, -1.0], [3.8, 2.8, 3.6], f"hoed_{look}"), ([-1.1, 38.5, 0.9], [2.2, 1.8, 3.0], f"hoed_{look}"),
            ([-0.7, 37.6, 3.2], [1.4, 1.6, 1.6], f"hoed_{look}")])
        _deel(geo, sw, f"{look}_mantel", "body", LIJF, [
            ([-6.2, 1.2, 3.6], [12.4, 11.6, 1.3], f"mantel_{look}"), ([-6.6, 11.6, -3.8], [13.2, 1.5, 8.6], f"mantel_{look}"),
            ([-6.6, 6.0, -3.6], [1.3, 5.8, 7.2], f"mantel_{look}"), ([5.3, 6.0, -3.6], [1.3, 5.8, 7.2], f"mantel_{look}")])
        _deel(geo, sw, f"{look}_knop", "arm_right", ARM_R, [([-6.5, 27.4, -8.4], [2.8, 2.8, 2.8], f"knop_{look}"),
                                                            ([-5.9, 30.2, -7.8], [1.6, 1.2, 1.6], f"knop_{look}")])
    a = _schilder(h, "guh_sitting.png", (0.62, 0.10, 0.90), sw, {
        "baard": ((232, 232, 236), 10, haar(0.86)), "wenkbrauw": ((214, 214, 220), 8, haar(0.8)), "staf": ((122, 88, 58), 8, houtnerf()),
        "hoed_grijs": ((96, 104, 128), 10, None), "band_grijs": ((60, 66, 86), 6, None), "mantel_grijs": ((112, 116, 132), 10, strepen(0.9, 6)),
        "knop_grijs": ((246, 196, 70), 8, steen((255, 226, 120))),
        "hoed_wit": ((246, 246, 250), 6, None), "band_wit": ((214, 220, 236), 5, None), "mantel_wit": ((250, 250, 252), 6, strepen(0.95, 6)),
        "knop_wit": ((200, 240, 255), 6, steen((255, 255, 255)))}, 21301551)
    _bewaar(h, "guhdalf", geo_file, a)


def araguh(h):
    geo_file, geo = _zittend(h, "araguh")
    sw = kn._swatches(geo, ["kap", "mantel", "stoppel", "zwaard", "gevest", "riem", "kroon", "kroon_steen"])
    _deel(geo, sw, "araguh_kap", "head", HOOFD, [
        ([-8.6, 24.6, -6.2], [17.2, 2.2, 13.0], "kap"), ([-8.7, 13.0, 5.6], [17.4, 12.6, 1.5], "kap"),
        ([-8.9, 14.0, -3.0], [1.4, 11.6, 9.0], "kap"), ([7.5, 14.0, -3.0], [1.4, 11.6, 9.0], "kap"),
        ([-8.6, 23.6, -7.4], [17.2, 2.4, 1.4], "kap"),
        ([-3.4, 12.4, -7.6], [6.8, 2.4, 0.7], "stoppel")])                # a stubbly chin
    _deel(geo, sw, "araguh_mantel", "body", LIJF, [
        ([-6.4, 0.8, 3.6], [12.8, 12.2, 1.4], "mantel"), ([-6.8, 11.4, -3.9], [13.6, 1.6, 8.8], "mantel"),
        ([-5.4, 4.2, -4.4], [10.8, 1.3, 8.6], "riem")])
    _deel(geo, sw, "araguh_zwaard", "body", LIJF, [
        ([4.2, 2.0, 5.0], [1.4, 15.0, 0.9], "zwaard"), ([2.9, 16.4, 4.7], [4.0, 1.1, 1.5], "gevest"), ([4.2, 17.5, 4.9], [1.4, 3.2, 1.1], "gevest")],
        rotation=[0, 0, -22])
    # the knabbel crown (only shown when he is crowned): a ring of little golden wedges
    _deel(geo, sw, "araguh_kroon", "head", HOOFD, [
        ([-5.2, 26.0, -5.0], [10.4, 1.6, 9.6], "kroon"), ([-5.2, 27.6, -5.0], [1.8, 2.2, 1.8], "kroon"), ([3.4, 27.6, -5.0], [1.8, 2.2, 1.8], "kroon"),
        ([-5.2, 27.6, 2.8], [1.8, 2.2, 1.8], "kroon"), ([3.4, 27.6, 2.8], [1.8, 2.2, 1.8], "kroon"), ([-1.1, 27.6, -5.2], [2.2, 3.2, 1.8], "kroon"),
        ([-0.8, 26.3, -5.4], [1.6, 1.2, 0.6], "kroon_steen")])
    a = _schilder(h, "guh_sitting.png", (0.07, 0.50, 0.80), sw, {
        "kap": ((62, 82, 60), 12, None), "mantel": ((54, 72, 54), 12, strepen(0.9, 5)), "stoppel": ((70, 54, 44), 16, haar(0.7)),
        "zwaard": ((206, 212, 222), 6, metaal()), "gevest": ((112, 80, 50), 8, None), "riem": ((92, 62, 40), 8, None),
        "kroon": ((250, 204, 70), 10, metaal(24)), "kroon_steen": ((236, 70, 80), 4, steen((255, 150, 150)))}, 21301552)
    _bewaar(h, "araguh", geo_file, a)


def leguhlas(h):
    geo_file, geo = _zittend(h, "leguhlas")
    sw = kn._swatches(geo, ["haar", "oor", "boog", "pees", "koker", "pijl", "tuniek", "speld"])
    _elfenoren(geo, sw, "oor")
    _deel(geo, sw, "leguhlas_haar", "head", HOOFD, _lang_haar(sw, "haar", tot=5.0))
    _deel(geo, sw, "leguhlas_tuniek", "body", LIJF, [([-5.6, 9.6, -4.6], [11.2, 3.2, 9.2], "tuniek"), ([-1.0, 10.2, -5.0], [2.0, 2.0, 0.6], "speld")])
    # the bow in his left paw (a flat arc of three pieces) and the quiver on his back
    _deel(geo, sw, "leguhlas_boog", "arm_left", ARM_L, [
        ([5.2, 6.4, -7.6], [0.9, 6.2, 0.9], "boog"), ([5.2, 12.0, -6.8], [0.9, 4.6, 0.9], "boog"), ([5.2, 2.2, -6.8], [0.9, 4.6, 0.9], "boog"),
        ([5.2, 15.8, -5.6], [0.9, 2.2, 0.9], "boog"), ([5.2, 0.8, -5.6], [0.9, 2.2, 0.9], "boog"), ([5.5, 1.6, -5.2], [0.3, 15.8, 0.3], "pees")])
    _deel(geo, sw, "leguhlas_koker", "body", LIJF, [
        ([-5.4, 4.0, 7.6], [3.2, 9.6, 2.8], "koker"), ([-4.9, 13.6, 8.1], [0.6, 3.4, 0.6], "pijl"), ([-3.6, 13.6, 8.8], [0.6, 2.6, 0.6], "pijl"),
        ([-4.2, 13.6, 7.8], [0.6, 4.0, 0.6], "pijl")], rotation=[0, 0, 14])
    a = _schilder(h, "guh_sitting.png", (0.12, 0.30, 1.04), sw, {
        "haar": ((246, 226, 150), 10, haar(0.88)), "oor": ((246, 222, 196), 6, None), "boog": ((150, 110, 66), 8, houtnerf()),
        "pees": ((240, 240, 230), 3, None), "koker": ((112, 78, 50), 8, strepen(0.85, 8)), "pijl": ((236, 236, 226), 6, None),
        "tuniek": ((84, 140, 88), 10, strepen(0.92, 5)), "speld": ((120, 200, 120), 5, steen((220, 255, 200)))}, 21301553)
    _bewaar(h, "leguhlas", geo_file, a)


def gimguh(h):
    geo_file, geo = _zittend(h, "gimguh")
    sw = kn._swatches(geo, ["helm", "helmrand", "hoorn", "baard", "vlecht", "ring", "bijl", "steel", "malien"])
    _deel(geo, sw, "gimguh_helm", "head", HOOFD, [
        ([-7.4, 23.4, -7.2], [14.8, 3.2, 13.6], "helm"), ([-6.0, 26.6, -5.8], [12.0, 1.8, 10.8], "helm"), ([-1.0, 21.0, -7.5], [2.0, 5.6, 1.0], "helmrand"),
        ([-7.6, 23.0, -7.4], [15.2, 1.2, 14.0], "helmrand"),
        ([7.2, 24.6, -1.6], [2.6, 2.2, 2.2], "hoorn"), ([9.2, 26.2, -1.4], [1.8, 2.6, 1.8], "hoorn"), ([10.0, 28.4, -1.2], [1.1, 2.0, 1.4], "hoorn"),
        ([-9.8, 24.6, -1.6], [2.6, 2.2, 2.2], "hoorn"), ([-11.0, 26.2, -1.4], [1.8, 2.6, 1.8], "hoorn"), ([-11.1, 28.4, -1.2], [1.1, 2.0, 1.4], "hoorn")])
    _deel(geo, sw, "gimguh_baard", "head", HOOFD, [
        ([-6.2, 9.4, -8.4], [12.4, 6.2, 1.9], "baard"), ([-5.2, 5.2, -8.2], [10.4, 4.4, 1.7], "baard"), ([-4.6, 14.4, -8.5], [9.2, 1.5, 1.1], "baard"),
        ([-4.4, 0.8, -8.0], [2.0, 4.6, 1.5], "vlecht"), ([2.4, 0.8, -8.0], [2.0, 4.6, 1.5], "vlecht"), ([-1.2, 1.6, -8.0], [2.4, 3.8, 1.5], "baard"),
        ([-4.6, 0.2, -8.2], [2.4, 0.9, 1.9], "ring"), ([2.2, 0.2, -8.2], [2.4, 0.9, 1.9], "ring")])
    _deel(geo, sw, "gimguh_malien", "body", LIJF, [([-5.4, 2.6, -4.4], [10.8, 9.8, 8.8], "malien")])
    _deel(geo, sw, "gimguh_bijl", "arm_right", ARM_R, [
        ([-5.6, 1.0, -7.6], [1.1, 17.0, 1.1], "steel"), ([-9.2, 13.4, -7.5], [3.8, 5.2, 0.9], "bijl"), ([-4.5, 13.4, -7.5], [3.2, 5.2, 0.9], "bijl"),
        ([-10.0, 14.2, -7.4], [0.9, 3.6, 0.7], "bijl")])
    a = _schilder(h, "guh_sitting.png", (0.04, 0.62, 0.74), sw, {
        "helm": ((150, 156, 168), 8, metaal()), "helmrand": ((214, 176, 80), 8, None), "hoorn": ((238, 228, 200), 6, strepen(0.9, 5)),
        "baard": ((196, 92, 44), 14, haar(0.82)), "vlecht": ((180, 78, 36), 10, strepen(0.75, 4)), "ring": ((240, 200, 80), 6, None),
        "bijl": ((196, 202, 214), 6, metaal()), "steel": ((104, 74, 48), 8, houtnerf()), "malien": ((126, 132, 146), 12, strepen(0.8, 3))}, 21301554)
    _bewaar(h, "gimguh", geo_file, a)


def _hobbit(h, kind, vacht, sjaal, haarkleur, ding, seed):
    geo_file, geo = _zittend(h, kind)
    sw = kn._swatches(geo, ["haar", "sjaal", "vest", "knoop", "ding", "ding2"])
    _deel(geo, sw, f"{kind}_haar", "head", HOOFD, [
        ([-5.4, 25.4, -6.0], [10.8, 1.8, 9.6], "haar"), ([-3.8, 27.0, -4.6], [3.4, 1.4, 3.2], "haar"), ([0.8, 27.0, -5.4], [3.6, 1.6, 3.6], "haar"),
        ([-1.6, 27.0, 0.2], [3.4, 1.2, 3.0], "haar"), ([-5.8, 23.6, -7.3], [3.0, 2.4, 1.0], "haar"), ([2.6, 24.2, -7.3], [3.4, 1.8, 1.0], "haar")])
    _deel(geo, sw, f"{kind}_sjaal", "body", LIJF, [
        ([-5.5, 11.6, -4.7], [11.0, 2.4, 9.4], "sjaal"), ([2.4, 6.4, -5.3], [2.4, 5.4, 1.1], "sjaal"), ([-5.2, 3.0, -4.5], [10.4, 6.4, 8.8], "vest"),
        ([-0.5, 7.4, -4.8], [1.0, 1.0, 0.5], "knoop"), ([-0.5, 5.0, -4.8], [1.0, 1.0, 0.5], "knoop")])
    if ding == "knabbel":
        _deel(geo, sw, f"{kind}_ding", "arm_left", ARM_L, [([1.4, 9.6, -8.8], [3.4, 3.0, 2.6], "ding"), ([2.4, 12.6, -8.2], [1.4, 0.8, 1.4], "ding2")])
    else:   # the bucket of the well
        _deel(geo, sw, f"{kind}_ding", "arm_left", ARM_L, [([1.2, 9.4, -9.4], [3.8, 3.6, 3.4], "ding"), ([1.0, 12.8, -9.6], [4.2, 0.6, 3.8], "ding2"),
                                                          ([2.8, 13.4, -8.2], [0.5, 2.2, 0.5], "ding2")])
    a = _schilder(h, "guh_sitting.png", vacht, sw, {
        "haar": (haarkleur, 16, haar(0.8)), "sjaal": (sjaal, 8, strepen(0.82, 4)), "vest": ((126, 92, 60), 10, None), "knoop": ((246, 206, 90), 4, None),
        "ding": ((250, 196, 70), 10, None) if ding == "knabbel" else ((150, 108, 70), 8, houtnerf()),
        "ding2": ((236, 150, 50), 8, None) if ding == "knabbel" else ((120, 124, 136), 6, metaal())}, seed)
    _bewaar(h, kind, geo_file, a)


def guhrond(h):
    geo_file, geo = _zittend(h, "guhrond")
    sw = kn._swatches(geo, ["haar", "oor", "diadeem", "steen", "mantel", "zoom", "kraag"])
    _elfenoren(geo, sw, "oor")
    _deel(geo, sw, "guhrond_haar", "head", HOOFD, _lang_haar(sw, "haar", tot=3.0))
    _deel(geo, sw, "guhrond_diadeem", "head", HOOFD, [
        ([-7.4, 22.6, -7.4], [14.8, 1.0, 0.8], "diadeem"), ([-7.6, 22.6, -7.0], [0.8, 1.0, 12.0], "diadeem"), ([6.8, 22.6, -7.0], [0.8, 1.0, 12.0], "diadeem"),
        ([-1.1, 21.6, -7.7], [2.2, 2.4, 0.8], "steen")])
    _deel(geo, sw, "guhrond_mantel", "body", LIJF, [
        ([-6.4, 0.6, 3.6], [12.8, 12.4, 1.4], "mantel"), ([-6.8, 11.2, -4.0], [13.6, 1.8, 9.0], "kraag"),
        ([-6.7, 1.0, -4.2], [1.4, 10.4, 8.0], "mantel"), ([5.3, 1.0, -4.2], [1.4, 10.4, 8.0], "mantel"),
        ([-6.5, 0.4, 3.5], [13.0, 1.2, 1.6], "zoom")])
    a = _schilder(h, "guh_sitting.png", (0.10, 0.22, 0.98), sw, {
        "haar": ((62, 48, 44), 10, haar(0.8)), "oor": ((238, 214, 196), 6, None), "diadeem": ((214, 220, 232), 6, metaal()),
        "steen": ((90, 150, 240), 4, steen((210, 235, 255))), "mantel": ((128, 36, 52), 10, strepen(0.9, 6)),
        "zoom": ((230, 196, 90), 6, None), "kraag": ((150, 48, 64), 8, rand((230, 196, 90), 2))}, 21301557)
    _bewaar(h, "guhrond", geo_file, a)


def guhladriel(h):
    geo_file, geo = _zittend(h, "guhladriel")
    sw = kn._swatches(geo, ["haar", "oor", "diadeem", "ster", "jurk", "zoom", "flesje"])
    _elfenoren(geo, sw, "oor")
    _deel(geo, sw, "guhladriel_haar", "head", HOOFD, _lang_haar(sw, "haar", tot=1.0, breed=8.0) + [
        ([-8.6, 6.0, 4.4], [2.2, 8.0, 2.0], "haar"), ([6.4, 6.0, 4.4], [2.2, 8.0, 2.0], "haar")])
    _deel(geo, sw, "guhladriel_diadeem", "head", HOOFD, [
        ([-7.6, 22.8, -7.4], [15.2, 0.8, 0.8], "diadeem"), ([-7.8, 22.8, -7.0], [0.8, 0.8, 12.0], "diadeem"), ([7.0, 22.8, -7.0], [0.8, 0.8, 12.0], "diadeem"),
        ([-1.3, 22.0, -7.8], [2.6, 2.6, 0.8], "ster"), ([-0.5, 24.6, -7.7], [1.0, 1.4, 0.6], "ster")])
    _deel(geo, sw, "guhladriel_jurk", "body", LIJF, [
        ([-5.6, 1.0, -4.6], [11.2, 11.8, 9.2], "jurk"), ([-7.0, 0.2, -5.4], [14.0, 2.6, 10.8], "jurk"), ([-7.1, 0.0, -5.5], [14.2, 0.9, 11.0], "zoom"),
        ([-5.7, 11.4, -4.7], [11.4, 1.2, 9.4], "zoom")])
    _deel(geo, sw, "guhladriel_flesje", "arm_right", ARM_R, [([-4.2, 10.2, -7.6], [1.8, 2.6, 1.8], "flesje"), ([-3.8, 12.8, -7.2], [1.0, 1.0, 1.0], "diadeem")])
    a = _schilder(h, "guh_sitting.png", (0.13, 0.14, 1.12), sw, {
        "haar": ((250, 222, 120), 10, haar(0.9)), "oor": ((250, 232, 214), 5, None), "diadeem": ((230, 236, 246), 5, metaal(20)),
        "ster": ((255, 250, 200), 3, steen((255, 255, 255), (255, 255, 255))), "jurk": ((244, 246, 252), 6, sterren((210, 226, 250))),
        "zoom": ((200, 220, 246), 5, None), "flesje": ((200, 244, 255), 4, steen((255, 255, 255)))}, 21301558)
    _bewaar(h, "guhladriel", geo_file, a)


# =====================================================================================================================
# the Mika's: Boromika, Smikagol
# =====================================================================================================================
def _mika(h, identifier):
    geo_file = kn._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.{identifier}"
    return geo_file, geo


def mika_anims(prefix, kop=-6, staart=14, sneller=1.0, verborgen=()):
    """The two animations the NPC renderer plays (idle, happy) for the Mika's bones."""
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.03, 1.02], "4.0": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [kop, 0, 0], "1.0": [kop - 2, 0, 3], "2.0": [kop, 0, 0], "3.0": [kop - 2, 0, -3], "4.0": [kop, 0, 0]}},
            "tail": {"rotation": {str(round(i * 0.5 / sneller, 3)): [0, staart if i % 2 else -staart, 0] for i in range(int(8 * sneller) + 1)}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -12], "2.9": [0, 0, 0], "4.0": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 12], "3.4": [0, 0, 0], "4.0": [0, 0, 0]}}}
    happy = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, 0], "0.3": [0, 0, 0], "0.45": [0, 1.5, 0], "0.6": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [kop, 0, 0], "0.3": [kop - 12, 0, 0], "0.6": [kop, 0, 0]}}}
    for bot in verborgen:
        idle[bot] = {"scale": {"0.0": [0, 0, 0]}}
        happy[bot] = {"scale": {"0.0": [0, 0, 0]}}
    return {"format_version": "1.8.0", "animations": {
        f"{prefix}.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        f"{prefix}.happy": {"loop": False, "animation_length": 0.6, "bones": happy}}}


def boromika(h):
    geo_file, geo = _mika(h, "guh_npc_boromika")
    sw = kn._swatches(geo, ["schild", "schildrand", "hoorn", "hoornpunt", "bont", "haar", "riem"])
    _deel(geo, sw, "boromika_schild", "body", [0, 5, 5], [
        ([-5.0, 10.2, 1.6], [10.0, 1.0, 10.0], "schild"), ([-5.6, 9.8, 1.0], [11.2, 0.8, 11.2], "schildrand"), ([-1.0, 11.2, 5.6], [2.0, 0.8, 2.0], "schildrand")])
    _deel(geo, sw, "boromika_hoorn", "body", [0, 5, 5], [
        ([6.4, 3.4, 2.0], [1.8, 1.8, 3.4], "hoorn"), ([6.6, 3.6, 5.4], [1.4, 1.4, 2.4], "hoorn"), ([6.8, 4.2, 7.6], [1.0, 1.0, 1.6], "hoornpunt"),
        ([6.3, 3.3, 1.2], [2.0, 2.0, 0.9], "hoornpunt"), ([6.6, 5.2, 2.4], [0.5, 4.6, 0.5], "riem")])
    _deel(geo, sw, "boromika_bont", "body", [0, 5, 5], [([-7.0, 7.6, -2.4], [14.0, 3.0, 3.6], "bont"), ([-6.4, 9.6, -1.6], [12.8, 1.2, 2.4], "bont")])
    _deel(geo, sw, "boromika_haar", "head", [0, 5, -2], [([-5.2, 13.6, -11.6], [10.4, 1.3, 7.6], "haar"), ([-5.6, 11.6, -12.6], [4.0, 2.6, 0.9], "haar"),
                                                        ([2.6, 12.2, -12.6], [3.2, 2.0, 0.9], "haar")])
    a = _schilder(h, "mika.png", (0.07, 0.55, 0.78), sw, {
        "schild": ((40, 44, 60), 6, boompje), "schildrand": ((196, 170, 96), 8, metaal(20)), "hoorn": ((240, 232, 210), 6, strepen(0.9, 6)),
        "hoornpunt": ((214, 214, 222), 5, metaal()), "bont": ((104, 76, 56), 18, haar(0.75)), "haar": ((86, 58, 40), 12, haar(0.8)),
        "riem": ((92, 62, 40), 6, None)}, 21301559)
    _bewaar(h, "boromika", geo_file, a)
    h.w(os.path.join(h.A, *ANIM, "guh_npc_boromika.animation.json"), mika_anims("animation.guh_sitting", kop=-4, staart=10))


def _smikagol_geo(h, identifier):
    """The Mika, starved: a narrow, low body, thin legs tucked under it, the big head left as it is, three hairs, a loincloth."""
    geo_file, geo = _mika(h, identifier)
    for name in ("body", "tail"):
        for c in _bone(geo, name)["cubes"]:
            c["origin"][0] *= 0.66
            c["size"][0] *= 0.66
            c["origin"][1] = c["origin"][1] * 0.8 + 0.4
            c["size"][1] *= 0.8
    for name, naar_binnen in (("leg_back_left", 1.9), ("leg_back_right", 1.9), ("leg_front_left", 0.65), ("leg_front_right", 0.65)):
        b = _bone(geo, name)
        links = b["pivot"][0] > 0
        b["pivot"][0] += -naar_binnen if links else naar_binnen
        for c in b["cubes"]:
            dun = c["size"][0] * 0.74
            c["origin"][0] += -naar_binnen if links else naar_binnen + (c["size"][0] - dun)
            c["size"][0] = dun
            c["size"][2] *= 0.9
    sw = kn._swatches(geo, ["haar", "doek", "vis"])
    _deel(geo, sw, "smikagol_haar", "head", [0, 5, -2], [([-2.2, 14.0, -8.0], [0.5, 3.4, 0.5], "haar"), ([0.2, 14.0, -7.0], [0.5, 4.2, 0.5], "haar"),
                                                        ([2.0, 14.0, -8.4], [0.5, 2.8, 0.5], "haar")])
    _deel(geo, sw, "smikagol_doek", "body", [0, 5, 5], [([-4.4, 1.2, 6.6], [8.8, 5.6, 4.4], "doek", 0.2)])
    # the fish he holds in his mouth when he has caught one (hidden unless he fishes / it is shown by the animator)
    _deel(geo, sw, "smikagol_vis", "head", [0, 5, -2], [([-2.6, 2.6, -14.2], [5.2, 1.6, 1.6], "vis"), ([2.4, 2.2, -14.0], [1.6, 2.4, 1.2], "vis")])
    return geo_file, geo, sw


def _smikagol_ogen(a, geo):
    """The Mika's angry red eyes become huge pale-blue moons (only on the front of his head: the rest of him keeps its marks)."""
    f = a.astype(np.float32) / 255
    r, g, b = f[..., 0], f[..., 1], f[..., 2]
    gezicht = np.zeros(a.shape[:2], bool)
    for c in _bone(geo, "head")["cubes"]:
        face = c["uv"].get("north") if isinstance(c.get("uv"), dict) else None
        if face:
            (u, v), (w, hh) = face["uv"], face["uv_size"]
            x0, x1, y0, y1 = sorted((int(u * 4), int((u + w) * 4))) + sorted((int(v * 4), int((v + hh) * 4)))
            gezicht[y0:y1, x0:x1] = True
    rood = (r > 0.55) & (g < 0.35) & (b < 0.35) & (a[..., 3] > 0) & gezicht
    a[rood, 0] = (a[rood, 0] * 0.55).astype(np.uint8)
    a[rood, 1] = np.clip(a[rood, 0].astype(np.int32) + 96, 0, 255).astype(np.uint8)
    a[rood, 2] = 250


def smikagol(h):
    kleuren = {"haar": ((60, 60, 56), 6, None), "doek": ((132, 110, 84), 12, rafels), "vis": ((150, 196, 214), 8, strepen(0.85, 6))}
    for identifier, bestand, tex in (("guh_npc_smikagol", "guh_npc_smikagol.geo.json", "npc_smikagol"), ("smikagol", "smikagol.geo.json", "smikagol")):
        geo_file, geo, sw = _smikagol_geo(h, identifier)
        img = _src(h, "mika.png")
        a = np.asarray(img).copy()
        _smikagol_ogen(a, geo)
        # (the eyes are blue now, so "pinkish" only finds the fur)
        a = np.asarray(h.recolour(Image.fromarray(a), hue=0.24, sat=0.30, val=0.82, only=h.pinkish).convert("RGBA")).copy()
        rng = np.random.default_rng(21301560)
        for name, (kleur, var, patroon) in kleuren.items():
            kn._paint(a, sw[name], kleur, rng, var, patroon)
        kn._save_geo(h, bestand, geo_file)
        h.save(Image.fromarray(a), "entity", f"{tex}.png")
    h.w(os.path.join(h.A, *ANIM, "guh_npc_smikagol.animation.json"), mika_anims("animation.guh_sitting", kop=6, staart=6, verborgen=("smikagol_vis",)))
    h.w(os.path.join(h.A, *ANIM, "smikagol.animation.json"), smikagol_anims())


def _poten(swing, lengte, fase=0.0):
    """A four-legged walk: the diagonal pairs swing against each other."""
    def poot(teken):
        return {"rotation": {"0.0": [teken * swing, 0, 0], str(round(lengte / 2, 3)): [-teken * swing, 0, 0], str(lengte): [teken * swing, 0, 0]}}
    return {"leg_front_left": poot(1), "leg_back_right": poot(1), "leg_front_right": poot(-1), "leg_back_left": poot(-1)}


def smikagol_anims():
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.03, 1.05, 1.02], "3.0": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [8, 0, 0], "0.75": [10, 14, 4], "1.5": [8, 0, 0], "2.25": [10, -14, -4], "3.0": [8, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, -8, 0], "1.5": [0, 8, 0], "3.0": [0, -8, 0]}},
            "smikagol_vis": {"scale": {"0.0": [0, 0, 0]}}}
    loop = {**_poten(32, 0.5), "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.6, 0], "0.5": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [4, 0, 3], "0.25": [4, 0, -3], "0.5": [4, 0, 3]}},
            "tail": {"rotation": {"0.0": [0, -14, 0], "0.25": [0, 14, 0], "0.5": [0, -14, 0]}},
            "smikagol_vis": {"scale": {"0.0": [0, 0, 0]}}}
    # creeping ahead on his belly: low, the head stretched forward, quick little paws
    kruip = {**_poten(22, 0.36), "root": {"position": {"0.0": [0, -1.1, 0]}},
             "body": {"rotation": {"0.0": [-4, 0, 0]}, "position": {"0.0": [0, 0, 0], "0.18": [0, 0.3, 0], "0.36": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [16, 10, 0], "0.18": [16, -10, 0], "0.36": [16, 10, 0]}, "position": {"0.0": [0, -0.4, -1.2]}},
             "tail": {"rotation": {"0.0": [10, -6, 0], "0.18": [10, 6, 0], "0.36": [10, -6, 0]}},
             "smikagol_vis": {"scale": {"0.0": [0, 0, 0]}}}
    # fishing: hunched at the water's edge, the head dips, now and then a fish in his mouth
    vis = {"body": {"rotation": {"0.0": [-6, 0, 0]}, "scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.04, 1.02], "4.0": [1, 1, 1]}},
           "head": {"rotation": {"0.0": [18, 0, 0], "1.6": [22, 6, 0], "2.4": [22, -6, 0], "3.0": [40, 0, 0], "3.2": [-6, 0, 0], "3.8": [-6, 0, 4], "4.0": [18, 0, 0]},
                    "position": {"0.0": [0, 0, 0], "3.0": [0, -1.2, -1.0], "3.2": [0, 0.4, 0], "4.0": [0, 0, 0]}},
           "tail": {"rotation": {"0.0": [0, -5, 0], "2.0": [0, 5, 0], "3.1": [0, 24, 0], "3.3": [0, -24, 0], "3.5": [0, 24, 0], "4.0": [0, -5, 0]}},
           "smikagol_vis": {"scale": {"0.0": [0, 0, 0], "3.05": [0, 0, 0], "3.1": [1, 1, 1], "3.9": [1, 1, 1], "4.0": [0, 0, 0]}}}
    grijp = {"root": {"position": {"0.0": [0, 0, 0], "0.25": [0, 2.4, -3.0], "0.5": [0, 0.6, -5.0], "0.9": [0, 0, -2.0], "1.5": [0, 0, 0]}},
             "body": {"rotation": {"0.0": [0, 0, 0], "0.25": [-18, 0, 0], "0.5": [10, 0, 0], "1.5": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [8, 0, 0], "0.25": [-20, 0, 0], "0.5": [26, 0, 0], "0.9": [-10, 0, 8], "1.2": [-10, 0, -8], "1.5": [8, 0, 0]}},
             "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [-60, 0, -12], "0.5": [-70, 0, 14], "1.5": [0, 0, 0]}},
             "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-60, 0, 12], "0.5": [-70, 0, -14], "1.5": [0, 0, 0]}},
             "smikagol_vis": {"scale": {"0.0": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.smikagol.idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "animation.smikagol.loop": {"loop": True, "animation_length": 0.5, "bones": loop},
        "animation.smikagol.kruip": {"loop": True, "animation_length": 0.36, "bones": kruip},
        "animation.smikagol.vis": {"loop": True, "animation_length": 4.0, "bones": vis},
        "animation.smikagol.grijp": {"loop": False, "animation_length": 1.5, "bones": grijp}}}


# =====================================================================================================================
# the Nine: a Knekel-Mika rider
# =====================================================================================================================
ZADEL = 5.4          # how far the Knekel-Mika's upper body is lifted onto the steed's back


def knekel_ruiter(h):
    geo_file = kn._load(h, "knekel_mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"].update(identifier="geometry.knekel_ruiter", visible_bounds_width=4, visible_bounds_height=4, visible_bounds_offset=[0, 1.8, 0])
    geo["bones"] = [b for b in geo["bones"] if b["name"] not in ("fork", "horns", "leg_left", "leg_right")]
    # the rider sits: everything of him goes up onto the steed's back, and he hangs on the steed (bone ros)
    for b in geo["bones"]:
        if b["name"] == "root":
            continue
        b["pivot"] = [b["pivot"][0], b["pivot"][1] + ZADEL, b["pivot"][2]]
        for c in b.get("cubes", []):
            c["origin"] = [c["origin"][0], c["origin"][1] + ZADEL, c["origin"][2]]
        if b.get("parent") == "root":
            b["parent"] = "ros"
    sw = kn._swatches(geo, ["ros", "ros_buik", "manen", "oog", "hoef", "kap", "mantel", "bot", "teugel"])
    ros = {"name": "ros", "parent": "root", "pivot": [0, 12, 4], "cubes": _cubes(sw, [
        ([-4.0, 10.0, -9.0], [8.0, 8.0, 18.0], "ros"), ([-3.4, 9.2, -8.0], [6.8, 1.0, 16.0], "ros_buik"),
        ([-4.3, 17.6, -3.4], [8.6, 0.9, 7.6], "mantel")])}                       # (a saddle cloth)
    geo["bones"].insert(1, ros)
    _deel(geo, sw, "ros_hals", "ros", [0, 16, -8], [([-2.2, 14.0, -12.6], [4.4, 9.6, 5.0], "ros"), ([-0.6, 15.0, -8.2], [1.2, 9.4, 1.6], "manen"),
                                                    ([-0.5, 24.0, -11.4], [1.0, 1.6, 3.6], "manen")], rotation=[-14, 0, 0])
    _deel(geo, sw, "ros_hoofd", "ros_hals", [0, 23, -11], [
        ([-2.4, 20.4, -19.4], [4.8, 4.8, 8.4], "ros"), ([-2.0, 20.0, -21.6], [4.0, 3.6, 2.4], "ros_buik"),
        ([-2.55, 23.0, -16.4], [0.4, 1.4, 1.8], "oog"), ([2.15, 23.0, -16.4], [0.4, 1.4, 1.8], "oog"),
        ([-2.2, 25.0, -13.4], [1.2, 2.4, 1.0], "ros"), ([1.0, 25.0, -13.4], [1.2, 2.4, 1.0], "ros"),
        ([-2.7, 21.4, -20.0], [5.4, 0.6, 0.6], "teugel")], rotation=[20, 0, 0])
    for name, x, z in (("ros_poot_vl", 2.0, -8.0), ("ros_poot_vr", -4.2, -8.0), ("ros_poot_al", 2.0, 6.0), ("ros_poot_ar", -4.2, 6.0)):
        _deel(geo, sw, name, "ros", [x + 1.1, 10.5, z + 1.2], [([x, 1.0, z], [2.2, 9.6, 2.4], "ros"), ([x - 0.2, 0.0, z - 0.3], [2.6, 1.2, 3.0], "hoef")])
    _deel(geo, sw, "ros_staart", "ros", [0, 16.5, 9], [([-1.4, 10.0, 8.6], [2.8, 7.4, 2.4], "manen"), ([-1.0, 6.4, 9.6], [2.0, 4.0, 1.8], "manen")],
          rotation=[18, 0, 0])
    # his bony legs down the steed's flanks
    for name, x in (("leg_left", 4.0), ("leg_right", -5.8)):
        _deel(geo, sw, name, "ros", [x + 0.9, 18.0, 0.0], [([x, 9.4, -1.6], [1.8, 9.0, 2.2], "bot"), ([x - 0.2, 8.2, -3.0], [2.2, 1.4, 3.6], "bot")])
    # the hood (the ears stick out through it) and the tattered cloak
    hy = 28.0 + ZADEL
    _deel(geo, sw, "kap", "head", [0.0, 31.75 + ZADEL, 3.5], [
        ([-6.7, hy + 10.2, -4.9], [13.4, 1.0, 11.0], "kap"), ([-6.7, hy - 0.6, 5.2], [13.4, 11.4, 1.0], "kap"),
        ([-6.8, hy - 0.6, -3.6], [1.0, 11.0, 9.0], "kap"), ([5.8, hy - 0.6, -3.6], [1.0, 11.0, 9.0], "kap"),
        ([-6.7, hy + 8.4, -5.6], [13.4, 2.0, 1.2], "kap")])
    _deel(geo, sw, "mantel", "body", [0, 27.0 + ZADEL, 2.6], [
        ([-5.6, 14.0 + ZADEL, 2.4], [11.2, 13.4, 1.0], "mantel"), ([-5.9, 25.6 + ZADEL, -2.6], [11.8, 1.2, 5.6], "mantel")], rotation=[10, 0, 0])
    kn._save_geo(h, "knekel_ruiter.geo.json", geo_file)
    a = np.asarray(_src(h, "knekel_mika.png")).copy()
    rng = np.random.default_rng(21301561)

    def gloed(block):
        block[..., :3] = (255, 120, 30)
        block[10:22, 10:22, :3] = (255, 226, 120)

    def as_barsten(block):
        for i in range(6):
            x = (i * 11 + 5) % 30
            for y in range(4 + i * 3, min(32, 12 + i * 4)):
                block[y, (x + (y % 3)) % 32, :3] = (214, 88, 30)

    for name, (kleur, var, patroon) in {
            "ros": ((30, 28, 34), 10, as_barsten), "ros_buik": ((46, 42, 50), 10, None), "manen": ((84, 80, 92), 22, haar(0.6)), "oog": ((255, 140, 40), 2, gloed),
            "hoef": ((14, 12, 16), 4, None), "kap": ((24, 20, 30), 8, None), "mantel": ((30, 26, 38), 10, rafels), "bot": ((70, 68, 72), 10, strepen(0.8, 5)),
            "teugel": ((90, 30, 24), 6, None)}.items():
        kn._paint(a, sw[name], kleur, rng, var, patroon)
    h.save(Image.fromarray(a), "entity", "knekel_ruiter.png")
    h.w(os.path.join(h.A, *ANIM, "knekel_ruiter.animation.json"), ruiter_anims())


def ruiter_anims():
    def poot(a, b, lengte):
        return {"rotation": {"0.0": [a, 0, 0], str(round(lengte / 2, 3)): [b, 0, 0], str(lengte): [a, 0, 0]}}
    idle = {"ros": {"scale": {"0.0": [1, 1, 1], "1.6": [1.02, 1.02, 1.01], "3.2": [1, 1, 1]}},
            "ros_hoofd": {"rotation": {"0.0": [0, 0, 0], "0.8": [6, 8, 0], "1.6": [0, 0, 0], "2.4": [6, -8, 0], "3.2": [0, 0, 0]}},
            "ros_staart": {"rotation": {"0.0": [0, 0, -6], "1.6": [0, 0, 6], "3.2": [0, 0, -6]}},
            "head": {"rotation": {"0.0": [4, -18, 0], "1.6": [4, 18, 0], "3.2": [4, -18, 0]}},
            "mantel": {"rotation": {"0.0": [0, 0, 0], "1.6": [3, 0, 0], "3.2": [0, 0, 0]}},
            "arm_left": {"rotation": {"0.0": [-34, 0, 0]}}, "arm_right": {"rotation": {"0.0": [-34, 0, 0]}}}
    draf = {"ros": {"position": {"0.0": [0, 0, 0], "0.15": [0, 1.4, 0], "0.3": [0, 0, 0], "0.45": [0, 1.4, 0], "0.6": [0, 0, 0]},
                    "rotation": {"0.0": [-3, 0, 0], "0.3": [3, 0, 0], "0.6": [-3, 0, 0]}},
            "ros_poot_vl": poot(38, -30, 0.6), "ros_poot_ar": poot(34, -28, 0.6), "ros_poot_vr": poot(-30, 38, 0.6), "ros_poot_al": poot(-28, 34, 0.6),
            "ros_hals": {"rotation": {"0.0": [4, 0, 0], "0.3": [-6, 0, 0], "0.6": [4, 0, 0]}},
            "ros_staart": {"rotation": {"0.0": [26, 0, -8], "0.3": [34, 0, 8], "0.6": [26, 0, -8]}},
            "body": {"rotation": {"0.0": [12, 0, 0], "0.3": [16, 0, 0], "0.6": [12, 0, 0]}},
            "head": {"rotation": {"0.0": [-10, 0, 0]}},
            "mantel": {"rotation": {"0.0": [34, 0, 0], "0.15": [44, 0, 4], "0.3": [36, 0, 0], "0.45": [46, 0, -4], "0.6": [34, 0, 0]}},
            "arm_left": {"rotation": {"0.0": [-52, 0, 0], "0.3": [-46, 0, 0], "0.6": [-52, 0, 0]}},
            "arm_right": {"rotation": {"0.0": [-52, 0, 0], "0.3": [-46, 0, 0], "0.6": [-52, 0, 0]}}}
    # blinded by the Lichtflesje: the steed rears, the rider's paws are over his eye sockets, the head shakes
    verblind = {"ros": {"rotation": {"0.0": [-16, 0, 0], "0.5": [-22, 0, 0], "1.0": [-16, 0, 0]}},
                "ros_poot_vl": poot(-50, -30, 1.0), "ros_poot_vr": poot(-30, -50, 1.0),
                "ros_poot_al": {"rotation": {"0.0": [16, 0, 0]}}, "ros_poot_ar": {"rotation": {"0.0": [16, 0, 0]}},
                "ros_hoofd": {"rotation": {"0.0": [-10, 14, 0], "0.25": [-10, -14, 0], "0.5": [-10, 14, 0], "0.75": [-10, -14, 0], "1.0": [-10, 14, 0]}},
                "body": {"rotation": {"0.0": [14, 0, 0]}},
                "head": {"rotation": {"0.0": [14, 16, 0], "0.25": [14, -16, 0], "0.5": [14, 16, 0], "0.75": [14, -16, 0], "1.0": [14, 16, 0]}},
                "arm_left": {"rotation": {"0.0": [-165, 0, 26]}}, "arm_right": {"rotation": {"0.0": [-165, 0, -26]}},
                "mantel": {"rotation": {"0.0": [-8, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.knekel_ruiter.idle": {"loop": True, "animation_length": 3.2, "bones": idle},
        "animation.knekel_ruiter.draf": {"loop": True, "animation_length": 0.6, "bones": draf},
        "animation.knekel_ruiter.verblind": {"loop": True, "animation_length": 1.0, "bones": verblind}}}


# =====================================================================================================================
KINDS = ("guhdalf", "smikagol", "araguh", "leguhlas", "gimguh", "boromika", "merrie", "pippguh", "guhrond", "guhladriel")


def build(h):
    guhdalf(h)
    araguh(h)
    leguhlas(h)
    gimguh(h)
    _hobbit(h, "merrie", (0.09, 1.25, 0.93), (84, 150, 90), (176, 124, 70), "knabbel", 21301555)
    _hobbit(h, "pippguh", (0.07, 1.05, 0.86), (70, 110, 200), (120, 84, 54), "emmer", 21301556)
    guhrond(h)
    guhladriel(h)
    boromika(h)
    smikagol(h)
    knekel_ruiter(h)


def check(h):
    problems = []
    for kind in KINDS:
        for path in (os.path.join(h.A, *GEO, f"guh_npc_{kind}.geo.json"), os.path.join(h.TEX, "entity", f"npc_{kind}.png")):
            if not os.path.exists(path):
                problems.append(path)
    for name in ("smikagol", "knekel_ruiter"):
        geo = os.path.join(h.A, *GEO, f"{name}.geo.json")
        anim = os.path.join(h.A, *ANIM, f"{name}.animation.json")
        if not os.path.exists(geo) or not os.path.exists(anim) or not os.path.exists(os.path.join(h.TEX, "entity", f"{name}.png")):
            problems.append(name)
            continue
        bones = {b["name"] for b in json.load(open(geo, encoding="utf-8"))["minecraft:geometry"][0]["bones"]}
        for anim_name, a in json.load(open(anim, encoding="utf-8"))["animations"].items():
            for bone in a["bones"]:
                if bone not in bones:
                    problems.append(f"{anim_name}: no bone {bone}")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
def preview(out, only=()):
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    shots = [(f"guh_npc_{k}", f"guhs:entity/npc_{k}", ()) for k in KINDS]
    shots = [s for s in shots if s[0] not in ("guh_npc_guhdalf", "guh_npc_araguh") and (not only or s[0] in only)]
    shots += [s for s in [("guh_npc_guhdalf", "guhs:entity/npc_guhdalf", ("wit_",)), ("guh_npc_guhdalf", "guhs:entity/npc_guhdalf", ("grijs_",)),
                          ("guh_npc_araguh", "guhs:entity/npc_araguh", ("araguh_kroon",)), ("guh_npc_araguh", "guhs:entity/npc_araguh", ()),
                          ("smikagol", "guhs:entity/smikagol", ("smikagol_vis",)), ("knekel_ruiter", "guhs:entity/knekel_ruiter", ())]
              if not only or s[0] in only]
    for model, tex, hide in shots:
        verborgen = [b["name"] for b in json.load(open(os.path.join(geo, f"{model}.geo.json")))["minecraft:geometry"][0]["bones"]
                     if any(b["name"].startswith(p) for p in hide)]
        q = wr.geo_quads(os.path.join(geo, f"{model}.geo.json"), tex, hide=verborgen)
        naam = model + ("_zonder_" + hide[0].strip("_") if hide else "")
        for yaw in (35, 150):
            wr.render(q, yaw=yaw, pitch=-12, size=360, ss=1).save(os.path.join(out, f"{naam}_{yaw}.png"))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    import make_v2
    if "--build" in sys.argv:
        build(make_v2)
        print(check(make_v2) or "models ok")
    preview(sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else ".")
