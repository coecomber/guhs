"""
einde2, slice cast_team - the little toolkit its characters are drawn with (CONTRACT_E2 11.2):

  Figuur        one GuhNpcEntity.Kind: the base model ("zit" = the sitting guh, "mika" = the ordinary Mika) loaded and never
                written back, its texture recoloured, plus
                  kleur()   a swatch in a free 8x8 cell of ITS OWN sheet (knuffeldal_npcs._paint), by name
                  k()       a cube that samples one swatch on all six faces (knuffeldal_npcs._cube)
                  bot()     an extra bone <basis>_<ding> on one of the base bones
                  verf()    paint ON the base cubes in MODEL space: fn(vlak, X, Y, Z, zet) is called for every face with the
                            model coordinates of its texels, so a hem, a hairline or an arrow runs on at the same height over
                            the overlapping cubes the round guh is made of
                  gezicht() the pixels of the face (the front of the foremost head cube), as the viewer sees them
                  breder()  resize the cubes of a base bone around a point (round Miroh, the big kroegbaas)
  anim / T      animations as data: T((t, x, y, z), ...) is one channel; weef() weaves a move into the idle loop
  quads()       the model as quads for wiki_renders.render, with a pose taken from an animation at a time

Which texel lies where on a face is GeckoLib's own rule (GeometryCube.bake, VertexSet, GeometryQuadUvs of geckolib 5.5.2):
the cube is mirrored in x when it is baked, so on the "north" face u runs with +x of the file, on "south" against it,
"west" is the face at the file's +x side, and "up" has its top row at the back. wiki_renders.geo_quads draws every face
the other way round (which nobody sees on a symmetric guh); quads() here follows GeckoLib, so that what is painted on one
side (a scar, a squeezed eye) is seen on that side.
"""
import os

import numpy as np
from PIL import Image

import features.einde2_kit as kit
from features import knuffeldal_npcs as kn

PX = 4                                   # texture pixels per uv unit (512 px sheets on a 128 uv model)
BASIS = {"zit": ("guh_sitting.geo.json", "guh_sitting.png"), "mika": ("mika.geo.json", "mika.png")}
BASISBOTTEN = {"zit": ("root", "body", "tail", "foot_left", "foot_right", "arm_left", "arm_right", "head", "ear_left", "ear_right"),
               "mika": ("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right",
                        "leg_front_left", "leg_front_right")}
# the face: (bone, index of the cube whose north face carries it)
GEZICHT = {"zit": ("head", 2), "mika": ("head", 2)}
VLAKKEN = ("north", "south", "east", "west", "up", "down")


def punten(c, vlak, A, B):
    """The model coordinates (X, Y, Z) of the texels at the fractions A (along u) and B (along v) of a face of a cube."""
    x0, y0, z0 = c["origin"]
    w, hh, d = c["size"]
    x1, y1, z1 = x0 + w, y0 + hh, z0 + d
    one = np.ones_like(A)
    if vlak == "north":
        return x0 + A * w, y1 - B * hh, z0 * one
    if vlak == "south":
        return x1 - A * w, y1 - B * hh, z1 * one
    if vlak == "west":
        return x1 * one, y1 - B * hh, z0 + A * d
    if vlak == "east":
        return x0 * one, y1 - B * hh, z1 - A * d
    if vlak == "up":
        return x0 + A * w, y1 * one, z1 - B * d
    return x0 + A * w, y0 * one, z0 + B * d


def _rechthoek(uv):
    (u, v), (w, hh) = uv["uv"], uv["uv_size"]
    return int(round(u * PX)), int(round(v * PX)), max(1, int(round(w * PX))), max(1, int(round(hh * PX)))


