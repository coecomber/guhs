"""
W-GROT (round W, user request): "Caves hebben echt een dichte deur en een interface om de 'dungeon' te starten, te queuen met andere
spelers etc."

Every cave dungeon (grotkerker, U-GROTKERKER) gets a real, CLOSED door at its entrance. You cannot walk in: right-clicking the door
opens the dungeon interface (solo / with your party / the queue), and the group is teleported in. This generator:
  - computes the door cells per cave from the cave plan (tools/vadsrijk/grotten/grotplan.py: the exact openings the entrance
    structures carve) and zone 6's stair (tools/vadsrijk/zone6/bouw6.py zoutgrot()), plus the stand spot in front of the door
    ("voor", where you come out) and the spot just inside ("binnen", where the group is teleported in);
  - verifies them against the generated map tiles when they are there (every door cell is open now, both stand spots are walkable:
    a floor + 2 free cells, and with the door shut the inside is not reachable through the air from the stand spot in a window
    round the door); ``--check`` only prints that report;
  - writes src/main/java/.../vadsrijk/grotkerker/GrotDeurData.java (GENERATED);
  - paints the door textures (6 themes x panel / knocker / hatch) + block models + the blockstate of guhs:vr_grotdeur;
  - lang (nl; en copies the Dutch for the later English pass, as the round-W brief allows).

The doors are placed by the GAME (GrotDeur, the first time a door's chunk is loaded), only in open cells, so no map regen is needed.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_w_grot.py [--check]
"""
import json
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "grotkerker", "GrotDeurData.java")
BLOK = "vr_grotdeur"
THEMAS = ["zwam", "kristal", "wortel", "koraal", "wolk", "zout"]


# ===================================================================================================================== doors
def _ring(cx, cz, r0, r1, hoek, y0, y1):
    """The cells of a round entrance wall's door wedge (grotplan's `deur` test: |angle - hoek| < 0.42, r0 < r <= r1)."""
    uit = []
    for x in range(int(cx) - 7, int(cx) + 8):
        for z in range(int(cz) - 7, int(cz) + 8):
            r = math.hypot(x - cx, z - cz)
            a = math.atan2(z - cz, x - cx) - hoek
            if r0 < r <= r1 and abs(math.atan2(math.sin(a), math.cos(a))) < 0.42:
                for y in range(y0, y1 + 1):
                    uit.append((x, y, z))
    return uit


def _boog(cx, cz, richting, S, breed, hoog, d):
    """The OPENING cells of grotplan.boog() on plane d (the arch frame round a tunnel mouth)."""
    dx, dz = richting
    L = math.hypot(dx, dz)
    dx, dz = dx / L, dz / L
    nx, nz = -dz, dx
    a = int(math.ceil(S))
    half = breed / 2.0
    uit = []
    for s in range(-int(half) - 2, int(half) + 3):
        for y in range(a - 1, a + hoog + 2):
            x, z = round(cx + dx * d + nx * s), round(cz + dz * d + nz * s)
            hy = (y - a + 0.5) / hoog
            if y >= a and abs(s) <= half and (hy < 0.75 or (abs(s) / (half + 0.5)) ** 2 + ((hy - 0.75) / 0.6) ** 2 < 1):
                uit.append((x, y, z))
    return uit


def _luik(cx, cz, y, r):
    """A hatch: every cell of the shaft disc at height y (the 3x3 pillar in the middle stays)."""
    uit = []
    for x in range(int(cx) - 5, int(cx) + 6):
        for z in range(int(cz) - 5, int(cz) + 6):
            if math.hypot(x - cx, z - cz) <= r and max(abs(x - cx), abs(z - cz)) > 1.01:
                uit.append((x, y, z))
    return uit


def _vlak(x, seed, dz, y0, y1, reserve):
    """The open cells of the plane x = const that hang together with `seed` (|z - seed z| <= dz, y0..y1): the real opening of a
    tunnel mouth in the tiles (the carved tunnel is wider than grotplan's arch). Without tiles: `reserve` (the plan's opening)."""
    open_ = _open_fn()
    if open_ is None or open_(*seed) is None:
        return reserve
    gezien, rij = {seed}, [seed]
    while rij:
        c = rij.pop()
        for (a, b) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x, c[1] + a, c[2] + b)
            if n in gezien or not (y0 <= n[1] <= y1) or abs(n[2] - seed[2]) > dz or not open_(*n):
                continue
            gezien.add(n)
            rij.append(n)
    if len(gezien) >= (y1 - y0 + 1) * (2 * dz + 1):
        raise RuntimeError(f"vlak x={x} rond {seed}: de opening is niet dicht te krijgen (geen rand gevonden)")
    return sorted(gezien)


