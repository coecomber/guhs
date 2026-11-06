"""prender: perspectief-renderer met uitgestelde belichting (deferred) voor de Hoogvads-renders.

Bouwt op mcrender (Res/Baker: blockstates, modellen, textures uit de jars), maar:
  - PCam: echte perspectiefcamera (positie, kijkpunt, beeldhoek); OCam: orthografisch (zoals mcrender)
  - buffers: albedo, normaal, n.L, MC-vlakfactor, emissief, diepte -> belichting achteraf
  - zon: schaduwkaart (orthografische dieptepass vanuit de zon) + kleur van glas-in-lood in de zonnestraal
  - frustum-culling per blok, backface-culling per vlak (camera-positie), 'vrij'-test per blok
Alle coördinaten zijn VOXELcoördinaten van het volume (x oost, y omhoog, z zuid).
"""
import math, time
import numpy as np
import mcrender as R

AIR = R.AIR


# ===================================================================== camera's
class PCam:
    persp = True

    def __init__(self, pos, kijk, fov=50.0, W=1600, H=900, rol=0.0):
        self.pos = np.asarray(pos, float)
        f = np.asarray(kijk, float) - self.pos
        f /= np.linalg.norm(f)
        r = np.cross(f, (0.0, 1.0, 0.0))
        r /= np.linalg.norm(r)
        u = np.cross(r, f)
        if rol:
            a = math.radians(rol)
            r, u = r * math.cos(a) + u * math.sin(a), -r * math.sin(a) + u * math.cos(a)
        self.f, self.right, self.up = f, r, u
        self.W, self.H = int(W), int(H)
        self.focal = (W / 2) / math.tan(math.radians(fov) / 2)
        self.cx, self.cy = W / 2, H / 2

    def project(self, P):
        v = np.asarray(P, float) - self.pos
        z = v @ self.f
        zs = np.maximum(z, 1e-3)
        sx = self.cx + self.focal * (v @ self.right) / zs
        sy = self.cy - self.focal * (v @ self.up) / zs
        return sx, sy, -z

    def naar(self, p):
        """Richting van punt p naar de camera (genormaliseerd)."""
        v = self.pos - np.asarray(p, float)
        return v / (np.linalg.norm(v) + 1e-9)

    def wereldpos(self, zb):
        H, W = zb.shape
        px = (np.arange(W, dtype=np.float32) + 0.5 - self.cx) / self.focal
        py = -(np.arange(H, dtype=np.float32) + 0.5 - self.cy) / self.focal
        d = (self.f[None, None, :] + px[None, :, None] * self.right[None, None, :] +
             py[:, None, None] * self.up[None, None, :]).astype(np.float32)
        z = -zb
        return self.pos.astype(np.float32) + d * z[..., None], d

    def straal(self, H, W):
        px = (np.arange(W, dtype=np.float32) + 0.5 - self.cx) / self.focal
        py = -(np.arange(H, dtype=np.float32) + 0.5 - self.cy) / self.focal
        d = (self.f[None, None, :] + px[None, :, None] * self.right[None, None, :] +
             py[:, None, None] * self.up[None, None, :]).astype(np.float32)
        return d / np.linalg.norm(d, axis=2, keepdims=True)


class OCam:
    """Orthografisch: d = richting van de scène naar de camera (zoals mcrender.Camera)."""
    persp = False

    def __init__(self, d, scale):
        d = np.asarray(d, float)
        self.d = d / np.linalg.norm(d)
        r = np.cross((0.0, 1.0, 0.0), self.d)
        if np.linalg.norm(r) < 1e-6:
            r = np.array([1.0, 0, 0])
        self.right = r / np.linalg.norm(r)
        self.up = np.cross(self.d, self.right)
        self.scale = scale
        self.ox = self.oy = 0.0
        self.W = self.H = 0

    @staticmethod
    def van_yaw_pitch(yaw, pitch, scale):
        th, ph = math.radians(yaw), math.radians(pitch)
        return OCam((math.cos(ph) * math.sin(th), math.sin(ph), math.cos(ph) * math.cos(th)), scale)

    def project(self, P):
        P = np.asarray(P, float)
        return P @ self.right * self.scale + self.ox, -(P @ self.up) * self.scale + self.oy, P @ self.d

    def naar(self, p):
        return self.d

    def pas_aan(self, lo, hi, marge=8):
        """Beeldgrootte en offset zo dat de doos lo..hi past."""
        C = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])], float)
        sx = C @ self.right * self.scale
        sy = -(C @ self.up) * self.scale
        self.ox, self.oy = -sx.min() + marge, -sy.min() + marge
        self.W, self.H = int(sx.max() - sx.min() + 2 * marge), int(sy.max() - sy.min() + 2 * marge)


