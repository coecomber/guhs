"""Design renders of the Ambachtseiland next to the Knuffelkamp.

python ambacht_render.py <ss> <out_dir> [overzicht,brug,plein,kaart]
Builds the Knuffelkamp (kamp.bouw, unchanged) in its own scene, pastes it into a wider scene that extends WEST by PAD
columns, then builds ambachtseiland.bouw in that wider scene. Big-scene column x = world x - (KAMP_OX - PAD).
"""
import os
import sys
import time

import numpy as np

try:
    import psutil
    psutil.Process().nice(psutil.BELOW_NORMAL_PRIORITY_CLASS if os.name == "nt" else 10)
except Exception:
    pass

import kamp as KP
import vrkit as K
import erfkit as EK
import ambachtseiland as A

KAMP_OX, KAMP_OZ = -534, 865          # kamp scene (0,0) in the world (kamp_wereld: -150 + IND.KAMP_DX, 865)
PAD = 170
OX, OZ = KAMP_OX - PAD, KAMP_OZ


def bouw():
    t = time.time()
    S0 = K.Scene(*KP.KAMP_MAAT)
    I0 = KP.bouw(S0)
    X0, Y0, Z0 = S0.v.shape
    S = K.Scene(X0 + PAD, Y0, Z0)
    S.pal, S.idx = list(S0.pal), dict(S0.idx)
    S.v[PAD:, :, :] = S0.v
    S.guhs = [(x + PAD, y, z, a, b, c) for (x, y, z, a, b, c) in S0.guhs]
    S.roof = {(x + PAD, y, z) for (x, y, z) in S0.roof}
    # open sea west of the camp scene
    S.v[:PAD, KP.ZEE - 1, :] = S.id("minecraft:light_blue_terracotta")
    S.v[:PAD, KP.ZEE, :] = S.id("minecraft:water")
    kH = np.full((X0 + PAD, Z0), -1, int)
    kH[PAD:, :] = I0.H
    print("kamp klaar %.0fs" % (time.time() - t), flush=True)
    C = A.bouw(S, OX, OZ, kH)
    print("eiland klaar %.0fs, ertsen %d" % (time.time() - t, C.n_ertsen), flush=True)
    return S, C


def npc_extra(C, x1=0, z1=0, y1=0, x2=10 ** 6, z2=10 ** 6):
    ex = []
    for (x, y, z, yaw, soort) in C.npcs:
        if not (x1 <= x < x2 and z1 <= z < z2):
            continue
        ex += EK.dier(soort, x - x1, y - y1, z - z1, yaw, scale=0.8)
    return ex


def crop(S, C, x1, z1, x2, z2, bodem=KP.ZEE - 3):
    N = K.uitsnede(S, x1, z1, x2, z2)
    KP.sokkel(N, bodem)
    return N


def info(C):
    w = C.w
    out = ["WORLD coords (x, z); y = scene y + 52"]
    for k, v in C.labels.items():
        out.append("%-28s %s  y=%d" % (k, w(v[0], v[2]), v[1] + A.OY))
    for k, (x1, z1, x2, z2) in C.zone.items():
        out.append("zone %-23s %s .. %s" % (k, w(x1, z1), w(x2, z2)))
    xi, xk, yd = C.brug_eind
    out.append("brug: x %d .. %d at z %d, deck y %d (len %d)" % (w(xi, 0)[0], w(xk, 0)[0], A.BRUG_Z, yd + A.OY, xk - xi))
    sx, sz, se = C.steiger
    out.append("vissteiger: x %d .. %d at z %d" % (w(se, 0)[0], w(sx, 0)[0], A.STEIGER_Z))
    I = C.I
    land = np.argwhere(I.H >= 0)
    out.append("eiland bbox: %s .. %s" % (w(land[:, 0].min(), land[:, 1].min()), w(land[:, 0].max(), land[:, 1].max())))
    return "\n".join(out)


def main():
    ss = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    K.OUT = sys.argv[2] if len(sys.argv) > 2 else os.getcwd()
    wat = sys.argv[3].split(",") if len(sys.argv) > 3 else ["overzicht", "brug", "plein", "kaart"]
    suf = "" if ss == 2 else "_schets"
    S, C = bouw()
    with open(os.path.join(K.OUT, "ambachtseiland_coords.txt"), "w", encoding="utf-8") as f:
        f.write(info(C))
    print(info(C), flush=True)
    land = np.argwhere(C.I.H >= 0)
    ex1, ez1 = land[:, 0].min(), land[:, 1].min()
    ex2, ez2 = land[:, 0].max(), land[:, 1].max()
    xi, xk, yd = C.brug_eind
    px, pz = C.s(*A.PLEIN)
    if "overzicht" in wat:
        x1, z1, x2, z2 = max(0, ex1 - 14), ez1 - 14, xk + 75, ez2 + 14
        N = crop(S, C, x1, z1, x2, z2)
        K.render(N, "ambacht_1_overzicht" + suf, yaw=35, pitch=32, scale=5.0, ss=ss, extra=npc_extra(C, x1, z1, 0, x2, z2))
    if "brug" in wat:
        _, bz = C.s(0, A.BRUG_Z)
        x1, z1, x2, z2 = max(0, ex1 - 6), bz - 46, xk - 18, bz + 40
        N = crop(S, C, x1, z1, x2, z2)
        K.render(N, "ambacht_2_vanafdebrug" + suf, yaw=78, pitch=16, scale=9.0, ss=ss, extra=npc_extra(C, x1, z1, 0, x2, z2))
    if "plein" in wat:
        x1, z1, x2, z2 = px - 30, pz - 28, px + 30, pz + 30
        N = crop(S, C, x1, z1, x2, z2, KP.VLOER - 6)
        lab = [(x - x1, y, z - z1, t) for t, (x, y, z) in C.labels.items()
               if t in ("Smederij", "Keuken", "Viswinkel", "Verzamelhuisje", "Meester Knutsel")]
        K.render(N, "ambacht_3_plein" + suf, yaw=32, pitch=36, scale=15.0, ss=ss, extra=npc_extra(C, x1, z1, 0, x2, z2), labels=lab)
    if "kaart" in wat:
        x1, z1, x2, z2 = max(0, ex1 - 10), ez1 - 10, xk + 6, ez2 + 10
        N = crop(S, C, x1, z1, x2, z2)
        lab = [(x - x1, y, z - z1, t) for t, (x, y, z) in C.labels.items()]
        K.render(N, "ambacht_4_kaart" + suf, yaw=0, pitch=89.5, scale=6.0, ss=ss, extra=npc_extra(C, x1, z1, 0, x2, z2), labels=lab,
                 titel="Ambachtseiland (bovenaanzicht, noord = boven)", noroof=False, schaduw=False)


if __name__ == "__main__":
    main()
