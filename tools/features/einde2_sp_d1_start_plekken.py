"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), slice sp_d1_start: what this slice adds to the BUILD of Bikinibodem (markers and scene
cameras; no blocks). Called by einde2_sp_plekken from the build module right before its self-check, so every marker is checked
with the build (reach, light) and every camera line (b.camera: a free line from the eye to what it looks at).

Markers (all prefixed d1s_):
  d1s_krat_1..5        the five crates of the grill game round grill_speler (Java Bakken.KRATTEN_REL: the same offsets):
                       bun, burger, cheese, sea lettuce, pickle
  d1s_knabbel_deur     on the Hoofdstraat right in front of the double door of De Krokante Knabbel (the hint of step 3)
  d1s_<scene>_camera   the key camera shots of the scenes (template coordinates = anchor + the scene's relative position,
                       rounded down), only so the kit checks their lines of sight
"""

import math

# the offsets of the crates from grill_speler (Java: Bakken.KRATTEN_REL)
KRATTEN = [(-2, 0, 0), (-2, 0, 2), (0, 0, 3), (2, 0, 2), (4, 0, 1)]


def _pos(b, id):
    return tuple(b.plekken[id]["pos"])


def _plus(a, d):
    return tuple(int(math.floor(a[i] + d[i])) for i in range(3))


def bikinibodem(b):
    gx, gy, gz = _pos(b, "grill_speler")
    for i, (dx, dy, dz) in enumerate(KRATTEN, 1):
        b.prop(f"d1s_krat_{i}", gx + dx, gy + dy, gz + dz, wie=f"sp_d1_start: krat {i} van het grillspel (per speler, een display)")
    b.prop("d1s_knabbel_deur", 139, 16, 108, wie="sp_d1_start: op de Hoofdstraat voor de deur van De Krokante Knabbel (hint stap 3)")
    # the scene cameras (anchor + the relative eye / look-at of the Java scene, D1Scenes)
    eiland, voet, krabguh, grill = _pos(b, "aankomst_speler"), _pos(b, "bubbel_onder"), _pos(b, "krabguh"), _pos(b, "grill_speler")
    b.camera("d1s_aankomst_stad", [_plus(eiland, (14, -44, 30)), _plus(eiland, (6, -62, 20))], _plus(eiland, (2, -92, -2)))
    b.camera("d1s_aankomst_knabbel", [_plus(eiland, (20, -84, 8)), _plus(eiland, (26, -86, 4))], _plus(eiland, (33.5, -91.2, -6)))
    b.camera("d1s_aankomst_bubbel", _plus(eiland, (-2.5, 2.4, 4.5)), _plus(eiland, (0.5, 1.0, -3.5)))
    b.camera("d1s_welkom", _plus(voet, (-3.5, 2.6, 6.5)), _plus(voet, (6, 1.0, 1.5)))
    b.camera("d1s_sollicitatie_tellen", _plus(krabguh, (-1.6, 1.5, -1.0)), _plus(krabguh, (0.5, 0.9, 0.5)))
    b.camera("d1s_sollicitatie", _plus(krabguh, (-1.8, 2.0, 2.9)), _plus(krabguh, (0.5, 0.9, 0.6)))
    b.camera("d1s_sollicitatie_octo", _plus(krabguh, (-6.5, 2.4, 9.0)), _plus(krabguh, (-6.5, 2.5, 4.5)))
    b.camera("d1s_bakken_uitleg", _plus(grill, (-2.5, 2.1, 4.2)), _plus(grill, (1.5, 0.9, 0.3)))
    b.camera("d1s_bakken_kratten", _plus(grill, (0.5, 2.7, -0.3)), _plus(grill, (0.6, 0.4, 2.0)))
    b.camera("d1s_bakken_klaar_octo", _plus(grill, (0.8, 2.3, 3.6)), _plus(grill, (1.5, 2.4, 7.0)))
    b.camera("d1s_bakken_klaar_patguh", _plus(grill, (3.0, 2.0, 29.0)), _plus(grill, (5.5, 1.1, 24.5)))


def schelpenstad(b):
    pass
