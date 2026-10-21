"""
Op Safari! step 2, slice S3 (vervoer & momenten): what the rides and the moments need in the park, built after every
zone module (NA_ZONES), plus the ride data the game reads (written by route_data.schrijf into assets/guhs/safari/, so the
server AND every client have the same paths):

  * the truck route: the route line (terrein.route) at standing height, a halte per zone (a sign, a bench, a lantern);
  * lantern posts along the paths (they light the park at night);
  * the canoe: a jetty at the village pond and a path up the river, back before the first low spot (the beaver dam);
  * the kabelbaan IJskust -> Bamboebergen: two stations (deck, ladder, roof), pylons and an iron-bar cable;
  * the struisvogelrace track on the savanna (an oval, a start arch, the sign of the scoreboard);
  * the schemabord at the village square (block guhs:safari_schemabord);
  * atmosphere spots (sfeer.json): the steam of the warme bronnen, the village fountain, the moment spots per zone.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES, RIVIER, MEREN, ROUTE, P
from .zone_savanne import zoek, vlak, onderbouw, bord, pad_afstand

W, H, WATER_Y = kit.W, kit.H, kit.WATER_Y
NA_ZONES = True       # safari_park.zone_modules runs this module after all zone modules

ZONE_NAAM = {"entree": "Entree", "savanne": "Savanne", "delta": "Rivierdelta", "regenwoud": "Regenwoud",
             "bamboe": "Bamboebergen", "ijskust": "IJskust", "oertijd": "Oerijstijd", "europa": "Europees Woud",
             "australie": "Australie"}

_VLOER = ("planks", "slab", "stairs", "path", "dirt", "sand", "gravel", "stone", "grass_block", "podzol", "mud", "ice",
          "terracotta", "brick", "block", "cobble", "andesite", "granite", "diorite", "clay", "calcite", "tuff",
          "deepslate", "concrete", "wool", "mosaic", "snow_block", "mycelium", "roots", "_wood", "prismarine")
_NIET = ("button", "_wall", "pressure", "fence", "gate", "_log", "lantern", "torch", "sign", "banner", "leaves",
         "carpet", "rail", "chain", "candle", "ladder", "bars", "pane", "glass", "trapdoor", "door", "chest")


def _vloer(s):
    n = s.split("[")[0]
    if any(w in n for w in _NIET):
        return False
    return any(w in n for w in _VLOER)


def staan(park, x, z):
    """Standing height on top of the walkable surface of column x,z (bridge decks, paths, slabs half a block)."""
    x, z = int(min(max(x, 0), W - 1)), int(min(max(z, 0), W - 1))
    h = int(park.hoogte[x, z])
    for y in range(min(H - 2, max(h, WATER_Y) + 9), max(h - 4, 1), -1):
        i = park.v[x, y, z]
        if not i:
            continue
        s = park.pal[i]
        if _vloer(s):
            return y + (0.5 if "_slab" in s and "type=bottom" in s else 1.0)
    return h + 1.0


def _herbemonster(punten, stap):
    """A polyline resampled every `stap` blocks (x, z)."""
    uit = [punten[0]]
    rest = 0.0
    for (ax, az), (bx, bz) in zip(punten, punten[1:]):
        d = math.hypot(bx - ax, bz - az)
        t = stap - rest
        while t <= d:
            uit.append((ax + (bx - ax) * t / d, az + (bz - az) * t / d))
            t += stap
        rest = d - (t - stap)
    return uit


def _glad(ys, r=2):
    ys = np.array(ys, float)
    n = len(ys)
    med = np.array([np.median(ys[max(0, i - r):i + r + 1]) for i in range(n)])
    return [float(np.mean(med[max(0, i - 1):i + 2])) for i in range(n)]


def _zone_op(park, x, z):
    zi = int(park.zone[int(x), int(z)])
    return ZONES[zi] if zi >= 0 else None


def _rotatie_naar(dx, dz):
    """Sign rotation (0..15, 0 = text faces south) so that its front faces direction (dx, dz)."""
    hoek = math.degrees(math.atan2(-dx, dz))   # 0 = south
    return int(round(((hoek % 360) / 22.5))) % 16


def _vrije_grond(park, x, z, pd, min_pad=2.0):
    x, z = int(x), int(z)
    if not (2 <= x < W - 2 and 2 <= z < W - 2) or not park.binnen[x, z] or park.water[x, z] or park.hek[x, z]:
        return False
    if pd[x, z] < min_pad or park.hek_afstand[x, z] < 3:
        return False
    g = int(park.hoogte[x, z])
    return all(park.v[x, y, z] == 0 for y in range(g + 1, g + 4))


# ======================================================================================================================
# the truck route + haltes + lanterns
TRUCK_HALF = 1.6        # round 3: half the width of the (longer, 3-row) truck: the road is cleared this wide
TRUCK_HOOG = 5          # free blocks above the road: the truck, the seated passengers' heads, a little air
_HOUD = ("guhs:safaripoort",)


def _rijbaan(park, lijn, log):
    """Round 3 (the truck 'drove through walls and you took damage'): one level per route point (the smoothed standing
    height, never a dip under the ground), and a corridor TRUCK_HALF wide on both sides that is made free: nothing in
    the TRUCK_HOOG blocks above the road (trees, fences, posts, a terrace corner), bumps cut down to the road, dips filled
    (never over water: a bridge deck is kept as it is). Returns the integer road levels (standing y) per point."""
    ruw = [staan(park, x, z) for x, z in lijn]
    zacht = _glad(ruw, r=3)
    # never below the ground under the middle: a dip of the smoothing would put the wheels inside the road
    niveaus = [int(round(max(z_, r - 0.25))) for z_, r in zip(zacht, ruw)]
    # nearest route point per corridor cell
    toewijzing = {}
    for k, (x, z) in enumerate(lijn):
        r = TRUCK_HALF + 0.5
        for cx in range(int(math.floor(x - r)), int(math.ceil(x + r)) + 1):
            for cz in range(int(math.floor(z - r)), int(math.ceil(z + r)) + 1):
                if not (0 <= cx < W and 0 <= cz < W):
                    continue
                d = math.hypot(cx + 0.5 - x, cz + 0.5 - z)
                if d <= r and ((cx, cz) not in toewijzing or d < toewijzing[(cx, cz)][1]):
                    toewijzing[(cx, cz)] = (k, d)
    weg = gevuld = bulten = 0
    pad_top = park.id("minecraft:dirt_path")
    for (cx, cz), (k, d) in toewijzing.items():
        y = niveaus[k]
        for yy in range(y, min(H, y + TRUCK_HOOG)):
            i = park.v[cx, yy, cz]
            if i and not park.pal[i].startswith(_HOUD):
                s = park.pal[i]
                if s.startswith("minecraft:water"):
                    continue
                park.v[cx, yy, cz] = 0
                if _vloer(s) and yy == y:
                    bulten += 1
                else:
                    weg += 1
        onder = park.pal[park.v[cx, y - 1, cz]]
        if park.water[cx, cz] or onder.startswith("minecraft:water"):
            continue                                            # a bridge (deck kept) or open water: left as it is
        if park.v[cx, y - 1, cz] == 0 or not _vloer(onder):
            yy = y - 1
            while yy > 0 and (park.v[cx, yy, cz] == 0 or not _vloer(park.pal[park.v[cx, yy, cz]])) and y - 1 - yy < 8:
                park.v[cx, yy, cz] = park.id("minecraft:dirt") if yy < y - 1 else pad_top
                yy -= 1
            gevuld += 1
        if int(park.hoogte[cx, cz]) < y - 1 or int(park.hoogte[cx, cz]) >= y:
            park.hoogte[cx, cz] = y - 1
        park.pad[cx, cz] = True
    log(f"    s3: rijbaan vrijgemaakt ({len(toewijzing)} vakken): {weg} blokken weg, {bulten} bulten, {gevuld} kuilen")
    return niveaus


_ZACHT = ("air", "grass", "fern", "flower", "poppy", "dandelion", "tulip", "daisy", "carpet", "lily", "bush", "sapling",
          "petals", "rose", "orchid", "allium", "bluet", "cornflower", "lilac", "peony", "dead_bush", "vine", "leaf_litter",
          "snow[", "path")


def _dorp_masker(park):
    """The cells of the entreedorp (zone_entree's INS) in world coords, or None."""
    try:
        from . import zone_entree as ze
    except ImportError:
        return None
    m = np.zeros((W, W), bool)
    for (x, z) in ze.INS:
        wx, wz = ze.OX + x, ze.OZ + z
        if 0 <= wx < W and 0 <= wz < W:
            m[wx, wz] = True
    return m


def _dorpsweg(park, lijn, log):
    """Round 3: the park route ran straight through Het Watergat in the village (its loop point lay inside the
    building: 'the truck drives through the wall'). Inside the village the road is now searched around the buildings
    (shortest way over free ground, paths preferred, kept TRUCK_HALF + 1 from anything solid), from where the route comes
    in to where it goes out. Returns the new polyline (1.5 apart) and the index range of the village road in it."""
    import heapq
    from scipy.ndimage import binary_dilation
    dorp = _dorp_masker(park)
    if dorp is None:
        return lijn, None
    binnen = [bool(dorp[int(x), int(z)]) for x, z in lijn]
    if not any(binnen) or all(binnen):
        return lijn, None
    # rotate so the village part is one run in the middle
    n = len(lijn)
    start = next(i for i in range(n) if binnen[i] and not binnen[i - 1])
    lijn = lijn[start:] + lijn[:start]
    binnen = binnen[start:] + binnen[:start]
    eind = next(i for i in range(n) if not binnen[i])           # first point after the village part
    vóór = lijn[eind:]                                           # outside, from the exit around the park to the entry
    A = vóór[-1]                                                 # last point outside before entering
    B = vóór[0]                                                  # first point outside after leaving
    # obstacles: anything solid (not a plant/path) 1..4 above the ground, plus fences; grown by the truck's half width
    x0, x1 = int(min(A[0], B[0], *np.nonzero(dorp)[0])) - 4, int(max(A[0], B[0], *np.nonzero(dorp)[0])) + 5
    z0, z1 = int(min(A[1], B[1], *np.nonzero(dorp)[1])) - 4, int(max(A[1], B[1], *np.nonzero(dorp)[1])) + 5
    x0, z0, x1, z1 = max(x0, 1), max(z0, 1), min(x1, W - 1), min(z1, W - 1)
    bl = np.zeros((x1 - x0, z1 - z0), bool)
    for x in range(x0, x1):
        for z in range(z0, z1):
            if park.water[x, z]:
                bl[x - x0, z - z0] = True
                continue
            g = int(park.hoogte[x, z])
            for y in range(g + 1, g + 5):
                i = park.v[x, y, z]
                if i and not any(w in park.pal[i] for w in _ZACHT):
                    bl[x - x0, z - z0] = True
                    break
    groei = binary_dilation(bl, iterations=int(math.ceil(TRUCK_HALF)))
    # the gateways where the route crosses the village fence are free by definition (their posts stand beside the road)
    for (gx_, gz_) in (A, B):
        for x in range(int(gx_) - 5, int(gx_) + 6):
            for z in range(int(gz_) - 5, int(gz_) + 6):
                if x0 <= x < x1 and z0 <= z < z1 and math.hypot(x - gx_, z - gz_) <= 5 and not park.water[x, z]:
                    groei[x - x0, z - z0] = False
    # the search may leave the village a little, but only over ground we would not mind as a road
    def kost(x, z):
        if groei[x - x0, z - z0]:
            return None
        c = 1.0 if park.pad[x, z] else 1.8
        if not dorp[x, z]:
            c += 0.6
        return c
    sa = (int(A[0]), int(A[1]))
    sb = (int(B[0]), int(B[1]))
    dist = {sa: 0.0}
    vorig = {}
    hoop = [(0.0, sa)]
    while hoop:
        d, (x, z) = heapq.heappop(hoop)
        if (x, z) == sb:
            break
        if d > dist.get((x, z), 1e9):
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
            nx_, nz_ = x + dx, z + dz
            if not (x0 <= nx_ < x1 and z0 <= nz_ < z1):
                continue
            c = kost(nx_, nz_) if (nx_, nz_) not in (sa, sb) else 1.0
            if c is None:
                continue
            nd = d + c * (1.414 if dx and dz else 1.0)
            if nd < dist.get((nx_, nz_), 1e9):
                dist[(nx_, nz_)] = nd
                vorig[(nx_, nz_)] = (x, z)
                heapq.heappush(hoop, (nd, (nx_, nz_)))
    if sb not in vorig:
        log("    s3: dorpsweg: geen vrije weg door het dorp gevonden; de route blijft zoals hij was")
        return lijn, None
    pad = [sb]
    while pad[-1] != sa:
        pad.append(vorig[pad[-1]])
    pad.reverse()
    # smooth (a running mean) and resample
    pts = [(x + 0.5, z + 0.5) for x, z in pad]
    zacht = []
    for i in range(len(pts)):
        lo, hi = max(0, i - 3), min(len(pts), i + 4)
        zacht.append((sum(p[0] for p in pts[lo:hi]) / (hi - lo), sum(p[1] for p in pts[lo:hi]) / (hi - lo)))
    zacht[0], zacht[-1] = pts[0], pts[-1]
    weg = _herbemonster(zacht, 1.5)
    nieuw = weg[1:-1] + vóór                                     # the village road, then the rest of the loop
    log(f"    s3: dorpsweg om de gebouwen heen ({len(weg) * 1.5:.0f} blokken, was {eind * 1.5:.0f})")
    return nieuw, (0, len(weg) - 2)


def ververs_route(park):
    """(final fix round 3) After the check's repairs (path jumps raised road cells, holes filled) the truck's levels are
    taken again from the road as it is now: per point the highest standing height of the road cells under the truck's
    footprint (never a wheel inside the road), smoothed upward only. Bridges keep their deck height (staan finds it)."""
    s3 = getattr(park, "_s3", None) or {}
    r = s3.get("route")
    if not r or not r.get("punten"):
        return
    # through every safaripoort exactly in its middle (its posts stand 2 blocks from the middle, the truck is 1.6 wide)
    n_ = len(r["punten"])
    for u in r.get("poorten") or []:
        ox, _, oz = u["pos"]
        midden = ox + 2.0 if u["as"] == "x" else oz + 2.0
        for k in range(u["i"] - 7, u["i"] + 8):
            w = 1.0 if abs(k - u["i"]) <= 3 else max(0.0, 1.0 - (abs(k - u["i"]) - 3) / 4.0)
            p = r["punten"][k % n_]
            if u["as"] == "x":
                p[0] = round(p[0] + w * (midden - p[0]), 2)
            else:
                p[2] = round(p[2] + w * (midden - p[2]), 2)
    # the snapped points may leave the corridor _rijbaan cleared by a few tenths: clear trees and plants (never
    # structures) in the truck's footprint again, from the road up TRUCK_HOOG
    for (x, y, z) in [tuple(p[:3]) for p in r["punten"]]:
        for cx in range(int(math.floor(x - TRUCK_HALF)), int(math.floor(x + TRUCK_HALF)) + 1):
            for cz in range(int(math.floor(z - TRUCK_HALF)), int(math.floor(z + TRUCK_HALF)) + 1):
                if not (0 <= cx < W and 0 <= cz < W):
                    continue
                ddx = max(cx - x, 0.0, x - (cx + 1))
                ddz = max(cz - z, 0.0, z - (cz + 1))
                if ddx * ddx + ddz * ddz >= TRUCK_HALF * TRUCK_HALF:
                    continue
                bij_poort = park.hek[cx, cz] and any(
                    math.hypot(cx + 0.5 - (u["pos"][0] + (2 if u["as"] == "x" else 0.5)),
                               cz + 0.5 - (u["pos"][2] + (2 if u["as"] == "z" else 0.5))) < 8
                    for u in r.get("poorten") or [])
                for yy in range(int(math.ceil(y - 0.01)) - 1, min(H, int(math.ceil(y - 0.01)) + TRUCK_HOOG)):
                    i = park.v[cx, yy, cz]
                    if not i:
                        continue
                    st = park.pal[i]
                    if bij_poort and ("fence_gate" in st or ("_log" in st and "axis=y" not in st)):
                        # a footpath gate (+ its lintel) in the zone fence right beside a safaripoort, on the truck
                        # road: the safaripoort lets walkers through too, so this little gate goes
                        park.v[cx, yy, cz] = 0
                    elif yy > int(math.ceil(y - 0.01)) and any(w in st for w in ("_log", "_leaves", "_wood"))                             and not st.startswith(("minecraft:stripped_", "guhs:")) and not park.hek[cx, cz]:
                        park.v[cx, yy, cz] = 0
    nieuw = []
    for (x, y, z) in [tuple(p[:3]) for p in r["punten"]]:
        best = None
        for cx in range(int(math.floor(x - TRUCK_HALF)), int(math.floor(x + TRUCK_HALF)) + 1):
            for cz in range(int(math.floor(z - TRUCK_HALF)), int(math.floor(z + TRUCK_HALF)) + 1):
                if not (0 <= cx < W and 0 <= cz < W) or not park.pad[cx, cz]:
                    continue
                if math.hypot(cx + 0.5 - x, cz + 0.5 - z) > TRUCK_HALF + 0.3:
                    continue
                h = staan(park, cx, cz)
                best = h if best is None else max(best, h)
        nieuw.append(float(best if best is not None else y))
    # smooth upward only (a max-filter then a gentle average that never goes below the max-filtered value)
    hoog = [max(nieuw[max(0, k - 1):k + 2]) for k in range(len(nieuw))]
    glad = _glad(hoog, r=1)
    ys = [max(g, h - 0.25) for g, h in zip(glad, hoog)]
    for p, y in zip(r["punten"], ys):
        p[1] = round(y, 2)


def truckroute(park, log):
    lijn = _herbemonster(park.route_lijn, 1.5)
    lijn, dorpsdeel = _dorpsweg(park, lijn, log)
    park._s3_dorpsdeel = dorpsdeel
    niveaus = _rijbaan(park, lijn, log)
    ys = [float(y) for y in niveaus]
    punten = [[round(x + 0.0, 2), round(y, 2), round(z + 0.0, 2)] for (x, z), y in zip(lijn, ys)]
    park._s3_lijn, park._s3_niveaus = lijn, niveaus
    park._pad_d = None                                   # (the corridor made path cells: the distance map is stale)
    pd = pad_afstand(park)
    zcent = {}
    for zi, k in enumerate(ZONES):
        xs, zs = np.nonzero(park.zone == zi)
        if len(xs):
            zcent[k] = (xs.mean(), zs.mean())
    haltes = []
    gebruikt = set()
    for k in ROUTE[:-1]:
        doel = getattr(park, "plein", None) if k == "entree" else zcent.get(k)
        if doel is None:
            continue
        kand = [i for i, (x, z) in enumerate(lijn) if _zone_op(park, x, z) == k and i not in gebruikt]
        if not kand:
            continue
        i = min(kand, key=lambda j: (lijn[j][0] - doel[0]) ** 2 + (lijn[j][1] - doel[1]) ** 2)
        if k == "entree":
            # round 3: an open platform along the village road (not a little house the truck drives through)
            deel = getattr(park, "_s3_dorpsdeel", None)
            # (the village road itself is narrow between Het Watergat and the fence: also the first stretch after it)
            n_ = len(lijn)
            kand_e = (list(range(deel[0], deel[1] + 26)) + list(range(n_ - 40, n_))) if deel else kand
            # never closer than 10 points to a zone border (its safaripoort must not stand at the stop)
            grenzen = [j for j in range(n_) if _zone_op(park, *lijn[j]) != _zone_op(park, *lijn[j - 1])]
            kand_e = [j for j in kand_e if _zone_op(park, *lijn[j]) == "entree"
                      and all(min(abs(j - g), n_ - abs(j - g)) >= 10 for g in grenzen)] or kand_e
            i = _perron(park, lijn, kand_e, pd, log)
            if i is None:
                i = min(kand, key=lambda j: j if j < len(lijn) // 2 else len(lijn))
                _halte_bouw(park, lijn, i, k, pd)
        else:
            _halte_bouw(park, lijn, i, k, pd)
        gebruikt.update(range(i - 30, i + 30))
        haltes.append({"zone": k, "naam": ZONE_NAAM[k], "i": i})
    lantaarns = _lantaarns(park, lijn, pd)
    poorten = _poorten(park, lijn, niveaus, haltes, log)
    log(f"    s3: truckroute {len(punten)} punten ({len(punten) * 1.5:.0f} blokken), {len(haltes)} haltes, "
        f"{lantaarns} lantaarnpalen, {len(poorten)} safaripoorten")
    return {"punten": punten, "stap": 1.5, "haltes": haltes, "poorten": poorten}


# ======================================================================================================================
# round 3: the safaripoort (guhs:safaripoort, 4 wide x 6 high x 1 deep) where the route crosses a zone border
POORT = "guhs:safaripoort"
POORT_PAAL = "minecraft:stripped_dark_oak_log"


def _poort_cellen(gx, gz, as_):
    """The 4 columns of a gate whose middle is between cells gx-1|gx (as_ 'x': along x) or gz-1|gz (as_ 'z')."""
    return [(gx + o, gz) if as_ == "x" else (gx, gz + o) for o in (-2, -1, 0, 1)]


def zet_poort(park, gx, y, gz, as_, palen=True):
    """A safaripoort: 4 x 6 part blocks (deel_x 0..3 along `as_`, deel_y 0..5), closed; with palen a sturdy log post on
    both sides (one block out, 7 high) so the fence line ends against it. y = the road level (standing height)."""
    for dx, (cx, cz) in enumerate(_poort_cellen(gx, gz, as_)):
        for dy in range(6):
            park.set(cx, y + dy, cz, POORT, **{"as": as_, "deel_x": dx, "deel_y": dy, "open": "false"})
    if palen:
        for o in (-3, 2):
            px, pz = (gx + o, gz) if as_ == "x" else (gx, gz + o)
            onderbouw(park, px, pz, y)
            for yy in range(min(int(park.hoogte[px, pz]) + 1, y), y + 7):
                park.set(px, yy, pz, POORT_PAAL, axis="y")
            park.set(px, y + 7, pz, "guhs:lampion_roze", hanging="false")
            park.hek[px, pz] = True


def _poorten(park, lijn, niveaus, haltes, log):
    """Where the route goes from one zone into another: a safaripoort across the road at the border (the zone fence
    there), never within 9 points (~13 blocks) of a stop (a truck must not stand in the gateway)."""
    stops = [h["i"] for h in haltes]
    zones = [_zone_op(park, x, z) for x, z in lijn]
    uit = []
    vorige = None
    n = len(lijn)
    for k in range(n):
        z0 = zones[k]
        if z0 is None:
            continue
        if vorige is not None and z0 != zones[vorige] and k - vorige < 6:
            # the border lies between vorige and k: put the gate on the fence column closest to that spot
            mid = (vorige + k) // 2
            best = None
            for j in range(max(0, mid - 6), min(n, mid + 7)):
                x, z = lijn[j]
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        cx, cz = int(x) + dx, int(z) + dz
                        if 0 <= cx < W and 0 <= cz < W and park.hek[cx, cz]:
                            d = abs(j - mid) + abs(dx) + abs(dz)
                            if best is None or d < best[0]:
                                best = (d, j)
            j = best[1] if best else mid
            if any(min(abs(j - s), n - abs(j - s)) < 9 for s in stops):
                log(f"    s3: poort bij route-punt {j} overgeslagen: te dicht bij een halte")
            elif all(min(abs(j - u["i"]), n - abs(j - u["i"])) > 10 for u in uit):
                rx, rz = _richting(lijn, j)
                as_ = "z" if abs(rx) >= abs(rz) else "x"       # the gate spans across the road
                x, z = lijn[j]
                gx, gz = (int(round(x)), int(math.floor(z))) if as_ == "x" else (int(math.floor(x)), int(round(z)))
                y = niveaus[j]
                zet_poort(park, gx, y, gz, as_)
                origin = _poort_cellen(gx, gz, as_)[0]
                uit.append({"pos": [origin[0], y, origin[1]], "as": as_, "i": j,
                            "van": zones[vorige], "naar": z0})
        vorige = k
    for u in uit:
        log(f"    s3: safaripoort {u['van']} -> {u['naar']} bij {u['pos']} (as {u['as']})")
    return uit


# ======================================================================================================================
# round 3: the truck stop at the entree, an open platform along the road (no little house the truck drives through)
def _perron(park, lijn, kandidaten, pd, log):
    """The platform at the first candidate route point (from the middle of the candidates outward) where a 12 x 4
    platform fits beside the road on free ground; returns that point's index (the stop) or None."""
    if not kandidaten:
        return None
    n = len(lijn)
    mid = kandidaten[0] + 10
    volgorde = sorted(kandidaten, key=lambda j: min(abs(j - mid), n - abs(j - mid)))
    for i in volgorde:
        if _perron_op(park, lijn, i, pd, log):
            return i
    log("    s3: perron entree: geen ruimte langs de dorpsweg")
    return None


def _perron_vrij(park, cx, cz, y):
    if not (2 <= cx < W - 2 and 2 <= cz < W - 2) or park.water[cx, cz] or park.hek[cx, cz] or not park.binnen[cx, cz]:
        return False
    if abs(int(park.hoogte[cx, cz]) - (y - 1)) > 2:
        return False
    for yy in range(y, y + 5):
        i = park.v[cx, yy, cz]
        if i and not any(w in park.pal[i] for w in _ZACHT):
            return False
    return True


def _perron_op(park, lijn, i, pd, log):
    x, z = lijn[i]
    rx, rz = _richting(lijn, i)
    y = park._s3_niveaus[i]
    for kant in (1, -1):
        nx, nz = -rz * kant, rx * kant
        # the platform: 12 along the road, 4 deep, starting 2.5 from the road's middle (the truck passes beside it)
        cellen = []
        for a in range(-6, 6):
            for b in range(0, 4):
                cx = int(math.floor(x + rx * (a + 0.5) + nx * (2.6 + b)))
                cz = int(math.floor(z + rz * (a + 0.5) + nz * (2.6 + b)))
                cellen.append((a, b, cx, cz))
        if not all(_perron_vrij(park, cx, cz, y) for _, _, cx, cz in cellen):
            continue
        # (final fix round 3) the road may curve after the stop: no platform cell may come within the truck's reach of
        # ANY route point near it (the posts stood right on the road a few points further)
        buur = [lijn[j % len(lijn)] for j in range(i - 40, i + 41)]
        def _raakt(cx, cz):
            for qx, qz in buur:
                ddx = max(cx - qx, 0.0, qx - (cx + 1))
                ddz = max(cz - qz, 0.0, qz - (cz + 1))
                if ddx * ddx + ddz * ddz < (TRUCK_HALF + 0.3) ** 2:
                    return True
            return False
        # (only what stands up counts: the floor lies at road level; posts, lamps, chairs and signs stand on it)
        if any(_raakt(cx, cz) for a_, b_, cx, cz in cellen if b_ in (0, 3) and a_ in (-6, -1, 5) or b_ in (1, 3)):
            continue
        f_weg = _facing(nx, nz)              # from the road outward
        f_road = _tegen(f_weg)               # toward the road
        langs = _facing(rx, rz)
        for a, b, cx, cz in cellen:
            vlak(park, cx, cz, y - 1, top="minecraft:smooth_sandstone", onder="minecraft:sandstone", vrij=6)
        # the edge to the road: a low slab step, then the floor
        for a, b, cx, cz in cellen:
            if b == 0:
                park.set(cx, y - 1, cz, "minecraft:cut_sandstone")
        # roof posts (the four corners and the middle back) and a thatched roof (rieten dak) 4 up
        posten = [(a, b) for a in (-6, -1, 5) for b in (0, 3)]
        for a, b, cx, cz in cellen:
            if (a, b) in posten:
                for yy in range(y, y + 4):
                    park.set(cx, yy, cz, "minecraft:stripped_acacia_log", axis="y")
            park.set(cx, y + 4, cz, "guhs:rieten_dak_plaat", type="bottom")
        # chairs along the back (looking at the road), planters between them, a lamp, the route-map sign
        for a, b, cx, cz in cellen:
            if b == 3 and (a, b) not in posten:
                if a in (-5, -3, 0, 2, 4):
                    kit.stoel(park, cx, y, cz, f_road)
                elif a in (-4, 1, 3):
                    park.set(cx, y, cz, "minecraft:potted_azure_bluet" if a == 1 else "minecraft:potted_red_tulip")
            if b == 1 and a in (-6, 5):
                park.set(cx, y, cz, "guhs:klusjes_guhlampje", facing=f_road, lit="true")
        # the route map (a sign at the back, between the posts) and the halte sign at the road end
        ma = [c for c in cellen if c[0] == -2 and c[1] == 3][0]
        bord(park, ma[2], y + 1, ma[3], ["Safaritruck", "Halte Entree", "rondje door", "alle 8 zones!"],
             rotation=_rotatie_naar(-nx, -nz))
        ka = [c for c in cellen if c[0] == 5 and c[1] == 1][0]
        bord(park, ka[2], y + 1, ka[3], ["", "Halte", "Entree", "~ safaritruck ~"], rotation=_rotatie_naar(-nx, -nz))
        # beside it: an old safari jeep (decor) and a little lookout
        for (dx_, bouw_) in ((10, lambda px, pz: _oude_jeep(park, px, pz, rx, rz)),
                             (-10, lambda px, pz: _uitkijkje(park, px, pz, f_road))):
            # beside it (only where it stays clear of the road, also where the road curves)
            for uit_ in (4.5, 6.0, 7.5):
                px, pz = x + rx * dx_ + nx * uit_, z + rz * dx_ + nz * uit_
                if all(math.hypot(px - qx, pz - qz) > TRUCK_HALF + 3.5 for qx, qz in buur):
                    bouw_(px, pz)
                    break
        log(f"    s3: perron entree bij route-punt {i} ({int(x)},{int(z)}), kant {f_weg}")
        return True
    return False


def _oude_jeep(park, x, z, rx, rz):
    """A parked old safari jeep (blocks): olive body, black wheels, a spare wheel, a little roof bar."""
    x, z = int(x), int(z)
    if not (3 <= x < W - 3 and 3 <= z < W - 3) or park.water[x, z]:
        return
    g = int(park.hoogte[x, z])
    langs_x = abs(rx) >= abs(rz)
    for a in range(-2, 3):
        for b in range(-1, 2):
            cx, cz = (x + a, z + b) if langs_x else (x + b, z + a)
            if park.water[cx, cz] or park.hek[cx, cz]:
                return
    for a in range(-2, 3):
        for b in range(-1, 2):
            cx, cz = (x + a, z + b) if langs_x else (x + b, z + a)
            vlak(park, cx, cz, g, top=park.get(cx, g, cz) if park.v[cx, g, cz] else "grass_block", vrij=4)
            wiel = a in (-2, 2) and b in (-1, 1)
            park.set(cx, g + 1, cz, "minecraft:black_concrete" if wiel else "minecraft:green_terracotta")
            if a == 2:
                park.set(cx, g + 2, cz, "minecraft:glass_pane" if b == 0 else "minecraft:green_terracotta")
            elif a == -2 and b == 0:
                park.set(cx, g + 2, cz, "minecraft:black_concrete")       # the spare wheel
            elif a in (-1, 0, 1) and b == 0:
                park.set(cx, g + 2, cz, "minecraft:brown_carpet")
    for a in (-1, 1):
        for b in (-1, 1):
            cx, cz = (x + a, z + b) if langs_x else (x + b, z + a)
            park.set(cx, g + 2, cz, "minecraft:dark_oak_fence")
            park.set(cx, g + 3, cz, "minecraft:dark_oak_slab", type="bottom")
    cx, cz = x, z
    park.set(cx, g + 3, cz, "minecraft:dark_oak_slab", type="bottom")


def _uitkijkje(park, x, z, kijk):
    """A small lookout: a 3x3 deck on posts 3 up, a ladder, railings, a lantern."""
    x, z = int(x), int(z)
    if not (3 <= x < W - 3 and 3 <= z < W - 3):
        return
    if any(park.water[x + a, z + b] or park.hek[x + a, z + b] for a in (-1, 0, 1) for b in (-1, 0, 1)):
        return
    g = max(int(park.hoogte[x + a, z + b]) for a in (-1, 0, 1) for b in (-1, 0, 1))
    for a in (-1, 0, 1):
        for b in (-1, 0, 1):
            onderbouw(park, x + a, z + b, int(park.hoogte[x + a, z + b]) + 1)
            park.set(x + a, g + 4, z + b, "minecraft:spruce_planks")
            if (a, b) != (0, 0) and abs(a) + abs(b) == 2:
                for yy in range(int(park.hoogte[x + a, z + b]) + 1, g + 4):
                    park.set(x + a, yy, z + b, "minecraft:stripped_spruce_log", axis="y")
            if abs(a) == 1 or abs(b) == 1:
                park.set(x + a, g + 5, z + b, "minecraft:spruce_fence")
    off = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[kijk]    # the ladder on the back
    sx, sz = x + off[0], z + off[1]
    for yy in range(int(park.hoogte[sx, sz]) + 1, g + 4):          # the ladder's backing
        park.set(sx, yy, sz, "minecraft:stripped_spruce_log", axis="y")
    lx, lz = x + off[0] * 2, z + off[1] * 2
    onderbouw(park, lx, lz, int(park.hoogte[lx, lz]) + 1)
    for yy in range(int(park.hoogte[lx, lz]) + 1, g + 5):
        park.set(lx, yy, lz, "minecraft:ladder", facing=_facing(off[0], off[1]), waterlogged="false")
    park.set(x + off[0], g + 5, z + off[1], "minecraft:air")
    park.set(x, g + 5, z, "minecraft:lantern", hanging="false", waterlogged="false")


def _richting(lijn, i):
    a, b = lijn[max(i - 3, 0)], lijn[min(i + 3, len(lijn) - 1)]
    d = math.hypot(b[0] - a[0], b[1] - a[1]) or 1
    return (b[0] - a[0]) / d, (b[1] - a[1]) / d


def _halte_bouw(park, lijn, i, zone, pd):
    """A halte beside the route: a sign facing the road, a bench and a lantern post."""
    x, z = lijn[i]
    rx, rz = _richting(lijn, i)
    for kant in (1, -1):
        nx, nz = -rz * kant, rx * kant           # perpendicular
        for afst in (3, 4, 5):
            sx, sz = int(round(x + nx * afst)), int(round(z + nz * afst))
            if not _vrije_grond(park, sx, sz, pd):
                continue
            # bench next to the sign, along the road
            bx, bz = int(round(sx + rx * 2)), int(round(sz + rz * 2))
            lx, lz = int(round(sx - rx * 2)), int(round(sz - rz * 2))
            if not (_vrije_grond(park, bx, bz, pd) and _vrije_grond(park, lx, lz, pd)):
                continue
            g = int(park.hoogte[sx, sz])
            bord(park, sx, g + 1, sz, ["", "Halte", ZONE_NAAM[zone], "~ safaritruck ~"],
                 rotation=_rotatie_naar(-nx, -nz))
            gb = int(park.hoogte[bx, bz])
            facing = _facing(-nx, -nz)
            park.set(bx, gb + 1, bz, "minecraft:spruce_stairs", facing=_tegen(facing), half="bottom", shape="straight",
                     waterlogged="false")
            gl = int(park.hoogte[lx, lz])
            park.set(lx, gl + 1, lz, "minecraft:dark_oak_fence")
            park.set(lx, gl + 2, lz, "minecraft:dark_oak_fence")
            park.set(lx, gl + 3, lz, "minecraft:lantern", hanging="false", waterlogged="false")
            return
    # nowhere free: the halte is only on the map


def _facing(dx, dz):
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def _tegen(f):
    return {"east": "west", "west": "east", "north": "south", "south": "north"}[f]


def _lantaarns(park, lijn, pd, elke=26):
    n = 0
    kant = 1
    for i in range(10, len(lijn) - 10, int(elke / 1.5)):
        x, z = lijn[i]
        rx, rz = _richting(lijn, i)
        kant = -kant
        for afst in (3, 4):
            sx, sz = int(round(x - rz * kant * afst)), int(round(z + rx * kant * afst))
            if not _vrije_grond(park, sx, sz, pd):
                continue
            if any(math.hypot(sx - a, sz - b) < r + 2 for a, b, r, _ in kit.RESERVERINGEN.values()):
                continue
            g = int(park.hoogte[sx, sz])
            park.set(sx, g + 1, sz, "minecraft:dark_oak_fence")
            park.set(sx, g + 2, sz, "minecraft:dark_oak_fence")
            park.set(sx, g + 3, sz, "minecraft:lantern", hanging="false", waterlogged="false")
            n += 1
            break
    return n


# ======================================================================================================================
# the canoe
_NAT = ("minecraft:water", "minecraft:seagrass", "minecraft:tall_seagrass", "minecraft:kelp")
_LUCHTIG = ("minecraft:lily_pad", "minecraft:sugar_cane")      # a canoe glides past a lily pad


def _water_ok(park, x, z):
    x, z = int(x), int(z)
    if not (1 <= x < W - 1 and 1 <= z < W - 1):
        return False
    if not park.pal[park.v[x, WATER_Y, z]].startswith(_NAT):
        return False
    return all(park.v[x, y, z] == 0 or park.pal[park.v[x, y, z]].startswith(_LUCHTIG) for y in (WATER_Y + 1, WATER_Y + 2))


def _snap(park, x, z, r=4):
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if _water_ok(park, x + dx, z + dz):
                # prefer cells with water around (the middle of the stream)
                ruim = sum(_water_ok(park, x + dx + a, z + dz + b) for a, b in ((2, 0), (-2, 0), (0, 2), (0, -2)))
                k = (dx * dx + dz * dz) - ruim * 1.5
                if best is None or k < best[0]:
                    best = (k, x + dx + 0.5, z + dz + 0.5)
    return None if best is None else (best[1], best[2])


MAX_KANO = 420     # blocks upstream before the guided trip turns (a trip of a few minutes)


def _midden(park, pad, r=0.9, schuif=1.75):
    """(final fix round 3) Keep the canoe off the banks: a point with a bank or wall block closer than r is moved to the
    nearest spot (up to `schuif` away) whose whole surroundings within r are open water; points that cannot are kept."""
    def vrij(x, z):
        for cx in range(int(math.floor(x - r)), int(math.floor(x + r)) + 1):
            for cz in range(int(math.floor(z - r)), int(math.floor(z + r)) + 1):
                ddx = max(cx - x, 0.0, x - (cx + 1))
                ddz = max(cz - z, 0.0, z - (cz + 1))
                if ddx * ddx + ddz * ddz < r * r and not _water_ok(park, cx + 0.5, cz + 0.5):
                    return False
        return True
    uit = []
    for x, z in pad:
        if vrij(x, z):
            uit.append((x, z))
            continue
        best = None
        for d in np.arange(0.25, schuif + 0.01, 0.25):
            for k in range(16):
                h = 2 * math.pi * k / 16
                nx_, nz_ = x + math.cos(h) * d, z + math.sin(h) * d
                if vrij(nx_, nz_):
                    best = (nx_, nz_)
                    break
            if best:
                break
        uit.append(best or (x, z))
    return uit


def _vrij_stuk(park, a, b):
    """Every cell between two path points is open water with headroom (no crossing a dam or a bank)."""
    n = int(math.hypot(b[0] - a[0], b[1] - a[1]) * 4) + 1
    return all(_water_ok(park, a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1))


def kano(park, log):
    meer = P(*MEREN[0][0])                         # the village pond (the river ends in it)
    plein = getattr(park, "plein", None) or (meer[0], 0, meer[1] + 20)
    # start: the pond water closest to the square
    best = None
    for dx in range(-30, 31):
        for dz in range(-30, 31):
            x, z = meer[0] + dx, meer[1] + dz
            if _water_ok(park, x, z) and _water_ok(park, x + 1, z) and _water_ok(park, x, z + 1):
                d = math.hypot(x - plein[0], z - plein[2])
                if best is None or d < best[0]:
                    best = (d, x, z)
    if best is None:
        log("    s3: kano: geen vijverwater gevonden")
        return None
    sx, sz = best[1] + 0.5, best[2] + 0.5
    rivier = [P(x, z) for x, z in reversed(RIVIER)]
    rivier = [meer] + rivier
    ruw = _herbemonster(rivier, 1.0)
    # from the start to the river: straight to the pond centre first
    pad = [(sx, sz)]
    for x, z in _herbemonster([(sx, sz), meer], 1.0)[1:]:
        s = _snap(park, x, z)
        if s:
            pad.append(s)
    geblokt = None
    lengte = 0.0
    for x, z in ruw:
        s = _snap(park, x, z)
        if s is None or math.hypot(s[0] - pad[-1][0], s[1] - pad[-1][1]) > 3.5 or not _vrij_stuk(park, pad[-1], s):
            geblokt = (int(x), int(z))
            break
        lengte += math.hypot(s[0] - pad[-1][0], s[1] - pad[-1][1])
        pad.append(s)
        if lengte > MAX_KANO:
            geblokt = "max"
            break
    # turn a little before the blocked spot, then back the same way
    pad = pad[:max(10, len(pad) - 12)]
    pad = _herbemonster(_midden(park, pad), 1.5)
    heen = [[round(x, 2), WATER_Y + 0.6, round(z, 2)] for x, z in pad]
    terug = list(reversed(heen))[1:]
    _steiger(park, sx, sz, plein)
    log(f"    s3: kano {len(heen)} punten heen ({len(heen) * 1.5:.0f} blokken), keert bij {geblokt}")
    return {"punten": heen + terug, "keer": len(heen) - 1, "start": heen[0], "stap": 1.5}


def _steiger(park, sx, sz, plein):
    """A small plank jetty from the shore to the canoe's mooring."""
    dx, dz = plein[0] - sx, plein[2] - sz
    d = math.hypot(dx, dz) or 1
    dx, dz = dx / d, dz / d
    x, z = sx + dx * 1.6, sz + dz * 1.6          # (final fix round 3: not on the mooring itself, the canoe lies beside it)
    for _ in range(12):
        xi, zi = int(x), int(z)
        if not park.water[xi, zi]:
            break
        for a in (-1, 0, 1):
            cx, cz = int(round(x - dz * a)), int(round(z + dx * a))
            if park.water[cx, cz] and park.v[cx, WATER_Y + 1, cz] == 0:
                park.set(cx, WATER_Y + 1, cz, "minecraft:spruce_slab", type="bottom", waterlogged="false")
        x += dx
        z += dz


# ======================================================================================================================
# the kabelbaan
def kabelbaan(park, log):
    """Round 3: a LOOP with two parallel lines (out on one, back on the other, so the gondolas never meet), T-shaped
    masts between the lines (the gondolas hang in the middle under their cable, far from the pole) and at both ends an
    open station where the gondola rides a half circle (the turning loop) along an open deck and stands still ~5 s at its
    middle. Signs and lanterns hang on the station's outer edge, never in the gondola's way."""
    # (the decks stand on legs: the ground under a station may be rough, it only has to be free of trees and paths)
    a = None
    for half, helling in ((5, 4), (4, 6), (3, 9)):
        a = zoek(park, "ijskust", P(205, 95), 50, half, minhek=8, pad_min=4, helling=helling)
        if a:
            break
    b = None
    for half, helling, weg_r in ((5, 5, 12), (4, 8, 10), (3, 12, 8), (3, 16, 6)):
        b = zoek(park, "bamboe", P(420, 90), 80, half, minhek=8, pad_min=3, helling=helling,
                 weg_van=[(x, z) for x, z, r, k in kit.RESERVERINGEN.values() if k == "bamboe"], weg_r=weg_r)
        if b:
            break
    if not a or not b:
        log(f"    s3: kabelbaan: geen plek ({a}, {b})")
        return None
    (ax, az, ag), (bx, bz, bg) = a, b
    lengte = math.hypot(bx - ax, bz - az)
    ux, uz = (bx - ax) / lengte, (bz - az) / lengte
    nx, nz = -uz, ux                       # to the right of A -> B
    top = 0
    for t in np.linspace(0, 1, int(lengte) + 1):
        for off in np.linspace(-KABEL_R - 2, KABEL_R + 2, 9):
            x, z = int(round(ax + (bx - ax) * t + nx * off)), int(round(az + (bz - az) * t + nz * off))
            if 0 <= x < W and 0 <= z < W:
                kol = np.nonzero(park.v[x, :, z])[0]
                if len(kol):
                    top = max(top, int(kol.max()))
    kabel = min(H - 8, top + 8)            # the cable height (iron bars at kabel)
    dek = kabel - 6                        # the station decks (the gondola's floor stands on them)
    # the loop (gondola floor points): out on the right line, a half circle around B, back on the left, around A
    lus = []
    stap = 1.0
    n_lijn = max(2, int(lengte / stap))
    for k in range(n_lijn + 1):
        t = k / n_lijn
        lus.append((ax + (bx - ax) * t + nx * KABEL_R, az + (bz - az) * t + nz * KABEL_R))
    stations = []
    n_boog = max(8, int(math.pi * KABEL_R / stap))
    for (cx, cz, terug) in ((bx, bz, False),):
        for k in range(1, n_boog):
            hoek = math.pi * k / n_boog        # from +n around the far side (+u) to -n
            px = cx + nx * KABEL_R * math.cos(hoek) + ux * KABEL_R * math.sin(hoek)
            pz = cz + nz * KABEL_R * math.cos(hoek) + uz * KABEL_R * math.sin(hoek)
            lus.append((px, pz))
            if k == n_boog // 2:
                stations.append({"i": len(lus) - 1, "naam": "bamboe"})
    for k in range(n_lijn + 1):
        t = k / n_lijn
        lus.append((bx + (ax - bx) * t - nx * KABEL_R, bz + (az - bz) * t - nz * KABEL_R))
    for k in range(1, n_boog):
        hoek = math.pi * k / n_boog
        px = ax - nx * KABEL_R * math.cos(hoek) - ux * KABEL_R * math.sin(hoek)
        pz = az - nz * KABEL_R * math.cos(hoek) - uz * KABEL_R * math.sin(hoek)
        lus.append((px, pz))
        if k == n_boog // 2:
            stations.append({"i": len(lus) - 1, "naam": "ijskust"})
    # the stations, the cable along the whole loop, the masts
    for (x, z, g, naam, naar) in ((ax, az, ag, "IJskust", (ux, uz)), (bx, bz, bg, "Bamboebergen", (-ux, -uz))):
        _station(park, x, z, g, dek, kabel, naar, naam)
    for (px, pz) in _herbemonster(lus + [lus[0]], 0.5):
        cx, cz = int(math.floor(px)), int(math.floor(pz))
        if park.v[cx, kabel, cz] == 0:
            park.set(cx, kabel, cz, "minecraft:iron_bars")
    n_mast = max(1, int(lengte // 36))
    masten = []
    for k in range(1, n_mast + 1):
        t0 = k / (n_mast + 1)
        # (final fix round 3: a mast never stands on a path, the truck road or water: a pole on the road blocked the truck
        #  and made the path-jump repair build dirt ramps up to the mast top under the cable; slide along the line)
        beste = None
        for dt in sorted(np.linspace(-0.12, 0.12, 49), key=abs):
            t = t0 + dt
            mx, mz = ax + (bx - ax) * t, az + (bz - az) * t
            if _mast_vrij(park, mx, mz):
                beste = (mx, mz)
                break
        if beste is None:
            log(f"    s3: kabelbaan: geen vrije mastplek rond t={t0:.2f}, mast overgeslagen")
            continue
        mx, mz = beste
        _mast(park, mx, mz, kabel, (nx, nz))
        masten.append([round(mx, 1), round(mz, 1)])
    gondel_y = dek + 1.0
    punten = [[round(x, 2), gondel_y, round(z, 2)] for x, z in lus]
    log(f"    s3: kabelbaan-lus ({ax},{az}) <-> ({bx},{bz}), {lengte:.0f} blokken per kant, {len(punten)} punten, "
        f"kabel y{kabel}, dek y{dek}, {len(masten)} T-masten")
    return {"lus": punten, "stations": stations, "kabel": kabel, "dek": dek, "masten": masten, "stap": stap,
            "a": [ax + 0.5, gondel_y, az + 0.5], "b": [bx + 0.5, gondel_y, bz + 0.5]}


KABEL_R = 4.5      # half the distance between the two lines (and the radius of the turning loops); 4.5 keeps the
                   # gondolas (centred under their cable) well clear of the centred 2x2 mast pole


def _station(park, x, z, g, dek, kabel, naar, naam):
    """An open station: a round deck (radius 7) at `dek` on log legs, open in the middle ring where the gondola rides
    its half circle; a ladder up on the far side (away from the line), railings on the outer edge, a roof of slabs on
    posts at the edge, signs and a lantern on the outer edge."""
    ux, uz = naar
    R = 8      # (final fix round 3: 8 not 7, the turning loop (KABEL_R 4.5) + the gondola stay clear of the roof posts)
    for dx in range(-R - 1, R + 2):
        for dz in range(-R - 1, R + 2):
            d = math.hypot(dx, dz)
            if d > R + 0.5:
                continue
            cx, cz = x + dx, z + dz
            park.set(cx, dek, cz, "minecraft:spruce_planks")
            for yy in range(dek + 1, kabel + 3):
                if park.v[cx, yy, cz] and not park.get(cx, yy, cz).startswith("minecraft:iron_bars"):
                    park.v[cx, yy, cz] = 0
            if R - 0.5 < d <= R + 0.5:
                park.set(cx, dek + 1, cz, "minecraft:spruce_fence")
            park.set(cx, kabel + 3, cz, "minecraft:spruce_slab", type="bottom", waterlogged="false")
    # legs and roof posts on the edge, 8 of them (none on the line side: the gondolas come in there)
    for k in range(8):
        hoek = 2 * math.pi * k / 8 + math.pi / 8
        lx, lz = int(round(x + math.cos(hoek) * (R - 0.5))), int(round(z + math.sin(hoek) * (R - 0.5)))
        on_line = (math.cos(hoek) * ux + math.sin(hoek) * uz) > 0.3     # (final fix: well away from both lines)
        for yy in range(int(park.hoogte[lx, lz]) + 1, dek):
            park.set(lx, yy, lz, "minecraft:spruce_log", axis="y")
        onderbouw(park, lx, lz, int(park.hoogte[lx, lz]) + 1)
        if not on_line:
            for yy in range(dek + 1, kabel + 3):
                park.set(lx, yy, lz, "minecraft:spruce_log", axis="y")
    # the line side is open: no railing where the lines leave the station
    for dx in range(-R - 1, R + 2):
        for dz in range(-R - 1, R + 2):
            d = math.hypot(dx, dz)
            if R - 0.5 < d <= R + 0.5 and (dx * ux + dz * uz) / max(d, 0.1) > 0.3:
                park.set(x + dx, dek + 1, z + dz, "minecraft:air")
    # the centre: a little stand with the station sign (wall signs on a post, facing outward) and a lantern
    for yy in range(dek + 1, dek + 3):
        park.set(x, yy, z, "minecraft:stripped_spruce_log", axis="y")
    park.set(x, dek + 3, z, "minecraft:lantern", hanging="false", waterlogged="false")
    for f, (ox, oz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
        if ox * ux + oz * uz > 0.7:
            continue                                   # (the gondola turns in front of the line side)
        park.set(x + ox, dek + 2, z + oz, "minecraft:spruce_wall_sign", facing=f, waterlogged="false")
        park.bordjes = getattr(park, "bordjes", [])
        park.bordjes.append({"pos": [x + ox, dek + 2, z + oz],
                             "tekst": ["Kabelbaan", naam, "instappen als", "de gondel stilstaat"]})
    # the ladder on the far side (away from the lines), from the ground to the deck
    lx, lz = int(round(x - ux * (R + 1))), int(round(z - uz * (R + 1)))
    bx, bz = int(round(x - ux * R)), int(round(z - uz * R))
    f = _facing(-ux, -uz)
    onderbouw(park, lx, lz, int(park.hoogte[lx, lz]) + 1)
    for yy in range(int(park.hoogte[bx, bz]) + 1, dek):
        park.set(bx, yy, bz, "minecraft:spruce_planks")
    for yy in range(int(park.hoogte[lx, lz]) + 1, dek + 2):
        park.set(lx, yy, lz, "minecraft:ladder", facing=f, waterlogged="false")
    park.set(bx, dek + 1, bz, "minecraft:air")
    vx, vz = int(round(x - ux * (R + 3))), int(round(z - uz * (R + 3)))
    if 0 <= vx < W and 0 <= vz < W and not park.water[vx, vz] and park.v[vx, int(park.hoogte[vx, vz]) + 1, vz] == 0:
        bord(park, vx, int(park.hoogte[vx, vz]) + 1, vz, ["", "Kabelbaan", naam, "klim omhoog!"],
             rotation=_rotatie_naar(-ux, -uz))


def _mast_cellen(mx, mz):
    """The 2x2 pole of a mast, centred on (mx, mz) (so it stays as far as possible from both lines)."""
    x0, z0 = int(round(mx)) - 1, int(round(mz)) - 1
    return [(x0 + dx, z0 + dz) for dx in (0, 1) for dz in (0, 1)]


def _mast_vrij(park, mx, mz, marge=2):
    """No path, road, water, fence or building within `marge` of the pole."""
    for (cx, cz) in _mast_cellen(mx, mz):
        for dx in range(-marge, marge + 1):
            for dz in range(-marge, marge + 1):
                x, z = cx + dx, cz + dz
                if not (0 <= x < W and 0 <= z < W):
                    return False
                if park.pad[x, z] or park.water[x, z] or park.hek[x, z]:
                    return False
    return True


def _mast(park, mx, mz, kabel, n):
    """A T-mast between the two lines: a 2x2 log pole from the ground up to the cable, a crossbeam along n on top that
    reaches past both cables (they hang from its ends), a lamp on top."""
    nx, nz = n
    for (cx, cz) in _mast_cellen(mx, mz):
        g = int(park.hoogte[cx, cz])
        onderbouw(park, cx, cz, g + 1)
        for y in range(g + 1, kabel + 2):
            park.set(cx, y, cz, "minecraft:stripped_spruce_log", axis="y")
    x0, z0 = _mast_cellen(mx, mz)[0]
    for k in np.linspace(-(KABEL_R + 1.5), KABEL_R + 1.5, 20):
        cx, cz = int(math.floor(mx + nx * k + 0.5)), int(math.floor(mz + nz * k + 0.5))
        park.set(cx, kabel + 1, cz, "minecraft:spruce_planks")
    park.set(x0, kabel + 2, z0, "guhs:lampion_roze", hanging="false")


# ======================================================================================================================
# round 3: the glass-bottom boat (delta water + the ijskust bay)
def _boot_water(park, x, z):
    """Open water at the surface with 2 free blocks above and water under it too (the boat floats, nothing to hit)."""
    x, z = int(x), int(z)
    if not (1 <= x < W - 1 and 1 <= z < W - 1):
        return False
    if not park.pal[park.v[x, WATER_Y, z]].startswith("minecraft:water"):
        return False
    if not park.pal[park.v[x, WATER_Y - 1, z]].startswith(_NAT):
        return False
    return all(park.v[x, y, z] == 0 or park.pal[park.v[x, y, z]].startswith(_LUCHTIG) for y in (WATER_Y + 1, WATER_Y + 2))


def boot(park, zone, log):
    """A calm loop over the biggest open water of the zone, BOOT_R from every shore (the hull is 3 wide), and a jetty
    from the nearest path to the loop's stop."""
    from scipy.ndimage import label, distance_transform_edt as edt
    zi_ = ZONES.index(zone)
    mask = np.zeros((W, W), bool)
    xs, zs = np.nonzero(park.zone == zi_)
    for x, z in zip(xs, zs):
        if _boot_water(park, x, z):
            mask[x, z] = True
    lab, n = label(mask)
    if not n:
        log(f"    s3: boot {zone}: geen open water")
        return None
    grootte = np.bincount(lab.ravel())
    grootte[0] = 0
    deel = lab == grootte.argmax()
    d = edt(deel)
    if d.max() < 6:
        return boot_rivier(park, zone, log, f"het open water is te smal (max {d.max():.1f})")
    cx, cz = np.unravel_index(int(d.argmax()), d.shape)
    stralen = []
    for k in range(48):
        h = 2 * math.pi * k / 48
        r = 0.0
        while True:
            px, pz = cx + 0.5 + math.cos(h) * (r + 0.5), cz + 0.5 + math.sin(h) * (r + 0.5)
            ix, iz = int(px), int(pz)
            if not (0 <= ix < W and 0 <= iz < W) or d[ix, iz] < 4.0:
                break
            r += 0.5
        stralen.append(r)
    stralen = np.array(stralen)
    stralen = np.minimum(stralen, np.convolve(np.r_[stralen[-2:], stralen, stralen[:2]], np.ones(5) / 5, "valid"))
    r_max = min(26.0, float(np.median(stralen)) * 1.3)
    stralen = np.clip(stralen, 0, r_max)
    if stralen.min() < 4:
        stralen = np.maximum(stralen * 0.75, 0)
    punten = [(cx + 0.5 + math.cos(2 * math.pi * k / 48) * stralen[k], cz + 0.5 + math.sin(2 * math.pi * k / 48) * stralen[k])
              for k in range(48)]
    lus = _herbemonster(punten + [punten[0]], 1.5)[:-1]
    if len(lus) < 30:
        return boot_rivier(park, zone, log, f"de lus over het open water is te kort ({len(lus)} punten)")
    # every point (and the hull around it) on open water
    for x, z in lus:
        for ox in (-1.5, 0, 1.5):
            for oz in (-1.5, 0, 1.5):
                if not _boot_water(park, x + ox, z + oz):
                    return boot_rivier(park, zone, log, f"de lus raakt de oever bij ({int(x)},{int(z)})")
    # the stop: the loop point closest to a path; a jetty from the shore toward it
    pd = pad_afstand(park)
    best = None
    for k, (x, z) in enumerate(lus):
        for h in range(0, 360, 15):
            for r in range(3, 20):
                sx, sz = int(x + math.cos(math.radians(h)) * r), int(z + math.sin(math.radians(h)) * r)
                if not (0 <= sx < W and 0 <= sz < W) or park.water[sx, sz]:
                    if 0 <= sx < W and 0 <= sz < W and not park.water[sx, sz]:
                        break
                    continue
                if park.binnen[sx, sz] and not park.hek[sx, sz] and park.hek_afstand[sx, sz] >= 3:
                    kost = r * 2 + pd[sx, sz]
                    if best is None or kost < best[0]:
                        best = (kost, k, sx, sz, r, h)
                break
    if best is None:
        log(f"    s3: boot {zone}: geen plek voor de steiger")
        return None
    _, k, sx, sz, r, h = best
    lus = lus[k:] + lus[:k]             # the loop starts at the stop
    x, z = lus[0]
    _boot_steiger(park, sx, sz, x, z, lus)
    log(f"    s3: boot {zone}: lus van {len(lus)} punten ({len(lus) * 1.5:.0f} blokken), steiger bij ({sx},{sz})")
    return {"zone": zone, "punten": [[round(a, 2), WATER_Y + 0.55, round(b, 2)] for a, b in lus], "stap": 1.5,
            "steiger": [sx + 0.5, int(park.hoogte[sx, sz]) + 1.0, sz + 0.5]}


def boot_rivier(park, zone, log, waarom):
    """No lake wide enough for a loop: a calm trip along the river through the zone and back the same way (like the
    canoe), from a jetty near the path; it turns before the first spot where the hull would touch a bank or a bridge."""
    pts = [P(x, z) for x, z in RIVIER]
    ruw = _herbemonster(pts, 1.0)
    zi_ = ZONES.index(zone)
    run, beste = [], []
    for x, z in ruw:
        ix, iz = int(x), int(z)
        ok = 0 <= ix < W and 0 <= iz < W and park.zone[ix, iz] == zi_
        s = _snap(park, x, z, r=3) if ok else None
        if s and all(_boot_water(park, s[0] + ox, s[1] + oz) for ox in (-1.2, 0, 1.2) for oz in (-1.2, 0, 1.2)):
            if run and (math.hypot(s[0] - run[-1][0], s[1] - run[-1][1]) > 3 or not _vrij_stuk(park, run[-1], s)):
                if len(run) > len(beste):
                    beste = run
                run = []
            run.append(s)
        else:
            if len(run) > len(beste):
                beste = run
            run = []
    if len(run) > len(beste):
        beste = run
    if len(beste) < 40:
        log(f"    s3: boot {zone}: {waarom}, en geen rivierstuk van 40+ blokken vrij ({len(beste)})")
        return None
    pad = _herbemonster(beste, 1.5)
    # the stop at the end closest to a path
    pd = pad_afstand(park)
    if pd[int(pad[-1][0]), int(pad[-1][1])] < pd[int(pad[0][0]), int(pad[0][1])]:
        pad = list(reversed(pad))
    x, z = pad[0]
    best = None
    for h in range(0, 360, 10):
        for r in range(2, 16):
            sx, sz = int(x + math.cos(math.radians(h)) * r), int(z + math.sin(math.radians(h)) * r)
            if not (0 <= sx < W and 0 <= sz < W):
                break
            if park.water[sx, sz]:
                continue
            if park.binnen[sx, sz] and not park.hek[sx, sz] and park.hek_afstand[sx, sz] >= 3:
                kost = r * 2 + pd[sx, sz]
                if best is None or kost < best[0]:
                    best = (kost, sx, sz)
            break
    if best is None:
        log(f"    s3: boot {zone}: geen plek voor de steiger aan de rivier")
        return None
    _, sx, sz = best
    _boot_steiger(park, sx, sz, x, z, pad)
    heen = [[round(a, 2), WATER_Y + 0.55, round(b, 2)] for a, b in pad]
    terug = list(reversed(heen))[1:-1]
    log(f"    s3: boot {zone}: {waarom}; rivierrit heen en terug ({len(heen) * 1.5:.0f} blokken), steiger bij ({sx},{sz})")
    return {"zone": zone, "punten": heen + terug, "stap": 1.5, "heen_en_terug": True,
            "steiger": [sx + 0.5, int(park.hoogte[sx, sz]) + 1.0, sz + 0.5]}


def _boot_steiger(park, sx, sz, x, z, punten=()):
    """A plank jetty (3 wide) from the shore cell (sx, sz) to 2.3 blocks before the boat's stop. Its deck lies in the water
    surface (waterlogged bottom slabs, level with the boat's floor), so the 2 blocks above the water stay free for the
    canoe; it keeps clear of the canoe's line itself, and only its end posts (a fence with a lantern) stand up."""
    kano = [(p[0], p[2]) for p in ((getattr(park, "_s3_kano", None) or {}).get("punten") or [])]

    def bij_kano(cx, cz):
        return any(abs(kx - cx - 0.5) < 1.2 and abs(kz - cz - 0.5) < 1.2 for kx, kz in kano)

    dx, dz = x - (sx + 0.5), z - (sz + 0.5)
    d = math.hypot(dx, dz) or 1
    dx, dz = dx / d, dz / d
    lang = max(1.0, d - 2.3)
    t = 0.0
    while t <= lang:
        px, pz = sx + 0.5 + dx * t, sz + 0.5 + dz * t
        for a in (-1, 0, 1):
            cx, cz = int(math.floor(px - dz * a)), int(math.floor(pz + dx * a))
            if park.water[cx, cz] and park.v[cx, WATER_Y + 1, cz] == 0 and not bij_kano(cx, cz):
                park.set(cx, WATER_Y, cz, "minecraft:spruce_slab", type="bottom", waterlogged="true")
        t += 0.5
    ex, ez = int(math.floor(sx + 0.5 + dx * lang)), int(math.floor(sz + 0.5 + dz * lang))
    for a in (-1, 1):
        cx, cz = int(math.floor(ex - dz * a)), int(math.floor(ez + dx * a))
        if any(math.hypot(cx + 0.5 - bx_, cz + 0.5 - bz_) < 2.2 for bx_, bz_ in punten):
            continue                    # (final fix round 3: an end post never where the boat passes)
        if park.water[cx, cz] and not bij_kano(cx, cz):
            park.set(cx, WATER_Y, cz, "minecraft:spruce_fence", waterlogged="true")
            park.set(cx, WATER_Y + 1, cz, "minecraft:spruce_fence")
            park.set(cx, WATER_Y + 2, cz, "minecraft:lantern", hanging="false", waterlogged="false")


# ======================================================================================================================
# the struisvogelrace
def racebaan(park, log):
    plek = zoek(park, "savanne", P(380, 470), 70, 16, minhek=7, pad_min=5, helling=3,
                weg_van=[(x, z) for x, z, r, k in kit.RESERVERINGEN.values() if k == "savanne"], weg_r=26)
    if not plek:
        plek = zoek(park, "savanne", P(380, 470), 90, 13, minhek=6, pad_min=4, helling=4)
    if not plek:
        log("    s3: racebaan: geen plek")
        return None
    cx, cz, g = plek
    a, b = 13.0, 7.0
    # flat ground, track band (width 3) of coarse dirt
    for x in range(int(cx - a - 3), int(cx + a + 4)):
        for z in range(int(cz - b - 3), int(cz + b + 4)):
            e = math.hypot((x - cx) / (a + 2.5), (z - cz) / (b + 2.5))
            if e <= 1.0:
                f = math.hypot((x - cx) / a, (z - cz) / b)
                top = "minecraft:coarse_dirt" if 0.78 <= f <= 1.18 else "grass_block"
                vlak(park, x, z, g, top=top)
    # hay bales on the outside and the inside edge, every few blocks
    for k in range(48):
        t = k / 48 * 2 * math.pi
        for schaal, elk in ((1.30, 2), (0.66, 3)):
            if k % elk:
                continue
            x, z = int(round(cx + math.cos(t) * a * schaal)), int(round(cz + math.sin(t) * b * schaal))
            if park.v[x, g + 1, z] == 0:
                park.set(x, g + 1, z, "minecraft:hay_block", axis="y")
    # the centre line, starting at the start/finish (the east end), anticlockwise
    lijn = []
    for k in range(72):
        t = k / 72 * 2 * math.pi
        x, z = cx + math.cos(t) * a, cz - math.sin(t) * b
        lijn.append([round(x, 2), g + 1.0, round(z, 2)])
    sx, sz = int(round(cx + a)), int(round(cz))
    # start arch across the track (along z), checkered carpet line
    for dz in range(-2, 3):
        park.set(sx, g + 1, sz + dz, "minecraft:white_carpet" if (dz % 2 == 0) else "minecraft:black_carpet")
    for dz in (-3, 3):
        for y in range(g + 1, g + 6):
            park.set(sx, y, sz + dz, "minecraft:stripped_acacia_log", axis="y")
    for dz in range(-3, 4):
        park.set(sx, g + 6, sz + dz, "minecraft:acacia_planks")
    bord_x, bord_z = sx + 3, sz - 5
    if park.v[bord_x, g + 1, bord_z] == 0:
        bord(park, bord_x, g + 1, bord_z, ["Struisvogelrace", "2 rondjes", "klik op een", "struisvogel!"],
             rotation=_rotatie_naar(-1, 1))
    log(f"    s3: racebaan bij ({cx},{cz}) y{g}")
    return {"lijn": lijn, "start": [sx + 0.5, g + 1.0, sz + 0.5], "wacht": [sx + 2.5, g + 1.0, sz + 4.5],
            "bord": [bord_x + 0.5, g + 3.6, bord_z + 0.5], "rondes": 2}


# ======================================================================================================================
# schemabord, atmosphere and the moment spots
def schemabord(park, log):
    plein = getattr(park, "plein", None)
    if not plein:
        return None
    px, py, pz = plein
    pd = pad_afstand(park)
    for r in range(5, 16):
        for k in range(16):
            t = k / 16 * 2 * math.pi
            x, z = int(round(px + math.cos(t) * r)), int(round(pz + math.sin(t) * r))
            if park.water[x, z] or park.hek[x, z]:
                continue
            g = int(park.hoogte[x, z])
            if abs(g - py) > 1 or park.v[x, g + 1, z] or park.v[x, g + 2, z]:
                continue
            if not _vloer(park.get(x, g, z)):
                continue
            f = _facing(px - x, pz - z)
            park.set(x, g + 1, z, "guhs:safari_schemabord", facing=f)
            log(f"    s3: schemabord bij ({x},{g + 1},{z}) kijkt {f}")
            return [x, g + 1, z]
    return None


def sfeer(park, plekken_per_zone):
    uit = {}
    oert = getattr(park, "_oertijd", {}) or {}
    bronnen = []
    for b in oert.get("bronnen", []) or []:
        try:
            x, z = int(b[0]), int(b[1])
            bronnen.append([x + 0.5, WATER_Y + 1.0, z + 0.5] if len(b) < 3 else [x + 0.5, float(b[2]) + 1.0, z + 0.5])
        except (TypeError, ValueError, IndexError):
            pass
    if not bronnen:
        x, z, _, _ = kit.RESERVERINGEN["warme_bronnen"]
        bronnen = [[x + 0.5, float(park.hoogte[x, z]) + 1.0, z + 0.5]]
    uit["bronnen"] = bronnen
    plein = getattr(park, "plein", None)
    if plein:
        uit["fontein"] = [plein[0] + 0.5, plein[1] + 5.5, plein[2] + 0.5]
    uit["zones"] = plekken_per_zone
    return uit


def bouw(park, log=print):
    if getattr(park, "hek_afstand", None) is None:
        park.hek_afstand = distance_transform_edt(~(park.hek & ~park.water))
    s3 = {}
    s3["route"] = truckroute(park, log)
    s3["kano"] = kano(park, log)
    park._s3_kano = s3["kano"]
    s3["kabelbaan"] = kabelbaan(park, log)
    s3["boot"] = {"routes": [r for r in (boot(park, "delta", log), boot(park, "ijskust", log)) if r]}
    s3["race"] = racebaan(park, log)
    s3["schemabord"] = schemabord(park, log)
    park._s3 = s3
    park._s3_ververs = ververs_route


def plekken(park, plekken):
    """No homes change here; this only remembers, per zone, where the feeding spots are (the first home of the zone's
    feeding species) for sfeer.json / the moments."""
    per_zone = {}
    for p in plekken:
        per_zone.setdefault(p["zone"], []).append({"soort": p["soort"], "centrum": p["centrum"]})
    park._s3["sfeer"] = sfeer(park, {k: v[:24] for k, v in per_zone.items()})
