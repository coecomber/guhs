# NOTITIES_Z2_BAZEN — zone 2 "De Splinterpieken", slice "bazen" (Guhs 1.10, Het Vadsrijk)

Branch `110-z2-bazen`, worktree `guhs_vadsrijk/z2-bazen`. Package `feature/vadsrijk/zone2/bazen`, ids `vr_z2_baas_*`, lang
`vadsrijk.z2bazen.*`, generator `tools/features/vadsrijk_z2_bazen.py` (in FEATURES; uses the zone-1 bazen generator's helpers),
Blockbench copies in `blockbench/vadsrijk/z2/`. Registered by one line each in `VadsrijkFeature` and `client/VadsrijkClient`.
Dev command root: `/guhs vadsrijk z2bazen ...` (tp, bouw, fase, split, boor ontgrendel|reset|volgorde, burcht reset|los|oplossing,
loot, items, herstel). Kern's `/guhs vadsrijk baas reset z2bazen:galmwolf|z2bazen:spiegelzucht` works too.

## The fight engine (zone-1 playtest lesson: bosses stood still and could be shot from range)
`Z2Baas` (extends kern `VadsrijkBaas`, GeckoLib) does it all itself, so nothing waits for vanilla goals:
- **targets**: `strijders()` = every player in the arena/loose radius (also far away; creative only when nobody else is there, so
  the AutoCheck/dev client sees the moves); own target choice every 5 ticks, the boss chases with navigation and bites in reach;
- **rotation**: `kiesAanval` every time the global cooldown `rustT` runs out, each move with its own cooldown; players who stay
  9+ blocks away for 2.5 s are preferred by gap closers / lobs (`afstandsDoel`);
- **no cheese**: hits from players outside the fight do nothing ("Je staat buiten het gevecht"), a boss pulled out jumps back
  (kinematic `zSpring`), an empty arena for 30 s resets it;
- **careless = fainted**: the mechanic hits are true damage as a fraction of the player's max Vads (howl 60 %, beam 45 %, steam
  45 %, mirror strike-back 20 %, blades 30 %, stack 60 %/n): two mistakes faint you;
- colours: red = damage, pink = safe, purple = mechanic, yellow = stack (`Z2Baas.ROOD/ROZE/PAARS/GEEL`, same values as the
  f-bazen kern constants).
- Zone-2 tone: no pink plof. Crystal ones shatter into grey shards (`Z2Baas.opPlof`), the Mika's flee in smoke.
- **Merge note**: the zone-1 fix agent (f-bazen) is putting a similar engine into kern `VadsrijkBaas` (`vechters()`, `springNaar`,
  `lob`, `raakRond`, `inGevecht`...). Ours has different names on purpose (`strijders`, `zSpring`, `zRond`, `zDoetMee`, `zLeeg`...)
  so the merge compiles; both then run (harmless: both pick the nearest/hitter, both leash). Integration may fold ours into kern's.

## The bosses
- **De Galmwolf** (`galmwolf/Galmwolf`, arena `z2bazen:galmwolf`, lv 24, respawn 20 min, HP 5200+200·lv). Kristalgalm (3 s charge,
  the whole arena rings purple): not behind a pillar = 60 % + Bevroren + knocked back; each pillar that shielded someone cracks
  (`IJspilaarBlok` BARST 1), the next howl shatters it. 3 % of his HP during the charge interrupts (stunned 3 s). Pounce (red circle,
  prefers the ranged player), ice breath (red strip), bites. Fase 2 (60 %): IJsroep (2 new pillars on broken spots, purple first)
  when < 3 stand; Sneeuwknabbelaar pups (`Z2Hulpje` "welp", max 6). Fase 3 (30 %): Sneeuwjacht = blizzard (synced flag, client
  white fog `Z2BazenClient`), faster howls. The pillars = wereld's 8 ice pillars (POI galmwolf_pilaar_*): converted to our block
  at the fight start (Herstel `z2_galm_pilaren`), original ice back 10 s after the defeat / on reset / at server start.
