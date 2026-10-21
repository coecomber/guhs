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
    park.set(cx, y + hoog, cz + 1, "minecraft:ladder[facing=south,waterlogged=false]")   # (round 4) up INTO the hatch
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
    kit.toren_kijkplek(park, cx, cz, y + hoog)
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
    kit.stoel(park, cx, y + 1, cz, (kx, kz))                # a guh chair looking out of the slit
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


def extra_plek(plekken, soort, zone, x, y, z, straal, volwassenen=2, jongen=1, water=False, boom=False):
    """Add a home (an extra family) of `soort`."""
    plekken.append({"soort": soort, "zone": zone, "centrum": [int(x), int(y), int(z)], "straal": int(straal),
                    "volwassenen": volwassenen, "jongen": jongen, "water": water, "boom": boom})


def _reset_caches(park):
    park._water_d = None
    park._pad_d = None


# ======================================================================================================================
# test round 2: shared fixes (water that stays where it belongs, bridges a canoe fits under, real viewing huts)
_ZACHT = ("minecraft:air", "short_grass", "tall_grass", "fern", "minecraft:snow[", "dead_bush", "dandelion", "poppy",
          "cornflower", "oxeye_daisy", "allium", "azure_bluet", "tulip", "moss_carpet", "_carpet", "torch", "lily_pad",
          "sugar_cane", "vine", "glow_lichen", "seagrass", "kelp")
_PLANT = ("_leaves", "_log", "vine", "short_grass", "tall_grass", "fern", "dead_bush", "bamboo", "dandelion", "poppy",
          "cornflower", "oxeye_daisy", "allium", "azure_bluet", "tulip", "mushroom", "sugar_cane", "minecraft:snow[",
          "mangrove_roots", "azalea", "jungle_wood", "moss_carpet", "cocoa", "propagule", "lily_pad")


def _lut(park, woorden, ook_lucht=False):
    lut = np.zeros(len(park.pal) + 512, bool)
    for i, s in enumerate(park.pal):
        if any(w in s for w in woorden) and "fence" not in s and "door" not in s:
            lut[i] = True
    if ook_lucht:
        lut[0] = True
    return lut


def _bron_water_lut(park):
    """True for still (source) water: the block a leak starts from (falling water, level 8, is left alone)."""
    lut = np.zeros(len(park.pal) + 512, bool)
    for i, s in enumerate(park.pal):
        if s == "minecraft:water" or s == "minecraft:water[level=0]":
            lut[i] = True
    return lut


def vul_laagtes(park, zone, top="minecraft:snow_block", sub="minecraft:stone", sneeuw=0.0, rng=None):
    """Dry ground below the water line (a basin next to the sea or river) floods in the game as soon as the water next
    to it ticks. Raise every dry column of `zone` whose surface is below WATER_Y up to WATER_Y, so the water stays put.
    Fences (and the strip along them) are left alone: dicht_water() closes the few leaks there."""
    zi = ZONES.index(zone)
    m = (park.zone == zi) & ~park.water & (park.hoogte < kit.WATER_Y) & (park.hek_afstand > 2)
    zacht = _lut(park, _ZACHT, ook_lucht=True)
    for x, z in zip(*np.nonzero(m)):
        h = int(park.hoogte[x, z])
        for y in range(h + 1, kit.WATER_Y + 1):
            if zacht[park.v[x, y, z]]:
                park.set(x, y, z, sub if y < kit.WATER_Y else (
                    "minecraft:dirt_path" if park.pad[x, z] else top))
        park.hoogte[x, z] = kit.WATER_Y
        if rng is not None and sneeuw and not park.pad[x, z] and park.v[x, kit.WATER_Y + 1, z] == 0 and \
                rng.random() < sneeuw:
            park.set(x, kit.WATER_Y + 1, z, "minecraft:snow[layers=1]")
    _reset_caches(park)
    return int(m.sum())


def dicht_water(park, zones, grond="minecraft:stone", oever=None, rondes=6):
    """Close every leak of still water in the given zones: a soft cell (air, plants, snow layer) next to or under
    still water gets glass when it belongs to a glass build (the tunnel), else ground. Returns the number filled."""
    zm = np.zeros_like(park.binnen)
    for zone in zones:
        zm |= park.zone == ZONES.index(zone)
    from scipy.ndimage import binary_dilation
    zm = binary_dilation(zm, iterations=2)
    oever = oever or grond
    totaal = 0
    for _ in range(rondes):
        bron = _bron_water_lut(park)
        zacht = _lut(park, _ZACHT, ook_lucht=True)
        glas = _lut(park, ("glass", "prismarine"))
        gevonden = []
        for y in range(kit.WATER_Y - 14, kit.WATER_Y + 24):
            lag = park.v[:, y, :]
            w = bron[lag] & zm
            if not w.any():
                continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nb = np.roll(np.roll(lag, -dx, 0), -dz, 1)
                for x, z in zip(*np.nonzero(w & zacht[nb])):
                    gevonden.append((int(x) + dx, y, int(z) + dz))
            onder = park.v[:, y - 1, :]
            for x, z in zip(*np.nonzero(w & zacht[onder])):
                gevonden.append((int(x), y - 1, int(z)))
        if not gevonden:
            break
        for (x, y, z) in set(gevonden):
            if not park.inb(x, y, z) or not zacht[park.v[x, y, z]]:
                continue
            kolom = park.v[x, max(y - 3, 0):y + 4, z]
            if glas[kolom].any():
                park.set(x, y, z, "minecraft:glass")
            else:
                park.set(x, y, z, oever if y >= kit.WATER_Y else grond)
                if y > park.hoogte[x, z] and not park.water[x, z]:
                    park.hoogte[x, z] = y
            totaal += 1
    return totaal


