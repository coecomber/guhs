"""
1.8.1: het gebouw De Patatzak (De Guhsnackbar), overgenomen uit variant_b van snackbar-ontwerp/_bron/guhsnackbar.py. The render
scene had a terrain of its own; here only the building, its plate and its foundation are set, so the world's ground runs on
around it. Coordinates below are those of the render (floor y 4, cone at CX, CZ) and are shifted by DX/DZ into the template.
"""
import math
import random

NAAM = "snackbar"
CX, CZ = 25, 18                     # the cone in render coordinates
DX, DZ = -5, -4                     # render -> template
W, H, L = 41, 43, 43                # template size
G = 4                               # floor level (the plate) in the template


def straal(y):
    return 8.4 + (y - 5) * .24


class Bouw:
    """The small part of the render Scene that variant_b uses, writing into a make_structures.Structure."""

    def __init__(self, s):
        self.s = s

    def set(self, x, y, z, naam, props=None, nbt=None):
        x, z = int(x) + DX, int(z) + DZ
        if not (0 <= x < W and 0 <= y < H and 0 <= z < L):
            raise SystemExit(f"snackbar: outside the template: {x} {y} {z} {naam}")
        props = dict(props or {})
        if "[" in naam:
            naam, rest = naam[:-1].split("[")
            props.update(p.split("=") for p in rest.split(","))
        if ":" not in naam:
            naam = "guhs:" + naam
        self.s.set(x, int(y), z, naam, props or None, nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, naam):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, naam)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, naam, ymin=None):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                if ymin is not None and y < ymin:
                    continue
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1:
                        self.set(x, y, z, naam)

    def cylinder(self, cx, cz, r, y1, y2, naam):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if math.hypot(x - cx, z - cz) <= r:
                    for y in range(y1, y2 + 1):
                        self.set(x, y, z, naam)


def st(b, f):
    return f"{b}[facing={f}]"


def trap(b, f):
    return f"{b}[facing={f},half=bottom,shape=straight,waterlogged=false]"


def tafeltje(S, x, y, z, stoelen="ew"):
    S.set(x, y, z, st("guh_tafel", "south"))
    if "e" in stoelen:
        S.set(x + 1, y, z, st("guh_stoel", "west"))
    if "w" in stoelen:
        S.set(x - 1, y, z, st("guh_stoel", "east"))


def bord(S, x1, y1, z, breedte, hoogte, eerste):
    """Tiles `eerste`.. of a sign, looking south, left to right and top to bottom as you read it."""
    for rij in range(hoogte):
        for kol in range(breedte):
            S.set(x1 + kol, y1 + hoogte - 1 - rij, z, "snackbar_bord",
                  {"facing": "south", "tegel": str(eerste + rij * breedte + kol)})


