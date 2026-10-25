"""
The park volume and everything to write it (see SAFARI_CONTRACT.md).

World coordinates: x east, y up, z south; the stamped area is 768 x 160 x 768 (6 x 6 tiles of 128). Plattegrond C's park
coordinates (0..512) are world coordinates minus 128 (PARK_OFFSET).

The volume is a numpy uint16 array [x, y, z] of palette ids; a palette id is a full block state ("minecraft:oak_log" +
properties). Air (id 0) is never written: a tile only lists real blocks, the rest of the box stays as it was (the game
clears the box before a re-stamp). Fences, iron bars and gates get their connections computed in one pass
(fix_connections) before writing, because the stamp places blocks without neighbour updates.
"""
import gzip
import json
import math
import os
import struct

import numpy as np

W = 768                 # stamped area x and z
H = 160                 # tile height (y 0..159)
TEGEL = 128             # tile size
TEGELS = W // TEGEL     # 6
PARK_OFFSET = 128       # plattegrond park coords + 128 = world
WATER_Y = 62            # sea / river surface
BRUG_Y = WATER_Y + 3    # bridge deck block: y 63 and 64 stay free above the water (boats, the canoe, the glass boat)
DATA_VERSION = 3955     # 1.21.1 block names, like tools/make_structures.py (26.1.2 upgrades on load)
SEED = 20261019

OUT_STRUCT = os.path.join("src", "main", "resources", "data", "guhs", "structure", "safari")
OUT_DATA = os.path.join("src", "main", "resources", "data", "guhs", "safari")

# Landmark spots from plattegrond C (park coords -> world), for the zone slices: naam -> (x, z, straal, zone).
# Nothing is built here; terrein.py keeps these spots clear of trees and rocks so a zone module has room.
_PUNTEN = [
    ("trotse_rots", 215, 415, 14, "savanne"), ("vadse_baobab_poel", 320, 470, 16, "savanne"),
    ("termietenheuvels", 445, 455, 12, "savanne"), ("trekvlakte", 400, 495, 14, "savanne"),
    ("flamingomeer", 480, 275, 14, "delta"), ("nijlpaardenpoel", 430, 335, 12, "delta"),
    ("mangrovevlonders", 485, 340, 12, "delta"), ("krokodillenbank", 355, 410, 8, "delta"),
    ("waterval_grot", 295, 285, 14, "regenwoud"), ("reuzenboom", 355, 325, 10, "regenwoud"),
    ("ruine", 455, 150, 12, "regenwoud"), ("okapi_dal", 490, 215, 12, "regenwoud"),
    ("pagode", 435, 30, 10, "bamboe"), ("hangbrug", 480, 95, 10, "bamboe"),
    ("theeterrassen", 395, 50, 14, "bamboe"), ("bamboedoolhof", 495, 50, 12, "bamboe"),
    ("ijsgrot_fossiel", 262, 42, 12, "oertijd"), ("bevroren_waterval", 335, 112, 10, "oertijd"),
    ("warme_bronnen", 250, 125, 10, "oertijd"),
    ("scheepswrak", 70, 55, 12, "ijskust"), ("ijsbergkliffen", 120, 20, 14, "ijskust"),
    ("poolstation", 205, 75, 10, "ijskust"), ("iglo_dorpje", 35, 100, 12, "ijskust"),
    ("pinguinglijbaan", 125, 95, 10, "ijskust"),
    ("beverdam", 75, 195, 10, "europa"), ("holle_eik", 150, 235, 8, "europa"),
    ("bruggetje_molen", 215, 185, 10, "europa"), ("dassenheuvel", 35, 255, 10, "europa"),
    ("eucalyptusbos", 55, 305, 14, "australie"), ("billabong", 150, 350, 12, "australie"),
    ("quokka_eiland", 215, 300, 10, "australie"), ("wombatheuvel", 95, 370, 10, "australie"),
    ("expertenpaviljoen", 70, 485, 8, "entree"),
]
RESERVERINGEN = {n: (x + PARK_OFFSET, z + PARK_OFFSET, r, zone) for n, x, z, r, zone in _PUNTEN}

