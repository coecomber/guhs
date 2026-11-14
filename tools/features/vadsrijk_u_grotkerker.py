"""
U-GROTKERKER (round U, user decision): every zone cave becomes a REPEATABLE mini-dungeon ("grotkerker") with an end boss and a
personal end chest on every clear.

The caves themselves stay as U-GROTTEN built them (tools/vadsrijk/grotten): the run uses the existing rooms, so no map regen is
needed. This generator reads the cave plan (grot.py builds the 2.5D floor/ceiling grid in memory, no tiles needed) and writes:
  - src/main/java/.../vadsrijk/grotkerker/KerkerData.java (GENERATED): per cave the run rooms (pack spots: centre, radius, floor),
    the boss room, the GATE (the exact air columns of the tunnel into the boss room, closed with barriers during a run), the
    "behind the gate" circles (nobody gets locked in) and the cave box; zone 6 (De Zingende Zoutgrot, built by u-z6wereld's own
    generator) as a fixed entry that the game only switches on when zone 6's Z6WereldPlekken is present;
  - the six cave Pluimguh colours (the rare cave mount): textures recoloured from the pink Pluimguh + egg icons;
  - lang (nl + en): boss names/title cards, cast bar names, run messages, book entries, pets, mount colours, dev commands.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_grotkerker.py
"""
import json
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "grotkerker", "KerkerData.java")
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "grotten"))
sys.path.insert(0, HIER)

# per cave: the two run rooms (kamer names; "koraalhal:w"/":o" = the west/east half of one big room), the boss room, the gate tunnel
# (index into g.gangen: the one that ends in the boss room), levels (mobs, mid-elite, boss)
RUN = {
    "zwamgrot": dict(kamers=["sporenhal", "glimzaal"], baas="elitehol", gang=2, lv=(11, 13), elite=13, baaslv=14),
    # 1.10.3 "Grotere dungeons": zones 2+ have 4 pack rooms + the elite's own den (the last room), a sluis (a second gate that the
    # run shuts until the levers in the sluis room are pulled; gang = the tunnel name in grotplan) and a chest halfway (room 2)
    "kristalgrot": dict(kamers=["kristalhal", "ijsmeer", "galmzaal", "pegelhal", "kristalhol"], baas="elitehol", gang="poort",
                        sluis="sluis", sluiskamer="kristalsluis", lv=(22, 24), elite=24, baaslv=25),
    "wortelgrot": dict(kamers=["wortelvoet", "wortelhal", "knoestzaal", "zwamwortelzaal", "wortelhol"], baas="elitehol", gang="poort",
                       sluis="sluis", sluiskamer="wortelsluis", lv=(44, 46), elite=47, baaslv=48),
    "koraalgrot": dict(kamers=["koraalhal", "koraaltuin", "anemonenzaal", "parelzaal", "parelhol"], baas="elitehol", gang="poort",
                       sluis="sluis", sluiskamer="koraalsluis", lv=(53, 55), elite=55, baaslv=56),
    "wolkengrot": dict(kamers=["blaasgatbodem", "wolkenhal", "donderzaal", "pluiszaal", "grauwhol"], baas="elitehol", gang="poort",
                       sluis="sluis", sluiskamer="wolkensluis", lv=(58, 60), elite=60, baaslv=60),
    # zone 6: rooms 1 + 2 are the two halves of the existing salt hall (zone 6's own generator; spawn spots read from the tiles),
    # the rest is the dungeon wing stamp (grotplan.zoutgrot); the boss room is the existing round chamber (a virtual room there)
    "zoutgrot": dict(kamers=[("hal_west", 3861, 50, -540, 8), ("hal_oost", 3889, 50, -534, 6), "galmzoutzaal", "kristalzoutzaal", "zouthol"],
                     baas="zoutkamer", gang="poort", sluis="sluis", sluiskamer="zoutsluis", lv=(64, 66), elite=66, baaslv=67),
}
# zone 6: De Zingende Zoutgrot (u-z6wereld, tools/vadsrijk/zone6/bouw6.py zoutgrot()): hall centre (ex, 50, hz) with ex = 3874,
# hz = -542 (stand "zoutgrot" = hz + 8), hall radii 22 x 15, the lake at (ex + 8, hz - 7), the elite chamber "zoutgrot_elite"
# (3917, 50, -542). Java re-reads both POIs from Z6WereldPlekken when it exists (and only then switches this cave on).
Z6 = dict(id="zoutgrot", zone=6, doos=(3846, 44, -562, 3932, 66, -518), kamers=[("hal_west", 3861, 50, -540, 8), ("hal_oost", 3889, 50, -534, 6)],
          baas=("baaskamer", 3917, 50, -542, 7), achter=[(3917, 50, -542, 10)], lv=(64, 66), elite=66, baaslv=67)

