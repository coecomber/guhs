"""
Het Knuffelbad (2.8) - the structure knuffelbad (96 x 72 x 96, one piece around the anchor guhs:knuffelbad_midden) on the
guhzee coast:

  - the Badhuis: a giant pink guh head (the dome of the bath hall: the washing tubs, Badmeester Bubbel, a foam fountain)
    between two wings (changing cabins and a cafe, the indoor foam baths); guh faces all round
  - the Buitenbad: a big pool (pink guhzee water) with foam islands, lily pads, rubber ducks, a lifeguard chair, parasols
  - three water slides, built from the same paths the rides follow (knuffelbad_glij.py):
      Grote Plons   from the mouth of a giant guh head on a tall tower: over its pink tongue down a steep drop, a banked
                    turn, a second drop, a hump and a launch far into the pool: PLONS
      Roze Trechter from a tower down into two funnels shaped like guh faces looking up with their mouths wide open (you
                    circle down and get swallowed), then into the pink foam bath
      Glimtunnel    a dark tube with glowing stars spiralling round the tower of the Sterrenguh (a starry guh head),
                    light rings, and out into the pool
  - wolkenlifts in the three towers take you up to the start gates (glijbaan_start)

check(s) is the geometry self-check (SystemExit on problems): the rides always have room (nothing in the way anywhere,
for every lateral), under every running part of a ride there is slide surface, every start gate stands on a floor and can
be reached (a wolkenlift column that ends at its platform), the NPC stands on a floor, the anchor is there, nothing
floats. Run it on its own:  python tools/features/knuffelbad_bouw.py   (from the project root)
"""
import math
import os
import random

import numpy as np

try:
    from features import knuffelbad_glij as kg
except ImportError:          # (run on its own: python tools/features/knuffelbad_bouw.py)
    import sys as _sys
    _sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    from features import knuffelbad_glij as kg

NAME = "knuffelbad"
W, H, D = 96, 72, 96
G = 4                               # the ground's top block (template y); the ground surface is at y = G + 1
ANCHOR = "guhs:knuffelbad_midden"
WATER_Y = G + 0.9                   # the pool's surface (world units in the template)

KS = "guhs:knuffelsteen"
KS_TRAP = "guhs:knuffelsteen_trap"
KS_PLAAT = "guhs:knuffelsteen_plaat"
KS_MUUR = "guhs:knuffelsteen_muur"
KS_GEZICHT = "guhs:knuffelsteen_gezicht"
PLUIS = "guhs:pluisdak"
PLUIS_TRAP = "guhs:pluisdak_trap"
PLUIS_PLAAT = "guhs:pluisdak_plaat"
KLINK = "guhs:knuffelklinkers"
GRAS = "guhs:knuffelgras"
TEGEL = "guhs:knuffelbad_badtegel"
SCHUIM = "guhs:knuffelbad_schuim"
TOBBE = "guhs:guh_wastobbe"
START = "guhs:glijbaan_start"
TRECHTER = kg.TRECHTERTEGEL
GLIM = kg.GLIMTEGEL
GOOT = kg.GOOT
WATER = "minecraft:water"
AIR = "minecraft:air"
LIFT = "guhs:wolkenlift"
STREAM = "guhs:wolkenstroom"
PARELMOER = "guhs:parelmoer_tegels"

SLIDES = ("roze_trechter", "glimtunnel", "grote_plons")
POOL = (46, 38, 89, 61)             # the big pool: water x 44..89, z 38..61 (rounded corners)
NPC_SPOT = (48, G + 2, 86)
GT_Y = 50                           # the Glimtunnel starts up here (its tower is as high)          # Badmeester Bubbel (on his podium at the back of the bath hall)


def mc(name):
    return "minecraft:" + name


# =====================================================================================================================
# the three slides
# =====================================================================================================================
def paden():
    """The slides: {id: dict(pad, start=(x, y, z) of the start gate, facing, ...)} in template coordinates."""
    out = {}

    # --- Grote Plons: out of the giant guh head's mouth, over its tongue ------------------------------------------------
    p = kg.Pad("grote_plons", 14.5, 48, 19.5, 180)
    p.zet(vpref=0.35, k=0.24).fx("start")
    p.recht(2.5, helling=-3)
    p.eendje(0.0, 1.0)
    p.fx("val", "spetters").zet(vpref=0.6)
    p.recht(3, helling=-62)
    p.rij(18, 4, [-0.7, 0.0, 0.7, 0.0])
    p.recht(19)
    p.recht(5, helling=-22)
    p.fx("spetters").zet(vpref=0.7, k=0.26)
    p.rij(15, 4, [0.8, 0.9, 0.9, 0.5])
    p.bocht(-90, 10, helling=-24)
    p.fx("val", "spetters")
    p.rij(10, 3, [-0.6, 0.6, 0.0])
    p.recht(4, helling=-50)
    p.recht(7)
    p.recht(5, helling=-4)
    p.fx("spetters").zet(vpref=0.55)
    p.rij(20, 6, [-0.9, -0.3, 0.3, 0.9, 0.6, 0.0])
    p.bocht(55, 6, helling=-2)            # an S-bend: right, then left
    p.bocht(-55, 6, helling=-2)
    p.recht(3, helling=9)
    p.recht(3, helling=-10)
    p.recht(3, helling=-3)
    p.recht(2, helling=8)
    p.merk["lanceer"] = p.s()
    sm = kg.Samples(p)
    ss, vs = sm.rit()
    p.eendje(0.0, 5.0)
    p.eendje(-0.5, 9.0)
    p.eendje(0.5, 9.0)
    p.vlucht(WATER_Y, vs[-1])
    p.merk["plons"] = p.s()
    p.fx("plons", "spetters")
    p.drijven(1.5)
    p.fx("spetters")
    p.drijven(5)
    p.uitstap = (int(p.x) + 0.5, G + 1.0, POOL[1] - 2.5, 180.0)
    out["grote_plons"] = dict(pad=p, start=(14, 48, 18), facing="south", kleur="blos")

    # --- Roze Trechter: two guh-face funnels, then the foam bath -----------------------------------------------------------
    p = kg.Pad("roze_trechter", 46.5, 36, 4.5, 180)
    p.zet(vpref=0.35).fx("start")
    p.recht(3, helling=-4)
    p.fx("spetters").zet(vpref=0.5)
    p.rij(9, 3, [0.0, -0.6, 0.6])
    p.recht(4, helling=-32)
    p.recht(3)
    p.recht(3, helling=-5)
    t1 = p.trechter(2.25, 8, 1.6, kegel=0.7, rechtsom=True, afvoer=5, kleuren=("roze", "wit"))
    for i in range(9):      # ducks round the funnel: high and low on the wall
        p.eendjes.append((t1["s_in"] + 6 + i * 8.5, [0.8, -0.2, -0.8, 0.3, 0.9, -0.5, 0.1, 0.7, -0.7][i]))
    p.zet(prof="goot", k=0.22, dmax=1.5, vpref=0.55).fx("spetters")
    p.recht(6, helling=-10)
    p.rij(24, 5, [-0.6, 0.0, 0.6, 0.9, 0.3])
    p.bocht(30, 8, helling=-10)
    p.recht(4, helling=-10)
    p.bocht(-67, 7, helling=-6)
    p.recht(1.5, helling=-4)
    t2 = p.trechter(2.0, 6.5, 1.5, kegel=0.75, rechtsom=False, tot_y=WATER_Y, kleuren=("blos", "wit"))
    for i in range(7):
        p.eendjes.append((t2["s_in"] + 5 + i * 7.5, [-0.8, 0.2, 0.8, -0.3, -0.9, 0.5, -0.1][i]))
    p.merk["plons"] = p.s()
    p.fx("plons", "schuim")
    p.drijven(1.0)
    p.fx("schuim")
    p.drijven(2.0)
    fx_, fz_ = t2["cx"], t2["cz"]
    p.uitstap = (math.floor(fx_ - 8.2) + 0.5, G + 1.0, math.floor(fz_) + 0.5, 270.0)
    out["roze_trechter"] = dict(pad=p, start=(46, 36, 3), facing="south", kleur="roze", trechters=[t1, t2])

    # --- Glimtunnel: a star tube spiralling round the Sterrenguh's tower ----------------------------------------------------
    p = kg.Pad("glimtunnel", 81.5, GT_Y, 9.5, 90)          # (the tube's outside stays inside the template: x <= 95)
    p.zet(prof="goot", k=0.22, dmax=1.2, vpref=0.35).fx("start")
    p.recht(1.5, helling=-4)
    midden = p.midden_van_bocht(90, 6.5)
    p.zet(prof="buis", R=2.0, dmax=1.25, vpref=0.58).fx("sterren", "donker")
    for lap in range(4):
        p.rij(40, 5, [0.2, 0.9, 0.5, -0.4, 0.0] if lap % 2 == 0 else [-0.9, -0.3, 0.6, 1.0, 0.2])
        p.bocht(360, 6.5, helling=-13, inloop=3.0 if lap == 0 else 0.0)
    rows = np.array(p.rows[int(8.0 / kg.DS):len(p.rows)], float)
    lap = int(round(2 * math.pi * 6.5 / kg.DS / math.cos(math.radians(13))))
    ring = rows[:lap * 3]
    midden = (float(ring[:, 0].mean()), float(ring[:, 2].mean()))
    p.bocht(135, 6.5, helling=-13, inloop=3.0)
    p.fx("sterren", "donker", "lichtring").zet(vpref=0.62)
    p.rij(24, 5, [0.0, 0.6, -0.6, 0.0, 0.8])
    p.recht(10, helling=-12)
    p.bocht(-45, 8, helling=-10)
    p.recht(4, helling=-3)
    p.merk["uit_buis"] = p.s()
    sm = kg.Samples(p)
    ss, vs = sm.rit()
    p.vlucht(WATER_Y, vs[-1])
    p.merk["plons"] = p.s()
    p.fx("plons", "spetters")
    p.drijven(1.2)
    p.fx("spetters")
    p.drijven(4)
    p.uitstap = (POOL[2] + 2.5, G + 1.0, int(p.z) + 0.5, 270.0)
    out["glimtunnel"] = dict(pad=p, start=(80, GT_Y, 9), facing="east", kleur="glim", midden=midden)
    return out


