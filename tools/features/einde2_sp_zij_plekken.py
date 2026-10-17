"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), slice sp_zij: what this slice adds to the BUILD of Bikinibodem: markers only, no
blocks. Called by einde2_sp_plekken from the build module right before its self-check, so every marker is checked with the
build (reach from the island, light, camera lines).

  zij_lantaarn_1..3    where the three ghost lanterns of De Vliegende Guhlander lie (things of one player, Java Guhlander):
                       among the Kruimelkwallen east of the mooring, behind De Vetemmer, beside Sponsguh's house in the west.
                       Each is the free, walkable cell nearest to its target (searched here, so a later change of the town
                       moves the marker instead of burying it).
  zij_<scene>_<n>      the cameras of the scenes of SpZijScenes (each shot's first eye point and what it looks at, relative
                       to the scene's anchor marker): the kit proves a free line to the speakers' markers. The scene
                       hakker plays on the ghost ship (an overlay, not part of the build): nothing to check there.
"""

# the three lanterns: id -> (target x, z in template coordinates, what it is)
LANTAARNS = {
    "zij_lantaarn_1": (156, 172, "spooklantaarn 1 van de spookkapitein: tussen de Kruimelkwallen, oost van de aanlegplaats"),
    "zij_lantaarn_2": (139, 151, "spooklantaarn 2 van de spookkapitein: achter De Vetemmer, noord van de aanlegplaats"),
    "zij_lantaarn_3": (76, 172, "spooklantaarn 3 van de spookkapitein: bij de knabbelkoorvrucht van Sponsguh, in het westen"),
}

# the scene cameras (SpZijScenes): scene -> (anchor marker, [(eye, look-at), ...] relative to the anchor block, markers to see)
CAMERAS = {
    "sandy": ("sandy", [((4.8, 2.0, 5.6), (0.5, 1.3, 2.5)), ((1.6, 1.8, 6.8), (0.5, 1.2, 0.5)), ((0.5, 1.8, 7.4), (0.5, 1.7, 4.5)),
                        ((4.8, 2.3, 5.2), (0.5, 1.3, 2.5))], ["sandy"]),
    "sandy_raket": ("raket", [((6.5, 2.6, 7.5), (1.5, 2.4, 1.0)), ((4.5, 1.9, 4.6), (2.6, 1.3, 2.0)), ((5.5, 1.5, 6.5), (0.5, 5.0, 0.5)),
                              ((6.5, 2.4, 7.0), (1.5, 2.2, 1.5))], ["raket"]),
    "guhlander": ("guhlander_trap", [((6.5, 2.0, 8.5), (0.5, 6.0, -2.0)), ((2.5, 1.8, 5.0), (0.5, 1.4, 1.5))], ["guhlander_trap"]),
    "helden": ("zeemeerman", [((-1.0, 2.2, 5.6), (-0.6, 1.1, 1.5)), ((-2.4, 1.9, 3.4), (0.5, 1.1, 1.0)), ((-1.6, 1.7, 0.2), (0.5, 1.0, 1.6))],
               ["zeemeerman", "mosselguh"]),
}


def _cel(anker, rel):
    import math
    return tuple(int(math.floor(a + r)) for a, r in zip(anker, rel))


def _vrij(b, x, y, z):
    """A cell you can stand in: a solid floor under it, feet and head free."""
    return b.solid_at((x, y - 1, z)) and not b.solid_at((x, y, z)) and not b.solid_at((x, y + 1, z))


def _zoek(b, tx, tz, y0=16):
    """The free, walkable cell nearest to (tx, tz) at walking level (y0 .. y0 + 2), in rings up to 8 blocks out."""
    for r in range(0, 9):
        ring = sorted({(tx + dx, tz + dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) if max(abs(dx), abs(dz)) == r},
                      key=lambda c: ((c[0] - tx) ** 2 + (c[1] - tz) ** 2, c))
        for (x, z) in ring:
            for y in (y0, y0 + 1, y0 + 2):
                if _vrij(b, x, y, z):
                    return x, y, z
    return None


def bikinibodem(b):
    for mid, (tx, tz, wie) in LANTAARNS.items():
        cel = _zoek(b, tx, tz)
        if cel is None:
            b.fouten.append(f"sp_zij: geen vrije plek voor {mid} bij ({tx}, {tz})")
            continue
        b.prop(mid, *cel, wie=wie)
    for scene, (anker_id, shots, zicht) in CAMERAS.items():
        anker = b.plekken[anker_id]["pos"]
        for i, (oog, kijk) in enumerate(shots, start=1):
            b.camera(f"zij_{scene}_{i}", _cel(anker, oog), _cel(anker, kijk), zicht=zicht if i == 1 else (),
                     wie=f"de camera van scene einde2_sp_zij_{scene}, shot {i}")


def schelpenstad(b):
    pass
