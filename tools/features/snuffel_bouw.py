"""
Het Snuffeleiland (kern): the small TEST island that the kern ships, and the bare floor of the game tests.

The real island (the beach, Snuffeldorp, the meadow) is the island slice's: it writes data/guhs/snuffel/eiland.json and
its own templates and this little island is gone. Until then this is what stands in guhs:snuffeleiland, so that the dog
form, sniffing, the tree, the residents and the trip there and back can be played on a dev server:

  - a round sand island with a green middle in the flat sea (the dimension's sea level is y 62: `ZEE`), with shallows
    around it that slope down to the sea floor (the template holds the whole underwater foot, so nothing floats);
  - the tree's spot north of the middle, a path from the south beach to it, a little jetty with lanterns and barrels on
    the east side (where the captain stands), a campfire with two log seats, two small trees, flowers;
  - three scent sources (a buried bone, a buried ball, the salty barrels) and three residents (Kapitein Zoutsnoet, Jutje
    Kwispel, a corgi puppy).

Everything Java needs comes out of `eiland()` as the dict that becomes eiland.json (see feature/snuffel/Eiland.java).
"""
import math
import random

# the dimension's flat sea (feature/snuffel/Eiland.java ZEE, ZAND, STEEN; the layers are written by snuffel.dimensie)
ZEE, ZAND, STEEN = 62, 43, 39
SX, SY, SZ = 61, 34, 61
C = 30                                   # the island's middle (x and z)
OORSPRONG = (-C, ZAND + 1, -C)           # the template stands on the sea floor; the island's middle is world 0, 0
G = ZEE + 1 - OORSPRONG[1]               # template y of the ground blocks (their top is the beach: world y 64)
BOOM = (C, G + 1, C - 7)                 # the tree's foot
STRAND = ((C + 0.5, G + 1, C + 13.5), 180.0)       # washed ashore on the south beach, looking at the island
HAVEN = ((C + 21.5, G + 1, C + 0.5), 90.0)         # the end of the jetty, looking at the island
VERSIE = 1


def straal(hoek):
    """The beach line: a circle with a few soft bumps."""
    return 15.0 + 1.3 * math.sin(3 * hoek + 0.6) + 0.8 * math.sin(5 * hoek + 2.0)


