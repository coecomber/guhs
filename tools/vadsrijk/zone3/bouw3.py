"""Zone 3 "De Schemerwortel": the GAME parts on top of the render kit (one tile window W at a time, rel coordinates).

Every spot comes from plan3.plekken() (the same list as POI_Z3.md / Z3WereldPlekken), converted back to rel blocks with R(id),
so the built map and the published coordinates cannot drift apart.
  voor_kit(W)  before honingtak/plekken: nothing yet (hook)
  bouw(W)      after the kit: cloud sea, wind columns, honey lifts + Honinglift, Honingtak interiors + cell doors, the Mika-imker
               hall, Kale Kruin + perches, grey patches, glide platforms, viewpoint, Pelszaal, spiral balconies, the
               Kaaszeppelinwerf (dungeon 1), the Hart van de Boom ring (dungeon 2, rooms empty for the bazen slice), the
               Hartslag-zaal floor, the Wortelweg (path + root tunnels), Mosvoet floor, secrets, campfires (last).
Facings are in rel terms (rel +x = "east"); maak3 mirrors the states into the world.
"""
import math
import functools

import numpy as np

import plan3 as PL
import boom
from schemerkit import hash3, vnoise

PLANK, HOUT, HEK = "minecraft:spruce_planks", "minecraft:stripped_spruce_wood", "minecraft:spruce_fence"
LAMP_R, LAMP_G, LAMP_M = "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"
KAMPVUUR = "minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false,z3kv=1]"
LUCHT = "minecraft:air"


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
    return 2299 - X, Y, Z


def leeg(W, x1, y1, z1, x2, y2, z2):
    W.wfill(x1, y1, z1, x2, y2, z2, LUCHT)


def vloer_rond(W, cx, y, cz, r, blok=PLANK, hek=False, gat=None):
    W.cilinder(cx + 0.5, cz + 0.5, r, y, y, blok)
    if hek:
        W.cilinder(cx + 0.5, cz + 0.5, r, y + 1, y + 1, HEK, hollow=1)
        if gat:
            for (gx, gz) in gat:
                leeg(W, gx - 1, y + 1, gz - 1, gx + 1, y + 1, gz + 1)


def loopbrug(W, a, b, y, breed=1, blok=PLANK, hekken=True):
    """Straight plank walkway (rel x/z ints) at floor y from a to b, with fences on both sides and 3 air above."""
    n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
    dx, dz = (b[0] - a[0]) / max(1, n), (b[1] - a[1]) / max(1, n)
    nx, nz = -dz, dx
    ln = math.hypot(nx, nz) or 1
    nx, nz = nx / ln, nz / ln
    for i in range(n + 1):
        x, z = a[0] + dx * i, a[1] + dz * i
        for w in range(-breed, breed + 1):
            W.wset(x + nx * w, y, z + nz * w, blok)
            for h in (1, 2, 3):
                W.wset(x + nx * w, y + h, z + nz * w, LUCHT)
        if hekken:
            W.wset(x + nx * (breed + 1), y + 1, z + nz * (breed + 1), HEK, keep=True)
            W.wset(x - nx * (breed + 1), y + 1, z - nz * (breed + 1), HEK, keep=True)


def trap_recht(W, a, ya, b, yb, breed=1, blok="minecraft:spruce_planks"):
    """A real staircase between floor (a, ya) and floor (b, yb) (rel x/z): one block up per block forward, stair blocks facing
    the way up, air above every step (also through decks above it)."""
    if yb < ya:
        a, ya, b, yb = b, yb, a, ya
    dy = yb - ya
    if dy == 0:
        return loopbrug(W, a, b, ya, breed)
    L = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
    n = max(abs(dy), int(L))
    ux, uz = (b[0] - a[0]) / max(1e-9, L), (b[1] - a[1]) / max(1e-9, L)
    if abs(ux) >= abs(uz):
        f = "east" if ux > 0 else "west"
    else:
        f = "south" if uz > 0 else "north"
    ln = math.hypot(ux, uz) or 1
    nx, nz = -uz / ln, ux / ln
    for i in range(n + 1):
        t = i / n
        x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
        y = ya + round(dy * t)
        nxt = ya + round(dy * min(1, (i + 1) / n))
        for w in range(-breed, breed + 1):
            px, pz = x + nx * w, z + nz * w
            W.wset(px, y, pz, blok)
            if nxt > y:
                W.wset(px, y + 1, pz, f"minecraft:spruce_stairs[facing={f},half=bottom,shape=straight,waterlogged=false]")
                for h in (2, 3, 4):
                    W.wset(px, y + h, pz, LUCHT)
            else:
                for h in (1, 2, 3):
                    W.wset(px, y + h, pz, LUCHT)


# ================================================================================================ cloud sea
ZEE_Y = 62


def zee(W):
    zee_m = W.RR > 560
    if not zee_m.any():
        return
    # a thin sea (3-4 deep: a fall from the crown lands soft; thin keeps the tiles small) over a sand floor at y 58; the island
    # shore slopes down to it (kit boom.grondhoogte), so you can always wade out
    W.kolom(np.full(W.XX.shape, ZEE_Y - 4.0), np.full(W.XX.shape, ZEE_Y - 4.0), "minecraft:sand", mask=zee_m, keep=True)
    W.kolom(np.full(W.XX.shape, ZEE_Y - 3.0), np.full(W.XX.shape, float(ZEE_Y)), "minecraft:water", mask=zee_m, keep=True)
    # cloud puffs floating on the sea (soft, walkable; the zone edge is a ring of them + barrier)
    cel = 40
    x1, _, z1, x2, _, z2 = W.b
    for ci in range(int(math.floor(x1 / cel)) - 1, int(math.floor(x2 / cel)) + 2):
        for cj in range(int(math.floor(z1 / cel)) - 1, int(math.floor(z2 / cel)) + 2):
            h = float(hash3(ci, 0, cj, 3301))
            if h > 0.55:
                continue
            cx = ci * cel + cel * float(hash3(ci, 1, cj, 3302))
            cz = cj * cel + cel * float(hash3(ci, 2, cj, 3303))
            rr = math.hypot(cx, cz)
            if rr < float(boom.eilandrand(np.array([math.atan2(cz, cx)]))[0]) + 45:
                continue
            r = 8 + 10 * float(hash3(ci, 3, cj, 3304))
            W.bol((cx, ZEE_Y + 1, cz), r, None, ry=r * 0.3, ymin=ZEE_Y + 1, keep=True, mixfn=lambda x, y, z, m: W.kies(
                ["minecraft:white_wool", "guhs:pluiswolblok", "minecraft:white_concrete_powder", "minecraft:pink_wool"],
                [5, 3, 1, 0.3], x, y, z, 3305))
    # the zone edge: an invisible wall around the tile grid
    for (xa, xb, za, zb) in ((PL.rel_x(PL.TX0 * 64), PL.rel_x(PL.TX0 * 64), -704, 703),
                             (PL.rel_x(PL.TX1 * 64 + 63), PL.rel_x(PL.TX1 * 64 + 63), -704, 703),
                             (PL.rel_x(PL.TX1 * 64 + 63), PL.rel_x(PL.TX0 * 64), -704, -704),
                             (PL.rel_x(PL.TX1 * 64 + 63), PL.rel_x(PL.TX0 * 64), 703, 703)):
        W.wfill(xa, ZEE_Y - 6, za, xb, ZEE_Y + 14, zb, "minecraft:barrier")


