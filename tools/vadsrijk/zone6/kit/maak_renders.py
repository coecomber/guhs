"""Conceptrenders van zone 6 "Het Zijdeland" (Guhs 1.10+, Het Vadsrijk).

python maak_renders.py <shot ...> [snel|mid]
  shots: kloof_overzicht kloof_straat skyline bazaar spiegelzaal karavaan zoutvlakte toren   (of: alles)
  snel = klein (960x540, ss 1, zonder schaduw), mid = finale maat maar ss 1, zonder = finale (ss 2, 2400x1350).
Elk shot bouwt zijn eigen venster (spelcoördinaten) met dezelfde generators: zijwereld (land), rozenkloof,
saffraanpoort, landschap (karavaan, bakens, toren). De kaart staat in kaart.py.
"""
import sys, os, time, math
import numpy as np
import hoogkit  # noqa
import opname as O
import schemerkit as SK
import zijwereld as ZW

SNEL = "snel" in sys.argv
MID = "mid" in sys.argv
SS = 1 if (SNEL or MID) else 2
PRE = "_proef_" if SNEL else ("_mid_" if MID else "")
BREED, HOOG = (960, 540) if SNEL else (2400, 1350)

ZON_WEST = dict(O.GOUDEN_UUR, zon=(-0.80, 0.30, 0.45), zenit=(120, 128, 214), horizon=(255, 190, 170))
ZON_NAMIDDAG = dict(O.NAMIDDAG, zon=(-0.45, 0.52, 0.62), zenit=(128, 150, 230), horizon=(255, 214, 204))
ZON_OOST_OCHTEND = dict(O.GOUDEN_UUR, zon=(0.78, 0.32, 0.40), zenit=(126, 140, 222), horizon=(255, 200, 186))
ZON_KLOOF = dict(O.GOUDEN_UUR, zon=(-0.74, 0.42, 0.40), zenit=(108, 122, 214), horizon=(255, 192, 172),
                 amb_kl=(0.40, 0.36, 0.56), zon_k=1.08)


def kleurgrade(sat=1.15, con=1.08):
    def f(rgb):
        m = rgb.mean(axis=2, keepdims=True)
        rgb = m + (rgb - m) * sat
        return (rgb - 0.5) * con + 0.5
    return f
SCHEMER = dict(O.GOUDEN_UUR, zon=(-0.85, 0.10, 0.30), zon_kl=(1.0, 0.62, 0.62), amb_kl=(0.42, 0.36, 0.58), zon_k=0.85,
               zenit=(58, 52, 120), horizon=(252, 150, 160), zon_rgb=(255, 170, 150), wolk_licht=(255, 196, 210),
               wolk_schaduw=(110, 90, 160))
NACHT = dict(O.GOUDEN_UUR, zon=(0.35, 0.55, -0.6), zon_kl=(0.55, 0.62, 1.0), amb_kl=(0.20, 0.18, 0.36), zon_k=0.55,
             mc=0.30, zenit=(14, 14, 44), horizon=(70, 48, 110), zon_rgb=(60, 60, 110), wolk_licht=(90, 80, 140),
             wolk_schaduw=(30, 26, 60), cirrus=False)
BINNEN = dict(O.NAMIDDAG, zon=(-0.25, 0.85, 0.40), amb_kl=(0.66, 0.56, 0.58), zon_kl=(1.0, 0.86, 0.66), zon_k=1.6,
              mc=0.22)


def log(*a):
    O.log(*a)


def venster(x1, y1, z1, x2, y2, z2, delen=("land", "groen", "rozenkloof", "saffraanpoort", "landschap"),
            figuren=True, uitsluit=None):
    t = time.time()
    W = SK.Wereld(x1, y1, z1, x2, y2, z2, 1.0)
    if "land" in delen:
        ZW.bouw_land(W)
    if "rozenkloof" in delen:
        import rozenkloof as RK
        RK.bouw(W)
    if "saffraanpoort" in delen:
        import saffraanpoort as SP
        SP.bouw(W)
    if "landschap" in delen:
        import landschap as LS
        LS.bouw(W)
    if "groen" in delen:
        ZW.groen(W, uitsluit=uitsluit or _vrij_van_steden)
    if figuren:
        import zijfiguren as ZF
        ZF.bevolk(W)
    log("venster", W.v.shape, "gebouwd in %.0fs" % (time.time() - t), len(W.pal), "blokken")
    return W


def _vrij_van_steden(x, z):
    """Geen wilde palmen in de stad of op de kloofrichels (die hebben hun eigen groen)."""
    sx, sz = ZW.SAFFRAAN
    if ((x - sx) / (ZW.STAD_R[0] + 6)) ** 2 + ((z - sz) / (ZW.STAD_R[1] + 6)) ** 2 < 1:
        return True
    if 3300 < x < 3500 and -780 < z < -650:
        return True
    return False


