# NOTITIES X-RAID (round X, 2026-11-09): "is de grote toren de raid?"

Branch `110-x-raid` from b9ef85f4c. Test steps for the user: `SPIEK_X_RAID.md`.

## 1. What the user walked into
- **Yes, the big tower is the raid, but only its outside.** De Sterrendovende Toren on the map (u-z6wereld:
  `kit/landschap.sterrendovende_toren` + `bouw6.toren`, axis 4140,-688, voet floor y 84) is decoration.
- **The raid itself is the binnentoren** (u-z6raid): one structure template (`raid_binnentoren.nbt`, `binnentoren.py`),
  placed at runtime in a pocket UNDER the tower:
  - anchor 4140,16,-688, y 2..68;
  - De Wachtzaal = floor 0 (feet y 4), then floors 1-4 at y 16/28/40/52;
  - the start circle is at anchor (-6, -12, 0) = 4134,4,-688, the exit arch at 4120,4,-688.
- **How you were meant to get in:** walk into the door of the tower. `Z6Raid.poorten` teleports you from a box around the POI
  `raid_toren_poort` (4123,85,-689) to De Wachtzaal.
- **Why "er gebeurt niks"** (all measured on the round-W tiles, see `torencheck.py` below):
  - **The door box was 4 wide, the niche 9 wide.** The niche was "sealed" by black glass only 5 wide (x 4124, z -690..-686).
    You walked round it at the sides, through the open 3x5 door in the niche's back wall (x 4127), into the voet.
  - **The tower was hollow inside**: the voet had no roof, every section's top ring was open in the middle, so the inside was
    one tube from y 85 to the lens (~105,000 air cells connected to the door).
  - **A pillar in the doorway**: the voet is an octagon with a corner on the door axis. That corner stuck out 1 block in front
    of the gate facade (x 4121, z -689..-687, y 85..97), right in the middle of the way in. So you naturally went round it at
    the side, where the box was not.
  - **A gold ring stood edge-on in the walkway**: the "ring lock" (`LS.ring(kanteling=pi/2)`) was rotated along the walking
    direction. Gold blocks at foot and head height from x 4120 to 4126 on the axis.
- **"De torenlagen niet goed aangesloten":** correct, they literally floated (torencheck on the old world: 9 floating pieces):
  - sections 2, 3 and 4 of the shaft: each section's deck was a ring r-1..r+2.4, but the next section is 2.5-3 narrower, so
    its wall stood 1.5-2 blocks INSIDE the ring's inner edge, on air. 4559 / 3509 / 2191 blocks;
  - the four tilted gold rings, 4-10 blocks off the shaft;
  - the Lege Lens + its purple core: the spokes were 3 above the spire tops, and half of the spires were 4 shorter;
  - also an open slit into the tube at every joint.

## 2. What changed
### The tower outside (generator, `tools/vadsrijk/zone6/`, marked `[x-raid]`)
- `kit/landschap.sterrendovende_toren`:
  - the voet is solid inside, with a closed roof deck (the iwan carves the niche out of it);
  - every section's deck is a closed disc (the next section stands on it), and the gold corbel under the balcony steps out
    to r+1.4;
  - the four floating rings are now gold bands of 2 ON the wall halfway up each section (magenta jewels);
  - all 10 crown spires reach the lens ring; the lens sits on them (lens_y = deck + 11 instead of + 14), and a slim pedestal
    from the deck reaches the purple core;
  - the outer crenels no longer hang over the voet wall (d <= 18).
