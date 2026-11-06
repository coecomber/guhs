"""Zone 4 "De Lampionkust": the PLAN (grid, areas, mob zones, every POI) - shared by bouw4 (the game parts), maak4 (tiles)
and poi4 (Z4WereldPlekken.java + POI_Z4.md).

World coordinates everywhere (the render kit in kit/ already works in world coordinates; no offset, no mirroring).
Island positions come from kit/indeling.py (copy of guhs1100-avontuur/wereldindeling/_bron/indeling.py).
Stand positions = the air block a player/NPC stands in. Heights come from the kit's height field (kust.hoogte) or from
the known deck heights; maak4 snaps every stand spot onto the built tiles afterwards (poicheck4.snap).
"""
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "kit"))

import kust as KU  # noqa: E402

ZEE = KU.ZEE                      # top water block (62); water surface at 63
T = 64
X0, Z0, X1, Z1 = KU.LAND_Z4       # 1740, -716, 2540, -266 (the land box of the world layout)
TX0, TZ0, TX1, TZ1 = 27, -12, 39, -5   # tile grid: x 1728..2559, z -768..-257
GX0, GZ0, GX1, GZ1 = TX0 * T, TZ0 * T, TX1 * T + T - 1, TZ1 * T + T - 1
# the Kringstroom (current ring): ellipse around (2150, -470), half axes 470 x 255 (kust.schuim); in game the push starts at
# STROOM_D (normalised ellipse distance) or STROOM_RAND blocks from the tile-grid edge, whatever comes first
STROOM = (2150, -470, 470, 255)
STROOM_D, STROOM_RAND = 0.93, 26
# the Grauwe Mistbank (south-east, towards the empire) around the Mysterie-eiland: fog ellipse (x, z, rx, rz) + full-fog core
MIST = (2490, -320, 120, 80)
MIST_KERN = (2490, -320, 60, 42)
EIL = KU.EIL
WRAK = KU.WRAK
SCHILDPAD_LUS = KU.SCHILDPAD_LUS
PIER = KU.HAVENS["z4_pier"]       # (naam, 2515, -565, 72, z4)
LUCHTHAVEN = (1796, -470, 70)     # haven.py: the airship platform of Lampionhaven (deck y 70, stand 71)
KY = ZEE + 3                      # haven.py: quay/jetty deck (65)

# island id -> (EIL name, level lo, hi, mobs?)
EILANDEN = {
    "lampionhaven": ("Lampionhaven", 50, 50, False),
    "knuffeltij": ("Knuffeltij-strand", 50, 51, False),
    "strandfeest": ("Strandfeesteiland", 51, 52, False),
    "bloemeneiland": ("Bloemeneiland", 51, 52, True),
    "koraalbaai": ("Koraalbaai", 53, 54, True),
    "vliegereiland": ("Vliegereiland", 52, 53, True),
    "vuurtoreneiland": ("Vuurtoreneiland", 54, 55, False),
    "pierkop": ("Pierkop", 55, 56, False),
    "mysterie": ("Mysterie-eiland", 55, 56, True),
}
# Dutch display names (lang): areas + biomes
NAMEN = {
    "lampionhaven": "Lampionhaven", "knuffeltij": "Het Knuffeltij-strand", "strandfeest": "Strandfeesteiland",
    "bloemeneiland": "Bloemeneiland", "koraalbaai": "De Koraalbaai", "vliegereiland": "Vliegereiland",
    "vuurtoreneiland": "Vuurtoreneiland", "pierkop": "Pierkop en de Opstijgpier", "mysterie": "Het Mysterie-eiland",
    "schildpad": "Het Schildpadeiland", "wrak": "Het Wrak van de Vliegervanger", "zee": "De Lampionkust",
}
NAMEN_EN = {
    "lampionhaven": "Lampionhaven", "knuffeltij": "Cuddle Tide Beach", "strandfeest": "Beach Party Island",
    "bloemeneiland": "Flower Island", "koraalbaai": "Coral Bay", "vliegereiland": "Kite Island",
    "vuurtoreneiland": "Lighthouse Island", "pierkop": "Pierhead and the Ascent Pier", "mysterie": "Mystery Island",
    "schildpad": "Turtle Island", "wrak": "Wreck of the Kite Catcher", "zee": "The Lantern Coast",
}


