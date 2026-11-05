"""Zone 3 "De Schemerwortel" (revamp): the GAME parts on top of the kit (one tile window W at a time, rel coordinates, NOT
mirrored: world x = 2300 + rel x).

Every spot comes from plan3.plekken() (the same list as POI_Z3.md / Z3WereldPlekken), converted back to rel blocks with R(id), so
the built map and the published coordinates cannot drift apart. Every stair/ramp here is in HALF steps (a full block or a bottom
slab per stand height): nothing needs a jump.
  init()       module import: the rail gaps of the inner stair / riding spiral, the no-tree spots (deterministic per process)
  bouw(W)      after the kit: cloud sea + rescue columns, Honingtak (interiors, chimneys, cells + lifts, dock, market, wind
               landing, binnenhof + trunk lifts), Schorsstad (wall houses furnished, ground gates, the Pelszaal under the floor,
               the passage to the roots), riding spiral (start ramp, balconies), crown (Kale Kruin, grey patches, glide
               platforms, lookout, Mika's mooring, blossom curtains, lantern strings), werf rooms, the Hart dungeon, the
               ground (Mosvoet, Pluimstal, ring path, posts, kettles, lookout), secrets, campfires (last).
"""
import functools
import math

import numpy as np

import plan3 as PL
import boom2 as B
import plekken2 as P
import werf2 as WF
from schemerkit import hash3, vnoise

PLANK, HOUT, HEK = "minecraft:spruce_planks", "minecraft:stripped_spruce_wood", "minecraft:spruce_fence"
SLAB = "minecraft:spruce_slab[type=bottom,waterlogged=false]"
LAMP_R, LAMP_G, LAMP_M = "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"
LANTAARN = "minecraft:lantern[hanging=false,waterlogged=false]"
HANGLAMP = "minecraft:lantern[hanging=true,waterlogged=false]"
ALTAAR = "guhs:vr_z3_baas_altaar"
KAMPVUUR = "minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false,z3kv=1]"
LUCHT = "minecraft:air"
HY = B.HONING_Y


@functools.lru_cache(maxsize=1)
def FL():
    out = {}

    def plat(p, o):
        if isinstance(o, list) and len(o) == 3 and all(isinstance(v, int) for v in o):
            out[p] = o
        elif isinstance(o, dict):
            for k, v in o.items():
                plat(f"{p}.{k}" if p else k, v)
    plat("", PL.plekken())
    return out


def R(k):
    """POI id -> rel block (x, y, z) ints."""
    X, Y, Z = FL()[k]
    return X - PL.TX_STAM, Y, Z - PL.TZ_STAM


def leeg(W, x1, y1, z1, x2, y2, z2):
    W.wfill(x1, y1, z1, x2, y2, z2, LUCHT)


def grond_f(x, z):
    return math.floor(PL.grond(x, z))


# ================================================================================================ init (rail gaps, free spots)
def init():
    # inner stair: the Honingtak landing opens to the binnenhof deck, the kruin-1 landing to lift 2's top platform, the nest
    for i in (1, 2, 5):
        a, L = B.trap_landing(i)
        B.TRAP_GATEN.append((a % (2 * math.pi), B.TRAP_WL / 2 + 0.02, L - 0.6, L + 0.6))
    a0, _ = B.trap_landing(0)
    B.TRAP_GATEN.append((a0 % (2 * math.pi), math.pi, 70, 81.6))           # the floor landing: the floor itself
    # riding spiral: onto the Honingplein, onto kruin 1 at the end, the balconies, the start
    B.RIJ_GATEN.append((B.TH_M, 0.5, HY - 3, HY + 3))
    th_end = B.RIJ["th0"] + 2 * math.pi * (B.RIJ["eind"] - B.RIJ["y0"]) / B.RIJ["stijg"]
    B.RIJ_GATEN.append((th_end % (2 * math.pi), 1.2, B.RIJ["eind"] - 12, B.RIJ["eind"] + 1))
    for k, (turn, deg) in enumerate(((0, 200), (2, 30), (3, 250))):
        th = math.radians(deg)
        lev = B.rij_lev(th, turn)
        B.RIJ_GATEN.append((th, 0.06, lev - 2, lev + 2))
    B.RIJ_GATEN.append((B.RIJ["th0"], 0.35, B.RIJ["y0"] - 1, B.RIJ["y0"] + 5))
    # no trees on: camps, ruins, lifts, the hamlet, the stable, the gates, the ring path, wind columns, the Wortelpoort
    vrij = B.PLEKKEN_VRIJ
    vrij.clear()
    for (x, z, y, seed, lab) in P.kampen():
        vrij.append((x, z, 14))
    for (x, z, y, Rr, seed) in P.ruines():
        vrij.append((x, z, Rr + 6))
    lx, lz, *_ = P.lift_a()
    vrij.append((lx, lz, 28))
    vrij.append((PL.MOSVOET[0], PL.MOSVOET[1], 34))
    vrij.append((PL.PLUIMSTAL[0], PL.PLUIMSTAL[1], 30))
    sx, sz = PL.PLUIMSTAL
    sa = math.atan2(sz, sx)
    vrij.append((sx + math.cos(sa) * 36, sz + math.sin(sa) * 36, 26))
    for a in B.POORT_HOEKEN:
        for r in range(50, 100, 8):
            vrij.append((r * math.cos(a), r * math.sin(a), 8))
    for deg in range(0, 360, 4):
        vrij.append((PL.WORTELWEG_R * math.cos(math.radians(deg)), PL.WORTELWEG_R * math.sin(math.radians(deg)), 6))
    for (wid, x, z, y0, y1, rad, lx, lz) in PL.windkolommen():
        vrij.append((x, z, 9))
    vrij.append((B.POORT[0], B.POORT[1], 30))
    for t in np.linspace(0, 1, 12):
        vrij.append((rijstart_ramp()[0][0] + (rijstart_ramp()[1][0] - rijstart_ramp()[0][0]) * t,
                     rijstart_ramp()[0][1] + (rijstart_ramp()[1][1] - rijstart_ramp()[0][1]) * t, 6))


@functools.lru_cache(maxsize=1)
def rijstart_ramp():
    """The riding spiral starts at level y0 (82) at angle th0 right against the trunk foot: a ramp straight outward to the
    ground. Returns (start (x, z) at the path, end (x, z) on the ground, stand at the path, ground stand at the end)."""
    th = B.RIJ["th0"] + 0.06
    x, sh, z = PL.rij_punt_lev(0, th, 0.5)
    ux, uz = math.cos(th), math.sin(th)
    for L in range(6, 80):
        ex, ez = x + ux * L, z + uz * L
        gs = PL.gstand(ex, ez)
        if L >= 2 * abs(sh - gs) + 4 and math.hypot(ex, ez) > 70:
            return (x, z), (ex, ez), sh, gs
    return (x, z), (x + ux * 40, z + uz * 40), sh, PL.gstand(x + ux * 40, z + uz * 40)


# ================================================================================================ (no cloud sea any more)
# [wereldindeling] the old water ring (bouw3.zee) is gone: the edges are natural (island lobes, hanging roots); the shared
# cloud sea / the Wolkenkloof / the Wolkenval round the island belong to the world layout, not to this map.


# ================================================================================================ wind columns (extra)
def windkolommen(W):
    for (wid, x, z, y0, y1, rad, lx, lz) in PL.windkolommen():
        if not (W.raakt(x - 14, y0 - 4, z - 14, x + 14, y1 + 8, z + 14) or W.raakt(lx - 8, y1 - 4, lz - 8, lx + 8, y1 + 6, lz + 8)):
            continue
        # the landing: a flush plank pad with 3 air above
        g = y1 - 1
        W.cilinder(lx, lz, 2.6, g, g, PLANK)
        W.cilinder(lx, lz, 2.6, g + 1, g + 3, LUCHT)
        if wid == "honingtak":
            # the deck fence between the column top and the deck is open
            for t in np.linspace(0, 1, 14):
                px, pz = x + (lx - x) * t, z + (lz - z) * t
                for w_ in (-1.5, -1, -0.5, 0, 0.5, 1, 1.5):
                    a = math.atan2(lz - z, lx - x)
                    qx, qz = px - math.sin(a) * w_, pz + math.cos(a) * w_
                    if math.hypot(qx, qz) > 60:
                        W.wset(qx, HY, qz, PLANK)
                        W.wfill(qx, HY + 1, qz, qx, HY + 4, qz, LUCHT)


