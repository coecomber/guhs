"""De Kaaszeppelinwerf (revamp): een LOS zwevend eiland naast de boom (noordoost), met een kabelbrug vanaf de
Werftak van Honingtak en 3 duidelijk gescheiden baasdekken (elk een eigen hoogte, eigen zeppelin, eigen kleur).

  Dok 1  Kaashangar      grondniveau (y ~161), westkant van het eiland      geel   - baas 1
  Dok 2  Scheepshelling  op een steigertoren (y 190), noordkant             oranje - baas 2
  Dok 3  Vlaggenschip    op de hoge masttoren (y 222), oostkant             grauwblauw (Mika) - baas 3
Route: kabelbrug -> havenpoort -> pad naar Dok 1 -> trap rond toren 2 -> trap rond toren 3.

    bouw(W)
"""
import math
import numpy as np
import schemerkit as SK
import boom2 as B
import plekken2 as P
from schemerkit import vnoise, hash3
from boom2 import PLANK, HOUT, HEK, LANTAARN, pol, mix

C = (251.0, -251.0)          # middelpunt van het eiland
TOP = 162.0
R_EIL = 70.0
POORT_HOEK = math.radians(135)       # richting boom
DOKKEN = [dict(naam="Dok 1 - Kaashangar (baas 1)", hoek=170, r=40, y=168, toren=0, kleur="minecraft:yellow_wool",
               rand="minecraft:yellow_concrete", zep=dict(L=34, as_="x", off=(-30, 14, 0), af=0.9)),
          dict(naam="Dok 2 - Scheepshelling (baas 2)", hoek=270, r=40, y=192, toren=1, kleur="minecraft:orange_wool",
               rand="minecraft:orange_concrete", zep=dict(L=30, as_="z", off=(0, 10, -28), af=0.55)),
          dict(naam="Dok 3 - Mika's vlaggenschip (baas 3)", hoek=10, r=38, y=224, toren=1,
               kleur="minecraft:light_blue_wool", rand="minecraft:light_gray_concrete",
               zep=dict(L=44, as_="x", off=(32, 16, 0), af=1.0))]


def dok_pos(d):
    a = math.radians(d["hoek"])
    return C[0] + d["r"] * math.cos(a), d["y"], C[1] + d["r"] * math.sin(a)


def poort_pos():
    return C[0] + (R_EIL - 6) * math.cos(POORT_HOEK), C[1] + (R_EIL - 6) * math.sin(POORT_HOEK)


def brug_begin():
    L, rr = P.htak_lijn("Werftak")
    x, yt, z = B.tak_top(L, rr, 148)
    return np.array([x, float(B.HONING_Y), z])      # [zone 3 kaart] the Werftak boardwalk level


def eiland_profiel(XX, ZZ):
    dx, dz = XX - C[0], ZZ - C[1]
    r = np.hypot(dx, dz)
    th = np.arctan2(dz, dx)
    R = R_EIL * (1 + 0.1 * vnoise(np.cos(th) * 100, np.sin(th) * 100, cell=40, seed=901, oct=2))
    q = np.clip(r / R, 0, 1.2)
    top = TOP + 4 * vnoise(XX, ZZ, cell=26, seed=902) - 6 * q ** 2 - 8 * np.clip((q - 0.75) / 0.25, 0, 1) ** 2
    bot = top - 4 - 78 * np.clip(1 - q, 0, 1) ** 1.2 + 5 * vnoise(XX, ZZ, cell=12, seed=903) * q
    return top, bot, q <= 1


