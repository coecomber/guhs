"""
Het Knuffeldal (2.8) - the characters' looks: Burgemeester Vadsema (the sitting guh with a top hat and a golden chain of
office), Cocotje (a peachy little guh with long hanging ears and a bow: "ze hangen aan me veh"), and the Kruimel-Mika
(the Mika's model, sandy and covered in crumbs, with a bib). Geo models get extra bones whose faces each sample one
8x8 swatch in a free part of the UV sheet (like tools/make_guh_variants.py); the textures are recoloured and painted.
Used by SittingGuhRenderers.NPC_MODELEN (KnuffeldalClient) and the Kruimel-Mika's renderer.
"""
import json
import os
import random

import numpy as np
from PIL import Image

UV = 128
SW = 8


def _used(geo):
    used = np.zeros((UV, UV), bool)
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if isinstance(uv, dict):
                for face in uv.values():
                    (u, v), (w, h) = face["uv"], face["uv_size"]
                    used[int(min(v, v + h)):int(np.ceil(max(v, v + h))), int(min(u, u + w)):int(np.ceil(max(u, u + w)))] = True
            elif isinstance(uv, list):
                u, v = uv
                sx, sy, sz = cube["size"]
                used[int(v):int(np.ceil(v + sz + sy)), int(u):int(np.ceil(u + 2 * (sx + sz)))] = True
    return used


def _swatches(geo, names):
    used = _used(geo)
    out = {}
    for name in names:
        for y in range(0, UV - SW + 1, SW):
            found = False
            for x in range(0, UV - SW + 1, SW):
                if not used[y:y + SW, x:x + SW].any():
                    used[y:y + SW, x:x + SW] = True
                    out[name] = (x, y)
                    found = True
                    break
            if found:
                break
        if name not in out:
            raise SystemExit(f"knuffeldal_npcs: no free texture space for {name}")
    return out


def _cube(origin, size, swatch, inflate=0.0):
    face = {"uv": list(swatch), "uv_size": [SW, SW]}
    c = {"origin": origin, "size": size, "uv": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}
    if inflate:
        c["inflate"] = inflate
    return c


def _paint(arr, swatch, colour, rng, var=8, pattern=None):
    x, y = swatch
    block = arr[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4]
    base = np.array(colour, np.float32)
    noise = rng.normal(0, var / 2, (SW * 4, SW * 4, 1))
    block[..., :3] = np.clip(base + noise, 0, 255).astype(np.uint8)
    block[..., 3] = 255
    if pattern:
        pattern(block)


def _load(h, name):
    return json.load(open(os.path.join(h.A, "geo", "entity", name), encoding="utf-8"))


def _save_geo(h, name, geo_file):
    h.w(os.path.join(h.A, "geo", "entity", name), geo_file)


