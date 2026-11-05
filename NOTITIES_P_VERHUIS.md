# NOTITIES_P_VERHUIS: zone-3 content on the revamped map (branch 110-p-verhuis, from e5ab2935e = 110-k-int)

Merged: `110-m-bazen` (c4b476917), `110-n-extra` (09834cae6, incl. o-draak; lang conflict = union), `110-p-wereld` twice
(e7ce3bc2b = POI v2, 51ea05dee = zone 3 at the connected-world position, trunk (1404, -576)).

## Zone 3 can move (lead, 2026-11-05)
- No absolute zone-3 coordinates in zone-3 code any more. Every `w("<id>", x, y, z)` fallback is written in the **v2 frame**
  (trunk (2300, 0)) and shifted to the map's trunk (`Z3NpcPlekken.v2(...)`, `Z3WezensPlekken.v2(...)`, `Z3BazenPlekken.w`).
  Scene keyframes (opening, BAM, ride dragon, throne set, cliffhanger, airship) use `v2(...)` too.
- Trunk/zone box come from `Z3WereldPlekken.STAM_X/Z` and `ZONE_*` (Z3NpcPlekken.TX/TZ, Z3WezensPlekken, Z3BazenPlekken.STAM,
  Z3Lagen). One zone lookup: `Z3WereldPlekken.inZone3(x, z)` (Z3NpcPlekken.zone3(x, z) — was `x > 1500` —, Z3WezensPlekken.inZone,
  Z3Lagen.inZone3 all delegate). The werf box (`WERF_X0..Z1`) is read from the map's `z3_zeppelinwerf` cylinder.
- Tests (systemen layered areas, biome layers) are relative to the trunk.

## Checks
- `python tools/vadsrijk/zone3/poicode3.py`: every POI id used in code exists in Z3WereldPlekken (wa(own, anchor) only needs the
  anchor; loop ids as prefix) + every quest `poi:` exists in Z3NpcPlekken.POI. Result: OK (444 ids, 198 uses).

## Story changes (main quest, ids unchanged; "het is belangrijker dat alles klopt")
1. hq01: Honingtak = a ring town round the trunk; Raatje on the Bloesemplein (south side of the ring deck); dock at the end of the Imkerstraat (NW).
2. hq02: the Nestkapel is a honeycomb cell under the north of the deck (kapellift). Raatje sends you DOWN to the Pluimstal (on the ground, north,
   at the foot of the riding spiral) via the binnenhof lift + the north gate, and gives the Honinglift key (actie_klaar z3_honinglift; hq09 keeps it too).
3. hq03: guided first ride: Stalmeester Veertje (a moving Pluimguh figure) rides ahead of you up the OUTER riding spiral, 4 turns, past the
   Bloesemplein, through the leaf tunnel, to the end on kruin 1; then glide from the zweefplatform on the south edge of kruin 1.
4. hq04: grauwe vlekken on kruin 1 (SE, N) and kruin 2 (SW, via a wind column); the Mika zeppelinsteiger on the NE edge of kruin 1; Blad at the kruin-1 camp (east).
5. hq05: the nest is on TOP of the tree (above kruin 3): wind columns kruin 1 -> 2 -> 3, or the inner stair; the Kale Kruin = a dead mesa west on kruin 1.
6. hq06: the Raattrossen = the honeycomb cell under the east of the deck; the mouse "comes from below, from the dark".
7. hq07: "Hangnest Dauwdrup" became **Imkerdorp Zoemrust** on the Bijentak (SE; POI ids hangnest_* kept); title "Imkerdorp Zoemrust";
   Kaasschutter posts on the Westtak, Noordtak (east side) and Zuidwesttak; you ride off the spiral onto the branches.
8. hq08: the werf is its own island NE: over the cable bridge; Taleggio Dok 1 (ground), Brokkel Dok 2 (scaffold tower), Roquefort Dok 3 (mast tower, the cabin there).
9. hq09: "De Stamspiraal" -> **"De binnentrap"**: through the stair door on the Bloesemplein, down the inner stair; Knoest is a light in a wall house halfway.
10. hq10: the Lantaarntoren in the middle of the hollow trunk (rings lit from the stair: the trigger is the stair spot `schorsstad_ring_N`);
    **the Pelszaal is UNDER the Schorsstad floor** (stair down from the floor); Knoest sends you up with the binnenhof lift.
11. hq11: Mosvoet is the hamlet on the ground at the foot of the Honinglift (SE); the mould kettles at the farm in the WEST.
12. hq12: the Wortelpoort is EAST on the ground; the Wortelgang leads down to the roots hall (Hartslag-zaal); the 4 lanterns at the Hart gate in the hall's WEST wall.
13. hq13: the Hart = a ring of rooms round the roots hall; the egg lies in the root basket in the middle of the hall.
14. hq14: the escape: out of the Wortelgang, up the outer riding spiral, the wind columns up the crown storeys, to the nest (cutscene keyframes built from the spiral).
15. hq15: the cliffhanger looks east from the edge of the Bloesemplein (the Nestkapel is a cell without a view now).
- Side: s01 kuikens on kruin 1; s02 Opa Bast to the nest on top; s04 lights along the inner stair above Honingtak; s05 lookouts (De Kroon on kruin 2, Takpunt = end of the Westtak, the first spiral balcony, the Wortelgewelf ruin, the mast).