# ================================================================================================ Honingtak
def interieur(W, x, fy, z, deur, fn, Rh):
    """Furniture by function inside a round/hex house (centre x, z; floor block fy - 1; door direction deur)."""
    dx, dz = math.cos(deur), math.sin(deur)
    tx, tz = -dz, dx
    vloer = "minecraft:stone_bricks" if fn == "smid" else ("minecraft:polished_deepslate" if fn == "bank" else PLANK)
    W.cilinder(x, z, Rh - 1.0, fy - 1, fy - 1, vloer)
    W.cilinder(x, z, Rh - 1.0, fy, fy + 4, LUCHT)
    loper = {"juwelier": "minecraft:purple_carpet", "drankjes": "minecraft:pink_carpet", "uitrusting": "minecraft:orange_carpet",
             "algemeen": "minecraft:yellow_carpet", "jagershut": "minecraft:brown_carpet", "burgemeester": "minecraft:pink_carpet"}
    if fn in loper:
        for d in np.arange(-Rh + 1.5, Rh - 1.0, 1.0):
            W.wset(x + dx * d, fy, z + dz * d, loper[fn])
    def at(a, b, s):      # a = towards the back (-door), b = sideways
        W.wset(x - dx * a + tx * b, fy, z - dz * a + tz * b, s)

    def op(a, b, s):
        W.wset(x - dx * a + tx * b, fy + 1, z - dz * a + tz * b, s)
    rug = Rh - 2.0
    if fn == "smid":
        at(rug, -1.5, "minecraft:anvil[facing=east]")
        at(rug, 1.5, "minecraft:blast_furnace[facing=south,lit=true]")
        at(0, rug, "minecraft:magma_block")
        op(0, rug, "minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]")
        at(0, -rug, "minecraft:grindstone[face=floor,facing=north]")
        at(1.5, -rug + 0.5, "minecraft:smithing_table")
    elif fn == "juwelier":
        for b in (-1.5, -0.5, 0.5, 1.5):
            at(0.4, b, "minecraft:quartz_slab[type=top,waterlogged=false]")
            op(0.4, b, "minecraft:white_stained_glass")
        at(0, rug, "guhs:weidekristal_roze")
        at(0, -rug, "guhs:guh_kristal_blok")
    elif fn in ("drankjes", "uitrusting", "algemeen", "bank", "jagershut"):
        for b in (-1.5, -0.5, 0.5, 1.5):
            at(0.4, b, "minecraft:stripped_oak_wood" if fn != "bank" else "minecraft:gold_block")
            op(0.4, b, "minecraft:oak_slab[type=bottom,waterlogged=false]")
        for b in (-1.5, 1.5):
            at(rug, b, "minecraft:barrel[facing=up,open=false]")
            op(rug, b, "guhs:glimpotje[licht=3]" if fn == "drankjes" else
               ("minecraft:chest[facing=north,type=single,waterlogged=false]" if fn == "bank" else "minecraft:honeycomb_block"))
        if fn == "jagershut":
            px, py, pz = R("gebouw.jagershut.premiebord")
            jx, jy, jz = R("gebouw.jagershut.jachtbord")
            W.wset(px, py, pz, "minecraft:lectern[facing=north,has_book=false,powered=false]")
            W.wset(jx, jy, jz, "minecraft:chiseled_bookshelf[facing=north,slot_0_occupied=true,slot_1_occupied=false,"
                               "slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=false]")
    else:   # homes + the mayor
        at(rug, -1, "minecraft:pink_bed[facing=east,occupied=false,part=foot]")
        at(rug, 0, "minecraft:pink_bed[facing=east,occupied=false,part=head]")
        at(rug - 0.5, 1.6, "minecraft:smoker[facing=south,lit=true]")
        at(0, rug, "minecraft:barrel[facing=up,open=false]")
        op(0, rug, "minecraft:honey_block")
        at(0, -rug, "guhs:guh_tafel[facing=south]")
        op(0, -rug, "guhs:theepotje[facing=south]")
        at(-1, -rug + 0.5, "guhs:guh_stoel[facing=north]")
        if fn == "burgemeester":
            at(1, rug, "guhs:guh_bloempot")
    W.wset(x, fy + 4, z, HANGLAMP)
    # the door line free
    for d in np.arange(Rh - 1.5, Rh + 2.5, 0.5):
        for b in (-1, 0, 1):
            for h in (0, 1, 2):
                W.wset(x + dx * d + tx * b * 0.8, fy + h, z + dz * d + tz * b * 0.8, LUCHT)


def schoorsteen(W, sx, sz, top):
    W.wfill(sx, top - 3, sz, sx, top + 1, sz, "minecraft:bricks")
    W.wset(sx, top + 1, sz, "minecraft:brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")


