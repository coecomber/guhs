"""
1.10 "Het Vadsrijk", zone 3 "De Schemerwortel", the bazen slice: ONLY the art (models, textures, animations, blocks, icons).
The texts and the build(h) hook live in vadsrijk_z3_bazen.py, which calls alles().

Writes (textures in textures/vadsrijk/z3bazen/, GeckoLib models + animations under geckolib/*/entity/, Blockbench copies in
blockbench/vadsrijk/z3/):
  vr_z3_baas_kruinroofvogel  root, body, nek, head, snavel, vleugel_l, vleugel2_l, vleugel_r, vleugel2_r, staart, poot_l, poot_r
                             (idle, walk, vlieg, duik, wind, grijp, hop, sprong) + glowmask (eyes)
  vr_z3_baas_taleggio|brokkel|roquefort|piepvel|bleu   the mika.geo.json bones + accessories (idle, walk, slag, worp, draai, sprong;
                             taleggio +touw/hang, brokkel +platen/plaat_1..4, roquefort +paraplu/sabel, piepvel +hemdje, cape,
                             cape_1..4, kraag/capezwier, buig; also piepvel_sokken.png)
  vr_z3_baas_wortelwurger    root, kern, wortel_1..6 (idle, walk, grijp, klop) + glowmask
  vr_z3_baas_nestwachter     root, body, head, arm_l, arm_r, been_l, been_r, kristallen (idle, walk, stoot, stamp, buig) + roze + glowmasks
  vr_z3_baas_schimmelkolos   root, ketel, deksel, pijp, poot_1..4, grijparm (idle, walk, pomp, grijp)
  vr_z3_baas_vahoegnir       + Vahoegje: everything from vadsrijk_draak.py (the redesigned dragon, o-draak)
  vr_huisdier_vahoegje / vr_huisdier_piepje   pet animations (idle, hop, sprong); muisje_kaal.png
  vr_z3_baas_keizerarm       root, arm, onderarm, hand, vinger_1..5 (idle, grijp, tik; plus walk = idle)
  props/adds: kooi, ballon, voedingswortel, kanon, ketel, schimmelbol, kloddertje; matroos.png (mika texture)
  blocks: lantaarnpaal (lit), altaar, poort, hendel (stand 0..3), ringknop (stand 0..7), nerf, nesttak (barst)
  item icons: 15 items + 9 trophies.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_bazen_modellen.py
"""
import copy
import json
import math
import os

import numpy as np
from PIL import Image

try:
    from features.vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel, _icoon, _rng
    from features.vadsrijk_draak import alles as draak_alles
except ImportError:  # standalone
    from vadsrijk_bazen import Geo, vlak, disc, lijn, kf, bbmodel, _icoon, _rng
    from vadsrijk_draak import alles as draak_alles

TEX = "textures/vadsrijk/z3bazen"

SCHIMMEL = (138, 154, 176)
SCHIMMEL_DONKER = (92, 104, 126)
HONING = (240, 184, 64)
AMBERROZE = (255, 120, 190)
HOUT = (150, 104, 64)
HOUT_DONKER = (96, 64, 40)
GRIJS_HOUT = (128, 124, 120)
IJZER = (84, 86, 98)
KAAS = (246, 206, 92)


def leeg(W, H, f):
    return np.zeros((H, W, 4), np.uint8)


def schimmel(a, W, H, f, r):
    """Grey-blue mould spots."""
    for _ in range(max(1, W * H // 120)):
        disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0.8, 2.6), SCHIMMEL, alpha=0.55)


def veren(a, W, H, f, r):
    """Ragged feathers: V-strokes, a few rotten mould spots."""
    if f == "down":
        return
    for y in np.arange(1, H, 3.5):
        for x in np.arange(r.uniform(0, 3), W, 4):
            lijn(a, x, y, x + 1.6, y + 2.2, (70, 70, 80), dik=0.8)
            lijn(a, x + 3.2, y, x + 1.6, y + 2.2, (70, 70, 80), dik=0.8)
    schimmel(a, W, H, f, r)


