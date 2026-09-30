"""
Het Guh-Circuit (2.9) - the three race tracks: their shapes (a closed path of corners with rounded fillets), their height
profiles, and building them into the circuit template (road, kerbs, barriers, embankments or floating decks, checkpoint
rings, VAHOEG pads, rainbow boost rings, kaassaus, stuiterpaddenstoelen, the Vadslooping, the markers for the Java side),
plus the geometry self-check of every track.

  Regenboogbaan (baan 1)  north: starts on the ground, climbs into the sky, a floating rainbow road with two jumps, a
                          chicane, boost rings and Mika-pikkers on cloud platforms, and down again.
  Vadsbaan      (baan 2)  south-west: cheese road through Vadsland: the Vadslooping (a scripted corkscrew loop, see
                          RaceRit.java), slippery kaassaus hairpins in the notch, stuiterpaddenstoelen on the west leg.
  Kaasbergbaan  (baan 3)  south-east: around and over De Kaasberg: up the Knabbelhelling (Mika's roll kaasknabbels down
                          it), an ice stretch over the summit, and down the other side.

Everything here is in template coordinates (x east, z south, y up; the ground you walk on is G). Keep the numbers that
the Java side needs in sync with feature/circuit/CircuitBanen.java (starts, boxes, the looping's size).
"""
import math

import numpy as np

G = 4
STEP = 0.25
HALF, KERB, WALL = 3.5, 4.5, 5.5          # |offset| from the middle of the road: lanes, kerbs, barrier
# the Vadslooping (RaceRit.java: radius, length, side, ticks; DROP there too)
LOOP_R, LOOP_L, LOOP_W, LOOP_TICKS, LOOP_DROP = 7.0, 12.0, 8.0, 56, 3.6

TRACK_TAG = ["guhs:circuit_regenboogweg", "guhs:circuit_regenboogweg_plaat", "guhs:circuit_kaasweg", "guhs:circuit_kaasweg_plaat",
             "guhs:circuit_kaassaus", "guhs:circuit_bergijs", "guhs:circuit_stuiterpaddenstoel", "minecraft:smooth_sandstone",
             "minecraft:smooth_sandstone_slab", "minecraft:cut_red_sandstone", "minecraft:cut_red_sandstone_slab"]
RACE_TAG = ["minecraft:smooth_quartz", "minecraft:smooth_quartz_slab", "minecraft:purpur_block", "minecraft:purpur_slab",
            "guhs:race_vahoegpad", "guhs:race_checkpoint", "minecraft:white_concrete", "minecraft:black_concrete"]


