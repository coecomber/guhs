"""opname: perspectief-opnames van een Wereld (hoogkit) met zon, schaduw, wolkenzee, lucht, nevel en lichtbundels.

  opname(W, naam, pos, kijk, fov, breed, hoog, ss, licht=GOUDEN_UUR, ...)
Lichtsferen: GOUDEN_UUR, NAMIDDAG, MIDDAG (dicts; zon = richting naar de zon in wereldcoördinaten).
"""
import os, math, time
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import hoogkit as H
import schemerkit as SK
import prender as PR

V = H.V
R, E, K = SK.R, SK.E, SK.K
OUT = V.OUT
SK.OUT = K.OUT = OUT
R.TRANSLUCENT_HINT = tuple(R.TRANSLUCENT_HINT) + ("kathedraalglas",)
R.GRASS = (150, 214, 110)
R.FOLIAGE = (120, 196, 90)
R.WATER = (110, 196, 240)
EMISSIVE = set(SK.EMISSIVE) | {"guhs:wolkenlamp", "guhs:park_lantaarn", "guhs:guh_lantaarnpaal", "guhs:lampion_roze",
                               "guhs:lampion_geel", "guhs:lampion_mint", "minecraft:lantern", "guhs:sterrenlantaarn",
                               "minecraft:sea_lantern", "minecraft:pearlescent_froglight", "minecraft:candle",
                               "minecraft:pink_candle", "minecraft:white_candle", "minecraft:glowstone",
                               "minecraft:end_rod", "minecraft:campfire"}


def log(*a):
    print(time.strftime("%H:%M:%S"), *a, flush=True)


GOUDEN_UUR = dict(zon=(-0.62, 0.20, 0.42), zon_kl=(1.0, 0.80, 0.56), amb_kl=(0.50, 0.47, 0.62), zon_k=0.95, mc=0.30,
                  zenit=(112, 132, 206), horizon=(255, 196, 150), zon_rgb=(255, 214, 150), wolk_licht=(255, 226, 200),
                  wolk_schaduw=(150, 128, 186))
MIDDAG = dict(zon=(0.35, 0.80, 0.45), zon_kl=(1.0, 0.96, 0.88), amb_kl=(0.56, 0.58, 0.68), zon_k=0.80, mc=0.35,
              zenit=(120, 170, 236), horizon=(236, 226, 246), zon_rgb=(255, 250, 230), wolk_licht=(255, 252, 252),
              wolk_schaduw=(176, 180, 220))
NAMIDDAG = dict(zon=(-0.55, 0.45, 0.50), zon_kl=(1.0, 0.88, 0.70), amb_kl=(0.54, 0.54, 0.66), zon_k=0.90, mc=0.32,
                zenit=(118, 152, 226), horizon=(255, 214, 190), zon_rgb=(255, 232, 190), wolk_licht=(255, 240, 228),
                wolk_schaduw=(164, 150, 204))


def zet_licht(L):
    PR.LICHT = PR.Licht(zon=L["zon"], zon_kl=L["zon_kl"], amb_kl=L["amb_kl"], mc=L["mc"], zon_k=L["zon_k"])


def _zon(L):
    z = np.asarray(L["zon"], np.float32)
    return z / np.linalg.norm(z)


# ------------------------------------------------------------------ wolkenzee (hoogteveld, één keer berekend)
_HF = {}