# ===================================================================== licht
class Licht:
    """Zon + omgevingslicht. zon = richting NAAR de zon."""

    def __init__(self, zon=(0.5, 0.5, 0.3), zon_kl=(1.0, 0.84, 0.62), amb_kl=(0.50, 0.50, 0.62), mc=0.35,
                 zon_k=0.85):
        z = np.asarray(zon, float)
        self.zon = z / np.linalg.norm(z)
        self.zon_kl = np.array(zon_kl, np.float32) * zon_k
        self.amb_kl = np.array(amb_kl, np.float32)
        self.mc = mc


def mc_factor(n):
    n = n / (np.linalg.norm(n) + 1e-9)
    w = np.abs(n)
    return float((w[1] * (1.0 if n[1] > 0 else 0.5) + w[0] * 0.62 + w[2] * 0.8) / w.sum())


# ===================================================================== kern
def _zichtbare_blokken(vol, opaque, air, cam, marge_blok=1.5):
    occ = opaque[vol]
    idxs = np.argwhere(~air[vol])
    if not len(idxs):
        return idxs, occ
    occp = np.pad(occ, 1, constant_values=False)
    ix, iy, iz = idxs[:, 0] + 1, idxs[:, 1] + 1, idxs[:, 2] + 1
    full = opaque[vol[idxs[:, 0], idxs[:, 1], idxs[:, 2]]]
    if cam.persp:
        c = idxs + 0.5
        v = c - cam.pos
        sgx = np.where(v[:, 0] < 0, 1, -1)
        sgy = np.where(v[:, 1] < 0, 1, -1)
        sgz = np.where(v[:, 2] < 0, 1, -1)
        vrij = ~occp[ix + sgx, iy, iz] | ~occp[ix, iy + sgy, iz] | ~occp[ix, iy, iz + sgz]
        # binnen in een blok staan (camera) -> alles vrij
        keep = vrij | ~full
        idxs, c = idxs[keep], c[keep]
        z = (c - cam.pos) @ cam.f
        ok = z > 0.2
        zs = np.maximum(z, 0.2)
        sx = cam.cx + cam.focal * ((c - cam.pos) @ cam.right) / zs
        sy = cam.cy - cam.focal * ((c - cam.pos) @ cam.up) / zs
        m = cam.focal * marge_blok / zs + 4
        ok &= (sx > -m) & (sx < cam.W + m) & (sy > -m) & (sy < cam.H + m)
        return idxs[ok], occ
    d = cam.d
    sxn = 1 if d[0] > 0 else -1
    szn = 1 if d[2] > 0 else -1
    vrij = ~occp[ix + sxn, iy, iz] | ~occp[ix, iy + (1 if d[1] > 0 else -1), iz] | ~occp[ix, iy, iz + szn]
    return idxs[vrij | ~full], occ


class Buffers:
    def __init__(self, H, W, diep_alleen=False):
        self.H, self.W = H, W
        self.zb = np.full((H, W), -1e9, np.float32)
        self.alpha = np.zeros((H, W), np.float32)
        if diep_alleen:
            self.gz = np.full((H, W), -1e9, np.float32)  # diepte van glas (naar de zon toe)
            self.gk = np.ones((H, W, 3), np.float32)     # kleur van dat glas
            return
        self.alb = np.zeros((H, W, 3), np.float32)
        self.nrm = np.zeros((H, W, 3), np.float16)
        self.nl = np.zeros((H, W), np.float32)
        self.mcf = np.ones((H, W), np.float32)
        self.em = np.zeros((H, W), np.float32)
        self.ao = np.ones((H, W), np.float32)


