"""
2.10.1: a Reisguh (the sky-blue guh-conductor, a travel waypoint: quest/Reisguh.java) in the big buildings of the
Guhmension. Not a feature module (not in features.FEATURES): the builders of the Elf-Guhjestocht (Guhwarden), the
Knuffeldal stadje, the Guhkermis, Guhland, the Ballonfestival and the Guhcircuit call zet() on their template.

zet() tries the given spots in order and puts her on the first one with a solid floor, two free blocks for her and nobody
standing within 1.5 blocks; no spot at all is an error (the generator stops), so a rebuilt building never silently loses her.
Her name comes with the template (ReisName); the first time she registers in a world where that name is already taken
(a second Guhkermis...) Reisguh.java adds her coordinates.
"""
import math

AIR = "minecraft:air"
# what she can stand in (air, plants, snow layers, carpets...) and what's no floor (fences, plants, lamps, signs...)
DOOR = ("air", "short_grass", "tall_grass", "fern", "flower", "poppy", "dandelion", "tulip", "daisy", "orchid", "allium",
        "bluet", "cornflower", "lily_of_the_valley", "snow", "carpet", "rijpsprietjes", "ijsbloempje", "sprietjes", "bloem")
GEEN_VLOER = ("fence", "wall", "sign", "lantern", "lampion", "torch", "flower", "sapling", "carpet", "slab", "plaat",
              "pane", "bars", "chain", "button", "rail", "door", "trapdoor", "banner", "leaves", "bladeren", "short_grass",
              "tall_grass", "sprietjes", "ijsbloempje")


def _vrij(name):
    if name is None or name == AIR:
        return True
    kort = name.split(":")[-1]
    return any(t in kort for t in DOOR) and "block" not in kort


def _vloer(name):
    if name is None or name == AIR:
        return False
    kort = name.split(":")[-1]
    if kort in ("snow_block", "grass_block"):
        return True
    if kort == "snow":
        return False
    return not any(t in kort for t in GEEN_VLOER) and not _vrij(name)


def past(s, x, y, z):
    """Can she stand in block (x, y, z) of the template?"""
    if not (s.inside(x, y, z) and s.inside(x, y + 1, z)):
        return False
    if not (_vloer(s.get(x, y - 1, z)) and _vrij(s.get(x, y, z)) and _vrij(s.get(x, y + 1, z))):
        return False
    for (ex, ey, ez, _nbt) in s.entities:
        if math.hypot(ex - (x + 0.5), ez - (z + 0.5)) < 1.5 and abs(ey - y) < 2:
            return False
    return True


def nbt(naam, yaw, Byte, floats):
    return {"id": "guhs:guh_npc", "Kind": "reisguh", "ReisName": naam, "PersistenceRequired": Byte(1),
            "Rotation": floats(float(yaw), 0.0)}


def zet(s, spots, naam, yaw, Byte, floats, waar=""):
    """Puts the Reisguh `naam` on the first spot of `spots` ((x, y, z): the block she stands in; y = floor + 1) that fits,
    looking `yaw` (0 south, 90 west, 180 north, -90 east). Returns the spot."""
    for (x, y, z) in spots:
        if past(s, x, y, z):
            s.entity(x + 0.5, float(y), z + 0.5, nbt(naam, yaw, Byte, floats))
            return (x, y, z)
    detail = ", ".join(f"{p}: {s.get(p[0], p[1] - 1, p[2])}/{s.get(*p)}/{s.get(p[0], p[1] + 1, p[2])}" for p in spots[:6])
    raise SystemExit(f"reisguh {naam} {waar}: no free spot ({detail})")


def rondom(x, y, z, straal=3):
    """Spots around (x, y, z), nearest first (for zet): the spot itself, then rings of 1..straal, a floor 1 up or down too."""
    uit = [(x, y, z)]
    for r in range(1, straal + 1):
        ring = [(x + dx, y, z + dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) if max(abs(dx), abs(dz)) == r]
        ring.sort(key=lambda p: (abs(p[0] - x) + abs(p[2] - z), p))
        uit += ring
    return uit + [(p[0], p[1] + dy, p[2]) for dy in (1, -1) for p in uit]


def aantal(s):
    """How many Reisguhs the template holds (for the builders' self-checks)."""
    return sum(1 for e in s.entities if e[3].get("id") == "guhs:guh_npc" and e[3].get("Kind") == "reisguh")
