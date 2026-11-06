"""De Hoedenpaden: a second layer in the Sporenwoud, 30-50 blocks up (fix round "verticaal", VOORSTEL_EPISCHER_ZONE1 idea 6).

Reads the giant mushrooms that sporenwoud() actually built (scan of its stamp, so it follows any change there), picks a
connected network of tall caps and builds on top of it:
  - a wooden deck on every cap of the network (fence rim, lanterns), rope bridges (3 wide, slab half steps, fence rails,
    spore lanterns hanging under them) between them;
  - lookout huts (plank floor, mushroom roof), an elite nest (the highest cap), a secret treasure room on a lone cap that
    only a narrow unlit plank bridge reaches (from behind a hut);
  - three ways up from the forest floor, all walkable in survival: De Wenteltrapzwam (stairs spiralling round a stem),
    Het Wortelpad (a slab ramp round a giant root) and De Holle Steel (a ladder inside a hollow stem).
POIs go into P (WereldPlekken): hoedenpad_*, geheim_hoedenschat. Falling off is allowed (you land in the forest).
"""
import math
import random

import numpy as np
from scipy import ndimage

import kaart as KA
import paadkit as PK

BRUG = dict(vol="minecraft:spruce_planks", slab="minecraft:spruce_slab", trap="minecraft:spruce_stairs", hek="minecraft:spruce_fence")
TRAP = dict(vol="minecraft:dark_oak_planks", slab="minecraft:dark_oak_slab", trap="minecraft:dark_oak_stairs", hek="minecraft:dark_oak_fence")
WORTEL = dict(vol="minecraft:mangrove_planks", slab="minecraft:mangrove_slab", trap="minecraft:mangrove_stairs", hek="minecraft:mangrove_fence")
STEEL = "minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]"
ROOD = "minecraft:red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]"
LANT = "minecraft:lantern[hanging=false,waterlogged=false]"
LANT_H = "minecraft:lantern[hanging=true,waterlogged=false]"
DOOS = (-600, 60, -170, 451, 120, 591)        # the Sporenwoud stamp box (x0, y0, z0, X, Y, Z)
HOOG_MIN, HOOG_MAX = 30, 50                   # cap top above the ground


# ================================================================ reading the forest
def zwammen(W, K):
    """The giant mushrooms of the Sporenwoud stamp: dicts x, z (cap centre), g (ground), T (top cap block), r (cap radius)."""
    st = next(s for s in W.stempels if s.naam == "sporenwoud")
    pal = st.pal
    stem = [i for i, s in enumerate(pal) if "steel" in s or "mushroom_stem" in s]
    hat = [i for i, s in enumerate(pal) if any(t in s for t in ("hoed", "mushroom_block", "wool", "knuffelblok", "froglight",
                                                                  "terracotta"))] + stem
    ox, oy, oz = st.off
    v = st.v
    isstem = np.isin(v, stem)
    ishat = np.isin(v, hat)
    lab, n = ndimage.label(isstem.sum(1) > 6)
    hy = np.where(ishat.any(1), ishat.shape[1] - 1 - np.argmax(ishat[:, ::-1, :], axis=1), -1)
    uit = []
    for k in range(1, n + 1):
        xs, zs = np.nonzero(lab == k)
        if len(xs) < 4:
            continue
        mx, mz = xs.mean(), zs.mean()
        best = None
        for x in range(int(mx) - 3, int(mx) + 4):
            for z in range(int(mz) - 3, int(mz) + 4):
                if 0 <= x < hy.shape[0] and 0 <= z < hy.shape[1]:
                    key = (hy[x, z], -math.hypot(x - mx, z - mz))
                    if best is None or key > best[0]:
                        best = (key, x, z)
        _, cx, cz = best
        T = int(hy[cx, cz]) + oy
        wx, wz = int(cx + ox), int(cz + oz)
        g = K.h(wx, wz)
        if T - g < 12:
            continue
        ext = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            d = 0
            while d < 25:
                x, z = cx + dx * (d + 1), cz + dz * (d + 1)
                if not (0 <= x < hy.shape[0] and 0 <= z < hy.shape[1]) or hy[x, z] < T - oy - 13 or hy[x, z] > T - oy:
                    break
                d += 1
            ext.append(d)
        uit.append(dict(x=wx, z=wz, g=g, T=T, r=min(ext)))
    return uit


