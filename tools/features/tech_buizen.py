"""
bbq2 (tech-buizen): the Knabbelbuizen and what belongs to them. Java: feature/techbuis.

  knabbelbuis            a see-through tube with pink couplings; arms towards every tube and everything that holds items
                         (multipart: a core of framed panes + an arm per side)
  knabbelbuis_richting   the Richtingstuk: a thicker straight piece with arrows, a wide mouth at the back, a nozzle in front
  knabbelbuis_filter     the Filterstuk: the same with a little guh housing around it (ears, a face on both sides: asleep
                         without vadskracht, happy, surprised when it cannot get rid of its items)
  opzuiger               a guh machine with a big round snoet that sticks out
  voorraadmeter, snuffelsensor, guhklok, guhteller
                         the sensors: small guh machines with a lamp (the Guhklok: a bell) that lights up with the signal

and with them: the item models, loot tables, tags, the recipes (the "Zout" tier of DESIGN: each needs guhs:zoutkristal), the
two screens (textures/gui/techbuis_filter.png, techbuis_meter.png), the texts, four visible advancements in the tab
"techniek" and the test room techbuis_test_kamer. This module has no FTB quests: the chapter Guh-technologie is written by
tech_quests (CONTRACT_130 8). Wiki texts: tech_buizen_wiki.py.
"""
import copy
import json
import os

import numpy as np
from PIL import Image, ImageDraw

from features import bbq2, vadskracht

BUIS, RICHTING, FILTER, OPZUIGER = "knabbelbuis", "knabbelbuis_richting", "knabbelbuis_filter", "opzuiger"
METER, SNUFFEL, KLOK, TELLER = "voorraadmeter", "snuffelsensor", "guhklok", "guhteller"
SENSOREN = (METER, SNUFFEL, KLOK, TELLER)
ALLES = (BUIS, RICHTING, FILTER, OPZUIGER) + SENSOREN

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
# a model that points north, turned to each of the six directions
ROT6 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}

# colours
GLAS = (255, 232, 242)           # the pale pink of the glass
GLAS_RAND = (246, 170, 204)      # the darker line along a tube
GLANS = (255, 255, 255)
ROZE = (236, 138, 184)           # the couplings
ROZE_DONKER = (176, 84, 132)
ROZE_LICHT = (255, 190, 220)
PIJL = (150, 96, 214)            # the arrows (the purple of the machines' accent)
PIJL_LICHT = (196, 160, 240)
KAST = (232, 150, 190)           # machine pink (the same as the test machine of the vadskracht)
PAARS = (150, 110, 205)
DONKER = (58, 28, 60)
GOUD = (250, 206, 92)


def _t(kleur, f):
    return tuple(int(max(0, min(255, c * f))) for c in kleur[:3])


# =====================================================================================================================
# textures of the tubes
# =====================================================================================================================
def _glas(richting):
    """
    Tube wall: almost clear, with a line along both long edges of the 8-pixel strip in the middle and a streak of shine.
    richting "v": the tube runs up-down in the picture (for top and bottom faces), "h": left-right (for the side faces).
    """
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = GLAS
    a[..., 3] = 52
    for i in range(16):
        for rand, alfa in ((4, 150), (11, 150), (3, 150), (12, 150)):
            if richting == "v":
                a[i, rand, :3] = GLAS_RAND
                a[i, rand, 3] = alfa
            else:
                a[rand, i, :3] = GLAS_RAND
                a[rand, i, 3] = alfa
        if i % 4 != 3:                                    # a broken streak of shine next to one edge
            if richting == "v":
                a[i, 6, :3] = GLANS
                a[i, 6, 3] = 120
            else:
                a[6, i, :3] = GLANS
                a[6, i, 3] = 120
    return Image.fromarray(a)


def _ruit():
    """A pane of the core: clear with a thin frame around the 8 x 8 middle and a corner of shine."""
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = GLAS
    a[..., 3] = 52
    for i in range(4, 12):
        for x, y in ((i, 4), (i, 11), (4, i), (11, i)):
            a[y, x, :3] = GLAS_RAND
            a[y, x, 3] = 170
    for x, y in ((6, 6), (7, 6), (6, 7), (9, 9)):
        a[y, x, :3] = GLANS
        a[y, x, 3] = 130
    return Image.fromarray(a)


def _ring():
    """The pink of the couplings (opaque), with a light and a dark edge so a ring looks round."""
    rng = np.random.default_rng(4107)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, 4, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(ROZE[c] + n, 0, 255)
    a[..., 3] = 255
    a[0, :, :3] = ROZE_LICHT
    a[:, 0, :3] = ROZE_LICHT
    a[15, :, :3] = ROZE_DONKER
    a[:, 15, :3] = ROZE_DONKER
    return Image.fromarray(a)


def _pijlen(richting):
    """
    The wall of a Richtingstuk: glass with two solid chevrons. richting "omhoog": they point to the top of the picture (the
    top face: north is up), "links": to the left (the west face: north is left; the east face mirrors it).
    """
    img = _glas("v" if richting == "omhoog" else "h").copy()   # (fromarray gives a read-only picture)
    px = img.load()
    for start in (4, 9):                                  # two chevrons behind each other
        for d in range(4):                                # d = how far from the tip
            for kant in (-1, 1):
                for dik in (0, 1):
                    if richting == "omhoog":
                        x, y = 7.5 + kant * (d + 0.5), start + d + dik
                    else:
                        x, y = start + d + dik, 7.5 + kant * (d + 0.5)
                    x, y = int(x), int(y)
                    if 0 <= x < 16 and 0 <= y < 16:
                        px[x, y] = (PIJL if dik == 0 else PIJL_LICHT) + (255,)
    return img