FENCE_FAMILY = ("minecraft:dark_oak_fence", "minecraft:iron_bars", "minecraft:spruce_fence", "minecraft:oak_fence")
GATE = "minecraft:dark_oak_fence_gate"


def state(name, **props):
    """'minecraft:oak_log', axis='y' -> 'minecraft:oak_log[axis=y]' (props sorted). Bare names get minecraft:."""
    if ":" not in name:
        name = "minecraft:" + name
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={str(v).lower()}" for k, v in sorted(props.items())) + "]"


def split_state(s):
    if "[" not in s:
        return s, {}
    name, rest = s[:-1].split("[", 1)
    return name, dict(p.split("=", 1) for p in rest.split(","))


class Park:
    def __init__(self):
        self.pal = ["minecraft:air"]
        self.idx = {"minecraft:air": 0}
        self.v = np.zeros((W, H, W), np.uint16)
        self.rng = np.random.default_rng(SEED)
        # filled by terrein.py, used by plekken.py and later zone modules
        self.hoogte = None      # int [x, z] ground surface y (top solid block)
        self.zone = None        # int [x, z] index into ZONES (-1 = outside the park)
        self.binnen = None      # bool [x, z] inside the outer fence
        self.water = None       # bool [x, z] water column
        self.hek = None         # bool [x, z] fence column
        self.pad = None         # bool [x, z] path column
        self.bomen = []         # (x, y_top, z, soort) of every tree
        self.ingang = None      # (x, y, z, yaw)
        # round 3: viewpoints (kijkplekken.json), their favourite spots, lights, seats and every bridge built
        self.kijkplekken = []   # dicts: naam, zone, oog [x,y,z], richting [dx,dz] | None, stoel [x,y,z] | None, favorieten
        self.lampen = []        # (x, y, z, soort)
        self.stoelen = []       # (x, y, z, kijkrichting) of every guh chair (kit.stoel)
        self.bruggen = []       # (x, z, zone) middle of every bridge

    # --- round 3: viewpoints, favourite spots, lights ---------------------------------------------------------------
    def kijkplek(self, naam, zone, oog, richting=None, stoel=None):
        """Register a viewpoint (hut window, tower deck, dome). oog = the eye position, richting = (dx, dz) the view
        direction (None = all round, a tower), stoel = the seat the kijk-guh sits on. Returns the dict (favorieten
        are added with favoriet())."""
        if richting is not None:
            n = math.hypot(richting[0], richting[1]) or 1.0
            richting = [round(richting[0] / n, 3), round(richting[1] / n, 3)]
        k = {"naam": naam, "zone": zone, "oog": [round(float(c), 2) for c in oog], "richting": richting,
             "stoel": [int(c) for c in stoel] if stoel is not None else None, "favorieten": []}
        self.kijkplekken.append(k)
        return k

    def favoriet(self, naam, kijkplek, pos, soorten=(), straal=3):
        """A favourite spot in a viewpoint's view: animals of `soorten` nearby walk there now and then (the AI reads
        kijkplekken.json). kijkplek: the dict from kijkplek() or its naam."""
        if isinstance(kijkplek, str):
            kijkplek = next((k for k in self.kijkplekken if k["naam"] == kijkplek), None)
            if kijkplek is None:
                return None
        f = {"naam": naam, "pos": [int(c) for c in pos], "soorten": list(soorten), "straal": int(straal)}
        kijkplek["favorieten"].append(f)
        return f

    LAMP = {"lantaarn": "minecraft:lantern[hanging=false,waterlogged=false]",
            "lantaarn_hangend": "minecraft:lantern[hanging=true,waterlogged=false]",
            "guhlamp": "guhs:klusjes_guhlampje[facing=south,lit=true]",
            "fakkel": "minecraft:torch", "ijslantaarn": "minecraft:sea_lantern", "kampvuur": "minecraft:campfire[lit=true,"
            "signal_fire=false,waterlogged=false,facing=north]", "glow_lichen": "minecraft:glow_lichen[down=true,east=false,"
            "north=false,south=false,up=false,waterlogged=false,west=false]", "froglight": "minecraft:pearlescent_froglight[axis=y]"}

    def lamp(self, x, y, z, soort="lantaarn"):
        """Place a light (a LAMP key or any block state) and register it (lights are listed for the checks / later
        passes)."""
        blok = self.LAMP.get(soort, soort)
        self.set(int(x), int(y), int(z), blok)
        self.lampen.append((int(x), int(y), int(z), soort))

    # --- palette / blocks -----------------------------------------------------------------------------------------
    def id(self, s, **props):
        s = state(s, **props) if props or "[" not in s else s
        if ":" not in s:
            s = "minecraft:" + s
        i = self.idx.get(s)
        if i is None:
            i = self.idx[s] = len(self.pal)
            self.pal.append(s)
        return i

    def inb(self, x, y, z):
        return 0 <= x < W and 0 <= y < H and 0 <= z < W

    def set(self, x, y, z, s, keep=False, **props):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z) or (keep and self.v[x, y, z] != 0):
            return
        self.v[x, y, z] = self.id(s, **props)

    def get(self, x, y, z):
        return self.pal[self.v[x, y, z]] if self.inb(x, y, z) else "minecraft:air"

    def leeg(self, x, y, z):
        return not self.inb(x, y, z) or self.v[x, y, z] == 0

    def fill(self, x1, y1, z1, x2, y2, z2, s, keep=False, **props):
        x1, x2 = sorted((int(x1), int(x2)))
        y1, y2 = sorted((int(y1), int(y2)))
        z1, z2 = sorted((int(z1), int(z2)))
        x1, y1, z1 = max(x1, 0), max(y1, 0), max(z1, 0)
        x2, y2, z2 = min(x2, W - 1), min(y2, H - 1), min(z2, W - 1)
        if x1 > x2 or y1 > y2 or z1 > z2:
            return
        i = self.id(s, **props)
        box = self.v[x1:x2 + 1, y1:y2 + 1, z1:z2 + 1]
        if keep:
            box[box == 0] = i
        else:
            box[...] = i

    def sphere(self, cx, cy, cz, r, s, keep=False, squash=1.0):
        i = self.id(s)
        r0 = int(math.ceil(r)) + 1
        for x in range(int(cx) - r0, int(cx) + r0 + 1):
            for z in range(int(cz) - r0, int(cz) + r0 + 1):
                for y in range(int(cy - r0 * squash) - 1, int(cy + r0 * squash) + 2):
                    if not self.inb(x, y, z):
                        continue
                    d = math.sqrt((x - cx) ** 2 + ((y - cy) / squash) ** 2 + (z - cz) ** 2)
                    if d <= r + 0.3 and (not keep or self.v[x, y, z] == 0):
                        self.v[x, y, z] = i

    def top(self, x, z):
        """Highest non-air y in a column (-1 if empty)."""
        col = np.nonzero(self.v[x, :, z])[0]
        return int(col[-1]) if len(col) else -1

    # --- vegetation -------------------------------------------------------------------------------------------------
    def leaves(self, soort):
        return state(f"{soort}_leaves", distance=1, persistent="true", waterlogged="false")

    def log(self, soort, axis="y"):
        return state(f"{soort}_log", axis=axis)

    def blob(self, cx, cy, cz, rx, ry, rz, s, gat=0.0):
        r = self.rng
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1.0 and (gat == 0 or r.random() > gat * d):
                        self.set(x, y, z, s, keep=True)

    def boom(self, soort, x, y, z, grootte=1.0):
        """A tree standing on the ground block at y (trunk starts at y+1). soort: eik, berk, spar, acacia, jungle,
        mangrove, kers, eucalyptus, baobab, wilg, dode. Returns the top y."""
        r = self.rng
        g = grootte
        if soort == "acacia":
            h = int(4 + 2 * g + r.integers(0, 2))
            dx, dz = [(1, 0), (-1, 0), (0, 1), (0, -1)][r.integers(0, 4)]
            cx, cz = x, z
            for i in range(1, h + 1):
                if i == h - 2:
                    self.set(cx + dx, y + i - 1, cz + dz, self.log("acacia"))   # the bend: no diagonal-only joint
                    cx, cz = cx + dx, cz + dz
                self.set(cx, y + i, cz, self.log("acacia"))
            top = y + h
            rr = 3 + g
            for ax in range(int(cx - rr - 1), int(cx + rr + 2)):
                for az in range(int(cz - rr - 1), int(cz + rr + 2)):
                    d = math.hypot(ax - cx, az - cz)
                    if d <= rr:
                        self.set(ax, top + 1, az, self.leaves("acacia"), keep=True)
                    if d <= rr - 1.6:
                        self.set(ax, top + 2, az, self.leaves("acacia"), keep=True)
            return top + 2
        if soort == "baobab":
            h = int(7 + 3 * g)
            for i in range(1, h + 1):
                for ax in range(x - 1, x + 2):
                    for az in range(z - 1, z + 2):
                        if i < h - 1 or (ax == x or az == z):
                            self.set(ax, y + i, az, self.log("jungle") if (ax, az) != (x, z) else "minecraft:jungle_wood")
            for k in range(5):
                a = k * 2 * math.pi / 5 + r.random()
                bx, bz = x + round(math.cos(a) * 3), z + round(math.sin(a) * 3)
                self.set(bx, y + h, bz, self.log("jungle"))
                self.blob(bx, y + h + 1, bz, 2.4, 1.2, 2.4, self.leaves("acacia"))
            return y + h + 2
        if soort in ("jungle", "reus"):
            h = int((9 if soort == "jungle" else 16) + 4 * g + r.integers(0, 4))
            dik = 2 if soort == "reus" or g > 1.2 else 1
            for i in range(1, h + 1):
                for ax in range(x, x + dik):
                    for az in range(z, z + dik):
                        self.set(ax, y + i, az, self.log("jungle"))
            rr = 3.2 + g + (1.5 if dik == 2 else 0)
            self.blob(x + dik / 2 - 0.5, y + h, z + dik / 2 - 0.5, rr, 2.2, rr, self.leaves("jungle"), gat=0.25)
            for k in range(int(4 + 3 * g)):                       # vines hanging from the canopy
                a = r.random() * 2 * math.pi
                vx, vz = x + round(math.cos(a) * (rr - 0.5)), z + round(math.sin(a) * (rr - 0.5))
                for d in range(int(r.integers(2, 6))):
                    yy = y + h - 2 - d
                    if self.leeg(vx, yy, vz) and not self.leeg(vx, yy + 1, vz):
                        self.set(vx, yy, vz, state("vine", up="true", north="false", east="false", south="false",
                                                   west="false"))
                    elif self.leeg(vx, yy, vz):
                        break
            return y + h + 2
        if soort == "mangrove":
            h = int(5 + 2 * g)
            for (ax, az) in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
                for i in range(0, 3):
                    self.set(ax, y + i, az, state("mangrove_roots", waterlogged="false"), keep=True)
            for i in range(1, h + 1):
                self.set(x, y + i, z, self.log("mangrove"))
            self.blob(x, y + h, z, 3 + g, 2, 3 + g, self.leaves("mangrove"), gat=0.2)
            return y + h + 2
        if soort in ("spar", "den"):
            h = int(7 + 4 * g + r.integers(0, 3))
            for i in range(1, h + 1):
                self.set(x, y + i, z, self.log("spruce"))
            for i in range(3, h + 2):
                rr = max(0.6, (h + 2 - i) * 0.38 * (1 + 0.2 * g)) if (i % 2 == 0 or i > h - 1) else \
                    max(0.4, (h + 2 - i) * 0.25)
                for ax in range(int(x - rr - 1), int(x + rr + 2)):
                    for az in range(int(z - rr - 1), int(z + rr + 2)):
                        if math.hypot(ax - x, az - z) <= rr:
                            self.set(ax, y + i, az, self.leaves("spruce"), keep=True)
            self.set(x, y + h + 2, z, self.leaves("spruce"), keep=True)
            return y + h + 2
        if soort == "eucalyptus":
            h = int(9 + 3 * g)
            for i in range(1, h + 1):
                self.set(x, y + i, z, self.log("birch") if i < h - 3 else "minecraft:stripped_birch_log[axis=y]")
            self.blob(x, y + h, z, 2.5 + g, 2.6, 2.5 + g, self.leaves("azalea"), gat=0.3)
            self.blob(x + 1, y + h - 3, z, 1.6, 1.2, 1.6, self.leaves("azalea"), gat=0.2)
            return y + h + 2
        if soort == "dode":
            h = int(3 + 2 * g)
            for i in range(1, h + 1):
                self.set(x, y + i, z, self.log("spruce"))
            self.set(x + 1, y + h, z, self.log("spruce", "x"))
            self.set(x - 1, y + h - 1, z, self.log("spruce", "x"))
            return y + h
        # loofbomen: eik, berk, kers, wilg
        hout = {"eik": "oak", "berk": "birch", "kers": "cherry", "wilg": "oak"}[soort]
        blad = {"eik": "oak", "berk": "birch", "kers": "cherry", "wilg": "dark_oak"}[soort]
        h = int((4 if soort != "berk" else 6) + 2 * g + r.integers(0, 2))
        for i in range(1, h + 1):
            self.set(x, y + i, z, self.log(hout))
        rr = 2.4 + g * (1.4 if soort == "eik" else 1.0)
        self.blob(x, y + h, z, rr, 2.0 + 0.4 * g, rr, self.leaves(blad), gat=0.15)
        if soort == "wilg":
            for k in range(10):
                a = r.random() * 2 * math.pi
                vx, vz = x + round(math.cos(a) * rr), z + round(math.sin(a) * rr)
                for d in range(int(r.integers(2, 5))):
                    if self.leeg(vx, y + h - 1 - d, vz):
                        self.set(vx, y + h - 1 - d, vz, self.leaves(blad))
        return y + h + 2

    # --- fences -----------------------------------------------------------------------------------------------------
    def fix_connections(self):
        """Every fence, wall, pane, bar and gate gets the connections the game computes (controle.verbindingen)."""
        from . import controle
        controle.verbindingen(self, True)

    # --- export ------------------------------------------------------------------------------------------------------
    def palette_nbt(self, ids):
        """Palette compound list payload for the given ids (in that order)."""
        out = []
        for i in ids:
            name, props = split_state(self.pal[i])
            body = _str_tag("Name", name)
            if props:
                pb = b"".join(_str_tag(k, v) for k, v in sorted(props.items()))
                body += b"\x0a" + _name("Properties") + pb + b"\x00"
            out.append(body + b"\x00")
        return b"\x0a" + struct.pack(">i", len(out)) + b"".join(out)

    def schrijf_tegel(self, tx, tz, root):
        x0, z0 = tx * TEGEL, tz * TEGEL
        box = self.v[x0:x0 + TEGEL, :, z0:z0 + TEGEL]
        xs, ys, zs = np.nonzero(box)
        ids = box[xs, ys, zs]
        uniq, inv = np.unique(ids, return_inverse=True)
        # blocks with a block entity (sign texts) get their own compound with an "nbt" tag, after the plain ones
        be = {(x - x0, y, z - z0): payload for (x, y, z), payload in getattr(self, "block_nbt", {}).items()
              if x0 <= x < x0 + TEGEL and z0 <= z < z0 + TEGEL}
        extra = b""
        n_be = 0
        if be:
            sleutel = xs.astype(np.int64) * 1_000_000 + ys.astype(np.int64) * 1000 + zs.astype(np.int64)
            gezocht = np.array([a * 1_000_000 + b * 1000 + c for a, b, c in be], dtype=np.int64)
            treffers = np.nonzero(np.isin(sleutel, gezocht))[0]
            plain = np.ones(len(xs), dtype=bool)
            for i in treffers:
                plain[i] = False
                p = (int(xs[i]), int(ys[i]), int(zs[i]))
                extra += (b"\x09" + _name("pos") + b"\x03" + struct.pack(">iiii", 3, *p)
                          + b"\x03" + _name("state") + struct.pack(">i", int(inv[i]))
                          + b"\x0a" + _name("nbt") + be[p] + b"\x00"
                          + b"\x00")
                n_be += 1
            xs, ys, zs, inv = xs[plain], ys[plain], zs[plain], inv[plain]
        n = len(xs)
        # every block compound has the same 36-byte layout: pos (list of 3 ints) + state (int) + end
        rec = np.zeros(n, dtype=[("t1", "u1"), ("l1", ">u2"), ("n1", "S3"), ("lt", "u1"), ("ln", ">i4"),
                                 ("x", ">i4"), ("y", ">i4"), ("z", ">i4"),
                                 ("t2", "u1"), ("l2", ">u2"), ("n2", "S5"), ("s", ">i4"), ("end", "u1")])
        rec["t1"], rec["l1"], rec["n1"], rec["lt"], rec["ln"] = 9, 3, b"pos", 3, 3
        rec["x"], rec["y"], rec["z"] = xs, ys, zs
        rec["t2"], rec["l2"], rec["n2"], rec["s"] = 3, 5, b"state", inv
        blocks = b"\x0a" + struct.pack(">i", n + n_be) + rec.tobytes() + extra
        body = (b"\x03" + _name("DataVersion") + struct.pack(">i", DATA_VERSION)
                + b"\x09" + _name("size") + b"\x03" + struct.pack(">iiii", 3, TEGEL, H, TEGEL)
                + b"\x09" + _name("palette") + self.palette_nbt(uniq)
                + b"\x09" + _name("blocks") + blocks
                + b"\x09" + _name("entities") + b"\x0a" + struct.pack(">i", 0))
        data = b"\x0a" + _name("") + body + b"\x00"
        path = os.path.join(root, OUT_STRUCT, f"park_{tx}_{tz}.nbt")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(gzip.compress(data, 6, mtime=0))   # mtime 0: identical park = identical files (no git churn)
        # fences, bars, panes, walls: their connections from the tile's own neighbours (tools/verbindingen.py; a side on
        # the tile's edge keeps what was written)
        import sys
        tools = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
        if tools not in sys.path:
            sys.path.insert(0, tools)
        import hekjes
        hekjes.herstel_bestand(path)
        return path, n

    def bord_nbt(self, x, y, z, regels):
        """Sign text (front and back) for the sign at x,y,z: max 4 lines, as plain strings (checked in game on 26.1.2)."""
        regels = (list(regels) + ["", "", "", ""])[:4]

        def kant(lines):
            msgs = b"".join(_strval(t) for t in lines)   # plain strings: in 26.1 a string IS a literal text component (the DataFixer left JSON strings as raw text)
            return (b"\x09" + _name("messages") + b"\x08" + struct.pack(">i", 4) + msgs
                    + _str_tag("color", "black") + b"\x01" + _name("has_glowing_text") + b"\x00" + b"\x00")
        payload = (b"\x0a" + _name("front_text") + kant(regels) + b"\x0a" + _name("back_text") + kant(regels)
                   + b"\x01" + _name("is_waxed") + b"\x01")
        if not hasattr(self, "block_nbt"):
            self.block_nbt = {}
        self.block_nbt[(int(x), int(y), int(z))] = payload

    def infobord_nbt(self, x, y, z, soort):
        """The species (a soorten.json id) of the guhs:infobord at x,y,z: clicking it shows that species' Guhdex text."""
        if not hasattr(self, "block_nbt"):
            self.block_nbt = {}
        self.block_nbt[(int(x), int(y), int(z))] = _str_tag("Soort", soort)

    def schrijf(self, root, log=print):
        """Check + repair (controle.herstel: fences, leaves, support, water, holes...) and write all tiles."""
        from . import controle
        for b in getattr(self, "bordjes", []):
            self.bord_nbt(*b["pos"], b["tekst"])
        controle.herstel(self, log)
        stats = []
        for tx in range(TEGELS):
            for tz in range(TEGELS):
                stats.append(self.schrijf_tegel(tx, tz, root))
        return stats


