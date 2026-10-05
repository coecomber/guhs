"""
Guhpixel slice "lobby" (Java: feature/guhpixel/lobby; namespaces lobby, internetcafe; English: tools/lang/en/c31_px_lobby.json).

STUB from the foundation. What is here are PLACEHOLDERS that the lobby slice replaces:
  - the lobby template guhs:guhpixel/lobby (guhpixel_lobby_bouw.py: a flat plaza with every anchor pad and the exit portal);
  - the structure "internetcafe" (the Guh-internetcafé "De Trage Verbinding"): a kiosk with the portal
    guhs:guhpixel_portaal[soort=in], with its random_spread set and its guaranteed copy (ring 4300-5600, sector 0 of 2).
The slice builds: the final lobby, the café + Beheerder-guh, the Welkomstguh + Netwerkkabeltje, the Verkoper-guh (opens the
shop), chat guhs, boards, the lobby parkour with a personal best, 10 golden knabbels per player (DESIGN_PX section 1).
"""
from features import guhpixel_lib as lib
from features import guhpixel_lobby_bouw

CAFE = "internetcafe"
CAFE_SALT = 20301001
GEGARANDEERD = (4300, 5600)      # outside the live server's pregenerated square (half-width 3000: its corners reach 4243)

TEXTS = {
    f"structure.guhs.{CAFE}": "Guh-internetcafé \"De Trage Verbinding\"",
    f"structure.guhs.{CAFE}.tooltip": "Guhs slapen achter oude beige computers. Loop door het grote beeldscherm naar Guhpixel!",
}


def build(h):
    guhpixel_lobby_bouw.build(h)
    lib.kiosk(h, CAFE, spacing=80, separation=30, salt=CAFE_SALT, sector=0, ring=GEGARANDEERD, portaal=True)
    lib.teksten(h, TEXTS)
    selfcheck(h)


def selfcheck(h):
    lib.controleer(h, "guhpixel_lobby", keys=TEXTS, templates=(guhpixel_lobby_bouw.NAME, CAFE))


def ftb(fq):
    pass
