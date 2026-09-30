"""
Builds the structure templates (data/guhs/structure/*.nbt):

  hamster_house   - a giant Littlest-Pet-Shop style hamster playset: magenta base plate, flower running wheel,
                    cheese wedge house (with the guh spawner + loot chest), twisty yellow tube up to a pink ball,
                    apple house on a purple seesaw... and a ~10 block long giant guh.
  evil_mika_home  - the dark version in Mika's biome: black/crying obsidian, a broken wheel, and Mikas inside.
  empty / portal_room - empty rooms for the GameTests.

You can also edit the generated .nbt files in-game with a structure block (Load -> guhs:hamster_house) and save
them again; just don't re-run this script afterwards.

Run from the project root:  python tools/make_structures.py
"""
import gzip
import json
import math
import os
import struct

# Minecraft 1.21.1, on purpose also for Guhs 1.1.0 (26.1.2): the templates are written in 1.21.1's formats (block names such
# as minecraft:chain, sign texts and item components in block entities as 1.21.1 had them), and 26.1.2's DataFixer upgrades
# every template with an older DataVersion when it loads it. Raise this only together with writing the 26.1 formats.
DATA_VERSION = 3955
OUT = os.path.join("src", "main", "resources", "data", "guhs", "structure")


# ---------------------------------------------------------------------------------------------------------------------
# Tiny NBT writer
# ---------------------------------------------------------------------------------------------------------------------
class Byte(int): pass
class Short(int): pass
class Float(float): pass
class Double(float): pass
class Long(int): pass


class NbtList(list):
    def __init__(self, tag_type, items):
        super().__init__(items)
        self.tag_type = tag_type


def _tag_type(v):
    if isinstance(v, Byte) or isinstance(v, bool): return 1
    if isinstance(v, Short): return 2
    if isinstance(v, Long): return 4
    if isinstance(v, int): return 3
    if isinstance(v, Float): return 5
    if isinstance(v, (Double, float)): return 6
    if isinstance(v, str): return 8
    if isinstance(v, NbtList): return 9
    if isinstance(v, dict): return 10
    raise TypeError(v)


def _payload(v):
    t = _tag_type(v)
    if t == 1: return struct.pack(">b", int(v))
    if t == 2: return struct.pack(">h", v)
    if t == 3: return struct.pack(">i", v)
    if t == 4: return struct.pack(">q", v)
    if t == 5: return struct.pack(">f", v)
    if t == 6: return struct.pack(">d", v)
    if t == 8:
        b = v.encode()
        return struct.pack(">H", len(b)) + b
    if t == 9:
        return bytes([v.tag_type if v else 0]) + struct.pack(">i", len(v)) + b"".join(_payload(i) for i in v)
    if t == 10:
        out = b""
        for k, item in v.items():
            kb = k.encode()
            out += bytes([_tag_type(item)]) + struct.pack(">H", len(kb)) + kb + _payload(item)
        return out + b"\x00"


def ints(*v): return NbtList(3, list(v))
def doubles(*v): return NbtList(6, [Double(x) for x in v])
def floats(*v): return NbtList(5, [Float(x) for x in v])
def compounds(items): return NbtList(10, items)


