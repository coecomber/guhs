"""
Het Snuffeleiland: the ISLAND itself (DESIGN_VERHALENPAD C, slice snuffel-dorp). One fixed, hand-laid-out island in the
flat sea of guhs:snuffeleiland, written as tiles `guhs:snuffeldorp/eiland_<i>_<j>` + data/guhs/snuffel/eiland.json (the
kern's Eiland.java stamps it once and keeps the residents and the tree on their spots).

The map (template coordinates: x east, z south; the ground's top is world y 64):

      north:  the CLOSED part: wooded hills, a rocky top, standing stones and a lighthouse on the north-east cape
              ---- a ridge of rock from coast to coast, with ONE road through it: the friendly roadblock ----
      the meadow ("de wei"): Meester Truffelneus' snuffelschooltje, the pond, the bee tree, and on a knoll the ring of
              stones where the companion's tree grows
              ---- a hedge with the weipoort ----
      Snuffeldorp: the plein with the well, the dokterspraktijk, the bakkerij, the schooltje, Oma Wolletje's huisje,
              the moestuin of Tuinder Knolletje, the vissershut, the havenkantoor, the harbour with Kapitein Zoutsnoet's
              boat on the east side, Jutje Kwispel's juttershut at the strandpoort
      south-west: the wide beach where the player washes ashore

Everything Java needs comes out of `eiland(h)`: the Structure, the dict for eiland.json (the kern's shape) and the dict
for data/guhs/snuffeldorp/dorp.json (this slice's own spots: scene anchors, the line of the roadblock).
`python tools/features/snuffel_dorp_bouw.py <map>` writes a top-down map picture for a quick look.
"""
import math
import random

from . import snuffel_bouw as kern

ZEE, ZAND = kern.ZEE, kern.ZAND
SX, SY, SZ = 168, 62, 192
CX, CZ = 84, 98                           # the middle of the island's outline
OORSPRONG = (-CX, ZAND + 1, -CZ)          # the template stands on the sea floor; the island's middle is world 0, 0
G = ZEE + 1 - OORSPRONG[1]                # template y of the ground blocks at height 0 (their top is world y 64)
VERSIE = 4                                # (the kern's test island was 1; raise it when the island changes on a live server; 4: the tiles' order)
TEGEL = 48

# --- the plan ---------------------------------------------------------------------------------------------------------------
PLEIN = (84, 114)                         # the well stands here
BOOM = (97, 80)                           # the knoll with the ring of stones
BOOM_HOOG = 2
TRAINER = (71, 78)
WEIPOORT = (84, 96)
STRANDPOORT = (70, 134)
VERSPERRING_Z = 50                        # the roadblock stands across the road at this z
GRENS_Z = 47                              # north of this line (z < GRENS_Z) the island is closed until you are a Snuffelneus
HAVEN_X = 118                             # the quay's edge; the harbour basin lies east of it
STEIGER_Z = 114                           # the jetty's middle line
STEIGER_X1 = 139


def ruis(seed):
    """Smooth value noise 0..1 (deterministic)."""
    rng = random.Random(seed)
    tabel = [[rng.random() for _ in range(64)] for _ in range(64)]

    def f(x, z, schaal=8.0):
        x, z = x / schaal, z / schaal
        x0, z0 = math.floor(x), math.floor(z)
        fx, fz = x - x0, z - z0
        fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
        a, b = tabel[x0 % 64][z0 % 64], tabel[(x0 + 1) % 64][z0 % 64]
        c, d = tabel[x0 % 64][(z0 + 1) % 64], tabel[(x0 + 1) % 64][(z0 + 1) % 64]
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz
    return f


R1, R2, R3 = ruis(4101), ruis(4102), ruis(4103)


def kust(x, z):
    """How far inside the beach line this column is (blocks, about; negative = sea)."""
    dx, dz = x - CX, z - CZ
    rho = math.hypot(dx, dz)
    a = math.atan2(dz, dx)
    r = 1.0 / math.sqrt((math.cos(a) / 60.0) ** 2 + (math.sin(a) / 76.0) ** 2)
    r *= 1 + 0.035 * math.sin(3 * a + 0.6) + 0.025 * math.sin(5 * a + 2.0) + 0.015 * math.sin(9 * a + 1.0)
    d = r - rho
    # the harbour basin: a rounded box cut out of the east coast
    bx, bz = max(HAVEN_X - x, 0), max(abs(z - STEIGER_Z) - 9, 0)
    return min(d, math.hypot(bx, bz) - 0.01 if (bx or bz) else -3.0)


def in_haven(x, z):
    return x >= HAVEN_X and abs(z - STEIGER_Z) <= 9


def strandbreedte(x, z):
    """How wide the sand is here: narrow around the island, wide in the south-west bay where you wash ashore."""
    a = math.degrees(math.atan2(z - CZ, x - CX))
    return 4.5 + 17 * math.exp(-((a - 116) / 30.0) ** 2) + 1.5 * R3(x, z, 5.0)


def rug_z(x):
    """The ridge between the meadow and the closed part of the island."""
    return 50 + 2.4 * math.sin(x * 0.075 + 0.4)


def hoogte(x, z):
    """Ground height above G for a land column (0 in the whole start zone, hills in the closed part)."""
    d = kust(x, z)
    if d < 0:
        return 0
    if in_haven(x, z):
        return 0
    zr = rug_z(x)
    weg = abs(x - CX) <= 2.6                                   # the road through the ridge
    if zr - 3.2 <= z <= zr + 0.6:                              # the ridge itself: a wall of rock, 4 to 6 high
        if weg:
            return 0
        return 4 + int(2.2 * R1(x, z, 5.0))
    if z < zr - 3.2:                                           # the closed part: hills
        if weg and z > zr - 9:
            return 0
        land = min(1.0, max(0.0, (d - 3) / 12.0))
        top = math.exp(-((x - 58) ** 2 + (z - 27) ** 2) / (2 * 9.0 ** 2)) * 7
        h = (2.5 + 7.5 * R1(x, z, 13.0) + 3 * R2(x, z, 6.0) + top) * land
        kaap = math.exp(-((x - 116) ** 2 + (z - 36) ** 2) / (2 * 7.0 ** 2))
        h = h * (1 - kaap) + 3 * kaap * land
        pad = math.exp(-((x - (CX - (zr - z) * 0.35)) ** 2) / (2 * 2.2 ** 2)) if z > 26 else 0
        h = h * (1 - 0.8 * pad)
        return max(0, int(round(h)))
    # the knoll of the tree
    kd = math.hypot(x - BOOM[0], z - BOOM[1])
    if kd <= 3.6:
        return BOOM_HOOG
    if kd <= 6.4:
        return 1
    return 0


