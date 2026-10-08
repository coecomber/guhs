"""
biomes3 slice "bouw-wolk1": the three templates (not in FEATURES; bio_bouw_wolk1.py calls them).

Each structure brings its own island, made with the wereld slice's builder (bio_wereld_eiland), and stands in free air
above the meadow of the Wolkenweide (guhs:bio_plek, kind "lucht"). A template starts M blocks under the meadow's top
block of the start column and its start jigsaw sits HOOGTE blocks above that block, in the island's top:

    y 0 .. M-1   the cloud "feet" under the lift columns (only placed where the world has air: processor
                 guhs:wolkenhoeder_hut_wolkvoet, so they never cut into a meadow that lies a little higher)
    y M          the meadow's top block under the anchor: the 3 x 3 guhs:wolkenlift pads lie here, like the natural ones
    y M+1 ..     guhs:wolkenstroom up to the island; the island with its top at M + HOOGTE

  hoeder(h)       wolkenhoeder_hut: the rare big island (38 x 32) with a hill, big trees, a flower meadow, a little lake
                  whose water runs over the south rim, falls into the pool of a catch island and from there into a pool
                  in a cloud; the hut, the fold with the herd, the wolkenhoeder
  sterrenwacht(h) sterrenwacht_ruine: a small high island with the half-fallen observatory
  haven(h)        luchtballon_haven: an island with a T-shaped jetty, the booth and three balloons

All water is written in its END state (sources in the basins, level 1 on the lip, falling water in the drop), every
source is walled in, and a fall lands on source water: when a neighbour update makes the fluid tick, nothing changes.
Each function returns (Structure, info); info holds the spots the Java side and the pictures use.
"""
import math

from features import bio_lib as lib
from features import bio_wereld_eiland as eil

M = 5
HOOGTE = {"wolkenhoeder_hut": 46, "sterrenwacht_ruine": 66, "luchtballon_haven": 30}
GROND = "minecraft:pink_wool"
WATER = "minecraft:water"
BED = "guhs:parelmoer"
KUDDE = 4                       # the fold's herd (Java: Kudde.AANTAL)
KUDDE_TAG = "guhs_bio_bouw_wolk1_kudde"
PLEK = "wolkenhoeder_hut"       # the wolkenhoeder's role by place (NpcRollen)
NAAST = {"west": (-1, 0, "east"), "east": (1, 0, "west"), "north": (0, -1, "south"), "south": (0, 1, "north")}


def blokken(h):
    return {"wit": lib.blok(h, "wolkenblok_wit", "minecraft:white_wool"), "roze": lib.blok(h, "wolkenblok_roze", "minecraft:pink_wool"),
            "wit_plaat": lib.blok(h, "wolkenblok_wit_plaat", "minecraft:smooth_quartz_slab"),
            "bank": lib.blok(h, "wolkenbank", "guhs:guh_bank"), "bed": lib.blok(h, "wolkenbed", "minecraft:white_bed"),
            "lamp": lib.blok(h, "wolkenlamp", "guhs:guh_kristal_lamp"),
            "hek": lib.blok(h, "roze_lakhout_hek", "guhs:vadshout_hek"), "poort": lib.blok(h, "roze_lakhout_poort", "guhs:vadshout_poort"),
            "planken": lib.blok(h, "roze_lakhout_planken", "guhs:guhbloesem_planks"),
            "deur": lib.blok(h, "roze_lakhout_deur", "guhs:vadshout_deur"),
            "balk": lib.blok(h, "roze_lakhout_balk", "guhs:guhbloesem_log")}


def anker(s, x, y, z, naam, final=GROND):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def boom(s, rnd, x, y, z, groot=False, bloesem=False):
    """A tree with its foot ON (x, y, z). Leaves are persistent (nothing in a template may decay)."""
    log = "guhs:guhbloesem_log" if bloesem else "guhs:pluizenboom_stam"
    blad = "guhs:guhbloesem_leaves" if bloesem else "guhs:pluizenboom_bladeren"
    props = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    hoog = int(rnd.integers(6, 9)) if groot else int(rnd.integers(3, 5))
    straal = float(rnd.uniform(3.2, 4.0)) if groot else 2.1
    for yy in range(y + 1, y + 1 + hoog):
        s.set(x, yy, z, log, {"axis": "y"})
    bollen = [(0, 0, 0, 1.0)]
    if groot:
        bollen += [(int(rnd.choice((-2, 2))), -1, int(rnd.choice((-1, 1))), 0.6), (int(rnd.choice((-1, 1))), 2, 0, 0.55)]
    for (ox, oy, oz, f) in bollen:
        rx, ry = straal * f, straal * 0.72 * f
        mx, my, mz = x + ox, y + hoog + oy, z + oz
        for dx in range(-5, 6):
            for dy in range(-4, 5):
                for dz in range(-5, 6):
                    if (dx / rx) ** 2 + (dy / ry) ** 2 + (dz / rx) ** 2 <= 1 and (mx + dx, my + dy, mz + dz) not in s.blocks:
                        s.set(mx + dx, my + dy, mz + dz, blad, dict(props))


