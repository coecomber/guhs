"""
einde2 (the Guheinde expansion), slice deco: the builder of the small decoration buildings of the outer islands.

Every building is ONE template einde2_deco/<id> (at most 24 x 24), the start piece of a guhs:einde2_deco_bouwsel structure
(features/einde2_deco.py writes the worldgen). The piece is put down with its bottom G blocks under the surface in the
middle of its box (start_height -G), so:

    y 0 .. G-1   the footing: the building's own ground, down into the island (on a slope it shows as a low terrace)
    y G          the ground layer: it replaces the top block of the island (floors lie here)
    y G+1 ..     the building; above its ground only the cells it needs are cleared (air), so a hill next to it slopes
                 down to it instead of being cut off like a wall. Cells nobody sets stay what the island has.

Deco = kit.Bouw with the ground helpers (blob, grond), the things every building has (bord: its sign joke, kist: its one
modest chest) and klaar(): fences join up, the anchor (a jigsaw in the middle of the ground layer) and the self-check:
kit.Bouw.check (out-of-bounds blocks, floating blocks, hanging things, light on every floor you can walk on, the spots
you must be able to reach) plus the rules of this slice (size, one chest at most, a sign, the anchor in the middle).

The buildings themselves: einde2_deco_bouw_a.py (lucht, water), _b.py (aarde, vuur), _c.py (the two common biomes and the
extra ones). BOUWERS (below) maps an id to its function; each returns (Deco, start) with start = a cell at the rim where a
player walks in.
"""
import math

import features.einde2_kit as kit
from features import guheinde_bouw as gb

G = 4                         # template y of the ground layer (the jigsaw start_height is -G)
MAX_VOET = 24                 # at most this wide and deep
PREFIX = "sign.guhs.einde2_deco"
AIR = gb.AIR
KIST = "guhs:chests/einde2_deco"
KIST_THEE = "guhs:chests/einde2_deco_thee"
KIST_MIKA = "guhs:chests/einde2_deco_mika"
KISTEN = (KIST, KIST_THEE, KIST_MIKA)
RICHTING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
TEGEN = gb.OPP

# the lights of this slice's own blocks (the kit's light check reads kit.LICHT) and the small things you walk through
kit.LICHT.update({"guhs:einde2_deco_eeuwige_vlam": 15, "guhs:schimmelhoed": 10, "guhs:kristalkaas_erts": 4, "minecraft:redstone_lamp": 0,
                  "guhs:lampion_roze": 15, "guhs:lampion_geel": 15, "guhs:lampion_mint": 15})
EIGEN_DUN = ("guhs:einde2_deco_windgong", "guhs:einde2_deco_eeuwige_vlam", "guhs:einde2_deco_lotustegel", "minecraft:moss_carpet",
             "guhs:guh_waterlelie", "minecraft:string", "minecraft:cobweb", "minecraft:dead_bush", "minecraft:flower_pot",
             "minecraft:sea_pickle", "guhs:vlaggetjes", "minecraft:lily_pad")


# what the little plants of the outer islands grow on (E2BiomesBlokken.Plantje: the ground of the islands; moss is not it)
EILANDGROND = ("guhs:kaaskorst", "guhs:gebarsten_korst", "guhs:kruimelzand", "guhs:schimmelkaas", "guhs:klipkaas", "guhs:koorkorst", "guhs:kruimelrots")
PLANTGROND = {"guhs:korstgras": EILANDGROND, "guhs:dorre_korststruik": EILANDGROND, "guhs:schimmelpaddenstoel": EILANDGROND,
              "minecraft:flowering_azalea": ("minecraft:moss_block",), "minecraft:moss_carpet": ("minecraft:moss_block",)}


