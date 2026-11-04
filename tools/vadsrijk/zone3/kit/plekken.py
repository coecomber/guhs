"""Plekken in zone 3 (ZONE3_PLAN.md §4): dorpjes, setpieces en aankleding rond de wereldboom.

Elke functie werkt in wereldcoördinaten (t.o.v. de stamas) en slaat zichzelf over als hij buiten het venster valt.
  hangnest(W)        Hangnest Dauwdrup onder de W-tak (nesthuisjes, hangbrugplein)
  mosvoet(W)         Mosvoet in een holle wortelknol (ZO), met waterrad en het onderstation van de Honinglift
  schorsstad(W)      de Schorsstad in de holle stam: ringterrassen, wandhuisjes, Lantaarntoren, lichtbundels
  hartslag(W)        de Hartslag-zaal: wortelpilaren, het ei in de wortelwieg, grotlantaarns
  werf(W)            de Kaaszeppelinwerf aan de punt van de NW-tak (kerker 1)
  schimmelboerderij(W), lichtkeverbos(W), paddenbloesemdal(W), honingmoeras(W), wortelgewelf(W),
  wortelpoort(W), pluimposten(W), kruinring(W), mika_steiger(W)
  bouw(W)            alles
"""
import math, functools
import numpy as np
import schemerkit as SK
import boom
import vrkit as VK
from schemerkit import hash3, vnoise

LAMP_R, LAMP_G = "guhs:lampion_roze", "guhs:lampion_geel"
HOUT, PLANK, HEK = "minecraft:stripped_spruce_wood", "minecraft:spruce_planks", "minecraft:spruce_fence"
STEEL = "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]"


def grond(W, x, z):
    """Maaiveld-y bij (x, z) volgens het terreinmodel (niet het venster)."""
    return float(boom.grondhoogte(np.array([x]), np.array([z]))[0])


def boven(W, x, z, ymax=None):
    """Bovenkant van wat er in het venster staat op (x, z) (of het terreinmodel als de kolom leeg is)."""
    i, j = W.vx(x), W.vz(z)
    if 0 <= i < W.v.shape[0] and 0 <= j < W.v.shape[2]:
        col = W.v[i, :, j]
        if ymax is not None:
            col = col[:max(0, W.vy(ymax))]
        nz = np.nonzero(col)[0]
        if len(nz):
            return W.ys[nz.max()] + 0.5 / W.k
    return grond(W, x, z) + 1


# ===================================================================================================== Hangnest
HANGNEST = (-488, 140, 33)


def nesthuisje(W, cx, cy, cz, r=6.0, seed=0):
    """Komvormig nesthuisje: een gevlochten kom, een rond pluisdakje, deurtje en raampjes, lampion eronder."""
    gevlecht = ["minecraft:stripped_spruce_wood", "minecraft:stripped_birch_wood", "minecraft:hay_block",
                "minecraft:spruce_wood", "minecraft:stripped_oak_wood"]
    W.bol((cx, cy, cz), r, None, ry=r * 0.8, hollow=1.3, ymax=cy + 1,
          mixfn=lambda x, y, z, m: W.kies(gevlecht, [3, 2, 2, 1, 1], x, y, z, 400 + seed))
    W.cilinder(cx, cz, r - 1.2, int(cy - 1), int(cy - 1), PLANK)
    # muurtje + dak
    W.cilinder(cx, cz, r - 0.6, int(cy + 1), int(cy + 3), "minecraft:white_terracotta" if seed % 2 else
               "minecraft:pink_terracotta", hollow=1)
    for k in range(5):
        rr = (r + 0.8) * (1 - k / 5.0)
        W.cilinder(cx, cz, max(0.7, rr), int(cy + 4 + k), int(cy + 4 + k), "guhs:pluisdak" if k % 2 == 0 else
                   "minecraft:pink_wool")
    a = seed * 1.3
    dx, dz = math.cos(a), math.sin(a)
    for k in range(2):
        W.wset(cx + dx * (r - 0.6), cy + 1 + k, cz + dz * (r - 0.6), "minecraft:air")
    W.wset(cx - dx * (r - 0.6), cy + 2, cz - dz * (r - 0.6), "minecraft:orange_stained_glass")
    W.wset(cx + dz * (r - 0.6), cy + 2, cz - dx * (r - 0.6), "minecraft:pearlescent_froglight")
    W.wset(cx, cy - r * 0.8 - 1, cz, "minecraft:iron_chain[axis=y]")
    W.wset(cx, cy - r * 0.8 - 2, cz, LAMP_R if seed % 2 else LAMP_G)
    VK.oortjes(W, W.vx(cx), W.vy(cy + 8), W.vz(cz), (1, 0), gap=1, h=2, w=2) if W.k == 1 else None


def touw(W, p0, p1, blok="minecraft:iron_chain[axis=y]"):
    n = int(max(abs(p1[1] - p0[1]), abs(p1[0] - p0[0]), abs(p1[2] - p0[2])) * W.k + 1)
    for i in range(n + 1):
        t = i / max(1, n)
        W.wset(*(p0[k] + (p1[k] - p0[k]) * t for k in range(3)), blok, keep=True)


