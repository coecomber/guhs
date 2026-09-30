"""
Piep (2.8.1) - exports the Blockbench projects in blockbench/ (hand-tweaked by the user) to the game files, the same as
Blockbench's own File > Export (Bedrock geometry + animations + the texture), without touching the .bbmodel:

  (the user's own turtles: Poepschilly = blockbench/PoepSchilly.bbmodel, Schilly = blockbench/Schilly.bbmodel; both read-only)
  pieppiepmuisje / poepschilly / boze_kaasknabbel / boze_oppernabbel (bedrock entity projects):
      assets/guhs/geckolib/models/entity/<name>.geo.json, assets/guhs/geckolib/animations/entity/<name>.animation.json,
      assets/guhs/textures/entity/<name>.png
  roze_guh_koek (java block project, the full tray of 6): assets/guhs/models/block/roze_guh_koek_6.json and its textures
      (the 1-5 models are made from it: the first n koeken, in element order, the tray elements first)

Run from the project root:  python tools/features/piep_bbexport.py [name ...]    (no names: every one newer than its export)
"""
import base64
import io
import json
import os
import sys

from PIL import Image

BB = "blockbench"
A = os.path.join("src", "main", "resources", "assets", "guhs")
ENTITIES = ("pieppiepmuisje", "poepschilly", "schilly", "boze_kaasknabbel", "boze_oppernabbel")
# the project a model comes from, when its file name differs (the user's own Poepschilly: blockbench/Schilly.bbmodel)
BRON = {"poepschilly": "PoepSchilly", "schilly": "Schilly"}


def bron(name):
    """The .bbmodel of a model (read-only: never written here)."""
    own = os.path.join(BB, f"{BRON.get(name, name)}.bbmodel")
    return own if os.path.exists(own) else os.path.join(BB, f"{name}.bbmodel")


def _r(v):
    return round(float(v), 4)


def export_entity(name):
    bb = json.load(open(bron(name), encoding="utf-8"))
    groups = {g["uuid"]: g for g in bb.get("groups", [])}
    elements = {e["uuid"]: e for e in bb.get("elements", [])}
    bones = []

    def walk(node, parent):
        if isinstance(node, str):
            return
        g = groups.get(node["uuid"]) or node
        if g.get("export") is False:
            return
        o, rot = g.get("origin", [0, 0, 0]), g.get("rotation", [0, 0, 0])
        bone = {"name": g["name"], "pivot": [_r(-o[0]), _r(o[1]), _r(o[2])]}
        if parent:
            bone["parent"] = parent
        if any(rot):
            bone["rotation"] = [_r(-rot[0]), _r(-rot[1]), _r(rot[2])]
        cubes = []
        for child in node.get("children", []):
            if isinstance(child, str) and child in elements:
                e = elements[child]
                if e.get("export") is False or e.get("type", "cube") != "cube":
                    continue
                f, t = e["from"], e["to"]
                cube = {"origin": [_r(-t[0]), _r(f[1]), _r(f[2])], "size": [_r(t[0] - f[0]), _r(t[1] - f[1]), _r(t[2] - f[2])], "uv": {}}
                for d, face in e.get("faces", {}).items():
                    if face.get("texture") is None:
                        continue
                    u0, v0, u1, v1 = face["uv"]
                    cube["uv"][d] = {"uv": [_r(u0), _r(v0)], "uv_size": [_r(u1 - u0), _r(v1 - v0)]}
                if e.get("inflate"):
                    cube["inflate"] = e["inflate"]
                erot = e.get("rotation")
                if erot and any(erot):
                    eo = e.get("origin", o)
                    cube["pivot"] = [_r(-eo[0]), _r(eo[1]), _r(eo[2])]
                    cube["rotation"] = [_r(-erot[0]), _r(-erot[1]), _r(erot[2])]
                cubes.append(cube)
        bone["cubes"] = cubes
        bones.append(bone)
        for child in node.get("children", []):
            if not isinstance(child, str):
                walk(child, g["name"])

    for node in bb["outliner"]:
        walk(node, None)
    res = bb.get("resolution", {"width": 64, "height": 64})
    vb = bb.get("visible_box", [1, 1, 0])
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": res["width"], "texture_height": res["height"],
                        "visible_bounds_width": vb[0], "visible_bounds_height": vb[1], "visible_bounds_offset": [0, vb[1] / 2, 0]},
        "bones": bones}]}
    anims = {}
    for a in bb.get("animations", []):
        out = {}
        for gid, animator in a.get("animators", {}).items():
            bname = animator.get("name") or groups.get(gid, {}).get("name")
            chans = {}
            for k in sorted(animator.get("keyframes", []), key=lambda k: float(k["time"])):
                p = k["data_points"][0]
                vals = [float(p[c]) if not isinstance(p[c], str) else float(p[c] or 0) for c in ("x", "y", "z")]
                chans.setdefault(k["channel"], {})[str(round(float(k["time"]), 4))] = vals
            if chans:
                out[bname] = chans
        loop = {"loop": True, "hold": "hold_on_last_frame"}.get(a.get("loop"), False)
        anims[a["name"]] = {"loop": loop, "animation_length": a.get("length", 1), "bones": out}
    tex = bb["textures"][0]["source"]
    img = Image.open(io.BytesIO(base64.b64decode(tex.split(",", 1)[1]))).convert("RGBA")
    return geo, {"format_version": "1.8.0", "animations": anims}, img


