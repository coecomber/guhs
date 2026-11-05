"""Alle grote gebouwen van Hoogvads op hun plek zetten (aangeroepen door stad.bouw)."""
import math
import numpy as np
from palet import P, trap, plaat
import bouwhulp as BH
import kathedraal as KA
import wijken as WK
import figuren as FG


def kathedraalplein(W, S):
    y0 = 210
    BH.doos(W, 40, y0, -36, 64, y0, 36, "vloer")
    # trap van het plein (210) naar de kathedraal (214)
    for k in range(4):
        BH.doos(W, 60 + k, y0 + k, -30, 60 + k, y0 + k, 30, "wit_glad")
        BH.doos(W, 60 + k, y0 + 1 + k, -30, 60 + k, y0 + 1 + k, 30, trap("wit_trap", "east"))
    # trap van de markt (208) naar het plein (210)
    for k in range(2):
        BH.doos(W, 40 + k, 208 + k, -24, 40 + k, 208 + k, 24, "wit_glad")
        BH.doos(W, 40 + k, 209 + k, -24, 40 + k, 209 + k, 24, trap("wit_trap", "east"))
    # obelisken met lantaarns
    for z in (-26, 26):
        BH.doos(W, 50, y0 + 1, z - 1, 52, y0 + 2, z + 1, "wit_glad")
        BH.doos(W, 51, y0 + 3, z, 51, y0 + 18, z, P["zuil"])
        BH.spits(W, 51, z, y0 + 19, 1, 3, dak="dak_goud", hoeken=4, rand=None, oortjes=True)
    for x in range(44, 60, 5):
        for z in (-33, 33):
            BH.lantaarnpaal(W, x, y0 + 1, z, h=4)


# verschuivingen van de gebouwscripts (ronde 1-coördinaten) naar de nieuwe, kortere walvis (ronde 2)
D_PALEIS = (-18, 30, 0)
D_UNI = (-30, 30, 0)
D_KAZ = (-30, 30, 0)
D_PLEIN = (-44, 30, 0)
D_HAVEN = (-56, 30, 0)


def alles(W, S, figuren=True, log=print):
    import time
    import markt2 as MK2
    t = time.time()
    BH.keermuur(W, 14, -36, 126, 36, KA.Y0, kanten=("n", "z"), stap=10)
    KA.bouw(W, figuren)
    log("kathedraal %.0fs" % (time.time() - t))
    Wpl = BH.Schuif(W, *D_PLEIN)
    kathedraalplein(Wpl, S)
    Wp = BH.Schuif(W, *D_PALEIS)
    WK.paleis(Wp, S)
    log("paleis %.0fs" % (time.time() - t))
    Wu = BH.Schuif(W, *D_UNI)
    WK.universiteit(Wu, S)
    MK2.markt(W, S)
    Wk = BH.Schuif(W, *D_KAZ)
    WK.kazerne(Wk, S)
    log("uni/markt/kazerne %.0fs" % (time.time() - t))
    WK.staartpark(W, S)
    WK.ooggaarde(W, S)
    W.balkon_oog = WK.balkon(W, S, 150, 70, kant=1, r=7)
    Wh = BH.Schuif(W, *D_HAVEN)
    WK.haven(Wh, S)
    WK.haven_schepen(Wh)
    log("parken/haven %.0fs" % (time.time() - t))
    W.npcs = []
    if figuren:
        KA.figuren(W)
        WK.paleis_figuren(Wp)
        WK.uni_figuren(Wu)
        MK2.markt_figuren(W)
        WK.kazerne_figuren(Wk)
        WK.haven_figuren(Wh)
        FG.menigte(W, -2, -32, 16, 32, 241, 40, seed=51)
        park_figuren(W, S)
        deeltjes(W, S)
        log("figuren %.0fs" % (time.time() - t))


def park_figuren(W, S):
    rng = np.random.RandomState(77)
    for k in range(60):
        x, z = rng.uniform(-195, -157), rng.uniform(-44, 44)
        g = S.g(int(x), int(z))
        if g is None:
            continue
        W.wguh(x, g + 1, z, rng.uniform(0, 360), skin=["guh", "guh_mint", "guh_bloesemguh", "guh_wolk"][k % 4],
               sitting=rng.rand() < 0.4)


def deeltjes(W, S):
    """Bloesemblaadjes in de parken en op de markt, zwermen vogeltjes (wolkzwaluwen) rond de walvis."""
    rng = np.random.RandomState(99)
    D = W.deeltjes
    roze = [(255, 190, 215), (255, 214, 232), (250, 160, 200)]
    for (x1, x2, z1, z2, n) in ((-195, -157, -45, 45, 500), (-72, -8, -24, 24, 220), (-60, -10, -68, -42, 120),
                                 (132, 170, 30, 80, 80)):
        for _ in range(n):
            x, z = rng.uniform(x1, x2), rng.uniform(z1, z2)
            g = S.g(int(x), int(z))
            if g is None:
                continue
            D.append((x, g + rng.uniform(1, 12), z, roze[rng.randint(3)], 0.28, 0.0, "blad"))
    for (cx, cy, cz, n) in ((190, 280, 150, 22), (-230, 270, 150, 16), (40, 300, -150, 18), (300, 230, 60, 14)):
        for _ in range(n):
            D.append((cx + rng.normal(0, 14), cy + rng.normal(0, 6), cz + rng.normal(0, 14), (255, 252, 250),
                      0.9, 0.0, "vogel"))
