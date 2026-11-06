"""[U-Z5] Ronde 3 van de Ordekathedraal: diepte (gotiek.py) op het bestaande grondplan.

buiten(W): westgevel (koningsgalerij, steunberen met pinakels, blinde zijportalen, diepe torenramen, roos met omlijsting,
           hogels op de spitsen), flanken (omlijste zijbeukramen met maaswerk, wimpergen, borstwering, waterspuwers, dubbele
           luchtbogen, lichtbeukpilasters), transept (diepe portalen, traptorentjes met spitsen, roos-omlijsting), koor
           (pinakels op de straalsteunberen), dak (dakkapellen, nokkam, vier spitsjes rond de koepel).
binnen(W): kruisribgewelven (schip, zijbeuken), de kruising open tot in de koepel, bundelpijlers met diensten, een
           triforium, reuzenkroonluchters, kandelaars bij het altaar.
De grondplan-maten (X0, Y0, NB, ZB, ...) en alles waar bouw5 op rekent (de poort, de torentrap, het plein) blijven.
"""
import math

import numpy as np

import palet
from palet import P, trap, plaat
import bouwhulp as BH
import gotiek as G
import kathedraal as KA


def _grond(W, x, z, y_van, n=14):
    """Vult onder (x, z) vanaf y_van naar beneden aan tot de grond (max n), zodat iets niet zweeft."""
    for k in range(n):
        y = y_van - k
        if not G.lucht(W, x, y, z):
            return
        BH.zet(W, x, y, z, P["steen_bak"])


def _fundament(W, vl, u1, u2, d1, d2, y_van):
    for u in range(int(u1), int(u2) + 1):
        for d in range(d1, d2 + 1):
            x, z = vl.p(u, d)
            _grond(W, x, z, y_van)


# ============================================================================================== buiten
def westgevel(W):
    X0, Y0 = KA.X0, KA.Y0
    ytop = Y0 + KA.H_SCHIP
    y_romp = Y0 + KA.TOREN_H
    vt = G.Vlak("x", X0 - 1, -1)          # torens: voorvlak x = 17
    vm = G.Vlak("x", X0, -1)              # middendeel: x = 18
    # ---- steunberen tegen de torens (binnen- en buitenhoek), versprongen, met pinakels
    for s in (-1, 1):
        for (u, br) in ((s * 13, 0), (s * 27, 0)):
            _fundament(W, vt, u - br, u + br, 1, 4, Y0)
            G.steunbeer(W, vt, u, Y0 - 1, y_romp + 1, diep=4, breed=br, pinakel_h=10, nis_y=Y0 + 9,
                        gargouille_y=Y0 + KA.GALM_Y - 4)
    # ---- blinde zijportalen in de torenvoeten
    for s in (-1, 1):
        zc = s * 20
        x, z = vt.p(zc, 0)
        BH.doos(W, x, Y0 + 1, zc - 2, x, Y0 + 9, zc + 2, "goudblok")      # dichte deurvleugels
        BH.doos(W, x, Y0 + 1, zc, x, Y0 + 9, zc, "goud")
        _fundament(W, vt, zc - 6, zc + 6, 1, 2, Y0)
        G.portaal(W, vt, zc + 0.5, Y0 + 1, 5, 9, lagen=2, gevel=False)
    # ---- koningsgalerij (nissen met guh-heiligen) over de torens + diepe lancetten
    for s in (-1, 1):
        for u in range(14, 27, 2):
            if abs(u - 20) <= 0:
                continue
            G.nis(W, vt, s * u, Y0 + 21, h=4)
        zc = s * 20
        G.omlijsting(W, vt, zc + 0.5, Y0 + 30, 5, 8, lagen=2, spits=1.3)
        G.maaswerk(W, vt, zc + 0.5, Y0 + 30, 5, 8, spits=1.3)
        # galmgaten: omlijsting op de drie open kanten
        G.omlijsting(W, vt, zc + 0.5, Y0 + KA.GALM_Y, 7, 5, lagen=1, spits=1.3, bank=False)
        for (zz, o) in ((zc + s * 7, s),):
            vz = G.Vlak("z", zz, o)
            G.omlijsting(W, vz, X0 - 1 + 7 + 0.5, Y0 + KA.GALM_Y, 7, 5, lagen=1, spits=1.3, bank=False)
        # hoekpinakels op de toren groter (met hogels), en pinakels midden op elke kant
        r = 7
        x0 = X0 - 1
        for (px, pz) in ((x0 - 1, zc), (x0 + r, zc - r - 1), (x0 + r, zc + r + 1), (x0 + 2 * r + 1, zc)):
            G.pinakel(W, px, y_romp + 2, pz, h=9)
        # hogels op de grote spits
        G.hogels(W, x0 + r, zc, y_romp + 1, r - 0.5, KA.SPITS_TOP - y_romp - 1, hoeken=8, elke=3, start=3)
        # een ring van kleine spitsjes om de voet van de grote spits (de "kroon")
        for k in range(8):
            a = k * math.pi / 4 + math.pi / 8
            px, pz = x0 + r + int(round(math.cos(a) * 7.3)), zc + int(round(math.sin(a) * 7.3))
            if G.lucht(W, px, y_romp + 1, pz):
                G.pinakel(W, px, y_romp + 1, pz, h=5, staaf=False)
    # ---- de Grote Poort: colonnetten met beelden op consoles in de dagkanten (de 5 archivolten blijven)
    for k in range(5):
        x = X0 - 1 + k
        hw = (22 - 2 * k) // 2
        for s in (-1, 1):
            z = s * hw
            if abs(z) < 9:
                continue      # (the Wolkwacht guards stand in the portal: wolkwacht_*)
            for y in range(Y0 + 1, Y0 + 9):
                G.zet_lucht(W, x, y, z, G.muurtje("wit_muur"))
            G.zet_lucht(W, x, Y0 + 9, z, plaat("goud_plaat"))
            G.zet_lucht(W, x, Y0 + 10, z, P["beeld"])
            G.zet_lucht(W, x, Y0 + 11, z, P["beeld_gezicht"])
            G.zet_lucht(W, x, Y0 + 12, z, G.trede("goud_trap", "north" if s > 0 else "south", "top"))
            G.zet_lucht(W, x, Y0 + 13, z, plaat("goud_plaat"))
    # ---- het grote roosvenster: een terugspringende ring eromheen
    roos_omlijsting(W, vm, 0.5, ytop + 7, 6.5, oortjes=True)
    # ---- hogels op de topgevel (de schuine randen) + pinakels op de gevelhoeken
    for k in range(0, 24, 2):
        hw = 12 - k // 2
        for s in (-1, 1):
            G.zet_lucht(W, X0 - 1, ytop + 4 + k, s * (hw + 1), G.trede("goud_trap", "north" if s > 0 else "south"))


