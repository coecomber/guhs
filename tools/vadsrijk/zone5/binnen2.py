"""[U-Z5BINNEN] Hoogvads interiors, round 2 (feedback: "the chandeliers read as wooden scaffolding, the interiors are too dark").

1. Round hanging chandeliers from the new light blocks (vadsrijk_t_z5blokken.py section LICHT, Java zone5/binnen/Z5Licht):
     S   one `kroonluchter` block (a 3-wide round 3D model) on a chain;
     M   a ring of `gouden_kaarsenrand` (thin gold rail, a candle on every node, crystal drops) r 3 on 4 spokes around a
         kroonluchter;
     L   + a smaller crown ring above, crystal pendants under the big ring;
     XL  3 tiers (crown / big ring with a curtain of pendants / lower ring), two kroonluchters in the centre column.
   gotiek.kroonluchter() now builds these (by radius), so every old call site (cathedral, throne room, Raadszaal, library) gets
   them. No slab rings any more, nothing grid-like hanging in the air.
2. Lighter interiors (cathedral, library, Raadszaal, throne room, palace chancellery, academy hall): marble checker floors with
   gold rosettes, wall sconces, standing candelabra, glowing gold in the vault ribs, coloured light pools (carpet mosaics) under
   the stained glass, banners, pew ends, and hidden light on the golden guh statue.
Everything new goes into AIR (floors: only floor blocks are swapped, same height), and stays 2 blocks away from every spot.
"""
import math

from palet import P, trap
import bouwhulp as BH
import gotiek as G

KL = "guhs:kroonluchter"
HANGER = "guhs:kristallen_hanger"
KANDELAAR = "guhs:gouden_kandelaar"
LICHTLIJST = "guhs:gouden_lichtlijst"
VERBORGEN = "minecraft:light[level=15,waterlogged=false]"
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}
RICHTING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
VLOER_OUD = ("guhs:tegel_dambord", "guhs:viltsteen_tegels", "minecraft:dark_oak_planks", "minecraft:spruce_planks")
POEL = ("minecraft:pink_carpet", "minecraft:magenta_carpet", "minecraft:light_blue_carpet", "minecraft:yellow_carpet",
        "minecraft:pink_carpet", "minecraft:purple_carpet", "minecraft:lime_carpet")

_PLEKKEN = []   # (x, y, z) local spots: keep standing things 2 away


def _b(v):
    return "true" if v else "false"


def rand(n=False, e=False, s=False, w=False, kaars=True, kristal=False):
    return (f"guhs:gouden_kaarsenrand[east={_b(e)},kaars={_b(kaars)},kristal={_b(kristal)},north={_b(n)},south={_b(s)},"
            f"west={_b(w)}]")


def naam(W, x, y, z):
    return G.naam(W, x, y, z)


def lucht(W, x, y, z):
    return G.lucht(W, x, y, z)


def vrij_van_plekken(x, z, y=None, d=2):
    for (px, py, pz) in _PLEKKEN:
        if abs(px - x) <= d and abs(pz - z) <= d and (y is None or abs(py - y) <= 4):
            return False
    return True


# ============================================================================================== chandeliers
def ketting(W, x, y_van, z, ymax):
    """A chain from y_van up to the ceiling (or at most ymax)."""
    top = G.plafond(W, x, y_van, z, ymax)
    if top - y_van > 45:
        top = y_van + 6
    for y in range(y_van, top):
        G.zet_lucht(W, x, y, z, G.KETTING)


