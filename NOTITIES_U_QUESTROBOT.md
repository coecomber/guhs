# NOTITIES U-QUESTROBOT (round U, agent U-QUESTROBOT, branch 110-u-questrobot, from 110-v-int 16206e964)

Goal: the user has not played the quests of zones 2-5 yet. A robot plays EVERY main and side quest of zones 1-6 through the real
quest engine and reports where something gets stuck; then the real bugs it found are fixed.

## What is built
- **`feature/vadsrijk/questrobot/QuestRobot`**: the runtime robot (`QuestRobot.speel(player, totZone)`). For every quest in questlog
  order (data-driven from `Quests.alle()`: zone 6 is played automatically as soon as `Quests` reads `quests_z6.json`):
  - wait until it is free (`na`); a level gate is met by setting the level (reported when above the cap 60; class quests set that
    class's level);
  - take it by clicking the giver (`QuestMotor.klik` on the live NPC guh, or a stand-in made from its `NpcDef`, never added to the
    world) and answering "yes" (`QuestMotor.kiesVoorTest(AANNEMEN_OPTIE)`); auto side/secret quests are started by their world
    trigger where known (geheim_spion: the first clue), else by the robot (noted);
  - each objective through the hook the game uses: PRAAT = click; GEBIED/GEHEIM poi = stand on the spot + the ticker's poi check
    (`NpcHaken.robotPlekken`); GEBIED area = `Haken.GEBIED_BETREDEN` (area must exist in `Gebieden`); KAMPVUUR = `Haken.KAMPVUUR` at
    the area's campfire (must lie in that area); OPVROLIJK/ELITE/BAAS/VERZAMEL-drops = `Haken.VERSLAGEN` with a mob of the right
    kind standing in the right area (a spot from tracker/campfire/arena/centre that `Gebieden.op` maps to that area); secrets =
    `NpcHaken.robotVind`; teller maps = filled (noted); ESCORTE = click (must start following) + the destination must be a
    `NpcPlekken.POI`, arrival simulated; LEVEL = set; POP = `Haken.BLOK_GERAAKT` then counted; KLASSE/MELD = `NpcApi.meld`;
  - hand in by clicking the turn-in NPC, then `QuestMotor.afronden` (actie_klaar runs: scenes, flags, the next zone's start);
  - every dialogue page that opens is checked (a `Praat.versierder`): text key present in en_us and nl_nl, not blank; the tracker
    step (`QuestStap.van`) must have its texts and a spot (except "anywhere" steps); an objective's `plek` must resolve;
  - when the main story of a zone is done: its flag (`zone1_klaar` .. `zone6_klaar`, + `z6_lijn_e_open` when zone 6 exists) and the
    airship line to the next zone (`QuestMotor.reisSlot` at the next zone's first main level) are checked;
  - a stuck quest is reported (step + why) and then pushed through by hand ("DOORGEDUWD"), so the chain behind it is still played.
- **`RobotRapport`**: writes `<game dir>/questrobot/robot.json` + `robot.md`.
- **Dev command** `/guhs vadsrijk questrobot alles | zone <n>` (op 2): runs with a FakePlayer "guhs_questrobot" in Het Vadsrijk (the
  live NPCs are used where loaded); the caller's own state is untouched.
- **Gametest** `VadsrijkQuestRobotGameTests.robotSpeeltAlles` (`-Pgt=VadsrijkQuestRobotGameTests`): the whole run headless, writes
  `run/questrobot/robot.json`.
- **`tools/vadsrijk/questrobot/statisch.py`**: static checks over every quests*.json (texts nl+en for titel/tekst/bezig/doel/aanbod/
  klaar/praat + the "where" keys, `na` links/order, speakers, level <= cap, actions registered in `QuestMotor.ACTIES`, a code
  source for every MELD key and teller map, item models + names).
- **`tools/vadsrijk/questrobot/rapport.py`**: merges the robot run + the static checks + a standability check of every target spot
  on the real map tiles (floor within reach, not boxed in) into **`QUESTROBOT_RAPPORT.md`** (Dutch summary + technical list per zone).

## Shared-file changes (add-only, marked `// U-QUESTROBOT`)
- `npc/NpcHaken.java`: `public static robotPlekken(p)` and `robotVind(p, sl, soort, id, pos)` (wrappers of the ticker's own code).
- `npc/QuestMotor.java`: `public static final int AANNEMEN_OPTIE`.
- `VadsrijkFeature.java`: one line registering `QuestRobotRegistry`.

## Session 2 (2026-11-08, after the PC restart)
- Compiled + gametests green on the old head (robot zones 1-5 = 41/19/24/23/24, 0 stuck). Report regenerated, commit 757dfab1a.
- **Merged** 110-v-int at 784de11e8 (z6verhaal + z6wezens + the zone-6 glue; v-int's WIP doc commit ab254bad1 was deliberately
  left out) and 110-u-z6raid. 110-u-tapijt was already in. Lang conflicts were resolved as a key union: v-int's values win (the zone-6
  retcon of z5_hq10/cliff); z6raid adds 290 keys. VadsrijkFeature: Z6RaidRegistry goes before SpecRegistry (which must stay LAST).
- First zone-6 run: 11/25 passed, 14 stuck, and lijn E did not open. Both turned out to be robot gaps, not game bugs:
  - zone-6 pois are only in QuestPlek.POI (Z6Haken.plekken counts them itself, in story order). The robot now also stands on
    QuestPlek-only pois, and Z6Haken registers the robot hooks: PLEK_HAKEN (vergeetBij + stap), SPOEL (spoel, uitgesteld, spoel),
    and LOS_OP (Z6Puzzels::losOp);
  - the great swim waits for a "quiet moment", but `RijkScenes.bezig` is game-time based and the robot runs in a single tick. The new
    `RijkScenes.vergeet(p)` (add-only, `// U-QUESTROBOT`) is called in the robot's spoel().
- New robot hook: **`QuestRobot.LOS_OP`**: a zone solves a dialogue puzzle that is open, with the right answers. The robot calls it
  after every spot and after a meld click. Zone 6's star beacons, exam, haggling and song are now solved for real.
- **z6_s05_afdingen ("never reported")**: NOT a code bug in the final u-z6verhaal commit. Z6Haken.klikVoor opens Z6Puzzels for
  z6_kardemom|pistache|lokum, and Z6Puzzels.kies melds `z6_puzzel:<id>`. The finding came from the static check of the then-uncommitted
  worktree, and from statisch.py not knowing the generic emitter. statisch.py now reads puzzels.json (an id counts when a bakens entry
  or the Java opens it). The robot plays all 3 haggling steps through the real puzzle path ("echt geteld via npc:z6_kardemom" etc.).
- The airship check now also counts z5_havenmeester as a captain, and zone 5 -> "zijdeland" is checked (open at lv 60+).
- rapport.py: `OP_HET_WATER` = {z4_kringstroom}: a boat spot. Water there is correct, so it is no longer a standability error.
- Final run: zones 1-6 = 41/19/24/23/24/25 passed, 0 stuck; 328 NPCs with 0 missing texts; every zone flag is set, z6_lijn_e_open is
  set, and every airship gate opens. Static: 0 findings. Standability: 0 errors.

### Shared-file changes (session 2)
- `kern/scene/RijkScenes.java`: `public static vergeet(ServerPlayer)` (U-QUESTROBOT).
- `zone6/verhaal/Z6Haken.java` (u-z6verhaal's file): 3 robot-hook registrations in registreer() (U-QUESTROBOT).

### For the integrator
- This branch contains v-int up to 784de11e8 + u-z6raid. Merging it into v-int: only the robot/rapport files, RijkScenes.vergeet, and
  the Z6Haken hook lines are new (plus the lang union, which equals v-int + z6raid's keys).
- z6_s05_afdingen needs no fix. Drop it from the open list.
- Open (small): z6_s15_oostroute's tracker points to vluchteling_1 for all 3 soup bowls.