class Ruimte:
    """Occupancy of the Sporenwoud box (terrain + every stamp so far)."""

    def __init__(self, W, K):
        x0, y0, z0, X, Y, Z = DOOS
        self.o = (x0, y0, z0)
        self.occ = PK.bezetting(W, K, x0, y0, z0, X, Y, Z)

    def vol(self, x, y, z):
        x0, y0, z0 = self.o
        i, j, k = x - x0, y - y0, z - z0
        if 0 <= i < self.occ.shape[0] and 0 <= j < self.occ.shape[1] and 0 <= k < self.occ.shape[2]:
            return bool(self.occ[i, j, k])
        return y < y0

    def doos_vrij(self, x1, y1, z1, x2, y2, z2):
        x0, y0, z0 = self.o
        a = self.occ[max(0, x1 - x0):x2 - x0 + 1, max(0, y1 - y0):y2 - y0 + 1, max(0, z1 - z0):z2 - z0 + 1]
        return not a.any()


# ================================================================ pieces
def dek(B, m):
    """The deck on a cap: planks at T+1 (stand T+2) out to rd+1 (the rim ring gets its fence later, see dek_rand())."""
    cx, cz, rd, L = m["x"], m["z"], m["rd"], m["L"]
    y = L // 2 - 1
    for x in range(cx - rd - 1, cx + rd + 2):
        for z in range(cz - rd - 1, cz + rd + 2):
            d = math.hypot(x - cx, z - cz)
            if d > rd + 1:
                continue
            B.dek(x, z, L, BRUG)
            if (x - cx) % 4 == 0 and (z - cz) % 4 == 0 and d < rd:
                B.set(x, y, z, "minecraft:stripped_spruce_wood[axis=y]")


def naast_laan(B, x, z):
    lanen = getattr(B, "laan", set())
    # V-INT: also the diagonal neighbours. A bridge lane that met the round rim only diagonally left a diagonal gap in the fence
    # ring, which u-hekken's gap fill (rightly) closes -> the cap had no way in (hoedenpad_netwerk.hoeden.7, -374 103 82).
    return any((x + dx, z + dz) in lanen for dx in (-1, 0, 1) for dz in (-1, 0, 1) if dx or dz)


def dek_rand(B, m):
    """Fence on the deck rim, open only right where a bridge lane touches it."""
    cx, cz, rd, L = m["x"], m["z"], m["rd"], m["L"]
    y = L // 2 - 1
    for x in range(cx - rd - 1, cx + rd + 2):
        for z in range(cz - rd - 1, cz + rd + 2):
            d = math.hypot(x - cx, z - cz)
            if rd < d <= rd + 1 and not naast_laan(B, x, z):
                B.set(x, y + 1, z, BRUG["hek"])
                if (x * 7 + z * 3) % 9 == 0:
                    B.set(x, y + 2, z, LANT)