def eiland(W):
    if not W.raakt(C[0] - 90, 60, C[1] - 90, C[0] + 90, 260, C[1] + 90):
        return
    XX, ZZ = W.XX, W.ZZ
    top, bot, m = eiland_profiel(XX, ZZ)

    def rots(y, mm):
        ids = W.kies(["minecraft:stone", "minecraft:tuff", "minecraft:andesite", "minecraft:deepslate",
                      "minecraft:calcite"], [4, 3, 2, 1, 0.6], XX, np.floor(y / 3.0) + XX * 0, ZZ, 910)
        mosr = y > top - 8
        return np.where(mosr, W.kies(["minecraft:moss_block", "minecraft:grass_block", "minecraft:dirt",
                                      "minecraft:pale_moss_block"], [3, 2, 1, 1], XX, y, ZZ, 914), ids)
    W.kolom(bot, top - 3, None, mask=m, laagfn=rots)
    W.kolom(top - 2.5, top - 1, "minecraft:dirt", mask=m)
    opp = W.kies(["minecraft:grass_block", "minecraft:moss_block", "minecraft:coarse_dirt", "minecraft:light_blue_terracotta",
                  "minecraft:pale_moss_block", "minecraft:gravel"], [5, 2, 1.5, 0.8, 0.8, 1], XX, 1, ZZ, 911)
    W.kolom(top, top, opp, mask=m)
    # hangende wortels en mos aan de onderkant (zachte, natuurlijke onderkant)
    rng = np.random.default_rng(912)
    for i in range(26):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(10, 60)
        x, z = C[0] + r * math.cos(a), C[1] + r * math.sin(a)
        t, b, _ = eiland_profiel(np.array([x]), np.array([z]))
        lng = rng.uniform(8, 24)
        W.buis([(x, b[0] + 3, z), (x + rng.uniform(-3, 3), b[0] - lng, z + rng.uniform(-3, 3))], [2.2, 0.6], None,
               mixfn=mix(W, ["minecraft:mangrove_roots", "minecraft:dark_oak_wood", "minecraft:moss_block"], [3, 2, 1],
                         913 + i))
    W._werf_top = top


def werf_top(x, z):
    t, b, m = eiland_profiel(np.array([x]), np.array([z]))
    return float(t[0])


TRAP_H = 17.0     # [zone 3 kaart] half size of the square stair round a dock tower (outside the deck, r 15)