def _richting(k):
    return {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[k]


def _ruim_bomen(park, cx, cz, r):
    """Remove the trees (trunk + crown) whose trunk stands within r of (cx, cz)."""
    plant = _lut(park, ("_leaves", "_log", "vine", "cocoa", "jungle_wood", "mangrove_roots", "propagule"))
    blijf = []
    for (bx, by, bz, soort) in park.bomen:
        if math.hypot(bx - cx, bz - cz) <= r:
            rr = 8 if soort in ("reus", "jungle", "baobab") else 6
            a0, a1 = max(bx - rr, 0), min(bx + rr + 2, kit.W)
            b0, b1 = max(bz - rr, 0), min(bz + rr + 2, kit.W)
            sub = park.v[a0:a1, :, b0:b1]
            sub[plant[sub]] = 0
        else:
            blijf.append((bx, by, bz, soort))
    park.bomen[:] = blijf


def kijkhut(park, cx, cz, kijk, hout="spruce", verhoog=1, B=3, D=2, glas="guhs:eenrichtingsglas", kegel=26,
            bodem="minecraft:sand", oever_blok="minecraft:coarse_dirt", blad="oak", diep_water=True, log=print):
    """A real viewing hut (2B+1 wide, 2D+1 deep) looking along kijk = (kx, kz): a raised wooden floor, a front wall of
    one-way glass three high, glass in the front half of the side walls, a bench row, a slab roof with a leafy
    overhang, steps up to a door at the back. In a cone in front of it the view is cleared (trees, tall plants), mud
    on the banks becomes `oever_blok`, and the water gets deeper and clear (`bodem`).
    Returns (floor y, eye point, [points along the view axis at 8 / 14 / 20 blocks])."""
    kx, kz = kijk
    wx_, wz_ = -kz, kx                                      # 'right' when looking along kijk

    def W(u, w):
        return cx + u * kx + w * wx_, cz + u * kz + w * wz_

    voet = [W(u, w) for u in range(-D - 1, D + 2) for w in range(-B - 1, B + 2)]
    g = max(int(park.hoogte[x, z]) for x, z in voet)
    F = g + verhoog                                         # top of the floor
    _ruim_bomen(park, cx, cz, B + D + 4)
    for x, z in voet:                                       # clear the spot
        ruim_kolom(park, x, z, int(park.hoogte[x, z]) + 1, F + 9)
    planks = f"minecraft:{hout}_planks"
    for u in range(-D, D + 1):
        for w in range(-B, B + 1):
            x, z = W(u, w)
            for y in range(int(park.hoogte[x, z]) - 1, F + 1):
                park.set(x, y, z, planks if y >= F - 1 else "minecraft:cobblestone")
    # walls F+1 .. F+4
    log_s = f"minecraft:{hout}_log[axis=y]"
    for u in range(-D, D + 1):
        for w in range(-B, B + 1):
            x, z = W(u, w)
            hoek = abs(u) == D and abs(w) == B
            for y in range(F + 1, F + 5):
                if hoek:
                    s = log_s
                elif u == D:                                     # the front: glass three high
                    s = glas if y <= F + 3 else planks
                elif abs(w) == B:                                # sides: glass in the front half
                    s = glas if (u >= 0 and F + 2 <= y <= F + 3) else planks
                elif u == -D:                                    # the back
                    s = glas if (abs(w) == 2 and y == F + 2) else planks
                else:
                    s = "minecraft:air"
                park.set(x, y, z, s)
    # roof with overhang, leaves on top
    dak = f"minecraft:{hout}_slab[type=bottom,waterlogged=false]"
    rng = np.random.default_rng(cx * 1000 + cz)
    for u in range(-D - 1, D + 2):
        for w in range(-B - 1, B + 2):
            x, z = W(u, w)
            park.set(x, F + 5, z, dak)
            if abs(u) <= D and abs(w) <= B and rng.random() < 0.45:
                park.set(x, F + 6, z, park.leaves(blad))
    # door at the back + steps down
    x, z = W(-D, 0)
    naar = _richting((kx, kz))
    park.set(x, F + 1, z, f"minecraft:{hout}_door[facing={naar},half=lower,hinge=left,open=false,powered=false]")
    park.set(x, F + 2, z, f"minecraft:{hout}_door[facing={naar},half=upper,hinge=left,open=false,powered=false]")
    # (round 4) the steps go down until they meet the real ground behind the hut (the floor stands on the HIGHEST ground
    # of the footprint: on a slope the old fixed number of steps ended in the air, 2-3 blocks above the ground)
    for i in range(1, 12):
        sx, sz = W(-D - i, 0)
        y = F + 1 - i
        grond_h = int(park.hoogte[sx, sz])
        if grond_h >= y:
            break                                           # the ground itself is the next step
        for yy in range(grond_h, y):
            park.set(sx, yy, sz, planks)
        park.set(sx, y, sz, f"minecraft:{hout}_stairs[facing={naar},half=bottom,shape=straight,waterlogged=false]")
        ruim_kolom(park, sx, sz, y + 1, y + 4)
        park.hoogte[sx, sz] = y
        if y <= grond_h + 1:
            break
    # benches (backrest away from the glass), a lantern, a sign-free sill
    terug = _richting((-kx, -kz))
    for w in (-2, -1, 1, 2):
        bx, bz = W(0, w)
        kit.stoel(park, bx, F + 1, bz, (kx, kz))                 # guh chairs looking through the glass
    lx, lz = W(0, 0)
    park.set(lx, F + 4, lz, "minecraft:lantern[hanging=true,waterlogged=false]")
    # the view: clear the cone in front
    plant = _lut(park, _PLANT)
    modder = _lut(park, ("minecraft:mud", "muddy_mangrove_roots", "minecraft:packed_mud"))
    for t in range(D + 1, kegel + 1):
        hw = B + 1 + (t - D - 1) * 0.55
        for k in range(-int(hw), int(hw) + 1):
            x, z = cx + t * kx + k * wx_, cz + t * kz + k * wz_
            if not (0 <= x < kit.W and 0 <= z < kit.W) or not park.binnen[x, z]:
                continue
            if any(abs(bx - x) <= 1 and abs(bz - z) <= 1 for bx, _, bz, _ in park.bomen) and t <= kegel - 6:
                _ruim_bomen(park, x, z, 1.5)
            hoog = F + 10 if t <= 12 else F + 4
            for y in range(int(park.hoogte[x, z]) + 1, hoog):
                if plant[park.v[x, y, z]]:
                    park.v[x, y, z] = 0
            h = int(park.hoogte[x, z])
            if modder[park.v[x, h, z]]:
                park.set(x, h, z, bodem if park.water[x, z] else oever_blok)
            if park.water[x, z] and diep_water and not park.pad[x, z]:
                nb = min(h, kit.WATER_Y - 4)
                park.set(x, nb, z, bodem)
                for y in range(nb + 1, kit.WATER_Y + 1):
                    s = park.get(x, y, z)
                    if not any(b in s for b in ("glass", "fence", "planks", "_log", "slab", "stairs", "ice")):
                        park.set(x, y, z, "minecraft:water")
                park.hoogte[x, z] = nb
            elif not park.water[x, z] and t <= D + 4 and h > F - 1:
                for y in range(F, h + 1):                        # nothing higher than the floor right in front
                    park.v[x, y, z] = 0
                park.set(x, F - 1, z, oever_blok)
                park.hoogte[x, z] = F - 1
    _reset_caches(park)
    oog = (*W(D - 1, 0), F + 2)
    punten = [W(t, 0) for t in (8, 14, 20)]
    log(f"    kijkhut bij ({cx},{cz}) kijkt {naar}, vloer y{F}")
    return F, oog, punten


def bruggen(park, zones, top=None, hout="spruce", oever_aandeel=None, log=print):
    """Rebuild every route bridge whose water cells lie mostly in `zones` (or, with oever_aandeel, whose banks lie at
    least that share in `zones`): the deck at WATER_Y + 3 (a canoe fits under), ramps of half slabs up from the
    banks, railings on both sides, a few posts only near the banks."""
    from scipy.ndimage import binary_dilation, label, distance_transform_edt as edt
    top = top or kit.WATER_Y + 4                            # walking surface on the deck (deck block at top - 1)
    zis = [ZONES.index(z) for z in zones]
    M = park.pad & park.water
    lab, n = label(binary_dilation(M, iterations=3))
    planks = f"minecraft:{hout}_planks"
    slab_b = f"minecraft:{hout}_slab[type=bottom,waterlogged=false]"
    fence = f"minecraft:{hout}_fence"
    oud = _lut(park, (f"{hout}_planks", f"{hout}_fence", f"{hout}_log", f"{hout}_slab"))
    gedaan = 0
    for L in range(1, n + 1):
        regio = lab == L
        kruis = M & regio
        if not kruis.any():
            continue
        zn = park.zone[kruis]
        zn = zn[zn >= 0]
        if len(zn) == 0:
            continue
        mijn = np.bincount(zn).argmax() in zis
        if not mijn and oever_aandeel:
            oev = binary_dilation(kruis, iterations=6) & ~park.water & park.binnen
            zo = park.zone[oev]
            zo = zo[zo >= 0]
            mijn = len(zo) > 0 and np.isin(zo, zis).mean() >= oever_aandeel
        if not mijn:
            continue
        xs, zs = np.nonzero(regio)
        x0, x1 = max(xs.min() - 10, 1), min(xs.max() + 11, kit.W - 1)
        z0, z1 = max(zs.min() - 10, 1), min(zs.max() + 11, kit.W - 1)
        kr = kruis[x0:x1, z0:z1]
        dw = edt(~kr)
        # 1. the old deck, rails and posts out
        oude = binary_dilation(kr, iterations=3) & park.water[x0:x1, z0:z1]
        for x, z in zip(*np.nonzero(oude)):
            X, Z = x + x0, z + z0
            for y in range(int(park.hoogte[X, Z]) + 1, kit.WATER_Y + 5):
                if oud[park.v[X, y, Z]]:
                    park.set(X, y, Z, "minecraft:water" if y <= kit.WATER_Y else "minecraft:air")
        # 2. the deck and the ramps
        dek = {}
        padw = park.pad[x0:x1, z0:z1]
        for x, z in zip(*np.nonzero(padw & (dw <= 10))):
            X, Z = x + x0, z + z0
            s = top - 0.5 * float(np.floor(dw[x, z]))
            grond_s = int(park.hoogte[X, Z]) + 1 if not park.water[X, Z] else kit.WATER_Y + 1
            if s <= grond_s:
                continue
            dek[(X, Z)] = s
        for (X, Z), s in dek.items():
            if s == int(s):
                y_dek = int(s) - 1
                park.set(X, y_dek, Z, planks)
            else:
                y_dek = int(np.floor(s))
                park.set(X, y_dek, Z, slab_b)
            if not park.water[X, Z]:
                for y in range(int(park.hoogte[X, Z]) + 1, y_dek):
                    park.set(X, y, Z, planks)
            else:
                for y in range(kit.WATER_Y + 1, y_dek):
                    park.set(X, y, Z, "minecraft:air")
            for y in range(int(np.ceil(s)), int(np.ceil(s)) + 3):
                if "fence" not in park.get(X, y, Z):
                    park.set(X, y, Z, "minecraft:air")
        # posts near the banks only (the channel stays free for a canoe)
        land = ~park.water[x0:x1, z0:z1]
        dl = edt(~land)
        for (X, Z), s in dek.items():
            if park.water[X, Z] and dl[X - x0, Z - z0] <= 1.5 and (X + Z) % 3 == 0:
                for y in range(int(park.hoogte[X, Z]) + 1, int(np.ceil(s)) - 1):
                    park.set(X, y, Z, f"minecraft:{hout}_log[axis=y]")
        # 3. railings next to the deck
        rails = {}
        for (X, Z), s in dek.items():
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    q = (X + dx, Z + dz)
                    if q in dek or park.pad[q] or park.hek[q]:
                        continue
                    rails[q] = max(rails.get(q, 0), s)
        for (X, Z), s in rails.items():
            gs = int(park.hoogte[X, Z]) + 1 if not park.water[X, Z] else kit.WATER_Y + 1
            if s - gs < 1:
                continue
            if s == int(s):
                ys = int(s) - 1
                park.set(X, ys, Z, planks)
            else:
                ys = int(np.floor(s))
                park.set(X, ys, Z, slab_b)
            if not park.water[X, Z]:
                for y in range(gs, ys):
                    park.set(X, y, Z, planks)
            park.set(X, ys + 1, Z, fence)
        gedaan += 1
        log(f"    brug opnieuw bij x{xs.min()}-{xs.max()} z{zs.min()}-{zs.max()}: dek y{top - 1}, {len(dek)} vakken")
    _reset_caches(park)
    return gedaan


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
    for k in range(26):
        a = rng.random() * 2 * math.pi
        d = rng.random() * r
        x, z = int(cx + math.cos(a) * d), int(cz + math.sin(a) * d)
        if not mag_bouwen(park, x, z, 7):
            continue
        g = grond(park, x, z) if not park.water[x, z] else kit.WATER_Y - 2
        top = int(72 + rng.random() * 24)
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


KOEPEL_GLAS = "minecraft:pink_stained_glass"    # (round 4) the dome and its tube: pink glass, not plain glass


def onderwatertunnel(park, rng, sx, sz):
    """Round 3 (rebuilt): the underwater glass dome next to the wreck, reached from an entrance hall on the shore by a
    straight stair corridor (prismarine under the ground) and a level glass tube (only under water).
      - the dome floor sits at WATER_Y - 8 (one higher than before) and the sea bed round it is dug down to at least
        2 below the floor, so through the glass you look into open water, never at stone;
      - rings of sea lanterns on the sea bed (and seagrass), a lantern in the floor;
      - the kijk-guh's chair in the middle (all-round viewpoint) plus 3 chairs facing out.
    Returns (dome centre (x, floor y, z), points along the tube in the water)."""
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    bx, bz = wx + 11, wz + 12
    if not park.water[bx, bz]:
        for r in range(1, 12):
            hit = [(bx + dx, bz + dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1)
                   if park.water[bx + dx, bz + dz] and park.hoogte[bx + dx, bz + dz] < kit.WATER_Y - 6]
            if hit:
                bx, bz = hit[0]
                break
    VLOER = kit.WATER_Y - 8
    cy = VLOER + 3
    zi = ZONES.index(ZONE)
    # 1. the sea bed round the dome: only ever dug down (a gentle bowl), sand and gravel, seagrass
    for x in range(bx - 13, bx + 14):
        for z in range(bz - 13, bz + 14):
            if not park.water[x, z]:
                continue
            d = math.hypot(x - bx, z - bz)
            if d > 13:
                continue
            doel = VLOER - 2 + max(0, int((d - 8) * 0.7))
            h = int(park.hoogte[x, z])
            if h > doel:
                for y in range(doel + 1, h + 1):
                    park.set(x, y, z, "minecraft:water")
                park.set(x, doel, z, "minecraft:sand" if rng.random() < 0.7 else "minecraft:gravel")
                park.set(x, doel - 1, z, "minecraft:stone")
                park.hoogte[x, z] = doel
            if d > 6.5 and rng.random() < 0.12:
                park.set(x, int(park.hoogte[x, z]) + 1, z, "minecraft:seagrass")
    # 2. the dome (a little flattened so its top stays under the water line), floor, lantern, chairs
    for x in range(bx - 7, bx + 8):
        for z in range(bz - 7, bz + 8):
            for y in range(VLOER - 1, kit.WATER_Y):
                d = math.sqrt((x - bx) ** 2 + ((y - cy) * 1.35) ** 2 + (z - bz) ** 2)
                if d <= 5.6 and y >= VLOER:
                    park.set(x, y, z, KOEPEL_GLAS)
            if math.hypot(x - bx, z - bz) <= 5.6:
                for y in range(int(park.hoogte[x, z]), VLOER):          # a solid base under the dome
                    park.set(x, y, z, "minecraft:prismarine_bricks")
    for x in range(bx - 5, bx + 6):
        for z in range(bz - 5, bz + 6):
            for y in range(VLOER + 1, cy + 5):
                d = math.sqrt((x - bx) ** 2 + ((y - cy) * 1.35) ** 2 + (z - bz) ** 2)
                if d <= 4.5:
                    park.set(x, y, z, "minecraft:air")
            if math.hypot(x - bx, z - bz) <= 4.6:
                park.set(x, VLOER, z, "minecraft:dark_prismarine" if (x + z) % 2 else "minecraft:prismarine_bricks")
    park.set(bx, VLOER, bz, "minecraft:sea_lantern")
    # 3. sea lanterns in rings on the bed (lights under 8 blocks of water: no ice melts up at the pack ice)
    for (r, n) in ((8, 8), (11.5, 12)):
        for k in range(n):
            a = 2 * math.pi * k / n + (0.3 if r > 9 else 0)
            x, z = int(round(bx + math.cos(a) * r)), int(round(bz + math.sin(a) * r))
            if park.water[x, z]:
                park.lamp(x, int(park.hoogte[x, z]), z, "ijslantaarn")
    # 4. the entrance: the cardinal direction with the nearest land that has room for the hall
    beste = None
    for d4 in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for t in range(6, 70):
            x, z = bx + d4[0] * t, bz + d4[1] * t
            if not (0 <= x < kit.W and 0 <= z < kit.W) or not park.binnen[x, z]:
                break
            if not park.water[x, z] and park.zone[x, z] == zi:
                # the hall stands so far inland that the stairs end at the shore
                for extra in range(0, 30):
                    hx, hz = bx + d4[0] * (t + extra), bz + d4[1] * (t + extra)
                    if not (4 <= hx < kit.W - 4 and 4 <= hz < kit.W - 4):
                        break
                    g = max(int(park.hoogte[hx + a, hz + b]) for a in range(-3, 4) for b in range(-3, 4))
                    nodig = (g - VLOER) + 4
                    if extra >= nodig and all(mag_bouwen(park, hx + a, hz + b, 3) and not park.water[hx + a, hz + b]
                                              for a in range(-3, 4) for b in range(-3, 4)):
                        kand = (t + extra, d4, (hx, hz), g, t)
                        if beste is None or kand[0] < beste[0]:
                            beste = kand
                        break
                break
    langs = []
    if beste is None:
        kit.stoel(park, bx, VLOER + 1, bz, "north")
        return (bx, VLOER, bz), langs
    afstand, (ux, uz), (hx, hz), gH, t_land = beste
    trap_start = afstand                         # the stairs end in the middle of the hall (hall is 7 x 7)
    trap_lengte = gH - VLOER                    # one step up per block
    vlak_tot = trap_start - trap_lengte          # the level tube from the dome to the foot of the stairs
    naar_hal = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(ux, uz)]
    wx_, wz_ = -uz, ux
    # (round 4) level the hall plot FIRST: levelling it after the corridor filled the top steps (radius 4 > the hall's
    # 3) with 2 layers of stone, so the way down to the dome was shut
    egaliseer(park, hx, hz, 4, gH, top="minecraft:snow_block", rond=False)
    for s in range(4, trap_start + 1):
        vloer = VLOER if s <= vlak_tot else VLOER + (s - vlak_tot)
        cx_, cz_ = bx + ux * s, bz + uz * s
        onder_water = park.water[cx_, cz_] and vloer + 4 < kit.WATER_Y
        for w in range(-2, 3):
            x, z = cx_ + wx_ * w, cz_ + wz_ * w
            if abs(w) == 2:                       # side walls
                for y in range(vloer, vloer + 5):
                    park.set(x, y, z, KOEPEL_GLAS if onder_water and y > vloer else "minecraft:prismarine_bricks")
            else:
                park.set(x, vloer, z, "minecraft:prismarine_bricks")
                for y in range(vloer + 1, vloer + 4):
                    park.set(x, y, z, "minecraft:air")
                park.set(x, vloer + 4, z, KOEPEL_GLAS if onder_water else "minecraft:prismarine_bricks")
                if s > vlak_tot and w != 99:
                    park.set(x, vloer, z, f"minecraft:prismarine_brick_stairs[facing={naar_hal},half=bottom,shape=straight,waterlogged=false]")
                    park.set(x, vloer - 1, z, "minecraft:prismarine_bricks")
            # under the corridor down to the old ground / sea bed: solid (no hanging tube under the land)
            if not park.water[x, z]:
                for y in range(min(int(park.hoogte[x, z]), vloer - 1), vloer):
                    park.set(x, y, z, "minecraft:stone")
            # a corridor that comes out above the land gets the land built over it again (snow mound)
            if not park.water[x, z] and vloer + 5 > int(park.hoogte[x, z]):
                for y in range(vloer + 5, vloer + 6):
                    park.set(x, y, z, "minecraft:snow_block")
        if s % 4 == 0:                            # lights in the ceiling / floor
            if onder_water:
                park.set(cx_, vloer, cz_, "minecraft:sea_lantern")
            else:
                park.set(cx_, vloer + 4, cz_, "minecraft:sea_lantern")
        if onder_water:
            langs.append((cx_ + wx_ * 4, cz_ + wz_ * 4))
    # the dome's glass must not close the tube mouth
    for s in range(3, 7):
        for w in (-1, 0, 1):
            x, z = bx + ux * s + wx_ * w, bz + uz * s + wz_ * w
            for y in range(VLOER + 1, VLOER + 4):
                park.set(x, y, z, "minecraft:air")
            park.set(x, VLOER, z, "minecraft:prismarine_bricks")
    # 5. the entrance hall on the shore: 7 x 7, packed-ice walls with spruce corner posts, a door away from the sea
    for a in range(-3, 4):
        for b in range(-3, 4):
            x, z = hx + a, hz + b
            rand = abs(a) == 3 or abs(b) == 3
            hoek = abs(a) == 3 and abs(b) == 3
            for y in range(gH + 1, gH + 6):
                if hoek:
                    park.set(x, y, z, "minecraft:spruce_log[axis=y]")
                elif rand:
                    park.set(x, y, z, "minecraft:packed_ice" if y != gH + 3 else "minecraft:blue_ice")
                else:
                    park.set(x, y, z, "minecraft:air")
            park.set(x, gH, z, "minecraft:spruce_planks")
            park.set(x, gH + 6, z, "minecraft:spruce_planks" if not rand else "minecraft:spruce_slab[type=bottom,waterlogged=false]")
            park.set(x, gH + 7, z, "minecraft:snow_block" if not rand else "minecraft:air")
    # windows on both sides (glass, the view over the sea)
    for w in (-1, 0, 1):
        for zij in (-3, 3):
            x, z = hx + wx_ * zij + ux * w, hz + wz_ * zij + uz * w
            park.set(x, gH + 3, z, "minecraft:glass")
    # the stair well inside the hall: the last three steps come up through the hall floor (the hall code above laid
    # planks over them); railings on both sides and at the sea end, the walk-in side toward the door stays open
    for s_ in range(trap_start - 3, trap_start + 1):
        trede = VLOER + (s_ - vlak_tot)
        for w in (-1, 0, 1):
            x, z = bx + ux * s_ + wx_ * w, bz + uz * s_ + wz_ * w
            # (round 4) the step under the hall's wall row too, but only up to the floor: walking down, the head
            # passes under the hall floor there (it blocked the way down by one block)
            for y in range(trede + 1, (gH + 4) if s_ > trap_start - 3 else (gH + 1)):
                park.set(x, y, z, "minecraft:air")
            park.set(x, trede, z, f"minecraft:prismarine_brick_stairs[facing={naar_hal},half=bottom,shape=straight,waterlogged=false]")
        for w in (-2, 2):
            x, z = bx + ux * s_ + wx_ * w, bz + uz * s_ + wz_ * w
            if abs(x - hx) <= 2 and abs(z - hz) <= 2:
                park.set(x, gH + 1, z, "minecraft:spruce_fence")
    dx_, dz_ = hx + ux * 3, hz + uz * 3          # the door, away from the sea
    deur = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(ux, uz)]
    park.set(dx_, gH + 1, dz_, f"minecraft:spruce_door[facing={deur},half=lower,hinge=left,open=false,powered=false]")
    park.set(dx_, gH + 2, dz_, f"minecraft:spruce_door[facing={deur},half=upper,hinge=left,open=false,powered=false]")
    for y in (gH + 3,):
        park.set(dx_, y, dz_, "minecraft:spruce_planks")
    veilige_lamp(park, hx, gH + 5, hz, "lantaarn_hangend")
    veilige_lamp(park, dx_ + ux, gH + 3, dz_ + uz, "lantaarn")
    ex, ez = hx + wx_ * 2 + ux * 1, hz + wz_ * 2 + uz * 1
    park.set(ex, gH + 1, ez, "guhs:bestaand_knuffelguh", facing=deur)
    try:
        from .zone_savanne import bord, paadje
        rot = {"south": 0, "west": 4, "north": 8, "east": 12}[deur]
        bord(park, hx - wx_ * 2 + ux, gH + 1, hz - wz_ * 2 + uz, ["Onderwater-", "kijkkoepel", "", "trap naar beneden"],
             rotation=rot, blok="minecraft:spruce_sign")
        paadje(park, (dx_ + ux * 2, dz_ + uz * 2), max_r=30, blok="dirt_path")
    except Exception:
        pass
    _reset_caches(park)
    # 6. chairs: the kijk-guh in the middle looking at the wreck, three more facing out
    naar_wrak = _kardinaal(wx - bx, wz - bz)
    kit.stoel(park, bx, VLOER + 1, bz, naar_wrak)
    for d4 in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if d4 == (ux, uz) or d4 == naar_wrak:
            continue
        kit.stoel(park, bx + d4[0] * 3, VLOER + 1, bz + d4[1] * 3, d4)
    park._ijskust_dome = {"centrum": (bx, VLOER, bz), "hal": (hx, gH, hz), "richting": (ux, uz)}
    return (bx, VLOER, bz), langs


