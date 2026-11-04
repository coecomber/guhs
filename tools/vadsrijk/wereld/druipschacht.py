"""De Druipschacht: one gigantic vertical cave (~80 high) in the Druipgrotten (fix round "verticaal", VOORSTEL idea 7).

A round shaft carved into the highest part of the Druipgrotten hills, with
  - a crystal skylight at the top (pink glass with crystal ribs, flush with the hill top: daylight falls in, nobody falls in);
  - waterfalls dropping from the dome into a lake on the crystal floor, dripstone and crystals on the ceiling and walls;
  - a 3-wide ledge path spiralling along the wall (slab half steps, wall rail on the void side) from the lake shore up to
    a balcony under the dome;
  - a short tunnel from the nearest cave node (it follows the real cave floor there) onto the spiral path.
The place is searched (highest ground, away from the existing caves), so it keeps fitting if the caves change.
POIs: druipschacht_* (richel = ledge spots, plafond = air under the ceiling for hanging creatures).
"""
import math
import random

import numpy as np

import kaart as KA
import paadkit as PK

STEEN = dict(vol="minecraft:polished_deepslate", slab="minecraft:polished_deepslate_slab",
             trap="minecraft:polished_deepslate_stairs", hek="minecraft:cobbled_deepslate_wall")
R_WAND = 14            # shaft radius (main part)
HOOGTE = 80            # floor -> skylight
MEER_R = 8             # lake radius
C_HELIX = 8.5          # path rise in halves per radian (inner lane at r 10.5: < 0.85 half per block)


def holte_cellen(W):
    st = next(s for s in W.stempels if s.naam == "druipgrotten")
    g = st.lut[st.v]
    xs, ys, zs = np.nonzero(g == 1)
    ox, oy, oz = st.off
    return np.stack([xs + ox, ys + oy, zs + oz], 1)


def kies_plek(W, K):
    from scipy import ndimage
    holte = holte_cellen(W)
    r = R_WAND + 3
    yy, xx = np.mgrid[-r:r + 1, -r:r + 1]
    fp = (xx ** 2 + yy ** 2) <= r * r
    mn = ndimage.minimum_filter(K.H, footprint=fp)
    beste = None
    for x in range(-540, -170, 3):
        for z in range(-540, -200, 3):
            i, j = KA.idx(x, z)
            if K.namen[K.gebied[i, j]] != "grotten":
                continue
            Hmin = int(mn[i, j])
            vloer = Hmin + 6 - HOOGTE
            if vloer < 26:
                continue
            d = np.hypot(holte[:, 0] - x, holte[:, 2] - z)
            dicht = holte[(d < r + 4) & (holte[:, 1] > vloer - 6)]
            if len(dicht):
                continue
            score = Hmin
            if beste is None or score > beste[0]:
                beste = (score, x, z, vloer)
    return beste


def profiel(y, vloer, top):
    """Shaft radius at height y: a slightly bulging cylinder, closing to a dome of radius 5 at the top."""
    koepel = top - 22
    if y <= koepel:
        return R_WAND + 1.2 * math.sin((y - vloer) / (koepel - vloer) * math.pi)
    t = min(1.0, (y - koepel) / (top - koepel))
    if y > top:
        return 0.0
    return 5 + (R_WAND - 5) * math.cos(t * math.pi / 2) ** 1.3


