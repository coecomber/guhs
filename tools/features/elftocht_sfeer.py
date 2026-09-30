"""
De Elf-Guhjestocht - extra polder atmosphere between the villages: the open polder gets a few Dutch polder landmarks
that skaters see from the ice (2.10: spread evenly over the whole polder instead of fixed spots).

  hooiberg(s, cx, cz, rng)            a Dutch haystack barn: four tall poles, a pile of hay with a sleepy guh face and a
                                      snowy pyramid roof with a little flag
  ijsbaantje(s, cx, cz, rng, info)    the polder skating pond "Het Vadse Rondje": an oval of polderijs with low
                                      boarding, lamp posts, a bench, a vuurkorf and little guhtjes skating on it (they
                                      cheer like the audience along the canal)
  sfeer(s, ice, reserved, rng, info)  places them on free polder ground, spread out (elftocht_land.verspreid)

All of it stands on the template's ground layer (y = GY) and hangs together (the core's floating check).
"""
import math

from features import elftocht_bouw as B
from features import elftocht_route as R

GY, OY = R.GY, R.OY

HOOIBERGEN = 6
IJSBAANTJES = 3


def _vrij(s, cells, ice, reserved, marge_ijs):
    return all(B.is_free(s, x, z, ice, reserved) and not R.in_plot(x, z, margin=2) and not B.near_start(x, z)
               and not B.near_ice(x, z, ice, marge_ijs) for x, z in cells)


def _zoek(s, cx, cz, rx, rz, ice, reserved, marge_ijs, straal=16):
    """The first free spot (a box of +-rx, +-rz plus one block of room) around (cx, cz), searching outwards."""
    for r in range(0, straal + 1, 2):
        for dx in range(-r, r + 1, 2):
            for dz in (-r, r) if abs(dx) != r else range(-r, r + 1, 2):
                x, z = cx + dx, cz + dz
                if not (rx + 3 < x < R.SIZE_X - rx - 3 and rz + 3 < z < R.SIZE_Z - rz - 3):
                    continue
                cells = [(x + a, z + b) for a in range(-rx - 1, rx + 2) for b in range(-rz - 1, rz + 2)]
                if _vrij(s, cells, ice, reserved, marge_ijs):
                    return x, z, cells
    return None


