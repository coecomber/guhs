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

## Phase 2 (GO from the coordinator, 2026-11-09 evening)
- Merged 110-x-z1grond 36c27d2da (no conflicts).
- **Galmtop fix** (`zone2/bouw2.py`, X-INT `_naald_aankomst`): X-KAART's rotated start made the top treads round into cells
  under the plank disc and the railing gap missed the arrival. Now, just outside the disc (6.6 < d <= 8): tread level top-1,
  tread level top, a bottom slab at top+1, then a wide railing gap (top+2/top+3, +-0.45 rad). Independent of the start angle,
  so Meerkijk/Kloofkijk get the same arrival. loopcheck2 VAL 33 -> 30, 930,195,-1108 gone; bereik zone 2 63/63.
- **Zone-1 TRAP review:** classified all 303 TRAP/TRAP- by block and path blocks nearby (script in my scratchpad): 277 are
  roof tiles / plaster / tent cloth (roofs, eaves), 18 cherry + 2 dark-oak stairs are chairs inside houses. Real: the two stone
  swim stairs out of the bay at De Haven (`wereld/bouwsels.py`, z 456 and 528) ended 1 below the quay. The top tread now
  replaces the quay edge (`range(-1, 5)`). TRAP 215 -> 209. Not fixed: VAL 83,95,27 (1-cell nook in a mushroom stem by a
  spiral walk in Zwamzoet, way out only by breaking) and the lighthouse cellar (checker does not climb the hatch).
- **Full regen** zones 1-6 (`maak_kaart --audit --geen-cache`), then `--zone 1 --audit` after the quay fix. BereikDoelen dump
  (+ z6raid_heraut, z6raid_zaalwachter), bereikcheck --strikt exit 0. `vadsrijk_w_grot.py` + `--check`: all good
  (DungeonDeurData unchanged). The full suite also re-dumps bereik_doelen.json (other tests move NPCs): I kept the dedicated
  BereikDoelen dump.
- **Merged v1.9.7** (main's hotfixes 1.9.3-1.9.7; 110 was built on 1.9.2). Conflicts: `Guhrrit.java` (v1.9.3's keyed
  `verschijn(..., sleutel)` + 1.10's "never in the Vadsrijk" guard, both kept), `guhhuisje_villa.png` (v1.9.7's), FTB en_us
  (regenerated), lang (key-wise; the 16 renamed goods Konijnenpluis/Wolkenpluis/... from 1.9.6 win). English: new overlay
  `c426_oud_197.json` (36 safari names from v1.9.7's en_us + 14 storage/give-away/Terughekje texts) and the 17 renamed keys
  retranslated in place (c413, c424). engelscheck --alles 0/0/0, --merk --alles (50,289).
- **Dev commands** (ZonePoortRegistry, X-INT): `/guhs vadsrijk zones alsspeler aan|uit` (ZonePoort.TEST_STRENG: every player
  counts as a normal player, not saved) and `/guhs vadsrijk zones zone1klaar` (zone-1 main quests done + flag + lv 20).
- Full suite: 503 tests, 500 green; the 3 red pass alone (Z2Bazen 6/6, Ruiters 3/3, Z3Bazen 6/6). Quest robot 41/19/24/23/24/25,
  0 stuck; gate 6 main 13/12/15/7/10/10; gate 1 main 13/13 other 26/26.
- AutoCheck `tools/autocheck/vadsrijk_x_int.txt` (35 shots, no crash) + a re-aimed short round `vadsrijk_x_int_kort.txt`.
  The end-of-zone-1 shot shows the captain's "Het schip naar De Splinterpieken vaart binnenkort!".
- Site: 110-x-int merged into 110-u-site, runtime dump from x-int's run/, regen (renders, icons, map), full build 946 pages,
  zone-1 build 380 files 0 leaks 0 broken; u-site 482e0b159; gh-pages 477b071f2 (both previews 200, zone1/zone2.html 404).
- Docs: SPIEKBRIEF (new ch. 1 + 2, then round W as W1/W2, then B), TESTEN_RONDE_X.md, EINDRAPPORT_RONDE_X.md,
  RELEASE_CHECKLIST.md. run/ cleaned; run/defaultconfigs/guhs-vadsrijk-zones.toml = openZones 6 for the client.