def bouw(h, borden):
    """Makes the template; borden: {name: first tile}."""
    from features import reisguh_plek
    s = h.Structure((W, H, L))
    S = Bouw(s)
    cx, cz = CX, CZ
    # foundation under the plate, then the plate, and room above it (a tree or a hill must not stand in the terrace)
    for x in range(cx - 20, cx + 21):
        for z in range(cz - 14, cz + 28):
            terras = ((x - cx) / 19) ** 2 + ((z - cz - 10) / 16) ** 2 <= 1
            zak = math.hypot(x - cx, z - cz) <= 10
            if terras or zak:
                S.fill(x, 0, z, x, G - 1, z, "minecraft:dirt")
                S.set(x, G, z, "knuffelklinkers")
                if x + DX >= 0:
                    S.air(x, G + 1, z, x, G + 6, z)
    S.cylinder(cx, cz, 8, 4, 4, "tegel_hartjes")
    # the inside of the cone stays empty
    for y in range(5, 22):
        S.cylinder(cx, cz, straal(y) - 1.1, y, y, "minecraft:air")
    for y in range(5, 24):
        r = straal(y)
        for x in range(cx - 14, cx + 15):
            for z in range(cz - 14, cz + 15):
                d = math.hypot(x - cx, z - cz)
                if r - 1.1 < d <= r:
                    a = math.atan2(z - cz, x - cx)
                    streep = int((a + math.pi) / (2 * math.pi) * 12 + .5) % 2
                    S.set(x, y, z, "pleister_framboos" if streep else "minecraft:quartz_block")
    S.cylinder(cx, cz, straal(12) - 1, 12, 12, "pleister_boter")
    # the fries
    rng = random.Random(4)
    r = straal(23)
    for x in range(int(cx - r), int(cx + r) + 1):
        for z in range(int(cz - r), int(cz + r) + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r - 1.0:
                top = 24 + rng.randint(1, 4) + int((r - d) * .75)
                S.fill(x, 22, z, x, top, z, "minecraft:yellow_concrete" if rng.random() < .55 else "pleister_boter")
    # a dollop of sauce
    S.ellipsoid(cx, 33, cz, 7, 3, 7, "minecraft:quartz_block", ymin=31)
    S.ellipsoid(cx - 1, 35, cz, 4, 2.4, 4, "minecraft:quartz_block")
    S.ellipsoid(cx - 1, 37, cz, 2, 1.8, 2, "minecraft:quartz_block")
    # the fork
    for i in range(15):
        y, x = 27 + i, cx + 7 + i // 3
        for dz in (-2, 0, 2) if i > 9 else (0,):
            S.set(x, y, cz + 3 + dz, "pleister_framboos")
        if i == 9:
            S.fill(x, y, cz + 1, x, y, cz + 5, "pleister_framboos")
    # the way in and the awning on the south side, the hatch south east
    S.air(cx - 2, 5, cz + 6, cx + 2, 8, cz + 12)
    for x in range(cx - 4, cx + 5):
        b = "dakpan_roze_trap" if x % 2 else "guh_dakpan_wit_trap"
        S.set(x, 10, cz + 11, trap(b, "north"))
        S.set(x, 9, cz + 12, trap(b, "north"))
    S.air(cx + 5, 7, cz + 4, cx + 8, 8, cz + 11)
    # the counter with the pans, the machines against the back wall
    for a in range(-170, -9, 8):
        x = cx + 6 * math.cos(math.radians(a))
        z = cz + 3 + 4.6 * math.sin(math.radians(a))
        S.set(round(x), 5, round(z), st("besteltafel", "south"))
    for dx in (-5, -3, -1, 1, 3, 5):
        S.set(cx + dx, 5, cz - 7 + abs(dx) // 3, st("frituurautomaat", "south"))
    for dx in (-3, -1, 1, 3):
        S.set(cx + dx, 6, cz - 2, st("frying_pan", "south"))
    S.set(cx, 5, cz - 6, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/snackbar"})
    # the signs: the menu (one block in front of the render's wall, against the block in front of the machines), the name
    bord(S, cx - 5, 7, cz - 5, 11, 4, borden["menu"])
    bord(S, cx - 8, 17, cz + 13, 17, 4, borden["naam"])
    bord(S, cx - 5, 12, cz + 11, 10, 2, borden["snackbar"])
    for x in (cx - 6, cx + 6):
        S.fill(x, 18, cz + 12, x, 19, cz + 12, "pleister_boter")
    tafeltje(S, cx - 6, 5, cz + 4, "e")
    tafeltje(S, cx + 6, 5, cz + 4, "w")
    # the terrace
    tafeltje(S, cx - 12, 5, cz + 17)
    tafeltje(S, cx - 5, 5, cz + 20)
    tafeltje(S, cx + 7, 5, cz + 20)
    tafeltje(S, cx + 14, 5, cz + 15)
    S.set(cx - 12, 6, cz + 17, "snackbar_parasol", {"geel": "false"})
    S.set(cx + 7, 6, cz + 20, "snackbar_parasol", {"geel": "true"})
    S.set(cx + 17, 5, cz + 18, st("park_prullenbak", "west"))
    for x in (cx - 16, cx + 18):
        S.set(x, 5, cz + 6, "seizoensbloembak")
    # the jigsaw in the middle (the structure is placed by it)
    s.set(W // 2, 0, L // 2, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:snackbar_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "minecraft:dirt", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    # the characters: the Snackbar-guh behind the counter, a Reisguh on the terrace
    ms = h.ms
    gx, gz = cx + 0.5 + DX, cz - 3.5 + DZ
    s.entity(gx, float(G + 1), gz, {"id": "guhs:guh_npc", "Kind": "snackbarguh", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(0.0, 0.0)})
    plekken = [(cx + DX + 4, G + 1, cz + DZ + 14), (cx + DX + 5, G + 1, cz + DZ + 14), (cx + DX - 5, G + 1, cz + DZ + 14)]
    reisguh_plek.zet(s, plekken, "Guhsnackbar", 0.0, ms.Byte, ms.floats, "snackbar")
    return s


def check(s):
    """The things that go wrong silently."""
    fout = []
    gx, gz = CX + DX, CZ + DZ - 4
    if s.get(gx, G + 1, gz) not in (None, "minecraft:air") or s.get(gx, G + 2, gz) not in (None, "minecraft:air"):
        fout.append("the Snackbar-guh's spot is not free")
    for y in (G + 1, G + 2):
        if s.get(CX + DX, y, CZ + DZ + 8) not in (None, "minecraft:air"):
            fout.append("the door is blocked")
    if not any(z == CZ + DZ - 2 and s.get(CX + DX, 5, z) == "guhs:besteltafel" for z in range(L)):
        fout.append("no counter in front of the Snackbar-guh")
    return fout
