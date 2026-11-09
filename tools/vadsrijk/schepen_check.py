"""W-SCHEPEN: checks EVERY boarding point of Het Vadsrijk against the real generated map tiles (survival, on foot).

    python tools/vadsrijk/schepen_check.py            # report; exit code 1 when any boarding point fails
    python tools/vadsrijk/schepen_check.py --kaart    # also print a small top-down map around every airship harbour
    python tools/vadsrijk/schepen_check.py --alleen opstijgpier,oosttak   # only harbours whose id/name contains one of these

What is a boarding point:
  * every airship harbour of every ACTIVE line (luchtschip/Lijnen.java + zone6/verhaal/Z6Lijn.java); its stand spot is
    Haven.plek() (the POI, the zone-4 anchor rule, or the placeholder), the docked ship lies at Haven.dok() = plek + UIT * uit,
    deck at plek.y, bow outward (the click boxes are LuchtschipEntity.DELEN, mirrored below);
  * every zone-4 boat dock (boot_dok_<id> POIs, BootZee): the boat is summoned onto water within Boot.ZOEK blocks and you
    step off onto the planks;
  * the harbour bells / captains / boards that sit next to a harbour (only that they stand on a reachable cell).

Checks per airship harbour (each one FAILS loudly):
  STAND    plek is a stand cell (solid floor below, 2 free cells);
  BEREIK   plek is part of a walkable area (flood fill on foot: >= MIN_CELLEN cells or >= MIN_VER blocks away);
  RAND     no fence / wall / railing / solid block between the stand spot and the ship (the boarding edge), at feet and head;
  KIER     the ship's stern starts at most MAX_KIER blocks beyond the pier's last floor cell, deck height = pier height +-1;
  KLIK     a reachable stand cell has the ship's click box within ENTITY_REACH (3.0, the survival entity interaction range)
           of the eye, with nothing solid in between (fails > 3.0, warns > 2.5);
  RUIMTE   the docked hull (and balloon) do not stick into the terrain / pier (solid blocks inside the hull box beyond the pier);
  LANDING  the arrival spot (WereldReizen.veiligeLanding(plek), replicated) is on the same walkable area as the stand spot;
  BARRIERE the tip lies inside a free area of indeling/Barrieres (else the barrier pushes you off the pier).
Per boat dock: STAND + BEREIK of the planks, water within ZOEK (6) of the planks (the key puts the boat there).
"""
import math
import os
import re
import sys
from collections import deque

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

T = 64
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

ENTITY_REACH = 3.0          # Attributes.ENTITY_INTERACTION_RANGE default (survival)
REACH_WENS = 2.5
OOG = 1.62
MAX_KIER = 1.0
MIN_CELLEN, MIN_VER = 300, 25
BOOT_ZOEK = 6

# ---- LuchtschipEntity mirrors (keep in sync with LuchtschipEntity.DELEN / Lijnen.Haven.UIT) ------------------------------------
DELEN = [(-6.5, -3.25, 6.5, 4.25), (-2.2, -3.25, 6.5, 4.25), (2.2, -3.25, 6.5, 4.25), (6.5, -3.25, 6.5, 4.25),
         (10.5, -2.5, 4, 3), (-6.5, 0, 5.5, 4.25), (-6, 8.5, 9, 9), (0, 8.5, 9, 9), (6, 8.5, 9, 9), (11.5, 10, 6, 6.5),
         (-12.5, 11, 5, 5), (6.75, 17.5, 7, 2.5)]
# the visible ship: every cube of the model vr_luchtschip (scale 4: 1 px = 0.25 block; ship frame +z = bow = model -z), the two
# propellers as their spinning discs. ROMP_ACHTER = the stern (hull, cabin, rails), PROP_ACHTER = the propeller discs.
SCHAAL = 4 / 16


