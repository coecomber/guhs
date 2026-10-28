"""
1.8.0 (Guhdromenland), bouwplan C "droomwezens": the dream NPCs, the Sterrenvisje and the "glas met een restje".
Java: feature/droomwezens (DroomwezensFeature, DroomNpc, DroomNpcRol, KussenProjectiel, SterrenvisjeEntity, GlasRestjeBlock,
DroomwezensCommando, client/DroomwezensClient).

  NPCs (GuhNpcEntity kinds, the sitting guh with extra bones, after the m03 renders):
    snoes              the zwerfguh: grey-pink, a bent ear, a plaster and loose fluff
    snoes_blij         Snoes after the story: pinker, the same bent ear, a knus sjaaltje
    zandguh            a night-blue nachtjapon, a floppy slaapmuts with a star, a sack of glittering droomzand (glowmask)
    wekkerguh          a red alarm clock on his back (bells, hammer, winding key), little bells on his ears; anim "rinkel"
    rommelguh          the Evivads look (guh_npc_weeb_evivads) with a glass with a restje in each paw, crumbs
    droom_omaguh       Oma-guh (guh_npc_omaguh) with her knitting: needles, a little knitted patch and a ball of wool
    kussengevecht_guh  striped pyjamas, a slaapmutsje, a pillow in his paws; anim "gooi" (the role throws a KussenProjectiel)
  sterrenvisje         a tiny glowing fish with star fins, in four pastel colours (+ glowmasks), spawn egg, bucket
  glas_restje          1-3 glasses with a restje (sap, cola, limo, melk, thee), the Rommelguh's trademark

Standalone (from the project root):  python tools/features/droomwezens.py         (only these files; nl_nl/en_us patched,
English also in tools/lang/en/c212_droomwezens.json)
                                     python tools/features/droomwezens.py bb      (also rewrite the blockbench/*.bbmodel)
"""
import base64
import copy
import io
import json
import math
import os
import sys
import uuid

import numpy as np
from PIL import Image, ImageDraw

try:
    from features import sterrenwacht_hulp as hulp
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import sterrenwacht_hulp as hulp

BONES = {}
CLOTHES = []

c = hulp.cube
NPCS = ["snoes", "snoes_blij", "zandguh", "wekkerguh", "rommelguh", "droom_omaguh", "kussengevecht_guh"]
# kinds with their own animation file (their extra named animations: name -> length in seconds)
EIGEN_ANIM = {"wekkerguh": {"rinkel": 1.0}, "kussengevecht_guh": {"gooi": 0.7}}
GLOED = ["zandguh"]
KLEUREN = {"geel": (255, 226, 120), "roze": (255, 160, 205), "lila": (196, 166, 255), "munt": (140, 236, 208)}
DRANKJES = {"sap": (255, 168, 40), "cola": (92, 40, 22), "limo": (176, 236, 90), "melk": (246, 244, 236), "thee": (196, 120, 60)}

WIT = (250, 248, 246)
GOUD = (246, 204, 74)


def _rng(seed):
    return np.random.default_rng(seed)