STOEL = "guhs:guh_stoel"
_KIJK = {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}


def stoel(park, x, y, z, kijk):
    """A guh chair (guhs:guh_stoel) to sit on, looking toward `kijk` ("north".. or a (dx, dz) unit step): the chair's
    facing is the way the sitter looks (its backrest is on the other side)."""
    if not isinstance(kijk, str):
        kijk = _KIJK[(int((kijk[0] > 0) - (kijk[0] < 0)), int((kijk[1] > 0) - (kijk[1] < 0)))]
    park.set(x, y, z, STOEL, facing=kijk)
    if hasattr(park, "stoelen"):
        park.stoelen.append((int(x), int(y), int(z), kijk))


def toren_kijkplek(park, x, z, dek, naam=None):
    """A lookout tower deck (standing on dek + 1) as an all-round viewpoint (round 3: every tower registers itself)."""
    from .terrein import ZONES
    zi = int(park.zone_vol[int(x), int(z)]) if getattr(park, "zone_vol", None) is not None else -1
    zone = ZONES[zi] if zi >= 0 else "?"
    park.kijkplek(naam or f"toren_{zone}_{int(x)}_{int(z)}", zone, (x + 0.5, dek + 2.6, z + 0.5), None,
                  (int(x), int(dek) + 1, int(z)))


