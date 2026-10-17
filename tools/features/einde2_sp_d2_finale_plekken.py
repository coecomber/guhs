"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), slice sp_d2_finale: what this slice adds to the BUILDS of Bikinibodem and Schelpenstad.
Markers only (no block changes; both boxes and self-checks unchanged), all prefixed d2f_:

  bikinibodem   d2f_emmer_1..6        where the player's own bucket-helmet guhs stand (step 10), on the Hoofdstraat, the plein
                                      and the Bellenlaan: the way from the stadspoort to the Bellenkom, none on a resident's spot
                d2f_podium_trap       the field in front of the west steps up to the Bellenkom stage (step 11's hint)
                d2f_bellenkom_poort   the south gate of the Bellenkom (step 11's resume spot, the column of light at 10_b)
                camera d2f_guhfy      the main shot of the song: from the field onto the stage (the kit checks the line)
  schelpenstad  d2f_hasselguh_start   where the player's own David Hasselguh floats beside the pier head when the ride starts
                                      (in the void: los)

Self-check: David Hasselguh swims in a straight arc from d2f_hasselguh_start to bikinibodem/hasselguh_landing, through
blocks (DavidHasselguhEntity.tick: setPos). A rider whose head is in a block would suffocate (playtest 1, item 9), so the
ride is simulated here with the entity's own numbers against the blocks of BOTH builds; a rider cell in a solid block fails.
"""
import math

# the Schelpenstad build sits next to Bikinibodem's (structure placement "bij" +224, +64; the same y): its template
# coordinates + this = Bikinibodem's template coordinates (the road seam: Bikinibodem x 223 = Schelpenstad x -1)
SCHELPENSTAD_BIJ = (224, 0, 64)

EMMERS = [(196, 16, 113), (176, 16, 111), (154, 16, 113), (126, 16, 109), (111, 16, 97), (104, 16, 78)]
PODIUM_TRAP = (96, 16, 48)
BELLENKOM_POORT = (106, 16, 66)
HASSELGUH_START = (242, 14, 55)
LANDING = "hasselguh_landing"

# DavidHasselguhEntity: SNELHEID, REM_AFSTAND, rugHoogte(), the arc (at most 12, 0.12 x the distance), 0.4 a tick up or down
SNELHEID, REM, RUG, BOOG_MAX, BOOG_DEEL, KLEM = 0.55, 6.0, 1.55, 12.0, 0.12, 0.4

_bouw = {}


def bikinibodem(b):
    for i, (x, y, z) in enumerate(EMMERS, 1):
        b.prop(f"d2f_emmer_{i}", x, y, z, wie=f"sp_d2_finale: hier staat een guh met een emmerhelm (stap 10), nr. {i}")
    b.prop("d2f_podium_trap", *PODIUM_TRAP, wie="sp_d2_finale: voor het trapje links vooraan het podium van de Bellenkom")
    b.prop("d2f_bellenkom_poort", *BELLENKOM_POORT, wie="sp_d2_finale: de zuidpoort van de Bellenkom")
    b.camera("d2f_guhfy", (106, 20, 58), (106, 19, 43), zicht=["podium_sponsguh", "podium_trommel"],
             wie="sp_d2_finale: het grote shot van Ik ben een Guhfy Guhber, vanaf het veld op het podium")
    _bouw["bikinibodem"] = b
    _rit_check()


def schelpenstad(b):
    b.prop("d2f_hasselguh_start", *HASSELGUH_START, wie="sp_d2_finale: hier drijft jouw David Hasselguh naast de pierkop als de rit begint",
           los=True)
    _bouw["schelpenstad"] = b
    _rit_check()


def _rit_check():
    """Once both builds are made: the ride home (step 9) must keep its riders' heads out of every block."""
    if len(_bouw) < 2:
        return
    bk, sc = _bouw["bikinibodem"], _bouw["schelpenstad"]
    dx, dy, dz = SCHELPENSTAD_BIJ

    def vast(c):
        x, y, z = c
        if bk.inside(x, y, z) and c in bk.s.blocks:
            return bk.solid_at(c)
        s = (x - dx, y - dy, z - dz)
        return sc.inside(*s) and s in sc.s.blocks and sc.solid_at(s)

    sx, sy, sz = HASSELGUH_START
    start = (sx + dx + 0.5, sy + dy, sz + dz + 0.5)
    lx, ly, lz = bk.plekken[LANDING]["pos"]
    doel = (lx + 0.5, ly, lz + 0.5)
    fout = rit_fouten(start, doel, vast)
    if fout:
        bk.fouten.append(f"sp_d2_finale: de rit op David Hasselguh van d2f_hasselguh_start naar {LANDING} brengt een rijder met zijn "
                         f"hoofd in een blok, bv. {fout[:3]} (verplaats d2f_hasselguh_start)")


def rit_fouten(start, doel, vast):
    """The cells the riders' bodies (the player in front, the two behind, a head 1.62 up) pass that are solid."""
    x, y, z = start
    heel = max(1.0, math.hypot(doel[0] - start[0], doel[2] - start[2]))
    boog = min(BOOG_MAX, math.dist(start, doel) * BOOG_DEEL)
    fout, n = [], 0
    while n < 20000:
        vx, vz = doel[0] - x, doel[2] - z
        rest = math.hypot(vx, vz)
        if rest < 0.6 and abs(doel[1] - RUG - y) < 1.0:
            break
        stap = min(rest, SNELHEID * max(0.25, min(1.0, rest / REM)))
        if rest > 1e-4:
            x, z = x + vx * stap / rest, z + vz * stap / rest
        t = max(0.0, min(1.0, 1.0 - (rest - stap) / heel))
        doel_y = start[1] + (doel[1] - RUG - start[1]) * t + math.sin(math.pi * t) * boog
        y += max(-KLEM, min(KLEM, doel_y - y))
        n += 1
        richting = math.atan2(vz, vx)
        for voor, opzij in ((0.6, 0.0), (-1.4, 0.9), (-1.4, -0.9)):
            px = x + voor * math.cos(richting) - opzij * math.sin(richting)
            pz = z + voor * math.sin(richting) + opzij * math.cos(richting)
            for hoog in (RUG + 0.2, RUG + 0.9, RUG + 1.62):
                c = (math.floor(px), math.floor(y + hoog), math.floor(pz))
                if vast(c) and c not in fout:
                    fout.append(c)
    return fout