def export_koek():
    bb = json.load(open(os.path.join(BB, "roze_guh_koek.bbmodel"), encoding="utf-8"))
    texs = bb["textures"]
    names = [t["name"].replace(".png", "") for t in texs]
    imgs = {names[i]: Image.open(io.BytesIO(base64.b64decode(t["source"].split(",", 1)[1]))).convert("RGBA") for i, t in enumerate(texs)}
    elements = []
    for e in bb["elements"]:
        faces = {d: {"uv": f["uv"], "texture": "#" + names[f["texture"]]} for d, f in e.get("faces", {}).items() if f.get("texture") is not None}
        el = {"from": e["from"], "to": e["to"], "faces": faces}
        if e.get("rotation") and any(e["rotation"]):
            axis = "xyz"[[i for i, v in enumerate(e["rotation"]) if v][0]]
            el["rotation"] = {"angle": [v for v in e["rotation"] if v][0], "axis": axis, "origin": e.get("origin", [8, 8, 8])}
        elements.append({"_name": e.get("name", ""), **el})
    return elements, imgs


def main(names):
    w = lambda p, o: (os.makedirs(os.path.dirname(p), exist_ok=True), json.dump(o, open(p, "w", encoding="utf-8"), indent=2))
    for name in names:
        if name == "roze_guh_koek":
            elements, imgs = export_koek()
            tray = [e for e in elements if e["_name"] == "bakje"]
            koeken = [e for e in elements if e["_name"] != "bakje"]
            per = max(1, len(koeken) // 6)
            textures = {"particle": "guhs:block/roze_guh_koek_top", **{k: f"guhs:block/roze_guh_koek_{k}" for k in imgs}}
            for n in range(1, 7):
                els = [{k: v for k, v in e.items() if k != "_name"} for e in tray + koeken[:per * n]]
                w(os.path.join(A, "models", "block", f"roze_guh_koek_{n}.json"),
                  {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": textures, "elements": els})
            for k, img in imgs.items():
                img.save(os.path.join(A, "textures", "block", f"roze_guh_koek_{k}.png"))
        else:
            geo, anims, img = export_entity(name)
            w(os.path.join(A, "geckolib", "models", "entity", f"{name}.geo.json"), geo)
            w(os.path.join(A, "geckolib", "animations", "entity", f"{name}.animation.json"), anims)
            img.save(os.path.join(A, "textures", "entity", f"{name}.png"))
        print("exported", name)


def newer():
    out = []
    for name in ENTITIES + ("roze_guh_koek",):
        src = bron(name)
        dst = os.path.join(A, "models", "block", "roze_guh_koek_6.json") if name == "roze_guh_koek" else os.path.join(A, "geckolib", "models", "entity", f"{name}.geo.json")
        if os.path.exists(src) and (not os.path.exists(dst) or os.path.getmtime(src) > os.path.getmtime(dst)):
            out.append(name)
    return out


if __name__ == "__main__":
    main(sys.argv[1:] or newer())
