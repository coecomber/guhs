"""
The guh sled rail geometry, the same as SleePath.java: used to put sled tracks in structures (Guhland, the guh kermis)
and to draw them in the wiki renders. A piece's anchor is a block on its entry side; facing = travel direction.

Shapes (in the piece's own frame: facing north = travelling towards -z, anchor block centre at the origin):
  straight, curve_left, curve_right, slope      the 4x4 pieces
  drop                                           a steep coaster hill: 8 up over 4 blocks, flat at both ends
  spiral_left, spiral_right                      a corkscrew: half a turn (radius 1.5) going 4 up, comes back alongside
  jump                                           a ramp of 2 blocks, then the sled flies 10 blocks and lands on a rail
"""
import math

SHAPES = ("straight", "curve_left", "curve_right", "slope", "drop", "spiral_left", "spiral_right", "jump")
RIDE_HEIGHT = 0.2
DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
# the jump: ramp length, flight length and the flight's drop-off
JUMP_RAMP, JUMP_LENGTH, JUMP_A = 2.0, 12.0, 0.11


def rotate(v, facing):
    x, y, z = v
    return {"south": (-x, y, -z), "east": (-z, y, x), "west": (z, y, -x)}.get(facing, (x, y, z))


def raw(shape, u):
    """(pos, direction) at u (0..1, not by length) in the piece's own frame."""
    if shape == "curve_right":
        a = math.radians(180 + 90 * u)
        return (2.5 + 2 * math.cos(a), 0, 0.5 + 2 * math.sin(a)), (-math.sin(a), 0, math.cos(a))
    if shape == "curve_left":
        a = math.radians(-90 * u)
        return (-1.5 + 2 * math.cos(a), 0, 0.5 + 2 * math.sin(a)), (math.sin(a), 0, -math.cos(a))
    if shape == "slope":
        return (0.5, 4 * u, 0.5 - 4 * u), (0, 1, -1)
    if shape == "drop":
        return (0.5, 8 * (3 * u * u - 2 * u ** 3), 0.5 - 4 * u), (0, 48 * u * (1 - u), -4)
    if shape in ("spiral_right", "spiral_left"):
        a = math.pi + math.pi * u
        m = 1 if shape == "spiral_right" else -1
        x = 0.0 + 1.5 * math.cos(a)
        return (0.5 + m * (x - 0.5) if m < 0 else x, 4 * u, 0.5 + 1.5 * math.sin(a)), \
               (m * -1.5 * math.pi * math.sin(a), 4, 1.5 * math.pi * math.cos(a))
    if shape == "jump":
        s = JUMP_LENGTH * u
        if s <= JUMP_RAMP:
            return (0.5, 0.25 * s * s, 0.5 - s), (0, 0.5 * s, -1)
        d = s - JUMP_RAMP
        return (0.5, 1 + d - JUMP_A * d * d, 0.5 - s), (0, 1 - 2 * JUMP_A * d, -1)
    return (0.5, 0, 0.5 - 4 * u), (0, 0, -1)


def _norm(v):
    n = math.sqrt(sum(c * c for c in v))
    return tuple(c / n for c in v)


def local(shape, t):
    p, h = raw(shape, t)
    return p, _norm(h)


def length(shape, steps=256):
    pts = [raw(shape, i / steps)[0] for i in range(steps + 1)]
    return sum(math.dist(pts[i], pts[i + 1]) for i in range(steps))


LENGTH = {s: length(s) for s in SHAPES}
ENTRY = {s: raw(s, 0)[0] for s in SHAPES}
EXIT = {s: raw(s, 1)[0] for s in SHAPES}


def world(anchor, facing, shape, t):
    """Rail point (without ride height; relative to the structure) and heading."""
    p, h = local(shape, t)
    r = rotate(p, facing)
    return (anchor[0] + 0.5 + r[0], anchor[1] + r[1], anchor[2] + 0.5 + r[2]), rotate(h, facing)


EXIT_Y = {s: raw(s, 1)[0][1] for s in SHAPES}


