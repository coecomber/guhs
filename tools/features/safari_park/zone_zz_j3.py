"""
Op Safari! test round 3, slice J3 (lobby & beleving): writes data/guhs/safari/beleving.json for the game
(feature/safari/lobby/Beleving), after every other zone module (name order: it reads what they built).

  * "speeltuin": the guh-speeltuin of the lobby (zone_entree): its box, entrance and the toys the game places (the
    multi-blocks with a block entity and the knabbelbal, so not in the tiles): {soort, pos [x, y, z], facing};
  * "kampvuur": the campfire by the lodge, the storyteller's spot and the way he looks;
  * "parade_dorp" (and parade.json "dorp"): the penguin parade's walk through the lobby (from the oostpoort along the
    foot path, round the fountain and back); the IJskust part of the parade follows the truck route (the game reads it
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
    """(round 4) The walk of zone_entree's scene: from the oostpoort along the foot path to the square, once round the
    fountain and back (local points, feet on the ground)."""
    return [_wereld(x, z) for (x, z) in info.get("PARADE") or []]


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
    # (round 4) the village walk of the pinguinparade, its own file (momenten/ParadeRoute reads it first)
    ppad = os.path.join(kit.OUT_DATA, "parade.json")
    os.makedirs(os.path.dirname(ppad), exist_ok=True)
    with open(ppad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"dorp": data["parade_dorp"]}, f, indent=1, ensure_ascii=False)
        f.write("\n")
    data["kijkstoelen"] = _kijkstoelen(park)
    pad = os.path.join(kit.OUT_DATA, "beleving.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    log(f"    beleving: speeltuin {'ja' if s else 'NEE'}, kampvuur {'ja' if k else 'NEE'}, parade {len(data['parade_dorp'])} punten, "
        f"{len(data['kijkstoelen'])} kijkstoelen -> {pad}")