def y_grond(x, z):
    """Top ground block (kit height field)."""
    return KU.hoogte(x, z)[0]


def stand(x, z):
    """Stand y on the natural ground (never below the water surface)."""
    return max(y_grond(x, z), ZEE) + 1


# ------------------------------------------------------------------------------------------------ the schildpad (turtle)
_SA = math.radians(215)
SCHILDPAD_POS = (int(SCHILDPAD_LUS[0] + SCHILDPAD_LUS[2] * math.cos(_SA)), int(SCHILDPAD_LUS[1] + SCHILDPAD_LUS[3] * math.sin(_SA)))


SCHILDPAD_R, SCHILDPAD_RZ, SCHILDPAD_H = 24.0, 18.0, 12.0     # eilanden.schildpad: shell half axes (forward, side), height


def schildpad_assen():
    """(x0, z0, fx, fz): the static turtle's centre and swimming direction (eilanden.schildpad)."""
    tx, tz = -math.sin(_SA) * SCHILDPAD_LUS[2], math.cos(_SA) * SCHILDPAD_LUS[3]
    L = math.hypot(tx, tz)
    return SCHILDPAD_POS[0], SCHILDPAD_POS[1], tx / L, tz / L


def schildpad_lok(u, v):
    x0, z0, fx, fz = schildpad_assen()
    return x0 + fx * u - fz * v, z0 + fz * u + fx * v


def schildpad_top(x, z):
    """Top block of the static turtle shell at (x, z), or the sea floor outside it."""
    x0, z0, fx, fz = schildpad_assen()
    u = (x - x0) * fx + (z - z0) * fz
    v = -(x - x0) * fz + (z - z0) * fx
    d = (u / SCHILDPAD_R) ** 2 + (v / SCHILDPAD_RZ) ** 2
    if d > 1:
        return y_grond(x, z)
    return int(ZEE - 3 + SCHILDPAD_H * math.sqrt(1 - d))


def schildpad_route(n=24):
    """Waypoints of the swim loop (counter-clockwise, starting at the static fallback position)."""
    cx, cz, rx, rz = SCHILDPAD_LUS
    return [(int(cx + rx * math.cos(_SA + i * 2 * math.pi / n)), int(cz + rz * math.sin(_SA + i * 2 * math.pi / n)))
            for i in range(n)]


# ------------------------------------------------------------------------------------------------ boat docks (sailboat)
# island -> direction (dx, dz) the dock points to (towards the main sailing line); the dock is built where the beach meets
# water at least 2 deep, 9 blocks long, deck y 64 (stand 65)
DOK_RICHTING = {
    "knuffeltij": (1, 0), "strandfeest": (0, 1), "bloemeneiland": (-1, 0), "koraalbaai": (-1, 0), "vliegereiland": (0, 1),
    "vuurtoreneiland": (-1, 0), "pierkop": (0, 1), "mysterie": (-1, 0), "schildpad": (0, 1),
}
DOK_Y = ZEE + 2


def dok_plek(eiland):
    """(x0, z0, x1, z1, dx, dz): the dock from the last beach block (x0, z0) out to the end (x1, z1)."""
    if eiland == "schildpad":
        cx, cz = SCHILDPAD_POS
        rr = 22
    else:
        e = EIL[EILANDEN[eiland][0]]
        cx, cz, rr = e[1], e[2], max(e[3], e[4]) + 30
    dx, dz = DOK_RICHTING[eiland]
    x, z = cx, cz
    laatste = None
    for s in range(0, rr + 40):
        x, z = cx + dx * s, cz + dz * s
        y = y_grond(x, z)
        if y >= ZEE + 1:
            laatste = (x, z)
        elif laatste is not None and y <= ZEE - 2:
            break
    if laatste is None:
        laatste = (cx, cz)
    x0, z0 = laatste
    return (x0, z0, x0 + dx * 10, z0 + dz * 10, dx, dz)


