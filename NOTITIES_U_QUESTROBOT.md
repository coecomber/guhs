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
