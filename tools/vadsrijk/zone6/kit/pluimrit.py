"""Pluimguh met ruiter (uit de mount-kit, renders-mounts/_bron, gekopieerd) als extra-quads in wereldcoördinaten.

    bereden(pos, yaw, variant, galop=True)  -> quads (wereld, blokken); zet ze met SK.quads_naar_voxel in een venster
    los(pos, yaw, variant)                  -> pluimguh zonder ruiter (stal, oefenbaan)
    speler_staand(pos, yaw, lantaarn=True)   -> speler te voet met een Nestlantaarn (Wortelduister)
yaw: 0 = kijkt naar het noorden (-z), 90 = naar het westen, 180 = naar het zuiden, 270 = naar het oosten.
"""
import os
import numpy as np
import mountkit as K
import mounts as MT

GEO = os.path.join(K.MODELS, "{}.geo.json")
PNG = os.path.join(K.MODELS, "{}.png")
GALOP = {"body": {"rot": [9, 0, 0], "pos": [0, 1.5, 0]},
         "nek": {"rot": [22, 0, 0]}, "hoofd": {"rot": [-14, 0, 0]},
         "leg_l": {"rot": [-58, 0, 0]}, "scheen_l": {"rot": [22, 0, 0]}, "voet_l": {"rot": [30, 0, 0]},
         "leg_r": {"rot": [48, 0, 0]}, "scheen_r": {"rot": [62, 0, 0]}, "voet_r": {"rot": [40, 0, 0]},
         "vleugel_l": {"rot": [0, 0, -38]}, "vleugel_r": {"rot": [0, 0, 38]},
         "staart": {"rot": [26, 0, 0]}, "oor_l": {"rot": [-28, 0, 0]}, "oor_r": {"rot": [-28, 0, 0]},
         "kuif": {"rot": [-30, 0, 0]}, "root": {"pos": [0, 3.0, 0]}}
_cache = {}


def mount(variant="roze"):
    k = ("pluimguh", variant)
    if k not in _cache:
        _cache[k] = K.laad(GEO.format("vr_pluimguh"), PNG.format(f"vr_pluimguh_{variant}"))
    return _cache[k]


def speler():
    if "speler" not in _cache:
        _cache["speler"] = K.laad(GEO.format("speler"), PNG.format("speler"))
    return _cache["speler"]


def bereden(pos, yaw, variant="roze", galop=True, stof=True):
    m = mount(variant)
    base = K.base_mat(pos, yaw)
    pose = GALOP if galop else None
    W = K.bone_mats(m, pose)
    zb, zp, teugels, spreid = MT.ZADEL["pluimguh"]
    rp = {k: dict(v) for k, v in K.RIJ_POSE.items()}
    if galop:
        rp["leg_r"] = {"rot": [-30, -8, 40]}
        rp["leg_l"] = {"rot": [-30, 8, -40]}
        rp["arm_r"] = {"rot": [-68, 6, 4]}
        rp["arm_l"] = {"rot": [-68, -6, -4]}
        rp["head"] = {"rot": [-14, -16, 0]}
    else:
        rp["leg_r"] = {"rot": [-34, -8, spreid]}
        rp["leg_l"] = {"rot": [-34, 8, -spreid]}
    q = K.quads(m, base, pose)
    rq, handen = K.ruiter_quads(speler(), base, W, zb, zp, rp, 14 if galop else 0)
    q += rq
    for h, (b, p) in zip(handen, teugels):
        tp = (base @ W[b] @ np.r_[K.punt(np.eye(4), p), 1])[:3]
        q += K.balk_quads(h, tp, 0.06, (120, 60, 56))
    if galop and stof:
        # stofwolkjes achter de poten (achter = tegen de kijkrichting in)
        a = np.radians(yaw)
        achter = np.array([np.sin(a), 0, np.cos(a)])
        rng = np.random.default_rng(5)
        for i in range(6):
            p = np.array(pos, float) + achter * (1.4 + i * 0.45) + np.array([0, 0.2 + 0.1 * (i % 2), 0])
            p += np.cross(achter, [0, 1, 0]) * rng.uniform(-0.5, 0.5)
            q += K.wolkje_quads(tuple(p), 0.16 + 0.05 * (i % 3), kleur=(236, 226, 214))
    return q


def los(pos, yaw, variant="roze"):
    return K.quads(mount(variant), K.base_mat(pos, yaw))


def speler_staand(pos, yaw, lantaarn=True):
    """Speler te voet; rechterarm vooruit met een lantaarn (als klein blokje-quad, lichtgevend in de render)."""
    sp = speler()
    pose = {"arm_r": {"rot": [-60, 0, 0]}, "head": {"rot": [8, 0, 0]}}
    base = K.base_mat(pos, yaw, K.SPELER_SCHAAL)
    q = K.quads(sp, base, pose)
    if lantaarn:
        a = np.radians(yaw)
        voor = np.array([-np.sin(a), 0, -np.cos(a)])
        rechts = np.array([np.cos(a), 0, -np.sin(a)]) * -1
        hand = np.array(pos, float) + voor * 0.75 + rechts * 0.35 + np.array([0, 1.15, 0])
        q += K.balk_quads(hand + [0, 0.35, 0], hand, 0.04, (60, 60, 70))
        q += K.balk_quads(hand + [0, -0.05, 0], hand + [0, -0.4, 0], 0.32, (255, 214, 120))
    return q
