"""De Ordekathedraal van Hoogvads (wereldcoördinaten).

Westgevel op x = X0 met de Grote Poort (60 hoog, spitsboog met getrapte bogen, deuren + guh-roosvenster in het
boogveld), twee gevel-torens met achthoekige spitsen (oortjes!), schip met zijbeuken, luchtbogen en pinakels,
een transept met roosvensters, een gouden koepel op de kruising en een koor met een halfronde apsis.
Binnen: zuilen, spitsbogen, gewelf met gouden ribben, dambordvloer, banken, altaar met een gouden guh-beeld.
"""
import math
import numpy as np
import palet
from palet import P, trap, plaat
import bouwhulp as BH

X0 = 18            # westgevel
X_KRUIS = (74, 92)
X_KOOR = 106       # middelpunt apsis
Y0 = 244           # vloer
NB = 13            # halve binnenbreedte schip (z -13..13)
ZB = 30            # buitenkant zijbeuken
H_SCHIP = 58       # binnenhoogte schip -> muren tot Y0+58  [zone 5 kaart] was 46: the whale hangs 20 lower in the game, so the
                   # cathedral grows (Grote Poort ~58 high as in the story; gable spire top local 338 = game 318)
TOREN_H = 58       # [zone 5 kaart] front towers (was 46)
GALM_Y = 46        # [zone 5 kaart] belfry floor above Y0 (the galmgaten; was 36)
SPITS_TOP = 336    # [zone 5 kaart] local y of the tower spires (was 316)
H_BEUK = 28   # [zone 5 kaart] was 24
TRANSEPT_Z = 46


def _gewelf_hoogte(dz, breed, y_aanzet, pijl):
    q = min(1.0, abs(dz) / breed)
    return y_aanzet + pijl * (1 - q ** 1.7) ** 0.6


def schip(W):
    ytop = Y0 + H_SCHIP
    # vloeren
    BH.doos(W, X0, Y0, -ZB, X_KOOR, Y0, ZB, "vloer")
    # buitenmuren zijbeuken
    for s in (-1, 1):
        BH.doos(W, X0 + 4, Y0 + 1, s * ZB, X_KRUIS[0] - 1, Y0 + H_BEUK, s * ZB, "marmer")
        BH.doos(W, X0 + 4, Y0 + 1, s * (ZB - 1), X_KRUIS[0] - 1, Y0 + 1, s * (ZB - 1), "marmer_glad")
        BH.doos(W, X_KRUIS[1] + 1, Y0 + 1, s * ZB, X_KOOR, Y0 + H_BEUK, s * ZB, "marmer")
        # schipmuren (lichtbeuk) op z = +-(NB+1), +-(NB+2)
        BH.doos(W, X0 + 4, Y0 + H_BEUK - 6, s * (NB + 1), X_KOOR, ytop, s * (NB + 2), "marmer")
        BH.doos(W, X0 + 4, Y0 + H_BEUK + 1, s * (NB + 1), X_KOOR, Y0 + H_BEUK + 1, s * (NB + 2), "goudrand")
        BH.doos(W, X0 + 4, ytop, s * (NB + 1), X_KOOR, ytop, s * (NB + 3), "goud")
        BH.doos(W, X0 + 4, Y0 + H_BEUK, s * (NB + 3), X_KOOR, Y0 + H_BEUK, s * (ZB + 1), "goud")
    # zuilen + arcade (bogen tussen schip en zijbeuken)
    xs = list(range(X0 + 10, X_KRUIS[0] - 2, 10)) + list(range(X_KRUIS[1] + 6, X_KOOR - 2, 10))
    W._kath_zuilen = xs
    for s in (-1, 1):
        for x in xs:
            BH.zuil(W, x, s * (NB + 1), Y0, Y0 + H_BEUK - 8, dik=3, schacht="zuil", kop="zuilkop", kap=None)
        # spitsbogen tussen de zuilen: muur vullen boven de boog
        for i in range(len(xs) - 1):
            xa, xb = xs[i] + 2, xs[i + 1] - 2
            if xb - xa < 3:
                continue
            breed = xb - xa + 1
            for x in range(xa, xb + 1):
                du = x + 0.5 - (xa + xb + 1) / 2.0
                top = BH.boog_vorm(du, breed, 10, 1.1)
                for y in range(int(Y0 + 1 + top), Y0 + H_BEUK - 5):
                    BH.zet(W, x, y, s * (NB + 1), "marmer")
                    BH.zet(W, x, y, s * (NB + 2), "marmer")
                BH.zet(W, x, int(Y0 + 1 + top), s * (NB + 1), "goud")
        # triforium-strook en lichtbeuk-ramen
        for i in range(len(xs) - 1):
            xm = (xs[i] + xs[i + 1]) / 2.0
            BH.boograam(W, "z", (NB + 1) if s > 0 else -(NB + 2), xm, Y0 + H_BEUK + 6, 6, 14, glas="glas",
                        kader="goud", spits=1.3, diep=2, midstijl="goud")
            # ramen in de zijbeuk
            BH.boograam(W, "z", s * ZB, xm, Y0 + 5, 5, 13, glas="glas_guh" if i % 2 else "glas_hart", kader="goud",
                        spits=1.2, diep=1)
    # gewelf: spitstongewelf boven het schip, gouden gordelbogen
    for x in range(X0 + 4, X_KOOR + 1):
        rib = (x - X0) % 10 == 0
        for dz in range(-NB, NB + 1):
            y = _gewelf_hoogte(dz + 0.5 * np.sign(dz), NB + 1, ytop - 12, 12)
            BH.zet(W, x, int(y), dz, "goud" if (rib or dz == 0) else "marmer_tegels")
            BH.zet(W, x, int(y) + 1, dz, "marmer")
    # gewelf zijbeuken (plat met ribben)
    for s in (-1, 1):
        BH.doos(W, X0 + 4, Y0 + H_BEUK - 1, s * (NB + 3), X_KOOR, Y0 + H_BEUK - 1, s * (ZB - 1), "marmer_tegels")
    # daken: schip (steil zadeldak, nok langs x), zijbeuken (lessenaar)
    BH.zadeldak(W, X0 + 4, -(NB + 2), X_KOOR, NB + 2, ytop + 1, nok="x", dak="dak_roze", overstek=1,
                gevel=P["marmer"])
    for s in (-1, 1):
        for i, z in enumerate(range(NB + 3, ZB + 2)):
            y = Y0 + H_BEUK + 1 + (ZB + 1 - z) // 2
            BH.doos(W, X0 + 4, y, s * z, X_KOOR, y, s * z, trap("dak_roze_trap", "north" if s > 0 else "south"))
    # nokkam met gouden puntjes
    nok_y = ytop + 1 + (2 * NB + 7) // 2
    for x in range(X0 + 4, X_KOOR + 1, 4):
        BH.zet(W, x, nok_y + 1, 0, "goud")


