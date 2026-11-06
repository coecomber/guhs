"""Rozenkloof: de aankomststad van het Zijdeland, uitgehouwen in een roze zandsteenkloof (Petra-achtig).

Spelcoördinaten. De kloof (zijwereld.KLOOF) loopt van de zee (west, x 3236) naar het plateau (oost, x 3548).
Dwarsdoorsnede: kloofbodem (beek, plein, markt) -> richel 1 (+11) -> richel 2 (+24) -> richel 3 (+38) -> plateau (~120).

Wijken (west -> oost):
  De Zeemond (x 3240-3300)   pier, vissersbootjes, de kloof opent naar zee
  Het Rozenplein (x 3370-3430) plein met stervormige fontein, markt, bultguh-stal, tapijtwever
  De Schatkamer (x 3418, zuidwand) de grote uitgehouwen gevel (de hal van Kloofmoeder Roosje)
  De Richels (x 3320-3480)   rotshuizen op drie richels, trappen, bruggen, hangende tuinen, rotsgevels
  De Luchthaven (noordrand, x 3398)  meertoren met dek, lijn E (Staarthaven <-> Rozenkloof)
  De Siq (x 3480-3548)       smalle kloof omhoog naar het plateau en de karavaanweg
"""
import math
import numpy as np
import zijbouw as ZB
import zijwereld as ZW
from zijbouw import zet, doos, leeg, vlak, raakt, rng_van, lantaarn, luik, FACE
from zpalet import Z, WOL, TAPIJT, b, trap, plaat

BOX = (3230, 40, -800, 3560, 200, -600)


def as_punt(t):
    """Punt op de kloofas: t = 0..len(KLOOF)-1 -> (x, z, hw, bodem), plus de eenheidsrichting."""
    K = ZW.KLOOF
    i = min(int(t), len(K) - 2)
    f = t - i
    a, c = np.array(K[i], float), np.array(K[i + 1], float)
    p = a * (1 - f) + c * f
    d = c[:2] - a[:2]
    d /= np.linalg.norm(d)
    return p, d


def t_van_x(x):
    K = ZW.KLOOF
    for i in range(len(K) - 1):
        if K[i][0] <= x <= K[i + 1][0]:
            return i + (x - K[i][0]) / (K[i + 1][0] - K[i][0])
    return 0.0 if x < K[0][0] else len(K) - 1.0


