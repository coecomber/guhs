# NOTITIES_R_INT: zone-3 revamp integration (branch 110-r-int, worktree guhs_vadsrijk/r-int)

Base: f8cd85d3b (110-p-verhuis = m-bazen + n-extra (+ o-draak) + p-wereld up to 51ea05dee). Never gradle in k-int (the user
plays there). Gradle only via `gradle_slot.sh` with GUHS_SLOTS=3. build/moddev/artifacts copied from k-int (jars only, for
make_ftbquests). Scratch tools: scratchpad `kijk.py` (block slices around a point), `dicht.py` (loopcheck cells -> DICHT list),
`camcheck.py` (opening/flight cameras vs tiles).

## Merged
| branch | head | notes |
|---|---|---|
| 110-p-wereld | 05fa29a72 | clean (walkability pass: werf tower stairs, Pelszaal spiral, wide doors, piers) |
| 110-q-dialoog | 3eefb5e06 | clean (PraatScherm portraits, Praat portrait tag, QuestMotor portretBron, `/guhs vadsrijk npc portrettest`) |

## p-wereld leftovers
- **Piepvel height window**: already done by p-verhuis (`Z3BazenRegistry`: `arena_piepvel` `.met(r, 6, 14)`, Pelszaal floor 61,
  plein 81). The other arenas go through m-bazen's `Z3BazenPlekken.indeel` (dungeon windows 6/14, disjoint) at registration.
- **Zone borders**: already done by p-verhuis: `Z3NpcPlekken.zone3`, `Z3WezensPlekken.inZone`, `Z3Lagen.inZone3`,
  `Reiskaart.hier` all use `Z3WereldPlekken.inZone3`. `QuestStap` only knows zone 2 (z < -1700; zone 2 has not moved in this
  branch) and the camp island; the zone-3 airship hint is `Z3Haken.route` (a QuestStap ROUTE) with inZone3. Nothing to change.
- **Travel map lines B/C**: `ReiskaartScherm.ROUTES` now Kandijkom ↔ Schemerwortel (B) and Schemerwortel ↔ zone 4 (C); the old
  Zwamzoet ↔ Schemerwortel and Kandijkom ↔ zone 4 lines are gone. The map art/place positions are unchanged (old layout art).
  The captains' fast-travel lists (QuestMotor) are unchanged (the "luxury" travel map stays).
- **Opening/flight cameras**: already relative to the trunk / dock (p-verhuis). Checked against the final tiles (`camcheck.py`):
  all 13 keyframes are in open air (only the last close-up at the dock has 3 solid blocks around it = the deck). Not seen in a client.
- **"De Stamspiraal" -> "Het Rijpad"** (area name `z3_stamspiraal`, lang via `vadsrijk_z3_wereld.py`). FTB quests regenerated
  (`make_ftbquests.py`): the zone-3 FTB texts were still the pre-p-verhuis story (hq09 "De Stamspiraal" etc.); now the same as
  the quest source (hq09 "De binnentrap"). index.txt/art json only differed in line endings (restored).
- **The 2 POIs without a stand spot**:
  - `hangnest_woning_3` (Raatweide house 3): the skep house sits inside the crown-1 leaf mass and was FULL of leaves.
    `bouw3.dorp_vrij`: clears leaves/vines inside every village house (all 9).
  - `kuiken_6`: buried in the crown-1 leaves. Cause: `poicheck3.door()` matched "water" in every `waterlogged=false` state, so
    leaves/fences/full slabs counted as passable and the snapper never moved such spots. Fixed (match on the block name); the
    snapper now looks UP first when the spot is buried in terrain (leaves, moss, grass, dirt, terracotta), and a fence/wall top is
    not a floor any more. Moved spots (vs p-verhuis): kuiken_6, kuiken_1, grauwe_vlek_1/2, arena_kruinroofvogel_deur (+1/+2 up out
    of leaves/moss), lift_kapel_boven (off a fence top).
- **Trap pockets**: `bouw3.DICHT` fills the small above-ground VAL pockets with their floor block (leaf hollow by the Kruinlifttak
  house platform 34 cells, the gap by the Oosttaksteiger platform, the fence/hay-bale ring on a branch deck, a 5-deep hanging-moss
  shaft, Mosvoet ground dips, single 2-3-up holes; 71 cells). The list is made from loopcheck's new `cellen` field (loopcheck.py
  writes the cells of a VAL region <= 64 cells into the json): rerun loopcheck3 + `dicht.py` after a generator change that moves them.

## Checks (final tiles, after `maak_kaart.py --zone alle --geen-cache --audit`)
- **poicheck3**: 423/424 standable (only `oosttaksteiger_luchtschip`, the moored airship prop; known).
- **loopcheck3**: no POI finding (every POI reachable from the respawn and returnable). VAL 49, ALL under the island
  undersides (y 19-47: cone undersides, hanging roots; you only get there by falling off the edge). No VAL above ground left
  (p-wereld had 63 incl. ~15 above ground). PAD 396 (dirt path with a block above; cosmetic), LEEG 796 (island edges, expected).
