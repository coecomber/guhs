"""Elf-Guhjestocht village 10: Franeguh (the star village, a nod to the Eise Eisinga planetarium).

"Het Guhsterrenhuis": a knuffelsteen house carrying a deep-blue sterrenkoepel that is itself a guh head (guh ears, a guh
face with rosy cheeks, glowing stars). Inside, under the starry dome, a hanging guh-planetarium: a glowing sun, planets on
chains and a pink guh-planet with a face, benches and a telescope. On the quay a lampion pergola (two long rows of hanging
lampions), two grachtenpandjes (klokgevel + trapgevel, guh faces and ears), the warme-chocovet kraam with a giant mug on
the roof, the ringed planet "Guhturnus" and star lanterns everywhere.
Contract: elftocht_dorp_api.py (plot 28x32x24, ice south of the plot, front = south).
"""
INDEX = 10
NAAM = "Franeguh"

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

KX, KY, KZ, KR = 13, 5, 7, 6          # the dome: centre + radius
DAK = {"guhs:pluisdak", "guhs:knuffelsteen", "minecraft:white_concrete", "minecraft:brown_concrete", "minecraft:bricks",
       "minecraft:cyan_terracotta", "minecraft:white_terracotta", "guhs:knotwilg_bladeren", "minecraft:spruce_planks"}


def _ster(x, y, z):
    """Deterministic star pattern on the dome."""
    h = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791)
    return h % 7 == 0 or h % 11 == 0


