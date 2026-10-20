"""
Op Safari! slice S4 (quests & extra's): what the quests need in the park. Runs after every other zone module ("zz").

  bouw(park, log)       Dikkie's stal (de te vahe olifant): a small pen near the savanne drinkpoel with a gate that is far
                        too narrow for an elephant, a shelter, a sign, and the olifantenloopband next to it
  plekken(park, ...)    the geheimpjes: one spot per zone, found in what the zone modules built (a chest, a barrel, a pot, the
                        fossil's skull) -> data/guhs/safari/geheimpjes.json; the pootafdrukken: footprint trails from the
                        nearest path to the homes of the hiders; quests.json (the stal, the loopband)

The game (feature/safari/quests) reads geheimpjes.json and quests.json; the JSON is written relative to the working
directory (the generator always runs from the project root).
"""
import json
import math
import os

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES
from .zone_savanne import SPOTS, bord

# the hiders that get a footprint trail (design 1.6: verstopper + klein territorium)
SPOOR = ("okapi", "das", "bever", "quokka", "vuursalamander", "sabeltandtijger")
STAL = 9            # the pen's outer size (fence ring)
# zones with more than one candidate block: the geheimpje is the one nearest to this reservation (kit.RESERVERINGEN)
ANKER = {"europa": "holle_eik", "regenwoud": "ruine"}

_STAND = {}         # what bouw found, for plekken / quests.json


def _schrijf(naam, data):
    pad = os.path.join(kit.OUT_DATA, naam)
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")


# =====================================================================================================================
# Dikkie's stal
# =====================================================================================================================
def _stal_plek(park):
    """A dry, flat-ish 13x13 spot in the savanne, 6..30 from the poel's rim, away from paths, fences, water and trees."""
    if "poel" not in SPOTS:
        return None
    px_, _, pz = SPOTS["poel"]
    rx, rz = SPOTS.get("poel_r", (10, 8))
    sav = ZONES.index("savanne")
    hek_d = distance_transform_edt(~park.hek)
    pad_d = distance_transform_edt(~park.pad)
    water_d = distance_transform_edt(~park.water)
    bomen = np.zeros(park.hoogte.shape, bool)
    for (x, _, z, _s) in park.bomen:
        if 0 <= x < kit.W and 0 <= z < kit.W:
            bomen[x, z] = True
    boom_d = distance_transform_edt(~bomen)
    beste = None
    for r in range(int(max(rx, rz)) + 8, int(max(rx, rz)) + 34, 2):
        for k in range(48):
            a = k / 48 * 2 * math.pi
            x, z = int(px_ + math.cos(a) * r), int(pz + math.sin(a) * r)
            if not (8 <= x < kit.W - 8 and 8 <= z < kit.W - 8):
                continue
            box = (slice(x - 6, x + 7), slice(z - 6, z + 7))
            if not (park.zone_vol[box] == sav).all():
                continue
            if hek_d[x, z] < 12 or pad_d[x, z] < 5 or pad_d[x, z] > 18 or water_d[x, z] < 8 or boom_d[x, z] < 6:
                continue
            h = park.hoogte[box]
            if int(h.max()) - int(h.min()) > 2 or park.water[box].any() or park.pad[box].any():
                continue
            score = pad_d[x, z] + abs(r - (max(rx, rz) + 12)) * 0.3
            if beste is None or score < beste[0]:
                beste = (score, x, z)
        if beste is not None:
            break
    return None if beste is None else (beste[1], beste[2])


