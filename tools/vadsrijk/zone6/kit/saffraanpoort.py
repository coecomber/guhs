"""Saffraanpoort: de hoofdstad van het Zijdeland, een rivier-oasestad aan de Saffraanrivier.

Spelcoördinaten. Stadsellips rond SAFFRAAN (3762, -846), stralen 128 x 104; de rivier loopt er west-oost doorheen.
Wijken:
  Het Rozijnenpaleis (noordoever, 3722-3802 x -950..-866): terras, Spiegelzaal onder de gouden koepel, 4 minaretten
      met guh-oortjes, iwan naar de rivier, hof met vijver (noord), paradijstuin met kanalen (zuid, naar de rivier)
  De Grote Bazaar (zuidoever, z -812): overdekte straat met een rij koepeltjes met lichtgaten, kruising met grote koepel
  De Saffraanbrug (x 3790): een brug met twee lagen bogen en een paviljoen (Khaju-achtig)
  De Kanaalwijk (zuidwest): kanaaltjes van de rivier naar het zuiden, huizen met trappen naar het water
  De Karavanserai (bij de Westpoort, 3676, -796): binnenhof met arcaden, bultguhs, tapijten
  De Sterrenwacht van Twinkel (noordoost, 3846, -906): toren met sterrenkoepel
  Woonwijken met dakterrassen, windtorens en koepeltjes; norias (waterraderen) en bootjes op de rivier; stadsmuur.
"""
import math
import numpy as np
import zijbouw as ZB
import zijwereld as ZW
from zijbouw import zet, doos, leeg, vlak, raakt, rng_van, lantaarn, luik, FACE
from zpalet import Z, WOL, TAPIJT, b, trap, plaat

CX, CZ = ZW.SAFFRAAN
RX, RZ = ZW.STAD_R
Y = ZW.LAAG + 2          # straatniveau (bovenkant stadsplateau = LAAG+1 -> je staat op LAAG+2)
PALEIS = (3722, -952, 3802, -886)
ZAAL = (3738, -916, 3786, -894)          # de Spiegelzaal (binnenmaten incl. muren)
TUIN = (3732, -883, 3792, -861)
BAZAAR = (3698, -819, 3828, -806)        # de overdekte straat (x1, z1, x2, z2)
KRUIS = 3762                             # x van de kruising (grote koepel)
BRUG_X = 3810
KARAVANSERAI = (3664, -812, 3694, -782)
STERRENWACHT = (3846, -906)
WESTPOORT = (3640, -806)
ZUIDPOORT = (3758, -742)


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
    # vloer: dambord met een sterrenpatroon en een lange loper
    for x in range(x1 + 1, x2):
        for z in range(z1 + 1, z2):
            k = "vloer_dambord" if (x + z) % 2 else "wit_glad"
            if abs(z - (z1 + z2) / 2) < 2.6:
                k = "tegel_hart" if abs(z - (z1 + z2) / 2) > 1.6 else "marmer"
            zet(W, x, yv - 1, z, k)
    zm = (z1 + z2) // 2
    for x in range(x1 + 3, x2 - 6):
        for dz in (-1, 0, 1):
            zet(W, x, yv, zm + dz, TAPIJT["magenta"] if abs(dz) == 1 else TAPIJT["roze"])
    # zuilenrij langs beide kanten (dubbele zuilen met gouden kapitelen)
    for x in range(x1 + 4, x2 - 3, 6):
        for z in (z1 + 3, z2 - 3):
            for y in range(yv, yv + 11):
                zet(W, x, y, z, "zuil_wit")
            zet(W, x, yv + 11, z, "zuilkop")
            zet(W, x, yv - 0, z, "goud")
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
    if figuren:
        W.wguh(x2 - 2.5, yv + 2, zm + 0.5, 270, skin="guh_golden")
        for i, x in enumerate(range(x1 + 8, x2 - 8, 7)):
            W.wguh(x + 0.5, yv, zm - 4.5, 180, skin=["guh", "guh_mint", "guh_bloesemguh", "guh_wolk"][i % 4])
            W.wguh(x + 2.5, yv, zm + 4.5, 0, skin=["guh_snow", "guh", "guh_konijntje_roze", "guh_pluisguh"][i % 4])


