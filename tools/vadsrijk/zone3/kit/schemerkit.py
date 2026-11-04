"""Schemerkit: bouw- en renderkit voor zone 3 "De Schemerwortel" (Guhs 1.10, Het Vadsrijk).

Bovenop de kopieën van de zone-1-kit in deze map (vrkit/erfkit/mijnkit/vibekit/mcrender/mcextra/stadkit;
assets = renders-erfenis/_bron/jars/guhs-assets-1.8.1.zip, alleen gelezen). Renders gaan naar renders-schemerwortel/.

Kern:
  Wereld(x1, y1, z1, x2, y2, z2, k)
      Een venster op de zone in WERELDCOORDINATEN RELATIEF AAN DE STAMAS T (T = wereld -2300, 0).
      k = voxels per blok: k=1 is de echte schaal (close-ups), k=0.5 is half (het hele vak in één scène).
      Alle bouwfuncties werken in wereldcoördinaten, dus hetzelfde bouwscript vult elk venster en elke schaal.
      Hulpjes: kolom (per kolom vullen tussen twee hoogtekaarten), bol, buis (tak/wortel langs een lijn),
      wset/wfill (losse blokken), kies (blokmix via hash), vnoise/vnoise3 (deterministische waarderuis).
  render(W, naam, ...)
      Render + nabewerking met de dieptebuffer: nevel op afstand, schemer onder de kruin (met zonnebundels),
      donker + gloed (tweede pass met alleen lichtblokken) voor grotten en avond, labels, titel, uitsnede.
  T_WERELD: om naar echte spelcoördinaten te rekenen (wereld_x = x + T_WERELD[0]).
"""
import os, sys, math, time, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import vrkit as VK  # noqa: E402
import stadkit as SK  # noqa: E402

V, R, E, K = VK.V, VK.R, VK.E, VK.K
OUT = os.path.abspath(os.path.join(HERE, ".."))
V.OUT = K.OUT = VK.OUT = SK.OUT = OUT
T_WERELD = (-2300, 0)

# kleuren voor getinte blokken: goudgroen blad, turquoise sap
R.GRASS = (150, 204, 92)
R.FOLIAGE = (198, 192, 66)
R.WATER = (70, 214, 206)

V.EMISSIVE |= {"minecraft:pearlescent_froglight", "minecraft:ochre_froglight", "minecraft:verdant_froglight",
               "minecraft:shroomlight", "minecraft:glowstone", "minecraft:sea_lantern", "minecraft:lantern",
               "minecraft:soul_lantern", "minecraft:end_rod", "minecraft:campfire", "minecraft:torch",
               "minecraft:wall_torch", "minecraft:jack_o_lantern", "minecraft:redstone_lamp",
               "minecraft:cave_vines_plant", "minecraft:cave_vines", "minecraft:magma_block",
               "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "guhs:guh_kristal_lamp",
               "guhs:guh_kristal_cluster", "guhs:guh_kristal_blok", "guhs:weidekristal_roze", "guhs:weidekristal_lila",
               "guhs:weidekristal_cyaan", "guhs:glimpaddenstoel", "guhs:glimmos", "guhs:lichtmosblok", "guhs:lichtmos",
               "guhs:glimtegel", "guhs:sterrenlantaarn", "guhs:guh_lantaarnpaal", "guhs:japan_lampion",
               "guhs:elftocht_lampion", "guhs:park_lantaarn", "guhs:kaasmos", "guhs:paddenstoellampje",
               "guhs:schimmelguh_licht", "guhs:belletjeskristal"}
EMISSIVE = V.EMISSIVE

Scene, stairs, slab, has_block = VK.Scene, VK.stairs, VK.slab, VK.has_block