def _stal(park, log):
    plek = _stal_plek(park)
    if plek is None:
        log("    s4: no spot for Dikkie's stal (no savanne poel?)")
        return
    cx, cz = plek
    g = int(np.median(park.hoogte[cx - 6:cx + 7, cz - 6:cz + 7]))
    grond = park.get(cx, int(park.hoogte[cx, cz]), cz)
    # flatten 13x13 to g (keep the savanne's own top block)
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            h = int(park.hoogte[x, z])
            for y in range(min(h, g) - 3, g):
                if park.leeg(x, y, z):
                    park.set(x, y, z, "minecraft:dirt")
            park.set(x, g, z, grond if "grass" in grond or "sand" in grond or "dirt" in grond else "minecraft:coarse_dirt")
            for y in range(g + 1, g + 12):
                park.set(x, y, z, "minecraft:air")
            park.hoogte[x, z] = g
    h = STAL // 2
    # the pen floor: hay and coarse dirt
    for x in range(cx - h + 1, cx + h):
        for z in range(cz - h + 1, cz + h):
            park.set(x, g, z, "minecraft:coarse_dirt" if (x + z) % 3 else "minecraft:rooted_dirt")
    # a fence ring, two high (the elephant stays in anyway: its home), the gate on the south side: ONE wide
    for x in range(cx - h, cx + h + 1):
        for z in (cz - h, cz + h):
            for y in (g + 1, g + 2):
                park.set(x, y, z, "minecraft:acacia_fence")
    for z in range(cz - h, cz + h + 1):
        for x in (cx - h, cx + h):
            for y in (g + 1, g + 2):
                park.set(x, y, z, "minecraft:acacia_fence")
    poort = (cx, g + 1, cz + h)
    park.set(cx, g + 1, cz + h, "minecraft:acacia_fence_gate", facing="south", open="false", in_wall="false", powered="false")
    park.set(cx, g + 2, cz + h, "minecraft:air")
    for x in (cx - 1, cx + 1):          # two posts beside the narrow gate (stripped logs): it looks REALLY narrow
        for y in (g + 1, g + 2, g + 3):
            park.set(x, y, cz + h, "minecraft:stripped_acacia_log", axis="y")
    park.set(cx, g + 3, cz + h, "minecraft:stripped_acacia_log", axis="x")
    # the shelter at the back (north half): acacia posts and a slab roof with hay on top
    for x in (cx - h + 1, cx + h - 1):
        for y in range(g + 1, g + 5):
            park.set(x, y, cz - h + 1, "minecraft:acacia_log", axis="y")
            park.set(x, y, cz - 1, "minecraft:acacia_log", axis="y")
    for x in range(cx - h, cx + h + 1):
        for z in range(cz - h, cz):
            park.set(x, g + 5, z, "minecraft:acacia_slab", type="bottom", waterlogged="false")
    for x in range(cx - h + 1, cx + h):
        for z in range(cz - h + 1, cz - 1):
            park.set(x, g + 6, z, "minecraft:hay_block", axis="y")
    park.set(cx - 2, g + 1, cz - h + 1, "minecraft:hay_block", axis="y")
    park.set(cx + 2, g + 1, cz - h + 1, "minecraft:hay_block", axis="y")
    park.set(cx + 3, g + 1, cz - h + 2, "minecraft:water_cauldron", level="3")
    # the olifantenloopband outside, to the right of the gate, and the sign to the left
    loopband = (cx + 3, g + 1, cz + h + 2)
    park.set(*loopband, "guhs:olifantenloopband", facing="north")
    bord(park, cx - 3, g + 1, cz + h + 2, ["Hier woont", "Dikkie", "(hij past niet meer", "door het poortje)"], rotation=0)
    _STAND["olifant"] = {"stal": [cx, g + 1, cz], "poort": list(poort), "loopband": list(loopband)}
    log(f"    s4: Dikkie's stal at ({cx}, {g + 1}, {cz}), loopband {loopband}")


def bouw(park, log):
    _STAND.clear()
    _stal(park, log)


# =====================================================================================================================
# the geheimpjes and the footprint trails
# =====================================================================================================================
def _posities(park, naam, **eigen):
    """Every position of a block (by name, with these properties) in the park."""
    ids = []
    for i, s in enumerate(park.pal):
        n, props = kit.split_state(s)
        if n == naam and all(props.get(k) == v for k, v in eigen.items()):
            ids.append(i)
    if not ids:
        return []
    return [tuple(int(c) for c in p) for p in np.argwhere(np.isin(park.v, ids))]