- **Vrouwe Spiegelzucht** (`spiegel/Spiegelzucht`, arena `z2bazen:spiegelzucht`, lv 33, respawn 5 min, HP 6000+260·lv).
  Spiegeling: 3/4/5 images (`Spiegelbeeld`, same model, no nameplate, no vanilla shadow on any of them) on a ring; images get a
  particle shadow away from the light, she never. Hit an image = it shatters + strikes back (20 %); 5 % on her while split = found.
  Light: fase 1 from the north (all images show their shadow); fase 2 (70 %) a rotating light orb, only images in its 55° cone show a
  shadow; fase 3 (35 %) two orbs, faster, and the glass dome breaks open ring by ring (red shard circles), restored after
  (Herstel `z2_spiegel_hal`, `z2_spiegel_zaal`). Spiegelstraal: she blinks to one of two spots, one of 4 crystal eyes charges (the path
  glows purple, live); fold the mirror on its line (right-click to flat) and the beam blinds her (stunned 5 s, 8 %, images break);
  anyone in the path takes 45 %; mirrors flip back to a random diagonal after each beam. Also shard throws (red), a blink to the
  ranged player, and from fase 2 Scherfregen (yellow stack circle, 60 % shared).
- **De Spiegelburcht** (`spiegel/Spiegelburcht`): Lichtkamer (1 beam, 3 turns) -> gate poort_2; Spiegelgang = the gauntlet (shard
  blades as red strips + 3 waves; z2-wezens ids `vr_mob_splinterling`/`vr_z2_splinterling`/`vr_mob_ijzelgeest` if they exist, else
  kern's proefklodder lv 31) -> poort_3; Prismazaal (2 beams, 2 locks at once) -> poort_4 = the hall. Blocks: `SpiegelBlok`
  (stand 0 "/", 1 "\", 2 flat), `KristalBlokken.Lichtbron` (facing), `.Slot` (LIT), `vr_z2_baas_kristalpoort`. All placed only in
  air / our own blocks (Herstel `z2_burcht_*`); everything resets after 60 s with nobody in the burcht. Beams: `Lichtstraal` (grid,
  one layer = stand y 176).
- **De Mika-boormeester** (`boor/Boormeester`, loose boss in the Splinterkloof cave, lv 32, HP 5600+240·lv). Asleep until
  **`Boormeester.ontgrendel(ServerPlayer)`** (npc slice's secret trigger; remembers the unlock per player) or a right-click when
  **`Boormeester.MAG_STARTEN`** (Predicate, default: unlocked before) allows. 3 `Boorkop` entities ride on the machine (rood / geel /
  blauw); hits heat them (4 % of his HP per head); right order (random per cycle) = 10 % each, all 3 = kernsmelting (8 s stunned,
  double damage); wrong order = steam (red ring 6, 45 %) + all heads cool. Clue: the control panel every 15 s lights the heads in
  order, each with its tone (rood low, geel mid, blauw high) + tips. Armour: players do 25 % to the machine. Attacks: Boorstoot (red
  strip, ram; prefers the ranged player), Steenregen (red circles), Mika-mijnwerkers (purple circles, `Z2Hulpje` on the mika model,
  max 5). Defeat: chests (kern), then a smoke puff and a Mika on a mine cart rolls off (no death) + cliffhanger chat + "WORDT
  VERVOLGD..." card; back asleep 10 min later.

## Loot (`Z2BazenLoot`, effects `Z2BazenEffecten`; everything from a boss chest is gebonden)
| item | boss | rarity / chance | effect |
|---|---|---|---|
| `vr_z2_baas_galmklauw` De Galmklauw (zwaard, ridder, Water) | Galmwolf | Leg. 8 % | every 5th hit echoes 40 % around the target |
| `vr_z2_baas_ijspelsmantel` IJspelsmantel (borst) | Galmwolf | Leg. 8 % | 15 % when hit: attacker Bevroren 2 s |
| `vr_z2_baas_spiegelstaf` Spiegelstaf van Zucht (staf, magier, Roze) | Spiegelzucht | Leg. 10 % | crits bounce 30 % to a 2nd enemy |
| `vr_z2_baas_schaduwloze` De Schaduwloze (kruisboog, jager, Natuur) | Spiegelzucht | Leg. 10 % | +25 % on a full-Vads enemy |
| `vr_z2_baas_spiegelkroon` Spiegelkroontje van Zucht (hoofd) | Spiegelzucht | **Guhddelijk** 2 % | 10 %: a hit bounces back half, you take 0 (20 s cd) |
| `vr_z2_baas_boorhamer` Oververhitte Boorhamer (hamer, ridder, Vuur) | Boormeester | Leg. 15 % | every 3rd hit Brandend 3 s |
| `vr_z2_baas_stoomlaarzen` Stoomlaarzen (voeten) | Boormeester | Leg. 15 % | < 30 % Vads: steam push + Snelpootjes (30 s cd) |
| `vr_z2_baas_boorbril` Bril van de Boormeester (hoofd) | Boormeester | **Guhddelijk** 3 % | 12 % on hit: 4 s Vahoeg (20 s cd) |
First kill per player (`extra("z2bazen").eerste_<baas>`): a sure named Legendary + the trophy (else 25 %). Trophies
`vr_z2_baas_trofee_galmwolf|spiegelzucht|boormeester` (NOT gebonden, deco for economie). Random gear: `economie:z2_baas_<baas>` ->
`economie:baas_<baas>` -> `economie:zone2` -> `economie:zone1` -> `kern:klodderreus`. Items roll at the boss level (Galmwolf 24,
the other two 32/33 = the lv 30-35 band).

## For other slices / integration
- **npc**: `Boormeester.ontgrendel(ServerPlayer)` (returns false when he fled and isn't back yet), `Boormeester.MAG_STARTEN`.
  The heart shard / flashback after Spiegelzucht: listen to kern `Haken.VERSLAGEN` (mob instanceof Spiegelzucht); POI
  `burcht_splinter` is the pedestal.
- **systemen**: "Bevroren" status: `Z2Baas.bevroren()` returns `Status.valueOf("BEVROREN")` when kern has it, else PLAKKERIG.
- **wezens**: the gauntlet tries `guhs:vr_mob_splinterling`, `guhs:vr_z2_splinterling`, `guhs:vr_mob_ijzelgeest` (first that is a
  VadsrijkMob). Pups/miners are our own small adds (`vr_z2_baas_welp`, `vr_z2_baas_mijnwerker`).
- **wereld**: coordinates are literals copied from POI_Z2.md v1 (ids in the comments of `Z2BazenPlekken`); switch to
  `Z2WereldPlekken.plek(id)` after the merge. Requests in `POI_VERZOEKEN_Z2.md` section z2-bazen.
- **wiki**: the interactive `vr_z2_baas_spiegel` block needs a blokuitleg.py entry (right-click turns it).
- **economie**: trophies as deco; per-boss tables `economie:z2_baas_<baas>` if wanted.
- Kern `Telegraaf` has no purple/yellow in this base: we pass our own colour ints (same as f-bazen's).

## Open choices for the user
- Balance numbers are first guesses (HP, 3 % interrupt, 4 % heat per head, 25 % armour, the fractions above).
- Spiegelstraal: grid beams can only hit her on a straight line, so she channels on one of two spots on the mirror rows.
- The Boormeester's drill heads ride on the machine (wereld's 3 iron pads in the cave stay deco).
- Two puzzles (Lichtkamer, Prismazaal) + the Spiegelgang gauntlet (wereld's 6 spiegelgang spots unused).
- Placeholder dev builds (`/guhs vadsrijk z2bazen bouw alles`) only fill air: on the real map they add nothing.

## Resume point (2026-11-04)
- Everything above is built and committed on `110-z2-bazen`.
- Gametests `runGameTestServer -Pgt=VadsrijkZ2Bazen`: **5/5 passed** (galmwolf: ranged target + rotation, pillar shields / cracks /
  shatters, careless howl 60 %, interrupt, no damage from outside, phases + blizzard, reset restores; spiegelzucht: ranged target,
  hall setup, images strike back / she takes damage / found at 5 %, light cone, beam misses through a diagonal mirror and blinds
  her through the folded one, fase 3; both puzzles solve with their solution and not with their start; boormeester: asleep,
  start, ranged target, 25 % armour, wrong order = steam, right order = meltdown + 3 x 10 %, double damage, defeat = cart flight +
  chest; loot). Each test has its own batch (bosses fight every player within 24, side-by-side tests disturbed each other).
- AutoCheck `tools/autocheck/vadsrijk_z2_bazen.txt` (player in creative at ~16 blocks in each arena): 23 screenshots in
  `run/screenshots/autocheck/` (not committed). All three bosses act from the start: Galmwolf pounces onto the ranged player,
  pillars, purple howl + pups + white blizzard in fase 3; Spiegelzucht's images with particle shadows, red shard circles at the
  player, she blinks to the ranged player; Boormeester with 3 heads + nameplates, Steenregen circles, rams the player, miners
  swarm. Burcht: the Lichtkamer beam + lock, gate opens after `burcht los`, Spiegelgang waves.
  (Dev-build note: in the first run the Galmwolf stand-in floor was missing when the player arrived although the build reported
  its blocks; the script now builds it again after the teleport. Irrelevant on the real map.)
