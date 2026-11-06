"""U-Z5: check renders of the GAME build of zone 5 (maak5.bouw_wereld = kit + bouw5), with the round-2 render kit (opname/prender).

    python tools/vadsrijk/zone5/render5.py <uit-map> <prefix> <shot ...> [snel|mid]
shots: kathedraal kathedraal_zij kathedraal_binnen universiteit uni_binnen paleis troonzaal raadszaal academie station overzicht
       (or: alles). Default output size 1600x900 ss 1 ("mid"); "snel" = 960x540 without shadows; nothing = 2400x1350 ss 2.
Coordinates are the kit's LOCAL coordinates (head = +x, y = render y; the game is 20 lower and turned 180 degrees).
"""
import os
import sys
import time

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
sys.path.insert(1, os.path.join(HIER, "kit"))

SNEL = "snel" in sys.argv
GROOT = "groot" in sys.argv
SS = 2 if GROOT else 1
BREED, HOOG = (960, 540) if SNEL else ((2400, 1350) if GROOT else (1600, 900))


def log(*a):
    print(time.strftime("%H:%M:%S"), "r5", *a, flush=True)


def main():
    args = [a for a in sys.argv[1:] if a not in ("snel", "groot")]
    uit, pre, shots = args[0], args[1], args[2:]
    import maak5
    W, S, info = maak5.bouw_wereld()
    import opname as O
    os.makedirs(uit, exist_ok=True)
    O.OUT = uit
    W.labels = []
    zon_west = dict(O.GOUDEN_UUR, zon=(-0.80, 0.24, 0.55))
    zon_zuid = dict(O.GOUDEN_UUR, zon=(-0.30, 0.20, 0.93))
    binnen = dict(O.NAMIDDAG, zon=(-0.30, 0.78, 0.55), amb_kl=(0.40, 0.35, 0.46), zon_kl=(1.0, 0.86, 0.66), zon_k=1.6, mc=0.25)

    def schoot(naam, pos, kijk, fov, licht, **kw):
        if SNEL:
            kw["schaduw"] = False
            kw.pop("stralen", None)
        kw.setdefault("zon_px", 1.2 if SNEL else 2.0)
        return O.opname(W, pre + naam, pos, kijk, fov=fov, breed=BREED, hoog=HOOG, ss=SS, licht=licht, **kw)

    SHOTS = {
        "kathedraal": lambda: schoot("kathedraal_west", (-125, 330, 140), (50, 296, 0), 50, zon_west, nevel=(0.2, 1800),
                                     titel="De Ordekathedraal", zon_box=(-70, 230, -100, 140, 341, 100)),
        "kathedraal_zij": lambda: schoot("kathedraal_zuid", (-10, 290, 120), (66, 275, 0), 62, zon_zuid, nevel=(0.2, 1800),
                                         titel="De Ordekathedraal (zuid)", zon_box=(-20, 230, -60, 140, 341, 100)),
        "kathedraal_binnen": lambda: schoot("kathedraal_binnen", (26, 251, -3), (96, 266, 1), 74, binnen, binnen=True,
                                            achtergrond=(40, 30, 50), gloed=0.9, titel="In de kathedraal",
                                            zon_box=(12, 240, -52, 130, 341, 52),
                                            stralen=dict(sterkte=1.5, max=110.0, n=64, box=(18, 244, -30, 124, 320, 30), buiten=0.0)),
        "universiteit": lambda: schoot("universiteit", (-128, 300, -150), (-36, 240, -55), 50, O.NAMIDDAG, nevel=(0.2, 2000),
                                       titel="De Universiteit", zon_box=(-100, 225, -110, 30, 321, 10)),
        "uni_binnen": lambda: schoot("universiteit_bibliotheek", (-37.5, 238.2, -71.2), (-62, 241.5, -76), 78, binnen, binnen=True,
                                     achtergrond=(40, 30, 50), gloed=0.9, titel="De bibliotheek",
                                     zon_box=(-80, 230, -90, -30, 260, -60)),
        "paleis": lambda: schoot("paleis_raadszaal", (24, 302, 66), (-118, 268, -2), 48, zon_west, nevel=(0.2, 1800),
                                 titel="Raadszaal en paleis", zon_box=(-170, 225, -70, -20, 341, 70)),
        "troonzaal": lambda: schoot("troonzaal", (-126.5, 257, -1.5), (-148, 267, 0.5), 76, binnen, binnen=True,
                                    achtergrond=(40, 30, 50), gloed=0.9, titel="De troonzaal", zon_box=(-158, 245, -50, -113, 300, 50)),
        "raadszaal": lambda: schoot("raadszaal_binnen", (-91, 259, 10), (-108, 262, -2), 72, binnen, binnen=True,
                                    achtergrond=(40, 30, 50), gloed=0.9, titel="De Raadszaal", zon_box=(-115, 250, -20, -85, 290, 20)),
        "academie": lambda: schoot("academie", (-24, 280, 0), (-36, 245, 60), 60, O.NAMIDDAG, nevel=(0.2, 1800),
                                   titel="Academie van de Wolkwacht", zon_box=(-90, 225, 10, 30, 321, 100)),
        "station": lambda: schoot("station", (60, 270, -10), (66, 245, -70), 60, O.NAMIDDAG, nevel=(0.2, 1800),
                                  titel="De luchthaven", zon_box=(20, 225, -110, 120, 321, -40)),
        "overzicht": lambda: schoot("overzicht", (420, 360, 380), (-20, 225, -10), 40, zon_zuid, nevel=(0.3, 2600),
                                    titel="Hoogvads", zon_box=(-200, 180, -120, 200, 341, 205)),
    }
    if shots == ["alles"]:
        shots = list(SHOTS)
    for s in shots:
        log("shot", s)
        SHOTS[s]()


if __name__ == "__main__":
    main()
