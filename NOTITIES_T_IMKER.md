# T-IMKER: the secret zone-3 boss De Mika-imker ("Mika de Imker")

Branch `110-t-imker` (from abb8a027c, the final r-int with the zone-3 revamp). Cheat sheet: `SPIEK_T_IMKER.md`.

## What was built
- **The boss** `feature/vadsrijk/imker/MikaImker` (a `Z3Baas`, entity `guhs:vr_z3_imker_mika`, baasId `imker`, lv 47, Natuur,
  HP 7800 + 300/lv, a Mika: flees in smoke). Three phases (66 % / 33 %):
  1. *Kwaliteitscontrole*: grey honey lobs (red circle -> sticky puddle 20 s: PLAKKERIG + 3 %/s), cheese-smoke puff (red strip,
     20 % + DUIZELIG), an angry swarm chasing one player (10 % per sting; a smoke cloud puts it to sleep; no stings inside smoke).
  2. *De grauwe koningin*: he jumps onto one of 3 perches round the hive and pours (immune). Every 6 s the hive's stream fires
     along its opening (red lane, 22 % + PLAKKERIG). Players turn the hive (right-click, shift = back) so the lane points at his
     perch: the bees sting HIM (stun 5 s, x1.25 damage taken, -6 % HP, grey meter -35 %), then he climbs another perch. A full
     meter (30 s of pouring) = 35 % on everyone, meter back to 40 %. The hive texture greys in 3 steps.
  3. *De grote rookpot*: he chases again, 2 swarms, more lobs, and the big cheese-smoke bomb = `stapel` circle at the hive
     (50 % shared, 40 % on everyone if too few inside; solo needs 1). Default 7-min enrage.
- **The room objects**: Koninginnekorf `vr_z3_imker_korf` and 3 Rookpotjes `vr_z3_imker_rookpot` (both `Z3Doel`, not hittable,
  right-click only). Pots: smoke 8 s, cooldown 15 s (10 s solo).
- **Arena** `z3imker:mika_imker` (kern `Arenas`, r 10, -2/+10 high) on `mika_imker_raatzaal`; door = the hatch; respawn 10 min;
  the board hides inside the floor. The arena's maker only makes him when a player inside may fight (quest done, or quest
  running + hatch open + intro read) or a dev asked (`ImkerVerhaal.maak`).
