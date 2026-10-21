"""
Ronde 3 (levendige dieren, ontwerp hoofdstuk 5): extra animaties die het diergedrag (SafariDier) gebruikt, gemaakt uit de
botten van het model, zodat elke soort ze heeft zonder dat de goedgekeurde modellen veranderen.

    slaap   liggen/opgekruld, rustig ademen (de client toont er de slaapoogjes bij, <naam>_slaap.png)
    rust    hetzelfde liggen met open oogjes (een rustmoment overdag)
    plus per soort (EXTRA): pinguin buikglij, olifant/mammoet spuit, bever draag, ...

Liggen per lichaamsvorm: met pootjes (leg_*) zakt het hele dier (root) de pootlengte omlaag en klappen de poten plat
(voorpoten naar voren, achterpoten naar achteren); zonder pootjes (zee, rups) alleen rustig deinen. Hangende dieren
(luiaard, glimworm) slapen in hun hang-houding (die laat SafariDier staan).
Draairichtingen zijn Blockbench (bedrock): +x kantelt de bovenkant naar voren (-z), zoals de kop bij "eet".
"""


def kf(*pairs):
    return {str(round(float(t), 3)): v for t, v in pairs}


def _botten(geo):
    g = geo["minecraft:geometry"][0]
    uit = {}
    for b in g["bones"]:
        cs = b.get("cubes", [])
        lo = min((c["origin"][1] for c in cs), default=None)
        hi = max((c["origin"][1] + c["size"][1] for c in cs), default=None)
        uit[b["name"]] = dict(parent=b.get("parent"), pivot=b.get("pivot", [0, 0, 0]), lo=lo, hi=hi)
    return uit


def _poten(bt):
    return [n for n in bt if n.startswith("leg_")]


def _lengte(bt, poten):
    hoog = 0.0
    for n in poten:
        b = bt[n]
        if b["lo"] is not None:
            hoog = max(hoog, b["pivot"][1] - b["lo"])
    return hoog


def _lig(bt, adem=True):
    """The lying pose: bones -> channels."""
    poten = _poten(bt)
    L = _lengte(bt, poten)
    bones = {}
    wortel = "root" if "root" in bt else "body"
    if poten and L > 0.5:
        bones[wortel] = {"position": kf((0, [0, -(L - 0.6), 0]))}
        for n in poten:
            voor = "front" in n or (len(poten) == 2)        # two-legged: both forward (sitting on its heels)
            hoek = -82 if voor else 82
            bones[n] = {"rotation": kf((0, [hoek, 0, 0]))}
    if "body" in bt and adem:
        bones.setdefault("body", {})["scale"] = kf((0, [1.0, 1.0, 1.0]), (1.6, [1.04, 0.97, 1.02]), (3.2, [1.0, 1.0, 1.0]))
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [0, 35, 0]))}
    for oor in ("ear_left", "ear_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 22 if oor == "ear_left" else -22]))}
    for v in ("wing_left", "wing_right", "flipper_left", "flipper_right"):
        if v in bt:
            bones[v] = {"rotation": kf((0, [0, 0, 0]))}
    if not poten:
        # no legs (sea animals, larvae): a slow bob, sunk a little
        bones[wortel] = {"position": kf((0, [0, -0.5, 0]), (1.6, [0, 0.0, 0]), (3.2, [0, -0.5, 0]))}
    return bones


def slaap(bt):
    return {"loop": True, "animation_length": 3.2, "bones": _lig(bt)}


def rust(bt):
    bones = _lig(bt)
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [0, 0, 0]), (2.0, [0, 12, 0]), (4.0, [0, -12, 0]), (6.0, [0, 0, 0]))}
    return {"loop": True, "animation_length": 6.0, "bones": bones}


