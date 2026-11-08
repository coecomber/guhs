# NOTITIES U-QUESTLOPEN (round U, agent U-QUESTLOPEN, branch 110-u-questlopen, from 110-v-int 3349c1d52)

Goal: an OP-only admin screen to click through the MAIN QUEST LINE of zones 1-6 in story order, for fast story playtests,
with every scene playing for real and bosses shown (not silently skipped).

## Merged
- **110-u-questrobot (71e7a772b)** merged in (its driver is reused, see below). Conflicts: lang json (union of keys, resolved with
  a script), VadsrijkFeature (Z6RaidRegistry was registered twice after the merge: fixed to v-int's order, commit "merge fixup").
  The robot's worktree had uncommitted changes (Z6Haken robot hooks); those are NOT in this branch: integration gets them from
  110-u-questrobot later.

## What is built (`feature/vadsrijk/questlopen/`)
- **`QlStappen`**: the chronological list, data-driven from `Quests.alle()` (main quests in questlog order = story order; the
  zone-6 quests come along automatically). Per quest: NEEM (take; not for auto quests), one DOEL per objective, LEVER (hand in);
  plus an OPENING pseudo step (the opening film in the Knuffelkamp). minLv = quest lv (LEVEL objective: its n), verwachtLv = the
  next main quest's lv. Optional quests behind toggles, slotted after their anchor (latest main quest in `na`; `@rijdier` = after
  the main step that melds `rijdier:starter`, i.e. z3_hq03_pluimstal):
  - Advancements: soort KLASSE + your class + actie_klaar `advancement:` (spec17_/z3_adv35_/z5_adv58_<klasse>).
  - Rijdier/huisdier: soort RIJDIER + quests whose actie_klaar contains `huisdier:` or starts with `rijdier`.
  Done-state is READ from the questlog (so it always matches the character); `zetStand(p, lijst, n)` rewrites the questlog so
  exactly steps < n count (Terug / Spring back / tests). Rewards, items and flags are never taken back.
- **`QlDoe`**: completes one step in 3 tiers. Tier 1 `echt` = what the player does (real `QuestMotor.klik` on the live NPC = the
  real dialogue opens; stand on the spot = `NpcHaken.robotPlekken` + `QuestRobot.PLEK_HAKEN`; `Haken.GEBIED_BETREDEN`;
  `Haken.KAMPVUUR`; training dummies; the opening = `Verhaal.opening`). Tier 2 `haak` = the hook the game calls (NpcApi.meld,
  Haken.VERSLAGEN with a mob of the right kind in the right area, escort arrival, hand-in = Questlog.voltooi + actie_klaar).
  Tier 3 `dwing` = the counter (Questlog.tel / neemAan / geef items / fill teller maps). **Driver reuse:** the objective-type ->
  hook mapping, `mob()`, `plekIn()`, `verslagen()`, `geheim()` and the stand-in NPC are COPIED from U-QUESTROBOT's `QuestRobot`
  (its methods are private and built around a whole run on a FakePlayer); the robot's public `PLEK_HAKEN` / `SPOEL` lists and
  `NpcHaken.robotPlekken/robotVind` + `QuestMotor.AANNEMEN_OPTIE` are used directly.
- **`QuestLoper`**: one session per player (memory; reset on a character switch, `Karakter.slot`). State machine ticked on
  ServerTickEvent: RUST -> (NAAR: teleport + 20 ticks for chunks/NPCs) -> WACHT (tier 1; wait until `rustig`: no talking screen
  `Praat.lopend`, no `RijkScenes.bezig`, no zone scene runner `QlHaken.SCENE`, 10 quiet ticks; not counted -> tier 2 -> tier 3 ->
  "vast") -> AANKOMST (level for the next step, teleport there, 20 quiet ticks so arrival triggers can play) -> RUST + screen opens.
  Boss steps: BAAS_IN (teleport between the arena door and centre: `Arenas` does the real intro + title card) -> VECHT (you
  fight, until the step counts) or VERSLA (after the intro: `krijgSchade` from the player in 15 % chunks, then the rest; the
  real plof -> beloon -> VERSLAGEN hook -> cheer-up scene + chest + story moment; immune after 14 hits / no boss after 10 s ->
  tier 2). SPOEL ("Spring naar" forward): one step per 2 quiet ticks, silent tiers 1-3 at once (robot style: click's pending
  action runs at once, `QuestRobot.SPOEL` flushes zones 3-5), films muted via `KernPayloads.VOOR_STUUR`, waits for the zones'
  server-side scene runners (max 60 s per step) so flags set at a scene's end are kept. HERHAAL: replays the Scene/Titel payloads
  recorded during the last step.
