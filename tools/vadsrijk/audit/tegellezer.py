"""Fast reader for the Vadsrijk map tiles (structure .nbt written by wereld/nbtsnel.py).

The per-block compounds of nbtsnel are fixed 36-byte records, so they are read with numpy in one go; the few blocks with
block-entity nbt (written after them) and any other layout fall back to a tiny generic NBT parser.
"""
import gzip
import os
import re
import struct

import numpy as np

T = 64
HOOG = 320

_BLOK = np.dtype([("t_pos", "u1"), ("n_pos", ">u2"), ("s_pos", "S3"), ("et", "u1"), ("cnt", ">i4"),
                  ("x", ">i4"), ("y", ">i4"), ("z", ">i4"),
                  ("t_st", "u1"), ("n_st", ">u2"), ("s_st", "S5"), ("st", ">i4"), ("end", "u1")])


class _P:
    def __init__(self, data, p=0):
        self.d, self.p = data, p

    def rd(self, fmt):
        v = struct.unpack_from(fmt, self.d, self.p)
        self.p += struct.calcsize(fmt)
        return v[0] if len(v) == 1 else v

    def rstr(self):
        n = self.rd(">H")
        s = self.d[self.p:self.p + n].decode("utf-8")
        self.p += n
        return s

    def payload(self, t):
        if t == 1: return self.rd(">b")
        if t == 2: return self.rd(">h")
        if t == 3: return self.rd(">i")
        if t == 4: return self.rd(">q")
        if t == 5: return self.rd(">f")
        if t == 6: return self.rd(">d")
        if t == 8: return self.rstr()
        if t == 9:
            et = self.rd(">B")
            n = self.rd(">i")
            return [self.payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = self.rd(">B")
                if tt == 0:
                    return d
                k = self.rstr()
                d[k] = self.payload(tt)
        if t == 11:
            n = self.rd(">i")
            v = struct.unpack_from(f">{n}i", self.d, self.p)
            self.p += 4 * n
            return list(v)
        if t == 7:
            n = self.rd(">i"); self.p += n; return None
        if t == 12:
            n = self.rd(">i"); self.p += 8 * n; return None
        raise ValueError(t)


def _state(entry):
    naam = entry["Name"]
    props = entry.get("Properties") or {}
    if props:
        return naam + "[" + ",".join(f"{k}={props[k]}" for k in sorted(props)) + "]"
    return naam


def lees(pad):
    """-> (palette list of state strings, xs, ys, zs, states) as int arrays (local tile coords)."""
    data = gzip.decompress(open(pad, "rb").read())
    P = _P(data)
    assert P.rd(">B") == 10
    P.rstr()
    palette, blokken = None, None
    while True:
        t = P.rd(">B")
        if t == 0:
            break
        k = P.rstr()
        if k == "blocks" and t == 9:
            et = P.rd(">B")
            n = P.rd(">i")
            # fast part: fixed records
            m = min(n, (len(data) - P.p) // 36)
            a = np.frombuffer(data, _BLOK, count=m, offset=P.p) if m else np.zeros(0, _BLOK)
            ok = (a["t_pos"] == 9) & (a["end"] == 0) & (a["t_st"] == 3) & (a["s_st"] == b"state") & (a["cnt"] == 3)
            slecht = np.nonzero(~ok)[0]
            k_snel = int(slecht[0]) if len(slecht) else m
            a = a[:k_snel]
            P.p += 36 * k_snel
            xs, ys, zs, st = [a["x"].astype(np.int32)], [a["y"].astype(np.int32)], [a["z"].astype(np.int32)], [a["st"].astype(np.int32)]
            for _ in range(n - k_snel):
                c = P.payload(et)
                pos = c["pos"]
                xs.append(np.array([pos[0]], np.int32)); ys.append(np.array([pos[1]], np.int32))
                zs.append(np.array([pos[2]], np.int32)); st.append(np.array([c["state"]], np.int32))
            blokken = (np.concatenate(xs), np.concatenate(ys), np.concatenate(zs), np.concatenate(st))
        elif k == "palette" and t == 9:
            et = P.rd(">B")
            n = P.rd(">i")
            palette = [_state(P.payload(et)) for _ in range(n)]
        else:
            P.payload(t)
    return palette, blokken


_NAAM = re.compile(r"t_(-?\d+)_(-?\d+)\.nbt$")


def tegels(map_):
    """Yields (tx, tz, path) for every tile in the folder."""
    for f in sorted(os.listdir(map_)):
        m = _NAAM.match(f)
        if m:
            yield int(m.group(1)), int(m.group(2)), os.path.join(map_, f)