def _pyjama_strepen(kleur, streep, breed=5):
    def f(block):
        for x in range(block.shape[1]):
            if (x // breed) % 2:
                block[:, x, :3] = streep
    return f


def _sterretjes(n, kleur, seed):
    def f(block):
        r = np.random.default_rng(seed)
        for _ in range(n):
            x, y = r.integers(2, block.shape[1] - 2), r.integers(2, block.shape[0] - 2)
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                block[y + dy, x + dx, :3] = kleur
    return f


def _glitter(n, seed):
    def f(block):
        r = np.random.default_rng(seed)
        for _ in range(n):
            x, y = r.integers(1, block.shape[1] - 1), r.integers(1, block.shape[0] - 1)
            block[y, x, :3] = (255, 250, 220)
    return f


def _klok(block):
    """The clock face: a white round dial, twelve dots, two hands (ten past ten, a happy face)."""
    n = block.shape[0]
    cx = cy = (n - 1) / 2
    block[..., :3] = (210, 50, 60)
    yy, xx = np.mgrid[0:n, 0:n]
    rr = np.hypot(xx - cx, yy - cy)
    block[rr < n * 0.44, :3] = (252, 250, 244)
    for i in range(12):
        a = i / 12 * 2 * math.pi
        x, y = int(round(cx + math.sin(a) * n * 0.36)), int(round(cy - math.cos(a) * n * 0.36))
        block[y, x, :3] = (60, 50, 60)
    for (a, l) in ((math.radians(300), 0.22), (math.radians(60), 0.32)):
        for t in np.linspace(0, l * n, 20):
            block[int(round(cy - math.cos(a) * t)), int(round(cx + math.sin(a) * t)), :3] = (40, 34, 44)
    block[int(cy), int(cx), :3] = GOUD


def _pleister(block):
    block[..., :3] = (236, 196, 160)
    for y in range(block.shape[0]):
        for x in range(block.shape[1]):
            if (x + y) % 7 == 0:
                block[y, x, :3] = (214, 172, 140)
    m = block.shape[0] // 2
    block[m - 2:m + 2, 4:-4, :3] = (246, 222, 196)


def _gebreid(block):
    for y in range(block.shape[0]):
        for x in range(block.shape[1]):
            if (x // 3 + y // 2) % 2:
                block[y, x, :3] = np.clip(block[y, x, :3].astype(int) - 30, 0, 255)


def _kussen_rand(block):
    block[:2, :, :3] = (240, 120, 170)
    block[-2:, :, :3] = (240, 120, 170)
    block[:, :2, :3] = (240, 120, 170)
    block[:, -2:, :3] = (240, 120, 170)


# =====================================================================================================================
# the NPC models
# =====================================================================================================================
def _geo_van(h, model, identifier):
    geo_file = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", model + ".geo.json"), encoding="utf-8"))
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = identifier
    return geo_file, geo


def _tex_van(h, naam):
    return np.asarray(Image.open(os.path.join(h.TEX, "entity", naam + ".png")).convert("RGBA")).copy()


def _bone(geo, name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "parent": parent, "pivot": pivot, "cubes": cubes}
    if rotation:
        b["rotation"] = rotation
    geo["bones"].append(b)


def _buig_oor(geo, oor="ear_right", hoek=38):
    """Snoes' bent ear: the ear bone tipped outward-down (the idle animation still wiggles it on top of this)."""
    for b in geo["bones"]:
        if b["name"] == oor:
            b["rotation"] = [0, 0, hoek if oor == "ear_right" else -hoek]


def npcs(h):
    out = {}
    # --- Snoes: the zwerfguh (grey-pink, a bent ear, a plaster on his cheek, loose fluff) -----------------------------------
    for staat, (sat, val) in (("snoes", (0.32, 0.86)), ("snoes_blij", (0.85, 1.0))):
        geo_file, geo = hulp.sitting_geo(h, f"geometry.guh_npc_{staat}")
        sw = hulp.swatches(geo, ["pleister", "pluis", "sjaal", "sjaal2"])
        _buig_oor(geo)
        _bone(geo, "snoes_pleister", "head", [3, 19, -5], [c([2.2, 18.2, -5.45], [3.6, 1.4, 0.5], sw["pleister"], rotation=[0, 0, 22],
                                                              pivot=[4, 18.9, -5.2])])
        _bone(geo, "snoes_pluis", "head", [0, 24, 0], [c([-2.5, 25.8, -1.2], [1.0, 1.0, 1.0], sw["pluis"]),
                                                       c([1.6, 25.9, 0.4], [0.8, 0.8, 0.8], sw["pluis"]),
                                                       c([6.6, 20.5, -2.0], [0.9, 0.9, 0.9], sw["pluis"]),
                                                       c([-7.4, 17.0, 1.0], [0.8, 0.8, 0.8], sw["pluis"])])
        if staat == "snoes_blij":
            _bone(geo, "snoes_sjaal", "body", [0, 12, 0], [c([-5.6, 11.0, -3.6], [11.2, 2.2, 7.2], sw["sjaal"], inflate=0.1),
                                                           c([2.0, 7.4, -4.0], [2.2, 3.8, 0.8], sw["sjaal2"])])
        hulp.save_geo(h, f"guh_npc_{staat}.geo.json", geo_file)
        a = hulp.sitting_texture(h, hue=0.94, sat=sat, val=val)
        r = _rng(1820)
        if staat == "snoes":                           # a few dusty smudges on the pink fluff
            pink = (a[..., 3] > 0) & (np.abs(a[..., 0].astype(int) - a[..., 2].astype(int)) < 60)
            ys, xs = np.nonzero(pink)
            for i in r.choice(len(ys), 40, replace=False):
                y, x = ys[i], xs[i]
                a[max(0, y - 2):y + 3, max(0, x - 2):x + 3, :3] = (a[max(0, y - 2):y + 3, max(0, x - 2):x + 3, :3] * 0.86).astype(np.uint8)
        hulp.paint_swatch(a, sw["pleister"], (236, 196, 160), r, 4, _pleister)
        hulp.paint_swatch(a, sw["pluis"], (236, 222, 228) if staat == "snoes" else (255, 226, 238), r, 8)
        hulp.paint_swatch(a, sw["sjaal"], (255, 196, 90), r, 6, _pyjama_strepen(None, (255, 240, 200), 4))
        hulp.paint_swatch(a, sw["sjaal2"], (255, 196, 90), r, 6)
        h.save(Image.fromarray(a), "entity", f"npc_{staat}.png")
        out[staat] = (geo_file, a)

    # --- Zandguh: nachtjapon, a floppy slaapmuts with a star, a sack of droomzand (glittering, glowmask) ----------------------
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_zandguh")
    sw = hulp.swatches(geo, ["japon", "muts", "ster", "zak", "touw"])
    _bone(geo, "zand_japon", "body", [0, 7, 0], [c([-5.2, 2.0, -3.4], [10.4, 10.2, 6.8], sw["japon"], inflate=0.14)])
    muts = [c([-6.6, 25.0, -5.4], [13.2, 2.2, 10.0], sw["muts"])]
    for i, (w, y, z) in enumerate(((10.0, 27.0, -4.4), (7.6, 29.0, -3.0), (5.2, 30.8, -1.0), (3.2, 32.0, 1.6), (2.2, 32.4, 4.0))):
        muts.append(c([-w / 2, y, z], [w, 2.2, max(2.2, 8.0 - i * 1.6)], sw["muts"]))
    _bone(geo, "zand_muts", "head", [0, 26, 0], muts)
    _bone(geo, "zand_ster", "zand_muts", [0, 33, 6], [c([-1.4, 31.6, 5.8], [2.8, 2.8, 2.8], sw["ster"])])
    _bone(geo, "zand_zak", "body", [6, 4, -5], [c([4.6, 0.6, -8.2], [4.4, 4.6, 4.4], sw["zak"]),
                                                c([5.6, 5.2, -7.2], [2.4, 1.0, 2.4], sw["touw"])])
    hulp.save_geo(h, "guh_npc_zandguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.93, sat=0.75, val=1.0)
    r = _rng(1821)
    hulp.paint_swatch(a, sw["japon"], (64, 72, 150), r, 6, _sterretjes(10, (250, 230, 140), 3))
    hulp.paint_swatch(a, sw["muts"], (70, 80, 164), r, 6, _sterretjes(6, (250, 230, 140), 4))
    hulp.paint_swatch(a, sw["ster"], (255, 226, 90), r, 4)
    hulp.paint_swatch(a, sw["zak"], (218, 196, 150), r, 8, _glitter(26, 5))
    hulp.paint_swatch(a, sw["touw"], (150, 110, 80), r, 4)
    h.save(Image.fromarray(a), "entity", "npc_zandguh.png")
    gloed = np.zeros_like(a)
    for naam, kracht in (("ster", 1.0), ("zak", 0.0)):
        x, y = sw[naam]
        blok = a[y * 4:(y + 8) * 4, x * 4:(x + 8) * 4]
        doel = gloed[y * 4:(y + 8) * 4, x * 4:(x + 8) * 4]
        if kracht:
            doel[..., :3] = (blok[..., :3] * kracht).astype(np.uint8)
        else:                                          # only the glitter specks of the sack
            fel = blok[..., :3].min(-1) > 235
            doel[fel, :3] = (255, 250, 220)
        doel[..., 3] = 255
    for naam in ("japon", "muts"):                     # the little stars on the nightgown and the cap glow softly
        x, y = sw[naam]
        blok = a[y * 4:(y + 8) * 4, x * 4:(x + 8) * 4]
        doel = gloed[y * 4:(y + 8) * 4, x * 4:(x + 8) * 4]
        fel = (blok[..., 0] > 230) & (blok[..., 2] < 180)
        doel[fel, :3] = (200, 180, 110)
        doel[..., 3] = 255
    gloed[..., 3] = 255
    h.save(Image.fromarray(gloed), "entity", "npc_zandguh_glowmask.png")
    out["zandguh"] = (geo_file, a)

    # --- Wekkerguh: a red alarm clock on his back, bells on his ears ------------------------------------------------------------
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_wekkerguh")
    sw = hulp.swatches(geo, ["kast", "wijzer", "bel", "staal", "sleutel"])
    klok = [c([-4.5, 3.2, 3.0], [9.0, 8.0, 3.0], sw["kast"]), c([-3.5, 2.2, 3.0], [7.0, 10.0, 3.0], sw["kast"]),
            c([-3.6, 3.2, 6.0], [7.2, 8.0, 0.4], sw["wijzer"])]
    _bone(geo, "wekker", "body", [0, 7, 4], klok)
    _bone(geo, "wekker_bellen", "wekker", [0, 12, 4.5], [c([-4.6, 11.4, 3.4], [3.2, 2.2, 2.2], sw["bel"]),
                                                          c([1.4, 11.4, 3.4], [3.2, 2.2, 2.2], sw["bel"]),
                                                          c([-0.4, 12.0, 4.0], [0.8, 2.2, 0.8], sw["staal"]),
                                                          c([-1.0, 13.6, 3.8], [2.0, 1.0, 1.2], sw["staal"])])
    _bone(geo, "wekker_sleutel", "wekker", [4.5, 7, 4.5], [c([4.4, 6.6, 4.2], [2.2, 0.8, 0.8], sw["sleutel"]),
                                                           c([6.4, 5.4, 4.2], [0.8, 3.2, 1.6], sw["sleutel"])])
    _bone(geo, "wekker_belletje_l", "ear_left", [10, 28, -1], [c([9.6, 27.4, -2.3], [1.6, 1.6, 1.6], sw["bel"])])
    _bone(geo, "wekker_belletje_r", "ear_right", [-10, 28, -1], [c([-11.2, 27.4, -2.3], [1.6, 1.6, 1.6], sw["bel"])])
    hulp.save_geo(h, "guh_npc_wekkerguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.97, sat=0.95, val=1.0)
    r = _rng(1822)
    hulp.paint_swatch(a, sw["kast"], (214, 52, 64), r, 6)
    hulp.paint_swatch(a, sw["wijzer"], (252, 250, 244), r, 0, _klok)
    hulp.paint_swatch(a, sw["bel"], GOUD, r, 8)
    hulp.paint_swatch(a, sw["staal"], (190, 190, 200), r, 6)
    hulp.paint_swatch(a, sw["sleutel"], (230, 200, 90), r, 6)
    h.save(Image.fromarray(a), "entity", "npc_wekkerguh.png")
    out["wekkerguh"] = (geo_file, a)

    # --- Rommelguh: the Evivads look with a glass with a restje in each paw and crumbs on his belly ------------------------------
    geo_file, geo = _geo_van(h, "guh_npc_weeb_evivads", "geometry.guh_npc_rommelguh")
    sw = hulp.swatches(geo, ["glas", "sap", "cola", "kruimel"])
    _bone(geo, "rommel_glas_l", "arm_left", [3, 9, -7], [c([1.6, 7.0, -9.6], [2.8, 3.8, 2.8], sw["glas"]),
                                                         c([1.8, 7.1, -9.4], [2.4, 1.4, 2.4], sw["sap"], inflate=0.02)])
    _bone(geo, "rommel_glas_r", "arm_right", [-3, 9, -7], [c([-4.4, 7.0, -9.6], [2.8, 3.8, 2.8], sw["glas"]),
                                                           c([-4.2, 7.1, -9.4], [2.4, 1.0, 2.4], sw["cola"], inflate=0.02)])
    _bone(geo, "rommel_kruimels", "body", [0, 5, -3], [c([-2.2, 5.4, -3.5], [0.7, 0.5, 0.5], sw["kruimel"]),
                                                       c([1.0, 3.8, -3.5], [0.6, 0.6, 0.5], sw["kruimel"]),
                                                       c([-0.4, 2.6, -3.5], [0.7, 0.5, 0.5], sw["kruimel"]),
                                                       c([2.6, 7.0, -3.4], [0.5, 0.5, 0.5], sw["kruimel"])])
    hulp.save_geo(h, "guh_npc_rommelguh.geo.json", geo_file)
    a = _tex_van(h, "npc_weeb_evivads")
    r = _rng(1823)

    def glazig(block):
        block[..., :3] = (206, 236, 250)
        block[:, :3, :3] = (250, 254, 255)
        block[:4, :, :3] = (236, 248, 255)
    hulp.paint_swatch(a, sw["glas"], (206, 236, 250), r, 0, glazig)
    hulp.paint_swatch(a, sw["sap"], DRANKJES["sap"], r, 6)
    hulp.paint_swatch(a, sw["cola"], DRANKJES["cola"], r, 6)
    hulp.paint_swatch(a, sw["kruimel"], (226, 184, 110), r, 10)
    h.save(Image.fromarray(a), "entity", "npc_rommelguh.png")
    out["rommelguh"] = (geo_file, a)

    # --- Oma-guh with her knitting -----------------------------------------------------------------------------------------------
    geo_file, geo = _geo_van(h, "guh_npc_omaguh", "geometry.guh_npc_droom_omaguh")
    sw = hulp.swatches(geo, ["naald", "lapje", "wol"])
    _bone(geo, "oma_breiwerk", "body", [0, 8, -7], [c([-3.8, 7.6, -8.6], [7.6, 0.6, 0.6], sw["naald"], rotation=[0, 0, 18], pivot=[0, 8, -8.3]),
                                                     c([-3.8, 7.6, -8.6], [7.6, 0.6, 0.6], sw["naald"], rotation=[0, 0, -18], pivot=[0, 8, -8.3]),
                                                     c([-2.6, 4.2, -8.4], [5.2, 3.6, 0.8], sw["lapje"])])
    _bone(geo, "oma_wol", "body", [6, 2, -5], [c([4.6, 0.2, -7.6], [3.4, 3.4, 3.4], sw["wol"]),
                                                c([3.2, 2.4, -8.0], [1.6, 0.4, 0.4], sw["wol"])])
    hulp.save_geo(h, "guh_npc_droom_omaguh.geo.json", geo_file)
    a = _tex_van(h, "npc_omaguh")
    r = _rng(1824)
    hulp.paint_swatch(a, sw["naald"], (200, 200, 214), r, 4)
    hulp.paint_swatch(a, sw["lapje"], (250, 150, 196), r, 6, _gebreid)
    hulp.paint_swatch(a, sw["wol"], (190, 160, 240), r, 6, _gebreid)
    h.save(Image.fromarray(a), "entity", "npc_droom_omaguh.png")
    out["droom_omaguh"] = (geo_file, a)

    # --- Kussengevecht-guh: striped pyjamas, a slaapmutsje with a pompom, a pillow in his paws -------------------------------------
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_kussengevecht_guh")
    sw = hulp.swatches(geo, ["pyjama", "muts", "pompon", "kussen"])
    _bone(geo, "pyjama", "body", [0, 7, 0], [c([-5.2, 2.2, -3.2], [10.4, 9.8, 6.4], sw["pyjama"], inflate=0.12)])
    muts = [c([-6.4, 25.0, -5.2], [12.8, 2.0, 9.6], sw["muts"]), c([-4.6, 27.0, -3.8], [9.2, 2.2, 7.0], sw["muts"]),
            c([-2.8, 29.0, -2.0], [5.6, 2.0, 5.0], sw["muts"])]
    _bone(geo, "pyjama_muts", "head", [0, 26, 0], muts, rotation=[-8, 0, 10])
    _bone(geo, "pyjama_pompon", "pyjama_muts", [0, 31, 0], [c([-1.4, 30.8, -0.8], [2.8, 2.8, 2.8], sw["pompon"])])
    _bone(geo, "kussen", "body", [0, 8, -7], [c([-5.0, 4.6, -10.4], [10.0, 7.0, 3.4], sw["kussen"])])
    hulp.save_geo(h, "guh_npc_kussengevecht_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.95, sat=1.0, val=1.0)
    r = _rng(1825)
    hulp.paint_swatch(a, sw["pyjama"], (150, 196, 250), r, 4, _pyjama_strepen(None, (246, 250, 255), 5))
    hulp.paint_swatch(a, sw["muts"], (150, 196, 250), r, 4, _pyjama_strepen(None, (246, 250, 255), 5))
    hulp.paint_swatch(a, sw["pompon"], WIT, r, 6)
    hulp.paint_swatch(a, sw["kussen"], (255, 236, 244), r, 4, _kussen_rand)
    h.save(Image.fromarray(a), "entity", "npc_kussengevecht_guh.png")
    out["kussengevecht_guh"] = (geo_file, a)
    return out


def _kf(pairs):
    return {f"{t:g}": list(v) for t, v in pairs}


def animaties(h):
    """The two kinds with their own named animations: the sitting guh's file (idle, happy...) plus theirs."""
    basis = json.load(open(os.path.join(h.A, "geckolib", "animations", "entity", "guh_sitting.animation.json"), encoding="utf-8"))
    out = {}
    w = copy.deepcopy(basis)
    w["animations"]["animation.guh_sitting.rinkel"] = {"animation_length": 1.0, "bones": {
        "wekker": {"rotation": _kf([(0, (0, 0, 0)), (0.1, (0, 0, 8)), (0.2, (0, 0, -8)), (0.3, (0, 0, 8)), (0.4, (0, 0, -8)),
                                    (0.5, (0, 0, 8)), (0.6, (0, 0, -8)), (0.7, (0, 0, 6)), (0.8, (0, 0, -4)), (1.0, (0, 0, 0))])},
        "wekker_bellen": {"position": _kf([(0, (0, 0, 0)), (0.15, (0, 0.6, 0)), (0.3, (0, 0, 0)), (0.45, (0, 0.6, 0)), (0.6, (0, 0, 0))])},
        "head": {"rotation": _kf([(0, (0, 0, 0)), (0.2, (-10, 0, 0)), (0.8, (-10, 0, 0)), (1.0, (0, 0, 0))])},
        "ear_left": {"rotation": _kf([(0, (0, 0, 0)), (0.25, (0, 0, -18)), (0.5, (0, 0, 0)), (0.75, (0, 0, -18)), (1.0, (0, 0, 0))])},
        "ear_right": {"rotation": _kf([(0, (0, 0, 0)), (0.25, (0, 0, 18)), (0.5, (0, 0, 0)), (0.75, (0, 0, 18)), (1.0, (0, 0, 0))])}}}
    out["wekkerguh"] = w
    k = copy.deepcopy(basis)
    k["animations"]["animation.guh_sitting.gooi"] = {"animation_length": 0.7, "bones": {
        "kussen": {"rotation": _kf([(0, (0, 0, 0)), (0.2, (60, 0, 0)), (0.38, (-95, 0, 0)), (0.5, (-110, 0, 0)), (0.7, (0, 0, 0))]),
                   "position": _kf([(0, (0, 0, 0)), (0.2, (0, 3, 2)), (0.38, (0, 6, -3)), (0.5, (0, 4, -5)), (0.7, (0, 0, 0))])},
        "arm_left": {"rotation": _kf([(0, (0, 0, 0)), (0.2, (-80, 0, 0)), (0.4, (40, 0, 0)), (0.7, (0, 0, 0))])},
        "arm_right": {"rotation": _kf([(0, (0, 0, 0)), (0.2, (-80, 0, 0)), (0.4, (40, 0, 0)), (0.7, (0, 0, 0))])},
        "body": {"rotation": _kf([(0, (0, 0, 0)), (0.2, (8, 0, 0)), (0.4, (-10, 0, 0)), (0.7, (0, 0, 0))])},
        "pyjama_muts": {"rotation": _kf([(0, (-8, 0, 10)), (0.4, (12, 0, 4)), (0.7, (-8, 0, 10))])}}}
    out["kussengevecht_guh"] = k
    for naam, d in out.items():
        h.w(os.path.join(h.A, "geckolib", "animations", "entity", f"guh_npc_{naam}.animation.json"), d)
    return out


# =====================================================================================================================
# the Sterrenvisje (64x64 uv, 4x textures)
# =====================================================================================================================
SV_UV, SV_PX = 64, 4


class _Atlas:
    def __init__(self):
        self.x = self.y = self.rij = 0
        self.regios = []

    def neem(self, w, h):
        w, h = max(1, int(math.ceil(w))), max(1, int(math.ceil(h)))
        if self.x + w > SV_UV:
            self.x, self.y, self.rij = 0, self.y + self.rij + 1, 0
        r = (self.x, self.y, w, h)
        self.x += w + 1
        self.rij = max(self.rij, h)
        return r


def _face_dims(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def _sv_cube(atlas, rol, origin, size, rotation=None, pivot=None):
    uv = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        w, h = _face_dims(f, size)
        x, y, ww, hh = atlas.neem(w, h)
        atlas.regios.append((rol, f, (x, y, ww, hh)))
        uv[f] = {"uv": [x, y], "uv_size": [w, h]}
    cube = {"origin": origin, "size": size, "uv": uv}
    if rotation:
        cube["rotation"], cube["pivot"] = rotation, pivot or origin
    return cube


def sterrenvisje_geo():
    at = _Atlas()
    lijf = [_sv_cube(at, "lijf", [-1.5, 0.5, -3.0], [3, 3, 5]), _sv_cube(at, "snoet", [-1.0, 1.0, -3.8], [2, 2, 0.8])]
    staart = [_sv_cube(at, "ster", [-0.25, 0.0, 2.0], [0.5, 4, 1.6]), _sv_cube(at, "ster", [-0.25, 1.2, 1.6], [0.5, 1.6, 3.0]),
              _sv_cube(at, "ster", [-0.25, 0.4, 2.4], [0.5, 3.2, 2.2], rotation=[45, 0, 0], pivot=[0, 2, 3.5])]
    rug = [_sv_cube(at, "ster", [-0.25, 3.5, -1.2], [0.5, 1.2, 1.6]), _sv_cube(at, "ster", [-0.25, 3.9, -1.8], [0.5, 0.5, 2.8])]
    vinnen = [_sv_cube(at, "ster", [1.5, 1.0, -1.4], [1.2, 0.4, 1.2]), _sv_cube(at, "ster", [-2.7, 1.0, -1.4], [1.2, 0.4, 1.2])]
    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "lijf", "parent": "root", "pivot": [0, 2, 0], "cubes": lijf},
             {"name": "staart", "parent": "lijf", "pivot": [0, 2, 2], "cubes": staart},
             {"name": "rugvin", "parent": "lijf", "pivot": [0, 3.5, -1], "cubes": rug},
             {"name": "vinnen", "parent": "lijf", "pivot": [0, 1.2, -1], "cubes": vinnen}]
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.sterrenvisje", "texture_width": SV_UV, "texture_height": SV_UV,
                        "visible_bounds_width": 1, "visible_bounds_height": 1, "visible_bounds_offset": [0, 0.25, 0]},
        "bones": bones}]}
    return geo, at.regios


