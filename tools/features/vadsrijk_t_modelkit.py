"""
T-MODELS (night round T, decision R5 "hybride"): model kit v2 for the generated Vadsrijk mobs.

Not a feature by itself (not in FEATURES): vadsrijk_wezens.Model and the painters of zone 1-3 call into it when a model is built
with stijl 2 (see vadsrijk_wezens.stijl()). Everything stays generated and deterministic.

Geometry
  * rond_blok:  a big cube becomes a rounded box of 4 cubes (3 axis bars inset by c on the other two axes + one inner cube
                inset by 0.29c on all axes = a two-step quarter circle on every edge and corner). Thin parts (< 3 px), crystals,
                blades, shields etc. stay sharp (SCHERP). The face goes on the front bar, so it stays flat.
  * taps:       long thin limbs (role in LEDEMAAT, 2x as long as thick) become 2 cubes: the lower part 80 % as thick.
Painting (per face region, in model space)
  * light from above (a gradient over the model height), a soft contact shadow at the bottom of side faces, a bevel highlight
    at the top, recessed faces of a rounded box darker (fake normals: the box reads round), soft low-frequency mottling;
  * patterns redone softly (glans = a real specular blob + sparkle, stippen with a shadow, plekjes as soft blobs,
    facet with light edges, fur strokes on vacht/pluis/manen);
  * faces drawn 4x supersampled (anti-aliased) + an extra catchlight: cute guh eyes with shine.
Glow
  * glowmask regions get a hot centre and a softer edge (instead of a flat colour).
Secondary motion
  * naloop(): ears, tails, antennae, manes... get a small lagging wiggle in every looping animation that does not move them yet.
"""
import math

import numpy as np
from PIL import Image

SCHERP = {"kristal", "zwaard", "spiegel", "schild", "plaat", "vizier", "staf", "steel_wapen", "tand", "angel", "boor", "ijzer",
          "lamp", "lampje", "licht", "veer", "knop", "gevest", "snavel", "klauw", "naald", "hoorn", "kaas", "glimvlek", "spore",
          "meeldraad", "vin", "strik", "bloem", "bloemhart", "bel", "druppel", "bol", "zwart"}
LEDEMAAT = {"poot", "been", "broek", "laars"}
WIEBEL_PREFIX = ("oor", "ear", "staart", "tail", "antenne", "voelspriet", "manen", "wiebel", "pluim", "kuif", "lap", "sprieten")


# =====================================================================================================================
# geometry
# =====================================================================================================================
def _half(v):
    return round(v * 2) / 2.0


def auto_c(rol, size, gezicht):
    if rol in SCHERP:
        return 0.0
    m = min(size)
    if m < 3:
        return 0.0
    c = min(3.0, max(0.5, _half(m * 0.2)))
    if gezicht:
        c = max(0.5, _half(c * 0.5))
    return min(c, m * 0.3)


def rond_blok(model, raw, bone, rol, origin, size, c, rot=None, pivot=None, inflate=0.0, gezicht=None):
    """The rounded box (4 cubes). raw = the model's plain cube function."""
    ox, oy, oz = origin
    sx, sy, sz = size

    def bar(ix, iy, iz, voor=False):
        rec = {"north": iz, "south": iz, "east": ix, "west": ix, "up": iy, "down": iy}
        return ((ox + ix, oy + iy, oz + iz), (sx - 2 * ix, sy - 2 * iy, sz - 2 * iz), rec, voor)

    stukken = [bar(c, c, 0, True), bar(0, c, c), bar(c, 0, c)]
    if c >= 2.5:
        # three steps on the quarter circle (0/30/60/90 degrees): 3 more bars + the inner cube
        a, b = c * 0.134, c * 0.5
        stukken += [bar(b, b, a), bar(a, b, b), bar(b, a, b), bar(c * 0.29, c * 0.29, c * 0.29)]
    else:
        k = _half(max(0.5, c * 0.29)) if c >= 1.5 else c * 0.5
        stukken.append(bar(k, k, k))
    for o, s, rec, voor in stukken:
        if min(s) <= 0:
            continue
        raw(bone, rol, o, s, rot, pivot, inflate, gezicht if voor else None, {f: (v / c if c else 0) for f, v in rec.items()})
    return len(stukken)


def taps(raw, bone, rol, origin, size, rot=None, pivot=None, inflate=0.0):
    """A limb in 2 parts: the upper 55 % full, the lower part 80 % as thick (centred)."""
    ox, oy, oz = origin
    sx, sy, sz = size
    boven = sy * 0.55
    raw(bone, rol, (ox, oy + sy - boven, oz), (sx, boven, sz), rot, pivot, inflate, None, None)
    f = 0.8
    raw(bone, rol, (ox + sx * (1 - f) / 2, oy, oz + sz * (1 - f) / 2), (sx * f, sy - boven + 0.05, sz * f), rot, pivot, inflate, None, None)


