"""Elf-Guhjestocht village 6: GUHDELOOPEN — "Poffertjes & vlaggenfeest".

The flag village: a pink raadhuis with stepped gables that are guh heads (eyes, snoet, ears on top), two narrow
grachtenpandjes with klokgevels and hoist beams, and in the middle the big poffertjeskraam "De Vadse Pan" (striped
awning, a guh-face gable, a giant poffertje sign with powdered sugar on the roof, a poffertjes pan full of little
poffertjes and a warme-chocovet corner). West: a flag tribune where the audience waves Dutch and guh flags; east: the
vlaggenveld with a tall flag mast, snowman-guhs and a skate rack. Along the ice: a welcome arch with guh ears on the
steiger (the Stempelguh stands under it), vuurkorven, lampion posts, bunting over the plein and mooring posts.
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


INDEX = 6
NAAM = "Guhdeloopen"
SLUG = "guhdeloopen"

ORANJE_DAK, ORANJE_VOL, ORANJE_PLAAT = "acacia_stairs", "acacia_planks", "acacia_slab"
WIT = "quartz_slab"
GLAS = "light_blue_stained_glass"
LUIK = "warped_trapdoor"


def trapgevel(d, x0, x1, z, y0, mat, cap=WIT, facing="south", oren=True):
    """A Dutch stepped gable (trapgevel) in the plane z over x0..x1 starting at y0 (the roof's first layer).
    The steps come in pairs and always stand above the roof behind it. Guh ears sit on the middle steps."""
    c = (x0 + x1) // 2
    for x in range(x0, x1 + 1):
        dist = min(x - x0, x1 - x)
        top = y0 + 2 * (dist // 2) + 2
        for y in range(y0, top):
            d.set(x, y, z, mat)
        if oren and abs(x - c) in (1, 2):
            continue
        d.plaat(x, top, z, cap)
    if oren:
        # ears on the pair of steps next to the middle
        top2 = y0 + 2 * ((c - x0 - 1) // 2) + 2
        for side in (-1, 1):
            d.set(c + side * 2, top2, z, "pink_wool")
            d.set(c + side * 1, top2, z, "pink_terracotta")
            d.set(c + side * 2, top2 + 1, z, "pink_wool")


def raadhuis(d):
    """Pink raadhuis x 1..9, z 1..7: two floors, white bands and pilasters, stepped gables that are guh heads."""
    x0, x1, z0, z1 = 1, 9, 1, 7
    d.fill(x0, -1, z0, x1, -1, z1, VH)
    for y in range(0, 7):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    d.set(x, y, z, KS)
        for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):      # white corner pilasters
            d.set(x, y, z, "white_concrete")
    for x in range(x0, x1 + 1):                                      # white band between the floors
        for z in (z0, z1):
            d.set(x, 3, z, "white_concrete")
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            d.set(x, 3, z, "white_concrete")
    d.fill(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, VH_PLAAT, {"type": "top", "waterlogged": "false"})   # upstairs floor
    d.trap_omhoog(x0 + 1, z1 - 2, VH_TRAP, VH)
    # roof + stepped gables at both ends; each gable is a guh head (face + ears on the steps)
    d.zadeldak(x0, x1, z0, z1, 7, langs="z", trap=ORANJE_DAK, vol=ORANJE_VOL, overstek=0)
    for z, fac in ((z1, "south"), (z0, "north")):
        trapgevel(d, x0, x1, z, 7, KS, facing=fac)
        d.gezicht(5, 8, z, fac, 2.6)
    # south facade: door, big windows, shutters, sign, lamps, flags
    d.deur(5, 0, z1, "south", "spruce")
    d.set(5, -1, z1 + 1, KLINK)
    d.plaat(5, 2, z1, KS_PLAAT, "double")
    for x in (2, 3, 7, 8):
        d.fill(x, 1, z1, x, 2, z1, GLAS)
    for x in (3, 5, 7):
        d.set(x, 4, z1, GLAS)
        d.luiken(x, 4, z1, "south", LUIK)
    d.bord(5, 2, z1 + 1, "south", ["Raadhuis", "Guhdeloopen", "Vahoeg en", "vlaggetjes!"], hout="spruce")
    for x in (3, 7):
        d.set(x, 3, z1 + 1, VH_HEK)
        d.lantaarn(x, 2, z1 + 1, hanging=True)
    d.vlag(4, 4, z1 + 1, "nl", facing="south")
    d.vlag(6, 4, z1 + 1, "guh", facing="south")
    # flag mast on the roof ridge
    for y in range(12, 15):
        d.set(5, y, 4, VH_HEK)
    d.vlag(5, 15, 4, "nl")
    # side and back windows + little faces
    for x in (x0, x1):
        for z in (3, 5):
            d.fill(x, 1, z, x, 2, z, GLAS)
            d.set(x, 4, z, GLAS)
            d.set(x, 5, z, GLAS)
        d.kopje(x, 5, 4, "west" if x == x0 else "east", 1)
    for x in (3, 7):
        d.fill(x, 1, z0, x, 2, z0, GLAS)
        d.set(x, 4, z0, GLAS)
    # inside: a council table with guh chairs and a teapot, a bookshelf, the village's plushie, lamps
    for x in (4, 5, 6):
        d.set(x, 0, 4, "guhs:guh_tafel", {"facing": "south"})
    for x in (4, 6):
        d.set(x, 0, 3, "guhs:guh_stoel", {"facing": "south"})
        d.set(x, 0, 5, "guhs:guh_stoel", {"facing": "north"})
    d.set(5, 1, 4, "guhs:theepotje", {"facing": "south"})
    d.fill(8, 0, 2, 8, 1, 2, "bookshelf")
    d.set(8, 0, 6, "guhs:knuffel_pinguh", {"facing": "west"})
    d.set(8, 4, 4, "guhs:guh_bank", {"facing": "west"})
    d.lantaarn(8, 4, 2)
    d.lantaarn(2, 0, 6)


def gevelhuis(d, x0, muur, rand, dak_trap, dak_vol, deur_x, kleur_luik):
    """A narrow grachtenpandje (5 wide) with a klokgevel (bell gable), a hoist beam and a face in the gable."""
    x1, z0, z1 = x0 + 4, 1, 7
    d.fill(x0, -1, z0, x1, -1, z1, VH)
    for y in range(0, 7):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    d.set(x, y, z, muur)
    d.fill(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, VH_PLAAT, {"type": "top", "waterlogged": "false"})
    d.trap_omhoog(x1 - 1, z1 - 1, VH_TRAP, VH)
    d.zadeldak(x0, x1, z0, z1, 7, langs="z", trap=dak_trap, vol=dak_vol, overstek=0)
    for z, fac in ((z1, "south"), (z0, "north")):
        prof = [8, 10, 11, 10, 8]
        for i, x in enumerate(range(x0, x1 + 1)):
            for y in range(7, prof[i]):
                d.set(x, y, z, muur)
            d.set(x, prof[i], z, rand)
        d.kopje(x0 + 2, 9, z, fac, 3 if fac == "south" else 0)
        d.set(x0 + 1, 10, z, rand)
        d.set(x0 + 3, 10, z, rand)
        # little ears on the bell top
        d.set(x0 + 1, 11, z, "pink_wool")
        d.set(x0 + 3, 11, z, "pink_wool")
    # hoist beam with its chain
    d.set(x0 + 2, 11, z1 + 1, "spruce_fence")
    d.set(x0 + 2, 10, z1 + 1, "chain", {"axis": "y", "waterlogged": "false"})
    # facade
    d.deur(deur_x, 0, z1, "south", "spruce")
    d.set(deur_x, 2, z1, rand)
    ramen = [x for x in range(x0 + 1, x1) if x != deur_x]
    for x in ramen:
        d.set(x, 1, z1, GLAS)
        d.set(x, 2, z1, GLAS)
    for x in (x0 + 1, x0 + 3):
        d.set(x, 4, z1, GLAS)
        d.set(x, 5, z1, GLAS)
        d.plaat(x, 6, z1 + 1, WIT)
    d.set(x0 + 2, 4, z1, muur)
    d.set(x0 + 2, 5, z1, GLAS)
    for x in ramen:
        d.plaat(x, 3, z1 + 1, WIT)
    d.vlag(x0 + 2, 4, z1 + 1, "nl" if x0 < 20 else "oranje", facing="south")
    d.set(deur_x, -1, z1 + 1, KLINK)
    for z in (3, 5):
        d.set(x0, 1, z, GLAS)
        d.set(x1, 4, z, GLAS)
    d.set(x0 + 2, 4, z0, GLAS)
    d.set(x0 + 2, 1, z0, GLAS)
    # inside
    d.set(x0 + 1, 0, 3, "guhs:guh_bank", {"facing": "east"})
    d.set(x0 + 1, 0, 5, "guhs:guh_tafel", {"facing": "east"})
    d.lantaarn(x0 + 2, 0, 2)
    d.lantaarn(x0 + 1, 4, 3)


def poffertjeskraam(d):
    """De Vadse Pan: x 10..18, z 9..15, open front with a counter, striped awning, pink guh-head gables with ears,
    a giant poffertje sign on the ridge."""
    x0, x1, z0, z1 = 10, 18, 9, 15
    d.fill(x0, -1, z0, x1, -1, z1, KLINK)
    for y in range(0, 5):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z == z0:
                    d.set(x, y, z, VH)
        for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            d.set(x, y, z, VH_STAM, {"axis": "y"})
    for z in (11, 13):                                # side windows
        for x in (x0, x1):
            d.set(x, 2, z, GLAS)
    # counter + striped awning with lampions
    for x in range(x0 + 1, x1):
        d.set(x, 0, z1, "stripped_spruce_wood", {"axis": "y"})
        d.plaat(x, 1, z1, "spruce_slab")
    for x in range(x0, x1 + 1):
        wol = "red_wool" if (x - x0) % 2 == 0 else "white_wool"
        d.set(x, 4, z1, wol)
        d.set(x, 4, z1 + 1, wol)
    for x in range(x0, x1 + 1, 2):
        d.lampion(x, 3, z1 + 1, "geel" if x % 4 == 2 else "roze", hanging=True)
    d.set(11, 2, z1, "guhs:theepotje", {"facing": "south"})
    d.set(17, 2, z1, "guhs:theepotje", {"facing": "south"})
    # roof; both gables become pink guh heads with a face and ears on the ridge
    top = d.zadeldak(x0, x1, z0, z1, 5, langs="z", trap=DAK_TRAP, vol=DAK, overstek=1, gevel="pink_wool")
    for z, fac in ((z1, "south"), (z0, "north")):
        d.gezicht(14, 7, z, fac, 2, skin=False)
        d.oren(14, top + 1, z, "x", gap=0)
    # giant poffertje sign on the ridge (golden brown rim, a pat of butter, powdered sugar on top)
    cy = top + 5
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            r = math.hypot(dx, dy)
            if r <= 3.2:
                d.set(14 + dx, cy + dy, 12, "orange_terracotta" if r > 2.2 else "yellow_terracotta")
    d.set(14, cy, 12, "yellow_concrete")
    d.set(15, cy + 1, 12, "yellow_concrete")
    for dx in range(-2, 3):
        d.set(14 + dx, cy + 3 + (1 if abs(dx) < 2 else 0), 12, "white_concrete")
    d.set(14, top + 1, 12, "stripped_spruce_wood", {"axis": "y"})
    # inside: the poffertjes pan (black with a lot of tiny poffertjes), a smoker, flour, butter, crates of knabbels
    for x in (12, 13, 14, 15, 16):
        d.set(x, 0, 11, "black_concrete")
        d.set(x, 1, 11, "yellow_candle", {"candles": str(3 + (x % 2)), "lit": "false", "waterlogged": "false"})
    d.set(11, 0, 10, "smoker", {"facing": "south", "lit": "true"})
    d.set(17, 0, 10, "smoker", {"facing": "south", "lit": "true"})
    d.set(11, 0, 12, "guhs:block_of_kaasknabbels")
    d.set(17, 0, 14, "barrel", {"facing": "up", "open": "false"})
    d.set(17, 1, 14, "white_carpet")
    d.set(11, 1, 12, "yellow_carpet")
    # the side door (east) and a sign
    d.deur(x1, 0, 12, "east", VH_DEUR)
    d.bord(x1 + 1, 2, 13, "east", ["De Vadse Pan", "poffertjes met", "poedersuiker", "VAHOEG!"])
    d.bord(x0 - 1, 2, 12, "west", ["Poffertjes!", "Zo rond als", "een guhbuikje"])
    d.kopje(x0, 2, 11, "west", 3, hout=True)
    d.kopje(x1, 2, 10, "east", 3, hout=True)
    d.boost.append((14, 2, z1))


def tribune(d):
    """Flag tribune west of the plein (x 0..6, z 12..18), seats looking south at the ice."""
    x0, x1 = 0, 6
    rows = [(18, 0), (17, 1), (16, 2), (15, 3)]
    for z, h in rows:
        for x in range(x0, x1 + 1):
            for y in range(0, h):
                d.set(x, y, z, VH)
            d.trap(x, h, z, VH_TRAP, "north")
    # back wall with a big guh face looking north (the tribune is pretty from behind too)
    z = 14
    for x in range(x0, x1 + 1):
        for y in range(0, 7):
            d.set(x, y, z, KS)
    d.gezicht(3, 4, z, "north", 2.6)
    for x in range(x0, x1 + 1):
        d.plaat(x, 7, z, KS_PLAAT)
    for side in (-1, 1):
        d.set(3 + side * 2, 7, z, "pink_wool")
        d.set(3 + side * 1, 7, z, "pink_terracotta")
        d.set(3 + side * 2, 8, z, "pink_wool")
    # flags: wall banners on the back wall above the top row + banners on the seat ends
    for x, soort in ((0, "nl"), (2, "oranje"), (4, "guh"), (6, "nl")):
        d.vlag(x, 5, 15, soort, facing="south")
    # audience
    d.publiek_guh(1, 1, 18, 0)
    d.publiek_guh(4, 2, 17, 0)
    d.publiek_guh(2, 3, 16, 0)
    # lampions on posts at both ends of the front row
    d.paal(0, 19, 3, "roze")
    d.paal(6, 19, 3, "geel")


def vlaggenveld(d):
    """East: the flag field with a tall mast on a round podium, snowman-guhs, a skate rack and a knotwilg."""
    cx, cz = 23, 13
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if dx * dx + dz * dz <= 5:
                d.plaat(cx + dx, 0, cz + dz, KS_PLAAT)
    for y in range(1, 10):
        d.set(cx, y, cz, VH_HEK)
    d.vlag(cx, 10, cz, "nl")
    d.publiek_guh(cx - 1, 1, cz + 1, 0)
    d.publiek_guh(cx + 1, 1, cz + 1, 0)
    d.vlag(cx - 2, 1, cz, "oranje")
    d.vlag(cx + 2, 1, cz, "guh")
    d.sneeuwpopguh(20, 17, "south")
    d.sneeuwpopguh(26, 17, "south")
    d.knotwilg(26, 9, 3)
    d.knotwilg(20, 9, 2)
    # skate rack: a bench with a little sign
    d.set(24, 0, 17, "guhs:guh_bank", {"facing": "south"})
    d.set(23, 0, 17, "guhs:guh_bank", {"facing": "south"})
    d.bord(25, 0, 17, "south", ["Schaatsen", "binden hier", "(met vadsige", "veters)"], wall=False)


def plein_en_kade(d):
    # klinker paths
    for x in range(0, 28):
        for z in range(20, 24):
            d.set(x, -1, z, KLINK)
    for z in range(16, 20):
        for x in range(8, 21):
            d.set(x, -1, z, KLINK)
    for z in range(9, 16):
        d.set(5, -1, z, KLINK)
    for z in range(12, 20):
        d.set(19, -1, z, KLINK)
    for x in range(5, 9):
        d.set(x, -1, 19, KLINK)
    for z in range(9, 20):
        d.set(7, -1, z, KLINK)
    for z in range(8, 12):
        d.set(19, -1, z, KLINK)
    for x in range(19, 25):
        d.set(x, -1, 8, KLINK)
    for x in (19, 24):
        d.set(x, -1, 8, KLINK)
    # steiger and welcome arch with the Stempelguh
    for x in range(11, 18):
        for z in range(21, 24):
            d.set(x, -1, z, VH)
    for x in (11, 17):
        for y in range(0, 4):
            d.set(x, y, 22, VH_STAM, {"axis": "y"})
    for x in range(11, 18):
        d.set(x, 4, 22, VH_STAM, {"axis": "x"})
    for x in range(12, 17):
        d.set(x, 3, 22, "guhs:vlaggetjes", {"axis": "x"})
    d.set(13, 5, 22, KS)
    d.set(15, 5, 22, KS)
    d.kopje(14, 5, 22, "south", 3)
    d.set(14, 6, 22, KS)
    d.oren(14, 6, 22, "x", gap=0)
    d.vlag(11, 5, 22, "nl")
    d.vlag(17, 5, 22, "nl")
    d.bord(13, 4, 23, "south", ["Welkom in", "Guhdeloopen"], glow=True, kleur="orange")
    d.bord(15, 4, 23, "south", ["Stempel", "hier! VAHOEG!"], glow=True, kleur="orange")
    d.stempelguh(14, 0, 22, 0)
    # vuurkorven, lampion posts, mooring posts
    d.vuurkorf(9, 21)
    d.vuurkorf(19, 21)
    for x, k in ((2, "roze"), (6, "geel"), (22, "geel"), (26, "roze")):
        d.paal(x, 21, 3, k)
    for x in (0, 4, 24, 27):
        d.set(x, 0, 23, VH_STAM, {"axis": "y"})
    d.publiek_guh(4, 0, 21, 0)
    d.publiek_guh(24, 0, 21, 0)
    # bunting across the plein between two masts
    for x in (8, 20):
        for y in range(0, 5):
            d.set(x, y, 19, VH_HEK)
        d.vlag(x, 5, 19, "oranje")
    for x in range(9, 20):
        d.set(x, 4, 19, "guhs:vlaggetjes", {"axis": "x"})
    # poffertjes tables on the plein and glowing ice crystals
    for x in (10, 18):
        d.set(x, 0, 18, "guhs:guh_tafel", {"facing": "south"})
        d.set(x - 1, 0, 18, "guhs:guh_stoel", {"facing": "east"}) if x == 10 else d.set(x + 1, 0, 18, "guhs:guh_stoel", {"facing": "west"})
        d.set(x, 1, 18, "white_carpet")
    for x, z in ((7, 12), (8, 5), (16, 7), (27, 11)):
        y = d.maaiveld(x, z)
        if y is not None:
            d.set(x, y, z, IJSPEGEL)
            d.licht(x, y, z)
    # a guh molentje on a post between the back houses + bench + knotwilg
    d.set(13, 0, 3, VH_STAM, {"axis": "y"})
    d.set(13, 1, 3, MOLENTJE)
    d.set(12, 0, 5, "guhs:guh_bank", {"facing": "south"})
    d.set(14, 0, 5, "guhs:guh_bank", {"facing": "south"})
    d.knotwilg(11, 2, 3)
    d.knotwilg(15, 1, 2)


def bouw(s, ox, oy, oz):
    d = Dorp(s, ox, oy, oz, INDEX, SLUG, 20290606)
    raadhuis(d)
    gevelhuis(d, 17, "bricks", "quartz_block", DAK_TRAP, DAK, 18, "warped_trapdoor")
    gevelhuis(d, 22, "orange_terracotta", "quartz_block", "dark_oak_stairs", "dark_oak_planks", 23, "spruce_trapdoor")
    poffertjeskraam(d)
    tribune(d)
    vlaggenveld(d)
    plein_en_kade(d)
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