class Figuur:
    """One Kind being drawn: see the module docstring."""

    def __init__(self, h, kind, lijf, vacht):
        geo_naam, tex_naam = BASIS[lijf]
        self.h, self.kind, self.lijf = h, kind, lijf
        self.bestand = kn._load(h, geo_naam)
        self.geo = self.bestand["minecraft:geometry"][0]
        self.geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
        self.botten = {b["name"]: b for b in self.geo["bones"]}
        self.rng = kit.rng(kind)
        bron = Image.open(os.path.join(h.TEX, "entity", tex_naam)).convert("RGBA")
        hue, sat, val = vacht
        self.a = np.asarray(h.recolour(bron, hue=hue, sat=sat, val=val, only=h.pinkish).convert("RGBA")).copy()
        gebruikt = kn._used(self.geo)
        self.vrij = [(x, y) for y in range(0, kn.UV - kn.SW + 1, kn.SW) for x in range(0, kn.UV - kn.SW + 1, kn.SW)
                     if not gebruikt[y:y + kn.SW, x:x + kn.SW].any()]
        self.staal = {}
        self.gloed = np.zeros(self.a.shape[:2], bool)          # the pixels of the glowmask
        self.alfa = 255                                        # < 255: a spirit (everything is painted see-through)

    # ---- swatches, cubes, bones ---------------------------------------------------------------------------------
    def kleur(self, naam, rgb, var=6, patroon=None, gloed=False):
        """The swatch of that name (made and painted the first time)."""
        if naam not in self.staal:
            if not self.vrij:
                raise SystemExit(f"einde2_cast_team {self.kind}: no free texture space for {naam} (reuse a swatch)")
            s = self.vrij.pop(0)
            self.staal[naam] = s
            kn._paint(self.a, s, rgb, self.rng, var, patroon)
            if gloed:
                x, y = s
                self.gloed[y * PX:(y + kn.SW) * PX, x * PX:(x + kn.SW) * PX] = self.a[y * PX:(y + kn.SW) * PX, x * PX:(x + kn.SW) * PX, 3] > 0
        return naam

    def k(self, origin, size, naam, inflate=0.0):
        return kn._cube([float(v) for v in origin], [float(v) for v in size], self.staal[naam], inflate)

    def bot(self, naam, ouder, pivot, kubussen=(), rotatie=None):
        b = {"name": naam, "parent": ouder, "pivot": [float(v) for v in pivot], "cubes": list(kubussen)}
        if rotatie:
            b["rotation"] = [float(v) for v in rotatie]
        self.geo["bones"].append(b)
        self.botten[naam] = b
        return b

    def breder(self, bot, sx=1.0, sy=1.0, sz=1.0, om=(0.0, 0.0, 0.0)):
        """Resizes the cubes of a base bone around a point (their uv stays: the fur stretches a little)."""
        for c in self.botten[bot]["cubes"]:
            lo = [om[i] + (c["origin"][i] - om[i]) * s for i, s in enumerate((sx, sy, sz))]
            c["origin"], c["size"] = [round(v, 3) for v in lo], [round(c["size"][i] * s, 3) for i, s in enumerate((sx, sy, sz))]

    # ---- painting on the base cubes -----------------------------------------------------------------------------
    def verf(self, botten, fn, kubussen=None):
        """fn(vlak, X, Y, Z, zet): zet(mask, rgb, var=5, gloed=False) paints the texels of the mask."""
        for naam in ([botten] if isinstance(botten, str) else botten):
            for i, c in enumerate(self.botten[naam]["cubes"]):
                if not isinstance(c.get("uv"), dict) or (kubussen is not None and i not in kubussen):
                    continue
                for vlak, uv in c["uv"].items():
                    x, y, W, H = _rechthoek(uv)
                    A, B = np.meshgrid((np.arange(W) + 0.5) / W, (np.arange(H) + 0.5) / H)
                    blok = self.a[y:y + H, x:x + W]
                    gloei = self.gloed[y:y + H, x:x + W]

                    def zet(mask, rgb, var=5, gloed=False, blok=blok, gloei=gloei):
                        mask = np.broadcast_to(np.asarray(mask, bool), blok.shape[:2])
                        if not mask.any():
                            return
                        ruis = self.rng.normal(0, var / 2, (int(mask.sum()), 1)) if var else 0
                        blok[mask, :3] = np.clip(np.array(rgb[:3], np.float32) + ruis, 0, 255).astype(np.uint8)
                        blok[mask, 3] = rgb[3] if len(rgb) > 3 else 255
                        if gloed:
                            gloei[mask] = True
                    fn(vlak, *punten(c, vlak, A, B), zet)

    def gezicht(self):
        """The face as (pixels, breedte, hoogte): a view of the sheet, left = the viewer's left (the character's right)."""
        bot, i = GEZICHT[self.lijf]
        x, y, W, H = _rechthoek(self.botten[bot]["cubes"][i]["uv"]["north"])
        return self.a[y:y + H, x:x + W], W, H

    def doorzichtig(self, alfa):
        """A spirit: every opaque pixel gets this alpha (CONTRACT_E2 11.2 F: 150-190)."""
        self.a[self.a[..., 3] > 0, 3] = alfa

    # ---- writing ------------------------------------------------------------------------------------------------
    def bewaar(self, gloed=False):
        h = self.h
        kn._save_geo(h, f"guh_npc_{self.kind}.geo.json", self.bestand)
        h.save(Image.fromarray(self.a), "entity", f"npc_{self.kind}.png")
        if gloed:
            m = np.zeros_like(self.a)
            m[self.gloed] = self.a[self.gloed]
            m[self.gloed, 3] = 255
            h.save(Image.fromarray(m), "entity", f"npc_{self.kind}_glowmask.png")


