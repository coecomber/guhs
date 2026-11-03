"""Vergezicht vanaf de vuurtoren van Knuffelkamp: klifrand, zee, en ver weg de skyline van Zwamzoet.

Lage camera (pitch 6) die naar het noorden kijkt. De verre laag (z < VER_Z) rendert apart en krijgt nevel.
zwamzoet_silhouet(S, ...) is een grove schets van de reuzenzwam-stad, alleen bedoeld als horizon.
"""
import math, random
import numpy as np
import vrkit as K
import kamp as KP

MAAT = (220, 96, 440)
ZEE = KP.ZEE
GROND = 30
VER_Z = 150
CROP = (0.025, 0.0, 0.975, 0.785)
rng = random.Random(6)


def klifrand(S):
    """Grasrand van het eiland (z >= ~380) met een steile klif naar de zee."""
    X, Y, Z = S.v.shape
    n = K.ruis(11)
    H = np.full((X, Z), -1, int)
    for x in range(X):
        rand_z = 378 + 5 * n(x * 1.3, 0) + 3 * math.sin(x / 9.0)
        for z in range(Z):
            d = z - rand_z
            if d < 0:
                continue
            H[x, z] = int(round(min(GROND + 2 * n(x, z), ZEE + 1 + d * 6)))
    Hp = np.pad(H, 1, constant_values=-1)
    buur = np.minimum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
    for x in range(X):
        for z in range(Z):
            h = H[x, z]
            if h < 0:
                S.set(x, ZEE - 1, z, "minecraft:sand" if z > 372 else "minecraft:prismarine")
                S.set(x, ZEE, z, "minecraft:water")
                continue
            lo = max(ZEE - 2, min(h, buur[x, z] if buur[x, z] >= 0 else ZEE - 1) - 1)
            for y in range(lo, h + 1):
                if y == h:
                    b = KP.GRAS
                elif y > ZEE + 1:
                    b = ["minecraft:stone", "minecraft:andesite", "minecraft:calcite", "minecraft:stone",
                         "minecraft:tuff"][(y + x // 6) // 3 % 5]
                    if y > h - 5 and rng.random() < 0.2:
                        b = "minecraft:moss_block"
                else:
                    b = "minecraft:mossy_cobblestone"
                S.set(x, y, z, b)
            if h > ZEE + 5 and rng.random() < 0.16:
                S.set(x, h + 1, z, rng.choice(["minecraft:short_grass"] * 4 + ["minecraft:pink_tulip",
                      "guhs:guhoortjes", "guhs:roze_guhbloem", "minecraft:oxeye_daisy", "minecraft:allium"]))
    return H


def zwam(S, x, z, y0, h, r, steel="minecraft:white_concrete", hoed="minecraft:pink_concrete", lichtjes=True):
    """Reuzenzwam: steel (cilinder, iets taps) + afgeplatte hoed met lichtjes en een guhhuisje bovenop."""
    for k in range(h):
        rr = max(1.5, r * 0.22 * (1.25 - 0.35 * k / h))
        S.cylinder(x, z, rr, y0 + k, y0 + k, steel)
    S.ellipsoid(x, y0 + h, z, r, r * 0.42, r, hoed, hollow=1.5, ymin=y0 + h - 1)
    for k in range(int(r * 1.5)):
        a = rng.uniform(0, 6.28)
        rr = rng.uniform(0.3, 0.92) * r
        xx, zz = int(x + rr * math.cos(a)), int(z + rr * math.sin(a))
        if not S.inb(xx, 0, zz):
            continue
        col = S.v[xx, :, zz]
        ys = np.nonzero(col)[0]
        if len(ys):
            S.set(xx, int(ys.max()), zz, "minecraft:white_concrete")
    if lichtjes:
        for k in range(14):
            a = 2 * math.pi * k / 14
            S.set(int(x + (r - 1) * math.cos(a)), y0 + h - 1, int(z + (r - 1) * math.sin(a)),
                  "minecraft:pearlescent_froglight")
    top = y0 + h + int(r * 0.42)
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            for k in range(2):
                S.set(x + dx, top + k, z + dz, "guhs:pastelbaksteen_roze")
    S.set(x - 1, top + 2, z, "minecraft:pink_wool")
    S.set(x + 1, top + 2, z, "minecraft:pink_wool")


def zwamzoet_silhouet(S, x0=25, z0=40):
    """Grove horizon-schets van Zwamzoet: een heuveleiland met reuzenzwammen, hoogste in het midden."""
    S.ellipsoid(x0 + 80, ZEE, z0 + 30, 85, 4, 26, "minecraft:grass_block", ymin=ZEE)
    zwammen = [(-60, 8, 14, 6), (-38, 18, 20, 7), (-10, 0, 34, 9), (16, 18, 26, 8), (40, 4, 18, 6),
               (60, 22, 13, 5), (82, 10, 22, 7), (-75, 22, 10, 4), (100, 20, 9, 4)]
    for (dx, dz, h, r) in zwammen:
        x, z = x0 + 80 + dx, z0 + 25 + dz
        hoed = "guhs:guhpaddenstoel_hoed" if r > 7 else rng.choice(["minecraft:pink_concrete", "minecraft:pink_wool"])
        zwam(S, x, z, ZEE + 2, h, r, hoed=hoed)
    # spits van de ordetempel op de hoogste hoed
    tx, tz = x0 + 70, z0 + 25
    top = ZEE + 2 + 34 + 6
    for k in range(9):
        rr = max(0.6, 2.5 - k * 0.3)
        S.cylinder(tx, tz, rr, top + k, top + k, "minecraft:pink_concrete" if k < 7 else "minecraft:white_concrete")


def bouw(S):
    klifrand(S)
    lx, lz = 42, 404
    KP.vuurtoren(S, lx, GROND, lz)
    for (x, z) in ((105, 386), (111, 387)):
        S.guh(x + 0.5, GROND + 1, z + 0.5, 0)
    S.set(108, GROND + 1, 390, "minecraft:spruce_log[axis=x]")
    S.set(109, GROND + 1, 390, "minecraft:spruce_log[axis=x]")
    S.guh(108.5, GROND + 2, 390.5, 0, sitting=True)
    KP.boom(S, 180, GROND, 408, KP.BOOM_ROZE, h=6, r=5)
    KP.boom(S, 198, GROND, 420, KP.BOOM_ROZE, h=5, r=4)
    KP.boom(S, 160, GROND, 425, KP.BOOM_GROEN, h=5, r=3.5)
    KP.vlag(S, 128, GROND + 1, 392, (1, 0), h=5, w=3, paal_h=9, embleem="guhs:mini_luchtballon")
    KP.guhballon(S, 168, 70, 300, r=5)
    zwamzoet_silhouet(S)


if __name__ == "__main__":
    import sys
    ss = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    S = K.Scene(*MAAT)
    bouw(S)
    banden = []
    for (z2, nv) in [(VER_Z, 0.5)] + [(z, 0.5 * (380 - z) / (380 - VER_Z)) for z in range(VER_Z + 20, 381, 20)]:
        m = np.zeros(S.v.shape, bool)
        m[:, :, :z2] = True
        banden.append((m, nv))
    suf = "" if ss == 2 else "_schets"
    K.render_lagen(S, "knuffelkamp_vergezicht" + suf, banden, yaw=0, pitch=8, scale=7, ss=ss, nevel=0.5, crop=CROP,
                   zon=(0.86, 0.5, 0.03), manen=((0.12, 0.16, 0.022), (0.2, 0.1, 0.012)), wolken=9)