def _kast_ruis(seed, kleur=KAST):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, 5, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _filter_zij(staat):
    """The side of the Filterstuk's housing (12 x 12 in the middle of the picture): a frame and the guh face."""
    a = _kast_ruis(4121)
    a[2, 2:14, :3] = _t(KAST, 0.72)
    a[13, 2:14, :3] = _t(KAST, 0.72)
    a[2:14, 2, :3] = _t(KAST, 0.72)
    a[2:14, 13, :3] = _t(KAST, 0.72)
    a[10:12, 4:12, :3] = PAARS if staat != "slaapt" else _t(PAARS, 0.55)    # a little sieve under the face
    for x in (5, 7, 9):
        a[10:12, x, :3] = _t(PAARS, 0.6 if staat != "slaapt" else 0.4)
    return vadskracht.snoet(Image.fromarray(a), 2, 5, staat)


def _filter_boven():
    """The top (and bottom) of the housing: a frame, an arrow to the top of the picture (north), the ears' colours."""
    a = _kast_ruis(4122, _t(KAST, 1.08))
    a[2, 1:15, :3] = _t(KAST, 0.72)
    a[13, 1:15, :3] = _t(KAST, 0.72)
    a[2:14, 1, :3] = _t(KAST, 0.72)
    a[2:14, 14, :3] = _t(KAST, 0.72)
    img = Image.fromarray(a).copy()
    px = img.load()
    for d in range(4):                                    # one chevron
        for kant in (-1, 1):
            for dik in (0, 1):
                px[int(7.5 + kant * (d + 0.5)), 5 + d + dik] = (PIJL if dik == 0 else PIJL_LICHT) + (255,)
    return img


def _filter_kop():
    """The front and back of the housing: a frame and the dark round hole the tube goes into."""
    a = _kast_ruis(4123)
    a[2, 1:15, :3] = _t(KAST, 0.72)
    a[13, 1:15, :3] = _t(KAST, 0.72)
    a[2:14, 1, :3] = _t(KAST, 0.72)
    a[2:14, 14, :3] = _t(KAST, 0.72)
    for y in range(4, 12):
        for x in range(4, 12):
            if (x in (4, 11)) and (y in (4, 11)):
                continue
            rand = x in (4, 11) or y in (4, 11)
            a[y, x, :3] = ROZE_DONKER if rand else DONKER
    return Image.fromarray(a)


def _oor():
    """The ear colours as a little texture of their own (the ears of the Filterstuk)."""
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = vadskracht.OOR
    a[..., 3] = 255
    a[4:12, 4:12, :3] = vadskracht.OOR_BINNEN
    return Image.fromarray(a)


def textures(h):
    h.save(_glas("v"), "block", "knabbelbuis_boven.png")
    h.save(_glas("h"), "block", "knabbelbuis_zij.png")
    h.save(_ruit(), "block", "knabbelbuis_ruit.png")
    h.save(_ring(), "block", "knabbelbuis_ring.png")
    h.save(_pijlen("omhoog"), "block", "knabbelbuis_richting_boven.png")
    h.save(_pijlen("links"), "block", "knabbelbuis_richting_zij.png")
    for staat in vadskracht.STATEN:
        h.save(_filter_zij(staat), "block", f"knabbelbuis_filter_zij_{staat}.png")
    h.save(_filter_boven(), "block", "knabbelbuis_filter_boven.png")
    h.save(_filter_kop(), "block", "knabbelbuis_filter_kop.png")
    h.save(_oor(), "block", "knabbelbuis_filter_oor.png")


# =====================================================================================================================
# models of the tubes (all point north; the blockstates turn them)
# =====================================================================================================================
def _vlak(tex, uv=None, cull=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def _frame(x0, y0, x1, y1, z0, z1, dik, tex="#ring"):
    """A square ring (four bars) around the tube, between z0 and z1."""
    balken = [([x0, y1 - dik, z0], [x1, y1, z1]), ([x0, y0, z0], [x1, y0 + dik, z1]),
              ([x0, y0 + dik, z0], [x0 + dik, y1 - dik, z1]), ([x1 - dik, y0 + dik, z0], [x1, y1 - dik, z1])]
    return [{"from": a, "to": b, "faces": {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}}
            for a, b in balken]


def _wand(x0, y0, x1, y1, z0, z1, boven="#boven", zij="#zij"):
    """Four walls of a tube along z (no ends), the side faces mirrored so the picture runs the same way on both."""
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {
        "up": _vlak(boven, [x0, z0, x1, z1]), "down": _vlak(boven, [x0, z1, x1, z0]),
        "west": _vlak(zij, [z0, 16 - y1, z1, 16 - y0]), "east": _vlak(zij, [z1, 16 - y1, z0, 16 - y0])}}


GLAS_TEX = {"particle": "guhs:block/knabbelbuis_ring", "boven": "guhs:block/knabbelbuis_boven", "zij": "guhs:block/knabbelbuis_zij",
            "ruit": "guhs:block/knabbelbuis_ruit", "ring": "guhs:block/knabbelbuis_ring"}


def _arm():
    """The arm of a tube towards the north: 4 pixels of tube and a coupling at the end."""
    return [_wand(4, 4, 12, 12, 0, 4)] + _frame(3, 3, 13, 13, 0, 1, 1)


def _dop():
    """The pane that closes the core at the north when there is no arm."""
    return [{"from": [4, 4, 4], "to": [12, 12, 12], "faces": {"north": _vlak("#ruit", [4, 4, 12, 12])}}]


def _model(textures, elements, translucent=True):
    m = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if translucent:
        m["render_type"] = "minecraft:translucent"
    return m


