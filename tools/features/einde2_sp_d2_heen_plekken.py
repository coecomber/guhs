"""
einde2 "Sponsguh" (CONTRACT_E2_C 4.1), slice sp_d2_heen: what this slice adds to the BUILDS of Bikinibodem and Schelpenstad
(markers, scene cameras b.camera(...), small props inside its own spots). Called by einde2_sp_plekken from the two build
modules right before their self-check, so every marker you add is checked with the build (reach, light, camera lines).
Keep the builds' boxes and their self-checks green; name every marker you add in your report. Yours alone.

No block is placed: only markers (template coordinates, walking level y 16). The cameras are the opening shots of this
slice's scenes; the Java scene scripts (spd2heen.D2Scenes) read their eye and look-at points, so the kit's line check
(eye -> look-at, eye -> every actor spot in zicht) guards what the player sees first.

Bikinibodem:
  d2h_knabbel            zone  the hall, kitchen and office of De Krokante Knabbel (walking in starts part 2: step 0)
  d2h_zaal               prop  the middle of the hall: the anchor of the scene "manager", the hint's target of step 0
  d2h_manager_camera     pad   the opening shot of "manager" (from the door side, over the tables, at Krabguh and the till)
  d2h_ijs_camera         pad   the opening shot of "ijssalon" (behind the counter, at the three stools)
  d2h_plein_camera       pad   the opening shot of "bevroren" (over the square, at the king's throne)
  d2h_mobiel_camera      pad   the opening shot of "rit" (in front of De Krokante Knabbel, at the Knabbelburgermobiel)
Schelpenstad:
  d2h_kroeg_camera       pad   the opening shot of "kroeg" (over the yard, at the parking bays and the swing gates)
  d2h_binnen_camera      pad   the opening shot of "baby" (inside the hull, from the bow end over the tables)
  d2h_sponsguh_deur      prop  where your Sponsguh waits inside the swing gates during the stand-still game
  d2h_patguh_deur        prop  where your Patguh waits next to him
"""


def bikinibodem(b):
    b.zone("d2h_knabbel", 128, 16, 83, 151, 21, 105, wie="De Krokante Knabbel van binnen: de zaal, de keuken en het kantoor")
    b.prop("d2h_zaal", 140, 16, 99, wie="het midden van de zaal van De Krokante Knabbel")
    b.camera("d2h_manager", (146, 19, 104), (140, 17, 95), zicht=["d2h_zaal"],
             wie="de camera van de scene 'manager'")
    b.camera("d2h_ijs", (78, 18, 86), (77, 17, 90), zicht=["ijs_speler"], wie="de camera van de scene 'ijssalon'")
    b.camera("d2h_plein", (100, 19, 118), (92, 18, 127), zicht=["neptuguh_troon", "bevroren_krabguh"],
             wie="de camera van de scene 'bevroren'")
    b.camera("d2h_mobiel", (145, 18, 117), (139, 17, 112), zicht=["mobiel_start"], wie="de camera van de scene 'rit'")


def schelpenstad(b):
    b.camera("d2h_kroeg", (47, 19, 46), (36, 17, 37), zicht=["kroeg_parkeer"], wie="de camera van de scene 'kroeg'")
    b.camera("d2h_binnen", (40, 19, 31), (30, 17, 26), zicht=["barman", "slapende_mika"], wie="de camera van de scene 'baby'")
    b.prop("d2h_sponsguh_deur", 33, 16, 33, wie="jouw Sponsguh wacht binnen bij de klapdeurtjes (het stilstaan-spel)")
    b.prop("d2h_patguh_deur", 34, 16, 33, wie="jouw Patguh wacht naast hem")