def cells(shape):
    """The blocks a piece takes up besides its anchor (as SleePath.cells)."""
    out = []
    if shape in ("straight", "curve_left", "curve_right", "slope"):
        for x in range(-1, 3):
            for z in range(0, -4, -1):
                if x or z:
                    out.append((x, -z if shape == "slope" else 0, z))
        return out
    if shape == "jump":
        return [(x, 0, z) for x in range(-1, 3) for z in (0, -1) if x or z]
    found = set()
    for i in range(1, 400):
        (x, y, z), _ = raw(shape, i / 400)
        for side in (-0.45, 0.45):
            _, h = raw(shape, i / 400)
            flat = _norm((h[0], 0, h[2]))
            sx, sz = -flat[2] * side, flat[0] * side
            top = max(0, math.ceil(EXIT_Y[shape]) - 1)
            found.add((math.floor(x + sx + 0.5), min(top, math.floor(y + 0.001)), math.floor(z + sz + 0.5)))
    found.discard((0, 0, 0))
    return sorted(found)


def nearest_dir(h):
    x, _, z = h
    if abs(x) > abs(z):
        return "east" if x > 0 else "west"
    return "south" if z > 0 else "north"


def attach(end, travel, shape, down=False):
    """(anchor, facing) of a piece continuing from rail point `end` travelling `travel` (down: ridden backwards)."""
    if down:
        exit_dir = nearest_dir(raw(shape, 1)[1])     # north or south in the piece's own frame
        facing = OPPOSITE[travel] if exit_dir == "north" else travel
        o = rotate(EXIT[shape], facing)
    else:
        facing = travel
        o = rotate(ENTRY[shape], facing)
    c = (end[0] - 0.5 - o[0], end[1] - o[1], end[2] - 0.5 - o[2])
    return (round(c[0]), math.floor(c[1] + 0.01), round(c[2])), facing


def build(start_anchor, start_facing, sequence):
    """Pieces for a track: sequence of shapes (prefix 'down_' = ridden downwards; 'down' = a slope going down).
    Returns [(anchor, facing, shape, down)]."""
    pieces = []
    anchor, facing = start_anchor, start_facing
    for i, item in enumerate(sequence):
        down = item == "down" or item.startswith("down_")
        shape = "slope" if item == "down" else item[5:] if item.startswith("down_") else item
        if i:
            end, h = end_of(pieces[-1])
            anchor, facing = attach(end, nearest_dir(h), shape, down)
        pieces.append((anchor, facing, shape, down))
    return pieces


def end_of(piece):
    """Where the sled leaves a piece of a track (rail point) and its heading."""
    anchor, facing, shape, down = piece
    end, h = world(anchor, facing, shape, 0 if down else 1)
    return end, tuple(-c for c in h) if down else h


def start_of(piece):
    anchor, facing, shape, down = piece
    end, h = world(anchor, facing, shape, 1 if down else 0)
    return end, tuple(-c for c in h) if down else h


def place(s, pieces, finish=()):
    """Puts the rail blocks of the pieces into a make_structures Structure (indexes in `finish` get the finish line)."""
    for i, (anchor, facing, shape, _down) in enumerate(pieces):
        s.set(*anchor, "guhs:slee_rail", {"facing": facing, "shape": shape}, {"id": "guhs:slee_rail", "Finish": 1} if i in finish else None)
        for c in cells(shape):
            r = rotate(c, facing)
            off = (round(r[0]), c[1], round(r[2]))
            s.set(anchor[0] + off[0], anchor[1] + off[1], anchor[2] + off[2], "guhs:slee_rail_part",
                  {"dx": str(off[0] + 3), "dy": str(off[1]), "dz": str(off[2] + 3)})


def blocks(anchor, facing, shape):
    out = [anchor]
    for c in cells(shape):
        r = rotate(c, facing)
        out.append((anchor[0] + round(r[0]), anchor[1] + c[1], anchor[2] + round(r[2])))
    return out
