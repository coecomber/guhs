"""T-WERELD: THE world layout of Het Vadsrijk (wereldindeling W-3) for the map generators.

Mirror of src/main/java/nl/juiced/guhs/feature/vadsrijk/indeling/VadsrijkZones.java (change BOTH, then regenerate the map).
Every zone keeps building in its own LOCAL coordinates;   world = local + (dx, dz)   (y never shifts). Offsets are multiples of
64, so a local tile (tx, tz) becomes world tile (tx + dx/64, tz + dz/64).
"""

TEGEL = 64

# zone -> (dx, dz); local = world for zone 1, 4, 5
KAMP_DX, KAMP_DZ = -384, 0          # Knuffelkamp: local = the old camp coordinates (island around (0, 1000))
Z2_DX, Z2_DZ = 704, 1344            # zone 2: local = the old zone-2 frame (Kandijkom (0, -2400), zone2/plan.py)
Z3_DX, Z3_DZ = 1404, -576           # zone 3: local = relative to the trunk axis (zone3/plan3.py TX_STAM/TZ_STAM)

# tile boxes (inclusive tile indices, WORLD) per zone (WERELDINDELING §2)
TEGELS = {
    "kamp": (-8, 13, -4, 18),
    "z1": (-11, -11, 11, 9),
    "z2": (6, -23, 15, -13),
    "z3": (16, -16, 26, -3),
    "z4": (27, -12, 39, -5),
    "z5": (38, -12, 49, -5),
}
# land (content) boxes, WORLD: minx, minz, maxx, maxz
LAND = {
    "kamp": (-488, 886, -258, 1106),
    "z1": (-640, -650, 640, 650),
    "z2": (434, -1436, 974, -806),
    "z3": (1166, -897, 1725, -338),
    "z4": (1740, -716, 2540, -266),
    "z5": (2440, -660, 3150, -380),
}

# airship harbours of the transitions (W-3 §4): id -> (name, x, y-stand, z, zone). POI ids: haven_<van>_<naar>_vertrek
HAVENS = {
    "haven_z1_z2_vertrek": ("Noordklifhaven", 150, 121, -600, "z1"),
    "haven_z2_z1_vertrek": ("Kandijkom-dok", 704, 91, -900, "z2"),
    "haven_z2_z3_vertrek": ("Galmdok", 955, 111, -835, "z2"),
    "haven_z3_z2_vertrek": ("Weststeiger", 1190, 171, -590, "z3"),
    "haven_z3_z4_vertrek": ("Oosttaksteiger", 1630, 171, -500, "z3"),
    "haven_z4_z3_vertrek": ("Lampionhaven (luchthaven)", 1800, 71, -470, "z4"),
}
# viewpoints at the transitions (W-3 §5.2)
UITKIJK = {
    "u1_noordkliftop": ("Noordkliftop", 300, 151, -560, "z1"),
    "u2_kandijkom_balkon": ("Kandijkom-balkon", 690, 101, -880, "z2"),
    "u3_galmkam": ("Galmkam", 940, 151, -980, "z2"),
    "u4_westtak": ("Westtak-uitkijk", 1200, 176, -640, "z3"),
    "u6_oosttak": ("Oosttak-uitkijk", 1645, 181, -470, "z3"),
}


def kamp(x, z):
    return x + KAMP_DX, z + KAMP_DZ


def z2(x, z):
    return x + Z2_DX, z + Z2_DZ


def z2_lokaal(x, z):
    return x - Z2_DX, z - Z2_DZ


def schuif(o, dx, dz):
    """Shift every [x, y, z] int triple (and {min,max} boxes) in a nested POI tree by (dx, dz)."""
    if isinstance(o, list) and len(o) == 3 and all(isinstance(v, (int, float)) for v in o):
        return [type(o[0])(o[0] + dx), o[1], type(o[2])(o[2] + dz)]
    if isinstance(o, dict):
        return {k: schuif(v, dx, dz) for k, v in o.items()}
    if isinstance(o, list):
        return [schuif(v, dx, dz) for v in o]
    return o
