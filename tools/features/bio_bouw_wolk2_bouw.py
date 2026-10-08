"""
biomes3 slice "bouw-wolk2": the three templates (not in FEATURES; bio_bouw_wolk2.py calls build()).

  regenboogbrug      a floating island (west) and a cloud bank (east) with a rainbow of regenboogblok between them, three
                     wide and one thick; a thin streak of cloud under the whole span (you never fall further than onto
                     cloud); the pot of kaasknabbels on the east bank.
  wolkenkasteeltje   a big cloud bank with a small half-ruined castle of white cloud with pink turrets; in its hall the
                     sleeping giant on a cloud bed, his hoard behind him; a beanstalk around a wolkenlift comes up through
                     the forecourt.
  bliksemsmidse      a dark thundercloud on a white puff, hollow: the forge of the smid-guh.

All three are guhs:bio_plek structures of kind "lucht": the START JIGSAW (the anchor) lands `hoogte` blocks above the
meadow in the middle of the start chunk, and the air around it is kept free of natural islands. The way up is a 3 x 3
wolkenlift column that stands on the meadow two blocks from the anchor's column (the meadow's height is only known
there), next to it a wolkenstroom down; so every template is `hoogte` blocks taller than its building.

Template coordinates: x east, y up, z south. DEK is the y of the walking floor's blocks (= the anchor's y).
The numbers the Java side needs (Kasteel.java, Brug.java, Smidse.java) are in MATEN and checked by its game tests.
"""
import math

from features import bio_lib as lib
from features import bio_wereld_eiland as eiland

WIT, ROZE, DONKER = "guhs:wolkenblok_wit", "guhs:wolkenblok_roze", "guhs:bliksemsmidse_wolk"
REGEN = "guhs:regenboogblok"
LUCHT = "minecraft:air"
ZIJ = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
LINKS = {"north": "west", "west": "south", "south": "east", "east": "north"}     # a quarter turn counterclockwise

# hoogte: the anchor's height above the meadow; ruimte: the air kept free around it; spacing/separation in chunks
STRUCTUREN = {
    "regenboogbrug": {"hoogte": 36, "ruimte": 38, "spacing": 14, "separation": 6, "salt": 21500901},
    "wolkenkasteeltje": {"hoogte": 102, "ruimte": 34, "spacing": 16, "separation": 7, "salt": 21500911},
    "bliksemsmidse": {"hoogte": 24, "ruimte": 20, "spacing": 9, "separation": 4, "salt": 21500921},
}
# template positions the Java side mirrors (filled by the builders)
MATEN = {}


