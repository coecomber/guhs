"""Zone 4: the GAME parts on top of the approved render generators (kit/): everything a survival player needs.

    bouw(W)   applies the game overlay to one tile window W (schemerkit.Wereld, k=1, world coordinates)

The overlay is a list of world-coordinate edits (fills), built once per process (deterministic, from the plan + the kit's
height field) and clipped to every window:
  - house interiors: carved clean (the kit never carves terrain), real floor, a hanging lampion, furniture per building;
    stairs from every door down to the ground when the door sits > 1 block above it;
  - Lampionhaven: one continuous boardwalk along the north and south arm (the kit left 1-block gaps), the stable paddock;
  - sailboat docks on every island (+ the turtle), the luchthaven stair kept;
  - the big lighthouse: a door and a spiral stair up to the gallery (viewpoint);
  - the Koraalbaai: a round raft arena in the lagoon + a floating walkway from the diving jetty, bubble-column exits from the
    3 underwater grottos;
  - the Wrak van de Vliegervanger: a real small dungeon (hull with 3 rooms below deck, boss on the fore deck);
  - unlit campfires (kern checkpoints), secrets (a knuffel/chest marker), the Opstijgpier secret raft.
Marks: every rule here exists for the walkability standard (T_BRIEF) or a gameplay spot (POI_Z4).
"""
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "kit"))
sys.path.insert(0, HIER)

import kust as KU  # noqa: E402
import plan4 as PL  # noqa: E402

ZEE = PL.ZEE
LUCHT = "minecraft:air"
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
RICHTING = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}
PAAL = "minecraft:stripped_cherry_log[axis=y]"
PLANK = "minecraft:cherry_planks"

_OPS = None


class Ops:
    def __init__(self):
        self.l = []      # (x1, y1, z1, x2, y2, z2, state)

    def f(self, x1, y1, z1, x2, y2, z2, s):
        self.l.append((min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2), s))

    def s(self, x, y, z, s):
        self.l.append((x, y, z, x, y, z, s))


def g(x, z):
    return PL.y_grond(int(x), int(z))


def trap_blok(facing, b="minecraft:cherry_stairs"):
    return f"{b}[facing={facing},half=bottom,shape=straight]"


# ------------------------------------------------------------------------------------------------ stairs from doors
def trap_lengte(x, y, z, fx, fz):
    """Dry run of trap_af: (number of stairs, ends over water?)."""
    ys = y - 1
    for i in range(1, 40):
        gr = max(g(x + fx * i, z + fz * i), ZEE)
        if gr >= (ys - 1 if i == 1 else ys):
            return i - 1, g(x + fx * i, z + fz * i) <= ZEE
        ys -= 1
    return 99, True