class Baan:
    """One track: nodes = [(x, z, radius, flags)] in driving order (closed); flags "loop" = the edge to the next node is the looping."""

    def __init__(self, name, index, nodes, box, laps):
        # (the middle of the road runs through the middle of the blocks: 9 columns of road, the rings and pads line up)
        self.name, self.index, self.box, self.laps = name, index, box, laps
        self.nodes = [(x + 0.5, z + 0.5, r, flags) for x, z, r, flags in nodes]
        self.heights = []        # [(x, z, h2)] keyframes (half blocks above the ground), by the nearest point of the path
        self.gaps = []           # [(x, z, length)] jumps: no road here
        self.rings = []          # [(n, x, z)]
        self.pads = []           # [(x, z, works_on_lastig)]
        self.boosts = []         # [(x, z)] rainbow boost rings
        self.saus = []           # [(x0, z0, x1, z1)] kaassaus areas
        self.paddenstoelen = []  # [(x0, z0, x1, z1)] stuiterpaddenstoel stretch
        self.ijs = []            # [(x0, z0, x1, z1)] bergijs
        self.mikas = []          # [(x, z, side, vanaf)] Mika-pikker spots (side: +1 left of the driving direction, -1 right)
        self.rollen = []         # [(x, z, vanaf)] rolling kaasknabbel spots (they roll against the driving direction)
        self.start = None        # (x, z) of the start marker (faces the driving direction)
        self.floating = False    # a deck on pillars where it's high (the Regenboogbaan)
        self.samples = None
        self.cols = None

    # --- the path ------------------------------------------------------------------------------------------------------
    def build_samples(self):
        """Points along the middle of the road: dicts with x, z, tangent, left normal, s, curve (+1 left / -1 right / 0), loop."""
        n = len(self.nodes)
        corners = []
        for i, (x, z, r, flags) in enumerate(self.nodes):
            px, pz = self.nodes[i - 1][:2]
            nx_, nz_ = self.nodes[(i + 1) % n][:2]
            d1 = np.array([x - px, z - pz], float)
            d2 = np.array([nx_ - x, nz_ - z], float)
            d1 /= np.linalg.norm(d1)
            d2 /= np.linalg.norm(d2)
            cross = d1[0] * d2[1] - d1[1] * d2[0]
            angle = math.acos(max(-1.0, min(1.0, float(d1 @ d2))))
            t = r * math.tan(angle / 2) if r > 0 and angle > 1e-6 else 0.0
            corners.append((np.array([x, z], float), d1, d2, t, r, cross, angle))
        out = []
        s = 0.0

        def add(p, tang, curve, loop):
            nonlocal s
            left = np.array([tang[1], -tang[0]])          # (x east, z south): left of the driving direction
            out.append({"x": p[0], "z": p[1], "t": tang, "n": left, "s": s, "curve": curve, "loop": loop})
            s += STEP

        for i in range(n):
            c, d1, d2, t, r, cross, angle = corners[i]
            nxt = corners[(i + 1) % n]
            loop = "loop" in self.nodes[i][3]
            # the arc of corner i
            if t > 0:
                a0 = c - d1 * t
                sign = 1 if cross > 0 else -1               # cross > 0: turning towards +z of the direction = right in screen terms
                normal_to_centre = np.array([-d1[1], d1[0]]) * sign
                centre = a0 + normal_to_centre * r
                length = r * angle
                k = int(length / STEP)
                for j in range(k):
                    phi = (j * STEP) / r * sign
                    v = a0 - centre
                    rot = np.array([v[0] * math.cos(phi) - v[1] * math.sin(phi), v[0] * math.sin(phi) + v[1] * math.cos(phi)])
                    p = centre + rot
                    tang = np.array([-rot[1], rot[0]]) * sign
                    tang /= np.linalg.norm(tang)
                    add(p, tang, -sign, False)
            # the straight from corner i to corner i+1
            b0 = c + d2 * t
            b1 = nxt[0] - nxt[1] * nxt[3]
            seg = b1 - b0
            length = float(np.linalg.norm(seg))
            if length > 1e-6:
                tang = seg / length
                k = int(length / STEP)
                for j in range(k):
                    add(b0 + tang * (j * STEP), tang, 0, loop)
        self.samples = out
        self.length = s
        return out

    def nearest(self, x, z):
        best, bi = 1e18, 0
        for i, p in enumerate(self.samples):
            d = (p["x"] - x) ** 2 + (p["z"] - z) ** 2
            if d < best:
                best, bi = d, i
        return bi

    def s_at(self, x, z):
        return self.samples[self.nearest(x, z)]["s"]

    def h2_at_s(self, s):
        if not hasattr(self, "_keys"):
            self._keys = sorted((self.s_at(x, z), h) for x, z, h in self.heights) if self.heights else [(0, 0)]
        keys = self._keys
        if len(keys) == 1:
            return keys[0][1]
        L = self.length
        # periodic linear interpolation between keyframes
        for (s0, h0), (s1, h1) in zip(keys, keys[1:] + [(keys[0][0] + L, keys[0][1])]):
            if s0 <= s <= s1 or s0 <= s + L <= s1:
                ss = s if s0 <= s <= s1 else s + L
                f = 0 if s1 == s0 else (ss - s0) / (s1 - s0)
                return int(round(h0 + (h1 - h0) * f))
        return keys[0][1]

    def in_gap(self, s):
        if not hasattr(self, "_gap_s"):
            self._gap_s = [(self.s_at(x, z), length) for x, z, length in self.gaps]
        for s0, length in self._gap_s:
            if abs(s - s0) <= length / 2:
                return True
        return False

    # --- which columns belong to the road ----------------------------------------------------------------------------
    def classify(self):
        """column (x, z) -> dict(off, s, h2, i, curve) for every column within the barrier of the road (not in the looping / a gap)."""
        x0, z0, x1, z1 = self.box
        P = np.array([(p["x"], p["z"]) for p in self.samples])
        T = np.array([p["t"] for p in self.samples])
        N = np.array([p["n"] for p in self.samples])
        H = np.array([self.h2_at_s(p["s"]) for p in self.samples])
        self.sample_h2 = H
        cols = {}
        xs = np.arange(x0, x1 + 1) + 0.5
        for z in range(z0, z1 + 1):
            cz = z + 0.5
            dx = xs[:, None] - P[None, :, 0]
            dz = cz - P[None, :, 1]
            d2 = dx * dx + dz * dz
            idx = d2.argmin(1)
            for k, x in enumerate(range(x0, x1 + 1)):
                i = idx[k]
                p = self.samples[i]
                along = dx[k, i] * T[i, 0] + dz[0, i] * T[i, 1]
                off = dx[k, i] * N[i, 0] + dz[0, i] * N[i, 1]
                if abs(off) > WALL + 0.6 or abs(along) > 0.6 or p["loop"] or self.in_gap_cached(i):
                    continue
                cols[(x, z)] = {"off": off, "s": p["s"], "h2": int(H[i]), "i": i, "curve": p["curve"]}
        self.cols = cols
        return cols

    def in_gap_cached(self, i):
        if not hasattr(self, "_gapmask"):
            self._gapmask = [self.in_gap(p["s"]) for p in self.samples]
        return self._gapmask[i]

    def road(self):
        return {c: v for c, v in self.cols.items() if abs(v["off"]) <= KERB}

    def walk(self, h2):
        return G + (h2 + 1) // 2

    def point(self, x, z):
        """The sample nearest (x, z): position, tangent, normal, walking height."""
        p = self.samples[self.nearest(x, z)]
        return p, self.walk(int(self.sample_h2[self.nearest(x, z)]))


