"""Plekken in de vernieuwde Schemerwortel (ronde 20): Honingtak, Schorsstad, liften, windkolommen, imkerdorpjes,
kampjes, ruïnes, nestjes, molentjes en het leven (deeltjes + guhs).

Alles in wereldcoördinaten t.o.v. de stamas; elke functie slaat zichzelf over buiten het venster.
    bouw(W)     alles (na boom2.bouw)
    leven(W)    deeltjes (bijen, vlinders, bloesem, vuurvliegjes, druppels) + figuren; aan het eind aanroepen
"""
import math
import numpy as np
import schemerkit as SK
import boom2 as B
import vrkit as VK
from schemerkit import hash3, vnoise
from boom2 import PLANK, HOUT, HEK, LAMP_R, LAMP_G, LANTAARN, HANGLAMP, pol, mix

RAAT = "minecraft:honeycomb_block"
GLAS = "minecraft:orange_stained_glass"
HY = B.HONING_Y
# Honingtak-takken: hoek (graden), lengte; de Werftak (315) loopt door naar de kabelbrug
HTAKKEN = [("Lifttak", 30, 122), ("Kruinlifttak", 150, 112), ("Imkerstraat", 225, 118), ("Werftak", 315, 138),
           # [wereldindeling] the two airship piers on long branch tips: west (from zone 2) and east (to zone 4)
           ("Weststeiger", 184, 200), ("Oosttaksteiger", 19, 218)]
STEIGERS = ("Weststeiger", "Oosttaksteiger")
WERFTAK_HOEK = 315


# ===================================================================================================== bouwstenen
def platform(W, cx, y, cz, R, hek=True, steun=8):
    W.cilinder(cx, cz, R, y - 1, y - 1, None, mixfn=mix(W, [PLANK, "minecraft:stripped_spruce_wood", PLANK,
                                                           "minecraft:spruce_slab[type=top,waterlogged=false]"],
                                                       [4, 1, 2, 0.5], int(cx * 7 + cz)))
    if hek:
        for a in np.linspace(0, 2 * math.pi, int(2 * math.pi * R), endpoint=False):
            if hash3(a * 30, cx, cz, 5) < 0.85:
                W.wset(cx + (R - 0.5) * math.cos(a), y, cz + (R - 0.5) * math.sin(a), HEK, keep=True)
    for k in range(4):
        a = k * math.pi / 2 + 0.6
        p0 = (cx + (R - 1) * math.cos(a), y - 1, cz + (R - 1) * math.sin(a))
        p1 = (cx + (R * 0.3) * math.cos(a), y - steun, cz + (R * 0.3) * math.sin(a))
        W.buis([p0, p1], [0.8, 0.8], HOUT)


def skep(W, cx, fy, cz, a_deur, soort=0, R=5.0):
    """Bijenkorfhuisje (skep): gestapelde strobanden, ronde deur, honingramen, guh-oortjes, lampion."""
    banden = [("minecraft:hay_block", "minecraft:yellow_terracotta"), ("guhs:roze_hooibaal", "minecraft:pink_terracotta"),
              ("minecraft:hay_block", RAAT)][soort % 3]
    hgt = int(R * 1.9)
    for k in range(hgt):
        t = k / hgt
        r = R * math.sqrt(max(0.0, 1 - t ** 2.2)) + 0.4
        W.cilinder(cx, cz, r, fy + k, fy + k, banden[1] if k % 3 == 2 else banden[0], hollow=1.3)
    dx, dz = math.cos(a_deur), math.sin(a_deur)
    for k in range(3):
        W.wset(cx + dx * (R - 0.3), fy + k, cz + dz * (R - 0.3), "minecraft:air")
        W.wset(cx + dx * (R + 0.4), fy + k, cz + dz * (R + 0.4), "minecraft:air")
    W.wset(cx + dx * (R + 0.6), fy + 3, cz + dz * (R + 0.6), LAMP_G)
    for s in (-1, 1):
        W.wset(cx - dz * s * (R - 0.2), fy + 2, cz + dx * s * (R - 0.2), "minecraft:ochre_froglight")
    if W.k >= 1:
        VK.oortjes(W, W.vx(cx), W.vy(fy + hgt - 1), W.vz(cz), (1, 0), gap=1, h=2, w=2)


def raathuis(W, cx, fy, cz, a_deur, R=4.5, h=7, dak="minecraft:pink_wool"):
    """Zeshoekig raathuisje: honingraatwanden, oranje ramen, een puntdak van pluis, deur met lampion."""
    bx = W._box(cx - R - 1, fy - 1, cz - R - 1, cx + R + 1, fy + h + 6, cz + R + 1)
    if bx is None:
        return
    sl, (x, y, z) = bx
    dxy = np.maximum(np.abs(x - cx) * 0.5 + np.abs(z - cz) * 0.866, np.abs(x - cx))
    v = W.v[sl]
    wand = (dxy <= R) & (dxy > R - 1.1) & (y >= fy) & (y < fy + h)
    v[wand] = W.id(RAAT)
    hoek = np.degrees(np.arctan2(z - cz, x - cx)) % 60
    raam = wand & (np.abs(hoek - 30) < 10) & (y >= fy + 2) & (y <= fy + 4)
    v[raam] = W.id("minecraft:ochre_froglight")
    v[(dxy <= R) & (y >= fy - 1) & (y < fy)] = W.id(PLANK)
    for k in range(6):
        rr = (R + 0.8) * (1 - k / 6.0)
        m = (dxy <= rr) & (y >= fy + h + k) & (y < fy + h + k + 1)
        v[m] = W.id(dak if k % 2 == 0 else "guhs:pluisdak")
    dx, dz = math.cos(a_deur), math.sin(a_deur)
    for k in range(3):
        W.wset(cx + dx * (R - 0.5), fy + k, cz + dz * (R - 0.5), "minecraft:air")
    W.wset(cx + dx * (R + 0.5), fy + 3, cz + dz * (R + 0.5), LAMP_R)


