"""1.8.0 Guhdromenland: a walkability check of the island templates (playtest fixes, testbuild 2).

For every island template + its json (data/guhs/dromen/eiland/<id>.json) it reports:
  - fence blocks (hekjes, fences) in the corridor from the arrival spot to the steiger (you must be able to walk out),
  - the arrival and steiger spots: something solid below the feet, feet and head free,
  - entities that stand inside a solid block (feet or head),
  - for the bath: whether the tub can be walked in and out (see droombadje.py; reported by name only).
Run from the worktree root:  python tools/features/dromen_eilandcheck.py [id ...]
"""
import json
import math
import os
import sys

sys.path.insert(0, "tools")
sys.path.insert(0, os.path.join("tools", "features"))
import hekjes as nbt  # noqa: E402  (the byte-exact NBT reader)

D = os.path.join("src", "main", "resources", "data", "guhs")
EILANDEN = ("plein", "liedjes", "sterren", "badje", "molentje", "vlieg")
LUCHT = ("minecraft:air", "minecraft:cave_air", "minecraft:light")
# blocks you walk through (no collision) or that count as "nothing there" for standing room
DOOR = ("minecraft:air", "minecraft:cave_air", "minecraft:light", "minecraft:water", "minecraft:short_grass", "minecraft:tall_grass",
        "guhs:pluisgras", "guhs:maanbloemetje", "guhs:roze_guhbloem", "minecraft:allium", "minecraft:lilac", "minecraft:torch",
        "minecraft:pink_petals", "guhs:zingbloemetje", "minecraft:sign", "guhs:wolkenweide_rank", "minecraft:light_blue_stained_glass")


def _str(b):
    return b.decode("utf-8", "replace") if isinstance(b, (bytes, bytearray)) else b


class Template:
    def __init__(self, pad):
        _, root, _ = nbt.lees(pad)
        r = root.v
        self.size = r[b"size"].v[1]
        pal = r[b"palette"].v[1]
        self.namen = []
        for p in pal:
            naam = _str(p[b"Name"].v)
            props = {}
            if b"Properties" in p:
                props = {_str(k): _str(t.v) for k, t in p[b"Properties"].v.items()}
            self.namen.append((naam, props))
        self.blok = {}
        for b in r[b"blocks"].v[1]:
            pos = tuple(b[b"pos"].v[1])
            self.blok[pos] = b[b"state"].v
        self.entities = []
        if b"entities" in r:
            for e in r[b"entities"].v[1]:
                pos = e[b"pos"].v[1]
                n = e[b"nbt"].v
                eid = _str(n[b"id"].v) if b"id" in n else "?"
                kind = _str(n[b"Kind"].v) if b"Kind" in n else ""
                self.entities.append((pos, eid, kind))

    def naam(self, x, y, z):
        s = self.blok.get((x, y, z))
        return "minecraft:air" if s is None else self.namen[s][0]

    def props(self, x, y, z):
        s = self.blok.get((x, y, z))
        return {} if s is None else self.namen[s][1]

    def vrij(self, x, y, z):
        n = self.naam(x, y, z)
        return n in DOOR or n.endswith("_sign") or n.endswith("_carpet") or "bloem" in n or "lampion" in n and False

    def vast(self, x, y, z):
        n = self.naam(x, y, z)
        return n not in DOOR and "water" not in n


def hek(naam):
    return "hekje" in naam or naam.endswith("_fence") or naam.endswith("_wall") or "tralies" in naam or naam.endswith("iron_bars")


