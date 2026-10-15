"""
einde2, slice cast_volk - the workbench of the cast (CONTRACT_E2 11.2): one `Pop` per GuhNpcEntity.Kind.

A Pop loads one of the two base bodies read-only (`zit` = guh_sitting, `mika` = mika), recolours the fur, PAINTS clothes, hair
and face paint straight onto the faces of the base cubes (`verf`: every texel knows where it lies in the model, so "a belt at
y 5..6" or "white paint on the front of the head" is one line and stays right over the overlapping cubes of the round body),
and hangs extra bones on the base bones whose faces each sample one 8 x 8 swatch in a free cell of ITS OWN sheet
(knuffeldal_npcs `_swatches` / `_cube` / `_paint`). Head things are written in the SITTING GUH's head coordinates for both
bodies (`k`, `kbot`): the Mika's head is the same head, 12 lower and 5 further to the front.

Also here: the little animation language (tracks, the idle with the signature move woven in, props that only show during
a move), the spirit / statue finishes, and a quick offline renderer for the previews (wiki_renders' quads, numpy z-buffer).
"""
import copy
import json
import math
import os

import numpy as np
from PIL import Image

from features import einde2_kit as kit
from features import knuffeldal_npcs as kn

PX = 4                       # texture pixels per uv unit (512 px sheet, 128 uv)
SW = kn.SW
VLAKKEN = ("north", "south", "east", "west", "up", "down")
# lijf -> (base model, base texture, head offset from the sitting guh's head, eye centre x / y / radius in guh head coordinates)
LIJVEN = {"zit": ("guh_sitting.geo.json", "guh_sitting.png", (0.0, 0.0, 0.0), (4.25, 21.5, 2.7)),
          "mika": ("mika.geo.json", "mika.png", (0.0, -12.0, -5.0), (3.5, 19.6, 2.4))}
BASISBOTTEN = {"zit": ("root", "body", "tail", "foot_left", "foot_right", "arm_left", "arm_right", "head", "ear_left", "ear_right"),
               "mika": ("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right",
                        "leg_front_left", "leg_front_right")}
KOP_PIVOT = (0.0, 19.5, 0.0)


def _hsv(rgb):
    mx, mn = rgb.max(-1), rgb.min(-1)
    s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) / 6
    return h, s, mx


def _vacht(h, s, v):
    """The fur of the two base sheets (and not the Mika's red eyes, which are pinkish to make_v2.pinkish too)."""
    return ((h > 0.85) | (h < 0.05)) & (s > 0.12) & (s < 0.6)


def _herkleur(a, hue, sat, val, masker):
    """make_v2.recolour for the fur, but the deep pink parts (the snout, the inside of the ears, the pads) do not turn
    orange when a fur gets more colour: the saturation is capped."""
    if hue is None and sat == 1.0 and val == 1.0:
        return a
    rgb = a[..., :3].astype(np.float32) / 255
    hh, s, v = _hsv(rgb)
    nh = np.full(hh.shape, hh if hue is None else hue, np.float32) if hue is not None else hh
    ns = np.minimum(s * sat, np.maximum(s, 0.46))
    nv = np.clip(v * val, 0, 1)
    i = np.floor(nh * 6).astype(int) % 6
    f = nh * 6 - np.floor(nh * 6)
    p, q, t = nv * (1 - ns), nv * (1 - f * ns), nv * (1 - (1 - f) * ns)
    out = np.zeros_like(rgb)
    for n, (x, y, z) in enumerate([(nv, t, p), (q, nv, p), (p, nv, t), (p, q, nv), (t, p, nv), (nv, p, q)]):
        sel = i == n
        out[..., 0][sel], out[..., 1][sel], out[..., 2][sel] = x[sel], y[sel], z[sel]
    a[masker, :3] = (out[masker] * 255).astype(np.uint8)
    return a