# =====================================================================================================================
# small painters for swatches (a block is 32 x 32 pixels, the top row first)
# =====================================================================================================================
def strepen(rgb, stap=8, dik=4, staand=False):
    """Stripes in a second colour."""
    def p(block):
        for i in range(0, 32, stap):
            if staand:
                block[:, i:i + dik, :3] = rgb
            else:
                block[i:i + dik, :, :3] = rgb
    return p


def rand(rgb, dik=3):
    """A border in a second colour."""
    def p(block):
        block[:dik, :, :3] = rgb
        block[-dik:, :, :3] = rgb
        block[:, :dik, :3] = rgb
        block[:, -dik:, :3] = rgb
    return p


def stip(rgb, r=7, cx=15.5, cy=15.5):
    def p(block):
        yy, xx = np.mgrid[0:32, 0:32]
        block[np.hypot(xx - cx, yy - cy) <= r, :3] = rgb
    return p


def samen(*ps):
    def p(block):
        for q in ps:
            q(block)
    return p


def donker(rgb, f):
    return tuple(int(max(0, min(255, v * f))) for v in rgb)


# =====================================================================================================================
# animations as data
# =====================================================================================================================
def T(*punten_):
    """One channel: (t, x, y, z) points -> {"t": [x, y, z]}; a scale may be given as (t, s)."""
    out = {}
    for p in punten_:
        v = [p[1]] * 3 if len(p) == 2 else list(p[1:])
        out[repr(float(round(p[0], 3)))] = [float(x) for x in v]
    return out


NUL = T((0, 0))                          # the scale of a prop that is not shown


def anim(lengte, botten, lus=False, weg=()):
    """One animation; `weg` = the prop bones that stay hidden in it (those it does not move itself)."""
    botten = {b: dict(c) for b, c in botten.items()}
    for prop in weg:
        botten.setdefault(prop, {}).setdefault("scale", NUL)
    return {"loop": True if lus else False, "animation_length": float(lengte), "bones": botten}