def _licht(c, f):
    return tuple(int(min(255, v * f + 255 * (1 - f) * 0.0)) for v in c)


def sterrenvisje_tex(regios, kleur):
    S = SV_PX
    img = np.zeros((SV_UV * S, SV_UV * S, 4), np.uint8)
    gloed = np.zeros_like(img)
    gloed[..., 3] = 255
    r = _rng(1826)
    k = np.array(kleur, float)
    for rol, f, (x, y, w, hh) in regios:
        blok = img[y * S:(y + hh) * S, x * S:(x + w) * S]
        g = gloed[y * S:(y + hh) * S, x * S:(x + w) * S]
        if rol == "ster":
            basis = np.clip(k * 0.35 + 255 * 0.65, 0, 255)
            blok[..., :3] = basis + r.normal(0, 4, blok.shape[:2] + (1,))
            g[..., :3] = basis * 0.95
        else:
            basis = k * (0.88 if f in ("down",) else 1.0)
            blok[..., :3] = np.clip(basis + r.normal(0, 5, blok.shape[:2] + (1,)), 0, 255)
            g[..., :3] = k * 0.35
            if rol == "lijf" and f in ("east", "west"):     # the eye near the front, a blush behind it, two tiny stars
                hh_px, w_px = blok.shape[:2]
                front = 1 if f == "east" else 0
                ex = int(w_px * (0.18 if front else 0.82))
                ey = int(hh_px * 0.38)
                yy, xx = np.mgrid[0:hh_px, 0:w_px]
                oog = np.hypot(xx - ex, yy - ey) < S * 0.42
                blok[oog, :3] = (30, 26, 40)
                blok[(np.hypot(xx - ex + 0.6, yy - ey + 0.6) < S * 0.16), :3] = (255, 255, 255)
                g[oog, :3] = 0
                bx = ex + (S if front else -S)
                blos = np.hypot(xx - bx, yy - ey - S * 0.8) < S * 0.4
                blok[blos, :3] = (255, 140, 170)
            if rol == "lijf" and f == "up":
                blok[..., :3] = np.clip(blok[..., :3] * 1.05, 0, 255)
        blok[..., 3] = 255
    return Image.fromarray(img), Image.fromarray(gloed)


