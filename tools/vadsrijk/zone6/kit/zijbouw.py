"""Bouwstenen voor het Zijdeland (zone 6), in WERELDcoördinaten op een schemerkit-Wereld (k=1).

Alles is deterministisch (seeds uit de positie), zodat dezelfde aanroep in elk venster hetzelfde gebouw geeft:
zo kan een shot een uitsnede bouwen en kan de kaartgenerator later precies dezelfde stad als tegels exporteren.

Richtingen: x oost, z zuid. 'kant' = (dx, dz) = de buitenkant waar een gevel naar kijkt.
Hoofdstukken: helpers, koepels, torens (minaret met guh-oortjes), bogen/iwan, huizen, groen, markt, water, licht.
"""
import math
import numpy as np
from zpalet import Z, WOL, TAPIJT, MUURTJE, b, trap, plaat

LUCHT = "minecraft:air"
FACE = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
TEGEN = {"east": "west", "west": "east", "north": "south", "south": "north"}


# ============================================================================================ helpers
def zet(W, x, y, z, k, keep=False):
    if k is None:
        return
    W.wset(int(math.floor(x)), int(math.floor(y)), int(math.floor(z)), b(k), keep=keep)


# [zone 6 kaart] game data recorded while building (maak6/poi6 export them; renders ignore them)
def plek(W, pid, x, y, z):
    """A named POI: the STAND position (the air block the feet are in), game coordinates. poi6 snaps it onto real blocks."""
    if not hasattr(W, "plekken"):
        W.plekken = {}
    W.plekken[pid] = (int(math.floor(x)), None if y is None else int(math.floor(y)), int(math.floor(z)))


def huis_rol(W, hid, rol, deur, binnen):
    """A house/shop with a role (woning, winkel_<soort>, herberg, bank, ...): its door block and a stand spot inside."""
    if not hasattr(W, "huizen"):
        W.huizen = []
    W.huizen.append(dict(id=hid, rol=rol, deur=[int(v) for v in deur], binnen=[int(v) for v in binnen]))


def doos(W, x1, y1, z1, x2, y2, z2, k, keep=False):
    if k is None:
        return
    W.wfill(min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2), b(k), keep=keep)


def leeg(W, x1, y1, z1, x2, y2, z2):
    W.wfill(min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2), LUCHT)


def raakt(W, x1, y1, z1, x2, y2, z2, marge=4):
    return W.raakt(min(x1, x2) - marge, min(y1, y2) - marge, min(z1, z2) - marge, max(x1, x2) + marge,
                   max(y1, y2) + marge, max(z1, z2) + marge)


def rng_van(*a):
    h = 0
    for v in a:
        h = (h * 1000003 + int(round(float(v) * 7))) & 0x7FFFFFFF
    return np.random.RandomState(h)


def muurtje(k):
    return MUURTJE.get(k, b(k))


def vlak(W, kant, c, u, y, k):
    """Zet een blok op muurvlak: kant=(dx,dz) normaal; c = vlakcoördinaat (x of z), u = langs het vlak."""
    dx, dz = kant
    if dx:
        zet(W, c, y, u, k)
    else:
        zet(W, u, y, c, k)


def luik(kant, half="top", open_=True, hout="minecraft:cherry_trapdoor"):
    return f"{hout}[facing={FACE[kant]},half={half},open={'true' if open_ else 'false'},powered=false,waterlogged=false]"


def lantaarn(hangend=True, soort="minecraft:lantern"):
    return f"{soort}[hanging={'true' if hangend else 'false'},waterlogged=false]"


def ketting(as_="y"):
    return f"minecraft:iron_chain[axis={as_},waterlogged=false]"


# ============================================================================================ koepels
def _profiel_ui(t):
    """Uienkoepel: straalfactor op hoogte t (0..1)."""
    if t < 0.42:
        return 1.0 + 0.22 * math.sin(t / 0.42 * math.pi / 2)
    q = min(1.0, max(0.0, (t - 0.42) / 0.58))
    return 1.22 * (1 - q) ** 1.55 * (1 + 0.35 * q) + 0.0


def _profiel_rond(t):
    return math.sqrt(max(0.0, 1 - t * t))


def _profiel_spits(t):
    """Perzische spitse koepel (iets puntig)."""
    return max(0.0, math.sqrt(max(0.0, 1 - t ** 1.7)) * (1 - 0.25 * t ** 3))