def roos_omlijsting(W, vl, u_mid, y_mid, r, oortjes=False):
    """Twee terugspringende ringen om een roosvenster (wit, dan goud), met kleine hogels rondom. oortjes: de guh-roos heeft
    twee kleine ronde 'oortjes' schuin erboven: daar blijft de ring weg."""
    def vrij(du, dy):
        return oortjes and dy > 0.5 and abs(du) > 1.5
    for (d, r1, r2, blok) in ((1, r + 1.0, r + 2.2, "wit_glad"), (2, r + 2.2, r + 3.1, "goud")):
        for du in range(-int(r2) - 2, int(r2) + 3):
            for dy in range(-int(r2) - 2, int(r2) + 3):
                dd = math.hypot(du + 0.5, dy + 0.5)
                if r1 < dd <= r2 and not vrij(du, dy):
                    vl.zet_l(W, u_mid + du, int(y_mid + dy), d, P[blok])
    for k in range(16):
        a = k * math.pi / 8
        if vrij(math.cos(a), math.sin(a)):
            continue
        u = u_mid + math.cos(a) * (r + 3.6)
        y = y_mid + math.sin(a) * (r + 3.6)
        vl.zet_l(W, u, int(y), 2, G.trede("goud_trap", vl.naar_muur))


def flanken(W):
    X0, Y0, NB, ZB = KA.X0, KA.Y0, KA.NB, KA.ZB
    ytop = Y0 + KA.H_SCHIP
    xs = list(range(X0 + 10, KA.X_KRUIS[0] - 2, 10)) + list(range(KA.X_KRUIS[1] + 6, KA.X_KOOR - 2, 10))
    for s in (-1, 1):
        va = G.Vlak("z", s * ZB, s)                  # buitenmuur zijbeuk
        vc = G.Vlak("z", s * (NB + 2), s)            # buitenkant lichtbeuk
        for i in range(len(xs) - 1):
            xm = (xs[i] + xs[i + 1]) / 2.0
            if KA.X_KRUIS[0] - 2 <= xm <= KA.X_KRUIS[1] + 2:
                continue
            # zijbeukraam: omlijsting + maaswerk + een wimperg erboven tot over de dakrand
            G.omlijsting(W, va, xm, Y0 + 5, 5, 13, lagen=2, spits=1.2)
            G.maaswerk(W, va, xm, Y0 + 5, 5, 13, spits=1.2)
            wimperg(W, va, xm, Y0 + 5 + 13 + 6, 5, 1)
            # lichtbeukraam: omlijsting (alleen waar lucht: boven het zijbeukdak)
            G.omlijsting(W, vc, xm, Y0 + KA.H_BEUK + 6, 6, 14, lagen=1, spits=1.3, bank=False)
            G.maaswerk(W, G.Vlak("z", s * (NB + 2), s), xm, Y0 + KA.H_BEUK + 6, 6, 14, spits=1.3, d=0)
        # borstwering op de zijbeukdakrand (tussen de steunberen) + waterspuwers
        for (xa, xb) in ((X0 + 14, KA.X_KRUIS[0] - 1), (KA.X_KRUIS[1] + 1, KA.X_KOOR)):
            G.borstwering(W, va, xa, xb, Y0 + KA.H_BEUK + 1, d=2, elke=5)
        # lichtbeukpilasters tussen de ramen + borstwering langs de schipdakrand
        for x in xs:
            if KA.X_KRUIS[0] - 2 <= x <= KA.X_KRUIS[1] + 2:
                continue
            for y in range(Y0 + KA.H_BEUK + 4, ytop):
                vc.zet_l(W, x, y, 1, G.muurtje("wit_muur"))
            vc.zet_l(W, x, ytop, 1, G.trede("goud_trap", vc.naar_muur, "top"))
    # steunberen: waterspuwers, nis met beeld, en een tweede (lagere) luchtboog; hogels op de pinakels
    for s in (-1, 1):
        for x in xs:
            if KA.X_KRUIS[0] - 2 <= x <= KA.X_KRUIS[1] + 2:
                continue
            vb = G.Vlak("z", s * (ZB + 3), s)
            G.gargouille(W, vb, x, Y0 + KA.H_BEUK + 3)
            G.nis(W, vb, x, Y0 + 8, h=4)
            G.hogels(W, x, s * (ZB + 2), Y0 + KA.H_BEUK + 15, 2, 9, hoeken=4, elke=2, start=1)
            # tweede luchtboog (onder de eerste), 2 dik met een omgekeerde trap eronder
            za, zb = ZB + 1, NB + 3
            ya, yb = Y0 + KA.H_BEUK + 3, Y0 + KA.H_BEUK + 9
            for k in range(0, za - zb + 1):
                t = k / float(za - zb)
                y = int(ya + (yb - ya) * t + 3 * math.sin(math.pi * t))
                z = s * (za - k)
                if not G.lucht(W, x, y, z) and k > 0:
                    continue
                BH.zet(W, x, y, z, P["wit_glad"])
                G.zet_lucht(W, x, y - 1, z, G.trede("wit_trap", "north" if s > 0 else "south", "top"))
                G.zet_lucht(W, x, y + 1, z, plaat("goud_plaat"))
            # de bovenste luchtboog krijgt een maaswerkrand (muurtjes) erboven
            za, zb = ZB + 1, NB + 3
            ya, yb = Y0 + KA.H_BEUK + 10, Y0 + KA.H_SCHIP - 10
            for k in range(1, za - zb):
                t = k / float(za - zb)
                y = int(ya + (yb - ya) * t + 6 * math.sin(math.pi * t))
                if k % 2 == 0:
                    G.zet_lucht(W, x, y + 2, s * (za - k), G.muurtje("wit_muur"))