def level_blocks(h2):
    """(top full block y, slab y or None) for a surface h2 half blocks above the ground; you walk at G + (h2 + 1) // 2."""
    if h2 % 2 == 0:
        return G + h2 // 2 - 1, None
    return G + h2 // 2 - 1, G + h2 // 2


def cardinal(t):
    """The facing ("east", ...) nearest a tangent (x east, z south)."""
    if abs(t[0]) >= abs(t[1]):
        return "east" if t[0] > 0 else "west"
    return "south" if t[1] > 0 else "north"


FACING_VEC = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}


# ======================================================================================================================
# the three tracks
# ======================================================================================================================
def regenboog():
    b = Baan("regenboog", 1, [(22, 62, 14, ()), (170, 62, 14, ()), (170, 20, 14, ()), (120, 20, 10, ()), (108, 30, 10, ()),
                              (84, 30, 10, ()), (72, 20, 10, ()), (22, 20, 14, ())], (4, 4, 187, 76), laps=3)
    b.floating = True
    b.heights = [(40, 62, 0), (126, 62, 0), (170, 36, 28), (160, 20, 28), (141, 20, 28), (137, 20, 26), (124, 20, 26),
                 (104, 30, 22), (88, 30, 22), (76, 22, 26), (58, 20, 26), (54, 20, 24), (36, 20, 24), (22, 32, 24), (36, 62, 0)]
    b.gaps = [(139, 20, 3), (56, 20, 3)]
    b.start = (59, 62)
    b.rings = [(0, 56, 62), (1, 170, 40), (2, 128, 20), (3, 64, 20), (4, 22, 40)]
    b.pads = [(84, 62, True), (145, 20, True), (62, 20, True), (98, 30, False), (22, 50, False)]
    b.boosts = [(170, 47), (112, 62), (94, 30), (22, 46)]
    b.mikas = [(152, 20, +1, 0), (132, 20, -1, 1), (114, 25, +1, 2), (100, 30, +1, 0), (46, 20, +1, 1), (22, 28, -1, 2)]
    return b


def vads():
    b = Baan("vads", 2, [(14, 124, 10, ()), (82, 124, 10, ()), (82, 140, 0, ("loop",)), (74, 152, 0, ()), (74, 182, 8, ()),
                         (60, 182, 6, ()), (60, 150, 8, ()), (40, 150, 8, ()), (40, 182, 6, ()), (14, 182, 10, ())],
             (4, 112, 95, 189), laps=3)
    b.heights = [(40, 124, 0)]
    b.start = (43, 124)
    b.rings = [(0, 40, 124), (1, 82, 136), (2, 74, 166), (3, 60, 166), (4, 40, 166), (5, 14, 146)]
    b.pads = [(28, 124, True), (74, 160, False), (40, 162, True), (14, 171, False)]
    b.saus = [(50, 143, 70, 160), (30, 143, 50, 160), (4, 174, 24, 189)]
    b.paddenstoelen = [(8, 152, 20, 168)]
    b.mikas = [(80, 162, -1, 0), (50, 164, +1, 1), (50, 138, -1, 2), (22, 152, -1, 0), (66, 132, +1, 1), (8, 132, +1, 2)]
    return b


def kaasberg():
    b = Baan("kaasberg", 3, [(106, 124, 10, ()), (178, 124, 12, ()), (178, 184, 10, ()), (150, 184, 8, ()), (150, 142, 8, ()),
                             (122, 142, 8, ()), (122, 184, 8, ()), (106, 184, 8, ())], (96, 112, 187, 189), laps=3)
    b.heights = [(130, 124, 0), (150, 176, 0), (150, 150, 24), (136, 142, 24), (122, 150, 24), (122, 176, 0)]
    b.start = (133, 124)
    b.rings = [(0, 130, 124), (1, 178, 150), (2, 150, 164), (3, 136, 142), (4, 122, 164), (5, 106, 150)]
    b.pads = [(150, 124, True), (178, 170, False), (106, 170, True), (164, 184, False)]
    b.ijs = [(126, 136, 146, 148)]
    b.rollen = [(147.5, 153, 0), (150.5, 153, 1), (153.5, 153, 2)]
    b.mikas = [(186, 146, -1, 0), (136, 136, +1, 1), (128, 146, -1, 2), (116, 166, -1, 0), (112, 130, +1, 1), (168, 190, -1, 2)]
    return b