def wolkveld(N=2048, span=7000.0, seed=4):
    key = (N, span, seed)
    if key in _HF:
        return _HF[key]
    rng = np.random.RandomState(seed)
    # grote schaal: dichtheid (waar de wolkenbergen liggen)
    dicht = np.zeros((N, N), np.float32)
    for cel, amp in ((256, 1.0), (128, 0.5), (64, 0.25)):
        g = rng.rand(N // cel + 2, N // cel + 2).astype(np.float32)
        im = Image.fromarray(g).resize(((N // cel + 2) * cel, (N // cel + 2) * cel), Image.BICUBIC)
        dicht += np.asarray(im, np.float32)[:N, :N] * amp
    dicht = (dicht - dicht.min()) / (dicht.max() - dicht.min())
    # stapelwolken: veel koepeltjes (max-samenstelling), groot en klein
    b = np.zeros((N, N), np.float32)
    for (n, r0, r1) in ((900, 18, 46), (3500, 7, 18), (9000, 3, 7)):
        xs = rng.randint(0, N, n)
        ys = rng.randint(0, N, n)
        rs = rng.uniform(r0, r1, n)
        for x, y, r in zip(xs, ys, rs):
            dd = dicht[y, x]
            hgt = (0.25 + 0.75 * dd) * (r / 46.0) ** 0.55
            if dd < 0.22 and r > 10:
                continue
            ri = int(r) + 1
            x0, x1, y0, y1 = max(0, x - ri), min(N, x + ri + 1), max(0, y - ri), min(N, y + ri + 1)
            yy, xx = np.mgrid[y0:y1, x0:x1]
            q = 1 - ((xx - x) ** 2 + (yy - y) ** 2) / (r * r)
            dome = hgt * np.sqrt(np.clip(q, 0, 1))
            sub = b[y0:y1, x0:x1]
            np.maximum(sub, dome, out=sub)
    b = b + 0.12 * dicht
    b = np.clip(b / np.percentile(b, 99.5), 0, 1).astype(np.float32)
    px = span / N
    gy, gx = np.gradient(b)
    _HF[key] = (b, (gx / px).astype(np.float32), (gy / px).astype(np.float32), span, N)
    return _HF[key]


def _hf(b, x, z, span, N):
    u = np.clip(((x + span / 2) / span * N).astype(np.int32), 0, N - 1)
    v = np.clip(((z + span / 2) / span * N).astype(np.int32), 0, N - 1)
    return b[v, u]


def wolkenzee(pos, d, maxt, L, Y0=40.0, A=62.0, zk=None, w2v=None, stappen=40):
    """Ray-march door het wolkenhoogteveld. pos (3,) wereld, d (n,3) genormaliseerd, maxt (n,) afstand tot geometrie.
    Geeft (raak (n,), t (n,), kleur (n,3))."""
    b, gx, gz, span, N = wolkveld()
    n = len(d)
    raak = np.zeros(n, bool)
    tt = np.full(n, np.inf, np.float32)
    kl = np.zeros((n, 3), np.float32)
    idx = np.nonzero(d[:, 1] < -1e-4)[0]
    if not len(idx) or pos[1] < Y0:
        return raak, tt, kl
    dd = d[idx]
    t_top = np.maximum((Y0 + A - pos[1]) / dd[:, 1], 0)
    t_vlak = (Y0 - pos[1]) / dd[:, 1]
    t_bot = np.minimum(t_vlak, maxt[idx])
    vorige = t_top.copy()
    gevonden = np.zeros(len(idx), bool)
    tr = np.full(len(idx), np.inf, np.float32)
    for k in range(1, stappen + 1):
        f = (k / stappen) ** 1.6
        t = t_top + (t_bot - t_top) * f
        x = pos[0] + dd[:, 0] * t
        y = pos[1] + dd[:, 1] * t
        z = pos[2] + dd[:, 2] * t
        h = Y0 + A * _hf(b, x, z, span, N)
        hit = (~gevonden) & (y <= h)
        tr[hit] = (vorige[hit] + t[hit]) * 0.5
        gevonden |= hit
        vorige = t
    rest = (~gevonden) & (t_vlak <= maxt[idx])
    tr[rest] = t_vlak[rest]
    gevonden |= rest
    gi = np.nonzero(gevonden)[0]
    t = tr[gi]
    x = pos[0] + dd[gi, 0] * t
    z = pos[2] + dd[gi, 2] * t
    y = pos[1] + dd[gi, 1] * t
    hx = _hf(gx, x, z, span, N) * A
    hz = _hf(gz, x, z, span, N) * A
    hv = _hf(b, x, z, span, N)
    nrm = np.stack([-hx, np.ones_like(hx), -hz], 1)
    nrm /= np.linalg.norm(nrm, axis=1, keepdims=True)
    zon = _zon(L)
    lam = np.clip(nrm @ zon, 0, 1)
    licht_ = np.clip(0.22 + 1.0 * lam * (0.5 + 0.5 * hv ** 0.7), 0, 1.25)
    if zk is not None and w2v is not None:
        Pv = w2v(np.stack([x, y + 1.0, z], 1).astype(np.float32))
        lit, _ = zk.licht(Pv, bias=1.0, zacht=1, buiten=1.0)
        licht_ = licht_ * (0.3 + 0.7 * lit)
    wl = np.array(L["wolk_licht"], np.float32) / 255
    ws = np.array(L["wolk_schaduw"], np.float32) / 255
    a = np.clip(licht_, 0, 1)[:, None]
    c = ws[None] * (1 - a) + wl[None] * a * (1 + 0.15 * np.clip(licht_ - 1, 0, 1)[:, None])
    tegen = np.clip(dd[gi] @ zon, 0, 1) ** 6
    c += (np.array(L["zon_rgb"], np.float32) / 255)[None] * (tegen * (1 - hv) * 0.35)[:, None]
    sel = idx[gi]
    raak[sel] = True
    tt[sel] = t
    kl[sel] = c
    return raak, tt, kl


def lucht_kleur(d, L, pos=None, cirrus=True):
    zen = np.array(L["zenit"], np.float32) / 255
    hor = np.array(L["horizon"], np.float32) / 255
    zr = np.array(L["zon_rgb"], np.float32) / 255
    zon = _zon(L)
    e = np.clip(d[:, 1], 0, 1)
    f = np.clip(e / 0.55, 0, 1) ** 0.55
    c = hor[None] * (1 - f[:, None]) + zen[None] * f[:, None]
    cs = np.clip(d @ zon, 0, 1)
    c = c + zr[None] * (0.55 * cs ** 8 + 0.35 * cs ** 48)[:, None]
    schijf = cs > 0.99965
    c[schijf] = np.array((1.0, 0.98, 0.9), np.float32)
    rand = (cs > 0.9993) & ~schijf
    c[rand] = c[rand] * 0.5 + 0.5
    if cirrus and pos is not None:
        b, _, _, span, N = wolkveld()
        boven = d[:, 1] > 0.01
        if boven.any():
            t = (1600.0 - pos[1]) / np.maximum(d[boven, 1], 0.01)
            x = pos[0] + d[boven, 0] * t
            z = pos[2] + d[boven, 2] * t * 0.35
            w = _hf(b, x * 0.6 + 900, z * 3.0, span, N)
            w = np.clip((w - 0.35) * 1.6, 0, 1) * np.clip(d[boven, 1] / 0.08, 0, 1) * \
                np.clip(1 - d[boven, 1] / 0.6, 0, 1)
            wk = (np.array(L["wolk_licht"], np.float32) / 255) * 0.97 + zr[None] * 0.2 * (cs[boven] ** 3)[:, None]
            c[boven] = c[boven] * (1 - 0.55 * w[:, None]) + wk * (0.55 * w[:, None])
    return np.clip(c, 0, 1.2)


# ------------------------------------------------------------------ schaduwkaart op een deel
class Zonkaart(PR.Zonkaart):
    def __init__(self, vol, pal, res, bk, a, b, extras, hide, px):
        self.o = np.array(a, np.float32)
        sub = vol[a[0]:b[0], a[1]:b[1], a[2]:b[2]]
        ex = []
        for q in extras:
            Pq = np.asarray(q[0], float)
            if (Pq[:, 0] >= a[0] - 2).all() and (Pq[:, 0] <= b[0] + 2).all() and (Pq[:, 2] >= a[2] - 2).all() and \
                    (Pq[:, 2] <= b[2] + 2).all():
                ex.append((Pq - self.o, q[1], q[2], q[3], q[4]))
        super().__init__(sub, pal, res, bk, extras=ex, hide=lambda s: s in hide, px_per_blok=px, log=log)

    def licht(self, P, bias=0.35, zacht=1, buiten=1.0):
        Q = np.asarray(P, np.float32) - self.o
        sh = Q.shape[:-1]
        Q2 = Q.reshape(-1, 3)
        sx, sy, dz = self.cam.project(Q2)
        Hh, Ww = self.zb.shape
        binnen = (sx >= 0) & (sx < Ww) & (sy >= 0) & (sy < Hh)
        lit, gk = PR.Zonkaart.licht(self, Q2, bias, zacht)
        lit = np.where(binnen, lit, buiten)
        gk = np.where(binnen[:, None], gk, 1.0)
        return lit.reshape(sh).astype(np.float32), gk.reshape(sh + (3,)).astype(np.float32)


def _stralen(rgb, cam, B, zk, W, cfg, L, ss):
    """Volumetrisch zonlicht: punten langs de kijkstraal in de schaduwkaart (lage resolutie, daarna vergroot)."""
    if zk is None:
        return rgb
    f = cfg.get("schaal", 2 * ss)
    Hs, Ws = B.zb.shape
    h, w = Hs // f, Ws // f
    zb = B.zb[::f, ::f][:h, :w]
    px = (np.arange(w, dtype=np.float32) * f + 0.5 * f - cam.cx) / cam.focal
    py = -(np.arange(h, dtype=np.float32) * f + 0.5 * f - cam.cy) / cam.focal
    dvec = (cam.f[None, None] + px[None, :, None] * cam.right[None, None] +
            py[:, None, None] * cam.up[None, None]).astype(np.float32)
    mx = cfg.get("max", 200.0) * W.k
    zdiep = np.where(zb < -1e8, mx, -zb)
    zdiep = np.minimum(zdiep, mx)
    n = cfg.get("n", 48)
    acc = np.zeros((h, w, 3), np.float32)
    jit = np.random.RandomState(1).rand(h, w).astype(np.float32)
    o = np.array(W.b[:3], np.float32)
    for k in range(n):
        tz = zdiep * ((k + jit) / n)
        Q = cam.pos[None, None].astype(np.float32) + dvec * tz[..., None]
        lit, gk = zk.licht(Q, bias=0.6, zacht=0, buiten=cfg.get("buiten", 0.0))
        if "box" in cfg:
            bx = cfg["box"]
            Qw = Q / W.k + o
            m = (Qw[..., 0] > bx[0]) & (Qw[..., 0] < bx[3]) & (Qw[..., 1] > bx[1]) & (Qw[..., 1] < bx[4]) & \
                (Qw[..., 2] > bx[2]) & (Qw[..., 2] < bx[5])
            lit = lit * m
        acc += lit[..., None] * gk
    acc /= n
    dl = zdiep / mx
    zr = np.array(L["zon_rgb"], np.float32) / 255
    s = acc * dl[..., None] * zr[None, None] * cfg.get("sterkte", 0.5)
    im = Image.fromarray((np.clip(s, 0, 1) * 255).astype(np.uint8)).resize((Ws, Hs), Image.BILINEAR)
    im = im.filter(ImageFilter.GaussianBlur(ss * 1.5))
    return rgb + np.asarray(im, np.float32) / 255


def _font(sz, vet=True):
    try:
        return ImageFont.truetype("segoeuib.ttf" if vet else "segoeui.ttf", sz)
    except OSError:
        return ImageFont.load_default()


def _deeltjes(img, W, cam, zb, ss):
    """Kleine dingen (bloesemblaadjes, vogeltjes, glinsters) als stipjes in perspectief, met dieptetest.
    W.deeltjes = [(x, y, z, (r,g,b), grootte_in_blokken, gloed, vorm)] in wereldcoördinaten."""
    D = W.deeltjes
    o = np.array(W.b[:3], np.float32)
    Pv = (np.array([d[:3] for d in D], np.float32) - o) * W.k
    sx, sy, dz = cam.project(Pv)
    Hs, Ws = zb.shape
    laag = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gl = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d1, d2 = ImageDraw.Draw(laag), ImageDraw.Draw(gl)
    for i, (x, y, z, kl, gr, gloed, vorm) in enumerate(D):
        if dz[i] > -1:
            continue
        px, py = int(sx[i]), int(sy[i])
        if not (0 <= px < Ws and 0 <= py < Hs):
            continue
        if zb[py, px] > dz[i] + 1.0:
            continue
        r = max(0.6, cam.focal * gr * W.k / (-dz[i]) / ss * 0.5)
        fx, fy = sx[i] / ss, sy[i] / ss
        if vorm == "vogel":
            w_ = r * 2.2
            d1.line([(fx - w_, fy - r * 0.6), (fx, fy), (fx + w_, fy - r * 0.6)], fill=tuple(kl) + (230,),
                    width=max(1, int(r * 0.5)))
        elif vorm == "blad":
            d1.ellipse((fx - r * 1.2, fy - r * 0.6, fx + r * 1.2, fy + r * 0.6), fill=tuple(kl) + (235,))
        else:
            d1.ellipse((fx - r, fy - r, fx + r, fy + r), fill=tuple(kl) + (225,))
        if gloed:
            g = r * 3.5 * gloed
            d2.ellipse((fx - g, fy - g, fx + g, fy + g), fill=tuple(kl) + (120,))
    gl = gl.filter(ImageFilter.GaussianBlur(3))
    o_ = img.convert("RGBA")
    o_ = Image.alpha_composite(o_, gl)
    o_ = Image.alpha_composite(o_, laag)
    return o_.convert("RGB")


# ------------------------------------------------------------------ de opname zelf
def opname(W, naam, pos, kijk, fov=50, breed=1600, hoog=900, ss=2, licht=GOUDEN_UUR, schaduw=True,
           zon_box=None, zon_px=2.5, wolken=True, wolk_y=40.0, nevel=(0.0, 3000.0), stralen=None, gloed=0.6,
           titel=None, onderschrift=None, extra=(), hol=True, binnen=False, belichting=1.0, verberg=None,
           kleur=None, achtergrond=None, rol=0.0, deeltjes=True):
    """Perspectief-opname van Wereld W. pos/kijk in WERELDcoördinaten. Schrijft OUT/<naam>.png (+ _namen.png).
    nevel = (sterkte 0..1, afstand) luchtperspectief. stralen = dict(sterkte, max, n, box) lichtbundels.
    zon_box = (x1,y1,z1,x2,y2,z2) wereld: alleen dit deel werpt schaduw (sneller).
    verberg = functie(vol, W) die blokken weghaalt voor een doorkijk. binnen=True: achtergrond = achtergrond-kleur."""
    t0 = time.time()
    zet_licht(licht)
    r, bk = V.res()
    W.fix_connections()
    vol = W.v.copy()
    if verberg is not None:
        verberg(vol, W)
    if hol:
        n = SK.hol_maken(W.pal, vol)
        log(naam, "binnenblokken weg:", n)
    ext, hide = E.special_extras(vol, W.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in W.guhs:
        ext += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8 * W.k, sitting=sitting)
    ext = list(ext) + list(W.extra) + list(extra)
    pv, kv = np.array(W.naar_voxel(pos)), np.array(W.naar_voxel(kijk))
    cam = PR.PCam(pv, kv, fov, breed * ss, hoog * ss, rol=rol)
    zk = None
    if schaduw:
        if zon_box:
            a = [max(0, W.vx(zon_box[0])), max(0, W.vy(zon_box[1])), max(0, W.vz(zon_box[2]))]
            b_ = [min(W.v.shape[0], W.vx(zon_box[3])), min(W.v.shape[1], W.vy(zon_box[4])),
                  min(W.v.shape[2], W.vz(zon_box[5]))]
        else:
            a, b_ = [0, 0, 0], list(W.v.shape)
        zk = Zonkaart(vol, W.pal, r, bk, a, b_, ext, hide, zon_px)
    B, trans = PR.render(vol, W.pal, cam, r, bk, extras=ext, emissive=EMISSIVE, hide=lambda s: s in hide,
                         tol=0.02, log=lambda s: log(naam, s))
    P, _ = cam.wereldpos(B.zb)
    rgb = PR.belicht(B, trans, cam, r, zk, P) * belichting
    log(naam, "belicht %.0fs" % (time.time() - t0))
    Hs, Ws = B.zb.shape
    d = cam.straal(Hs, Ws).reshape(-1, 3)
    geo = (B.alpha > 0.5).reshape(-1)
    pos_w = np.asarray(pos, np.float32)
    dist = np.where(geo, np.linalg.norm((P - cam.pos.astype(np.float32)).reshape(-1, 3), axis=1) / W.k, np.inf)
    dist = dist.astype(np.float32)
    out = rgb.reshape(-1, 3).copy()
    if binnen:
        if achtergrond is not None:
            out[~geo] = np.array(achtergrond, np.float32) / 255
    else:
        out[~geo] = lucht_kleur(d[~geo], licht, pos_w)
        if nevel and nevel[0] > 0:
            dg = d[geo]
            hz = lucht_kleur(np.stack([dg[:, 0], np.full(len(dg), 0.02, np.float32), dg[:, 2]], 1), licht, None,
                             cirrus=False)
            f = (1 - np.exp(-dist[geo] / nevel[1])) * nevel[0]
            out[geo] = out[geo] * (1 - f[:, None]) + hz * f[:, None]
        if wolken:
            o = np.array(W.b[:3], np.float32)

            def w2v(Q):
                return (Q - o) * W.k
            raak, tt, kl = wolkenzee(pos_w, d, dist, licht, Y0=wolk_y, zk=zk, w2v=w2v)
            if raak.any():
                dr_ = d[raak]
                hz = lucht_kleur(np.stack([dr_[:, 0], np.full(len(dr_), 0.015, np.float32), dr_[:, 2]], 1), licht,
                                 None, cirrus=False)
                f = 1 - np.exp(-tt[raak] / 2600.0)
                out[raak] = kl[raak] * (1 - f[:, None]) + hz * f[:, None]
            log(naam, "wolkenzee %.0fs" % (time.time() - t0))
    rgb = out.reshape(Hs, Ws, 3)
    if stralen:
        rgb = _stralen(rgb, cam, B, zk, W, stralen, licht, ss)
        log(naam, "stralen %.0fs" % (time.time() - t0))
    if gloed:
        em = (B.em > 0.5)[..., None] * np.clip(rgb, 0, 1)
        emi = Image.fromarray((em * 255).astype(np.uint8))
        b1 = np.asarray(emi.filter(ImageFilter.GaussianBlur(3 * ss)), np.float32) / 255
        b2 = np.asarray(emi.filter(ImageFilter.GaussianBlur(14 * ss)), np.float32) / 255
        bloom = (b1 * 0.6 + b2 * 1.6) * gloed
        rgb = 1 - (1 - np.clip(rgb, 0, 1)) * (1 - np.clip(bloom, 0, 1))
    if kleur:
        rgb = kleur(rgb)
    rgb = np.clip(rgb, 0, 1)
    rgb = rgb * rgb * (3 - 2 * rgb) * 0.25 + rgb * 0.75
    yy, xx = np.mgrid[0:Hs, 0:Ws].astype(np.float32)
    vg = 1 - 0.22 * (((xx / Ws - 0.5) * 1.6) ** 2 + ((yy / Hs - 0.5) * 1.9) ** 2)
    rgb = rgb * np.clip(vg, 0.6, 1)[..., None]
    img = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8))
    img = img.resize((breed, hoog), Image.LANCZOS)
    if deeltjes and getattr(W, "deeltjes", None):
        img = _deeltjes(img, W, cam, B.zb, ss)
    dr = ImageDraw.Draw(img)
    if titel:
        dr.text((22, 14), titel, font=_font(34), fill=(255, 248, 240), stroke_width=3, stroke_fill=(110, 50, 80))
    if onderschrift:
        dr.text((22, 58), onderschrift, font=_font(18, False), fill=(255, 246, 240), stroke_width=2,
                stroke_fill=(110, 50, 80))
    pad = os.path.join(OUT, naam + ".png")
    img.save(pad)
    if W.labels:
        o2 = img.copy()
        d2 = ImageDraw.Draw(o2)
        for (x, y, z, tekst) in W.labels:
            sx, sy, dz = cam.project(np.array(W.naar_voxel((x, y, z)), float))
            if dz < 0 and 0 <= sx < Ws and 0 <= sy < Hs:
                K.ballon(d2, sx / ss, sy / ss, tekst, sz=17)
        o2.save(os.path.join(OUT, naam + "_namen.png"))
    log(naam, "klaar %.0fs" % (time.time() - t0), img.size, pad)
    return pad
