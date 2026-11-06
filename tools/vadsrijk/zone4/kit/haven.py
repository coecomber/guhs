"""Lampionhaven, de hub van zone 4: een vrolijk havenstadje op palen en duinen rond een beschutte haveninham.

    bouw(W)      stempelt het stadje in venster W (één keer gebouwd, daarna uit de cache)
    vrij(x, z)   False als daar een bouwwerk staat (zodat er geen palm door een dak groeit)

Indeling (spelcoördinaten, zie kust.BAAI): de inham ligt aan de oostkant (x 1868-1915, z -488..-440) met een
noord- en een zuidarm. Kade langs de westoever met drie steigers en bootjes; paalwoningen langs beide armen;
het visrestaurant op palen aan de noordarm; het vuurtorentje met oortjes op de punt van de zuidarm; het
Havenplein met de vliegerwinkel en de lampionwerkplaats; huisjes in terrassen tegen de Lampionduin; boven op de
duin het uitkijkpunt; aan de westkant de luchthaven met een aangemeerd luchtschip.
"""
import math
import numpy as np
import kust as KU
import bouwsels as BW

ZEE = KU.ZEE
VAK = (1730, 50, -566, 1935, 132, -378)
STRAAT_VENSTER = (1834, 52, -506, 1914, 104, -444)
STRAAT_YAW = -24
_P = None
_VRIJ = None


STATISCH_LUCHTSCHIP = False   # T-INT: the moving t-luchtschip ship replaces the anchored one


def _y(x, z):
    return KU.hoogte(x, z)[0]


def _ymax(x1, z1, x2, z2):
    return max(_y(x, z) for x in range(x1, x2 + 1, 2) for z in range(z1, z2 + 1, 2))