## Lore correction (user via lead): the Keizer's "long arms" are figurative
- F1 (`Z3Scenes.armFlash`, same timing + beat `piepvel_weg`): a cold grey mist of the Vadsloosheid rolls in through the knot hole in the
  Pelszaal wall, two Mika henchmen (mika model, matroos texture) step out, grab the Count and drag him back into the mist. No arm figurant.
  The npc fallback flash (Z3Verhaal.flashF1) shows only the mist.
- F2 throne room: no hand on the armrest; only the tapping from the shadow behind the throne + a grey haze.
- Zone-2 flashback (`WereldLucht` section 6): the finger line on the horizon is now a soft grey mist bank.
- Texts rephrased: z3bazen f1.floep/stilte/onder, f2.tik/onder (+ the z3npc scene copies), hq10 Knoest ("zijn grijpgrage macht reikt ver"),
  relief_8 + relief.alle, the "vingerafdruk" = a grey frost patch, z2 Poollicht Pien (a mist bank), z2_hq12 (a cat with a crown and creeping mist).
  The Schimmelkolos grab arm (a machine) stays, texts say "de grijparm van de Kolos". The keizerarm model generator stays but is unused.
  Titles "Vingers"/"de Pink" kept as nicknames.

## Other moves
- **npc:** all spots by v2 id; where texts for all NPCs/POIs/areas/campfires/bosses; new POIs z3_rijpad_start/eind, z3_honingdeur,
  z3_schorsstad_vloer, z3_pelstrap, z3_kabelbrug; z3_zweefplatform = zweefplatform_1. Life: `Z3Leven` (3 Pluimguh riders ride the spiral
  up and down near players + Veertje's guide ride), new NPCs Raatweide/Molenhoek imkers, 4 travellers (3 ground camps + kruin 1), 6 bridge guhs
  (BRUGGEN). Book page: + area z3_stamspiraal (lookouts/campfires come from the map lists).
- **wezens:** layers (crown 212+, branches 160-226, dark stair 55-232, ground 45-120, roots 0-72); paths per height (rijpad 76-240, branches
  165-232 with a smaller TAK_VRIJ 1.5 so mobs live on the narrow branch flanks, kabelbrug, wortelweg, wortelgang); branch spawners on the
  flanks of the 5 big branches; stair spawners in the dark trunk above Honingtak; leaders + 7 lairs anchored to v2 POIs (`wa`, own id wins
  if wereld adds `leider_*`/`hol_*`); safe spots: Honingtak ring cylinder, villages, Mosvoet, Pluimstal, werf island, each Hart room, hall
  basket, Pelszaal, Schorsstad town, stair landings, posts, campfires. Critters per new layer + bee swarms near BIJENKORVEN.
- **bazen:** werf box from the map, Hart = ring r 76..150 (the hall and the Wortelgang are not the dungeon checkpoint), the Pelszaal height
  window `.met(16, 6, 14)` (p-wereld's request), the F1 knot hole in the Pelszaal wall, the Kruinroofvogel chase flight bends round the tree
  (`Kruinroofvogel.omDeBoom`), HOOGVADS relative to the trunk.
- **systemen:** Z3Lagen box/trunk from the map; Z3Wind.KOLOMMEN from the map's WINDKOLOMMEN.
- **wereld code:** Z3Reizen airship flight comes in along the dock's outward line; opening cameras raised for the narrow tree.
- **evening lights:** `systemen/client/Z3Avondlicht` (lantern/lampion glow by block sampling, LICHTJES glow + fireflies, campfire sparks,
  bee-swarm motes, layer motes, the egg's pink pulse). Chimney smoke + hive particles are p-wereld's Z3Sfeer.

## Open
- "De Stamspiraal" is still the area name of the riding spiral (z3-wereld's lang); maybe "Het Rijpad".
- Nothing seen in a client: the riders' seat height (guh at +1.55 on the Pluimguh) and the mist look need a look in the dev client.

## Tests (2026-11-05)
- `runGameTestServer -Pgt=VadsrijkZ3,Z3Npc,VadsrijkVriendjes,VadsrijkBiomen`: 28 tests, 27 passed; the one failure (VadsrijkZ3Wezens
  spawnersPerLaag: no branch spawners, the Honingtak safe cylinder reached over the branches) is fixed: rerun `-Pgt=VadsrijkZ3Wezens` 6/6.
  Plan now: kruinzee 91, takkenbanen 27, schorsstad 51, wortelvelden 87, wortelduister 71 alive max (147 spawners).
- `poicode3.py`: OK. No AutoCheck (memory; the map tiles are p-wereld's and not regenerated here).
- Last merge of 110-p-wereld: 51ea05dee (nothing newer at the end).
