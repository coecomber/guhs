"""
Op Safari! step 2, slice S2 (the lobby): where the lobby's characters stand and the lodge beds, on top of zone_entree.

Runs after zone_entree and zone_kriebelhuis (name order). It writes data/guhs/safari/lobby.json for the game
(feature/safari/lobby/LobbyPlekken):
  * "npcs": every guh character of the lobby: {id, kind, pos [x, y, z] (the block it stands in), yaw}, the eight zone experts
    with their zone; every spot is a free standing spot found near the spot the village plan meant (air for feet and head,
    something solid below);
  * "pennen": the four aaihoek pens {soort, min [x, z], max [x, z]}, "alpaca_lus": the alpaca's walk (points on the path);
  * "lodge": the box of the lodge huts (sleeping there = the vroege-vogel bonus; the beds are in zone_entree's scene),
    "plein": the square's middle + radius (the visitor guhs stay around it);
  * (round 4) "bus": the Guh-safaribus box and its bell (clicking either takes you back to the Guhmension), and the
    characters of park.lobby_npcs_extra (zone_kriebelhuis).
"""
import json
import math
import os

from . import kit
from . import zone_entree as E

STA = E.OY + E.G + 1          # world y of the feet on the village ground


def _wereld(x, z):
    return E.OX + x, E.OZ + z


def _vast(s):
    b = s.split("[")[0]
    return b != "minecraft:air" and not any(k in b for k in ("water", "lava", "fence", "pane", "bars", "door", "carpet",
                                                                 "slab", "stairs", "leaves", "sign", "lantern", "torch"))


def _vrij(park, x, y, z):
    return park.leeg(x, y, z) and park.leeg(x, y + 1, z) and _vast(park.get(x, y - 1, z))


def _plek(park, lx, lz, y=STA, r=4):
    """The free standing spot nearest to local (lx, lz) at feet height y (also one up or down)."""
    wx, wz = _wereld(lx, lz)
    beste = None
    for d in range(0, r + 1):
        for dx in range(-d, d + 1):
            for dz in range(-d, d + 1):
                if max(abs(dx), abs(dz)) != d:
                    continue
                for dy in (0, 1, -1):
                    if _vrij(park, wx + dx, y + dy, wz + dz):
                        k = dx * dx + dz * dz + abs(dy) * 4
                        if beste is None or k < beste[0]:
                            beste = (k, (wx + dx, y + dy, wz + dz))
        if beste:
            return list(beste[1])
    return [wx, y, wz]


def _yaw_naar(van, naar):
    """Minecraft yaw that looks from van to naar (x, z)."""
    dx, dz = naar[0] - van[0], naar[1] - van[1]
    return round(math.degrees(math.atan2(-dx, dz)), 1)


def bouw(park, log=print):
    info = E.INFO
    npcs = []

    def npc(id_, kind, plek, kijk, y=STA, **extra):
        p = _plek(park, *plek, y=y)
        npcs.append(dict(id=id_, kind=kind, pos=p, yaw=_yaw_naar((p[0], p[2]), _wereld(*kijk)), **extra))

    # (round 4, the new lobby) the characters at the spots of zone_entree's scene: (where, looking at)
    for id_, (plek, kijk) in info["NPC"].items():
        npc(id_, "safari_" + id_, plek, kijk)
    ijs = (info.get("J3") or {}).get("ijskraam")
    if ijs:
        npc("ijsverkoper", "safari_ijsverkoper", ijs["verkoper"], ijs["kijk"])
    # the nine experts in the expertenpaviljoen: each in front of its counter, looking into the pavilion
    pv = info["PAVILJOEN"]
    for zone, (bx, bz, b) in zip(E.EXPERT_ZONES, pv["balies"]):
        lx, lz = pv["x"] + math.cos(b) * (pv["r"] - 2.3), pv["z"] + math.sin(b) * (pv["r"] - 2.3)
        p = _plek(park, int(round(lx)), int(round(lz)), r=1)
        npcs.append(dict(id=f"expert_{zone}", kind=f"safari_expert_{zone}", pos=p,
                         yaw=_yaw_naar((p[0], p[2]), _wereld(pv["x"], pv["z"])), zone=zone,
                         balie=[E.OX + bx, STA, E.OZ + bz]))
    # (round 4) characters other modules add (zone_kriebelhuis: its counter and shop): {id, kind, pos, yaw}
    for extra in getattr(park, "lobby_npcs_extra", None) or []:
        npcs.append(dict(extra))

    (l1x, l1z), (l2x, l2z) = info["LODGE"]
    fy = E.OY + E.G + 3
    lodge_min, lodge_max = _wereld(l1x, l1z), _wereld(l2x, l2z)
    bedden = [[E.OX + x, E.OY + y, E.OZ + z] for (x, y, z) in info["BEDDEN"]]
    pennen = []
    for (x0, z0, soort) in E.PENS:
        a, b = _wereld(x0 + 1, z0 + 1), _wereld(x0 + 6, z0 + 7)
        pennen.append(dict(soort=soort, min=list(a), max=list(b)))
    bus = info["BUS"]
    data = {
        "npcs": npcs,
        "pennen": pennen,
        "alpaca_lus": [list(_wereld(x, z)) for (x, z) in info["ALPACA"]],
        "lodge": {"min": [lodge_min[0], fy - 4, lodge_min[1]], "max": [lodge_max[0], fy + 6, lodge_max[1]], "bedden": bedden},
        "plein": {"centrum": [E.OX + E.CX, STA, E.OZ + E.CZ], "straal": E.SQ_R + 6},
        "bus": {"min": E.wereld(bus["min"][0], bus["min"][2], y=bus["min"][1]),
                "max": E.wereld(bus["max"][0], bus["max"][2], y=bus["max"][1]),
                "bel": E.wereld(bus["bel"][0], bus["bel"][2], y=bus["bel"][1])},
    }
    pad = os.path.join(kit.OUT_DATA, "lobby.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    log(f"    lobby: {len(npcs)} characters, {len(bedden)} lodge beds -> {pad}")
