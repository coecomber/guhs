"""
Het Guh-Circuit (2.9) - the template around the tracks: the ground, De Kaasberg (a cheese mountain with a guh face and a
snowy top), the Mikapoort over the top of the Knabbelhelling (the pushing Mika's), the hub between the tracks (the
Regenboogtribune, the Kaas-en-Bergtribune, the Pitpaleis with Coach Vahoegvroem, guh faces on its gables and a giant guh
head on its roof, the boulevard with flags, snack stands and entrance arches), Vadsland (cheese wedges, the kaasfabriekje,
giant guh mushrooms, a kaassaus pond with a fountain, the Vadslooping's signboard), the Regenboog field (rainbow arches,
cloud hills, clouds under the floating road), lamp posts along every road, flowers and blossom trees, and the whole
template's geometry self-check. The tracks themselves are circuit_banen.py.
"""
import math
import random

import numpy as np

from features import circuit_banen as cb

W, H, D = 192, 60, 192
G = cb.G
NPC = (96.5, G, 88.5)                 # Coach Vahoegvroem in the Pitpaleis, facing south (Rotation 0)
ANCHOR = "guhs:guh_circuit_midden"
ANCHOR_POS = (96, G - 1, 96)
# the floating scoreboards (keep in sync with CircuitBanen.BOARDS): per track the whole races and the fastest laps
BOARDS = {"regenboog": [(86, 15, 69), (106, 15, 69)], "vads": [(74, 15, 117), (84, 15, 117)], "kaasberg": [(108, 15, 117), (118, 15, 117)]}
ENTRANCES = [(0, 93), (191, 93)]
LAMPS = ("guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint")
LIGHTS = ("lampion", "circuit_regenboogweg", "race_checkpoint", "race_vahoegpad", "circuit_boostring", "guh_kristal_lamp",
          "minecraft:lantern", "minecraft:sea_lantern", "minecraft:glowstone", "circuit_bergijs", "minecraft:shroomlight")
STAIRS = {"half": "bottom", "shape": "straight", "waterlogged": "false"}


def mc(n):
    return "minecraft:" + n


def lamp(rng, hanging="false"):
    return (rng.choice(LAMPS), {"hanging": hanging, "waterlogged": "false"})


# ======================================================================================================================
# helpers: guh faces
# ======================================================================================================================
def face_disc(s, cx, cy, cz, r, normal, skin=mc("pink_wool"), cheek=mc("magenta_wool"), ears=True, depth=1):
    """A round guh face in a vertical plane (normal "north"/"south"/"east"/"west": the way it looks), with ears on top."""
    nx, nz = cb.FACING_VEC[normal]
    ux, uz = -nz, nx                                  # to the right as you look at the face
    placed = []

    def put(u, v, block, d=0):
        x = int(math.floor(cx + ux * u + nx * d))
        z = int(math.floor(cz + uz * u + nz * d))
        y = int(math.floor(cy + v))
        s.set(x, y, z, block)
        placed.append((x, y, z))
    for u in range(-r - 1, r + 2):
        for v in range(-r - 1, r + 2):
            if u * u + (v * 1.08) ** 2 <= r * r + 1:
                for d in range(-depth + 1, 1):
                    put(u, v, skin, d)
    if ears:
        for side in (-1, 1):
            ex, ey = side * r * 0.62, r * 0.82
            er = max(2, r * 0.36)
            for u in range(int(ex - er - 1), int(ex + er + 2)):
                for v in range(int(ey - er), int(ey + er + 2)):
                    d = math.hypot(u - ex, v - ey)
                    if d <= er:
                        put(u, v, cheek if d <= er * 0.5 else skin)
    # eyes (black with a white shine), cheeks, nose, smile
    for side in (-1, 1):
        ex, ey = side * r * 0.4, r * 0.12
        er = max(1.0, r * 0.2)
        for u in range(int(ex - er - 1), int(ex + er + 2)):
            for v in range(int(ey - er - 1), int(ey + er + 2)):
                if math.hypot(u - ex, v - ey) <= er:
                    put(u, v, mc("black_wool"), 1)
        put(int(round(ex + er * 0.4)), int(round(ey + er * 0.4)), mc("white_wool"), 1)
        cx2, cy2 = side * r * 0.62, -r * 0.3
        for u in range(int(cx2 - 1), int(cx2 + 2)):
            put(u, int(round(cy2)), cheek, 1)
    put(0, int(round(-r * 0.2)), cheek, 1)
    for u in (-1, 0, 1):
        put(u, int(round(-r * 0.45)) - (1 if u == 0 else 0), mc("magenta_wool"), 1)
    return placed


