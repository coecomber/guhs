"""Elf-Guhjestocht village 9: Guhlingen (the mill village).

A big pink-thatched stellingmolen "De Vahoege Wiek" with a guh face as the sail hub, pink guh-ear tips on its white sails
and guh ears on its cap; a steaming oliebollen- en knabbelbollenkraam with a giant powdered-sugar knabbelbol (with a guh
face and ears) on the roof; bakkerij "De Vadse Wiek" with a trapgevel, twin guh faces and ears on both gables; knotwilgen,
flour sacks, sneeuwpopguhs, lantern posts with flag garlands, vuurkorven and a cheering crowd on the quay.
Contract: elftocht_dorp_api.py (plot 28x32x24, ice south of the plot, front = south).
"""
INDEX = 9
NAAM = "Guhlingen"

# ---------------------------------------------------------------------------------------------------------------------
# Small building kit (local plot coordinates: x 0..27 west->east, z 0..23 north->south (the ice is south of z=23),
# y 0 = first block above the ground layer; y -1 = the ground itself).
# ---------------------------------------------------------------------------------------------------------------------
try:
    import elftocht_dorp_api as api
except ImportError:  # (when loaded as tools/features/...)
    from features import elftocht_dorp_api as api

FULL = ("minecraft:", "guhs:")
LAMPIONS = ("guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint")


def _mc(name):
    return name if name.startswith(FULL) else "minecraft:" + name