def check(eid):
    js = json.load(open(os.path.join(D, "dromen", "eiland", eid + ".json"), encoding="utf-8"))
    t = Template(os.path.join(D, "structure", "dromen", f"eiland_{eid}.nbt"))
    mx, my, mz = js["midden"]
    fouten = []

    def abs_(rel):
        return (int(math.floor(mx + rel[0])), int(math.floor(my + rel[1])), int(math.floor(mz + rel[2])))

    plekken = {}
    if "aankomst" in js:
        plekken["aankomst"] = js["aankomst"]["plek"]
    if "steiger" in js:
        plekken["steiger"] = js["steiger"]
    for e, rel in js.get("steigers", {}).items():
        plekken["steiger_" + e] = rel
    for naam, rel in plekken.items():
        x, y, z = abs_(rel)
        if not t.vast(x, y - 1, z) and not t.vast(x, y - 2, z):
            fouten.append(f"{naam} {x},{y},{z}: nothing solid under the feet ({t.naam(x, y - 1, z)})")
        if t.vast(x, y, z) or t.vast(x, y + 1, z):
            fouten.append(f"{naam} {x},{y},{z}: feet/head blocked ({t.naam(x, y, z)} / {t.naam(x, y + 1, z)})")
    # the corridor arrival -> steiger (2 wide), at feet and head height: no fences
    if "aankomst" in js and "steiger" in js:
        a, s = abs_(js["aankomst"]["plek"]), abs_(js["steiger"])
        n = int(max(abs(s[0] - a[0]), abs(s[2] - a[2]))) * 2 + 1
        gezien = set()
        for i in range(n + 1):
            f = i / n
            cx, cz = a[0] + (s[0] - a[0]) * f, a[2] + (s[2] - a[2]) * f
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in range(-1, 3):
                        p = (int(round(cx)) + dx, a[1] + dy, int(round(cz)) + dz)
                        if p in gezien:
                            continue
                        gezien.add(p)
                        if hek(t.naam(*p)):
                            fouten.append(f"fence {t.naam(*p)} at {p} in the walk from the arrival to the steiger")
    # the plein: its five steigers and the arrival, corridors from the arrival to each steiger
    if "steigers" in js:
        a = abs_(js["aankomst"]["plek"])
        for e, rel in js["steigers"].items():
            s = abs_(rel)
            r = math.hypot(s[0] - a[0], s[2] - a[2])
            for k in range(int(r * 0.6), int(r) + 1):
                f = k / r
                cx, cz = a[0] + (s[0] - a[0]) * f, a[2] + (s[2] - a[2]) * f
                for dy in (0, 1):
                    p = (int(round(cx)), a[1] + dy, int(round(cz)))
                    if hek(t.naam(*p)):
                        fouten.append(f"fence {t.naam(*p)} at {p} on the way to the steiger of {e}")
    for (pos, eid_, kind) in t.entities:
        x, y, z = int(math.floor(pos[0])), int(math.floor(pos[1] + 0.01)), int(math.floor(pos[2]))
        hoog = 1 if eid_ in ("guhs:blubguh", "guhs:badeendje", "guhs:sterrenvisje") else 2
        for dy in range(hoog):
            if t.vast(x, y + dy, z):
                fouten.append(f"entity {eid_}{'/' + kind if kind else ''} at {x},{y},{z} inside {t.naam(x, y + dy, z)}")
                break
    return t, js, fouten


# --- walkability: a breadth-first walk over standing spots (feet height as a float), steps up of at most one block
HALF = ("_slab",)


def _top(t, x, y, z):
    """How high the block at (x, y, z) lets you stand: None = not solid; else the top above y (0.5 bottom slab, 1.5 fence)."""
    n = t.naam(x, y, z)
    if not t.vast(x, y, z) or n.endswith("_sign") or "lampion" in n or n.endswith("_carpet") or "zeepbel" == n.split(":")[-1]:
        return None
    if hek(n):
        return 1.5
    if n.endswith("_slab"):
        return 1.0 if t.props(x, y, z).get("type") in ("top", "double") else 0.5
    return 1.0


def _vrij_boven(t, x, z, f, hoogte):
    y = math.floor(f)
    while y < f + hoogte:
        top = _top(t, x, y, z)
        if top is not None and y + top > f + 1e-6 and not (y < f and y + top <= f + 1e-6):
            return False
        y += 1
    return True


def _vloeren(t, x, z, ymin, ymax, hoogte):
    uit = []
    for y in range(ymin, ymax):
        top = _top(t, x, y, z)
        if top is not None and top <= 1.0:
            f = y + top
            if _vrij_boven(t, x, z, f, hoogte):
                uit.append(f)
    # in water you can swim: every free water cell counts as a spot as well
    for y in range(ymin, ymax):
        if "water" in t.naam(x, y, z) and _vrij_boven(t, x, z, y, 1.0):
            uit.append(float(y))
    return uit


