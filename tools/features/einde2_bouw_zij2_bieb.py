"""
einde2, slice bouw_zij2: the Bibliotheek van Wan Shi Guh (Korstwoestijn). Build only: the trade of knowledge, the new
Guhboeken and the fox-guhs' errands are a later phase, which reads the markers (also the scene spot of the wink with
Mew-guh: knipoog_mew_*).

Half sunk, as in the story: a slab of cracked desert floats in the void, and all you see on it is the tip of a tower
with a green copper roof, four little corner turrets and a dome, poking out of the crumb sand. The library itself hangs
UNDER the slab: a square tower of 45 x 45, six storeys deep, its windows looking out into the void. You climb in through
the window of the tip, and go down:

    the tip (the ring gallery around the well, the book stairs down)
    zaal_1 .. zaal_5   five storeys of halls around the well, each with its own subject, a boekentrap in the north-east
                       corner down to the next; bridges cross the well on the second and the fourth
    the uilenzaal      the bottom: the dais of Wan Shi Guh under the well, the kennisbalie, the big bookcase behind him
    the planetarium    a domed round room off zaal_1 (its dome is the one you saw outside), with the wheel of sun and moon

    maak(h) -> (the kit.Bouw, its problems); bouw(h) raises SystemExit on a problem
"""
import math
import random

import features.einde2_kit as kit
from features import einde2_bouw_zij2_gereedschap as g
from features import guheinde_bouw as gb

NAAM = "bibliotheek_wan_shi_guh"
W, H, D = 112, 96, 112
Y = 146                                   # the world y of template y 0
S = 66                                    # the desert: the layer of the surface blocks
T0 = 34                                   # the tower: x and z of its low corner; 45 wide (local 0..44)
VLOEREN = [54, 45, 36, 27, 18]            # zaal_1 .. zaal_5: the y of each floor layer
UIL = 9                                   # the floor of the uilenzaal
TIP = 63                                  # the floor of the tip (also the roof of zaal_1)
ZAND, KORST = "guhs:kruimelzand", "guhs:gebarsten_korst"
MUUR = [("guhs:kaaskorst_stenen", 58), ("guhs:gebarsten_kaaskorst_stenen", 22), ("guhs:aangevreten_kaaskorst_stenen", 6), ("sandstone", 14)]
VLOER, VLOER2, RING = "smooth_sandstone", "cut_sandstone", "guhs:kaaskorst_stenen"
KOPER, KOPERTRAP, KOPERPLAAT = "oxidized_cut_copper", "oxidized_cut_copper_stairs", "oxidized_cut_copper_slab"
KAST = "bookshelf"
HEK = "dark_oak_fence"
PLAN_C = (22, 56)                         # the planetarium: its middle (x, z)
ZAALNAMEN = [("Zaal 1", "Knabbelkunde", "Alles over", "de knabbel"), ("Zaal 2", "Vadskunst", "Liggen, rollen", "en niksen"),
             ("Zaal 3", "Njegologie", "Waarom njeg?", "Daarom njeg."), ("Zaal 4", "Mika-zaken", "Fluisteren!", "Ze lezen mee."),
             ("Zaal 5", "Kaaskaarten", "Elk gat", "op de kaart")]


def X(u):
    return T0 + u


def eiland(b):
    duin = g.Ruis(NAAM + "/duin", 11.0)

    def top(x, z, m):
        return S

    grond = g.eiland(b, 56, 56, 50, 50, S, 13, KORST, [("guhs:kaaskorst", 80), (KORST, 20)], NAAM, rafel=0.1, top_fn=top, onder="guhs:kaaskorst")
    return grond, duin