class Kit:
    def __init__(self, s, ox, oy, oz):
        self.s, self.ox, self.oy, self.oz = s, ox, oy, oz
        self.lichten, self.boost, self.publiek = [], [], []

    # --- raw access ---------------------------------------------------------------------------------------------------
    def abs(self, x, y, z):
        return self.ox + x, self.oy + y, self.oz + z

    def set(self, x, y, z, name, props=None, nbt=None):
        ax, ay, az = self.abs(x, y, z)
        if not api.inside(self.ox, self.oy, self.oz, ax, ay, az):
            raise SystemExit(f"elftocht dorp {NAAM}: block outside the plot at local {(x, y, z)} ({name})")
        self.s.set(ax, ay, az, _mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(*self.abs(x, y, z))

    def air(self, x, y, z):
        return self.get(x, y, z) in (None, "minecraft:air")

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if not only_empty or self.air(x, y, z):
                        self.set(x, y, z, name, props)

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "air")

    # --- block helpers ----------------------------------------------------------------------------------------------
    def stairs(self, x, y, z, name, facing, half="bottom"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def slab(self, x, y, z, name, typ="bottom"):
        self.set(x, y, z, name, {"type": typ, "waterlogged": "false"})

    def log(self, x, y, z, name, axis="y"):
        self.set(x, y, z, name, {"axis": axis})

    def face(self, x, y, z, facing="south", stemming=0, hout=False):
        """A guh face block (knuffelsteen or vadshout) looking `facing`."""
        self.set(x, y, z, "guhs:vadshout_gezicht" if hout else "guhs:knuffelsteen_gezicht",
                 {"facing": facing, "stemming": str(stemming)})

    def door(self, x, y, z, facing, hinge="left", name="guhs:vadshout_deur"):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, name, {"facing": facing, "half": half, "hinge": hinge, "open": "false",
                                          "powered": "false"})

    def lampion(self, x, y, z, kleur=0, hanging=False, licht=True):
        self.set(x, y, z, LAMPIONS[kleur % 3] if isinstance(kleur, int) else kleur,
                 {"hanging": "true" if hanging else "false", "waterlogged": "false"})
        if licht:
            self.lichten.append(self.abs(x, y, z))

    def lantern(self, x, y, z, hanging=False, soul=False):
        self.set(x, y, z, "soul_lantern" if soul else "lantern",
                 {"hanging": "true" if hanging else "false", "waterlogged": "false"})
        self.lichten.append(self.abs(x, y, z))

    def vuurkorf(self, x, z, y=0):
        """A fire basket: a dark iron stand with a crackling campfire on top (high enough not to step in)."""
        self.set(x, y, z, "polished_blackstone_wall", {"up": "true", "north": "none", "south": "none", "east": "none",
                                                       "west": "none", "waterlogged": "false"})
        self.set(x, y + 1, z, "campfire", {"lit": "true", "facing": "south", "signal_fire": "false", "waterlogged": "false"})
        self.lichten.append(self.abs(x, y + 1, z))

    def sneeuwpop(self, x, z, facing="south", y=0):
        self.set(x, y, z, "guhs:sneeuwpopguh", {"facing": facing, "half": "lower"})
        self.set(x, y + 1, z, "guhs:sneeuwpopguh", {"facing": facing, "half": "upper"})

    def sign(self, x, y, z, facing, lines, wall=True, wood="spruce", colour="white"):
        """A waxed, glowing sign; 1.2.0: every line is a translate key sign.guhs.elftocht.dorp<INDEX>.<slug> with the Dutch
        text as fallback (tools/sign_text.py), so each player reads it in their own language."""
        import json
        import sign_text
        from make_structures import Byte, NbtList
        msgs = sign_text.messages(f"sign.guhs.elftocht.dorp{INDEX}", lines)
        empty = NbtList(8, [json.dumps("")] * 4)
        nbt = {"id": "minecraft:sign", "is_waxed": Byte(1),
               "front_text": {"messages": msgs, "color": colour, "has_glowing_text": Byte(1)},
               "back_text": {"messages": empty, "color": "black", "has_glowing_text": Byte(0)}}
        if wall:
            self.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    def banner(self, x, y, z, soort="nl", facing="south", wall=False):
        """A banner: 'nl' (rood-wit-blauw), 'oranje', 'guh' (roze with a white guh-ear roundel), 'fries' (blue/white)."""
        from make_structures import compounds
        pats = {"nl": ("white", [("stripe_top", "red"), ("stripe_bottom", "blue")]),
                "oranje": ("orange", [("border", "yellow")]),
                "guh": ("pink", [("circle", "white"), ("triangles_top", "magenta"), ("border", "magenta")]),
                "fries": ("white", [("stripe_downright", "light_blue"), ("small_stripes", "light_blue"),
                                    ("border", "blue")]),
                "sterren": ("blue", [("flower", "yellow"), ("border", "light_blue")]),
                "sneeuw": ("light_blue", [("cross", "white"), ("border", "white")])}[soort]
        nbt = {"id": "minecraft:banner", "patterns": compounds([{"pattern": "minecraft:" + p, "color": c}
                                                                for p, c in pats[1]])}
        if wall:
            self.set(x, y, z, f"{pats[0]}_wall_banner", {"facing": facing}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{pats[0]}_banner", {"rotation": rot}, nbt)

    def vlaggenmast(self, x, z, h=4, soort="nl", facing="south", paal="spruce_fence"):
        self.fill(x, 0, z, x, h - 1, z, paal)
        self.banner(x, h, z, soort, facing)

    def oren(self, cx, y, z, gap=2, blok="pink_wool", binnen="pink_terracotta", axis="x"):
        """A pair of guh ears standing on a ridge/top at height y, centred on cx (along x, or along z with axis='z')."""
        for side in (-1, 1):
            a = cx + side * gap
            b = a + side
            pts = [(a, y, blok), (b, y, blok), (a, y + 1, binnen), (b, y + 1, blok), (b, y + 2, blok)]
            for (p, yy, name) in pts:
                if axis == "x":
                    self.set(p, yy, z, name)
                else:
                    self.set(z, yy, p, name)

    def sneeuw(self, x0, x1, z0, z1, op, laag=2, ymin=0):
        """Snow layers on every column top (in the box) whose top block is one of `op` (full blocks only)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(api.PLOT_Y - 2, ymin - 1, -1):
                    n = self.get(x, y, z)
                    if n in (None, "minecraft:air"):
                        continue
                    if n in op and self.air(x, y + 1, z):
                        self.set(x, y + 1, z, "snow", {"layers": str(laag)})
                    break

    def publiek_guh(self, x, z, yaw=0, y=0):
        api.publiek(self.s, *self.abs(x, y, z), yaw)
        self.publiek.append((*self.abs(x, y, z), yaw))

    def guh(self, x, y, z, yaw=0, variant=None, scale=1.0, tags=None, stil=False, zit=False):
        """A decoration guh (stil = stays put, e.g. behind a counter; zit = sitting, e.g. on a sled)."""
        from make_structures import Byte, Double, NbtList, compounds, floats
        nbt = {"id": "guhs:guh", "PersistenceRequired": Byte(1), "Rotation": floats(float(yaw), 0.0),
               "attributes": compounds([{"id": "minecraft:scale", "base": Double(scale)}])}
        if stil:
            nbt["NoAI"] = Byte(1)
        if zit:
            nbt["Sitting"] = Byte(1)
        if variant:
            nbt["Variant"] = variant
        if tags:
            nbt["Tags"] = NbtList(8, list(tags))
        ax, ay, az = self.abs(x, y, z)
        self.s.entity(ax + 0.5, ay, az + 0.5, nbt)

    def stempelpost(self, x, z, dak="guhs:pluisdak_plaat", paal="spruce_fence", naam=None, soort="nl"):
        """The stamp booth: 5x4 canopy on four posts, the Stempelguh at (x, 0, z) facing the ice, a stamp table beside him,
        a flag on the roof and a sign with the village name. z should be 21/22 (the canal edge is z = 23)."""
        for px in (x - 2, x + 2):
            for pz in (z - 2, z + 1):
                self.fill(px, 0, pz, px, 2, pz, paal)
        for px in range(x - 3, x + 4):
            for pz in range(z - 3, z + 2):
                self.slab(px, 3, pz, dak)
        self.fill(x - 2, -1, z - 2, x + 2, -1, z + 1, "guhs:vadshout_planken")
        self.set(x + 1, 0, z - 1, "guhs:guh_tafel", {"facing": "south"})
        self.set(x - 1, 0, z - 1, "barrel", {"facing": "up", "open": "false"})
        self.lampion(x - 1, 1, z - 1, 0)
        self.lampion(x - 1, 2, z + 1, 1, hanging=True)
        self.lampion(x + 1, 2, z + 1, 2, hanging=True)
        self.set(x, 4, z - 1, "guhs:vadshout_planken")
        self.banner(x, 5, z - 1, soort)
        self.face(x, 4, z, "south", 1, hout=True)
        if naam:
            self.sign(x, 2, z - 2, "south", naam, wall=True)
            self.set(x, 2, z - 3, "guhs:vadshout_planken")
            self.set(x, 1, z - 3, "guhs:vadshout_planken")
            self.set(x, 0, z - 3, "guhs:vadshout_planken")

    def hekken(self):
        """Connect every fence in the plot to its neighbouring fences and solid walls (templates keep block states as they
        are saved, so a fence row would otherwise stay a row of loose posts)."""
        vast = ("bricks", "knuffelsteen", "planks", "planken", "concrete", "wool", "terracotta", "pluisdak", "stam", "log",
                "glass", "barrel", "hay_block")
        for x in range(api.PLOT_X):
            for z in range(api.PLOT_Z):
                for y in range(-1, api.PLOT_Y):
                    n = self.get(x, y, z)
                    if not n or not (n.endswith("_fence") or n.endswith("_hek")):
                        continue
                    props = {"waterlogged": "false"}
                    for side, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                        m = self.s.get(*self.abs(x + dx, y, z + dz)) or ""
                        ok = m.endswith("_fence") or m.endswith("_hek") or m.endswith("_fence_gate") or (
                            any(v in m for v in vast) and not any(v in m for v in ("slab", "stairs", "plaat", "trap", "wall", "muur", "pane")))
                        props[side] = "true" if ok else "false"
                    self.set(x, y, z, n, props)

MX, MZ, HY = 10, 8, 18          # mill axis (x, z) and the sail hub height
SAIL = 9                         # sail arm length
DAK = {"guhs:pluisdak", "minecraft:bricks", "guhs:vadshout_planken", "minecraft:spruce_planks", "guhs:knuffelsteen",
       "minecraft:white_concrete", "minecraft:brown_terracotta", "minecraft:pink_wool", "guhs:knotwilg_bladeren",
       "minecraft:hay_block", "minecraft:barrel"}


def _oct(dx, dz, r):
    return max(abs(dx), abs(dz)) <= r and abs(dx) + abs(dz) <= r + r // 2


def _molen(k):
    # --- brick base (y 0..6) with a knuffelsteen band, door + guh face on the south side ---
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            x, z = MX + dx, MZ + dz
            if _oct(dx, dz, 5):
                k.set(x, -1, z, "guhs:knuffelklinkers")
                if not _oct(dx, dz, 4):
                    for y in range(0, 7):
                        k.set(x, y, z, "guhs:knuffelsteen" if y in (0, 6) else "bricks")
    k.door(MX, 0, MZ + 5, "south")
    k.face(MX, 2, MZ + 5, "south", 0)
    for (x, z, f) in ((MX, MZ - 5, "north"), (MX - 5, MZ, "west"), (MX + 5, MZ, "east")):
        k.set(x, 3, z, "white_stained_glass")
        k.set(x, 4, z, "white_stained_glass")
    for (x, z) in ((MX - 2, MZ + 4), (MX + 2, MZ + 4)):
        k.set(x, 3, z, "white_stained_glass")
    # ladder up (against a brick pillar on the north side)
    k.fill(MX, 0, MZ - 4, MX, 6, MZ - 4, "bricks")
    for y in range(0, 9):
        k.set(MX, y, MZ - 3, "ladder", {"facing": "south", "waterlogged": "false"})
    # flour sacks and a knabbelgraan bin inside
    k.set(MX - 3, 0, MZ - 1, "white_wool")
    k.set(MX - 3, 0, MZ, "white_wool")
    k.set(MX - 3, 1, MZ, "white_wool")
    k.set(MX + 3, 0, MZ - 1, "barrel", {"facing": "up", "open": "false"})
    k.set(MX + 3, 0, MZ + 1, "hay_block", {"axis": "y"})
    k.lantern(MX + 3, 1, MZ + 1)

    # --- the stelling (gallery) at y 7 with a fence railing and struts down to the ground ---
    for dx in range(-8, 9):
        for dz in range(-8, 9):
            x, z = MX + dx, MZ + dz
            if _oct(dx, dz, 8) and not _oct(dx, dz, 4):
                k.set(x, 7, z, "spruce_planks")
                if not _oct(dx, dz, 7):
                    k.set(x, 8, z, "spruce_fence")
    for (dx, dz) in ((-8, 0), (8, 0), (0, -8), (-6, -6), (6, -6), (-6, 6), (6, 6)):
        k.fill(MX + dx, 0, MZ + dz, MX + dx, 6, MZ + dz, "spruce_fence")
    for (dx, dz, c) in ((-6, 6, 0), (6, 6, 1), (-8, 0, 2), (8, 0, 0)):
        k.lampion(MX + dx, 9, MZ + dz, c)

    # --- the pink thatched body (y 7..17) ---
    for y in range(7, 18):
        r, cut = (4, 6) if y < 11 else ((4, 5) if y < 14 else (3, 4))
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                if max(abs(dx), abs(dz)) <= r and abs(dx) + abs(dz) <= cut:
                    inner = max(abs(dx), abs(dz)) <= r - 1 and abs(dx) + abs(dz) <= cut - 1
                    x, z = MX + dx, MZ + dz
                    if not inner:
                        k.set(x, y, z, "guhs:pluisdak")
                    elif y == 7 and (dx, dz) != (0, -3):
                        k.set(x, y, z, "spruce_planks")
    k.door(MX + 4, 8, MZ, "east")                          # gallery door
    for (x, y, z) in ((MX, 12, MZ + 4), (MX - 4, 11, MZ), (MX, 15, MZ + 3), (MX, 11, MZ - 4)):
        k.set(x, y, z, "white_stained_glass")
    # knuffelsteen band + little window sills
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            if max(abs(dx), abs(dz)) <= 3 and abs(dx) + abs(dz) <= 4 and not (max(abs(dx), abs(dz)) <= 2 and abs(dx) + abs(dz) <= 3):
                k.set(MX + dx, 17, MZ + dz, "guhs:knuffelsteen")

    # --- the cap: a guh head with ears and a snowy top ---
    for dy in range(0, 4):
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                if dx * dx + dz * dz + (dy * 1.3) ** 2 <= 3.6 ** 2:
                    k.set(MX + dx, HY + dy, MZ + dz, "guhs:vadshout_planken")
    k.oren(MX, HY + 3, MZ, gap=1)
    k.set(MX, HY + 1, MZ - 4, "guhs:vadshout_planken")          # the tail beam (staart) at the back
    k.fill(MX, HY - 1, MZ - 5, MX, HY + 1, MZ - 5, "guhs:vadshout_stam", {"axis": "y"})
    # axle + hub (the hub IS a guh face)
    k.log(MX, HY, MZ + 4, "guhs:vadshout_stam", "z")
    k.face(MX, HY, MZ + 5, "south", 2, hout=True)
    # --- the sails (a + in "vreugdestand"), white cloth with pink guh-ear tips ---
    for (dx, dy) in ((0, 1), (1, 0), (0, -1), (-1, 0)):
        px, py = dy, -dx                                      # the cloth side (the trailing edge)
        for i in range(1, SAIL + 1):
            x, y = MX + dx * i, HY + dy * i
            k.log(x, y, MZ + 5, "guhs:vadshout_stam", "y" if dx == 0 else "x")
            if i >= 3:
                tip = i >= SAIL - 1
                k.set(x + px, y + py, MZ + 5, "pink_wool" if tip else "white_wool")
                k.set(x + 2 * px, y + 2 * py, MZ + 5, "pink_terracotta" if tip else ("spruce_planks" if i % 3 == 0 else "white_wool"))
    # mill name board under the gallery
    k.sign(MX + 1, 2, MZ + 6, "south", ["De Vahoege Wiek", "maalt knabbelgraan", "tot knabbelmeel!"], wall=False)


def _bakkerij(k):
    x0, x1, z0, z1 = 19, 26, 1, 9
    k.fill(x0, -1, z0, x1, -1, z1, "spruce_planks")
    for y in range(0, 5):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    corner = x in (x0, x1) and z in (z0, z1)
                    k.set(x, y, z, "guhs:knuffelsteen" if corner or y == 4 else "bricks")
    # windows with green shutters, door, sign
    for (x, z, f) in ((20, z1, "south"), (25, z1, "south"), (20, z0, "north"), (25, z0, "north")):
        k.set(x, 1, z, "glass")
        k.set(x, 2, z, "glass")
    for (x, y) in ((21, 3), (24, 3)):
        k.set(x, y, z1, "glass")
        k.set(x, y, z0, "glass")
    for z in (3, 5, 7):
        k.set(x0, 2, z, "glass")
        k.set(x1, 2, z, "glass")
    for sx in (21, 24):                                       # green shutters beside the ground-floor windows
        for y in (1, 2):
            k.set(sx, y, z1, "green_terracotta")
    k.door(22, 0, z1, "south", "left")
    k.door(23, 0, z1, "south", "right")
    k.sign(22, 3, z1 + 1, "south", ["Bakkerij", "De Vadse Wiek", "vers knabbelbrood"], wall=True)
    k.sign(23, 3, z1 + 1, "south", ["Hier geen Mika's", "met snaaipoten!", "njeg njeg"], wall=True)
    # the roof: pluisdak layers, ridge north-south, trapgevels on both ends
    for i in range(0, 4):
        y = 5 + i
        for z in range(z0 + 1, z1):
            k.set(x0 + i, y, z, "guhs:pluisdak")
            k.set(x1 - i, y, z, "guhs:pluisdak")
    steps = ((5, 20, 25), (6, 20, 25), (7, 21, 24), (8, 21, 24), (9, 22, 23), (10, 22, 23))
    for z in (z0, z1):
        for (y, a, b) in steps:
            for x in range(a, b + 1):
                k.set(x, y, z, "bricks")
        for (y, x) in ((7, 20), (7, 25), (9, 21), (9, 24), (11, 22), (11, 23)):
            k.slab(x, y, z, "guhs:knuffelsteen_plaat")
        k.face(22, 7, z, "south" if z == z1 else "north", 1)
        k.face(23, 7, z, "south" if z == z1 else "north", 3)
        # guh ears on top of the gable
        for (x, y, n) in ((22, 12, "pink_wool"), (22, 13, "pink_wool"), (21, 13, "pink_wool"),
                          (23, 12, "pink_wool"), (23, 13, "pink_wool"), (24, 13, "pink_wool")):
            k.set(x, y, z, n)
        k.set(22, 11, z, "pink_terracotta")
        k.set(23, 11, z, "pink_terracotta")
    # chimney with a little smoke fire
    k.fill(25, 5, 3, 25, 9, 3, "bricks")
    k.set(25, 10, 3, "campfire", {"lit": "true", "facing": "south", "signal_fire": "false", "waterlogged": "false"})
    # inside: oven, counter, bread
    k.set(x0 + 1, 0, z0 + 1, "smoker", {"facing": "south", "lit": "true"})
    k.set(x0 + 2, 0, z0 + 1, "smoker", {"facing": "south", "lit": "true"})
    k.set(x1 - 1, 0, z0 + 1, "barrel", {"facing": "up", "open": "false"})
    k.set(x1 - 1, 0, z0 + 2, "white_wool")
    for x in range(x0 + 2, x1 - 1):
        k.set(x, 0, z0 + 4, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
    k.fill(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, "spruce_planks")
    k.lantern(22, 3, z0 + 4, hanging=True)
    k.lantern(23, 3, z0 + 6, hanging=True)
    # guh-ijsbloempjes in window boxes along the front
    for x in (20, 25):
        k.set(x, 0, z1 + 1, "guhs:guh_bloempot", {"facing": "south", "gewaterd": "true"})


def _kraam(k):
    """The oliebollen- en knabbelbollenkraam with a giant knabbelbol on its roof (boost: warme kaasmelk)."""
    x0, x1, z0, z1 = 19, 26, 12, 16
    for y in range(0, 4):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    k.set(x, y, z, "pink_concrete" if (x + (1 if z == z1 else 0)) % 2 else "white_concrete")
    k.fill(x0, 4, z0, x1, 4, z1, "white_concrete")
    k.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "spruce_planks")
    # counter opening + goodies
    k.clear(x0 + 1, 1, z1, x1 - 1, 2, z1)
    for x in range(x0 + 1, x1):
        k.set(x, 0, z1, "guhs:vadshout_planken")
        k.set(x, 1, z1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"}) if x in (21, 24) else None
    k.set(22, 1, z1, "guhs:theepotje", {"facing": "south"})
    k.set(23, 1, z1, "guhs:theepotje", {"facing": "south"})
    k.boost.append(k.abs(22, 1, z1))
    k.boost.append(k.abs(23, 1, z1))
    k.set(x0 + 1, 0, z0 + 1, "guhs:frying_pan", {"facing": "south"})
    k.set(x0 + 2, 0, z0 + 1, "smoker", {"facing": "south", "lit": "true"})
    k.lantern(22, 3, z0 + 2, hanging=True)
    # red-white awning + signs
    for x in range(x0, x1 + 1):
        k.set(x, 3, z1 + 1, "red_wool" if x % 2 else "white_wool")
    k.sign(21, 4, z1 + 1, "south", ["OLIEBOLLEN", "&", "KNABBELBOLLEN"], wall=True, colour="yellow")
    k.sign(24, 4, z1 + 1, "south", ["Warme kaasmelk", "erbij? Wordt je", "lekker VAHOEG!"], wall=True, colour="yellow")
    k.door(x1, 0, 14, "east")
    # the giant knabbelbol: brown ball, powdered sugar, raisins, a guh face and ears
    cx, cy, cz = 22.5, 7, 14
    for x in range(20, 26):
        for y in range(5, 10):
            for z in range(12, 17):
                d = ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5
                if d <= 2.6:
                    top = y >= 8 and d > 1.6
                    k.set(x, y, z, "white_concrete_powder" if top else "brown_terracotta")
    for (x, y, z) in ((20, 7, 14), (25, 6, 13), (21, 6, 12), (24, 8, 12)):
        if not k.air(x, y, z):
            k.set(x, y, z, "black_concrete")
    k.face(22, 7, 16, "south", 0, hout=True)
    k.face(23, 7, 16, "south", 2, hout=True)
    for (x, y) in ((21, 10), (21, 11), (20, 11), (24, 10), (24, 11), (25, 11)):
        k.set(x, y, 14, "brown_terracotta" if y == 10 else "pink_wool")


def _kade(k):
    # klinkers along the quay and the paths
    k.fill(0, -1, 19, 27, -1, 23, "guhs:knuffelklinkers")
    k.fill(MX - 1, -1, MZ + 6, MX + 1, -1, 18, "guhs:knuffelklinkers")
    k.fill(17, -1, 10, 27, -1, 11, "guhs:knuffelklinkers")
    k.fill(17, -1, 12, 18, -1, 18, "guhs:knuffelklinkers")
    k.stempelpost(5, 22, naam=["Stempelpost", "GUHLINGEN", "stempel 9 van 11", "plof!"], soort="fries")
    # lantern posts with flag garlands along the ice
    posts = (0, 11, 18, 27)
    for x in posts:
        k.fill(x, 0, 23, x, 2, 23, "spruce_fence")
        k.lampion(x, 3, 23, x % 3)
    for a, b in zip(posts, posts[1:]):
        for x in range(a + 1, b):
            if k.air(x, 3, 23):
                k.set(x, 3, 23, "guhs:vlaggetjes", {"axis": "x"})
    for (x, z) in ((9, 23), (16, 23), (22, 23)):
        k.set(x, 0, z, "guhs:vadshout_stam", {"axis": "y"})         # bolders
    k.vuurkorf(13, 19)
    k.vuurkorf(26, 19)
    k.vuurkorf(1, 18)
    for x in (14, 15):
        k.set(x, 0, 18, "guhs:guh_bank", {"facing": "south"})
    k.vlaggenmast(8, 18, 5, "nl")
    k.vlaggenmast(17, 18, 5, "guh")
    for (x, z) in ((10, 21), (12, 22), (15, 21), (20, 22), (24, 21)):
        k.publiek_guh(x, z, 0)


def _tuin(k):
    # knotwilgen (pollard willows) with snowy heads
    for (x, z) in ((1, 2), (1, 9), (4, 16)):
        k.fill(x, 0, z, x, 2, z, "guhs:knotwilg_stam", {"axis": "y"})
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) == (0, 0) or abs(dx) + abs(dz) == 1:
                    k.set(x + dx, 3, z + dz, "guhs:knotwilg_bladeren", {"persistent": "true", "distance": "1"})
        k.set(x, 3, z, "guhs:knotwilg_stam", {"axis": "y"})
        k.set(x, 4, z, "guhs:knotwilg_bladeren", {"persistent": "true", "distance": "1"})
    # sneeuwpopguhs, a flour cart, guh-molentjes, ice flowers
    k.sneeuwpop(7, 16, "south")
    k.sneeuwpop(13, 16, "south")
    k.set(6, 0, 13, "hay_block", {"axis": "x"})
    k.set(6, 0, 12, "hay_block", {"axis": "x"})
    k.set(6, 1, 12, "white_wool")
    k.set(14, 0, 13, "barrel", {"facing": "up", "open": "false"})
    k.set(14, 1, 13, "white_wool")
    k.set(3, 0, 12, "guhs:guh_molentje", {"facing": "south"})
    k.set(16, 0, 6, "guhs:guh_molentje", {"facing": "south"})
    k.sign(3, 0, 6, "south", ["Welkom in", "GUHLINGEN", "molendorp van", "de kaasknabbels"], wall=False)
    for (x, z) in ((0, 13), (2, 14), (5, 10), (15, 3), (16, 11), (3, 4), (12, 17), (27, 7), (18, 3)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:guh_ijsbloempje")
    for (x, z) in ((2, 6), (4, 1), (15, 14), (17, 1), (0, 16), (8, 1)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:rijpsprietjes")
    # a baker guh behind the counter and a few kids with sleds by the mill
    k.guh(22, 0, 14, 0, scale=0.9, tags=["guhs_elftocht_bakker"], stil=True)


def bouw(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    _kade(k)
    _molen(k)
    _bakkerij(k)
    _kraam(k)
    _tuin(k)
    k.sneeuw(0, 27, 0, 23, DAK, laag=2, ymin=1)
    k.hekken()
    x, y, z = k.abs(5, 0, 22)
    api.stempelguh(s, x, y, z, 0, INDEX)
    return {"stempelguh": (x, y, z, 0), "publiek": k.publiek, "boost": k.boost, "lichten": k.lichten}


def check(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    x, y, z = 5, 0, 22
    if k.get(x, y - 1, z) in (None, "minecraft:air") or not k.air(x, y, z) or not k.air(x, y + 1, z):
        raise SystemExit(f"{NAAM}: the Stempelguh has no floor or no room")
    for dz in range(z + 1, api.PLOT_Z):
        if not k.air(x, 0, dz) or not k.air(x, 1, dz):
            raise SystemExit(f"{NAAM}: the way from the ice to the Stempelguh is blocked")
    # the mill door and the bakkerij doors are whole and have free space in front
    for (x, z, dz) in ((MX, MZ + 5, 1), (22, 9, 1), (23, 9, 1)):
        lo, hi = s.get(*k.abs(x, 0, z)), s.get(*k.abs(x, 1, z))
        if lo != hi or not lo or not lo.endswith("deur"):
            raise SystemExit(f"{NAAM}: door at {(x, z)} is not whole")
        if not k.air(x, 0, z + dz) or not k.air(x, 1, z + dz):
            raise SystemExit(f"{NAAM}: door at {(x, z)} is blocked")
    # the sails stay inside the plot and clear the gallery
    if MX - SAIL < 0 or MX + SAIL + 2 >= api.PLOT_X or HY + SAIL >= api.PLOT_Y:
        raise SystemExit(f"{NAAM}: the sails don't fit")