# the bosses: id -> (nl name, en name, nl subtitle, en subtitle)
BAZEN = {
    "zwamgrot": ("Moeder Glimhoed", "Mother Glowcap", "Ze bewaakt de sporen van De Zwamgrot", "She guards the spores of the Toadstool Grotto"),
    "kristalgrot": ("Tante Tinkelpoot", "Auntie Tinkletoes", "Een kristalspin met een kristalhelder zangstemmetje", "A crystal spider with a crystal-clear little voice"),
    "wortelgrot": ("Ouwe Knoestbuik", "Old Knotbelly", "Hij slaapt al eeuwen tussen de wortels. Nu niet meer.", "He slept for ages between the roots. Not any more."),
    "koraalgrot": ("De Grote Zeurslak", "The Great Moan Snail", "Hij zeurt al sinds vloed. En hij is NIET blij.", "He has been moaning since high tide. And he is NOT happy."),
    "wolkengrot": ("Opperpluis", "High Fluff", "De grootste pluismijt van Moeder Wolkwal", "The biggest fluff mite of Mother Cloudwhale"),
    "zoutgrot": ("Koning Galmschaar", "King Echoclaw", "De zingende zoutkreeft van De Zingende Zoutgrot", "The singing salt crab of the Singing Salt Cave"),
}
GROTNAAM = {"zwamgrot": ("De Zwamgrot", "The Toadstool Grotto"), "kristalgrot": ("De Kristalgrot", "The Crystal Grotto"),
            "wortelgrot": ("De Wortelgrot", "The Root Grotto"), "koraalgrot": ("De Koraalgrot", "The Coral Grotto"),
            "wolkengrot": ("De Wolkengrotten", "The Cloud Caves"), "zoutgrot": ("De Zingende Zoutgrot", "The Singing Salt Cave")}
# the attacks (cast bar names: vadsrijk.vfx.aanval.<id>)
AANVALLEN = {
    "grot_sporenkegel": ("Sporenkegel", "Spore Cone"), "grot_glimschuil": ("Glimschuil", "Glow Shelter"), "grot_sporenregen": ("Sporenregen", "Spore Rain"),
    "grot_rinkelregen": ("Rinkelregen", "Tinkle Rain"), "grot_kristalzang": ("Kristalzang", "Crystal Song"), "grot_draadwaaier": ("Draadwaaier", "Thread Fan"),
    "grot_wortelgreep": ("Wortelgreep", "Root Grip"), "grot_samen_tillen": ("Samen tillen!", "Lift together!"), "grot_knoestkruis": ("Knoestkruis", "Knot Cross"),
    "grot_getij": ("Getij", "Tide"), "grot_bubbelregen": ("Bubbelregen", "Bubble Rain"), "grot_schelpzeur": ("Schelpzeur", "Shell Moan"),
    "grot_pluisbui": ("Pluisbui", "Fluff Shower"), "grot_donderknuffel": ("Donderknuffel", "Thunder Hug"), "grot_wolkenschuil": ("Wolkenschuil", "Cloud Shelter"),
    "grot_zoutzang": ("Zoutzang", "Salt Song"), "grot_galmring": ("Galmring", "Echo Ring"), "grot_kristalschuil": ("Kristalschuil", "Crystal Shelter"),
}
# the cave pets (mini bosses): id -> (nl, en, nl text, en text)
PETS = {
    "glimhoedje": ("Glimhoedje", "Little Glowcap", "Een piepklein Moeder Glimhoedje dat zachtjes licht geeft.", "A tiny Mother Glowcap that softly glows."),
    "tinkeltje": ("Tinkeltje", "Tinkles", "Een kristalspinnetje dat bij elke stap tinkelt.", "A crystal spiderling that tinkles at every step."),
    "knoestje": ("Knoestje", "Knotty", "Een mini-Knoestbuik. Slaapt het liefst.", "A mini Knotbelly. Loves to nap."),
    "zeurtje": ("Zeurtje", "Little Moaner", "Een slakje dat zeurt, maar wel heel schattig.", "A little snail that moans, but very cutely."),
    "pluisje": ("Pluisje", "Fluffy", "Een pluismijtje dat je jas knuffelt.", "A fluff mite that hugs your coat."),
    "galmpje": ("Galmpje", "Little Echo", "Een zoutkreeftje dat zachtjes meezingt.", "A salt crab that softly sings along."),
}
PET_VAN = {"zwamgrot": "glimhoedje", "kristalgrot": "tinkeltje", "wortelgrot": "knoestje", "koraalgrot": "zeurtje", "wolkengrot": "pluisje",
           "zoutgrot": "galmpje"}
