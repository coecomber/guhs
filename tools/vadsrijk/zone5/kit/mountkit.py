"""Mountkit: GeckoLib-geo + texture uit python (zoals tools/features/vadsrijk_economie.py), poseerbaar renderen
met de gedeelde software-renderer (base-ontwerpen/_bron/mcrender.py, alleen geimporteerd, niet aangepast).

  Model(naam)            bouwt bones + cubes; textures op 4 px per modelpixel (D), per vlak beschilderbaar.
  m.blob(...)            afgeronde 'knuffel'-vorm (overlappende dozen, zoals de guh-kop).
  m.staaf(...)           een balkje van punt p0 naar p1 (geweitakken, staartpluimen).
  m.schrijf(dir, tex)    schrijft <naam>.geo.json + <tex>.png (GeckoLib-formaat, format_version 1.12.0).
  laad(geo, png)         leest ze terug; quads(model, base4, pose) -> extra-quads voor mcrender.render.
  bone_mats(...)         4x4 wereldmatrix per bone (voor het zadelpunt van de ruiter, teugels).
  speler()               een Steve-vormige speler (klassieke cuboids, simpele skin).
  diorama(...) / render_scene(...)   grondje + afbeelding.

Conventies: GeckoLib-coordinaten (voorkant = -z, y omhoog, voeten op y=0), rotaties in graden zoals in de
geo/animatie-json. Ruiterpose en loop-pose staan in de scripts zelf.
"""
import json, math, os, sys, zlib
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