def _vierkant(p, H):
    """Point on the square of half size H (start corner (-H, -H), going +x, then +z, -x, -z) at perimeter length p,
    with its outward normal."""
    p = p % (8 * H)
    side, f = int(p // (2 * H)), p % (2 * H)
    if side == 0:
        return (-H + f, -H), (0, -1)
    if side == 1:
        return (H, -H + f), (1, 0)
    if side == 2:
        return (H - f, H), (0, 1)
    return (-H, H - f), (-1, 0)


def _p_van_hoek(g, H):
    """Perimeter length of the point of the square in direction g."""
    best, bp = 1e9, 0.0
    for p in np.arange(0, 8 * H, 0.25):
        (qx, qz), _ = _vierkant(p, H)
        d = abs(B.wrap(math.atan2(qz, qx) - g))
        if d < best:
            best, bp = d, p
    return bp


def toren_trap(d):
    """[zone 3 kaart] The stair round a dock tower: [(x, z, stand, nx, nz)] every half block from the ground to the deck edge
    at the boss gate (towards the island centre)."""
    x, y, z = dok_pos(d)
    g = math.atan2(C[1] - z, C[0] - x)
    H = TRAP_H
    p_end = _p_van_hoek(g, H)
    L_trap = 2.0 * (y - 160) + 8
    (sx, sz), _ = _vierkant(p_end - L_trap, H)
    sh0 = float(math.floor(werf_top(x + sx, z + sz))) + 1
    L_trap = 2.0 * (y - sh0) + 8
    p0 = p_end - L_trap
    out = []
    for p in np.arange(p0, p_end + 0.01, 0.5):
        (qx, qz), (nx, nz) = _vierkant(p, H)
        sh = float(B.stand(sh0 - 1 + max(0.0, (p - p0 - 6) / 2.0)))
        out.append((x + qx, z + qz, min(sh, float(y)), nx, nz))
    return out


def toren(W, x, z, y0, y1, R=6, d=None):
    """Steigertoren (donker hout, kruisverbanden) met een trap eromheen ([zone 3 kaart] the stair: toren_trap, half steps)."""
    for (dx, dz) in ((-R, -R), (R, -R), (-R, R), (R, R)):
        W.wfill(x + dx, y0, z + dz, x + dx, y1, z + dz, "minecraft:stripped_dark_oak_log[axis=y]")
    for y in range(int(y0) + 8, int(y1), 8):
        for (a, b) in (((-R, -R), (R, -R)), ((R, -R), (R, R)), ((R, R), (-R, R)), ((-R, R), (-R, -R))):
            W.buis([(x + a[0], y, z + a[1]), (x + b[0], y, z + b[1])], [0.6, 0.6], "minecraft:dark_oak_planks")
            W.buis([(x + a[0], y - 7, z + a[1]), (x + b[0], y, z + b[1])], [0.5, 0.5], "minecraft:dark_oak_fence")
    if d is None:
        return
    pad = toren_trap(d)
    for i, (px, pz, sh, nx, nz) in enumerate(pad):
        for w in np.arange(-1.5, 1.51, 0.5):
            P.trede(W, px + nx * w, sh, pz + nz * w, "minecraft:dark_oak_planks",
                    "minecraft:dark_oak_slab[type=bottom,waterlogged=false]", lucht=4)
        ry = int(sh) if sh % 1 == 0 else int(math.floor(sh)) + 1
        if 4 < i < len(pad) - 3:
            for s in (-1, 1):
                W.wset(px + nx * s * 2.4, ry, pz + nz * s * 2.4, "minecraft:dark_oak_fence", keep=True)
        top = int(sh) - 1 if sh % 1 == 0 else int(math.floor(sh))
        if i % 10 == 0:
            g = math.floor(werf_top(px, pz))
            if g < top - 1:
                W.wfill(px, g, pz, px, top - 1, pz, "minecraft:dark_oak_fence")
        if i % 24 == 12:
            W.wset(px + nx * 2.4, ry + 1, pz + nz * 2.4, LANTAARN)
    for k in range(4):
        cx_, cz_ = ((-1, -1), (1, -1), (1, 1), (-1, 1))[k]
        best = min(pad, key=lambda q: abs(q[0] - (x + cx_ * TRAP_H)) + abs(q[1] - (z + cz_ * TRAP_H)))
        if abs(best[0] - (x + cx_ * TRAP_H)) + abs(best[1] - (z + cz_ * TRAP_H)) < 1.0:
            for ddx in np.arange(-1.5, 1.51, 0.5):
                for ddz in np.arange(-1.5, 1.51, 0.5):
                    P.trede(W, x + cx_ * TRAP_H + ddx, best[2], z + cz_ * TRAP_H + ddz, "minecraft:dark_oak_planks",
                            "minecraft:dark_oak_slab[type=bottom,waterlogged=false]", lucht=4)


def podium_trap(d):
    """[zone 3 kaart] Dok 1 stands on a stone podium: a straight half-step ramp from the ground up to the boss gate."""
    x, y, z = dok_pos(d)
    g = math.atan2(C[1] - z, C[0] - x)
    a = (x + 15.5 * math.cos(g), z + 15.5 * math.sin(g))
    for L in range(8, 40):
        b = (x + (15.5 + L) * math.cos(g), z + (15.5 + L) * math.sin(g))
        sb = math.floor(werf_top(*b)) + 1
        if L >= 2 * (y - sb) + 2:
            return a, b, sb
    return a, b, sb


def dek(W, d):
    x, y, z = dok_pos(d)
    if not W.raakt(x - 60, y - 80, z - 60, x + 60, y + 50, z + 60):
        return
    g = werf_top(x, z)
    if d["toren"]:
        toren(W, x, z, g, y - 1, R=6, d=d)
    else:
        W.cilinder(x, z, 16, g - 4, y - 2, None, mixfn=mix(W, ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                                                              "minecraft:cobblestone"], [3, 2, 1], 920))
        a_, b_, sb = podium_trap(d)
        P.helling(W, b_, sb, a_, y, breed=1.5, vol="minecraft:stone_bricks",
                  half="minecraft:stone_brick_slab[type=bottom,waterlogged=false]", hek="minecraft:dark_oak_fence",
                  steun="minecraft:stone_bricks", grond=lambda xx, zz: math.floor(werf_top(xx, zz)))
    # arena-dek: rond, met gekleurde rand (de kleur van de baas) en een hek
    W.cilinder(x, z, 15, y - 1, y - 1, None, mixfn=mix(W, ["minecraft:dark_oak_planks", "minecraft:spruce_planks",
                                                          "minecraft:stripped_dark_oak_wood"], [3, 2, 1], 921))
    W.cilinder(x, z, 15, y - 1, y - 1, d["rand"], hollow=1.5)
    W.cilinder(x, z, 15, y, y + 8, None)
    for k in range(48):
        a = 2 * math.pi * k / 48
        W.wset(x + 14.6 * math.cos(a), y, z + 14.6 * math.sin(a), "minecraft:iron_bars", keep=True)
    # vlaggen in de dokkleur
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        px, pz = x + 14 * math.cos(a), z + 14 * math.sin(a)
        W.wfill(px, y, pz, px, y + 8, pz, "minecraft:iron_bars")
        W.wfill(px + 1, y + 6, pz, px + 3, y + 8, pz, d["kleur"])
    # dragers onder het dek
    for k in range(6):
        a = k * math.pi / 3
        W.buis([(x + 13 * math.cos(a), y - 2, z + 13 * math.sin(a)), (x + 5 * math.cos(a), y - 14, z + 5 * math.sin(a))],
               [1, 1], "minecraft:dark_oak_log[axis=y]")
    # baaspoort ([zone 3 kaart] the rail is open there and a plank bridge reaches the stair)
    a = math.atan2(C[1] - z, C[0] - x)
    for rr in np.arange(12.0, 19.6, 0.5):
        for w_ in np.arange(-1.5, 1.51, 0.5):
            P.trede(W, x + rr * math.cos(a) - w_ * math.sin(a), float(y), z + rr * math.sin(a) + w_ * math.cos(a),
                    "minecraft:dark_oak_planks", "minecraft:dark_oak_slab[type=bottom,waterlogged=false]", lucht=4)
    px, pz = x + 15 * math.cos(a), z + 15 * math.sin(a)
    for s in (-1, 1):
        qx, qz = px - math.sin(a) * 3 * s, pz + math.cos(a) * 3 * s
        W.wfill(qx, y, qz, qx, y + 6, qz, "minecraft:iron_block")
    W.buis([(px - math.sin(a) * 3, y + 7, pz + math.cos(a) * 3), (px + math.sin(a) * 3, y + 7, pz - math.cos(a) * 3)],
           [0.8, 0.8], d["kleur"])
    # de zeppelin van dit dok
    zp = d["zep"]
    zeppelin(W, (x + zp["off"][0], y + zp["off"][1], z + zp["off"][2]), zp["L"], zp["as_"], zp["af"], d["kleur"])
    W.label(x, y + 24, z, d["naam"])


