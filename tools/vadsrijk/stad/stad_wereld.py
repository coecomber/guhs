"""Zwamzoet as a map stamp: zwamzoet.bouw(S) with world tweaks, written with its offset + POIs to an .npz.

python stad_wereld.py <out.npz>
World = scene + (OX, OY, OZ): scene (128, 128) -> world (0, 0); ground G=8 -> y 63 (just above the bay at 62).
Tweaks against the concept render:
  - terrain: only the grass layer (the tile writer makes the ground below; the bay is the global sea);
  - harbour: no airship mast (the airship docks at De Haven, contract area `haven`);
  - lifts: real wolkenlift columns (up + down) instead of a chain;
  - the 10 ground houses -> shops with interiors (smidse, veilinghuis, bank, uitrusting, kruidenier, kleermaker,
    havenmeester) + 6 citizens' houses; questbord, campfire, Mika clues; the ordetempel gets an interior + side door.
"""
import json
import math
import sys

import numpy as np

import stadkit as SK
import zwamzoet as Z

OX, OY, OZ = -128, 55, -128
G = Z.G
POI = {}
DEUREN = {}
GEBOUWEN = {}


def w(x, y, z):
    return [int(x + OX), int(y + OY), int(z + OZ)]


# ------------------------------------------------------------------------------------------------ patches
def terrein(S):
    X, Y, Z_ = S.v.shape
    for x in range(X):
        kust = 216 + int(3 * math.sin(x * 0.05))
        S.v[x, G, :kust] = S.id("knuffelgras")
        if 30 < x < 236:
            for z in range(208, kust + 1):
                for y in range(1, G + 1):
                    S.set(x, y, z, "knuffelsteen" if y < G else Z.KLINK)
            S.set(x, G + 1, kust, Z.HEK if x % 6 else Z.LANT)


def haven(S):
    for px in (84, 160):
        for z in range(214, 246):
            S.fill(px - 1, G, z, px + 1, G, z, Z.PLANK)
            if z % 4 == 0:
                for dx in (-2, 2):
                    for y in range(1, G + 1):
                        S.set(px + dx, y, z, "minecraft:cherry_log[axis=y]")
            if z % 8 == 0:
                S.set(px + 2, G + 1, z, Z.LANT)
    for (bx, bz) in ((92, 228), (152, 236), (70, 240)):
        Z.bootje(S, bx, bz)
    Z.vuurtoren(S, 34, 234)


def lift(S, naam, hoek, zw):
    """Liftje: open shaft, an up column (wolkenlift + wolkenstroom) in the middle and a down column next to it."""
    cx, cz, T, r, rs, _, p, _ = zw[naam]
    a = math.radians(hoek)
    lx, lz = int(round(cx + (r * 0.84) * math.cos(a))), int(round(cz + (r * 0.84) * math.sin(a)))
    d = math.hypot(lx - cx, lz - cz)
    top, _ = Z.hoed_profiel(d, T, r, p)
    vx, vz = cx - lx, cz - lz
    naar = ("east" if vx > 0 else "west") if abs(vx) > abs(vz) else ("south" if vz > 0 else "north")
    ox, oz = {"east": (0, 1), "west": (0, 1), "south": (1, 0), "north": (1, 0)}[naar]
    for y in range(G + 1, top + 5):
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            S.set(lx + dx, y, lz + dz, Z.HEK if y % 12 else "roze_lakhout_planken")
        for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0), (0, 0)):
            if y <= top + 3:
                S.set(lx + dx, y, lz + dz, "minecraft:air")
    S.set(lx, G + 1, lz, f"wolkenlift[down=false,facing={naar}]")
    S.set(lx + ox, G + 1, lz + oz, f"wolkenlift[down=true,facing={naar}]")
    for y in range(G + 2, top + 3):
        S.set(lx, y, lz, f"wolkenstroom[down=false,facing={naar}]")
        S.set(lx + ox, y, lz + oz, f"wolkenstroom[down=true,facing={naar}]")
    # station: a plank floor with the two columns through it, a little roof
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if (dx, dz) not in ((0, 0), (ox, oz)):
                S.set(lx + dx, top, lz + dz, Z.PLANK)
    S.fill(lx - 2, top + 5, lz - 2, lx + 2, top + 5, lz + 2, "guh_dakpan_roze")
    S.set(lx - 2, top + 1, lz - 2, Z.LANT)
    for t in range(0, int(d - p * r) + 3):
        x, z = lx - math.cos(a) * t, lz - math.sin(a) * t
        if t > 2:
            S.fill(int(x) - 1, T, int(z) - 1, int(x) + 1, T, int(z) + 1, Z.PLANK)
    POI[f"lift_{naam}_voet"] = w(lx, G + 1, lz)
    POI[f"lift_{naam}_top"] = w(lx, top + 1, lz)