def buikglij(bt):
    """The penguin on its belly, flippers back, sliding (the movement itself is SafariDier's)."""
    bones = {"root": {"rotation": kf((0, [84, 0, 0])), "position": kf((0, [0, 3.5, 0]))}}
    for f, z in (("flipper_left", -35), ("flipper_right", 35)):
        if f in bt:
            bones[f] = {"rotation": kf((0, [-70, 0, z]), (0.3, [-74, 0, z * 1.2]), (0.6, [-70, 0, z]))}
    for n in _poten(bt):
        bones[n] = {"rotation": kf((0, [-80, 0, 0]))}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [-70, 0, 0]))}
    return {"loop": True, "animation_length": 0.6, "bones": bones}


def spuit(bt):
    """Trunk up and a shower (the water itself: particles from SafariDier)."""
    bones = {}
    if "trunk" in bt:
        bones["trunk"] = {"rotation": kf((0, [0, 0, 0]), (0.4, [-115, 0, 0]), (1.6, [-120, 0, 0]), (2.0, [0, 0, 0]))}
    if "trunk_tip" in bt:
        bones["trunk_tip"] = {"rotation": kf((0, [0, 0, 0]), (0.4, [-50, 0, 0]), (1.6, [-55, 0, 0]), (2.0, [0, 0, 0]))}
    for oor, z in (("ear_left", -25), ("ear_right", 25)):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 0]), (0.5, [0, z, 0]), (1.0, [0, 0, 0]), (1.5, [0, z, 0]), (2.0, [0, 0, 0]))}
    return {"loop": False, "animation_length": 2.0, "bones": bones}


def draag(bt, walk):
    """Walking with something in its mouth (the beaver's branch): the walk with the head up."""
    bones = {k: dict(v) for k, v in walk["bones"].items()}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [-14, 0, 0]))}
    return {"loop": True, "animation_length": walk["animation_length"], "bones": bones}


def ren(walk):
    """Running with the herd: the walk, twice as fast."""
    L = walk["animation_length"]
    bones = {}
    for b, chans in walk["bones"].items():
        bones[b] = {ch: {str(round(float(t) / 2, 3)): v for t, v in keys.items()} for ch, keys in chans.items()}
    return {"loop": True, "animation_length": round(L / 2, 3), "bones": bones}


# --- round 4: one-shot "show" animations (test round 4: "de dieren mogen meer speciale emotes/acties doen") ------------
# Lengths match BezigheidGoal.SHOW (ticks / 20). Each only when the model has the bones it moves (else not made, so
# not in soorten.json "acties" either).

def _wortel(bt):
    return "root" if "root" in bt else "body"


def _trillen(t0, t1, stap, a, b):
    """Keyframes alternating a/b from t0 to t1 every stap seconds."""
    uit, t, i = [], t0, 0
    while t <= t1 + 1e-6:
        uit.append((t, a if i % 2 == 0 else b))
        t += stap
        i += 1
    return uit


def brul(bt):
    """A roar: head up and forward, body leaning back, tail up."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [0, 0, 0]), (0.35, [-28, 0, 0]), (0.6, [-24, 0, 0]), (1.6, [-26, 0, 0]), (2.5, [0, 0, 0]))}}
    if "body" in bt:
        bones["body"] = {"rotation": kf((0, [0, 0, 0]), (0.35, [-6, 0, 0]), (1.6, [-6, 0, 0]), (2.5, [0, 0, 0]))}
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [0, 0, 0]), (0.4, [-35, 0, 0]), (1.8, [-30, 0, 0]), (2.5, [0, 0, 0]))}
    for oor in ("ear_left", "ear_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 0]), (0.35, [20, 0, 0]), (1.6, [20, 0, 0]), (2.5, [0, 0, 0]))}
    return {"loop": True, "animation_length": 2.5, "bones": bones}


def gaap(bt):
    """A big slow yawn: head tipping back, a little shake at the end."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [0, 0, 0]), (0.9, [-38, 0, 6]), (1.6, [-40, 0, 6]), (2.0, [-6, 0, -8]),
                                     (2.2, [-4, 0, 8]), (2.5, [0, 0, 0]))}}
    for oor, z in (("ear_left", 25), ("ear_right", -25)):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 0]), (0.9, [0, 0, z]), (1.6, [0, 0, z]), (2.5, [0, 0, 0]))}
    return {"loop": True, "animation_length": 2.5, "bones": bones}


