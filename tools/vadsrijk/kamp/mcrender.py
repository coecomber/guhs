"""Kleine software-renderer voor Minecraft-blokvolumes (blockstate + JSON-modellen + textures uit jars).

Orthografische camera, z-buffer, cutout + translucent pass, supersampling.
"""
import io, json, math, zipfile, functools, os
import numpy as np
from PIL import Image

APPDATA = os.environ.get("APPDATA", "")
HERE = os.path.dirname(os.path.abspath(__file__))
JARS = [os.path.join(HERE, "guhs-1.2.4.jar"),
        os.path.join(APPDATA, "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:light", "minecraft:barrier",
       "minecraft:structure_void"}
TRANSLUCENT_HINT = ("stained_glass", "water", "ice", "slime", "honey", "kaas_saus", "eenrichtingsglas", "tankwand",
                    "roze_slijmblok", "knuffelbad_schuim")
GRASS = (124, 189, 107)
FOLIAGE = (89, 168, 64)
WATER = (63, 118, 228)


class Res:
    def __init__(self, jars=JARS):
        self.zips = [zipfile.ZipFile(j) for j in jars]
        self.names = [set(z.namelist()) for z in self.zips]

    def read(self, path):
        for z, n in zip(self.zips, self.names):
            if path in n:
                return z.read(path)
        return None

    def json(self, ns_path, kind):
        ns, p = ns_path.split(":", 1) if ":" in ns_path else ("minecraft", ns_path)
        d = self.read(f"assets/{ns}/{kind}/{p}.json")
        return json.loads(d) if d else None

    @functools.lru_cache(maxsize=None)
    def texture(self, ref):
        ns, p = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        d = self.read(f"assets/{ns}/textures/{p}.png")
        if d is None:
            im = Image.new("RGBA", (16, 16), (255, 0, 255, 255))
        else:
            im = Image.open(io.BytesIO(d)).convert("RGBA")
            if im.height > im.width:  # animatie: eerste frame
                im = im.crop((0, 0, im.width, im.width))
        a = np.asarray(im).astype(np.float32) / 255.0
        return a

    @functools.lru_cache(maxsize=None)
    def model(self, ref):
        """Geeft (elements, textures) met parent-keten opgelost."""
        if ref.startswith("builtin/") or ref.startswith("minecraft:builtin/"):
            return None, {}
        m = self.json(ref, "models")
        if m is None:
            return None, {}
        elements, textures = None, {}
        if "parent" in m:
            elements, textures = self.model(m["parent"])
            textures = dict(textures)
        textures.update(m.get("textures", {}))
        if "elements" in m:
            elements = m["elements"]
        return elements, textures


def parse_state(s):
    if "[" in s:
        n, p = s[:-1].split("[", 1)
        props = dict(kv.split("=", 1) for kv in p.split(",") if kv)
    else:
        n, props = s, {}
    return n, props


def _match_variant(key, props):
    if key == "":
        return True
    for kv in key.split(","):
        k, v = kv.split("=", 1)
        if props.get(k) != v:
            return False
    return True


def _match_when(when, props):
    if "OR" in when:
        return any(_match_when(w, props) for w in when["OR"])
    if "AND" in when:
        return all(_match_when(w, props) for w in when["AND"])
    for k, v in when.items():
        if str(props.get(k, "")).lower() not in str(v).lower().split("|"):
            return False
    return True


def rot_y(p, deg, c=8.0):
    x, y, z = p[..., 0] - c, p[..., 1], p[..., 2] - c
    for _ in range((deg // 90) % 4):
        x, z = -z, x
    return np.stack([x + c, y, z + c], -1)


def rot_x(p, deg, c=8.0):
    x, y, z = p[..., 0], p[..., 1] - c, p[..., 2] - c
    for _ in range((deg // 90) % 4):
        y, z = z, -y
    return np.stack([x, y + c, z + c], -1)


DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0),
        "down": (0, -1, 0)}


def face_corners(d, f, t):
    """P00, P10, P01 (u-as, v-as) per vlak, in modelcoördinaten (0..16)."""
    fx, fy, fz = f
    tx, ty, tz = t
    if d == "north": return (tx, ty, fz), (fx, ty, fz), (tx, fy, fz)
    if d == "south": return (fx, ty, tz), (tx, ty, tz), (fx, fy, tz)
    if d == "west": return (fx, ty, fz), (fx, ty, tz), (fx, fy, fz)
    if d == "east": return (tx, ty, tz), (tx, ty, fz), (tx, fy, tz)
    if d == "up": return (fx, ty, fz), (tx, ty, fz), (fx, ty, tz)
    if d == "down": return (fx, fy, tz), (tx, fy, tz), (fx, fy, fz)


def default_uv(d, f, t):
    fx, fy, fz = f
    tx, ty, tz = t
    return {"down": [fx, 16 - tz, tx, 16 - fz], "up": [fx, fz, tx, tz],
            "north": [16 - tx, 16 - ty, 16 - fx, 16 - fy], "south": [fx, 16 - ty, tx, 16 - fy],
            "west": [fz, 16 - ty, tz, 16 - fy], "east": [16 - tz, 16 - ty, 16 - fz, 16 - fy]}[d]


def elem_rotate(pts, r):
    if not r:
        return pts
    o = np.array(r.get("origin", [8, 8, 8]), float)
    a = math.radians(r.get("angle", 0))
    ax = r.get("axis", "y")
    p = pts - o
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[..., 0], p[..., 1], p[..., 2]
    if ax == "x":
        y, z = y * c - z * s, y * s + z * c
    elif ax == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    q = np.stack([x, y, z], -1)
    if r.get("rescale"):
        k = 1 / max(abs(c), 1e-6)
        sc = np.array([k if ax != "x" else 1, k if ax != "y" else 1, k if ax != "z" else 1])
        q = q * sc
    return q + o


class Faces:
    """Gebakken vlakken van één blockstate (in blokcoördinaten 0..1)."""

    def __init__(self):
        self.quads = []  # (P00,P10,P01 [3x3], uvcorners 4x2, tex key, cullface dir or None, tint, translucent)
        self.full_opaque = False


class Baker:
    def __init__(self, res):
        self.res = res
        self.cache = {}

    def tint_for(self, name):
        n = name.split(":")[1]
        if name.startswith("minecraft:"):
            if n in ("grass_block", "short_grass", "tall_grass", "fern", "large_fern", "sugar_cane", "potted_fern"):
                return GRASS
            if n.endswith("leaves") and n not in ("cherry_leaves", "azalea_leaves", "flowering_azalea_leaves", "pale_oak_leaves"):
                return {"spruce_leaves": (97, 153, 97), "birch_leaves": (128, 167, 85)}.get(n, FOLIAGE)
            if n in ("vine", "lily_pad"):
                return FOLIAGE
            if n in ("water", "water_cauldron"):
                return WATER
        return (255, 255, 255)

    def models_for(self, state):
        name, props = parse_state(state)
        bs = self.res.json(name, "blockstates")
        if bs is None:
            return name, props, []
        out = []
        if "variants" in bs:
            for k, v in bs["variants"].items():
                if _match_variant(k, props):
                    out.append(v[0] if isinstance(v, list) else v)
                    break
            else:
                v = next(iter(bs["variants"].values()))
                out.append(v[0] if isinstance(v, list) else v)
        else:
            for part in bs.get("multipart", []):
                if "when" not in part or _match_when(part["when"], props):
                    a = part["apply"]
                    out.append(a[0] if isinstance(a, list) else a)
        return name, props, out

    def bake(self, state):
        if state in self.cache:
            return self.cache[state]
        F = Faces()
        name, props, models = self.models_for(state)
        tint_col = self.tint_for(name)
        trans_block = any(h in name for h in TRANSLUCENT_HINT)
        n_elems = 0
        full_cube_candidate = True
        for mv in models:
            elements, textures = self.res.model(mv["model"])
            if not elements:
                continue
            for el in elements:
                n_elems += 1
                f = np.array(el["from"], float)
                t = np.array(el["to"], float)
                if not (np.allclose(f, 0) and np.allclose(t, 16)) or el.get("rotation"):
                    full_cube_candidate = False
                for d, fc in el.get("faces", {}).items():
                    tex = fc.get("texture", "")
                    for _ in range(8):
                        if isinstance(tex, dict):
                            tex = tex.get("sprite", "")
                        if tex.startswith("#"):
                            tex = textures.get(tex[1:], "")
                        else:
                            break
                    if isinstance(tex, dict):
                        tex = tex.get("sprite", "")
                    if not tex or tex.startswith("#"):
                        continue
                    if ":" not in tex:
                        tex = "minecraft:" + tex
                    c = np.array(face_corners(d, f, t), float)
                    c = elem_rotate(c, el.get("rotation"))
                    c = rot_x(c, mv.get("x", 0))
                    c = rot_y(c, mv.get("y", 0))
                    uv = fc.get("uv") or default_uv(d, f, t)
                    u0, v0, u1, v1 = uv
                    cu = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                    k = (fc.get("rotation", 0) // 90) % 4
                    cu = [cu[(i + k) % 4] for i in range(4)]
                    cf = fc.get("cullface")
                    if cf:
                        dv = np.array([[8 + 8 * DIRS[cf][0], 8 + 8 * DIRS[cf][1], 8 + 8 * DIRS[cf][2]]], float)
                        dv = rot_y(rot_x(dv, mv.get("x", 0)), mv.get("y", 0))[0] - 8
                        cf = tuple(int(round(x / 8)) for x in dv)
                    tint = tint_col if "tintindex" in fc else (255, 255, 255)
                    if name == "minecraft:water":
                        tint = WATER
                    texa = self.res.texture(tex)
                    al = texa[..., 3]
                    semi = ((al > 0.05) & (al < 0.95)).mean() > 0.2
                    F.quads.append((c / 16.0, np.array(cu, float) / 16.0, tex, cf, np.array(tint) / 255.0,
                                    trans_block or semi))
                    if (al < 0.99).any():
                        full_cube_candidate = False
        F.full_opaque = full_cube_candidate and n_elems == 1 and not trans_block
        if not F.quads:
            F = self.fallback(name, props)
        self.cache[state] = F
        return F

    def fallback(self, name, props):
        """Entity-gerenderde blokken (kisten, bedden, water) als eenvoudige doos."""
        F = Faces()
        n = name.split(":")[1]
        box, tex, h_tint, trans = None, None, (1, 1, 1), False
        if "chest" in n:
            box, tex = ((1, 0, 1), (15, 14, 15)), "minecraft:block/oak_planks"
            h_tint = (0.85, 0.7, 0.5)
        elif n.endswith("_bed"):
            col = n[:-4]
            box, tex = ((0, 0, 0), (16, 9, 16)), f"minecraft:block/{col}_wool"
        elif n == "water":
            lvl = int(props.get("level", 0))
            box, tex, h_tint, trans = ((0, 0, 0), (16, 14 if lvl == 0 else max(2, 14 - 2 * lvl), 16)), \
                "minecraft:block/water_still", np.array(WATER) / 255, True
        elif n in ("kaas_saus", "kaasfrituursaus") or n.endswith("_saus"):
            lvl = int(props.get("level", 0))
            hgt = 14 if lvl in (0, 8) else max(2, 14 - 2 * lvl)
            box, tex, h_tint, trans = ((0, 0, 0), (16, hgt, 16)), f"guhs:block/{n}_still", (1, 1, 1), False
        elif n == "lava":
            box, tex = ((0, 0, 0), (16, 14, 16)), "minecraft:block/lava_still"
        elif "sign" in n or "banner" in n or "head" in n or "skull" in n:
            return F
        else:
            box, tex, h_tint = ((2, 0, 2), (14, 12, 14)), "minecraft:block/pink_wool", (1, 0.6, 0.9)
        f, t = np.array(box[0], float), np.array(box[1], float)
        for d in DIRS:
            c = np.array(face_corners(d, f, t), float)
            u0, v0, u1, v1 = default_uv(d, f, t)
            cu = np.array([(u0, v0), (u1, v0), (u1, v1), (u0, v1)], float)
            F.quads.append((c / 16.0, cu / 16.0, tex, None, np.array(h_tint, float), trans))
        return F


class Camera:
    def __init__(self, yaw_deg=45, pitch_deg=30, scale=24):
        th, ph = math.radians(yaw_deg), math.radians(pitch_deg)
        # richting van de scène naar de camera
        self.d = np.array([math.cos(ph) * math.sin(th), math.sin(ph), math.cos(ph) * math.cos(th)])
        self.right = np.array([math.cos(th), 0.0, -math.sin(th)])
        self.up = np.cross(self.d, self.right)
        self.up /= np.linalg.norm(self.up)
        self.scale = scale

    def project(self, p):
        p = np.asarray(p, float)
        sx = p @ self.right * self.scale
        sy = -(p @ self.up) * self.scale
        dz = p @ self.d
        return sx, sy, dz


LIGHT = np.array([0.35, 1.0, 0.55])
LIGHT /= np.linalg.norm(LIGHT)


def shade(normal):
    n = normal / (np.linalg.norm(normal) + 1e-9)
    w = np.abs(n)
    mc = (w[1] * (1.0 if n[1] > 0 else 0.5) + w[0] * 0.62 + w[2] * 0.8) / w.sum()
    l = max(0.0, float(n @ LIGHT))
    return 0.6 * mc + 0.4 * (0.6 + 0.4 * l)


def render(vol, palette, cam, res=None, baker=None, bg=None, margin=40, emissive=(), ao=True,
           hide=lambda s: False, extras=()):
    """vol[x,y,z] = paletindex. Geeft PIL-afbeelding (RGBA)."""
    res = res or Res()
    baker = baker or Baker(res)
    X, Y, Z = vol.shape
    states = palette
    baked = [None] * len(states)
    air = np.zeros(len(states), bool)
    opaque = np.zeros(len(states), bool)
    for i, s in enumerate(states):
        nm = s.split("[")[0]
        if nm in AIR or hide(s):
            air[i] = True
            continue
        baked[i] = baker.bake(s)
        opaque[i] = baked[i].full_opaque
    occ = opaque[vol]
    solid_any = ~air[vol]
    idxs = np.argwhere(solid_any)
    # schermgrenzen
    corners = np.array([[x, y, z] for x in (0, X) for y in (0, Y) for z in (0, Z)], float)
    sx, sy, _ = cam.project(corners)
    # strakker: alleen gebruikte blokken
    if len(idxs):
        lo, hi = idxs.min(0), idxs.max(0) + 1
        corners = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])], float)
        sx, sy, _ = cam.project(corners)
    if len(extras):
        ep = np.concatenate([e[0] for e in extras])
        ex, ey, _ = cam.project(ep)
        sx, sy = np.concatenate([sx, ex]), np.concatenate([sy, ey])
    ox, oy = -sx.min() + margin, -sy.min() + margin
    W, H = int(sx.max() - sx.min() + 2 * margin), int(sy.max() - sy.min() + 2 * margin)
    img = np.zeros((H, W, 3), np.float32)
    if bg is not None:
        img[:] = bg
    alpha = np.zeros((H, W), np.float32)
    zb = np.full((H, W), -1e9, np.float32)
    names = [s.split("[")[0] for s in states]
    trans_list = []

    def raster(P, cu, texa, tint, sh, translucent, bias):
        sxp, syp, dz = cam.project(P)
        sxp = sxp + ox
        syp = syp + oy
        e1 = np.array([sxp[1] - sxp[0], syp[1] - syp[0]])
        e2 = np.array([sxp[2] - sxp[0], syp[2] - syp[0]])
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-3:
            return
        x0 = int(max(0, math.floor(min(sxp[0], sxp[1], sxp[2], sxp[1] + e2[0]))))
        x1 = int(min(W - 1, math.ceil(max(sxp[0], sxp[1], sxp[2], sxp[1] + e2[0]))))
        y0 = int(max(0, math.floor(min(syp[0], syp[1], syp[2], syp[1] + e2[1]))))
        y1 = int(min(H - 1, math.ceil(max(syp[0], syp[1], syp[2], syp[1] + e2[1]))))
        if x1 < x0 or y1 < y0:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5 - sxp[0], np.arange(y0, y1 + 1) + 0.5 - syp[0])
        a = (gx * e2[1] - gy * e2[0]) / det
        b = (gy * e1[0] - gx * e1[1]) / det
        m = (a >= -1e-4) & (a <= 1 + 1e-4) & (b >= -1e-4) & (b <= 1 + 1e-4)
        if not m.any():
            return
        depth = dz[0] + a * (dz[1] - dz[0]) + b * (dz[2] - dz[0]) + bias
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        m &= depth > sub
        if not m.any():
            return
        uv = cu[0] + a[..., None] * (cu[1] - cu[0]) + b[..., None] * (cu[3] - cu[0])
        th, tw = texa.shape[:2]
        tu = np.clip((uv[..., 0] * tw).astype(int), 0, tw - 1)
        tv = np.clip((uv[..., 1] * th).astype(int), 0, th - 1)
        px = texa[tv, tu]
        al = px[..., 3]
        col = px[..., :3] * tint * sh
        if translucent:
            m &= al > 0.02
            if not m.any():
                return
            ia = img[y0:y1 + 1, x0:x1 + 1]
            aa = al[..., None]
            ia[m] = (ia * (1 - aa) + col * aa)[m]
            alpha[y0:y1 + 1, x0:x1 + 1][m] = np.maximum(alpha[y0:y1 + 1, x0:x1 + 1][m], al[m])
        else:
            m &= al > 0.5
            img[y0:y1 + 1, x0:x1 + 1][m] = col[m]
            sub[m] = depth[m]
            alpha[y0:y1 + 1, x0:x1 + 1][m] = 1.0

    # eenvoudige omgevingsocclusie: hoeveel lucht boven een blok
    sky = np.zeros_like(occ)
    if ao:
        sky = np.flip(np.cumsum(np.flip(occ, 1), 1), 1) > 0  # er staat iets dichts boven of op dit niveau
    for (x, y, z) in idxs:
        si = vol[x, y, z]
        F = baked[si]
        if F is None:
            continue
        base = np.array([x, y, z], float)
        for k, (P, cu, tex, cf, tint, tr) in enumerate(F.quads):
            if cf is not None:
                nx, ny, nz = x + cf[0], y + cf[1], z + cf[2]
                if 0 <= nx < X and 0 <= ny < Y and 0 <= nz < Z:
                    nb = vol[nx, ny, nz]
                    if occ[nx, ny, nz] or (nb == si and (tr or not F.full_opaque)) or \
                            (tr and names[nb] == names[si]):
                        continue
            Pw = P + base
            nrm = np.cross(Pw[2] - Pw[0], Pw[1] - Pw[0])
            if np.linalg.norm(nrm) < 1e-9:
                continue
            if nrm @ cam.d < 0:
                # achterkant; enkelzijdig zoals in MC, behalve bij dunne platte vlakken
                if F.full_opaque or cf is not None:
                    continue
                nrm = -nrm
            sh = shade(nrm)
            if ao and y + 1 < Y and nrm[1] < 0.5 and sky[x, y + 1, z]:
                sh *= 0.86
            if names[si] in emissive:
                sh = 1.08
            texa = res.texture(tex)
            if tr:
                sxp, syp, dz = cam.project(Pw.mean(0))
                trans_list.append((float(dz), Pw, cu, texa, tint, sh))
            else:
                raster(Pw, cu, texa, tint, sh, False, k * 1e-4)
    for k, (Pw, cu, tex, tint, tr) in enumerate(extras):
        nrm = np.cross(Pw[2] - Pw[0], Pw[1] - Pw[0])
        if np.linalg.norm(nrm) < 1e-9:
            continue
        if nrm @ cam.d < 0:
            continue
        sh = shade(nrm)
        texa = res.texture(tex) if isinstance(tex, str) else tex
        if tr:
            trans_list.append((float(cam.project(Pw.mean(0))[2]), Pw, cu, texa, np.asarray(tint, float), sh))
        else:
            raster(Pw, cu, texa, np.asarray(tint, float), sh, False, 2e-3)
    trans_list.sort(key=lambda t: t[0])
    for dz, Pw, cu, texa, tint, sh in trans_list:
        raster(Pw, cu, texa, tint, sh, True, 0)
    out = np.dstack([np.clip(img, 0, 1), alpha])
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA"), (ox, oy)