def hekken(s, b):
    """Fences join their neighbours (fence, gate, any full block), as the game would on placement."""
    los = {"minecraft:water", "minecraft:air", "guhs:roze_gras", "guhs:roze_guhbloem", "guhs:wolkenstroom", "guhs:bleek_hangmos",
           "guhs:guh_kristal_cluster", b["lamp"], b["bank"], b["bed"], b["deur"], "guhs:mini_luchtballon", "guhs:lampion_roze",
           "guhs:guh_telescoop", "guhs:guh_stoel", "guhs:sterrenlantaarn", "guhs:sterrenwacht_ruine_sterrenkaart", "minecraft:hay_block"}
    for (x, y, z), (naam, props, _) in list(s.blocks.items()):
        if naam != b["hek"]:
            continue
        nieuw = {"waterlogged": "false"}
        for kant, (dx, dz, _) in NAAST.items():
            buur = s.get(x + dx, y, z + dz)
            nieuw[kant] = "true" if buur is not None and buur not in los and "plaat" not in buur and "slab" not in buur and "glass_pane" not in buur else "false"
        s.blocks[(x, y, z)] = (naam, nieuw, None)


def kolom(h, s, tops, cx, cz, top_y, kant, omlaag, bereik=7):
    """A 3 x 3 lift (up) or stream (down) column from the meadow to the island's rim on this side, in the first free spot:
    the whole column free of blocks, and the three rim columns next to it at the island's flat top. Returns (x, z)."""
    dx, dz, kijk = NAAST[kant]
    for afw in sorted(range(-bereik, bereik + 1), key=abs):
        rij = []
        for t in (-1, 0, 1):
            if dx:
                lijn = [x for (x, z) in tops if z == cz + afw + t]
                rij.append((min(lijn) if dx < 0 else max(lijn)) if lijn else None)
            else:
                lijn = [z for (x, z) in tops if x == cx + afw + t]
                rij.append((min(lijn) if dz < 0 else max(lijn)) if lijn else None)
        if None in rij or max(rij) - min(rij) > 1:
            continue
        rand = min(rij) if (dx or dz) < 0 else max(rij)
        kx, kz = (rand + 2 * dx, cz + afw) if dx else (cx + afw, rand + 2 * dz)
        randen = [((r, cz + afw + t) if dx else (cx + afw + t, r)) for t, r in zip((-1, 0, 1), rij)]
        if any(tops.get(p) != top_y for p in randen):
            continue
        boven = top_y + (1 if omlaag else 2)
        if any((kx + a, y, kz + c) in s.blocks for a in (-1, 0, 1) for c in (-1, 0, 1) for y in range(0, boven + 3)):
            continue
        eil.lift(h, s, kx, kz, M, boven, omlaag=omlaag, kijk=kijk)
        return kx, kz
    raise SystemExit(f"bio_bouw_wolk1: no free spot for a {'stream' if omlaag else 'lift'} column on the {kant} side")


def voet(s, b, x, z):
    """The cloud foot under a column: steps of cloud that widen downwards (y below M: only placed in air)."""
    for dy in range(1, M + 1):
        r = 1.6 + dy
        for ax in range(-7, 8):
            for az in range(-7, 8):
                if math.hypot(ax, az) <= r and (x + ax, M - dy, z + az) not in s.blocks:
                    s.set(x + ax, M - dy, z + az, b["wit"])


def mat(s, b, tops, x, z, kant, kleur):
    """A cloud doormat on the rim in front of a column (white where the lift sets you down, pink where the stream starts)."""
    dx, dz, _ = NAAST[kant]
    for t in (-1, 0, 1):
        for d in (2, 3):
            p = (x - d * dx + (t if dz else 0), z - d * dz + (t if dx else 0))
            if p in tops and s.get(p[0], tops[p], p[1]) == GROND:
                s.set(p[0], tops[p], p[1], b[kleur])


def bekken(s, cellen, y, rand, diep=()):
    """Source water in these columns at y (two deep in `diep`), a bed under it, and a wall around it wherever the island
    has none (water never stands next to air). `rand` is the wall block."""
    cellen = set(cellen)
    for (x, z) in cellen:
        s.set(x, y, z, WATER, {"level": "0"})
        if (x, z) in diep:
            s.set(x, y - 1, z, WATER, {"level": "0"})
            s.set(x, y - 2, z, BED)
        else:
            s.set(x, y - 1, z, BED)
        for yy in range(y + 1, y + 4):
            s.blocks.pop((x, yy, z), None)
    for (x, z) in cellen:
        for (dx, dz, _) in NAAST.values():
            p = (x + dx, z + dz)
            if p not in cellen and (p[0], y, p[1]) not in s.blocks:
                s.set(p[0], y, p[1], rand)
                if (p[0], y - 1, p[1]) not in s.blocks:
                    s.set(p[0], y - 1, p[1], rand)