def bouw(W, K, P):
    plek = kies_plek(W, K)
    if plek is None:
        print("  druipschacht: geen plek gevonden")
        return {}
    _, cx, cz, vloer = plek
    rng = random.Random(77)
    Hc = K.h(cx, cz)
    top = vloer + HOOGTE
    print(f"  druipschacht: midden ({cx}, {cz}), vloer y {vloer}, top y {top} (heuvel {Hc})")
    B = PK.Bouw()
    # ------------------------------------------------ carve the shaft (never through the hill except the skylight)
    rmax = R_WAND + 2
    binnen = set()
    dak = {}
    RK = profiel(top, vloer, top) + 0.3          # skylight radius
    lens = set()
    ys = max(K.h(x, z) for x in range(cx - 6, cx + 7) for z in range(cz - 6, cz + 7) if math.hypot(x - cx, z - cz) <= RK)
    for x in range(cx - rmax, cx + rmax + 1):
        for z in range(cz - rmax, cz + rmax + 1):
            d = math.hypot(x - cx, z - cz)
            h = K.h(x, z)
            i, j = KA.idx(x, z)
            W.lo_extra[i, j] = min(W.lo_extra[i, j], vloer - 6)
            hoogste = None
            for y in range(vloer + 1, min(top, h - 1) + 1):
                if d <= profiel(y, vloer, top):
                    binnen.add((x, y, z))
                    hoogste = y
            if hoogste is not None:
                dak[(x, z)] = hoogste
                rib = abs(math.sin(4 * math.atan2(z - cz, x - cx))) < 0.2 or d < 1.2
                glas = "guh_kristal_blok" if rib else "minecraft:pink_stained_glass"
                if d <= RK:
                    # skylight: one flat crystal lens at the highest ground of the opening
                    B.set(x, ys, z, glas)
                    for y in range(hoogste + 1, ys):
                        binnen.add((x, y, z))
                    dak[(x, z)] = ys - 1
                    lens.add((x, z))
                    W.bezet[i, j] = True
                elif hoogste >= h - 1:
                    B.set(x, h, z, glas)
                    W.bezet[i, j] = True
    for (x, y, z) in binnen:
        B.carve(x, y, z)
    # the collar round the lens on the hill (closes the chimney where the ground is lower, steps of 1), crystals on it
    for x in range(cx - 9, cx + 10):
        for z in range(cz - 9, cz + 10):
            d = math.hypot(x - cx, z - cz)
            if not RK < d <= RK + 2 or (x, z) in lens:
                continue
            h = K.h(x, z)
            kop = ys - 1 if d <= RK + 1 else ys - 2
            for y in range(h + 1, kop + 1):
                B.set(x, y, z, rng.choice(["minecraft:calcite", "minecraft:calcite", "minecraft:amethyst_block", "minecraft:tuff"]))
            if rng.random() < 0.3:
                B.set(x, max(h, kop) + 1, z, rng.choice(["minecraft:amethyst_cluster[facing=up,waterlogged=false]",
                                                         "guh_kristal_cluster", "weidekristal_roze"]))
            i, j = KA.idx(x, z)
            W.bezet[i, j] = True
    # ------------------------------------------------ floor, lake, waterfalls
    for x in range(cx - rmax, cx + rmax + 1):
        for z in range(cz - rmax, cz + rmax + 1):
            d = math.hypot(x - cx, z - cz)
            if d > profiel(vloer + 1, vloer, top) + 0.5:
                continue
            if d <= MEER_R:
                diep = 3 if d < MEER_R - 2 else 1
                for y in range(vloer - diep, vloer + 1):
                    B.set(x, y, z, "minecraft:water")
                B.set(x, vloer - diep - 1, z, rng.choice(["minecraft:amethyst_block", "minecraft:calcite", "minecraft:clay"]))
            else:
                B.set(x, vloer, z, rng.choice(["minecraft:calcite", "minecraft:smooth_basalt", "minecraft:amethyst_block",
                                               "minecraft:calcite", "minecraft:dripstone_block"]))
                B.set(x, vloer - 1, z, "minecraft:calcite")
    vallen = []
    for k in range(3):
        a = k * 2 * math.pi / 3 + 0.4
        x, z = int(round(cx + 6 * math.cos(a))), int(round(cz + 6 * math.sin(a)))
        yt = dak.get((x, z))
        if yt is None:
            continue
        for y in range(vloer + 1, yt + 1):
            B.set(x, y, z, "minecraft:water")
        vallen.append([x, vloer + 1, z])
    # ------------------------------------------------ the spiral ledge
    # entrance: tunnel from the nearest cave node onto the path
    knopen = P.get("grot_knopen", {})
    occ_box = (cx - 80, vloer - 8, cz - 80, 161, HOOGTE + 30, 161)
    occ = PK.bezetting(W, K, *occ_box)
    node = None
    for naam, p in knopen.items():
        dd = math.hypot(p[0] - cx, p[2] - cz)
        if node is None or dd < node[0]:
            node = (dd, naam, p)
    _, nnaam, (nx, ny, nz) = node
    theta_e = math.atan2(nz - cz, nx - cx)
    # stand level of the real cave floor a few blocks into the node, towards the shaft
    ux, uz = (cx - nx) / node[0], (cz - nz) / node[0]
    sx, sz = int(round(nx + ux * 3)), int(round(nz + uz * 3))

    def vloer_op(x, z, rond):
        ox, oy, oz = occ_box[:3]
        col = occ[x - ox, :, z - oz]
        for y in range(rond + 4, rond - 8, -1):
            if col[y - oy - 1] and not col[y - oy] and not col[y - oy + 1]:
                return y
        return rond
    L_n = 2 * vloer_op(sx, sz, ny)
    R_pad = R_WAND - 1.5
    ex, ez = cx + R_pad * math.cos(theta_e), cz + R_pad * math.sin(theta_e)
    L_e = L_n                                   # flat-ish tunnel; the helix meets it here
    phi_e = theta_e
    phi0 = phi_e - (L_e - 2 * (vloer + 1)) / C_HELIX
    L_top = 2 * (top - 26)
    phi1 = phi0 + (L_top - 2 * (vloer + 1)) / C_HELIX

    def L_helix(phi):
        return int(math.floor(2 * (vloer + 1) + C_HELIX * (phi - phi0) + 0.5))
    lanen = set()
    rails = []
    for x in range(cx - rmax, cx + rmax + 1):
        for z in range(cz - rmax, cz + rmax + 1):
            d = math.hypot(x - cx, z - cz)
            th = math.atan2(z - cz, x - cx)
            for n in range(-3, 6):
                phi = th + 2 * math.pi * n
                if phi < phi0 - 0.05 or phi > phi1 + 0.05:
                    continue
                L = L_helix(min(max(phi, phi0), phi1))
                Rw = profiel(L // 2, vloer, top)
                if Rw - 3.5 < d <= Rw + 0.5:
                    B.dek(x, z, L, STEEN, kop=4)
                    lanen.add((x, z, L))
                    for y in range((L - 1) // 2 - 2, (L - 1) // 2):
                        B.set(x, y, z, "minecraft:cobbled_deepslate", keep=True)
                elif Rw - 4.5 < d <= Rw - 3.5:
                    rails.append((x, z, L))
    for (x, z, L) in rails:
        if any((x, z, L2) in lanen for L2 in range(L - 3, L + 4)):
            continue
        B.reling(x, z, L, STEEN, lamp=(x * 3 + z * 7) % 7 == 0)
        B.set(x, (L - 1) // 2 - 1, z, "minecraft:cobbled_deepslate", keep=True)
    # the tunnel (3 wide, carved 4 high, deck follows from the node floor to the ledge)
    tunnel = []
    for x, z, t, d in PK.lijn_cellen((sx, sz), (ex, ez), 1.5, 1.0):
        if math.hypot(x - cx, z - cz) > R_pad - 1 and t > 0.95:
            continue
        L = PK.hoogte_op(t, L_n, L_e)
        if d <= 1.5:
            B.dek(x, z, L, STEEN, kop=4)
            tunnel.append((x, z))
            i, j = KA.idx(x, z)
            W.lo_extra[i, j] = min(W.lo_extra[i, j], L // 2 - 6)
            assert K.h(x, z) >= L // 2 + 6, "druipschacht: tunnel te dicht bij het oppervlak"
        else:
            for y in range(L // 2 - 1, L // 2 + 4):
                if B.get(x, y, z) is None and (x, y, z) not in binnen:
                    B.set(x, y, z, "minecraft:tuff", keep=True)
    # the tunnel mouth into the node: lights
    B.set(sx, L_n // 2 + 3, sz, "minecraft:ochre_froglight[axis=y]")
    # ------------------------------------------------ the balcony at the top end + ledge niches
    rich = []
    for k, f in enumerate((0.18, 0.42, 0.66, 0.9)):
        phi = phi0 + (phi1 - phi0) * f
        L = L_helix(phi)
        Rw = profiel(L // 2, vloer, top)
        bx, bz = cx + (Rw - 2) * math.cos(phi), cz + (Rw - 2) * math.sin(phi)
        for x in range(int(bx) - 3, int(bx) + 4):
            for z in range(int(bz) - 3, int(bz) + 4):
                if math.hypot(x - bx, z - bz) <= 2.6 and math.hypot(x - cx, z - cz) > Rw - 5.5:
                    for dy in range(1, 3):
                        B.wis(x, L // 2 + dy - 1, z) if "wall" in (B.get(x, L // 2 + dy - 1, z) or "") else None
                    B.dek(x, z, L, STEEN, kop=4)
        rich.append([int(round(cx + (Rw - 3) * math.cos(phi))), (L + 1) // 2, int(round(cz + (Rw - 3) * math.sin(phi)))])
    # balcony
    phi = phi1
    L = L_helix(phi1)
    Rw = profiel(L // 2, vloer, top)
    bx, bz = cx + (Rw - 3) * math.cos(phi), cz + (Rw - 3) * math.sin(phi)
    for x in range(int(bx) - 5, int(bx) + 6):
        for z in range(int(bz) - 5, int(bz) + 6):
            dd = math.hypot(x - bx, z - bz)
            if dd <= 4.2:
                for dy in range(0, 2):
                    s = B.get(x, L // 2 + dy, z) or ""
                    if "wall" in s or "lantern" in s:
                        B.wis(x, L // 2 + dy, z)
                B.dek(x, z, L, STEEN, kop=4)
                B.set(x, (L - 1) // 2 - 1, z, "minecraft:cobbled_deepslate", keep=True)
            elif dd <= 5.2 and math.hypot(x - cx, z - cz) < Rw - 1:
                if not any((x, z, L2) in lanen for L2 in range(L - 3, L + 4)):
                    B.reling(x, z, L, STEEN, lamp=(x + z) % 4 == 0)
    balkon = [int(round(bx)), (L + 1) // 2, int(round(bz))]
    # ------------------------------------------------ crystals, dripstone, light
    plafond = []
    for (x, y, z) in binnen:
        if (x, y + 1, z) not in binnen and y + 1 < K.h(x, z) and B.get(x, y, z) is None:
            r = rng.random()
            if r < 0.12:
                B.set(x, y, z, "minecraft:amethyst_cluster[facing=down,waterlogged=false]")
            elif r < 0.22:
                B.set(x, y, z, "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]")
                B.set(x, y + 1, z, "minecraft:dripstone_block")
            elif r < 0.27:
                B.set(x, y + 1, z, rng.choice(["guh_kristal_blok", "minecraft:ochre_froglight[axis=y]", "minecraft:amethyst_block"]))
            if r > 0.985 and math.hypot(x - cx, z - cz) > 6:
                plafond.append([x, y - 1, z])
        for dx, dz, fac in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            q = (x + dx, y, z + dz)
            if q in binnen or B.get(*q) is not None:
                continue
            r = rng.random()
            if r < 0.025:
                B.set(*q, rng.choice(["minecraft:ochre_froglight[axis=y]", "minecraft:pearlescent_froglight[axis=y]",
                                      "minecraft:shroomlight", "guh_kristal_blok"]))
            elif r < 0.09:
                B.set(*q, rng.choice(["minecraft:amethyst_block", "guh_kristal_blok", "minecraft:calcite", "minecraft:budding_amethyst"]))
            elif r < 0.11 and B.get(x, y, z) is None and B.get(x, y - 1, z) is None and (x, y, z) not in B.c - binnen:
                B.set(x, y, z, f"minecraft:amethyst_cluster[facing={fac},waterlogged=false]", keep=True)
    # invisible light in the void + a few crystal clusters on the lake shore
    for y in range(vloer + 3, top - 4, 5):
        for a in range(0, 360, 72):
            for rr in (3.5, 8.5):
                x = int(round(cx + rr * math.cos(math.radians(a + y * 7 + rr * 20))))
                z = int(round(cz + rr * math.sin(math.radians(a + y * 7 + rr * 20))))
                if B.get(x, y, z) is None and (x, y, z) in binnen:
                    B.set(x, y, z, "minecraft:light[level=15,waterlogged=false]")
    for k in range(10):
        a = k * 0.63
        x, z = int(round(cx + (MEER_R + 1.5) * math.cos(a))), int(round(cz + (MEER_R + 1.5) * math.sin(a)))
        if B.get(x, vloer + 1, z) is None and all((x, z, L2) not in lanen for L2 in range(2 * vloer, 2 * vloer + 8)):
            B.set(x, vloer + 1, z, rng.choice(["guh_kristal_cluster", "minecraft:amethyst_cluster[facing=up,waterlogged=false]"]))
    # clusters must not stand on paths / rails: drop any that ended up in a headroom slot of a path cell
    for (x, z, L) in lanen:
        for dy in range(0, 3):
            s = B.get(x, L // 2 + dy, z)
            if s and ("cluster" in s or "dripstone[" in s):
                B.wis(x, L // 2 + dy, z)
    global LAATSTE
    LAATSTE = B
    B.stempel(W, "druipschacht", bezet=False)
    P_ = {
        "druipschacht_ingang": [sx, L_n // 2, sz],
        "druipschacht_bodem": [int(round(cx + (MEER_R + 2.5) * math.cos(phi0 + 0.3))), vloer + 1,
                               int(round(cz + (MEER_R + 2.5) * math.sin(phi0 + 0.3)))],
        "druipschacht_balkon": balkon,
        "druipschacht_lichtschacht": [cx, ys + 1, cz],
        "druipschacht_meer": [cx, vloer, cz],
    }
    for k, p in enumerate(rich):
        P_[f"druipschacht_richel_{k + 1}"] = p
    rng.shuffle(plafond)
    for k, p in enumerate(plafond[:6]):
        P_[f"druipschacht_plafond_{k + 1}"] = p
    for k, p in enumerate(vallen):
        P_[f"druipschacht_waterval_{k + 1}"] = p
    P.update({k: [int(v) for v in p] for k, p in P_.items()})
    return P_
