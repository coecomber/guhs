"""
Op Safari! step 2, slice S2 (the lobby): where the lobby's characters stand and the lodge beds, on top of zone_entree.

Runs after zone_entree (name order). It only adds a few blocks (a bed in every lodge hut) and writes
data/guhs/safari/lobby.json for the game (feature/safari/lobby/LobbyPlekken):
  * "npcs": every guh character of the lobby: {id, kind, pos [x, y, z] (the block it stands in), yaw}, the eight zone experts
    with their zone; every spot is a free standing spot found near the spot the village plan meant (air for feet and head,
    something solid below);
  * "pennen": the four aaihoek pens {soort, min [x, z], max [x, z]}, "alpaca_lus": the alpaca's walk (points on the path);
  * "lodge": the box of the lodge huts (sleeping there = the vroege-vogel bonus), "plein": the square's middle + radius
    (the visitor guhs stay around it).
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
    npcs = []

    def npc(id_, kind, lx, lz, yaw=None, kijk=None, y=STA, **extra):
        p = _plek(park, lx, lz, y)
        if kijk is not None:
            yaw = _yaw_naar((p[0], p[2]), _wereld(*kijk))
        npcs.append(dict(id=id_, kind=kind, pos=p, yaw=yaw if yaw is not None else 0.0, **extra))

    # the giftshop seller behind the door (giftshop GX 26..44, door in the east wall at z 58)
    npc("verkoper", "safari_verkoper", 41, 58, kijk=(46, 58))
    # the hoofdranger inside the rangerstation (26..44 x 33..46, door east at z 39), looking at the door
    npc("hoofdranger", "safari_hoofdranger", 38, 42, kijk=(46, 39))
    # the fotograaf on the square, facing the park gate (south)
    npc("fotograaf", "safari_fotograaf", E.CX - 7, E.CZ + 7, yaw=0.0)
    # the dierenverzorger at the barn door (barn 104..116 x 58..70, door west at z 65)
    npc("verzorger", "safari_verzorger", 101, 64, kijk=(94, 60))
    # the lodgebeheerder where the deck to the huts begins
    npc("lodgebeheerder", "safari_lodgebeheerder", 50, 77, kijk=(44, 82))
    # the eight zone experts behind their counters in the expertenpaviljoen (same geometry as zone_entree)
    zones = ["savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie"]
    pvx, pvz, pvr = 54, 37, 5.5
    for k, zone in enumerate(zones):
        a = k / 8 * 2 * math.pi + math.pi / 8
        b = a + math.pi / 8
        lx, lz = pvx + math.cos(b) * (pvr - 1.9), pvz + math.sin(b) * (pvr - 1.9)
        p = _plek(park, int(round(lx)), int(round(lz)), r=1)
        balie = _wereld(int(round(pvx + math.cos(b) * (pvr - 0.6))), int(round(pvz + math.sin(b) * (pvr - 0.6))))
        npcs.append(dict(id=f"expert_{zone}", kind=f"safari_expert_{zone}", pos=p,
                         yaw=_yaw_naar((p[0], p[2]), _wereld(pvx, pvz)), zone=zone,
                         balie=[balie[0], STA, balie[1]]))

    # a bed in every lodge hut (along the west wall, head to the north), so sleeping there is possible
    fy = E.OY + E.G + 3
    bedden = []
    for (hx, hz, _) in [(20, 82, "x"), (30, 90, "z"), (20, 92, "x"), (42, 84, "z"), (40, 94, "x")]:
        x, z = E.OX + hx + 1, E.OZ + hz + 1
        if park.leeg(x, fy + 1, z) and park.leeg(x, fy + 1, z + 1):
            park.set(x, fy + 1, z, "minecraft:yellow_bed[facing=north,occupied=false,part=head]")
            park.set(x, fy + 1, z + 1, "minecraft:yellow_bed[facing=north,occupied=false,part=foot]")
            bedden.append([x, fy + 1, z])
    lodge_min = _wereld(18, 80)
    lodge_max = _wereld(49, 101)

    pennen = []
    for (x0, z0, soort) in E.PENS:
        a, b = _wereld(x0, z0), _wereld(x0 + 5, z0 + 6)
        pennen.append(dict(soort=soort, min=list(a), max=list(b)))

    data = {
        "npcs": npcs,
        "pennen": pennen,
        "alpaca_lus": [list(_wereld(x, z)) for (x, z) in [(120, 60), (126, 59), (130, 64), (127, 72), (121, 74), (119, 67)]],
        "lodge": {"min": [lodge_min[0], fy - 4, lodge_min[1]], "max": [lodge_max[0], fy + 6, lodge_max[1]], "bedden": bedden},
        "plein": {"centrum": [E.OX + E.CX, STA, E.OZ + E.CZ], "straal": E.SQ_R + 6},
    }
    pad = os.path.join(kit.OUT_DATA, "lobby.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    log(f"    lobby: {len(npcs)} characters, {len(bedden)} lodge beds -> {pad}")