def all_banen():
    out = [regenboog(), vads(), kaasberg()]
    for b in out:
        b.build_samples()
        b.classify()
    return out


# ======================================================================================================================
# building a track into the template
# ======================================================================================================================
def surface(b, x, z, c):
    """(full block, props, slab block, props) of the road surface at a column."""
    off, s = c["off"], c["s"]
    if abs(off) > HALF:                                     # kerbs: two by two, a colour and white
        stripe = int(s) // 2 % 2 == 0
        if b.name == "regenboog":
            return ("minecraft:smooth_quartz", {}, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"}) if stripe else \
                ("minecraft:purpur_block", {}, "minecraft:purpur_slab", {"type": "bottom", "waterlogged": "false"})
        if b.name == "vads":
            return ("minecraft:smooth_sandstone", {}, "minecraft:smooth_sandstone_slab", {"type": "bottom", "waterlogged": "false"}) if stripe else \
                ("minecraft:cut_red_sandstone", {}, "minecraft:cut_red_sandstone_slab", {"type": "bottom", "waterlogged": "false"})
        return ("minecraft:smooth_quartz", {}, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"}) if stripe else \
            ("minecraft:cut_red_sandstone", {}, "minecraft:cut_red_sandstone_slab", {"type": "bottom", "waterlogged": "false"})
    if b.name == "regenboog":
        k = max(0, min(6, int(math.floor(off + HALF))))
        k = 6 - k                                           # red on the left of the driving direction
        return ("guhs:circuit_regenboogweg", {"kleur": str(k)}, "guhs:circuit_regenboogweg_plaat",
                {"kleur": str(k), "type": "bottom", "waterlogged": "false"})
    for x0, z0, x1, z1 in b.saus:
        if x0 <= x <= x1 and z0 <= z <= z1 and c["h2"] % 2 == 0:
            return ("guhs:circuit_kaassaus", {}, None, None)
    for x0, z0, x1, z1 in b.ijs:
        if x0 <= x <= x1 and z0 <= z <= z1 and c["h2"] % 2 == 0:
            return ("guhs:circuit_bergijs", {}, None, None)
    for x0, z0, x1, z1 in b.paddenstoelen:
        if x0 <= x <= x1 and z0 <= z <= z1 and c["h2"] % 2 == 0 and (x * 3 + z * 5) % 7 == 0:
            return ("guhs:circuit_stuiterpaddenstoel", {}, None, None)
    return ("guhs:circuit_kaasweg", {}, "guhs:circuit_kaasweg_plaat", {"type": "bottom", "waterlogged": "false"})


def wall_needed(b, c):
    off = c["off"]
    if not KERB < abs(off) <= WALL:
        return False
    if c["h2"] >= 4:
        return not b.floating or c["curve"] != 0 and off * c["curve"] < 0       # floating: only on the outside of the curves
    return c["curve"] != 0 and off * c["curve"] < 0                              # on the ground: the outside of the curves


def build(s, b, mc):
    """Builds the track's road, kerbs, barriers, embankments / decks into s."""
    road = b.road()
    deck_rows = {}
    for (x, z), c in b.cols.items():
        off, h2 = c["off"], c["h2"]
        top, slab = level_blocks(h2)
        walk = b.walk(h2)
        if abs(off) <= KERB:
            full, fp, half, hp = surface(b, x, z, c)
            if slab is not None and half is None:
                full, fp, half, hp = "guhs:circuit_kaasweg", {}, "guhs:circuit_kaasweg_plaat", {"type": "bottom", "waterlogged": "false"}
            floating = b.floating and h2 >= 6
            bottom = top - 1 if floating else 0
            for y in range(bottom, top + 1):
                if y == top:
                    s.set(x, y, z, full, fp)
                elif floating:
                    s.set(x, y, z, "guhs:pluiswolblok")
                elif y >= G - 1:
                    s.set(x, y, z, under_block(b, x, y, z))
                else:
                    s.set(x, y, z, mc("dirt"))
            if slab is not None:
                s.set(x, slab, z, half, hp)
            if floating:
                deck_rows[(x, z)] = top - 1
            for y in range(walk, walk + 6):
                s.set(x, y, z, mc("air"))
        elif wall_needed(b, c):
            base = walk
            floating = b.floating and h2 >= 6
            top, slab = level_blocks(h2)
            for y in range((top if floating else 0), base):
                s.set(x, y, z, mc("white_concrete") if y >= G - 1 else mc("dirt"))
            if b.name == "regenboog":
                s.set(x, base, z, mc("white_stained_glass"))
                s.set(x, base + 1, z, mc("pink_stained_glass") if int(c["s"]) % 4 < 2 else mc("light_blue_stained_glass"))
            elif b.name == "vads":
                s.set(x, base, z, "guhs:belegen_kaas_stenen")
                s.set(x, base + 1, z, mc("orange_stained_glass"))
            else:
                s.set(x, base, z, "guhs:belegen_kaas_stenen")
                s.set(x, base + 1, z, mc("yellow_stained_glass"))
            for y in range(base + 2, base + 6):
                if s.get(x, y, z) not in (None, mc("air")):
                    s.blocks.pop((x, y, z), None)
    return deck_rows


def under_block(b, x, y, z):
    """What an embankment is made of (the road's body under the surface)."""
    if b.name == "regenboog":
        return "minecraft:white_concrete" if (x + y + z) % 5 else "minecraft:pink_concrete"
    if b.name == "vads":
        return "minecraft:yellow_terracotta" if (x + z) % 3 else "guhs:belegen_kaas_stenen"
    return "guhs:belegen_kaas_stenen" if (x * 7 + y * 3 + z) % 4 else "minecraft:yellow_terracotta"


def pillars(s, b, deck_rows, mc):
    """The Regenboogbaan's cloud pillars: under the middle of the floating road every 18 blocks, from the ground to the deck."""
    done = []
    for (x, z), under in sorted(deck_rows.items(), key=lambda kv: b.cols[kv[0]]["s"]):
        c = b.cols[(x, z)]
        if abs(c["off"]) > 1.2 or int(c["s"]) % 18 != 0:
            continue
        if any(abs(x - px) + abs(z - pz) < 8 for px, pz in done):
            continue
        done.append((x, z))
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for y in range(G - 1, under):
                    kind = mc("white_wool") if (y // 3) % 2 == 0 else mc("pink_wool")
                    if abs(dx) + abs(dz) == 2:
                        if y % 4 == 0:
                            s.set(x + dx, y, z + dz, "guhs:pluiswolblok")
                        continue
                    s.set(x + dx, y, z + dz, kind)
        for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):   # a puffy cloud where the pillar meets the deck
            s.set(x + dx, under - 1, z + dz, "guhs:pluiswolblok")
            s.set(x + dx, under, z + dz, "guhs:pluiswolblok")
    return done


def ring(s, b, n, x, z, mc):
    """A checkpoint ring: two glowing posts beside the road and a beam over it, with guh ears on top (race_checkpoint)."""
    p, walk = b.point(x, z)
    t = p["t"]
    axis = "x" if abs(t[0]) > abs(t[1]) else "z"
    props = {"nummer": str(n), "baan": str(b.index)}
    cx, cz = int(math.floor(p["x"])), int(math.floor(p["z"]))
    top = walk + 6
    lo, hi = -5, 5
    for a in (lo, hi):
        px, pz = (cx, cz + a) if axis == "x" else (cx + a, cz)
        for y in range(walk, top):
            s.set(px, y, pz, "guhs:race_checkpoint", props)
        y = walk - 1
        while y >= 0 and s.get(px, y, pz) in (None, mc("air")):
            s.set(px, y, pz, mc("white_concrete") if b.name != "regenboog" else "guhs:pluiswolblok")
            y -= 1
            if b.floating and y < walk - 3:
                break
    for a in range(lo, hi + 1):
        px, pz = (cx, cz + a) if axis == "x" else (cx + a, cz)
        s.set(px, top, pz, "guhs:race_checkpoint", props)
    ear = mc("pink_wool") if b.name != "vads" else mc("yellow_wool")
    for e in (-3, 3):
        for dy, width in ((1, 1), (2, 0)):
            for d in range(-width, width + 1):
                px, pz = (cx, cz + e + d) if axis == "x" else (cx + e + d, cz)
                s.set(px, top + dy, pz, ear)
    return (cx, cz, axis, walk)


def boost_ring(s, b, x, z, mc):
    """A rainbow arch over the road whose inside is the shimmering boost skin (circuit_boostring): run through = VAHOEG."""
    p, walk = b.point(x, z)
    t = p["t"]
    axis = "x" if abs(t[0]) > abs(t[1]) else "z"
    cx, cz = int(math.floor(p["x"])), int(math.floor(p["z"]))
    colours = ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple"]
    r_out, r_in = 6.8, 5.2
    for u in range(-7, 8):
        for v in range(0, 10):
            d = math.hypot(u, v * 0.8)
            px, pz = (cx, cz + u) if axis == "x" else (cx + u, cz)
            y = walk - 1 + v
            if r_in < d <= r_out:
                k = min(6, int((d - r_in) / (r_out - r_in) * 7))
                s.set(px, y, pz, mc(colours[k] + "_concrete"))
            elif d <= r_in and v >= 1 and abs(u) <= 5:
                if s.get(px, y, pz) in (None, mc("air")):
                    s.set(px, y, pz, "guhs:circuit_boostring", {"axis": axis})
    # feet of the arch down to the ground (floating: down to the deck's underside)
    for u in (-6, 6):
        px, pz = (cx, cz + u) if axis == "x" else (cx + u, cz)
        y = walk - 1
        while y >= 0 and s.get(px, y, pz) in (None, mc("air")):
            s.set(px, y, pz, mc("white_concrete"))
            y -= 1
            if b.floating and y < walk - 3:
                break
    return (cx, cz, axis, walk)


def pad_row(s, b, x, z, lastig):
    """Two rows of VAHOEG pads across the road (silver ones don't work on lastig)."""
    p, walk = b.point(x, z)
    facing = cardinal(p["t"])
    fx, fz = FACING_VEC[facing]
    cx, cz = int(math.floor(p["x"])), int(math.floor(p["z"]))
    out = []
    for step in (0, 1):
        for w in range(-3, 4):
            if facing in ("east", "west"):
                px, pz = cx + fx * step, cz + w
            else:
                px, pz = cx + w, cz + fz * step
            c = b.cols.get((px, pz))
            if c is None or abs(c["off"]) > HALF:
                continue
            s.set(px, b.walk(c["h2"]), pz, "guhs:race_vahoegpad", {"facing": facing, "lastig": "true" if lastig else "false"})
            out.append((px, b.walk(c["h2"]), pz))
    return out


def start_marker(s, b):
    p, walk = b.point(*b.start)
    facing = cardinal(p["t"])
    x, z = b.start
    s.set(x, walk, z, "guhs:race_start", {"facing": facing, "baan": str(b.index)})
    return (x, walk, z, facing)


def mika_spot(s, b, x, z, side, vanaf, mc, platform):
    """A Mika-pikker spot beside the road (a little platform where it's high up)."""
    p, walk = b.point(x, z)
    n = p["n"]
    px = int(math.floor(p["x"] + n[0] * side * 6.8))
    pz = int(math.floor(p["z"] + n[1] * side * 6.8))
    to_road = cardinal((-n[0] * side, -n[1] * side))
    y = walk
    if platform or s.get(px, y - 1, pz) in (None, mc("air")):
        block = platform or "guhs:belegen_kaas_stenen"
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if s.get(px + dx, y - 1, pz + dz) in (None, mc("air")):
                    s.set(px + dx, y - 1, pz + dz, block)
        # a little bridge of the same stuff to the road's edge (so it hangs on to something)
        for k in range(2, 8):
            qx = int(math.floor(p["x"] + n[0] * side * (6.8 - k * 0.5)))
            qz = int(math.floor(p["z"] + n[1] * side * (6.8 - k * 0.5)))
            if s.get(qx, y - 1, qz) in (None, mc("air")):
                s.set(qx, y - 1, qz, block)
    for yy in range(y, y + 3):
        s.set(px, yy, pz, mc("air"))
    s.set(px, y, pz, "guhs:circuit_mikaplek", {"facing": to_road, "vanaf": str(vanaf)})
    return (px, y, pz)


def rol_spot(s, b, x, z, vanaf):
    p, walk = b.point(x, z)
    down = cardinal((-p["t"][0], -p["t"][1]))
    px, pz = int(math.floor(x)), int(math.floor(z))
    c = b.cols.get((px, pz))
    y = b.walk(c["h2"]) if c else walk
    s.set(px, y, pz, "guhs:circuit_rolplek", {"facing": down, "vanaf": str(vanaf)})
    return (px, y, pz, down)


# ======================================================================================================================
# the Vadslooping (same formula as RaceRit.java)
# ======================================================================================================================
def loop_path(E, facing, t, radius=LOOP_R, length=LOOP_L, side=LOOP_W, drop=LOOP_DROP):
    fx, fz = FACING_VEC[facing]
    rx, rz = FACING_VEC[CLOCKWISE[facing]]
    a = 2 * math.pi * t
    rho = radius - drop * (1 - math.cos(a)) / 2
    f = length * t + rho * math.sin(a)
    up = radius - rho * math.cos(a)
    sd = side * t
    return (E[0] + fx * f + rx * sd, E[1] + up, E[2] + fz * f + rz * sd)


def looping(s, b, mc):
    """The Vadslooping: a corkscrew ring of cheese road (radius R + 1 around the loop's middle), orange rails, guh ears on
    top and a big guh face on its outer side; the marker at the entrance starts the ride (circuit_looping)."""
    i = [k for k, n in enumerate(b.nodes) if "loop" in n[3]][0]
    ex, ez = b.nodes[i][:2]
    p = b.samples[b.nearest(ex, ez - 3)]
    facing = cardinal(p["t"])
    walk = G
    E = (ex, walk, ez)
    fx, fz = FACING_VEC[facing]
    rx, rz = FACING_VEC[CLOCKWISE[facing]]
    ring_blocks = set()
    rail = set()
    steps = 900
    for k in range(steps + 1):
        t = k / steps
        a = 2 * math.pi * t
        # the loop's middle and the outward direction at t
        cxm = E[0] + fx * LOOP_L * t + rx * LOOP_W * t
        cym = E[1] + LOOP_R
        czm = E[2] + fz * LOOP_L * t + rz * LOOP_W * t
        ox, oy, oz = fx * math.sin(a), -math.cos(a), fz * math.sin(a)
        for w in np.linspace(-2.6, 2.6, 11):
            for rr in (LOOP_R + 0.55, LOOP_R + 1.0, LOOP_R + 1.45):
                q = (int(math.floor(cxm + ox * rr + rx * w)), int(math.floor(cym + oy * rr)), int(math.floor(czm + oz * rr + rz * w)))
                ring_blocks.add(q)
            if abs(w) > 2.4:
                q = (int(math.floor(cxm + ox * (LOOP_R + 0.2) + rx * w * 1.15)), int(math.floor(cym + oy * (LOOP_R + 0.2))),
                     int(math.floor(czm + oz * (LOOP_R + 0.2) + rz * w * 1.15)))
                rail.add(q)
    for q in ring_blocks:
        if q[1] < G - 1:
            continue
        k = (q[0] * 3 + q[1] + q[2] * 5) // 2
        s.set(*q, "guhs:circuit_kaasweg" if q[1] <= G - 1 else ("minecraft:yellow_concrete" if k % 3 else "minecraft:orange_concrete"))
    for q in rail - ring_blocks:
        if q[1] >= G:
            s.set(*q, mc("orange_stained_glass"))
    # the path itself stays free (the guh and its rider: 3.2 blocks above the feet, 0.8 around)
    path_air = set()
    for k in range(steps + 1):
        px, py, pz = loop_path(E, facing, k / steps)
        for dy in range(0, 4):
            for d in (-1, 0, 1):
                q = (int(math.floor(px + rx * d * 0.8)), int(math.floor(py + dy)), int(math.floor(pz + rz * d * 0.8)))
                if q[1] >= G:
                    path_air.add(q)
    for q in path_air:
        if q in ring_blocks and q[1] > G + 1:
            s.blocks.pop(q, None)
    # guh ears on top of the loop (on both edges of the ring), and the entrance marker
    ytop = int(math.floor(E[1] + 2 * LOOP_R + 1.45))
    for side in (LOOP_W * 0.5 - 2.2, LOOP_W * 0.5 + 2.2):
        cxm = E[0] + fx * LOOP_L * 0.5 + rx * side
        czm = E[2] + fz * LOOP_L * 0.5 + rz * side
        for dy, width in ((1, 2), (2, 2), (3, 1), (4, 0)):
            for d in range(-width, width + 1):
                q = (int(math.floor(cxm + fx * d)), ytop + dy, int(math.floor(czm + fz * d)))
                s.set(*q, mc("magenta_wool") if dy >= 2 and abs(d) < width else mc("pink_wool"))
    s.set(int(math.floor(E[0])), walk, int(math.floor(E[2])), "guhs:circuit_looping", {"facing": facing, "vanaf": "0"})
    return E, facing, ring_blocks


# ======================================================================================================================
# the self-check of a track
# ======================================================================================================================
PASSABLE = ("minecraft:air", "guhs:race_vahoegpad", "guhs:race_start", "guhs:circuit_boostring", "guhs:circuit_mikaplek",
            "guhs:circuit_rolplek", "guhs:circuit_looping")


def passable(s, x, y, z):
    b = s.get(x, y, z)
    return b is None or b in PASSABLE


def solid(s, x, y, z):
    b = s.get(x, y, z)
    return b is not None and b not in PASSABLE and "kaas_saus" not in b


def check(s, b):
    problems = []
    road = b.road()
    allowed = set(TRACK_TAG + RACE_TAG)
    # 1. the road: no holes, allowed surface, room above it, gentle steps (half a block between neighbours; gaps excepted)
    for (x, z), c in road.items():
        h2 = c["h2"]
        top, slab = level_blocks(h2)
        if not solid(s, x, top, z) or (slab is not None and not solid(s, x, slab, z)):
            problems.append(f"{b.name}: hole in the road at {x},{z}")
            continue
        surf = s.get(x, top if slab is None else slab, z)
        if surf not in allowed:
            problems.append(f"{b.name}: the race guh may not run on {surf} at {x},{z}")
        walk = b.walk(h2)
        for y in range(walk, walk + 4):
            if not passable(s, x, y, z):
                problems.append(f"{b.name}: no room above the road at {x},{y},{z}: {s.get(x, y, z)}")
                break
        for dx, dz in ((1, 0), (0, 1)):
            n = road.get((x + dx, z + dz))
            if n and abs(n["h2"] - h2) > 1:
                problems.append(f"{b.name}: step of {abs(n['h2'] - h2) / 2} blocks at {x},{z}")
    # 2. rings: numbered 0..n-1, spanning the road, crossed in order driving along the middle
    rings = {}
    for (x, y, z), (blk, props, _) in s.blocks.items():
        if blk == "guhs:race_checkpoint" and props.get("baan") == str(b.index):
            rings.setdefault(int(props["nummer"]), []).append((x, y, z))
    if sorted(rings) != list(range(len(b.rings))):
        problems.append(f"{b.name}: rings {sorted(rings)}")
    boxes = {}
    for n, blocks in rings.items():
        lo = [min(q[i] for q in blocks) for i in range(3)]
        hi = [max(q[i] for q in blocks) + 1 for i in range(3)]
        boxes[n] = (lo[0] - 0.5, lo[1] - 1, lo[2] - 0.5, hi[0] + 0.5, hi[1] + 1, hi[2] + 0.5)
        if max(hi[0] - lo[0], hi[2] - lo[2]) < 10:
            problems.append(f"{b.name}: ring {n} is too narrow")

    def inside(box, q):
        return box[0] <= q[0] < box[3] and box[1] <= q[1] < box[4] and box[2] <= q[2] < box[5]
    sx, sz = b.start
    first = b.nearest(sx + 0.5, sz + 0.5)
    order, current = [], None
    n = len(b.samples)
    for k in range(n + 1):
        p = b.samples[(first + k) % n]
        y = b.walk(int(b.sample_h2[(first + k) % n]))
        now = [m for m, box in boxes.items() if inside(box, (p["x"], y + 0.1, p["z"]))]
        if now and now[0] != current:
            order.append(now[0])
        current = now[0] if now else None
    want = list(range(1, len(b.rings))) + [0]
    if order != want:
        problems.append(f"{b.name}: driving a lap goes through the rings {order} (want {want})")
    # 3. the start marker is on the road, past ring 0
    walk = b.walk(road[(sx, sz)]["h2"]) if (sx, sz) in road else None
    if walk is None or s.get(sx, walk, sz) != "guhs:race_start" or not solid(s, sx, walk - 1, sz):
        problems.append(f"{b.name}: the start marker isn't on the road")
    elif 0 in boxes and inside(boxes[0], (sx + 0.5, walk, sz + 0.5)):
        problems.append(f"{b.name}: the start marker is inside ring 0")
    return problems


# ======================================================================================================================
# everything of the three tracks, in the right order
# ======================================================================================================================
def build_tracks(s, banen, mc):
    """Road first (it clears the room above itself), then the looping, rings, boost rings, pads, markers. Returns what the
    rest of the template and the self-check need to know."""
    info = {}
    for b in banen:
        deck = build(s, b, mc)
        info[b.name] = {"deck": deck}
    for b in banen:
        d = info[b.name]
        if b.floating:
            d["pillars"] = pillars(s, b, d["deck"], mc)
        if any("loop" in n[3] for n in b.nodes):
            d["looping"] = looping(s, b, mc)
        d["rings"] = [ring(s, b, n, x, z, mc) for n, x, z in b.rings]
        d["boosts"] = [boost_ring(s, b, x, z, mc) for x, z in b.boosts]
        d["pads"] = [pad_row(s, b, x, z, lastig) for x, z, lastig in b.pads]
        d["start"] = start_marker(s, b)
        platform = "guhs:pluiswolblok" if b.floating else None
        d["mikas"] = [mika_spot(s, b, x, z, side, vanaf, mc, platform) for x, z, side, vanaf in b.mikas]
    return info


def lap_lengths(banen):
    return {b.name: b.length for b in banen}