def _rust(lengte, lijf):
    """The quiet part of an idle loop of that length: breathing, a little head, the ears, the tail (per 4 seconds)."""
    n = int(round(lengte / 4))
    b = {"body": {"scale": []}, "head": {"rotation": []}, "ear_left": {"rotation": []}, "ear_right": {"rotation": []}, "tail": {"rotation": []}}
    for i in range(n):
        t = 4.0 * i
        b["body"]["scale"] += [(t, 1, 1, 1), (t + 2, 1.03, 1.04, 1.03)]
        b["head"]["rotation"] += [(t, 0, 0, 0), (t + 1, 0, 0, 4), (t + 2, 0, 0, 0), (t + 3, 0, 0, -4)]
        b["ear_left"]["rotation"] += [(t, 0, 0, 0), (t + 2.6, 0, 0, 0), (t + 2.75, 0, 0, -14), (t + 2.9, 0, 0, 0)]
        b["ear_right"]["rotation"] += [(t, 0, 0, 0), (t + 3.1, 0, 0, 0), (t + 3.25, 0, 0, 14), (t + 3.4, 0, 0, 0)]
        b["tail"]["rotation"] += [(t, 0, -10, 0), (t + 2, 0, 10, 0)] if lijf == "zit" else \
            [(t, 0, -16, 0), (t + 1, 0, 16, 0), (t + 2, 0, -16, 0), (t + 3, 0, 16, 0)]
    b["body"]["scale"].append((lengte, 1, 1, 1))
    b["head"]["rotation"].append((lengte, 0, 0, 0))
    b["ear_left"]["rotation"].append((lengte, 0, 0, 0))
    b["ear_right"]["rotation"].append((lengte, 0, 0, 0))
    b["tail"]["rotation"].append((lengte, 0, -10 if lijf == "zit" else -16, 0))
    return b


def _lijst(kanaal):
    return sorted((float(t), v) for t, v in kanaal.items())


def weef(lengte, lijf, zet, begin, weg=(), extra=None):
    """The idle loop: the quiet part with the move `zet` (an anim()) woven in at `begin` seconds; `extra` = more bones
    that move all the time (steam). Props of `weg` are hidden outside the move."""
    duur = zet["animation_length"]
    rust = _rust(lengte, lijf)
    botten = {}
    for bot, kanalen in rust.items():
        botten[bot] = {k: T(*[(t,) + tuple(v) for t, *v in kanalen[k]]) for k in kanalen}
    for bot, kanalen in zet["bones"].items():
        for k, kanaal in kanalen.items():
            nieuw = [(t + begin, v) for t, v in _lijst(kanaal)]
            oud = [(t, v) for t, v in _lijst(botten.get(bot, {}).get(k, {})) if t < begin - 1e-6 or t > begin + duur + 1e-6]
            if not oud:
                oud = [(0.0, nieuw[0][1]), (float(lengte), nieuw[-1][1])]
            alles = dict((repr(float(round(t, 3))), v) for t, v in sorted(oud + nieuw, key=lambda p: p[0]))
            botten.setdefault(bot, {})[k] = alles
    for bot, kanalen in (extra or {}).items():
        botten.setdefault(bot, {}).update(kanalen)
    return anim(lengte, botten, lus=True, weg=weg)


def op(animatie, t):
    """The pose {bone: {"rotation" / "position" / "scale": [x, y, z]}} of an animation at t seconds (linear between keys)."""
    pose = {}
    for bot, kanalen in animatie["bones"].items():
        for k, kanaal in kanalen.items():
            punten_ = _lijst(kanaal)
            v = punten_[-1][1]
            for (t0, v0), (t1, v1) in zip(punten_, punten_[1:]):
                if t0 <= t <= t1:
                    f = 0 if t1 == t0 else (t - t0) / (t1 - t0)
                    v = [a + (b - a) * f for a, b in zip(v0, v1)]
                    break
            if t <= punten_[0][0]:
                v = punten_[0][1]
            pose.setdefault(bot, {})[k] = list(v)
    return pose