def burgemeester(h):
    geo_file = _load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_burgemeesterguh"
    sw = _swatches(geo, ["hoed", "band", "goud", "medaille"])
    H = [0, 13, 0]
    geo["bones"].append({"name": "burgemeester_hoed", "parent": "head", "pivot": H, "cubes": [
        _cube([-5.5, 25.8, -6.0], [11, 0.8, 10], sw["hoed"]),
        _cube([-4.0, 26.6, -4.5], [8, 6.4, 7], sw["hoed"]),
        _cube([-4.0, 26.6, -4.5], [8, 1.6, 7], sw["band"], inflate=0.12),
        _cube([-0.8, 27.0, -4.9], [1.6, 1.0, 0.4], sw["goud"])]})
    geo["bones"].append({"name": "burgemeester_ketting", "parent": "body", "pivot": [0, 12, -4], "cubes": [
        _cube([-3.6, 11.3, -4.7], [7.2, 0.6, 0.6], sw["goud"]),
        _cube([-3.6, 9.6, -4.7], [0.6, 1.8, 0.6], sw["goud"]),
        _cube([3.0, 9.6, -4.7], [0.6, 1.8, 0.6], sw["goud"]),
        _cube([-3.0, 9.0, -4.8], [1.4, 0.6, 0.6], sw["goud"]),
        _cube([1.6, 9.0, -4.8], [1.4, 0.6, 0.6], sw["goud"]),
        _cube([-1.3, 7.2, -5.0], [2.6, 2.6, 0.6], sw["medaille"])]})
    _save_geo(h, "guh_npc_burgemeesterguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.80, sat=0.55, val=0.96, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2801)
    _paint(a, sw["hoed"], (46, 36, 58), rng, 6)
    _paint(a, sw["band"], (238, 110, 170), rng, 6)
    _paint(a, sw["goud"], (246, 200, 70), rng, 10)

    def medal(block):
        block[..., :3] = (246, 200, 70)
        for yy in range(32):
            for xx in range(32):
                d = ((xx - 15.5) ** 2 + (yy - 15.5) ** 2) ** 0.5
                if d < 11:
                    block[yy, xx, :3] = (255, 230, 130)
                if 5 < d < 7 and yy > 16:
                    block[yy, xx, :3] = (180, 120, 40)
        for ex in (10, 19):
            block[11:15, ex:ex + 3, :3] = (40, 30, 50)
    _paint(a, sw["medaille"], (246, 200, 70), rng, 4, medal)
    h.save(Image.fromarray(a), "entity", "npc_burgemeesterguh.png")


def cocotje(h):
    geo_file = _load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_cocotje"
    sw = _swatches(geo, ["oor", "oor_binnen", "strik"])
    # her ears hang down along her head (the ear bones stay, so the sitting guh's ear twitch still moves them)
    for bone in geo["bones"]:
        if bone["name"] == "ear_left":
            bone["pivot"] = [7.2, 24.5, -1]
            bone["cubes"] = [_cube([7.0, 13.5, -3.0], [2.2, 11.5, 4.6], sw["oor"]), _cube([6.9, 12.2, -2.6], [2.4, 1.6, 3.8], sw["oor"]),
                             _cube([6.8, 14.5, -2.4], [0.3, 9.0, 3.4], sw["oor_binnen"])]
        elif bone["name"] == "ear_right":
            bone["pivot"] = [-7.2, 24.5, -1]
            bone["cubes"] = [_cube([-9.2, 13.5, -3.0], [2.2, 11.5, 4.6], sw["oor"]), _cube([-9.3, 12.2, -2.6], [2.4, 1.6, 3.8], sw["oor"]),
                             _cube([-7.1, 14.5, -2.4], [0.3, 9.0, 3.4], sw["oor_binnen"])]
    geo["bones"].append({"name": "cocotje_strikje", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        _cube([-3.2, 25.9, -2.2], [2.6, 2.2, 1.6], sw["strik"]), _cube([0.6, 25.9, -2.2], [2.6, 2.2, 1.6], sw["strik"]),
        _cube([-0.6, 26.2, -2.4], [1.2, 1.6, 2.0], sw["strik"])]})
    _save_geo(h, "guh_npc_cocotje.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.05, sat=0.55, val=1.04, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2802)
    _paint(a, sw["oor"], (250, 196, 170), rng, 10)
    _paint(a, sw["oor_binnen"], (246, 150, 170), rng, 6)
    _paint(a, sw["strik"], (240, 90, 150), rng, 8)
    h.save(Image.fromarray(a), "entity", "npc_cocotje.png")


def kruimel_mika(h):
    geo_file = _load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.kruimel_mika"
    geo["bones"] = [b for b in geo["bones"] if b["name"] != "kruimel_slabbetje"]
    sw = _swatches(geo, ["slab"])
    geo["bones"].append({"name": "kruimel_slabbetje", "parent": "head", "pivot": [0, 5, -2], "cubes": [
        _cube([-3.8, -0.8, -12.5], [7.6, 3.4, 0.5], sw["slab"])]})
    _save_geo(h, "kruimel_mika.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    img = h.recolour(src, hue=0.085, sat=0.75, val=1.05, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = random.Random(2803)
    opaque = np.argwhere(a[..., 3] > 0)
    for _ in range(900):                                        # crumbs everywhere
        y, x = opaque[rng.randrange(len(opaque))]
        if rng.random() < 0.6:
            a[y:y + 2, x:x + 2, :3] = (186, 124, 60) if rng.random() < 0.5 else (250, 214, 130)
    nrng = np.random.default_rng(2804)

    def hearts(block):
        block[..., :3] = (255, 250, 246)
        for cy in (8, 22):
            for cx in (8, 22):
                for dy in range(-3, 4):
                    for dx in range(-3, 4):
                        if (abs(dx) + abs(dy) <= 3 and dy >= -1) or (dy == -2 and abs(dx) in (1, 2)):
                            block[cy + dy, cx + dx, :3] = (238, 110, 160)
    _paint(a, sw["slab"], (255, 250, 246), nrng, 4, hearts)
    h.save(Image.fromarray(a), "entity", "kruimel_mika.png")


def build(h):
    burgemeester(h)
    cocotje(h)
    kruimel_mika(h)