def nerven(a, W, H, f, r):
    """Wood grain."""
    for _ in range(max(2, W // 3)):
        x = r.uniform(0, W)
        lijn(a, x, 0, x + r.uniform(-2, 2), H, HOUT_DONKER, dik=0.7)


def bont(a, W, H, f, r):
    """Short fur strokes."""
    for _ in range(max(2, W * H // 12)):
        x, y = r.uniform(0, W), r.uniform(0, H)
        lijn(a, x, y, x + r.uniform(-0.8, 0.8), y + 1.6, (90, 90, 98), dik=0.6)


def ogen(kleur, boos=True, verdrietig=False, glow=False, basis=None, pupil=(30, 20, 30)):
    """A face painter: two eyes (glow = only the eyes, for a glowmask)."""
    def p(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not glow and basis is not None:
            a[..., :3] = basis
            a[..., :3] += _rng(W + H).normal(0, 4, (H, W, 1))
            a[..., 3] = 255
        for x in (W * 0.3, W * 0.7):
            disc(a, x, H * 0.42, W * 0.12, kleur, ry=H * 0.11)
            if not glow:
                disc(a, x, H * 0.44, W * 0.05, pupil)
                disc(a, x - W * 0.04, H * 0.38, W * 0.025, (255, 255, 255))
        if not glow and boos:
            lijn(a, W * 0.12, H * 0.22, W * 0.42, H * 0.32, (40, 36, 44), dik=max(1.2, W * 0.05))
            lijn(a, W * 0.88, H * 0.22, W * 0.58, H * 0.32, (40, 36, 44), dik=max(1.2, W * 0.05))
        if not glow and verdrietig:
            lijn(a, W * 0.15, H * 0.3, W * 0.4, H * 0.22, (40, 36, 44), dik=max(1, W * 0.04))
            lijn(a, W * 0.85, H * 0.3, W * 0.6, H * 0.22, (40, 36, 44), dik=max(1, W * 0.04))
        return np.clip(a, 0, 255).astype(np.uint8)
    return p


def gloei(kleur, seed, glow):
    """A glowing part: normal painter, or (glowmask) the same colour brightened."""
    def p(W, H, f):
        a = vlak(kleur, 8, seed)(W, H, f)
        if glow:
            a[..., :3] = np.clip(a[..., :3].astype(np.int32) + 40, 0, 255)
        return a
    return p


# =====================================================================================================================
# 1. De Kruinroofvogel
# =====================================================================================================================
def kruinroofvogel(glow=False):
    g = Geo("kruinroofvogel", 256, 256, 2)

    def w(p):
        return leeg if glow else p
    veer = w(vlak((126, 126, 136), 10, 301, extra=veren, verloop=(96, 98, 112)))
    buik = w(vlak((172, 168, 160), 8, 302, extra=schimmel, verloop=(140, 138, 140)))
    snavel = w(vlak((214, 196, 120), 6, 303, verloop=(160, 140, 80)))
    klauw = w(vlak((70, 64, 60), 5, 304))
    gezicht = ogen((236, 222, 110), glow=glow, basis=(126, 126, 136))
    g.bone("root")
    g.bone("body", "root", (0, 22, 0))
    g.cube("body", (-9, 13, -9), (18, 19, 20), veer, faces={"north": buik, "down": buik})
    g.bone("nek", "body", (0, 30, -6))
    g.cube("nek", (-5, 28, -12), (10, 9, 8), veer)
    g.bone("head", "nek", (0, 36, -10))
    g.cube("head", (-6, 34, -19), (12, 9, 11), veer, faces={"north": gezicht})
    g.cube("head", (-6.5, 41, -15), (13, 2, 6), veer, rotation=(-15, 0, 0))   # ragged crest
    g.bone("snavel", "head", (0, 37, -19))
    g.cube("snavel", (-2, 34, -25), (4, 4, 6), snavel)
    g.cube("snavel", (-1.5, 31, -25), (3, 3, 2), snavel)
    for zij, s in (("l", -1), ("r", 1)):
        x0 = -29 if s < 0 else 9
        g.bone("vleugel_" + zij, "body", (9 * s, 30, -4))
        g.cube("vleugel_" + zij, (x0, 28, -8), (20, 2, 16), veer)
        x1 = -45 if s < 0 else 29
        g.bone("vleugel2_" + zij, "vleugel_" + zij, (29 * s, 29, -4))
        g.cube("vleugel2_" + zij, (x1, 28.2, -7), (16, 1.6, 14), veer)
        g.cube("vleugel2_" + zij, (x1 + (0 if s < 0 else 10), 28.4, 7), (6, 1.2, 4), veer)   # a ragged tip
        g.bone("poot_" + zij, "body", (4 * s, 14, -1))
        px = -6 if s < 0 else 2
        g.cube("poot_" + zij, (px, 3, -3), (4, 11, 4), klauw)
        g.cube("poot_" + zij, (px - 1, 0, -6), (6, 3, 7), klauw)
    g.bone("staart", "body", (0, 18, 11))
    g.cube("staart", (-6, 15, 10), (12, 3, 14), veer, rotation=(-20, 0, 0), pivot=(0, 18, 11))
    vouw_l, vouw_r = [0, 25, 75], [0, -25, -75]
    vouw2_l, vouw2_r = [0, 30, 60], [0, -30, -60]
    anims = {"animations": {
        "idle": kf({"vleugel_l": {"rotation": {0: vouw_l, 2.0: vouw_l}}, "vleugel_r": {"rotation": {0: vouw_r, 2.0: vouw_r}},
                    "vleugel2_l": {"rotation": {0: vouw2_l, 2.0: vouw2_l}}, "vleugel2_r": {"rotation": {0: vouw2_r, 2.0: vouw2_r}},
                    "body": {"scale": {0: [1, 1, 1], 1.0: [1.03, 1.02, 1.0], 2.0: [1, 1, 1]}},
                    "head": {"rotation": {0: [0, 0, 0], 0.8: [0, 25, 0], 1.0: [0, 25, 0], 1.6: [0, -20, 0], 2.0: [0, 0, 0]}},
                    "staart": {"rotation": {0: [0, 0, 0], 1.0: [5, 0, 0], 2.0: [0, 0, 0]}}}),
        "walk": kf({"vleugel_l": {"rotation": {0: vouw_l, 0.8: vouw_l}}, "vleugel_r": {"rotation": {0: vouw_r, 0.8: vouw_r}},
                    "vleugel2_l": {"rotation": {0: vouw2_l, 0.8: vouw2_l}}, "vleugel2_r": {"rotation": {0: vouw2_r, 0.8: vouw2_r}},
                    "poot_l": {"rotation": {0: [-30, 0, 0], 0.4: [30, 0, 0], 0.8: [-30, 0, 0]}},
                    "poot_r": {"rotation": {0: [30, 0, 0], 0.4: [-30, 0, 0], 0.8: [30, 0, 0]}},
                    "body": {"rotation": {0: [0, 0, -4], 0.4: [0, 0, 4], 0.8: [0, 0, -4]}},
                    "nek": {"rotation": {0: [8, 0, 0], 0.2: [-4, 0, 0], 0.4: [8, 0, 0], 0.6: [-4, 0, 0], 0.8: [8, 0, 0]}}}),
        "vlieg": kf({"vleugel_l": {"rotation": {0: [0, 0, -35], 0.4: [0, 0, 35], 0.8: [0, 0, -35]}},
                     "vleugel_r": {"rotation": {0: [0, 0, 35], 0.4: [0, 0, -35], 0.8: [0, 0, 35]}},
                     "vleugel2_l": {"rotation": {0: [0, 0, 10], 0.2: [0, 0, -20], 0.5: [0, 0, 25], 0.8: [0, 0, 10]}},
                     "vleugel2_r": {"rotation": {0: [0, 0, -10], 0.2: [0, 0, 20], 0.5: [0, 0, -25], 0.8: [0, 0, -10]}},
                     "poot_l": {"rotation": {0: [50, 0, 0], 0.8: [50, 0, 0]}}, "poot_r": {"rotation": {0: [50, 0, 0], 0.8: [50, 0, 0]}},
                     "body": {"rotation": {0: [15, 0, 0], 0.8: [15, 0, 0]}, "position": {0: [0, 0, 0], 0.4: [0, 2, 0], 0.8: [0, 0, 0]}}}),
        "duik": kf({"body": {"rotation": {0: [0, 0, 0], 0.4: [-15, 0, 0], 0.7: [45, 0, 0], 1.2: [0, 0, 0]}},
                    "vleugel_l": {"rotation": {0: [0, 0, -30], 0.4: [0, 0, -40], 0.7: [0, 60, 20], 1.2: vouw_l}},
                    "vleugel_r": {"rotation": {0: [0, 0, 30], 0.4: [0, 0, 40], 0.7: [0, -60, -20], 1.2: vouw_r}},
                    "poot_l": {"rotation": {0.5: [0, 0, 0], 0.8: [-70, 0, 0], 1.2: [0, 0, 0]}},
                    "poot_r": {"rotation": {0.5: [0, 0, 0], 0.8: [-70, 0, 0], 1.2: [0, 0, 0]}}}, loop=False),
        "wind": kf({"vleugel_l": {"rotation": {0: vouw_l, 0.4: [0, 0, -45], 0.7: [0, -30, 30], 1.0: [0, 0, -40], 1.3: [0, -30, 30], 1.8: vouw_l}},
                    "vleugel_r": {"rotation": {0: vouw_r, 0.4: [0, 0, 45], 0.7: [0, 30, -30], 1.0: [0, 0, 40], 1.3: [0, 30, -30], 1.8: vouw_r}},
                    "body": {"rotation": {0: [0, 0, 0], 0.4: [-15, 0, 0], 1.3: [-10, 0, 0], 1.8: [0, 0, 0]}},
                    "head": {"rotation": {0: [0, 0, 0], 0.4: [-25, 0, 0], 1.3: [-25, 0, 0], 1.8: [0, 0, 0]}}}, loop=False),
        "grijp": kf({"body": {"rotation": {0: [0, 0, 0], 0.3: [-25, 0, 0], 0.6: [10, 0, 0], 1.0: [0, 0, 0]}},
                     "poot_l": {"rotation": {0: [0, 0, 0], 0.3: [-80, 0, 0], 0.6: [-60, 0, 0], 1.0: [0, 0, 0]}},
                     "poot_r": {"rotation": {0: [0, 0, 0], 0.3: [-80, 0, 0], 0.6: [-60, 0, 0], 1.0: [0, 0, 0]}},
                     "vleugel_l": {"rotation": {0: vouw_l, 0.3: [0, 0, -30], 1.0: vouw_l}},
                     "vleugel_r": {"rotation": {0: vouw_r, 0.3: [0, 0, 30], 1.0: vouw_r}}}, loop=False),
        "hop": kf({"root": {"position": {0: [0, 0, 0], 0.2: [0, 4, 0], 0.4: [0, 0, 0]}},
                   "vleugel_l": {"rotation": {0: vouw_l, 0.2: [0, 0, -20], 0.4: vouw_l}},
                   "vleugel_r": {"rotation": {0: vouw_r, 0.2: [0, 0, 20], 0.4: vouw_r}}}),
        "sprong": kf({"root": {"position": {0: [0, 0, 0], 0.4: [0, 14, 0], 0.8: [0, 0, 0]}},
                      "vleugel_l": {"rotation": {0: vouw_l, 0.2: [0, 0, -35], 0.4: [0, 0, 30], 0.6: [0, 0, -35], 0.8: vouw_l}},
                      "vleugel_r": {"rotation": {0: vouw_r, 0.2: [0, 0, 35], 0.4: [0, 0, -30], 0.6: [0, 0, 35], 0.8: vouw_r}}}, loop=False),
    }}
    return g, anims


# =====================================================================================================================
# 2. The Mika bosses (mika.geo.json + accessories)
# =====================================================================================================================
FACE = (11, 49, 25, 60)   # texel box of the face in mika.png


def _mika_bron(a_dir):
    return np.asarray(Image.open(os.path.join(a_dir, "textures", "entity", "mika.png")).convert("RGBA"), np.float32).copy()


def _tint(a, kleur, mix, gezicht=True):
    k = a.shape[1] // 128
    fx0, fy0, fx1, fy1 = [v * k for v in FACE]
    face = a[fy0:fy1, fx0:fx1].copy()
    l = a[..., :3].mean(axis=2, keepdims=True)
    a[..., :3] = a[..., :3] * (1 - mix) + (l / 200.0) * np.array(kleur) * mix
    if gezicht:
        a[fy0:fy1, fx0:fx1] = face
    return a


def _gezicht_vak(img, k=4):
    fx0, fy0, fx1, fy1 = [v * k for v in FACE]
    return img[fy0:fy1, fx0:fx1]


def mika_geo(naam, a_dir, tint, gezicht_extra=None):
    """A Geo with the mika bones (uv unchanged) and the tinted mika texture in the top-left 128x128 texels."""
    geo = json.load(open(os.path.join(a_dir, "geckolib", "models", "entity", "mika.geo.json"), encoding="utf-8"))
    g = Geo(naam, 256, 256, 4)
    for b in copy.deepcopy(geo["minecraft:geometry"][0]["bones"]):
        g.bones.append(b)
        g.idx[b["name"]] = b
    a = tint(_mika_bron(a_dir))
    if gezicht_extra:
        vak = _gezicht_vak(a)
        gezicht_extra(vak, vak.shape[1], vak.shape[0])
    g.img[:512, :512] = np.clip(a, 0, 255).astype(np.uint8)
    g.x, g.y, g.rij = 0, 128, 0
    return g


def mika_anims(extra=None):
    an = {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 1.0: [1.02, 1.04, 1.0], 2.0: [1, 1, 1]}},
                    "tail": {"rotation": {0: [0, -12, 0], 1.0: [0, 12, 0], 2.0: [0, -12, 0]}},
                    "ear_left": {"rotation": {0: [0, 0, 0], 1.6: [0, 0, 0], 1.7: [0, 0, 15], 1.8: [0, 0, 0], 2.0: [0, 0, 0]}}}),
        "walk": kf({"leg_front_left": {"rotation": {0: [-25, 0, 0], 0.25: [25, 0, 0], 0.5: [-25, 0, 0]}},
                    "leg_back_right": {"rotation": {0: [-25, 0, 0], 0.25: [25, 0, 0], 0.5: [-25, 0, 0]}},
                    "leg_front_right": {"rotation": {0: [25, 0, 0], 0.25: [-25, 0, 0], 0.5: [25, 0, 0]}},
                    "leg_back_left": {"rotation": {0: [25, 0, 0], 0.25: [-25, 0, 0], 0.5: [25, 0, 0]}},
                    "body": {"position": {0: [0, 0, 0], 0.125: [0, 0.6, 0], 0.25: [0, 0, 0], 0.375: [0, 0.6, 0], 0.5: [0, 0, 0]}},
                    "tail": {"rotation": {0: [10, 0, 0], 0.25: [-5, 0, 0], 0.5: [10, 0, 0]}}}),
        "slag": kf({"leg_front_right": {"rotation": {0: [0, 0, 0], 0.15: [-90, 20, 0], 0.35: [-10, -20, 0], 0.6: [0, 0, 0]}},
                    "head": {"rotation": {0: [0, 0, 0], 0.15: [0, -15, 0], 0.35: [0, 15, 0], 0.6: [0, 0, 0]}}}, loop=False),
        "worp": kf({"body": {"rotation": {0: [0, 0, 0], 0.25: [-15, 0, 0], 0.45: [15, 0, 0], 0.8: [0, 0, 0]}},
                    "leg_front_left": {"rotation": {0: [0, 0, 0], 0.25: [-110, 0, 0], 0.45: [-20, 0, 0], 0.8: [0, 0, 0]}},
                    "head": {"rotation": {0: [0, 0, 0], 0.25: [-10, 0, 0], 0.45: [8, 0, 0], 0.8: [0, 0, 0]}}}, loop=False),
        "draai": kf({"root": {"rotation": {0: [0, 0, 0], 0.4: [0, 180, 0], 0.8: [0, 360, 0]}}}, loop=False),
        "sprong": kf({"root": {"position": {0: [0, 0, 0], 0.2: [0, -1.5, 0], 0.5: [0, 9, 0], 0.9: [0, 0, 0]}},
                      "leg_front_left": {"rotation": {0.2: [0, 0, 0], 0.5: [-40, 0, 0], 0.9: [0, 0, 0]}},
                      "leg_front_right": {"rotation": {0.2: [0, 0, 0], 0.5: [-40, 0, 0], 0.9: [0, 0, 0]}}}, loop=False),
    }
    an.update(extra or {})
    return {"animations": an}


def _oogglas(vak, W, H, x, kleur=(230, 200, 90)):
    for t in np.linspace(0, 6.28, 48):
        disc(vak, x + math.cos(t) * W * 0.14, H * 0.4 + math.sin(t) * W * 0.14, 1.0, kleur)


def taleggio(a_dir):
    def tint(a):
        return _tint(a, (255, 214, 150), 0.55)

    def gez(v, W, H):
        v[:int(H * 0.18), :, :3] = (214, 60, 50)   # bandana rim
        for x in range(0, W, 6):
            disc(v, x + 2, H * 0.08, 1.2, (255, 240, 230))
    g = mika_geo("taleggio", a_dir, tint, gez)
    bandana = vlak((214, 60, 50), 8, 311, extra=lambda a, W, H, f, r: [disc(a, r.uniform(0, W), r.uniform(0, H), 1.2, (255, 240, 230)) for _ in range(W * H // 40)])
    touw = vlak((196, 160, 104), 8, 312, extra=lambda a, W, H, f, r: [lijn(a, 0, y, W, y + 3, (150, 116, 70), dik=1.2) for y in range(-4, H, 4)])
    mes = vlak((236, 236, 240), 5, 313, verloop=(190, 190, 200))
    heft = vlak((110, 76, 44), 6, 314)
    g.cube("head", (-7.5, 12.5, -12.3), (15, 2.5, 11.5), bandana)
    g.cube("head", (6.5, 10, -1.5), (3, 4, 1.5), bandana, rotation=(0, 0, -30))   # the knot
    g.cube("body", (-4, 10, 2), (8, 3.5, 8), touw)   # rope coil
    g.cube("body", (-3, 13.5, 3), (6, 1.5, 6), touw)
    g.bone("touw", "body", (0, 13, 6))
    g.cube("touw", (-0.75, 13, 5.25), (1.5, 40, 1.5), touw)
    g.bone("mes", "leg_front_right", (-4, 2, -12))
    g.cube("mes", (-5, 1, -18), (1, 3, 5), heft)
    g.cube("mes", (-5.2, 0, -26), (1.4, 5, 8), mes)
    return g, mika_anims({
        "hang": kf({"root": {"rotation": {0: [0, 0, -14], 1.0: [0, 0, 14], 2.0: [0, 0, -14]}},
                    "leg_back_left": {"rotation": {0: [30, 0, 0], 2.0: [30, 0, 0]}}, "leg_back_right": {"rotation": {0: [30, 0, 0], 2.0: [30, 0, 0]}},
                    "tail": {"rotation": {0: [40, 10, 0], 1.0: [40, -10, 0], 2.0: [40, 10, 0]}}})})


def brokkel(a_dir):
    def tint(a):
        return _tint(a, (120, 110, 104), 0.6)

    def gez(v, W, H):
        v[:int(H * 0.2), :, :3] = (70, 74, 60)   # helmet rim
        for x in (W * 0.3, W * 0.7):   # goggles
            for t in np.linspace(0, 6.28, 40):
                disc(v, x + math.cos(t) * W * 0.15, H * 0.4 + math.sin(t) * W * 0.15, 1.3, (60, 50, 40))
        for _ in range(16):
            disc(v, _rng(330 + _).uniform(0, W), _rng(360 + _).uniform(H * 0.6, H), 1.0, (40, 36, 34), alpha=0.6)
    g = mika_geo("brokkel", a_dir, tint, gez)
    helm = vlak((90, 96, 76), 8, 321, extra=schimmel)
    bril = vlak((150, 210, 230), 6, 322)
    plaat = vlak((120, 120, 132), 8, 323, extra=lambda a, W, H, f, r: [disc(a, x, y, 1.0, (200, 200, 210)) for x in (2, W - 3) for y in (2, H - 3)])
    bom = vlak(KAAS, 8, 324, extra=lambda a, W, H, f, r: disc(a, W / 2, H / 2, W * 0.2, (230, 150, 50)))
    g.cube("head", (-7.5, 12.5, -12), (15, 3, 11), helm)
    g.cube("head", (-1, 15.5, -7), (2, 3, 2), helm)
    g.cube("head", (-6.5, 7.5, -12.6), (13, 3, 0.8), bril)
    g.bone("platen", "body", (0, 5, 5))
    for i, (o, s) in enumerate((((-7.6, 2, 0), (1, 6, 9)), ((6.6, 2, 0), (1, 6, 9)), ((-5, 2, 10.6), (10, 6, 1)), ((-5, 10, 1), (10, 1, 8))), 1):
        g.bone(f"plaat_{i}", "platen", (o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2))
        g.cube(f"plaat_{i}", o, s, plaat)
    for i in range(5):   # the bomb belt
        g.cube("body", (-6 + i * 2.6, 0.2, -2.8), (2, 2, 1), bom)
    return g, mika_anims()


def roquefort(a_dir):
    def tint(a):
        a = _tint(a, (200, 214, 236), 0.5)
        r = _rng(331)
        for _ in range(140):   # blue cheese veins
            x, y = r.uniform(0, a.shape[1]), r.uniform(0, a.shape[0])
            lijn(a, x, y, x + r.uniform(-8, 8), y + r.uniform(-8, 8), (70, 100, 160), dik=1.5)
        return a

    def gez(v, W, H):   # eye patch over the left eye + its band
        disc(v, W * 0.3, H * 0.42, W * 0.15, (24, 20, 28))
        lijn(v, 0, H * 0.2, W, H * 0.55, (24, 20, 28), dik=2)
    g = mika_geo("roquefort", a_dir, tint, gez)
    hoed = vlak((40, 36, 52), 6, 341, extra=lambda a, W, H, f, r: lijn(a, 0, H - 1.5, W, H - 1.5, HONING, dik=1.2))
    jas = vlak((150, 40, 56), 8, 342, extra=lambda a, W, H, f, r: [disc(a, W * 0.5, y, 1.1, HONING) for y in np.arange(3, H, 6)])
    sabel = vlak((226, 230, 240), 4, 343, verloop=(170, 176, 196))
    gevest = vlak(HONING, 6, 344)
    paraplu = vlak((30, 28, 34), 5, 345, extra=lambda a, W, H, f, r: [lijn(a, x, 0, x, H, (60, 56, 66), dik=0.8) for x in np.arange(2, W, 6)])
    stok = vlak((110, 76, 44), 5, 346)
    g.cube("head", (-9, 13, -12.5), (18, 1.5, 13), hoed)
    g.cube("head", (-6, 14.5, -10), (12, 4, 9), hoed)
    g.cube("head", (-1, 18.5, -12.8), (2, 2, 2), vlak((240, 240, 240), 4, 347))   # skull-and-cheese badge
    g.cube("body", (-7, 0.5, -2.5), (14, 9.5, 14), jas, inflate=0.3)
    g.cube("body", (-6, 0, 11.5), (12, 6, 6), jas)   # coat tails
    g.bone("sabel", "leg_front_right", (-4, 2, -12))
    g.cube("sabel", (-5, 1.5, -15), (1.6, 2.5, 2.5), gevest)
    g.cube("sabel", (-4.8, 2.2, -29), (1, 1.2, 14), sabel)
    g.bone("paraplu", "head", (0, 18, -6))
    g.cube("paraplu", (-0.5, 18, -6.5), (1, 14, 1), stok)
    g.cube("paraplu", (-10, 32, -16), (20, 2, 20), paraplu)
    g.cube("paraplu", (-7, 34, -13), (14, 2, 14), paraplu)
    return g, mika_anims({
        "sabel": kf({"leg_front_right": {"rotation": {0: [0, 0, 0], 0.15: [-80, 30, 0], 0.3: [-20, -30, 0], 0.45: [-80, -30, 0], 0.6: [-20, 30, 0], 0.8: [0, 0, 0]}},
                     "body": {"rotation": {0: [0, 0, 0], 0.15: [0, 15, 0], 0.45: [0, -15, 0], 0.8: [0, 0, 0]}}}, loop=False)})


PELS = (120, 110, 132)
# the pieppiepmuisje look (feature "piep", textures/entity/pieppiepmuisje.png): plum-grey fur, a cream belly, pink ears/nose/tail
MUIS_VACHTEN = [(150, 142, 162), (98, 86, 112), (176, 170, 184), (120, 108, 134), (196, 190, 200)]
MUIS_BUIK = (232, 222, 204)
MUIS_ROZE = (240, 146, 168)
MUIS_ROZE_DONKER = (206, 104, 132)
STEEK = (246, 236, 214)


def _muisvel(a, cx, cy, w, h, vacht, r):
    """One shed winter coat of a pieppiepmuisje, laid flat: head up (two round ears, pink inside, a pink nose), an oval body
    with a cream belly patch, four little paw flaps, and the pink tail curling out at the bottom."""
    sx, sy = w / 2, h / 2
    # tail first (under the pelt): a curl from the bottom
    t0x, t0y = cx + r.uniform(-1, 1), cy + sy * 0.85
    kr = r.choice([-1, 1])
    px, py = t0x, t0y
    for i in range(1, 9):
        nx, ny = t0x + kr * math.sin(i * 0.45) * w * 0.32, t0y + i * h * 0.05
        lijn(a, px, py, nx, ny, MUIS_ROZE_DONKER if i > 6 else MUIS_ROZE, dik=max(1.0, w * 0.06))
        px, py = nx, ny
    # paws
    for dx, dy in ((-0.62, -0.25), (0.62, -0.25), (-0.58, 0.45), (0.58, 0.45)):
        disc(a, cx + dx * sx, cy + dy * sy, sx * 0.2, vacht, ry=sy * 0.12)
        disc(a, cx + dx * sx * 1.12, cy + dy * sy, sx * 0.09, MUIS_ROZE)
    # body + belly
    disc(a, cx, cy + sy * 0.15, sx * 0.62, vacht, ry=sy * 0.62)
    disc(a, cx, cy + sy * 0.22, sx * 0.34, MUIS_BUIK, ry=sy * 0.38, alpha=0.9)
    # head + ears (big round mouse ears, pink inside)
    hy = cy - sy * 0.55
    for s in (-1, 1):
        disc(a, cx + s * sx * 0.42, hy - sy * 0.22, sx * 0.3, vacht)
        disc(a, cx + s * sx * 0.42, hy - sy * 0.2, sx * 0.18, MUIS_ROZE)
    disc(a, cx, hy, sx * 0.4, vacht, ry=sy * 0.28)
    disc(a, cx, hy + sy * 0.2, sx * 0.1, MUIS_ROZE)   # the little nose
    # fluffy fur strokes
    for _ in range(int(w * h / 40)):
        x, y = cx + r.uniform(-sx * 0.55, sx * 0.55), cy + r.uniform(-sy * 0.3, sy * 0.7)
        c = tuple(min(255, v + 34) for v in vacht)
        lijn(a, x, y, x + r.uniform(-0.8, 0.8), y + 1.6, c, dik=0.6)


def _pels(sokken):
    """The cape: patchwork of shed winter coats of pieppiepmuisjes (plum-grey fur, cream bellies, pink ears/noses/tails),
    sewn together with big cream stitches, or (sokken) knitted socks."""
    if sokken:
        def p(W, H, f):
            a = np.zeros((H, W, 4), np.float32)
            kleuren = [(240, 90, 120), (250, 210, 80), (120, 190, 240), (140, 220, 140), (250, 250, 250)]
            for y in range(H):
                a[y, :, :3] = kleuren[(y // 6) % len(kleuren)]
            a[..., 3] = 255
            for y in range(0, H, 3):
                for x in range(0, W, 4):
                    disc(a, x + 1.5, y + 1.5, 0.8, (255, 255, 255), alpha=0.25)
            return np.clip(a, 0, 255).astype(np.uint8)
        return p

    def p(W, H, f):
        r = _rng(3510 + W * 7 + H * 13 + sum(map(ord, f)) % 97)
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = (84, 74, 96)
        a[..., :3] += r.normal(0, 5, (H, W, 1))
        a[..., 3] = 255
        # one pelt per patch (~22 x 26 px = 5.5 x 6.5 texels): even a thin face shows at least one whole mouse
        pw, ph = 22.0, 26.0
        nx, ny = max(1, int(round(W / pw))), max(1, int(round(H / ph)))
        cw, ch = W / nx, H / ny
        for j in range(ny):
            for i in range(nx):
                vacht = MUIS_VACHTEN[(i + j * 2 + int(r.integers(2)) * 2) % len(MUIS_VACHTEN)]   # neighbours differ
                x0, y0 = i * cw, j * ch
                a[int(y0):int(y0 + ch) + 1, int(x0):int(x0 + cw) + 1, :3] = np.array(vacht) * 0.55   # the patch ground (darker, so each pelt stands out)
                if cw >= 8 and ch >= 8:
                    _muisvel(a, x0 + cw / 2 + r.uniform(-1, 1), y0 + ch / 2, min(cw * 0.95, ch * 0.8), ch * 0.9, vacht, r)
        # the seams: cream running stitches along every patch edge
        for i in range(1, nx):
            x = i * cw
            for y in np.arange(1, H, 4):
                lijn(a, x - 1.2, y, x + 1.2, y + 1.6, STEEK, dik=0.8)
        for j in range(1, ny):
            y = j * ch
            for x in np.arange(1, W, 4):
                lijn(a, x, y - 1.2, x + 1.6, y + 1.2, STEEK, dik=0.8)
        return np.clip(a, 0, 255).astype(np.uint8)
    return p


def _muisjes_aan_cape(g, bone, punten, oor_naar, staart=True):
    """3D bits so the cape reads as mouse pelts from every angle: little round ears sticking out (pink inside) and limp pink
    tails hanging down. punten = (x, y, z) attach points in model space; oor_naar = the outward face direction (x, z)."""
    vacht = vlak(MUIS_VACHTEN[0], 6, 361)
    roze = vlak(MUIS_ROZE, 6, 362)
    for (x, y, z) in punten:
        ox, oz = oor_naar
        # two ears: thin discs (1 x 1.5 x 1.5) standing out of the cape surface
        for s in (-0.9, 0.9):
            ex, ez = (x, z + s) if ox else (x + s, z)
            g.cube(bone, (ex - 0.5 + ox * 0.6, y, ez - 0.5 + oz * 0.6), (1 if ox else 1.4, 1.4, 1.4 if ox else 1), vacht,
                   faces={"east" if ox > 0 else "west" if ox < 0 else "south" if oz > 0 else "north": roze})
        if staart:
            g.cube(bone, (x - 0.25 + ox * 0.8, y - 7, z - 0.25 + oz * 0.8), (0.5, 7, 0.5), roze)


def piepvel(a_dir, sokken=False):
    def tint(a):
        return _tint(a, (176, 164, 200), 0.55)

    def gez(v, W, H):
        _oogglas(v, W, H, W * 0.7)   # the monocle
        lijn(v, W * 0.84, H * 0.45, W * 0.95, H * 0.95, (230, 200, 90), dik=1)
        for s in (-1, 1):   # a curly, vain moustache
            lijn(v, W * 0.5, H * 0.66, W * 0.5 + s * W * 0.22, H * 0.64, (70, 60, 80), 2)
            lijn(v, W * 0.5 + s * W * 0.22, H * 0.64, W * 0.5 + s * W * 0.26, H * 0.56, (70, 60, 80), 1.5)
    g = mika_geo("piepvel_sokken" if sokken else "piepvel", a_dir, tint, gez)
    kroon = vlak(HONING, 6, 352, extra=lambda a, W, H, f, r: [disc(a, x, H * 0.5, 1.0, AMBERROZE) for x in np.arange(2, W, 4)])
    hemd = vlak((250, 250, 246), 5, 353, extra=lambda a, W, H, f, r: [disc(a, W * 0.5, y, 0.8, (200, 200, 210)) for y in np.arange(2, H, 4)])
    pels = _pels(sokken)
    g.cube("head", (-3, 14, -8), (6, 2, 6), kroon)
    for x in (-3, -0.5, 2):
        g.cube("head", (x, 16, -8), (1, 2, 1), kroon)
    g.bone("hemdje", "body", (0, 5, 5))
    g.cube("hemdje", (-6, 1, -2.3), (12, 8.5, 12), hemd, inflate=0.4)
    g.bone("cape", "body", (0, 11, -1))
    g.bone("cape_1", "cape", (-8, 11, 4))
    g.cube("cape_1", (-9, 0, -2), (1.5, 12, 14), pels)
    g.bone("cape_2", "cape", (8, 11, 4))
    g.cube("cape_2", (7.5, 0, -2), (1.5, 12, 14), pels)
    g.bone("cape_3", "cape", (0, 11, 12))
    g.cube("cape_3", (-8.5, -0.5, 11.5), (17, 12.5, 1.5), pels)
    g.cube("cape_3", (-7, -0.5, 13), (14, 3, 3), pels)   # the train on the floor
    g.bone("cape_4", "cape", (0, 11, 4))
    g.cube("cape_4", (-8.5, 10.5, -2), (17, 2, 13.5), pels)
    g.bone("kraag", "cape", (0, 11, -2))
    g.cube("kraag", (-9.5, 9, -4), (19, 6, 3), pels)
    if not sokken:
        # (M-BAZEN) mouse ears along the outside of every panel + limp pink tails at the hem: the panels go with their bones
        _muisjes_aan_cape(g, "cape_1", [(-9, 9, 1), (-9, 9, 7), (-9, 4, 4), (-9, 4, 10)], (-1, 0), staart=False)
        _muisjes_aan_cape(g, "cape_2", [(9, 9, 1), (9, 9, 7), (9, 4, 4), (9, 4, 10)], (1, 0), staart=False)
        _muisjes_aan_cape(g, "cape_3", [(-5, 9, 13), (0, 9, 13), (5, 9, 13), (-3, 4, 13), (3, 4, 13)], (0, 1), staart=False)
        for x in (-6, -2, 2, 6):   # limp pink tails trailing behind the train on the floor
            g.cube("cape_3", (x - 0.25, 0, 16), (0.5, 0.5, 4), vlak(MUIS_ROZE, 6, 363))
        for z in (1, 6, 11):   # and sticking out at the hem of the side panels
            g.cube("cape_1", (-12, 0.2, z), (3, 0.5, 0.5), vlak(MUIS_ROZE, 6, 364))
            g.cube("cape_2", (9, 0.2, z), (3, 0.5, 0.5), vlak(MUIS_ROZE, 6, 365))
        _muisjes_aan_cape(g, "kraag", [(-6, 15, -2.5), (0, 15, -2.5), (6, 15, -2.5)], (0, 0), staart=False)
    return g, mika_anims({
        "capezwier": kf({"root": {"rotation": {0: [0, 0, 0], 0.5: [0, 180, 0], 1.0: [0, 360, 0]}},
                         "cape_1": {"rotation": {0: [0, 0, -50], 1.0: [0, 0, -50]}}, "cape_2": {"rotation": {0: [0, 0, 50], 1.0: [0, 0, 50]}},
                         "cape_3": {"rotation": {0: [-60, 0, 0], 1.0: [-60, 0, 0]}}}),
        "buig": kf({"head": {"rotation": {0: [0, 0, 0], 0.5: [28, 0, 0], 1.5: [28, 0, 0], 2.0: [0, 0, 0]}},
                    "body": {"rotation": {0: [0, 0, 0], 0.5: [10, 0, 0], 1.5: [10, 0, 0], 2.0: [0, 0, 0]}},
                    "leg_front_right": {"rotation": {0: [0, 0, 0], 0.5: [-60, 0, 30], 1.5: [-60, 0, 30], 2.0: [0, 0, 0]}}}, loop=False)})


def bleu(a_dir):
    def tint(a):
        a = _tint(a, (150, 172, 210), 0.65)
        r = _rng(361)
        for _ in range(110):
            x, y = r.uniform(0, a.shape[1]), r.uniform(0, a.shape[0])
            lijn(a, x, y, x + r.uniform(-6, 6), y + r.uniform(-6, 6), (60, 84, 140), dik=1.3)
        return a

    def gez(v, W, H):
        _oogglas(v, W, H, W * 0.3, kleur=(200, 210, 230))
    g = mika_geo("bleu", a_dir, tint, gez)
    hoed = vlak((44, 52, 80), 6, 362, extra=lambda a, W, H, f, r: lijn(a, 0, H - 2, W, H - 2, (130, 160, 220), dik=1.5))
    cape = vlak((96, 120, 170), 8, 363, extra=lambda a, W, H, f, r: [lijn(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0, W), r.uniform(0, H), (60, 80, 130), 1)
                                                                     for _ in range(W * H // 40)], verloop=(70, 90, 140))
    degen = vlak((216, 222, 236), 4, 364)
    gevest = vlak((180, 190, 220), 5, 365)
    g.cube("head", (-7, 13, -11), (14, 1.2, 11), hoed)
    g.cube("head", (-4.5, 14.2, -9), (9, 9, 7), hoed)
    g.cube("body", (-8, 0, 10.6), (16, 11, 1.5), cape)
    g.cube("body", (-8, 10, -2), (16, 1.5, 13), cape)
    g.bone("degen", "leg_front_right", (-4, 2, -12))
    g.cube("degen", (-5, 1.5, -14.5), (1.6, 2.5, 2), gevest)
    g.cube("degen", (-4.6, 2.3, -30), (0.6, 0.6, 16), degen)
    return g, mika_anims()


def matroos_textuur(a_dir):
    a = _mika_bron(a_dir)
    k = a.shape[1] // 128
    fx0, fy0, fx1, fy1 = [v * k for v in FACE]
    face = a[fy0:fy1, fx0:fx1].copy()
    for y in range(a.shape[0]):   # a striped sailor shirt
        if (y // (3 * k)) % 2 == 0:
            a[y, :, :3] = a[y, :, :3] * 0.4 + np.array([250, 250, 250]) * 0.6
        else:
            a[y, :, :3] = a[y, :, :3] * 0.4 + np.array([60, 90, 170]) * 0.6
    a[fy0:fy1, fx0:fx1] = face
    vak = a[fy0:fy1, fx0:fx1]
    W, H = vak.shape[1], vak.shape[0]
    vak[:max(1, int(H * 0.14)), :, :3] = (250, 250, 250)   # sailor cap
    vak[int(H * 0.12):int(H * 0.18), :, :3] = (40, 60, 120)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# =====================================================================================================================
# 3. De Wortelwurger
# =====================================================================================================================
def wortelwurger(glow=False):
    g = Geo("wortelwurger", 256, 256, 2)

    def w(p):
        return leeg if glow else p
    wortel = w(vlak((116, 112, 116), 10, 371, extra=lambda a, W, H, f, r: (nerven(a, W, H, f, r), schimmel(a, W, H, f, r)), verloop=(84, 84, 96)))

    def kern(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not glow:
            a[..., :3] = (92, 70, 100)
            a[..., :3] += _rng(372).normal(0, 6, (H, W, 1))
            a[..., 3] = 255
        r = _rng(373 + W)
        for _ in range(W * H // 30):   # sickly glowing veins
            x, y = r.uniform(0, W), r.uniform(0, H)
            lijn(a, x, y, x + r.uniform(-4, 4), y + r.uniform(-4, 4), (220, 120, 210), dik=1.0)
        if f == "north":
            for x in (W * 0.32, W * 0.68):
                disc(a, x, H * 0.4, W * 0.08, (255, 170, 230))
        return np.clip(a, 0, 255).astype(np.uint8)
    g.bone("root")
    g.bone("kern", "root", (0, 16, 0))
    g.cube("kern", (-8, 8, -8), (16, 16, 16), kern)
    g.cube("kern", (-9, 6, -9), (18, 4, 18), wortel)
    hoog = (40, 18, 32, 16, 36, 20)
    for i in range(6):
        hoek = i * 60 + 15
        n = f"wortel_{i + 1}"
        g.bone(n, "kern", (0, 12, 0))
        g.cube(n, (-2.5, 9, -24), (5, 5, 17), wortel, rotation=(0, hoek, 0), pivot=(0, 12, 0))
        g.cube(n, (-2, 9, -28), (4, hoog[i] - 9, 4), wortel, rotation=(0, hoek, 0), pivot=(0, 12, 0))
        g.cube(n, (-1.5, hoog[i] - 2, -31), (3, 3, 5), wortel, rotation=(0, hoek, 0), pivot=(0, 12, 0))
    anims = {"animations": {
        "idle": kf({**{f"wortel_{i + 1}": {"rotation": {0: [0, 0, 0], 1.0 + i * 0.1: [6, 0, 4], 2.4: [0, 0, 0]}} for i in range(6)},
                    "kern": {"scale": {0: [1, 1, 1], 1.2: [1.04, 1.04, 1.04], 2.4: [1, 1, 1]}}}),
        "walk": kf({**{f"wortel_{i + 1}": {"rotation": {0: [10 * (-1) ** i, 0, 0], 0.4: [-10 * (-1) ** i, 0, 0], 0.8: [10 * (-1) ** i, 0, 0]}} for i in range(6)},
                    "kern": {"position": {0: [0, 0, 0], 0.4: [0, 1, 0], 0.8: [0, 0, 0]}}}),
        "grijp": kf({**{f"wortel_{i + 1}": {"rotation": {0: [0, 0, 0], 0.3: [-25, 0, 0], 0.6: [35, 0, 0], 1.2: [0, 0, 0]}} for i in range(6)},
                     "kern": {"scale": {0: [1, 1, 1], 0.3: [0.95, 1.1, 0.95], 0.6: [1.1, 0.9, 1.1], 1.2: [1, 1, 1]}}}, loop=False),
        "klop": kf({"kern": {"scale": {0: [1, 1, 1], 0.1: [1.15, 1.15, 1.15], 0.25: [0.97, 0.97, 0.97], 0.5: [1, 1, 1]}}}, loop=False),
    }}
    return g, anims


# =====================================================================================================================
# 4. De Oude Nestwachter
# =====================================================================================================================
def nestwachter(roze=False, glow=False):
    g = Geo("nestwachter", 256, 256, 2)

    def w(p):
        return leeg if glow else p
    hout = w(vlak(HOUT if roze else GRIJS_HOUT, 10, 381, extra=nerven if roze else (lambda a, W, H, f, r: (nerven(a, W, H, f, r), schimmel(a, W, H, f, r))),
                  verloop=HOUT_DONKER if roze else (94, 92, 96)))
    kristal = gloei(AMBERROZE if roze else (190, 150, 110), 382, glow)
    gezicht = ogen(AMBERROZE if roze else (200, 170, 120), boos=not roze, glow=glow, basis=HOUT if roze else GRIJS_HOUT)
    g.bone("root")
    g.bone("body", "root", (0, 16, 0))
    g.cube("body", (-8, 16, -5), (16, 20, 10), hout)
    g.cube("body", (-6, 20, -5.6), (12, 10, 0.6), kristal)   # the heart crystal on the chest
    g.bone("head", "body", (0, 36, 0))
    g.cube("head", (-5, 36, -4.5), (10, 9, 9), hout, faces={"north": gezicht})
    g.cube("head", (-6, 44, -5), (12, 2, 10), hout)
    for zij, s in (("l", -1), ("r", 1)):
        g.bone("arm_" + zij, "body", (10 * s, 34, 0))
        g.cube("arm_" + zij, (8 if s > 0 else -12, 12, -3), (4, 23, 6), hout)
        g.cube("arm_" + zij, (7.5 if s > 0 else -12.5, 9, -3.5), (5, 4, 7), hout)
        g.bone("been_" + zij, "body", (4 * s, 16, 0))
        g.cube("been_" + zij, (1 if s > 0 else -7, 0, -3), (6, 16, 6), hout)
    g.bone("kristallen", "body", (0, 36, 0))
    for i, (x, h, rz) in enumerate(((-11, 9, -20), (-8.5, 6, -5), (8.5, 8, 18), (6, 6, 5), (-2, 5, 0))):
        g.cube("kristallen", (x, 36, -1.5), (3, h, 3), kristal, rotation=(0, 0, rz))
    anims = {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 1.5: [1.02, 1.02, 1.0], 3.0: [1, 1, 1]}},
                    "arm_l": {"rotation": {0: [0, 0, 0], 1.5: [3, 0, -3], 3.0: [0, 0, 0]}},
                    "arm_r": {"rotation": {0: [0, 0, 0], 1.5: [3, 0, 3], 3.0: [0, 0, 0]}},
                    "kristallen": {"scale": {0: [1, 1, 1], 1.5: [1.05, 1.08, 1.05], 3.0: [1, 1, 1]}}}),
        "walk": kf({"been_l": {"rotation": {0: [-25, 0, 0], 0.6: [25, 0, 0], 1.2: [-25, 0, 0]}},
                    "been_r": {"rotation": {0: [25, 0, 0], 0.6: [-25, 0, 0], 1.2: [25, 0, 0]}},
                    "arm_l": {"rotation": {0: [20, 0, 0], 0.6: [-20, 0, 0], 1.2: [20, 0, 0]}},
                    "arm_r": {"rotation": {0: [-20, 0, 0], 0.6: [20, 0, 0], 1.2: [-20, 0, 0]}},
                    "body": {"position": {0: [0, 0, 0], 0.3: [0, 1, 0], 0.6: [0, 0, 0], 0.9: [0, 1, 0], 1.2: [0, 0, 0]}}}),
        "stoot": kf({"body": {"rotation": {0: [0, 0, 0], 0.4: [0, 35, -10], 0.6: [15, -20, 5], 1.1: [0, 0, 0]},
                              "position": {0: [0, 0, 0], 0.4: [0, 0, 3], 0.6: [0, 0, -6], 1.1: [0, 0, 0]}}}, loop=False),
        "stamp": kf({"been_r": {"rotation": {0: [0, 0, 0], 0.4: [-50, 0, 0], 0.6: [5, 0, 0], 1.0: [0, 0, 0]}},
                     "arm_l": {"rotation": {0: [0, 0, 0], 0.4: [-60, 0, -20], 0.6: [10, 0, 0], 1.0: [0, 0, 0]}},
                     "arm_r": {"rotation": {0: [0, 0, 0], 0.4: [-60, 0, 20], 0.6: [10, 0, 0], 1.0: [0, 0, 0]}},
                     "body": {"position": {0: [0, 0, 0], 0.4: [0, 2, 0], 0.6: [0, -1, 0], 1.0: [0, 0, 0]}}}, loop=False),
        "buig": kf({"body": {"rotation": {0: [0, 0, 0], 0.6: [40, 0, 0], 1.6: [40, 0, 0], 2.2: [0, 0, 0]}},
                    "head": {"rotation": {0: [0, 0, 0], 0.6: [20, 0, 0], 1.6: [20, 0, 0], 2.2: [0, 0, 0]}},
                    "arm_r": {"rotation": {0: [0, 0, 0], 0.6: [-30, 0, 20], 1.6: [-30, 0, 20], 2.2: [0, 0, 0]}}}, loop=False),
    }}
    return g, anims


# =====================================================================================================================
# 5. De Schimmelkolos
# =====================================================================================================================
def schimmelkolos():
    g = Geo("schimmelkolos", 256, 256, 2)

    def klinknagels(a, W, H, f, r):
        for x in np.arange(2, W, 6):
            disc(a, x, 2, 0.9, (150, 150, 160))
            disc(a, x, H - 3, 0.9, (150, 150, 160))
        schimmel(a, W, H, f, r)
        for _ in range(W // 6):   # oozing mould drips
            x = r.uniform(0, W)
            lijn(a, x, 0, x, r.uniform(3, H * 0.5), (120, 150, 190), dik=1.6)
    ketel = vlak((96, 104, 120), 10, 391, extra=klinknagels, verloop=(64, 70, 86))
    ijzer = vlak(IJZER, 8, 392, extra=schimmel)
    pijp = vlak((130, 120, 96), 8, 393, extra=schimmel)
    g.bone("root")
    g.bone("ketel", "root", (0, 22, 0))
    g.cube("ketel", (-16, 20, -16), (32, 30, 32), ketel)
    g.cube("ketel", (-17, 30, -17), (34, 3, 34), ijzer)
    g.cube("ketel", (-6, 34, -17.5), (12, 8, 1.5), vlak((190, 220, 255), 6, 394))   # a porthole
    g.bone("deksel", "ketel", (0, 50, 0))
    g.cube("deksel", (-15, 50, -15), (30, 6, 30), ijzer)
    g.bone("pijp", "ketel", (14, 44, 0))
    g.cube("pijp", (14, 40, -2.5), (5, 5, 5), pijp)
    g.cube("pijp", (16, 44, -2), (4, 18, 4), pijp)
    g.cube("pijp", (14.5, 60, -3), (7, 3, 6), pijp)
    for i, (x, z) in enumerate(((-12, -12), (8, -12), (-12, 8), (8, 8)), 1):
        g.bone(f"poot_{i}", "root", (x + 2, 22, z + 2))
        g.cube(f"poot_{i}", (x, 2, z), (4, 20, 4), ijzer)
        g.cube(f"poot_{i}", (x - 1.5, 0, z - 1.5), (7, 2, 7), ijzer)
    g.bone("grijparm", "ketel", (0, 36, -16))
    g.cube("grijparm", (-2.5, 34, -36), (5, 5, 20), ijzer)
    for x in (-4, 2):
        g.cube("grijparm", (x, 30, -41), (2, 8, 5), ijzer)
    anims = {"animations": {
        "idle": kf({"deksel": {"position": {0: [0, 0, 0], 0.3: [0, 1, 0], 0.5: [0, 0, 0], 2.0: [0, 0, 0]}},
                    "ketel": {"scale": {0: [1, 1, 1], 1.0: [1.02, 0.99, 1.02], 2.0: [1, 1, 1]}},
                    "grijparm": {"rotation": {0: [0, 0, 0], 1.0: [5, 0, 0], 2.0: [0, 0, 0]}}}),
        "walk": kf({"poot_1": {"rotation": {0: [-20, 0, 0], 0.5: [20, 0, 0], 1.0: [-20, 0, 0]}},
                    "poot_4": {"rotation": {0: [-20, 0, 0], 0.5: [20, 0, 0], 1.0: [-20, 0, 0]}},
                    "poot_2": {"rotation": {0: [20, 0, 0], 0.5: [-20, 0, 0], 1.0: [20, 0, 0]}},
                    "poot_3": {"rotation": {0: [20, 0, 0], 0.5: [-20, 0, 0], 1.0: [20, 0, 0]}},
                    "ketel": {"position": {0: [0, 0, 0], 0.25: [0, 1.5, 0], 0.5: [0, 0, 0], 0.75: [0, 1.5, 0], 1.0: [0, 0, 0]}}}),
        "pomp": kf({"deksel": {"position": {0: [0, 0, 0], 0.2: [0, 4, 0], 0.4: [0, 0, 0], 0.6: [0, 4, 0], 0.8: [0, 0, 0]}},
                    "ketel": {"scale": {0: [1, 1, 1], 0.2: [1.06, 0.95, 1.06], 0.4: [1, 1, 1], 0.6: [1.06, 0.95, 1.06], 0.8: [1, 1, 1]}},
                    "pijp": {"scale": {0: [1, 1, 1], 0.3: [1.2, 1.0, 1.2], 0.8: [1, 1, 1]}}}, loop=False),
        "grijp": kf({"grijparm": {"rotation": {0: [0, 0, 0], 0.5: [-35, 0, 0], 1.0: [25, 0, 0], 1.6: [0, 0, 0]},
                                  "scale": {0: [1, 1, 1], 0.5: [1, 1, 1.4], 1.0: [1, 1, 1.6], 1.6: [1, 1, 1]}}}, loop=False),
    }}
    return g, anims


# =====================================================================================================================
# 6. Vahoegnir (the draak model) + Vahoegje
# =====================================================================================================================
ROZE_DRAAK = (255, 154, 208)


# The dragon assets (geo, flyover + boss anims, vahoegnir_0..5 + glowmasks, Vahoegje) come from vadsrijk_draak.py since o-draak.


def piepje_anims():
    return {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 0.6: [1.03, 0.97, 1.02], 1.2: [1, 1, 1], 2.0: [1, 1, 1]}},
                    "ear_left": {"rotation": {0: [0, 0, 0], 1.4: [0, 0, 0], 1.5: [0, 0, 20], 1.6: [0, 0, 0], 2.0: [0, 0, 0]}},
                    "ear_right": {"rotation": {0: [0, 0, 0], 1.5: [0, 0, 0], 1.6: [0, 0, -20], 1.7: [0, 0, 0], 2.0: [0, 0, 0]}}}),
        "hop": kf({"root": {"position": {0: [0, 0, 0], 0.15: [0, 2, 0], 0.3: [0, 0, 0]}},
                   "foot_back_left": {"rotation": {0: [0, 0, 0], 0.15: [40, 0, 0], 0.3: [0, 0, 0]}},
                   "foot_back_right": {"rotation": {0: [0, 0, 0], 0.15: [40, 0, 0], 0.3: [0, 0, 0]}}}),
        "sprong": kf({"root": {"position": {0: [0, 0, 0], 0.3: [0, 5, 0], 0.6: [0, 0, 0]}, "rotation": {0: [0, 0, 0], 0.3: [-25, 0, 0], 0.6: [0, 0, 0]}},
                      "ear_left": {"rotation": {0: [0, 0, 0], 0.3: [-30, 0, 20], 0.6: [0, 0, 0]}},
                      "ear_right": {"rotation": {0: [0, 0, 0], 0.3: [-30, 0, -20], 0.6: [0, 0, 0]}}}, loop=False),
    }}


def muisje_kaal(a_dir):
    a = np.asarray(Image.open(os.path.join(a_dir, "textures", "entity", "pieppiepmuisje.png")).convert("RGBA"), np.float32).copy()
    l = a[..., :3].mean(axis=2, keepdims=True)
    l = 140 + (l - 140) * 0.5   # less contrast: the fluff is gone
    a[..., :3] = a[..., :3] * 0.2 + (l / 160.0) * np.array([206, 176, 182]) * 0.8
    r = _rng(411)
    m = (r.random(a.shape[:2]) < 0.06) & (a[..., 3] > 0)
    a[m, :3] = a[m, :3] * 0.6 + np.array([150, 150, 160]) * 0.4   # a few sad stubbles
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# =====================================================================================================================
# 8. De arm van de Keizer
# =====================================================================================================================
def keizerarm():
    g = Geo("keizerarm", 256, 256, 2)
    vacht = vlak((132, 130, 138), 8, 421, extra=bont, verloop=(110, 108, 118))
    klauw = vlak((70, 70, 76), 4, 422)
    g.bone("root")
    g.bone("arm", "root", (0, 0, 0))
    g.cube("arm", (-3.5, -3.5, -80), (7, 7, 80), vacht)
    g.bone("onderarm", "arm", (0, 0, -80))
    g.cube("onderarm", (-3, -3, -146), (6, 6, 66), vacht)
    g.cube("onderarm", (-3.6, -3.6, -84), (7.2, 7.2, 6), vacht)   # the elbow
    g.bone("hand", "onderarm", (0, 0, -146))
    g.cube("hand", (-6, -2, -156), (12, 4, 10), vacht)
    for i, x in enumerate((-5.5, -2.8, -0.2, 2.4, 5.0)):
        n = f"vinger_{i + 1}"
        lengte = (12, 16, 18, 16, 11)[i]
        z0 = -156 if i else -150
        g.bone(n, "hand", (x + 0.7, 0, z0))
        g.cube(n, (x, -0.7, z0 - lengte), (1.4, 1.4, lengte), vacht, rotation=(0, -40 if i == 0 else 0, 0), pivot=(x + 0.7, 0, z0))
        g.cube(n, (x + 0.2, -1.4, z0 - lengte - 1.5), (1, 1, 2), klauw, rotation=(0, -40 if i == 0 else 0, 0), pivot=(x + 0.7, 0, z0))
    vingers = [f"vinger_{i + 1}" for i in range(5)]
    idle = kf({**{v: {"rotation": {0: [0, 0, 0], 0.6 + i * 0.15: [12, 0, 0], 1.6: [0, 0, 0]}} for i, v in enumerate(vingers)},
               "hand": {"rotation": {0: [0, 0, 0], 0.8: [0, 4, 0], 1.6: [0, 0, 0]}}})
    anims = {"animations": {
        "idle": idle, "walk": idle,
        "grijp": kf({**{v: {"rotation": {0: [0, 0, 0], 0.4: [-30, 0, 0], 0.7: [85, 0, 0], 2.0: [85, 0, 0]}} for v in vingers},
                     "hand": {"rotation": {0: [0, 0, 0], 0.4: [-15, 0, 0], 0.7: [10, 0, 0], 2.0: [10, 0, 0]}}}, loop=False),
        "tik": kf({v: {"rotation": {0: [0, 0, 0], 0.1 + i * 0.12: [-25, 0, 0], 0.18 + i * 0.12: [15, 0, 0], 0.3 + i * 0.12: [0, 0, 0], 1.2: [0, 0, 0]}}
                   for i, v in enumerate(vingers)}),
    }}
    return g, anims


# =====================================================================================================================
# 9. Props and adds
# =====================================================================================================================
def kooi():
    g = Geo("kooi", 128, 128, 4)
    ijzer = vlak((70, 70, 80), 6, 431)
    korst = vlak((214, 160, 70), 10, 432, extra=lambda a, W, H, f, r: [disc(a, r.uniform(0, W), r.uniform(0, H), 1.2, (180, 120, 50)) for _ in range(W * H // 20)])
    slot = vlak((200, 170, 70), 6, 433, extra=lambda a, W, H, f, r: disc(a, W / 2, H * 0.6, W * 0.15, (40, 30, 30)) if f == "north" else None)
    g.bone("root", None, (0, 30, 0))
    g.bone("ketting", "root", (0, 30, 0))
    g.cube("ketting", (-0.5, 14, -0.5), (1, 16, 1), ijzer)
    g.bone("kooi", "root", (0, 14, 0))
    g.cube("kooi", (-6, 0, -6), (12, 1, 12), korst)
    g.cube("kooi", (-6.5, 12, -6.5), (13, 1.5, 13), korst)
    g.cube("kooi", (-4, 13.5, -4), (8, 1, 8), korst)
    for x in (-6, -2, 2, 5):
        g.cube("kooi", (x, 1, 5), (1, 11, 1), ijzer)
    for z in (-6, -2, 2, 5):
        g.cube("kooi", (-6, 1, z), (1, 11, 1), ijzer)
        g.cube("kooi", (5, 1, z), (1, 11, 1), ijzer)
    g.bone("deurtje", "kooi", (-4, 1, -6))
    for x in (-3.5, -0.5, 2.5):
        g.cube("deurtje", (x, 1, -6), (1, 11, 1), ijzer)
    g.cube("deurtje", (-4, 6, -6), (8, 1, 1), ijzer)
    g.bone("slot", "deurtje", (0, 6, -7))
    g.cube("slot", (-2, 3, -7.5), (4, 4, 1.5), slot)
    g.cube("slot", (-1.2, 7, -7.2), (2.4, 2, 0.8), ijzer)
    anims = {"animations": {
        "idle": kf({"root": {"rotation": {0: [0, 0, -4], 1.5: [0, 0, 4], 3.0: [0, 0, -4]}}}),
        "walk": kf({"root": {"rotation": {0: [0, 0, -4], 1.5: [0, 0, 4], 3.0: [0, 0, -4]}}}),
        "open": kf({"deurtje": {"rotation": {0: [0, 0, 0], 0.5: [0, -110, 0], 3.0: [0, -110, 0]}},
                    "slot": {"position": {0: [0, 0, 0], 0.3: [0, 1, 0], 0.9: [0, -14, 0], 3.0: [0, -14, 0]},
                             "rotation": {0: [0, 0, 0], 0.9: [0, 0, 90], 3.0: [0, 0, 90]}}}, loop=False),
    }}
    return g, anims


def ballon():
    g = Geo("ballon", 128, 128, 2)

    def plek(a, W, H, f, r):
        schimmel(a, W, H, f, r)
        if f in ("north", "south", "east", "west"):
            disc(a, W / 2, H / 2, W * 0.22, (160, 70, 230))
            disc(a, W / 2, H / 2, W * 0.12, (220, 160, 255))
    stof = vlak((222, 206, 140), 10, 441, extra=plek, verloop=(180, 170, 120))
    touw = vlak((150, 120, 80), 6, 442)
    g.bone("root")
    g.bone("ballon", "root", (0, 8, 0))
    g.cube("ballon", (-7, 2, -7), (14, 14, 14), stof)
    g.cube("ballon", (-8, 5, -5), (16, 8, 10), stof)
    g.cube("ballon", (-1, 0, -1), (2, 2, 2), touw)
    bob = {"ballon": {"position": {0: [0, 0, 0], 1.0: [0, 1, 0], 2.0: [0, 0, 0]}, "scale": {0: [1, 1, 1], 1.0: [1.04, 0.97, 1.04], 2.0: [1, 1, 1]}}}
    return g, {"animations": {"idle": kf(bob), "walk": kf(bob)}}


def voedingswortel(glow=False):
    g = Geo("voedingswortel", 128, 128, 2)

    def w(p):
        return leeg if glow else p
    wortel = w(vlak((112, 108, 112), 10, 451, extra=lambda a, W, H, f, r: (nerven(a, W, H, f, r), schimmel(a, W, H, f, r)), verloop=(84, 84, 92)))

    def kern(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not glow:
            a[..., :3] = (180, 80, 150)
            a[..., 3] = 255
        r = _rng(452 + W)
        for _ in range(W * H // 6):
            x, y = r.uniform(0, W), r.uniform(0, H)
            lijn(a, x, y, x + r.uniform(-3, 3), y + r.uniform(-3, 3), AMBERROZE, dik=1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    g.bone("root")
    g.bone("wortel", "root", (0, 0, 0))
    g.cube("wortel", (-5, 0, -5), (10, 30, 10), wortel)
    g.cube("wortel", (-7, 0, -7), (14, 4, 14), wortel)
    g.bone("kern", "wortel", (0, 14, 0))
    g.cube("kern", (-3.5, 10, -5.6), (7, 8, 1), kern)
    g.cube("kern", (-3.5, 10, 4.6), (7, 8, 1), kern)
    puls = {"kern": {"scale": {0: [1, 1, 1], 1.0: [1.1, 1.1, 1.1], 2.0: [1, 1, 1]}}}
    return g, {"animations": {"idle": kf(puls), "walk": kf(puls),
                              "klop": kf({"kern": {"scale": {0: [1, 1, 1], 0.1: [1.4, 1.4, 1.4], 0.4: [1, 1, 1]}},
                                          "wortel": {"scale": {0: [1, 1, 1], 0.1: [1.05, 1.0, 1.05], 0.4: [1, 1, 1]}}}, loop=False)}}


def kanon():
    g = Geo("kanon", 128, 128, 2)
    hout = vlak(HOUT, 8, 461, extra=nerven)
    loop = vlak(KAAS, 8, 462, extra=lambda a, W, H, f, r: [disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0.8, 1.8), (220, 170, 60)) for _ in range(W * H // 25)])
    ring = vlak(IJZER, 6, 463)
    g.bone("root")
    g.bone("voet", "root", (0, 0, 0))
    g.cube("voet", (-6, 0, -6), (12, 5, 12), hout)
    g.cube("voet", (-4, 5, -2), (8, 3, 4), hout)
    g.bone("loop", "voet", (0, 9, 0))
    g.cube("loop", (-3, 6, -16), (6, 6, 20), loop)
    g.cube("loop", (-3.5, 5.5, -17), (7, 7, 2), ring)
    g.cube("loop", (-3.5, 5.5, 2), (7, 7, 2), ring)
    rust = {"loop": {"rotation": {0: [-5, 0, 0], 2.0: [-5, 0, 0]}}}
    return g, {"animations": {"idle": kf(rust), "walk": kf(rust),
                              "schiet": kf({"loop": {"position": {0: [0, 0, 0], 0.08: [0, 0, 3], 0.5: [0, 0, 0]},
                                                     "rotation": {0: [-5, 0, 0], 0.08: [-14, 0, 0], 0.5: [-5, 0, 0]}}}, loop=False)}}


def ketel():
    g = Geo("ketel", 128, 128, 2)
    ijzer = vlak((96, 104, 120), 8, 471, extra=schimmel, verloop=(70, 76, 90))
    pijp = vlak((130, 120, 96), 6, 472)
    g.bone("root")
    g.bone("ketel", "root", (0, 0, 0))
    g.cube("ketel", (-8, 0, -8), (16, 14, 16), ijzer)
    g.cube("ketel", (6, 8, -2), (3, 12, 3), pijp)
    g.bone("deksel", "ketel", (0, 14, 0))
    g.cube("deksel", (-7, 14, -7), (14, 3, 14), ijzer)
    g.cube("deksel", (-1.5, 17, -1.5), (3, 2, 3), pijp)
    rammel = {"deksel": {"position": {0: [0, 0, 0], 0.15: [0, 1, 0], 0.3: [0, 0, 0], 0.45: [0, 0.6, 0], 0.6: [0, 0, 0], 1.5: [0, 0, 0]},
                         "rotation": {0: [0, 0, 0], 0.15: [0, 0, 4], 0.45: [0, 0, -3], 0.6: [0, 0, 0], 1.5: [0, 0, 0]}}}
    return g, {"animations": {"idle": kf(rammel), "walk": kf(rammel)}}


def schimmelbol():
    g = Geo("schimmelbol", 64, 64, 4)
    lijf = vlak(SCHIMMEL, 10, 481, extra=lambda a, W, H, f, r: [disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(0.8, 1.6), (180, 196, 214)) for _ in range(W * H // 25)])
    voet = vlak(SCHIMMEL_DONKER, 6, 482)
    g.bone("root")
    g.bone("body", "root", (0, 2, 0))
    g.cube("body", (-4, 2, -4), (8, 7, 8), lijf, faces={"north": ogen((255, 120, 120), basis=SCHIMMEL)})
    for zij, x in (("l", -3), ("r", 1)):
        g.bone("voet_" + zij, "body", (x + 1, 2, 0))
        g.cube("voet_" + zij, (x, 0, -1.5), (2, 2, 3), voet)
    return g, {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 0.8: [1.06, 0.95, 1.06], 1.6: [1, 1, 1]}}}),
        "walk": kf({"voet_l": {"rotation": {0: [-30, 0, 0], 0.2: [30, 0, 0], 0.4: [-30, 0, 0]}},
                    "voet_r": {"rotation": {0: [30, 0, 0], 0.2: [-30, 0, 0], 0.4: [30, 0, 0]}},
                    "body": {"position": {0: [0, 0, 0], 0.1: [0, 0.6, 0], 0.2: [0, 0, 0], 0.3: [0, 0.6, 0], 0.4: [0, 0, 0]}}})}}


def kloddertje():
    g = Geo("kloddertje", 64, 64, 4)
    pluis = vlak((112, 108, 120), 10, 491, extra=lambda a, W, H, f, r: [lijn(a, x, y, x + r.uniform(-1, 1), y + 1.5, (80, 78, 88), 0.6)
                                                                         for x, y in r.uniform(0, W, (W * H // 8, 2))])
    g.bone("root")
    g.bone("body", "root", (0, 0, 0))
    g.cube("body", (-3.5, 0, -3.5), (7, 6, 7), pluis, faces={"north": ogen((210, 210, 220), boos=False, verdrietig=True, basis=(112, 108, 120))})
    g.cube("body", (-2.5, 6, -2.5), (5, 1, 5), pluis)
    return g, {"animations": {
        "idle": kf({"body": {"scale": {0: [1, 1, 1], 1.0: [1.04, 0.96, 1.04], 2.0: [1, 1, 1]}}}),
        "walk": kf({"body": {"position": {0: [0, 0, 0], 0.15: [0, 1, 0], 0.3: [0, 0, 0]}, "scale": {0: [1, 1, 1], 0.15: [0.95, 1.06, 0.95], 0.3: [1, 1, 1]}}})}}


def ei(glow=False):
    g = Geo("ei", 64, 64, 4)

    def schaal(W, H, f):
        a = np.zeros((H, W, 4), np.float32)
        if not glow:
            a[..., :3] = (255, 196, 220)
            a[..., :3] += _rng(W * 3 + H).normal(0, 5, (H, W, 1))
            a[..., 3] = 255
        r = _rng(495 + W + H)
        for _ in range(max(2, W * H // 40)):
            x, y = r.uniform(0, W), r.uniform(0, H)
            lijn(a, x, y, x + r.uniform(-5, 5), y + r.uniform(-5, 5), (255, 90, 170), dik=1.2)
        if not glow:
            for _ in range(max(1, W * H // 300)):
                disc(a, r.uniform(0, W), r.uniform(0, H), r.uniform(1.5, 3), SCHIMMEL, alpha=0.6)
        return np.clip(a, 0, 255).astype(np.uint8)
    g.bone("root")
    g.bone("ei", "root", (0, 0, 0))
    g.cube("ei", (-4, 0, -4), (8, 2, 8), schaal)
    g.cube("ei", (-5, 2, -5), (10, 5, 10), schaal)
    g.cube("ei", (-4.5, 7, -4.5), (9, 2, 9), schaal)
    g.bone("schaal_boven", "ei", (0, 9, 0))
    g.cube("schaal_boven", (-3.5, 9, -3.5), (7, 2, 7), schaal)
    g.cube("schaal_boven", (-2, 11, -2), (4, 1, 4), schaal)
    puls = {"ei": {"scale": {0: [1, 1, 1], 1.0: [1.04, 1.05, 1.04], 2.0: [1, 1, 1]}}}
    return g, {"animations": {"idle": kf(puls), "walk": kf(puls),
                              "uitkomen": kf({"ei": {"rotation": {0: [0, 0, 0], 0.2: [0, 0, 8], 0.4: [0, 0, -8], 0.6: [0, 0, 10], 0.8: [0, 0, -10],
                                                                  1.0: [6, 0, 6], 1.2: [-6, 0, -6], 1.5: [0, 0, 0], 2.5: [0, 0, 0]}},
                                              "schaal_boven": {"position": {0: [0, 0, 0], 1.5: [0, 0, 0], 1.8: [2, 10, 0], 2.2: [6, 14, 2], 2.5: [9, 6, 3]},
                                                               "rotation": {0: [0, 0, 0], 1.5: [0, 0, 0], 2.5: [0, 0, 160]}}}, loop=False)}}


def krabbel_textuur(a_dir):
    a = _tint(_mika_bron(a_dir), (170, 168, 176), 0.75)
    vak = _gezicht_vak(a, a.shape[1] // 128)
    W, H = vak.shape[1], vak.shape[0]
    for x in (W * 0.3, W * 0.7):   # spectacles
        for t in np.linspace(0, 6.28, 40):
            disc(vak, x + math.cos(t) * W * 0.12, H * 0.42 + math.sin(t) * W * 0.12, 0.9, (60, 50, 40))
    lijn(vak, W * 0.42, H * 0.42, W * 0.58, H * 0.42, (60, 50, 40), 1)
    k = a.shape[1] // 128   # a big quill painted over the body texture's top-left area
    for i in range(40):
        y = 70 * k + i * k
        lijn(a, 40 * k + i * k * 0.6, y, 40 * k + i * k * 0.6 + 6 * k, y - 4 * k, (250, 250, 245), dik=2)
    lijn(a, 40 * k, 70 * k, 64 * k, 110 * k, (60, 50, 40), dik=2)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# =====================================================================================================================
# 10. Blocks
# =====================================================================================================================
def _blokken():
    def vul(kleur, seed, extra=None, verloop=None):
        return Image.fromarray(vlak(kleur, 12, seed, extra=extra, verloop=verloop)(16, 16, "north"), "RGBA")

    def glas(aan):
        def e(a, W, H, f, r):
            a[0, :, :3] = (60, 50, 40)
            a[-1, :, :3] = (60, 50, 40)
            a[:, 0, :3] = (60, 50, 40)
            a[:, -1, :3] = (60, 50, 40)
            if aan:
                disc(a, 8, 8, 4, (255, 250, 220), alpha=0.8)
        return e

    def ringen(stand):
        def e(a, W, H, f, r):
            for rad, k in ((7, (120, 80, 50)), (5, (170, 120, 76)), (3, (120, 80, 50)), (1.5, (170, 120, 76))):
                for t in np.linspace(0, 6.28, 80):
                    disc(a, 8 + math.cos(t) * rad, 8 + math.sin(t) * rad, 0.4, k)
            hoek = math.radians(stand * 45)
            for s in np.linspace(1, 7.3, 12):
                disc(a, 8 + math.sin(hoek) * s, 8 - math.cos(hoek) * s, 0.8, AMBERROZE)
        return e

    def vlecht(barst):
        def e(a, W, H, f, r):
            for y in range(0, 16, 4):
                for x in range(0, 16, 4):
                    k = HOUT_DONKER if (x + y) % 8 == 0 else (176, 130, 80)
                    lijn(a, x, y + 1, x + 4, y + 3, k, dik=1.5)
            if barst:
                lijn(a, 3, 0, 7, 7, (40, 30, 20), 1)
                lijn(a, 7, 7, 5, 15, (40, 30, 20), 1)
                lijn(a, 7, 7, 14, 10, (40, 30, 20), 1)
        return e
    t = {
        "lantaarnpaal_hout": vul(HOUT, 501, extra=nerven),
        "lantaarnpaal_glas_uit": vul((80, 76, 90), 502, extra=glas(False)),
        "lantaarnpaal_glas_aan": vul((255, 190, 200), 503, extra=glas(True)),
        "altaar_goud": vul(HONING, 504, extra=lambda a, W, H, f, r: lijn(a, 0, 13, 16, 13, (200, 140, 40), 1)),
        "altaar_kristal": vul(AMBERROZE, 505, extra=lambda a, W, H, f, r: lijn(a, 2, 14, 14, 2, (255, 230, 245), 1)),
        "poort": vul((120, 128, 146), 506, extra=lambda a, W, H, f, r: (schimmel(a, W, H, f, r), disc(a, 8, 8, 3.2, (200, 60, 60)),
                                                                        disc(a, 8, 8, 1.6, (240, 200, 90)))),
        "hendel_voet": vul((110, 110, 118), 507),
        "hendel_stok": vul(HOUT, 508, extra=nerven),
        "hendel_knop": vul(AMBERROZE, 509),
        "ringknop_zijkant": vul(HOUT, 510, extra=nerven),
        "nerf": vul((240, 100, 180), 511, extra=lambda a, W, H, f, r: [lijn(a, r.uniform(0, 16), r.uniform(0, 16), r.uniform(0, 16), r.uniform(0, 16),
                                                                            (255, 220, 240), 1) for _ in range(4)]),
        "nesttak": vul((150, 106, 64), 512, extra=vlecht(False)),
        "nesttak_barst": vul((150, 106, 64), 512, extra=vlecht(True)),
    }
    for s in range(8):
        t[f"ringknop_{s}"] = vul((196, 150, 100), 513, extra=ringen(s))
    return t


def _el(van, tot, tex, rot=None, faces=("north", "south", "east", "west", "up", "down")):
    e = {"from": van, "to": tot, "faces": {f: {"texture": tex} for f in faces}}
    if rot:
        e["rotation"] = rot
    return e


def _blok_assets(w, a_dir):
    mb, bs = os.path.join(a_dir, "models", "block"), os.path.join(a_dir, "blockstates")
    B = "guhs:block/vr_z3_baas_"
    for aan in (False, True):
        glas = B + "lantaarnpaal_glas_" + ("aan" if aan else "uit")
        w(os.path.join(mb, f"vr_z3_baas_lantaarnpaal_{'aan' if aan else 'uit'}.json"), {
            "parent": "minecraft:block/block", "textures": {"particle": B + "lantaarnpaal_hout", "hout": B + "lantaarnpaal_hout", "glas": glas},
            "elements": [_el([6, 0, 6], [10, 2, 10], "#hout"), _el([7, 2, 7], [9, 11, 9], "#hout"), _el([5, 11, 5], [11, 15, 11], "#glas"),
                         _el([4.5, 15, 4.5], [11.5, 16, 11.5], "#hout")]})
    w(os.path.join(bs, "vr_z3_baas_lantaarnpaal.json"), {"variants": {
        "lit=false": {"model": B + "lantaarnpaal_uit"}, "lit=true": {"model": B + "lantaarnpaal_aan"}}})
    w(os.path.join(mb, "vr_z3_baas_altaar.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": B + "altaar_goud", "goud": B + "altaar_goud", "kristal": B + "altaar_kristal"},
        "elements": [_el([1, 0, 1], [15, 3, 15], "#goud"), _el([3, 3, 3], [13, 9, 13], "#goud"), _el([2, 9, 2], [14, 11, 14], "#goud"),
                     _el([6, 11, 6], [10, 16, 10], "#kristal", rot={"origin": [8, 11, 8], "axis": "y", "angle": 45})]})
    w(os.path.join(bs, "vr_z3_baas_altaar.json"), {"variants": {"": {"model": B + "altaar"}}})
    for naam in ("poort", "nerf"):
        w(os.path.join(mb, f"vr_z3_baas_{naam}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": B + naam}})
        w(os.path.join(bs, f"vr_z3_baas_{naam}.json"), {"variants": {"": {"model": B + naam}}})
    w(os.path.join(mb, "vr_z3_baas_hendel.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": B + "hendel_voet", "voet": B + "hendel_voet", "stok": B + "hendel_stok", "knop": B + "hendel_knop"},
        "elements": [_el([3, 0, 3], [13, 3, 13], "#voet"),
                     _el([7, 3, 7], [9, 12, 9], "#stok", rot={"origin": [8, 3, 8], "axis": "x", "angle": -45}),
                     _el([6.5, 11, 6.5], [9.5, 14, 9.5], "#knop", rot={"origin": [8, 3, 8], "axis": "x", "angle": -45})]})
    w(os.path.join(bs, "vr_z3_baas_hendel.json"), {"variants": {
        f"stand={s}": {"model": B + "hendel", **({"y": s * 90} if s else {})} for s in range(4)}})
    for s in range(8):
        w(os.path.join(mb, f"vr_z3_baas_ringknop_{s}.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": B + f"ringknop_{s}", "side": B + "ringknop_zijkant", "bottom": B + "ringknop_zijkant"}})
    w(os.path.join(bs, "vr_z3_baas_ringknop.json"), {"variants": {f"stand={s}": {"model": B + f"ringknop_{s}"} for s in range(8)}})
    for b in (0, 1):
        w(os.path.join(mb, f"vr_z3_baas_nesttak_{b}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": B + ("nesttak_barst" if b else "nesttak")}})
    w(os.path.join(bs, "vr_z3_baas_nesttak.json"), {"variants": {f"barst={b}": {"model": B + f"nesttak_{b}"} for b in (0, 1)}})


# =====================================================================================================================
# 11. Item icons
# =====================================================================================================================
ITEMS_WAPEN = ["vr_z3_baas_touwknots", "vr_z3_baas_lontstaf", "vr_z3_baas_roquefortsabel", "vr_z3_baas_paraplu", "vr_z3_baas_kooisleutel",
               "vr_z3_baas_hartwortel", "vr_z3_baas_lantaarnstaf", "vr_z3_baas_schimmelbreker", "vr_z3_baas_roofklauw"]


def _iconen():
    O = (40, 30, 46, 255)
    HO = HOUT + (255,)
    RZ = AMBERROZE + (255,)
    GD = HONING + (255,)
    ic = {}
    ic["vr_z3_baas_touwknots"] = _icoon(lambda d: (d.line([(3, 13), (8, 8)], fill=HO, width=2), d.ellipse([7, 2, 14, 9], fill=(196, 160, 104, 255), outline=O),
                                                   d.line([(8, 4), (13, 7)], fill=(150, 116, 70, 255)), d.line([(8, 7), (13, 4)], fill=(150, 116, 70, 255))))
    ic["vr_z3_baas_lontstaf"] = _icoon(lambda d: (d.line([(3, 14), (10, 7)], fill=HO, width=2), d.ellipse([8, 3, 14, 9], fill=KAAS + (255,), outline=O),
                                                  d.line([(12, 4), (14, 1)], fill=(60, 50, 40, 255)), d.point([(14, 0), (15, 1)], fill=(255, 150, 40, 255))))
    ic["vr_z3_baas_roquefortsabel"] = _icoon(lambda d: (d.line([(4, 12), (14, 2)], fill=(226, 230, 240, 255), width=2), d.line([(2, 11), (6, 15)], fill=GD, width=2),
                                                        d.point([(9, 6), (11, 5)], fill=(70, 100, 160, 255))))
    ic["vr_z3_baas_paraplu"] = _icoon(lambda d: (d.pieslice([1, 1, 15, 13], 180, 360, fill=(30, 28, 34, 255), outline=O),
                                                 d.line([(8, 7), (8, 14)], fill=HO), d.arc([6, 12, 10, 16], 0, 180, fill=HO),
                                                 d.line([(1, 7), (15, 7)], fill=(240, 240, 240, 255))))
    ic["vr_z3_baas_kooisleutel"] = _icoon(lambda d: (d.ellipse([8, 1, 15, 8], outline=GD, width=2), d.line([(2, 14), (10, 6)], fill=GD, width=2),
                                                     d.line([(3, 11), (5, 13)], fill=GD), d.point([(11, 4)], fill=RZ)))
    ic["vr_z3_baas_sokkencape"] = _icoon(lambda d: (d.polygon([(4, 2), (12, 2), (14, 14), (2, 14)], fill=(240, 90, 120, 255), outline=O),
                                                    d.line([(3, 6), (13, 6)], fill=(250, 210, 80, 255), width=2), d.line([(2, 10), (14, 10)], fill=(120, 190, 240, 255), width=2)))
    ic["vr_z3_baas_hartwortel"] = _icoon(lambda d: (d.line([(3, 14), (10, 7)], fill=(116, 112, 116, 255), width=2), d.line([(10, 7), (13, 2)], fill=(116, 112, 116, 255)),
                                                    d.line([(10, 7), (14, 6)], fill=(116, 112, 116, 255)), d.ellipse([9, 4, 13, 8], fill=RZ)))
    ic["vr_z3_baas_lantaarnstaf"] = _icoon(lambda d: (d.line([(3, 14), (9, 8)], fill=HO, width=2), d.rectangle([8, 2, 13, 8], fill=(255, 190, 200, 255), outline=O),
                                                      d.point([(10, 5)], fill=(255, 255, 230, 255))))
    ic["vr_z3_baas_schimmelbreker"] = _icoon(lambda d: (d.line([(3, 14), (9, 8)], fill=HO, width=2), d.rectangle([6, 1, 14, 7], fill=(120, 140, 180, 255), outline=O),
                                                        d.point([(8, 3), (12, 5)], fill=(255, 255, 255, 255))))
    ic["vr_z3_baas_roofklauw"] = _icoon(lambda d: (d.line([(2, 13), (13, 2)], fill=HO, width=2), d.arc([3, 3, 15, 15], 180, 270, fill=(126, 126, 136, 255), width=2),
                                                   d.polygon([(12, 1), (15, 1), (13, 4)], fill=(70, 64, 60, 255))))
    ic["vr_z3_baas_kruinveren"] = _icoon(lambda d: (d.rectangle([3, 3, 12, 13], fill=(140, 140, 150, 255), outline=O), d.rectangle([6, 3, 9, 5], fill=(0, 0, 0, 0)),
                                                    *[d.line([(x, 7), (x + 1, 12)], fill=GD) for x in (4, 7, 10)]))
    ic["vr_z3_baas_eierwarmer"] = _icoon(lambda d: (d.arc([3, 0, 13, 10], 0, 180, fill=GD), d.ellipse([5, 6, 11, 15], fill=RZ, outline=O),
                                                    d.point([(7, 9)], fill=(255, 230, 245, 255))))
    ic["vr_z3_baas_bleumonocle"] = _icoon(lambda d: (d.ellipse([4, 3, 12, 11], outline=(200, 210, 230, 255), width=2), d.ellipse([6, 5, 10, 9], fill=(170, 200, 240, 160)),
                                                     d.line([(11, 10), (14, 15)], fill=(200, 210, 230, 255))))

    def trofee(top):
        def t(d):
            d.rectangle([4, 12, 11, 14], fill=(200, 160, 80, 255), outline=O)
            d.rectangle([6, 10, 9, 11], fill=(220, 180, 90, 255))
            top(d)
        return _icoon(t)
    mika = lambda kleur, extra=None: (lambda d: (d.ellipse([3, 3, 12, 10], fill=kleur, outline=O), d.polygon([(3, 4), (4, 1), (6, 3)], fill=kleur),
                                                d.polygon([(12, 4), (11, 1), (9, 3)], fill=kleur), d.point([(6, 6), (9, 6)], fill=(255, 60, 60, 255)),
                                                extra(d) if extra else None))
    ic["vr_z3_baas_trofee_kruinroofvogel"] = trofee(lambda d: (d.ellipse([4, 2, 11, 9], fill=(126, 126, 136, 255), outline=O),
                                                               d.polygon([(4, 5), (1, 7), (4, 7)], fill=(214, 196, 120, 255)), d.point([(6, 4)], fill=(236, 222, 110, 255))))
    ic["vr_z3_baas_trofee_taleggio"] = trofee(mika((246, 214, 160, 255), lambda d: d.line([(3, 3), (12, 3)], fill=(214, 60, 50, 255), width=2)))
    ic["vr_z3_baas_trofee_brokkel"] = trofee(mika((120, 110, 104, 255), lambda d: d.rectangle([4, 5, 11, 7], outline=(60, 50, 40, 255))))
    ic["vr_z3_baas_trofee_roquefort"] = trofee(mika((200, 214, 236, 255), lambda d: d.polygon([(2, 3), (13, 3), (8, 0)], fill=(40, 36, 52, 255))))
    ic["vr_z3_baas_trofee_piepvel"] = trofee(mika((176, 164, 200, 255), lambda d: (d.rectangle([6, 1, 9, 3], fill=GD), d.ellipse([8, 4, 11, 7], outline=GD))))
    ic["vr_z3_baas_trofee_wortelwurger"] = trofee(lambda d: (d.ellipse([5, 3, 10, 9], fill=(92, 70, 100, 255), outline=O),
                                                             *[d.line([(7, 6), (x, y)], fill=(116, 112, 116, 255)) for x, y in ((2, 2), (13, 2), (2, 9), (13, 9))]))
    ic["vr_z3_baas_trofee_nestwachter"] = trofee(lambda d: (d.rectangle([5, 2, 10, 9], fill=HO, outline=O), d.point([(6, 4), (9, 4)], fill=RZ),
                                                            d.rectangle([3, 1, 4, 4], fill=RZ), d.rectangle([11, 1, 12, 4], fill=RZ)))
    ic["vr_z3_baas_trofee_bleu"] = trofee(mika((150, 172, 210, 255), lambda d: d.rectangle([5, 0, 10, 3], fill=(44, 52, 80, 255))))
    ic["vr_z3_baas_trofee_vahoegnir"] = trofee(lambda d: (d.ellipse([3, 3, 12, 10], fill=ROZE_DRAAK + (255,), outline=O),
                                                          d.polygon([(4, 4), (5, 0), (7, 3)], fill=(255, 220, 240, 255)),
                                                          d.polygon([(11, 4), (10, 0), (8, 3)], fill=(255, 220, 240, 255)), d.point([(6, 6), (9, 6)], fill=O)))
    return ic


# =====================================================================================================================
# writing everything
# =====================================================================================================================
def alles(w, a_dir, png):
    tex = os.path.join(a_dir, *TEX.split("/"))
    geo_dir = os.path.join(a_dir, "geckolib", "models", "entity")
    anim_dir = os.path.join(a_dir, "geckolib", "animations", "entity")
    bb = {}

    def model(id_, g, an, vb, textuur):
        w(os.path.join(geo_dir, id_ + ".geo.json"), g.json(vb))
        w(os.path.join(anim_dir, id_ + ".animation.json"), {"format_version": "1.8.0", **an})
        png(os.path.join(tex, textuur + ".png"), g.png())
        bb[id_] = (g.json(), g.png(), an)

    def glowmask(textuur, g):
        png(os.path.join(tex, textuur + "_glowmask.png"), g.png())

    g, an = kruinroofvogel()
    model("vr_z3_baas_kruinroofvogel", g, an, (8, 6, 2), "kruinroofvogel")
    glowmask("kruinroofvogel", kruinroofvogel(glow=True)[0])
    for naam, maker in (("taleggio", taleggio), ("brokkel", brokkel), ("roquefort", roquefort), ("piepvel", piepvel), ("bleu", bleu)):
        g, an = maker(a_dir)
        model("vr_z3_baas_" + naam, g, an, (4, 4, 1), naam)
    png(os.path.join(tex, "piepvel_sokken.png"), piepvel(a_dir, sokken=True)[0].png())
    g, an = wortelwurger()
    model("vr_z3_baas_wortelwurger", g, an, (6, 5, 2), "wortelwurger")
    glowmask("wortelwurger", wortelwurger(glow=True)[0])
    g, an = nestwachter()
    model("vr_z3_baas_nestwachter", g, an, (4, 5, 2), "nestwachter")
    glowmask("nestwachter", nestwachter(glow=True)[0])
    png(os.path.join(tex, "nestwachter_roze.png"), nestwachter(roze=True)[0].png())
    glowmask("nestwachter_roze", nestwachter(roze=True, glow=True)[0])
    g, an = schimmelkolos()
    model("vr_z3_baas_schimmelkolos", g, an, (6, 6, 2), "schimmelkolos")
    # Vahoegnir + Vahoegje: the redesigned dragon (o-draak), its own generator
    bb.update(draak_alles(w, a_dir, png))
    w(os.path.join(anim_dir, "vr_huisdier_piepje.animation.json"), {"format_version": "1.8.0", **piepje_anims()})
    png(os.path.join(tex, "muisje_kaal.png"), muisje_kaal(a_dir))
    g, an = keizerarm()
    model("vr_z3_baas_keizerarm", g, an, (12, 4, 0), "keizerarm")
    for id_, maker, vb in (("kooi", kooi, (2, 3, 1)), ("ballon", ballon, (2, 2, 0.5)), ("voedingswortel", voedingswortel, (2, 3, 1)),
                           ("kanon", kanon, (2, 2, 0.5)), ("ketel", ketel, (2, 2, 0.5)), ("schimmelbol", schimmelbol, (1, 1, 0.3)),
                           ("kloddertje", kloddertje, (1, 1, 0.3)), ("ei", ei, (1, 1, 0.4))):
        g, an = maker()
        model("vr_z3_baas_" + id_, g, an, vb, id_)
    glowmask("voedingswortel", voedingswortel(glow=True)[0])
    glowmask("ei", ei(glow=True)[0])
    png(os.path.join(tex, "krabbel.png"), krabbel_textuur(a_dir))
    png(os.path.join(tex, "matroos.png"), matroos_textuur(a_dir))
    # items
    for item, im in _iconen().items():
        png(os.path.join(a_dir, "textures", "item", item + ".png"), im)
        w(os.path.join(a_dir, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a_dir, "models", "item", item + ".json"),
          {"parent": "minecraft:item/handheld" if item in ITEMS_WAPEN else "minecraft:item/generated", "textures": {"layer0": "guhs:item/" + item}})
    # blocks
    for naam, im in _blokken().items():
        png(os.path.join(a_dir, "textures", "block", "vr_z3_baas_" + naam + ".png"), im)
    _blok_assets(w, a_dir)
    return bb


def blockbench(bb):
    d = os.path.join("blockbench", "vadsrijk", "z3")
    os.makedirs(d, exist_ok=True)
    for naam, (geo, im, an) in bb.items():
        with open(os.path.join(d, naam + ".bbmodel"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(bbmodel(geo, im, an, naam), f)


def _png(pad, im):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    im.save(pad)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    bb = alles(_schrijf, a, _png)
    blockbench(bb)
    print(f"vadsrijk_z3_bazen_modellen: {len(bb)} models + textures, blocks, icons")


if __name__ == "__main__":
    _direct()