def trap_af(o, x, y, z, fx, fz, breed=1, blok="minecraft:cherry_stairs", steun="minecraft:pink_terracotta"):
    """From the stand spot (x, y, z) (a full block at y-1 under it) walk outward along (fx, fz) and go DOWN until the natural
    ground: the first stair sits level with that block (its top = the landing top), every next one 1 lower; it ends when the
    ground is at most 1 below the last step. Stairs face back up (towards the door); support below, 3 blocks of headroom."""
    rx, rz = -fz, fx
    up = RICHTING[(-fx, -fz)]
    ys = y - 1
    for i in range(1, 40):
        cx, cz = x + fx * i, z + fz * i
        gr = max(g(cx, cz), ZEE)
        if gr >= (ys - 1 if i == 1 else ys):
            for j in range(-(breed // 2), breed // 2 + 1):
                o.f(cx + rx * j, gr + 1, cz + rz * j, cx + rx * j, gr + 3, cz + rz * j, LUCHT)
            return (cx, gr + 1, cz)
        for j in range(-(breed // 2), breed // 2 + 1):
            px, pz = cx + rx * j, cz + rz * j
            o.f(px, ys + 1, pz, px, ys + 3, pz, LUCHT)
            o.s(px, ys, pz, trap_blok(up, blok))
            gp = g(px, pz)
            if gp <= ZEE:
                o.f(px, gp, pz, px, ys - 1, pz, PAAL)
            elif gp < ys - 1:
                o.f(px, gp + 1, pz, px, ys - 1, pz, steun)
        ys -= 1
    return (x + fx * 40, ys, z + fz * 40)


# ------------------------------------------------------------------------------------------------ house interiors
def interieur(o, h):
    x, y, z, w, d, hh, f = h["x"], h["y"], h["z"], h["w"], h["d"], h["h"], h["facing"]
    x2, z2 = x + w - 1, z + d - 1
    xa, xb, za, zb = x + 1, x2 - 1, z + 1, z2 - 1
    # clean interior (terrain or palms that grew in) + a real floor
    o.f(xa, y, za, xb, y + hh - 1, zb, LUCHT)
    o.f(xa, y - 1, za, xb, y - 1, zb, "guhs:roze_lakhout_planken")
    fx, fz = DIRS[f]
    bx, bz = x + w // 2, z + d // 2
    o.s(bx, y + hh - 1, bz, "guhs:lampion_geel[hanging=true]")
    # the back row (opposite the door) and the corners in it
    if f == "south":
        achter = [(xx, za) for xx in range(xa, xb + 1)]
    elif f == "north":
        achter = [(xx, zb) for xx in range(xa, xb + 1)]
    elif f == "east":
        achter = [(xa, zz) for zz in range(za, zb + 1)]
    else:
        achter = [(xb, zz) for zz in range(za, zb + 1)]
    links, rechts = achter[0], achter[-1]
    # the cell just inside the door must stay free
    dx_, dy_, dz_ = h["deur"]
    binnen_deur = (dx_ - fx, dz_ - fz)
    soort = h["id"]
    winkel = soort in ("vliegerwinkel", "winkel", "bank", "havenmeester", "botenverhuur", "visrestaurant", "opvangcentrum")
    if winkel:
        # counter one row in front of the back row (the npc stands behind it), with a gap at one end
        for (cx, cz) in achter[1:]:
            tx, tz = cx + fx, cz + fz
            if (tx, tz) != binnen_deur:
                o.s(tx, y, tz, "minecraft:stripped_cherry_log[axis=%s]" % ("x" if fz else "z"))
        o.s(links[0], y, links[1], "minecraft:barrel[facing=up]")
        if soort == "bank":
            o.s(rechts[0], y, rechts[1], "minecraft:gold_block")
        elif soort == "vliegerwinkel":
            o.s(rechts[0], y, rechts[1], "minecraft:loom[facing=%s]" % f)
        elif soort == "havenmeester":
            o.s(rechts[0], y, rechts[1], "minecraft:lectern[facing=%s]" % f)
        elif soort == "opvangcentrum":
            for (cx, cz) in achter[1:-1:2]:
                o.s(cx, y + 1, cz, "guhs:knuffel_mager[facing=%s]" % f)
        else:
            o.s(rechts[0], y, rechts[1], "minecraft:chest[facing=%s]" % f)
        return
    # homes / workshops / inn: bed in the back-right corner (head at the back), table, barrel
    hx, hz = rechts
    if (hx + fx, hz + fz) != binnen_deur:
        o.s(hx, y, hz, f"minecraft:pink_bed[facing={OPP[f]},part=head,occupied=false]")
        o.s(hx + fx, y, hz + fz, f"minecraft:pink_bed[facing={OPP[f]},part=foot,occupied=false]")
    o.s(links[0], y, links[1], "minecraft:barrel[facing=up]")
    if soort in ("lampionwerkplaats", "vliegerwerkplaats"):
        o.s(links[0] + fx, y, links[1] + fz, "minecraft:crafting_table")
        o.s(links[0] + 2 * fx, y, links[1] + 2 * fz, "minecraft:loom[facing=%s]" % f)
    elif soort == "kookhuis":
        o.s(links[0] + fx, y, links[1] + fz, "minecraft:smoker[facing=%s,lit=false]" % f)
        o.s(links[0] + 2 * fx, y, links[1] + 2 * fz, "minecraft:crafting_table")
    elif soort == "stal":
        o.s(links[0] + fx, y, links[1] + fz, "minecraft:hay_block[axis=y]")
    elif soort == "herberg":
        o.s(links[0] + fx, y, links[1] + fz, "guhs:guh_tafel[facing=%s]" % f)
        o.s(hx + 2 * fx, y, hz + 2 * fz, "guhs:guh_tafel[facing=%s]" % f) if (hx + 2 * fx, hz + 2 * fz) != binnen_deur else None
    else:
        o.s(links[0] + fx, y, links[1] + fz, "guhs:guh_tafel[facing=%s]" % f)


def deur_trap(o, h):
    """Stairs down from the door when the ground in front is more than 1 block lower; headroom in front of the door."""
    fx, fz = DIRS[h["facing"]]
    vx, vy, vz = h["voor"]
    o.f(vx, vy, vz, vx, vy + 2, vz, LUCHT)
    gr = g(vx, vz)
    if h["op_palen"]:
        return     # stilt houses: their boardwalk is built by haven() (Lampionhaven) or the dock
    if gr + 2 < vy:
        # landing block in front of the door (level with the floor), then the stair down
        o.f(vx, gr + 1 if gr > ZEE else vy - 1, vz, vx, vy - 1, vz, "minecraft:pink_terracotta" if gr > ZEE else PLANK)
        # straight out, or along the front wall (left/right) when the slope falls away as fast as a stair: the shortest one
        # that ends on dry ground
        keus = []
        for (ax, az) in ((fx, fz), (-fz, fx), (fz, -fx)):
            n, nat = trap_lengte(vx, vy, vz, ax, az)
            keus.append((n + (50 if nat else 0), (ax, az)))
        ax, az = min(keus)[1]
        trap_af(o, vx, vy, vz, ax, az, breed=1)
    elif gr + 2 == vy:
        o.s(vx, vy - 1, vz, "minecraft:pink_terracotta")    # a 1-block step: just a landing


# ------------------------------------------------------------------------------------------------ Lampionhaven extras
def haven(o):
    KY = PL.KY
    # continuous boardwalks in front of the stilt houses (the kit put planks only in front of each house)
    for (xa, xb, za, zb) in ((1860, 1915, -490, -489), (1868, 1906, -443, -442)):
        o.f(xa, KY, za, xb, KY, zb, PLANK)
        o.f(xa, KY + 1, za, xb, KY + 3, zb, LUCHT)
        for xx in range(xa, xb + 1, 4):
            for zz in (za, zb):
                gp = g(xx, zz)
                if gp < KY - 1:
                    o.f(xx, gp, zz, xx, KY - 1, zz, PAAL)
    # the 1-block alleys between the stilt houses of both arms: a plank floor level with the boardwalk (no pits)
    import plan4
    for (z0, za, zb) in ((-497, -497, -491), (-441, -441, -435)):
        rij = [h for h in plan4.huizen() if h["z"] == z0 and h.get("op_palen")]
        xs = sorted((h["x"], h["x"] + h["w"] - 1) for h in rij)
        for (a1, b1), (a2, b2) in zip(xs, xs[1:]):
            for xx in range(b1 + 1, a2):
                o.f(xx, KY, za, xx, KY, zb, PLANK)
                o.f(xx, KY + 1, za, xx, KY + 3, zb, LUCHT)
    # ladders out of the water at the three jetty ends
    for z in (-478, -464, -451):
        ladder(o, 1892, z, 1, 0, KY - 1)
    # stable paddock west of the stable (fence ring with a gate towards the town)
    sx, sz = 1800, -458
    for xx in range(sx, sx + 9):
        for zz in range(sz, sz + 9):
            rand = xx in (sx, sx + 8) or zz in (sz, sz + 8)
            gy = g(xx, zz) + 1
            if rand:
                st = "minecraft:cherry_fence_gate[facing=east,in_wall=false,open=false]" if (xx == sx + 8 and zz == sz + 4) else "minecraft:cherry_fence"
                o.s(xx, gy, zz, st)
    o.s(sx + 2, g(sx + 2, sz + 2) + 1, sz + 2, "minecraft:hay_block[axis=y]")
    o.s(sx + 3, g(sx + 3, sz + 2) + 1, sz + 2, "minecraft:water_cauldron[level=3]")
    # questbord on the plaza
    q = PL.plekken()["questbord"]
    o.s(q[0] - 1, q[1], q[2], "minecraft:cherry_fence")
    o.s(q[0] - 1, q[1] + 1, q[2], "guhs:pastelbaksteen_framboos")


# ------------------------------------------------------------------------------------------------ docks
def dok(o, eiland):
    x0, z0, x1, z1, dx, dz = PL.dok_plek(eiland)
    rx, rz = -dz, dx
    y = PL.DOK_Y
    n = abs(x1 - x0) + abs(z1 - z0)
    for i in range(0, n + 1):
        cx, cz = x0 + dx * i, z0 + dz * i
        for j in (-1, 0, 1):
            px, pz = cx + rx * j, cz + rz * j
            gp = g(px, pz)
            if gp >= y:
                continue     # still on the beach: the dock starts where the ground is lower
            o.s(px, y, pz, PLANK if j == 0 else "guhs:roze_lakhout_planken")
            o.f(px, y + 1, pz, px, y + 3, pz, LUCHT)
            if i % 3 == 0 and j != 0:
                o.f(px, gp, pz, px, y - 1, pz, PAAL)
    ladder(o, x1, z1, dx, dz, y)
    # bollard + lampion at the end, a fence post on each side
    ex, ez = x1, z1
    for j in (-1, 1):
        o.s(ex + rx * j, y + 1, ez + rz * j, "minecraft:cherry_fence")
    o.s(ex + rx * -1, y + 2, ez + rz * -1, "guhs:lampion_roze[hanging=false]")
    # the beach end: make sure the step onto the dock is at most 1 block
    gw = g(x0 - dx, z0 - dz)
    if gw > y + 1:
        o.f(x0 - dx - rx, y + 1, z0 - dz - rz, x0 - dx + rx, gw + 2, z0 - dz + rz, LUCHT)
        o.f(x0 - dx - rx, y, z0 - dz - rz, x0 - dx + rx, y, z0 - dz + rz, PLANK)


def ladder(o, ex, ez, dx, dz, y):
    """A ladder from the water up onto the end of a dock (deck y): swimmers can always climb out."""
    o.f(ex, ZEE - 2, ez, ex, y - 1, ez, PLANK)
    lx, lz = ex + dx, ez + dz
    fac = RICHTING[(dx, dz)]
    for yy in range(ZEE - 2, y + 1):
        o.s(lx, yy, lz, f"minecraft:ladder[facing={fac},waterlogged={'true' if yy <= ZEE else 'false'}]")
    o.f(lx, y + 1, lz, lx, y + 2, lz, LUCHT)


# ------------------------------------------------------------------------------------------------ the big lighthouse
def vuurtoren(o):
    """Door (south) + a spiral stair on the 5x5 ring round a calcite core: one stair up per side cell, the 4 corners are
    landings (level with the stair before them, so every stair has a floor behind and in front), up to level yt-1; there the
    stair turns outward under the gallery and one last stair comes up through a hatch in the gallery floor (yt)."""
    e = PL.EIL["Vuurtoreneiland"]
    cx, cz = e[1], e[2]
    y = max(g(x, z) for x in range(cx - 6, cx + 7, 2) for z in range(cz - 6, cz + 7, 2)) + 1    # eilanden.vuurtoreneiland
    yt = y + 38
    o.f(cx, y, cz + 2, cx, y + 2, cz + 8, LUCHT)                    # the door hole through the wall + corridor
    o.f(cx, y - 1, cz + 2, cx, y - 1, cz + 8, "guhs:gladde_knuffelsteen")
    o.f(cx - 1, y, cz - 1, cx + 1, yt - 1, cz + 1, "minecraft:calcite")     # the core
    ring = [(0, 2), (1, 2), (2, 2), (2, 1), (2, 0), (2, -1), (2, -2), (1, -2), (0, -2), (-1, -2), (-2, -2), (-2, -1), (-2, 0),
            (-2, 1), (-2, 2), (-1, 2)]
    hoek = {(2, 2), (2, -2), (-2, -2), (-2, 2)}
    s, i = y, 0
    klaar = False
    while not klaar:
        a = ring[i % 16]
        nb = ring[(i + 1) % 16]
        if a in hoek:
            o.f(cx + a[0], s, cz + a[1], cx + a[0], s + 3, cz + a[1], LUCHT)
            o.s(cx + a[0], s - 1, cz + a[1], "minecraft:smooth_quartz")
            i += 1
            continue
        o.f(cx + a[0], s + 1, cz + a[1], cx + a[0], s + 3, cz + a[1], LUCHT)
        d = (nb[0] - a[0], nb[1] - a[1])
        o.s(cx + a[0], s, cz + a[1], trap_blok(RICHTING[d], "minecraft:quartz_stairs"))
        if s >= yt - 1:
            # landings level with this last stair (through corners) up to the next side cell; from there one stair outward
            # through a hatch in the gallery floor (a pane of the lamp room's glass goes for the head room)
            j = i + 1
            while True:
                c = ring[j % 16]
                o.s(cx + c[0], s, cz + c[1], "minecraft:smooth_quartz")
                o.f(cx + c[0], s + 1, cz + c[1], cx + c[0], yt + 2, cz + c[1], LUCHT)
                if c not in hoek:
                    break
                j += 1
            out = (1, 0) if c[0] == 2 else (-1, 0) if c[0] == -2 else (0, 1) if c[1] == 2 else (0, -1)
            ex, ez = c[0] + out[0], c[1] + out[1]
            o.f(cx + ex, yt, cz + ez, cx + ex, yt + 2, cz + ez, LUCHT)
            o.s(cx + ex, yt, cz + ez, trap_blok(RICHTING[out], "minecraft:quartz_stairs"))
            o.s(cx + ex + out[0], yt, cz + ez + out[1], "guhs:pastelbaksteen_wolkje")
            klaar = True
        s += 1
        i += 1
    return y


# ------------------------------------------------------------------------------------------------ Koraalbaai
def koraalbaai(o):
    e = PL.EIL["Koraalbaai"]
    cx, cz = e[1], e[2]
    y = ZEE + 1          # raft deck on the water surface (stand 64)
    for xx in range(cx - 7, cx + 8):
        for zz in range(cz - 7, cz + 8):
            d = math.hypot(xx - cx, zz - cz)
            if d <= 7.2:
                o.s(xx, y, zz, "minecraft:cherry_planks" if d < 6.2 else "guhs:roze_lakhout_planken")
                o.f(xx, y + 1, zz, xx, y + 4, zz, LUCHT)
                if d > 6.6 and (xx + zz) % 3 == 0:
                    o.s(xx, y + 1, zz, "guhs:lampion_mint[hanging=false]")
    # floating walkway from the end of the diving jetty (z cz-19, deck ZEE+2) down to the raft
    o.s(cx, ZEE + 2, cz - 18, PLANK)
    o.s(cx, ZEE + 1, cz - 17, PLANK)
    for zz in range(cz - 17, cz - 6):
        o.f(cx - 1, y, zz, cx + 1, y, zz, PLANK)
        o.f(cx - 1, y + 1, zz, cx + 1, y + 3, zz, LUCHT)
    o.s(cx, ZEE + 2, cz - 17, trap_blok("north", "minecraft:cherry_stairs"))
    o.s(cx, ZEE + 3, cz - 19, LUCHT)    # the kit's fence post at the jetty end would block the way
    # small rafts round the lagoon: never more than ~8 swims from something to climb onto
    for k in range(10):
        h = k * math.pi / 5 + 0.3
        for rr in (14, 24):
            if rr == 24 and k % 2:
                continue
            vx, vz = int(cx + math.cos(h) * rr), int(cz + math.sin(h) * rr)
            if g(vx, vz) >= ZEE:
                continue
            o.f(vx - 1, ZEE + 1, vz - 1, vx + 1, ZEE + 1, vz + 1, "guhs:roze_lakhout_planken")
            o.f(vx - 1, ZEE + 2, vz - 1, vx + 1, ZEE + 3, vz + 1, LUCHT)
    # the grottos: a 3x3 shaft up to the lagoon floor with an upward bubble column (fast way out + air)
    import eilanden as EI
    for (dx_, dz_, r) in EI.GROTJES:
        gx, gz = cx + dx_, cz + dz_
        gy = g(gx, gz) - r + 2
        bodem = int(gy - r * 0.55)
        o.f(gx + 2, bodem, gz, gx + 2, ZEE, gz, "minecraft:water")
        o.s(gx + 2, bodem - 1, gz, "minecraft:soul_sand")
        o.f(gx + 2, bodem, gz, gx + 2, ZEE, gz, "minecraft:bubble_column[drag=false]")
        o.f(gx + 1, bodem, gz - 1, gx + 3, g(gx + 2, gz) + 1, gz + 1, "minecraft:water")
        o.f(gx + 2, bodem, gz, gx + 2, ZEE, gz, "minecraft:bubble_column[drag=false]")
        o.s(gx + 2, bodem - 1, gz, "minecraft:soul_sand")


# ------------------------------------------------------------------------------------------------ the Wrak dungeon
def wrak(o):
    x0, x1, z0, z1 = PL.WRAK_HULL
    zm = (z0 + z1) // 2
    V, D = PL.WRAK_VLOER, PL.WRAK_DEK          # floor block V (stand V + 1), deck block D (stand D + 1)
    hout, donker = "minecraft:dark_oak_planks", "minecraft:spruce_planks"
    # the rock it is stranded on
    for xx in range(x0 - 6, x1 + 9):
        for zz in range(z0 - 6, z1 + 7):
            q = ((xx - (x0 + x1) / 2) / ((x1 - x0) / 2 + 7)) ** 2 + ((zz - zm) / ((z1 - z0) / 2 + 5)) ** 2
            if q <= 1:
                gp = g(xx, zz)
                o.f(xx, gp, zz, xx, ZEE + 1 + int(2 * (1 - q)), zz, "minecraft:tuff")
    # hull: shell, floor, deck, railing
    o.f(x0, V - 1, z0, x1, D, z1, hout)
    o.f(x0 + 1, V + 1, z0 + 1, x1 - 1, D - 1, z1 - 1, LUCHT)
    o.f(x0 + 1, V, z0 + 1, x1 - 1, V, z1 - 1, donker)
    o.f(x0, D, z0, x1, D, z1, donker)
    o.f(x0 + 1, D + 1, z0 + 1, x1 - 1, D + 5, z1 - 1, LUCHT)
    for (a, b, c, d) in ((x0, z0, x1, z0), (x0, z1, x1, z1), (x0, z0, x0, z1)):
        o.f(a, D + 1, b, c, D + 1, d, "minecraft:dark_oak_fence")
    o.f(x0 - 4, D - 2, zm, x0 - 1, D - 1, zm, hout)                # bowsprit
    for xx in range(x0, x1 + 1, 3):
        o.s(xx, D - 2, z0, "minecraft:gray_concrete")
        o.s(xx, D - 2, z1, "minecraft:gray_concrete")
    # 3 rooms (stern -> bow): ruim x 2171..2179, kombuis 2162..2169, knuffelkooi 2151..2160; walls with a 2-high doorway
    for xw in (2170, 2161):
        o.f(xw, V + 1, z0 + 1, xw, D - 1, z1 - 1, donker)
        o.f(xw, V + 1, zm, xw, V + 2, zm, LUCHT)
    for xl in (2175, 2165, 2155):
        o.s(xl, D - 1, zm, "minecraft:lantern[hanging=true,waterlogged=false]")
    # the stern: a walkway from the dock over the rock, 3 stairs up onto the deck, a gap in the stern railing
    o.f(x1 + 4, ZEE + 1, zm - 1, x1 + 13, ZEE + 1, zm + 1, PLANK)
    o.f(x1 + 1, ZEE + 2, zm - 1, x1 + 13, ZEE + 6, zm + 1, LUCHT)
    for i, xx in enumerate((x1 + 3, x1 + 2, x1 + 1)):      # 64, 65, 66: the last one level with the deck block
        o.f(xx, ZEE + 2 + i, zm - 1, xx, ZEE + 2 + i, zm + 1, trap_blok("west", "minecraft:spruce_stairs"))
        o.f(xx, ZEE + 1, zm - 1, xx, ZEE + 1 + i, zm + 1, "minecraft:tuff")
        o.f(xx, ZEE + 3 + i, zm - 1, xx, ZEE + 5 + i, zm + 1, LUCHT)
    o.f(x1, D + 1, z0 + 1, x1, D + 1, z1 - 1, "minecraft:dark_oak_fence")
    o.f(x1, D + 1, zm - 1, x1, D + 2, zm + 1, LUCHT)
    for xx in range(x1 + 9, x1 + 14, 4):
        for zz in (zm - 1, zm + 1):
            o.f(xx, g(xx, zz), zz, xx, ZEE, zz, PAAL)
    ladder(o, x1 + 13, zm, 1, 0, ZEE + 1)
    o.s(x1 + 13, ZEE + 2, zm + 1, "minecraft:cherry_fence")
    o.s(x1 + 13, ZEE + 3, zm + 1, "guhs:lampion_roze[hanging=false]")
    # the hatch in the stern deck: 9 stairs down along +z into the ruim (x 2176..2177)
    #   (the first one level with the deck block behind it at z0+1, the last one at V+2 with a step block at V+1 below it)
    for i in range(9):
        zz = z0 + 2 + i
        o.f(2176, D + 1 - i, zz, 2177, D + 3 - i, zz, LUCHT)
        o.f(2176, D - i, zz, 2177, D - i, zz, trap_blok("north", "minecraft:spruce_stairs"))
    o.f(2176, V + 1, z0 + 11, 2177, V + 1, z0 + 11, donker)
    o.f(2176, V + 2, z0 + 11, 2177, V + 4, z0 + 11, LUCHT)
    o.f(2176, D + 1, z0 + 1, 2177, D + 1, z0 + 1, LUCHT)
    # props: barrels (ruim), stove (kombuis), caged knuffels + the treasure chest (knuffelkooi)
    o.s(2179, V + 1, z0 + 1, "minecraft:barrel[facing=up]")
    o.s(2179, V + 1, z0 + 2, "minecraft:barrel[facing=up]")
    o.s(2179, V + 1, z1 - 1, "minecraft:barrel[facing=up]")
    o.s(2165, V + 1, z1 - 1, "minecraft:smoker[facing=north,lit=false]")
    o.s(2163, V + 1, z1 - 1, "minecraft:barrel[facing=up]")
    for xx in range(x0 + 2, 2160, 2):
        o.s(xx, V + 1, z0 + 1, "guhs:knuffel_mager[facing=south]")
    o.f(x0 + 1, V + 1, z0 + 2, 2159, V + 3, z0 + 2, "minecraft:iron_bars")
    o.s(x0 + 3, V + 1, zm + 3, "minecraft:chest[facing=north]")
    # 9 stairs from the knuffelkooi up east through a hatch onto the fore deck (z zm+3..zm+4, x 2152..2160)
    #   (V+1 .. D: the first one next to the floor, the last one level with the deck block east of it)
    for i in range(D - V):
        xx = x0 + 2 + i
        o.f(xx, V + 2 + i, zm + 3, xx, V + 4 + i, zm + 4, LUCHT)
        o.f(xx, V + 1 + i, zm + 3, xx, V + 1 + i, zm + 4, trap_blok("east", "minecraft:spruce_stairs"))
    # the war-kite mast on the fore deck (the boss arena)
    mx = x0 + 4
    o.f(mx, D + 1, zm - 4, mx, D + 12, zm - 4, "minecraft:dark_oak_fence")
    for i in range(6):
        for j in range(1, 6 - i // 2):
            o.s(mx + j, D + 11 - i, zm - 4, "minecraft:gray_wool" if (i + j) % 3 else "minecraft:black_wool")


# ------------------------------------------------------------------------------------------------ kites + misc
WOL = [((255, 120, 170), "pink"), ((140, 220, 250), "light_blue"), ((255, 230, 120), "yellow"), ((180, 150, 250), "purple"),
       ((130, 230, 170), "lime"), ((255, 170, 120), "orange"), ((255, 255, 255), "white"), ((250, 90, 140), "magenta")]


def wol(kleur):
    return "minecraft:%s_wool" % min(WOL, key=lambda w: sum((a - b) ** 2 for a, b in zip(w[0], kleur)))[1]


def kampvuren(o):
    for k, v in PL.plekken().items():
        if k.startswith("kampvuur_"):
            x, y, z = v
            o.f(x - 1, y, z - 1, x + 1, y + 2, z + 1, LUCHT)
            o.s(x, y - 1, z, "minecraft:cobblestone" if g(x, z) < y - 1 else "guhs:roze_zand")
            o.s(x, y, z, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")


def geheimen(o):
    P = PL.plekken()
    for k, v in P.items():
        if k.startswith("geheim_knuffel_"):
            x, y, z = v
            o.s(x, y, z, "guhs:knuffel_normal[facing=south]")
        elif k.startswith("geheim_schat_"):
            x, y, z = v
            o.s(x, y, z, "minecraft:chest[facing=south]")
    # the secret raft under the Opstijgpier platform
    s = P["geheim_schat_1"]
    o.f(s[0] - 1, ZEE + 1, s[2] - 1, s[0] + 1, ZEE + 1, s[2] + 1, PLANK)


def mysterie(o):
    P = PL.plekken()
    x, y, z = P["mysterie_strand"]
    # a path of flat stones from the beach up to the gate
    a, b = P["mysterie_strand"], P["mysterie_poort"]
    pad(o, (a[0], a[2]), (b[0], b[2]), "guhs:gladde_knuffelsteen")


def pad(o, a, b, blok="guhs:knuffelklinkers", breed=2, grond=None):
    """A 4-connected path from a to b (x, z) on the ground (grond(x, z) -> top block; default the natural ground), smoothed so
    no step is more than 1 (filled up, or cut into the slope with 3 blocks of headroom), `breed` cells wide."""
    grond = grond or (lambda x, z: max(g(x, z), ZEE + 1))
    n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1])))
    if n == 0:
        return
    cel = []
    for t in range(n + 1):
        c = (int(round(a[0] + (b[0] - a[0]) * t / n)), int(round(a[1] + (b[1] - a[1]) * t / n)))
        if cel and c[0] != cel[-1][0] and c[1] != cel[-1][1]:
            cel.append((c[0], cel[-1][1]))      # no diagonal steps: go round the corner
        cel.append(c)
    gr = [grond(x, z) for x, z in cel]
    y = list(gr)
    for i in range(1, len(y)):
        y[i] = max(min(y[i], y[i - 1] + 1), y[i - 1] - 1)
    for i in range(len(y) - 2, -1, -1):
        y[i] = max(min(y[i], y[i + 1] + 1), y[i + 1] - 1)
    langs_z = abs(b[1] - a[1]) > abs(b[0] - a[0])
    kol = {}
    for j in range(breed):                 # the main strip first; a column gets ONE height (corner cells overlap)
        for (x, z), yy in zip(cel, y):
            px, pz = (x + j, z) if langs_z else (x, z + j)
            kol.setdefault((px, pz), yy)
    for (px, pz), yy in kol.items():
        if True:
            gg = grond(px, pz)
            if gg < yy:
                o.f(px, gg + 1, pz, px, yy - 1, pz, "minecraft:pink_terracotta")
            o.s(px, yy, pz, blok)
            o.f(px, yy + 1, pz, px, yy + 3, pz, LUCHT)


def paden(o):
    """Paths that connect each island's dock to its heart (only on islands where the slope needs it)."""
    P = PL.plekken()
    for eid, doel in (("vuurtoreneiland", "vuurtoren_deur"), ("vliegereiland", "werkplaats_vlieger"),
                      ("pierkop", "opstijgpier_begin"), ("mysterie", "mysterie_strand"), ("koraalbaai", "duiksteiger")):
        w = P.get(f"boot_dok_{eid}_wal")
        d = P[doel]
        if w:
            pad(o, (w[0], w[2]), (d[0], d[2]), "guhs:knuffelklinkers" if eid != "mysterie" else "guhs:gladde_knuffelsteen")


def turtle(o):
    """The static Schildpadeiland (fallback until the moving turtle entity exists; the island is not in the height field): a
    floating dock on its right flank with a ladder, and a path up the shell to the garden on its back."""
    x0, z0, fx, fz = PL.schildpad_assen()
    gr = PL.schildpad_top
    zx, zz = PL.schildpad_lok(0, PL.SCHILDPAD_RZ + 2)
    dx, dz = (1 if -fz > 0.5 else -1 if -fz < -0.5 else 0), (1 if fx > 0.5 else -1 if fx < -0.5 else 0)
    if dx and dz:
        dz = 0
    zx, zz = int(round(zx)), int(round(zz))
    # 3 x 6 floating dock (deck on the water surface), pointing away from the shell
    for i in range(0, 6):
        for j in (-1, 0, 1):
            px, pz = zx + dx * i + (j if dz else 0), zz + dz * i + (j if dx else 0)
            o.s(px, ZEE + 1, pz, PLANK)
            o.f(px, ZEE + 2, pz, px, ZEE + 4, pz, LUCHT)
    ladder(o, zx + dx * 5, zz + dz * 5, dx, dz, ZEE + 1)
    rx, rz = PL.schildpad_lok(6, 0)
    pad(o, (zx - dx, zz - dz), (int(rx), int(rz)), "minecraft:lime_terracotta", grond=lambda x, z: max(gr(x, z), ZEE + 1))
    # no water cave under the shell: rock from the sea floor up to the shell (the turtle "stands" on a reef until it swims)
    for x in range(x0 - 26, x0 + 27):
        for z in range(z0 - 26, z0 + 27):
            u = (x - x0) * fx + (z - z0) * fz
            v = -(x - x0) * fz + (z - z0) * fx
            if (u / PL.SCHILDPAD_R) ** 2 + (v / PL.SCHILDPAD_RZ) ** 2 <= 0.92:
                o.f(x, g(x, z), z, x, ZEE - 5, z, "guhs:zeegroen_steen")


def maak_ops():
    o = Ops()
    import plan4
    for h in plan4.huizen():
        interieur(o, h)
        deur_trap(o, h)
    haven(o)
    for eid in PL.EILANDEN:
        if eid != "lampionhaven":
            dok(o, eid)
    vuurtoren(o)
    koraalbaai(o)
    wrak(o)
    paden(o)
    turtle(o)
    mysterie(o)
    kampvuren(o)
    geheimen(o)
    return o.l


def ops():
    global _OPS
    if _OPS is None:
        _OPS = maak_ops()
    return _OPS


def bouw(W):
    x1, y1, z1, x2, y2, z2 = W.b
    for (a, b, c, d, e, f, s) in ops():
        if d < x1 or a >= x2 or f < z1 or c >= z2:
            continue
        W.wfill(a, b, c, d, e, f, s)
