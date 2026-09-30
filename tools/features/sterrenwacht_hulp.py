"""
Helpers shared by the three "buiten" features of 2.8 (sterrenwacht, ballon, kamperen): model elements, NPC geo models
with extra bones (each face samples one 8x8 swatch of a free part of the 128x128 sheet), painting, big guh faces made
of blocks (for the buildings), and the geometry self-check of a template (nothing floats, every important spot can be
walked to from the edge, NPCs stand on a floor).
"""
import json
import math
import os
from collections import deque

import numpy as np
from PIL import Image

AIR = "minecraft:air"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}

# guh colours (the guh's own style)
EYE_DARK = (34, 24, 52)
EYE_RING = (64, 132, 214)
EYE_RING_LIGHT = (104, 206, 232)
WHITE = (255, 255, 255)
BLUSH = (255, 150, 188)
NOSE = (232, 112, 164)
MOUTH = (150, 72, 116)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


# =====================================================================================================================
# block models
# =====================================================================================================================
def el(frm, to, tex, faces=None, rot=None, uv=None, shade=True):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex})
                                          for f in (faces or ("down", "up", "north", "south", "west", "east"))}}
    if rot:
        e["rotation"] = rot
    if not shade:
        e["shade"] = False
    return e


def facing_variants(model, extra_props=None):
    """facing=<f>[,<extra>] -> the model turned (north = as modelled)."""
    out = {}
    for f, r in ROT.items():
        for key, m in (extra_props or {"": model}).items():
            out[f"facing={f}" + (f",{key}" if key else "")] = {"model": m, **({"y": r} if r else {})}
    return out


# =====================================================================================================================
# textures
# =====================================================================================================================
def noisy(base, var, rng, size=16):
    a = np.zeros((size, size, 4), np.uint8)
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            a[y, x, :3] = clamp(c + v for c in base)
            a[y, x, 3] = 255
    return Image.fromarray(a).copy()


def guh_face_pixels(w, h, mood=0):
    """A guh face on a w x h grid (w, h >= 16): {(x, y): colour}. mood 0 happy, 1 sleepy, 2 surprised."""
    out = {}
    s = min(w, h) / 16.0

    def put(x, y, c):
        for dx in range(max(1, int(round(s)))):
            for dy in range(max(1, int(round(s)))):
                px, py = int(x * s) + dx + (w - int(16 * s)) // 2, int(y * s) + dy + (h - int(16 * s)) // 2
                if 0 <= px < w and 0 <= py < h:
                    out[(px, py)] = c

    for ex in (2, 9):
        if mood == 1:
            for dx, dy in ((0, 7), (1, 8), (2, 8), (3, 8), (4, 7)):
                put(ex + dx, dy, EYE_DARK)
        else:
            big = mood == 2
            for y in (range(4, 10) if big else range(5, 10)):
                for dx in range(5):
                    cx, cy = 2.0, (6.5 if big else 7.0)
                    d = math.hypot(dx - cx, (y - cy) * 1.05)
                    if d <= 2.6:
                        c = EYE_DARK
                        if d > 1.5 and y >= cy:
                            c = EYE_RING_LIGHT if d > 2.1 else EYE_RING
                        put(ex + dx, y, c)
            put(ex + 1, (5 if big else 6), WHITE)
            put(ex + 3, 8, (220, 230, 255))
    for x, y in ((1, 11), (2, 11), (13, 11), (14, 11)):
        put(x, y, BLUSH)
    put(7, 11, NOSE)
    put(8, 11, NOSE)
    if mood == 2:
        for x, y in ((7, 13), (8, 13), (6, 14), (9, 14), (7, 15), (8, 15)):
            put(x, y, MOUTH)
    else:
        for x, y in ((6, 12), (7, 13), (8, 13), (9, 12)):
            put(x, y, MOUTH)
    return out


def paint_face(img, box, mood=0):
    """Paints a guh face into the box (x0, y0, w, h) of an RGBA image."""
    x0, y0, w, h = box
    px = img.load()
    for (x, y), c in guh_face_pixels(w, h, mood).items():
        px[x0 + x, y0 + y] = c + (255,)


def particle_frames(h, name, frames):
    """Writes particle textures (PIL images) and the particle json."""
    for i, img in enumerate(frames):
        h.save(img, "particle", f"{name}_{i}.png")
    h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(len(frames))]})