# =====================================================================================================================
# the model as quads, and a picture of them
# =====================================================================================================================
def render(qs, yaw=35, pitch=-25, size=512, margin=0.08, ss=2):
    """wiki_renders.render (orthographic, the same light and fit), with all samples of all quads in one numpy pass:
    a sheet of poses takes seconds instead of minutes."""
    import wiki_renders as wr
    R = wr.rot_matrix(yaw, pitch)
    hoeken = np.array([R @ (q.origin + a * q.u + b * q.v) for q in qs for a in (0, 1) for b in (0, 1)])
    lo, hi = hoeken[:, :2].min(0), hoeken[:, :2].max(0)
    W = size * ss
    schaal = W * (1 - 2 * margin) / (max(hi - lo) or 1)
    midden = (lo + hi) / 2
    licht = np.array([-0.35, 0.85, -0.4])
    licht /= np.linalg.norm(licht)
    P_, Z_, C_ = [], [], []
    for q in qs:
        if (R @ q.normal)[2] > 1e-6:
            continue
        nrm = q.normal / (np.linalg.norm(q.normal) or 1)
        schaduw = 0.62 + 0.38 * max(0.0, float(np.dot(nrm, licht)))
        th, tw = q.tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        nu = max(2, int(np.linalg.norm(R @ q.u) * schaal * 1.6) + 1)
        nv = max(2, int(np.linalg.norm(R @ q.v) * schaal * 1.6) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        S = (q.origin + A[..., None] * q.u + B[..., None] * q.v) @ R.T
        px = ((S[..., 0] - midden[0]) * schaal + W / 2).astype(int)
        py = (-(S[..., 1] - midden[1]) * schaal + W / 2).astype(int)
        rgba = q.tex[np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int), np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)]
        m = (px >= 0) & (px < W) & (py >= 0) & (py < W) & (rgba[..., 3] > 20)
        P_.append(py[m] * W + px[m])
        Z_.append(S[..., 2][m])
        C_.append(rgba[m][:, :3] * schaduw)
    beeld = np.zeros((W * W, 4), np.float32)
    if P_:
        P, Z, C = np.concatenate(P_), np.concatenate(Z_), np.concatenate(C_)
        volgorde = np.argsort(-Z, kind="stable")                     # far first: the nearest sample is written last
        beeld[P[volgorde], :3] = C[volgorde]
        beeld[P[volgorde], 3] = 255
    img = Image.fromarray(np.clip(beeld.reshape(W, W, 4), 0, 255).astype(np.uint8), "RGBA")
    return img.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------------------------------------------------------------
def quads(geo, tex, pose=None):
    """Quads of a geo model (a dict) with its texture (a float array), in blocks, posed like GeckoLib does it: per bone
    translate, then turn and scale around its pivot (rotation = the model's own plus the animation's)."""
    import math
    import wiki_renders as wr
    pose = pose or {}
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    op_naam = {b["name"]: b for b in geo["bones"]}

    def eigen(b):
        p = pose.get(b["name"], {})
        rot = [a + c for a, c in zip(b.get("rotation", (0, 0, 0)), p.get("rotation", (0, 0, 0)))]
        rx, ry, rz = (math.radians(a) for a in rot)
        rx, rz = -rx, -rz
        X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
        Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
        Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
        M = Z @ Y @ X @ np.diag(p.get("scale", (1, 1, 1)))
        piv = np.array(b.get("pivot", (0, 0, 0)), float)
        return M, piv - M @ piv + np.array(p.get("position", (0, 0, 0)), float)

    def keten(naam):
        M, t = np.eye(3), np.zeros(3)
        while naam:
            b = op_naam[naam]
            Mb, tb = eigen(b)
            M, t = Mb @ M, Mb @ t + tb
            naam = b.get("parent")
        return M, t

    out = []
    for b in geo["bones"]:
        M, t = keten(b["name"])
        if abs(np.linalg.det(M)) < 1e-9:
            continue
        for c in b.get("cubes", []):
            inf = c.get("inflate", 0)
            cc = {"origin": [v - inf for v in c["origin"]], "size": [v + 2 * inf for v in c["size"]]}
            for vlak, n in (("north", (0, 0, -1)), ("south", (0, 0, 1)), ("west", (1, 0, 0)), ("east", (-1, 0, 0)), ("up", (0, 1, 0)), ("down", (0, -1, 0))):
                if vlak not in c.get("uv", {}):
                    continue
                hoek = [np.array([float(v[0, 0]) for v in punten(cc, vlak, np.array([[a]]), np.array([[bb]]))]) for a, bb in ((0, 0), (1, 0), (0, 1))]
                o, u, v = hoek[0], hoek[1] - hoek[0], hoek[2] - hoek[0]
                (fu, fv), (fw, fh) = c["uv"][vlak]["uv"], c["uv"][vlak]["uv_size"]
                out.append(wr.Quad((M @ o + t) / 16, M @ u / 16, M @ v / 16, tex, (fu / tw, fv / th, (fu + fw) / tw, (fv + fh) / th), M @ np.array(n, float)))
    return out