# the cave Pluimguh colours (rare cave mount): id -> (rgb, egg (body, dots, ears), nl, en, cave)
PLUIM = {
    "glimzwam": ((150, 232, 176), ((160, 236, 186), (255, 250, 200), (255, 150, 200)), "Glimzwam", "Glowshroom", "zwamgrot"),
    "amethist": ((188, 150, 242), ((196, 160, 246), (255, 255, 255), (255, 170, 210)), "Amethist", "Amethyst", "kristalgrot"),
    "wortelhout": ((198, 144, 96), ((206, 154, 104), (255, 226, 150), (255, 160, 170)), "Wortelhout", "Rootwood", "wortelgrot"),
    "koraal": ((255, 132, 122), ((255, 142, 132), (255, 236, 200), (255, 190, 210)), "Koraal", "Coral", "koraalgrot"),
    "onweer": ((132, 142, 178), ((140, 150, 186), (255, 240, 130), (255, 170, 210)), "Onweer", "Thunderstorm", "wolkengrot"),
    "zoutroze": ((255, 214, 222), ((255, 222, 228), (255, 255, 255), (255, 150, 180)), "Zoutroze", "Salt Pink", "zoutgrot"),
}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ===================================================================================================================== data
def _ellips(k, x, z):
    return math.sqrt(((x - k["cx"]) / k["rx"]) ** 2 + ((z - k["cz"]) / k["rz"]) ** 2)


def _poort(g, gang, k, verboden=None):
    """The gate: walk the tunnel back from the boss room until ~3 blocks outside its edge; take the air columns of the tunnel across
    that spot (2 rows deep). Returns (cells [(x, y0, z, y1)], centre (x, y, z)). 1.10.3: also the sluis (k = the sluis room; then
    `verboden` = the rooms that must be unreachable from k's side with it shut)."""
    import numpy as np
    P = gang["punten"]
    # sample the polyline densely from the boss end backwards
    pts = []
    for (ax, az, aS), (bx, bz, bS) in reversed(list(zip(P[:-1], P[1:]))):
        L = math.hypot(bx - ax, bz - az)
        n = max(1, int(L / 0.25))
        for i in range(n, -1, -1):
            t = i / n
            pts.append((ax + (bx - ax) * t, az + (bz - az) * t, aS + (bS - aS) * t, bx - ax, bz - az))
    marge = 1.0 + 3.0 / min(k["rx"], k["rz"])
    gx = gz = None
    for (x, z, S, dx, dz) in pts:
        if _ellips(k, x, z) >= marge:
            gx, gz, gS, gdx, gdz = x, z, S, dx, dz
            break
    if gx is None:
        raise RuntimeError(f"{g.id}: no gate spot found")
    L = math.hypot(gdx, gdz)
    ux, uz = gdx / L, gdz / L          # along the tunnel (towards the boss room)
    nx, nz = -uz, ux                    # across
    cellen = {}
    for diep in (-0.4, 0.4):
        for s10 in range(-40, 41, 2):
            s = s10 / 10.0
            x, z = int(round(gx + ux * diep + nx * s)), int(round(gz + uz * diep + nz * s))
            i, j = x - g.x0, z - g.z0
            if not (0 <= i < g.X and 0 <= j < g.Z) or np.isnan(g.F[i, j]):
                continue
            if abs(s) > gang["breed"] / 2 + 1.4:
                continue
            y0 = int(math.ceil(g.F[i, j]))
            y1 = int(g.C[i, j])
            cellen[(x, z)] = (x, y0, z, y1)
    if len(cellen) < 3:
        raise RuntimeError(f"{g.id}: gate too small ({len(cellen)})")
    # check: with the gate shut, the boss room must not be reachable from the rest of the cave (4-neighbour walk over the cave
    # columns; a player can't squeeze diagonally between two blocks that only touch at a corner)
    dicht = set(cellen)
    start = (int(round(k["cx"])) - g.x0, int(round(k["cz"])) - g.z0)
    gezien, rij = {start}, [start]
    while rij:
        i, j = rij.pop()
        for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            a, b = i + di, j + dj
            if (a, b) in gezien or not (0 <= a < g.X and 0 <= b < g.Z) or np.isnan(g.F[a, b]) or (a + g.x0, b + g.z0) in dicht:
                continue
            gezien.add((a, b))
            rij.append((a, b))
    if verboden is None:
        verboden = [kk for kk in g.kamers if kk is not k and kk["naam"] != "schatnis"]
    lek = [kk["naam"] for kk in verboden if (int(round(kk["cx"])) - g.x0, int(round(kk["cz"])) - g.z0) in gezien]
    if lek:
        raise RuntimeError(f"{g.id}: the gate leaks into {lek}")
    return sorted(cellen.values()), (int(round(gx)), int(math.ceil(gS)), int(round(gz)))


def _gang(g, sleutel):
    if isinstance(sleutel, int):
        return g.gangen[sleutel]
    for ga in g.gangen:
        if ga.get("naam") == sleutel:
            return ga
    raise RuntimeError(f"{g.id}: no tunnel {sleutel}")