KIT = os.path.dirname(os.path.abspath(__file__))  # kopie: eigen mcrender in deze map
sys.path.insert(0, KIT)
import mcrender as R  # noqa: E402
from mcrender import face_corners  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.dirname(HERE)
MODELS = os.path.join(HERE, "modellen")
D = 4  # texturepixels per modelpixel
JARS = [os.path.join(KIT, "..", "..", "renders-erfenis", "_bron", "jars", "guhs-assets-1.8.1.zip"),
        os.path.join(R.APPDATA, "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]


def mix(a, b, t):
    return tuple(float(x) * (1 - t) + float(y) * t for x, y in zip(a, b))


def licht(c, f):
    return tuple(min(255.0, v * f) for v in c)


# ======================================================================================================================
# model bouwen
# ======================================================================================================================
class Model:
    def __init__(self, naam, atlas_w=1024):
        self.naam = naam
        self.bones, self.bmap = [], {}
        self.aw = atlas_w
        self.x = self.y = self.rij = 0
        self.regions = []
        self.rng = np.random.default_rng(zlib.crc32(naam.encode()))

    def bone(self, naam, pivot, parent=None, rotation=None):
        b = {"name": naam, "pivot": [float(v) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = [float(v) for v in rotation]
        b["cubes"] = []
        self.bones.append(b)
        self.bmap[naam] = b
        return naam

    def _plek(self, pw, ph):
        if self.x + pw > self.aw:
            self.x, self.y, self.rij = 0, self.y + self.rij, 0
        p = (self.x, self.y)
        self.x += pw
        self.rij = max(self.rij, ph)
        return p

    def _vlak(self, pw, ph, kleur, fur, rand):
        a = np.zeros((ph, pw, 4), np.float32)
        a[..., :3] = kleur
        a[..., 3] = 255
        a[..., :3] += self.rng.normal(0, fur, (ph, pw, 1))
        if rand:
            yy, xx = np.mgrid[0:ph, 0:pw]
            d = np.minimum(np.minimum(xx + 0.5, pw - xx - 0.5), np.minimum(yy + 0.5, ph - yy - 0.5)) / (D * 1.2)
            a[..., :3] *= (1 - rand * (1 - np.clip(d, 0, 1)))[..., None]
        return a

    def cube(self, bone, origin, size, kleur, verf=None, inflate=0, rotation=None, pivot=None, fur=5, rand=0.10):
        """kleur: rgb, of dict vlak->rgb. verf: functie(arr, vlak) die het vlak beschildert (arr = HxWx4, 0..255),
        of dict vlak->functie(arr). Vlakken: north = voorkant (-z)."""
        sx, sy, sz = size
        dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz),
                "down": (sx, sz)}
        uv = {}
        for face, (w, h) in dims.items():
            pw, ph = max(1, int(round(w * D))), max(1, int(round(h * D)))
            u, v = self._plek(pw, ph)
            c = kleur.get(face, kleur.get("*")) if isinstance(kleur, dict) else kleur
            a = self._vlak(pw, ph, c, fur, rand)
            if verf:
                f = verf.get(face) if isinstance(verf, dict) else (lambda arr, fc, _v=verf: _v(arr, fc))
                if f:
                    try:
                        f(a, face)
                    except TypeError:
                        f(a)
            self.regions.append((u, v, a))
            uv[face] = {"uv": [u / D, v / D], "uv_size": [pw / D, ph / D]}
        c = {"origin": [float(o) for o in origin], "size": [float(s) for s in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        if rotation is not None:
            c["rotation"] = [float(r) for r in rotation]
            c["pivot"] = [float(p) for p in (pivot if pivot is not None else origin)]
        self.bmap[bone]["cubes"].append(c)
        return c

    def box(self, bone, center, size, kleur, **kw):
        cx, cy, cz = center
        sx, sy, sz = size
        return self.cube(bone, (cx - sx / 2, cy - sy / 2, cz - sz / 2), size, kleur, **kw)

    def blob(self, bone, center, size, kleur, r=2.0, voor=None, verf=None, fur=5, rand=0.10, hoek=True):
        """Afgeronde vorm: drie dozen met ingesprongen randen (+ een hoekvuller). voor = schilder voor de voorkant
        (het voorste vlak, van de doos met de volle diepte)."""
        sx, sy, sz = size
        cx, cy, cz = center
        sets = [((sx, sy - 2 * r, sz - 2 * r), None), ((sx - 2 * r, sy, sz - 2 * r), None),
                ((sx - 2 * r, sy - 2 * r, sz), voor)]
        if hoek:
            sets.append(((sx - 0.75 * r, sy - 0.75 * r, sz - 0.75 * r), None))
        for (a, b, c), v in sets:
            vv = verf
            if v:
                vv = dict(verf) if isinstance(verf, dict) else ({f: verf for f in ("north", "south", "east", "west", "up", "down")} if verf else {})
                vv["north"] = v
            self.box(bone, (cx, cy, cz), (a, b, c), kleur, verf=vv, fur=fur, rand=rand)

    def staaf(self, bone, p0, p1, w, kleur, verf=None, fur=4, rand=0.08, d=None):
        """Balkje van p0 naar p1 (GeckoLib-coordinaten), dikte w (of (w, d))."""
        p0, p1 = np.array(p0, float), np.array(p1, float)
        v = p1 - p0
        L = float(np.linalg.norm(v))
        dx, dy, dz = v / L
        dd = d if d is not None else w
        rz = math.degrees(math.asin(max(-1, min(1, -dx))))
        cz = math.cos(math.radians(rz))
        a = math.degrees(math.atan2(dz / max(cz, 1e-6), dy / max(cz, 1e-6)))
        return self.cube(bone, (p0[0] - w / 2, p0[1], p0[2] - dd / 2), (w, L, dd), kleur, verf=verf,
                         rotation=(-a, 0, rz), pivot=p0, fur=fur, rand=rand)

    def textuur(self):
        H = self.y + self.rij
        H = int(math.ceil(H / (4 * D)) * 4 * D)
        t = np.zeros((H, self.aw, 4), np.float32)
        for u, v, a in self.regions:
            t[v:v + a.shape[0], u:u + a.shape[1]] = a
        return t

    def geo(self):
        t = self.textuur()
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.naam}", "texture_width": self.aw // D,
                            "texture_height": t.shape[0] // D,
                            "visible_bounds_width": 5, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
            "bones": self.bones}]}

    def schrijf(self, tex_naam=None, map_=MODELS, geo=True):
        os.makedirs(map_, exist_ok=True)
        if geo:
            with open(os.path.join(map_, f"{self.naam}.geo.json"), "w", encoding="utf-8", newline="\n") as f:
                json.dump(self.geo(), f, indent=1)
        p = os.path.join(map_, f"{tex_naam or self.naam}.png")
        Image.fromarray(np.clip(self.textuur(), 0, 255).astype(np.uint8), "RGBA").save(p)
        return p