def rek(bt):
    """A stretch (the play bow): front down with the front legs forward, back end up, tail up."""
    voor = [n for n in _poten(bt) if "front" in n]
    if len(voor) < 2:
        return None
    w = _wortel(bt)
    bones = {w: {"rotation": kf((0, [0, 0, 0]), (0.6, [10, 0, 0]), (1.8, [10, 0, 0]), (2.5, [0, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (0.6, [0, -1.2, 0]), (1.8, [0, -1.2, 0]), (2.5, [0, 0, 0]))}}
    for n in voor:
        bones[n] = {"rotation": kf((0, [0, 0, 0]), (0.6, [-48, 0, 0]), (1.8, [-48, 0, 0]), (2.5, [0, 0, 0]))}
    for n in _poten(bt):
        if "back" in n:
            bones[n] = {"rotation": kf((0, [0, 0, 0]), (0.6, [-8, 0, 0]), (1.8, [-8, 0, 0]), (2.5, [0, 0, 0]))}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [0, 0, 0]), (0.6, [-18, 0, 0]), (1.8, [-18, 0, 0]), (2.5, [0, 0, 0]))}
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [0, 0, 0]), (0.6, [-40, 0, 0]), (1.8, [-40, 0, 0]), (2.5, [0, 0, 0]))}
    return {"loop": True, "animation_length": 2.5, "bones": bones}