def _geheimpjes(park, log):
    zone_van = lambda p: ZONES[int(park.zone_vol[p[0], p[2]])] if park.zone_vol[p[0], p[2]] >= 0 else "?"   # noqa: E731
    kandidaten = {z: [] for z in ZONES}
    for p in getattr(park, "geheimpjes", []):
        kandidaten.setdefault(zone_van(p), []).append(tuple(p))
    for p in _posities(park, "minecraft:decorated_pot", cracked="true"):
        if zone_van(p) in ("regenwoud", "bamboe"):
            kandidaten[zone_van(p)].append(p)
    for p in _posities(park, "minecraft:barrel", facing="up"):
        if zone_van(p) == "ijskust" and p[1] <= kit.WATER_Y:
            kandidaten["ijskust"].append(p)
    for p in _posities(park, "minecraft:skeleton_skull"):
        if zone_van(p) == "oertijd":
            kandidaten["oertijd"].append(p)
    for p in _posities(park, "minecraft:chest"):
        if zone_van(p) in ("europa", "australie"):
            kandidaten[zone_van(p)].append(p)
    uit = []
    for zone in ZONES:
        if zone == "entree":
            continue
        lijst = sorted(set(kandidaten.get(zone, [])))
        if zone in ANKER and len(lijst) > 1:             # e.g. the ranger chest is the one in the holle eik
            ex, ez, _, _ = kit.RESERVERINGEN[ANKER[zone]]
            lijst.sort(key=lambda p: math.hypot(p[0] - ex, p[2] - ez))
        if not lijst:
            raise SystemExit(f"safari s4: no geheimpje found in zone {zone}")
        if len(lijst) > 1 and zone not in ANKER:
            log(f"    s4: {len(lijst)} geheimpje candidates in {zone}, taking the first {lijst[0]}")
        uit.append({"id": zone, "zone": zone, "pos": list(lijst[0])})
    return uit


def _sporen(park, plekken, log):
    """Footprint trails (every 2-3 blocks) from the nearest path to the home of every hider, stopping near the home."""
    d, (ix, iz) = distance_transform_edt(~park.pad, return_indices=True)
    rng = np.random.default_rng(kit.SEED + 404)
    totaal = 0
    for p in plekken:
        if p["soort"] not in SPOOR:
            continue
        hx, _, hz = p["centrum"]
        if not (0 <= hx < kit.W and 0 <= hz < kit.W):
            continue
        sx, sz = int(ix[hx, hz]), int(iz[hx, hz])
        lengte = math.hypot(sx - hx, sz - hz)
        if lengte > 45:
            f = 45 / lengte
            sx, sz = int(hx + (sx - hx) * f), int(hz + (sz - hz) * f)
            lengte = 45
        if lengte < 6:
            continue
        zone = park.zone_vol[hx, hz]
        dx, dz = (hx - sx) / lengte, (hz - sz) / lengte
        facing = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
        t = 2.0
        while t < lengte - 3:
            x = int(round(sx + dx * t + rng.normal(0, 0.4)))
            z = int(round(sz + dz * t + rng.normal(0, 0.4)))
            t += 2.0 + rng.random()
            if not (0 <= x < kit.W and 0 <= z < kit.W) or park.pad[x, z] or park.water[x, z] or park.zone_vol[x, z] != zone:
                continue
            y = int(park.hoogte[x, z])
            onder = park.get(x, y, z)
            if "water" in onder or "leaves" in onder or "glass" in onder or "fence" in onder or not park.leeg(x, y + 1, z):
                continue
            park.set(x, y + 1, z, "guhs:pootafdruk", facing=facing)
            totaal += 1
    log(f"    s4: {totaal} pootafdrukken on the trails to {', '.join(SPOOR)}")


def plekken(park, plekken_):
    log = print
    geheim = _geheimpjes(park, log)
    _schrijf("geheimpjes.json", {"geheimpjes": geheim,
                                 "uitleg": "per zone het geheimpje (S4): rechtsklik op dit blok in het safaripark"})
    log("    s4: geheimpjes " + ", ".join(f"{g['zone']} {tuple(g['pos'])}" for g in geheim))
    _sporen(park, plekken_, log)
    _schrijf("quests.json", {**_STAND, "uitleg": "S4: Dikkie's stal (de te vahe olifant) en de olifantenloopband"})