def render(vol, pal, cam, res, bk, extras=(), emissive=(), hide=lambda s: False, ao=True, diep_alleen=False,
           tol=0.0, trans_terug=True, log=None):
    """Rastert het volume + extra quads. Geeft (Buffers, translucente-quads-lijst).
    diep_alleen=True: alleen diepte + glas (voor de schaduwkaart van de zon)."""
    t0 = time.time()
    X, Y, Z = vol.shape
    n = len(pal)
    baked = [None] * n
    air = np.zeros(n, bool)
    opaque = np.zeros(n, bool)
    names = [s.split("[")[0] for s in pal]
    for i, s in enumerate(pal):
        if names[i] in AIR or hide(s):
            air[i] = True
            continue
        baked[i] = bk.bake(s)
        opaque[i] = baked[i].full_opaque
    idxs, occ = _zichtbare_blokken(vol, opaque, air, cam)
    W, H = cam.W, cam.H
    B = Buffers(H, W, diep_alleen)
    trans = []
    persp = cam.persp
    emis = set(emissive)

    def raster(P, cu, texa, tint, attr, translucent, bias, diep=0):
        if persp:
            P4 = np.vstack([P, P[1] + P[2] - P[0]])
            z4 = (P4 - cam.pos) @ cam.f
            if (z4 < 0.15).all():
                return
            zmin, zmax = z4.min(), z4.max()
            if diep < 6 and (zmin < 0.15 or zmax / max(zmin, 1e-3) > 1.18):
                for (a0, b0) in ((0, 0), (0.5, 0), (0, 0.5), (0.5, 0.5)):
                    u = P[1] - P[0]
                    v = P[2] - P[0]
                    o = P[0] + u * a0 + v * b0
                    Pn = np.array([o, o + u * 0.5, o + v * 0.5])
                    cu0 = np.asarray(cu, float)
                    du_ = cu0[1] - cu0[0]
                    dv_ = cu0[3] - cu0[0]
                    c0 = cu0[0] + du_ * a0 + dv_ * b0
                    cun = np.array([c0, c0 + du_ * 0.5, c0 + du_ * 0.5 + dv_ * 0.5, c0 + dv_ * 0.5])
                    raster(Pn, cun, texa, tint, attr, translucent, bias, diep + 1)
                return
            if zmin < 0.15:
                return
        _raster1(P, cu, texa, tint, attr, translucent, bias)

    def _raster1(P, cu, texa, tint, attr, translucent, bias):
        sxp, syp, dz = cam.project(P)
        if persp and (dz > -0.15).any():
            return
        e1x, e1y = sxp[1] - sxp[0], syp[1] - syp[0]
        e2x, e2y = sxp[2] - sxp[0], syp[2] - syp[0]
        det = e1x * e2y - e1y * e2x
        if abs(det) < 1e-4:
            return
        x0 = int(max(0, math.floor(min(sxp[0], sxp[1], sxp[2], sxp[1] + e2x))))
        x1 = int(min(W - 1, math.ceil(max(sxp[0], sxp[1], sxp[2], sxp[1] + e2x))))
        y0 = int(max(0, math.floor(min(syp[0], syp[1], syp[2], syp[1] + e2y))))
        y1 = int(min(H - 1, math.ceil(max(syp[0], syp[1], syp[2], syp[1] + e2y))))
        if x1 < x0 or y1 < y0:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1, dtype=np.float32) + 0.5 - sxp[0],
                             np.arange(y0, y1 + 1, dtype=np.float32) + 0.5 - syp[0])
        a = (gx * e2y - gy * e2x) / det
        b = (gy * e1x - gx * e1y) / det
        lo, hi = -1e-4 - tol, 1 + 1e-4 + tol
        m = (a >= lo) & (a <= hi) & (b >= lo) & (b <= hi)
        if not m.any():
            return
        depth = dz[0] + a * (dz[1] - dz[0]) + b * (dz[2] - dz[0]) + bias
        sub = B.zb[y0:y1 + 1, x0:x1 + 1]
        m &= depth > sub
        if not m.any():
            return
        ac, bc = np.clip(a, 0, 1), np.clip(b, 0, 1)
        uv0 = cu[0][0] + ac * (cu[1][0] - cu[0][0]) + bc * (cu[3][0] - cu[0][0])
        uv1 = cu[0][1] + ac * (cu[1][1] - cu[0][1]) + bc * (cu[3][1] - cu[0][1])
        th, tw = texa.shape[:2]
        tu = np.clip((uv0 * tw).astype(np.int32), 0, tw - 1)
        tv = np.clip((uv1 * th).astype(np.int32), 0, th - 1)
        px = texa[tv, tu]
        al = px[..., 3]
        if diep_alleen:
            if translucent:
                dicht = m & (al > 0.95)
                sub[dicht] = depth[dicht]
                g = m & (al > 0.02) & (al <= 0.95)
                if g.any():
                    gz = B.gz[y0:y1 + 1, x0:x1 + 1]
                    gk = B.gk[y0:y1 + 1, x0:x1 + 1]
                    nieuw = g & (depth > gz)
                    kl = px[..., :3] * tint
                    kl = kl / np.maximum(kl.max(-1, keepdims=True), 1e-3)
                    gk[nieuw] = kl[nieuw] * 0.95
                    gz[nieuw] = depth[nieuw]
            else:
                m &= al > 0.5
                sub[m] = depth[m]
            return
        if translucent:
            return
        m &= al > 0.5
        if not m.any():
            return
        nrm, nl, mcf, em, aof = attr
        sub[m] = depth[m]
        B.alb[y0:y1 + 1, x0:x1 + 1][m] = (px[..., :3] * tint)[m]
        B.nrm[y0:y1 + 1, x0:x1 + 1][m] = nrm
        B.nl[y0:y1 + 1, x0:x1 + 1][m] = nl
        B.mcf[y0:y1 + 1, x0:x1 + 1][m] = mcf
        B.em[y0:y1 + 1, x0:x1 + 1][m] = em
        B.ao[y0:y1 + 1, x0:x1 + 1][m] = aof
        B.alpha[y0:y1 + 1, x0:x1 + 1][m] = 1.0

    sky = np.flip(np.cumsum(np.flip(occ, 1), 1), 1) > 0 if ao else None
    LZ = LICHT.zon
    qcache = {}

    def quads_van(si):
        q = qcache.get(si)
        if q is None:
            F = baked[si]
            q = []
            for k, (P, cu, tex, cf, tint, tr) in enumerate(F.quads):
                nrm = np.cross(P[2] - P[0], P[1] - P[0])
                ln = np.linalg.norm(nrm)
                if ln < 1e-9:
                    continue
                nrm = nrm / ln
                q.append((k, P, cu, res.texture(tex), cf, tint, tr, nrm, F.full_opaque or cf is not None,
                          names[si] in emis))
            qcache[si] = q
        return q

    nb = 0
    for (x, y, z) in idxs:
        si = vol[x, y, z]
        F = baked[si]
        if F is None:
            continue
        base = np.array([x, y, z], float)
        if persp:
            vc = cam.pos - (base + 0.5)
        for (k, P, cu, texa, cf, tint, tr, nrm, enkel, em) in quads_van(si):
            if cf is not None:
                nx, ny, nz = x + cf[0], y + cf[1], z + cf[2]
                if 0 <= nx < X and 0 <= ny < Y and 0 <= nz < Z:
                    nbk = vol[nx, ny, nz]
                    if occ[nx, ny, nz] or (nbk == si and (tr or not F.full_opaque)) or \
                            (tr and names[nbk] == names[si]):
                        continue
            Pw = P + base
            if persp:
                kant = float(nrm @ (cam.pos - Pw[0]))
            else:
                kant = float(nrm @ cam.d)
            nn = nrm
            if kant < 0:
                if enkel and not diep_alleen:
                    continue
                if not diep_alleen:
                    nn = -nrm
            if diep_alleen:
                raster(Pw, cu, texa, tint, None, tr, k * 1e-4)
                continue
            aof = 1.0
            if ao and not em and y + 1 < Y and nn[1] < 0.5 and sky[x, y + 1, z]:
                aof = 0.86
            attr = (nn, float(nn @ LZ), 1.0 - LICHT.mc + LICHT.mc * mc_factor(nn), 1.0 if em else 0.0, aof)
            if tr:
                trans.append((float(cam.project(Pw.mean(0))[2]), Pw, cu, texa, tint, attr))
            else:
                raster(Pw, cu, texa, tint, attr, False, k * 1e-4)
        nb += 1
    for (Pw, cu, tex, tint, tr) in extras:
        Pw = np.asarray(Pw, float)
        nrm = np.cross(Pw[2] - Pw[0], Pw[1] - Pw[0])
        ln = np.linalg.norm(nrm)
        if ln < 1e-9:
            continue
        nrm = nrm / ln
        kant = float(nrm @ (cam.pos - Pw[0])) if persp else float(nrm @ cam.d)
        if kant < 0 and not diep_alleen:
            continue
        texa = res.texture(tex) if isinstance(tex, str) else tex
        tint = np.asarray(tint, float)
        if diep_alleen:
            raster(Pw, cu, texa, tint, None, tr, 2e-3)
            continue
        attr = (nrm, float(nrm @ LZ), 1.0 - LICHT.mc + LICHT.mc * mc_factor(nrm), 0.0, 1.0)
        if tr:
            trans.append((float(cam.project(Pw.mean(0))[2]), Pw, cu, texa, tint, attr))
        else:
            raster(Pw, cu, texa, tint, attr, False, 2e-3)
    if diep_alleen:
        trans_d = []
    if log:
        log("raster %d blokken, %.0fs" % (nb, time.time() - t0))
    return B, trans


