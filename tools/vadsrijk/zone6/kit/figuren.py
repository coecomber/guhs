"""Figuren voor Hoogvads: guhs, ridder-guhs (helm + pluim + tabberd), de koningin, luchtschepen-figuren.

Extra quads in VOXELcoördinaten van de Wereld (W.naar_voxel). Alleen bij k=1 zinvol.
"""
import math
import numpy as np
import mcextra as E
import erfkit as K


def kleur(rgb, a=1.0):
    t = np.ones((2, 2, 4), np.float32)
    t[..., :3] = np.array(rgb, np.float32) / 255.0
    t[..., 3] = a
    return t


def doos_quads(lo, hi, rgb, Mt=None, tex=None):
    """Zes vlakken van een doos (lokaal lo..hi), optioneel door affine Mt (mcextra.affine) gezet."""
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    c = (lo + hi) / 2
    t = tex if tex is not None else kleur(rgb)
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    out = []
    ex = hi - lo
    for ax in range(3):
        for kant in (0, 1):
            o = lo.copy()
            o[ax] = hi[ax] if kant else lo[ax]
            u = np.zeros(3)
            v = np.zeros(3)
            a1, a2 = [a for a in range(3) if a != ax]
            u[a1] = ex[a1]
            v[a2] = ex[a2]
            n = np.cross(v, u)
            fc = o + u / 2 + v / 2
            if n @ (fc - c) < 0:
                u, v = v, u
            Pq = np.array([o, o + u, o + v])
            if Mt is not None:
                Pq = E.apply(Mt, Pq)
            out.append((Pq, cu, t, np.ones(3), False))
    return out


ZILVER = (214, 220, 236)
ROZE = (246, 120, 168)
GOUD = (238, 190, 70)


def ridder(W, x, y, z, yaw=180, skin="guh", pluim=ROZE, cape=ROZE, speer=True, schaal=0.8):
    """Elite-ridder van Hoogvads: guh met zilveren helm, roze pluim, roze tabberd met gouden rand en een lans."""
    W.wguh(x, y, z, yaw, skin=skin)
    vx, vy, vz = W.naar_voxel((x, y, z))
    Mt = E.affine(schaal / 0.8 * 1.0, yaw, (vx, vy, vz))
    q = []
    # helm (iets groter dan het hoofd) met vizier-spleet
    q += doos_quads((-0.46, 0.74, -0.46), (0.46, 0.93, 0.28), ZILVER, Mt)
    q += doos_quads((-0.48, 0.72, -0.50), (0.48, 0.77, 0.30), GOUD, Mt)
    # pluim
    q += doos_quads((-0.07, 0.93, -0.30), (0.07, 1.20, 0.15), pluim, Mt)
    q += doos_quads((-0.07, 1.05, 0.15), (0.07, 1.17, 0.42), pluim, Mt)
    # tabberd (voor) en cape (achter)
    q += doos_quads((-0.40, 0.08, -0.55), (0.40, 0.36, -0.49), cape, Mt)
    q += doos_quads((-0.40, 0.08, -0.56), (0.40, 0.12, -0.49), GOUD, Mt)
    q += doos_quads((-0.52, 0.04, 0.55), (0.52, 0.55, 0.62), cape, Mt)
    if speer:
        q += doos_quads((0.62, 0.0, -0.10), (0.68, 1.75, -0.04), (210, 170, 110), Mt)
        q += doos_quads((0.58, 1.75, -0.14), (0.72, 1.98, 0.0), ZILVER, Mt)
        q += doos_quads((0.66, 1.45, -0.08), (0.70, 1.70, 0.30), pluim, Mt)
    W.extra.extend(q)


def kroon(W, x, y, z, yaw=180, schaal=1.0):
    """Gebreide wollen kroon (roze met gouden puntjes) op een hoofd op hoogte y."""
    vx, vy, vz = W.naar_voxel((x, y, z))
    Mt = E.affine(schaal, yaw, (vx, vy, vz))
    q = doos_quads((-0.40, 0.0, -0.45), (0.40, 0.16, 0.20), (250, 150, 190), Mt)
    for i in range(5):
        a = -0.32 + i * 0.16
        q += doos_quads((a - 0.04, 0.16, -0.46), (a + 0.04, 0.30, -0.38), GOUD, Mt)
    q += doos_quads((-0.06, 0.30, -0.47), (0.06, 0.40, -0.40), (255, 120, 170), Mt)
    W.extra.extend(q)


def koningin(W, x, y, z, yaw=90):
    """Koningin Zachtmoed de Derde: oma-guh met een gebreide kroon en een roze mantel."""
    import bouwhulp as BH
    n0 = len(W.extra)
    BH.npc(W, "guh_npc_omaguh", x, y, z, yaw=yaw, schaal=1.15)
    top = max(float(np.asarray(q[0])[:, 1].max()) for q in W.extra[n0:]) if len(W.extra) > n0 else None
    vx, vy, vz = W.naar_voxel((x, y, z))
    Mt = E.affine(1.15, yaw, (vx, vy, vz))
    W.extra.extend(doos_quads((-0.62, 0.05, 0.40), (0.62, 0.75, 0.50), (236, 110, 160), Mt))
    W.extra.extend(doos_quads((-0.64, 0.02, 0.38), (0.64, 0.10, 0.52), GOUD, Mt))
    ytop = (top / W.k + W.b[1]) if top is not None else y + 1.3
    kroon(W, x, ytop - 0.08, z, yaw, 1.1)


def menigte(W, x1, z1, x2, z2, y, n, seed=0, skins=None, kijk=None, zit=False, vrij=None):
    """n guhs verspreid in een rechthoek (y = vloerhoogte). kijk = yaw of None (willekeurig)."""
    rng = np.random.RandomState(seed)
    skins = skins or ["guh", "guh", "guh_mint", "guh_wolk", "guh_bloesemguh", "guh_snow", "guh_pluisguh",
                      "guh_golden", "guh_choco", "guh_konijntje_roze"]
    for _ in range(n):
        x = rng.uniform(x1, x2)
        z = rng.uniform(z1, z2)
        if vrij is not None and not vrij(x, z):
            continue
        W.wguh(x, y, z, kijk if kijk is not None else rng.uniform(0, 360), skin=skins[rng.randint(len(skins))],
               sitting=zit)


def pluimguh_ruiter(W, x, y, z, yaw=0, variant="roze"):
    """Een pluimguh (vogel-mount uit renders-mounts) met een ridder erop, als extra quads."""
    try:
        import mounts as MO
        import mountkit as MK
        m = MO.pluimguh(variant)
        vx, vy, vz = W.naar_voxel((x, y, z))
        base = MK.base_mat((vx, vy, vz), yaw=yaw, schaal=1.0)
        W.extra.extend(MK.quads(m, base))
    except Exception as e:  # noqa
        print("pluimguh mislukt:", e)