- **`QlPlek`**: where a step is (NPC live position / QuestPlek / NpcPlekken by type; boss = arena door) and a standable spot
  (floor + 2 free blocks, no fluid; NPCs 2 blocks away; fallback `Npcs.staanPlek`); teleport into the Vadsrijk facing the target.
- **`QlNet`** payloads `vr_questlopen_actie` (C2S, OP re-checked: `permissions().hasPermission(COMMANDS_GAMEMASTER)`) and
  `vr_questlopen_stand` (S2C: meta CompoundTag + list of Components, 4 per step).
- **Client** `QuestLopenScherm`: small draggable panel (no pause, no blur), current step box, the list (✔ / ▶ / ☠ / `+`), buttons:
  Volgende (Enter) or Zelf vechten + Laat hem verslaan, ◀ Terug, Spring naar (pick a row), Herhaal scène, Alleen teleporteren,
  Reset hoofdlijn (2 clicks), toggles Level min/verwacht, Advancements, Rijdier/huisdier. Warning with the character name first.
- **Commands** `/guhs vadsrijk questlopen` and `... questlopen verder` (OP 2). Menu: button "Questlopen (admin)" in the
  Instellingen tab when OP (closes the menu and runs the command).
- **Texts** `tools/features/vadsrijk_u_questlopen.py` (in FEATURES after vadsrijk_u_z6raid; lang written with `_direct()`, append only).

## Decisions made myself
- Steps = take / each objective / hand in (finest grain, so every location gets its own teleport). Taking a quest opens the REAL
  offer dialogue; the player clicks "yes" (if they close it, tier 2 takes it with actie_aanname + escort start).
- Boss buttons replace "Volgende" at the boss step and BOTH play the intro (so you never stand next to an active boss while the
  screen is open). "Volgende" on a boss step = "Laat hem verslaan".
- "Terug" reverts quest state only (no rewards/flags/items back); a step that counts by itself (a LEVEL objective) is skipped
  further back.
- "Reset hoofdlijn" = `Questlog.wisAlles` (whole npc slice: side quests, secrets, flags too) + forget `npc:opening`/`npc:bam`; level,
  items and other slices' state stay. A partial reset of only some flags would leave the story inconsistent.
- The level setting only goes UP (generic + current class; class quests also that class).
- No new keybind (taken list is long): the screen reopens by itself, plus the `verder` command.
- Optional toggles default OFF (pure main line).

## Shared-file changes (add-only, marked U-QUESTLOPEN)
- `VadsrijkFeature`: register + payloads lines. `client/VadsrijkClient`: `QuestLopenClient.init`.
- `kern/KernPayloads`: `public static BiPredicate VOOR_STUUR` checked at the top of `stuur` (mute + scene recorder).
- `kern/scene/RijkScenes`: `public static void vergeet(p)`.
- `npc/Verhaal`: `public static boolean bezig(p)` (zone 1's scene runner).
- `zone2/npc/Z2NpcRegistry`, `zone3/npc/Z3Haken`, `zone4/verhaal/Z4Haken`, `zone5/verhaal/Z5Haken` (+ Z5Finale), `zone6/verhaal/Z6Haken`:
  one line each `QlHaken.SCENE.add(ZxVerhaal::bezig)`.
- `menu/client/InstellingenPaneel`: the OP button.
- `tools/features/__init__.py`: one FEATURES line.

## Tests
- `VadsrijkQuestLopenGameTests` (`-Pgt=VadsrijkQuestLopen`): `stappenVolledigEnOpVolgorde` (every main quest of zones 1-6, all its
  steps consecutive and in order, zones never go back, `na` first, min levels; toggles add spec17 of your class after hq09 and the
  mount quests after the first mount), `levelZetten`, `springNaarMetVlaggen` (fast-forward to zone 2: all zone-1 main quests
  handed in through the hooks + `zone1_klaar`; then back to hq06).
- See the commit message / resume point below for the run result.

## Open points
- Not played in a real client (no client run in this slice): pacing of the reopen, the panel layout at big GUI scales, and the
  boss VERSLA on bosses with own immunity phases (falls back to the hook after 14 hits) need a look in the dev client.
- Zone 6 scenes: `QuestRobot.SPOEL` has no zone-6 flush in this branch (the robot's uncommitted change); "Spring naar" waits for
  the zone-6 scene runner instead (slower, still correct).
- Escorts are simulated like the robot (follow starts for real, the arrival is counted); the NPC is not walked there.
- The step list text "what" uses the quest's own objective text; NEEM/LEVER use generic texts.