def sterrenvisje_anim():
    return {"format_version": "1.8.0", "animations": {
        "zwem": {"loop": True, "animation_length": 1.0, "bones": {
            "lijf": {"rotation": _kf([(0, (0, -6, 0)), (0.5, (0, 6, 0)), (1.0, (0, -6, 0))])},
            "staart": {"rotation": _kf([(0, (0, 22, 0)), (0.5, (0, -22, 0)), (1.0, (0, 22, 0))])},
            "vinnen": {"rotation": _kf([(0, (0, 0, 10)), (0.25, (0, 0, -10)), (0.5, (0, 0, 10)), (0.75, (0, 0, -10)), (1.0, (0, 0, 10))])}}},
        "spartel": {"loop": True, "animation_length": 0.5, "bones": {
            "lijf": {"rotation": _kf([(0, (0, 0, 70)), (0.25, (0, 0, 110)), (0.5, (0, 0, 70))]),
                     "position": _kf([(0, (0, 0, 0)), (0.25, (0, 1, 0)), (0.5, (0, 0, 0))])},
            "staart": {"rotation": _kf([(0, (0, 35, 0)), (0.25, (0, -35, 0)), (0.5, (0, 35, 0))])}}}}}


def sterrenvisje(h):
    geo, regios = sterrenvisje_geo()
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "sterrenvisje.geo.json"), geo)
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "sterrenvisje.animation.json"), sterrenvisje_anim())
    eerste = None
    for naam, kleur in KLEUREN.items():
        tex, gloed = sterrenvisje_tex(regios, kleur)
        h.save(tex, "entity", f"sterrenvisje_{naam}.png")
        h.save(gloed, "entity", f"sterrenvisje_{naam}_glowmask.png")
        eerste = eerste or tex
    return geo, eerste