def toren_schil(b):
    """Empties the tower's box, then its walls (two thick, with windows where it hangs free under the slab), its floors
    with the well in the middle, and the stepped foot."""
    rng = random.Random(kit.seed(NAAM + "/muur"))
    for u in range(-1, 46):
        for w in range(-1, 46):
            for y in range(2, TIP + 1):
                b.s.blocks.pop((X(u), y, X(w)), None)
    for u in range(0, 45):
        for w in range(0, 45):
            muur = u < 2 or u > 42 or w < 2 or w > 42
            buiten = u in (0, 44) or w in (0, 44)
            langs = u if w in (0, 1, 43, 44) else w
            for y in range(UIL, TIP + 1):
                if not muur:
                    continue
                if y in VLOEREN[1:] + [UIL] and buiten:
                    b.set(X(u), y, X(w), KOPER)                              # (a green band at every floor)
                elif y < 50 and 8 <= langs <= 36 and langs % 6 == 4 and any(f + 3 <= y <= f + 6 for f in VLOEREN[1:] + [UIL]) \
                        and not (u < 2 and w < 2) and not ((u < 2 or u > 42) and (w < 2 or w > 42)):
                    b.set(X(u), y, X(w), "orange_stained_glass")           # (a tall window, through both layers)
                else:
                    b.set(X(u), y, X(w), gb.pick(rng, MUUR))
    # pilasters on the outside between the windows, where the tower hangs free
    for k in range(1, 44, 6):
        for y in range(UIL, 50):
            for (u, w) in ((k, -1), (k, 45), (-1, k), (45, k)):
                b.set(X(u), y, X(w), "cut_sandstone" if y % 9 else "chiseled_sandstone")
    # floors (the roof of zaal_1 = the floor of the tip is one of them)
    for f in VLOEREN + [UIL, TIP]:
        for u in range(2, 43):
            for w in range(2, 43):
                put = 17 <= u <= 27 and 17 <= w <= 27 and f != UIL
                if put:
                    continue
                gang = 13 <= u <= 31 and 13 <= w <= 31
                b.set(X(u), f, X(w), RING if gang else (VLOER2 if (u % 6 == 0 or w % 6 == 0) else VLOER))
    # the foot: an upside-down stepped pyramid with a lamp at its point
    for y, inzet in ((8, 0), (7, 2), (6, 5), (5, 9), (4, 14), (3, 19)):
        for u in range(inzet, 45 - inzet):
            for w in range(inzet, 45 - inzet):
                rand = u in (inzet, 44 - inzet) or w in (inzet, 44 - inzet)
                b.set(X(u), y, X(w), KOPER if rand else gb.pick(rng, MUUR))
    for y in (2, 1):
        b.set(X(22), y, X(22), "sea_lantern" if y == 1 else KOPER)
    for u, w in ((21, 22), (23, 22), (22, 21), (22, 23)):
        b.set(X(u), 2, X(w), "sea_lantern")


def balustrade(b, f, open_=()):
    """The railing around the well on the floor at y f (a fence on the ledge), with gaps at open_ = [(u, w)]."""
    for u in range(16, 29):
        for w in range(16, 29):
            if u in (16, 28) or w in (16, 28):
                if (u, w) not in open_:
                    b.set(X(u), f + 1, X(w), HEK)


def trappenhuis(b):
    """The boekentrappen in the north-east corner: from each zaal a flight of eight steps down to the next (the last one
    into the uilenzaal), on bookcases, in two lanes that take turns."""
    alle = VLOEREN + [UIL]
    for i in range(len(VLOEREN)):
        f, onder = alle[i], alle[i + 1]
        baan = (2, 3) if i % 2 == 0 else (10, 11)
        for k in range(8):
            u, y = 33 + k, f - 1 - k
            for w in baan:
                b.s.blocks.pop((X(u), f, X(w)), None)                       # (the hole in the floor of this zaal)
                g.trap(b, X(u), y, X(w), "dark_oak_stairs", "west")
                for yy in range(onder + 1, y):
                    b.set(X(u), yy, X(w), KAST)
        # the railing of the hole: along its open side(s) and at its far end
        zijden = (4,) if i % 2 == 0 else (9, 12)
        for u in range(33, 41):
            for w in zijden:
                b.set(X(u), f + 1, X(w), HEK)
        for w in baan:
            b.set(X(41), f + 1, X(w), HEK)
        naam = ZAALNAMEN[i + 1] if i + 1 < len(ZAALNAMEN) else ("Uilenzaal", "Stil zijn.", "Hij hoort", "alles. Njeg.")
        b.sign(X(42), onder + 2, X(baan[0] + (2 if i % 2 == 0 else -2)), "west", list(naam))


