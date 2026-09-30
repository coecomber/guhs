"""Elf-Guhjestocht village 11: Dokguh (the last village before the finish: snow, sleds and Pinguhs).

A giant sneeuwguh "Sneeuwbert" (three snowballs, coal eyes, orange snoet, rosy cheeks, snowy guh ears, a red scarf and
stick arms with mittens) and his little sneeuwguh family; a sledging hill with sleds and sledging guhtjes; a fenced
Pinguh-kolonie on the ice with an iglo that has a guh face and ears, fish barrels and waddling Pinguhs; the "warme
kaasmelk" kiosk; and on the quay a big snow-white lampion arch ("Nog eentje! Dan naar huis, VAHOEG!") with hanging lampions,
a guh face as keystone and guh ears on top, with the Stempelguh right under it.
Contract: elftocht_dorp_api.py (plot 28x32x24, ice south of the plot, front = south).
"""
INDEX = 11
NAAM = "Dokguh"

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
        """A waxed, glowing sign with literal Dutch text (the same in every language, like all guh signs)."""
        import json
        from make_structures import Byte, NbtList
        msgs = NbtList(8, [json.dumps({"text": t}) for t in (list(lines) + [""] * 4)[:4]])
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
               "attributes": compounds([{"id": "minecraft:generic.scale", "base": Double(scale)}])}
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

SG = (6, 7)                      # the giant sneeuwguh (x, z)
BOOG = (13.5, 5, 19, 6.5, 5.5)   # lampion arch: centre x, centre y, z, outer and inner radius
STEMPEL = (13, 0, 21)
DAK = {"guhs:pluiswolblok", "minecraft:spruce_planks", "guhs:vadshout_planken", "minecraft:snow_block",
       "guhs:knotwilg_bladeren", "minecraft:white_concrete", "minecraft:light_blue_concrete", "minecraft:barrel",
       "minecraft:spruce_log", "minecraft:stripped_spruce_log"}


def _bol(k, cx, cy, cz, r, name, keep=None):
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if y >= 0 and ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5 <= r and (keep is None or keep(x, y, z)):
                    k.set(x, y, z, name)


def _voorkant(k, x, y, z_from=23):
    """The front-most filled z at column (x, y)."""
    for z in range(z_from, -1, -1):
        if not k.air(x, y, z):
            return z
    return None


def _sneeuwguh(k):
    cx, cz = SG
    _bol(k, cx, 2.3, cz, 3.4, "snow_block")
    _bol(k, cx, 7.2, cz, 2.6, "snow_block")
    _bol(k, cx, 11.2, cz, 2.2, "snow_block")
    # face on the head: eyes, snoet (nose), cheeks
    for (x, y, n) in ((cx - 1, 12, "black_concrete"), (cx + 1, 12, "black_concrete"), (cx - 2, 11, "pink_concrete"),
                      (cx + 2, 11, "pink_concrete"), (cx, 10, "black_concrete")):
        z = _voorkant(k, x, y)
        k.set(x, y, z, n)
    z = _voorkant(k, cx, 11)
    k.set(cx, 11, z, "orange_concrete")
    k.set(cx, 11, z + 1, "orange_concrete")                      # the snoet sticks out
    # coal buttons
    for y in (6, 8):
        z = _voorkant(k, cx, y)
        k.set(cx, y, z, "black_concrete")
    # red scarf around the neck + a tail hanging down the front
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
            if 1.5 < d <= 2.6 and not k.air(x, 9, z) or (1.5 < d <= 2.3 and k.air(x, 9, z)):
                k.set(x, 9, z, "red_wool")
    z = _voorkant(k, cx + 1, 8)
    k.set(cx + 1, 8, z + 1, "red_wool")
    k.set(cx + 1, 7, z + 1, "red_wool")
    # snowy guh ears with a pink inside
    k.oren(cx, 13, cz, gap=1, blok="snow_block", binnen="pink_concrete")
    # stick arms with red mittens
    for (d, n) in ((-1, 4), (1, 4)):
        start = cx + d * 3
        for i in range(n):
            k.set(start + d * i, 8 + (1 if i == n - 1 else 0), cz, "spruce_fence")
        k.set(start + d * (n - 1), 10, cz, "red_wool")
        k.set(start + d * (n - 2), 9, cz, "spruce_fence")
    k.sign(cx + 3, 0, cz + 4, "south", ["Sneeuwbert", "de vahoegste", "sneeuwguh van", "de polder"], wall=False)


def _familie(k):
    for (x, z) in ((1, 15), (3, 16), (5, 15), (8, 16)):
        k.sneeuwpop(x, z, "south")
    k.set(2, 0, 16, "snow", {"layers": "3"})
    k.set(4, 0, 16, "snow", {"layers": "2"})
    k.set(7, 0, 15, "snow", {"layers": "3"})