# ================================================================================================ wind columns
def windkolommen(W):
    for (wid, x, z, y0, y1, r, lx, lz) in PL.windkolommen():
        if not W.raakt(x - 14, y0 - 3, z - 14, x + 14, y1 + 8, z + 14) and not W.raakt(lx - 6, y1 - 3, lz - 6, lx + 6, y1 + 5, lz + 6):
            continue
        # through the crown: a blossom tube round the shaft (so nobody drifts into the hollow crown), plus for the spiral column a
        # ladder shaft beside it (the way down)
        if y1 > 200 and math.hypot(x, z) < 380:
            yb = max(y0 + 3, 186)
            W.cilinder(x, z, r + 1.8, yb, y1 - 1, None, hollow=1.2, mixfn=lambda xx, yy, zz, m: W.kies(
                ["minecraft:flowering_azalea_leaves", "minecraft:cherry_leaves", "minecraft:stripped_cherry_wood"], [3, 2, 0.4], xx, yy, zz, 3402))
        if wid == "spiraal_kruin":
            lx_ = x + r + 3
            W.wfill(lx_ + 1, y0, z - 1, lx_ + 1, y1 - 1, z + 1, "minecraft:stripped_spruce_wood")
            W.wfill(lx_, y0, z - 1, lx_, y1 + 2, z + 1, "minecraft:stripped_spruce_wood")
            for yy in range(y0, y1):
                W.wset(lx_, yy, z, "minecraft:ladder[facing=west,waterlogged=false]")
            W.wset(lx_, y1, z, LUCHT)
            W.wset(lx_, y1 + 1, z, LUCHT)
            W.wfill(lx_ - 1, y0 - 1, z - 1, lx_ - 1, y1 + 2, z + 1, "minecraft:stripped_spruce_wood")
            W.wfill(lx_ - 1, y0, z, lx_ - 1, y0 + 1, z, LUCHT)
            W.wfill(lx_ - 1, y1, z, lx_ - 1, y1 + 1, z, LUCHT)
            W.wfill(x - 6, y0 - 1, z - 6, x + 6, y0 - 1, z + 14, PLANK)
            leeg(W, x - 6, y0, z - 6, x + 6, y0 + 3, z + 14)
            for xx in range(int(x) + 2, int(lx_) + 3):
                for zz in range(int(z) - 3, int(z) + 4):
                    if not (xx == int(lx_) and zz == int(z)):
                        W.wset(xx, y1 - 1, zz, PLANK)
        # the shaft: free air up to the top (through leaves / plumes)
        W.cilinder(x, z, r + 0.6, y0, y1 + 6, LUCHT)
        # bottom: a blossom ring on the ground (petals, lanterns), flush with the ground
        W.cilinder(x, z, r + 1.6, y0 - 1, y0 - 1, None, mixfn=lambda xx, yy, zz, m: W.kies(
            ["minecraft:moss_block", "guhs:vadsmos", "minecraft:pink_terracotta"], [3, 2, 1], xx, yy, zz, 3401))
        W.cilinder(x, z, r + 0.4, y0, y0, "minecraft:pink_petals[facing=north,flower_amount=4]")
        for k in range(4):
            a = k * math.pi / 2 + 0.4
            px, pz = x + (r + 2.6) * math.cos(a), z + (r + 2.6) * math.sin(a)
            W.wfill(px, y0, pz, px, y0 + 2, pz, "minecraft:stripped_cherry_wood")
            W.wset(px, y0 + 3, pz, LAMP_R)
        # rings of blossom every 12 blocks on the way up (so you see the column from afar), outside the shaft
        for yy in range(y0 + 10, y1 - 4, 12):
            for k in range(6):
                a = k * math.pi / 3 + yy * 0.3
                W.wset(x + (r + 1.6) * math.cos(a), yy, z + (r + 1.6) * math.sin(a), "minecraft:flowering_azalea_leaves", keep=True)
        # top: a landing deck from the shaft edge to the landing point, at the landing's floor
        d = math.hypot(lx - x, lz - z) or 1
        ux, uz = (lx - x) / d, (lz - z) / d
        a = (x + ux * (r + 1), z + uz * (r + 1))
        loopbrug(W, (round(a[0]), round(a[1])), (round(lx), round(lz)), y1 - 1, breed=2, hekken=False)
        W.cilinder(lx, lz, 3.2, y1 - 1, y1 - 1, PLANK)
        W.cilinder(lx, lz, 3.2, y1, y1 + 3, LUCHT)


# ================================================================================================ honey lifts
def liftjes(W):
    for (lid, x, z, yo) in PL.LIFTJES:
        if not W.raakt(x - 6, yo - 3, z - 30, x + 6, 250, z + 30):
            continue
        s = -1 if z < 0 else 1
        yc, r, zc = PL.tak_oost(x)
        hw = r * 0.62 + 4
        # the shaft: clear, two chains, a top beam
        leeg(W, x - 1, yo, z - 1, x + 1, PL.DEK_Y + 4, z + 1)
        for y in range(yo, PL.DEK_Y + 6):
            W.wset(x - 2, y, z, "minecraft:iron_chain[axis=y]")
            W.wset(x + 2, y, z, "minecraft:iron_chain[axis=y]")
        W.wfill(x - 2, PL.DEK_Y + 6, z, x + 2, PL.DEK_Y + 6, z, "minecraft:stripped_spruce_log[axis=x]")
        W.wset(x, PL.DEK_Y + 5, z, "minecraft:bee_nest[facing=south,honey_level=5]")
        # top: platform around the shaft top (the bucket), walkway to the deck edge
        W.wfill(x - 2, PL.DEK_Y, z - 2, x + 2, PL.DEK_Y, z + 2, "minecraft:honeycomb_block")
        loopbrug(W, (x, z - s * 2), (x, round(zc + s * (hw - 1))), PL.DEK_Y, breed=1)
        W.wfill(x - 2, PL.DEK_Y + 1, z + s * 3, x + 2, PL.DEK_Y + 1, z + s * 3, HEK)
        W.wset(x + 2, PL.DEK_Y + 2, z + s * 2, LAMP_G)
        # bottom landing
        W.wfill(x - 2, yo - 1, z - 2, x + 2, yo - 1, z + 2, "minecraft:honeycomb_block")
        leeg(W, x - 2, yo, z - 2, x + 2, yo + 3, z + 2)
        W.wfill(x - 3, yo, z + s * 3, x + 3, yo, z + s * 3, HEK)
        W.wset(x - 2, yo + 2, z + s * 2, LAMP_R, keep=True)


def cel_deur(W, cx, yb, cz, R, zijde, tot_z):
    """A door in a hanging raatcel wall (side zijde along z) and a plank bridge from it to z = tot_z at the floor level."""
    w0, w1 = 1.155 * (R - 1.3), 1.155 * R + 0.6
    for d in np.arange(w0 - 0.5, w1 + 0.5, 0.5):
        for dx in (-1, 0, 1):
            W.wset(cx + dx, yb, cz + zijde * d, PLANK)
            for h in (1, 2, 3):
                W.wset(cx + dx, yb + h, cz + zijde * d, LUCHT)
    loopbrug(W, (cx, round(cz + zijde * (w1 + 0.5))), (cx, tot_z), yb, breed=1)


def honingtak_toegang(W):
    """Doors + bridges: the cafe, the Nestkapel and the Raattros bottom cells to their lift landings and to the spiral stairs."""
    if not W.raakt(380, 150, -60, 640, 245, 60):
        return
    for (lid, x, z, yo), cel in zip(PL.LIFTJES, (PL.CAFE_CEL, PL.KAPEL_CEL, PL.WOON_CEL)):
        cx, yb, cz, Rr = cel
        s = -1 if z < 0 else 1
        cel_deur(W, cx, yb, cz, Rr, s, z - s * 3)
        # the tros spiral stair bottom (kit: at cx - 12, zc + r + 3) -> a bridge to the cell
        yc, r, zc = PL.tak_oost(cx)
        sx, sz = cx - 12, round(zc + r + 3)
        if s < 0:
            cel_deur(W, cx, yb, cz, Rr, 1, sz)
        loopbrug(W, (sx + 1, sz), (cx - 2, sz), yb, breed=1)
        W.wfill(sx - 1, yb, sz - 1, sx + 1, yb, sz + 1, PLANK)
        W.wfill(cx - 3, yb, sz - 1, cx + 1, yb, sz + 1, PLANK)
        leeg(W, cx - 3, yb + 1, sz - 1, cx + 1, yb + 3, sz + 1)          # the junction of the two bridges stays open
        leeg(W, sx + 1, yb + 1, sz - 1, sx + 2, yb + 3, sz + 1)