def ring_cellen(r):
    """The cells of a round ring of radius r around (0, 0), 4-connected, in order around the circle."""
    pts = []
    n = max(16, int(2 * math.pi * r * 4))
    for k in range(n):
        a = 2 * math.pi * k / n
        c = (int(round(r * math.cos(a))), int(round(r * math.sin(a))))
        if not pts or pts[-1] != c:
            pts.append(c)
    if pts[0] == pts[-1]:
        pts.pop()
    uit = []
    for i, c in enumerate(pts):
        if uit and abs(uit[-1][0] - c[0]) + abs(uit[-1][1] - c[1]) == 2:
            a, b = uit[-1], c
            k1, k2 = (a[0], b[1]), (b[0], a[1])
            uit.append(min((k1, k2), key=lambda q: abs(math.hypot(*q) - r)))
        uit.append(c)
    a, b = uit[-1], uit[0]
    if abs(a[0] - b[0]) + abs(a[1] - b[1]) == 2:
        k1, k2 = (a[0], b[1]), (b[0], a[1])
        uit.append(min((k1, k2), key=lambda q: abs(math.hypot(*q) - r)))
    # remove doubles (keep order)
    gezien, schoon = set(), []
    for c in uit:
        if c not in gezien:
            gezien.add(c)
            schoon.append(c)
    return schoon


