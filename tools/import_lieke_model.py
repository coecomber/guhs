"""
One-off import of Lieke's improved guh model ("Guhs Lieke" folder) into the mod:
  - copies her model + texture over assets/guhs/geo/entity/guh.geo.json and textures/entity/guh.png
  - adds the `saddle` bone the game needs (hidden unless the guh wears a saddle) and paints the leather into a
    free part of her texture
  - writes her Blockbench project to blockbench/guh.bbmodel (with the saddle too), pointing at the game texture,
    so from now on you can edit the guh in Blockbench and export straight into the mod.

Run from the project root:  python tools/import_lieke_model.py
"""
import base64
import copy
import io
import json
import os
import uuid

from PIL import Image, ImageDraw

SRC = os.path.join("..", "Guhs Lieke")
ASSETS = os.path.join("src", "main", "resources", "assets", "guhs")
UV = 128          # UV space of the model
PX = 4            # texture pixels per UV unit (512 png)
LEATHER = (140, 86, 46, 255)
LEATHER_DARK = (96, 56, 30, 255)

geo_file = json.load(open(os.path.join(SRC, "guh.geo.json"), encoding="utf-8"))
geo = geo_file["minecraft:geometry"][0]
texture = Image.open(os.path.join(SRC, "Guh texture lieke.png")).convert("RGBA")
bb = json.load(open(os.path.join(SRC, "guh1.geo.bbmodel"), encoding="utf-8"))

# --- saddle cubes, sitting on top of Lieke's body (its top is at y=11) ---------------------------------------------
SADDLE = [  # origin, size, colour
    ([-4, 11, 0.5], [8, 1, 7], LEATHER),
    ([-3, 11.5, 0], [6, 1.5, 1], LEATHER_DARK),
    ([-3.5, 11.5, 7], [7, 2, 1], LEATHER_DARK),
    ([6.5, 3, 3], [0.5, 8.5, 2], LEATHER_DARK),
    ([-7, 3, 3], [0.5, 8.5, 2], LEATHER_DARK),
]
PIVOT = [0, 11, 4]

# --- guh armour: a small helmet cap + side plates, one bone per tier (only the worn tier is shown) ---------------
ARMOR_TIERS = {  # tier: (main colour, highlight)
    "iron": ((206, 208, 214, 255), (240, 242, 246, 255)),
    "diamond": ((74, 214, 204, 255), (170, 250, 240, 255)),
    "netherite": ((72, 62, 68, 255), (120, 104, 112, 255)),
}
ARMOR_CUBES = [  # origin, size, use highlight colour
    ([-5.5, 15, -10], [11, 1.5, 8], True),      # helmet cap on top of the head
    ([-7.25, 12.5, -12.5], [14.5, 1, 0.5], False),  # brim across the forehead, above the eyes
    ([6.6, 3.5, 5.5], [0.6, 5.5, 5], False),     # plate on the left flank
    ([-7.2, 3.5, 5.5], [0.6, 5.5, 5], False),    # plate on the right flank
]

# --- find free room in the UV sheet ----------------------------------------------------------------------------------
used = [[False] * UV for _ in range(UV)]
for bone in geo["bones"]:
    for cube in bone.get("cubes", []):
        for face in cube.get("uv", {}).values():
            (u, v), (w, h) = face["uv"], face["uv_size"]
            for x in range(int(min(u, u + w)), int(max(u, u + w) + 0.999)):
                for y in range(int(min(v, v + h)), int(max(v, v + h) + 0.999)):
                    if 0 <= x < UV and 0 <= y < UV:
                        used[y][x] = True


def free_spot(w, h):
    for y in range(UV - h + 1):
        for x in range(UV - w + 1):
            if all(not used[yy][xx] for yy in range(y, y + h) for xx in range(x, x + w)):
                for yy in range(y, y + h):
                    for xx in range(x, x + w):
                        used[yy][xx] = True
                return x, y
    raise RuntimeError("no free texture space for the saddle")


def face_sizes(size):
    w, h, d = size
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}


draw = ImageDraw.Draw(texture)
saddle_cubes = []
for origin, size, colour in SADDLE:
    uv = {}
    for direction, (fw, fh) in face_sizes(size).items():
        cw, ch = max(1, int(fw + 0.999)), max(1, int(fh + 0.999))
        u, v = free_spot(cw, ch)
        uv[direction] = {"uv": [u, v], "uv_size": [fw, fh]}
        draw.rectangle([u * PX, v * PX, (u + cw) * PX - 1, (v + ch) * PX - 1], fill=colour)
    saddle_cubes.append({"origin": origin, "size": size, "uv": uv})

armor_bones = {}
for tier, (main, light) in ARMOR_TIERS.items():
    cubes = []
    for origin, size, highlight in ARMOR_CUBES:
        uv = {}
        for direction, (fw, fh) in face_sizes(size).items():
            cw, ch = max(1, int(fw + 0.999)), max(1, int(fh + 0.999))
            u, v = free_spot(cw, ch)
            uv[direction] = {"uv": [u, v], "uv_size": [fw, fh]}
            draw.rectangle([u * PX, v * PX, (u + cw) * PX - 1, (v + ch) * PX - 1], fill=light if highlight and direction == "up" else main)
        cubes.append({"origin": origin, "size": size, "uv": uv})
    armor_bones[tier] = cubes