# ------------------------------------------------------------------------------------------------ the houses (from the kit)
# haven.py / eilanden.py house origins (x, z) -> building id
HUIS_NAMEN = {
    (1840, -488): "vliegerwinkel", (1826, -452): "lampionwerkplaats", (1822, -470): "herberg", (1822, -484): "bank",
    (1840, -444): "winkel", (1849, -444): "kookhuis", (1857, -446): "havenmeester", (1812, -456): "stal",
    (1904, -500): "visrestaurant", (1868, -497): "botenverhuur", (2282, -653): "vliegerwerkplaats",
    (2408, -498): "vuurtorenwachter", (2079, -417): "bijenboerderij", (1925, -365): "opvangcentrum",
}
_HUIZEN = None


def huizen():
    """All houses of the kit (built once): [dict(naam, x, y, z, w, d, h, facing, deur, voor, op_palen)] with an id per house."""
    global _HUIZEN
    if _HUIZEN is None:
        import bouwsels as BW
        import haven as HV
        import eilanden as EI
        # every plek, always (a tile worker may have built only some of them: the overlay needs ALL houses), each once (cached)
        HV._plek()
        for n in EI.EILANDBOUW:
            if n not in ("bootje", "wrak"):
                EI.plek(n)
        uit, n = [], 0
        gezien = set()
        for h in sorted(BW.HUIZEN, key=lambda h: (h["x"], h["z"], h["y"])):     # same order in every process
            if (h["x"], h["z"]) in gezien:
                continue
            gezien.add((h["x"], h["z"]))
            h = dict(h)
            if (h["x"], h["z"]) in HUIS_NAMEN:
                h["id"] = HUIS_NAMEN[(h["x"], h["z"])]
            elif abs(h["x"] - SCHILDPAD_POS[0]) < 30 and abs(h["z"] - SCHILDPAD_POS[1]) < 30:
                h["id"] = "schildpadhutje"
            else:
                n += 1
                h["id"] = f"woning_{n}"
            uit.append(h)
        _HUIZEN = uit
    return _HUIZEN


