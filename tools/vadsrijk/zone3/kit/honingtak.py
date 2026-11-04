"""Hubstad Honingtak (ZONE3_PLAN.md §3): de honingstad op en onder de oostelijke reuzentak, op kruinhoogte.

Vier niveaus:
  1. Takrug (y ~238): Bloesemplein met knuffelbeeld, zoemende markt, bloesemboompje, luchtschipmast + luchtschip
  2. Korfhuisjes (y 208-232): bijenkorfhuisjes (skep-vorm) op plateautjes langs de flanken, touwbruggen, trapjes
  3. Raattrossen (y 172-210): hangende trossen zeshoekige raatcellen onder de tak; onderaan Nestkapel en café
  4. Pluimstal (y ~214, westkant, op het bladerdak naast de tak): 8 boxen, oefenbaan, zweefplatform
Plus honingliftjes, de grote Honinglift (kabel naar Mosvoet), lampions en imkerguhs.

bouw() geeft een Wereld (k=1) in wereldcoördinaten t.o.v. de stamas; plaats(W) stempelt hem in elk venster.
"""
import math, functools
import numpy as np
import schemerkit as SK
import boom
import vrkit as VK
import kamp as KP

B = (285, 150, -100, 665, 268, 105)        # bouwvak (wereld)
DEK_Y = 238
PLEIN = (470, -2)
MAST = (622, 0)
KAPEL = (505, 0)
CAFE = (428, 0)
WONEN = (568, 0)
STAL = (296, 236, 26)                       # hoek van het stalplateau (op de kruinrand)
HOUT, PLANK, HEK = "minecraft:stripped_spruce_wood", "minecraft:spruce_planks", "minecraft:spruce_fence"
RAAT = "minecraft:honeycomb_block"
GLAS = "minecraft:orange_stained_glass"
LAMP_R, LAMP_G = "guhs:lampion_roze", "guhs:lampion_geel"

# korfhuisjes: (x, kant, vloer-y, soort)
KORVEN = [(388, 1, 216, 0), (412, -1, 212, 1), (440, 1, 222, 2), (458, -1, 218, 0), (492, 1, 214, 1),
          (520, -1, 224, 2), (546, 1, 220, 0), (584, -1, 214, 1), (600, 1, 226, 2), (402, 1, 228, 1),
          (430, -1, 228, 2), (536, -1, 210, 0), (566, 1, 230, 2), (476, -1, 230, 1), (510, 1, 230, 0),
          (594, -1, 230, 2)]


def tak(x):
    """(midden-y, straal, midden-z) van de oosttak bij x."""
    L, rr = boom.tak_lijn("oost")
    i = int(np.argmin(np.abs(L[:, 0] - x)))
    return L[i, 1], rr[i], L[i, 2]


def tak_boven(x):
    yc, r, zc = tak(x)
    return yc + r * 0.72


def tak_onder(x):
    yc, r, zc = tak(x)
    return yc - r * 0.72


def zeshoek(dx, dz):
    """'Straal' van een punt in een zeshoek met punten op de x-as."""
    return np.maximum(np.abs(dx) * 0.5 + np.abs(dz) * 0.866, np.abs(dx))


def raatcel(H, cx, ytop, cz, R, h, wand=RAAT, vloer=PLANK, ramen=True, deur=None, roof=False, binnen=None):
    """Hangende zeshoekige raatcel van ytop naar beneden (h hoog)."""
    bx = H._box(cx - R - 1, ytop - h, cz - R - 1, cx + R + 1, ytop, cz + R + 1)
    if bx is None:
        return
    sl, (x, y, z) = bx
    d = zeshoek(x - cx, z - cz)
    yb = ytop - h
    rand = (d <= R) & (d > R - 1.2)
    v = H.v[sl]
    v[rand & (y >= yb) & (y <= ytop)] = H.id(wand)
    v[(d <= R) & (y < yb + 1) & (y >= yb)] = H.id(vloer if vloer else wand)
    v[(d <= R) & (y > ytop - 1)] = H.id(RAAT)
    if ramen:
        hoek = np.degrees(np.arctan2(z - cz, x - cx)) % 60
        rm = rand & (np.abs(hoek - 30) < 9) & (y >= yb + h * 0.35) & (y <= yb + h * 0.75)
        v[rm] = H.id(GLAS)
    if binnen:
        v[(d < R - 1.2) & (y >= yb + 1) & (y <= ytop - 1)] = 0
    if roof:
        for p in np.argwhere((rand | ((d <= R) & (y > ytop - 1))) & (v != 0)):
            H.roof.add((sl[0].start + p[0], sl[1].start + p[1], sl[2].start + p[2]))
    # honingdruppels onder de cel
    for k in range(int(R)):
        a = k * 2.4
        px, pz = cx + (R - 2) * math.cos(a) * 0.6, cz + (R - 2) * math.sin(a) * 0.6
        if k % 2 == 0:
            for j in range(1 + k % 3):
                H.wset(px, yb - 1 - j, pz, "minecraft:honey_block", keep=True)