def spiegelzaal(W, spiegels=True, figuren=True):
    """De Spiegelzaal: wanden met spiegelbogen (doorkijk naar een gespiegelde zaal), spiegelmozaïek van
    parelmoertegels en goud, een getrapt muqarnasgewelf, kroonluchters en lichtvensters bovenin."""
    x1, z1, x2, z2 = ZAAL
    yv = Y + 3
    H = 20
    if not raakt(W, x1 - 2, yv - 4, z1 - 30, x2 + 2, yv + H + 14, z2 + 30):
        return
    doos(W, x1, yv - 1, z1, x2, yv + H, z2, "wit_glad")
    leeg(W, x1 + 1, yv, z1 + 1, x2 - 1, yv + H - 1, z2 - 1)
    zm = (z1 + z2) // 2
    spiegelzaal_inhoud(W, figuren)
    # wanden: spiegelbogen tussen de zuilen, daarboven mozaïek en lichtvensters
    for z_w, kant in ((z1, (0, 1)), (z2, (0, -1))):
        for x in range(x1 + 1, x2):
            for y in range(yv, yv + H):
                k = "tegel_parel" if (x + y) % 3 else "filigraan"
                if y in (yv + 12, yv + 13):
                    k = "goud" if y == yv + 12 else "tegel_turkoois"
                zet(W, x, y, z_w, k)
        for x in range(x1 + 7, x2 - 3, 6):
            # spiegelboog: opening (lucht) met helder glas -> je kijkt in de gespiegelde zaal
            for du in np.arange(-2, 2.01, 1.0):
                top = ZB.boog_top(du, 5, 7, 1.2)
                for y in range(yv, int(yv + top) + 1):
                    zet(W, x + du, y, z_w, "minecraft:white_stained_glass" if spiegels else "tegel_parel")
            for du in (-3, 3):
                for y in range(yv, yv + 9):
                    zet(W, x + du, y, z_w, "goud")
            # bovenvenster (licht)
            for du in (-1, 0, 1):
                for y in (yv + 15, yv + 16, yv + 17):
                    zet(W, x + du, y, z_w, "glas_regenboog" if du == 0 else "glas")
    # oostwand achter de troon: grote spiegelmozaïek-nis; westwand: de deur
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
                zet(W, x, yv + H - 1, z, "tegel_parel" if (x * 3 + z) % 5 else "goud")
    # kroonluchters
    for x in range(x1 + 8, x2 - 4, 12):
        ZB.kroonluchter(W, x, yv + H - 1, zm, r=4, ringen=2, lengte=5)
    if spiegels:
        for c in (z1, z2):
            S = Spiegel(W, c)
            spiegelzaal_inhoud(S, figuren)
            # gespiegelde wand aan de overkant + gewelf (eenvoudig)
            doos(S, x1, yv + H - 1, z1 + 1, x2, yv + H - 1, z2 - 1, "tegel_parel")
            for x in range(x1 + 1, x2):
                for y in range(yv, yv + H - 1):
                    zet(S, x, y, z2 if c == z1 else z1, "tegel_parel" if (x + y) % 3 else "filigraan")
            for x in range(x1 + 8, x2 - 4, 12):
                ZB.kroonluchter(S, x, yv + H - 1, zm, r=4, ringen=2, lengte=5)
            for z in range(z1 + 1, z2):
                for y in range(yv, yv + H):
                    zet(S, x2, y, z, "tegel_parel" if (z + y) % 2 else "filigraan")
    W.label(x2 - 2, yv + 12, zm, "troon van Sultane Rozijntje, pauwenwaaier")
    W.label(x1 + 13, yv + 7, z1, "spiegelboog (doorkijk naar de gespiegelde zaal)")
    W.label(x1 + 20, yv + 15, zm, "kroonluchter")
    W.label(x1 + 30, yv + 19, z2, "muqarnasgewelf + lichtvensters")