def eiland(h):
    """-> (Structure, dict for eiland.json)."""
    s = h.Structure((SX, SY, SZ))
    rng = random.Random(2130_5101)
    gras = set()
    for x in range(SX):
        for z in range(SZ):
            dx, dz = x - C, z - C
            d = math.hypot(dx, dz)
            r = straal(math.atan2(dz, dx))
            groen = d <= r - 3.6 + 0.5 * math.sin(dx * 0.9) * math.cos(dz * 0.7)
            for y in range(0, G + 1):
                diep = G - y                                     # 0 = the ground itself
                # the foot under water: gentle shallows first (1.4 out per block down), then steeper
                rand = r + min(diep, 4) * 1.4 + max(0, diep - 4) * 0.55
                if d > rand:
                    continue
                if diep == 0 and groen:
                    blok = "minecraft:grass_block"
                elif groen and diep <= 2 and d <= r - 4.4:
                    blok = "minecraft:dirt"
                elif d > rand - 2.6 or diep <= 3:
                    blok = "minecraft:sand"
                else:
                    blok = "minecraft:stone"
                s.set(x, y, z, blok)
            if groen:
                gras.add((x, z))

    # --- the path from the south beach to the tree, and a ring of path around the tree's spot ---
    for z in range(BOOM[2] + 2, C + 12):
        if (C, z) in gras:
            s.set(C, G, z, "minecraft:dirt_path")
    for dx, dz in ((-1, 2), (0, 2), (1, 2), (-2, 1), (2, 1), (-2, 0), (2, 0), (-2, -1), (2, -1), (-1, -2), (0, -2), (1, -2)):
        if (BOOM[0] + dx, BOOM[2] + dz) in gras:
            s.set(BOOM[0] + dx, G, BOOM[2] + dz, "minecraft:dirt_path")
    vrij = {(C, z) for z in range(0, SZ)} | {(BOOM[0] + dx, BOOM[2] + dz) for dx in range(-2, 3) for dz in range(-2, 3)}

    # --- the jetty on the east side: three planks wide, on posts, two lantern posts at its end, barrels at its start ---
    jx0 = C + 13
    for x in range(jx0, C + 23):
        for z in range(C - 1, C + 2):
            if s.get(x, G, z) is None:
                s.set(x, G, z, "minecraft:spruce_planks")
                if z != C and (x - jx0) % 3 == 0:
                    for y in range(G - 1, -1, -1):
                        if s.get(x, y, z) is not None:
                            break
                        s.set(x, y, z, "minecraft:spruce_fence", {"waterlogged": "true"} if y <= G - 1 else {})
    for z in (C - 1, C + 1):
        s.set(C + 22, G + 1, z, "minecraft:spruce_fence")
        s.set(C + 22, G + 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    s.set(C + 14, G + 1, C - 1, "minecraft:barrel", {"facing": "up"})
    s.set(C + 15, G + 1, C - 1, "minecraft:barrel", {"facing": "up"})
    s.set(C + 14, G + 2, C - 1, "minecraft:barrel", {"facing": "south"})
    for x in range(C + 10, C + 14):
        vrij.add((x, C))

    # --- the campfire west of the middle, with two logs to sit on ---
    vx, vz = C - 6, C + 2
    s.set(vx, G + 1, vz, "minecraft:campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"})
    for dx, dz, as_ in ((-2, 0, "z"), (-2, 1, "z"), (0, 2, "x"), (1, 2, "x")):
        s.set(vx + dx, G + 1, vz + dz, "minecraft:stripped_spruce_log", {"axis": as_})
    for dx in range(-3, 3):
        for dz in range(-2, 4):
            vrij.add((vx + dx, vz + dz))

    # --- two small trees ---
    for tx, tz, hoog in ((C - 7, C - 6, 3), (C + 7, C - 8, 4)):
        for y in range(1, hoog + 1):
            s.set(tx, G + y, tz, "minecraft:oak_log", {"axis": "y"})
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(0, 3):
                    ver = abs(dx) + abs(dz) + dy * 1.5
                    if ver <= 3.2 and not (dx == 0 and dz == 0 and dy == 0):
                        if s.get(tx + dx, G + hoog + dy, tz + dz) is None:
                            s.set(tx + dx, G + hoog + dy, tz + dz, "minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                vrij.add((tx + dx, tz + dz))

    # --- what stands and lies where (template coordinates = relative to the island's corner) ---
    bewoners = [
        dict(sleutel="kapitein", bewoner="kapitein", plek=[C + 16.5, G + 1, C + 1.5], yaw=-90.0),
        dict(sleutel="redder", bewoner="redder", plek=[C + 3.5, G + 1, C + 11.5], yaw=160.0, houding="kwispel"),
        dict(sleutel="proef_pup", ras="corgi", kleur="sable", pup=True, plek=[vx + 1.5, G + 1, vz - 1.5], yaw=135.0, houding="zit"),
    ]
    for b in bewoners:
        vrij.add((int(math.floor(b["plek"][0])), int(math.floor(b["plek"][2]))))
    vrij.add((C + 7, C - 4))           # (where the ball is buried)

    # --- flowers and tufts of grass on the green ---
    planten = ["minecraft:short_grass"] * 6 + ["minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:azure_bluet"]
    for (x, z) in sorted(gras):
        if (x, z) in vrij or s.get(x, G, z) != "minecraft:grass_block" or s.get(x, G + 1, z) is not None:
            continue
        if rng.random() < 0.17:
            s.set(x, G + 1, z, rng.choice(planten))

    geuren = [
        dict(id="proef_botje", soort="eten", rang=1, icoon="minecraft:bone"),
        dict(id="proef_bal", soort="voorwerp", rang=1, icoon="minecraft:slime_ball"),
        dict(id="proef_zeelucht", soort="vreemd", rang=1, icoon="minecraft:nautilus_shell"),
    ]
    bronnen = [
        dict(id="proef_botje", geur="proef_botje", plek=[C - 4, G + 1, C + 12], graven=True, bereik=22),
        dict(id="proef_bal", geur="proef_bal", plek=[C + 7, G + 1, C - 4], graven=True, bereik=22),
        dict(id="proef_zeelucht", geur="proef_zeelucht", plek=[C + 15, G + 1, C], graven=False, bereik=14),
    ]
    data = dict(versie=VERSIE, oorsprong=list(OORSPRONG), maat=[SX, SY, SZ],
                stukken=[dict(template="guhs:snuffel/eiland", plek=[0, 0, 0])],
                strand=dict(plek=list(STRAND[0]), yaw=STRAND[1]), haven=dict(plek=list(HAVEN[0]), yaw=HAVEN[1]),
                boom=list(BOOM), boom_draai=0, grens=10, bewoners=bewoners, geuren=geuren, geurbronnen=bronnen)
    problems = controleer(s, data)
    if problems:
        raise SystemExit("snuffel: the test island is not right:\n  " + "\n  ".join(problems))
    return s, data


VAST = ("minecraft:grass_block", "minecraft:sand", "minecraft:dirt_path", "minecraft:spruce_planks", "minecraft:dirt", "minecraft:stone")


def controleer(s, data):
    """Everything that stands somewhere stands on solid ground with room above it; the growth scene's camera has free air."""
    problems = []

    def staat(naam, plek, hoog=2):
        x, y, z = int(math.floor(plek[0])), int(round(plek[1])), int(math.floor(plek[2]))
        if s.get(x, y - 1, z) not in VAST:
            problems.append(f"{naam} at {plek} stands on {s.get(x, y - 1, z)}")
        for dy in range(hoog):
            if s.get(x, y + dy, z) not in (None, "minecraft:short_grass"):
                problems.append(f"{naam} at {plek}: {s.get(x, y + dy, z)} in the way")

    staat("strand", data["strand"]["plek"])
    staat("haven", data["haven"]["plek"])
    staat("boom", [data["boom"][0], data["boom"][1], data["boom"][2]], 3)
    for b in data["bewoners"]:
        staat("bewoner " + b["sleutel"], b["plek"])
    for b in data["geurbronnen"]:
        x, y, z = b["plek"]
        if s.get(x, y - 1, z) is None:
            problems.append(f"geurbron {b['id']} hangs in the air")
    # the camera of the growth scene (feature/snuffel/Boom.java): 12 south of the tree, 6 up, 9 west .. 9 east
    bx, by, bz = data["boom"]
    for dx in range(-9, 10):
        if s.get(bx + dx, by + 6, bz + 12) is not None or s.get(bx + dx, by + 5, bz + 12) is not None:
            problems.append(f"the growth scene's camera path is blocked at x {bx + dx}")
    for t in range(1, 12):                 # ...and nothing between the camera's middle and the tree
        x, y, z = bx, by + 1 + round(5 * t / 12), bz + t
        if s.get(x, y, z) is not None:
            problems.append(f"the view on the tree is blocked at {(x, y, z)} by {s.get(x, y, z)}")
    return problems


# --- the game tests' floor: grass with a rim of sand, nothing on it -----------------------------------------------------------
TEST_MAAT = (21, 5, 21)


def test_vloer(h):
    t = h.Structure(TEST_MAAT)
    for x in range(TEST_MAAT[0]):
        for z in range(TEST_MAAT[2]):
            rand = x < 2 or z < 2 or x >= TEST_MAAT[0] - 2 or z >= TEST_MAAT[2] - 2
            t.set(x, 0, z, "minecraft:sand" if rand else "minecraft:grass_block")
    return t
