"""
De barbecueputten (structure guhs:barbecueput), a ruined-portal parody: broken giant barbecues with a half-missing
grillkool frame, charred guh statues, guh faces on every side and a chest. Three templates share one structure:
  - barbecueput_groot: the big pit, with the Grillguh (his quest: mend the frame, win back the Aanmaakblokjes, light it)
  - barbecueput_klein_a / barbecueput_klein_b: small broken pits, no NPC
Each template has a centre jigsaw (guhs:barbecueput_midden) in its ground layer G, and a geometry self-check
(check()) that build() runs: the frame must be completable, nothing floats, the Grillguh and the chest stand on
something, lanterns hang or stand, the plants grow on something they like.
"""
import math
import random

MIDDEN = "guhs:barbecueput_midden"


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


G_BIG, G_SMALL = 4, 3
BIG = (37, 20, 37)
SMALL = (19, 13, 19)
NPC_BIG = (27.5, G_BIG + 1.0, 14.5)
HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
HEK = "guhs:houtskoolsteen_stenen_hek"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
GRILLKOOL = "guhs:grillkool"
TRALIES = "guhs:roosterijzer_tralies"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
WORST = "guhs:worst_stam"
SATE = "guhs:sate_stam"
VLEES = "guhs:sate_vlees"
MOSTERD = "guhs:mosterd_blok"
PLASJE = "guhs:pindasausplasje"
SMEUL = "guhs:smeulkooltjes"
VERKOOLD = "guhs:verkoold_guhbot"

# blocks you can stand in / that don't need to connect to anything to count as supported
THIN = (PLASJE, SMEUL, "minecraft:lantern", "minecraft:campfire", mc("chain"))


