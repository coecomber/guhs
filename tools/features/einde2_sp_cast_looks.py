"""
einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast - the story LOOKS and the story animations of the Sponsguh cast.

The cast's models are written by einde2_sponsguh_wezens_modellen (phase A, owned by sp_cast); this module adds to them:

  voeg_toe(id, atlas, model)   the look bones of that model (one bone per look, named look_<look>), painted on the
                               model's own atlas AFTER its own patches (the phase-A layout of every sheet stays the same).
                               The renderer (E2SponsguhClient.WezenRenderer) hides every look bone whose look is off
                               (SponsguhWezen's synced looks mask, the order of SpCast.LOOKS); `droog` has no bone: the
                               renderer tints the model grey-brown and draws it at 0.85 scale.
  animaties(id, A, namen)      the story animations the scenes name (CONTRACT_E2_C 7.2), added to the model's animation set
  LOOKS / VERBERG              which looks each model has (= the Java table Cast.looks) and the bones a look hides
  STORY                        which story animations each model has (= the Java table Cast.eigen, checked by
                               einde2_sponsguh_wezens_modellen.check through NODIG)

Model coordinates: x left(+)/right(-) of the character, y up, z: the face looks to -z (north).
"""
import math

import numpy as np

from features import boerderij_dieren as bd

cube, bone, plain = bd.cube, bd.bone, bd.plain

# the eight looks, in the order of SpCast.LOOKS (the bit of each in SponsguhWezen's mask)
ALLE = ["snor", "emmer", "droog", "ijsje", "netje", "petje", "manager", "kroon_vast"]

# model id -> the looks it can show (Cast.looks in Java says the same; octoliam wears octo_mika's model)
LOOKS = {
    "sponsguh": ["snor", "emmer", "droog", "ijsje", "netje", "petje", "manager", "kroon_vast"],
    "patguh": ["snor", "emmer", "droog", "ijsje", "netje"],
    "meneer_krabguh": ["emmer"],
    "parel_guh": ["emmer"],
    "sandy_guh": ["emmer"],
    "octo_mika": ["emmer"],
    "bikinibodem_guh": ["emmer"],
}
# looks without a bone of their own (the renderer draws them)
ZONDER_BOT = {"droog"}
# a look that is on hides these bones of the model (Sponsguh holds the crown up with both arms: the arms are the look's own)
VERBERG = {("sponsguh", "kroon_vast"): ["arm_left", "arm_right"]}

# the story animations per model: (name, lasting?) - lasting ones go on until another is set (Cast.DOORLOPEND)
STORY = {
    "sponsguh": ["zing", "rennen", "huil_blij", "salueer", "slurp", "knipper"],
    "patguh": ["zing", "nies", "trots"],
    "meneer_krabguh": ["knuffel", "paniek"],
    "octo_mika": ["trommel_tel", "buig"],
    "plankmika": ["lach_kwaad", "vlucht"],
    "neptuguh": ["blaas"],
    "dennis_mika": ["remmen", "plat"],
    "cycloop_mika": ["nat"],
    "juf_pufguh": ["zucht"],
    "mindy_guh": ["geef"],
    "david_hasselguh": ["zwaai"],
    "loopgraafmonster": ["wieg"],
    "bikinibodem_guh": ["dans"],
}
# the story animations that last (a pose or a loop: Cast.DOORLOPEND in Java has them too)
DOORLOPEND = {"zing", "rennen", "knuffel", "paniek", "vlucht", "plat", "nat", "wieg", "dans"}
# the length (seconds) of each one-shot: Cast.duur in Java says the same in ticks (x 20)
DUUR = {"huil_blij": 2.4, "salueer": 1.6, "slurp": 1.6, "knipper": 0.8, "nies": 1.6, "trots": 1.6, "trommel_tel": 2.4, "buig": 1.6,
        "lach_kwaad": 2.0, "blaas": 1.6, "remmen": 1.2, "zucht": 1.6, "geef": 1.6, "zwaai": 1.4}

# --- colours ------------------------------------------------------------------------------------------------------------------
EMMER = (150, 156, 168)
EMMER_LICHT = (204, 210, 220)
EMMER_DONKER = (100, 104, 116)
GAT = (18, 16, 24)
SNOR = (118, 70, 38)
SNOR_LICHT = (170, 116, 60)
WIT = (248, 248, 250)
PET_BLAUW = (60, 110, 200)
MANAGER = (34, 44, 88)
GOUD = (250, 200, 60)
GOUD_DONKER = (196, 140, 36)
GEEL = (250, 214, 74)
ROOD = (214, 44, 48)
STOK = (170, 120, 70)
NET = (236, 236, 244)