# =====================================================================================================================
# items: the bucket and the spawn egg, the glas met een restje
# =====================================================================================================================
def emmer_icoon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(3, 6), (12, 6), (11, 14), (4, 14)], fill=(150, 156, 170))
    d.line([(3, 6), (12, 6)], fill=(200, 206, 216))
    d.rectangle([4, 7, 11, 9], fill=(150, 130, 230))
    d.arc([2, 1, 13, 10], 200, 340, fill=(110, 116, 130))
    for (x, y) in ((6, 10), (9, 11)):
        img.putpixel((x, y), (255, 226, 120))
    for (x, y) in ((7, 8), (10, 8)):
        img.putpixel((x, y), (255, 250, 220))
    return img


def glas_textures(h):
    g = np.zeros((16, 16, 4), np.uint8)
    g[..., :3] = (214, 238, 250)
    g[..., 3] = 120
    g[:, 0:2, 3] = 200
    g[0:2, :, :3] = (250, 254, 255)
    g[0:2, :, 3] = 220
    h.save(Image.fromarray(g), "block", "glas_restje_glas.png")
    r = _rng(1827)
    for naam, kleur in DRANKJES.items():
        a = np.zeros((16, 16, 4), np.uint8)
        a[..., :3] = np.clip(np.array(kleur) + r.normal(0, 6, (16, 16, 1)), 0, 255)
        a[0:2, :, :3] = np.clip(np.array(kleur) * 1.15 + 20, 0, 255)
        a[..., 3] = 235
        if naam == "cola":
            for _ in range(5):
                a[r.integers(3, 15), r.integers(1, 15), :3] = (190, 140, 110)
        h.save(Image.fromarray(a), "block", f"glas_restje_{naam}.png")