# ---------------------------------------------------------------------------------------------------------------------
# Structure builder
# ---------------------------------------------------------------------------------------------------------------------
class Structure:
    def __init__(self, size):
        self.size = size
        self.blocks = {}      # (x, y, z) -> (block name, properties dict, block entity nbt or None)
        self.entities = []

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, name, props=None, nbt=None):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if self.inside(x, y, z):
            self.blocks[(x, y, z)] = (name, props or {}, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if not only_empty or (x, y, z) not in self.blocks:
                        self.set(x, y, z, name, props)

    def sphere(self, cx, cy, cz, r, name, shell=None, inside="minecraft:air"):
        """Ball of radius r; with `shell` only the outer `shell` thick layer, the inside filled with `inside`."""
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for y in range(int(cy - r - 1), int(cy + r + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    d = math.dist((x, y, z), (cx, cy, cz))
                    if d <= r + 0.3:
                        if shell is None or d > r - shell + 0.3:
                            self.set(x, y, z, name)
                        elif inside:
                            self.set(x, y, z, inside)

    def ring_xy(self, cx, cy, z0, z1, r_out, r_in, name, keep=lambda angle: True):
        """A standing ring (like a wheel) in the X/Y plane, from z0 to z1."""
        for x in range(int(cx - r_out - 1), int(cx + r_out + 2)):
            for y in range(int(cy - r_out - 1), int(cy + r_out + 2)):
                d = math.dist((x, y), (cx, cy))
                if r_in - 0.3 <= d <= r_out + 0.3 and keep(math.degrees(math.atan2(y - cy, x - cx)) % 360):
                    for z in range(z0, z1 + 1):
                        self.set(x, y, z, name)

    def line(self, a, b, name, thickness=0):
        steps = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
        for s in range(steps + 1):
            t = s / steps
            p = [a[i] + (b[i] - a[i]) * t for i in range(3)]
            for dx in range(-thickness, thickness + 1):
                for dy in range(-thickness, thickness + 1):
                    for dz in range(-thickness, thickness + 1):
                        self.set(p[0] + dx, p[1] + dy, p[2] + dz, name)

    def tube(self, path, name, radius=1):
        """Hollow see-through tube (3x3 with an air core) along a list of points."""
        for a, b in zip(path, path[1:]):
            steps = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
            for s in range(steps + 1):
                t = s / steps
                p = [round(a[i] + (b[i] - a[i]) * t) for i in range(3)]
                for dx in range(-radius, radius + 1):
                    for dy in range(-radius, radius + 1):
                        for dz in range(-radius, radius + 1):
                            q = (p[0] + dx, p[1] + dy, p[2] + dz)
                            if self.get(*q) != "minecraft:air":
                                self.set(*q, name)
                self.set(*p, "minecraft:air")

    def entity(self, x, y, z, nbt):
        self.entities.append((x, y, z, nbt))

    def clear_above(self, footprint, y0, name="minecraft:air", top=None):
        """Air in the empty spots above the plate, so hills don't poke through the build. Near the edge of the plate only
        a few blocks are cleared, further in more and more: nearby mountains slope down to the build instead of being cut
        off like a wall (the structure's beard_box terrain adaptation smooths the rest)."""
        fp = set(footprint)
        dist = {}
        frontier = [c for c in fp if any((c[0] + dx, c[1] + dz) not in fp for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
        for c in frontier:
            dist[c] = 0
        while frontier:
            nxt = []
            for (x, z) in frontier:
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = (x + dx, z + dz)
                    if n in fp and n not in dist:
                        dist[n] = dist[(x, z)] + 1
                        nxt.append(n)
            frontier = nxt
        limit = min(top or self.size[1], self.size[1])
        for (x, z) in footprint:
            col_top = min(limit, y0 + 4 + int(dist.get((x, z), 0) * 1.5))
            for y in range(y0, col_top):
                if (x, y, z) not in self.blocks:
                    self.set(x, y, z, name)

    def save(self, name):
        palette, palette_index, blocks = [], {}, []
        for (x, y, z), (block, props, nbt) in sorted(self.blocks.items()):
            key = (block, tuple(sorted(props.items())))
            if key not in palette_index:
                palette_index[key] = len(palette)
                entry = {"Name": block}
                if props:
                    entry["Properties"] = dict(props)
                palette.append(entry)
            b = {"pos": ints(x, y, z), "state": palette_index[key]}
            if nbt:
                b["nbt"] = nbt
            blocks.append(b)
        entities = [{"pos": doubles(x, y, z), "blockPos": ints(int(x), int(y), int(z)), "nbt": nbt}
                    for x, y, z, nbt in self.entities]
        root = {"DataVersion": DATA_VERSION, "size": ints(*self.size), "palette": compounds(palette),
                "blocks": compounds(blocks), "entities": compounds(entities)}
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name + ".nbt")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(gzip.compress(b"\x0a\x00\x00" + _payload(root)))
        print(f"wrote {path}: {len(blocks)} blocks, {len(entities)} entities, size {self.size}")


# average colours of the vanilla wool / concrete / terracotta textures (for turning the guh texture into blocks)
BLOCK_COLOURS = {"white_wool": (234, 236, 237), "white_concrete": (207, 213, 214), "white_terracotta": (210, 178, 161), "orange_wool": (241, 118, 20), "orange_concrete": (224, 97, 1), "orange_terracotta": (162, 84, 38), "magenta_wool": (190, 69, 180), "magenta_concrete": (169, 49, 159), "magenta_terracotta": (150, 88, 109), "light_blue_wool": (58, 175, 218), "light_blue_concrete": (36, 137, 199), "light_blue_terracotta": (114, 109, 138), "yellow_wool": (249, 198, 40), "yellow_concrete": (241, 175, 22), "yellow_terracotta": (186, 133, 35), "lime_wool": (112, 185, 26), "lime_concrete": (94, 169, 25), "lime_terracotta": (103, 118, 53), "pink_wool": (238, 141, 173), "pink_concrete": (214, 101, 143), "pink_terracotta": (162, 78, 79), "gray_wool": (63, 68, 72), "gray_concrete": (55, 58, 62), "gray_terracotta": (58, 43, 36), "light_gray_wool": (142, 142, 135), "light_gray_concrete": (125, 125, 115), "light_gray_terracotta": (136, 107, 98), "cyan_wool": (21, 138, 145), "cyan_concrete": (21, 119, 136), "cyan_terracotta": (87, 91, 91), "purple_wool": (122, 42, 173), "purple_concrete": (100, 32, 156), "purple_terracotta": (118, 70, 86), "blue_wool": (53, 57, 158), "blue_concrete": (45, 47, 143), "blue_terracotta": (75, 60, 91), "brown_wool": (114, 72, 41), "brown_concrete": (96, 60, 32), "brown_terracotta": (77, 51, 36), "green_wool": (85, 110, 27), "green_concrete": (73, 91, 37), "green_terracotta": (76, 83, 42), "red_wool": (161, 39, 35), "red_concrete": (142, 33, 33), "red_terracotta": (143, 61, 47), "black_wool": (21, 21, 26), "black_concrete": (9, 11, 16), "black_terracotta": (37, 23, 16)}


def mc(name):
    return "minecraft:" + name


def guh_nbt(scale, health=None, **extra):
    nbt = {"id": "guhs:guh", "PersistenceRequired": Byte(1),
           "attributes": compounds([{"id": "minecraft:scale", "base": Double(scale)}])}
    if health:
        nbt["attributes"].append({"id": "minecraft:max_health", "base": Double(health)})
        nbt["Health"] = Float(health)
    nbt.update(extra)
    return nbt


def chest(s, x, y, z, facing, loot):
    s.set(x, y, z, mc("chest"), {"facing": facing, "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": loot})


# ---------------------------------------------------------------------------------------------------------------------
# Hamster house
# ---------------------------------------------------------------------------------------------------------------------
def hamster_house():
    W, H, D = 32, 26, 28
    s = Structure((W, H, D))

    # base plate: a rounded magenta plate with pink polka dots, like the playset's base
    footprint = []
    for x in range(W):
        for z in range(D):
            if ((x - 15.5) / 16.2) ** 2 + ((z - 13.5) / 14.2) ** 2 <= 1.0:
                footprint.append((x, z))
                dot = (x * 3 + z * 5) % 11 == 0
                s.set(x, 0, z, mc("pink_concrete") if dot else mc("magenta_concrete"))

    # --- the flower running wheel (back left) ---
    wx, wy = 8, 11
    s.fill(7, 1, 23, 9, wy, 24, mc("purple_concrete"))          # trunk
    s.fill(5, 1, 22, 11, 2, 25, mc("purple_concrete"))          # foot
    s.ring_xy(wx, wy, 20, 21, 7, 5.6, mc("pink_concrete"))     # rim
    for a in range(0, 360, 30):                                 # flower scallops
        s.set(wx + 7.9 * math.cos(math.radians(a)), wy + 7.9 * math.sin(math.radians(a)), 20.5, mc("pink_terracotta"))
        s.set(wx + 7.9 * math.cos(math.radians(a)), wy + 7.9 * math.sin(math.radians(a)), 21, mc("pink_terracotta"))
    for a in range(0, 360, 72):                                 # petals / spokes
        for r in range(1, 6):
            s.set(wx + r * math.cos(math.radians(a)), wy + r * math.sin(math.radians(a)), 22, mc("pink_terracotta"))
    s.fill(wx - 1, wy - 1, 21, wx + 1, wy + 1, 22, mc("red_concrete"))  # hub

    # --- the cheese wedge house (back right), slope going down to the right ---
    x0, x1, z0, z1 = 18, 29, 14, 25
    for x in range(x0, x1 + 1):
        top = 9 - (x - x0) * 5 // (x1 - x0)          # 9 high on the left, 4 on the right
        for z in range(z0, z1 + 1):
            for y in range(1, top + 1):
                wall = x in (x0, x1) or z in (z0, z1) or y == top
                s.set(x, y, z, mc("yellow_terracotta") if wall else mc("air"))
    for (hx, hy, hz) in ((x0, 5, 18), (x0, 3, 22), (x1, 2, 17), (x1, 3, 23), (24, 6, z1), (21, 4, z1)):
        s.set(hx, hy, hz, mc("orange_terracotta"))   # cheese holes
    s.fill(22, 1, z0, 25, 3, z0, mc("air"))            # door
    s.fill(23, 4, z0, 24, 4, z0, mc("air"))            # arched top
    for (fx, fy) in ((20, 6), (26, 5), (28, 3)):      # roses on the front wall
        s.set(fx, fy, z0, mc("red_concrete"))
        s.set(fx - 1, fy, z0, mc("lime_concrete"))
    s.set(24, 1, 22, "guhs:guh_spawner")              # the (mineable) guh spawner
    chest(s, 20, 1, 24, "south", "guhs:chests/hamster_house")
    for (bx, bz) in ((26, 16), (27, 16), (26, 17), (27, 17), (25, 16), (28, 16), (25, 17), (28, 17)):
        s.set(bx, 1, bz, mc("purple_concrete"))       # food bowl
    s.set(26, 1, 16, mc("air"))
    s.set(27, 1, 16, "guhs:block_of_kaasknabbels")

    # --- twisty yellow tube from the cheese roof up to the pink ball ---
    tube_path = [(22, 7, 20), (23, 10, 20), (22, 12, 19), (21, 14, 20), (22, 16, 20)]
    s.tube(tube_path, mc("yellow_stained_glass"))

    # --- pink see-through ball on a magenta cup ---
    bx, by, bz = 22, 20, 20
    s.fill(bx - 2, 16, bz - 2, bx + 2, 16, bz + 2, mc("magenta_concrete"))
    s.ring_xy(bx, 17, bz - 3, bz + 3, 3.2, 2.6, mc("magenta_concrete"), keep=lambda a: 180 <= a <= 360)
    s.sphere(bx, by, bz, 4, mc("pink_stained_glass"), shell=1)
    s.entity(bx + 0.5, by - 3, bz + 0.5, guh_nbt(0.6))  # a little guh rolling around in the ball

    # --- purple seesaw with the apple house ---
    s.fill(22, 14, 12, 30, 14, 13, mc("purple_concrete"))
    s.sphere(28, 17, 12.5, 2.4, mc("red_concrete"), shell=1)
    s.set(28, 20, 12, mc("spruce_log"))
    s.set(29, 20, 12, mc("lime_concrete"))
    s.fill(27, 16, 10, 28, 17, 10, mc("air"))         # window with a view

    # --- water bottle, a slide tube from the wheel to the ball ---
    s.fill(14, 3, 24, 15, 8, 25, mc("light_blue_stained_glass"))
    s.fill(14, 9, 24, 15, 9, 25, mc("white_concrete"))
    s.set(14, 4, 23, mc("iron_bars"))
    s.tube([(9, 19, 20), (13, 21, 20), (17, 21, 20)], mc("yellow_stained_glass"))

    # --- snacks lying around on the plate ---
    s.set(4, 1, 8, mc("yellow_terracotta"))
    s.set(5, 1, 9, mc("red_concrete"))
    s.set(3, 1, 11, "guhs:block_of_kaasknabbels")

    # --- the giant guh (~10 blocks long) in the middle of the playground ---
    s.entity(15.5, 1.0, 8.5, guh_nbt(6.9, health=250, Rotation=floats(20.0, 0.0)))

    s.clear_above(footprint, 1)
    s.save("hamster_house")


# ---------------------------------------------------------------------------------------------------------------------
# Evil Mika home
# ---------------------------------------------------------------------------------------------------------------------
def evil_mika_home():
    W, H, D = 22, 18, 20
    s = Structure((W, H, D))
    footprint = []
    for x in range(W):
        for z in range(D):
            if ((x - 10.5) / 11.2) ** 2 + ((z - 9.5) / 10.2) ** 2 <= 1.0:
                footprint.append((x, z))
                spot = (x * 7 + z * 3) % 13 == 0
                s.set(x, 0, z, mc("crying_obsidian") if spot else mc("black_concrete"))

    # a broken, dark running wheel (left)
    s.fill(4, 1, 15, 5, 7, 16, mc("black_concrete"))
    s.ring_xy(4.5, 8, 13, 13, 5, 4, mc("magenta_terracotta"), keep=lambda a: not (20 <= a <= 110))
    for a in (0, 150, 250):
        for r in range(1, 4):
            s.set(4.5 + r * math.cos(math.radians(a)), 8 + r * math.sin(math.radians(a)), 14, mc("red_concrete"))

    # the evil cheese wedge (purple/black with glowing crying obsidian holes)
    x0, x1, z0, z1 = 9, 19, 7, 16
    for x in range(x0, x1 + 1):
        top = 8 - (x - x0) * 4 // (x1 - x0)
        for z in range(z0, z1 + 1):
            for y in range(1, top + 1):
                wall = x in (x0, x1) or z in (z0, z1) or y == top
                s.set(x, y, z, mc("purple_terracotta") if wall else mc("air"))
        for z in (z0, z1):
            s.set(x, top, z, mc("black_terracotta"))
    for (hx, hy, hz) in ((x0, 4, 10), (x0, 2, 13), (x1, 2, 9), (14, 5, z1), (12, 3, z1)):
        s.set(hx, hy, hz, mc("crying_obsidian"))
    s.fill(12, 1, z0, 14, 3, z0, mc("air"))           # door
    s.set(11, 4, z0, mc("red_stained_glass"))
    s.set(16, 3, z0, mc("red_stained_glass"))
    chest(s, 17, 1, 15, "west", "guhs:chests/evil_mika_home")
    s.set(10, 1, 15, mc("crying_obsidian"))
    s.set(10, 1, 14, mc("cobweb"))
    s.set(18, 3, 8, mc("cobweb"))

    # a dark red ball on top
    s.fill(12, 9, 10, 14, 9, 12, mc("black_concrete"))
    s.sphere(13, 12, 11, 2.6, mc("red_stained_glass"), shell=1)

    # crying obsidian corner spikes
    for (px, pz, h) in ((1, 9, 4), (20, 9, 3), (10, 1, 3), (10, 18, 2)):
        s.fill(px, 1, pz, px, h, pz, mc("crying_obsidian"))

    # the Mikas
    for (mx, mz) in ((13.5, 11.5), (16.5, 9.5)):
        s.entity(mx, 1.0, mz, {"id": "guhs:mika", "PersistenceRequired": Byte(1)})
    s.clear_above(footprint, 1)
    s.save("evil_mika_home")


# ---------------------------------------------------------------------------------------------------------------------
# Hungry Guh picnic
# ---------------------------------------------------------------------------------------------------------------------
def guh_picnic():
    W, H, D = 13, 9, 13
    s = Structure((W, H, D))
    footprint = [(x, z) for x in range(2, 11) for z in range(2, 11)]
    for x, z in footprint:                                  # checkered picnic blanket
        s.set(x, 0, z, mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
    # parasol
    s.fill(3, 1, 9, 3, 5, 9, mc("birch_fence"))
    for x in range(0, 7):
        for z in range(6, 13):
            d = math.dist((x, z), (3, 9))
            if d <= 3.2:
                angle = math.degrees(math.atan2(z - 9, x - 3)) % 360
                s.set(x, 6 if d > 1.6 else 7, z, mc("pink_wool") if int(angle // 45) % 2 else mc("white_wool"))
    # picnic things
    s.set(8, 1, 8, "guhs:guh_taart", {"bites": "0"})
    s.set(7, 1, 4, "guhs:pink_kussen", {"facing": "south"})
    s.set(5, 1, 9, "guhs:frying_pan", {"facing": "north", "filled": "false"})   # a hint what to do...
    s.set(4, 1, 4, "guhs:block_of_kaasknabbels")
    s.set(2, 1, 2, mc("potted_pink_tulip"))
    s.set(10, 1, 10, mc("potted_allium"))
    s.set(10, 1, 2, mc("potted_pink_tulip"))
    chest(s, 9, 1, 4, "west", "guhs:chests/guh_picnic")
    # a jukebox with the guh record in it: it plays when someone comes near (quest/PicknickMuziek)
    s.set(10, 1, 6, mc("jukebox"), {"has_record": "true"},
          {"id": "minecraft:jukebox", "RecordItem": {"id": "guhs:music_disc_ze_hangen", "count": 1}})
    # the Hungry Guh, sitting in the middle of the blanket
    s.entity(6.5, 1.0, 6.5, {"id": "guhs:quest_guh", "PersistenceRequired": Byte(1), "Rotation": floats(0.0, 0.0)})
    s.clear_above(footprint, 1)
    s.save("guh_picnic")


# ---------------------------------------------------------------------------------------------------------------------
# Building blocks for the playset structures
# ---------------------------------------------------------------------------------------------------------------------
def plate(s, cx, cz, rx, rz, main="magenta_concrete", dots="pink_concrete"):
    """Rounded base plate with polka dots; returns the footprint (for clearing the air above it)."""
    footprint = []
    for x in range(s.size[0]):
        for z in range(s.size[2]):
            if ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 <= 1.0:
                footprint.append((x, z))
                s.set(x, 0, z, mc(dots) if (x * 3 + z * 5) % 11 == 0 else mc(main))
    return footprint


def flower_wheel(s, cx, cy, z, r, rim="pink_concrete", scallop="pink_terracotta", stand="purple_concrete"):
    """Standing flower-shaped running wheel (in the X/Y plane at depth z..z+1) on a purple stand behind it."""
    s.fill(cx - 1, 1, z + 3, cx + 1, cy, z + 4, mc(stand))
    s.fill(cx - 3, 1, z + 2, cx + 3, 2, z + 5, mc(stand))
    s.ring_xy(cx, cy, z, z + 1, r, r - 1.4, mc(rim))
    for a in range(0, 360, 30):
        rx, ry = cx + (r + 0.9) * math.cos(math.radians(a)), cy + (r + 0.9) * math.sin(math.radians(a))
        s.set(rx, ry, z, mc(scallop))
        s.set(rx, ry, z + 1, mc(scallop))
    for a in range(0, 360, 72):
        for rr in range(1, int(r)):
            s.set(cx + rr * math.cos(math.radians(a)), cy + rr * math.sin(math.radians(a)), z + 2, mc(scallop))
    s.fill(cx - 1, cy - 1, z + 1, cx + 1, cy + 1, z + 2, mc("red_concrete"))


def cheese_house(s, x0, x1, z0, z1, high, low, door_x, loot=None, spawner=None, wall="yellow_terracotta"):
    """Cheese wedge house (slopes down to the right) with cheese holes, a door and roses on the front."""
    for x in range(x0, x1 + 1):
        top = high - (x - x0) * (high - low) // max(1, x1 - x0)
        for z in range(z0, z1 + 1):
            for y in range(1, top + 1):
                edge = x in (x0, x1) or z in (z0, z1) or y == top
                s.set(x, y, z, mc(wall) if edge else mc("air"))
    for i, (hx, hz) in enumerate(((x0, z0 + 3), (x1, z1 - 3), ((x0 + x1) // 2, z1), (x0 + 2, z1), (x1, z0 + 2))):
        s.set(hx, 2 + (i * 3) % max(2, low - 1), hz, mc("orange_terracotta"))
    s.fill(door_x - 1, 1, z0, door_x + 1, 3, z0, mc("air"))
    s.set(door_x, 4, z0, mc("air"))
    for fx in range(x0 + 2, x1 - 1, 4):
        fy = max(2, high - (fx - x0) * (high - low) // max(1, x1 - x0) - 2)
        s.set(fx, fy, z0, mc("red_concrete"))
        s.set(fx - 1, fy, z0, mc("lime_concrete"))
    if spawner:
        s.set(*spawner, "guhs:guh_spawner")
    if loot:
        chest(s, x0 + 2, 1, z1 - 1, "south", loot)


def glass_ball(s, cx, cy, cz, r, glass="pink_stained_glass", cup="magenta_concrete"):
    cup_y = int(cy - r)
    s.fill(cx - 2, cup_y, cz - 2, cx + 2, cup_y, cz + 2, mc(cup))
    s.sphere(cx, cy, cz, r, mc(glass), shell=1)


def apple_house(s, cx, cy, cz, r=2.4):
    s.sphere(cx, cy, cz, r, mc("red_concrete"), shell=1)
    s.set(cx, cy + r + 1, cz, mc("spruce_log"))
    s.set(cx + 1, cy + r + 1, cz, mc("lime_concrete"))
    s.fill(cx - 1, cy - 1, cz - int(r), cx, cy, cz - int(r), mc("air"))


def water_bottle(s, x, z, y0, h=6):
    s.fill(x, y0, z, x + 1, y0 + h - 1, z + 1, mc("light_blue_stained_glass"))
    s.fill(x, y0 + h, z, x + 1, y0 + h, z + 1, mc("white_concrete"))
    s.set(x, y0 + 1, z - 1, mc("iron_bars"))


def food_bowl(s, x, z, snack="guhs:block_of_kaasknabbels"):
    for dx in range(4):
        for dz in range(2):
            s.set(x + dx, 1, z + dz, mc("purple_concrete"))
    s.set(x + 1, 1, z, snack)
    s.set(x + 2, 1, z, snack)


def guh_portrait(s, x0, y0, z, facing_south=True):
    """A 7x7 wool pixel-art portrait of a guh face, on a wall (in the X/Y plane at depth z)."""
    art = [
        "M.....M",
        "MPPPPPM",
        "PkbPkbP",
        "PkwPkwP",
        "PPPmPPP",
        "PPmPmPP",
        ".PPPPP.",
    ]
    colours = {"M": "magenta_wool", "P": "pink_wool", "k": "black_wool", "b": "light_blue_wool", "w": "white_wool",
               "m": "magenta_wool"}
    for row, line in enumerate(art):
        for col, ch in enumerate(line):
            if ch in colours:
                s.set(x0 + (col if facing_south else 6 - col), y0 + 6 - row, z, mc(colours[ch]))


def big_wheel_block(s, x, y, z, facing="south"):
    """A placed Guh Wheel (3x3) with its invisible parts, facing `facing` (south or north)."""
    s.set(x, y, z, "guhs:guh_wheel", {"facing": facing, "running": "false"})
    along = 1 if facing == "north" else -1  # clockwise of north is east (+x), of south is west (-x)
    for side in range(3):
        for height in range(3):
            if side != 1 or height != 0:
                s.set(x + along * (side - 1), y + height, z, "guhs:guh_wheel_part",
                      {"facing": facing, "side": str(side), "height": str(height)})


def guh(s, x, y, z, scale=1.0, **extra):
    s.entity(x, y, z, guh_nbt(scale, **extra))


# ---------------------------------------------------------------------------------------------------------------------
# Medium hamster house
# ---------------------------------------------------------------------------------------------------------------------
def hamster_house_medium():
    W, H, D = 46, 32, 40
    s = Structure((W, H, D))
    footprint = plate(s, 22.5, 19.5, 23.2, 20.2)
    flower_wheel(s, 9, 11, 29, 7)
    flower_wheel(s, 37, 8, 6, 5, rim="lime_concrete", scallop="yellow_terracotta")
    cheese_house(s, 24, 38, 22, 34, 11, 5, 29, loot="guhs:chests/hamster_house", spawner=(30, 1, 30))
    s.tube([(28, 10, 28), (27, 14, 27), (25, 17, 25), (22, 19, 22)], mc("yellow_stained_glass"))
    glass_ball(s, 20, 23, 21, 4.5)
    s.tube([(9, 19, 30), (14, 22, 28), (17, 23, 23)], mc("yellow_stained_glass"))
    s.tube([(35, 9, 26), (38, 13, 20), (39, 15, 14)], mc("lime_stained_glass"))
    glass_ball(s, 39, 19, 12, 3.5, glass="light_blue_stained_glass")
    s.fill(28, 15, 12, 40, 15, 13, mc("purple_concrete"))
    apple_house(s, 30, 18, 12)
    water_bottle(s, 17, 34, 3, 7)
    food_bowl(s, 6, 12)
    food_bowl(s, 40, 30)
    guh(s, 20.5, 1.0, 11.5, scale=7.5, health=300, Rotation=floats(-25.0, 0.0))
    guh(s, 8.5, 1.0, 20.5, scale=1.1)
    guh(s, 34.5, 1.0, 18.5, scale=0.8)
    guh(s, 20.5, 20.0, 21.5, scale=0.6)
    s.clear_above(footprint, 1)
    s.save("hamster_house_medium")


# ---------------------------------------------------------------------------------------------------------------------
# Large hamster house: a whole hamster playground city
# ---------------------------------------------------------------------------------------------------------------------
def hamster_house_large():
    W, H, D = 72, 48, 64
    s = Structure((W, H, D))
    footprint = plate(s, 35.5, 31.5, 36.2, 32.2)
    # three wheels, one huge
    flower_wheel(s, 12, 16, 48, 11)
    flower_wheel(s, 60, 9, 50, 6, rim="lime_concrete", scallop="yellow_terracotta")
    flower_wheel(s, 62, 8, 10, 5, rim="light_blue_concrete", scallop="white_concrete")
    # cheese castle: two wedges and a round tower
    cheese_house(s, 28, 46, 38, 56, 14, 7, 36, loot="guhs:chests/hamster_house", spawner=(40, 1, 50))
    cheese_house(s, 47, 58, 30, 42, 9, 4, 52, loot="guhs:chests/hamster_house", spawner=(53, 1, 38))
    for y in range(1, 30):
        for x in range(20, 27):
            for z in range(24, 31):
                d = math.dist((x, z), (23, 27))
                if 2.2 <= d <= 3.4:
                    s.set(x, y, z, mc("purple_concrete") if y % 6 else mc("magenta_concrete"))
                elif d < 2.2:
                    s.set(x, y, z, mc("air"))
    glass_ball(s, 23, 35, 27, 5.5)
    guh(s, 23.5, 31.0, 27.5, scale=0.7)
    # tube network
    s.tube([(36, 13, 46), (30, 18, 40), (26, 24, 33), (24, 29, 29)], mc("yellow_stained_glass"))
    s.tube([(12, 28, 50), (16, 31, 42), (20, 33, 32)], mc("yellow_stained_glass"))
    s.tube([(52, 10, 36), (58, 14, 26), (60, 17, 16)], mc("lime_stained_glass"))
    glass_ball(s, 60, 21, 14, 4, glass="light_blue_stained_glass")
    s.tube([(44, 12, 44), (50, 20, 30), (48, 26, 18)], mc("yellow_stained_glass"))
    glass_ball(s, 47, 30, 16, 4)
    # seesaw with two apple houses, a slide, bottles, bowls
    s.fill(30, 19, 14, 56, 19, 15, mc("purple_concrete"))
    apple_house(s, 33, 22, 14)
    apple_house(s, 54, 22, 14, r=2.8)
    s.line((8, 26, 44), (4, 2, 30), mc("pink_concrete"), thickness=1)
    water_bottle(s, 26, 58, 3, 9)
    water_bottle(s, 64, 30, 3, 7)
    for (bx, bz) in ((8, 16), (20, 10), (44, 8), (64, 40), (14, 34)):
        food_bowl(s, bx, bz)
    # the MEGA guh (~14 blocks), a giant guh and friends
    guh(s, 34.5, 1.0, 17.5, scale=9.5, health=500, Rotation=floats(15.0, 0.0))
    guh(s, 12.5, 1.0, 28.5, scale=6.9, health=250, Rotation=floats(-60.0, 0.0))
    for (gx, gz, sc) in ((50.5, 24.5, 1.2), (58.5, 32.5, 0.8), (26.5, 40.5, 1.0), (6.5, 20.5, 0.6), (64.5, 20.5, 1.6)):
        guh(s, gx, 1.0, gz, scale=sc)
    s.clear_above(footprint, 1)
    s.save("hamster_house_large")


# ---------------------------------------------------------------------------------------------------------------------
# Cheese fountain: tiers of kaasknabbels with kaas saus flowing through
# ---------------------------------------------------------------------------------------------------------------------
def cheese_fountain():
    W, H, D = 17, 14, 17
    s = Structure((W, H, D))
    c = 8
    footprint = []
    KB = "guhs:block_of_kaasknabbels"
    SAUS = ("guhs:kaas_saus", {"level": "0"})
    for x in range(W):
        for z in range(D):
            d = math.dist((x, z), (c, c))
            if d <= 8.4:
                footprint.append((x, z))
                s.set(x, 0, z, mc("pink_concrete") if d > 7.2 else KB)   # plaza + basin floor
                if 6.2 < d <= 7.2:
                    s.set(x, 1, z, KB)                                     # basin rim
                elif d <= 6.2:
                    s.set(x, 1, z, *SAUS)                                  # basin full of kaas saus
    s.fill(c - 1, 1, c - 1, c + 1, 10, c + 1, KB)                          # central column
    for (y, r) in ((5, 3.6), (8, 2.4)):                                    # two open bowls: the saus spills over the edge
        for x in range(W):
            for z in range(D):
                if math.dist((x, z), (c, c)) <= r + 0.3:
                    s.set(x, y, z, KB)
    s.set(c, 11, c, *SAUS)                                                 # the spout: flows down bowl after bowl
    for (x, z) in ((0, 8), (16, 8), (8, 0), (8, 16)):                     # on the plaza (in the rim the saus washed them away)
        s.set(x, 1, z, mc("potted_pink_tulip"))
    guh(s, 3.5, 1.0, 3.5, scale=0.8)
    guh(s, 13.5, 1.0, 12.5, scale=1.1)
    s.clear_above(footprint, 1)
    s.save("cheese_fountain")


# ---------------------------------------------------------------------------------------------------------------------
# Guh caves: an underground network of hamster tubes and rooms (a jigsaw structure, like the ancient city)
# ---------------------------------------------------------------------------------------------------------------------
TUBE_GLASS = "yellow_stained_glass"


def jigsaw(s, x, y, z, facing, final="minecraft:air"):
    """A jigsaw connector facing out of the piece; the game replaces it by `final` after connecting."""
    s.set(x, y, z, mc("jigsaw"), {"orientation": f"{facing}_up"},
          {"id": "minecraft:jigsaw", "name": "guhs:tube", "target": "guhs:tube", "pool": "guhs:guh_caves/tubes",
           "joint": "rollable", "final_state": final, "selection_priority": 0, "placement_priority": 0})


def tube_straight():
    s = Structure((5, 5, 9))
    for x in range(5):
        for y in range(5):
            for z in range(9):
                shell = x in (0, 4) or y in (0, 4)
                s.set(x, y, z, mc(TUBE_GLASS) if shell else mc("air"))
    for z in (2, 6):                                      # little lights along the tube
        s.set(2, 4, z, mc("pearlescent_froglight"))
    jigsaw(s, 2, 2, 0, "north")
    jigsaw(s, 2, 2, 8, "south")
    s.save("guh_caves/tube_straight")


def tube_junction():
    s = Structure((7, 5, 7))
    for x in range(7):
        for y in range(5):
            for z in range(7):
                shell = x in (0, 6) or y in (0, 4) or z in (0, 6)
                s.set(x, y, z, mc(TUBE_GLASS) if shell else mc("air"))
    s.set(3, 4, 3, mc("pearlescent_froglight"))
    for (x, z, f) in ((3, 0, "north"), (3, 6, "south"), (0, 3, "west"), (6, 3, "east")):
        s.fill(x - (1 if f in ("north", "south") else 0), 1, z - (1 if f in ("west", "east") else 0),
               x + (1 if f in ("north", "south") else 0), 3, z + (1 if f in ("west", "east") else 0), mc("air"))
        jigsaw(s, x, 2, z, f)
    s.save("guh_caves/tube_junction")


def tube_end():
    s = Structure((5, 5, 2))
    for x in range(5):
        for y in range(5):
            s.set(x, y, 1, mc(TUBE_GLASS))
            s.set(x, y, 0, mc(TUBE_GLASS) if x in (0, 4) or y in (0, 4) else mc("air"))
    s.set(2, 2, 1, mc("pink_stained_glass"))
    jigsaw(s, 2, 2, 0, "north")
    s.save("guh_caves/tube_end")


def room_shell(s, wall="pink_concrete", trim="white_concrete", floor="birch_planks"):
    W, H, D = s.size
    for x in range(W):
        for y in range(H):
            for z in range(D):
                edge = x in (0, W - 1) or z in (0, D - 1)
                if y == 0:
                    s.set(x, y, z, mc(floor))
                elif y == H - 1:
                    s.set(x, y, z, mc(wall))
                elif edge:
                    s.set(x, y, z, mc(trim) if y == 1 or y == H - 2 else mc(wall))
                else:
                    s.set(x, y, z, mc("air"))


def door(s, x, z, facing):
    """A 3x3 opening in the wall with a connector in the middle."""
    if facing in ("north", "south"):
        s.fill(x - 1, 1, z, x + 1, 3, z, mc("air"))
    else:
        s.fill(x, 1, z - 1, x, 3, z + 1, mc("air"))
    jigsaw(s, x, 2, z, facing)


def central_room():
    """The big hamster room in the middle of every guh cave: bedding, hay, a guh wheel, guh portraits, a chest."""
    s = Structure((21, 10, 21))
    room_shell(s)
    for x in range(1, 20):                       # hamster bedding
        for z in range(1, 20):
            if (x * 7 + z * 3) % 5 == 0:
                s.set(x, 1, z, mc("yellow_carpet"))
    for (x, z) in ((4, 4), (16, 4), (4, 16), (16, 16), (10, 10)):
        s.set(x, 9, z, mc("pearlescent_froglight"))
    s.fill(2, 1, 2, 4, 1, 4, mc("hay_block"))
    s.fill(2, 1, 15, 4, 1, 18, mc("hay_block"))
    for z, south in ((0, True), (20, False)):   # wool guh "paintings" on the north and south walls
        guh_portrait(s, 1, 2, z, facing_south=south)
        guh_portrait(s, 13, 2, z, facing_south=south)
    big_wheel_block(s, 16, 1, 18, facing="north")
    food_bowl(s, 12, 3)
    chest(s, 19, 1, 9, "west", "guhs:chests/guh_caves")
    s.set(19, 1, 11, "guhs:frying_pan", {"facing": "west", "filled": "false"})
    guh(s, 7.5, 1.0, 12.5, scale=0.9)
    guh(s, 13.5, 1.0, 7.5, scale=1.3)
    for (x, z, f) in ((10, 0, "north"), (10, 20, "south"), (0, 10, "west"), (20, 10, "east")):
        door(s, x, z, f)
    s.save("guh_caves/central_room")


def nest_room():
    s = Structure((9, 6, 9))
    room_shell(s, wall="pink_wool", trim="white_wool")
    s.fill(1, 1, 1, 7, 1, 7, mc("yellow_carpet"))
    s.fill(2, 1, 5, 4, 1, 7, mc("hay_block"))
    s.set(4, 5, 4, mc("pearlescent_froglight"))
    guh(s, 3.5, 2.0, 6.5, scale=0.7)
    guh(s, 5.5, 1.0, 3.5, scale=0.5)
    door(s, 4, 0, "north")
    s.save("guh_caves/nest_room")


def pantry_room():
    s = Structure((9, 6, 9))
    room_shell(s, wall="yellow_terracotta", trim="orange_terracotta")
    s.fill(1, 1, 6, 7, 2, 7, "guhs:block_of_kaasknabbels")
    for x in range(3, 6):                        # a little pool of kaas saus
        for z in range(3, 5):
            s.set(x, 0, z, "guhs:kaas_saus", {"level": "0"})
    chest(s, 1, 1, 1, "south", "guhs:chests/guh_caves")
    s.set(4, 5, 4, mc("pearlescent_froglight"))
    door(s, 4, 0, "north")
    s.save("guh_caves/pantry_room")


def portrait_room():
    s = Structure((9, 7, 9))
    room_shell(s, wall="white_concrete", trim="pink_concrete", floor="pink_wool")
    guh_portrait(s, 1, 0, 8, facing_south=False)
    s.set(4, 6, 4, mc("pearlescent_froglight"))
    s.set(1, 1, 4, mc("potted_pink_tulip"))
    s.set(7, 1, 4, mc("potted_allium"))
    door(s, 4, 0, "north")
    s.save("guh_caves/portrait_room")


def guh_caves():
    central_room()
    tube_straight()
    tube_junction()
    tube_end()
    nest_room()
    pantry_room()
    portrait_room()


# ---------------------------------------------------------------------------------------------------------------------
# Challenging guh caves: the hostile version (a jigsaw dungeon), centred on the dungeon hall
# ---------------------------------------------------------------------------------------------------------------------
def dungeon_jigsaw(s, x, y, z, facing, final="minecraft:air"):
    s.set(x, y, z, mc("jigsaw"), {"orientation": f"{facing}_up"},
          {"id": "minecraft:jigsaw", "name": "guhs:dungeon", "target": "guhs:dungeon",
           "pool": "guhs:challenging_guh_caves/tubes", "joint": "rollable", "final_state": final,
           "selection_priority": 0, "placement_priority": 0})


def dungeon_door(s, x, y, z, facing):
    if facing in ("north", "south"):
        s.fill(x - 1, y - 1, z, x + 1, y + 1, z, mc("air"))
    else:
        s.fill(x, y - 1, z - 1, x, y + 1, z + 1, mc("air"))
    dungeon_jigsaw(s, x, y, z, facing)


def spawner(s, x, y, z, entity):
    s.set(x, y, z, mc("spawner"), None, {"id": "minecraft:mob_spawner", "Delay": Short(20),
                                        "SpawnData": {"entity": {"id": entity}}, "SpawnCount": Short(2),
                                        "MaxNearbyEntities": Short(5), "RequiredPlayerRange": Short(14)})


def evil_shell(s, wall="polished_blackstone_bricks", floor="blackstone", trim="crying_obsidian"):
    W, H, D = s.size
    for x in range(W):
        for y in range(H):
            for z in range(D):
                edge = x in (0, W - 1) or z in (0, D - 1)
                corner = x in (0, W - 1) and z in (0, D - 1)
                if y == 0:
                    s.set(x, y, z, mc(floor))
                elif y == H - 1:
                    s.set(x, y, z, mc("blackstone"))
                elif corner:
                    s.set(x, y, z, mc(trim))
                elif edge:
                    s.set(x, y, z, mc("magenta_terracotta") if y == H - 2 else mc(wall))
                else:
                    s.set(x, y, z, mc("air"))


def mika_nbt(scale=1.0, health=None, boss=False):
    nbt = {"id": "guhs:mika", "PersistenceRequired": Byte(1), "Boss": Byte(1 if boss else 0),
           "attributes": compounds([{"id": "minecraft:scale", "base": Double(scale)}])}
    if health:
        nbt["attributes"].append({"id": "minecraft:max_health", "base": Double(health)})
        nbt["Health"] = Float(health)
    return nbt


def dungeon_hall():
    """The heart of a challenging guh cave: Big Mika, Mika spawners, a magma ring and the treasure."""
    s = Structure((25, 12, 25))
    evil_shell(s)
    c = 12
    for x in range(1, 24):                                       # checkered evil floor
        for z in range(1, 24):
            s.set(x, 0, z, mc("red_nether_bricks") if (x + z) % 2 else mc("black_concrete"))
            d = math.dist((x, z), (c, c))
            if 5.5 < d <= 7:
                s.set(x, 0, z, mc("magma_block"))                 # hot ring around the treasure
    for x in range(c - 4, c + 5):                                # treasure platform
        for z in range(c - 4, c + 5):
            if math.dist((x, z), (c, c)) <= 4.5:
                s.set(x, 1, z, mc("crying_obsidian") if (x + z) % 3 == 0 else mc("black_concrete"))
    chest(s, c - 1, 2, c, "south", "guhs:chests/challenging_guh_caves_treasure")
    chest(s, c + 1, 2, c, "south", "guhs:chests/challenging_guh_caves_treasure")
    for (x, z) in ((3, 3), (21, 3), (3, 21), (21, 21)):          # crying obsidian pillars with soul lights
        s.fill(x, 1, z, x, 10, z, mc("crying_obsidian"))
        s.set(x, 7, z + (1 if z < c else -1), mc("soul_lantern"), {"hanging": "false", "waterlogged": "false"})
    spawner(s, 5, 1, c, "guhs:mika")
    spawner(s, 19, 1, c, "guhs:mika")
    s.fill(c - 1, 11, c - 1, c + 1, 11, c + 1, mc("shroomlight"))
    s.entity(c + 0.5, 2.0, c - 2.5, mika_nbt(2.5, boss=True))     # Big Mika guards the treasure (hurts! see MikaEntity.makeBoss)
    s.entity(8.5, 1.0, 17.5, mika_nbt(1.1))
    s.entity(16.5, 1.0, 7.5, mika_nbt(0.9))
    for (x, z, f) in ((c, 0, "north"), (c, 24, "south"), (0, c, "west"), (24, c, "east")):
        dungeon_door(s, x, 2, z, f)
    s.save("challenging_guh_caves/dungeon_hall")


def dungeon_tube():
    s = Structure((5, 5, 9))
    for x in range(5):
        for y in range(5):
            for z in range(9):
                shell = x in (0, 4) or y in (0, 4)
                if not shell:
                    s.set(x, y, z, mc("air"))
                elif y == 0:
                    s.set(x, y, z, mc("magma_block") if z in (3, 5) and x == 2 else mc("blackstone"))
                else:
                    s.set(x, y, z, mc("red_stained_glass") if z % 4 == 2 else mc("black_stained_glass"))
    s.set(2, 4, 4, mc("shroomlight"))
    dungeon_jigsaw(s, 2, 2, 0, "north")
    dungeon_jigsaw(s, 2, 2, 8, "south")
    s.save("challenging_guh_caves/tube_straight")


def dungeon_junction():
    s = Structure((7, 5, 7))
    for x in range(7):
        for y in range(5):
            for z in range(7):
                shell = x in (0, 6) or y in (0, 4) or z in (0, 6)
                s.set(x, y, z, (mc("blackstone") if y == 0 else mc("black_stained_glass")) if shell else mc("air"))
    s.set(3, 4, 3, mc("shroomlight"))
    for (x, z, f) in ((3, 0, "north"), (3, 6, "south"), (0, 3, "west"), (6, 3, "east")):
        dungeon_door(s, x, 2, z, f)
    s.save("challenging_guh_caves/tube_junction")


def dungeon_end():
    s = Structure((5, 5, 2))
    for x in range(5):
        for y in range(5):
            s.set(x, y, 1, mc("crying_obsidian") if (x, y) == (2, 2) else mc("black_stained_glass"))
            s.set(x, y, 0, mc("black_stained_glass") if x in (0, 4) or y in (0, 4) else mc("air"))
    dungeon_jigsaw(s, 2, 2, 0, "north")
    s.save("challenging_guh_caves/tube_end")


def spawner_room():
    s = Structure((11, 7, 11))
    evil_shell(s)
    spawner(s, 2, 1, 2, "minecraft:zombie")
    spawner(s, 8, 1, 8, "minecraft:skeleton")
    chest(s, 5, 1, 5, "north", "guhs:chests/challenging_guh_caves")
    for (x, z) in ((2, 8), (8, 2), (5, 2), (5, 8)):
        s.set(x, 1, z, mc("cobweb"))
    s.set(5, 6, 5, mc("shroomlight"))
    dungeon_door(s, 5, 2, 0, "north")
    dungeon_door(s, 5, 2, 10, "south")
    s.save("challenging_guh_caves/spawner_room")


def parkour_room():
    """Jump across kaasknabbel pillars over a magma pit; the chest waits on the far side."""
    s = Structure((13, 11, 13))
    evil_shell(s, floor="magma_block")
    for x in range(1, 12):                                       # ledges at both ends
        for z in (1, 2):
            s.set(x, 4, z, mc("blackstone"))
        for z in (10, 11):
            s.set(x, 4, z, mc("blackstone"))
    for (x, z) in ((6, 4), (4, 6), (7, 7), (5, 9)):             # stepping pillars
        s.fill(x, 1, z, x, 4, z, "guhs:block_of_kaasknabbels")
    for y in range(1, 5):                                        # ladders out of the pit
        s.set(1, y, 6, mc("ladder"), {"facing": "east", "waterlogged": "false"})
        s.set(11, y, 6, mc("ladder"), {"facing": "west", "waterlogged": "false"})
    chest(s, 6, 5, 11, "north", "guhs:chests/challenging_guh_caves")
    s.fill(5, 10, 5, 7, 10, 7, mc("shroomlight"))
    dungeon_door(s, 6, 6, 0, "north")
    dungeon_door(s, 6, 6, 12, "south")
    s.save("challenging_guh_caves/parkour_room")


def mika_den():
    s = Structure((9, 7, 9))
    evil_shell(s, wall="black_concrete", floor="magenta_wool")
    spawner(s, 4, 1, 6, "guhs:mika")
    chest(s, 1, 1, 7, "east", "guhs:chests/challenging_guh_caves")
    s.set(4, 6, 4, mc("shroomlight"))
    for (x, z) in ((2.5, 3.5), (6.5, 4.5)):
        s.entity(x, 1.0, z, mika_nbt(1.0))
    dungeon_door(s, 4, 2, 0, "north")
    s.save("challenging_guh_caves/mika_den")


def challenging_guh_caves():
    dungeon_hall()
    dungeon_tube()
    dungeon_junction()
    dungeon_end()
    spawner_room()
    parkour_room()
    mika_den()


# ---------------------------------------------------------------------------------------------------------------------
# Grand cheese fountain: the big, cooler one
# ---------------------------------------------------------------------------------------------------------------------
def grand_cheese_fountain():
    W, H, D = 31, 24, 31
    s = Structure((W, H, D))
    c = 15
    KB = "guhs:block_of_kaasknabbels"
    SAUS = ("guhs:kaas_saus", {"level": "0"})
    footprint = []
    for x in range(W):
        for z in range(D):
            d = math.dist((x, z), (c, c))
            if d <= 15.4:
                footprint.append((x, z))
                ring = (int(d) % 3 == 0)
                s.set(x, 0, z, mc("pink_concrete") if ring else mc("white_concrete"))   # plaza
                if 11.2 < d <= 12.3:
                    s.fill(x, 1, z, x, 2, z, KB)                                        # big basin wall
                elif d <= 11.2:
                    s.set(x, 0, z, KB)
                    s.fill(x, 1, z, x, 2, z, *SAUS)                                     # a deep pool of kaas saus
    s.fill(c - 2, 1, c - 2, c + 2, 16, c + 2, KB)                                      # central column
    for (y, r) in ((6, 7.2), (11, 5.0), (15, 3.2)):                                    # three open bowls (cascade)
        for x in range(W):
            for z in range(D):
                if math.dist((x, z), (c, c)) <= r + 0.3:
                    s.set(x, y, z, KB if math.dist((x, z), (c, c)) < r - 0.7 else mc("pink_concrete"))
    s.sphere(c, 19, c, 2.6, "guhs:compressed_super_vahoege_vads")                      # a vads orb on top...
    s.set(c, 22, c, *SAUS)                                                             # ...with the spout
    for (x, z) in ((c - 7, c - 7), (c + 7, c - 7), (c - 7, c + 7), (c + 7, c + 7)):    # four jets in the pool
        s.fill(x, 1, z, x, 8, z, KB)
        s.set(x, 9, z, mc("shroomlight"))
        s.set(x, 10, z, mc("pink_concrete"))
        s.set(x, 11, z, *SAUS)
    for a in range(0, 360, 45):                                                        # flower beds on the plaza
        x = int(round(c + 13.8 * math.cos(math.radians(a))))
        z = int(round(c + 13.8 * math.sin(math.radians(a))))
        s.set(x, 1, z, mc("potted_pink_tulip") if a % 90 else mc("potted_allium"))
    chest(s, c, 1, 1, "south", "guhs:chests/guh_picnic")
    guh(s, 5.5, 1.0, 15.5, scale=1.4)
    guh(s, 25.5, 1.0, 12.5, scale=0.8)
    guh(s, 15.5, 1.0, 28.5, scale=2.0)
    s.clear_above(footprint, 1)
    s.save("grand_cheese_fountain")


# ---------------------------------------------------------------------------------------------------------------------
# Guh statue: the guh model (assets/guhs/geckolib/models/entity/guh.geo.json) built out of blocks, coloured from its texture
# ---------------------------------------------------------------------------------------------------------------------
STATUE_BONES = ("body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right",
                "leg_front_left", "leg_front_right")


STATUE_EXTRA_COLOURS = {"purpur_block": (170, 125, 170), "cherry_planks": (226, 178, 172)}


def _lab(rgb):
    def lin(c):
        c /= 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (lin(c) for c in rgb)
    xyz = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.9505, 0.2126 * r + 0.7152 * g + 0.0722 * b,           (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.089
    f = [t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116 for t in xyz]
    return 116 * f[1] - 16, 500 * (f[0] - f[1]), 200 * (f[1] - f[2])


def nearest_block(rgb):
    """The wool / concrete / terracotta (or purpur, cherry) block that looks most like this colour."""
    colours = {**BLOCK_COLOURS, **STATUE_EXTRA_COLOURS}
    lab = _lab(rgb)
    return min(colours, key=lambda k: sum((a - b) ** 2 for a, b in zip(_lab(colours[k]), lab)))


def voxel_guh(s, ox, oy, oz, scale=1.5, bones=STATUE_BONES, texture="guh.png", face_south=False, bounds_bones=None):
    """Voxelizes the guh model into `s`, with the model's feet at y=oy and its front (face) towards -z."""
    from PIL import Image
    here = os.path.dirname(os.path.abspath(__file__))
    res = os.path.join(here, "..", "src", "main", "resources", "assets", "guhs")
    geo = json.load(open(os.path.join(res, "geckolib", "models", "entity", "guh.geo.json")))["minecraft:geometry"][0]
    tex = Image.open(os.path.join(res, "textures", "entity", texture)).convert("RGBA")
    cubes = [c for b in geo["bones"] if b["name"] in bones for c in b.get("cubes", [])]
    frame = [c for b in geo["bones"] if b["name"] in (bounds_bones or bones) for c in b.get("cubes", [])]
    lo = [min(c["origin"][i] for c in frame) for i in range(3)]
    hi = [max(c["origin"][i] + c["size"][i] for c in frame) for i in range(3)]

    def containing(p):
        for c in cubes:
            if all(c["origin"][i] <= p[i] < c["origin"][i] + c["size"][i] for i in range(3)):
                return c
        return None

    def sample(c, face, p):
        (x0, y0, z0), (sx, sy, sz) = c["origin"], c["size"]
        x1, y1, z1 = x0 + sx, y0 + sy, z0 + sz
        u, v = {"north": (x1 - p[0], y1 - p[1]), "south": (p[0] - x0, y1 - p[1]), "east": (z1 - p[2], y1 - p[1]),
                "west": (p[2] - z0, y1 - p[1]), "up": (p[0] - x0, p[2] - z0), "down": (p[0] - x0, z1 - p[2])}[face]
        f = c["uv"][face]
        tx = min(f["uv"][0] + f["uv_size"][0] - 0.01, f["uv"][0] + max(0.0, u))
        ty = min(f["uv"][1] + f["uv_size"][1] - 0.01, f["uv"][1] + max(0.0, v))
        # the texture image can be sharper than the model's UV space
        r, g, b, a = tex.getpixel((int(tx * tex.width / geo["description"]["texture_width"]),
                                   int(ty * tex.height / geo["description"]["texture_height"])))
        return (r, g, b) if a > 0 else None

    step = 1.0 / scale
    nx, ny, nz = (int(math.ceil((hi[i] - lo[i]) * scale)) for i in range(3))
    dirs = (("west", -1, 0, 0), ("east", 1, 0, 0), ("down", 0, -1, 0), ("up", 0, 1, 0), ("north", 0, 0, -1),
            ("south", 0, 0, 1))
    for bx in range(nx):
        for by in range(ny):
            for bz in range(nz):
                p = (lo[0] + (bx + 0.5) * step, lo[1] + (by + 0.5) * step, lo[2] + (bz + 0.5) * step)
                c = containing(p)
                if not c:
                    continue
                block = "pink_wool"
                for face, dx, dy, dz in dirs:
                    if not containing((p[0] + dx * step, p[1] + dy * step, p[2] + dz * step)):
                        rgb = sample(c, face, p)
                        if rgb:
                            block = nearest_block(rgb)
                        break
                # the model's +x is the guh's left: mirror so the statue isn't flipped
                if face_south:                           # turned around: facing +z
                    s.set(ox + bx, oy + by, oz + (nz - 1 - bz), mc(block))
                else:
                    s.set(ox + (nx - 1 - bx), oy + by, oz + bz, mc(block))
    return nx, ny, nz


def guh_statue():
    """A big guh made of blocks, on a little lawn. That's it. Quite rare."""
    s = Structure((40, 27, 50))
    footprint = []
    for x in range(40):
        for z in range(50):
            if ((x - 19.5) / 20) ** 2 + ((z - 24.5) / 25) ** 2 <= 1:
                footprint.append((x, z))
                s.set(x, 0, z, mc("pink_wool") if (x + 2 * z) % 9 else mc("pink_concrete"))
    voxel_guh(s, 3, 1, 1, scale=1.5)
    s.clear_above(footprint, 1)
    s.save("guh_statue")


# ---------------------------------------------------------------------------------------------------------------------
# Guhland: the extra extra large hamster house, a whole guh theme park (very rare)
# ---------------------------------------------------------------------------------------------------------------------
def flowing_fountain(s, c_x, c_z, y0=1):
    """A small cheese fountain (the saus flows from the spout over two open bowls into the basin)."""
    KB = "guhs:block_of_kaasknabbels"
    SAUS = ("guhs:kaas_saus", {"level": "0"})
    for x in range(c_x - 8, c_x + 9):
        for z in range(c_z - 8, c_z + 9):
            d = math.dist((x, z), (c_x, c_z))
            if 6.2 < d <= 7.2:
                s.set(x, y0, z, KB)
            elif d <= 6.2:
                s.set(x, y0 - 1, z, KB)
                s.set(x, y0, z, *SAUS)
    s.fill(c_x - 1, y0, c_z - 1, c_x + 1, y0 + 9, c_z + 1, KB)
    for (y, r) in ((y0 + 4, 3.6), (y0 + 7, 2.4)):
        for x in range(c_x - 4, c_x + 5):
            for z in range(c_z - 4, c_z + 5):
                if math.dist((x, z), (c_x, c_z)) <= r + 0.3:
                    s.set(x, y, z, KB)
    s.set(c_x, y0 + 10, c_z, *SAUS)


def food_stand(s, x0, z0, facing_east, stripe, goods):
    """A 7x5 snack stand with a striped awning; the counter faces the main path."""
    front = x0 + 6 if facing_east else x0
    back = x0 if facing_east else x0 + 6
    s.fill(x0, 1, z0, x0 + 6, 1, z0 + 4, mc("white_concrete"))
    s.fill(back, 2, z0, back, 4, z0 + 4, mc(stripe + "_concrete"))
    for z in (z0, z0 + 4):
        s.fill(front, 2, z, front, 4, z, mc("birch_fence"))
        s.fill(back, 2, z, back, 4, z, mc("birch_fence"))
    s.fill(front, 2, z0 + 1, front, 2, z0 + 3, mc("pink_terracotta"))       # counter
    for x in range(x0 - 1, x0 + 8):                                         # striped awning
        for z in range(z0 - 1, z0 + 6):
            s.set(x, 5, z, mc(stripe + "_wool") if (x + z) % 2 else mc("white_wool"))
    s.set(x0 + 3, 6, z0 + 2, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    mid = x0 + 3
    for i, g in enumerate(goods):
        name, props = (g if isinstance(g, tuple) else (g, None))
        s.set(front, 3, z0 + 1 + i, name, props)
    chest(s, mid, 2, z0 + 2, "east" if facing_east else "west", "guhs:chests/guh_picnic")


def ferris_wheel(s, cx, hub_y, z_front, r):
    z_back = z_front + 4
    for z in (z_front, z_back):                                             # two rims with spokes
        s.ring_xy(cx, hub_y, z, z, r, r - 1, mc("pink_concrete"))
        for a in range(0, 360, 30):
            ex, ey = cx + (r - 1) * math.cos(math.radians(a)), hub_y + (r - 1) * math.sin(math.radians(a))
            s.line((cx, hub_y, z), (ex, ey, z), mc("white_concrete"))
        for a in range(15, 360, 30):
            s.set(cx + (r + 0.6) * math.cos(math.radians(a)), hub_y + (r + 0.6) * math.sin(math.radians(a)), z,
                  mc("pearlescent_froglight"))
    s.fill(cx - 1, hub_y - 1, z_front - 2, cx + 1, hub_y + 1, z_back + 2, mc("magenta_concrete"))   # hub
    for z in (z_front - 2, z_back + 2):                                     # A-frame legs
        s.line((cx - 16, 1, z), (cx, hub_y, z), mc("purple_concrete"), thickness=1)
        s.line((cx + 16, 1, z), (cx, hub_y, z), mc("purple_concrete"), thickness=1)
    colours = ["yellow", "light_blue", "lime", "orange", "magenta", "red"]
    for i, a in enumerate(range(0, 360, 30)):                               # 12 gondolas, each with a sitting guh
        px = int(round(cx + r * math.cos(math.radians(a))))
        py = int(round(hub_y + r * math.sin(math.radians(a))))
        col = colours[i % len(colours)]
        s.fill(px, py, z_front, px, py, z_back, mc("purple_concrete"))      # axle between the rims
        s.fill(px - 1, py - 1, z_front + 1, px + 1, py - 1, z_back - 1, mc(col + "_wool"))   # roof
        for dx in (-1, 1):
            for dz in (1, 3):
                s.fill(px + dx, py - 3, z_front + dz, px + dx, py - 2, z_front + dz, mc("birch_fence"))
        s.fill(px - 1, py - 4, z_front + 1, px + 1, py - 4, z_back - 1, mc(col + "_concrete"))  # floor
        if i % 2 == 0:
            guh(s, px + 0.5, py - 3.0, z_front + 2.5, scale=0.6, Sitting=Byte(1))
    s.fill(cx - 4, 1, z_front - 1, cx + 4, hub_y - r - 5, z_back + 1, mc("white_concrete"))  # boarding platform


def carousel(s, cx, cz, r=9):
    for x in range(cx - r - 3, cx + r + 4):
        for z in range(cz - r - 3, cz + r + 4):
            d = math.dist((x, z), (cx, cz))
            if d <= r + 0.4:
                s.set(x, 1, z, mc("pink_wool") if int(d) % 2 else mc("white_wool"))
            if d <= r + 2.4:                                                # striped cone roof
                ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                s.set(x, 10 + int((r + 2.4 - d) * 0.55), z, mc("magenta_wool") if int(ang / 30) % 2 else mc("white_wool"))
    s.fill(cx, 2, cz, cx, 16, cz, mc("gold_block"))
    s.set(cx, 17, cz, mc("pearlescent_froglight"))
    for a in range(0, 360, 45):                                             # poles, a guh at each
        px, pz = cx + 6 * math.cos(math.radians(a)), cz + 6 * math.sin(math.radians(a))
        s.fill(int(round(px)), 2, int(round(pz)), int(round(px)), 9, int(round(pz)), mc("birch_fence"))
        guh(s, round(px) + 0.5 + (0.9 if a % 90 else -0.9) * 0, 2.0, round(pz) + 1.5 if a < 180 else round(pz) - 0.5,
            scale=0.7, Sitting=Byte(1))


def sled_coaster(s, y=7):
    """A guh sled coaster (tools/slee_track.py) on pillars with a hill, a station with a ladder and locked sleds."""
    import slee_track
    seq = (["straight"] * 11 + ["curve_left"] + ["straight"] * 3 + ["curve_left"] + ["straight"] * 2
           + ["slope", "straight", "down"] + ["straight"] * 6 + ["curve_left"] + ["straight"] * 3 + ["curve_left"])
    pieces = slee_track.build((114, y, 96), "north", seq)
    slee_track.place(s, pieces)
    for i, (anchor, facing, shape, _down) in enumerate(pieces):     # pillars under every other piece
        if i % 2 == 0:
            ax, ay, az = anchor
            s.fill(ax, 1, az, ax, ay - 1, az, mc("white_concrete"))
            s.set(ax, ay - 1, az, mc("pink_concrete"))
    # the station on the east side: platform, roof, fence, a ladder up and two locked sleds
    s.fill(117, y - 1, 66, 119, y - 1, 82, mc("birch_planks"))
    for z in range(66, 83):
        s.set(119, y, z, mc("birch_fence"))
    for z in (66, 82):
        s.fill(117, y, z, 118, y, z, mc("birch_fence"))
    for (x, z) in ((119, 66), (119, 82), (117, 66), (117, 82)):
        s.fill(x, y + 1, z, x, y + 3, z, mc("birch_fence"))
    s.fill(117, y + 4, 66, 119, y + 4, 82, mc("pink_wool"))
    s.fill(118, y + 5, 66, 118, y + 5, 82, mc("white_wool"))
    s.fill(120, 1, 74, 120, y - 1, 74, mc("white_concrete"))
    for yy in range(1, y + 1):
        s.set(121, yy, 74, mc("ladder"), {"facing": "east", "waterlogged": "false"})
    s.set(119, y, 74, mc("birch_fence_gate"), {"facing": "east", "open": "false", "in_wall": "false", "powered": "false"})
    s.fill(117, 1, 66, 117, y - 2, 66, mc("white_concrete"))
    s.fill(117, 1, 82, 117, y - 2, 82, mc("white_concrete"))
    s.entity(115.0, y + 0.2, 76.0, {"id": "guhs:guh_slee", "Locked": Byte(1), "Speed": 2, "Rotation": floats(180.0, 0.0)})
    s.entity(115.0, y + 0.2, 70.0, {"id": "guhs:guh_slee", "Locked": Byte(1), "Speed": 1, "Running": Byte(1),
                                    "Rotation": floats(180.0, 0.0), "Passengers": compounds([guh_nbt(0.55)])})


def billboard(s, x0, z, colour="magenta_concrete"):
    s.fill(x0 - 1, 2, z + 1, x0 + 7, 10, z + 1, mc(colour))
    s.fill(x0 + 1, 1, z + 1, x0 + 1, 1, z + 1, mc("purple_concrete"))
    s.fill(x0 + 5, 1, z + 1, x0 + 5, 1, z + 1, mc("purple_concrete"))
    guh_portrait(s, x0, 3, z)


def hamster_house_extra_extra_large():
    W, H, D = 128, 72, 128
    s = Structure((W, H, D))
    c = 63.5
    footprint = []
    for x in range(W):
        for z in range(D):
            if ((x - c) / 63.5) ** 4 + ((z - c) / 63.5) ** 4 <= 1:     # a rounded square plate
                footprint.append((x, z))
                s.set(x, 0, z, mc("pink_concrete") if (x * 3 + z * 5) % 13 else mc("white_concrete"))
    fp = set(footprint)
    for (x, z) in footprint:                                          # low wall around the park, gate at the south
        if any((x + dx, z + dz) not in fp for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) and not 56 <= x <= 71:
            s.fill(x, 1, z, x, 2, z, mc("magenta_terracotta"))
    # paths: main avenue (gate -> fountain -> ferris wheel) and a cross street
    for x in range(60, 68):
        for z in range(28, 128):
            if (x, z) in fp:
                s.set(x, 0, z, mc("yellow_concrete") if (x + z) % 4 else mc("white_concrete"))
    for x in range(8, 120):
        for z in range(60, 68):
            if (x, z) in fp:
                s.set(x, 0, z, mc("yellow_concrete") if (x + z) % 4 else mc("white_concrete"))
    for x in range(W):                                                # the fountain plaza
        for z in range(D):
            d = math.dist((x, z), (64, 64))
            if d <= 15:
                s.set(x, 0, z, mc("white_concrete") if int(d) % 3 else mc("pink_concrete"))
    flowing_fountain(s, 64, 64)
    for z in range(70, 124, 8):                                       # lanterns along the avenue
        for x in (58, 69):
            s.fill(x, 1, z, x, 2, z, mc("birch_fence"))
            s.set(x, 3, z, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    # entrance gate with a guh portrait on top
    s.fill(54, 1, 122, 57, 13, 125, mc("pink_concrete"))
    s.fill(70, 1, 122, 73, 13, 125, mc("pink_concrete"))
    s.fill(54, 11, 122, 73, 13, 125, mc("magenta_concrete"))
    s.fill(54, 14, 122, 57, 15, 125, mc("pearlescent_froglight"))
    s.fill(70, 14, 122, 73, 15, 125, mc("pearlescent_froglight"))
    guh_portrait(s, 61, 14, 126)
    s.fill(60, 14, 125, 68, 21, 125, mc("magenta_concrete"))
    # the big ferris wheel at the north end of the avenue
    ferris_wheel(s, 64, 38, 18, 28)
    carousel(s, 28, 88)
    sled_coaster(s)
    # food stands along the avenue
    food_stand(s, 50, 76, True, "yellow", ["guhs:frying_pan", "guhs:block_of_kaasknabbels", "guhs:frying_pan"])
    food_stand(s, 71, 76, False, "orange", ["guhs:block_of_kaasknabbels"] * 3)
    food_stand(s, 50, 92, True, "red", [mc("cake"), mc("cake"), mc("cake")])
    food_stand(s, 71, 92, False, "lime", [mc("melon"), mc("pumpkin"), mc("melon")])
    food_stand(s, 50, 108, True, "light_blue", [mc("cauldron"), "guhs:frying_pan", mc("cauldron")])
    # guh portrait billboards around the plaza
    for (bx, bz) in ((38, 44), (82, 44), (38, 76), (82, 76)):
        billboard(s, bx, bz)
    # the mascot: a guh statue built from blocks (scale 1)
    voxel_guh(s, 8, 1, 100, scale=1.0)
    # hamster castle with tubes to a lookout ball
    cheese_house(s, 86, 106, 106, 118, 13, 6, 96, loot="guhs:chests/hamster_house", spawner=(100, 1, 114))
    s.tube([(90, 12, 112), (84, 18, 108), (80, 24, 104)], mc("yellow_stained_glass"))
    glass_ball(s, 80, 29, 104, 5)
    guh(s, 80.5, 25.0, 104.5, scale=0.8)
    # running wheels
    flower_wheel(s, 104, 14, 12, 10)
    flower_wheel(s, 20, 9, 30, 6, rim="lime_concrete", scallop="yellow_terracotta")
    big_wheel_block(s, 34, 1, 52)
    big_wheel_block(s, 40, 1, 52)
    # bowls, bottles and a guh spawner under a glass dome
    for (fx, fz) in ((14, 56), (44, 100), (100, 30), (112, 76)):
        food_bowl(s, fx, fz)
    water_bottle(s, 22, 44, 1, 8)
    s.set(28, 1, 60, "guhs:guh_spawner")
    s.sphere(28, 1, 60, 3, mc("pink_stained_glass"), shell=1, inside=None)
    # the MEGA guh and lots of park visitors
    guh(s, 40.5, 1.0, 36.5, scale=9.5, health=500, Rotation=floats(30.0, 0.0))
    for (gx, gz, sc) in ((62.5, 90.5, 1.0), (66.5, 100.5, 0.7), (60.5, 112.5, 1.3), (80.5, 60.5, 0.9),
                         (48.5, 64.5, 1.1), (90.5, 70.5, 0.6), (20.5, 70.5, 1.5), (100.5, 110.5, 1.0),
                         (70.5, 40.5, 0.8), (30.5, 110.5, 1.2), (12.5, 84.5, 0.9), (56.5, 50.5, 2.0)):
        guh(s, gx, 1.0, gz, scale=sc)
    # 2.10.1: a Reisguh just inside the gate, on the avenue, greeting who comes in: Guhland's travel waypoint
    from features import reisguh_plek
    reisguh_plek.zet(s, reisguh_plek.rondom(62, 1, 118), "Guhland", 0.0, Byte, floats, "(Guhland)")
    s.clear_above(footprint, 1, top=40)
    s.save("hamster_house_extra_extra_large")


# ---------------------------------------------------------------------------------------------------------------------
# Guhramid: a very rare pink stepped pyramid with a treasure hall, a mummy guh, a trap and a secret room
# ---------------------------------------------------------------------------------------------------------------------
def guhramid():
    W, H, D = 41, 24, 41
    s = Structure((W, H, D))
    c = 20
    for x in range(W):
        for z in range(D):
            s.set(x, 0, z, mc("pink_terracotta"))                                    # foundation
    for y in range(1, 21):                                                          # the stepped body
        r = 21 - y
        for x in range(c - r, c + r + 1):
            for z in range(c - r, c + r + 1):
                edge = max(abs(x - c), abs(z - c)) == r
                corner = abs(x - c) == r and abs(z - c) == r
                if corner:
                    block = mc("magenta_terracotta")
                elif edge and y % 4 == 0:
                    block = mc("pink_concrete")
                elif edge and y == 10:
                    block = mc("pink_glazed_terracotta")
                else:
                    block = mc("pink_terracotta")
                s.set(x, y, z, block)
    s.fill(c - 1, 20, c - 1, c + 1, 20, c + 1, mc("gold_block"))                     # golden tip...
    s.set(c, 21, c, "guhs:compressed_super_vahoege_vads")                            # ...with a vads cap
    s.set(c, 22, c, mc("lantern"), {"hanging": "false", "waterlogged": "false"})

    # treasure hall
    s.fill(11, 1, 11, 29, 9, 29, mc("air"))
    for x in range(11, 30):
        for z in range(11, 30):
            s.set(x, 0, z, mc("pink_concrete") if (x + z) % 2 else mc("white_concrete"))
    for (x, z) in ((11, 11), (29, 11), (11, 29), (29, 29)):                         # pillars
        s.fill(x, 1, z, x, 9, z, mc("magenta_terracotta"))
    for x in range(13, 29, 5):                                                      # hanging lanterns
        for z in range(13, 29, 5):
            s.set(x, 9, z, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    s.fill(18, 1, 17, 22, 1, 21, mc("gold_block"))                                  # the dais
    s.fill(19, 2, 18, 21, 2, 20, mc("pink_wool"))
    s.entity(20.5, 3.0, 19.5, guh_nbt(1.6, Variant="snow", Sitting=Byte(1), PersistenceRequired=Byte(1),
                                      CustomName='{"translate":"entity.guhs.guh.mummy"}'))
    # the trap: a pressure plate in front of the dais, TNT underneath
    s.set(20, 0, 23, mc("tnt"), {"unstable": "false"})
    s.set(20, 1, 23, mc("light_weighted_pressure_plate"), {"power": "0"})
    for (x, z, facing) in ((12, 12, "south"), (28, 12, "south"), (12, 28, "north"), (28, 28, "north")):
        chest(s, x, 1, z, facing, "guhs:chests/guhramid")
    # portraits on the side walls (on the north wall the portrait hides the secret room)
    guh_portrait(s, 17, 2, 10)                                                      # north wall: the secret door
    for z0 in (14, 22):
        for row in range(7):
            s.set(10, 2 + row, z0 + 1, mc("magenta_wool"))

    # secret room behind the north portrait: a golden guh and the best loot
    # (deep enough inside that the sloping outer wall still covers it: at z=6 the steps reach y7)
    s.fill(15, 1, 6, 25, 4, 9, mc("air"))
    s.fill(15, 0, 6, 25, 0, 9, mc("gold_block"))
    s.set(20, 4, 8, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    chest(s, 20, 1, 6, "south", "guhs:chests/guhramid_secret")
    s.entity(17.5, 1.0, 7.5, guh_nbt(1.0, Variant="golden", Sitting=Byte(1), PersistenceRequired=Byte(1)))
    s.set(23, 1, 7, "guhs:guh_spawner")

    # entrance corridor from the south, with kaas saus pools on the sides for a snack
    s.fill(19, 1, 30, 21, 4, 40, mc("air"))
    for z in range(30, 41):
        s.set(19, 0, z, mc("yellow_concrete")); s.set(20, 0, z, mc("pink_concrete")); s.set(21, 0, z, mc("yellow_concrete"))
    for z in (32, 36):
        s.set(18, 0, z, "guhs:block_of_kaasknabbels"); s.set(18, 1, z, "guhs:kaas_saus", {"level": "0"})
        s.set(22, 0, z, "guhs:block_of_kaasknabbels"); s.set(22, 1, z, "guhs:kaas_saus", {"level": "0"})
    for z in (31, 35, 39):
        s.set(20, 4, z, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    for x in (16, 24):                                                              # obelisks beside the door
        s.fill(x, 1, 39, x, 7, 39, mc("magenta_terracotta"))
        s.set(x, 8, 39, mc("gold_block"))
        s.set(x, 4, 40, mc("pink_glazed_terracotta"))
    guh(s, 23.5, 1.0, 38.5, scale=0.9)
    s.clear_above([(x, z) for x in range(W) for z in range(D)], 1)
    s.save("guhramid")


# ---------------------------------------------------------------------------------------------------------------------
# Guh villages: hamster-style houses around a fountain plaza, one per guh profession + homes, with guh villagers
# ---------------------------------------------------------------------------------------------------------------------
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def villager_nbt():
    return {"id": "minecraft:villager", "PersistenceRequired": Byte(1),
            "VillagerData": {"type": "guhs:guh", "profession": "minecraft:none", "level": 1}}


def door_block(s, x, z, outward):
    """An oak door in a wall, opening to `outward`."""
    facing = OPPOSITE[outward]
    for y, half in ((1, "lower"), (2, "upper")):
        s.set(x, y, z, mc("oak_door"), {"facing": facing, "half": half, "hinge": "left", "open": "false", "powered": "false"})


def bed(s, x, z, facing, colour="pink"):
    """A bed with its foot at (x, z) and its head one step towards `facing`."""
    dx, dz = STEP[facing]
    s.set(x, 1, z, mc(f"{colour}_bed"), {"facing": facing, "part": "foot", "occupied": "false"})
    s.set(x + dx, 1, z + dz, mc(f"{colour}_bed"), {"facing": facing, "part": "head", "occupied": "false"})


def village_house(s, x0, z0, door_side, job, roof, wall="pink_concrete"):
    """A 9x9 hamster house: pink walls, a stepped cheese roof, round-ish windows, a door towards the path.
    job = a guh job site block (a workplace with one bed) or None (a home with two beds)."""
    x1, z1 = x0 + 8, z0 + 8
    cx, cz = x0 + 4, z0 + 4
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, 0, z, mc("birch_planks"))
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            for y in range(1, 5):
                if corner:
                    s.set(x, y, z, mc("white_terracotta"))
                elif edge:
                    window = y in (2, 3) and ((x in (x0, x1) and z in (cz - 1, cz + 1)) or (z in (z0, z1) and x in (cx - 1, cx + 1)))
                    s.set(x, y, z, mc("pink_stained_glass") if window else mc(wall))
                else:
                    s.set(x, y, z, mc("air"))
    for level in range(4):                                                  # stepped cheese roof
        for x in range(x0 - 1 + level, x1 + 2 - level):
            for z in range(z0 - 1 + level, z1 + 2 - level):
                if level == 3 or x in (x0 - 1 + level, x1 + 1 - level) or z in (z0 - 1 + level, z1 + 1 - level):
                    hole = (x * 7 + z * 3 + level) % 11 == 0
                    s.set(x, 5 + level, z, mc("orange_terracotta") if hole else mc(roof))
    # the door, in the middle of the wall on the path side
    dx, dz = STEP[door_side]
    door_x, door_z = {"north": (cx, z0), "south": (cx, z1), "west": (x0, cz), "east": (x1, cz)}[door_side]
    for y in range(1, 3):
        s.set(door_x, y, door_z, mc("air"))
    door_block(s, door_x, door_z, door_side)
    s.set(door_x + dx, 0, door_z + dz, mc("yellow_concrete"))
    s.set(cx, 4, cz, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    # inside: the job site facing the room, beds along the back wall
    back = OPPOSITE[door_side]
    bx, bz = STEP[back]
    if job:
        s.set(cx + bx * 2 + (1 if bz else 0), 1, cz + bz * 2 + (1 if bx else 0), f"guhs:{job}", {"facing": door_side})
        bed(s, cx + bx * 2 - (2 if bz else 0) + (0 if bz else 0), cz + bz * 2 - (2 if bx else 0), door_side if False else back)
    else:
        for off in (-2, 2):
            bed(s, cx + bx * 2 + (off if bz else 0) - bx, cz + bz * 2 + (off if bx else 0) - bz, back)
        chest(s, cx - (bz and 0) + (bx * 3 if bx else 0), 1, cz + (bz * 3 if bz else 0), door_side, "guhs:chests/guh_village")
    s.set(cx + (1 if not bx else 0) * 3, 1, cz + (1 if not bz else 0) * 3, mc("potted_pink_tulip"))
    s.entity(cx + 0.5, 1.0, cz + 0.5, villager_nbt())


def guh_village(name, seed, roof="yellow_terracotta", bouwplaats=None):
    """A guh village (64 x 16 x 64). bouwplaats (2.9, beroepen): a function (s, x0, z0, door_side, roof, rng) that builds the
    first home plot instead of a house (Bob de Guhbouwer's half-built house in layout_c); the other layouts leave it None."""
    import random as _random
    rng = _random.Random(seed)
    W = D = 64
    c = 32
    s = Structure((W, 16, D))
    footprint = []
    for x in range(W):
        for z in range(D):
            if ((x - 31.5) / 32) ** 4 + ((z - 31.5) / 32) ** 4 <= 1:
                footprint.append((x, z))
                s.set(x, 0, z, mc("pink_wool") if (x * 5 + z * 3) % 17 else mc("pink_concrete_powder"))
    fp = set(footprint)
    for i in range(W):                                                      # the two main paths
        for w in (-1, 0, 1):
            for (x, z) in ((c + w, i), (i, c + w)):
                if (x, z) in fp:
                    s.set(x, 0, z, mc("yellow_concrete") if (i + w) % 5 else mc("white_concrete"))
    for x in range(c - 10, c + 11):                                         # the plaza
        for z in range(c - 10, c + 11):
            if math.dist((x, z), (c, c)) <= 10:
                s.set(x, 0, z, mc("white_concrete") if int(math.dist((x, z), (c, c))) % 3 else mc("pink_concrete"))
    flowing_fountain(s, c, c)
    s.set(c + 9, 0, c + 2, mc("pink_concrete"))
    s.set(c + 9, 1, c + 2, mc("bell"), {"attachment": "floor", "facing": "north", "powered": "false"})   # meeting point
    for (x, z) in ((c - 2, 8), (c + 2, 56), (8, c + 2), (56, c - 2), (c - 2, 44), (c + 2, 20), (20, c + 2), (44, c - 2)):
        s.fill(x, 1, z, x, 2, z, mc("birch_fence"))                        # lamp posts along the paths
        s.set(x, 3, z, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    # eight plots: two per quadrant, doors facing a main path
    plots = [(21, 5, "east"), (5, 20, "south"), (34, 5, "west"), (50, 20, "south"),
             (21, 50, "east"), (5, 35, "north"), (34, 50, "west"), (50, 35, "north")]
    kinds = ["knabbelbak", "naaitafel", "vadsaambeeld", "buizenbank", "mikatrofee", "zaadbak", None, None]
    rng.shuffle(kinds)
    bouw_plot = kinds.index(None) if bouwplaats else -1
    for i, ((x0, z0, door), job) in enumerate(zip(plots, kinds)):
        if i == bouw_plot:
            bouwplaats(s, x0, z0, door, roof, rng)
            continue
        village_house(s, x0, z0, door, job, roof)
        if job == "knabbelbak":                                             # the vads temmer keeps a few guhs
            px, pz = (x0 + 1, z0 + 11) if z0 < c else (x0 + 1, z0 - 5)
            for x in range(px, px + 7):
                for z in range(pz, pz + 4):
                    if x in (px, px + 6) or z in (pz, pz + 3):
                        s.set(x, 1, z, mc("birch_fence"))
            guh(s, px + 2.5, 1.0, pz + 1.5, scale=0.8)
            guh(s, px + 4.5, 1.0, pz + 1.5, scale=1.1)
        if job == "zaadbak":                                                # the knabbelboer's kaasknabbel field
            px, pz = (x0 + 1, z0 + 11) if z0 < c else (x0 + 1, z0 - 5)
            for x in range(px, px + 7):
                for z in range(pz, pz + 4):
                    if x == px + 3:
                        s.set(x, 0, z, mc("water"), {"level": "0"})
                    else:
                        s.set(x, 0, z, mc("farmland"), {"moisture": "7"})
                        s.set(x, 1, z, "guhs:kaasknabbelplant", {"age": str(rng.randrange(2, 8))})
            s.set(px + 3, 1, pz - 1 if z0 < c else pz + 4, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    for (x, z) in ((c + 4, c + 12), (c - 12, c - 4)):                      # villagers hanging around the plaza
        s.entity(x + 0.5, 1.0, z + 0.5, villager_nbt())
    for i in range(6):                                                      # flowers
        x, z = rng.randrange(4, 60), rng.randrange(4, 60)
        if (x, z) in fp and s.get(x, 1, z) is None and s.get(x, 0, z) in (mc("pink_wool"), mc("pink_concrete_powder")):
            s.set(x, 1, z, mc("pink_tulip"))
    s.clear_above(footprint, 1, top=16)
    s.save(f"guh_village/{name}")
    return s


def guh_villages():
    guh_village("layout_a", 1)
    guh_village("layout_b", 2, roof="orange_terracotta")


def empty_room(name, size):
    Structure(size).save(name)


if __name__ == "__main__":
    hamster_house()
    evil_mika_home()
    guh_picnic()
    hamster_house_medium()
    hamster_house_large()
    cheese_fountain()
    guh_caves()
    challenging_guh_caves()
    grand_cheese_fountain()
    guh_statue()
    hamster_house_extra_extra_large()
    guhramid()
    guh_villages()
    empty_room("empty", (5, 4, 5))
    empty_room("portal_room", (7, 8, 3))
    empty_room("wire_room", (44, 3, 3))