# ======================================================================================================================
# schilderhulpjes (arr = HxWx4 float 0..255, pixelcoordinaten)
# ======================================================================================================================
def ellips(arr, cx, cy, rx, ry, kleur, alpha=1.0):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = ((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2 <= 1
    arr[m, :3] = arr[m, :3] * (1 - alpha) + np.array(kleur, np.float32) * alpha
    return m


def zacht(arr, cx, cy, rx, ry, kleur, alpha=0.6):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    d = ((xx + 0.5 - cx) / rx) ** 2 + ((yy + 0.5 - cy) / ry) ** 2
    t = np.clip(1 - d, 0, 1)[..., None] * alpha
    arr[..., :3] = arr[..., :3] * (1 - t) + np.array(kleur, np.float32) * t


def guh_oog(arr, cx, cy, r, iris=(80, 160, 235)):
    """Een LPS-guh-oog: donkere rand, blauwe iris, zwarte pupil, twee glimmers, wimpertje."""
    ellips(arr, cx, cy, r * 1.0, r * 1.12, (22, 14, 30))
    ellips(arr, cx, cy + r * 0.12, r * 0.80, r * 0.88, iris)
    ellips(arr, cx, cy + r * 0.25, r * 0.80, r * 0.62, licht(iris, 0.75), 0.6)
    ellips(arr, cx, cy - r * 0.05, r * 0.62, r * 0.70, (18, 12, 26))
    ellips(arr, cx - r * 0.30, cy - r * 0.38, r * 0.26, r * 0.26, (255, 255, 255))
    ellips(arr, cx + r * 0.32, cy + r * 0.28, r * 0.13, r * 0.13, (255, 255, 255))


def guh_gezicht(arr, oog_y=0.40, afstand=0.25, r=None, blos=(255, 130, 175), mond=True, iris=(80, 160, 235)):
    h, w = arr.shape[:2]
    r = r or max(3, w * 0.12)
    cy = h * oog_y
    for s in (-1, 1):
        cx = w / 2 + s * w * afstand
        zacht(arr, cx + s * r * 0.9, cy + r * 1.7, r * 1.0, r * 0.6, blos, 0.75)
        guh_oog(arr, cx, cy, r, iris)
        # wimper
        x0 = int(cx + s * r * 0.85)
        y0 = int(cy - r * 0.9)
        for k in range(max(2, int(r * 0.45))):
            xx = x0 + s * k
            if 0 <= xx < w and 0 <= y0 - k < h:
                arr[max(0, y0 - k):y0 - k + 2, xx, :3] = (22, 14, 30)
    if mond:
        my = int(cy + r * 1.9)
        for dx in range(-int(r * 0.6), int(r * 0.6) + 1):
            yy = my + int(round(abs(math.sin(dx / max(1, r * 0.6) * math.pi)) * 1.2))
            xx = int(w / 2 + dx)
            if 0 <= xx < w and 0 <= yy < h:
                arr[yy, xx, :3] = arr[yy, xx, :3] * 0.55


def schubben(kleur_f=0.86, stap=8, ofs=0):
    """Veren-patroon: rijtjes zachte boogjes (donkerder)."""
    def f(arr, face=None):
        h, w = arr.shape[:2]
        for ri, y0 in enumerate(range(ofs, h + stap, stap)):
            off = (stap // 2) * (ri % 2)
            for x in range(w):
                t = ((x + off) % stap) / stap
                yy = y0 + int(round(stap * 0.45 * (1 - (2 * t - 1) ** 2)))
                for k in (0, 1):
                    if 0 <= yy - k < h:
                        arr[yy - k, x, :3] *= kleur_f if k == 0 else (1 + kleur_f) / 2
    return f


def sterretjes(n=12, kleur=(255, 245, 200), seed=1):
    def f(arr, face=None):
        h, w = arr.shape[:2]
        rng = np.random.default_rng(seed + h * 31 + w)
        for _ in range(max(1, int(n * h * w / 1600))):
            x, y = int(rng.integers(1, max(2, w - 1))), int(rng.integers(1, max(2, h - 1)))
            arr[y, x, :3] = kleur
            if rng.random() < 0.4:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x + dx < w and 0 <= y + dy < h:
                        arr[y + dy, x + dx, :3] = mix(arr[y + dy, x + dx, :3], kleur, 0.7)
    return f


def combi(*fs):
    fs = [f for f in fs if f]

    def f(arr, face=None):
        for g in fs:
            g(arr, face)
    return f


# ======================================================================================================================
# laden + poseren
# ======================================================================================================================
def laad(geo_path, png_path):
    g = json.load(open(geo_path, encoding="utf-8"))["minecraft:geometry"][0]
    tex = np.asarray(Image.open(png_path).convert("RGBA")).astype(np.float32) / 255.0
    return {"g": g, "tex": tex}


def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def rotm(rot):
    """GeckoLib-rotatie (graden) in de gespiegelde ruimte, zelfde volgorde als mcextra.geo_quads."""
    rx, ry, rz = (math.radians(v) for v in rot)
    return _rx(-rx) @ _ry(-ry) @ _rz(rz)


def M4(Rm=None, t=(0, 0, 0)):
    M = np.eye(4)
    if Rm is not None:
        M[:3, :3] = Rm
    M[:3, 3] = t
    return M


def T(t):
    return M4(None, t)


def S(s):
    M = np.eye(4)
    M[0, 0] = M[1, 1] = M[2, 2] = s
    return M


def RY(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return M4(np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]]))


def bone_mats(m, pose=None):
    pose = pose or {}
    bones = {b["name"]: b for b in m["g"]["bones"]}
    W = {}

    def get(n):
        if n in W:
            return W[n]
        b = bones[n]
        P = get(b["parent"]) if b.get("parent") else np.eye(4)
        pv = np.array([-b["pivot"][0], b["pivot"][1], b["pivot"][2]])
        p = pose.get(n, {})
        rot = np.array(b.get("rotation", [0, 0, 0]), float) + np.array(p.get("rot", [0, 0, 0]), float)
        pos = p.get("pos", [0, 0, 0])
        L = T((-pos[0], pos[1], pos[2])) @ T(pv) @ M4(rotm(rot)) @ T(-pv)
        W[n] = P @ L
        return W[n]

    for n in bones:
        get(n)
    return W


def punt(M, p):
    """GeckoLib-punt (ongespiegeld) door matrix M."""
    v = np.array([-p[0], p[1], p[2], 1.0])
    return (M @ v)[:3]


def quads(m, base, pose=None, verberg=(), tint=(1, 1, 1)):
    g = m["g"]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    texa = m["tex"]
    W = bone_mats(m, pose)
    out = []
    tint = np.asarray(tint, float)
    for b in g["bones"]:
        if any(s == b["name"] or (s.endswith("*") and b["name"].startswith(s[:-1])) for s in verberg):
            continue
        Mb = base @ W[b["name"]]
        for cube in b.get("cubes", []):
            o = np.array(cube["origin"], float)
            sz = np.array(cube["size"], float)
            inf = cube.get("inflate", 0)
            f = np.array([-(o[0] + sz[0]), o[1], o[2]]) - inf
            t = np.array([-o[0], o[1] + sz[1], o[2] + sz[2]]) + inf
            Rc = None
            if cube.get("rotation"):
                piv = cube["pivot"]
                pv = np.array([-piv[0], piv[1], piv[2]])
                Rc = T(pv) @ M4(rotm(cube["rotation"])) @ T(-pv)
            Mc = Mb @ Rc if Rc is not None else Mb
            for d, fu in cube["uv"].items():
                u, v = fu["uv"]
                w, h = fu["uv_size"]
                c = np.array(face_corners(d, f, t), float)
                c4 = np.c_[c, np.ones(3)] @ Mc.T
                cu = np.array([(u / tw, v / th), ((u + w) / tw, v / th), ((u + w) / tw, (v + h) / th),
                               (u / tw, (v + h) / th)])
                out.append((c4[:, :3], cu, texa, tint, False))
    return out


def base_mat(pos, yaw=0.0, schaal=1.0):
    """Model op wereldpositie pos (blokken), yaw in graden (0 = kijkt naar -z/noord, 180 = naar de camera bij yaw 0)."""
    return T(pos) @ RY(yaw) @ S(schaal / 16.0)


# ======================================================================================================================
# speler (klassieke Steve-cuboids, simpele skin)
# ======================================================================================================================
def speler(naam="speler"):
    m = Model(naam, atlas_w=512)
    huid, haar, shirt, broek, schoen = (232, 178, 140), (96, 62, 38), (70, 190, 200), (60, 70, 160), (90, 90, 100)

    def hoofd(arr, face):
        h, w = arr.shape[:2]
        if face == "up":
            arr[..., :3] = np.array(haar) + np.random.default_rng(3).normal(0, 6, (h, w, 1))
            return
        if face == "down":
            return
        k = int(h * 0.25) if face == "north" else int(h * 0.45)
        arr[:k, :, :3] = np.array(haar) + np.random.default_rng(4).normal(0, 6, (k, w, 1))
        if face == "south":
            arr[:, :, :3] = np.array(haar) + np.random.default_rng(5).normal(0, 6, (h, w, 1))
        if face == "north":
            p = h // 8
            arr[k:k + p, :p, :3] = haar
            arr[k:k + p, w - p:, :3] = haar
            ey = h // 2
            for ex in (p, 5 * p):
                arr[ey:ey + p, ex:ex + p, :3] = (255, 255, 255)
                arr[ey:ey + p, ex + p:ex + 2 * p, :3] = (70, 60, 160)
            arr[int(h * 0.62):int(h * 0.62) + p, 3 * p:5 * p, :3] = (150, 90, 70)
            arr[int(h * 0.75):int(h * 0.75) + p // 2 + 1, 2 * p:6 * p, :3] = (110, 60, 50)

    def romp(arr, face):
        h, w = arr.shape[:2]
        if face in ("north", "south"):
            arr[int(h * 0.82):, :, :3] = (40, 40, 60)  # riem
            if face == "north":
                arr[:int(h * 0.12), int(w * 0.3):int(w * 0.7), :3] = huid
                ellips(arr, w * 0.5, h * 0.45, w * 0.16, w * 0.14, (255, 140, 185))  # klein guh-hartje-logo

    def arm(arr, face):
        h, w = arr.shape[:2]
        if face == "down":
            arr[..., :3] = huid
            return
        if face != "up":
            arr[int(h * 0.35):, :, :3] = np.array(huid) + np.random.default_rng(6).normal(0, 4, (h - int(h * 0.35), w, 1))

    def been(arr, face):
        h, w = arr.shape[:2]
        if face == "down":
            arr[..., :3] = schoen
            return
        if face != "up":
            arr[int(h * 0.8):, :, :3] = schoen

    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], "root")
    m.cube("body", [-4, 12, -2], [8, 12, 4], shirt, verf=romp, fur=3, rand=0.06)
    m.bone("head", [0, 24, 0], "body")
    m.cube("head", [-4, 24, -4], [8, 8, 8], huid, verf=hoofd, fur=3, rand=0.05)
    m.bone("arm_r", [-5, 22, 0], "body")
    m.cube("arm_r", [-8, 12, -2], [4, 12, 4], shirt, verf=arm, fur=3, rand=0.06)
    m.bone("arm_l", [5, 22, 0], "body")
    m.cube("arm_l", [4, 12, -2], [4, 12, 4], shirt, verf=arm, fur=3, rand=0.06)
    m.bone("leg_r", [-1.9, 12, 0], "root")
    m.cube("leg_r", [-3.9, 0, -2], [4, 12, 4], broek, verf=been, fur=3, rand=0.06)
    m.bone("leg_l", [1.9, 12, 0], "root")
    m.cube("leg_l", [-0.1, 0, -2], [4, 12, 4], broek, verf=been, fur=3, rand=0.06)
    return m