LICHT = Licht()


# ===================================================================== zon-schaduwkaart
class Zonkaart:
    def __init__(self, vol, pal, res, bk, extras=(), hide=lambda s: False, px_per_blok=3.0, log=None):
        self.cam = OCam(LICHT.zon, px_per_blok)
        X, Y, Z = vol.shape
        self.cam.pas_aan((0, 0, 0), (X, Y, Z), marge=4)
        t = time.time()
        B, _ = render(vol, pal, self.cam, res, bk, extras=extras, hide=hide, ao=False, diep_alleen=True)
        self.zb, self.gz, self.gk = B.zb, B.gz, B.gk
        if log:
            log("zonkaart %dx%d %.0fs" % (self.cam.W, self.cam.H, time.time() - t))

    def licht(self, P, bias=0.35, zacht=1):
        """P (...,3) voxelpunten -> (zonlicht 0..1, glaskleur (...,3))."""
        sh = P.shape[:-1]
        Q = P.reshape(-1, 3)
        sx, sy, dz = self.cam.project(Q)
        H, W = self.zb.shape
        tot = np.zeros(len(Q), np.float32)
        n = 0
        for ox in range(-zacht, zacht + 1):
            for oy in range(-zacht, zacht + 1):
                ix = np.clip((sx + ox).astype(np.int32), 0, W - 1)
                iy = np.clip((sy + oy).astype(np.int32), 0, H - 1)
                tot += (self.zb[iy, ix] <= dz + bias)
                n += 1
        lit = tot / n
        ix = np.clip(sx.astype(np.int32), 0, W - 1)
        iy = np.clip(sy.astype(np.int32), 0, H - 1)
        glas = self.gz[iy, ix] > dz + bias
        gk = np.where(glas[:, None], self.gk[iy, ix], 1.0)
        return lit.reshape(sh), gk.reshape(sh + (3,))