def _koepelhuis(k):
    x0, x1, z0, z1 = KX - 6, KX + 6, KZ - 6, KZ + 6          # 7..19 x 1..13
    k.fill(x0, -1, z0, x1, -1, z1, "guhs:glimtegel", {"fel": "false"})
    for (x, z) in ((KX, KZ), (KX - 3, KZ), (KX + 3, KZ), (KX, KZ - 3), (KX, KZ + 3), (KX - 2, KZ - 2), (KX + 2, KZ + 2),
                   (KX - 2, KZ + 2), (KX + 2, KZ - 2)):
        k.set(x, -1, z, "guhs:glimtegel", {"fel": "true"})
    for y in range(0, 5):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    pil = x in (x0, x1, x0 + 3, x1 - 3) and z in (z0, z1) or z in (z0, z0 + 4, z1 - 4, z1) and x in (x0, x1)
                    k.set(x, y, z, "light_blue_terracotta" if pil else "guhs:knuffelsteen")
    # tall windows
    for x in (x0 + 1, x0 + 2, x1 - 2, x1 - 1):
        for z in (z0, z1):
            k.fill(x, 1, z, x, 3, z, "light_blue_stained_glass")
    for z in (z0 + 2, KZ, z1 - 2):
        for x in (x0, x1):
            k.fill(x, 1, z, x, 3, z, "light_blue_stained_glass")
    # cornice + flat roof around the dome
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            d = ((x - KX) ** 2 + (z - KZ) ** 2) ** 0.5
            if d > KR - 1:
                k.set(x, 5, z, "guhs:knuffelsteen")
            if x in (x0, x1) or z in (z0, z1):
                k.slab(x, 6, z, "guhs:knuffelsteen_plaat")
    # the dome: deep blue with glowing stars, shell 1 thick
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(KY, KY + KR + 1):
                d = ((x - KX) ** 2 + (y - KY) ** 2 + (z - KZ) ** 2) ** 0.5
                if KR - 1.0 < d <= KR + 0.3:
                    k.set(x, y, z, ("glowstone" if (x + z) % 2 else "sea_lantern") if _ster(x, y, z) else "blue_concrete")
    # the dome is a guh head: ears, face, rosy cheeks
    k.oren(KX, KY + 5, KZ, gap=3, blok="blue_concrete", binnen="pink_concrete")
    k.face(KX, KY + 3, KZ + 5, "south", 0)
    k.set(KX - 2, KY + 2, KZ + 5, "pink_concrete")
    k.set(KX + 2, KY + 2, KZ + 5, "pink_concrete")
    k.set(KX, KY + KR, KZ, "gold_block")
    k.set(KX, KY + KR + 1, KZ, "guhs:sterrenlantaarn")
    k.lichten.append(k.abs(KX, KY + KR + 1, KZ))
    # telescopes on the roof corners
    for (x, z, f) in ((x0 + 1, z0 + 1, "north"), (x1 - 1, z0 + 1, "east"), (x0 + 1, z1 - 1, "west"), (x1 - 1, z1 - 1, "south")):
        k.set(x, 6, z, "guhs:guh_telescoop", {"facing": f})
    # entrance: door, portico with columns, sign, lanterns
    k.door(KX, 0, z1, "south")
    k.face(KX, 3, z1, "south", 1)
    for x in (KX - 2, KX + 2):
        k.fill(x, 0, z1 + 1, x, 2, z1 + 1, "guhs:knuffelsteen_muur", {"up": "true", "north": "none", "south": "none",
                                                                     "east": "none", "west": "none", "waterlogged": "false"})
    for x in range(KX - 2, KX + 3):
        k.set(x, 3, z1 + 1, "light_blue_terracotta")
    k.stairs(KX - 3, 3, z1 + 1, "guhs:knuffelsteen_trap", "east")
    k.stairs(KX + 3, 3, z1 + 1, "guhs:knuffelsteen_trap", "west")
    k.slab(KX - 1, 4, z1 + 1, "guhs:knuffelsteen_plaat")
    k.slab(KX + 1, 4, z1 + 1, "guhs:knuffelsteen_plaat")
    k.set(KX, 4, z1 + 1, "guhs:sterrenlantaarn")
    k.lichten.append(k.abs(KX, 4, z1 + 1))
    k.sign(KX - 1, 3, z1 + 2, "south", ["HET", "GUHSTERRENHUIS", "kijk omhoog", "en zeg VAHOEG!"], wall=True, colour="yellow")
    k.sign(KX + 1, 3, z1 + 2, "south", ["Sterrenkoepel", "van Franeguh", "Mika's mogen", "alleen kijken"], wall=True, colour="yellow")
    k.lampion(KX - 1, 2, z1 + 1, 0, hanging=True)
    k.lampion(KX + 1, 2, z1 + 1, 1, hanging=True)
    # inside: the hanging guh-planetarium
    k.fill(KX, KY + 1, KZ, KX, KY + KR - 1, KZ, "chain", {"axis": "y", "waterlogged": "false"})
    k.set(KX, KY, KZ, "shroomlight")                            # the sun
    k.lichten.append(k.abs(KX, KY, KZ))
    planets = ((KX - 3, KZ, "light_blue_concrete"), (KX + 3, KZ, "red_concrete"), (KX, KZ - 3, "orange_concrete"),
               (KX - 2, KZ + 3, "lime_concrete"), (KX + 2, KZ + 3, "pink_concrete"))
    for (x, z, c) in planets:
        top = max(y for y in range(KY, KY + KR + 1) if not k.air(x, y, z))
        low = top - 1
        while k.air(x, low - 1, z) and low > KY - 1:
            low -= 1
            if top - low >= 3:
                break
        for y in range(low, top):
            k.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
        k.set(x, low - 1, z, c)
    for (x, z, f) in ((KX, KZ + 4, "north"), (KX, KZ - 4, "south"), (KX - 4, KZ, "east"), (KX + 4, KZ, "west"),
                      (KX - 1, KZ + 4, "north"), (KX + 1, KZ + 4, "north")):
        k.set(x, 0, z, "guhs:guh_bank", {"facing": f})
    k.set(KX - 4, 0, KZ + 4, "guhs:guh_telescoop", {"facing": "north"})
    k.set(KX + 4, 0, KZ - 4, "guhs:guh_telescoop", {"facing": "west"})
    k.set(KX - 5, 0, KZ - 5, "bookshelf")
    k.set(KX - 4, 0, KZ - 5, "bookshelf")
    k.set(KX + 5, 0, KZ + 5, "guhs:guh_bloempot", {"facing": "north", "gewaterd": "true"})