def vakwerk(W, cx, fy, cz, w=7, d=6, h=5, muur="minecraft:white_terracotta", dak="minecraft:orange_terracotta", deur=None):
    """Vakwerkhuisje (as-gericht): balken, witte muren, ramen met gloed, een getrapt dak.
    [zone 3 kaart] deur = the direction (angle) the door faces (default south); a real plank floor."""
    x1, x2, z1, z2 = cx - w // 2, cx + w // 2, cz - d // 2, cz + d // 2
    W.wfill(x1, fy - 1, z1, x2, fy - 1, z2, PLANK)
    W.wfill(x1, fy, z1, x2, fy + h, z2, muur)
    W.wfill(x1 + 1, fy, z1 + 1, x2 - 1, fy + h - 1, z2 - 1, "minecraft:air")
    for (xx, zz) in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        W.wfill(xx, fy, zz, xx, fy + h, zz, "minecraft:stripped_dark_oak_log[axis=y]")
    W.wfill(x1, fy + h, z1, x2, fy + h, z2, "minecraft:stripped_dark_oak_wood")
    for x in range(int(x1) + 2, int(x2) - 1, 2):
        W.wset(x, fy + 2, z1, "minecraft:ochre_froglight")
        W.wset(x, fy + 2, z2, "minecraft:ochre_froglight")
    dx, dz = (math.cos(deur), math.sin(deur)) if deur is not None else (0.0, 1.0)
    if abs(dx) > abs(dz):
        xd = x2 if dx > 0 else x1
        W.wfill(xd, fy, cz, xd, fy + 2, cz, "minecraft:air")
        W.wset(xd + (1 if dx > 0 else -1), fy + 3, cz, LAMP_G)
    else:
        zd = z2 if dz > 0 else z1
        W.wfill(cx, fy, zd, cx, fy + 2, zd, "minecraft:air")
        W.wset(cx, fy + 3, zd + (1 if dz > 0 else -1), LAMP_G)
    for k in range(d // 2 + 2):
        W.wfill(x1 - 1, fy + h + 1 + k, z1 - 1 + k, x2 + 1, fy + h + 1 + k, z2 + 1 - k, dak)


def touwbrug(W, p0, p1, zak=3.0, breed=1):
    """Rope bridge between two levels p0[1] and p1[1] (top block y; you stand one higher), sagging by zak.
    [zone 3 kaart] half steps (bottom slab or a full plank) so it never needs a jump; fences on both sides."""
    n = int(max(abs(p1[0] - p0[0]), abs(p1[2] - p0[2]), 2) + 1) * 2
    d = np.array([p1[2] - p0[2], 0, -(p1[0] - p0[0])], float)
    d /= np.linalg.norm(d) + 1e-9
    for i in range(n + 1):
        t = i / n
        x = p0[0] + (p1[0] - p0[0]) * t
        z = p0[2] + (p1[2] - p0[2]) * t
        lev = p0[1] + (p1[1] - p0[1]) * t - zak * math.sin(math.pi * t)
        sh = float(B.stand(lev))
        for s in np.arange(-breed, breed + 0.01, 0.5):
            trede(W, x + d[0] * s, sh, z + d[2] * s, "minecraft:spruce_planks",
                  "minecraft:spruce_slab[type=bottom,waterlogged=false]")
        if i % 4 == 0 and 0 < t < 1:
            for s in (-1, 1):
                W.wset(x + d[0] * s * (breed + 0.8), math.floor(sh) + (0 if sh % 1 == 0 else 1), z + d[2] * s * (breed + 0.8),
                       HEK, keep=True)


def trede(W, x, sh, z, vol, half, lucht=4):
    """[zone 3 kaart] One walking cell for stand height sh (multiple of 0.5): a full block at sh-1 or a bottom slab at
    floor(sh) (with nothing under it changed), air above."""
    if sh % 1 == 0:
        W.wset(x, sh - 1, z, vol)
        y0 = int(sh)
    else:
        W.wset(x, math.floor(sh), z, half)
        y0 = int(math.floor(sh)) + 1
    for h in range(lucht):
        W.wset(x, y0 + h, z, "minecraft:air")


def helling(W, a, sh_a, b, sh_b, breed=1.5, vol=PLANK, half="minecraft:spruce_slab[type=bottom,waterlogged=false]",
            hek=HEK, steun=HOUT, lucht=4, grond=None):
    """[zone 3 kaart] A walkway from (a = (x, z), stand sh_a) to (b, stand sh_b) in HALF steps (never a jump): the stand height
    goes linearly and is quantized to 0.5. Fence rails on both sides (hek=None: none), a post under it every 4 blocks down to
    grond(x, z) (a function giving the top block y below; None = 3 blocks of support)."""
    L = math.hypot(b[0] - a[0], b[1] - a[1])
    n = max(2, int(L * 2) + 1)
    ux, uz = (b[0] - a[0]) / max(L, 1e-9), (b[1] - a[1]) / max(L, 1e-9)
    nx, nz = -uz, ux
    for i in range(n + 1):
        t = i / n
        x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
        sh = float(B.stand(sh_a - 1 + (sh_b - sh_a) * t))
        for w in np.arange(-breed, breed + 0.01, 0.5):
            trede(W, x + nx * w, sh, z + nz * w, vol, half, lucht=lucht)
        top = int(sh) - 1 if sh % 1 == 0 else int(math.floor(sh))
        if hek is not None and 0.0 < t < 1.0:
            ry = int(sh) if sh % 1 == 0 else int(math.floor(sh)) + 1
            for s_ in (-1, 1):
                W.wset(x + nx * s_ * (breed + 0.9), ry, z + nz * s_ * (breed + 0.9), hek, keep=True)
        if i % 8 == 0:
            g = grond(x, z) if grond else top - 3
            if g < top - 1:
                for s_ in (-1, 1):
                    W.wfill(x + nx * s_ * breed, g, z + nz * s_ * breed, x + nx * s_ * breed, top - 1, z + nz * s_ * breed,
                            steun, keep=True)


def lampionsnoer(W, p0, p1, zak=2.5, elke=3):
    n = int(max(abs(p1[0] - p0[0]), abs(p1[2] - p0[2]), 2))
    for i in range(1, n):
        t = i / n
        x = p0[0] + (p1[0] - p0[0]) * t
        z = p0[2] + (p1[2] - p0[2]) * t
        y = p0[1] + (p1[1] - p0[1]) * t - zak * math.sin(math.pi * t)
        W.wset(x, y, z, "minecraft:iron_chain[axis=x]" if abs(p1[0] - p0[0]) > abs(p1[2] - p0[2]) else
               "minecraft:iron_chain[axis=z]", keep=True)
        if i % elke == 0:
            W.wset(x, y - 1, z, [LAMP_R, LAMP_G, "guhs:lampion_mint"][i // elke % 3], keep=True)


def kampvuur(W, x, y, z):
    W.wset(x, y, z, "minecraft:campfire[facing=south,lit=true,signal_fire=false,waterlogged=false]")


def tent(W, x, y, z, kleur="minecraft:pink_wool", L=5):
    for k in range(4):
        W.wfill(x - 3 + k, y + k, z, x - 3 + k, y + k, z + L, kleur)
        W.wfill(x + 3 - k, y + k, z, x + 3 - k, y + k, z + L, kleur if k < 3 else "minecraft:white_wool")
    W.wfill(x - 2, y, z, x + 2, y + 2, z + L - 1, "minecraft:air")
    W.wfill(x - 2, y - 1, z, x + 2, y - 1, z + L, "minecraft:brown_wool")


def kamp(W, x, z, y=None, guhs=("guh", "guh_mint", "guh_bloesemguh"), seed=0, label=None):
    if y is None:
        y = int(round(float(B.grondhoogte(np.array([x]), np.array([z]))[0]))) + 1
    if not W.raakt(x - 14, y - 4, z - 14, x + 14, y + 10, z + 14):
        return
    kleuren = ["minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool"]
    W.cilinder(x, z, 9, y - 1, y - 1, None, mixfn=mix(W, ["minecraft:coarse_dirt", "minecraft:dirt_path",
                                                         "minecraft:moss_block"], [2, 2, 1], 50 + seed))
    W.cilinder(x, z, 9, y, y + 6, None)
    kampvuur(W, x, y, z)
    tent(W, x - 7, y, z - 8, kleuren[seed % 4])
    tent(W, x + 5, y, z + 2, kleuren[(seed + 2) % 4], L=4)
    for (dx, dz) in ((-3, 2), (3, -2)):
        W.wset(x + dx, y, z + dz, "minecraft:stripped_oak_log[axis=x]")
    W.wset(x - 2, y, z + 4, "minecraft:barrel[facing=up,open=false]")
    W.wfill(x + 7, y, z - 6, x + 7, y + 3, z - 6, HEK)
    W.wset(x + 7, y + 4, z - 6, LANTAARN)
    for i, sk in enumerate(guhs):
        a = i * 2.1 + seed
        W.wguh(x + 3.2 * math.cos(a) + 0.5, y, z + 3.2 * math.sin(a) + 0.5, (90 - math.degrees(a)) % 360, skin=sk,
               sitting=(i % 2 == 0))
    W.deeltjes += [(x + 0.5, y + 2 + k * 1.3, z + 0.5 + 0.3 * k, (255, 200, 120), 0.35, 1.0, "stip") for k in range(5)]
    if label:
        W.label(x, y + 10, z, label)
    B.PLEKKEN_VRIJ.append((x, z, 13))


def molentje(W, x, y, z, a=0.0, h=8, L=5):
    """Klein windmolentje op een tak (wieken van wit/roze wol, schuin = draait)."""
    W.wfill(x, y, z, x, y + h, z, "minecraft:stripped_spruce_log[axis=y]")
    W.wset(x, y + h, z, "minecraft:spruce_planks")
    for k in range(4):
        b = a + k * math.pi / 2 + 0.3
        for t in np.arange(1, L + 0.5, 0.5):
            W.wset(x + 1, y + h + t * math.sin(b), z + t * math.cos(b),
                   "minecraft:white_wool" if k % 2 else "minecraft:pink_wool")


def vogelnest(W, x, y, z, r=2.5):
    W.bol((x, y, z), r, None, ry=1.2, ymax=y, mixfn=mix(W, ["minecraft:hay_block", "minecraft:stripped_spruce_wood",
                                                            "minecraft:spruce_wood"], [3, 2, 1], 77))
    W.wset(x, y, z, "minecraft:turtle_egg[eggs=3,hatch=0]")
    W.deeltjes.append((x + 1, y + 1.5, z, (110, 170, 240), 0.7, 0, "vlinder"))


def ruine(W, x, z, y=None, R=9, seed=0):
    if y is None:
        y = int(round(float(B.grondhoogte(np.array([x]), np.array([z]))[0])))
    if not W.raakt(x - R - 4, y - 4, z - R - 4, x + R + 4, y + 16, z + R + 4):
        return
    st = mix(W, ["minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:stone_bricks",
                 "minecraft:mossy_cobblestone", "minecraft:moss_block"], [3, 2, 2, 1, 1], 60 + seed)
    W.cilinder(x, z, R, y, y, None, mixfn=st)
    for k in range(7):
        a = k * 2 * math.pi / 7 + seed
        hh = 3 + int(9 * hash3(k, seed, 0, 61))
        p = (x + R * math.cos(a), y + 1, z + R * math.sin(a))
        W.cilinder(p[0], p[2], 1.3, y + 1, y + hh, None, mixfn=st)
        if k % 2 == 0:
            b = a + 2 * math.pi / 7
            q = (x + R * math.cos(b), y + hh, z + R * math.sin(b))
            W.buis([(p[0], y + hh, p[2]), ((p[0] + q[0]) / 2, y + hh + 2.5, (p[2] + q[2]) / 2), q], [1, 1, 1], None,
                   mixfn=st)
        for j in range(int(hh)):
            W.wset(p[0] + 1.4 * math.cos(a), y + 1 + j, p[2] + 1.4 * math.sin(a), "minecraft:vine[east=true]",
                   keep=True) if hash3(k, j, seed, 62) < 0.5 else None
    W.label(x, y + 14, z, "ruïne van het boomvolk")
    B.PLEKKEN_VRIJ.append((x, z, R + 4))


# ===================================================================================================== Honingtak
def dek_rand(th):
    r = 68 + 5 * vnoise(np.cos(th) * 200, np.sin(th) * 200, cell=50, seed=801)
    for (_, a, _) in HTAKKEN:
        r = r + 9 * np.exp(-(B.wrap(th - math.radians(a)) / 0.16) ** 2)
    return r


# [zone 3 kaart] layout functions (also read by plan3 for the POIs)
RING_R = 58.0
CELLEN = {"cafe": (190, 9.0, 10), "nestkapel": (262, 10.0, 13), "raattros": (10, 6.0, 8)}   # angle, R, height (hang under the deck)


def htak_lijn(naam):
    for (n, a, lengte) in HTAKKEN:
        if n == naam:
            return B.tak_lijn(math.radians(a), HY - 9, lengte, 6, 12, 5, krul=0.05, zak=2)
    raise KeyError(naam)


def ring_huizen():
    """Houses round the ring deck: (k, angle, cx, cz, kind 0 skep / 1 raathuis / 2 vakwerk)."""
    out = []
    n = 24
    for k in range(n):
        a = 2 * math.pi * k / n + 0.17
        if abs(B.wrap(a - B.TH_M)) < 0.5:
            continue
        if any(abs(B.wrap(a - math.radians(t[1]))) < 0.17 for t in HTAKKEN):
            continue
        if any(abs(B.wrap(a - math.radians(t[1]))) < 0.14 and t[2] < 186 for t in B.TAKLIJST):
            continue
        out.append((k, a, RING_R * math.cos(a), RING_R * math.sin(a), k % 3))
    return out


def tak_huizen():
    """Houses on platforms beside the Honingtak branches: (branch, j, cx, cz, door angle, bridge start (x, z), kind)."""
    out = []
    for (naam, a, lengte) in HTAKKEN:
        if naam not in ("Lifttak", "Imkerstraat", "Kruinlifttak"):
            continue
        L, rr = htak_lijn(naam)
        for j, s_ in enumerate((88, 110)):
            x, yt, z = B.tak_top(L, rr, s_)
            aa = math.radians(a) + (0.11 if j % 2 else -0.11) * (1 if naam != "Imkerstraat" else -1)
            cx, cz = s_ * math.cos(aa), s_ * math.sin(aa)
            zij = np.array([cx - x, cz - z])
            zij /= np.linalg.norm(zij) + 1e-9
            cx, cz = x + zij[0] * 12, z + zij[1] * 12
            out.append((naam, j, cx, cz, math.atan2(z - cz, x - cx), (x + zij[0] * 3, z + zij[1] * 3), j))
    return out


def honingtak(W):
    if not W.raakt(-170, HY - 40, -170, 170, HY + 30, 170):
        return
    # 1. de takken onder Honingtak; [zone 3 kaart] the boardwalk on top is LEVEL with the deck (166) on posts, so you walk
    #    off the deck onto it without a drop
    for (naam, a, lengte) in HTAKKEN:
        L, rr = htak_lijn(naam)
        B.bouw_tak(W, naam, L, rr, seed=int(a) + 77, blad=(naam not in ("Werftak", "Lifttak") + STEIGERS), bovenlaag=False)
        lo, hi = L.min(0) - 20, L.max(0) + 20
        if not W.raakt(lo[0], HY - 20, lo[2], hi[0], HY + 10, hi[2]):
            continue
        for i in range(0, len(L)):
            x, y, z = L[i]
            s = math.hypot(x, z)
            if s < 40:
                continue
            W.cilinder(x, z, 3.0, HY, HY, None, mixfn=mix(W, [PLANK, "minecraft:stripped_spruce_wood"], [4, 1], 810))
            W.cilinder(x, z, 3.0, HY + 1, HY + 4, None)
            if i % 3 == 0:
                W.wfill(x, y, z, x, HY - 1, z, HOUT)
        for i in range(4, len(L) - 1, 4):
            x, y, z = L[i]
            if math.hypot(x, z) < 40:
                continue
            d = L[i + 1] - L[i - 1]
            n = np.array([-d[2], 0, d[0]]) / (np.hypot(d[0], d[2]) + 1e-9)
            for s in (-1, 1):
                W.wset(x + n[0] * s * 3.2, HY + 1, z + n[2] * s * 3.2, HEK)
            if i % 8 == 0:
                for s in (-1, 1):
                    W.wfill(x + n[0] * s * 3.2, HY + 1, z + n[2] * s * 3.2, x + n[0] * s * 3.2, HY + 3,
                            z + n[2] * s * 3.2, HEK)
                    W.wset(x + n[0] * s * 3.2, HY + 4, z + n[2] * s * 3.2, LAMP_G if s > 0 else LAMP_R)
    # huisjes op uitstekende plateaus naast de tak
    for (naam, j, cx, cz, deur, (bx_, bz_), soort) in tak_huizen():
        a = [t[1] for t in HTAKKEN if t[0] == naam][0]
        platform(W, cx, HY + 1, cz, 7.5, steun=10)
        if soort == 0:
            skep(W, cx, HY + 1, cz, deur, soort=int(a) % 3, R=5)
        else:
            raathuis(W, cx, HY + 1, cz, deur, R=4.6, dak=["minecraft:pink_wool", "minecraft:yellow_wool",
                                                           "minecraft:orange_wool"][int(a) % 3])
        touwbrug(W, (bx_, HY, bz_), (cx + math.cos(deur) * 6.5, HY, cz + math.sin(deur) * 6.5), zak=0.6)
    # 2. het ringdek
    XX, ZZ, RR, TH = W.XX, W.ZZ, W.RR, W.TH
    Rs = B.r_stam(HY, TH - HY / 160.0)
    rij_buiten = Rs + B.RIJ["breed"] - 2.5
    plein = np.abs(B.wrap(TH - B.TH_M)) < 0.5
    rand = dek_rand(TH)
    m = (RR <= rand) & ((RR > rij_buiten + 0.5) | (plein & (RR > Rs - 1)))
    patroon = W.kies([PLANK, PLANK, "minecraft:stripped_spruce_wood", RAAT, "minecraft:spruce_planks"],
                     [5, 2, 1, 0.8, 1], XX, 0, ZZ, 820)
    ringen = (np.abs(((RR - 44) % 9) - 4.5) < 0.6) & ~plein
    patroon = np.where(ringen, W.id(RAAT), patroon)
    plein_m = plein & (RR > Rs + 4) & (RR < 62)
    patroon = np.where(plein_m, W.kies(["minecraft:yellow_terracotta", RAAT, "minecraft:orange_terracotta",
                                        "minecraft:stripped_birch_wood"], [3, 2, 1, 2], XX, 0, ZZ, 821), patroon)
    W.kolom(np.full(XX.shape, HY - 1.0), np.full(XX.shape, HY - 0.0), patroon, mask=m)
    W.kolom(np.full(XX.shape, HY + 1.0), np.full(XX.shape, HY + 6.0), "minecraft:air", mask=m & (RR < rand - 1.5))
    # leuning binnen (langs het rijpad) en buiten, met lampionpalen
    for a in np.linspace(0, 2 * math.pi, 900, endpoint=False):
        rs = float(B.r_stam(HY, np.array([a - HY / 160.0]))[0])
        ra = float(dek_rand(np.array([a]))[0])
        if abs(B.wrap(a - B.TH_M)) > 0.5:
            W.wset((rs + B.RIJ["breed"] - 1.6) * math.cos(a), HY + 1, (rs + B.RIJ["breed"] - 1.6) * math.sin(a), HEK)
        on_tak = any(abs(B.wrap(a - math.radians(t[1]))) < 0.07 for t in HTAKKEN)
        if not on_tak:
            W.wset((ra - 0.6) * math.cos(a), HY + 1, (ra - 0.6) * math.sin(a), HEK, keep=True)
    for k in range(40):
        a = 2 * math.pi * k / 40
        ra = float(dek_rand(np.array([a]))[0]) - 0.6
        if any(abs(B.wrap(a - math.radians(t[1]))) < 0.09 for t in HTAKKEN):
            continue
        x, z = ra * math.cos(a), ra * math.sin(a)
        W.wfill(x, HY + 1, z, x, HY + 4, z, HEK)
        W.wset(x, HY + 5, z, LAMP_R if k % 2 else LAMP_G)
        b = 2 * math.pi * (k + 1) / 40
        rb = float(dek_rand(np.array([b]))[0]) - 0.6
        lampionsnoer(W, (x, HY + 5, z), (rb * math.cos(b), HY + 5, rb * math.sin(b)), zak=1.5, elke=3)
    # dragers onder het dek + hangende raatcellen
    for k in range(30):
        a = 2 * math.pi * k / 30 + 0.05
        rs = float(B.r_stam(HY - 18, np.array([a]))[0])
        ra = float(dek_rand(np.array([a]))[0])
        pts = [pol(rs - 2, a, HY - 22), pol(rs + 12, a + 0.02, HY - 10), pol(ra - 4, a + 0.03, HY - 2)]
        W.buis(B._spline(pts, 5), np.linspace(2.2, 1.0, 11), None, mixfn=B._takmix(W, 830 + k, mos=0.1))
    for k in range(9):
        a = 2 * math.pi * k / 9 + 0.3
        if any(abs(B.wrap(a - math.radians(t[1]))) < 0.2 for t in HTAKKEN):
            continue
        if any(abs(B.wrap(a - math.radians(c[0]))) < 0.45 for c in CELLEN.values()):
            continue
        r = 55
        raatcel(W, r * math.cos(a), HY - 2, r * math.sin(a), 4.2 + (k % 2), 6 + (k % 3) * 2)
    for (ad, Rc, hc) in CELLEN.values():
        a = math.radians(ad)
        raatcel(W, 55 * math.cos(a), HY - 2, 55 * math.sin(a), Rc, hc)
    # 3. huizen rond de ring
    for (k, a, cx, cz, soort) in ring_huizen():
        deur = a + math.pi
        if soort == 0:
            skep(W, cx, HY + 1, cz, deur, soort=k, R=5.2)
        elif soort == 1:
            raathuis(W, cx, HY + 1, cz, deur, R=4.8, h=7, dak=["minecraft:pink_wool", "minecraft:orange_wool"][k % 2])
        else:
            vakwerk(W, cx, HY + 1, cz, w=7, d=6, h=5, dak=["minecraft:orange_terracotta", "minecraft:pink_terracotta"][k % 2],
                    deur=deur)
    honingplein(W)
    W.label(-70, HY + 14, -12, "Honingtak (ringstad)")


def raatcel(W, cx, ytop, cz, R, h):
    """Hangende zeshoekige raatcel onder het dek, met ramen en honingdruppels."""
    bx = W._box(cx - R - 1, ytop - h - 3, cz - R - 1, cx + R + 1, ytop, cz + R + 1)
    if bx is None:
        return
    sl, (x, y, z) = bx
    d = np.maximum(np.abs(x - cx) * 0.5 + np.abs(z - cz) * 0.866, np.abs(x - cx))
    yb = ytop - h
    v = W.v[sl]
    rand = (d <= R) & (d > R - 1.2)
    v[rand & (y >= yb) & (y <= ytop)] = W.id(RAAT)
    v[(d <= R) & (y >= yb) & (y < yb + 1)] = W.id(RAAT)
    hoek = np.degrees(np.arctan2(z - cz, x - cx)) % 60
    v[rand & (np.abs(hoek - 30) < 9) & (y >= yb + 2) & (y <= yb + 3)] = W.id("minecraft:ochre_froglight")
    for k in range(5):
        a = k * 2.4
        px, pz = cx + R * 0.5 * math.cos(a), cz + R * 0.5 * math.sin(a)
        for j in range(1 + k % 3):
            W.wset(px, yb - 1 - j, pz, "minecraft:honey_block", keep=True)
        W.deeltjes.append((px, yb - 4 - k % 3, pz, (240, 170, 40), 0.45, 0.3, "druppel"))


def honingplein(W):
    """Het Honingplein (zuid): hier komen wenteltrap, rijpad en ringdek samen. Fontein, marktkraampjes, bord."""
    a = B.TH_M
    c = pol(52, a, HY)
    if not W.raakt(c[0] - 20, HY - 4, c[2] - 20, c[0] + 20, HY + 14, c[2] + 20):
        return
    cx, _, cz = c
    W.cilinder(cx, cz, 4.5, HY + 1, HY + 1, RAAT)
    W.cilinder(cx, cz, 3.5, HY + 1, HY + 1, "minecraft:honey_block")
    W.wfill(cx, HY + 1, cz, cx, HY + 3, cz, RAAT)
    W.wset(cx, HY + 4, cz, "minecraft:ochre_froglight")
    W.deeltjes += [(cx + 2 * math.cos(k), HY + 3 + 0.4 * k, cz + 2 * math.sin(k), (250, 180, 40), 0.4, 0.4, "druppel")
                   for k in range(6)]
    kleuren = ["minecraft:pink_wool", "minecraft:yellow_wool", "minecraft:orange_wool", "minecraft:white_wool"]
    for k, da in enumerate((-0.38, -0.2, 0.2, 0.38)):
        p = pol(60, a + da, HY + 1)
        W.wfill(p[0] - 1, HY + 1, p[2] - 1, p[0] + 1, HY + 1, p[2] + 1, "minecraft:barrel[facing=up,open=false]")
        for (dx, dz) in ((-1.5, -1.5), (1.5, -1.5), (-1.5, 1.5), (1.5, 1.5)):
            W.wfill(p[0] + dx, HY + 1, p[2] + dz, p[0] + dx, HY + 3, p[2] + dz, HEK)
        W.wfill(p[0] - 2, HY + 4, p[2] - 2, p[0] + 2, HY + 4, p[2] + 2, kleuren[k])
    # boog met bord boven de plek waar het rijpad het dek raakt
    rs = float(B.r_stam(HY, np.array([a]))[0])
    for s in (-1, 1):
        p = pol(rs + B.RIJ["breed"], a + s * 0.12, HY + 1)
        W.wfill(p[0], HY + 1, p[2], p[0], HY + 7, p[2], "minecraft:stripped_spruce_log[axis=y]")
    W.buis([pol(rs + B.RIJ["breed"], a - 0.12, HY + 8), pol(rs + B.RIJ["breed"] + 1, a, HY + 10),
            pol(rs + B.RIJ["breed"], a + 0.12, HY + 8)], [0.8, 0.8, 0.8], "minecraft:orange_wool")
    W.label(cx, HY + 10, cz, "Honingplein (trap + rijpad + dek)")


# ===================================================================================================== Schorsstad
TOREN_RINGEN = [92 + 10 * i for i in range(7)]
TOREN_TOP = 160


def _toren_gaten():
    for y in TOREN_RINGEN:
        B.TRAP_GATEN.append((B.trap_hoek(y) % (2 * math.pi), 0.09, y - 0.6, y + 0.6))


_toren_gaten()


def schors_huizen():
    """[zone 3 kaart] Houses in the trunk wall along the stair: (y0 = stair level at the door, door angle, floor block y)."""
    out = []
    for y0 in np.arange(B.VOET + 6, 158, 4.5):
        if any(abs(y0 - yb) < 6 for (yb, _) in B.BORDES):
            continue
        a = B.trap_hoek(y0)
        out.append((float(y0), a, int(math.floor(float(B.stand(y0)) - 0.01)) if float(B.stand(y0)) % 1 else int(B.stand(y0)) - 1))
    return out


def schorsstad(W):
    """Binnen in de holle stam: straatjes langs de wenteltrap (huisjes in de wand), plein op de vloer, Lantaarntoren."""
    if not W.raakt(-40, B.VOET - 2, -40, 40, 165, 40):
        return
    Ri = B.R_IN
    # plein op de vloer: harsfontein, kraampjes, kampvuur, bankjes
    W.cilinder(0, 0, 6, B.VOET + 1, B.VOET + 1, "minecraft:mossy_stone_bricks")
    W.cilinder(0, 0, 5, B.VOET + 1, B.VOET + 1, "minecraft:honey_block")
    for k in range(10):
        a = k * 2 * math.pi / 10
        p = pol(11, a, B.VOET + 1)
        if k % 2:
            W.wset(p[0], p[1], p[2], "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
        else:
            W.wset(p[0], p[1], p[2], "minecraft:candle[candles=4,lit=true,waterlogged=false]")
    # Lantaarntoren: een zuil in het midden met lichtringen, touwbruggen naar de trap ([zone 3 kaart] 7 rings, every 10)
    for y in range(B.VOET + 1, TOREN_TOP):
        W.cilinder(0, 0, 2.2, y, y, "minecraft:stripped_spruce_wood" if y % 9 else "minecraft:spruce_wood")
    for i, y in enumerate(TOREN_RINGEN):
        W.cilinder(0, 0, 4.5, y, y, PLANK)
        W.cilinder(0, 0, 4.5, y + 1, y + 1, HEK, hollow=1)
        for k in range(8):
            a = k * math.pi / 4
            W.wset(4.8 * math.cos(a), y - 1, 4.8 * math.sin(a), LAMP_G if k % 2 else "minecraft:ochre_froglight")
        a = B.trap_hoek(y)
        for w in (-1, 0, 1):
            W.wset(4.5 * math.cos(a) - w * math.sin(a), y + 1, 4.5 * math.sin(a) + w * math.cos(a), "minecraft:air")
        touwbrug(W, pol(4.0, a, y), pol(B.TRAP_R0 + 1.5, a, y), zak=1.0)
    # huisjes in de wand langs de trap: elk huisje heeft zijn drempel op de trap
    for (y0, a, h) in schors_huizen():
        ca, sa = math.cos(a), math.sin(a)
        kleur = ["minecraft:stripped_oak_wood", "minecraft:stripped_birch_wood", "minecraft:stripped_cherry_wood"][int(y0) % 3]
        for dd in np.arange(Ri + 0.5, Ri + 7, 0.5):
            for w in np.arange(-3, 3.1, 0.5):
                x, z = dd * ca - w * sa, dd * sa + w * ca
                W.wset(x, h, z, PLANK)
                for k in range(1, 6):
                    W.wset(x, h + k, z, "minecraft:air")
                W.wset(x, h + 6, z, PLANK)
        for w in np.arange(-3, 3.1, 0.5):
            for k in range(1, 6):
                x, z = (Ri + 0.4) * ca - w * sa, (Ri + 0.4) * sa + w * ca
                b = kleur
                if abs(w) < 1 and k < 4:
                    b = "minecraft:air"
                elif 1.5 < abs(w) < 2.6 and k in (2, 3):
                    b = "minecraft:ochre_froglight" if int(y0 * 3) % 4 else GLAS
                elif k == 5:
                    b = "minecraft:spruce_planks"
                W.wset(x, h + k, z, b)
        W.wset((Ri - 0.4) * ca - 2.2 * sa, h + 1, (Ri - 0.4) * sa + 2.2 * ca,
               "minecraft:candle[candles=3,lit=true,waterlogged=false]")
        W.wset((Ri - 0.5) * ca, h + 5, (Ri - 0.5) * sa, HANGLAMP)
    # harsgloed: amberkristallen in de wand
    rng = np.random.default_rng(7)
    for k in range(40):
        a = rng.uniform(0, 2 * math.pi)
        y = rng.uniform(B.VOET + 4, 280)
        W.bol(pol(Ri + 0.5, a, y), rng.uniform(1.2, 2.2), None, ry=2, mixfn=mix(W, [
            "minecraft:ochre_froglight", "minecraft:resin_block", "minecraft:orange_stained_glass"], [2, 3, 1], 610))
    W.label(0, 130, 0, "Schorsstad (in de stam)")


def lichtbundels(sterkte=0.2):
    """Schuine zonnebundels door de kwastgaten naar binnen (doorschijnende extra-quads)."""
    q = []
    for (a, y, r) in B.KWASTGATEN:
        if y > 200:
            continue
        p0 = np.array([math.cos(a) * 30, y, math.sin(a) * 30])
        d = np.array([-math.cos(a) * 0.9, -0.55, -math.sin(a) * 0.9])
        p1 = p0 + d * 40
        side = np.array([-math.sin(a), 0, math.cos(a)]) * r * 0.8
        up = np.array([0, r * 0.8, 0])
        tex = np.ones((2, 2, 4), np.float32)
        tex[..., :3] = (1.0, 0.86, 0.55)
        tex[..., 3] = sterkte
        for s in (side, up):
            P = np.array([p0 - s, p1 - s, p0 + s], float)
            for PP in (P, P[[0, 2, 1]]):
                q.append((PP, np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float), tex, (1, 1, 1), True))
    return q


# ===================================================================================================== liften + wind
def lift_a():
    """Honinglift A: maaiveld -> Honingtak (aan de punt van de Lifttak). Geeft (x, z, y_onder, y_boven)."""
    L, rr = htak_lijn("Lifttak")
    x, yt, z = B.tak_top(L, rr, 120)
    a = math.radians(30)
    n = np.array([math.sin(a), -math.cos(a)])
    lx, lz = x + n[0] * 10, z + n[1] * 10
    g = float(B.grondhoogte(np.array([lx]), np.array([lz]))[0])
    return lx, lz, int(round(g)) + 1, HY, (x, z)        # [zone 3 kaart] top = the boardwalk level


def lift_b():
    """Honinglift B: Honingtak (Kruinlifttak) -> bovenop kruin 1."""
    L, rr = htak_lijn("Kruinlifttak")
    x, yt, z = B.tak_top(L, rr, 96)
    a = math.radians(150)
    n = np.array([-math.sin(a), math.cos(a)])
    lx, lz = x + n[0] * 9, z + n[1] * 9
    top = B.kruin_top(0, lx, lz)
    return lx, lz, HY, int(round(top)), (x, z)


def lift(W, lx, lz, y0, y1, naam, aanloop):
    """Honinglift: twee kettingen, een honingraat-cabine halverwege, een station onder en boven."""
    if not W.raakt(lx - 14, y0 - 4, lz - 14, lx + 14, y1 + 14, lz + 14):
        return
    # schacht door bladeren/hout vrijmaken
    W.cilinder(lx, lz, 2.6, y0 + 1, y1 + 10, None)
    for s in (-1.5, 1.5):
        for y in range(y0 + 1, y1 + 9):
            W.wset(lx + s, y, lz, "minecraft:iron_chain[axis=y]")
    # bovenframe met wiel
    for s in (-3, 3):
        W.wfill(lx + s, y1 + 1, lz, lx + s, y1 + 9, lz, "minecraft:stripped_dark_oak_log[axis=y]")
    W.wfill(lx - 3, y1 + 9, lz, lx + 3, y1 + 9, lz, "minecraft:stripped_dark_oak_log[axis=x]")
    W.cilinder(lx, lz, 1.6, y1 + 10, y1 + 10, RAAT)
    W.wset(lx, y1 + 11, lz, "minecraft:ochre_froglight")
    # stations: platform met hek, poortje, lantaarns, bord in de laagkleur
    for (y, kl) in ((y0, "minecraft:orange_wool"), (y1, "minecraft:pink_wool" if "kruin" in naam else "minecraft:orange_wool")):
        W.cilinder(lx, lz, 5.5, y, y, None, mixfn=mix(W, [PLANK, RAAT], [4, 1], 840))
        W.cilinder(lx, lz, 5.5, y + 1, y + 6, None)
        W.cilinder(lx, lz, 3.0, y, y, "minecraft:yellow_terracotta")
        for k in range(10):
            a = k * 2 * math.pi / 10
            if k in (0, 5):
                continue
            W.wset(lx + 5 * math.cos(a), y + 1, lz + 5 * math.sin(a), HEK)
        for s in (-1, 1):
            W.wfill(lx + 5 * s, y + 1, lz + 2, lx + 5 * s, y + 3, lz + 2, HEK)
            W.wset(lx + 5 * s, y + 4, lz + 2, LANTAARN)
        W.wfill(lx - 1, y + 1, lz - 5, lx + 1, y + 2, lz - 5, kl)
    if y0 < 100:
        # onderstation op de grond: een kleine sokkel tot op het maaiveld + pad
        W.cilinder(lx, lz, 5.5, y0 - 6, y0 - 1, None, mixfn=mix(W, ["minecraft:mossy_stone_bricks", "minecraft:stone_bricks"],
                                                                [2, 1], 841))
        W.cilinder(lx, lz, 9, y0 + 1, y0 + 12, None)
        # afdakje over het wachtplatform en een pad naar buiten (weg van de stam)
        for (dx, dz) in ((-4, -4), (4, -4), (-4, 4), (4, 4)):
            W.wfill(lx + dx, y0 + 1, lz + dz, lx + dx, y0 + 5, lz + dz, "minecraft:stripped_spruce_log[axis=y]")
        for k in range(3):
            W.cilinder(lx, lz, 5.5 - k * 1.6, y0 + 6 + k, y0 + 6 + k, None, hollow=1.6, mixfn=mix(
                W, ["minecraft:orange_wool", "minecraft:yellow_wool", RAAT], [2, 2, 1], 842))
        W.cilinder(lx, lz, 3.2, y0 + 6, y0 + 8, None)
        ua = math.atan2(lz, lx)
        for t in range(6, 26):
            px_, pz_ = lx + t * math.cos(ua), lz + t * math.sin(ua)
            gy = int(round(float(B.grondhoogte(np.array([px_]), np.array([pz_]))[0])))
            W.cilinder(px_, pz_, 1.6, gy, gy, "minecraft:dirt_path")
            if t % 6 == 0:
                W.wfill(px_ + 2.5 * math.sin(ua), gy + 1, pz_ - 2.5 * math.cos(ua), px_ + 2.5 * math.sin(ua), gy + 3,
                        pz_ - 2.5 * math.cos(ua), HEK)
                W.wset(px_ + 2.5 * math.sin(ua), gy + 4, pz_ - 2.5 * math.cos(ua), LANTAARN)
    # aanloop naar de tak / het blad
    ax, az = aanloop
    touwbrug(W, (ax, y0 if y0 > 100 else y1, az), (lx + (ax - lx) * 0.25, y0 if y0 > 100 else y1, lz + (az - lz) * 0.25),
             zak=0.3)
    # cabine halverwege (bij de onderste: net boven het station, zodat je ziet dat hij daar stopt)
    cy = int(y0 + (y1 - y0) * 0.42)
    W.wfill(lx - 1, cy, lz - 1, lx + 1, cy, lz + 1, RAAT)
    for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        W.wfill(lx + dx, cy + 1, lz + dz, lx + dx, cy + 2, lz + dz, HEK)
    W.wfill(lx - 1, cy + 3, lz - 1, lx + 1, cy + 3, lz + 1, "minecraft:yellow_wool")
    W.wset(lx, cy + 1, lz, "minecraft:barrel[facing=up,open=false]")
    for k in range(4):
        W.deeltjes.append((lx + 0.4 * k - 0.6, cy - 1 - k * 1.2, lz, (240, 170, 40), 0.4, 0.3, "druppel"))
    W.label(lx, y1 + 14, lz, naam)


WIND = []   # (x, z, y_onder, y_boven, naam)


def _op_tak(tak, s_):
    L, rr = B.tak_van(tak)
    x, yt, z = B.tak_top(L, rr, s_)
    return x, z, int(math.floor(yt))


def windkolommen_plan():
    """[zone 3 kaart] (id, x, z, y0 = top block at the foot, y1 = top block of the landing, naam).
    Branch tips -> kruin 1, kruin 1 -> 2, kruin 2 -> 3, the ground -> Honingtak (deck level)."""
    WIND.clear()
    for (wid, tak, s_) in (("werf_tak", "Westtak", 116), ("hangnest", "Bijentak", 112),
                           ("paddenbloesem_tak", "Zuidwesttak", 112), ("spiraal_kruin", "Molentak", 108)):
        x, z, yt = _op_tak(tak, s_)
        WIND.append((wid, x, z, yt, int(round(B.kruin_top(0, x, z))), "windkolom: " + tak + " -> kruin 1"))
    a = math.radians(250)
    x, z = 76 * math.cos(a), 76 * math.sin(a)
    WIND.append(("mosvoet_kruin", x, z, int(round(B.kruin_top(0, x, z))), int(round(B.kruin_top(1, x, z))),
                 "windkolom: kruin 1 -> 2"))
    a = math.radians(330)
    x, z = 46 * math.cos(a), 46 * math.sin(a)
    WIND.append(("lichtkever_kruin", x, z, int(round(B.kruin_top(1, x, z))), int(round(B.kruin_top(2, x, z))),
                 "windkolom: kruin 2 -> 3"))
    a = math.radians(205)
    x, z = 78 * math.cos(a), 78 * math.sin(a)
    WIND.append(("honingtak", x, z, int(round(float(B.grondhoogte(np.array([x]), np.array([z]))[0]))), HY,
                 "windkolom: maaiveld -> Honingtak"))
    return WIND


def windkolommen(W):
    for (wid, x, z, y0, y1, naam) in windkolommen_plan():
        if not W.raakt(x - 10, y0 - 4, z - 10, x + 10, y1 + 8, z + 10):
            continue
        W.cilinder(x, z, 3.6, y0 + 1, y1 + 6, None)            # gat door het blad
        for (y, top) in ((y0, False), (y1, True)):
            W.cilinder(x, z, 4.6, y, y, None, mixfn=mix(W, ["minecraft:pink_glazed_terracotta", "minecraft:pink_concrete",
                                                            "minecraft:white_concrete"], [2, 2, 1], 850))
            W.cilinder(x, z, 2.0, y, y, "minecraft:pearlescent_froglight")
            for k in range(8):
                a = k * math.pi / 4
                W.wset(x + 5.4 * math.cos(a), y + 1, z + 5.4 * math.sin(a),
                       "minecraft:pink_tulip" if k % 2 else "minecraft:peony[half=lower]")
            if top:
                # rand van het gat bovenin: bloesemkrans, zodat je ziet waar je uitkomt
                W.cilinder(x, z, 4.6, y, y, "minecraft:cherry_leaves", hollow=1)
        W.label(x, y1 + 8, z, naam)
        # bloesem die omhoog wervelt
        rng = np.random.default_rng(int(abs(x) + abs(z)))
        for i in range(int((y1 - y0) * 1.6)):
            t = rng.uniform(0, 1)
            a = rng.uniform(0, 2 * math.pi) + t * 12
            r = rng.uniform(0.5, 3.2)
            W.deeltjes.append((x + r * math.cos(a), y0 + 1 + t * (y1 - y0 + 4), z + r * math.sin(a),
                               [(255, 170, 210), (255, 220, 236), (250, 140, 190)][i % 3], 0.4, 0.35, "stip"))
        W.extra += SK.quads_naar_voxel(W, windzuil_quads(x, z, y0 + 1, y1 + 5))


def windzuil_quads(x, z, y0, y1, r=3.0, n=12):
    q = []
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    tex = np.ones((2, 2, 4), np.float32)
    tex[..., :3] = (1.0, 0.72, 0.86)
    tex[..., 3] = 0.16
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        p0 = np.array([x + r * math.cos(a0), y0, z + r * math.sin(a0)])
        p1 = np.array([x + r * math.cos(a1), y0, z + r * math.sin(a1)])
        P = np.array([p0, p1, p0 + [0, y1 - y0, 0]])
        q.append((P, cu, tex, (1, 1, 1), True))
        q.append((P[[0, 2, 1]], cu, tex, (1, 1, 1), True))
    return q


# ===================================================================================================== imkerdorpjes
DORPEN = [("Bijentak", "Imkerdorp Zoemrust"), ("Zuidwesttak", "Imkerdorp Raatweide"), ("Molentak", "Molenhoek")]


def dorp_huizen():
    """[zone 3 kaart] (tak, dorpnaam, j, cx, cz, yt, deur, (bx, bz) bridge start on the branch, n, zij, (x, z) branch point)."""
    out = []
    for (tak, naam) in DORPEN:
        L, rr = B.tak_van(tak)
        for j, s in enumerate((62, 88, 114)):
            x, yt, z = B.tak_top(L, rr, s)
            yt = int(round(yt))
            i = int(np.argmin(np.abs(np.hypot(L[:, 0], L[:, 2]) - s)))
            d = L[min(i + 1, len(L) - 1)] - L[i - 1]
            n = np.array([-d[2], 0, d[0]]) / (np.hypot(d[0], d[2]) + 1e-9)
            zij = 1 if j % 2 else -1
            cx, cz = x + n[0] * zij * 12, z + n[2] * zij * 12
            deur = math.atan2(z - cz, x - cx)
            out.append((tak, naam, j, cx, cz, yt, deur, (x + n[0] * zij * 3, z + n[2] * zij * 3), n, zij, (x, z), d, s))
    return out


def imkerdorpen(W):
    for (tak, naam) in DORPEN:
        L, rr = B.tak_van(tak)
        lo, hi = L.min(0) - 40, L.max(0) + 40
        if not W.raakt(*lo, *hi):
            continue
        for (tak_, naam_, j, cx, cz, yt, deur, (bx0, bz0), n, zij, (x, z), d, s) in dorp_huizen():
            if tak_ != tak:
                continue
            platform(W, cx, yt + 1, cz, 7, steun=9)
            if j == 1 and tak == "Molentak":
                vakwerk(W, cx, yt + 1, cz, w=7, d=6, h=5, dak="minecraft:pink_terracotta", deur=deur)
            elif j == 1:
                raathuis(W, cx, yt + 1, cz, deur, R=4.4)
            else:
                skep(W, cx, yt + 1, cz, deur, soort=j + len(tak), R=4.8)
            touwbrug(W, (bx0, yt, bz0), (cx - n[0] * zij * 6, yt, cz - n[2] * zij * 6), zak=0.5)
            # bijenkasten aan de overkant + bijen
            for k in range(3):
                bx_, bz_ = x - n[0] * zij * (3 + k * 1.2) + d[0] * 0.1 * k, z - n[2] * zij * (3 + k * 1.2) + d[2] * 0.1 * k
                W.wset(bx_, yt + 1, bz_, "minecraft:beehive[facing=north,honey_level=5]")
            if j == 2 or tak == "Molentak":
                molentje(W, x + n[0] * -zij * 2, yt + 1, z + n[2] * -zij * 2, a=s * 0.1, h=9 if tak == "Molentak" else 6,
                         L=6 if tak == "Molentak" else 4)
        x, yt, z = B.tak_top(L, rr, 90)
        W.label(x, yt + 18, z, naam)
        # vogelnestjes en eekhoorn-guhs op de tak
        for s in (40, 76, 102):
            x, yt, z = B.tak_top(L, rr, s)
            vogelnest(W, x + 2, int(round(yt)) + 1, z - 2)
            W.wguh(x - 1.5, int(round(yt)) + 1, z + 1.5, 90, skin="guh_choco")


# ===================================================================================================== leven
def leven(W, n_schaal=1.0):
    """Deeltjes over de hele boom: bijen bij Honingtak, vlinders boven de kruin, vallende bloesem onder de
    kruinranden, vuurvliegjes in de wortels en onder de kruin, druppelende honing."""
    rng = np.random.default_rng(999)
    D = W.deeltjes
    k = W.k
    nf = n_schaal * (1.0 if k >= 1 else 0.6)
    # vallende bloesem: onder de randen van de kruinverdiepingen, in banen naar beneden
    for i in range(int(2600 * nf)):
        s = rng.choice([0, 0, 0, 1, 2])
        kr = B.KRUIN[s]
        a = rng.uniform(0, 2 * math.pi)
        r = kr["R"] * rng.uniform(0.75, 1.15)
        y = rng.uniform(B.VOET, kr["top"] - 10) if s == 0 else rng.uniform(B.KRUIN[s - 1]["top"], kr["top"] - 8)
        x, z = r * math.cos(a) + (kr["top"] - y) * 0.15, r * math.sin(a)
        if W.binnen(x, y, z):
            D.append((x, y, z, [(255, 170, 210), (255, 214, 232), (246, 130, 180), (255, 236, 200)][i % 4],
                      0.35, 0, "stip"))
    # vlinders boven de kruinweides
    for i in range(int(500 * nf)):
        s = int(rng.integers(0, 3))
        kr = B.KRUIN[s]
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(kr["R"] * 0.3, kr["R"] * 0.95)
        x, z = r * math.cos(a), r * math.sin(a)
        y = B.kruin_top(s, x, z) + rng.uniform(1.5, 7)
        if W.binnen(x, y, z):
            D.append((x, y, z, [(255, 150, 200), (255, 200, 80), (170, 200, 255), (255, 255, 255)][i % 4],
                      0.7, 0, "vlinder"))
    # bijenzwermen rond Honingtak
    for i in range(int(500 * nf)):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(40, 130)
        c = (r * math.cos(a), B.HONING_Y + rng.uniform(1, 14), r * math.sin(a))
        for j in range(3):
            p = (c[0] + rng.normal(0, 1.2), c[1] + rng.normal(0, 0.8), c[2] + rng.normal(0, 1.2))
            if W.binnen(*p):
                D.append(p + ((250, 200, 40), 0.3, 0, "bij"))
    # vuurvliegjes in het Wortelduister en in de schemer onder de kruin
    for i in range(int(1500 * nf)):
        a = rng.uniform(0, 2 * math.pi)
        if i % 2:
            r = rng.uniform(10, 110)
            y = rng.uniform(16, 50)
            kl = (150, 255, 170) if i % 3 else (190, 170, 255)
        else:
            r = rng.uniform(40, 200)
            x0, z0 = r * math.cos(a), r * math.sin(a)
            y = float(B.grondhoogte(np.array([x0]), np.array([z0]))[0]) + rng.uniform(1.5, 8)
            kl = (220, 255, 140)
        x, z = r * math.cos(a), r * math.sin(a)
        if W.binnen(x, y, z):
            D.append((x, y, z, kl, 0.3, 1.0, "stip"))
    # vogels boven de kruin en rond de takken (kleine donkere v'tjes als stip)
    for i in range(int(80 * nf)):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(60, 240)
        y = rng.uniform(170, 310)
        x, z = r * math.cos(a), r * math.sin(a)
        if W.binnen(x, y, z):
            D.append((x, y, z, (90, 70, 90), 0.9, 0, "vlinder"))


def figuren(W):
    """Guhs op het Honingplein, de ring, in de Schorsstad en onderweg (alleen bij k=1)."""
    if W.k < 1:
        return
    rng = np.random.default_rng(31)
    skins = ["guh", "guh_golden", "guh_mint", "guh_bloesemguh", "guh_choco"]
    for i in range(30):
        a = B.TH_M + rng.uniform(-0.45, 0.45)
        r = rng.uniform(44, 66)
        W.wguh(r * math.cos(a), B.HONING_Y + 1, r * math.sin(a), rng.uniform(0, 360), skin=skins[i % 5])
    for i in range(40):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(42, 50)
        W.wguh(r * math.cos(a), B.HONING_Y + 1, r * math.sin(a), (90 - math.degrees(a) + 90) % 360, skin=skins[i % 5])
    for i in range(10):
        a = rng.uniform(0, 2 * math.pi)
        W.wguh(13 * math.cos(a), B.VOET + 1, 13 * math.sin(a), rng.uniform(0, 360), skin=skins[i % 5],
               sitting=i % 3 == 0)
    for y in (100, 124, 148, 190, 214):
        a = B.trap_hoek(y + 0.5)
        W.wguh(19.5 * math.cos(a), math.floor(y) + 1, 19.5 * math.sin(a), (180 - math.degrees(a)) % 360,
               skin=skins[y % 5])


# ===================================================================================================== alles
def bouw(W):
    """Alles behalve honingtak() (die moet vóór boom2.stam, zodat de stam de takvoeten weer uithakt)."""
    schorsstad(W)
    for (fn, naam) in ((lift_a, "Honinglift A: maaiveld -> Honingtak"), (lift_b, "Honinglift B: Honingtak -> kruin 1")):
        lx, lz, y0, y1, aan = fn()
        lift(W, lx, lz, y0, y1, naam, aan)
        B.PLEKKEN_VRIJ.append((lx, lz, 26))
    windkolommen(W)
    imkerdorpen(W)
    # kampjes, ruïnes en een paar losse dingen op de grond en in de kruin
    for (x, z, y, seed, label) in kampen():
        kamp(W, x, z, y=y, seed=seed, label=label)
    for (x, z, y, R, seed) in ruines():
        ruine(W, x, z, y=y, R=R, seed=seed)


def kampen():
    """[zone 3 kaart] (x, z, y stand or None = ground, seed, label): 3 travellers' camps on the ground, 1 on kruin 1."""
    out = [(172 * math.cos(1.75), 172 * math.sin(1.75), None, 1, "reizigerskamp"),
           (185 * math.cos(3.6), 185 * math.sin(3.6), None, 2, "reizigerskamp"),
           (165 * math.cos(5.55), 165 * math.sin(5.55), None, 3, None)]
    a = 0.4
    x, z = 108 * math.cos(a), 108 * math.sin(a)
    out.append((x, z, int(round(B.kruin_top(0, x, z))) + 1, 4, "kamp op kruin 1"))
    return out


def ruines():
    """[zone 3 kaart] (x, z, y floor or None, R, seed): 2 ruins on the ground, 1 on kruin 2."""
    a = 2.6
    x, z = 72 * math.cos(a), 72 * math.sin(a)
    return [(175 * math.cos(4.4), 175 * math.sin(4.4), None, 9, 1), (190 * math.cos(0.95), 190 * math.sin(0.95), None, 7, 2),
            (x, z, int(round(B.kruin_top(1, x, z))), 6, 3)]
