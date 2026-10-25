"""
Fences, walls, panes, bars, fence gates and garlands (vlaggetjes) in the structure templates: do their stored connections
match their neighbours in the template? (A jigsaw piece is placed with a known shape: what is stored is what you see.)

The rule is tools/verbindingen.py (make_structures.Structure.save applies it to every template it writes). This script is for
the .nbt files themselves, also the hand-made / copied ones without a generator:

    python tools/hekjes.py                 scan: per template the number of blocks whose stored state differs (exit 1 if any)
    python tools/hekjes.py --fix           rewrite only those connection properties in place (north/east/south/west/up/
                                           in_wall, a garland's axis); every other byte of the template stays the same
    python tools/hekjes.py --fix PAD ...   only these .nbt files (or folders)
    -v                                     list every differing block

(from the project root)
"""
import gzip
import io
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verbindingen as vb  # noqa: E402

ROOT = os.path.join("src", "main", "resources", "data", "guhs", "structure")
SLEUTELS = ("north", "east", "south", "west", "up", "in_wall")
STANDAARD = {"fence": "false", "pane": "false", "wall": "none", "up": "true", "in_wall": "false", "axis": "x"}


# --- a small NBT reader / writer that keeps every tag type (a template without changes is written back byte for byte) ---
class Tag:
    __slots__ = ("t", "v")

    def __init__(self, t, v):
        self.t, self.v = t, v


_FMT = {1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}


def _read(f, t):
    if t in _FMT:
        n = struct.calcsize(_FMT[t])
        return struct.unpack(_FMT[t], f.read(n))[0]
    if t == 7:
        return f.read(struct.unpack(">i", f.read(4))[0])
    if t == 8:
        return f.read(struct.unpack(">H", f.read(2))[0])          # (raw bytes: modified UTF-8 stays as it was)
    if t == 9:
        et, n = struct.unpack(">bi", f.read(5))
        return (et, [_read(f, et) for _ in range(n)])
    if t == 10:
        d = {}
        while True:
            ct = f.read(1)[0]
            if ct == 0:
                return d
            name = f.read(struct.unpack(">H", f.read(2))[0])
            d[name] = Tag(ct, _read(f, ct))
    if t == 11:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(f">{n}i", f.read(4 * n)))
    if t == 12:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(f">{n}q", f.read(8 * n)))
    raise ValueError(f"unknown NBT tag {t}")


def _write(f, t, v):
    if t in _FMT:
        f.write(struct.pack(_FMT[t], v))
    elif t == 7:
        f.write(struct.pack(">i", len(v)) + v)
    elif t == 8:
        f.write(struct.pack(">H", len(v)) + v)
    elif t == 9:
        et, items = v
        f.write(struct.pack(">bi", et, len(items)))
        for i in items:
            _write(f, et, i)
    elif t == 10:
        for name, tag in v.items():
            f.write(struct.pack(">bH", tag.t, len(name)) + name)
            _write(f, tag.t, tag.v)
        f.write(b"\x00")
    elif t == 11:
        f.write(struct.pack(f">i{len(v)}i", len(v), *v))
    elif t == 12:
        f.write(struct.pack(f">i{len(v)}q", len(v), *v))
    else:
        raise ValueError(f"unknown NBT tag {t}")


def lees(path):
    raw = open(path, "rb").read()
    data = gzip.decompress(raw) if raw[:2] == b"\x1f\x8b" else raw
    f = io.BytesIO(data)
    t = f.read(1)[0]
    name = f.read(struct.unpack(">H", f.read(2))[0])
    return name, Tag(t, _read(f, t)), data


def bytes_van(name, root):
    f = io.BytesIO()
    f.write(struct.pack(">bH", root.t, len(name)) + name)
    _write(f, root.t, root.v)
    return f.getvalue()


def _s(tag):
    return tag.v.decode("utf-8")


# --- the check ----------------------------------------------------------------------------------------------------------
def _relevant(naam):
    return vb.soort(naam) is not None or naam in vb.SLINGERS