def model_dozen():
    import json
    geo = json.load(open(os.path.join(ROOT, "src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity",
                                      "vr_luchtschip.geo.json"), encoding="utf-8"))
    dozen = []
    for g in geo["minecraft:geometry"]:
        for b in g["bones"]:
            if b["name"].startswith("prop"):
                continue
            for c in b.get("cubes", []):
                (ox, oy, oz), (sx, sy, sz) = c["origin"], c["size"]
                dozen.append((b["name"], (ox * SCHAAL, (ox + sx) * SCHAAL), (-(oz + sz) * SCHAAL, -oz * SCHAAL), (oy * SCHAAL, (oy + sy) * SCHAAL)))
    for k in (-1, 1):   # (lateral, ship z, y) of the spinning propeller discs (hub x +-17 px, blade 16 px, at model z 35..38)
        dozen.append(("prop", tuple(sorted((k * 2.25, k * 6.25))), (-9.5, -8.75), (-0.5, 3.5)))
    return dozen


MODEL = model_dozen()
ROMP_ACHTER = min(d[2][0] for d in MODEL if d[0] == "romp")
PROP_ACHTER = min(d[2][0] for d in MODEL if d[0] == "prop")


def lees_uit():
    src = open(os.path.join(JAVA, "luchtschip", "Lijnen.java"), encoding="utf-8").read()
    m = re.search(r"double UIT = ([\d.]+)", src)
    return float(m.group(1))


UIT = lees_uit()

# ---- blocks ----------------------------------------------------------------------------------------------------------------------
LUCHT, VAST, HALF, HEK, KLIM, WATER = 0, 1, 2, 3, 4, 5
_PASS = ("air", "structure_void", "light", "short_grass", "tall_grass", "fern", "tulip", "allium", "daisy", "cornflower",
         "bluet", "petals", "carpet", "torch", "button", "sign", "pressure_plate", "rail", "sapling", "lily_pad", "riet",
         "guhbloem", "guhoortjes", "hanging_moss", "firefly_bush", "bush", "portal", "cobweb", "waterlelie", "flower",
         "dandelion", "poppy", "orchid", "lilac", "rose", "peony", "redstone_wire", "lever", "candle", "snow[",
         "glow_lichen", "seagrass", "kelp", "banner", "item_frame", "tripwire", "dead_bush", "potted", "flower_pot",
         "knuffel_", "mushroom", "head", "skull", "end_rod", "lightning_rod", "bell", "lantern", "chain", "door", "_deur",
         "fence_gate", "trapdoor", "wall_")


def klasse(state):
    naam = state.split("[")[0]
    kort = naam.split(":", 1)[-1]
    props = state[len(naam):]
    if kort in ("water", "bubble_column"):
        return WATER
    if kort in ("ladder", "scaffolding") or kort.endswith(("vine", "vines", "vines_plant")):
        return KLIM
    if kort in ("barrier",):
        return HEK                      # invisible wall: as bad as a fence on a boarding edge
    if kort.endswith(("leaves", "bladeren", "_log", "_wood")) or kort in ("sea_lantern", "redstone_lamp", "glowstone"):
        return VAST
    if kort.endswith(("_slab", "_plaat")):
        return HALF if "type=bottom" in props or "type=" not in props else VAST
    if kort.endswith(("stairs", "_trap")):
        return HALF
    if kort.endswith("fence_gate"):
        return LUCHT
    if kort.endswith(("fence", "hekje", "hek", "_muur", "pane", "railing", "reling")) or kort.endswith("_wall") and "wall_" not in kort \
            or kort == "iron_bars" or "lantaarnpaal" in kort:
        return HEK
    for w in _PASS:
        if w in kort:
            return LUCHT
    return VAST