def zeppelin(W, c, L, as_, af=1.0, kleur="minecraft:yellow_wool"):
    """Kaaszeppelin: gatenkaas-romp (af = deel bekleed, de rest ijzeren ribben), gondel, vinnen, propeller."""
    cx, cy, cz = c
    rx, rz = (L / 2, L / 5.5) if as_ == "x" else (L / 5.5, L / 2)
    ry = L / 5.5
    lang = (lambda x, z: x - cx) if as_ == "x" else (lambda x, z: z - cz)

    def huid(x, y, z, m):
        gat = vnoise(x + z, y, cell=3.5, seed=930) > 0.55
        ids = W.kies(["guhs:gatenkaas_stenen", "guhs:belegen_kaas_stenen", "minecraft:yellow_concrete",
                      "minecraft:yellow_terracotta"], [3, 2, 1, 1], x, y, z, 931)
        ids = np.where(gat, W.id("minecraft:orange_terracotta"), ids)
        bekl = lang(x, z) < -L / 2 + L * af
        rib = (np.abs(lang(x, z)) % 4 < 1)
        return np.where(bekl, ids, np.where(rib, W.id("minecraft:iron_block"), 0)).astype(np.uint16)
    W.bol(c, rx, None, ry=ry, rz=rz, hollow=1.3, mixfn=huid)
    # gondel
    if as_ == "x":
        W.wfill(cx - 6, cy - ry - 4, cz - 2, cx + 6, cy - ry - 1, cz + 2, "minecraft:dark_oak_planks")
        W.wfill(cx - 5, cy - ry - 3, cz - 2, cx + 5, cy - ry - 2, cz - 2, "minecraft:light_blue_stained_glass")
        tail = (cx - L / 2 - 1, cy, cz)
        for s in (-1, 1):
            W.wfill(tail[0], cy + s * 2, cz, tail[0] + 5, cy + s * (ry + 2), cz, kleur)
            W.wfill(tail[0], cy, cz + s * 2, tail[0] + 5, cy, cz + s * (rz + 2), kleur)
        for k in range(4):
            a = k * math.pi / 2 + 0.4
            W.wset(tail[0] - 1, cy + 3 * math.sin(a), cz + 3 * math.cos(a), "minecraft:white_wool")
    else:
        W.wfill(cx - 2, cy - ry - 4, cz - 6, cx + 2, cy - ry - 1, cz + 6, "minecraft:dark_oak_planks")
        W.wfill(cx - 2, cy - ry - 3, cz - 5, cx - 2, cy - ry - 2, cz + 5, "minecraft:light_blue_stained_glass")
        tail = (cx, cy, cz + L / 2 + 1)
        for s in (-1, 1):
            W.wfill(cx, cy + s * 2, tail[2], cx, cy + s * (ry + 2), tail[2] - 5, kleur)
            W.wfill(cx + s * 2, cy, tail[2], cx + s * (rx + 2), cy, tail[2] - 5, kleur)
    # meertouwen naar het dek ([zone 3 kaart] chains)
    W.wfill(cx, cy - ry - 14, cz, cx, cy - ry - 5, cz, "minecraft:iron_chain[axis=y]")