def trap(facing, half="bottom", shape="straight"):
    return {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"}


def plaat(soort="bottom"):
    return {"type": soort, "waterlogged": "false"}


def anker(s, x, y, z, naam, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def bord(h, s, x, y, z, facing, stam, regels, muur=False):
    """A cherry sign with Dutch lines (keys sign.guhs.<stam>.<slug>)."""
    import sign_text
    B = h.ms.Byte
    voor = {"messages": sign_text.messages(f"sign.guhs.{stam}", regels), "color": "black", "has_glowing_text": B(0)}
    leeg = {"messages": sign_text.messages(f"sign.guhs.{stam}", ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": B(1), "front_text": voor, "back_text": leeg}
    if muur:
        s.set(x, y, z, "minecraft:cherry_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        s.set(x, y, z, "minecraft:cherry_sign", {"rotation": {"south": "0", "west": "4", "north": "8", "east": "12"}[facing], "waterlogged": "false"}, nbt)


def is_wolk(naam):
    return naam in (WIT, ROZE, DONKER)


def rond_af(s, vlak=(), onder=True):
    """
    Softens every bank of cloud in the template: where a step shows, a cloud stair (or an inner corner) leans against it,
    above and (upside down) below. `vlak`: columns (x, z) whose top must stay as it is (floors you build on).
    """
    vol = {p: n for p, (n, _props, _nbt) in s.blocks.items() if is_wolk(n)}
    vlak = set(vlak)
    for (x, y, z), naam in sorted(vol.items()):
        for dy, half in ((1, "bottom"), (-1, "top")):
            if dy == -1 and not onder:
                continue
            p = (x, y + dy, z)
            if p in s.blocks or not s.inside(*p) or (dy == 1 and (x, z) in vlak):
                continue
            buren = [f for f, (dx, dz) in ZIJ.items() if (x + dx, y + dy, z + dz) in vol]
            stuk = naam + "_trap"
            if len(buren) == 1:
                s.set(*p, stuk, trap(buren[0], half))
            elif len(buren) == 2 and {buren[0], buren[1]} not in ({"east", "west"}, {"north", "south"}):
                a, b = buren if LINKS[buren[0]] == buren[1] else buren[::-1]
                s.set(*p, stuk, trap(a, half, "inner_left"))
            elif len(buren) >= 3:
                s.set(*p, naam)


def bol(s, cx, cy, cz, rx, ry, rz, blok, plat=0.55, alleen_leeg=True):
    """A blob of cloud, flatter underneath. Returns its cells."""
    uit = set()
    for x in range(int(cx - rx - 1), int(cx + rx + 2)):
        for z in range(int(cz - rz - 1), int(cz + rz + 2)):
            dh = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if dh > 1:
                continue
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                dy = (y - cy) / (ry if y >= cy else ry * plat)
                if dh + dy * dy <= 1 and s.inside(x, y, z) and not (alleen_leeg and (x, y, z) in s.blocks):
                    s.set(x, y, z, blok)
                    uit.add((x, y, z))
    return uit


def dek(s, cx, cz, rx, rz, y, blok=WIT, dik=2, zaad="dek"):
    """A level floor of cloud with a wavy rim: `dik` blocks thick, its top blocks at y; clears cloud above it. Returns its columns."""
    rnd = lib.rng(f"bio_bouw_wolk2/{zaad}")
    p = [float(rnd.uniform(0, 6.283)) for _ in range(3)]
    kolommen = set()
    for x in range(int(cx - rx - 2), int(cx + rx + 3)):
        for z in range(int(cz - rz - 2), int(cz + rz + 3)):
            a = math.atan2(z - cz, x - cx)
            golf = 1 + 0.07 * math.sin(3 * a + p[0]) + 0.05 * math.sin(5 * a + p[1]) + 0.04 * math.sin(8 * a + p[2])
            if ((x - cx) / (rx * golf)) ** 2 + ((z - cz) / (rz * golf)) ** 2 <= 1 and s.inside(x, y, z):
                for yy in range(y - dik + 1, y + 1):
                    s.set(x, yy, z, blok)
                yy = y + 1
                while (x, yy, z) in s.blocks and is_wolk(s.blocks[(x, yy, z)][0]):
                    del s.blocks[(x, yy, z)]
                    yy += 1
                kolommen.add((x, z))
    return kolommen


def liften(h, s, x_op, z_op, x_neer, z_neer, y_dek, hoogte, kijk, rand=ROZE):
    """
    The way up and the way down: a 3 x 3 wolkenlift column around (x_op, z_op) from the meadow to two above the deck (it
    puffs you off to `kijk`), and a 3 x 3 wolkenstroom down around (x_neer, z_neer) through a hole in the deck. The meadow's
    top block is at y_dek - hoogte under the anchor; both pads lie one above it, on a foot of cloud.
    """
    grond = y_dek - hoogte
    for (cx, cz, omlaag) in ((x_op, z_op, False), (x_neer, z_neer, True)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                binnen = abs(dx) <= 1 and abs(dz) <= 1
                for y in range(grond + 2, y_dek + 3):
                    # the column's own air: nothing of the bank may stand in it (the hole through the deck)
                    if binnen and (cx + dx, y, cz + dz) in s.blocks:
                        del s.blocks[(cx + dx, y, cz + dz)]
                if binnen:
                    s.set(cx + dx, grond, cz + dz, WIT)
        eiland.lift(h, s, cx, cz, grond + 1, y_dek if omlaag else y_dek + 2, omlaag=omlaag, kijk=kijk)
        # a pink rim round the hole in the deck, so you see where the stream is
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if max(abs(dx), abs(dz)) == 2 and (cx + dx, y_dek, cz + dz) in s.blocks and is_wolk(s.blocks[(cx + dx, y_dek, cz + dz)][0]):
                    s.set(cx + dx, y_dek, cz + dz, rand)
    return grond + 1


# =====================================================================================================================
# 1. the rainbow bridge
# =====================================================================================================================
BRUG_STIJGING = 9          # blocks the arc rises above its feet
BRUG_X0, BRUG_X1 = 13, 47  # the arc's first and last column
BRUG_Z = 14                # its north lane (three lanes: z 14, 15, 16)


def boog_hoogtes(n, stijging):
    """
    The walking height of the arc's n columns in HALF blocks above its feet: a parabola, made walkable. A column an even
    number of half blocks high is a block (or a stair when its lower neighbour is a whole block lower), an odd one a slab;
    so two neighbours never differ by more than a whole block, and where they differ a whole block the higher one is even.
    """
    c = (n - 1) / 2.0
    t = [max(2, int(round(2 * (1 + stijging * (1 - ((i - c) / (c + 1)) ** 2))))) for i in range(n)]
    t[0] = t[-1] = 2                                   # the feet: one stair up from the bank
    for _ in range(4 * n):
        klaar = True
        for i in range(n - 1):
            a, b = (i, i + 1) if i < c else (i + 1, i)   # a is the outer (lower) one of the pair
            if t[b] - t[a] > 2:
                t[b] = t[a] + 2
                klaar = False
            if t[b] - t[a] == 2 and t[b] % 2 == 1:
                t[b] -= 1
                klaar = False
        if klaar:
            break
    return t


def boog(s, x0, z0, y_voet, hoogtes):
    """Three lanes of regenboogblok along x. y_voet: the y of the block the feet stand ON (the bank's top block)."""
    n = len(hoogtes)
    for i, hier in enumerate(hoogtes):
        links = hoogtes[i - 1] if i > 0 else 0
        rechts = hoogtes[i + 1] if i < n - 1 else 0
        for baan, strook in enumerate(("a", "b", "c")):
            x, z = x0 + i, z0 + baan
            if hier % 2 == 0:
                y = y_voet + hier // 2
                if links == hier - 2 and rechts >= hier - 1:
                    s.set(x, y, z, REGEN + "_trap", {**trap("east"), "strook": strook})
                elif rechts == hier - 2 and links >= hier - 1:
                    s.set(x, y, z, REGEN + "_trap", {**trap("west"), "strook": strook})
                else:
                    s.set(x, y, z, REGEN, {"axis": "x", "strook": strook})
            else:
                y = y_voet + (hier + 1) // 2
                s.set(x, y, z, REGEN + "_plaat", {"axis": "x", "strook": strook, **plaat("bottom")})
                s.set(x, y - 1, z, REGEN + "_plaat", {"axis": "x", "strook": strook, **plaat("top")})


def regenboogbrug(h):
    naam = "regenboogbrug"
    hoogte = STRUCTUREN[naam]["hoogte"]
    Y = hoogte
    s = h.Structure((61, hoogte + 24, 31))
    # the west end: a floating island; the east end: a bank of cloud with a level top
    tops = eiland.eiland(h, s, (9, Y, 15), 8, 7, zaad=naam, vorm="rond")
    eiland.wolk(h, s, (51, Y - 2, 15), 8, 3, 7, zaad=naam + "_oost", kleur="wit")
    oost = dek(s, 51, 15, 7.5, 6.5, Y, zaad=naam + "_oost")
    # a pink puff behind the pot, so the end of the rainbow is a place
    bol(s, 56, Y + 1, 12, 3.2, 2.2, 3.0, ROZE)
    bol(s, 55, Y + 1, 19, 2.6, 1.8, 2.4, WIT)
    # the streak of cloud under the span: whoever steps off the side lands on cloud
    for (cx, cz, rx, rz) in ((17, 15, 7, 5), (25, 14, 7, 4.5), (33, 16, 7, 5), (41, 15, 7, 4.5), (47, 15, 5, 5)):
        bol(s, cx, Y - 6, cz, rx, 1.3, rz, WIT, plat=0.8)
    # the deck south of the island where the lifts arrive, and a slope of cloud from the streak up to it
    zuid = dek(s, 22, 24, 12.5, 3.6, Y, zaad=naam + "_zuid")
    for i in range(7):
        bol(s, 30 - i * 1.2, Y - 6 + i, 20 + i * 0.35, 3.0, 1.0, 2.6, WIT, plat=0.9)
    # clouds hugging the island's rim under the foot of the arc
    bol(s, 15, Y - 2, 15, 4.5, 2.0, 4.5, WIT)
    bol(s, 6, Y - 1, 21, 4.0, 1.8, 3.0, WIT)
    op = (30, 24)
    neer = (24, 24)
    pad_y = liften(h, s, op[0], op[1], neer[0], neer[1], Y, hoogte, kijk="west")
    rond_af(s, vlak=oost | zuid | set(tops))
    # the arc: its feet stand on the island (x 13) and on the east bank (x 47)
    for x in (BRUG_X0, BRUG_X1):
        for z in range(BRUG_Z, BRUG_Z + 3):
            for y in range(Y + 1, Y + 4):
                s.blocks.pop((x, y, z), None)
            if (x, Y, z) not in s.blocks:
                s.set(x, Y, z, WIT)
    hoogtes = boog_hoogtes(BRUG_X1 - BRUG_X0 + 1, BRUG_STIJGING)
    boog(s, BRUG_X0, BRUG_Z, Y, hoogtes)
    # a doorstep of cloud at both feet: from soft cloud (which you sink into a little) the first stair is just too high
    for x in (BRUG_X0 - 1, BRUG_X1 + 1):
        for z in range(BRUG_Z, BRUG_Z + 3):
            for y in range(Y + 1, Y + 4):
                s.blocks.pop((x, y, z), None)
            if (x, Y, z) not in s.blocks:
                s.set(x, Y, z, WIT)
            s.set(x, Y + 1, z, WIT + "_plaat", plaat())
    # a small blossom tree on the island, beside the foot of the rainbow
    for y in range(Y + 1, Y + 4):
        s.set(6, y, 11, "guhs:guhbloesem_log", {"axis": "y"})
    for (dx, dy, dz) in [(a, b, c) for a in range(-2, 3) for b in range(0, 3) for c in range(-2, 3)]:
        if abs(dx) + abs(dz) + (dy * 1.4) <= 3.1 and (6 + dx, Y + 3 + dy, 11 + dz) not in s.blocks:
            s.set(6 + dx, Y + 3 + dy, 11 + dz, "guhs:guhbloesem_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
    # nothing may stand in the walker's way on the arc
    for i, hier in enumerate(hoogtes):
        for z in range(BRUG_Z, BRUG_Z + 3):
            top = Y + (hier + 1) // 2
            for y in range(top + 1, top + 4):
                s.blocks.pop((BRUG_X0 + i, y, z), None)
    # the pot at the end, and its sign
    pot = (53, Y + 1, 15)
    s.set(*pot, "guhs:regenboogbrug_pot", {"facing": "west"})
    bord(h, s, 52, Y + 1, 17, "west", naam, ["Einde van de", "regenboog.", "Geen pot goud:", "een pot KAAS!"])
    s.set(53, Y + 1, 13, "guhs:wolkenlamp")
    # the anchor, in the deck two blocks west of the lift's column
    anker(s, op[0] - 3, Y, op[1], naam, WIT)
    s.save(naam)
    MATEN[naam] = {"anker": (op[0] - 3, Y, op[1]), "pot": pot, "boog_x": (BRUG_X0, BRUG_X1), "boog_z": BRUG_Z, "dek": Y,
                   "west": (11, Y + 1, 15), "oost": (49, Y + 1, 15), "lift": op + (pad_y,), "stroom": neer + (pad_y,), "hoogtes": hoogtes}
    return s


# =====================================================================================================================
# 2. the cloud castle
# =====================================================================================================================
def beanstalk(s, cx, cz, y0, y1):
    """A twisting stalk round the 3 x 3 lift column at (cx, cz): two strands on the ring just outside it, with leaves."""
    ring = [(-2, -2), (-1, -2), (0, -2), (1, -2), (2, -2), (2, -1), (2, 0), (2, 1), (2, 2), (1, 2), (0, 2), (-1, 2), (-2, 2), (-2, 1), (-2, 0), (-2, -1)]
    rnd = lib.rng("bio_bouw_wolk2/bonenstaak")
    for y in range(y0, y1 + 1):
        for streng in (0, 8):
            for k in (0, 1):
                dx, dz = ring[(y + streng + k) % 16]
                s.set(cx + dx, y, cz + dz, "minecraft:moss_block")
        # a leaf now and then: a big dripleaf on a short stem of leaves, pointing away from the stalk
        if y % 5 == 2 and y < y1 - 3:
            dx, dz = ring[(y + (0 if rnd.random() < 0.5 else 8)) % 16]
            ux, uz = (dx > 0) - (dx < 0), (dz > 0) - (dz < 0)
            if abs(dx) == 2 and abs(dz) == 2:
                uz = 0
            if (ux, uz) == (0, 0):
                continue
            kant = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}.get((ux, uz))
            if kant is None:
                continue
            s.set(cx + dx + ux, y, cz + dz + uz, "minecraft:flowering_azalea_leaves" if rnd.random() < 0.3 else "minecraft:azalea_leaves",
                  {"distance": "1", "persistent": "true", "waterlogged": "false"})
            s.set(cx + dx + 2 * ux, y, cz + dz + 2 * uz, "minecraft:big_dripleaf", {"facing": kant, "tilt": "none", "waterlogged": "false"})


def toren(s, cx, cz, y0, hoog, dak=4, ruine=0, zaad="toren"):
    """A round turret of pink cloud (5 wide), a white band, a pointed roof; `ruine`: this many blocks eaten off its top, no roof."""
    rnd = lib.rng(f"bio_bouw_wolk2/{zaad}")
    rond = [(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if abs(dx) + abs(dz) < 4]
    for (dx, dz) in rond:
        rand = abs(dx) == 2 or abs(dz) == 2 or abs(dx) + abs(dz) == 3
        top = y0 + hoog - (int(rnd.integers(0, ruine + 1)) if ruine else 0)
        for y in range(y0, top):
            band = y in (y0 + hoog - 2,) and not ruine
            s.set(cx + dx, y, cz + dz, WIT if band else ROZE if rand or y == y0 else LUCHT)
        if not ruine and rand and (dx + dz) % 2 == 0:
            s.set(cx + dx, y0 + hoog, cz + dz, ROZE + "_plaat", plaat())           # little teeth round the top
    if ruine:
        return
    # the roof: rings of pink stairs closing to a point
    for i in range(dak):
        r = 2 - i * 0.62
        y = y0 + hoog + i
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                d = math.hypot(dx, dz)
                if d <= r + 0.45:
                    s.set(cx + dx, y, cz + dz, ROZE if d <= r - 0.55 else ROZE + "_plaat", None if d <= r - 0.55 else plaat())
    s.set(cx, y0 + hoog + dak, cz, WIT + "_plaat", plaat())


def wolkenkasteeltje(h):
    naam = "wolkenkasteeltje"
    hoogte = STRUCTUREN[naam]["hoogte"]
    Y = hoogte
    s = h.Structure((49, hoogte + 24, 45))
    rnd = lib.rng("bio_bouw_wolk2/kasteel")
    # --- the bank: billows from the island builder, a level top to build on, a few puffs on its rim ----------------------
    eiland.wolk(h, s, (24, Y - 3, 21), 21, 4.5, 19, zaad=naam, kleur="wit")
    vloer = dek(s, 24, 21, 20, 18.5, Y, dik=3, zaad=naam)
    for (cx, cz, r, kleur) in ((5, 12, 3.4, ROZE), (44, 30, 3.8, ROZE), (42, 8, 3.0, WIT), (7, 33, 3.2, WIT), (38, 37, 2.8, ROZE)):
        bol(s, cx, Y + 1, cz, r, r * 0.6, r * 0.9, kleur)
    # --- the lifts through the forecourt, the beanstalk round the one that goes up ----------------------------------------
    op, neer = (24, 34), (33, 31)
    pad_y = liften(h, s, op[0], op[1], neer[0], neer[1], Y, hoogte, kijk="north")
    rond_af(s, vlak=vloer)
    beanstalk(s, op[0], op[1], pad_y - 2, Y + 3)
    for dx in range(-2, 3):                                                            # the stalk's top: a whole ring, the crown sits on it
        for dz in range(-2, 3):
            if max(abs(dx), abs(dz)) == 2:
                s.set(op[0] + dx, Y + 3, op[1] + dz, "minecraft:moss_block")
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2), (-2, 0), (2, 0), (0, 2)):   # the stalk's crown: leaves round the hole, the north side open
        s.set(op[0] + dx, Y + 4, op[1] + dz, "minecraft:flowering_azalea_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
    for dx in (-1, 0, 1):                                                              # (no strand in the way where the lift puffs you off)
        for y in range(Y + 1, Y + 4):
            s.blocks.pop((op[0] + dx, y, op[1] - 2), None)

    # --- the castle: outer walls x 16..32, z 4..22; the hall inside is x 17..31, z 5..21 --------------------------------
    X0, X1, Z0, Z1 = 16, 32, 4, 22
    MUUR = 8                                                                             # wall height above the floor
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            rand = x in (X0, X1) or z in (Z0, Z1)
            s.set(x, Y, z, ROZE if (x + z) % 2 == 0 and not rand else WIT)                # a soft checkered floor
            for y in range(Y + 1, Y + MUUR + 4):
                s.blocks.pop((x, y, z), None)
            if not rand:
                continue
            # the ruin: the wall is eaten away in long soft bites (never below 4: nobody walks in over it)
            langs = x if z in (Z0, Z1) else z
            beet = 2.2 * math.sin(langs * 0.55 + (1.3 if z == Z0 else 2.9 if z == Z1 else 4.1 if x == X0 else 0.4)) + 1.4 * math.sin(langs * 1.3 + x * 0.2)
            top = MUUR - max(0, int(round(beet)))
            if z == Z1 and 21 <= x <= 27:
                top = MUUR                                                               # (the gate's wall stands)
            top = max(4, top)
            for y in range(Y + 1, Y + top + 1):
                s.set(x, y, z, WIT)
            if top == MUUR and langs % 2 == 0:
                s.set(x, Y + MUUR + 1, z, WIT)                                           # battlements
            elif top < MUUR:
                s.set(x, Y + top + 1, z, WIT + "_plaat", plaat())                        # a soft crumbled edge
    # windows: tall slits in the long walls
    for z in (8, 12, 16):
        for x in (X0, X1):
            for y in (Y + 3, Y + 4):
                if (x, y + 2, z) in s.blocks:
                    s.blocks.pop((x, y, z), None)
    # the gate in the south wall: three wide, four high, a pink arch over it
    for x in (23, 24, 25):
        for y in range(Y + 1, Y + 5):
            s.blocks.pop((x, y, Z1), None)
    s.set(23, Y + 4, Z1, ROZE + "_trap", trap("west", "top"))
    s.set(25, Y + 4, Z1, ROZE + "_trap", trap("east", "top"))
    for x in (22, 23, 24, 25, 26):
        s.set(x, Y + 5, Z1, ROZE)
    for x in (22, 26):
        for y in range(Y + 1, Y + 5):
            s.set(x, y, Z1, ROZE)
    # the turrets: three whole, the north-east one fell in
    toren(s, X0, Z1, Y + 1, 11, zaad="zw")
    toren(s, X1, Z1, Y + 1, 12, zaad="zo")
    toren(s, X0, Z0, Y + 1, 13, zaad="nw")
    toren(s, X1, Z0, Y + 1, 7, ruine=3, zaad="no")
    for (x, z) in ((34, 2), (35, 4), (33, 1), (36, 6)):                                  # its rubble on the cloud outside
        s.set(x, Y + 1, z, ROZE if (x + z) % 2 else ROZE + "_plaat", None if (x + z) % 2 else plaat())
    # what is left of the roof: slabs of cloud over the north end, ragged
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z0 + 7):
            rafel = 2.0 * math.sin(x * 0.7) + 1.2 * math.sin(x * 1.9 + 1)
            if z - Z0 <= 3.5 + rafel and (x, Y + MUUR, z) not in s.blocks:
                s.set(x, Y + MUUR, z, WIT + "_plaat", plaat("top"))

    # --- the hall -------------------------------------------------------------------------------------------------------
    # the giant's cloud bed: x 19..30, z 10..17, one high; a pink pillow under his head (west), a blanket rolled up at his feet
    for x in range(19, 31):
        for z in range(10, 18):
            s.set(x, Y + 1, z, ROZE if x in (19, 30) or z in (10, 17) else WIT)          # a pink frame round the mattress
    for z in range(11, 17):
        s.set(19, Y + 2, z, ROZE + "_plaat", plaat())
        s.set(20, Y + 2, z, ROZE + "_plaat", plaat())
    for z in range(10, 18):
        s.set(30, Y + 2, z, ROZE)
        s.set(29, Y + 2, z, ROZE + "_trap", trap("east"))
    for (x, z) in ((19, 10), (19, 17), (30, 10), (30, 17)):                               # bed posts
        s.set(x, Y + 2, z, WIT)
        s.set(x, Y + 3, z, ROZE + "_plaat", plaat())
    # the east side of the hall is blocked: a giant kaasknabbel fell over there, and a giant cushion
    for x in (31,):
        for z in range(9, 19):
            for y in range(Y + 1, Y + 4):
                s.set(x, y, z, "guhs:block_of_kaasknabbels")
    for z in range(9, 19):                                                                 # (it is round: the corners off, a bite out of it)
        s.set(31, Y + 4, z, "guhs:block_of_kaasknabbels") if 10 <= z <= 17 else None
    for (y, z) in ((Y + 4, 12), (Y + 4, 13), (Y + 3, 9), (Y + 4, 17)):
        s.blocks.pop((31, y, z), None)
    # a giant cushion, south-east: 4 x 3, a button in the middle
    for x in range(27, 31):
        for z in range(19, 22):
            s.set(x, Y + 1, z, "minecraft:magenta_wool" if (x, z) in ((28, 20), (29, 20)) else "minecraft:pink_wool")
    for x in range(27, 31):
        s.set(x, Y + 2, 21, "minecraft:pink_wool")
    # a giant cup by the pillow (south-west corner): white with a pink rim, tea in it
    for (dx, dz) in ((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2), (2, 2)):
        for y in range(Y + 1, Y + 4):
            s.set(18 + dx - 1, y, 19 + dz, "minecraft:white_concrete" if y < Y + 3 else "minecraft:pink_concrete")
    s.set(18, Y + 1, 20, "minecraft:white_concrete")
    s.set(18, Y + 2, 20, "minecraft:brown_concrete_powder")
    # the hoard at the back (north): a heap of knabbels and gold; the clickable heap in front of it
    for x in range(21, 28):
        for z in range(5, 8):
            d = abs(x - 24) + (z - 5) * 1.3
            hoog = 3 - int(d * 0.75)
            for y in range(Y + 1, Y + 1 + max(0, hoog)):
                r = float(rnd.random())
                s.set(x, y, z, "minecraft:gold_block" if r < 0.25 else "minecraft:raw_gold_block" if r < 0.4 else "guhs:block_of_kaasknabbels")
    schat = (24, Y + 1, 8)
    s.set(*schat, "guhs:wolkenkasteeltje_schat", {"facing": "south"})
    bord(h, s, 22, Y + 1, 8, "south", naam, ["Eén kruimel", "per dag.", "De reus telt ze.", "(Echt waar.)"])
    # light: cloud lamps in the corners and over the hoard
    for (x, y, z) in ((17, Y + 5, 5), (31, Y + 5, 5), (17, Y + 5, 21), (31, Y + 5, 21), (24, Y + 6, 6)):
        s.set(x, y, z, "guhs:wolkenlamp")
    # the giant, asleep on his bed, his head west on the pillow
    reus = (24.0, Y + 2, 14.0)
    s.entity(reus[0], reus[1], reus[2], {"id": "guhs:reuzenguh", "Rotation": h.floats(90.0, 0.0), "PersistenceRequired": h.Byte(1)})
    # --- outside --------------------------------------------------------------------------------------------------------
    bord(h, s, 27, Y + 1, 24, "south", naam, ["Sssst.", "Reus slaapt.", "Niet rennen.", "Niet springen."])
    s.set(21, Y + 1, 24, "guhs:wolkenlamp")
    landing = (24.5, Y + 1, 27.5)
    anker(s, op[0] + 3, Y, op[1], naam, WIT)
    s.save(naam)
    MATEN[naam] = {"anker": (op[0] + 3, Y, op[1]), "reus": reus, "schat": schat, "landing": landing, "hal": (17, Y + 1, 5, 31, Y + MUUR + 2, 21),
                   "poort": (24, Y + 1, Z1), "dek": Y, "lift": op + (pad_y,), "stroom": neer + (pad_y,)}
    return s


# =====================================================================================================================
# 3. the lightning forge
# =====================================================================================================================
def bliksemsmidse(h):
    naam = "bliksemsmidse"
    hoogte = STRUCTUREN[naam]["hoogte"]
    Y = hoogte
    s = h.Structure((33, hoogte + 18, 35))
    # a white puff to stand on, and the dark cloud on it
    eiland.wolk(h, s, (16, Y - 2, 18), 12, 3, 13, zaad=naam, kleur="wit")
    vloer = dek(s, 16, 18, 11.5, 12.5, Y, zaad=naam)
    cx, cy, cz = 16, Y + 4, 12
    for (bx, by, bz, rx, ry, rz) in ((cx, cy, cz, 8.5, 5.6, 7.5), (cx - 5, cy + 1, cz - 2, 5, 4, 4.5), (cx + 5, cy + 0.5, cz + 1, 5.5, 4.4, 5),
                                     (cx + 1, cy + 3.5, cz - 1, 5, 3.2, 4.5), (cx - 3, cy + 3, cz + 2, 4, 2.8, 3.6)):
        bol(s, bx, by, bz, rx, ry, rz, DONKER, plat=0.85)
    op, neer = (16, 26), (9, 24)
    pad_y = liften(h, s, op[0], op[1], neer[0], neer[1], Y, hoogte, kijk="north")
    rond_af(s, vlak=vloer)
    # the forge inside: x 11..21, z 7..16, four high; the floor is dark cloud
    for x in range(11, 22):
        for z in range(7, 17):
            rond = (x in (11, 21)) + (z in (7, 16))
            if rond == 2:
                continue
            s.set(x, Y, z, DONKER)
            for y in range(Y + 1, Y + 5 - (1 if rond else 0)):
                s.set(x, y, z, LUCHT)
    # the doorway south (three wide, three high) with a little dark porch roof
    for x in (15, 16, 17):
        for z in range(17, 21):
            for y in range(Y + 1, Y + 4):
                if (x, y, z) in s.blocks and s.blocks[(x, y, z)][0].startswith(DONKER):
                    s.set(x, y, z, LUCHT)
            if (x, Y, z) in s.blocks and z <= 19:
                s.set(x, Y, z, DONKER)
    # the hearth against the north wall: a lit blast furnace between two chimneys of deepslate, bellows of barrels
    s.set(16, Y + 1, 7, "minecraft:blast_furnace", {"facing": "south", "lit": "true"})
    for x in (15, 17):
        s.set(x, Y + 1, 7, "minecraft:polished_deepslate")
        s.set(x, Y + 2, 7, "minecraft:polished_deepslate_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    s.set(16, Y + 2, 7, "minecraft:polished_deepslate_slab", plaat())
    s.set(16, Y + 3, 7, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # the anvil in the middle, the smid behind it (he looks at the door)
    s.set(16, Y + 1, 11, "minecraft:anvil", {"facing": "east"})
    smid = (16.5, Y + 1, 9.5)
    s.entity(smid[0], smid[1], smid[2], {"id": "guhs:guh_npc", "Kind": "smidguh", "PersistenceRequired": h.Byte(1), "Rotation": h.floats(0.0, 0.0)})
    # cloud waiting to be pressed (west), pressed blocks stacked up (east)
    for (x, y, z) in ((12, Y + 1, 9), (12, Y + 1, 10), (13, Y + 1, 9), (12, Y + 2, 9), (12, Y + 1, 13)):
        s.set(x, y, z, WIT)
    s.set(13, Y + 1, 10, WIT + "_plaat", plaat())
    s.set(12, Y + 2, 10, ROZE + "_plaat", plaat())
    for (x, y, z, b, p) in ((20, Y + 1, 9, WIT + "_trap", trap("west")), (20, Y + 1, 10, ROZE, None), (20, Y + 2, 10, WIT + "_plaat", plaat()),
                            (19, Y + 1, 9, DONKER + "_plaat", plaat()), (20, Y + 1, 13, "guhs:bliksemsmidse_wolkentafel", {"facing": "west"}),
                            (20, Y + 1, 14, "guhs:wolkenbank", {"facing": "west"}), (12, Y + 1, 14, "guhs:bliksemsmidse_wolkenplank", {"facing": "east"}),
                            (13, Y + 1, 15, "minecraft:water_cauldron", {"level": "3"}), (19, Y + 1, 15, "minecraft:grindstone", {"face": "floor", "facing": "north"})):
        s.set(x, y, z, b, p)
    # light: a cloud lamp inside, the little rain cloud of the shop over a flower pot by the door
    s.set(14, Y + 3, 12, "guhs:wolkenlamp")
    s.set(18, Y + 3, 13, "guhs:wolkenlamp")
    s.set(19, Y + 1, 19, "minecraft:potted_pink_tulip")
    s.set(19, Y + 3, 19, "guhs:bliksemsmidse_onweerswolkje", {"facing": "south"})
    bord(h, s, 13, Y + 1, 20, "south", naam, ["Bliksemsmidse.", "Pluis erin.", "Blok eruit.", "Tik. Tik. Njeg."])
    # a lightning rod on top of the cloud, for the look of it
    top = max(y for (x, y, z) in s.blocks if x == cx and z == cz and s.blocks[(x, y, z)][0].startswith(DONKER))
    s.set(cx, top + 1, cz, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    anker(s, op[0] + 3, Y, op[1], naam, WIT)
    s.save(naam)
    MATEN[naam] = {"anker": (op[0] + 3, Y, op[1]), "smid": smid, "dek": Y, "deur": (16, Y + 1, 18), "lift": op + (pad_y,), "stroom": neer + (pad_y,)}
    return s


# =====================================================================================================================
# checks over the templates (pure; the same rules as the Java side's BrugCheck)
# =====================================================================================================================
def loop_check(hoogtes):
    """The arc's walking heights: no step of more than a block, and a whole-block step only onto a stair."""
    fout = []
    for i in range(len(hoogtes) - 1):
        a, b = sorted((hoogtes[i], hoogtes[i + 1]))
        if b - a > 2 or (b - a == 2 and b % 2 == 1):
            fout.append(f"arc column {i}: {hoogtes[i]} -> {hoogtes[i + 1]} half blocks")
    if hoogtes[0] != 2 or hoogtes[-1] != 2:
        fout.append("the arc's feet must be one stair above the bank")
    return fout


def zweef_check(s, naam, grond):
    """Everything in the template hangs together: one piece (neighbours by a face, an edge or a corner; a cloud lamp may float)."""
    vast = {p for p, (n, _p, _n) in s.blocks.items() if n not in (LUCHT, "guhs:wolkenlamp") and p[1] > grond}
    if not vast:
        return [f"{naam}: empty"]
    buren = [(dx, dy, dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if (dx, dy, dz) != (0, 0, 0)]
    over, stukken = set(vast), []
    while over:
        start = next(iter(over))
        stuk, rij = {start}, [start]
        over.discard(start)
        while rij:
            x, y, z = rij.pop()
            for (dx, dy, dz) in buren:
                q = (x + dx, y + dy, z + dz)
                if q in over:
                    over.discard(q)
                    stuk.add(q)
                    rij.append(q)
        stukken.append(stuk)
    stukken.sort(key=len, reverse=True)
    los = [p for stuk in stukken[1:] for p in stuk]
    return [f"{naam}: {len(los)} blocks in {len(stukken) - 1} loose pieces, e.g. {sorted(los)[:4]}"] if los else []


def build(h):
    fout = []
    b = regenboogbrug(h)
    fout += loop_check(MATEN["regenboogbrug"]["hoogtes"]) + zweef_check(b, "regenboogbrug", 3)
    k = wolkenkasteeltje(h)
    fout += zweef_check(k, "wolkenkasteeltje", 3)
    f = bliksemsmidse(h)
    fout += zweef_check(f, "bliksemsmidse", 3)
    if fout:
        raise SystemExit("bio_bouw_wolk2 templates:\n  " + "\n  ".join(fout))
    return {"regenboogbrug": b, "wolkenkasteeltje": k, "bliksemsmidse": f}
