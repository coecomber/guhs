"""
einde2, slice geestenwereld: the build of the Geestenwereld (DESIGN_EINDE2 5 "De Geestenwereld" + "Uitwerking"): floating
islands in lila mist, 608 x 160 x 608, template 0,0,0 at world (-304, 32, -304). Seven zones, joined by floating paths of
maansteen with lotus leaves on them:

  1. de Meditatiecirkel (the middle, where you arrive): a still pond with stone lotus leaves, the lantern tree on an islet,
     the arrival lawn to the south;
  2. het Gefluisterwoud (west): black-and-white trees with glowing leaves, darker trees and ground at its rim (shadow
     spirits), a clearing with a burnt stump for Hei Bai-guh;
  3. Koh's Grot (north-west): an ancient tree on hollow roots; the room under it has hundreds of masks on its walls
     (and a Mika, a Guhdalf beard, a Sponsguh grin and a Blue Spirit among them);
  4. de Witte Lotus-ruines (north): a courtyard with the 3 x 3 lotus puzzle before the gate, the Hal van de Voorgangers with
     eight memorial stones and the fresco "Raava en Vaatu? Nooit van gehoord.", and the Hemeltrap up to
  5. de Top der Voorgangers: a small island high above, where Roku-guh and Kyoshi-guh stand;
  6. de Hemelvijver (east): a pond that hangs in the sky, mist falling UP out of it, Tui and La circling above it;
  7. het Theepaviljoen (south-east): a pagoda with a tea table and the Theegeest's seat;
  8. Fang's Hemelpad (north-east): the perch where Fang waits, and his flight to the Top (a pad marker).

Markers (data/guhs/einde2/plekken/geestenwereld.json, read by Plekken / the Java slice): per zone zone_<z>, plek_<z> (where
/guhs einde2 geest plek <z> puts you), meditatiesteen_<z> (the stone that sends you back), lore_<id> (every memorial
stone); the residents (npc markers with an entity or a kind); the 8 lost lanterns verdwaalde_lantaarn_1..8 and the 8
empty hooks lantaarnboom_haak_1..8; the 9 tiles tegel_1..9 of the puzzle; the routes (hemeltrap, fang_vlucht, koi_cirkel).
"""
import math

import features.einde2_kit as kit
from features import einde2_geestenwereld_tekst as tekst

NAAM = "geestenwereld"
W, H, D = 608, 160, 608
OORSPRONG = (-304, 32, -304)          # the world position of template 0,0,0
PREFIX = "sign.guhs.einde2_geestenwereld"

ZONES = ["meditatiecirkel", "gefluisterwoud", "koh_grot", "lotus_ruines", "top_voorgangers", "hemelvijver", "theepaviljoen", "fang_pad"]

# the zones: centre x, z and the floor (the y you stand at), radius of the island
M = dict(x=304, z=304, f=50, r=31)
WOUD = dict(x=150, z=336, f=48, r=58)
KOH = dict(x=168, z=150, f=40, r=44)
LOTUS = dict(x=338, z=140, f=56, r=48)
TOP = dict(x=356, z=52, f=112, r=15)
VIJVER = dict(x=476, z=300, f=66, r=34)
THEE = dict(x=436, z=470, f=54, r=27)
FANG = dict(x=496, z=150, f=84, r=13)

GRAS, AARDE, STEEN = "guhs:geestengras", "guhs:geestenaarde", "guhs:maansteen"
TEGEL, PLAAT, TRAP = "guhs:maansteen_tegels", "guhs:maansteen_plaat", "guhs:maansteen_trap"
HOUT, SHOUT, PLANK, SPLANK = "guhs:geestenhout", "guhs:schaduwhout", "guhs:geestenplanken", "guhs:schaduwplanken"
CYAAN, ROZE, SCHADUW = "guhs:gloeibladeren_cyaan", "guhs:gloeibladeren_roze", "guhs:schaduwbladeren"
NEVEL, LOTUSBLAD, LANTAARN = "guhs:nevelwater", "guhs:lotusblad", "guhs:geestlantaarn"
STEEL, HOED = "guhs:geestpaddenstoel_steel", "guhs:geestpaddenstoel_hoed"
MEDITATIE, GEDENK, PUZZEL, MASKER = "guhs:meditatiesteen", "guhs:gedenksteen", "guhs:lotuspuzzeltegel", "guhs:geestenmasker"
MASKERS = ["noh", "guh", "mika", "guhdalf", "sponsguh", "vos", "aap", "lach", "verdriet", "blanco", "blauwe_geest", "uil"]
WATER = "minecraft:water"

# light of our blocks for the kit's light map (the dimension has sky light; only Koh's room is checked for it)
kit.LICHT.update({LANTAARN: 15, CYAAN: 13, ROZE: 13, HOED: 12, NEVEL: 8, MEDITATIE: 7, GEDENK: 4, MASKER: 3})

YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}
DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


class Geest(kit.Bouw):
    """The build, with a few helpers of its own."""

    def __init__(self, h):
        super().__init__(h, NAAM, (W, H, D), kit.seed(NAAM), prefix=PREFIX)
        self.dun |= {NEVEL, MASKER, "minecraft:chain", "minecraft:candle"}
        self.los_cellen = set()          # decoration that floats on purpose (islets, loose lotus leaves, the koi's mist)
        self.vrij = []                   # (x, z, r): keep trees and decoration out of these circles
        self.licht_kamer = []            # cells that must have block light (Koh's room)

    def leeg(self, x, y, z):
        return (x, y, z) not in self.s.blocks

    def zet(self, x, y, z, naam, props=None, nbt=None):
        """Only where nothing stands yet."""
        if self.inside(x, y, z) and self.leeg(x, y, z):
            self.set(x, y, z, naam, props, nbt)

    def is_vrij(self, x, z, marge=0):
        return all(math.hypot(x - vx, z - vz) > vr + marge for vx, vz, vr in self.vrij)


# =====================================================================================================================
# noise and shapes
# =====================================================================================================================
def _hash(*a):
    v = 0x9E3779B1
    for n in a:
        v = (v ^ (int(n) & 0xFFFFFFFF)) * 0x85EBCA6B & 0xFFFFFFFF
        v ^= v >> 13
        v = v * 0xC2B2AE35 & 0xFFFFFFFF
        v ^= v >> 16
    return v / 0xFFFFFFFF


def ruis(x, z, schaal, seed):
    """Smooth value noise in 0..1."""
    gx, gz = x / schaal, z / schaal
    x0, z0 = math.floor(gx), math.floor(gz)
    fx, fz = gx - x0, gz - z0
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a, b = _hash(x0, z0, seed), _hash(x0 + 1, z0, seed)
    c, d = _hash(x0, z0 + 1, seed), _hash(x0 + 1, z0 + 1, seed)
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz


def hoekruis(th, seed):
    s = 0.0
    for k in range(2, 6):
        s += math.sin(k * th + 6.283 * _hash(k, seed)) * (0.9 / k)
    return s / 1.1


def eiland(b, cx, cz, f, r, diepte, seed, top=GRAS, ruw=0.12, rand_donker=None):
    """A floating island: flat grass at y f - 1 (you stand at f), aarde under it, maansteen below that, the bottom a rough
    upside-down dome with drips. rand_donker: from this fraction of the radius on, the ground is aarde (dark rim)."""
    for x in range(int(cx - r * 1.25) - 1, int(cx + r * 1.25) + 2):
        for z in range(int(cz - r * 1.25) - 1, int(cz + r * 1.25) + 2):
            dx, dz = x - cx, z - cz
            d = math.hypot(dx, dz)
            rr = r * (1 + ruw * hoekruis(math.atan2(dz, dx), seed))
            if d > rr:
                continue
            t = d / rr
            dd = max(2, int(diepte * math.sqrt(max(0.0, 1 - t * t)) * (0.7 + 0.45 * ruis(x, z, 7, seed))))
            if _hash(x, z, seed, 7) < 0.03 and t < 0.85:
                dd += 2 + int(_hash(x, z, seed, 8) * 6)
            oppervlak = top
            if rand_donker is not None and t > rand_donker:
                oppervlak = AARDE
            b.zet(x, f - 1, z, oppervlak)
            for y in range(f - 2, f - 4, -1):
                b.zet(x, y, z, AARDE)
            for y in range(f - 4, f - 1 - dd, -1):
                b.zet(x, y, z, STEEN)