SPELER_SCHAAL = 0.9375  # 32 modelpixels = 1,875 blok (zoals in-game)

# rijpose: benen schrijlings (naar voren + naar buiten), handen naar voren aan de teugels
RIJ_POSE = {"leg_r": {"rot": [-38, -8, 24]}, "leg_l": {"rot": [-38, 8, -24]},
            "arm_r": {"rot": [-52, 6, 4]}, "arm_l": {"rot": [-52, -6, -4]},
            "head": {"rot": [4, -12, 0]}}


def ruiter_quads(spm, mount_base, mount_W, zadel_bone, zadel_punt, pose=None, voorover=0.0):
    """Speler op het zadel: heup (pivot y=12) op zadel_punt (GeckoLib-coords in de zadel-bone).
    Geeft (quads, handpunten in wereldcoords)."""
    pose = dict(RIJ_POSE if pose is None else pose)
    zp = punt(np.eye(4), zadel_punt)
    B = mount_base @ mount_W[zadel_bone] @ T(zp) @ S(SPELER_SCHAAL) @ T((0, -12, 0))
    if voorover:
        b = dict(pose.get("body", {}))
        b["rot"] = list(np.array(b.get("rot", [0, 0, 0]), float) + [voorover, 0, 0])
        pose["body"] = b
    W = bone_mats(spm, pose)
    handen = [(B @ W["arm_" + s])[:3] @ np.r_[punt(np.eye(4), (x, 12.5, -0.0)), 1] for s, x in (("r", -6), ("l", 6))]
    return quads(spm, B, pose), handen