def schoot(W, naam, pos, kijk, fov, licht, **kw):
    hc = kw.pop("hoogte_cam", None)
    if hc is not None:
        pos = (pos[0], ZW.hoogte(W, pos[0], pos[2]) + hc, pos[2])
    if SNEL:
        kw["schaduw"] = kw.get("schaduw_snel", False)
        if not kw.get("stralen_snel"):
            kw.pop("stralen", None)
    kw.pop("schaduw_snel", None)
    kw.pop("stralen_snel", None)
    kw.setdefault("zon_px", 1.2 if SNEL else 2.6)
    kw.setdefault("wolken", False)
    kw.setdefault("kleur", kleurgrade())
    return O.opname(W, PRE + naam, pos, kijk, fov=fov, breed=BREED, hoog=HOOG, ss=SS, licht=licht, **kw)


# ============================================================================================ shots
def kloof_overzicht():
    W = venster(3150, 30, -840, 3560, 205, -590)
    return schoot(W, "01a_rozenkloof_overzicht", (3214, 124, -772), (3450, 104, -704), 50, ZON_KLOOF,
                  nevel=(0.25, 1400), titel="Rozenkloof",
                  onderschrift="de kloofstad in roze zandsteen: aankomst per luchtschip (lijn E), de Schatkamer, "
                               "huizen op drie richels, bruggen en hangende tuinen")


def kloof_straat():
    W = venster(3340, 55, -775, 3510, 160, -645)
    return schoot(W, "01b_rozenkloof_plein", (3380, 71.2, -719), (3478, 90, -702), 64, ZON_KLOOF,
                  nevel=(0.12, 900), titel="Het Rozenplein",
                  onderschrift="op de kloofbodem: fontein, marktkraampjes, de bultguh-stal; achterin de Schatkamer",
                  zon_box=(3340, 55, -775, 3510, 160, -645))


def skyline():
    W = venster(3610, 55, -975, 3910, 210, -735)
    return schoot(W, "02a_saffraanpoort_skyline", (3626, 132, -722), (3772, 90, -884), 50, ZON_WEST,
                  nevel=(0.2, 1400), titel="Saffraanpoort",
                  onderschrift="de hoofdstad aan de Saffraanrivier: het Rozijnenpaleis met de gouden koepel en vier "
                               "minaretten met guh-oortjes, de Grote Bazaar, norias en de Saffraanbrug",
                  zon_box=(3610, 55, -975, 3910, 210, -735))


def stad_lucht():
    W = venster(3610, 55, -975, 3910, 210, -735)
    return schoot(W, "_luchtfoto_saffraanpoort", (3640, 170, -730), (3770, 74, -858), 60, ZON_WEST, nevel=(0.1, 2000),
                  titel="Saffraanpoort (luchtfoto, werkshot)")


def bazaar_binnen():
    import saffraanpoort as SP
    W = venster(3694, 60, -832, 3832, 112, -792, delen=("land",), figuren=False)
    SP.bazaar(W, figuren=True)
    import zijfiguren as ZF
    ZF.bevolk(W)
    y = SP.Y
    return schoot(W, "02b_bazaar_binnen", (3702.5, y + 2.4, -812.0), (3790, y + 8.0, -812.5), 64, BINNEN, binnen=True,
                  achtergrond=(250, 214, 190), gloed=0.9, titel="De Grote Bazaar",
                  onderschrift="een overdekte straat van koepeltjes met lichtgaten: kruiden, tapijten, lampjes en "
                               "honderden winkeltjes; middenin de kruising onder de grote koepel",
                  zon_box=(3694, 60, -832, 3832, 112, -792), schaduw_snel=True,
                  stralen=dict(sterkte=1.4, max=90.0, n=48, box=(3698, y, -819, 3828, y + 30, -806), buiten=0.0))


def spiegelzaal():
    import saffraanpoort as SP
    x1, z1, x2, z2 = SP.ZAAL
    W = venster(x1 - 3, 60, z1 - 26, x2 + 3, 104, z2 + 26, delen=("land",), figuren=False)
    SP.spiegelzaal(W, spiegels=True, figuren=True)
    import zijfiguren as ZF
    ZF.bevolk(W)
    yv = SP.Y + 3
    zm = (z1 + z2) / 2 + 0.5
    return schoot(W, "02c_paleis_spiegelzaal", (x1 + 2.5, yv + 2.6, zm), (x2, yv + 6.5, zm), 70, BINNEN, binnen=True,
                  achtergrond=(60, 40, 70), gloed=1.0, titel="De Spiegelzaal",
                  onderschrift="in het Rozijnenpaleis: spiegelbogen, parelmoermozaïek, kroonluchters en de troon van "
                               "Sultane Rozijntje onder de pauwenwaaier",
                  zon_box=(x1 - 3, 60, z1 - 1, x2 + 3, 104, z2 + 1), schaduw_snel=True,
                  stralen=dict(sterkte=1.0, max=60.0, n=40, box=(x1, yv, z1, x2, yv + 20, z2), buiten=0.0))


