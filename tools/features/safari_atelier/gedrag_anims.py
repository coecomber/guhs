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