def buis_modellen(h):
    A = h.A
    h.w(f"{A}/models/block/knabbelbuis_arm.json", _model(GLAS_TEX, _arm()))
    h.w(f"{A}/models/block/knabbelbuis_dop.json", _model(GLAS_TEX, _dop()))
    # the block state: per side an arm (when it joins something) or a pane (when it does not)
    delen = []
    for kant, rot in ROT6.items():
        delen.append({"when": {kant: "true"}, "apply": {"model": "guhs:block/knabbelbuis_arm", **rot}})
        delen.append({"when": {kant: "false"}, "apply": {"model": "guhs:block/knabbelbuis_dop", **rot}})
    h.w(f"{A}/blockstates/knabbelbuis.json", {"multipart": delen})
    # in your hand: a straight piece of tube (two arms, four panes)
    recht = _arm() + [dict(e, **{"from": [e["from"][0], e["from"][1], 16 - e["to"][2]], "to": [e["to"][0], e["to"][1], 16 - e["from"][2]]})
                      for e in _zonder_uv(_arm())]
    recht.append({"from": [4, 4, 4], "to": [12, 12, 12], "faces": {
        "up": _vlak("#ruit", [4, 4, 12, 12]), "down": _vlak("#ruit", [4, 4, 12, 12]),
        "east": _vlak("#ruit", [4, 4, 12, 12]), "west": _vlak("#ruit", [4, 4, 12, 12])}})
    h.w(f"{A}/models/block/knabbelbuis_los.json", _model(GLAS_TEX, recht))
    h.w(f"{A}/models/item/knabbelbuis.json", {"parent": "guhs:block/knabbelbuis_los"})


def _zonder_uv(elements):
    uit = copy.deepcopy(elements)
    for e in uit:
        for v in e["faces"].values():
            v.pop("uv", None)
    return uit


def _stuk_basis():
    """
    What the Richtingstuk and the Filterstuk share (pointing north): a wide mouth at the back (south) where things go in,
    and a narrower nozzle at the front where they come out.
    """
    return _frame(1, 1, 15, 15, 14, 16, 2) + _frame(3, 3, 13, 13, 0, 2, 1) + _frame(2, 2, 14, 14, 2, 3, 1)


def richting_modellen(h):
    A = h.A
    tex = dict(GLAS_TEX, boven="guhs:block/knabbelbuis_richting_boven", zij="guhs:block/knabbelbuis_richting_zij")
    h.w(f"{A}/models/block/knabbelbuis_richting.json", _model(tex, [_wand(3, 3, 13, 13, 0, 16)] + _stuk_basis()))
    h.w(f"{A}/blockstates/knabbelbuis_richting.json", {"variants": {
        f"facing={kant}": {"model": "guhs:block/knabbelbuis_richting", **rot} for kant, rot in ROT6.items()}})
    h.w(f"{A}/models/item/knabbelbuis_richting.json", {"parent": "guhs:block/knabbelbuis_richting"})


def _frame_y(x0, z0, x1, z1, y0, y1, dik, tex="#ring"):
    """A square ring (four bars) around a tube that stands up, between y0 and y1."""
    balken = [([x0, y0, z0], [x1, y1, z0 + dik]), ([x0, y0, z1 - dik], [x1, y1, z1]),
              ([x0, y0, z0 + dik], [x0 + dik, y1, z1 - dik]), ([x1 - dik, y0, z0 + dik], [x1, y1, z1 - dik])]
    return [{"from": a, "to": b, "faces": {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}}
            for a, b in balken]


def _wand_y(x0, z0, x1, z1, y0, y1, tex="#boven"):
    """Four walls of a tube that stands up (no ends)."""
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {
        "north": _vlak(tex, [x0, 16 - y1, x1, 16 - y0]), "south": _vlak(tex, [x0, 16 - y1, x1, 16 - y0]),
        "west": _vlak(tex, [z0, 16 - y1, z1, 16 - y0]), "east": _vlak(tex, [z0, 16 - y1, z1, 16 - y0])}}


