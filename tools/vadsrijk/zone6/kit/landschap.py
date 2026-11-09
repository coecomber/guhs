"""Landschap-stukken van het Zijdeland (spelcoördinaten): de vijf sterrenbakens, het karavaankamp,
De Sterrendovende Toren (raid) bij de Moederoase, de ingang van De Verzonken Sterrenwacht (kerker) en van de
Zingende Zoutgrot. bouw(W) zet alles wat in het venster valt.
"""
import math
import numpy as np
import zijbouw as ZB
import zijwereld as ZW
from zijbouw import zet, doos, leeg, raakt, rng_van, lantaarn
from zpalet import Z, WOL, TAPIJT, b, trap, plaat

# naam -> (x, z, aan?)  aan = al weer aangestoken in de renders (de Zoutvlakte-render toont het moment van aansteken)
BAKENS = {"palmbaken": (3478, -906, True), "duinbaken": (3655, -560, False), "zoutbaken": (3930, -612, True),
          "ruinebaken": (4045, -1000, False), "oasebaken": (4062, -842, False)}


# ============================================================================================ sterrenbaken
def ster(W, cx, cy, cz, r=4.0, kern="minecraft:pearlescent_froglight", punt="minecraft:sea_lantern", aan=True):
    """Een 3D-ster (8 punten) van lichtblokken; uit = grauw."""
    if not aan:
        kern, punt = "grauw", "grauw2"
    for dx in range(-int(r) - 1, int(r) + 2):
        for dy in range(-int(r) - 1, int(r) + 2):
            for dz in range(-int(r) - 1, int(r) + 2):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d < 1.6:
                    zet(W, cx + dx, cy + dy, cz + dz, kern)
                    continue
                # punten langs de 6 assen + 4 schuine (in het xy-vlak)
                for (ax, ay, az) in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1),
                                     (0.7, 0.7, 0), (-0.7, 0.7, 0), (0.7, -0.7, 0), (-0.7, -0.7, 0)):
                    t = dx * ax + dy * ay + dz * az
                    if t <= 0:
                        continue
                    lat = math.sqrt(max(0, d * d - t * t))
                    lim = r * (1.0 if abs(ay) < 0.1 or abs(ax) > 0.1 else 1.0)
                    if t <= lim and lat <= 1.4 * (1 - t / (lim + 0.5)):
                        zet(W, cx + dx, cy + dy, cz + dz, punt)
                        break


def ring(W, cx, cy, cz, R, kanteling=0.0, rond_as=0.0, dik=0.7, k="goudblok", juweel="minecraft:magenta_stained_glass",
         stappen=None):
    """Een (gekantelde) ring rond (cx, cy, cz): torus met straal R."""
    n = stappen or int(R * 9)
    ca, sa = math.cos(kanteling), math.sin(kanteling)
    cb, sb = math.cos(rond_as), math.sin(rond_as)
    for i in range(n):
        a = i * 2 * math.pi / n
        x, y, z = R * math.cos(a), 0.0, R * math.sin(a)
        y, z = y * ca - z * sa, y * sa + z * ca           # kantelen om de x-as
        x, z = x * cb - z * sb, x * sb + z * cb           # draaien om de y-as
        for ox in (-dik, 0, dik):
            for oy in (-dik, 0, dik):
                zet(W, cx + x + ox, cy + y + oy, cz + z, juweel if i % 9 == 0 and ox == 0 and oy == 0 else k)