def karavaan():
    import zijfiguren as ZF
    W = venster(3630, 55, -990, 3900, 200, -555)
    pad = ZW.WEG_ZUID[2:]
    ZF.karavaan(W, pad, lambda x, z: ZW.hoogte(W, x, z), n_bult=9, n_tapijt=3, seed=4, start=14.0, stap=5.0)
    return schoot(W, "03_roze_duinen_karavaan", (3733, 0, -578), (3744, 82, -716), 56, ZON_WEST,
                  nevel=(0.28, 900), titel="De Roze Duinen",
                  onderschrift="een sterrenkaravaan op weg naar Saffraanpoort: guhs op bultguhs en vliegende tapijten",
                  zon_box=(3660, 55, -800, 3800, 140, -560), hoogte_cam=6.5)


def zoutvlakte():
    W = venster(3850, 55, -720, 4045, 140, -520)
    bx, by, bz = W.baken
    kx, ky, kz = W.kamp
    import zijfiguren as ZF
    for i, (dx, dz, yaw) in enumerate(((4, 5, 200), (-5, 6, 120), (6, -3, 300))):
        ZF.zet_bultguh(W, kx + dx + 0.5, ZW.hoogte(W, kx + dx, kz + dz), kz + dz + 0.5, yaw, variant=["zandroze", "wolkwit", "kaneel"][i],
                       deken=i, ruiter=False)
    for k in range(6):
        a = k * math.pi / 3
        W.wguh(kx + 0.5 + math.cos(a) * 2.3, ky, kz + 0.5 + math.sin(a) * 2.3, (math.degrees(a) + 90) % 360,
               skin=["guh", "guh_mint", "guh_wolk", "guh_golden", "guh_snow", "guh_bloesemguh"][k], sitting=True)
    ZF.zet_tapijt(W, bx + 12.5, by - 18, bz + 8.5, 40, patroon=2, ruiter=True, skin="guh_konijntje_roze", hoogte=6.0)
    lampen = [(bx + 0.5, by + 0.5, bz + 0.5, (0.75, 0.85, 1.0), 46, 1.5),
              (kx + 0.5, ky + 1.2, kz + 0.5, (1.0, 0.62, 0.32), 16, 1.6)]
    return schoot(W, "04_zoutvlakte_sterrenbaken", (3990, ky + 13.0, -556), (bx - 4, by - 12, bz + 2), 62, NACHT, nevel=(0.25, 700),
                  titel="De Zoutvlakte bij nacht", onderschrift="het Zoutbaken brandt weer: de karavaan kan op de sterren "
                  "verder. Op de grond het sterrenbeeld-raadsel (een guh-hoofdje), bij het kampvuur de karavaan",
                  gloed=1.3, lampen=lampen, sterren=2600, zon_box=(3850, 55, -720, 4045, 140, -520))


def toren():
    W = venster(3862, 55, -912, 4192, 262, -598)
    tx, ty, tz = W.toren_top
    return schoot(W, "05_sterrendovende_toren", (3930, 146, -872), (4137, 170, -689), 63, SCHEMER, nevel=(0.22, 1100),
                  titel="De Sterrendovende Toren", onderschrift="de raid bij de Moederoase: vier verdiepingen, vier bazen, "
                  "en bovenin de Lege Lens van Hertogin Ringeling, die het sterrenlicht naar binnen zuigt",
                  gloed=1.0, sterren=900, zon_box=(4040, 55, -800, 4192, 262, -598),
                  lampen=[(tx + 0.5, ty + 0.5, tz + 0.5, (0.7, 0.55, 1.0), 40, 0.8)])


SHOTS = dict(karavaan=karavaan, zoutvlakte=zoutvlakte, toren=toren, bazaar=bazaar_binnen, spiegelzaal=spiegelzaal, kloof_overzicht=kloof_overzicht, kloof_straat=kloof_straat, skyline=skyline, stad_lucht=stad_lucht)

if __name__ == "__main__":
    namen = [a for a in sys.argv[1:] if a not in ("snel", "mid")]
    if "alles" in namen:
        namen = list(SHOTS)
    for n in namen:
        t = time.time()
        SHOTS[n]()
        log("== %s klaar in %.0fs" % (n, time.time() - t))
