"""Zone 5 "Hoogvads": the GAME parts on top of the approved render kit (night round T, T-Z5WERELD).

Works in the kit's LOCAL coordinates (head = +x, axis z = 0, y = render y; plan5.spel turns it into the game). Called by maak5:
    vooraf(W)          before the city is built: records the kit's knights/NPCs (for the townsfolk spawns), patches the kit
    bouw(W, S, log)    after the city: interiors with real floors + doors, roles of houses (inn, bank, shops), stairs from every
                       door, the palace/council/university/academy/station/soup kitchen/cathedral made walkable, the airport
                       piers for the airships, railings + waterfall grilles, campfires, the whale skin detail, the university
                       wall depth, the rooms inside the whale; returns info (plekken, huizen, bewoners, uitkijken, vangnet).
Every block goes through the kit (palet.P keys or literal states), so the palette rules stay in one place.
"""
import math

import numpy as np

import palet
from palet import P, trap, plaat
import bouwhulp as BH

LUCHT = "minecraft:air"
NPCS = []          # (soort, x, y, z, yaw) of the kit's BH.npc calls (local)
RIDDERS = set()    # indices into W.guhs that are knights
HUIZEN = []        # every house of huizen2 (local): dict


# =============================================================================================== hooks into the kit
def vooraf(W):
    import figuren as FG
    import huizen2 as H2
    NPCS.clear()
    RIDDERS.clear()
    HUIZEN.clear()
    org_ridder, org_npc, org_huis = FG.ridder, BH.npc, H2.huis2

    def ridder(W_, x, y, z, *a, **kw):
        n0 = len(W_.guhs)
        org_ridder(W_, x, y, z, *a, **kw)
        RIDDERS.update(range(n0, len(W_.guhs)))

    def npc(W_, soort, x, y, z, yaw=180, schaal=1.0):
        vx, vy, vz = W_.naar_voxel((x, y, z))
        b = W_.b
        NPCS.append((soort, vx + b[0], vy + b[1], vz + b[2], yaw))
        org_npc(W_, soort, x, y, z, yaw=yaw, schaal=schaal)

    def huis2(W_, x1, z1, x2, z2, y0, verd=3, seed=0, voor="south", winkel=False, luifel=None):
        rng = np.random.RandomState(abs(int(seed)) % (2 ** 31))
        rng.randint(16)
        rng.rand()
        hv = int(rng.choice([4, 5, 5]))      # the same draws as huis2 (gevel, rand, hv)
        y_top = org_huis(W_, x1, z1, x2, z2, y0, verd=verd, seed=seed, voor=voor, winkel=winkel, luifel=luifel)
        HUIZEN.append(dict(x1=x1, z1=z1, x2=x2, z2=z2, y0=y0, hv=hv, verd=verd, voor=voor, winkel=winkel, y_top=y_top))
        return y_top
    FG.ridder = ridder
    BH.npc = npc
    H2.huis2 = huis2
    import kathedraal as KA
    KA.BH.npc = npc
    import wijken as WK
    WK.BH.npc = npc
    WK.FG.ridder = ridder
    import markt2 as MK2
    MK2.BH.npc = npc
    import gebouwen as GB
    GB.FG.ridder = ridder


# =============================================================================================== small helpers
def zet(W, x, y, z, b):
    BH.zet(W, x, y, z, b)


def doos(W, x1, y1, z1, x2, y2, z2, b):
    BH.doos(W, x1, y1, z1, x2, y2, z2, b)


def leeg(W, x1, y1, z1, x2, y2, z2):
    BH.leeg(W, x1, y1, z1, x2, y2, z2)


def naam(W, x, y, z):
    return W.wget(x, y, z).split("[")[0]


def is_lucht(W, x, y, z):
    return naam(W, x, y, z) in (LUCHT, "minecraft:cave_air")


def deur(W, x, y, z, facing, soort="cherry", hinge="left"):
    """A real (closed) door, lower half at y."""
    zet(W, x, y, z, f"minecraft:{soort}_door[facing={facing},half=lower,hinge={hinge},open=false,powered=false]")
    zet(W, x, y + 1, z, f"minecraft:{soort}_door[facing={facing},half=upper,hinge={hinge},open=false,powered=false]")


def hanglamp(W, x, y_plafond, z, keten=1):
    for k in range(keten):
        zet(W, x, y_plafond - 1 - k, z, "minecraft:iron_chain[axis=y,waterlogged=false]")
    zet(W, x, y_plafond - 1 - keten, z, "minecraft:lantern[hanging=true,waterlogged=false]")


def bed(W, x, y, z, facing, kleur="pink"):
    """Bed with its foot at (x, y, z), head one block in 'facing'."""
    dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
    zet(W, x, y, z, f"minecraft:{kleur}_bed[facing={facing},occupied=false,part=foot]")
    zet(W, x + dx, y, z + dz, f"minecraft:{kleur}_bed[facing={facing},occupied=false,part=head]")


TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}
RICHTING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def trap_af(W, S, x, z, y_vloer, uit, breed=1, max_k=12, blok="wit_trap", vul="steen_bak"):
    """From a door/landing (floor block at y_vloer, door cell at (x, z)) a stair OUT (direction 'uit') to the city ground:
    down when the ground outside is lower (each stair supported to the ground), up when it is higher (a cutting through the
    ground with 3 blocks of headroom). Stops where the ground meets the stair."""
    dx, dz = RICHTING[uit]
    zx, zz = (abs(dz), abs(dx))     # sideways
    cur = y_vloer
    for k in range(1, max_k + 1):
        cx, cz = x + dx * k, z + dz * k
        g = S.g(cx, cz) if S is not None else None
        if g is None:
            return
        if g == cur:
            return
        for s_ in range(-(breed // 2), breed - breed // 2):
            px, pz = cx + zx * s_, cz + zz * s_
            if g < cur:           # down: a stair at cur (its top = the floor height), supported
                h = cur
                if g < h - 1:
                    doos(W, px, g + 1, pz, px, h - 1, pz, vul)
                zet(W, px, h, pz, trap(blok, TEGEN[uit]))
                leeg(W, px, h + 1, pz, px, h + 3, pz)
            else:                 # up: a stair at cur + 1 (high side outward), the ground above it cut away
                h = cur + 1
                zet(W, px, h, pz, trap(blok, uit))
                leeg(W, px, h + 1, pz, px, h + 3, pz)
        cur = cur - 1 if g < cur else cur + 1
        if g < cur + 1 and g >= cur:   # reached the ground level
            return


# =============================================================================================== houses
ROLLEN = [   # role, how many, preferred spot (local) -> the nearest suitable house gets it
    ("herberg", "herberg_hemelbed", (-60, 0, 40), True),
    ("bank", "bank_hoogvads", (-60, 0, -30), True),
    ("winkel", "winkel_kleding", (-20, 0, -40), False),
    ("winkel", "winkel_smid", (-20, 0, 40), False),
    ("winkel", "winkel_bakker", (30, 0, -40), False),
    ("winkel", "winkel_wapens", (30, 0, 40), False),
    ("winkel", "winkel_kruiden", (-160, 0, -60), False),
    ("winkel", "winkel_juwelen", (-160, 0, 60), False),
    ("winkel", "winkel_kaarten", (140, 0, -40), False),
    ("winkel", "winkel_speelgoed", (140, 0, 40), False),
]


def huizen(W, S, info):
    """Every house: a real door, a hollow ground floor with a floor, a lamp and furniture by role, stairs down from the door."""
    rng = np.random.RandomState(51)
    # roles: the nearest unclaimed house (shops: one with a shop front)
    rol_van = {}
    for (rol, hid, (px, _, pz), groot) in ROLLEN:
        best, bi = 1e9, None
        for i, h in enumerate(HUIZEN):
            if i in rol_van:
                continue
            w = h["x2"] - h["x1"] + 1
            d_ = h["z2"] - h["z1"] + 1
            if groot and w * d_ < 70:
                continue
            if rol == "winkel" and not h["winkel"] and any(hh["winkel"] and j not in rol_van for j, hh in enumerate(HUIZEN)):
                continue
            cx, cz = (h["x1"] + h["x2"]) / 2, (h["z1"] + h["z2"]) / 2
            dd = math.hypot(cx - px, cz - pz)
            if dd < best:
                best, bi = dd, i
        if bi is not None:
            rol_van[bi] = (rol, hid)
    out = []
    n_woning = 0
    for i, h in enumerate(HUIZEN):
        rol, hid = rol_van.get(i, ("woning", None))
        if hid is None:
            n_woning += 1
            hid = f"woning_{n_woning}"
        r = huis_binnen(W, S, h, rol, rng)
        if r is None:
            continue
        deur_p, binnen_p, toon_p = r
        out.append(dict(id=hid, rol=rol, deur=deur_p, binnen=binnen_p, toon=toon_p))
        info["plekken"][f"huis_{hid}"] = binnen_p
        if rol != "woning":
            info["plekken"][f"{hid}_balie"] = toon_p
            info["plekken"][f"{hid}_deur"] = deur_p
    info["huizen_lokaal"] = out
    return out


def huis_binnen(W, S, h, rol, rng):
    import huizen2 as H2
    K = H2.Kavel(h["x1"], h["z1"], h["x2"], h["z2"], h["voor"])
    w, d, y0, hv = K.w, K.d, h["y0"], h["hv"]
    if w < 5 or d < 5:
        return None
    yt = y0 + hv - 1             # highest interior block of the ground floor (y0 + hv = the band/next floor)

    def p(u, y, v):
        return K.p(u, y, v)

    def z_(u, y, v, b):
        x, yy, z = K.p(u, y, v)
        zet(W, x, yy, z, b)
    # hollow ground floor + floor + walls stay
    for u in range(1, w - 1):
        for v in range(1, d - 1):
            for y in range(y0 + 1, yt + 1):
                z_(u, y, v, LUCHT)
            z_(u, y0, v, "minecraft:stripped_cherry_wood[axis=y]" if rol in ("herberg", "bank") else
               ("minecraft:pale_oak_planks" if (u + v) % 2 else "minecraft:cherry_planks"))
    # ceiling under the next floor (the band at y0+hv is solid already) + a lamp
    cx, cz = (w - 1) // 2, (d - 1) // 2
    x, _, z = p(cx, 0, max(2, d // 2))
    hanglamp(W, x, y0 + hv, z, keten=0 if hv <= 4 else 1)
    # the door (replaces the fake plank door of huis2): opens out to the street
    du = w // 2
    dx_, dy_, dz_ = p(du, y0 + 1, 0)
    deur(W, dx_, dy_, dz_, K.uit)
    # inside of the door free
    z_(du, y0 + 1, 1, LUCHT)
    z_(du, y0 + 2, 1, LUCHT)
    # outside of the door free (huis2 may put a shutter of the neighbouring window or a lantern there)
    for y in range(y0 + 1, y0 + 4):
        b_ = naam(W, *p(du, y, -1))
        if b_ != LUCHT and ("trapdoor" in b_ or "lantern" in b_ or "leaves" in b_ or "fence" in b_):
            z_(du, y, -1, LUCHT)
    # stairs down to the street when the street lies lower
    x0, _, z0 = p(du, 0, 0)
    trap_af(W, S, x0, z0, y0, K.uit, breed=1, max_k=10)
    binnen = list(p(cx, y0 + 1, max(1, d // 2)))
    toon = binnen
    # furniture along the walls (never in front of the door: v >= 2 on the door side)
    if rol == "woning":
        bed(W, *p(1, y0 + 1, d - 3), facing=_dir(K, "achter"), kleur=["pink", "white", "light_blue", "magenta"][rng.randint(4)])
        z_(w - 2, y0 + 1, d - 2, "minecraft:barrel[facing=up,open=false]")
        z_(w - 2, y0 + 1, d - 3, "minecraft:bookshelf")
        z_(w - 2, y0 + 2, d - 2, "minecraft:potted_pink_tulip")
        if w >= 7:
            z_(w - 3, y0 + 1, 2, "minecraft:pale_oak_fence")
            z_(w - 3, y0 + 2, 2, "minecraft:white_carpet")
        z_(cx, y0 + 1, d - 2 if d > 6 else 2, "minecraft:pink_carpet")
    else:
        # counter across the room at v = 3 (or the middle), with a gap at the side; the keeper stands behind it
        vb = min(3, d - 3)
        for u in range(1, w - 1):
            if u == w - 2:
                continue
            z_(u, y0 + 1, vb, "minecraft:stripped_cherry_wood[axis=y]" if rol != "bank" else P["wit_glad"])
            if rol == "bank" and u % 2:
                z_(u, y0 + 2, vb, "minecraft:iron_bars")
        toon = list(p(cx, y0 + 1, vb - 1))
        binnen = list(p(cx, y0 + 1, vb + 1)) if vb + 1 <= d - 2 else binnen
        waren = {"herberg": ["minecraft:barrel[facing=up,open=false]", "minecraft:cake[bites=0]"],
                 "bank": ["minecraft:gold_block", "minecraft:chest[facing=north,type=single,waterlogged=false]"],
                 "winkel": ["minecraft:barrel[facing=up,open=false]", "minecraft:bookshelf", "minecraft:flowering_azalea_leaves"]}[rol]
        for u in range(1, w - 1):
            z_(u, y0 + 1, d - 2, waren[u % len(waren)])
        if rol == "herberg":
            # tables with stools in front of the counter... behind it is the bar; beds upstairs are not reachable, so a bed corner
            bed(W, *p(1, y0 + 1, d - 3), facing=_dir(K, "achter"), kleur="pink") if d >= 7 else None
        if rol == "bank":
            z_(1, y0 + 2, vb, "minecraft:lantern[hanging=false,waterlogged=false]")
    return [int(dx_), int(dy_), int(dz_)], [int(v) for v in binnen], [int(v) for v in toon]


def _dir(K, kant):
    return K.binnen if kant == "achter" else K.uit


# =============================================================================================== big buildings
def raadszaal(W, S, info):
    """The council hall (Wp shift: cella x -113..-89, z -17..17, floor 256): hollow, a door east, lectern, benches, side room."""
    y = 256
    leeg(W, -112, y + 1, -16, -90, y + 19, 16)
    doos(W, -112, y, -16, -90, y, 16, "vloer")
    doos(W, -106, y, -2, -90, y, 2, "minecraft:pink_wool")
    # door: a 5-wide, 6-high gold-framed opening in the east wall (x -89) where the gold block door was
    doos(W, -89, y + 1, -3, -89, y + 12, 3, "goudblok")
    leeg(W, -89, y + 1, -2, -89, y + 6, 2)
    doos(W, -89, y + 7, -2, -89, y + 7, 2, "goud")
    # the lectern of the Hoge Raad on a dais (west end), benches in two blocks facing it
    doos(W, -106, y + 1, -5, -104, y + 1, 5, P["wit_glad"])
    zet(W, -105, y + 2, 0, "minecraft:lectern[facing=east,has_book=true,powered=false]")
    for x in range(-101, -92, 2):
        for s in (-1, 1):
            for zz in range(4, 14):
                zet(W, x, y + 1, s * zz, trap("goud_trap", "east") if zz in (4, 13) else "minecraft:cherry_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
    # the side room (north-west): wall at x -107 with a door, a desk with the torn page (a book on a lectern), shelves
    doos(W, -107, y + 1, -16, -107, y + 6, -6, P["marmer"])
    doos(W, -112, y + 1, -6, -107, y + 6, -6, P["marmer"])
    deur(W, -107, y + 1, -9, "east", soort="pale_oak")
    zet(W, -111, y + 1, -12, "minecraft:lectern[facing=east,has_book=true,powered=false]")
    for zz in range(-15, -6):
        zet(W, -112, y + 1, zz, "minecraft:bookshelf")
        zet(W, -112, y + 2, zz, "minecraft:bookshelf")
    zet(W, -110, y + 1, -15, "minecraft:chest[facing=south,type=single,waterlogged=false]")
    for x in (-108, -100, -92):
        KA_luchter(W, x, y + 19, 0)
    info["plekken"]["raadszaal_lessenaar"] = (-103, y + 1, 0)


def KA_luchter(W, x, y_plafond, z):
    for k in range(3):
        zet(W, x, y_plafond - 1 - k, z, "minecraft:iron_chain[axis=y,waterlogged=false]")
    zet(W, x, y_plafond - 4, z, "goudblok")
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        zet(W, x + dx, y_plafond - 4, z + dz, "minecraft:lantern[hanging=true,waterlogged=false]")


def paleis(W, S, info):
    """Palace (Wp shift: block x -152..-122, plateau 252): a gate from the front into the throne room, the chancellery."""
    y = 252
    # the front: open the gold door (x -121) and the wall between it and the throne room (x -126..-121), 5 wide, 7 high
    leeg(W, -126, y + 1, -2, -121, y + 7, 2)
    doos(W, -126, y, -2, -121, y, 2, "vloer")
    doos(W, -121, y + 8, -3, -121, y + 8, 3, "goud")
    for s in (-3, 3):
        doos(W, -121, y + 1, s, -121, y + 7, s, "goud")
    # a carpet runs from the door to the throne
    doos(W, -128, y, -1, -121, y, 1, "minecraft:pink_wool")
    # throne room: guards' torches -> lanterns on the columns; standing spots
    for x in range(-146, -127, 5):
        for s in (-1, 1):
            zet(W, x, y + 6, s * 8, "minecraft:lantern[hanging=false,waterlogged=false]")
    # the chancellery in the south wing (local +z): x -146..-128, z 25..42, 6 high, a corridor from the throne room (x -131)
    leeg(W, -146, y + 1, 25, -128, y + 6, 42)
    doos(W, -146, y, 25, -128, y, 42, "vloer")
    leeg(W, -133, y + 1, 13, -129, y + 5, 24)
    doos(W, -133, y, 13, -129, y, 24, "vloer")
    doos(W, -133, y + 1, 24, -129, y + 5, 24, P["marmer"])
    deur(W, -131, y + 1, 24, "north", soort="pale_oak")
    # desk + bookshelves + map table
    doos(W, -143, y + 1, 33, -139, y + 1, 35, "minecraft:stripped_cherry_wood[axis=y]")
    zet(W, -141, y + 2, 34, "minecraft:lectern[facing=north,has_book=true,powered=false]")
    for x in range(-146, -128):
        if x % 3:
            zet(W, x, y + 1, 42, "minecraft:bookshelf")
            zet(W, x, y + 2, 42, "minecraft:bookshelf")
    zet(W, -135, y + 1, 40, "minecraft:cartography_table")
    for (x, z) in ((-140, 29), (-134, 37)):
        hanglamp(W, x, y + 7, z)
    info["plekken"]["paleis_kanselarij"] = (-137, y + 1, 30)
    info["plekken"]["paleis_kanselarij_bureau"] = (-141, y + 1, 37)


def universiteit(W, S, info):
    """University (Wu shift: x -76..6, z -80..-30, floor 234, wings 12 deep): ground floors of the wings hollow with rooms,
    doors to the courtyard, a two-storey library under the rotunda, the archive, the guild house (east wing, workshops),
    an observatory with a spiral stair; outside: buttresses, window sills/hoods, oriels."""
    y0 = 234
    xa, xb, za, zb, d = -76, 6, -80, -30, 12
    wingen = [(xa + 1, za + 1, xb - 1, za + d - 1), (xa + 1, zb - d + 1, xb - 1, zb - 1),
              (xa + 1, za + d, xa + d - 1, zb - d), (xb - d + 1, za + d, xb - 1, zb - d)]
    for (x1, z1, x2, z2) in wingen:
        leeg(W, x1, y0 + 1, z1, x2, y0 + 6, z2)
        doos(W, x1, y0, z1, x2, y0, z2, "minecraft:dark_oak_planks")
        for x in range(x1 + 3, x2 - 1, 7):
            for z in range(z1 + 3, z2 - 1, 7):
                hanglamp(W, x, y0 + 7, z)
    gx = -35
    # the gate passage already exists (x -38..-32); doors from the courtyard into each wing (middle of each inner wall)
    ia, ib, ja, jb = xa + d + 1, xb - d - 1, za + d + 1, zb - d - 1
    for (x, z, f) in ((-50, ja - 1, "south"), (-20, ja - 1, "south"), (-50, jb + 1, "north"), (-20, jb + 1, "north"),
                      (ia - 1, -55, "east"), (ib + 1, -55, "east")):
        leeg(W, x, y0 + 1, z, x, y0 + 2, z)
        deur(W, x, y0 + 1, z, f, soort="dark_oak")
    # a cloister walk around the courtyard: keep the floor at y0 (the courtyard is grass inside)
    # library: the north wing x -62..-36 two storeys high (hollow to y0+13), shelves up the long walls, reading desks
    leeg(W, -62, y0 + 1, za + 1, -36, y0 + 13, za + d - 1)
    for x in range(-62, -35):
        for z in (za + 1, za + d - 1):
            for y in range(y0 + 1, y0 + 13):
                zet(W, x, y, z, "minecraft:bookshelf" if (y - y0) % 4 else "minecraft:dark_oak_planks")
    for x in range(-58, -38, 5):
        doos(W, x, y0 + 1, za + 5, x + 2, y0 + 1, za + 7, "minecraft:dark_oak_planks")
        zet(W, x + 1, y0 + 2, za + 6, "minecraft:lectern[facing=south,has_book=true,powered=false]")
        zet(W, x, y0 + 2, za + 5, "minecraft:candle[candles=3,lit=true,waterlogged=false]")
    for x in range(-60, -36, 8):
        KA_luchter(W, x, y0 + 14, za + 6)
    # archive (west wing, south part) + lecture hall (west wing north part + the south wing's west end)
    for z in range(-66, -54, 2):
        doos(W, -74, y0 + 1, z, -67, y0 + 3, z, "minecraft:bookshelf")
    for x in range(-74, -66, 2):
        for z in range(-48, -32):
            if z % 3:
                zet(W, x, y0 + 1, z, "minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
    zet(W, -75, y0 + 2, -42, "minecraft:lectern[facing=east,has_book=true,powered=false]")
    # the Gildehuis (guild house) in the east wing (x -5..5, z -67..-43) + its hall in the south wing's east end (z -41..-31)
    werk = [("gilde_keuken", -46, ["minecraft:smoker[facing=west,lit=false]", "minecraft:barrel[facing=up,open=false]"]),
            ("gilde_brouwerij", -51, ["minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]", "minecraft:cauldron"]),
            ("gilde_smidse", -56, ["minecraft:anvil[facing=north]", "minecraft:blast_furnace[facing=west,lit=false]"]),
            ("gilde_atelier", -61, ["minecraft:loom[facing=west]", "minecraft:crafting_table"]),
            ("gilde_bijenstal", -66, ["minecraft:beehive[facing=west,honey_level=5]", "minecraft:composter[level=0]"])]
    for (wid, zc, spul) in werk:
        zw = zc + 3      # a dividing wall on the south side of each workshop, with a 2-wide opening in the middle
        doos(W, -5, y0 + 1, zw, 5, y0 + 6, zw, "guhs:pastelbaksteen_roze")
        leeg(W, -1, y0 + 1, zw, 0, y0 + 3, zw)
        for k, b in enumerate(spul):
            zet(W, 4, y0 + 1, zc - 1 + k * 2, b)
        info["plekken"][wid] = (0, y0 + 1, zc)
    leeg(W, -5, y0 + 1, -41, 5, y0 + 6, -31)          # the guild hall (the south wing's east end)
    doos(W, -6, y0 + 1, -41, -6, y0 + 6, -31, "guhs:pastelbaksteen_roze")   # its wall to the lecture hall
    doos(W, 6, y0 + 1, -38, 6, y0 + 3, -36, "guhs:pastelbaksteen_roze")
    deur(W, 6, y0 + 1, -37, "east", soort="dark_oak")
    trap_af(W, S, 6, -37, y0, "east", breed=3)
    doos(W, -3, y0 + 1, -33, 3, y0 + 1, -33, "minecraft:stripped_cherry_wood[axis=y]")
    zet(W, 0, y0 + 2, -33, "minecraft:lectern[facing=north,has_book=true,powered=false]")
    BH.vaandel(W, 5, y0 + 3, -34, "west")
    info["plekken"]["gilde_hal"] = (0, y0 + 1, -36)
    info["plekken"]["gilde_ingang"] = (8, y0 + 1, -37)
    # the gate passage (x -38..-32) ends at z -28 above a trench in front of the south wing: fill it, stairs up to the street
    poorttrap(W, -76, 6, -27, -24, gx, "south", y0, 238)
    # the observatory tower (x -3..7, z -81..-71, to y0+46): a spiral stair from the north wing (door on its west side)
    tx, tz = 2, -76
    doos(W, tx - 5, y0 + 1, tz - 5, tx + 5, y0 + 7, tz + 5, P["marmer"])      # its walls (the wing carve went through them)
    wenteltrap(W, tx, tz, y0 + 1, y0 + 45, r_wand=4, wand="marmer", kern="marmer_glad", trede="minecraft:dark_oak_stairs",
               plaat_="minecraft:dark_oak_slab", ingang="west", vloer="minecraft:dark_oak_planks")
    zet(W, *open_[-1][:1], y0 + 45, open_[-1][1], "minecraft:dark_oak_stairs[facing=" + "north" + ",half=bottom,shape=straight,waterlogged=false]") if False else None
    leeg(W, tx - 4, y0 + 46, tz - 4, tx + 4, y0 + 49, tz + 4)
    zet(W, tx + 3, y0 + 46, tz + 3, "minecraft:copper_block")
    zet(W, tx + 3, y0 + 47, tz + 3, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    leeg(W, tx - 5, y0 + 1, tz, tx - 3, y0 + 2, tz)
    deur(W, tx - 5, y0 + 1, tz, "west", soort="dark_oak")
    info["plekken"]["universiteit_sterrenwacht"] = (tx - 6, y0 + 1, tz)
    info["plekken"]["universiteit_sterrenwacht_top"] = (tx - 2, y0 + 46, tz + 2)
    info["uitkijken"].append((tx - 2, y0 + 46, tz + 2))
    # ---- wall depth outside (decision 4): buttresses between the windows, sills under and hoods over every window,
    #      and oriels on the long outer facades
    for x in range(xa + 4, xb - 1, 4):
        for (zz, f, o) in ((za - 1, "north", -1), (zb + 1, "south", 1)):
            if abs(x - gx) <= 8:
                continue
            doos(W, x, y0 + 1, zz, x, y0 + 19, zz, P["wit_glad"])
            zet(W, x, y0 + 20, zz, trap("wit_trap", TEGEN[f]))
            zet(W, x, y0 + 1, zz + o, trap("steen_trap", TEGEN[f]))
    for z in range(za + 4, zb - 1, 4):
        for (xx, f, o) in ((xa - 1, "west", -1), (xb + 1, "east", 1)):
            doos(W, xx, y0 + 1, z, xx, y0 + 19, z, P["wit_glad"])
            zet(W, xx, y0 + 20, z, trap("wit_trap", TEGEN[f]))
    for x in range(xa + 2, xb - 1, 4):
        for v in range(3):
            yb = y0 + 2 + v * 7
            for (zz, f) in ((za - 1, "north"), (zb + 1, "south")):
                zet(W, x, yb - 1, zz, plaat("wit_plaat", "top"))
                zet(W, x + 1, yb - 1, zz, plaat("wit_plaat", "top"))
                zet(W, x, yb + 4, zz, trap("goud_trap", TEGEN[f], "top"))
                zet(W, x + 1, yb + 4, zz, trap("goud_trap", TEGEN[f], "top"))
                if v == 1 and (x // 4) % 2 == 0:
                    zet(W, x, yb - 1, zz + (-1 if f == "north" else 1), "minecraft:flowering_azalea_leaves")
    for (ox, oz, f) in ((-62, za - 1, "north"), (-14, za - 1, "north"), (-62, zb + 1, "south"), (-10, zb + 1, "south")):
        erker(W, ox, oz, y0 + 8, f)


def erker(W, x, z, y, uit, breed=5, h=10):
    """A rounded oriel (bay window) on consoles sticking 2 out of a facade at (x, z), from y up h."""
    dx, dz = RICHTING[uit]
    zx, zz = abs(dz), abs(dx)
    for s in range(-(breed // 2), breed // 2 + 1):
        for k in (1, 2):
            if k == 2 and abs(s) == breed // 2:
                continue
            px, pz = x + zx * s + dx * (k - 1), z + zz * s + dz * (k - 1)
            zet(W, px, y - 1, pz, trap("wit_trap", TEGEN[uit], "top"))
            zet(W, px, y, pz, P["wit_glad"])
            for yy in range(y + 1, y + h - 1):
                rand_ = abs(s) == breed // 2 or (k == 2 and abs(s) == breed // 2 - 1)
                zet(W, px, yy, pz, P["marmer"] if rand_ or (yy - y) % 4 == 0 else P["raam"])
            zet(W, px, y + h - 1, pz, P["goud"])
            zet(W, px, y + h, pz, trap("dak_goud_trap", TEGEN[uit]) if k == 2 else P["dak_goud"])
    zet(W, x + dx * 1, y + h + 1, z + dz * 1, P["goudblok"] if "goudblok" in P else "minecraft:gold_block")


RING = [(0, -2), (1, -2), (2, -2), (2, -1), (2, 0), (2, 1), (2, 2), (1, 2), (0, 2), (-1, 2), (-2, 2), (-2, 1), (-2, 0),
        (-2, -1), (-2, -2), (-1, -2)]          # the stair lane around a 3x3 core, clockwise seen from above
START = {"north": 0, "east": 4, "south": 8, "west": 12}


def _richting(a, b):
    dx, dz = b[0] - a[0], b[1] - a[1]
    return {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(dx, dz)]


def _trede(trede, f):
    return trap(trede, f) if trede in P else f"{trede}[facing={f},half=bottom,shape=straight,waterlogged=false]"


def _plaat(p_):
    return plaat(p_, "top") if p_ in P else f"{p_}[type=top,waterlogged=false]"


def wenteltrap(W, cx, cz, y1, y_vloer, r_wand=4, wand="marmer", kern="marmer_glad", trede="wit_trap", plaat_="wit_plaat",
               ingang="west", vloer=None, vloer_r=None):
    """A spiral stair in a square shaft: a 3x3 core, a 1-wide stair lane around it (16 steps per turn, 15 blocks headroom),
    walls from ring 3 to r_wand. The lane starts on the floor at y1 on the 'ingang' side (open a door there yourself) and its last
    step is at y_vloer (its top = the top of a floor block at y_vloer). Returns the lane cells of the last steps (keep them open
    in that floor)."""
    doos(W, cx - r_wand, y1, cz - r_wand, cx + r_wand, y_vloer + 3, cz + r_wand, wand)
    leeg(W, cx - 2, y1, cz - 2, cx + 2, y_vloer + 3, cz + 2)
    doos(W, cx - 1, y1, cz - 1, cx + 1, y_vloer + 3, cz + 1, kern)
    ring = RING[START[ingang]:] + RING[:START[ingang]]
    y, i = y1, 1
    laatst = []
    while y <= y_vloer:
        a, b, c = ring[(i - 1) % 16], ring[i % 16], ring[(i + 1) % 16]
        # the step faces where you go NEXT (at a corner: the new side), so its high side always leads on to the next step
        f = _richting(b, c) if y < y_vloer else _richting(a, b)
        px, pz = cx + b[0], cz + b[1]
        zet(W, px, y, pz, _trede(trede, f))
        if y > y1:
            zet(W, px, y - 1, pz, _plaat(plaat_))
        laatst.append((px, pz, y))
        y += 1
        i += 1
    if vloer:
        # the top floor (its blocks at y_vloer, like the last step): open above the three steps before the last, and the last
        # step stays a stair (its top = the floor's top: no 1.5 step)
        r_ = vloer_r or r_wand
        lx, lz, ly = laatst[-1]
        st_ = W.wget(lx, ly, lz)
        doos(W, cx - r_, y_vloer, cz - r_, cx + r_, y_vloer, cz + r_, vloer)
        leeg(W, cx - r_, y_vloer + 1, cz - r_, cx + r_, y_vloer + 4, cz + r_)
        for (px, pz, yy) in laatst[-4:-1]:
            zet(W, px, y_vloer, pz, LUCHT)
        zet(W, lx, ly, lz, st_)
    return [(px, pz) for (px, pz, yy) in laatst[-4:]]


def academie(W, S, info):
    """Academy of the Cloud Guard (Wk shift: walls x -76..6, z 30..80, floor 234; keep x -49..-21, z 66..78 to y 264):
    the keep's ground floor = the hall (captain), the barracks (beds) and the lockers; a door from the drill yard; a tower
    with a spiral stair to a lookout; the Proefarena (t-z5wezens) in the yard."""
    y0 = 234
    gx = -35
    leeg(W, -48, y0 + 1, 67, -22, y0 + 9, 77)
    doos(W, -48, y0, 67, -22, y0, 77, "minecraft:spruce_planks")
    # door in the north face (z 66) towards the yard
    leeg(W, gx - 1, y0 + 1, 66, gx + 1, y0 + 4, 66)
    for s in (-2, 2):
        doos(W, gx + s, y0 + 1, 65, gx + s, y0 + 5, 65, P["wit_glad"])
    doos(W, gx - 2, y0 + 5, 65, gx + 2, y0 + 5, 65, "goud")
    # hall: banners, the captain's table
    doos(W, gx - 2, y0 + 1, 72, gx + 2, y0 + 1, 73, "minecraft:stripped_spruce_wood[axis=y]")
    zet(W, gx, y0 + 2, 73, "minecraft:lectern[facing=north,has_book=true,powered=false]")
    for x in (gx - 6, gx + 6):
        BH.vaandel(W, x, y0 + 4, 77, "north")
    # barracks (west part): bunk beds; lockers (east part): barrels and chests
    for x in range(-47, -40, 2):
        bed(W, x, y0 + 1, 76, "south", kleur="pink")
        bed(W, x, y0 + 4, 76, "south", kleur="white") if False else None
    for x in range(-29, -22):
        zet(W, x, y0 + 1, 77, "minecraft:barrel[facing=north,open=false]" if x % 2 else "minecraft:chest[facing=north,type=single,waterlogged=false]")
        zet(W, x, y0 + 2, 77, "minecraft:barrel[facing=north,open=false]")
    for x in range(-44, -24, 6):
        hanglamp(W, x, y0 + 10, 72)
    # the east tower (round, r 3, x -21, z 66, y0+1..y0+36): hollow, a ladder up to a floor at y0+33, a door onto a balcony
    tx, tz = -21, 66
    leeg(W, tx - 1, y0 + 1, tz - 1, tx + 1, y0 + 35, tz + 1)
    for y in range(y0 + 1, y0 + 34):
        zet(W, tx, y, tz + 1, "minecraft:ladder[facing=north,waterlogged=false]")
    doos(W, tx - 1, y0 + 33, tz - 1, tx + 1, y0 + 33, tz, "minecraft:spruce_planks")
    zet(W, tx - 1, y0 + 33, tz + 1, "minecraft:spruce_planks")
    zet(W, tx + 1, y0 + 33, tz + 1, "minecraft:spruce_planks")
    leeg(W, tx, y0 + 1, tz - 3, tx, y0 + 2, tz - 2)
    deur(W, tx, y0 + 1, tz - 2, "north", soort="spruce")
    leeg(W, tx, y0 + 34, tz - 3, tx, y0 + 35, tz - 2)
    doos(W, tx - 2, y0 + 33, tz - 6, tx + 2, y0 + 33, tz - 4, P["wit_glad"])
    for x in range(tx - 2, tx + 3):
        zet(W, x, y0 + 34, tz - 6, P["wit_muur"])
    for z in (tz - 5, tz - 4):
        zet(W, tx - 2, y0 + 34, z, P["wit_muur"])
        zet(W, tx + 2, y0 + 34, z, P["wit_muur"])
    info["plekken"]["academie_toren"] = (tx, y0 + 34, tz - 5)
    info["plekken"]["academie_toren_voet"] = (tx, y0 + 1, tz - 4)
    info["uitkijken"].append((tx, y0 + 34, tz - 5))
    # the yard lies 4 below the street outside the gate: a trench ran along the front (z 24..27) -> filled to the street, and a
    # 5-wide stair from the gate passage up to the street
    poorttrap(W, -76, 6, 24, 27, gx, "north", y0, 238)
    # Proefarena (t-z5wezens): a round sand floor r 11 with a low fence ring with 4 gaps, centre (-12, 50)
    cx, cz, r = -12, 50, 11
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            dd = math.hypot(x - cx, z - cz)
            if dd <= r + 0.5:
                zet(W, x, y0, z, "guhs:geharkt_zand" if palette_heeft("guhs:geharkt_zand") else "minecraft:sand")
                leeg(W, x, y0 + 1, z, x, y0 + 4, z)
            if r + 0.5 < dd <= r + 1.5 and not (abs(x - cx) <= 1 or abs(z - cz) <= 1):
                zet(W, x, y0 + 1, z, "minecraft:spruce_fence")
    info["plekken"]["z5w_proefarena"] = (cx, y0 + 1, cz)


def poorttrap(W, x1, x2, za, zb, gx, uit, y_laag, y_hoog, breed=5):
    """In front of a gate of a sunken building (university, academy): the trench z za..zb along the front (x1..x2) is filled to
    the street (y_hoog = the street's floor block), and a stair (breed wide around gx) climbs from the gate floor (y_laag) to it,
    going 'uit' (north = -z, south = +z)."""
    for x in range(x1, x2 + 1):
        if abs(x - gx) <= breed // 2:
            continue
        for z in range(za, zb + 1):
            if is_lucht(W, x, y_laag + 1, z) or naam(W, x, y_laag + 1, z).endswith("grass"):
                doos(W, x, y_laag + 1, z, x, y_hoog - 1, z, "minecraft:dirt")
                zet(W, x, y_hoog, z, "guhs:wolkenweide_gras")
    dz = -1 if uit == "north" else 1
    z0 = zb if uit == "north" else za            # the first cell outside the passage
    for j in range(y_hoog - y_laag):
        z = z0 + dz * j
        for x in range(gx - breed // 2, gx + breed // 2 + 1):
            doos(W, x, y_laag, z, x, y_laag + j, z, "steen_bak")
            zet(W, x, y_laag + 1 + j, z, trap("steen_trap", uit))
            leeg(W, x, y_laag + 2 + j, z, x, y_laag + 5 + j, z)


def palette_heeft(s):
    return True


def station(W, S, info):
    """The airport station (Wh shift: hall x 40..94, z -78..-58, floor 236): benches, the harbour master's counter, the log
    book, crates with the loose plank (t-z5wezens' Schuilnest hatch), departure boards; pier 2 free for the line-D airship."""
    y0 = 236
    # benches (rows facing the piers)
    for x in range(44, 62, 3):
        for z in (-70, -66):
            zet(W, x, y0 + 1, z, "minecraft:cherry_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
            zet(W, x + 1, y0 + 1, z, "minecraft:cherry_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    # harbour master's counter (x 76..84, z -73) with the keeper behind (z -74..-76)
    doos(W, 76, y0 + 1, -73, 84, y0 + 1, -73, "minecraft:stripped_cherry_wood[axis=y]")
    zet(W, 84, y0 + 2, -73, "minecraft:lectern[facing=south,has_book=true,powered=false]")
    zet(W, 77, y0 + 2, -73, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    for x in range(76, 85):
        zet(W, x, y0 + 1, -77, "minecraft:barrel[facing=north,open=false]")
    # crates in the north-east corner + the loose plank (a trapdoor on the floor behind them)
    for (x, z) in ((92, -76), (93, -76), (93, -75), (92, -74), (93, -74)):
        zet(W, x, y0 + 1, z, "minecraft:barrel[facing=up,open=false]")
    zet(W, 93, y0 + 2, -76, "minecraft:barrel[facing=up,open=false]")
    # departure boards: a dark board with lanterns at each pier head (the text is t-luchtschip's)
    for (px, bid) in ((52, "haven_5_1_vertrekbord"), (72, "haven_5_4_vertrekbord"), (92, "haven_5_6_vertrekbord")):
        bx, bz = px + 4, -80
        zet(W, bx - 1, y0 + 1, bz, "minecraft:dark_oak_fence")
        zet(W, bx + 1, y0 + 1, bz, "minecraft:dark_oak_fence")
        doos(W, bx - 1, y0 + 2, bz, bx + 1, y0 + 3, bz, "minecraft:dark_oak_planks")
        zet(W, bx, y0 + 4, bz, "minecraft:lantern[hanging=false,waterlogged=false]")
        info["plekken"][bid] = (bx, y0 + 1, bz + 1)
    # hanging lamps in the hall
    for x in range(46, 92, 8):
        hanglamp(W, x, y0 + 11, -68, keten=2)


def pieren(W, S, info):
    """Pier 2 (x 72, the line-D airship): no anchored ship and no mooring tower at its end, but a short mooring mast at each
    side and a gate; railings stay on all piers (the kit has fences on both sides)."""
    y0 = 236
    # the city wall crosses every pier (its top stands on the deck): a gate through it, 5 wide and 5 high, a gold lintel
    for px in (52, 72, 92):
        for z in range(-80, -130, -1):
            muur = any(not is_lucht(W, x, y, z) for x in range(px - 2, px + 3) for y in (y0 + 2, y0 + 3))
            if not muur:
                continue
            leeg(W, px - 2, y0 + 1, z, px + 2, y0 + 5, z)
            doos(W, px - 2, y0, z, px + 2, y0, z, "minecraft:stripped_cherry_wood[axis=y]")
            doos(W, px - 2, y0 + 6, z, px + 2, y0 + 6, z, "goud")
    px = 72
    # remove the mooring tower of pier 2 (x 70..74, z -166..-162, y0+1..y0+28) and put the deck back
    leeg(W, px - 3, y0 + 1, -168, px + 3, y0 + 29, -160)
    for z in range(-168, -159):
        doos(W, px - 2, y0, z, px + 2, y0, z, "minecraft:stripped_cherry_wood[axis=y]" if z % 4 else P["goud"])
        zet(W, px - 3, y0 + 1, z, "minecraft:pale_oak_fence")
        zet(W, px + 3, y0 + 1, z, "minecraft:pale_oak_fence")
    # mooring masts at the end, a gate across the end
    for s in (-3, 3):
        doos(W, px + s, y0 + 1, -168, px + s, y0 + 10, -168, P["wit_glad"])
        zet(W, px + s, y0 + 11, -168, P["goud"])
        zet(W, px + s, y0 + 12, -168, "minecraft:lantern[hanging=false,waterlogged=false]")
    for x in range(px - 2, px + 3):
        zet(W, x, y0 + 1, -168, "minecraft:pale_oak_fence_gate[facing=north,in_wall=false,open=false,powered=false]")
    zet(W, px - 1, y0 + 1, -161, "minecraft:bell[attachment=floor,facing=east,powered=false]")
    info["plekken"]["haven_5_4_bel"] = (px - 1, y0 + 1, -161)


def soepkeuken(W, S, info):
    y0 = 238
    # the open front had a lit campfire under the kettle: keep it (it is inside the copper ring), make the tables walkable


def kathedraal(W, S, info):
    """Open the Grote Poort (the gold door leaves become an open double door 9 wide x 15 high) and check the nave floor."""
    import kathedraal as KA
    Y0, xa = KA.Y0, KA.X0 + 4
    leeg(W, xa, Y0 + 1, -4, xa, Y0 + 15, 4)
    doos(W, xa, Y0 + 16, -5, xa, Y0 + 16, 5, "goud")
    for s in (-5, 5):
        doos(W, xa, Y0 + 1, s, xa, Y0 + 15, s, "goudblok")
    # open door leaves standing against the inside of the wall
    for z in range(-4, 5):
        for y in range(Y0 + 1, Y0 + 16):
            pass
    doos(W, xa, Y0, -4, xa, Y0, 4, "vloer")
    # the plaza stair up to the cathedral terrace (x 16..19) was buried in the terrace ground: clear above each step
    for k in range(3):        # (the 4th step at x 19 lies at the terrace height already)
        x = 16 + k
        for (z1, z2) in ((-12, 12), (-30, -28), (28, 30)):
            leeg(W, x, 242 + k, z1, x, Y0, z2)
    # the south front tower (x X0-1..X0+13, z 13..27): a spiral stair from the aisle (door on its east side) to the belfry floor
    r = 7
    x0, zc = KA.X0 - 1, 20
    cx = x0 + r
    yv = Y0 + KA.GALM_Y - 1
    wenteltrap(W, cx, zc, Y0 + 1, yv, r_wand=6, wand="marmer", kern="marmer_glad", trede="wit_trap", plaat_="wit_plaat",
               ingang="east", vloer=P["wit_glad"])
    leeg(W, cx - 6, yv + 5, zc - 6, cx + 6, yv + 6, zc + 6)
    # the belfry openings reach down to the floor: a balustrade in them (you look out, you do not fall out)
    for x in range(x0, x0 + 2 * r + 1):
        for z in range(zc - r, zc + r + 1):
            rand_ = x in (x0, x0 + 2 * r) or z in (zc - r, zc + r)
            if rand_ and is_lucht(W, x, yv + 1, z):
                zet(W, x, yv + 1, z, P["wit_muur"])
    # door from the aisle: through the tower's east wall at z 20 into the lane start (cx+2, 20)
    leeg(W, cx + 3, Y0 + 1, zc, x0 + 2 * r + 2, Y0 + 3, zc)
    info["plekken"]["kathedraal_geveltoren"] = (cx - 4, yv + 1, zc + 4)
    info["plekken"]["kathedraal_geveltoren_voet"] = (x0 + 2 * r + 2, Y0 + 1, zc)
    info["uitkijken"].append((cx - 4, yv + 1, zc + 4))


def wolkwaldroom(W, S, info):
    """A round chamber inside the whale under the cathedral for t-z5verhaal's finale (the spy fight, r 16): a soft floor of
    belly skin and cloud, a dome with cloud patches, lit by sea lanterns. Reached only by the story's teleport."""
    cx, cy, cz, r = 100, 186, 0, 17
    W.bol((cx, cy, cz), r + 2, "guhs:walvisbuik", ry=13, ymin=cy - 2)
    W.bol((cx, cy + 1, cz), r, None, ry=11, ymin=cy)
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            dd = math.hypot(x - cx, z - cz)
            if dd <= r:
                zet(W, x, cy, z, "guhs:wolkenblok_wit" if (x * 7 + z * 3) % 11 == 0 else "guhs:walvisbuik")
                if r - 1.5 < dd and (x + z) % 5 == 0:
                    zet(W, x, cy + 1, z, "minecraft:sea_lantern")
    info["plekken"]["wolkwaldroom_arena"] = (cx, cy + 1, cz)
    info["plekken"]["z5w_wolkwaldroom_deur"] = (cx + r - 2, cy + 1, cz)


def wolkplatformen(W, S, info):
    """t-z5wezens' combat spots on the fins and the tail fluke: a flat round floor at the skin height of the centre, whale skin
    where the skin is, soft cloud where it would hang in the air, a rim of cloud bumps (no holes, no sudden drops)."""
    import plan5 as PL
    X, Y, Z = W.v.shape
    for pid, (cx, cz, r) in PL.PLATFORMS.items():
        i, k = W.vx(cx), W.vz(cz)
        col = W.v[i, :, k]
        nz = np.nonzero(col)[0]
        if not len(nz):
            continue
        ytop = int(W.b[1] + nz.max())
        for x in range(cx - r - 2, cx + r + 3):
            for z in range(cz - r - 2, cz + r + 3):
                dd = math.hypot(x - cx, z - cz)
                if dd > r + 1.5:
                    continue
                ii, kk = W.vx(x), W.vz(z)
                if not (0 <= ii < X and 0 <= kk < Z):
                    continue
                c = W.v[ii, :, kk]
                nzc = np.nonzero(c)[0]
                huid = len(nzc) and abs(int(W.b[1] + nzc.max()) - ytop) < 8
                if dd <= r:
                    leeg(W, x, ytop + 1, z, x, ytop + 6, z)
                    zet(W, x, ytop, z, P["huid"] if huid else "guhs:wolkenblok_wit")
                    zet(W, x, ytop - 1, z, P["huid"] if huid else "guhs:wolkenblok_wit")
                else:
                    zet(W, x, ytop, z, "guhs:wolkenblok_wit")
                    zet(W, x, ytop + 1, z, "guhs:wolkenblok_wit")
        info["plekken"][pid] = (cx, ytop + 1, cz)


# =============================================================================================== t-z5wezens: rooms + real access
# their volumes inside the whale (LOCAL, = their render-y placeholders; the game lowers everything by DY like the rest)
BUIKGANGEN = (-106, 184, -12, -2, 196, 10)
SCHUILNEST = (78, 206, -106, 114, 214, -80)


def ladderschacht(W, x, z, y_onder, y_boven, steun="north"):
    """A 1x1 shaft from y_onder up to y_boven with a ladder on its 'steun' side (the block there is made solid)."""
    dx, dz = RICHTING[steun]
    f = TEGEN[steun]
    leeg(W, x, y_onder, z, x, y_boven, z)
    for y in range(y_onder, y_boven + 1):
        if is_lucht(W, x + dx, y, z + dz):
            zet(W, x + dx, y, z + dz, P["steen_bak"])
        zet(W, x, y, z, f"minecraft:ladder[facing={f},waterlogged=false]")


def tunnel(W, x1, z1, x2, z2, y_vloer, h=3, vloer="steen_bak"):
    leeg(W, min(x1, x2), y_vloer + 1, min(z1, z2), max(x1, x2), y_vloer + h, max(z1, z2))
    doos(W, min(x1, x2), y_vloer, min(z1, z2), max(x1, x2), y_vloer, max(z1, z2), vloer)


def wezens_toegang(W, S, info):
    """The rooms of t-z5wezens stay free (carved to air: they build their stand-ins in there), with REAL access next to the
    teleport hatches: a ladder shaft from the soup kitchen hatch to the Buikgangen and from the loose plank in the station to
    the Schuilnest; ladder shafts from the city rim through the whale's flank onto both fins; a railed path over the tail
    stalk to the fluke (both lobes)."""
    for (x1, y1, z1, x2, y2, z2) in (BUIKGANGEN, SCHUILNEST):
        leeg(W, x1, y1, z1, x2, y2, z2)
        doos(W, x1, y1 - 1, z1, x2, y1 - 1, z2, P["steen_bak"])
    # soup kitchen: the hatch (in the floor at y 238) -> a ladder down to y 185, a tunnel north into the Buikgangen (z 10)
    hx, hz = -31, 21
    ladderschacht(W, hx, hz, 185, 238, steun="north")
    zet(W, hx, 239, hz + 1, "minecraft:spruce_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]")
    tunnel(W, hx - 1, 10, hx + 1, hz - 1, 184)
    info["plekken"]["z5w_buik_luik"] = (hx, 239, hz + 1) if False else (hx + 1, 239, hz)
    info["plekken"]["z5w_buik_ladder_onder"] = (hx, 185, hz - 2)
    # station: the loose plank (floor y 236) -> a ladder down to y 207, a tunnel south into the Schuilnest (z -80)
    sx, sz = 91, -76
    ladderschacht(W, sx, sz, 207, 236, steun="east")
    zet(W, sx - 1, 237, sz, "minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]")
    tunnel(W, sx - 1, -81, sx + 1, sz - 1, 206)
    info["plekken"]["z5w_schuilnest_luik"] = (sx, 237, sz)
    info["plekken"]["z5w_schuilnest_ladder_onder"] = (sx, 207, -79)
    # fins: from inside the city wall a ladder down through the flank to the fin's top, then a tunnel out onto it
    import plan5 as PL
    for (k, kant) in (("zuidvin", 1), ("noordvin", -1)):
        fx, fz, _ = PL.PLATFORMS[f"z5w_{k}"]
        top = info["plekken"].get(f"z5w_{k}")
        if not top:
            continue
        yf = top[1] - 1                       # the platform's floor block
        zs = kant * 88                        # shaft inside the wall
        g = S.g(fx, zs)
        if g is None:
            for zz in range(80, 96):
                g = S.g(fx, kant * zz)
                if g is not None:
                    zs = kant * zz
        if g is None:
            continue
        ladderschacht(W, fx, zs, yf + 1, g, steun="east")
        zet(W, fx, g + 1, zs, "minecraft:spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]")
        z_a, z_b = sorted((zs, fz))
        tunnel(W, fx - 1, z_a, fx + 1, z_b, yf)
        info["plekken"][f"z5w_lift_{k}_boven"] = (fx + 1, g + 1, zs)
        info["plekken"][f"z5w_lift_{k}_onder"] = (fx, yf + 1, zs + kant * 3)
    # the tail: a railed path from the west end of the Staartpark over the tail stalk to the fluke and on to both lobes
    pad(W, [(-196, 0), (-262, 0)])
    for zl in (-62, 62):
        pad(W, [(-262, 0), (-266, zl)])
    info["plekken"]["z5w_lift_staart_boven"] = (-194, None, 2)
    info["plekken"]["z5w_lift_staart_onder"] = (-250, None, 0)


def _top(W, x, z, ymax=330):
    i, k = W.vx(x), W.vz(z)
    col = W.v[i, :W.vy(ymax), k]
    nz = np.nonzero(col)[0]
    return int(W.b[1] + nz.max()) if len(nz) else None


def pad(W, punten, breed=1, blok="steen", hek="minecraft:pale_oak_fence"):
    """A path (3 wide) along the whale's top surface between the points, steps of at most 1, railings both sides, 3 free above."""
    pts = []
    for (a, b) in zip(punten, punten[1:]):
        n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1])))
        for i in range(n + 1):
            pts.append((round(a[0] + (b[0] - a[0]) * i / max(n, 1)), round(a[1] + (b[1] - a[1]) * i / max(n, 1))))
    vorig = None
    for j, (x, z) in enumerate(pts):
        t = _top(W, x, z)
        if t is None:
            t = vorig if vorig is not None else 220
        h = t if vorig is None else max(vorig - 1, min(vorig + 1, t))
        langs_x = j + 1 < len(pts) and pts[j + 1][1] == z or j > 0 and pts[j - 1][1] == z
        for s_ in range(-breed - 1, breed + 2):
            px, pz = (x, z + s_) if langs_x else (x + s_, z)
            if abs(s_) <= breed:
                doos(W, px, min(h, t) - 1, pz, px, h, pz, blok)
                leeg(W, px, h + 1, pz, px, h + 3, pz)
            else:
                if is_lucht(W, px, h + 1, pz) or naam(W, px, h + 1, pz) == LUCHT:
                    zet(W, px, h, pz, blok)
                    zet(W, px, h + 1, pz, hek)
        vorig = h


# =============================================================================================== edges, falls, waterfalls
def watervallen(W, S):
    """The waterfall channels through the city wall: a grille of waterlogged chains in the wall so you cannot swim out."""
    n = 0
    for (x, kant) in ((-175, 1), (-126, -1), (60, 1), (-60, -1)):
        g, z0 = None, None
        for z in range(0, kant * 120, kant):
            gg = S.g(x, z)
            if gg is not None:
                g, z0 = gg, z
        if g is None:
            continue
        for dx in range(-3, 4):
            for zz in (z0, z0 - kant):
                for y in (g + 1, g + 2):
                    if naam(W, x + dx, y, zz) in ("minecraft:water", LUCHT):
                        zet(W, x + dx, y, zz, "minecraft:iron_chain[axis=y,waterlogged=true]" if y == g + 1 else
                            "minecraft:iron_chain[axis=y,waterlogged=false]")
                        n += 1
    return n


def kampvuren(W, S, info):
    """An unlit campfire (kern checkpoint) next to every kampvuur_* stand spot."""
    import plan5
    P_ = dict(plan5.plekken())
    for k, (x, y, z) in P_.items():
        if not k.startswith("kampvuur_"):
            continue
        if y is None:
            g = S.g(x, z)
            y = (g + 1) if g is not None else None
        if y is None:
            continue
        zet(W, x + 2, y, z, "minecraft:campfire[facing=west,lit=false,signal_fire=false,waterlogged=false]")
        leeg(W, x, y, z, x + 1, y + 2, z)
        info["plekken"][k] = (x, y, z)


def terrastrappen(W, S):
    """The city lies on terraces 4 blocks apart (stad.grondhoogte) with retaining walls: wherever a STREET crosses a terrace edge,
    a stair is cut along the street (down into the lower street, or, when that is too short, up into the higher one), the
    balustrade on the edge is opened, 3 blocks of headroom. Returns (stairs placed, edges that did not fit)."""
    G = np.where(np.isnan(S.G), -9999, S.G).astype(int)
    straat = S.straat & S.m & ~S.bezet
    # open ground too (gardens, squares between the houses): not built on, not the wall; there a stair every few blocks
    XX, ZZ = np.meshgrid(S.xs, S.zs, indexing="ij")
    park = ((XX >= -199) & (XX <= -153) & (np.abs(ZZ) <= 60)) | ((XX >= 126) & (XX <= 176) & (ZZ >= 26) & (ZZ <= 100))
    open_ = S.m & (~S.bezet | park) & ~S.rand
    vrij_v = W.v[:, :, :]
    st = straat | open_
    X, Z = G.shape
    XX, ZZ = np.meshgrid(S.xs, S.zs, indexing="ij")
    dun = ((XX.astype(int) * 7 + ZZ.astype(int) * 13) % 11 == 0)
    n, mis = 0, 0
    gedaan = set()
    for (dx, dz, uit) in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
        nb = np.roll(np.roll(G, -dx, 0), -dz, 1)
        nst = np.roll(np.roll(st, -dx, 0), -dz, 1)
        nstraat = np.roll(np.roll(straat, -dx, 0), -dz, 1)
        rand = st & nst & (G - nb >= 2) & (nb > -9000) & ((straat & nstraat) | dun)
        for (i, k) in np.argwhere(rand):
            x, z = int(S.xs[i]), int(S.zs[k])
            gh, gl = int(G[i, k]), int(nb[i, k])
            d = gh - gl
            if d > 10:
                continue
            # down along the street: d cells of lower street ahead?
            ok_neer = all(0 <= i + dx * j < X and 0 <= k + dz * j < Z and st[i + dx * j, k + dz * j] and
                          G[i + dx * j, k + dz * j] <= gl for j in range(1, d + 1))
            ok_op = all(0 <= i - dx * j < X and 0 <= k - dz * j < Z and st[i - dx * j, k - dz * j] and
                        G[i - dx * j, k - dz * j] >= gh for j in range(0, d))
            if ok_neer:
                leeg(W, x, gh + 1, z, x, gh + 2, z)            # the balustrade on the edge
                trap_af(W, S, x, z, gh, uit, breed=1, max_k=d + 1, blok="steen_trap")
                n += 1
            elif ok_op:
                # from the low cell, going back into the high terrace: stairs up (high side away from the low street)
                lx, lz = x + dx, z + dz
                tegen = TEGEN[uit]
                for j in range(d):
                    cx, cz = x - dx * j, z - dz * j
                    h = gl + 1 + j
                    zet(W, cx, h, cz, trap("steen_trap", tegen))
                    leeg(W, cx, h + 1, cz, cx, gh + 3, cz)
                leeg(W, lx, gl + 1, lz, lx, gl + 3, lz)
                n += 1
            else:
                mis += 1
    return n, mis


def trap_hekken(W):
    """Walkability rule "fences never block stairs": a fence/wall/bar right above the upper side of a stair is removed."""
    v = W.v
    fam = np.array([any(t in s.split("[")[0] for t in ("_fence", "_wall", "_muur", "iron_bars")) and "gate" not in s for s in W.pal])
    trap_f = {}
    for i, s in enumerate(W.pal):
        k = s.split("[")[0]
        if (k.endswith("_stairs") or k.endswith("_trap")) and "facing=" in s and "half=bottom" in s:
            trap_f[i] = s.split("facing=")[1].split(",")[0].split("]")[0]
    n = 0
    X, Y, Z = v.shape
    for i, f in trap_f.items():
        dx, dz = RICHTING[f]
        pts = np.argwhere(v == i)
        if not len(pts):
            continue
        x, y, z = pts[:, 0] + dx, pts[:, 1] + 1, pts[:, 2] + dz
        ok = (x >= 0) & (x < X) & (y < Y) & (z >= 0) & (z < Z)
        x, y, z = x[ok], y[ok], z[ok]
        m = fam[v[x, y, z]]
        v[x[m], y[m], z[m]] = 0
        n += int(m.sum())
    return n


# =============================================================================================== whale skin detail
def huid_detail(W, S):
    """Decision 4: break up the big flat, stepped areas of the whale skin outside the city: mottled patches in three tones,
    cloud barnacles in little clusters, and soft cloud tufts on the back (only on the exposed top layer)."""
    import wolkwal as WW
    v = W.v
    ids = {k: W.id(P[k]) for k in ("huid", "huid_licht", "huid_pok", "buik")}
    huid_ids = np.array(list(ids.values()), np.uint16)
    X, Y, Z = v.shape
    # exposed top: highest non-air per column that is a skin block and not in the city
    heeft = v != 0
    top = np.where(heeft.any(1), Y - 1 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
    ti = np.clip(top, 0, Y - 1)
    blok = np.take_along_axis(v, ti[:, None, :], axis=1)[:, 0, :]
    skin = np.isin(blok, huid_ids) & (top > 0)
    stad = S.m
    sel = skin & ~stad
    xs = W.xs[:, None] * np.ones((1, Z))
    zs = np.ones((X, 1)) * W.zs[None, :]
    import schemerkit as SK
    n1 = SK.vnoise(xs, zs, cell=9.0, seed=31, oct=2)
    n2 = SK.vnoise(xs * 1.7, zs * 1.7, cell=5.0, seed=37, oct=1)
    h = SK.hash3(xs, np.zeros_like(xs), zs, 23)
    nieuw = np.where(n1 > 0.62, ids["huid_licht"], ids["huid"]).astype(np.uint16)
    nieuw = np.where((n2 > 0.78) & (h > 0.35), ids["huid_pok"], nieuw)
    nieuw = np.where((n1 < 0.22) & (h > 0.7), ids["huid_licht"], nieuw)
    keep_buik = blok == ids["buik"]
    nieuw = np.where(keep_buik, blok, nieuw)
    ii, kk = np.nonzero(sel)
    v[ii, ti[ii, kk], kk] = nieuw[ii, kk]
    # the layer below the top on steps (where the neighbour column is lower: the visible riser) gets the same pattern
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb = np.roll(np.roll(top, dx, 0), dz, 1)
        riser = sel & (nb < top - 0) & (nb > 0)
        ii, kk = np.nonzero(riser)
        for d in (1, 2):
            jj = np.clip(ti[ii, kk] - d, 0, Y - 1)
            m = np.isin(v[ii, jj, kk], huid_ids)
            v[ii[m], jj[m], kk[m]] = nieuw[ii[m], kk[m]]
    # cloud tufts: small soft bumps on the back (outside the city), sparse
    rng = np.random.RandomState(12)
    pts = np.argwhere(sel & (h > 0.993))
    wolk = "guhs:wolkenblok_wit"
    n = 0
    for (i, k) in pts:
        x, z = W.xs[i], W.zs[k]
        y = W.b[1] + top[i, k] + 1
        if not np.isnan(WW.rug(x, z)) and rng.rand() < 0.5:
            W.bol((x, y - 0.5, z), rng.uniform(1.2, 2.6), wolk, ry=rng.uniform(0.8, 1.4), keep=True)
            n += 1
    return n


# =============================================================================================== townsfolk
def bewoners(W, info):
    """Every render guh (W.guhs) and kit NPC as a spawn: (gx, gy, gz, yaw, skin, kind) in GAME coordinates."""
    import plan5 as PL
    out = []
    x1, y1, z1 = W.b[:3]
    for i, (x, y, z, yaw, skin, zit) in enumerate(W.guhs):
        gx, gy, gz = PL.GX0 - (x / W.k + x1), y / W.k + y1 + PL.DY, PL.GZ0 - (z / W.k + z1)
        soort = 2 if i in RIDDERS else (1 if zit else 0)
        out.append((gx, gy, gz, (yaw + 180) % 360, skin, soort))
    for (soort, x, y, z, yaw) in NPCS:
        skin = "npc_" + soort[len("guh_npc_"):] if soort.startswith("guh_npc_") else soort
        out.append((PL.GX0 - x, y + PL.DY, PL.GZ0 - z, (yaw + 180) % 360, skin, 3))
    # only skins that exist as an entity texture in the mod (else a plain guh)
    import os
    tex = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "src", "main", "resources", "assets", "guhs",
                       "textures", "entity")
    out = [(a, b_, c, d, s_ if os.path.exists(os.path.join(tex, s_ + ".png")) else "guh", e) for (a, b_, c, d, s_, e) in out]
    info["bewoners"] = out


# =============================================================================================== all
def bouw(W, S, log=print):
    import plan5 as PL
    info = dict(plekken={}, huizen=[], bewoners=[], uitkijken=[], vangnet=[])
    raadszaal(W, S, info)
    paleis(W, S, info)
    universiteit(W, S, info)
    academie(W, S, info)
    station(W, S, info)
    pieren(W, S, info)
    soepkeuken(W, S, info)
    kathedraal(W, S, info)
    log("grote gebouwen klaar")
    hz = huizen(W, S, info)
    log(f"huizen: {len(hz)} met deur + interieur")
    nt, mis = terrastrappen(W, S)
    log(f"terrastrappen: {nt} (passen niet: {mis})")
    wolkwaldroom(W, S, info)
    wolkplatformen(W, S, info)
    wezens_toegang(W, S, info)
    n = watervallen(W, S)
    kampvuren(W, S, info)
    nt = huid_detail(W, S)
    log(f"watervalroosters {n}, huid-plukjes {nt}")
    log(f"hekken boven trappen weg: {trap_hekken(W)}")
    bewoners(W, info)
    # viewpoints + catch spots (local -> game in poi5 via the plekken ids)
    if getattr(W, "balkon_oog", None):
        bx, by, bz = W.balkon_oog
        info["plekken"]["balkon_oog"] = (bx, by + 1, bz + 4)
        info["uitkijken"].append((bx, by + 1, bz + 4))
    info["uitkijken"] = [PL.spel(*u) for u in info["uitkijken"]]
    for k in ("kathedraal_plein", "markt_fontein", "luchthaven_station", "paleis_hof", "academie_oefenplein",
              "universiteit_binnenhof", "staartpark_prieel", "ooggaarde_tuin"):
        info["vangnet_ids"] = info.get("vangnet_ids", []) + [k]
    # planned spots without a height that lie in the city: on the city ground (not on a roof or a dome)
    for k, (x, y, z) in PL.plekken().items():
        if y is None and k not in info["plekken"]:
            g = S.g(x, z)
            if g is not None:
                info["plekken"][k] = (x, g + 1, z)
    # the house list in game coordinates
    for h in info.get("huizen_lokaal", []):
        info["huizen"].append(dict(id=h["id"], rol=h["rol"], deur=list(PL.spel(*h["deur"])), binnen=list(PL.spel(*h["binnen"]))))
    return info