def _sleeheuvel(k):
    """A sledging hill falling to the south, with sleds and sledging guhtjes."""
    x0, x1 = 11, 16
    for z in range(0, 10):
        h = max(0, 5 - int(z * 0.62))
        for x in range(x0, x1 + 1):
            if h:
                k.fill(x, 0, z, x, h - 1, z, "snow_block")
    # the top platform with a start flag and a sled ready to go
    k.fill(x1, 5, 0, x1, 7, 0, "spruce_fence")
    k.banner(x1, 8, 0, "sneeuw", "south")
    k.set(x0, 5, 0, "barrel", {"facing": "up", "open": "false"})
    for (x, z, y) in ((13, 1, 5), (12, 11, 0), (14, 12, 0), (16, 11, 0)):
        k.slab(x, y, z, "spruce_slab")
        k.stairs(x, y, z + 1, "spruce_stairs", "north")
    k.sign(x0 - 1, 0, 10, "south", ["SLEEHEUVEL", "sleeen tot je", "zo vahoeg bent", "als een kaasbol"], wall=False)
    for (x, y, z) in ((13, 5.5, 1), (12, 0.5, 11), (16, 0.5, 11)):         # guhtjes sitting on their sleds
        k.guh(x, y, z, 0, scale=0.6, tags=["guhs_elftocht_sleeguhtje"], stil=True, zit=True)
    x, z = 15, 6
    y = 0
    while not k.air(x, y, z):
        y += 1
    k.guh(x, y, z, 180, scale=0.6, tags=["guhs_elftocht_sleeguhtje"])