- `bouw6.toren`:
  - black glass seal and the edge-on ring removed;
  - the octagon corner in front of the gate facade is cut back (only wall blocks, x tx-20/tx-19);
  - the back-wall door is filled with sterrenvlies (purple glass, the same block as the raid's floor gates) inside a gold
    ring set flush in the wall;
  - a gold circle is inlaid in the niche floor;
  - `toren_poort` (= `raid_toren_buiten`, where you come out) is now the stair top 4119,85,-688, outside the door box.
- `plan6.py`: the plan values of toren_poort/toren_raid_ingang follow (the recorded spots win anyway).

### The raid door (Java, `zone6/raid/`)
- `Z6RaidPlekken.bijPoort`: the door box is the WHOLE niche: x poort-1 .. poort+3, 9 wide, y poort-1 .. +3.
- `Z6RaidPlekken.buiten()`: falls back to poort-4 (the stair top) when the POI lies inside the box. So it works on the old
  committed POIs too (old buiten 4122 lies in the new box), before integration regenerates.
- `Z6Raid.deur(sl, p)` (was inline in `poorten`, now testable):
  - places the binnentoren first when it is not there yet (never a teleport into bare rock);
  - the existing "just came out" list stays as a second guard;
  - `naar(sl, p, ...)` overload; `start()` teleports within the level the group stands in (same level in the game).
- `Z6RaidBouw.zetTemplate` split off `plaats` (gametests place the template at their own anchor without saving a version).
  The template itself is unchanged, so `VERSIE` stays 1.
- **New `Z6RaidHeraut`:**
  - **Heraut Sterrenroep** (NPC, role `z6raid_heraut`) on the stair top next to the door (poort + (-4, 0, 2)).
  - **Zaalwachter Pril** in De Wachtzaal next to the circle (anchor + (1, -12, 4)).
  - A click on either: 4 chat lines (2-10 players, walk in, 3 s on the gold circle, the Party tab, never alone, the exit),
    plus whether the tower is free or busy.
  - **Name texts** (TextDisplay, billboard, dark background, gold bold title + lines): over the door niche "De Sterrendovende
    Toren / Raid voor 2-10 spelers / Loop naar binnen: De Wachtzaal", over the start circle, over the exit arch.
    - Kept every 5 s while a player is within 48: one per spot, extras removed, respawned when gone. Tag `guhs_z6raid_bord_<id>`.
    - x-dungeon had not committed its name-text style yet, so this is a plain TextDisplay. **Integration: align with
      x-dungeon's style if it differs.**
  - **Tracker line** "De Sterrendovende Toren (raid)" (quest id `z6raid`, side colour):
    - shown after z6_hq10 (`Z6VerhaalApi.torenOpen`), until the first clear (`ringelingVriend`), while you are in zone 6
      and not in a raid group;
    - the beam points at the door; in De Wachtzaal it points at the circle.
- **Not solo-able:** unchanged. The circle needs 2 real players; nep-spelers never join (`partyInRij` and the circle only
  take real players). Only the dev command `z6raid start <n>` adds made-up members.
- **No Dungeon-door GUI.** The raid keeps its walk-in door + circle + queue. The party leader can already queue from the Party
  tab (`Z6Raid.partyInRij`). Reason: the raid's lock/queue/wachtzaal is its own system. Putting it behind `DungeonDeur`
  would mean a second queue next to `Z6Raid.RIJ`, which is a new feature, not a fix.

### Texts
- 26 new keys (`vadsrijk.z6raid.heraut.*`, `.bord.*`, `.volg.*`, `vadsrijk.npc.naam/plek/praat.z6raid_*`).
  - The generator has them: `tools/features/vadsrijk_u_z6raid.py`, block `X_RAID_TEKSTEN` / `X_RAID_EN`.
  - Written into nl_nl/en_us; English in the new overlay `tools/lang/en/c430_vr_x_raid.json`; `apply_en.py` run.
- No existing Dutch text changed (so engelscheck gets no "NL GEWIJZIGD").

## 3. Checks
- **New `tools/vadsrijk/zone6/raid/torencheck.py`** (block level, on `_cache/z6/wereld.npz` after maak6). It checks:
  - LEK: inside air connected to outside air, per section inside its own wall;
  - ZWEEF: 26-connected solid pieces not touching the ground;
  - NAAD: the lowest wall layer of every section on solid;
  - DEUR: niche open, walk in from the stair top free 3 wide, back wall closed.

| check | round W (old) | now |
|---|---|---|
| LEK | 50,059 air cells (y 86..221) | 0 |
| ZWEEF | 9 pieces (sections 2-4, 4 rings, lens + core) | 0 |
| NAAD | 7 wall cells on air | 0 |
| DEUR | 28 holes in the back wall, pillar on the axis | open niche, free way in, back wall closed |

- `loopcheck6.py` (whole zone 6) after the change: **0 VAL**. POI unreachable: only `toren_top` (the camera anchor above the
  lens, expected, as in round U). TRAP lines are the known decorative cornices (unchanged).
- Renders: see section 6.

## 4. Gametests (`VadsrijkZ6RaidGameTests`, `-Pgt=VadsrijkZ6Raid`)
New:
- `deurNaarWachtzaal`:
  - every niche cell (5 deep x 9 wide) is the door; the side of the niche takes you into De Wachtzaal;
  - the exit arch puts you outside the box; 5 more door checks while standing there do nothing (no loop);
  - walking back in works.
- `startCirkel`: one player on the circle never starts; a second real player for 3 s starts the raid for both (n = 2, no
  made-up members, both on floor 1); the tracker line points at the circle inside and the door outside.
- `wachtzaalBereikbaar`: the REAL template placed at a test anchor. From the door's arrival spot you can walk to the start
  circle, the exit arch and the hall keeper (no softlock).

Results (`runGameTestServer -Pgt=VadsrijkZ6Raid`, 10 tests):
- my 3 new tests pass, and so do schaal, verdiepingEnWipe, lootPerClear, zandloper and sterrenbeelden;
- **`spiegels` is FLAKY, and it already was** (not caused by this slice):
  - on the untouched base b9ef85f4c (my src/main/java checked out back to b9ef85f4c), the batches loot+spiegel failed 1 of 2
    runs with the same message ("two beams on a fake image: it breaks into a clone");
  - spiegel alone passes; spiegel with each of my batches passes.
- The new tests build far from the test grid (z + 4000..4600): the 60x67x51 template must not land in a neighbour test.
  They also restore the shared `Z6Raid.groep`/`RIJ` instead of `wisAlles()`.

## 5. Shared-file changes (marked X-RAID)
- `npc/NpcVolg.java`: `public static final List<Function<ServerPlayer, VolgRegel>> EXTRA` + the loop in `bouw` (add-only).
- `zone6/raid/*` is u-z6raid's own package (Z6Raid, Z6RaidPlekken, Z6RaidBouw, Z6RaidRegistry, tests; new Z6RaidHeraut).
- `tools/vadsrijk/zone6/kit/landschap.py`, `bouw6.py`, `plan6.py` (u-z6wereld's generator; zone 6 only).
- lang nl_nl/en_us (+26 keys), `tools/features/vadsrijk_u_z6raid.py`, new `tools/lang/en/c430_vr_x_raid.json`.

## 6. Open points / for integration
- **Generated outputs NOT committed** (per brief). Integration must regenerate zone 6 (`maak_kaart.py --zone 6`). That
  changes tiles t_64_-11/-12, t_65_-11/-12, `Z6WereldPlekken.java` and `POI_Z6.md`:
  - `raid_toren_buiten`/`toren_poort` 4119,85,-688;
  - `raid_toren_poort` 4123,85,-688;
  - `toren_top` 240.

  The Java works with both the old and the new POIs.
- `bereik_doelen.json` gets two new NPCs after the dump gametest:
  - `z6raid_heraut` is on the tiles (stair top);
  - `z6raid_zaalwachter` is in the runtime area of the binnentoren (listed, not checked).
- x-kaart touches zone-6 terrain grounding too. My generator changes are only inside `toren()` / `sterrendovende_toren()`,
  so a merge conflict there is unlikely.
- Not checked in a client (no client runs this round): the looks of the bands/lens, the TextDisplay readability, the NPC
  placement on the stair top. The user judges looks.