def balk_quads(p0, p1, w, kleur):
    """Vierkant buisje tussen twee wereldpunten (teugels, stofwolkjes-strepen)."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    a = p1 - p0
    a /= np.linalg.norm(a) + 1e-9
    h = np.array([0, 1.0, 0]) if abs(a[1]) < 0.9 else np.array([1.0, 0, 0])
    u = np.cross(a, h)
    u /= np.linalg.norm(u)
    v = np.cross(a, u)
    tex = np.ones((2, 2, 4), np.float32)
    tex[..., :3] = np.array(kleur) / 255.0
    out = []
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    hw = w / 2
    ring = [u * hw + v * hw, -u * hw + v * hw, -u * hw - v * hw, u * hw - v * hw]
    for i in range(4):
        c0, c1 = ring[i], ring[(i + 1) % 4]
        for P in ((p0 + c0, p0 + c1, p1 + c0), (p0 + c1, p0 + c0, p1 + c1)):
            out.append((np.array(P), cu, tex, np.ones(3), False))
    return out


def schaduw_quad(cx, y, cz, rx, rz, sterkte=0.42):
    n = 64
    yy, xx = np.mgrid[0:n, 0:n]
    d = ((xx + 0.5 - n / 2) / (n / 2)) ** 2 + ((yy + 0.5 - n / 2) / (n / 2)) ** 2
    tex = np.zeros((n, n, 4), np.float32)
    tex[..., :3] = (0.18, 0.12, 0.22)
    tex[..., 3] = np.clip(1 - d, 0, 1) ** 1.4 * sterkte
    P = np.array([(cx - rx, y, cz - rz), (cx + rx, y, cz - rz), (cx - rx, y, cz + rz)])
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    return [(P, cu, tex, np.ones(3), True)]


def wolkje_quads(c, r, kleur=(250, 245, 250)):
    """Stofwolkje als paar kubusjes (wereldcoords)."""
    out = []
    tex = np.ones((2, 2, 4), np.float32)
    tex[..., :3] = np.array(kleur) / 255.0
    cu = np.array([(0, 0), (1, 0), (1, 1), (0, 1)], float)
    c = np.asarray(c, float)
    f, t = c - r, c + r
    for d in ("north", "south", "east", "west", "up", "down"):
        P = np.array(face_corners(d, f, t), float)
        out.append((P, cu, tex, np.ones(3), False))
    return out


# ======================================================================================================================
# diorama + render
# ======================================================================================================================
_RES = None


def res():
    global _RES
    if _RES is None:
        r = R.Res(JARS)
        _RES = (r, R.Baker(r))
    return _RES


def diorama(bx, bz, hoogte=2, seed=7, roze=0.22, bloemen=0.10, pad=None):
    """Rond grasveldje (bx x bz blokken) met roze plekjes en bloemetjes. Geeft (vol, pal, top_y)."""
    rng = np.random.default_rng(seed)
    pal = ["minecraft:air", "minecraft:grass_block[snowy=false]", "minecraft:dirt", "minecraft:pink_wool",
           "minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:short_grass",
           "minecraft:pink_concrete_powder", "minecraft:coarse_dirt", "minecraft:lily_of_the_valley"]
    Y = hoogte + 2
    vol = np.zeros((bx, Y, bz), np.int32)
    cx, cz = (bx - 1) / 2, (bz - 1) / 2
    for x in range(bx):
        for z in range(bz):
            d = ((x - cx) / (bx / 2)) ** 2 + ((z - cz) / (bz / 2)) ** 2
            if d > 1.0:
                continue
            hh = hoogte
            vol[x, :hh - 1, z] = 2
            if hh == 1 and d > 0.55 and rng.random() < 0.5:
                pass
            top = 1
            if rng.random() < roze:
                top = 3 if rng.random() < 0.6 else 8
            vol[x, hh - 1, z] = top
            if d > 0.18 and rng.random() < bloemen and hh == hoogte:
                vol[x, hh, z] = int(rng.choice([4, 5, 6, 10]))
    return vol, pal, hoogte


def gradient(w, h, top, bot):
    t = np.linspace(0, 1, h)[:, None, None]
    a = np.array(top, float) * (1 - t) + np.array(bot, float) * t
    a = np.repeat(a, w, 1)
    return Image.fromarray(a.astype(np.uint8), "RGB").convert("RGBA")


def render_scene(vol, pal, extras, yaw=35, pitch=22, scale=110, ss=2, sky=((206, 226, 252), (255, 236, 246)),
                 margin=40, schaduw=True):
    r, bk = res()
    cam = R.Camera(yaw, pitch, scale * ss)
    im, _ = R.render(vol, pal, cam, r, bk, extras=extras, margin=margin * ss)
    im = im.resize((im.width // ss, im.height // ss), Image.LANCZOS)
    bg = gradient(im.width, im.height, *sky)
    if schaduw:
        sh = Image.new("RGBA", im.size, (70, 50, 90, 0))
        a = im.split()[3].filter(ImageFilter.GaussianBlur(16))
        sh.putalpha(a.point(lambda v: int(v * 0.25)))
        bg.paste(sh, (8, 14), sh)
    bg.paste(im, (0, 0), im)
    return bg


def font(px, vet=True):
    for n in (("comicbd.ttf" if vet else "comic.ttf"), "segoeuib.ttf", "arialbd.ttf"):
        try:
            return ImageFont.truetype(os.path.join(r"C:\Windows\Fonts", n), px)
        except OSError:
            pass
    return ImageFont.load_default()


def titel(im, tekst, sub=None, px=40):
    d = ImageDraw.Draw(im)
    f = font(px)
    d.text((24, 16), tekst, font=f, fill=(90, 40, 80), stroke_width=3, stroke_fill=(255, 255, 255))
    if sub:
        d.text((26, 22 + px), sub, font=font(int(px * 0.5), False), fill=(110, 70, 100), stroke_width=2,
               stroke_fill=(255, 255, 255))
    return im