- **Story** `ImkerVerhaal`: down the ladder with the hatch open -> intro scene (sleutel `t_imker`, 6 pages, the Mika caught
  pouring "rijpe kaas-honing", the Keizer's badge) -> on closing he wakes (title card MIKA DE IMKER). Defeat -> z3-bazen's
  moment `baas:imker` -> Z3Koppelingen.moment's default -> `NpcApi.meld(p, "z3_imker", "verslagen")` = the quest step; plus the
  follow-up scene once (`t_imker_na`: the notebook "Rapport aan de Keizer", the queen turns gold and sings, Zoempje). After the
  quest: re-fights without scenes ("Jij weer?!").
- **Portrait**: `Praat.portretBron` for `vadsrijk.z3imker.naam` and z3-npc's `vadsrijk.z3npc.imker.naam` -> the boss's own
  entity type (a stand-in of the GeckoLib model) instead of the book. Narrator pages keep the book.
- **Rewards**: pet **Zoempje** (`Huisdier("zoempje", "guhs:vr_z3_imker_mika", ...)`, first kill, own bee model + idle/hop/sprong),
  trophy item `vr_z3_imker_trofee` (first kill, then 25 %), z3 loot chest (`Z3BazenLoot.kist("imker")` -> zone3 table), first
  kill also 4 honey bottles + 8 honeycomb. XP x1.5 of a zone-3 boss.
- **Art + texts**: generator `tools/features/vadsrijk_t_imker.py` (FEATURES line added): the mika model in a beekeeper's
  suit (hat, mesh veil, badge, `roker` + `honingblik` bones; the renderer hides the smoker while pouring and the can otherwise),
  hive (+ korf_grauw1/2), smoke pot, Zoempje, trophy icon, 56 nl/en texts. Blockbench copies in blockbench/vadsrijk/z3/.
- **The room** (map generator, committed source only): `bouw3.py` raatzaal R 8 -> 11, 8 -> 10 high, floor-light ring, 4 wall
  hives, a spy desk (lectern + barrel), 5 lanterns; `plan3.py` moves `mika_imker_raatzaal` 6 blocks off the cell centre (away
  from the hatch) so the ladder comes down at the room's south edge (offset ~(-2, +8)). Checked with an in-memory generation of
  tiles (22,-9)/(22,-10): the pod hangs in air under the Raattros cell, walls closed, ladder reaches floor and hatch, nothing
  cut (only the cell's honey drips under it).
- **Dev commands** `/guhs vadsrijk imker spawn|klaarzetten|fase <2|3>|steek|zwerm|bom|intro|na|zoempje|waar|reset|tp`.
- **Gametests** `VadsrijkImkerGameTests` (`-Pgt=VadsrijkImker`): fase1RookEnZwerm, fase2KorfSteektHem, verhaalPoortEnPortret.
  Result: compileJava OK, `runGameTestServer -Pgt=VadsrijkImker` -> "All 4 required tests passed" (3 imker + 1 always-on).

## Decisions made alone
- Name: entity/title "Mika de Imker"; z3-npc's name key stays "De Mika-imker" (its file); both resolve to the same portrait.
- Arena = the existing raatzaal, enlarged (a 15-block hex was too small for 3 phases) rather than a new room elsewhere.
- Secret stays secret: no visible board (hidden in the floor), no spawn unless the story allows.
- The old lore scene keys `vadsrijk.z3npc.imker.scene.*` are left in z3-npc's lang (unused now); the new scenes have own keys.
- Swarms are particle clouds (server-side positions), not bee entities: guhs stay passive, vanilla bees would wander off.
- Re-fight cooldown 10 min; first-kill extras only once.

## Shared-file changes (all small, marked `T-IMKER`)
- `VadsrijkFeature.java`: +1 line `ImkerRegistry.register(modBus)` (after Z3BazenRegistry).
- `client/VadsrijkClient.java`: +1 line `ImkerClient.init(modBus)`.
- `zone3/npc/Z3Koppelingen.java`: `imkerTik` now only calls `ImkerVerhaal.tik(p)` (the old lore scene + its 14 s meld are gone),
  javadoc lines, 6 now-unused imports removed.
- `tools/features/__init__.py`: +1 FEATURES line `vadsrijk_t_imker`.
- `tools/vadsrijk/zone3/bouw3.py` (raatzaal block) + `plan3.py` (one `put`): see "The room".
- `lang/nl_nl.json` + `en_us.json`: +56 keys (from the generator).

## Integration TODO
- Regenerate zone 3 (`maak_kaart --zone 3` or at least tiles 22,-9 and 22,-10) so the bigger, shifted raatzaal and the new
  `Z3WereldPlekken.mika_imker_raatzaal` (~1459,145,-573) exist. Generated files were NOT committed. Until then the old smaller
  room at 1458,145,-567 is used (the fight works there, a bit cramped; the ladder stands in the middle).
- t-vfx's decal kit: the telegraphs go through `Telegraaf.cirkel/lijn` (and Z3Baas.inslag/stapel), nothing to do.

## Open / not done
- No AutoCheck/dev-client run (no visual check of the models, the veil cutout, the hive's turn direction vs. the red lane).
  If the hive's hole faces the wrong way: the model's front is -z (as Brokkel's cannon); turn `korf()`'s face in the generator.
- Balance untested by hand (HP, stream 22 %, grey pulse 35 %).
- The Verkenningsboek entry (Z3NpcRegistry `ZoneBoeken.baas("imker", ...)`) still carries z3-npc's comment "(a lore scene, no
  boss entity)"; it counts the quest, so it works.

## Resume point
Everything committed on `110-t-imker`. Next: integration merge + zone-3 regen, then the dev-client look (SPIEK).