def sounds(h, entries):
    """Adds our sound events to sounds.json (idempotent)."""
    def patch(d):
        for event, sound_list in entries.items():
            d[event] = {"sounds": sound_list, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


# =====================================================================================================================
# advancements
# =====================================================================================================================
def quest_advancements(h, names):
    for name in names:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def display_advancement(h, name, parent, icon, frame, crit, title, desc):
    """A shown advancement in the Knuffeldal tab (guhs:knuffeldal/<name>)."""
    adv = {"parent": f"guhs:knuffeldal/{parent}",
           "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.knuffeldal.{name}.title"},
                       "description": {"translate": f"advancements.guhs.knuffeldal.{name}.description"},
                       "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
           "criteria": crit}
    h.w(f"{h.D}/advancement/knuffeldal/{name}.json", adv)
    h.lang(f"advancements.guhs.knuffeldal.{name}.title", title, title)
    h.lang(f"advancements.guhs.knuffeldal.{name}.description", desc, desc)


IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def in_structure(name):
    return {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{name}"}}}}}


# =====================================================================================================================
# NPC models: the sitting guh with extra bones
# =====================================================================================================================
UV = 128
SW = 8


def _used(geo):
    used = np.zeros((UV, UV), bool)
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if isinstance(uv, dict):
                for face in uv.values():
                    (u, v), (w, hh) = face["uv"], face["uv_size"]
                    used[int(min(v, v + hh)):int(np.ceil(max(v, v + hh))), int(min(u, u + w)):int(np.ceil(max(u, u + w)))] = True
            elif isinstance(uv, list):
                u, v = uv
                sx, sy, sz = cube["size"]
                used[int(v):int(np.ceil(v + sz + sy)), int(u):int(np.ceil(u + 2 * (sx + sz)))] = True
    return used


def swatches(geo, names):
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
            raise SystemExit(f"buiten npc model: no free texture space for {name}")
    return out


def cube(origin, size, swatch, inflate=0.0, rotation=None, pivot=None):
    face = {"uv": list(swatch), "uv_size": [SW, SW]}
    c = {"origin": origin, "size": size, "uv": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}
    if inflate:
        c["inflate"] = inflate
    if rotation:
        c["rotation"] = rotation
        c["pivot"] = pivot or origin
    return c


def paint_swatch(arr, swatch, colour, rng, var=8, pattern=None):
    """Fills a swatch of the (512x512: 4 px per uv unit) sitting guh texture."""
    x, y = swatch
    block = arr[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4]
    base = np.array(colour, np.float32)
    noise = rng.normal(0, var / 2, (SW * 4, SW * 4, 1))
    block[..., :3] = np.clip(base + noise, 0, 255).astype(np.uint8)
    block[..., 3] = 255
    if pattern:
        pattern(block)


def sitting_geo(h, identifier):
    geo_file = json.load(open(os.path.join(h.A, "geo", "entity", "guh_sitting.geo.json"), encoding="utf-8"))
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = identifier
    return geo_file, geo


def save_geo(h, name, geo_file):
    h.w(os.path.join(h.A, "geo", "entity", name), geo_file)


def sitting_texture(h, hue, sat, val):
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    return np.asarray(h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish).convert("RGBA")).copy()


# =====================================================================================================================
# buildings: guh faces of blocks, and the geometry self-check
# =====================================================================================================================
def face_role(u, v, R):
    """A guh face (front view, u right, v up) of radius R: skin, ear, ear_in, eye, ring, shine, nose, mouth, cheek or None."""
    role = None
    for sx in (-1, 1):
        d = math.dist((u, v), (sx * 0.62 * R, 0.78 * R))
        if d <= 0.3 * R + 0.35:
            role = "ear_in" if d <= 0.15 * R + 0.2 else "ear"
    if (u / (R + 0.4)) ** 2 + (v / (0.86 * R + 0.4)) ** 2 <= 1:
        role = "skin"
        for sx in (-1, 1):
            ex, ey = sx * 0.4 * R, 0.08 * R
            d = math.dist((u, v), (ex, ey))
            er = 0.2 * R + 0.3
            if d <= er:
                role = "eye"
                if d > er * 0.55 and v < ey:
                    role = "ring"
                if math.dist((u, v), (ex - 0.07 * R, ey + 0.08 * R)) <= max(0.5, 0.06 * R):
                    role = "shine"
            if math.dist((u, v), (sx * 0.62 * R, -0.3 * R)) <= 0.13 * R + 0.3:
                role = "cheek"
        if abs(u) <= 0.08 * R + 0.3 and abs(v + 0.2 * R) <= 0.06 * R + 0.3:
            role = "nose"
        mouth_v = -0.36 * R
        if abs(u) <= 0.18 * R + 0.3 and abs(v - (mouth_v + 0.35 * (u / (0.18 * R + 0.3)) ** 2 * 0.1 * R)) <= 0.5 and abs(u) > 0.05 * R:
            role = "mouth"
    return role