def steunberen(W):
    """Steunberen langs de zijbeuken met pinakels, en luchtbogen naar de schipmuur."""
    xs = [x for x in range(X0 + 10, X_KOOR - 1, 10) if not (X_KRUIS[0] - 2 <= x <= X_KRUIS[1] + 2)]
    for s in (-1, 1):
        for x in xs:
            z0 = s * (ZB + 1)
            BH.doos(W, x - 1, Y0, z0, x + 1, Y0 + H_BEUK + 14, s * (ZB + 3), "marmer_glad")
            BH.doos(W, x - 1, Y0 + H_BEUK + 2, s * (ZB + 1), x + 1, Y0 + H_BEUK + 2, s * (ZB + 3), "goud")
            BH.spits(W, x, s * (ZB + 2), Y0 + H_BEUK + 15, 2, 9, dak="dak_goud", hoeken=4, rand=None, oortjes=False)
            # luchtboog: van de pier (y Y0+H_BEUK+12) naar de schipmuur (y Y0+H_SCHIP-8)
            za, zb = ZB + 1, NB + 3
            ya, yb = Y0 + H_BEUK + 10, Y0 + H_SCHIP - 10
            for k in range(0, za - zb + 1):
                t = k / float(za - zb)
                y = ya + (yb - ya) * t + 6 * math.sin(math.pi * t)
                z = s * (za - k)
                BH.zet(W, x, int(y), z, "wit_glad")
                BH.zet(W, x, int(y) - 1, z, "marmer_glad" if 0.15 < t < 0.85 else "wit_glad")
                BH.zet(W, x, int(y) + 1, z, "goud_plaat" if False else plaat("goud_plaat"))