def honinglift(W):
    bx, by, bz = R("honinglift_boven")
    ox, oy, oz = R("honinglift_onder")
    # top station: a balcony beside the deck with a roof frame
    if W.raakt(bx - 12, by - 6, bz - 20, bx + 12, by + 10, bz + 16):
        yc, r, zc = PL.tak_oost(bx)
        hw = r * 0.62 + 4
        W.wfill(bx - 4, by - 1, round(zc + hw - 1), bx + 6, by - 1, bz + 8, PLANK)
        leeg(W, bx - 4, by, round(zc + hw), bx + 6, by + 4, bz + 8)
        for (dx, dz) in ((-4, 8), (6, 8)):
            W.wfill(bx + dx, by, bz + dz, bx + dx, by + 5, bz + dz, HOUT)
        W.wfill(bx - 4, by + 6, bz + 8, bx + 6, by + 6, bz + 8, "minecraft:stripped_spruce_log[axis=x]")
        W.wfill(bx + 7, by, round(zc + hw), bx + 7, by, bz + 8, HEK)
        W.wset(bx + 1, by + 5, bz + 7, LAMP_G)
    # the cable: two chains from the top station to the Mosvoet station, three honey buckets on it
    a = np.array([bx + 1.0, by + 5.0, bz + 8.0])
    b = np.array([ox - 2.0, oy + 33.0, oz])
    lo, hi = np.minimum(a, b) - 4, np.maximum(a, b) + 4
    if not W.raakt(*lo, *hi):
        return
    n = int(np.max(np.abs(b - a))) * 2
    for off in (-1.5, 1.5):
        for i in range(n + 1):
            t = i / n
            p = a + (b - a) * t + np.array([off, -12 * math.sin(math.pi * t), 0])
            W.wset(p[0], p[1], p[2], "minecraft:iron_chain[axis=y]")
    for t in (0.22, 0.5, 0.78):
        p = a + (b - a) * t + np.array([0, -12 * math.sin(math.pi * t), 0])
        W.wfill(p[0] - 1, p[1] - 4, p[2] - 1, p[0] + 1, p[1] - 4, p[2] + 1, "minecraft:honeycomb_block")
        W.wfill(p[0] - 1, p[1] - 3, p[2] - 1, p[0] + 1, p[1] - 3, p[2] + 1, "minecraft:orange_terracotta")
        W.wfill(p[0], p[1] - 2, p[2], p[0], p[1], p[2], "minecraft:iron_chain[axis=y]")


# ================================================================================================ Honingtak interiors
def korf_interieurs(W):
    if not W.raakt(370, 200, -50, 620, 245, 50):
        return
    for (x, kant, fy, soort, fn) in PL.KORVEN:
        cz = PL.korf_midden(x, kant, fy)
        if not W.raakt(x - 10, fy - 3, cz - 10, x + 10, fy + 12, cz + 10):
            continue
        k = kant
        # floor: planks (stone for the smith) with a runner; nothing on the door line
        vloer = "minecraft:stone_bricks" if fn == "smid" else ("minecraft:polished_deepslate" if fn == "bank" else PLANK)
        W.cilinder(x + 0.5, cz + 0.5, 4.8, fy - 1, fy - 1, vloer)
        W.cilinder(x + 0.5, cz + 0.5, 4.8, fy, fy + 6, LUCHT)
        loper = {"juwelier": "minecraft:purple_carpet", "drankjes": "minecraft:pink_carpet", "uitrusting": "minecraft:orange_carpet",
                 "algemeen": "minecraft:yellow_carpet", "jagershut": "minecraft:brown_carpet", "burgemeester": "minecraft:pink_carpet"}
        if fn in loper:
            for dz in range(-3, 4):
                W.wset(x, fy, cz + dz, loper[fn])
        back = cz - k * 4          # against the wall on the branch side
        mid = cz - k * 1.5
        if fn == "smid":
            W.wset(x - 2, fy, back, "minecraft:anvil[facing=east]")
            W.wset(x + 2, fy, back, "minecraft:blast_furnace[facing=south,lit=true]" if k < 0 else "minecraft:blast_furnace[facing=north,lit=true]")
            W.wset(x + 3, fy, cz, "minecraft:magma_block")
            W.wset(x + 3, fy + 1, cz, "minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]")
            W.wset(x - 3, fy, cz, "minecraft:grindstone[face=floor,facing=north]")
            W.wset(x - 3, fy, cz + k * 2, "minecraft:honey_block")
        elif fn == "juwelier":
            for dx in (-2, -1, 0, 1, 2):
                W.wset(x + dx, fy, mid, "minecraft:quartz_slab[type=top,waterlogged=false]")
                W.wset(x + dx, fy + 1, mid, "minecraft:white_stained_glass")
            W.wset(x - 3, fy, cz, "guhs:weidekristal_roze")
            W.wset(x + 3, fy, cz, "guhs:guh_kristal_blok")
            W.wset(x + 3, fy + 1, cz, "guhs:glimpotje[licht=3]")
        elif fn in ("drankjes", "uitrusting", "algemeen", "bank", "jagershut"):
            for dx in (-2, -1, 0, 1, 2):
                W.wset(x + dx, fy, mid, "minecraft:stripped_oak_wood" if fn != "bank" else "minecraft:gold_block")
                W.wset(x + dx, fy + 1, mid, "minecraft:oak_slab[type=bottom,waterlogged=false]")
            for dx in (-3, -2, 2, 3):
                W.wset(x + dx, fy, back, "minecraft:barrel[facing=up,open=false]")
                W.wset(x + dx, fy + 1, back, "guhs:glimpotje[licht=3]" if fn == "drankjes" else
                       ("minecraft:chest[facing=north,type=single,waterlogged=false]" if fn == "bank" else "minecraft:honeycomb_block"))
            if fn == "drankjes":
                W.wset(x, fy + 2, mid, "guhs:theepotje[facing=south]")
            if fn == "jagershut":
                px, py, pz = R("gebouw.jagershut.premiebord")
                jx, jy, jz = R("gebouw.jagershut.jachtbord")
                W.wset(px, py, pz, "minecraft:lectern[facing=north,has_book=false,powered=false]")
                W.wset(jx, jy, jz, "minecraft:chiseled_bookshelf[facing=north,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=false]")
                W.wset(x, fy + 4, back, "minecraft:target")
        else:   # homes + the mayor
            W.wset(x - 2, fy, back, "minecraft:pink_bed[facing=east,occupied=false,part=foot]")
            W.wset(x - 1, fy, back, "minecraft:pink_bed[facing=east,occupied=false,part=head]")
            W.wset(x + 2, fy, back, "minecraft:smoker[facing=south,lit=true]" if k < 0 else "minecraft:smoker[facing=north,lit=true]")
            W.wset(x + 3, fy, cz, "minecraft:barrel[facing=up,open=false]")
            W.wset(x + 3, fy + 1, cz, "minecraft:honey_block")
            W.wset(x - 3, fy, cz, "guhs:guh_tafel[facing=south]")
            W.wset(x - 3, fy + 1, cz, "guhs:theepotje[facing=south]")
            W.wset(x - 3, fy, cz + k, "guhs:guh_stoel[facing=north]")
            if fn == "burgemeester":
                W.wset(x + 3, fy, cz + k * 2, "guhs:guh_bloempot")
                W.wset(x - 3, fy, cz - k * 2, "minecraft:bee_nest[facing=south,honey_level=5]")
        W.wset(x, fy + 6, cz, "minecraft:lantern[hanging=true,waterlogged=false]")
        W.wset(x, fy + 7, cz, "minecraft:iron_chain[axis=y]")
        # door line free, and a gap in the plateau fence in front of the door and where the stair arrives
        for d in np.arange(PL.KORF_R - 1.5, PL.KORF_R + 3.2, 0.5):
            for dx in (-1, 0, 1):
                for h in (0, 1, 2):
                    W.wset(x + dx, fy + h, cz + k * d, LUCHT)
        tx, tz = x - 7, cz - k * 3
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                if W.wget(tx + dx, fy, tz + dz) == HEK:
                    W.wset(tx + dx, fy, tz + dz, LUCHT)