class Wereld:
    def __init__(self):
        self.cache = {}
        self.namen = {}

    def tegel(self, tx, tz):
        if (tx, tz) not in self.cache:
            p = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
            g = np.zeros((T, 384, T), np.uint8)
            nm = None
            if os.path.exists(p):
                pal, (xs, ys, zs, st) = TL.lees(p)
                kl = np.array([klasse(s) for s in pal], np.uint8)
                g[xs, ys + 64, zs] = kl[st]
                nm = (pal, {(int(x), int(y), int(z)): int(s) for x, y, z, s in zip(xs, ys, zs, st)})
            self.cache[(tx, tz)] = g
            self.namen[(tx, tz)] = nm
        return self.cache[(tx, tz)]

    def k(self, x, y, z):
        if not -64 <= y < 320:
            return LUCHT
        return int(self.tegel(x // T, z // T)[x % T, y + 64, z % T])

    def naam(self, x, y, z):
        self.tegel(x // T, z // T)
        nm = self.namen[(x // T, z // T)]
        if not nm:
            return "minecraft:air"
        s = nm[1].get((x % T, y, z % T))
        return "minecraft:air" if s is None else nm[0][s]

    def vrij(self, x, y, z):
        return self.k(x, y, z) == LUCHT

    def vloer(self, x, y, z):
        return self.k(x, y, z) in (VAST, HALF)

    def stand(self, x, y, z):
        """Can a player stand with his feet in cell (x, y, z)? (a bottom slab / stair IN the cell counts too)"""
        if self.k(x, y, z) == HALF and self.vrij(x, y + 1, z) and self.vrij(x, y + 2, z):
            return True
        return self.vloer(x, y - 1, z) and self.vrij(x, y, z) and self.vrij(x, y + 1, z)

    def gebied(self, start, limiet=4000):
        """Flood fill on foot from a stand cell: step +-1 (jump up needs headroom), fall up to 3. -> set of cells."""
        if not self.stand(*start):
            return set()
        gezien = {start}
        q = deque([start])
        while q and len(gezien) < limiet:
            x, y, z = q.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                for dy in (0, 1, -1, -2, -3):
                    ny = y + dy
                    if dy == 1 and not self.vrij(x, y + 2, z):
                        continue
                    if dy < 0 and not all(self.vrij(nx, y + d, nz) for d in range(dy + 1, 2)):
                        continue
                    if self.stand(nx, ny, nz):
                        c = (nx, ny, nz)
                        if c not in gezien:
                            gezien.add(c)
                            q.append(c)
                        break
        return gezien

    def landing(self, plek):
        """WereldReizen.veiligeLanding: spiral r 0..4, first column cell from y-3 up to y+24 with a floor + 2 free."""
        px, py, pz = plek
        for r in range(0, 5):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) != r:
                        continue
                    for y in range(py - 3, py + 25):
                        x, z = px + dx, pz + dz
                        if self.k(x, y - 1, z) in (VAST, HALF, HEK) and self.vrij(x, y, z) and self.vrij(x, y + 1, z):
                            return (x, y, z)
        return None


# ---- sources ---------------------------------------------------------------------------------------------------------------------
def vrije_gebieden():
    """indeling/Barrieres.VRIJ: (zone, cx, cz, rx, rz, vanafY) superellipses (p 4); outside them (in the sea area) you are pushed."""
    src = open(os.path.join(JAVA, "indeling", "Barrieres.java"), encoding="utf-8").read()
    return [(m.group(1), *(float(m.group(i)) for i in range(2, 6)), int(m.group(6)))
            for m in re.finditer(r'new Vrij\("(\w+)", (-?[\d.]+), (-?[\d.]+), (-?[\d.]+), (-?[\d.]+), (-?\d+)\)', src)]


VRIJ = vrije_gebieden()


def barriere_vrij(x, y, z):
    if not (-1300 < x < 4700 and -1900 < z < 1700):
        return True
    return any(y >= v[5] and ((abs(x - v[1]) / v[3]) ** 4 + (abs(z - v[2]) / v[4]) ** 4) ** 0.25 <= 1 for v in VRIJ)


def pois():
    alle = {}
    for f in ("wereld/WereldPlekken.java", "zone2/wereld/Z2WereldPlekken.java", "zone3/wereld/Z3WereldPlekken.java",
              "zone4/wereld/Z4WereldPlekken.java", "zone5/wereld/Z5WereldPlekken.java", "zone6/wereld/Z6WereldPlekken.java"):
        p = os.path.join(JAVA, f)
        if os.path.exists(p):
            src = open(p, encoding="utf-8").read()
            for m in re.finditer(r'Map\.entry\("([a-z0-9_]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', src):
                alle.setdefault(m.group(1), (int(m.group(2)), int(m.group(3)), int(m.group(4))))
    return alle


def havens_en_lijnen(alle):
    src = open(os.path.join(JAVA, "luchtschip", "Lijnen.java"), encoding="utf-8").read()
    havens = {}
    for m in re.finditer(r'Haven (\w+) = new Haven\("([a-z0-9_]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\), (-?[\d.]+)f', src):
        havens[m.group(1)] = (m.group(2), (int(m.group(3)), int(m.group(4)), int(m.group(5))), float(m.group(6)))
    lijnen = []
    for m in re.finditer(r'Lijn (L\w+) = new Lijn\("([a-z0-9_]+)", "(\w)", (\w+), (\w+), List\.of\(', src):
        lijnen.append((m.group(3), m.group(2), havens[m.group(4)], havens[m.group(5)]))
    # line E (zone 6, added at runtime)
    z6 = open(os.path.join(JAVA, "zone6", "verhaal", "Z6Lijn.java"), encoding="utf-8").read()
    z6p = open(os.path.join(JAVA, "zone6", "verhaal", "Z6VerhaalPlekken.java"), encoding="utf-8").read()
    res = {m.group(1): (m.group(2), (int(m.group(3)), int(m.group(4)), int(m.group(5))))
           for m in re.finditer(r'BlockPos (\w+) = w\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+)\)', z6p)}
    per_id = {m.group(1): (int(m.group(2)), int(m.group(3)), int(m.group(4)))
              for m in re.finditer(r'w\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+)\)', z6p)}
    hz6 = {}
    for m in re.finditer(r'Haven (\w+) = new Lijnen\.Haven\("([a-z0-9_]+)", Z6VerhaalPlekken\.(\w+), (-?[\d.]+)f', z6):
        alias, reserve = res.get(m.group(3), (m.group(3).lower(), per_id.get(m.group(3).lower())))
        pos = alle.get(alias, reserve)
        hz6[m.group(1)] = (m.group(2), pos, float(m.group(4)))
    m = re.search(r'Lijn LE = new Lijnen\.Lijn\(ID, "(\w)", (\w+), (\w+),', z6)
    if m:
        lijnen.append((m.group(1), "z5_z6", hz6[m.group(2)], hz6[m.group(3)]))
    return lijnen


def uitvector(richting):
    r = math.radians(richting)
    return (-math.sin(r), math.cos(r))


def plek(h, alle):
    """Haven.plek() incl. the zone-4 anchor rule (Lijnen.zone4Haven)."""
    pid, reserve, richting = h
    u = uitvector(richting)
    if pid in alle:
        return alle[pid], u, "POI"
    anker = {"haven_z4_z3_vertrek": "haven_3_4", "haven_z4_z5_vertrek": "haven_4_5"}.get(pid)
    if anker and anker + "_luchtschip" in alle and anker + "_landing" in alle:
        s, la = alle[anker + "_luchtschip"], alle[anker + "_landing"]
        return (math.floor(s[0] - u[0] * UIT), la[1], math.floor(s[2] - u[1] * UIT)), u, "anker " + anker
    return reserve, u, "RESERVE (geen POI)"


def doos_afstand(p, lo, hi):
    return math.sqrt(sum(max(lo[i] - p[i], 0, p[i] - hi[i]) ** 2 for i in range(3)))


def klikdozen(dok, u):
    """World AABBs of the click boxes of a ship docked at dok, bow along u."""
    uit = []
    for z, yb, b, h in DELEN:
        cx, cz = dok[0] + u[0] * z, dok[2] + u[1] * z
        uit.append(((cx - b / 2, dok[1] + yb, cz - b / 2), (cx + b / 2, dok[1] + yb + h, cz + b / 2)))
    return uit


def zicht(w, a, b):
    """Nothing solid / fence between point a and b (sampled every 0.2)."""
    n = max(1, int(math.dist(a, b) / 0.2))
    for i in range(1, n):
        t = i / n
        x, y, z = (a[k] + (b[k] - a[k]) * t for k in range(3))
        if w.k(math.floor(x), math.floor(y), math.floor(z)) in (VAST, HEK):
            return False
    return True


def dichtstbij(p, lo, hi):
    return tuple(min(max(p[i], lo[i]), hi[i]) for i in range(3))


def kaartje(w, mid, r, y, merk):
    regels = []
    for z in range(mid[2] - r, mid[2] + r + 1):
        s = ""
        for x in range(mid[0] - r, mid[0] + r + 1):
            if (x, z) in merk:
                s += merk[(x, z)]
                continue
            k0, k1 = w.k(x, y, z), w.k(x, y - 1, z)
            s += {HEK: "F", VAST: "#", HALF: "s", KLIM: "H", WATER: "~"}.get(k0) or {VAST: "=", HALF: "-", HEK: "f", WATER: "~"}.get(k1, ".")
        regels.append(f"      {z:5d} {s}")
    return regels


def check_haven(w, letter, lid, h, alle, kaart):
    fouten, waarsch, info = [], [], []
    (px, py, pz), u, bron = plek(h, alle)
    pid = h[0]
    info.append(f"plek {px} {py} {pz} ({bron}), richting {h[2]:.0f}")
    if bron.startswith("RESERVE"):
        fouten.append("RESERVE: geen POI voor deze haven op de kaart (het schip ligt op de geplande plek, niet aan een echte pier)")
    dok = (px + 0.5 + u[0] * UIT, py, pz + 0.5 + u[1] * UIT)
    # STAND
    if not w.stand(px, py, pz):
        fouten.append(f"STAND: plek {px},{py},{pz} is geen staplek (vloer {w.naam(px, py - 1, pz)}, voet {w.naam(px, py, pz)}, "
                      f"hoofd {w.naam(px, py + 1, pz)})")
    gebied = w.gebied((px, py, pz))
    ver = max((math.hypot(c[0] - px, c[2] - pz) for c in gebied), default=0)
    if gebied and len(gebied) < MIN_CELLEN and ver < MIN_VER:
        fouten.append(f"BEREIK: de pier is een eiland ({len(gebied)} cellen, verste {ver:.0f} blokken)")
    info.append(f"loopgebied {len(gebied)}{'+' if len(gebied) >= 4000 else ''} cellen, verste {ver:.0f}")
    # the pier's last floor cell along u (from plek outward), and the edge cells in between
    ax = (round(u[0]), round(u[1]))
    laatste = 0
    for d in range(0, 12):
        x, z = px + ax[0] * d, pz + ax[1] * d
        if (x, py, z) in gebied or w.stand(x, py, z):
            laatste = d
        else:
            break
    stern = UIT + min(ROMP_ACHTER, PROP_ACHTER)          # the ship's rearmost point (props), from the plek centre along u
    kier = stern - (laatste + 0.5)
    info.append(f"pier loopt {laatste} blok(ken) voorbij de plek door; achtersteven {stern:.2f} van het plekmidden, kier {kier:+.2f}")
    if kier > MAX_KIER:
        fouten.append(f"KIER: {kier:.1f} blokken lucht tussen de pierrand en de achtersteven")
    if kier < -1.5:
        waarsch.append(f"KIER: de achtersteven steekt {-kier:.1f} blokken over de pier")
    # RAND: fence/wall/solid on the boarding edge (cells between plek and the stern), feet + head, and 1 left/right
    rand = []
    eind = max(laatste + 1, math.ceil(stern))
    for d in range(0, eind + 1):
        for s in (-1, 0, 1):
            x, z = px + ax[0] * d - ax[1] * s, pz + ax[1] * d + ax[0] * s
            for dy in (0, 1):
                kk = w.k(x, py + dy, z)
                if kk == HEK or (kk == VAST and s == 0):
                    rand.append(f"{w.naam(x, py + dy, z).split('[')[0]} @ {x},{py + dy},{z}")
    if rand:
        fouten.append("RAND: hek/muur/blok op de instaprand: " + "; ".join(sorted(set(rand))[:6]))
    # KLIK
    dozen = klikdozen(dok, u)
    best, bestc = 1e9, None
    for c in gebied:
        if abs(c[0] - px) > 8 or abs(c[2] - pz) > 8 or abs(c[1] - py) > 1:
            continue
        oog = (c[0] + 0.5, c[1] + OOG, c[2] + 0.5)
        for lo, hi in dozen:
            d = doos_afstand(oog, lo, hi)
            if d < best and (d == 0 or zicht(w, oog, dichtstbij(oog, lo, hi))):
                best, bestc = d, c
    if best > ENTITY_REACH:
        fouten.append(f"KLIK: dichtstbijzijnde klikvak {best:.1f} blokken van een oog (max {ENTITY_REACH})")
    elif best > REACH_WENS:
        waarsch.append(f"KLIK: klikvak {best:.1f} blokken (wens <= {REACH_WENS})")
    info.append(f"klikafstand {best:.2f} vanaf {bestc}")
    # RUIMTE: solid blocks / fences overlapping any cube of the docked ship's model (hull, cabin, balloon, fins, ears, props)
    botsing = []
    e = 0.06
    for naam, (l0, l1), (a0, a1), (y0, y1) in MODEL:
        # ship frame -> world: along = u, lateral = (-u_z, u_x) (cardinal ships: an axis-aligned box)
        hoeken = [(dok[0] + u[0] * a - u[1] * l, dok[2] + u[1] * a + u[0] * l) for a in (a0, a1) for l in (l0, l1)]
        xa, xb = min(h[0] for h in hoeken) + e, max(h[0] for h in hoeken) - e
        za, zb = min(h[1] for h in hoeken) + e, max(h[1] for h in hoeken) - e
        ya, yb = py + y0 + e, py + y1 - e
        for x in range(math.floor(xa), math.floor(xb) + 1):
            for z in range(math.floor(za), math.floor(zb) + 1):
                for y in range(math.floor(ya), math.floor(yb) + 1):
                    if w.k(x, y, z) in (VAST, HEK, HALF, KLIM):
                        botsing.append((x, y, z, naam))
    botsing = sorted(set(botsing))
    if botsing:
        fouten.append(f"RUIMTE: {len(botsing)} blok(ken) in het model van het afgemeerde schip, b.v. "
                      + ", ".join(f"{w.naam(*b[:3]).split('[')[0].split(':')[-1]}@{b[0]},{b[1]},{b[2]} ({b[3]})" for b in botsing[:5]))
    # BARRIERE: the tip and the landing spot lie in a free area (indeling/Barrieres), else you are pushed off the pier
    if not barriere_vrij(px + 0.5, py, pz + 0.5):
        fouten.append(f"BARRIERE: de pierpunt {px},{py},{pz} ligt buiten elk vrij gebied (Barrieres.VRIJ): je wordt er weggeduwd")
    # LANDING
    la = w.landing((px, py, pz))
    if la is None:
        fouten.append("LANDING: veiligeLanding vindt geen staplek")
    elif la not in gebied:
        fouten.append(f"LANDING: je landt op {la}, niet op de pier zelf (los van het loopgebied)")
    elif abs(la[1] - py) > 1 or math.hypot(la[0] - px, la[2] - pz) > 2:
        fouten.append(f"LANDING: je landt op {la}, niet op de pierpunt {px},{py},{pz} (onder/naast de pier)")
    else:
        info.append(f"landing {la}")
    # neighbours: bel/kapitein/bord of this harbour stand on the walkable area
    for sleutel, p in alle.items():
        if math.dist(p, (px, py, pz)) < 16 and any(sleutel.endswith(t) for t in ("_kapitein", "_bel", "_vertrekbord", "_bord", "_landing")):
            if not any(math.dist(p, c) <= 2.5 for c in gebied):
                waarsch.append(f"{sleutel} {p} ligt niet bij het loopgebied van de pier")
    uitkaart = []
    if kaart:
        merk = {(px, pz): "P"}
        for a in range(int(stern), int(stern) + 22):
            x, z = math.floor(dok[0] + u[0] * (a - UIT)), math.floor(dok[2] + u[1] * (a - UIT))
            merk.setdefault((x, z), "o")
        if la:
            merk[(la[0], la[2])] = "L"
        uitkaart = kaartje(w, (px, py, pz), 14, py, merk)
    return fouten, waarsch, info, uitkaart


def check_boot(w, bid, p, alle):
    fouten, info = [], []
    x, y, z = p
    st = None
    for dy in (0, 1, -1):
        if w.stand(x, y + dy, z):
            st = (x, y + dy, z)
            break
    if st is None:
        fouten.append(f"STAND: {p} is geen staplek (vloer {w.naam(x, y - 1, z)}, voet {w.naam(x, y, z)})")
        return fouten, info
    gebied = w.gebied(st)
    ver = max((math.hypot(c[0] - x, c[2] - z) for c in gebied), default=0)
    if len(gebied) < 60 and ver < 10:
        fouten.append(f"BEREIK: steiger is een eilandje ({len(gebied)} cellen)")
    water = None
    for dy in range(1, -5, -1):
        for dx in range(-BOOT_ZOEK, BOOT_ZOEK + 1):
            for dz in range(-BOOT_ZOEK, BOOT_ZOEK + 1):
                if dx * dx + dz * dz > BOOT_ZOEK ** 2:
                    continue
                if w.k(x + dx, y + dy, z + dz) == WATER and w.vrij(x + dx, y + dy + 1, z + dz):
                    if water is None or dx * dx + dz * dz < water[0]:
                        water = (dx * dx + dz * dz, (x + dx, y + dy, z + dz))
        if water:
            break
    if water is None:
        fouten.append(f"WATER: geen open water binnen {BOOT_ZOEK} blokken (de X-toets zet de boot nergens neer)")
    else:
        info.append(f"water op {math.sqrt(water[0]):.1f} ({water[1]})")
    info.append(f"loopgebied {len(gebied)} cellen")
    return fouten, info


def main():
    kaart = "--kaart" in sys.argv
    alleen = None
    if "--alleen" in sys.argv:
        alleen = sys.argv[sys.argv.index("--alleen") + 1].split(",")
    alle = pois()
    w = Wereld()
    slecht = 0
    n = 0
    print(f"UIT = {UIT}: romp-achtersteven {UIT + ROMP_ACHTER:.2f}, schroeven {UIT + PROP_ACHTER:.2f} voorbij het plekmidden "
          f"(pierrand = 0.50), entity-bereik {ENTITY_REACH}")
    for letter, lid, a, b in havens_en_lijnen(alle):
        if lid == "kamp_z1" and not (a[0] in alle and b[0] in alle):
            print(f"\nlijn {letter} ({lid}): INACTIEF (Lijn.actief: {a[0]} / {b[0]} zijn geen POI) -> FOUT: lijn 0 vliegt niet")
            slecht += 1
            continue
        for h in (a, b):
            if alleen and not any(s in h[0] for s in alleen):
                continue
            n += 1
            f, wa, inf, kt = check_haven(w, letter, lid, h, alle, kaart)
            print(f"\nlijn {letter} haven {h[0]}: {'OK' if not f else 'FOUT'}")
            for r in inf:
                print("    " + r)
            for r in wa:
                print("    let op: " + r)
            for r in f:
                print("    FOUT " + r)
            for r in kt:
                print(r)
            slecht += bool(f)
    print("\n-- bootsteigers (zone 4) --")
    for bid, p in sorted(alle.items()):
        if not bid.startswith("boot_dok_") or bid.endswith(("_water", "_wal")):
            continue
        if alleen and not any(s in bid for s in alleen):
            continue
        n += 1
        f, inf = check_boot(w, bid, p, alle)
        print(f"{bid} {p}: {'OK' if not f else 'FOUT'}  " + "; ".join(inf))
        for r in f:
            print("    FOUT " + r)
        slecht += bool(f)
    print(f"\n{n} instappunten gecontroleerd, {slecht} met fouten")
    return 1 if slecht else 0


if __name__ == "__main__":
    sys.exit(main())