def sterrenbaken(W, x, z, aan=True, naam=None):
    """Sterrenbaken: getrapte achthoekige sokkel, vier slanke pijlers met bogen en tegelbanden, een open
    bovenkamer met de ster, twee gouden ringen er omheen (armillair), en op de grond een sterrenbeeld-puzzel."""
    y0 = ZW.hoogte(W, x, z)
    if not raakt(W, x - 30, y0 - 4, z - 30, x + 30, y0 + 48, z + 30):
        return
    # sokkel
    for st, r in enumerate((10, 8, 6)):
        for dx in range(-r - 1, r + 2):
            for dz in range(-r - 1, r + 2):
                a = math.atan2(dz, dx)
                sec = math.pi / 8
                rr = math.hypot(dx, dz) * math.cos((a % (2 * sec)) - sec)
                if rr <= r:
                    zet(W, x + dx, y0 + st, z + dz, "goud" if rr > r - 1 and st == 0 else ("wit_glad" if st < 2 else "vloer_dambord"))
    yb = y0 + 3
    h = 26
    for (px, pz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        for y in range(yb, yb + h):
            zet(W, x + px, y, z + pz, "wit_glad" if (y - yb) % 9 not in (6, 7) else ("tegel_turkoois" if aan else "grauw2"))
        zet(W, x + px, yb + h, z + pz, "zuilkop")
    for lvl in range(1, 4):
        yy = yb + lvl * 8
        for k in range(-3, 4):
            for (ax, az) in ((k, -3), (k, 3), (-3, k), (3, k)):
                top = ZB.boog_top(k, 6, 0, 0.6)
                if abs(k) < 3:
                    zet(W, x + ax, yy - 1 + int(top * 0.5), z + az, "goud")
                zet(W, x + ax, yy, z + az, "wit_glad")
    yt = yb + h + 1
    doos(W, x - 4, yt, z - 4, x + 4, yt, z + 4, "goud")
    for (px, pz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
        doos(W, x + px, yt + 1, z + pz, x + px, yt + 9, z + pz, "zuil_wit")
    doos(W, x - 4, yt + 10, z - 4, x + 4, yt + 10, z + 4, "goud")
    ZB.koepel(W, x + 0.5, yt + 11, z + 0.5, 4, k_dak="koepel_turkoois" if aan else "grauw2", vorm="ui", ribben=6,
              oortjes=True)
    ster(W, x, yt + 5, z, r=3.6, aan=aan)
    ring(W, x + 0.5, yt + 5.5, z + 0.5, 4.6, kanteling=0.5, rond_as=0.3, dik=0.0, k="goud")
    ring(W, x + 0.5, yt + 5.5, z + 0.5, 4.6, kanteling=-0.6, rond_as=1.4, dik=0.0, k="goud")
    if aan:
        for y in range(yt + 22, yt + 34):
            zet(W, x, y, z, "minecraft:end_rod[facing=up]")
    # sterrenbeeld-puzzel op de grond: een guh-hoofdje van gloeitegels met lijnen
    pts = [(-14, 0), (-9, -7), (0, -10), (9, -7), (14, 0), (9, 8), (0, 11), (-9, 8), (-13, -13), (13, -13),
           (-5, 1), (5, 1)]
    lijnen = [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 0), (1, 8), (3, 9)]
    rng = rng_van(x, z, 3)
    for i, (px, pz) in enumerate(pts):
        gx, gz = x + px * 1.3 + 18, z + pz * 1.3
        gy = ZW.hoogte(W, gx, gz) - 1
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if abs(dx) + abs(dz) < 2:
                    zet(W, gx + dx, gy, gz + dz, "minecraft:sea_lantern" if aan or i < 4 else "grauw2")
    for (a, c) in lijnen:
        (ax, az), (cx_, cz_) = pts[a], pts[c]
        n = int(max(abs(cx_ - ax), abs(cz_ - az)) * 1.3)
        for k in range(1, n):
            t = k / n
            gx, gz = x + (ax + (cx_ - ax) * t) * 1.3 + 18, z + (az + (cz_ - az) * t) * 1.3
            gy = ZW.hoogte(W, gx, gz) - 1
            zet(W, gx, gy, gz, "minecraft:white_glazed_terracotta" if aan else "grauw")
    if naam:
        W.label(x, yt + 22, z, naam)
    W.baken = (x, yt + 5, z)


# ============================================================================================ karavaankamp
def tent(W, x, y, z, r=4, h=6, kl=("roze", "wit"), seed=0, as_="x"):
    """Bedoeïenentent: lange lage tent op palen, dak van gestreepte wol (nok in het midden), open voorkant met
    tapijten, kussens en een lantaarn."""
    L, B_ = r + 3, r
    for l in range(-L, L + 1):
        for w in range(-B_, B_ + 1):
            dy = int(round((B_ - abs(w)) * 0.75))
            px, pz = (x + l, z + w) if as_ == "x" else (x + w, z + l)
            k = WOL[kl[(l // 2) % 2]]
            zet(W, px, y + 2 + dy, pz, k)
            if abs(w) == B_:
                zet(W, px, y + 1 + dy, pz, TAPIJT[kl[1]].replace("_carpet", "_wool") if l % 3 else k)
            if w > -B_ and abs(l) < L:
                zet(W, px, y, pz, TAPIJT[["roze", "lila", "turkoois", "geel"][(seed + l // 3) % 4]], keep=True)
    for l in (-L, 0, L):
        for w in (-B_, 0, B_):
            px, pz = (x + l, z + w) if as_ == "x" else (x + w, z + l)
            top = 2 + int(round((B_ - abs(w)) * 0.75))
            for dy in range(0, top):
                zet(W, px, y + dy, pz, "minecraft:stripped_cherry_log[axis=y]")
    # achterwand dicht, voorkant (+z of +x) open
    for l in range(-L, L + 1):
        px, pz = (x + l, z - B_) if as_ == "x" else (x - B_, z + l)
        for dy in range(0, 2):
            zet(W, px, y + dy, pz, WOL[kl[0]])
    for k_ in range(3):
        px, pz = (x - 2 + k_ * 2, z + 1) if as_ == "x" else (x + 1, z - 2 + k_ * 2)
        zet(W, px, y, pz, "guhs:pink_kussen" if k_ % 2 else "guhs:yellow_kussen")
    zet(W, x, y + 2 + int(B_ * 0.75) - 1, z, lantaarn(True))


def kamp(W, x, z, seed=0):
    y = ZW.hoogte(W, x, z)
    if not raakt(W, x - 20, y - 2, z - 20, x + 20, y + 12, z + 20):
        return
    zet(W, x, y, z, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    for k in range(8):
        a = k * math.pi / 4
        zet(W, x + round(math.cos(a) * 2.2), y, z + round(math.sin(a) * 2.2), "guhs:pink_kussen")
    for i, (dx, dz) in enumerate(((-9, -5), (7, -8), (-6, 8), (10, 6))):
        tent(W, x + dx, ZW.hoogte(W, x + dx, z + dz), z + dz, r=4 + (i % 2), h=6 + (i % 2),
             kl=[("roze", "wit"), ("turkoois", "wit"), ("geel", "oranje"), ("lila", "roze")][i], seed=i)
    W.kamp = (x, y, z)


# ============================================================================================ de toren
def sterrendovende_toren(W):
    """De raid: vier verdiepingen (Zandwachter, Spiegelvizier, Gedoofde Sterrenbeelden, Ringeling) in een gedraaide
    grauwe toren met roze-violette accenten en goud; gouden ringen zweven eromheen; bovenop de 'Lege Lens' die
    sterrenlicht naar binnen zuigt. Alleen hier is het land een beetje grauw."""
    tx, tz = ZW.TOREN
    y0 = ZW.hoogte(W, tx, tz)
    if not raakt(W, tx - 50, y0 - 6, tz - 50, tx + 50, y0 + 170, tz + 50):
        return
    # grauwe vlek rond de voet (het enige grauw van de zone)
    for dx in range(-30, 31):
        for dz in range(-30, 31):
            d = math.hypot(dx, dz)
            if d < 30 and ZB.rng_van(dx, dz, 7).rand() < (1 - d / 30) ** 1.2:
                gy = ZW.hoogte(W, tx + dx, tz + dz) - 1
                if not ZW.is_water(W, tx + dx, tz + dz):
                    zet(W, tx + dx, gy, tz + dz, ["grauw", "grauw2", "grauw3"][int(abs(dx * 7 + dz * 3)) % 3])
    # voet: achthoekige vesting
    def okt(dx, dz, rot=0.0):
        a = math.atan2(dz, dx) + rot
        sec = math.pi / 8
        return math.hypot(dx, dz) * math.cos((a % (2 * sec)) - sec)
    for y in range(y0 - 3, y0 + 14):
        for dx in range(-20, 21):
            for dz in range(-20, 21):
                d = okt(dx, dz)
                if 16.6 < d <= 18 or (y < y0 + 1 and d <= 18):
                    k = "grauw_steen" if (y // 4) % 3 else "grauw2"
                    if y == y0 + 12:
                        k = "goud"
                    zet(W, tx + dx, y, tz + dz, k)
                # [x-raid] the voet is SOLID inside (the iwan below carves the door niche out of it): its hollow inside was
                #   open to the niche at the sides and above the arch, a second way into the empty tower
                elif y0 + 1 <= y < y0 + 13 and d <= 16.6:
                    zet(W, tx + dx, y, tz + dz, "grauw_steen")
                if y == y0 + 13 and 16.6 < d <= 18:   # [x-raid] (was 18.4: the outer crenels hung over the wall edge)
                    zet(W, tx + dx, y + 1, tz + dz, "grauw_steen" if (dx + dz) % 3 else "minecraft:air")
                # [x-raid] the voet's roof: a closed deck over the whole octagon. It was open, so the shaft stood on nothing
                #   and the voet was the bottom of one ~150-block hollow tube (the user walked into it: "er gebeurt niks").
                if y == y0 + 13 and d <= 16.6:
                    zet(W, tx + dx, y, tz + dz, "grauw_steen")
    ZB.iwan(W, (-1, 0), tx - 17, tz + 0.5, y0 + 1, 9, 9, 4, buiten="grauw_steen", kader="koepel_lavendel",
            vak="grauw2", deur="minecraft:air", muqarnas="goud")
    # de schacht: 4 verdiepingen, steeds smaller, gedraaid (achthoek die per blok een beetje roteert)
    secties = [(16, 34, "grauw_steen", "koepel_lavendel"), (13, 32, "grauw_steen", "rots_roze"),
               (10.5, 30, "grauw2", "koepel_lavendel"), (8, 24, "grauw_steen", "goud")]
    y = y0 + 14
    rot = 0.0
    for si, (r, h, wand, accent) in enumerate(secties):
        for dy in range(h):
            rot += 0.012
            for dx in range(-int(r) - 2, int(r) + 3):
                for dz in range(-int(r) - 2, int(r) + 3):
                    d = okt(dx, dz, rot)
                    if r - 1.6 < d <= r:
                        k = wand
                        a = math.atan2(dz, dx) + rot
                        if dy % 8 in (3, 4, 5) and abs(math.sin(a * 4)) < 0.18:
                            k = "minecraft:purple_stained_glass"
                        if dy % 8 == 0:
                            k = accent
                        zet(W, tx + dx, y + dy, tz + dz, k)
                    # [x-raid] Ringeling's gold rings: a band of 2 ON the wall halfway up every section (they used to
                    #   float 4-10 blocks off the shaft, tilted, and read as loose pieces); a magenta jewel now and then
                    elif dy in (h // 2, h // 2 + 1) and r < d <= r + 1.0:
                        a = math.atan2(dz, dx)
                        zet(W, tx + dx, y + dy, tz + dz, "minecraft:magenta_stained_glass"
                            if dy == h // 2 and int((a + math.pi) * r) % 9 == 0 else "goudblok")
        y += h
        # rondgang met kantelen tussen de verdiepingen
        # [x-raid] the deck is CLOSED over the whole section. It was a ring r-1..r+2.4 only: the next, narrower section stood
        #   1.5-2 blocks inside its inner edge, so every joint had an open slit into the hollow shaft. The gold corbel under
        #   the balcony now steps out to r+1.4 (was r+1), so the 2.4 balcony sits on it.
        for dx in range(-int(r) - 3, int(r) + 4):
            for dz in range(-int(r) - 3, int(r) + 4):
                d = okt(dx, dz, rot)
                if d <= r + 2.4:
                    zet(W, tx + dx, y, tz + dz, "goud" if d > r + 1.4 else "grauw_steen")
                    if d > r + 1.4 and (dx + dz) % 2 == 0:
                        zet(W, tx + dx, y + 1, tz + dz, "grauw_steen")
                if r - 0.2 < d <= r + 1.4:
                    zet(W, tx + dx, y - 1, tz + dz, "goud")
        # grauwe Mika-vaandels
        for k in range(4):
            a = k * math.pi / 2 + rot
            bx, bz = tx + math.cos(a) * (r + 0.6), tz + math.sin(a) * (r + 0.6)
            for dy in range(2, 9):
                zet(W, bx, y - dy, bz, WOL["wit"].replace("white", "gray") if dy % 3 else WOL["lila"])
        y += 1
    # kroon: open ring van spitsen + de Lege Lens
    r = 8
    for k in range(10):
        a = k * 2 * math.pi / 10 + rot
        px, pz = tx + math.cos(a) * r, tz + math.sin(a) * r
        for dy in range(0, 12):              # [x-raid] all 10 spires reach the lens ring (half stopped 4 short)
            zet(W, px, y + dy, pz, "goud" if dy > 8 else "grauw_steen")
    # [x-raid] the Lege Lens rests ON the crown: its spokes + ring (lens_y) touch the spire tops (y + 11); it floated 3 blocks
    #   above them. A slim pedestal from the deck carries the glass ball (its bottom is at lens_y - 4).
    lens_y = y + 11
    for dy in range(0, 10):              # (up through the lens shell to its purple core)
        zet(W, tx, y + dy, tz, "grauw_steen" if dy < 6 else "goud")
    W.bol((tx + 0.5, lens_y + 0.5, tz + 0.5), 6.2, "minecraft:tinted_glass", ry=4.5, hollow=1.2)
    W.bol((tx + 0.5, lens_y + 0.5, tz + 0.5), 2.0, "minecraft:purple_stained_glass")
    ring(W, tx + 0.5, lens_y + 0.5, tz + 0.5, 8.5, kanteling=0.0, dik=0.6, k="goudblok")
    for k in range(5):
        a = k * 2 * math.pi / 5
        for s in np.arange(6.2, 8.5, 0.5):
            zet(W, tx + math.cos(a) * s, lens_y, tz + math.sin(a) * s, "goud")
    # [x-raid] (the four free-floating tilted rings are gone: they are the gold bands on the shaft wall now, see the sections)
    # sterrenlicht dat naar de lens wordt gezogen (deeltjes, in spiralen)
    rng = np.random.RandomState(5)
    for s in range(14):
        a0 = rng.uniform(0, 2 * math.pi)
        R0 = rng.uniform(70, 160)
        h0 = rng.uniform(40, 110)
        for i in range(70):
            t = i / 69
            a = a0 + t * 2.4
            R = R0 * (1 - t) + 2
            W.deeltjes.append((tx + math.cos(a) * R, lens_y + h0 * (1 - t) ** 1.3, tz + math.sin(a) * R,
                               (255, 236, 180) if i % 3 else (220, 200, 255), 0.35 + 0.25 * (1 - t), 0.9, "stip"))
    W.toren_top = (tx, lens_y, tz)
    W.label(tx, lens_y + 14, tz, "De Sterrendovende Toren (raid)")
    W.label(tx, y0 + 30, tz, "1 Zandwachter - 2 Spiegelvizier - 3 Gedoofde Sterrenbeelden - 4 Hertogin Ringeling")


# ============================================================================================ ingangen
def kerker_ingang(W):
    x, y, z = ZW.PLEK["verzonken_sterrenwacht"]
    y = ZW.hoogte(W, x, z)
    if not raakt(W, x - 16, y - 4, z - 16, x + 16, y + 24, z + 16):
        return
    # half in het zand gezakte koepelruïne met een gebroken sterrenkijker
    W.bol((x + 0.5, y - 2, z + 0.5), 11, b("rots_marmer"), ry=9, hollow=1.5)
    leeg(W, x - 2, y, z + 8, x + 2, y + 5, z + 13)
    for k in range(12):
        a = k * math.pi / 6
        if k % 3:
            for dy in range(0, 9 - (k % 4) * 2):
                zet(W, x + math.cos(a) * 14, y + dy, z + math.sin(a) * 14, "zuil")
    for i in range(10):
        zet(W, x + 3 + i * 0.8, y + 7 + i * 0.5, z - 2, "minecraft:copper_block")
    W.label(x, y + 14, z, "De Verzonken Sterrenwacht (kerker)")


def grot_ingang(W):
    x, y, z = ZW.PLEK["zoutgrot"]
    y = ZW.hoogte(W, x, z)
    if not raakt(W, x - 10, y - 10, z - 10, x + 10, y + 12, z + 10):
        return
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            d = math.hypot(dx, dz * 1.4)
            if d < 7:
                for dy in range(0, int(5 - d * 0.6)):
                    zet(W, x + dx, y + dy, z + dz, "zoutkristal" if (dx + dz) % 3 else "zout")
    leeg(W, x - 2, y - 6, z - 1, x + 2, y + 3, z + 6)
    for k in range(6):
        zet(W, x - 3 + k, y + 4, z + 3, "guhs:fossielmijn_zoutkristalletjes")
    W.label(x, y + 9, z, "De Zingende Zoutgrot")


def bouw(W):
    for naam, (x, z, aan) in BAKENS.items():
        sterrenbaken(W, x, z, aan=aan, naam=naam.replace("baken", "baken").capitalize())
    sterrendovende_toren(W)
    kerker_ingang(W)
    grot_ingang(W)
    kamp(W, 3906, -584)