def val(s, kolommen, y_boven, y_water):
    """The fall: level 1 on the lip (y_boven, fed by the source next to it), falling water down to just above the pool."""
    for (x, z) in kolommen:
        s.set(x, y_boven, z, WATER, {"level": "1"})
        for y in range(y_water + 1, y_boven):
            s.set(x, y, z, WATER, {"level": "8"})


def planten(s, rnd, tops, vrij, kans_gras=0.2, kans_bloem=0.05, veldjes=0):
    """Pink grass and guh flowers on the free ground; `veldjes` patches of pink petals (a flower meadow)."""
    kern = [p for p in sorted(tops) if p not in vrij]
    midden = [kern[int(rnd.integers(len(kern)))] for _ in range(veldjes)] if kern else []
    for (x, z), y in sorted(tops.items()):
        if (x, z) in vrij or s.get(x, y, z) != GROND or (x, y + 1, z) in s.blocks:
            continue
        q = float(rnd.random())
        veld = min((math.hypot(x - a, z - c) for (a, c) in midden), default=9.0)
        if veld < 2.6 and q < 0.75:
            s.set(x, y + 1, z, "minecraft:pink_petals", {"facing": ("north", "east", "south", "west")[int(rnd.integers(4))],
                                                         "flower_amount": str(4 if veld < 1.5 else int(rnd.integers(2, 4)))})
        elif q < kans_bloem:
            s.set(x, y + 1, z, "guhs:roze_guhbloem")
        elif q < kans_bloem + kans_gras:
            s.set(x, y + 1, z, "guhs:roze_gras")


def npc(h, s, x, y, z, kind, yaw, plek=None):
    nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": h.ms.Byte(1), "Rotation": h.ms.floats(float(yaw), 0.0)}
    if plek:
        nbt["RoleData"] = {"guhs_plek": plek}
    s.entity(x + 0.5, float(y), z + 0.5, nbt)