def is_taps(rol, size):
    sx, sy, sz = size
    return rol in LEDEMAAT and sy >= 2.0 * max(sx, sz) and max(sx, sz) >= 1.5 and sy >= 3


# =====================================================================================================================
# painting
# =====================================================================================================================
def model_hoogte(model):
    ys = [(r["o"][1], r["o"][1] + r["s"][1]) for r in model.regios if "o" in r]
    if not ys:
        return 0.0, 16.0
    return min(a for a, _ in ys), max(b for _, b in ys)


def _ruis_laag(shape, r, schaal=6.0):
    """Soft low-frequency noise in [-1, 1] (a small random grid, bilinear upscaled)."""
    h, w = shape
    gh, gw = max(2, int(h / schaal) + 2), max(2, int(w / schaal) + 2)
    g = r.uniform(-1, 1, (gh, gw)).astype(np.float32)
    im = Image.fromarray(((g + 1) * 127.5).astype(np.uint8)).resize((w, h), Image.BILINEAR)
    return np.asarray(im, np.float32) / 127.5 - 1


def licht(model, reg, h, w, S):
    """Light factor per pixel of a face region: gradient over the model height, contact shadow + bevel, recess darkening."""
    y0, y1 = model._yb if hasattr(model, "_yb") else model_hoogte(model)
    span = max(1.0, y1 - y0)
    f = reg["f"]
    o, s = reg["o"], reg["s"]
    rec = (reg.get("rec") or {}).get(f, 0.0)
    L = np.ones((h, w), np.float32)
    if f in ("north", "south", "east", "west"):
        rows = (np.arange(h, dtype=np.float32) + 0.5) / S          # model px from the top of the face
        y = (o[1] + s[1]) - rows
        yn = np.clip((y - y0) / span, 0, 1)
        L *= (0.80 + 0.26 * yn)[:, None]
        if s[1] >= 3 and not reg.get("rec"):
            fh = max(1.0, s[1] * S)
            t = rows * S / fh                                        # 0 top .. 1 bottom of this face
            L *= (1.0 - 0.10 * np.clip((t - 0.72) / 0.28, 0, 1) ** 1.5)[:, None]     # contact shadow
            bev = max(1, int(round(S * 0.5)))
            L[:bev] *= 1.07                                          # bevel highlight at the top edge
        L *= 0.97 if f in ("east", "west") else (0.94 if f == "south" else 1.0)
    elif f == "up":
        yn = np.clip((o[1] + s[1] - y0) / span, 0, 1)
        L *= 1.04 + 0.06 * yn
        if not reg.get("rec") and min(s[0], s[2]) >= 3:
            bev = max(1, int(round(S * 0.4)))
            L[:bev] *= 0.95
            L[-bev:] *= 0.95
            L[:, :bev] *= 0.95
            L[:, -bev:] *= 0.95
    else:
        L *= 0.70
    if f == "up":
        L *= 1.0 - 0.05 * rec
    elif f == "down":
        L *= 1.0 - 0.10 * rec
    else:
        L *= 1.0 - 0.03 * rec
    return L


def vul_v2(model, reg, blok, kleur, r, var=4.0):
    h, w = blok.shape[:2]
    S = model.px
    L = licht(model, reg, h, w, S)
    mott = _ruis_laag((h, w), r, 5.0 * S / 4) * 0.045
    fijn = r.normal(0, var, (h, w)).astype(np.float32)
    c = np.array(kleur, np.float32)[None, None, :] * (L * (1 + mott))[..., None] + fijn[..., None]
    blok[..., :3] = np.clip(c, 0, 255)
    blok[..., 3] = 255
    return L


def _blend(blok, m, kleur, a=1.0):
    """Mix a colour into blok where mask m (0..1 floats) with strength a."""
    m = np.clip(m * a, 0, 1)[..., None]
    blok[..., :3] = np.clip(blok[..., :3] * (1 - m) + np.array(kleur, np.float32) * m, 0, 255)


