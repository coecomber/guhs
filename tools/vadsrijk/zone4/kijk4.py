"""Debug helper: print a horizontal slice (y) or a vertical slice of the zone-4 tiles around a point (block initials).
    python tools/vadsrijk/zone4/kijk4.py x y z [r] [--v x|z]"""
import sys
sys.path.insert(0, __file__.rsplit("\\", 1)[0] if "\\" in __file__ else __file__.rsplit("/", 1)[0])
import poicheck4 as P

b = P.Lezer()
x, y, z = (int(v) for v in sys.argv[1:4])
r = int(sys.argv[4]) if len(sys.argv) > 4 and not sys.argv[4].startswith("--") else 6
codes = {}


def c(s):
    if s is None:
        return "."
    n = s.split("[")[0].split(":")[1]
    if n not in codes:
        codes[n] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#$%&*+=?"[len(codes) % 70]
    return codes[n]


if "--v" in sys.argv:
    as_ = sys.argv[sys.argv.index("--v") + 1]
    for yy in range(y + r, y - r - 1, -1):
        rij = "".join(c(b(x + d, yy, z) if as_ == "x" else b(x, yy, z + d)) for d in range(-r, r + 1))
        print(f"{yy:4d} {rij}")
else:
    for zz in range(z - r, z + r + 1):
        print(f"{zz:5d} " + "".join(c(b(xx, y, zz)) for xx in range(x - r, x + r + 1)))
print({v: k for k, v in codes.items()})