def filter_modellen(h):
    """
    The Filterstuk in three builds, so its face is always upright: lying (pointing north, the blockstate turns it; the
    face on both sides, the ears on top), and standing with the arrow up ("op") or down ("neer"): the face on the north
    and south side, the ears at the top of the north side.
    """
    A = h.A
    alle = ("north", "south", "east", "west", "up", "down")
    for staat in vadskracht.STATEN:
        tex = dict(GLAS_TEX, kast=f"guhs:block/knabbelbuis_filter_zij_{staat}", kastboven="guhs:block/knabbelbuis_filter_boven",
                   kop="guhs:block/knabbelbuis_filter_kop", oor="guhs:block/knabbelbuis_filter_oor",
                   particle="guhs:block/knabbelbuis_filter_kop")
        oor = lambda p0, p1: {"from": p0, "to": p1, "faces": {f: _vlak("#oor", [4, 4, 12, 12] if f in ("north", "south") else [0, 0, 4, 4])
                                                                 for f in alle if f != "down"}}
        # lying
        kast = {"from": [1, 2, 3], "to": [15, 14, 14], "faces": {
            "west": _vlak("#kast", [2, 2, 13, 14]), "east": _vlak("#kast", [13, 2, 2, 14]),
            "up": _vlak("#kastboven", [1, 2, 15, 13]), "down": _vlak("#kastboven", [1, 13, 15, 2]),
            "north": _vlak("#kop", [1, 2, 15, 14]), "south": _vlak("#kop", [1, 2, 15, 14])}}
        oren = [oor([x0, 14, 7], [x0 + 3, 17, 9]) for x0 in (3, 10)]
        stompjes = [_wand(3, 3, 13, 13, 0, 3), _wand(3, 3, 13, 13, 14, 16)]
        h.w(f"{A}/models/block/knabbelbuis_filter_{staat}.json", _model(tex, stompjes + [kast] + oren + _stuk_basis()))
        # standing: the wide mouth where things go in (below when the arrow points up), the nozzle at the other end
        for naam, op in (("op", True), ("neer", False)):
            y = (lambda a, b: (a, b)) if op else (lambda a, b: (16 - b, 16 - a))
            ringen = (_frame_y(1, 1, 15, 15, *y(0, 2), 2) + _frame_y(3, 3, 13, 13, *y(14, 16), 1) + _frame_y(2, 2, 14, 14, *y(13, 14), 1))
            pijl = [1, 2, 15, 13] if op else [1, 13, 15, 2]
            kast = {"from": [2, y(2, 13)[0], 1], "to": [14, y(2, 13)[1], 15], "faces": {
                "north": _vlak("#kast", [2, 3, 14, 14]), "south": _vlak("#kast", [2, 3, 14, 14]),
                "west": _vlak("#kastboven", pijl), "east": _vlak("#kastboven", pijl),
                "up": _vlak("#kop", [2, 1, 14, 15]), "down": _vlak("#kop", [2, 1, 14, 15])}}
            boven = y(2, 13)[1]                             # the ears stand on the top edge of the north side
            oren = [oor([x0, boven, 0], [x0 + 3, boven + 4, 2]) for x0 in (2, 11)]
            stompjes = [_wand_y(3, 3, 13, 13, *y(13, 16)), _wand_y(3, 3, 13, 13, *y(0, 2))]
            h.w(f"{A}/models/block/knabbelbuis_filter_{naam}_{staat}.json", _model(tex, stompjes + [kast] + oren + ringen))

    def variant(kant, staat):
        if kant == "up":
            return {"model": f"guhs:block/knabbelbuis_filter_op_{staat}"}
        if kant == "down":
            return {"model": f"guhs:block/knabbelbuis_filter_neer_{staat}"}
        return {"model": f"guhs:block/knabbelbuis_filter_{staat}", **ROT6[kant]}

    h.w(f"{A}/blockstates/knabbelbuis_filter.json", {"variants": {
        f"facing={kant},snoet={staat}": variant(kant, staat) for kant in ROT6 for staat in vadskracht.STATEN}})
    h.w(f"{A}/models/item/knabbelbuis_filter.json", {"parent": "guhs:block/knabbelbuis_filter_werkt"})


# =====================================================================================================================
# the Opzuiger and the sensors: guh machines (vadskracht.machine) with something sticking out
# =====================================================================================================================
def _zet(img, x, y, kleur):
    img.putpixel((x, y), tuple(kleur[:3]) + (255,))


def _wis_luik(img, basis):
    """Paints over the hatch that vadskracht.machine puts under the face (rows 8..14): we draw our own thing there."""
    for y in range(8, 15):
        for x in range(1, 15):
            _zet(img, x, y, basis)


OPZUIGER_BASIS, OPZUIGER_ACCENT = (240, 150, 186), (132, 96, 196)


def _opzuiger_voor(img, staat):
    """The Opzuiger's snoet: a big round mouth (shut when asleep, with a knabbel stuck in it when full)."""
    _wis_luik(img, OPZUIGER_BASIS)
    lip, gat = (214, 84, 140), (46, 20, 44)
    for y in range(8, 14):
        for x in range(4, 12):
            hoek = x in (4, 11) and y in (8, 13)
            if hoek:
                continue
            rand = x in (4, 11) or y in (8, 13)
            if staat == "slaapt":
                _zet(img, x, y, lip if y in (10, 11) and 4 < x < 11 else OPZUIGER_BASIS)
            else:
                _zet(img, x, y, lip if rand else gat)
    if staat == "vol":
        for x, y in ((7, 10), (8, 10), (7, 11), (8, 11), (6, 11), (9, 10)):
            _zet(img, x, y, GOUD)                         # a knabbel that does not fit any more
    elif staat == "werkt":
        for x, y in ((6, 10), (9, 11)):
            _zet(img, x, y, (110, 60, 100))               # a bit of depth in the hole


def _toevoegen(h, name, staten, elements, textures=None):
    """Adds model elements (and textures) to the models block/<name>_<staat> that vadskracht.machine wrote."""
    for staat in staten:
        def fn(m, staat=staat):
            m["elements"] = m["elements"] + [e if not callable(e) else e(staat) for e in elements]
            m["textures"].update(textures(staat) if callable(textures) else (textures or {}))
        h.patch_json(f"{h.A}/models/block/{name}_{staat}.json", fn)


def opzuiger(h):
    vadskracht.machine(h, OPZUIGER, OPZUIGER_BASIS, OPZUIGER_ACCENT, voor=_opzuiger_voor)
    # the snoet sticks out: a short trunk whose end shows the mouth of the front texture
    slurf = {"from": [4, 2, -3], "to": [12, 8, 0], "faces": {
        "north": _vlak("#voor", [4, 8, 12, 14]), "up": _vlak("#zij", [4, 0, 12, 3]), "down": _vlak("#zij", [4, 13, 12, 16]),
        "west": _vlak("#zij", [0, 8, 3, 14]), "east": _vlak("#zij", [13, 8, 16, 14])}}
    _toevoegen(h, OPZUIGER, vadskracht.STATEN, [slurf])


# name -> (basis, accent)
SENSOR_KLEUR = {METER: ((226, 186, 128), (120, 190, 110)), SNUFFEL: ((250, 222, 214), (236, 110, 150)),
                KLOK: ((244, 212, 150), (150, 110, 205)), TELLER: ((212, 192, 234), (236, 138, 184))}


