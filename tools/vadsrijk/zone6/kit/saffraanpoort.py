"""Saffraanpoort: de hoofdstad van het Zijdeland, een rivier-oasestad aan de Saffraanrivier.

Spelcoördinaten. Stadsellips rond SAFFRAAN (3762, -846), stralen 128 x 104; de rivier loopt er west-oost doorheen.
U-Z6WERELD (Saffraanpoort-fork): herbouwd met DIEPTE (koepelkit.py) en als echte, beloopbare kaart:
  Het Rozijnenpaleis (noordoever): terras, iwan-portaal -> de Spiegelzaal (troonzaal van Sultane Rozijntje) -> noordhof ->
      buitentrap naar het dak (paleis_dak); gevels met pilasters, nissen in twee lagen, muqarnas-kroonlijst; 4 minaretten met
      guh-oortjes, zijvleugels met erkers; de Paradijstuin met kanalen en de ghats naar de rivier.
  De Saffraanbrug (op de as van het paleis, x 3762): bogen in het water, overdekte zuilengangen op het dek, een paviljoen met koepel.
  De Grote Bazaar (zuidoever): overdekte straat, winkelnissen met toonbank en doorgang, koepeltjes met lichtgat, ronde messing
      kroonluchters, de kruising onder de gouden koepel; trap naar het dak (bazaar_dak).
  De Karavanserai (bij de Westpoort): binnenhof met arcaden in twee lagen, stal, keuken, gildebord, rijdieren.
  De Sterrenwacht van Twinkel (noordoost): toren met wenteltrap (een tunnel in de toren) naar de sterrenkoepel; brouwerij ernaast.
  Woonwijken: rijhuizen in bouwblokken met straten van 5; bovenverdiepingen kragen uit, erkers, balkons, dakkamers met koepeltjes.
  De Kanaalwijk (zuid): kanalen met kades en bruggetjes. Stadsmuur met torens en drie poorten (west, zuid, oost).
Alle plekken van plan6 worden met zijbouw.plek vastgelegd; ~10 huizen zijn te betreden (zijbouw.huis_rol).
"""
import math
import numpy as np
import zijbouw as ZB
import zijwereld as ZW
import koepelkit as KK
from zijbouw import zet, doos, leeg, vlak, raakt, rng_van, lantaarn, luik, FACE, plek
from zpalet import Z, WOL, TAPIJT, b, trap, plaat

CX, CZ = ZW.SAFFRAAN
RX, RZ = ZW.STAD_R
Y = ZW.LAAG + 2          # straatniveau (bovenkant stadsplateau = LAAG+1 -> je staat op LAAG+2)
PALEIS = (3722, -952, 3802, -886)
ZAAL = (3738, -916, 3786, -894)          # de Spiegelzaal (binnenmaten incl. muren)
TUIN = (3732, -883, 3792, -861)
BAZAAR = (3698, -819, 3828, -806)        # de overdekte straat (x1, z1, x2, z2)
KRUIS = 3762                             # x van de kruising (grote koepel) = de as paleis - brug - bazaar - Zuidpoort
BRUG_X = KRUIS
BRUG_Z = (-860, -826)
KARAVANSERAI = (3664, -812, 3694, -782)
STERRENWACHT = (3846, -906)
WESTPOORT = (3644, -806)                 # (x, z) in de muur; buiten ligt de weg op straatniveau
ZUIDPOORT = (3762, -742)
OOSTPOORT = (3881, -884)
POORTEN = {"west": (WESTPOORT, (-1, 0)), "zuid": (ZUIDPOORT, (0, 1)), "oost": (OOSTPOORT, (1, 0))}
KANAAL = (3688, -794, 3758, -752)        # de Kanaalwijk
# straten: raster van bouwblokken 19 + straat 8 (straat-x 3759..3766 ligt op de as; 8 breed, zodat de uitkragende
# bovenverdiepingen, erkers en kroonlijsten van beide kanten nog licht en lucht laten)
GX0, GZ0, BLOK, STRAAT = 6, 11, 27, 8
ML = KK.ML


def rivier_z(x):
    """z van de rivieras en de halve breedte op x (lineair tussen de punten van ZW.RIVIER)."""
    R = ZW.RIVIER
    for i in range(len(R) - 1):
        a, c = R[i], R[i + 1]
        if min(a[0], c[0]) <= x <= max(a[0], c[0]):
            t = (x - a[0]) / (c[0] - a[0])
            return a[1] + t * (c[1] - a[1]), a[2] + t * (c[2] - a[2])
    return R[-1][1], R[-1][2]


def in_stad(x, z, marge=0):
    return ((x - CX) / (RX - marge)) ** 2 + ((z - CZ) / (RZ - marge)) ** 2 < 1


def rivier_afstand(x, z):
    d, v, _ = ZW.lijn_afstand(np.array([[x + 0.5]]), np.array([[z + 0.5]]), ZW.RIVIER)
    return float(d[0, 0]), float(v[0, 0, 0])


def straat_x(x):
    return (x - GX0) % BLOK < STRAAT


def straat_z(z):
    return (z - GZ0) % BLOK < STRAAT


def in_kanaalwijk(x, z):
    return KANAAL[0] <= x <= KANAAL[2] and KANAAL[1] <= z <= KANAAL[3]


# ============================================================================================ spiegel-proxy
class Spiegel:
    """Gespiegelde blik op een Wereld in het vlak z = c (of x = c): alles wat erdoor gebouwd wordt, komt
    gespiegeld terecht (ook trappen, deuren en guhs). Zo krijgt de Spiegelzaal echte spiegelbeelden."""

    def __init__(self, W, c, as_="z"):
        self.W, self.c, self.as_ = W, c, as_

    def __getattr__(self, k):
        return getattr(self.W, k)

    def _m(self, x, z):
        return (x, 2 * self.c - z) if self.as_ == "z" else (2 * self.c - x, z)

    def _s(self, s):
        if "[" not in s:
            return s
        if self.as_ == "z":
            s = s.replace("facing=north", "facing=TMP").replace("facing=south", "facing=north").replace("facing=TMP", "facing=south")
        else:
            s = s.replace("facing=east", "facing=TMP").replace("facing=west", "facing=east").replace("facing=TMP", "facing=west")
        s = s.replace("_left", "_TMP").replace("_right", "_left").replace("_TMP", "_right")
        s = s.replace("hinge=left", "hinge=TMP").replace("hinge=right", "hinge=left").replace("hinge=TMP", "hinge=right")
        return s

    def wset(self, x, y, z, s, **kw):
        x2, z2 = self._m(x, z)
        if self.as_ == "z":
            z2 = 2 * self.c - z
        self.W.wset(x2, y, z2, self._s(s), **kw)

    def wget(self, x, y, z):
        x2, z2 = self._m(x, z)
        return self.W.wget(x2, y, z2)

    def wfill(self, x1, y1, z1, x2, y2, z2, s, **kw):
        a, c_ = self._m(x1, z1)
        b_, d = self._m(x2, z2)
        self.W.wfill(min(a, b_), y1, min(c_, d), max(a, b_), y2, max(c_, d), self._s(s), **kw)

    def raakt(self, x1, y1, z1, x2, y2, z2):
        a, c_ = self._m(x1, z1)
        b_, d = self._m(x2, z2)
        return self.W.raakt(min(a, b_), y1, min(c_, d), max(a, b_), y2, max(c_, d))

    def label(self, *a):
        pass

    def wguh(self, x, y, z, yaw=180, **kw):
        x2, z2 = self._m(x, z)
        self.W.wguh(x2, y, z2, (180 - yaw) % 360 if self.as_ == "z" else (-yaw) % 360, **kw)


# ============================================================================================ het paleis
def spiegelzaal_inhoud(W, figuren=True):
    """Alles BINNEN de Spiegelzaal (zonder de spiegelwanden): vloer, loper, zuilen, kroonluchters, troon, hofguhs.
    Wordt ook gespiegeld gebouwd (achter de spiegels)."""
    x1, z1, x2, z2 = ZAAL
    yv = Y + 3
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            k = "vloer_dambord" if (x + z) % 2 else "wit_glad"
            if abs(z - (z1 + z2) / 2) < 2.6:
                k = "mozaiek_ster" if abs(z - (z1 + z2) / 2) > 1.6 else "marmer"
            zet(W, x, yv - 1, z, k)
    zm = (z1 + z2) // 2
    for x in range(x1 + 3, x2 - 6):
        for dz in (-1, 0, 1):
            zet(W, x, yv, zm + dz, TAPIJT["magenta"] if abs(dz) == 1 else TAPIJT["roze"])
    # zuilenrij langs beide kanten (zuilen met gouden kapitelen, gebeitelde voet)
    for x in range(x1 + 4, x2 - 3, 6):
        for z in (z1 + 3, z2 - 3):
            for y in range(yv, yv + 11):
                zet(W, x, y, z, "zuil_wit")
            zet(W, x, yv + 11, z, "zuilkop")
            zet(W, x, yv, z, "zs_beitel")
            zet(W, x, yv + 12, z, "goud")
    # troon op een verhoging aan het oosteinde
    for x in range(x2 - 6, x2):
        for z in range(zm - 4, zm + 5):
            zet(W, x, yv, z, "marmer_glad")
            if x >= x2 - 4:
                zet(W, x, yv + 1, z, "marmer_glad")
    for z in range(zm - 4, zm + 5):
        zet(W, x2 - 6, yv, z, trap("marmer_glad", "east"))
        zet(W, x2 - 4, yv + 1, z, trap("marmer_glad", "east") if abs(z - zm) > 1 else "marmer_glad")
    for y in range(yv + 2, yv + 5):
        for dz in (-1, 0, 1):
            zet(W, x2 - 2, y, zm + dz, "goudblok" if y == yv + 4 else "minecraft:pink_wool")
    zet(W, x2 - 3, yv + 2, zm, "minecraft:pink_wool")
    # pauwenwaaier achter de troon (goud + turkoois)
    for a in np.linspace(-1.3, 1.3, 15):
        for r in np.arange(2, 7.5, 0.5):
            zet(W, x2 - 1, yv + 4 + r * math.cos(a), zm + r * math.sin(a),
                "tegel_turkoois" if r > 6.2 else ("goud" if int(r * 2) % 3 == 0 else "glas_regenboog"))
    # messing staande lampen naast de troon
    for dz in (-4, 4):
        zet(W, x2 - 5, yv + 1, zm + dz, ZB.muurtje("zs"))
        zet(W, x2 - 5, yv + 2, zm + dz, KK.ml(False))
    if figuren:
        W.wguh(x2 - 2.5, yv + 2, zm + 0.5, 90, skin="guh_golden")   # W-LEVEN: on the throne, facing the hall (-x; 270 = east)
        for i, x in enumerate(range(x1 + 8, x2 - 8, 7)):
            W.wguh(x + 0.5, yv, zm - 4.5, 180, skin=["guh", "guh_mint", "guh_bloesemguh", "guh_wolk"][i % 4])
            W.wguh(x + 2.5, yv, zm + 4.5, 0, skin=["guh_snow", "guh", "guh_konijntje_roze", "guh_pluisguh"][i % 4])


