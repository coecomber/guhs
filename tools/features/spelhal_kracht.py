"""
1.7.0 "Onder de Wol", ronde 45-47 ("Vadskracht in de structuren", ontwerp-guhs170-mijnen.md): the five cave game halls
(grotspellen.py: Schatgraven, Glimwormvangen, Lichtjespuzzel; mijnspellen_bouw.py: the Wisselgrot and the Kegelgrot) show
working vadskracht. The halls are protected (Java feature/spelen/Spelhal), but they only need the early tier: per hall a
Guhrad with a guh ("Vast": no owner) behind a Vadsschakelaar, Vadslampen for the hall's lights and two "scorebord-lampjes"
flanking the host (whose top 3 floats above him), over Guhdraad, a lever in front of the wheel (on = the guh's break) and
signs "Dit is een ... op N vadskracht". The GAMES never depend on it: no game code reads this power, except the Kegelgrot's
pin setter, which only shows it (Java mijnspellen/kegelgrot/Kegelzetter: with power a little show when the pins are set, a
"pauze" sign without; the pins are set either way).

What is NOT done: the Glimwormvangen hall stays dark on purpose (its glimpotjes are its light, driven by the game), so it
only gets the scoreboard lamps; hanging lanterns over the Wisselgrot's rail network and the Kegelgrot's lanes stay (a
Vadslamp stands on a block, it can't hang over a lane).

    hal(S, naam, texts, prefix, ...)   everything of one hall on its template S (before the RAND shift), see there
The Java side checks it in game tests (KrachtProef: GrotSpellenGameTests / MijnspellenGameTests, the hall templates placed,
also turned).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HERE, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HERE, ".."))

from features import onderwol_vadskracht as ov  # noqa: E402
from features import spelhal_bouw as sh  # noqa: E402

# the sign lines every hall shares: (Dutch, English), each at most 16 characters
BORD_RAD = [("Dit is een", "This is a"), ("Guhrad op", "Guh wheel of"), ("15 vadskracht", "15 vads power"), ("Hendel = pauze", "Lever = a break")]
BORD_LAMPEN = [("Vadslampen op", "Vads lamps on"), ("1 vadskracht", "1 vads power"), ("per stuk.", "each."), ("Knopje = aan/uit", "Switch = on/off")]
BORD_SCORE = [("Scorebord-", "Scoreboard"), ("lampjes op", "lamps on"), ("vadskracht!", "vads power!"), ("Spelen: altijd!", "Play: always!")]


def bord(ms, S, x, y, z, facing, lines, texts, prefix, wall=False, hout="cherry"):
    """A sign (a wall sign facing `facing`, or a standing one turned that way) with translatable lines (Dutch, English)."""
    nbt = sh.bord_nbt(ms, lines, texts, prefix)
    if wall:
        S.set(x, y, z, f"minecraft:{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        S.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": {"south": "0", "west": "4", "north": "8", "east": "12"}[facing],
                                                  "waterlogged": "false"}, nbt)


def hal(ms, S, naam, texts, prefix, *, rad, hendel, schakelaar, lampen=(), score=(), binnen, y, verboden=(), borden=(),
        draad=True):
    """One hall's vadskracht on its template S (hall coordinates, before the RAND shift):

      rad         (x, y, z, facing) of the wheel block (bottom middle of the 3 x 3; its spots must be air)
      hendel      (x, y, z, face, facing) of the lever next to the wheel block (face floor or wall)
      schakelaar  (x, y, z, facing, (x, z) of the first wire spot) the Vadsschakelaar against one of the wheel's parts
      lampen      [(x, y, z, facing)] the hall's Vadslampen; score: the same for the scoreboard lamps
      binnen      (x0, z0, x1, z1) where the wire may run; y: its height (the floor you stand on)
      verboden    (x, z) spots the wire must keep off (a game field, a rail network)
      borden      [(x, y, z, facing, lines, key, wall)] the signs
    Lays the shortest Guhdraad from the switch to every lamp that doesn't touch the net yet (ov.leg_pad, never next to a
    door, a lift, a lever...), checks it fits one guh (15 VK). Returns {"wiel", "lampen", "draad"}."""
    b = ov.Plat(S, naam)
    rx, ry, rz, rf = rad
    ov.guhrad(b, rx, ry, rz, rf)
    hx, hy, hz, face, hf = hendel
    assert S.get(hx, hy, hz) in (None, "minecraft:air"), f"{naam}: the lever at {(hx, hy, hz)} needs room"
    S.set(hx, hy, hz, ov.HENDEL, {"face": face, "facing": hf, "powered": "false"})
    sx, sy, sz, sf, start = schakelaar
    assert S.get(sx, sy, sz) in (None, "minecraft:air"), f"{naam}: the switch at {(sx, sy, sz)} needs room, not {S.get(sx, sy, sz)}"
    ov.schakelaar(b, sx, sy, sz, sf)
    alle = list(lampen) + list(score)
    for (lx, ly, lz, lf) in alle:
        assert S.get(lx, ly, lz) in (None, "minecraft:air"), f"{naam}: the lamp at {(lx, ly, lz)} needs room, not {S.get(lx, ly, lz)}"
        ov.lamp(b, lx, ly, lz, lf)
    for (bx, by, bz, bf, lines, key, wall) in borden:
        assert S.get(bx, by, bz) in (None, "minecraft:air"), f"{naam}: the sign at {(bx, by, bz)} needs room, not {S.get(bx, by, bz)}"
        bord(ms, S, bx, by, bz, bf, lines, texts, f"{prefix}.{key}", wall=wall)
    net = set()
    if draad:
        x0, z0, x1, z1 = binnen
        eigen = {(sx, sy, sz)}
        verboden = set(verboden)

        def vrij(x, z):
            return x0 <= x <= x1 and z0 <= z <= z1 and (x, z) not in verboden and ov.draad_vrij(b, x, y, z, eigen=eigen)
        assert vrij(*start), f"{naam}: the wire can't start at {start} (y {y})"
        S.set(start[0], y, start[1], ov.DRAAD)
        net.add(start)
        for (lx, ly, lz, _f) in sorted(alle, key=lambda l: abs(l[0] - start[0]) + abs(l[2] - start[1])):
            if ly != y:
                continue   # (a lamp at another height: the caller wires it itself)
            van = [(lx + dx, lz + dz) for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(p in net for p in van):
                continue
            net.update(ov.leg_pad(b, van, net, y, vrij))
    assert len(alle) <= 15, f"{naam}: {len(alle)} lamps ask more than one guh gives (15 VK)"
    return {"wiel": (rx, ry, rz), "lampen": [l[:3] for l in alle], "draad": net, "plat": b}


def verbind(S):
    """The block states of the Guhdraad in S (call it after everything is placed, before saving)."""
    ov.verbind(ov.Plat(S, "hal"))
