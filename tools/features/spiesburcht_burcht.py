"""
The two big buildings of the Barbecuether (structure type guhs:burcht, see BurchtStructure.java):

  - guhs:spiesburcht       the fortress parody: a charcoal keep of houtskoolsteen stenen with giant guh faces on all
                           four sides, four long bridges on arches and pillars (saté lamp posts, mika heads on the
                           railings), the Vonk-Mika spawner on its platform, bridge towers with chests, and the
                           pindasaus-tuintje (nether wart garden) in the keep's courtyard, with a guh scarecrow
  - guhs:mika_grillpaleis  the bastion parody: a grill-iron palace on a foundation rising out of the frying-sauce sea,
                           a sauce moat, a gate under a huge evil Mika face, four horned towers, a hall with a
                           mountain of stolen kaasknabbels and vads treasure chests, caged plush guhs, an empty Mika
                           throne (no boss), lots of Nether-Mikas and walkways to the roof

Each build is one big template, saved in 32x32 tiles (spiesburcht/stuk_i_j, mika_grillpaleis/stuk_i_j). Both have a
geometry self-check (check_*) that build() runs: nothing floats, there is a walkable way to everything that matters,
chests/plants/lanterns/Mikas stand on something, the sauce stays in its basins, the spawner is there.
"""
import json
import math
import random

TILE = 32

STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
TRAP = "guhs:houtskoolsteen_stenen_trap"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
MUUR = "guhs:houtskoolsteen_stenen_muur"
HEK = "guhs:houtskoolsteen_stenen_hek"
HOUTSKOOL = "guhs:houtskoolsteen"
ROOSTER = "guhs:roosterijzer"
PILAAR = "guhs:roosterijzer_pilaar"
GEPOLIJST = "guhs:gepolijst_roosterijzer"
TRALIES = "guhs:roosterijzer_tralies"
GLOEIKOOL = "guhs:gloeikool"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
SATE = "guhs:sate_stam"
VLEES = "guhs:sate_vlees"
WORST = "guhs:worst_stam"
MOSTERD = "guhs:mosterd_blok"
NYLIUM = "guhs:pindasaus_nylium"
SCHEUTJES = "guhs:pindascheutjes"
ZWAMMETJE = "guhs:sate_zwammetje"
PLASJE = "guhs:pindasausplasje"
SMEUL = "guhs:smeulkooltjes"
SAUS = "guhs:kaasfrituursaus"
GRILLKOOL = "guhs:grillkool"
MIKAKOP = "guhs:verkoolde_mikakop"
MIKAKOP_MUUR = "guhs:verkoolde_mikakop_muur"
KNABBELS = "guhs:block_of_kaasknabbels"
VADS = "guhs:compressed_super_vahoege_vads"
UIENLICHT = "guhs:uienlicht"
ROOKGAT = "guhs:rookgat"
AIR = "minecraft:air"

FACES = ("north", "east", "south", "west")
# blocks you walk through (for the walk check) and things that stand on the floor
PASS = {AIR, SCHEUTJES, ZWAMMETJE, PLASJE, SMEUL, "minecraft:wall_torch", "minecraft:spruce_wall_sign", "minecraft:dark_oak_wall_sign",
        "minecraft:crimson_wall_sign", "minecraft:crimson_sign", "minecraft:chain", "minecraft:lantern", "minecraft:soul_lantern",
        "minecraft:fire", "minecraft:soul_fire", MIKAKOP_MUUR, "minecraft:red_carpet", "minecraft:black_carpet", "minecraft:gray_carpet",
        "minecraft:pink_carpet", "minecraft:light_gray_carpet"}
NEEDS_FLOOR = {SCHEUTJES, ZWAMMETJE, PLASJE, SMEUL, "minecraft:chest", "minecraft:barrel", "minecraft:campfire", "minecraft:soul_campfire",
               "minecraft:crimson_sign", MIKAKOP, "minecraft:red_carpet", "minecraft:black_carpet", "minecraft:gray_carpet", "minecraft:pink_carpet",
               "minecraft:light_gray_carpet", "minecraft:spawner"}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def rot_face(face, k):
    """A facing turned k quarter turns clockwise (seen from above)."""
    if face in ("up", "down"):
        return face
    return FACES[(FACES.index(face) + k) % 4]


