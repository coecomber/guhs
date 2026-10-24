"""
1.7.0 "Rondje Guhmension": the 9 holes of the back 9 of the guh golf course (10-18), each a mini version of a spot in the
Guhmension. Taken over from the approved render scripts (MineScapeStory/mijnen-ontwerp/_bron/golf/g10..g18_*.py, see
mijnen-ontwerp/golf/voorstel_achterste9.md), with the render-only bits turned into the real thing:

  - the concept blocks (c_*) are the real blocks (golfbaan_ijsvilt, golfbaan_schuifguh / _schuifrail, the 1.7.0 layer
    stones and ores);
  - the "glass = the other position" of the moving obstacles became markers the Golfguh reads (GolfGame): the rail under a
    sliding guh block says its position (stand=a / b), the Kruimeldraaimolen has a golfbaan_draaias hub, the snapping teeth
    hang under a golfbaan_haptand jaw;
  - the slanted bank corners of the Kokosflipper are golfbaan_bankhoek blocks, the looping of the Wolkenhop starts at a
    golfbaan_looping block in the floor (the ring only starts two blocks up, so the lane under it stays free);
  - the signs are real signs with their own texts (golf18.SIGNS); the little guh figures of the renders are left out.

Every hole is built by bouw(P) in its own local coordinates; P is a Plot (golf18.py puts it in the 18-hole template).
"""
import math

# ---------------------------------------------------------------------------------------------------------------------
# the plot: a hole in local coordinates on a make_structures.Structure
# ---------------------------------------------------------------------------------------------------------------------
CONCEPT = {"c_ijsvilt": "golfbaan_ijsvilt", "c_ruitjesvilt": "ruitjesvilt", "c_schuifguh": "golfbaan_schuifguh",
           "c_schuifrail": "golfbaan_schuifrail", "c_viltsteen": "viltsteen", "c_viltsteen_tegels": "viltsteen_tegels",
           "c_vadserts_vilt": "vadserts_vilt", "c_vadserts_knuffel": "vadserts_knuffel", "c_vahoegiet_erts": "vahoegiet_erts",
           "c_knabbelerts_knuffel": "kaasknabbelerts_knuffel", "c_knabbelerts_vilt": "kaasknabbelerts_vilt",
           "c_pigmenterts_vilt": "pigmenterts_vilt"}
LEAVES = ("_bladeren", "_leaves", "_naalden")
SIGN_ROT = {"south": 0, "west": 4, "north": 8, "east": 12}   # a standing sign whose text faces that way


def parse(b):
    """'name[k=v,...]' -> ('ns:name', {k: v}); no namespace = guhs:, concept names become the real blocks."""
    props = {}
    if "[" in b:
        b, rest = b.split("[", 1)
        for kv in rest.rstrip("]").split(","):
            k, v = kv.split("=")
            props[k] = v
    b = CONCEPT.get(b, b)
    name = b if ":" in b else "guhs:" + b
    if name.endswith(LEAVES):
        props.setdefault("persistent", "true")
    return name, props


class Plot:
    """A hole in local coordinates: everything shifted by (ox, oy, oz). signs: [(x, y, z, facing, tag)] for golf18."""

    def __init__(self, S, ox, oy, oz, nr):
        self.S, self.o, self.nr = S, (ox, oy, oz), nr
        self.signs = []

    def set(self, x, y, z, b, keep=False):
        ox, oy, oz = self.o
        p = (ox + x, oy + y, oz + z)
        if keep and self.S.get(*p) not in (None, "minecraft:air"):
            return
        name, props = parse(b)
        self.S.set(*p, name, props)

    def get(self, x, y, z):
        ox, oy, oz = self.o
        return self.S.get(ox + x, oy + y, oz + z)

    def fill(self, x1, y1, z1, x2, y2, z2, b, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, b, keep=keep)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def tee(self, x, y, z, facing, niveau):
        self.set(x, y, z, f"golfbaan_afslag[facing={facing},hole={self.nr},niveau={niveau}]")

    def cup(self, x, y, z):
        self.set(x, y, z, f"golfbaan_hole[hole={self.nr}]")

    def bord(self, x, y, z, facing, tag):
        """A standing sign (its text comes from golf18.SIGNS[nr][tag])."""
        ox, oy, oz = self.o
        self.signs.append((ox + x, oy + y, oz + z, facing, tag))
        self.set(x, y, z, f"minecraft:cherry_sign[rotation={SIGN_ROT[facing]},waterlogged=false]")

    def bumper(self, x, y, z):
        self.set(x, y, z, "roze_slijmblok")

    def boom(self, x, y, z, h=4, r=2, log="vadshout_stam[axis=y]", leaves="vadshout_bladeren"):
        for yy in range(y, y + h):
            self.set(x, yy, z, log)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                for dy in range(-1, 2):
                    d = abs(dx) + abs(dz) + abs(dy) * 1.5
                    if d <= r + 0.6 and (dx, dz) != (0, 0):
                        self.set(x + dx, y + h + dy - 1, z + dz, leaves, keep=True)
        self.set(x, y + h, z, leaves)
        self.set(x, y + h + 1, z, leaves, keep=True)

    def grond(self, x1, z1, x2, z2, y, top="knuffelgras", onder="minecraft:dirt"):
        """Ground from y=0 up to y (the top layer 'top')."""
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                for yy in range(0, y):
                    self.set(x, yy, z, onder)
                self.set(x, y, z, top)


def cellen(*rects):
    c = set()
    for x0, z0, x1, z1 in rects:
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                c.add((x, z))
    return c


def muurcellen(cells, ook_vrij=()):
    w = set()
    for (x, z) in cells:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                n = (x + dx, z + dz)
                if n not in cells and n not in ook_vrij:
                    w.add(n)
    return w