def honingtak_extra(W):
    if not W.raakt(-170, HY - 40, -170, 170, HY + 30, 170):
        return
    for fn, (hx, hz, deur, Rr, waar, soort) in PL.ring_functies():
        if not W.raakt(hx - 10, HY - 3, hz - 10, hx + 10, HY + 16, hz + 10):
            continue
        interieur(W, hx, HY + 1, hz, deur, fn, Rr if soort != 2 else 3.6)
    for (x, z, top, sx, sz) in PL.huis_daken():
        if W.raakt(sx - 2, top - 5, sz - 2, sx + 2, top + 3, sz + 2):
            schoorsteen(W, sx, sz, top)
    # the 3 big cells under the deck: a door + bridge to the lift shaft, the shaft (a hole in the deck), interiors
    for naam, (ad, Rc, hc) in P.CELLEN.items():
        a = math.radians(ad)
        ux, uz = math.cos(a), math.sin(a)
        cx, cz = 55 * ux, 55 * uz
        yb = HY - 2 - hc
        if not W.raakt(cx - 22, yb - 16, cz - 22, cx + 22, HY + 8, cz + 22):
            continue
        rs = 55 + Rc + 3.5
        sx, sz = rs * ux, rs * uz
        # cell floor (planks over the honeycomb bottom) + air inside
        for r_ in np.arange(0, Rc - 1.0, 0.5):
            pass
        W.cilinder(cx, cz, Rc * 0.85, yb + 1, yb + hc - 2, LUCHT)
        W.cilinder(cx, cz, Rc * 0.85, yb, yb, "minecraft:quartz_block" if naam == "nestkapel" else PLANK)
        # door in the outer wall + a landing round the shaft at the cell floor, the shaft itself through the deck
        for d in np.arange(Rc - 2.0, rs - 55 + 2.5, 0.5):
            for w_ in (-1, -0.5, 0, 0.5, 1):
                W.wset(cx + ux * d - uz * w_, yb, cz + uz * d + ux * w_, PLANK)
                W.wfill(cx + ux * d - uz * w_, yb + 1, cz + uz * d + ux * w_, cx + ux * d - uz * w_, yb + 3,
                        cz + uz * d + ux * w_, LUCHT)
        W.cilinder(sx, sz, 2.6, yb, yb, "minecraft:honeycomb_block")
        W.cilinder(sx, sz, 1.6, yb + 1, HY + 6, LUCHT)
        for s_ in (-1, 1):
            W.wfill(sx - uz * 2 * s_, yb + 1, sz + ux * 2 * s_, sx - uz * 2 * s_, HY + 7, sz + ux * 2 * s_, "minecraft:iron_chain[axis=y]")
        W.wfill(sx - uz * 2, HY + 7, sz + ux * 2, sx + uz * 2, HY + 7, sz - ux * 2, "minecraft:stripped_spruce_wood")
        W.wset(sx, HY + 6, sz, "minecraft:bee_nest[facing=south,honey_level=5]")
        # a fence ring round the shaft hole on the deck, open towards the trunk (the top stop)
        for k in range(12):
            b = k * math.pi / 6
            if math.cos(b - (a + math.pi)) > 0.6:
                continue
            W.wset(sx + 2.6 * math.cos(b), HY + 1, sz + 2.6 * math.sin(b), HEK)
        W.wset(sx + ux * 2.6, HY + 2, sz + uz * 2.6, LAMP_G)
        lx, ly, lz = R({"cafe": "lift_cafe_boven", "nestkapel": "lift_kapel_boven", "raattros": "lift_raattros_boven"}[naam])
        W.wfill(lx - 1, HY, lz - 1, lx + 1, HY, lz + 1, PLANK)
        if naam == "cafe":
            for k in range(5):
                b = k * 1.25
                W.wset(cx + 4 * math.cos(b), yb + 1, cz + 4 * math.sin(b), "guhs:guh_tafel[facing=south]")
                W.wset(cx + 4 * math.cos(b), yb + 2, cz + 4 * math.sin(b), "guhs:theepotje[facing=south]")
            W.wfill(cx - ux * 6 - 1, yb + 1, cz - uz * 6 - 1, cx - ux * 6 + 1, yb + 1, cz - uz * 6 + 1,
                    "minecraft:oak_slab[type=bottom,waterlogged=false]")
        elif naam == "nestkapel":
            ex, ey, ez = R("gebouw.nestkapel.eiersokkel")
            W.wset(ex - ux, ey, ez - uz, "minecraft:chiseled_quartz_block")
            W.wset(ex - ux, ey + 1, ez - uz, "guhs:weidekristal_roze")
            dx_, dy_, dz_ = R("gebouw.nestkapel.droomsteen")
            W.wset(dx_ - ux, dy_, dz_ - uz, "minecraft:quartz_pillar[axis=y]")
            W.wset(dx_ - ux, dy_ + 1, dz_ - uz, "guhs:guh_kristal_blok")
            for k in range(6):
                b = k * math.pi / 3
                W.wset(cx + 6 * math.cos(b), yb + hc - 3, cz + 6 * math.sin(b), "minecraft:pearlescent_froglight")
            for d in (1, 3):
                for w_ in (-4, -3, 3, 4):
                    W.wset(cx + ux * d - uz * w_, yb + 1, cz + uz * d + ux * w_,
                           "minecraft:birch_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
        else:
            W.wset(cx + 2, yb + 1, cz - 2, "minecraft:beehive[facing=south,honey_level=5]")
            W.wset(cx - 2, yb + 1, cz + 2, "minecraft:barrel[facing=up,open=false]")
            # the secret hatch + ladder down to the Mika-imker raatzaal
            hx, hy, hz = R("mika_imker_luik")
            zx, zy, zz = R("mika_imker_raatzaal")
            Rz, hh = 8, 8
            for x in range(zx - 10, zx + 11):
                for z in range(zz - 10, zz + 11):
                    d = max(abs(x - zx) * 0.5 + abs(z - zz) * 0.866, abs(x - zx))
                    if d <= Rz:
                        for y in range(zy - 1, zy + hh):
                            rand = d > Rz - 1.2 or y in (zy - 1, zy + hh - 1)
                            W.wset(x, y, z, ("minecraft:honeycomb_block" if (y + x) % 3 else "minecraft:light_gray_terracotta")
                                   if rand else LUCHT)
            for (ddx, ddz) in ((-3, -3), (3, 3), (0, 4)):
                W.wset(zx + ddx, zy, zz + ddz, "minecraft:beehive[facing=south,honey_level=2]")
            for y in range(zy, hy - 1):
                W.wset(hx, y, hz, "minecraft:ladder[facing=north,waterlogged=false]")
                W.wset(hx, y, hz + 1, "minecraft:honeycomb_block")
            W.wset(hx, hy - 1, hz, "minecraft:spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
            W.wset(zx + 2, zy + hh - 2, zz, HANGLAMP)
        W.wset(cx, yb + hc - 3, cz, HANGLAMP)
    dok(W)
    kramen(W)
    binnenhof(W)


def dok(W):
    """The two airship piers: a round platform (deck level) at the branch tip, mast, bell, chains."""
    for naam, pre in (("Weststeiger", "dok"), ("Oosttaksteiger", "oosttaksteiger")):
        (mx, mz), (ux, uz) = PL.steiger(naam)
        if not W.raakt(mx - 30, HY - 20, mz - 30, mx + 30, HY + 30, mz + 30):
            continue
        ua = math.atan2(uz, ux)
        W.cilinder(mx, mz, 7.5, HY, HY, None, mixfn=lambda x, y, z, m: W.kies([PLANK, "minecraft:stripped_spruce_wood"], [4, 1],
                                                                               x, y, z, 3601))
        W.cilinder(mx, mz, 7.5, HY + 1, HY + 5, LUCHT)
        for k in range(30):
            b = k * 2 * math.pi / 30
            if math.cos(b - (ua + math.pi)) > 0.8:
                continue        # open towards the boardwalk
            W.wset(mx + 7.2 * math.cos(b), HY + 1, mz + 7.2 * math.sin(b), HEK)
        W.wfill(mx, HY + 1, mz, mx, HY + 22, mz, "minecraft:stripped_spruce_log[axis=y]")
        W.wfill(mx - 3, HY + 20, mz, mx + 3, HY + 20, mz, "minecraft:stripped_spruce_log[axis=x]")
        W.wset(mx, HY + 23, mz, LAMP_R)
        for s in (-3, 3):
            W.wset(mx + s, HY + 19, mz, HANGLAMP)
        bx, by, bz = R(f"{pre}_bel")
        W.wset(bx, by, bz, "minecraft:bell[attachment=floor,facing=north,powered=false]")
        for k in range(6):
            b = ua + math.pi / 2 + k * 0.4
            for h in range(0, 18, 3):
                W.wset(mx + 8.5 * math.cos(b), HY - h, mz + 8.5 * math.sin(b), "minecraft:iron_chain[axis=y]", keep=True)
        # a departure board (the colour of the line): a post with wool
        W.wfill(mx - uz * 5, HY + 1, mz + ux * 5, mx - uz * 5, HY + 3, mz + ux * 5, "minecraft:stripped_spruce_log[axis=y]")
        W.wset(mx - uz * 5, HY + 4, mz + ux * 5, "minecraft:pink_wool" if pre == "dok" else "minecraft:light_blue_wool")


def kramen(W):
    cx, cz = PL.pol(B.TH_M, 52)
    if not W.raakt(cx - 30, HY - 2, cz - 30, cx + 30, HY + 8, cz + 30):
        return
    kleuren = ["minecraft:pink_wool", "minecraft:yellow_wool", "minecraft:orange_wool", "minecraft:white_wool", "minecraft:lime_wool"]
    for (i, kant, a, r) in PL.KRAMEN:
        x, z = r * math.cos(a), r * math.sin(a)
        W.wfill(x - 1, HY + 1, z - 1, x + 1, HY + 1, z + 1, "minecraft:barrel[facing=up,open=false]")
        for (dx, dz) in ((-1.5, -1.5), (1.5, -1.5), (-1.5, 1.5), (1.5, 1.5)):
            W.wfill(x + dx, HY + 1, z + dz, x + dx, HY + 3, z + dz, HEK)
        W.wfill(x - 2, HY + 4, z - 2, x + 2, HY + 4, z + 2, kleuren[i])
        W.wset(x, HY + 3, z, HANGLAMP)


def binnenhof(W):
    """The round deck inside the hollow trunk at the Honingtak landing (r 14, level HY) + the two trunk lifts."""
    if not W.raakt(-26, B.VOET - 2, -26, 26, 240, 26):
        return
    W.cilinder(0, 0, 14.6, HY, HY, None, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:honeycomb_block", PLANK, "minecraft:stripped_birch_wood"], [2, 3, 1], x, y, z, 3611))
    W.cilinder(0, 0, 14.6, HY + 1, HY + 1, "minecraft:birch_fence", hollow=0.9)
    W.cilinder(0, 0, 3.5, HY + 1, HY + 1, "minecraft:honey_block")
    W.wfill(0, HY + 1, 0, 0, HY + 3, 0, "minecraft:honeycomb_block")
    W.wset(0, HY + 4, 0, "minecraft:ochre_froglight")
    a1, L1 = B.trap_landing(1)
    # bridge from the landing to the deck (the rail of both is open there)
    for r in np.arange(12.5, 17.5, 0.5):
        for w_ in np.arange(-2.5, 2.51, 0.5):
            P.trede(W, r * math.cos(a1) - w_ * math.sin(a1), HY + 1, r * math.sin(a1) + w_ * math.cos(a1), PLANK, SLAB, lucht=4)
    # lift 1: floor -> binnenhof (shaft at r 9 opposite the landing); lift 2: binnenhof -> the kruin-1 landing
    for (k, a, y0, y1) in ((1, B.TH_M + math.pi, B.VOET, HY), (2, B.trap_landing(2)[0], HY, B.trap_landing(2)[1])):
        x, z = 9 * math.cos(a), 9 * math.sin(a)
        tx, tz = -math.sin(a), math.cos(a)
        for s_ in (-1, 1):
            W.wfill(x + tx * 2 * s_, y0 + 1, z + tz * 2 * s_, x + tx * 2 * s_, y1 + 6, z + tz * 2 * s_, "minecraft:iron_chain[axis=y]")
        W.cilinder(x, z, 1.4, y0 + 1, y1 - 1, LUCHT)
        W.cilinder(x, z, 1.4, y1 + 1, y1 + 4, LUCHT)
        W.cilinder(x, z, 2.4, y0, y0, "minecraft:honeycomb_block")
        if k == 2:
            # the top platform + bridge to the kruin-1 landing
            W.cilinder(x, z, 3.2, y1, y1, PLANK)
            for r in np.arange(9, 17.5, 0.5):
                for w_ in np.arange(-1.5, 1.51, 0.5):
                    P.trede(W, r * math.cos(a) - w_ * math.sin(a), y1 + 1, r * math.sin(a) + w_ * math.cos(a), PLANK, SLAB, lucht=4)
            W.cilinder(x, z, 1.4, y1 + 1, y1 + 4, LUCHT)
        W.wfill(x - tx * 2, y1 + 7, z - tz * 2, x + tx * 2, y1 + 7, z + tz * 2, "minecraft:stripped_spruce_wood")
        W.wset(x, y1 + 6, z, "minecraft:bee_nest[facing=south,honey_level=5]")
    # a lantern chain down the middle of the shaft from the nest floor to the binnenhof (life in the empty hollow)
    for y in range(HY + 8, B.NEST_Y - 1):
        W.wset(0, y, 0, "minecraft:iron_chain[axis=y]")
        if y % 12 == 0:
            for k in range(4):
                b = k * math.pi / 2 + y
                W.wset(1.5 * math.cos(b), y - 1, 1.5 * math.sin(b), HANGLAMP)
                W.wset(1.5 * math.cos(b), y, 1.5 * math.sin(b), "minecraft:iron_chain[axis=y]")


# ================================================================================================ Schorsstad
def schorsstad_extra(W):
    if not W.raakt(-110, 40, -110, 110, 170, 110):
        return
    # furnish the houses in the trunk wall (their floor block h, door at angle a at the stair)
    fns = ["woning", "winkel", "woning", "thee", "woning", "smid", "woning", "lantaarn"]
    for k, (y0, a, h) in enumerate(P.schors_huizen()):
        ca, sa = math.cos(a), math.sin(a)
        c = (B.R_IN + 3.6) * ca, (B.R_IN + 3.6) * sa
        if not W.raakt(c[0] - 8, h - 2, c[1] - 8, c[0] + 8, h + 8, c[1] + 8):
            continue
        fn = fns[k % len(fns)]
        def at(d, w, y, s):
            W.wset((B.R_IN + d) * ca - w * sa, y, (B.R_IN + d) * sa + w * ca, s)
        at(6.0, -2, h + 1, "minecraft:pink_bed[facing=north,occupied=false,part=foot]" if fn == "woning" else "minecraft:barrel[facing=up,open=false]")
        at(6.0, 2, h + 1, "minecraft:smoker[facing=south,lit=true]" if fn != "smid" else "minecraft:anvil[facing=east]")
        at(4.0, 2.5, h + 1, "guhs:guh_tafel[facing=south]")
        at(4.0, 2.5, h + 2, "guhs:theepotje[facing=south]" if fn in ("thee", "woning") else LANTAARN)
        at(4.0, -2.5, h + 1, "minecraft:chest[facing=north,type=single,waterlogged=false]")
        at(3.5, 0, h, "minecraft:brown_carpet" if k % 2 else "minecraft:orange_carpet")
        at(5.0, 0, h + 4, HANGLAMP)
    poorten(W)
    pelszaal(W)
    wortelgang_trap(W)


def poorten(W):
    """The two ground gates: the tunnel floor from the Schorsstad floor (stand 81) out to the ground, in half steps."""
    for a in B.POORT_HOEKEN:
        r_uit = PL.poort_uit_r(a) + 6
        ux, uz = math.cos(a), math.sin(a)
        if not W.raakt(min(0, ux * r_uit) - 8, 60, min(0, uz * r_uit) - 8, max(0, ux * r_uit) + 8, 95, max(0, uz * r_uit) + 8):
            continue
        g_uit = PL.gstand(ux * r_uit, uz * r_uit)
        P.helling(W, (ux * 20, uz * 20), B.VOET + 1, (ux * 30, uz * 30), B.VOET + 1, breed=2.5, vol=PLANK, hek=None, lucht=5)
        P.helling(W, (ux * 30, uz * 30), B.VOET + 1, (ux * r_uit, uz * r_uit), g_uit, breed=2.5, vol="minecraft:dirt_path",
                  half="minecraft:mud_brick_slab[type=bottom,waterlogged=false]", hek=None, lucht=5)
        for t in (0.35, 0.7):
            r = 20 + (r_uit - 20) * t
            for s_ in (-1, 1):
                W.wset(ux * r - uz * 3.6 * s_, B.VOET + 3, uz * r + ux * 3.6 * s_, LANTAARN, keep=True)


def pelszaal(W):
    """Graaf Piepvel's Pelszaal UNDER the Schorsstad floor: a round hall r 16 (stand 61, air to 74), reached by a straight stair
    from the floor (a hole with a rail in the plein). Cheese-rind throne, the big cage, 4 hanging cages, 6 blown-out posts."""
    fy = PL.PELSZAAL_VLOER
    if not W.raakt(-30, fy - 3, -30, 30, B.VOET + 3, 30):
        return
    W.cilinder(0, 0, PL.PELSZAAL_R + 1.5, fy - 1, fy + 15, None, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:dark_oak_wood", "minecraft:stripped_dark_oak_wood", "minecraft:spruce_wood"], [3, 2, 1], x, y, z, 3701))
    W.cilinder(0, 0, PL.PELSZAAL_R + 0.5, fy + 1, fy + 13, LUCHT)
    W.cilinder(0, 0, PL.PELSZAAL_R + 0.5, fy, fy, None, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:dark_oak_planks", "minecraft:spruce_planks", "guhs:belegen_kaas_stenen"], [4, 2, 1], x, y, z, 3702))
    for k in range(8):
        b = k * math.pi / 4
        W.wset(15 * math.cos(b), fy + 11, 15 * math.sin(b), "minecraft:shroomlight")
    tx, ty, tz = R("piepvel_troon")
    W.wfill(tx - 2, fy + 1, tz - 2, tx + 2, fy + 1, tz, "guhs:belegen_kaas_stenen")
    W.wfill(tx - 2, fy + 2, tz - 2, tx + 2, fy + 5, tz - 2, "guhs:gatenkaas_stenen")
    W.wset(tx, fy + 6, tz - 2, "minecraft:gold_block")
    gx, gy, gz = R("piepvel_grote_kooi")
    bars = "minecraft:iron_bars[east=true,north=true,south=true,west=true,waterlogged=false]"
    W.wfill(gx - 1, fy + 1, gz - 1, gx + 3, fy + 4, gz + 1, bars)
    leeg(W, gx, fy + 1, gz, gx + 2, fy + 3, gz)
    W.wfill(gx - 1, fy + 5, gz - 1, gx + 3, fy + 5, gz + 1, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
    for k in range(1, 5):
        cx_, cy_, cz_ = R(f"piepvel_kooi_{k}")
        W.wfill(cx_, cy_, cz_, cx_ + 1, cy_ + 1, cz_ + 1, bars)
        W.wfill(cx_, cy_ + 2, cz_, cx_ + 1, cy_ + 2, cz_ + 1, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
        for yy in range(cy_ + 3, fy + 14):
            W.wset(cx_, yy, cz_, "minecraft:iron_chain[axis=y]")
    for k in range(1, 7):
        px, py, pz = R(f"piepvel_lantaarnpaal_{k}")
        W.wfill(px, fy + 1, pz, px, fy + 3, pz, "minecraft:dark_oak_fence")
        W.wset(px, fy + 4, pz, "guhs:lampion_uit")
    # the stair: from the floor hole (stand 81) along a chord down to the hall edge (stand 61), 1 block per 2 = half steps
    pa = PL.pelszaal_trap_hoek()
    a0 = pa - 0.75
    top = (21 * math.cos(a0), 21 * math.sin(a0))
    tx_, tz_ = -math.sin(a0), math.cos(a0)
    # the stair runs tangentially (+t) for 40 blocks then turns inward to the hall door at angle pa
    mid = (top[0] + tx_ * 0, top[1] + tz_ * 0)
    eind = (21 * math.cos(pa), 21 * math.sin(pa))
    P.helling(W, top, B.VOET + 1, eind, fy + 1 + 4, breed=1.5, vol="minecraft:dark_oak_planks",
              half="minecraft:dark_oak_slab[type=bottom,waterlogged=false]", hek=None, lucht=4)
    deur = (12.5 * math.cos(pa), 12.5 * math.sin(pa))
    P.helling(W, eind, fy + 1 + 4, deur, fy + 1, breed=1.5, vol="minecraft:dark_oak_planks",
              half="minecraft:dark_oak_slab[type=bottom,waterlogged=false]", hek=None, lucht=4)
    # the hole in the floor: a rail round it (open on the stair side), lanterns
    for t in np.linspace(-0.25, 0.25, 9):
        for s_ in (-1, 1):
            r = 21 + s_ * 2.4
            W.wset(r * math.cos(a0 + t), B.VOET + 1, r * math.sin(a0 + t), "minecraft:dark_oak_fence", keep=True)
    W.wset(top[0], B.VOET + 2, top[1], LUCHT)


def wortelgang_trap(W):
    """The passage from the Schorsstad floor (stair foot) down to the Wortelpoort tunnel: the 'Wortels' landing of the stair."""
    a0, _ = B.trap_landing(0)
    ux, uz = math.cos(0.0), math.sin(0.0)
    if not W.raakt(-30, 40, -30, 100, 90, 30):
        return
    # from the floor at r 20 (east) straight east to the tunnel at x 86 (its floor ~ 47.5)
    Lg = B.gang_lijn()
    i = int(np.argmin(np.abs(Lg[:, 0] - 86)))
    gx, gy, gz = Lg[i]
    doel = PL.gang_stand(gy)
    P.helling(W, (18, 0), B.VOET + 1, (30, 0), B.VOET + 1, breed=2.0, vol=PLANK, hek=None, lucht=4)
    P.helling(W, (30, 0), B.VOET + 1, (gx - 2, gz), doel, breed=2.0, vol="minecraft:stripped_spruce_wood",
              half="minecraft:spruce_slab[type=bottom,waterlogged=false]", hek=None, lucht=4)
    for t in np.linspace(0.1, 0.9, 6):
        x = 30 + (gx - 32) * t
        y = B.VOET + 1 + (doel - B.VOET - 1) * t
        W.wset(x, y + 3, gz * t + 2.6, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    W.wfill(17, B.VOET + 1, -2, 17, B.VOET + 4, 2, LUCHT)


# ================================================================================================ riding spiral
def rijpad_extra(W):
    if not W.raakt(-200, 60, -200, 200, 240, 200):
        return
    a, b, sh, gs = rijstart_ramp()
    P.helling(W, b, gs, a, sh, breed=4.0, vol="minecraft:dirt_path", half="minecraft:mud_brick_slab[type=bottom,waterlogged=false]",
              hek=None, lucht=6, grond=lambda x, z: grond_f(x, z))
    for k in (1, 2, 3):
        x, y, z = R(f"spiraal_balkon_{k}")
        jx, jy, jz = R(f"spiraal_zweefsprong_{k}")
        if not W.raakt(x - 12, y - 6, z - 12, x + 12, y + 6, z + 12):
            continue
        W.cilinder(x + 0.5, z + 0.5, 4.0, y - 1, y - 1, PLANK)
        W.cilinder(x + 0.5, z + 0.5, 4.0, y, y + 3, LUCHT)
        a_ = math.atan2(z, x)
        for d in np.arange(-4, 9, 0.5):
            for w_ in (-1, 0, 1):
                W.wset(x + math.cos(a_) * d - math.sin(a_) * w_, y - 1, z + math.sin(a_) * d + math.cos(a_) * w_, PLANK)
                W.wset(x + math.cos(a_) * d - math.sin(a_) * w_, y, z + math.sin(a_) * d + math.cos(a_) * w_, LUCHT)
        for k2 in range(16):
            b2 = k2 * math.pi / 8
            if math.cos(b2 - a_) < -0.4 or abs(math.sin(b2 - a_)) < 0.3:
                continue
            W.wset(x + 4.2 * math.cos(b2), y, z + 4.2 * math.sin(b2), HEK, keep=True)
        W.wset(jx, y, jz, LUCHT)
        W.wfill(jx + 0.5, y, jz + 2, jx + 0.5, y + 4, jz + 2, "minecraft:stripped_cherry_log[axis=y]")
        W.wset(jx + 0.5, y + 5, jz + 2, LAMP_R)


# ================================================================================================ crown
def kruin_extra(W):
    if W.b[4] < 180:
        return
    kale_kruin(W)
    for i in range(1, 4):
        x, y, z = R(f"grauwe_vlek_{i}")
        if not W.raakt(x - 14, y - 8, z - 14, x + 14, y + 10, z + 14):
            continue
        for dx in range(-10, 11):
            for dz in range(-10, 11):
                if dx * dx + dz * dz > 100 or hash3(x + dx, 0, z + dz, 3801) < 0.15:
                    continue
                h = W.hoogte(x + dx, z + dz)
                if h is None or abs(h - (y - 1)) > 4:
                    continue
                W.wset(x + dx, h, z + dz, "minecraft:pale_moss_block" if hash3(x + dx, 1, z + dz, 3802) < 0.6 else
                       "minecraft:light_blue_terracotta")
        W.wfill(x + 4, y, z + 4, x + 4, y + 3, z + 4, "minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]")
        W.wset(x + 4, y + 4, z + 4, "minecraft:light_gray_concrete")
    for i in (1, 2):
        x, y, z = R(f"zweefplatform_{i}")
        if not W.raakt(x - 20, y - 6, z - 20, x + 20, y + 10, z + 20):
            continue
        a = math.atan2(z, x)
        ux, uz = math.cos(a), math.sin(a)
        for d in np.arange(-6, 16, 0.5):
            for w_ in np.arange(-2, 2.1, 0.5):
                W.wset(x + ux * d - uz * w_, y - 1, z + uz * d + ux * w_, PLANK)
                W.wfill(x + ux * d - uz * w_, y, z + uz * d + ux * w_, x + ux * d - uz * w_, y + 3, z + uz * d + ux * w_, LUCHT)
            if 0 < d < 14 and d % 2 == 0:
                for s_ in (-1, 1):
                    W.wset(x + ux * d - uz * 2.8 * s_, y, z + uz * d + ux * 2.8 * s_, HEK)
        ex, ez = x + ux * 15, z + uz * 15
        W.wfill(ex - uz * 3, y, ez + ux * 3, ex - uz * 3, y + 6, ez + ux * 3, "minecraft:stripped_cherry_log[axis=y]")
        W.wfill(ex - uz * 3, y + 4, ez + ux * 3, ex - uz * 3 + ux * 2, y + 6, ez + ux * 3 + uz * 2, "minecraft:pink_wool")
    x, y, z = R("uitkijk_kroon")
    if W.raakt(x - 8, y - 25, z - 8, x + 8, y + 6, z + 8):
        g = int(math.floor(B.kruin_top(1, x, z)))
        W.cilinder(x + 0.5, z + 0.5, 4.5, y - 1, y - 1, PLANK)
        leeg(W, x - 4, y, z - 4, x + 4, y + 4, z + 4)
        W.cilinder(x + 0.5, z + 0.5, 4.5, y, y, HEK, hollow=1)
        for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
            W.wfill(x + dx, g - 3, z + dz, x + dx, y - 2, z + dz, HOUT)
        for yy in range(g + 1, y):
            W.wset(x, yy, z, "minecraft:stripped_spruce_log[axis=y]")
            W.wset(x, yy, z + 1, "minecraft:ladder[facing=south,waterlogged=false]")
            W.wset(x, yy, z + 2, LUCHT)
        W.wset(x, y - 1, z + 1, LUCHT)
        W.wset(x, y, z + 1, LUCHT)
        W.wfill(x - 1, g, z - 1, x + 1, g, z + 3, PLANK)
        W.wset(x, y + 4, z, LAMP_R)
    mx, my, mz = R("mika_steiger")
    if W.raakt(mx - 14, my - 4, mz - 14, mx + 14, my + 24, mz + 14):
        for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
            W.wfill(mx + dx, my, mz + dz, mx + dx, my + 14, mz + dz, "minecraft:iron_block")
        W.wfill(mx - 4, my + 14, mz - 4, mx + 4, my + 14, mz + 4, "minecraft:light_gray_concrete")
        W.wfill(mx - 3, my + 15, mz - 3, mx + 3, my + 15, mz + 3, "minecraft:light_blue_wool")
        W.wfill(mx, my + 1, mz, mx, my + 13, mz, "minecraft:iron_chain[axis=y]")
    kruin_leven(W)


def kale_kruin(W):
    x, y, z = R("arena_kruinroofvogel")
    if not W.raakt(x - 45, y - 30, z - 45, x + 45, y + 30, z + 45):
        return
    # a dead flat top (r KALE_R) on a stepped skirt that comes down to kruin 1 on the trunk side (1 block per 2 = no jumps)
    for yy in range(y - 2, y - 26, -1):
        rr = PL.KALE_R + (y - 2 - yy) * 1.6
        W.cilinder(x + 0.5, z + 0.5, rr, yy, yy, None, keep=True, mixfn=lambda xx, y_, zz, m: W.kies(
            ["minecraft:pale_oak_leaves", "minecraft:pale_moss_block", "minecraft:stripped_pale_oak_wood"], [3, 2, 1], xx, y_, zz, 3811))
    W.cilinder(x + 0.5, z + 0.5, PL.KALE_R + 1, y - 1, y - 1, None, mixfn=lambda xx, y_, zz, m: W.kies(
        ["minecraft:pale_moss_block", "minecraft:light_gray_terracotta", "minecraft:stripped_pale_oak_wood", "minecraft:gravel"],
        [4, 2, 1, 1], xx, y_, zz, 3812))
    W.cilinder(x + 0.5, z + 0.5, PL.KALE_R + 1, y, y + 24, LUCHT)
    for k in range(9):
        a = k * 2 * math.pi / 9 + 0.3
        p0 = (x + (PL.KALE_R - 1) * math.cos(a), y - 2, z + (PL.KALE_R - 1) * math.sin(a))
        p1 = (x + (PL.KALE_R + 5) * math.cos(a + 0.1), y + 14 + 4 * (k % 3), z + (PL.KALE_R + 5) * math.sin(a + 0.1))
        W.buis([p0, p1], [1.8, 0.9], "minecraft:pale_oak_wood")
    for k in range(30):
        a = k * 2 * math.pi / 30
        W.buis([(x + 15 * math.cos(a), y, z + 15 * math.sin(a)), (x + 16 * math.cos(a + 0.4), y, z + 16 * math.sin(a + 0.4))],
               [0.6, 0.6], "minecraft:stripped_spruce_wood")
    # the ramp from kruin 1 (towards the trunk) onto the mesa
    bx, by, bz = R("arena_kruinroofvogel_bord")
    a = math.atan2(z - bz, x - bx)
    rand = (x - math.cos(a) * (PL.KALE_R - 1), z - math.sin(a) * (PL.KALE_R - 1))
    P.helling(W, (bx, bz), by, rand, y, breed=2.5, vol="minecraft:stripped_pale_oak_wood",
              half="minecraft:pale_oak_slab[type=bottom,waterlogged=false]", hek="minecraft:pale_oak_fence", lucht=4)
    W.wfill(bx - 2, by - 1, bz - 2, bx + 2, by - 1, bz + 2, PLANK)
    for i in (1, 2):
        px, py, pz = R(f"kruinroofvogel_zit_{i}")
        if W.raakt(px - 12, py - 6, pz - 12, px + 12, py + 20, pz + 12):
            W.cilinder(px + 0.5, pz + 0.5, 6, py - 1, py - 1, "minecraft:stripped_pale_oak_wood")
            W.cilinder(px + 0.5, pz + 0.5, 6, py, py + 10, LUCHT)
            W.buis([(px - 5, py - 1, pz), (px - 7, py + 10, pz + 2)], [1.4, 0.7], "minecraft:pale_oak_wood")
            W.buis([(px + 5, py - 1, pz), (px + 6, py + 8, pz - 3)], [1.4, 0.7], "minecraft:pale_oak_wood")


def kruin_leven(W):
    """Blossom curtains under the storey rims, glowberry strands, lantern strings from the branches up to kruin 1."""
    for s, kr in enumerate(B.KRUIN):
        n = [60, 40, 26][s]
        for i in range(n):
            a = 2 * math.pi * i / n + 0.05 * s
            r = kr["R"] * (0.92 + 0.05 * float(hash3(i, s, 0, 3821)))
            x, z = r * math.cos(a), r * math.sin(a)
            t, b, binnen, q = B.kruin_profiel(s, np.array([x]), np.array([z]))
            if not binnen[0]:
                continue
            yb = float(b[0])
            if not W.raakt(x - 3, yb - 16, z - 3, x + 3, yb + 2, z + 3):
                continue
            lengte = 4 + int(10 * float(hash3(i, s, 1, 3822)))
            soort = ["minecraft:cherry_leaves", "minecraft:flowering_azalea_leaves", "minecraft:cave_vines_plant[berries=true]",
                     "minecraft:pale_hanging_moss[tip=false]"][i % 4]
            for k in range(lengte):
                W.wset(x, yb - 1 - k, z, soort, keep=True)
                if soort.endswith("_leaves") and k % 3 == 0:
                    W.wset(x + 1, yb - 1 - k, z, soort, keep=True)
            if i % 5 == 0:
                W.wset(x, yb - 2 - lengte, z, HANGLAMP, keep=True)
    # lantern strings from every big branch (s 70) up to the underside of kruin 1
    for (naam, a, *_r) in B.TAKLIJST:
        x0, y0, z0 = PL.tak_punt(naam, 78)
        x1, z1 = 0.8 * x0, 0.8 * z0
        t, b, binnen, q = B.kruin_profiel(0, np.array([x1]), np.array([z1]))
        y1 = float(b[0]) - 1
        if y1 < y0 + 3 or not W.raakt(min(x0, x1) - 3, y0 - 6, min(z0, z1) - 3, max(x0, x1) + 3, y1 + 2, max(z0, z1) + 3):
            continue
        n = int(math.hypot(x1 - x0, z1 - z0) + abs(y1 - y0)) + 1
        for i in range(1, n):
            tt = i / n
            x = x0 + (x1 - x0) * tt
            z = z0 + (z1 - z0) * tt
            y = y0 + 3 + (y1 - y0 - 3) * tt - 3 * math.sin(math.pi * tt)
            W.wset(x, y, z, "minecraft:iron_chain[axis=y]", keep=True)
            if i % 4 == 0:
                W.wset(x, y - 1, z, [LAMP_R, LAMP_G, LAMP_M][i // 4 % 3], keep=True)


# ================================================================================================ werf rooms
def werf_extra(W):
    cx, cz = WF.C
    if not W.raakt(cx - 90, 120, cz - 90, cx + 90, 260, cz + 90):
        return
    # the bewapeningsloods: a hall (inner x -6..6, z -4..8) with the gate (3 x 4, open) in its +z wall at z + 9
    bx, by, bz = R("werf_kamer_bewapening")
    fy = by - 1
    W.wfill(bx - 7, fy, bz - 5, bx + 7, fy + 7, bz + 9, "minecraft:dark_oak_planks")
    W.wfill(bx - 6, fy + 1, bz - 4, bx + 6, fy + 6, bz + 8, LUCHT)
    W.wfill(bx - 6, fy, bz - 4, bx + 6, fy, bz + 8, "minecraft:spruce_planks")
    W.wfill(bx - 7, fy + 7, bz - 5, bx + 7, fy + 7, bz + 9, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]")
    W.wfill(bx - 1, fy + 1, bz + 9, bx + 1, fy + 4, bz + 9, LUCHT)        # the gate (bazen closes it)
    W.wfill(bx - 1, fy + 1, bz - 5, bx + 1, fy + 3, bz - 5, LUCHT)        # the way in
    W.wset(bx, fy + 6, bz + 2, HANGLAMP)
    for gx_ in (-6, 6):
        W.wset(bx + gx_, fy + 3, bz + 2, "minecraft:ochre_froglight")
    # the ground in front of both doors flush (a pad), so no step
    for dz_ in (-8, -7, -6, 10, 11, 12):
        W.wfill(bx - 2, fy, bz + dz_, bx + 2, fy, bz + dz_, "minecraft:coarse_dirt")
        W.wfill(bx - 2, fy + 1, bz + dz_, bx + 2, fy + 4, bz + dz_, LUCHT)
    for key, muur, extra in (("werf_kamer_gieterij", "minecraft:bricks", "minecraft:magma_block"),
                             ("werf_kamer_touwwerk", "minecraft:dark_oak_planks", None)):
        x, y, z = R(key)
        fy = y - 1
        W.wfill(x - 6, fy, z - 5, x + 6, fy + 6, z + 5, muur)
        W.wfill(x - 5, fy + 1, z - 4, x + 5, fy + 5, z + 4, LUCHT)
        W.wfill(x - 5, fy, z - 4, x + 5, fy, z + 4, "minecraft:spruce_planks")
        for (dx, dz) in ((6, 0), (-6, 0), (0, 5), (0, -5)):
            W.wfill(x + dx - (0 if dx else 1), fy + 1, z + dz - (0 if dz else 1), x + dx + (0 if dx else 1), fy + 3,
                    z + dz + (0 if dz else 1), LUCHT)
        if extra:
            for dx in (-3, 3):
                W.wfill(x + dx, fy, z - 2, x + dx, fy, z + 2, extra)
        else:
            for dz in (-3, 3):
                W.wfill(x - 3, fy + 1, z + dz, x + 3, fy + 1, z + dz, "minecraft:white_wool")
        W.wset(x, fy + 5, z, HANGLAMP)
    # the wreck beside Roquefort's deck + a stair back up onto the deck
    wx_, wy_, wz_ = R("roquefort_wrak")
    if W.raakt(wx_ - 14, wy_ - 10, wz_ - 14, wx_ + 14, wy_ + 8, wz_ + 14):
        W.bol((wx_, wy_ - 4, wz_), 9, None, ry=3.5, rz=6, ymax=wy_ - 2, mixfn=lambda x, y, z, m: W.kies(
            ["guhs:gatenkaas_stenen", "minecraft:dark_oak_planks", "minecraft:yellow_concrete"], [2, 2, 1], x, y, z, 3901))
        W.cilinder(wx_, wz_, 6.5, wy_ - 1, wy_ - 1, "minecraft:dark_oak_planks")
        W.cilinder(wx_, wz_, 6.5, wy_, wy_ + 5, LUCHT)
        x3, y3, z3 = R("arena_roquefort")
        a = math.atan2(z3 - wz_, x3 - wx_)
        P.helling(W, (wx_ + math.cos(a) * 5, wz_ + math.sin(a) * 5), wy_, (x3 - math.cos(a) * 13, z3 - math.sin(a) * 13), y3,
                  breed=1.0, vol="minecraft:dark_oak_planks", half="minecraft:dark_oak_slab[type=bottom,waterlogged=false]",
                  hek="minecraft:dark_oak_fence", lucht=3)
    kx, ky, kz = R("roquefort_kajuit")
    if W.raakt(kx - 8, ky - 2, kz - 8, kx + 8, ky + 8, kz + 8):
        W.wfill(kx - 3, ky, kz - 3, kx + 3, ky + 4, kz + 3, "minecraft:dark_oak_planks")
        W.wfill(kx - 2, ky, kz - 2, kx + 2, ky + 3, kz + 2, LUCHT)
        for (dx, dz) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
            W.wfill(kx + dx, ky, kz + dz, kx + dx, ky + 1, kz + dz, LUCHT)
        W.wset(kx + 2, ky, kz + 2, "minecraft:chest[facing=west,type=single,waterlogged=false]")
        W.wset(kx, ky + 3, kz, HANGLAMP)
    # the entrance: altar pad
    vx, vy, vz = R("werf_vahoeg_altaar")
    W.wfill(vx - 1, vy - 1, vz - 1, vx + 1, vy - 1, vz + 1, "minecraft:dark_oak_planks")
    leeg(W, vx - 1, vy, vz - 1, vx + 1, vy + 2, vz + 1)
    W.wset(vx, vy, vz, ALTAAR)


# ================================================================================================ the Hart dungeon
def hart_kamers():
    """[(id, x, z, r)]: the aderloop start at the hall gate, then the rooms."""
    out = []
    for (naam, deg, kr) in PL.HART_KAMERS:
        x, z = PL.pol(math.radians(deg), PL.HART_R)
        out.append((naam, x, z, kr))
    return out


def hart_extra(W):
    if not W.raakt(-150, 0, -150, 150, 70, 150):
        return
    V = PL.HART_VLOER
    schors = lambda x, y, z, m: np.where(hash3(x, y, z, 3951) < 0.07, W.id("guhs:weidekristal_roze"),
                                         W.kies(B.SCHORS, B.SCHORS_W, x, y, z, 3952))
    vloer = lambda x, y, z, m: W.kies(["minecraft:stripped_spruce_wood", "minecraft:moss_block", "guhs:weidekristal_roze",
                                       "minecraft:stripped_oak_wood"], [5, 2, 0.4, 2], x, y, z, 3953)

    def gang(a, b, y=V, breed=2.0):
        """A corridor (rel x, z) at floor y with an air core 4 high, cut through the rock (lanterns now and then)."""
        L = math.hypot(b[0] - a[0], b[1] - a[1])
        n = max(2, int(L * 2))
        for i in range(n + 1):
            t = i / n
            x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
            if not W.raakt(x - 6, y - 3, z - 6, x + 6, y + 8, z + 6):
                continue
            W.cilinder(x, z, breed + 0.5, y, y, None, mixfn=vloer)
            W.cilinder(x, z, breed + 0.5, y + 1, y + 4, LUCHT)
            W.cilinder(x, z, breed + 1.5, y + 5, y + 5, None, mixfn=schors)
            if i % 16 == 8:
                W.wset(x, y + 4, z, "minecraft:soul_lantern[hanging=true,waterlogged=false]")

    def kamer(x, z, kr, hh, soort):
        if not W.raakt(x - kr - 4, V - 3, z - kr - 4, x + kr + 4, V + hh + 4, z + kr + 4):
            return
        W.cilinder(x, z, kr + 2.5, V - 1, V + hh + 1, None, mixfn=schors)
        W.cilinder(x, z, kr + 0.5, V + 1, V + hh, LUCHT)
        if soort == "jaarring":
            W.cilinder(x, z, kr + 0.5, V, V, None, mixfn=lambda xx, yy, zz, m: np.where(
                (np.hypot(xx - x, zz - z) // 3) % 2 == 0, W.id("minecraft:stripped_oak_wood"), W.id("minecraft:stripped_spruce_wood")))
        else:
            W.cilinder(x, z, kr + 0.5, V, V, None, mixfn=vloer)
        for k in range(8):
            a = k * math.pi / 4
            W.wset(x + (kr - 0.5) * math.cos(a), V + hh - 1, z + (kr - 0.5) * math.sin(a),
                   "minecraft:pearlescent_froglight" if k % 2 else "guhs:weidekristal_roze")
            W.wset(x + (kr + 0.4) * math.cos(a + 0.4), V + 3, z + (kr + 0.4) * math.sin(a + 0.4),
                   "minecraft:soul_lantern[hanging=false,waterlogged=false]")

    # the rooms first (their root walls), then the corridors through them: hall gate -> aderloop -> rooms in walking order
    hx, hy, hz = R("hart_poort")
    kamers = hart_kamers()
    for (naam, x, z, kr) in kamers:
        hh = 16 if naam in ("wortelwurger", "nestwachter", "baron_bleu") else 11
        kamer(x, z, kr, hh, "jaarring" if naam in ("nestwachter", "ringenzaal") else "")
        if naam == "schimmelkwekerij" and W.raakt(x - 14, V - 2, z - 14, x + 14, V + 8, z + 14):
            for k in range(3):
                a = k * 2 * math.pi / 3 + 0.5
                kx, kz = x + 8 * math.cos(a), z + 8 * math.sin(a)
                W.cilinder(kx, kz, 2.2, V + 1, V + 4, "minecraft:iron_block")
                W.cilinder(kx, kz, 2.4, V + 3, V + 3, "minecraft:light_blue_terracotta", hollow=0.8)
    start = (hx - 6, hz)
    gang((hx + 2, hz), start, y=hy - 1)
    P.helling(W, (start[0] + 1, start[1]), hy, (start[0] - 18, start[1]), V + 1, breed=2.0, vol="minecraft:stripped_spruce_wood",
              half="minecraft:spruce_slab[type=bottom,waterlogged=false]", hek=None, lucht=4)
    vorige = (start[0] - 18, start[1])
    for i, (naam, x, z, kr) in enumerate(kamers):
        gang(vorige, (x, vorige[1]))
        gang((x, vorige[1]), (x, z))
        vorige = (x, z)
        if naam in ("schimmelkwekerij", "ringenzaal"):
            (gx, gz), zs = PL.hart_poort_van(naam)
            gang((x, z), (x, gz + zs * 3), breed=1.0)        # 3 wide at the gate (bazen fills x-1..1, 4 high)
            if W.raakt(gx - 5, V - 2, gz - 5, gx + 5, V + 8, gz + 5):
                W.wfill(gx - 3, V + 5, gz, gx + 3, V + 6, gz, "minecraft:stripped_dark_oak_wood")
                for s_ in (-2, 2):
                    W.wfill(gx + s_, V + 1, gz, gx + s_, V + 4, gz, "minecraft:stripped_dark_oak_log[axis=y]")
            vorige = (x, gz + zs * 3)
    # Baron Bleu -> the Nestkamer: down under the hall floor and up into the egg basket from below
    naam, bx, bz, kr = kamers[-1]
    diep = 9
    inwaarts = (bx - kr - 2, bz)
    gang((bx, bz), inwaarts)
    P.helling(W, inwaarts, V + 1, (inwaarts[0] - 30, bz), diep + 1, breed=2.0, vol="minecraft:stripped_spruce_wood",
              half="minecraft:spruce_slab[type=bottom,waterlogged=false]", hek=None, lucht=4)
    for x in np.arange(inwaarts[0] - 30, 2.5, -0.5):
        if W.raakt(x - 4, diep - 3, bz - 4, x + 4, diep + 8, bz + 4):
            W.wfill(x, diep - 1, bz - 3, x, diep + 5, bz + 3, "minecraft:mangrove_roots")
            W.wfill(x, diep + 1, bz - 2, x, diep + 4, bz + 2, LUCHT)
            W.wfill(x, diep, bz - 2, x, diep, bz + 2, "minecraft:stripped_spruce_wood")
            if int(x * 2) % 24 == 0:
                W.wset(x, diep + 4, bz, "minecraft:soul_lantern[hanging=true,waterlogged=false]")
    # up into the basket (its floor at ~20) by a ladder through the floor (never reachable from the hall)
    hy0 = int(B.HART[1] - B.HART[3] * 0.72)
    lx_, lz_ = 3, int(math.floor(bz))
    if W.raakt(lx_ - 3, diep - 2, lz_ - 3, lx_ + 3, hy0 + 4, lz_ + 3):
        for y in range(diep + 1, hy0 + 1):
            W.wset(lx_, y, lz_ - 1, "minecraft:stripped_spruce_wood")
            W.wset(lx_, y, lz_, "minecraft:ladder[facing=south,waterlogged=false]")
            W.wset(lx_, y, lz_ + 1, LUCHT)
        W.wset(lx_, hy0 + 1, lz_, LUCHT)
        W.wset(lx_, hy0 + 2, lz_, LUCHT)
    ex, ey, ez = 0, hy0, 0
    if W.raakt(-16, hy0 - 2, -16, 16, hy0 + 8, 16):
        W.cilinder(ex, ez, 11.5, hy0, hy0, None, hollow=5.5, mixfn=lambda x, y, z, m: W.kies(
            ["minecraft:stripped_spruce_wood", "guhs:weidekristal_roze", "minecraft:moss_block"], [5, 1, 2], x, y, z, 3961))
        W.cilinder(ex, ez, 11.5, hy0 + 1, hy0 + 4, LUCHT, hollow=5.5)
    # the hall gate: frame, cave lanterns (soul = unlit), board + altar
    if W.raakt(hx - 10, hy - 4, hz - 10, hx + 10, hy + 10, hz + 10):
        for s_ in (-3, 3):
            W.wfill(hx, hy, hz + s_, hx, hy + 5, hz + s_, "minecraft:stripped_dark_oak_log[axis=y]")
        W.wfill(hx, hy + 6, hz - 3, hx, hy + 6, hz + 3, "minecraft:stripped_dark_oak_wood")
        W.wset(hx + 1, hy + 5, hz, LAMP_R)
        for k in range(1, 5):
            lx, ly, lz = R(f"hart_grotlantaarn_{k}")
            W.wfill(lx, ly - 1, lz, lx, ly - 1, lz, "minecraft:stripped_spruce_wood")
            W.wfill(lx, ly, lz, lx, ly + 1, lz, "minecraft:dark_oak_fence")
            W.wset(lx, ly + 2, lz, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
        x, y, z = R("hart_vahoeg_altaar")
        W.wfill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, "minecraft:stripped_spruce_wood")
        leeg(W, x - 1, y, z - 1, x + 1, y + 2, z + 1)
        W.wset(x, y, z, ALTAAR)
        x, y, z = R("hart_bord")
        W.wfill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, "minecraft:stripped_spruce_wood")
        leeg(W, x - 1, y, z - 1, x + 1, y + 2, z + 1)
    # cave lanterns along the Wortelpoort tunnel
    for k in range(1, 10):
        key = f"grotlantaarn_{k}"
        if key not in FL():
            continue
        x, y, z = R(key)
        if W.raakt(x - 2, y - 2, z - 2, x + 2, y + 4, z + 2):
            W.wset(x, y - 1, z, "minecraft:stripped_spruce_wood")
            W.wset(x, y, z, "minecraft:dark_oak_fence")
            W.wset(x, y + 1, z, "minecraft:soul_lantern[hanging=false,waterlogged=false]")


# ================================================================================================ the ground
def grond_extra(W):
    if W.b[1] > 120:
        return
    ringpad(W)
    mosvoet(W)
    pluimstal(W)
    for i, (deg, r) in enumerate(((205, 150), (195, 168), (215, 172), (185, 155))):
        x, y, z = R(f"schimmelketel_{i + 1}")
        if W.raakt(x - 4, y - 3, z - 4, x + 4, y + 4, z + 4):
            W.wset(x - 2, y, z, "minecraft:cauldron")
            W.wset(x - 2, y + 1, z, "minecraft:brown_mushroom")
            W.wfill(x - 3, y - 1, z - 1, x - 1, y - 1, z + 1, "minecraft:mycelium")
    x, y, z = R("uitkijk_wortelgewelf")
    g = PL.gstand(x, z)
    if W.raakt(x - 6, g - 3, z - 6, x + 6, y + 6, z + 6):
        for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
            W.wfill(x + dx, g - 1, z + dz, x + dx, y - 1, z + dz, "minecraft:mossy_stone_bricks")
        W.wfill(x - 3, y - 1, z - 3, x + 3, y - 1, z + 3, "minecraft:spruce_planks")
        leeg(W, x - 3, y, z - 3, x + 3, y + 3, z + 3)
        for yy in range(g, y):
            W.wset(x, yy, z, "minecraft:mossy_stone_bricks")
            W.wset(x, yy, z + 1, "minecraft:ladder[facing=south,waterlogged=false]")
            W.wset(x, yy, z + 2, LUCHT)
        W.wset(x, y - 1, z + 1, LUCHT)
        W.wset(x, y, z + 1, LUCHT)
        for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
            W.wset(x + dx, y, z + dz, HEK)
        W.wset(x - 3, y + 1, z - 3, LANTAARN)


def ringpad(W):
    """The ground ring path (r 172) with lantern posts, plus spokes to the two gates and the Wortelpoort."""
    rr = W.RR
    band = np.abs(rr - PL.WORTELWEG_R) < 2.2
    spokes = []
    for a in B.POORT_HOEKEN:
        spokes.append((a, PL.poort_uit_r(a) + 6, PL.WORTELWEG_R))
    spokes.append((0.0, B.POORT[0] + 16, PL.WORTELWEG_R))
    for (a, r0, r1) in spokes:
        d = np.abs(np.sin(W.TH - a)) * rr
        band |= (d < 1.8) & (np.cos(W.TH - a) > 0) & (rr > r0) & (rr < r1)
    rand = B.eilandrand(W.TH)
    band &= rr < rand - 4
    if not band.any():
        return
    g = np.floor(B.grondhoogte(W.XX, W.ZZ))
    W.kolom(g, g, W.kies(["minecraft:dirt_path", "minecraft:dirt_path", "minecraft:coarse_dirt"], [5, 3, 1], W.XX, 0, W.ZZ, 4001),
            mask=band)
    W.kolom(g + 1, g + 4, LUCHT, mask=band)
    lamp = (np.abs(rr - PL.WORTELWEG_R - 3.2) < 0.5) & (hash3(np.round(W.TH * 140), 0, 0, 4002) < 0.06) & (rr < rand - 4)
    W.kolom(g + 1, g + 2, HEK, mask=lamp)
    W.kolom(g + 3, g + 3, LANTAARN, mask=lamp)
    for i in range(1, 5):
        x, y, z = R(f"pluimpost_{i}")
        if W.raakt(x - 5, y - 3, z - 5, x + 5, y + 6, z + 5):
            W.wfill(x - 2, y - 1, z - 2, x + 2, y - 1, z + 2, PLANK)
            leeg(W, x - 2, y, z - 2, x + 2, y + 3, z + 2)
            W.wfill(x + 2, y, z + 2, x + 2, y + 2, z + 2, HEK)
            W.wset(x + 2, y + 3, z + 2, LAMP_G)
            W.wset(x - 2, y, z + 2, "minecraft:hay_block")


def mosvoet(W):
    mx, mz = PL.MOSVOET
    if not W.raakt(mx - 34, 40, mz - 34, mx + 34, 110, mz + 34):
        return
    g = PL.gstand(mx, mz)
    W.cilinder(mx, mz, 9, g - 1, g - 1, None, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:mossy_stone_bricks", "minecraft:stone_bricks", "minecraft:moss_block"], [3, 2, 1], x, y, z, 4101))
    W.cilinder(mx, mz, 9, g, g + 5, LUCHT)
    W.wset(mx, g, mz, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    for i, f in enumerate(("burgemeester", "theehuis", "winkel", "stalpost", "waterrad", "woning")):
        a = i * math.pi / 3 + 0.3
        hx, hz = mx + 17 * math.cos(a), mz + 17 * math.sin(a)
        gg = PL.gstand(hx, hz)
        deur = a + math.pi
        # a flat plank pad under the house and a path to the plaza
        W.cilinder(hx, hz, 6.5, gg - 3, gg - 2, "minecraft:dirt")
        W.cilinder(hx, hz, 6.5, gg - 1, gg - 1, PLANK)
        W.cilinder(hx, hz, 6.5, gg, gg + 12, LUCHT)
        if i % 2 == 0:
            P.skep(W, hx, gg, hz, deur, soort=i, R=4.8)
        else:
            P.raathuis(W, hx, gg, hz, deur, R=4.4, h=6, dak=["minecraft:pink_wool", "minecraft:orange_wool"][i % 2])
        interieur(W, hx, gg, hz, deur, "woning" if f in ("woning", "burgemeester") else "algemeen", 4.4)
        P.helling(W, (hx + math.cos(deur) * 5.5, hz + math.sin(deur) * 5.5), gg, (mx - math.cos(deur) * 8.5, mz - math.sin(deur) * 8.5),
                  g, breed=1.0, vol="minecraft:dirt_path", half="minecraft:mud_brick_slab[type=bottom,waterlogged=false]", hek=None,
                  lucht=3, grond=lambda x, z: grond_f(x, z))
        if f == "waterrad":
            W.buis([(hx + 6, gg + 4, hz - 3), (hx + 6, gg + 4, hz + 3)], [3.5, 3.5], None, mixfn=lambda x, y, z, m: W.kies(
                ["minecraft:stripped_spruce_wood", "minecraft:spruce_planks"], [1, 1], x, y, z, 4102))
    for (x, z, top, sx, sz) in PL.huis_daken()[-6:]:
        if W.raakt(sx - 2, top - 5, sz - 2, sx + 2, top + 3, sz + 2):
            schoorsteen(W, sx, sz, top)


def pluimstal(W):
    sx, sz = PL.PLUIMSTAL
    if not W.raakt(sx - 60, 40, sz - 60, sx + 60, 110, sz + 60):
        return
    sa = math.atan2(sz, sx)
    ox, oz = math.cos(sa), math.sin(sa)
    tx, tz = -oz, ox
    g = PL.gstand(sx, sz)
    # a long stable (22 x 12) along the ring, door towards the trunk, 8 stalls with fence gates, hay, water troughs
    for d in np.arange(-11, 11.01, 0.5):
        for w_ in np.arange(-6, 6.01, 0.5):
            x, z = sx + tx * d + ox * w_, sz + tz * d + oz * w_
            wand = abs(d) > 10.4 or abs(w_) > 5.4
            W.wfill(x, g - 4, z, x, g - 2, z, "minecraft:dirt")
            W.wset(x, g - 1, z, "minecraft:spruce_planks")
            W.wfill(x, g, z, x, g + 5, z, "minecraft:white_terracotta" if wand else LUCHT)
            W.wset(x, g + 6 + int((6 - abs(w_)) * 0.6), z, "minecraft:orange_terracotta")
            if not wand and abs(w_) > 2.4 and abs(d) % 5.5 < 0.6:
                W.wset(x, g, z, "minecraft:spruce_fence")
            if not wand and abs(w_) > 4.4:
                W.wset(x, g, z, "minecraft:hay_block" if int(d) % 3 else "minecraft:water_cauldron[level=3]")
    for w_ in np.arange(-1.5, 1.51, 0.5):
        for h in (0, 1, 2):
            W.wset(sx - ox * 6 + tx * w_, g + h, sz - oz * 6 + tz * w_, LUCHT)
            W.wset(sx + ox * 6 + tx * w_, g + h, sz + oz * 6 + tz * w_, LUCHT)
    for d in (-8, 0, 8):
        W.wset(sx + tx * d, g + 4, sz + tz * d, HANGLAMP)
    # the starter pen and the training track (an oval of fences round a path)
    kx, ky, kz = R("pluimstal_keuze")
    W.cilinder(kx + ox * 6, kz + oz * 6, 10, ky - 1, ky - 1, "minecraft:coarse_dirt")
    W.cilinder(kx + ox * 6, kz + oz * 6, 10, ky, ky + 4, LUCHT)
    ex, ey, ez = R("pluimstal_oefenbaan")
    for k in range(72):
        b = k * 2 * math.pi / 72
        for rr_, blok in ((14.5, "minecraft:dirt_path"), (12.5, "minecraft:dirt_path"), (13.5, "minecraft:dirt_path")):
            x, z = ex + rr_ * 1.6 * math.cos(b), ez + rr_ * math.sin(b)
            gg = grond_f(x, z)
            W.wset(x, gg, z, blok)
            W.wfill(x, gg + 1, z, x, gg + 3, z, LUCHT)
        x, z = ex + 16.5 * 1.6 * math.cos(b), ez + 16.5 * math.sin(b)
        if k % 18 != 0:
            W.wset(x, grond_f(x, z) + 1, z, HEK)
        x, z = ex + 10.5 * 1.6 * math.cos(b), ez + 10.5 * math.sin(b)
        W.wset(x, grond_f(x, z) + 1, z, HEK)


# ================================================================================================ secrets + campfires + paths
def geheimen(W):
    for k in FL():
        if not (k.startswith("geheim_") or k.startswith("imker_aanwijzing") or k.startswith("muurverhaal_")
                or k.startswith("boomvolk_lichtje")):
            continue
        x, y, z = R(k)
        if not W.raakt(x - 3, y - 3, z - 3, x + 3, y + 4, z + 3):
            continue
        if W.wget(x, y - 1, z) == LUCHT:
            W.wfill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, PLANK, keep=True)
        leeg(W, x, y, z, x, y + 1, z)
        if k.startswith("muurverhaal_"):
            a = math.atan2(z, x)
            for dy in range(1, 5):
                for w in (-1, 0, 1):
                    W.wset((B.R_IN + 0.6) * math.cos(a) - math.sin(a) * w, y + dy, (B.R_IN + 0.6) * math.sin(a) + math.cos(a) * w,
                           "minecraft:chiseled_tuff" if (dy + w) % 2 else "guhs:weidekristal_roze")
        if k.startswith("boomvolk_lichtje"):
            a = math.atan2(z, x)
            W.wset((B.R_IN + 0.6) * math.cos(a), y + 2, (B.R_IN + 0.6) * math.sin(a), "minecraft:ochre_froglight")


def kampvuren(W):
    for k in FL():
        if not k.startswith("kampvuur_"):
            continue
        x, y, z = R(k)
        if not W.raakt(x - 4, y - 3, z - 4, x + 4, y + 5, z + 4):
            continue
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) + abs(dz) > 3:
                    continue
                if W.wget(x + dx, y - 1, z + dz) in (LUCHT, "minecraft:water"):
                    W.wset(x + dx, y - 1, z + dz, "minecraft:stripped_spruce_wood")
                for h in (0, 1, 2):
                    W.wset(x + dx, y + h, z + dz, LUCHT)
        W.wset(x, y, z, KAMPVUUR)
        W.wset(x - 2, y, z, "minecraft:stripped_spruce_log[axis=z]")
        W.wset(x + 2, y, z, "minecraft:stripped_spruce_log[axis=z]")


def pad_vrij(W):
    """Paths stay free: nothing solid in the 3 cells above a dirt-path block (plumes, lamps, roots, leaves)."""
    if "minecraft:dirt_path" not in W.idx:
        return
    pid = W.idx["minecraft:dirt_path"]
    laat = {W.idx[s] for s in W.idx if any(w in s for w in ("lantern", "lampion", "chain", "ladder", "campfire", "petals",
                                                          "carpet", "slab", "stairs", "torch", "z3kv", "fence", "wall"))}
    laat.add(0)
    xs, ys, zs = np.nonzero(W.v == pid)
    for dy in (1, 2, 3):
        yy = ys + dy
        ok = yy < W.v.shape[1]
        a, b, c = xs[ok], yy[ok], zs[ok]
        cur = W.v[a, b, c]
        weg = ~np.isin(cur, list(laat)) & (cur != pid)
        W.v[a[weg], b[weg], c[weg]] = 0


def bouw(W):
    windkolommen(W)
    honingtak_extra(W)
    schorsstad_extra(W)
    rijpad_extra(W)
    kruin_extra(W)
    werf_extra(W)
    hart_extra(W)
    grond_extra(W)
    pad_vrij(W)
    geheimen(W)
    kampvuren(W)


init()