def _patroon_sleutel(patroon):
    """A key that names what a pattern paints: where its function is written and what it captured (never its address: a
    fresh closure can get the address of a dead one, which made the swatches depend on what ran before in the process)."""
    if patroon is None:
        return None
    code = getattr(patroon, "__code__", None)
    if code is None:
        return (repr(patroon),)
    cellen = tuple(_patroon_sleutel(v) if callable(v) else repr(v) for v in (c.cell_contents for c in (patroon.__closure__ or ())))
    return (os.path.basename(code.co_filename), code.co_firstlineno, code.co_name, cellen, repr(patroon.__defaults__))


class Pop:
    """One character: its model (base body + extra bones) and its 512 px sheet."""

    def __init__(self, h, kind, lijf, hue=None, sat=1.0, val=1.0):
        self.h, self.kind, self.lijf = h, kind, lijf
        model, tex, self.dk, self.oog = LIJVEN[lijf]
        self.geo_file = kn._load(h, model)
        self.geo = self.geo_file["minecraft:geometry"][0]
        self.geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
        self.geo["bones"] = [b for b in self.geo["bones"] if b["name"] in BASISBOTTEN[lijf]]
        used = kn._used(self.geo)
        self.cellen = [(x, y) for y in range(0, kn.UV - SW + 1, SW) for x in range(0, kn.UV - SW + 1, SW)
                       if not used[y:y + SW, x:x + SW].any()]
        self.sw = {}
        self.rng = kit.rng(f"cast_volk/{kind}")
        src = Image.open(os.path.join(h.TEX, "entity", tex)).convert("RGBA")
        bron = np.asarray(src).astype(np.float32) / 255
        self.vachtmasker = _vacht(*_hsv(bron[..., :3])) & (bron[..., 3] > 0)
        self.a = _herkleur(np.asarray(src).copy(), hue, sat, val, self.vachtmasker)
        # what must stay when the head is painted: the eyes and the mouth (everything on the face that is no fur)
        self.vast = np.zeros(self.a.shape[:2], bool)
        x0, y0, x1, y1 = self._rechthoek(self.bone("head")["cubes"][2], "north")
        self.gezicht = (x0, y0, x1, y1)
        self.vast[y0:y1, x0:x1] = ~self.vachtmasker[y0:y1, x0:x1]
        bx0, by0, bx1, by1 = self._rechthoek(self.bone("body")["cubes"][3], "east")
        self.huid = tuple(int(v) for v in np.median(self.a[by0:by1, bx0:bx1, :3].reshape(-1, 3), 0))
        self.mix = {}
        self.gloei = None

    # --- the base ---------------------------------------------------------------------------------------------------------
    def bone(self, name):
        for b in self.geo["bones"]:
            if b["name"] == name:
                return b
        raise KeyError(name)

    @staticmethod
    def _rechthoek(cube, vlak):
        (u, v), (w, hh) = cube["uv"][vlak]["uv"], cube["uv"][vlak]["uv_size"]
        return int(round(u * PX)), int(round(v * PX)), int(round((u + w) * PX)), int(round((v + hh) * PX))

    def _punten(self, cube, vlak):
        """The model position (X, Y, Z arrays) of every texel of a face of a base cube (the layout wiki_renders.geo_quads uses)."""
        x0, y0, x1, y1 = self._rechthoek(cube, vlak)
        nu, nv = x1 - x0, y1 - y0
        a, b = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        (ox, oy, oz), (w, hh, d) = cube["origin"], cube["size"]
        ex, ey, ez = ox + w, oy + hh, oz + d
        o, uu, vv = {"north": ((ex, ey, oz), (-w, 0, 0), (0, -hh, 0)), "south": ((ox, ey, ez), (w, 0, 0), (0, -hh, 0)),
                     "east": ((ex, ey, ez), (0, 0, -d), (0, -hh, 0)), "west": ((ox, ey, oz), (0, 0, d), (0, -hh, 0)),
                     "up": ((ex, ey, oz), (-w, 0, 0), (0, 0, d)), "down": ((ex, oy, ez), (-w, 0, 0), (0, 0, -d))}[vlak]
        return (x0, y0, x1, y1), tuple(o[i] + a * uu[i] + b * vv[i] for i in range(3))

    def verf(self, botten, kleur, waar=None, var=5, vlakken=None, over=False, kop=None):
        """Paints the base cubes of these bones where waar(x, y, z, vlak) says so (model coordinates; for the head the sitting
        guh's head coordinates on both bodies). The eyes and the mouth stay unless over=True. kleur: an rgb, or a function
        (x, y, z) -> an rgb array."""
        for naam in ([botten] if isinstance(botten, str) else botten):
            is_kop = naam in ("head", "ear_left", "ear_right") if kop is None else kop
            dx, dy, dz = self.dk if is_kop else (0, 0, 0)
            for cube in self.bone(naam)["cubes"]:
                if isinstance(cube.get("uv"), list):
                    continue
                for vlak in cube["uv"]:
                    if vlakken and vlak not in vlakken:
                        continue
                    (x0, y0, x1, y1), (X, Y, Z) = self._punten(cube, vlak)
                    X, Y, Z = X - dx, Y - dy, Z - dz
                    m = np.ones(X.shape, bool) if waar is None else np.broadcast_to(np.asarray(waar(X, Y, Z, vlak), bool), X.shape).copy()
                    if not over:
                        m &= ~self.vast[y0:y1, x0:x1]
                    if not m.any():
                        continue
                    c = np.asarray(kleur(X, Y, Z) if callable(kleur) else kleur, np.float32)
                    c = np.broadcast_to(c, X.shape + (3,)) + self.rng.normal(0, var / 2, X.shape + (1,))
                    blok = self.a[y0:y1, x0:x1]
                    blok[m, :3] = np.clip(c[m], 0, 255).astype(np.uint8)
                    blok[m, 3] = 255

    def ogen(self, links=1.0, rechts=1.0):
        """Makes an eye bigger or smaller (links = the +x eye, on the left of the sheet). Call it before any paint on the face."""
        x0, y0, x1, y1 = self.gezicht
        mid = (x0 + x1) // 2
        vacht = tuple(int(v) for v in np.median(self.a[y0:y1, x0:x1][~self.vast[y0:y1, x0:x1]].reshape(-1, 4), 0))
        ey = y0 + int(round((24.5 + self.dk[1] - (self.oog[1] + self.dk[1])) * PX))
        for (ax0, ax1), f in (((x0, mid), links), ((mid, x1), rechts)):
            if f == 1.0:
                continue
            m = self.vast[y0:y1, ax0:ax1].copy()
            rows = np.where(m.any(1))[0]
            rows = rows[rows < (ey - y0) + int(self.oog[2] * PX) + 2]              # the eye, not the mouth under it
            cols = np.where(m[rows.min():rows.max() + 1].any(0))[0]
            bx0, bx1, by0, by1 = ax0 + cols.min(), ax0 + cols.max() + 1, y0 + rows.min(), y0 + rows.max() + 1
            oog = Image.fromarray(self.a[by0:by1, bx0:bx1].copy())
            masker = Image.fromarray((self.vast[by0:by1, bx0:bx1] * 255).astype(np.uint8))
            self.a[by0:by1, bx0:bx1][self.vast[by0:by1, bx0:bx1]] = vacht
            self.vast[by0:by1, bx0:bx1] = False
            nw, nh = max(2, int(round(oog.width * f))), max(2, int(round(oog.height * f)))
            oog, masker = oog.resize((nw, nh), Image.NEAREST), np.asarray(masker.resize((nw, nh), Image.NEAREST)) > 127
            cx, cy = (bx0 + bx1) // 2, (by0 + by1) // 2
            px0, py0 = cx - nw // 2, cy - nh // 2
            px0, py0 = min(max(px0, ax0), ax1 - nw), min(max(py0, y0), y1 - nh)
            doel = self.a[py0:py0 + nh, px0:px0 + nw]
            doel[masker] = np.asarray(oog)[masker]
            self.vast[py0:py0 + nh, px0:px0 + nw] |= masker

    # --- extra bones --------------------------------------------------------------------------------------------------------
    def kl(self, rgb, patroon=None, var=6):
        """The swatch of a colour (with an optional pattern function over the 32 x 32 px block), made on first use."""
        key = (tuple(rgb), _patroon_sleutel(patroon))
        if key not in self.sw:
            if not self.cellen:
                raise SystemExit(f"einde2_cast_volk: no free texture space on the sheet of {self.kind}")
            self.sw[key] = self.cellen.pop(0)
            kn._paint(self.a, self.sw[key], rgb, self.rng, var, patroon)
        return self.sw[key]

    def c(self, origin, size, rgb, inflate=0.0, patroon=None):
        return kn._cube([float(v) for v in origin], [float(v) for v in size], self.kl(rgb, patroon), inflate)

    def k(self, origin, size, rgb, inflate=0.0, patroon=None):
        """A cube in the sitting guh's head coordinates."""
        return self.c([origin[i] + self.dk[i] for i in range(3)], size, rgb, inflate, patroon)

    def bot(self, naam, ouder, pivot, cubes, rot=None):
        if any(b["name"] == naam for b in self.geo["bones"]):
            raise SystemExit(f"einde2_cast_volk: {self.kind} has the bone {naam} twice")
        b = {"name": naam, "parent": ouder, "pivot": [float(v) for v in pivot], "cubes": cubes}
        if rot:
            b["rotation"] = [float(v) for v in rot]
        self.geo["bones"].append(b)
        if naam.startswith("mix_"):
            self.mix.setdefault(int(naam.split("_")[1]), []).append(naam)
        return naam

    def kbot(self, naam, cubes, pivot=KOP_PIVOT, rot=None, ouder="head"):
        return self.bot(naam, ouder, [pivot[i] + self.dk[i] for i in range(3)], cubes, rot)

    # --- finishes -------------------------------------------------------------------------------------------------------------
    def geest(self, tint=(170, 214, 255), mate=0.3, alfa=188, gloed=0.42):
        """A memory or a spirit: everything fades towards a pale light, see-through (the eyes stay), and a soft glow all over."""
        op = self.a[..., 3] > 0
        rgb = self.a[..., :3].astype(np.float32)
        rgb = rgb * (1 - mate) + np.array(tint, np.float32) * mate
        self.a[..., :3] = np.where(op[..., None], np.clip(rgb, 0, 255), self.a[..., :3]).astype(np.uint8)
        self.a[..., 3] = np.where(op & ~self.vast, alfa, self.a[..., 3])
        g = np.zeros_like(self.a)
        g[..., :3] = np.where(op[..., None], np.clip(rgb * gloed, 0, 255), 0).astype(np.uint8)
        g[..., 3] = np.where(op, 255, 0)
        self.gloei = g

    def steen(self, basis=(150, 150, 146), mos=(104, 132, 84)):
        """A statue: the whole sheet in weathered stone, the eyes too, moss in the dark folds and low down."""
        op = self.a[..., 3] > 0
        licht = self.a[..., :3].astype(np.float32).mean(-1) / 255
        licht = 0.5 + 0.8 * licht
        korrel = self.rng.normal(0, 0.035, licht.shape)
        rgb = np.array(basis, np.float32) * np.clip(licht + korrel, 0.5, 1.32)[..., None]
        vlek = (self.rng.random(licht.shape) < 0.012 + 0.07 * (licht < 0.86)) & op
        groot = np.zeros_like(vlek)
        for dy in (0, 1):
            for dx in (0, 1):
                groot[dy:, dx:] |= vlek[:vlek.shape[0] - dy, :vlek.shape[1] - dx]
        rgb[groot & op] = np.array(mos, np.float32) * (0.85 + 0.3 * self.rng.random(int((groot & op).sum())))[:, None]
        self.a[..., :3] = np.where(op[..., None], np.clip(rgb, 0, 255), self.a[..., :3]).astype(np.uint8)

    def gloed(self, rgb_masker):
        """A glowmask of chosen pixels: rgb_masker(a) -> bool array over the sheet."""
        m = rgb_masker(self.a) & (self.a[..., 3] > 0)
        g = np.zeros_like(self.a)
        g[m, :3] = self.a[m, :3]
        g[..., 3] = np.where(m, 255, 0)
        self.gloei = g

    def klaar(self):
        h = self.h
        kn._save_geo(h, f"guh_npc_{self.kind}.geo.json", self.geo_file)
        h.save(Image.fromarray(self.a), "entity", f"npc_{self.kind}.png")
        if self.gloei is not None:
            h.save(Image.fromarray(self.gloei), "entity", f"npc_{self.kind}_glowmask.png")
        return self