def _plekken(g, k, n=36, rmax=0.8):
    """1.10.3 (live bug "mobs buiten de grot"): the spawn spots of a room: dry, full floor blocks of THIS room with 3 free cells
    above, inside rmax of its normalised radius, no deco on them; spread evenly, at most n. Java only spawns on these."""
    import numpy as np
    uit = []
    for i in range(g.X):
        for j in range(g.Z):
            if int(g.eig[i, j]) != k["i"]:
                continue
            S = g.F[i, j]
            if np.isnan(S) or g.Wt[i, j] > -900 or S != math.ceil(S):
                continue
            x, z = i + g.x0, j + g.z0
            if _ellips(k, x, z) > rmax:
                continue
            y = int(S)
            if not all(g.is_lucht(x, y + d, z) for d in range(3)) or not g.is_vast(x, y - 1, z):
                continue
            uit.append((x, y, z))
    if len(uit) > n:
        stap = len(uit) / n
        uit = [uit[int(i * stap)] for i in range(n)]
    return uit


_TG = None


def _tegel_plekken(cx, cz, r, y, n=36):
    """Spawn spots read from the map tiles (for rooms another generator built: zone 6's salt hall + round chamber): a solid floor
    block at y - 1, three free cells above, within r of (cx, cz)."""
    global _TG
    if _TG is None:
        sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk"))
        import verbind as VB
        _TG = VB._Tegels(VB.UIT)
    T = 64

    def blok(x, yy, z):
        v = _TG.dicht((x // T, z // T))
        return "minecraft:air" if v is None else _TG.pal[int(v[x % T, yy, z % T])]

    def vrij(b):
        n_ = b.split("[")[0]
        return n_ in ("minecraft:air", "minecraft:cave_air") or any(w in b for w in ("carpet", "kristalletjes", "short_dry_grass", "dead_bush"))
    uit = []
    for x in range(int(cx - r), int(cx + r) + 1):
        for z in range(int(cz - r), int(cz + r) + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 > r * r:
                continue
            onder = blok(x, y - 1, z)
            if vrij(onder) or "water" in onder or "slab" in onder or "stairs" in onder or "wall" in onder or "fence" in onder:
                continue
            if all(blok(x, y + d, z) == "minecraft:air" for d in range(3)):
                uit.append((x, y, z))
    if len(uit) > n:
        stap = len(uit) / n
        uit = [uit[int(i * stap)] for i in range(n)]
    return uit


def _kist(g, k):
    """The chest halfway (room 2): the floor spot nearest the room centre with a dry 3x3 floor round it."""
    best = None
    for (x, y, z) in _plekken(g, k, n=100000, rmax=0.7):
        if all(g.stand(x + dx, z + dz) == y and g.is_lucht(x + dx, y, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
            d = math.hypot(x - k["cx"], z - k["cz"])
            if best is None or d < best[0]:
                best = (d, (x, y, z))
    if best is None:
        raise RuntimeError(f"{g.id}: no spot for the chest halfway in {k['naam']}")
    return best[1]


def _doos(g):
    """1.10.3 (live bug "steeds naar de ingang"): the cave box covers ALL cave air and water + 2 (the old one stopped at the
    floor - 2 / the highest ceiling + 1 and missed the lake bed and the pockets under it). The entrance shaft above the highest
    room ceiling is not in it (the surface round it must never count as "in the dungeon"); kerker data's per-column space
    (herbouw.py) is the precise part."""
    import numpy as np
    from grot import LUCHT, ZACHT, ZET
    lucht = np.isin(g.op, (LUCHT, ZACHT))
    wid = g.pidx.get("minecraft:water")
    if wid is not None:
        lucht |= (g.op == ZET) & (g.blk == wid)
    ii, yy, jj = np.nonzero(lucht)
    plafond = int(np.max(g.C[~np.isnan(g.F)]))
    y1 = min(int(yy.max()) + g.y0 + 2, plafond + 2)
    return (int(ii.min()) + g.x0 - 2, int(yy.min()) + g.y0 - 2, int(jj.min()) + g.z0 - 2, int(ii.max()) + g.x0 + 2, y1,
            int(jj.max()) + g.z0 + 2)


def kerkers():
    import grot
    uit = []
    for g in grot.grotten():
        g.bouw()
        r = RUN[g.id]
        kn = {k["naam"]: k for k in g.kamers}
        doos = _doos(g)
        kamers = []
        for n, naam in enumerate(r["kamers"]):
            if isinstance(naam, tuple):   # (zone 6: a fixed spot in a room of zone 6's own generator)
                nm, cx, y, cz, rad = naam
                pl = _tegel_plekken(cx, cz, rad, y)
                if len(pl) < 8:
                    raise RuntimeError(f"{g.id}: room {nm} has only {len(pl)} spawn spots in the tiles")
                kamers.append((f"kamer_{n + 1}", cx, y, cz, rad, pl))
                continue
            basis, _, helft = naam.partition(":")
            k = kn[basis]
            cx, cz, rad = k["cx"], k["cz"], min(k["rx"], k["rz"]) * 0.72
            if helft:   # a half of one big room: centre at a third of the long axis, smaller radius
                cx = k["cx"] + (k["rx"] * 0.45 if helft == "o" else -k["rx"] * 0.45)
                rad = min(k["rx"] * 0.42, k["rz"] * 0.62)
            pl = _plekken(g, k)
            if helft:
                pl = [p for p in pl if math.hypot(p[0] - cx, p[2] - cz) <= rad + 1]
            if len(pl) < 8:
                raise RuntimeError(f"{g.id}: room {naam} has only {len(pl)} spawn spots")
            kamers.append((f"kamer_{n + 1}", int(round(cx)), int(math.ceil(k["S"])), int(round(cz)), max(4, int(round(rad))), pl))
        b = kn[r["baas"]]
        bpl = _tegel_plekken(b["cx"], b["cz"], min(b["rx"], b["rz"]) - 1, int(math.ceil(b["S"]))) if b.get("virtueel") else _plekken(g, b)
        baas = ("baaskamer", int(round(b["cx"])), int(math.ceil(b["S"])), int(round(b["cz"])), int(min(b["rx"], b["rz"])), bpl)
        poort, pc = _poort(g, _gang(g, r["gang"]), b)
        # behind the gate: the boss room (radius + 2, but always 1.5 short of the gate itself, so standing in front of it is not "behind")
        dmin = min(math.hypot(c[0] - b["cx"], c[2] - b["cz"]) for c in poort)
        achter = [(int(round(b["cx"])), int(math.ceil(b["S"])), int(round(b["cz"])), int(min(max(b["rx"], b["rz"]) + 2, dmin - 1.5)))]
        if "schatnis" in kn:
            s = kn["schatnis"]
            achter.append((int(round(s["cx"])), int(math.ceil(s["S"])), int(round(s["cz"])), 7))
        # the tunnel from the boss room on to the treasure nook
        for ga in g.gangen:
            p0, p1 = ga["punten"][0], ga["punten"][-1]
            if _ellips(b, p0[0], p0[1]) <= 1.2 and "schatnis" in kn and _ellips(kn["schatnis"], p1[0], p1[1]) <= 1.6:
                for (x, z, S) in ga["punten"]:
                    achter.append((int(round(x)), int(math.ceil(S)), int(round(z)), 4))
        sluis, sbord, kist = [], None, None
        if "sluis" in r:
            sk = kn[r["sluiskamer"]]
            achter_sluis = [kn[n_] for n_ in r["kamers"][2:] if not isinstance(n_, tuple)] + [b]
            sluis, sbord = _poort(g, _gang(g, r["sluis"]), sk, verboden=achter_sluis)
            k2 = r["kamers"][1]
            if isinstance(k2, tuple):
                kist = min(kamers[1][5], key=lambda p: math.hypot(p[0] - k2[1], p[2] - k2[3]))
            else:
                kist = _kist(g, kn[k2])
            if len(g.hendels) < 2:
                raise RuntimeError(f"{g.id}: the sluis room needs levers")
        if g.id == "zoutgrot":   # (+ the salt hall, stair foot and chamber of zone 6's own generator)
            doos = tuple(min(a, b_) for a, b_ in zip(doos[:3], Z6["doos"][:3])) + tuple(max(a, b_) for a, b_ in zip(doos[3:], Z6["doos"][3:]))
        uit.append(dict(id=g.id, zone=g.zone, doos=doos, kamers=kamers, baas=baas, poort=poort, bord=pc, achter=achter,
                        lv=r["lv"], elite=r["elite"], baaslv=r["baaslv"], sluis=sluis, sbord=sbord, hendels=list(g.hendels), kist=kist))
    return uit


def java(ks):
    def pos(p):
        return "null" if p is None else f"new BlockPos({p[0]}, {p[1]}, {p[2]})"

    def kamer(k):
        pl = ", ".join(f"{x}, {y}, {z}" for (x, y, z) in k[5])
        return f'new Kamer("{k[0]}", {k[1]}, {k[2]}, {k[3]}, {k[4]}, new int[] {{{pl}}})'

    def kolommen(cs):
        return "List.of(" + ", ".join(f"new int[] {{{x}, {a}, {z}, {b}}}" for (x, a, z, b) in cs) + ")"
    regels = []
    for d in ks:
        x0, y0, z0, x1, y1, z1 = d["doos"]
        kamers = ",\n                        ".join(kamer(k) for k in d["kamers"])
        achter = ", ".join(f"new int[] {{{x}, {y}, {z}, {r}}}" for (x, y, z, r) in d["achter"])
        hendels = ", ".join(pos(h) for h in d["hendels"])
        regels.append(f'            new Kerker("{d["id"]}", {d["zone"]}, {x0}, {y0}, {z0}, {x1}, {y1}, {z1}, {d["lv"][0]}, {d["lv"][1]}, {d["elite"]}, '
                      f'{d["baaslv"]},\n                    List.of({kamers}),\n                    {kamer(d["baas"])},\n'
                      f'                    {kolommen(d["poort"])},\n                    {pos(d["bord"])},\n                    List.of({achter}),\n'
                      f'                    {kolommen(d["sluis"])}, {pos(d["sbord"])},\n                    List.of({hendels}), {pos(d["kist"])})')
    src = f"""package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/features/vadsrijk_u_grotkerker.py from tools/vadsrijk/grotten/grotplan.py (do not edit by hand). The six
 * cave runs (U-GROTKERKER): per cave its box, the pack rooms (centre stand position + radius + the spawn spots {{x, y, z, ...}}),
 * the boss room, the gate columns ({{x, y0, z, y1}}: air from y0 up to y1 - 1 in the tunnel into the boss room; empty = no gate),
 * where the gate board hangs, and the "behind the gate" circles ({{x, y, z, r}}: a gate never closes on anyone there). 1.10.3
 * "Grotere dungeons" (zones 2+): 4 pack rooms + the elite's den (the last room), the sluis columns + its board, the levers in
 * the sluis room and the spot of the chest halfway. Zone 6 is switched on in {{@link Kerkers}} only when u-z6wereld's
 * Z6WereldPlekken is present. The per-column cave space lives in data/guhs/vadsrijk/grotkerker/&lt;id&gt;.json ({{@link Kerkers#ruimte}}).
 */
public final class KerkerData {{
    /** A room: id, stand centre, fight radius, and its spawn spots (x, y, z triples; empty = random within r). */
    public record Kamer(String id, int x, int y, int z, int r, int[] plekken) {{
        public Kamer(String id, int x, int y, int z, int r) {{
            this(id, x, y, z, r, new int[0]);
        }}

        public BlockPos pos() {{
            return new BlockPos(x, y, z);
        }}

        public int aantalPlekken() {{
            return plekken.length / 3;
        }}

        public BlockPos plek(int i) {{
            return new BlockPos(plekken[i * 3], plekken[i * 3 + 1], plekken[i * 3 + 2]);
        }}

        public boolean binnen(double px, double py, double pz, double extra) {{
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return dx * dx + dz * dz <= (r + extra) * (r + extra) && py > y - 4 && py < y + 12;
        }}
    }}

    public record Kerker(String id, int zone, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int minLv, int maxLv, int eliteLv,
            int baasLv, List<Kamer> kamers, Kamer baas, List<int[]> poort, BlockPos bord, List<int[]> achter, List<int[]> sluis, BlockPos sluisBord,
            List<BlockPos> hendels, BlockPos kist) {{
        /** The 1.10.2 shape (two rooms, no sluis): the Zwamgrot and the gametests' stand-in caves. */
        public Kerker(String id, int zone, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int minLv, int maxLv, int eliteLv, int baasLv,
                List<Kamer> kamers, Kamer baas, List<int[]> poort, BlockPos bord, List<int[]> achter) {{
            this(id, zone, minX, minY, minZ, maxX, maxY, maxZ, minLv, maxLv, eliteLv, baasLv, kamers, baas, poort, bord, achter, List.of(), null, List.of(), null);
        }}

        /** In the cave box AND (when the cave has its generated per-column space) in the cave's real space. */
        public boolean bevat(double x, double y, double z) {{
            if (!(x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1 && y >= minY && y < maxY + 1)) {{
                return false;
            }}
            Kerkers.Ruimte r = Kerkers.ruimte(this);
            return r == null || r.bevat(x, y, z);
        }}

        /** 1.10.3: the big layout (4 pack rooms + the elite's den, a sluis, a chest halfway). */
        public boolean groot() {{
            return kamers.size() > 2;
        }}

        /** Behind the gate (the boss room, the treasure nook and the tunnel between)? */
        public boolean achter(double x, double y, double z) {{
            for (int[] c : achter) {{
                double dx = x - (c[0] + 0.5), dz = z - (c[2] + 0.5);
                if (dx * dx + dz * dz <= (double) c[3] * c[3] && y > c[1] - 4 && y < c[1] + 12) {{
                    return true;
                }}
            }}
            return false;
        }}
    }}

    public static final List<Kerker> ALLE = List.of(
{(","+chr(10)).join(regels)});

    private KerkerData() {{
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)


# ===================================================================================================================== assets
def assets():
    """The six cave Pluimguh colours: textures recoloured from the pink one (t-rijdier's mask) + egg icons."""
    import numpy as np
    from PIL import Image
    try:
        import vadsrijk_t_rijdier as TR
        import vadsrijk_rijdieren as RD
    except ImportError:
        from features import vadsrijk_t_rijdier as TR
        from features import vadsrijk_rijdieren as RD
    tex = os.path.join(A, "textures", "entity")
    bron = os.path.join(tex, "vr_eco_pluimguh_roze.png")
    n = 0
    if os.path.exists(bron):
        arr = np.asarray(Image.open(bron).convert("RGBA"))
        mk = TR.masker("pluimguh", arr)
        for k, (kleur, _, _, _, _) in PLUIM.items():
            Image.fromarray(TR.herkleur(arr, mk, kleur)).save(os.path.join(tex, f"vr_eco_pluimguh_{k}.png"))
            n += 1
    for k, (_, ei, _, _, _) in PLUIM.items():
        p = os.path.join(A, "textures", "item", RD.ITEM_TEX, f"vr_item_pluimei_{k}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        RD.ei(*ei).png(p)
        _schrijf(os.path.join(A, "models", "item", f"vr_item_pluimei_{k}.json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{RD.ITEM_TEX}/vr_item_pluimei_{k}"}})
        _schrijf(os.path.join(A, "items", f"vr_item_pluimei_{k}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_item_pluimei_{k}"}})
    return n


# ===================================================================================================================== texts
def teksten():
    """{key: (nl, en)}"""
    t = {}
    K = "vadsrijk.grotkerker."
    for i, (nl, en, onl, oen) in BAZEN.items():
        t[f"entity.guhs.vr_grot_baas_{i}"] = (nl, en)
        t[K + f"baas.{i}"] = (nl, en)
        t[K + f"baas.{i}.onder"] = (onl, oen)
        gn = GROTNAAM[i]
        t[f"vadsrijk.grotten.boek.baas.{i}"] = (f"Grotbaas van {gn[0].replace('De ', 'de ')} verslagen", f"Cave boss of {gn[1].replace('The ', 'the ')} defeated")
        t[K + f"grot.{i}"] = gn
    for k, v in AANVALLEN.items():
        t[f"vadsrijk.vfx.aanval.{k}"] = v
    for p, (nl, en, tnl, ten) in PETS.items():
        t[f"vadsrijk.huisdier.{p}.naam"] = (nl, en)
        t[f"vadsrijk.huisdier.{p}.tekst"] = (tnl, ten)
    for g, p in PET_VAN.items():
        gn = GROTNAAM[g]
        t[f"vadsrijk.huisdier.{p}.bron"] = (f"Zeldzaam uit de eindkist van {gn[0].replace('De ', 'de ')}.", f"Rare from the end chest of {gn[1].replace('The ', 'the ')}.")
    E = "vadsrijk.economie."
    for k, (_, _, nl, en, g) in PLUIM.items():
        gn = GROTNAAM[g]
        t[E + f"pluimkleur.{k}"] = (nl, en)
        t[f"item.guhs.vr_item_pluimei_{k}"] = (f"Pluimguh-ei ({nl})", f"Pluimguh Egg ({en})")
        t[E + f"pluimkleur.bron.{k}"] = (f"Zeldzaam (10%, zeker na 15 keer) uit de eindkist van {gn[0].replace('De ', 'de ')}.",
                                          f"Rare (10%, sure after 15 tries) from the end chest of {gn[1].replace('The ', 'the ')}.")
    t.update({
        K + "start": ("⚔ De grot wordt wakker! Versla de grotbewoners in beide grotkamers, dan gaat de grotpoort open.",
                      "⚔ The cave wakes up! Defeat the cave dwellers in both cave rooms and the cave gate opens."),
        K + "start.groep": ("(%s spelers: meer grotbewoners, een sterkere baas)", "(%s players: more cave dwellers, a tougher boss)"),
        K + "meedoen": ("⚔ Je doet mee met de run die al bezig is in %s.", "⚔ You join the run already going on in %s."),
        K + "kamer": ("✔ Grotkamer %s is leeg! (%s/%s)", "✔ Cave room %s is clear! (%s/%s)"),
        K + "elite": ("✔ De elite is verslagen!", "✔ The elite is defeated!"),
        K + "poort.open": ("De grotpoort gaat open... de grotbaas wacht!", "The cave gate opens... the cave boss is waiting!"),
        K + "poort.dicht": ("Grotpoort", "Cave gate"),
        K + "poort.nog": ("Versla eerst de grotbewoners: %s over", "Defeat the cave dwellers first: %s left"),
        K + "poort.wacht": ("Er is nog iemand achter de poort", "Someone is still behind the gate"),
        K + "poort.klaar": ("Open! Volgende run over %s", "Open! Next run in %s"),
        K + "poort.botst": ("De grotpoort zit dicht. Versla eerst de grotbewoners!", "The cave gate is shut. Defeat the cave dwellers first!"),
        K + "verslagen": ("★ %s verslagen! Je eindkist staat klaar.", "★ %s defeated! Your end chest is ready."),
        K + "verslagen.geen": ("★ %s verslagen! (Te weinig meegeholpen voor een eindkist: meer dan 5%% nodig.)",
                               "★ %s defeated! (Helped too little for an end chest: more than 5%% needed.)"),
        K + "reset.straks": ("De grot slaapt over %s weer in. Daarna kun je opnieuw!", "The cave falls asleep again in %s. Then you can go again!"),
        K + "reset": ("De grot is weer in slaap gevallen: een nieuwe run kan beginnen.", "The cave fell asleep again: a new run can start."),
        K + "reset.leeg": ("Iedereen is weg: de grot valt weer in slaap.", "Everyone left: the cave falls asleep again."),
        K + "titel.klaar": ("Grot uitgespeeld!", "Cave cleared!"),
        K + "titel.klaar.onder": ("%s keer uitgespeeld", "cleared %s times"),
        K + "zeldzaam": ("✦ Zeldzaam! %s", "✦ Rare! %s"),
        K + "pet.mis": ("Geen huisdiertje dit keer (%s%%)", "No pet this time (%s%%)"),
        K + "boek.hint.baas": ("Speel de grotkerker van %s uit: versla de grotbaas.", "Clear the cave dungeon of %s: defeat the cave boss."),
        K + "boek.cat6": ("Grotten", "Caves"),
        K + "bord.titel": ("Grotkerker", "Cave dungeon"),
        K + "bijroep": ("%s roept hulp uit de grot!", "%s calls for help from the cave!"),
        K + "terug": ("%s springt terug naar het midden!", "%s jumps back to the middle!"),
        # 1.10.3 "Grotere dungeons": the big caves (4 rooms, the sluis + levers, the chest halfway)
        "vadsrijk.grotkerker.start.groot": ("⚔ De grot wordt wakker! Vier grotkamers, een sluis met hendels en een elite staan tussen jou en de grotbaas.", "⚔ The cave wakes up! Four cave rooms, a sluice gate with levers and an elite stand between you and the cave boss."),
        "vadsrijk.grotkerker.sluis.dicht": ("Sluis", "Sluice gate"),
        "vadsrijk.grotkerker.sluis.nog": ("Haal de hendels in de sluiszaal over: %s/%s", "Pull the levers in the sluice room: %s/%s"),
        "vadsrijk.grotkerker.sluis.hendel": ("Hendel overgehaald! (%s/%s)", "Lever pulled! (%s/%s)"),
        "vadsrijk.grotkerker.sluis.open": ("De sluis gaat open! Op naar de diepere grotkamers.", "The sluice gate opens! On to the deeper cave rooms."),
        "vadsrijk.grotkerker.sluis.botst": ("De sluis zit dicht. Haal eerst alle hendels in de sluiszaal over!", "The sluice gate is shut. Pull every lever in the sluice room first!"),
        "vadsrijk.grotkerker.tussenkist": ("★ Halverwege! Er staat een tussenkist voor je klaar.", "★ Halfway there! A chest is waiting for you."),
        "vadsrijk.grotkerker.venster.tussenkist": ("• Halverwege (na grotkamer 2): een tussenkist met %s brons, een uitrustingsstuk en erts", "• Halfway (after cave room 2): a chest with %s bronze, a piece of gear and ore"),
        # 1.10.3 "pech zichtbaar": a pet miss says how far the sure pet is
        "vadsrijk.grotkerker.pet.mis.pech": ("Geen huisdier %s deze keer (%s%%) · pech %s/%s · nog %s keer tot een zekere drop", "No pet %s this time (%s%%) · pity %s/%s · %s more tries until a sure drop"),
        "vadsrijk.grotkerker.pet.mis.zeker": ("Geen huisdier %s deze keer (%s%%) · pech %s/%s · de volgende keer is hij zeker van jou!", "No pet %s this time (%s%%) · pity %s/%s · next time it's guaranteed yours!"),

    })
    return t


def bestanden():
    ks = kerkers()
    java(ks)
    n = assets()
    return ks, n


def build(h):
    bestanden()
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)   # (the shared helper takes en first)


def _direct():
    ks, n = bestanden()
    tk = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            regels = f.read().split("\n")
        data = json.loads("\n".join(regels))
        nieuw = [(k, v[i]) for k, v in tk.items() if data.get(k) != v[i]]
        bestaand = {k for k, _ in nieuw if k in data}
        uit = []
        for r in regels:
            s = r.strip()
            if s.startswith('"') and '":' in s:
                k = json.loads(s.split('":', 1)[0] + '"')
                if k in bestaand:
                    continue
            uit.append(r)
        while uit and uit[-1].strip() == "":
            uit.pop()
        assert uit[-1].strip() == "}"
        uit.pop()
        if nieuw:
            if not uit[-1].rstrip().endswith(","):
                uit[-1] = uit[-1].rstrip() + ","
            for m, (k, v) in enumerate(nieuw):
                uit.append(f"  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}" + ("," if m < len(nieuw) - 1 else ""))
        uit.append("}")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(uit) + "\n")
        json.loads(open(p, encoding="utf-8").read())
    for d in ks:
        print(f"  {d['id']}: kamers {d['kamers']}, baas {d['baas']}, poort {len(d['poort'])} kolommen @ {d['bord']}")
    print(f"vadsrijk_u_grotkerker: {len(ks)} caves, {n} pluimguh textures, {len(tk)} texts")


if __name__ == "__main__":
    _direct()