# ======================================================================================================================
# round 3: the ONE bridge builder (every route / path crossing and every zone's own bridge goes through here)
BRUG_STIJL = {   # zone -> (deck block, slab, fence, post log)
    "hout": ("spruce_planks", "spruce_slab", "spruce_fence", "spruce_log"),
    "savanne": ("acacia_planks", "acacia_slab", "acacia_fence", "acacia_log"),
    "australie": ("acacia_planks", "acacia_slab", "acacia_fence", "stripped_acacia_log"),
    "delta": ("mangrove_planks", "mangrove_slab", "mangrove_fence", "mangrove_log"),
    "regenwoud": ("jungle_planks", "jungle_slab", "jungle_fence", "jungle_log"),
    "bamboe": ("bamboe_planks", "bamboo_slab", "bamboo_fence", "stripped_bamboo_block"),
    "ijskust": ("spruce_planks", "spruce_slab", "spruce_fence", "stripped_spruce_log"),
    "oertijd": ("stone_bricks", "stone_brick_slab", "stone_brick_wall", "cobblestone"),
    "europa": ("stone_bricks", "stone_brick_slab", "stone_brick_wall", "mossy_stone_bricks"),
    "entree": ("dark_oak_planks", "dark_oak_slab", "dark_oak_fence", "dark_oak_log"),
}