def verschillen(root):
    """[(pos, name, stored props, computed props)] for every block whose connections differ, plus the palette and blocks."""
    c = root.v
    if b"palette" not in c:
        return [], None, None      # (a template with "palettes": none of ours)
    palette = c[b"palette"].v[1]
    blocks = c[b"blocks"].v[1]
    staten = []
    for e in palette:
        naam = _s(e[b"Name"])
        props = {k.decode(): _s(v) for k, v in e[b"Properties"].v.items()} if b"Properties" in e else {}
        staten.append((naam, props))
    if not any(_relevant(n) for n, _ in staten):
        return [], palette, blocks
    wereld = {}
    for b in blocks:
        pos = tuple(b[b"pos"].v[1])
        naam, props = staten[b[b"state"].v]
        nbt = None
        if naam == "minecraft:jigsaw" and b"nbt" in b and b"final_state" in b[b"nbt"].v:
            nbt = {"final_state": _s(b[b"nbt"].v[b"final_state"])}
        wereld[pos] = (naam, dict(props), nbt)
    oud = {p: (n, dict(pr)) for p, (n, pr, _) in wereld.items() if _relevant(n)}
    vb.verbind_blokken(wereld)
    uit = []
    for p, (naam, props) in oud.items():
        nieuw = wereld[p][1]
        s = vb.soort(naam)
        keys = ("axis",) if naam in vb.SLINGERS else SLEUTELS
        diff = {}
        for k in keys:
            if k not in nieuw:
                continue
            std = STANDAARD.get(k) if k in ("up", "in_wall", "axis") else STANDAARD.get(s)
            if nieuw[k] != props.get(k, std):
                diff[k] = nieuw[k]
        if diff:
            uit.append((p, naam, props, diff))
    return uit, palette, blocks


def herstel(root, palette, blocks, diffs):
    """Point the differing blocks at a palette entry with the computed connections (a new entry when there is none yet)."""
    index = {}
    for i, e in enumerate(palette):
        naam = _s(e[b"Name"])
        props = tuple(sorted((k.decode(), _s(v)) for k, v in e[b"Properties"].v.items())) if b"Properties" in e else ()
        index.setdefault((naam, props), i)
    per_pos = {p: diff for p, _, _, diff in diffs}
    for b in blocks:
        pos = tuple(b[b"pos"].v[1])
        if pos not in per_pos:
            continue
        e = palette[b[b"state"].v]
        props = dict(e[b"Properties"].v) if b"Properties" in e else {}
        for k, v in per_pos[pos].items():
            props[k.encode()] = Tag(8, v.encode())
        sleutel = (_s(e[b"Name"]), tuple(sorted((k.decode(), _s(v)) for k, v in props.items())))
        if sleutel not in index:
            nieuw = {b"Name": e[b"Name"]}
            for k, v in e.items():
                if k not in (b"Name", b"Properties"):
                    nieuw[k] = v
            nieuw[b"Properties"] = Tag(10, props)
            palette.append(nieuw)
            index[sleutel] = len(palette) - 1
        b[b"state"] = Tag(3, index[sleutel])


def bestanden(paden):
    for p in paden:
        if os.path.isdir(p):
            for d, _, fs in os.walk(p):
                for f in sorted(fs):
                    if f.endswith(".nbt"):
                        yield os.path.join(d, f)
        else:
            yield p


def groep(pad):
    rel = os.path.relpath(pad, ROOT).replace(os.sep, "/")
    return rel.split("/")[0] if "/" in rel else "(top)"


def herstel_bestand(pad, fix=True):
    """Scan (and with fix: repair in place) one .nbt; returns the differing blocks [(pos, name, stored, changed props)]."""
    name, root, _ = lees(pad)
    diffs, palette, blocks = verschillen(root)
    if diffs and fix:
        herstel(root, palette, blocks, diffs)
        with open(pad, "wb") as f:
            f.write(gzip.compress(bytes_van(name, root), mtime=0))
    return diffs


def main(args):
    fix = "--fix" in args
    verbose = "-v" in args
    paden = [a for a in args if not a.startswith("-")] or [ROOT]
    totaal, per_groep, per_bestand = 0, {}, []
    for pad in sorted(bestanden(paden)):
        diffs = herstel_bestand(pad, fix)
        if diffs:
            per_bestand.append((pad, len(diffs)))
            totaal += len(diffs)
            g = groep(pad)
            per_groep[g] = per_groep.get(g, 0) + len(diffs)
            if verbose:
                for p, naam, props, diff in sorted(diffs):
                    print(f"  {pad} {p} {naam} {props} -> {diff}")
    for pad, n in per_bestand:
        print(f"{n:6d}  {os.path.relpath(pad, ROOT)}")
    print("per group:", ", ".join(f"{g} {n}" for g, n in sorted(per_groep.items(), key=lambda x: -x[1])) or "-")
    print(f"{'fixed' if fix else 'differing'}: {totaal} blocks in {len(per_bestand)} templates")
    return 1 if totaal and not fix else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