FACE_WOOL = {"skin": "minecraft:pink_wool", "ear": "minecraft:pink_wool", "ear_in": "minecraft:magenta_wool", "eye": "minecraft:black_wool",
             "ring": "minecraft:light_blue_wool", "shine": "minecraft:white_wool", "nose": "minecraft:magenta_wool",
             "mouth": "minecraft:purple_wool", "cheek": "minecraft:pink_concrete"}


def wall_face(s, cx, cy, z_or_x, R, facing, palette=None, only_role=False):
    """A flat guh face of blocks, standing, looking `facing` (north/south: in the x-y plane at z; east/west: z-y at x).
    Returns the number of blocks set."""
    pal = dict(FACE_WOOL)
    pal.update(palette or {})
    n = 0
    rr = int(R + 2)
    for du in range(-rr, rr + 1):
        for dv in range(-rr, rr + 2):
            role = face_role(du, dv, R)
            if role is None:
                continue
            # looking south: the face's right (u) is towards -x; north: +x; east: +z; west: -z
            if facing == "south":
                x, z = cx - du, z_or_x
            elif facing == "north":
                x, z = cx + du, z_or_x
            elif facing == "east":
                x, z = z_or_x, cx + du
            else:
                x, z = z_or_x, cx - du
            s.set(x, cy + dv, z, pal[role])
            n += 1
    return n


def floor_face(s, cx, cz, y, R, up="north", palette=None):
    """A guh face lying on the ground (to be seen from above); `up` is where the ears point."""
    pal = dict(FACE_WOOL)
    pal.update(palette or {})
    rr = int(R + 2)
    n = 0
    for du in range(-rr, rr + 1):
        for dv in range(-rr, rr + 2):
            role = face_role(du, dv, R)
            if role is None:
                continue
            dx, dz = {"north": (du, -dv), "south": (-du, dv), "east": (dv, du), "west": (-dv, -du)}[up]
            s.set(cx + dx, y, cz + dz, pal[role])
            n += 1
    return n


PASSABLE_PREFIX = ("minecraft:air", "minecraft:light", "minecraft:torch", "minecraft:wall_torch", "minecraft:lantern", "minecraft:soul_lantern")


def passable(b, extra=()):
    if b is None or b == AIR:
        return True
    if b in extra:
        return True
    return b.endswith(("_carpet", "_flower", "_tulip", "_sapling", "torch", "_button", "_pressure_plate", "_sign", "_banner")) \
        or b in ("minecraft:short_grass", "minecraft:dandelion", "minecraft:poppy", "minecraft:cornflower", "minecraft:allium",
                 "minecraft:oxeye_daisy", "minecraft:azure_bluet", "minecraft:lily_of_the_valley", "guhs:roze_gras", "guhs:guhoortjes",
                 "guhs:kaasbloem", "guhs:roze_guhbloem", "guhs:pluisgras", "guhs:guhpaddenstoel", "minecraft:rail", "guhs:guh_slaapzak",
                 "minecraft:snow")


def check_floating(s, ground_y):
    """Every block must hang together with the ground (6-neighbours), starting from all blocks at y <= ground_y."""
    solid = {p for p, (b, _, _) in s.blocks.items() if b != AIR}
    seen = set()
    q = deque(p for p in solid if p[1] <= ground_y)
    seen.update(q)
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in seen:
                seen.add(n)
                q.append(n)
    return sorted(solid - seen)


def walk(s, starts, extra_passable=(), max_down=1):
    """Spots a player can walk to (feet position) from the starts: a passable spot and the one above it, a non-passable
    block under it; steps up or down one block, ladders up and down."""
    def ok(x, y, z):
        if not (0 <= x < s.size[0] and 0 <= z < s.size[2] and 0 < y < s.size[1] - 1):
            return False
        return passable(s.get(x, y, z), extra_passable) and passable(s.get(x, y + 1, z), extra_passable) \
            and (not passable(s.get(x, y - 1, z), extra_passable) or s.get(x, y, z) == "minecraft:ladder")

    seen = set()
    q = deque()
    for st in starts:
        if ok(*st):
            seen.add(st)
            q.append(st)
    while q:
        x, y, z = q.popleft()
        nbrs = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in range(1, -max_down - 1, -1):
                if dy == 1 and not passable(s.get(x, y + 2, z), extra_passable):
                    continue
                nbrs.append((x + dx, y + dy, z + dz))
        if s.get(x, y, z) == "minecraft:ladder" or s.get(x, y + 1, z) == "minecraft:ladder":
            nbrs += [(x, y + 1, z), (x, y - 1, z)]
        for n in nbrs:
            if n not in seen and ok(*n):
                seen.add(n)
                q.append(n)
    return seen


def near_reachable(reach, x, y, z, r=2):
    """Is there a walkable spot within r blocks (horizontally, same floor +-1) of (x, y, z)?"""
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy, z + dz) in reach:
                    return True
    return False