def schud(bt):
    """A wet-dog shake: the body wiggles from side to side, head and ears the other way."""
    if "body" not in bt:
        return None
    keys = [(0, [0, 0, 0])] + _trillen(0.12, 1.2, 0.12, [0, 0, 14], [0, 0, -14]) + [(1.8, [0, 0, 0])]
    bones = {"body": {"rotation": kf(*keys)}}
    if "head" in bt:
        bones["head"] = {"rotation": kf(*[(t, [0, 0, -v[2] * 0.8]) for t, v in keys])}
    for oor in ("ear_left", "ear_right", "wing_left", "wing_right", "flipper_left", "flipper_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf(*[(t, [0, 0, abs(v[2]) * (1 if "left" in oor else -1)]) for t, v in keys])}
    return {"loop": True, "animation_length": 1.8, "bones": bones}


def snuffel(bt):
    """Nose to the ground, sniffing from side to side."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [0, 0, 0]), (0.5, [26, -18, 0]), (0.8, [24, -18, 0]), (1.1, [27, -16, 0]),
                                     (1.6, [26, 18, 0]), (1.9, [24, 18, 0]), (2.2, [27, 16, 0]), (3.0, [0, 0, 0]))}}
    if "hals" in bt:
        bones["hals"] = {"rotation": kf((0, [0, 0, 0]), (0.5, [30, 0, 0]), (2.2, [30, 0, 0]), (3.0, [0, 0, 0]))}
    elif "neck" in bt:
        bones["neck"] = {"rotation": kf((0, [0, 0, 0]), (0.5, [20, 0, 0]), (2.2, [20, 0, 0]), (3.0, [0, 0, 0]))}
    return {"loop": True, "animation_length": 3.0, "bones": bones}


def rondkijk(bt):
    """Looking around: head turns slowly to one side, the other, then back (with a little tilt)."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [0, 0, 0]), (0.8, [-4, 42, 6]), (1.5, [-4, 42, 6]), (2.2, [-4, -42, -6]),
                                     (2.9, [-4, -42, -6]), (3.5, [0, 0, 0]))}}
    for oor in ("ear_left", "ear_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 0]), (0.9, [-12, 0, 0]), (1.1, [0, 0, 0]), (2.3, [-12, 0, 0]), (2.5, [0, 0, 0]))}
    return {"loop": True, "animation_length": 3.5, "bones": bones}


def huppel(bt):
    """A few happy hops on the spot."""
    w = _wortel(bt)
    if w not in bt:
        return None
    bones = {w: {"position": kf((0, [0, 0, 0]), (0.18, [0, 3, 0]), (0.36, [0, 0, 0]), (0.54, [0, 3, 0]), (0.72, [0, 0, 0]),
                                (0.9, [0, 3.5, 0]), (1.1, [0, 0, 0]), (1.5, [0, 0, 0]))}}
    for n in _poten(bt):
        hoek = -25 if "front" in n else 25
        bones[n] = {"rotation": kf((0, [0, 0, 0]), (0.18, [hoek, 0, 0]), (0.36, [0, 0, 0]), (0.54, [hoek, 0, 0]),
                                   (0.72, [0, 0, 0]), (0.9, [hoek, 0, 0]), (1.1, [0, 0, 0]))}
    return {"loop": True, "animation_length": 1.5, "bones": bones}


def krab(bt):
    """Scratching behind the ear with a back leg (head tilted toward it)."""
    if "leg_back_left" not in bt or "head" not in bt:
        return None
    w = _wortel(bt)
    bones = {w: {"rotation": kf((0, [0, 0, 0]), (0.3, [0, 0, -8]), (2.1, [0, 0, -8]), (2.5, [0, 0, 0]))},
             "head": {"rotation": kf((0, [0, 0, 0]), (0.3, [6, 0, 24]), (2.1, [6, 0, 24]), (2.5, [0, 0, 0]))},
             "leg_back_left": {"rotation": kf((0, [0, 0, 0]), (0.3, [-55, 0, 25]),
                                              *_trillen(0.4, 2.0, 0.1, [-70, 0, 28], [-50, 0, 22]), (2.5, [0, 0, 0]))}}
    return {"loop": True, "animation_length": 2.5, "bones": bones}


def pluk(bt):
    """The giraffe reaches up and plucks a leaf (prop_pluk shows during this), then chews."""
    if "hals" not in bt or "head" not in bt:
        return None
    bones = {"hals": {"rotation": kf((0, [0, 0, 0]), (0.7, [-22, 0, 0]), (1.3, [-22, 0, 0]), (1.8, [-6, 0, 0]), (3.0, [-6, 0, 0]),
                                     (3.5, [0, 0, 0]))},
             "head": {"rotation": kf((0, [0, 0, 0]), (0.7, [-30, 0, 0]), (1.0, [-20, 0, 0]), (1.3, [-28, 0, 0]),
                                     (1.8, [6, 0, 0]), *_trillen(2.0, 3.0, 0.15, [8, 0, 3], [4, 0, -3]), (3.5, [0, 0, 0]))}}
    for oor in ("ear_left", "ear_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf((0, [0, 0, 0]), (0.7, [0, 0, 18 if oor == "ear_left" else -18]), (1.8, [0, 0, 0]))}
    return {"loop": True, "animation_length": 3.5, "bones": bones}


def schelp(bt):
    """The otter on its back, tapping the shell on its belly with both front paws (prop_schelp shows during this)."""
    if "leg_front_left" not in bt or "leg_front_right" not in bt:
        return None
    bones = {_wortel(bt): {"rotation": kf((0, [0, 0, 180]), (2.25, [0, 0, 184]), (4.5, [0, 0, 180])),
                           "position": kf((0, [0, 8, 0]), (2.25, [0, 8.3, 0]), (4.5, [0, 8, 0]))}}
    tik = _trillen(0, 4.5, 0.25, [-100, 0, 14], [-72, 0, 20])
    bones["leg_front_left"] = {"rotation": kf(*tik)}
    bones["leg_front_right"] = {"rotation": kf(*[(t, [v[0], 0, -v[2]]) for t, v in tik])}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [24, 0, 0]))}
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [0, -20, 0]), (2.25, [0, 20, 0]), (4.5, [0, -20, 0]))}
    return {"loop": True, "animation_length": 4.5, "bones": bones}