def _meter_voor(img, staat):
    basis, accent = SENSOR_KLEUR[METER]
    _wis_luik(img, basis)
    for y in range(9, 14):                                # a gauge: a dark window with bars that fill up
        for x in range(3, 13):
            _zet(img, x, y, _t(basis, 0.45))
    vol = {"slaapt": 0, "werkt": 2, "vol": 4}[staat]
    for i in range(4):
        for y in range(10, 13):
            for x in (4 + i * 2, 5 + i * 2):
                if x <= 11:
                    _zet(img, x, y, accent if i < vol else _t(basis, 0.65))


def _snuffel_voor(img, staat):
    basis, accent = SENSOR_KLEUR[SNUFFEL]
    _wis_luik(img, basis)
    for y in range(8, 12):                                # the nose (the model lets it stick out)
        for x in range(6, 10):
            _zet(img, x, y, accent)
    _zet(img, 6, 8, _t(accent, 1.2))
    for x in (6, 9):                                      # nostrils
        _zet(img, x, 10, DONKER)
    for x, y in ((2, 9), (3, 10), (2, 11), (13, 9), (12, 10), (13, 11)):   # whiskers
        _zet(img, x, y, _t(basis, 0.6))
    if staat == "vol":                                    # it smells something: sniff lines
        for x, y in ((5, 13), (7, 13), (8, 13), (10, 13)):
            _zet(img, x, y, (255, 255, 255))


def _klok_voor(img, staat):
    basis, accent = SENSOR_KLEUR[KLOK]
    _wis_luik(img, basis)
    wit = (255, 250, 240)
    for y in range(8, 15):                                # a round dial
        for x in range(4, 12):
            if (x in (4, 11)) and (y in (8, 14)):
                continue
            rand = x in (4, 11) or y in (8, 14)
            _zet(img, x, y, _t(basis, 0.6) if rand else wit)
    wijzer = DONKER if staat != "slaapt" else _t(basis, 0.5)
    for x, y in ((7, 11), (7, 10), (7, 9)):               # the big hand: up
        _zet(img, x, y, wijzer)
    for x, y in (((8, 11), (9, 11)) if staat != "vol" else ((8, 10), (9, 9))):   # the small hand: jumps at a tick
        _zet(img, x, y, accent)


def _teller_voor(img, staat):
    basis, accent = SENSOR_KLEUR[TELLER]
    _wis_luik(img, basis)
    for y in range(9, 14):                                # a little slate with tally marks
        for x in range(3, 13):
            _zet(img, x, y, (70, 60, 84))
    streep = (255, 255, 255) if staat != "slaapt" else (150, 140, 160)
    aantal = {"slaapt": 2, "werkt": 3, "vol": 4}[staat]
    for i in range(aantal):
        for y in range(10, 13):
            _zet(img, 4 + i * 2, y, streep)
    if staat == "vol":                                    # the fifth mark goes through the other four
        for x, y in ((3, 12), (5, 11), (7, 11), (9, 10), (11, 10)):
            _zet(img, x, y, accent)


def _lamp(aan, kleur_uit, kleur_aan):
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = kleur_aan if aan else kleur_uit
    a[..., 3] = 255
    a[0:16, 0:2, :3] = _t(kleur_aan if aan else kleur_uit, 1.25 if aan else 1.1)
    a[14:16, :, :3] = _t(kleur_aan if aan else kleur_uit, 0.75)
    if aan:
        a[4:8, 4:8, :3] = (255, 255, 255)
    return Image.fromarray(a)


def _doos(p0, p1, tex, zonder=()):
    return {"from": list(p0), "to": list(p1),
            "faces": {f: {"texture": tex, "uv": [0, 0, 8, 8]} for f in ("north", "south", "east", "west", "up", "down") if f not in zonder}}


def sensoren(h):
    A = h.A
    h.save(_lamp(False, (120, 40, 60), (255, 120, 150)), "block", "techbuis_lamp_uit.png")
    h.save(_lamp(True, (120, 40, 60), (255, 120, 150)), "block", "techbuis_lamp_aan.png")
    h.save(_lamp(False, (176, 136, 52), (255, 226, 110)), "block", "techbuis_bel_uit.png")
    h.save(_lamp(True, (176, 136, 52), (255, 226, 110)), "block", "techbuis_bel_aan.png")
    h.save(_lamp(False, _t(PAARS, 0.6), PAARS), "block", "techbuis_voeler.png")
    voor = {METER: _meter_voor, SNUFFEL: _snuffel_voor, KLOK: _klok_voor, TELLER: _teller_voor}
    for name in SENSOREN:
        basis, accent = SENSOR_KLEUR[name]
        vadskracht.machine(h, name, basis, accent, voor=voor[name])
        extra = []
        if name == METER:        # a feeler at the back, into what it counts
            extra.append(_doos((6, 6, 16), (10, 10, 18), "#voeler", zonder=("north",)))
        elif name == SNUFFEL:    # the nose sticks out of the front
            extra.append({"from": [6, 4, -2], "to": [10, 8, 0], "faces": {
                "north": _vlak("#voor", [6, 8, 10, 12]), "up": _vlak("#voor", [6, 8, 10, 9]), "down": _vlak("#voor", [6, 11, 10, 12]),
                "west": _vlak("#voor", [6, 8, 7, 12]), "east": _vlak("#voor", [9, 8, 10, 12])}})
        elif name == TELLER:     # the socket at the back where the pulses come in
            extra.append(_doos((5, 5, 16), (11, 11, 17), "#voeler", zonder=("north",)))
        if name == KLOK:         # a bell on top (it is the lamp)
            lamp = [_doos((5, 16, 6), (11, 18, 12), "#lamp", zonder=("down",)), _doos((7, 18, 8), (9, 19, 10), "#lamp", zonder=("down",))]
        else:
            lamp = [_doos((7, 16, 7), (9, 18, 9), "#lamp", zonder=("down",))]
        soort = "bel" if name == KLOK else "lamp"
        _toevoegen(h, name, vadskracht.STATEN, extra + lamp,
                   {"lamp": f"guhs:block/techbuis_{soort}_uit", "voeler": "guhs:block/techbuis_voeler"})
        # the same three models once more with the lamp on, and a blockstate that also knows the signal
        for staat in vadskracht.STATEN:
            m = json.load(open(f"{A}/models/block/{name}_{staat}.json", encoding="utf-8"))
            m["textures"]["lamp"] = f"guhs:block/techbuis_{soort}_aan"
            h.w(f"{A}/models/block/{name}_{staat}_aan.json", m)
        h.w(f"{A}/blockstates/{name}.json", {"variants": {
            f"facing={f},signaal={'true' if aan else 'false'},snoet={staat}":
                {"model": f"guhs:block/{name}_{staat}" + ("_aan" if aan else ""), **({"y": r} if r else {})}
            for f, r in ROT.items() for aan in (False, True) for staat in vadskracht.STATEN}})