# =============================================================================================================================
# swatch patterns (a 32 x 32 px block, changed in place)
# =============================================================================================================================
def _donker(c, f=0.75):
    return tuple(int(v * f) for v in c)


def rand(kleur, dikte=3):
    def patroon_rand(b):
        b[:dikte, :, :3] = b[-dikte:, :, :3] = b[:, :dikte, :3] = b[:, -dikte:, :3] = kleur
    return patroon_rand


def zoom(kleur, dikte=4, boven=False):
    def patroon_zoom(b):
        if boven:
            b[:dikte, :, :3] = kleur
        else:
            b[-dikte:, :, :3] = kleur
    return patroon_zoom


def strepen(kleur, stap=8, dikte=3, liggend=True):
    def patroon_strepen(b):
        for i in range(0, 32, stap):
            if liggend:
                b[i:i + dikte, :, :3] = kleur
            else:
                b[:, i:i + dikte, :3] = kleur
    return patroon_strepen


def ruit(kleur, stap=8):
    def patroon_ruit(b):
        for y in range(32):
            for x in range(32):
                if (x + y) % stap == 0 or (x - y) % stap == 0:
                    b[y, x, :3] = kleur
    return patroon_ruit


def net(stap=6):
    """A net: only the threads stay, the rest is see-through."""
    def patroon_net(b):
        m = np.ones((32, 32), bool)
        for y in range(32):
            for x in range(32):
                if (x + y) % stap == 0 or (x - y) % stap == 0:
                    m[y, x] = False
        b[m, 3] = 0
    return patroon_net