def paleis(W, figuren=True, spiegels=False):
    x1, z1, x2, z2 = PALEIS
    if not raakt(W, x1 - 10, Y - 6, z1 - 10, x2 + 10, Y + 90, z2 + 10):
        return
    yt = Y + 2                           # terrasniveau
    # terras met sierlijke keermuur (blinde bogen, gouden band)
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(Y - 2, yt):
                rand = x in (x1, x2) or z in (z1, z2)
                k = "steen_roze" if not rand else ("goud" if y == yt - 1 else ("wit_glad" if (x + z) % 9 == 0 else "steen_roze"))
                zet(W, x, y, z, k)
            zet(W, x, yt - 1, z, "vloer_dambord" if (x // 3 + z // 3) % 2 else "wit_glad")
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            zet(W, x, yt, z, ZB.muurtje("wit") if x % 4 else "wit_glad")
            if x % 8 == 0:
                zet(W, x, yt + 1, z, lantaarn(False))
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            zet(W, x, yt, z, ZB.muurtje("wit") if z % 4 else "wit_glad")
    # trap van het terras naar de tuin
    for i in range(3):
        for x in range(CX - 6, CX + 7):
            zet(W, x, yt - 1 - i, z2 + 1 + i, trap("wit_glad", "north"))
    for x in range(CX - 6, CX + 7):
        zet(W, x, yt, z2, "minecraft:air")
    # de zaal (buitenkant) met iwan naar het zuiden en een gouden koepel
    zx1, zz1, zx2, zz2 = ZAAL
    yv = Y + 3
    if spiegels or True:
        spiegelzaal(W, spiegels=spiegels, figuren=figuren)
    # buitenmantel: pilasters + spitsboognissen + tegelband
    for (kant, c, u1, u2) in (((0, 1), zz2, zx1, zx2), ((0, -1), zz1, zx1, zx2)):
        for u in range(u1, u2 + 1):
            for y in range(yt, yv + 22):
                k = "muur_roze" if (u - u1) % 6 else "wit_glad"
                if y in (yv + 19, yv + 20):
                    k = "tegel_turkoois" if (u + y) % 2 else "tegel_blauw"
                if y == yv + 21:
                    k = "goud"
                vlak(W, kant, c + kant[1], u, y, k)
        for u in range(u1 + 3, u2 - 2, 6):
            ZB.boog(W, kant, c + kant[1], u + 0.5, yt + 1, 3, 6, diep=1, kader="goud", achter="tegel_blauw")
            ZB.boog(W, kant, c + kant[1], u + 0.5, yt + 12, 3, 3, diep=1, kader="goud", achter="glas")
    for z in range(zz1, zz2 + 1):
        for x in (zx1 - 1, zx2 + 1):
            for y in range(yt, yv + 22):
                zet(W, x, y, z, "muur_roze" if (z - zz1) % 6 else "wit_glad")
    ZB.dakrand(W, zx1 - 1, zz1 - 1, zx2 + 1, zz2 + 1, yv + 22, "muur_roze")
    doos(W, zx1, yv + 21, zz1, zx2, yv + 21, zz2, "steen_vanille")
    ZB.iwan(W, (0, 1), zz2 + 1, CX + 0.5, yt, 11, 12, 5, buiten="muur_roze", kader="tegel_turkoois", vak="tegel_blauw")
    # de grote gouden koepel op een hoge trommel
    ZB.koepel(W, CX + 0.5, yv + 22, (zz1 + zz2) / 2 + 0.5, 12, k_dak="koepel_goud", vorm="ui", ribben=12, rib="wit_glad",
              trommel=8, trommel_k="muur_roze", raam="glas_regenboog", oortjes=True)
    # twee kleinere koepels op de zijvleugels
    for sx in (-1, 1):
        px = CX + sx * 33
        ZB.huis(W, px - 7, zz1 + 2, px + 7, zz2 - 2, yt, verd=3, seed=40 + sx, gevel="muur_roze", dakk=False,
                voor=(0, 1), dakterras=False)
        ZB.koepel(W, px + 0.5, yt + 16, (zz1 + zz2) / 2 + 0.5, 6, k_dak="koepel_turkoois", vorm="ui", ribben=8,
                  trommel=3, trommel_k="muur_vanille", oortjes=True)
    # vier minaretten met guh-oortjes op de hoeken van het terras
    for (mx, mz) in ((x1 + 4, z1 + 4), (x2 - 4, z1 + 4), (x1 + 4, z2 - 4), (x2 - 4, z2 - 4)):
        ZB.minaret(W, mx, mz, yt, 46, r=3, wand="muur_vanille", band="tegel_turkoois", dak="koepel_goud", balkons=2)
    # noordhof met vijver en arcaden
    hz1, hz2 = z1 + 8, zz1 - 3
    ZB.kanaal(W, CX - 14, hz1 + 3, CX + 14, hz2 - 3, yt)
    ZB.fontein(W, CX, yt, (hz1 + hz2) // 2, r=3)
    for x in range(x1 + 8, x2 - 7, 5):
        for z in (hz1,):
            for y in range(yt, yt + 6):
                zet(W, x, y, z, "zuil_wit")
            zet(W, x, yt + 6, z, "zuilkop")
        doos(W, x, yt + 7, hz1 - 2, x + 4, yt + 7, hz1, "muur_roze")
    W.label(CX, yv + 70, (zz1 + zz2) // 2, "Het Rozijnenpaleis (Sultane Rozijntje)")
    # paradijstuin (chahar bagh) tussen terras en rivier
    tx1, tz1, tx2, tz2 = TUIN
    for x in range(tx1, tx2 + 1):
        for z in range(tz1, tz2 + 1):
            if ZW.is_water(W, x, z):
                continue
            zet(W, x, Y - 1, z, "gras" if (abs(x - CX) > 2 and abs(z - (tz1 + tz2) // 2) > 2) else "vloer_zand")
    ZB.kanaal(W, CX - 1, tz1 + 1, CX + 1, tz2 - 2, Y)
    ZB.kanaal(W, tx1 + 2, (tz1 + tz2) // 2 - 1, tx2 - 2, (tz1 + tz2) // 2 + 1, Y)
    ZB.fontein(W, CX, Y, (tz1 + tz2) // 2, r=3, tegel="tegel_blauw")
    for (x, z) in ((tx1 + 5, tz1 + 4), (tx2 - 5, tz1 + 4), (tx1 + 5, tz2 - 4), (tx2 - 5, tz2 - 4),
                   (tx1 + 16, tz1 + 4), (tx2 - 16, tz1 + 4), (tx1 + 16, tz2 - 4), (tx2 - 16, tz2 - 4)):
        ZB.palm(W, x, Y, z, seed=x)
    for x in range(tx1 + 2, tx2 - 1, 3):
        for z in (tz1 + 1, tz2 - 1):
            ZB.struik(W, x, Y, z, seed=x + z, soort="minecraft:flowering_azalea_leaves")
    # ghats: trappen naar de rivier
    for x in range(tx1, tx2 + 1):
        for i in range(5):
            z = tz2 + 1 + i
            if ZW.is_water(W, x, z) and i > 1:
                break
            zet(W, x, Y - 1 - i, z, trap("wit_glad", "north"))
            leeg(W, x, Y - i, z, x, Y + 3, z)


# ============================================================================================ de bazaar
def bazaar(W, figuren=True, binnen=False):
    """Overdekte bazaarstraat: traveeën van 10 met elk een koepeltje met lichtgat, spitsbogen tussen de traveeën,
    winkelnissen aan beide kanten met waren, hangende lampen en tapijten; in het midden de kruising met de grote
    koepel. Buiten: een rij roze/turkoois koepeltjes op het dak."""
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
    nb = (x2 - x1) // 10
    for i in range(nb):
        bx = x1 + 1 + i * 10
        mid = bx + 5
        groot = abs(mid - KRUIS) < 6
        # spitsboog tussen de traveeën (dwarsboog)
        for z in range(z1, z2 + 1):
            du = z - zm
            top = ZB.boog_top(du, z2 - z1 + 1, H - 6, 1.0)
            for y in range(int(yv + top), yv + H):
                zet(W, bx, y, z, "steen_perzik" if (y + z) % 5 else "tegel_turkoois")
        for y in range(yv, yv + H - 4):
            zet(W, bx, y, z1, "zuil_wit")
            zet(W, bx, y, z2, "zuil_wit")
        # winkelnissen aan beide kanten
        for kant, zw in (((0, 1), z1), ((0, -1), z2)):
            s = kant[1]
            for x in range(bx + 2, bx + 9):
                for d in range(1, winkel_d):
                    z = zw - s * d
                    for y in range(yv, yv + 6):
                        zet(W, x, y, z, "minecraft:air")
                z_achter = zw - s * winkel_d
                rng = rng_van(x, zw, i)
                for y in range(yv, yv + 6):
                    zet(W, x, y, z_achter, WOL[["roze", "turkoois", "geel", "lila", "magenta", "oranje", "blauw"][(x + y + i) % 7]]
                        if y > yv + 1 else "hout")
                zet(W, x, yv, zw - s * 1, "hout")
                zet(W, x, yv + 1, zw - s * 1, ZB.WAREN[rng.randint(len(ZB.WAREN))] if rng.rand() < 0.85 else "minecraft:air")
                if rng.rand() < 0.5:
                    zet(W, x, yv, zw - s * 3, ZB.WAREN[rng.randint(len(ZB.WAREN))])
                zet(W, x, yv + 6, zw - s * 0, "goud")
            for x in (bx + 1, bx + 9):
                for y in range(yv, yv + 7):
                    zet(W, x, y, zw - s, "steen_vanille")
            ZB.hanglamp(W, bx + 5, yv + 7, zw - s * 2, lengte=2)
            # luifeltje boven de winkel (de straat in)
            for x in range(bx + 2, bx + 9):
                zet(W, x, yv + 6, zw + s * 0, WOL[["roze", "wit"][x % 2]] if i % 2 else WOL[["turkoois", "wit"][x % 2]])
        # gewelf + koepeltje met lichtgat
        r = 4.6 if not groot else 8
        cxk = mid if not groot else KRUIS
        if not groot or mid == min(range(x1 + 6, x2, 10), key=lambda m: abs(m - KRUIS)):
            doos(W, bx + 1, yv + H, z1 - 1, bx + 9, yv + H, z2 + 1, "steen_perzik")
            for dx in range(-int(r) - 1, int(r) + 2):
                for dz in range(-int(r) - 1, int(r) + 2):
                    if math.hypot(dx, dz) <= r + 0.3:
                        zet(W, cxk + dx, yv + H, zm + dz, "minecraft:air")
            kd = ["koepel_roze", "koepel_turkoois"][i % 2] if not groot else "koepel_goud"
            ZB.koepel(W, cxk + 0.5, yv + H, zm + 0.5, r, k_dak=kd, vorm="rond" if not groot else "spits",
                      ribben=8, rib="goud" if not groot else "wit_glad", oortjes=groot, trommel=0 if not groot else 4,
                      trommel_k="muur_perzik", raam="glas_regenboog")
            # het lichtgat (oculus) bovenin: lucht in de top van de koepel
            ht = int(r * (1.0 if not groot else 1.35)) + (4 if groot else 0)
            for dy in range(ht - 2, ht + 6):
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        if abs(dx) + abs(dz) < 2 or groot:
                            zet(W, cxk + dx, yv + H + dy, zm + dz, "minecraft:air")
            zet(W, cxk, yv + H + ht + 3, zm, "minecraft:air")
            # hangende lantaarns onder de koepel, tapijten aan de pijlers
            for dz in (-2, 0, 2):
                ZB.hanglamp(W, cxk + (1 if dz == 0 else -1), yv + H - 1, zm + dz, lengte=3 + abs(dz) // 2,
                            soort="minecraft:lantern")
        # lampionsnoer in de lengte van de travee, gekleurde lampions aan kettingen, tapijten aan de pijlers
        for x in range(bx + 1, bx + 10):
            zet(W, x, yv + H - 4, zm - 3, ZB.ketting("x"))
            zet(W, x, yv + H - 4, zm + 3, ZB.ketting("x"))
            if x % 3 == 0:
                for dz in (-3, 3):
                    zet(W, x, yv + H - 5, zm + dz,
                        ["guhs:lampion_roze[hanging=true]", "guhs:lampion_geel[hanging=true]",
                         "guhs:lampion_mint[hanging=true]"][(x + dz) % 3])
        for zw, s_ in ((z1, 1), (z2, -1)):
            pal = [("magenta", "geel"), ("turkoois", "wit"), ("roze", "lila"), ("oranje", "geel")][i % 4]
            for dy in range(2, 9):
                for dx in (-1, 0, 1):
                    k = WOL[pal[0]] if (dy + abs(dx)) % 3 else WOL[pal[1]]
                    zet(W, bx + dx, yv + dy, zw + s_, k if abs(dx) < 2 else k)
            for dx in (-1, 0, 1):
                zet(W, bx + dx, yv + 1, zw + s_, TAPIJT[pal[1]])
            # kruidenbergjes voor de winkels
            for x in range(bx + 2, bx + 9, 2):
                zet(W, x, yv, zw + s_ * 1, ["minecraft:orange_concrete_powder", "minecraft:yellow_concrete_powder",
                                            "minecraft:red_concrete_powder", "minecraft:brown_concrete_powder",
                                            "minecraft:lime_concrete_powder"][(x + i) % 5])
    # dak tussen de koepels: plat met borstwering
    ZB.dakrand(W, x1, za - 1, x2, zb + 1, yv + H + 1, "muur_perzik", kanteel=True)
    # ingangen: iwans aan de kopse kanten
    ZB.iwan(W, (-1, 0), x1, zm + 0.5, yv, 7, 7, 3, buiten="muur_perzik", kader="tegel_roze", vak="tegel_blauw")
    ZB.iwan(W, (1, 0), x2, zm + 0.5, yv, 7, 7, 3, buiten="muur_perzik", kader="tegel_roze", vak="tegel_blauw")
    # dwarsstraat naar de brug/het paleis (noord) door de kruising
    for x in range(KRUIS - 2, KRUIS + 3):
        for z in range(za - 1, z1):
            for y in range(yv, yv + 7):
                zet(W, x, y, z, "minecraft:air")
    if figuren:
        rng = rng_van(x1, z1, 99)
        for k in range(46):
            x = rng.uniform(x1 + 3, x2 - 3)
            z = rng.uniform(z1 + 0.6, z2 - 0.4)
            W.wguh(x, yv, z, rng.choice([90, 270, 60, 300, 0, 180]), skin=rng.choice(
                ["guh", "guh", "guh_mint", "guh_wolk", "guh_bloesemguh", "guh_snow", "guh_golden", "guh_choco",
                 "guh_konijntje_roze", "guh_pluisguh"]))
    W.label(KRUIS, yv + H + 16, zm, "De Grote Bazaar")
    W.label(x1 + 26, yv + H + 2, zm, "koepeltje met lichtgat")
    W.label(x1 + 13, yv + 3, z1 - 2, "winkelnis (kruiden, potten, manden)")
    W.label(x1 + 33, yv + 9, zm - 3, "lampionsnoeren")
    W.label(KRUIS, yv + 6, zm, "de kruising onder de grote koepel")


# ============================================================================================ brug, norias, boten
def saffraanbrug(W):
    """Brug met twee lagen spitsbogen en een paviljoen met koepeltje in het midden (Khaju-achtig)."""
    x = BRUG_X
    d1, rb = rivier_afstand(x, -846)
    z1, z2 = -870, -828
    if not raakt(W, x - 8, Y - 10, z1, x + 8, Y + 25, z2):
        return
    for z in range(z1, z2 + 1):
        for xx in range(x - 6, x + 7):
            zet(W, xx, Y + 3, z, "vloer_zand")
            for y in range(ZW.ZEE - 3, Y + 3):
                zet(W, xx, y, z, "steen_roze" if abs(xx - x) >= 5 else ("steen_vanille" if y <= ZW.ZEE + 1 else "minecraft:air"))
        for xx in (x - 6, x + 6):
            zet(W, xx, Y + 4, z, ZB.muurtje("steen_roze") if z % 4 else "wit_glad")
    # onderbogen (in de westgevel en oostgevel): spitsbogen boven het water
    for kant, c in (((-1, 0), x - 6), ((1, 0), x + 6)):
        for zc in range(z1 + 3, z2 - 1, 6):
            ZB.boog(W, kant, c, zc + 0.5, ZW.ZEE + 1, 4, 3, diep=13, kader="tegel_roze")
            ZB.boog(W, kant, c, zc + 0.5, Y + 4, 3, 3, diep=1, kader="goud", achter="minecraft:air") if False else None
    # bovenste arcade (wandelgalerij) langs beide kanten
    for z in range(z1, z2 + 1, 3):
        for xx in (x - 6, x + 6):
            for y in range(Y + 4, Y + 9):
                zet(W, xx, y, z, "steen_roze")
    for z in range(z1, z2 + 1):
        for xx in range(x - 6, x + 7):
            zet(W, xx, Y + 9, z, "steen_vanille")
        for xx in (x - 6, x + 6):
            zet(W, xx, Y + 10, z, ZB.muurtje("steen_roze") if z % 3 else "goud")
    # paviljoen in het midden
    zm = (z1 + z2) // 2
    doos(W, x - 7, Y + 4, zm - 5, x + 7, Y + 12, zm + 5, "muur_vanille")
    leeg(W, x - 6, Y + 4, zm - 4, x + 6, Y + 11, zm + 4)
    for kant, c in (((-1, 0), x - 7), ((1, 0), x + 7)):
        ZB.boog(W, kant, c, zm + 0.5, Y + 4, 5, 4, diep=1, kader="tegel_turkoois")
    ZB.koepel(W, x + 0.5, Y + 13, zm + 0.5, 4.5, k_dak="koepel_turkoois", vorm="ui", ribben=8, trommel=2,
              trommel_k="muur_vanille", oortjes=True)
    W.label(x, Y + 26, zm, "De Saffraanbrug")


def noria(W, x, z, r=7, as_="x"):
    """Waterrad (noria) aan de oever: houten rad met emmertjes, op een stenen pijler, half in het water."""
    yc = ZW.ZEE + r - 2
    for a in np.linspace(0, 2 * math.pi, 64, endpoint=False):
        for rr in (r, r - 0.6):
            px = x + (rr * math.cos(a) if as_ == "z" else 0)
            pz = z + (rr * math.cos(a) if as_ == "x" else 0)
            zet(W, px, yc + rr * math.sin(a), pz, "minecraft:stripped_cherry_log[axis=y]")
    for k in range(8):
        a = k * math.pi / 4
        for s in np.arange(0, r, 0.7):
            px = x + (s * math.cos(a) if as_ == "z" else 0)
            pz = z + (s * math.cos(a) if as_ == "x" else 0)
            zet(W, px, yc + s * math.sin(a), pz, "hout")
        px = x + ((r + 0.8) * math.cos(a) if as_ == "z" else 0)
        pz = z + ((r + 0.8) * math.cos(a) if as_ == "x" else 0)
        zet(W, px, yc + (r + 0.8) * math.sin(a), pz, "minecraft:barrel[facing=up,open=true]")
    for d in (-1, 1):
        px, pz = (x, z + d * 2) if as_ == "z" else (x + d * 2, z)
        for y in range(ZW.ZEE - 3, yc + 1):
            zet(W, px, y, pz, "steen_roze")


def bootje(W, x, z, L=9, richting=(1, 0), kleur="roze"):
    fx, fz = richting
    rx, rz = -fz, fx
    y = ZW.ZEE + 1
    for l in range(-L // 2, L // 2 + 1):
        t = abs(l) / (L / 2)
        hw = 2 * (1 - t ** 2)
        for s in range(-2, 3):
            if abs(s) <= hw + 0.3:
                zet(W, x + fx * l + rx * s, y - 1, z + fz * l + rz * s, "hout")
                if abs(s) >= hw - 0.7:
                    zet(W, x + fx * l + rx * s, y, z + fz * l + rz * s, "minecraft:cherry_planks")
    for h in range(1, 9):
        zet(W, x, y + h, z, "minecraft:stripped_cherry_log[axis=y]")
    for h in range(3, 8):
        for s in range(0, 9 - h):
            zet(W, x - fx * 0 + rx * 0 + fx * (s - 1) * 0.6, y + h, z + fz * (s - 1) * 0.6, WOL[kleur]) if False else None
    # driehoekig zeil (feloek)
    for h in range(2, 9):
        for k in range(0, 9 - h):
            zet(W, x + fx * (k - 1), y + h, z + fz * (k - 1), WOL[kleur] if (h + k) % 4 else WOL["wit"])


# ============================================================================================ overige gebouwen
def karavanserai(W):
    x1, z1, x2, z2 = KARAVANSERAI
    if not raakt(W, x1, Y, z1, x2, Y + 30, z2):
        return
    doos(W, x1, Y, z1, x2, Y + 9, z2, "muur_boter")
    leeg(W, x1 + 6, Y, z1 + 6, x2 - 6, Y + 9, z2 - 6)
    leeg(W, x1 + 1, Y, z1 + 1, x2 - 1, Y + 3, z2 - 1)
    for x in range(x1 + 6, x2 - 5, 4):
        for z in (z1 + 6, z2 - 6):
            ZB.boog(W, (0, 1) if z == z1 + 6 else (0, -1), z, x + 0.5, Y, 3, 2, diep=1, kader="tegel_roze")
    ZB.dakrand(W, x1, z1, x2, z2, Y + 10, "muur_boter")
    ZB.iwan(W, (-1, 0), x1, (z1 + z2) / 2, Y, 7, 7, 3, buiten="muur_boter", kader="tegel_roze", vak="tegel_turkoois")
    for (tx, tz) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        ZB.wachttoren(W, tx, tz, Y, 13, 3, wand="muur_boter", band="tegel_roze", dak="koepel_roze")
    ZB.fontein(W, (x1 + x2) // 2, Y, (z1 + z2) // 2, r=3)
    W.karavanserai = ((x1 + x2) / 2, Y, (z1 + z2) / 2)
    W.label((x1 + x2) // 2, Y + 18, (z1 + z2) // 2, "Karavanserai")


def sterrenwacht(W):
    x, z = STERRENWACHT
    if not raakt(W, x - 12, Y, z - 12, x + 12, Y + 60, z + 12):
        return
    ZB.wachttoren(W, x, z, Y, 30, 7, wand="muur_lavendel", band="tegel_blauw", dak=None, kantelen=False)
    ZB.koepel(W, x + 0.5, Y + 31, z + 0.5, 7, k_dak="koepel_lavendel", vorm="rond", ribben=12, rib="goud", oortjes=True)
    for k in range(12):
        zet(W, x + 2 + k * 0.7, Y + 36 + k * 0.6, z, "minecraft:copper_block")
    W.label(x, Y + 46, z, "Sterrenwacht van Twinkel")


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
        if math.hypot(x - WESTPOORT[0], z - WESTPOORT[1]) < 7 or math.hypot(x - ZUIDPOORT[0], z - ZUIDPOORT[1]) < 7:
            continue
        yb = min(Y - 1, ZW.hoogte(W, int(x), int(z)) - 2)
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(yb, Y + 10):
                    zet(W, x + dx, y, z + dz, "steen_roze" if (y - Y) % 6 else "steen_perzik")
                zet(W, x + dx, Y + 10, z + dz, "steen_vanille")
                if i % 3 != 0:
                    zet(W, x + dx, Y + 11, z + dz, "steen_roze")
                    if i % 3 == 1:
                        zet(W, x + dx, Y + 12, z + dz, plaat("steen_vanille"))
        if i % 52 == 0:
            ZB.wachttoren(W, int(x), int(z), yb, Y + 16 - yb, 4)
    # westpoort
    wx, wz = WESTPOORT
    if raakt(W, wx - 10, Y, wz - 10, wx + 10, Y + 30, wz + 10):
        for (tx, tz) in ((wx - 1, wz - 8), (wx - 1, wz + 8)):
            ZB.wachttoren(W, tx, tz, Y - 1, 20, 4, dak="koepel_turkoois")
        doos(W, wx - 3, Y, wz - 5, wx + 1, Y + 14, wz + 5, "steen_roze")
        ZB.iwan(W, (-1, 0), wx - 3, wz + 0.5, Y, 5, 6, 5, buiten="steen_roze", kader="tegel_turkoois", vak="tegel_roze")
        for x in range(wx - 3, wx + 2):
            for z in range(wz - 2, wz + 3):
                for y in range(Y, Y + 6):
                    zet(W, x, y, z, "minecraft:air")
        W.label(wx, Y + 30, wz, "Westpoort (van Rozenkloof)")
    # zuidpoort (de duinroute)
    zx, zz = ZUIDPOORT
    if raakt(W, zx - 12, Y - 4, zz - 10, zx + 12, Y + 30, zz + 10):
        yb = min(Y - 1, ZW.hoogte(W, zx, zz + 6) - 2)
        for (tx, tz) in ((zx - 9, zz), (zx + 9, zz)):
            ZB.wachttoren(W, tx, tz, yb, Y + 20 - yb, 4, dak="koepel_goud")
        doos(W, zx - 6, yb, zz - 3, zx + 6, Y + 16, zz + 1, "steen_roze")
        ZB.iwan(W, (0, 1), zz + 1, zx + 0.5, Y, 6, 7, 5, buiten="steen_roze", kader="tegel_turkoois", vak="tegel_roze")
        for x in range(zx - 2, zx + 3):
            for z in range(zz - 4, zz + 3):
                for y in range(Y, Y + 6):
                    zet(W, x, y, z, "minecraft:air")
        for k in (-4, 4):
            for y in range(Y + 8, Y + 15):
                zet(W, zx + k, y, zz + 2, WOL["roze"] if y % 2 else WOL["wit"])
        W.label(zx, Y + 30, zz, "Zuidpoort (de duinroute)")


def vrij(x1, z1, x2, z2):
    """Is deze rechthoek vrij voor een woonhuis (binnen de stad, buiten de rivier en de grote gebouwen)?"""
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2), ((x1 + x2) // 2, (z1 + z2) // 2)):
        if not in_stad(x, z, marge=8):
            return False
        d, rb = rivier_afstand(x, z)
        if d < rb + 7:
            return False
    for (a, b_, c, d) in (PALEIS, (TUIN[0] - 4, TUIN[1], TUIN[2] + 4, TUIN[3] + 10), (BAZAAR[0] - 3, BAZAAR[1] - 9, BAZAAR[2] + 3, BAZAAR[3] + 9),
                          (KARAVANSERAI[0] - 4, KARAVANSERAI[1] - 4, KARAVANSERAI[2] + 4, KARAVANSERAI[3] + 4),
                          (STERRENWACHT[0] - 12, STERRENWACHT[1] - 12, STERRENWACHT[0] + 12, STERRENWACHT[1] + 12),
                          (BRUG_X - 12, -900, BRUG_X + 12, -800), (KRUIS - 10, -840, KRUIS + 10, -818),
                          (WESTPOORT[0] - 4, WESTPOORT[1] - 12, WESTPOORT[0] + 30, WESTPOORT[1] + 8),
                          (ZUIDPOORT[0] - 12, ZUIDPOORT[1] - 22, ZUIDPOORT[0] + 12, ZUIDPOORT[1] + 4)):
        if not (x2 < a or x1 > c or z2 < b_ or z1 > d):
            return False
    return True


def woonwijken(W):
    x1, _, z1, x2, _, z2 = W.b
    blok = 19
    for bx in range(CX - RX, CX + RX, blok):
        for bz in range(CZ - RZ, CZ + RZ, blok):
            if not raakt(W, bx, Y, bz, bx + blok, Y + 30, bz + blok, marge=2):
                continue
            rng = rng_van(bx, bz, 5)
            # 2-4 huizen per bouwblok (straatjes van 3 ertussen)
            x = bx + 2
            while x < bx + blok - 5:
                w = rng.randint(6, 10)
                z = bz + 2
                while z < bz + blok - 5:
                    d = rng.randint(6, 10)
                    hx2, hz2 = min(x + w, bx + blok - 2), min(z + d, bz + blok - 2)
                    if hx2 - x >= 5 and hz2 - z >= 5 and vrij(x, z, hx2, hz2):
                        voor = [(0, 1), (0, -1), (1, 0), (-1, 0)][rng.randint(4)]
                        ZB.huis(W, x, z, hx2, hz2, Y, verd=int(rng.choice([1, 2, 2, 2, 3, 3, 4])), seed=x * 7 + z,
                                voor=voor)
                    z = hz2 + 2
                x += w + 2
            # straatjes: lantaarnpaaltje op de hoek, palmen
            if vrij(bx, bz, bx + 1, bz + 1) and rng.rand() < 0.5:
                ZB.palm(W, bx, Y, bz, seed=bx)


def kanaalwijk(W):
    for kx in (3690, 3716):
        z_r = -846
        for z in range(z_r + 8, z_r + 40):
            if raakt(W, kx - 3, Y - 4, z, kx + 3, Y + 2, z, marge=1):
                ZB.kanaal(W, kx - 1, z, kx + 1, z, Y)
        # bruggetje
        for x in range(kx - 2, kx + 3):
            zet(W, x, Y, z_r + 24, plaat("steen_vanille"))


def rivieroevers(W):
    """Kademuren met lantaarns langs de rivier in de stad; norias (waterraderen) op de oevers en feloekjes."""
    d, v, _ = ZW.lijn_afstand(W.XX, W.ZZ, ZW.RIVIER)
    rb = v[..., 0]
    st = ((W.XX - CX) / RX) ** 2 + ((W.ZZ - CZ) / RZ) ** 2 < 1
    kade = st & (d > rb) & (d < rb + 1.3)
    for i, j in np.argwhere(kade):
        x, z = int(math.floor(W.xs[i])), int(math.floor(W.zs[j]))
        for y in range(ZW.ZEE - 2, Y):
            zet(W, x, y, z, "steen_roze")
        zet(W, x, Y - 1, z, "steen_vanille")
        zet(W, x, Y, z, ZB.muurtje("steen_vanille") if (x + z) % 9 else "steen_vanille")
        if (x * 3 + z) % 29 == 0:
            zet(W, x, Y + 1, z, lantaarn(False))
    for (nx, kant) in ((3700, -1), (3742, 1), (3836, -1), (3858, 1)):
        zc, rb_ = rivier_z(nx)
        nz = int(round(zc + kant * (rb_ - 2)))
        if raakt(W, nx - 9, ZW.ZEE - 3, nz - 3, nx + 9, ZW.ZEE + 14, nz + 3):
            noria(W, nx, nz, r=7, as_="z")
    for (bx, kl, dz) in ((3722, "roze", 3), (3762, "wit", -3), (3790, "turkoois", 2), (3672, "geel", -2)):
        zc, _ = rivier_z(bx)
        bz = int(round(zc + dz))
        if raakt(W, bx - 6, ZW.ZEE, bz - 6, bx + 6, ZW.ZEE + 12, bz + 6):
            bootje(W, bx, bz, L=9, richting=(1, 0), kleur=kl)


def bouw(W, figuren=True, spiegels=False):
    if not raakt(W, CX - RX - 20, Y - 10, CZ - RZ - 20, CX + RX + 20, Y + 120, CZ + RZ + 20, marge=0):
        return
    rivieroevers(W)
    paleis(W, figuren=figuren, spiegels=spiegels)
    bazaar(W, figuren=figuren)
    saffraanbrug(W)
    karavanserai(W)
    sterrenwacht(W)
    kanaalwijk(W)
    woonwijken(W)
    stadsmuur(W)