# =====================================================================================================================
# the screens
# =====================================================================================================================
def _scherm(vakken):
    """
    The background of the Filterstuk's / Voorraadmeter's screen (176 x 184 on a 256 x 256 sheet), in the look of a vanilla
    container: the example slots (pink: they hold no real items), the number field, your inventory. The positions are those
    of feature/techbuis/FilterMenu and client/FilterScreen.
    """
    B, H = 176, 184
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    grijs, wit, donker, zwart = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (0, 0, 0, 255)
    d.rectangle([2, 2, B - 3, H - 3], fill=grijs)
    d.rectangle([2, 1, B - 4, 2], fill=wit)
    d.rectangle([1, 2, 2, H - 4], fill=wit)
    d.rectangle([3, H - 3, B - 3, H - 2], fill=donker)
    d.rectangle([B - 3, 3, B - 2, H - 3], fill=donker)
    d.line([2, 0, B - 4, 0], fill=zwart)
    d.line([0, 2, 0, H - 4], fill=zwart)
    d.line([3, H - 1, B - 3, H - 1], fill=zwart)
    d.line([B - 1, 3, B - 1, H - 4], fill=zwart)
    for x, y in ((1, 1), (B - 3, 1), (1, H - 3), (B - 2, H - 2), (B - 2, 2), (2, H - 2)):
        d.point([x, y], fill=zwart)
    d.point([B - 3, 2], fill=grijs)
    d.point([2, H - 3], fill=grijs)

    def vak(x, y, vulling=(139, 139, 139, 255)):
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=vulling)
        d.line([x - 1, y - 1, x + 15, y - 1], fill=(55, 55, 55, 255))
        d.line([x - 1, y - 1, x - 1, y + 15], fill=(55, 55, 55, 255))
        d.line([x, y + 16, x + 16, y + 16], fill=wit)
        d.line([x + 16, y, x + 16, y + 16], fill=wit)

    roze = (222, 160, 190, 255)
    if vakken == 1:
        vak(26, 36, roze)
    else:
        for i in range(9):
            vak(8 + (i % 3) * 18, 18 + (i // 3) * 18, roze)
    # the number field between the - and + buttons (68..168 x 66..82)
    d.rectangle([90, 66, 145, 81], fill=(60, 34, 56, 255))
    d.line([90, 66, 145, 66], fill=(34, 16, 32, 255))
    d.line([90, 66, 90, 81], fill=(34, 16, 32, 255))
    d.line([91, 81, 145, 81], fill=wit)
    d.line([145, 67, 145, 81], fill=wit)
    for rij in range(3):
        for kol in range(9):
            vak(8 + kol * 18, 102 + rij * 18)
    for kol in range(9):
        vak(8 + kol * 18, 160)
    return img


def schermen(h):
    h.save(_scherm(9), "gui", "techbuis_filter.png")
    h.save(_scherm(1), "gui", "techbuis_meter.png")


# =====================================================================================================================
# loot, tags, recipes
# =====================================================================================================================
def data(h):
    for name in ALLES:
        h.self_drop(name)
        h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{name}"])
    vadskracht.toon(h, FILTER)     # (machine() did it for the Opzuiger and the sensors; a plain tube is no vadskracht block)
    Z, D, K = "guhs:zoutkristal", "guhs:guh_wire", "guhs:kaas_knabbels"
    # the "Zout" tier: everything needs zoutkristal from the Guhbarbecuether
    h.shaped(BUIS, ["GGG", "KZK", "GGG"], {"G": "minecraft:glass", "K": K, "Z": Z}, f"guhs:{BUIS}", 8)
    h.shaped(RICHTING, ["BHB", " Z "], {"B": f"guhs:{BUIS}", "H": "minecraft:hopper", "Z": Z}, f"guhs:{RICHTING}", 2)
    h.shaped(FILTER, [" T ", "DRD", " Z "], {"T": "minecraft:redstone_torch", "D": D, "R": f"guhs:{RICHTING}", "Z": Z}, f"guhs:{FILTER}")
    h.shaped(OPZUIGER, ["IHI", "ZKZ", "IDI"], {"I": "minecraft:iron_ingot", "H": "minecraft:hopper", "Z": Z, "K": K, "D": D},
             f"guhs:{OPZUIGER}")
    hart = {METER: "minecraft:repeater", SNUFFEL: K, KLOK: "minecraft:clock", TELLER: "minecraft:stone_button"}
    for name in SENSOREN:
        h.shaped(name, [" Z ", "DXD", "SSS"], {"Z": Z, "D": D, "X": hart[name], "S": "minecraft:stone"}, f"guhs:{name}")


def test_templates(h):
    """techbuis_test_kamer: 13 x 7 x 13 with a stone floor (things stand at helper y 2)."""
    s = h.Structure((13, 7, 13))
    s.fill(0, 0, 0, 12, 0, 12, "minecraft:stone")
    s.save("techbuis_test_kamer")


# =====================================================================================================================
# texts
# =====================================================================================================================
G = "gui.guhs.techbuis."
TEXTS = {
    "block.guhs.knabbelbuis": "Knabbelbuis",
    "block.guhs.knabbelbuis_richting": "Richtingstuk",
    "block.guhs.knabbelbuis_filter": "Filterstuk",
    "block.guhs.opzuiger": "Opzuiger",
    "block.guhs.voorraadmeter": "Voorraadmeter",
    "block.guhs.snuffelsensor": "Snuffelsensor",
    "block.guhs.guhklok": "Guhklok",
    "block.guhs.guhteller": "Guhteller",
    "block.guhs.knabbelbuis.lore": "Doorzichtige buis waar je je spullen doorheen ziet rollen. Plakt vanzelf vast aan kisten, guhmachines, "
                                   "de Bank Guh en het Hapluikje. Zelf doet hij niks: zet er een Richtingstuk aan, njeg!",
    "block.guhs.knabbelbuis_richting.lore": "Een stuk Knabbelbuis met een pijl. Hapt spullen uit wat erachter zit en stuurt ze de buis in. "
                                            "Tussen twee buizen is het eenrichtingsverkeer. Sluipen + klikken: omdraaien. "
                                            "Een redstonesignaal zet het op slot.",
    "block.guhs.knabbelbuis_filter.lore": "Een Richtingstuk met een kieskeurig guhtje erin: het laat alleen door wat op zijn lijstje staat "
                                          "en hapt met grote happen. Wat erachter zit krijgt die spullen het eerst: zo sorteer je. "
                                          f"Wil {vadskracht.getal('BUISFILTER')} vadskracht.",
    "block.guhs.opzuiger.lore": "Slurpt alle losse spullen in de buurt naar binnen. Nooit meer bukken, vads lekker! "
                                f"Wil {vadskracht.getal('OPZUIGER')} vadskracht.",
    "block.guhs.voorraadmeter.lore": "Telt wat er in de kist, de machine of de Bank Guh achter hem zit en geeft een redstonesignaal als "
                                     f"er genoeg (of juist te weinig) is. Wil {vadskracht.getal('SENSOR')} vadskracht.",
    "block.guhs.snuffelsensor.lore": "Een neus die ruikt wie er in de buurt is: guhs, Mika's of spelers. Ruikt hij iemand, dan geeft hij "
                                     f"een redstonesignaal. Wil {vadskracht.getal('SENSOR')} vadskracht.",
    "block.guhs.guhklok.lore": "Geeft om de zoveel tijd een redstonetikje, of een signaal zolang het dag (of nacht) is. Klik om hem te "
                               f"verzetten. Wil {vadskracht.getal('SENSOR')} vadskracht.",
    "block.guhs.guhteller.lore": "Telt de redstonetikjes die aan zijn achterkant binnenkomen en geeft bij de zoveelste zelf een tikje. "
                                 f"Klik om te kiezen tot hoeveel hij telt. Wil {vadskracht.getal('SENSOR')} vadskracht.",
    # what a Richtingstuk / Filterstuk says (use it, or look at the Filterstuk)
    G + "stand.op_slot": "Op slot: er komt een redstonesignaal binnen",
    G + "stand.verstopt": "Verstopt: zijn spullen kunnen nergens heen, njeg!",
    G + "stand.geen_plek": "Deze buis komt nergens uit waar spullen in kunnen",
    G + "stand.plekken": "Stuurt spullen naar %s plek(ken)",
    G + "stand.onderweg": "Stuurt spullen naar %s plek(ken); er rollen er nu %s door de buis",
    G + "stand.hapt_uit": "Hapt uit: %s",
    # behind a Bank Guh: a bank gives to a Filterstuk only, and only with its upgrade
    G + "stand.bank_filter": "Uit een Bank Guh hapt alleen een Filterstuk, njeg",
    G + "stand.bank_maagje": "Deze Bank Guh geeft pas iets met het Bodemloos Knabbelmaagje",
    G + "filter.alles": "Laat alles door (het lijstje is leeg)",
    G + "filter.alleen": "Laat alleen door: %s",
    G + "filter.behalve": "Laat alles door behalve: %s",
    G + "filter.bewaar": "Laat van elk ding minstens %s liggen",
    # the Voorraadmeter
    G + "meter.geen_kist": "Ziet niks om in te tellen: zet hem met zijn rug tegen een kist, een machine of de Bank Guh",
    G + "meter.alles": "dingen",
    G + "meter.telt": "Telt %s × %s in: %s",
    G + "meter.minstens": "Geeft een signaal bij minstens %s",
    G + "meter.minder": "Geeft een signaal bij minder dan %s",
    # the Snuffelsensor
    G + "snuffel.zoekt": "Snuffelt naar %s, binnen %s blokken",
    G + "snuffel.wat.guhs": "guhs",
    G + "snuffel.wat.mikas": "Mika's",
    G + "snuffel.wat.spelers": "spelers",
    G + "snuffel.wat.alles": "guhs, Mika's en spelers",
    G + "snuffel.ruikt": "Ruikt er nu: %s",
    # the Guhklok
    G + "klok.elke": "Tikt elke %s",
    G + "klok.s1": "tel",
    G + "klok.s": "%s tellen",
    G + "klok.min1": "minuut",
    G + "klok.min": "%s minuten",
    G + "klok.dag": "Geeft een signaal zolang het dag is",
    G + "klok.nacht": "Geeft een signaal zolang het nacht is",
    # the Guhteller
    G + "teller.tot": "Telt nu tot %s",
    G + "teller.nul": "Weer op nul, njeg",
    G + "teller.geteld": "Geteld: %s van de %s",
    # the Opzuiger
    G + "opzuiger.vol": "Zit vol: er past niks meer bij. Haal hem leeg met een Richtingstuk!",
    G + "opzuiger.bereik": "Slurpt alles op binnen %s blokken",
    # the screens
    G + "scherm.filter.lijst.alleen": "Alleen deze",
    G + "scherm.filter.lijst.behalve": "Alles behalve",
    G + "scherm.filter.lijst.uitleg": "Laat het Filterstuk alleen de dingen in de vakjes door, of juist alles behalve die? "
                                      "Klik met een ding op een vakje om het als voorbeeld te geven (je houdt het zelf).",
    G + "scherm.filter.getal": "Laat liggen:",
    G + "scherm.meter.lijst.alleen": "Minstens",
    G + "scherm.meter.lijst.behalve": "Minder dan",
    G + "scherm.meter.lijst.uitleg": "Geeft de Voorraadmeter een signaal als er minstens zoveel is, of juist als er minder is? "
                                     "Zonder voorbeeld in het vakje telt hij alles.",
    G + "scherm.meter.getal": "Aantal:",
    G + "scherm.precies.aan": "Heel precies",
    G + "scherm.precies.uit": "Niet zo precies",
    G + "scherm.precies.uitleg": "Heel precies: een ding telt alleen als het helemaal hetzelfde is als het voorbeeld (naam, betovering, "
                                 "wat erin zit). Niet zo precies: dezelfde soort is goed genoeg.",
    G + "scherm.getal.uitleg": "Klik: 1 erbij of eraf. Met shift: 8. Met ctrl: 64.",
    # /guhs techbuis
    G + "commando.geen_stuk": "Daar zit geen Richtingstuk of Filterstuk",
    G + "commando.plek": "- %s op %s (kant %s): %s blokken buis, %s stuk(ken) onderweg",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


def advancements(h):
    def heeft(*ids):
        return {i: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{i}"}]}} for i in ids}

    bbq2.zichtbaar(h, "techniek", "tech_buizen_buis", "root", f"guhs:{BUIS}", "task", "Rollebollen",
                   "Maak een Knabbelbuis: daar rollen je spullen vanzelf doorheen", criteria=heeft(BUIS))
    bbq2.zichtbaar(h, "techniek", "tech_buizen_filter", "tech_buizen_buis", f"guhs:{FILTER}", "task", "Kieskeurig guhtje",
                   "Maak een Filterstuk en laat het je spullen sorteren", criteria=heeft(FILTER))
    bbq2.zichtbaar(h, "techniek", "tech_buizen_opzuiger", "tech_buizen_buis", f"guhs:{OPZUIGER}", "task", "Slurp!",
                   "Maak een Opzuiger. Nooit meer bukken, njeg", criteria=heeft(OPZUIGER))
    bbq2.zichtbaar(h, "techniek", "tech_buizen_sensor", "tech_buizen_buis", f"guhs:{SNUFFEL}", "task", "De guh ziet alles",
                   "Maak een Voorraadmeter, een Snuffelsensor, een Guhklok of een Guhteller", criteria=heeft(*SENSOREN))
    h.patch_json(f"{h.D}/advancement/techniek/tech_buizen_sensor.json", lambda d: d.update(requirements=[list(SENSOREN)]))


def selfcheck(h):
    A, D = h.A, h.D
    paden = [f"{A}/blockstates/{n}.json" for n in ALLES] + [f"{A}/models/item/{n}.json" for n in ALLES]
    paden += [f"{D}/loot_table/blocks/{n}.json" for n in ALLES] + [f"{D}/recipe/{n}.json" for n in ALLES]
    paden += [f"{A}/models/block/knabbelbuis_{d}.json" for d in ("arm", "dop", "los", "richting")]
    paden += [f"{A}/models/block/knabbelbuis_filter_{v}{s}.json" for s in vadskracht.STATEN for v in ("", "op_", "neer_")]
    paden += [f"{A}/models/block/{n}_{s}{aan}.json" for n in SENSOREN for s in vadskracht.STATEN for aan in ("", "_aan")]
    paden += [f"{A}/models/block/{OPZUIGER}_{s}.json" for s in vadskracht.STATEN]
    paden += [os.path.join(h.TEX, "gui", f"techbuis_{s}.png") for s in ("filter", "meter")]
    paden += [f"{D}/structure/techbuis_test_kamer.nbt"]
    missing = [p for p in paden if not os.path.exists(p)]
    missing += [k for k in TEXTS if k not in h.NL]
    missing += [f"block.guhs.{n}{e}" for n in ALLES for e in ("", ".lore") if f"block.guhs.{n}{e}" not in TEXTS]
    # every recipe of this module is "Zout" tier
    for n in ALLES:
        p = f"{D}/recipe/{n}.json"
        if os.path.exists(p) and "guhs:zoutkristal" not in json.dumps(json.load(open(p, encoding="utf-8"))):
            missing.append(f"recipe {n}: geen zoutkristal")
    if missing:
        raise SystemExit(f"tech_buizen: missing {missing}")


def build(h):
    textures(h)
    buis_modellen(h)
    richting_modellen(h)
    filter_modellen(h)
    opzuiger(h)
    sensoren(h)
    schermen(h)
    data(h)
    test_templates(h)
    texts(h)
    advancements(h)
    selfcheck(h)