def _pandje(k, x0, x1, z0, z1, h, muur, rand, gevel, face_st):
    """A narrow canal house (grachtenpand) with its gable to the south, a pluisdak roof, a guh face and ears."""
    k.fill(x0, -1, z0, x1, -1, z1, "spruce_planks")
    for y in range(0, h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    k.set(x, y, z, rand if (x in (x0, x1) and z in (z0, z1)) or y == h - 1 else muur)
    w = x1 - x0 + 1
    mid = (x0 + x1) / 2
    # windows (white frames) on every floor
    for y in range(1, h - 1, 3):
        for x in range(x0 + 1, x1):
            k.set(x, y, z1, "glass")
            k.set(x, y + 1, z1, "glass")
            k.set(x, y, z0, "glass")
    for y in range(3, h - 1, 3):
        k.set(x0, y, (z0 + z1) // 2, "glass")
        k.set(x1, y, (z0 + z1) // 2, "glass")
    k.clear(x0 + 1, 1, z1, x1 - 1, 2, z1)
    for x in range(x0 + 1, x1):
        k.set(x, 1, z1, muur)
        k.set(x, 2, z1, "glass")
    dx = x0 + 1
    k.door(dx, 0, z1, "south", "left")
    # roof: layers of pluisdak, ridge along z
    layers = (w + 1) // 2
    for i in range(layers):
        y = h + i
        for z in range(z0 + 1, z1):
            k.set(x0 + i, y, z, "guhs:pluisdak")
            k.set(x1 - i, y, z, "guhs:pluisdak")
    # the gables
    for z in (z0, z1):
        if gevel == "klok":
            prof = [(0, x0, x1), (1, x0, x1), (2, x0 + 1, x1 - 1), (3, x0 + 1, x1 - 1)]
        else:  # trap
            prof = [(0, x0, x1), (1, x0 + 1, x1 - 1), (2, x0 + 1, x1 - 1), (3, x0 + 1, x1 - 1)]
            if w >= 4:
                prof = [(0, x0, x1), (1, x0, x1), (2, x0 + 1, x1 - 1), (3, x0 + 1, x1 - 1), (4, int(mid), int(mid) + 1)]
        top = 0
        for (dy, a, b) in prof:
            for x in range(a, b + 1):
                k.set(x, h + dy, z, rand if gevel == "klok" and (x in (a, b)) else muur)
            top = h + dy
        if gevel == "klok":
            k.slab(x0, h + 2, z, "guhs:knuffelsteen_plaat")
            k.slab(x1, h + 2, z, "guhs:knuffelsteen_plaat")
        fx = int(mid) if w % 2 else int(mid)
        k.face(fx, h + 1, z, "south" if z == z1 else "north", face_st)
        # ears on top
        ex = int(mid)
        if w % 2:
            ears = ((ex - 1, top + 1), (ex - 1, top + 2), (ex + 1, top + 1), (ex + 1, top + 2))
            k.set(ex, top + 1, z, rand)
        else:
            ears = ((ex, top + 1), (ex, top + 2), (ex - 1, top + 2), (ex + 1, top + 1), (ex + 1, top + 2), (ex + 2, top + 2))
        for (x, y) in ears:
            k.set(x, y, z, "pink_wool")


def _pandjes(k):
    _pandje(k, 21, 23, 2, 10, 8, "cyan_terracotta", "white_terracotta", "klok", 1)
    _pandje(k, 24, 27, 1, 10, 7, "bricks", "white_terracotta", "trap", 3)
    k.set(23, 0, 11, "guhs:guh_bloempot", {"facing": "south", "gewaterd": "true"})
    k.set(27, 0, 11, "guhs:guh_bloempot", {"facing": "south", "gewaterd": "true"})


def _chocovet(k):
    """The warme-chocovet kraam (boost) with a giant steaming mug with a guh face on its roof."""
    x0, x1, z0, z1 = 21, 26, 13, 17
    for y in range(0, 4):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    k.set(x, y, z, "brown_concrete" if (x + z) % 2 else "white_terracotta")
    k.fill(x0, 4, z0, x1, 4, z1, "brown_terracotta")
    k.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "spruce_planks")
    k.clear(x0 + 1, 1, z1, x1 - 1, 2, z1)
    for x in range(x0 + 1, x1):
        k.set(x, 0, z1, "guhs:vadshout_planken")
    for x in (x0 + 1, x0 + 3):
        k.set(x, 1, z1, "guhs:theepotje", {"facing": "south"})
        k.boost.append(k.abs(x, 1, z1))
    k.set(x0 + 2, 1, z1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
    k.set(x0 + 4, 1, z1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
    for x in range(x0, x1 + 1):
        k.set(x, 3, z1 + 1, "brown_wool" if x % 2 else "white_wool")
    k.sign(x0 + 2, 4, z1 + 1, "south", ["WARME", "CHOCOVET", "met een toef", "slagroom"], wall=True, colour="yellow")
    k.sign(x0 + 3, 4, z1 + 1, "south", ["Een bekertje en", "je schaatst weer", "lekker VAHOEG!"], wall=True, colour="yellow")
    k.lantern(x0 + 2, 3, z0 + 2, hanging=True)
    k.door(x0, 0, z0 + 2, "west")
    k.guh(x0 + 3, 0, z0 + 2, 0, scale=0.9, tags=["guhs_elftocht_kraamguh"], stil=True)
    # the mug
    cx, cz = 23.5, 15
    for y in range(5, 9):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
                if d <= 2.0:
                    k.set(x, y, z, "brown_concrete" if y == 8 and d < 1.3 else "white_concrete")
    for (x, y) in ((26, 6), (26, 7)):
        k.set(x, y, 15, "white_concrete")
    k.face(23, 6, 17, "south", 1)
    k.set(24, 6, 17, "pink_concrete")
    k.set(23, 9, 15, "white_wool")                               # slagroom / marshmallow
    k.set(24, 9, 15, "pink_wool")
    k.set(24, 10, 15, "white_wool")
    k.set(23, 9, 14, "white_wool")


def _guhturnus(k):
    """The ringed planet Guhturnus on a post: a pink planet with a face and ears and a golden ring."""
    cx, cy, cz = 3, 6, 9
    k.fill(cx, 0, cz, cx, 3, cz, "guhs:vadshout_stam", {"axis": "y"})
    for x in range(0, 7):
        for y in range(3, 10):
            for z in range(4, 15):
                d = ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5
                if d <= 2.3:
                    k.set(x, y, z, "pink_concrete")
                dr = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
                if y == cy and 2.3 < dr <= 3.5:
                    k.set(x, y, z, "yellow_concrete" if (x + z) % 2 else "orange_concrete")
    k.face(cx, cy + 1, cz + 2, "south", 0)
    k.oren(cx, cy + 2, cz, gap=1, blok="pink_wool", binnen="magenta_wool")
    k.sign(cx + 1, 0, cz + 3, "south", ["Planeet", "GUHTURNUS", "de vadsigste", "planeet van allemaal"], wall=False)


def _sneeuwheuvel(k):
    """A natural sneeuwguh-heuveltje: a snow mound with a guh face and snowy ears, a telescope on top."""
    cx, cz = 3, 2
    for x in range(0, 7):
        for z in range(0, 6):
            for y in range(0, 4):
                if ((x - cx) ** 2 + (z - cz) ** 2 + (y * 1.4) ** 2) ** 0.5 <= 3.3:
                    k.set(x, y, z, "snow_block")
    k.face(cx, 1, cz + 3, "south", 3)
    for (x, y) in ((cx - 2, 2), (cx - 2, 3), (cx - 3, 3), (cx + 2, 2), (cx + 2, 3), (cx + 3, 3)):
        k.set(x, y, cz, "snow_block")
    k.set(cx, 3, cz, "snow_block")
    k.set(cx, 4, cz, "guhs:guh_telescoop", {"facing": "north"})


def _kade(k):
    k.fill(0, -1, 18, 27, -1, 23, "guhs:knuffelklinkers")
    k.fill(KX - 1, -1, 14, KX + 1, -1, 17, "guhs:knuffelklinkers")
    k.fill(20, -1, 11, 27, -1, 12, "guhs:knuffelklinkers")
    k.fill(20, -1, 13, 20, -1, 17, "guhs:knuffelklinkers")
    k.stempelpost(9, 22, dak="guhs:pluisdak_plaat", naam=["Stempelpost", "FRANEGUH", "stempel 10 van 11", "plof!"],
                  soort="sterren")
    # lampion pergola: a fence beam on posts with lampions hanging under it every other block
    for x in (0, 6, 15, 20, 27):
        k.fill(x, 0, 19, x, 3, 19, "spruce_fence")
    for x in range(0, 28):
        k.set(x, 4, 19, "spruce_fence")
    i = 0
    for x in range(1, 27, 2):
        if k.air(x, 3, 19):
            k.lampion(x, 3, 19, i, hanging=True)
            i += 1
    for x in (1, 14, 17, 26):
        k.set(x, 0, 23, "guhs:vadshout_stam", {"axis": "y"})
        k.set(x, 1, 23, "guhs:sterrenlantaarn")
        k.lichten.append(k.abs(x, 1, 23))
    k.vuurkorf(3, 17)
    k.vuurkorf(18, 17)
    k.vlaggenmast(KX - 3, 17, 5, "nl")
    k.vlaggenmast(KX + 3, 17, 5, "sterren")
    for x in (4, 5):
        k.set(x, 0, 18, "guhs:guh_bank", {"facing": "south"})
    for (x, z) in ((3, 21), (5, 22), (13, 21), (15, 22), (19, 21), (23, 22)):
        k.publiek_guh(x, z, 0)
    k.set(21, 0, 22, "guhs:guh_telescoop", {"facing": "south"})
    # a few star-gazing guhs on the flat roof? no: they stay on the quay. Snow, flowers and lanterns in the garden:
    for (x, z) in ((0, 2), (5, 1), (0, 16), (6, 16), (20, 2), (19, 15), (2, 13)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:guh_ijsbloempje")
    for (x, z) in ((1, 4), (6, 3), (19, 9), (0, 12), (20, 7)):
        if k.air(x, 0, z):
            k.set(x, 0, z, "guhs:rijpsprietjes")
    k.sneeuwpop(6, 13, "south")
    for (x, z) in ((5, 15),):
        k.fill(x, 0, z, x, 2, z, "guhs:knotwilg_stam", {"axis": "y"})
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if abs(dx) + abs(dz) <= 1:
                    k.set(x + dx, 3, z + dz, "guhs:knotwilg_bladeren", {"persistent": "true", "distance": "1"})
        k.set(x, 4, z, "guhs:knotwilg_bladeren", {"persistent": "true", "distance": "1"})


def bouw(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    _kade(k)
    _koepelhuis(k)
    _pandjes(k)
    _chocovet(k)
    _guhturnus(k)
    _sneeuwheuvel(k)
    k.sneeuw(0, 27, 0, 23, DAK, laag=2, ymin=1)
    k.hekken()
    x, y, z = k.abs(9, 0, 22)
    api.stempelguh(s, x, y, z, 0, INDEX)
    return {"stempelguh": (x, y, z, 0), "publiek": k.publiek, "boost": k.boost, "lichten": k.lichten}


def check(s, ox, oy, oz):
    k = Kit(s, ox, oy, oz)
    x, y, z = 9, 0, 22
    if k.get(x, y - 1, z) in (None, "minecraft:air") or not k.air(x, y, z) or not k.air(x, y + 1, z):
        raise SystemExit(f"{NAAM}: the Stempelguh has no floor or no room")
    for dz in range(z + 1, api.PLOT_Z):
        if not k.air(x, 0, dz) or not k.air(x, 1, dz):
            raise SystemExit(f"{NAAM}: the way from the ice to the Stempelguh is blocked")
    for (x, z) in ((KX, KZ + 6), (22, 10), (25, 10)):
        lo, hi = k.get(x, 0, z), k.get(x, 1, z)
        if lo != hi or not lo or not lo.endswith("deur"):
            raise SystemExit(f"{NAAM}: door at {(x, z)} is not whole")
        if not k.air(x, 0, z + 1) or not k.air(x, 1, z + 1):
            raise SystemExit(f"{NAAM}: door at {(x, z)} is blocked")
    # the dome's inside stays open under the stars (headroom for the planetarium)
    for y in range(0, 3):
        if not k.air(KX, y, KZ + 2):
            raise SystemExit(f"{NAAM}: the planetarium floor is blocked")
