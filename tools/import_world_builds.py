"""
Copies hand-made builds out of a Minecraft world save into structure templates (data/guhs/structure/<name>.nbt),
so they can generate in the Guhmension. Works with newer worlds too (the "Guh structures" world is 26.2): it reads
the region files directly and only keeps block names/properties that 1.21.1 understands.

Run from the project root:  python tools/import_world_builds.py ["run/saves/Guh structures"]
The world is superflat (ground = grass at y-61), so layer y-61 becomes y0 of each template.
"""
import glob
import gzip
import json
import math
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_structures as ms  # noqa: E402

WORLD = os.path.join("run", "saves", "Guh structures")
GROUND_Y = -61

# name: (min x, min z), (max x, top y, max z) in world coordinates
# (a third value True = "grounded": drop the empty rows under the build, so it stands on the ground)
BUILDS = {
    "mini_picnic": ((-6, -6), (-2, -59, -2)),
    "block_guh": ((8, -28), (15, -53, -21)),
    "giant_kaasknabbel": ((-36, 18), (-12, -47, 28)),
    "kaasknabbel_arch": ((-42, -29), (-2, -25, -21), True),   # floated 7 blocks up in the world
    "giant_cake": ((16, 18), (32, -51, 34)),
    "quartz_statue": ((3, 43), (6, -54, 46)),
    "guh_fossil": ((-31, 62), (-14, -56, 68)),
}
# blocks that don't exist in 1.21.1 yet
REPLACE = {"minecraft:pale_oak_fence": "minecraft:birch_fence"}
# under the builds the grass turned into dirt: leave the Guhmension's own ground there instead
SKIP_AT_GROUND = {"minecraft:dirt", "minecraft:grass_block"}


def parse(d):
    pos = 0

    def rd(f):
        nonlocal pos
        v = struct.unpack_from(f, d, pos)
        pos += struct.calcsize(f)
        return v[0]

    def rs():
        nonlocal pos
        n = rd(">H")
        v = d[pos:pos + n].decode("utf-8", "replace")
        pos += n
        return v

    def pl(t):
        nonlocal pos
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 7:
            n = rd(">i"); v = d[pos:pos + n]; pos += n; return v
        if t == 8: return rs()
        if t == 9:
            et = rd(">b"); n = rd(">i"); return [pl(et) for _ in range(n)]
        if t == 10:
            o = {}
            while True:
                tt = rd(">b")
                if tt == 0: return o
                k = rs(); o[k] = pl(tt)
        if t == 11:
            n = rd(">i"); v = struct.unpack_from(f">{n}i", d, pos); pos += 4 * n; return v
        if t == 12:
            n = rd(">i"); v = struct.unpack_from(f">{n}q", d, pos); pos += 8 * n; return v
        raise ValueError(t)
    t = rd(">b"); rs(); return pl(t)


def unpack(longs, bits, count):
    per = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for l in longs:
        l &= (1 << 64) - 1
        for i in range(per):
            out.append((l >> (i * bits)) & mask)
            if len(out) == count:
                return out
    return out


def read_world(world):
    """(x, y, z) -> (name, props) for every non-air block, plus the block entities."""
    region_dir = os.path.join(world, "dimensions", "minecraft", "overworld", "region")   # 26.x layout
    if not os.path.isdir(region_dir):
        region_dir = os.path.join(world, "region")                                         # 1.21 layout
    blocks, entities = {}, {}
    for f in glob.glob(os.path.join(region_dir, "r.*.mca")):
        raw = open(f, "rb").read()
        for i in range(1024):
            off = int.from_bytes(raw[i * 4:i * 4 + 3], "big")
            if not off:
                continue
            ln = struct.unpack_from(">i", raw, off * 4096)[0]
            c = parse(zlib.decompress(raw[off * 4096 + 5:off * 4096 + 4 + ln]))
            cx, cz = c["xPos"], c["zPos"]
            for sec in c["sections"]:
                pal = sec.get("block_states", {}).get("palette", [])
                if not pal or (len(pal) == 1 and pal[0]["Name"] == "minecraft:air"):
                    continue
                data = sec["block_states"].get("data")
                ids = [0] * 4096 if len(pal) == 1 else unpack(data, max(4, math.ceil(math.log2(len(pal)))), 4096)
                for idx, b in enumerate(ids):
                    p = pal[b]
                    if p["Name"] != "minecraft:air":
                        blocks[(cx * 16 + idx % 16, sec["Y"] * 16 + idx // 256, cz * 16 + (idx // 16) % 16)] = \
                            (p["Name"], p.get("Properties", {}))
            for be in c.get("block_entities", []):
                entities[(be["x"], be["y"], be["z"])] = be
    return blocks, entities


def block_entity(be, build=None):
    """Only the parts 1.21.1 reads the same way: the id, and the text of signs (1.2.0: lines listed in
    sign_text.IMPORTED become translate keys with the Dutch as fallback; the rest stays literal)."""
    import sign_text
    nbt = {"id": be["id"]}
    for side in ("front_text", "back_text"):
        if side in be:
            text = be[side]
            nbt[side] = {"messages": ms.NbtList(8, [sign_text.imported_message(build, m if isinstance(m, str) else "")
                                                    for m in text["messages"]]),
                         "color": text.get("color", "black"), "has_glowing_text": ms.Byte(text.get("has_glowing_text", 0))}
    if "is_waxed" in be:
        nbt["is_waxed"] = ms.Byte(be["is_waxed"])
    return nbt


def main(world=WORLD):
    blocks, entities = read_world(world)
    for name, ((x0, z0), (x1, y1, z1), *grounded) in BUILDS.items():
        base = GROUND_Y
        if grounded and grounded[0]:
            base = min(y for (x, y, z), b in blocks.items() if x0 <= x <= x1 and z0 <= z <= z1 and GROUND_Y < y <= y1)
        s = ms.Structure((x1 - x0 + 1, y1 - base + 1, z1 - z0 + 1))
        for x in range(x0, x1 + 1):
            for y in range(base, y1 + 1):
                for z in range(z0, z1 + 1):
                    b = blocks.get((x, y, z))
                    if not b or (y == GROUND_Y and b[0] in SKIP_AT_GROUND):
                        continue
                    be = entities.get((x, y, z))
                    s.set(x - x0, y - base, z - z0, REPLACE.get(b[0], b[0]), b[1], block_entity(be, name) if be else None)
        # air in every empty spot above the ground inside the build's box, so hills can't poke through it
        s.clear_above([(x, z) for x in range(s.size[0]) for z in range(s.size[2])], 1)
        s.save(name)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else WORLD)