def haven(W):
    """Havenpoort (aankomst van de brug), loodsen met kaas, kranen, paden tussen de dokken."""
    px, pz = poort_pos()
    if not W.raakt(C[0] - 90, 120, C[1] - 90, C[0] + 90, 250, C[1] + 90):
        return
    yk = 160
    # kade: stenen plateau op brughoogte
    W.cilinder(px, pz, 10, werf_top(px, pz) - 6, yk - 1, None, mixfn=mix(W, [
        "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cobblestone"], [3, 2, 1], 940))
    W.cilinder(px, pz, 10, yk, yk + 8, None)
    a = POORT_HOEK
    for s in (-1, 1):
        qx, qz = px + math.cos(a) * 8 - math.sin(a) * 4 * s, pz + math.sin(a) * 8 + math.cos(a) * 4 * s
        W.wfill(qx, yk, qz, qx, yk + 9, qz, "minecraft:stone_bricks")
        W.wset(qx, yk + 10, qz, LANTAARN)
    W.buis([(px + math.cos(a) * 8 + math.sin(a) * 4, yk + 9, pz + math.sin(a) * 8 - math.cos(a) * 4),
            (px + math.cos(a) * 8 - math.sin(a) * 4, yk + 9, pz + math.sin(a) * 8 + math.cos(a) * 4)], [1, 1],
           "minecraft:yellow_wool")
    W.label(px, yk + 16, pz, "Kaaszeppelinwerf: havenpoort")
    # paden: poort -> dok 1 -> voet toren 2 -> voet toren 3
    punten = [(px, pz)] + [dok_pos(d)[::2] for d in DOKKEN]
    for (a_, b_) in zip(punten, punten[1:]):
        n = int(math.hypot(b_[0] - a_[0], b_[1] - a_[1]))
        for i in range(n):
            t = i / n
            x = a_[0] + (b_[0] - a_[0]) * t + 4 * math.sin(t * math.pi)
            z = a_[1] + (b_[1] - a_[1]) * t
            y = werf_top(x, z)
            W.cilinder(x, z, 2, y, y, None, mixfn=mix(W, ["minecraft:dirt_path", "minecraft:gravel",
                                                          "minecraft:coarse_dirt"], [4, 1, 1], 941))
            if i % 9 == 0:
                W.wfill(x + 3, y + 1, z, x + 3, y + 3, z, HEK)
                W.wset(x + 3, y + 4, z, LANTAARN)
    # loodsen met kaaswielen en kratten, een kraan
    for k, (da, dr) in enumerate(((160, 20), (250, 22), (100, 30), (350, 18))):
        a = math.radians(da)
        x, z = C[0] + dr * math.cos(a), C[1] + dr * math.sin(a)
        y = int(round(werf_top(x, z))) + 1
        P.vakwerk(W, x, y, z, w=9, d=7, h=5, muur="minecraft:yellow_terracotta",
                  dak="minecraft:light_gray_concrete" if k % 2 else "minecraft:orange_terracotta")
        for j in range(3):
            W.cilinder(x + 7 + j * 2.5, z - 4, 1.2, y, y, "guhs:gatenkaas_stenen")
            W.wset(x - 7, y + j, z + 4, "minecraft:barrel[facing=up,open=false]")
    x, z = C[0] + 8, C[1] + 10
    y = werf_top(x, z)
    W.wfill(x, y, z, x, y + 22, z, "minecraft:iron_block")
    W.wfill(x, y + 22, z, x + 16, y + 22, z, "minecraft:iron_block")
    W.wfill(x + 15, y + 8, z, x + 15, y + 21, z, "minecraft:iron_chain[axis=y]")
    W.cilinder(x + 15, z, 1.6, y + 6, y + 7, "guhs:gatenkaas_stenen")


