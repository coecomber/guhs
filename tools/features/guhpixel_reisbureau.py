"""
Guhpixel slice "reisbureau" (Java: feature/guhpixel/reisbureau; namespace reisbureau; English: tools/lang/en/c38_px_reisbureau.json).

STUB from the foundation. The PLACEHOLDER that the slice replaces: the structure "reisbureau" (Reisbureau "De
Vadsvakantie"): a kiosk, with its random_spread set and its guaranteed copy (ring 4300-5600, sector 1 of 2).
The slice builds: the real structure + Reisagent-guh questline (3 steps), Reisstempel, Reisbalie, the trip screen, 16
destinations, postcards, 32 souvenirs, the reispas, the Gouden koffertje (DESIGN_PX section 5). Independent of the dimension.
The first destination is spelled exactly "Vadsen bij huize Lingsesdijk 86", its picture "Huize Lingsesdijk 86".
"""
from features import guhpixel_lib as lib

NAME = "reisbureau"
SALT = 20301101
GEGARANDEERD = (4300, 5600)

TEXTS = {
    f"structure.guhs.{NAME}": "Reisbureau \"De Vadsvakantie\"",
    f"structure.guhs.{NAME}.tooltip": "Stuur je guh op vakantie! Hij komt terug met een ansichtkaart en een souvenir.",
}


def build(h):
    lib.kiosk(h, NAME, spacing=80, separation=30, salt=SALT, sector=1, ring=GEGARANDEERD, portaal=False)
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_reisbureau", keys=TEXTS, templates=(NAME,))


def ftb(fq):
    pass
