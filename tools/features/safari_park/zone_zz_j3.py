"""
Op Safari! test round 3, slice J3 (lobby & beleving): writes data/guhs/safari/beleving.json for the game
(feature/safari/lobby/Beleving), after every other zone module (name order: it reads what they built).

  * "speeltuin": the guh-speeltuin of the village (zone_entree): its box, entrance and the three toys the game places
    (they are multi-blocks with a block entity, so not in the tiles): {soort, pos [x, y, z], facing};
  * "kampvuur": the campfire by the lodge, the storyteller's spot and the way he looks;
  * "parade_dorp": the penguin parade's walk through the village (from the park gate in the north, past the truck
    station, round the fountain and back); the IJskust part of the parade follows the truck route (the game reads it
    from route.json, so it stays right when the route changes);
  * "kijkstoelen": the guh chairs of the lookouts outside the village (one per lookout: chairs closer than 7 blocks
    to an earlier one belong to the same lookout), with the way the chair looks and its zone. The game uses these for
    the kijk-guhs when data/guhs/safari/kijkplekken.json (the river pipeline's viewpoints) is missing.
"""
import json
import math
import os

import numpy as np

from . import kit
from . import terrein
from . import zone_entree as E


def _wereld(x, z, y=None):
    return [E.OX + int(round(x)), E.OY + (E.G + 1 if y is None else y), E.OZ + int(round(z))]


def _parade(info):
    kruising = sorted(info.get("KRUISING") or [], key=lambda p: p[1])
    if not kruising:
        return []
    kx, kz = kruising[0]
    if kz < 20 and kx < 92:
        heen = [(kx, kz + 1), (kx + 1, kz + 4), (85, 13), (88, 19), (90, 27), (91, 34), (93, 39)]
    else:
        heen = [(kx, kz + 1), ((kx + 98) / 2, kz + 3), (98, 39)]
    heen += [(88, 47), (82, 49), (E.CX + 11, E.CZ - 10)]
    start = math.atan2(heen[-1][1] - E.CZ, heen[-1][0] - E.CX)
    rond = [(E.CX + math.cos(start + k * math.pi / 4) * 9.5, E.CZ + math.sin(start + k * math.pi / 4) * 9.5) for k in range(1, 9)]
    route = heen + rond + list(reversed(heen))
    return [_wereld(x, z) for (x, z) in route]


def _kijkstoelen(park):
    ids = [i for i, s in enumerate(park.pal) if s.startswith(kit.STOEL)]
    if not ids:
        return []
    xs, ys, zs = np.nonzero(np.isin(park.v, ids))
    uit = []
    for x, y, z in sorted(zip(xs.tolist(), ys.tolist(), zs.tolist())):
        if not park.binnen[x, z] or park.zone_vol[x, z] == 0:          # the village's own chairs (truck stop, bus)
            continue
        if any(abs(x - s[0]) + abs(z - s[2]) + abs(y - s[1]) < 7 for s in (u["pos"] for u in uit)):
            continue
        props = kit.split_state(park.pal[park.v[x, y, z]])[1]
        uit.append({"pos": [x, y, z], "facing": props.get("facing", "north"),
                    "zone": terrein.ZONES[int(park.zone_vol[x, z])]})
    return uit


def bouw(park, log=print):
    info = E.INFO
    j3 = info.get("J3") or {}
    data = {}
    s = j3.get("speeltuin")
    if s:
        data["speeltuin"] = {
            "centrum": _wereld(*s["centrum"]), "min": _wereld(*s["min"])[0::2], "max": _wereld(*s["max"])[0::2],
            "ingang": _wereld(*s["ingang"]),
            "speelgoed": [{"soort": t["soort"], "pos": _wereld(t["pos"][0], t["pos"][2], t["pos"][1]), "facing": t["facing"]}
                          for t in s["speelgoed"]],
        }
    k = j3.get("kampvuur")
    if k:
        v = _wereld(*k["verteller"])
        c = _wereld(*k["kijk"])
        data["kampvuur"] = {"centrum": _wereld(*k["centrum"]), "verteller": v,
                            "yaw": round(math.degrees(math.atan2(-(c[0] - v[0]), c[2] - v[2])), 1)}
    data["parade_dorp"] = _parade(info)
    data["kijkstoelen"] = _kijkstoelen(park)
    pad = os.path.join(kit.OUT_DATA, "beleving.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    log(f"    beleving: speeltuin {'ja' if s else 'NEE'}, kampvuur {'ja' if k else 'NEE'}, parade {len(data['parade_dorp'])} punten, "
        f"{len(data['kijkstoelen'])} kijkstoelen -> {pad}")