def wimperg(W, vl, u_mid, y_voet, breed, d):
    """Een losse wimperg (driehoekige topgevel met hogels en een pinakeltje) tegen een gevel."""
    hw = breed / 2.0 + 1
    h = int(hw) + 3
    for k in range(h):
        br = hw - k * hw / h
        for du in np.arange(-br, br + 0.01, 1.0):
            vl.zet_l(W, u_mid + du, y_voet + k, d, P["marmer_glad"])
        for sgn in (-1, 1):
            vl.zet(W, u_mid + sgn * br, y_voet + k, d, P["goud"]) if G.lucht(W, *_xyz(vl, u_mid + sgn * br, y_voet + k, d)) or True else None
            if k % 2 == 1:
                vl.zet_l(W, u_mid + sgn * (br + 1), y_voet + k, d, G.trede("goud_trap", vl.langs[1] if sgn > 0 else vl.langs[0]))
    x, z = vl.p(u_mid, d)
    G.pinakel(W, x, y_voet + h, z, h=4)


def _xyz(vl, u, y, d):
    x, z = vl.p(u, d)
    return x, y, z


def transept(W):
    Y0, NB = KA.Y0, KA.NB
    ytop = Y0 + KA.H_SCHIP
    xa, xb = KA.X_KRUIS
    T = KA.TRANSEPT_Z
    for s in (-1, 1):
        vl = G.Vlak("z", s * T, s)
        um = (xa + xb + 1) / 2.0
        # het blinde portaal (goud) krijgt een diep portaal ervoor
        _fundament(W, vl, um - 9, um + 9, 1, 3, Y0)
        G.portaal(W, vl, um, Y0 + 1, 9, 12, lagen=3, gevel=False)
        roos_omlijsting(W, vl, um, ytop - 16, 7.5)
        # traptorentjes op de gevelhoeken (achthoekig, met een slanke spits)
        for xt in (xa - 1, xb + 1):
            zt = s * (T + 1)
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    _grond(W, xt + dx, zt + dz, Y0 - 3, n=20)
            for y in range(Y0 - 2, ytop + 8):
                for dx in range(-2, 3):
                    for dz in range(-2, 3):
                        if abs(dx) + abs(dz) <= 3 and max(abs(dx), abs(dz)) <= 2:
                            BH.zet(W, xt + dx, y, zt + dz, P["marmer_glad"] if (y - Y0) % 12 else P["goud"])
            for y in range(Y0 + 8, ytop + 6, 6):   # schietgaatjes / raampjes
                G.zet_lucht(W, xt, y, zt + 3 * s, None)
                BH.zet(W, xt, y, zt + 2 * s, P["raam"])
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if abs(dx) + abs(dz) <= 4 and max(abs(dx), abs(dz)) <= 3:
                        G.zet_lucht(W, xt + dx, ytop + 8, zt + dz, P["goud"])
            G.slanke_spits(W, xt, zt, ytop + 9, 16, r0=2.4, dak="dak_roze", hoeken=8)
        # omlijsting op de ramen in de transeptmuren
        for (x, o) in ((xa, -1), (xb, 1)):
            vx = G.Vlak("x", x, o)
            for zz in (KA.ZB + 6, KA.ZB + 12):
                G.omlijsting(W, vx, s * zz + 0.5, Y0 + 8, 4, 18, lagen=1, spits=1.0)


