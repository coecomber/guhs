"""Fast structure-template (.nbt) writer for the Vadsrijk map tiles (CONTRACT_VADSRIJK §10).

A tile has up to ~10^5 blocks, so the per-block compounds are written with numpy in one go instead of a Python NBT tree.
Format = vanilla StructureTemplate: {DataVersion, size, palette, blocks:[{pos:[x,y,z], state:i, nbt?}], entities:[]}.
DataVersion 4790 = 26.1.2, so block names are the 26.1 names (minecraft:iron_chain, ...) and no DataFixer pass runs on load.
"""
import gzip
import struct
import zlib

import numpy as np

DATA_VERSION = 4790


def _str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _named(tag, name):
    return bytes([tag]) + _str(name)


def _compound_kv(d):
    """Tiny generic writer for the palette/nbt (strings, ints, nested dicts, bytes)."""
    out = b""
    for k, v in d.items():
        if isinstance(v, dict):
            out += _named(10, k) + _compound_kv(v) + b"\x00"
        elif isinstance(v, bool):
            out += _named(1, k) + struct.pack(">b", int(v))
        elif isinstance(v, int):
            out += _named(3, k) + struct.pack(">i", v)
        elif isinstance(v, str):
            out += _named(8, k) + _str(v)
        elif isinstance(v, list) and all(isinstance(i, dict) for i in v):
            out += _named(9, k) + bytes([10]) + struct.pack(">i", len(v)) + b"".join(_compound_kv(i) + b"\x00" for i in v)
        else:
            raise TypeError((k, v))
    return out


def _nbt_lijf(v):
    """U-HEKKEN: block-entity nbt as a dict, or as raw compound bytes (body without the end tag, kept as read)."""
    return bytes(v) if isinstance(v, (bytes, bytearray)) else _compound_kv(v)


_BLOK = np.dtype([("t_pos", "u1"), ("n_pos", ">u2"), ("s_pos", "S3"), ("et", "u1"), ("cnt", ">i4"),
                  ("x", ">i4"), ("y", ">i4"), ("z", ">i4"),
                  ("t_st", "u1"), ("n_st", ">u2"), ("s_st", "S5"), ("st", ">i4"), ("end", "u1")])
assert _BLOK.itemsize == 36


def schrijf(pad, size, palette, xs, ys, zs, states, extra_nbt=None, niveau=9):
    """palette: list of (name, props dict). xs/ys/zs/states: int arrays (one entry per block).
    extra_nbt: {(x,y,z): dict} block-entity nbt for a few blocks (written in a slow second list part)."""
    extra_nbt = extra_nbt or {}
    n = len(xs)
    if extra_nbt:
        sleutels = set(extra_nbt)
        mask = np.array([(int(a), int(b), int(c)) not in sleutels for a, b, c in zip(xs, ys, zs)], bool) if n else np.zeros(0, bool)
    else:
        mask = np.ones(n, bool)
    a = np.zeros(int(mask.sum()), _BLOK)
    a["t_pos"], a["n_pos"], a["s_pos"], a["et"], a["cnt"] = 9, 3, b"pos", 3, 3
    a["x"], a["y"], a["z"] = xs[mask], ys[mask], zs[mask]
    a["t_st"], a["n_st"], a["s_st"] = 3, 5, b"state"
    a["st"] = states[mask]
    a["end"] = 0
    # K-INT: y-major order (y, z, x) + gzip level 9: ~13 % smaller tiles. The order is free: vanilla StructureTemplate sorts the
    # blocks itself on load (buildInfoList), and the Python readers do not care.
    a = a[np.lexsort((a["x"], a["z"], a["y"]))]
    slow = b""
    nslow = 0
    for i in np.nonzero(~mask)[0]:
        x, y, z = int(xs[i]), int(ys[i]), int(zs[i])
        slow += (_named(9, "pos") + bytes([3]) + struct.pack(">iiii", 3, x, y, z) + _named(3, "state")
                 + struct.pack(">i", int(states[i])) + _named(10, "nbt") + _nbt_lijf(extra_nbt[(x, y, z)]) + b"\x00" + b"\x00")
        nslow += 1
    pal = b""
    for (name, props) in palette:
        e = {"Name": name}
        if props:
            e["Properties"] = dict(props)
        pal += _compound_kv(e) + b"\x00"
    body = (b"\x0a" + _str("")
            + _named(3, "DataVersion") + struct.pack(">i", DATA_VERSION)
            + _named(9, "size") + bytes([3]) + struct.pack(">iiii", 3, *size)
            + _named(9, "palette") + bytes([10]) + struct.pack(">i", len(palette)) + pal
            + _named(9, "blocks") + bytes([10]) + struct.pack(">i", len(a) + nslow) + a.tobytes() + slow
            + _named(9, "entities") + bytes([10]) + struct.pack(">i", 0)
            + b"\x00")
    with open(pad, "wb") as f:
        f.write(gzip.compress(body, compresslevel=niveau, mtime=0))
    return len(a) + nslow


def lees(pad):
    """Minimal reader (for checks): returns (size, palette names, blocks as (x,y,z,state) array)."""
    data = gzip.decompress(open(pad, "rb").read())
    p = [0]

    def rd(fmt):
        v = struct.unpack_from(fmt, data, p[0])
        p[0] += struct.calcsize(fmt)
        return v[0] if len(v) == 1 else v

    def rstr():
        n = rd(">H")
        s = data[p[0]:p[0] + n].decode()
        p[0] += n
        return s

    def payload(t):
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 8: return rstr()
        if t == 9:
            et = rd(">B"); n = rd(">i")
            return [payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = rd(">B")
                if tt == 0:
                    return d
                k = rstr()
                d[k] = payload(tt)
        if t == 11:
            n = rd(">i"); return [rd(">i") for _ in range(n)]
        if t == 7:
            n = rd(">i"); p[0] += n; return None
        if t == 12:
            n = rd(">i"); p[0] += 8 * n; return None
        raise ValueError(t)
    assert rd(">B") == 10
    rstr()
    root = payload(10)
    return root