def huis_punten(h):
    """deur (stand outside), binnen (interior centre), npc (interior, at the back wall), doos (box corners)."""
    x, y, z, w, d = h["x"], h["y"], h["z"], h["w"], h["d"]
    fx, fz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[h["facing"]]
    bx, bz = x + w // 2, z + d // 2
    # the npc stands at the back wall, opposite the door
    nx, nz = bx - fx * max(1, (w if fx else d) // 2 - 2), bz - fz * max(1, (d if fz else w) // 2 - 2)
    return {"deur": list(h["voor"]), "binnen": [bx, y, bz], "npc": [nx, y, nz],
            "doos_min": [x, y - 1, z], "doos_max": [x + w - 1, y + h["h"] + 4, z + d - 1]}


# ------------------------------------------------------------------------------------------------ the Wrak dungeon
# A stranded Mika war-kite ship on its rock: the hull (x 2150..2180, z -706..-694) holds 3 rooms below deck, the boss fights on
# the open fore deck (arena) next to the war-kite mast. Entrance: a ramp from the rock onto the stern deck, a hatch with stairs.
WRAK_HULL = (2150, 2180, -706, -694)     # x0, x1, z0, z1 (outer walls)
WRAK_VLOER = 56                           # floor block (stand 57) of the rooms below deck
WRAK_DEK = 66                             # deck floor (stand 67)
WRAK_KAMERS = [("ruim", 2171, 2179), ("kombuis", 2162, 2169), ("knuffelkooi", 2151, 2160)]   # x ranges (inner), stern -> bow


# ------------------------------------------------------------------------------------------------ the plan: every POI
def _p(x, y, z):
    return [int(x), int(y), int(z)]


def plekken():
    P = {}

    def put(k, x, y, z):
        P[k] = _p(x, y, z)

    # --- Lampionhaven
    py = y_grond(1848, -462) + 1     # Havenplein stand (haven.py)
    put("respawn", 1848, py, -467)
    put("plein", 1848, py, -467)
    put("plein_fontein", 1848, py, -466)
    put("questbord", 1842, py, -466)
    put("lampionmast", 1849, py, -457)
    put("kade", 1866, KY + 1, -470)
    for i, z in enumerate((-478, -464, -451)):
        put(f"steiger_{i + 1}", 1880, KY, z)            # haven.py: jetty deck KY - 1, stand KY
        put(f"steiger_{i + 1}_eind", 1891, KY, z)
    put("boot_dok_lampionhaven", 1891, KY, -464)
    put("boot_dok_lampionhaven_water", 1894, ZEE + 1, -464)
    lx, lz, ly = LUCHTHAVEN
    # line C (zone 3 -> 4): the airship port of Lampionhaven (haven_3_4_*)
    put("haven_3_4_landing", lx, ly + 1, lz + 2)
    put("haven_3_4_bel", lx + 3, ly + 1, lz - 2)
    put("haven_3_4_kapitein", lx + 2, ly + 1, lz + 3)
    put("haven_3_4_vertrekbord", lx + 3, ly + 1, lz + 1)
    put("haven_3_4_luchtschip", lx - 26, ly, lz)
    put("haven_3_4_trap_onder", lx + 6, y_grond(lx + 6, lz - 6) + 1, lz - 7)
    put("lampionduin_top", 1860, y_grond(1860, -517) + 1, -515)
    put("uitkijk_lampionduin", 1860, y_grond(1860, -517) + 1, -517)
    put("vuurtorentje", 1912, y_grond(1912, -431) + 1, -436)
    put("visrestaurant_terras", 1909, KY + 1, -489)
    put("werkplaats_lampion", 1837, y_grond(1837, -449) + 1, -447)
    put("werkplaats_vlieger", 2287, stand(2287, -644), -644)
    put("werkplaats_kookvuur", 1853, stand(1853, -440), -440)
    put("werkplaats_visplek", 1880, KY, -451)
    put("werkplaats_koraalbank", 2186, ZEE + 3, -595)          # on the diving jetty (north ring)
    put("stal_wei", 1808, stand(1808, -452), -452)
    put("bank_kluis", 1825, y_grond(1825, -480) + 1, -480)
    for h in huizen():
        for k, v in huis_punten(h).items():
            P[f"gebouw.{h['id']}.{k}"] = v
    # --- the islands: centre stand, NPC spots, campfires, docks
    for eid, (naam, lo, hi, _) in EILANDEN.items():
        e = EIL[naam]
        if eid != "lampionhaven":
            ex_, ez_ = (e[1] + 4, e[2] + 10) if eid == "vuurtoreneiland" else (e[1], e[2])     # not inside the big lighthouse
            put(f"eiland_{eid}", ex_, stand(ex_, ez_), ez_)
            x0, z0, x1, z1, dx, dz = dok_plek(eid)
            put(f"boot_dok_{eid}", x1, DOK_Y + 1, z1)
            put(f"boot_dok_{eid}_water", x1 + dx * 2 + (dz != 0) * 2, ZEE + 1, z1 + dz * 2 + (dx != 0) * 2)
            put(f"boot_dok_{eid}_wal", x0, stand(x0, z0), z0)
    _x0, _z0, _fx, _fz = schildpad_assen()
    _zx, _zz = schildpad_lok(0, SCHILDPAD_RZ + 2)
    _dx, _dz = (1 if -_fz > 0.5 else -1 if -_fz < -0.5 else 0), (1 if _fx > 0.5 else -1 if _fx < -0.5 else 0)
    if _dx and _dz:
        _dz = 0
    _zx, _zz = int(round(_zx)), int(round(_zz))
    put("boot_dok_schildpad", _zx + _dx * 5, ZEE + 2, _zz + _dz * 5)
    put("boot_dok_schildpad_water", _zx + _dx * 7 + (_dz != 0) * 2, ZEE + 1, _zz + _dz * 7 + (_dx != 0) * 2)
    # campfires (unlit = kern checkpoints): one per island + the town
    put("kampvuur_lampionhaven", 1839, py, -460)
    sf = EIL["Strandfeesteiland"]
    put("kampvuur_strandfeest", sf[1] - 6, y_grond(sf[1] - 6, sf[2] + 10) + 1, sf[2] + 10)      # eilanden.strandfeest
    kt = EIL["Knuffeltij-strand"]
    put("kampvuur_knuffeltij", kt[1] + 14, stand(kt[1] + 14, kt[2] + 2), kt[2] + 2)
    bl = EIL["Bloemeneiland"]
    put("kampvuur_bloemeneiland", bl[1] + 4, stand(bl[1] + 4, bl[2] + 4), bl[2] + 4)
    kb = EIL["Koraalbaai"]
    put("kampvuur_koraalbaai", kb[1] - 8, None or stand(kb[1] - 8, kb[2] - 50), kb[2] - 50)
    vl = EIL["Vliegereiland"]
    put("kampvuur_vliegereiland", vl[1] - 22, stand(vl[1] - 22, vl[2] + 24), vl[2] + 24)
    vt = EIL["Vuurtoreneiland"]
    put("kampvuur_vuurtoreneiland", vt[1] + 4, stand(vt[1] + 4, vt[2] + 14), vt[2] + 14)
    pk = EIL["Pierkop"]
    put("kampvuur_pierkop", pk[1] - 4, stand(pk[1] - 4, pk[2] - 8), pk[2] - 8)
    my = EIL["Mysterie-eiland"]
    put("kampvuur_mysterie", my[1] - 14, stand(my[1] - 14, my[2] + 6), my[2] + 6)
    # --- Knuffeltij-strand (story core)
    ox, oz = kt[1] - 10, kt[2] - 20                      # eilanden.knuffeltij: the Opvangcentrum origin
    put("knuffelwas", ox + 6, stand(ox + 6, oz + 13), oz + 13)
    put("droogveld", ox + 8, stand(ox + 8, oz + 19), oz + 19)
    put("soepkraam", ox + 19, stand(ox + 19, oz + 6), oz + 6)
    put("npc_opvangleider", ox + 8, y_grond(ox + 8, oz + 4) + 1, oz + 4)
    for i, (dx_, dz_) in enumerate(((-40, 22), (-30, 26), (-18, 30), (-4, 32), (8, 33), (20, 30), (32, 24), (44, 14))):
        x, z = kt[1] + dx_, kt[2] + dz_
        put(f"knuffeltij_knuffel_{i + 1}", x, stand(x, z), z)
    put("knuffeltij_feest", kt[1] + 2, stand(kt[1] + 2, kt[2] + 6), kt[2] + 6)
    # --- Strandfeesteiland
    put("strandfeest_bar", sf[1] + 8, None or stand(sf[1] + 8, sf[2] - 1), sf[2] - 1)
    put("npc_limonadeguh", sf[1] + 8, None or stand(sf[1] + 8, sf[2] - 6), sf[2] - 6)
    put("strandfeest_volleybal", sf[1] + 26, stand(sf[1] + 26, sf[2] + 2), sf[2] + 2)
    put("strandfeest_dansvloer", sf[1] - 6, stand(sf[1] - 6, sf[2] + 5), sf[2] + 5)
    # --- Bloemeneiland
    put("npc_imker", bl[1] - 2, y_grond(bl[1] - 2, bl[2] - 10) + 1, bl[2] - 10)
    put("bijenkorven", bl[1] + 9, stand(bl[1] + 9, bl[2] - 1), bl[2] - 1)
    put("picknick_1", bl[1] + 11, stand(bl[1] + 11, bl[2] + 13), bl[2] + 13)
    put("picknick_2", bl[1] - 13, stand(bl[1] - 13, bl[2] + 11), bl[2] + 11)
    put("picknick_3", bl[1] + 19, stand(bl[1] + 19, bl[2] - 1), bl[2] - 1)
    put("kransenbank", bl[1] + 2, stand(bl[1] + 2, bl[2] + 2), bl[2] + 2)
    # --- Koraalbaai: diving jetty from the north ring, the lagoon arena (rafts), the 3 underwater grottos
    put("duiksteiger", kb[1], ZEE + 3, kb[2] - 40)
    put("duiksteiger_eind", kb[1], ZEE + 3, kb[2] - 20)
    put("npc_duikmeester", kb[1] + 1, ZEE + 3, kb[2] - 38)
    put("arena_zeeslurf", kb[1], ZEE + 2, kb[2])            # the round raft in the middle of the lagoon (deck ZEE + 1)
    put("arena_zeeslurf_deur", kb[1], ZEE + 3, kb[2] - 20)  # = end of the diving jetty (where you wait)
    put("arena_zeeslurf_bord", kb[1] - 1, ZEE + 3, kb[2] - 22)
    put("zeeslurf_grotje", kb[1] - 14, y_grond(kb[1] - 14, kb[2]) - 8 + 2, kb[2])
    import eilanden as EI
    for gi, (dx_, dz_, r) in enumerate(EI.GROTJES):
        gx, gz = kb[1] + dx_, kb[2] + dz_
        gy = y_grond(gx, gz) - r + 2
        put(f"grotje_{gi + 1}", gx, int(gy - r * 0.55) + 1, gz)
        put(f"grotje_{gi + 1}_uitgang", gx, ZEE + 1, gz)
    # --- Vliegereiland
    put("wedstrijdpaal", vl[1] + 2, stand(vl[1] + 2, vl[2]), vl[2])
    put("uitkijk_vliegerheuvel", vl[1] + 1, stand(vl[1] + 1, vl[2] + 2), vl[2] + 2)
    put("vliegerwedstrijd_start", vl[1] - 6, stand(vl[1] - 6, vl[2] + 6), vl[2] + 6)
    put("vliegerwedstrijd_finish", vl[1] + 30, stand(vl[1] + 30, vl[2] + 10), vl[2] + 10)
    # --- Vuurtoreneiland: the big lighthouse (door south, spiral stair up to the gallery) + the keeper's house
    vy = max(y_grond(x, z) for x in range(vt[1] - 6, vt[1] + 7, 2) for z in range(vt[2] - 6, vt[2] + 7, 2)) + 1
    put("vuurtoren_deur", vt[1], vy, vt[2] + 8)
    put("vuurtoren_binnen", vt[1], vy, vt[2])
    put("uitkijk_vuurtoren", vt[1], vy + 39, vt[2] + 5)     # the gallery (h 38): floor vy + 38, stand + 1
    put("vuurtoren_lamp", vt[1], vy + 39, vt[2])
    # --- Pierkop + Opstijgpier: line D (zone 4 -> 5), haven_4_5_*
    px, pz, pyy = PIER[1], PIER[2], PIER[3]
    put("opstijgpier", px - 4, pyy + 1, pz)
    put("haven_4_5_landing", px + 2, pyy + 1, pz + 3)
    put("haven_4_5_bel", px - 5, pyy + 1, pz - 5)
    put("haven_4_5_kapitein", px - 3, pyy + 1, pz + 1)
    put("haven_4_5_vertrekbord", px - 5, pyy + 1, pz - 5)
    put("haven_4_5_mast", px, pyy + 31, pz)
    put("haven_4_5_luchtschip", px + 24, pyy, pz)
    put("opstijgpier_begin", pk[1] + 2, pyy + 1, pz)
    put("opstijgpier_cliffhanger", px + 6, pyy + 1, pz)
    put("uitkijk_pierkop", px - 6, pyy + 1, pz + 6)
    # --- Mysterie-eiland
    put("mysterie_poort", my[1], stand(my[1], my[2] + 3), my[2] + 3)
    put("mysterie_flash", my[1], stand(my[1], my[2] - 2), my[2] - 2)
    put("mysterie_strand", my[1] - 26, stand(my[1] - 26, my[2] + 4), my[2] + 4)
    # --- Schildpadeiland (static fallback position + the loop)
    sx, sz = SCHILDPAD_POS
    put("schildpad", sx, ZEE + 11, sz)
    _rx, _rz = schildpad_lok(6, 0)
    put("schildpad_rug", int(_rx), schildpad_top(int(_rx), int(_rz)) + 1, int(_rz))
    put("schildpad_lus_midden", SCHILDPAD_LUS[0], ZEE + 1, SCHILDPAD_LUS[1])
    # --- Wrak van de Vliegervanger (small dungeon)
    wx0, wx1, wz0, wz1 = WRAK_HULL
    wzm = (wz0 + wz1) // 2
    put("wrak_aanleg", wx1 + 12, ZEE + 2, wzm)
    put("boot_dok_wrak", wx1 + 12, ZEE + 2, wzm)
    put("boot_dok_wrak_water", wx1 + 12, ZEE + 1, wzm + 3)
    put("wrak_ingang", wx1 - 2, WRAK_DEK + 1, wzm)
    put("wrak_luik", 2176, WRAK_DEK + 1, wz0 + 1)
    for naam, a, b in WRAK_KAMERS:
        put(f"wrak_{naam}", (a + b) // 2, WRAK_VLOER + 1, wzm)
    put("wrak_schat", wx0 + 3, WRAK_VLOER + 1, wzm + 3)
    put("arena_vliegervanger", wx0 + 8, WRAK_DEK + 1, wzm)
    put("arena_vliegervanger_deur", wx0 + 15, WRAK_DEK + 1, wzm)
    put("arena_vliegervanger_bord", wx1 - 2, WRAK_DEK + 1, wzm + 3)
    put("wrak_oorlogsvlieger", wx0 + 4, WRAK_DEK + 12, wzm)
    # --- viewpoints + secrets
    put("geheim_knuffel_1", 1916, stand(1916, -426), -426)                 # behind the little lighthouse (south arm tip)
    put("geheim_knuffel_2", kb[1] + 16, None or (y_grond(kb[1] + 16, kb[2] + 2) - 4 + 2 - 2), kb[2] + 2)   # grotje 2
    put("geheim_knuffel_3", 2350, stand(2350, -585), -585)                  # rotsje between Koraalbaai and the Vuurtoren
    put("geheim_knuffel_4", my[1] + 8, stand(my[1] + 8, my[2] + 4), my[2] + 4)   # behind the grey gate's rubble
    put("geheim_knuffel_5", 1790, stand(1790, -610), -610)                  # the big rock south-west of Lampionhaven
    put("geheim_schat_1", px + 4, ZEE + 2, pz + 1)                          # under the Opstijgpier platform (a raft)
    put("geheim_schat_2", 2240, stand(2240, -640), -640)                    # the sandbank south of the Vliegereiland
    put("geheim_schat_3", kb[1] - 2, None or (y_grond(kb[1] - 2, kb[2] - 14) - 6 + 2 - 3), kb[2] - 14)   # grotje 3
    put("geheim_schat_4", 2425, stand(2425, -400), -400)                    # rock east of the turtle loop
    return P


# ------------------------------------------------------------------------------------------------ areas + mob zones
def gebieden():
    """(id, cx, cz, rx, rz, lo, hi, stad): ellipses around the islands (first match wins) + the whole zone last."""
    g = [("z4_lampionhaven", 1846, -470, 78, 72, 50, 50, True)]
    for eid, (naam, lo, hi, _) in EILANDEN.items():
        if eid == "lampionhaven":
            continue
        e = EIL[naam]
        g.append((f"z4_{eid}", e[1], e[2], e[3] + 26, e[4] + 26, lo, hi, False))
    g.append(("z4_wrak", WRAK[0], WRAK[1], 30, 22, 54, 55, False))
    sx, sz = SCHILDPAD_LUS[0], SCHILDPAD_LUS[1]
    g.append(("z4_schildpad", sx, sz, SCHILDPAD_LUS[2] + 30, SCHILDPAD_LUS[3] + 30, 52, 53, False))
    g.append(("z4_zee", STROOM[0], STROOM[1], STROOM[2] + 40, STROOM[3] + 60, 50, 56, False))
    return g


def mobzones():
    """(id, cx, cz, rx, rz, lo, hi): only on islands with mobs (never in Lampionhaven, Knuffeltij, Strandfeest, Vuurtoren,
    Pierkop or on the turtle); keep ~12 blocks from the houses/jetties (mob slice: check PADEN too)."""
    z = []
    for eid, (naam, lo, hi, mobs) in EILANDEN.items():
        if not mobs:
            continue
        e = EIL[naam]
        z.append((f"z4_{eid}", e[1], e[2], e[3] - 6, e[4] - 6, lo, hi))
    z.append(("z4_wrak", WRAK[0], WRAK[1], 18, 8, 54, 55))
    return z


def leven():
    """Lists for atmosphere code: lantern strings / lampion posts (glow), chimneys, beehives."""
    import bouwsels as BW
    return {"SCHOORSTENEN": [[h["x"] + 1, h["y"] + h["h"] + 2, h["z"] + 1] for h in huizen()],
            "BIJENKORVEN": [[EIL["Bloemeneiland"][1] - 6 - 4 + (i % 3) * 3 + 12, stand(EIL["Bloemeneiland"][1] + 2, EIL["Bloemeneiland"][2]) + 1,
                             EIL["Bloemeneiland"][2] - 12 + 8 + (i // 3) * 3] for i in range(6)]}