# glass positions per count (block pixels: x, z of the glass's corner)
GLAS_PLEKKEN = {1: [(6.5, 6.5)], 2: [(4.5, 7.5), (9.0, 5.0)], 3: [(4.0, 8.5), (9.5, 8.0), (6.5, 3.5)]}


def glas_model(n, drankje):
    els = []
    for i, (x, z) in enumerate(GLAS_PLEKKEN[n]):
        hoog = 5.0 if i % 2 == 0 else 4.2
        rest = (1.4, 0.9, 1.8)[i % 3]
        els.append({"from": [x + 0.3, 0.05, z + 0.3], "to": [x + 2.7, 0.05 + rest, z + 2.7],
                    "faces": {f: {"texture": "#drank", "uv": [0, 0, 2.4 if f not in ("up", "down") else 2.4, rest if f not in ("up", "down") else 2.4]}
                              for f in ("north", "south", "east", "west", "up", "down")}})
        els.append({"from": [x, 0, z], "to": [x + 3, hoog, z + 3],
                    "faces": {f: {"texture": "#glas", "uv": [0, 0, 3, hoog if f not in ("up", "down") else 3]}
                              for f in ("north", "south", "east", "west", "down")}})
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "ambientocclusion": False,
            "textures": {"particle": "guhs:block/glas_restje_glas", "glas": "guhs:block/glas_restje_glas",
                         "drank": f"guhs:block/glas_restje_{drankje}"},
            "elements": els}


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def glas(h):
    A, D, w = h.A, h.D, h.w
    glas_textures(h)
    varianten = {}
    for n in (1, 2, 3):
        for dr in DRANKJES:
            w(f"{A}/models/block/glas_restje_{n}_{dr}.json", glas_model(n, dr))
            for f, y in ROT.items():
                v = {"model": f"guhs:block/glas_restje_{n}_{dr}"}
                if y:
                    v["y"] = y
                varianten[f"drankje={dr},facing={f},glazen={n}"] = v
    w(f"{A}/blockstates/glas_restje.json", {"variants": varianten})
    w(f"{A}/models/item/glas_restje.json", {"parent": "guhs:block/glas_restje_2_sap"})
    w(f"{D}/loot_table/blocks/glas_restje.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "guhs:glas_restje", "functions": [
            {"function": "minecraft:set_count", "add": False, "count": float(n),
             "conditions": [{"condition": "minecraft:block_state_property", "block": "guhs:glas_restje", "properties": {"glazen": str(n)}}]}
            for n in (2, 3)] + [{"function": "minecraft:explosion_decay"}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(f"{D}/recipe/glas_restje.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["G G", " G "],
                                       "key": {"G": "minecraft:glass_pane"}, "result": {"id": "guhs:glas_restje", "count": 3}})