def bouw_plek(leven=True):
    P = BW.Plek(*VAK)
    kl = BW.MUREN
    # ------------------------------------------------------------------ kade langs de westoever van de inham
    KY = ZEE + 3   # dekhoogte kade/steigers (65)
    for z in range(-490, -437):
        for x in range(1860, 1871):
            yb = _y(x, z)
            if yb <= KY:
                P.s(x, KY, z, "minecraft:cherry_planks" if x > 1862 else "guhs:knuffelklinkers")
                if yb < KY - 1 and (x - 1860) % 4 == 2 and z % 4 == 0:
                    BW.palen(P, x, z, KY - 1)
                elif yb < KY:
                    P.f(x, max(yb, KY - 3), z, x, KY - 1, z, "minecraft:pink_terracotta") if yb > ZEE else None
    for z in range(-490, -437, 6):
        BW.lantaarnpaal(P, 1870, KY + 1, z, z)
    # drie steigers met bootjes
    for i, z in enumerate((-478, -464, -451)):
        BW.steiger(P, 1871, z - 1, 1892, z + 1, KY - 1, hek=False)
        BW.bootje(P, 1882, ZEE + 1, z - 4, "east", zeilkleur=["minecraft:pink_wool", "minecraft:light_blue_wool",
                                                              "minecraft:yellow_wool"][i])
        if i != 1:
            BW.zeilboot_groot(P, 1884, ZEE + 1, z + 5, "east",
                              kleur=["minecraft:magenta_wool", "minecraft:pink_wool", "minecraft:lime_wool"][i])
        # [zone 4 kaart] the string hangs 2 higher (the lampions were at head height on the jetty)
        BW.lampionsnoer(P, (1871.5, KY + 5.2, z + 0.5), (1892.5, KY + 4.2, z + 0.5), zak=1.0, seed=i)
        for x in (1871, 1892):
            P.f(x, KY, z, x, KY + 4, z, "minecraft:cherry_fence")
    P.lab.append((1882, KY + 4, -464, "steigers met bootjes"))
    # ------------------------------------------------------------------ paalwoningen langs de noordarm (kijken zuid)
    huizen = []
    x = 1868
    j = 0
    while x < 1904:
        w = 5 + (j % 3)
        d = 7
        z0 = -497
        y0 = KY + 1   # [zone 4 kaart] one level with the quay (the house is cut into the dune; bouw4 carves the interior)
        h = 6 + (j * 7) % 4
        huizen.append(BW.huis(P, x, y0, z0, w, d, h, "south", kl[j % len(kl)], BW.DAKEN[j % 4], oortjes=j % 2 == 0,
                              op_palen=True))
        # loopplank voor de deur, boven het water
        P.f(x, y0 - 1, z0 + d, x + w - 1, y0 - 1, z0 + d + 1, "minecraft:cherry_planks")
        x += w + 1
        j += 1
    # visrestaurant op de punt van de noordarm, met terras boven het water
    xr, zr = 1904, -500
    yr = KY + 1
    BW.huis(P, xr, yr, zr, 11, 8, 6, "south", "pastelbaksteen_hemelsblauw", "guh_dakpan_wit", oortjes=True,
            op_palen=True, luifel="minecraft:light_blue_wool")
    P.f(xr - 1, yr - 1, zr + 8, xr + 11, yr - 1, zr + 15, "minecraft:cherry_planks")
    for px in range(xr - 1, xr + 12, 4):
        BW.palen(P, px, zr + 15, yr - 2)
        P.s(px, yr, zr + 15, "minecraft:cherry_fence")
    for (px, pz, kleur) in ((xr + 1, zr + 11, "minecraft:light_blue_wool"), (xr + 6, zr + 12, "minecraft:pink_wool"),
                            (xr + 10, zr + 11, "minecraft:light_blue_wool")):
        P.s(px, yr, pz, "guhs:guh_tafel[facing=south]")
        BW.parasol(P, px, yr + 1, pz, kleur, r=1)
    P.s(xr + 5, yr + 3, zr + 8, "guhs:kermiskraam_vissen[facing=south]")
    P.lab.append((xr + 5, yr + 10, zr + 4, "Visrestaurant 'De Blije Bot'"))
    # ------------------------------------------------------------------ paalwoningen langs de zuidarm (kijken noord)
    x = 1874
    j = 3
    while x < 1904:
        w = 5 + (j % 2)
        z0 = -441
        y0 = max(KY + 1, _ymax(x, z0, x + w - 1, z0 + 6) + 1)
        BW.huis(P, x, y0, z0, w, 7, 6 + j % 3, "north", kl[(j * 3) % len(kl)], BW.DAKEN[(j + 1) % 4],
                oortjes=j % 2 == 1, op_palen=True)
        P.f(x, y0 - 1, z0 - 2, x + w - 1, y0 - 1, z0 - 1, "minecraft:cherry_planks")
        x += w + 1
        j += 1
    # vuurtorentje met oortjes op de punt van de zuidarm
    vx, vz = 1912, -431
    BW.vuurtoren(P, vx, _y(vx, vz) + 1, vz, h=20, r=3.6, label="vuurtorentje met oortjes")
    # ------------------------------------------------------------------ Havenplein + huisjes eromheen
    py = _y(1848, -462) + 1
    for x in range(1836, 1861):
        for z in range(-476, -449):
            if (x - 1848) ** 2 / 150 + (z - -462) ** 2 / 180 <= 1.0:
                yb = _y(x, z)
                P.f(x, min(yb, py - 1), z, x, py - 1, z, "guhs:knuffelklinkers")
                P.f(x, py, z, x, py + 6, z, "minecraft:air")
    # fonteintje met een guh-beeldje
    P.f(1846, py, -464, 1850, py, -460, "guhs:pastelbaksteen_wolkje")
    P.f(1847, py, -463, 1849, py, -461, "minecraft:water")
    P.s(1848, py + 1, -462, "guhs:baltoguh_beeldje[facing=south]")
    # midden: een hoge mast waar de lampionsnoeren als een tentje vanaf lopen
    P.f(1848, py + 1, -458, 1848, py + 10, -458, "minecraft:stripped_cherry_log[axis=y]")
    P.s(1848, py + 11, -458, "guhs:knuffelblok_roze")
    for a in range(8):
        h = a * math.pi / 4
        BW.lampionsnoer(P, (1848.5, py + 10.5, -457.5),
                        (1848.5 + math.cos(h) * 13, py + 5, -457.5 + math.sin(h) * 12), zak=1.2, seed=a,
                        slinger=a % 2 == 0)
    P.lab.append((1848, py + 14, -460, "Havenplein"))
    # vliegerwinkel (noordkant plein), met vliegers op het dak
    yv = _ymax(1840, -488, 1849, -481) + 1
    BW.huis(P, 1840, yv, -488, 9, 7, 6, "south", "pastelbaksteen_boter", "guh_dakpan_roze", oortjes=True,
            luifel="minecraft:pink_wool")
    for i, (kl_, dx) in enumerate((((255, 120, 170), 1), ((140, 220, 250), 4), ((255, 230, 120), 7))):
        BW.vlieger(P, (1840 + dx + 0.5, yv + 10, -484.5), (1840 + dx + 2.5, yv + 17 + i * 2, -486.5 + i), 1.6, kl_,
                   richting=(-0.3, 0.1, 1.0), seed=i)
    P.lab.append((1844, yv + 14, -484, "Vliegerwinkel"))
    # lampionwerkplaats (zuidwest), met een afdak vol lampions
    yl = _ymax(1826, -452, 1835, -444) + 1
    BW.huis(P, 1826, yl, -452, 9, 8, 5, "east", "pastelbaksteen_perzik", "dakpan_roze", oortjes=False)
    for z in range(-452, -444):
        P.s(1836, yl + 3, z, "guhs:tiki_rietdak_plaat[type=bottom]")
        P.s(1837, yl + 3, z, "guhs:tiki_rietdak_plaat[type=bottom]")
        if z % 2 == 0:
            P.s(1836, yl + 2, z, BW.LAMPIONS[(z // 2) % 3])
            P.s(1837, yl + 2, z, BW.LAMPIONS[(z // 2 + 1) % 3])
    P.f(1838, yl, -452, 1838, yl + 2, -452, "minecraft:cherry_fence")
    P.f(1838, yl, -445, 1838, yl + 2, -445, "minecraft:cherry_fence")
    P.s(1837, yl, -449, "guhs:guh_tafel[facing=east]")
    P.lab.append((1830, yl + 10, -448, "Lampionwerkplaats"))
    # rij huisjes ten westen en zuiden van het plein
    for i, (hx, hz, w, d, fac) in enumerate(((1822, -470, 7, 8, "east"), (1822, -484, 7, 9, "east"),
                                             (1840, -444, 7, 7, "north"), (1849, -444, 6, 7, "north"),
                                             (1857, -446, 6, 7, "north"), (1812, -456, 6, 8, "east"),
                                             (1810, -440, 8, 7, "north"), (1824, -436, 7, 7, "north"),
                                             (1836, -430, 8, 7, "north"), (1850, -432, 7, 7, "north"),
                                             (1864, -432, 6, 6, "north"), (1820, -420, 7, 7, "north"),
                                             (1836, -416, 7, 7, "north"), (1852, -418, 6, 6, "north"))):
        yy = _ymax(hx, hz, hx + w - 1, hz + d - 1) + 1
        BW.huis(P, hx, yy, hz, w, d, 5 + (i * 5) % 4, fac, kl[(i * 7 + 2) % len(kl)], BW.DAKEN[i % 4],
                oortjes=i % 3 == 0)
    # snoeren tussen de huisjes langs de straat ten zuiden van het plein
    for i, (a, b) in enumerate((((1843, -444), (1843, -437)), ((1852, -444), (1852, -438)),
                                ((1828, -436), (1828, -444)), ((1860, -446), (1866, -438)))):
        ya, yb = _y(*a) + 6, _y(*b) + 6
        BW.lampionsnoer(P, (a[0] + 0.5, ya, a[1] + 0.5), (b[0] + 0.5, yb, b[1] + 0.5), zak=1.0, seed=i, slinger=True)
    # ------------------------------------------------------------------ terrassen tegen de Lampionduin
    for i, (hx, hz) in enumerate(((1812, -500), (1822, -502), (1832, -505), (1852, -497), (1862, -500),
                                  (1872, -508), (1816, -516), (1830, -520), (1876, -520), (1806, -488))):
        w, d = 6 + i % 2, 6
        yy = _ymax(hx, hz, hx + w - 1, hz + d - 1) + 1
        BW.huis(P, hx, yy, hz, w, d, 5, "south", kl[(i * 3 + 1) % len(kl)], BW.DAKEN[(i + 2) % 4], oortjes=i % 2 == 1)
    # trappenpad van het plein naar de top van de duin, met lampionpaaltjes
    pad = [(1850, -476), (1852, -484), (1857, -492), (1860, -500), (1858, -508), (1860, -516)]
    for (a, b) in zip(pad, pad[1:]):
        n = int(math.dist(a, b) * 1.5) + 1
        for t in range(n + 1):
            x = a[0] + (b[0] - a[0]) * t / n
            z = a[1] + (b[1] - a[1]) * t / n
            for o in (-1, 0, 1):
                xx, zz = int(x) + o, int(z)
                P.s(xx, _y(xx, zz), zz, "guhs:knuffelklinkers")
    for i, (x, z) in enumerate(pad[1:]):
        BW.lantaarnpaal(P, x + 2, _y(x + 2, z) + 1, z, i)
    # Lampionduin: uitkijkpunt bovenop
    ux, uz = 1860, -517
    uy = _y(ux, uz) + 1
    P.f(ux - 3, uy - 1, uz - 3, ux + 3, uy - 1, uz + 3, "guhs:pastelbaksteen_wolkje")
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        BW.lantaarnpaal(P, ux + dx, uy, uz + dz, dx + dz)
    P.s(ux, uy, uz - 2, "guhs:guh_bankje_roze[facing=north]")
    P.f(ux, uy, uz + 2, ux, uy + 7, uz + 2, "minecraft:cherry_fence")
    for i in range(3):
        for jj in range(1, 4):
            P.s(ux + jj, uy + 7 - i, uz + 2, "minecraft:pink_wool" if (i + jj) % 2 else "minecraft:white_wool")
    P.lab.append((ux, uy + 10, uz, "Lampionduin (uitkijk naar de Boom)"))
    # ------------------------------------------------------------------ luchthaven (west) met aangemeerd luchtschip
    lx, lz = 1796, -470
    ly = 70
    P.f(lx - 5, ly, lz - 6, lx + 5, ly, lz + 6, "guhs:roze_lakhout_planken")
    for (dx, dz) in ((-5, -6), (5, -6), (-5, 6), (5, 6), (0, -6), (0, 6)):
        for y in range(_y(lx + dx, lz + dz), ly):
            P.s(lx + dx, y, lz + dz, "minecraft:stripped_cherry_log[axis=y]")
    for dx in range(-5, 6):
        for dz in (-6, 6):
            P.s(lx + dx, ly + 1, lz + dz, "guhs:guh_hekje_wit")
    # trap omhoog
    y0 = _y(lx + 6, lz - 6)
    for i in range(ly - y0):
        P.f(lx + 6, y0 + i + 1, lz - 6 + i, lx + 7, y0 + i + 1, lz - 6 + i, "minecraft:cherry_planks")
    # vertrekbord
    P.f(lx + 3, ly + 1, lz, lx + 3, ly + 3, lz, "minecraft:cherry_fence")
    P.f(lx + 2, ly + 4, lz, lx + 4, ly + 5, lz, "guhs:pastelbaksteen_framboos")
    P.lab.append((lx, ly + 18, lz, "luchthaven (lijn C naar de Boom)"))
    # T-INT: no anchored block ship any more: t-luchtschip's real line-C ship docks here (haven_3_4_luchtschip) on a timetable
    try:
        if not STATISCH_LUCHTSCHIP:
            raise RuntimeError("t-luchtschip docks the real ship here")
        import kamp as KP
        import schemerkit as SK
        S = SK.Scene(50, 40, 22)
        KP.luchtschip(S, 2, 10, 11, L=40)
        S.guhs = []
        ax, ay, az = P.anker
        # schip ten westen van het platform, boeg naar het oosten, dek op ly
        ox, oy, oz = lx - 46 - ax, ly - 10 - ay, lz - 11 - az
        sl = S.v
        X, Y, Z = P.v.shape
        for (i, j, k) in np.argwhere(sl > 0):
            xx, yy, zz = ox + i, oy + j, oz + k
            if 0 <= xx < X and 0 <= yy < Y and 0 <= zz < Z:
                P.v[xx, yy, zz] = P.id(S.pal[sl[i, j, k]])
    except Exception as e:  # noqa: BLE001
        print("luchtschip overgeslagen:", e)
    # ------------------------------------------------------------------ leven
    if leven:
        guhs = [(1866, KY + 1, -470, 90, "guh"), (1867, KY + 1, -458, 200, "guh_mint"),
                (1880, KY, -464, 90, "guh_golden"), (1889, KY, -478, 270, "guh_pinguh"),
                (1846, py, -468, 160, "guh"), (1851, py, -455, 20, "guh_mint"), (1844, py, -456, 300, "guh_mager"),
                (1845, py, -457, 280, "guh_mager"), (1854, py, -466, 220, "guh_pluisguh"),
                (1905, yr, -489, 180, "guh"), (1910, yr, -490, 160, "guh_golden"),
                (1838, yl, -448, 270, "guh"), (1860, uy, -515, 300, "guh_mint")]
        for (x, y, z, yaw, sk) in guhs:
            P.guhje(x + 0.5, y, z + 0.5, yaw, sk)
        # zittende guhs aan het restaurant
        P.guhje(xr + 2.5, yr, zr + 11.5, 270, "guh", zit=True)
        P.guhje(xr + 5.5, yr, zr + 12.5, 90, "guh_pluisguh", zit=True)
        # vlinders en zeemeeuwen-stipjes boven de haven
        rng = np.random.default_rng(3)
        for i in range(30):
            P.dl.append((1840 + rng.uniform(0, 70), ZEE + 10 + rng.uniform(0, 25), -500 + rng.uniform(0, 60),
                         (255, 255, 255) if i % 3 else (255, 170, 210), 0.4, 0, "vlinder" if i % 3 == 0 else "stip"))
    P.lab.append((1850, 110, -470, "Lampionhaven"))
    return P


def _plek(leven=True):
    global _P, _VRIJ
    if _P is None:
        _P = bouw_plek(leven)
        _VRIJ = set(_P.ankers)
        X, Y, Z = _P.v.shape
        ax, ay, az = _P.anker
        bez = np.argwhere((_P.v[:, ZEE + 2 - ay:, :] > 0).any(axis=1))
        _VRIJ |= {(int(i) + ax, int(k) + az) for (i, k) in bez}
    return _P


def vrij(x, z):
    if _VRIJ is None:
        return True
    for dx in (-2, 0, 2):
        for dz in (-2, 0, 2):
            if (x + dx, z + dz) in _VRIJ:
                return False
    return True


def bouw(W, leven=True):
    if not W.raakt(*VAK):
        return
    _plek(leven).zet(W)