def spiegelzaal(W, spiegels=True, figuren=True):
    """De Spiegelzaal: wanden met spiegelbogen, parelmoermozaïek, een getrapt muqarnasgewelf, ronde messing kroonluchters en
    lichtvensters bovenin. spiegels=True (alleen de render): achter de bogen een echte gespiegelde zaal; in het spel zijn de
    bogen spiegels van wit glas voor parelmoer (niets erachter is te betreden). Deuren: zuid (het iwan) en noord (de hof)."""
    x1, z1, x2, z2 = ZAAL
    yv = Y + 3
    H = 20
    if not raakt(W, x1 - 2, yv - 4, z1 - 30, x2 + 2, yv + H + 14, z2 + 30):
        return
    doos(W, x1, yv - 1, z1, x2, yv + H, z2, "wit_glad")
    leeg(W, x1 + 1, yv, z1 + 1, x2 - 1, yv + H - 1, z2 - 1)
    zm = (z1 + z2) // 2
    spiegelzaal_inhoud(W, figuren)
    for z_w, kant in ((z1, (0, 1)), (z2, (0, -1))):
        for x in range(x1 + 1, x2):
            for y in range(yv, yv + H):
                k = "tegel_parel" if (x + y) % 3 else "filigraan"
                if y in (yv + 12, yv + 13):
                    k = "goud" if y == yv + 12 else "tegel_turkoois"
                zet(W, x, y, z_w, k)
        for x in range(x1 + 7, x2 - 3, 6):
            if z_w == z1 and abs(x - KRUIS) < 3:
                continue          # the north door
            for du in np.arange(-2, 2.01, 1.0):
                top = ZB.boog_top(du, 5, 7, 1.2)
                for y in range(yv, int(yv + top) + 1):
                    zet(W, x + du, y, z_w, "minecraft:white_stained_glass")
                    if not spiegels:
                        zet(W, x + du, y, z_w - kant[1], "tegel_parel")
            for du in (-3, 3):
                for y in range(yv, yv + 9):
                    zet(W, x + du, y, z_w, "goud")
            for du in (-1, 0, 1):
                for y in (yv + 15, yv + 16, yv + 17):
                    zet(W, x + du, y, z_w, "glas_regenboog" if du == 0 else "glas")
    for z in range(z1 + 1, z2):
        for y in range(yv, yv + H):
            zet(W, x2, y, z, "tegel_parel" if (z + y) % 2 else "filigraan")
            zet(W, x1, y, z, "tegel_parel" if (z + y) % 2 else "wit_glad")
    # gewelf: getrapt (muqarnas) naar een vlak plafond met sterrenmozaïek
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            dz = min(z - z1, z2 - z)
            if dz <= 4:
                y = yv + H - 5 + dz
                zet(W, x, y, z, trap("wit_glad", "north" if z - z1 < z2 - z else "south", half="top")
                    if (x + dz) % 2 else trap("goud", "north" if z - z1 < z2 - z else "south", half="top"))
                for yy in range(y + 1, yv + H):
                    zet(W, x, yy, z, "wit_glad")
            else:
                zet(W, x, yv + H - 1, z, "mozaiek_ster" if (x * 3 + z) % 5 else "goud")
    for x in range(x1 + 8, x2 - 4, 12):
        KK.ronde_luchter(W, x, yv + H - 1, zm, r=4.2, lengte=4, lagen=3)
    # noorddeur naar de hof
    for x in range(KRUIS - 1, KRUIS + 2):
        for y in range(yv, yv + 4):
            for dz in (0, 1, 2):
                zet(W, x, y, z1 - dz, "minecraft:air")
        for dz in (1, 2):
            zet(W, x, yv - 1, z1 - dz, "wit_glad")
    if spiegels:
        for c in (z1, z2):
            S = Spiegel(W, c)
            spiegelzaal_inhoud(S, figuren)
            doos(S, x1, yv + H - 1, z1 + 1, x2, yv + H - 1, z2 - 1, "tegel_parel")
            for x in range(x1 + 1, x2):
                for y in range(yv, yv + H - 1):
                    zet(S, x, y, z2 if c == z1 else z1, "tegel_parel" if (x + y) % 3 else "filigraan")
            for x in range(x1 + 8, x2 - 4, 12):
                KK.ronde_luchter(S, x, yv + H - 1, zm, r=4.2, lengte=4, lagen=3)
            for z in range(z1 + 1, z2):
                for y in range(yv, yv + H):
                    zet(S, x2, y, z, "tegel_parel" if (z + y) % 2 else "filigraan")
    W.label(x2 - 2, yv + 12, zm, "troon van Sultane Rozijntje, pauwenwaaier")
    W.label(x1 + 20, yv + 15, zm, "ronde messing kroonluchter")
    plek(W, "paleis_spiegelzaal", x1 + 14, yv, zm + 4)
    plek(W, "paleis_troon", x2 - 4, yv + 2, zm)
    plek(W, "paleis_audientie", x2 - 9, yv, zm + 3)
    plek(W, "paleis_grootvizier", x2 - 8, yv, zm - 4)
    plek(W, "paleis_glans", x1 + 9, yv, zm - 4)