def koor(W):
    Y0, NB = KA.Y0, KA.NB
    cx, r = KA.X_KOOR, NB + 2
    for x in range(cx, cx + r + 4):
        for z in range(-r - 3, r + 4):
            d = math.hypot(x - cx, z)
            if x >= cx and r + 2.5 < d <= r + 3.5 and abs(z) % 6 == 0:
                G.pinakel(W, x, Y0 + 41, z, h=8)


def dak(W):
    X0, Y0, NB = KA.X0, KA.Y0, KA.NB
    ytop = Y0 + KA.H_SCHIP
    nok_y = ytop + 1 + (2 * NB + 7) // 2
    # nokkam: een doorlopende rij gouden platen met om de 4 een lelietje
    for x in range(X0 + 4, KA.X_KOOR + 1):
        if KA.X_KRUIS[0] - 1 <= x <= KA.X_KRUIS[1] + 1:
            continue
        G.zet_lucht(W, x, nok_y + 1, 0, plaat("goud_plaat"))
        if x % 4 == 2:
            G.zet_lucht(W, x, nok_y + 1, 0, P["goud"])
            G.zet_lucht(W, x, nok_y + 2, 0, "minecraft:end_rod[facing=up]")
    # dakkapellen (kleine wimpergjes met een raampje) op de schipdaken
    for x in (X0 + 22, X0 + 42, KA.X_KRUIS[1] + 8):
        for s in (-1, 1):
            z0 = s * (NB + 2)
            yb = ytop + 4
            zz = z0 - s * 3
            for k in range(5):
                for du in range(-2 + k // 2, 3 - k // 2):
                    BH.zet(W, x + du, yb + k, zz, P["marmer_glad"])
                    for dd in range(1, 3):
                        G.zet_lucht(W, x + du, yb + k, zz - s * dd, P["marmer"])
                BH.zet(W, x - 2 + k // 2, yb + k, zz, P["goud"])
                BH.zet(W, x + 2 - k // 2, yb + k, zz, P["goud"])
            BH.zet(W, x, yb + 1, zz, P["raam"])
            G.pinakel(W, x, yb + 5, zz, h=3, staaf=False)
    # vier spitsjes rond de koepel (op de hoeken van de kruising)
    for x in (KA.X_KRUIS[0] + 1, KA.X_KRUIS[1] - 1):
        for z in (-(NB + 1), NB + 1):
            for y in range(ytop + 1, ytop + 10):
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        BH.zet(W, x + dx, y, z + dz, P["marmer_glad"] if (y - ytop) % 4 else P["goud"])
            G.zet_lucht(W, x, ytop + 5, z + (2 if z > 0 else -2), P["raam"])
            G.slanke_spits(W, x, z, ytop + 10, 14, r0=1.8, dak="dak_goud", hoeken=4)


def buiten(W):
    westgevel(W)
    flanken(W)
    transept(W)
    koor(W)
    dak(W)


# ============================================================================================== binnen
def binnen(W):
    X0, Y0, NB, ZB = KA.X0, KA.Y0, KA.NB, KA.ZB
    ytop = Y0 + KA.H_SCHIP
    xa_k, xb_k = KA.X_KRUIS
    xs = list(range(X0 + 10, xa_k - 2, 10)) + list(range(xb_k + 6, KA.X_KOOR - 2, 10))
    # ---- kruisribgewelven in het schip (traveeën tussen de pijlers), niet in de kruising
    grenzen = [X0 + 5] + [x for x in xs if x < xa_k] + [xa_k - 1]
    for a, b in zip(grenzen[:-1], grenzen[1:]):
        G.kruisribgewelf(W, a, b, -NB, NB, ytop - 14, 13)
    grenzen = [xb_k + 1] + [x for x in xs if x > xb_k] + [KA.X_KOOR]
    for a, b in zip(grenzen[:-1], grenzen[1:]):
        G.kruisribgewelf(W, a, b, -NB, NB, ytop - 14, 13)
    # zijbeuken (achter de torens begint de zijbeuk pas op x 32)
    for s in (-1, 1):
        z1, z2 = (NB + 3, ZB - 1) if s > 0 else (-(ZB - 1), -(NB + 3))
        gr = [X0 + 15] + [x for x in xs if X0 + 15 < x < xa_k] + [xa_k - 1]
        for a, b in zip(gr[:-1], gr[1:]):
            G.kruisribgewelf(W, a, b, z1, z2, Y0 + KA.H_BEUK - 8, 6, gordel=True)
        gr = [xb_k + 1] + [x for x in xs if x > xb_k] + [KA.X_KOOR - 1]
        for a, b in zip(gr[:-1], gr[1:]):
            G.kruisribgewelf(W, a, b, z1, z2, Y0 + KA.H_BEUK - 8, 6, gordel=True)
    # transeptarmen (x 75..91): drie traveeën dwars
    T = KA.TRANSEPT_Z
    for s in (-1, 1):
        zs = [NB + 2, NB + 12, NB + 22, T - 1]
        for a, b in zip(zs[:-1], zs[1:]):
            z1, z2 = (a, b) if s > 0 else (-b, -a)
            G.kruisribgewelf(W, xa_k + 1, xb_k - 1, z1, z2, ytop - 14, 12, gordel=False)
    # ---- de kruising open tot in de koepel (trommel + koepel van KA.transept: r 12, ry 10, trommel 5 op ytop+3)
    cx = (xa_k + xb_k) // 2
    r, ry, yb = 12, 10, ytop + 3 + 5
    for x in range(xa_k, xb_k + 1):
        for z in range(-NB, NB + 1):
            for y in range(ytop - 14, yb + ry + 1):
                dd = math.hypot(x - cx, z)
                if y < ytop + 3:
                    if dd < 10.5:
                        BH.zet(W, x, y, z, G.LUCHT)
                elif y < yb:
                    if dd < r - 1.6:
                        BH.zet(W, x, y, z, G.LUCHT)
                else:
                    if (((x - cx) / (r - 1.7)) ** 2 + (z / (r - 1.7)) ** 2 + ((y - yb) / (ry - 1.7)) ** 2) < 1:
                        BH.zet(W, x, y, z, G.LUCHT)
    # gouden ring waar de koepel begint (van binnen te zien) en vier pendentief-hoeken
    for x in range(xa_k, xb_k + 1):
        for z in range(-NB, NB + 1):
            dd = math.hypot(x - cx, z)
            if 10.5 <= dd < 11.6:
                BH.zet(W, x, ytop - 1, z, P["goud"])
    # ---- bundelpijlers: diensten tegen de pijlers, de schipdienst loopt door tot de gewelfaanzet
    for s in (-1, 1):
        for x in xs:
            z = s * (NB + 1)
            G.bundelpijler(W, x, z, Y0 + 1, Y0 + KA.H_BEUK - 8)
            for y in range(Y0 + 1, ytop - 14):
                BH.zet(W, x, y, z - s * 2, G.muurtje("wit_muur"))
            BH.zet(W, x, ytop - 14, z - s * 2, G.trede("goud_trap", "north" if s > 0 else "south", "top"))
    # ---- triforium: een arcade van smalle boogjes in de binnenlaag van de schipmuur, met een gouden lijst eronder
    for s in (-1, 1):
        zi = s * (NB + 1)
        for x in range(X0 + 5, KA.X_KOOR):
            if xa_k - 1 <= x <= xb_k + 1 or x in xs or x in [v - 1 for v in xs] or x in [v + 1 for v in xs]:
                continue
            ya, yb_ = Y0 + KA.H_BEUK, Y0 + KA.H_BEUK + 4
            if x % 3 == 0:
                for y in range(ya, yb_):
                    BH.zet(W, x, y, zi, G.muurtje("wit_muur"))
            else:
                for y in range(ya, yb_):
                    BH.zet(W, x, y, zi, G.LUCHT)
                BH.zet(W, x, yb_, zi, G.trede("wit_trap", "north" if s < 0 else "south", "top"))
                BH.zet(W, x, ya - 1, zi, P["goud"])
            G.zet_lucht(W, x, ya - 1, zi - s, G.trede("goud_trap", "south" if s > 0 else "north", "top"))
    # ---- reuzenkroonluchters: in elke travee van het schip, een gigant in de koepel, kleine in de zijbeuken
    voor = [x for x in xs if x < xa_k]
    for i, (a, b) in enumerate(zip(voor[:-1], voor[1:])):
        if i % 2 == 0:
            G.kroonluchter(W, (a + b) // 2, Y0 + 16, 0, r=5.6, lagen=3, ymax=ytop + 2)   # U-Z5BINNEN: lower (was +21)
    na = [xb_k + 1] + [x for x in xs if x > xb_k] + [KA.X_KOOR]
    G.kroonluchter(W, (na[0] + na[-1]) // 2, Y0 + 16, 0, r=4.6, lagen=3, ymax=ytop + 2)   # U-Z5BINNEN: lower (was +21)
    G.kroonluchter(W, cx, Y0 + 21, 0, r=8.5, lagen=4, ymax=yb + ry + 2)                   # U-Z5BINNEN: XL, lower (was +26)
    for s in (-1, 1):
        for a, b in zip([x for x in xs if X0 + 15 < x < xa_k][:-1], [x for x in xs if X0 + 15 < x < xa_k][1:]):
            G.kroonluchter(W, (a + b) // 2, Y0 + 12, s * 22, r=2.2, lagen=2, ymax=Y0 + KA.H_BEUK + 2)   # U-Z5BINNEN: S
    # ---- kandelaars langs het altaar en het middenpad
    for z in (-8, 8):
        for x in (cx - 8, cx + 8):
            G.kandelaar(W, x, Y0 + 1, z, h=3)
    for x in range(X0 + 12, xa_k - 4, 9):
        for z in (-3, 3):
            pass


def bouw(W):
    buiten(W)
