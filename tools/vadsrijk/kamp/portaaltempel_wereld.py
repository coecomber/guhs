"""The ruined order temple (Guhmension, rare) as the structure template data/guhs/structure/vadsrijk/wereld/portaaltempel.nbt.

python portaaltempel_wereld.py <out.nbt>
The render's portal glass becomes the real sleeping portal (guhs:vr_w_portaal[wakker=false]); the template starts 2 blocks
under the ground of the render (the jigsaw start_height is -3, so the ground layer lands on the surface).
"""
import os
import sys

import numpy as np

import portaaltempel as PT
import vrkit as K

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "wereld"))
import blokken as B  # noqa: E402
import nbtsnel  # noqa: E402


def main(uit):
    S = K.Scene(*PT.MAAT)
    PT.bouw(S)
    S.fix_connections()
    for i, s in enumerate(S.pal):
        naam = s.split("[")[0]
        if naam == "minecraft:tinted_glass":
            S.v[S.v == i] = 0
        elif naam in ("minecraft:purple_stained_glass", "minecraft:light_gray_stained_glass", "minecraft:white_stained_glass"):
            S.v[S.v == i] = S.id("guhs:vr_w_portaal[axis=x,wakker=false]")
    y0 = PT.G - 2
    v = S.v[:, y0:, :]
    xs, ys, zs = np.nonzero(v)
    x0, z0 = xs.min(), zs.min()
    v = v[x0:xs.max() + 1, :ys.max() + 1, z0:zs.max() + 1]
    P = B.Palet()
    lut = P.lut(S.pal)
    g = lut[v]
    xs, ys, zs = np.nonzero(g)
    ids = g[xs, ys, zs]
    gebruikt, inv = np.unique(ids, return_inverse=True)
    palette = [B.splits(P.lijst[i]) for i in gebruikt]
    mis = B.controleer(P)
    if mis:
        print("ONBEKENDE BLOKKEN:", mis)
    n = nbtsnel.schrijf(uit, list(v.shape), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32), inv.astype(np.int32))
    print("portaaltempel:", n, "blokken, maat", v.shape)


if __name__ == "__main__":
    main(sys.argv[1])