_OPEN = []


def _open_fn():
    """A cached 'is this cell open in the tiles' function (None when the tools/tiles are not there)."""
    if _OPEN:
        return _OPEN[0]
    try:
        sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk"))
        import loopcheck as L
    except ImportError:
        _OPEN.append(None)
        return None
    cache = {}

    def open_(x, y, z):
        t = (x // 64, z // 64)
        if t not in cache:
            p = os.path.join(L.TEGELS, f"t_{t[0]}_{t[1]}.nbt")
            if not os.path.exists(p):
                cache[t] = None
            else:
                pal, xs, ys, zs, st = L.lees_tegel(p)
                kl = [L.klasse(n, pp) for n, pp in pal]
                cache[t] = ({(int(a), int(b), int(c)): int(s_) for a, b, c, s_ in zip(xs, ys, zs, st)}, kl)
        if cache[t] is None:
            return None
        s_ = cache[t][0].get((x % 64, y, z % 64))
        return s_ is None or cache[t][1][s_] in (L.PASS, L.CLIMB)
    _OPEN.append(open_)
    return open_


def _muur2(x, z0, z1, y0, y1):
    return [(x, y, z) for z in range(z0, z1 + 1) for y in range(y0, y1 + 1)]


def _muur(x0, x1, y0, y1, z):
    return [(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)]


def deuren():
    """Every door: cave, main door?, theme, cells, voor (stand spot outside), binnen (spot just inside), luik (a hatch)."""
    D = []
    # zone 1, De Zwamgrot: the giant toadstool's door (east), 3 wide x 3 high, in the stem wall
    D.append(dict(grot="zwamgrot", hoofd=True, thema="zwam", cellen=_ring(-419, 148, 3.6, 4.7, 0.0, 69, 71),
                  voor=(-412, 69, 148), binnen=(-417, 69, 148)))
    # zone 2, De Kristalgrot: the tunnel behind the crystal arch on the west flank of the Galmtop. Not IN the arch: the mouth is a
    # roofless notch in the cliff for its first blocks (you could climb round an arch door); 6 blocks in, the tunnel is closed
    D.append(dict(grot="kristalgrot", hoofd=True, thema="kristal", cellen=_vlak(872, (872, 120, -1106), 5, 117, 124,
                                                                                  _muur2(872, -1108, -1104, 118, 122)),
                  voor=(869, 119, -1104), binnen=(876, 117, -1107)))
    # zone 3, De Wortelgrot: the hollow root stump's door (north), the inner ring (the outer ring stays a 1-deep frame)
    D.append(dict(grot="wortelgrot", hoofd=True, thema="wortel", cellen=_ring(1410, -731, 3.6, 4.7, -math.pi / 2, 69, 71),
                  voor=(1410, 69, -737), binnen=(1410, 69, -733)))
    # zone 4, De Koraalgrot: the dry way (the coral chimney on the atoll rim, door to the south-west) = the main door; the
    # flooded mouth on the sea floor gets a door too (the cave has two ways in)
    D.append(dict(grot="koraalgrot", hoofd=True, thema="koraal", cellen=_ring(2234, -600, 3.6, 4.8, math.radians(135), 71, 73),
                  voor=(2230, 71, -596), binnen=(2232, 71, -598)))
    D.append(dict(grot="koraalgrot", hoofd=False, thema="koraal", cellen=_vlak(2267, (2267, 52, -572), 5, 50, 56, _boog(2268, -572, (-1, 0), 51, 3, 3, 1)),
                  voor=(2270, 51, -572), binnen=(2232, 71, -598)))
    # zone 5, De Wolkengrotten: the blowhole on the whale's head is a hole with a spiral down: a cloud HATCH in the shaft
    D.append(dict(grot="wolkengrot", hoofd=True, thema="wolk", cellen=_luik(2492, -520, 204, 3.6), luik=True,
                  voor=(2494, 205, -520), binnen=(2492, 199, -517), ingang=(2498, 209, -520)))   # (a flying whale: no walkable window edge)
    # zone 6, De Zingende Zoutgrot: across the rock-cut stair where it reaches the hall passage (3 wide x 4 high); the game moves it
    # with u-z6wereld's POI "zoutgrot" (3874, 50, -534) like the rest of the zone-6 cave entry
    D.append(dict(grot="zoutgrot", hoofd=True, thema="zout", cellen=_muur(3873, 3875, 50, 53, -523),
                  voor=(3874, 50, -521), binnen=(3874, 50, -526)))
    for d in D:
        d.setdefault("luik", False)
        d["cellen"] = sorted(set(d["cellen"]))
        d["deel"] = _delen(d)
    return D


def _delen(d):
    """Per cell the look: 0 panel, 1 panel with the knocker (middle column, one above the bottom), 2 hatch."""
    if d["luik"]:
        return [2] * len(d["cellen"])
    ys = [c[1] for c in d["cellen"]]
    y0 = min(ys)
    xs = sorted(set((c[0], c[2]) for c in d["cellen"]))
    mid = xs[len(xs) // 2]
    return [1 if (c[0], c[2]) == mid and c[1] == y0 + 1 else 0 for c in d["cellen"]]


def _yaw(van, naar):
    """Minecraft yaw to look from `van` towards `naar` (0 = south, 90 = west)."""
    dx, dz = naar[0] - van[0], naar[2] - van[2]
    return round(math.degrees(math.atan2(-dx, dz)) / 45.0) * 45.0


# ===================================================================================================================== check
def controle(D, stil=False):
    """Checks the doors against the generated tiles (skipped per door when its tiles are missing). Returns a list of problems."""
    sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk"))
    try:
        import loopcheck as L
        import numpy as np
        from scipy import ndimage
    except ImportError as e:
        return [f"(controle overgeslagen: {e})"]
    cache = {}

    def tegel(tx, tz):
        if (tx, tz) not in cache:
            p = os.path.join(L.TEGELS, f"t_{tx}_{tz}.nbt")
            if not os.path.exists(p):
                cache[(tx, tz)] = None
            else:
                pal, x, y, z, st = L.lees_tegel(p)
                kl = [L.klasse(n, pp) for n, pp in pal]
                namen = [n for n, _ in pal]
                cache[(tx, tz)] = ({(int(a), int(b), int(c)): int(s) for a, b, c, s in zip(x, y, z, st)}, kl, namen)
        return cache[(tx, tz)]

    def info(x, y, z):
        t = tegel(x // 64, z // 64)
        if t is None:
            return None
        s = t[0].get((x % 64, y, z % 64))
        if s is None:
            return ("minecraft:air", L.PASS)
        return (t[2][s], t[1][s])

    def open_(x, y, z):
        i = info(x, y, z)
        return i is not None and (i[1] in (L.PASS, L.CLIMB))

    def vloer(x, y, z):
        if (x, y - 1, z) in deurcel:
            return True
        i = info(x, y - 1, z)
        return i is not None and i[1] in (L.SOLID, L.HALF, L.STAIR, L.DIRT_PATH)

    fout = []
    for d in D:
        deurcel = set(d["cellen"])
        naam = f"{d['grot']}{'' if d['hoofd'] else ' (2e deur)'}"
        if info(*d["cellen"][0]) is None:
            fout.append(f"{naam}: geen tegels hier (controle overgeslagen)")
            continue
        dicht = [c for c in d["cellen"] if not open_(*c)]
        if dicht:
            fout.append(f"{naam}: {len(dicht)} deurcellen zijn nu niet open (worden overgeslagen): {[(c, info(*c)[0]) for c in dicht[:4]]}")
        for k in ("voor", "binnen"):
            v = d[k]
            if d["grot"] == "koraalgrot" and not d["hoofd"] and k == "voor":
                ok = open_(*v)                       # (under water: just open)
            else:
                ok = vloer(*v) and open_(*v) and open_(v[0], v[1] + 1, v[2])
            if not ok:
                fout.append(f"{naam}: {k} {v} is geen goede staanplek (vloer {info(v[0], v[1] - 1, v[2])}, {info(*v)}, {info(v[0], v[1] + 1, v[2])})")
        # the seal (the walkability standard's movement model, tools/vadsrijk/loopcheck.py): with the door cells solid, the spot
        # inside must NOT be reachable by walking/jumping/falling from the stand spot in front of the door (a window of tiles round it)
        if d["grot"] == "zoutgrot":
            continue      # (zone 6's tiles leave the rock under the surface out: the movement graph says nothing there)
        lek = _lek(L, d, [c for e in D if e["grot"] == d["grot"] for c in e["cellen"]])
        if lek:
            fout.append(f"{naam}: LEK: {lek}")
    if not stil:
        for d in D:
            print(f"  {d['grot']:12s} {'hoofd' if d['hoofd'] else '2e   '} {d['thema']:8s} {len(d['cellen']):2d} cellen  voor {d['voor']}  binnen {d['binnen']}")
        print("  controle: " + ("alles goed" if not fout else ""))
        for f in fout:
            print("   - " + f)
    return fout


def _lek(L, d, alle):
    """loopcheck's survival movement graph in the tiles round the door, the door cells made solid, seeded at "voor": is "binnen"
    reachable? Returns '' (sealed) or a description."""
    import io
    import contextlib
    xs = [c[0] for c in d["cellen"]] + [d["voor"][0], d["binnen"][0]]
    zs = [c[2] for c in d["cellen"]] + [d["voor"][2], d["binnen"][2]]
    tx0, tx1 = min(xs) // 64 - 1, max(xs) // 64 + 1
    tz0, tz1 = min(zs) // 64 - 1, max(zs) // 64 + 1
    V = L.Venster(tx0, tz0, tx1, tz1)
    for (x, y, z) in alle:
        if 0 <= x - V.x0 < V.k.shape[0] and 0 <= z - V.z0 < V.k.shape[2]:
            V.k[x - V.x0, y - L.Y0, z - V.z0] = L.SOLID
    oud = (L.SEEDS, L.POIS, L.LINKS)
    L.SEEDS, L.POIS, L.LINKS = [list(d["voor"])], {"binnen": list(d["binnen"])}, []
    buf = io.StringIO()
    try:
        with contextlib.redirect_stdout(buf):
            L.analyse(V, (V.x0, V.z0, V.x0 + (tx1 - tx0 + 1) * 64, V.z0 + (tz1 - tz0 + 1) * 64))
    finally:
        L.SEEDS, L.POIS, L.LINKS = oud
    # and the stand spot itself is reachable by walking from the edge of the window (the outside world; "ingang" when given)
    L.SEEDS, L.POIS, L.LINKS = ([list(d["ingang"])] if d.get("ingang") else None), {"voor": list(d["voor"])}, []
    buf2 = io.StringIO()
    try:
        with contextlib.redirect_stdout(buf2):
            L.analyse(V, (V.x0, V.z0, V.x0 + (tx1 - tx0 + 1) * 64, V.z0 + (tz1 - tz0 + 1) * 64))
    finally:
        L.SEEDS, L.POIS, L.LINKS = oud
    if not (d["grot"] == "koraalgrot" and not d["hoofd"]):
        for r in buf2.getvalue().splitlines():
            if r.startswith("POI") and "voor" in r:
                return "de staanplek voor de deur is zelf niet te bereiken: " + r.strip()
    uit = buf.getvalue()
    for r in uit.splitlines():
        if r.startswith("POI") and "binnen" in r:
            if "NIET BEREIKBAAR" in r:
                return ""
            return "binnen: " + r.strip()
    return f"binnen {d['binnen']} is lopend te bereiken vanaf voor {d['voor']} terwijl de deur dicht is"


# ===================================================================================================================== java
def java(D):
    regels = []
    for d in D:
        cel = ", ".join(f"new int[] {{{x}, {y}, {z}, {p}}}" for (x, y, z), p in zip(d["cellen"], d["deel"]))
        v, b = d["voor"], d["binnen"]
        mid = (sum(c[0] for c in d["cellen"]) / len(d["cellen"]), 0, sum(c[2] for c in d["cellen"]) / len(d["cellen"]))
        yv, yb = _yaw(v, mid), _yaw(mid, b) if (b[0], b[2]) != (round(mid[0]), round(mid[2])) else 0.0
        regels.append(f'            new Deur("{d["grot"]}", {str(d["hoofd"]).lower()}, "{d["thema"]}", {str(d["luik"]).lower()},\n'
                      f'                    List.of({cel}),\n'
                      f'                    new BlockPos({v[0]}, {v[1]}, {v[2]}), {yv}f, new BlockPos({b[0]}, {b[1]}, {b[2]}), {yb}f)')
    src = f"""package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * GENERATED by tools/features/vadsrijk_w_grot.py (do not edit by hand). W-GROT: the closed door of every cave dungeon: its cells
 * ({{x, y, z, look}}: look 0 panel, 1 panel with the knocker, 2 hatch), the stand spot in front of it ({{@code voor}}, where you come out,
 * with the yaw that looks at the door) and the spot just inside ({{@code binnen}}, where the group is teleported in). {{@code hoofd}} =
 * the main door (De Koraalgrot also has one on its flooded sea-floor mouth). Zone 6 is moved with u-z6wereld's POI "zoutgrot" in
 * {{@link GrotDeur#deuren()}}.
 */
public final class GrotDeurData {{
    public record Deur(String grot, boolean hoofd, String thema, boolean luik, List<int[]> cellen, BlockPos voor, float yawVoor, BlockPos binnen,
            float yawBinnen) {{
    }}

    public static final List<Deur> ALLE = List.of(
{("," + chr(10)).join(regels)});

    private GrotDeurData() {{
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)


# ===================================================================================================================== assets
# per theme: (frame, panel, panel light, metal, accent)
KLEUR = {
    "zwam": ((112, 74, 52), (222, 210, 186), (238, 230, 210), (196, 60, 60), (255, 240, 240)),       # stem door, red cap metal
    "kristal": ((150, 160, 184), (214, 226, 240), (236, 244, 252), (160, 110, 220), (220, 190, 255)),
    "wortel": ((58, 40, 26), (104, 74, 48), (128, 94, 62), (150, 150, 150), (120, 170, 80)),
    "koraal": ((52, 112, 108), (98, 170, 160), (130, 196, 184), (255, 132, 122), (255, 210, 190)),
    "wolk": ((196, 204, 220), (240, 244, 250), (255, 255, 255), (232, 190, 110), (255, 230, 160)),
    "zout": ((210, 170, 176), (246, 222, 226), (255, 238, 240), (214, 168, 64), (255, 220, 120)),
}


def _tex(thema, deel):
    from PIL import Image
    fr, pa, pl, me, ac = KLEUR[thema]
    im = Image.new("RGBA", (16, 16), fr + (255,))
    px = im.load()

    def mix(c, t):
        return tuple(max(0, min(255, int(v * t))) for v in c[:3]) + (255,)
    for x in range(16):
        for y in range(16):
            if deel == 2:      # hatch: planks across with two metal bands and a ring in the middle
                plank = (y // 4) % 2
                c = pa if plank else pl
                if y % 4 == 0:
                    c = mix(c, 0.82)
                if x in (3, 12):
                    c = me
                px[x, y] = c + (255,) if len(c) == 3 else c
                continue
            if x in (0, 15) or y in (0, 15):
                continue      # frame
            # vertical boards with a darker seam every 5 px, a light top edge
            c = pl if (x // 5) % 2 == 0 else pa
            if x % 5 == 0:
                c = mix(c, 0.8)
            if y == 1:
                c = mix(c, 1.08)
            # two horizontal metal bands with rivets
            if y in (4, 11):
                c = me
                if x in (2, 7, 13):
                    c = ac
            px[x, y] = c + (255,) if len(c) == 3 else c
    if deel == 1:              # the knocker: a ring under a little plate
        for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
            px[x, y] = ac + (255,)
        for (x, y) in ((6, 8), (9, 8), (5, 9), (10, 9), (5, 10), (10, 10), (6, 11), (9, 11), (7, 12), (8, 12)):
            px[x, y] = me + (255,)
    if deel == 2:
        for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
            px[x, y] = ac + (255,)
    return im


def assets():
    tex = os.path.join(A, "textures", "block", "vadsrijk", "grotdeur")
    os.makedirs(tex, exist_ok=True)
    varianten = {}
    for t in THEMAS:
        for deel, naam in ((0, "paneel"), (1, "klink"), (2, "luik")):
            _tex(t, deel).save(os.path.join(tex, f"{t}_{naam}.png"))
            m = f"{BLOK}_{t}_{naam}"
            _schrijf(os.path.join(A, "models", "block", f"{m}.json"),
                     {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/vadsrijk/grotdeur/{t}_{naam}"}})
            varianten[f"deel={deel},thema={t}"] = {"model": f"guhs:block/{m}"}
    _schrijf(os.path.join(A, "blockstates", f"{BLOK}.json"), {"variants": varianten})
    return len(varianten)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ===================================================================================================================== texts
def teksten():
    K = "vadsrijk.grotdeur."
    t = {
        f"block.guhs.{BLOK}": "Grotdeur",
        K + "titel": "Grotkerker",
        K + "zone": "Zone %s",
        K + "aanbevolen": "Aanbevolen: level %s-%s · %s",
        K + "spelers.solo": "1-4 spelers (alleen goed te doen)",
        K + "spelers.groep": "2-4 spelers (alleen is pittig)",
        K + "baas": "Grotbaas: %s",
        K + "buit": "Buit bij elke clear",
        K + "buit.munten": "• %s brons",
        K + "buit.stuk.eerste": "• 1 uitrustingsstuk (lv %s): je eerste clear is Episch!",
        K + "buit.stuk": "• 1 uitrustingsstuk (lv %s): Zeldzaam, %s%% kans op Episch",
        K + "buit.erts": "• 3-5 ertsen van de zone",
        K + "buit.kern": "• %s%% kans op de bazenkern van de zone",
        K + "zeldzaam": "Zeldzaam (persoonlijk, voor iedereen apart)",
        K + "pet": "• Huisdier %s: %s%% per clear · pech %s/%s",
        K + "pet.heb": "• Huisdier %s: ✔ al van jou",
        K + "pluim": "• Pluimguh-kleur %s: %s%% per clear · pech %s/%s",
        K + "pluim.heb": "• Pluimguh-kleur %s: ✔ al van jou",
        K + "pluim.geen": "• Pluimguh-kleur %s: %s%% · pas als je een Pluimguh hebt",
        K + "pech.uitleg": "Pech = keer op rij mis; bij de laatste is het zeker raak.",
        K + "jij": "Jij: %s keer uitgespeeld",
        K + "wachttijd": "Wachttijd: nog %s",
        K + "wachttijd.geen": "Geen wachttijd: je kunt naar binnen",
        K + "status.vrij": "De grot is vrij.",
        K + "status.bezet": "Bezet: een groep van %s is binnen (%s).",
        K + "status.binnen": "Jouw groep is binnen (%s).",
        K + "status.rij": "Jij staat in de wachtrij: %s/%s",
        K + "status.rij.bezet": "Jij staat in de wachtrij: %s/%s (wacht tot de grot vrij is)",
        K + "status.check": "Klaar-check loopt...",
        K + "knop.solo": "Solo starten",
        K + "knop.groep": "Starten met groep (%s)",
        K + "knop.groep.geen": "Starten met groep",
        K + "knop.rij": "In de wachtrij",
        K + "knop.rij.uit": "Uit de wachtrij",
        K + "knop.nu": "Nu starten (%s/%s)",
        K + "knop.terug": "Terug naar binnen",
        K + "knop.verlaat": "Grot verlaten",
        K + "knop.blijf": "Nog even blijven",
        K + "tip.solo": "Alleen naar binnen. De grot is dan helemaal van jou.",
        K + "tip.groep": "Met je groepje (party). Iedereen krijgt eerst een klaar-check.",
        K + "tip.groep.geen": "Je zit niet in een groepje. Nodig iemand uit via het Vadsrijk-menu of /guhs party.",
        K + "tip.rij": "Zoek medespelers voor deze grot. Bij 4 (of als jullie eerder starten) krijgt iedereen een klaar-check.",
        K + "tip.nu": "Start nu met wie er in de wachtrij staat (ook als het er minder dan 4 zijn).",
        # ready check
        K + "klaar.titel": "Klaar voor %s?",
        K + "klaar.uitleg": "Iedereen moet op Klaar! drukken. Dan gaan jullie samen naar binnen.",
        K + "klaar.ja": "Klaar!",
        K + "klaar.nee": "Nee",
        K + "klaar.tijd": "Nog %s s",
        K + "klaar.wacht": "Je bent klaar. Wachten op de rest...",
        # chat / overlay
        K + "msg.rij.erin": "Je staat in de wachtrij voor %s (%s/%s). Je mag rondlopen; uitloggen of een andere wereld = uit de rij.",
        K + "msg.rij.uit": "Je staat niet meer in de wachtrij voor %s.",
        K + "msg.rij.status": "Wachtrij %s: %s/%s",
        K + "msg.rij.status.bezet": "Wachtrij %s: %s/%s · grot bezet",
        K + "msg.rij.weg.dim": "Je bent uit de wachtrij voor %s (andere wereld).",
        K + "msg.check.start": "Klaar-check voor %s! Druk op Klaar! (%s s).",
        K + "msg.check.nee": "%s doet (nog) niet mee. De anderen blijven in de wachtrij.",
        K + "msg.check.nee.groep": "%s doet (nog) niet mee. Starten gaat niet door.",
        K + "msg.check.tijd": "Niet iedereen was op tijd klaar. Wie niet antwoordde, staat niet meer in de rij.",
        K + "msg.check.tijd.groep": "Niet iedereen was op tijd klaar. Starten gaat niet door.",
        K + "msg.binnen": "Jullie gaan %s binnen! (%s %s)",
        K + "msg.binnen.speler": "speler",
        K + "msg.binnen.spelers": "spelers",
        K + "msg.nep.buiten": "(%s is een nep-testspeler en blijft buiten)",
        K + "msg.niet.vadsrijk": "Dat kan alleen in Het Vadsrijk.",
        K + "msg.al.kerker": "Je zit al in een grotkerker (%s). Ga daar eerst naar buiten.",
        K + "msg.al.raid": "Je zit al in de raid. Eerst die afmaken of verlaten.",
        K + "msg.al.rij": "Je staat al in de wachtrij voor %s.",
        K + "msg.cooldown": "Je moet nog %s wachten voor %s.",
        K + "msg.cooldown.ander": "%s moet nog %s wachten voor deze grot.",
        K + "msg.bezet.ander": "%s zit al in een grotkerker of de raid.",
        K + "msg.bezet": "De grot is bezet door een andere groep. Ga in de wachtrij: jullie zijn daarna aan de beurt.",
        K + "msg.geen.groep": "Je zit niet in een groepje.",
        K + "msg.groep.leider": "Alleen de leider van je groepje kan starten.",
        K + "msg.groep.te.groot": "Je groepje is te groot voor een grot (max %s).",
        K + "msg.check.bezig": "Er loopt al een klaar-check voor deze grot. Even wachten.",
        K + "msg.dicht": "De deur zit dicht. Rechtsklik erop om de grotkerker te starten.",
        K + "msg.niet.van.jou": "De grot is van een andere groep. Je staat weer buiten.",
        K + "msg.niet.gestart": "Deze grot is dicht. Start hem via de deur.",
        K + "msg.uit": "Je bent weer buiten bij de deur van %s.",
        K + "msg.klaar.uit": "Grot uitgespeeld! Rechtsklik op de deur (of gebruik het venster) om naar buiten te gaan.",
        K + "msg.sluit.straks": "Er wacht een andere groep: de grot gaat over %s dicht en jullie staan dan buiten.",
        K + "msg.gesloten": "De grot is gesloten. Bedankt voor het spelen!",
        K + "msg.wachttijd.gezet": "Wachttijd voor %s: %s (dan kun je opnieuw).",
        K + "msg.leeg": "Jullie groep is weg: de grot gaat weer dicht.",
        K + "msg.klaar.ja": "%s is klaar.",
        K + "klaar.popup": "Grot uitgespeeld!",
    }
    return t


def build(h):
    D = deuren()
    controle(D, stil=True)
    java(D)
    assets()
    for k, v in teksten().items():
        h.lang(k, v, v)   # (the shared helper takes en first; en copies the Dutch until the English pass)


def _lang_direct(tk):
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            regels = f.read().split("\n")
        data = json.loads("\n".join(regels))
        nieuw = [(k, v) for k, v in tk.items() if data.get(k) != v]
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


if __name__ == "__main__":
    D = deuren()
    fout = controle(D)
    if "--check" not in sys.argv:
        java(D)
        n = assets()
        tk = teksten()
        _lang_direct(tk)
        print(f"vadsrijk_w_grot: {len(D)} deuren, {n} blokvarianten, {len(tk)} teksten")
    sys.exit(1 if any("LEK" in f or "staanplek" in f for f in fout) else 0)