class Pit:
    def __init__(self, h, size, g, seed):
        self.s = h.Structure(size)
        self.h = h
        self.W, self.H, self.D = size
        self.G = g
        self.rng = random.Random(seed)
        self.frame = []          # intended grillkool positions (the whole frame, corners included)
        self.inner = []          # the inside of the frame
        self.npc = None
        self.chests = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    # --- pieces ------------------------------------------------------------------------------------------------------
    def ground(self, radius, paved, cx, cz):
        """The charred ground: a paved plaza in the middle, then ash and houtskoolsteen, frayed at the edge. Air above."""
        G = self.G
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot(x - cx, z - cz)
                wobble = self.rng.uniform(-1.2, 1.2)
                if d > radius + wobble:
                    continue
                if d < paved + wobble * 0.5:
                    top = self.rng.choice([STENEN] * 5 + [GEBARSTEN] * 3 + [HOUTSKOOL])
                else:
                    top = self.rng.choice([HOUTSKOOL] * 3 + [AS_AARDE] * 3 + [AS] * 2 + [GEBARSTEN])
                self.set(x, G, z, top)
                for y in range(max(0, G - 2), G):
                    self.set(x, y, z, HOUTSKOOL if d < paved else AS_AARDE)
                for y in range(G + 1, self.H):
                    self.set(x, y, z, "air")

    def stand(self, x, z, block, props=None, y=None):
        self.set(x, (self.G + 1) if y is None else y, z, block, props)

    def grill_frame(self, x0, y0, z, missing, along_x=True, cracked=()):
        """A 4x5 grillkool frame (inside 2x3) with its bottom row at y0; `missing` frame spots are left empty, `cracked`
        ones are filled with cracked bricks (break them, then put grillkool there)."""
        for i in range(4):
            for j in range(5):
                edge = i in (0, 3) or j in (0, 4)
                x, zz = (x0 + i, z) if along_x else (x0, z + i)
                if edge:
                    self.frame.append((x, y0 + j, zz))
                    if (i, j) in missing:
                        self.set(x, y0 + j, zz, "air")
                    elif (i, j) in cracked:
                        self.set(x, y0 + j, zz, GEBARSTEN)
                    else:
                        self.set(x, y0 + j, zz, GRILLKOOL)
                else:
                    self.inner.append((x, y0 + j, zz))
                    self.set(x, y0 + j, zz, "air")

    def charred_guh(self, x0, z0, facing, scale=1):
        """A charred guh statue: sooty body, head with glowing gloeikool eyes, pink-ish cheeks, ears and a tail.
        facing: 'north' / 'south' (the face is on that side)."""
        G = self.G
        body, fur = "black_wool", "black_wool"
        front = -1 if facing == "north" else 1
        # body 5 wide, 3 deep, 3 high
        for x in range(x0, x0 + 5):
            for z in range(z0 - 1, z0 + 2):
                for y in range(G + 1, G + 4):
                    self.set(x, y, z, body if (x + y + z) % 5 else "gray_wool")
        # feet
        for x in (x0, x0 + 4):
            self.set(x, G + 1, z0 + 2 * front, "gray_wool")
        # head 5 wide, 4 high, 3 deep in front of / above the body
        hz0 = z0 + front * 1
        for x in range(x0, x0 + 5):
            for z in (hz0 - 1, hz0, hz0 + 1):
                for y in range(G + 4, G + 8):
                    self.set(x, y, z, fur)
        fz = hz0 + front * 1
        self.set(x0 + 1, G + 6, fz, "guhs:gloeikool")      # eyes glow like embers
        self.set(x0 + 3, G + 6, fz, "guhs:gloeikool")
        self.set(x0, G + 5, fz, "pink_terracotta")
        self.set(x0 + 4, G + 5, fz, "pink_terracotta")
        self.set(x0 + 2, G + 5, fz, "gray_wool")
        # ears
        for x in (x0, x0 + 4):
            self.set(x, G + 8, hz0, fur)
            self.set(x, G + 9, hz0, "gray_wool")
        # tail
        self.set(x0 + 2, G + 2, z0 - 2 * front, "gray_wool")
        self.set(x0 + 2, G + 3, z0 - 2 * front, "gray_wool")

    def chest(self, x, y, z, facing, loot):
        self.set(x, y, z, "chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 {"id": "minecraft:chest", "LootTable": loot})
        self.chests.append((x, y, z))

    def lantern_post(self, x, z, height=3):
        for y in range(self.G + 1, self.G + 1 + height):
            self.set(x, y, z, MUUR, wall_props())
        self.set(x, self.G + 1 + height, z, "lantern", {"hanging": "false", "waterlogged": "false"})

    def midden(self, x, z):
        floor = self.s.blocks.get((x, self.G, z))
        name, props = (floor[0], floor[1]) if floor else (STENEN, {})
        final = name + ("[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else "")
        self.s.set(x, self.G, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def wall_props(**kw):
    p = {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"}
    p.update(kw)
    return p


def fence_props(**kw):
    p = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    p.update(kw)
    return p


def bars(east_west=True):
    ew, ns = ("true", "false") if east_west else ("false", "true")
    return {"east": ew, "west": ew, "north": ns, "south": ns, "waterlogged": "false"}


# =====================================================================================================================
# the big pit with the Grillguh
# =====================================================================================================================
def groot(h):
    p = Pit(h, BIG, G_BIG, 27012001)
    W, H, D = BIG
    G = p.G
    cx, cz = W // 2, D // 2          # 18, 18
    p.ground(17, 12, cx, cz)

    # --- the barbecue: a long brick grill, walls 3 high, a grate on top, dead coals inside ---
    x0, x1, z0, z1 = cx - 6, cx + 6, cz + 1, cz + 5          # 12..24, 19..23
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            wall = x in (x0, x1) or z in (z0, z1)
            for y in range(G + 1, G + 4):
                if wall:
                    p.set(x, y, z, STENEN if p.rng.random() > 0.2 else GEBARSTEN)
                elif y == G + 1:
                    p.set(x, y, z, p.rng.choice(["coal_block", AS, AS, "guhs:houtskoolsteen"]))
                elif y == G + 2:
                    p.set(x, y, z, "campfire", {"lit": "false", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
                else:
                    p.set(x, y, z, "air")
    # guh faces all along the front and the sides of the barbecue
    for x in range(x0 + 1, x1, 3):
        p.set(x, G + 2, z0, GEBEITELD)
        p.set(x, G + 2, z1, GEBEITELD)
    for z in (z0 + 2,):
        p.set(x0, G + 2, z, GEBEITELD)
        p.set(x1, G + 2, z, GEBEITELD)
    # the rim and the grate
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                p.set(x, G + 4, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
            else:
                p.set(x, G + 4, z, TRALIES, bars(True))
    # on the grate: two giant sausages and a saté skewer, all a bit burnt
    for x in range(x0 + 2, x0 + 7):
        p.set(x, G + 5, z0 + 1, WORST, {"axis": "x"})
        p.set(x + 3, G + 5, z0 + 3, WORST, {"axis": "x"})
    p.set(x0 + 4, G + 6, z0 + 1, MOSTERD)
    p.set(x0 + 7, G + 6, z0 + 3, MOSTERD)
    for x in range(x1 - 5, x1):
        p.set(x, G + 5, z0 + 2, SATE, {"axis": "x"})
    for x in (x1 - 4, x1 - 2):
        for dz in (-1, 0, 1):
            for dy in (0, 1):
                if not (dz == 0 and dy == 0):
                    if p.get(x, G + 5 + dy, z0 + 2 + dz) in (None, "minecraft:air"):
                        p.set(x, G + 5 + dy, z0 + 2 + dz, VLEES)

    # --- behind the barbecue: the oven wall with the broken grillkool frame, and a chimney ---
    fz = z1 + 3                                 # 26
    fx = cx - 2                                 # 16..19
    for x in range(fx - 4, fx + 8):
        for y in range(G + 1, G + 9):
            if fx - 1 <= x <= fx + 4 and y <= G + 6:
                continue                        # the opening around the frame
            top = y == G + 8
            p.set(x, y, fz + 1, (GEBEITELD if (x - fx) % 3 == 0 and y == G + 5 else STENEN) if not top else PLAAT,
                  {"type": "bottom", "waterlogged": "false"} if top else None)
    for x in range(fx - 1, fx + 5):
        p.set(x, G + 7, fz + 1, STENEN)
        p.set(x, G + 8, fz + 1, GEBEITELD if x in (fx, fx + 3) else STENEN)
    # a chimney on the oven wall
    for y in range(G + 9, G + 14):
        for x in (fx + 5, fx + 6):
            p.set(x, y, fz + 1, STENEN)
    p.set(fx + 5, G + 14, fz + 1, "campfire", {"lit": "false", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    p.set(fx + 6, G + 14, fz + 1, GEBEITELD)
    # the frame itself: 4 spots gone (2 empty, 2 cracked bricks)
    p.grill_frame(fx, G + 1, fz, missing={(1, 4), (2, 0)}, cracked={(0, 3), (3, 2)})
    # a few grillkool blocks "fell off" and lie about (like the obsidian of a ruined portal)
    p.set(fx - 3, G + 1, fz - 1, GRILLKOOL)
    p.set(fx + 7, G + 1, fz - 2, GRILLKOOL)

    # --- two charred guh statues guarding the pit (faces to the plaza) ---
    p.charred_guh(3, cz + 5, "north")
    p.charred_guh(W - 8, cz + 5, "north")
    # and two small ones at the entrance, facing the visitors coming in (south faces the other way)
    p.charred_guh(5, 6, "south")
    p.charred_guh(W - 10, 6, "south")

    # --- the Grillguh's corner: his counter, a chest, a smoker, a barrel of charcoal ---
    nx, nz = int(NPC_BIG[0]), int(NPC_BIG[2])
    p.npc = (nx, G + 1, nz)
    for z in range(nz - 2, nz + 3):
        p.set(nx + 2, G + 1, z, STENEN)
        p.set(nx + 2, G + 2, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    p.set(nx + 2, G + 1, nz, GEBEITELD)
    p.chest(nx + 1, G + 1, nz - 2, "west", "guhs:chests/barbecueput_groot")
    p.set(nx + 1, G + 1, nz + 2, "smoker", {"facing": "west", "lit": "false"})
    p.set(nx + 1, G + 1, nz + 3, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel", "LootTable": "guhs:chests/barbecueput_klein"})
    p.chests.append((nx + 1, G + 1, nz + 3))
    for z in (nz - 3, nz + 4):
        for y in range(G + 1, G + 4):
            p.set(nx + 2, y, z, HEK, fence_props())
        p.set(nx + 2, G + 4, z, "lantern", {"hanging": "false", "waterlogged": "false"})

    # --- the ring of low broken walls, guh-face pillars at the corners, lantern posts ---
    for a in range(0, 360, 5):
        r = 12.5
        x = int(round(cx + r * math.cos(math.radians(a))))
        z = int(round(cz + r * math.sin(math.radians(a))))
        gap = 80 <= a <= 100 or 260 <= a <= 280          # the ways in (north and south)
        if gap or p.rng.random() < 0.18:
            continue
        if p.get(x, G + 1, z) in (None, "minecraft:air"):
            p.set(x, G + 1, z, MUUR, wall_props())
    for a in (45, 135, 225, 315):
        x = int(round(cx + 13.5 * math.cos(math.radians(a))))
        z = int(round(cz + 13.5 * math.sin(math.radians(a))))
        for y in range(G + 1, G + 4):
            p.set(x, y, z, STENEN if y < G + 3 else GEBEITELD)
        p.set(x, G + 4, z, "lantern", {"hanging": "false", "waterlogged": "false"})
    for (x, z) in ((cx - 4, 4), (cx + 4, 4), (cx - 4, D - 5), (cx + 4, D - 5)):
        p.lantern_post(x, z)

    # --- leftovers: sauce puddles, embers, fallen sausages ---
    for _ in range(30):
        x, z = p.rng.randrange(4, W - 4), p.rng.randrange(4, D - 4)
        if p.get(x, G + 1, z) == "minecraft:air" and p.get(x, G, z) in (HOUTSKOOL, AS_AARDE, AS, STENEN, GEBARSTEN):
            ok = p.get(x, G, z) in (HOUTSKOOL, AS_AARDE, AS)
            p.set(x, G + 1, z, SMEUL if ok and p.rng.random() < 0.6 else PLASJE)
    for (x, z) in ((7, cz - 4), (W - 12, cz - 6)):
        for i in range(4):
            if p.get(x + i, G + 1, z) == "minecraft:air":
                p.set(x + i, G + 1, z, WORST, {"axis": "x"})
    p.midden(cx, cz)
    return p


# =====================================================================================================================
# the small broken pits
# =====================================================================================================================
def klein(h, variant):
    p = Pit(h, SMALL, G_SMALL, 27012100 + variant)
    W, H, D = SMALL
    G = p.G
    cx, cz = W // 2, D // 2          # 9, 9
    p.ground(8, 4, cx, cz)
    if variant == 0:
        # a little grill knocked over, the frame standing behind it with 5 pieces missing
        for x in range(cx - 3, cx + 2):
            for z in (cz - 1, cz + 1):
                p.set(x, G + 1, z, STENEN if p.rng.random() > 0.3 else GEBARSTEN)
            p.set(x, G + 1, cz, "campfire", {"lit": "false", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
        for x in range(cx - 3, cx):
            p.set(x, G + 2, cz, TRALIES, bars(True))
        p.set(cx - 3, G + 2, cz - 1, GEBEITELD)
        p.set(cx + 1, G + 2, cz + 1, GEBEITELD)
        p.grill_frame(cx - 1, G + 1, cz + 4, missing={(1, 0), (2, 0), (2, 4)}, cracked={(0, 2)})
        p.set(cx + 4, G + 1, cz + 2, GRILLKOOL)
        p.chest(cx + 3, G + 1, cz - 2, "north", "guhs:chests/barbecueput_klein")
        # a charred guh head half in the ground
        for x in range(cx - 7, cx - 4):
            for z in range(cz - 6, cz - 3):
                for y in (G, G + 1, G + 2):
                    p.set(x, y, z, "black_wool")
        p.set(cx - 7, G + 2, cz - 3, "guhs:gloeikool")
        p.set(cx - 5, G + 2, cz - 3, "guhs:gloeikool")
        p.set(cx - 7, G + 3, cz - 5, "black_wool")
        p.set(cx - 5, G + 3, cz - 5, "black_wool")
    else:
        # the frame along z, sunk one block, with a burnt saté skewer through the grill in front of it
        p.grill_frame(cx - 4, G, cz - 2, missing={(1, 0), (1, 4), (2, 4)}, along_x=False, cracked={(0, 3)})
        # (the frame sinks one layer into the ground: its bottom is the ground there)
        for x in range(cx - 1, cx + 3):
            for z in range(cz - 2, cz + 2):
                edge = x in (cx - 1, cx + 2) or z in (cz - 2, cz + 1)
                p.set(x, G + 1, z, STENEN if edge else AS)
                if not edge:
                    p.set(x, G + 2, z, TRALIES, bars(False))
        for z in range(cz - 3, cz + 3):
            if p.get(cx, G + 3, z) in (None, "minecraft:air"):
                p.set(cx, G + 3, z, SATE, {"axis": "z"})
        p.set(cx, G + 3, cz - 1, VLEES)
        p.set(cx, G + 3, cz + 1, VLEES)
        p.set(cx - 1, G + 1, cz + 3, GEBEITELD)
        p.set(cx + 2, G + 1, cz + 3, GEBEITELD)
        p.chest(cx + 4, G + 1, cz + 3, "west", "guhs:chests/barbecueput_klein")
        p.set(cx + 5, G + 1, cz - 3, GRILLKOOL)
        for (x, z) in ((cx + 5, cz + 5), (cx - 5, cz + 5)):
            p.lantern_post(x, z, 2)
    for _ in range(10):
        x, z = p.rng.randrange(2, W - 2), p.rng.randrange(2, D - 2)
        if p.get(x, G + 1, z) == "minecraft:air" and p.get(x, G, z) in (HOUTSKOOL, AS_AARDE, AS, STENEN, GEBARSTEN):
            ok = p.get(x, G, z) in (HOUTSKOOL, AS_AARDE, AS)
            p.set(x, G + 1, z, SMEUL if ok and p.rng.random() < 0.7 else PLASJE)
    p.midden(cx, cz)
    return p


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def check(p, name):
    blocks = p.s.blocks
    problems = []

    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    def empty(c):
        return nm(c) in (None, "minecraft:air")

    # the frame: can be finished with grillkool, the inside is free, it stands on something
    have = [c for c in p.frame if nm(c) == GRILLKOOL]
    if not (4 <= len(have) <= len(p.frame) - 2):
        problems.append(f"{name}: frame has {len(have)} of {len(p.frame)} grillkool (it must be broken, but not gone)")
    for c in p.inner:
        if not empty(c):
            problems.append(f"{name}: frame inside blocked at {c}: {nm(c)}")
    for c in p.frame:
        if nm(c) not in (GRILLKOOL, GEBARSTEN, "minecraft:air", None):
            problems.append(f"{name}: frame spot {c} holds {nm(c)}")
    bottom = min(c[1] for c in p.frame)
    for c in p.frame:
        if c[1] == bottom and empty((c[0], c[1] - 1, c[2])):
            problems.append(f"{name}: frame bottom {c} hangs in the air")
    # nothing floats: every block above the ground connects to the ground through other blocks
    solid = {c for c, b in blocks.items() if b[0] not in ("minecraft:air",)}
    seen = {c for c in solid if c[1] <= p.G}
    todo = list(seen)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in seen:
                seen.add(n)
                todo.append(n)
    for c in solid:
        if c not in seen:
            problems.append(f"{name}: floating block {nm(c)} at {c}")
    # things that need something under them
    for c, b in blocks.items():
        below = nm((c[0], c[1] - 1, c[2]))
        if b[0] in (PLASJE, SMEUL, "minecraft:campfire", "minecraft:chest", "minecraft:smoker", "minecraft:barrel") or \
                (b[0] == "minecraft:lantern" and b[1].get("hanging") == "false"):
            if below in (None, "minecraft:air", PLASJE, SMEUL):
                problems.append(f"{name}: {b[0]} at {c} stands on nothing")
        if b[0] == SMEUL and below not in (HOUTSKOOL, AS, AS_AARDE):
            problems.append(f"{name}: smeulkooltjes at {c} on {below}")
    if p.npc:
        x, y, z = p.npc
        if empty((x, y - 1, z)) or not empty((x, y, z)) or not empty((x, y + 1, z)):
            problems.append(f"{name}: the Grillguh has no place to sit at {p.npc}")
    if not p.chests:
        problems.append(f"{name}: no chest")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN for b in blocks.values()):
        problems.append(f"{name}: no centre jigsaw")
    return problems


def build_all(h):
    """Builds and checks the three templates; returns the problems found (empty: all good)."""
    problems = []
    pits = {"barbecueput_groot": groot(h), "barbecueput_klein_a": klein(h, 0), "barbecueput_klein_b": klein(h, 1)}
    for name, p in pits.items():
        if name == "barbecueput_groot":
            p.s.entity(NPC_BIG[0], NPC_BIG[1], NPC_BIG[2], {"id": "guhs:guh_npc", "Kind": "grillguh", "PersistenceRequired": h.Byte(1),
                                                             "Rotation": h.floats(90.0, 0.0)})
        problems += check(p, name)
        p.s.save(name)
    return problems, pits