# =====================================================================================================================
# helpers
# =====================================================================================================================
class Bouw:
    """The structure plus what must stay free (the rides' room) and what belongs to a slide."""

    def __init__(self, h):
        self.s = h.Structure((W, H, D))
        self.h = h
        self.vrij = set()          # cells that must stay air (the rides)
        self.glij = set()          # cells of the slides (running surface, walls, tubes)
        self.rng = random.Random(28070)

    def set(self, x, y, z, name, props=None, nbt=None, force=False):
        if not force and (((x, y, z) in self.vrij and name != AIR) or (x, y, z) in self.glij):
            return
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)


def rotate_cell(x, z, facing, w, d):
    """A cell of a (w x d) sprite built facing north, turned to face `facing` (in the sprite's own box)."""
    if facing == "south":
        return w - 1 - x, d - 1 - z
    if facing == "east":
        return d - 1 - z, x
    if facing == "west":
        return z, w - 1 - x
    return x, z


def voxel_kop(h, scale, texture="guh.png", bones=("head", "ear_left", "ear_right")):
    """A giant guh head made of blocks (make_structures.voxel_guh), facing north: {(x, y, z): block}, size."""
    t = h.ms.Structure((200, 200, 200))
    nx, ny, nz = h.ms.voxel_guh(t, 0, 0, 0, scale=scale, bones=bones, texture=texture, bounds_bones=bones)
    cells = {k: v[0] for k, v in t.blocks.items()}
    return cells, (nx, ny, nz)


def plaats_kop(b, cells, size, cx, y0, cz, facing, hol=2, vloer=None, grond=False):
    """Puts a voxel head centred on (cx, cz) with its bottom at y0, facing `facing`. hol: hollow it out, keeping a shell of
    that many blocks (grond: it stands on the ground, so the inside goes all the way down). vloer: an inner floor height
    (template y) - the floor of the room inside. Returns the placed cells and the box (x, z, width, depth)."""
    nx, ny, nz = size
    w, d = (nx, nz) if facing in ("north", "south") else (nz, nx)
    ox, oz = int(round(cx - w / 2)), int(round(cz - d / 2))
    inside = set()
    if hol:
        solid = set(cells)
        for (x, y, z) in cells:
            deep = all((x + dx, y + dy, z + dz) in solid or (grond and y + dy < 0 and (x + dx, 0, z + dz) in solid)
                       for dx in range(-hol, hol + 1) for dy in range(-hol, hol + 1)
                       for dz in range(-hol, hol + 1) if abs(dx) + abs(dy) + abs(dz) <= hol)
            if deep:
                inside.add((x, y, z))
    placed = {}
    for (x, y, z), blk in cells.items():
        rx, rz = rotate_cell(x, z, facing, nx, nz)
        wx, wy, wz = ox + rx, y0 + y, oz + rz
        if (x, y, z) in inside:
            b.set(wx, wy, wz, AIR)
            placed[(wx, wy, wz)] = AIR
        else:
            b.set(wx, wy, wz, blk)
            placed[(wx, wy, wz)] = blk
    if vloer is not None:
        for (wx, wy, wz), blk in list(placed.items()):
            if wy == vloer and blk == AIR:
                b.set(wx, wy, wz, PARELMOER)
    return placed, (ox, oz, w, d)


def zet_gezicht(b, x, y, z, facing, stemming=0):
    b.set(x, y, z, KS_GEZICHT, {"facing": facing, "stemming": str(stemming)})


# =====================================================================================================================
# the slides' blocks
# =====================================================================================================================
def bouw_glijbanen(b, paden_):
    """Builds every slide from its path; returns {id: Samples}. The rides' room is kept free (b.vrij)."""
    samples = {}
    verboden = set()
    for sid in SLIDES:
        info = paden_[sid]
        sm = kg.Samples(info["pad"])
        samples[sid] = sm
        info["samples"] = sm
        info["ruimte"] = kg.vrije_ruimte(sm)
        verboden |= info["ruimte"]
    for sid in SLIDES:
        info = paden_[sid]
        sm = info["samples"]
        lucht = set()
        eigen = set()
        trechters = info.get("trechters", [])

        def in_trechter(x, z, y, _t=trechters):
            return any(math.hypot(x - t["cx"], z - t["cz"]) <= t["r_rand"] + 0.6 and t["y_bodem"] - 2.5 < y < t["y_rand"] + 2.5 for t in _t)
        claims = kg.voxel_goot(b.s, sm, info["kleur"], eigen, lucht, binnen_trechter=in_trechter if trechters else None, verboden=verboden)
        goot = {xz for xz in claims.cols}
        for t in trechters:
            kg.voxel_trechter(b.s, t, eigen, lucht, verboden=verboden, goot=goot)
        if sid == "glimtunnel":
            def raam(i, _sm=sm):
                return 20 < _sm.s[i] < 26 or 60 < _sm.s[i] < 64 or 100 < _sm.s[i] < 104
            kg.voxel_buis(b.s, sm, eigen, lucht, raam=raam, ring_elke=4, verboden=verboden, streep=PLUIS, streep_elke=6)
        info["eigen"] = eigen
        b.glij |= eigen
        info["lucht"] = lucht
    # air where the rides go (not the running surface itself): last, so nothing else can be in the way
    for sid in SLIDES:
        info = paden_[sid]
        for cell in info["lucht"] | info["ruimte"]:
            if b.s.get(*cell) == GOOT:
                continue
            b.vrij.add(cell)
    return samples


def maak_vrij(b):
    """Clears the rides' room (after everything was built)."""
    for (x, y, z) in b.vrij:
        if 0 <= x < W and 0 <= y < H and 0 <= z < D and b.s.get(x, y, z) != GOOT:
            b.s.set(x, y, z, AIR)


