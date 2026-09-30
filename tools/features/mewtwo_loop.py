"""
1.0.0 fix round: the walkability check of the kloon-eiland (mewtwo_bouw.check calls it on the finished template).

A player is 1.8 blocks tall, walks up 0.6 without jumping (stairs, slabs, half steps) and jumps at most one full block, but
only with room above: a jump from floor f needs the air up to f + 1 + 1.8 over the spot it jumps from. From the start (the
steiger, where the Reisguh drops you) a flood fill over the standing spots (column x/z, feet height f in half blocks) walks to
the four neighbours when the step fits:
  - up to 0.5: always (both columns free from the lower feet to the higher feet + 1.8);
  - up to 1.0: a jump (the start column free up to f1 + 1 + 1.8, the target column free up to f2 + 1.8);
  - down: at most 3 blocks (no fall damage), the target column free from its feet up to the start's feet + 1.8.
reachable(...) returns the reached standing spots; spot_reached(...) says whether a quest spot (a block you right-click) has
a reached standing spot next to it (or one right on top of it for a floor block).
Block shapes: the template's block names/properties (air, water and thin decorations are passable; bottom slabs, bottom
stairs and beds are half-height floors; fences, walls, panes, lanterns and other small blocks are obstacles you can't stand in).
"""
from collections import deque

PASSABLE = ("carpet", "flower", "bloem", "short_grass", "tall_grass", "seagrass", "button", "papieren", "coral_fan", "_coral",
            "allium", "water", "sapling", "plekje")
# (solid enough to bump into, too thin or odd-shaped to stand on as a floor)
OBSTACLE = ("fence", "_wall", "pane", "lantern", "lampion", "chain", "rod", "potted", "brewing", "notitieplek", "onderdelenkist",
            "computer", "reageerbuis", "knabbelschaal", "lectern", "cauldron", "campfire", "tankwand", "kloontank", "trapdoor",
            "leaves", "jigsaw_never")


def shape(block):
    """(kind, top): kind "air" (passable), "floor" (stand on it at y + top) or "obstacle"."""
    if block is None:
        return "air", 0.0
    name, props = block[0], block[1] or {}
    kort = name.split(":")[-1]
    if name == "minecraft:air" or kort in ("cave_air", "void_air") or any(t in kort for t in PASSABLE) and "block" not in kort:
        return "air", 0.0
    if any(t in kort for t in OBSTACLE):
        return "obstacle", 1.5
    if kort.endswith("_slab"):
        return "floor", 1.0 if props.get("type") in ("top", "double") else 0.5
    if kort.endswith("_stairs"):
        return "floor", 1.0 if props.get("half") == "top" else 0.5
    if kort.endswith("_bed"):
        return "floor", 0.5
    return "floor", 1.0


class Loop:
    def __init__(self, s, y0=0):
        """s: a make_structures.Structure; y0: the world y of template y 0 (spots are given and returned in world y)."""
        self.s = s
        self.y0 = y0
        self._cache = {}

    def _shape(self, x, y, z):
        key = (x, y, z)
        v = self._cache.get(key)
        if v is None:
            v = shape(self.s.blocks.get((x, y - self.y0, z)))
            self._cache[key] = v
        return v

    def vrij(self, x, z, van, tot):
        """Is column (x, z) free of floors and obstacles from height van up to tot (world y, floats)? A half-height floor
        (a bottom slab or stair) counts only as high as its top."""
        y = int(van // 1)
        while y < tot - 1e-6:
            kind, top = self._shape(x, y, z)
            if kind == "obstacle" or kind == "floor" and y + top > van + 1e-6:
                return False
            y += 1
        return True

    def feet(self, x, z, y):
        """The feet heights when standing on the block at (x, y, z): [] (no floor there, or no room above it), one height, or
        for a stair both its low front (y + 0.5) and its high back (y + 1): a stair is walked up half a block at a time."""
        kind, top = self._shape(x, y, z)
        if kind != "floor":
            return []
        name = (self.s.blocks.get((x, y - self.y0, z)) or ("",))[0]
        tops = [0.5, 1.0] if name.endswith("_stairs") and top == 0.5 else [top]
        out = []
        for t in tops:
            f = y + t
            if t < 1.0 and name.endswith("_stairs"):
                ok = self.vrij(x, z, y + 1, f + 1.8)
            else:
                ok = self.vrij(x, z, f, f + 1.8)
            if ok:
                out.append(f)
        return out

    def standing(self, x, z, near, span=4):
        """The standing feet heights of column (x, z) around height near."""
        out = []
        for y in range(int(near) - span - 1, int(near) + span + 1):
            out.extend(self.feet(x, z, y))
        return out

    def stap(self, x1, z1, f1, x2, z2, f2, max_up=1.0):
        if (x1, z1) == (x2, z2):
            # (on one stair: from its front half to its back half)
            return abs(f2 - f1) <= 0.5 + 1e-6 and self.vrij(x1, z1, max(f1, f2), max(f1, f2) + 1.8)
        dh = f2 - f1
        if dh > max_up + 1e-6 or dh < -3.0 - 1e-6:
            return False
        if dh <= 0.5 + 1e-6:
            hoog = max(f1, f2) + 1.8
            return self.vrij(x1, z1, f1, hoog) and self.vrij(x2, z2, f2, hoog)
        # a jump: room over the start for the whole jump
        return self.vrij(x1, z1, f1, f1 + dh + 1.8) and self.vrij(x2, z2, f2, f2 + 1.8)

    def reachable(self, start, limit=200000, allowed=None, max_up=1.0):
        """Flood fill from a world spot (x, feet y, z): the set of reached (x, z, feet*2). allowed(x, z): only these columns
        (a route); max_up: the highest step up (0.5: walking only, no jumps)."""
        sx, sy, sz = start
        fs = [f for f in self.standing(sx, sz, sy, 2) if abs(f - sy) <= 1.0]
        if not fs:
            raise ValueError(f"no standing spot at the start {start}")
        f0 = min(fs, key=lambda f: abs(f - sy))
        seen = {(sx, sz, int(f0 * 2))}
        todo = deque([(sx, sz, f0)])
        while todo and len(seen) < limit:
            x, z, f = todo.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (0, 0)):
                nx, nz = x + dx, z + dz
                if allowed is not None and not allowed(nx, nz):
                    continue
                for f2 in self.standing(nx, nz, f, 4):
                    key = (nx, nz, int(f2 * 2))
                    if key in seen or not self.stap(x, z, f, nx, nz, f2, max_up):
                        continue
                    seen.add(key)
                    todo.append((nx, nz, f2))
        return seen

    @staticmethod
    def spot_reached(seen, spot, floor_block=False):
        """A block you right-click at (x, y, z) (world): a reached standing spot next to it (feet within y-1 .. y+0.5), or on
        top of it for a floor block."""
        x, y, z = spot
        for (sx, sz, f2) in seen:
            f = f2 / 2
            if max(abs(sx - x), abs(sz - z)) <= 1 and (sx, sz) != (x, z) and y - 1.5 <= f <= y + 1.0:
                return True
            if floor_block and (sx, sz) == (x, z) and abs(f - (y + 1)) <= 0.5:
                return True
        return False

    @staticmethod
    def feet_at(seen, x, z, f):
        return (x, z, int(f * 2)) in seen