def guh_head(s, cx, cy, cz, r, normal):
    """A giant round guh head (a ball of pink wool) with ears and a face looking `normal`."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 * 1.1 + (z + 0.5 - cz) ** 2 <= r * r:
                    s.set(x, y, z, mc("pink_wool"))
    nx, nz = cb.FACING_VEC[normal]
    ux, uz = -nz, nx
    for side in (-1, 1):                               # round ears on top
        ex, ez = cx + ux * side * r * 0.6, cz + uz * side * r * 0.6
        for dy in range(0, int(r * 0.7)):
            for d in range(-3, 4):
                for t in (0, 1):
                    q = (int(math.floor(ex + ux * d - nx * t)), int(math.floor(cy + r * 0.7 + dy)), int(math.floor(ez + uz * d - nz * t)))
                    if d * d + (dy - r * 0.35) ** 2 <= (r * 0.4) ** 2:
                        s.set(*q, mc("magenta_wool") if t == 0 and d * d + (dy - r * 0.35) ** 2 <= (r * 0.2) ** 2 else mc("pink_wool"))
    # the face on the front of the ball: eyes, cheeks and a smile on its frontmost blocks
    def front(u, v):
        x = int(math.floor(cx + ux * u))
        z0 = int(math.floor(cz + uz * u))
        y = int(math.floor(cy + v))
        for t in range(int(r) + 2, -1, -1):
            q = (x + nx * t if ux == 0 else x, y, z0 + nz * t if uz == 0 else z0)
            if s.get(*q) == mc("pink_wool"):
                return q
        return None
    for side in (-1, 1):
        for du in range(-1, 2):
            for dv in range(-1, 2):
                if abs(du) + abs(dv) <= 1:
                    q = front(side * r * 0.4 + du, r * 0.1 + dv)
                    if q:
                        s.set(*q, mc("black_wool"))
        q = front(side * r * 0.62, -r * 0.25)
        if q:
            s.set(*q, mc("magenta_wool"))
    for du in (-1, 0, 1):
        q = front(du, -r * 0.45 - (1 if du == 0 else 0))
        if q:
            s.set(*q, mc("magenta_wool"))


def gable_roof(s, x0, x1, z0, z1, y0):
    """A solid gable roof of pluisdak along x over z0..z1 (eaves one block out), stairs on both slopes, ridge on top."""
    for k in range(0, (z1 - z0) // 2 + 2):
        za, zb = z0 - 1 + k, z1 + 1 - k
        if za > zb:
            break
        for x in range(x0 - 1, x1 + 2):
            for z in range(za, zb + 1):
                if z == za and za < zb:
                    s.set(x, y0 + k, z, "guhs:pluisdak_trap", {"facing": "south", **STAIRS})
                elif z == zb and za < zb:
                    s.set(x, y0 + k, z, "guhs:pluisdak_trap", {"facing": "north", **STAIRS})
                else:
                    s.set(x, y0 + k, z, "guhs:pluisdak")
    return y0 + (z1 - z0) // 2 + 1


# ======================================================================================================================
# the ground and De Kaasberg
# ======================================================================================================================
def ground(s):
    for x in range(W):
        for z in range(D):
            s.fill(x, 0, z, x, G - 2, z, mc("dirt"))
            s.set(x, G - 1, z, mc("grass_block"), {"snowy": "false"})


MOUNTAIN = (128, 138, 144, 176)       # the core of the mountain (full height); it slopes off over FALL blocks
PEAK, FALL = 12, 9


def mountain_height(x, z):
    x0, z0, x1, z1 = MOUNTAIN
    dx = max(x0 - x, 0, x - x1)
    dz = max(z0 - z, 0, z - z1)
    d = math.hypot(dx, dz)
    if d >= FALL:
        return 0
    base = PEAK * (1 - d / FALL) ** 0.8
    # a bit higher towards the summit (north end of the core)
    lift = 1.0 if z <= 150 else max(0.75, 1 - (z - 150) / 104)
    return int(round(base * lift))


def kaasberg(s, rng):
    """The mountain: cheese all through, gatenkaas holes in its sides, snow near the top, a guh face looking north."""
    top = {}
    for x in range(100, 188):
        for z in range(112, 190):
            h = mountain_height(x, z)
            if h <= 0:
                continue
            top[(x, z)] = G - 1 + h
            for y in range(G - 1, G + h):
                s.set(x, y, z, mc("yellow_terracotta") if (x * 3 + y * 7 + z) % 5 else "guhs:belegen_kaas_stenen")
            y = G - 1 + h
            if h >= PEAK - 1:
                s.set(x, y, z, mc("snow_block"))
            elif rng.random() < 0.12:
                s.set(x, y, z, "guhs:gatenkaas")
            else:
                s.set(x, y, z, "guhs:belegen_kaas_tegels" if (x + z) % 7 == 0 else "guhs:belegen_kaas_stenen")
    # gatenkaas holes in the slopes (little dents)
    for _ in range(40):
        x, z = rng.randint(118, 156), rng.randint(128, 184)
        h = mountain_height(x, z)
        if 3 <= h <= PEAK - 2:
            s.set(x, G - 1 + h, z, mc("air"))
    return top


def kaasberg_face_and_flag(s, rng):
    """A big guh face on the Kaasberg's north slope (it looks at the tribunes) and a chequered flag on its top."""
    face_disc(s, 136.5, G + 7, 130, 6, "north", skin=mc("yellow_wool"), cheek=mc("orange_wool"), depth=3)
    # the summit flag: a white pole with a chequered flag
    fx, fz = 136, 134
    h = mountain_height(fx, fz)
    base = G + h
    for y in range(base, base + 9):
        s.set(fx, y, fz, mc("white_concrete"))
    for i in range(6):
        for j in range(4):
            s.set(fx + 1 + i, base + 5 + j, fz, mc("black_wool") if (i + j) % 2 else mc("white_wool"))
    s.set(fx, base + 9, fz, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})