def korf(H, cx, fy, cz, kant, soort=0, R=6.0):
    """Bijenkorfhuisje (skep): gestapelde ringen, ronde deur naar buiten, honingramen, guh-oortjes."""
    banden = [("minecraft:hay_block", "minecraft:yellow_terracotta"),
              ("guhs:roze_hooibaal", "minecraft:pink_terracotta"),
              ("minecraft:hay_block", RAAT)][soort]
    hgt = 11
    # plateau
    H.cilinder(cx, cz, R + 2.5, fy - 1, fy - 1, PLANK)
    H.cilinder(cx, cz, R + 2.5, fy, fy, HEK, hollow=1)
    for k in range(hgt):
        t = k / hgt
        r = R * math.sqrt(max(0.0, 1 - t ** 2.2)) + 0.4
        b = banden[1] if k % 3 == 2 else banden[0]
        H.cilinder(cx, cz, r, fy + k, fy + k, b, hollow=1.3)
        for p in range(0):
            pass
    H.cilinder(cx, cz, R - 0.5, fy, fy, "minecraft:yellow_carpet")
    # deur (naar buiten, weg van de tak) en ramen
    dz = kant
    for k in range(0, 4):
        for w in (-1, 0, 1):
            if k == 3 and w != 0:
                continue
            H.wset(cx + w, fy + k, cz + dz * (R - 0.2), "minecraft:air")
            H.wset(cx + w, fy + k, cz + dz * (R + 0.6), "minecraft:air")
    H.wset(cx, fy + 4, cz + dz * (R + 0.3), LAMP_G)
    for (wx_, wz_) in ((R - 0.3, 0), (-R + 0.3, 0)):
        H.wset(cx + wx_, fy + 3, cz + wz_, GLAS)
        H.wset(cx + wx_, fy + 4, cz + wz_, GLAS)
    # guh-oortjes op de top
    vx, vy, vz = H.vx(cx), H.vy(fy + hgt - 1), H.vz(cz)
    VK.oortjes(H, vx, vy, vz, (1, 0), gap=2, h=3, w=3, buiten="minecraft:pink_wool",
               binnen="minecraft:pink_concrete")
    # dragers naar de tak
    yc, rt, zc = tak(cx)
    for dx in (-3, 3):
        p0 = (cx + dx, fy - 1, cz - kant * (R + 1))
        p1 = (cx + dx * 0.5, yc - 2, zc + kant * rt * 0.6)
        H.buis([p0, p1], [0.9, 0.9], HOUT)
    # lampje onder het plateau
    for j in range(3):
        H.wset(cx, fy - 2 - j, cz + kant * (R + 1), "minecraft:iron_chain[axis=y]")
    H.wset(cx, fy - 5, cz + kant * (R + 1), LAMP_R)