def zwevend(s):
    """The solid blocks that are not connected to the ground (6-neighbours), as connected groups (largest first)."""
    solids = {k for k, v in s.blocks.items() if solid(v[0])}
    reached = {k for k in solids if k[1] <= G}
    todo = list(reached)
    while todo:
        cx, cy, cz = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (cx + d[0], cy + d[1], cz + d[2])
            if n in solids and n not in reached:
                reached.add(n)
                todo.append(n)
    rest = solids - reached
    groups = []
    while rest:
        first = rest.pop()
        group, todo = {first}, [first]
        while todo:
            cx, cy, cz = todo.pop()
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (cx + d[0], cy + d[1], cz + d[2])
                if n in rest:
                    rest.discard(n)
                    group.add(n)
                    todo.append(n)
        groups.append(group)
    groups.sort(key=len, reverse=True)
    return groups


def verbind(b, paden_, rondes=60):
    """Pillars under everything that would float (bits of chute between the supports, a rim): for every floating group
    one pillar from the ground up to its lowest block that has a free column under it. Small leftovers go."""
    alle_ruimte = set()
    for sid in SLIDES:
        alle_ruimte |= paden_[sid]["ruimte"] | paden_[sid]["lucht"]
    for _ in range(rondes):
        groups = zwevend(b.s)
        if not groups:
            return
        progress = False
        for group in groups:
            done = False
            for (x, y, z) in sorted(group, key=lambda c: (c[1], c[0], c[2])):
                if (x, y - 1, z) in group:
                    continue
                cells = [(x, yy, z) for yy in range(G + 1, y)]
                if any(c in alle_ruimte or solid(b.s.get(*c)) for c in cells):
                    continue
                for c in cells:
                    b.set(*c, KS_MUUR, {"up": "true"}, force=True)
                if cells:
                    b.set(x, G + 1, z, KS, force=True)
                done = True
                break
            if not done and len(group) <= 6:
                for c in group:                  # (a crumb: gone)
                    b.s.blocks.pop(c, None)
                done = True
            progress |= done
        if not progress:
            return


def steunen(b, paden_):
    """Pillars under the chutes and tubes (every few blocks, where they are high above the ground) and under the funnels'
    rims: knuffelsteen walls on a knuffelsteen foot, a pink cap. A pillar that would be in a ride's way is left out."""
    alle_ruimte = set()
    for sid in SLIDES:
        alle_ruimte |= paden_[sid]["ruimte"] | paden_[sid]["lucht"]
    gedaan = set()

    def pilaar(x, z, top):
        if (x, z) in gedaan or top <= G + 1:
            return False
        cells = [(x, y, z) for y in range(G + 1, top + 1)]
        if any(c in alle_ruimte or b.s.get(*c) not in (None, AIR) for c in cells):
            return False
        for (cx, cy, cz) in cells:
            b.set(cx, cy, cz, KS_MUUR, {"up": "true"} if cy < top else {"up": "true"})
        b.set(x, G + 1, z, KS)
        b.set(x, top, z, KS)
        gedaan.add((x, z))
        return True

    for sid in SLIDES:
        info = paden_[sid]
        sm = info["samples"]
        eigen = info["eigen"]
        last = -99.0
        for i in range(len(sm.s)):
            if sm.prof[i] not in (kg.PROFIELEN["goot"], kg.PROFIELEN["buis"]):
                continue
            if sm.s[i] - last < 5.0:
                continue
            x, z = int(math.floor(sm.p[i][0])), int(math.floor(sm.p[i][2]))
            below = [y for (ex, y, ez) in eigen if ex == x and ez == z and y <= sm.p[i][1]]
            if not below:
                continue
            low = min(below)
            if low - 1 <= G + 1:
                last = sm.s[i]
                continue
            if pilaar(x, z, low - 1):
                last = sm.s[i]
        for t in info.get("trechters", []):
            for k in range(8):
                a = k / 8 * 2 * math.pi + 0.2
                x = int(math.floor(t["cx"] + (t["r_rand"] + 0.6) * math.cos(a)))
                z = int(math.floor(t["cz"] + (t["r_rand"] + 0.6) * math.sin(a)))
                below = [y for (ex, y, ez) in eigen if ex == x and ez == z]
                if below:
                    pilaar(x, z, min(below) - 1)


# =====================================================================================================================
# the ground, the pool and the foam bath
# =====================================================================================================================
def in_pool(x, z, r=4):
    x0, z0, x1, z1 = POOL
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cz = min(max(z, z0 + r), z1 - r)
    return math.hypot(x - cx, z - cz) <= r + 0.3


def schuimbad(paden_):
    t2 = paden_["roze_trechter"]["trechters"][1]
    return t2["cx"], t2["cz"], 5.6


def in_schuimbad(paden_, x, z):
    cx, cz, r = schuimbad(paden_)
    return math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= r


def bouw_terrein(b, paden_):
    """The foundation (pink wool, a knuffelsteen top layer) and the ground: knuffelgras lawns, knuffelklinkers paths,
    bath tiles round the pools, the guhzee's pink powder at the edge."""
    s = b.s
    for x in range(W):
        for z in range(D):
            for y in range(G - 1):
                s.set(x, y, z, mc("pink_wool"))
            s.set(x, G - 1, z, KS)
            edge = min(x, z, W - 1 - x, D - 1 - z)
            s.set(x, G, z, mc("pink_concrete_powder") if edge < 2 else GRAS)
    # paths (3 wide): the entrance from the south to the bath house, and round to the three towers
    paths = [((46, 90), (50, 95)),                 # entrance
             ((10, 62), (86, 64)),                 # the promenade between the pool and the bath house
             ((10, 22), (12, 64)),                 # west, up to the Grote Plons tower
             ((12, 22), (40, 24)),                 # to the Roze Trechter tower
             ((38, 8), (40, 24)),
             ((88, 22), (90, 64)),                 # east, to the Glimtunnel tower
             ((75, 18), (87, 20))]
    for (x0, z0), (x1, z1) in paths:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                s.set(x, G, z, KLINK)


def bouw_zwembad(b, paden_):
    s = b.s
    x0, z0, x1, z1 = POOL
    for x in range(x0 - 4, x1 + 5):
        for z in range(z0 - 4, z1 + 5):
            if in_pool(x, z):
                s.set(x, 1, z, TEGEL)
                for y in (2, 3, 4):
                    s.set(x, y, z, WATER)
            else:
                near = min((math.hypot(dx, dz) for dx in range(-4, 5) for dz in range(-4, 5) if in_pool(x + dx, z + dz)), default=99)
                if near <= 1.5:
                    for y in (1, 2, 3):
                        s.set(x, y, z, TEGEL)
                    s.set(x, G, z, TEGEL)
                elif near <= 3.2:
                    s.set(x, G, z, TEGEL if (x + z) % 7 else mc("light_blue_glazed_terracotta") if False else TEGEL)
    # a light blue stripe on the pool's floor (swimming lanes) and a big guh face on the bottom in the middle
    for x in range(x0 + 3, x1 - 2):
        for z in (z0 + 8, z1 - 8):
            if in_pool(x, z):
                s.set(x, 1, z, mc("light_blue_concrete"))
    cxp, czp = (x0 + x1) // 2 + 6, (z0 + z1) // 2
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            r = math.hypot(dx, dz)
            if r <= 5.6:
                s.set(cxp + dx, 1, czp + dz, mc("pink_concrete"))
            for ex in (-2, 2):
                if math.hypot(dx - ex, dz + 1.5) <= 1.1:
                    s.set(cxp + dx, 1, czp + dz, mc("black_concrete"))
            if abs(dz - 2) <= 0 and abs(dx) <= 2:
                s.set(cxp + dx, 1, czp + dz, mc("black_concrete"))
            for ex in (-4, 4):
                if math.hypot(dx - ex, dz - 1) <= 0.8:
                    s.set(cxp + dx, 1, czp + dz, mc("magenta_concrete"))
    # the foam bath under the second funnel: pink foam on warm water, bath tiles round it
    cx, cz, r = schuimbad(paden_)
    for x in range(int(cx - r - 4), int(cx + r + 5)):
        for z in range(int(cz - r - 4), int(cz + r + 5)):
            d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
            if d <= r:
                s.set(x, 1, z, TEGEL)
                s.set(x, 2, z, WATER)
                s.set(x, 3, z, WATER)
                s.set(x, G, z, SCHUIM)
            elif d <= r + 1.2:
                for y in (1, 2, 3, G):
                    s.set(x, y, z, TRECHTER)
            elif d <= r + 3.0:
                s.set(x, G, z, TEGEL)