# =====================================================================================================================
# texts
# =====================================================================================================================
TEKSTEN = {
    "entity.guhs.guh_npc.snoes": ("Snoes", "Snoes"),
    "entity.guhs.guh_npc.snoes_blij": ("Snoes", "Snoes"),
    "entity.guhs.guh_npc.zandguh": ("Zandguh", "Zandguh"),
    "entity.guhs.guh_npc.wekkerguh": ("Wekkerguh", "Wekkerguh"),
    "entity.guhs.guh_npc.rommelguh": ("Rommelguh", "Rommelguh"),
    "entity.guhs.guh_npc.droom_omaguh": ("Oma-guh", "Oma-guh"),
    "entity.guhs.guh_npc.kussengevecht_guh": ("Kussengevecht-guh", "Pillow fight guh"),
    "entity.guhs.sterrenvisje": ("Sterrenvisje", "Starfishlet"),
    "entity.guhs.kussen_projectiel": ("Kussentje", "Little pillow"),
    "item.guhs.sterrenvisje_spawn_egg": ("Sterrenvisje-spawnei", "Starfishlet Spawn Egg"),
    "item.guhs.sterrenvisje_emmer": ("Emmer met een sterrenvisje", "Bucket of Starfishlet"),
    "block.guhs.glas_restje": ("Glas met een restje", "Glass with a little bit left"),
    "item.guhs.glas_restje": ("Glas met een restje", "Glass with a little bit left"),
    "block.guhs.glas_restje.lore": ("Niet weggooien, die is nog niet leeg! (Rommelguh)", "Don't throw it away, it's not empty yet! (Rommelguh)"),
    "block.guhs.glas_restje.lore2": ("Klik met nog een glas: erbij. Lege hand: een slokje.", "Click with another glass: one more. Empty hand: a sip."),
    "gui.guhs.glas_restje.slokje": ("*slurp* Njeg... lauw. Precies goed.", "*slurp* Njeg... lukewarm. Just right."),
    "gui.guhs.glas_restje.slokje2": ("Je neemt een slokje. Er blijft natuurlijk een restje over.", "You take a sip. A little bit stays behind, of course."),
    "gui.guhs.glas_restje.slokje3": ("Hmm, %s van gisteren. Vintage.", "Hmm, yesterday's %s. Vintage."),
    "gui.guhs.glas_restje.drankje.sap": ("sinaasappelsap", "orange juice"),
    "gui.guhs.glas_restje.drankje.cola": ("cola", "cola"),
    "gui.guhs.glas_restje.drankje.limo": ("limonade", "lemonade"),
    "gui.guhs.glas_restje.drankje.melk": ("melk", "milk"),
    "gui.guhs.glas_restje.drankje.thee": ("thee", "tea"),
    "gui.guhs.sterrenvisje.kleur.geel": ("geel", "yellow"),
    "gui.guhs.sterrenvisje.kleur.roze": ("roze", "pink"),
    "gui.guhs.sterrenvisje.kleur.lila": ("lila", "lilac"),
    "gui.guhs.sterrenvisje.kleur.munt": ("munt", "mint"),
    "gui.guhs.droomwezens.onbekend": ("Onbekend droomwezen: %s. Kies uit: %s", "Unknown dream creature: %s. Pick from: %s"),
    "gui.guhs.droomwezens.gespawnd": ("%s staat voor je neus, njeg!", "%s is right in front of you, njeg!"),
    "subtitles.guhs.droomwezens.kussen": ("Kussentje ploft", "Pillow plops"),
    "subtitles.guhs.droomwezens.rinkel": ("Wekker rinkelt", "Alarm clock rings"),
    "subtitles.guhs.droomwezens.slokje": ("Slurp", "Slurp"),
}
GRAPPEN = {
    "snoes": [("...njeg. Ik ben gewoon een beetje moe. Al heel lang.", "...njeg. I'm just a little tired. For a long time now."),
              ("Iedereen heeft een plekje om te slapen. Ik niet.", "Everyone has a little place to sleep. Not me."),
              ("Mijn oortje? Dat knakt al sinds... altijd eigenlijk.", "My ear? It's been bent since... always, really."),
              ("Als ik mijn ogen dichtdoe, is het zo stil. Te stil.", "When I close my eyes, it's so quiet. Too quiet.")],
    "snoes_blij": [("Ik heb geslapen! Echt geslapen! Njeg njeg!", "I slept! Really slept! Njeg njeg!"),
                   ("Die doos is het fijnste plekje van de hele wereld.", "That box is the coziest spot in the whole world."),
                   ("Welkom in Guhdromenland! Welterusten alvast!", "Welcome to Guhdreamland! Sweet dreams already!"),
                   ("Het kattenhuisje is ook heel mooi hoor. Voor overdag.", "The cat house is very pretty too, though. For daytime.")],
    "zandguh": [("Een snufje droomzand, en hup: de mooiste dromen. Niet in je ogen wrijven!", "A pinch of dream sand and poof: the nicest dreams. Don't rub your eyes!"),
                ("Ik strooi al vierhonderd jaar. Mijn zak is nog nooit leeg geweest. Vahoeg.", "I've been sprinkling for four hundred years. My sack has never been empty. Vahoeg."),
                ("Gaap jij nou? Mooi zo. Dan werkt het.", "Are you yawning? Good. Then it works.")],
    "wekkerguh": [("TRRRRRING! Oh, sorry. Reflex.", "BRRRRRING! Oh, sorry. Reflex."),
                  ("Nog vijf minuutjes? Dat zegt iedereen. Altijd.", "Five more minutes? Everyone says that. Always."),
                  ("Wil je wakker worden? Dan rinkel ik je zachtjes terug naar je bedje.", "Want to wake up? Then I'll ring you gently back to your bed."),
                  ("Ik heb al 3 keer gesnoozed vandaag. Records breken, njeg.", "I've hit snooze 3 times today already. Breaking records, njeg.")],
    "rommelguh": [("Niet weggooien! Die is nog niet leeg!", "Don't throw it away! It's not empty yet!"),
                  ("Ik verzamel glazen. Met een restje. Dat is een hobby.", "I collect glasses. With a little bit left. It's a hobby."),
                  ("Opruimen? Morgen. Of overmorgen. Of in een droom.", "Tidy up? Tomorrow. Or the day after. Or in a dream."),
                  ("Deze is van dinsdag. Of vorige dinsdag. Proost!", "This one's from Tuesday. Or last Tuesday. Cheers!")],
    "droom_omaguh": [("Ik brei een sjaaltje. Voor wie? Dat zie je vanzelf, vadsje.", "I'm knitting a scarf. Who for? You'll see, dear."),
                     ("Nog een koekje, vadsje? Er zijn er genoeg.", "Another cookie, dear? There are plenty."),
                     ("Op de familiedag waren we met zo veel. En toch miste ik er eentje...", "At the family day there were so many of us. And still I missed one..."),
                     ("Steek, steek, laat vallen... ach, een gaatje is gezellig.", "Stitch, stitch, drop... oh well, a little hole is cozy.")],
    "kussengevecht_guh": [("KUSSENGEVECHT! Niet op je snoet, dat is de regel.", "PILLOW FIGHT! Not on the snout, that's the rule."),
                          ("Pas op, deze is extra pluizig! PLOF!", "Watch out, this one's extra fluffy! FLUMP!"),
                          ("Ik heb nog nooit verloren. Ik heb ook nog nooit gewonnen. Njeg.", "I've never lost. I've never won either. Njeg."),
                          ("Veertjes overal. Dat is hoe je weet dat het een goed gevecht was.", "Feathers everywhere. That's how you know it was a good fight.")],
}
for _k, _gr in GRAPPEN.items():
    for _i, (_nl, _en) in enumerate(_gr):
        TEKSTEN[f"gui.guhs.droomwezens.{_k}.grap.{_i}"] = (_nl, _en)