def gevel_diepte(W, kant, c, u1, u2, y1, y2, stap=6, lagen=2, achter_boven="glas"):
    """Paleisgevel met diepte op vlak c: pilasters (1 uit) elke `stap`, daartussen spitsboognissen 2 diep in twee lagen
    (mozaïek onder, glas boven), een mozaïekband en een muqarnas-kroonlijst."""
    for u in range(int(u1), int(u2) + 1, stap):
        KK.pilaster(W, kant, c, u, y1, y2 - 4)
    h = y2 - y1
    for u in range(int(u1) + stap // 2, int(u2) - 1, stap):
        ZB.boog(W, kant, c, u + 0.5, y1 + 1, 3, max(2, int(h * 0.25)), diep=2, kader="goud", achter="mozaiek_bloem")
        if lagen > 1:
            ZB.boog(W, kant, c, u + 0.5, y1 + int(h * 0.55), 3, max(1, int(h * 0.12)), diep=2, kader="tegel_turkoois",
                    achter=achter_boven)
            KK.V(W, kant, c, 1, u, y1 + int(h * 0.55) - 1, trap("zs", FACE[KK.tegen(kant)], half="top"))
            ZB.bloembak(W, kant, c, u, y1 + int(h * 0.55) - 1) if False else None
    KK.muqarnas(W, kant, c, u1, u2, y2 - 3)


def paleis(W, figuren=True, spiegels=False):
    x1, z1, x2, z2 = PALEIS
    if not raakt(W, x1 - 10, Y - 6, z1 - 10, x2 + 10, Y + 90, z2 + 10):
        return
    yt = Y + 2                           # terrasniveau (stand)
    # terras met sierlijke keermuur (blinde bogen, gouden band)
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(Y - 2, yt):
                rand = x in (x1, x2) or z in (z1, z2)
                k = "steen_roze" if not rand else ("goud" if y == yt - 1 else ("zs_beitel" if (x + z) % 9 == 0 else "zs"))
                zet(W, x, y, z, k)
            zet(W, x, yt - 1, z, "vloer_dambord" if (x // 3 + z // 3) % 2 else "wit_glad")
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            zet(W, x, yt, z, ZB.muurtje("wit") if x % 4 else "wit_glad")
            if x % 8 == 0:
                zet(W, x, yt + 1, z, KK.ml(False))
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            zet(W, x, yt, z, ZB.muurtje("wit") if z % 4 else "wit_glad")
    # brede trap van het terras naar de tuin (op de as)
    for i in range(2):
        for x in range(CX - 12, CX + 13):
            zet(W, x, yt - 1 - i, z2 + 1 + i, trap("wit_glad", "north"))   # [u-z6afronding] 1 higher: no jump at the top
            for y in range(yt - i, yt + 3):
                zet(W, x, y, z2 + 1 + i, "minecraft:air")
    for x in range(CX - 12, CX + 13):
        zet(W, x, yt, z2, "minecraft:air")
        zet(W, x, yt - 1, z2, "wit_glad")
    for x in (CX - 13, CX + 13):
        zet(W, x, yt, z2, "zs_beitel")
        zet(W, x, yt + 1, z2, KK.ml(False))
    zx1, zz1, zx2, zz2 = ZAAL
    yv = Y + 3
    spiegelzaal(W, spiegels=spiegels, figuren=figuren)
    # buitenmantel (1 voor de zaalmuur) met diepte
    for (kant, c, u1, u2) in (((0, 1), zz2 + 2, zx1 - 1, zx2 + 1), ((0, -1), zz1 - 2, zx1 - 1, zx2 + 1)):
        for u in range(u1, u2 + 1):
            for y in range(yt, yv + 22):
                k = "muur_roze"
                if y in (yv + 15, yv + 16):
                    k = "mozaiek_ster" if (u + y) % 2 else "tegel_turkoois"
                vlak(W, kant, c, u, y, k)
                vlak(W, kant, c - kant[1], u, y, "muur_roze")
        gevel_diepte(W, kant, c, u1 + 1, u2 - 1, yt, yv + 21)
    for z in range(zz1 - 2, zz2 + 3):
        for x in (zx1 - 1, zx2 + 1):
            for y in range(yt, yv + 22):
                zet(W, x, y, z, "muur_roze" if (z - zz1) % 6 else "zs")
    ZB.dakrand(W, zx1 - 1, zz1 - 1, zx2 + 1, zz2 + 1, yv + 22, "muur_roze")
    doos(W, zx1, yv + 21, zz1, zx2, yv + 21, zz2, "steen_vanille")
    # noorddeur van de zaal naar de hof (door zaalmuur + dubbele mantel + pilaster)
    for x in range(CX - 1, CX + 2):
        for dz in (1, 2, 3):
            for y in range(yv, yv + 4):
                zet(W, x, y, zz1 - dz, "minecraft:air")
            zet(W, x, yv - 1, zz1 - dz, "wit_glad")
    for x in range(CX - 2, CX + 3):
        zet(W, x, yv + 4, zz1 - 3, "goud" if abs(x - CX) < 2 else "zs_beitel")
    KK.wandlamp(W, (0, -1), zz1 - 3, CX - 3, yv + 2)
    KK.wandlamp(W, (0, -1), zz1 - 3, CX + 3, yv + 2)
    # de minaret in de zuidoosthoek is te beklimmen: ladder in een schacht naar het eerste balkon (paleis_minaret)
    mx, mz = x2 - 4, z2 - 4
    yb = yt + int(46 * 0.55)
    # het iwan-portaal: een eigen massa voor de zuidgevel, de nis loopt tot de zaaldeur
    c_iw = zz2 + 6
    doos(W, CX - 10, yt, zz2 + 1, CX + 10, yt + 21, c_iw, "muur_roze")
    for sx_ in (-1, 1):       # twee slanke torentjes met guh-oortjes naast het iwan
        ZB.minaret(W, CX + sx_ * 12, c_iw - 2, yt, 26, r=2, wand="muur_vanille", band="mozaiek_ster", dak="koepel_tegel", balkons=1)
    ZB.iwan(W, (0, 1), c_iw, CX + 0.5, yt, 11, 12, 6, buiten="muur_roze", kader="mozaiek_ster", vak="tegel_turkoois")
    for u in (CX - 10, CX + 10):
        KK.pilaster(W, (0, 1), c_iw + 1, u, yt, yt + 26, k="zuil", voet="zs_beitel")
    for x in range(CX - 1, CX + 2):        # de zaaldeur in de nis (open boog), 4 hoog
        for dz in (0, 1, 2):
            for y in range(yt, yt + 5):
                zet(W, x, y, zz2 + dz, "minecraft:air")
            zet(W, x, yt - 1, zz2 + dz, "wit_glad")
    for dx in (-3, 3):
        KK.ronde_luchter(W, CX + dx, yt + 13, c_iw - 2, r=1.6, lengte=3, lagen=1)
    plek(W, "paleis_iwan", CX + 3, yt, c_iw)
    plek(W, "paleis_poort", CX, yt, c_iw - 3)
    # de grote gouden koepel op een hoge trommel
    ZB.koepel(W, CX + 0.5, yv + 22, (zz1 + zz2) / 2 + 0.5, 12, k_dak="koepel_goud", vorm="ui", ribben=12, rib="wit_glad",
              trommel=8, trommel_k="muur_roze", raam="glas_regenboog", oortjes=True)
    # zijvleugels: huizen met diepte (erkers, kroonlijst) en turkoois koepels
    for sx in (-1, 1):
        px = CX + sx * 33
        KK.huis2(W, px - 7, zz1 + 2, px + 7, zz2 - 2, yt, (0, 1), verd=3, seed=40 + sx, gevel="muur_roze")
        ZB.koepel(W, px + 0.5, yt + 16, (zz1 + zz2) / 2 + 0.5, 6, k_dak="koepel_tegel", vorm="ui", ribben=8,
                  trommel=3, trommel_k="muur_vanille", oortjes=True)
    # vier minaretten met guh-oortjes op de hoeken van het terras
    for (mx_, mz_) in ((x1 + 4, z1 + 4), (x2 - 4, z1 + 4), (x1 + 4, z2 - 4), (x2 - 4, z2 - 4)):
        ZB.minaret(W, mx_, mz_, yt, 46, r=3, wand="muur_vanille", band="tegel_turkoois", dak="koepel_goud", balkons=2)
    if raakt(W, mx - 5, yt, mz - 5, mx + 5, yb + 4, mz + 5):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if math.hypot(dx + 0.5, dz + 0.5) <= 1.9:
                    for y in range(yt, yb + 1):
                        zet(W, mx + dx, y, mz + dz, "muur_vanille")
        for y in range(yt, yb + 1):
            zet(W, mx, y, mz, "minecraft:ladder[facing=west,waterlogged=false]")
        zet(W, mx, yt - 1, mz, "wit_glad")
        for dd in (1, 2, 3, 4):
            for y in (yt, yt + 1, yt + 2):
                zet(W, mx - dd, y, mz, "minecraft:air")
            for y in (yb + 1, yb + 2):
                zet(W, mx - dd, y, mz, "minecraft:air")
        leeg(W, mx, yb + 1, mz, mx, yb + 2, mz)
        ZB.boog(W, (-1, 0), mx - 4, mz + 0.5, yt, 1, 2, diep=1, kader="mozaiek_ster")
        for y in (yt, yt + 1, yt + 2):
            zet(W, mx - 4, y, mz, "minecraft:air")
        plek(W, "paleis_minaret", mx - 3, yb + 1, mz)
        plek(W, "geheim_schat_2", mx - 1, yb + 1, mz)          # boven in de minaretschacht, naast de ladder
        zet(W, mx - 2, yb + 3, mz, KK.ml(True))
    # noordhof met vijver en arcaden
    hz1, hz2 = z1 + 8, zz1 - 3
    ZB.kanaal(W, CX - 14, hz1 + 3, CX + 14, hz2 - 6, yt)
    ZB.fontein(W, CX, yt, (hz1 + hz2) // 2, r=3)
    for x in range(x1 + 8, x2 - 7, 5):
        for y in range(yt, yt + 6):
            zet(W, x, y, hz1, "zuil_wit")
        zet(W, x, yt + 6, hz1, "zuilkop")
        doos(W, x, yt + 7, hz1 - 3, x + 4, yt + 7, hz1, "muur_roze")
        zet(W, x + 2, yt + 6, hz1, trap("zs", "south", half="top"))
        KK.V(W, (0, 1), hz1 - 3, 0, x + 2, yt + 2, KK.ml(False)) if False else None
    for x in range(x1 + 8, x2 - 7, 10):
        KK.ronde_luchter(W, x + 2, yt + 7, hz1 - 1, r=1.2, lengte=1, lagen=1)
    # buitentrap langs de noordgevel naar het dak (paleis_dak)
    xs = zx1 - 12
    for i in range(yv + 21 - yt + 1):
        x = xs + i
        for z in (zz1 - 3, zz1 - 4):
            for y in range(yt - 1, yt + i):
                zet(W, x, y, z, "steen_roze")
            zet(W, x, yt + i, z, trap("zs", "east"))
            leeg(W, x, yt + i + 1, z, x, yt + i + 4, z)
        zet(W, x, yt + i + 1, zz1 - 5, ZB.muurtje("zs") if i % 5 else "zs_beitel")
        zet(W, x, yt + i, zz1 - 5, "steen_roze")
        if i % 6 == 3:
            zet(W, x, yt + i + 2, zz1 - 5, KK.ml(False))
    xe = xs + yv + 21 - yt
    for x in (xe, xe + 1):
        for z in (zz1 - 1, zz1 - 2):
            leeg(W, x, yv + 22, z, x, yv + 24, z)
            zet(W, x, yv + 21, z, "steen_vanille")
    for z in (zz1 - 3, zz1 - 4):
        zet(W, xe + 1, yv + 21, z, "steen_vanille")
        leeg(W, xe + 1, yv + 22, z, xe + 1, yv + 24, z)
    plek(W, "paleis_dak", xe + 1, yv + 22, zz1 + 4)
    W.label(CX, yv + 70, (zz1 + zz2) // 2, "Het Rozijnenpaleis (Sultane Rozijntje)")
    # paradijstuin (chahar bagh): pad op de as met smalle kanaaltjes erlangs, plein rond de fontein
    tx1, tz1, tx2, tz2 = TUIN
    tm = (tz1 + tz2) // 2
    for x in range(tx1, tx2 + 1):
        for z in range(tz1, tz2 + 1):
            if ZW.is_water(W, x, z):
                continue
            pad = abs(x - CX) <= 4 or abs(z - tm) <= 1 or math.hypot(x - CX, z - tm) < 8
            zet(W, x, Y - 1, z, "vloer_zand" if pad else "gras")
            if pad and (x + z) % 5 == 0:
                zet(W, x, Y - 1, z, "mozaiek_bloem")
    for z in range(tz1 + 1, tz2 - 1):
        if abs(z - tm) > 8:
            ZB.kanaal(W, CX - 5, z, CX - 5, z, Y)
            ZB.kanaal(W, CX + 5, z, CX + 5, z, Y)
    for x in range(tx1 + 2, tx2 - 1):
        if abs(x - CX) > 8:
            ZB.kanaal(W, x, tm, x, tm, Y)
    ZB.fontein(W, CX, Y, tm, r=3, tegel="tegel_blauw")
    for (x, z) in ((tx1 + 5, tz1 + 4), (tx2 - 5, tz1 + 4), (tx1 + 5, tz2 - 4), (tx2 - 5, tz2 - 4),
                   (tx1 + 16, tz1 + 4), (tx2 - 16, tz1 + 4), (tx1 + 16, tz2 - 4), (tx2 - 16, tz2 - 4)):
        ZB.palm(W, x, Y, z, seed=x)
    for x in range(tx1 + 2, tx2 - 1, 3):
        for z in (tz1 + 1, tz2 - 1):
            if abs(x - CX) > 5:
                ZB.struik(W, x, Y, z, seed=x + z, soort="minecraft:flowering_azalea_leaves")
    for (lx, lz) in ((CX - 9, tm - 6), (CX + 9, tm - 6), (CX - 9, tm + 6), (CX + 9, tm + 6), (CX - 6, tz1 + 2), (CX + 6, tz1 + 2)):
        KK.lantaarnpaal(W, lx, Y, lz)
    # ghats: brede trappen naar de rivier (niet onder de brug)
    for x in range(tx1, tx2 + 1):
        if abs(x - BRUG_X) <= 8:
            continue
        for i in range(10):
            z = tz2 + 1 + i
            yy = Y - 1 - i
            if yy < ZW.ZEE - 1:
                break
            zet(W, x, yy, z, trap("wit_glad", "north"))
            for y in range(ZW.ZEE - 3, yy):
                zet(W, x, y, z, "wit_glad")
            leeg(W, x, yy + 1, z, x, Y + 3, z)
    # kampvuur (checkpoint) in de tuin + figuren: ruiters van de sultane op pluimguhs
    zet(W, tx2 - 3, Y, tz1 + 2, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    plek(W, "kampvuur_saffraanpoort_paleis", tx2 - 3, Y, tz1 + 4)
    plek(W, "paleis_tuin", CX + 6, Y, tm + 4)
    plek(W, "saffraanpoort_paleisplein", CX - 6, Y, tz2 - 2)
    import zijfiguren as ZF
    ZF.zet_pluimguh(W, CX - 8.5, Y, z2 + 6.5, 180, "roze", ruiter=True)
    ZF.zet_pluimguh(W, CX + 8.5, Y, z2 + 6.5, 180, "goud", ruiter=True)
    ZF.zet_bultguh(W, tx1 + 3.5, Y, tm + 3.5, 90, variant="saffraan", deken=2, ruiter=False)
    if figuren:
        rng = rng_van(CX, tm, 4)
        for k in range(10):
            W.wguh(tx1 + 3 + rng.uniform(0, tx2 - tx1 - 6), Y, tz1 + 2 + rng.uniform(0, tz2 - tz1 - 4), rng.uniform(0, 360),
                   skin=["guh", "guh_mint", "guh_bloesemguh", "guh_wolk", "guh_golden"][k % 5])


# ============================================================================================ de bazaar
WINKELS = {(1, -1): ("bazaar_smidse", "smidse"), (2, 1): ("bazaar_koopman_1", "kruiden"), (3, 1): ("bazaar_naaiatelier", "naai"),
           (4, -1): ("bazaar_knoopje", "tapijt"), (8, -1): ("bazaar_koopman_2", "potten"), (10, 1): ("bazaar_koopman_3", "lampen"),
           (5, 1): ("bazaar_kruiden", "kruiden"), (7, -1): ("bazaar_potten", "potten"), (9, 1): ("bazaar_juwelier", "juwelen")}
LEERMEESTERS = {"bazaar_smidse": "bazaar_smidse_leermeester", "bazaar_naaiatelier": "bazaar_atelier_leermeester"}


def bazaar(W, figuren=True, binnen=False):
    """Overdekte bazaarstraat: traveeën van 10 met elk een koepeltje met lichtgat, spitsbogen tussen de traveeën, winkelnissen
    aan beide kanten (toonbank met een doorgang, de koopman erachter), ronde messing kroonluchters, tapijten aan de pijlers; in het
    midden de kruising met de grote gouden koepel en de doorgangen naar de brug (noord) en de Zuidpoort (zuid)."""
    x1, z1, x2, z2 = BAZAAR
    if not raakt(W, x1 - 8, Y - 2, z1 - 10, x2 + 8, Y + 40, z2 + 10):
        return
    zm = (z1 + z2) // 2
    yv = Y
    H = 14
    winkel_d = 5
    za, zb = z1 - winkel_d, z2 + winkel_d          # buitenmaten met winkels
    doos(W, x1, yv, za - 1, x2, yv + H, zb + 1, "muur_perzik")
    leeg(W, x1 + 1, yv, z1, x2 - 1, yv + H - 1, z2)
    for x in range(x1, x2 + 1):
        for z in range(za - 1, zb + 2):
            zet(W, x, yv - 1, z, "vloer_dambord" if (x + z) % 2 else "vloer_zand")
            if z1 <= z <= z2 and abs(z - zm) <= 1 and x % 4 == 0:
                zet(W, x, yv - 1, z, "mozaiek_ster")
    nb = (x2 - x1) // 10
    for i in range(nb):
        bx = x1 + 1 + i * 10
        mid = bx + 5
        groot = abs(mid - KRUIS) < 6
        for z in range(z1, z2 + 1):
            du = z - zm
            top = ZB.boog_top(du, z2 - z1 + 1, H - 6, 1.0)
            for y in range(int(yv + top), yv + H):
                zet(W, bx, y, z, "steen_perzik" if (y + z) % 5 else "tegel_turkoois")
            zet(W, bx, int(yv + top), z, trap("zs", "east", half="top"))
        for y in range(yv, yv + H - 4):
            zet(W, bx, y, z1, "zuil_wit")
            zet(W, bx, y, z2, "zuil_wit")
        zet(W, bx, yv, z1, "zs_beitel")
        zet(W, bx, yv, z2, "zs_beitel")
        # winkelnissen aan beide kanten: toonbank bx+2..bx+7, doorgang op bx+8
        for kant, zw in (((0, 1), z1), ((0, -1), z2)):
            s = kant[1]
            if groot:
                continue
            for x in range(bx + 2, bx + 9):
                for d in range(1, winkel_d):
                    z = zw - s * d
                    for y in range(yv, yv + 6):
                        zet(W, x, y, z, "minecraft:air")
                z_achter = zw - s * winkel_d
                rng = rng_van(x, zw, i)
                for y in range(yv, yv + 6):
                    zet(W, x, y, z_achter, WOL[["roze", "turkoois", "geel", "lila", "magenta", "oranje", "blauw"][(x + y + i) % 7]]
                        if y > yv + 1 else "palm_planken")
                if x <= bx + 7:
                    zet(W, x, yv, zw - s * 1, "palm_planken")
                    zet(W, x, yv + 1, zw - s * 1, ZB.WAREN[rng.randint(len(ZB.WAREN))] if rng.rand() < 0.75 else "minecraft:air")
                if rng.rand() < 0.45 and x <= bx + 6:
                    zet(W, x, yv, zw - s * 4, ZB.WAREN[rng.randint(len(ZB.WAREN))])
                zet(W, x, yv + 6, zw, "goud")
                zet(W, x, yv + 6, zw - s, "mozaiek_bloem" if x % 2 else "goud")
            for x in (bx + 1, bx + 9):
                for y in range(yv, yv + 7):
                    zet(W, x, y, zw - s, "zs" if y != yv else "zs_beitel")
            KK.ronde_luchter(W, bx + 5, yv + 6, zw - s * 3, r=1.2, lengte=1, lagen=1, kroon=False)
            # zijden luifel boven de winkel (de straat in)
            for x in range(bx + 2, bx + 9):
                zet(W, x, yv + 5, zw + s, "luifel_zijde")
            # een bijzondere winkel?
            if (i, s) in WINKELS:
                pid, soort = WINKELS[(i, s)]
                plek(W, pid, bx + 4, yv, zw - s * 2)
                if pid in LEERMEESTERS:
                    plek(W, LEERMEESTERS[pid], bx + 6, yv, zw - s * 2)
                if soort == "juwelen":
                    for x in range(bx + 2, bx + 8):
                        zet(W, x, yv + 1, zw - s * 1, ["minecraft:gold_block", "minecraft:amethyst_block", "guhs:theepotje"][x % 3])
                if soort == "smidse":
                    zet(W, bx + 3, yv, zw - s * 3, "minecraft:anvil[facing=east]")
                    zet(W, bx + 6, yv, zw - s * 4, "minecraft:blast_furnace[facing=" + ("south" if s > 0 else "north") + ",lit=true]")
                    zet(W, bx + 7, yv, zw - s * 4, "minecraft:smithing_table")
                elif soort == "naai":
                    zet(W, bx + 3, yv, zw - s * 4, "minecraft:loom[facing=" + ("south" if s > 0 else "north") + "]")
                    zet(W, bx + 6, yv, zw - s * 4, "minecraft:loom[facing=" + ("south" if s > 0 else "north") + "]")
                    for x in range(bx + 2, bx + 8):
                        zet(W, x, yv + 1, zw - s * 1, WOL[["roze", "turkoois", "geel", "lila"][x % 4]])
                elif soort == "tapijt":
                    for x in range(bx + 2, bx + 8):
                        for y in range(yv + 2, yv + 6):
                            zet(W, x, y, zw - s * 5, "zijde" if (x + y) % 3 else WOL["turkoois"])
                    zet(W, bx + 5, yv, zw - s * 4, "minecraft:loom[facing=" + ("south" if s > 0 else "north") + "]")
        # gewelf + koepeltje met lichtgat
        r = 4.6 if not groot else 8
        cxk = mid if not groot else KRUIS
        if not groot or mid == min(range(x1 + 6, x2, 10), key=lambda m: abs(m - KRUIS)):
            doos(W, bx + 1, yv + H, z1 - 1, bx + 9, yv + H, z2 + 1, "steen_perzik")
            for dx in range(-int(r) - 1, int(r) + 2):
                for dz in range(-int(r) - 1, int(r) + 2):
                    if math.hypot(dx, dz) <= r + 0.3:
                        zet(W, cxk + dx, yv + H, zm + dz, "minecraft:air")
            kd = ["koepel_roze", "koepel_tegel"][i % 2] if not groot else "koepel_goud"
            ZB.koepel(W, cxk + 0.5, yv + H, zm + 0.5, r, k_dak=kd, vorm="rond" if not groot else "spits",
                      ribben=8, rib="goud" if not groot else "wit_glad", oortjes=groot, trommel=0 if not groot else 4,
                      trommel_k="muur_perzik", raam="glas_regenboog")
            ht = int(r * (1.0 if not groot else 1.35)) + (4 if groot else 0)
            for dy in range(ht - 2, ht + 6):
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        if abs(dx) + abs(dz) < 2 or groot:
                            zet(W, cxk + dx, yv + H + dy, zm + dz, "minecraft:air")
            # muqarnas-ring onder de koepel
            for k in range(int(r * 8)):
                a = k * 2 * math.pi / int(r * 8)
                zet(W, cxk + 0.5 + math.cos(a) * (r + 0.4), yv + H - 1, zm + 0.5 + math.sin(a) * (r + 0.4),
                    "goud" if k % 2 else "mozaiek_ster", keep=True)
            KK.ronde_luchter(W, cxk if not groot else cxk + 2, yv + H - 1, zm, r=2.4 if not groot else 4.2,
                             lengte=3 if not groot else 2, lagen=1 if not groot else 2)
        # lampionsnoer in de lengte van de travee, tapijten aan de pijlers
        for x in range(bx + 1, bx + 10):
            zet(W, x, yv + H - 4, zm - 4, ZB.ketting("x"))
            zet(W, x, yv + H - 4, zm + 4, ZB.ketting("x"))
            if x % 3 == 0:
                for dz in (-4, 4):
                    zet(W, x, yv + H - 5, zm + dz,
                        ["guhs:lampion_roze[hanging=true]", "guhs:lampion_geel[hanging=true]",
                         "guhs:lampion_mint[hanging=true]"][(x + dz) % 3])
        for zw, s_ in ((z1, 1), (z2, -1)):
            pal = [("magenta", "geel"), ("turkoois", "wit"), ("roze", "lila"), ("oranje", "geel")][i % 4]
            for dy in range(2, 9):
                zet(W, bx, yv + dy, zw + s_, WOL[pal[0]] if dy % 3 else WOL[pal[1]])
    # dak tussen de koepels: plat met borstwering
    ZB.dakrand(W, x1, za - 1, x2, zb + 1, yv + H + 1, "muur_perzik", kanteel=True)
    # buitengevels met diepte (noord naar de rivier, zuid naar de straat)
    for (kant, c) in (((0, -1), za - 1), ((0, 1), zb + 1)):
        for u in range(x1 + 3, x2 - 2, 10):
            KK.pilaster(W, kant, c, u, yv, yv + H - 2)
        for u in range(x1 + 8, x2 - 3, 10):
            if abs(u - KRUIS) > 6:
                ZB.boog(W, kant, c, u + 0.5, yv + 1, 3, 4, diep=2, kader="tegel_turkoois", achter="mozaiek_guh")
                KK.wandlamp(W, kant, c, u - 3, yv + 4)
        KK.muqarnas(W, kant, c, x1 + 1, x2 - 1, yv + H - 3)
    # ingangen: iwans aan de kopse kanten
    ZB.iwan(W, (-1, 0), x1, zm + 0.5, yv, 7, 7, 3, buiten="muur_perzik", kader="mozaiek_ster", vak="tegel_blauw")
    ZB.iwan(W, (1, 0), x2, zm + 0.5, yv, 7, 7, 3, buiten="muur_perzik", kader="mozaiek_ster", vak="tegel_blauw")
    for xx, s in ((x1 - 1, -1), (x2 + 1, 1)):
        for z in range(zm - 1, zm + 2):
            for y in range(yv, yv + 4):
                zet(W, xx - s, y, z, "minecraft:air")
                zet(W, xx - 2 * s, y, z, "minecraft:air")
    # dwarsdoorgangen bij de kruising: noord naar de brug, zuid naar de straat (Zuidpoort)
    for x in range(KRUIS - 4, KRUIS + 5):
        for z in list(range(za - 1, z1)) + list(range(z2 + 1, zb + 2)):
            for y in range(yv, yv + 8):
                zet(W, x, y, z, "minecraft:air")
            zet(W, x, yv - 1, z, "mozaiek_ster" if x == KRUIS else "vloer_zand")
    for (c, kant) in ((za - 1, (0, -1)), (zb + 1, (0, 1))):
        for u in range(KRUIS - 6, KRUIS + 7):
            for y in range(yv, yv + 11):
                if abs(u - KRUIS) == 5 or y in (yv + 8, yv + 9, yv + 10) and abs(u - KRUIS) <= 5:
                    vlak(W, kant, c + kant[1], u, y, "mozaiek_ster" if (u + y) % 2 else "goud")
                if abs(u - KRUIS) == 6:
                    vlak(W, kant, c + kant[1], u, y, "zuil")
    # trap naar het dak (zuidkant, oosteinde): bazaar_dak
    sx0 = x2 - 16
    for i in range(H + 1):
        x = sx0 + i
        for z in (zb + 2, zb + 3):
            for y in range(yv - 1, yv + i):
                zet(W, x, y, z, "zs")
            zet(W, x, yv + i, z, trap("zs", "east"))
            leeg(W, x, yv + i + 1, z, x, yv + i + 3, z)
        zet(W, x, yv + i + 1, zb + 4, ZB.muurtje("zs") if i % 4 else "zs_beitel")
        zet(W, x, yv + i, zb + 4, "zs")
    xe = sx0 + H
    for x in (xe, xe + 1):
        leeg(W, x, yv + H + 1, zb + 1, x, yv + H + 3, zb + 1)
    zet(W, xe + 1, yv + H, zb + 2, "zs")
    zet(W, xe + 1, yv + H, zb + 3, "zs")
    leeg(W, xe + 1, yv + H + 1, zb + 2, xe + 1, yv + H + 3, zb + 3)
    plek(W, "bazaar_dak", xe - 4, yv + H + 1, zb - 1)
    plek(W, "dak_1", xe - 6, yv + H + 1, zb - 2)
    plek(W, "bazaar_west", x1 + 4, yv, zm + 5)
    plek(W, "bazaar_oost", x2 - 4, yv, zm - 5)
    plek(W, "bazaar_kruising", KRUIS - 3, yv, zm + 3)
    plek(W, "kwastje_1", KRUIS + 6, yv, z1 + 1)
    plek(W, "kwastje_2", x1 + 30, yv + H + 1, za + 1)
    import zijfiguren as ZF
    ZF.zet_tapijt(W, x1 + 26.5, yv + H + 1, zb - 1.5, 90, patroon=1, fase=0.4, skin="guh_bloesemguh", hoogte=1.4)
    ZF.zet_tapijt(W, x1 + 84.5, yv + H + 1, za + 2.5, 250, patroon=2, fase=1.4, ruiter=False, hoogte=1.0)
    ZF.zet_tapijt(W, x2 - 10.5, yv + H + 1, za + 2.5, 200, patroon=3, fase=0.8, skin="guh_mint", hoogte=1.6)
    ZF.zet_bultguh(W, x1 - 6.5, yv, zm + 4.5, 70, variant="kaneel", deken=3, ruiter=False)
    if figuren:
        rng = rng_van(x1, z1, 99)
        for k in range(46):
            x = rng.uniform(x1 + 3, x2 - 3)
            z = rng.uniform(z1 + 1.6, z2 - 1.4)
            W.wguh(x, yv, z, rng.choice([90, 270, 60, 300, 0, 180]), skin=rng.choice(
                ["guh", "guh", "guh_mint", "guh_wolk", "guh_bloesemguh", "guh_snow", "guh_golden", "guh_choco",
                 "guh_konijntje_roze", "guh_pluisguh"]))
    W.label(KRUIS, yv + H + 16, zm, "De Grote Bazaar")


# ============================================================================================ brug, norias, boten
def saffraanbrug(W):
    """De Saffraanbrug op de as paleis - bazaar (Khaju-achtig): spitsbogen met pijlers in het water (laag 1), op het dek aan
    beide kanten een overdekte zuilengang met bogen (laag 2), in het midden een paviljoen met een turkoois koepeltje.
    Het dek ligt op straatniveau: geen trappen nodig."""
    x = BRUG_X
    za, zb = BRUG_Z
    if not raakt(W, x - 12, ZW.ZEE - 6, za, x + 12, Y + 25, zb):
        return
    bx1, bx2 = x - 7, x + 7
    for z in range(za, zb + 1):
        t = (z - za) % 8
        for xx in range(bx1, bx2 + 1):
            # dek
            zet(W, xx, Y - 1, z, "mozaiek_ster" if xx == x and z % 3 == 0 else "vloer_zand")
            for y in range(Y + 0, Y + 8):
                zet(W, xx, y, z, "minecraft:air")
            # onderbouw: pijlers (3 dik) en bogen ertussen
            if t in (0, 1, 2):
                for y in range(ZW.ZEE - 4, Y - 1):
                    zet(W, xx, y, z, "steen_roze" if (y // 3) % 2 else "zs")
            else:
                q = abs(t - 5) / 2.5          # 0 midden .. 1 bij de pijler
                top = Y - 4 + int(round(2.2 * q ** 1.6))
                for y in range(top, Y - 1):
                    zet(W, xx, y, z, "zs")
                zet(W, xx, top - 1, z, trap("zs", "north" if t < 5 else "south", half="top")) if q > 0.3 else None
                for y in range(ZW.ZEE - 4, top - 1):
                    if W.wget(xx, y, z).startswith("minecraft:water") or y <= ZW.ZEE:
                        pass
        # snijwaters (pijlerneuzen) up- en downstream
        if t == 1:
            for xx, s in ((bx1 - 1, -1), (bx2 + 1, 1)):
                for y in range(ZW.ZEE - 4, Y - 2):
                    zet(W, xx, y, z, "zs_beitel" if y == Y - 3 else "zs")
                zet(W, xx, Y - 2, z, trap("zs", "east" if s < 0 else "west"))
        # zuilengang op het dek (buitenste 3 rijen)
        for xx, s in ((bx1, -1), (bx2, 1)):
            if t in (0, 4):
                for y in range(Y, Y + 4):
                    zet(W, xx, y, z, "zuil" if y > Y else "zs_beitel")
                zet(W, xx, Y + 4, z, "zuilkop")
            else:
                zet(W, xx, Y, z, ZB.muurtje("zs"))
                zet(W, xx, Y + 4, z, trap("zs", "north" if t < 4 else "south", half="top"))
            for k in range(3):
                zet(W, xx - s * k, Y + 5, z, "steen_roze")
            zet(W, xx, Y + 6, z, ZB.muurtje("zs") if z % 3 else "zs_beitel")
            zet(W, xx + s, Y + 5, z, trap("zs", "east" if s > 0 else "west", half="top"))
            if t == 0:
                zet(W, xx - s * 2, Y + 4, z, KK.ml(True))
    # paviljoen in het midden
    zm = (za + zb) // 2
    for (a_, c_) in ((bx1 - 3, bx1 - 1), (bx2 + 1, bx2 + 3)):
        doos(W, a_, ZW.ZEE - 4, zm - 6, c_, Y - 2, zm + 6, "zs")
        for zz in (zm - 6, zm + 6):
            zet(W, a_ if a_ < x else c_, Y - 2, zz, "zs_beitel")
    doos(W, bx1 - 3, Y - 1, zm - 6, bx2 + 3, Y + 12, zm + 6, "muur_vanille")
    leeg(W, bx1 - 2, Y, zm - 5, bx2 + 2, Y + 11, zm + 5)
    for xx in range(bx1 - 2, bx2 + 3):
        for zz in range(zm - 5, zm + 6):
            zet(W, xx, Y - 1, zz, "mozaiek_bloem" if (xx + zz) % 2 else "tegel_parel")
    for kant, c in (((0, -1), zm - 6), ((0, 1), zm + 6)):
        ZB.boog(W, kant, c, x + 0.5, Y, 9, 6, diep=1, kader="mozaiek_ster")
    for kant, c in (((-1, 0), bx1 - 3), ((1, 0), bx2 + 3)):
        ZB.boog(W, kant, c, zm + 0.5, Y + 1, 5, 4, diep=1, kader="tegel_turkoois")
        for u in range(zm - 2, zm + 3):
            vlak(W, kant, c, u, Y, ZB.muurtje("zs"))
        KK.muqarnas(W, kant, c, zm - 6, zm + 6, Y + 9)
    for kant, c in (((0, -1), zm - 6), ((0, 1), zm + 6)):
        KK.muqarnas(W, kant, c, bx1 - 3, bx2 + 3, Y + 9)
    ZB.dakrand(W, bx1 - 3, zm - 6, bx2 + 3, zm + 6, Y + 13, "muur_vanille")
    ZB.koepel(W, x + 0.5, Y + 13, zm + 0.5, 5.5, k_dak="koepel_tegel", vorm="ui", ribben=8, trommel=2,
              trommel_k="muur_vanille", oortjes=True)
    KK.ronde_luchter(W, x, Y + 11, zm, r=3.2, lengte=2, lagen=2)
    for (xx, zz) in ((bx1 - 2, zm - 5), (bx2 + 2, zm - 5), (bx1 - 2, zm + 5), (bx2 + 2, zm + 5)):
        zet(W, xx, Y, zz, "guhs:potted_roze_hibiscus")
    plek(W, "saffraanbrug", x + 2, Y, za + 9)
    plek(W, "saffraanbrug_paviljoen", x + 4, Y, zm + 2)
    W.label(x, Y + 26, zm, "De Saffraanbrug")


def noria(W, x, z, r=7, as_="x"):
    """Waterrad (noria) aan de oever: houten rad met emmertjes, op een stenen pijler, half in het water."""
    yc = ZW.ZEE + r - 2
    for a in np.linspace(0, 2 * math.pi, 64, endpoint=False):
        for rr in (r, r - 0.6):
            px = x + (rr * math.cos(a) if as_ == "z" else 0)
            pz = z + (rr * math.cos(a) if as_ == "x" else 0)
            zet(W, px, yc + rr * math.sin(a), pz, "palm_stam" if False else "minecraft:stripped_cherry_log[axis=y]")
    for k in range(8):
        a = k * math.pi / 4
        for s in np.arange(0, r, 0.7):
            px = x + (s * math.cos(a) if as_ == "z" else 0)
            pz = z + (s * math.cos(a) if as_ == "x" else 0)
            zet(W, px, yc + s * math.sin(a), pz, "palm_planken")
        px = x + ((r + 0.8) * math.cos(a) if as_ == "z" else 0)
        pz = z + ((r + 0.8) * math.cos(a) if as_ == "x" else 0)
        zet(W, px, yc + (r + 0.8) * math.sin(a), pz, "minecraft:barrel[facing=up,open=true]")
    for d in (-1, 1):
        px, pz = (x, z + d * 2) if as_ == "z" else (x + d * 2, z)
        for y in range(ZW.ZEE - 3, yc + 1):
            zet(W, px, y, pz, "steen_roze")
        zet(W, px, yc + 1, pz, "goud")


def bootje(W, x, z, L=9, richting=(1, 0), kleur="roze"):
    fx, fz = richting
    rx, rz = -fz, fx
    y = ZW.ZEE + 1
    for l in range(-L // 2, L // 2 + 1):
        t = abs(l) / (L / 2)
        hw = 2 * (1 - t ** 2)
        for s in range(-2, 3):
            if abs(s) <= hw + 0.3:
                zet(W, x + fx * l + rx * s, y - 1, z + fz * l + rz * s, "palm_planken")
                if abs(s) >= hw - 0.7:
                    zet(W, x + fx * l + rx * s, y, z + fz * l + rz * s, "minecraft:cherry_planks")
    for h in range(1, 9):
        zet(W, x, y + h, z, "palm_stam")
    for h in range(2, 9):
        for k in range(0, 9 - h):
            zet(W, x + fx * (k - 1), y + h, z + fz * (k - 1), WOL[kleur] if (h + k) % 4 else WOL["wit"])


# ============================================================================================ karavanserai
def karavanserai(W, figuren=True):
    """Vierkant hof met arcaden in twee lagen (galerij boven, bereikbaar met een trap), stal (zuid), keuken (noord), gildebord
    (west, bij de ingang), hoektorens met koepeltjes; buitengevels met pilasters, nissen en een muqarnas-kroonlijst.
    Ingangen: het iwan aan de westkant (naar de Westpoort) en een poort in het noorden (naar de straat naar de bazaar)."""
    x1, z1, x2, z2 = KARAVANSERAI
    if not raakt(W, x1 - 8, Y, z1 - 8, x2 + 8, Y + 30, z2 + 8):
        return
    cx1, cz1, cx2, cz2 = x1 + 8, z1 + 8, x2 - 8, z2 - 8
    xm, zmid = (x1 + x2) // 2, (z1 + z2) // 2
    doos(W, x1, Y - 1, z1, x2, Y + 11, z2, "zs")
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            zet(W, x, Y - 1, z, "vloer_dambord" if (x + z) % 2 else "vloer_zand")
    leeg(W, x1 + 2, Y, z1 + 2, x2 - 2, Y + 4, z2 - 2)
    leeg(W, cx1 - 3, Y + 6, cz1 - 3, cx2 + 3, Y + 9, cz2 + 3)
    leeg(W, cx1, Y, cz1, cx2, Y + 14, cz2)
    for x in range(cx1, cx2 + 1):
        for z in range(cz1, cz2 + 1):
            zet(W, x, Y - 1, z, "mozaiek_ster" if (x + z) % 4 == 0 else ("mozaiek_bloem" if (x - z) % 4 == 0 else "vloer_zand"))
    # arcaden rond het hof (beneden en boven)
    ring = [(x, cz1 - 1, (0, -1)) for x in range(cx1 - 1, cx2 + 2)] + [(x, cz2 + 1, (0, 1)) for x in range(cx1 - 1, cx2 + 2)] + \
           [(cx1 - 1, z, (-1, 0)) for z in range(cz1, cz2 + 1)] + [(cx2 + 1, z, (1, 0)) for z in range(cz1, cz2 + 1)]
    for (x, z, kant) in ring:
        u = x if kant[0] == 0 else z
        t = (u - (cx1 - 1)) % 4 if kant[0] == 0 else (u - (cz1 - 1)) % 4
        hoek = (x in (cx1 - 1, cx2 + 1)) and (z in (cz1 - 1, cz2 + 1))
        f_tegen = FACE[kant]
        for (ya, yb_) in ((Y, Y + 4), (Y + 6, Y + 9)):
            if t == 0 or hoek:
                for y in range(ya, yb_):
                    zet(W, x, y, z, "zuil" if y > ya else "zs_beitel")
                zet(W, x, yb_ - 1, z, "zuilkop")
            else:
                zet(W, x, yb_ - 1, z, trap("zs", ("east" if t == 1 else "west") if kant[0] == 0 else ("south" if t == 1 else "north"),
                                           half="top") if t != 2 else "mozaiek_ster")
                if ya == Y + 6:
                    zet(W, x, ya, z, "palm_hek")
        zet(W, x, Y + 4, z, "zs")
        zet(W, x, Y + 5, z, "mozaiek_bloem" if (x + z) % 2 else "goud")
        zet(W, x, Y + 10, z, "goud")
    ZB.dakrand(W, cx1 - 1, cz1 - 1, cx2 + 1, cz2 + 1, Y + 11, "muur_boter")
    # galerij-vloer is er (plafond van beneden op Y+5); lampen
    for (x, z, kant) in ring[::5]:
        zet(W, x - kant[0], Y + 4, z - kant[1], KK.ml(True))
        zet(W, x - kant[0], Y + 9, z - kant[1], KK.ml(True))
    # trap naar de galerij (zuidwest, in de benedenring, oplopend naar het oosten)
    tz_ = (cz2 + 2, cz2 + 3)
    for i in range(7):
        x = cx1 + 1 + i
        for z in tz_:
            if i < 6:   # [u-z6afronding] 6 treads: the gallery floor is Y+5 (stand Y+6), 5 treads left a 1-block jump
                zet(W, x, Y + i, z, trap("zs", "east"))
                for y in range(Y, Y + i):
                    zet(W, x, y, z, "zs")
            leeg(W, x, Y + i + 1, z, x, Y + i + 3, z) if i < 6 else None
    for x in range(cx1 + 1, cx1 + 7):
        zet(W, x, Y + 6, cz2 + 4, ZB.muurtje("zs"))
    # stal (zuidring): hooi, troggen, bultguhs
    for x in range(x1 + 3, x2 - 2):
        if (x - x1) % 5 == 0:
            zet(W, x, Y, z2 - 3, "minecraft:hay_block[axis=y]")
            zet(W, x + 1, Y, z2 - 3, "minecraft:composter[level=7]")
    import zijfiguren as ZF
    ZF.zet_bultguh(W, x1 + 6.5, Y, z2 - 4.5, 90, variant="saffraan", deken=0, ruiter=False)
    ZF.zet_bultguh(W, x2 - 6.5, Y, z2 - 4.5, 270, variant="wolkwit", deken=1, ruiter=False)
    plek(W, "karavanserai_stal", xm + 2, Y, z2 - 5)
    # keuken (noordring): fornuizen, ketel, vaten
    for i, k in enumerate(["minecraft:smoker[facing=south,lit=true]", "minecraft:furnace[facing=south,lit=true]", "minecraft:cauldron",
                           "minecraft:barrel[facing=up,open=false]", "guhs:theepotje", "minecraft:barrel[facing=up,open=true]",
                           "minecraft:smoker[facing=south,lit=false]"]):
        zet(W, xm - 3 + i, Y, z1 + 2, k)
    plek(W, "karavanserai_keuken", xm, Y, z1 + 4)
    # gildebord (westring, naast de ingang): lessenaar + bord van palmhout met zijde
    for z in range(zmid - 8, zmid - 3):
        for y in range(Y + 1, Y + 4):
            zet(W, x1 + 2, y, z, "zijde" if y > Y + 1 and z not in (zmid - 8, zmid - 4) else "palm_planken")
    zet(W, x1 + 3, Y, zmid - 6, "minecraft:lectern[facing=east,has_book=true,powered=false]")
    plek(W, "karavanserai_gildebord", x1 + 5, Y, zmid - 6)
    # hof: fontein, kampvuur, rijdieren
    ZB.fontein(W, xm, Y, zmid, r=2)
    zet(W, cx1 + 1, Y, cz1 + 1, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    plek(W, "kampvuur_saffraanpoort_karavanserai", cx1 + 3, Y, cz1 + 1)
    plek(W, "karavanserai", x1 + 4, Y, zmid + 2)
    plek(W, "karavanserai_hof", xm + 5, Y, zmid + 3)
    plek(W, "kwastje_5", cx2 + 1, Y + 6, cz1 - 2)
    ZF.zet_bultguh(W, cx1 + 3.5, Y, cz2 - 1.5, 30, variant="zandroze", deken=2, ruiter=True, skin="guh_choco")
    ZF.zet_bultguh(W, cx2 - 2.5, Y, cz2 - 2.5, 300, variant="kaneel", deken=3, ruiter=False)
    ZF.zet_tapijt(W, cx2 - 2.5, Y, cz1 + 2.5, 0, patroon=3, ruiter=False, hoogte=1.0)
    ZF.zet_pluimguh(W, cx1 + 2.5, Y, zmid + 0.5, 90, "honing")
    # ingangen
    doos(W, x1 - 4, Y - 1, zmid - 6, x1 - 1, Y + 15, zmid + 6, "zs")
    ZB.iwan(W, (-1, 0), x1 - 4, zmid + 0.5, Y, 7, 7, 3, buiten="muur_boter", kader="mozaiek_ster", vak="tegel_turkoois")
    for x in range(x1 - 1, x1 + 2):
        for z in range(zmid - 1, zmid + 2):
            for y in range(Y, Y + 4):
                zet(W, x, y, z, "minecraft:air")
            zet(W, x, Y - 1, z, "vloer_zand")
    for x in range(xm - 1, xm + 2):
        for z in (z1, z1 + 1):
            for y in range(Y, Y + 4):
                zet(W, x, y, z, "minecraft:air")
    ZB.boog(W, (0, -1), z1, xm + 0.5, Y, 3, 3, diep=1, kader="mozaiek_ster")
    for x in range(xm - 1, xm + 2):
        for y in range(Y, Y + 4):
            zet(W, x, y, z1, "minecraft:air")
    # buitengevels met diepte
    for (kant, c, u1, u2) in (((0, -1), z1, x1, x2), ((0, 1), z2, x1, x2), ((1, 0), x2, z1, z2), ((-1, 0), x1, z1, z2)):
        for u in range(u1 + 3, u2 - 2, 6):
            KK.pilaster(W, kant, c, u, Y, Y + 8)
        for u in range(u1 + 6, u2 - 3, 6):
            if kant == (-1, 0) and abs(u - zmid) < 8 or kant == (0, -1) and abs(u - xm) < 4:
                continue
            ZB.boog(W, kant, c, u + 0.5, Y + 1, 1, 2, diep=2, kader="tegel_turkoois", achter="mozaiek_bloem")
            KK.raamnis(W, kant, c, u, Y + 6, rng_van(u, c), soort="trali")
        KK.muqarnas(W, kant, c, u1 + 1, u2 - 1, Y + 8)
    ZB.dakrand(W, x1, z1, x2, z2, Y + 12, "muur_boter")
    for (tx, tz) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        ZB.wachttoren(W, tx, tz, Y - 1, 16, 3, wand="muur_boter", band="mozaiek_ster", dak="koepel_tegel")
    if figuren:
        rng = rng_van(x1, z1, 7)
        for k in range(8):
            W.wguh(cx1 + 1 + rng.uniform(0, cx2 - cx1 - 2), Y, cz1 + 1 + rng.uniform(0, cz2 - cz1 - 2), rng.uniform(0, 360),
                   skin=["guh", "guh_mint", "guh_choco", "guh_wolk"][k % 4])
    W.label(xm, Y + 22, zmid, "Karavanserai")


# ============================================================================================ sterrenwacht
def sterrenwacht(W, figuren=True):
    """Lavendel toren (r 7, 30 hoog) met een sterrenkoepel. Binnen: de kamer van Twinkel, en een wenteltrap als TUNNEL door de
    massieve toren (geen val mogelijk) naar het platform onder de koepel met de koperen kijker. Ernaast de brouwerij."""
    x, z = STERRENWACHT
    if not raakt(W, x - 30, Y - 2, z - 14, x + 12, Y + 60, z + 12):
        return
    h = 30
    yt = Y + h
    ZB.wachttoren(W, x, z, Y, h, 7, wand="muur_lavendel", band="mozaiek_ster", dak=None, kantelen=False)
    binnen = []
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            if math.hypot(dx + 0.5, dz + 0.5) <= 5.6:
                binnen.append((x + dx, z + dz))
                zet(W, x + dx, Y - 1, z + dz, "mozaiek_ster" if (dx + dz) % 3 == 0 else "tegel_parel")
                for y in range(Y, Y + 5):
                    zet(W, x + dx, y, z + dz, "minecraft:air")
                for y in range(Y + 5, yt):
                    zet(W, x + dx, y, z + dz, "muur_lavendel")
    # wenteltrap: een vierkante ring (halve breedte 3) rond de kern, 1 omhoog per stap
    ring = [(x - 3 + i, z + 3) for i in range(6)] + [(x + 3, z + 3 - i) for i in range(6)] + \
           [(x + 3 - i, z - 3) for i in range(6)] + [(x - 3, z - 3 + i) for i in range(6)]
    n = h + 1
    for i in range(n):
        px, pz = ring[i % 24]
        qx, qz = ring[(i + 1) % 24]
        f = FACE[(int(np.sign(qx - px)), int(np.sign(qz - pz)))]
        if i <= h:      # [u-z6afronding] tread h too: the platform (stand yt + 1) was 1 block above the last tread
            zet(W, px, Y + i, pz, trap("zs", f) if i > 0 else "zs")
        for y in range(Y + i + 1, Y + i + 4):
            zet(W, px, y, pz, "minecraft:air")
        if i % 4 == 2 and Y + i + 2 < yt:
            ox, oz = px + (1 if px > x else -1 if px < x else 0), pz + (1 if pz > z else -1 if pz < z else 0)
            if (ox, oz) in binnen and Y + i + 2 >= Y + 5:
                zet(W, ox, Y + i + 2, oz, KK.ml(False))
    # platform onder de koepel: open boven de laatste treden
    for i in range(h - 3, h + 1):
        px, pz = ring[i % 24]
        for y in range(yt, yt + 3):
            if y > Y + i:
                zet(W, px, y, pz, "minecraft:air")
    px, pz = ring[(h + 1) % 24]           # [u-z6afronding] the landing after the last tread
    zet(W, px, yt, pz, "muur_lavendel")
    ZB.koepel(W, x + 0.5, yt + 1, z + 0.5, 7, k_dak="koepel_lavendel", vorm="rond", ribben=12, rib="goud", oortjes=True)
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            if math.hypot(dx + 0.5, dz + 0.5) <= 5.2:
                for y in range(yt + 1, yt + 6):
                    if W.wget(x + dx, y, z + dz) != "minecraft:air" and (x + dx, z + dz) in binnen:
                        zet(W, x + dx, y, z + dz, "minecraft:air")
    # de kijker (koper) door de koepel naar buiten, op een statief
    for k in range(13):
        zet(W, x - 1 + k * 0.7, yt + 2 + k * 0.6, z, "minecraft:copper_block")
    zet(W, x - 1, yt + 1, z, "palm_hek")
    KK.ronde_luchter(W, x, yt + 7, z - 2, r=1.6, lengte=1, lagen=1)
    # deur (zuid) + kamer van Twinkel
    for dx in (-1, 0, 1):
        for dz in range(4, 9):
            for y in range(Y, Y + 4):
                zet(W, x + dx, y, z + dz, "minecraft:air")
            zet(W, x + dx, Y - 1, z + dz, "mozaiek_ster")
    ZB.boog(W, (0, 1), z + 7, x + 0.5, Y, 3, 3, diep=1, kader="mozaiek_ster")
    for dx in (-1, 0, 1):
        for y in range(Y, Y + 4):
            zet(W, x + dx, y, z + 7, "minecraft:air")
            zet(W, x + dx, y, z + 8, "minecraft:air")
    KK.wandlamp(W, (0, 1), z + 7, x - 2, Y + 2)
    KK.wandlamp(W, (0, 1), z + 7, x + 2, Y + 2)
    zet(W, x - 4, Y, z - 1, "minecraft:lectern[facing=east,has_book=true,powered=false]")
    zet(W, x + 4, Y, z, "minecraft:bookshelf")
    zet(W, x + 4, Y, z + 1, "minecraft:bookshelf")
    zet(W, x - 1, Y + 4, z + 1, KK.ml(True))
    plek(W, "sterrenwacht", x, Y, z + 5)
    plek(W, "sterrenwacht_twinkel", x, Y, z)
    plek(W, "sterrenwacht_top", x + 2, yt + 1, z + 2)
    # de brouwerij ernaast (west)
    d = KK.huis2(W, x - 20, z - 4, x - 11, z + 4, Y, (0, 1), verd=2, seed=77, gevel="muur_lavendel", open_=True, rol="brouwerij",
                 hid="huis_sterrenwacht_brouwerij")
    plek(W, "sterrenwacht_brouwerij", x - 15, Y, z - 1)
    W.label(x, Y + 46, z, "Sterrenwacht van Twinkel")


# ============================================================================================ muur en poorten
def _pos(gx, gz, kant, d, u):
    dx, dz = kant
    return (gx + dx * d + (u if dx == 0 else 0), gz + dz * d + (u if dz == 0 else 0))


def poort(W, naam, gx, gz, kant):
    """Stadspoort: een poortgebouw door de muur (doorgang 5 breed, 6 hoog op straatniveau) met een iwan naar buiten, twee
    torens met koepels, lantaarns; buiten en binnen een vlak voorplein op straatniveau (y 70 = de grond)."""
    if not raakt(W, gx - 20, Y - 6, gz - 20, gx + 20, Y + 40, gz + 20):
        return
    dx, dz = kant
    # voorplein buiten (vlak, vrij) en binnen
    for d in range(-12, 15):
        for u in range(-8 if d < 3 else -7, 9 if d < 3 else 8):
            px, pz = _pos(gx, gz, kant, d, u)
            if d >= 3 or (d < -3 and in_stad(px, pz)):
                for y in range(Y - 4, Y - 1):
                    zet(W, px, y, pz, "zand")
                zet(W, px, Y - 1, pz, "vloer_zand" if abs(u) <= 2 else "zand")
                leeg(W, px, Y, pz, px, Y + 45, pz)
    # poortgebouw
    for d in range(-3, 3):
        for u in range(-6, 7):
            px, pz = _pos(gx, gz, kant, d, u)
            for y in range(Y - 4, Y + 14):
                zet(W, px, y, pz, "steen_roze" if (y - Y) % 6 else "zs")
    for d in range(-4, 4):
        for u in range(-2, 3):
            px, pz = _pos(gx, gz, kant, d, u)
            leeg(W, px, Y, pz, px, Y + 5, pz)
            zet(W, px, Y - 1, pz, "mozaiek_ster" if u == 0 and d % 2 == 0 else "vloer_zand")
    c = (gx + dx * 3) if dx else (gz + dz * 3)
    um = gz if dx else gx
    ZB.iwan(W, kant, c - (dx + dz), um + 0.5, Y, 5, 6, 2, buiten="steen_roze", kader="mozaiek_ster", vak="tegel_roze")
    for d in (-4, 3):
        for u in range(-2, 3):
            px, pz = _pos(gx, gz, kant, d, u)
            leeg(W, px, Y, pz, px, Y + 5, pz)
    for u in (-3, 3):
        px, pz = _pos(gx, gz, kant, 3, u)
        zet(W, px, Y + 3, pz, KK.ml(False)) if False else None
        KK.wandlamp(W, kant, c, um + u, Y + 3)
    # torens
    for u in (-8, 8):
        px, pz = _pos(gx, gz, kant, 0, u)
        ZB.wachttoren(W, px, pz, Y - 4, 24, 4, dak=["koepel_tegel", "koepel_goud", "koepel_roze"][len(naam) % 3])
    # vaandels van zijde
    for u in (-4, 4):
        px, pz = _pos(gx, gz, kant, 3, u)
        for y in range(Y + 7, Y + 13):
            zet(W, px, y, pz, "zijde" if y % 2 else WOL["wit"])
    px, pz = _pos(gx, gz, kant, 0, 0)
    plek(W, "saffraanpoort_" + naam + "poort", px, Y, pz)
    W.label(px, Y + 28, pz, naam.capitalize() + "poort")


def stadsmuur(W):
    n = int(2 * math.pi * math.sqrt((RX * RX + RZ * RZ) / 2.0))
    for i in range(n):
        a = i * 2 * math.pi / n
        x = CX + RX * math.cos(a)
        z = CZ + RZ * math.sin(a)
        if not raakt(W, x - 3, Y, z - 3, x + 3, Y + 20, z + 3, marge=2):
            continue
        d, rb = rivier_afstand(x, z)
        if d < rb + 4:
            continue
        if min(math.hypot(x - p[0][0], z - p[0][1]) for p in POORTEN.values()) < 10:
            continue
        yb = min(Y - 3, ZW.hoogte(W, int(x), int(z)) - 3)
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(yb, Y + 10):
                    zet(W, x + dx, y, z + dz, "steen_roze" if (y - Y) % 6 else "zs")
                zet(W, x + dx, Y + 10, z + dz, "steen_vanille")
                zet(W, x + dx, Y + 9, z + dz, "mozaiek_ster" if i % 4 == 0 else "steen_vanille")
                if i % 3 != 0:
                    zet(W, x + dx, Y + 11, z + dz, "steen_roze")
                    if i % 3 == 1:
                        zet(W, x + dx, Y + 12, z + dz, plaat("steen_vanille"))
        if i % 40 == 20:
            ZB.wachttoren(W, int(x), int(z), yb, Y + 16 - yb, 4, dak=["koepel_tegel", "koepel_roze", "koepel_goud"][(i // 40) % 3])
    for naam, ((gx, gz), kant) in POORTEN.items():
        poort(W, naam, gx, gz, kant)


# ============================================================================================ woonwijken
UITSLUIT = [
    (PALEIS[0] - 3, PALEIS[1] - 3, PALEIS[2] + 3, PALEIS[3] + 1),
    (TUIN[0] - 4, TUIN[1], TUIN[2] + 4, TUIN[3] + 12),
    (BAZAAR[0] - 10, BAZAAR[1] - 9, BAZAAR[2] + 4, BAZAAR[3] + 9),
    (BRUG_X - 12, BRUG_Z[0] - 4, BRUG_X + 12, BRUG_Z[1] + 2),
    (KARAVANSERAI[0] - 8, KARAVANSERAI[1] - 3, KARAVANSERAI[2] + 3, KARAVANSERAI[3] + 3),
    (WESTPOORT[0] - 2, -826, KARAVANSERAI[2] + 6, -813),                          # straat Westpoort -> bazaar
    (STERRENWACHT[0] - 23, STERRENWACHT[1] - 10, STERRENWACHT[0] + 10, STERRENWACHT[1] + 10),
]
for (_gx, _gz), _k in POORTEN.values():
    _ax, _az = _gx - _k[0] * 9, _gz - _k[1] * 9
    UITSLUIT.append((_ax - 11, _az - 11, _ax + 11, _az + 11))
# huizen die te betreden zijn: doelpunt -> (id, rol)
OPEN = [((3730, -780), "huis_herberg_saffraan", "herberg"), ((3790, -782), "huis_bank_saffraan", "bank"),
        ((3820, -790), "huis_winkel_lampen", "winkel_lampen"), ((3740, -760), "huis_winkel_dadels", "winkel_dadels"),
        ((3800, -762), "huis_winkel_kruiden", "winkel_kruiden"), ((3700, -905), "huis_woning_saffraan_1", "woning"),
        ((3835, -935), "huis_woning_saffraan_2", "woning"), ((3845, -800), "huis_winkel_potten", "winkel_potten"),
        ((3690, -770), "huis_woning_saffraan_3", "woning"), ((3830, -880), "huis_winkel_zijde", "winkel_zijde")]


def vrij(x1, z1, x2, z2):
    """Is deze rechthoek vrij voor een woonhuis (binnen de stad, buiten de rivier en de grote gebouwen)?"""
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2), ((x1 + x2) // 2, (z1 + z2) // 2)):
        if not in_stad(x, z, marge=7):
            return False
        d, rb = rivier_afstand(x, z)
        if d < rb + 6:
            return False
    for (a, b_, c, d) in UITSLUIT:
        if not (x2 < a or x1 > c or z2 < b_ or z1 > d):
            return False
    return True


def huizen_plan():
    """Alle huizen (deterministisch): rijhuizen per bouwblok, de noordrij kijkt naar het noorden, de zuidrij naar het zuiden."""
    out = []
    x0 = CX - RX + ((GX0 - (CX - RX)) % BLOK)
    z0 = CZ - RZ + ((GZ0 - (CZ - RZ)) % BLOK)
    for sx in range(x0 - BLOK, CX + RX, BLOK):
        for sz in range(z0 - BLOK, CZ + RZ, BLOK):
            bx, bz = sx + STRAAT, sz + STRAAT          # het bouwblok: bx..bx+18, bz..bz+18
            rng = rng_van(bx, bz, 5)
            for rij, (za, zb, kant) in enumerate(((bz, bz + 9, (0, -1)), (bz + 9, bz + 18, (0, 1)))):
                x = bx
                while x < bx + 18:
                    w = int(rng.randint(7, 11))
                    x2 = min(x + w, bx + 18)
                    if bx + 18 - x2 < 6:
                        x2 = bx + 18
                    if x2 - x >= 6 and vrij(x, za, x2, zb):
                        out.append(dict(x1=x, z1=za, x2=x2, z2=zb, kant=kant, verd=int(rng.choice([1, 2, 2, 2, 3, 3, 4])),
                                        seed=x * 7 + za))
                    x = x2
    # te betreden huizen kiezen
    gekozen = set()
    for (tx, tz), hid, rol in OPEN:
        best, bd = None, 1e9
        for i, h in enumerate(out):
            if i in gekozen or h["x2"] - h["x1"] < 7:
                continue
            dd = math.hypot((h["x1"] + h["x2"]) / 2 - tx, (h["z1"] + h["z2"]) / 2 - tz)
            if dd < bd:
                best, bd = i, dd
        if best is not None:
            gekozen.add(best)
            out[best].update(open=True, hid=hid, rol=rol, verd=max(2, out[best]["verd"]))
    return out


_HUIZEN = None


def woonwijken(W, figuren=True):
    global _HUIZEN
    if _HUIZEN is None:
        _HUIZEN = huizen_plan()
    for h in _HUIZEN:
        if not raakt(W, h["x1"] - 2, Y - 3, h["z1"] - 2, h["x2"] + 2, Y + 40, h["z2"] + 2, marge=2):
            continue
        KK.huis2(W, h["x1"], h["z1"], h["x2"], h["z2"], Y, h["kant"], verd=h["verd"], seed=h["seed"],
                 open_=h.get("open", False), rol=h.get("rol"), hid=h.get("hid"),
                 dakluik="dak_2" if h.get("hid") == "huis_winkel_potten" else None)
        if h.get("rol") == "herberg":
            plek(W, "saffraanpoort_herberg", (h["x1"] + h["x2"]) // 2 - 2, Y, (h["z1"] + h["z2"]) // 2)
            plek(W, "saffraanpoort_herberg_balie", (h["x1"] + h["x2"]) // 2 + 2, Y, (h["z1"] + h["z2"]) // 2)
        if h.get("rol") == "bank":
            plek(W, "saffraanpoort_bank", (h["x1"] + h["x2"]) // 2 - 1, Y, (h["z1"] + h["z2"]) // 2)
    # straten: bestrating, lantaarnpalen op de kruisingen, palmen op pleintjes
    x1, _, z1, x2, _, z2 = W.b
    for sx in range(int(x1) - BLOK, int(x2) + BLOK):
        if (sx - GX0) % BLOK != 0:
            continue
        for sz in range(int(z1) - BLOK, int(z2) + BLOK):
            if (sz - GZ0) % BLOK != 0:
                continue
            if not in_stad(sx + 2, sz + 2, marge=8):
                continue
            d, rb = rivier_afstand(sx + 2, sz + 2)
            if d < rb + 3:
                continue
            ok = all(not (a <= sx + 2 <= c and b_ <= sz + 2 <= dd) for (a, b_, c, dd) in UITSLUIT[:5])
            if ok and W.wget(sx, Y, sz) == "minecraft:air" and not in_kanaalwijk(sx, sz):
                KK.lantaarnpaal(W, sx, Y, sz)
                for k in range(5):
                    zet(W, sx + k, Y - 1, sz + 2, "mozaiek_ster" if k == 2 else "vloer_zand", keep=False) if False else None


def kanaalwijk(W):
    """Kanalen in de straten van de Kanaalwijk (de straat wordt kade - water 3 - kade), bruggetjes van platen met hekjes,
    lantaarns; plekken voor Kindje Pluk en de badeendjes."""
    x1, z1, x2, z2 = KANAAL
    if not raakt(W, x1 - 4, Y - 4, z1 - 4, x2 + 4, Y + 6, z2 + 4):
        return
    water = set()
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if not in_stad(x, z, marge=9):
                continue
            ex = (z - GZ0) % BLOK in (2, 3, 4, 5)          # E-W canal in the street (quays of 2)
            ns = (x - GX0) % BLOK in (2, 3, 4, 5) and x > 3720 and z < -758   # one N-S canal (x 3734..3737)
            if ex or ns:
                water.add((x, z))
    for (x, z) in water:
        for y in range(Y - 3, Y):
            zet(W, x, y, z, "minecraft:water[level=0]" if y >= Y - 2 else "tegel_turkoois")
        leeg(W, x, Y, z, x, Y + 2, z)
    for (x, z) in water:
        for (nx, nz) in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if (nx, nz) not in water and in_stad(nx, nz, marge=9) and W.wget(nx, Y, nz) == "minecraft:air":
                zet(W, nx, Y - 1, nz, "tegel_blauw" if (nx + nz) % 2 else "zs")
    # bruggetjes waar een N-Z-straat het kanaal kruist
    for (x, z) in water:
        if straat_x(x) and (z - GZ0) % BLOK in (2, 3, 4, 5) and not ((x - GX0) % BLOK in (2, 3, 4, 5) and x > 3720):
            zet(W, x, Y - 1, z, plaat("zs", "top"))
            if (x - GX0) % BLOK == 3 and (z - GZ0) % BLOK == 3 and x > 3700:
                plek(W, "kanaalwijk_brug", x, Y, z)
            if (x - GX0) % BLOK in (1, STRAAT - 2):
                zet(W, x, Y, z, "palm_hek")
    # de kade-plekken (eendjes, Pluk)
    zq = GZ0 + ((z1 - GZ0) // BLOK + 1) * BLOK + STRAAT - 1   # de zuidkade van het O-W-kanaal
    kades = [(x, zq) for x in range(x1, x2 + 1) if in_stad(x, zq, marge=9) and (x, zq) not in water and not straat_x(x)]
    kades = [k for k in kades if W.wget(k[0], Y, k[1]) == "minecraft:air"] or kades
    for i in range(10):
        if kades:
            kx, kz = kades[int(i * (len(kades) - 1) / 9)]
            plek(W, f"kanaalwijk_eend_{i + 1}", kx, Y, kz)
    if kades:
        kx, kz = kades[len(kades) // 2]
        plek(W, "kanaalwijk", kx, Y, kz)
        plek(W, "kanaalwijk_pluk", kades[len(kades) // 2 + 2][0], Y, kz)
        plek(W, "kwastje_3", kades[len(kades) // 4][0], Y, kz)


def oever_z(x, zc, kant):
    """z of the first dry stand spot on the bank (2 past the quay), going from the river axis outward (kant -1 = north)."""
    z = int(round(zc))
    for _ in range(40):
        d, rb = rivier_afstand(x, z)
        if d > rb + 2.5:
            return z
        z += kant
    return z


def rivieroevers(W):
    """Kademuren met lantaarns langs de rivier in de stad, ladders uit het water (om de ~10), norias (waterraderen) op de
    oevers en feloekjes."""
    d, v, _ = ZW.lijn_afstand(W.XX, W.ZZ, ZW.RIVIER)
    rb = v[..., 0]
    st = ((W.XX - CX) / RX) ** 2 + ((W.ZZ - CZ) / RZ) ** 2 < 1
    kade = st & (d > rb) & (d < rb + 1.3)
    cellen = set()
    for i, j in np.argwhere(kade):
        x, z = int(math.floor(W.xs[i])), int(math.floor(W.zs[j]))
        cellen.add((x, z))
        for y in range(ZW.ZEE - 2, Y):
            zet(W, x, y, z, "steen_roze" if y < Y - 2 else "zs")
        zet(W, x, Y - 1, z, "zs_beitel" if (x + z) % 11 == 0 else "steen_vanille")
        zet(W, x, Y, z, ZB.muurtje("zs") if (x + z) % 9 else "zs_beitel")
        if (x * 3 + z) % 29 == 0:
            zet(W, x, Y + 1, z, KK.ml(False))
    # ladders: om de ~10 blokken langs de kade, aan de waterkant
    for (x, z) in sorted(cellen):
        if (x + 2 * z) % 10 != 0:
            continue
        if abs(x - BRUG_X) <= 9:
            continue
        for (nx, nz) in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            dn, rn = rivier_afstand(nx, nz)
            if dn < rn - 0.2:
                f = FACE[(nx - x, nz - z)]
                for y in range(ZW.ZEE - 3, Y):
                    zet(W, nx, y, nz, f"minecraft:ladder[facing={f},waterlogged={'true' if y <= ZW.ZEE else 'false'}]")
                zet(W, x, Y, z, "minecraft:air")
                break
    for k, (nx, kant) in enumerate(((3700, -1), (3742, 1), (3836, -1), (3858, 1))):
        zc, rb_ = rivier_z(nx)
        nz = int(round(zc + kant * (rb_ - 2)))
        if raakt(W, nx - 9, ZW.ZEE - 3, nz - 3, nx + 9, ZW.ZEE + 14, nz + 3):
            noria(W, nx, nz, r=7, as_="z")
        if k < 3:
            kz = oever_z(nx + 3, zc, kant)
            plek(W, f"noria_{k + 1}", nx + 3, Y, kz)
            if k == 0:
                plek(W, "saffraanpoort_knars", nx - 3, Y, oever_z(nx - 3, zc, kant))
            if k == 1:
                plek(W, "noria_brug", nx - 3, Y, oever_z(nx - 3, zc, kant) - kant)
    for (bx, kl, dz) in ((3722, "roze", 3), (3790, "wit", -3), (3820, "turkoois", 2), (3680, "geel", -2)):
        zc, _ = rivier_z(bx)
        bz = int(round(zc + dz))
        if raakt(W, bx - 6, ZW.ZEE, bz - 6, bx + 6, ZW.ZEE + 12, bz + 6):
            bootje(W, bx, bz, L=9, richting=(1, 0), kleur=kl)


def stadsgrond(W):
    """De straten: bestrating op straatniveau in de hele stad (de huizen en gebouwen komen erbovenop)."""
    x1, _, z1, x2, _, z2 = W.b
    for x in range(int(max(x1, CX - RX)), int(min(x2, CX + RX + 1))):
        for z in range(int(max(z1, CZ - RZ)), int(min(z2, CZ + RZ + 1))):
            if not in_stad(x, z, marge=1):
                continue
            if ZW.is_water(W, x, z):
                continue
            k = "vloer_zand"
            if straat_x(x) and straat_z(z):
                k = "mozaiek_ster" if (x - GX0) % BLOK in (3, 4) and (z - GZ0) % BLOK in (3, 4) else "vloer_dambord"
            elif (straat_x(x) and (x - GX0) % BLOK in (3, 4)) or (straat_z(z) and (z - GZ0) % BLOK in (3, 4)):
                k = "zs"
            zet(W, x, Y - 1, z, k)


def bouw(W, figuren=True, spiegels=False):
    if not raakt(W, CX - RX - 20, Y - 10, CZ - RZ - 20, CX + RX + 20, Y + 120, CZ + RZ + 20, marge=0):
        return
    stadsgrond(W)
    rivieroevers(W)
    paleis(W, figuren=figuren, spiegels=spiegels)
    bazaar(W, figuren=figuren)
    saffraanbrug(W)
    karavanserai(W, figuren=figuren)
    sterrenwacht(W, figuren=figuren)
    woonwijken(W, figuren=figuren)
    kanaalwijk(W)
    stadsmuur(W)
    # Westpoort-straat: rijdieren bij de poorten, het rekruteerbord
    import zijfiguren as ZF
    for naam, ((gx, gz), kant) in POORTEN.items():
        px, pz = _pos(gx, gz, kant, -7, 4)
        ZF.zet_bultguh(W, px + 0.5, Y, pz + 0.5, {"west": 90, "zuid": 0, "oost": 270}[naam], variant="zandroze", deken=1, ruiter=False)
        px, pz = _pos(gx, gz, kant, -8, -4)
        if naam == "zuid":
            ZF.zet_tapijt(W, px + 0.5, Y, pz + 0.5, 20, patroon=0, ruiter=False, hoogte=1.0)
        else:
            ZF.zet_pluimguh(W, px + 0.5, Y, pz + 0.5, 90, "mint" if naam == "west" else "lavendel")
    wx, wz = WESTPOORT
    if raakt(W, wx, Y, wz - 20, wx + 20, Y + 6, wz):
        bx_, bz_ = wx + 10, -822
        for x in range(bx_ - 2, bx_ + 3):
            for y in range(Y, Y + 3):
                zet(W, x, y, bz_ - 1, "palm_planken" if x in (bx_ - 2, bx_ + 2) or y == Y else "zijde")
        zet(W, bx_, Y + 3, bz_ - 1, KK.ml(False))
        plek(W, "saffraanpoort_recruitbord", bx_, Y, bz_ + 1)
    plek(W, "kwastje_4", CX - 12, Y, TUIN[1] + 3)
    plek(W, "geheim_knuffel_4", TUIN[2] - 14, Y, TUIN[1] + 4)            # tussen de palmen en struiken van de Paradijstuin
    plek(W, "geheim_knuffel_6", STERRENWACHT[0] - 23, Y, STERRENWACHT[1] - 6)   # achter de brouwerij van de sterrenwacht
    plek(W, "geheim_knuffel_7", ZAAL[0] + 1, Y + 3, ZAAL[3] - 1)         # de zuidwesthoek van de Spiegelzaal, achter een zuil
