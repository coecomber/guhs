# HERVAT U-Z6AFRONDING (paused 2026-11-06 ~17:30, PC shutdown)

Branch `110-u-z6wereld`, worktree `guhs_vadsrijk/u-z6wereld`. Last commits: 84499b1d6 (Rozenplein stall) + the "WIP pauze:" commit.

## Done
- The uncommitted rozenkloof.py diff (a market stall moved off the stair line) is committed (84499b1d6).
- Full regen run 1 (`maak_kaart.py --zone 6`) and all checks run. Results before the fixes:
  - poicheck6: 11/334 not standable;
  - zweefcheck (copy of 110-u-deco with zone 6 added, `tools/vadsrijk/_zweefcheck_tmp.py`, UNTRACKED, do not commit): 1 floating flower at 4038 68 -719;
  - loopcheck6: VAL 21, TRAP 386, TRAP- 109, about 67 POIs unreachable;
  - audit6: kuil 20, trap 101, deur 33, poi 41;
  - verbindcheck: whole map los 6348 / fout 1234 (not specific to zone 6).
- Fixes (in the WIP commit, NOT yet verified with a full regen):
  - `poi6.py`: snapping skips palm crowns, trunks, wool, tents and awnings (oasis/Palmenkloven spots used to land on palm tops; karavaankamp landed on a tent); door cells are never a stand spot (z6_wacht_3); wider snap (r 16) when nothing is found within 6.
  - `poicheck6.py`: toren_top and raid_toren_binnen skipped (they are anchors).
  - `plan6.py`: karavaan_weg_8 at y 71 (it used to sit on the Zuidpoort roof); rozenkloof_zeemond x 3272 (3250 is outside the tiles). The same change is in `rozenkloof.py` zeemond().
  - `saffraanpoort.py`: palace terrace stair 1 higher (it ended in a 1-block jump: the cause of all the unreachable paleis_* spots and geheim_knuffel_7); sterrenwacht spiral +1 tread; karavanserai gallery stair 6 treads (kwastje_5).
  - `bouw6.py`: the Oostpoort roads (oaseweg + ruineweg) now go AROUND the rock mesa (x 3896..3956, z -930..-882) instead of through a stair tunnel up to y ~101; tower gate stair = real stairs all the way down to the torenpad (it was 6 full blocks ending ~10 above the ground); dungeon stair 6 treads y-1..y-6 (used to be a 1-block jump plus a 1.5 step = a trap); salt-cave entrance: the mound in front of the stair top is removed.
  - `zijwereld.py` `vul_kuilen()`: enclosed terrace basins in the land field (you fall in and can't get out) are filled flat to 1 below the lowest rim. Used to be about 7 VAL pits in the dunes/zijdeland plus the plateau dip of 605 cells above the Schatkamer.
  - The waterfall stair was checked: walkable both ways (OK, no change).
- Schatkamer rework (the lead's extra task), in `rozenkloof.py` schatkamer() + geheimen():
  - the facade sits 3 back (FD) in a rough rock frame with a shadow slot and weathering;
  - portico 6 deep with 6 free-standing ROUND columns (plus shape, 3 wide, base + torus + capital);
  - entablature: architrave, a frieze of carved guhs, a 2-step cornice;
  - the pediment projects, with a guh medallion, little guh acroteria and an urn;
  - attic band;
  - upper level: two projecting pavilions (columns, a deep niche with a statue, half pediments rising towards the centre = broken), a deep central recess with a round tholos (drum, 10 free columns, entablature ring, cone, urn);
  - a rock crown over it (where the plateau is lower, about y 105) so it stays recessed;
  - the hall moved back (hx1 = X1+12), passage X1+9..11; the hidden stair now runs at x = X1+7 (behind the pavilion niches) and arrives at the same tholos floor (y0+22).
  - Checked with snede.py (front elevation in the scratchpad): depth 0..9+ everywhere. ONE old _proef render (from before the crown fix) is in renders-zijdeland/ronde2/_proef_01d_schatkamer.png. rozenkloof_check.py ran green on the first version (before the crown change).

## Next steps (exact)
1. `cd tools/vadsrijk/zone6/kit && python maak_renders.py schatkamer_gevel snel` and look at it. Polish if needed (the user wants depth; the half-pediment direction and the tholos size are worth checking).
2. Rest of the canyon (lead): more relief on the canyon walls (bigger setbacks between bays, pilasters/cornice bands along the ledges, deeper reveals on the rock houses). Possibly in rozenkloof.facades()/petra gevel_*. NOT started yet.
3. `python rozenkloof_check.py` (kit) must stay green.
4. Final renders: first rename the old 01a/01c/01d (+ _namen) in renders-zijdeland/ronde2 to *_oud. Then `python maak_renders.py kloof_overzicht kloof_richel schatkamer_gevel` (final quality; also a street view: kloof_straat). Look at them.
5. `python tools/vadsrijk/maak_kaart.py --zone 6` (~10 min incl. verbind + overgang).
6. Checks: loopcheck6, poicheck6, audit6, audit/verbindcheck.py, `python tools/vadsrijk/_zweefcheck_tmp.py --zone 6`. Debug tools in the scratchpad (stap.sh A B = where the walk breaks; probe.py, hgrid.py, lijn.py) are NOT persistent; recreate them if needed. Remaining known items: many TRAP findings are decorative cornice stairs (roofs); check the walk flights with stap-style BFS.
7. Gametests: `JAVA_HOME="$APPDATA/PrismLauncher/java/java-runtime-delta" GUHS_SLOTS=3 bash ../gradle_slot.sh <worktree> runGameTestServer -Pgt=Z6WereldGameTests` (and Z6BlokkenGameTests).
8. Copy POI_Z6.md + Z6WereldPlekken.java to guhs_vadsrijk/ (the shared copies).
9. NOTITIES_U_Z6WERELD.md: add the check results + this round; finish SPIEK_U_Z6WERELD.md (Schatkamer, toren stair, Oostpoort roads).
10. Commit: `git checkout -- src/main/resources/data/guhs/structure/vadsrijk/tegels` + clean the untracked tiles (t_51.. etc.), keep Z6WereldPlekken.java, do not commit _zweefcheck_tmp.py.