def bereik(t, start, doelen, hoogte=1.8, straal=70):
    """start, doelen: (x, y, z) feet blocks. Returns the doelen that can be walked to (steps up <= 1, falls <= 4)."""
    sx, sy, sz = start
    from collections import deque
    ymin, ymax = sy - 20, sy + 30
    cache = {}

    def vl(x, z):
        if (x, z) not in cache:
            cache[(x, z)] = _vloeren(t, x, z, ymin, ymax, hoogte)
        return cache[(x, z)]

    begin = min(vl(sx, sz), key=lambda f: abs(f - sy), default=float(sy))
    gezien = {(sx, sz, begin)}
    q = deque([(sx, sz, begin)])
    while q:
        x, z, h = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if abs(nx - sx) > straal or abs(nz - sz) > straal:
                continue
            for f in vl(nx, nz):
                if -4 <= f - h <= 1.0 + 1e-6 and (nx, nz, f) not in gezien:
                    zw = "water" in t.naam(x, math.floor(h), z) and "water" in t.naam(nx, math.floor(f), nz)
                    if f - h > 1e-6 and not _vrij_boven(t, x, z, h, hoogte + (f - h)) and not zw:
                        continue       # no room to jump up from here
                    gezien.add((nx, nz, f))
                    q.append((nx, nz, f))
        # swimming up or down in water
        if "water" in t.naam(x, math.floor(h), z):
            for f in vl(x, z):
                if abs(f - h) <= 1.0 and (x, z, f) not in gezien:
                    gezien.add((x, z, f))
                    q.append((x, z, f))
    plekken = {(x, z): [] for (x, z, _) in gezien}
    for (x, z, h) in gezien:
        plekken[(x, z)].append(h)
    ok = []
    for d in doelen:
        dx, dy, dz = d
        if any(abs(h - dy) <= 1.01 for h in plekken.get((dx, dz), [])):
            ok.append(d)
    return ok


def wandel(eid, t, js):
    """Island-specific walks: [(name, start, [goals])]."""
    mx, my, mz = js["midden"]
    ab = lambda r: (int(math.floor(mx + r[0])), int(math.floor(my + r[1])), int(math.floor(mz + r[2])))
    a = ab(js["aankomst"]["plek"])
    uit = []
    if eid == "liedjes":
        uit.append(("aankomst -> elke klokjesheuvel", a, [ab(v) for v in js["koor"].values()]))
    if eid == "plein":
        uit.append(("aankomst -> binnen in het Droomposthuisje", a, [(26, my + 1, 18)]))
    if eid == "badje":
        b = json.load(open(os.path.join(D, "dromen", "badje_route.json"), encoding="utf-8")) if os.path.exists(
            os.path.join(D, "dromen", "badje_route.json")) else {}
        kuip = (54, my + 1, 56)                      # BX, VLOER+1, BZ of droombadje.py (C+2, T+1, CZ+4)
        uit.append(("aankomst -> in het grote bad", a, [kuip]))
        uit.append(("in het grote bad -> aankomst", kuip, [a]))
        if "snoes" in b:
            uit.append(("aankomst -> Snoes", a, [ab(b["snoes"])]))
    if eid == "vlieg":
        pl = json.load(open(os.path.join(D, "dromen", "vlieg_plekken.json"), encoding="utf-8"))
        uit.append(("aankomst -> elke Rookguh in de stal", a, [ab([s[0], 1, s[2]]) for s in pl["stal"]]))
        uit.append(("aankomst -> de Rookguh naar beneden", a, [ab([pl["terug"][0], 1, pl["terug"][2]])]))
        top = max(t.blok)  # (unused)
        import vliegdroom as vd                                    # the stair's foot and the view on top
        _, plek = vd.ontwerp()
        uit.append(("aankomst -> de voet van de wenteltrap", a, [ab(plek["trap_begin"])]))
        uit.append(("aankomst -> het uitzicht op de hoogste wolk", a, [ab(plek["uitzicht"])]))
    if eid == "sterren":
        uit.append(("aankomst -> het opstapkussen", a, [(46, my + 2, 56)]))   # dromen_sterren.py: next to the first pillow
    return uit


def main(args):
    ids = args or list(EILANDEN)
    totaal = 0
    for eid in ids:
        t, js, fouten = check(eid)
        for (naam, start, doelen) in wandel(eid, t, js):
            ok = bereik(t, start, doelen)
            for d in doelen:
                if d not in ok:
                    fouten.append(f"walk '{naam}': {d} cannot be reached from {start}")
        totaal += len(fouten)
        print(f"{eid}: {'ok' if not fouten else str(len(fouten)) + ' problem(s)'}")
        for f in fouten[:40]:
            print("   ", f)
    return totaal


if __name__ == "__main__":
    sys.exit(1 if main(sys.argv[1:]) else 0)