def ring(W, x, y, z, r, kristal=True, hangers=0, kronen=0, kern=None, ymax=330):
    """A round ring of candle rails radius r at height y around (x, z): a candle on every node, crystal drops on every other one,
    `kronen` round kroonluchter crowns spread over the ring, every `hangers`-th cell a crystal pendant under it, and chains from
    4 points (the crowns, or the axis points) up to the ceiling. No spokes: from below a ring with a cross reads as a grid.
    kern: a block for the centre (or None). Only in air."""
    cellen = ring_cellen(r)
    n = len(cellen)
    verb = {c: set() for c in cellen}
    for i, c in enumerate(cellen):
        for d in (cellen[i - 1], cellen[(i + 1) % n]):
            dx, dz = d[0] - c[0], d[1] - c[1]
            if abs(dx) + abs(dz) == 1:
                verb[c].add((dx, dz))
    kroon = set()
    if kronen:
        for k in range(kronen):
            a = 2 * math.pi * (k + 0.5) / kronen
            kroon.add(min(cellen, key=lambda c: math.hypot(c[0] - r * math.cos(a), c[1] - r * math.sin(a))))
    ophang = sorted(kroon)[:: max(1, len(kroon) // 4)][:4] if kroon else         [c for c in ((int(round(r)), 0), (-int(round(r)), 0), (0, int(round(r))), (0, -int(round(r)))) if c in verb]
    for i, c in enumerate(cellen):
        v = verb[c]
        if not lucht(W, x + c[0], y, z + c[1]):
            continue
        if c in kroon:
            BH.zet(W, x + c[0], y, z + c[1], KL)
            continue
        BH.zet(W, x + c[0], y, z + c[1], rand(n=(0, -1) in v, e=(1, 0) in v, s=(0, 1) in v, w=(-1, 0) in v, kaars=True,
                                             kristal=kristal and i % 2 == 0))
        if hangers and i % hangers == 1:
            for k in range(1, 3 if hangers <= 2 else 2):
                G.zet_lucht(W, x + c[0], y - k, z + c[1], HANGER)
    for c in ophang:
        ketting(W, x + c[0], y + 1, z + c[1], ymax)
    if kern is not None:
        BH.zet(W, x, y, z, kern)


def kroonluchter(W, x, y, z, maat="M", ymax=330):
    """A round hanging chandelier of size S/M/L/XL; y = the height of its main ring (the centre kroonluchter block)."""
    y = int(y)
    if maat == "S":
        BH.zet(W, x, y, z, KL)
        ketting(W, x, y + 1, z, ymax)
        return
    if maat == "M":
        ring(W, x, y, z, 3, kristal=True, kronen=4, kern=KL, ymax=ymax)
        ketting(W, x, y + 1, z, ymax)
        return
    if maat == "L":
        ring(W, x, y, z, 5, kristal=True, hangers=3, kronen=6, kern=KL, ymax=ymax)
        ring(W, x, y + 3, z, 2, kristal=False, kern=KL, ymax=ymax)
        for k in (1, 2):
            G.zet_lucht(W, x, y + k, z, G.KETTING)
        ketting(W, x, y + 4, z, ymax)
        return
    # XL: a big ring r 8 with 8 crowns and a curtain of pendants (y+1), a lower ring r 5 (y-3), a crown ring r 3 (y+5)
    ring(W, x, y + 1, z, 8, kristal=True, hangers=2, kronen=8, kern=KL, ymax=ymax)
    ring(W, x, y - 3, z, 5, kristal=True, hangers=3, kronen=4, kern=KL, ymax=ymax)
    ring(W, x, y + 5, z, 3, kristal=False, kronen=4, kern=KL, ymax=ymax)
    for k in (-2, -1, 0, 2, 3, 4):
        G.zet_lucht(W, x, y + k, z, G.KETTING)
    ketting(W, x, y + 6, z, ymax)


def maat_voor_r(r):
    return "XL" if r >= 8 else "L" if r >= 5 else "M" if r >= 3.5 else "S"


# ============================================================================================== light pieces
def wandluchter(W, x, y, z, facing):
    """A wall sconce at (x, y, z) sticking out to `facing`, only in air with a solid wall (not glass) behind it."""
    dx, dz = RICHTING[facing]
    if not lucht(W, x, y, z):
        return False
    achter = naam(W, x - dx, y, z - dz)
    if achter in ("minecraft:air", "minecraft:cave_air") or "glas" in achter or "glass" in achter or "pane" in achter:
        return False
    BH.zet(W, x, y, z, f"guhs:gouden_wandluchter[facing={facing}]")
    return True


def wandluchter_zoek(W, x, y, z, as_, st, max_stap=20, vaandel_y=None):
    """Walk from (x, y, z) along as_ ('x'/'z') in direction st (+1/-1) to the first wall; put a sconce in the last air cell,
    facing back. vaandel_y: also hang a banner on that wall face at that height."""
    dx, dz = (st, 0) if as_ == "x" else (0, st)
    terug = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(dx, dz)]
    cx, cz = x, z
    for _ in range(max_stap):
        if not lucht(W, cx + dx, y, cz + dz):
            ok = wandluchter(W, cx, y, cz, terug)
            if ok and vaandel_y is not None and lucht(W, cx, vaandel_y, cz) and lucht(W, cx, vaandel_y + 1, cz) and                     not lucht(W, cx + dx, vaandel_y, cz + dz) and not lucht(W, cx + dx, vaandel_y + 1, cz + dz):
                BH.vaandel(W, cx, vaandel_y, cz, terug)
            return ok
        cx, cz = cx + dx, cz + dz
    return False


def kandelaar(W, x, y, z):
    """A standing candelabrum on the floor y-1 (2 blocks of air needed, never on a spot)."""
    if not (lucht(W, x, y, z) and lucht(W, x, y + 1, z)) or lucht(W, x, y - 1, z) or not vrij_van_plekken(x, z, y):
        return False
    BH.zet(W, x, y, z, KANDELAAR)
    return True


def vloer(W, x1, x2, z1, z2, y, rozetten=(), ruit=2, alles=False, binnen=None):
    """Marble checker floor (white / rose, `ruit` wide squares) with gold rosettes at (cx, cz, r); only floor blocks are swapped
    (the carpet runner, stairs and doors stay)."""
    wit, roze, goud = P["wit_glad"], P["marmer_glad"], P["goudblok"]
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if binnen is not None and not binnen(x, z):
                continue
            n = naam(W, x, y, z)
            if not (alles and n not in ("minecraft:air", "minecraft:cave_air") and "wool" not in n and "stairs" not in n
                    and "trap" not in n) and n not in VLOER_OUD:
                continue
            b = wit if ((x // ruit) + (z // ruit)) % 2 == 0 else roze
            for (cx, cz, r) in rozetten:
                dd = math.hypot(x - cx, z - cz)
                if dd <= r + 0.5:
                    a = math.atan2(z - cz, x - cx)
                    q = dd / max(r, 1)
                    if dd < 1.0 or abs(dd - r) < 0.6 or abs(dd - r * 0.5) < 0.5:
                        b = goud
                    elif math.cos(8 * a) > 0.35 and q > 0.5:
                        b = roze
                    else:
                        b = wit
            BH.zet(W, x, y, z, b)


def lichtpoel(W, x, z, y, lengte, breed, langs="z", kleur0=0):
    """A coloured light pool on the floor (carpet mosaic in a pointed-window shape), as if the sun shines through stained glass.
    (x, z) = the end near the window, langs = the axis the pool runs along (+ or - by the sign of lengte)."""
    st = 1 if lengte > 0 else -1
    for i in range(abs(lengte)):
        t = i / max(1, abs(lengte) - 1)
        hw = breed * (1 - max(0.0, t - 0.55) / 0.45 * 0.9) if t > 0.55 else breed
        hw = max(0.0, hw)
        for k in range(-int(math.ceil(hw)), int(math.ceil(hw)) + 1):
            if abs(k) > hw + 0.25:
                continue
            xx, zz = (x + k, z + st * i) if langs == "z" else (x + st * i, z + k)
            if not lucht(W, xx, y, zz) or lucht(W, xx, y - 1, zz) or not vrij_van_plekken(xx, zz, y, d=0):
                continue
            BH.zet(W, xx, y, zz, POEL[(kleur0 + abs(k) * 2 + (i // 2)) % len(POEL)])


def ribben_licht(W, x1, x2, y1, y2, z1, z2, elke=4, y_binnen=0):
    """Hidden light in the vault ribs: some golden rib blocks become glowing gold (gouden_lichtlijst). Only ribs seen from
    INSIDE: the air under the block reaches down below y_binnen (the hall floor area, not an outside roof)."""
    goud = P["goud"]
    n = 0
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                if (x * 7 + y * 3 + z * 5) % elke or W.wget(x, y, z) != goud or not lucht(W, x, y - 1, z):
                    continue
                yy = y - 1
                while yy > y_binnen - 2 and lucht(W, x, yy, z):
                    yy -= 1
                if yy < y_binnen:
                    BH.zet(W, x, y, z, LICHTLIJST)
                    n += 1
    return n


def plafond_licht(W, punten, y_van, ymax, y_min):
    """Glowing gold set flush into the ceiling above each (x, z) (only if that ceiling is at y_min or higher)."""
    for (x, z) in punten:
        top = G.plafond(W, x, y_van, z, ymax)
        if y_min <= top < ymax and not lucht(W, x, top, z):
            BH.zet(W, x, top, z, LICHTLIJST)


# ============================================================================================== the buildings
def kathedraal(W):
    import kathedraal as KA
    X0, Y0, NB, ZB = KA.X0, KA.Y0, KA.NB, KA.ZB
    xa_k, xb_k = KA.X_KRUIS
    cx = (xa_k + xb_k) // 2
    ytop = Y0 + KA.H_SCHIP
    T = KA.TRANSEPT_Z
    xs = list(range(X0 + 10, xa_k - 2, 10)) + list(range(xb_k + 6, KA.X_KOOR - 2, 10))

    def binnen(x, z):
        if x <= KA.X_KOOR:
            return abs(z) <= ZB - 1 or (xa_k <= x <= xb_k and abs(z) <= T - 1)
        return math.hypot(x - KA.X_KOOR, z) <= NB + 0.5
    # floor: marble checker, rosettes in the crossing, in the transept arms and inside the Grote Poort
    vloer(W, X0 + 1, KA.X_KOOR + NB + 1, -T, T, Y0, rozetten=((cx, 0, 8), (cx, T - 10, 5), (cx, -(T - 10), 5), (X0 + 7, 0, 3)),
          binnen=binnen)
    # pews: golden ends
    for x in range(X0 + 12, xa_k - 5, 3):
        for s in (-1, 1):
            for zz in (4, NB - 1):
                n = naam(W, x, Y0 + 1, s * zz)
                if "trap" in n or "stairs" in n:
                    BH.zet(W, x, Y0 + 1, s * zz, trap("goud_trap", "west"))
    # candelabra along the middle aisle (on the gold lines, every other pew row) and around the altar platform
    for i, x in enumerate(range(X0 + 12, xa_k - 5, 3)):
        if i % 2 == 0:
            for s in (-1, 1):
                kandelaar(W, x, Y0 + 1, s * 3)
    for (x, z) in ((cx - 9, -10), (cx - 9, 10), (cx + 9, -10), (cx + 9, 10), (cx - 9, -5), (cx - 9, 5)):
        kandelaar(W, x, Y0 + 1, z)
    # wall sconces: on every pillar (nave face + aisle face, two heights; a banner above the nave one) and on the aisle walls
    for x in xs:
        for s in (-1, 1):
            wandluchter_zoek(W, x, Y0 + 4, 0, "z", s, vaandel_y=Y0 + 7)
            wandluchter_zoek(W, x, Y0 + 12, 0, "z", s)
            wandluchter_zoek(W, x, Y0 + 4, s * (ZB - 2), "z", -s)
    for (a, b) in zip(xs[:-1], xs[1:]):
        if b - a > 12:
            continue
        for x in (a + 3, b - 3):
            for s in (-1, 1):
                wandluchter_zoek(W, x, Y0 + 4, s * (NB + 4), "z", s)
    for s in (-1, 1):      # the transept arms: sconces on their east/west walls
        for z in range(NB + 6, T - 2, 5):
            wandluchter_zoek(W, cx, Y0 + 4, s * z, "x", -1)
            wandluchter_zoek(W, cx, Y0 + 4, s * z, "x", 1)
    # coloured light pools under the stained glass: in the aisles (one per bay) and below the transept roses
    for i, (a, b) in enumerate(zip(xs[:-1], xs[1:])):
        if b - a > 12:
            continue
        for s in (-1, 1):
            lichtpoel(W, (a + b) // 2, s * (ZB - 3), Y0 + 1, -s * 9, 1.5, kleur0=i)
    for s in (-1, 1):
        lichtpoel(W, cx, s * (T - 3), Y0 + 1, -s * 8, 3.0, kleur0=3)
    # glowing gold in the vault ribs (nave, aisles, transept, choir): only ribs seen from inside
    n = ribben_licht(W, X0 + 4, KA.X_KOOR + NB, Y0 + KA.H_BEUK - 9, ytop + 4, -T, T, elke=3, y_binnen=Y0 + 22)
    # the golden guh statue in the apse: candelabra around its plinth, hidden light in front of it, a halo of glowing gold
    bx = KA.X_KOOR + 4
    for (dx, dz) in ((-5, -5), (-5, 5), (-6, -2), (-6, 2)):
        kandelaar(W, bx + dx, Y0 + 1, dz)
    for y in range(Y0 + 4, Y0 + 15, 3):
        for z in (-3, 0, 3):
            G.zet_lucht(W, bx - 6, y, z, VERBORGEN)
    for k in range(28):
        a = 2 * math.pi * k / 28
        yy, zz = int(round(Y0 + 12 + 6.5 * math.sin(a))), int(round(6.5 * math.cos(a)))
        if yy > Y0 + 8:
            G.zet_lucht(W, bx + 5, yy, zz, LICHTLIJST)
    return n


def bibliotheek(W):
    y0, za = 234, -80
    # lighter: a marble checker floor, pale wood for the desks and galleries, a gold band in the shelf walls, glowing gold
    # flush in the ceiling, sconces under and above the galleries
    vloer(W, -61, -37, za + 2, za + 10, y0)
    for x in range(-62, -35):
        for z in range(za + 1, za + 12):
            for y in range(y0 + 1, y0 + 13):
                n = naam(W, x, y, z)
                if n == "minecraft:dark_oak_planks":
                    BH.zet(W, x, y, z, P["goud"] if (z in (za + 1, za + 11) and y == y0 + 4) else "minecraft:pale_oak_planks")
                elif n in ("minecraft:dark_oak_fence", "minecraft:dark_oak_slab"):
                    BH.zet(W, x, y, z, W.wget(x, y, z).replace("dark_oak", "pale_oak"))
    for x in range(-59, -37, 4):
        for y in (y0 + 3, y0 + 9):
            wandluchter(W, x, y, za + 2, "south")
            wandluchter(W, x + 2, y, za + 10, "north")
    plafond_licht(W, [(x, za + 6) for x in range(-61, -36, 3)], y0 + 5, y0 + 22, y0 + 13)


def raadszaal(W):
    y = 256
    vloer(W, -112, -90, -16, 16, y, rozetten=((-98, 0, 5),))
    for x in range(-107, -89, 4):
        for s in (-1, 1):
            wandluchter(W, x, y + 5, s * 16, "north" if s > 0 else "south")
            if lucht(W, x, y + 9, s * 16) and lucht(W, x, y + 10, s * 16):
                BH.vaandel(W, x, y + 9, s * 16, "north" if s > 0 else "south")
    for (x, z) in ((-106, -7), (-106, 7), (-103, -7), (-103, 7)):
        kandelaar(W, x, y + 1, z)
    # a flush coffered ceiling: glowing gold in a grid IN the ceiling (nothing hanging)
    plafond_licht(W, [(x, z) for x in range(-110, -90, 4) for z in range(-14, 15, 4)], y + 6, y + 26, y + 18)


def troonzaal(W):
    y0, xa, xb = 252, -152, -129
    vloer(W, xa, xb, -13, 13, y0, rozetten=((xa + 15, 0, 5),))
    for x in range(xa + 3, xb, 5):
        for s in (-1, 1):
            wandluchter_zoek(W, x, y0 + 5, 0, "z", s)
            wandluchter_zoek(W, x, y0 + 5, s * 10, "z", s)
            kandelaar(W, x + 2, y0 + 1, s * 5)
    # the throne: candelabra before the dais, hidden light in front of the queen
    for z in (-5, 5):
        kandelaar(W, xa + 7, y0 + 1, z)
    for y in (y0 + 6, y0 + 10):
        for z in (-2, 2):
            G.zet_lucht(W, xa + 8, y, z, VERBORGEN)
    plafond_licht(W, [(x, 0) for x in range(xa + 2, xb, 3)], y0 + 20, y0 + 40, y0 + 24)


def kanselarij(W):
    y = 252
    vloer(W, -146, -128, 25, 42, y)
    vloer(W, -133, -129, 13, 24, y)
    kroonluchter(W, -140, y + 5, 29, "S", ymax=y + 8)
    kroonluchter(W, -134, y + 5, 37, "S", ymax=y + 8)
    for x in range(-145, -128, 4):
        wandluchter(W, x, y + 3, 25, "south")
    for z in range(27, 41, 4):
        wandluchter(W, -146, y + 3, z, "east")
        wandluchter(W, -128, y + 3, z, "west")
    for z in range(15, 23, 4):
        wandluchter(W, -133, y + 3, z, "east")
        wandluchter(W, -129, y + 3, z, "west")


def academie(W):
    y0, gx = 234, -35
    vloer(W, -48, -22, 67, 77, y0, rozetten=((gx, 70, 3),))
    for x in (-42, -28):
        kroonluchter(W, x, y0 + 7, 71, "M", ymax=y0 + 11)
    for x in range(-47, -22, 4):
        wandluchter(W, x, y0 + 4, 67, "south")
    for z in (70, 74):
        wandluchter(W, -48, y0 + 4, z, "east")
        wandluchter(W, -22, y0 + 4, z, "west")
    for x in (gx - 4, gx + 4):
        kandelaar(W, x, y0 + 1, 74)


def bouw(W, S, info, log=print):
    """After bouw5 + hoog3: every interior lighter. The spots of info['plekken'] stay free."""
    import plan5 as PL
    _PLEKKEN[:] = [tuple(v) for v in info.get("plekken", {}).values() if v and len(v) == 3 and v[1] is not None]
    _PLEKKEN.extend(tuple(v) for v in PL.plekken().values() if v and v[1] is not None)
    n = kathedraal(W)
    bibliotheek(W)
    raadszaal(W)
    troonzaal(W)
    kanselarij(W)
    academie(W)
    log(f"binnen2 (U-Z5BINNEN): lichte interieurs, {n} gouden lichtribben")