def touwbrug(H, p0, p1, zak=3.0):
    n = int(max(abs(p1[0] - p0[0]), abs(p1[2] - p0[2])) + 1)
    for i in range(n + 1):
        t = i / max(1, n)
        x = p0[0] + (p1[0] - p0[0]) * t
        z = p0[2] + (p1[2] - p0[2]) * t
        y = p0[1] + (p1[1] - p0[1]) * t - zak * math.sin(math.pi * t)
        H.wset(x, y, z, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        if i % 2 == 0:
            H.wset(x, y + 1, z - 1, HEK, keep=True)
            H.wset(x, y + 1, z + 1, HEK, keep=True)


def trap(H, p0, p1, breed=1):
    """Schuine trap van plank-blokken tussen twee punten (met onderdelen ernaast)."""
    n = int(max(abs(p1[1] - p0[1]), abs(p1[0] - p0[0]), abs(p1[2] - p0[2])) + 1)
    for i in range(n + 1):
        t = i / max(1, n)
        x, y, z = (p0[k] + (p1[k] - p0[k]) * t for k in range(3))
        for w in range(-breed, breed + 1):
            H.wset(x, y, z + w, PLANK)


def takrug(H):
    """Niveau 1: dek, plein, markt, bloesemboompje, mast."""
    for x in range(360, 628):
        yc, r, zc = tak(x)
        hw = r * 0.62 + 4
        if abs(x - PLEIN[0]) < 18:
            hw = max(hw, math.sqrt(max(0, 18 ** 2 - (x - PLEIN[0]) ** 2)))
        for z in range(int(-hw), int(hw) + 1):
            H.wset(x, DEK_Y, zc + z, PLANK)
            # dragers tot in de tak
            for y in range(DEK_Y - 1, DEK_Y - 9, -1):
                if abs(z) > r * 0.55:
                    if (x % 6 == 0 and abs(abs(z) - int(hw)) < 1):
                        H.wset(x, y, zc + z, HOUT)
        # rand: hekje, lampions
        for s in (-1, 1):
            H.wset(x, DEK_Y + 1, zc + s * int(hw), HEK)
            if x % 12 == 0:
                for k in range(1, 4):
                    H.wset(x, DEK_Y + k, zc + s * int(hw), "minecraft:stripped_spruce_wood")
                H.wset(x, DEK_Y + 4, zc + s * int(hw), LAMP_R if (x // 12) % 2 else LAMP_G)
    # slingers lampions langs de flanken van de tak (gezellig bij schemer)
    for x in range(366, 624, 5):
        yc, r, zc = tak(x)
        for s_ in (-1, 1):
            zz = zc + s_ * (r + 0.6)
            hang = 2 + int(2.5 * abs(math.sin(x / 9.0)))
            for k in range(hang):
                H.wset(x, yc + 1 - k, zz, "minecraft:iron_chain[axis=y]", keep=True)
            H.wset(x, yc + 1 - hang, zz, [LAMP_R, LAMP_G, "guhs:lampion_mint"][(x // 5) % 3], keep=True)
    # pad over het dek
    for x in range(362, 626):
        yc, r, zc = tak(x)
        for z in (-1, 0, 1):
            H.wset(x, DEK_Y, zc + z, "minecraft:stripped_birch_wood")
    # Bloesemplein: roze/witte tegels in ringen
    px, pz = PLEIN
    for x in range(px - 17, px + 18):
        for z in range(pz - 17, pz + 18):
            d = math.hypot(x - px, z - pz)
            if d < 16:
                b = "guhs:pastelbaksteen_roze" if int(d) % 4 < 2 else "minecraft:white_concrete"
                if d < 3.5:
                    b = "minecraft:honeycomb_block"
                H.wset(x, DEK_Y, z, b)
            elif d < 17:
                H.wset(x, DEK_Y + 1, z, HEK)
    knuffelbeeld(H, px, DEK_Y + 1, pz)
    for (dx, dz) in ((-9, -9), (9, -9), (-9, 9), (9, 9)):
        H.wset(px + dx, DEK_Y + 1, pz + dz, "guhs:guh_bankje_roze[facing=south]")
        H.wset(px + dx, DEK_Y + 1, pz + dz + (1 if dz > 0 else -1), "guhs:guh_bloempot")
    for (dx, dz) in ((-14, 0), (14, 0), (0, -14), (0, 14)):
        for k in range(1, 5):
            H.wset(px + dx, DEK_Y + k, pz + dz, "minecraft:stripped_spruce_wood")
        H.wset(px + dx, DEK_Y + 5, pz + dz, LAMP_R)
    H.label(px, DEK_Y + 10, pz, "Bloesemplein")
    # zoemende markt: kraampjes met bijenkorfluifels
    for i, x in enumerate(range(398, 448, 12)):
        for s in (-1, 1):
            yc, r, zc = tak(x)
            z0 = zc + s * (r * 0.62 + 1)
            kraam(H, x, DEK_Y + 1, z0, s, i)
    H.label(420, DEK_Y + 9, 0, "Zoemende markt")
    # bloesemboompje dat uit de tak groeit
    bx, bz = 548, 3
    for k in range(1, 13):
        H.wset(bx, DEK_Y + k, bz, "guhs:guhbloesem_log[axis=y]")
        if k > 7:
            H.wset(bx + (k % 2), DEK_Y + k, bz + 1, "guhs:guhbloesem_log[axis=y]")
    H.bol((bx, DEK_Y + 15, bz), 9, None, ry=6, mixfn=lambda x, y, z, m: H.kies(
        ["minecraft:cherry_leaves", "guhs:guhbloesem_leaves"], [3, 2], x, y, z, 301), keep=True)
    for k in range(10):
        a = k * 0.63
        H.wset(bx + 7 * math.cos(a), DEK_Y + 11, bz + 7 * math.sin(a), LAMP_R, keep=True)
    # luchtschipmast op de punt
    mx, mz = MAST
    for k in range(1, 26):
        H.wset(mx, DEK_Y + k, mz, "minecraft:stripped_spruce_log[axis=y]")
    H.cilinder(mx, mz, 3.5, DEK_Y + 16, DEK_Y + 16, PLANK)
    H.cilinder(mx, mz, 3.5, DEK_Y + 17, DEK_Y + 17, HEK, hollow=1)
    for k in range(1, 12):  # aanlegarm naar het zuiden
        H.wset(mx, DEK_Y + 14, mz + k, "minecraft:stripped_spruce_log[axis=z]")
    H.wset(mx, DEK_Y + 26, mz, LAMP_G)
    for k in range(5):
        for j in range(1, 4):
            H.wset(mx + j, DEK_Y + 24 - k // 2, mz, "minecraft:pink_wool" if (k + j) % 2 else "minecraft:white_wool")
    H.label(mx, DEK_Y + 30, mz, "Luchtschipmast")


def kraam(H, x, y, z, s, i):
    kl = ["minecraft:yellow_wool", "minecraft:pink_wool", "minecraft:orange_wool", "minecraft:white_wool"][i % 4]
    for dx in (-2, 2):
        for k in range(0, 4):
            H.wset(x + dx, y + k, z + s * 3, "minecraft:spruce_fence")
    for dx in range(-3, 4):
        for dz in range(0, 5):
            b = kl if (dx + 4) % 2 else "minecraft:yellow_terracotta"
            H.wset(x + dx, y + 4 + (1 if dz in (1, 2, 3) else 0), z + s * dz, b)
    for dx in range(-2, 3):
        H.wset(x + dx, y, z + s * 1, "minecraft:beehive[facing=north,honey_level=5]" if dx % 2 else
               "minecraft:barrel[facing=up,open=false]")
    H.wguh(x + 0.5, y, z + s * 3 + 0.5, 0 if s > 0 else 180, skin="guh_golden")


def knuffelbeeld(H, x, y, z):
    """Knuffelbeeld: een grote imkerguh met een honingpot (uit wol), op een sokkel."""
    H.cilinder(x, z, 3.2, y, y + 1, "minecraft:quartz_block")
    H.bol((x, y + 6, z), 3.6, "minecraft:pink_wool", ry=4.2)
    H.bol((x, y + 6.5, z - 2.9), 1.3, "minecraft:pink_concrete", ry=1)
    for dx in (-1.5, 1.5):
        H.wset(x + dx, y + 7, z - 3.6, "minecraft:black_concrete")
    # imkerhoedje
    H.cilinder(x, z, 4.5, y + 10, y + 10, "minecraft:white_wool")
    H.cilinder(x, z, 2.6, y + 11, y + 12, "minecraft:white_wool")
    VK.oortjes(H, H.vx(x), H.vy(y + 11), H.vz(z), (1, 0), gap=4, h=3, w=2)
    # honingpot in de armen
    H.bol((x + 3.5, y + 5, z - 1.5), 1.6, "minecraft:orange_terracotta")
    H.wset(x + 3.5, y + 6.6, z - 1.5, "minecraft:honey_block")


def korfhuisjes(H):
    for (x, kant, fy, soort) in KORVEN:
        yc, r, zc = tak(x)
        cz = zc + kant * (r + 6.5 + (2 if fy >= 228 else 0))
        korf(H, x, fy, cz, kant, soort)
        # trapje naar het dek
        trap(H, (x - 7, fy - 1, cz - kant * 3), (x - 7 - (DEK_Y - fy) * 0.9, DEK_Y, zc + kant * (r * 0.62 + 3)),
             breed=0)
    # touwbruggen tussen buren op dezelfde kant
    per = {}
    for (x, kant, fy, soort) in KORVEN:
        per.setdefault(kant, []).append((x, fy))
    for kant, lst in per.items():
        lst.sort()
        for (x0, f0), (x1, f1) in zip(lst, lst[1:]):
            if abs(f1 - f0) > 6 or x1 - x0 > 40:
                continue
            yc, r, zc = tak((x0 + x1) / 2)
            zz = zc + kant * (r + 6.5)
            touwbrug(H, (x0 + 8, f0 - 1, zz), (x1 - 8, f1 - 1, zz), zak=2.5)
    H.label(440, 232, 30, "Korfhuisjes")


def tros(H, cx, cz, cellen):
    """Een raattros: cellen = [(dx, dz, ytop, R, h, soort)] onder de tak, met kettingen en een wenteltrap."""
    for (dx, dz, ytop, R, h, soort) in cellen:
        x, z = cx + dx, cz + dz
        kl = {"raat": RAAT, "kapel": "minecraft:quartz_block", "cafe": RAAT}.get(soort, RAAT)
        raatcel(H, x, ytop, z, R, h, wand=kl, binnen=soort in ("cafe", "kapel"))
        top_tak = tak_onder(x)
        for k in range(int(ytop) + 1, int(top_tak) + 2):
            for o in (-R * 0.5, R * 0.5):
                H.wset(x + o, k, z, "minecraft:iron_chain[axis=y]", keep=True)
    # wenteltrap van de onderste cel tot op het dek, langs de flank
    yc, r, zc = tak(cx)
    sx, sz = cx - 12, cz + r + 3
    ybot = min(c[2] - c[4] for c in cellen)
    for y in range(int(ybot), DEK_Y + 1):
        a = y * 0.55
        H.wset(sx, y, sz, "minecraft:stripped_spruce_log[axis=y]")
        for rr in (1, 2):
            H.wset(sx + rr * math.cos(a), y, sz + rr * math.sin(a), "minecraft:spruce_slab[type=top,waterlogged=false]")
        if y % 9 == 0:
            H.wset(sx, y, sz + 3, LAMP_G, keep=True)


def raattrossen(H):
    ondr = tak_onder
    # café-tros
    cx, cz = CAFE
    t = ondr(cx)
    tros(H, cx, cz, [(-9, -4, t - 1, 5, 7, "raat"), (8, -5, t - 1, 5, 7, "raat"), (0, 7, t - 1, 5, 8, "raat"),
                     (-4, 1, t - 9, 6, 8, "raat"), (6, 2, t - 10, 5, 7, "raat"),
                     (0, 0, t - 19, 9, 10, "cafe")])
    H.label(cx, t - 34, cz, "Café De Zoete Raat")
    # Nestkapel-tros
    cx, cz = KAPEL
    t = ondr(cx)
    tros(H, cx, cz, [(-8, -6, t - 1, 5, 8, "raat"), (9, -3, t - 1, 5, 7, "raat"), (-2, 8, t - 1, 5, 7, "raat"),
                     (0, 0, t - 10, 6, 8, "raat"), (0, 0, t - 19, 10, 13, "kapel")])
    nestkapel_buiten(H, cx, t - 19 - 13, cz)
    H.label(cx, t - 44, cz, "Nestkapel")
    # woontros
    cx, cz = WONEN
    t = ondr(cx)
    tros(H, cx, cz, [(-7, -5, t - 1, 5, 7, "raat"), (7, -4, t - 1, 5, 7, "raat"), (0, 6, t - 1, 5, 7, "raat"),
                     (-4, 0, t - 9, 5, 7, "raat"), (5, 2, t - 9, 5, 7, "raat"), (0, -1, t - 17, 6, 8, "raat")])
    H.label(cx, t - 30, cz, "Raattrossen")
    # lichtjes tussen de cellen
    for (cx, cz) in (CAFE, KAPEL, WONEN):
        t = ondr(cx)
        for k in range(8):
            a = k * 0.8
            H.wset(cx + 11 * math.cos(a), t - 4 - (k % 3) * 5, cz + 11 * math.sin(a), LAMP_G, keep=True)


def nestkapel_buiten(H, cx, yb, cz):
    """Omgekeerde spits onder de Nestkapel, roze band, honingglas-ramen."""
    for k in range(10):
        r = 7.5 * (1 - k / 10.0)
        H.cilinder(cx, cz, max(0.6, r), yb - k, yb - k, "minecraft:pink_concrete" if k % 3 else "minecraft:quartz_block")
    H.wset(cx, yb - 10, cz, "minecraft:pearlescent_froglight")
    H.wset(cx, yb - 11, cz, "minecraft:iron_chain[axis=y]")
    H.wset(cx, yb - 12, cz, LAMP_R)


def pluimstal(H):
    """Niveau 4: open stal met 8 boxen, voerzolder, oefenbaan en zweefplatform, op het bladerdak."""
    x0, y, z0 = STAL
    # plateau
    H.wfill(x0, y - 1, z0, x0 + 70, y - 1, z0 + 64, PLANK)
    for x in range(x0, x0 + 71, 7):
        for z in range(z0, z0 + 65, 8):
            for k in range(2, 26):
                H.wset(x, y - k, z, HOUT)
    # stal: 8 boxen in een rij, open naar het zuiden
    sx, sz = x0 + 6, z0 + 4
    H.wfill(sx, y, sz, sx + 47, y, sz + 10, "minecraft:spruce_planks")
    for i in range(9):
        x = sx + i * 6
        for k in range(0, 6):
            H.wset(x, y + k, sz, "minecraft:stripped_spruce_log[axis=y]")
            H.wset(x, y + k, sz + 10, "minecraft:stripped_spruce_log[axis=y]")
        for z in range(sz + 1, sz + 9):
            H.wset(x, y + 1, z, "minecraft:spruce_fence")
    H.wfill(sx, y + 1, sz, sx + 47, y + 4, sz, "minecraft:spruce_planks")
    for i in range(8):
        x = sx + i * 6 + 3
        H.wset(x, y + 1, sz + 1, "minecraft:hay_block")
        H.wset(x - 1, y + 1, sz + 1, "minecraft:water_cauldron[level=3]")
        H.wset(x, y + 3, sz + 1, LAMP_G)
    # dak (zadeldak met roze pluisdak-banen) en voerzolder vol hooi
    for k in range(7):
        for x in range(sx - 1, sx + 49):
            b = "guhs:pluisdak" if k % 2 else "minecraft:dark_oak_planks"
            H.wset(x, y + 6 + k, sz - 1 + k, b, roof=True)
            H.wset(x, y + 6 + k, sz + 11 - k, b, roof=True)
    for x in range(sx + 1, sx + 47, 2):
        H.wset(x, y + 6, sz + 5, "minecraft:hay_block", roof=True)
        H.wset(x, y + 7, sz + 5, "guhs:roze_hooibaal", roof=True)
    H.label(sx + 24, y + 16, sz + 5, "Pluimstal")
    # oefenbaan: ovaal pad met hek
    ox, oz, rx, rz = x0 + 34, z0 + 40, 26, 13
    for a in np.linspace(0, 2 * math.pi, 400):
        for w in range(-2, 3):
            x = ox + (rx + w) * math.cos(a)
            z = oz + (rz + w) * math.sin(a)
            H.wset(x, y - 1, z, "minecraft:dirt_path")
        H.wset(ox + (rx + 3) * math.cos(a), y, oz + (rz + 3) * math.sin(a), "minecraft:spruce_fence")
        H.wset(ox + (rx - 3) * math.cos(a), y, oz + (rz - 3) * math.sin(a), "minecraft:spruce_fence")
    for (dx, dz) in ((-12, 0), (0, 0), (12, 0)):
        H.wset(ox + dx, y, oz + dz, "minecraft:hay_block")
        H.wset(ox + dx, y + 1, oz + dz, "minecraft:hay_block")
    # zweefplatform over de rand (naar het zuiden), met vlag
    for z in range(z0 + 64, z0 + 80):
        for x in range(x0 + 2, x0 + 11):
            H.wset(x, y - 1, z, PLANK)
        H.wset(x0 + 1, y, z, HEK)
        H.wset(x0 + 11, y, z, HEK)
    VK.vlag(H, H.vx(x0 + 6), H.vy(y), H.vz(z0 + 79), (1, 0), kleur="minecraft:pink_wool", h=4, w=4, paal_h=9)
    H.label(x0 + 6, y + 12, z0 + 78, "Zweefplatform")
    # brede helling naar het dek (langs de tak)
    trap(H, (x0 + 56, y - 1, z0 + 2), (372, DEK_Y, 8), breed=2)
    # pluimguhs + stalguhs worden in de close-ups als modellen toegevoegd (zie extra_pluimguhs)


def pluimguh_plekken():
    """Waar in de stal en op de baan pluimguhs staan: (x, y, z, yaw, variant)."""
    x0, y, z0 = STAL
    sx, sz = x0 + 6, z0 + 4
    out = [(sx + i * 6 + 3, y, sz + 6, 180, ["roze", "honing", "mint"][i % 3]) for i in (0, 2, 3, 5, 6)]
    out += [(x0 + 34 + 26, y, z0 + 40, 0, "roze"), (x0 + 34, y, z0 + 40 + 13, 270, "mint")]
    return out


def liftjes(H):
    """Honingliftjes: emmercabines aan kettingen tussen dek en trossen."""
    for (x, z, y0, y1) in ((440, -16, 196, DEK_Y), (515, 15, 200, DEK_Y), (575, -14, 204, DEK_Y)):
        yc, r, zc = tak(x)
        for y in range(y0, y1 + 6):
            H.wset(x, y, z, "minecraft:iron_chain[axis=y]")
        H.wset(x, y1 + 6, z, "minecraft:stripped_spruce_log[axis=x]")
        H.wset(x - 1, y1 + 6, z, "minecraft:stripped_spruce_log[axis=x]")
        cy = (y0 + y1) // 2
        H.wfill(x - 1, cy, z - 1, x + 1, cy, z + 1, "minecraft:honeycomb_block")
        for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            H.wset(x + dx, cy + 1, z + dz, "minecraft:spruce_fence")
        H.wset(x, cy + 1, z, "minecraft:barrel[facing=up,open=false]")


def honinglift_quads():
    """De grote Honinglift: twee kabels van de tak naar Mosvoet, met emmercabines (extra-quads, wereld)."""
    import mountkit as MK
    a = np.array([392.0, 206.0, 22.0])
    b = np.array([255.0, 100.0, 400.0])
    q = []
    for off in (-1.2, 1.2):
        o = np.array([off, 0, 0])
        pts = [a + o + (b - a) * t + np.array([0, -10 * math.sin(math.pi * t), 0]) for t in np.linspace(0, 1, 40)]
        for p0, p1 in zip(pts, pts[1:]):
            q += MK.balk_quads(p0, p1, 0.18, (70, 50, 40))
    for t in (0.18, 0.47, 0.76):
        p = a + (b - a) * t + np.array([0, -10 * math.sin(math.pi * t) - 2.5, 0])
        q += MK.balk_quads(p + [0, 0, 0], p + [0, 2.4, 0], 0.15, (70, 50, 40))
        for dy in (0.0, 0.4):
            q += MK.balk_quads(p + [-1.3, -1.6 + dy, 0], p + [1.3, -1.6 + dy, 0], 1.6, (236, 178, 60))
    return q


def luchtschip(H):
    """Het luchtschip (uit de kamp-kit) aangemeerd aan de mast, boeg naar het oosten."""
    L = 42
    S = SK.Scene(L + 8, 42, 30)
    KP.luchtschip(S, 2, 12, 15, L=L)
    mx, mz = MAST
    SK.stempel(H, S, mx - L + 2 - 2, DEK_Y - 13, mz + 12 - 15 + 12)


def imkerguhs(H):
    pos = [(462, -6, 90), (476, 4, 200), (468, 10, 330), (485, -3, 250), (500, 2, 160), (520, -5, 60),
           (530, 6, 300), (455, 3, 30), (600, 0, 90), (610, -4, 180), (380, 2, 90), (395, -5, 260)]
    for i, (x, z, a) in enumerate(pos):
        yc, r, zc = tak(x)
        H.wguh(x + 0.5, DEK_Y + 1, zc + z + 0.5, a, skin=["guh_golden", "guh", "guh_golden", "guh_bloesemguh"][i % 4])


def cafe_cel():
    """(cx, vloer-y, cz, R) van het café (onderste cel van de café-tros)."""
    cx, cz = CAFE
    t = tak_onder(cx)
    return cx, int(t - 19 - 10), cz, 9


def cafe_interieur(H):
    """Café De Zoete Raat: planken met een loper, honingtap aan de bar, tafeltjes, kussens, dansende guhs."""
    cx, yb, cz, R = cafe_cel()
    y = yb + 1
    # vloer: planken, loper van de luik-trap naar de bar
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            if zeshoek(np.array(x - cx), np.array(z - cz)) < R - 1.2:
                H.wset(x, yb, z, "minecraft:spruce_planks" if (x + z) % 5 else "minecraft:oak_planks")
    for z in range(cz - 5, cz + 8):
        H.wset(cx, yb, z, "minecraft:yellow_wool")
        H.wset(cx + 1, yb, z, "minecraft:orange_wool")
    # bar langs de noordwand, met honingtap en potjes
    for x in range(cx - 5, cx + 6):
        H.wset(x, y, cz - 5, "minecraft:stripped_oak_wood")
        H.wset(x, y + 1, cz - 5, "minecraft:oak_slab[type=bottom,waterlogged=false]")
        H.wset(x, y, cz - 7, "minecraft:beehive[facing=south,honey_level=5]" if x % 2 else
               "minecraft:barrel[facing=south,open=false]")
        H.wset(x, y + 2, cz - 7, "minecraft:bee_nest[facing=south,honey_level=5]" if x % 3 == 0 else
               "minecraft:honeycomb_block")
        H.wset(x, y + 3, cz - 7, "guhs:glimpotje[licht=3]" if x % 2 else "guhs:theepotje[facing=south]")
    H.wset(cx - 2, y + 1, cz - 5, "minecraft:honey_block")
    H.wset(cx - 2, y + 2, cz - 5, "minecraft:copper_lantern[hanging=false,waterlogged=false]")
    H.wset(cx + 3, y + 2, cz - 5, "guhs:theepotje[facing=south]")
    H.wguh(cx + 0.5, y, cz - 6 + 0.5, 180, skin="guh_golden")
    # tafeltjes met stoeltjes en kussens
    for (dx, dz) in ((-5, 1), (4, 2), (-2, 5)):
        H.wset(cx + dx, y, cz + dz, "guhs:guh_tafel[facing=south]")
        H.wset(cx + dx, y + 1, cz + dz, "guhs:theepotje[facing=south]" if dx < 0 else "guhs:glimpotje[licht=2]")
        H.wset(cx + dx - 1, y, cz + dz, "guhs:guh_stoel[facing=east]")
        H.wset(cx + dx + 1, y, cz + dz, "guhs:guh_stoel[facing=west]")
    H.wset(cx - 6, y, cz - 2, "guhs:pink_zitzak[facing=east]")
    H.wset(cx + 6, y, cz - 2, "guhs:orange_zitzak[facing=west]")
    H.wset(cx + 6, y, cz + 4, "guhs:pink_kussen[facing=west]")
    H.wset(cx - 7, y, cz + 3, "guhs:guh_bloempot")
    H.wset(cx + 7, y, cz - 1, "guhs:guh_bloempot")
    # lampen aan kettingen
    for (dx, dz) in ((-4, 0), (4, 0), (0, 4), (0, -2)):
        H.wset(cx + dx, y + 8, cz + dz, "minecraft:iron_chain[axis=y]")
        H.wset(cx + dx, y + 7, cz + dz, "minecraft:lantern[hanging=true,waterlogged=false]")
    H.wset(cx - 7, y + 4, cz - 3, LAMP_R)
    H.wset(cx + 7, y + 4, cz + 2, LAMP_G)
    # luik met trapje naar de cel erboven
    for k in range(1, 9):
        H.wset(cx + 5, y + k - 1, cz + 6, "minecraft:ladder[facing=north,waterlogged=false]")
    H.wset(cx + 5, y + 8, cz + 6, "minecraft:air")
    # gasten: zittend en dansend
    H.wguh(cx - 5.5 - 0.9, y, cz + 1.5, 90, sitting=True)
    H.wguh(cx + 4.5 + 1.0, y, cz + 2.5, 270, sitting=True, skin="guh_mint")
    H.wguh(cx - 1.5, y, cz + 5.5, 90, sitting=True, skin="guh_bloesemguh")
    for k, sk in enumerate(("guh", "guh_golden", "guh_golden")):
        a = k * 2.1
        H.wguh(cx + 1.5 + 1.6 * math.cos(a), y, cz + 1.5 + 1.6 * math.sin(a), (k * 120 + 40) % 360, skin=sk)


@functools.lru_cache(maxsize=1)
def bouw():
    H = SK.Wereld(*B, k=1)
    takrug(H)
    korfhuisjes(H)
    raattrossen(H)
    cafe_interieur(H)
    pluimstal(H)
    # [zone 3 kaart] the honey lifts are built by bouw3 (shafts that match the ride scenes)
    luchtschip(H)
    imkerguhs(H)
    return H


def plaats(W, guhs=True):
    H = bouw()
    if not W.raakt(*B):
        return
    x0, y, z0 = STAL
    W.leeg(x0 - 2, y - 1, z0 - 2, x0 + 72, y + 22, z0 + 82)  # bladeren boven het stalplateau weg
    W.leeg(355, DEK_Y - 1, -30, 400, DEK_Y + 30, 30)          # en boven het begin van het dek
    SK.stempel(W, H, B[0], B[1], B[2])
    W.labels += H.labels
    # [zone 3 kaart] the Honinglift cable is real blocks now (bouw3), no render quads
    if not guhs:
        W.guhs = []