def hangbrug(W, p0, p1, zak=3.0):
    n = int(max(abs(p1[0] - p0[0]), abs(p1[2] - p0[2])) + 1)
    for i in range(n + 1):
        t = i / max(1, n)
        x = p0[0] + (p1[0] - p0[0]) * t
        z = p0[2] + (p1[2] - p0[2]) * t
        y = p0[1] + (p1[1] - p0[1]) * t - zak * math.sin(math.pi * t)
        W.wset(x, y, z, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        if i % 3 == 0:
            d = np.array([p1[2] - p0[2], 0, -(p1[0] - p0[0])], float)
            d /= np.linalg.norm(d) + 1e-9
            W.wset(x + d[0] * 1.4, y + 1, z + d[2] * 1.4, HEK, keep=True)
            W.wset(x - d[0] * 1.4, y + 1, z - d[2] * 1.4, HEK, keep=True)


def hangnest(W):
    cx, cy, cz = HANGNEST
    if not W.raakt(cx - 60, cy - 30, cz - 60, cx + 60, 200, cz + 60):
        return
    # plein: rond hangplatform
    W.cilinder(cx, cz, 9, cy - 1, cy - 1, PLANK)
    W.cilinder(cx, cz, 9, cy, cy, HEK, hollow=1)
    W.cilinder(cx, cz, 2.5, cy - 1, cy - 1, "minecraft:honeycomb_block")
    W.wset(cx, cy, cz, "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    tak_y = lambda x, z: boom.tak_top("west", x, z) - 2 * boom.tak_lijn("west")[1][0] * 0 - 24
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        p = (cx + 8 * math.cos(a), cy, cz + 8 * math.sin(a))
        touw(W, p, (p[0], boom.tak_top("west", p[0], p[2]) - 22, p[2]))
    huisjes = [(-20, -6, 4), (-14, 14, -3), (2, 21, 6), (18, 12, -6), (22, -6, 3), (8, -20, -1),
               (-8, -22, 8), (-30, 6, 11), (30, 4, 10), (-2, 32, 12)]
    for i, (dx, dz, dy) in enumerate(huisjes):
        x, y, z = cx + dx, cy + dy, cz + dz
        nesthuisje(W, x, y, z, r=5.5 + (i % 3) * 0.8, seed=i)
        touw(W, (x, y + 9, z), (x, boom.tak_top("west", x, z) - 20, z))
        hangbrug(W, (cx + dx * 0.35, cy - 1, cz + dz * 0.35), (x - dx * 0.2, y - 1, z - dz * 0.2), zak=2)
    W.label(cx, cy + 14, cz, "Hangnest Dauwdrup")
    # touwladder naar de tak
    for y in range(cy, int(boom.tak_top("west", cx + 10, cz)) - 20):
        W.wset(cx + 10, y, cz, "minecraft:ladder[facing=south,waterlogged=false]")
    for (x, z, a) in ((-488, 30, 0), (-484, 36, 120), (-492, 34, 250)):
        W.wguh(x + 0.5, cy, z + 0.5, a, skin="guh")


# ===================================================================================================== Mosvoet
MOSVOET = (250, 66, 420)


def mosvoet(W):
    cx, gy, cz = MOSVOET
    if not W.raakt(cx - 70, 50, cz - 70, cx + 70, 130, cz + 70):
        return
    g = grond(W, cx, cz)
    schors = lambda x, y, z, m: W.kies(boom.SCHORS, boom.SCHORS_W, x, y, z, 500)
    mos = lambda x, y, z, m: W.kies(boom.MOS + ["minecraft:moss_block"], [6, 2, 2, 0.5, 4], x, y, z, 501)
    # de holle wortelknol
    W.bol((cx, g + 12, cz), 24, None, ry=20, mixfn=schors)
    W.bol((cx, g + 19, cz), 22, None, ry=15, mixfn=mos, ymin=g + 26)
    W.bol((cx, g + 33, cz), 27, None, ry=8, mixfn=mos, ymin=g + 30)   # mosdak als een paddenstoelhoed
    for k in range(16):
        a = k * 2 * math.pi / 16
        W.wset(cx + 25 * math.cos(a), g + 31, cz + 25 * math.sin(a), LAMP_G if k % 2 else LAMP_R)
    W.bol((cx, g + 12, cz), 20, "minecraft:air", ry=16)
    W.bol((cx, g + 12, cz), 20.5, None, ry=0.6, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:spruce_planks", "minecraft:oak_planks"], [3, 1], x, y, z, 502), ymax=g + 1)
    # ronde deurtjes en raampjes in ringen rond de knol
    for k in range(10):
        a = k * 2 * math.pi / 10
        for (hh, rr) in ((g + 2, 24.0), (g + 13, 23.0)):
            x, z = cx + rr * math.cos(a), cz + rr * math.sin(a) * 1.0
            if hh == g + 2 and k % 2:
                W.bol((x, hh + 2, z), 2.2, "minecraft:air", ry=2.6)
                W.wset(x + math.cos(a) * 1.5, hh + 5, z + math.sin(a) * 1.5, LAMP_G)
            else:
                W.bol((x, hh + 2, z), 1.4, "minecraft:orange_stained_glass", ry=1.4)
    W.wset(cx, g + 34, cz, "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    # kleine knolhuisjes eromheen
    for i, (dx, dz) in enumerate(((-38, 10), (-30, -26), (34, -22), (40, 18), (6, 40), (-18, 38))):
        x, z = cx + dx, cz + dz
        gg = grond(W, x, z)
        W.bol((x, gg + 4, z), 7, None, ry=6.5, mixfn=schors)
        W.bol((x, gg + 7, z), 6.8, None, ry=4, mixfn=mos, ymin=gg + 8)
        W.bol((x, gg + 4, z), 5.6, "minecraft:air", ry=5)
        a = math.atan2(cz - z, cx - x)
        W.bol((x + 6.5 * math.cos(a), gg + 2.5, z + 6.5 * math.sin(a)), 1.6, "minecraft:air", ry=2.2)
        W.wset(x - 6.5 * math.cos(a), gg + 5, z - 6.5 * math.sin(a), "minecraft:orange_stained_glass")
        W.wset(x, gg + 12, z, "minecraft:brick_wall", keep=True)
    # sapbeek met waterrad
    for t in np.linspace(0, 1, 160):
        x = cx - 70 + 140 * t
        z = cz + 30 + 8 * math.sin(t * 6)
        gg = grond(W, x, z)
        for w in range(-2, 3):
            W.wset(x, gg, z + w, "minecraft:water")
            W.wset(x, gg - 1, z + w, "minecraft:mud")
            W.wset(x, gg + 1, z + w, "minecraft:air")
    wx_, wz_ = cx + 20, cz + 30
    gg = grond(W, wx_, wz_)
    for a in np.linspace(0, 2 * math.pi, 70):
        for rr in (6, 6.5):
            W.wset(wx_ + rr * math.cos(a), gg + 4 + rr * math.sin(a), wz_ + 3, "minecraft:stripped_spruce_wood")
    for k in range(8):
        a = k * math.pi / 4
        for rr in np.linspace(0, 6, 8):
            W.wset(wx_ + rr * math.cos(a), gg + 4 + rr * math.sin(a), wz_ + 3, "minecraft:spruce_planks")
    # onderstation van de Honinglift
    lx, lz = 255, 400
    lg = grond(W, lx, lz)
    W.wfill(lx - 4, lg, lz - 4, lx + 4, lg, lz + 4, PLANK)          # [zone 3 kaart] flush with the ground
    W.wfill(lx - 4, lg + 1, lz - 4, lx + 4, lg + 3, lz + 4, "minecraft:air")
    for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        W.wfill(lx + dx, lg + 1, lz + dz, lx + dx, lg + 34, lz + dz, HOUT)
    W.wfill(lx - 4, lg + 35, lz - 4, lx + 4, lg + 35, lz + 4, "minecraft:honeycomb_block")
    W.wset(lx, lg + 36, lz, LAMP_G)
    W.label(cx, g + 40, cz, "Mosvoet")
    for (dx, dz, a) in ((-4, -8, 0), (3, -9, 90), (10, -30, 200), (-20, 30, 300)):
        W.wguh(cx + dx + 0.5, g + 1, cz + dz + 0.5, a)


# ===================================================================================================== Schorsstad
RINGEN = [80 + 16 * i for i in range(7)]
TOREN = (0, 0, 3.5)


def schorsstad(W):
    if not W.raakt(-75, 72, -75, 75, 200, 75):
        return
    Ri = boom.R_BINNEN
    rng = np.random.default_rng(7)
    # vloer
    W.cilinder(0, 0, Ri + 1, 75, 75, None, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:moss_block", "minecraft:rooted_dirt", "guhs:vadsmos", "minecraft:spruce_planks"], [3, 1, 1, 1],
        x, y, z, 600))
    W.cilinder(18, -12, 9, 75, 75, "minecraft:water")
    for i, y in enumerate(RINGEN):
        # terras langs de wand
        W.cilinder(0, 0, Ri + 0.5, y, y, None, hollow=7, mixfn=lambda x, yy, z, m: W.kies(
            ["minecraft:spruce_planks", "minecraft:stripped_spruce_wood", "minecraft:spruce_planks",
             "minecraft:moss_block"], [4, 1, 2, 0.6], x, yy, z, 601 + i))
        # rand met hekjes en lantaarnpaden
        for a in np.linspace(0, 2 * math.pi, 260, endpoint=False):
            x, z = (Ri - 7) * math.cos(a), (Ri - 7) * math.sin(a)
            if hash3(a * 50, y, 0, 602) < 0.86:
                W.wset(x, y + 1, z, HEK)
        for k in range(16):
            a = k * 2 * math.pi / 16 + i * 0.2
            x, z = (Ri - 6.5) * math.cos(a), (Ri - 6.5) * math.sin(a)
            for h in (1, 2, 3):
                W.wset(x, y + h, z, HEK)
            W.wset(x, y + 4, z, "minecraft:lantern[hanging=false,waterlogged=false]" if k % 2 else LAMP_G)
        # wandhuisjes: nissen in de schors met deurtje en verlicht raam
        for k in range(9):
            a = k * 2 * math.pi / 9 + i * 0.37
            ca, sa = math.cos(a), math.sin(a)
            for d in np.arange(Ri - 0.5, Ri + 9, 0.5):
                for w in np.arange(-3.5, 3.6, 0.5):
                    for h in range(1, 7):
                        W.wset(d * ca - w * sa, y + h, d * sa + w * ca, "minecraft:air")
                    W.wset(d * ca - w * sa, y, d * sa + w * ca, PLANK)
            # gevel
            for w in np.arange(-3.5, 3.6, 0.5):
                for h in range(1, 7):
                    x, z = (Ri + 0.3) * ca - w * sa, (Ri + 0.3) * sa + w * ca
                    b = "minecraft:stripped_oak_wood" if h < 6 else "minecraft:spruce_planks"
                    if abs(w) < 1 and h < 4:
                        b = "minecraft:air"
                    if 1.5 < abs(w) < 3 and h in (3, 4):
                        b = "minecraft:ochre_froglight" if (k + i) % 3 else "minecraft:orange_stained_glass"
                    W.wset(x, y + h, z, b)
            W.wset((Ri - 0.5) * ca, y + 6, (Ri - 0.5) * sa, LAMP_R)
        # amberroze-kristallen
        for k in range(5):
            a = rng.uniform(0, 2 * math.pi)
            yy = y + rng.uniform(7, 14)
            W.bol(((Ri - 0.5) * math.cos(a), yy, (Ri - 0.5) * math.sin(a)), rng.uniform(1.8, 3.2), None, ry=2.6,
                  mixfn=lambda x, y_, z, m: W.kies(["guhs:weidekristal_roze", "minecraft:pink_stained_glass",
                                                    "guhs:guh_kristal_blok"], [3, 2, 1], x, y_, z, 603))
    # trappen tussen de ringen: schuine helling langs de wand
    for i in range(len(RINGEN) - 1):
        y0, y1 = RINGEN[i], RINGEN[i + 1]
        a0 = i * 1.9 + 0.5
        for t in np.linspace(0, 1, 120):
            a = a0 + t * 0.85
            y = y0 + (y1 - y0) * t
            for rr in np.arange(Ri - 6.5, Ri - 1, 0.5):
                W.wset(rr * math.cos(a), y, rr * math.sin(a), "minecraft:spruce_planks")
            W.wset((Ri - 7) * math.cos(a), y + 1, (Ri - 7) * math.sin(a), HEK)
    # houten liften met tegenwicht
    for (a, y0, y1) in ((2.6, 76, 177), (5.2, 76, 145)):
        x, z = (Ri - 10) * math.cos(a), (Ri - 10) * math.sin(a)
        for y in range(y0, y1):
            W.wset(x - 1.5, y, z, "minecraft:iron_chain[axis=y]")
            W.wset(x + 1.5, y, z, "minecraft:iron_chain[axis=y]")
        cy = (y0 + y1) // 2
        W.wfill(x - 2, cy, z - 2, x + 2, cy, z + 2, PLANK)
        W.wfill(x - 1, cy + 30, z - 1, x + 1, cy + 32, z + 1, "minecraft:cobblestone")
    lantaarntoren(W)
    W.label(0, 186, 0, "Lantaarntoren")


def lantaarntoren(W, aan=7):
    """De Lantaarntoren hangt in het midden aan wortelkettingen; 'aan' ringen zijn aangestoken."""
    for y in range(78, 186):
        W.cilinder(0, 0, 2.6, y, y, "minecraft:stripped_spruce_wood" if y % 8 else "minecraft:spruce_wood")
    for i, y in enumerate(RINGEN):
        ry = y + 3
        W.cilinder(0, 0, 7, ry, ry, PLANK)
        W.cilinder(0, 0, 7, ry + 1, ry + 1, HEK, hollow=1)
        lit = i < aan
        for k in range(12):
            a = k * math.pi / 6
            W.wset(7.6 * math.cos(a), ry, 7.6 * math.sin(a),
                   ("minecraft:pearlescent_froglight" if k % 2 else "guhs:weidekristal_roze") if lit else
                   "minecraft:spruce_planks")
            W.wset(6.4 * math.cos(a), ry + 2, 6.4 * math.sin(a), LAMP_R if (lit and k % 3 == 0) else
                   ("minecraft:lantern[hanging=false,waterlogged=false]" if lit else "guhs:lampion_uit"))
        # loopbrug van de toren naar het terras
        a = i * 0.9
        for rr in np.arange(7, boom.R_BINNEN - 6, 0.5):
            W.wset(rr * math.cos(a), y + 3 - (rr - 7) * 3 / (boom.R_BINNEN - 13), rr * math.sin(a),
                   "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    # spits + wortelkettingen naar het plafond
    for k in range(8):
        W.cilinder(0, 0, max(0.6, 3.5 - k * 0.45), 186 + k, 186 + k, "minecraft:pink_concrete")
    W.wset(0, 194, 0, "minecraft:pearlescent_froglight")
    for k in range(6):
        a = k * math.pi / 3
        touw(W, (0, 184, 0), (40 * math.cos(a), 197, 40 * math.sin(a)), "minecraft:mangrove_roots")


def lichtbundels(sterkte=0.22):
    """Schuine zonnebundels door de kwastgaten naar binnen (doorschijnende extra-quads)."""
    q = []
    for (a, y, r) in boom.KWASTGATEN:
        p0 = np.array([math.cos(a) * 70, y, math.sin(a) * 70])
        d = np.array([-math.cos(a) * 0.9, -0.55, -math.sin(a) * 0.9])
        p1 = p0 + d * 70
        side = np.array([-math.sin(a), 0, math.cos(a)]) * r * 0.8
        up = np.array([0, r * 0.8, 0])
        tex = np.ones((2, 2, 4), np.float32)
        tex[..., :3] = (1.0, 0.92, 0.66)
        tex[..., 3] = sterkte
        for s in (side, up):
            P = np.array([p0 - s, p1 - s, p0 + s], float)
            for PP in (P, P[[0, 2, 1]]):
                q.append((PP, np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float), tex, (1, 1, 1), True))
    return q


# ===================================================================================================== Hartslag-zaal
EI = (0, 56, 0)


def hartslag(W):
    cx, fy, cz, r, h = boom.HARTSLAG
    if not W.raakt(-90, 0, -90, 90, 75, 90):
        return
    schors = lambda x, y, z, m: np.where(hash3(x, y, z, 700) < 0.09, W.id("guhs:weidekristal_roze"),
                                         W.kies(boom.SCHORS, boom.SCHORS_W, x, y, z, 701))
    # wortelpilaren die van het plafond naar de vloer buigen
    for k in range(9):
        a = k * 2 * math.pi / 9 + 0.2
        r0 = 18 + (k % 3) * 9
        p0 = (r0 * math.cos(a), fy + h + 6, r0 * math.sin(a))
        p1 = ((r0 + 12) * math.cos(a + 0.2), fy + h * 0.5, (r0 + 12) * math.sin(a + 0.2))
        p2 = ((r0 + 20) * math.cos(a + 0.1), fy - 2, (r0 + 20) * math.sin(a + 0.1))
        L = boom._spline([p0, p1, p2], 8)
        W.buis(L, np.linspace(5.5, 3.5, len(L)), None, mixfn=schors)
    # nestkamer met het ei, door een gat in het plafond zichtbaar
    ex, ey, ez = EI
    W.bol((ex, ey, ez), 11, "minecraft:air", ry=9)
    W.cilinder(ex, ez, 6, fy + 20, ey, "minecraft:air")
    for k in range(10):
        a = k * 2 * math.pi / 10
        L = boom._spline([(9 * math.cos(a), ey - 8, 9 * math.sin(a)), (6 * math.cos(a + 0.4), ey - 4, 6 * math.sin(a + 0.4)),
                          (4.5 * math.cos(a + 0.8), ey + 2, 4.5 * math.sin(a + 0.8))], 5)
        W.buis(L, np.full(len(L), 1.4), None, mixfn=schors)
    W.bol((ex, ey - 1, ez), 3.6, "guhs:weidekristal_roze", ry=5.2)
    W.bol((ex, ey - 1, ez), 2.6, "minecraft:pearlescent_froglight", ry=4.0)
    for k in range(6):
        a = k * math.pi / 3
        W.wset(ex + 3.7 * math.cos(a), ey + 1, ez + 3.7 * math.sin(a), "minecraft:pearlescent_froglight")
    # gloeiende aders naar het ei toe (in de vloer, als spaken)
    for k in range(12):
        a = k * math.pi / 6
        for rr in np.arange(8, r, 0.7):
            y = fy + 0.0
            x, z = rr * math.cos(a + 0.15 * math.sin(rr / 9)), rr * math.sin(a + 0.15 * math.sin(rr / 9))
            for yy in range(int(fy) - 3, int(fy) + 12):
                b = W.wget(x, yy, z)
                if b != "minecraft:air" and W.wget(x, yy + 1, z) == "minecraft:air":
                    W.wset(x, yy, z, "guhs:weidekristal_roze")
                    break
    # grotlantaarns langs de Grote Wortelgang en in de zaal (sommige aan)
    L = boom._spline(boom.WORTELGANG, 10)
    for i in range(0, len(L), 6):
        x, y, z = L[i]
        W.wfill(x + 7, y - 9, z, x + 7, y - 6, z, HOUT)
        W.wset(x + 7, y - 5, z, "minecraft:lantern[hanging=false,waterlogged=false]" if i % 12 == 0 else
               "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    W.wset(30, fy + 1, 20, "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    W.label(ex, ey + 12, ez, "Het ei (Hart van de Boom)")
    W.label(0, fy + 4, 40, "Hartslag-zaal")


# ===================================================================================================== Kaaszeppelinwerf
WERF = (-600, 140, -570)


def kaaszeppelin(W, c, L=30, af=0.6, seed=0):
    """Half gebouwde kaaszeppelin: gatenkaas-romp met ijzeren ribben; af = fractie bekleed."""
    cx, cy, cz = c
    huid = lambda x, y, z, m: np.where(
        (vnoise(x, z + y, cell=4, seed=800 + seed) > 0.55), 0,
        W.kies(["guhs:gatenkaas_stenen", "guhs:belegen_kaas_stenen", "minecraft:yellow_concrete"], [3, 2, 1],
               x, y, z, 801 + seed))
    rib = lambda x, y, z, m: np.full(x.shape, W.id("minecraft:iron_block"), np.uint16)
    m = W.bol(c, L / 2, None, ry=L / 5, rz=L / 5, hollow=1.2,
              mixfn=lambda x, y, z, mm: np.where(x < cx - L / 2 + L * af, huid(x, y, z, mm),
                                                 np.where(((x - cx) % 4 < 1), rib(x, y, z, mm), 0)))
    W.wfill(cx - 4, cy - L / 5 - 3, cz - 2, cx + 6, cy - L / 5 - 1, cz + 2, "minecraft:gray_concrete")
    for k in range(3):
        W.wset(cx - L / 2 - 1 - k, cy, cz, "minecraft:light_gray_wool")


def werf(W):
    cx, cy, cz = WERF
    if not W.raakt(cx - 70, cy - 50, cz - 70, cx + 70, cy + 40, cz + 70):
        return
    # steigers (rooster van palen en vloeren) onder de takpunt
    for x in range(cx - 40, cx + 41, 8):
        for z in range(cz - 30, cz + 31, 10):
            W.wfill(x, cy - 30, z, x, cy + 6, z, "minecraft:dark_oak_fence")
    for y in (cy - 30, cy - 15, cy):
        W.wfill(cx - 40, y, cz - 30, cx + 40, y, cz - 28, "minecraft:dark_oak_planks")
        W.wfill(cx - 40, y, cz + 28, cx + 40, y, cz + 30, "minecraft:dark_oak_planks")
    # kranen
    for (x, z) in ((cx - 30, cz - 30), (cx + 25, cz + 30)):
        W.wfill(x, cy - 30, z, x, cy + 22, z, "minecraft:iron_block")
        W.wfill(x, cy + 22, z, x + 20, cy + 22, z, "minecraft:iron_block")
        W.wfill(x + 18, cy + 4, z, x + 18, cy + 21, z, "minecraft:iron_chain[axis=y]")
    for i, (dx, dz, af) in enumerate(((0, -14, 0.8), (0, 14, 0.45), (-2, 0, 0.2))):
        kaaszeppelin(W, (cx + dx, cy - 12 - i * 3, cz + dz), L=32 - i * 2, af=af, seed=i)
    # grauwe Mika-vlaggen
    for (x, z) in ((cx - 40, cz - 30), (cx + 40, cz + 30), (cx + 40, cz - 30)):
        VK.vlag(W, W.vx(x), W.vy(cy + 6), W.vz(z), (1, 0), kleur="minecraft:light_gray_wool",
                rand="minecraft:light_blue_wool", h=3, w=3, paal_h=6) if W.k == 1 else None
    W.label(cx, cy + 26, cz, "Kaaszeppelinwerf")


# ===================================================================================================== dalen
def schimmelboerderij(W):
    c = (-300, -300)
    if not W.raakt(c[0] - 90, 50, c[1] - 90, c[0] + 90, 120, c[1] + 90):
        return
    ketels = [(-320, -280), (-280, -330), (-345, -335), (-260, -270)]
    for i, (x, z) in enumerate(ketels):
        g = grond(W, x, z)
        W.cilinder(x, z, 7, int(g), int(g + 12), "minecraft:iron_block")
        W.cilinder(x, z, 7.2, int(g + 3), int(g + 9), None, hollow=1.2, mixfn=lambda xx, y, zz, m: W.kies(
            ["minecraft:light_blue_terracotta", "minecraft:cyan_terracotta"], [1, 1], xx, y // 2, zz, 900))
        W.bol((x, g + 13, z), 7, "minecraft:light_gray_concrete", ry=4, ymin=g + 12)
        W.wfill(x, g + 16, z, x, g + 22, z, "minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]")
        # pijpen naar de dichtstbijzijnde wortel (richting stam)
        r = math.hypot(x, z)
        p1 = (x * (r - 70) / r, g + 6, z * (r - 70) / r)
        W.buis([(x, g + 6, z), ((x + p1[0]) / 2, g + 14, (z + p1[2]) / 2), p1], [1.6, 1.6, 1.6],
               "minecraft:light_gray_concrete")
    # geparkeerde Mika-zeppelin
    x, z = -360, -250
    g = grond(W, x, z)
    W.bol((x, g + 30, z), 20, None, ry=8, rz=8, mixfn=lambda xx, y, zz, m: W.kies(
        ["minecraft:light_gray_wool", "minecraft:light_blue_terracotta", "minecraft:gray_wool"], [4, 2, 1],
        xx, y, zz, 901))
    W.wfill(x - 5, g + 20, z - 2, x + 5, g + 21, z + 2, "minecraft:gray_concrete")
    touw(W, (x, g + 20, z), (x + 3, g + 1, z + 6), HEK)
    W.label(-300, 110, -300, "Mika-schimmelboerderij")


def reuzenzwam(W, x, z, h, r, kleur="minecraft:purple_wool"):
    g = grond(W, x, z)
    W.cilinder(x, z, max(1.2, r * 0.2), int(g), int(g + h), STEEL)
    W.bol((x, g + h, z), r, None, ry=r * 0.45, ymin=g + h - 1, mixfn=lambda xx, y, zz, m: W.kies(
        [kleur, "minecraft:purple_concrete_powder", "minecraft:magenta_wool"], [5, 2, 0.6], xx, y, zz, 950))
    W.cilinder(x, z, r - 1, int(g + h - 1), int(g + h - 1), None, mixfn=lambda xx, y, zz, m: np.where(
        hash3(xx, y, zz, 951) < 0.35, W.id("guhs:lichtmosblok"), W.id("minecraft:verdant_froglight")))


def lichtkeverbos(W):
    if not W.raakt(-200, 50, -620, 200, 140, -330):
        return
    rng = np.random.default_rng(11)
    for _ in range(26):
        a = math.radians(270 + rng.uniform(-18, 18))
        r = rng.uniform(360, 600)
        x, z = r * math.cos(a), r * math.sin(a)
        reuzenzwam(W, x, z, rng.uniform(10, 26), rng.uniform(6, 13),
                   kleur=["minecraft:purple_wool", "minecraft:magenta_wool", "minecraft:purple_wool"][_ % 3])
    for _ in range(int(500 * W.k)):
        a = math.radians(270 + rng.uniform(-20, 20))
        r = rng.uniform(340, 620)
        x, z = r * math.cos(a), r * math.sin(a)
        y = boven(W, x, z, ymax=140)
        W.wset(x, y + rng.uniform(1, 12), z, "minecraft:verdant_froglight" if _ % 3 else "minecraft:ochre_froglight",
               keep=True) if _ % 2 else W.wset(x, y, z, "minecraft:firefly_bush", keep=True)
    W.label(0, 120, -470, "Lichtkeverbos")


def vijver(W, x, z, rr, g):
    """[zone 3 kaart] A walkable pond: 1-2 deep water (surface g - 1) in a bowl whose banks slope up (0.6 per block)."""
    if not W.raakt(x - rr - 24, g - 8, z - rr - 24, x + rr + 24, g + 24, z + rr + 24):
        return
    D = np.hypot(W.XX - x, W.ZZ - z)
    m = D < rr + 22
    if not m.any():
        return
    s = np.floor(g - 1 + np.clip(D - rr, 0, None) * 0.6)
    gt = boom.grondhoogte(W.XX, W.ZZ)
    s = np.minimum(s, np.floor(gt + 0.5))
    W.kolom(s + 1, np.full(D.shape, g + 22.0), "minecraft:air", mask=m)
    W.kolom(np.full(D.shape, g - 6.0), s, "minecraft:dirt", mask=m, keep=True)
    W.kolom(s, s, W.kies(["minecraft:grass_block", "minecraft:moss_block", "minecraft:mud"], [4, 2, 1], W.XX, 0, W.ZZ, 973), mask=m & (D >= rr))
    W.kolom(np.full(D.shape, g - 3.0), np.full(D.shape, g - 2.0), "minecraft:mud", mask=D < rr)
    W.kolom(np.full(D.shape, g - 1.0), np.full(D.shape, g - 1.0), "minecraft:water", mask=D < rr)


def paddenbloesemdal(W):
    c = (-300, 300)
    if not W.raakt(c[0] - 150, 50, c[1] - 150, c[0] + 150, 130, c[1] + 150):
        return
    rng = np.random.default_rng(12)
    for i in range(7):
        a = math.radians(135 + rng.uniform(-14, 14))
        r = rng.uniform(300, 560)
        x, z, rr = r * math.cos(a), r * math.sin(a), rng.uniform(8, 16)
        g = grond(W, x, z)
        vijver(W, x, z, rr, g)   # [zone 3 kaart] shallow pond with sloping banks (was a 4-deep bowl with walls)
        for _ in range(int(rr * 2)):
            aa, d = rng.uniform(0, 6.28), rng.uniform(0, rr * 0.8)
            W.wset(x + d * math.cos(aa), g, z + d * math.sin(aa), "minecraft:lily_pad", keep=True)
    for i in range(40):
        a = math.radians(135 + rng.uniform(-20, 20))
        r = rng.uniform(280, 600)
        x, z = r * math.cos(a), r * math.sin(a)
        g = boven(W, x, z, ymax=130)
        if i % 4 == 0:
            for k in range(1, 8):
                W.wset(x, g + k - 1, z, "guhs:guhbloesem_log[axis=y]")
            W.bol((x, g + 9, z), 5, None, ry=3.5, keep=True, mixfn=lambda xx, y, zz, m: W.kies(
                ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves"], [1, 1], xx, y, zz, 960))
        else:
            W.bol((x, g + 1, z), rng.uniform(1.8, 3.2), "minecraft:cherry_leaves", ry=1.6, keep=True)
    W.label(-300, 110, 300, "Paddenbloesemdal")


def honingmoeras(W):
    if not W.raakt(150, 50, 150, 520, 140, 520):
        return
    rng = np.random.default_rng(13)
    for i in range(18):
        a = math.radians(45 + rng.uniform(-17, 14))
        r = rng.uniform(230, 560)
        x, z, rr = r * math.cos(a), r * math.sin(a), rng.uniform(12, 26)
        if math.hypot(x - MOSVOET[0], z - MOSVOET[2]) < 45:
            continue
        g = grond(W, x, z)
        vijver(W, x, z, rr, g)   # [zone 3 kaart] shallow pond with sloping banks (was a 4-deep bowl with walls)
        # honingeilandje
        W.bol((x + rr * 0.2, g - 1, z), rr * 0.3, None, ry=2, mixfn=lambda xx, y, zz, m: W.kies(
            ["minecraft:honeycomb_block", "minecraft:honey_block", "minecraft:resin_block"], [3, 2, 1], xx, y, zz, 970))
        # vlonderpad over het water
        for t in np.linspace(-1, 1, int(rr * 3)):
            W.wset(x - rr * t, g, z + rr * 0.5 * t, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    # sapwatervallen vanaf de kruinrand
    for a in (math.radians(30), math.radians(52)):
        rr = float(boom.kruinrand(np.array([a]))[0]) + 2
        x, z = rr * math.cos(a), rr * math.sin(a)
        if W.b[4] < 200 or not W.raakt(x - 10, 60, z - 10, x + 10, 210, z + 10):
            continue
        for y in range(64, 212):
            for w in range(-3, 4):
                W.wset(x - math.sin(a) * w, y, z + math.cos(a) * w,
                       "minecraft:white_wool" if hash3(w, y, a * 10, 971) < 0.12 else "minecraft:water")
        g = grond(W, x, z)
        W.bol((x, g + 3, z), 9, None, ry=4, keep=True, mixfn=lambda xx, y, zz, m: W.kies(
            ["minecraft:white_wool", "guhs:pluiswolblok"], [2, 1], xx, y, zz, 972))
    W.label(330, 100, 300, "Honingmoeras")


def wortelgewelf(W):
    c = (340, -150)
    if not W.raakt(c[0] - 80, 50, c[1] - 80, c[0] + 80, 140, c[1] + 80):
        return
    schors = lambda x, y, z, m: np.where(boom.rot_mask(W, x, y, z, -0.1), W.kies(boom.ROT, boom.ROT_W, x, y, z, 980),
                                         W.kies(boom.SCHORS, boom.SCHORS_W, x, y, z, 981))
    for (p0, p1, p2) in (((290, 58, -210), (340, 118, -150), (392, 58, -92)),
                         ((300, 58, -80), (342, 112, -148), (380, 58, -220))):
        L = boom._spline([p0, p1, p2], 14)
        W.buis(L, np.interp(np.linspace(0, 1, len(L)), [0, 0.5, 1], [10, 7, 10]), None, mixfn=schors)
    g = grond(W, c[0], c[1])
    W.wset(c[0], g + 1, c[1], "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
    rng = np.random.default_rng(14)
    for _ in range(60):
        W.wset(c[0] + rng.uniform(-30, 30), g + rng.uniform(4, 40), c[1] + rng.uniform(-30, 30),
               "minecraft:verdant_froglight", keep=True)
    W.label(c[0], 126, c[1], "Wortelgewelf")


def wortelpoort(W):
    x, z = 0, 268
    if not W.raakt(x - 30, 40, z - 30, x + 30, 110, z + 30):
        return
    g = grond(W, x, z)
    W.buis([(x - 11, g - 2, z + 3), (x - 9, g + 14, z), (x, g + 20, z - 2), (x + 9, g + 14, z), (x + 11, g - 2, z + 3)],
           [3.5, 3, 3, 3, 3.5], None, mixfn=lambda xx, y, zz, m: W.kies(boom.SCHORS, boom.SCHORS_W, xx, y, zz, 990))
    W.buis([(x, g + 3, z + 14), (x, g - 2, z), (x, g - 8, z - 16)], [8, 8, 8], "minecraft:air", plat=1.0)
    for dx in (-7, 7):
        W.wfill(x + dx, g + 1, z + 6, x + dx, g + 4, z + 6, HOUT)
        W.wset(x + dx, g + 5, z + 6, "minecraft:lantern[hanging=false,waterlogged=false]")
    W.label(x, g + 26, z, "Grote Wortelpoort")


POSTEN = [(math.radians(270), 520), (math.radians(225), 520), (math.radians(135), 500), (math.radians(45), 540)]


def pluimpost(W, x, z):
    g = grond(W, x, z)
    W.wfill(x - 4, g, z - 3, x + 4, g, z + 3, PLANK)
    for (dx, dz) in ((-4, -3), (4, -3), (-4, 3), (4, 3)):
        W.wfill(x + dx, g + 1, z + dz, x + dx, g + 4, z + dz, HOUT)
    for k in range(4):
        W.wfill(x - 5 + k, g + 5 + k, z - 4, x - 5 + k, g + 5 + k, z + 4, "guhs:pluisdak")
        W.wfill(x + 5 - k, g + 5 + k, z - 4, x + 5 - k, g + 5 + k, z + 4, "guhs:pluisdak")
    W.wset(x + 2, g + 1, z + 1, "minecraft:water_cauldron[level=3]")
    W.wset(x - 2, g + 1, z + 1, "minecraft:hay_block")
    W.wset(x, g + 4, z + 3, LAMP_G)
    W.wguh(x + 0.5, g + 1, z - 1.5, 180)


def pluimposten(W):
    for (a, r) in POSTEN:
        x, z = r * math.cos(a), r * math.sin(a)
        if W.raakt(x - 12, 50, z - 12, x + 12, 120, z + 12):
            pluimpost(W, x, z)


def kruinring(W):
    """De Kruinring: een rijpad over de bladkussens (Pluimstal -> Kruintop -> terug), met kampvuurtjes."""
    if W.b[4] < 200 or not hasattr(W, "_kruin_top"):
        return
    XX, ZZ, TH = W.XX, W.ZZ, W.TH
    ring = 225 + 75 * np.cos(TH)
    pad = (np.abs(W.RR - ring) < 2.4) | ((np.abs(XX - 0) < 2.4) & (ZZ < -60) & (ZZ > -150))
    top = W._kruin_top
    W.kolom(np.round(top), np.round(top), "minecraft:dirt_path", mask=pad & (W.RR < 330))
    W.kolom(np.round(top) + 1, np.round(top) + 6, "minecraft:air", mask=pad & (W.RR < 330))
    for k in range(6):
        a = k * 2 * math.pi / 6 + 0.3
        rr = 225 + 75 * math.cos(a) + 5
        x, z = rr * math.cos(a), rr * math.sin(a)
        t, _ = boom.kruin_top(np.array([x]), np.array([z]))
        W.wset(x, t[0] + 1, z, "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")
        W.wfill(x - 2, t[0], z - 2, x + 2, t[0], z + 2, PLANK)


def mika_steiger(W):
    """Mika-zeppelinaanlegsteiger op de kruin (NW): grauwe spuitinstallatie en een zeppelin."""
    x, z = -190, -200
    if not W.raakt(x - 50, 190, z - 50, x + 50, 300, z + 50):
        return
    t, _ = boom.kruin_top(np.array([x]), np.array([z]))
    y = float(t[0]) + 1
    W.wfill(x - 10, y, z - 10, x + 10, y, z + 10, "minecraft:gray_concrete")
    W.wfill(x - 10, y + 1, z - 10, x + 10, y + 1, z - 10, "minecraft:iron_bars[east=true,north=false,south=false,west=true,waterlogged=false]")
    for (dx, dz) in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
        W.cilinder(x + dx, z + dz, 2.5, int(y + 1), int(y + 7), "minecraft:light_blue_terracotta")
        W.wset(x + dx, y + 8, z + dz, "minecraft:iron_block")
    W.wfill(x, y + 1, z, x, y + 26, z, "minecraft:iron_block")
    W.bol((x + 4, y + 34, z), 18, None, ry=7, rz=7, mixfn=lambda xx, yy, zz, m: W.kies(
        ["minecraft:light_gray_wool", "minecraft:light_blue_terracotta", "minecraft:gray_wool"], [4, 2, 1],
        xx, yy, zz, 1001))
    W.wfill(x - 2, y + 25, z - 2, x + 8, y + 26, z + 2, "minecraft:gray_concrete")
    W.label(x, y + 46, z, "Mika-steiger")


def bouw(W):
    for f in (hangnest, mosvoet, schorsstad, hartslag, schimmelboerderij,   # [zone 3 kaart] werf: bouw3 builds the dungeon lichtkeverbos, paddenbloesemdal,
              honingmoeras, wortelgewelf, wortelpoort, pluimposten, kruinring, mika_steiger):
        f(W)
    return W
