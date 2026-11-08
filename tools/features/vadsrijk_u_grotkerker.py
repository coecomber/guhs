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
    "kristalgrot": dict(kamers=["kristalhal", "ijsmeer"], baas="elitehol", gang=3, lv=(22, 24), elite=24, baaslv=25),
    "wortelgrot": dict(kamers=["wortelvoet", "wortelhal"], baas="elitehol", gang=2, lv=(44, 46), elite=47, baaslv=48),
    "koraalgrot": dict(kamers=["koraalhal:o", "koraalhal:w"], baas="elitehol", gang=3, lv=(53, 55), elite=55, baaslv=56),
    "wolkengrot": dict(kamers=["blaasgatbodem", "wolkenhal"], baas="elitehol", gang=2, lv=(58, 60), elite=60, baaslv=60),
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


def _poort(g, gang, k):
    """The gate: walk the tunnel back from the boss room until ~3 blocks outside its edge; take the air columns of the tunnel across
    that spot (2 rows deep). Returns (cells [(x, y0, z, y1)], centre (x, y, z))."""
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
    lek = [kk["naam"] for kk in g.kamers if kk is not k and kk["naam"] != "schatnis"
           and (int(round(kk["cx"])) - g.x0, int(round(kk["cz"])) - g.z0) in gezien]
    if lek:
        raise RuntimeError(f"{g.id}: the gate leaks into {lek}")
    return sorted(cellen.values()), (int(round(gx)), int(math.ceil(gS)), int(round(gz)))


def kerkers():
    import grot
    import numpy as np
    uit = []
    for g in grot.grotten():
        g.bouw()
        r = RUN[g.id]
        kn = {k["naam"]: k for k in g.kamers}
        # the cave box (same as vadsrijk_u_grotten: rooms + tunnels, the shaft above the highest ceiling not included)
        F = g.F
        ii, jj = np.nonzero(~np.isnan(F))
        x0, x1 = int(ii.min()) + g.x0 - 1, int(ii.max()) + g.x0 + 1
        z0, z1 = int(jj.min()) + g.z0 - 1, int(jj.max()) + g.z0 + 1
        y0 = int(math.ceil(np.nanmin(F))) - 2
        y1 = int(np.max(g.C[~np.isnan(F)])) + 1
        for t in g.trappen:
            x0, x1 = min(x0, int(t["cx"]) - 4), max(x1, int(t["cx"]) + 4)
            z0, z1 = min(z0, int(t["cz"]) - 4), max(z1, int(t["cz"]) + 4)
        kamers = []
        for n, naam in enumerate(r["kamers"]):
            basis, _, helft = naam.partition(":")
            k = kn[basis]
            cx, cz, rad = k["cx"], k["cz"], min(k["rx"], k["rz"]) * 0.72
            if helft:   # a half of one big room: centre at a third of the long axis, smaller radius
                cx = k["cx"] + (k["rx"] * 0.45 if helft == "o" else -k["rx"] * 0.45)
                rad = min(k["rx"] * 0.42, k["rz"] * 0.62)
            kamers.append((f"kamer_{n + 1}", int(round(cx)), int(math.ceil(k["S"])), int(round(cz)), max(4, int(round(rad)))))
        b = kn[r["baas"]]
        baas = ("baaskamer", int(round(b["cx"])), int(math.ceil(b["S"])), int(round(b["cz"])), int(min(b["rx"], b["rz"])))
        poort, pc = _poort(g, g.gangen[r["gang"]], b)
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
        uit.append(dict(id=g.id, zone=g.zone, doos=(x0, y0, z0, x1, y1, z1), kamers=kamers, baas=baas, poort=poort, bord=pc, achter=achter,
                        lv=r["lv"], elite=r["elite"], baaslv=r["baaslv"]))
    uit.append(dict(id=Z6["id"], zone=6, doos=Z6["doos"], kamers=Z6["kamers"], baas=Z6["baas"], poort=[], bord=None, achter=Z6["achter"],
                    lv=Z6["lv"], elite=Z6["elite"], baaslv=Z6["baaslv"]))
    return uit


def java(ks):
    def kamer(k):
        return f'new Kamer("{k[0]}", {k[1]}, {k[2]}, {k[3]}, {k[4]})'
    regels = []
    for d in ks:
        x0, y0, z0, x1, y1, z1 = d["doos"]
        kamers = ", ".join(kamer(k) for k in d["kamers"])
        poort = ", ".join(f"new int[] {{{x}, {a}, {z}, {b}}}" for (x, a, z, b) in d["poort"])
        achter = ", ".join(f"new int[] {{{x}, {y}, {z}, {r}}}" for (x, y, z, r) in d["achter"])
        bord = "null" if d["bord"] is None else f"new BlockPos({d['bord'][0]}, {d['bord'][1]}, {d['bord'][2]})"
        regels.append(f'            new Kerker("{d["id"]}", {d["zone"]}, {x0}, {y0}, {z0}, {x1}, {y1}, {z1}, {d["lv"][0]}, {d["lv"][1]}, {d["elite"]}, '
                      f'{d["baaslv"]},\n                    List.of({kamers}),\n                    {kamer(d["baas"])},\n'
                      f'                    List.of({poort}),\n                    {bord},\n                    List.of({achter}))')
    src = f"""package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/features/vadsrijk_u_grotkerker.py from tools/vadsrijk/grotten/grotplan.py (do not edit by hand). The six
 * cave runs (U-GROTKERKER): per cave its box, the two pack rooms (centre stand position + spawn radius), the boss room, the gate
 * columns ({{x, y0, z, y1}}: air from y0 up to y1 - 1 in the tunnel into the boss room; empty = no gate), where the gate board
 * hangs, and the "behind the gate" circles ({{x, y, z, r}}: a gate never closes on anyone there). Zone 6 is switched on in
 * {{@link Kerkers}} only when u-z6wereld's Z6WereldPlekken is present.
 */
public final class KerkerData {{
    /** A room: id, stand centre, spawn/fight radius. */
    public record Kamer(String id, int x, int y, int z, int r) {{
        public BlockPos pos() {{
            return new BlockPos(x, y, z);
        }}

        public boolean binnen(double px, double py, double pz, double extra) {{
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return dx * dx + dz * dz <= (r + extra) * (r + extra) && py > y - 4 && py < y + 12;
        }}
    }}

    public record Kerker(String id, int zone, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int minLv, int maxLv, int eliteLv,
            int baasLv, List<Kamer> kamers, Kamer baas, List<int[]> poort, BlockPos bord, List<int[]> achter) {{
        public boolean bevat(double x, double y, double z) {{
            return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1 && y >= minY && y < maxY + 1;
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