def zaal(b, i, f):
    """One storey: bookcases along every wall and in rows in the four halls, pillars between gallery and halls, three
    corner rooms (the fourth is the stairwell), lanterns, and the cabinet where new Guhboeken appear."""
    rng = random.Random(kit.seed(f"{NAAM}/zaal{i}"))
    boek = lambda: KAST if rng.random() < 0.93 else "chiseled_bookshelf"   # noqa: E731
    cbs = {"facing": "south", **{f"slot_{k}_occupied": "true" if k % 2 else "false" for k in range(6)}}

    def kast(u, y, w):
        n = boek()
        b.set(X(u), y, X(w), n, cbs if n != KAST else None)

    # along the outer walls, four high, with a ledge on top
    for k in range(3, 42):
        for (u, w) in ((k, 2), (k, 42), (2, k), (42, k)):
            if (u > 31 and w < 13) or b.get(X(u), f + 1, X(w)) is not None or (i == 1 and u == 2 and 21 <= w <= 23):
                continue                                                     # (the stairwell keeps its walls bare; the planetarium's door)
            for y in range(f + 1, f + 5):
                kast(u, y, w)
            g.plaat(b, X(u), f + 5, X(w), "dark_oak_slab")
    # pillars on the line between the gallery and the halls
    for k in range(12, 33, 4):
        for (u, w) in ((k, 12), (k, 32), (12, k), (32, k)):
            for y in range(f + 1, f + 9):
                b.set(X(u), y, X(w), "chiseled_sandstone" if y in (f + 1, f + 8) else "cut_sandstone")
            b.set(X(u), f + 5, X(w), "sea_lantern")
    # rows of bookcases in the halls (north, south, west, east), three high, a lantern on every second row
    for r, k in enumerate((16, 20, 24, 28)):
        for lang in range(4, 10):
            for (u, w) in ((k, lang), (k, 44 - lang), (lang, k), (44 - lang, k)):
                for y in range(f + 1, f + 4):
                    kast(u, y, w)
        if r % 2 == 0:
            for (u, w) in ((k, 9), (k, 35), (9, k), (35, k)):
                b.set(X(u), f + 4, X(w), "lantern", gb.lamp(False))
    # lanterns under the ceiling
    for u in range(6, 40, 8):
        for w in range(6, 40, 8):
            if not (16 <= u <= 28 and 16 <= w <= 28) and b.get(X(u), f + 9, X(w)) is not None and not (u > 31 and w < 13):
                g.hanglamp(b, X(u) + 1, f + 8, X(w) + 1, ketting=2)
    for (u, w) in ((14, 14), (30, 14), (14, 30), (30, 30), (22, 14), (22, 30), (14, 22), (30, 22)):
        if b.get(X(u), f + 9, X(w)) is not None and b.get(X(u), f + 8, X(w)) is None and not (i == 1 and (u, w) == (22, 14)):
            g.hanglamp(b, X(u), f + 8, X(w), ketting=1, lamp="soul_lantern")
    # the three corner rooms, each floor a different use
    hoeken = [((3, 3), 0), ((3, 33), 1), ((33, 33), 2)]
    for (u0, w0), soort in hoeken:
        soort = (soort + i) % 3
        if soort == 0:                                                       # the reading room: tables, chairs, a rug
            for du in range(2, 8):
                for dw in range(2, 8):
                    b.set(X(u0 + du), f + 1, X(w0 + dw), "red_carpet" if (du + dw) % 2 else "orange_carpet")
            for du, dw in ((3, 3), (6, 6)):
                b.set(X(u0 + du), f + 1, X(w0 + dw), "guhs:guh_tafel", {"facing": "north"})
                b.set(X(u0 + du + 1), f + 1, X(w0 + dw), "guhs:guh_stoel", {"facing": "west"})
                b.set(X(u0 + du - 1), f + 1, X(w0 + dw), "guhs:guh_stoel", {"facing": "east"})
            b.set(X(u0 + 6), f + 1, X(w0 + 3), "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
        elif soort == 1:                                                     # the scroll store: barrels, a chart table
            for du, dw in ((2, 2), (3, 2), (2, 3), (7, 7), (7, 6), (6, 7), (2, 7), (7, 2)):
                g.ton(b, X(u0 + du), f + 1, X(w0 + dw), "up")
            g.ton(b, X(u0 + 2), f + 2, X(w0 + 2), "up")
            b.set(X(u0 + 4), f + 1, X(w0 + 4), "cartography_table")
            b.set(X(u0 + 5), f + 1, X(w0 + 4), "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
            b.set(X(u0 + 4), f + 2, X(w0 + 4), "lantern", gb.lamp(False))
        else:                                                                # the snooze corner: cushions, for a long book
            for du, dw, kleur, facing in ((2, 2, "yellow", "south"), (4, 2, "orange", "south"), (2, 4, "brown", "east"), (6, 6, "yellow", "north"),
                                          (4, 6, "orange", "north")):
                b.set(X(u0 + du), f + 1, X(w0 + dw), f"guhs:{kleur}_zitzak", {"facing": facing})
            b.set(X(u0 + 7), f + 1, X(w0 + 2), "guhs:guh_kristal_lamp")
            b.set(X(u0 + 3), f + 1, X(w0 + 4), "guhs:guh_taart", {"bites": "2"})
    # the cabinet for new Guhboeken: in the middle of the south wall, in a golden frame, a lectern in front of it
    guhboek_kast(b, i, f, f"de gouden kast van zaal {i}: hier verschijnen nieuwe Guhboeken")
    b.zone(f"zaal_{i}", X(2), f + 1, X(2), X(42), f + 8, X(42), wie=f"{ZAALNAMEN[i - 1][0]}: {ZAALNAMEN[i - 1][1]}")


def guhboek_kast(b, i, f, wie):
    for y in range(f + 1, f + 5):
        for u in (21, 23):
            b.set(X(u), y, X(42), "gold_block")
    for u in (21, 22, 23):
        b.set(X(u), f + 4, X(42), "gold_block")
        g.plaat(b, X(u), f + 5, X(42), "dark_oak_slab")
    b.set(X(22), f + 1, X(42), KAST)
    b.set(X(22), f + 2, X(42), KAST)
    b.set(X(22), f + 3, X(42), "sea_lantern")
    b.set(X(22), f + 1, X(40), "lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    b.prop(f"guhboek_kast_{i}", X(22), f + 2, X(42), wie=wie)


def bruggen(b):
    """A bridge across the well on the second floor (north-south) and on the fourth (east-west)."""
    for f, ns in ((VLOEREN[1], True), (VLOEREN[3], False)):
        open_ = set()
        for a in range(17, 28):
            for d in range(20, 25):
                u, w = (d, a) if ns else (a, d)
                b.set(X(u), f, X(w), "dark_oak_planks" if d in (20, 24) else VLOER2)
                if d in (20, 24):
                    b.set(X(u), f + 1, X(w), HEK)
        for d in (21, 22, 23):
            for a in (16, 28):
                open_.add((d, a) if ns else (a, d))
        balustrade(b, f, open_)
        for a in (19, 25):
            u, w = (20, a) if ns else (a, 20)
            b.set(X(u), f + 2, X(w), "lantern", gb.lamp(False))
    for f in (VLOEREN[0], VLOEREN[2], VLOEREN[4], TIP):
        balustrade(b, f)


def tip(b, grond):
    """What sticks out of the sand: the tower's tip with its window (the way in), its copper roof, the four turret tops."""
    rng = random.Random(kit.seed(NAAM + "/tip"))
    for u in range(12, 33):
        for w in range(12, 33):
            wand = u in (12, 32) or w in (12, 32)
            for y in range(TIP + 1, 81):
                if wand:
                    hoek = u in (12, 32) and w in (12, 32)
                    langs = u if w in (12, 32) else w
                    if hoek:
                        b.set(X(u), y, X(w), "cut_sandstone")
                    elif 70 <= y <= 75 and langs % 4 == 2 and not (w == 32 and 20 <= u <= 24):
                        b.set(X(u), y, X(w), "orange_stained_glass_pane")
                    elif y in (69, 77):
                        b.set(X(u), y, X(w), KOPER)
                    else:
                        b.set(X(u), y, X(w), gb.pick(rng, MUUR))
                else:
                    b.s.blocks.pop((X(u), y, X(w)), None)
    # the window in the south face: three wide, four high, the sill level with the sand
    for u in (21, 22, 23):
        for y in range(S + 1, S + 5):
            b.s.blocks.pop((X(u), y, X(32)), None)
        b.set(X(u), S, X(32), "chiseled_sandstone")
        b.set(X(u), S + 5, X(32), KOPER)
    b.prop("ingang_top", X(22), S, X(32), wie="de vensterbank van het raam in de torenspits: hier klim je naar binnen")
    # inside: three steps down from the sill to the gallery
    for k, w in enumerate((31, 30)):
        for u in (21, 22, 23):
            g.trap(b, X(u), S - 1 - k, X(w), "sandstone_stairs", "south")
            for yy in range(TIP + 1, S - 1 - k):
                b.set(X(u), yy, X(w), "cut_sandstone")
    # the roof: a stepped pyramid of green copper with a golden point
    for k in range(0, 12):
        y, lo, hi = 81 + k, 11 + k, 33 - k
        if lo > hi:
            break
        for u in range(lo, hi + 1):
            for w in range(lo, hi + 1):
                rand = u in (lo, hi) or w in (lo, hi)
                if rand:
                    f = "south" if w == lo else "north" if w == hi else "east" if u == lo else "west"
                    hoek = u in (lo, hi) and w in (lo, hi)
                    if hoek or lo == hi:
                        b.set(X(u), y, X(w), KOPER)
                    else:
                        g.trap(b, X(u), y, X(w), KOPERTRAP, f)
                elif u in (lo + 1, hi - 1) or w in (lo + 1, hi - 1):
                    b.set(X(u), y, X(w), KOPER)                              # (a second ring inside: the roof is one piece)
    b.set(X(22), 93, X(22), "gold_block")
    b.set(X(22), 94, X(22), "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # the rope they came down by, in the story: a chain from the roof down the whole well
    for y in range(15, 91):
        if b.get(X(22), y, X(22)) is None:
            b.set(X(22), y, X(22), "chain", {"axis": "y", "waterlogged": "false"})
    for (u, w) in ((13, 13), (31, 13), (13, 31), (31, 31)):      # (a standing light in every corner of the tip)
        for y in range(TIP + 1, TIP + 4):
            b.set(X(u), y, X(w), "polished_blackstone_wall")
        b.set(X(u), TIP + 4, X(w), "lantern", gb.lamp(False))
    # the book stairs from the tip down to zaal_1: along the north side of the gallery
    f = VLOEREN[0]
    for k in range(8):
        u, y = 17 + k, TIP - 1 - k
        for w in (13, 14):
            b.s.blocks.pop((X(u), TIP, X(w)), None)
            g.trap(b, X(u), y, X(w), "dark_oak_stairs", "west")
            for yy in range(f + 1, y):
                b.set(X(u), yy, X(w), KAST)
    for u in range(17, 25):
        b.set(X(u), TIP + 1, X(15), HEK)
    for w in (13, 14):
        b.set(X(25), TIP + 1, X(w), HEK)
    b.sign(X(15), TIP + 2, X(13), "east", ["Naar beneden", "Zes zalen.", "Fluisteren.", "Njeg snoepen."], wall=False)
    b.s.blocks.pop((X(15), TIP + 2, X(13)), None)
    b.sign(X(16), TIP + 1, X(15), "south", ["Naar beneden", "Zes zalen.", "Fluisteren.", "Njeg snoepen."], wall=False)
    b.sign(X(26), f + 2, X(13), "east", list(ZAALNAMEN[0]), wall=False)
    b.s.blocks.pop((X(26), f + 2, X(13)), None)
    b.sign(X(26), f + 1, X(13), "east", list(ZAALNAMEN[0]), wall=False)
    b.plek("vosguh_1", X(26), TIP + 1, X(29), yaw=180.0, wie="een vos-guh, in de torenspits bij de ingang", kind="VOSGUH")
    # the four corner turrets: only their tops show above the sand
    for (u0, w0) in ((0, 0), (40, 0), (0, 40), (40, 40)):
        for u in range(u0, u0 + 5):
            for w in range(w0, w0 + 5):
                rand = u in (u0, u0 + 4) or w in (w0, w0 + 4)
                for y in range(TIP + 1, 71):
                    if rand or y < S:
                        b.set(X(u), y, X(w), gb.pick(rng, MUUR) if y != 69 else KOPER)
                    else:
                        b.set(X(u), y, X(w), "sea_lantern" if y == 69 and (u, w) == (u0 + 2, w0 + 2) else "sandstone")
        for k in range(3):
            for u in range(u0 + k, u0 + 5 - k):
                for w in range(w0 + k, w0 + 5 - k):
                    b.set(X(u), 71 + k, X(w), KOPER)


def uilenzaal(b):
    """The bottom: one great hall. Under the well the dais of Wan Shi Guh with the kennisbalie; behind him the bookcase
    the wink with Mew-guh needs; shelves six high around."""
    f = UIL
    rng = random.Random(kit.seed(NAAM + "/uil"))
    for k in range(3, 42):
        for (u, w) in ((k, 2), (k, 42), (2, k), (42, k)):
            if (u > 31 and w < 13) or b.get(X(u), f + 1, X(w)) is not None:
                continue
            for y in range(f + 1, f + 7):
                b.set(X(u), y, X(w), KAST if rng.random() < 0.95 else "sea_lantern")
            g.plaat(b, X(u), f + 7, X(w), "dark_oak_slab")
    for k in range(12, 33, 4):
        for (u, w) in ((k, 12), (k, 32), (12, k), (32, k)):
            for y in range(f + 1, f + 9):
                b.set(X(u), y, X(w), "chiseled_sandstone" if y in (f + 1, f + 8) else "cut_sandstone")
            b.set(X(u), f + 4, X(w), "sea_lantern")
    for r, k in enumerate((16, 20, 24, 28)):
        for lang in range(4, 10):
            for (u, w) in ((k, lang), (lang, k), (44 - lang, k)):
                for y in range(f + 1, f + 6):
                    b.set(X(u), y, X(w), KAST)
        for (u, w) in ((k, 9), (9, k), (35, k)):
            b.set(X(u), f + 6, X(w), "lantern", gb.lamp(False))
    # the dais: round, one step up, a red rug with a golden rim
    for u in range(15, 30):
        for w in range(15, 30):
            d = math.hypot(u - 22, w - 22)
            if d <= 6.4:
                b.set(X(u), f + 1, X(w), "gold_block" if d > 5.4 else ("red_terracotta" if (u + w) % 2 else "orange_terracotta"))
            elif d <= 7.5:
                g.plaat(b, X(u), f + 1, X(w), "smooth_sandstone_slab")
    # the bookcase behind him (north): seven wide, three high, real bookshelves, two free above
    for u in range(19, 26):
        for y in range(f + 2, f + 5):
            b.set(X(u), y, X(18), KAST)
    b.prop("knipoog_mew_plank", X(22), f + 4, X(18), wie="de bovenkant van de boekenkast achter Wan Shi Guh: hier gooit Mew-guh de boeken af", los=True)
    b.pad("knipoog_mew_zweef", [(X(19), f + 5, X(18)), (X(25), f + 5, X(18))], wie="de lijn waarlangs Mew-guh boven de kast zweeft")
    b.plek("wan_shi_guh", X(22), f + 2, X(20), yaw=0.0, wie="de uil-guh Wan Shi Guh, op zijn verhoging onder de schacht", kind="WAN_SHI_GUH")
    # the kennisbalie in front of him: a desk of dark oak, one block high
    for u in range(20, 25):
        b.set(X(u), f + 2, X(23), "dark_oak_planks" if u != 22 else "lectern", None if u != 22 else {"facing": "south", "has_book": "false", "powered": "false"})
    b.prop("kennis_balie", X(22), f + 2, X(23), wie="de kennisbalie: hier ruil je een stukje kennis")
    b.camera("knipoog_mew", (X(22), f + 4, X(29)), (X(22), f + 3, X(20)), zicht=["wan_shi_guh", "knipoog_mew_plank"],
             wie="de camera van de knipoog met Mew-guh: kijkt naar de uil, met de kast achter hem")
    b.sign(X(20), f + 2, X(24), "south", ["Kennisbalie", "Ruil kennis", "voor kennis.", "Njeg voor kaas."])
    b.sign(X(24), f + 2, X(24), "south", ["Regel 1:", "kennis delen.", "Regel 2:", "terugbrengen!"])
    b.plek("vosguh_3", X(27), f + 2, X(25), yaw=90.0, wie="een vos-guh, naast de verhoging in de uilenzaal", kind="VOSGUH")
    b.zone("uilenzaal", X(2), f + 1, X(2), X(42), f + 8, X(42), wie="de uilenzaal: de onderste zaal, het rijk van Wan Shi Guh")
    guhboek_kast(b, 6, f, "de gouden kast van de uilenzaal: hier verschijnen nieuwe Guhboeken")
    # four standing lights around the dais, reading tables in the south, the owl mosaic in the floor of the north
    for (u, w) in ((14, 14), (30, 14), (14, 30), (30, 30)):
        for y in range(f + 1, f + 4):
            b.set(X(u), y, X(w), "polished_blackstone_wall")
        b.set(X(u), f + 4, X(w), "lantern", gb.lamp(False))
    for u in (10, 22, 34):
        b.set(X(u), f + 1, X(37), "guhs:guh_tafel", {"facing": "north"})
        b.set(X(u) + 1, f + 1, X(37), "guhs:guh_stoel", {"facing": "west"})
        b.set(X(u) - 1, f + 1, X(37), "guhs:guh_stoel", {"facing": "east"})
    for u in range(6, 40, 8):
        for w in range(6, 40, 8):
            if not (14 <= u <= 30 and 14 <= w <= 30) and not (u > 31 and w < 13):
                g.hanglamp(b, X(u) + 1, f + 8, X(w) + 1, ketting=2)
    oog = ["..YYY...YYY..",
           ".YKKKY.YKKKY.",
           ".YKWKY.YKWKY.",
           ".YKKKY.YKKKY.",
           "..YYY.O.YYY..",
           "......O......"]
    for j, rij in enumerate(oog):
        for i, ch in enumerate(rij):
            blok = {"Y": "yellow_terracotta", "K": "black_terracotta", "W": "white_terracotta", "O": "orange_terracotta"}.get(ch)
            if blok:
                b.set(X(16 + i), f, X(5 + j), blok)


def planetarium(b):
    """The domed round room west of zaal_1: a night sky in the dome, the wheel of sun and moon in the middle. Its dome is
    the one that sticks out of the sand."""
    cx, cz = PLAN_C
    f = VLOEREN[0]
    R = 8
    for x in range(cx - R - 1, cx + R + 2):
        for z in range(cz - R - 1, cz + R + 2):
            d = math.hypot(x - cx, z - cz)
            if d > R + 1.3:
                continue
            for y in range(f - 1, f + 17):
                hoog = y - (f + 7)                                           # (above the wall: the dome)
                rr = math.hypot(d, max(0, hoog)) if hoog > 0 else d
                if y < f:
                    b.set(x, y, z, "cut_sandstone")
                elif y == f and d <= R:
                    b.set(x, y, z, "blue_terracotta" if (int(math.degrees(math.atan2(z - cz, x - cx)) + 360) // 30) % 2 else "light_blue_terracotta")
                elif rr > R + 1.3:
                    continue
                elif rr > R + 0.3:
                    b.set(x, y, z, KOPER if hoog > 0 else "cut_sandstone")
                elif rr > R - 0.7:
                    if hoog > 0:
                        ster = (x * 7 + y * 13 + z * 5) % 11 == 0
                        b.set(x, y, z, "sea_lantern" if ster else ("glowstone" if (x * 3 + z * 11 + y) % 37 == 0 else "black_concrete"))
                    else:
                        b.set(x, y, z, "cut_sandstone" if y != f + 4 else "chiseled_sandstone")
                else:
                    b.s.blocks.pop((x, y, z), None)
    # the corridor to zaal_1 through the tower's west wall
    for x in range(cx + R - 1, X(2)):
        for z in (cz - 1, cz, cz + 1):
            b.set(x, f, z, RING)
            for y in range(f + 1, f + 4):
                b.s.blocks.pop((x, y, z), None)
            b.set(x, f + 4, z, "cut_sandstone")
        for z in (cz - 2, cz + 2):
            for y in range(f, f + 5):
                if b.get(x, y, z) is None or x < T0:
                    b.set(x, y, z, "cut_sandstone")
    # the wheel: a pedestal with the lodestone, a ring of gold and copper in the floor around it, two levers
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            d = math.hypot(x - cx, z - cz)
            if 2.6 <= d <= 3.6:
                b.set(x, f, z, "gold_block" if (int(math.degrees(math.atan2(z - cz, x - cx)) + 360) // 45) % 2 else KOPER)
            elif d < 1.2:
                b.set(x, f, z, "yellow_terracotta")
    b.set(cx, f + 1, cz, "chiseled_sandstone")
    b.set(cx, f + 2, cz, "lodestone")
    b.set(cx, f + 3, cz, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    b.prop("planetarium_wiel", cx, f + 2, cz, wie="het wiel van het planetarium: de magneetsteen op de sokkel (draai zon en maan)")
    b.plek("professor", cx + 3, f + 1, cz + 3, yaw=135.0, wie="professor Zei-guh, bij het wiel van het planetarium", kind="PROFESSOR_ZEI_GUH")
    b.zone("planetarium", cx - R, f + 1, cz - R, cx + R, f + 15, cz + R, wie="het planetarium")
    b.sign(cx - 1, f + 1, cz + 2, "south", ["Het Wiel", "Draai: zon,", "maan, knabbel.", "Njeg te hard!"], wall=False)
    for hoek in range(0, 360, 60):
        x, z = int(round(cx + math.cos(math.radians(hoek)) * 6)), int(round(cz + math.sin(math.radians(hoek)) * 6))
        if abs(z - cz) > 1 or x < cx:
            b.set(x, f + 1, z, "guhs:guh_kristal_lamp")
    b.set(cx - 5, f + 1, cz - 3, "guhs:guh_telescoop", {"facing": "east"})


def woestijn(b, grond, duin):
    """The surface: the landing pad, the flagstone path to the window, dunes of crumb sand, dry bushes, the professor's
    abandoned dig, and the planetarium's dome coming up through the sand."""
    rng = random.Random(kit.seed(NAAM + "/woestijn"))
    for x in range(51, 62):
        for z in range(92, 103):
            rand = x in (51, 61) or z in (92, 102)
            b.set(x, S, z, "chiseled_sandstone" if rand else "cut_sandstone")
            for y in (S - 1, S - 2):
                if b.get(x, y, z) is None:
                    b.set(x, y, z, "guhs:kaaskorst")
    for x, z in ((56, 97), (55, 97), (57, 97), (56, 96), (56, 98)):
        b.set(x, S, z, "pink_concrete")
    b.zone("aankomst", 51, S + 1, 92, 61, S + 9, 102, wie="het landingsplein in de woestijn")
    b.prop("aankomst_speler", 56, S + 1, 97)
    b.prop("uitzicht", 56, S + 24, 138, wie="van het zuiden, hoog: de spits in het zand, en daaronder de toren in de leegte")
    for z in range(X(33), 92):
        for x in (55, 56, 57):
            if rng.random() < 0.8:
                b.set(x, S, z, "cut_sandstone" if rng.random() < 0.7 else "sandstone")
    # dunes: low mounds of crumb sand (on the crust: it cannot fall), layers on their flanks; bushes
    vrij = lambda x, z: not (50 <= x <= 62 and z >= X(32)) and not (X(11) <= x <= X(33) and X(11) <= z <= X(33))   # noqa: E731
    for (x, z), t in sorted(grond.items()):
        if b.get(x, t, z) != KORST or b.get(x, t + 1, z) is not None or not vrij(x, z):
            continue
        n = duin(x, z)
        if n > 0.74:
            b.set(x, t + 1, z, ZAND)
            if n > 0.86:
                b.set(x, t + 2, z, ZAND)
        elif n > 0.62:
            b.set(x, t + 1, z, "guhs:kruimelzandlaag", {"layers": str(1 + int((n - 0.62) / 0.12 * 5))})
        elif n < 0.3 and rng.random() < 0.02:
            b.set(x, t + 1, z, "guhs:dorre_korststruik")
    # the dig: a pit two deep next to the south-west turret, a tent, crates, signs
    px, pz = 40, 84
    for x in range(px, px + 5):
        for z in range(pz, pz + 4):
            b.set(x, S, z, gb.AIR)
            b.set(x, S - 1, z, ZAND if (x + z) % 3 else KORST)
            b.set(x, S - 2, z, "guhs:kaaskorst")
    for x in range(px, px + 5):
        g.trap(b, x, S, pz + 3, "sandstone_stairs", "south")
    for x in (px - 3, px - 2, px - 1):
        for k, kleur in enumerate(("white_wool", "white_wool")):
            b.set(x, S + 2, pz + k, kleur)
        b.set(x, S + 2, pz + 2, "orange_wool")
    for x in (px - 3, px - 1):
        for z in (pz, pz + 2):
            b.set(x, S + 1, z, "spruce_fence")
    g.ton(b, px - 2, S + 1, pz + 3)
    b.set(px - 2, S + 2, pz + 3, "lantern", gb.lamp(False))
    b.sign(px + 5, S + 1, pz + 1, "east", ["Opgraving van", "prof. Zei-guh", "Njeg graven:", "er wordt gelezen"], wall=False)
    b.sign(58, S + 1, 90, "south", ["Bibliotheek", "van Wan Shi Guh", "Ingang: het", "raam. Echt."], wall=False)
    # half-buried pillars along the path
    for z, hoog in ((72, 3), (78, 2), (84, 4)):
        for x in (53, 59):
            for y in range(S + 1, S + 1 + hoog):
                b.set(x, y, z, "cut_sandstone" if y % 2 else "sandstone")
            b.set(x, S + 1 + hoog, z, "lantern", gb.lamp(False))


def in_duin(b, c):
    """A light for the pass, in the dunes: the crumb sand (or its layer) under the dark spot becomes a glowing nugget."""
    onder = (c[0], c[1] - 1, c[2])
    bl = b.s.blocks.get(onder)
    if bl is None or bl[0] not in (ZAND, "guhs:kruimelzandlaag"):
        return None
    b.set(*onder, "ochre_froglight", {"axis": "y"})
    return onder, 15


def _bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    grond, duin = eiland(b)
    toren_schil(b)
    planetarium(b)
    for i, f in enumerate(VLOEREN, 1):
        zaal(b, i, f)
    uilenzaal(b)
    trappenhuis(b)
    bruggen(b)
    tip(b, grond)
    woestijn(b, grond, duin)
    b.plek("vosguh_2", X(22), VLOEREN[2] + 1, X(11), yaw=0.0, wie="een vos-guh, met een stapel boeken in zaal 3", kind="VOSGUH")
    b.connect()
    start = (56, S + 1, 97)
    buiten = lambda c: c[1] > TIP + 1 and not (X(12) < c[0] < X(32) and X(12) < c[2] < X(32))     # noqa: E731
    n, over = g.verlicht(b, start, lambda c: [g.in_vloer("ochre_froglight"), in_duin] if buiten(c) else
                         [g.in_vloer("guhs:guh_kristal_lamp"), g.op_vloer("lantern", gb.lamp(False)), g.in_wand("sea_lantern")])
    b.laatste_licht = n
    return b, start, over


VERWACHT = ["ingang_top", "uilenzaal", "wan_shi_guh", "kennis_balie", ("vosguh", 3), ("guhboek_kast", 6), ("zaal", 5), "planetarium",
            "planetarium_wiel", "professor", "knipoog_mew_plank", "knipoog_mew_zweef", "knipoog_mew_camera"]


def eigen_check(b):
    """The bookcase of the wink: real bookshelves behind the owl, two free above; every zaal reachable is the kit's job
    (its npc and prop spots)."""
    p = []
    plank = b.plekken["knipoog_mew_plank"]["pos"]
    uil = b.plekken["wan_shi_guh"]["pos"]
    if b.get(*plank) != "minecraft:bookshelf" or any(b.get(plank[0], plank[1] + k, plank[2]) is not None for k in (1, 2)):
        p.append("de kast van de knipoog: geen boekenkast of geen twee blokken vrij erboven")
    if not (plank[2] < uil[2] and abs(plank[0] - uil[0]) <= 3):
        p.append("de kast van de knipoog staat niet achter Wan Shi Guh")
    kasten = sum(1 for bl in b.s.blocks.values() if bl[0] in ("minecraft:bookshelf", "minecraft:chiseled_bookshelf"))
    if kasten < 3000:
        p.append(f"maar {kasten} boekenkasten: dat is geen bibliotheek")
    return p


def maak(h):
    b, start, over = _bouw(h)
    problemen = b.check(start, VERWACHT)
    problemen += [f"{len(over)} donkere plekken kregen geen licht, bv. {over[:4]}"] if over else []
    problemen += eigen_check(b)
    return b, problemen


def bouw(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"{NAAM}: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    print(f"{NAAM}: self-check ok {b.stats}, {b.laatste_licht} lights added")
    return b