geo["bones"] = [b for b in geo["bones"] if b["name"] != "saddle" and not b["name"].startswith("armor_")]
geo["bones"].append({"name": "saddle", "parent": "body", "pivot": PIVOT, "cubes": saddle_cubes})
for tier, cubes in armor_bones.items():
    # helmet + brim move with the head, the flank plates with the body
    geo["bones"].append({"name": f"armor_{tier}", "parent": "head", "pivot": [0, 6, -2], "cubes": cubes[:2]})
    geo["bones"].append({"name": f"armor_{tier}_body", "parent": "body", "pivot": [0, 6, 6], "cubes": cubes[2:]})

with open(os.path.join(ASSETS, "geo", "entity", "guh.geo.json"), "w", encoding="utf-8") as f:
    json.dump(geo_file, f, indent=2)
texture.save(os.path.join(ASSETS, "textures", "entity", "guh.png"))

# --- the Blockbench project: add the saddle group + cubes, embed the new texture, point it at the game texture --------
body_group = next(g for g in bb["groups"] if g["name"] == "body")
group_uuid = str(uuid.uuid4())
group = copy.deepcopy(body_group)
group.update({"name": "saddle", "uuid": group_uuid, "origin": [-PIVOT[0], PIVOT[1], PIVOT[2]], "rotation": [0, 0, 0],
              "children": []})
bb["groups"] = [g for g in bb["groups"] if g["name"] != "saddle"] + [group]
template = next(e for e in bb["elements"] if e.get("type") == "cube")
new_uuids = []
for cube in saddle_cubes:
    (ox, oy, oz), (sx, sy, sz) = cube["origin"], cube["size"]
    element = copy.deepcopy(template)
    eid = str(uuid.uuid4())
    element.update({
        "name": "saddle", "uuid": eid,
        # Blockbench stores bedrock cubes with X mirrored compared to the .geo.json
        "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz],
        "origin": [-PIVOT[0], PIVOT[1], PIVOT[2]],
        "faces": {d: {"uv": [f["uv"][0], f["uv"][1], f["uv"][0] + f["uv_size"][0], f["uv"][1] + f["uv_size"][1]], "texture": 0}
                  for d, f in cube["uv"].items()},
    })
    element.pop("rotation", None)
    bb["elements"].append(element)
    new_uuids.append(eid)


def add_to_body(nodes):
    for node in nodes:
        if isinstance(node, dict):
            if node.get("uuid") == body_group["uuid"]:
                node["children"].append({"uuid": group_uuid, "isOpen": False, "children": new_uuids})
                return True
            if add_to_body(node.get("children", [])):
                return True
    return False


assert add_to_body(bb["outliner"]), "body group not found in outliner"

head_group = next(g for g in bb["groups"] if g["name"] == "head")
for tier, cubes in armor_bones.items():
    gid = str(uuid.uuid4())
    group = copy.deepcopy(head_group)
    group.update({"name": f"armor_{tier}", "uuid": gid, "origin": [0, 6, -2], "rotation": [0, 0, 0], "children": []})
    bb["groups"].append(group)
    ids = []
    for cube in cubes:
        (ox, oy, oz), (sx, sy, sz) = cube["origin"], cube["size"]
        element = copy.deepcopy(template)
        eid = str(uuid.uuid4())
        element.update({"name": f"armor_{tier}", "uuid": eid, "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz],
                        "origin": [0, 6, -2],
                        "faces": {d: {"uv": [f["uv"][0], f["uv"][1], f["uv"][0] + f["uv_size"][0], f["uv"][1] + f["uv_size"][1]],
                                      "texture": 0} for d, f in cube["uv"].items()}})
        element.pop("rotation", None)
        bb["elements"].append(element)
        ids.append(eid)

    # plates go in their own group under the body
    body_gid = str(uuid.uuid4())
    body_part = copy.deepcopy(body_group)
    body_part.update({"name": f"armor_{tier}_body", "uuid": body_gid, "origin": [0, 6, 6], "rotation": [0, 0, 0], "children": []})
    bb["groups"].append(body_part)

    def add_to(nodes, parent_uuid, entry):
        for node in nodes:
            if isinstance(node, dict):
                if node.get("uuid") == parent_uuid:
                    node["children"].append(entry)
                    return True
                if add_to(node.get("children", []), parent_uuid, entry):
                    return True
        return False
    assert add_to(bb["outliner"], head_group["uuid"], {"uuid": gid, "isOpen": False, "children": ids[:2]})
    assert add_to(bb["outliner"], body_group["uuid"], {"uuid": body_gid, "isOpen": False, "children": ids[2:]})
buffer = io.BytesIO()
texture.save(buffer, format="PNG")
tex = bb["textures"][0]
tex["source"] = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()
tex["name"] = "guh.png"
tex["relative_path"] = "../src/main/resources/assets/guhs/textures/entity/guh.png"
os.makedirs("blockbench", exist_ok=True)
with open(os.path.join("blockbench", "guh.bbmodel"), "w", encoding="utf-8") as f:
    json.dump(bb, f)
print("imported Lieke's guh model (+ saddle + armour)")
