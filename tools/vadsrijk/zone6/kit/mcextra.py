"""Extra modellen voor mcrender: BER-blokken (guhhuisjes, guhrad, bankguh) en GeckoLib-guhs."""
import json, math
import numpy as np
from mcrender import face_corners, default_uv, elem_rotate, parse_state, DIRS


def affine(scale=1.0, rot_y_deg=0.0, translate=(0, 0, 0), pre=(0, 0, 0), scale_xyz=None):
    """Matrix: p -> T + Ry(rot) * S * (p + pre). rot_y_deg volgt MC Axis.YP (tegen de klok in van boven)."""
    s = np.diag(scale_xyz if scale_xyz is not None else (scale, scale, scale))
    a = math.radians(rot_y_deg)
    c, si = math.cos(a), math.sin(a)
    r = np.array([[c, 0, si], [0, 1, 0], [-si, 0, c]])
    M = r @ s
    t = np.asarray(translate, float) + M @ np.asarray(pre, float)
    return M, t


def apply(Mt, p):
    M, t = Mt
    return p @ M.T + t


def model_quads(baker, model_ref, Mt, tint=(1, 1, 1)):
    elements, textures = baker.res.model(model_ref)
    out = []
    for el in elements or []:
        f = np.array(el["from"], float)
        t = np.array(el["to"], float)
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
            uv = fc.get("uv") or default_uv(d, f, t)
            u0, v0, u1, v1 = uv
            cu = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            k = (fc.get("rotation", 0) // 90) % 4
            cu = [cu[(i + k) % 4] for i in range(4)]
            texa = baker.res.texture(tex)
            al = texa[..., 3]
            semi = bool(((al > 0.05) & (al < 0.95)).mean() > 0.2)
            out.append((apply(Mt, c / 16.0), np.array(cu, float) / 16.0, tex, tint, semi))
    return out


GEO_SKIP = ("outfit", "armor", "saddle", "teckel", "ender", "koning", "wolk", "zeemeer", "pinguh", "balto",
            "mewtwo", "stitch", "neck", "pluis_", "asguh", "glans")


def geo_quads(res, geo_path, tex_path, Mt, tint=(1, 1, 1), skip=GEO_SKIP):
    """GeckoLib/Bedrock-geometrie in rustpose. Coordinaten in modelpixels, x gespiegeld zoals GeckoLib."""
    g = json.loads(res.read(geo_path))["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    texa = res.texture(tex_path)
    out = []
    for b in g["bones"]:
        if any(s in b["name"] for s in skip):
            continue
        for cube in b.get("cubes", []):
            o = np.array(cube["origin"], float)
            sz = np.array(cube["size"], float)
            inf = cube.get("inflate", 0)
            f = np.array([-(o[0] + sz[0]), o[1], o[2]]) - inf
            t = np.array([-o[0], o[1] + sz[1], o[2] + sz[2]]) + inf
            uvs = cube.get("uv")
            faces = {}
            if isinstance(uvs, dict):
                for d, fu in uvs.items():
                    u, v = fu["uv"]
                    w, h = fu.get("uv_size", [0, 0])
                    faces[d] = (u, v, u + w, v + h)
            elif isinstance(uvs, list):
                u, v = uvs
                dx, dy, dz = sz
                faces = {"north": (u + dz, v + dz, u + dz + dx, v + dz + dy),
                         "east": (u, v + dz, u + dz, v + dz + dy),
                         "south": (u + dz + dx + dz, v + dz, u + 2 * dz + 2 * dx, v + dz + dy),
                         "west": (u + dz + dx, v + dz, u + 2 * dz + dx, v + dz + dy),
                         "up": (u + dz, v, u + dz + dx, v + dz),
                         "down": (u + dz + dx, v + dz, u + dz + 2 * dx, v)}
            rot = cube.get("rotation")
            piv = cube.get("pivot")
            for d, (u0, v0, u1, v1) in faces.items():
                c = np.array(face_corners(d, f, t), float)
                if rot and piv:
                    pv = np.array([-piv[0], piv[1], piv[2]], float)
                    for ax, ang in (("z", rot[2]), ("y", -rot[1]), ("x", -rot[0])):
                        if ang:
                            c = elem_rotate(c, {"origin": pv, "axis": ax, "angle": ang})
                cu = np.array([(u0 / tw, v0 / th), (u1 / tw, v0 / th), (u1 / tw, v1 / th), (u0 / tw, v1 / th)])
                out.append((apply(Mt, c / 16.0), cu, texa, tint, False))
    return out


FACING_YROT = {"south": 0, "west": 90, "north": 180, "east": 270}


def mc_rot(facing):
    """De -toYRot()+180 uit de renderers."""
    return -FACING_YROT.get(facing, 180) + 180.0


def special_extras(vol, palette, baker):
    """BER-blokken in het volume vervangen door hun echte modellen. Geeft (extras, verborgen states)."""
    res = baker.res
    extras, hide = [], set()
    for i, s in enumerate(palette):
        n, props = parse_state(s)
        if n in ("guhs:guhhuisje_deel", "guhs:guh_wheel_part"):
            hide.add(s)
        if n not in ("guhs:guhhuisje_groot", "guhs:guhhuisje_medium", "guhs:guhhuisje_klein", "guhs:guh_wheel",
                     "guhs:bank_guh"):
            continue
        hide.add(s)
        for (x, y, z) in np.argwhere(vol == i):
            fac = props.get("facing", "north")
            if n.startswith("guhs:guhhuisje_"):
                maat = n.split("_")[-1]
                w, sc = {"klein": (2, 1.0), "medium": (3, 1.5), "groot": (4, 2.0)}[maat]
                zij, diep = (w - 1) / 2.0 - (w - 1) // 2, (w - 1) / 2.0
                fv = np.array(DIRS[fac], float)
                langs = np.array([-fv[2], 0, fv[0]])  # clockwise
                mid = np.array([x + 0.5, y, z + 0.5]) + langs * zij - fv * diep
                Mt = affine(sc, mc_rot(fac), mid, (-0.5, 0, -0.5))
                extras += model_quads(baker, f"guhs:block/guhhuisje_{maat}_model", Mt)
            elif n == "guhs:guh_wheel":
                Mt = affine(2.0, mc_rot(fac), (x + 0.5, y, z + 0.5), (-0.5, 0, -0.5))
                extras += model_quads(baker, "guhs:block/guh_wheel_stand", Mt)
                extras += model_quads(baker, "guhs:block/guh_wheel_ring", Mt)
            elif n == "guhs:bank_guh":
                Mt = affine(1.0, mc_rot(fac), (x + 0.5, y, z + 0.5), scale_xyz=(1.35, 1.05, 1.35))
                extras += geo_quads(res, "assets/guhs/geckolib/models/entity/guh_sitting.geo.json",
                                    "guhs:entity/guh_sitting", Mt)
    return extras, hide


def guh(res, x, y, z, yaw=0, skin="guh", scale=1.0, sitting=False):
    """Een guh-figuurtje (x,y,z in volume-blokcoordinaten, voeten op y, yaw 0 = kijkt naar het noorden)."""
    Mt = affine(scale, yaw, (x, y, z))
    if sitting:
        return geo_quads(res, "assets/guhs/geckolib/models/entity/guh_sitting.geo.json", f"guhs:entity/{skin}", Mt)
    return geo_quads(res, "assets/guhs/geckolib/models/entity/guh.geo.json", f"guhs:entity/{skin}", Mt)
