"""[U-Z5] Gotiek met diepte voor Hoogvads (ronde U: "heel vlak en saai, moet veel meer diepte krijgen").

Bouwstenen in de kit-stijl (wereldcoördinaten, palet.P-sleutels of letterlijke blokstates):
  Vlak            een gevelvlak: as 'x' (muur op x = c, u = z) of 'z' (muur op z = c, u = x), o = +1/-1 naar buiten;
                  p(u, d) -> (x, z) met d = 0 de buitenste muurlaag, d > 0 buiten, d < 0 de muur in
  pinakel         slanke pinakel (muurtje-schacht, gouden hogels, knop)
  hogels          gouden hogels (crockets) langs de ribben van een achthoekige/vierkante spits
  steunbeer       getrapte steunbeer (versprongen, met schuine trapkappen, nis met beeld, pinakel erop)
  omlijsting      terugspringende vensteromlijsting (archivolten naar buiten, colonnetten, druiplijst, vensterbank)
  maaswerk        stenen maaswerk in een spitsboogvenster (middenstijl, Y-tracering, drieblad bovenin)
  portaal         diep portaal: lagen bogen die naar binnen kleiner worden, colonnetten, timpaan, wimperg met hogels
  nis             nis met een guh-heilige (beeld), baldakijn en console
  gargouille      guh-waterspuwer die uit de muur steekt
  borstwering     open borstwering (muurtjes + platen) met pinakeltjes
  kroonluchter    reuzenkroonluchter: kettingen tot het plafond, ringen met kaarsen en lantaarns in lagen, kristallen
  kruisribgewelf  kruisribgewelf over een rechthoekige travee (gouden graat-, gordel- en scheibogen)
Alles alleen in lucht (behalve waar een functie zelf uitsnijdt), zodat deuren/trappen van bouw5 blijven.
"""
import math

import numpy as np

import palet
from palet import P, trap, plaat
import bouwhulp as BH

LUCHT = "minecraft:air"
NAAM = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
KAARS = "minecraft:white_candle[candles={n},lit=true,waterlogged=false]"
KETTING = "minecraft:iron_chain[axis=y,waterlogged=false]"
LANTAARN_H = "minecraft:lantern[hanging=true,waterlogged=false]"
LANTAARN = "minecraft:lantern[hanging=false,waterlogged=false]"
KRISTAL = "minecraft:end_rod[facing=down]"


def naam(W, x, y, z):
    return W.wget(int(x), int(y), int(z)).split("[")[0]


def lucht(W, x, y, z):
    return naam(W, x, y, z) in (LUCHT, "minecraft:cave_air")


def zet_lucht(W, x, y, z, blok):
    """Alleen in lucht zetten."""
    if blok is not None and lucht(W, x, y, z):
        BH.zet(W, x, y, z, blok)


def trede(sleutel, facing, half="bottom"):
    if sleutel in P:
        return trap(sleutel, facing, half)
    return f"{sleutel}[facing={facing},half={half},shape=straight,waterlogged=false]"


def muurtje(sleutel="wit_muur"):
    return P.get(sleutel, sleutel)


class Vlak:
    """Een gevelvlak. as_='x': de muur staat op x = c (u loopt langs z); as_='z': op z = c (u langs x). o: +1/-1 naar buiten."""

    def __init__(self, as_, c, o):
        self.as_, self.c, self.o = as_, int(c), int(o)
        self.uit_v = (o, 0) if as_ == "x" else (0, o)
        self.uit = NAAM[self.uit_v]                      # facing naar buiten
        self.naar_muur = NAAM[(-self.uit_v[0], -self.uit_v[1])]
        self.langs = ("south", "north") if as_ == "x" else ("east", "west")   # +u, -u

    def p(self, u, d):
        if self.as_ == "x":
            return self.c + self.o * d, int(math.floor(u))
        return int(math.floor(u)), self.c + self.o * d

    def zet(self, W, u, y, d, blok):
        x, z = self.p(u, d)
        BH.zet(W, x, y, z, blok)

    def zet_l(self, W, u, y, d, blok):
        x, z = self.p(u, d)
        zet_lucht(W, x, y, z, blok)

    def get(self, W, u, y, d):
        x, z = self.p(u, d)
        return naam(W, x, y, z)

    def is_lucht(self, W, u, y, d):
        x, z = self.p(u, d)
        return lucht(W, x, y, z)