def _stijl(stijl):
    d = BRUG_STIJL.get(stijl, BRUG_STIJL["hout"])
    plank = d[0].replace("bamboe_planks", "bamboo_planks")
    return plank, d[1], d[2], d[3]


def brug_cellen(park, cellen, stijl="hout", lantaarn_elke=6):
    """Build a bridge over the given deck cells [(x, z)] (path cells over water): the deck at BRUG_Y (2 free blocks
    above the water), railings on the water cells beside the deck (an edge block + fence/wall), posts only at the
    banks, lanterns on the railing every few cells. Idempotent (a second call rebuilds the same)."""
    plank, slab, hek, post = _stijl(stijl)
    cellen = set((int(x), int(z)) for x, z in cellen)
    if not cellen:
        return
    paal = state(post, axis="y") if post.endswith(("_log", "_block")) else post
    is_muur = hek.endswith("_wall")
    for x, z in cellen:
        park.set(x, BRUG_Y, z, plank)
        for y in range(WATER_Y + 1, BRUG_Y):
            park.v[x, y, z] = 0
        for y in range(BRUG_Y + 1, BRUG_Y + 5):
            park.v[x, y, z] = 0
        if getattr(park, "hoogte", None) is not None:
            pass                                   # (the column stays water: park.hoogte keeps the bed)
    rand = set()
    for x, z in cellen:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = (x + dx, z + dz)
                if c in cellen or not (0 <= c[0] < W and 0 <= c[1] < W):
                    continue
                if getattr(park, "pad", None) is not None and park.pad[c]:
                    continue
                if park.water is not None and not park.water[c]:
                    continue
                rand.add(c)
    for i, (x, z) in enumerate(sorted(rand)):
        park.set(x, BRUG_Y, z, plank)
        for y in range(WATER_Y + 1, BRUG_Y):
            park.v[x, y, z] = 0
        if is_muur:
            park.set(x, BRUG_Y + 1, z, hek, up="true", north="none", south="none", east="none", west="none",
                     waterlogged="false")
        else:
            park.set(x, BRUG_Y + 1, z, hek, north="false", south="false", east="false", west="false",
                     waterlogged="false")
        bij_oever = any(not park.water[min(max(x + a, 0), W - 1), min(max(z + b, 0), W - 1)]
                        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if bij_oever:
            bed = int(park.hoogte[x, z]) if getattr(park, "hoogte", None) is not None else WATER_Y - 3
            for y in range(bed + 1, BRUG_Y):
                park.set(x, y, z, paal)
        if lantaarn_elke and (x * 7 + z * 13) % lantaarn_elke == 0:
            park.set(x, BRUG_Y + 2, z, "minecraft:lantern", hanging="false", waterlogged="false")
            park.lampen.append((x, BRUG_Y + 2, z, "lantaarn")) if hasattr(park, "lampen") else None
    xs = [c[0] for c in cellen]
    zs = [c[1] for c in cellen]
    from .terrein import ZONES
    cx, cz = int(sum(xs) / len(xs)), int(sum(zs) / len(zs))
    zv = getattr(park, "zone_vol", None)
    park.bruggen.append((cx, cz, ZONES[int(zv[cx, cz])] if zv is not None else stijl))


def brug(park, a, b, stijl="hout", breed=3, oprit=6):
    """A zone's own straight bridge between bank points a=(x, z) and b=(x, z): the cells over water become deck
    (brug_cellen), the path cells beyond both ends climb to it as a gentle ramp of slabs (half a block per block).
    The cells are marked as path (park.pad)."""
    plank, slab, _, _ = _stijl(stijl)
    ax, az = a
    bx, bz = b
    lengte = max(1.0, math.hypot(bx - ax, bz - az))
    ux, uz = (bx - ax) / lengte, (bz - az) / lengte
    half = breed // 2
    dek = set()
    oprit_cellen = {}
    for i in range(-oprit * 2, int(lengte * 2) + oprit * 2 + 1):
        t = i / 2
        fx, fz = ax + ux * t, az + uz * t
        for k in range(-half, half + 1):
            x, z = int(round(fx - uz * k)), int(round(fz + ux * k))
            if not (0 <= x < W and 0 <= z < W):
                continue
            if park.water[x, z]:
                dek.add((x, z))
            else:
                d = -t if t < 0 else (t - lengte if t > lengte else 0)
                oprit_cellen[(x, z)] = min(oprit_cellen.get((x, z), 1e9), d)
            if getattr(park, "pad", None) is not None:
                park.pad[x, z] = True
    brug_cellen(park, dek, stijl)
    for (x, z), d in oprit_cellen.items():
        doel2 = 2 * (BRUG_Y + 1) - int(math.floor(d * 2 + 1))        # walking height in half blocks
        g = int(park.hoogte[x, z])
        if doel2 <= 2 * (g + 1):
            continue
        top_vol = doel2 // 2 - 1
        for y in range(g + 1, top_vol + 1):
            park.set(x, y, z, plank)
        if doel2 % 2:
            park.set(x, top_vol + 1, z, slab, type="bottom", waterlogged="false")
        for y in range(top_vol + 1 + (doel2 % 2), top_vol + 5):
            park.v[x, y, z] = 0


def _strval(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _name(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b


def _str_tag(k, v):
    vb = str(v).encode()
    return b"\x08" + _name(k) + struct.pack(">H", len(vb)) + vb


# --- noise ------------------------------------------------------------------------------------------------------------
def ruis(rng, schaal, n=W):
    from scipy.ndimage import gaussian_filter
    a = rng.standard_normal((n, n))
    a = gaussian_filter(a, schaal, mode="wrap")
    return a / (a.std() + 1e-9)