# --- 1.6.0 deel 2 (F10a): work animations for the helper and build animals (design 2.8) ------------------------------
# Played by the helpers (F10b) while they build; named bouw_<naam> and NOT park acties (safari_dieren skips "bouw_").
# Each only when the model has the bones it moves; looping, so the helper can hold one as long as a job lasts.

def _voorpoten(bt):
    for paar in (("leg_front_left", "leg_front_right"), ("arm_left", "arm_right"), ("flipper_left", "flipper_right"),
                 ("wing_left", "wing_right")):
        if all(n in bt for n in paar):
            return paar
    return None


def bouw_knaag(bt):
    """Gnawing: head down at the trunk, quick little chews, ears twitching (the beaver felling a tree)."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [24, 0, 0]), *_trillen(0.1, 1.1, 0.1, [30, -6, 3], [26, 6, -3]), (1.2, [24, 0, 0]))}}
    if "body" in bt:
        bones["body"] = {"rotation": kf((0, [6, 0, 0]), (0.6, [8, 0, 0]), (1.2, [6, 0, 0]))}
    for oor in ("ear_left", "ear_right"):
        if oor in bt:
            bones[oor] = {"rotation": kf(*_trillen(0, 1.2, 0.3, [0, 0, 0], [-15, 0, 0]))}
    pp = _voorpoten(bt)
    if pp:
        for n in pp:
            bones[n] = {"rotation": kf((0, [-35, 0, 0]))}
    return {"loop": True, "animation_length": 1.2, "bones": bones}


def bouw_graaf(bt):
    """Digging: front down, the front paws scraping back one after the other (the earth flying: particles of the helper)."""
    pp = _voorpoten(bt)
    if not pp:
        return None
    w = _wortel(bt)
    bones = {w: {"rotation": kf((0, [12, 0, 0])), "position": kf((0, [0, -0.6, 0]))},
             pp[0]: {"rotation": kf((0, [-50, 0, 0]), (0.2, [40, 0, 0]), (0.4, [-50, 0, 0]))},
             pp[1]: {"rotation": kf((0, [40, 0, 0]), (0.2, [-50, 0, 0]), (0.4, [40, 0, 0]))}}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [14, 0, 0]))}
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [-30, 0, 0]), (0.2, [-36, 0, 0]), (0.4, [-30, 0, 0]))}
    return {"loop": True, "animation_length": 0.4, "bones": bones}


def bouw_sjouw(bt, walk):
    """Carrying a load while walking: the walk, head up; a trunk curled up, arms held in front."""
    if not walk:
        return None
    bones = {k: dict(v) for k, v in walk["bones"].items()}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [-12, 0, 0]))}
    if "trunk" in bt:
        bones["trunk"] = {"rotation": kf((0, [-70, 0, 0]))}
    if "trunk_tip" in bt:
        bones["trunk_tip"] = {"rotation": kf((0, [-80, 0, 0]))}
    for arm in ("arm_left", "arm_right"):
        if arm in bt:
            bones[arm] = {"rotation": kf((0, [-70, 0, 12 if arm == "arm_left" else -12]))}
    return {"loop": True, "animation_length": walk["animation_length"], "bones": bones}


def bouw_stamp(bt):
    """Stamping the ground flat: up on the back legs a little, both front legs down with a thump, twice."""
    pp = _voorpoten(bt)
    if not pp or "leg_back_left" not in bt:
        return None
    w = _wortel(bt)
    bones = {w: {"rotation": kf((0, [0, 0, 0]), (0.35, [-14, 0, 0]), (0.5, [2, 0, 0]), (0.6, [0, 0, 0]),
                                (0.95, [-14, 0, 0]), (1.1, [2, 0, 0]), (1.2, [0, 0, 0]), (1.6, [0, 0, 0]))}}
    for n in pp:
        bones[n] = {"rotation": kf((0, [0, 0, 0]), (0.35, [-40, 0, 0]), (0.5, [6, 0, 0]), (0.6, [0, 0, 0]),
                                   (0.95, [-40, 0, 0]), (1.1, [6, 0, 0]), (1.2, [0, 0, 0]), (1.6, [0, 0, 0]))}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [0, 0, 0]), (0.35, [-10, 0, 0]), (0.5, [8, 0, 0]), (0.95, [-10, 0, 0]),
                                        (1.1, [8, 0, 0]), (1.6, [0, 0, 0]))}
    return {"loop": True, "animation_length": 1.6, "bones": bones}


def bouw_duw(bt):
    """Pushing (demolishing, shoving snow): head down, the whole animal leaning in and shoving forward in pushes."""
    if "head" not in bt:
        return None
    w = _wortel(bt)
    bones = {"head": {"rotation": kf((0, [22, 0, 0]))},
             w: {"position": kf((0, [0, 0, 0]), (0.5, [0, 0, -1.6]), (0.7, [0, 0, -1.4]), (1.2, [0, 0, 0]))}}
    for n in _poten(bt):
        if "back" in n:
            bones[n] = {"rotation": kf((0, [0, 0, 0]), (0.5, [24, 0, 0]), (0.7, [22, 0, 0]), (1.2, [0, 0, 0]))}
    if "trunk" in bt:
        bones["trunk"] = {"rotation": kf((0, [20, 0, 0]))}
    return {"loop": True, "animation_length": 1.2, "bones": bones}


def bouw_pik(bt):
    """Pecking / hacking: head (and a bird's body) in quick strokes forward (a woodpecker at a beam, a bill planting)."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf(*_trillen(0, 0.6, 0.075, [0, 0, 0], [38, 0, 0]))}}
    for nek in ("hals", "neck"):
        if nek in bt:
            bones[nek] = {"rotation": kf(*_trillen(0, 0.6, 0.075, [0, 0, 0], [20, 0, 0]))}
    if "body" in bt and ("wing_left" in bt or "leg_left" in bt):
        bones["body"] = {"rotation": kf(*_trillen(0, 0.6, 0.075, [0, 0, 0], [8, 0, 0]))}
    if "tail" in bt:
        bones["tail"] = {"rotation": kf((0, [-15, 0, 0]))}
    return {"loop": True, "animation_length": 0.6, "bones": bones}


def bouw_klop(bt):
    """Patting / hammering with both front paws (or flippers) down in turn (packing mud, snow, ice; nailing planks)."""
    pp = _voorpoten(bt)
    if not pp:
        return None
    a, b = pp
    bones = {a: {"rotation": kf((0, [-90, 0, 0]), (0.25, [-30, 0, 0]), (0.5, [-90, 0, 0]))},
             b: {"rotation": kf((0, [-30, 0, 0]), (0.25, [-90, 0, 0]), (0.5, [-30, 0, 0]))}}
    if not a.startswith("leg"):
        for n, z in ((a, 20), (b, -20)):
            bones[n]["rotation"] = {t: [v[0], 0, z] for t, v in bones[n]["rotation"].items()}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [16, 0, 0]))}
    return {"loop": True, "animation_length": 0.5, "bones": bones}


