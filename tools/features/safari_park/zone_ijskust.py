"""
Zone module IJskust (ontwerp 1.4): the landmarks of the ice coast around the sea, built on the base terrain.

  pinguinglijbaan   an ice slide from a snow platform down into the sea (players slide too)
  ijsbergkliffen    jagged ice spires on the north shore
  onderwatertunnel  a walkable glass tube from the east shore under the sea to a glass dome next to the wreck
  poolstation       a little research station with a vuurtoren and a hot chocolate kiosk
  zeehondenrotsen   flat rocks in the sea
  schotsen-springpad  floes in a row from the south shore to a small iceberg (the ijsbeer sleeps there)
  iglo-dorpje       a few igloos round a campfire
  scheepswrak       a ship half frozen in a field of pack ice; a barrel in the hold is the geheimpje spot
  uitkijktoren + schuilhut

Also the shared helpers the other zone modules may use (zoek, toren, schuilhut, ...).
plekken(park, plekken) moves the homes of the ijskust animals to their landmarks.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES

ZONE = "ijskust"
LUCHT = 0


# ======================================================================================================================
# shared helpers
def _pad_afstand(park):
    if getattr(park, "_pad_d", None) is None:
        park._pad_d = distance_transform_edt(~park.pad)
    return park._pad_d


def _water_afstand(park):
    if getattr(park, "_water_d", None) is None:
        park._water_d = distance_transform_edt(~park.water)
    return park._water_d


def mag_bouwen(park, x, z, marge=4):
    """Inside the park, not on a path, never within `marge` of a fence (escape rule: nothing that makes a ledge)."""
    return (0 <= x < kit.W and 0 <= z < kit.W and park.binnen[x, z] and not park.pad[x, z]
            and park.hek_afstand[x, z] > marge)


def zoek(park, zone, doel, r=3, water=False, max_helling=2, pad_marge=3, hek_marge=6, max_r=60):
    """The column nearest to `doel` (x, z) in `zone` where a thing of radius r fits: (not) water, flat enough, away
    from paths and fences. Returns (x, z) or None."""
    zi = ZONES.index(zone)
    pd = _pad_afstand(park)
    tx, tz = doel
    best = None
    for rr in range(0, max_r):
        for a in range(max(1, int(2 * math.pi * rr))):
            hoek = a / max(1, int(2 * math.pi * rr)) * 2 * math.pi
            x, z = int(round(tx + math.cos(hoek) * rr)), int(round(tz + math.sin(hoek) * rr))
            if not (r <= x < kit.W - r and r <= z < kit.W - r):
                continue
            blok_z = park.zone[x - r:x + r + 1, z - r:z + r + 1]
            if (blok_z != zi).any():
                continue
            if park.water[x - r:x + r + 1, z - r:z + r + 1].any() != water and not water:
                continue
            if water and not park.water[x - r:x + r + 1, z - r:z + r + 1].all():
                continue
            if (park.hek_afstand[x - r:x + r + 1, z - r:z + r + 1] <= hek_marge).any():
                continue
            if (pd[x - r:x + r + 1, z - r:z + r + 1] <= pad_marge).any():
                continue
            h = park.hoogte[x - r:x + r + 1, z - r:z + r + 1]
            if not water and int(h.max()) - int(h.min()) > max_helling:
                continue
            return x, z
    return best


def ruim_kolom(park, x, z, y_van, y_tot=None):
    """Air from y_van up (trees, plants, snow layers), but never a fence."""
    y_tot = kit.H - 1 if y_tot is None else y_tot
    for y in range(max(0, y_van), min(kit.H, y_tot + 1)):
        s = park.get(x, y, z)
        if any(f in s for f in kit.FENCE_FAMILY) or "fence_gate" in s:
            continue
        park.v[x, y, z] = LUCHT


def egaliseer(park, cx, cz, r, y, top="minecraft:snow_block", sub="minecraft:stone", rond=True, ruim=12):
    """Make a (round) pad of radius r flat at ground level y: fill up or cut down, clear `ruim` blocks above."""
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            if rond and math.hypot(x - cx, z - cz) > r + 0.4:
                continue
            if not mag_bouwen(park, x, z, 3) or park.water[x, z]:
                continue
            g = int(park.hoogte[x, z])
            for yy in range(min(g, y) - 3, y):
                if park.v[x, yy, z] == 0 or yy > g - 1:
                    park.set(x, yy, z, sub)
            park.set(x, y, z, top)
            ruim_kolom(park, x, z, y + 1, y + ruim)
            park.hoogte[x, z] = y


def grond(park, x, z):
    return int(park.hoogte[x, z])


def toren(park, cx, cz, hout="spruce", dak="minecraft:spruce_slab[type=bottom,waterlogged=false]", hoog=10,
          vloer=None):
    """A wooden lookout tower (5x5, platform at +hoog, railing, roof, a ladder inside, a 'telescope' on the rail)."""
    y = max(grond(park, x, z) for x in range(cx - 2, cx + 3) for z in range(cz - 2, cz + 3))
    egaliseer(park, cx, cz, 3, y, top=park.get(cx, grond(park, cx, cz), cz), rond=False)
    log = f"minecraft:{hout}_log[axis=y]"
    planks = vloer or f"minecraft:{hout}_planks"
    for dx in (-2, 2):
        for dz in (-2, 2):
            for i in range(1, hoog + 5):
                park.set(cx + dx, y + i, cz + dz, log)
    # spine with the ladder
    for i in range(1, hoog + 1):
        park.set(cx, y + i, cz, log)
        park.set(cx, y + i, cz + 1, "minecraft:ladder[facing=south,waterlogged=false]")
    park.fill(cx - 2, y + hoog, cz - 2, cx + 2, y + hoog, cz + 2, planks)
    park.set(cx, y + hoog, cz + 1, "minecraft:air")
    park.set(cx, y + hoog, cz, log)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 or abs(dz) == 2:
                if not (abs(dx) == 2 and abs(dz) == 2):
                    park.set(cx + dx, y + hoog + 1, cz + dz, f"minecraft:{hout}_fence")
    # cross braces low down
    for i in (3, 6):
        for dz in range(-1, 2):
            park.set(cx - 2, y + i, cz + dz, f"minecraft:{hout}_fence")
            park.set(cx + 2, y + i, cz + dz, f"minecraft:{hout}_fence")
    # roof: a small pyramid of slabs
    park.fill(cx - 3, y + hoog + 5, cz - 3, cx + 3, y + hoog + 5, cz + 3, dak)
    park.fill(cx - 2, y + hoog + 6, cz - 2, cx + 2, y + hoog + 6, cz + 2, dak)
    park.fill(cx - 1, y + hoog + 6, cz - 1, cx + 1, y + hoog + 6, cz + 1, planks)
    park.set(cx, y + hoog + 7, cz, dak)
    park.set(cx, y + hoog + 4, cz, "minecraft:lantern[hanging=true,waterlogged=false]")
    # the 'verrekijker' on the rail (a lightning rod lying down looks like a telescope)
    park.set(cx + 1, y + hoog + 2, cz - 2, "minecraft:lightning_rod[facing=north,powered=false,waterlogged=false]")
    return y + hoog


def schuilhut(park, cx, cz, kijk=(0, -1), hout="spruce", muur=None, dak=None):
    """A small bird hide (5x5): slit windows at eye height toward `kijk`, the door on the other side."""
    y = max(grond(park, x, z) for x in range(cx - 2, cx + 3) for z in range(cz - 2, cz + 3))
    egaliseer(park, cx, cz, 3, y, top=park.get(cx, grond(park, cx, cz), cz), rond=False)
    muur = muur or f"minecraft:{hout}_planks"
    dak = dak or f"minecraft:{hout}_slab[type=bottom,waterlogged=false]"
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            rand = abs(dx) == 2 or abs(dz) == 2
            for i in range(1, 4):
                park.set(cx + dx, y + i, cz + dz, muur if rand else "minecraft:air")
            park.set(cx + dx, y + 4, cz + dz, dak)
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for i in range(1, 4):
            park.set(cx + dx, y + i, cz + dz, f"minecraft:{hout}_log[axis=y]")
    kx, kz = kijk
    for t in range(-1, 2):                                  # the slit, 3 wide
        sx, sz = (cx + t, cz + 2 * kz) if kz else (cx + 2 * kx, cz + t)
        park.set(sx, y + 2, sz, "minecraft:air")
    dx, dz = -kx, -kz                                       # the door
    for i in (1, 2):
        park.set(cx + 2 * dx, y + i, cz + 2 * dz, "minecraft:air")
    park.set(cx, y + 1, cz, f"minecraft:{hout}_slab[type=bottom,waterlogged=false]")   # a bench
    return y


def blob(park, cx, cy, cz, rx, ry, rz, keuze, rng, alleen_lucht=False, alleen_water=False):
    """Ellipsoid of random blocks from `keuze` [(block, weight)]."""
    namen = [k for k, _ in keuze]
    w = np.array([g for _, g in keuze], float)
    w /= w.sum()
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 > 1:
                    continue
                if not park.inb(x, y, z):
                    continue
                s = park.get(x, y, z)
                if alleen_lucht and park.v[x, y, z] != 0 and "water" not in s:
                    continue
                if alleen_water and "water" not in s and park.v[x, y, z] != 0:
                    continue
                park.set(x, y, z, namen[int(rng.choice(len(namen), p=w))])


def kolom_top_vrij(park, x, z):
    """First y above the ground at (x, z) that is air (for an animal home)."""
    y = int(park.hoogte[x, z]) + 1
    while y < kit.H - 2 and park.v[x, y, z] != 0 and not park.get(x, y, z).startswith(
            ("minecraft:snow[", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:dead_bush")):
        y += 1
    return y


def zet_plek(plekken, soort, nr, x, y, z, straal=None):
    """Move the nr-th home of `soort` (0-based)."""
    eigen = [p for p in plekken if p["soort"] == soort]
    if nr < len(eigen):
        eigen[nr]["centrum"] = [int(x), int(y), int(z)]
        if straal is not None:
            eigen[nr]["straal"] = int(straal)


# ======================================================================================================================
# the sea
def _zee(park):
    zi = ZONES.index(ZONE)
    m = park.water & (park.zone == zi)
    xs, zs = np.nonzero(m)
    cx, cz = int(xs.mean()), int(zs.mean())
    return m, cx, cz


def _land_bij(park, doel, richting, zone=ZONE):
    """Walk from doel (in the water) in `richting` until the first land column of the zone, plus 2."""
    x, z = doel
    dx, dz = richting
    n = math.hypot(dx, dz)
    dx, dz = dx / n, dz / n
    zi = ZONES.index(zone)
    for t in range(0, 80):
        px, pz = int(round(x + dx * t)), int(round(z + dz * t))
        if not park.water[px, pz] and park.zone[px, pz] == zi:
            return int(round(x + dx * (t + 2))), int(round(z + dz * (t + 2)))
    return None


# ======================================================================================================================
def ijsbergkliffen(park, rng):
    cx, cz, r, _ = kit.RESERVERINGEN["ijsbergkliffen"]
    for k in range(16):
        a = rng.random() * 2 * math.pi
        d = rng.random() * r
        x, z = int(cx + math.cos(a) * d), int(cz + math.sin(a) * d)
        if not mag_bouwen(park, x, z, 7):
            continue
        g = grond(park, x, z) if not park.water[x, z] else kit.WATER_Y - 2
        top = int(70 + rng.random() * 18)
        rr = 1.6 + rng.random() * 2.4
        for y in range(g - 2, top + 1):
            f = 1 - (y - g) / max(1, (top - g))
            rad = max(0.7, rr * (0.45 + 0.55 * f))
            for xx in range(int(x - rad) - 1, int(x + rad) + 2):
                for zz in range(int(z - rad) - 1, int(z + rad) + 2):
                    if math.hypot(xx - x, zz - z) <= rad and mag_bouwen(park, xx, zz, 6):
                        s = "minecraft:packed_ice" if rng.random() < 0.7 else (
                            "minecraft:blue_ice" if rng.random() < 0.6 else "minecraft:snow_block")
                        park.set(xx, y, zz, s)
        park.set(x, top + 1, z, "minecraft:snow_block")
        if not park.water[x, z]:
            park.hoogte[x, z] = max(park.hoogte[x, z], top + 1)


def zeehondenrotsen(park, rng, zee, sx, sz):
    """Three flat rocks in the north-west of the sea. Returns the top of the biggest one (for the seals)."""
    plekken = []
    for (ox, oz, r) in ((-26, -14, 3.2), (-20, -24, 2.6), (-31, -2, 2.4)):
        x, z = sx + ox, sz + oz
        if not zee[x, z]:
            continue
        blob(park, x, kit.WATER_Y, z, r, 1.6, r, [("minecraft:stone", 3), ("minecraft:andesite", 2),
                                                  ("minecraft:diorite", 1)], rng)
        # a flat top just above the water
        for xx in range(int(x - r), int(x + r) + 1):
            for zz in range(int(z - r), int(z + r) + 1):
                if math.hypot(xx - x, zz - z) <= r - 0.6:
                    for y in range(kit.WATER_Y + 2, kit.WATER_Y + 4):
                        park.set(xx, y, zz, "minecraft:air")
                    park.set(xx, kit.WATER_Y + 1, zz, "minecraft:smooth_stone")
        plekken.append((x, kit.WATER_Y + 2, z))
    return plekken


def springpad(park, rng, zee, sx, sz):
    """Floes from the south shore to a small iceberg. Returns the iceberg top (for the ijsbeer)."""
    ix, iz = sx - 16, sz + 14
    land = _land_bij(park, (ix, iz), (-0.4, 1))
    blob(park, ix, kit.WATER_Y + 1, iz, 5, 5, 5, [("minecraft:packed_ice", 5), ("minecraft:snow_block", 2),
                                                  ("minecraft:blue_ice", 1)], rng)
    for x in range(ix - 6, ix + 7):              # cut the underside of the berg off at the sea bed, flat top snow
        for z in range(iz - 6, iz + 7):
            col = [y for y in range(kit.WATER_Y + 1, kit.WATER_Y + 8) if park.v[x, y, z] != 0
                   and "water" not in park.get(x, y, z)]
            if col:
                park.set(x, col[-1], z, "minecraft:snow_block")
    top = (ix, kit.WATER_Y + 7, iz)
    if land:
        lx, lz = land
        n = int(math.hypot(lx - ix, lz - iz))
        for t in range(6, n - 1, 3):
            fx = int(round(ix + (lx - ix) * t / n))
            fz = int(round(iz + (lz - iz) * t / n))
            for dx in (0, 1):
                for dz in (0, 1):
                    if zee[fx + dx, fz + dz]:
                        park.set(fx + dx, kit.WATER_Y, fz + dz, "minecraft:packed_ice")
    return top


def scheepswrak(park, rng, sx, sz):
    """A ship frozen in the pack ice, bow to the east. A barrel in the hold = the geheimpje spot."""
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    # pack ice field
    for x in range(wx - 13, wx + 14):
        for z in range(wz - 11, wz + 12):
            d = math.hypot((x - wx) / 1.2, z - wz) + rng.random() * 2.5
            if d < 10.5 and park.water[x, z]:
                park.set(x, kit.WATER_Y, z, "minecraft:packed_ice" if rng.random() < 0.8 else "minecraft:ice")
                if rng.random() < 0.25:
                    park.set(x, kit.WATER_Y + 1, z, "minecraft:snow[layers=1]")
    L = 9                                                   # half length
    for dx in range(-L, L + 1):
        t = abs(dx) / L
        w = 3 if dx < L - 3 else max(0, 3 - (dx - (L - 3)))     # pointed bow at +x
        if dx < -L + 1:
            w = 2
        x = wx + dx
        bodem = kit.WATER_Y - 4 + int(t * 1.5)
        for dz in range(-w, w + 1):
            z = wz + dz
            for y in range(bodem, kit.WATER_Y + 3):
                rand = abs(dz) == w or dx in (-L, L) or y == bodem
                if rand:
                    s = "minecraft:dark_oak_planks" if y in (kit.WATER_Y + 1, bodem) else "minecraft:spruce_planks"
                    if y == kit.WATER_Y + 2:
                        s = "minecraft:dark_oak_fence"
                    park.set(x, y, z, s)
                else:
                    park.set(x, y, z, "minecraft:air")
            park.set(x, kit.WATER_Y + 1, z, "minecraft:spruce_planks" if abs(dz) < w else park.get(x, kit.WATER_Y + 1, z))
        # the hold is half full of ice
        for dz in range(-w + 1, w):
            for y in range(bodem + 1, bodem + 3):
                if rng.random() < 0.55:
                    park.set(x, y, wz + dz, "minecraft:packed_ice")
    # deck: frost and a hatch with a ladder down to the barrel
    for dx in range(-L + 1, L - 2):
        for dz in range(-2, 3):
            if rng.random() < 0.3:
                park.set(wx + dx, kit.WATER_Y + 2, wz + dz, "minecraft:snow[layers=1]")
    hx = wx - 3
    park.set(hx, kit.WATER_Y + 1, wz, "minecraft:air")
    for y in range(kit.WATER_Y - 3, kit.WATER_Y + 2):
        park.set(hx, y, wz, "minecraft:air")
        park.set(hx, y, wz - 1, "minecraft:air")
    for y in range(kit.WATER_Y - 3, kit.WATER_Y + 1):
        park.set(hx, y, wz + 1, "minecraft:spruce_planks")
        park.set(hx, y, wz, "minecraft:ladder[facing=north,waterlogged=false]")
    park.set(hx, kit.WATER_Y - 3, wz - 1, "minecraft:barrel[facing=up,open=false]")      # het geheimpje
    park.set(hx - 1, kit.WATER_Y - 3, wz - 1, "minecraft:air")
    # mast with a broken top and a torn sail
    mx = wx + 1
    for y in range(kit.WATER_Y + 2, kit.WATER_Y + 14):
        park.set(mx, y, wz, "minecraft:dark_oak_log[axis=y]")
    for k in range(1, 5):
        park.set(mx + k, kit.WATER_Y + 13 - (k // 2), wz, "minecraft:dark_oak_log[axis=x]")
    for dz in range(-3, 4):
        park.set(mx, kit.WATER_Y + 11, wz + dz, "minecraft:spruce_fence")
        for y in range(kit.WATER_Y + 6, kit.WATER_Y + 11):
            if rng.random() < 0.7 and dz != 0:
                park.set(mx, y, wz + dz, "minecraft:white_wool")
    for y in range(kit.WATER_Y + 3, kit.WATER_Y + 13):
        if rng.random() < 0.5:
            park.set(mx, y, wz - 1, "minecraft:ice")


def onderwatertunnel(park, rng, sx, sz):
    """A walkable glass tube from the east shore under the sea to a glass dome next to the wreck.
    Returns (dome centre, points along the tube in the water) for the water animals."""
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    bx, bz = wx + 11, wz + 12                                 # the dome, south-east of the wreck
    ingang = _land_bij(park, (bx, bz), (1, 0.15))
    if ingang is None:
        return None, []
    ax, az = ingang[0] + 4, ingang[1]
    ytop = grond(park, ax, az)
    VLOER = kit.WATER_Y - 9                                  # floor of the tube
    pad = [(ax, az), (bx, bz)]
    lengte = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / lengte, (bz - az) / lengte
    x0, x1 = min(ax, bx) - 4, max(ax, bx) + 4
    z0, z1 = min(az, bz) - 4, max(az, bz) + 4
    vloer_van = {}
    binnenkant, schil = [], []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s = (x - ax) * ux + (z - az) * uz
            if s < -0.5 or s > lengte:
                continue
            d = abs(-(x - ax) * uz + (z - az) * ux)
            vy = max(VLOER, ytop - int(s))
            if d <= 1.4:
                binnenkant.append((x, z, vy, s))
            elif d <= 2.6:
                schil.append((x, z, vy, s))
            vloer_van[(x, z)] = vy
    for x, z, vy, s in schil + binnenkant:
        for y in range(vy, vy + 5):
            park.set(x, y, z, "minecraft:glass")
        park.set(x, vy, z, "minecraft:prismarine_bricks")
    for x, z, vy, s in binnenkant:
        for y in range(vy + 1, vy + 4):
            park.set(x, y, z, "minecraft:air")
        if vy > VLOER:                                       # the stairs down from the shore
            park.set(x, vy, z, "minecraft:prismarine_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
            park.set(x, vy - 1, z, "minecraft:prismarine_bricks")
        elif int(s) % 6 == 0 and abs(-(x - ax) * uz + (z - az) * ux) < 0.6:
            park.set(x, vy, z, "minecraft:sea_lantern")
    # above ground at the entrance: no glass box, an ice arch instead
    for x, z, vy, s in schil + binnenkant:
        if vy >= ytop - 1:
            for y in range(vy + 1, vy + 5):
                park.set(x, y, z, "minecraft:air")
    for t in (-1, 0, 1, 2):
        for dd in (-2, 2):
            x, z = int(round(ax - uz * dd + ux * t)), int(round(az + ux * dd + uz * t))
            for y in range(ytop + 1, ytop + 5):
                park.set(x, y, z, "minecraft:packed_ice")
        for dd in range(-2, 3):
            x, z = int(round(ax - uz * dd + ux * t)), int(round(az + ux * dd + uz * t))
            park.set(x, ytop + 5, z, "minecraft:packed_ice")
    # the dome
    cy = VLOER + 3
    for x in range(bx - 6, bx + 7):
        for z in range(bz - 6, bz + 7):
            for y in range(VLOER - 1, cy + 6):
                d = math.sqrt((x - bx) ** 2 + ((y - cy) * 1.15) ** 2 + (z - bz) ** 2)
                if d <= 5.6 and y >= VLOER:
                    park.set(x, y, z, "minecraft:glass")
    for x in range(bx - 5, bx + 6):
        for z in range(bz - 5, bz + 6):
            for y in range(VLOER + 1, cy + 5):
                d = math.sqrt((x - bx) ** 2 + ((y - cy) * 1.15) ** 2 + (z - bz) ** 2)
                if d <= 4.5:
                    park.set(x, y, z, "minecraft:air")
            if math.hypot(x - bx, z - bz) <= 4.6:
                park.set(x, VLOER, z, "minecraft:prismarine_bricks")
    for x, z, vy, s in binnenkant:                           # the dome's glass must not close the tube
        for y in range(vy + 1, vy + 4):
            park.set(x, y, z, "minecraft:air")
    park.set(bx, VLOER, bz, "minecraft:sea_lantern")
    park.set(bx, VLOER + 1, bz, "minecraft:spruce_slab[type=bottom,waterlogged=false]")       # a bench
    langs = [(int(round(ax + ux * s)), int(round(az + uz * s))) for s in range(8, int(lengte), 5)]
    return (bx, VLOER, bz), langs


def pinguinglijbaan(park, rng, zee):
    cx, cz, _, _ = kit.RESERVERINGEN["pinguinglijbaan"]
    # direction to the nearest water
    wd = _water_afstand(park)
    best = None
    for a in range(0, 360, 10):
        dx, dz = math.cos(math.radians(a)), math.sin(math.radians(a))
        for t in range(0, 30):
            x, z = int(round(cx + dx * t)), int(round(cz + dz * t))
            if zee[x, z]:
                if best is None or t < best[0]:
                    best = (t, dx, dz)
                break
    if best is None:
        return None
    t, dx, dz = best
    # platform on the far side, slide toward the water dropping 1 per 2
    px, pz = int(round(cx - dx * 4)), int(round(cz - dz * 4))
    g = grond(park, px, pz)
    hoog = g + 9
    for x in range(px - 2, px + 3):
        for z in range(pz - 2, pz + 3):
            for y in range(grond(park, x, z) - 1, hoog + 1):
                park.set(x, y, z, "minecraft:snow_block")
            park.hoogte[x, z] = hoog
    # stairs up on the back side
    sx, sz = -dx, -dz
    for k in range(1, hoog - g + 1):
        x, z = int(round(px + sx * (2 + k))), int(round(pz + sz * (2 + k)))
        y = hoog - k
        if not mag_bouwen(park, x, z, 4):
            break
        for w in (-1, 0, 1):
            xx, zz = int(round(x - sz * w)), int(round(z + sx * w))
            for yy in range(grond(park, xx, zz) - 1, y + 1):
                park.set(xx, yy, zz, "minecraft:snow_block")
            ruim_kolom(park, xx, zz, y + 1, y + 4)
    # the slide
    lengte = 2 * (hoog - kit.WATER_Y) + 4
    for k in range(0, lengte):
        y = hoog - k // 2
        x, z = int(round(px + dx * (3 + k))), int(round(pz + dz * (3 + k)))
        water_hier = zee[x, z]
        for w in (-2, -1, 0, 1, 2):
            xx, zz = int(round(x - dz * w)), int(round(z + dx * w))
            if abs(w) == 2:
                if not water_hier:
                    for yy in range(grond(park, xx, zz), y + 2):
                        park.set(xx, yy, zz, "minecraft:snow_block")
            else:
                for yy in range(min(grond(park, xx, zz), y) - 1, y):
                    park.set(xx, yy, zz, "minecraft:snow_block")
                park.set(xx, y, zz, "minecraft:blue_ice" if not water_hier else "minecraft:packed_ice")
                ruim_kolom(park, xx, zz, y + 1, y + 4)
        if water_hier and y <= kit.WATER_Y:
            break
    bodem = (int(round(px + dx * (3 + lengte - 2))), int(round(pz + dz * (3 + lengte - 2))))
    return (px, hoog + 1, pz), bodem


def iglo(park, cx, cz, deur):
    g = grond(park, cx, cz)
    egaliseer(park, cx, cz, 4, g, top="minecraft:snow_block")
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            for y in range(g + 1, g + 5):
                d = math.sqrt((x - cx) ** 2 + ((y - g - 0.5) * 1.15) ** 2 + (z - cz) ** 2)
                if d <= 3.6:
                    park.set(x, y, z, "minecraft:snow_block" if d > 2.6 else "minecraft:air")
    dx, dz = deur
    for k in range(2, 5):                                   # the door tunnel
        x, z = cx + dx * k, cz + dz * k
        for y in (g + 1, g + 2):
            park.set(x, y, z, "minecraft:air")
        park.set(x, g + 3, z, "minecraft:snow_block")
        for w in (-1, 1):
            for y in (g + 1, g + 2):
                park.set(x - dz * w, y, z + dx * w, "minecraft:snow_block")
    park.set(cx, g + 1, cz, "minecraft:white_carpet")
    park.set(cx - dz, g + 1, cz + dx, "minecraft:lantern[hanging=false,waterlogged=false]")


def iglodorpje(park, rng):
    cx, cz, _, _ = kit.RESERVERINGEN["iglo_dorpje"]
    c = zoek(park, ZONE, (cx, cz), r=3, max_helling=3)
    if c is None:
        return None
    cx, cz = c
    g = grond(park, cx, cz)
    egaliseer(park, cx, cz, 3, g, top="minecraft:snow_block")
    park.set(cx, g + 1, cz, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        park.set(cx + dx * 2, g + 1, cz + dz * 2, "minecraft:spruce_log[axis=" + ("x" if dz else "z") + "]")
    for hoek in (20, 110, 200, 290):
        a = math.radians(hoek)
        ix, iz = int(round(cx + math.cos(a) * 9)), int(round(cz + math.sin(a) * 9))
        if all(mag_bouwen(park, ix + dx, iz + dz, 4) and not park.water[ix + dx, iz + dz]
               for dx in (-4, 0, 4) for dz in (-4, 0, 4)):
            # door toward the campfire
            ddx, ddz = cx - ix, cz - iz
            deur = (1, 0) if abs(ddx) >= abs(ddz) and ddx > 0 else (-1, 0) if abs(ddx) >= abs(ddz) else \
                (0, 1) if ddz > 0 else (0, -1)
            iglo(park, ix, iz, deur)
    return cx, g + 1, cz


def poolstation(park, rng):
    cx, cz, _, _ = kit.RESERVERINGEN["poolstation"]
    c = zoek(park, ZONE, (cx, cz), r=6, max_helling=3)
    if c is None:
        return None
    cx, cz = c
    g = max(grond(park, x, z) for x in range(cx - 6, cx + 7) for z in range(cz - 6, cz + 7))
    egaliseer(park, cx, cz, 7, g, top="minecraft:snow_block", rond=False)
    # the station: 7x5, white with a grey roof, windows, a door to the south
    x0, z0 = cx - 6, cz - 5
    for x in range(x0, x0 + 7):
        for z in range(z0, z0 + 5):
            rand = x in (x0, x0 + 6) or z in (z0, z0 + 4)
            for y in range(g + 1, g + 4):
                park.set(x, y, z, "minecraft:white_concrete" if rand else "minecraft:air")
            park.set(x, g + 4, z, "minecraft:light_gray_concrete")
    for x in (x0 + 2, x0 + 4):
        park.set(x, g + 2, z0, "minecraft:glass")
        park.set(x, g + 2, z0 + 4, "minecraft:glass")
    for z in (z0 + 2,):
        park.set(x0, g + 2, z, "minecraft:glass")
    park.set(x0 + 3, g + 1, z0 + 4, "minecraft:air")
    park.set(x0 + 3, g + 2, z0 + 4, "minecraft:air")
    park.set(x0 + 1, g + 1, z0 + 1, "minecraft:crafting_table")
    park.set(x0 + 5, g + 1, z0 + 1, "minecraft:barrel[facing=up,open=false]")
    park.set(x0 + 3, g + 3, z0 + 2, "minecraft:lantern[hanging=true,waterlogged=false]")
    # antenna
    for y in range(g + 5, g + 10):
        park.set(x0 + 1, y, z0 + 1, "minecraft:iron_bars")
    park.set(x0 + 1, g + 10, z0 + 1, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    # the vuurtoren: red and white rings, a lamp room
    vx, vz = cx + 3, cz + 3
    for y in range(g + 1, g + 16):
        kleur = "minecraft:red_concrete" if ((y - g - 1) // 3) % 2 else "minecraft:white_concrete"
        for x in range(vx - 1, vx + 2):
            for z in range(vz - 1, vz + 2):
                park.set(x, y, z, kleur)
    for x in range(vx - 1, vx + 2):
        for z in range(vz - 1, vz + 2):
            park.set(x, g + 16, z, "minecraft:glass" if (x, z) != (vx, vz) else "minecraft:sea_lantern")
            park.set(x, g + 17, z, "minecraft:glass")
            park.set(x, g + 18, z, "minecraft:red_concrete")
    park.set(vx, g + 17, vz, "minecraft:sea_lantern")
    park.set(vx, g + 19, vz, "minecraft:red_concrete")
    # the hot chocolate kiosk: a counter, striped awning, a pot
    kx, kz = cx - 3, cz + 4
    for x in range(kx - 1, kx + 2):
        park.set(x, g + 1, kz, "minecraft:spruce_planks")
        park.set(x, g + 2, kz, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    for x in (kx - 2, kx + 2):
        for y in range(g + 1, g + 4):
            park.set(x, y, kz, "minecraft:spruce_fence")
            park.set(x, y, kz - 2, "minecraft:spruce_fence")
    for x in range(kx - 2, kx + 3):
        for z in range(kz - 2, kz + 1):
            park.set(x, g + 4, z, "minecraft:orange_wool" if (x + z) % 2 else "minecraft:white_wool")
    park.set(kx, g + 1, kz - 1, "minecraft:cauldron")
    park.set(kx + 1, g + 1, kz - 1, "minecraft:brown_concrete")
    park.set(kx - 1, g + 3, kz, "minecraft:candle[candles=1,lit=false,waterlogged=false]")
    return cx, g + 1, cz


# ======================================================================================================================
def bouw(park, log=print):
    rng = np.random.default_rng(kit.SEED + 501)
    zee, sx, sz = _zee(park)
    park._ijskust = {}
    ijsbergkliffen(park, rng)
    park._ijskust["zeehonden"] = zeehondenrotsen(park, rng, zee, sx, sz)
    park._ijskust["ijsberg"] = springpad(park, rng, zee, sx, sz)
    scheepswrak(park, rng, sx, sz)
    park._ijskust["tunnel"] = onderwatertunnel(park, rng, sx, sz)
    park._ijskust["glijbaan"] = pinguinglijbaan(park, rng, zee)
    park._ijskust["iglo"] = iglodorpje(park, rng)
    park._ijskust["station"] = poolstation(park, rng)
    # uitkijktoren on the north-east shore, schuilhut on the west shore looking at the seal rocks
    t = zoek(park, ZONE, (sx + 70, sz - 22), r=3, max_helling=4)
    if t:
        toren(park, *t, hout="birch", dak="minecraft:birch_slab[type=bottom,waterlogged=false]")
    h = zoek(park, ZONE, (sx - 46, sz - 12), r=3, max_helling=3)
    if h:
        schuilhut(park, *h, kijk=(1, 0) if sx > h[0] else (-1, 0))
    log(f"    ijskust: zee {sx},{sz} tunnel {park._ijskust['tunnel'][0]} glijbaan {park._ijskust['glijbaan']}"
        f" toren {t} hut {h}")


def plekken(park, plekken):
    info = getattr(park, "_ijskust", None)
    if not info:
        return
    zee, sx, sz = _zee(park)
    # narwal: the deepest water far from the tunnel and the wreck
    dome, langs = info["tunnel"]
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    xs, zs = np.nonzero(zee)
    diepte = kit.WATER_Y - park.hoogte[xs, zs]
    best = None
    for x, z, d in zip(xs, zs, diepte):
        ver = min(math.hypot(x - dome[0], z - dome[2]) if dome else 99, math.hypot(x - wx, z - wz))
        if ver < 14:
            continue
        sc = d + ver * 0.05
        if best is None or sc > best[0]:
            best = (sc, x, z)
    if best:
        _, x, z = best
        zet_plek(plekken, "narwal", 0, x, int(park.hoogte[x, z]) + 2, z, 12)
    # octopus: on the bed next to the tube
    if langs:
        x, z = langs[len(langs) // 2]
        for (ox, oz) in ((4, 0), (-4, 0), (0, 4), (0, -4)):
            if zee[x + ox, z + oz]:
                zet_plek(plekken, "octopus", 0, x + ox, int(park.hoogte[x + ox, z + oz]) + 1, z + oz, 6)
                break
    # pinguins at the slide: one group on the platform, one at the bottom on the shore
    if info["glijbaan"]:
        top, bodem = info["glijbaan"]
        zet_plek(plekken, "pinguin", 0, top[0], top[1], top[2], 6)
        bx, bz = bodem
        land = None
        for r in range(0, 12):
            for a in range(0, 360, 20):
                x = int(round(bx + math.cos(math.radians(a)) * r))
                z = int(round(bz + math.sin(math.radians(a)) * r))
                if not park.water[x, z] and park.zone[x, z] == ZONES.index(ZONE) and mag_bouwen(park, x, z, 4):
                    land = (x, z)
                    break
            if land:
                break
        if land:
            zet_plek(plekken, "pinguin", 1, land[0], kolom_top_vrij(park, *land), land[1], 10)
    # zeehonden on the biggest rock
    if info["zeehonden"]:
        x, y, z = info["zeehonden"][0]
        zet_plek(plekken, "zeehond", 0, x, y, z, 8)
    # the ijsbeer sleeps on the little iceberg
    if info["ijsberg"]:
        x, y, z = info["ijsberg"]
        top = max(yy for yy in range(kit.WATER_Y, kit.WATER_Y + 10) if park.v[x, yy, z] != 0) + 1
        zet_plek(plekken, "ijsbeer", 0, x, top, z, 7)