# ============================================================================================ rotsgevels
def rotsgevel(W, cx, y0, cz, kant, breed=13, hoog=18, groot=False, seed=0):
    """Uitgehouwen Petra-gevel in de rotswand: eerst een massief rotsblok erachter (zodat er altijd wand is),
    dan een nis van 3 diep met een podium, zuilen met gouden kapitelen, een fries, een fronton met guh-oortjes;
    bij 'groot' een tweede verdieping met een tholos (rond tempeltje met koepeltje) tussen twee halve frontons.
    Een donkere deur met lantaarns leidt de rots in."""
    if not raakt(W, cx - breed, y0, cz - breed, cx + breed, y0 + hoog + 12, cz + breed):
        return
    dx, dz = kant
    s = dx + dz
    hw = breed // 2
    rng = rng_van(cx, cz, seed)
    steen, glad, kop = "rots", "rots_marmer", "zuilkop"

    def P(u, d, y, k):
        """u langs de gevel, d naar buiten (+) of de rots in (-)."""
        if dx:
            zet(W, cx + d * dx, y, cz + u, k)
        else:
            zet(W, cx + u, y, cz + d * dz, k)
    # rotsmassa erachter en ernaast (nooit hoger dan het land ter plekke: niets steekt uit)
    for u in range(-hw - 4, hw + 5):
        for d in range(-10, 1):
            xx, zz = (cx + d * dx, cz + u) if dx else (cx + u, cz + d * dz)
            top = min(y0 + hoog + 6, ZW.hoogte(W, xx, zz) - 1, y0 + hoog + 1 if d > -4 else 999)
            for y in range(y0 - 1, top + 1):
                band = ZW.BANDEN[int((y + 0.0) // 3) % len(ZW.BANDEN)]
                P(u, d, y, band)
    # nis
    for u in range(-hw, hw + 1):
        for d in range(-3, 1):
            for y in range(y0, y0 + hoog):
                P(u, d, y, "minecraft:air")
    # podium (3 treden) over de hele breedte
    for u in range(-hw, hw + 1):
        P(u, 0, y0, trap("vloer_zand", FACE[(-dx, -dz)]))
        P(u, -1, y0, "vloer_zand")
        P(u, -2, y0, "vloer_zand")
        P(u, -3, y0, "vloer_zand")
    hz = hoog if not groot else int(hoog * 0.55)
    # zuilen (onderste orde)
    n = 6 if breed >= 13 else 4
    zuil_u = [int(round(-hw + 1 + i * (breed - 3) / (n - 1))) for i in range(n)]
    for u in zuil_u:
        for y in range(y0 + 1, y0 + hz - 4):
            P(u, -1, y, "zuil")
        P(u, -1, y0 + hz - 4, kop)
        P(u, -1, y0 + 1, glad)
    # fries + architraaf
    for u in range(-hw, hw + 1):
        P(u, -1, y0 + hz - 3, "goud")
        P(u, -1, y0 + hz - 2, glad if u % 3 else "filigraan")
        P(u, -1, y0 + hz - 1, plaat("marmer_glad", "top") if False else glad)
        P(u, -3, y0 + hz - 3, glad)
        for y in range(y0 + hz - 3, y0 + hz):
            P(u, -2, y, glad)
    # deur (donker, diep) met lantaarns
    for u in range(-1, 2):
        for y in range(y0 + 1, y0 + 6):
            for d in range(-9, -2):
                P(u, d, y, "minecraft:air")
    for u in (-2, 2):
        for y in range(y0 + 1, y0 + 6):
            P(u, -3, y, "goud" if y == y0 + 5 else glad)
    P(0, -3, y0 + 6, "glas_guh")
    P(-1, -3, y0 + 6, "goud")
    P(1, -3, y0 + 6, "goud")
    P(0, -6, y0 + 4, lantaarn(True))
    P(-3, -1, y0 + 4, lantaarn(False)) if False else None
    if not groot:
        # fronton (driehoek van trappen) met guh-oortjes op de punt
        for i in range(hw + 1):
            yy = y0 + hz + i // 2
            for u in (-hw + i, hw - i):
                P(u, -1, yy, trap("marmer_glad", FACE[(1, 0)] if (u < 0) == bool(dx) else FACE[(-1, 0)]) if False else glad)
            for u in range(-hw + i + 1, hw - i):
                P(u, -2, yy, "rots_roze" if i % 2 else glad)
        yt = y0 + hz + hw // 2
        for k in (-1, 1):
            P(k * 2, -1, yt + 1, "goud")
            P(k * 2, -1, yt + 2, "goud")
        P(0, -1, yt + 1, "goudblok")
        return
    # ---- groot: bovenste orde met tholos (zoals Al-Khazneh)
    yb = y0 + hz
    for u in range(-hw, hw + 1):
        for d in range(-4, 1):
            for y in range(yb, y0 + hoog):
                P(u, d, y, "minecraft:air")
    for u in range(-hw, hw + 1):
        P(u, -1, yb, glad)
        P(u, -2, yb, glad)
    # halve frontons links en rechts
    for side in (-1, 1):
        for i in range(5):
            for u in range(side * (hw - 5 + i), side * hw + side, side):
                pass
        for i in range(4):
            u0 = side * (hw - 3)
            for y in range(yb + 1, yb + int(hoog * 0.3)):
                P(u0 + side * 0, -1, y, "zuil")
                P(u0 + side * 2, -1, y, "zuil")
            yy = yb + int(hoog * 0.3)
            for u in range(min(u0 - side, u0 + side * 3), max(u0 - side, u0 + side * 3) + 1):
                P(u, -1, yy, "goud")
                P(u, -1, yy + 1, glad)
            for k in range(3):
                for u in range(min(u0 + side * k, u0 + side * 3), max(u0 + side * k, u0 + side * 3) + 1):
                    P(u, -1, yy + 2 + k, glad)
    # tholos: rond tempeltje in het midden (r 3) met zuiltjes, koepeltje en een urn met oortjes
    tc = 0
    yt0 = yb + 1
    th = int(hoog * 0.3)
    if dx:
        tx, tz = cx - dx * 3, cz
    else:
        tx, tz = cx, cz - dz * 3
    for k in range(8):
        a = k * math.pi / 4
        px, pz = tx + round(math.cos(a) * 3), tz + round(math.sin(a) * 3)
        for y in range(yt0, yt0 + th):
            zet(W, px, y, pz, "zuil")
        zet(W, px, yt0 + th, pz, "zuilkop")
    for ddx in range(-4, 5):
        for ddz in range(-4, 5):
            if math.hypot(ddx, ddz) <= 4.2:
                zet(W, tx + ddx, yt0 + th + 1, tz + ddz, "goud" if math.hypot(ddx, ddz) > 3.2 else glad)
    for ddx in range(-2, 3):
        for ddz in range(-2, 3):
            zet(W, tx + ddx, yt0, tz + ddz, "rots_roze")
    zet(W, tx, yt0 + 1, tz, "minecraft:decorated_pot[facing=north,waterlogged=false,cracked=false]")
    ZB.koepel(W, tx, yt0 + th + 2, tz, 3.4, k_dak="rots_marmer", vorm="spits", ribben=8, rib="goud", oortjes=True)


# ============================================================================================ rotshuizen
def wand_z(W, x, zijde, y_basis, cz, hw):
    """Eerste rotsblok van de wand boven y_basis, zoekend vanaf de kloofas naar buiten (zijde -1 = noord)."""
    for k in range(0, int(hw) + 8):
        z = int(round(cz + zijde * k))
        if ZW.hoogte(W, x, z) - 1 >= y_basis + 5:
            return z
    return int(round(cz + zijde * hw))


def kantelkroon(W, x1, x2, y, c, k="rots_marmer"):
    """Nabatese trapkantelen (crow-steps) als reliëf: per 4 blokken 1-2-2-1 hoog, de rots erboven 1 terug."""
    for x in range(x1, x2 + 1):
        h = (1, 2, 2, 1)[(x - x1) % 4]
        for dy in range(h):
            zet(W, x, y + dy, c, k)
        for dy in range(h, 4):
            zet(W, x, y + dy, c, "minecraft:air")


def rotswoning(W, x1, x2, c, y0, h, zijde, winkel=False, seed=0, rand_z=None):
    """Een woning UITGEHOUWEN in de kloofwand. c = z van het eerste rotsblok van de wand; zijde -1 = noordwand
    (de gevel kijkt naar het zuiden). Nis van 2 diep tussen twee zuilen, achterin een gepleisterde gevel met boogdeur,
    raampjes met bloembakken, kroonlijst met gouden band en trapkantelen; ervoor een terrasje met potten, een luifel
    en lantaarns. winkel=True: brede open winkelpui met een kraam ervoor."""
    rng = rng_van(x1, c, seed)
    kant = (0, -zijde)

    def P(x, d, y, k):        # d > 0 = de rots in, d < 0 = de kloof in
        zet(W, x, y, c + zijde * d, k)
    gevel = ["muur_roze", "rots_marmer", "muur_perzik", "muur_vanille", "muur_roze", "muur_boter"][rng.randint(6)]
    xm = (x1 + x2) // 2
    hk = h - 4
    for x in range(x1 - 1, x2 + 2):
        for y in range(y0, y0 + h + 2):
            for d in range(0, 5):
                P(x, d, y, ZW.BANDEN[int(y // 3) % len(ZW.BANDEN)])
    for x in range(x1 + 1, x2):
        for y in range(y0, y0 + hk):
            P(x, 0, y, "minecraft:air")
            P(x, 1, y, "minecraft:air")
            P(x, 2, y, gevel)
    for x in (x1, x2):
        for y in range(y0, y0 + hk - 1):
            P(x, 0, y, "zuil" if y > y0 else "rots_marmer")
        P(x, 0, y0 + hk - 1, "zuilkop")
    for x in range(x1, x2 + 1):
        P(x, 0, y0 + hk, "goud")
        P(x, -1, y0 + hk, "minecraft:smooth_quartz_slab[type=top,waterlogged=false]")
        P(x, 0, y0 + hk + 1, "tegel_turkoois" if (x - x1) % 2 else "rots_marmer")
    kantelkroon(W, x1, x2, y0 + hk + 2, c)
    if winkel:
        for x in range(xm - 2, xm + 3):
            for y in range(y0, y0 + 4):
                for d in range(2, 7):
                    P(x, d, y, "minecraft:air")
        for x in range(xm - 2, xm + 3):
            P(x, 6, y0 + 1, ZB.WAREN[rng.randint(len(ZB.WAREN))])
            P(x, 6, y0, "hout")
        P(xm, 4, y0 + 3, lantaarn(True))
    else:
        for x in range(xm - 1, xm + 2):
            for y in range(y0, y0 + 4 if x == xm else y0 + 3):
                for d in range(2, 6):
                    P(x, d, y, "minecraft:air")
        P(xm, 5, y0 + 2, lantaarn(True))
        P(xm, 2, y0 + 4, "glas_guh")
    if hk >= 9:
        for x in (xm - 3, xm + 3):
            if x1 < x < x2:
                for y in (y0 + 6, y0 + 7):
                    P(x, 2, y, "minecraft:air")
                    P(x, 3, y, "glas")
                P(x, 2, y0 + 8, trap("wit_glad", "south" if zijde < 0 else "north", half="top"))
                P(x, 1, y0 + 5, luik((0, -zijde), half="top", open_=False))
                P(x, 1, y0 + 6, ZB.BLOEMEN[rng.randint(len(ZB.BLOEMEN))].replace("[half=lower]", ""))
        for x in range(xm - 1, xm + 2):
            P(x, 1, y0 + 5, plaat("hout", "top"))
            P(x, 0, y0 + 5, plaat("hout", "top"))
            P(x, 0, y0 + 6, "minecraft:cherry_fence")
    rng2 = rng_van(x1, y0, seed, 3)
    for x in range(x1, x2 + 1):
        for d in (-1, -2, -3):
            if W.wget(x, y0, c + zijde * d) == "minecraft:air" and W.wget(x, y0 - 1, c + zijde * d) != "minecraft:air":
                if rng2.rand() < 0.12:
                    P(x, d, y0, ZB.PLANTEN[rng2.randint(len(ZB.PLANTEN))])
    kl = [("roze", "wit"), ("turkoois", "wit"), ("geel", "oranje"), ("lila", "roze"), ("magenta", "roze")][rng.randint(5)]
    ZB.luifel(W, kant, c, xm - (2 if winkel else 1), xm + (2 if winkel else 1), y0 + 4, diep=2, kleuren=kl,
              schuin=False)
    for x in (x1, x2):
        if rng.rand() < 0.5:
            P(x, -1, y0 + 3, lantaarn(False))
    if rand_z is not None:
        for x in range(x1 - 1, x2 + 2):
            zet(W, x, y0, rand_z, "minecraft:cherry_fence" if (x - x1) % 5 else "rots_marmer")
            if (x - x1) % 10 == 0:
                zet(W, x, y0 + 1, rand_z, lantaarn(False))
    if winkel:
        ZB.kraam(W, xm, y0, c - zijde * 3, kant=(0, -zijde), seed=seed, breed=5, diep=2)


def hangende_tuin_rand(W, x1, x2, y, z, seed=0):
    rng = rng_van(x1, z, seed, 13)
    for x in range(x1, x2 + 1):
        if rng.rand() < 0.45:
            for k in range(1, rng.randint(2, 9)):
                zet(W, x, y - k, z, "minecraft:flowering_azalea_leaves" if rng.rand() < 0.5 else "minecraft:cherry_leaves",
                    keep=True)


def wandtrap(W, x, zijde, cz, hw, y_van, y_tot, richting=1):
    """Trap schuin in de wand uitgehouwen, van y_van naar y_tot (2 breed), met een muurtje aan de kloofkant."""
    for i in range(int(y_tot - y_van) + 1):
        xx = x + richting * i
        y = y_van + i
        c = wand_z(W, xx, zijde, y - 1, cz, hw)
        for w in (0, 1):
            z = c + zijde * w
            zet(W, xx, y, z, trap("vloer_zand", "east" if richting > 0 else "west"))
            for yy in range(y + 1, y + 5):
                zet(W, xx, yy, z, "minecraft:air")
        zet(W, xx, y + 1, c - zijde, ZB.muurtje("steen_vanille") if i % 4 else "rots_marmer")


def trap_tegen_wand(W, x, z, y_van, y_tot, richting=1, breed=2, k="vloer_zand"):
    """Trap langs de wand (oplopend in +x of -x), met een muurtje aan de open kant."""
    n = int(y_tot - y_van)
    for i in range(n):
        xx = x + richting * i
        for w in range(breed):
            zet(W, xx, y_van + i, z + w, trap(k, "east" if richting > 0 else "west"))
            for y in range(y_van - 6, y_van + i):
                zet(W, xx, y, z + w, "rots", keep=True)
        zet(W, xx, y_van + i + 1, z + breed, "steen_vanille")


def brug(W, x, z1, z2, y, breed=5, k="rots_marmer"):
    """Boogbrug over de kloof (noord-zuid), met balustrade, lantaarns en een vaandel in het midden."""
    if not raakt(W, x - breed, y - 20, z1, x + breed, y + 6, z2):
        return
    L = z2 - z1
    for z in range(z1, z2 + 1):
        t = (z - z1) / L
        boog_y = y - 2 - int(round(11 * (2 * t - 1) ** 2))
        for xx in range(x - breed // 2, x + breed // 2 + 1):
            zet(W, xx, y, z, "vloer_zand")
            zet(W, xx, y - 1, z, k)
            zet(W, xx, boog_y, z, k)
            if xx in (x - breed // 2, x + breed // 2):
                for yy in range(boog_y, y - 1):
                    zet(W, xx, yy, z, k if (yy - boog_y) > 0 else "goud")
        for xx in (x - breed // 2, x + breed // 2):
            zet(W, xx, y + 1, z, ZB.muurtje("marmer") if (z - z1) % 6 else "marmer_glad")
            if (z - z1) % 12 == 6:
                zet(W, xx, y + 2, z, lantaarn(False))
    zm = (z1 + z2) // 2
    for yy in range(y - 6, y - 1):
        zet(W, x, yy, zm, WOL["roze"] if yy % 2 else WOL["wit"])


def hangbrug(W, x, z1, z2, y, doorzak=4):
    """Touwbrug: planken op kettingen, met touwleuningen."""
    L = z2 - z1
    for z in range(z1, z2 + 1):
        t = (z - z1) / L
        yy = y - doorzak * 4 * t * (1 - t)
        for xx in (x - 1, x, x + 1):
            zet(W, xx, yy, z, plaat("hout"))
        for xx in (x - 2, x + 2):
            zet(W, xx, yy + 1, z, "minecraft:cherry_fence")


# ============================================================================================ luchthaven
def luchthaven(W):
    """Meertoren op de noordrand + dek over de rand + een aangemeerd luchtschip (lijn E)."""
    x, z = 3398, -786
    y0 = ZW.hoogte(W, x, z)
    if not raakt(W, x - 40, y0 - 10, z - 20, x + 40, y0 + 70, z + 40):
        return
    # stenen voet met bogen
    for dx in range(-8, 9):
        for dz in range(-8, 9):
            if math.hypot(dx, dz) <= 8.2:
                for y in range(y0 - 4, y0 + 18):
                    k = "steen_roze" if math.hypot(dx, dz) > 6.8 else "minecraft:air"
                    if y < y0:
                        k = "rots"
                    zet(W, x + dx, y, z + dz, k)
    for k in range(8):
        a = k * math.pi / 4
        for y in range(y0, y0 + 8):
            for r in (6.5, 7.5, 8.2):
                if y < y0 + 6 or abs(math.cos(a)) < 0.2 or True:
                    pass
        ZB.boog(W, (1, 0) if abs(math.cos(a)) > 0.7 else (0, 1), 0, 0, 0, 0, 0, diep=0) if False else None
    for (kant, c, u) in (((0, 1), z + 8, x + 0.5), ((0, -1), z - 8, x + 0.5), ((1, 0), x + 8, z + 0.5), ((-1, 0), x - 8, z + 0.5)):
        ZB.boog(W, kant, c, u, y0, 5, 4, diep=2, kader="tegel_roze")
    yd = y0 + 18
    for dx in range(-10, 11):
        for dz in range(-10, 11):
            if math.hypot(dx, dz) <= 10.4:
                zet(W, x + dx, yd, z + dz, "goud" if math.hypot(dx, dz) > 9.4 else "vloer_dambord")
                if 9.4 < math.hypot(dx, dz) <= 10.4:
                    zet(W, x + dx, yd + 1, z + dz, "minecraft:cherry_fence")
    # mast met lantaarn en vaandels
    for y in range(yd + 1, yd + 26):
        zet(W, x, y, z, "minecraft:stripped_cherry_log[axis=y]")
    for y in range(yd + 4, yd + 10):
        zet(W, x + 1, y, z, WOL["roze"] if y % 2 else WOL["wit"])
    zet(W, x, yd + 26, z, "goudblok")
    zet(W, x, yd + 27, z, "minecraft:pearlescent_froglight")
    ZB.guh_oortjes(W, x, yd + 26, z, 1.4)
    # loopbrug naar de kloofrand + het schip (ligt boven de kloof, neus naar het oosten)
    for zz in range(z + 10, z + 26):
        for xx in range(x - 1, x + 2):
            zet(W, xx, yd, zz, plaat("hout", "top"))
        zet(W, x - 2, yd + 1, zz, "minecraft:cherry_fence")
        zet(W, x + 2, yd + 1, zz, "minecraft:cherry_fence")
    import wijken as WK
    WK.luchtschip(W, x + 2, yd, z + 33, richting=(1, 0), L=58, ballon=("minecraft:pink_wool", "minecraft:white_wool"))
    # vertrekbord
    for y in range(yd + 1, yd + 4):
        zet(W, x + 4, y, z + 8, "minecraft:cherry_fence")
    doos(W, x + 3, yd + 4, z + 8, x + 6, yd + 5, z + 8, "hout")
    W.label(x, yd + 30, z, "Luchthaven (lijn E naar Hoogvads)")


# ============================================================================================ de stad
def bouw(W, figuren=True):
    """Bouwt de hele kloofstad (alleen wat in het venster valt)."""
    if not raakt(W, *BOX, marge=0):
        return
    K = ZW.KLOOF
    # ---- de wanden: per wandvlak (bodem->richel 1, richel 1->richel 2, richel 2->plateau) uitgehouwen woningen,
    #      winkels (onderaan), rotsgevels en trappen; hangende tuinen langs de richelranden
    Q, DY = ZW.Q_RICHEL, ZW.DY_RICHEL
    for zijde in (-1, 1):
        for f in range(3):
            x = 3318 + (6 if zijde > 0 else 0) + f * 3
            while x < ZW.KLOOF_EIND - 14:
                p, _ = as_punt(t_van_x(x))
                cx, cz, hw, bodem = p
                rng = rng_van(x, zijde, f, 77)
                w = int(rng.randint(8, 13))
                y0 = int(bodem + (0, DY[0], DY[1])[f]) + 1
                hoog = (DY[0], DY[1] - DY[0], 16)[f]
                c = wand_z(W, x + w // 2, zijde, y0, cz, hw)
                rand = None
                if f > 0:
                    rand = int(round(cz + zijde * hw * Q[f - 1])) - zijde
                soort = rng.rand()
                if soort < 0.16 and f < 2:
                    rotsgevel(W, x + w // 2, y0, c, (0, -zijde), breed=11, hoog=hoog - 2, seed=x + f)
                elif soort < 0.24 and f < 2:
                    wandtrap(W, x, zijde, cz, hw, y0, y0 + hoog, richting=1)
                    w = hoog + 2
                else:
                    rotswoning(W, x, x + w - 1, c, y0, min(hoog, 15), zijde, winkel=(f == 0 and rng.rand() < 0.45),
                               seed=x + f * 13, rand_z=rand)
                if rand is not None:
                    hangende_tuin_rand(W, x, x + w, y0 - 1, rand, seed=x)
                x += w + int(rng.randint(2, 5))
    # ---- de Schatkamer: de grote gevel aan het oosteinde, kijkt de kloof in (naar het westen, avondzon)
    p, d = as_punt(t_van_x(ZW.KLOOF_EIND - 1))
    sk_y = ZW.hoogte(W, ZW.KLOOF_EIND - 4, int(p[1]))
    rotsgevel(W, ZW.KLOOF_EIND + 1, sk_y, int(p[1]), (-1, 0), breed=27, hoog=46, groot=True, seed=1)
    W.label(ZW.KLOOF_EIND, sk_y + 50, int(p[1]), "De Schatkamer (hal van Kloofmoeder Roosje)")
    # een tweede grote gevel in de noordwand, bij het plein
    p, d = as_punt(t_van_x(3420))
    rotsgevel(W, 3420, int(p[3]) + 1, int(p[1] - p[2] * Q[0]) - 1, (0, 1), breed=19, hoog=DY[0] - 1, seed=2)
    # ---- bruggen over de kloof
    for bx, r in ((3352, 1), (3446, 1), (3396, 2)):
        p, d = as_punt(t_van_x(bx))
        cx, cz, hw, bodem = p
        if r == 1:
            brug(W, bx, int(cz - hw * Q[1]), int(cz + hw * Q[1]), int(bodem + DY[1]), breed=5)
        else:
            hangbrug(W, bx, int(cz - hw * 0.92), int(cz + hw * 0.92), int(bodem + DY[2]) + 1)
    # ---- lampionsnoeren tussen de wanden
    for lx in range(3330, 3470, 12):
        p, d = as_punt(t_van_x(lx))
        cx, cz, hw, bodem = p
        y = bodem + ZW.DY_RICHEL[0] + 5
        ZB.lampionsnoer(W, (lx, y, cz - hw * 0.41), (lx + 3, y, cz + hw * 0.41), doorzak=4)
    # ---- de kloofbodem: plein, fontein, markt, palmen, stal, tapijtwever
    plein = (3400, -714)
    yb = ZW.hoogte(W, *plein)
    for dx in range(-22, 23):
        for dz in range(-10, 11):
            x, z = plein[0] + dx, plein[1] + dz
            if (dx / 22.0) ** 2 + (dz / 10.0) ** 2 <= 1 and not ZW.is_water(W, x, z):
                zet(W, x, yb - 1, z, "vloer_dambord" if (abs(dx) + abs(dz)) % 7 else "tegel_roze")
                leeg(W, x, yb, z, x, yb + 3, z)
    ZB.fontein(W, plein[0], yb, plein[1], r=5)
    for i, (mx, mz, kk) in enumerate(((3384, -706, (0, -1)), (3392, -706, (0, -1)), (3408, -706, (0, -1)),
                                      (3416, -706, (0, -1)), (3380, -722, (0, 1)), (3420, -722, (0, 1)))):
        ZB.kraam(W, mx, ZW.hoogte(W, mx, mz), mz, kant=kk, seed=i)
    W.label(plein[0], yb + 8, plein[1], "Het Rozenplein")
    # beek: bruggetjes
    for bx in range(3320, 3480, 22):
        p, d = as_punt(t_van_x(bx))
        y = int(p[3]) + 1
        for dz in range(-3, 4):
            zet(W, bx, y, int(p[1]) + dz, plaat("hout"))
            zet(W, bx + 1, y, int(p[1]) + dz, plaat("hout"))
    # palmen langs de beek
    for i, px in enumerate(range(3318, ZW.KLOOF_EIND - 6, 17)):
        p, d = as_punt(t_van_x(px))
        for zz in (p[1] - 6 - (i % 3), p[1] + 6 + (i % 2)):
            if abs(px - plein[0]) < 24 and abs(zz - plein[1]) < 11:
                continue
            if not ZW.is_water(W, px, zz):
                ZB.palm(W, px, ZW.hoogte(W, px, int(zz)), int(zz), seed=i)
    # bultguh-stal (karavanserai-hoekje) aan de oostkant van het plein
    stal = (3436, -720)
    ys = ZW.hoogte(W, *stal)
    doos(W, stal[0] - 5, ys - 1, stal[1] - 4, stal[0] + 5, ys - 1, stal[1] + 4, "minecraft:hay_block[axis=y]")
    for (sx, sz) in ((stal[0] - 5, stal[1] - 4), (stal[0] + 5, stal[1] - 4), (stal[0] - 5, stal[1] + 4),
                     (stal[0] + 5, stal[1] + 4)):
        doos(W, sx, ys, sz, sx, ys + 4, sz, "minecraft:cherry_fence")
    for xx in range(stal[0] - 6, stal[0] + 7):
        for zz in range(stal[1] - 5, stal[1] + 6):
            zet(W, xx, ys + 5, zz, WOL["oranje" if (xx // 2) % 2 else "geel"])
    W.label(stal[0], ys + 8, stal[1], "Bultguh-stal")
    W.stal = (stal[0], ys, stal[1])
    W.plein = (plein[0], yb, plein[1])
    # ---- de Zeemond: pier
    for xx in range(3236, 3284):
        for zz in (-746, -745, -744):
            zet(W, xx, ZW.ZEE + 1, zz, plaat("hout", "top"))
        if xx % 6 == 0:
            for y in range(ZW.ZEE - 4, ZW.ZEE + 3):
                zet(W, xx, y, -747, "minecraft:stripped_cherry_log[axis=y]")
            zet(W, xx, ZW.ZEE + 3, -747, lantaarn(False))
    luchthaven(W)
    W.label(3290, 80, -742, "De Zeemond")
    W.label(3490, 100, -758, "De Siq (naar het plateau en de karavaanweg)")