def stip(kleur, r=9.0, cx=15.5, cy=15.5, ring=None):
    def patroon_stip(b):
        yy, xx = np.mgrid[0:32, 0:32]
        d = np.hypot(xx - cx, yy - cy)
        b[d <= r, :3] = kleur
        if ring is not None:
            b[(d > r) & (d <= r + 3), :3] = ring
    return patroon_stip


def wiel(hout, donker):
    """A cart wheel seen from the side: a round rim, spokes, a hub; the corners see-through."""
    def patroon_wiel(b):
        yy, xx = np.mgrid[0:32, 0:32]
        d = np.hypot(xx - 15.5, yy - 15.5)
        b[..., :3] = hout
        b[d > 15.8, 3] = 0
        b[(d > 11.5) & (d <= 15.8), :3] = donker
        b[(np.abs(xx - 15.5) < 1.6) | (np.abs(yy - 15.5) < 1.6), :3] = donker
        b[np.abs((xx - 15.5) - (yy - 15.5)) < 1.8, :3] = donker
        b[np.abs((xx - 15.5) + (yy - 15.5)) < 1.8, :3] = donker
        b[d < 4, :3] = hout
        b[d > 15.8, :3] = donker
    return patroon_wiel


def blad(licht, nerf):
    """A cabbage: pale ribs from the heart over darker leaves."""
    def patroon_blad(b):
        yy, xx = np.mgrid[0:32, 0:32]
        d = np.hypot(xx - 15.5, yy - 15.5)
        b[(d > 6) & (d < 8.5), :3] = nerf
        b[(d > 12) & (d < 14), :3] = nerf
        b[d <= 5, :3] = licht
        for a in range(0, 360, 60):
            m = np.abs((xx - 15.5) * math.sin(math.radians(a)) - (yy - 15.5) * math.cos(math.radians(a))) < 1.0
            b[m & (d > 5), :3] = licht
    return patroon_blad


