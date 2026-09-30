"""Elf-Guhjestocht village 7: VADSKUM — "Klunplek & ijshockey".

The ice-club village. From the canal a klunpad of red klunmatten climbs over a little dijkje on the wooden klunbrugje
(railings, lanterns, hay bales, a sign that tells you to keep your skates on) and runs to the ijsbaan of IJsclub
"De Vadse Schaats": a real hockey rink of polderijs with boarding, two red goals with nets, a big guh face painted on
the ice at centre ice, blue lines, a puck, three ice-hockey guhtjes and a scoreboard (Guhtjes 3 - Mika's 2) with guh
ears. West stands the wooden club house (two floors, green shutters, a snow-blanket roof, guh-face gables, a balcony
over the rink and a kantine hatch with warme chocovet). Along the ice: the Stempelguh in a little stamp booth on a
steiger, a skate grinder's shed, spectator benches, vuurkorven, lanterns, snowman-guhs and knotwilgen.
"""
# =====================================================================================================================
# Helpers (identical copy in villages 06 Guhdeloopen, 07 Vadskum and 08 Knabbelsward, so every module stands alone).
# All coordinates in the Dorp methods are LOCAL: x 0..27 west->east, z 0..23 north->south (z 23 = the canal edge),
# y 0 = the first layer on top of the polder ground (y -1 = the ground itself).
# =====================================================================================================================
import json
import math
import random

try:  # as tools/features/elftocht_dorp_xx.py (package) or standalone next to the api
    from . import elftocht_dorp_api as api
except ImportError:
    try:
        from features import elftocht_dorp_api as api
    except ImportError:
        import elftocht_dorp_api as api
from make_structures import Byte, NbtList, compounds

PX, PY, PZ = api.PLOT_X, api.PLOT_Y, api.PLOT_Z
FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
ROT = {"south": 0, "west": 4, "north": 8, "east": 12}          # standing banner / sign rotation per facing

KS, KS_TRAP, KS_PLAAT, KS_MUUR, KS_FACE = ("guhs:knuffelsteen", "guhs:knuffelsteen_trap", "guhs:knuffelsteen_plaat",
                                          "guhs:knuffelsteen_muur", "guhs:knuffelsteen_gezicht")
DAK, DAK_TRAP, DAK_PLAAT = "guhs:pluisdak", "guhs:pluisdak_trap", "guhs:pluisdak_plaat"
VH, VH_TRAP, VH_PLAAT, VH_HEK, VH_STAM, VH_FACE = ("guhs:vadshout_planken", "guhs:vadshout_trap", "guhs:vadshout_plaat",
                                                  "guhs:vadshout_hek", "guhs:vadshout_stam", "guhs:vadshout_gezicht")
VH_DEUR, VH_LUIK = "guhs:vadshout_deur", "guhs:vadshout_luik"
KLINK = "guhs:knuffelklinkers"
RIJPGRAS, SPRIET, IJSBLOEM, POLDERIJS = "guhs:rijpgras", "guhs:rijpsprietjes", "guhs:guh_ijsbloempje", "guhs:polderijs"
WILG_STAM, WILG_BLAD, IJSPEGEL, MOLENTJE = ("guhs:knotwilg_stam", "guhs:knotwilg_bladeren", "guhs:ijspegelguh_kristal",
                                            "guhs:guh_molentje")

FACE_BLOCKS = {"skin": "minecraft:pink_wool", "eye": "minecraft:black_concrete", "shine": "minecraft:white_concrete",
               "ring": "minecraft:light_blue_concrete", "nose": "minecraft:magenta_concrete",
               "mouth": "minecraft:purple_concrete", "cheek": "minecraft:pink_concrete"}

# hand-drawn guh faces (seen from the front; o skin, # eye, * eye shine, n snoet, c blush, m mouth)
GEZICHT_5 = [".ooo.",
             "o#o#o",
             "conoc",
             ".omo."]
GEZICHT_7 = [".ooooo.",
             "o*#o*#o",
             "o##o##o",
             "oconoco",
             ".omomo.",
             "..omo.."]
TEKEN = {"o": "skin", "#": "eye", "*": "shine", "n": "nose", "c": "cheek", "m": "mouth"}

# Dutch / guh flags as banner patterns (1.21 block entity format)
VLAGGEN = {"nl": ("white", [("stripe_top", "red"), ("stripe_bottom", "blue")]),
           "oranje": ("orange", [("gradient_up", "yellow"), ("circle", "white")]),
           "guh": ("pink", [("circle", "white"), ("curly_border", "magenta")]),
           "vads": ("white", [("stripe_top", "red"), ("stripe_bottom", "blue"), ("circle", "orange")]),
           "ijs": ("light_blue", [("rhombus", "white"), ("border", "blue")]),
           "kerk": ("white", [("cross", "yellow"), ("border", "red")]),
           "snert": ("lime", [("rhombus", "white"), ("circle", "green")])}


def mc(n):
    return n if ":" in n else "minecraft:" + n


def face_role(u, v, R):
    """Where on a guh face of radius R (u right, v up from its middle): skin, eye, shine, ring, nose, mouth, cheek."""
    role = None
    if (u / (R + 0.4)) ** 2 + (v / (0.9 * R + 0.4)) ** 2 <= 1:
        role = "skin"
        for sx in (-1, 1):
            ex, ey = sx * 0.42 * R, 0.1 * R
            d = math.dist((u, v), (ex, ey))
            er = 0.27 * R + 0.35
            if d <= er:
                role = "ring" if (v < ey - 0.25 * er and d > 0.45 * er) else "eye"
                if round(u) == round(ex - 0.3 * er) and round(v) == round(ey + 0.35 * er):
                    role = "shine"
            if math.dist((u, v), (sx * 0.7 * R, -0.35 * R)) <= 0.13 * R + 0.25:
                role = "cheek"
        if math.dist((u, v), (0, -0.28 * R)) <= 0.08 * R + 0.3:
            role = "nose"
        y0 = round(-0.46 * R)
        if (round(u), round(v)) in {(-1, y0), (0, y0 - 1), (1, y0)} or (R >= 5 and (round(u), round(v)) in {(-2, y0 + 1), (2, y0 + 1)}):
            role = "mouth"
    return role