def baan(P, cells, y, muur=1, vloer="golfbaan_vilt", stenen=("minecraft:pink_concrete", "minecraft:white_concrete"),
         voet="minecraft:white_concrete", vrij=(), open_=(), goud=True):
    """Felt at height y with little walls round it in the style of golf.py. vrij: cells where no wall may go (other
    lanes), open_: wall cells that stay open (ways through)."""
    walls = muurcellen(cells, set(vrij)) - set(open_)
    for (x, z) in walls:
        P.set(x, y, z, voet)
        top = y + muur
        hoek = goud and sum((x + dx, z + dz) in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) == 0
        for yy in range(y + 1, top + 1):
            if hoek:
                P.set(x, yy, z, "minecraft:gold_block")
            elif yy == top and (x * 7 + z * 13) % 9 == 0:
                P.set(x, yy, z, "guh_kristal_lamp")
            else:
                P.set(x, yy, z, stenen[0] if ((x + z) // 2) % 2 else stenen[1])
    for (x, z) in cells:
        P.set(x, y, z, vloer)
    return walls


def trap(blok, facing, half="bottom", shape="straight"):
    return f"{blok}[facing={facing},half={half},shape={shape},waterlogged=false]"


def kaaswiel(P, cx, y0, cz, r, dikte, blok="gatenkaas", rand="klipkaas", staand=None, snee=True):
    """A cheese wheel: flat (staand=None) or upright with its axis along 'x' or 'z'. snee = a slice cut out."""
    for a in range(-r - 1, r + 2):
        for b in range(-r - 1, r + 2):
            d = math.hypot(a, b)
            if d > r + .35:
                continue
            ang = math.degrees(math.atan2(b, a)) % 360
            if snee and 20 <= ang <= 70 and d > .8:
                continue
            bl = rand if d > r - .9 else blok
            for t in range(dikte):
                if staand is None:
                    P.set(cx + a, y0 + t, cz + b, bl)
                elif staand == "x":
                    P.set(cx + t, y0 + r + b, cz + a, bl)
                else:
                    P.set(cx + a, y0 + r + b, cz + t, bl)


# =====================================================================================================================
# 10 De Blubberslalom (Kaasmoeras): felt islands in a tub of kaassaus, narrow bridges, kaasmodder brakes the ball
# =====================================================================================================================
class G10:
    NR, ID, NAAM = 10, "blubberslalom", "De Blubberslalom"
    PARS = (3, 4, 5)
    MAAT = (26, 16, 44)
    G = 2
    BASIS = 1

    @staticmethod
    def bouw(P):
        G = G10.G
        P.grond(0, 0, 25, 43, G - 1, top="modderig_kaasgras")
        bak = cellen((5, 4, 20, 38))
        baan(P, bak, G, muur=1, vloer="kaas_saus[level=0]", stenen=("kaaskorst_stenen", "minecraft:white_concrete"))
        eil = cellen((5, 31, 20, 38), (17, 26, 18, 30), (13, 20, 20, 25), (13, 17, 14, 19), (9, 15, 14, 16), (5, 4, 13, 14))
        for (x, z) in eil:
            P.set(x, G, z, "golfbaan_vilt")
        for x in range(15, 19):                    # kaasmodder: sticky (brakes hard)
            for z in range(21, 25):
                P.set(x, G, z, "kaasmodder")
        for x in range(6, 13):
            P.set(x, G, 13, "kaasmodder")
            P.set(x, G, 12, "kaasmodder")
        for (x, z) in ((10, 6), (11, 6), (10, 7), (11, 7)):   # a puddle of saus next to the cup
            P.set(x, G, z, "kaas_saus[level=0]")
        P.set(11, G, 6, "borrelende_kaassaus")
        P.cup(8, G, 7)
        P.tee(12, G, 37, "north", "lastig")
        P.tee(17, G, 33, "north", "medium")
        P.tee(14, G, 24, "north", "makkelijk")
        for (x, z) in ((8, 27), (11, 22), (7, 19), (16, 12), (19, 9), (10, 29), (15, 17)):
            P.set(x, G + 1, z, "guh_waterlelie")
        for (x, z) in ((7, 24), (6, 22), (18, 15), (19, 16), (17, 6), (18, 5)):
            P.set(x, G, z, "kaasmodder")
            P.set(x, G + 1, z, "kaasriet")
        for (x, z) in ((2, 6), (23, 12), (2, 28), (23, 33)):
            P.boom(x, G, z, h=4, r=2, log="knotwilg_stam[axis=y]", leaves="knotwilg_bladeren")
        for (x, z) in ((1, 10), (3, 15), (24, 20), (22, 26), (1, 34), (24, 40), (3, 41), (22, 2), (2, 2)):
            P.set(x, G, z, "moerasgras")
        P.set(22, G, 37, "knuffel_kaasmoerasguh")
        P.bord(21, G + 2, 36, "east", "naam")
        P.bord(21, G + 2, 23, "east", "modder")
        P.bord(12, G + 2, 39, "south", "saus")
        P.bord(4, G + 2, 10, "south", "green")


# =====================================================================================================================
# 11 De Kristalschacht (Guhkristalmijn): a funnel under a mine tower, a 6-block shaft and a tunnel into the quarry
# =====================================================================================================================
class G11:
    NR, ID, NAAM = 11, "kristalschacht", "De Kristalschacht"
    PARS = (3, 4, 5)
    MAAT = (26, 18, 46)
    GL, GU = 3, 9
    BASIS = GL - 1

    @staticmethod
    def laag(y):
        return "c_viltsteen" if y >= 6 else "knuffelsteen" if y >= 3 else "guh_kristalsteen"

    @staticmethod
    def bouw(P):
        GL, GU = G11.GL, G11.GU
        for x in range(0, 26):
            for z in range(0, 46):
                top = GU - 1 if x <= 15 else GL - 1
                for y in range(0, top + 1):
                    P.set(x, y, z, G11.laag(y))
                if x <= 15:
                    P.set(x, GU - 1, z, "minecraft:grass_block[snowy=false]")
                else:
                    P.set(x, GL - 1, z, "c_viltsteen_tegels" if (x + z) % 7 else "guh_kristalsteen")
        for (y, z, b) in ((7, 6, "c_vadserts_vilt"), (4, 12, "c_vahoegiet_erts"), (2, 33, "c_vahoegiet_erts"),
                          (5, 38, "c_knabbelerts_knuffel"), (1, 9, "c_vahoegiet_erts"), (6, 29, "c_pigmenterts_vilt"),
                          (3, 4, "c_vadserts_knuffel"), (7, 40, "c_knabbelerts_vilt")):
            P.set(15, y, z, b)
        for (y, z) in ((4, 8), (5, 18), (3, 35), (4, 30), (6, 14)):
            P.set(16, y, z, "guh_kristal_cluster[facing=east]")
        # up top: the lane to the funnel
        boven = cellen((6, 21, 10, 41))
        baan(P, boven, GU, muur=1)
        P.tee(8, GU, 40, "north", "lastig")
        P.tee(8, GU, 34, "north", "medium")
        tx, tz = 8, 24                              # the funnel: stairs sloping down to the hole
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) == (0, 0):
                    continue
                f = "east" if dx > 0 else "west" if dx < 0 else ("south" if dz > 0 else "north")
                P.set(tx + dx, GU, tz + dz, trap("gladde_knuffelsteen_trap", f))
        for y in range(GL + 1, GU + 1):            # the shaft
            P.set(tx, y, tz, "minecraft:air")
        for (x, z) in ((5, 21), (11, 21), (5, 27), (11, 27)):   # the mine tower over the funnel
            for y in range(GU + 1, GU + 6):
                P.set(x, y, z, "minecraft:stripped_cherry_log[axis=y]")
        for x in range(5, 12):
            P.set(x, GU + 6, 21, "minecraft:stripped_cherry_log[axis=x]")
            P.set(x, GU + 6, 27, "minecraft:stripped_cherry_log[axis=x]")
        for z in range(21, 28):
            P.set(5, GU + 6, z, "minecraft:stripped_cherry_log[axis=z]")
            P.set(11, GU + 6, z, "minecraft:stripped_cherry_log[axis=z]")
        for x in range(5, 12):
            P.set(x, GU + 6, 24, "minecraft:stripped_cherry_log[axis=x]")
        for (x, z) in ((5, 21), (11, 21), (5, 27), (11, 27)):
            P.set(x, GU + 7, z, "guh_kristal_cluster[facing=up]")
        P.set(8, GU + 7, 24, "guh_kristal_blok")
        P.set(8, GU + 8, 24, "guh_kristal_cluster[facing=up]")
        P.set(8, GU + 5, 24, "minecraft:iron_chain[axis=y]")
        P.set(8, GU + 4, 24, "guh_kristal_lamp")
        # the tunnel from the bottom of the shaft to the cliff face
        P.set(tx, GL, tz, trap("gladde_knuffelsteen_trap", "west"))   # a slope: rolls east
        for x in range(tx + 1, 16):
            P.set(x, GL, tz, "golfbaan_vilt")
            P.set(x, GL + 1, tz, "minecraft:air")
        P.set(15, GL + 2, tz, "guh_kristal_lamp")
        # down below: the quarry lane along the cliff
        beneden = cellen((16, 3, 20, 30))
        baan(P, beneden, GL, muur=1, vrij={(15, z) for z in range(0, 46)})
        P.set(16, GL, tz, "golfbaan_vilt")
        for (x, z) in ((17, 16), (19, 12), (18, 8)):          # crystal pillars
            P.set(x, GL + 1, z, "guh_kristal_blok")
            P.set(x, GL + 2, z, "guh_kristal_blok")
            P.set(x, GL + 3, z, "guh_kristal_cluster[facing=up]")
        P.cup(17, GL, 5)
        P.tee(18, GL, 22, "north", "makkelijk")
        for i, x in enumerate(range(9, 16)):                  # stairs from the top down (south side)
            y = GU - 1 - i
            for z in (43, 44):
                P.set(x, y, z, trap("knuffelsteen_trap", "west"))
                for yy in range(y + 1, GU + 1):
                    P.set(x, yy, z, "minecraft:air")
        for z in (43, 44):
            for x in range(16, 22):
                P.set(x, GL - 1, z, "c_viltsteen_tegels")
        for z in range(29, 42):
            P.set(13, GU, z, "minecraft:rail[shape=north_south,waterlogged=false]")
        for (x, z) in ((23, 6), (23, 18), (23, 28)):
            P.set(x, GL, z, "guh_lantaarnpaal[half=lower]")
            P.set(x, GL + 1, z, "guh_lantaarnpaal[half=upper]")
        for (x, z) in ((22, 12), (24, 24), (22, 33), (21, 40)):
            P.set(x, GL, z, "guh_kristal_cluster[facing=up]")
        P.bord(11, GU + 2, 36, "east", "naam")
        P.bord(12, GU + 1, 25, "east", "trechter")
        P.bord(21, GL + 2, 24, "east", "beneden")
        P.bord(16, GU, 33, "east", "lagen")


# =====================================================================================================================
# 12 De Schuifvadsjes (Vadswoud): two rows of sliding guh blocks, vads trees on the lane
# =====================================================================================================================
class G12:
    NR, ID, NAAM = 12, "schuifvadsjes", "De Schuifvadsjes"
    PARS = (3, 4, 5)
    MAAT = (24, 16, 44)
    G = 2
    BASIS = G - 1
    # per row: z, the x's of position A (a block stands there at the start), the x's of position B
    RIJEN = ((22, (7, 8, 12, 13), (9, 10, 14, 15)), (11, (9, 10, 11, 15, 16), (6, 7, 12, 13, 14)))

    @staticmethod
    def bouw(P):
        G = G12.G
        P.grond(0, 0, 23, 43, G - 1, top="vadsmos")
        lane = cellen((6, 4, 16, 39))
        baan(P, lane, G, muur=1, stenen=("vadshout_planken", "minecraft:white_concrete"))
        for (x, z) in ((9, 30), (14, 27), (8, 18)):           # trees ON the lane (their trunks are obstacles)
            P.boom(x, G + 1, z, h=5, r=1)
            P.set(x, G, z, "golfbaan_vilt")
        for z, a, b in G12.RIJEN:                             # the rails, and the blocks in position A
            for x in range(6, 17):
                stand = "a" if x in a else "b" if x in b else "geen"
                P.set(x, G, z, f"c_schuifrail[stand={stand}]")
            for x in a:
                P.set(x, G + 1, z, "c_schuifguh")
            P.set(5, G + 1, z, "minecraft:gold_block")
            P.set(17, G + 1, z, "minecraft:gold_block")
        P.cup(11, G, 6)
        P.tee(11, G, 38, "north", "lastig")
        P.tee(13, G, 33, "north", "medium")
        P.tee(11, G, 16, "north", "makkelijk")
        for (x, z) in ((2, 4), (2, 14), (3, 24), (2, 34), (20, 8), (21, 19), (20, 30), (21, 40), (2, 41)):
            P.boom(x, G, z, h=4 + (x + z) % 3, r=2)
        P.set(20, G, 25, "vadsmolen[facing=west]")
        for (x, z) in ((4, 9), (19, 14), (4, 30), (19, 36), (22, 2), (1, 20)):
            P.set(x, G, z, "knabbelroos")
        P.bord(17, G + 2, 35, "east", "naam")
        P.bord(17, G + 2, 24, "east", "schuif")
        P.bord(12, G + 2, 40, "south", "bomen")


# =====================================================================================================================
# 13 De Guhpiek (Guh Peaks): up a slope to a top of ijsvilt; a gap in the wall behind the cup
# =====================================================================================================================
class G13:
    NR, ID, NAAM = 13, "guhpiek", "De Guhpiek"
    PARS = (2, 4, 5)
    MAAT = (24, 18, 44)
    G = 2
    GT = G + 3
    BASIS = G - 1
    SNEEUW = "minecraft:snow_block"

    @staticmethod
    def piek(P, cx, cz, h, r, y0):
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                for y in range(y0, y0 + h):
                    if d <= r * (1 - (y - y0) / h) + 0.3:
                        P.set(x, y, z, G13.SNEEUW if (y - y0) > h * 0.35 or (x + z + y) % 4 else "minecraft:packed_ice", keep=True)

    @staticmethod
    def bouw(P):
        G, GT, SNEEUW = G13.G, G13.GT, G13.SNEEUW
        P.grond(0, 0, 23, 43, G - 1, top=SNEEUW)
        P.fill(5, 0, 3, 17, GT - 1, 23, SNEEUW)
        for (x, z) in ((5, 3), (17, 3), (5, 23), (17, 23)):
            P.set(x, GT - 1, z, "minecraft:air")
        beneden = cellen((9, 30, 13, 41))
        baan(P, beneden, G, muur=1, open_={(x, 29) for x in range(9, 14)})
        for x in range(9, 14):
            for z in range(31, 34):
                P.set(x, G, z, "c_ijsvilt")
        for x in range(9, 14):                                # the slope: slab, stairs, block (3 steps up)
            for i in range(3):
                z = 29 - 3 * i
                y = G + 1 + i
                P.fill(x, G, z - 2, x, y - 1, z, SNEEUW)
                P.set(x, y, z, "minecraft:quartz_slab[type=bottom,waterlogged=false]")
                P.set(x, y, z - 1, trap("minecraft:quartz_stairs", "north"))
                P.set(x, y, z - 2, "golfbaan_vilt" if i < 2 else "c_ijsvilt")
        for z in range(21, 30):
            top = G + 1 + (29 - z) // 3
            for x in (8, 14):
                P.fill(x, G, z, x, top, z, "minecraft:white_concrete")
                P.set(x, top + 1, z, "minecraft:pink_concrete" if ((x + z) // 2) % 2 else "minecraft:white_concrete")
        top = cellen((7, 6, 15, 20))
        baan(P, top, GT, muur=1, vloer="c_ijsvilt", vrij=cellen((9, 21, 13, 29)), open_={(x, 5) for x in range(10, 13)})
        for x in range(10, 13):
            P.set(x, GT, 5, "minecraft:air")
            P.set(x, GT - 1, 4, "minecraft:air")
            P.set(x, GT - 1, 5, "minecraft:air")
        P.cup(11, GT, 8)
        P.tee(11, G, 40, "north", "lastig")
        P.tee(11, G, 36, "north", "medium")
        P.tee(11, GT, 19, "north", "makkelijk")
        G13.piek(P, 2, 12, 9, 3, G)
        G13.piek(P, 20, 16, 11, 3, G)
        G13.piek(P, 3, 25, 6, 3, G)
        G13.piek(P, 20, 4, 7, 3, G)
        for (x, z) in ((3, 33), (20, 30), (19, 39), (2, 40), (21, 24)):
            P.boom(x, G, z, h=5, r=1, log="sneeuwguhspar_stam[axis=y]", leaves="sneeuwguhspar_naalden[sneeuw=true]")
        P.set(16, GT, 18, "sneeuwpopguh[facing=south,half=lower]")
        P.set(16, GT + 1, 18, "sneeuwpopguh[facing=south,half=upper]")
        P.set(6, GT, 12, "ijspegelguh_kristal[facing=up]")
        P.set(16, GT, 10, "ijspegelguh_kristal[facing=up]")
        P.set(15, G, 38, "knuffel_snow[facing=west]")
        P.bord(14, G + 2, 37, "east", "naam")
        P.bord(16, GT + 1, 14, "east", "ijs")
        P.bord(13, GT + 1, 5, "south", "gat")


# =====================================================================================================================
# 14 Het Gatenkaasgokje (Gatenkaasheuvels): three little holes on a cheese hill, three tubes to the green
# =====================================================================================================================
class G14:
    NR, ID, NAAM = 14, "gatenkaasgokje", "Het Gatenkaasgokje"
    PARS = (2, 4, 5)
    MAAT = (26, 18, 46)
    G = 2
    GT = G + 3
    BASIS = G - 1
    GATEN = (8, 12, 16)

    @staticmethod
    def bouw(P):
        G, GT = G14.G, G14.GT
        P.grond(0, 0, 25, 45, G - 1, top="korstgras")
        green = cellen((3, 34, 21, 44))
        baan(P, green, G, muur=1)
        for x in range(6, 10):
            for z in range(36, 39):
                P.set(x, G, z, "kaas_saus[level=0]")
        for x in range(15, 19):
            for z in range(35, 38):
                P.set(x, G, z, "kaasmodder")
        P.cup(13, G, 40)
        aanloop = cellen((9, 3, 15, 13))
        baan(P, aanloop, G, muur=1, open_={(x, 14) for x in range(9, 16)})
        for x in range(2, 23):                     # the hill
            for z in range(12, 34):
                dx = abs(x - 12) / 10.5
                h = G + 7 - 3 * dx * dx - (max(0, 24 - z) * 0.55)
                for y in range(0, int(h) + 1):
                    P.set(x, y, z, "gatenkaas")
        for x in range(6, 19):                     # the bowl the lane lies in on top
            for z in range(22, 30):
                for y in range(GT + 1, G + 10):
                    P.set(x, y, z, "minecraft:air")
        for x in range(8, 17):                     # and the gully of the slope
            for z in range(12, 22):
                for y in range(G + 1 + (z - 14) // 3 + 1, G + 10):
                    P.set(x, y, z, "minecraft:air")
        for x in range(9, 16):
            for i in range(3):
                z = 14 + 3 * i
                y = G + 1 + i
                P.fill(x, G, z, x, y - 1, z + 2, "gatenkaas")
                P.set(x, y, z, "minecraft:smooth_sandstone_slab[type=bottom,waterlogged=false]")
                P.set(x, y, z + 1, trap("minecraft:smooth_sandstone_stairs", "south"))
                P.set(x, y, z + 2, "golfbaan_vilt")
                for yy in range(y + 1, GT + 3):
                    for zz in (z, z + 1, z + 2):
                        P.set(x, yy, zz, "minecraft:air")
        for z in range(14, 23):
            top = G + 1 + (z - 14) // 3
            for x in (8, 16):
                P.fill(x, G, z, x, top, z, "klipkaas")
                P.set(x, top + 1, z, "gatenkaas_stenen")
        top = cellen((7, 23, 17, 28))
        baan(P, top, GT, muur=1, stenen=("gatenkaas_stenen", "klipkaas"), voet="klipkaas", vrij=cellen((9, 14, 15, 22)))
        for hx in G14.GATEN:                       # three little holes, each a tube to the green
            P.set(hx, GT, 26, "minecraft:air")
            P.set(hx, G + 2, 26, "minecraft:air")
            for z in range(26, 34):
                fy = G + 1 if z < 30 else G
                P.set(hx, fy, z, trap("minecraft:smooth_sandstone_stairs", "north"))
                for yy in range(fy + 1, G + 3):
                    P.set(hx, yy, z, "minecraft:air")
            P.set(hx, G, 34, "golfbaan_vilt")
            P.set(hx, G + 2, 33, "klipkaas")
            P.set(hx, G + 3, 33, "guh_kristal_lamp")
        for (cx, cy, cz, r) in ((4, 2, 18, 1.4), (20, 2, 16, 1.6), (19, 3, 28, 1.3), (5, 3, 29, 1.5), (21, 1, 23, 1.2),
                                (6, 1, 13, 1.2), (18, 1, 32, 1.0), (11, 1, 33, 0.9)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                for y in range(int(cy - r - 1), int(cy + r + 2)):
                    for z in range(int(cz - r - 1), int(cz + r + 2)):
                        if math.dist((x, y, z), (cx, cy, cz)) <= r and P.get(x, y, z) == "guhs:gatenkaas" and y >= G:
                            P.set(x, y, z, "minecraft:air")
        P.tee(12, G, 4, "south", "lastig")
        P.tee(12, G, 9, "south", "medium")
        P.tee(19, G, 42, "west", "makkelijk")
        kaaswiel(P, 23, G, 40, 2, 1)
        kaaswiel(P, 1, G, 25, 2, 1, staand="z")
        kaaswiel(P, 22, G, 6, 2, 2)
        for (x, z) in ((2, 38), (23, 34), (4, 6), (20, 11), (1, 44), (24, 44)):
            P.set(x, G, z, "kaasbloem")
        P.bord(16, G + 2, 6, "east", "naam")
        P.bord(18, GT + 1, 27, "east", "gaatjes")
        P.bord(5, G + 2, 39, "south", "saus")
        P.bord(22, G + 2, 36, "east", "modder")


# =====================================================================================================================
# 15 De Kruimeldraaimolen (Knabbel Crumbs): a round square with four turning arms round a knabbel tower
# =====================================================================================================================
class G15:
    NR, ID, NAAM = 15, "kruimeldraaimolen", "De Kruimeldraaimolen"
    PARS = (3, 4, 5)
    MAAT = (28, 16, 44)
    G = 2
    BASIS = G - 1
    C = (13, 18)

    @staticmethod
    def bouw(P):
        G = G15.G
        cx, cz = G15.C
        P.grond(0, 0, 27, 43, G - 1, top="kruimelzand")
        plein = {(x, z) for x in range(0, 28) for z in range(0, 44) if math.hypot(x - cx, z - cz) <= 9.4}
        cells = plein | cellen((11, 27, 15, 41), (12, 3, 14, 9))
        baan(P, cells, G, muur=1, stenen=("kruimelrots_stenen", "minecraft:white_concrete"), goud=False)
        P.set(cx, G + 1, cz, "block_of_kaasknabbels")   # the knabbel tower in the middle, the hub on top of its foot
        P.set(cx, G + 2, cz, "golfbaan_draaias")
        P.set(cx, G + 3, cz, "block_of_kaasknabbels")
        P.set(cx, G + 4, cz, "block_of_kaasknabbels")
        P.set(cx, G + 5, cz, "roze_guh_koek[facing=south,koeken=3]")
        for k in range(1, 7):                      # the four arms as a "+" to start with (GolfGame turns them)
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                P.set(cx + dx * k, G + 1, cz + dz * k, "golfbaan_wiek")
        for (x, z) in ((6, 13), (20, 13), (6, 23), (20, 23), (9, 10), (17, 10)):
            P.bumper(x, G + 1, z)
        P.cup(13, G, 5)
        P.tee(13, G, 40, "north", "lastig")
        P.tee(13, G, 35, "north", "medium")
        P.tee(20, G, 17, "west", "makkelijk")
        for (x, z, h) in ((2, 6, 3), (24, 8, 2), (1, 30, 2), (25, 33, 3), (3, 40, 1), (24, 41, 2), (20, 2, 2)):
            P.fill(x, G, z, x + 1, G + h - 1, z + 1, "kruimelrots")
            P.set(x, G + h, z, "kruimelrots_stenen")
        for (x, z) in ((5, 34), (21, 30), (4, 2), (8, 36), (19, 37), (25, 20), (1, 18)):
            P.set(x, G, z, "kaasknabbelplant[age=2]")
        P.set(22, G, 26, "roze_guh_koek[facing=west,koeken=2]")
        P.bord(16, G + 2, 37, "east", "naam")
        P.bord(16, G + 2, 28, "east", "wachten")
        P.bord(15, G + 2, 8, "east", "kruimels")


# =====================================================================================================================
# 16 De Kokosflipper (Guhwaii): a pinball table on an island: launch lane, slanted bank corners, coconut bumpers
# =====================================================================================================================
class G16:
    NR, ID, NAAM = 16, "kokosflipper", "De Kokosflipper"
    PARS = (3, 4, 5)
    MAAT = (28, 16, 46)
    G = 2
    BASIS = G - 1
    PALM = "guhwaii_palm_planken"

    @staticmethod
    def palm(P, x, y, z, h):
        for k in range(h):
            P.set(x + (1 if k >= h - 2 and h > 4 else 0), y + k, z, "guhwaii_palm_stam[axis=y]")
        tx = x + (1 if h > 4 else 0)
        top = y + h
        P.set(tx, top, z, "guhwaii_palm_blad")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            P.set(tx + dx, top, z + dz, "guhwaii_palm_blad")
            if dx == 0 or dz == 0:
                P.set(tx + 2 * dx, top, z + 2 * dz, "guhwaii_palm_blad")
                P.set(tx + 3 * dx, top - 1, z + 3 * dz, "guhwaii_palm_blad")
        P.set(tx, top - 1, z + 1, "kokosnoot[hangend=true,rijp=1]")

    @staticmethod
    def bouw(P):
        G, PALM = G16.G, G16.PALM
        for x in range(28):
            for z in range(46):
                P.set(x, 0, z, "minecraft:sand")
                eiland = (3 <= x <= 23 and 3 <= z <= 34) or (14 <= x <= 22 and 34 <= z <= 44)
                P.set(x, 1, z, "minecraft:sand" if eiland else "minecraft:water[level=0]")
        tafel = cellen((7, 6, 19, 8), (7, 9, 15, 30), (17, 9, 19, 43))
        baan(P, tafel, G, muur=1, stenen=(PALM, "minecraft:white_concrete"), open_={(x, 31) for x in range(10, 13)})
        for x in range(10, 13):                    # the drain at the bottom: a gutter to the lagoon
            for z in (31, 32, 33, 34, 35):
                P.set(x, 1, z, "minecraft:water[level=0]")
                P.set(x, G, z, "minecraft:air")
        # the slanted bank corners at the top (a 45 degree bank wall: the ball bounces off at an angle) and two on the table
        P.set(19, G, 6, "minecraft:white_concrete")
        P.set(19, G + 1, 6, PALM)
        P.set(18, G + 1, 6, "golfbaan_bankhoek[facing=north]")     # (north-east corner)
        P.set(19, G + 1, 7, "golfbaan_bankhoek[facing=north]")
        P.set(7, G, 6, "minecraft:white_concrete")
        P.set(7, G + 1, 6, PALM)
        P.set(8, G + 1, 6, "golfbaan_bankhoek[facing=west]")       # (north-west corner)
        P.set(7, G + 1, 7, "golfbaan_bankhoek[facing=west]")
        P.set(14, G + 1, 20, "golfbaan_bankhoek[facing=east]")      # a "/" on the table: sends the ball west, to the cup
        P.set(13, G + 1, 21, "golfbaan_bankhoek[facing=east]")
        P.set(8, G + 1, 12, "golfbaan_bankhoek[facing=south]")      # a "\" by the left wall: sends it back east
        P.set(9, G + 1, 13, "golfbaan_bankhoek[facing=south]")
        for (x, z) in ((11, 12), (14, 11), (12, 16), (9, 18), (11, 24), (7, 22)):   # coconut bumpers
            P.bumper(x, G + 1, z)
            P.set(x, G + 2, z, "kokosnoot[hangend=false,rijp=1]")
        P.cup(9, G, 26)
        P.tee(18, G, 42, "north", "lastig")
        P.tee(18, G, 36, "north", "medium")
        P.tee(13, G, 28, "north", "makkelijk")
        for (x, z, h) in ((4, 5, 6), (22, 12, 5), (4, 20, 5), (22, 28, 6), (5, 32, 4), (24, 40, 5), (2, 14, 4)):
            G16.palm(P, x, G, z, h)
        for (x, z) in ((6, 34), (16, 34), (21, 44), (21, 4), (4, 10)):
            P.set(x, G, z, "tiki_fakkel")
        P.set(22, G, 20, "tiki_beeld[facing=west]")
        P.set(22, G, 34, "tiki_surfplankrek[facing=west]")
        for (x, z) in ((5, 26), (21, 24), (5, 16), (20, 38)):
            P.set(x, G, z, "guhwaii_orchidee")
        P.bord(20, G + 2, 39, "east", "naam")
        P.bord(20, G + 2, 9, "east", "bank")
        P.bord(13, G + 2, 31, "south", "putje")


# =====================================================================================================================
# 17 De Wolkenhop (Pink Puffs): three cloud islands, two slime launch pads, a vahoeg looping
# =====================================================================================================================
class G17:
    NR, ID, NAAM = 17, "wolkenhop", "De Wolkenhop"
    PARS = (3, 5, 6)
    MAAT = (28, 22, 46)
    GC = 9
    BASIS = 1
    WOLK = ("wolkenblok_roze", "wolkenblok_wit")
    LOOP = (13, 18)                   # the looping's floor block (x, z) on island 2; the ball goes north

    @staticmethod
    def blob(P, cx, cy, cz, rx, ry, rz, onderkant=True):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    wobbel = 0.12 * math.sin(x * 1.7 + z * 0.9) + 0.1 * math.cos(z * 1.3 - y)
                    if d <= 1 + wobbel and (not onderkant or y < G17.GC):
                        P.set(x, y, z, G17.WOLK[(x + 2 * y + z) % 5 == 0], keep=True)

    @staticmethod
    def bouw(P):
        GC, WOLK = G17.GC, G17.WOLK
        for (cx, cz, r) in ((4, 8, 4), (22, 14, 5), (6, 30, 5), (21, 36, 4), (13, 24, 3), (24, 2, 3), (3, 43, 3)):
            G17.blob(P, cx, 0.5, cz, r, 1.3, r * 0.8, onderkant=False)
        e1 = cellen((11, 30, 15, 42))                         # island 1 (the tees) with a launch pad
        G17.blob(P, 13, GC - 2, 36, 7, 3.2, 8)
        baan(P, e1, GC, muur=1, stenen=WOLK, open_={(x, 29) for x in range(10, 17)})
        for x in range(11, 16):
            P.set(x, GC, 30, "roze_slijmblok")
        e2 = cellen((11, 12, 15, 23))                         # island 2 with the looping and another launch pad
        G17.blob(P, 13, GC - 2, 17, 6.5, 3, 7)
        baan(P, e2, GC, muur=1, stenen=WOLK, open_={(x, 11) for x in range(10, 17)} | {(x, 24) for x in range(10, 17)})
        lc = (GC + 3.2, 18)
        for y in range(GC + 2, GC + 8):                       # the ring (from two blocks up: the lane under it stays free)
            for z in range(13, 24):
                d = math.hypot(y - lc[0], z - lc[1])
                if 2.6 <= d <= 3.6:
                    for x in (12, 13, 14):
                        P.set(x, y, z, "golfbaan_vilt")
                    for x in (11, 15):
                        P.set(x, y, z, "minecraft:pink_concrete" if (y + z) % 2 else "minecraft:white_concrete")
        for x in range(12, 15):
            P.set(x, GC, G17.LOOP[1], "golfbaan_looping[facing=north]")
        P.set(13, GC + 8, 18, "minecraft:gold_block")
        P.set(13, GC + 9, 18, "wolkenlamp")
        for x in range(11, 16):
            P.set(x, GC, 12, "roze_slijmblok")
        e3 = cellen((9, 1, 17, 7))                            # island 3: the green with a high cloud wall behind it
        G17.blob(P, 13, GC - 2, 4, 6.5, 3, 5)
        baan(P, e3, GC, muur=2, stenen=WOLK, open_={(x, 8) for x in range(8, 19)})
        P.cup(13, GC, 3)
        P.tee(13, GC, 41, "north", "lastig")
        P.tee(13, GC, 35, "north", "medium")
        P.tee(13, GC, 14, "north", "makkelijk")
        for (x, z, k) in ((9, 33, 0), (17, 38, 1), (9, 15, 2), (17, 20, 3), (8, 5, 1), (18, 2, 0)):
            P.set(x, GC, z, f"bloemwolk[kleur={k}]")
        for (x, z) in ((17, 32), (17, 13), (8, 1)):
            P.set(x, GC, z, "wolkenlamp")
        P.set(9, GC, 40, "knuffel_wolk[facing=east]")
        P.bord(16, GC + 2, 37, "east", "naam")
        P.bord(16, GC + 2, 31, "east", "plank")
        P.bord(16, GC + 2, 22, "east", "looping")


# =====================================================================================================================
# 18 De Grote Guhmond (the finale): a giant guh head with snapping teeth, a tongue, a throat funnel and a stomach
# =====================================================================================================================
class G18:
    NR, ID, NAAM = 18, "grote_guhmond", "De Grote Guhmond"
    PARS = (3, 5, 6)
    MAAT = (30, 28, 50)
    G = 6
    BASIS = G - 1
    HC = (15, G + 6, 16)
    R = 9.5
    YM = 1
    TANDEN = range(12, 19)            # the snapping teeth (x) at z = 24: their jaws hang at G + 4

    @staticmethod
    def in_hoofd(x, y, z):
        return math.dist((x, (y - G18.HC[1]) * 1.08 + G18.HC[1], z), G18.HC) <= G18.R

    @staticmethod
    def bouw(P):
        G, YM, MAATY = G18.G, G18.YM, G18.MAAT[1]
        P.grond(0, 0, 29, 49, G - 1, top="minecraft:grass_block[snowy=false]")
        for x in range(3, 28):
            for y in range(G, MAATY):
                for z in range(4, 28):
                    if G18.in_hoofd(x, y, z):
                        P.set(x, y, z, "minecraft:pink_wool")
        for ex in (8, 22):                          # ears
            for x in range(ex - 2, ex + 3):
                for y in range(G + 13, G + 18):
                    for z in range(13, 17):
                        if math.dist((x * 1.0, y * 0.8, z * 1.6), (ex, (G + 13) * 0.8, 14.5 * 1.6)) <= 2.6:
                            P.set(x, y, z, "minecraft:magenta_concrete" if abs(x - ex) <= 0 and z == 16 else "minecraft:pink_wool")
        voor = {}                                   # the face: on the frontmost block of the head per (x, y)
        for x in range(3, 28):
            for y in range(G, MAATY):
                for z in range(27, 3, -1):
                    if G18.in_hoofd(x, y, z):
                        voor[(x, y)] = z
                        break
        for (x, y) in [(x, y) for x in (11, 12) for y in (G + 8, G + 9, G + 10)] + [(x, y) for x in (18, 19) for y in (G + 8, G + 9, G + 10)]:
            if (x, y) in voor:
                P.set(x, y, voor[(x, y)], "minecraft:white_concrete" if (x, y) in ((11, G + 10), (18, G + 10)) else "minecraft:black_concrete")
        for (x, y) in ((9, G + 7), (10, G + 7), (20, G + 7), (21, G + 7)):
            P.set(x, y, voor[(x, y)], "minecraft:magenta_concrete")
        P.set(15, G + 7, voor[(15, G + 7)], "minecraft:pink_terracotta")
        for x in range(12, 19):                     # the mouth with the tongue
            for z in range(18, 28):
                for y in range(G + 1, G + 5):
                    P.set(x, y, z, "minecraft:air")
                P.set(x, G, z, "tong")
        for x in range(11, 20):
            if (x, G + 5) in voor:
                P.set(x, G + 5, voor[(x, G + 5)], "minecraft:magenta_terracotta")
        for x in (13, 17):
            P.set(x, G + 4, voor[(x, G + 5)], "tand")
        for x in G18.TANDEN:                         # the snapping teeth: a jaw at G + 4, the tooth down to the tongue (closed)
            P.set(x, G + 4, 24, "golfbaan_haptand")
            for y in (G + 1, G + 2, G + 3):
                P.set(x, y, 24, "tand")
        tx, tz = 15, 20                             # the throat funnel at the back of the mouth
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) != (0, 0):
                    f = "east" if dx > 0 else "west" if dx < 0 else ("south" if dz > 0 else "north")
                    P.set(tx + dx, G, tz + dz, trap("minecraft:cherry_stairs", f))
        for y in range(YM + 1, G + 1):
            P.set(tx, y, tz, "minecraft:air")
        P.set(15, G + 3, 21, "guh_kristal_lamp")
        for x in range(7, 24):                      # the stomach below
            for z in range(7, 26):
                for y in range(YM, G):
                    P.set(x, y, z, "maagwand")
        maag = cellen((10, 9, 20, 22))
        for x in range(9, 22):
            for z in range(8, 24):
                for y in range(YM + 1, YM + 4):
                    P.set(x, y, z, "minecraft:air")
        baan(P, maag, YM, muur=1, stenen=("maagwand", "maagbodem"), voet="maagwand")
        P.set(tx, YM, tz, trap("minecraft:cherry_stairs", "south"))
        for x in range(14, 17):
            for z in range(14, 17):
                P.set(x, YM, z, "maagzuur")
        for (x, z) in ((12, 16), (18, 13), (17, 19)):
            P.bumper(x, YM + 1, z)
        P.cup(12, YM, 11)
        for (x, z) in ((10, 9), (20, 9), (10, 22), (20, 22)):
            P.set(x, YM + 3, z, "guh_kristal_lamp")
        for i in range(G - YM):                     # stairs down to the stomach (east side)
            z = 18 - i
            P.set(23, YM + i, z, trap("minecraft:cherry_stairs", "south"))
            for yy in range(YM + i + 1, G + 3):
                P.set(23, yy, z, "minecraft:air")
        aan = cellen((12, 28, 18, 48))               # the run-up outside
        baan(P, aan, G, muur=1, open_={(x, 27) for x in range(11, 20)})
        for x in range(12, 19):
            for z in (38, 39):
                P.set(x, G, z, "kaasmodder")
        for (x, z) in ((13, 33), (17, 33), (15, 31)):
            P.bumper(x, G + 1, z)
        P.tee(15, G, 47, "north", "lastig")
        P.tee(15, G, 42, "north", "medium")
        P.tee(15, G, 29, "north", "makkelijk")
        for z in range(30, 48, 4):
            P.set(10, G, z, "lampion_roze[hanging=false,waterlogged=false]")
            P.set(20, G, z, "lampion_geel[hanging=false,waterlogged=false]")
        P.bord(19, G + 2, 44, "east", "naam")
        P.bord(19, G + 2, 30, "east", "tanden")
        P.bord(21, YM + 2, 12, "west", "maag")
        P.bord(22, G, 45, "east", "trofee")


HOLES = [G10, G11, G12, G13, G14, G15, G16, G17, G18]
