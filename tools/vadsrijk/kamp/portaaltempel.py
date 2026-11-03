"""Portaaltempel: ingestorte ordetempel van de oude Knuffelorde (Guhs 1.10.0, ingang naar Het Vadsrijk).

Zeldzame structuur (~40x40) in de Guhmension: roze gras, roze wol eronder. Drie getrapte terrassen van roze
steen, half overwoekerd, met zuilen (deels gebroken), een brede trap aan de zuidkant en bovenop een ronde poort
met een guhgezicht-sluitsteen en een slapend (grijs-paars) portaal.
bouw(S, ox, oz, g) zet de tempel met de noordwest-hoek van zijn vak op (ox, oz) en de grond op hoogte g.
Portaalblokken renderen leeg, dus het portaal is hier getint glas + paars/grijs glas (alleen voor de render).
"""
import math, random
import numpy as np
import vrkit as K
from vrkit import stairs, slab, pix, oor

MAAT = (76, 56, 78)
G = 12
GRAS = "guhs:knuffelgras"
WOL = "minecraft:pink_wool"
ROZE, GLAD, PASTEL, LICHT = "guhs:roze_knuffelsteen", "guhs:gladde_knuffelsteen", "guhs:pastelbaksteen_roze", \
    "guhs:knuffelsteen"
MOS, KLIM, KLIM2 = "minecraft:moss_block", "minecraft:azalea_leaves", "minecraft:flowering_azalea_leaves"
LUCHT_GUHMENSION = ((196, 184, 238), (255, 228, 240))
rng = random.Random(1100)

SLUIT_KEY = {"#": GLAD, "o": "minecraft:black_concrete", "w": "minecraft:white_concrete",
             "b": "minecraft:pink_concrete", "m": "minecraft:black_concrete"}


def terrein(S):
    X, Y, Z = S.v.shape
    n = K.ruis(21)
    H = np.zeros((X, Z), int)
    for x in range(X):
        for z in range(Z):
            H[x, z] = int(round(G + 1.6 * n(x, z) + 1.2 * math.sin(x / 11.0) * math.cos(z / 13.0)))
    for x in range(X):
        for z in range(Z):
            h = H[x, z]
            rand = x in (0, X - 1) or z in (0, Z - 1)
            lo = 3 if rand else max(3, min(h, H[max(0, x - 1), z], H[min(X - 1, x + 1), z], H[x, max(0, z - 1)],
                                            H[x, min(Z - 1, z + 1)]) - 1)
            for y in range(lo, h + 1):
                S.set(x, y, z, GRAS if y == h else WOL)
    return H


def kruimel(S, x, y, z, b, kans=0.12):
    """Zet b, maar soms een gat of een mosblok (verval)."""
    r = rng.random()
    if r < kans * 0.5:
        return
    S.set(x, y, z, MOS if r < kans else b)


def terras(S, x1, z1, x2, z2, y1, y2, wand=ROZE, vloer=GLAD, verval=0.1):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            rand = x in (x1, x2) or z in (z1, z2)
            for y in range(y1, y2 + 1):
                if y == y2:
                    if rand and rng.random() < verval * 2.2:
                        continue
                    kruimel(S, x, y, z, vloer if not rand else PASTEL, verval * 0.6)
                elif rand:
                    kruimel(S, x, y, z, wand if (y + x + z) % 5 else PASTEL, verval)
                else:
                    S.set(x, y, z, wand)


def zuil(S, x, y, z, h, heel=True):
    S.set(x, y, z, GLAD)
    for k in range(1, h):
        S.set(x, y + k, z, "guhs:pastelbaksteen_roze_muur" if k % 4 else LICHT)
    if heel:
        S.set(x, y + h, z, GLAD)
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            S.set(x + dx, y + h, z + dz, slab("guhs:gladde_knuffelsteen_plaat", "top"))
    else:
        S.set(x, y + h, z, slab("guhs:gladde_knuffelsteen_plaat"))


def klimop(S, n, box):
    """Hangende klimop-plukken op de buitenkant van muren binnen box=(x1,z1,x2,z2,y1,y2)."""
    x1, z1, x2, z2, y1, y2 = box
    for _ in range(n):
        x, z = rng.randint(x1, x2), rng.randint(z1, z2)
        y = rng.randint(y1, y2)
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if not S.is_air(x, y, z) and S.inb(x + dx, y, z + dz) and S.is_air(x + dx, y, z + dz):
                L = rng.randint(2, 6)
                for k in range(L):
                    for w in (0, 1) if rng.random() < 0.6 else (0,):
                        xx, zz = x + dx + (w if dz else 0), z + dz + (w if dx else 0)
                        if S.is_air(xx, y - k, zz) and not S.is_air(xx - dx, y - k, zz - dz):
                            S.set(xx, y - k, zz, KLIM2 if rng.random() < 0.3 else KLIM)
                break


