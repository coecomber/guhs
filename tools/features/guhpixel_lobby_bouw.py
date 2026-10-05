"""
The lobby island of Guhpixel: template guhs:guhpixel/lobby (Java: feature/guhpixel/Lobby, LobbyPlek).

PLACEHOLDER from the foundation: a flat round plaza with every anchor pad and the exit portal. The lobby slice replaces
build() with the real lobby (logo, store, roofs, parkour); it must keep:
  - the size exactly 97 x (at most 96) x 97; the template is stamped with its min corner at world (-48, 68, -48), so
    local (48, 32, 48) = world (0, 100, 0) = the spawn point (feet) and the plaza floor is local y 31 (world y 99);
  - every anchor of ANKERS: a 5 x 5 pad (anchor +-2 in x and z) with a solid floor at world y 99 and 4 free blocks above,
    and NO NPC of an anchor in the template (LobbyNpcs places them);
  - the exit portal: guhs:guhpixel_portaal[soort=uit] at world x -1..1, y 100..102, z 30;
  - no free-flowing water or lava (they would flow forever in the void).
Bump VERSIE whenever the template changes: a server that already has a lobby then clears the box and stamps it again.
"""
import math
import os
import re

VERSIE = 1
NAME = "guhpixel/lobby"
W, H = 97, 40                    # (H may grow to 96)
MIN = (-48, 68, -48)             # world position of the template's min corner
VLOER = 99                       # world y of the plaza floor

# name: (x, y, z, yaw) in world coordinates; a copy of LobbyPlek.java (checked by selfcheck)
ANKERS = {
    "SPAWN": (0, 100, 0, 180), "WELKOM": (3, 100, -5, 0), "SPEL_SKYBLOK": (-24, 100, -16, 0), "SPEL_BEDWARS": (-16, 100, -21, 0),
    "SPEL_VADSNITE": (-8, 100, -24, 0), "SPEL_AMONG": (0, 100, -26, 0), "SPEL_GUHMON": (8, 100, -24, 0), "SPEL_BZG": (16, 100, -21, 0),
    "SPEL_RESERVE": (24, 100, -16, 0), "BORD_AMONG": (4, 100, -29, 0), "WINKEL": (-28, 100, 6, -90), "BORD_STATS": (-10, 100, 14, 180),
    "BORD_ONLINE": (10, 100, 14, 180), "PARKOUR_START": (28, 100, 6, -90), "UITGANG": (0, 100, 28, 0),
}
PORTAAL = [(x, y, 30) for x in (-1, 0, 1) for y in (100, 101, 102)]
LUCHT = (None, "minecraft:air")


def lokaal(x, y, z):
    """World coordinates -> template coordinates."""
    return x - MIN[0], y - MIN[1], z - MIN[2]


def build(h):
    s = h.Structure((W, H, W))
    straal = 44
    for x in range(-48, 49):
        for z in range(-48, 49):
            d = math.hypot(x, z)
            if d > straal + 0.5:
                continue
            # two layers: pink concrete with white rings, on a cheese-yellow underside
            ring = int(d) % 8 == 7
            s.set(*lokaal(x, VLOER, z), "minecraft:white_concrete" if ring else "minecraft:pink_concrete")
            s.set(*lokaal(x, VLOER - 1, z), "minecraft:yellow_terracotta")
            if d > straal - 0.5:
                s.set(*lokaal(x, VLOER + 1, z), "minecraft:pink_stained_glass")   # a low rim, so nobody rolls off
    # the pads: a cheese-coloured floor, magenta under the spawn point
    for naam, (ax, ay, az, _) in ANKERS.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                s.set(*lokaal(ax + dx, VLOER, az + dz), "minecraft:magenta_concrete" if naam == "SPAWN" else "minecraft:yellow_concrete")
        s.set(*lokaal(ax, VLOER, az), "minecraft:sea_lantern")
    # the exit portal with a simple frame: posts beside it, a lintel above it, a wall behind it
    for (x, y, z) in PORTAAL:
        s.set(*lokaal(x, y, z), "guhs:guhpixel_portaal", {"soort": "uit"})
    for y in range(100, 105):
        for x in (-3, 3):
            s.set(*lokaal(x, y, 30), "minecraft:lime_concrete")
        for x in range(-3, 4):
            s.set(*lokaal(x, y, 31), "minecraft:white_concrete")
    for x in range(-3, 4):
        s.set(*lokaal(x, 104, 30), "minecraft:lime_concrete")
    controleer(s)
    s.save(NAME)
    h.w(f"{h.D}/guhpixel/lobby_versie.json", {"versie": VERSIE})
    return s


def controleer(s):
    """The fixed geometry every version of the lobby must keep (see the module comment)."""
    problems = []
    if s.size[0] != 97 or s.size[2] != 97 or s.size[1] > 96:
        problems.append(f"the lobby must be 97 x (max 96) x 97, not {s.size}")
    for naam, (ax, ay, az, _) in ANKERS.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if s.get(*lokaal(ax + dx, ay - 1, az + dz)) in LUCHT:
                    problems.append(f"anchor {naam}: no floor at {(ax + dx, ay - 1, az + dz)}")
                for dy in range(4):
                    b = s.get(*lokaal(ax + dx, ay + dy, az + dz))
                    if b not in LUCHT and b != "guhs:guhpixel_portaal":
                        problems.append(f"anchor {naam}: {b} in the way at {(ax + dx, ay + dy, az + dz)}")
    for (x, y, z) in PORTAAL:
        b = s.blocks.get(lokaal(x, y, z))
        if not b or b[0] != "guhs:guhpixel_portaal" or b[1].get("soort") != "uit":
            problems.append(f"no exit portal block at {(x, y, z)}")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b in ("minecraft:water", "minecraft:lava"):
            problems.append(f"free {b} at template {(x, y, z)}")
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "LobbyPlek.java")
    if os.path.exists(java):
        src = open(java, encoding="utf-8").read()
        found = {m.group(1): tuple(int(float(v)) for v in m.group(2, 3, 4, 5))
                 for m in re.finditer(r"^\s+([A-Z_]+)\((-?\d+), (-?\d+), (-?\d+), (-?\d+)\)[,;]", src, re.M)}
        if found != ANKERS:
            problems.append("ANKERS is not the same table as LobbyPlek.java")
    if problems:
        raise SystemExit("guhpixel lobby check failed:\n  " + "\n  ".join(problems[:40]))
