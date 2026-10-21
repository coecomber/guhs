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


# soort -> the extra behaviour animations it gets here (on top of slaap/rust)
EXTRA = {
    "pinguin": ["buikglij"],
    "olifant": ["spuit"], "mammoet": ["spuit"],
    "bever": ["draag"],
    "zebra": ["ren"], "kafferbuffel": ["ren"], "giraffe": ["ren"], "edelhert": ["ren"], "struisvogel": [],   # (struisvogel has its own ren)
}


def voeg_toe(soort, geo, anims):
    """Adds slaap, rust and the soort's EXTRA animations to an animation file (in place); returns the names added."""
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
    return nieuw