# =====================================================================================================================
# the towers (with a wolkenlift up to each start gate)
# =====================================================================================================================
def lift(b, x, z, vloer, facing):
    """A wolkenlift: the pad in the ground, the stream up to 2 above the floor block `vloer`, puffing you off to `facing`."""
    b.set(x, G, z, LIFT, {"facing": facing, "down": "false"}, force=True)
    for y in range(G + 1, vloer + 3):
        b.set(x, y, z, STREAM, {"facing": facing, "down": "false"}, force=True)
    b.set(x, vloer + 3, z, AIR, force=True)
    return (x, z, vloer)


def ronde_toren(b, cx, cz, r, y0, y1, muur=KS, streep=PLUIS, ramen=True, deur=None):
    """A round tower: wall between r-1 and r, hollow inside, a pluisdak ring every 7 blocks, pink windows in a spiral."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
            if d > r:
                continue
            ang = math.atan2(z + 0.5 - cz, x + 0.5 - cx)
            for y in range(y0, y1 + 1):
                if d > r - 1.1:
                    blk = streep if (y - y0) % 7 == 6 else muur
                    if ramen and (y - y0) % 7 in (2, 3) and abs(((ang + (y - y0) * 0.45) % (math.pi / 2)) - math.pi / 4) < 0.18:
                        blk = mc("pink_stained_glass")
                    b.set(x, y, z, blk)
                else:
                    b.set(x, y, z, AIR)
    if deur:
        dx, dz = deur
        for y in (y0, y0 + 1, y0 + 2):
            for k in (-1, 0, 1):
                px, pz = int(math.floor(cx + dx * (r - 0.5) + (-dz) * k)), int(math.floor(cz + dz * (r - 0.5) + dx * k))
                b.set(px, y, pz, AIR)


def bouw_torens(b, paden_, h):
    lifts = {}
    # --- Grote Plons: a round tower with a giant pink guh head on top; the ride leaves through its mouth ---------------------
    gp = paden_["grote_plons"]
    gx, gz = 14.5, 11.0
    ronde_toren(b, gx, gz, 4.4, G + 1, 46, deur=(1, 0))
    cells, size = voxel_kop(h, 1.2)
    placed, box = plaats_kop(b, cells, size, gx, 44, 10.5, "south", hol=2)
    for (x, y, z), blk in placed.items():          # a fluffy pink guh (the texture's shading as pink wool)
        if blk == mc("purpur_block"):
            b.set(x, y, z, mc("pink_wool"))
    # the floor of the room inside the head (above the tower): up to y 47, the start gate at the mouth
    for (x, y, z), blk in placed.items():
        if blk == AIR and y <= 47:
            b.set(x, y, z, PARELMOER if y == 47 else KS)
    lifts["grote_plons"] = lift(b, 14, 11, 47, "south")
    for y in range(G + 1, 47):
        b.set(14, y, 11, STREAM, {"facing": "south", "down": "false"}, force=True)
    sx, sy, sz = gp["start"]
    for x in range(sx - 2, sx + 3):
        for z in range(sz - 6, sz + 1):
            if b.s.get(x, sy - 1, z) in (None, AIR):
                b.set(x, sy - 1, z, PARELMOER)
            for y in range(sy, sy + 3):
                if b.s.get(x, y, z) not in (None, AIR) and (x, z) != (14, 11):
                    b.set(x, y, z, AIR)
    b.set(sx, sy, sz, START, {"facing": gp["facing"], "glijbaan": "grote_plons"}, force=True)
    for (lx, lz) in ((sx - 2, sz), (sx + 2, sz)):
        b.set(lx, sy, lz, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})

    # --- Roze Trechter: a square tower on the north edge with a canopy -----------------------------------------------------
    rt = paden_["roze_trechter"]
    sx, sy, sz = rt["start"]
    x0, x1, z0, z1 = sx - 5, sx + 4, 0, sz + 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wall = x in (x0, x1) or z in (z0, z1)
            for y in range(G + 1, sy):
                if wall:
                    blk = PLUIS if (y - G) % 8 == 0 else KS
                    if (y - G) % 8 in (3, 4) and (x - x0) % 3 == 1 and z in (z0, z1):
                        blk = mc("pink_stained_glass")
                    b.set(x, y, z, blk)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, sy - 1, z, KS if wall else PARELMOER)
    for (fx_, fy, fz, f) in ((x0 + 2, 14, z1, "south"), (x1 - 2, 14, z1, "south"), (x0 + 2, 26, z1, "south"), (x1 - 2, 26, z1, "south"),
                              (x0, 20, z0 + 2, "west"), (x1, 20, z0 + 2, "east")):
        zet_gezicht(b, fx_, fy, fz, f, (fy // 6) % 4)
    for y in (G + 1, G + 2, G + 3):
        for x in (sx - 1, sx, sx + 1):
            b.set(x, y, z1, AIR)          # the door (south)
    lifts["roze_trechter"] = lift(b, x0 + 1, z0 + 1, sy - 1, "east")
    b.set(sx, sy, sz, START, {"facing": rt["facing"], "glijbaan": "roze_trechter"}, force=True)
    # railing and a canopy over the platform (pluisdak, stripes), with guh ears on top
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z == z0) and (x, z) != (x0 + 1, z0 + 1):
                b.set(x, sy, z, KS_MUUR, {"up": "true"})
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0, z1 + 2):
            b.set(x, sy + 5, z, PLUIS_PLAAT, {"type": "bottom"})
    for (px, pz) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        for y in range(sy + 1, sy + 5):
            b.set(px, y, pz, KS_MUUR, {"up": "true"})
    for ex in (x0 + 1, x1 - 1):
        for dy in range(0, 4):
            for dx in (-1, 0, 1):
                if abs(dx) + dy <= 3:
                    b.set(ex + dx, sy + 6 + dy, z0 + 3, PLUIS if dy < 3 else mc("magenta_wool"))

    # --- Glimtunnel: a slim tower in the middle of the helix, the Sterrenguh's head on top -----------------------------------
    gt = paden_["glimtunnel"]
    mx, mz = gt["midden"]
    ronde_toren(b, mx, mz, 3.4, G + 1, GT_Y - 1, muur=KS, streep=GLIM, ramen=False, deur=(-1, 0))
    for x in range(int(mx - 4), int(mx + 5)):
        for z in range(int(mz - 4), int(mz + 5)):
            if math.hypot(x + 0.5 - mx, z + 0.5 - mz) <= 3.4:
                b.set(x, GT_Y - 1, z, PARELMOER)
    # the neck (in the middle) and the head of the Sterrenguh (a starry guh, looking south over the pool) up above
    for x in range(int(mx - 3), int(mx + 4)):
        for z in range(int(mz - 3), int(mz + 4)):
            if math.hypot(x + 0.5 - mx, z + 0.5 - mz) <= 1.3:
                for y in range(GT_Y, GT_Y + 7):
                    b.set(x, y, z, GLIM)
    cells, size = voxel_kop(h, 0.8, texture="guh_starry.png")
    plaats_kop(b, cells, size, mx, GT_Y + 7, mz + 1, "south", hol=0)
    # from the tower top a little bridge north, over the tube, to the balcony with the start gate
    sx, sy, sz = gt["start"]
    for x in range(sx - 2, sx + 1):
        for z in range(sz - 1, int(mz) - 2):
            b.set(x, sy - 1, z, PARELMOER)
    for z in range(sz - 1, int(mz) - 2):
        b.set(sx - 3, sy, z, KS_MUUR, {"up": "true"})
    b.set(sx, sy, sz, START, {"facing": gt["facing"], "glijbaan": "glimtunnel"}, force=True)
    lifts["glimtunnel"] = lift(b, int(mx) - 2, int(mz), GT_Y - 1, "north")
    return lifts


# =====================================================================================================================
# the bath house: a giant guh head (the bath hall) between two wings
# =====================================================================================================================
BADHUIS_KOP = (48.0, 80.0)
TOBBEN = [(41, 80, "east"), (55, 80, "west"), (42, 75, "east"), (54, 75, "west")]
FONTEIN = (48, 78)


def vleugel(b, x0, x1, z0, z1, hoog=7, deuren=()):
    """A wing: knuffelsteen walls with windows and guh faces, a pluisdak gable roof, a tiled floor. deuren: (x, z) of
    doors (3 wide along the wall they are in)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wall = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, PARELMOER if not wall else KS)
            for y in range(G + 1, G + 1 + hoog):
                if wall:
                    blk = KS
                    if y in (G + 3, G + 4) and ((x - x0) % 4 == 2 or (z - z0) % 4 == 2) and not (x in (x0, x1) and z in (z0, z1)):
                        blk = mc("pink_stained_glass")
                    if y == G + 1 + hoog - 1:
                        blk = PLUIS
                    b.set(x, y, z, blk)
                else:
                    b.set(x, y, z, AIR)
    # gable roof along x
    top = G + 1 + hoog
    mid = (z0 + z1) / 2
    half = (z1 - z0) / 2 + 1
    for k in range(int(half) + 1):
        y = top + k
        for x in range(x0 - 1, x1 + 2):
            for z in (int(math.floor(mid - half + k)), int(math.ceil(mid + half - k))):
                if z0 - 1 <= z <= z1 + 1:
                    facing = "south" if z < mid else "north"
                    b.set(x, y, z, PLUIS_TRAP, {"facing": facing, "half": "bottom", "shape": "straight"})
            if k < half:
                for z in range(int(math.floor(mid - half + k)) + 1, int(math.ceil(mid + half - k))):
                    if x in (x0 - 1, x1 + 1):
                        b.set(x, y, z, KS)
    # guh faces on the long walls and on the gables, round windows
    for x in range(x0 + 3, x1 - 1, 5):
        zet_gezicht(b, x, G + 5, z0, "north", (x // 5) % 4)
        zet_gezicht(b, x, G + 5, z1, "south", (x // 5 + 1) % 4)
    for xg, f in ((x0 - 1, "west"), (x1 + 1, "east")):
        zet_gezicht(b, xg, top + 3, int(mid), f, 0)
    for (dx_, dz_) in deuren:
        for y in (G + 1, G + 2, G + 3):
            for k in (-1, 0, 1):
                if dx_ in (x0, x1):
                    b.set(dx_, y, dz_ + k, AIR)
                else:
                    b.set(dx_ + k, y, dz_, AIR)


def gang(b, x0, x1, z0, z1):
    """A short corridor with a flat roof (between a wing and the head)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wall = z in (z0, z1)
            b.set(x, G, z, PARELMOER if not wall else KS)
            for y in range(G + 1, G + 5):
                b.set(x, y, z, (mc("pink_stained_glass") if y == G + 2 else KS) if wall else AIR)
            b.set(x, G + 5, z, PLUIS_PLAAT, {"type": "bottom"})


def bouw_badhuis(b, h):
    """The Badhuis: a giant guh head (sunk a little into the ground, so the hall inside is wide) whose mouth is the door to
    the pool, two wings joined to it by corridors."""
    vleugel(b, 6, 26, 72, 90, deuren=((26, 80), (16, 90)))
    vleugel(b, 70, 90, 72, 90, deuren=((70, 80), (80, 90)))
    gang(b, 26, 35, 78, 82)
    gang(b, 61, 70, 78, 82)
    cx, cz = BADHUIS_KOP
    cells, size = voxel_kop(h, 1.8)
    remap = {mc("purpur_block"): KS, mc("pink_wool"): KS, mc("black_concrete"): mc("black_stained_glass"),
             mc("light_blue_concrete"): mc("light_blue_stained_glass"), mc("gray_concrete"): mc("black_stained_glass")}
    cells = {k: remap.get(v, v) for k, v in cells.items()}
    y0 = 0
    placed, box = plaats_kop(b, cells, size, cx, y0, cz, "north", hol=2, grond=True)
    ox, oz, w, d = box
    # below the floor it's solid ground again; the floor of the hall is pearl tiles
    for (x, y, z), blk in placed.items():
        if y < G:
            b.set(x, y, z, KS)
        elif y == G:
            b.set(x, y, z, PARELMOER if blk == AIR else KS)
    # doors: the mouth (north, to the pool) and the back (south, the entrance); side doors to the corridors
    for y in range(G + 1, G + 5):
        for x in range(45, 52):
            for z in range(oz - 1, oz + 5):
                if placed.get((x, y, z), AIR) != AIR:
                    b.set(x, y, z, AIR)
            for z in range(oz + d - 5, oz + d + 1):
                if placed.get((x, y, z), AIR) != AIR and 46 <= x <= 50:
                    b.set(x, y, z, AIR)
        for z in range(79, 82):
            for x in list(range(ox - 1, ox + 5)) + list(range(ox + w - 5, ox + w + 1)):
                if placed.get((x, y, z), AIR) != AIR:
                    b.set(x, y, z, AIR)
    for x in range(44, 53):             # the mouth's lip: a pink step and a tongue carpet
        b.set(x, G, oz + 1, mc("pink_concrete"))
    # inside: the foam fountain in the middle, the four wash tubs round it, Badmeester Bubbel's podium at the back
    fx_, fz_ = FONTEIN
    for x in range(fx_ - 3, fx_ + 4):
        for z in range(fz_ - 3, fz_ + 4):
            dd = math.hypot(x - fx_, z - fz_)
            if dd < 0.5:
                b.set(x, G, z, TRECHTER)
            elif dd <= 2.4:
                b.set(x, G, z, SCHUIM)
                b.set(x, G - 1, z, WATER)
            elif dd <= 3.4:
                b.set(x, G, z, TRECHTER)
                b.set(x, G + 1, z, TRECHTER if (x + z) % 2 else PLUIS_PLAAT, {} if (x + z) % 2 else {"type": "bottom"})
    for y in range(G + 1, G + 4):
        b.set(fx_, y, fz_, mc("pink_stained_glass"))
    b.set(fx_, G + 4, fz_, "guhs:guh_kristal_lamp")
    for (x, z, f) in TOBBEN:
        b.set(x, G + 1, z, TOBBE, {"facing": f, "vulling": "leeg"})
        b.set(x, G + 1, z + 1, "guhs:white_kussen", {"facing": f})
    nx, ny, nz = NPC_SPOT
    for x in range(nx - 1, nx + 2):
        for z in range(nz - 1, nz + 2):
            b.set(x, ny - 1, z, KS)
    for x in range(nx - 1, nx + 2):
        b.set(x, ny - 1, nz - 2, KS_TRAP, {"facing": "south", "half": "bottom", "shape": "straight"})
    b.set(nx - 2, ny, nz, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    b.set(nx + 2, ny, nz, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    # hanging lampions under the dome
    for (lx, lz) in ((40, 76), (56, 76), (42, 84), (54, 84), (48, 73)):
        for y in range(G + 26, G + 8, -1):
            if b.s.get(lx, y, lz) not in (None, AIR):
                for yy in range(y - 1, y - 4, -1):
                    b.set(lx, yy, lz, mc("chain"), {"axis": "y", "waterlogged": "false"})
                b.set(lx, y - 4, lz, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
                break
    # the west wing: a cafe (tables, chairs, cake) and changing cabins; the east wing: two indoor foam baths
    for (x, z) in ((10, 76), (16, 76), (10, 83), (16, 83)):
        b.set(x, G + 1, z, "guhs:guh_tafel", {"facing": "north"})
        b.set(x - 1, G + 1, z, "guhs:guh_stoel", {"facing": "east"})
        b.set(x + 1, G + 1, z, "guhs:guh_stoel", {"facing": "west"})
    b.set(10, G + 2, 76, "guhs:guh_taart", {"bites": "0"})
    b.set(16, G + 2, 83, "guhs:guh_taart", {"bites": "1"})
    for x in range(19, 26, 3):             # changing cabins: little booths with a cushion
        for z in range(84, 90):
            for y in range(G + 1, G + 4):
                if z in (84,) or x in (19,):
                    pass
        for y in range(G + 1, G + 4):
            b.set(x, y, 86, KS if y < G + 3 else PLUIS)
            b.set(x, y, 87, KS if y < G + 3 else PLUIS)
            b.set(x, y, 88, KS if y < G + 3 else PLUIS)
        b.set(x + 1, G + 1, 88, "guhs:pink_kussen", {"facing": "north"})
    for (bx, bz) in ((76, 81), (84, 81)):
        for x in range(bx - 2, bx + 3):
            for z in range(bz - 3, bz + 4):
                edge = x in (bx - 2, bx + 2) or z in (bz - 3, bz + 3)
                if edge:
                    b.set(x, G + 1, z, TRECHTER)
                else:
                    b.set(x, G - 1, z, WATER)
                    b.set(x, G, z, SCHUIM)


# =====================================================================================================================
# decoration: parasols, lounge chairs, lamps, flower boxes, a lifeguard chair, a giant rubber duck
# =====================================================================================================================
def parasol(b, x, z, kleur="pink"):
    if not solid(b.s.get(x, G, z)):
        return
    for y in range(G + 1, G + 4):
        b.set(x, y, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) + abs(dz) <= 3:
                b.set(x + dx, G + 4, z + dz, mc(f"{kleur}_wool") if (dx + dz) % 2 == 0 else mc("white_wool"))
    b.set(x, G + 5, z, mc(f"{kleur}_wool"))


def ligstoel(b, x, z, facing):
    b.set(x, G + 1, z, KS_TRAP, {"facing": facing, "half": "bottom", "shape": "straight"})
    step = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[facing]
    b.set(x + step[0], G + 1, z + step[1], "guhs:white_kussen", {"facing": facing})


def lamp(b, x, z, kleur="roze"):
    if not solid(b.s.get(x, G, z)):
        return
    for y in range(G + 1, G + 4):
        b.set(x, y, z, KS_MUUR, {"up": "true"})
    b.set(x, G + 4, z, f"guhs:lampion_{kleur}", {"hanging": "false", "waterlogged": "false"})


def eend_standbeeld(b, x0, z0):
    """A giant rubber duck (yellow, orange beak, a guh's pink blush) on the lawn by the entrance."""
    geel, oranje = mc("yellow_concrete"), mc("orange_concrete")
    for x in range(x0 - 5, x0 + 6):
        for y in range(G + 1, G + 12):
            for z in range(z0 - 4, z0 + 5):
                px, py, pz = x - x0, y - (G + 1), z - z0
                body = (px / 5.2) ** 2 + ((py - 2.6) / 2.9) ** 2 + (pz / 3.8) ** 2 <= 1
                tail = (px + 4.4) ** 2 / 2.2 + (py - 4.6) ** 2 / 3 + pz ** 2 / 2.5 <= 1
                head = (px - 2.6) ** 2 + (py - 7.2) ** 2 + pz ** 2 <= 2.9 ** 2
                beak = 5.0 <= px <= 6.6 and 6.2 <= py <= 7.4 and abs(pz) <= 1.2
                if body or tail or head:
                    b.set(x, y, z, geel)
                if beak:
                    b.set(x, y, z, oranje)
    for dz in (-2, 2):
        b.set(x0 + 5, G + 9, z0 + dz, mc("black_concrete"))      # the eyes
        b.set(x0 + 4, G + 8, z0 + 2 * dz // abs(dz) + dz // 2, mc("pink_concrete"))     # a guh's blush


def decoratie(b, paden_):
    x0, z0, x1, z1 = POOL
    for i, x in enumerate(range(x0 + 2, x1 - 1, 9)):
        parasol(b, x, z1 + 3, ("pink", "magenta", "light_blue")[i % 3])
        ligstoel(b, x + 2, z1 + 3, "north")
        ligstoel(b, x - 2, z1 + 3, "north")
    for x in range(x0 - 3, x1 + 4, 6):
        lamp(b, x, z0 - 4, "roze" if (x // 6) % 2 else "geel")
    for z in range(z0 + 2, z1, 7):
        lamp(b, x1 + 4, z, "mint")
    for x in range(12, 90, 8):
        b.set(x, G + 1, 65, "guhs:seizoensbloembak", {"seizoen": "zomer"})
    # the lifeguard chair at the pool's south-west corner
    lx, lz = x0 - 3, z1 + 1
    for y in range(G + 1, G + 5):
        for (dx, dz) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            b.set(lx + dx, y, lz + dz, mc("cherry_fence") if y < G + 4 else mc("white_wool"))
    b.set(lx, G + 5, lz, KS_TRAP, {"facing": "south", "half": "bottom", "shape": "straight"})
    parasol(b, lx, lz - 1, "red")
    eend_standbeeld(b, 64, 93 - 4)
    # bunting between the lamp posts along the promenade
    for x in range(12, 88):
        if x % 8 != 0:
            b.set(x, G + 4, 64, "guhs:vlaggetjes", {"axis": "x"})


BLOEMEN = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:guhoortjes", "minecraft:pink_tulip", "minecraft:allium"]
BLAD = {"distance": "1", "persistent": "true", "waterlogged": "false"}


def entree(b):
    """The way in (south): a portico with pink columns, a fluffy roof with little guh ears, lampions, a row of guh faces
    over the door and flower boxes along the path."""
    x0, x1, z0, z1 = 43, 53, 91, 94
    for (x, z) in ((x0, z0), (x0, z1), (x1, z0), (x1, z1)):
        for y in range(G + 1, G + 6):
            b.set(x, y, z, KS_MUUR, {"up": "true"})
        b.set(x, G + 1, z, KS)
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            b.set(x, G + 6, z, PLUIS_PLAAT, {"type": "bottom"})
        b.set(x, G + 6, z0 - 1, PLUIS)
    for x in range(x0, x1 + 1, 2):
        zet_gezicht(b, x, G + 7, z1 + 1, "south", (x // 2) % 4)
    for ex in (x0 + 1, x1 - 1):
        for dy in range(3):
            for dx in (-1, 0, 1):
                if abs(dx) + dy <= 2:
                    b.set(ex + dx, G + 7 + dy, z1, PLUIS if dy < 2 else mc("magenta_wool"))
    for (x, z) in ((x0, z1 + 1), (x1, z1 + 1)):
        b.set(x, G + 5, z, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    for z in range(z0, 96, 2):
        for x in (45, 51):
            if b.get(x, G + 1, z) in (None, AIR) and solid(b.get(x, G, z)):
                b.set(x, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "zomer"})


def boompje(b, x, z, hoogte=5, r=2.6):
    """A pluizenboom: a trunk and a round, fluffy pink crown (only where there is room)."""
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if not (0 <= x + dx < W and 0 <= z + dz < D) or b.get(x + dx, G, z + dz) != GRAS:
                return False
            for y in range(G + 1, G + hoogte + 4):
                if b.get(x + dx, y, z + dz) not in (None, AIR) or (x + dx, y, z + dz) in b.vrij:
                    return False
    for y in range(G + 1, G + 1 + hoogte):
        b.set(x, y, z, "guhs:pluizenboom_stam", {"axis": "y"})
    cy = G + hoogte + 1.5
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-2, 3):
                if (dx * dx + dz * dz) / (r * r) + (dy * dy) / (1.8 ** 2) <= 1.0 and b.get(x + dx, int(cy + dy), z + dz) in (None, AIR):
                    b.set(x + dx, int(cy + dy), z + dz, "guhs:pluizenboom_bladeren", BLAD)
    return True


def groen(b):
    """Pluizenbomen along the edges and flowers, fluffy grass and little guh mushrooms on the lawns."""
    for (x, z) in ((5, 30), (5, 44), (5, 58), (22, 34), (30, 60), (36, 44), (60, 4), (68, 6), (92, 44), (92, 58), (70, 36), (4, 92), (92, 92),
                   (26, 4), (30, 26), (93, 30)):
        boompje(b, x, z, hoogte=4 + (x + z) % 3)
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if b.get(x, G, z) == GRAS and b.get(x, G + 1, z) in (None, AIR) and (x, G + 1, z) not in b.vrij and b.rng.random() < 0.16:
                r = b.rng.random()
                b.set(x, G + 1, z, "guhs:pluisgras" if r < 0.55 else b.rng.choice(BLOEMEN) if r < 0.94 else "guhs:guhpaddenstoel")


def bewoners(b, paden_):
    """The NPC, rubber ducks floating in the pool and the foam baths, lily pads."""
    s = b.s
    nx, ny, nz = NPC_SPOT
    s.entity(nx + 0.5, float(ny), nz + 0.5, {"id": "guhs:guh_npc", "Kind": "badmeesterguh", "PersistenceRequired": b.h.ms.Byte(1),
                                             "Rotation": b.h.ms.floats(180.0, 0.0)})
    x0, z0, x1, z1 = POOL
    rng = random.Random(28071)
    laag = [(sm.p[i][0], sm.p[i][2]) for sid in SLIDES for sm in [paden_[sid]["samples"]] for i in range(len(sm.s)) if sm.p[i][1] < 9]
    spots = []
    for _ in range(40):
        x, z = rng.randint(x0 + 2, x1 - 2), rng.randint(z0 + 2, z1 - 2)
        if any(math.hypot(x + 0.5 - lx, z + 0.5 - lz) < 4.0 for lx, lz in laag):       # (not where the slides land)
            continue
        spots.append((x, z))
        if len(spots) >= 14:
            break
    for i, (x, z) in enumerate(spots[:8]):
        s.entity(x + 0.5, 4.9, z + 0.5, {"id": "guhs:badeendje", "Soort": b.h.ms.Byte(0), "Deco": b.h.ms.Byte(1),
                                         "Rotation": b.h.ms.floats(float(rng.randint(0, 359)), 0.0)})
    for (x, z) in spots[8:]:
        if b.s.get(x, G, z) == WATER and b.s.get(x, G + 1, z) in (None, AIR):
            b.set(x, G + 1, z, "guhs:guh_waterlelie")
    cx, cz, r = schuimbad(paden_)
    s.entity(cx + 3.2, 4.9, cz - 2.5, {"id": "guhs:badeendje", "Soort": b.h.ms.Byte(0), "Deco": b.h.ms.Byte(1),
                                       "Rotation": b.h.ms.floats(40.0, 0.0)})


# =====================================================================================================================
# the whole template
# =====================================================================================================================
def template(h):
    b = Bouw(h)
    paden_ = paden()
    bouw_terrein(b, paden_)
    for sid in SLIDES:                 # where you get off: a little square of path tiles, kept free
        ux, _uy, uz, _yaw = paden_[sid]["pad"].uitstap
        cx, cz = int(math.floor(ux)), int(math.floor(uz))
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if b.get(cx + dx, G, cz + dz) == GRAS:
                    b.s.set(cx + dx, G, cz + dz, KLINK)
                for y in (G + 1, G + 2):
                    b.vrij.add((cx + dx, y, cz + dz))
    bouw_glijbanen(b, paden_)
    bouw_zwembad(b, paden_)
    lifts = bouw_torens(b, paden_, h)
    bouw_badhuis(b, h)
    decoratie(b, paden_)
    entree(b)
    steunen(b, paden_)
    groen(b)
    maak_vrij(b)
    s = b.s
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    maak_vrij(b)
    verbind(b, paden_)
    s.set(48, G, 48, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "minecraft:water", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    bewoners(b, paden_)
    return s, dict(b=b, paden=paden_, lifts=lifts)


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
PASSABLE = {None, AIR, WATER, SCHUIM, STREAM, "guhs:guh_waterlelie", "guhs:vlaggetjes"}
FLUIDS = {WATER, STREAM, AIR, None}


def solid(blk):
    return blk not in PASSABLE and not str(blk).startswith("guhs:lampion") and blk != mc("chain")


_DUN = ("flower", "tulip", "poppy", "dandelion", "allium", "orchid", "daisy", "cornflower", "lily_of", "sapling", "short_grass", "tall_grass",
        "fern", "torch", "lantern", "sign", "button", "rail", "vine", "candle", "banner", "pressure_plate", "ladder", "petals", "bush", "rose")


def _dicht(s, x, y, z):
    """Is this point inside something you can't see through (for the rider's camera)? Layers and slabs by their height."""
    c = (math.floor(x), math.floor(y), math.floor(z))
    b = s.blocks.get(c)
    if b is None or not solid(b[0]) or any(w in b[0] for w in _DUN):
        return False
    name, props = b[0], b[1]
    top = 1.0
    if name == GOOT:
        top = int(props.get("lagen", 8)) / 8.0
    elif (name.endswith("_slab") or name.endswith("_plaat")) and props.get("type", "bottom") == "bottom":
        top = 0.5
    elif name.endswith("_carpet"):
        top = 1 / 16
    return y - c[1] < top


def check(s, info):
    problems = []
    paden_ = info["paden"]
    get = s.get
    # 1. every ride has room everywhere (anything of its own slide is fine: the steep drops' stairs touch the ring)
    for sid in SLIDES:
        inf = paden_[sid]
        eigen = inf["eigen"]
        bad = [c for c in inf["ruimte"] if 0 <= c[1] < H and solid(get(*c)) and c not in eigen]
        for c in bad[:12]:
            problems.append(f"{sid}: in the ride's way: {get(*c)} at {c}")
        if len(bad) > 12:
            problems.append(f"{sid}: ... {len(bad)} cells in the way in all")
        # 2. under every running part of the ride there is running surface
        sm = inf["samples"]
        missing = 0
        gaten = [(t["cx"], t["cz"], t["r_gat"] + 0.6) for t in inf.get("trechters", [])]
        for i in range(len(sm.s)):
            if sm.prof[i] not in (kg.PROFIELEN["goot"], kg.PROFIELEN["trechter"], kg.PROFIELEN["buis"]):
                continue
            if any(math.hypot(sm.p[i][0] - gx, sm.p[i][2] - gz) < gr for gx, gz, gr in gaten):
                continue
            q = sm.p[i] - sm.u[i] * 0.12
            cell = (int(math.floor(q[0])), int(math.floor(q[1])), int(math.floor(q[2])))
            under = (cell[0], int(math.floor(q[1] - 0.6)), cell[2])
            if get(*cell) != GOOT and get(*under) != GOOT:
                missing += 1
                if missing <= 6:
                    problems.append(f"{sid}: no running surface under the ride at s={sm.s[i]:.1f} {cell}: {get(*cell)}")
        # 2b. a tube is closed all round (its wall's inner layer), and every slide fits in the template
        gat = 0
        for i in range(len(sm.s)):
            p_, t_, u_ = sm.p[i], sm.t[i], sm.u[i]
            marge = (sm.R[i] + kg.BUIS_DIK + 0.5) if sm.prof[i] == kg.PROFIELEN["buis"] else 2.8
            c_ = p_ + u_ * (sm.R[i] if sm.prof[i] == kg.PROFIELEN["buis"] else 0.0)
            if c_[0] - marge < 0 or c_[0] + marge > W or c_[2] - marge < 0 or c_[2] + marge > D:
                problems.append(f"{sid}: the slide goes out of the template at s={sm.s[i]:.1f} ({c_[0]:.1f}, {c_[2]:.1f})")
                break
            if sm.prof[i] != kg.PROFIELEN["buis"] or sm.s[i] < 2.0 or sm.s[i] > sm.lengte - 2.0:
                continue
            R = sm.R[i]
            for x in range(int(c_[0]) - 4, int(c_[0]) + 5):
                for y in range(int(c_[1]) - 4, int(c_[1]) + 5):
                    for z in range(int(c_[2]) - 4, int(c_[2]) + 5):
                        q = np.array((x + 0.5, y + 0.5, z + 0.5)) - c_
                        along = float(q @ t_)
                        if abs(along) > kg.STAP * 0.9:
                            continue
                        rho = float(np.linalg.norm(q - along * t_))
                        if R + 0.3 < rho < R + 1.2 and not solid(get(x, y, z)) and get(x, y, z) != "minecraft:pink_stained_glass":
                            gat += 1
                            if gat <= 6:
                                problems.append(f"{sid}: a hole in the tube at s={sm.s[i]:.1f} {(x, y, z)}: {get(x, y, z)}")
        # 2c. the rider's camera is never inside a block: every tick of the ride, every lateral, the eyes (1.24 along the
        #     surface's normal, like ZwembandjeEntity) with a little box round them (the camera's near plane, the game's
        #     "in a wall" test)
        ticks, _v = sm.rit()
        oog = 0
        for sv in ticks:
            i = min(len(sm.s) - 1, int(round(sv / kg.STAP)))
            vrij_zwevend = sm.prof[i] in (kg.PROFIELEN["lucht"], kg.PROFIELEN["water"])
            for lat in ((0.0,) if vrij_zwevend else (-1.0, -0.5, 0.0, 0.5, 1.0)):
                pos, nrm = sm.punt(i, lat)
                eye = pos + nrm * 1.24
                if any(_dicht(s, eye[0] + dx, eye[1] + dy, eye[2] + dz) for dx in (-0.25, 0.25) for dy in (-0.1, 0.1) for dz in (-0.25, 0.25)):
                    oog += 1
                    if oog <= 4:
                        problems.append(f"{sid}: the rider's camera is in a block at s={sv:.1f} lateral {lat} {tuple(np.floor(eye).astype(int))}")
        # 3. the start gate: on a floor, facing the way the ride starts
        x, y, z = inf["start"]
        if get(x, y, z) != START or s.blocks[(x, y, z)][1].get("facing") != inf["facing"]:
            problems.append(f"{sid}: no start gate at {inf['start']}")
        if not solid(get(x, y - 1, z)):
            problems.append(f"{sid}: the start gate doesn't stand on a floor")
        # 4. where you get off: a floor and room above it
        ux, uy, uz, _yaw = inf["pad"].uitstap
        cell = (int(math.floor(ux)), int(math.floor(uy)), int(math.floor(uz)))
        if not solid(get(cell[0], cell[1] - 1, cell[2])) or solid(get(*cell)) or solid(get(cell[0], cell[1] + 1, cell[2])):
            problems.append(f"{sid}: the way off the ride at {cell} is not a free floor ({get(cell[0], cell[1] - 1, cell[2])}, {get(*cell)})")
        # 5. the wolkenlift: pad, whole stream column, air on top, a floor to land on that leads to the gate
        lx, lz, vloer = info["lifts"][sid]
        if get(lx, G, lz) != LIFT:
            problems.append(f"{sid}: no wolkenlift pad at {(lx, G, lz)}")
        for yy in range(G + 1, vloer + 3):
            if get(lx, yy, lz) != STREAM:
                problems.append(f"{sid}: the lift column is broken at {(lx, yy, lz)}: {get(lx, yy, lz)}")
                break
        if solid(get(lx, vloer + 3, lz)):
            problems.append(f"{sid}: something on top of the lift at {(lx, vloer + 3, lz)}")
        facing = s.blocks[(lx, G, lz)][1].get("facing", "north")
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        landing = (lx + dx, lz + dz)
        seen, todo = {landing}, [landing]
        while todo:
            cx, cz = todo.pop()
            for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (cx + ddx, cz + ddz)
                if n in seen or abs(n[0] - lx) > 14 or abs(n[1] - lz) > 14:
                    continue
                if solid(get(n[0], vloer, n[1])) and not solid(get(n[0], vloer + 1, n[1])) and not solid(get(n[0], vloer + 2, n[1])):
                    seen.add(n)
                    todo.append(n)
                elif get(n[0], vloer + 1, n[1]) == START:
                    seen.add(n)
        if not solid(get(landing[0], vloer, landing[1])):
            problems.append(f"{sid}: nothing to land on next to the lift at {(landing[0], vloer, landing[1])}")
        if y - 1 != vloer or (x, z) not in seen:
            problems.append(f"{sid}: the start gate can't be reached from the lift (floor {vloer}, gate {inf['start']})")
    # 6. Badmeester Bubbel stands on his podium, and the anchor is there
    nx, ny, nz = NPC_SPOT
    if not solid(get(nx, ny - 1, nz)) or solid(get(nx, ny, nz)) or solid(get(nx, ny + 1, nz)):
        problems.append(f"the Badmeester at {NPC_SPOT} doesn't stand on a floor")
    if get(48, G, 48) != mc("jigsaw"):
        problems.append("no anchor jigsaw at (48, G, 48)")
    # 7. the way in: from the south edge along the ground into the bath hall, to the podium and out to the pool
    start = (48, 95)
    seen, todo = {start}, [start]
    while todo:
        cx, cz = todo.pop()
        for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (cx + ddx, cz + ddz)
            if n in seen or not (0 <= n[0] < W and 0 <= n[1] < D):
                continue
            if solid(get(n[0], G, n[1])) and not solid(get(n[0], G + 1, n[1])) and not solid(get(n[0], G + 2, n[1])):
                seen.add(n)
                todo.append(n)
    for name, spot in (("the podium", (nx, nz - 3)), ("the pool", (POOL[0] + 10, POOL[3] + 2)), ("a wash tub", (TOBBEN[0][0] - 1, TOBBEN[0][1])),
                       ("the Grote Plons tower", (19, 11)), ("the Glimtunnel tower", (int(paden_["glimtunnel"]["midden"][0]) - 4, int(paden_["glimtunnel"]["midden"][1])))):
        if spot not in seen:
            problems.append(f"{name} can't be reached on foot from the entrance ({spot})")
    # 8. nothing floats: every solid block is connected to the ground
    solids = {k for k, v in s.blocks.items() if solid(v[0])}
    ground = [k for k in solids if k[1] <= G]
    reached = set(ground)
    todo = list(ground)
    while todo:
        cx, cy, cz = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (cx + d[0], cy + d[1], cz + d[2])
            if n in solids and n not in reached:
                reached.add(n)
                todo.append(n)
    floating = sorted(solids - reached)
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. " + ", ".join(f"{get(*c)}@{c}" for c in floating[:8]))
    return problems


def build_all(h, pad_dir):
    """Writes the template and the three path files; returns a summary. SystemExit when the self-check fails."""
    s, info = template(h)
    problems = check(s, info)
    if problems:
        raise SystemExit("knuffelbad geometry check failed:\n  " + "\n  ".join(problems[:60]))
    os.makedirs(pad_dir, exist_ok=True)
    summary = {}
    for sid in SLIDES:
        inf = info["paden"][sid]
        out = kg.exporteer(inf["samples"], inf["pad"], inf["start"], inf["facing"], os.path.join(pad_dir, f"{sid}.json"))
        ss, vs = inf["samples"].rit()
        summary[sid] = dict(lengte=round(inf["samples"].lengte, 1), ticks=len(ss), eendjes=len(out["eendjes"]), vmax=round(float(vs.max()), 2))
    s.save(NAME)
    return s, info, summary


if __name__ == "__main__":
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    stub = types.SimpleNamespace(Structure=ms.Structure, ms=ms, mc=ms.mc)
    tpl, inf = template(stub)
    found = check(tpl, inf)
    print("\n".join(found[:80]) if found else "geometry check ok")
    print(f"{len(tpl.blocks)} blocks, {len(tpl.entities)} entities")

