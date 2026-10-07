"""
einde2, slice cast_schurk - the drawing kit of its characters (CONTRACT_E2 11.2): one `Blad` per GuhNpcEntity.Kind.

A Blad is a copy of a base model (the ordinary Mika: `mika.geo.json` + `mika.png`, read-only) with things added: extra bones
whose cube faces are painted in the free part of the Kind's OWN 512 px sheet. It is a boerderij_dieren "atlas" as well
(`patch(w, h, painter)`), so `kubus` paints every face of a cube with a painter of its own (a robe with a golden hem, a
skull mask, a flame) instead of one flat swatch; patches of the same painter and size are shared between cubes, so a
sheet does not fill up. `gloed=True` copies the patch into the glow mask (npc_<kind>_glowmask.png).

Also here: the Mika's face repainted per character (`snuit`: the red slit eyes, brows and grin of mika.png drawn anew with
another mood), the painters (cloth, armour, hair, flame, stone, metal) and the tall Mika body (`lang_lijf`).
"""
import copy
import json
import math
import os

import numpy as np
from PIL import Image

from features import boerderij_dieren as bd
from features import einde2_kit as kit

UV = 128                  # the sheet in model units
PX = 4                    # texels per unit
FACES = bd.FACES
MIKA_BOTTEN = ("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")
LANG_BOTTEN = ("root", "body", "head", "ear_left", "ear_right", "arm_left", "arm_right", "leg_left", "leg_right", "tail")

# --- colours --------------------------------------------------------------------------------------------------------------
MIKA = (228, 170, 186)
MIKA_DONKER = (196, 136, 156)
ZWART = (30, 26, 34)
HAAR = (34, 28, 38)
HAAR_LICHT = (70, 60, 80)
BRUIN = (104, 66, 44)
BRUIN_LICHT = (150, 104, 70)
GRIJS = (150, 150, 160)
GRIJS_LICHT = (206, 206, 214)
WIT = (248, 246, 244)
GOUD = (246, 198, 64)
GOUD_DONKER = (190, 136, 36)
ROOD = (176, 34, 38)
ROOD_DONKER = (112, 22, 30)
ROOD_LICHT = (222, 70, 60)
BLAUW_VUUR = (70, 170, 255)
ORANJE = (255, 150, 40)
GEEL = (255, 226, 96)
GROEN = (38, 78, 56)
GROEN_LICHT = (64, 112, 78)
STEEN = (136, 134, 128)
STEEN_DONKER = (96, 94, 92)
KAAS = (250, 214, 96)
KAAS_GAT = (206, 150, 40)
ROZE = (250, 150, 190)
ROZE_DONKER = (222, 104, 156)
KOPER = (204, 150, 70)
MARINE = (40, 52, 96)


# =====================================================================================================================
# painters (W, H, rng) -> uint8 RGBA, as boerderij_dieren's
# =====================================================================================================================
def effen(colour, var=6):
    return bd.plain(colour, var)