def kabelbrug(W):
    """[zone 3 kaart] The cable bridge: deck in half steps (planks / slabs) so it never needs a jump, fence rails, pylons,
    two sagging cables (fences) with chain hangers."""
    a = brug_begin()
    px, pz = poort_pos()
    b = np.array([px + math.cos(POORT_HOEK) * 10, 159.0, pz + math.sin(POORT_HOEK) * 10])
    lo = np.minimum(a, b) - 30
    hi = np.maximum(a, b) + 40
    if not W.raakt(*lo, *hi):
        return
    d = b - a
    lng = np.hypot(d[0], d[2])
    zij = np.array([-d[2], 0, d[0]]) / lng
    n = int(lng) * 2
    for i in range(n + 1):
        t = i / n
        p = a + d * t
        lev = a[1] + (b[1] - a[1]) * t - 5 * math.sin(math.pi * t)
        sh = float(B.stand(lev))
        for w in np.arange(-2, 2.1, 0.5):
            P.trede(W, p[0] + zij[0] * w, sh, p[2] + zij[2] * w, "minecraft:dark_oak_planks",
                    "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        ry = int(sh) if sh % 1 == 0 else int(math.floor(sh)) + 1
        for s in (-1, 1):
            if 0.02 < t < 0.98:
                W.wset(p[0] + zij[0] * 2.9 * s, ry, p[2] + zij[2] * 2.9 * s, HEK)
                if i % 24 == 12:
                    W.wset(p[0] + zij[0] * 2.9 * s, ry + 1, p[2] + zij[2] * 2.9 * s, P.LAMP_G)
    pyl_h = 26
    for (p, yb) in ((a, a[1]), (b, b[1])):
        for s in (-1, 1):
            x, z = p[0] + zij[0] * 3.9 * s, p[2] + zij[2] * 3.9 * s
            W.wfill(x, yb - 6, z, x, yb + pyl_h, z, "minecraft:stripped_dark_oak_log[axis=y]")
            W.wset(x, yb + pyl_h + 1, z, LANTAARN)
    for s in (-1, 1):
        o = zij * 3.9 * s
        for i in range(0, 161):
            t = i / 160
            p = a + d * t + o
            y = (a[1] + pyl_h) + ((b[1] + pyl_h) - (a[1] + pyl_h)) * t - 24 * math.sin(math.pi * t)
            W.wset(p[0], y, p[2], "minecraft:dark_oak_fence")
            if i % 8 == 4 and 0.05 < t < 0.95:
                yd = a[1] + (b[1] - a[1]) * t - 5 * math.sin(math.pi * t)
                W.wfill(p[0], yd + 2, p[2], p[0], y - 1, p[2], "minecraft:iron_chain[axis=y]")
    m = a + d * 0.5
    W.label(m[0], m[1] + 12, m[2], "kabelbrug (Werftak -> werf)")


def bouw(W):
    eiland(W)
    haven(W)
    for d in DOKKEN:
        dek(W, d)
    kabelbrug(W)