class Bouw:
    """The island under construction: the Structure, the surface height per column, where nothing may grow."""

    def __init__(self, h):
        self.h = h
        self.ms = h.ms
        self.s = h.Structure((SX, SY, SZ))
        self.top = {}            # (x, z) -> template y of the top GROUND block of a land column
        self.zand = set()        # sand columns
        self.vrij = set()        # columns where no plant or tree may come
        self.laag = set()        # columns where only low plants may come (no tree, no bush)
        self.rng = random.Random(2130_7001)
        self.borden = []

    # --- blocks ---
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name if ":" in name else "minecraft:" + name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def leeg(self, x, y, z):
        self.s.blocks.pop((x, y, z), None)

    def vul(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def voet(self, x, z):
        """Feet level on this column (template y)."""
        return self.top.get((x, z), G) + 1

    def bezet(self, x0, z0, x1, z1, rand=0):
        for x in range(x0 - rand, x1 + rand + 1):
            for z in range(z0 - rand, z1 + rand + 1):
                self.vrij.add((x, z))

    # --- small things ---
    def trap(self, x, y, z, facing, name, om=False, shape=None):
        p = {"facing": facing, "half": "top" if om else "bottom", "shape": shape or "straight", "waterlogged": "false"}
        self.set(x, y, z, name, p)

    def plaat(self, x, y, z, name, boven=False):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def luik(self, x, y, z, facing, name="spruce_trapdoor", boven=False, open_=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if boven else "bottom", "open": "true" if open_ else "false",
                                 "powered": "false", "waterlogged": "false"})

    def lamp(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def hanglamp(self, x, y, z):
        """A lantern hanging at (x, y, z) on a chain that runs up to whatever is above it (a lantern needs something to hang from)."""
        self.lamp(x, y, z, hangend=True)
        for yy in range(y + 1, y + 9):
            if self.get(x, yy, z) is not None:
                break
            self.set(x, yy, z, "chain", {"axis": "y", "waterlogged": "false"})

    def lantaarnpaal(self, x, z, hoog=2, paal="spruce_fence"):
        y = self.voet(x, z)
        for i in range(hoog):
            self.set(x, y + i, z, paal)
        self.lamp(x, y + hoog, z)
        self.vrij.add((x, z))

    def bord(self, x, y, z, regels, rotation=None, facing=None, hout="spruce"):
        """A waxed sign: standing (rotation 0..15; 0 = its text faces south) or on a wall (facing = the side its text faces)."""
        import sign_text
        B = self.ms.Byte
        regels = (list(regels) + ["", "", "", ""])[:4]
        prefix = "sign.guhs.snuffel_dorp"
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
               "back_text": {"messages": sign_text.messages(prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
        if facing:
            self.set(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, f"{hout}_sign", {"rotation": str(rotation or 0), "waterlogged": "false"}, nbt)
        self.borden.append(regels)
        self.vrij.add((x, z))

    def pot(self, x, y, z, bloem="poppy"):
        self.set(x, y, z, "potted_" + bloem)

    def tapijt(self, x, y, z, kleur):
        self.set(x, y, z, kleur + "_carpet")

    def bed(self, x, y, z, facing, kleur="red"):
        """The foot at (x, y, z), the head one block further in `facing`."""
        dx, dz = RICHTING[facing]
        self.set(x, y, z, kleur + "_bed", {"facing": facing, "part": "foot", "occupied": "false"})
        self.set(x + dx, y, z + dz, kleur + "_bed", {"facing": facing, "part": "head", "occupied": "false"})

    def vat(self, x, y, z, facing="up"):
        self.set(x, y, z, "barrel", {"facing": facing, "open": "false"})

    def tafel(self, x, y, z, hout="spruce", op=None):
        self.set(x, y, z, hout + "_fence")
        self.set(x, y + 1, z, hout + "_pressure_plate", {"powered": "false"})
        if op:
            self.leeg(x, y + 1, z)
            self.set(x, y + 1, z, op)

    def blad(self, x, y, z, soort="oak"):
        if self.get(x, y, z) is None and self.s.inside(x, y, z):
            self.set(x, y, z, soort + "_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})

    def boom(self, x, z, hoog=4, soort="oak", breed=2):
        y = self.voet(x, z)
        self.set(x, y - 1, z, "dirt")
        for i in range(hoog):
            self.set(x, y + i, z, soort + "_log", {"axis": "y"})
        ty = y + hoog - 1
        for dy in range(-1, 3):
            r = breed + (0.6 if dy <= 0 else -0.5 if dy == 1 else -1.2)
            for dx in range(-breed - 1, breed + 2):
                for dz in range(-breed - 1, breed + 2):
                    if math.hypot(dx, dz) <= r + 0.25 * self.rng.random() and not (dx == 0 and dz == 0 and dy < 1):
                        self.blad(x + dx, ty + dy, z + dz, soort)
        self.bezet(x - 1, z - 1, x + 1, z + 1)

    def spar(self, x, z, hoog=7):
        y = self.voet(x, z)
        self.set(x, y - 1, z, "dirt")
        for i in range(hoog):
            self.set(x, y + i, z, "spruce_log", {"axis": "y"})
        for i, r in enumerate((2, 1, 2, 1, 1, 0)):
            ly = y + hoog - 5 + i
            if ly <= y:
                continue
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) <= r + (1 if r == 2 else 0) and (dx or dz or ly >= y + hoog):
                        self.blad(x + dx, ly, z + dz, "spruce")
        self.blad(x, y + hoog, z, "spruce")
        self.bezet(x - 1, z - 1, x + 1, z + 1)

    def struik(self, x, z, soort="azalea"):
        y = self.voet(x, z)
        if self.get(x, y, z) is None:
            self.set(x, y, z, "flowering_azalea_leaves" if soort == "bloei" else soort + "_leaves",
                     {"persistent": "true", "distance": "1", "waterlogged": "false"})
        self.vrij.add((x, z))


STEEL = {k: "true" for k in ("down", "east", "north", "south", "up", "west")}       # a mushroom stem with skin on every side
RICHTING = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
TEGEN = {"north": "south", "south": "north", "west": "east", "east": "west"}
LINKS = {"north": "west", "west": "south", "south": "east", "east": "north"}       # a quarter turn to the left
STEEN_MIX = ["stone", "stone", "andesite", "cobblestone", "mossy_cobblestone", "stone", "andesite"]


# =====================================================================================================================
# the ground
# =====================================================================================================================
def grond(b):
    rng = random.Random(2130_7002)
    for x in range(SX):
        for z in range(SZ):
            d = kust(x, z)
            if d < 0:
                haven = in_haven(x, z)
                zr = rug_z(x)
                # the ridge runs on into the sea as a row of rocks (nobody walks around its ends)
                klip = G + 3 + int(d * 0.8) if (zr - 3.2 <= z <= zr + 0.6 and d >= -7 and not haven) else -1
                for y in range(G, klip + 1):
                    b.set(x, y, z, rng.choice(STEEN_MIX))
                for y in range(0, G):
                    diep = G - y
                    if haven:
                        # the basin: deep enough for a boat right up to the quay, a floor of sand and gravel
                        if diep >= 5:
                            b.set(x, y, z, "gravel" if diep == 5 and rng.random() < 0.3 else "sand" if diep <= 7 else "stone")
                        continue
                    rand = min(diep, 4) * 1.4 + max(0, diep - 4) * 0.55
                    if y <= klip:
                        b.set(x, y, z, rng.choice(STEEN_MIX))
                    elif -d <= rand:
                        b.set(x, y, z, "sand" if (-d > rand - 2.6 or diep <= 3) else "stone")
                continue
            hgt = hoogte(x, z)
            top = G + hgt
            zr = rug_z(x)
            rots = zr - 3.2 <= z <= zr + 0.6 and hgt >= 2
            steil = z < zr - 3.2 and hgt >= 9
            zand = d < strandbreedte(x, z) and hgt == 0 and not (z < zr + 2 and d > 3.5)
            for y in range(0, top + 1):
                diep = top - y
                if rots or steil:
                    blok = rng.choice(STEEN_MIX) if diep <= 5 else "stone"
                    if diep == 0 and not steil and rng.random() < 0.55:
                        blok = "grass_block" if rng.random() < 0.7 else "moss_block"
                    if steil and diep == 0 and rng.random() < 0.4:
                        blok = "grass_block"
                elif zand:
                    blok = "sand" if diep <= 3 else "sandstone" if diep <= 5 else "stone"
                elif diep == 0:
                    blok = "grass_block"
                elif diep <= 3:
                    blok = "dirt"
                else:
                    blok = "stone"
                b.set(x, y, z, blok)
            b.top[(x, z)] = top
            if zand:
                b.zand.add((x, z))
    # the quay wall: stone bricks down the basin's landward sides
    for x in range(SX):
        for z in range(SZ):
            if (x, z) in b.top and any(in_haven(x + dx, z + dz) and (x + dx, z + dz) not in b.top for dx, dz in ((1, 0), (0, 1), (0, -1))):
                for y in range(G - 5, G + 1):
                    b.set(x, y, z, "stone_bricks" if (x + y + z) % 5 else "mossy_stone_bricks")


def pad(b, punten, breed=1.2, blok="dirt_path", kans=1.0):
    """A path along a polyline (on land only): the top ground block becomes `blok`; the columns stay free of plants."""
    rng = random.Random(hash(tuple(punten)) & 0xFFFF)
    for (x0, z0), (x1, z1) in zip(punten, punten[1:]):
        n = int(max(abs(x1 - x0), abs(z1 - z0)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            px, pz = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
            r = int(math.ceil(breed)) + 1
            for x in range(int(px) - r, int(px) + r + 1):
                for z in range(int(pz) - r, int(pz) + r + 1):
                    if (x, z) in b.top and math.hypot(x + 0.5 - px - 0.5, z + 0.5 - pz - 0.5) <= breed:
                        b.vrij.add((x, z))
                        y = b.top[(x, z)]
                        huidig = b.get(x, y, z)
                        if huidig in ("minecraft:grass_block", "minecraft:dirt", "minecraft:sand", "minecraft:moss_block") and b.get(x, y + 1, z) is None:
                            if rng.random() <= kans:
                                if (x, z) in b.zand and blok == "dirt_path":
                                    b.set(x, y, z, "spruce_planks" if (x + z) % 3 else "stripped_spruce_log", {} if (x + z) % 3 else {"axis": "x"})
                                else:
                                    b.set(x, y, z, blok)


def trapje(b, x, z, facing, name="cobblestone_stairs"):
    """A stair block ON the ground of this column (one step up towards `facing`)."""
    y = b.voet(x, z)
    b.trap(x, y, z, facing, name)
    b.vrij.add((x, z))


# =====================================================================================================================
# houses
# =====================================================================================================================
class Stijl:
    def __init__(self, muur, balk, dak, vloer="spruce_planks", deur="spruce", plint="cobblestone", raam="glass_pane", muur_props=None):
        self.muur, self.balk, self.dak, self.vloer, self.deur, self.plint, self.raam = muur, balk, dak, vloer, deur, plint, raam
        self.muur_props = muur_props


def huis(b, x0, z0, w, d, deur, stijl, hoog=3, nok=None, schoorsteen=None, ramen=True, naam=None, lamp=True):
    """A cottage with its min corner at (x0, z0), w wide (x) and d deep (z); deur = (side, offset along that wall). The
    ridge runs along `nok` ("x" or "z"; default: the longer side). Returns helper numbers for the inside."""
    s = stijl
    x1, z1 = x0 + w - 1, z0 + d - 1
    nok = nok or ("x" if w >= d else "z")
    b.bezet(x0, z0, x1, z1, rand=1)
    # the floor and the plinth (they replace the ground), a clean inside
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, s.plint if rand else s.vloer)
            for y in range(G + 1, G + hoog + 8):
                b.leeg(x, y, z)
    # walls
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)):
                continue
            hoek = x in (x0, x1) and z in (z0, z1)
            for y in range(G + 1, G + hoog + 1):
                if hoek:
                    b.set(x, y, z, s.balk, {"axis": "y"})
                else:
                    b.set(x, y, z, s.muur, s.muur_props)
    # windows: every third block of a wall, at the second row
    if ramen:
        for x in range(x0 + 2, x1 - 1):
            if (x - x0) % 3 == 2 or w <= 6:
                for z in (z0, z1):
                    b.set(x, G + 2, z, s.raam)
        for z in range(z0 + 2, z1 - 1):
            if (z - z0) % 3 == 2 or d <= 6:
                for x in (x0, x1):
                    b.set(x, G + 2, z, s.raam)
    # the door
    kant, off = deur
    if kant in ("north", "south"):
        dx_, dz_ = x0 + off, z0 if kant == "north" else z1
    else:
        dx_, dz_ = x0 if kant == "west" else x1, z0 + off
    for i, half in enumerate(("lower", "upper")):
        b.set(dx_, G + 1 + i, dz_, s.deur + "_door", {"facing": TEGEN[kant], "half": half, "hinge": "left", "open": "false", "powered": "false"})
    if hoog >= 3:
        b.set(dx_, G + 3, dz_, s.muur, s.muur_props)
    ox, oz = RICHTING[kant]
    buiten = (dx_ + ox, dz_ + oz)
    b.vrij.add(buiten)
    b.vrij.add((dx_ + 2 * ox, dz_ + 2 * oz))
    for k in (1, 2):
        px, pz = dx_ + k * ox, dz_ + k * oz
        if (px, pz) in b.top and b.get(px, b.top[(px, pz)], pz) in ("minecraft:grass_block", "minecraft:sand"):
            b.set(px, b.top[(px, pz)], pz, "dirt_path" if (px, pz) not in b.zand else "spruce_planks")
    # the roof
    dakt, dakp, dakv = s.dak
    if nok == "x":
        span = d + 2
        for i in range((span + 1) // 2):
            y = G + hoog + i
            zn, zs = z0 - 1 + i, z1 + 1 - i
            for x in range(x0 - 1, x1 + 2):
                if zn == zs:
                    b.plaat(x, y, zn, dakp)
                else:
                    b.trap(x, y, zn, "south", dakt)
                    b.trap(x, y, zs, "north", dakt)
            # the gable ends
            for z in range(zn + 1, zs):
                if i >= 1 or True:
                    for x in (x0, x1):
                        if y > G + hoog - 1 and zn < z < zs and b.get(x, y, z) is None:
                            b.set(x, y, z, s.muur if i < 2 else dakv, s.muur_props if i < 2 else None)
        top = G + hoog + (span + 1) // 2 - 1
    else:
        span = w + 2
        for i in range((span + 1) // 2):
            y = G + hoog + i
            xw, xe = x0 - 1 + i, x1 + 1 - i
            for z in range(z0 - 1, z1 + 2):
                if xw == xe:
                    b.plaat(xw, y, z, dakp)
                else:
                    b.trap(xw, y, z, "east", dakt)
                    b.trap(xe, y, z, "west", dakt)
            for x in range(xw + 1, xe):
                for z in (z0, z1):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, s.muur if i < 2 else dakv, s.muur_props if i < 2 else None)
        top = G + hoog + (span + 1) // 2 - 1
    # a lantern by the door, on the side where the name sign is not: hanging under the eave when the door is under one,
    # else on a post
    if lamp:
        onder_dakrand = (nok == "x" and kant in ("north", "south")) or (nok == "z" and kant in ("west", "east"))
        zx, zz = RICHTING[LINKS[kant]]
        lx, lz = dx_ + ox - zx, dz_ + oz - zz
        if onder_dakrand:
            b.lamp(lx, G + hoog - 1, lz, hangend=True)
        else:
            b.set(lx, G + 1, lz, "spruce_fence")
            b.lamp(lx, G + 2, lz)
        b.vrij.add((lx, lz))
    # the chimney: bricks from the floor through the roof, a campfire on top for the smoke
    if schoorsteen:
        cx_, cz_ = schoorsteen
        for y in range(G + 1, top + 2):
            b.set(cx_, y, cz_, "bricks")
        b.set(cx_, top + 2, cz_, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    # the name on a sign next to the door
    if naam:
        zij = LINKS[kant]
        sx_, sz_ = dx_ + RICHTING[zij][0] + ox, dz_ + RICHTING[zij][1] + oz
        b.bord(sx_, G + 2, sz_, naam, facing=kant)
    return dict(x0=x0, z0=z0, x1=x1, z1=z1, y=G + 1, top=top, deur=(dx_, dz_), buiten=buiten)


def bloembak(b, x, z, bloemen=("poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "pink_tulip", "orange_tulip")):
    if (x, z) in b.top and b.get(x, b.top[(x, z)], z) == "minecraft:grass_block" and b.get(x, b.top[(x, z)] + 1, z) is None:
        b.set(x, b.top[(x, z)] + 1, z, b.rng.choice(bloemen))
        b.vrij.add((x, z))


def tuintje(b, h_, kanten=("north", "south", "west", "east")):
    """Flowers along the outside of a house's walls (not in front of the door)."""
    for x in range(h_["x0"] + 1, h_["x1"]):
        for kant, z in (("north", h_["z0"] - 1), ("south", h_["z1"] + 1)):
            if kant in kanten and (x, z) not in b.vrij_deur and b.rng.random() < 0.75:
                bloembak(b, x, z)
    for z in range(h_["z0"] + 1, h_["z1"]):
        for kant, x in (("west", h_["x0"] - 1), ("east", h_["x1"] + 1)):
            if kant in kanten and (x, z) not in b.vrij_deur and b.rng.random() < 0.75:
                bloembak(b, x, z)


# =====================================================================================================================
# Snuffeldorp
# =====================================================================================================================
def plein(b):
    px, pz = PLEIN
    rng = random.Random(2130_7003)
    for x in range(px - 9, px + 10):
        for z in range(pz - 9, pz + 10):
            r = math.hypot(x - px, z - pz)
            if r <= 8.4 and (x, z) in b.top:
                y = b.top[(x, z)]
                b.set(x, y, z, rng.choice(["stone_bricks", "stone_bricks", "cobblestone", "andesite", "mossy_stone_bricks", "stone_bricks"])
                      if r > 2.6 else "polished_andesite")
                b.vrij.add((x, z))
    # the well: a ring of stone with water, four posts and a little roof
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                b.set(px + dx, G + 1, pz + dz, "mossy_stone_bricks" if (dx + dz) % 2 else "stone_bricks")
                b.set(px + dx, G, pz + dz, "stone_bricks")
    b.set(px, G, pz, "water", {"level": "0"})
    b.set(px, G - 1, pz, "water", {"level": "0"})
    b.set(px, G - 2, pz, "stone_bricks")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        b.set(px + dx, G + 2, pz + dz, "spruce_fence")
        b.set(px + dx, G + 3, pz + dz, "spruce_fence")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.plaat(px + dx, G + 4, pz + dz, "spruce_slab")
    b.set(px, G + 4, pz, "spruce_planks")
    b.set(px, G + 3, pz, "chain", {"axis": "y", "waterlogged": "false"})
    b.plaat(px, G + 5, pz, "spruce_slab")
    # benches (stairs), lantern posts, flower tubs
    for dx, facing in ((-6, "east"), (6, "west")):
        for dz in (-4, -3):
            b.trap(px + dx, G + 1, pz + dz, facing, "spruce_stairs")
    for dx, dz in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
        b.lantaarnpaal(px + dx, pz + dz, 3)
    for dx, dz in ((-4, 6), (4, 6), (-7, 1), (7, -1)):
        b.set(px + dx, G + 1, pz + dz, "flowering_azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
    # the notice board on the north side
    b.set(px - 3, G + 1, pz - 7, "spruce_fence")
    b.set(px - 3, G + 2, pz - 7, "spruce_planks")
    b.bord(px - 3, G + 2, pz - 6, ["Snuffeldorp", "Welkom, njeg!", "Pootjes vegen", ""], facing="south")
    # the baker's stall on the west side of the plein
    sx_, sz_ = px - 7, pz - 1
    for dz in (0, 1, 2):
        b.set(sx_, G + 1, sz_ + dz, "barrel", {"facing": "up", "open": "false"} if dz != 1 else {"facing": "east", "open": "false"})
        b.plaat(sx_, G + 4, sz_ + dz, "white_wool" if False else "spruce_slab")
        b.set(sx_ + 1, G + 4, sz_ + dz, "red_carpet" if dz % 2 == 0 else "white_carpet")
        b.plaat(sx_ + 1, G + 3, sz_ + dz, "spruce_slab", boven=True)
        b.plaat(sx_, G + 3, sz_ + dz, "spruce_slab", boven=True)
        b.leeg(sx_, G + 4, sz_ + dz)
        b.set(sx_, G + 4, sz_ + dz, "red_carpet" if dz % 2 == 0 else "white_carpet")
    for dz in (0, 2):
        b.set(sx_ + 1, G + 1, sz_ + dz, "spruce_fence")
        b.set(sx_ + 1, G + 2, sz_ + dz, "spruce_fence")
        b.set(sx_, G + 2, sz_ + dz, "spruce_fence")
    b.set(sx_, G + 2, sz_ + 1, "cake", {"bites": "0"})
    b.set(sx_ + 1, G + 1, sz_ + 1, "hay_block", {"axis": "x"})


INTERIEUR_BLOEMEN = ("poppy", "dandelion", "blue_orchid", "allium", "azure_bluet", "red_tulip", "oxeye_daisy", "cornflower")


def dokter(b):
    st = Stijl("mushroom_stem", "stripped_spruce_log", ("brick_stairs", "brick_slab", "bricks"), vloer="birch_planks", deur="birch", muur_props=STEEL)
    hh = huis(b, 65, 99, 10, 8, ("south", 5), st, schoorsteen=(66, 100), naam=["Dokter", "Pleisterpoot", "", "Au? Kom binnen"])
    y = hh["y"]
    # the red cross, lying on the roof's south slope above the door (seen from the plein)
    for (x, dy, z) in ((70, 3, 107), (69, 4, 106), (70, 4, 106), (71, 4, 106), (70, 5, 105)):
        b.set(x, G + dy, z, "red_wool")
    # inside: the examination table (a white bed), a cupboard of bottles, a basket, the doctor's desk
    b.bed(72, y, 101, "north", "white")
    b.set(73, y, 100, "bookshelf")
    b.set(73, y + 1, 100, "bookshelf")
    b.set(73, y, 101, "brewing_stand")
    b.set(67, y, 100, "cauldron")
    b.tafel(68, y, 104, "birch")
    b.pot(69, y, 104, "blue_orchid") if False else None
    b.set(66, y, 104, "barrel", {"facing": "up", "open": "false"})
    b.pot(66, y + 1, 104, "red_tulip")
    for x in range(68, 72):
        for z in range(101, 104):
            b.tapijt(x, y, z, "white" if (x + z) % 2 else "red")
    b.hanglamp(70, G + 4, 102)
    return hh


def bakker(b):
    st = Stijl("smooth_sandstone", "oak_log", ("spruce_stairs", "spruce_slab", "spruce_planks"), vloer="oak_planks", deur="oak")
    hh = huis(b, 61, 110, 9, 8, ("east", 4), st, schoorsteen=(62, 111), naam=["Bakkerij", "Kruimelsnuit", "", "Vers brood!"])
    y = hh["y"]
    # the oven: a wall of bricks with two furnaces and a smoker, sacks of flour, the counter
    for z in (112, 113):
        b.set(62, y, z, "furnace", {"facing": "east", "lit": "true"})
        b.set(62, y + 1, z, "bricks")
    b.set(62, y, 114, "smoker", {"facing": "east", "lit": "false"})
    b.set(62, y, 115, "hay_block", {"axis": "y"})
    b.set(62, y + 1, 115, "hay_block", {"axis": "y"})
    b.set(62, y, 116, "barrel", {"facing": "up", "open": "false"})
    for z in (112, 113):
        b.set(66, y, z, "spruce_trapdoor", {"facing": "west", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
    b.set(66, y, 115, "barrel", {"facing": "up", "open": "false"})
    b.set(66, y + 1, 115, "cake", {"bites": "0"})
    b.set(64, y, 116, "crafting_table")
    b.pot(64, y + 1, 116, "dandelion")
    b.tapijt(64, y, 113, "orange")
    b.tapijt(65, y, 113, "orange")
    b.hanglamp(65, G + 4, 113)
    return hh


def school(b):
    st = Stijl("bricks", "stripped_oak_log", ("dark_oak_stairs", "dark_oak_slab", "dark_oak_planks"), vloer="oak_planks", deur="dark_oak")
    hh = huis(b, 93, 99, 10, 8, ("south", 4), st, naam=["Het schooltje", "van Juf", "Blaffetje", ""])
    y = hh["y"]
    # the blackboard, three little desks (stairs to sit on, a trapdoor as desk), a bookcase
    for x in (96, 97, 98):
        b.set(x, y + 1, 100, "black_concrete")
    b.bord(97, y + 1, 101, ["1 bot + 1 bot", "= 2 botten", "", "njeg = njeg"], facing="south", hout="birch")
    for x in (95, 97, 99):
        b.trap(x, y, 104, "south", "oak_stairs")
        b.set(x, y, 103, "oak_trapdoor", {"facing": "south", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
    b.set(101, y, 100, "bookshelf")
    b.set(101, y + 1, 100, "bookshelf")
    b.set(101, y, 101, "lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    b.tapijt(94, y, 100, "yellow")
    b.tapijt(94, y, 101, "lime")
    b.pot(94, y, 104, "oxeye_daisy") if False else None
    b.hanglamp(98, G + 4, 102)
    # the schoolyard: a fence, the bell post WITHOUT its bell (the juf lost it), a hopscotch of carpets
    for x in range(93, 105):
        for z in range(107, 111):
            if (x, z) in b.top:
                b.vrij.add((x, z))
    for x in range(94, 105):
        if x not in (97, 98):
            b.set(x, G + 1, 110, "oak_fence")
    for z in range(107, 111):
        b.set(104, G + 1, z, "oak_fence")
    for i, yy in enumerate((G + 1, G + 2, G + 3)):
        b.set(102, yy, 108, "stripped_oak_log", {"axis": "y"})
    b.set(101, G + 3, 108, "oak_fence")
    b.set(101, G + 2, 108, "chain", {"axis": "y", "waterlogged": "false"})
    b.bord(102, G + 2, 109, ["Schoolbel", "ZOEK!", "Wie hem vindt", "krijgt een aai"], facing="south", hout="oak")
    for i, kleur in enumerate(("red", "yellow", "lime", "light_blue")):
        b.tapijt(95 + i, G + 1, 108, kleur)
    return hh


def oma(b):
    st = Stijl("birch_planks", "stripped_birch_log", ("mud_brick_stairs", "mud_brick_slab", "mud_bricks"), vloer="spruce_planks", deur="birch")
    hh = huis(b, 63, 122, 8, 7, ("east", 3), st, schoorsteen=(64, 123), naam=["Oma Wolletje", "breit voor", "het hele dorp", ""])
    y = hh["y"]
    # wool everywhere: baskets of wool, a loom, her bed with a lilac blanket, a rug
    for (x, z, kleur) in ((64, 127, "purple"), (65, 127, "magenta"), (64, 126, "pink"), (68, 123, "light_blue"), (69, 123, "white")):
        b.set(x, y, z, kleur + "_wool")
    b.set(65, y + 1, 127, "lime_wool")
    b.set(66, y, 123, "loom", {"facing": "south"})
    b.bed(68, y, 127, "east", "purple") if False else b.bed(68, y, 127, "west", "purple")
    for x in (66, 67):
        for z in (124, 125):
            b.tapijt(x, y, z, "purple" if (x + z) % 2 else "pink")
    b.pot(69, y, 127, "allium") if False else None
    b.hanglamp(67, G + 4, 125)
    # the porch on the east side: a little deck with her bench and baskets
    for z in range(123, 128):
        for x in (71, 72):
            if (x, z) in b.top:
                b.set(x, G, z, "spruce_planks")
                b.vrij.add((x, z))
    b.trap(72, G + 1, 127, "north", "birch_stairs")
    b.set(71, G + 1, 127, "purple_wool")
    b.set(72, G + 1, 123, "white_wool")
    b.pot(72, G + 2, 123, "allium")
    return hh


def tuinder(b):
    st = Stijl("oak_planks", "oak_log", ("oxidized_cut_copper_stairs", "oxidized_cut_copper_slab", "oxidized_cut_copper"), vloer="oak_planks", deur="oak")
    hh = huis(b, 93, 123, 7, 7, ("west", 3), st, naam=["Tuinder", "Knolletje", "Verse knollen", ""])
    y = hh["y"]
    b.bed(97, y, 128, "east", "green") if False else b.bed(97, y, 127, "north", "green")
    b.set(94, y, 124, "composter", {"level": "3"})
    b.set(95, y, 124, "barrel", {"facing": "up", "open": "false"})
    b.pot(95, y + 1, 124, "fern")
    b.tafel(94, y, 128, "oak")
    b.tapijt(95, y, 126, "green")
    b.tapijt(96, y, 126, "lime")
    b.hanglamp(96, G + 4, 126)
    # the moestuin south-east of the house: beds of carrots, potatoes, beetroots and wheat, a water ditch, a scarecrow, the coop
    gx0, gx1, gz0, gz1 = 102, 110, 129, 138
    for x in range(gx0 - 1, gx1 + 2):
        for z in range(gz0 - 1, gz1 + 2):
            if (x, z) not in b.top:
                continue
            b.vrij.add((x, z))
            rand = x in (gx0 - 1, gx1 + 1) or z in (gz0 - 1, gz1 + 1)
            if rand:
                if not (x == gx0 - 1 and z in (133, 134)):
                    b.set(x, G + 1, z, "spruce_fence")
            elif x == 106:
                b.set(x, G, z, "water", {"level": "0"}) if z not in (133, 134) else b.set(x, G, z, "spruce_planks")
            elif z in (133, 134):
                b.set(x, G, z, "dirt_path")
            else:
                gewas = (("carrots", "7"), ("potatoes", "7"), ("beetroots", "3"), ("wheat", "7"))[(0 if x < 106 else 2) + (0 if z < 133 else 1)]
                b.set(x, G, z, "farmland", {"moisture": "7"})
                b.set(x, G + 1, z, gewas[0], {"age": gewas[1]})
    for dz in (0, 1):
        for i in range(2):
            b.set(gx0 - 1, G + 2 + i, 132 + dz * 3, "spruce_fence")
    b.plaat(gx0 - 1, G + 4, 133, "spruce_slab")
    b.plaat(gx0 - 1, G + 4, 134, "spruce_slab")
    b.plaat(gx0 - 1, G + 4, 132, "spruce_slab")
    b.plaat(gx0 - 1, G + 4, 135, "spruce_slab")
    # the scarecrow (with a carved pumpkin head; it only scares crows, njeg)
    b.set(104, G + 1, 131, "oak_fence")
    b.set(104, G + 2, 131, "hay_block", {"axis": "y"})
    b.set(104, G + 3, 131, "carved_pumpkin", {"facing": "west"})
    # the chicken coop in the south-east corner of the garden (the exam's animal scent hangs here)
    for x in (108, 109, 110):
        for z in (136, 137, 138):
            b.set(x, G, z, "coarse_dirt")
            b.leeg(x, G + 1, z)
    b.set(109, G + 1, 137, "hay_block", {"axis": "x"})
    b.set(110, G + 1, 137, "spruce_planks")
    b.set(110, G + 1, 138, "spruce_planks")
    b.set(109, G + 1, 138, "spruce_planks")
    for (x, z) in ((109, 137), (110, 137), (109, 138), (110, 138)):
        b.plaat(x, G + 2, z, "spruce_slab")
    b.set(108, G + 1, 138, "white_wool")
    b.bord(108, G + 1, 136, ["Kippenhok", "Niet blaffen", "a.u.b.", ""], rotation=12, hout="oak")
    return hh


def visser(b):
    st = Stijl("spruce_planks", "stripped_dark_oak_log", ("prismarine_brick_stairs", "prismarine_brick_slab", "prismarine_bricks"), vloer="spruce_planks", deur="spruce")
    hh = huis(b, 107, 103, 7, 6, ("south", 3), st, naam=["Visser Natneus", "Verse vis", "(als hij bijt)", ""])
    y = hh["y"]
    b.bed(112, y, 104, "west", "light_blue")
    b.vat(108, y, 104)
    b.vat(108, y + 1, 104, "south")
    b.vat(108, y, 105)
    b.tafel(108, y, 107, "spruce")
    b.tapijt(110, y, 106, "cyan")
    b.hanglamp(110, G + 4, 105)
    # outside: a drying rack for nets, barrels of fish
    for x in (114, 115, 116):
        b.set(x, G + 1, 106, "spruce_fence")
    b.set(114, G + 2, 106, "spruce_fence")
    b.set(116, G + 2, 106, "spruce_fence")
    b.set(115, G + 2, 106, "spruce_fence")
    b.vat(114, G + 1, 108)
    b.vat(115, G + 1, 108, "south")
    b.bezet(114, 105, 116, 108)
    return hh


def havenkantoor(b):
    st = Stijl("mushroom_stem", "stripped_dark_oak_log", ("dark_prismarine_stairs", "dark_prismarine_slab", "dark_prismarine"), vloer="dark_oak_planks", deur="dark_oak", muur_props=STEEL)
    hh = huis(b, 106, 120, 7, 6, ("north", 3), st, naam=["Havenkantoor", "Kapt. Zoutsnoet", "Vaart als het", "niet stormt"])
    y = hh["y"]
    b.set(107, y, 124, "cartography_table")
    b.set(108, y, 124, "bookshelf")
    b.set(108, y + 1, 124, "bookshelf")
    b.bed(111, y, 124, "north", "blue")
    b.tafel(107, y, 121, "dark_oak")
    b.tapijt(109, y, 122, "blue")
    b.tapijt(109, y, 123, "white")
    b.hanglamp(109, G + 4, 122)
    # a flagpole with a blue-white flag on the quay side
    for i in range(1, 7):
        b.set(113, G + i, 119, "spruce_fence")
    for i, kleur in enumerate(("blue_wool", "white_wool")):
        b.set(114, G + 6 - i, 119, kleur)
        b.set(115, G + 6 - i, 119, kleur)
    b.bezet(113, 119, 115, 119)
    return hh


def juttershut(b):
    """Jutje Kwispel's hut at the strandpoort: driftwood walls, a lean-to roof, everything she found on the beach around it."""
    rng = random.Random(2130_7004)
    x0, z0, w, d = 55, 130, 6, 6
    x1, z1 = x0 + w - 1, z0 + d - 1
    b.bezet(x0, z0, x1, z1, rand=1)
    hout = ["stripped_oak_log", "stripped_spruce_log", "spruce_planks", "stripped_birch_log", "oak_planks", "stripped_spruce_log"]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, "spruce_planks")
            for y in range(G + 1, G + 9):
                b.leeg(x, y, z)
            if rand:
                hoogte_ = 3 if x >= x0 + 3 else 2
                for y in range(G + 1, G + 1 + hoogte_):
                    naam = rng.choice(hout)
                    b.set(x, y, z, naam, {"axis": "y"} if naam.endswith("_log") else None)
    # a door on the east side (to the path), a window to the sea
    for i, half in enumerate(("lower", "upper")):
        b.set(x1, G + 1 + i, z0 + 3, "spruce_door", {"facing": "west", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    b.set(x0, G + 2, z0 + 2, "glass_pane")
    b.set(x0 + 2, G + 2, z1, "glass_pane")
    # the lean-to roof: low on the west (sea) side, high on the east side
    for z in range(z0 - 1, z1 + 2):
        b.plaat(x0 - 1, G + 2, z, "spruce_slab", boven=True)
        b.plaat(x0, G + 3, z, "spruce_slab")
        b.plaat(x0 + 1, G + 3, z, "spruce_slab")
        b.plaat(x0 + 2, G + 3, z, "spruce_slab", boven=True)
        b.plaat(x0 + 3, G + 4, z, "spruce_slab")
        b.plaat(x0 + 4, G + 4, z, "spruce_slab")
        b.plaat(x0 + 5, G + 4, z, "spruce_slab")
        b.plaat(x0 + 6, G + 4, z, "spruce_slab")
    # inside: her bed, a barrel of shells, a rug
    y = G + 1
    b.bed(x0 + 1, y, z0 + 1, "east", "red")
    b.vat(x0 + 1, y, z1 - 1)
    b.pot(x0 + 1, y + 1, z1 - 1, "dead_bush")
    b.tapijt(x0 + 3, y, z0 + 2, "red")
    b.tapijt(x0 + 3, y, z0 + 3, "white")
    b.lamp(x0 + 3, G + 3, z0 + 3, hangend=True)
    # outside: what the sea gave her
    b.bord(x1 + 1, G + 2, z0 + 2, ["Jutje Kwispel", "Strandjutter", "Gevonden =", "gehouden, njeg"], facing="east")
    for (x, z, wat, props) in ((x1 + 1, z1 + 1, "barrel", {"facing": "up", "open": "false"}), (x1 + 2, z1 + 1, "barrel", {"facing": "east", "open": "false"}),
                               (x0 - 2, z1, "stripped_spruce_log", {"axis": "z"}), (x0 - 2, z1 - 1, "stripped_spruce_log", {"axis": "z"}),
                               (x0 + 1, z1 + 2, "spruce_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})):
        if (x, z) in b.top:
            b.set(x, b.voet(x, z), z, wat, props)
            b.vrij.add((x, z))
    b.lamp(x1 + 1, G + 2, z0 + 4, hangend=False) if False else None
    b.set(x1 + 1, G + 1, z0 + 5, "spruce_fence")
    b.lamp(x1 + 1, G + 2, z0 + 5)
    b.vrij.add((x1 + 1, z0 + 3))
    return dict(deur=(x1, z0 + 3))


def strandpoort(b):
    """The arch of driftwood where the beach path enters the village."""
    x, z = STRANDPOORT
    for dx in (-2, 2):
        for i in range(3):
            b.set(x + dx, b.voet(x + dx, z) + i if i == 0 else G + 1 + i, z, "stripped_spruce_log", {"axis": "y"})
        b.vrij.add((x + dx, z))
    for dx in range(-2, 3):
        b.set(x + dx, G + 4, z, "stripped_spruce_log", {"axis": "x"})
    b.bord(x, G + 4, z + 1, ["~ Snuffeldorp ~", "Hier woont", "iedereen op", "vier poten"], facing="south")       # (on the beam: a wall sign needs a block behind it)
    b.lamp(x - 1, G + 3, z, hangend=True)
    b.lamp(x + 1, G + 3, z, hangend=True)
    b.set(x, G + 3, z, "stripped_spruce_log", {"axis": "x"}) if False else None


def heg(b):
    """The hedge between the village and the meadow, with the weipoort."""
    wx, wz = WEIPOORT
    for x in range(8, SX - 8):
        z = wz + (1 if (x // 9) % 2 else 0)
        if (x, z) not in b.top or (x, z) in b.zand or abs(x - wx) <= 2 or b.top[(x, z)] != G:
            continue
        if kust(x, z) < 7:
            continue
        b.set(x, G + 1, z, "oak_leaves" if (x * 7) % 5 else "flowering_azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
        b.set(x, G + 2, z, "oak_leaves" if (x * 3) % 7 else "azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
        b.vrij.add((x, z))
        b.vrij.add((x, z - 1))
        b.vrij.add((x, z + 1))
    # the gate: two posts with lanterns, a beam, a sign; two open fence gates
    for dx in (-2, 2):
        for i in range(3):
            b.set(wx + dx, G + 1 + i, wz, "oak_log", {"axis": "y"})
        b.lamp(wx + dx, G + 4, wz)
    for dx in range(-1, 2):
        b.plaat(wx + dx, G + 3, wz, "oak_slab", boven=True)
    b.bord(wx, G + 3, wz + 1, ["Naar de wei", "Snuffelschool", "Truffelneus", ""], facing="south", hout="oak")
    b.bord(wx, G + 3, wz - 1, ["Snuffeldorp", "", "", ""], facing="north", hout="oak")
    for dx in (-1, 1):
        b.set(wx + dx, G + 1, wz, "oak_fence_gate", {"facing": "east" if dx < 0 else "west", "open": "true", "in_wall": "false", "powered": "false"})


def haven(b):
    """The quay, the jetty with its posts and lanterns, and the captain's boat."""
    rng = random.Random(2130_7005)
    # the quay: paved from the plein's east road to the water
    for x in range(102, HAVEN_X):
        for z in range(109, 120):
            if (x, z) in b.top and b.top[(x, z)] == G:
                huidig = b.get(x, G, z)
                if huidig in ("minecraft:grass_block", "minecraft:sand", "minecraft:dirt_path", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks"):
                    b.set(x, G, z, rng.choice(["cobblestone", "stone_bricks", "cobblestone", "andesite", "mossy_cobblestone"]))
                b.vrij.add((x, z))
    # mooring posts and lanterns along the quay's edge
    for z in (107, 121):
        if (HAVEN_X - 1, z) in b.top:
            b.lantaarnpaal(HAVEN_X - 1, z, 2)
    for z in (110, 118):
        b.set(HAVEN_X - 1, G + 1, z, "stripped_dark_oak_log", {"axis": "y"})
    # crates and a coil of rope
    b.vat(103, G + 1, 110)
    b.vat(104, G + 1, 110, "east")
    b.vat(103, G + 2, 110, "south")
    b.set(104, G + 1, 118, "hay_block", {"axis": "z"})
    b.vat(103, G + 1, 118)
    # the jetty
    z0, z1 = STEIGER_Z - 1, STEIGER_Z + 1
    for x in range(HAVEN_X, STEIGER_X1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G, z, "spruce_planks" if (x % 4) else "stripped_spruce_log", {} if (x % 4) else {"axis": "z"})
            b.top[(x, z)] = G
            b.vrij.add((x, z))
        if (x - HAVEN_X) % 4 == 2:
            for z in (z0, z1):
                for y in range(G - 1, -1, -1):
                    if b.get(x, y, z) is not None:
                        break
                    b.set(x, y, z, "spruce_fence", {"waterlogged": "true"} if y <= G - 1 else {})
    for x in (HAVEN_X + 6, HAVEN_X + 14):
        b.set(x, G + 1, z0, "spruce_fence")
        b.lamp(x, G + 2, z0)
    for z in (z0, z1):
        b.set(STEIGER_X1, G + 1, z, "spruce_fence")
        b.lamp(STEIGER_X1, G + 2, z)
    b.bord(HAVEN_X + 1, G + 1, z0, ["Haven", "Boot naar huis:", "vraag de", "kapitein"], rotation=4)
    # the visser's spot: a barrel and a little crate at the jetty's north edge
    b.vat(HAVEN_X + 4, G + 1, z0)
    boot(b)


def boot(b):
    """Kapitein Zoutsnoet's boat, moored on the south side of the jetty, bow to the east: a round little hull, a cabin
    on the stern, a mast with a striped sail and a pennant."""
    bz = STEIGER_Z + 4                      # the boat's middle line
    xa, xv = 126, 137                        # stern .. bow
    def breed(x):
        t = (x - xa) / (xv - xa)
        return 2 if 0.12 < t < 0.72 else 1 if t < 0.9 else 0
    for x in range(xa, xv + 1):
        w = breed(x)
        for dz in range(-w, w + 1):
            z = bz + dz
            rand = abs(dz) == w
            b.set(x, G - 2, z, "dark_oak_planks") if not rand or w == 0 else None
            b.set(x, G - 1, z, "dark_oak_planks" if rand or x in (xa, xv) else "spruce_planks")
            b.set(x, G, z, "spruce_planks")                       # the deck: level with the jetty
            b.top[(x, z)] = G
            b.vrij.add((x, z))
        if w == 0:
            b.set(x, G + 1, bz, "dark_oak_fence")
            b.lamp(x, G + 2, bz)
    # the red stripe under the gunwale and the gunwale itself (trapdoors standing up along the seaward side)
    for x in range(xa, xv):
        w = breed(x)
        if w:
            b.set(x, G, bz + w, "red_terracotta")
            b.set(x, G, bz - w, "red_terracotta")
            b.luik(x, G + 1, bz + w, "north", "dark_oak_trapdoor", open_=True)
    # the gangway keeps the jetty side open between x 129 and 132; rails elsewhere on that side
    for x in range(xa, xv):
        w = breed(x)
        if w and not (129 <= x <= 132):
            b.luik(x, G + 1, bz - w, "south", "dark_oak_trapdoor", open_=True)
    for x in (130, 131):
        b.set(x, G, STEIGER_Z + 2, "spruce_planks")
        b.top[(x, STEIGER_Z + 2)] = G
        b.vrij.add((x, STEIGER_Z + 2))
    # the cabin on the stern
    for x in (xa, xa + 1):
        for dz in (-1, 0, 1):
            b.set(x, G + 1, bz + dz, "spruce_planks" if not (x == xa + 1 and dz == 0) else "air")
            b.plaat(x, G + 2, bz + dz, "dark_oak_slab")
    b.leeg(xa + 1, G + 1, bz)
    b.set(xa, G + 1, bz, "glass_pane")
    b.lamp(xa + 1, G + 3, bz) if False else None
    # the mast, the yard and the square sail before it (white with a red stripe), a pennant on top
    mx = 132
    for y in range(G + 1, G + 11):
        b.set(mx, y, bz, "spruce_fence" if y > G + 1 else "stripped_spruce_log", None if y > G + 1 else {"axis": "y"})
    for dz in range(-2, 3):
        b.set(mx + 1, G + 9, bz + dz, "spruce_fence")
        for y in range(G + 4, G + 9):
            b.set(mx + 1, y, bz + dz, "red_wool" if y == G + 6 else "white_wool")
    b.set(mx - 1, G + 10, bz, "red_wool")
    b.set(mx - 2, G + 10, bz, "red_wool")
    b.vat(135, G + 1, bz)


# =====================================================================================================================
# the meadow
# =====================================================================================================================
def wei(b):
    rng = random.Random(2130_7006)
    tx, tz = TRAINER
    # Meester Truffelneus' snuffelschooltje: an open shelter, a row of log seats, a notice board, hay bales to hide things in
    for (dx, dz) in ((-3, -3), (1, -3), (-3, -1), (1, -1)):
        for i in range(3):
            b.set(tx + dx, G + 1 + i, tz + dz - 2, "spruce_log" if False else "stripped_spruce_log", {"axis": "y"})
    for dx in range(-4, 3):
        for dz in range(-4, 1):
            x, z = tx + dx, tz + dz - 2
            rij = dz + 4
            y = G + 4 + (1 if rij in (1, 2, 3) else 0) + (0 if rij != 2 else 0)
            if rij == 2:
                b.plaat(x, y, z, "spruce_slab", boven=True)
            else:
                b.trap(x, G + 4 + (0 if rij in (0, 4) else 1), z, "south" if rij < 2 else "north", "spruce_stairs")
            b.vrij.add((x, z))
    b.set(tx - 1, G + 1, tz - 5, "bookshelf")
    b.set(tx - 2, G + 1, tz - 5, "barrel", {"facing": "up", "open": "false"})
    b.pot(tx - 2, G + 2, tz - 5, "oxeye_daisy")
    b.lamp(tx - 1, G + 4, tz - 5, hangend=True)
    b.bord(tx + 2, G + 1, tz - 2, ["Snuffelschool", "Truffelneus", "Neus omlaag,", "staart omhoog"], rotation=2)
    b.bezet(tx - 4, tz - 6, tx + 3, tz + 3)
    for i, (dx, dz) in enumerate(((-3, 2), (-1, 3), (1, 3), (3, 2))):
        b.set(tx + dx, G + 1, tz + dz, "stripped_spruce_log", {"axis": "x"})
        b.vrij.add((tx + dx, tz + dz))
    for (dx, dz) in ((6, -4), (7, -4), (6, -3)):
        b.set(tx + dx, G + 1, tz + dz, "hay_block", {"axis": "y"})
        b.vrij.add((tx + dx, tz + dz))
    b.set(tx + 6, G + 2, tz - 4, "hay_block", {"axis": "x"})
    # the pond in the north-west of the meadow
    px, pz = 64, 62
    for x in range(px - 6, px + 7):
        for z in range(pz - 5, pz + 6):
            r = math.hypot((x - px) / 1.25, z - pz) + 0.8 * R3(x, z, 3.0)
            if (x, z) not in b.top:
                continue
            if r <= 3.6:
                b.set(x, G, z, "water", {"level": "0"})
                b.set(x, G - 1, z, "water", {"level": "0"} if r <= 2.4 else None) if r <= 2.4 else b.set(x, G - 1, z, "clay")
                b.set(x, G - 2, z, "clay")
                b.vrij.add((x, z))
                if rng.random() < 0.18:
                    b.set(x, G + 1, z, "lily_pad")
            elif r <= 4.7:
                zand = rng.random() < 0.5
                b.set(x, G, z, "sand" if zand else "grass_block")
                b.vrij.add((x, z))
                if not zand and rng.random() < 0.5:
                    b.set(x, G + 1, z, "fern")
    # the bee tree on the west side: a big oak with a bee nest and flowers around it
    bx, bz_ = 56, 72
    b.boom(bx, bz_, hoog=6, soort="oak", breed=3)
    b.set(bx + 1, G + 3, bz_, "bee_nest", {"facing": "east", "honey_level": "5"})
    for _ in range(26):
        x, z = bx + rng.randint(-5, 5), bz_ + rng.randint(-5, 5)
        bloembak(b, x, z, ("poppy", "dandelion", "cornflower", "oxeye_daisy", "allium", "azure_bluet"))
    # the tree's knoll: a ring of stones around the foot, steps on the village side
    kx, kz = BOOM
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            r = math.hypot(dx, dz)
            x, z = kx + dx, kz + dz
            if 1.6 <= r <= 2.3:
                b.set(x, G + BOOM_HOOG + 1, z, "mossy_cobblestone_slab" if (dx + dz) % 2 else "cobblestone_slab", {"type": "bottom", "waterlogged": "false"})
            if r < 1.6:
                b.set(x, G + BOOM_HOOG, z, "coarse_dirt" if (dx or dz) else "rooted_dirt")
    b.vrij.add((kx, kz))
    for dx in range(-8, 9):
        for dz in range(-8, 9):
            b.laag.add((kx + dx, kz + dz))
    # the way up: steps on the south-west (from the path) and on the north (towards the camera's side, for a walk around)
    for (x, z, facing) in ((kx - 7, kz, "east"), (kx - 4, kz, "east")):
        trapje(b, x, z, facing)
        trapje(b, x, z + 1, facing)
        trapje(b, x, z - 1, facing)
    for (x, z) in ((kx - 3, kz), (kx - 2, kz)):
        b.leeg(x, G + BOOM_HOOG + 1, z)
    # the camera corridor of the growth scene stays free of trees and bushes (see controleer); flowers may grow there
    for x in range(kx - 12, kx + 13):
        for z in range(kz - 16, kz + 3):
            b.laag.add((x, z))


def versperring(b):
    """The friendly roadblock in the cut through the ridge: a striped barrier, lanterns, flower pots and the sign."""
    z = VERSPERRING_Z
    for x in range(CX - 3, CX + 4):
        if (x, z) not in b.top or b.top[(x, z)] != G:
            continue
        b.set(x, G + 1, z, "spruce_fence")
        b.set(x, G + 2, z, "red_wool" if (x - CX) % 2 == 0 else "white_wool")
        b.vrij.add((x, z))
    for dx in (-3, 3):
        x = CX + dx
        if (x, z) in b.top and b.top[(x, z)] == G:
            b.set(x, G + 1, z, "stripped_spruce_log", {"axis": "y"})
            b.set(x, G + 2, z, "stripped_spruce_log", {"axis": "y"})
            b.lamp(x, G + 3, z)
    b.bord(CX, G + 1, z + 1, ["Hier mag je", "pas door als", "Snuffelneus", "(rang 2 van 5)"], rotation=0)
    b.bord(CX - 2, G + 1, z + 1, ["Sorry, njeg!", "Eerst leren", "snuffelen bij", "Truffelneus"], rotation=1, hout="oak")
    b.pot(CX + 2, G + 1, z + 1, "poppy")
    b.pot(CX + 1, G + 1, z + 1, "dandelion") if False else None
    for dz in (1, 2):
        for dx in range(-3, 4):
            b.vrij.add((CX + dx, z + dz))
    # the strange mushroom beside the road just south of the roadblock (the exam's strange scent hangs here): a small giant
    # one of full blocks, with potted little ones on mossy stones around it
    mx, mz = CX + 7, z + 5
    b.set(mx, G + 1, mz, "mushroom_stem", STEEL)
    b.set(mx, G + 2, mz, "mushroom_stem", STEEL)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(mx + dx, G + 3, mz + dz, "red_mushroom_block", STEEL)
    b.set(mx + 2, G + 1, mz + 1, "mossy_cobblestone")
    b.pot(mx + 2, G + 2, mz + 1, "red_mushroom")
    b.set(mx - 2, G + 1, mz, "mossy_cobblestone_slab", {"type": "bottom", "waterlogged": "false"})
    b.pot(mx - 1, G + 1, mz + 2, "brown_mushroom")
    b.bezet(mx - 2, mz - 2, mx + 2, mz + 2)


# =====================================================================================================================
# the closed part: what you see from the meadow
# =====================================================================================================================
def dicht(b):
    rng = random.Random(2130_7007)
    # the lighthouse on the north-east cape: white with red bands, a lantern room, a red cap
    lx, lz = 116, 35
    basis = b.voet(lx, lz)
    for y in range(basis - 2, basis + 16):
        i = y - basis
        r = 3.2 if i < 5 else 2.6 if i < 11 else 2.2
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                d = math.hypot(dx, dz)
                if d <= r + 0.3:
                    rand = d > r - 0.9
                    if rand:
                        b.set(lx + dx, y, lz + dz, "red_concrete" if i in (3, 4, 9, 10) else "white_concrete")
                    elif i < 0:
                        b.set(lx + dx, y, lz + dz, "stone_bricks")
                    else:
                        b.leeg(lx + dx, y, lz + dz)
    top = basis + 16
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            d = math.hypot(dx, dz)
            if d <= 3.4:
                b.set(lx + dx, top, lz + dz, "stone_brick_slab", {"type": "bottom", "waterlogged": "false"} if d > 2.3 else None) if d > 2.3 else b.set(lx + dx, top, lz + dz, "stone_bricks")
            if 1.5 < d <= 2.4:
                for y in (top + 1, top + 2):
                    b.set(lx + dx, y, lz + dz, "glass")
            if d <= 2.4:
                b.set(lx + dx, top + 3, lz + dz, "red_concrete")
            if d <= 1.4:
                b.set(lx + dx, top + 4, lz + dz, "red_concrete")
    b.set(lx, top + 1, lz, "sea_lantern")
    b.set(lx, top + 2, lz, "sea_lantern")
    b.set(lx, top + 5, lz, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.set(lx, basis, lz + 3, "air")
    for i, half in enumerate(("lower", "upper")):
        b.set(lx, basis + i, lz + 3, "dark_oak_door", {"facing": "north", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    b.bezet(lx - 5, lz - 5, lx + 5, lz + 5)
    # the standing stones on the hill west of the road
    sx_, sz_ = 60, 27
    for k in range(7):
        a = k * math.tau / 7
        x, z = int(round(sx_ + 4.5 * math.cos(a))), int(round(sz_ + 4.5 * math.sin(a)))
        if (x, z) in b.top:
            y = b.voet(x, z)
            for i in range(2 + (k % 3)):
                b.set(x, y + i, z, "mossy_cobblestone" if (i + k) % 2 else "cobblestone")
            b.vrij.add((x, z))
    # the forest: spruces and oaks on the hills, thinner near the coast
    for _ in range(900):
        x, z = rng.randint(14, SX - 14), rng.randint(8, 46)
        if (x, z) not in b.top or (x, z) in b.vrij or (x, z) in b.zand or z > rug_z(x) - 5:
            continue
        if kust(x, z) < 5 or b.get(x, b.top[(x, z)], z) not in ("minecraft:grass_block",):
            continue
        if any((x + dx, z + dz) in b.vrij for dx in (-2, 0, 2) for dz in (-2, 0, 2)):
            continue
        if rng.random() < 0.6:
            b.spar(x, z, rng.randint(6, 9))
        else:
            b.boom(x, z, rng.randint(4, 6), "oak" if rng.random() < 0.7 else "birch")
    # the road goes on behind the roadblock (so you can see where it leads)
    pad(b, [(CX, VERSPERRING_Z - 1), (CX - 2, 42), (CX - 6, 34), (CX - 14, 29), (CX - 20, 28)], 1.2, "dirt_path", 0.85)
    pad(b, [(CX - 6, 34), (CX + 8, 30), (CX + 22, 33), (CX + 28, 35)], 1.0, "dirt_path", 0.7)


def palm(b, x, z, hoog=5):
    y = b.voet(x, z)
    for i in range(hoog):
        b.set(x, y + i, z, "jungle_log", {"axis": "y"})
    top = y + hoog
    b.blad(x, top, z, "jungle")
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        b.blad(x + dx, top, z + dz, "jungle")
        b.blad(x + 2 * dx, top, z + 2 * dz, "jungle")
        b.blad(x + 3 * dx, top - 1, z + 3 * dz, "jungle")
    for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        b.blad(x + dx, top, z + dz, "jungle")
        b.blad(x + 2 * dx, top - 1, z + 2 * dz, "jungle")
    b.bezet(x - 1, z - 1, x + 1, z + 1)


def strand_(b, plek):
    """The beach where you wash ashore: palms, a sandcastle, a washed-up crate and barrel, Jutje's flag and a beach sign. The
    spot itself and the way Jutje comes running (the waking scene) stay empty."""
    sx_, sz_ = plek
    for i in range(0, 14):
        for d in (-2, -1, 0, 1, 2):
            b.vrij.add((sx_ + round(i * 0.62) + d, sz_ - i))
            b.vrij.add((sx_ + round(i * 0.62), sz_ - i + d))
    for dx in range(-6, 4):
        for dz in range(-3, 6):
            b.vrij.add((sx_ + dx, sz_ + dz))
    for (x, z, hoog) in ((sx_ - 12, sz_ - 8, 5), (sx_ + 12, sz_ + 2, 6), (sx_ - 9, sz_ - 20, 5), (sx_ + 22, sz_ - 4, 5), (sx_ + 16, sz_ + 6, 4)):
        if (x, z) in b.zand and (x, z) not in b.vrij and kust(x, z) > 2.5:
            palm(b, x, z, hoog)
    # washed ashore with you: a crate, a barrel on its side, a plank
    for (dx, dz, wat, props) in ((-5, -4, "barrel", {"facing": "west", "open": "false"}), (-6, -5, "spruce_planks", None),
                                 (-6, -4, "spruce_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"}),
                                 (5, 4, "stripped_spruce_log", {"axis": "x"}), (6, 4, "stripped_spruce_log", {"axis": "x"})):
        x, z = sx_ + dx, sz_ + dz
        if (x, z) in b.zand and kust(x, z) > 1.5 and b.get(x, G + 1, z) is None:
            b.set(x, G + 1, z, wat, props)
            b.vrij.add((x, z))
    # a sandcastle with a little flag
    cx_, cz_ = sx_ - 8, sz_ + 1
    if all((cx_ + dx, cz_ + dz) in b.zand for dx in (-1, 0, 1) for dz in (-1, 0, 1)) and kust(cx_, cz_) > 3:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                hoek = dx and dz
                b.set(cx_ + dx, G + 1, cz_ + dz, "sandstone_wall" if hoek else "cut_sandstone_slab" if (dx or dz) else "chiseled_sandstone",
                      {"type": "bottom", "waterlogged": "false"} if (not hoek and (dx or dz)) else {"up": "true", "north": "none", "south": "none",
                                                                                               "east": "none", "west": "none", "waterlogged": "false"}
                      if hoek else None)
                b.vrij.add((cx_ + dx, cz_ + dz))
        b.set(cx_, G + 2, cz_, "sandstone_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
        b.set(cx_, G + 3, cz_, "red_carpet")
    # Jutje's beach sign where the planks begin
    bx, bz_ = sx_ + 6, sz_ - 4
    if (bx, bz_) in b.top and b.get(bx, G + 1, bz_) is None:
        b.bord(bx, G + 1, bz_, ["Snuffeldorp", "die kant op", "(volg de", "planken)"], rotation=2)


# =====================================================================================================================
# plants
# =====================================================================================================================
def planten(b):
    rng = random.Random(2130_7008)
    # trees of the start zone: a few in the village, a loose row along the meadow's edges, palms of driftwood are not needed
    plekken = [(58, 108, "oak"), (76, 100, "birch"), (90, 104, "oak"), (100, 116, "birch"), (76, 128, "oak"), (88, 130, "birch"),
               (58, 118, "birch"), (114, 98, "oak"), (50, 96, "oak"), (118, 90, "birch"), (46, 84, "oak"), (120, 74, "oak"), (112, 62, "birch"),
               (48, 64, "birch"), (74, 58, "oak"), (106, 56, "oak"), (42, 110, "oak"), (124, 126, "oak"), (100, 138, "birch"), (84, 142, "oak"),
               (114, 140, "oak"), (60, 92, "birch"), (108, 92, "oak"), (36, 98, "birch"), (128, 100, "birch")]
    for x, z, soort in plekken:
        if (x, z) in b.top and (x, z) not in b.vrij and (x, z) not in b.zand and b.top[(x, z)] == G and kust(x, z) > 5:
            if not any((x + dx, z + dz) in b.vrij or (x + 2 * dx, z + 2 * dz) in b.laag for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
                b.boom(x, z, rng.randint(4, 5), soort, 2)
    # bushes
    for _ in range(70):
        x, z = rng.randint(20, SX - 20), rng.randint(54, SZ - 30)
        if ((x, z) in b.top and (x, z) not in b.vrij and (x, z) not in b.laag and (x, z) not in b.zand and b.top[(x, z)] == G
                and b.get(x, G, z) == "minecraft:grass_block"):
            b.struik(x, z, rng.choice(("azalea", "bloei", "oak")))
    # flowers and grass on every free grass block, a bit more colour in the meadow
    bloemen = ["poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "pink_tulip", "white_tulip"]
    for (x, z), y in sorted(b.top.items()):
        if (x, z) in b.vrij or b.get(x, y, z) != "minecraft:grass_block" or b.get(x, y + 1, z) is not None:
            continue
        wei_ = rug_z(x) + 2 < z < WEIPOORT[1]
        kans = rng.random()
        if kans < (0.07 if wei_ else 0.035):
            b.set(x, y + 1, z, rng.choice(bloemen))
        elif kans < 0.24:
            b.set(x, y + 1, z, "short_grass" if rng.random() < 0.85 else "fern")
    # the beach: driftwood, shells (dead coral fans do not exist dry: stones and bushes), a sandcastle
    for _ in range(40):
        x, z = rng.randint(12, SX - 12), rng.randint(60, SZ - 8)
        if (x, z) in b.zand and (x, z) not in b.vrij and b.get(x, G + 1, z) is None and kust(x, z) > 1.5:
            wat = rng.random()
            if wat < 0.35:
                b.set(x, G + 1, z, "dead_bush")
            elif wat < 0.6:
                b.set(x, G + 1, z, "stripped_spruce_log", {"axis": rng.choice("xz")})
            elif wat < 0.8:
                b.set(x, G + 1, z, "cobblestone_slab", {"type": "bottom", "waterlogged": "false"})
            else:
                b.set(x, G + 1, z, "sandstone_slab", {"type": "bottom", "waterlogged": "false"})
            b.vrij.add((x, z))


VERBIND = ("_fence", "glass_pane", "_wall")


def verbind(b):
    """Fences, panes and walls get their connections (a stamp does it too, but the renders and the tiles' edges want it)."""
    s = b.s
    vast = lambda n: n is not None and not any(n.endswith(e) for e in ("_carpet", "lantern", "_sign", "_slab", "_stairs", "_door", "_trapdoor", "chain",
                                                                              "campfire", "_pressure_plate", "water", "_leaves", "_bed", "lily_pad", "cake"))
    planten_ = ("short_grass", "fern", "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "pink_tulip", "white_tulip", "orange_tulip",
                "dead_bush", "red_mushroom", "brown_mushroom", "carrots", "potatoes", "beetroots", "wheat", "blue_orchid", "red_tulip")
    for (x, y, z), (name, props, nbt) in list(s.blocks.items()):
        kort = name.split(":")[1]
        if kort.endswith("_fence") or kort == "glass_pane":
            p = dict(props)
            for richting, (dx, dz) in RICHTING.items():
                buur = s.get(x + dx, y, z + dz)
                bk = buur.split(":")[1] if buur else None
                aan = bk is not None and (bk.endswith("_fence") or bk == "glass_pane" or bk.endswith("_fence_gate")
                                          or (vast(bk) and bk not in planten_ and not bk.startswith("potted_") and bk != "air"))
                if kort.endswith("_fence") and bk == "glass_pane":
                    aan = False
                if kort == "glass_pane" and bk is not None and bk.endswith("_fence"):
                    aan = False
                p[richting] = "true" if aan else "false"
            p.setdefault("waterlogged", "false")
            s.blocks[(x, y, z)] = (name, p, nbt)


# =====================================================================================================================
# the island
# =====================================================================================================================
def eiland(h):
    """-> (Structure, dict for eiland.json, dict for dorp.json)."""
    b = Bouw(h)
    b.vrij_deur = b.vrij
    grond(b)
    px, pz = PLEIN
    wx, wz = WEIPOORT
    kx, kz = BOOM
    # --- the paths (before the buildings, so that doorsteps join them) ---
    wegen = [
        [(58, 157), (62, 148), (STRANDPOORT[0], STRANDPOORT[1] + 2), STRANDPOORT, (77, 124), (px - 5, pz + 5)],          # the beach to the plein
        [(px, pz - 8), (wx, wz + 2), (wx, wz - 2), (wx, 82), (CX, 66), (CX, VERSPERRING_Z + 1)],                      # the plein to the meadow and the roadblock
        [(wx, 82), (TRAINER[0] + 4, TRAINER[1] + 1)],                                                                  # to the trainer
        [(wx, 80), (kx - 8, kz)],                                                                                      # to the tree's knoll
        [(px + 8, pz), (103, STEIGER_Z)],                                                                              # the plein to the quay
        [(px - 8, pz - 3), (70, 109), (70, 107)],                                                                      # to the doctor
        [(px + 7, pz - 5), (97, 108), (97, 107)],                                                                      # to the school
        [(px - 8, pz), (71, 114)],                                                                                     # to the bakery
        [(77, 124), (73, 125)],                                                                                        # to oma
        [(px + 5, pz + 7), (91, 126)],                                                                                 # to the tuinder
        [(91, 126), (91, 131), (100, 133)],                                                                            # and on to his moestuin
        [(110, 110), (110, 109)],
        [(109, 118), (109, 119)],
    ]
    for w in wegen:
        pad(b, w, 1.25)
    plein(b)
    huizen = dict(dokter=dokter(b), bakker=bakker(b), school=school(b), oma=oma(b), tuinder=tuinder(b), visser=visser(b),
                  kantoor=havenkantoor(b), jutter=juttershut(b))
    strandpoort(b)
    heg(b)
    haven(b)
    wei(b)
    versperring(b)
    dicht(b)
    for naam in ("dokter", "bakker", "school", "oma", "tuinder", "visser", "kantoor"):
        tuintje(b, huizen[naam])
    # --- what stands and lies where (template coordinates = relative to the island's corner; y = feet level) ---
    F = G + 1

    def plek(x, z, dy=0.0):
        return [x + 0.5, b.voet(x, z) + dy, z + 0.5]

    strand = (56, 161)
    while kust(*strand) < 4.0:
        strand = (strand[0], strand[1] - 1)
    bewoners = [
        dict(sleutel="redder", bewoner="redder", plek=plek(STRANDPOORT[0] + 1, STRANDPOORT[1] - 2), yaw=200.0 - 180.0 if False else 20.0, houding="kwispel"),
        dict(sleutel="dokter", bewoner="dokter", plek=plek(69, 102), yaw=0.0),
        dict(sleutel="trainer", bewoner="trainer", plek=plek(TRAINER[0], TRAINER[1]), yaw=-70.0, houding="zit"),
        dict(sleutel="bakker", bewoner="bakker", plek=plek(px - 5, pz), yaw=-90.0),
        dict(sleutel="visser", bewoner="visser", plek=plek(HAVEN_X + 5, STEIGER_Z), yaw=180.0, houding="zit"),
        dict(sleutel="juf", bewoner="juf", plek=plek(100, 108), yaw=90.0),
        dict(sleutel="oma", bewoner="oma", plek=plek(72, 125), yaw=-90.0, houding="zit"),
        dict(sleutel="tuinder", bewoner="tuinder", plek=plek(104, 133), yaw=90.0),
        dict(sleutel="pup", bewoner="pup", plek=plek(px + 3, pz + 3), yaw=135.0, houding="kwispel"),
        # (his key is not "kapitein": that is the captain at a dock in the Guhmensie, with the dock slice's role)
        dict(sleutel="havenkapitein", bewoner="kapitein", plek=plek(134, STEIGER_Z), yaw=90.0),
    ]
    haven_plek = (131, STEIGER_Z)
    geuren = [
        dict(id="kluifje", soort="eten", rang=1, icoon="minecraft:bone"),
        dict(id="fluitje", soort="voorwerp", rang=1, icoon="minecraft:goat_horn"),
        dict(id="bijen", soort="dier", rang=1, icoon="minecraft:honeycomb"),
        dict(id="bosgeestje", soort="vreemd", rang=1, icoon="minecraft:moss_block"),
        dict(id="deegroller", soort="eten", rang=1, icoon="minecraft:stick"),
        dict(id="dobber", soort="dier", rang=1, icoon="minecraft:fishing_rod"),
        dict(id="schoolbel", soort="voorwerp", rang=1, icoon="minecraft:bell"),
        dict(id="bolwol", soort="dier", rang=1, icoon="minecraft:purple_wool"),
        dict(id="gietertje", soort="voorwerp", rang=1, icoon="minecraft:bucket"),
        dict(id="stuiterbal", soort="voorwerp", rang=1, icoon="minecraft:slime_ball"),
        dict(id="kaasknabbel", soort="eten", rang=1, icoon="guhs:kaas_knabbels"),
        dict(id="tweedpet", soort="voorwerp", rang=1, icoon="minecraft:leather_helmet"),
        dict(id="kippen", soort="dier", rang=1, icoon="minecraft:egg"),
        dict(id="paddenstoel", soort="vreemd", rang=1, icoon="minecraft:red_mushroom"),
        dict(id="papa_sjaal", soort="vreemd", rang=1, icoon="minecraft:blue_wool"),
    ]

    def bron(id, geur, x, z, graven=True, bereik=26):
        return dict(id="dorp_" + id, geur=geur, plek=[x, b.voet(x, z), z], graven=graven, bereik=bereik)

    bronnen = [
        # the three lessons
        bron("les_kluifje", "kluifje", TRAINER[0] + 7, TRAINER[1] + 5, True, 16),
        bron("les_fluitje", "fluitje", 62, 88, True, 34),
        bron("les_bijen", "bijen", 58, 72, False, 30),
        # what the villagers lost
        bron("deegroller", "deegroller", 106, 100 - 1, True, 44),
        bron("dobber", "dobber", 135, 130, True, 44),
        bron("schoolbel", "schoolbel", 70, 60, True, 62),
        bron("bolwol", "bolwol", 50, 146, True, 44),
        bron("gietertje", "gietertje", 108, 84, True, 58),
        bron("stuiterbal", "stuiterbal", 66, 142, True, 40),
        # the exam: one of every kind
        bron("examen_kaasknabbel", "kaasknabbel", 102, 62, True, 40),
        bron("examen_tweedpet", "tweedpet", 74, 91, True, 40),
        bron("examen_kippen", "kippen", 108, 137, False, 40),
        bron("examen_paddenstoel", "paddenstoel", CX + 7, VERSPERRING_Z + 6, False, 40),
        # father's scarf: under the stones at the tree, on its north side
        dict(id="dorp_papa_sjaal", geur="papa_sjaal", plek=[kx, G + BOOM_HOOG + 1, kz - 2], graven=True, bereik=30),
    ]
    for br in bronnen:
        b.vrij.add((br["plek"][0], br["plek"][2]))
    for bw in bewoners:
        b.vrij.add((int(math.floor(bw["plek"][0])), int(math.floor(bw["plek"][2]))))
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            b.vrij.add((strand[0] + dx, strand[1] + dz))
    strand_(b, strand)
    planten(b)
    verbind(b)
    emmer = (px, G + 2, pz - 1)                    # the air block on the well's north rim (the side of the weipoort): the bucket of the companion's scene stands here
    data = dict(versie=VERSIE, oorsprong=list(OORSPRONG), maat=[SX, SY, SZ], stukken=[],
                strand=dict(plek=[strand[0] + 0.5, b.voet(*strand), strand[1] + 0.5], yaw=180.0),
                haven=dict(plek=[haven_plek[0] + 0.5, G + 1, haven_plek[1] + 0.5], yaw=90.0),
                boom=[kx, G + BOOM_HOOG + 1, kz], boom_draai=2, grens=12, bewoners=bewoners, geuren=geuren, geurbronnen=bronnen)
    dorp = dict(
        plekken=dict(
            strand=[strand[0], b.voet(*strand), strand[1]],
            strandpoort=[STRANDPOORT[0], F, STRANDPOORT[1]],
            emmer=list(emmer),
            plein=[px, F, pz - 5],
            weipoort=[wx, F, wz],
            wei=[TRAINER[0] + 2, F, TRAINER[1] + 2],
            boom=[kx, G + BOOM_HOOG + 1, kz],
            haven=[haven_plek[0], F, haven_plek[1]],
            versperring=[CX, F, VERSPERRING_Z + 1],
            dokter=[70, F, 107],
        ),
        grens_z=GRENS_Z,
    )
    problems = controleer(b, data, dorp)
    if problems:
        raise SystemExit("snuffel_dorp: the island is not right:\n  " + "\n  ".join(problems))
    b.data, b.dorp = data, dorp
    return b, data, dorp


def tegel_volgorde(s, tegels_):
    """The order in which the tiles are stamped. The game checks every stamped block's support at once, so a wall sign whose
    wall lies in a tile that comes LATER would be gone again before its wall is there (seen on a dev server: the sign
    "Snuffeldorp" on the weipoort, whose beam lies exactly on a seam). So a tile comes after every tile one of its wall
    signs leans on; the rest keeps its sorted order."""
    na = {t: set() for t in tegels_}                 # tile -> the tiles that must be stamped before it
    for (x, y, z), (name, props, _nbt) in s.blocks.items():
        if name.endswith("_wall_sign"):
            dx, dz = RICHTING[TEGEN[props["facing"]]]
            hier, daar = (x // TEGEL, z // TEGEL), ((x + dx) // TEGEL, (z + dz) // TEGEL)
            if daar != hier and daar in na:
                na[hier].add(daar)
    volgorde = []
    over = sorted(tegels_)
    while over:
        vrij = [t for t in over if not (na[t] - set(volgorde))]
        if not vrij:
            raise SystemExit(f"snuffel_dorp: wall signs lean on each other's tiles in a circle: {over}")
        volgorde.append(vrij[0])
        over.remove(vrij[0])
    return volgorde


def tegels(b, data):
    """Saves the island as tiles of TEGEL x SY x TEGEL and fills data["stukken"] (in the order they are stamped)."""
    s = b.s
    stukken = []
    per = {}
    for (x, y, z), v in s.blocks.items():
        per.setdefault((x // TEGEL, z // TEGEL), []).append(((x, y, z), v))
    for (i, j) in tegel_volgorde(s, list(per)):
        t = b.h.Structure((min(TEGEL, SX - i * TEGEL), SY, min(TEGEL, SZ - j * TEGEL)))
        for (x, y, z), v in per[(i, j)]:
            if v[0] != "minecraft:air":
                t.blocks[(x - i * TEGEL, y, z - j * TEGEL)] = v
        naam = f"snuffeldorp/eiland_{i}_{j}"
        t.save(naam)
        stukken.append(dict(template="guhs:" + naam, plek=[i * TEGEL, 0, j * TEGEL]))
    data["stukken"] = stukken
    return stukken


VAST = ("minecraft:grass_block", "minecraft:sand", "minecraft:dirt_path", "minecraft:spruce_planks", "minecraft:dirt", "minecraft:stone",
        "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cobblestone", "minecraft:andesite", "minecraft:mossy_cobblestone",
        "minecraft:polished_andesite", "minecraft:birch_planks", "minecraft:oak_planks", "minecraft:stripped_spruce_log", "minecraft:coarse_dirt",
        "minecraft:rooted_dirt", "minecraft:dark_oak_planks", "minecraft:moss_block")
LOS = (None, "minecraft:short_grass", "minecraft:fern", "minecraft:white_carpet", "minecraft:red_carpet")


def controleer(b, data, dorp):
    """Everything that stands somewhere stands on solid ground with room above it; buried things lie under open sky; the
    scenes have the free room their scripts count on; the start zone is closed off from the rest except by the roadblock."""
    s = b.s
    problems = []

    def staat(naam, plek, hoog=2):
        x, y, z = int(math.floor(plek[0])), int(round(plek[1])), int(math.floor(plek[2]))
        if s.get(x, y - 1, z) not in VAST:
            problems.append(f"{naam} at {plek} stands on {s.get(x, y - 1, z)}")
        for dy in range(hoog):
            if s.get(x, y + dy, z) not in LOS:
                problems.append(f"{naam} at {plek}: {s.get(x, y + dy, z)} in the way")

    staat("strand", data["strand"]["plek"])
    staat("haven", data["haven"]["plek"])
    for bw in data["bewoners"]:
        staat("bewoner " + bw["sleutel"], bw["plek"])
    geuren = {g["id"] for g in data["geuren"]}
    for br in data["geurbronnen"]:
        x, y, z = br["plek"]
        if br["geur"] not in geuren:
            problems.append(f"geurbron {br['id']}: unknown scent {br['geur']}")
        if s.get(x, y - 1, z) is None:
            problems.append(f"geurbron {br['id']} hangs in the air")
        if br["graven"]:
            if s.get(x, y - 1, z) not in ("minecraft:grass_block", "minecraft:sand", "minecraft:coarse_dirt", "minecraft:dirt_path", "minecraft:rooted_dirt"):
                problems.append(f"geurbron {br['id']} is buried under {s.get(x, y - 1, z)}")
            if s.get(x, y, z) not in LOS and not s.get(x, y, z).endswith("_slab"):
                problems.append(f"geurbron {br['id']}: {s.get(x, y, z)} on top of it")
        if z < GRENS_Z + 3:
            problems.append(f"geurbron {br['id']} lies in the closed part")
    # what a stamp would take away again: the game checks every placed block's support (a wall sign with air behind it, a
    # hanging lantern under nothing, a plant on the wrong ground simply disappear)
    PLANT = ("short_grass", "fern", "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "pink_tulip", "white_tulip", "orange_tulip")
    GROND = ("grass_block", "dirt", "coarse_dirt", "rooted_dirt", "moss_block", "farmland", "podzol")
    for (x, y, z), (name, props, _nbt) in s.blocks.items():
        k = name.split(":")[1]
        onder = (s.get(x, y - 1, z) or ":").split(":")[1]
        boven = s.get(x, y + 1, z)
        fout = None
        if k.endswith("_wall_sign"):
            dx, dz = RICHTING[TEGEN[props["facing"]]]
            achter = s.get(x + dx, y, z + dz)
            if achter is None or achter.split(":")[1] in PLANT:
                fout = "has nothing behind it"
        elif k.endswith("_sign") or k.endswith("_carpet") or k.endswith("_pressure_plate") or k == "cake" or (k.endswith("_door") and props.get("half") == "lower"):
            if not onder or onder in PLANT:
                fout = "stands on nothing"
        elif k == "lantern" and props.get("hanging") == "true":
            if boven is None:
                fout = "hangs from nothing"
        elif k == "lantern":
            if not onder:
                fout = "stands on nothing"
        elif k in PLANT and onder not in GROND:
            fout = f"grows on {onder}"
        elif k == "dead_bush" and onder not in ("sand", "red_sand", "dirt", "coarse_dirt", "terracotta"):
            fout = f"grows on {onder}"
        elif k in ("carrots", "potatoes", "beetroots", "wheat") and onder != "farmland":
            fout = f"grows on {onder}"
        elif k == "lily_pad" and onder != "water":
            fout = f"floats on {onder}"
        if fout:
            problems.append(f"{k} at {(x, y, z)} {fout}")
    # the growth scene's camera (feature/snuffel/Boom.java), turned by boom_draai: 12 blocks to the unturned south of the
    # tree, 6 up, 9 west .. 9 east; with boom_draai 2 that is NORTH of the tree, looking back over the village
    bx, by, bz = data["boom"]
    teken = {0: 1, 2: -1}[data["boom_draai"]]
    for dx in range(-10, 11):
        for dy in (4, 5, 6, 7):
            if s.get(bx + dx, by + dy, bz + teken * 12) is not None:
                problems.append(f"the growth scene's camera path is blocked at x {bx + dx}, y +{dy}: {s.get(bx + dx, by + dy, bz + teken * 12)}")
    for t in range(1, 12):
        for dx in (-6, 0, 6):
            x, y, z = bx + round(dx * t / 12), by + 1 + round(5 * t / 12), bz + teken * t
            if s.get(x, y, z) is not None:
                problems.append(f"the view on the tree is blocked at {(x, y, z)} by {s.get(x, y, z)}")
    for dy in range(0, 5):
        if s.get(bx, by + dy, bz) is not None:
            problems.append(f"the tree's own spot holds {s.get(bx, by + dy, bz)} at +{dy}")
    if s.get(bx, by - 1, bz) is None:
        problems.append("the tree stands on nothing")
    # the scenes' floors: flat and free where their actors walk (the same numbers as feature/snuffeldorp/DorpScenes.java)
    def vloer(naam, anker, vakken, dy=0):
        ax, ay, az = anker
        for (dx, dz) in vakken:
            x, z = ax + dx, az + dz
            if s.get(x, ay + dy - 1, z) not in VAST:
                problems.append(f"scene {naam}: no floor at {(dx, dz)} ({s.get(x, ay + dy - 1, z)})")
            for h_ in (0, 1):
                if s.get(x, ay + dy + h_, z) not in LOS:
                    problems.append(f"scene {naam}: {s.get(x, ay + dy + h_, z)} at {(dx, dz)}")

    pl = dorp["plekken"]
    vloer("wakker", pl["strand"], [(0, 0), (1, -1), (1, -2), (2, -3), (3, -5), (4, -7), (6, -10), (7, -11), (-1, 1)])
    vloer("maatje", pl["emmer"], [(1, -6), (1, -5), (1, -4), (3, 4), (-5, 1), (0, -1), (0, -2)], dy=-1)
    vloer("afvaart", pl["haven"], [(0, 0), (2, 0), (-1, 1), (0, 2), (0, 3)])
    # the bucket's spot: on the well's rim
    ex, ey, ez = pl["emmer"]
    if s.get(ex, ey - 1, ez) not in ("minecraft:stone_bricks", "minecraft:mossy_stone_bricks") or s.get(ex, ey, ez) is not None:
        problems.append("the bucket's spot is not on the well's rim")
    # the start zone is closed: walking from the beach you never get north of the line (jump height 1, no swimming)
    start = (int(data["strand"]["plek"][0]), int(data["strand"]["plek"][2]))
    gezien, rij = {start}, [start]
    bereikt = {}
    while rij:
        x, z = rij.pop()
        y = b.voet(x, z) if (x, z) in b.top else None
        if y is None:
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in gezien or n not in b.top:
                continue
            ny = loopvlak(s, n[0], n[1], y)
            if ny is None:
                continue
            gezien.add(n)
            bereikt[n] = ny
            rij.append(n)
    noord = [n for n in gezien if n[1] < GRENS_Z]
    if noord:
        problems.append(f"the closed part can be walked into, e.g. at {sorted(noord)[:4]}")
    for naam, p in pl.items():
        if naam in ("emmer",):
            continue
        if (p[0], p[2]) not in gezien and (p[0], p[2]) != start:
            problems.append(f"spot {naam} {p} cannot be walked to from the beach")
    for bw in data["bewoners"]:
        c = (int(math.floor(bw["plek"][0])), int(math.floor(bw["plek"][2])))
        if c not in gezien and not binnen_bereik(s, c, gezien):
            problems.append(f"resident {bw['sleutel']} cannot be walked to from the beach")
    for br in data["geurbronnen"]:
        c = (br["plek"][0], br["plek"][2])
        if not any((c[0] + dx, c[1] + dz) in gezien for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
            problems.append(f"geurbron {br['id']} cannot be walked to from the beach")
    b.bereikbaar = gezien
    return problems


def loopvlak(s, x, z, van_y):
    """The feet level a dog reaches on column (x, z) coming from feet level van_y (a step or a jump of one block up, any
    drop down to 3), or None. Doors and open gates are passable."""
    for y in (van_y + 1, van_y, van_y - 1, van_y - 2, van_y - 3):
        onder, hier, boven = s.get(x, y - 1, z), s.get(x, y, z), s.get(x, y + 1, z)
        if staanbaar(onder) and doorloopbaar(hier) and (doorloopbaar(boven) or True):
            if y > van_y and not doorloopbaar(s.get(x, y + 1, z)):
                continue
            return y
    return None


def staanbaar(n):
    if n is None:
        return False
    k = n.split(":")[1]
    if k in ("water", "lily_pad", "air") or k.endswith("_carpet") or k.endswith("_door") or k.endswith("_sign") or k.endswith("_pane"):
        return False
    return not doorloopbaar(n)


def doorloopbaar(n):
    if n is None:
        return True
    k = n.split(":")[1]
    return (k in ("air", "short_grass", "fern", "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "pink_tulip", "white_tulip",
                  "orange_tulip", "dead_bush", "red_mushroom", "brown_mushroom", "carrots", "potatoes", "beetroots", "wheat", "lantern", "chain")
            or k.endswith("_carpet") or k.endswith("_door") or k.endswith("_fence_gate") or k.endswith("_pressure_plate") or k.endswith("_sign"))


def binnen_bereik(s, c, gezien):
    return any((c[0] + dx, c[1] + dz) in gezien for dx in (-1, 0, 1) for dz in (-1, 0, 1))


# --- the game tests' floor: a field of grass with a sand rim (feature/snuffeldorp/SnuffeldorpGameTests marks its own island on it) ---
TEST_MAAT = (41, 6, 41)


def test_vloer(h):
    t = h.Structure(TEST_MAAT)
    for x in range(TEST_MAAT[0]):
        for z in range(TEST_MAAT[2]):
            rand = x < 3 or z < 3 or x >= TEST_MAAT[0] - 3 or z >= TEST_MAAT[2] - 3
            t.set(x, 0, z, "minecraft:sand" if rand else "minecraft:grass_block")
    return t


# =====================================================================================================================
# a quick look: a top-down map
# =====================================================================================================================
KLEUR = {"short_grass": (104, 168, 80), "fern": (96, 160, 76), "poppy": (220, 60, 50), "dandelion": (240, 220, 60), "cornflower": (80, 110, 230),
         "oxeye_daisy": (240, 240, 240), "azure_bluet": (220, 230, 240), "allium": (180, 110, 220), "pink_tulip": (240, 160, 200),
         "white_tulip": (236, 236, 236), "orange_tulip": (240, 150, 60), "azalea_leaves": (88, 140, 60), "flowering_azalea_leaves": (150, 130, 150),
         "grass_block": (112, 176, 86), "sand": (226, 212, 160), "dirt_path": (176, 140, 88), "water": (70, 150, 210), "stone": (130, 130, 130),
         "andesite": (140, 140, 138), "cobblestone": (124, 124, 124), "mossy_cobblestone": (110, 128, 104), "stone_bricks": (150, 150, 150),
         "spruce_planks": (124, 92, 56), "oak_leaves": (60, 130, 50), "spruce_leaves": (44, 96, 60), "birch_leaves": (96, 150, 76),
         "brick_stairs": (170, 88, 70), "bricks": (160, 84, 68), "spruce_stairs": (110, 82, 50), "dark_oak_stairs": (72, 48, 28),
         "mud_brick_stairs": (150, 118, 92), "oxidized_cut_copper_stairs": (86, 170, 140), "prismarine_brick_stairs": (100, 176, 160),
         "dark_prismarine_stairs": (50, 96, 84), "white_concrete": (236, 236, 236), "red_concrete": (170, 44, 40), "farmland": (96, 62, 36)}


def plattegrond(b, pad_):
    from PIL import Image
    img = Image.new("RGB", (SX, SZ), (40, 110, 190))
    tops = {}
    for (x, y, z), (name, _, _) in b.s.blocks.items():
        if (x, z) not in tops or y > tops[(x, z)][0]:
            tops[(x, z)] = (y, name.split(":")[1])
    for (x, z), (y, k) in tops.items():
        if y < G - 1:
            c = (60, 136, 206) if y >= G - 4 else (48, 118, 196)
        else:
            c = KLEUR.get(k)
            if c is None:
                hsh = sum(ord(ch) for ch in k)
                c = (90 + hsh % 120, 80 + (hsh * 7) % 120, 70 + (hsh * 13) % 120)
            licht = 1 + 0.035 * (y - G)
            c = tuple(max(0, min(255, int(v * licht))) for v in c)
        img.putpixel((x, z), c)
    img = img.resize((SX * 6, SZ * 6), Image.NEAREST)
    img.save(pad_)


if __name__ == "__main__":
    import os
    import sys
    import types
    sys.path.insert(0, "tools")
    import make_structures as ms_
    h_ = types.SimpleNamespace(Structure=ms_.Structure, ms=ms_)
    bb, d_, dd = eiland(h_)
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    plattegrond(bb, os.path.join(out, "plattegrond.png"))
    print(len(bb.s.blocks), "blocks;", "strand", d_["strand"], "haven", d_["haven"])