def stof(colour, zoom=None, boven=None, strepen=None, var=7, zoom_dik=0.14):
    """Cloth: the colour with a soft weave, a hem (`zoom`) along the bottom, a band (`boven`) along the top, upright
    stripes (`strepen` = (colour, every n px))."""
    def paint(W, H, rng):
        a = bd.plain(colour, var)(W, H, rng).astype(np.float32)
        a[::3, :, :3] *= 0.96
        if strepen:
            kleur, elke = strepen
            for x in range(elke // 2, W, elke):
                a[:, x:x + max(1, elke // 6), :3] = kleur
        d = max(1, int(round(H * zoom_dik)))
        if zoom:
            a[H - d:, :, :3] = np.array(zoom, np.float32) + rng.normal(0, 3, (d, W, 1))
            a[H - d - 1:H - d, :, :3] *= 0.7
        if boven:
            a[:d, :, :3] = np.array(boven, np.float32) + rng.normal(0, 3, (d, W, 1))
            a[d:d + 1, :, :3] *= 0.7
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def pantser(colour, rand=GOUD, donker=0.72, platen=True):
    """Armour: plates with a dark seam, a golden edge all round."""
    def paint(W, H, rng):
        a = bd.plain(colour, 6)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        a[..., :3] *= (1.08 - 0.22 * (yy / max(1, H)))[..., None]
        if platen and H >= 12:
            for y in range(H // 3, H, max(4, H // 3)):
                a[y:y + 1, :, :3] *= donker
        e = max(1, min(W, H) // 9)
        for sl in (np.s_[:e, :], np.s_[H - e:, :], np.s_[:, :e], np.s_[:, W - e:]):
            a[sl][..., :3] = np.array(rand, np.float32) + rng.normal(0, 4, a[sl].shape[:2] + (1,))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def haren(colour=HAAR, licht=HAAR_LICHT, dicht=14):
    """Hair: soft upright strands with a sheen."""
    def paint(W, H, rng):
        a = bd.plain(colour, 5)(W, H, rng).astype(np.float32)
        for _ in range(max(2, W * H // dicht)):
            x, y, lang = int(rng.integers(0, W)), int(rng.integers(0, H)), int(rng.integers(3, 9))
            a[y:y + lang, x:x + 1, :3] = np.array(licht, np.float32) if rng.random() < 0.4 else np.array(colour, np.float32) * 0.7
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def metaal(colour, licht):
    """Brass, gold or steel: a shine from the top left."""
    def paint(W, H, rng):
        a = bd.plain(colour, 5)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        t = np.clip(1 - (xx / max(1, W) + yy / max(1, H)), 0, 1)[..., None]
        a[..., :3] = a[..., :3] * (1 - 0.55 * t) + np.array(licht, np.float32) * 0.55 * t
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def steen(colour=STEEN, donker=STEEN_DONKER):
    """Rough stone with a few cracks (the Dai Li's gloves)."""
    def paint(W, H, rng):
        a = bd.plain(colour, 12)(W, H, rng).astype(np.float32)
        for _ in range(max(1, W * H // 90)):
            x, y = float(rng.integers(0, W)), float(rng.integers(0, H))
            hoek = rng.uniform(0, 2 * math.pi)
            for _stap in range(int(rng.integers(4, 10))):
                if 0 <= int(y) < H and 0 <= int(x) < W:
                    a[int(y), int(x), :3] = donker
                hoek += rng.uniform(-0.8, 0.8)
                x, y = x + math.cos(hoek), y + math.sin(hoek)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def vlam(kern=GEEL, rand=ORANJE, punt=ROOD_LICHT, tongen=3):
    """A flame, cut out of the patch (see-through around it): a bright core, a darker edge, licking tongues on top."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        u = (xx + 0.5) / W
        v = 1 - (yy + 0.5) / H                                    # 0 at the foot, 1 at the tip
        breed = 0.5 * np.sqrt(np.clip(1 - v, 0, 1)) * (0.62 + 0.38 * np.abs(np.cos(u * math.pi * tongen)) ** 0.7) + 0.02
        breed = np.where(v < 0.12, breed * (0.55 + v * 3.75), breed)
        d = np.abs(u - 0.5) / np.maximum(breed, 1e-3)
        binnen = d <= 1
        t = np.clip(d * 0.75 + v * 0.55, 0, 1)[..., None]
        kleur = np.where(t < 0.45, np.array(kern, np.float32) * (1 - t / 0.45) + np.array(rand, np.float32) * (t / 0.45),
                         np.array(rand, np.float32) * (1 - (t - 0.45) / 0.55) + np.array(punt, np.float32) * ((t - 0.45) / 0.55))
        a[..., :3] = kleur
        a[..., 3] = np.where(binnen, 255, 0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def bliksem(kleur=(190, 230, 255), kern=(255, 255, 255)):
    """A jagged bolt along the patch, see-through around it."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        x = W / 2
        for y in range(H):
            if y % 5 == 0:
                x = W / 2 + rng.uniform(-0.3, 0.3) * W
            x0 = int(np.clip(x + rng.uniform(-0.6, 0.6), 1, W - 2))
            a[y, max(0, x0 - 2):x0 + 3] = (*kleur, 255)
            a[y, x0] = (*kern, 255)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def met(painter, extra):
    """painter, then extra(W, H, rng, a) on top."""
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        extra(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rond(painter):
    """The patch cut round (see-through corners)."""
    def paint(W, H, rng):
        a = painter(W, H, rng).copy()
        yy, xx = np.mgrid[0:H, 0:W]
        a[((xx + 0.5 - W / 2) / (W / 2)) ** 2 + ((yy + 0.5 - H / 2) / (H / 2)) ** 2 > 1, 3] = 0
        return a
    return paint


def disc(a, cx, cy, r, colour, alpha=1.0, ry=None):
    """A filled ellipse, blended (numpy: no pixel loop)."""
    H, W = a.shape[:2]
    ry = ry or r
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - r - 1)), min(W, int(cx + r + 2))
    if y0 >= y1 or x0 >= x1:
        return
    yy, xx = np.mgrid[y0:y1, x0:x1]
    m = ((xx + 0.5 - cx) / r) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1
    blok = a[y0:y1, x0:x1]
    blok[m, :3] = blok[m, :3] * (1 - alpha) + np.array(colour, np.float32)[:3] * alpha
    if a.shape[2] > 3:
        blok[m, 3] = 255


def streep(a, x0, y0, x1, y1, colour, dik=1.0):
    """A straight line of `dik` px."""
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        disc(a, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, dik / 2 + 0.3, colour)


# =====================================================================================================================
# the sheet + the model of one Kind
# =====================================================================================================================
class Blad:
    def __init__(self, h, kind, basis="mika", tint=None):
        """kind: the Kind id (lower case). basis: "mika" (the model is a copy of mika.geo.json), or None (an empty model on
        the Mika's sheet: the tall body builds its own bones). tint = (hue, sat, val) recolours the pink fur."""
        self.h, self.kind = h, kind
        self.rng = kit.rng(f"cast_schurk/{kind}")
        pad = os.path.join(h.A, "geckolib", "models", "entity", "mika.geo.json")
        self.basis = json.load(open(pad, encoding="utf-8"))
        self.geo_file = copy.deepcopy(self.basis)
        self.geo = self.geo_file["minecraft:geometry"][0]
        self.geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
        self.geo["bones"] = [b for b in self.geo["bones"] if b["name"] in MIKA_BOTTEN]
        src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
        if tint:
            src = h.recolour(src, hue=tint[0], sat=tint[1], val=tint[2], only=h.pinkish).convert("RGBA")
        self.img = np.asarray(src).copy()
        self.gloed = np.zeros_like(self.img)
        self.used = np.zeros((UV, UV), bool)
        for b in self.geo["bones"]:
            for c in b.get("cubes", []):
                for f in c["uv"].values():
                    (u, v), (w, hh) = f["uv"], f["uv_size"]
                    self.used[max(0, int(min(v, v + hh))):int(math.ceil(max(v, v + hh))) + 1,
                              max(0, int(min(u, u + w))):int(math.ceil(max(u, u + w))) + 1] = True
        if basis is None:
            self.geo["bones"] = []
        self._gedeeld = {}
        self.vacht = self._vachtkleur()

    def _vachtkleur(self):
        """The fur colour of this sheet (the middle of the face cube's side)."""
        blok = self.img[52 * PX:58 * PX, 41 * PX:49 * PX, :3].reshape(-1, 3)
        return tuple(int(v) for v in np.median(blok, 0))

    # --- the atlas ----------------------------------------------------------------------------------------------------
    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        for v in range(0, UV - h + 1):
            vrij = ~self.used[v:v + h].any(0)
            if not vrij.any():
                continue
            run = 0
            for u in range(UV):
                run = run + 1 if vrij[u] else 0
                if run >= w:
                    u0 = u - w + 1
                    self.used[v:v + h, u0:u0 + w] = True
                    return u0, v
        raise SystemExit(f"einde2_cast_schurk: the sheet of {self.kind} is full (lower a cube's k or share a painter)")

    def patch(self, w, h, painter, k=1.0, gloed=False):
        """A face patch (boerderij_dieren's Atlas.patch), k = texels kept per model pixel (0.5 for big plain faces)."""
        sleutel = (id(painter), round(w, 3), round(h, 3), k, gloed)
        if sleutel in self._gedeeld:
            return dict(self._gedeeld[sleutel][0])
        tw, th = max(w * k, 0.5), max(h * k, 0.5)
        u, v = self.alloc(tw, th)
        W, H = int(math.ceil(tw)) * PX, int(math.ceil(th)) * PX
        arr = painter(W, H, self.rng)
        self.img[v * PX:v * PX + H, u * PX:u * PX + W] = arr
        if gloed:
            self.gloed[v * PX:v * PX + H, u * PX:u * PX + W] = arr
        face = {"uv": [u, v], "uv_size": [int(math.ceil(tw)), int(math.ceil(th))]}
        self._gedeeld[sleutel] = (face, painter)                    # (the painter is kept alive: its id is the key)
        return dict(face)

    def kubus(self, origin, size, paint, overrides=None, inflate=None, k=1.0, gloed=False, alleen=None):
        """A cube; every face painted by `paint` (or overrides[face]); alleen = the faces it has (a flat flame: two)."""
        uv = {}
        for f in (alleen or FACES):
            w, h = bd.face_size(f, size)
            uv[f] = self.patch(max(w, 0.5), max(h, 0.5), (overrides or {}).get(f, paint), k, gloed)
        c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        return c

    def paar(self, origin, size, paint, **kw):
        """The cube and its mirror image across x."""
        o = list(origin)
        return [self.kubus(o, size, paint, **kw), self.kubus([-(o[0] + size[0]), o[1], o[2]], size, paint, **kw)]

    # --- bones --------------------------------------------------------------------------------------------------------
    def bot(self, naam, parent, pivot, cubes, rotation=None):
        b = {"name": naam, "parent": parent, "pivot": [round(v, 3) for v in pivot], "cubes": list(cubes)}
        if rotation:
            b["rotation"] = list(rotation)
        self.geo["bones"].append(b)
        return b

    def basisbot(self, naam):
        return next(b for b in self.geo["bones"] if b["name"] == naam)

    def namen(self):
        return [b["name"] for b in self.geo["bones"]]

    def punt(self, v):
        return list(v)

    # --- the face of the ordinary Mika (the north face of the big head cube: 14 x 11 units, 56 x 44 px) -----------------
    def gezicht(self):
        return self.img[49 * PX:60 * PX, 11 * PX:25 * PX]

    def gezicht_gloed(self):
        return self.gloed[49 * PX:60 * PX, 11 * PX:25 * PX]

    def schrijf(self, gloed=False):
        h = self.h
        h.w(os.path.join(h.A, "geckolib", "models", "entity", f"guh_npc_{self.kind}.geo.json"), self.geo_file)
        h.save(Image.fromarray(self.img), "entity", f"npc_{self.kind}.png")
        if gloed:
            h.save(Image.fromarray(self.gloed), "entity", f"npc_{self.kind}_glowmask.png")


# =====================================================================================================================
# the Mika's face, drawn anew
# =====================================================================================================================
OOG_ROOD = (232, 44, 40)
OOG_LICHT = (255, 96, 72)
LIJN = (48, 14, 34)                # brows, lashes, the mouth: the dark plum of mika.png


def snuit(blad, ogen="spleet", brauw="boos", mond="grijns", lid=0.0, onderlid=0.0, oog=OOG_ROOD, oog_licht=OOG_LICHT, groot=1.0,
          wang=None, gloei=False, extra=None):
    """Repaints the Mika's face. It stays the Mika's: two big red eyes with a slit pupil, dark brows, a grin with a fang.
    ogen: spleet (the slit pupil) | rond (a big round pupil with two glints: cheerful) | dicht (shut, a curved line);
    brauw: boos | streng (thin, steep) | droevig | dik (bushy, `brauw_kleur` grey) | blij | geen | (name, colour);
    mond: grijns | dun (a thin one-sided smirk) | zuur | blij (open, a tongue) | recht | o;
    lid / onderlid: how much of the eye the upper / lower lid covers (0..1); wang: a blush colour; gloei: the eyes go into
    the glow mask; extra(a): more paint on the 56 x 44 face (crumbs, a scar)."""
    a = blad.gezicht()
    H, W = a.shape[:2]
    f = a.astype(np.float32)
    vacht = np.array(blad.vacht, np.float32)
    rng = blad.rng
    f[..., :3] = vacht + rng.normal(0, 2.4, (H, W, 1)) + rng.normal(0, 1.6, ((H + 1) // 2, (W + 1) // 2, 1)).repeat(2, 0).repeat(2, 1)[:H, :W]
    f[..., 3] = 255
    kleur_brauw = LIJN
    if isinstance(brauw, tuple):
        brauw, kleur_brauw = brauw
    r = 8.4 * groot
    yy, xx = np.mgrid[0:H, 0:W]
    gl = blad.gezicht_gloed()
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * 13.4, 21.0 + (1.0 if groot > 1.05 else 0.0)
        dx, dy = (xx + 0.5 - cx) / r, (yy + 0.5 - cy) / (r * 0.94)
        d = np.hypot(dx, dy)
        binnen = d <= 1
        if ogen == "dicht":
            boog = binnen & (np.abs(d - 0.8) < 0.13) & (dy > 0.1)
            f[boog, :3] = LIJN
        else:
            t = np.clip((dy + 1) / 2, 0, 1)[..., None]
            iris = np.array(oog_licht, np.float32) * (1 - t) + np.array(oog, np.float32) * t
            f[..., :3] = np.where(binnen[..., None], iris, f[..., :3])
            if ogen == "rond":
                f[binnen & (np.hypot(dx, dy - 0.05) < 0.5), :3] = (26, 8, 14)
            else:
                f[binnen & (np.abs(dx) < 0.2 * (1 - np.abs(dy) ** 1.6) + 0.045), :3] = (26, 8, 14)
            f[binnen & (d > 0.83), :3] = (58, 12, 22)
            if gloei:
                m = binnen & (d <= 0.83)
                gl[m] = np.concatenate([f[m, :3], np.full((int(m.sum()), 1), 255, np.float32)], 1).astype(np.uint8)
            disc(f, cx + 0.34 * r, cy - 0.42 * r, 0.2 * r, (255, 232, 226))
            if ogen == "rond":
                disc(f, cx - 0.36 * r, cy + 0.34 * r, 0.11 * r, (255, 214, 214))
            if lid > 0:                                               # a heavy upper lid, a lash line under it
                rand = -1 + 2 * lid
                m = binnen & (dy < rand)
                f[m, :3] = vacht * 0.9
                f[binnen & (np.abs(dy - rand) < 0.11), :3] = LIJN
            if onderlid > 0:
                rand = 1 - 2 * onderlid
                f[binnen & (dy > rand), :3] = vacht * 0.95
                f[binnen & (np.abs(dy - rand) < 0.08), :3] = np.array(LIJN, np.float32) * 1.6
        # the brow: a bar over the eye, slanted
        if brauw != "geen":
            helling, dik, hoog, lengte = {"boos": (0.46, 2.3, 1.02, 1.3), "streng": (0.62, 1.5, 1.0, 1.35), "droevig": (-0.42, 2.0, 1.25, 1.2),
                                          "dik": (0.2, 3.2, 1.12, 1.35), "blij": (-0.12, 1.4, 1.5, 0.95), "recht": (0.0, 2.0, 1.2, 1.25)}[brauw]
            for x in range(int(cx - lengte * r), int(cx + lengte * r) + 1):
                t = (x - cx) / (lengte * r) * sx                       # -1 at the nose side ... +1 outside
                y0 = cy - hoog * r + t * -helling * r
                d_ = dik * (1.0 if brauw != "streng" else 0.7 + 0.5 * (1 - abs(t)))
                for y in range(int(round(y0 - d_)), int(round(y0 + d_ * 0.6)) + 1):
                    if 0 <= y < H and 0 <= x < W:
                        f[y, x, :3] = kleur_brauw
            if brauw == "dik":                                        # bushy: a ragged edge
                for _ in range(26):
                    x, y = int(cx + rng.uniform(-1.3, 1.3) * r), int(cy - 1.12 * r + rng.uniform(-4, 3))
                    if 0 <= y < H and 0 <= x < W:
                        f[y, x, :3] = kleur_brauw
        if wang:
            disc(f, cx + sx * 1.5, cy + 1.32 * r, 4.2, wang, 0.55, ry=2.2)
    # the mouth (the nose is a cube of its own)
    my, breed = 37.0, 11.0
    for x in range(int(W / 2 - breed), int(W / 2 + breed) + 1):
        t = (x - W / 2) / breed
        if mond in ("grijns", "blij"):
            dy_ = -(t * t) * 4.2 + 2.2
        elif mond == "dun":
            dy_ = -max(0.0, t) ** 2 * 5.0 + 1.0 if abs(t) < 0.8 else None
        elif mond == "zuur":
            dy_ = (t * t) * 3.4 - 0.6
        elif mond == "recht":
            dy_ = 0.6 if abs(t) < 0.6 else None
        else:
            dy_ = None
        if dy_ is None:
            continue
        for k in range(2):
            y = int(round(my + dy_)) + k
            if 0 <= y < H:
                f[y, x, :3] = LIJN
    if mond == "blij":                                                # an open laugh with a tongue
        disc(f, W / 2, my + 2.6, 5.0, LIJN, ry=3.0)
        disc(f, W / 2, my + 3.8, 3.0, (240, 110, 130), ry=1.6)
    if mond == "o":
        disc(f, W / 2, my + 1.5, 2.6, LIJN, ry=2.4)
    if mond in ("grijns", "dun", "zuur"):                            # the little fang
        fx = int(W / 2 + (5 if mond != "dun" else 6))
        ty = int(my + (0.6 if mond != "zuur" else 1.4))
        f[ty + 1:ty + 4, fx:fx + 3, :3] = (255, 255, 255)
        f[ty + 4:ty + 5, fx + 1:fx + 2, :3] = (255, 255, 255)
    if extra:
        extra(f)
    a[...] = np.clip(f, 0, 255).astype(np.uint8)


# =====================================================================================================================
# hair, a topknot (shared by half the Fire Nation)
# =====================================================================================================================
def haardos(b, naam, kleur=HAAR, licht=HAAR_LICHT, zij=True, achter=True, pony=0.0, parent="head"):
    """A cap of hair on the Mika's head (its ears stick out of it): the top, the back, the temples; pony = a fringe that
    hangs this many units over the forehead. Returns the bone."""
    p = haren(kleur, licht)
    cubes = [b.kubus([-6.9, 12.3, -11.5], [13.8, 2.1, 11.7], p, k=0.75),
             b.kubus([-5.8, 13.6, -10.4], [11.6, 1.1, 9.4], p, k=0.75)]
    if achter:
        cubes.append(b.kubus([-7.4, 3.0, -1.4], [14.8, 11.2, 1.7], p, k=0.75))
    if zij:
        cubes += b.paar([6.9, 7.0, -9.6], [1.3, 6.4, 8.6], p, k=0.75)
    if pony:
        cubes.append(b.kubus([-6.6, 12.5 - pony, -12.5], [13.2, pony + 0.4, 1.0], p))
    return b.bot(naam, parent, [0, 13, -6], cubes)


def knot(b, naam, kleur=HAAR, band=ROOD, y=14.5, z=-7.4, maat=3.2, parent="head"):
    """The topknot: a bun on the crown with a ribbon round its foot."""
    p = haren(kleur)
    return b.bot(naam, parent, [0, y, z + maat / 2], [
        b.kubus([-maat / 2, y, z], [maat, maat * 0.85, maat], p),
        b.kubus([-maat / 2 - 0.25, y, z - 0.25], [maat + 0.5, 0.9, maat + 0.5], effen(band, 5))])


def vlamkroon(b, naam, y=17.0, z=-6.6, schaal=1.0, parent="head", rotation=None, pivot=None):
    """The golden flame piece of the Fire Nation in a topknot: a tall middle tongue between two lower ones."""
    g = metaal(GOUD, (255, 244, 170))
    s = schaal
    cubes = [b.kubus([-0.6 * s, y, z], [1.2 * s, 3.0 * s, 0.8 * s], g),
             b.kubus([-1.7 * s, y, z + 0.1], [1.0 * s, 2.0 * s, 0.6 * s], g), b.kubus([0.7 * s, y, z + 0.1], [1.0 * s, 2.0 * s, 0.6 * s], g),
             b.kubus([-2.0 * s, y - 0.3 * s, z - 0.1], [4.0 * s, 0.8 * s, 1.0 * s], g)]
    return b.bot(naam, parent, pivot or [0, y, z], cubes, rotation)


# =====================================================================================================================
# the tall Mika body ("lang"): the tall Mika shape of CONTRACT_E2 3.3 (mikas), all numbers x 0.65 = 30 px high
# =====================================================================================================================
F = 0.65


def _verplaatst(bone, naam, parent, schaal, shift):
    b = copy.deepcopy(bone)
    b["name"], b["parent"] = naam, parent

    def p(v):
        return [round(v[i] * schaal + shift[i], 3) for i in range(3)]
    b["pivot"] = p(b.get("pivot", [0, 0, 0]))
    for c in b.get("cubes", []):
        c["origin"] = p(c["origin"])
        c["size"] = [round(s * schaal, 3) for s in c["size"]]
    return b


def vel(colour, poot=0.0, buik=False):
    """The tall Mika's skin: its pink with a soft sheen down the middle, a paler belly, and the last `poot` part of a limb a
    darker paw with two toe lines."""
    def paint(W, H, rng):
        a = bd.plain(colour, 6)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        a[..., :3] *= (1.05 - 0.16 * np.abs((xx + 0.5) / W - 0.5) * 2)[..., None]
        if buik:
            e = ((xx + 0.5 - W / 2) / (W * 0.34)) ** 2 + ((yy + 0.5 - H * 0.56) / (H * 0.36)) ** 2
            t = np.clip(1 - e, 0, 1)[..., None] * 0.6
            a[..., :3] = a[..., :3] * (1 - t) + np.array((250, 214, 224), np.float32) * t
        if poot > 0:
            start = int(H * (1 - poot))
            a[start:, :, :3] = np.array(MIKA_DONKER, np.float32) + rng.normal(0, 3, (H - start, W, 1))
            for x in (W // 3, (2 * W) // 3):
                a[H - max(2, H // 12):H, x, :3] = np.array(MIKA_DONKER, np.float32) * 0.7
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def lang_lijf(b):
    """Builds the ten bones of the tall body into an empty Blad; returns the place of its head: (shift, scale) of the
    ordinary Mika's head, so a character can hang things on it in the Mika head's own coordinates (`op_kop`)."""
    basis = {x["name"]: x for x in b.basis["minecraft:geometry"][0]["bones"]}
    s = 0.64 * F                                                    # the Mika's head, 16 wide -> 6.66
    sh = (0.0, 34 * F, 7 * 0.64 * F)
    huid, buik = vel(b.vacht), vel(b.vacht, buik=True)
    b.geo["bones"].append({"name": "root", "pivot": [0, 0, 0], "cubes": []})
    b.bot("body", "root", [0, 22 * F, 0], [b.kubus([-3 * F, 22 * F, -1.5 * F], [6 * F, 12 * F, 3 * F], huid, overrides={"north": buik}, k=2.0)])
    b.geo["bones"].append(_verplaatst(basis["head"], "head", "body", s, sh))
    for e in ("ear_left", "ear_right"):
        b.geo["bones"].append(_verplaatst(basis[e], e, "head", s, sh))
    staart = _verplaatst(basis["tail"], "tail", "body", 0.7 * F, (0, 23.2 * F, -6.2 * F))
    staart["rotation"] = [-35, 0, 0]
    b.geo["bones"].append(staart)
    arm, been = vel(b.vacht, poot=0.14), vel(b.vacht, poot=0.16)
    for side, sx in (("left", 1), ("right", -1)):
        ax = 3 * F if sx > 0 else -5 * F
        b.bot(f"arm_{side}", "body", [sx * 4 * F, 33.5 * F, 0], [b.kubus([ax, 14 * F, -1 * F], [2 * F, 20 * F, 2 * F], arm, k=2.0)])
        lx = 0.5 * F if sx > 0 else -2.5 * F
        b.bot(f"leg_{side}", "root", [sx * 1.5 * F, 22 * F, 0], [b.kubus([lx, 0, -1 * F], [2 * F, 22 * F, 2 * F], been, k=2.0)])
    b.geo["description"].update({"visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]})
    return sh, s


class OpKop:
    """Writes cubes in the ordinary Mika head's coordinates onto the (smaller, higher) head of the tall body."""

    def __init__(self, blad, shift, schaal):
        self.b, self.sh, self.s = blad, shift, schaal

    def punt(self, v):
        return [v[i] * self.s + self.sh[i] for i in range(3)]

    def kubus(self, origin, size, paint, **kw):
        kw.setdefault("k", 1.0 / self.s * 0.75)
        return self.b.kubus(self.punt(origin), [x * self.s for x in size], paint, **kw)

    def paar(self, origin, size, paint, **kw):
        return [self.kubus(origin, size, paint, **kw), self.kubus([-(origin[0] + size[0]), origin[1], origin[2]], size, paint, **kw)]

    def bot(self, naam, parent, pivot, cubes, rotation=None):
        return self.b.bot(naam, parent, self.punt(pivot), cubes, rotation)


# =====================================================================================================================
# animations (seconds; the game plays 20 ticks a second)
# =====================================================================================================================
WEG = [0.001, 0.001, 0.001]


def kf(pairs):
    return {str(round(t, 3)): [round(x, 3) for x in v] for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": round(length, 3), "bones": bones}


def houding(bones, length=2.0):
    """A pose that lasts (a loop): constant values, or keyframes for a little life in it."""
    return anim(length, bones, True)


def golf(as_, amp, length, n=4, fase=0.0, om=(0, 0, 0)):
    """A soft back-and-forth on one axis (0 = x, 1 = y, 2 = z) around `om`, as keyframes over length."""
    out = []
    for i in range(n + 1):
        v = list(om)
        v[as_] += amp * math.sin(2 * math.pi * (i / n) + fase)
        out.append((length * i / n, v))
    return kf(out)


def oren(L, hoek=14, om=0.0):
    """The two ear twitches of an idle of length L (om: the ears' resting tilt, for drooping ears)."""
    return {"ear_left": {"rotation": kf([(0, [0, 0, om]), (L * 0.62, [0, 0, om]), (L * 0.64, [0, 0, om - hoek]), (L * 0.67, [0, 0, om])])},
            "ear_right": {"rotation": kf([(0, [0, 0, -om]), (L * 0.3, [0, 0, -om]), (L * 0.32, [0, 0, hoek - om]), (L * 0.35, [0, 0, -om])])}}