def poort(S, cx, cy, z, R=10, r=7.4):
    """Ronde poort in het vlak z..z+1 (voorkant = zuid, z+1), met sluitsteen (guhgezicht + oortjes) en een slapend portaal erin."""
    for x in range(cx - R - 1, cx + R + 2):
        for y in range(cy - R - 1, cy + R + 2):
            d = math.hypot(x - cx, y - cy)
            if r < d <= R + 0.3 and y >= cy - R:
                a = math.degrees(math.atan2(y - cy, x - cx))
                b = GLAD if int((a + 360) // 15) % 2 else ROZE
                for dz in (0, 1):
                    S.set(x, y, z + dz, b)
                if d > R - 0.7:
                    S.set(x, y, z + 2, PASTEL) if y < cy - R + 3 else None
            elif d <= r:
                # slapend portaal: grijs getint glas achter, paars/grijs glas voor, een paar sterrenstofjes
                S.set(x, y, z, "minecraft:tinted_glass")
                q = rng.random()
                S.set(x, y, z + 1, "minecraft:white_stained_glass" if q < 0.05 else
                      "minecraft:light_gray_stained_glass" if q < 0.4 else "minecraft:purple_stained_glass")
    # sluitsteen met guhgezicht, steekt naar voren
    top = cy + R + 2
    for x in range(cx - 3, cx + 4):
        for y in range(top - 6, top + 1):
            S.set(x, y, z + 1, GLAD)
            S.set(x, y, z + 2, GLAD)
    pix(S, cx - 2, top - 1, z + 3, (1, 0), K.GEZICHT_5, SLUIT_KEY)
    for (x0, side) in ((cx - 4, -1), (cx + 2, 1)):
        oor(S, x0, top + 1, z + 2, (1, 0), h=5, w=3, buiten=ROZE, binnen="minecraft:pink_concrete", dik=(0, -1))
    # voeten van de poort
    for sx in (-1, 1):
        for x in range(cx + sx * (R - 2) - 1, cx + sx * (R - 2) + 2):
            for dz in (-1, 0, 1, 2):
                for k in range(3):
                    S.set(x, cy - R + k, z + dz, PASTEL if k < 2 else GLAD)


def guhbeeld(S, x, y, z, kop_eraf=False):
    """Zittend guhbeeld van steen op een sokkel; kop_eraf=True: de kop ligt ernaast in het gras."""
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            S.set(x + dx, y, z + dz, ROZE)
            S.set(x + dx, y + 1, z + dz, GLAD)
    S.ellipsoid(x, y + 4, z, 2.2, 2.6, 2.2, LICHT, ymin=y + 2)
    if kop_eraf:
        kx, kz = x + 4, z + 2
        S.ellipsoid(kx, y + 1, kz, 2.2, 1.9, 2.2, LICHT, ymin=y)
        S.set(kx - 1, y + 2, kz + 2, "minecraft:black_concrete")
        S.set(kx + 1, y + 2, kz + 2, "minecraft:black_concrete")
        S.set(kx + 1, y + 1, kz - 2, MOS)
    else:
        S.ellipsoid(x, y + 8, z, 2.4, 2.2, 2.4, LICHT)
        S.set(x - 1, y + 8, z + 2, "minecraft:black_concrete")
        S.set(x + 1, y + 8, z + 2, "minecraft:black_concrete")
        for ex in (-2, 2):
            for k in range(3):
                S.set(x + ex, y + 10 + k, z, LICHT if k < 2 else "minecraft:pink_concrete")


def bouw(S, ox=16, oz=16, g=G):
    """Tempel in een 40x(44 incl. trap) vak met noordwest-hoek (ox, oz), grond op y = g."""
    cx = ox + 20
    # terrassen
    terras(S, ox, oz + 2, ox + 40, oz + 40, g - 2, g + 3, verval=0.10)
    terras(S, ox + 6, oz + 6, ox + 34, oz + 32, g + 4, g + 7, verval=0.12)
    terras(S, ox + 10, oz + 8, ox + 30, oz + 24, g + 8, g + 10, verval=0.08)
    top = g + 10
    # brede trap aan de zuidkant
    for k in range(10):
        for zz in (oz + 43 - 2 * k, oz + 42 - 2 * k):
            for x in range(cx - 5, cx + 6):
                if rng.random() < 0.06:
                    continue
                st = stairs("guhs:gladde_knuffelsteen_trap", "north") if zz == oz + 43 - 2 * k else GLAD
                if rng.random() < 0.05:
                    st = slab("guhs:gladde_knuffelsteen_plaat")
                S.set(x, g + 1 + k, zz, st)
                for y in range(g - 1, g + 1 + k):
                    S.set(x, y, zz, PASTEL)
                if rng.random() < 0.07:
                    S.set(x, g + 2 + k, zz, "minecraft:moss_carpet")
        for sx in (-6, 6):
            for zz in (oz + 43 - 2 * k, oz + 42 - 2 * k):
                for y in range(g - 1, g + 2 + k):
                    S.set(cx + sx, y, zz, ROZE)
    # trapleuning-beelden en lampjes onderaan
    guhbeeld(S, cx - 10, g + 1, oz + 46)
    guhbeeld(S, cx + 10, g + 1, oz + 46, kop_eraf=True)
    # zuilen: rondom terras 1 (deels gebroken), vier grote op het bovenste terras
    for z in range(oz + 6, oz + 40, 5):
        for x in (ox + 2, ox + 38):
            h = 11 if rng.random() < 0.45 else rng.randint(2, 7)
            zuil(S, x, g + 4, z, h, heel=h == 11)
    for x in range(ox + 7, ox + 34, 6):
        h = 11 if rng.random() < 0.4 else rng.randint(1, 6)
        zuil(S, x, g + 4, oz + 38, h, heel=h == 11)
    for (x, z, h, heel) in ((ox + 11, oz + 9, 14, True), (ox + 29, oz + 9, 9, False), (ox + 11, oz + 23, 14, True),
                            (ox + 29, oz + 23, 14, True)):
        zuil(S, x, top + 1, z, h, heel)
    # architraaf-restje tussen twee hele zuilen
    for z in range(oz + 9, oz + 24):
        if z < oz + 18:
            S.set(ox + 11, top + 15, z, GLAD)
    # de poort
    poort(S, cx, top + 11, oz + 15)
    # een gevallen zuil + brokstukken
    for i in range(9):
        S.set(ox - 6 + i, g + 1, oz + 30, "guhs:pastelbaksteen_roze" if i % 3 else LICHT)
    S.set(ox - 7, g + 1, oz + 30, GLAD)
    for (x, z) in ((ox + 44, oz + 12), (ox + 45, oz + 13), (ox - 4, oz + 8), (ox + 46, oz + 30)):
        for k in range(rng.randint(1, 3)):
            S.set(x, g + 1 + k, z, LICHT)
    for _ in range(70):
        x, z = rng.randint(ox - 8, ox + 48), rng.randint(oz - 6, oz + 50)
        h = S.top_y(x, z) if S.inb(x, 0, z) else -1
        if h >= 0 and S.get(x, h, z) == GRAS:
            S.set(x, h + 1, z, rng.choice([slab("guhs:gladde_knuffelsteen_plaat"), slab("guhs:knuffelsteen_plaat"),
                                           stairs("guhs:knuffelsteen_trap", rng.choice(["north", "east"])),
                                           "minecraft:moss_carpet", ROZE]))
    # ruïnemuurtjes in een gebroken ring eromheen
    for a in range(0, 360, 4):
        if rng.random() < 0.45:
            continue
        ar = math.radians(a)
        x, z = int(cx + 31 * math.cos(ar)), int(oz + 22 + 29 * math.sin(ar))
        if not S.inb(x, 0, z) or (abs(x - cx) < 9 and z > oz + 40):
            continue
        h = S.top_y(x, z)
        for k in range(1, rng.randint(1, 4)):
            S.set(x, h + k, z, ROZE if rng.random() < 0.7 else MOS)
    klimop(S, 160, (ox, oz + 2, ox + 40, oz + 40, g + 1, g + 10))
    klimop(S, 50, (ox + 9, oz + 7, ox + 31, oz + 25, top + 2, top + 22))
    # bomen en bloemen
    for (x, z) in ((ox - 7, oz - 2), (ox + 47, oz + 2), (ox - 9, oz + 44), (ox + 50, oz + 46), (ox + 49, oz + 22),
                   (ox - 10, oz + 18), (ox + 22, oz - 9)):
        if S.inb(x, 0, z):
            K.Scene.tree  # noqa
            h = S.top_y(x, z)
            for k in range(1, 7):
                S.set(x, h + k, z, "guhs:guhbloesem_log[axis=y]")
            S.ellipsoid(x, h + 8, z, 4.2, 3.0, 4.2, "guhs:guhbloesem_leaves", keep=True)
    X, Y, Z = S.v.shape
    for x in range(X):
        for z in range(Z):
            h = S.top_y(x, z)
            if S.get(x, h, z) == GRAS and rng.random() < 0.2:
                S.set(x, h + 1, z, rng.choice(["guhs:roze_gras", "guhs:roze_gras", "guhs:weide_roze_gras",
                                               "guhs:guhoortjes", "guhs:roze_guhbloem", "minecraft:allium"]))
    # één nieuwsgierige guh bij de trap
    S.guh(cx + 2.5, g + 1, oz + 47.5, 200)


if __name__ == "__main__":
    import sys
    ss = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    suf = "" if ss == 2 else "_schets"
    S = K.Scene(*MAAT)
    terrein(S)
    bouw(S)
    print("blokken:", int((S.v > 0).sum()), flush=True)
    K.render(S, "portaaltempel_iso_zuidoost" + suf, yaw=35, pitch=30, scale=11, ss=ss, sky=LUCHT_GUHMENSION)
    K.render(S, "portaaltempel_iso_noordwest" + suf, yaw=215, pitch=30, scale=11, ss=ss, sky=LUCHT_GUHMENSION)
    K.render(S, "portaaltempel_trap_vooraanzicht" + suf, yaw=0, pitch=12, scale=15, ss=ss, sky=LUCHT_GUHMENSION)