def gevel(W):
    """Westgevel met de Grote Poort (60 hoog) en twee torens."""
    xg = X0
    ytop = Y0 + H_SCHIP
    # middendeel gevel z -12..12, 4 dik, tot een topgevel
    BH.doos(W, xg, Y0, -12, xg + 4, ytop + 4, 12, "marmer")
    for k in range(0, 24):
        hw = 12 - k // 2
        BH.doos(W, xg, ytop + 4 + k, -hw, xg + 4, ytop + 4 + k, hw, "marmer")
        BH.zet(W, xg - 1, ytop + 4 + k, -hw, "goud")
        BH.zet(W, xg - 1, ytop + 4 + k, hw, "goud")
    BH.spits(W, xg + 2, 0, ytop + 28, 2, 6, dak="dak_goud", hoeken=4, rand=None)   # [zone 5 kaart] h 8 -> 6 (ears stay under y 320)
    # Grote Poort: getrapte spitsbogen (archivolten), 60 hoog
    breed0, h_recht = 22, 45   # [zone 5 kaart] was 32: the Grote Poort is ~57 high now
    for k, (kl, dx) in enumerate((("wit_glad", -1), ("goud", 0), ("marmer_glad", 1), ("goud", 2), ("wit_glad", 3))):
        br = breed0 - 2 * k
        hr = h_recht - k
        for du in range(-br // 2, br // 2 + 1):
            top = BH.boog_vorm(du, br, hr, 0.95)
            for y in range(Y0 + 1, int(Y0 + 1 + top) + 1):
                for x in range(xg - 1, xg + dx + 1):
                    BH.zet(W, x, y, du, "minecraft:air")
            BH.zet(W, xg + dx, int(Y0 + 1 + top), du, kl)
            BH.zet(W, xg + dx, int(Y0 + top), du, kl)
    # achterwand van de poort: deuren + boogveld met guh-roos
    xa = xg + 4
    BH.doos(W, xa, Y0 + 1, -6, xa, Y0 + 58, 6, "marmer_glad")
    BH.doos(W, xa, Y0 + 1, -5, xa, Y0 + 20, 5, "goudblok")
    BH.doos(W, xa, Y0 + 1, 0, xa, Y0 + 20, 0, "goud")
    BH.doos(W, xa, Y0 + 21, -6, xa, Y0 + 21, 6, "goud")
    BH.guh_roos(W, "x", xa, 0.5, Y0 + 34, 6.5, diep=1, kader="filigraan")   # [zone 5 kaart] higher + bigger (portal 58 high)
    # beelden in nissen naast de poort (guh-heiligen)
    for s in (-1, 1):
        z = s * 14
        for (dy, k) in ((0, "beeld"), (1, "beeld"), (2, "beeld"), (3, "beeld_gezicht")):
            BH.zet(W, xg - 2, Y0 + 26 + dy, z, k)
        BH.zet(W, xg - 2, Y0 + 30, z - 1, "beeld")
        BH.zet(W, xg - 2, Y0 + 30, z + 1, "beeld")
        BH.zet(W, xg - 2, Y0 + 25, z, "goud")
    # groot guh-roosvenster boven de poort
    BH.guh_roos(W, "x", xg, 0.5, ytop + 7, 6.5, diep=2, kader="wit_glad")
    BH.doos(W, xg - 1, ytop - 1, -12, xg - 1, ytop - 1, 12, "goud")
    # galerij van guh-beeldjes op de gevelrand
    for z in range(-11, 12, 2):
        BH.zet(W, xg - 1, ytop, z, "beeld")
        BH.zet(W, xg - 1, ytop + 1, z, "beeld_gezicht")
    # torens
    for s in (-1, 1):
        toren(W, xg - 1, s * 20)


def toren(W, x0, zc):
    """Vierkante geveltoren (15 breed) met hoekpilasters, galmgaten, een gouden balustrade en een achthoekige spits."""
    r = 7
    y_romp = Y0 + TOREN_H
    BH.doos(W, x0, Y0, zc - r, x0 + 2 * r, y_romp, zc + r, "marmer")
    # hoekpilasters
    for dx in (0, 2 * r):
        for dz in (-r, r):
            BH.doos(W, x0 + dx - (1 if dx == 0 else -1) * 0, Y0, zc + dz, x0 + dx, y_romp + 2, zc + dz, "wit_glad")
    # banden
    for y in (Y0 + 28, Y0 + GALM_Y - 2, y_romp):
        BH.doos(W, x0 - 1, y, zc - r - 1, x0 + 2 * r + 1, y, zc + r + 1, "goud")
    # ramen: lange lancetten en galmgaten
    BH.boograam(W, "x", x0 - 0, zc + 0.5, Y0 + 30, 5, 8, glas="glas", kader="goud", diep=1, spits=1.3)
    for (vlak, c) in (("x", x0), ("z", zc - r), ("z", zc + r)):
        u = zc + 0.5 if vlak == "x" else x0 + r + 0.5
        BH.boograam(W, vlak, c, u, Y0 + GALM_Y, 7, 5, glas="minecraft:air", kader="goud", diep=1, spits=1.3,
                    midstijl="wit_glad")
    # guhgezicht (gebeiteld) onder de galmgaten
    for (dz, blok) in ((-3, "marmer_gezicht"), (3, "marmer_gezicht")):
        pass
    BH.zet(W, x0 - 1, Y0 + GALM_Y - 3, zc, "marmer_gezicht")
    # balustrade + hoektorentjes
    BH.muren(W, x0 - 1, y_romp + 1, zc - r - 1, x0 + 2 * r + 1, y_romp + 1, zc + r + 1, "wit_muur")
    for dx in (-1, 2 * r + 1):
        for dz in (-r - 1, r + 1):
            BH.spits(W, x0 + dx, zc + dz, y_romp + 2, 1.5, 7, dak="dak_goud", hoeken=4, rand=None, oortjes=False)
    # achthoekige spits
    BH.spits(W, x0 + r, zc, y_romp + 1, r - 0.5, SPITS_TOP - y_romp - 1, dak="dak_roze", hoeken=8, rand="goud")
    # vaandel aan de toren
    BH.vaandel(W, x0 - 1, Y0 + 18, zc + (r - 2) * (1 if zc > 0 else -1), "west")


def transept(W):
    ytop = Y0 + H_SCHIP
    xa, xb = X_KRUIS
    for s in (-1, 1):
        BH.doos(W, xa, Y0, s * (NB + 1), xb, Y0, s * TRANSEPT_Z, "vloer")
        BH.doos(W, xa, Y0 + 1, s * (ZB + 1), xa, ytop, s * TRANSEPT_Z, "marmer")
        BH.doos(W, xb, Y0 + 1, s * (ZB + 1), xb, ytop, s * TRANSEPT_Z, "marmer")
        BH.doos(W, xa, Y0 + 1, s * TRANSEPT_Z, xb, ytop + 4, s * TRANSEPT_Z, "marmer")
        BH.doos(W, xa, Y0 + 1, s * (TRANSEPT_Z - 1), xb, Y0 + 3, s * (TRANSEPT_Z - 1), "marmer_glad")
        # topgevel van de transeptgevel
        for k in range(0, 11):
            BH.doos(W, xa + k, ytop + 4 + k, s * TRANSEPT_Z, xb - k, ytop + 4 + k, s * TRANSEPT_Z, "marmer")
        # roosvenster
        BH.rond_raam(W, "z", s * TRANSEPT_Z, (xa + xb + 1) / 2.0, ytop - 16, 7.5, diep=1, kader="wit_glad")
        # portaal
        for du in range(-4, 5):
            top = BH.boog_vorm(du, 9, 12, 1.0)
            for y in range(Y0 + 1, int(Y0 + 1 + top)):
                BH.zet(W, (xa + xb) // 2 + du, y, s * TRANSEPT_Z, "goudblok" if y < Y0 + 13 else "glas")
            BH.zet(W, (xa + xb) // 2 + du, int(Y0 + 1 + top), s * TRANSEPT_Z, "goud")
        # ramen in de transeptmuren
        for x in (xa, xb):
            for zz in (ZB + 6, ZB + 12):
                BH.boograam(W, "x", x, s * zz + 0.5, Y0 + 8, 4, 18, glas="glas", kader="goud", diep=1)
        BH.zadeldak(W, xa, s * (NB + 3) if s > 0 else -TRANSEPT_Z, xb, s * TRANSEPT_Z if s > 0 else -(NB + 3),
                    ytop + 1, nok="z", dak="dak_roze", overstek=1, gevel=P["marmer"])
        BH.vaandel(W, xa - 1, Y0 + 20, s * (TRANSEPT_Z - 4), "west")
        BH.vaandel(W, xb + 1, Y0 + 20, s * (TRANSEPT_Z - 4), "east")
    # kruisingskoepel
    cx = (xa + xb) // 2
    BH.doos(W, xa, ytop, -NB - 2, xb, ytop + 2, NB + 2, "marmer_glad")
    BH.koepel(W, cx, ytop + 3, 0, 12, ry=10, dak="dak_goud", rib="wit_glad", ribben=16, trommel=5,
              trommel_blok="marmer_glad", ramen="glas")


def koor(W):
    ytop = Y0 + H_SCHIP
    cx = X_KOOR
    r = NB + 2
    # apsis: halve cirkel naar het oosten
    for x in range(cx, cx + r + 4):
        for z in range(-r - 3, r + 4):
            d = math.hypot(x - cx, z)
            if x >= cx and d <= r + 0.5:
                BH.zet(W, x, Y0, z, "vloer")
            if x >= cx and r - 1.5 < d <= r + 0.5:
                for y in range(Y0 + 1, ytop + 1):
                    a = math.degrees(math.atan2(z, x - cx))
                    raam = (Y0 + 10 <= y <= ytop - 12) and (int((a + 90) / 22.5) % 2 == 1)
                    BH.zet(W, x, y, z, "glas_guh" if raam and y > Y0 + 24 else ("glas" if raam else "marmer"))
            if x >= cx and r + 0.5 < d <= r + 3.5 and abs(z) % 6 == 0:
                BH.doos(W, x, Y0, z, x, Y0 + 40, z, "marmer_glad")
    # halve koepel als dak
    for dy in range(0, r + 2):
        for x in range(cx, cx + r + 3):
            for z in range(-r - 2, r + 3):
                d = ((x - cx) / (r + 1.5)) ** 2 + (z / (r + 1.5)) ** 2 + (dy / (r * 0.9)) ** 2
                if 0.8 < d <= 1:
                    BH.zet(W, x, ytop + 1 + dy, z, "dak_roze")
    BH.spits(W, cx + 2, 0, ytop + int(r * 0.9) + 1, 2, 10, dak="dak_goud", hoeken=4, rand=None)


def interieur(W, figuren=True):
    ytop = Y0 + H_SCHIP
    # middenpad (loper) en banken
    BH.doos(W, X0 + 5, Y0, -2, X_KRUIS[0] - 1, Y0, 2, "minecraft:pink_wool")
    BH.doos(W, X0 + 5, Y0, -3, X_KRUIS[0] - 1, Y0, -3, "goud")
    BH.doos(W, X0 + 5, Y0, 3, X_KRUIS[0] - 1, Y0, 3, "goud")
    bank = "minecraft:cherry_stairs" if palet.MODUS == "vanilla" else "guhs:vadshout_trap"
    import bouwhulp
    for x in range(X0 + 12, X_KRUIS[0] - 5, 3):
        for s in (-1, 1):
            for z in range(4, NB):
                BH.zet(W, x, Y0 + 1, s * z, bank + "[facing=west,half=bottom,shape=straight,waterlogged=false]")
    # altaar op de kruising
    cx = (X_KRUIS[0] + X_KRUIS[1]) // 2
    for k in range(3):
        BH.doos(W, cx - 7 + k, Y0 + 1 + k, -9 + k, cx + 7 - k, Y0 + 1 + k, 9 - k, "marmer_glad" if k < 2 else "vloer")
    BH.doos(W, cx - 1, Y0 + 4, -3, cx + 1, Y0 + 5, 3, "goudblok")
    BH.doos(W, cx - 1, Y0 + 6, -3, cx + 1, Y0 + 6, 3, "filigraan")
    for z in (-3, -1, 1, 3):
        BH.zet(W, cx, Y0 + 7, z, "minecraft:white_candle[candles=4,lit=true,waterlogged=false]")
    # het gouden guh-beeld in de apsis ('de Eerste Knuffel')
    bx = X_KOOR + 4
    beeld_guh(W, bx, Y0 + 1, 0, 14, "beeld_goud", "west")
    # kroonluchters
    # [U-Z5] the nave chandeliers hang from the new rib vaults: kathedraal3.binnen
    # vaandels aan de zuilen
    for x in getattr(W, "_kath_zuilen", [])[::2]:
        for s in (-1, 1):
            BH.vaandel(W, x, Y0 + 16, s * (NB - 1), "south" if s < 0 else "north")
    # kaarsen langs de wanden
    for x in range(X0 + 8, X_KRUIS[0], 5):
        for s in (-1, 1):
            BH.zet(W, x, Y0 + 1, s * (ZB - 2), "minecraft:pink_candle[candles=3,lit=true,waterlogged=false]")


def luchter(W, x, y, z, groot=False):
    """[U-Z5] a giant chain chandelier (gotiek.kroonluchter; the throne room uses this too)."""
    import gotiek as G
    G.kroonluchter(W, x, y + 3, z, r=5.5 if groot else 4.2, lagen=4 if groot else 3, ymax=y + 40)


def luchter_oud(W, x, y, z, groot=False):
    r = 4 if groot else 3
    for dy in range(0, 14):
        BH.zet(W, x, y + 4 + dy, z, "minecraft:iron_chain[axis=y,waterlogged=false]")
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if r - 0.7 < d <= r + 0.3:
                BH.zet(W, x + dx, y + 2, z + dz, "goud_plaat" if False else plaat("goud_plaat"))
                if (dx + dz) % 2 == 0:
                    BH.zet(W, x + dx, y + 3, z + dz, "minecraft:white_candle[candles=1,lit=true,waterlogged=false]")
    BH.zet(W, x, y + 2, z, "goudblok")
    BH.zet(W, x, y + 1, z, "minecraft:lantern[hanging=false,waterlogged=false]")


def beeld_guh(W, x, y, z, h, blok, kijk="west"):
    """Groot guh-beeld: buikje (ellipsoïde), hoofd met oortjes, armpjes open (knuffel). h = hoogte."""
    s = h / 14.0
    W.bol((x + 0.5, y + 4 * s, z + 0.5), 4.2 * s, P[blok], ry=4.6 * s)
    W.bol((x + 0.5, y + 10 * s, z + 0.5), 4.4 * s, P[blok], ry=3.8 * s)
    for k in (-1, 1):
        W.bol((x + 0.5, y + 13.6 * s, z + 0.5 + k * 2.6 * s), 1.5 * s, P[blok], ry=1.8 * s)
        W.buis([(x + 0.5, y + 7 * s, z + 0.5 + k * 3.5 * s), (x - 1.5 * s, y + 8.5 * s, z + 0.5 + k * 6.5 * s)],
               [1.2 * s, 1.0 * s], P[blok])
    # sokkel
    BH.doos(W, x - 4, y - 1, z - 4, x + 4, y, z + 4, "wit_glad")
    BH.doos(W, x - 4, y + 1 - 1, z - 4, x + 4, y - 1, z + 4, "goud")


def bouw(W, figuren=True):
    schip(W)
    steunberen(W)
    transept(W)
    koor(W)
    gevel(W)
    import kathedraal3 as K3   # [U-Z5] depth: buttresses, portals, tracery, flyers, pinnacles; rib vaults, chandeliers
    K3.buiten(W)
    interieur(W, figuren)
    K3.binnen(W)
    W.label(X0 - 2, Y0 + 46, 0, "de Grote Poort")
    W.label(X0 + 6, 316, 20, "geveltorens met oortjes")
    W.label((X_KRUIS[0] + X_KRUIS[1]) / 2, 306, 0, "gouden kruisingskoepel")
    W.label(50, Y0 + H_SCHIP + 8, 30, "luchtbogen + pinakels")


def figuren(W, rng=None):
    """Guhs in de kathedraal: kerkgangers in de banken, een paar op het middenpad, een koortje bij het altaar."""
    rng = rng or np.random.RandomState(5)
    skins = ["guh", "guh_mint", "guh_wolk", "guh_bloesemguh", "guh_snow", "guh_pluisguh", "guh_golden"]
    for x in range(X0 + 12, X_KRUIS[0] - 5, 3):
        for s in (-1, 1):
            for z in range(4, NB):
                if rng.rand() < 0.35:
                    BH.guh(W, x + 0.5, Y0 + 1.5, s * z + 0.5, yaw=90, skin=skins[rng.randint(len(skins))], zit=True)
    for k in range(8):
        BH.guh(W, X0 + 10 + rng.uniform(0, 50), Y0 + 1, rng.uniform(-1.5, 1.5), yaw=rng.choice([90, 270]),
               skin=skins[rng.randint(len(skins))])
    cx = (X_KRUIS[0] + X_KRUIS[1]) // 2
    for z in range(-6, 7, 2):
        BH.guh(W, cx + 5.5, Y0 + 3, z + 0.5, yaw=270, skin="guh_wolk")