# ------------------------------------------------------------------------------------------------ shops
def meubel(S, soort, x0, z0, x1, z1, y, voor):
    """Interior per shop (floor y = first air layer). x0..x1/z0..z1 = the inside."""
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    # lights in every shop
    for (lx, lz) in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        S.set(lx, y, lz, "minecraft:lantern[hanging=false,waterlogged=false]")
    # the counter runs across the back half, opposite the door
    if voor in ("east", "west"):
        cx_ = x0 + 2 if voor == "east" else x1 - 2
        for z in range(z0 + 1, z1):
            if z != mz:
                S.set(cx_, y, z, "minecraft:cherry_slab[type=top,waterlogged=false]" if soort != "bank" else "minecraft:gold_block")
        achter = [(cx_ + (-1 if voor == "east" else 1), y, z) for z in range(z0, z1 + 1)]
    else:
        cz_ = z0 + 2 if voor == "south" else z1 - 2
        for x in range(x0 + 1, x1):
            if x != mx:
                S.set(x, y, cz_, "minecraft:cherry_slab[type=top,waterlogged=false]" if soort != "bank" else "minecraft:gold_block")
        achter = [(x, y, cz_ + (-1 if voor == "south" else 1)) for x in range(x0, x1 + 1)]
    spul = {
        "smidse": ["minecraft:anvil[facing=north]", "minecraft:blast_furnace[facing=south,lit=false]", "minecraft:smithing_table",
                   "minecraft:grindstone[face=floor,facing=north]", "minecraft:barrel[facing=up,open=false]"],
        "veilinghuis": ["minecraft:lectern[facing=south,has_book=false,powered=false]", "minecraft:barrel[facing=up,open=false]",
                        "minecraft:chest[facing=east,type=single,waterlogged=false]", "minecraft:cartography_table"],
        "bank": ["minecraft:chest[facing=east,type=single,waterlogged=false]", "minecraft:gold_block", "minecraft:iron_block",
                 "minecraft:barrel[facing=up,open=false]"],
        "uitrusting": ["minecraft:smithing_table", "minecraft:barrel[facing=up,open=false]", "minecraft:fletching_table",
                       "minecraft:target"],
        "kruidenier": ["minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]",
                       "minecraft:cauldron", "minecraft:barrel[facing=up,open=false]", "minecraft:composter[level=0]"],
        "kleermaker": ["minecraft:loom[facing=north]", "minecraft:pink_wool", "minecraft:white_wool", "minecraft:light_blue_wool",
                       "minecraft:barrel[facing=up,open=false]"],
        "havenmeester": ["minecraft:cartography_table", "minecraft:barrel[facing=up,open=false]",
                         "minecraft:chest[facing=east,type=single,waterlogged=false]"],
        "huis": ["minecraft:red_bed[facing=north,occupied=false,part=foot]", "minecraft:barrel[facing=up,open=false]",
                 "minecraft:flower_pot"],
    }[soort]
    for i, p in enumerate(achter[1:-1]):
        if i % 2 == 0:
            S.set(*p, spul[(i // 2) % len(spul)])
    if soort == "bank":
        for p in achter[1:-1]:
            S.set(p[0], p[1] + 1, p[2], "minecraft:iron_bars")
    return (mx, y, mz)


def winkel(S, soort, x0, z0, wd, dp, voor, h=6, pal=0):
    top = Z.huis(S, x0, G + 1, z0, wd, dp, h, voor, Z.PALETTEN[pal])
    x1, z1 = x0 + wd - 1, z0 + dp - 1
    mid = (x0 + x1) // 2 if voor in ("north", "south") else (z0 + z1) // 2
    deur = {"south": (mid, G + 1, z1 + 1), "north": (mid, G + 1, z0 - 1),
            "east": (x1 + 1, G + 1, mid), "west": (x0 - 1, G + 1, mid)}[voor]
    binnen = meubel(S, soort, x0 + 1, z0 + 1, x1 - 1, z1 - 1, G + 1, voor) if soort else None
    GEBOUWEN.setdefault(soort or "huis", []).append(dict(deur=w(*deur), binnen=w(*binnen) if binnen else None,
                                                        doos=[w(x0, G, z0), w(x1, top, z1)]))
    return deur, binnen


def grondhuisjes(S, rng):
    winkel(S, "smidse", 78, 171, 11, 11, "east", 7, 3)
    winkel(S, "veilinghuis", 162, 168, 15, 15, "west", 8, 1)
    winkel(S, "bank", 148, 136, 11, 9, "south", 6, 2)
    winkel(S, "uitrusting", 98, 136, 11, 9, "south", 6, 0)
    winkel(S, "kruidenier", 82, 192, 11, 9, "east", 6, 4)
    winkel(S, "kleermaker", 164, 192, 11, 9, "west", 6, 1)
    winkel(S, "havenmeester", 56, 196, 9, 9, "south", 5, 2)
    for i, (x, z, v) in enumerate(((36, 176, "east"), (212, 176, "west"), (60, 118, "south"), (188, 116, "south"),
                                    (106, 56, "south"), (142, 56, "south"))):
        if Z.vrij(S, x - 1, z - 1, x + 8, z + 8):
            deur, binnen = winkel(S, "huis", x, z, 7, 7, v, 5, i % 5)


# ------------------------------------------------------------------------------------------------ extras after bouw
def questbord(S, x, z):
    for k in range(4):
        S.set(x + k, G + 1, z, "minecraft:spruce_fence")
    S.fill(x, G + 2, z, x + 3, G + 4, z, "minecraft:spruce_planks")
    S.fill(x, G + 5, z, x + 3, G + 5, z, SK.stairs("guhs:guh_dakpan_roze_trap", "north"))
    S.set(x - 1, G + 5, z, "minecraft:lantern[hanging=false,waterlogged=false]")
    S.set(x + 4, G + 5, z, "minecraft:lantern[hanging=false,waterlogged=false]")
    S.fill(x, G + 2, z + 1, x + 3, G + 4, z + 1, "minecraft:air")


def ordetempel_binnen(S, info):
    cx, cz, T, r, _, _, p, _ = Z.ZWAMMEN["tempel"]
    yv = T + 1 + 3
    hx1, hx2, hz1, hz2 = cx - 13, cx + 13, cz - 11, cz + 8
    # lights + carpet + the class altar with three pedestals and a statue
    for x in range(hx1 + 3, hx2 - 2, 5):
        for z in range(hz1 + 3, hz2 - 1, 5):
            S.set(x, yv + 23, z, "minecraft:shroomlight")
            S.set(x, yv + 22, z, "minecraft:iron_chain[axis=y]")
            S.set(x, yv + 21, z, "minecraft:lantern[hanging=true,waterlogged=false]")
    for z in range(hz1 + 1, hz2 + 1):
        for x in (cx - 1, cx, cx + 1):
            S.set(x, yv - 1, z, "minecraft:pink_carpet" if False else "pastelbaksteen_framboos")
    for lx, kl, ding in ((-6, "minecraft:red_wool", "minecraft:anvil[facing=north]"),
                         (0, "minecraft:lime_wool", "minecraft:target"),
                         (6, "minecraft:purple_wool", "minecraft:enchanting_table")):
        S.set(cx + lx, yv, hz1 + 4, "roze_knuffelsteen")
        S.set(cx + lx, yv + 1, hz1 + 4, ding)
        S.set(cx + lx, yv, hz1 + 3, kl)
    Z.guhbeeld(S, cx, yv, hz1 + 1, 5)
    # a normal side door in the east wall (the big Knuffeldeur is for ceremonies)
    for y in (yv, yv + 1):
        S.set(hx2, y, cz - 2, "minecraft:air")
    S.set(hx2, yv, cz - 2, "roze_lakhout_deur[facing=east,half=lower,hinge=left,open=false]")
    S.set(hx2, yv + 1, cz - 2, "roze_lakhout_deur[facing=east,half=upper,hinge=left,open=false]")
    S.fill(hx2 + 1, yv - 1, cz - 4, hx2 + 4, yv - 1, cz, "tegel_hartjes")
    POI["ordetempel_binnen"] = w(cx, yv, cz - 2)
    POI["ordetempel_zijdeur"] = w(hx2 + 1, yv, cz - 2)
    POI["ordetempel_altaar"] = w(cx, yv, hz1 + 6)
    dx1, y0, fz1, dw, dh = info["deur"]
    DEUREN["ordetempel"] = dict(min=w(dx1, y0, fz1 + 1), max=w(dx1 + dw - 1, y0 + dh - 1, fz1 + 2), as_="x",
                                voor=w(cx, y0, fz1 + 5))


def main(uit):
    Z.terrein, Z.haven, Z.lift, Z.grondhuisjes = terrein, haven, lift, grondhuisjes
    Z.KLEIN[0] = (70, 128, 30, 8, "rood")
    S = SK.Scene(*Z.MAAT)
    info = Z.bouw(S)
    ordetempel_binnen(S, info)
    px, pz, pr = Z.PLEIN
    questbord(S, px - 9, pz - pr + 2)
    POI["questbord"] = w(px - 7, G + 1, pz - pr + 3)
    S.set(px - 14, G + 1, pz + 8, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    POI["kampvuur_zwamzoet"] = w(px - 14, G + 1, pz + 8)
    POI["plein"] = w(px, G + 1, pz)
    POI["guhpoort"] = w(px, G + 1, 210)
    # Mika clue 1: black fluff under the west ring stall
    S.set(100, G + 1, 178, "minecraft:black_carpet")
    POI["mika_aanwijzing_1"] = w(100, G + 1, 178)
    POI["mika_spion_kraam"] = w(100, G + 1, 180)
    # Mika clue 5: the spy's lookout on the east cap edge
    cx, cz, T, r, _, _, p, _ = Z.ZWAMMEN["oost"]
    ux = int(cx + p * r - 2)
    S.set(ux, T + 1, cz, "minecraft:black_carpet")
    S.set(ux - 1, T + 1, cz, "mika_steen")
    S.set(ux - 1, T + 1, cz + 1, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    POI["mika_aanwijzing_5"] = w(ux, T + 1, cz)
    for naam, z_ in Z.ZWAMMEN.items():
        POI[f"hoed_{naam}"] = w(z_[0], z_[2] + 1, z_[1])
    S.fix_connections()
    mis = Z.check(S)
    np.savez_compressed(uit, v=S.v, pal=np.array(S.pal), off=np.array([OX, OY, OZ]))
    with open(uit.replace(".npz", ".json"), "w", encoding="utf-8") as f:
        json.dump(dict(poi=POI, deuren=DEUREN, gebouwen=GEBOUWEN, ontbreekt=mis), f, indent=1)
    print("stad:", int((S.v > 0).sum()), "blokken")


if __name__ == "__main__":
    main(sys.argv[1])