# =====================================================================================================================
# the Blockbench projects (to look at / tweak; the game files come from here)
# =====================================================================================================================
def bbmodel(geo_file, tex_png, anims=None, naam="model"):
    g = geo_file["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    rnd = __import__("random").Random(1820)

    def uid():
        return str(uuid.UUID(int=rnd.getrandbits(128), version=4))
    buf = io.BytesIO()
    tex_png.save(buf, "PNG")
    elements, groepen = [], {}
    for b in g["bones"]:
        px, py, pz = b["pivot"]
        rot = b.get("rotation", [0, 0, 0])
        groepen[b["name"]] = {"name": b["name"], "origin": [-px, py, pz], "rotation": [-rot[0], -rot[1], rot[2]], "uuid": uid(),
                              "export": True, "isOpen": False, "visibility": True, "children": [], "parent": b.get("parent")}
        for cu in b.get("cubes", []):
            (ox, oy, oz), (sx, sy, sz) = cu["origin"], cu["size"]
            crot = cu.get("rotation", [0, 0, 0])
            piv = cu.get("pivot", [ox + sx / 2, oy + sy / 2, oz + sz / 2])
            e = {"name": b["name"], "type": "cube", "uuid": uid(), "rescale": False, "locked": False,
                 "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz], "origin": [-piv[0], piv[1], piv[2]],
                 "rotation": [-crot[0], -crot[1], crot[2]], "inflate": cu.get("inflate", 0), "autouv": 0, "color": 0}
            uv = cu.get("uv", [0, 0])
            if isinstance(uv, dict):
                e["box_uv"] = False
                e["faces"] = {f: {"uv": [d["uv"][0], d["uv"][1], d["uv"][0] + d["uv_size"][0], d["uv"][1] + d["uv_size"][1]], "texture": 0}
                              for f, d in uv.items()}
            else:
                e["box_uv"] = True
                e["uv_offset"] = list(uv)
                if cu.get("mirror"):
                    e["mirror_uv"] = True
            elements.append(e)
            groepen[b["name"]]["children"].append(e["uuid"])
    outliner = []
    for grp in groepen.values():
        parent = grp.pop("parent")
        (groepen[parent]["children"] if parent in groepen else outliner).append(grp)
    animaties_bb = []
    for an, a in ((anims or {}).get("animations") or {}).items():
        animators = {}
        for bn, kanalen in a.get("bones", {}).items():
            if bn not in groepen:
                continue
            kfs = []
            for kanaal, waarde in kanalen.items():
                punten = waarde.items() if isinstance(waarde, dict) else [("0", waarde)]
                for t, v in punten:
                    if isinstance(v, dict):
                        v = v.get("post", v.get("vector", [0, 0, 0]))
                    if not isinstance(v, list):
                        v = [v, v, v]
                    kfs.append({"channel": kanaal, "data_points": [{"x": str(v[0]), "y": str(v[1]), "z": str(v[2])}],
                                "uuid": uid(), "time": float(t), "interpolation": "linear"})
            animators[groepen[bn]["uuid"]] = {"name": bn, "type": "bone", "keyframes": kfs}
        animaties_bb.append({"uuid": uid(), "name": an, "loop": "loop" if a.get("loop") else "once",
                             "length": a.get("animation_length", 1), "snapping": 24, "animators": animators})
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False}, "name": naam,
            "model_identifier": naam.lower(), "visible_box": [1.6, 2.2, 0.9], "resolution": {"width": tw, "height": th},
            "elements": elements, "outliner": outliner,
            "textures": [{"name": naam + ".png", "id": "0", "width": tex_png.width, "height": tex_png.height, "uv_width": tw, "uv_height": th,
                          "uuid": uid(), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}],
            "animations": animaties_bb}


BB_NAMEN = {"snoes": "Snoes", "snoes_blij": "SnoesBlij", "zandguh": "Zandguh", "wekkerguh": "Wekkerguh", "rommelguh": "Rommelguh",
            "droom_omaguh": "DroomOmaguh", "kussengevecht_guh": "KussengevechtGuh"}


def blockbench(h, modellen, anims, vis):
    os.makedirs("blockbench", exist_ok=True)
    basis_anim = json.load(open(os.path.join(h.A, "geckolib", "animations", "entity", "guh_sitting.animation.json"), encoding="utf-8"))
    for k, (geo_file, a) in modellen.items():
        with open(os.path.join("blockbench", BB_NAMEN[k] + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(bbmodel(geo_file, Image.fromarray(a), anims.get(k, basis_anim), BB_NAMEN[k]), f)
    geo, tex = vis
    with open(os.path.join("blockbench", "Sterrenvisje.bbmodel"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(bbmodel(geo, tex, sterrenvisje_anim(), "Sterrenvisje"), f)


# =====================================================================================================================
def build(h, bb=False):
    A, D, w = h.A, h.D, h.w
    modellen = npcs(h)
    anims = animaties(h)
    vis = sterrenvisje(h)
    glas(h)
    h.save(emmer_icoon(), "item", "sterrenvisje_emmer.png")
    h.item_model("sterrenvisje_emmer")
    w(f"{A}/models/item/sterrenvisje_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    w(f"{D}/loot_table/entities/sterrenvisje.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "minecraft:glowstone_dust", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})

    def patch(d):
        d["droomwezens.kussen"] = {"sounds": [{"name": "minecraft:dig/cloth1", "pitch": 1.2}, {"name": "minecraft:dig/cloth3", "pitch": 1.1}],
                                   "subtitle": "subtitles.guhs.droomwezens.kussen"}
        d["droomwezens.rinkel"] = {"sounds": [{"name": "minecraft:note/bell", "pitch": 1.5}, {"name": "minecraft:note/bell", "pitch": 1.34}],
                                   "subtitle": "subtitles.guhs.droomwezens.rinkel"}
        d["droomwezens.slokje"] = {"sounds": [{"name": "minecraft:random/drink", "pitch": 1.2, "volume": 0.6}],
                                   "subtitle": "subtitles.guhs.droomwezens.slokje"}
    h.patch_json(f"{A}/sounds.json", patch)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    if bb:
        blockbench(h, modellen, anims, vis)
    selfcheck(h)


def selfcheck(h):
    for k in NPCS:
        assert os.path.exists(os.path.join(h.A, "geckolib", "models", "entity", f"guh_npc_{k}.geo.json")), k
        assert os.path.exists(os.path.join(h.TEX, "entity", f"npc_{k}.png")), k
    for k in GLOED:
        assert os.path.exists(os.path.join(h.TEX, "entity", f"npc_{k}_glowmask.png")), k


ITEMS = ["sterrenvisje_emmer", "sterrenvisje_spawn_egg", "glas_restje"]


def _direct(bb):
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h, bb)
    en_pad = os.path.join("tools", "lang", "en", "c212_droomwezens.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    _mc26()
    print(f"droomwezens: {len(NPCS)} npcs, sterrenvisje x{len(KLEUREN)}, glas_restje, {len(TEKSTEN)} texts")


def _mc26():
    import zipfile
    import mc26
    naam = "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"
    for base in (os.path.join("build", "moddev", "artifacts"), os.path.join("..", "guhs", "build", "moddev", "artifacts"),
                 os.path.join("..", "..", "guhs", "build", "moddev", "artifacts"), os.path.join("..", "w-blub", "build", "moddev", "artifacts")):
        if os.path.exists(os.path.join(base, naam)):
            mc26._JAR = zipfile.ZipFile(os.path.join(base, naam))
            break
    mc26.SPAWN_EGGS = {"sterrenvisje_spawn_egg": mc26.SPAWN_EGGS["sterrenvisje_spawn_egg"]}
    mc26.spawn_eggs()
    for n in ["sterrenvisje_emmer", "sterrenvisje_spawn_egg", "glas_restje"]:
        with open(os.path.join(mc26.A, "items", n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    _direct(len(sys.argv) > 1 and sys.argv[1] == "bb")
