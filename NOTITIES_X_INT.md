# NOTITIES X-INT (round X integration, branch 110-x-int from b9ef85f4c)

## Phase 1 (merges + fixes)
- Worktree `guhs_vadsrijk/x-int`, run/ copied from w-int without saves/ and logs/.
- Merged `--no-ff`, in order: x-labels fb8f154db, x-engels 0e86eec57, x-dungeon 2e2aff572, x-zone1 3df5aec84, x-kaart c927d87af,
  x-raid 7a092b600.
  - Conflicts were only in `tools/lang/chunks.json`, `nl_nl.json` and `en_us.json` (every slice appended at the end). They were
    resolved with a 3-way JSON merge: chunks by chunk id, lang files key by key, so no key a slice removed comes back. No key was
    changed on both sides. en_us is rebuilt by apply_en anyway.
  - `zone6/bouw6.py` (x-kaart `pad()`/`_watertop` + x-raid `toren()`): git merged it cleanly. I checked that both changes are in.
    x-zone1's `X_ZONE1_XP` overrides W-BALANS's zone-4/6 xp, which is intended.
- **Lang chunk c416:** the English overlay loader (`lang.load_overlay`) reads EVERY `tools/lang/en/*.json`, so nothing was lost.
  But x-kaart's file was not registered in chunks.json and shared its number with w-party's `c416_vr_party`. It is now
  `c418_vr_x_kaart.json`, registered (per-key prefixes). x-raid's `c430_vr_x_raid` was not registered either: now it is.
- **x-dungeon's 6 new keys had no English** (they were Dutch in en_us): new overlay `c419_vr_x_dungeon.json` (registered);
  "Dungeon · %s · lv %s-%s" and "Dungeon" are allowlisted (the same in both languages).
- **Raid name texts in the DungeonBord style:** `Z6RaidHeraut` now makes each text the way `DungeonBord.maak` does: two text
  displays, the name big and bold in a colour (tower lilac, circle gold, exit lilac) over a small white line (scale 1.7 / 0.95,
  full bright, dark background 0x99201028, shadow, vertical billboard, view range x2). The door text's small line is
  "Raid voor 2-10 spelers · Loop naar binnen: De Wachtzaal"; the circle's is its one line; the exit is a name only.
  - One entity per line via U-DUBBEL (key `z6raidbord:<spot>:<line>`) and it waits for `Dubbel.klaar`.
  - It carries `DungeonBord.TAG`, so a copy from disk is refused (like a dungeon board). The slice's old 3-line texts
    (tag `guhs_z6raid_bord_<spot>`) are removed when seen.
  - `DungeonBord.maak`, `SCHAAL_*` and `HOOG_ONDER` became public (marked X-INT). No new lang keys.
- Generators: only the round-X text generators were run standalone, to confirm that the merged nl_nl is what they make:
  `vadsrijk_u_z6raid` (x-labels + x-raid), `vadsrijk_x_zone1`, `vadsrijk_u_huizen`. nl_nl did not change (consistent); the png
  byte drift was reverted. NOT run (per brief): `vadsrijk_kern.py`, `vadsrijk_economie.py` (stale). `vadsrijk_w_grot.py`
  depends on the map POIs, so it runs in phase 2 after the regen (with `--check`). The quest generators were changed only by
  x-zone1, which committed their outputs.
- `make_ftbquests.py` (with the 1.21.1 client jar copied to `build/moddev/artifacts/neoforge-21.1.251-client-extra-aka-minecraft-resources.jar`)
  + `apply_en.py`: the FTB English of "Een opknapper!" (x-engels c424) and its English header images are now in.
- `engelscheck.py --alles`: GEEN ENGELS 0, NL GEWIJZIGD 0, FORMAAT 0, VEROUDERD 0, EN_US OUD 0 (GELIJK 901 / NEDERLANDS? 369 are the
  old unreviewed ones, see NOTITIES_X_ENGELS). The default scope says OK. `--merk --alles`: 50,223 keys.
- `taalcheck.py`: 2,956 keys (incl. the DYNAMISCH table), 0 missing.
- compileJava OK.

## Stadsleven `budget` / `speeltuin` (the x-dungeon TODO)
- **Neither test depends on the clock time or the world time.** `begin()` pins `StadsLeven.testNacht`/`testRegen` (false), and
  `StadsLeven.nacht` returns that before it asks `Rooster.nacht` (world time via GuhTime). There is no real-clock call in
  stadsleven/ritme. The two failures had other causes, both now fixed in the TEST setup (no game behaviour changed):
  - **`speeltuin` "at night the playground sleeps"** (order-dependent): the children only go home when nobody hears it
    (`Babbels.iemandHoort`, 20 blocks). The gametest server never removes the mock players other tests made (`GuhMockPlayer`
    joins the player list and stays at its test's origin). Measured with a diagnostic run: 18 mock players, one 13 blocks from
    the playground, so "someone hears" and the children stayed up. Fix: `Babbels.testHoort` (null = real players; gametests
    only), set false for the night tick in `speeltuin` and reset in `einde()`.
  - **`budget` "exactly one playground"** (flaky even alone, failed "on tick 200"): `wacht()` waited at most 200 ticks for the
    forced test-town chunks to become entity-ticking, then ran the body anyway. The gametest server sprints (a batch of 200 ticks
    takes well under a second), while chunks become entity-ticking on the chunk workers in real time; so on a slow (first-batch)
    start a chunk was not ready, the playground corner's `Dubbel.klaar` said no, and no playground came. Fix: `wacht()` waits up
    to 1500 ticks (`WACHT_MAX` 300 x 5) and logs a warning if it still gives up; the 3 Stadsleven + 3 WLeven tests that use it
    got `timeoutTicks` 2000. The assert messages now include the counts (guhs/ents/players, children/heard/players).
- Shared files (marked X-INT): `stadsleven/Babbels.java` (testHoort), `StadslevenGameTests.java`, `WLevenGameTests.java` (timeouts).

## Tests (phase 1)
- compileJava OK.
- `runGameTestServer -Pgt=VadsrijkAlleLabelsGameTests,VadsrijkTaalGameTests,TaalGameTests,TaalsleutelsGameTests,VadsrijkDungeon,
  VadsrijkDungeonNep,VadsrijkGrotKerker,VadsrijkZonePoort,VadsrijkZwamgrotPoort,VadsrijkLuchtschip,VadsrijkSchepen,VadsrijkNpc,
  VadsrijkMenu,VadsrijkKern,VadsrijkBalans,VadsrijkParty,VadsrijkQuestLopen,VadsrijkZ6Raid,Stadsleven`: **90 tests, 89 passed**;
  the only failure was `speeltuin` (above). Log `guhs_vadsrijk/x-int-gt1.log`.
- After the fixes: `-Pgt=VadsrijkDungeon,VadsrijkDungeonNep,VadsrijkZ6Raid,VadsrijkParty,Stadsleven,WLeven` (mock players on
  the server before the Stadsleven batches): **30/30** (`x-int-gt3.log`); Stadsleven alone 3x in a row: 5/5 each.
- Not run yet (phase 2): map regen, map checkers, BereikDoelen, full suite, quest robot, AutoCheck, site, client.

## Open for phase 2
- Merge x-z1grond (branched from x-kaart).
- Full map regen zones 1-6 (zone 4 before zone 5): x-kaart needs 2, 5, 6; x-raid needs 6 (tower + raid POIs).
  Then `vadsrijk_w_grot.py` + `--check` (DungeonDeurData follows the POIs), BereikDoelen, and every checker.
- Raid boards and dungeon boards: not seen in a client yet (run-client at the end).
