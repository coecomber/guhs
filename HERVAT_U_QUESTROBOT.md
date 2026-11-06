# HERVAT U-QUESTROBOT (paused 2026-11-06, the PC was shut down)

Branch 110-u-questrobot (from 110-v-int 16206e964). For what is built, see NOTITIES_U_QUESTROBOT.md; for the test steps, see SPIEK_U_QUESTROBOT.md.

## Status
- The robot works: gametest `VadsrijkQuestRobotGameTests` (`-Pgt=VadsrijkQuestRobotGameTests`) plays all 131 quests of zones 1-5 through the real QuestMotor in about 2.5 s.
- The last full run (before the final lang fix and the NPC-text check) gave: zones 1-5 = 41/19/24/23/24 passed, 0 stuck. All zone flags are set and every airship gate opens.
- Dev command: `/guhs vadsrijk questrobot alles | zone <n>` (a FakePlayer).
- The report: `python tools/vadsrijk/questrobot/rapport.py run/questrobot/robot.json` writes QUESTROBOT_RAPPORT.md. The committed copy is from the run before the last fixes; regenerate it.

## Bugs found and fixed
1. z4_s11_schildpad: the drop area `z4_lampionkust` does not exist, so the quest was stuck. It now uses all zone-4 areas, and `NpcHaken.pastMob` accepts `a|b`. Changed: quests_z4.json + the generator.
2. Escorts without their own lines (z10_rammel, z3_s02, z5_s07) showed a missing text key. QuestMotor now uses a generic line.
3. `Z5VerhaalPlekken.OPSTIJGPIER` was a placeholder at sea. It now points to zone 4's real pier.
4. The POI `z4_kringstroom` lay outside the zone-4 map, so z4_s16 was unreachable. It is now at 1960,63,-270.
5. 89 lang keys were lost in a partial lang regeneration in 110-u-specs (amb_* NPC names, lampionkust travel-map/airship texts, werkplaats/luchtschip_z4 role buttons). I restored them from t-int in nl_nl + en_us. Because of this, Z4VerhaalGameTests.elkeStapWieEnWaar failed on v-int.

## Next steps
1. Run the gametests: `-Pgt=VadsrijkQuestRobotGameTests,VadsrijkNpcGameTests,Z5VerhaalGameTests,Z4VerhaalGameTests,Z3NpcGameTests` (round 4 was stopped by the pause; it had not been compiled yet after the last edits: the NPC-text check in QuestRobot.npcTeksten and the lang restore).
   - Run with `--no-configuration-cache`: the stale config cache from a copied .gradle once ran u-z6muziek's tree instead of this one.
2. Regenerate QUESTROBOT_RAPPORT.md with rapport.py, then make a final commit (without "WIP").
3. Zone 6: once 110-u-z6verhaal is committed, merge it (or wait for integration) and run the robot `alles`.
   - The static check of the u-z6verhaal worktree only flags z6_s05_afdingen (the `z6_puzzel:afdingen_*` meld steps have no emitter).
4. For the lead (also in the report): the zone-1 mount quests only open once you own a mount (zone-3 starter). There are 39 meld steps and 14 counters that the robot does not really play.
5. Note: I accidentally emptied `slots/` once (rmdir) and recreated it; this may have caused a short overshoot of the gradle slots.
