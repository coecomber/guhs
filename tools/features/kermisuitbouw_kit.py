"""
1.8.1: the attractions and stalls of the kermis extension, ported from the render kit (kermis-ontwerp/_bron/kermiskit_modules.py).
T is a Plan (tools/features/kermisuitbouw.py): world coordinates of kermis A, G = the floor. Only what stands still is here;
what moves (the Booster's arm, the wheel of the Reuzenrad, the seats of the Zweefmolen, the arms of the Guhtwister, the bumper
cars, the ride of the Spookguhhuis) belongs to the guhs:kermisrit_* blocks, the anchors of which are set here.
"""
import math
import numpy as np


def stairs(block, facing, half="bottom", shape="straight"):
    return f"{block}[facing={facing},half={half},shape={shape},waterlogged=false]"


def slab(block, typ="bottom"):
    return f"{block}[type={typ},waterlogged=false]"


TEGENOVER = {"north": "south", "south": "north", "east": "west", "west": "east"}
ROZE, CREME, ROOD, GEEL, BLAUW, GROEN = ("campingmarkt_tentdoek_" + c for c in ("rood", "creme", "rood", "geel", "blauw", "groen"))


def rit(T, soort, x, y, z, f):
    """The anchor of a ride, in the floor under its middle: the block does the rest (without the ride slices it is air)."""
    T.set(x, y, z, f"kermisrit_{soort}[facing={f}]")


def kraamblok(T, soort, x, y, z, f):
    T.set(x, y, z, f"kermiskraam_{soort}[facing={f}]")


class Lok:
    """Lokaal assenstelsel: a = breedte, h = hoogte boven G, b = naar achteren; f = richting waar de voorkant heen wijst."""
    VEC = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}

    def __init__(self, T, x, y, z, f):
        self.T, self.x, self.y, self.z, self.f = T, x, y, z, f
        self.F = self.VEC[f]
        self.B = (-self.F[0], -self.F[1])
        self.R = (-self.F[1], self.F[0])

    def w(self, a, h, b):
        return (self.x + a * self.R[0] + b * self.B[0], self.y + h, self.z + a * self.R[1] + b * self.B[1])

    def set(self, a, h, b, s, roof=False):
        self.T.set(*self.w(a, h, b), s, roof=roof)

    def fill(self, a1, h1, b1, a2, h2, b2, s, roof=False):
        for a in range(min(a1, a2), max(a1, a2) + 1):
            for h in range(min(h1, h2), max(h1, h2) + 1):
                for b in range(min(b1, b2), max(b1, b2) + 1):
                    self.set(a, h, b, s, roof)

    def guh(self, a, h, b, skin="guh", sitting=False):
        x, y, z = self.w(a, h, b)
        self.T.guh(x + 0.5, y, z + 0.5, {"north": 0, "east": 90, "south": 180, "west": 270}[self.f], skin=skin, sitting=sitting)


def bal(T, cx, cy, cz, rx, ry, rz, s, roof=False):
    for x in range(int(cx - rx), int(cx + rx) + 1):
        for y in range(int(cy - ry), int(cy + ry) + 1):
            for z in range(int(cz - rz), int(cz + rz) + 1):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1:
                    T.set(x, y, z, s, roof=roof)


def lijn(T, p, q, s):
    n = int(max(abs(q[i] - p[i]) for i in range(3))) + 1
    for i in range(n + 1):
        t = i / n
        T.set(*(round(p[j] + (q[j] - p[j]) * t) for j in range(3)), s)