def koepel(W, cx, y0, cz, r, k_dak="koepel_roze", vorm="ui", ribben=8, rib="goud", hoogte=None, oortjes=True,
           top="goudblok", trommel=0, trommel_k="muur_vanille", raam="glas", dik=1.6, binnen=None):
    """Koepel op (cx, y0, cz). vorm: 'ui' | 'rond' | 'spits'. Trommel (ronde muur met raampjes) eronder.
    Gouden ribben, bovenop een makelaar met (optioneel) twee guh-oortjes."""
    if not raakt(W, cx - r * 1.3, y0, cz - r * 1.3, cx + r * 1.3, y0 + trommel + r * 3, cz + r * 1.3):
        return y0
    if trommel:
        for dy in range(trommel):
            for dx in range(-int(r) - 1, int(r) + 2):
                for dz in range(-int(r) - 1, int(r) + 2):
                    d = math.hypot(dx + 0.5, dz + 0.5)
                    if r - 1.5 < d <= r:
                        a = math.atan2(dz + 0.5, dx + 0.5)
                        k = trommel_k
                        if 1 <= dy < trommel - 1 and math.cos(a * max(8, int(r * 1.2))) > 0.45:
                            k = raam
                        if dy == trommel - 1:
                            k = "goud"
                        zet(W, cx + dx, y0 + dy, cz + dz, k)
                    elif binnen and d <= r - 1.5 and dy == 0:
                        pass
        y0 += trommel
    prof = {"ui": _profiel_ui, "rond": _profiel_rond, "spits": _profiel_spits}[vorm]
    H = hoogte or (r * 2.1 if vorm == "ui" else (r * 1.0 if vorm == "rond" else r * 1.35))
    rmax = r * 1.25
    for iy in range(int(math.ceil(H)) + 1):
        t = min(1.0, (iy + 0.5) / H)
        rr = r * prof(t)
        rr_b = r * prof(min(1.0, (iy + 1.5) / H))
        if rr < 0.3:
            continue
        for dx in range(-int(rmax) - 1, int(rmax) + 2):
            for dz in range(-int(rmax) - 1, int(rmax) + 2):
                d = math.hypot(dx + 0.5, dz + 0.5)
                binnenr = min(rr, rr_b) - dik
                if d <= rr and d > binnenr:
                    a = math.atan2(dz + 0.5, dx + 0.5)
                    k = k_dak
                    if ribben and abs(math.sin(a * ribben / 2)) < 0.11 + 0.06 * t:
                        k = rib
                    if iy == 0:
                        k = rib
                    zet(W, cx + dx, y0 + iy, cz + dz, k)
    yt = y0 + int(math.ceil(H))
    # makelaar
    for dy in range(3):
        zet(W, cx, yt + dy, cz, top if dy != 1 else "goud")
    zet(W, cx, yt + 3, cz, "minecraft:end_rod[facing=up]")
    if oortjes and r >= 3:
        guh_oortjes(W, cx, yt - 1, cz, max(1.6, r * 0.3))
    return yt + 3


def guh_oortjes(W, cx, y, cz, s=1.5, as_="x", k="koepel_roze", binnen="minecraft:pink_wool"):
    """Twee ronde guh-oortjes (schijfjes) naast de makelaar, schuin naar buiten."""
    for sgn in (-1, 1):
        ox = cx + sgn * (s * 1.7 + 0.5)
        oy = y + s * 1.2
        for dx in range(-int(s) - 2, int(s) + 3):
            for dy in range(-int(s) - 2, int(s) + 3):
                if math.hypot(dx, dy) <= s + 0.4:
                    xx = ox + dx + sgn * dy * 0.35
                    if as_ == "x":
                        zet(W, xx, oy + dy, cz, k if math.hypot(dx, dy) > s - 0.8 else binnen)
                    else:
                        zet(W, cx, oy + dy, cz + (xx - cx), k if math.hypot(dx, dy) > s - 0.8 else binnen)


# ============================================================================================ torens
def minaret(W, cx, cz, y0, h, r=3, wand="muur_vanille", band="tegel_turkoois", dak="koepel_roze", balkons=2,
            oortjes=True, top_r=None):
    """Achthoekige minaret met tegelbanden, 1-3 rondgangen (balkons met hek en lantaarns), een open lantaarnkamer
    met zuiltjes en een uienkoepel met guh-oortjes."""
    if not raakt(W, cx - r - 4, y0, cz - r - 4, cx + r + 4, y0 + h + r * 4 + 10, cz + r + 4):
        return

    def okt(dx, dz):
        a = math.atan2(dz, dx)
        sec = math.pi / 8
        return math.hypot(dx, dz) * math.cos((a % (2 * sec)) - sec)
    rng = rng_van(cx, cz, 11)
    ys_bal = [y0 + int(h * f) for f in ((0.55, 0.82) if balkons == 2 else ((0.7,) if balkons == 1 else (0.4, 0.62, 0.84)))]
    for dy in range(h):
        y = y0 + dy
        rr = r + (0.6 if dy < 4 else 0)
        for dx in range(-r - 2, r + 3):
            for dz in range(-r - 2, r + 3):
                d = okt(dx + 0.5, dz + 0.5)
                if rr - 1.4 < d <= rr:
                    k = wand
                    if dy % 9 in (6, 7) and dy > 4:
                        k = band
                    a = math.atan2(dz + 0.5, dx + 0.5)
                    if dy % 9 in (2, 3, 4) and dy > 4 and abs(math.sin(a * 2)) < 0.25:
                        k = "glas"
                    zet(W, cx + dx, y, cz + dz, k)
    for yb in ys_bal:
        # consoles (getrapt), vloer, hek, lantaarns
        for dx in range(-r - 3, r + 4):
            for dz in range(-r - 3, r + 4):
                d = okt(dx + 0.5, dz + 0.5)
                if r - 0.5 < d <= r + 1.8:
                    zet(W, cx + dx, yb, cz + dz, "goud" if d > r + 1 else "wit_glad")
                    if r + 0.8 < d <= r + 1.8:
                        zet(W, cx + dx, yb + 1, cz + dz, "minecraft:cherry_fence")
                if r - 0.5 < d <= r + 0.9:
                    zet(W, cx + dx, yb - 1, cz + dz, "goud")
        for k in range(8):
            a = k * math.pi / 4 + math.pi / 8
            zet(W, cx + math.cos(a) * (r + 1.3), yb - 1, cz + math.sin(a) * (r + 1.3), lantaarn(True))
        leeg(W, cx - r + 2, yb + 1, cz - r + 2, cx + r - 2, yb + 2, cz + r - 2)
    # lantaarnkamer bovenop: zuiltjes + bogen, dan de koepel
    yt = y0 + h
    rt = top_r or r
    doos(W, cx - rt, yt, cz - rt, cx + rt, yt, cz + rt, "goud")
    for k in range(8):
        a = k * math.pi / 4
        px, pz = cx + round(math.cos(a) * (rt - 0.5)), cz + round(math.sin(a) * (rt - 0.5))
        doos(W, px, yt + 1, pz, px, yt + 4, pz, "zuil_wit")
    for dx in range(-rt, rt + 1):
        for dz in range(-rt, rt + 1):
            d = okt(dx + 0.5, dz + 0.5)
            if d <= rt + 0.2:
                zet(W, cx + dx, yt + 5, cz + dz, "wit_glad" if d > rt - 1.4 else "goud")
    zet(W, cx, yt + 4, cz, lantaarn(True))
    koepel(W, cx, yt + 6, cz, rt + 0.6, k_dak=dak, vorm="ui", ribben=8, oortjes=oortjes)