def hooiberg(s, cx, cz, rng):
    """A hooiberg (5 x 5): corner poles 6 high, a pile of hay (3 high, a sleepy guh face at the front) under a roof that
    sits on the poles: a ring of pluisdak slabs, a snowy pluisdak top and a little pink flag."""
    for dx in (-2, 2):
        for dz in (-2, 2):
            for y in range(OY, OY + 6):
                s.set(cx + dx, y, cz + dz, "minecraft:dark_oak_fence")
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            for y in range(OY, OY + 3):
                s.set(cx + dx, y, cz + dz, "minecraft:hay_block", {"axis": "y"})
    for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(cx + dx, OY + 3, cz + dz, "minecraft:hay_block", {"axis": "y"})
    # a sleepy guh face in the hay, looking at the canal side (south)
    s.set(cx, OY + 1, cz + 1, "guhs:vadshout_gezicht", {"facing": "south", "stemming": str(rng.randrange(3))})
    # loose hay and a pitchfork-ish stick
    s.set(cx + 1, OY, cz + 2, "minecraft:hay_block", {"axis": "x"})
    # the roof: a ring of slabs on the poles, a full block in the middle, snow on top, a flag
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if max(abs(dx), abs(dz)) == 2:
                s.set(cx + dx, OY + 6, cz + dz, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
            else:
                s.set(cx + dx, OY + 6, cz + dz, "guhs:pluisdak")
                if (dx, dz) != (0, 0):
                    s.set(cx + dx, OY + 7, cz + dz, "minecraft:snow", {"layers": str(rng.choice((2, 3)))})
    # the roof rests on the hay too (poles 6 high hold its corners; the middle block hangs on the ring)
    s.set(cx, OY + 7, cz, "minecraft:spruce_fence")
    s.set(cx, OY + 8, cz, "minecraft:pink_banner", {"rotation": str(rng.randrange(16))},
          B.banner_nbt([("white", "circle"), ("magenta", "triangles_bottom")]))


def ijsbaantje(s, cx, cz, rng, info, rx=7, rz=5):
    """Polderbaantje Het Vadse Rondje: an oval of ice with low boarding (an opening towards the canal side), two lamp posts, a
    bench, a vuurkorf, a sign and 3-4 little guhtjes skating on it."""
    ovaal = set()
    for dx in range(-rx, rx + 1):
        for dz in range(-rz, rz + 1):
            if (dx / (rx + 0.4)) ** 2 + (dz / (rz + 0.4)) ** 2 <= 1:
                ovaal.add((cx + dx, cz + dz))
    for (x, z) in ovaal:
        s.set(x, GY, z, B.ICE)
    rand = set()
    for (x, z) in ovaal:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n not in ovaal:
                rand.add(n)
    gat = {(cx + dx, cz + rz + 1) for dx in (-1, 0, 1)} | {(cx + dx, cz + rz) for dx in (-1, 0, 1)}
    for (x, z) in sorted(rand):
        if (x, z) in gat:
            continue
        s.set(x, OY, z, "minecraft:spruce_fence")
    # fences connect (the core's pieces rely on the game updating them; set the sides so they look right at once)
    for (x, z) in sorted(rand):
        if s.get(x, OY, z) != "minecraft:spruce_fence":
            continue
        props = {d: "true" if s.get(x + dx, OY, z + dz) == "minecraft:spruce_fence" else "false"
                 for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0)))}
        props["waterlogged"] = "false"
        s.set(x, OY, z, "minecraft:spruce_fence", props)
    # lamp posts at the ends, a bench and a vuurkorf at the opening, the club sign
    for x in (cx - rx - 2, cx + rx + 2):
        B.lamppaal(s, x, OY, cz)
        info["lichten"].append((x, OY + 3, cz))
    s.set(cx - 3, OY, cz + rz + 2, "guhs:guh_bank", {"facing": "north"})
    s.set(cx + 3, OY, cz + rz + 2, "guhs:guh_bank", {"facing": "north"})
    B.vuurkorf(s, cx + 5, OY, cz + rz + 2)
    info["lichten"].append((cx + 5, OY, cz + rz + 2))
    s.set(cx - 5, OY, cz + rz + 2, "minecraft:spruce_fence")
    s.set(cx - 5, OY + 1, cz + rz + 2, "minecraft:spruce_sign", {"rotation": "0", "waterlogged": "false"},
          B.sign_nbt(["Polderbaantje", "Het Vadse", "Rondje", "VAHOEG!"], "black"))
    # little guhtjes skating (they stay near their spot and cheer when a skater passes by)
    n = rng.choice((3, 4))
    plekken = [(cx - 3, cz - 1), (cx + 2, cz - 2), (cx + 4, cz + 1), (cx - 1, cz + 2)]
    for i, (x, z) in enumerate(plekken[:n]):
        yaw = (i * 97 + 40) % 360
        B.guh_publiek(s, x, OY, z, yaw, scale=rng.choice((0.55, 0.65, 0.75)))
        info["publiek_eigen"].append((x, OY, z, yaw))
    return ovaal | rand


def sfeer(s, ice, reserved, rng, info):
    """Places the skating ponds and the haystacks on free polder ground, spread out over the whole polder (reserving
    their space)."""
    from features import elftocht_land as land
    report = info["report"]
    n_baan = 0
    for (cx, cz) in land.verspreid(s, ice, reserved, IJSBAANTJES, afstand_ijs=(12, 34), onderling=60, rng=rng, rand=18):
        spot = _zoek(s, cx, cz, 9, 8, ice, reserved, R.HALF + 3, straal=14)
        if not spot:
            report.append(f"ijsbaantje near {(cx, cz)} didn't fit")
            continue
        x, z, cells = spot
        ijsbaantje(s, x, z, rng, info)
        reserved.update(cells)
        n_baan += 1
    n_hooi = 0
    for (cx, cz) in land.verspreid(s, ice, reserved, HOOIBERGEN, afstand_ijs=(6, 30), onderling=40, rng=rng, rand=10):
        spot = _zoek(s, cx, cz, 2, 2, ice, reserved, R.HALF + 3)
        if not spot:
            report.append(f"hooiberg near {(cx, cz)} didn't fit")
            continue
        x, z, cells = spot
        hooiberg(s, x, z, rng)
        reserved.update(cells)
        n_hooi += 1
    info["hooibergen"], info["ijsbaantjes"] = n_hooi, n_baan
    return n_hooi, n_baan