def _kolonie(k):
    """The fenced Pinguh-kolonie: an ice floor, an iglo with a guh face, snow mounds, fish barrels and Pinguhs."""
    x0, x1, z0, z1 = 17, 27, 0, 12
    k.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "packed_ice")
    k.fill(x0 + 2, -1, z0 + 8, x0 + 5, -1, z1 - 1, "blue_ice")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                k.set(x, 0, z, "spruce_fence")
    gx = 22
    k.set(gx, 0, z1, "spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})
    for x in (x0, x1):
        for z in (z0, z1):
            k.set(x, 1, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
            k.lichten.append(k.abs(x, 1, z))
    k.sign(gx - 1, 0, z1 + 1, "south", ["PINGUH-KOLONIE", "Niet aaien?", "Wel aaien!", "(heel zachtjes)"], wall=False)
    # the iglo
    ix, iz, r = 23, 5, 3.3
    _bol(k, ix, 0, iz, r, "snow_block", keep=lambda x, y, z: ((x - ix) ** 2 + y ** 2 + (z - iz) ** 2) ** 0.5 > r - 1.0)
    k.fill(ix, -1, iz - 2, ix, -1, iz + 2, "white_wool")
    k.clear(ix, 0, iz + 3, ix, 1, iz + 3)
    k.face(ix, 2, iz + 2, "south", 0)
    k.oren(ix, 4, iz, gap=1, blok="snow_block", binnen="pink_concrete")
    k.set(ix - 1, 0, iz - 1, "barrel", {"facing": "up", "open": "false"})
    k.lampion(ix + 1, 0, iz - 1, 1)
    # snow mounds and fish barrels
    for (x, z, h) in ((19, 2, 2), (26, 10, 1), (19, 5, 1)):
        k.fill(x, 0, z, x, h - 1, z, "snow_block")
        k.set(x, h, z, "snow", {"layers": "3"})
    for (x, z) in ((26, 1), (25, 1)):
        k.set(x, 0, z, "barrel", {"facing": "up", "open": "false"})
    # Pinguhs (klassiek and keizer come from the variant itself), some little ones
    for (x, z, yaw, sc) in ((20, 9, 0, 1.0), (21, 10, 30, 0.6), (25, 9, 200, 1.0), (19, 7, 90, 1.0), (26, 7, 270, 0.6),
                            (24, 10, 0, 1.0), (20, 3, 150, 0.6)):
        k.guh(x, 0, z, yaw, variant="pinguh", scale=sc, tags=["guhs_elftocht_pinguh"])


def _kiosk(k):
    """The warme-kaasmelk kiosk (boost), a little log hut with a snowy pluiswol roof."""
    x0, x1, z0, z1 = 21, 26, 14, 17
    for y in range(0, 3):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    corner = x in (x0, x1) and z in (z0, z1)
                    k.set(x, y, z, "spruce_log" if corner else "stripped_spruce_log", {"axis": "y" if corner else "x"})
    k.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "spruce_planks")
    k.clear(x0 + 1, 1, z1, x1 - 1, 2, z1)
    for x in range(x0 + 1, x1):
        k.set(x, 0, z1, "guhs:vadshout_planken")
    for x in (x0 + 2, x0 + 3):
        k.set(x, 1, z1, "guhs:theepotje", {"facing": "south"})
        k.boost.append(k.abs(x, 1, z1))
    k.set(x0 + 1, 1, z1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
    k.set(x1 - 1, 1, z1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
    k.fill(x0 - 1, 3, z0 - 1, x1 + 1, 3, z1 + 1, "guhs:pluiswolblok")
    k.fill(x0, 4, z0, x1, 4, z1, "guhs:pluiswolblok")
    k.fill(x0 + 1, 5, z0 + 1, x1 - 1, 5, z1 - 1, "guhs:pluiswolblok")
    k.face(23, 4, z1, "south", 1)
    k.face(24, 4, z1, "south", 3)
    for (x, y, n) in ((22, 6, "pink_wool"), (22, 7, "pink_wool"), (21, 7, "pink_wool"),
                      (25, 6, "pink_wool"), (25, 7, "pink_wool"), (26, 7, "pink_wool")):
        k.set(x, y, 15, n)
    k.sign(23, 3, z1 + 2, "south", ["WARME KAASMELK", "voor koude", "guhpootjes", "VAHOEG!"], wall=True, colour="yellow")
    k.lantern(23, 2, z0 + 1, hanging=True)
    k.door(x1, 0, z0 + 1, "east")
    k.guh(x0 + 2, 0, z0 + 1, 0, scale=0.9, tags=["guhs_elftocht_kraamguh"], stil=True)


def _boog(k):
    """The lampion arch over the Stempelguh."""
    cx, cy, z, ro, ri = BOOG
    for x in range(0, 28):
        for y in range(0, 14):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if y >= cy and ri <= d <= ro + 0.2:
                k.set(x, y, z, "guhs:pluiswolblok" if (x + y) % 3 else "pink_wool")
    for x in (7, 20):
        k.fill(x, 0, z, x, int(cy) - 1, z, "guhs:vadshout_stam", {"axis": "y"})
        k.set(x, 0, z, "guhs:knuffelsteen")
    # lampions hanging under the inner edge
    i = 0
    for x in range(8, 20):
        for y in range(13, 0, -1):
            if not k.air(x, y, z) and k.air(x, y - 1, z) and y - 1 >= 3:
                if (x + i) % 2 == 0:
                    k.lampion(x, y - 1, z, i, hanging=True)
                i += 1
                break
    # keystone face, ears, signs
    top = max(y for y in range(14) if not k.air(13, y, z))
    k.face(13, top, z, "south", 0)
    k.face(14, top, z, "south", 1)
    k.set(13, top + 1, z, "guhs:pluiswolblok")
    k.set(14, top + 1, z, "guhs:pluiswolblok")
    for (x, y) in ((12, 1), (12, 2), (11, 2), (15, 1), (15, 2), (16, 2)):
        k.set(x, top + y, z, "pink_wool")
    for (x, y) in ((12, 1), (15, 1)):
        k.set(x, top + y, z, "pink_concrete")
    ys = int(cy) + 3
    ring = [x for x in range(0, 28) if not k.air(x, ys, z)]
    k.sign(min(ring), ys, z + 1, "south", ["NOG EENTJE!", "dan naar huis,", "VAHOEG!"], wall=True, colour="yellow")
    k.sign(max(ring), ys, z + 1, "south", ["Stempelpost", "DOKGUH", "stempel 11 van 11", "plof!"], wall=True, colour="yellow")


def _sneeuwfort(k):
    """A little snowball fort with crenellations, a guh face, a pile of snowballs and a guhtje on guard."""
    x0, x1, z0, z1 = 0, 4, 0, 3
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                if not (z == z1 and x == 2):
                    k.set(x, 0, z, "snow_block")
                    k.set(x, 1, z, "snow_block")
                    if (x + z) % 2 == 0:
                        k.set(x, 2, z, "snow_block")
    k.face(1, 1, z1, "south", 2)
    k.face(3, 1, z1, "south", 0)
    k.set(1, 0, 1, "white_concrete_powder")
    k.set(2, 0, 1, "white_concrete_powder")
    k.set(1, 1, 1, "snow", {"layers": "4"})
    k.fill(3, 0, 1, 3, 1, 1, "spruce_fence")
    k.banner(3, 2, 1, "guh", "south")
    k.guh(2, 0, 2, 0, scale=0.6, tags=["guhs_elftocht_sleeguhtje"], stil=True)


def _boom(k):
    """The guh-kerstboom on the square: a snowy spruce cone with lampions and a star lantern on top."""
    cx, cz = 9, 2
    leaves = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    k.fill(cx, 0, cz, cx, 1, cz, "spruce_log", {"axis": "y"})
    for y in range(2, 9):
        r = 2 if y <= 3 else (1 if y <= 6 else 0)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) <= r:
                    k.set(cx + dx, y, cz + dz, "spruce_leaves", leaves)
    k.set(cx, 9, cz, "gold_block")
    k.set(cx, 10, cz, "guhs:sterrenlantaarn")
    k.lichten.append(k.abs(cx, 10, cz))
    for (x, y, z, c) in ((cx - 2, 2, cz, 0), (cx + 2, 2, cz, 1), (cx, 1, cz + 2, 2)):
        k.lampion(x, y, z, c, hanging=True)
    for (x, y, z, c) in ((cx - 1, 4, cz + 1, 1), (cx + 1, 4, cz - 1, 0), (cx + 1, 7, cz, 1), (cx - 2, 4, cz, 0)):
        k.set(x, y, z, "guhs:lampion_roze" if c == 0 else "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
        k.lichten.append(k.abs(x, y, z))


def _lantaarns(k):
    """Ice-crystal lamp posts (glowing ijspegelguh kristallen) along the path behind the quay."""
    for (x, z) in ((3, 12), (8, 12), (19, 13)):
        k.fill(x, 0, z, x, 1, z, "guhs:vadshout_stam", {"axis": "y"})
        k.set(x, 2, z, "guhs:ijspegelguh_kristal")
        k.lichten.append(k.abs(x, 2, z))


def _kade(k):
    k.fill(0, -1, 18, 27, -1, 23, "guhs:knuffelklinkers")
    k.fill(0, -1, 13, 20, -1, 13, "guhs:knuffelklinkers")
    k.fill(9, -1, 13, 10, -1, 17, "guhs:knuffelklinkers")
    sx, sy, sz = STEMPEL
    k.fill(sx - 2, -1, sz - 1, sx + 2, -1, sz + 1, "guhs:vadshout_planken")
    k.set(sx + 2, 0, sz - 1, "guhs:guh_tafel", {"facing": "south"})
    k.set(sx - 2, 0, sz - 1, "barrel", {"facing": "up", "open": "false"})
    k.lampion(sx - 2, 1, sz - 1, 0)
    for x in (sx - 3, sx + 4):
        k.vlaggenmast(x, sz, 4, "nl")
    for (x, z) in ((2, 21), (4, 22), (9, 21), (18, 21), (22, 22), (25, 21)):
        k.publiek_guh(x, z, 0)
    for x in (0, 6, 21, 27):
        k.set(x, 0, 23, "snow_block")
        k.set(x, 1, 23, "guhs:ijspegelguh_kristal")
        k.lichten.append(k.abs(x, 1, 23))
    k.vuurkorf(1, 19)
    k.vuurkorf(26, 19)
    k.vuurkorf(11, 16)
    for x in (4, 5):
        k.set(x, 0, 18, "guhs:guh_bank", {"facing": "south"})
    for (x, z) in ((0, 12), (10, 1), (1, 1), (16, 13), (0, 17), (7, 12), (27, 13)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:guh_ijsbloempje")
    for (x, z) in ((2, 11), (9, 2), (15, 14), (19, 16), (0, 5)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:rijpsprietjes")


def bouw(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    _kade(k)
    _sneeuwguh(k)
    _familie(k)
    _sleeheuvel(k)
    _kolonie(k)
    _kiosk(k)
    _boog(k)
    _sneeuwfort(k)
    _boom(k)
    _lantaarns(k)
    k.sneeuw(0, 27, 0, 23, DAK, laag=2, ymin=1)
    k.hekken()
    x, y, z = k.abs(*STEMPEL)
    api.stempelguh(s, x, y, z, 0, INDEX)
    return {"stempelguh": (x, y, z, 0), "publiek": k.publiek, "boost": k.boost, "lichten": k.lichten}


def check(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    x, y, z = STEMPEL
    if k.get(x, y - 1, z) in (None, "minecraft:air") or not k.air(x, y, z) or not k.air(x, y + 1, z):
        raise SystemExit(f"{NAAM}: the Stempelguh has no floor or no room")
    for dz in range(z + 1, api.PLOT_Z):
        if not k.air(x, 0, dz) or not k.air(x, 1, dz):
            raise SystemExit(f"{NAAM}: the way from the ice to the Stempelguh is blocked")
    # the Pinguh fence is closed all round (except the gate)
    for xx in range(17, 28):
        for zz in range(0, 13):
            if xx in (17, 27) or zz in (0, 12):
                n = k.get(xx, 0, zz) or ""
                if not (n.endswith("_fence") or n.endswith("_fence_gate")):
                    raise SystemExit(f"{NAAM}: hole in the Pinguh fence at {(xx, zz)}")
    lo, hi = k.get(26, 0, 15), k.get(26, 1, 15)
    if lo != hi or not lo or not lo.endswith("deur"):
        raise SystemExit(f"{NAAM}: kiosk door is not whole")
