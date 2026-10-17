"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), slice sp_d1_recept: what this slice adds to the BUILD of Bikinibodem (markers and scene
cameras only: not one block). Called by einde2_sp_plekken from the build module right before its self-check, so every camera
line and every spot below is checked with the build (reach, light, free lines from each eye to what it looks at).

Prefix d1r_. Template coordinates (walking level y 16; the stage of the Bellenkom y 18):
  d1r_kk_deur          the street in front of De Krokante Knabbel (where "Verder met het verhaal" puts you for steps 7 and 9)
  d1r_rust_2, _3       the rest points of the sneak through De Vetemmer (the first one is the build's sluip_start): the east end
                       of the middle lane and the west end of the last lane; a caught player is put back on the last one
                       he reached, facing down the corridor
  d1r_trommel_speler   where you stand behind your drum on the stage (podium_trommel is the note block itself)
  d1r_publiek_1..12    seats on the stands of the Bellenkom (the crowd of the song scene: found on the stand tops)
  d1r_<scene>_camera   the cameras of the scenes (pads: eye point(s), then what it looks at)
"""

STRUCTUUR = "bikinibodem"


def _stoel(b, x, z, y0=16, y1=24):
    """The spot on top of the stand at column x, z (the highest solid block + 1), or None."""
    top = None
    for y in range(y0 - 1, y1):
        if b.naam(x, y, z) and b.solid_at((x, y, z)):
            top = y
    if top is None or top < 16 or b.naam(x, top + 1, z) or b.naam(x, top + 2, z):
        return None
    return (x, top + 1, z)


def bikinibodem(b):
    # --- steps 7 and 9: in front of De Krokante Knabbel ---
    b.prop("d1r_kk_deur", 138, 16, 108, wie="sp_d1_recept: op straat voor De Krokante Knabbel (terug-plek van stap 7 en 9)")
    # --- step 8: the sneak ---
    b.prop("d1r_rust_2", 149, 16, 142, wie="sp_d1_recept: rustpunt 2 van de sluiproute (oosteinde van de middelste gang)")
    b.prop("d1r_rust_3", 140, 16, 146, wie="sp_d1_recept: rustpunt 3 van de sluiproute (westeinde van de laatste gang)")
    # --- step 10: the drum ---
    b.prop("d1r_trommel_speler", 110, 18, 44, wie="sp_d1_recept: hier sta je achter je trommel")
    # the crowd of the song: seats on the south and west stands (the first free stand top of each column)
    nr = 0
    for (x, z) in ((96, 61), (100, 62), (112, 62), (116, 61), (88, 57), (86, 53), (88, 49), (99, 63), (113, 63), (87, 55), (87, 51), (118, 60)):
        plek = _stoel(b, x, z)
        if plek:
            nr += 1
            b.prop(f"d1r_publiek_{nr}", *plek, wie="sp_d1_recept: een plek op de tribune (het publiek van het lied)")
    # --- the cameras ---
    # Krabguh's office (plan_z, feest): from the south-west corner over the desk to the safe; and back from the safe side
    b.camera("d1r_kantoor", (144, 18, 89), (149, 17, 85), zicht=["kluis", "krabguh"])
    b.camera("d1r_kantoor_2", (150, 18, 88), (145, 17, 86), zicht=["krabguh"])
    # Plankmika's lab: low, at the size of a very small Mika, on the cabinet of Plan Z and Karen
    b.camera("d1r_lab", (134, 17, 141), (134, 16, 138), zicht=["plan_z_kast", "karen"])
    # the street at night: Plankmika from his bucket to the door of De Krokante Knabbel
    b.camera("d1r_straat", (143, 19, 113), (139, 16, 112), zicht=["vetemmer_ingang", "d1r_kk_deur"])
    # the door of De Vetemmer: Sponsguh explains the sneak (sluip_uitleg)
    b.camera("d1r_vetemmer_deur", (143, 18, 113), (139, 17, 117), zicht=["vetemmer_ingang"])
    # the safe at the end of the sneak route (recept)
    b.camera("d1r_recept", (146, 18, 147), (150, 17, 146), zicht=["receptkluis"])
    # the Bellenkom (zoete_zege): wide from the field, Sponsguh, your drum, Octo, Octoliam on his stand, the crowd
    b.camera("d1r_zege_wijd", (106, 22, 60), (106, 19, 43), zicht=["podium_sponsguh", "podium_octo", "podium_trommel"])
    b.camera("d1r_zege_sponsguh", (101, 19, 49), (102, 19, 45), zicht=["podium_sponsguh"])
    b.camera("d1r_zege_trommel", (113, 19, 48), (110, 19, 44), zicht=["podium_trommel", "d1r_trommel_speler"])
    b.camera("d1r_zege_octo", (106, 19, 48), (106, 19, 43), zicht=["podium_octo"])
    b.camera("d1r_zege_octoliam", (120, 20, 51), (125, 19, 53), zicht=["octoliam"])
    b.camera("d1r_zege_publiek", (106, 21, 46), (106, 18, 58))


def schelpenstad(b):
    pass