def pinguinglijbaan(park, rng, zee):
    """The ice slide (round 3: plus side steps the pinguins walk up, 1 per 2 blocks with snow slabs as half steps,
    and lanterns at the top). Returns (top (x, y, z), bottom (x, z), points dict for the pinguin AI)."""
    cx, cz, _, _ = kit.RESERVERINGEN["pinguinglijbaan"]
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
    px, pz = int(round(cx - dx * 4)), int(round(cz - dz * 4))
    g = grond(park, px, pz)
    hoog = g + 9
    for x in range(px - 2, px + 3):
        for z in range(pz - 2, pz + 3):
            for y in range(grond(park, x, z) - 1, hoog + 1):
                park.set(x, y, z, "minecraft:snow_block")
            park.hoogte[x, z] = hoog
    # railing round the platform (not on the slide side)
    for x in range(px - 2, px + 3):
        for z in range(pz - 2, pz + 3):
            if max(abs(x - px), abs(z - pz)) == 2:
                if (x - px) * dx + (z - pz) * dz > 1.2:
                    continue
                park.set(x, hoog + 1, z, "minecraft:spruce_fence")
    sx, sz = -dx, -dz
    for k in range(1, hoog - g + 1):           # stairs up on the back side (for the visitors)
        x, z = int(round(px + sx * (2 + k))), int(round(pz + sz * (2 + k)))
        y = hoog - k
        if not mag_bouwen(park, x, z, 4):
            break
        for w in (-1, 0, 1):
            xx, zz = int(round(x - sz * w)), int(round(z + sx * w))
            for yy in range(grond(park, xx, zz) - 1, y + 1):
                park.set(xx, yy, zz, "minecraft:snow_block")
            ruim_kolom(park, xx, zz, y + 1, y + 4)
    lengte = 2 * (hoog - kit.WATER_Y) + 4
    trap = []
    laatste = 0
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
        # the side steps (w = +3, +4): the same fall, with a snow slab on the lower cell of every level change
        if not water_hier:
            for w in (3, 4):
                xx, zz = int(round(x - dz * w)), int(round(z + dx * w))
                if not mag_bouwen(park, xx, zz, 3):
                    continue
                for yy in range(min(grond(park, xx, zz), y) - 1, y + 1):
                    park.set(xx, yy, zz, "minecraft:snow_block")
                ruim_kolom(park, xx, zz, y + 1, y + 4)
                if k % 2 == 0 and k > 0:
                    park.set(xx, y + 1, zz, "minecraft:snow_block" if False else
                             "minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]")
                park.hoogte[xx, zz] = y
            trap.append((int(round(x - dz * 3)), y + 1, int(round(z + dx * 3))))
            laatste = k
        if water_hier and y <= kit.WATER_Y:
            break
    # the outer edge of the steps: a low snow wall so pinguins do not tumble off
    for k in range(0, laatste + 1):
        y = hoog - k // 2
        x, z = int(round(px + dx * (3 + k))), int(round(pz + dz * (3 + k)))
        xx, zz = int(round(x - dz * 5)), int(round(z + dx * 5))
        if mag_bouwen(park, xx, zz, 3):
            for yy in range(min(grond(park, xx, zz), y) - 1, y + 2):
                park.set(xx, yy, zz, "minecraft:snow_block")
    # the top of the steps joins the platform
    for w in (3, 4):
        for k in (0, -1, -2):
            xx, zz = int(round(px + dx * (3 + k) - dz * w)), int(round(pz + dz * (3 + k) + dx * w))
            if mag_bouwen(park, xx, zz, 3):
                for yy in range(grond(park, xx, zz) - 1, hoog + 1):
                    park.set(xx, yy, zz, "minecraft:snow_block")
                ruim_kolom(park, xx, zz, hoog + 1, hoog + 4)
                park.hoogte[xx, zz] = hoog
    bodem = (int(round(px + dx * (3 + lengte - 2))), int(round(pz + dz * (3 + lengte - 2))))
    for (ox, oz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        if (ox * dx + oz * dz) < 1.2:
            veilige_lamp(park, px + ox, hoog + 2, pz + oz, "lantaarn")
    punten = {"top": (px, hoog + 1, pz), "bodem": (bodem[0], kit.WATER_Y + 1, bodem[1]),
              "trap": trap[::-1]}                 # from the bottom up
    park._ijskust_glijbaan = punten
    return (px, hoog + 1, pz), bodem


def iglo(park, cx, cz, deur):
    """A snow igloo with a lit, cosy inside (fur rug, plush, lantern on packed ice so nothing melts)."""
    g = grond(park, cx, cz)
    egaliseer(park, cx, cz, 4, g, top="minecraft:snow_block")
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            for y in range(g + 1, g + 5):
                d = math.sqrt((x - cx) ** 2 + ((y - g - 0.5) * 1.15) ** 2 + (z - cz) ** 2)
                if d <= 3.6:
                    park.set(x, y, z, "minecraft:snow_block" if d > 2.6 else "minecraft:air")
    dx, dz = deur
    for k in range(2, 5):
        x, z = cx + dx * k, cz + dz * k
        for y in (g + 1, g + 2):
            park.set(x, y, z, "minecraft:air")
        park.set(x, g + 3, z, "minecraft:snow_block")
        for w in (-1, 1):
            for y in (g + 1, g + 2):
                park.set(x - dz * w, y, z + dx * w, "minecraft:snow_block")
    for (a, b) in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
        park.set(cx + a, g, cz + b, "minecraft:packed_ice")
        park.set(cx + a, g + 1, cz + b, "minecraft:white_carpet" if (a, b) != (0, 0) else "minecraft:light_gray_carpet")
    park.set(cx - dx, g + 1, cz - dz, "guhs:pink_kussen", facing=_richting((dx, dz)))
    park.set(cx - dz, g + 1, cz + dx, "guhs:bestaand_knuffelguh", facing=_richting((dx, dz)))
    veilige_lamp(park, cx + dz, g + 1, cz - dx, "lantaarn")


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
    """The research station (round 3: bigger, warm and cosy inside): 9 x 7, white walls with big windows, a red roof
    edge, inside a desk with a map, guh chairs, a guh table with hot chocolate, a bunk, a bookcase, carpets, a plush,
    guh lamps; the red-white vuurtoren next to it and the hot chocolate kiosk with a striped awning."""
    cx, cz, _, _ = kit.RESERVERINGEN["poolstation"]
    c = zoek(park, ZONE, (cx, cz), r=7, max_helling=3)
    if c is None:
        c = zoek(park, ZONE, (cx, cz), r=6, max_helling=4)
    if c is None:
        return None
    cx, cz = c
    g = max(grond(park, x, z) for x in range(cx - 7, cx + 8) for z in range(cz - 7, cz + 8))
    egaliseer(park, cx, cz, 8, g, top="minecraft:snow_block", rond=False)
    x0, z0 = cx - 7, cz - 6
    B, D = 9, 7
    for x in range(x0, x0 + B):
        for z in range(z0, z0 + D):
            rand = x in (x0, x0 + B - 1) or z in (z0, z0 + D - 1)
            hoek = x in (x0, x0 + B - 1) and z in (z0, z0 + D - 1)
            park.set(x, g, z, "minecraft:spruce_planks")
            for y in range(g + 1, g + 5):
                park.set(x, y, z, ("minecraft:stripped_spruce_log[axis=y]" if hoek else "minecraft:white_concrete")
                         if rand else "minecraft:air")
            park.set(x, g + 5, z, "minecraft:red_concrete" if rand else "minecraft:light_gray_concrete")
            park.set(x, g + 6, z, "minecraft:snow[layers=2]" if not rand else "minecraft:air")
    # big windows (2 high) on all sides, a door to the south
    for x in range(x0 + 2, x0 + B - 2):
        if x == x0 + B // 2:
            continue
        for y in (g + 2, g + 3):
            park.set(x, y, z0, "minecraft:glass")
            park.set(x, y, z0 + D - 1, "minecraft:glass")
    for z in range(z0 + 2, z0 + D - 2):
        for y in (g + 2, g + 3):
            park.set(x0, y, z, "minecraft:glass")
            park.set(x0 + B - 1, y, z, "minecraft:glass")
    dxd = x0 + B // 2
    park.set(dxd, g + 1, z0 + D - 1, "minecraft:spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]")
    park.set(dxd, g + 2, z0 + D - 1, "minecraft:spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]")
    park.set(dxd, g + 3, z0 + D - 1, "minecraft:white_concrete")
    # inside: carpets, desk, chairs, table, bunk, bookcase, plush, lamps
    for x in range(x0 + 1, x0 + B - 1):
        for z in range(z0 + 1, z0 + D - 1):
            park.set(x, g + 1, z, "minecraft:light_blue_carpet" if (x + z) % 3 else "minecraft:white_carpet")
    park.set(x0 + 1, g + 1, z0 + 1, "minecraft:cartography_table")
    park.set(x0 + 2, g + 1, z0 + 1, "minecraft:lectern[facing=south,has_book=false,powered=false]")
    kit.stoel(park, x0 + 2, g + 1, z0 + 2, "north")
    park.set(x0 + B - 2, g + 1, z0 + 1, "minecraft:bookshelf")
    park.set(x0 + B - 2, g + 2, z0 + 1, "minecraft:bookshelf")
    park.set(x0 + B - 3, g + 1, z0 + 1, "guhs:guh_kast", facing="south", open="false")
    park.set(x0 + B - 2, g + 1, z0 + D - 2, "minecraft:white_bed[facing=north,occupied=false,part=foot]")
    park.set(x0 + B - 2, g + 1, z0 + D - 3, "minecraft:white_bed[facing=north,occupied=false,part=head]")
    tx, tz = x0 + 4, z0 + 3
    park.set(tx, g + 1, tz, "guhs:guh_tafel", facing="north")
    kit.stoel(park, tx - 1, g + 1, tz, "east")
    kit.stoel(park, tx + 1, g + 1, tz, "west")
    park.set(tx, g + 2, tz, "minecraft:decorated_pot[facing=north,cracked=false,waterlogged=false]")
    park.set(x0 + 1, g + 1, z0 + D - 2, "guhs:bestaand_knuffelguh", facing="east")
    park.set(x0 + 1, g + 1, z0 + D - 3, "minecraft:potted_azure_bluet")
    park.set(x0 + 1, g + 1, z0 + 3, "minecraft:furnace[facing=east,lit=true]")
    for (lx, lz) in ((x0 + 2, z0 + 3), (x0 + B - 3, z0 + 3)):
        veilige_lamp(park, lx, g + 4, lz, "lantaarn_hangend")
    veilige_lamp(park, x0 + B - 3, g + 1, z0 + D - 2, "guhlamp")
    park._ijskust_station = {"stoel": (x0 + 2, g + 1, z0 + 2), "oog": (x0 + 2.5, g + 2.6, z0 + 2.5)}
    # antenna
    for y in range(g + 6, g + 11):
        park.set(x0 + 1, y, z0 + 1, "minecraft:iron_bars")
    park.set(x0 + 1, g + 11, z0 + 1, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    # the vuurtoren: red and white rings, a lamp room with a balcony
    vx, vz = x0 + B + 2, z0 + 1
    if mag_bouwen(park, vx, vz, 3):
        egaliseer(park, vx, vz, 2, g, top="minecraft:snow_block", rond=False)
        for y in range(g + 1, g + 17):
            kleur = "minecraft:red_concrete" if ((y - g - 1) // 3) % 2 else "minecraft:white_concrete"
            for x in range(vx - 1, vx + 2):
                for z in range(vz - 1, vz + 2):
                    park.set(x, y, z, kleur)
        for x in range(vx - 2, vx + 3):
            for z in range(vz - 2, vz + 3):
                if max(abs(x - vx), abs(z - vz)) == 2:
                    park.set(x, g + 17, z, "minecraft:iron_bars")
                park.set(x, g + 16, z, "minecraft:white_concrete") if max(abs(x - vx), abs(z - vz)) == 2 else None
        for x in range(vx - 1, vx + 2):
            for z in range(vz - 1, vz + 2):
                park.set(x, g + 17, z, "minecraft:glass" if (x, z) != (vx, vz) else "minecraft:sea_lantern")
                park.set(x, g + 18, z, "minecraft:glass")
                park.set(x, g + 19, z, "minecraft:red_concrete")
        park.set(vx, g + 18, vz, "minecraft:sea_lantern")
        park.set(vx, g + 20, vz, "minecraft:red_concrete")
    # the hot chocolate kiosk in front of the door
    kx, kz = cx - 3, z0 + D + 3
    if mag_bouwen(park, kx, kz, 3):
        egaliseer(park, kx, kz, 3, g, top="minecraft:snow_block", rond=False)
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
        park.set(kx - 1, g + 3, kz - 1, "minecraft:lantern[hanging=true,waterlogged=false]")
        for k in (-1, 1):                         # two log stools (not viewpoint chairs)
            park.set(kx + k * 2, g + 1, kz + 2, "minecraft:stripped_spruce_log[axis=y]")
    return cx, g + 1, cz


def kust_pad(park, log=print):
    """Test round 2: the route ran along the east shore half in the sea, on a plank 'bridge' at water level that did
    not work. Turn that strip of sea into real shore (snow on stone), with the path on it."""
    from scipy.ndimage import binary_dilation
    zi = ZONES.index(ZONE)
    kruis = park.pad & park.water & (park.zone == zi)
    if not kruis.any():
        return 0
    strook = binary_dilation(kruis, iterations=3) & park.water & (park.zone == zi)
    weg = _lut(park, ("spruce_planks", "spruce_fence", "spruce_log", "lily_pad"))
    for x, z in zip(*np.nonzero(strook)):
        for y in range(kit.WATER_Y + 1, kit.WATER_Y + 5):
            if weg[park.v[x, y, z]]:
                park.v[x, y, z] = LUCHT
        for y in range(int(park.hoogte[x, z]) + 1, kit.WATER_Y):
            s = park.get(x, y, z)
            if s.startswith("minecraft:water") or weg[park.v[x, y, z]] or park.v[x, y, z] == 0:
                park.set(x, y, z, "minecraft:stone")
        park.set(x, kit.WATER_Y, z, "minecraft:dirt_path" if park.pad[x, z] else "minecraft:snow_block")
        park.water[x, z] = False
        park.hoogte[x, z] = kit.WATER_Y
    _reset_caches(park)
    log(f"    ijskust: {int(strook.sum())} kolommen zee langs het pad -> oever")
    return int(strook.sum())


def veilige_lamp(park, x, y, z, soort="lantaarn"):
    """Round 3: a light that never melts ice or snow layers: plain ice within 4 becomes packed ice, snow layers within
    3 go (block light > 11 melts both in the game). Registered through park.lamp."""
    for dx in range(-4, 5):
        for dy in range(-4, 5):
            for dz in range(-4, 5):
                p = (x + dx, y + dy, z + dz)
                if not park.inb(*p) or park.v[p] == 0:
                    continue
                s = park.get(*p)
                if s == "minecraft:ice":
                    park.set(*p, "minecraft:packed_ice")
                elif s.startswith("minecraft:snow[") and max(abs(dx), abs(dy), abs(dz)) <= 3:
                    park.v[p] = LUCHT
    park.lamp(x, y, z, soort)


def _kardinaal(dx, dz):
    return (1 if dx > 0 else -1, 0) if abs(dx) >= abs(dz) else (0, 1 if dz > 0 else -1)


def oostrand(park, zones=("ijskust", "oertijd"), log=print):
    """Round 3: just outside the outer fence of these zones, every decor column that lies LOWER than the park ground
    behind the fence (or has a hole under it) is built up to at least that ground (+ a ragged ice/snow cap rising a
    little away from the fence): no looking down into the void and nothing to fall into from anywhere along the
    ijskust and the oertijd. Decor that is already higher stays as it is."""
    from scipy.ndimage import distance_transform_edt as edt
    zis = [ZONES.index(z) for z in zones]
    mijn = np.isin(park.zone, zis) & park.binnen
    if not mijn.any():
        return 0
    d, (ix, iz) = edt(~mijn, return_indices=True)
    band = (~park.binnen) & (d <= 12)
    rng = np.random.default_rng(kit.SEED + 777)
    n = 0
    xs, zs = np.nonzero(band)
    for x, z in zip(xs, zs):
        bx, bz = int(ix[x, z]), int(iz[x, z])
        binnen_h = int(park.hoogte[bx, bz])
        nodig = binnen_h + 1 + int(d[x, z] * 0.35) + int(rng.integers(0, 2))
        col = np.nonzero(park.v[x, :, z])[0]
        top = int(col.max()) if len(col) else -1
        laag = int(col.min()) if len(col) else 10 ** 6
        if top >= binnen_h - 1 and laag <= binnen_h - 8:
            continue
        for y in range(max(0, binnen_h - 12), max(top, nodig) + 1):
            if park.v[x, y, z] == 0 and y <= nodig:
                park.set(x, y, z, "minecraft:stone" if y < nodig - 2 else (
                    "minecraft:packed_ice" if rng.random() < 0.4 else "minecraft:snow_block"))
        if top < nodig:
            park.set(x, nodig, z, "minecraft:snow_block")
        n += 1
    log(f"    ijskust/oertijd: {n} decorkolommen achter het hek bijgewerkt (geen doorkijk, geen gaten)")
    return n


def pad_lampen(park, zone, stap=22, soort="lantaarn", log=print):
    """Round 3 lighting: a lantern post (fence + lantern) beside the route path every ~stap blocks in the zone, on the
    land next to the path, never near a fence (no ledge) and never near plain ice (veilige_lamp)."""
    zi = ZONES.index(zone)
    gekozen = []
    xs, zs = np.nonzero(park.pad & (park.zone == zi))
    volgorde = np.lexsort((zs, xs))
    for i in volgorde:
        x, z = int(xs[i]), int(zs[i])
        if any(abs(x - a) + abs(z - b) < stap for a, b in gekozen):
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2)):
            px, pz = x + dx, z + dz
            if not (0 <= px < kit.W and 0 <= pz < kit.W) or park.pad[px, pz] or park.water[px, pz]:
                continue
            if not park.binnen[px, pz] or park.hek_afstand[px, pz] <= 3 or park.zone[px, pz] != zi:
                continue
            g = int(park.hoogte[px, pz])
            if park.v[px, g + 1, pz] != 0 and not park.get(px, g + 1, pz).startswith(
                    ("minecraft:snow[", "minecraft:short_grass", "minecraft:dead_bush")):
                continue
            ruim_kolom(park, px, pz, g + 1, g + 3)
            park.set(px, g + 1, pz, "minecraft:spruce_fence")
            veilige_lamp(park, px, g + 2, pz, soort)
            gekozen.append((x, z))
            break
    log(f"    {zone}: {len(gekozen)} lantaarnpaaltjes langs het pad")
    return gekozen


def registreer_kijkplekken(park, log=print):
    """Round 3: the viewpoints of the ijskust with their favourite spots (kijkplekken.json, read by the animal AI and
    the kijk-guhs). The glijbaan is a viewpoint without a chair: its favourites are the slide points for the pinguins
    (glijbaan_top, glijbaan_bodem, glijbaan_trap_0.. from the bottom up), see SAFARI_CONTRACT.md."""
    info = park._ijskust
    dome = getattr(park, "_ijskust_dome", None)
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    if dome:
        bx, vy, bz = dome["centrum"]
        k = park.kijkplek("onderwaterkoepel_ijskust", ZONE, (bx + 0.5, vy + 2.6, bz + 0.5), None, (bx, vy + 1, bz))
        # in the water round the dome: pinguins diving, the octopus on the bed, seals passing
        for naam, (ox, oz), soorten in (("pinguins_duik", (-5, -5), ["pinguin"]), ("octopus_bodem", (7, 2), ["octopus"]),
                                        ("zeehonden_zwem", (2, 8), ["zeehond"]), ("narwal_diep", (-9, 6), ["narwal"])):
            x, z = bx + ox, bz + oz
            if park.water[x, z]:
                park.favoriet(f"koepel_{naam}", k, (x, int(park.hoogte[x, z]) + 2, z), soorten, straal=3)
    hut = info.get("kijkhut")
    if hut:
        F, oog, punten = hut
        stoelen = [s for s in park.stoelen if abs(s[0] - oog[0]) <= 4 and abs(s[2] - oog[1]) <= 4 and s[1] == F + 1]
        kijk = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[stoelen[0][3]] if stoelen else None
        k = park.kijkplek("kijkhut_ijskust_west", ZONE, (oog[0] + 0.5, F + 2.6, oog[1] + 0.5), kijk,
                          stoelen[0][:3] if stoelen else None)
        for (x, y, z) in info.get("zeehonden", [])[:3]:
            park.favoriet(f"kijkhut_ijskust_rots_{x}_{z}", k, (x, y, z), ["zeehond"], straal=2)
        for (x, z) in sorted(info.get("ijsveld") or [], key=lambda p: abs(p[0] - oog[0]))[:2]:
            park.favoriet(f"kijkhut_ijskust_pakijs_{x}_{z}", k, (x, kit.WATER_Y + 1, z), ["pinguin", "zeehond"], straal=3)
    gl = getattr(park, "_ijskust_glijbaan", None)
    if gl:
        tx, ty, tz = gl["top"]
        ox, oy, oz = gl["bodem"]
        k = park.kijkplek("glijbaan_ijskust", ZONE, (ox + 0.5, oy + 1.6, oz + 0.5), (tx - ox, tz - oz), None)
        park.favoriet("glijbaan_top", k, gl["top"], ["pinguin"], straal=2)
        park.favoriet("glijbaan_bodem", k, gl["bodem"], ["pinguin"], straal=2)
        for i, p in enumerate(gl["trap"]):
            park.favoriet(f"glijbaan_trap_{i}", k, p, ["pinguin"], straal=1)
    st = getattr(park, "_ijskust_station", None)
    if st:
        park.kijkplek("poolstation_ijskust", ZONE, st["oog"], (0, -1), st["stoel"])
    # the tower and the station: a favourite on the nearest shore (a pinguin family is put there in plekken())
    zi = ZONES.index(ZONE)
    park._ijskust_oever = []
    for k in park.kijkplekken:
        if k["zone"] != ZONE or not (k["naam"].startswith("toren_") or k["naam"] == "poolstation_ijskust"):
            continue
        ox, oz = int(k["oog"][0]), int(k["oog"][2])
        beste = None
        for wil_water in (True, False):          # the shore if there is one in view, else open snow nearby
            for r in range(8, 30):
                for a in range(0, 360, 12):
                    x = int(round(ox + math.cos(math.radians(a)) * r))
                    z = int(round(oz + math.sin(math.radians(a)) * r))
                    if not (2 <= x < kit.W - 2 and 2 <= z < kit.W - 2) or park.zone[x, z] != zi or park.water[x, z]:
                        continue
                    if not mag_bouwen(park, x, z, 5) or _pad_afstand(park)[x, z] < 3:
                        continue
                    if wil_water and not any(park.water[x + dx, z + dz] for dx in (-2, 0, 2) for dz in (-2, 0, 2)):
                        continue
                    beste = (x, z)
                    break
                if beste:
                    break
            if beste:
                break
        if beste:
            x, z = beste
            park.favoriet(f"{k['naam']}_oever", k, (x, kolom_top_vrij(park, x, z), z), ["pinguin", "zeehond"], straal=3)
            park._ijskust_oever.append(beste)
    log(f"    ijskust: kijkplekken koepel={bool(dome)} kijkhut={bool(hut)} glijbaan={bool(gl)}"
        f" oever={len(park._ijskust_oever)}")


# ======================================================================================================================
def bouw(park, log=print):
    rng = np.random.default_rng(kit.SEED + 501)
    park._ijskust = {}
    # (round 3: the river pipeline (terrein.py + kit.brug + controle.py) owns water and bridges: no raising of dry columns here any more)
    # (round 3: the coast path is laid by terrein.route; kust_pad tore the bridge down)
    zee, sx, sz = _zee(park)
    ijsbergkliffen(park, rng)
    park._ijskust["zeehonden"] = zeehondenrotsen(park, rng, zee, sx, sz)
    park._ijskust["ijsberg"] = springpad(park, rng, zee, sx, sz)
    scheepswrak(park, rng, sx, sz)
    park._ijskust["tunnel"] = onderwatertunnel(park, rng, sx, sz)
    park._ijskust["glijbaan"] = pinguinglijbaan(park, rng, zee)
    park._ijskust["iglo"] = iglodorpje(park, rng)
    park._ijskust["station"] = poolstation(park, rng)
    # uitkijktoren on the north-east shore; on the west shore a real viewing hut (one-way glass, raised floor) looking
    # east over the seal rocks to the wreck in its pack ice (test round 2: the old hide had one slit and saw nothing)
    t = zoek(park, ZONE, (sx + 70, sz - 22), r=3, max_helling=4)
    if t:
        top = toren(park, *t, hout="birch", dak="minecraft:birch_slab[type=bottom,waterlogged=false]")
        veilige_lamp(park, t[0] + 3, grond(park, t[0] + 3, t[1]) + 1, t[1], "guhlamp")
    h = zoek(park, ZONE, (sx - 46, sz - 10), r=5, max_helling=3, hek_marge=7)
    if h:
        kijk = (1, 0) if sx > h[0] else (-1, 0)
        park._ijskust["kijkhut"] = kijkhut(park, *h, kijk=kijk, hout="spruce", verhoog=2, kegel=30,
                                            bodem="minecraft:gravel", oever_blok="minecraft:snow_block",
                                            blad="spruce", diep_water=False, log=log)
        lx, lz = h[0] - kijk[0] * 5, h[1] + 2
        if mag_bouwen(park, lx, lz, 3) and not park.water[lx, lz]:
            veilige_lamp(park, lx, grond(park, lx, lz) + 1, lz, "guhlamp")
    park._ijskust["ijsveld"] = _ijsveld(park, zee)
    pad_lampen(park, ZONE, stap=24, soort="lantaarn", log=log)
    registreer_kijkplekken(park, log)
    log(f"    ijskust: zee {sx},{sz} tunnel {park._ijskust['tunnel'][0]} glijbaan {park._ijskust['glijbaan']}"
        f" toren {t} kijkhut {h}")


def _ijsveld(park, zee):
    """The pack ice cells round the wreck that have air above them (where pinguins and seals can sit)."""
    wx, wz, _, _ = kit.RESERVERINGEN["scheepswrak"]
    uit = []
    for x in range(wx - 14, wx + 15):
        for z in range(wz - 12, wz + 13):
            s = park.get(x, kit.WATER_Y, z)
            if ("packed_ice" in s or s == "minecraft:ice") and park.v[x, kit.WATER_Y + 2, z] == 0 \
                    and abs(z - wz) > 4:
                uit.append((x, z))
    return uit


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
    # (round 4) more life round the pink dome: a diving pinguin group, seals and a second octopus in the water just
    # outside the glass (homes in the water within 8..12 of the dome, on different sides), so the viewpoint is busy
    if dome:
        bx_, vy_, bz_ = dome
        for soort, hoek, r, n_v, n_j in (("pinguin", 225, 9, 3, 2), ("zeehond", 90, 10, 2, 1), ("octopus", 330, 8, 1, 0),
                                         ("pinguin", 30, 11, 2, 1)):
            for dr in (0, 1, 2, -1, 3):
                x = int(round(bx_ + math.cos(math.radians(hoek)) * (r + dr)))
                z = int(round(bz_ + math.sin(math.radians(hoek)) * (r + dr)))
                if zee[x, z] and park.hoogte[x, z] < kit.WATER_Y - 3:
                    extra_plek(plekken, soort, ZONE, x, int(park.hoogte[x, z]) + 1, z, 4, n_v, n_j, water=True)
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
    # test round 2: more life where the viewing hut looks, round the wreck and along the coast
    for (x, y, z) in info["zeehonden"][1:]:                 # the other two seal rocks
        extra_plek(plekken, "zeehond", ZONE, x, y, z, 6, 2, 1, water=True)
    ijs = info.get("ijsveld") or []
    if ijs:
        hut = info.get("kijkhut")
        hx = hut[1][0] if hut else wx - 30
        ijs.sort(key=lambda p: abs(p[0] - hx))               # the side of the pack ice facing the hut first
        x, z = ijs[0]
        extra_plek(plekken, "pinguin", ZONE, x, kit.WATER_Y + 1, z, 6, 2, 2, water=True)
        x, z = ijs[len(ijs) // 2]
        extra_plek(plekken, "zeehond", ZONE, x, kit.WATER_Y + 1, z, 7, 2, 1, water=True)
    # round 3: a pinguin family on the shore in view of the tower and of the station
    for (x, z) in getattr(park, "_ijskust_oever", []):
        extra_plek(plekken, "pinguin", ZONE, x, kolom_top_vrij(park, x, z), z, 6, 2, 2, water=True)
    # two more pinguin families: one on the north shore, one on the south shore (next to the water)
    wd = _water_afstand(park)
    zi = ZONES.index(ZONE)
    for doel in ((sx, sz - 48), (sx - 10, sz + 44)):
        beste = None
        for r in range(0, 40):
            for a in range(0, 360, 15):
                x = int(round(doel[0] + math.cos(math.radians(a)) * r))
                z = int(round(doel[1] + math.sin(math.radians(a)) * r))
                if (0 <= x < kit.W and 0 <= z < kit.W and park.zone[x, z] == zi and not park.water[x, z]
                        and 1.5 <= wd[x, z] <= 4 and mag_bouwen(park, x, z, 5)):
                    beste = (x, z)
                    break
            if beste:
                break
        if beste:
            extra_plek(plekken, "pinguin", ZONE, beste[0], kolom_top_vrij(park, *beste), beste[1], 9, 2, 2, water=True)