def wachttoren(W, cx, cz, y0, h, r, wand="steen_roze", band="tegel_roze", dak="koepel_roze", kantelen=True):
    """Ronde (stads)toren met kantelen, een rondgang op consoles, smalle raampjes en een koepeltje."""
    if not raakt(W, cx - r - 3, y0, cz - r - 3, cx + r + 3, y0 + h + r * 3, cz + r + 3):
        return
    for dy in range(h):
        for dx in range(-r - 1, r + 2):
            for dz in range(-r - 1, r + 2):
                d = math.hypot(dx + 0.5, dz + 0.5)
                if r - 1.5 < d <= r + (0.5 if dy < 3 else 0):
                    a = math.atan2(dz + 0.5, dx + 0.5)
                    k = wand
                    if dy % 11 == 8:
                        k = band
                    if dy % 11 in (4, 5) and abs(math.sin(a * 2.5)) < 0.2 and dy > 3:
                        k = "glas"
                    zet(W, cx + dx, y0 + dy, cz + dz, k)
    yt = y0 + h
    for dx in range(-r - 2, r + 3):
        for dz in range(-r - 2, r + 3):
            d = math.hypot(dx + 0.5, dz + 0.5)
            if d <= r + 1.5:
                zet(W, cx + dx, yt, cz + dz, "wit_glad" if d > r - 1 else wand)
            if r - 0.5 < d <= r + 1.5:
                zet(W, cx + dx, yt - 1, cz + dz, "goud")
                if kantelen:
                    a = math.atan2(dz + 0.5, dx + 0.5)
                    zet(W, cx + dx, yt + 1, cz + dz, wand)
                    if math.sin(a * (r + 3)) > 0:
                        zet(W, cx + dx, yt + 2, cz + dz, wand)
                        zet(W, cx + dx, yt + 3, cz + dz, plaat("wit_glad"))
    if dak:
        koepel(W, cx, yt + 1, cz, max(2, r - 2), k_dak=dak, vorm="ui", ribben=6, oortjes=True)


# ============================================================================================ bogen / iwan
def boog_top(du, breed, h_recht, spits=1.25):
    hw = breed / 2.0
    q = min(1.0, abs(du) / hw)
    return h_recht + hw * spits * (1 - q ** 1.5) ** 0.66


def boog(W, kant, c, u_mid, y1, breed, h_recht, diep=2, kader="tegel_turkoois", vulling=None, achter=None,
         spits=1.25, kader_dik=1):
    """Spitsboog-opening in een muur op vlakcoördinaat c (kant = buitennormaal). De opening gaat diep blokken de
    muur in (lucht, of 'vulling' = glas/luik op de achterste laag) met een tegelkader eromheen. achter = blok op de
    diepste laag (bv. een deur of donker)."""
    hw = breed / 2.0
    dx, dz = kant
    s = -1  # naar binnen = tegen de normaal in
    for du in np.arange(-hw - kader_dik, hw + kader_dik + 0.01, 1.0):
        u = int(math.floor(u_mid + du))
        top_g = boog_top(du, breed, h_recht, spits) if abs(du) < hw else -1
        top_k = boog_top(min(abs(du), hw), breed + 2 * kader_dik, h_recht, spits) + kader_dik
        for y in range(int(y1), int(y1 + top_k) + 1):
            dy = y - y1
            binnen = abs(du) < hw and 0 <= dy <= top_g
            if binnen:
                for k in range(diep):
                    cc = c + s * k * (dx + dz)
                    blok = LUCHT
                    if k == diep - 1:
                        blok = achter or vulling or LUCHT
                    vlak(W, kant, cc, u, y, blok)
            elif kader and dy >= 0:
                vlak(W, kant, c, u, y, kader)