def pad(T, x1, z1, x2, z2, G):
    """Geblokt kermispad."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            T.set(x, G, z, "minecraft:white_concrete" if (x // 2 + z // 2) % 2 else "minecraft:pink_concrete")


def lantaarn(T, x, z, G):
    T.set(x, G + 1, z, "guh_lantaarnpaal[half=lower]")
    T.set(x, G + 2, z, "guh_lantaarnpaal[half=upper]")


# ------------------------------------------------------------------ kraampjes
def kraam(T, x, z, f, G, soort, doek=ROOD, doek2=CREME):
    """Marktkraam 7 breed, 4 diep; (x, z) = midden van de toonbank."""
    L = Lok(T, x, G, z, f)
    L.fill(-3, 0, 0, 3, 0, 3, "minecraft:birch_planks")
    for a in (-3, 3):
        for b in (0, 3):
            L.fill(a, 1, b, a, 3, b, "minecraft:birch_fence")
    L.fill(-2, 1, 0, 2, 1, 0, "pleister_boter")
    L.fill(-3, 1, 3, 3, 3, 3, doek2)
    L.fill(-3, 1, 1, -3, 2, 2, doek)
    L.fill(3, 1, 1, 3, 2, 2, doek)
    for a in range(-4, 5):
        d = doek if (a + 4) % 2 == 0 else doek2
        for i, b in enumerate(range(-2, 5)):
            typ = "bottom" if i % 2 == 0 else "top"
            L.set(a, 4 + i // 2 - 1 + (1 if i >= 0 else 0), b, slab(d + "_plaat", typ), roof=True)
    L.set(-2, 3, -1, "lampion_roze[hanging=true]")
    L.set(2, 3, -1, "lampion_geel[hanging=true]")
    wx, wy, wz = L.w(0, 1, 0)
    kraamblok(T, soort, wx, wy, wz, f)
    return L


def churros(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "churros", ROOD, GEEL)
    L.set(-1, 2, 0, "frituurautomaat[facing=%s,snoet=werkt]" % f)
    L.set(1, 2, 0, "minecraft:barrel[facing=up,open=false]")
    for i in range(7):   # reuzenchurro op het dak, met kaneelbanden
        L.set(-3 + i, 7 + (1 if 1 < i < 5 else 0), 3, "minecraft:orange_terracotta" if i % 2 else "minecraft:yellow_terracotta")
        L.set(-3 + i, 8 + (1 if 1 < i < 5 else 0), 3, "minecraft:yellow_terracotta" if i % 2 else "minecraft:orange_terracotta")


def suikerspin(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "suikerspin", ROZE, CREME)
    L.set(-1, 2, 0, "guhbioscoop_popcornmachine[facing=%s]" % f)
    wx, wy, wz = L.w(0, 7, 3)
    for h in range(0, 3):
        T.set(wx, wy + h, wz, "minecraft:white_concrete")
    bal(T, wx, wy + 5, wz, 3.2, 3.4, 3.2, "minecraft:pink_wool")
    bal(T, wx + 1, wy + 6, wz - 1, 2, 2, 2, "minecraft:magenta_wool")


def poffertjes(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "poffertjes", BLAUW, CREME)
    L.set(-2, 2, 0, "minecraft:cake")
    wx, wy, wz = L.w(0, 7, 3)
    for i, (dx, dz, r) in enumerate([(0, 0, 3), (0, 0, 2.2), (0, 0, 1.4)]):
        bal(T, wx + dx, wy + i * 1.3, wz + dz, r, 0.9, r, "minecraft:yellow_terracotta")
    T.set(wx, wy + 3, wz, "minecraft:white_wool")
    T.set(wx + 1, wy + 2, wz, "minecraft:white_carpet")


def oliebollen(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "oliebollen", GEEL, CREME)
    L.set(1, 2, 0, "frituurautomaat[facing=%s,snoet=werkt]" % f)
    wx, wy, wz = L.w(0, 7, 3)
    bal(T, wx, wy + 2, wz, 3, 3, 3, "minecraft:brown_terracotta")
    bal(T, wx, wy + 4.2, wz, 2.2, 1, 2.2, "minecraft:white_wool")
    T.set(wx - 3, wy + 2, wz, "minecraft:yellow_terracotta")
    T.set(wx + 3, wy + 1, wz, "minecraft:yellow_terracotta")


def blikkengooien(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "blikken", BLAUW, GEEL)
    L.fill(-2, 1, 2, 2, 1, 2, "minecraft:birch_slab[type=top,waterlogged=false]")
    for a, h in [(-2, 2), (0, 2), (2, 2), (-1, 3), (1, 3)]:
        L.set(a, h, 2, "minecraft:cauldron")
    L.set(-3, 2, 0, "minecraft:cauldron")
    wx, wy, wz = L.w(0, 7, 3)
    for h, b in [(0, 3), (1, 2.2), (2, 1.4)]:
        bal(T, wx, wy + h, wz, b, 0.8, b, "minecraft:cauldron")


def knabbelschieten(T, x, z, f, G):
    L = kraam(T, x, z, f, G, "schieten", GROEN, CREME)
    for a in (-2, 0, 2):
        L.set(a, 2, 2, "minecraft:target")
    for a, s in [(-1, "knuffel_pinguh"), (1, "knuffel_mint")]:
        L.set(a, 3, 2, s)
    wx, wy, wz = L.w(0, 7, 3)
    for r, s in [(2.4, "minecraft:red_wool"), (1.5, "minecraft:white_wool"), (0.6, "minecraft:red_wool")]:
        for dx in range(-4, 5):
            for dy in range(-4, 5):
                if dx * dx + dy * dy <= r * r:
                    T.set(wx + dx * abs(L.R[0]), wy + 3 + dy, wz + dx * abs(L.R[1]), s)


def guhtjes_vissen(T, x, z, f, G):
    """Ronde vijver onder een rond dakje; (x, z) = midden."""
    R = 6
    for dx in range(-R - 1, R + 2):
        for dz in range(-R - 1, R + 2):
            d = math.hypot(dx, dz)
            if d <= R + 1.2:
                T.set(x + dx, G, z + dz, "minecraft:birch_planks")
            if R - 1 < d <= R:
                T.set(x + dx, G + 1, z + dz, "pleister_hemelsblauw")
            elif d <= R - 1:
                T.set(x + dx, G + 1, z + dz, "minecraft:water")
    rng = np.random.default_rng(3)
    for i in range(9):
        a = rng.uniform(0, 6.28); r = rng.uniform(0, R - 2.5)
        px, pz = x + round(r * math.cos(a)), z + round(r * math.sin(a))
        T.set(px, G + 2, pz, rng.choice(["knuffel_snow", "knuffel_mint", "knuffel_pinguh", "knuffel_golden"]))
    for h in range(2, 7):
        T.set(x, G + h, z, "minecraft:birch_fence")
    for rr, h in [(4, 7), (3, 8), (2, 8), (1, 9)]:
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                d = math.hypot(dx, dz)
                if rr - 1 < d <= rr + 0.3 or (rr == 1 and d <= 1):
                    sec = int((math.atan2(dz, dx) + math.pi) / (math.pi / 4)) % 2
                    T.set(x + dx, G + h, z + dz, BLAUW if sec else CREME, roof=True)
    T.set(x, G + 10, z, "minecraft:gold_block")
    for a in range(0, 360, 60):   # hengels op de rand
        px, pz = round(x + (R - 0.5) * math.cos(math.radians(a))), round(z + (R - 0.5) * math.sin(math.radians(a)))
        T.set(px, G + 2, pz, "minecraft:birch_fence")
    kraamblok(T, "vissen", x + R + 2, G + 1, z, "east")


# ------------------------------------------------------------------ attracties
def zweefmolen(T, cx, cz, G, open_naar="west"):
    R = 12
    for dx in range(-R - 2, R + 3):
        for dz in range(-R - 2, R + 3):
            d = math.hypot(dx, dz)
            if d <= R:
                T.set(cx + dx, G, cz + dz, "tegel_dambord")
            elif d <= R + 1:
                T.set(cx + dx, G, cz + dz, "pleister_framboos")
                poort = {"west": dx < 0 and abs(dz) < 3, "east": dx > 0 and abs(dz) < 3, "south": dz > 0 and abs(dx) < 3,
                         "north": dz < 0 and abs(dx) < 3}[open_naar]
                if not poort:
                    T.set(cx + dx, G + 1, cz + dz, "guh_hekje_roze")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if math.hypot(dx, dz) <= 2.3:
                for h in range(1, 13):
                    T.set(cx + dx, G + h, cz + dz, "pleister_boter" if h % 4 else "pleister_framboos")
    rr = 10
    for dx in range(-rr, rr + 1):
        for dz in range(-rr, rr + 1):
            d = math.hypot(dx, dz)
            if d <= rr:
                sec = int((math.atan2(dz, dx) + math.pi) / (math.pi / 6)) % 2
                T.set(cx + dx, G + 13 + int((rr - d) * 0.55), cz + dz, ROOD if sec else CREME, roof=True)
                if d > rr - 1:
                    T.set(cx + dx, G + 12, cz + dz, "minecraft:gold_block" if sec else "pleister_framboos")
    T.fill(cx, G + 19, cz, cx, G + 21, cz, "minecraft:gold_block")
    rit(T, "zweefmolen", cx, G, cz, open_naar)


def botsguhs(T, x1, z1, x2, z2, G, ingang="north"):
    H = 7
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            T.set(x, G, z, "retrotegel_paars" if (x + z) % 2 else "pleister_drop")
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            if not (ingang in ("north", "south") and abs(x - (x1 + x2) // 2) < 3 and z == (z1 if ingang == "north" else z2)):
                T.set(x, G + 1, z, "pleister_framboos")
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            T.set(x, G + 1, z, "pleister_framboos")
    for x in list(range(x1, x2, 6)) + [x2]:
        for z in (z1, z2):
            T.fill(x, G + 1, z, x, G + H - 1, z, "pleister_boter")
    for z in list(range(z1, z2, 5)) + [z2]:
        for x in (x1, x2):
            T.fill(x, G + 1, z, x, G + H - 1, z, "pleister_boter")
    mx = (x1 + x2) // 2
    for x in range(x1 - 1, x2 + 2):
        for z in range(z1 - 1, z2 + 2):
            hoog = min(abs(z - (z1 - 1)), abs(z - (z2 + 1))) // 2
            T.set(x, G + H + hoog, z, ROOD if (x - x1) // 3 % 2 else CREME, roof=True)
            if (x + z) % 4 == 0 and z1 < z < z2 and x1 < x < x2:
                T.set(x, G + H - 1, z, "minecraft:iron_bars", roof=True)
    for z in (z1 - 1, z2 + 1):
        for x in range(x1 - 1, x2 + 2, 2):
            T.set(x, G + H - 1, z, "lampion_roze[hanging=true]")
    rit(T, "botsguhs", mx, G, (z1 + z2) // 2, ingang)


def booster(T, cx, cz, G):
    """Blikvanger: mast tot G+40, arm 2x22 in het x-y-vlak (as langs z), gondels aan beide einden.
    Hoogste punt G+65 als de arm rechtop staat. hoek = stand van de arm in graden boven horizontaal."""
    hub = G + 40
    R = 10
    for dx in range(-R, R + 1):
        for dz in range(-R, R + 1):
            d = math.hypot(dx, dz)
            if d <= R - 1:
                T.set(cx + dx, G, cz + dz, "tegel_dambord")
            elif d <= R:
                T.set(cx + dx, G, cz + dz, "pleister_framboos")
                if not (dz > 0 and abs(dx) < 5):      # (the gate: the ride's ticket post stands in it)
                    T.set(cx + dx, G + 1, cz + dz, "guh_hekje_roze")
    for sx in (-1, 1):     # vier poten die naar de kop toelopen
        for sz in (-1, 1):
            # (the arm and its cages turn in z = cz + 2..6: the legs on that side stay out of it)
            lijn(T, (cx + sx * 7, G + 1, cz + (6 if sz < 0 else 1) * sz), (cx + sx, hub - 2, cz + sz), "pleister_framboos")
    for h in range(1, 40):     # mast
        n = "minecraft:gold_block" if h % 8 == 0 else ("pleister_boter" if h % 2 else "pleister_framboos")
        for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            T.set(cx + dx, G + h, cz + dz, n)
    for h in range(8, 40, 8):  # ringen
        r = max(1, 6 - h // 8)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if r - 1 < math.hypot(dx, dz) <= r + 0.4 and (h < 16 or dz < 2):
                    T.set(cx + dx, G + h, cz + dz, "pleister_vanille")
    for dx in range(-2, 3):    # kop en as
        for dz in range(-2, 5):
            for dy in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    T.set(cx + dx, hub + dy, cz + dz, "minecraft:gold_block" if dz in (-2, 4) else "pleister_framboos")
    T.fill(cx, hub + 3, cz + 1, cx, hub + 6, cz + 1, "minecraft:gold_block")
    rit(T, "booster", cx, G, cz, "south")


def twister(T, cx, cz, G, open_naar="north"):
    """Aanname Twister/Scrambler: drie armen uit het midden, aan elk armeinde drie karretjes aan een draaikruis. Losse zone."""
    R = 13
    for dx in range(-R - 1, R + 2):
        for dz in range(-R - 1, R + 2):
            d = math.hypot(dx, dz)
            if d <= R:
                T.set(cx + dx, G, cz + dz, "retrotegel_paars" if (dx + dz) % 2 else "pleister_lavendel")
            elif d <= R + 1:
                T.set(cx + dx, G, cz + dz, "pleister_hemelsblauw")
                poort = {"north": dz < 0 and abs(dx) < 5, "south": dz > 0 and abs(dx) < 5,
                         "east": dx > 0 and abs(dz) < 5, "west": dx < 0 and abs(dz) < 5}[open_naar]
                if not poort:
                    T.set(cx + dx, G + 1, cz + dz, "guh_hekje_mint")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if math.hypot(dx, dz) <= 2.3:
                T.fill(cx + dx, G + 1, cz + dz, cx + dx, G + 5, cz + dz, "pleister_hemelsblauw")
    bal(T, cx, G + 6, cz, 3, 2, 3, "pleister_korenbloem")
    T.fill(cx, G + 8, cz, cx, G + 10, cz, "minecraft:gold_block")
    rit(T, "twister", cx, G, cz, open_naar)


def reuzenrad(T, cx, cz, G, as_z=True):
    """Rad in het x-y-vlak (as langs z). cx/cz = midden, naaf op G+19."""
    RR, hub = 13, G + 19
    for z in range(cz - 4, cz + 5):
        T.set(cx, hub, z, "minecraft:gold_block")
    for zz in (cz - 4, cz + 4):
        lijn(T, (cx - 9, G + 1, zz), (cx, hub, zz), "pleister_mokka")
        lijn(T, (cx + 9, G + 1, zz), (cx, hub, zz), "pleister_mokka")
        lijn(T, (cx - 5, G + 10, zz), (cx + 5, G + 10, zz), "pleister_mokka")
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 3, cz + 7):
            T.set(x, G, z, "tegel_hartjes")
    T.fill(cx - 2, G + 1, cz - 2, cx + 2, G + 1, cz + 2, "pleister_roze")
    T.set(cx - 3, G + 1, cz + 4, "guh_bankje_roze[facing=north]")
    T.set(cx + 3, G + 1, cz + 4, "guh_bankje_roze[facing=north]")
    rit(T, "reuzenrad", cx, G, cz, "south")


def spook(T, x, y, z, s=1.0):
    """Lief spookje van wol met kraaloogjes."""
    bal(T, x, y, z, 3 * s, 4 * s, 3 * s, "minecraft:white_wool")
    T.set(x - 1, y + 1, z + round(3 * s), "minecraft:black_wool")
    T.set(x + 1, y + 1, z + round(3 * s), "minecraft:black_wool")
    T.set(x, y, z + round(3 * s), "minecraft:pink_wool")


def spookhuis(T, x1, z1, x2, z2, G):
    """Ingang aan de oostkant (x2). Binnen een slingerrit; dak weg te halen met noroof."""
    H = 9
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            T.set(x, G, z, "minecraft:polished_blackstone_bricks")
    T.walls(x1, G + 1, z1, x2, G + H, z2, "pleister_drop")
    for x in (x1, x2):
        for z in range(z1, z2 + 1, 4):
            T.fill(x, G + 1, z, x, G + H, z, "pleister_lavendel")
    for z in (z1, z2):
        for x in range(x1, x2 + 1, 4):
            T.fill(x, G + 1, z, x, G + H, z, "pleister_lavendel")
    # binnen: zigzag-rit (schotten + pad)
    for i, x in enumerate(range(x1 + 5, x2 - 3, 5)):
        for z in range(z1 + 1, z2):
            if (i % 2 == 0 and z < z2 - 3) or (i % 2 == 1 and z > z1 + 3):
                T.fill(x, G + 1, z, x, G + H - 3, z, "pleister_muisgrijs")
        gz = z1 + 3 if i % 2 else z2 - 2
        T.set(x - 2, G + 1, gz, "spooklantaarn[hanging=false]")
    # dak
    for x in range(x1 - 1, x2 + 2):
        for z in range(z1 - 1, z2 + 2):
            h = min(z - z1 + 1, z2 + 1 - z) // 2
            T.set(x, G + H + 1 + h, z, "dakpan_lavendel" if (x + h) % 3 else "dakpan_roze", roof=True)
    # toren
    tx, tz = x1 + 3, z1 + 3
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            d = math.hypot(dx, dz)
            if d <= 3.3:
                T.fill(tx + dx, G + 1, tz + dz, tx + dx, G + H + 8, tz + dz, "pleister_drop" if d > 2.2 else "minecraft:air", roof=True)
    for i, rr in enumerate((4, 3, 2, 1)):
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                if math.hypot(dx, dz) <= rr + 0.3:
                    T.fill(tx + dx, G + H + 9 + i * 2, tz + dz, tx + dx, G + H + 10 + i * 2, tz + dz, "dakpan_roze", roof=True)
    # gevel: twee monden (in/uit) aan de oostkant
    mz = (z1 + z2) // 2
    for z, naam in ((mz - 3, "in"), (mz + 3, "uit")):
        T.air(x2, G + 1, z - 1, x2, G + 4, z + 1)
        T.fill(x2 + 1, G + 5, z - 2, x2 + 1, G + 5, z + 2, "pleister_framboos")
        T.fill(x2 + 1, G + 1, z - 2, x2 + 1, G + 4, z - 2, "pleister_framboos")
        T.fill(x2 + 1, G + 1, z + 2, x2 + 1, G + 4, z + 2, "pleister_framboos")
    # rail-strook buiten (de echte Guhslee-rail is onzichtbaar in deze render)
    for x in range(x2 - 5, x2 + 7):
        T.set(x, G, mz - 3, "pleister_muisgrijs")
        T.set(x, G, mz + 3, "pleister_muisgrijs")
    for z in range(mz - 3, mz + 4):
        T.set(x2 + 7, G, z, "pleister_muisgrijs")
    T.set(x2 + 1, G + 7, mz, "spooklantaarn[hanging=false]")
    rit(T, "spookhuis", x2 + 2, G, mz, "east")
    # spookjes op het dak
    bal(T, x2 - 2, G + H + 5, mz, 3, 4, 3, "minecraft:white_wool", roof=True)
    for dz, s in ((-1, "minecraft:black_wool"), (1, "minecraft:black_wool")):
        T.set(x2 + 1, G + H + 6, mz + dz, s, roof=True)
    T.set(x2 + 1, G + H + 5, mz, "minecraft:pink_wool", roof=True)
    bal(T, x1 + 12, G + H + 6, z1 + 3, 2, 3, 2, "minecraft:white_wool", roof=True)
    for z in (z1 - 1, z2 + 1):
        for x in range(x1 + 2, x2, 5):
            T.set(x, G + 3, z, "spooklantaarn[hanging=false]")