def bouw_klim(bt):
    """Climbing / reaching up: arms over the head in turn (hanging rope bridges, a tree house)."""
    pp = _voorpoten(bt)
    if not pp:
        return None
    a, b = pp
    bones = {a: {"rotation": kf((0, [-160, 0, 10]), (0.5, [-110, 0, 10]), (1.0, [-160, 0, 10]))},
             b: {"rotation": kf((0, [-110, 0, -10]), (0.5, [-160, 0, -10]), (1.0, [-110, 0, -10]))}}
    w = _wortel(bt)
    bones[w] = {"position": kf((0, [0, 0, 0]), (0.5, [0, 0.8, 0]), (1.0, [0, 0, 0]))}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [-20, 0, 0]))}
    return {"loop": True, "animation_length": 1.0, "bones": bones}


def bouw_plant(bt):
    """Planting: head down to the ground, a little pat, back up with a happy wiggle (seedlings, flowers, reed)."""
    if "head" not in bt:
        return None
    bones = {"head": {"rotation": kf((0, [0, 0, 0]), (0.5, [40, 0, 0]), (1.1, [40, 0, 0]), (1.4, [0, 0, 0]),
                                     (1.6, [0, 0, 10]), (1.8, [0, 0, -10]), (2.0, [0, 0, 0]))}}
    for nek in ("hals", "neck"):
        if nek in bt:
            bones[nek] = {"rotation": kf((0, [0, 0, 0]), (0.5, [45, 0, 0]), (1.1, [45, 0, 0]), (1.4, [0, 0, 0]))}
    pp = _voorpoten(bt)
    if pp and pp[0].startswith(("leg_front", "arm")):
        bones[pp[0]] = {"rotation": kf((0, [0, 0, 0]), (0.6, [-30, 0, 0]), (0.75, [-10, 0, 0]), (0.9, [-30, 0, 0]),
                                       (1.05, [-10, 0, 0]), (1.4, [0, 0, 0]))}
    return {"loop": True, "animation_length": 2.0, "bones": bones}