def iwan(W, kant, c, u_mid, y0, breed, hoog, diep, buiten="muur_vanille", kader="tegel_turkoois",
         vak="tegel_blauw", deur="minecraft:air", muqarnas="goud"):
    """Perzisch iwan-portaal: een rechthoekige pishtaq (gevelvlak, steekt 1 uit) met tegelrand, en daarin een diepe
    spitsboognis (diep blokken) met getrapte muqarnas-rijen in het gewelf en een deur achterin."""
    dx, dz = kant
    s = dx + dz
    hw = breed // 2 + 3
    ytop = y0 + hoog + breed // 2 + 4
    # pishtaq (steekt 1 uit)
    for u in range(int(u_mid) - hw - 1, int(u_mid) + hw + 2):
        for y in range(y0, ytop + 1):
            rand = u in (int(u_mid) - hw - 1, int(u_mid) + hw + 1) or y == ytop
            k = kader if rand else (vak if (u + y) % 7 == 0 else buiten)
            vlak(W, kant, c + s, u, y, k)
            vlak(W, kant, c, u, y, buiten)
        vlak(W, kant, c + s, u, ytop + 1, "goud")
        if (u - int(u_mid)) % 3 == 0:
            vlak(W, kant, c + s, u, ytop + 2, plaat("wit_glad"))
    # de nis
    hwb = breed / 2.0
    for du in np.arange(-hwb, hwb + 0.01, 1.0):
        u = int(math.floor(u_mid + du))
        top = boog_top(du, breed, hoog, 1.15)
        for y in range(y0, int(y0 + top) + 1):
            for k in range(-1, diep):
                vlak(W, kant, c + s - k * s - s, u, y, LUCHT)
        # muqarnas: getrapte rijen (ondersteboven trappen) in de bovenste meters van de nis
        for k in range(diep):
            yy = int(y0 + top) - (k // 2)
            if yy > y0 + hoog - 2:
                vlak(W, kant, c - k * s, u, yy, trap(muqarnas if muqarnas in ("goud",) else "wit_glad",
                                                      FACE[(-dx, -dz)] if (k % 2 == 0) else FACE[(dx, dz)], half="top"))
    # achterwand met deur + tegels
    cb = c - (diep - 1) * s - s
    for du in range(-int(hwb), int(hwb) + 1):
        for y in range(y0, int(y0 + boog_top(du, breed, hoog, 1.15)) + 1):
            vlak(W, kant, cb, u_mid + du, y, vak if (du + y) % 2 == 0 else buiten)
    deurb = max(2, breed // 4)
    for du in range(-deurb // 2, deurb // 2 + 1):
        for y in range(y0, y0 + 4 + deurb // 2):
            vlak(W, kant, cb, u_mid + du, y, deur)
    # boogveld boven de deur: een rond guh-raampje
    yr = y0 + 6 + deurb // 2
    for du in range(-2, 3):
        for dy in range(-2, 3):
            if math.hypot(du, dy) <= 2.2:
                vlak(W, kant, cb, u_mid + du, yr + dy, "glas_guh" if math.hypot(du, dy) < 1.5 else "goud")


# ============================================================================================ groen
PLANTEN = ["guhs:potted_roze_hibiscus", "guhs:potted_guhwaii_plumeria", "guhs:potted_guhwaii_paradijsbloem",
           "minecraft:potted_flowering_azalea_bush", "minecraft:potted_azalea_bush", "guhs:potted_roze_guhbloem",
           "minecraft:potted_cactus", "guhs:potted_guhoortjes"]
BLOEMEN = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:peony[half=lower]", "minecraft:orange_tulip",
           "minecraft:pink_petals[facing=north,flower_amount=4]", "guhs:roze_guhbloem", "minecraft:oxeye_daisy"]


def palm(W, x, y, z, h=None, seed=0, kroon="guhs:guhwaii_palm_blad", stam="guhs:guhwaii_palm_stam[axis=y]",
         dadels=True):
    """Dadelpalm: licht gebogen stam, 8 hangende bladen (bogen van bladblokken), trosjes dadels onder de kroon."""
    rng = rng_van(x, z, seed)
    h = h or int(rng.randint(8, 15))
    a = rng.uniform(0, 2 * math.pi)
    buig = rng.uniform(0.6, 2.2)
    px, pz = x, z
    for i in range(h):
        t = i / max(h - 1, 1)
        px = x + math.cos(a) * buig * t * t
        pz = z + math.sin(a) * buig * t * t
        zet(W, px, y + i, pz, stam)
    tx, ty, tz = px, y + h, pz
    zet(W, tx, ty, tz, kroon)
    zet(W, tx, ty + 1, tz, kroon)
    n = 8
    for k in range(n):
        b_ = k * 2 * math.pi / n + rng.uniform(-0.2, 0.2)
        L = rng.uniform(4.0, 6.0)
        for s in np.arange(0.5, L, 0.5):
            hx = tx + math.cos(b_) * s
            hz = tz + math.sin(b_) * s
            hy = ty + 1 + 0.9 * s - 0.28 * s * s
            zet(W, hx, hy, hz, kroon)
            if s > L * 0.45 and rng.rand() < 0.35:
                zet(W, hx, hy - 1, hz, kroon)
    if dadels:
        for k in range(3):
            b_ = rng.uniform(0, 2 * math.pi)
            zet(W, tx + math.cos(b_), ty - 1, tz + math.sin(b_), "minecraft:orange_terracotta")


def struik(W, x, y, z, seed=0, soort=None):
    rng = rng_van(x, z, seed, 3)
    soort = soort or ["minecraft:flowering_azalea_leaves", "minecraft:azalea_leaves", "minecraft:cherry_leaves",
                      "guhs:guhbloesem_leaves"][rng.randint(4)]
    r = rng.uniform(1.0, 1.9)
    W.bol((x + 0.5, y + r * 0.6, z + 0.5), r, b(soort), ry=r * 0.75, keep=True)


def bloembak(W, kant, c, u, y):
    """Bloembak onder een raam: plaatje + bloemen erin (steekt 1 uit)."""
    dx, dz = kant
    s = dx + dz
    rng = rng_van(c, u, y)
    vlak(W, kant, c + s, u, y, luik(kant, half="top", open_=False))
    vlak(W, kant, c + s, u, y + 1, BLOEMEN[rng.randint(len(BLOEMEN))].replace("[half=lower]", ""))


def potten(W, x1, z1, x2, z2, y, seed=0, dicht=0.35):
    rng = rng_van(x1, z1, seed)
    for x in range(int(x1), int(x2) + 1):
        for z in range(int(z1), int(z2) + 1):
            if rng.rand() < dicht and W.wget(x, y, z) == LUCHT:
                zet(W, x, y, z, PLANTEN[rng.randint(len(PLANTEN))])


# ============================================================================================ luifels, lampjes
def luifel(W, kant, c, u1, u2, y, diep=3, kleuren=("roze", "wit"), schuin=True):
    """Gestreepte luifel boven een deur/kraam: wolstroken die schuin naar buiten aflopen, met een randje."""
    dx, dz = kant
    s = dx + dz
    for k in range(1, diep + 1):
        yy = y - (k // 2 if schuin else 0)
        for u in range(int(u1), int(u2) + 1):
            kl = kleuren[(u // 1) % len(kleuren)] if True else kleuren[0]
            vlak(W, kant, c + s * k, u, yy, TAPIJT[kl] if k == diep else WOL[kl])
    # paaltjes aan het eind
    if diep >= 3:
        for u in (u1, u2):
            for yy in range(y - diep // 2 - 3, y - diep // 2):
                vlak(W, kant, c + s * diep, u, yy, "minecraft:cherry_fence")


def lampionsnoer(W, p0, p1, doorzak=3.0, elke=3, kleuren=("glas", "glas_hart", "glas_regenboog")):
    """Snoer tussen twee punten (wereld) met hangende lantaarns en gekleurde glazen lampjes."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    L = float(np.linalg.norm(p1 - p0))
    n = max(2, int(L))
    for i in range(n + 1):
        t = i / n
        p = p0 + (p1 - p0) * t
        p[1] -= doorzak * 4 * t * (1 - t)
        as_ = "x" if abs(p1[0] - p0[0]) > abs(p1[2] - p0[2]) else "z"
        zet(W, p[0], p[1], p[2], ketting(as_))
        if i % elke == elke // 2 and 0 < i < n:
            zet(W, p[0], p[1] - 1, p[2], lantaarn(True))


def hanglamp(W, x, y_plafond, z, lengte=3, soort="minecraft:lantern"):
    for dy in range(1, lengte):
        zet(W, x, y_plafond - dy, z, ketting("y"))
    zet(W, x, y_plafond - lengte, z, lantaarn(True, soort))


def kroonluchter(W, cx, y_plafond, cz, r=4, ringen=2, lengte=5):
    """Grote hangende kroonluchter: ketting, gouden ringen met kaarsen/end-rods en glazen druppels."""
    for dy in range(1, lengte):
        zet(W, cx, y_plafond - dy, cz, ketting("y"))
    yc = y_plafond - lengte
    zet(W, cx, yc, cz, "goudblok")
    zet(W, cx, yc - 1, cz, "minecraft:pearlescent_froglight")
    zet(W, cx, yc - 2, cz, "goud")
    for ri in range(ringen):
        rr = r - ri * 1.6
        yy = yc - 1 - ri * 2
        for k in range(int(rr * 7)):
            a = k * 2 * math.pi / int(rr * 7)
            x, z = cx + math.cos(a) * rr, cz + math.sin(a) * rr
            zet(W, x, yy, z, plaat("goud"))
            if k % 2 == 0:
                zet(W, x, yy + 1, z, "minecraft:candle[candles=1,lit=true,waterlogged=false]")
            else:
                zet(W, x, yy - 1, z, "minecraft:end_rod[facing=down]")
        for k in range(4):
            a = k * math.pi / 2 + ri * 0.4
            for s in np.arange(0.5, rr, 0.6):
                zet(W, cx + math.cos(a) * s, yy + (rr - s) * 0.3, cz + math.sin(a) * s, plaat("goud"))


def hangende_tuin(W, x, y, z, lengte, seed=0):
    """Hangende ranken/bloemen langs een muur naar beneden."""
    rng = rng_van(x, z, y, seed)
    for dy in range(int(lengte)):
        if rng.rand() < 0.85:
            zet(W, x, y - dy, z, "minecraft:flowering_azalea_leaves" if rng.rand() < 0.4 else
                "minecraft:cherry_leaves", keep=True)


# ============================================================================================ huizen
GEVELS = ["muur_vanille", "muur_roze", "muur_perzik", "muur_vanille", "muur_boter", "muur_roze", "muur_lavendel"]


def dakrand(W, x1, z1, x2, z2, y, k, kanteel=True):
    """Borstwering met getrapte kantelen (Midden-Oosters: blokje, plaatje, blokje)."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if x in (x1, x2) or z in (z1, z2):
                zet(W, x, y, z, k)
                if kanteel and ((x + z) % 3 == 0):
                    zet(W, x, y + 1, z, k)
                    zet(W, x, y + 2, z, plaat("wit_glad"))
                elif kanteel:
                    zet(W, x, y + 1, z, plaat(k) if k in ("muur_vanille", "muur_roze", "muur_perzik", "muur_boter",
                                                          "steen_roze", "steen_vanille") else plaat("wit_glad"))


def mashrabiya(W, kant, c, u, y, breed=3, hoog=3):
    """Uitkragende houten erker met traliewerk (open luikjes als roosters), een plaatjesdakje en consoles."""
    dx, dz = kant
    s = dx + dz
    for du in range(-(breed // 2), breed // 2 + 1):
        for dy in range(hoog):
            vlak(W, kant, c + s, u + du, y + dy, luik(kant, half="top", open_=True))
            vlak(W, kant, c, u + du, y + dy, "minecraft:air")
        vlak(W, kant, c + s, u + du, y - 1, trap("hout", FACE[(-dx, -dz)], half="top"))
        vlak(W, kant, c + s, u + du, y + hoog, plaat("koepel_roze") if (du % 2) else plaat("hout"))
    for du in (-(breed // 2) - 1, breed // 2 + 1):
        for dy in range(hoog):
            vlak(W, kant, c + s, u + du, y + dy, "minecraft:cherry_fence")
    vlak(W, kant, c + s * 2, u, y - 1, lantaarn(True)) if False else None


def raam(W, kant, c, u, y, rng, soort=None):
    """Een raam met diepte: spitsboogje 1 diep met glas achterin, soms luiken, soms een bloembak."""
    soort = soort or ["boog", "boog", "luik", "rond", "dubbel"][rng.randint(5)]
    if soort == "boog" or soort == "dubbel":
        n = 2 if soort == "dubbel" else 1
        for i in range(n):
            uu = u + (i * 2 - (n - 1))
            vlak(W, kant, c, uu, y, LUCHT)
            vlak(W, kant, c, uu, y + 1, LUCHT)
            vlak(W, kant, c - (kant[0] + kant[1]), uu, y, "glas")
            vlak(W, kant, c - (kant[0] + kant[1]), uu, y + 1, "glas")
            vlak(W, kant, c, uu, y + 2, trap("wit_glad", FACE[(-kant[0], -kant[1])], half="top"))
        if rng.rand() < 0.5:
            bloembak(W, kant, c, u, y - 1)
    elif soort == "luik":
        vlak(W, kant, c, u, y, "glas")
        vlak(W, kant, c, u, y + 1, "glas")
        dx, dz = kant
        s = dx + dz
        rechts = (dz, -dx) if dx else (-dz, dx)
        ru = rechts[0] if dx == 0 else rechts[1]
        for side in (-1, 1):
            for yy in (y, y + 1):
                vlak(W, kant, c + s, u + side, yy, f"minecraft:cherry_trapdoor[facing={FACE[kant]},half=top,open=true,powered=false,waterlogged=false]")
        bloembak(W, kant, c, u, y - 1)
    elif soort == "rond":
        vlak(W, kant, c, u, y + 1, "glas_guh")
        vlak(W, kant, c, u, y, LUCHT)
        vlak(W, kant, c - (kant[0] + kant[1]), u, y, "glas")


def huis(W, x1, z1, x2, z2, y0, verd=2, seed=0, gevel=None, dakk=True, voor=(0, 1), koepeltje=None,
         windtoren=None, dakterras=True, tuin=True):
    """Plat stadshuis met diepte: verdiepingen van 5, raamnissen (boog/luik/rond/dubbel), bloembakken, een
    mashrabiya-erker, deur met luifel en lantaarn, kantelrand, dakterras met potten, parasol of pergola,
    soms een koepeltje of een windtoren, en een teruggezette bovenste verdieping (terrassen)."""
    if not raakt(W, x1, y0, z1, x2, y0 + verd * 5 + 16, z2):
        return
    rng = rng_van(x1, z1, seed)
    gevel = gevel or GEVELS[rng.randint(len(GEVELS))]
    sokkel = "steen_vanille" if gevel != "muur_vanille" else "steen_perzik"
    hgt = verd * 5
    doos(W, x1, y0 - 3, z1, x2, y0 - 1, z2, sokkel)
    doos(W, x1, y0, z1, x2, y0 + hgt, z2, gevel)
    leeg(W, x1 + 1, y0 + 1, z1 + 1, x2 - 1, y0 + hgt - 1, z2 - 1)
    for v in range(1, verd):
        doos(W, x1 + 1, y0 + v * 5, z1 + 1, x2 - 1, y0 + v * 5, z2 - 1, "hout")
    # sokkelband + kroonlijst
    for (x, z) in [(x, z1) for x in range(x1, x2 + 1)] + [(x, z2) for x in range(x1, x2 + 1)] + \
                  [(x1, z) for z in range(z1, z2 + 1)] + [(x2, z) for z in range(z1, z2 + 1)]:
        zet(W, x, y0, z, sokkel)
        zet(W, x, y0 + hgt - 1, z, "tegel_turkoois" if (x + z) % 2 else "tegel_blauw")
    # gevels: per kant raamnissen
    kanten = [((0, -1), z1, x1, x2), ((0, 1), z2, x1, x2), ((-1, 0), x1, z1, z2), ((1, 0), x2, z1, z2)]
    erker_gedaan = False
    for kant, c, u1, u2 in kanten:
        L = u2 - u1
        if L < 4:
            continue
        for v in range(verd):
            yv = y0 + v * 5 + 2
            n = max(1, (L - 1) // 4)
            for i in range(n):
                u = u1 + 2 + int((i + 0.5) * (L - 3) / n)
                if v == 0 and kant == voor and i == n // 2:
                    continue
                if v == 1 and not erker_gedaan and L >= 6 and rng.rand() < 0.65 and i == n // 2:
                    mashrabiya(W, kant, c, u, yv - 1, breed=3, hoog=3)
                    erker_gedaan = True
                    continue
                raam(W, kant, c, u, yv, rng)
    # deur met luifel, lantaarn en trapje
    kant, c = voor, (z2 if voor == (0, 1) else z1 if voor == (0, -1) else x2 if voor == (1, 0) else x1)
    um = (x1 + x2) // 2 if voor[0] == 0 else (z1 + z2) // 2
    boog(W, kant, c, um + 0.5, y0 + 1, 3, 2, diep=2, kader="tegel_roze",
         achter=f"minecraft:cherry_door[facing={FACE[(-kant[0], -kant[1])] if False else FACE[kant]},half=lower,hinge=left,open=false,powered=false]")
    vlak(W, kant, c - (kant[0] + kant[1]), um, y0 + 2, f"minecraft:cherry_door[facing={FACE[kant]},half=upper,hinge=left,open=false,powered=false]")
    kl = [("roze", "wit"), ("turkoois", "wit"), ("geel", "oranje"), ("lila", "roze"), ("magenta", "roze")][rng.randint(5)]
    luifel(W, kant, c, um - 2, um + 2, y0 + 5, diep=2, kleuren=kl, schuin=False)
    vlak(W, kant, c + kant[0] + kant[1], um - 2, y0 + 3, lantaarn(False)) if rng.rand() < 0.5 else None
    for du in (-1, 0, 1):
        vlak(W, kant, c + kant[0] + kant[1], um + du, y0, plaat("steen_vanille"))
    # dak
    yd = y0 + hgt
    doos(W, x1, yd, z1, x2, yd, z2, "steen_vanille")
    dakrand(W, x1, z1, x2, z2, yd + 1, gevel)
    if not dakk:
        return
    # teruggezette bovenkamer
    bx1, bz1 = x1 + 2 + rng.randint(0, 2), z1 + 2 + rng.randint(0, 2)
    bx2, bz2 = min(x2 - 2, bx1 + rng.randint(3, 6)), min(z2 - 2, bz1 + rng.randint(3, 6))
    if bx2 - bx1 >= 3 and bz2 - bz1 >= 3 and rng.rand() < 0.75:
        doos(W, bx1, yd + 1, bz1, bx2, yd + 4, bz2, gevel)
        leeg(W, bx1 + 1, yd + 1, bz1 + 1, bx2 - 1, yd + 3, bz2 - 1)
        for (kk, cc, uu) in (((0, 1), bz2, (bx1 + bx2) // 2), ((1, 0), bx2, (bz1 + bz2) // 2)):
            boog(W, kk, cc, uu + 0.5, yd + 1, 3, 1, diep=1, kader=None)
        doos(W, bx1, yd + 5, bz1, bx2, yd + 5, bz2, "steen_vanille")
        dakrand(W, bx1, bz1, bx2, bz2, yd + 6, gevel)
        k = koepeltje if koepeltje is not None else (rng.rand() < 0.35)
        if k:
            kd = ["koepel_roze", "koepel_turkoois", "koepel_goud", "koepel_lavendel", "koepel_roze"][rng.randint(5)]
            r = max(1.6, min(bx2 - bx1, bz2 - bz1) / 2 - 0.3)
            koepel(W, (bx1 + bx2) / 2, yd + 6, (bz1 + bz2) / 2, r, k_dak=kd, vorm=["ui", "rond", "spits"][rng.randint(3)],
                   ribben=6, oortjes=rng.rand() < 0.6)
    elif windtoren if windtoren is not None else rng.rand() < 0.3:
        wx, wz = x2 - 3, z1 + 1
        doos(W, wx, yd + 1, wz, wx + 2, yd + 8, wz + 2, gevel)
        for dy in (6, 7):
            for (ax, az) in ((wx + 1, wz), (wx + 1, wz + 2), (wx, wz + 1), (wx + 2, wz + 1)):
                zet(W, ax, yd + dy, az, "minecraft:air")
        doos(W, wx, yd + 9, wz, wx + 2, yd + 9, wz + 2, "goud")
    if dakterras:
        # potten, kussens, parasol of pergola
        potten(W, x1 + 1, z1 + 1, x2 - 1, z2 - 1, yd + 1, seed=seed, dicht=0.08)
        if rng.rand() < 0.5:
            px, pz = x1 + 2 + rng.randint(0, max(1, x2 - x1 - 3)), z1 + 2 + rng.randint(0, max(1, z2 - z1 - 3))
            if W.wget(px, yd + 1, pz) == LUCHT:
                doos(W, px, yd + 1, pz, px, yd + 3, pz, "minecraft:cherry_fence")
                kl = WOL[["roze", "turkoois", "geel", "wit", "lila"][rng.randint(5)]]
                for dx in range(-2, 3):
                    for dz in range(-2, 3):
                        if abs(dx) + abs(dz) <= 3:
                            zet(W, px + dx, yd + 4, pz + dz, kl.replace("_wool", "_carpet") if abs(dx) + abs(dz) == 3 else kl, keep=True)
        elif tuin and rng.rand() < 0.6:
            # pergola met ranken
            for (px, pz) in ((x1 + 1, z1 + 1), (x1 + 1, z2 - 1), (x2 - 1, z1 + 1), (x2 - 1, z2 - 1)):
                doos(W, px, yd + 1, pz, px, yd + 3, pz, "minecraft:cherry_fence")
            for x in range(x1 + 1, x2):
                for z in range(z1 + 1, z2):
                    if (x + z) % 2 == 0:
                        zet(W, x, yd + 4, z, "minecraft:flowering_azalea_leaves", keep=True)


# ============================================================================================ markt
WAREN = ["minecraft:orange_wool", "minecraft:yellow_wool", "minecraft:red_wool", "minecraft:pink_wool",
         "minecraft:brown_wool", "minecraft:barrel[facing=up,open=false]", "minecraft:hay_block[axis=y]",
         "minecraft:melon", "minecraft:pumpkin", "minecraft:honeycomb_block", "minecraft:magenta_wool",
         "minecraft:lime_wool", "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]",
         "guhs:theepotje"]


def kraam(W, x, y, z, kant=(0, 1), seed=0, breed=5, diep=3):
    """Marktkraam: toonbank met waren (kruidenbergjes, potten, manden), achterwand met hangende tapijten,
    gestreepte luifel op paaltjes, lantaarn."""
    rng = rng_van(x, z, seed, 5)
    dx, dz = kant
    rx, rz = (dz, -dx) if dx == 0 else (-dz, dx)
    rx, rz = (1, 0) if dx == 0 else (0, 1)
    hb = breed // 2

    def p(u, d):  # u langs de toonbank, d naar achter (tegen de normaal in)
        return x + rx * u - dx * d, z + rz * u - dz * d
    for u in range(-hb, hb + 1):
        for d in range(diep):
            px, pz = p(u, d)
            if d == 0:
                zet(W, px, y, pz, "hout" if abs(u) < hb else "minecraft:cherry_fence")
                if abs(u) < hb:
                    zet(W, px, y + 1, pz, WAREN[rng.randint(len(WAREN))] if rng.rand() < 0.8 else LUCHT)
            if d == diep - 1:
                for yy in range(y, y + 4):
                    zet(W, px, yy, pz, WOL[["roze", "turkoois", "geel", "lila", "magenta", "oranje"][(u + seed) % 6]]
                        if yy > y else "hout")
        for yy in range(y, y + 4):
            if abs(u) == hb:
                px, pz = p(u, 0)
                zet(W, px, yy, pz, "minecraft:cherry_fence")
                px, pz = p(u, diep - 1)
                zet(W, px, yy, pz, "minecraft:cherry_fence")
    kl = [("roze", "wit"), ("turkoois", "wit"), ("geel", "oranje"), ("lila", "wit"), ("magenta", "roze")][rng.randint(5)]
    for u in range(-hb - 1, hb + 2):
        for d in range(-1, diep + 1):
            px, pz = p(u, d)
            zet(W, px, y + 4 + (1 if 0 <= d < diep - 1 else 0), pz, WOL[kl[(u + 10) % 2]])
    px, pz = p(hb, -1)
    zet(W, px, y + 3, pz, lantaarn(True))


def fontein(W, cx, y, cz, r=4, ster=8, tegel="tegel_turkoois", rand="wit_glad"):
    """Stervormige fontein: een 8-puntig bekken van tegels met water, een schaal op een zuiltje en een straal."""
    for dx in range(-r - 2, r + 3):
        for dz in range(-r - 2, r + 3):
            a = math.atan2(dz, dx)
            rr = r * (0.82 + 0.18 * abs(math.cos(a * ster / 2)))
            d = math.hypot(dx, dz)
            if d <= rr + 1:
                zet(W, cx + dx, y - 1, cz + dz, tegel)
                if d > rr:
                    zet(W, cx + dx, y, cz + dz, rand)
                else:
                    zet(W, cx + dx, y, cz + dz, "minecraft:water[level=0]")
    doos(W, cx, y, cz, cx, y + 2, cz, "zuil_wit")
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            zet(W, cx + dx, y + 3, cz + dz, plaat("goud") if abs(dx) + abs(dz) == 2 else "goud")
    zet(W, cx, y + 4, cz, "minecraft:water[level=0]")
    zet(W, cx, y + 5, cz, "minecraft:water[level=0]")


def kanaal(W, x1, z1, x2, z2, y, rand="tegel_blauw", diep=2):
    """Recht kanaaltje (tuinkanaal) met tegelrand; water op y-1."""
    for x in range(int(min(x1, x2)) - 1, int(max(x1, x2)) + 2):
        for z in range(int(min(z1, z2)) - 1, int(max(z1, z2)) + 2):
            binnen = min(x1, x2) <= x <= max(x1, x2) and min(z1, z2) <= z <= max(z1, z2)
            if binnen:
                for k in range(diep):
                    zet(W, x, y - 1 - k, z, "minecraft:water[level=0]")
                zet(W, x, y - 1 - diep, z, "tegel_turkoois")
                leeg(W, x, y, z, x, y + 1, z)
            else:
                zet(W, x, y - 1, z, rand)