# ============================================================================================== pinakels, hogels, spitsen
def pinakel(W, x, y, z, h=7, voet="wit_glad", schacht="wit_muur", hogel="goud_trap", knop="goudblok", staaf=True):
    """Slanke pinakel vanaf y: voetblok, muurtjes-schacht met hogels om en om, gouden knop en een kristallen staafje."""
    BH.zet(W, x, y, z, voet)
    for k in range(1, h):
        BH.zet(W, x, y + k, z, muurtje(schacht))
        if k >= 2 and k % 2 == 0:
            for (dx, dz) in (((1, 0), (-1, 0)) if (k // 2) % 2 else ((0, 1), (0, -1))):
                zet_lucht(W, x + dx, y + k, z + dz, trede(hogel, NAAM[(-dx, -dz)]))
    BH.zet(W, x, y + h, z, knop)
    if staaf:
        BH.zet(W, x, y + h + 1, z, "minecraft:end_rod[facing=up]")


def hogels(W, cx, cz, y0, r0, h, hoeken=8, elke=3, blok="goud_trap", start=2):
    """Gouden hogels langs de ribben van een spits (BH.spits met dezelfde r0/h): knopjes die naar buiten steken."""
    for i in range(start, int(h) - 2, elke):
        t = i / float(h)
        r = r0 * (1 - t) ** 1.15 + 0.35
        for k in range(hoeken):
            a = k * 2 * math.pi / hoeken
            dx, dz = math.cos(a), math.sin(a)
            x, z = cx + int(round(dx * (r + 0.7))), cz + int(round(dz * (r + 0.7)))
            if abs(dx) >= abs(dz):
                f = "west" if dx > 0 else "east"
            else:
                f = "north" if dz > 0 else "south"
            zet_lucht(W, x, y0 + i, z, trede(blok, f))


def slanke_spits(W, cx, cz, y0, h, r0=2.2, dak="dak_roze", hoeken=8, top="goudblok"):
    """Een slanke spits met hogels (voor torentjes en hoekpinakels)."""
    BH.spits(W, cx, cz, y0, r0, h, dak=dak, hoeken=hoeken, top=top, oortjes=False, rand="goud")
    hogels(W, cx, cz, y0, r0, h, hoeken=4 if hoeken <= 4 else 8, elke=3)
    BH.zet(W, cx, y0 + int(h) + 2, cz, "minecraft:end_rod[facing=up]")


# ============================================================================================== steunberen, nissen, waterspuwers
def nis(W, vl, u, y, h=4, beeld="beeld", kop="beeld_gezicht", diep=1):
    """Nis (1 breed, h hoog, diep in de muur) met een guh-beeldje, een console eronder en een baldakijn erboven."""
    for d in range(0, -diep, -1):
        for yy in range(y, y + h):
            vl.zet(W, u, yy, d, LUCHT)
    # beeldje (voet, lijf, hoofd met gezicht) achterin
    vl.zet(W, u, y, -diep + 1 if diep > 1 else 0, plaat("goud_plaat") if "goud_plaat" in P else LUCHT)
    vl.zet(W, u, y + 1, -diep + 1 if diep > 1 else 0, beeld)
    vl.zet(W, u, y + 2, -diep + 1 if diep > 1 else 0, kop)
    # baldakijn: omgekeerde gouden trap buiten boven de nis + een spitsje
    vl.zet_l(W, u, y + h, 1, trede("goud_trap", vl.naar_muur, "top"))
    vl.zet_l(W, u, y + h + 1, 1, trede("wit_trap", vl.naar_muur))
    vl.zet_l(W, u, y - 1, 1, trede("wit_trap", vl.naar_muur, "top"))


def gargouille(W, vl, u, y):
    """Guh-waterspuwer: steekt 2 uit de muur, kijkt naar buiten (guhgezicht), onderkant een omgekeerde trap."""
    vl.zet_l(W, u, y, 1, P["wit_glad"])
    vl.zet_l(W, u, y, 2, P.get("marmer_gezicht", "wit_glad"))
    vl.zet_l(W, u, y - 1, 1, trede("wit_trap", vl.naar_muur, "top"))
    vl.zet_l(W, u, y + 1, 2, trede("goud_trap", vl.naar_muur))
    for s in (-1, 1):   # oortjes
        vl.zet_l(W, u + s, y + 1, 2, plaat("goud_plaat"))


def steunbeer(W, vl, u, y1, y2, diep=3, breed=1, blok="wit_glad", pinakel_h=8, nis_y=None, beeld=True, gargouille_y=None):
    """Getrapte steunbeer tegen een gevel: van diep (onder) tot 1 (boven) versprongen, elke sprong een schuine kap
    (trappen), een nis met beeld, en bovenop een pinakel met hogels. breed = halve breedte (1 -> 3 breed)."""
    n = max(1, diep)
    hoogte = y2 - y1
    for k in range(n):
        dd = n - k                                  # deze sectie steekt dd uit
        ya = y1 + int(hoogte * k / n)
        yb = y1 + int(hoogte * (k + 1) / n) - 1
        for d in range(1, dd + 1):
            for du in range(-breed, breed + 1):
                for y in range(ya, yb + 1):
                    vl.zet_l(W, u + du, y, d, blok)
        # schuine kap op de sprong
        for du in range(-breed, breed + 1):
            vl.zet_l(W, u + du, yb + 1, dd, trede("wit_trap", vl.naar_muur))
            if k < n - 1:
                pass
        # gouden band
        for du in range(-breed, breed + 1):
            vl.zet(W, u + du, ya, dd, P["goud"] if k > 0 else blok)
    if nis_y is not None and beeld:
        dd = n
        x, z = vl.p(u, dd)
        # nis in de voorkant van de onderste sectie
        vl2 = Vlak(vl.as_, vl.c + vl.o * dd, vl.o)
        nis(W, vl2, u, nis_y, h=4)
    if gargouille_y is not None:
        k = min(n - 1, max(0, int((gargouille_y - y1) * n / max(1, hoogte))))
        vl2 = Vlak(vl.as_, vl.c + vl.o * (n - k), vl.o)
        gargouille(W, vl2, u, gargouille_y)
    if pinakel_h:
        x, z = vl.p(u, 1)
        top = y2
        while not lucht(W, x, top, z) and top < y2 + 6:
            top += 1
        pinakel(W, x, top, z, h=pinakel_h)


# ============================================================================================== ramen en portalen
def omlijsting(W, vl, u_mid, y1, breed, h_recht, lagen=2, spits=1.2, kl=("wit_glad", "goud"), colonnet="wit_muur",
               bank=True, druip=True, hoog_min=None):
    """Terugspringende omlijsting om een bestaand venster (glas in het muurvlak d=0): laag k (1..lagen) staat k naar buiten en
    is k breder; colonnetten (muurtjes) langs de dagkanten, de boog afwisselend wit/goud, een vensterbank (plaat) en een
    druiplijst (omgekeerde trap) boven de buitenste boog."""
    hw = breed / 2.0
    for k in range(1, lagen + 1):
        br = breed + 2 * k
        kleur = kl[(k - 1) % len(kl)]
        hwk = br / 2.0
        for du in np.arange(-hwk, hwk + 0.01, 1.0):
            u = u_mid + du
            if abs(du) < hw + k - 1 + 0.01:
                # boogrand (de lijn van deze laag)
                top = BH.boog_vorm(min(abs(du), hwk - 0.5), br, h_recht, spits)
                yt = int(y1 + top) + 1
                vl.zet_l(W, u, yt, k, P[kleur] if kleur in P else kleur)
            else:
                # dagkant (colonnet) van onder tot de aanzet
                for y in range(int(y1), int(y1 + h_recht) + 1):
                    vl.zet_l(W, u, y, k, muurtje(colonnet))
                vl.zet_l(W, u, int(y1 + h_recht) + 1, k, P["goud"])      # kapiteeltje
        if bank:
            for du in np.arange(-hwk + 0.5, hwk, 1.0):
                vl.zet_l(W, u_mid + du, int(y1) - 1, k, plaat("wit_plaat", "top"))
    if druip:
        br = breed + 2 * lagen + 2
        for du in np.arange(-br / 2.0, br / 2.0 + 0.01, 1.0):
            top = BH.boog_vorm(min(abs(du), br / 2.0 - 0.5), br, h_recht, spits)
            vl.zet_l(W, u_mid + du, int(y1 + top) + 2, lagen, trede("goud_trap", vl.naar_muur, "top"))


def maaswerk(W, vl, u_mid, y1, breed, h_recht, spits=1.2, steen="wit_muur", d=0):
    """Stenen maaswerk in het glas van een spitsboogvenster (vlak d): middenstijl, een Y-splitsing en een drieblad bovenin.
    Alleen waar nu glas zit."""
    hw = breed / 2.0

    def zet(u, y):
        x, z = vl.p(u, d)
        n = naam(W, x, y, z)
        if "glas" in n or "glass" in n:
            BH.zet(W, x, y, z, muurtje(steen))
    top_mid = BH.boog_vorm(0, breed, h_recht, spits)
    if breed >= 3:
        for y in range(int(y1), int(y1 + h_recht)):
            zet(u_mid, y)
    # Y-splitsing: van de aanzet schuin naar de dagkanten
    for k in range(int(hw) + 1):
        zet(u_mid - k, int(y1 + h_recht + k * 0.9))
        zet(u_mid + k, int(y1 + h_recht + k * 0.9))
    # drieblad (een rondje) in de boogpunt
    if breed >= 5:
        yc = y1 + (h_recht + top_mid) / 2.0 + 1.5
        for (du, dy) in ((0, 1), (-1, 0), (1, 0), (0, -1)):
            zet(u_mid + du, int(yc + dy))


def portaal(W, vl, u_mid, y1, breed, h_recht, lagen=4, spits=1.1, gevel=True, timpaan=True, kl=("wit_glad", "goud", "marmer_glad", "goud"),
            pinakels=True):
    """Diep portaal VOOR een muur met een deuropening (breed x h_recht, al open) in het vlak: een blok van `lagen` diep, met
    trapsgewijs naar binnen kleiner wordende spitsbogen (archivolten afwisselend wit/goud, colonnetten op elke sprong),
    beeldjes tussen de colonnetten, en erboven een wimperg (driehoekige topgevel) met gouden hogels en een pinakel; links
    en rechts een pinakel. Het pad door de opening blijft vrij (vloer eronder wordt aangevuld)."""
    hw = breed / 2.0
    hw_buiten = hw + lagen + 1
    top_buiten = BH.boog_vorm(0, 2 * (hw + lagen), h_recht + lagen, spits) + 1
    for d in range(1, lagen + 1):
        hwd = hw + d - 0.5        # halve breedte van de opening op diepte d
        hd = h_recht + d          # recht stuk van de boog op diepte d
        for du in np.arange(-hw_buiten, hw_buiten + 0.01, 1.0):
            u = u_mid + du
            x, z = vl.p(u, d)
            # vloer
            if lucht(W, x, y1 - 1, z):
                BH.zet(W, x, y1 - 1, z, P["wit_glad"])
            for y in range(int(y1), int(y1 + top_buiten) + 2):
                dy = y - y1
                if abs(du) < hwd:
                    top = BH.boog_vorm(du, 2 * hwd, hd, spits)
                    if dy <= top:
                        continue          # de opening
                    if dy <= top + 1:
                        blok = kl[(d - 1) % len(kl)]
                        BH.zet(W, x, y, z, P[blok] if blok in P else blok)
                        continue
                # massief / colonnet op de sprong
                if abs(abs(du) - hwd) < 0.6 and dy <= hd:
                    blok = muurtje("wit_muur")
                    if dy == hd:
                        blok = P["goud"]
                    BH.zet(W, x, y, z, blok)
                else:
                    if lucht(W, x, y, z) or True:
                        BH.zet(W, x, y, z, P["marmer"] if (d % 2 or abs(du) > hw_buiten - 1.5) else P["marmer_glad"])
    yb = int(y1 + top_buiten) + 1
    if pinakels:
        for s in (-1, 1):
            x, z = vl.p(u_mid + s * hw_buiten, lagen)
            pinakel(W, x, yb - 2, z, h=8)
    # wimperg: driehoek op de buitenste laag (d = lagen), van de top van de boog omhoog
    if gevel:
        hg = int(hw_buiten) + 3
        for k in range(hg):
            br = hw_buiten - k * (hw_buiten / hg)
            for du in np.arange(-br, br + 0.01, 1.0):
                vl.zet(W, u_mid + du, yb + k, lagen, P["marmer"])
            # hogels op de schuine randen
            for s in (-1, 1):
                if k % 2 == 0:
                    vl.zet_l(W, u_mid + s * (br + 1), yb + k, lagen, trede("goud_trap", vl.langs[1] if s > 0 else vl.langs[0]))
                vl.zet(W, u_mid + s * br, yb + k, lagen, P["goud"])
        # een rond venstertje (drieblad) in de wimperg
        BH.rond_raam(W, vl.as_, vl.c + vl.o * lagen if vl.o < 0 else vl.c + vl.o * lagen, u_mid + 0.5, yb + hg / 3.0, 2.2,
                     glas="glas_roos", diep=1, kader="goud", spaken=6, ringen=())
        x, z = vl.p(u_mid, lagen)
        pinakel(W, x, yb + hg, z, h=6)
    return int(y1 + top_buiten)


# ============================================================================================== borstwering
def borstwering(W, vl, u1, u2, y, d=1, elke=4, pinakels=True, blok="wit_muur"):
    """Open borstwering (muurtjes met platen erop) langs een dakrand, om de `elke` een pinakeltje."""
    for u in range(int(min(u1, u2)), int(max(u1, u2)) + 1):
        x, z = vl.p(u, d)
        if not lucht(W, x, y, z):
            continue
        if lucht(W, x, y - 1, z):
            BH.zet(W, x, y - 1, z, trede("wit_trap", vl.naar_muur, "top"))
        if (u - u1) % elke == 0:
            if pinakels:
                pinakel(W, x, y, z, h=4, staaf=False)
            else:
                BH.zet(W, x, y, z, P["wit_glad"])
        else:
            BH.zet(W, x, y, z, muurtje(blok))
            zet_lucht(W, x, y + 1, z, plaat("goud_plaat"))


# ============================================================================================== kroonluchters
def plafond(W, x, y, z, ymax):
    """Eerste niet-lucht boven (x, y, z), tot ymax."""
    yy = y
    while yy < ymax and lucht(W, x, yy, z):
        yy += 1
    return yy


def kroonluchter(W, x, y, z, r=4.0, lagen=3, ymax=330, kristallen=True, hoofdketting=True, ringketting=True):
    """[U-Z5BINNEN] a ROUND hanging chandelier of the new light blocks (binnen2.kroonluchter), its size by r (S < 3.5 <= M < 5
    <= L < 8 <= XL); y = the height of its main ring. (The slab-ring version read as wooden scaffolding: kroonluchter_oud.)"""
    import binnen2
    binnen2.kroonluchter(W, x, y, z, binnen2.maat_voor_r(r), ymax=ymax)


def kroonluchter_oud(W, x, y, z, r=4.0, lagen=3, ymax=330, kristallen=True, hoofdketting=True, ringketting=True):
    """Reuzenkroonluchter met de bovenste ring op hoogte y: een hoofdketting tot het plafond, kettingen van de ring omhoog,
    `lagen` ringen (gouden platen) die naar onder kleiner worden, kaarsen op de ringen, hangende lantaarns eronder en
    kristallen staafjes; onderaan een gouden knop met een lantaarn."""
    y = int(y)
    if hoofdketting:
        top = plafond(W, x, y + 1, z, ymax)
        for yy in range(y + 1, top):
            BH.zet(W, x, yy, z, KETTING)
    rr_l = []
    for i in range(lagen):
        rr = r * (1 - i / (lagen + 0.6))
        yy = y - i * (5 if r >= 5 else 3)
        rr_l.append((rr, yy))
        for dx in range(-int(rr) - 1, int(rr) + 2):
            for dz in range(-int(rr) - 1, int(rr) + 2):
                dd = math.hypot(dx, dz)
                if rr - 0.6 < dd <= rr + 0.45:
                    BH.zet(W, x + dx, yy, z + dz, plaat("goud_plaat"))
                    if (dx + dz) % 2 == 0:
                        BH.zet(W, x + dx, yy + 1, z + dz, KAARS.format(n=1 + (abs(dx * 3 + dz) % 3)))
                    elif (dx * 7 + dz * 3) % 4 == 1:
                        zet_lucht(W, x + dx, yy - 1, z + dz, KRISTAL if (kristallen and i == lagen - 1) else LANTAARN_H)
        # (no spokes: from below a ring of slabs with spokes reads as a plank floor) the centre knot on the middle chain
        BH.zet(W, x, yy, z, "goudblok")
        # kettingen naar de ring erboven (of het plafond voor de bovenste)
        if ringketting:
            for (dx, dz) in ((int(rr), 0), (-int(rr), 0), (0, int(rr)), (0, -int(rr))):
                if i == 0:
                    top = plafond(W, x + dx, yy + 2, z + dz, ymax) if hoofdketting else yy + 4
                    if top - yy > 40:
                        top = yy + 6
                    for k in range(yy + 2, top):
                        zet_lucht(W, x + dx, k, z + dz, KETTING)
                else:
                    # hangs from the ring above: a chain up to its level, then a short gold arm out to it
                    stap = 5 if r >= 5 else 3
                    for k in range(yy + 1, yy + stap):
                        zet_lucht(W, x + dx, k, z + dz, KETTING)
                    r_boven = int(rr_l[i - 1][0])
                    ex, ez = (1 if dx > 0 else -1 if dx < 0 else 0), (1 if dz > 0 else -1 if dz < 0 else 0)
                    for k in range(int(rr), r_boven + 1):
                        zet_lucht(W, x + ex * k, yy + stap, z + ez * k, plaat("goud_plaat"))
    yb = rr_l[-1][1]
    for yy in range(yb - 1, rr_l[0][1]):
        if lucht(W, x, yy, z):
            BH.zet(W, x, yy, z, KETTING)
    BH.zet(W, x, yb - 1, z, "goudblok")
    BH.zet(W, x, yb - 2, z, LANTAARN_H)
    if kristallen:
        zet_lucht(W, x, yb - 3, z, KRISTAL)


def kandelaar(W, x, y, z, h=3):
    """Staande kandelaar: muurtje-schacht, gouden schaal, 4 kaarsen."""
    for k in range(h):
        BH.zet(W, x, y + k, z, muurtje("wit_muur") if k else P["goud"])
    BH.zet(W, x, y + h, z, KAARS.format(n=4))


# ============================================================================================== gewelven
def kruisribgewelf(W, x1, x2, z1, z2, y_aanzet, pijl, rib="goud", vul="marmer_tegels", rug="marmer", gordel=True):
    """Kruisribgewelf over de travee x1..x2, z1..z2 (lucht eronder): het snijpunt van twee spitstongewelven (langs en dwars),
    met gouden graatribben (de diagonalen), gordelbogen (dwars, aan de uiteinden) en een scheitelrib (langs het midden).
    pijl = hoogte van de kruin boven y_aanzet. Zet ook een rug (1 laag) erboven en maakt de ruimte eronder leeg."""
    cx, cz = (x1 + x2) / 2.0, (z1 + z2) / 2.0
    hx, hz = (x2 - x1 + 1) / 2.0, (z2 - z1 + 1) / 2.0

    def boog(q):
        q = min(1.0, abs(q))
        return pijl * (1 - q ** 1.7) ** 0.6
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            qx = (x + 0.5 - cx) / hx
            qz = (z + 0.5 - cz) / hz
            h = min(boog(qx), boog(qz))
            y = int(y_aanzet + h)
            diag = abs(abs(qx) - abs(qz)) < 0.11
            scheitel = abs(qz) < 0.5 / hz or abs(qx) < 0.5 / hx
            eind = gordel and (x in (x1, x2))
            blok = rib if (diag or eind or (scheitel and h > pijl * 0.85)) else vul
            for yy in range(y + 1, int(y_aanzet + pijl) + 3):
                if lucht(W, x, yy, z) or True:
                    BH.zet(W, x, yy, z, P[rug] if rug in P else rug)
            BH.zet(W, x, y, z, P[blok] if blok in P else blok)
            for yy in range(int(y_aanzet) - 1, y):
                BH.zet(W, x, yy, z, LUCHT)
            if diag and h < pijl * 0.97:
                # ribben steken een halve laag uit: omgekeerde plaat eronder
                BH.zet(W, x, y - 1, z, plaat("goud_plaat", "top"))


def bundelpijler(W, x, z, y1, y2, kern=1, schacht="wit_muur", kop="goud"):
    """Diensten (slanke schachten, muurtjes) tegen de 4 kanten van een vierkante pijler (halve breedte kern), van y1 tot y2,
    met een gouden kapiteel (omgekeerde trappen) bovenaan."""
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        px, pz = x + dx * (kern + 1), z + dz * (kern + 1)
        for y in range(y1, y2):
            zet_lucht(W, px, y, pz, muurtje(schacht))
        f = NAAM[(-dx, -dz)]
        zet_lucht(W, px, y2, pz, trede("goud_trap", f, "top"))
        zet_lucht(W, px, y1, pz, P["wit_glad"]) if False else None