def brug(B, a, La, b, Lb, sla_over, ra, rb, smal=False, licht=True, mat=BRUG):
    """A rope bridge between the centres a and b of two decks (radius ra / rb; stand levels La / Lb in halves, reached at
    the deck rims). sla_over(x, z) = this cell belongs to a deck/platform (left alone)."""
    breed = 0.5 if smal else 1.5
    D = math.dist(a, b)
    vrij = max(1.0, D - ra - rb - 2)

    def L_op(t):
        return PK.hoogte_op(min(1.0, max(0.0, (t * D - ra - 1) / vrij)), La, Lb)
    if not hasattr(B, "laan"):
        B.laan = set()
    rails = []
    for x, z, t, d in PK.lijn_cellen(a, b, breed, 1.0):
        if sla_over(x, z):
            continue
        L = L_op(t)
        if d <= breed:
            for dy in range(0, 3):
                s = B.get(x, L // 2 + dy, z)
                if s and ("fence" in s or "lantern" in s):
                    B.wis(x, L // 2 + dy, z)
            B.dek(x, z, L, mat)
            B.laan.add((x, z))
        else:
            rails.append((x, z, L))
    for x, z, L in rails:
        if (x, z) in B.laan:
            continue
        B.reling(x, z, L, mat, lamp=licht and (x * 5 + z * 11) % 13 == 0)
    if licht:
        # spore lanterns hanging under the bridge
        n = int(D // 8)
        for i in range(1, n):
            t = i / n
            x, z = int(round(a[0] + (b[0] - a[0]) * t)), int(round(a[1] + (b[1] - a[1]) * t))
            if sla_over(x, z):
                continue
            y = (L_op(t) - 1) // 2
            lang = 2 + i % 3
            for dy in range(1, lang):
                B.set(x, y - dy, z, "minecraft:iron_chain[axis=y,waterlogged=false]", keep=True)
            B.set(x, y - lang, z, "minecraft:pearlescent_froglight[axis=y]", keep=True)


def hut(B, m, deur, geheim=False):
    """A little lookout hut (5x5, plank floor = the deck) with a mushroom roof. deur = (dx, dz) side of the door."""
    cx, cz, L = m["x"], m["z"], m["L"]
    y = L // 2            # stand
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            B.set(x, y - 1, z, "minecraft:spruce_planks")
            rand = abs(x - cx) == 2 or abs(z - cz) == 2
            hoek = abs(x - cx) == 2 and abs(z - cz) == 2
            for dy in range(0, 3):
                if hoek:
                    B.set(x, y + dy, z, "minecraft:stripped_dark_oak_log[axis=y]")
                elif rand:
                    raam = dy == 1 and not geheim and (x - cx, z - cz) not in ((deur[0] * 2, deur[1] * 2),) and \
                        abs((x - cx) + (z - cz)) % 2 == 1
                    B.set(x, y + dy, z, "minecraft:glass_pane" if raam else "minecraft:spruce_planks")
                else:
                    B.carve(x, y + dy, z)
                    B.b.pop((x, y + dy, z), None)
    # door opening (no door: always open) + a carpet inside
    dx, dz = deur
    for dy in (0, 1):
        B.b.pop((cx + 2 * dx, y + dy, cz + 2 * dz), None)
        B.carve(cx + 2 * dx, y + dy, cz + 2 * dz)
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            B.set(x, y, z, "minecraft:red_carpet" if (x + z) % 2 else "minecraft:white_carpet")
    # roof: a flat ceiling + a small red mushroom cap with white spots
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.9:
                B.set(x, y + 3, z, ROOD if (x * 3 + z) % 5 else "minecraft:white_wool")
            if d <= 2.6:
                B.set(x, y + 4, z, ROOD if (x + z * 2) % 4 else "minecraft:white_wool")
            if d <= 1.2:
                B.set(x, y + 5, z, ROOD)
    B.set(cx, y + 2, cz, LANT_H)
    return [cx, y, cz]


def nest(B, m, open_):
    """The elite nest: a rim of logs and hay around the deck, moss + hay inside, eggs."""
    cx, cz, rd, L = m["x"], m["z"], m["rd"], m["L"]
    y = L // 2
    rng = random.Random(cx * 3 + cz)
    for x in range(cx - rd - 1, cx + rd + 2):
        for z in range(cz - rd - 1, cz + rd + 2):
            d = math.hypot(x - cx, z - cz)
            if d > rd + 1:
                continue
            if d > rd - 0.2:
                if open_(x, z):
                    continue
                B.b.pop((x, y + 1, z), None)
                B.set(x, y, z, rng.choice(["minecraft:hay_block[axis=y]", "minecraft:stripped_spruce_log[axis=x]",
                                           "minecraft:stripped_spruce_log[axis=z]", "minecraft:hay_block[axis=x]"]))
                if rng.random() < 0.45:
                    B.set(x, y + 1, z, "minecraft:hay_block[axis=y]" if rng.random() < 0.5 else "minecraft:spruce_fence")
                continue
            if d < rd - 1.5 and rng.random() < 0.55:
                B.set(x, y, z, "minecraft:moss_carpet" if rng.random() < 0.6 else "minecraft:yellow_carpet")
    for (dx, dz) in ((1, 1), (-1, 0), (0, -2)):
        B.set(cx + dx, y, cz + dz, "minecraft:sniffer_egg[hatch=0]")
    return [cx, y, cz]


# ================================================================ the three ways up
def toren_plek(K, R, W, nodes, kies, F, hoogte, dpad, al):
    """Find a free spot for an access tower of half-size F next to one of the network caps. Returns (cx, cz, g, node)."""
    beste = None
    for m in nodes:
        if m in kies:
            continue
        for ang in range(0, 360, 10):
            for D in range(m["rd"] + F + 4, m["rd"] + F + 28, 2):
                cx = int(round(m["x"] + D * math.cos(math.radians(ang))))
                cz = int(round(m["z"] + D * math.sin(math.radians(ang))))
                i, j = KA.idx(cx, cz)
                sl = (slice(i - F - 2, i + F + 3), slice(j - F - 2, j + F + 3))
                Hs = K.H[i - F:i + F + 1, j - F:j + F + 1]
                if Hs.size == 0 or Hs.max() - Hs.min() > 3 or K.H[sl].min() < Hs.max() - 4 or Hs.min() < 64:
                    continue
                if not K.g["woud"][sl].all() or (K.pad[sl] > 0.05).any() or W.bezet[sl].any() or (K.wl[sl] >= 0).any():
                    continue
                if any(math.hypot(cx - a, cz - b) < F + 12 for (a, b) in KA.LICHTSTRALEN):
                    continue
                if any(math.hypot(cx - a, cz - b) < 45 for (a, b) in al):
                    continue
                gmax = int(Hs.max())
                top = m["L"] // 2
                if not (HOOG_MIN <= top - gmax <= HOOG_MAX + 6):
                    continue
                if not R.doos_vrij(cx - F - 1, gmax + 1, cz - F - 1, cx + F + 1, top + 12, cz + F + 1):
                    continue
                if not brug_vrij(R, (cx, cz), m["L"], (m["x"], m["z"]), m["L"], F, m["rd"]):
                    continue
                score = abs(dpad[i, j] - 12) + 0.3 * D
                if beste is None or score < beste[0]:
                    beste = (score, cx, cz, gmax, m)
    return beste


def brug_vrij(R, a, La, b, Lb, ra, rb, toegestaan=8):
    bots = 0
    for x, z, t, d in PK.lijn_cellen(a, b, 1.5, 1.0):
        if math.hypot(x - a[0], z - a[1]) <= ra + 1.5 or math.hypot(x - b[0], z - b[1]) <= rb + 1.5:
            continue
        L = PK.hoogte_op(t, La, Lb)
        for y in range((L - 1) // 2 - 1, L // 2 + 4):
            if R.vol(x, y, z):
                bots += 1
    return bots <= toegestaan


def parasol(B, cx, cz, top, r):
    """A small red cap (roof over a tower platform)."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.3:
                continue
            t = d / r
            y1 = top - int(round(r * 0.45 * t ** 1.8))
            for y in range(y1 - 1, y1 + 1):
                B.set(x, y, z, "minecraft:white_wool" if y == y1 and (x * 5 + z * 3) % 7 == 0 and d > 1.5 else ROOD)
            if d > r - 1 and (x + z) % 3 == 0:
                B.set(x, y1 - 2, z, LANT_H)


def wenteltrap(B, cx, cz, g, Ltop, naar):
    """De Wenteltrapzwam: dark-oak stairs spiralling round a square mushroom stem, a platform under a red cap on top."""
    a_in, w = 3, 2
    beste = None
    for s0 in range(4):
        B2, info = PK.spiraal(cx, cz, a_in, w, 2 * (g + 1), Ltop, TRAP, start=s0, stijl="trap", lamp_elke=5)
        gx = np.mean([c[0] for c in info["gat"]]) if info["gat"] else cx
        gz = np.mean([c[1] for c in info["gat"]]) if info["gat"] else cz
        afst = math.hypot(gx - naar[0], gz - naar[1])
        if beste is None or afst > beste[0]:
            beste = (afst, B2, info)
    _, B2, info = beste
    top = Ltop // 2
    for x in range(cx - a_in + 1, cx + a_in):
        for z in range(cz - a_in + 1, cz + a_in):
            for y in range(g - 2, top + 9):
                B.set(x, y, z, STEEL)
    PK.voeg(B, B2)
    parasol(B, cx, cz, top + 9, 7)
    return info


def wortelpad(B, cx, cz, g, Ltop, naar):
    """Het Wortelpad: a 3-wide mangrove slab ramp spiralling round a giant root, gentle (1 up per 2)."""
    a_in, w = 5, 3
    beste = None
    for s0 in range(4):
        B2, info = PK.spiraal(cx, cz, a_in, w, 2 * (g + 1), Ltop, WORTEL, start=s0, stijl="slab", lamp_elke=6)
        gx = np.mean([c[0] for c in info["gat"]]) if info["gat"] else cx
        gz = np.mean([c[1] for c in info["gat"]]) if info["gat"] else cz
        afst = math.hypot(gx - naar[0], gz - naar[1])
        if beste is None or afst > beste[0]:
            beste = (afst, B2, info)
    _, B2, info = beste
    top = Ltop // 2
    rng = random.Random(cx + cz * 7)
    for x in range(cx - a_in + 1, cx + a_in):
        for z in range(cz - a_in + 1, cz + a_in):
            rand = max(abs(x - cx), abs(z - cz)) == a_in - 1
            for y in range(g - 2, top + 2):
                if rand:
                    s = rng.choice(["minecraft:dark_oak_wood[axis=y]", "minecraft:dark_oak_wood[axis=y]",
                                    "minecraft:stripped_dark_oak_wood[axis=y]", "minecraft:rooted_dirt", "minecraft:moss_block"])
                else:
                    s = "minecraft:dark_oak_wood[axis=y]"
                B.set(x, y, z, s)
    # roots splaying out at the foot + hanging roots under the ramp
    for k in range(10):
        a = k * math.pi / 5 + 0.3
        for d in range(a_in + 3, a_in + 7):
            x, z = int(round(cx + d * math.cos(a))), int(round(cz + d * math.sin(a)))
            B.set(x, g + (1 if d < a_in + 5 else 0), z, "minecraft:mangrove_roots", keep=True)
    PK.voeg(B, B2)
    # a mossy crown with a pink cap on top of the root
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            if math.hypot(x - cx, z - cz) <= 3.2:
                B.set(x, top + 2, z, "minecraft:moss_block")
    for y in range(top + 3, top + 8):
        B.set(cx, y, cz, "minecraft:dark_oak_wood[axis=y]")
    parasol(B, cx, cz, top + 10, 8)
    return info


def holle_steel(B, cx, cz, g, Ltop, naar):
    """De Holle Steel: a hollow mushroom stem with a ladder inside, doors at the foot and at the top platform."""
    top = Ltop // 2                     # stand level at the top
    yf = top - 1
    dx, dz = naar[0] - cx, naar[1] - cz
    # door side = away from the bridge (so the bridge lands on the platform, not in the door)
    if abs(dx) > abs(dz):
        deur = (-1 if dx > 0 else 1, 0)
    else:
        deur = (0, -1 if dz > 0 else 1)
    lad = (-deur[0], -deur[1])           # ladder on the wall opposite the door
    fac = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[lad]
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            d = math.hypot(x - cx, z - cz)
            if d > 3.2:
                continue
            binnen = abs(x - cx) <= 1 and abs(z - cz) <= 1
            for y in range(g - 2, top + 10):
                if binnen and g + 1 <= y <= top + 2:
                    B.carve(x, y, z)
                elif binnen and y == g:
                    B.set(x, y, z, "minecraft:spruce_planks")
                else:
                    licht = not binnen and d < 2.5 and (y - g) % 7 == 3 and (x - cx, z - cz) in ((deur[0] * 2, deur[1] * 2),)
                    B.set(x, y, z, "minecraft:shroomlight" if licht else STEEL)
    # floor at the top (not under the ladder), ladder from the bottom up to stand level
    lx, lz = cx + lad[0], cz + lad[1]
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if (x, z) != (lx, lz):
                B.set(x, yf, z, "minecraft:spruce_planks")
                B.c.discard((x, yf, z))
    for y in range(g + 1, top + 1):
        B.set(lx, y, lz, f"minecraft:ladder[facing={fac},waterlogged=false]")
    B.set(cx, top + 2, cz, LANT_H)
    # doorways (2 high) at the foot and at the top, both on the door side
    for (yy) in (g + 1, top):
        for k in (2, 3):
            for dy in (0, 1):
                B.b.pop((cx + deur[0] * k, yy + dy, cz + deur[1] * k), None)
                B.carve(cx + deur[0] * k, yy + dy, cz + deur[1] * k)
    # threshold block outside the foot door (ground stand g + 1)
    B.set(cx + deur[0] * 3, g, cz + deur[1] * 3, "minecraft:spruce_planks")
    # the platform round the stem at the top
    rp = 6
    plat = set()
    for x in range(cx - rp - 1, cx + rp + 2):
        for z in range(cz - rp - 1, cz + rp + 2):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.2 or d > rp + 1:
                continue
            B.dek(x, z, Ltop, TRAP)
            plat.add((x, z))
            if d > rp:
                t = max(0.0, min(1.0, ((x - cx) * dx + (z - cz) * dz) / (dx * dx + dz * dz)))
                if math.hypot(x - cx - t * dx, z - cz - t * dz) > 1.8:
                    B.set(x, top, z, TRAP["hek"])
                    if (x + 2 * z) % 5 == 0:
                        B.set(x, top + 1, z, LANT)
    # the door threshold inside the stem wall at the top is floor too
    for k in (2, 3):
        B.set(cx + deur[0] * k, yf, cz + deur[1] * k, "minecraft:spruce_planks")
    parasol(B, cx, cz, top + 10, 7.5)
    return dict(platform=plat, rp=rp, deur=deur, voet=[cx + deur[0] * 4, g + 1, cz + deur[1] * 4])


MOS = dict(vol="minecraft:moss_block", slab="minecraft:mossy_cobblestone_slab", trap="minecraft:mossy_cobblestone_stairs",
           hek="minecraft:spruce_fence")


def voet_heuvel(B, K, cx, cz, F, g):
    """Level ground under a tower (moss up to y g) and a soft mound round it: half a block lower per ring outwards."""
    for x in range(cx - F - 6, cx + F + 7):
        for z in range(cz - F - 6, cz + F + 7):
            k = max(abs(x - cx), abs(z - cz)) - F
            h = K.h(x, z)
            Lm = 2 * (g + 1) - max(0, k)
            if Lm <= 2 * (h + 1):
                continue
            for y in range(h + 1, (Lm - 1) // 2):
                B.set(x, y, z, "minecraft:dirt", keep=True)
            if B.get(x, (Lm - 1) // 2, z) is None and B.get(x, Lm // 2 - 1, z) is None:
                B.dek(x, z, Lm, MOS, kop=0)


def eigen_geheime_hoed(K, R, W, kandidaten):
    """No natural lone cap fits: grow our own small mushroom next to a hut/cap (stem + red cap) for the secret room."""
    for h in kandidaten:
        T = h["L"] // 2 - 2
        for D in range(h["rd"] + 11, h["rd"] + 24, 2):
            for ang in range(0, 360, 15):
                cx = int(round(h["x"] + D * math.cos(math.radians(ang))))
                cz = int(round(h["z"] + D * math.sin(math.radians(ang))))
                i, j = KA.idx(cx, cz)
                sl = (slice(i - 6, i + 7), slice(j - 6, j + 7))
                if not K.g["woud"][sl].all() or (K.pad[i - 2:i + 3, j - 2:j + 3] > 0.05).any() or W.bezet[sl].any() or                         (K.wl[sl] >= 0).any():
                    continue
                g = int(K.H[i - 1:i + 2, j - 1:j + 2].min())
                if T - g < 22:
                    continue
                if not R.doos_vrij(cx - 6, g + 1, cz - 6, cx + 6, T + 7, cz + 6):
                    continue
                m = dict(x=cx, z=cz, g=g, T=T, r=6, rd=3, L=h["L"], eigen=True)
                if not brug_vrij(R, (h["x"], h["z"]), h["L"], (cx, cz), h["L"], h["rd"], 3, 6):
                    continue
                return h, m
    return None


def groei_hoed(B, m):
    """Stem + red cap of our own secret mushroom (top block at m["T"])."""
    cx, cz, g, T = m["x"], m["z"], m["g"], m["T"]
    for y in range(g - 2, T - 1):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if math.hypot(x - cx, z - cz) <= 1.6:
                    B.set(x, y, z, STEEL)
    r = 6
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.3:
                continue
            t = d / r
            y1 = T - int(round(r * 0.55 * t ** 1.8))
            for y in range(y1 - 2, y1 + 1):
                B.set(x, y, z, "minecraft:white_wool" if y == y1 and (x * 3 + z * 5) % 6 == 0 and d > 1.5 else ROOD)


# ================================================================ main
def bouw(W, K, P):
    zw = zwammen(W, K)
    R = Ruimte(W, K)
    cand = [m for m in zw if HOOG_MIN <= m["T"] - m["g"] <= HOOG_MAX and m["r"] >= 6]
    for m in cand:
        m["rd"] = int(min(6, max(3, m["r"] * 0.45)))
        m["L"] = 2 * (m["T"] + 2)
    # feasible bridges
    rand = {}
    for i, a in enumerate(cand):
        for j in range(i + 1, len(cand)):
            b = cand[j]
            D = math.hypot(a["x"] - b["x"], a["z"] - b["z"])
            Lb = D - a["rd"] - b["rd"] - 2
            if Lb < 3 or Lb > 30 or abs(a["L"] - b["L"]) > 0.85 * Lb:
                continue
            if not brug_vrij(R, (a["x"], a["z"]), a["L"], (b["x"], b["z"]), b["L"], a["rd"], b["rd"]):
                continue
            rand[(i, j)] = D
    # the biggest connected group, trimmed to ~14 caps round its middle, spanning tree + a few loops
    buren = {i: set() for i in range(len(cand))}
    for (i, j) in rand:
        buren[i].add(j)
        buren[j].add(i)
    gezien, groepen = set(), []
    for i in range(len(cand)):
        if i in gezien or not buren[i]:
            continue
        g_, todo = set(), [i]
        while todo:
            k = todo.pop()
            if k in g_:
                continue
            g_.add(k)
            todo += list(buren[k])
        gezien |= g_
        groepen.append(g_)
    groep = max(groepen, key=len)
    mx = np.mean([cand[i]["x"] for i in groep])
    mz = np.mean([cand[i]["z"] for i in groep])
    start = min(groep, key=lambda i: math.hypot(cand[i]["x"] - mx, cand[i]["z"] - mz))
    net, rand_net = {start}, []
    while len(net) < 14:
        opties = [(rand[(min(i, j), max(i, j))], i, j) for i in net for j in buren[i] if j not in net]
        if not opties:
            break
        D, i, j = min(opties)
        net.add(j)
        rand_net.append((i, j))
    extra = sorted((D, i, j) for (i, j), D in rand.items() if i in net and j in net and (i, j) not in rand_net and (j, i) not in rand_net)
    for D, i, j in extra[:3]:
        rand_net.append((i, j))
    nodes = [cand[i] for i in net]
    print(f"  hoedenpaden: {len(cand)} hoge hoeden, netwerk {len(nodes)} hoeden, {len(rand_net)} bruggen")

    B = PK.Bouw()
    dpad = ndimage.distance_transform_edt(K.pad <= 0.1)

    # roles: the highest cap = elite nest; three big caps far apart = lookout huts
    nest_m = max(nodes, key=lambda m: m["T"])
    huts = []
    for m in sorted((m for m in nodes if m is not nest_m and m["rd"] >= 5), key=lambda m: -m["rd"]):
        if all(math.hypot(m["x"] - h["x"], m["z"] - h["z"]) > 45 for h in huts):
            huts.append(m)
        if len(huts) == 3:
            break
    # the secret: a lone cap outside the network, reached by a narrow unlit walk (from a hut deck if possible)
    geheim = None
    opties = []
    for h in nodes:
        for m in zw:
            if any(m is n or (m["x"], m["z"]) == (n["x"], n["z"]) for n in nodes) or m["r"] < 5 or m["T"] - m["g"] < 18:
                continue
            D = math.hypot(m["x"] - h["x"], m["z"] - h["z"])
            Lm = 2 * (m["T"] + 2)
            Lb = D - h["rd"] - 3 - 2
            if Lb < 4 or Lb > 30 or abs(Lm - h["L"]) > 0.85 * Lb:
                continue
            if not brug_vrij(R, (h["x"], h["z"]), h["L"], (m["x"], m["z"]), Lm, h["rd"], 3, 12):
                continue
            opties.append(((0 if h in huts else 1), D, id(m), h, dict(m, rd=3, L=Lm)))
    if opties:
        opties.sort(key=lambda o: o[:3])
        geheim = (opties[0][3], opties[0][4])
    else:
        geheim = eigen_geheime_hoed(K, R, W, huts + [n for n in nodes if n not in huts])
        if geheim is None:
            print("  hoedenpaden: geen geheime hoed gevonden")

    # access towers next to three different caps, spread out
    torens, al = [], ([(geheim[1]["x"], geheim[1]["z"])] if geheim else [])
    for soort, F in (("wenteltrap", 6), ("wortelpad", 9), ("holle_steel", 8)):
        p = toren_plek(K, R, W, nodes, [t[3] for t in torens], F, None, dpad, al)
        if p is None:
            print("  hoedenpaden: geen plek voor", soort)
            continue
        _, cx, cz, g, m = p
        torens.append((soort, cx, cz, m, g, F))
        al.append((cx, cz))
        i, j = KA.idx(cx, cz)
        W.bezet[i - F - 1:i + F + 2, j - F - 1:j + F + 2] = True

    # decks + where bridges meet them
    decks = list(nodes) + ([geheim[1]] if geheim else [])

    def in_dek(x, z):
        for m in decks:
            if math.hypot(x - m["x"], z - m["z"]) <= m["rd"] + 1:
                return True
        for (_, cx, cz, m, g, F) in torens:
            if math.hypot(x - cx, z - cz) <= F - 1.5:
                return True
        return False

    bruggen = [((cand[i]["x"], cand[i]["z"]), (cand[j]["x"], cand[j]["z"]), cand[i], cand[j]) for (i, j) in rand_net]
    toren_bruggen = [((cx, cz), (m["x"], m["z"]), m) for (_, cx, cz, m, g, F) in torens]

    for m in decks:
        dek(B, m)
    for (a, b, p, q) in bruggen:
        if p["L"] > q["L"]:
            a, b, p, q = b, a, q, p
        brug(B, a, p["L"], b, q["L"], in_dek, p["rd"], q["rd"])
    P_ = {}
    if geheim:
        h, m = geheim
        # the narrow plank walk to the secret cap, unlit
        brug(B, (h["x"], h["z"]), h["L"], (m["x"], m["z"]), m["L"], in_dek, h["rd"], m["rd"], smal=True, licht=False)
    # towers + their bridges
    for (soort, cx, cz, m, g, F) in torens:
        naar = (m["x"], m["z"])
        if soort == "wenteltrap":
            info = wenteltrap(B, cx, cz, g, m["L"], naar)
            voet = list(info["begin"][0]) if info["begin"] else [cx, cz]
            P_["hoedenpad_trap_wenteltrap"] = [int(voet[0]), g + 1, int(voet[1])]
        elif soort == "wortelpad":
            info = wortelpad(B, cx, cz, g, m["L"], naar)
            voet = list(info["begin"][0])
            P_["hoedenpad_trap_wortelpad"] = [int(voet[0]), g + 1, int(voet[1])]
        else:
            info = holle_steel(B, cx, cz, g, m["L"], naar)
            P_["hoedenpad_trap_holle_steel"] = info["voet"]
        voet_heuvel(B, K, cx, cz, F, g)
        P_[f"hoedenpad_top_{soort}"] = [cx + (F - 2 if soort != "holle_steel" else 5), m["L"] // 2, cz]
        brug(B, (cx, cz), m["L"], naar, m["L"], in_dek, F - 2, m["rd"])
    # rims, then the roles
    for m in decks:
        dek_rand(B, m)
    P_["hoedenpad_elitenest"] = nest(B, nest_m, lambda x, z: naast_laan(B, x, z))
    for k, h in enumerate(huts):
        P_[f"hoedenpad_uitkijkhut_{k + 1}"] = hut(B, h, (0, 1))
    if geheim:
        h, m = geheim
        dx, dz = h["x"] - m["x"], h["z"] - m["z"]
        deur = (int(np.sign(dx)), 0) if abs(dx) > abs(dz) else (0, int(np.sign(dz)))
        if m.get("eigen"):
            groei_hoed(B, m)
            i, j = KA.idx(m["x"], m["z"])
            W.bezet[i - 2:i + 3, j - 2:j + 3] = True
        schat = hut(B, m, deur, geheim=True)
        fac = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[deur]
        B.set(m["x"] - deur[0], schat[1], m["z"] - deur[1], f"minecraft:chest[facing={fac},type=single,waterlogged=false]")
        B.set(m["x"] - deur[0] + deur[1], schat[1], m["z"] - deur[1] + deur[0], "minecraft:gold_block")
        P_["geheim_hoedenschat"] = [m["x"], schat[1], m["z"]]
        P_["hoedenpad_schat_kist"] = [m["x"] - deur[0], schat[1], m["z"] - deur[1]]
    # creature spots: the plain caps of the network (deck centre)
    rollen = {id(nest_m)} | {id(h) for h in huts}
    k = 0
    for m in nodes:
        if id(m) in rollen:
            continue
        k += 1
        P_[f"hoedenpad_wezen_{k}"] = [m["x"] + 2, m["L"] // 2, m["z"]]
    global LAATSTE
    LAATSTE = B                     # for the walkability check (tools: dev only)
    B.stempel(W, "hoedenpaden", bezet=False)
    P.update({k: [int(v) for v in p] for k, p in P_.items()})
    P["hoedenpad_netwerk"] = {"hoeden": [[int(m["x"]), int(m["L"] // 2), int(m["z"])] for m in nodes]}
    return P_