def patroon_v2(blok, naam, kleur2, r, f, L):
    h, w = blok.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    if naam == "glans":
        if f in ("north", "up", "west", "east"):
            cx, cy = w * 0.27, h * 0.24
            d = np.hypot((xx - cx) / max(1.5, w * 0.16), (yy - cy) / max(1.2, h * 0.11))
            _blend(blok, np.exp(-d ** 2 * 1.6), (255, 255, 255), 0.55)
            sp = np.hypot(xx - (cx - w * 0.05), yy - (cy - h * 0.03)) < max(0.9, min(w, h) * 0.04)
            blok[sp, :3] = np.clip(blok[sp, :3] * 0.2 + 255 * 0.8, 0, 255)
            # a soft rim light along the opposite lower edge
            d2 = np.clip(1 - (w - xx) / max(2.0, w * 0.12), 0, 1) * np.clip(yy / h, 0, 1)
            _blend(blok, d2, (255, 255, 255), 0.12)
    elif naam == "stippen":
        n = max(1, (h * w) // 160)
        for _ in range(n):
            cx, cy, rr = r.integers(0, w), r.integers(0, h), max(1.5, min(h, w) * r.uniform(0.07, 0.14))
            d = np.hypot(xx - cx, yy - cy - rr * 0.18)
            _blend(blok, np.clip((rr * 1.15 - d) / 1.2, 0, 1), (0, 0, 0), 0.18)              # a little shadow below
            d = np.hypot(xx - cx, yy - cy)
            m = np.clip(rr - d + 0.5, 0, 1)
            col = np.array(kleur2, np.float32)
            _blend(blok, m, col * (0.92 + 0.12 * L[..., None].mean()), 1.0)
            hl = np.clip(rr * 0.45 - np.hypot(xx - cx + rr * 0.35, yy - cy + rr * 0.35), 0, 1)
            _blend(blok, hl, (255, 255, 255), 0.5)
    elif naam == "plekjes":
        for _ in range(max(1, (h * w) // 70)):
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            rx, ry = r.uniform(1.2, 3.2), r.uniform(1.0, 2.6)
            d = np.hypot((xx - cx) / rx, (yy - cy) / ry)
            _blend(blok, np.clip(1.2 - d, 0, 1), np.array(kleur2, np.float32) * L.mean(), 0.75)
    elif naam == "strepen":
        per = 6.0
        fase = (xx % per) / per
        m = np.clip(np.sin(fase * 2 * np.pi) * 2.2, 0, 1)
        _blend(blok, m, np.array(kleur2, np.float32) * L[..., None], 0.85)
    elif naam == "veer":
        mid = w / 2
        _blend(blok, np.clip(1.4 - np.abs(xx - mid), 0, 1), kleur2, 0.9)
        for k in range(1, max(2, h // 5)):
            y = k * 5
            _blend(blok, np.clip(1 - np.abs(yy - (y + np.abs(xx - mid) * 0.5)), 0, 1) * 0.6, kleur2, 0.6)
        t = np.clip((yy - h * 0.8) / max(1, h * 0.2), 0, 1)
        _blend(blok, t, (255, 255, 255), 0.18)
    elif naam == "facet":
        # big diagonal facets: alternating light/dark planes with bright edge lines
        per = max(6.0, min(h, w) * 0.55)
        u = (xx + yy * 0.6) / per
        band = np.floor(u)
        tint = np.where(band % 2 == 0, 1.08, 0.88).astype(np.float32)
        blok[..., :3] = np.clip(blok[..., :3] * tint[..., None], 0, 255)
        edge = np.clip(1 - np.abs(u - np.round(u)) * per / 1.1, 0, 1)
        _blend(blok, edge, (255, 255, 255), 0.45)
        _blend(blok, np.clip(1 - np.abs(u - np.round(u)) * per / 2.5, 0, 1) * (band % 2 == 1), kleur2, 0.3)
    elif naam == "knetter":
        for _ in range(max(1, (h * w) // 30)):
            cx, cy = r.integers(0, w), r.integers(0, h)
            blok[cy, cx, :3] = kleur2


def vacht_v2(blok, r, sterkte=1.0):
    """Fur/fluff: short downward strokes, lighter and darker."""
    h, w = blok.shape[:2]
    n = int(h * w / 9 * sterkte)
    for _ in range(n):
        x, y = int(r.integers(0, w)), int(r.integers(0, h))
        l = int(r.integers(2, 4))
        f = r.choice((0.86, 0.9, 1.1, 1.14))
        x2 = min(w - 1, x + int(r.integers(-1, 2)))
        blok[y:min(h, y + l), x, :3] = np.clip(blok[y:min(h, y + l), x, :3] * f, 0, 255)
        if l > 2 and y + l < h:
            blok[min(h - 1, y + l), x2, :3] = np.clip(blok[min(h - 1, y + l), x2, :3] * f, 0, 255)


def supersample(blok, teken, n=4):
    """Run a face-drawing function on a n x upscaled copy, then box-filter back down: anti-aliased faces."""
    h, w = blok.shape[:2]
    groot = np.asarray(Image.fromarray(blok).resize((w * n, h * n), Image.NEAREST)).copy()
    teken(groot)
    klein = np.asarray(Image.fromarray(groot).resize((w, h), Image.BOX))
    blok[..., :3] = klein[..., :3]


def oogglans(blok, ogen, cy, er):
    """An extra catchlight under each eye (the "shine" of a cute face), after the face is drawn (non-supersampled)."""
    h, w = blok.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    for ex in ogen:
        d = np.hypot(xx - (ex - er * 0.38), yy - (cy - er * 0.4))
        _blend(blok, np.clip(er * 0.36 - d + 0.5, 0, 1), (255, 255, 255), 0.95)
        d = np.hypot(xx - (ex + er * 0.35), yy - (cy + er * 0.42))
        _blend(blok, np.clip(er * 0.16 - d + 0.5, 0, 1), (230, 236, 255), 0.85)


def gloed_v2(blok, kleur, r):
    """A glowmask region: a hot centre fading to ~60 % at the edges, with a little flicker noise."""
    h, w = blok.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.hypot((xx - w / 2) / max(1, w / 2), (yy - h / 2) / max(1, h / 2)) / math.sqrt(2)
    f = 1.08 - 0.45 * d ** 1.4
    c = np.array(kleur, np.float32)[None, None, :] * f[..., None] + r.normal(0, 5, (h, w, 1))
    blok[..., :3] = np.clip(c, 0, 255)
    blok[..., 3] = 255


# =====================================================================================================================
# secondary motion
# =====================================================================================================================
def _wiebelt(naam):
    n = naam.lower()
    return n.startswith(WIEBEL_PREFIX) or "_oor" in n or n.endswith("oor")


def naloop(model, anims, sterkte=1.0):
    """Ears, tails, manes, antennae... get a small lagging wiggle in every looping animation that does not move them yet.
    Additive to the bone's own rest rotation (GeckoLib adds animation rotation to the rest pose). Bone and animation names
    are untouched."""
    bones = [b["name"] for b in model.bones if _wiebelt(b["name"])]
    if not bones:
        return anims
    for naam, a in anims.items():
        if a.get("loop") is not True:
            continue
        L = float(a["animation_length"])
        if L <= 0:
            continue
        cycli = max(1, int(round(L / 1.2)))
        for i, b in enumerate(bones):
            if b in a["bones"]:
                continue
            n = b.lower()
            if n.startswith(("staart", "tail")):
                as_, amp = 1, 9.0
            elif n.startswith(("manen", "pluim", "kuif", "lap")):
                as_, amp = 0, 5.0
            else:
                as_, amp = 2, 6.0
            if n.endswith("_r") or n.endswith("r") and n[:-1].endswith("_"):
                amp = -amp
            amp *= sterkte * (1.4 if naam in ("loop", "hop", "vlieg", "walk") else 1.0)
            fase = 0.25 + 0.15 * (i % 3)
            pts = {}
            for k in range(9):
                t = L * k / 8
                v = [0.0, 0.0, 0.0]
                v[as_] = round(amp * math.sin(2 * math.pi * (cycli * k / 8 - fase)), 2)
                pts[f"{t:g}"] = v
            a["bones"][b] = {"rotation": pts}
    return anims


def wiebel_kap(anims, bone="wiebel", lengte_idle=None):
    """Jiggle of a slime's top cap: a lagging squash-and-stretch on top of the body's own."""
    for naam, a in anims.items():
        if bone in a["bones"]:
            continue
        L = float(a["animation_length"])
        if a.get("loop") is True:
            cycli = max(1, int(round(L / 1.0))) if naam != "hop" else 1
            pts = {}
            for k in range(9):
                t = L * k / 8
                s = math.sin(2 * math.pi * (cycli * k / 8 - 0.3))
                amp = 0.07 if naam in ("hop", "blij") else 0.035
                pts[f"{t:g}"] = [round(1 + amp * s, 3), round(1 - amp * 1.3 * s, 3), round(1 + amp * s, 3)]
            a["bones"][bone] = {"scale": pts}
        else:
            # one-shots: a damped wobble after the action
            pts = {}
            for k in range(9):
                t = L * k / 8
                dem = math.exp(-2.5 * k / 8)
                s = math.sin(2 * math.pi * 1.5 * k / 8)
                pts[f"{t:g}"] = [round(1 + 0.08 * dem * s, 3), round(1 - 0.1 * dem * s, 3), round(1 + 0.08 * dem * s, 3)]
            a["bones"][bone] = {"scale": pts}
    return anims


# =====================================================================================================================
# counting
# =====================================================================================================================
def kubussen(model):
    return sum(len(b.get("cubes", [])) for b in model.bones)