# ===================================================================== belichten
def belicht(B, trans, cam, res, zk=None, P=None, schaduw_k=1.0):
    """Uitgestelde belichting: kleur = albedo * mcf * ao * (amb + zon * n.L * schaduw * glas)."""
    L = LICHT
    nl = np.clip(B.nl, 0, 1)
    sun = np.ones(B.zb.shape + (3,), np.float32)
    if zk is not None and P is not None:
        Pn = P + B.nrm.astype(np.float32) * 0.12
        lit, gk = zk.licht(Pn)
        lit = 1 - (1 - lit) * schaduw_k
        sun = lit[..., None] * gk
    lichtk = L.amb_kl[None, None, :] + L.zon_kl[None, None, :] * (nl[..., None] * sun)
    rgb = B.alb * (B.mcf * B.ao)[..., None] * lichtk
    em = B.em > 0.5
    rgb[em] = B.alb[em] * 1.05
    # doorschijnend (glas, water): vooruit belicht, achter naar voor; in perspectief dichtbij onderverdelen
    if cam.persp:
        def splits(item, diep=0):
            dz, Pw, cu, texa, tint, attr = item
            P4 = np.vstack([Pw, Pw[1] + Pw[2] - Pw[0]])
            z4 = (P4 - cam.pos) @ cam.f
            if (z4 < 0.15).all():
                return []
            if diep < 5 and (z4.min() < 0.15 or z4.max() / max(z4.min(), 1e-3) > 1.18):
                out = []
                u, v = Pw[1] - Pw[0], Pw[2] - Pw[0]
                cu0 = np.asarray(cu, float)
                du_, dv_ = cu0[1] - cu0[0], cu0[3] - cu0[0]
                for (a0, b0) in ((0, 0), (0.5, 0), (0, 0.5), (0.5, 0.5)):
                    o = Pw[0] + u * a0 + v * b0
                    Pn = np.array([o, o + u * 0.5, o + v * 0.5])
                    c0 = cu0[0] + du_ * a0 + dv_ * b0
                    cun = np.array([c0, c0 + du_ * 0.5, c0 + du_ * 0.5 + dv_ * 0.5, c0 + dv_ * 0.5])
                    out += splits((float(cam.project(Pn.mean(0))[2]), Pn, cun, texa, tint, attr), diep + 1)
                return out
            if z4.min() < 0.15:
                return []
            return [item]
        nieuw = []
        for it in trans:
            nieuw += splits(it)
        trans = nieuw
    trans.sort(key=lambda t: t[0])
    H, W = B.zb.shape
    persp = cam.persp
    for dz, Pw, cu, texa, tint, attr in trans:
        sxp, syp, dzz = cam.project(Pw)
        if persp and (dzz > -0.15).any():
            continue
        e1x, e1y = sxp[1] - sxp[0], syp[1] - syp[0]
        e2x, e2y = sxp[2] - sxp[0], syp[2] - syp[0]
        det = e1x * e2y - e1y * e2x
        if abs(det) < 1e-4:
            continue
        x0 = int(max(0, math.floor(min(sxp[0], sxp[1], sxp[2], sxp[1] + e2x))))
        x1 = int(min(W - 1, math.ceil(max(sxp[0], sxp[1], sxp[2], sxp[1] + e2x))))
        y0 = int(max(0, math.floor(min(syp[0], syp[1], syp[2], syp[1] + e2y))))
        y1 = int(min(H - 1, math.ceil(max(syp[0], syp[1], syp[2], syp[1] + e2y))))
        if x1 < x0 or y1 < y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1, dtype=np.float32) + 0.5 - sxp[0],
                             np.arange(y0, y1 + 1, dtype=np.float32) + 0.5 - syp[0])
        a = (gx * e2y - gy * e2x) / det
        b = (gy * e1x - gx * e1y) / det
        m = (a >= -1e-4) & (a <= 1 + 1e-4) & (b >= -1e-4) & (b <= 1 + 1e-4)
        depth = dzz[0] + a * (dzz[1] - dzz[0]) + b * (dzz[2] - dzz[0])
        m &= depth > B.zb[y0:y1 + 1, x0:x1 + 1]
        if not m.any():
            continue
        uv0 = cu[0][0] + a * (cu[1][0] - cu[0][0]) + b * (cu[3][0] - cu[0][0])
        uv1 = cu[0][1] + a * (cu[1][1] - cu[0][1]) + b * (cu[3][1] - cu[0][1])
        th, tw = texa.shape[:2]
        tu = np.clip((uv0 * tw).astype(np.int32), 0, tw - 1)
        tv = np.clip((uv1 * th).astype(np.int32), 0, th - 1)
        px = texa[tv, tu]
        al = px[..., 3]
        m &= al > 0.02
        if not m.any():
            continue
        nrm, nl_, mcf, em, aof = attr
        lk = L.amb_kl + L.zon_kl * max(0.0, nl_) * 0.8
        col = px[..., :3] * tint * mcf * lk * 1.08
        sub = rgb[y0:y1 + 1, x0:x1 + 1]
        aa = al[..., None]
        sub[m] = (sub * (1 - aa) + col * aa)[m]
        B.alpha[y0:y1 + 1, x0:x1 + 1][m] = np.maximum(B.alpha[y0:y1 + 1, x0:x1 + 1][m], al[m])
    return rgb
