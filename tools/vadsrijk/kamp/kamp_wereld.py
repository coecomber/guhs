"""Knuffelkamp as a map stamp: builds kamp.bouw(S) and writes it (with its world offset + POIs) to an .npz for the tile writer.

python kamp_wereld.py <out.npz>
World = scene + (OX, OY, OZ): scene sea level ZEE=10 -> y 62, camp floor VLOER=34 -> y 86.
"""
import json
import math
import sys

import numpy as np

import kamp as KP
import vrkit as K
from vrkit import stairs, slab

OX, OY, OZ = -150, 52, 865


def w(x, y, z):
    return [int(x + OX), int(y + OY), int(z + OZ)]


def main(uit):
    S = K.Scene(*KP.KAMP_MAAT)
    I = KP.bouw(S)
    poi = {}
    # campfires: unlit (kern checkpoints)
    for i, s in enumerate(S.pal):
        if s.startswith("minecraft:campfire"):
            S.pal[i] = s.replace("lit=true", "lit=false")
            S.idx[S.pal[i]] = i
    cx, cz = KP.KAMP
    poi["kampvuur_kamp"] = w(cx, KP.VLOER + 1, cz)
    # lighthouse: a campfire at the keeper's house + the secret cellar under the tower
    lx, lz = KP.LICHT
    ly = I.h(lx, lz)
    hx, hz = lx + 9, lz + 2
    fx, fz = hx + 2, hz + 7
    S.set(fx, I.h(fx, fz) + 1, fz, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    poi["kampvuur_vuurtoren"] = w(fx, I.h(fx, fz) + 1, fz)
    # cellar: 5x4x5 room under the tower foot, reached by a trapdoor in the tower floor
    S.fill(lx - 4, ly - 7, lz - 4, lx + 3, ly - 2, lz + 3, "minecraft:mossy_stone_bricks")
    for x in range(lx - 3, lx + 3):
        for z in range(lz - 3, lz + 3):
            for y in range(ly - 6, ly - 2):
                S.set(x, y, z, "minecraft:cave_air")
            S.set(x, ly - 7, z, "minecraft:spruce_planks")
            S.set(x, ly - 2, z, "minecraft:spruce_planks")
    S.set(lx + 1, ly - 2, lz + 1, "minecraft:spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
    S.set(lx + 1, ly - 1, lz + 1, "minecraft:cave_air")
    S.set(lx + 1, ly, lz + 1, "minecraft:spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]")
    for y in range(ly - 6, ly - 2):
        S.set(lx + 2, y, lz + 1, "minecraft:ladder[facing=west,waterlogged=false]")
    S.set(lx - 2, ly - 6, lz - 2, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    S.set(lx - 3, ly - 5, lz - 3, "minecraft:lantern[hanging=false,waterlogged=false]")
    poi["geheim_vuurtorenkelder"] = w(lx - 2, ly - 6, lz - 2)
    poi["vuurtoren_top"] = w(lx, ly + 32, lz)
    # dock + airship
    z0 = KP.STEIGER_Z
    kx = max(x for x in range(I.X) if I.H[x, z0] >= 0)
    x_eind = kx + 30
    poi["aankomst"] = w(kx - 4, KP.VLOER + 1, z0)
    poi["steiger_eind"] = w(x_eind - 2, KP.VLOER + 1, z0)
    S.set(x_eind - 4, KP.VLOER + 1, z0 - 7, "minecraft:bell[attachment=floor,facing=east,powered=false]")
    poi["luchtschip_bel"] = w(x_eind - 4, KP.VLOER + 1, z0 - 7)
    poi["luchtschip_kapitein"] = w(x_eind - 4, KP.VLOER + 1, z0 - 5)
    poi["ordetent"] = w(*(KP.ORDETENT[0], KP.VLOER + 1, KP.ORDETENT[1]))
    poi["training"] = w(KP.TRAINING[0], KP.VLOER + 1, KP.TRAINING[1])
    tx, tz = KP.TEMPEL
    poi["tempeltje"] = w(tx, I.h(tx, tz) + 3, tz)   # (integration: stand ON the plinth, not in its hollow)
    vx, vz = KP.VIJVER
    poi["vijver"] = w(vx, KP.VLOER + 1, vz)
    # sea: drop the scene's own water + sea floor (the global sea goes there); keep the dock posts
    zee = {S.idx.get(b) for b in ("minecraft:water", "minecraft:sand", "minecraft:prismarine", "minecraft:light_blue_terracotta")}
    zee.discard(None)
    zeekol = I.H < 0
    for y in range(0, KP.ZEE + 1):
        laag = S.v[:, y, :]
        m = zeekol & np.isin(laag, list(zee))
        laag[m] = 0
    S.fix_connections()
    hoogte = np.where(I.H >= 0, I.H + OY, -1)
    np.savez_compressed(uit, v=S.v, pal=np.array(S.pal), off=np.array([OX, OY, OZ]), hoogte=hoogte,
                        guhs=np.array([[g[0] + OX, g[1] + OY, g[2] + OZ, g[3]] for g in S.guhs], float))
    with open(uit.replace(".npz", ".json"), "w", encoding="utf-8") as f:
        json.dump(poi, f, indent=1)
    print("kamp:", int((S.v > 0).sum()), "blokken", poi)


if __name__ == "__main__":
    main(sys.argv[1])