class Bouw:
    def __init__(self, h, size, seed):
        self.h = h
        self.s = h.Structure(size)
        self.W, self.H, self.D = size
        self.rng = random.Random(seed)
        self.chests = []
        self.entities = []          # (x, y, z) of the mobs placed
        self.spawners = []
        self.must_reach = {}        # name: standing cell that must be reachable

    # --- basics ------------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if not only_empty or self.get(x, y, z) is None:
                        self.set(x, y, z, name, props)

    def air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def stair(self, x, y, z, facing, half="bottom", block=TRAP):
        self.set(x, y, z, block, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def slab(self, x, y, z, kind="bottom", block=PLAAT):
        self.set(x, y, z, block, {"type": kind, "waterlogged": "false"})

    def fence(self, x, y, z, block=HEK):
        self.set(x, y, z, block, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})

    def wall(self, x, y, z, block=MUUR):
        self.set(x, y, z, block, {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"})

    def bars(self, x, y, z):
        self.set(x, y, z, TRALIES, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})

    def lantern(self, x, y, z, hanging=False, soul=False):
        self.set(x, y, z, "soul_lantern" if soul else "lantern", {"hanging": "true" if hanging else "false", "waterlogged": "false"})

    def chain(self, x, y, z):
        self.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})

    def hang_lantern(self, x, y_ceiling, z, length=1, soul=False):
        """A lantern on a chain under the block at y_ceiling."""
        for i in range(1, length + 1):
            self.chain(x, y_ceiling - i, z)
        self.lantern(x, y_ceiling - length - 1, z, hanging=True, soul=soul)

    def chest(self, x, y, z, facing, loot, barrel=False):
        if barrel:
            self.set(x, y, z, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel", "LootTable": loot})
        else:
            self.set(x, y, z, "chest", {"facing": facing, "type": "single", "waterlogged": "false"}, {"id": "minecraft:chest", "LootTable": loot})
        self.chests.append((x, y, z))

    def spawner(self, x, y, z, entity):
        S = self.h.ms.Short
        self.set(x, y, z, "spawner", None, {"id": "minecraft:mob_spawner", "Delay": S(20), "SpawnData": {"entity": {"id": entity}},
                                            "SpawnCount": S(3), "MaxNearbyEntities": S(5), "RequiredPlayerRange": S(16),
                                            "MinSpawnDelay": S(200), "MaxSpawnDelay": S(800), "SpawnRange": S(4)})
        self.spawners.append((x, y, z, entity))

    def mob(self, x, y, z, entity, yaw=0.0):
        self.s.entity(x + 0.5, y, z + 0.5, {"id": entity, "PersistenceRequired": self.h.Byte(1), "Rotation": self.h.floats(yaw, 0.0)})
        self.entities.append((x, y, z, entity))

    def sign(self, x, y, z, facing, keys, wall=True, wood="crimson"):
        msgs = [json.dumps({"translate": f"sign.guhs.{k}"}) if k else '""' for k in keys] + ['""'] * (4 - len(keys))
        text = {"messages": self.h.ms.NbtList(8, msgs), "color": "yellow", "has_glowing_text": self.h.Byte(1)}
        empty = {"messages": self.h.ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": self.h.Byte(0)}
        nbt = {"id": "minecraft:sign", "is_waxed": self.h.Byte(1), "front_text": text, "back_text": empty}
        if wall:
            self.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    def mikakop(self, x, y, z, facing):
        self.set(x, y, z, MIKAKOP, {"facing": facing})

    def stairway(self, xs, z0, dz, y0, steps, facing, fill=STENEN, block=TRAP, along_x=False):
        """A straight stair `steps` high from y0 up, 3 wide (xs), moving dz per step; clears the headroom above each step
        (so it cuts its own hole through a ceiling). Returns the (x, z) of the landing after the top step."""
        for i in range(steps):
            z = z0 + dz * i
            y = y0 + i
            for x in range(xs[0], xs[1] + 1):
                px, pz = (z, x) if along_x else (x, z)
                self.stair(px, y, pz, facing, block=block)
                for yy in range(y0, y):
                    self.set(px, yy, pz, fill)
                for yy in range(y + 1, y + 4):
                    self.set(px, yy, pz, AIR)
        return z0 + dz * steps

    def brick(self, cracked=0.12):
        return GEBARSTEN if self.rng.random() < cracked else STENEN

    # --- finishing ------------------------------------------------------------------------------------------------------
    def connect(self):
        """Fences, walls and grill bars join their neighbours (the template keeps these shapes as they are)."""
        solid_like = lambda n: n is not None and n not in PASS and n not in (MIKAKOP,) and "sign" not in n and "lantern" not in n \
            and "chain" not in n and "carpet" not in n and "torch" not in n
        dirs = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
        for (x, y, z), (name, props, nbt) in list(self.s.blocks.items()):
            if name in (mc(HEK), mc(TRALIES), mc(MUUR)) or name.endswith("_fence") or name.endswith("iron_bars"):
                new = dict(props)
                for d, (dx, dz) in dirs.items():
                    n = self.get(x + dx, y, z + dz)
                    join = solid_like(n) and not (n or "").endswith("_stairs") and n != mc(TRAP) and n != mc(PLAAT) or n == name
                    if name == mc(MUUR):
                        new[d] = "low" if join else "none"
                    else:
                        new[d] = "true" if join else "false"
                if name == mc(MUUR):
                    above = self.get(x, y + 1, z)
                    straight = (new["north"] != "none" and new["south"] != "none" and new["east"] == "none" and new["west"] == "none") or \
                               (new["east"] != "none" and new["west"] != "none" and new["north"] == "none" and new["south"] == "none")
                    new["up"] = "false" if straight and (above is None or above == AIR) else "true"
                self.s.blocks[(x, y, z)] = (name, new, nbt)

    def save_tiles(self, name):
        """Cut the build into 32x32 tiles: <name>/stuk_i_j (empty tiles are left out)."""
        tiles = {}
        for (x, y, z), b in self.s.blocks.items():
            tiles.setdefault((x // TILE, z // TILE), {})[(x % TILE, y, z % TILE)] = b
        ents = {}
        for (x, y, z, nbt) in self.s.entities:
            ents.setdefault((int(x) // TILE, int(z) // TILE), []).append((x - (int(x) // TILE) * TILE, y, z - (int(z) // TILE) * TILE, nbt))
        nx, nz = (self.W + TILE - 1) // TILE, (self.D + TILE - 1) // TILE
        saved = []
        for i in range(nx):
            for j in range(nz):
                if (i, j) not in tiles:
                    continue
                t = self.h.Structure((min(TILE, self.W - i * TILE), self.H, min(TILE, self.D - j * TILE)))
                t.blocks = tiles[(i, j)]
                t.entities = ents.get((i, j), [])
                t.save(f"{name}/stuk_{i}_{j}")
                saved.append((i, j))
        return nx, nz, saved


# =====================================================================================================================
# checks shared by both builds
# =====================================================================================================================
def solid(name):
    return name is not None and name not in PASS and name != mc(SAUS)


def check_common(b, name, ground_y=0):
    blocks = b.s.blocks
    problems = []

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None

    # nothing floats: every block connects to the bottom layer (the pillars and the foundation)
    placed = {c for c, v in blocks.items() if v[0] != AIR}
    seen = {c for c in placed if c[1] <= ground_y}
    todo = list(seen)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in placed and n not in seen:
                seen.add(n)
                todo.append(n)
    floating = [c for c in placed if c not in seen]
    for c in floating[:20]:
        problems.append(f"{name}: floating {nm(c)} at {c}")
    if len(floating) > 20:
        problems.append(f"{name}: ... {len(floating)} floating blocks in all")
    # things that need a floor
    for c, v in blocks.items():
        below = nm((c[0], c[1] - 1, c[2]))
        if v[0] in NEEDS_FLOOR or (v[0] == "minecraft:lantern" and v[1].get("hanging") == "false"):
            if not solid(below):
                problems.append(f"{name}: {v[0]} at {c} stands on {below}")
        if v[0] in (SCHEUTJES, ZWAMMETJE) and below != NYLIUM:
            problems.append(f"{name}: {v[0]} at {c} grows on {below}")
        if v[0] == SMEUL and below not in (HOUTSKOOL, AS, AS_AARDE, NYLIUM):
            problems.append(f"{name}: smeulkooltjes at {c} on {below}")
        if v[0] == "minecraft:lantern" and v[1].get("hanging") == "true":
            above = nm((c[0], c[1] + 1, c[2]))
            if above is None or above == AIR:
                problems.append(f"{name}: hanging lantern at {c} hangs from nothing")
        if v[0] == mc(SAUS):
            if not (solid(below) or below == mc(SAUS)):
                problems.append(f"{name}: sauce at {c} would drip down onto {below}")
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = nm((c[0] + dx, c[1], c[2] + dz))
                if not (solid(n) or n == mc(SAUS)):
                    problems.append(f"{name}: sauce at {c} leaks sideways into {n}")
    # mobs stand on something, with room
    for (x, y, z, e) in b.entities:
        if not solid(nm((x, y - 1, z))) or solid(nm((x, y, z))) or solid(nm((x, y + 1, z))):
            problems.append(f"{name}: {e} at {(x, y, z)} has no room to stand")
    for c in b.chests:
        if not solid(nm((c[0], c[1] - 1, c[2]))):
            problems.append(f"{name}: chest at {c} floats")
    return problems


def walkable(b):
    """The cells you can stand in: something solid under you and room for your head."""
    blocks = b.s.blocks

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None
    cells = set()
    for (x, y, z), v in blocks.items():
        if solid(v[0]) and not solid(nm((x, y + 1, z))) and not solid(nm((x, y + 2, z))) and nm((x, y + 1, z)) != mc(SAUS):
            # fences and walls are too high to stand on in a walk (a normal player can't jump on them)
            if v[0] in (mc(HEK), mc(MUUR), mc(TRALIES)) or v[0].endswith("_fence"):
                continue
            cells.add((x, y + 1, z))
    return cells


def reach(b, start):
    """Every standing cell you can walk to from `start` (steps of one block up or down; unknown air counts as open)."""
    cells = walkable(b)
    blocks = b.s.blocks
    if start not in cells:
        return set(), cells
    seen = {start}
    todo = [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n in cells and n not in seen:
                    if dy == 1 and solid(blocks.get((x, y + 2, z), (None,))[0]):
                        continue            # no room to jump up
                    seen.add(n)
                    todo.append(n)
                    break
    return seen, cells


def check_paths(b, name, start):
    problems = []
    seen, cells = reach(b, start)
    if not seen:
        return [f"{name}: the start {start} is not a place to stand"]
    for what, cell in b.must_reach.items():
        if cell not in seen:
            problems.append(f"{name}: can't walk from {start} to {what} at {cell}")
    return problems


# =====================================================================================================================
# De Spiesburcht
# =====================================================================================================================
SB = (121, 66, 121)
SB_C = 60              # the middle (x and z)
SB_DECK = 30           # the floor you walk on (template y); the pillars go down to y 0
KEEP = 16              # the keep reaches this far from the middle


def spiesburcht(h):
    b = Bouw(h, SB, 27020001)
    c, Y = SB_C, SB_DECK
    keep(b, c, Y)
    for k in range(4):
        bridge(b, c, Y, k)
    tuintje(b, c, Y + 9)
    spawn_platform(b, c, Y, 0)                       # east: the Vonk-Mika spawner
    bridge_tower(b, c, Y, 3, "north")                # north: the watch post
    bridge_tower(b, c, Y, 2, "west")                 # west: the skewer store
    bridge_tower(b, c, Y, 1, "south")                # south: the gate
    b.connect()
    b.must_reach.update({"the tuintje": (c + 1, Y + 10, c), "the roof": (c - KEEP + 3, Y + 19, c)})
    return b


def rot(k, a, bb, c):
    """(along, across) of bridge k (0 east, 1 south, 2 west, 3 north) to (x, z)."""
    return [(c + a, c + bb), (c - bb, c + a), (c - a, c - bb), (c + bb, c - a)][k]


def keep(b, c, Y):
    K = KEEP
    x0, x1, z0, z1 = c - K, c + K, c - K, c + K
    top = Y + 18
    # --- the foundation: four big corner piers, a middle pier and thick walls under the edges, down to y 0 ---
    for (px, pz) in ((x0, z0), (x1 - 4, z0), (x0, z1 - 4), (x1 - 4, z1 - 4), (c - 3, c - 3)):
        for y in range(0, Y):
            for x in range(px, px + 5 + (2 if (px, pz) == (c - 3, c - 3) else 0)):
                for z in range(pz, pz + 5 + (2 if (px, pz) == (c - 3, c - 3) else 0)):
                    b.set(x, y, z, b.brick())
            if y % 7 == 3:
                for x in range(px, px + 5):
                    b.set(x, y, pz, GEBEITELD)
    for y in range(Y - 6, Y):                        # a thick skirt under the keep
        inset = (Y - 1 - y) // 2
        for x in range(x0 + inset, x1 + 1 - inset):
            for z in range(z0 + inset, z1 + 1 - inset):
                if x in (x0 + inset, x1 - inset) or z in (z0 + inset, z1 - inset) or y == Y - 1:
                    b.set(x, y, z, b.brick())
    # --- floors, walls, ceilings ---
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, Y, z, STENEN if not edge else GEBEITELD if (x + z) % 6 == 0 else STENEN)
            for y in range(Y + 1, top):
                if edge:
                    b.set(x, y, z, b.brick())
                elif y == Y + 9:
                    b.set(x, y, z, STENEN)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, top, z, STENEN)
    # buttresses on the outside
    for i in range(x0 + 4, x1 - 3, 8):
        for (bx, bz, f) in ((i, z0 - 1, "north"), (i, z1 + 1, "south"), (x0 - 1, i, "west"), (x1 + 1, i, "east")):
            for y in range(Y - 2, top - 2):
                b.set(bx, y, bz, STENEN if y % 5 else GEBEITELD)
            b.stair(bx, top - 2, bz, rot_face(f, 2))
    # windows (grill bars) in both floors, not where the faces go
    for i in range(x0 + 2, x1 - 1, 4):
        if abs(i - c) <= 9:
            continue
        for y in (Y + 12, Y + 13, Y + 14):
            for (wx, wz) in ((i, z0), (i, z1), (x0, i), (x1, i)):
                b.bars(wx, y, wz)
    # the four doors (arches 3 wide, 4 high) towards the bridges, with Mika heads over them
    for k in range(4):
        for bb in (-1, 0, 1):
            for y in range(Y + 1, Y + 5):
                x, z = rot(k, K, bb, c)
                b.set(x, y, z, AIR)
        for bb in (-2, 2):
            x, z = rot(k, K, bb, c)
            b.set(x, Y + 5, z, GEBEITELD)
        x, z = rot(k, K, 0, c)
        b.set(x, Y + 5, z, GLOEIKOOL)
    # the giant guh faces: two per side, left and right of each door, and one big one high up in the middle
    for k in range(4):
        for side in (-1, 1):
            guh_face(b, c, Y + 1, k, side * 8, K, small=True)
        guh_face(b, c, Y + 10, k, 0, K, small=False)
    # --- the roof: battlements, the four corner turrets, and the courtyard opening ---
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, top + 1, z, STENEN)
                if (x + z) % 2 == 0:
                    b.set(x, top + 2, z, STENEN)
    for (tx, tz) in ((x0, z0), (x1 - 6, z0), (x0, z1 - 6), (x1 - 6, z1 - 6)):
        turret(b, tx, top, tz)
    # the great hall on the deck floor: saté columns, a charred guh statue, braziers, chests, and the stairs up
    hall(b, c, Y)
    # stairs from the hall up to the first floor (along the north wall, going west), and from the first floor up to
    # the roof (along the south wall, going east); each cuts its own opening, with a railing round it
    land = b.stairway((z0 + 1, z0 + 3), c - 5, -1, Y + 1, 8, "west", along_x=True)
    for x in range(c - 12, c - 4):
        b.fence(x, Y + 10, z0 + 4)
    b.must_reach["the hall stairs top"] = (land, Y + 10, z0 + 2)
    land = b.stairway((z1 - 3, z1 - 1), c + 1, 1, Y + 10, 8, "east", along_x=True)
    for x in range(c + 1, c + 9):
        b.fence(x, top + 1, z1 - 4)
    b.must_reach["the roof stairs top"] = (land, top + 1, z1 - 2)


def turret(b, tx, top, tz):
    """A corner turret on the keep roof: a little room with windows, a pointed roof and a Mika head on top."""
    for x in range(tx, tx + 7):
        for z in range(tz, tz + 7):
            edge = x in (tx, tx + 6) or z in (tz, tz + 6)
            for y in range(top + 1, top + 7):
                if edge:
                    b.set(x, y, z, b.brick() if (y - top) % 3 else GEBEITELD if (x + z) % 2 else STENEN)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, top + 7, z, STENEN)
    for (dx, dz) in ((3, 0), (0, 3), (6, 3), (3, 6)):
        b.bars(tx + dx, top + 4, tz + dz)
        b.bars(tx + dx, top + 5, tz + dz)
    inner_x = tx + 6 if tx < SB_C else tx
    inner_z = tz + 6 if tz < SB_C else tz
    for y in (top + 1, top + 2):
        b.set(inner_x, y, tz + 3, AIR)
        b.set(tx + 3, y, inner_z, AIR)
    for r in range(4):
        for x in range(tx + r, tx + 7 - r):
            for z in range(tz + r, tz + 7 - r):
                if not (x in (tx + r, tx + 6 - r) or z in (tz + r, tz + 6 - r)):
                    b.set(x, top + 8 + r, z, STENEN)
                else:
                    if x == tx + r:
                        b.stair(x, top + 8 + r, z, "east")
                    elif x == tx + 6 - r:
                        b.stair(x, top + 8 + r, z, "west")
                    elif z == tz + r:
                        b.stair(x, top + 8 + r, z, "south")
                    else:
                        b.stair(x, top + 8 + r, z, "north")
    b.set(tx + 3, top + 11, tz + 3, STENEN)
    b.set(tx + 3, top + 12, tz + 3, GLOEIKOOL)
    b.mikakop(tx + 3, top + 13, tz + 3, "south")
    b.hang_lantern(tx + 3, top + 7, tz + 3, 1)


def guh_face(b, c, y0, k, along, K, small):
    """A charred guh face on the outside wall of side k: glowing eyes, pink cheeks, a smiley mouth, ears on top."""
    if small:
        w, hgt = 7, 6
        # (the pieces in face coordinates: u across (-3..3), v up (0..5))
        eyes = [(-2, 3), (2, 3), (-2, 4), (2, 4)]
        cheeks = [(-3, 2), (3, 2)]
        mouth = [(-1, 1), (0, 1), (1, 1), (-2, 2), (2, 2)]
        ears = [(-3, 6), (-2, 6), (2, 6), (3, 6)]
    else:
        w, hgt = 11, 8
        eyes = [(u, v) for u in (-3, -2, 2, 3) for v in (5, 6)]
        cheeks = [(-5, 3), (-4, 3), (4, 3), (5, 3)]
        mouth = [(-2, 1), (-1, 1), (0, 1), (1, 1), (2, 1), (-3, 2), (3, 2)]
        ears = [(u, 8) for u in (-5, -4, -3, 3, 4, 5)] + [(u, 9) for u in (-4, 4)]
    half = w // 2
    for u in range(-half, half + 1):
        for v in range(0, hgt):
            pos = rot(k, K + 1, along + u, c)
            b.set(pos[0], y0 + v, pos[1], "black_terracotta")
    for (u, v) in eyes:
        pos = rot(k, K + 1, along + u, c)
        b.set(pos[0], y0 + v, pos[1], GLOEIKOOL)
    for (u, v) in cheeks:
        pos = rot(k, K + 1, along + u, c)
        b.set(pos[0], y0 + v, pos[1], "pink_terracotta")
    for (u, v) in mouth:
        pos = rot(k, K + 1, along + u, c)
        b.set(pos[0], y0 + v, pos[1], "magenta_terracotta")
    for (u, v) in ears:
        pos = rot(k, K + 1, along + u, c)
        b.set(pos[0], y0 + v, pos[1], "black_terracotta")
    # (the face sticks out one block: the wall behind it holds it)


def hall(b, c, Y):
    K = KEEP
    # saté columns: giant skewers from floor to ceiling, with meat chunks
    for (x, z) in ((c - 9, c - 9), (c + 9, c - 9), (c - 9, c + 9), (c + 9, c + 9), (c - 9, c), (c + 9, c)):
        for y in range(Y + 1, Y + 9):
            b.set(x, y, z, SATE, {"axis": "y"})
        for y in (Y + 3, Y + 6):
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                b.set(x + dx, y, z + dz, VLEES)
        b.hang_lantern(x + 1, Y + 9, z + 1, 1)
    # a charred guh statue in the middle, eyes glowing, on a plinth
    for x in range(c - 2, c + 3):
        for z in range(c - 2, c + 3):
            b.set(x, Y + 1, z, GEBEITELD if (x + z) % 2 else STENEN)
    for x in range(c - 2, c + 3):
        for z in range(c - 1, c + 2):
            for y in range(Y + 2, Y + 5):
                b.set(x, y, z, "black_wool" if (x + y + z) % 4 else "gray_wool")
    for x in range(c - 2, c + 3):
        for z in range(c - 2, c + 1):
            for y in range(Y + 5, Y + 8):
                b.set(x, y, z, "black_wool")
    b.set(c - 1, Y + 7, c - 3, GLOEIKOOL)
    b.set(c + 1, Y + 7, c - 3, GLOEIKOOL)
    b.set(c - 2, Y + 6, c - 3, "pink_terracotta")
    b.set(c + 2, Y + 6, c - 3, "pink_terracotta")
    b.set(c, Y + 6, c - 3, "gray_wool")
    for x in (c - 2, c + 2):
        b.set(x, Y + 8, c - 1, "black_wool")
    b.sign(c, Y + 2, c - 2, "north", ["spiesburcht.beeld1", "spiesburcht.beeld2", "spiesburcht.beeld3"])
    # braziers (lit campfires on brick pedestals) and embers
    for (x, z) in ((c - 5, c - 12), (c + 5, c - 12), (c - 5, c + 12), (c + 5, c + 12)):
        b.set(x, Y + 1, z, STENEN)
        b.set(x, Y + 2, z, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    # red carpet from the south door to the statue
    for z in range(c + 3, c + K):
        for x in (c - 1, c, c + 1):
            b.set(x, Y + 1, z, "red_carpet")
    # chests: two in the hall
    b.chest(c + K - 2, Y + 1, c - K + 2, "west", "guhs:chests/spiesburcht")
    b.chest(c + K - 2, Y + 1, c + K - 2, "west", "guhs:chests/spiesburcht")
    b.chest(c - K + 6, Y + 1, c + K - 2, "north", "guhs:chests/spiesburcht", barrel=True)
    b.must_reach.update({"the hall chests": (c + K - 3, Y + 1, c - K + 2), "the statue": (c, Y + 1, c - 4)})


def tuintje(b, c, F):
    """The pindasaus-tuintje: an open courtyard with raised beds of pindasaus nylium (the nether wart garden)."""
    R = 8
    # the courtyard: no roof above it, low walls with arches all round
    for x in range(c - R, c + R + 1):
        for z in range(c - R, c + R + 1):
            for y in range(F + 1, F + 10):
                b.set(x, y, z, AIR)
            if x in (c - R, c + R) or z in (c - R, c + R):
                for y in range(F + 1, F + 9):
                    b.set(x, y, z, b.brick())
    for (x, z) in [(c - R, zz) for zz in range(c - 5, c + 6, 5)] + [(c + R, zz) for zz in range(c - 5, c + 6, 5)] + \
                  [(xx, c - R) for xx in range(c - 5, c + 6, 5)] + [(xx, c + R) for xx in range(c - 5, c + 6, 5)]:
        for y in range(F + 1, F + 4):
            b.set(x, y, z, AIR)
    # four beds of 5x4 nylium, sunk into the floor, bordered by stairs, full of pindascheutjes and saté sprouts
    for (bx, bz) in ((c - 6, c - 6), (c + 2, c - 6), (c - 6, c + 3), (c + 2, c + 3)):
        for x in range(bx, bx + 5):
            for z in range(bz, bz + 4):
                edge = x in (bx, bx + 4) or z in (bz, bz + 3)
                if edge:
                    face = "south" if z == bz else "north" if z == bz + 3 else "east" if x == bx else "west"
                    b.stair(x, F + 1, z, face, half="top")
                else:
                    b.set(x, F, z, NYLIUM)
                    b.set(x, F + 1, z, ZWAMMETJE if b.rng.random() < 0.15 else SCHEUTJES)
    # paths of spilled peanut sauce, a guh scarecrow in the middle, sauce buckets (cauldrons)
    for x in range(c - R + 1, c + R):
        for z in (c - 1, c):
            if b.get(x, F + 1, z) in (None, AIR) and b.rng.random() < 0.35:
                b.set(x, F + 1, z, PLASJE)
    scarecrow(b, c + 5, F + 1, c - 1)
    for (x, z) in ((c - R + 1, c - R + 1), (c + R - 1, c + R - 1)):
        b.set(x, F + 1, z, "cauldron")
    b.sign(c + 2, F + 2, c - R + 1, "south", ["spiesburcht.tuin1", "spiesburcht.tuin2", "spiesburcht.tuin3"])
    for x in range(c - R - 1, c + R + 2):
        for z in range(c - R - 1, c + R + 2):
            if x in (c - R - 1, c + R + 1) or z in (c - R - 1, c + R + 1):
                b.fence(x, F + 10, z)
    b.chest(c - R + 1, F + 1, c + R - 1, "east", "guhs:chests/spiesburcht_tuintje")


def scarecrow(b, x, y, z):
    """A guh scarecrow: a fence post, a hay body and a round pink guh head with ears."""
    b.fence(x, y, z)
    b.set(x, y + 1, z, "hay_block", {"axis": "y"})
    b.fence(x - 1, y + 1, z)
    b.fence(x + 1, y + 1, z)
    for dx in (-1, 0, 1):
        for dy in (2, 3):
            b.set(x + dx, y + dy, z, "pink_wool")
    b.set(x - 1, y + 3, z - 1, "black_wool")
    b.set(x + 1, y + 3, z - 1, "black_wool")
    b.set(x, y + 2, z - 1, "magenta_wool")
    b.set(x - 1, y + 4, z, "pink_wool")
    b.set(x + 1, y + 4, z, "pink_wool")


def bridge(b, c, Y, k):
    """A long bridge from the keep's door to the edge of the build: a deck with railings, arches on pillars below."""
    start, end = KEEP + 1, SB_C
    for a in range(start, end + 1):
        for bb in range(-2, 3):
            x, z = rot(k, a, bb, c)
            b.set(x, Y, z, STENEN if abs(bb) < 2 else GEBEITELD if a % 6 == 0 else STENEN)
            b.set(x, Y - 1, z, b.brick())
            if abs(bb) == 2:
                b.fence(x, Y + 1, z)
                for y in range(Y + 2, Y + 5):
                    b.set(x, y, z, AIR)
            else:
                for y in range(Y + 1, Y + 5):
                    b.set(x, y, z, AIR)
            # the underside: an arch between the pillars
            span = (a - start) % 12
            depth = int(round(3.2 * math.sin(math.pi * span / 12))) if span else 0
            for d in range(2, 5 - depth):
                b.set(x, Y - d, z, b.brick(0.2))
    # pillars every 12 blocks, down to y 0, with guh faces carved into them
    for a in range(start, end - 5, 12):
        for bb in range(-2, 3):
            for da in range(3):
                x, z = rot(k, a + da, bb, c)
                for y in range(0, Y - 1):
                    b.set(x, y, z, GEBEITELD if y % 6 == 2 and abs(bb) == 2 else b.brick(0.15))
    # saté lamp posts and Mika heads on the railings
    for a in range(start + 3, end - 8, 6):
        for bb in (-2, 2):
            x, z = rot(k, a, bb, c)
            if (a // 6) % 2 == 0:
                for y in range(Y + 1, Y + 4):
                    b.set(x, y, z, SATE, {"axis": "y"})
                b.set(x, Y + 2, z, VLEES)
                b.set(x, Y + 4, z, GLOEIKOOL)
            else:
                b.fence(x, Y + 2, z)
                b.mikakop(x, Y + 3, z, rot_face("south" if bb > 0 else "north", k))


def spawn_platform(b, c, Y, k):
    """The Vonk-Mika spawner at the end of the east bridge: a raised platform, fenced in, with guh-face pillars."""
    a0 = SB_C - 10
    P = Y + 3
    # stairs up from the bridge
    for i in range(3):
        for bb in (-1, 0, 1):
            x, z = rot(k, a0 - 3 + i, bb, c)
            b.stair(x, Y + 1 + i, z, rot_face("east", k))
            for y in range(Y + 1, Y + 1 + i):
                b.set(x, y, z, STENEN)
    for a in range(a0, a0 + 9):
        for bb in range(-4, 5):
            x, z = rot(k, a, bb, c)
            for y in range(Y - 1, P + 1):
                b.set(x, y, z, STENEN if y == P else b.brick())
            for y in range(P + 1, P + 6):
                b.set(x, y, z, AIR)
            if abs(bb) == 4 or a == a0 + 8:
                b.fence(x, P + 1, z)
    for (a, bb) in ((a0, -4), (a0, 4), (a0 + 8, -4), (a0 + 8, 4)):
        x, z = rot(k, a, bb, c)
        for y in range(P + 1, P + 5):
            b.set(x, y, z, GEBEITELD if y == P + 3 else STENEN)
        b.set(x, P + 5, z, GLOEIKOOL)
        b.mikakop(x, P + 6, z, rot_face("east", k))
    for bb in (-1, 0, 1):                              # the way in from the stairs
        x, z = rot(k, a0, bb, c)
        b.set(x, P + 1, z, AIR)
    x, z = rot(k, a0 + 4, 0, c)
    b.set(x, P + 1, z, GRILLKOOL)
    b.spawner(x, P + 2, z, "guhs:vonk_mika")
    for bb in (-1, 1):
        xx, zz = rot(k, a0 + 4, bb, c)
        b.set(xx, P + 1, zz, GLOEIKOOL)
    x, z = rot(k, a0 + 1, 3, c)
    b.chest(x, P + 1, z, rot_face("west", k), "guhs:chests/spiesburcht")
    x, z = rot(k, a0 - 4, 2, c)
    b.sign(x, Y + 1, z, rot_face("west", k), ["spiesburcht.vonk1", "spiesburcht.vonk2", "spiesburcht.vonk3"], wall=False)
    b.must_reach["the spawner platform"] = (rot(k, a0 + 2, 0, c)[0], P + 1, rot(k, a0 + 2, 0, c)[1])


def bridge_tower(b, c, Y, k, name):
    """A tower at the end of a bridge: a room on the deck with a chest, windows, battlements and a Mika head."""
    a0 = SB_C - 9
    for a in range(a0, a0 + 9):
        for bb in range(-4, 5):
            x, z = rot(k, a, bb, c)
            edge = a in (a0, a0 + 8) or abs(bb) == 4
            for y in range(0, Y):
                if edge or y >= Y - 2:
                    b.set(x, y, z, b.brick(0.18))
            b.set(x, Y, z, STENEN)
            for y in range(Y + 1, Y + 8):
                b.set(x, y, z, b.brick() if edge else AIR)
            b.set(x, Y + 8, z, STENEN)
            if edge and (a + bb) % 2 == 0:
                b.set(x, Y + 9, z, STENEN)
    for bb in (-1, 0, 1):                                  # doors on both ends (the bridge goes on through the tower)
        for a in (a0, a0 + 8):
            x, z = rot(k, a, bb, c)
            for y in range(Y + 1, Y + 5):
                b.set(x, y, z, AIR)
    for bb in (-4, 4):
        x, z = rot(k, a0 + 4, bb, c)
        for y in (Y + 3, Y + 4):
            b.bars(x, y, z)
    x, z = rot(k, a0 + 2, 3, c)
    b.chest(x, Y + 1, z, rot_face("north", k), "guhs:chests/spiesburcht")
    x, z = rot(k, a0 + 6, -3, c)
    if name == "west":
        for dy in range(1, 4):
            b.set(x, Y + dy, z, WORST, {"axis": "y"})
        b.set(x, Y + 4, z, MOSTERD)
        xx, zz = rot(k, a0 + 6, -2, c)
        b.set(xx, Y + 1, zz, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel", "LootTable": "guhs:chests/spiesburcht_tuintje"})
        b.chests.append((xx, Y + 1, zz))
    elif name == "south":
        xx, zz = rot(k, a0 + 1, -2, c)
        b.sign(xx, Y + 1, zz, rot_face("south", k), ["spiesburcht.welkom1", "spiesburcht.welkom2", "spiesburcht.welkom3"], wall=False)
    else:
        b.set(x, Y + 1, z, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    x, z = rot(k, a0 + 4, 0, c)
    b.hang_lantern(x, Y + 8, z, 2)
    # a Mika face on the outside end of the tower
    for (bb, v, block) in ((-2, 6, "red_concrete"), (2, 6, "red_concrete"), (-2, 7, TRAP), (2, 7, TRAP), (-1, 5, "white_concrete"),
                           (1, 5, "white_concrete"), (0, 5, "black_concrete")):
        x, z = rot(k, a0 + 9, bb, c)
        if block == TRAP:
            b.stair(x, Y + v, z, rot_face("west" if bb < 0 else "east", k), half="top")
        else:
            b.set(x, Y + v, z, block)
    for bb in (-3, 3):
        x, z = rot(k, a0 + 9, bb, c)
        b.set(x, Y + 7, z, PILAAR, {"axis": "y"})
        b.set(x, Y + 8, z, PILAAR, {"axis": "y"})
    x, z = rot(k, a0 + 4, 0, c)
    b.mikakop(x, Y + 9, z, rot_face("east", k))
    x, z = rot(k, a0 + 4, 0, c)
    b.must_reach[f"the {name} tower"] = (x, Y + 1, z)


def check_spiesburcht(b):
    problems = check_common(b, "spiesburcht")
    c, Y = SB_C, SB_DECK
    problems += check_paths(b, "spiesburcht", (c, Y + 1, c + KEEP - 3))
    if not any(e == "guhs:vonk_mika" for (*_p, e) in b.spawners):
        problems.append("spiesburcht: no Vonk-Mika spawner")
    if len(b.chests) < 6:
        problems.append(f"spiesburcht: only {len(b.chests)} chests")
    plants = sum(1 for v in b.s.blocks.values() if v[0] in (SCHEUTJES, ZWAMMETJE))
    if plants < 16:
        problems.append(f"spiesburcht: the tuintje has only {plants} plants")
    faces = sum(1 for v in b.s.blocks.values() if v[0] == GEBEITELD) + sum(1 for v in b.s.blocks.values() if v[0] == mc("pink_terracotta"))
    heads = sum(1 for v in b.s.blocks.values() if v[0] == MIKAKOP)
    if faces < 150 or heads < 20:
        problems.append(f"spiesburcht: not enough guh faces ({faces}) / Mika heads ({heads})")
    # the bridges are long: the deck reaches the edge of the build in all four directions
    for k in range(4):
        x, z = rot(k, SB_C, 0, c)
        if b.get(x, Y, z) not in (STENEN, GEBEITELD, GEBARSTEN):
            problems.append(f"spiesburcht: bridge {k} does not reach the edge ({b.get(x, Y, z)})")
    return problems


# =====================================================================================================================
# Het Mika-grillpaleis
# =====================================================================================================================
GP = (73, 52, 73)
GP_C = 36
GP_G = 12           # the palace's ground floor (template y); the foundation goes down to y 0
OUTER = 30          # the outer wall's distance from the middle
INNER = 13          # the palace keep's half width


def grillpaleis(h):
    b = Bouw(h, GP, 27020002)
    c, G = GP_C, GP_G
    foundation(b, c, G)
    outer_wall(b, c, G)
    moat_and_gate(b, c, G)
    palace_keep(b, c, G)
    walkways(b, c, G)
    roof_and_trophies(b, c, G)
    mikas(b, c, G)
    b.connect()
    return b


def foundation(b, c, G):
    """A ragged grill-iron base, wider at the top, rising out of the sauce (y 0 to the ground floor)."""
    for x in range(GP[0]):
        for z in range(GP[2]):
            d = max(abs(x - c), abs(z - c))
            for y in range(0, G + 1):
                reach_ = OUTER + 3 - (G - y) // 3
                if d <= reach_:
                    if y == G:
                        b.set(x, y, z, GEPOLIJST if d <= OUTER else ROOSTER)
                    else:
                        b.set(x, y, z, PILAAR if (x + z) % 7 == 0 else ROOSTER if (x * 3 + z + y) % 4 else HOUTSKOOL, {"axis": "y"} if (x + z) % 7 == 0 else None)
    # clear the air over the whole ground floor
    for x in range(GP[0]):
        for z in range(GP[2]):
            if max(abs(x - c), abs(z - c)) <= OUTER + 3 - 0:
                for y in range(G + 1, G + 4):
                    b.set(x, y, z, AIR)


def outer_wall(b, c, G):
    """The curtain wall: 9 high, 2 thick, battlements, grill windows, Mika faces, and four horned corner towers."""
    top = G + 9
    for x in range(c - OUTER, c + OUTER + 1):
        for z in range(c - OUTER, c + OUTER + 1):
            d = max(abs(x - c), abs(z - c))
            if d in (OUTER, OUTER - 1):
                for y in range(G + 1, top + 1):
                    b.set(x, y, z, GEPOLIJST if y in (G + 1, top) else ROOSTER if (x + z + y) % 9 else GEBEITELD)
                if d == OUTER and (x + z) % 2 == 0:
                    b.set(x, top + 1, z, GEPOLIJST)
            elif d == OUTER - 2:
                b.set(x, top - 1, z, GEPOLIJST)                 # the walkway on top of the wall (inside ledge)
    for i in range(c - OUTER + 5, c + OUTER - 4, 6):
        for (x, z) in ((i, c - OUTER), (i, c + OUTER), (c - OUTER, i), (c + OUTER, i)):
            b.bars(x, G + 5, z)
            b.bars(x, G + 6, z)
    for (tx, tz) in ((c - OUTER - 2, c - OUTER - 2), (c + OUTER - 4, c - OUTER - 2), (c - OUTER - 2, c + OUTER - 4), (c + OUTER - 4, c + OUTER - 4)):
        horned_tower(b, tx, G, tz)
    # evil Mika faces on the three other sides of the wall
    for k in (0, 2, 3):
        mika_face(b, c, G + 2, k, OUTER + 1, big=False)


def horned_tower(b, tx, G, tz):
    """A 7x7 corner tower, 20 high, with two big Mika horns and a fire on top."""
    top = G + 20
    for x in range(tx, tx + 7):
        for z in range(tz, tz + 7):
            edge = x in (tx, tx + 6) or z in (tz, tz + 6)
            for y in range(G - 2, top):
                if edge:
                    b.set(x, y, z, PILAAR if x in (tx, tx + 6) and z in (tz, tz + 6) else ROOSTER, {"axis": "y"} if x in (tx, tx + 6) and z in (tz, tz + 6) else None)
                elif y > G:
                    b.set(x, y, z, AIR)
            b.set(x, top, z, GEPOLIJST)
            if edge and (x + z) % 2:
                b.set(x, top + 1, z, GEPOLIJST)
    for y in range(G + 4, top - 2, 5):
        for (x, z) in ((tx + 3, tz), (tx + 3, tz + 6), (tx, tz + 3), (tx + 6, tz + 3)):
            b.bars(x, y, z)
            b.bars(x, y + 1, z)
    # the horns: two curved stacks leaning out
    for side in (-1, 1):
        x, z = tx + 3 + side * 2, tz + 3
        horn = ((0, 1), (0, 2), (side, 2), (side, 3), (side, 4), (2 * side, 4), (2 * side, 5), (3 * side, 5), (3 * side, 6))
        for i, (dx, dy) in enumerate(horn):
            b.set(x + dx, top + dy, z, PILAAR if i < 7 else "red_concrete", {"axis": "y"} if i < 7 else None)
    b.set(tx + 3, top + 1, tz + 3, GRILLKOOL)
    b.set(tx + 3, top + 2, tz + 3, "campfire", {"lit": "true", "facing": "north", "signal_fire": "true", "waterlogged": "false"})
    b.hang_lantern(tx + 3, top, tz + 3, 2, soul=True)


def mika_face(b, c, y0, k, dist, big):
    """An evil Mika face on side k: angry brows, red glowing eyes, fangs, horns above (the other side of the guh faces)."""
    s = 2 if big else 1
    parts = {}
    for u in range(-5 * s, 5 * s + 1):
        for v in range(0, 8 * s):
            parts[(u, v)] = "black_concrete"
    for u, v in [(u, v) for u in (-3, -2, 2, 3) for v in (4, 5)]:
        for du in range(s):
            for dv in range(s):
                parts[(u * s + du, v * s + dv)] = "red_concrete"
    for u, v in ((-2, 5), (2, 5)):
        parts[(u * s, v * s)] = GLOEIKOOL
    for u, v in ((-4, 6), (-3, 7), (-2, 7), (4, 6), (3, 7), (2, 7)):
        for du in range(s):
            parts[(u * s + du, v * s)] = "gray_concrete"
    for u in range(-3, 4):
        parts[(u * s, 2 * s)] = "red_nether_bricks"
    for u in (-2, 2):
        parts[(u * s, 1 * s)] = "white_concrete"
    if big:
        for (u, v) in ((-5, 8), (-5, 9), (-6, 9), (-6, 10), (5, 8), (5, 9), (6, 9), (6, 10)):
            for du in range(s):
                for dv in range(s):
                    parts[(u * s + du, v * s + dv)] = PILAAR
    for (u, v), block in parts.items():
        x, z = rot(k, dist, u, c)
        b.set(x, y0 + v, z, block, {"axis": "y"} if block == PILAAR else None)
    # (the face leans on the wall behind it)


def moat_and_gate(b, c, G):
    """A ring of frying sauce inside the wall and the gate on the south side under the big Mika face."""
    for x in range(c - OUTER + 2, c + OUTER - 1):
        for z in range(c - OUTER + 2, c + OUTER - 1):
            d = max(abs(x - c), abs(z - c))
            if d in (INNER + 5, INNER + 6):
                b.set(x, G, z, SAUS, {"level": "0"})
                b.set(x, G - 1, z, GRILLKOOL)
    # the gate: through the south wall, a bridge over the moat, a portcullis of grill bars pulled up
    for x in range(c - 2, c + 3):
        for y in range(G + 1, G + 6):
            for z in (c + OUTER, c + OUTER - 1):
                b.set(x, y, z, AIR)
        for z in range(c + INNER + 1, c + OUTER + 3):
            b.set(x, G, z, GEPOLIJST if abs(x - c) < 2 else PILAAR, {"axis": "z"} if abs(x - c) == 2 else None)
        b.bars(x, G + 6, c + OUTER)
    for x in (c - 3, c + 3):
        for y in range(G + 1, G + 7):
            b.set(x, y, c + OUTER + 1, PILAAR, {"axis": "y"})
        b.set(x, G + 7, c + OUTER + 1, GLOEIKOOL)
    for x in range(c - 12, c + 13):                      # the gatehouse: a tall wall to hang the big face on
        for z in (c + OUTER - 1, c + OUTER):
            for y in range(G + 1, G + 29):
                if abs(x - c) <= 2 and y <= G + 5:
                    continue
                b.set(x, y, z, GEPOLIJST if y in (G + 1, G + 28) or abs(x - c) == 12 else ROOSTER)
            if (x - c) % 2 == 0:
                b.set(x, G + 29, c + OUTER, GEPOLIJST)
    mika_face(b, c, G + 7, 1, OUTER + 1, big=True)
    b.sign(c + 3, G + 2, c + OUTER + 2, "south", ["grillpaleis.poort1", "grillpaleis.poort2", "grillpaleis.poort3"])
    # the approach outside the gate (a landing on the foundation)
    for x in range(c - 3, c + 4):
        for z in range(c + OUTER + 1, c + OUTER + 4):
            b.set(x, G, z, GEPOLIJST)
            for y in range(G + 1, G + 6):
                if b.get(x, y, z) in (None, AIR):
                    b.set(x, y, z, AIR)


def palace_keep(b, c, G):
    """The keep: the treasure hall with the mountain of stolen kaasknabbels, the empty throne, two floors above."""
    x0, x1 = c - INNER, c + INNER
    floors = (G, G + 9, G + 17)
    top = G + 25
    for x in range(x0, x1 + 1):
        for z in range(x0, x1 + 1):
            edge = x in (x0, x1) or z in (x0, x1)
            b.set(x, G, z, GEPOLIJST if (x + z) % 2 else ROOSTER)
            for y in range(G + 1, top + 1):
                if edge:
                    b.set(x, y, z, PILAAR if x in (x0, x1) and z in (x0, x1) else GEPOLIJST if y in floors or y == top else ROOSTER,
                          {"axis": "y"} if x in (x0, x1) and z in (x0, x1) else None)
                elif y in floors[1:] or y == top:
                    b.set(x, y, z, GEPOLIJST)
                else:
                    b.set(x, y, z, AIR)
            if edge and (x + z) % 2 == 0:
                b.set(x, top + 1, z, GEPOLIJST)
                b.set(x, top + 2, z, TRALIES if (x + z) % 4 == 0 else GEPOLIJST)
    # windows on the upper floors
    for i in range(x0 + 3, x1 - 2, 4):
        for fy in (G + 12, G + 13, G + 20, G + 21):
            for (x, z) in ((i, x0), (i, x1), (x0, i), (x1, i)):
                b.bars(x, fy, z)
    # the door (south) with gloeikool torches
    for x in range(c - 2, c + 3):
        for y in range(G + 1, G + 6):
            b.set(x, y, x1, AIR)
    for x in (c - 3, c + 3):
        b.set(x, G + 5, x1 + 1, GLOEIKOOL)
    # --- the treasure hall ---
    # the mountain of stolen kaasknabbels: a stepped heap in the middle
    for layer in range(5):
        r = 4 - layer
        for x in range(c - r, c + r + 1):
            for z in range(c - 2 - r, c - 2 + r + 1):
                if layer < 4 or (x + z) % 2 == 0:
                    b.set(x, G + 1 + layer, z, KNABBELS)
    b.set(c, G + 6, c - 2, VADS)
    b.sign(c, G + 1, c + 3, "south", ["grillpaleis.stapel1", "grillpaleis.stapel2", "grillpaleis.stapel3"], wall=False)
    # vads treasure chests around the heap, and sacks (barrels) of stolen knabbels
    for (x, z, f) in ((c - 6, c - 7, "south"), (c + 6, c - 7, "south"), (c - 6, c + 2, "north"), (c + 6, c + 2, "north")):
        b.set(x, G + 1, z, VADS)
        b.chest(x, G + 2, z, f, "guhs:chests/grillpaleis_schat")
    for (x, z) in ((x0 + 1, x0 + 1), (x0 + 2, x0 + 1), (x0 + 1, x0 + 2), (x1 - 1, x0 + 1), (x1 - 2, x0 + 1), (x1 - 1, x0 + 2)):
        b.chest(x, G + 1, z, "south", "guhs:chests/grillpaleis_voorraad", barrel=True)
    # the empty Mika throne (no boss: the Big Nether-Mika is out stealing more knabbels)
    for x in range(c - 2, c + 3):
        for z in range(x0 + 1, x0 + 4):
            b.set(x, G + 1, z, GEPOLIJST)
    b.stair(c, G + 2, x0 + 2, "north", block="minecraft:blackstone_stairs")
    b.set(c - 1, G + 2, x0 + 2, GEPOLIJST)
    b.set(c + 1, G + 2, x0 + 2, GEPOLIJST)
    for y in range(G + 2, G + 6):
        b.set(c, y, x0 + 1, PILAAR, {"axis": "y"})
    b.set(c - 1, G + 5, x0 + 1, "red_concrete")
    b.set(c + 1, G + 5, x0 + 1, "red_concrete")
    b.mikakop(c, G + 6, x0 + 1, "south")
    b.sign(c + 2, G + 2, x0 + 3, "south", ["grillpaleis.troon1", "grillpaleis.troon2", "grillpaleis.troon3"], wall=False)
    for x in range(c - 1, c + 2):
        for z in range(x0 + 4, c - 7):
            b.set(x, G + 1, z, "red_carpet")
    # fire bowls in the four corners, a hanging lantern ring
    for (x, z) in ((x0 + 2, x1 - 2), (x1 - 2, x1 - 2)):
        b.set(x, G + 1, z, GRILLKOOL)
        b.set(x, G + 2, z, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    for (x, z) in ((c - 7, c - 9), (c + 7, c - 9), (c - 7, c + 5), (c + 7, c + 5)):
        b.hang_lantern(x, G + 9, z, 2, soul=True)
    # --- first floor: the storerooms with caged plush guhs (stolen from guh children!) ---
    F = floors[1]
    for (cx, cz) in ((c - 7, c - 7), (c + 7, c - 7), (c - 7, c + 7)):
        cage(b, cx, F + 1, cz)
    b.chest(x1 - 2, F + 1, x1 - 2, "west", "guhs:chests/grillpaleis_voorraad")
    b.chest(x1 - 2, F + 1, x1 - 4, "west", "guhs:chests/grillpaleis_voorraad", barrel=True)
    b.sign(c, F + 1, c + 3, "south", ["grillpaleis.knuffel1", "grillpaleis.knuffel2", "grillpaleis.knuffel3"], wall=False)
    # stairs: ground floor -> first floor (east side, going north), first -> second (west side), second -> roof (east)
    stairs_up(b, x1 - 3, G, c + 6, "north", 8, (x1 - 3, x1 - 1))
    stairs_up(b, x0 + 1, F, c + 6, "north", 7, (x0 + 1, x0 + 3))
    stairs_up(b, x1 - 3, floors[2], c + 6, "north", 7, (x1 - 3, x1 - 1))
    # --- second floor: the Mika's grill kitchen (a big grill with sausages, sauce vats) ---
    S = floors[2]
    for x in range(c - 4, c + 5):
        for z in range(c - 3, c + 1):
            b.set(x, S + 1, z, GEPOLIJST if x in (c - 4, c + 4) or z in (c - 3, c) else GRILLKOOL)
            if x not in (c - 4, c + 4) and z not in (c - 3, c):
                b.bars(x, S + 2, z)
    for x in range(c - 3, c + 3):
        b.set(x, S + 3, c - 2, WORST, {"axis": "x"})
    b.set(c - 1, S + 3, c - 1, VLEES)
    b.set(c + 1, S + 3, c - 1, VLEES)
    b.chest(c - 6, S + 1, c + 6, "east", "guhs:chests/grillpaleis_voorraad")
    b.must_reach.update({"the treasure": (c - 6, G + 1, c - 5), "the throne": (c - 2, G + 1, x0 + 4), "the first floor": (c, F + 1, c),
                         "the kitchen": (c, S + 1, c + 3), "the roof": (c - 6, top + 1, c)})


def stairs_up(b, x0, floor, z_start, facing, steps, xs):
    """A straight stair (3 wide) from a floor up `steps` blocks, northwards, cutting a hole in the ceiling above it."""
    for i in range(steps):
        z = z_start - i
        for x in range(xs[0], xs[1] + 1):
            b.stair(x, floor + 1 + i, z, facing)
            for y in range(floor + 1, floor + 1 + i):
                b.set(x, y, z, GEPOLIJST)
            for y in range(floor + 2 + i, floor + 5 + i):
                b.set(x, y, z, AIR)


def cage(b, cx, y, cz):
    """A cage of grill bars with a pink plush guh inside."""
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            b.set(x, y - 1, z, GEPOLIJST)
            edge = x in (cx - 2, cx + 2) or z in (cz - 2, cz + 2)
            for dy in range(0, 3):
                if edge:
                    b.bars(x, y + dy, z)
                else:
                    b.set(x, y + dy, z, AIR)
            b.set(x, y + 3, z, GEPOLIJST)
    for x in range(cx - 1, cx + 2):
        b.set(x, y, cz, "pink_wool")
    b.set(cx, y + 1, cz, "pink_wool")
    b.set(cx - 1, y + 1, cz, "pink_carpet")
    b.set(cx + 1, y + 1, cz, "pink_carpet")
    b.set(cx, y, cz - 1, "black_wool")


def walkways(b, c, G):
    """Bridges from the wall walk to the keep's first floor (bastion style), and stairs up to the wall walk."""
    top = G + 9
    F = G + 9
    for k in (0, 2):
        for a in range(INNER + 1, OUTER - 1):
            for bb in (-1, 0, 1):
                x, z = rot(k, a, bb, c)
                b.set(x, F, z, GEPOLIJST)
                for y in range(F + 1, F + 4):
                    b.set(x, y, z, AIR)
            for bb in (-2, 2):
                x, z = rot(k, a, bb, c)
                b.set(x, F, z, GEPOLIJST)
                b.bars(x, F + 1, z)
        for bb in (-1, 0, 1):                              # doorways in the keep and a gap in the wall's inner side
            x, z = rot(k, INNER, bb, c)
            for y in range(F + 1, F + 4):
                b.set(x, y, z, AIR)
        # supports under the walkway
        for a in (INNER + 4, OUTER - 5):
            x, z = rot(k, a, 0, c)
            for y in range(G + 1, F):
                b.set(x, y, z, PILAAR, {"axis": "y"})
    # a stair up to the wall walk inside the north wall (from the courtyard)
    for i in range(8):
        for x in range(c - 6, c - 3):
            b.stair(x, G + 1 + i, c - OUTER + 3 + 7 - i, "north", block="minecraft:polished_blackstone_brick_stairs")
            for y in range(G + 1, G + 1 + i):
                b.set(x, y, c - OUTER + 3 + 7 - i, GEPOLIJST)
    # the wall-walk ledge at top-1 all round, 2 wide
    for x in range(c - OUTER + 2, c + OUTER - 1):
        for z in range(c - OUTER + 2, c + OUTER - 1):
            if max(abs(x - c), abs(z - c)) in (OUTER - 2, OUTER - 3):
                b.set(x, top - 1, z, GEPOLIJST)
                for y in range(top, top + 2):
                    b.set(x, y, z, AIR)
    b.must_reach["the wall walk"] = (c - OUTER + 3, top, c)
    b.must_reach["the east walkway"] = (rot(0, INNER + 5, 0, c)[0], F + 1, rot(0, INNER + 5, 0, c)[1])


def roof_and_trophies(b, c, G):
    """The keep's roof (a smoking grill chimney with horns, sacks of stolen knabbels, Mika heads on the battlements),
    evil Mika faces high on the keep, and the guh faces the Mikas "won" hung on the outside of the curtain wall."""
    top = G + 25
    x0, x1 = c - INNER, c + INNER
    # the grill chimney in the middle of the roof
    for x in range(c - 3, c + 4):
        for z in range(c - 3, c + 4):
            edge = x in (c - 3, c + 3) or z in (c - 3, c + 3)
            for y in range(top + 1, top + 9):
                if edge:
                    b.set(x, y, z, PILAAR if x in (c - 3, c + 3) and z in (c - 3, c + 3) else GEPOLIJST if y % 3 == 0 else ROOSTER,
                          {"axis": "y"} if x in (c - 3, c + 3) and z in (c - 3, c + 3) else None)
            b.set(x, top + 9, z, GEPOLIJST if edge else GRILLKOOL)
    for x in range(c - 2, c + 3):
        for z in range(c - 2, c + 3):
            b.bars(x, top + 10, z) if (x + z) % 2 else b.set(x, top + 10, z, ROOKGAT)
    for (dx, dz) in ((-1, -1), (1, 1)):
        b.set(c + dx, top + 11, c + dz, "campfire", {"lit": "true", "facing": "north", "signal_fire": "true", "waterlogged": "false"})
    for side in (-1, 1):                               # two big horns on the chimney
        x = c + side * 3
        for i, (dx, dy) in enumerate(((0, 10), (0, 11), (side, 11), (side, 12), (side, 13), (2 * side, 13), (2 * side, 14), (2 * side, 15))):
            b.set(x + dx, top + dy, c, PILAAR if i < 6 else "red_concrete", {"axis": "y"} if i < 6 else None)
    for (x, z) in ((c - 3, c - 4), (c + 3, c - 4), (c - 3, c + 4), (c + 3, c + 4)):
        b.lantern(x, top + 1, z, soul=True)
    # sacks of stolen knabbels in the roof corners
    for (x, z) in ((x0 + 2, x0 + 2), (x1 - 3, x0 + 2), (x0 + 2, x1 - 3), (x1 - 3, x1 - 3)):
        for dx in (0, 1):
            for dz in (0, 1):
                b.set(x + dx, top + 1, z + dz, KNABBELS)
        b.set(x + (x < c), top + 2, z + (z < c), KNABBELS)
    # Mika heads on the battlement posts of the keep
    for x in range(x0, x1 + 1, 4):
        for z in (x0, x1):
            if b.get(x, top + 1, z) == GEPOLIJST:
                b.mikakop(x, top + 2, z, "north" if z == x0 else "south")
    # evil Mika faces high on all four sides of the keep
    for k in range(4):
        mika_face(b, c, G + 14, k, INNER + 1, big=False)
    # the "trophies": guh faces (carved houtskoolsteen) hung on the outside of the curtain wall, with lanterns
    for i in range(c - OUTER + 4, c + OUTER - 3, 5):
        for k in range(4):
            if k == 1 and abs(i - c) <= 13:
                continue                                 # (the gatehouse)
            x, z = rot(k, OUTER + 1, i - c, c)
            if abs(i - c) >= OUTER - 3:
                continue
            b.set(x, G + 6, z, GEBEITELD)
            b.set(x, G + 7, z, GLOEIKOOL)
    # piles of loot and grill stations in the courtyard
    for (x, z) in ((c - 24, c - 12), (c + 24, c + 12), (c - 24, c + 14), (c + 22, c - 14)):
        for dx in range(3):
            for dz in range(2):
                b.set(x + dx, G + 1, z + dz, KNABBELS if (dx + dz) % 2 else "barrel", {"facing": "up", "open": "false"} if (dx + dz) % 2 == 0 else None)
        b.set(x + 1, G + 2, z, KNABBELS)
    for (x, z) in ((c - 22, c + 4), (c + 22, c - 4)):
        b.set(x, G + 1, z, GRILLKOOL)
        b.bars(x, G + 2, z)
        b.set(x + 1, G + 1, z, GRILLKOOL)
        b.bars(x + 1, G + 2, z)
        b.set(x, G + 3, z, WORST, {"axis": "x"})
        b.set(x + 1, G + 3, z, WORST, {"axis": "x"})


def mikas(b, c, G):
    """The Nether-Mikas at home: in the courtyard, the treasure hall, on the walkways and the wall."""
    spots = [(c - 23, G + 1, c), (c + 23, G + 1, c + 4), (c - 10, G + 1, c + 23), (c + 10, G + 1, c - 23), (c + 8, G + 1, c + 22),
             (c - 9, G + 1, c + 5), (c + 9, G + 1, c + 6), (c - 9, G + 1, c - 9), (c + 3, G + 10, c - 8), (c - 4, G + 10, c + 6),
             (c + 21, G + 10, c), (c - 21, G + 10, c + 1), (c, G + 18, c + 5), (c + 4, G + 18, c + 3)]
    for i, (x, y, z) in enumerate(spots):
        b.mob(x, y, z, "guhs:nether_mika", yaw=(i * 67) % 360)


def check_grillpaleis(b):
    problems = check_common(b, "mika_grillpaleis")
    c, G = GP_C, GP_G
    problems += check_paths(b, "mika_grillpaleis", (c, G + 1, c + OUTER + 2))
    if len(b.chests) < 8:
        problems.append(f"mika_grillpaleis: only {len(b.chests)} chests")
    knabbels = sum(1 for v in b.s.blocks.values() if v[0] == KNABBELS)
    if knabbels < 60:
        problems.append(f"mika_grillpaleis: the stolen pile has only {knabbels} knabbel blocks")
    mikas_ = sum(1 for e in b.entities if e[3] == "guhs:nether_mika")
    if mikas_ < 10:
        problems.append(f"mika_grillpaleis: only {mikas_} Nether-Mikas")
    if any(e[3] not in ("guhs:nether_mika",) for e in b.entities):
        problems.append("mika_grillpaleis: a boss or other creature snuck in")
    return problems


def build_all(h):
    """Builds, checks and saves both; returns {name: (tiles_x, tiles_z, anchor, problems)}."""
    out = {}
    sb = spiesburcht(h)
    problems = check_spiesburcht(sb)
    nx, nz, _ = sb.save_tiles("spiesburcht")
    out["spiesburcht"] = (nx, nz, (SB_C, SB_DECK, SB_C), problems, sb)
    gp = grillpaleis(h)
    problems = check_grillpaleis(gp)
    nx, nz, _ = gp.save_tiles("mika_grillpaleis")
    out["mika_grillpaleis"] = (nx, nz, (GP_C, GP_G, GP_C), problems, gp)
    return out