def vlam(kleur, licht):
    """A flame emblem, standing."""
    def patroon_vlam(b):
        yy, xx = np.mgrid[0:32, 0:32]
        breed = np.clip((yy - 3) / 29.0, 0, 1) * 11 * (0.7 + 0.3 * np.sin(yy / 3.0))
        m = np.abs(xx - 15.5 - 2 * np.sin(yy / 4.0)) < breed
        b[m, :3] = kleur
        m2 = (np.abs(xx - 15.5) < breed * 0.45) & (yy > 14)
        b[m2, :3] = licht
    return patroon_vlam


def glas(rand_kleur, lens=(196, 226, 240)):
    def patroon_glas(b):
        yy, xx = np.mgrid[0:32, 0:32]
        d = np.hypot(xx - 15.5, yy - 15.5)
        b[..., :3] = rand_kleur
        b[d < 11, :3] = lens
        b[(np.abs(xx - yy + 6) < 2) & (d < 10), :3] = (246, 252, 255)
    return patroon_glas


def gebreid(kleur):
    def patroon_gebreid(b):
        for y in range(0, 32, 4):
            b[y, :, :3] = _donker(kleur, 0.8)
        for x in range(0, 32, 4):
            b[(np.arange(32) // 4 % 2) == (x // 4 % 2), x, :3] = _donker(kleur, 0.88)
    return patroon_gebreid


def karton(kleur):
    """Cardboard: ribbed, with a strip of tape."""
    def patroon_karton(b):
        for x in range(0, 32, 3):
            b[:, x, :3] = _donker(kleur, 0.9)
        b[13:18, :, :3] = (226, 214, 178)
    return patroon_karton


def mikamasker(scar_rechts=True):
    """The Mika's face as a stage mask (a board): pink, angry red eyes with dark brows, a grin; the burn scar on one side."""
    def patroon_mikamasker(b):
        b[..., :3] = (240, 178, 196)
        yy, xx = np.mgrid[0:32, 0:32]
        for cx in (9, 22):
            d = np.hypot(xx - cx, (yy - 14) * 1.15)
            b[d < 4.6, :3] = (60, 14, 22)
            b[d < 3.6, :3] = (226, 40, 40)
            b[(np.abs(xx - cx) < 0.9) & (np.abs(yy - 14) < 3.2), :3] = (30, 8, 12)
        for x in range(4, 14):                                         # the angry brows, down towards the nose
            b[7 + (x - 4) // 3:9 + (x - 4) // 3, x, :3] = (70, 30, 50)
            b[7 + (x - 4) // 3:9 + (x - 4) // 3, 31 - x, :3] = (70, 30, 50)
        b[19:21, 14:18, :3] = (232, 120, 150)
        for x in range(10, 22):
            b[24 + (0 if 12 < x < 19 else -1), x, :3] = (96, 30, 60)
        cx = 22 if scar_rechts else 9                                  # the scar: a dark red patch round one eye (drawn on, too neat)
        d = np.hypot((xx - cx) * 0.8, yy - 13)
        ring = (d < 8) & (np.hypot(xx - cx, (yy - 14) * 1.15) >= 4.6)
        b[ring, :3] = (170, 60, 60)
    return patroon_mikamasker


# =============================================================================================================================
# animations
# =============================================================================================================================
def _t(t):
    return repr(round(float(t), 3))


def spoor(bot, kanaal, sleutels):
    """One track: (bone, 'rotation' | 'position' | 'scale', {seconds: [x, y, z]})."""
    return bot, kanaal, {float(t): [float(x) for x in v] for t, v in sleutels.items()}


def op(bot, t0, t1, waarde, kanaal="rotation", rust=(0, 0, 0), aan=0.25, uit=None):
    """Go to a value, hold it, come back."""
    uit = aan if uit is None else uit
    return spoor(bot, kanaal, {t0: rust, t0 + aan: waarde, t1 - uit: waarde, t1: rust})


def schud(bot, t0, t1, amp, n=3, kanaal="rotation", rust=(0, 0, 0)):
    """Back and forth n times around the rest value (amp per axis)."""
    s = {t0: rust}
    stap = (t1 - t0) / (2 * n + 1)
    for i in range(2 * n):
        teken = 1 if i % 2 == 0 else -1
        s[t0 + (i + 1) * stap] = [rust[j] + teken * amp[j] for j in range(3)]
    s[t1] = rust
    return spoor(bot, kanaal, s)


def reeks(bot, kanaal, *punten):
    """(t, value) pairs."""
    return spoor(bot, kanaal, dict(punten))


def toon(bot, t0, t1, aan=0.12):
    """A prop that only exists during a move: scale 0 -> 1 -> 0."""
    return spoor(bot, "scale", {0.0: (0, 0, 0), max(0.0, t0 - 0.001) if t0 > 0 else 0.0: (0, 0, 0), t0 + aan: (1, 1, 1), t1 - aan: (1, 1, 1), t1: (0, 0, 0)})


def verschuif(sporen, dt):
    return [(b, k, {t + dt: v for t, v in s.items()}) for b, k, s in sporen]


def _animatie(lengte, lus, sporen, verborgen):
    bones = {}
    for bot, kanaal, sleutels in sporen:
        kan = bones.setdefault(bot, {}).setdefault(kanaal, {})
        for t, v in sorted(sleutels.items()):
            kan[_t(min(max(t, 0.0), lengte))] = v
    for bot in verborgen:
        if "scale" not in bones.get(bot, {}):
            bones.setdefault(bot, {})["scale"] = {"0.0": [0.0, 0.0, 0.0]}
    for bot in bones:
        for kanaal in bones[bot]:
            bones[bot][kanaal] = dict(sorted(bones[bot][kanaal].items(), key=lambda kv: float(kv[0])))
    return {"loop": lus, "animation_length": float(lengte), "bones": bones}


_BASIS_IDLE = {}


def basis_idle(h, lijf):
    """The stock idle of the body (4 s): guh_sitting's for `zit`, Steele-Mika's (the Mika's bones) for `mika`."""
    if lijf not in _BASIS_IDLE:
        naam = "guh_sitting" if lijf == "zit" else "guh_npc_steele_mika"
        d = json.load(open(os.path.join(h.A, "geckolib", "animations", "entity", f"{naam}.animation.json"), encoding="utf-8"))
        a = d["animations"]["animation.guh_sitting.idle"]
        _BASIS_IDLE[lijf] = (float(a["animation_length"]),
                             [(bot, kanaal, {float(t): list(v) for t, v in s.items()}) for bot, kan in a["bones"].items() for kanaal, s in kan.items()])
    return copy.deepcopy(_BASIS_IDLE[lijf])


def idle(h, lijf, handtekening, lengte=8.0, start=4.4, zonder=()):
    """The idle of a character: the stock idle repeated up to `lengte`, and the signature move (tracks with times from 0)
    woven in once, from `start`: a bone the move uses follows the stock idle until then, and comes back to it at the end."""
    bl, basis = basis_idle(h, lijf)
    sig = verschuif(handtekening, start)
    eigen = {(b, k) for b, k, _ in sig}
    uit = []
    for bot, kanaal, sleutels in basis:
        if bot in zonder:
            continue
        s = {}
        n = int(round(lengte / bl))
        for i in range(n):
            for t, v in sleutels.items():
                s[t + i * bl] = v
        if (bot, kanaal) in eigen:
            s = {t: v for t, v in s.items() if t <= start - 0.39 or t >= lengte}
        uit.append((bot, kanaal, s))
    for bot, kanaal, sleutels in sig:
        rust = next((s for b, k, s in uit if b == bot and k == kanaal), None)
        if rust is not None:
            rust.update(sleutels)
        else:
            uit.append((bot, kanaal, dict(sleutels)))
    return uit


def bestand(h, basis, lijf, idle_sporen, animaties, verborgen=(), idle_lengte=8.0):
    """Writes guh_npc_<basis>.animation.json. animaties: {naam: (ticks, sporen)}; ticks 0 = a pose that lasts (houding)."""
    anims = {"animation.guh_sitting.idle": _animatie(idle_lengte, True, idle_sporen, verborgen)}
    for naam, (ticks, sporen) in animaties.items():
        if ticks:
            anims[f"animation.guh_sitting.{naam}"] = _animatie(ticks / 20.0, False, sporen, verborgen)
        else:
            lengte = max([max(s) for _, _, s in sporen] + [0.5])
            anims[f"animation.guh_sitting.{naam}"] = _animatie(lengte, "hold_on_last_frame", sporen, verborgen)
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", f"guh_npc_{basis}.animation.json"),
        {"format_version": "1.8.0", "animations": anims})
    return anims


# =============================================================================================================================
# a quick renderer for the previews (the quads of wiki_renders.geo_quads; z-buffered with numpy instead of a pixel loop)
# =============================================================================================================================
def teken(quads, yaw=28, pitch=-14, size=300, margin=0.06, ss=2, texturen=None, achtergrond=(236, 232, 240)):
    import wiki_renders as wr
    R = wr.rot_matrix(yaw, pitch)
    hoeken = np.array([R @ (q.origin + a * q.u + b * q.v) for q in quads for a in (0, 1) for b in (0, 1)])
    lo, hi = hoeken[:, :2].min(0), hoeken[:, :2].max(0)
    span = max(hi - lo) or 1
    W = size * ss
    schaal = W * (1 - 2 * margin) / span
    midden = (lo + hi) / 2
    zbuf = np.full(W * W, np.inf)
    col = np.zeros((W * W, 4), np.float32)
    col[:, :3] = achtergrond
    col[:, 3] = 255
    licht = np.array([-0.35, 0.85, -0.4])
    licht /= np.linalg.norm(licht)
    for q in quads:
        n = R @ q.normal
        if n[2] > 1e-6:
            continue
        schaduw = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, licht)))
        tex = (texturen or {}).get(q.tex) if isinstance(q.tex, str) else q.tex
        if tex is None:
            tex = wr.tex_array(q.tex)
        th, tw = tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        nu = max(2, int(np.linalg.norm(R @ q.u) * schaal * 1.6) + 1)
        nv = max(2, int(np.linalg.norm(R @ q.v) * schaal * 1.6) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        S = (q.origin + A[..., None] * q.u + B[..., None] * q.v) @ R.T
        px = ((S[..., 0] - midden[0]) * schaal + W / 2).astype(int).ravel()
        py = (-(S[..., 1] - midden[1]) * schaal + W / 2).astype(int).ravel()
        z = S[..., 2].ravel()
        tu = np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int).ravel()
        tv = np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int).ravel()
        rgba = tex[tv, tu]
        m = (px >= 0) & (px < W) & (py >= 0) & (py < W) & (rgba[:, 3] > 20)
        idx, z, rgba = (py * W + px)[m], z[m], rgba[m]
        volg = np.argsort(-z)
        idx, z, rgba = idx[volg], z[volg], rgba[volg]
        dichter = z < zbuf[idx]
        idx, z, rgba = idx[dichter], z[dichter], rgba[dichter]
        zbuf[idx] = z
        alfa = (rgba[:, 3:4] / 255.0)
        col[idx, :3] = rgba[:, :3] * schaduw * alfa + np.array(achtergrond, np.float32) * (1 - alfa)
    img = Image.fromarray(np.clip(col.reshape(W, W, 4), 0, 255).astype(np.uint8), "RGBA")
    return img.resize((size, size), Image.LANCZOS)