def nestkapel(W):
    cx, yb, cz, Rr = PL.KAPEL_CEL
    if not W.raakt(cx - 14, yb - 2, cz - 14, cx + 14, yb + 14, cz + 14):
        return
    for x in range(cx - 10, cx + 11):
        for z in range(cz - 11, cz + 12):
            d = float(np.maximum(abs(x - cx) * 0.5 + abs(z - cz) * 0.866, abs(x - cx)))
            if d < Rr - 1.3:
                W.wset(x, yb, z, "minecraft:quartz_block" if (x + z) % 2 else "minecraft:pink_concrete")
    # aisle runner, egg pedestal at the back (north, -z), pews, the Droomsteen
    for z in range(cz - 3, cz + 9):
        W.wset(cx, yb, z, "minecraft:pink_wool")
    ex, ey, ez = R("gebouw.nestkapel.eiersokkel")
    W.wset(ex, ey, ez - 1, "minecraft:chiseled_quartz_block")
    W.wset(ex, ey + 1, ez - 1, "minecraft:quartz_slab[type=bottom,waterlogged=false]")
    W.wset(ex - 1, ey, ez - 1, "minecraft:quartz_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    W.wset(ex + 1, ey, ez - 1, "minecraft:quartz_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
    for dz in (2, 4, 6):
        for dx in (-4, -3, -2, 2, 3, 4):
            W.wset(cx + dx, yb + 1, cz + dz, "minecraft:birch_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    dx_, dy_, dz_ = R("gebouw.nestkapel.droomsteen")
    W.wset(dx_, dy_, dz_ - 1, "minecraft:quartz_pillar[axis=y]")
    W.wset(dx_, dy_ + 1, dz_ - 1, "guhs:guh_kristal_blok")
    for k in range(6):
        a = k * math.pi / 3
        W.wset(cx + 7 * math.cos(a), yb + 8, cz + 7 * math.sin(a), "minecraft:pearlescent_froglight")
    W.wset(cx - 6, yb + 1, cz - 5, "guhs:guh_bloempot")
    W.wset(cx + 6, yb + 1, cz - 5, "guhs:guh_bloempot")


def mika_imker(W):
    """The secret raatzaal under the Raattros: a hatch in the bottom cell floor, a ladder down, a hexagon hall (R 9)."""
    lx, ly, lz = R("mika_imker_luik")
    hx, hy, hz = R("mika_imker_raatzaal")
    if not W.raakt(hx - 14, hy - 3, hz - 14, hx + 14, ly + 3, hz + 14):
        return
    Rr, h = 9, 9
    for x in range(hx - 11, hx + 12):
        for z in range(hz - 11, hz + 12):
            d = float(np.maximum(abs(x - hx) * 0.5 + abs(z - hz) * 0.866, abs(x - hx)))
            if d <= Rr:
                for y in range(hy - 1, hy + h):
                    rand = d > Rr - 1.2 or y in (hy - 1, hy + h - 1)
                    W.wset(x, y, z, ("minecraft:honeycomb_block" if (y + x) % 3 else "minecraft:light_gray_terracotta") if rand else LUCHT)
    for (dx, dz) in ((-4, -3), (4, 3), (0, 5)):
        W.wset(hx + dx, hy, hz + dz, "minecraft:beehive[facing=south,honey_level=2]")
    W.wset(hx, hy, hz - 4, "minecraft:honey_block")
    for y in range(hy, ly):
        W.wset(lx, y, lz, "minecraft:ladder[facing=north,waterlogged=false]")
        W.wset(lx, y, lz + 1, "minecraft:honeycomb_block")
    W.wset(lx, ly - 1, lz, "minecraft:spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
    W.wset(hx + 3, hy + h - 2, hz, "minecraft:lantern[hanging=true,waterlogged=false]")


# ================================================================================================ Kruinzee
def kale_kruin(W):
    x, y, z = R("arena_kruinroofvogel")
    if not W.raakt(x - 40, y - 15, z - 40, x + 40, y + 30, z + 40):
        return
    # a dead, flat crown top raised above the crown (a mesa, never a pit): bare pale floor (r 24) on a stepped skirt (1 block
    # per step, so you walk up from any side), dead branches round the rim, a nest ring of grey sticks
    W.cilinder(x + 0.5, z + 0.5, PL.KALE_R + 1, y, y + 26, LUCHT)
    for yy in range(y - 2, y - 16, -1):
        W.cilinder(x + 0.5, z + 0.5, PL.KALE_R + (y - 1 - yy), yy, yy, None, keep=True, mixfn=lambda xx, y_, zz, m: W.kies(
            ["minecraft:pale_oak_leaves", "minecraft:pale_moss_block", "minecraft:stripped_pale_oak_wood"], [3, 2, 1], xx, y_, zz, 3501))
    W.cilinder(x + 0.5, z + 0.5, 24, y - 1, y - 1, None, mixfn=lambda xx, y_, zz, m: W.kies(
        ["minecraft:pale_moss_block", "minecraft:light_gray_terracotta", "minecraft:stripped_pale_oak_wood", "minecraft:gravel"],
        [4, 2, 1, 1], xx, y_, zz, 3502))
    for k in range(9):
        a = k * 2 * math.pi / 9 + 0.3
        p0 = (x + 23 * math.cos(a), y - 2, z + 23 * math.sin(a))
        p1 = (x + 29 * math.cos(a + 0.1), y + 14 + 4 * (k % 3), z + 29 * math.sin(a + 0.1))
        W.buis([p0, p1], [1.8, 0.9], "minecraft:pale_oak_wood")
    for k in range(30):
        a = k * 2 * math.pi / 30
        W.buis([(x + 15 * math.cos(a), y, z + 15 * math.sin(a)), (x + 16 * math.cos(a + 0.4), y + 1, z + 16 * math.sin(a + 0.4))],
               [0.8, 0.8], "minecraft:stripped_spruce_wood")
    bx, by, bz = R("arena_kruinroofvogel_bord")
    W.wfill(bx - 2, by - 1, bz - 2, bx + 2, by - 1, bz + 2, PLANK)
    leeg(W, bx - 2, by, bz - 2, bx + 2, by + 3, bz + 2)
    for i in (1, 2):
        px, py, pz = R(f"kruinroofvogel_zit_{i}")
        if W.raakt(px - 12, py - 6, pz - 12, px + 12, py + 20, pz + 12):
            W.cilinder(px + 0.5, pz + 0.5, 7, py - 1, py - 1, "minecraft:stripped_pale_oak_wood")
            W.cilinder(px + 0.5, pz + 0.5, 8, py, py + 12, LUCHT)
            for yy in range(py - 2, py - 14, -1):
                W.cilinder(px + 0.5, pz + 0.5, 7 + (py - 1 - yy), yy, yy, "minecraft:pale_oak_leaves", keep=True)
            W.buis([(px - 5, py - 1, pz), (px - 7, py + 10, pz + 2)], [1.4, 0.7], "minecraft:pale_oak_wood")
            W.buis([(px + 5, py - 1, pz), (px + 6, py + 8, pz - 3)], [1.4, 0.7], "minecraft:pale_oak_wood")


def kruin_extra(W):
    for i in range(1, 4):
        x, y, z = R(f"grauwe_vlek_{i}")
        if W.raakt(x - 14, y - 8, z - 14, x + 14, y + 10, z + 14):
            for dx in range(-11, 12):
                for dz in range(-11, 12):
                    if dx * dx + dz * dz > 121 or hash3(x + dx, 0, z + dz, 3601) < 0.15:
                        continue
                    h = W.hoogte(x + dx, z + dz)
                    if h is None or h < y - 8:
                        continue
                    W.wset(x + dx, h, z + dz, "minecraft:pale_oak_leaves" if hash3(x + dx, 1, z + dz, 3602) < 0.6 else
                           "minecraft:light_blue_terracotta")
            W.wfill(x + 4, y, z + 4, x + 4, y + 3, z + 4, "minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]")
            W.wset(x + 4, y + 4, z + 4, "minecraft:light_gray_concrete")
    for i in (1, 2):
        x, y, z = R(f"zweefplatform_{i}")
        if not W.raakt(x - 20, y - 6, z - 20, x + 20, y + 10, z + 20):
            continue
        a = math.atan2(z, x)
        ux, uz = math.cos(a), math.sin(a)
        loopbrug(W, (round(x - ux * 6), round(z - uz * 6)), (round(x + ux * 12), round(z + uz * 12)), y - 1, breed=2, hekken=True)
        ex, ez = x + ux * 13, z + uz * 13
        W.wset(ex, y, ez, LUCHT)
        W.wfill(ex - uz * 3, y, ez + ux * 3, ex - uz * 3, y + 6, ez + ux * 3, "minecraft:stripped_cherry_log[axis=y]")
        W.wfill(ex - uz * 3, y + 4, ez + ux * 3, ex - uz * 3 + ux * 2, y + 6, ez + ux * 3 + uz * 2, "minecraft:pink_wool")
        # the fence must not close the jump end
        for w in (-2, -1, 0, 1, 2):
            for h in (1, 2):
                W.wset(round(x + ux * 12) - uz * w, y - 1 + h, round(z + uz * 12) + ux * w, LUCHT)
    x, y, z = R("uitkijk_kroon")
    if W.raakt(x - 8, y - 25, z - 8, x + 8, y + 6, z + 8):
        g = int(round(PL.kruin(x, z)))
        W.cilinder(x + 0.5, z + 0.5, 4.5, y - 1, y - 1, PLANK)
        W.cilinder(x + 0.5, z + 0.5, 4.5, y, y, HEK, hollow=1)
        leeg(W, x - 4, y, z - 4, x + 4, y + 4, z + 4)
        W.cilinder(x + 0.5, z + 0.5, 4.5, y, y, HEK, hollow=1)
        for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
            W.wfill(x + dx, g - 4, z + dz, x + dx, y - 2, z + dz, HOUT)
        for yy in range(g + 1, y):
            W.wset(x, yy, z, "minecraft:stripped_spruce_log[axis=y]")
            W.wset(x, yy, z + 1, "minecraft:ladder[facing=south,waterlogged=false]")
            W.wset(x, yy, z + 2, LUCHT)
        W.wset(x, y - 1, z + 1, LUCHT)
        W.wfill(x - 1, g, z - 1, x + 1, g, z + 3, PLANK)
        W.wset(x, y + 4, z, LAMP_R)


# ================================================================================================ Schorsstad: Pelszaal + spiral balconies
def pelszaal(W):
    x, y, z = R("arena_piepvel")
    if not W.raakt(x - 20, 170, z - 20, x + 20, 199, z + 20):
        return
    fy = PL.PELSZAAL_Y
    # clear the kit's spire + chains, a round hall floor (r 16) on top of the tower, low rail
    W.cilinder(0, 0, 17, fy, 196, LUCHT)
    W.cilinder(0, 0, 16.5, fy, fy, None, mixfn=lambda xx, yy, zz, m: W.kies(
        ["minecraft:dark_oak_planks", "minecraft:spruce_planks", "guhs:belegen_kaas_stenen"], [4, 2, 1], xx, yy, zz, 3701))
    W.cilinder(0, 0, 16.5, fy + 1, fy + 1, "minecraft:dark_oak_fence", hollow=1)
    W.cilinder(0, 0, 13, fy - 1, fy - 1, "minecraft:stripped_dark_oak_wood")
    # root chains from the rim to the trunk wall (the tower hangs)
    for k in range(6):
        a = k * math.pi / 3 + 0.5
        W.buis([(16 * math.cos(a), fy - 1, 16 * math.sin(a)), (55 * math.cos(a), 194, 55 * math.sin(a))], [0.8, 0.8],
               "minecraft:mangrove_roots")
    # throne of cheese rinds, the big cage beside it, 4 hanging cages, 6 blown-out lantern posts
    tx, ty, tz = R("piepvel_troon")
    W.wfill(tx - 2, fy + 1, tz - 2, tx + 2, fy + 1, tz, "guhs:belegen_kaas_stenen")
    W.wfill(tx - 2, fy + 2, tz - 2, tx + 2, fy + 5, tz - 2, "guhs:gatenkaas_stenen")
    W.wset(tx - 2, fy + 2, tz - 1, "guhs:gatenkaas_stenen")
    W.wset(tx + 2, fy + 2, tz - 1, "guhs:gatenkaas_stenen")
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
        for yy in range(cy_ + 3, 197):
            W.wset(cx_, yy, cz_, "minecraft:iron_chain[axis=y]")
    for k in range(1, 7):
        px, py, pz = R(f"piepvel_lantaarnpaal_{k}")
        W.wfill(px, fy + 1, pz, px, fy + 3, pz, "minecraft:dark_oak_fence")
        W.wset(px, fy + 4, pz, "guhs:lampion_uit")
    # stair from the 7th tower ring (stand ring+1) up to the hall, around the tower core
    ry = PL.TOREN_RING_Y[-1]
    for i, yy in enumerate(range(ry + 1, fy + 1)):
        a = -1.2 + i * 0.5
        for rr in (4.5, 5.5):
            W.wset(rr * math.cos(a), yy, rr * math.sin(a), PLANK)
            for h in (1, 2, 3):
                W.wset(rr * math.cos(a), yy + h, rr * math.sin(a), LUCHT)
    # the finger print on the trunk wall (after hq10; npc/bazen may hide it until then)
    vx, vy, vz = R("vingerafdruk")
    for dy in range(-3, 4):
        for w in range(-2, 3):
            if dy * dy / 9 + w * w / 4 <= 1:
                a = math.atan2(vz, vx)
                W.wset(vx - math.sin(a) * w, vy + dy, vz + math.cos(a) * w, "minecraft:gray_concrete" if (dy + w) % 3 else
                       "minecraft:light_gray_concrete")


def spiraal_extra(W):
    if not W.raakt(-150, 60, -150, 150, 210, 150):
        return
    for k in (1, 2, 3):
        x, y, z = R(f"spiraal_balkon_{k}")
        jx, jy, jz = R(f"spiraal_zweefsprong_{k}")
        W.cilinder(x + 0.5, z + 0.5, 4.5, y - 1, y - 1, PLANK)
        W.cilinder(x + 0.5, z + 0.5, 4.5, y, y + 3, LUCHT)
        W.cilinder(x + 0.5, z + 0.5, 4.5, y, y, HEK, hollow=1)
        W.wset(x, y + 3, z, LAMP_G)
        W.wset(x, y, z, LUCHT)
        loopbrug(W, (x, z), (jx, jz), y - 1, breed=1, hekken=False)
        leeg(W, jx - 1, y, jz - 1, jx + 1, y + 2, jz + 1)
        # an opening in the balcony rail towards the road (the road is on the trunk side)
        a = math.atan2(z, x)
        for d in (2, 3, 4, 5):
            for w in (-1, 0, 1):
                W.wset(x - math.cos(a) * d - math.sin(a) * w, y, z - math.sin(a) * d + math.cos(a) * w, LUCHT)


# ================================================================================================ dungeon 1: Kaaszeppelinwerf
def werf(W):
    cx, cy, cz = PL.WERF
    if not W.raakt(cx - 60, 90, cz - 60, cx + 60, 175, cz + 60):
        return
    A, B, C = PL.WERF_A, PL.WERF_B, PL.WERF_C
    # scaffold posts
    for x in range(cx - 44, cx + 45, 8):
        for z in range(cz - 34, cz + 35, 10):
            W.wfill(x, C - 8, z, x, A + 6, z, "minecraft:dark_oak_fence")
    # level C (y 110): the flag zeppelin hull with a flat deck = Roquefort's arena + his cabin
    W.bol((cx, C - 5, cz), 30, None, ry=8, rz=12, ymax=C - 1, mixfn=lambda x, y, z, m: W.kies(
        ["guhs:gatenkaas_stenen", "guhs:belegen_kaas_stenen", "minecraft:yellow_concrete"], [3, 2, 1], x, y, z, 3801))
    W.wfill(cx - 26, C, cz - 9, cx + 26, C, cz + 9, "minecraft:dark_oak_planks")
    leeg(W, cx - 26, C + 1, cz - 9, cx + 26, C + 12, cz + 9)
    W.wfill(cx - 26, C + 1, cz - 10, cx + 26, C + 1, cz - 10, "minecraft:dark_oak_fence")
    W.wfill(cx - 26, C + 1, cz + 10, cx + 26, C + 1, cz + 10, "minecraft:dark_oak_fence")
    W.bol((cx, C + 22, cz), 24, None, ry=8, rz=9, ymin=C + 15, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:yellow_wool", "minecraft:light_gray_wool", "minecraft:orange_wool"], [4, 2, 1], x, y, z, 3802))
    for (dx, dz) in ((-18, -8), (18, -8), (-18, 8), (18, 8)):
        W.wfill(cx + dx, C + 1, cz + dz, cx + dx, C + 16, cz + dz, "minecraft:iron_chain[axis=y]")
    kx, ky, kz = R("roquefort_kajuit")
    W.wfill(kx - 1, C + 1, kz - 4, kx + 5, C + 5, kz + 4, "minecraft:dark_oak_planks")
    leeg(W, kx, C + 1, kz - 3, kx + 4, C + 4, kz + 3)
    leeg(W, kx - 1, C + 1, kz - 1, kx - 1, C + 2, kz + 1)
    W.wset(kx + 3, C + 1, kz, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    W.wset(kx + 4, C + 3, kz, "minecraft:lantern[hanging=false,waterlogged=false]")
    VK_vlag(W, cx - 24, C + 1, cz)
    # the wreck next to the deck (Roquefort phase 3: you jump over): a tilted half hull with a flat plank top (r ~8)
    wx_, wy_, wz_ = R("roquefort_wrak")
    W.bol((wx_, wy_ - 4, wz_), 11, None, ry=4, rz=7, ymax=wy_ - 2, mixfn=lambda x, y, z, m: W.kies(
        ["guhs:gatenkaas_stenen", "minecraft:dark_oak_planks", "minecraft:yellow_concrete"], [2, 2, 1], x, y, z, 3803))
    W.cilinder(wx_, wz_, 8, wy_ - 1, wy_ - 1, "minecraft:dark_oak_planks")
    leeg(W, wx_ - 8, wy_, wz_ - 8, wx_ + 8, wy_ + 6, wz_ + 8)
    # level B (y 125): gieterij, bewapeningsloods, Brokkel's open deck, walkways
    W.wfill(cx - 40, B, cz - 30, cx + 40, B, cz - 26, "minecraft:dark_oak_planks")
    W.wfill(cx - 40, B, cz + 26, cx + 40, B, cz + 30, "minecraft:dark_oak_planks")
    W.wfill(cx + 14, B, cz - 30, cx + 40, B, cz + 30, "minecraft:dark_oak_planks")
    W.wfill(cx - 40, B, cz - 30, cx - 16, B, cz + 30, "minecraft:dark_oak_planks")
    leeg(W, cx - 40, B + 1, cz - 30, cx + 40, B + 9, cz + 30)
    gx, gy, gz = R("werf_kamer_gieterij")
    hal(W, gx, B, gz, 9, 7, 7, "minecraft:bricks", "minecraft:magma_block")
    bx_, by_, bz_ = R("werf_kamer_bewapening")
    hal(W, bx_, B, bz_, 9, 7, 7, "minecraft:dark_oak_planks", None)
    # (levers + cannons: bazen; the gate the cannons blow open = werf_loods_poort, a 3x4 opening of air in the south wall)
    gx_, gy_, gz_ = R("werf_loods_poort")
    leeg(W, gx_ - 1, B + 1, gz_ - 1, gx_ + 1, B + 4, gz_)
    W.wfill(gx_ - 1, B, gz_ - 1, gx_ + 1, B, gz_ + 2, "minecraft:dark_oak_planks")
    for i in (1, 2):
        kx_, ky_, kz_ = R(f"arena_brokkel_kanon_{i}")
        W.wset(kx_, B + 1, kz_, "minecraft:polished_blackstone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")
        W.wset(kx_, B + 2, kz_, "minecraft:dispenser[facing=north,triggered=false]")
    W.wfill(cx - 40, B + 1, cz - 31, cx + 40, B + 1, cz - 31, "minecraft:dark_oak_fence")
    W.wfill(cx - 40, B + 1, cz + 31, cx + 40, B + 1, cz + 31, "minecraft:dark_oak_fence")
    # level A (y 140): the rope-work deck (islands + rope bridges), Taleggio's hall in the open rigging
    for (dx, dz, r) in ((18, 16, 7), (-22, -14, 15), (20, -18, 6), (-4, 20, 6), (0, -2, 5)):
        W.cilinder(cx + dx + 0.5, cz + dz + 0.5, r, A, A, "minecraft:dark_oak_planks")
        W.cilinder(cx + dx + 0.5, cz + dz + 0.5, r, A + 1, A + 12, LUCHT)
    for (p, q) in (((18, 16), (-4, 20)), ((-4, 20), (0, -2)), ((0, -2), (-22, -14)), ((0, -2), (20, -18)), ((18, 16), (20, -18))):
        loopbrug(W, (cx + p[0], cz + p[1]), (cx + q[0], cz + q[1]), A, breed=1, blok="minecraft:spruce_slab[type=top,waterlogged=false]")
    for k in range(10):
        a = k * 0.63
        W.wfill(cx - 22 + 11 * math.cos(a), A + 4, cz - 14 + 11 * math.sin(a), cx - 22 + 11 * math.cos(a), A + 22,
                cz - 14 + 11 * math.sin(a), "minecraft:iron_chain[axis=y]")
    # cranes
    for (x, z) in ((cx - 36, cz - 30), (cx + 34, cz + 28)):
        W.wfill(x, C - 8, z, x, A + 22, z, "minecraft:iron_block")
        W.wfill(x, A + 22, z, x + 14, A + 22, z, "minecraft:iron_block")
        W.wfill(x + 12, A + 6, z, x + 12, A + 21, z, "minecraft:iron_chain[axis=y]")
    # entrance from the branch path down to level A, A -> B, B -> C
    ix, iy, iz = R("werf_ingang")
    W.wfill(ix - 2, iy - 1, iz - 2, ix + 2, iy - 1, iz + 2, "minecraft:dark_oak_planks")
    trap_recht(W, (ix, iz + 3), iy - 1, (cx + 18, cz + 16 - 8), A, breed=1, blok="minecraft:dark_oak_planks")
    ax, ay, az = R("werf_trap_ab")
    trap_recht(W, (ax, az), A, (ax - (A - B) - 2, az), B, breed=1, blok="minecraft:dark_oak_planks")
    loopbrug(W, (ax, cz - 6), (ax, az + 2), A, breed=1)
    bx2, by2, bz2 = R("werf_trap_bc")
    trap_recht(W, (bx2, bz2), B, (bx2 + (B - C) + 2, bz2 - 0), C, breed=1, blok="minecraft:dark_oak_planks")
    loopbrug(W, (bx2 + (B - C) + 2, bz2), (cx, cz + 9), C, breed=1)
    W.wset(cx - 2, A + 1, cz - 2, LAMP_G)


def VK_vlag(W, x, y, z):
    W.wfill(x, y, z, x, y + 7, z, "minecraft:dark_oak_fence")
    W.wfill(x + 1, y + 5, z, x + 3, y + 7, z, "minecraft:light_gray_wool")


def hal(W, cx, fy, cz, rx, rz, h, wand, vloer_extra):
    """A closed rectangular hall (inner rx x rz half sizes) on floor fy with door gaps on all four sides."""
    W.wfill(cx - rx - 1, fy, cz - rz - 1, cx + rx + 1, fy + h + 1, cz + rz + 1, wand)
    leeg(W, cx - rx, fy + 1, cz - rz, cx + rx, fy + h, cz + rz)
    W.wfill(cx - rx, fy, cz - rz, cx + rx, fy, cz + rz, "minecraft:dark_oak_planks")
    for (dx, dz) in ((rx + 1, 0), (-rx - 1, 0), (0, rz + 1), (0, -rz - 1)):
        leeg(W, cx + dx - (0 if dx else 1), fy + 1, cz + dz - (0 if dz else 1), cx + dx + (0 if dx else 1), fy + 3, cz + dz + (0 if dz else 1))
    if vloer_extra:
        for dx in range(-rx + 2, rx - 1, 4):
            W.wfill(cx + dx, fy, cz - rz + 2, cx + dx, fy, cz + rz - 2, vloer_extra)
    W.wset(cx, fy + h, cz, "minecraft:lantern[hanging=true,waterlogged=false]")


# ================================================================================================ dungeon 2: Hart van de Boom
def hart(W):
    if not W.raakt(-130, 0, -130, 130, 75, 130):
        return
    cx, fy, cz, r, h = PL.HARTSLAG
    # the Hartslag-zaal gets a real floor at fy (no sap lake inside): moss + pink veins as spokes towards the middle
    W.cilinder(0, 0, r - 1, 0, fy, None, mixfn=lambda x, y, z, m: np.where(
        (np.abs(np.sin(np.arctan2(z, x) * 6)) < 0.08) & (y == fy), W.id("guhs:weidekristal_roze"),
        W.kies(["minecraft:moss_block", "guhs:vadsmos", "minecraft:tuff", "minecraft:rooted_dirt"], [3, 2, 2, 1], x, y, z, 3901)))
    V = PL.HART_VLOER
    Rr = PL.HART_RING_R
    schors = lambda x, y, z, m: np.where(hash3(x, y, z, 3902) < 0.08, W.id("guhs:weidekristal_roze"),
                                         W.kies(boom.SCHORS, boom.SCHORS_W, x, y, z, 3903))
    # the ring corridor (shell + air), from -90 deg (entrance) round to 0 deg (Baron Bleu)
    # the ring corridor (shell + air) round the trunk axis
    W.cilinder(0, 0, Rr + 5, V - 1, V + 8, None, hollow=11, mixfn=schors)
    W.cilinder(0, 0, Rr + 3, V + 1, V + 6, LUCHT, hollow=7)
    W.cilinder(0, 0, Rr + 3, V, V, None, hollow=7, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:stripped_spruce_wood", "minecraft:stripped_oak_wood", "guhs:weidekristal_roze"], [5, 3, 0.4], x, y, z, 3904))
    # a wall closes the ring between Baron Bleu (0 deg) and the entrance (-90 deg): the way is one direction
    for a in np.linspace(math.radians(-80), math.radians(-12), 60):
        W.cilinder(Rr * math.cos(a), Rr * math.sin(a), 5, V + 1, V + 6, None, mixfn=schors)
    for (naam, a_deg, kr) in PL.HART_KAMERS:
        x, z = PL.polar(a_deg, Rr)
        if naam == "aderloop":
            continue
        hh = 14 if naam in ("wortelwurger", "nestwachter", "baron_bleu") else 10
        W.cilinder(x, z, kr + 2, V - 1, V + hh + 1, None, mixfn=schors)
        W.cilinder(x, z, kr, V + 1, V + hh, LUCHT)
        if naam == "nestwachter":
            W.cilinder(x, z, kr, V, V, None, mixfn=lambda xx, yy, zz, m: np.where(
                (np.hypot(xx - x, zz - z) // 3) % 2 == 0, W.id("minecraft:stripped_oak_wood"), W.id("minecraft:stripped_spruce_wood")))
        else:
            W.cilinder(x, z, kr, V, V, None, mixfn=lambda xx, yy, zz, m: W.kies(
                ["minecraft:stripped_spruce_wood", "minecraft:moss_block", "guhs:weidekristal_roze"], [5, 2, 0.5], xx, yy, zz, 3905))
        for k in range(8):
            a = k * math.pi / 4
            W.wset(x + (kr - 1) * math.cos(a), V + hh - 1, z + (kr - 1) * math.sin(a), "minecraft:pearlescent_froglight"
                   if k % 2 else "guhs:weidekristal_roze")
        if naam == "schimmelkwekerij":
            for k in range(3):
                a = k * 2 * math.pi / 3
                kx, kz = x + 9 * math.cos(a), z + 9 * math.sin(a)
                W.cilinder(kx, kz, 2.5, V + 1, V + 5, "minecraft:iron_block")
                W.cilinder(kx, kz, 2.7, V + 3, V + 4, "minecraft:light_blue_terracotta", hollow=0.8)
    # from Baron Bleu (0 deg) inward to the Nestkamer, and the floor ring round the egg
    loopbrug(W, (Rr - 20, 0), (11, 0), V, breed=2, hekken=False)
    for xx in range(11, Rr - 19):
        W.wfill(xx, V + 4, -3, xx, V + 4, 3, "minecraft:stripped_spruce_wood")
    ex, ey, ez = PL.EI
    W.cilinder(ex, ez, 10.5, V - 1, V - 1, None, hollow=4.5, mixfn=lambda x, y, z, m: W.kies(
        ["minecraft:stripped_spruce_wood", "guhs:weidekristal_roze"], [5, 1], x, y, z, 3906))
    # the entrance: a stair tunnel from the Hartslag-zaal north wall up to the ring at -90 deg
    px, py, pz = R("hart_poort")
    for i in range(0, Rr - 48):
        zz = pz - i
        yy = fy + round((V - fy) * i / (Rr - 50))
        yy = min(V, yy)
        W.wfill(px - 3, yy - 1, zz, px + 3, yy + 5, zz, "minecraft:dark_oak_planks")
        W.wfill(px - 2, yy, zz, px + 2, yy + 4, zz, LUCHT)
        W.wfill(px - 2, yy - 1, zz, px + 2, yy - 1, zz, "minecraft:stripped_spruce_wood")
        nxt = min(V, fy + round((V - fy) * (i + 1) / (Rr - 50)))
        if nxt > yy:
            W.wfill(px - 2, yy, zz, px + 2, yy, zz, "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
        if i % 8 == 0:
            W.wset(px + 2, yy + 3, zz, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    # the gate frame in the hall wall + 4 cave lanterns (unlit = soul lantern, lit in hq12)
    for k in range(1, 5):
        lx, ly, lz = R(f"hart_grotlantaarn_{k}")
        W.wfill(lx, ly, lz, lx, ly + 1, lz, "minecraft:dark_oak_fence")
        W.wset(lx, ly + 2, lz, "minecraft:soul_lantern[hanging=false,waterlogged=false]")
    for k in ("hart_bord", "hart_vahoeg_altaar"):
        x, y, z = R(k)
        W.wset(x, y, z, "minecraft:chiseled_stone_bricks" if "altaar" in k else LUCHT)


# ================================================================================================ Wortelvelden
def wortelweg(W):
    if W.b[1] > 100:
        return
    RRw = PL.WORTELWEG_R + 6 * np.sin(W.TH * 5)
    pad = np.abs(W.RR - RRw) < 2.6
    if not pad.any():
        return
    g = boom.grondhoogte(W.XX, W.ZZ)
    yt = np.floor(g + 0.5)
    W.kolom(yt - 2, yt - 1, "minecraft:dirt", mask=pad)
    W.kolom(yt, yt, W.kies(["minecraft:dirt_path", "minecraft:dirt_path", "minecraft:coarse_dirt"], [5, 3, 1], W.XX, 0, W.ZZ, 4001), mask=pad)
    W.kolom(yt + 1, yt + 5, LUCHT, mask=pad)
    # lanterns at the tunnel mouths and every ~40 blocks
    lamp = (np.abs(W.RR - RRw - 3.4) < 0.5) & (hash3(np.round(W.TH * 120), 0, 0, 4002) < 0.05)
    W.kolom(yt + 1, yt + 2, "minecraft:spruce_fence", mask=lamp)
    W.kolom(yt + 3, yt + 3, "minecraft:lantern[hanging=false,waterlogged=false]", mask=lamp)


def mosvoet_vloer(W):
    mx, mz = PL.MOSVOET
    if not W.raakt(mx - 30, 50, mz - 30, mx + 30, 110, mz + 30):
        return
    g = int(math.floor(PL.grond(mx, mz)))
    # the hollow root knot gets a flat plank floor at ground level (the kit left a bowl), round rugs, a table
    W.cilinder(mx + 0.5, mz + 0.5, 21, g - 6, g - 1, "minecraft:dirt")
    W.cilinder(mx + 0.5, mz + 0.5, 21, g, g, None, mixfn=lambda x, y, z, m: W.kies(["minecraft:spruce_planks", "minecraft:oak_planks"],
                                                                                    [3, 1], x, y, z, 4101))
    W.cilinder(mx + 0.5, mz + 0.5, 5, g, g, "minecraft:moss_block")
    W.cilinder(mx + 0.5, mz + 0.5, 4, g + 1, g + 1, "minecraft:moss_carpet")
    for k in range(10):
        a = k * 2 * math.pi / 10
        if k % 2:   # the kit's round doors (odd k, at ground + 2): make the threshold flush
            x, z = mx + 24 * math.cos(a), mz + 24 * math.sin(a)
            for d in np.arange(-5, 3, 0.5):
                xx, zz = mx + (24 + d) * math.cos(a), mz + (24 + d) * math.sin(a)
                W.wset(xx, g, zz, PLANK)
                for h in (1, 2, 3):
                    W.wset(xx, g + h, zz, LUCHT)
    for (dx, dz) in ((-8, 6), (9, -5), (-4, -12)):
        W.wset(mx + dx, g + 1, mz + dz, "guhs:guh_tafel[facing=south]")
        W.wset(mx + dx, g + 2, mz + dz, "guhs:theepotje[facing=south]")
    # the little root-knot houses round it (kit): a flat floor at ground level and a flush door sill
    for (dx, dz) in ((-38, 10), (-30, -26), (34, -22), (40, 18), (6, 40), (-18, 38)):
        hx, hz = mx + dx, mz + dz
        gg = int(math.floor(PL.grond(hx, hz)))
        W.cilinder(hx, hz, 5.4, gg - 3, gg - 1, "minecraft:dirt")
        W.cilinder(hx, hz, 5.4, gg, gg, "minecraft:spruce_planks")
        W.cilinder(hx, hz, 4.8, gg + 1, gg + 3, LUCHT)
        a = math.atan2(mz - hz, mx - hx)
        for d in np.arange(4.5, 8.5, 0.5):
            W.wset(hx + d * math.cos(a), gg, hz + d * math.sin(a), PLANK)
            W.wset(hx + d * math.cos(a), gg + 1, hz + d * math.sin(a), LUCHT)
            W.wset(hx + d * math.cos(a), gg + 2, hz + d * math.sin(a), LUCHT)
        W.wset(hx, gg + 1, hz, "minecraft:moss_carpet")
    W.wset(mx, g + 16, mz, "minecraft:iron_chain[axis=y]")
    W.wset(mx, g + 15, mz, "minecraft:lantern[hanging=true,waterlogged=false]")


# ================================================================================================ secrets + campfires
def geheimen(W):
    for k, v in FL().items():
        if not (k.startswith("geheim_") or k.startswith("imker_aanwijzing") or k.startswith("muurverhaal_")
                or k.startswith("boomvolk_lichtje")):
            continue
        x, y, z = R(k)
        if not W.raakt(x - 3, y - 3, z - 3, x + 3, y + 4, z + 3):
            continue
        if W.wget(x, y - 1, z) == LUCHT:
            W.wfill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, PLANK, keep=True)
        leeg(W, x, y, z, x, y + 1, z)
        if k.startswith("muurverhaal_"):   # a relief panel behind the spot, on the trunk wall
            a = math.atan2(z, x)
            for dy in range(0, 4):
                for w in (-1, 0, 1):
                    W.wset(x + math.cos(a) * 2 - math.sin(a) * w, y + dy, z + math.sin(a) * 2 + math.cos(a) * w,
                           "minecraft:chiseled_tuff" if (dy + w) % 2 else "guhs:weidekristal_roze")


def kampvuren(W):
    for k, v in FL().items():
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


def poort_trap(W):
    """The Grote Wortelpoort: a real staircase from the ground in the south down into the Grote Wortelgang (no drop)."""
    x, z = PL.WORTELPOORT
    if not W.raakt(x - 10, 40, z - 20, x + 10, 90, z + 45):
        return
    g = int(math.floor(PL.grond(x, z + 34) + 0.5))
    L = boom._spline(PL.WORTELGANG, 10)
    vloer = int(round(L[0][1] - 9))
    for zz in range(z - 6, z + 37):
        t = (z + 34 - zz) / 38.0
        y = round(g + (vloer - g) * min(1, max(0, t)))
        W.wfill(x - 2, y, zz, x + 2, y + 6, zz, LUCHT)
        if zz % 7 == 0:
            W.wset(x + 3, y + 3, zz, "minecraft:lantern[hanging=false,waterlogged=false]", keep=True)
    trap_recht(W, (x, z + 34), g, (x, z - 4), vloer, breed=2, blok="minecraft:stripped_spruce_wood")


def spiraal_uitgang(W):
    """The foot of the Stamspiraal lies below the root hill (y 70): a ramp tunnel due south up to the ground (no dead end)."""
    r0 = PL.spiraal_voet_r()
    if not W.raakt(-6, 60, r0 - 10, 6, 110, r0 + 50):
        return
    ytop = int(math.floor(PL.grond(0, r0 + 44) + 0.5))
    for zz in range(round(r0 - 8), round(r0 + 46)):
        t = (zz - (r0 - 6)) / 50.0
        y = round(70 + (ytop - 70) * min(1, max(0, t)))
        W.wfill(-2, y, zz, 2, y + 6, zz, LUCHT)
        if zz % 8 == 0:
            W.wset(3, y + 3, zz, "minecraft:lantern[hanging=false,waterlogged=false]", keep=True)
    trap_recht(W, (0, round(r0 - 6)), 70, (0, round(r0 + 44)), ytop, breed=2, blok="minecraft:stripped_spruce_wood")


def pad_vrij(W):
    """Riding paths stay free: nothing solid in the 3 cells above a dirt-path block (kit plumes, lamps, roots)."""
    if "minecraft:dirt_path" not in W.idx:
        return
    pid = W.idx["minecraft:dirt_path"]
    laat = {W.idx[s] for s in W.idx if any(w in s for w in ("lantern", "lampion", "chain", "ladder", "campfire", "petals",
                                                          "carpet", "slab", "stairs", "torch", "z3kv"))}
    laat.add(0)
    xs, ys, zs = np.nonzero(W.v == pid)
    for dy in (1, 2, 3):
        yy = ys + dy
        ok = yy < W.v.shape[1]
        a, b, c = xs[ok], yy[ok], zs[ok]
        cur = W.v[a, b, c]
        weg = ~np.isin(cur, list(laat)) & (cur != pid)
        W.v[a[weg], b[weg], c[weg]] = 0


def voor_kit(W):
    pass


def bouw(W):
    zee(W)
    werf(W)
    hart(W)
    wortelweg(W)
    mosvoet_vloer(W)
    windkolommen(W)
    liftjes(W)
    honingtak_toegang(W)
    honinglift(W)
    korf_interieurs(W)
    nestkapel(W)
    mika_imker(W)
    kale_kruin(W)
    kruin_extra(W)
    pelszaal(W)
    spiraal_extra(W)
    poort_trap(W)
    spiraal_uitgang(W)
    pad_vrij(W)
    geheimen(W)
    kampvuren(W)