def boom(b, x, f, z, hoogte, kroon, blad, seed, stam=HOUT, banden=False, hangers=True):
    """A glowing tree: a trunk (black and white bands when banden), a round crown of glowing leaves, strands hanging down."""
    for y in range(f, f + hoogte):
        n = stam
        if banden:
            n = SHOUT if ((y - f) // 2) % 2 == 0 else HOUT
        b.set(x, y, z, n, {"axis": "y"})
    top = f + hoogte
    for k in range(4):                       # a few branches (face to face, so every piece holds on to the trunk)
        th = 6.283 * _hash(seed, k)
        lengte = 2 + int(kroon * 0.5)
        lijn6(b, (x, top - 2, z), (x + round(math.cos(th) * lengte), top - 2 + lengte // 2, z + round(math.sin(th) * lengte)),
              stam if not banden else SHOUT, {"axis": "x" if abs(math.cos(th)) > abs(math.sin(th)) else "z"})
    cy = top + kroon // 3
    for y in range(top, cy + 1):             # (the trunk goes on into the crown: it holds every leaf)
        b.set(x, y, z, SHOUT if banden else stam, {"axis": "y"})
    for dx in range(-kroon - 1, kroon + 2):
        for dy in range(-kroon // 2 - 1, kroon // 2 + 2):
            for dz in range(-kroon - 1, kroon + 2):
                dd = math.sqrt(dx * dx + (dy * 1.8) ** 2 + dz * dz)
                if dd <= kroon + 0.3 * (ruis(x + dx, z + dz, 3, seed + dy) - 0.5) * 2:
                    if _hash(x + dx, cy + dy, z + dz, seed) < 0.9:
                        n = blad if not isinstance(blad, (list, tuple)) else blad[int(_hash(dx, dy, dz, seed + 1) * len(blad))]
                        b.zet(x + dx, cy + dy, z + dz, n)
    if hangers:                              # strands of leaves under the crown
        for k in range(kroon * 2):
            hx = x + int((_hash(seed, k, 1) * 2 - 1) * (kroon - 1))
            hz = z + int((_hash(seed, k, 2) * 2 - 1) * (kroon - 1))
            onder = None
            for yy in range(cy + 2, cy - kroon, -1):
                if not b.leeg(hx, yy, hz):
                    onder = yy
                    break
            if onder is None:
                continue
            for i in range(1, 2 + int(_hash(seed, k, 3) * 4)):
                if onder - i <= f + 2:
                    break
                b.zet(hx, onder - i, hz, blad if not isinstance(blad, (list, tuple)) else blad[0])


def lijn6(b, a, c, naam, props=None):
    """A line of blocks from a to c in which every block touches the one before it with a face (never only an edge)."""
    x, y, z = a
    b.zet(x, y, z, naam, props)
    n = max(abs(c[0] - a[0]), abs(c[1] - a[1]), abs(c[2] - a[2]), 1)
    for i in range(1, n + 1):
        t = i / n
        doel = (round(a[0] + (c[0] - a[0]) * t), round(a[1] + (c[1] - a[1]) * t), round(a[2] + (c[2] - a[2]) * t))
        for as_ in range(3):
            while (x, y, z)[as_] != doel[as_]:
                stap = 1 if doel[as_] > (x, y, z)[as_] else -1
                if as_ == 0:
                    x += stap
                elif as_ == 1:
                    y += stap
                else:
                    z += stap
                b.zet(x, y, z, naam, props)


def paddenstoel(b, x, f, z, hoogte, r, seed):
    """A giant spirit mushroom: a white stem and a glowing dome."""
    dik = 1 if r < 5 else 2
    for y in range(f, f + hoogte):
        for dx in range(-(dik - 1), dik):
            for dz in range(-(dik - 1), dik):
                b.zet(x + dx, y, z + dz, STEEL, {"axis": "y"})
    top = f + hoogte
    for y in range(top, top + int(round(r * 0.6)) + 1):      # (the stem goes on into the cap)
        b.zet(x, y, z, STEEL, {"axis": "y"})
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d > r + 0.3:
                continue
            hy = int(round((r * 0.6) * math.sqrt(max(0.0, 1 - (d / (r + 0.3)) ** 2))))
            for y in range(top - (1 if d > r - 1.2 else 0), top + hy + 1):   # (a solid cap: no step in it hangs loose)
                b.zet(x + dx, y, z + dz, HOED)


def lantaarnpaal(b, x, s, z):
    """A post of birch fence with a spirit lantern on it, standing on the block at y s - 1."""
    b.zet(x, s, z, "minecraft:birch_fence")
    b.zet(x, s + 1, z, "minecraft:birch_fence")
    b.zet(x, s + 2, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})


def gedenksteen(b, id, x, f, z, facing, naam_regels):
    """A memorial stone (right-click: the lore of id) with a standing sign on top that names it."""
    b.set(x, f, z, GEDENK, {"facing": facing})
    b.sign(x, f + 1, z, facing, naam_regels, wall=False)
    b.prop(f"lore_{id}", x, f, z, wie=f"gedenksteen: {tekst.LORE[id][0]}")


def meditatiesteen(b, zone, x, f, z):
    b.set(x, f, z, MEDITATIE)
    b.prop(f"meditatiesteen_{zone}", x, f, z, wie="meditatiesteen: rechtsklik = terug naar je lijf")


def pad(b, punten, ya, yb, half=1, lantaarn_elke=16, lotus_elke=34, seed=0, wie=""):
    """A floating path of maansteen tiles along the points (x, z), from height ya to yb (the y you stand at, by halves:
    a half step is a slab). Lotus leaves widen it now and then, lanterns on posts stand beside it. Never overwrites."""
    seg = list(zip(punten, punten[1:]))
    lengtes = [math.hypot(bx - ax, bz - az) for (ax, az), (bx, bz) in seg]
    totaal = sum(lengtes)
    cellen = {}
    midden = []
    afstand = 0.0
    for ((ax, az), (bx, bz)), lengte in zip(seg, lengtes):
        n = max(1, int(lengte * 4))
        nx, nz = -(bz - az) / lengte, (bx - ax) / lengte
        for i in range(n + 1):
            t = i / n
            px, pz = ax + (bx - ax) * t, az + (bz - az) * t
            dd = afstand + lengte * t
            y = ya + (yb - ya) * dd / totaal
            h2 = int(round(y * 2))
            midden.append((px, pz, h2, nx, nz, dd))
            o = -half
            while o <= half + 1e-9:
                c = (int(round(px + nx * o)), int(round(pz + nz * o)))
                hoogtes = cellen.setdefault(c, [])
                if all(abs(h2 - v) > 12 for v in hoogtes):       # (a spiral passes a column more than once)
                    hoogtes.append(h2)
                o += 0.5
        afstand += lengte
    for (x, z), hoogtes in cellen.items():
        for h2 in hoogtes:
            s = h2 // 2
            if h2 % 2:
                b.zet(x, s, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
            b.zet(x, s - 1, z, TEGEL)
            b.zet(x, s - 2, z, STEEN)
            if _hash(x, z, seed, 3) < 0.06 and len(hoogtes) == 1:   # little drips under the path
                for i in range(3, 3 + int(_hash(x, z, seed, 4) * 4)):
                    b.zet(x, s - i, z, STEEN)
    # lotus leaves on the path (they lie in the top layer: half a block lower than the tiles)
    volgende = lotus_elke * 0.6
    for px, pz, h2, nx, nz, dd in midden:
        if lotus_elke and dd >= volgende and dd < totaal - 8:
            volgende += lotus_elke
            s = h2 // 2
            laag = s if h2 % 2 else s - 1
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if math.hypot(dx, dz) <= 3.2:
                        x, z = int(round(px)) + dx, int(round(pz)) + dz
                        c = (x, laag, z)
                        if c in b.s.blocks and b.s.blocks[c][0] in (TEGEL, PLAAT):
                            b.set(x, laag, z, LOTUSBLAD)
                        elif b.leeg(x, laag, z) and b.leeg(x, laag + 1, z) and (x, z) not in cellen:
                            b.set(x, laag, z, LOTUSBLAD)
                            b.zet(x, laag - 1, z, STEEN)
    # lanterns: on a tile beside the path, the sides taking turns
    volgende, kant = lantaarn_elke * 0.5, 1
    for px, pz, h2, nx, nz, dd in midden:
        if lantaarn_elke and dd >= volgende and 6 < dd < totaal - 6:
            volgende += lantaarn_elke
            kant = -kant
            x, z = int(round(px + nx * (half + 1) * kant)), int(round(pz + nz * (half + 1) * kant))
            s = h2 // 2
            basis = s if h2 % 2 else s - 1
            if b.leeg(x, basis, z) and b.leeg(x, basis + 1, z):
                b.set(x, basis, z, TEGEL)
                b.zet(x, basis - 1, z, STEEN)
                lantaarnpaal(b, x, basis + 1, z)
                # (tiles from the post's foot to the path, face to face: on a diagonal the foot touches it only by an edge)
                mx, mz = int(round(px)), int(round(pz))
                ox, oz = x, z
                for _ in range(6):
                    if ox != mx:
                        ox += 1 if mx > ox else -1
                    elif oz != mz:
                        oz += 1 if mz > oz else -1
                    else:
                        break
                    if not b.leeg(ox, basis, oz) or not b.leeg(ox, basis - 1, oz):
                        break
                    b.set(ox, basis - 1, oz, TEGEL)
    return [(round(px), h2 // 2, round(pz)) for px, pz, h2, _, _, _ in midden[::8]]


# =====================================================================================================================
# 1. the Meditatiecirkel (arrival)
# =====================================================================================================================
def meditatiecirkel(b):
    cx, cz, f, r = M["x"], M["z"], M["f"], M["r"]
    eiland(b, cx, cz, f, r, 18, 101)
    # the still pond (radius 15) around an islet (radius 5) with the lantern tree; stone lotus leaves across the water
    for x in range(cx - 16, cx + 17):
        for z in range(cz - 16, cz + 17):
            d = math.hypot(x - cx, z - cz)
            if 5.5 < d <= 15.2:
                b.set(x, f - 1, z, WATER, {"level": "0"})
                b.set(x, f - 2, z, WATER, {"level": "0"})
                b.set(x, f - 3, z, STEEN)
            elif d <= 5.5:
                b.set(x, f - 1, z, GRAS)
    for i, (dx, dz) in enumerate([(0, 13), (0, 9), (2, 6)]):       # stepping lotus leaves of stone from the south
        for ex in range(-1, 2):
            for ez in range(-1, 2):
                if abs(ex) + abs(ez) < 2 or i == 0:
                    b.set(cx + dx + ex, f - 1, cz + dz + ez, TEGEL)
    for k in range(10):                                              # stone lotus leaves around the pond
        th = k * 6.283 / 10 + 0.3
        lx, lz = cx + round(math.cos(th) * 11), cz + round(math.sin(th) * 11)
        for ex in range(-1, 2):
            for ez in range(-1, 2):
                if abs(ex) + abs(ez) < 2:
                    b.set(lx + ex, f - 1, lz + ez, STEEN)
    for k in range(6):                                               # a few real lotus leaves floating on the water
        th = k * 6.283 / 6 + 0.9
        b.set(cx + round(math.cos(th) * 8), f - 1, cz + round(math.sin(th) * 8), LOTUSBLAD)
    # the lantern tree: a white tree with cyan leaves; 8 empty hooks (a chain) and lanterns of the spirits that stayed
    boom(b, cx, f, cz, 13, 8, [CYAAN, CYAAN, ROZE], 111, hangers=False)
    b.prop("lantaarnboom", cx, f, cz, wie="de lantaarnboom (stam)")
    haken = [(6, 0), (-6, 0), (0, 6), (0, -6), (4, 4), (-4, 4), (4, -4), (-4, -4)]
    cy = f + 13 + 8 // 3

    def kroon_onder(hx, hz):
        # the lowest leaf of the crown in this column (under it a chain or a lantern can hang)
        y = next(yy for yy in range(cy + 6, f, -1) if not b.leeg(hx, yy, hz))
        while not b.leeg(hx, y - 1, hz):
            y -= 1
        return y
    for i, (dx, dz) in enumerate(haken, 1):
        hx, hz = cx + dx, cz + dz
        onder = kroon_onder(hx, hz)
        b.set(hx, onder - 1, hz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.prop(f"lantaarnboom_haak_{i}", hx, onder - 2, hz, wie="een leeg haakje: hier hangt een teruggebrachte lantaarngeest")
        b.los.add(f"lantaarnboom_haak_{i}")
    for dx, dz in [(3, 2), (-3, -2), (2, -3), (-2, 3)]:              # lanterns that are still home
        hx, hz = cx + dx, cz + dz
        onder = kroon_onder(hx, hz)
        b.set(hx, onder - 1, hz, LANTAARN, {"hanging": "true", "waterlogged": "false"})
    # the resident lantern spirits float round the tree on the islet
    for i, (dx, dz) in enumerate([(3, 0), (-3, 0), (0, 3), (0, -3)], 1):
        b.plek(f"lantaarngeest_{i}", cx + dx, f, cz + dz, yaw=YAW["south"], wie="een lantaarngeest bij de lantaarnboom",
               wezen="guhs:lantaarngeest")
    b.prop("lijf", cx + 2, f, cz + 3, wie="waar je lijf mediteert (cutscene)")
    # the arrival lawn to the south (9 x 9), the stones, the view
    b.zone("aankomst", cx - 4, f, cz + 18, cx + 4, f + 3, cz + 26, wie="aankomst: hier landt je ziel")
    b.prop("aankomst_speler", cx, f, cz + 22, wie="waar je aankomt (kijkt naar het noorden, naar de lantaarnboom)")
    b.prop("plek_meditatiecirkel", cx, f, cz + 22)
    b.vrij.append((cx, cz + 22, 6))
    b.vrij += [(cx - 26, cz, 5), (cx, cz - 26, 5), (cx + 26, cz, 5), (cx + 18, cz + 20, 5)]   # (where the paths leave)
    meditatiesteen(b, "meditatiecirkel", cx - 6, f, cz + 22)
    gedenksteen(b, "meditatiecirkel", cx + 6, f, cz + 22, "west", ["Meditatie-", "cirkel"])
    gedenksteen(b, "geestenwereld", cx - 6, f, cz + 26, "east", ["De Geesten-", "wereld"])
    gedenksteen(b, "lantaarnboom", cx + 2, f, cz - 3, "south", ["Lantaarnboom"])
    b.prop("uitzicht", cx, f + 34, cz + 64, wie="uitzicht: van het zuiden over de Meditatiecirkel")
    b.plek("libelkonijnguh_1", cx + 10, f, cz + 20, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    for k in range(5):                                               # a few small glowing trees round the lawn
        th = 0.6 + k * 1.25
        tx, tz = cx + round(math.cos(th) * 24), cz + round(math.sin(th) * 24)
        if b.is_vrij(tx, tz, 2):
            boom(b, tx, f, tz, 6 + k % 3, 3, ROZE if k % 2 else CYAAN, 120 + k)
    b.zone("zone_meditatiecirkel", cx - r - 4, f - 20, cz - r - 4, cx + r + 4, f + 30, cz + r + 4)


# =====================================================================================================================
# 2. the Gefluisterwoud
# =====================================================================================================================
def woud(b):
    cx, cz, f, r = WOUD["x"], WOUD["z"], WOUD["f"], WOUD["r"]
    eiland(b, cx, cz, f, r, 22, 202, rand_donker=0.74)
    ingang = (cx + 52, cz - 6)
    b.prop("plek_gefluisterwoud", ingang[0] - 4, f, ingang[1])
    meditatiesteen(b, "gefluisterwoud", ingang[0] - 6, f, ingang[1] + 4)
    gedenksteen(b, "gefluisterwoud", ingang[0] - 6, f, ingang[1] - 4, "east", ["Gefluister-", "woud"])
    b.vrij += [(ingang[0] - 4, ingang[1], 6), (cx + 4, cz - r + 4, 6)]
    # Hei Bai-guh's clearing: a burnt stump in a ring of saplings
    hx, hz = cx - 6, cz + 4
    b.vrij.append((hx, hz, 9))
    for y in range(f, f + 2):
        for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
            b.set(hx + dx, y, hz + dz, SHOUT, {"axis": "y"})
    b.set(hx + 2, f - 1, hz, AARDE)
    b.prop("eikel_plek", hx + 2, f, hz, wie="hier plant je de knabbelkoor-eikel (zijquest Hei Bai-guh)")
    for k in range(8):
        th = k * 6.283 / 8
        b.zet(hx + round(math.cos(th) * 6), f, hz + round(math.sin(th) * 6), "minecraft:birch_sapling", {"stage": "0"})
    b.plek("hei_bai", hx - 3, f, hz + 3, yaw=YAW["east"], wie="Hei Bai-guh, de bosgeest", wezen="guhs:hei_bai_guh")
    gedenksteen(b, "hei_bai", hx + 4, f, hz - 5, "south", ["Hei Bai-guh"])
    # shadow spirits in the dark rim; the dragonfly-bunnies; two lost lanterns
    for i, th in enumerate((2.3, 3.4, 4.3, 1.6), 1):
        sx, sz = cx + round(math.cos(th) * r * 0.82), cz + round(math.sin(th) * r * 0.82)
        b.plek(f"schaduwgeest_{i}", sx, f, sz, yaw=0.0, wie="een schaduwgeest in de donkere rand (duwt alleen)", wezen="guhs:schaduwgeest")
        b.vrij.append((sx, sz, 3))
    b.plek("libelkonijnguh_2", cx + 20, f, cz - 14, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    b.plek("libelkonijnguh_3", cx - 22, f, cz - 18, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    for sx, sz in ((cx + 20, cz - 14), (cx - 22, cz - 18)):
        b.vrij.append((sx, sz, 3))
    l1, l2 = (cx - 40, cz + 30), (cx + 8, cz + 38)
    b.prop("verdwaalde_lantaarn_1", l1[0], f, l1[1], wie="verdwaalde lantaarngeest (in de donkere rand)")
    b.prop("verdwaalde_lantaarn_2", l2[0], f, l2[1], wie="verdwaalde lantaarngeest (tussen de bomen)")
    b.vrij += [(l1[0], l1[1], 3), (l2[0], l2[1], 3)]
    # the trees: black-and-white with glowing leaves inside, dark ones at the rim
    n = 0
    for gx in range(cx - r, cx + r + 1, 9):
        for gz in range(cz - r, cz + r + 1, 9):
            tx = gx + int((_hash(gx, gz, 21) - 0.5) * 6)
            tz = gz + int((_hash(gx, gz, 22) - 0.5) * 6)
            d = math.hypot(tx - cx, tz - cz)
            if d > r * 0.9 or not b.is_vrij(tx, tz, 3) or (b.s.blocks.get((tx, f - 1, tz), ("",))[0] not in (GRAS, AARDE)):
                continue
            if d > r * 0.72:
                boom(b, tx, f, tz, 8 + int(_hash(tx, tz) * 5), 4, SCHADUW, 230 + n, stam=SHOUT, hangers=False)
            else:
                boom(b, tx, f, tz, 9 + int(_hash(tx, tz) * 7), 4 + int(_hash(tz, tx) * 2), [CYAAN, ROZE], 230 + n, banden=True)
            n += 1
    b.zone("zone_gefluisterwoud", cx - r - 8, f - 26, cz - r - 8, cx + r + 8, f + 30, cz + r + 8)
    b.zone("woud_rand", cx - r, f, cz - r, cx + r, f + 4, cz + r, wie="de donkere rand: buiten 72 % van de straal (schaduwgeesten)")
    return ingang


# =====================================================================================================================
# 3. Koh's Grot: an ancient tree on hollow roots, the room under it full of masks
# =====================================================================================================================
def koh_grot(b):
    cx, cz, f, r = KOH["x"], KOH["z"], KOH["f"], KOH["r"]
    eiland(b, cx, cz, f, r, 20, 303, rand_donker=0.5)
    binnen, buiten, dak = 16, 18, f + 15
    deur = lambda x, z, y: z > cz and abs(x - cx) <= 2 and y <= f + 4   # noqa: E731  (the way in, south)
    for x in range(cx - buiten - 1, cx + buiten + 2):
        for z in range(cz - buiten - 1, cz + buiten + 2):
            d = math.hypot(x - cx, z - cz)
            if d > buiten + 0.5:
                continue
            if d > binnen:
                for y in range(f, dak + 1):
                    if not deur(x, z, y):
                        b.set(x, y, z, SHOUT if _hash(x, y, z, 31) < 0.7 else HOUT, {"axis": "y"})
            else:
                plafond = dak - 3 + int(3 * (d / binnen) ** 2)
                for y in range(plafond, dak + 2):
                    b.set(x, y, z, SHOUT, {"axis": "y"})
                b.set(x, f - 1, z, AARDE if _hash(x, z, 32) < 0.8 else GRAS)
    # the tunnel through the wall (south), with a root arch
    for z in range(cz + binnen - 1, cz + buiten + 3):
        for x in range(cx - 3, cx + 4):
            for y in range(f, f + 6):
                if abs(x - cx) == 3 or y == f + 5:
                    b.zet(x, y, z, SHOUT, {"axis": "y"})
    # the roots outside: thick arches from the wall to the ground
    for k in range(12):
        th = k * 6.283 / 12 + 0.13
        if abs(math.atan2(math.sin(th - 1.5708), math.cos(th - 1.5708))) < 0.4:
            continue                                                  # (not over the way in)
        for i in range(0, 11):
            t = i / 10
            rr = buiten + t * 9
            y = dak - 3 - int(t * t * (dak - 3 - f + 1))
            rx, rz = cx + math.cos(th) * rr, cz + math.sin(th) * rr
            for ex in (-1, 0, 1):
                for ez in (-1, 0, 1):
                    b.zet(int(round(rx)) + ex, y, int(round(rz)) + ez, SHOUT, {"axis": "y"})
                    b.zet(int(round(rx)) + ex, y - 1, int(round(rz)) + ez, SHOUT, {"axis": "y"})
    # the trunk and the crown of the ancient tree
    for y in range(dak + 1, dak + 44):
        rr = 6 if y < dak + 36 else 4
        for x in range(cx - rr, cx + rr + 1):
            for z in range(cz - rr, cz + rr + 1):
                if math.hypot(x - cx, z - cz) <= rr + 0.3:
                    b.set(x, y, z, SHOUT if _hash(x, y, z, 33) < 0.8 else HOUT, {"axis": "y"})
    kroon_y, kr = dak + 46, 19
    for x in range(cx - kr - 1, cx + kr + 2):
        for z in range(cz - kr - 1, cz + kr + 2):
            for y in range(kroon_y - 9, kroon_y + 10):
                dd = math.sqrt((x - cx) ** 2 + ((y - kroon_y) * 2.0) ** 2 + (z - cz) ** 2)
                if kr - 3.5 < dd <= kr + 1.5 * (ruis(x, z, 5, 34 + y) - 0.5) and _hash(x, y, z, 35) < 0.88:
                    b.zet(x, y, z, ROZE if _hash(x, y, z, 36) < 0.45 else (CYAAN if _hash(x, y, z, 37) < 0.5 else SCHADUW))
    for k in range(14):                                               # branches into the crown
        th = k * 6.283 / 14
        lijn6(b, (cx + round(math.cos(th) * 4), dak + 36, cz + round(math.sin(th) * 4)),
              (cx + round(math.cos(th) * (kr - 3)), dak + 34 + ((kr - 3) * 2) // 3, cz + round(math.sin(th) * (kr - 3))), SHOUT, {"axis": "y"})
    # the masks: hundreds of faces on the inside of the wall, looking into the room; a few winks among them
    vast = {(cx - 5, f + 2): "mika", (cx + 6, f + 2): "guhdalf", (cx - 9, f + 1): "sponsguh", (cx + 9, f + 1): "blauwe_geest"}
    geplaatst = {}
    for x in range(cx - binnen, cx + binnen + 1):
        for z in range(cz - binnen, cz + binnen + 1):
            d = math.hypot(x - cx, z - cz)
            if not (binnen - 1.6 < d <= binnen + 0.01):
                continue
            for y in range(f, dak - 3):
                if not b.leeg(x, y, z):
                    continue
                for facing, (fx, fz) in DIRS.items():
                    achter = (x - fx, y, z - fz)
                    voor = (x + fx, y, z + fz)
                    if achter in b.s.blocks and b.s.blocks[achter][0] in (SHOUT, HOUT) and b.leeg(*voor) \
                            and math.hypot(voor[0] - cx, voor[2] - cz) < d:
                        if y == f and _hash(x, z, 40) < 0.7:
                            break                                     # (the floor row stays mostly free)
                        if _hash(x, y, z, 41) < 0.62:
                            gezicht = MASKERS.index("blanco") if _hash(x, y, z, 42) < 0.18 else int(_hash(x, y, z, 43) * len(MASKERS))
                            if MASKERS[gezicht] in ("mika", "guhdalf", "sponsguh", "blauwe_geest"):
                                gezicht = MASKERS.index("noh")
                            b.set(x, y, z, MASKER, {"facing": facing, "gezicht": str(gezicht)})
                            geplaatst[(x, y, z)] = facing
                        break
    # the four winks, near the way in at eye height (each on the wall, a prop marker)
    for (wx, wy), naam in vast.items():
        best = min((c for c in geplaatst if c[1] == wy), key=lambda c: abs(c[0] - wx) + (0 if c[2] > cz else 100))
        b.set(*best, MASKER, {"facing": geplaatst[best], "gezicht": str(MASKERS.index(naam))})
        b.prop(f"masker_{naam}", *best, wie=f"het {naam}-masker tussen de gezichten (knipoog)")
        b.los.add(f"masker_{naam}")
    # dim lights: lanterns on low stones, glowing mushrooms
    for k in range(8):
        th = k * 6.283 / 8 + 0.4
        lx, lz = cx + round(math.cos(th) * 11), cz + round(math.sin(th) * 11)
        b.set(lx, f, lz, STEEN)
        b.set(lx, f + 1, lz, LANTAARN, {"hanging": "false", "waterlogged": "false"})
    for k in range(10):
        th = k * 6.283 / 10 + 0.1
        b.zet(cx + round(math.cos(th) * 14), f, cz + round(math.sin(th) * 14), HOED)
    for k in range(5):                                                # small glowing caps round Koh's spot
        th = k * 6.283 / 5 + 0.5
        b.zet(cx + round(math.cos(th) * 7), f, cz - 2 + round(math.sin(th) * 7), HOED)
    b.plek("koh", cx, f, cz - 2, yaw=YAW["south"], wie="Koh de Gezichtenrover (duizendpoot, ~12 blokken)", wezen="guhs:koh")
    b.prop("koh_raadsel", cx, f, cz + 6, wie="waar je staat voor Koh's praatmoment / de raadselronde (geen emotie tonen)")
    b.prop("grot_ingang", cx, f, cz + buiten + 2, wie="de ingang van Koh's Grot")
    b.prop("verdwaalde_lantaarn_3", cx - 10, f, cz - 6, wie="verdwaalde lantaarngeest (tussen de maskers)")
    b.zone("koh_kamer", cx - binnen + 1, f, cz - binnen + 1, cx + binnen - 1, dak - 4, cz + binnen - 1, wie="de kamer met de maskers")
    b.prop("plek_koh_grot", cx, f, cz + buiten + 5)
    meditatiesteen(b, "koh_grot", cx + 5, f, cz + buiten + 5)
    gedenksteen(b, "koh", cx - 5, f, cz + buiten + 5, "south", ["Koh's Grot", "geen emotie"])
    b.vrij.append((cx, cz, buiten + 10))
    for x in range(cx - binnen + 1, cx + binnen):
        for z in range(cz - binnen + 1, cz + binnen):
            if math.hypot(x - cx, z - cz) < binnen - 2 and b.leeg(x, f, z):
                b.licht_kamer.append((x, f, z))
    b.zone("zone_koh_grot", cx - r - 8, f - 24, cz - r - 8, cx + r + 8, kroon_y + 12, cz + r + 8)
    return (cx, cz + r - 2)


# =====================================================================================================================
# 4. the Witte Lotus-ruines and 5. the Top der Voorgangers
# =====================================================================================================================
VOORGANGERS = ["aang", "roku", "kyoshi", "kuruk", "yangchen", "szeto", "wan", "leeg"]
VOORGANGER_BORD = {"aang": ["Aang-guh", "lucht"], "roku": ["Roku-guh", "vuur"], "kyoshi": ["Kyoshi-guh", "aarde"],
                   "kuruk": ["Kuruk-guh", "water"], "yangchen": ["Yangchen-guh", "lucht"], "szeto": ["Szeto-guh", "vuur"],
                   "wan": ["Wan-guh", "de eerste"], "leeg": ["...", "(nog leeg)"]}
# the 3 x 3 puzzle: (row, column) -> (piece, the turn that makes the lotus whole)
TEGELS = {(0, 0): ("hoek", 0), (0, 1): ("rand", 0), (0, 2): ("hoek", 1), (1, 2): ("rand", 1), (2, 2): ("hoek", 2), (2, 1): ("rand", 2),
          (2, 0): ("hoek", 3), (1, 0): ("rand", 3), (1, 1): ("midden", 0)}


def fresco(b, x0, y0, z):
    """Two spirits turned into each other, light and dark, on the wall (its south face at z)."""
    bw, bh = 15, 11
    for u in range(bw):
        for v in range(bh):
            px, py = (u - (bw - 1) / 2) / ((bw - 1) / 2), (v - (bh - 1) / 2) / ((bh - 1) / 2)
            d = math.hypot(px, py)
            hoek = math.atan2(py, px)
            if d > 1.02:
                kleur = "minecraft:light_gray_concrete"
            else:
                licht = math.sin(hoek + d * 4.2) > 0
                kleur = "minecraft:white_concrete" if licht else "minecraft:black_concrete"
                if 0.85 < d:
                    kleur = "minecraft:cyan_concrete" if licht else "minecraft:purple_concrete"
            b.set(x0 + u, y0 + v, z, kleur)
    # the two eyes: a dark one in the light spirit, a light one in the dark spirit
    b.set(x0 + 4, y0 + 6, z, "minecraft:purple_concrete")
    b.set(x0 + 10, y0 + 4, z, "minecraft:light_blue_concrete")


def lotus_ruines(b):
    cx, cz, f, r = LOTUS["x"], LOTUS["z"], LOTUS["f"], LOTUS["r"]
    eiland(b, cx, cz, f, r, 22, 404)
    # the floor of the temple: courtyard (south) and hall (north), tiles with grass breaking through
    for x in range(cx - 20, cx + 21):
        for z in range(cz - 36, cz + 44):
            if b.s.blocks.get((x, f - 1, z), ("",))[0] in (GRAS, AARDE) and _hash(x, z, 41) < 0.86:
                b.set(x, f - 1, z, TEGEL)
    # the gate (z = cz + 20): an arch with a lotus over it, an opening 5 wide and 6 high
    gz = cz + 20
    for x in range(cx - 8, cx + 9):
        for y in range(f, f + 10):
            if not (abs(x - cx) <= 2 and y < f + 6):
                b.set(x, y, gz, TEGEL)
                b.set(x, y, gz + 1, TEGEL)
    for dx, dy in ((0, 7), (-1, 8), (1, 8), (0, 8), (0, 9), (-2, 9), (2, 9)):
        b.set(cx + dx, f + dy, gz + 2, "minecraft:white_concrete")
        b.zet(cx + dx, f + dy, gz + 1, TEGEL)
    b.set(cx, f + 8, gz + 2, "minecraft:pink_concrete")
    b.prop("poort", cx, f, gz, wie="de poort van de Witte Lotus (gaat in fase B dicht tot de puzzel klopt)")
    b.zone("poort_doorgang", cx - 2, f, gz, cx + 2, f + 5, gz + 1, wie="de doorgang van de poort")
    # the puzzle before the gate: 3 x 3 tiles in the floor, turned wrong
    for (rij, kol), (stuk, goed) in TEGELS.items():
        x, z = cx - 1 + kol, gz + 6 + rij
        draai = (goed + 1 + int(_hash(rij, kol, 44) * 3)) % 4
        b.set(x, f - 1, z, PUZZEL, {"stuk": stuk, "draai": str(draai)})
        n = rij * 3 + kol + 1
        b.prop(f"tegel_{n}", x, f - 1, z, wie=f"puzzeltegel {stuk} (rij {rij + 1}, kolom {kol + 1}); goed bij draai {goed}"
                                               + (" (de middelste: elke draai is goed)" if stuk == "midden" else ""))
    for x in range(cx - 2, cx + 3):                                   # a rim of dark tiles round the puzzle
        for z in range(gz + 5, gz + 10):
            if b.s.blocks.get((x, f - 1, z), ("",))[0] != PUZZEL:
                b.set(x, f - 1, z, "minecraft:polished_deepslate")
    gedenksteen(b, "witte_lotus", cx + 5, f, gz + 7, "west", ["Witte Lotus"])
    b.prop("plek_lotus_ruines", cx, f, gz + 14)
    meditatiesteen(b, "lotus_ruines", cx - 5, f, gz + 12)
    b.plek("libelkonijnguh_4", cx + 12, f, gz + 12, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    # broken columns in the courtyard
    for k, (dx, dz) in enumerate(((-12, 8), (12, 8), (-12, 16), (12, 16), (-16, 24), (16, 24))):
        if b.leeg(cx + dx, f - 1, gz + dz):
            continue                                                  # (no ground there: the island's edge)
        for y in range(f, f + 3 + int(_hash(k, 45) * 6)):
            b.set(cx + dx, y, gz + dz, TEGEL if y % 3 else STEEN)
        b.zet(cx + dx + 1, f, gz + dz, TEGEL)                           # a fallen piece
    # the Hal van de Voorgangers: from the gate north to the fresco wall (z = cz - 32)
    hz0, hz1 = cz - 31, gz - 1
    for z in range(hz0, hz1 + 1):
        for x in (cx - 10, cx + 10):
            if (z - hz0) % 5 == 0:
                hoog = 8 if _hash(x, z, 46) < 0.75 else 3 + int(_hash(x, z, 47) * 4)
                for y in range(f, f + hoog):
                    b.set(x, y, z, TEGEL)
    for z in range(hz0, hz1 + 1):                                     # the roof: slabs, with holes where it fell in
        for x in range(cx - 11, cx + 12):
            # (rows 0 and 2 of every five are whole, so every slab of the other rows touches one of them)
            if (z - hz0) % 5 in (0, 2) or _hash(x, z, 48) < 0.55:
                b.zet(x, f + 8, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    for z in range(hz0, hz1 + 1, 5):
        for x in range(cx - 10, cx + 11):
            b.set(x, f + 7, z, PLANK)
    b.zone("hal", cx - 9, f, hz0 + 1, cx + 9, f + 6, hz1, wie="de Hal van de Voorgangers")
    # the eight memorial stones: four along each side, the text side to the aisle
    for i, id in enumerate(VOORGANGERS):
        kant = -1 if i % 2 == 0 else 1
        z = hz0 + 5 + (i // 2) * 6
        x = cx + kant * 7
        facing = "east" if kant < 0 else "west"
        b.set(x, f, z, TEGEL)
        b.set(x, f + 1, z, GEDENK, {"facing": facing})
        b.sign(x, f + 2, z, facing, VOORGANGER_BORD[id], wall=False)
        b.prop(f"lore_{id}", x, f + 1, z, wie=f"gedenksteen: {tekst.LORE[id][0]}")
        b.los.add(f"lore_{id}")                                       # (on a plinth: you stand before it, in the aisle)
        b.set(x + (1 if kant < 0 else -1), f, z, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    # the fresco on the north wall and its sign
    wz = hz0 - 1
    for x in range(cx - 11, cx + 12):
        for y in range(f, f + 13):
            b.set(x, y, wz, TEGEL)
    fresco(b, cx - 7, f + 1, wz + 1)
    b.set(cx, f, wz + 2, TEGEL)
    b.sign(cx, f, wz + 3, "south", ["Raava en Vaatu?", "Nooit van", "gehoord."], wall=True)
    b.prop("fresco", cx, f, wz + 4, wie="het fresco: twee geesten in elkaar gedraaid, licht en donker")
    b.prop("verdwaalde_lantaarn_4", cx - 8, f, hz0 + 2, wie="verdwaalde lantaarngeest (een hoek van de hal)")
    # the Hemeltrap: a spiral of floating steps round a column of rising mist, up to the Top der Voorgangers
    sx, sz, sr = cx + 26, cz - 46, 8
    for y in range(f - 6, TOP["f"] + 6):
        b.set(sx, y, sz, NEVEL)
    b.set(sx, f - 7, sz, STEEN)
    b.los_cellen |= {(sx, y, sz) for y in range(f - 7, TOP["f"] + 6)}
    start = (cx + 11, hz0 + 2)
    th0 = math.atan2(start[1] - sz, start[0] - sx)
    hoogte = TOP["f"] - f
    lengte_nodig = hoogte / 0.42
    omwentelingen = lengte_nodig / (2 * math.pi * sr)
    # end where the bridge to the Top leaves (north side)
    th_eind = -math.pi / 2
    n_turn = math.ceil(omwentelingen)
    th1 = th_eind - 2 * math.pi * n_turn
    if th1 > th0:
        th1 -= 2 * math.pi
    while th0 - th1 < 2 * math.pi * omwentelingen:
        th1 -= 2 * math.pi
    stappen = 240
    spiraal = [(cx + 11, hz0 + 2), (cx + 15, hz0 + 2)]
    for i in range(stappen + 1):
        th = th0 + (th1 - th0) * i / stappen
        spiraal.append((sx + math.cos(th) * sr, sz + math.sin(th) * sr))
    # (the spiral starts with a short level walk from the door, then climbs)
    route = pad(b, spiraal, f, TOP["f"], half=1, lantaarn_elke=18, lotus_elke=0, seed=405)
    eind = spiraal[-1]
    brug = pad(b, [eind, (eind[0], TOP["z"] + 18), (TOP["x"] + 3, TOP["z"] + 8)], TOP["f"], TOP["f"], half=1, lantaarn_elke=0,
               lotus_elke=0, seed=406)
    b.pad("hemeltrap", route + brug, wie="de Hemeltrap: van de hal omhoog naar de Top der Voorgangers")
    b.zone("zone_lotus_ruines", cx - r - 8, f - 26, cz - r - 8, cx + r + 8, f + 20, cz + r + 8)
    return (cx, gz + 26)


def top_voorgangers(b):
    cx, cz, f, r = TOP["x"], TOP["z"], TOP["f"], TOP["r"]
    eiland(b, cx, cz, f, r, 16, 505)
    for k in range(10):                                               # a circle of standing stones
        th = k * 6.283 / 10
        x, z = cx + round(math.cos(th) * 10), cz + round(math.sin(th) * 10)
        if z > cz + 6 and abs(x - cx) < 4:
            continue                                                  # (the way in, south)
        for y in range(f, f + 3 + k % 2):
            b.set(x, y, z, TEGEL)
        b.set(x, f + 3 + k % 2, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            if math.hypot(x - cx, z - cz) <= 4.3:
                b.set(x, f - 1, z, TEGEL if (x + z) % 2 else STEEN)
    b.plek("roku", cx - 3, f, cz - 2, yaw=YAW["south"], wie="Roku-guh als geest (h4: raad voor de finale)", kind="ROKU_GUH")
    b.plek("kyoshi", cx + 3, f, cz - 2, yaw=YAW["south"], wie="Kyoshi-guh als geest (h4: raad voor de finale)", kind="KYOSHI_GUH")
    b.prop("fang_landing", cx, f, cz + 6, wie="waar Fang je neerzet")
    b.prop("plek_top_voorgangers", cx, f, cz + 8)
    meditatiesteen(b, "top_voorgangers", cx + 6, f, cz + 6)
    gedenksteen(b, "top", cx - 6, f, cz + 6, "south", ["Top der", "Voorgangers"])
    b.prop("verdwaalde_lantaarn_5", cx + 7, f, cz - 7, wie="verdwaalde lantaarngeest (heel hoog)")
    b.zone("top", cx - r, f, cz - r, cx + r, f + 6, cz + r, wie="de top")
    b.zone("zone_top_voorgangers", cx - r - 6, f - 20, cz - r - 6, cx + r + 6, f + 16, cz + r + 6)


# =====================================================================================================================
# 6. the Hemelvijver
# =====================================================================================================================
def hemelvijver(b):
    cx, cz, f, r = VIJVER["x"], VIJVER["z"], VIJVER["f"], VIJVER["r"]
    # a bowl that hangs in the sky: a rim to walk on, water in the middle, an upside-down dome under it
    for x in range(cx - r - 2, cx + r + 3):
        for z in range(cz - r - 2, cz + r + 3):
            d = math.hypot(x - cx, z - cz)
            rr = r * (1 + 0.05 * hoekruis(math.atan2(z - cz, x - cx), 606))
            if d > rr:
                continue
            t = d / rr
            bodem = f - 2 - int(16 * math.sqrt(max(0.0, 1 - t * t)))
            if d > rr - 5:
                b.set(x, f - 1, z, TEGEL if d < rr - 1 else STEEN)
                for y in range(bodem, f - 1):
                    b.set(x, y, z, STEEN)
            else:
                diep = 2 + int(5 * (1 - t))
                for y in range(f - 1 - diep, f - 1):
                    b.set(x, y, z, WATER, {"level": "0"})
                for y in range(bodem, f - 1 - diep):
                    b.set(x, y, z, STEEN)
            if _hash(x, z, 61) < 0.05 and t < 0.9:                    # glowing roots under the bowl
                for i in range(1, 2 + int(_hash(x, z, 62) * 7)):
                    b.zet(x, bodem - i, z, CYAAN if _hash(x, z, 63) < 0.5 else ROZE)
    # mist falling UP out of the pond, in four columns
    for k in range(4):
        th = k * 6.283 / 4 + 0.785
        x, z = cx + round(math.cos(th) * 14), cz + round(math.sin(th) * 14)
        for y in range(f - 1, f + 44):
            b.set(x, y, z, NEVEL)
            if y < f + 10:
                b.zet(x + 1, y, z, NEVEL)
                b.zet(x, y, z + 1, NEVEL)
    # lotus leaves on the water
    for k in range(9):
        th = k * 6.283 / 9 + 0.2
        b.set(cx + round(math.cos(th) * 7), f - 1, cz + round(math.sin(th) * 7), LOTUSBLAD)
    # Tui and La circle above the water; the yin-yang circle as a pad
    cirkel = [(cx + round(math.cos(k * 6.283 / 16) * 12), f + 6, cz + round(math.sin(k * 6.283 / 16) * 12)) for k in range(16)]
    b.pad("koi_cirkel", cirkel, wie="de cirkel die Tui en La zwemmen (yin-yang), boven de vijver")
    b.plek("koi_tui", cx - 12, f + 6, cz, yaw=YAW["north"], wie="Tui, de maangeest (witte koi met zwarte vlek)", wezen="guhs:geest_koi", los=True)
    b.plek("koi_la", cx + 12, f + 6, cz, yaw=YAW["south"], wie="La, de oceaangeest (zwarte koi met witte vlek)", wezen="guhs:geest_koi", los=True)
    b.zone("vijver", cx - r + 5, f - 8, cz - r + 5, cx + r - 5, f - 1, cz + r - 5, wie="het water van de Hemelvijver")
    west = (cx - r + 2, cz)
    b.prop("plek_hemelvijver", west[0] + 1, f, west[1])
    meditatiesteen(b, "hemelvijver", west[0] + 1, f, west[1] + 4)
    gedenksteen(b, "hemelvijver", west[0] + 1, f, west[1] - 4, "east", ["Hemelvijver"])
    b.plek("libelkonijnguh_5", cx, f, cz + r - 2, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    b.prop("verdwaalde_lantaarn_6", cx + r - 3, f, cz + 3, wie="verdwaalde lantaarngeest (op de rand van de vijver)")
    b.zone("zone_hemelvijver", cx - r - 6, f - 30, cz - r - 6, cx + r + 6, f + 46, cz + r + 6)
    return west


# =====================================================================================================================
# 7. the Theepaviljoen
# =====================================================================================================================
def theepaviljoen(b):
    cx, cz, f, r = THEE["x"], THEE["z"], THEE["f"], THEE["r"]
    eiland(b, cx, cz, f, r, 16, 707)

    def oct_in(dx, dz, rr):
        return abs(dx) <= rr and abs(dz) <= rr and abs(dx) + abs(dz) <= rr * 1.42

    for dx in range(-10, 11):                                        # the floor, one step up
        for dz in range(-10, 11):
            if oct_in(dx, dz, 9):
                b.set(cx + dx, f, cz + dz, PLANK)
                b.set(cx + dx, f - 1, cz + dz, STEEN)
    for dz in (10, 11):                                              # steps up on the north side (towards the path)
        pass
    for x in range(cx - 2, cx + 3):
        b.set(x, f, cz - 10, TRAP, {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    pilaren = [(dx, dz) for dx, dz in ((8, 3), (8, -3), (-8, 3), (-8, -3), (3, 8), (-3, 8), (3, -8), (-3, -8))]
    for dx, dz in pilaren:
        for y in range(f + 1, f + 7):
            b.set(cx + dx, y, cz + dz, HOUT, {"axis": "y"})
    for laag, rr in enumerate((11, 9, 7, 5, 3, 1)):                  # the roof: stepping in, pink leaves on its edge
        y = f + 7 + laag
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                if oct_in(dx, dz, rr):
                    b.set(cx + dx, y, cz + dz, SPLANK if laag % 2 == 0 else PLANK)
                    if laag == 0 and not oct_in(dx, dz, rr - 1) and (dx + dz) % 2 == 0:
                        b.set(cx + dx, y - 1, cz + dz, ROZE)
    b.set(cx, f + 13, cz, "minecraft:birch_fence")
    b.set(cx, f + 14, cz, LANTAARN, {"hanging": "false", "waterlogged": "false"})
    b.set(cx, f + 6, cz, LANTAARN, {"hanging": "true", "waterlogged": "false"})
    for dx, dz in ((4, 4), (-4, -4), (4, -4), (-4, 4)):
        b.set(cx + dx, f + 6, cz + dz, LANTAARN, {"hanging": "true", "waterlogged": "false"})
    # the tea table with the pot and candles, cushions of maansteen slab round it
    b.set(cx, f + 1, cz, PLANK)
    b.set(cx, f + 2, cz, "minecraft:decorated_pot", {"facing": "north", "waterlogged": "false", "cracked": "false"})
    for dx, dz in ((1, 0), (0, 1), (-1, 0)):
        b.set(cx + dx, f + 1, cz + dz, PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(cx + 1, f + 2, cz + 1, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    b.set(cx + 1, f + 1, cz + 1, PLANK)
    b.prop("theetafel", cx, f + 1, cz, wie="de theetafel met de pot")
    b.plek("theegeest", cx, f + 1, cz - 2, yaw=YAW["south"], wie="de Theegeest (zit aan zijn theetafel)", kind="THEEGEEST_GUH")
    b.sign(cx - 2, f + 1, cz - 2, "south", ["Theepaviljoen", "Open: altijd", "(ook als er", "geen altijd is)"], wall=False)
    b.prop("plek_theepaviljoen", cx, f, cz - 14)
    meditatiesteen(b, "theepaviljoen", cx + 5, f, cz - 14)
    gedenksteen(b, "theepaviljoen", cx - 5, f, cz - 14, "north", ["Theepaviljoen"])
    b.vrij += [(cx, cz, 12), (cx, cz - 14, 6)]
    b.plek("libelkonijnguh_6", cx + 14, f, cz + 6, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    b.prop("verdwaalde_lantaarn_7", cx - 14, f, cz + 10, wie="verdwaalde lantaarngeest (in de theetuin)")
    for k, (dx, dz) in enumerate(((15, -8), (-16, 4), (10, 16), (-8, 18))):   # a tea garden of mushrooms
        paddenstoel(b, cx + dx, f, cz + dz, 4 + k % 3, 3, 700 + k)
    b.zone("zone_theepaviljoen", cx - r - 6, f - 20, cz - r - 6, cx + r + 6, f + 20, cz + r + 6)
    return (cx, cz - r + 2)


# =====================================================================================================================
# 8. Fang's Hemelpad
# =====================================================================================================================
def fang_pad(b):
    cx, cz, f, r = FANG["x"], FANG["z"], FANG["f"], FANG["r"]
    eiland(b, cx, cz, f, r, 12, 808)
    for dx in (-4, 4):                                               # a white torii with pink leaves on its beam
        for y in range(f, f + 8):
            b.set(cx + dx, y, cz - 4, HOUT, {"axis": "y"})
    for dx in range(-6, 7):
        b.set(cx + dx, f + 8, cz - 4, SPLANK)
        b.set(cx + dx, f + 6, cz - 4, PLANK) if abs(dx) <= 4 else None
    for dx in range(-6, 7, 3):
        b.set(cx + dx, f + 9, cz - 4, ROZE)
    b.plek("fang", cx, f, cz - 8, yaw=YAW["south"], wie="Fang, de geestdraak van Roku-guh (wacht hier, zadel)", wezen="guhs:fang")
    b.prop("fang_opstap", cx, f, cz - 2, wie="waar je op Fang stapt")
    b.prop("plek_fang_pad", cx, f, cz + 4)
    meditatiesteen(b, "fang_pad", cx + 4, f, cz + 4)
    gedenksteen(b, "fang", cx - 4, f, cz + 4, "south", ["Fang's", "Hemelpad"])
    b.prop("verdwaalde_lantaarn_8", cx + 7, f, cz - 6, wie="verdwaalde lantaarngeest (bij Fang)")
    tx, ty, tz = TOP["x"], TOP["f"], TOP["z"] + 6
    route = []
    for i in range(25):
        t = i / 24
        x = cx + (tx - cx) * t + math.sin(t * math.pi) * 30
        z = cz - 8 + (tz - (cz - 8)) * t - math.sin(t * math.pi) * 34
        y = f + 2 + (ty + 1 - f - 2) * t + math.sin(t * math.pi) * 22
        route.append((int(round(x)), int(round(y)), int(round(z))))
    b.pad("fang_vlucht", route, wie="Fang's vlucht van zijn rustplek naar de Top der Voorgangers (h4)")
    b.zone("zone_fang_pad", cx - r - 6, f - 16, cz - r - 6, cx + r + 6, f + 14, cz + r + 6)


# =====================================================================================================================
# decoration between the zones (floating on purpose)
# =====================================================================================================================
EILANDJES = [  # x, z, floor, radius, what
    (60, 70, 70, 7, "paddenstoel"), (250, 40, 92, 6, "nevel"), (560, 60, 60, 8, "paddenstoel"), (40, 230, 58, 6, "boom"),
    (250, 230, 74, 5, "nevel"), (410, 220, 84, 6, "paddenstoel"), (580, 250, 96, 7, "boom"), (560, 420, 72, 8, "paddenstoel"),
    (560, 580, 60, 7, "nevel"), (330, 580, 86, 6, "boom"), (220, 500, 66, 8, "paddenstoel"), (60, 540, 80, 7, "nevel"),
    (40, 420, 104, 5, "paddenstoel"), (250, 420, 40, 6, "boom"), (380, 380, 90, 5, "nevel"), (130, 30, 52, 6, "boom"),
]


def eilandjes(b):
    voor = set(b.s.blocks)
    for i, (x, z, f, r, wat) in enumerate(EILANDJES):
        eiland(b, x, z, f, r, 6 + r, 900 + i)
        if wat == "paddenstoel":
            paddenstoel(b, x, f, z, 8 + i % 5, 5 + i % 3, 910 + i)
        elif wat == "boom":
            boom(b, x, f, z, 7 + i % 4, 4, [ROZE, CYAAN], 920 + i)
        else:
            for y in range(f - 1, f + 30 + i % 10):                  # a waterfall that falls up out of a little pool
                b.set(x, y, z, NEVEL)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if dx or dz:
                        b.set(x + dx, f - 1, z + dz, WATER, {"level": "0"})
                        b.set(x + dx, f, z + dz, STEEN) if abs(dx) + abs(dz) == 2 else None
    # loose lotus leaves drifting in the mist
    for i in range(40):
        x, z = 20 + int(_hash(i, 1, 990) * 568), 20 + int(_hash(i, 2, 990) * 568)
        y = 30 + int(_hash(i, 3, 990) * 90)
        if all(b.leeg(x + dx, y + dy, z + dz) for dx in range(-3, 4) for dz in range(-3, 4) for dy in range(-3, 4)):
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if math.hypot(dx, dz) <= 2.3:
                        b.set(x + dx, y, z + dz, LOTUSBLAD)
    b.los_cellen |= set(b.s.blocks) - voor


# =====================================================================================================================
def maak(h):
    b = Geest(h)
    # (the clearings of the markers first: trees keep out of them)
    meditatiecirkel(b)
    woud_ingang = woud(b)
    koh_ingang = koh_grot(b)
    lotus_poort = lotus_ruines(b)
    top_voorgangers(b)
    vijver_west = hemelvijver(b)
    thee_noord = theepaviljoen(b)
    fang_pad(b)
    mx, mz, mf = M["x"], M["z"], M["f"]
    # the paths between the zones
    pad(b, [(mx - 30, mz), (mx - 60, mz + 4), (woud_ingang[0] + 4, woud_ingang[1] + 2), (woud_ingang[0] - 2, woud_ingang[1])],
        mf, WOUD["f"], seed=1)
    pad(b, [(mx, mz - 30), (mx + 6, mz - 70), (lotus_poort[0], lotus_poort[1] + 18), (lotus_poort[0], lotus_poort[1] - 4)],
        mf, LOTUS["f"], seed=2)
    pad(b, [(mx + 30, mz), (mx + 80, mz - 6), (vijver_west[0] - 6, vijver_west[1]), (vijver_west[0] + 2, vijver_west[1])],
        mf, VIJVER["f"], seed=3)
    pad(b, [(mx + 20, mz + 22), (mx + 70, mz + 80), (thee_noord[0] - 10, thee_noord[1] - 20), (thee_noord[0], thee_noord[1] + 2)],
        mf, THEE["f"], seed=4)
    pad(b, [(WOUD["x"] + 4, WOUD["z"] - WOUD["r"] + 4), (WOUD["x"] + 10, WOUD["z"] - 100), (koh_ingang[0], koh_ingang[1] + 6),
            (koh_ingang[0], koh_ingang[1] - 2)], WOUD["f"], KOH["f"], seed=5)
    pad(b, [(LOTUS["x"] - LOTUS["r"] + 4, LOTUS["z"] + 6), (LOTUS["x"] - 80, LOTUS["z"] + 10), (KOH["x"] + KOH["r"] - 2, KOH["z"] + 8)],
        LOTUS["f"], KOH["f"], seed=6)
    pad(b, [(VIJVER["x"], VIJVER["z"] - VIJVER["r"] + 2), (VIJVER["x"] + 10, VIJVER["z"] - 80), (FANG["x"], FANG["z"] + FANG["r"] - 2)],
        VIJVER["f"], FANG["f"], seed=7)
    pad(b, [(THEE["x"] + 18, THEE["z"] - 18), (VIJVER["x"] - 4, VIJVER["z"] + 90), (VIJVER["x"], VIJVER["z"] + VIJVER["r"] - 3)],
        THEE["f"], VIJVER["f"], seed=8)
    eilandjes(b)
    opruimen(b)
    b.connect()
    return b


def opruimen(b):
    """Leaves that ended up alone (a hole in a crown can cut a few off): taken away."""
    from features import guheinde_bouw as gb
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    onder = (start[0], start[1] - 1, start[2])
    for c in gb.floating(b, lambda c: c == onder):
        if c not in b.los_cellen and b.s.blocks[c][0] in (CYAAN, ROZE, SCHADUW):
            del b.s.blocks[c]


def controleer(b):
    """The kit's check (markers, reach from the arrival, floating blocks, signs and lanterns hanging on something) plus
    light in Koh's room and every marker the Java side asks for."""
    verwacht = [f"zone_{z}" for z in ZONES] + [f"plek_{z}" for z in ZONES] + [f"meditatiesteen_{z}" for z in ZONES]
    verwacht += [f"lore_{id}" for id in tekst.LORE] + [("verdwaalde_lantaarn", 8), ("lantaarnboom_haak", 8), ("tegel", 9),
                                                         ("lantaarngeest", 4), ("schaduwgeest", 4), ("libelkonijnguh", 6)]
    verwacht += ["hei_bai", "koh", "koi_tui", "koi_la", "theegeest", "roku", "kyoshi", "fang", "poort", "fresco", "hemeltrap",
                 "fang_vlucht", "koi_cirkel", "eikel_plek", "lantaarnboom", "theetafel", "fang_landing"]
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    onder = b.los_cellen
    problems = b.check(start, verwacht=verwacht, min_licht=0, zwevend_ok=lambda c: c in onder)
    licht = b.licht()
    donker = [c for c in b.licht_kamer if licht.get(c, 0) < 2]
    if donker:
        problems.append(f"Koh's kamer: {len(donker)} plekken zonder licht, bv. {donker[:4]}")
    for id, e in b.plekken.items():
        if id.startswith("plek_") or id.startswith("meditatiesteen_"):
            x, y, z = e["pos"]
            if not (0 <= x < W and 0 <= z < D):
                problems.append(f"{id} ligt buiten de wereld")
    return problems