class Dorp:
    def __init__(self, s, ox, oy, oz, index, slug, seed):
        self.s, self.ox, self.oy, self.oz = s, ox, oy, oz
        self.index, self.slug = index, slug
        self.rng = random.Random(seed)
        self.lichten, self.boost, self.publiek, self.stempel = [], [], [], None
        self.teksten = {}

    # --- basics -------------------------------------------------------------------------------------------------
    def abs(self, x, y, z):
        return (self.ox + x, self.oy + y, self.oz + z)

    def set(self, x, y, z, name, props=None, nbt=None):
        ax, ay, az = self.abs(x, y, z)
        if not api.inside(self.ox, self.oy, self.oz, ax, ay, az):
            raise SystemExit(f"dorp {self.slug}: {name} outside the plot at local {(x, y, z)}")
        self.s.set(ax, ay, az, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(*self.abs(x, y, z))

    def props(self, x, y, z):
        b = self.s.blocks.get(self.abs(x, y, z))
        return b[1] if b else {}

    def leeg(self, x, y, z):
        g = self.get(x, y, z)
        return g is None or g == "minecraft:air"

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if not only_empty or self.leeg(x, y, z):
                        self.set(x, y, z, name, props)

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "minecraft:air")

    def trap(self, x, y, z, name, facing, half="bottom"):
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def trap_omhoog(self, x, zs, name, vol, vloer_y=3):
        """An indoor staircase going up northwards from (x, 0, zs) to an upstairs floor whose slabs are at y vloer_y
        (walking height vloer_y + 1), with enough head room cut out of that floor and solid blocks underneath."""
        for i in range(vloer_y + 1):
            for yy in range(0, i):
                self.set(x, yy, zs - i, vol)
            self.trap(x, i, zs - i, name, "north")
        for i in range(vloer_y):
            self.set(x, vloer_y, zs - i, "minecraft:air")

    def plaat(self, x, y, z, name, soort="bottom"):
        self.set(x, y, z, name, {"type": soort, "waterlogged": "false"})

    # --- light, fire, flags, signs -------------------------------------------------------------------------------
    def licht(self, x, y, z):
        self.lichten.append(self.abs(x, y, z))

    def lantaarn(self, x, y, z, hanging=False, soul=False):
        self.set(x, y, z, "soul_lantern" if soul else "lantern", {"hanging": str(hanging).lower(), "waterlogged": "false"})
        self.licht(x, y, z)

    def lampion(self, x, y, z, kleur="roze", hanging=True):
        self.set(x, y, z, f"guhs:lampion_{kleur}", {"hanging": str(hanging).lower()})
        self.licht(x, y, z)

    def paal(self, x, z, h=3, kleur="geel", y0=0, hek=VH_HEK, top="lampion"):
        """A little post (fence) with a lampion / lantern on top."""
        for y in range(y0, y0 + h):
            self.set(x, y, z, hek)
        if top == "lampion":
            self.lampion(x, y0 + h, z, kleur, hanging=False)
        elif top == "lantaarn":
            self.lantaarn(x, y0 + h, z)

    def vuurkorf(self, x, z, y=0):
        """A vuurkorf: a little iron basket leg with a crackling fire on top."""
        self.set(x, y, z, "iron_bars", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        self.set(x, y + 1, z, "campfire", {"lit": "true", "signal_fire": "false", "facing": "south", "waterlogged": "false"})
        self.licht(x, y + 1, z)

    def vlag(self, x, y, z, soort="nl", facing=None, rotation=None):
        """Banner with a Dutch / guh flag. facing -> wall banner hanging on the block behind it."""
        kleur, lagen = VLAGGEN[soort]
        nbt = {"id": "minecraft:banner",
               "patterns": compounds([{"pattern": "minecraft:" + p, "color": c} for p, c in lagen])}
        if facing:
            self.set(x, y, z, f"{kleur}_wall_banner", {"facing": facing}, nbt)
        else:
            self.set(x, y, z, f"{kleur}_banner", {"rotation": str(ROT["south"] if rotation is None else rotation)}, nbt)

    def vlaggenmast(self, x, z, h=5, soort="nl", y0=0, hek=VH_HEK):
        for y in range(y0, y0 + h):
            self.set(x, y, z, hek)
        self.vlag(x, y0 + h, z, soort)

    def bord(self, x, y, z, facing, regels, wall=True, hout="spruce", kleur="black", glow=False):
        """A sign; each line is a lang key sign.guhs.elftocht.dorp<index>.<n> with the Dutch text as fallback (the texts
        are also exported in TEKSTEN so the core can register them in nl_nl + en_us)."""
        msgs = []
        for tekst in list(regels) + [""] * (4 - len(regels)):
            if tekst:
                key = f"sign.guhs.elftocht.dorp{self.index}.{len(self.teksten) + 1}"
                for k, v in self.teksten.items():
                    if v == tekst:
                        key = k
                self.teksten[key] = tekst
                msgs.append(json.dumps({"translate": key, "fallback": tekst}))
            else:
                msgs.append(json.dumps(""))
        blank = json.dumps("")
        nbt = {"id": "minecraft:sign", "is_waxed": Byte(1),
               "front_text": {"messages": NbtList(8, msgs), "color": kleur, "has_glowing_text": Byte(1 if glow else 0)},
               "back_text": {"messages": NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": Byte(0)}}
        if wall:
            self.set(x, y, z, f"{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, f"{hout}_sign", {"rotation": str(ROT[facing]), "waterlogged": "false"}, nbt)

    # --- guh faces and guh ears ------------------------------------------------------------------------------------
    def gezicht(self, cx, cy, cz, facing, R, blocks=None, skin=True):
        """A big guh face painted on the plane that looks `facing`, centred on (cx, cy, cz) (cy = the eye row).
        Small faces (R < 3) use hand-drawn pixel templates (5 or 7 wide), bigger ones the round formula."""
        blocks = dict(FACE_BLOCKS, **(blocks or {}))
        dx, dz = FACING[facing]
        rx, rz = -dz, dx
        if R < 3:
            tpl, mid = (GEZICHT_5, 1) if R < 2.3 else (GEZICHT_7, 2)
            half = len(tpl[0]) // 2
            for row, line in enumerate(tpl):
                for col, ch in enumerate(line):
                    role = TEKEN.get(ch)
                    if role is None or (role == "skin" and not skin):
                        continue
                    du, dv = half - col, mid - row          # du grows to the face's right = the viewer's left
                    self.set(cx + rx * du, cy + dv, cz + rz * du, blocks[role])
            return
        n = int(R + 1)
        for du in range(-n, n + 1):
            for dv in range(-n, n + 1):
                role = face_role(du, dv, R)
                if role is None or (role == "skin" and not skin):
                    continue
                self.set(cx + rx * du, cy + dv, cz + rz * du, blocks[role])

    def kopje(self, x, y, z, facing="south", stemming=None, hout=False):
        """A small guh face block (knuffelsteen or vadshout)."""
        st = self.rng.randrange(4) if stemming is None else stemming
        self.set(x, y, z, VH_FACE if hout else KS_FACE, {"facing": facing, "stemming": str(st)})

    def oren(self, cx, y, cz, langs="x", gap=1, buiten="pink_wool", binnen="pink_terracotta"):
        """Two guh ears standing on (cx, y-1, cz): each ear 2 wide at the bottom, 1 on top, leaning outward.
        `langs` is the axis the two ears stand next to each other on."""
        for side in (-1, 1):
            for k, (du, dy, mat) in enumerate(((gap + 2, 0, buiten), (gap + 1, 0, binnen), (gap + 2, 1, buiten))):
                u = side * du
                if langs == "x":
                    self.set(cx + u, y + dy, cz, mat)
                else:
                    self.set(cx, y + dy, cz + u, mat)

    # --- houses -----------------------------------------------------------------------------------------------------
    def deur(self, x, y, z, facing, hout="spruce", hinge="left"):
        name = hout if ":" in hout else f"{hout}_door"
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, name, {"facing": facing, "half": half, "hinge": hinge, "open": "false", "powered": "false"})

    def luiken(self, x, y, z, facing, luik="spruce_trapdoor", hoog=1):
        """Shutters left and right of a window at (x, y, z) in a wall that looks `facing`."""
        dx, dz = FACING[facing]
        rx, rz = -dz, dx
        for side in (-1, 1):
            for dy in range(hoog):
                self.set(x + rx * side + dx, y + dy, z + rz * side + dz, luik,
                         {"facing": facing, "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})

    def zadeldak(self, x0, x1, z0, z1, y0, langs="x", trap=DAK_TRAP, vol=DAK, overstek=1, gevel=None, sneeuw=False):
        """A gable roof over the box x0..x1 / z0..z1 (walls), starting at height y0; `langs` = ridge axis.
        gevel = material for the gable triangles and the eaves course (None = leave them to the caller).
        sneeuw=True: a thick snow blanket (eaves of `vol`, then snow blocks with snow layers) instead of stairs.
        Returns the ridge height."""
        if langs == "x":
            a0, a1, b0, b1 = z0, z1, x0, x1
        else:
            a0, a1, b0, b1 = x0, x1, z0, z1

        def pos(a, b, y):
            return (b, y, a) if langs == "x" else (a, y, b)

        i, top = 0, y0
        while True:
            lo, hi = a0 - overstek + i, a1 + overstek - i
            if lo > hi:
                break
            y = top = y0 + i
            for b in range(b0 - overstek, b1 + overstek + 1):
                if lo == hi:                                   # the ridge (odd width)
                    p = pos(lo, b, y)
                    if sneeuw:
                        self.set(*p, "minecraft:snow_block")
                        self._sneeuwlaag(p, 2)
                    else:
                        self.set(*p, vol)
                        self.plaat(p[0], p[1] + 1, p[2], DAK_PLAAT if vol == DAK else vol.replace("_planks", "_slab")
                                   .replace("_tiles", "_tile_slab"), "bottom")
                    continue
                for a, side in ((lo, -1), (hi, 1)):
                    p = pos(a, b, y)
                    if sneeuw:
                        self.set(*p, vol if i == 0 else "minecraft:snow_block")
                        self._sneeuwlaag(p, 3 if i == 0 else 2)
                    else:
                        # stairs climb towards the ridge
                        if langs == "x":
                            fac = "south" if side < 0 else "north"
                        else:
                            fac = "east" if side < 0 else "west"
                        self.trap(p[0], p[1], p[2], trap, fac)
            if gevel and i == 0 and overstek >= 1:             # eaves course on the long walls
                for b in range(b0, b1 + 1):
                    for a in (a0, a1):
                        self.set(*pos(a, b, y), gevel)
            if gevel:                                          # gable triangles
                for b in (b0, b1):
                    for a in range(max(lo + 1, a0), min(hi - 1, a1) + 1):
                        self.set(*pos(a, b, y), gevel)
            i += 1
            if hi - lo < 1:
                break
        return top

    def _sneeuwlaag(self, p, layers):
        x, y, z = p
        if y + 1 < PY:
            self.set(x, y + 1, z, "snow", {"layers": str(layers)})

    # --- nature and winter -----------------------------------------------------------------------------------------
    def knotwilg(self, x, z, h=3, y0=0):
        for y in range(y0, y0 + h):
            self.set(x, y, z, WILG_STAM)
        top = y0 + h
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                for dy in range(0, 2):
                    if dy == 1 and abs(dx) + abs(dz) == 2:
                        continue
                    self.set(x + dx, top + dy, z + dz, WILG_BLAD, {"persistent": "true"})
        self.set(x, top, z, WILG_STAM)
        self.set(x, top + 2, z, "snow", {"layers": "2"})
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            self.set(x + dx, top + 2, z + dz, "snow", {"layers": "1"})

    def sneeuwpopguh(self, x, z, facing="south", y=0):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, "guhs:sneeuwpopguh", {"facing": facing, "half": half})

    def maaiveld(self, x, z):
        """The feet height on the natural ground (rijpgras) of column x/z, or None when something is built there."""
        for y in range(4, -2, -1):
            g = self.get(x, y, z)
            if g is None and y == -1:
                g = mc(RIJPGRAS)
            if g is None or g == "minecraft:air":
                continue
            return y + 1 if g == mc(RIJPGRAS) and self.leeg(x, y + 1, z) and self.leeg(x, y + 2, z) else None
        return None

    def grondversiering(self, kans=0.22, x0=0, z0=0, x1=PX - 1, z1=PZ - 1, vrij=()):
        """Frost sprouts, ice flowers and snow drifts on the free rijpgras; `vrij` = cells to keep clear."""
        vrij = set(vrij)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                y = self.maaiveld(x, z)
                if (x, z) in vrij or y is None:
                    continue
                r = self.rng.random()
                if r < kans * 0.45:
                    self.set(x, y, z, SPRIET)
                elif r < kans * 0.65:
                    self.set(x, y, z, IJSBLOEM)
                elif r < kans:
                    self.set(x, y, z, "snow", {"layers": "1"})

    def sneeuw_op_daken(self, min_y=3):
        """Snow on the flat tops of full blocks (roof ridges, chimneys, walls) higher than min_y."""
        for x in range(PX):
            for z in range(PZ):
                for y in range(PY - 2, min_y - 1, -1):
                    g = self.get(x, y, z)
                    if g is None or g == "minecraft:air":
                        continue
                    if _vol_blok(g, self.props(x, y, z)) and self.leeg(x, y + 1, z):
                        self.set(x, y + 1, z, "snow", {"layers": "2"})
                    break

    # --- guhs -------------------------------------------------------------------------------------------------------
    def _tags_als_nbt_lijst(self):
        """make_structures only writes typed lists: turn the helper's plain Tags list into an NBT string list."""
        nbt = self.s.entities[-1][3]
        if isinstance(nbt.get("Tags"), list) and not isinstance(nbt["Tags"], NbtList):
            nbt["Tags"] = NbtList(8, list(nbt["Tags"]))

    def stempelguh(self, x, y, z, yaw=0):
        self.stempel = self.abs(x, y, z) + (yaw,)
        ax, ay, az = self.abs(x, y, z)
        api.stempelguh(self.s, ax, ay, az, yaw, self.index)
        self._tags_als_nbt_lijst()

    def publiek_guh(self, x, y, z, yaw=0):
        ax, ay, az = self.abs(x, y, z)
        self.publiek.append((ax, ay, az, yaw))
        api.publiek(self.s, ax, ay, az, yaw)
        self._tags_als_nbt_lijst()

    def resultaat(self):
        return {"stempelguh": self.stempel, "publiek": list(self.publiek), "boost": [self.abs(*b) for b in self.boost],
                "lichten": list(self.lichten)}


# ---------------------------------------------------------------------------------------------------------------------
# geometry self-check (shared)
# ---------------------------------------------------------------------------------------------------------------------
PASS_KEYS = ("_carpet", "_sign", "_banner", "_door", "_deur", "torch", "_button", "pressure_plate", "rail", "lampion", "vlaggetjes",
             "feestslingers", "seizoensslinger", "rijpsprietjes", "ijsbloempje", "guhoortjes", "short_grass", "fern",
             "_tulip", "poppy", "dandelion", "cornflower", "lily_of_the_valley", "oxeye", "allium", "azure_bluet",
             "orchid", "sapling", "cobweb", "vine", "lever", "tripwire", "bladerhoopje", "pluisgras", "roze_guhbloem",
             "knabbelroos")
NO_STAND = ("fence", "_wall", "_muur", "_hek", "_poort", "iron_bars", "_pane", "chain", "lantern", "campfire",
            "lightning_rod", "end_rod", "candle", "flower_pot", "potted", "bell", "lampion")
THIN_TOP = ("_stairs", "_trap", "_slab", "_plaat", "fence", "_wall", "_muur", "_hek", "_poort", "iron_bars", "_pane", "chain",
            "lantern", "campfire", "rod", "candle", "pot", "bell", "banner", "sign", "door", "trapdoor", "luik", "carpet",
            "snow", "glass", "leaves", "bladeren", "lampion", "vlaggetjes", "slinger", "torch", "sprietjes", "bloem",
            "cauldron", "hopper", "anvil", "grindstone", "scaffolding", "ladder", "button", "plate", "gezicht", "cake",
            "taart", "sneeuwpopguh", "tafel", "stoel", "bank", "molentje", "kristal", "theepotje", "bed", "barrel",
            "composter", "lectern", "head", "skull", "air", "polderijs", "ice", "hay_block", "stonecutter", "loom",
            "knuffel_", "kussen", "zitzak", "brewing", "enchant", "chest", "iron_bars", "sea_pickle")


def _vol_blok(name, props):
    n = name.split(":")[1]
    return not any(t in n for t in THIN_TOP)


def _passable(entry):
    if entry is None:
        return True
    name, props, _ = entry
    if name == "minecraft:air":
        return True
    if name == "minecraft:snow":
        return int(props.get("layers", "1")) <= 2
    if name.endswith(("_fence_gate", "_poort")):
        return props.get("open") == "true"
    return any(k in name for k in PASS_KEYS)


def _standable(entry):
    if entry is None or _passable(entry):
        return False
    return not any(k in entry[0] for k in NO_STAND)


def controleer(s, ox, oy, oz, index, naam):
    """The geometry checks every village of this builder must pass (raises SystemExit)."""
    def fout(msg):
        raise SystemExit(f"elftocht dorp {index} {naam}: {msg}")

    B = s.blocks

    def at(x, y, z):
        return B.get((x, y, z))

    in_plot = {p: v for p, v in B.items() if ox <= p[0] < ox + PX and oz <= p[2] < oz + PZ and oy - 3 <= p[1] < oy + PY}
    # 1. nothing above the plot's height (blocks outside the box can't be told apart from the core's in a shared
    #    structure, so we check the columns of our plot only)
    for (x, y, z), v in B.items():
        if ox <= x < ox + PX and oz <= z < oz + PZ and y >= oy + PY and v[0] != "minecraft:air":
            fout(f"block above the plot at {(x, y, z)}")
    # 2. nothing floating: every block is connected (faces) to the ground
    solid = {p for p, v in in_plot.items() if v[0] != "minecraft:air"}
    seen = set()
    todo = [p for p in solid if p[1] < oy or (p[1] == oy and (at(p[0], oy - 1, p[2]) is None
                                                               or at(p[0], oy - 1, p[2])[0] != "minecraft:air"))]
    seen.update(todo)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in seen:
                seen.add(n)
                todo.append(n)
    los = sorted(solid - seen)
    if los:
        fout(f"{len(los)} floating blocks, e.g. {[(p, B[p][0]) for p in los[:5]]}")
    # 3. doors / two-high blocks whole
    for (x, y, z), (name, props, _) in in_plot.items():
        if "half" in props and props["half"] in ("lower", "upper"):
            dy = 1 if props["half"] == "lower" else -1
            o = at(x, y + dy, z)
            want = "upper" if dy == 1 else "lower"
            if not o or o[0] != name or o[1].get("half") != want or o[1].get("facing") != props.get("facing") \
                    or o[1].get("hinge") != props.get("hinge"):
                fout(f"{name} at {(x, y, z)} is missing its other half")
    # 4. walkable from the canal: flood fill of feet positions
    def vast(x, y, z):
        """Standable block; an empty ground layer counts as the polder ground the core lays under the plot."""
        b = at(x, y, z)
        return _standable(b) or (b is None and y == oy - 1)

    def ok(x, y, z):
        return _passable(at(x, y, z)) and _passable(at(x, y + 1, z)) and vast(x, y - 1, z)

    def binnen(x, z):
        return ox <= x < ox + PX and oz <= z < oz + PZ

    start = [(x, oy, oz + PZ) for x in range(ox - 1, ox + PX + 1)]
    bereik = set(start)
    todo = list(start)
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not binnen(nx, nz):
                continue
            for ny in (y, y + 1, y - 1, y - 2, y - 3):
                if ny > y and not _passable(at(x, y + 2, z)):
                    continue
                if ny < y and not (_passable(at(nx, y, nz)) and _passable(at(nx, y + 1, nz))):
                    continue
                if ny < oy - 3:
                    continue
                n = (nx, ny, nz)
                if n not in bereik and ok(nx, ny, nz):
                    bereik.add(n)
                    todo.append(n)
                    break
    # 5. the Stempelguh and the audience
    stempel, publiek = None, []
    for ex, ey, ez, nbt in s.entities:
        tags = list(nbt.get("Tags", []))
        p = (int(math.floor(ex)), int(round(ey)), int(math.floor(ez)))
        if f"guhs_elftocht_dorp_{index}" in tags:
            stempel = p
        elif "guhs_elftocht_publiek" in tags and binnen(p[0], p[2]):
            publiek.append(p)
    if not stempel:
        fout("no Stempelguh")
    x, y, z = stempel
    if not binnen(x, z):
        fout(f"Stempelguh outside the plot {stempel}")
    if not (vast(x, y - 1, z) and _passable(at(x, y, z)) and _passable(at(x, y + 1, z))):
        fout(f"Stempelguh not on solid ground / not free at {stempel}")
    if z < oz + PZ - 1 - 3:
        fout(f"Stempelguh more than 3 blocks from the canal edge: {stempel}")
    if stempel not in bereik:
        fout(f"Stempelguh at {stempel} not reachable from the canal")
    if not 2 <= len(publiek) <= 8:
        fout(f"{len(publiek)} audience guhs (want 2..8)")
    for p in publiek:
        x, y, z = p
        if not (vast(x, y - 1, z) and _passable(at(x, y, z)) and _passable(at(x, y + 1, z))):
            fout(f"audience guh not on solid ground / not free at {p}")
    # 6. every door can be walked through (so no house is closed off and no door leads nowhere)
    for (x, y, z), (name, props, _) in in_plot.items():
        if name.endswith(("_door", "_deur")) and props.get("half") == "lower":
            fac = props.get("facing")
            dx, dz = FACING[fac]
            kanten = [p for p in ((x + dx, y, z + dz), (x - dx, y, z - dz)) if p in bereik
                      or (p[0], p[1] + 1, p[2]) in bereik]         # (a step down/up right behind the door is fine)
            if (x, y, z) not in bereik or len(kanten) < 2:
                fout(f"door {name} at {(x, y, z)} can't be walked through from both sides")
    return bereik


INDEX = 7
NAAM = "Vadskum"
SLUG = "vadskum"

HOUT, HOUT_TRAP, HOUT_PLAAT, HOUT_HEK = "spruce_planks", "spruce_stairs", "spruce_slab", "spruce_fence"
STAM = "dark_oak_log"
GROEN_LUIK = "warped_trapdoor"
GLAS = "light_blue_stained_glass"
RINK_X0, RINK_X1, RINK_Z0, RINK_Z1 = 12, 26, 2, 13       # boarding (the ice is inside)
RINK_CX, RINK_CZ = 19, 7.5
KLUN = ("red_carpet", "brown_carpet", "red_carpet")      # klunmatten over three blocks width


def clubhuis(d):
    """IJsclub De Vadse Schaats: x 1..9, z 1..7, two floors, gables north/south with guh faces."""
    x0, x1, z0, z1 = 1, 9, 1, 7
    cx = (x0 + x1) // 2
    d.fill(x0, -1, z0, x1, -1, z1, HOUT)
    for y in range(0, 7):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    d.set(x, y, z, HOUT)
        for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            d.set(x, y, z, STAM, {"axis": "y"})
    for x in range(x0 + 1, x1):                        # dark band at the upper floor
        for z in (z0, z1):
            d.set(x, 3, z, "stripped_dark_oak_log", {"axis": "x"})
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            d.set(x, 3, z, "stripped_dark_oak_log", {"axis": "z"})
    d.fill(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, HOUT_PLAAT, {"type": "top", "waterlogged": "false"})
    d.trap_omhoog(x0 + 1, z1 - 1, HOUT_TRAP, HOUT)      # stair up along the west wall
    # roof with a thick snow blanket and gables with guh faces + ears on the ridge ends
    top = d.zadeldak(x0, x1, z0, z1, 7, langs="z", vol="dark_oak_planks", overstek=1, gevel=HOUT, sneeuw=True)
    for z, fac in ((z1, "south"), (z0, "north")):
        d.gezicht(cx, 9, z, fac, 2)
    # a little chimney with smoke (campfire under a trapdoor would be too much: a lit campfire on top)
    # south facade: door, windows with shutters, kantine hatch with its counter + striped awning
    d.deur(3, 0, z1, "south", "spruce")
    d.set(3, -1, z1 + 1, HOUT)
    d.set(2, 1, z1, GLAS)
    d.set(5, 1, z1, GLAS)
    d.lucht(6, 1, z1, 8, 1, z1)                          # the kantine hatch (3 wide)
    for x in (6, 7, 8):
        d.plaat(x, 1, z1 + 1, HOUT_PLAAT, "bottom")
        d.set(x, 0, z1 + 1, "stripped_spruce_log", {"axis": "x"})
        d.set(x, 2, z1 + 1, "lime_wool" if x % 2 else "white_wool")
        d.set(x, 0, z1 - 1, HOUT)
    d.set(6, 1, z1 - 1, "guhs:theepotje", {"facing": "south"})
    d.set(7, 1, z1 - 1, "guhs:theepotje", {"facing": "south"})
    d.set(8, 1, z1 - 1, "guhs:block_of_kaasknabbels")
    d.boost.append((7, 2, z1 + 1))
    d.bord(5, 2, z1 + 1, "south", ["IJsclub", "De Vadse", "Schaats"], glow=True, kleur="lime")
    d.bord(4, 1, z1 + 1, "south", ["Warme", "chocovet met", "slagroom!"])
    for x in (2, 5, 8):
        d.set(x, 5, z1, GLAS)
        d.luiken(x, 5, z1, "south", GROEN_LUIK)
    d.vlag(x0, 4, z1 + 1, "ijs", facing="south")
    d.vlag(x1, 4, z1 + 1, "nl", facing="south")
    d.kopje(cx, 5, z1, "south", 3, hout=True)
    # east facade (towards the rink): windows + balcony door
    for z in (2, 6):
        d.set(x1, 1, z, GLAS)
        d.set(x1, 2, z, GLAS)
    d.deur(x1, 4, 4, "east", "spruce")
    d.set(x1, 5, 2, GLAS)
    d.set(x1, 5, 6, GLAS)
    d.kopje(x1, 1, 4, "east", 1, hout=True)
    # the balcony (x 10..11, z 2..6) on two posts, railing, lanterns
    for z in (2, 6):
        for y in range(0, 3):
            d.set(11, y, z, STAM, {"axis": "y"})
    d.fill(10, 3, 2, 11, 3, 6, HOUT_PLAAT, {"type": "top", "waterlogged": "false"})
    for z in range(2, 7):
        d.set(11, 4, z, HOUT_HEK)
    d.set(10, 4, 2, HOUT_HEK)
    d.set(10, 4, 6, HOUT_HEK)
    d.lantaarn(11, 5, 2)
    d.lantaarn(11, 5, 6)
    d.set(10, 4, 5, "guhs:guh_bank", {"facing": "east"})
    # west + north windows, faces
    for z in (3, 5):
        d.set(x0, 1, z, GLAS)
        d.set(x0, 5, z, GLAS)
    d.kopje(x0, 1, 4, "west", 0, hout=True)
    for x in (3, 7):
        d.set(x, 1, z0, GLAS)
        d.set(x, 5, z0, GLAS)
    # inside: kantine tables, a cosy stove, the club plushie, a lantern upstairs
    d.set(4, 0, 3, "guhs:guh_tafel", {"facing": "south"})
    d.set(4, 0, 2, "guhs:guh_stoel", {"facing": "south"})
    d.set(4, 0, 4, "guhs:guh_stoel", {"facing": "north"})
    d.set(4, 1, 3, "guhs:theepotje", {"facing": "south"})
    d.set(7, 0, 3, "guhs:guh_tafel", {"facing": "south"})
    d.set(6, 0, 3, "guhs:guh_stoel", {"facing": "east"})
    d.set(8, 0, 3, "guhs:guh_stoel", {"facing": "west"})
    d.set(8, 0, 2, "blast_furnace", {"facing": "west", "lit": "true"})
    d.lantaarn(5, 0, 5)
    d.set(5, 4, 2, "guhs:guh_tafel", {"facing": "south"})
    d.set(4, 4, 2, "guhs:knuffel_pinguh", {"facing": "south"})
    d.set(5, 4, 6, "guhs:guh_bank", {"facing": "north"})
    d.lantaarn(7, 4, 2)


def ijsbaan(d):
    """The hockey rink: boarding, polderijs, goals with nets, a guh face on the ice, puck, scoreboard."""
    x0, x1, z0, z1 = RINK_X0, RINK_X1, RINK_Z0, RINK_Z1
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            d.set(x, -1, z, POLDERIJS)
    ingangen = {(x0, 10), (x0, 11), (18, z1), (19, z1), (20, z1)}
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                if (x, z) in ingangen:
                    d.set(x, -1, z, POLDERIJS)
                    continue
                corner = (x in (x0, x1)) and (z in (z0, z1))
                d.set(x, -1, z, "packed_ice")
                d.set(x, 0, z, "light_blue_concrete" if corner else "white_concrete")
                if not corner:
                    d.set(x, 1, z, "red_carpet" if (x + z) % 2 else "blue_carpet")
    # goals: red posts + crossbar, white nets
    for gx in (x0 + 1, x1 - 1):
        for z in (6, 9):
            d.set(gx, 0, z, "red_concrete")
            d.set(gx, 1, z, "red_concrete")
        for z in (7, 8):
            d.set(gx, 1, z, "red_concrete")
            d.set(gx, 0, z, "white_stained_glass_pane",
                  {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    # the guh face on the ice (carpets) + centre line + blue lines
    R = 3.2
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            role = face_role(x - RINK_CX, -(z - RINK_CZ), R)
            if role:
                kleur = {"skin": "pink_carpet", "eye": "black_carpet", "shine": "white_carpet", "ring": "light_blue_carpet",
                         "nose": "magenta_carpet", "mouth": "purple_carpet", "cheek": "magenta_carpet"}[role]
                d.set(x, 0, z, kleur)
            elif x == RINK_CX:
                d.set(x, 0, z, "red_carpet")
            elif x in (RINK_CX - 4 - 1, RINK_CX + 4 + 1):
                d.set(x, 0, z, "blue_carpet")
    d.set(16, 0, 11, "polished_blackstone_pressure_plate", {"powered": "false"})   # the puck
    # ice-hockey guhtjes (two players and a goalie)
    d.publiek_guh(16, 0, 10, 270)
    d.publiek_guh(22, 0, 5, 90)
    d.publiek_guh(24, 0, 8, 90)
    # glowing ice crystals along the boarding and a spectators' bench on the east side
    for x, z in ((27, 4), (27, 11), (11, 13)):
        y = d.maaiveld(x, z)
        if y is not None:
            d.set(x, y, z, IJSPEGEL)
            d.licht(x, y, z)
    # lanterns on the corners
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        for y in range(1, 4):
            d.set(x, y, z, HOUT_HEK)
        d.lantaarn(x, 4, z)
    # scoreboard on the north boarding (x 15..23), readable from the ice, with a guh head on top
    bx0, bx1 = 15, 23
    for x in (bx0, bx1):
        for y in range(1, 8):
            d.set(x, y, z0, STAM, {"axis": "y"})
    for x in range(bx0 + 1, bx1):
        for y in range(4, 8):
            d.set(x, y, z0, "black_concrete")
    for x in range(bx0, bx1 + 1):
        d.set(x, 8, z0, "dark_oak_planks")
    d.bord(17, 6, z0 + 1, "south", ["GUHTJES"], glow=True, kleur="yellow")
    d.bord(17, 5, z0 + 1, "south", ["3"], glow=True, kleur="yellow")
    d.bord(21, 6, z0 + 1, "south", ["MIKA'S"], glow=True, kleur="light_blue")
    d.bord(21, 5, z0 + 1, "south", ["2"], glow=True, kleur="light_blue")
    d.bord(19, 5, z0 + 1, "south", ["-"], glow=True, kleur="white")
    d.set(18, 9, z0, "dark_oak_planks")
    d.kopje(19, 9, z0, "south", 3)
    d.set(20, 9, z0, "dark_oak_planks")
    d.oren(19, 9, z0, "x", gap=1)
    for x in (bx0, bx1):
        d.vlag(x, 9, z0, "ijs")


def klunplek(d):
    """The klunpad: from the canal over a little dike on the klunbrugje to the rink's west gate."""
    # the dike (z 16..18) across the west part
    for x in range(0, 11):
        d.set(x, 0, 16, RIJPGRAS)
        d.set(x, 0, 17, RIJPGRAS)
        d.set(x, 1, 17, RIJPGRAS)
        d.set(x, 0, 18, RIJPGRAS)
    # the klunbrugje at x 3..5: plank steps with klunmatten (carpet) on top, railings with lanterns
    prof = {19: 0, 18: 1, 17: 2, 16: 1, 15: 0}
    for i, x in enumerate((3, 4, 5)):
        for z, y in prof.items():
            for yy in range(0, y):
                d.set(x, yy, z, RIJPGRAS)
            d.set(x, y, z, HOUT)
            d.set(x, y + 1, z, KLUN[i])
    for x in (2, 6):
        for z, y in prof.items():
            for yy in range(0, y + 1):
                d.set(x, yy, z, HOUT)
            d.set(x, y + 1, z, HOUT_HEK)
            if z == 17:
                d.set(x, y + 2, z, HOUT_HEK)
        d.lantaarn(x, 5, 17)
    d.bord(7, 0, 19, "south", ["KLUNPLEK", "schaatsen aan,", "voorzichtig", "klunnen, njeg!"], wall=False)
    # hay bales along the dike
    for x, z, y in ((0, 16, 1), (1, 16, 1), (8, 16, 1), (9, 16, 1), (0, 18, 1), (9, 18, 1), (10, 17, 2)):
        d.set(x, y, z, "hay_block", {"axis": "x"})
    # klunpad north of the dike to the rink gate: planks with klunmatten
    for z in range(10, 15):
        for i, x in enumerate((3, 4, 5)):
            d.set(x, -1, z, HOUT)
            d.set(x, 0, z, KLUN[i])
    for x in range(6, 12):
        for z in (10, 11):
            d.set(x, -1, z, HOUT)
            d.set(x, 0, z, "red_carpet" if z == 10 else "brown_carpet")
    # klunpad south of the dike to the quay
    for i, x in enumerate((3, 4, 5)):
        d.set(x, -1, 20, HOUT)
        d.set(x, 0, 20, KLUN[i])
    d.knotwilg(9, 14, 2)
    d.sneeuwpopguh(1, 11, "east")
    d.sneeuwpopguh(8, 13, "west")


def kade(d):
    # quay
    for x in range(0, 28):
        for z in range(21, 24):
            d.set(x, -1, z, KLINK)
    # path from the rink's south gate to the quay
    for z in range(14, 21):
        for x in (18, 19, 20):
            d.set(x, -1, z, KLINK)
    # steiger with the Stempelguh in a little stamp booth
    for x in range(16, 23):
        for z in (22, 23):
            d.set(x, -1, z, HOUT)
    for x in (16, 22):
        d.set(x, 0, 23, STAM, {"axis": "y"})
        d.lantaarn(x, 1, 23)
    d.stempelguh(19, 0, 22, 0)
    for x in (16, 22):
        for y in range(0, 4):
            d.set(x, y, 20, STAM, {"axis": "y"})
    for x in range(15, 24):
        d.plaat(x, 4, 20, "dark_oak_slab")
    for x in range(16, 23):
        d.plaat(x, 4, 21, "dark_oak_slab")
    d.set(18, 4, 20, "dark_oak_planks")
    d.set(20, 4, 20, "dark_oak_planks")
    d.kopje(19, 4, 20, "south", 3, hout=True)
    d.oren(19, 5, 20, "x", gap=0)
    d.set(19, 5, 20, "dark_oak_planks")
    d.bord(17, 3, 21, "south", ["Welkom in", "Vadskum"], glow=True, kleur="lime")
    d.bord(21, 3, 21, "south", ["Stempel", "hier! VAHOEG!"], glow=True, kleur="lime")
    for x in range(17, 22):
        d.set(x, 3, 20, "guhs:vlaggetjes", {"axis": "x"})
    # skate grinder's shed (x 23..26, z 15..18)
    x0, x1, z0, z1 = 23, 26, 15, 18
    d.fill(x0, -1, z0, x1, -1, z1, HOUT)
    for y in range(0, 3):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    corner = x in (x0, x1) and z in (z0, z1)
                    d.set(x, y, z, "stripped_spruce_log", {"axis": "y" if corner else ("x" if z in (z0, z1) else "z")})
    d.lucht(x0 + 1, 0, z1, x1 - 1, 1, z1)
    d.set(x0 + 1, 0, z1 - 1, "grindstone", {"face": "floor", "facing": "south"})
    d.set(x1 - 1, 0, z1 - 1, "barrel", {"facing": "up", "open": "false"})
    d.set(x0 + 1, 0, z0 + 1, "anvil", {"facing": "east"})
    d.zadeldak(x0, x1, z0, z1, 3, langs="x", vol="dark_oak_planks", overstek=1, gevel="stripped_spruce_log", sneeuw=True)
    d.kopje(x0, 2, 16, "west", 0, hout=True)
    d.bord(x0 + 1, 2, z1 + 1, "south", ["Schaatsen", "slijpen:", "vlijmscherp", "en vahoeg!"])
    # spectators on benches along the ice
    for x in (8, 9, 11, 12):
        d.set(x, 0, 21, "guhs:guh_bank", {"facing": "south"})
    d.publiek_guh(10, 0, 21, 0)
    d.publiek_guh(13, 0, 21, 0)
    d.publiek_guh(25, 0, 21, 0)
    d.vuurkorf(7, 22)
    d.vuurkorf(14, 22)
    d.vuurkorf(24, 22)
    for x, k in ((1, "mint"), (27, "geel")):
        d.paal(x, 21, 3, k)
    d.sneeuwpopguh(21, 16, "south")
    d.knotwilg(14, 17, 3)
    d.vlaggenmast(10, 19, 6, "ijs")
    d.vlaggenmast(0, 20, 5, "nl")


def bouw(s, ox, oy, oz):
    d = Dorp(s, ox, oy, oz, INDEX, SLUG, 20290607)
    clubhuis(d)
    ijsbaan(d)
    klunplek(d)
    kade(d)
    d.sneeuw_op_daken()
    d.grondversiering(0.25)
    global TEKSTEN
    TEKSTEN = dict(d.teksten)
    return d.resultaat()


TEKSTEN = {}        # sign lang keys -> Dutch text (filled by bouw(); register them in nl_nl AND en_us)


def teksten():
    """All sign texts of this village {lang key: Dutch text} (builds the village once on a scratch structure)."""
    if not TEKSTEN:
        from make_structures import Structure
        bouw(Structure((PX, PY + 4, PZ)), 0, 3, 0)
    return dict(TEKSTEN)


def check(s, ox, oy, oz):
    controleer(s, ox, oy, oz, INDEX, NAAM)