def trap(facing, half="bottom", shape="straight"):
    return {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"}


def plaat(t="bottom"):
    return {"type": t, "waterlogged": "false"}


def luik(facing, half="bottom", open_=False):
    return {"facing": facing, "half": half, "open": "true" if open_ else "false", "powered": "false", "waterlogged": "false"}


class Deco(kit.Bouw):
    """A decoration building in the making (template coordinates; the ground layer is y = G)."""

    def __init__(self, h, id, size):
        super().__init__(h, f"einde2_deco/{id}", size, kit.seed(f"deco/{id}"), prefix=PREFIX)
        self.id = id
        self.mx, self.mz = size[0] // 2, size[2] // 2      # the middle column: the anchor, and where the surface is read
        self.veld = set()                                   # the (x, z) cells of its own ground
        self.borden = 0
        self.gloei = ("guhs:lichtstaafje", {"facing": "up"}, None)   # the little light bijlichten() puts on dark open ground
        self.gloei_hoog = ()                                         # the blocks of a mound it may also stand on
        self.dun.update(EIGEN_DUN)

    def is_dun(self, n):
        # (the kit takes everything named minecraft:snow... for a snow layer; a snow BLOCK is solid ground)
        return n != "minecraft:snow_block" and super().is_dun(n)

    # --- ground -------------------------------------------------------------------------------------------------------------
    def blob(self, rx, rz, cx=None, cz=None, ruw=0.1):
        """The cells of an irregular round patch (an ellipse with a wobbling rim) around (cx, cz)."""
        cx = self.mx if cx is None else cx
        cz = self.mz if cz is None else cz
        f1, f2 = self.rng.uniform(0, 6.28), self.rng.uniform(0, 6.28)
        cellen = set()
        for x in range(self.W):
            for z in range(self.D):
                dx, dz = (x - cx) / rx, (z - cz) / rz
                a = math.atan2(dz, dx)
                r = 1.0 + ruw * math.sin(3 * a + f1) + ruw * 0.7 * math.sin(5 * a + f2)
                if dx * dx + dz * dz <= r * r:
                    cellen.add((x, z))
        return cellen

    def grond(self, cellen, top, onder=None, vrij=6):
        """The building's own ground under these cells: the footing (y 0 .. G-1, `onder`), the ground layer (y G, `top`: a
        block name or a function (x, z) -> name) and air above it: three blocks at the rim and one more for every cell
        further in, up to `vrij` (so the land around slopes down to the building)."""
        cellen = set(cellen)
        self.veld |= cellen
        naam = top if callable(top) else (lambda x, z: top)
        rand = self._afstand(self.veld)
        for (x, z) in sorted(cellen):
            t = naam(x, z)
            for y in range(0, G):
                self.set(x, y, z, onder or t)
            self.set(x, G, z, t)
        for (x, z) in sorted(self.veld):
            for y in range(G + 1, min(self.H, G + 1 + min(vrij, 3 + rand[(x, z)]))):
                if (x, y, z) not in self.s.blocks:
                    self.set(x, y, z, AIR)

    @staticmethod
    def _afstand(cellen):
        """(x, z) -> how many cells it lies from the rim of this patch (0 = on the rim)."""
        dist = {c: 0 for c in cellen if any((c[0] + dx, c[1] + dz) not in cellen for dx, dz in RICHTING.values())}
        rij = list(dist)
        while rij:
            volgende = []
            for (x, z) in rij:
                for dx, dz in RICHTING.values():
                    n = (x + dx, z + dz)
                    if n in cellen and n not in dist:
                        dist[n] = dist[(x, z)] + 1
                        volgende.append(n)
            rij = volgende
        return dist

    def lucht(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def top(self, x, z):
        """The y of the highest block of this column that is not air (G for bare ground)."""
        ys = [y for (bx, y, bz), bl in self.s.blocks.items() if bx == x and bz == z and bl[0] != AIR]
        return max(ys) if ys else None

    def kies(self, tabel):
        return gb.pick(self.rng, tabel)

    def strooi(self, blok, kans, props=None, op=None, cellen=None):
        """Small plants on the bare ground (only on a block of `op`, and never where something stands already)."""
        for (x, z) in sorted(cellen or self.veld):
            onder = self.s.blocks.get((x, G, z))
            vrij = self.s.blocks.get((x, G + 1, z), (AIR,))[0] == AIR
            if onder and vrij and (op is None or onder[0] in op) and self.rng.random() < kans:
                self.set(x, G + 1, z, blok, props)

    # --- the things every building has -------------------------------------------------------------------------------------------
    def bord(self, x, y, z, facing, regels, wall=True, wood="spruce", glow=False, id=None):
        """The sign joke: up to four Dutch lines of at most 16 characters (they go through sign_text: every player reads them
        in their own language). Recorded as the spot `bordje` (then `bordje_2`...; or as `id`, which starts with bordje_)."""
        self.sign(x, y, z, facing, regels, wall=wall, wood=wood, glow=glow)
        self.borden += 1
        if id is None:
            n = 1 + sum(1 for k in self.plekken if k == "bordje" or k[:7] == "bordje_" and k[7:].isdigit())
            id = "bordje" if n == 1 else f"bordje_{n}"
        # (los: a sign may hang high; klaar() wants every sign READABLE: someone can stand within a few blocks of it)
        self.prop(id, x, y, z, wie=" / ".join(r for r in regels if r), los=True)

    def kist(self, x, y, z, facing, loot=KIST, ton=False):
        """The one modest chest (or a barrel) of a building, with its loot table; recorded as the spot `kist`."""
        assert loot in KISTEN, loot
        if ton:
            self.set(x, y, z, "barrel", {"facing": facing, "open": "false"}, {"id": "minecraft:barrel", "LootTable": loot})
        else:
            self.chest(x, y, z, facing, loot)
        self.prop("kist", x, y, z, wie=loot.split("/")[-1])

    def staaf(self, x, y, z, facing="up"):
        self.set(x, y, z, "guhs:lichtstaafje", {"facing": facing})

    def lampion(self, x, y, z, kleur="geel", hangend=False):
        self.set(x, y, z, f"guhs:lampion_{kleur}", gb.lamp(hangend))

    def lantaarn(self, x, y, z, hangend=False, ziel=False):
        self.set(x, y, z, "soul_lantern" if ziel else "lantern", gb.lamp(hangend))

    def kristal(self, x, y, z, facing="up"):
        self.set(x, y, z, "guhs:kristalkaas_kristal", {"facing": facing, "waterlogged": "false"})

    def dier(self, id, x, y, z, entity, yaw=0.0, honk=6):
        """A creature of another einde2 slice that lives here (it keeps within `honk` blocks of this spot)."""
        self.wezen(id, x, y, z, entity, yaw=yaw, honk=honk)

    # --- done ----------------------------------------------------------------------------------------------------------------
    def bijlichten(self, start, min_licht=4):
        """The Guheinde has no sky light: where the open ground of the building is still too dark to walk on, a little
        light of the biome (self.gloei: the block, its properties, the ground it stands on) is put on the bare ground, each
        time on the spot that lights the most dark cells; also on top of a mound of the blocks in self.gloei_hoog (you can
        walk up there). Rooms and roofs are never touched: a builder lights those by hand."""
        blok, props, op = self.gloei
        sterkte = kit.LICHT[gb.mc(blok)]
        start = tuple(start)
        for _ in range(24):
            bereik, licht = kit.Loper(self).bfs(start), self.licht()
            donker = [c for c in bereik if licht.get(c, 0) < min_licht]
            if not donker:
                return
            toppen = {}
            for (x, y, z), bl in self.s.blocks.items():
                if bl[0] != AIR and y > toppen.get((x, z), -1):
                    toppen[(x, z)] = y
            beste, n = None, 0
            for (x, z) in sorted(self.veld):
                y = toppen.get((x, z), -1)
                onder = self.s.blocks.get((x, y, z))
                if y < G or y + 1 >= self.H or (x, y + 1, z) == start or onder[0] in kit.WATER:
                    continue
                if (y == G and op and onder[0] not in op) or (y > G and onder[0] not in self.gloei_hoog):
                    continue                                    # (something stands here: a wall, a roof, a thing)
                raakt = sum(1 for c in donker if abs(c[0] - x) + abs(c[1] - (y + 1)) + abs(c[2] - z) <= sterkte - min_licht)
                if raakt > n:
                    beste, n = (x, y + 1, z), raakt
            if beste is None:
                return
            self.set(*beste, blok, props)

    def steun(self, c, kant):
        """Does the block at c hold the middle of its `kant` face ("up": something stands on it; "down": something hangs
        under it), the way the game asks it (a top slab holds nothing under it, a bottom slab nothing on it)?"""
        bl = self.s.blocks.get(c)
        if bl is None or bl[0] == AIR or bl[0] in kit.WATER:
            return False
        n, props = bl[0], bl[1]
        if gb.is_fency(n) or n.endswith("chain"):
            return True
        if n.endswith("_slab") or n.endswith("_plaat"):
            return props.get("type") == "double" or props.get("type") == ("top" if kant == "up" else "bottom")
        if n.endswith("_stairs") or n.endswith("_trap"):
            return props.get("half") == ("top" if kant == "up" else "bottom")
        return self.solid_at(c) and not any(t in n for t in ("trapdoor", "_door", "chest", "_kussen", "campfire", "leaves", "blad", "_bladeren"))

    def steun_fouten(self):
        """The things that hang or stand on the middle of a block, each on something that really holds it: the game looks at
        its neighbours when a piece is put down, and what has no hold is gone (the kit's check takes any slab for a hold)."""
        p = []
        for (x, y, z), (n, props, _) in sorted(self.s.blocks.items()):
            hangt = n == "guhs:einde2_deco_windgong" or props.get("hanging") == "true"
            staat = props.get("hanging") == "false" or n in ("guhs:einde2_deco_eeuwige_vlam", "guhs:einde2_deco_lotustegel")
            if hangt and not self.steun((x, y + 1, z), "down"):
                p.append(f"{n} op {(x, y, z)} hangt aan niets dat het houdt ({self.get(x, y + 1, z)})")
            if staat and not self.steun((x, y - 1, z), "up"):
                p.append(f"{n} op {(x, y, z)} staat op niets dat het houdt ({self.get(x, y - 1, z)})")
            if n == "guhs:kristalkaas_kristal":
                dx, dy, dz = {"up": (0, -1, 0), "down": (0, 1, 0), "north": (0, 0, 1), "south": (0, 0, -1), "east": (-1, 0, 0), "west": (1, 0, 0)}[props["facing"]]
                achter = self.s.blocks.get((x + dx, y + dy, z + dz))
                if achter is None or not self.solid_at((x + dx, y + dy, z + dz)) or self.is_dun(achter[0]) or gb.is_fency(achter[0]):
                    p.append(f"kristal op {(x, y, z)} groeit op niets ({achter and achter[0]})")
            if n in PLANTGROND:
                onder = self.get(x, y - 1, z)
                if onder not in PLANTGROND[n]:
                    p.append(f"{n} op {(x, y, z)} groeit niet op {onder}")
        return p

    def klaar(self, start, zwevend_ok=None, min_licht=4):
        """Joins the fences, lights the open ground, checks the building and puts the anchor in. Returns the problems (an
        empty list: fine)."""
        self.connect()
        self.bijlichten(start, min_licht)
        p = self.check(tuple(start), min_licht=min_licht, zwevend_ok=zwevend_ok, standaard=False)
        p += self.steun_fouten()
        bereik = kit.Loper(self).bfs(tuple(start))
        for id, e in self.plekken.items():
            if id.startswith("bordje"):
                x, y, z = e["pos"]
                if not any(abs(c[0] - x) + abs(c[2] - z) <= 3 and -4 <= c[1] - y <= 1 for c in bereik):
                    p.append(f"{id} {(x, y, z)}: niemand kan dichtbij genoeg staan om het te lezen")
        if self.W > MAX_VOET or self.D > MAX_VOET:
            p.append(f"te groot: {self.W} x {self.D} (hoogstens {MAX_VOET} x {MAX_VOET})")
        if self.borden < 1:
            p.append("geen bordje met een grap")
        kisten = [c for c, bl in self.s.blocks.items() if bl[2] and "LootTable" in bl[2]]
        if len(kisten) > 1:
            p.append(f"meer dan een kist: {sorted(kisten)}")
        for c in kisten:
            if self.s.blocks[c][2]["LootTable"] not in KISTEN:
                p.append(f"kist {c}: onbekende loot {self.s.blocks[c][2]['LootTable']}")
        if not kisten and "kist" in self.plekken or kisten and "kist" not in self.plekken:
            p.append("de plek `kist` en de kist horen bij elkaar")
        midden = self.s.blocks.get((self.mx, G, self.mz))
        if midden is None or midden[0] == AIR or midden[2] or midden[0] in kit.WATER:
            p.append(f"geen vaste grond in het midden {(self.mx, G, self.mz)} voor het anker: {midden and midden[0]}")
        else:
            self.anchor(self.mx, G, self.mz, f"guhs:einde2_deco/{self.id}")
        if (self.mx, self.mz) not in self.veld:
            p.append("het midden ligt niet op de eigen grond")
        # the ground layer is whole under everything that stands on the ground: nothing of the building stands on the island's
        # own ground, which may be lower there (a roof may hang over it)
        for (x, y, z), bl in self.s.blocks.items():
            if bl[0] != AIR and y <= G + 1 and (x, z) not in self.veld:
                p.append(f"{bl[0]} op {(x, y, z)} staat buiten de eigen grond")
                break
        for (x, z) in self.veld:
            for y in range(0, G + 1):
                if (x, y, z) not in self.s.blocks:
                    p.append(f"gat in de eigen grond op {(x, y, z)}")
                    break
        # water lies in a closed basin of source blocks: nothing flows out when the building appears
        for (x, y, z), bl in self.s.blocks.items():
            if bl[0] in kit.WATER:
                for n in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
                    buur = self.s.blocks.get(n)
                    if buur is None or buur[0] == AIR or self.is_dun(buur[0]):
                        p.append(f"water op {(x, y, z)} kan weglopen langs {n}")
        self.prop("midden", self.mx, G + 1, self.mz, wie="het midden van het bouwsel", los=True)
        return p


# id -> the function that builds it (filled by the three modules below)
BOUWERS = {}


def bouwer(id):
    def zet(fn):
        BOUWERS[id] = fn
        return fn
    return zet


def bouw_alles(h):
    """Builds and saves every template. Returns {id: (Deco, problems)}."""
    from features import einde2_deco_bouw_a, einde2_deco_bouw_b, einde2_deco_bouw_c  # noqa: F401  (they fill BOUWERS)
    uit = {}
    for id, fn in BOUWERS.items():
        b, start = fn(h)
        problemen = b.klaar(start, zwevend_ok=getattr(b, "zwevend_ok", None))
        b.save()
        uit[id] = (b, problemen)
    return uit