def mikapoort(s, b, mc_):
    """The Mikapoort: a bridge over the top of the Knabbelhelling where the Mika's push the kaasknabbels down (markers
    circuit_rolplek on its deck, looking down the hill), with Mika stone pillars and a crooked Mika face."""
    z = 152
    c = b.cols.get((150, z))
    walk = b.walk(c["h2"])
    deck = walk + 5
    for x in range(143, 159):
        for dz in (-1, 0, 1):
            s.set(x, deck, z + dz, "guhs:mika_steen")
        s.set(x, deck + 1, z - 1, "guhs:mika_steen_plaat", {"type": "bottom", "waterlogged": "false"})
    for x in (143, 144, 157, 158):
        for dz in (-1, 0, 1):
            y = deck - 1
            while y >= 0 and s.get(x, y, z + dz) in (None, mc("air")):
                s.set(x, y, z + dz, "guhs:mika_steen_pilaar", {"axis": "y"})
                y -= 1
    for x in (143, 158):
        for dy in range(0, 3):
            s.set(x, deck + 1 + dy, z, "guhs:mika_steen_pilaar", {"axis": "y"})
        s.set(x, deck + 4, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    spots = []
    for (x, _, vanaf) in b.rollen:
        px = int(math.floor(x))
        s.set(px, deck + 1, z + 1, "guhs:circuit_rolplek", {"facing": "south", "vanaf": str(vanaf)})
        spots.append((px, deck + 1, z + 1))
    return spots, deck


# ======================================================================================================================
# the hub: two grandstands, the Pitpaleis, the boulevard
# ======================================================================================================================
def tribune(s, rng, x0, x1, front_z, facing):
    """Six rows of zitzakken rising away from the track, a back wall, a striped roof, flags and lampions."""
    step = 1 if facing == "north" else -1           # rows go south (north stand) or north (south stand)
    colours = ("pink", "white", "magenta", "yellow", "light_blue", "lime")
    rows = 6
    for i in range(rows):
        zr = front_z + step * 2 * i
        for dz in (0, step):
            for x in range(x0, x1 + 1):
                for y in range(G, G + i):
                    s.set(x, y, zr + dz, mc("white_concrete") if (x + y) % 2 else mc("pink_concrete"))
        for x in range(x0 + 1, x1):
            if (x - x0) % 9 == 0:
                if i > 0:
                    s.set(x, G + i - 1, zr, "guhs:knuffelsteen_trap", {"facing": "north" if facing == "south" else "south", **STAIRS})
            else:
                s.set(x, G + i, zr, f"guhs:{colours[(x + i) % len(colours)]}_zitzak", {"facing": facing})
    back_z = front_z + step * 2 * rows
    for x in range(x0, x1 + 1):
        s.fill(x, G, back_z, x, G + 9, back_z, mc("white_concrete") if (x // 3) % 2 else mc("pink_concrete"))
        for k in range(0, 2 * rows + 1):
            z = front_z + step * k
            s.set(x, G + 10, z, mc("pink_wool") if (x // 2) % 2 else mc("white_wool"))
        s.set(x, G + 10, back_z, mc("magenta_wool"))
        s.set(x, G + 9, front_z, "guhs:vlaggetjes", {"axis": "x"})
    for x in range(x0, x1 + 1, 8):
        s.fill(x, G, front_z, x, G + 9, front_z, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, G + 9, front_z + step * 3, *lamp(rng, "true"))
    for x in (x0, x1):
        for k in range(0, 2 * rows + 1):
            z = front_z + step * k
            for y in range(G + k // 2, G + 10):
                s.set(x, y, z, mc("white_concrete") if (y + k) % 3 else mc("pink_concrete"))
    # guh ears on the roof, above the middle
    mid = (x0 + x1) // 2
    for side in (-7, 7):
        for dy in range(1, 5):
            for dx in range(-2, 3):
                if dx * dx + (dy - 2) ** 2 <= 5:
                    s.set(mid + side + dx, G + 10 + dy, back_z - step, mc("magenta_wool") if abs(dx) < 1 and 1 < dy < 4 else mc("pink_wool"))
    return back_z


def pitpaleis(s, rng):
    """The Pitpaleis: a big hall between the grandstands, a gable roof of pluisdak with a giant guh head on it, guh faces on
    the gables (the doors are their mouths), Coach Vahoegvroem inside with the kiesbord, trophies and tyre stacks."""
    X0, X1, Z0, Z1 = 72, 120, 84, 102
    top = G + 8
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            s.set(x, G - 1, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
            wall = x in (X0, X1) or z in (Z0, Z1)
            for y in range(G, top + 1):
                if wall:
                    pillar = (x - X0) % 8 == 0 and z in (Z0, Z1)
                    s.set(x, y, z, mc("quartz_pillar") if pillar else (mc("pink_concrete") if (y + x) % 5 else mc("white_concrete")),
                          {"axis": "y"} if pillar else None)
                else:
                    s.set(x, y, z, mc("air"))
            s.set(x, top + 1, z, mc("white_concrete"))
    # the gable roof (along x) of pluisdak
    ridge = gable_roof(s, X0, X1, Z0, Z1, top + 2)
    # gable ends: a big guh face each (the door is the mouth)
    for gx, normal in ((X0, "west"), (X1, "east")):
        face_disc(s, gx + 0.5, G + 9, 93.5, 7, normal, depth=1)
        for z in range(91, 96):                                   # the door
            for y in range(G, G + 4):
                s.set(gx, y, z, mc("air"))
                s.set(gx + (1 if normal == "west" else -1), y, z, mc("air"))
                s.set(gx - (1 if normal == "west" else -1), y, z, mc("air"))
        for z in (90, 96):
            s.set(gx - (1 if normal == "west" else -1), G + 3, z, *lamp(rng))
    # the giant guh head on the roof, looking north over the Regenboogtribune
    guh_head(s, 96.5, ridge + 6, 93.5, 6.5, "north")
    for x in range(93, 101):
        for z in range(90, 98):
            s.set(x, ridge + 1, z, "guhs:pluisdak")
    # inside: the kiesbord (three big track panels) on the north wall, lamps, trophies, tyre stacks, benches
    panels = [(80, ["purple", "magenta", "pink", "red", "orange", "yellow", "lime", "light_blue"]),
              (92, ["yellow", "orange", "yellow", "orange", "yellow", "orange", "yellow", "orange"]),
              (104, ["light_blue", "white", "light_blue", "white", "cyan", "white", "light_blue", "white"])]
    for px, cols in panels:
        for i in range(9):
            for j in range(6):
                edge = i in (0, 8) or j in (0, 5)
                s.set(px + i, G + 2 + j, Z0 + 1, mc("white_concrete") if edge else mc(cols[(i + j) % len(cols)] + "_concrete"))
        s.set(px + 4, G + 1, Z0 + 1, mc("gold_block"))
    for x in range(X0 + 4, X1 - 3, 6):
        s.set(x, top, 88, "guhs:guh_kristal_lamp")
        s.set(x, top, 98, "guhs:guh_kristal_lamp")
        s.set(x, top, 93, *lamp(rng, "true"))
    for x in (78, 114):                                            # trophies on tables
        s.set(x, G, 86, "guhs:guh_tafel")
        s.set(x, G + 1, 86, mc("gold_block"))
        s.set(x, G + 2, 86, "guhs:guh_taart", {"bites": "0"})
    for x, z in ((75, 99), (76, 99), (75, 100), (117, 99), (116, 99), (117, 100)):   # tyre stacks
        for y in range(G, G + 3):
            s.set(x, y, z, mc("black_concrete") if y % 2 == 0 else mc("pink_concrete"))
    for x in range(84, 110, 6):                                    # benches along the south wall
        s.set(x, G, 100, "guhs:pink_zitzak", {"facing": "north"})
        s.set(x + 1, G, 100, "guhs:white_zitzak", {"facing": "north"})
    s.set(92, G, 86, "guhs:block_of_kaasknabbels")
    s.set(101, G, 86, "guhs:block_of_kaasknabbels")
    s.set(101, G + 1, 86, "guhs:block_of_kaasknabbels")
    return (X0, X1, Z0, Z1)


def boulevard(s, rng):
    """The boulevard: from the west and east edge to the Pitpaleis' doors, flags, lamp posts, snack stands, entrance arches."""
    for x in list(range(0, 72)) + list(range(121, W)):
        for z in range(91, 96):
            s.set(x, G - 1, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
            for y in range(G, G + 6):
                s.set(x, y, z, mc("air"))
    for x in list(range(2, 70, 8)) + list(range(124, 190, 8)):
        for z in (90, 96):
            s.fill(x, G, z, x, G + 2, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                              "waterlogged": "false"})
            s.set(x, G + 3, z, *lamp(rng))
    # entrance arches with guh ears and a chequered banner
    for ex in (1, 190):
        for z in (89, 97):
            s.fill(ex, G, z, ex, G + 6, z, mc("quartz_pillar"), {"axis": "y"})
        for z in range(89, 98):
            s.set(ex, G + 7, z, mc("pink_concrete"))
            s.set(ex, G + 6, z, mc("black_wool") if z % 2 else mc("white_wool"))
        for z0 in (90, 95):
            for dz in range(0, 3):
                for dy in (1, 2):
                    if not (dy == 2 and dz != 1):
                        s.set(ex, G + 7 + dy, z0 + dz, mc("magenta_wool") if dy == 1 and dz == 1 else mc("pink_wool"))
        s.set(ex, G + 8, 93, *lamp(rng))
    # snack stands next to the boulevard
    stand(s, 40, 99, "pink", [("guhs:block_of_kaasknabbels", None), ("guhs:guh_taart", {"bites": "0"})])
    stand(s, 144, 99, "yellow", [("guhs:block_of_kaasknabbels", None), ("guhs:guh_taart", {"bites": "0"})])
    stand(s, 40, 83, "light_blue", [("guhs:guh_taart", {"bites": "0"}), ("guhs:block_of_kaasknabbels", None)])
    stand(s, 144, 83, "magenta", [("guhs:guh_taart", {"bites": "0"}), ("guhs:block_of_kaasknabbels", None)])


def stand(s, x0, z0, stripe, goods):
    """A 7x5 snack stand with a striped awning, its counter towards the boulevard."""
    towards = "north" if z0 > 93 else "south"
    front = z0 if towards == "north" else z0 + 4
    back = z0 + 4 if towards == "north" else z0
    for x in range(x0, x0 + 7):
        for z in range(z0, z0 + 5):
            s.set(x, G - 1, z, mc("white_concrete"))
    s.fill(x0, G, back, x0 + 6, G + 2, back, mc(stripe + "_concrete"))
    for x in (x0, x0 + 6):
        for z in (front, back):
            s.fill(x, G, z, x, G + 2, z, mc("birch_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                           "waterlogged": "false"})
    for x in range(x0 + 1, x0 + 6):
        s.set(x, G, front, mc("pink_terracotta"))
    for x in range(x0 - 1, x0 + 8):
        for z in range(z0 - 1, z0 + 6):
            s.set(x, G + 3, z, mc(stripe + "_wool") if (x + z) % 2 else mc("white_wool"))
    s.set(x0 + 3, G + 4, z0 + 2, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    for i, (name, props) in enumerate(goods):
        s.set(x0 + 2 + i * 2, G + 1, front, name, props)


# ======================================================================================================================
# Vadsland and the Regenboog field
# ======================================================================================================================
def cheese_wedge(s, x0, z0, length, height, along_x=True):
    """A giant cheese wedge (a triangle of yellow with gatenkaas holes, a rind of orange)."""
    for i in range(length):
        h = int(height * (1 - i / length)) + 1
        for w in range(0, 7):
            x, z = (x0 + i, z0 + w) if along_x else (x0 + w, z0 + i)
            for y in range(G, G + h):
                rind = w in (0, 6) and (y - G) % 3 == 0
                hole = (i * 5 + y * 3 + w * 7) % 13 == 0 and 0 < w < 6 and y < G + h - 1
                s.set(x, y, z, mc("orange_terracotta") if rind else (mc("air") if hole else mc("yellow_concrete")))


def guh_paddenstoel(s, x, z, h, r):
    """A giant guh mushroom: a stem and a pink bouncy cap (stuiterpaddenstoel blocks: you can bounce on it)."""
    for y in range(G, G + h):
        s.set(x, y, z, "guhs:guhpaddenstoel_steel", {"north": "true", "south": "true", "east": "true", "west": "true",
                                                    "up": "false", "down": "false"})
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3:
                s.set(x + dx, G + h, z + dz, "guhs:circuit_stuiterpaddenstoel")
                if d <= r - 1.2:
                    s.set(x + dx, G + h + 1, z + dz, "guhs:circuit_stuiterpaddenstoel")


def kaasfabriekje(s, rng, x0, z0):
    """A little cheese factory (12 x 10): yellow walls with round holes, a pluisdak roof, a chimney with a guh face."""
    X1, Z1 = x0 + 11, z0 + 9
    for x in range(x0, X1 + 1):
        for z in range(z0, Z1 + 1):
            s.set(x, G - 1, z, mc("yellow_terracotta"))
            wall = x in (x0, X1) or z in (z0, Z1)
            for y in range(G, G + 5):
                if wall:
                    hole = (x + z * 2 + y) % 6 == 0 and y in (G + 1, G + 2)
                    s.set(x, y, z, mc("yellow_stained_glass") if hole else mc("yellow_concrete"))
                else:
                    s.set(x, y, z, mc("air"))
    gable_roof(s, x0, X1, z0, Z1, G + 5)
    for z in range(z0 + 4, z0 + 6):                     # the door on the east side
        for y in range(G, G + 3):
            s.set(X1, y, z, mc("air"))
    s.set(X1 + 1, G + 2, z0 + 3, *lamp(rng))
    # the chimney, with a little guh face near its top
    cx, cz = x0 + 3, z0 + 3
    for y in range(G + 5, G + 14):
        s.set(cx, y, cz, mc("pink_terracotta"))
        s.set(cx + 1, y, cz, mc("pink_terracotta"))
    s.set(cx, G + 12, cz - 1, mc("black_wool"))
    s.set(cx + 1, G + 12, cz - 1, mc("black_wool"))
    s.set(cx, G + 14, cz, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    # inside: kaas saus vats and kaasknabbels
    for x in (x0 + 2, x0 + 3):
        for z in (Z1 - 2, Z1 - 1):
            s.set(x, G, z, "guhs:block_of_kaasknabbels")
    s.set(x0 + 8, G, Z1 - 1, mc("barrel"), {"facing": "up", "open": "false"})
    s.set(x0 + 9, G, Z1 - 1, mc("barrel"), {"facing": "up", "open": "false"})


def kaassaus_vijver(s, x0, z0, x1, z1):
    """A pond of kaas saus with a cheese fountain in the middle and a pink rim."""
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    rx, rz = (x1 - x0) / 2, (z1 - z0) / 2
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            d = ((x + 0.5 - cx) / rx) ** 2 + ((z + 0.5 - cz) / rz) ** 2
            if d <= 0.8:
                s.set(x, G - 1, z, "guhs:kaas_saus", {"level": "0"})
                s.set(x, G - 2, z, "guhs:kaas_saus", {"level": "0"})
                s.set(x, G - 3, z, mc("yellow_terracotta"))
            elif d <= 1.05:
                s.set(x, G - 1, z, mc("pink_terracotta"))
                s.set(x, G, z, "guhs:knuffelsteen_muur", {"up": "true" if (x + z) % 3 == 0 else "false"})
    fx, fz = int(cx), int(cz)
    for y in range(G - 1, G + 3):
        s.set(fx, y, fz, "guhs:belegen_kaas_stenen")
    s.set(fx, G + 3, fz, "guhs:block_of_kaasknabbels")
    s.set(fx, G + 4, fz, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})


def rainbow_arch(s, cx, cz, r, along_x=True):
    """A standing rainbow (seven bands) with a cloud at each foot."""
    colours = ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple"]
    for u in range(-r - 1, r + 2):
        for v in range(0, r + 2):
            d = math.hypot(u, v)
            if r - 7 < d <= r:
                band = int(r - d)
                x, z = (cx + u, cz) if along_x else (cx, cz + u)
                s.set(x, G - 1 + v, z, mc(colours[min(6, band)] + "_wool"))
    for u in (-r + 3, r - 3):
        x, z = (cx + u, cz) if along_x else (cx, cz + u)
        for dx in range(-3, 4):
            for dz in range(-2, 3):
                for dy in range(0, 3):
                    if dx * dx / 9 + dz * dz / 4 + dy * dy / 4 <= 1:
                        s.set(x + dx, G + dy, z + dz, mc("white_wool") if dy < 2 else "guhs:pluiswolblok")


def cloud_hill(s, cx, cz, rx, rz, h):
    for x in range(int(cx - rx), int(cx + rx) + 1):
        for z in range(int(cz - rz), int(cz + rz) + 1):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if d <= 1:
                top = int(h * (1 - d) ** 0.6)
                for y in range(G, G + top):
                    s.set(x, y, z, mc("white_wool") if y < G + top - 1 else "guhs:pluiswolblok")


def clouds_under_deck(s, banen_info, b, rng):
    """Puffy clouds hanging under the floating rainbow road (they hang on to the deck)."""
    deck = banen_info[b.name]["deck"]
    cells = sorted(deck.items(), key=lambda kv: b.cols[kv[0]]["s"])
    for (x, z), under in cells[::37]:
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for dy in range(1, 3):
                    if dx * dx + dz * dz + dy * dy * 3 <= 10 and (x + dx, z + dz) in deck:
                        q = (x + dx, under - dy, z + dz)
                        if s.get(*q) in (None, mc("air")):
                            s.set(*q, "guhs:pluiswolblok" if dy == 1 else mc("white_wool"))


# ======================================================================================================================
# lamps along the roads, flowers and trees
# ======================================================================================================================
def road_lamps(s, banen, rng):
    """Lamp posts beside the cheese roads (every ~11 blocks, both sides): no dark spots, no Mika spawns."""
    for b in banen:
        if b.name == "regenboog":
            continue
        placed = []
        for p in b.samples[::24]:
            for side in (-1, 1):
                for dist in (6.6, 7.6, 8.6):
                    x = int(math.floor(p["x"] + p["n"][0] * side * dist))
                    z = int(math.floor(p["z"] + p["n"][1] * side * dist))
                    if any(abs(x - px) + abs(z - pz) < 5 for px, pz in placed):
                        break
                    if not (0 <= x < W and 0 <= z < D) or any(bb.cols.get((x, z)) and abs(bb.cols[(x, z)]["off"]) <= cb.WALL for bb in banen):
                        continue
                    y = G
                    while y < H - 5 and s.get(x, y, z) not in (None, mc("air")):
                        y += 1
                    if s.get(x, y - 1, z) in (None, mc("air")) or not all(s.get(x, y + k, z) in (None, mc("air")) for k in range(4)):
                        continue
                    s.fill(x, y, z, x, y + 1, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                                      "waterlogged": "false"})
                    s.set(x, y + 2, z, *lamp(rng))
                    placed.append((x, z))
                    break


def flowers_and_trees(s, rng, banen):
    road = set()
    for b in banen:
        road |= set(b.cols)
    LEAVES = {"persistent": "true", "distance": "7", "waterlogged": "false"}

    def free(x, z, rad):
        for dx in range(-rad, rad + 1):
            for dz in range(-rad, rad + 1):
                p = (x + dx, z + dz)
                if not (1 <= p[0] < W - 1 and 1 <= p[1] < D - 1) or p in road or s.get(p[0], G, p[1]) not in (None,) \
                        or s.get(p[0], G - 1, p[1]) != mc("grass_block"):
                    return False
        return True
    trees = 0
    for _ in range(420):
        tx, tz = rng.randint(3, W - 4), rng.randint(3, D - 4)
        if not free(tx, tz, 3):
            continue
        trees += 1
        for y in range(G, G + 5):
            s.set(tx, y, tz, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-3, 4):
            for dy in range(-1, 3):
                for dz in range(-3, 4):
                    if dx * dx + dz * dz + dy * dy * 2 <= 9 and (dx, dz) != (0, 0) or (dx, dz) == (0, 0) and dy >= 1:
                        p = (tx + dx, G + 4 + dy, tz + dz)
                        if s.get(*p) in (None, mc("air")):
                            s.set(*p, "guhs:guhbloesem_leaves", LEAVES)
        if trees >= 34:
            break
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if (x, z) in road or s.get(x, G, z) is not None or s.get(x, G - 1, z) != mc("grass_block"):
                continue
            roll = rng.random()
            if roll < 0.04:
                s.set(x, G, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
            elif roll < 0.08:
                s.set(x, G, z, "guhs:roze_gras")
    # lamp posts in the open fields (no dark spots anywhere)
    for x in range(6, W - 4, 14):
        for z in range(6, D - 4, 14):
            if (x, z) in road or s.get(x, G, z) not in (None, "guhs:roze_gras", "guhs:roze_guhbloem", "guhs:knabbelroos",
                                                         "guhs:kaasbloem", "guhs:guhoortjes") or s.get(x, G - 1, z) != mc("grass_block"):
                continue
            if any(s.get(x, G + k, z) not in (None, mc("air")) for k in range(1, 4)):
                continue
            s.fill(x, G, z, x, G + 1, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false",
                                                              "waterlogged": "false"})
            s.set(x, G + 2, z, *lamp(rng))


# ======================================================================================================================
# the whole template's self-check (the tracks check themselves in circuit_banen.check)
# ======================================================================================================================
PASSABLE = cb.PASSABLE + ("guhs:roze_gras", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes",
                          "guhs:vlaggetjes", "guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint", "minecraft:jigsaw")


def passable(s, x, y, z):
    b = s.get(x, y, z)
    return b is None or b in PASSABLE


def solid(s, x, y, z):
    b = s.get(x, y, z)
    return b is not None and b not in PASSABLE and "kaas_saus" not in b and "fence" not in b


def check(s, banen, info):
    problems = []
    nx, ny, nz = int(NPC[0]), int(NPC[1]), int(NPC[2])
    if not (solid(s, nx, ny - 1, nz) and passable(s, nx, ny, nz) and passable(s, nx, ny + 1, nz)):
        problems.append("Coach Vahoegvroem isn't standing on a floor with room around her")
    if s.get(*ANCHOR_POS) != mc("jigsaw"):
        problems.append("no anchor jigsaw")
    for name, spots in BOARDS.items():
        for q in spots:
            if s.get(*q) not in (None, mc("air")):
                problems.append(f"the scoreboard spot {q} ({name}) isn't free: {s.get(*q)}")
    # the Regenboogbaan's start marker is where CircuitBanen.TEMPLATE_START says (facing east)
    st = info["regenboog"]["start"]
    if (st[0], st[1], st[2], st[3]) != (59, G, 62, "east"):
        problems.append(f"the Regenboogbaan's start marker moved: {st} (CircuitBanen.TEMPLATE_START is 59, {G}, 62 east)")

    # walking: from both entrances you reach the Coach, both grandstands' top rows, and every track's start
    def standable(x, y, z):
        return 0 <= x < W and 0 <= z < D and 0 < y < H - 2 and passable(s, x, y, z) and passable(s, x, y + 1, z) and solid(s, x, y - 1, z)
    starts = [(ex, G, ez + dz) for ex, ez in ENTRANCES for dz in (-1, 0, 1)]
    seen, todo = set(p for p in starts if standable(*p)), [p for p in starts if standable(*p)]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if n in seen or not standable(*n):
                    continue
                if dy == 1 and not passable(s, x, y + 2, z):
                    continue
                seen.add(n)
                todo.append(n)
                break
    targets = [("Coach Vahoegvroem", (nx, ny, nz + 2)), ("the top of the Regenboogtribune", (97, G + 5, 80)),
               ("the top of the Kaas-en-Bergtribune", (97, G + 5, 106))]
    for b in banen:
        x, y, z, _ = info[b.name]["start"]
        targets.append((f"the start of the {b.name}baan", (x, y, z)))
    for name, spot in targets:
        if spot not in seen:
            problems.append(f"can't walk to {name} at {spot}")
    # nothing floats: every block hangs together with the ground
    solid_blocks = {p for p, (b, _, _) in s.blocks.items() if b != mc("air")}
    todo = [p for p in solid_blocks if p[1] == 0]
    connected = set(todo)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid_blocks and n not in connected:
                connected.add(n)
                todo.append(n)
    floating = solid_blocks - connected
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:6]}")
    # no dark roads: a light within 12 blocks (walking distance on the map) of every road column
    lights = np.array([p for p, (b, _, _) in s.blocks.items() if any(k in b for k in LIGHTS)])
    far = []
    for b in banen:
        for (x, z), c in b.road().items():
            if np.min(np.abs(lights[:, 0] - x) + np.abs(lights[:, 2] - z)) > 12:
                far.append((b.name, x, z))
    if far:
        problems.append(f"{len(far)} road columns far from any light, e.g. {far[:5]}")
    return problems