def bouw_staartklap(bt):
    """The beaver's tail slapping down on the mud of the dam."""
    if "tail" not in bt:
        return None
    w = _wortel(bt)
    bones = {"tail": {"rotation": kf((0, [0, 0, 0]), (0.35, [-55, 0, 0]), (0.45, [12, 0, 0]), (0.55, [0, 0, 0]), (1.0, [0, 0, 0]))},
             w: {"rotation": kf((0, [0, 0, 0]), (0.35, [4, 0, 0]), (0.45, [-2, 0, 0]), (0.55, [0, 0, 0]))}}
    return {"loop": True, "animation_length": 1.0, "bones": bones}


def bouw_tik(bt):
    """Many arms busy at once (the octopus): every arm waving in turn, tips curling."""
    armen = sorted(n for n in bt if n.startswith("arm_") and not n.endswith("_tip"))
    if len(armen) < 4:
        return bouw_klop(bt)
    bones = {}
    for i, n in enumerate(armen):
        f = (i % 4) * 0.2
        bones[n] = {"rotation": kf((0, [0, 0, 0]), (f + 0.2, [-35, 0, 0]), (f + 0.4, [0, 0, 0]), (1.6, [0, 0, 0]))}
        if n + "_tip" in bt:
            bones[n + "_tip"] = {"rotation": kf((0, [0, 0, 0]), (f + 0.3, [-40, 0, 0]), (f + 0.5, [0, 0, 0]), (1.6, [0, 0, 0]))}
    return {"loop": True, "animation_length": 1.6, "bones": bones}


def bouw_wiebel(bt):
    """For a body without paws (the glimworm): a busy wiggle while it lays its threads of light."""
    w = _wortel(bt)
    if w not in bt:
        return None
    bones = {w: {"rotation": kf((0, [0, 0, 0]), (0.3, [0, 0, 12]), (0.6, [0, 0, 0]), (0.9, [0, 0, -12]), (1.2, [0, 0, 0]))}}
    if "head" in bt:
        bones["head"] = {"rotation": kf((0, [0, 0, 0]), (0.3, [10, 0, -8]), (0.9, [10, 0, 8]), (1.2, [0, 0, 0]))}
    return {"loop": True, "animation_length": 1.2, "bones": bones}