def _disc(a, cx, cy, r, colour, ry=None):
    H, W = a.shape[:2]
    ry = ry or r
    yy, xx = np.mgrid[0:H, 0:W]
    m = ((xx + 0.5 - cx) / max(r, 0.01)) ** 2 + ((yy + 0.5 - cy) / max(ry, 0.01)) ** 2 <= 1
    a[m, :3] = colour
    a[m, 3] = 255
    return m


def deuken(base=EMMER, licht=EMMER_LICHT, donker=EMMER_DONKER, gaten=()):
    """Old dented metal of a bucket: a soft shine, a few dents (a dark edge under a light spot), and eye holes
    (gaten: (fx, fy, fr) as fractions of the face: the middle and the radius in face widths)."""
    def paint(W, H, rng):
        a = plain(base, 7)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        t = np.clip(1 - (xx / max(1, W) * 0.7 + yy / max(1, H)), 0, 1)[..., None]
        a[..., :3] = a[..., :3] * (1 - 0.35 * t) + np.array(licht, np.float32) * 0.35 * t
        for _ in range(max(1, W * H // 260)):
            cx, cy, r = rng.uniform(2, max(3, W - 2)), rng.uniform(2, max(3, H - 2)), rng.uniform(1.4, 2.8)
            _disc(a, cx + 0.6, cy + 0.6, r, donker)
            _disc(a, cx, cy, r * 0.8, np.array(base) * 0.92 + np.array(licht) * 0.08)
        for x in range(0, W, max(6, W // 5)):                      # the seams of the sheet metal
            a[:, x:x + 1, :3] *= 0.86
        for fx, fy, fr in gaten:
            r = fr * W
            _disc(a, fx * W, fy * H, r + 1.0, donker)
            _disc(a, fx * W, fy * H, r, GAT)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def koord(base=SNOR, licht=SNOR_LICHT):
    """Twisted knabbelkoordraad: diagonal strands."""
    def paint(W, H, rng):
        a = plain(base, 8)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        streng = ((xx + yy) % 4) < 1.5
        a[streng, :3] = a[streng, :3] * 0.55 + np.array(licht, np.float32) * 0.45
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def met_embleem(base, embleem_kleur, band=None, anker=True):
    """A cap's front: the cloth, a band at the bottom, an emblem in the middle (an anchor, or a round badge)."""
    def paint(W, H, rng):
        a = plain(base, 6)(W, H, rng).astype(np.float32)
        if band:
            a[int(H * 0.68):, :, :3] = band
        cx, cy, r = W / 2, H * 0.38, min(W, H) * 0.22
        if anker:
            a[int(cy - r):int(cy + r), int(cx - 0.6):int(cx + 1.0), :3] = embleem_kleur
            a[int(cy - r):int(cy - r + 1.2), int(cx - r * 0.6):int(cx + r * 0.6), :3] = embleem_kleur
            for k in range(int(r * 1.2)):
                y = int(cy + r - k * 0.4)
                a[y:y + 1, int(cx - r + k * 0.8):int(cx - r + k * 0.8) + 1, :3] = embleem_kleur
                a[y:y + 1, int(cx + r - k * 0.8):int(cx + r - k * 0.8) + 1, :3] = embleem_kleur
        else:
            _disc(a, cx, cy, r * 1.2, embleem_kleur)
            _disc(a, cx, cy, r * 0.6, base)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def net_maas(W, H, rng):
    """The mesh of a jelly net: thin white threads, see-through in between."""
    a = np.zeros((H, W, 4), np.uint8)
    yy, xx = np.mgrid[0:H, 0:W]
    m = ((xx % 4) == 0) | ((yy % 4) == 0)
    a[m, :3] = NET
    a[m, 3] = 255
    return a


def bolletje(colour, stip=None):
    """An ice cream scoop: the colour with a lighter top and (optional) sprinkles."""
    def paint(W, H, rng):
        a = plain(colour, 6)(W, H, rng).astype(np.float32)
        a[:max(1, H // 4), :, :3] = a[:max(1, H // 4), :, :3] * 0.7 + 255 * 0.3
        if stip:
            for _ in range(max(1, W * H // 12)):
                x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
                a[y, x, :3] = stip[int(rng.integers(len(stip)))]
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


# =====================================================================================================================
# the look bones
# =====================================================================================================================
# model id -> (parent, bottom y, top y, half width, z front, z back, eye x, eye y, eye radius): Plankmika's bucket helmets
EMMERS = {
    "sponsguh": ("head", 22.8, 6.1, -3.1, 3.1, 2.3, 16.9, 1.1),
    "patguh": ("head", 27.4, 5.1, -3.1, 3.1, 1.8, 15.4, 0.9),
    "meneer_krabguh": ("head", 21.8, 4.7, -2.3, 2.3, 2.2, 17.9, 1.0),
    "parel_guh": ("head", 41.8, 7.9, -6.9, 5.9, 3.0, 33.4, 1.4),
    "sandy_guh": ("head", 27.8, 8.8, -6.2, 6.2, 2.5, 17.6, 1.2),
    "octo_mika": ("head", 48.4, 5.7, -6.4, 4.6, 2.2, 40.8, 1.2),
    "bikinibodem_guh": ("head", 21.6, 5.1, -4.4, 4.0, 1.9, 15.6, 0.9),
}


def emmer(a, id):
    """A dented metal bucket upside down over the head, two eye holes; the guh ears poke out of it."""
    parent, top, hw, z0, z1, ex, ey, er = EMMERS[id]
    bodem = ey - er - 1.6                                      # (the rim stays under the eye holes)
    hoog = top - bodem
    d = z1 - z0
    gaten = [(0.5 + sx * ex / (2 * hw), (top - 1.4 - ey) / (hoog - 1.4), er / (2 * hw)) for sx in (-1, 1)]
    metaal = deuken()
    rand = plain(EMMER_DONKER, 6)
    cubes = [cube(a, [-hw, bodem, z0], [2 * hw, hoog - 1.4, d], metaal, overrides={"north": deuken(gaten=gaten)}),
             cube(a, [-hw + 0.8, top - 1.4, z0 + 0.8], [2 * hw - 1.6, 1.4, d - 1.6], metaal),
             cube(a, [-hw - 0.35, bodem - 0.2, z0 - 0.35], [2 * hw + 0.7, 0.9, d + 0.7], rand)]
    for sx in (-1, 1):                                         # the two lugs of the handle
        x = hw if sx > 0 else -hw - 0.6
        cubes.append(cube(a, [x, bodem + hoog * 0.45, (z0 + z1) / 2 - 0.5], [0.6, 1.0, 1.0], rand))
    return bone("look_emmer", parent, [0, bodem, (z0 + z1) / 2], cubes)


def snor(a, id):
    """Mindy-guh's moustache of knabbelkoordraad, curled up at both ends."""
    p = koord()
    if id == "sponsguh":
        y, z, s = 12.7, -3.55, 1.0                             # under the nose, over the two teeth
    else:
        y, z, s = 12.9, -3.1, 0.85                             # Patguh: over his mouth
    cubes = [cube(a, [-2.2 * s, y, z], [4.4 * s, 1.1, 0.9], p)]
    for sx in (-1, 1):
        x0 = 2.1 * s if sx > 0 else -3.7 * s
        cubes.append(cube(a, [x0, y + 0.45, z + 0.05], [1.6 * s, 1.0, 0.8], p))
        x1 = 3.5 * s if sx > 0 else -4.3 * s
        cubes.append(cube(a, [x1, y + 1.2, z + 0.1], [0.8 * s, 1.1, 0.7], p))
    return bone("look_snor", "head", [0, y, z], cubes)


def ijsje(a, id):
    """The towering sundae of the Guhfy Guhber: a glass cup, four scoops, cream, a cherry and a wafer."""
    if id == "sponsguh":
        parent, cx, y, cz = "arm_right", -6.2, 5.6, -2.0     # in his right hand, held up in front of him
    else:
        parent, cx, y, cz = "body", -11.7, 7.6, 0.0          # Patguh: on the tip of his right arm point
    glas = plain((206, 232, 246), 5)
    bollen = [((250, 150, 186), None), ((252, 240, 210), [(214, 44, 48), (60, 110, 200), (120, 200, 80)]), ((122, 74, 42), None),
              ((150, 222, 170), None)]
    cubes = [cube(a, [cx - 1.0, y, cz - 1.0], [2.0, 0.8, 2.0], glas), cube(a, [cx - 1.6, y + 0.8, cz - 1.6], [3.2, 1.6, 3.2], glas)]
    yy = y + 2.4
    for i, (kleur, stip) in enumerate(bollen):
        w = 3.3 - 0.25 * i
        cubes.append(cube(a, [cx - w / 2, yy, cz - w / 2], [w, 2.4, w], bolletje(kleur, stip)))
        yy += 2.2
    cubes.append(cube(a, [cx - 0.9, yy, cz - 0.9], [1.8, 1.4, 1.8], plain(WIT, 4)))
    cubes.append(cube(a, [cx - 0.5, yy + 1.4, cz - 0.5], [1.0, 1.0, 1.0], plain(ROOD, 6)))
    cubes.append(cube(a, [cx + 0.6, yy - 1.0, cz - 0.2], [0.4, 3.0, 1.2], plain((236, 196, 120), 6)))
    return bone("look_ijsje", parent, [cx, y, cz], cubes)


def netje(a, id):
    """A jelly net: a stick from the paw up, a hoop over the head, a white mesh."""
    if id == "sponsguh":
        parent, x, y0, z = "arm_right", -6.2, 3.6, -1.6
    else:
        parent, x, y0, z = "body", -11.7, 6.0, -1.6
    stok = plain(STOK, 6)
    hoog = 17.6 if id == "sponsguh" else 14.0
    y1 = y0 + hoog
    ring = plain((120, 200, 236), 5)
    cubes = [cube(a, [x - 0.25, y0, z - 0.25], [0.5, hoog, 0.5], stok),
             cube(a, [x - 3.0, y1, z - 0.25], [6.0, 0.5, 0.5], ring), cube(a, [x - 3.0, y1 + 4.9, z - 0.25], [6.0, 0.5, 0.5], ring),
             cube(a, [x - 3.0, y1 + 0.5, z - 0.25], [0.5, 4.4, 0.5], ring), cube(a, [x + 2.5, y1 + 0.5, z - 0.25], [0.5, 4.4, 0.5], ring),
             cube(a, [x - 2.5, y1 + 0.5, z - 0.05], [5.0, 4.4, 0.1], net_maas)]
    return bone("look_netje", parent, [x, y0, z], cubes)


def pet(a, naam, kleur, embleem, band, klep):
    """Sponsguh's cap on top of his head (the ears poke out at the sides)."""
    voor = met_embleem(kleur, embleem, band=band, anker=naam == "look_petje")
    cubes = [cube(a, [-4.4, 20.3, -2.4], [8.8, 2.8, 4.8], plain(kleur, 6), overrides={"north": voor}),
             cube(a, [-3.2, 20.3, -5.2], [6.4, 0.6, 2.8], plain(klep, 6)),
             cube(a, [-0.6, 23.1, -0.6], [1.2, 0.5, 1.2], plain(band, 4))]
    if naam == "look_manager":                                    # the manager's cap: a gold band all round
        cubes.append(cube(a, [-4.55, 20.3, -2.55], [9.1, 0.8, 5.1], plain(GOUD, 5)))
    return bone(naam, "head", [0, 20.3, 0], cubes)


def kroon_vast(a):
    """Neptuguh's crown held up high in both hands (the look's own raised arms: the model's arms are hidden)."""
    geel, wit = plain(GEEL, 6), plain(WIT, 4)
    goud = plain(GOUD, 6)
    cubes = []
    for sx in (-1, 1):
        cx = 6.2 * sx
        cubes += [cube(a, [5.5 if sx > 0 else -6.9, 11.2, -1], [1.4, 1.6, 2], wit),
                  cube(a, [cx - 0.5, 12.8, -0.5], [1, 13.0, 1], geel), cube(a, [cx - 0.8, 25.6, -0.8], [1.6, 1.4, 1.6], geel)]

    def band(W, H, rng):
        arr = plain(GOUD, 6)(W, H, rng).astype(np.float32)
        for i, kleur in enumerate(((214, 44, 48), (60, 170, 230), (120, 220, 120))):
            _disc(arr, W * (0.25 + 0.25 * i), H * 0.5, min(W, H) * 0.28, kleur)
        return np.clip(arr, 0, 255).astype(np.uint8)
    cubes.append(cube(a, [-5.4, 26.0, -1.6], [10.8, 1.8, 3.2], goud, overrides={"north": band, "south": band}))
    for k in range(5):
        x = -4.6 + k * 2.1
        cubes.append(cube(a, [x, 27.8, -0.6], [1.0, 1.6 + (0.8 if k == 2 else 0), 1.0], plain(GOUD_DONKER if k % 2 else GOUD, 5)))
    return bone("look_kroon_vast", "head", [0, 22.6, 0], cubes)


def voeg_toe(id, a, model):
    """Adds the look bones of model `id` (painted on its atlas a, after everything of its own)."""
    looks = LOOKS.get(id, [])
    if not looks:
        return model
    bones = model["minecraft:geometry"][0]["bones"]
    for look in looks:
        if look in ZONDER_BOT:
            continue
        if look == "emmer":
            bones.append(emmer(a, id))
        elif look == "snor":
            bones.append(snor(a, id))
        elif look == "ijsje":
            bones.append(ijsje(a, id))
        elif look == "netje":
            bones.append(netje(a, id))
        elif look == "petje":
            bones.append(pet(a, "look_petje", WIT, PET_BLAUW, PET_BLAUW, PET_BLAUW))
        elif look == "manager":
            bones.append(pet(a, "look_manager", MANAGER, GOUD, GOUD_DONKER, (24, 24, 30)))
        elif look == "kroon_vast":
            bones.append(kroon_vast(a))
    return model


def botten(id):
    """The names of the look bones of model id."""
    return [f"look_{l}" for l in LOOKS.get(id, []) if l not in ZONDER_BOT]


# =====================================================================================================================
# the story animations
# =====================================================================================================================
def _kf(pairs):
    return {str(round(t, 3)): [round(x, 3) for x in v] for t, v in pairs}


def _anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": round(length, 3), "bones": bones}


def _golf(as_, amp, length, n=4, fase=0.0, om=(0, 0, 0)):
    out = []
    for i in range(n + 1):
        v = list(om)
        v[as_] += amp * math.sin(2 * math.pi * (i / n) + fase)
        out.append((length * i / n, v))
    return _kf(out)


def _heen_weer(waarden, length):
    """Keyframes evenly over length through these values (the last = the first: a smooth loop or back to rest)."""
    n = len(waarden) - 1
    return _kf([(length * i / n, v) for i, v in enumerate(waarden)])


def animaties(id, A, namen):
    """Adds the story animations of model id to A (name -> animation); namen = its bones."""
    heeft = lambda n: n in namen   # noqa: E731
    Z = [0, 0, 0]
    if id == "sponsguh":
        L = 2.0
        A["zing"] = _anim(L, {"body": {"rotation": _golf(2, 6, L)}, "head": {"rotation": _golf(2, -5, L, om=(-10, 0, 0))},
                              "arm_left": {"rotation": _golf(2, 14, L, om=(0, 0, -95))}, "arm_right": {"rotation": _golf(2, 14, L, om=(0, 0, 95))},
                              "ear_left": {"rotation": _golf(2, 10, L / 2)}, "ear_right": {"rotation": _golf(2, -10, L / 2)}})
        L = 0.5
        A["rennen"] = _anim(L, {"body": {"position": _heen_weer([Z, [0, 1.2, 0], Z, [0, 1.2, 0], Z], L), "rotation": _kf([(0, [12, 0, 0]), (L, [12, 0, 0])])},
                                "leg_left": {"rotation": _golf(0, 55, L)}, "leg_right": {"rotation": _golf(0, -55, L)},
                                "arm_left": {"rotation": _heen_weer([[-40, 0, -160], [30, 0, -60], [-60, 0, -150], [20, 0, -40], [-40, 0, -160]], L)},
                                "arm_right": {"rotation": _heen_weer([[30, 0, 60], [-50, 0, 160], [20, 0, 40], [-40, 0, 150], [30, 0, 60]], L)},
                                "ear_left": {"rotation": _kf([(0, [0, 0, 30]), (L, [0, 0, 30])])}, "ear_right": {"rotation": _kf([(0, [0, 0, -30]), (L, [0, 0, -30])])}})
        L = DUUR["huil_blij"]
        A["huil_blij"] = _anim(L, {"head": {"rotation": _kf([(t * 0.15, [-8, 0, 6 if t % 2 else -6]) for t in range(1, 15)] + [(0, Z), (L, Z)]),
                                            "scale": _kf([(0, [1, 1, 1]), (0.3, [1.04, 0.96, 1]), (2.0, [1.04, 0.96, 1]), (L, [1, 1, 1])])},
                                   "arm_left": {"rotation": _kf([(0, Z), (0.3, [-110, 0, -20]), (2.0, [-110, 0, -20]), (L, Z)])},
                                   "arm_right": {"rotation": _kf([(0, Z), (0.3, [-110, 0, 20]), (2.0, [-110, 0, 20]), (L, Z)])},
                                   "ear_left": {"rotation": _kf([(0, Z), (0.3, [0, 0, 40]), (1.6, [0, 0, 40]), (1.9, [0, 0, -20]), (L, Z)])},
                                   "ear_right": {"rotation": _kf([(0, Z), (0.3, [0, 0, -40]), (1.6, [0, 0, -40]), (1.9, [0, 0, 20]), (L, Z)])}}, False)
        L = DUUR["salueer"]
        A["salueer"] = _anim(L, {"arm_right": {"rotation": _kf([(0, Z), (0.25, [-150, 0, 40]), (1.3, [-150, 0, 40]), (L, Z)])},
                                 "body": {"scale": _kf([(0, [1, 1, 1]), (0.25, [1, 1.05, 1]), (1.3, [1, 1.05, 1]), (L, [1, 1, 1])])},
                                 "head": {"rotation": _kf([(0, Z), (0.25, [-6, 0, 0]), (1.3, [-6, 0, 0]), (L, Z)])}}, False)
        L = DUUR["slurp"]
        A["slurp"] = _anim(L, {"arm_right": {"rotation": _kf([(0, Z), (0.3, [-70, 0, -30]), (1.2, [-70, 0, -30]), (L, Z)])},
                               "head": {"rotation": _kf([(0, Z), (0.3, [14, 0, 0]), (1.2, [14, 0, 0]), (L, Z)]),
                                        "scale": _kf([(0, [1, 1, 1]), (0.4, [0.94, 0.94, 0.94]), (0.7, [1.08, 1.08, 1.08]), (1.0, [0.95, 0.95, 0.95]),
                                                      (1.3, [1.06, 1.06, 1.06]), (L, [1, 1, 1])])}}, False)
        L = DUUR["knipper"]
        A["knipper"] = _anim(L, {"head": {"scale": _kf([(0, [1, 1, 1]), (0.1, [1.04, 0.9, 1]), (0.2, [1, 1, 1]), (0.4, [1.04, 0.9, 1]), (0.5, [1, 1, 1]), (L, [1, 1, 1])])},
                                 "ear_left": {"rotation": _kf([(0, Z), (0.1, [0, 0, -18]), (0.5, [0, 0, -18]), (L, Z)])},
                                 "ear_right": {"rotation": _kf([(0, Z), (0.1, [0, 0, 18]), (0.5, [0, 0, 18]), (L, Z)])}}, False)
    if id == "patguh":
        L = 2.0
        A["zing"] = _anim(L, {"body": {"rotation": _golf(2, 7, L)}, "head": {"rotation": _golf(2, -6, L, om=(-8, 0, 0))},
                              "arm_left": {"rotation": _golf(2, 12, L, om=(0, 0, -30))}, "arm_right": {"rotation": _golf(2, 12, L, om=(0, 0, 30))}})
        L = DUUR["nies"]
        A["nies"] = _anim(L, {"head": {"rotation": _kf([(0, Z), (0.5, [-22, 0, 0]), (0.7, [26, 0, 0]), (1.0, [10, 0, 0]), (L, Z)])},
                              "body": {"position": _kf([(0, Z), (0.7, [0, 0, -1.2]), (1.0, Z), (L, Z)]),
                                       "scale": _kf([(0, [1, 1, 1]), (0.5, [1.06, 1.08, 1.06]), (0.7, [0.95, 0.94, 0.95]), (1.0, [1, 1, 1]), (L, [1, 1, 1])])},
                              "ear_left": {"rotation": _kf([(0, Z), (0.7, [0, 0, -30]), (1.1, Z), (L, Z)])},
                              "ear_right": {"rotation": _kf([(0, Z), (0.7, [0, 0, 30]), (1.1, Z), (L, Z)])}}, False)
        L = DUUR["trots"]
        A["trots"] = _anim(L, {"body": {"scale": _kf([(0, [1, 1, 1]), (0.3, [1.08, 1.04, 1.14]), (1.3, [1.08, 1.04, 1.14]), (L, [1, 1, 1])])},
                               "head": {"rotation": _kf([(0, Z), (0.3, [-14, 0, 0]), (1.3, [-14, 0, 0]), (L, Z)])},
                               "arm_left": {"rotation": _kf([(0, Z), (0.3, [0, 0, 26]), (1.3, [0, 0, 26]), (L, Z)])},
                               "arm_right": {"rotation": _kf([(0, Z), (0.3, [0, 0, -26]), (1.3, [0, 0, -26]), (L, Z)])}}, False)
    if id == "meneer_krabguh":
        L = 2.0
        A["knuffel"] = _anim(L, {"arm_left": {"rotation": _golf(2, 4, L, om=(-30, 0, 28))}, "arm_right": {"rotation": _golf(2, -4, L, om=(-30, 0, -28))},
                                 "body": {"rotation": _golf(2, 5, L)}, "steel_left": {"rotation": _golf(2, 6, L, om=(0, 0, 10))},
                                 "steel_right": {"rotation": _golf(2, -6, L, om=(0, 0, -10))}})
        L = 0.6
        A["paniek"] = _anim(L, {"arm_left": {"rotation": _heen_weer([[0, 0, -70], [0, 0, 10], [0, 0, -70]], L)},
                                "arm_right": {"rotation": _heen_weer([[0, 0, -10], [0, 0, 70], [0, 0, -10]], L)},
                                "body": {"position": _heen_weer([[-0.6, 0, 0], [0.6, 0.6, 0], [-0.6, 0, 0]], L)},
                                "steel_left": {"rotation": _heen_weer([[0, 0, -20], [0, 0, 20], [0, 0, -20]], L / 2 * 2)},
                                "steel_right": {"rotation": _heen_weer([[0, 0, 20], [0, 0, -20], [0, 0, 20]], L)},
                                "leg_left": {"rotation": _golf(0, 30, L)}, "leg_right": {"rotation": _golf(0, -30, L)}})
    if id == "octo_mika":
        L = DUUR["trommel_tel"]
        tel = [(0, Z)]
        for k in range(4):
            tel += [(0.15 + k * 0.5, [-95, 0, -10]), (0.4 + k * 0.5, [-60, 0, -10])]
        tel.append((L, Z))
        A["trommel_tel"] = _anim(L, {"arm_right": {"rotation": _kf(tel)},
                                     "head": {"rotation": _kf([(0, Z)] + [(0.15 + k * 0.5, [8, 0, 0]) for k in range(4)] + [(0.4 + k * 0.5, [0, 0, 0]) for k in range(4)] + [(L, Z)])}}, False)
        L = DUUR["buig"]
        A["buig"] = _anim(L, {"body": {"rotation": _kf([(0, Z), (0.4, [34, 0, 0]), (1.1, [34, 0, 0]), (L, Z)])},
                              "arm_right": {"rotation": _kf([(0, Z), (0.4, [-40, 0, 30]), (1.1, [-40, 0, 30]), (L, Z)])},
                              "arm_left": {"rotation": _kf([(0, Z), (0.4, [20, 0, -10]), (1.1, [20, 0, -10]), (L, Z)])}}, False)
    if id == "plankmika":
        L = DUUR["lach_kwaad"]
        A["lach_kwaad"] = _anim(L, {"head": {"rotation": _kf([(t * 0.1, [-18, 0, 5 if t % 2 else -5]) for t in range(1, 18)] + [(0, Z), (L, Z)])},
                                    "arm_left": {"rotation": _kf([(0, Z), (0.2, [0, 0, -140]), (1.8, [0, 0, -140]), (L, Z)])},
                                    "arm_right": {"rotation": _kf([(0, Z), (0.2, [0, 0, 140]), (1.8, [0, 0, 140]), (L, Z)])},
                                    "body": {"position": _kf([(t * 0.1, [0, 0.4 if t % 2 else 0, 0]) for t in range(20)] + [(L, Z)])}}, False)
        L = 0.3
        A["vlucht"] = _anim(L, {"leg_left": {"rotation": _golf(0, 60, L)}, "leg_right": {"rotation": _golf(0, -60, L)},
                                "arm_left": {"rotation": _golf(0, -60, L, om=(0, 0, -40))}, "arm_right": {"rotation": _golf(0, 60, L, om=(0, 0, 40))},
                                "body": {"position": _heen_weer([Z, [0, 0.5, 0], Z, [0, 0.5, 0], Z], L)},
                                "head": {"rotation": _kf([(0, [10, 0, 0]), (L, [10, 0, 0])])}})
    if id == "neptuguh":
        L = DUUR["blaas"]
        A["blaas"] = _anim(L, {"arm_left": {"rotation": _kf([(0, Z), (0.3, [-120, 0, -30]), (1.3, [-120, 0, -30]), (L, Z)])},
                               "head": {"rotation": _kf([(0, Z), (0.3, [-14, 0, 0]), (1.3, [-14, 0, 0]), (L, Z)]),
                                        "scale": _kf([(0, [1, 1, 1]), (0.4, [1.08, 1.04, 1.08]), (1.2, [1.1, 1.05, 1.1]), (L, [1, 1, 1])])},
                               "body": {"scale": _kf([(0, [1, 1, 1]), (0.3, [1.05, 1.03, 1.05]), (1.3, [1.05, 1.03, 1.05]), (L, [1, 1, 1])])}}, False)
        # his own big laugh (the basis "lach", made royal: the belly shakes, the trident goes up)
        L = 1.6
        A["lach"] = _anim(L, {"body": {"position": _kf([(t * 0.1, [0, 1.0 if t % 2 else 0, 0]) for t in range(17)]),
                                       "scale": _kf([(t * 0.1, [1.04, 0.98, 1.04] if t % 2 else [1, 1, 1]) for t in range(17)])},
                              "head": {"rotation": _kf([(0, Z), (0.2, [-16, 0, 0]), (1.4, [-16, 0, 0]), (L, Z)])},
                              "arm_right": {"rotation": _kf([(0, Z), (0.3, [-40, 0, 20]), (1.3, [-40, 0, 20]), (L, Z)])}}, False)
    if id == "dennis_mika":
        L = DUUR["remmen"]
        A["remmen"] = _anim(L, {"body": {"rotation": _kf([(0, Z), (0.2, [-18, 0, 0]), (0.6, [-10, 0, 0]), (L, Z)])},
                                "head": {"rotation": _kf([(0, Z), (0.2, [-10, 0, 0]), (0.8, [-10, 0, 0]), (L, Z)])},
                                "root": {"position": _kf([(0, [0, 3.4, 0]), (0.2, [0, 4.2, 0]), (0.4, [0, 3.4, 0]), (L, [0, 3.4, 0])])},
                                "motor": {"rotation": _kf([(0, Z), (0.2, [-6, 0, 0]), (0.4, Z), (L, Z)])}}, False)
        # flattened like a pancake under the lead boot, wobbling (a pose: the scene sets "loop" when he walks off)
        L = 1.2
        A["plat"] = _anim(L, {"root": {"scale": _heen_weer([[1.5, 0.18, 1.5], [1.4, 0.24, 1.4], [1.5, 0.18, 1.5]], L), "position": [0, 0, 0]},
                              "head": {"rotation": _golf(2, 8, L)}, "ear_left": {"rotation": _golf(2, 20, L)}, "ear_right": {"rotation": _golf(2, -20, L)},
                              "arm_left": {"rotation": [0, 0, -80]}, "arm_right": {"rotation": [0, 0, 80]}, "motor": {"scale": [0.001, 0.001, 0.001]}})
    if id == "cycloop_mika":
        L = 2.0
        A["nat"] = _anim(L, {"head": {"rotation": _golf(2, 4, L, om=(16, 0, 0))}, "body": {"rotation": _kf([(0, [8, 0, 0]), (L, [8, 0, 0])])},
                             "arm_left": {"rotation": _golf(0, 4, L, om=(0, 0, 8))}, "arm_right": {"rotation": _golf(0, -4, L, om=(0, 0, -8))},
                             "ear_left": {"rotation": _kf([(0, [0, 0, 40]), (L, [0, 0, 40])])}, "ear_right": {"rotation": _kf([(0, [0, 0, -40]), (L, [0, 0, -40])])}})
    if id == "juf_pufguh":
        L = DUUR["zucht"]
        A["zucht"] = _anim(L, {"head": {"rotation": _kf([(0, Z), (0.4, [-10, 0, 0]), (0.9, [18, 0, 0]), (1.3, [18, 0, 0]), (L, Z)])},
                               "body": {"scale": _kf([(0, [1, 1, 1]), (0.4, [1.06, 1.05, 1.06]), (0.9, [0.95, 0.96, 0.95]), (L, [1, 1, 1])])},
                               "ear_left": {"rotation": _kf([(0, Z), (0.9, [0, 0, 24]), (1.3, [0, 0, 24]), (L, Z)])},
                               "ear_right": {"rotation": _kf([(0, Z), (0.9, [0, 0, -24]), (1.3, [0, 0, -24]), (L, Z)])}}, False)
    if id == "mindy_guh":
        L = DUUR["geef"]
        A["geef"] = _anim(L, {"arm_right": {"rotation": _kf([(0, Z), (0.35, [-80, 0, 0]), (1.2, [-80, 0, 0]), (L, Z)])},
                              "arm_left": {"rotation": _kf([(0, Z), (0.35, [-60, 0, 0]), (1.2, [-60, 0, 0]), (L, Z)])},
                              "head": {"rotation": _kf([(0, Z), (0.35, [8, 0, 6]), (1.2, [8, 0, 6]), (L, Z)])}}, False)
    if id == "david_hasselguh":
        L = DUUR["zwaai"]
        A["zwaai"] = _anim(L, {"arm_right": {"rotation": _kf([(0, Z), (0.25, [0, 0, 60]), (0.5, [0, 0, 30]), (0.75, [0, 0, 60]), (1.0, [0, 0, 30]), (1.2, [0, 0, 50]), (L, Z)])},
                               "head": {"rotation": _kf([(0, Z), (0.3, [-8, 0, 6]), (1.1, [-8, 0, 6]), (L, Z)])}}, False)
    if id == "loopgraafmonster":
        L = 3.0
        A["wieg"] = _anim(L, {"body": {"rotation": _golf(2, 10, L)}, "head": {"rotation": _golf(2, -8, L, om=(6, 0, 0))},
                              "arm_left": {"rotation": _golf(2, 10, L, om=(0, 0, -40))}, "arm_right": {"rotation": _golf(2, 10, L, om=(0, 0, 40))},
                              "tail": {"rotation": _golf(1, 16, L)}})
    if id == "bikinibodem_guh":
        L = 1.0
        A["dans"] = _anim(L, {"body": {"position": _heen_weer([Z, [0, 1.4, 0], Z, [0, 1.4, 0], Z], L), "rotation": _golf(2, 10, L)},
                              "arm_left": {"rotation": _heen_weer([[0, 0, -140], [0, 0, -60], [0, 0, -140]], L)},
                              "arm_right": {"rotation": _heen_weer([[0, 0, 60], [0, 0, 140], [0, 0, 60]], L)},
                              "leg_left": {"rotation": _golf(0, 20, L)}, "leg_right": {"rotation": _golf(0, -20, L)},
                              "head": {"rotation": _golf(2, -8, L)}})
    # every story animation only moves bones the model has
    for naam in STORY.get(id, []):
        for bn in list(A.get(naam, {}).get("bones", {})):
            if not heeft(bn):
                del A[naam]["bones"][bn]
    return A