- **poicode3**: OK (443 ids, 198 uses). `schorshuis.16` is gone vs p-verhuis's copy (p-wereld's final map has 15 wall houses);
  not used in code.
- **audit** (zone 1 grid, it now also reads the zone-3 tiles in its box): kuil 6, poi 19, trap 14, boom 31, gras 66, stap 5,
  zee 1, water 5. boom 31 vs k-int's 8: the extra 23 are zone-3 spots (mostly the werf island deck at ~1610,166,-820: stacked
  stone-brick/plank decks the audit reads as "a tree on a path"; cosmetic, same as loopcheck's PAD).
- Tile sizes: zone 1 35.9 MB, zone 2 26.8 MB, zone 3 12.0 MB (154 tiles).

## Gametests
- `-Pgt=Vadsrijk,Z2Npc,Z3Npc,StadsMuziek` (123 tests, one run): **119/123**. Failed:
  - `gebiedNaarDeuntje` (StadsMuziek): asserted the zone-3 dock is Honingtak; the dock is now the Weststeiger on a branch tip
    OUTSIDE the ring town (r ~203 vs the town cylinder r 160). Test now checks the Bloesemplein. Fixed.
  - `grotSpawnerVindtDeVloer`, `spawnerCapsEnVeiligeStraal` (VadsrijkWezens): order-dependent flakes (known since j-int);
    rerun -> passed.
  - `opperzwamFasen` (VadsrijkBazen, zone 1): boss timing flake (known since j-int). Failed again in the 12-test rerun,
    passed alone (`-Pgt=VadsrijkBazenGameTests` 5/5).
- Rerun `-Pgt=VadsrijkWezensGameTests,VadsrijkBazenGameTests,StadsMuziek`: 11/12 (only opperzwamFasen), then VadsrijkBazen alone 5/5.
- **Flaky**: opperzwamFasen (twice in a row in the big/medium run, fine alone), grotSpawnerVindtDeVloer, spawnerCapsEnVeiligeStraal.

## AutoCheck
- Script `tools/autocheck/vadsrijk_r_int.txt` = q-dialoog's portrait script + a revamp walk (opening, Weststeiger, Bloesemplein,
  rijpad start/midden, binnentrap floor + Honingtak landing, binnenhof, Raatweide house 3, kuiken_6 rim, kabelbrug, Piepvel cape,
  the Schorsstad floor above the Pelszaal, the resting pink dragon + critters, Vahoegje).
- **Run 1** (`vadsrijk_r_int.txt`, ~166 s, 22 shots, no crash): all revamp shots taken; `spawn piepvel` reports the Pelszaal arena
  at 1404 61 -576, r 16, window -6/+14; `vriendjes draak nest` works (critters gathered on the nest; the dragon himself was out of
  frame, the script sets no look direction); Raatweide house 3 is now an empty hay-bale room (no leaves). The live-NPC portrait
  (Sergeant Pluizebol) showed. BUT the 4 `portrettest` shots had no screen: AutoCheck closed the PraatScherm as a "foreign
  screen" (it comes from a server command). Script bug in q-dialoog's script: `gui keep` added before the command (both scripts).
- **Run 2** (only `vadsrijk_q_dialoog.txt`, short): **0 problems**; player portrait ("Jij", your skin), zone-1 stand-in, zone-3
  stand-in (Imker Zoemzang, guh with hat), narrator (book), live NPC: all correct. (Run 2 deleted run 1's shots: the harness clears
  the folder.) Log findings = the known EntityIconLoader noise + droombed/bliksemsmidse model warnings (pre-existing).
- The AutoCheck world was removed: run/saves is empty.

## Docs / run dir
- TESTEN_VADSRIJK.md: the zone-3 section rewritten for the new layout (layers, route hq01-15, Het Rijpad, binnentrap, wind/lift ids,
  bosses incl. the Pelszaal under the floor + cape + new dragon, the resting pink dragon + critters, pet nameplates, portraits).
- SPIEKBRIEF.md: Z1-Z7 rewritten, **Z8 Praatscherm-portretten** (q-dialoog) added. Both point at r-int.
- run/: config, defaultconfigs, mods, resourcepacks, options.txt, usernamecache/usercache from k-int; **saves empty**.

## Open
- Nothing seen by a person: the whole revamped map, rijpad/binnentrap feel, the new dragon (wings in a V at rest), the resting pink
  dragon + critters, the portraits.
- The dock (Weststeiger) has no Honingtak town music (it is outside the ring town); fine by design, say if it should.
- Reiskaart art still shows the old layout (places/lines drawn on the old image positions).
- Mika de Imker has no portrait resolver (q-dialoog open point: the book is shown).
- VAL 49 under the island undersides: only reachable by falling off the island (the world layout's cloud sea is below).
- `bouw3.DICHT` is a fixed cell list: if the kit moves the branch decks/houses it must be remade (loopcheck3 + dicht.py).

## Resume point
- Head: see `git log -1` on 110-r-int. Regenerate: `python tools/vadsrijk/maak_kaart.py --zone alle --geen-cache --audit --werkers 3`
  (~10 min), then `python tools/vadsrijk/zone3/poicheck3.py`, `loopcheck3.py`, `poicode3.py`.