BOUW_ANIMS = {"knaag": bouw_knaag, "graaf": bouw_graaf, "sjouw": bouw_sjouw, "stamp": bouw_stamp, "duw": bouw_duw,
              "pik": bouw_pik, "klop": bouw_klop, "klim": bouw_klim, "plant": bouw_plant, "staartklap": bouw_staartklap,
              "tik": bouw_tik, "wiebel": bouw_wiebel}
# soort -> its work animations (design 2.8 table + the build animals of round 29); saved as bouw_<naam>
BOUW = {
    "bever": ["knaag", "sjouw", "staartklap", "graaf"],
    "stokstaartje": ["graaf", "sjouw"], "das": ["graaf", "sjouw"], "wombat": ["graaf", "sjouw"],
    "olifant": ["sjouw", "stamp", "duw"], "mammoet": ["duw", "stamp", "sjouw"],
    "orang_oetan": ["klim", "klop", "sjouw"], "gibbon": ["klim", "klop"],
    "pinguin": ["klop", "sjouw"], "capibara": ["graaf", "sjouw"], "zebra": ["stamp", "sjouw"],
    "koala": ["plant", "klim"], "toekan": ["plant", "pik"],
    "neushoorn": ["duw", "stamp"], "kafferbuffel": ["duw", "stamp"],
    "otter": ["graaf", "klop", "sjouw"], "flamingo": ["plant", "pik"], "kangoeroe": ["sjouw", "klop"],
    "specht": ["pik", "klop"], "octopus": ["tik"], "gekko": ["klop", "klim"], "glimworm": ["wiebel"],
    "prieelvogel": ["pik", "sjouw", "plant"],
}


def voeg_bouw_toe(soort, geo, anims):
    """F10a: the soort's work animations into its animation file (in place); returns the names added."""
    bt = _botten(geo)
    a = anims["animations"]
    walk = a.get("walk")
    nieuw = []
    for naam in BOUW.get(soort, []):
        f = BOUW_ANIMS[naam]
        anim = f(bt, walk) if naam == "sjouw" else f(bt)
        if anim and f"bouw_{naam}" not in a:
            a[f"bouw_{naam}"] = anim
            nieuw.append(f"bouw_{naam}")
    return nieuw


SHOWS = {"brul": brul, "gaap": gaap, "rek": rek, "schud": schud, "snuffel": snuffel, "rondkijk": rondkijk,
         "huppel": huppel, "krab": krab, "pluk": pluk, "schelp": schelp}

# soort -> the extra behaviour animations it gets here (on top of slaap/rust)
EXTRA = {
    "pinguin": ["buikglij"],
    "olifant": ["spuit"], "mammoet": ["spuit"],
    "bever": ["draag"],
    "zebra": ["ren"], "kafferbuffel": ["ren"], "giraffe": ["ren"], "edelhert": ["ren"], "struisvogel": [],   # (struisvogel has its own ren)
}


def voeg_toe(soort, geo, anims, shows=()):
    """Adds slaap, rust, the soort's EXTRA animations and (round 4) its show animations to an animation file (in place);
    returns the names added."""
    bt = _botten(geo)
    a = anims["animations"]
    nieuw = []

    def zet(naam, anim):
        if naam not in a:
            a[naam] = anim
            nieuw.append(naam)

    zet("slaap", slaap(bt))
    zet("rust", rust(bt))
    walk = a.get("walk")
    for e in EXTRA.get(soort, []):
        if e == "buikglij":
            zet(e, buikglij(bt))
        elif e == "spuit":
            zet(e, spuit(bt))
        elif e == "draag" and walk:
            zet(e, draag(bt, walk))
        elif e == "ren" and walk:
            zet(e, ren(walk))
    for naam in shows:
        anim = SHOWS[naam](bt)
        if anim:
            zet(naam, anim)
    return nieuw