# ---------------------------------------------------------------------------------------------------------------------
def hoeder(h):
    naam = "wolkenhoeder_hut"
    b = blokken(h)
    rnd = lib.rng("bio_bouw_wolk1/hoeder")
    X, Y, Z = 63, 80, 72
    s = h.Structure((X, Y, Z))
    cx, cz, top = 31, 27, M + HOOGTE[naam]
    lx, lz = cx + 4, cz + 10                     # the lake
    hut = (cx + 7, cz - 6)                       # north-west corner of the hut (7 x 6), its door looks south at the lake
    wei = (cx - 14, cz - 5, cx - 7, cz + 2)      # the fold (fence line)

    def meer(dx, dz):
        return ((dx - 4) / 5.2) ** 2 + ((dz - 10) / 3.8) ** 2 + 0.25 * math.sin((cx + dx) * 0.9) * math.sin((cz + dz) * 1.1) < 1

    def vlak(dx, dz):
        """0 on the building ground (lake, drain, hut, fold), rising to 1 a few blocks away from it."""
        d = [(math.hypot((dx - 4) / 7.5, (dz - 10) / 6.0) - 0.75) * 3,
             max(abs(dx - 10) - 4.5, abs(dz + 3.5) - 4.0, 0) / 3.0,
             max(abs(dx + 10.5) - 5.5, abs(dz + 1.5) - 5.5, 0) / 3.0,
             max(abs(dx - 4.5) - 2.5, 0) / 2.0 if dz > 10 else 9,
             (math.hypot(dx, dz) - 1.5) / 3.0]
        return max(0.0, min(1.0, min(d)))

    def heuvel(dx, dz):
        hoog = 7.5 * math.exp(-((dx + 2) ** 2 + (dz + 9) ** 2) / (2 * 5.0 ** 2)) + 3.4 * math.exp(-((dx + 14) ** 2 + (dz + 10) ** 2) / (2 * 3.2 ** 2))
        return hoog * vlak(dx, dz)

    # the body in three layers, each smaller and lower than the one above: a broad stepped underside with many drip points
    eil.eiland(h, s, (cx - 1, top - 10, cz - 1), 8.0, 7.0, zaad=naam + "/kern", vorm="rond", diepte=22, punten=3)
    eil.eiland(h, s, (cx - 1, top - 4, cz - 1), 13.5, 11.0, zaad=naam + "/midden", vorm="rond", diepte=19, punten=5)
    tops = eil.eiland(h, s, (cx, top, cz), 19.5, 15.5, zaad=naam, vorm="rond", diepte=22, punten=6, relief=heuvel)
    assert tops[(cx, cz)] == top, "the anchor column is flat"

    # ---- the lake, its drain to the south rim and the first fall ---------------------------------------------------
    water = {(x, z) for (x, z) in tops if meer(x - cx, z - cz)}
    diep = {p for p in water if all((p[0] + a, p[1] + c) in water for a in (-1, 0, 1) for c in (-1, 0, 1))}
    rand_z = min(max(z for (x, z) in tops if x == xx) for xx in (lx, lx + 1))
    goot = {(xx, z) for xx in (lx, lx + 1) for z in range(lz, rand_z + 1)}
    assert all(tops.get(p) == top for p in water | goot), "the lake lies on flat ground"
    bekken(s, water | goot, top, GROND, diep)
    # ---- the catch island under the rim, its pool, its drain and the second fall -----------------------------------
    vang_y, vz = top - 14, rand_z + 3
    vtops = eil.eiland(h, s, (lx, vang_y, vz), 6.2, 5.2, zaad=naam + "/vang", vorm="rond", diepte=10, punten=2)
    poel = {(x, z) for (x, z) in vtops if math.hypot(x - (lx + 0.5), z - vz) < 2.7}
    vrand_z = min(max(z for (x, z) in vtops if x == xx) for xx in (lx, lx + 1))
    vgoot = {(xx, z) for xx in (lx, lx + 1) for z in range(vz, vrand_z + 1)}
    bekken(s, poel | vgoot, vang_y, GROND)
    val(s, [(lx, rand_z + 1), (lx + 1, rand_z + 1)], top, vang_y)
    assert all((xx, rand_z + 1) in poel for xx in (lx, lx + 1)), "the first fall lands in the pool"
    # ---- the cloud that catches the second fall: a pool in white cloud ---------------------------------------------
    wolk_y, wz = vang_y - 11, vrand_z + 1
    eil.wolk(h, s, (lx + 0.5, wolk_y - 1, wz + 1), 7.5, 2.2, 6.0, zaad=naam + "/vangwolk")
    kom = {(x, z) for x in range(lx - 2, lx + 4) for z in range(wz - 2, wz + 4) if math.hypot(x - (lx + 0.5), z - (wz + 0.5)) < 2.6}
    for (x, z) in kom:
        s.set(x, wolk_y, z, WATER, {"level": "0"})
        s.set(x, wolk_y - 1, z, b["wit"])
        for yy in range(wolk_y + 1, wolk_y + 5):
            s.blocks.pop((x, yy, z), None)
    for (x, z) in kom:
        for (dx, dz, _) in NAAST.values():
            if (x + dx, z + dz) not in kom:
                s.set(x + dx, wolk_y, z + dz, b["wit"])
    val(s, [(lx, vrand_z + 1), (lx + 1, vrand_z + 1)], vang_y, wolk_y)
    assert all((xx, vrand_z + 1) in kom for xx in (lx, lx + 1)), "the second fall lands in the cloud pool"
    # (nothing may hang in a fall)
    for (xx, zz, y0, y1) in ((lx, rand_z + 1, vang_y + 1, top), (lx + 1, rand_z + 1, vang_y + 1, top),
                             (lx, vrand_z + 1, wolk_y + 1, vang_y), (lx + 1, vrand_z + 1, wolk_y + 1, vang_y)):
        for y in range(y0, y1 + 1):
            assert s.get(xx, y, zz) == WATER, (xx, y, zz, s.get(xx, y, zz))

    # ---- clouds around the island ----------------------------------------------------------------------------------
    eil.wolk(h, s, (cx - 21, top - 9, cz + 16), 6.5, 2.0, 5.0, zaad=naam + "/wolk1")
    eil.wolk(h, s, (cx - 6, top - 30, cz + 22), 8.0, 2.4, 6.0, zaad=naam + "/wolk4")
    eil.wolk(h, s, (cx + 19, top - 31, cz + 12), 6.0, 2.0, 5.0, zaad=naam + "/wolk5", kleur="roze")
    eil.wolk(h, s, (cx + 22, top - 17, cz - 12), 6.0, 2.0, 4.5, zaad=naam + "/wolk2", kleur="roze")
    eil.wolk(h, s, (cx - 16, top - 24, cz - 18), 7.0, 2.2, 5.0, zaad=naam + "/wolk3")

    # ---- the hut: lacquered wood under a roof of cloud --------------------------------------------------------------
    hx, hz = hut
    for x in range(hx, hx + 7):
        for z in range(hz, hz + 6):
            assert tops[(x, z)] == top, "the hut stands on flat ground"
            s.set(x, top, z, "guhs:guhbloesem_planks")
            rand = x in (hx, hx + 6) or z in (hz, hz + 5)
            hoek = x in (hx, hx + 6) and z in (hz, hz + 5)
            for y in range(top + 1, top + 4):
                if hoek:
                    s.set(x, y, z, b["balk"], {"axis": "y"})
                elif rand:
                    s.set(x, y, z, b["planken"])
                else:
                    s.blocks.pop((x, y, z), None)
    for (x, z) in ((hx, hz + 2), (hx, hz + 3), (hx + 6, hz + 2), (hx + 6, hz + 3), (hx + 2, hz), (hx + 4, hz), (hx + 1, hz + 5), (hx + 5, hz + 5)):
        s.set(x, top + 2, z, "minecraft:pink_stained_glass")
    for y, half in ((top + 1, "lower"), (top + 2, "upper")):
        s.set(hx + 3, y, hz + 5, b["deur"], {"facing": "north", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    # the roof: a fat white cloud lying on the walls, a pink tuft on top
    for x in range(hx - 1, hx + 8):
        for z in range(hz - 1, hz + 7):
            buiten = x in (hx - 1, hx + 7) or z in (hz - 1, hz + 6)
            hoek = x in (hx - 1, hx + 7) and z in (hz - 1, hz + 6)
            if not hoek:
                s.set(x, top + 4, z, b["wit_plaat"] if buiten else b["wit"], {"type": "bottom", "waterlogged": "false"} if buiten else None)
            if not buiten and 1 <= x - hx <= 5 and 1 <= z - hz <= 4:
                s.set(x, top + 5, z, b["wit"])
    for (x, z) in ((hx + 2, hz + 2), (hx + 3, hz + 2), (hx + 3, hz + 3), (hx + 4, hz + 3)):
        s.set(x, top + 6, z, b["roze"])
    # inside: a cloud bed, a table to make cloud blocks at, a lamp
    s.set(hx + 1, top + 1, hz + 2, b["bed"], {"facing": "north", "part": "foot", "occupied": "false"})
    s.set(hx + 1, top + 1, hz + 1, b["bed"], {"facing": "north", "part": "head", "occupied": "false"})
    s.set(hx + 5, top + 1, hz + 1, "minecraft:crafting_table")
    s.set(hx + 5, top + 1, hz + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(hx + 3, top + 3, hz + 2, b["lamp"])
    s.set(hx + 4, top + 1, hz + 1, "guhs:potted_roze_guhbloem")
    # the doorstep and a cloud bench by the lake
    s.set(hx + 3, top, hz + 6, b["wit"])
    s.set(hx + 5, top + 1, hz + 7, b["bank"], {"facing": "south"})
    s.set(hx + 6, top + 1, hz + 7, b["bank"], {"facing": "south"})
    s.set(hx + 1, top + 1, hz + 6, b["lamp"])

    # ---- the fold: a fence, a gate on the east side, hay, a trough, lamps on the corner posts ------------------------
    x0, z0, x1, z1 = wei
    poort = (x1, (z0 + z1) // 2)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            assert tops[(x, z)] == top, "the fold lies on flat ground"
            if x in (x0, x1) or z in (z0, z1):
                if (x, z) == poort:
                    s.set(x, top + 1, z, b["poort"], {"facing": "east", "open": "false", "powered": "false", "in_wall": "false"})
                else:
                    s.set(x, top + 1, z, b["hek"])
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        s.set(x, top + 2, z, b["lamp"])
    s.set(x0 + 1, top + 1, z0 + 1, "minecraft:hay_block", {"axis": "y"})
    s.set(x0 + 2, top + 1, z0 + 1, "minecraft:hay_block", {"axis": "x"})
    s.set(x0 + 1, top + 1, z1 - 1, "minecraft:water_cauldron", {"level": "3"})
    schapen = [(x0 + 3, z0 + 2), (x0 + 5, z0 + 3), (x0 + 3, z1 - 2), (x0 + 5, z1 - 2)][:KUDDE]
    for i, (x, z) in enumerate(schapen):
        s.entity(x + 0.5, float(top + 1), z + 0.5, {"id": "guhs:wolkenschaapje", "PersistenceRequired": h.ms.Byte(1), "Gehouden": h.ms.Byte(1),
                                                     "Tags": h.ms.NbtList(8, [KUDDE_TAG]), "Rotation": h.ms.floats(90.0 * i, 0.0)})
    hoeder_plek = (x1 + 2, top + 1, poort[1])
    npc(h, s, *hoeder_plek, "wolkenhoeder", -90, plek=PLEK)
    s.set(x1 + 2, top + 1, poort[1] - 2, b["bank"], {"facing": "east"})

    # ---- trees: three big ones on and around the hill, small ones near the rim ---------------------------------------
    vrij = set(water | goot)
    vrij |= {(x, z) for x in range(hx - 2, hx + 9) for z in range(hz - 2, hz + 9)}
    vrij |= {(x, z) for x in range(x0 - 1, x1 + 4) for z in range(z0 - 1, z1 + 2)}
    for (dx, dz, groot, bloesem) in ((-8, -11, True, False), (4, -9, True, True), (-17, -8, True, False), (14, 4, False, True),
                                     (-9, 6, False, False), (13, -10, False, True), (-5, 13, False, True), (16, -3, False, False),
                                     (12, 13, False, False)):
        p = (cx + dx, cz + dz)
        if p in tops and p not in vrij:
            boom(s, rnd, p[0], tops[p], p[1], groot, bloesem)
            vrij |= {(p[0] + a, p[1] + c) for a in (-1, 0, 1) for c in (-1, 0, 1)}

    # ---- the way up and the way down -------------------------------------------------------------------------------
    lift = kolom(h, s, tops, cx, cz, top, "west", False)
    stroom = kolom(h, s, tops, cx, cz, top, "east", True)
    voet(s, b, *lift)
    voet(s, b, *stroom)
    mat(s, b, tops, *lift, "west", "wit")
    mat(s, b, tops, *stroom, "east", "roze")
    planten(s, rnd, tops, vrij | {(x, z) for (x, z) in tops if s.get(x, tops[(x, z)], z) != GROND}, kans_gras=0.2, kans_bloem=0.1, veldjes=7)
    planten(s, rnd, vtops, poel | vgoot, kans_gras=0.25, kans_bloem=0.1)
    hekken(s, b)
    anker(s, cx, top, cz, naam)
    return s, {"midden": (cx, top, cz), "hoeder": hoeder_plek, "schapen": [(x, top + 1, z) for (x, z) in schapen], "lift": lift, "stroom": stroom,
               "meer": sorted(water | goot), "poel": sorted(poel | vgoot), "kom": sorted(kom), "vang_y": vang_y, "wolk_y": wolk_y,
               "val1": [(lx, rand_z + 1), (lx + 1, rand_z + 1)], "val2": [(lx, vrand_z + 1), (lx + 1, vrand_z + 1)],
               "hut": (hx, top, hz), "wei": wei, "tops": tops, "vtops": vtops}


# ---------------------------------------------------------------------------------------------------------------------
def sterrenwacht(h):
    naam = "sterrenwacht_ruine"
    b = blokken(h)
    rnd = lib.rng("bio_bouw_wolk1/sterrenwacht")
    X, Y, Z = 39, 92, 37
    s = h.Structure((X, Y, Z))
    cx, cz, top = 19, 18, M + HOOGTE[naam]
    tops = eil.eiland(h, s, (cx, top, cz), 10.5, 9.5, zaad=naam, vorm="rond", diepte=17, punten=3)
    tx, tz = cx, cz - 1                          # the tower's middle
    steen = ["guhs:knuffelsteen", "guhs:knuffelsteen", "guhs:gladde_knuffelsteen", "minecraft:calcite"]

    def muurhoogte(hoek):
        """The ruin's wall line: whole on the north-west, fallen to knee height on the south-east."""
        heel = 0.5 + 0.5 * math.cos(hoek - math.radians(225))
        return max(1, int(round(1.5 + 4.5 * heel + 1.2 * math.sin(hoek * 3 + 1.0))))

    for dx in range(-6, 7):
        for dz in range(-6, 7):
            d = math.hypot(dx, dz)
            x, z = tx + dx, tz + dz
            if d <= 4.6 and (x, z) in tops:
                s.set(x, top, z, "guhs:parelmoer_tegels" if (dx + dz) % 2 == 0 else "guhs:gladde_knuffelsteen")
            if 3.6 < d <= 4.6:
                hoek = math.atan2(dz, dx)
                deur = abs(dx) <= 0 and dz > 0
                for y in range(top + 1, top + 1 + muurhoogte(hoek)):
                    if deur and y <= top + 2:
                        continue
                    s.set(x, y, z, steen[int(rnd.integers(len(steen)))])
    # the dome: only the north-west half is left, a rib of glass in it
    for dx in range(-6, 7):
        for dy in range(0, 7):
            for dz in range(-6, 7):
                d = math.sqrt(dx * dx + (dy * 1.15) ** 2 + dz * dz)
                hoek = math.atan2(dz, dx)
                heel = 0.5 + 0.5 * math.cos(hoek - math.radians(225))
                if 3.9 < d <= 4.9 and heel > 0.62 - dy * 0.03 and float(rnd.random()) < 0.5 + heel * 0.6:
                    s.set(tx + dx, top + 6 + dy, tz + dz, "minecraft:light_blue_stained_glass" if (dx + dz) % 4 == 0 else "guhs:parelmoer")
    # rubble where the dome came down
    for (dx, dz, blok) in ((5, 4, "guhs:parelmoer"), (6, 2, "guhs:knuffelsteen"), (4, 6, "guhs:knuffelsteen_plaat"), (7, 4, "guhs:knuffelsteen_plaat"),
                           (2, 2, "guhs:knuffelsteen_plaat"), (6, 5, "guhs:parelmoer")):
        p = (tx + dx, tz + dz)
        if p in tops and (p[0], tops[p] + 1, p[1]) not in s.blocks:
            s.set(p[0], tops[p] + 1, p[1], blok, {"type": "bottom", "waterlogged": "false"} if blok.endswith("plaat") else None)
    for (dx, dz) in ((-2, 2), (1, -2), (3, 1), (-1, -3)):
        if (tx + dx, top + 1, tz + dz) not in s.blocks:
            s.set(tx + dx, top + 1, tz + dz, "guhs:bleekmos_tapijt")
    # inside: the telescope on a plinth, the star chart on the whole wall, the chair, a star lantern
    s.set(tx, top + 1, tz, "guhs:gladde_knuffelsteen")
    telescoop = (tx, top + 2, tz)
    s.set(*telescoop, "guhs:guh_telescoop", {"facing": "north"})
    s.set(tx + 1, top + 1, tz, "guhs:knuffelsteen_trap", {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    kaart = (tx, top + 3, tz - 3)
    s.set(tx, top + 3, tz - 3, "guhs:sterrenwacht_ruine_sterrenkaart", {"facing": "south"})
    for dx in (-1, 1):
        s.set(tx + dx, top + 3, tz - 3, "guhs:lampion_roze", {"hanging": "false"})
        s.set(tx + dx, top + 2, tz - 3, "guhs:knuffelsteen_plaat", {"type": "top", "waterlogged": "false"})
    stoel = (tx - 2, top + 1, tz - 1)
    s.set(*stoel, "guhs:guh_stoel", {"facing": "east"})
    npc(h, s, stoel[0], stoel[1] + 0.35, stoel[2], "sterrenwacht_ruine_sterrenkijker", -90)
    s.set(tx + 2, top + 1, tz - 2, "guhs:sterrenlantaarn")
    s.set(tx - 2, top + 1, tz + 1, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"})
    vrij = {(tx + dx, tz + dz) for dx in range(-6, 7) for dz in range(-6, 7) if math.hypot(dx, dz) <= 5.6}
    for (dx, dz, bloesem) in ((8, -3, True), (-7, 5, False)):
        p = (cx + dx, cz + dz)
        if p in tops and p not in vrij and tops[p] == top:
            boom(s, rnd, p[0], top, p[1], False, bloesem)
            vrij.add(p)
    lift = kolom(h, s, tops, cx, cz, top, "south", False)
    stroom = kolom(h, s, tops, cx, cz, top, "east", True)
    voet(s, b, *lift)
    voet(s, b, *stroom)
    mat(s, b, tops, *lift, "south", "wit")
    mat(s, b, tops, *stroom, "east", "roze")
    eil.wolk(h, s, (cx - 12, top - 6, cz + 9), 5.0, 1.8, 4.0, zaad=naam + "/wolk")
    planten(s, rnd, tops, vrij, kans_gras=0.25, kans_bloem=0.06)
    hekken(s, b)
    anker(s, cx, top, cz, naam, "guhs:parelmoer_tegels")
    return s, {"midden": (cx, top, cz), "telescoop": telescoop, "stoel": stoel, "kaart": kaart, "lift": lift, "stroom": stroom, "tops": tops}


# ---------------------------------------------------------------------------------------------------------------------
def haven(h):
    naam = "luchtballon_haven"
    b = blokken(h)
    rnd = lib.rng("bio_bouw_wolk1/haven")
    X, Y, Z = 43, 58, 49
    s = h.Structure((X, Y, Z))
    cx, cz, top = 21, 17, M + HOOGTE[naam]
    tops = eil.eiland(h, s, (cx, top, cz), 11.0, 9.0, zaad=naam, vorm="rond", diepte=15, punten=3)
    rand = max(z for (x, z) in tops if x == cx)
    eind = rand + 11                                         # the jetty's last row
    # the jetty: three wide, a T at its end with a mooring on each arm
    for z in range(rand - 1, eind + 1):
        for x in range(cx - 1, cx + 2):
            s.set(x, top, z, "guhs:guhbloesem_planks")
    for x in range(cx - 4, cx + 5):
        for z in range(eind - 2, eind + 1):
            s.set(x, top, z, "guhs:guhbloesem_planks")
    for z in range(rand + 2, eind - 2, 3):                   # posts under it and a rail along it
        for x in (cx - 1, cx + 1):
            for y in range(top - 3, top):
                s.set(x, y, z, b["hek"])
    for z in range(rand + 1, eind - 2):
        for x in (cx - 1, cx + 1):
            if (z - rand) % 2 == 1:
                s.set(x, top + 1, z, b["hek"])
    for x in (cx - 4, cx + 4):                               # (a lantern post on the island side of each arm only: the balloon leaves south)
        s.set(x, top + 1, eind - 2, b["hek"])
        s.set(x, top + 2, eind - 2, "guhs:lampion_roze", {"hanging": "false"})
    steigers = [(cx + 3, eind - 1), (cx - 3, eind - 1)]
    for (x, z) in steigers:
        s.set(x, top, z, "guhs:ballonsteiger")
    # the balloons: the one that flies on the east arm (it looks south, out over the meadow), a moored one on the west arm,
    # a third tied to a post on the island
    ballon = (cx + 3, top + 1, eind - 1)
    s.entity(ballon[0] + 0.5, float(ballon[1]), ballon[2] + 0.5, {"id": "guhs:luchtballon_haven_ballon", "Kleur": 1, "Rotation": h.ms.floats(0.0, 0.0)})
    s.entity(cx - 3 + 0.5, float(top + 1), eind - 1 + 0.5, {"id": "guhs:guh_luchtballon", "Kleur": 0, "Deco": h.ms.Byte(1), "Rotation": h.ms.floats(0.0, 0.0)})
    paal = (cx - 7, cz + 1)
    for y in range(top + 1, top + 5):
        s.set(paal[0], y, paal[1], b["hek"])
    s.entity(paal[0] + 0.5, float(top + 5), paal[1] + 0.5, {"id": "guhs:guh_luchtballon", "Kleur": 2, "Deco": h.ms.Byte(1), "Rotation": h.ms.floats(90.0, 0.0)})
    # the booth: a counter under a striped awning, the ballonvaarder behind it looking at the jetty
    kx, kz = cx + 4, cz + 2
    for x in range(kx, kx + 4):
        for z in range(kz, kz + 3):
            s.set(x, top, z, "guhs:guhbloesem_planks")
    for (x, z) in ((kx, kz), (kx + 3, kz), (kx, kz + 2), (kx + 3, kz + 2)):
        for y in range(top + 1, top + 4):
            s.set(x, y, z, b["hek"])
    for x in range(kx - 1, kx + 5):
        for z in range(kz - 1, kz + 4):
            s.set(x, top + 4, z, "minecraft:pink_wool" if (x + z) % 2 == 0 else "minecraft:white_wool")
    for x in (kx + 1, kx + 2):
        s.set(x, top + 1, kz + 2, b["planken"])
    s.set(kx + 1, top + 2, kz + 2, "guhs:mini_luchtballon")
    vaarder = (kx + 2, top + 1, kz + 1)
    npc(h, s, *vaarder, "ballonvaarderguh", 0)
    s.set(cx - 3, top + 1, rand - 2, b["bank"], {"facing": "south"})
    s.set(cx - 4, top + 1, rand - 2, b["bank"], {"facing": "south"})
    s.set(cx + 2, top + 1, rand - 1, b["lamp"])
    vrij = {(x, z) for x in range(kx - 2, kx + 6) for z in range(kz - 2, kz + 5)} | {(x, z) for x in range(cx - 5, cx + 4) for z in range(rand - 3, rand + 1)}
    vrij.add(paal)
    for (dx, dz, bloesem) in ((-6, -5, True), (5, -6, False)):
        p = (cx + dx, cz + dz)
        if p in tops and tops[p] == top:
            boom(s, rnd, p[0], top, p[1], False, bloesem)
            vrij.add(p)
    lift = kolom(h, s, tops, cx, cz, top, "west", False)
    stroom = kolom(h, s, tops, cx, cz, top, "north", True)
    voet(s, b, *lift)
    voet(s, b, *stroom)
    mat(s, b, tops, *lift, "west", "wit")
    mat(s, b, tops, *stroom, "north", "roze")
    eil.wolk(h, s, (cx + 13, top - 5, cz - 8), 5.5, 1.8, 4.0, zaad=naam + "/wolk", kleur="roze")
    planten(s, rnd, tops, vrij, kans_gras=0.25, kans_bloem=0.08)
    hekken(s, b)
    anker(s, cx, top, cz, naam)
    return s, {"midden": (cx, top, cz), "ballon": ballon, "vaarder": vaarder, "steigers": steigers, "lift": lift, "stroom": stroom, "tops": tops,
               "deco": [(cx - 3, top + 1, eind - 1), (paal[0], top + 5, paal[1])]}


def controleer_water(s, naam):
    """Every water block is closed in: a source has a solid block or water on all four sides and below; flowing and falling
    water has water above or beside it and water or a solid block below."""
    fout = []
    for (x, y, z), (blok, props, _) in s.blocks.items():
        if blok != WATER:
            continue
        onder = s.get(x, y - 1, z)
        if props["level"] == "0":
            for (dx, dz, _) in NAAST.values():
                buur = s.get(x + dx, y, z + dz)
                if buur is None:
                    fout.append(f"source at {x} {y} {z} is open to the side")
                elif buur == WATER and s.blocks[(x + dx, y, z + dz)][1]["level"] not in ("0", "1"):
                    fout.append(f"source at {x} {y} {z} next to a fall")
            if onder is None:
                fout.append(f"source at {x} {y} {z} has nothing under it")
        elif onder is None:
            fout.append(f"water at {x} {y} {z} falls into nothing")
    if fout:
        raise SystemExit(f"bio_bouw_wolk1 {naam}: water is not closed in:\n  " + "\n  ".join(fout[:20]))