# ===================================================================================================== ruis
def _hash2(ix, iz, seed):
    h = (ix.astype(np.int64) * 374761393 + iz.astype(np.int64) * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def _hash3(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 1103515245 + iz.astype(np.int64) * 668265263
         + seed * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def hash3(x, y, z, seed=0):
    """Deterministisch 0..1 per blok (wereldcoördinaten, ook arrays)."""
    return _hash3(np.floor(np.asarray(x, float)), np.floor(np.asarray(y, float)), np.floor(np.asarray(z, float)),
                  seed)


def vnoise(x, z, cell=32.0, seed=1, oct=3):
    """Gladde waarderuis -1..1 op wereldcoördinaten (arrays)."""
    x = np.asarray(x, float)
    z = np.asarray(z, float)
    tot, amp, out = 0.0, 1.0, 0.0
    for o in range(oct):
        fx, fz = x / cell, z / cell
        ix, iz = np.floor(fx), np.floor(fz)
        tx, tz = fx - ix, fz - iz
        tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
        s = seed * 31 + o * 7
        a = _hash2(ix, iz, s)
        b = _hash2(ix + 1, iz, s)
        c = _hash2(ix, iz + 1, s)
        d = _hash2(ix + 1, iz + 1, s)
        v = (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz
        out = out + amp * (v * 2 - 1)
        tot += amp
        amp *= 0.5
        cell /= 2.0
    return out / tot


def vnoise3(x, y, z, cell=24.0, seed=1, oct=2):
    x, y, z = (np.asarray(a, float) for a in (x, y, z))
    tot, amp, out = 0.0, 1.0, 0.0
    for o in range(oct):
        f = [a / cell for a in (x, y, z)]
        i = [np.floor(a) for a in f]
        t = [a - b for a, b in zip(f, i)]
        t = [a * a * (3 - 2 * a) for a in t]
        s = seed * 31 + o * 7
        acc = 0.0
        for dx in (0, 1):
            for dy in (0, 1):
                for dz in (0, 1):
                    w = (t[0] if dx else 1 - t[0]) * (t[1] if dy else 1 - t[1]) * (t[2] if dz else 1 - t[2])
                    acc = acc + w * _hash3(i[0] + dx, i[1] + dy, i[2] + dz, s)
        out = out + amp * (acc * 2 - 1)
        tot += amp
        amp *= 0.5
        cell /= 2.0
    return out / tot


# ===================================================================================================== wereld
class Wereld(Scene):
    """Venster [x1,x2) x [y1,y2) x [z1,z2) in wereldcoördinaten t.o.v. de stamas, k voxels per blok."""

    def __init__(self, x1, y1, z1, x2, y2, z2, k=1.0):
        X, Y, Z = int(round((x2 - x1) * k)), int(round((y2 - y1) * k)), int(round((z2 - z1) * k))
        super().__init__(X, Y, Z)
        self.k = k
        self.b = (x1, y1, z1, x2, y2, z2)
        self.xs = x1 + (np.arange(X) + 0.5) / k
        self.ys = y1 + (np.arange(Y) + 0.5) / k
        self.zs = z1 + (np.arange(Z) + 0.5) / k
        self.XX, self.ZZ = np.meshgrid(self.xs, self.zs, indexing="ij")
        self.RR = np.hypot(self.XX, self.ZZ)
        self.TH = np.arctan2(self.ZZ, self.XX)
        self.labels = []
        self.extra = []

    # ---- coördinaten
    def vx(self, x):
        return int(math.floor((x - self.b[0]) * self.k))

    def vy(self, y):
        return int(math.floor((y - self.b[1]) * self.k))

    def vz(self, z):
        return int(math.floor((z - self.b[2]) * self.k))

    def binnen(self, x, y, z, marge=0):
        x1, y1, z1, x2, y2, z2 = self.b
        return x1 - marge <= x < x2 + marge and y1 - marge <= y < y2 + marge and z1 - marge <= z < z2 + marge

    def raakt(self, x1, y1, z1, x2, y2, z2):
        """Overlapt de doos (wereld) met dit venster?"""
        a = self.b
        return not (x2 < a[0] or x1 >= a[3] or y2 < a[1] or y1 >= a[4] or z2 < a[2] or z1 >= a[5])

    def naar_voxel(self, p):
        """Wereldpunt -> voxelpunt (voor extra-quads en labels)."""
        x1, y1, z1 = self.b[:3]
        return ((p[0] - x1) * self.k, (p[1] - y1) * self.k, (p[2] - z1) * self.k)

    # ---- losse blokken (wereld)
    def wset(self, x, y, z, s, **kw):
        self.set(self.vx(x), self.vy(y), self.vz(z), s, **kw)

    def wget(self, x, y, z):
        return self.get(self.vx(x), self.vy(y), self.vz(z))

    def wfill(self, x1, y1, z1, x2, y2, z2, s, **kw):
        a, b = sorted((self.vx(x1), self.vx(x2)))
        c, d = sorted((self.vy(y1), self.vy(y2)))
        e, f = sorted((self.vz(z1), self.vz(z2)))
        X, Y, Z = self.v.shape
        a, c, e = max(a, 0), max(c, 0), max(e, 0)
        b, d, f = min(b, X - 1), min(d, Y - 1), min(f, Z - 1)
        if a > b or c > d or e > f:
            return
        i = self.id(s)
        sl = self.v[a:b + 1, c:d + 1, e:f + 1]
        if kw.get("keep"):
            sl[sl == 0] = i
        else:
            sl[...] = i
        if kw.get("roof"):
            for p in np.argwhere(sl == i):
                self.roof.add((a + p[0], c + p[1], e + p[2]))

    def wguh(self, x, y, z, yaw=180, **kw):
        """Guh-figuurtje op wereldpositie (alleen zinvol bij k=1)."""
        if self.binnen(x, y, z):
            vx, vy, vz = self.naar_voxel((x, y, z))
            self.guh(vx, vy, vz, yaw, **kw)

    def label(self, x, y, z, tekst):
        self.labels.append((x, y, z, tekst))

    # ---- grote vormen (gevectoriseerd)
    def ids(self, blokken):
        return np.array([self.id(b) for b in blokken], np.uint16)

    def kies(self, blokken, gewichten, x, y, z, seed=0):
        """Blokmix: geeft een id-array (vorm van x) op basis van hash3."""
        ids = self.ids(blokken)
        w = np.cumsum(np.asarray(gewichten, float))
        w /= w[-1]
        h = hash3(x, y, z, seed)
        return ids[np.searchsorted(w, h, side="right").clip(0, len(ids) - 1)]

    def kolom(self, bot, top, blok, mask=None, keep=False, laagfn=None):
        """Vul per kolom van wereld-y bot tot en met top (2D-arrays over het venster).
        blok: blocknaam, id-array (2D) of None met laagfn(y_wereld, kolommasker) -> id-array (2D)."""
        bot = np.asarray(bot, float)
        top = np.asarray(top, float)
        m0 = top >= bot
        if mask is not None:
            m0 = m0 & mask
        if not m0.any():
            return
        lo = max(0, self.vy(np.nanmin(np.where(m0, bot, np.inf))))
        hi = min(self.v.shape[1] - 1, self.vy(np.nanmax(np.where(m0, top, -np.inf))))
        if isinstance(blok, str):
            bid = self.id(blok)
        else:
            bid = blok
        for j in range(lo, hi + 1):
            y = self.ys[j]
            m = m0 & (bot <= y) & (y <= top + 0.999 / self.k)
            if not m.any():
                continue
            sl = self.v[:, j, :]
            if keep:
                m &= sl == 0
            if laagfn is not None:
                vals = laagfn(y, m)
                sl[m] = vals[m] if np.ndim(vals) else vals
            elif np.ndim(bid):
                sl[m] = bid[m]
            else:
                sl[m] = bid

    def _box(self, x1, y1, z1, x2, y2, z2):
        X, Y, Z = self.v.shape
        a, b = max(0, self.vx(x1)), min(X, self.vx(x2) + 1)
        c, d = max(0, self.vy(y1)), min(Y, self.vy(y2) + 1)
        e, f = max(0, self.vz(z1)), min(Z, self.vz(z2) + 1)
        if a >= b or c >= d or e >= f:
            return None
        return (slice(a, b), slice(c, d), slice(e, f)), np.meshgrid(self.xs[a:b], self.ys[c:d], self.zs[e:f],
                                                                     indexing="ij")

    def bol(self, c, r, blok, ry=None, rz=None, hollow=0.0, keep=False, ymin=None, ymax=None, mixfn=None):
        """(Afgeplatte) bol in wereldcoördinaten. mixfn(x,y,z,mask) -> id-array voor blokvariatie."""
        ry = r if ry is None else ry
        rz = r if rz is None else rz
        bx = self._box(c[0] - r - 1, c[1] - ry - 1, c[2] - rz - 1, c[0] + r + 1, c[1] + ry + 1, c[2] + rz + 1)
        if bx is None:
            return None
        sl, (x, y, z) = bx
        d = ((x - c[0]) / r) ** 2 + ((y - c[1]) / ry) ** 2 + ((z - c[2]) / rz) ** 2
        m = d <= 1
        if hollow:
            d2 = ((x - c[0]) / max(0.5, r - hollow)) ** 2 + ((y - c[1]) / max(0.5, ry - hollow)) ** 2 + \
                 ((z - c[2]) / max(0.5, rz - hollow)) ** 2
            m &= d2 > 1
        if ymin is not None:
            m &= y >= ymin
        if ymax is not None:
            m &= y <= ymax
        self._schrijf(sl, m, blok, keep, mixfn, (x, y, z))
        return m

    def _schrijf(self, sl, m, blok, keep, mixfn, xyz):
        v = self.v[sl]
        if keep:
            m = m & (v == 0)
        if not m.any():
            return
        if mixfn is not None:
            v[m] = mixfn(*xyz, m)[m]
        elif blok is None:
            v[m] = 0
        else:
            v[m] = self.id(blok)

    def buis(self, pts, rads, blok, keep=False, mixfn=None, plat=1.0):
        """Buis langs een polylijn (wereldpunten) met straal per punt; plat < 1 drukt de buis verticaal in.
        Gevectoriseerd per segment: afstand tot het lijnstuk <= geïnterpoleerde straal."""
        pts = np.asarray(pts, float)
        rads = np.asarray(rads, float)
        for i in range(len(pts) - 1):
            p0, p1 = pts[i], pts[i + 1]
            r0, r1 = rads[i], rads[i + 1]
            rm = max(r0, r1) + 1
            lo = np.minimum(p0, p1) - rm
            hi = np.maximum(p0, p1) + rm
            bx = self._box(*lo, *hi)
            if bx is None:
                continue
            sl, (x, y, z) = bx
            d = p1 - p0
            L2 = float(d @ d) or 1e-9
            t = ((x - p0[0]) * d[0] + (y - p0[1]) * d[1] + (z - p0[2]) * d[2]) / L2
            t = np.clip(t, 0, 1)
            qx, qy, qz = p0[0] + t * d[0], p0[1] + t * d[1], p0[2] + t * d[2]
            rr = r0 + (r1 - r0) * t
            dist2 = (x - qx) ** 2 + ((y - qy) / plat) ** 2 + (z - qz) ** 2
            self._schrijf(sl, dist2 <= rr * rr, blok, keep, mixfn, (x, y, z))

    def cilinder(self, cx, cz, r, y1, y2, blok, hollow=0.0, keep=False, mixfn=None):
        bx = self._box(cx - r - 1, y1, cz - r - 1, cx + r + 1, y2, cz + r + 1)
        if bx is None:
            return
        sl, (x, y, z) = bx
        d = np.hypot(x - cx, z - cz)
        m = d <= r
        if hollow:
            m &= d > r - hollow
        self._schrijf(sl, m, blok, keep, mixfn, (x, y, z))

    def leeg(self, x1, y1, z1, x2, y2, z2):
        self.wfill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def hoogte(self, x, z):
        """Hoogste niet-luchtblok (wereld-y) in de kolom op wereld (x, z), of None."""
        i, j = self.vx(x), self.vz(z)
        if not (0 <= i < self.v.shape[0] and 0 <= j < self.v.shape[2]):
            return None
        col = self.v[i, :, j]
        nz = np.nonzero(col)[0]
        return None if not len(nz) else self.ys[nz.max()] - 0.5 / self.k

    def boven(self, x, z):
        """y waarop je iets bovenop de kolom zet (wereld)."""
        h = self.hoogte(x, z)
        return None if h is None else int(round(h)) + 1


def quads_naar_voxel(W, qs):
    """Extra-quads (wereldcoördinaten) naar de voxelruimte van het venster."""
    x1, y1, z1 = W.b[:3]
    o = np.array([x1, y1, z1], float)
    out = []
    for (P, cu, tex, tint, tr) in qs:
        out.append(((np.asarray(P, float) - o) * W.k, cu, tex, tint, tr))
    return out


# ===================================================================================================== render
def _wereldpos(cam, zb, ox, oy, scale_px):
    H, W_ = zb.shape
    px = np.arange(W_, dtype=np.float32) + 0.5
    py = np.arange(H, dtype=np.float32) + 0.5
    a = (px - ox) / scale_px
    b = -(py - oy) / scale_px
    P = (a[None, :, None] * cam.right[None, None, :].astype(np.float32) +
         b[:, None, None] * cam.up[None, None, :].astype(np.float32) +
         zb[:, :, None] * cam.d[None, None, :].astype(np.float32))
    return P


def lucht(w, h, boven=(250, 196, 150), onder=(255, 236, 214), zon=None, wolken=0, seed=3, sterren=False,
          horizon=0.75):
    bg = VK.lucht(w, h, (boven, onder), zon=zon, wolken=wolken, seed=seed, horizon=horizon)
    if sterren:
        d = ImageDraw.Draw(bg, "RGBA")
        rng = random.Random(seed)
        for _ in range(int(w * h / 3000)):
            x, y = rng.random() * w, rng.random() * h * 0.6
            s = 1 if rng.random() < 0.8 else 2
            d.ellipse((x - s, y - s, x + s, y + s), fill=(255, 245, 230, int(100 + 120 * rng.random())))
    return bg


def render(W, naam, yaw=35, pitch=28, scale=4.0, ss=2, sky=((250, 196, 150), (255, 236, 214)), zon=None,
           wolken=0, sterren=False, titel=None, onderschrift=None, nevel=None, donker=None, gloed=False,
           post=None, crop=None, bounds=None, extra=(), hol=True, achtergrond=None, labels=True, schaduw=True):
    """Rendert venster W naar OUT/<naam>.png (en <naam>_namen.png als er labels zijn).

    nevel  = (kleur, sterkte 0..1, bereik in blokken): mengt naar een nevelkleur met de afstand tot de camera.
    donker = (factor rgb, gloedsterkte): alles donkerder, lichtblokken gloeien (tweede pass) — grotten/avond.
    gloed  = True: ook overdag een zachte gloed rond lichtblokken.
    post(rgb, alpha, P_wereld) -> rgb   eigen nabewerking per pixel (P in wereldcoördinaten t.o.v. T).
    achtergrond(w, h) -> RGBA-afbeelding in plaats van de lucht.
    """
    r, bk = V.res()
    t0 = time.time()
    W.fix_connections()
    vol = W.v.copy()
    if hol:
        n = hol_maken(W.pal, vol)
        print(naam, "binnenblokken weg:", n, flush=True)
    extras, hide = E.special_extras(vol, W.pal, bk)
    for (x, y, z, yaw_g, skin, sitting) in W.guhs:
        extras += E.guh(r, x, y, z, yaw_g, skin=skin, scale=0.8 * W.k, sitting=sitting)
    Xv, Yv, Zv = W.v.shape

    def in_venster(q):
        P = np.asarray(q[0])
        return bool(((P[:, 0] >= -2) & (P[:, 0] <= Xv + 2) & (P[:, 1] >= -2) & (P[:, 1] <= Yv + 2) &
                     (P[:, 2] >= -2) & (P[:, 2] <= Zv + 2)).any())
    extras = list(extras) + [q for q in list(extra) + list(W.extra) if in_venster(q)]
    if bounds:
        extras += [K.punt(*W.naar_voxel(p)) for p in bounds]
    sc = scale * ss
    cam = R.Camera(yaw, pitch, sc)
    im, (ox, oy) = R.render(vol, W.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=24 * ss,
                            emissive=EMISSIVE)
    zb = R.LAST_ZB
    print(naam, "pass 1 %.0fs" % (time.time() - t0), im.size, flush=True)
    glow = None
    if donker or gloed:
        oud = R.shade
        R.shade = lambda n_: 0.0
        try:
            glow, _ = R.render(vol, W.pal, cam, r, bk, hide=lambda s: s in hide, extras=extras, margin=24 * ss,
                               emissive=EMISSIVE)
        finally:
            R.shade = oud
        print(naam, "pass 2 %.0fs" % (time.time() - t0), flush=True)
    a = np.asarray(im, np.float32) / 255.0
    rgb, al = a[..., :3], a[..., 3]
    vox = _wereldpos(cam, zb, ox, oy, sc)
    P = vox / W.k + np.array(W.b[:3], np.float32)
    leeg_ = zb < -1e8
    P[leeg_] = np.nan
    if post is not None:
        rgb = post(rgb, al, P)
    if nevel:
        kl, sterk, bereik = nevel
        dz = zb / W.k
        dmax = np.nanmax(np.where(leeg_, np.nan, dz))
        f = np.clip((dmax - dz) / bereik, 0, 1) ** 0.9 * sterk
        f[leeg_] = 0
        rgb = rgb * (1 - f[..., None]) + np.array(kl, np.float32) / 255.0 * f[..., None]
    if donker:
        fac, gs = donker
        g = np.asarray(glow, np.float32)[..., :3] / 255.0
        rgb = rgb * np.array(fac, np.float32)
        rgb = np.maximum(rgb, g * 1.0)
    Hs, Ws = al.shape
    out = np.concatenate([np.clip(rgb, 0, 1), al[..., None]], 2)
    img = Image.fromarray((out * 255).astype(np.uint8), "RGBA")
    w2, h2 = Ws // ss, Hs // ss
    img = img.resize((w2, h2), Image.LANCZOS)
    if achtergrond is not None:
        bg = achtergrond(w2, h2)
    else:
        bg = lucht(w2, h2, *sky, zon=zon, wolken=wolken, sterren=sterren)
    if schaduw and achtergrond is None:
        sh = Image.new("RGBA", img.size, (90, 50, 80, 0))
        sa = img.split()[3].filter(ImageFilter.GaussianBlur(12))
        sh.putalpha(sa.point(lambda v: int(v * 0.22)))
        bg.paste(sh, (8, 14), sh)
    bg.paste(img, (0, 0), img)
    if donker or gloed:
        gs = donker[1] if donker else 0.55
        gl = glow.resize((w2, h2), Image.LANCZOS).convert("RGB")
        b1 = np.asarray(gl.filter(ImageFilter.GaussianBlur(3)), np.float32) / 255.0
        b2 = np.asarray(gl.filter(ImageFilter.GaussianBlur(16)), np.float32) / 255.0
        bloom = (b1 * 0.7 + b2 * 2.0 * np.array([1.0, 0.85, 0.8])) * gs
        o = np.asarray(bg.convert("RGB"), np.float32) / 255.0
        o = 1 - (1 - o) * (1 - np.clip(bloom, 0, 1) * 0.85)
        bg = Image.fromarray((np.clip(o, 0, 1) * 255).astype(np.uint8)).convert("RGBA")
    bg = bg.convert("RGB")

    def proj(p):
        v = np.array(W.naar_voxel(p), float)
        sx, sy, _ = cam.project(v)
        return (sx + ox) / ss, (sy + oy) / ss

    cx = (0, 0, w2, h2)
    W.crop_px = cx
    if crop:
        cx = (int(crop[0] * w2), int(crop[1] * h2), int(crop[2] * w2), int(crop[3] * h2))
        bg = bg.crop(cx)
    W.crop_px = cx
    d = ImageDraw.Draw(bg)
    licht_tekst = donker is not None
    kl1, kl2 = ((255, 236, 246), (246, 220, 236)) if licht_tekst else ((120, 40, 90), (140, 80, 110))
    if titel:
        d.text((18, 12), titel, font=K._font(30, True), fill=kl1, stroke_width=2,
               stroke_fill=(60, 20, 50) if licht_tekst else (255, 246, 240))
    if onderschrift:
        d.text((18, 50), onderschrift, font=K._font(17), fill=kl2, stroke_width=2,
               stroke_fill=(60, 20, 50) if licht_tekst else (255, 246, 240))
    pad = os.path.join(OUT, naam + ".png")
    bg.save(pad)
    if W.labels and labels:
        o2 = bg.copy()
        d2 = ImageDraw.Draw(o2)
        for (x, y, z, tekst) in W.labels:
            sx, sy = proj((x, y, z))
            K.ballon(d2, sx - cx[0], sy - cx[1], tekst, sz=17)
        o2.save(os.path.join(OUT, naam + "_namen.png"))
    print(naam, "klaar %.0fs" % (time.time() - t0), bg.size, flush=True)
    W.proj = proj
    return pad


def _is_kubus(bk, s):
    """Volle kubus (ook bladeren, glas, honing): één element van 0..16."""
    if s.split("[")[0] in R.AIR:
        return False
    F = bk.bake(s)
    if F.full_opaque:
        return True
    name, props, models = bk.models_for(s)
    els = []
    for mv in models:
        e, _ = bk.res.model(mv["model"])
        els += e or []
    return len(els) == 1 and els[0]["from"] == [0, 0, 0] and els[0]["to"] == [16, 16, 16] and         "water" not in s and "honey" not in s


def hol_maken(pal, vol):
    """Haalt blokken weg die aan alle 6 kanten door volle kubussen (ook bladeren/glas) zijn ingesloten.
    Onzichtbaar, maar scheelt veel rendertijd (de renderer loopt in Python over elk blok)."""
    r, bk = V.res()
    kub = np.array([_is_kubus(bk, s) for s in pal])
    ond = np.array([s.split("[")[0] not in R.AIR and bk.bake(s).full_opaque for s in pal])

    def ingesloten(occ):
        inn = np.zeros_like(occ)
        inn[1:-1, 1:-1, 1:-1] = occ[1:-1, 1:-1, 1:-1] & occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] &             occ[1:-1, :-2, 1:-1] & occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
        return inn
    a = ingesloten(ond[vol])
    b = ingesloten(kub[vol])
    b = ingesloten(b)  # bij bladeren/glas twee lagen laten staan (je kijkt door de gaatjes)
    weg = a | b
    vol[weg] = 0
    return int(weg.sum())


def schemer_post(sterkte=0.55, bundels=True, kruin_r=330, kruin_y=190, seed=5):
    """Nabewerking: onder de kruin (r < kruin_r, y < kruin_y) is het schemer; zonnebundels lichten plekken op."""
    def f(rgb, al, P):
        x, y, z = P[..., 0], P[..., 1], P[..., 2]
        r = np.hypot(x, z)
        rand = kruin_r * (1 + 0.08 * np.sin(np.arctan2(z, x) * 5))
        onder = np.clip((rand - r) / 60.0, 0, 1) * np.clip((kruin_y - 8 - y) / 30.0, 0, 1)
        onder = np.nan_to_num(onder)
        dim = 1 - sterkte * onder
        kleur = np.ones_like(rgb)
        kleur[...] = (1.0, 0.94, 1.04)
        if bundels:
            # schuine zonnebundels: gaten in het bladerdak, verschoven langs de lichtrichting
            sx = x + (y - kruin_y) * 0.35
            sz = z + (y - kruin_y) * 0.2
            n = vnoise(np.nan_to_num(sx), np.nan_to_num(sz), cell=26, seed=seed, oct=2)
            bun = np.clip((n - 0.38) * 4, 0, 1) * onder
            dim = dim + bun * sterkte * 1.25
            kleur = kleur * (1 - bun[..., None] * 0.3) + bun[..., None] * 0.3 * np.array((1.12, 1.0, 0.72))
        return rgb * dim[..., None] * kleur
    return f


def hoogtenevel_post(y0=60, y1=170, kleur=(246, 206, 196), sterkte=0.65):
    """Hoe lager, hoe nevelig: geeft diepte onder takken en de kruin."""
    kl = np.array(kleur, np.float32) / 255.0

    def f(rgb, al, P):
        y = np.nan_to_num(P[..., 1], nan=y1)
        t = np.clip((y1 - y) / (y1 - y0), 0, 1) ** 1.2 * sterkte
        return rgb * (1 - t[..., None]) + kl * t[..., None]
    return f


def lichtcirkel_post(punten, straal=12.0, kleur=(1.0, 0.82, 0.55), kracht=1.0):
    """Lichtcirkels (bv. de Nestlantaarn) voor een donkere scène: gebruik samen met donker=..."""
    def f(rgb, al, P):
        tot = np.zeros(rgb.shape[:2], np.float32)
        for (px, py, pz) in punten:
            d = np.sqrt((P[..., 0] - px) ** 2 + (P[..., 1] - py) ** 2 + (P[..., 2] - pz) ** 2)
            tot = np.maximum(tot, np.nan_to_num(np.clip(1 - d / straal, 0, 1)) ** 1.3)
        return rgb * (1 + tot[..., None] * kracht * 2.2 * np.array(kleur, np.float32))
    return f


def combineer(*fs):
    def f(rgb, al, P):
        for g in fs:
            if g is not None:
                rgb = g(rgb, al, P)
        return rgb
    return f


# ===================================================================================================== stempels
def stempel(W, S, wx, wy, wz, lucht_ook=False):
    """Zet een lokale Scene S (1 voxel = 1 blok, eigen (0,0,0)) in venster W met (0,0,0) op wereld (wx,wy,wz).
    Werkt op elke schaal k (bij k != 1 wordt bemonsterd). Guh-figuurtjes en roof-blokken gaan mee bij k == 1.
    lucht_ook=True: ook de lucht uit S overnemen (uithollen)."""
    sX, sY, sZ = S.v.shape
    if not W.raakt(wx, wy, wz, wx + sX, wy + sY, wz + sZ):
        return
    remap = np.array([W.id(p) for p in S.pal], np.uint16)
    X, Y, Z = W.v.shape
    a, b = max(0, W.vx(wx)), min(X, W.vx(wx + sX - 1e-6) + 1)
    c, d = max(0, W.vy(wy)), min(Y, W.vy(wy + sY - 1e-6) + 1)
    e, f = max(0, W.vz(wz)), min(Z, W.vz(wz + sZ - 1e-6) + 1)
    if a >= b or c >= d or e >= f:
        return
    ix = np.clip(np.floor(W.xs[a:b] - wx).astype(int), 0, sX - 1)
    iy = np.clip(np.floor(W.ys[c:d] - wy).astype(int), 0, sY - 1)
    iz = np.clip(np.floor(W.zs[e:f] - wz).astype(int), 0, sZ - 1)
    src = S.v[np.ix_(ix, iy, iz)]
    dst = W.v[a:b, c:d, e:f]
    if lucht_ook:
        dst[...] = remap[src]
    else:
        m = src != 0
        dst[m] = remap[src[m]]
    if W.k == 1:
        ox, oy, oz = W.vx(wx), W.vy(wy), W.vz(wz)
        for (x, y, z) in S.roof:
            W.roof.add((x + ox, y + oy, z + oz))
        for (x, y, z, yaw, skin, sit) in S.guhs:
            if 0 <= x + ox < X and 0 <= y + oy < Y and 0 <= z + oz < Z:
                W.guhs.append((x + ox, y + oy, z + oz, yaw, skin, sit))


class Lokaal(Scene):
    """Lokale bouwplaats (k=1) met wereld-anker, zodat bouwscripts in eigen coördinaten kunnen werken.
    Na het bouwen: stempel(W, L, *L.anker)."""

    def __init__(self, X, Y, Z, anker=(0, 0, 0)):
        super().__init__(X, Y, Z)
        self.anker = anker
