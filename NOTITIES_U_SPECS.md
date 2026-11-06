# NOTITIES_U_SPECS (round U, branch 110-u-specs, from eaee06d08)

The user's NEW job-advancement model (FEEDBACK_RONDE4 V2/V3): fewer advancements, further apart, a specialisation at the
first one, style-based specs (both deal good solo damage), support/tank as optional tree branches, free respec at the class NPC.

## What was built
- **Model** (`klassen/Advancement.java`): 3 steps instead of 4.
  - Step 1 at class lv **17**: the spec choice. Zone 1, before the Zwamtempel: quest `spec17_<klasse>` from the Grootmeester
    (offered at lv 16 after `hq09_druipgrotten`; hq10 = lv 16, the dungeon hq11 = lv 18).
  - Step 2 at class lv **35**: zone 3, quest `z3_adv35_<klasse>` from Ordemeester Wasja (Nestkapel), needs `z3_hq08_werf`
    (the first boss: Kaaszeppelinwerf, lv 37-40) and `z3_hq03_pluimstal` (the mount).
  - Step 3 at class lv **58**: end of Hoogvads, quest `z5_adv58_<klasse>` from Kapitein Veerkracht (Academie), needs the finale
    `z5_hq09_slaaplied` (lv 59 main quest; the cap is 60, so 58 = "~60" with a little room).
  - Story gates in code too: `Advancement.POORT` (2 = z3_hq08_werf done, 3 = z5_hq09_slaaplied done), so the zone-3 Wasja
    "advancement" option and the desk can't skip the story. `Advancement.POORTEN = false` in tests.
  - The step-1 hand-in opens the spec screen (`KlasseBalie.vraagSpec`, modus "spec"); the ceremony plays after the choice
    (`Specs.kies` -> `Advancement.voltooi`). Closing the screen keeps the choice open (`spec_open_<klasse>`): the K screen and
    the desk show "Kies je specialisatie!", a chat line on entering the dimension reminds you.
- **Specs** (`klassen/spec/Spec`): Schildridder / Reuzenkussen, Scherpschutter / Strikkenzetter, Sterrenwever / Zegenzanger
  (the user's proposal, nothing clashed). The chosen spec = a hidden tree node `<x>_spec_<id>` (row -1, cost 0): respec never
  removes it, the client sees it, and the spec branch simply requires it.
- **Effects per spec and step** (`SpecEffecten`, numbers in `SpecBalans`). Existing effects were re-homed (their call sites
  now ask `Specs.heeft(p, spec, step)`):
  | spec | step 1 | step 2 | step 3 |
  |---|---|---|---|
  | Schildridder | Dubbelklap (old r_adv1) + **Tegenstoot** (counter after a caught hit) | Kristalwal (old r_adv3) | **Bolwerkplof** (ult +30 %, stun) + Kussenklap +15 % |
  | Reuzenkussen | **Reuzenzwaai** (basics +30 % and sweep 50 % in 3 blocks; Kussenklap +60 %, 1 s slower) | **Kussenkoprol** (Stormsprong +60 %, 2 s faster, stun) | Nestvorm (old r_adv4) + basics +10 % |
  | Scherpschutter | Valkenblik (old j_adv2 curve + its crit stats) + **Verre schoten** (+30 % beyond 10 blocks) | **Opgeladen pijl** (Knuffelpijl +80 %) | Kruinvalk (old j_adv4) + basics +8 % |
  | Strikkenzetter | Saltosalvo (old j_adv1) + **Strikje** (trap on Rolsalto) + 8 % attack speed | IJsster (old j_adv3) | **Strikkenregen** (ult field) + 8 % attack speed |
  | Sterrenwever | element puddle (old m_adv1) + Vadsoverloop (old m_adv2 + its CDR/element stats) | Spiegelecho (old m_adv3) | Schemerwijze (old m_adv4) + element +8 % |
  | Zegenzanger | **Strijdlied** (you +15 %, party +8 %, song pulse 50 % every 2 s in a fight) | **Zangkring** (Vadsbron hurts foes) | **Slotakkoord** (Meteoorguh +30 %, party Vahoeg 8 s) |
  The old Paladijnsaura (r_adv2) became the Tank keystone `r_paladijnsaura`.
- **Tree** (`SpecBoom`, run LAST from `VadsrijkFeature.register`): 4 columns = spec A | trunk | spec B | side branch
  (Tank / Steun / Heel). Node ids unchanged (all effects keep working), only place/cost/gate/requirements. Rows keep their cost
  and level gate. Before lv 17 trunk + side = exactly 16 points. 20 new nodes (stat nodes so every branch is a path, and the
  keystones r_onbreekbaar, r_reuzenplof, r_paladijnsaura, j_strikkenweb, m_slotlied). The Pluisjager's Vuurregen/Kleefregen
  no longer exclude each other (they sit in different branches). K screen: column strips with headers, the other spec dimmed
  with a ×, hover = what the branch is.
- **Respec / switch** (`Specs`): the tree reset is always free (`Boom.RESPEC_MS = 0`). Switching spec: at a class NPC (desk
  session), at most once per 10 min, not within 10 s of a hit, not in a boss arena (`Arenas.op`). Only nodes that no longer
  fit are refunded (`Specs.snoei`). Class NPCs: Grootmeester (z1), z2_ordemeester, z3_wasja (role ordetempel); zone 4/5 got an
  extra dialogue option on `z4_duinwachter` and `z5_veerkracht` (`QuestMotor.EXTRA`, no role change).
- **Titles/ceremonies**: the ceremony scene is unchanged; the title card shows the spec title (`vadsrijk.klassen.titel.<spec>.<n>`),
  plus a ring in the spec colour. Old titles reused where they fit (Kristalkampioen, Vahoege Paladijn, Nestwachter,
  Sterrenschutter, Kruinvalk, Poolstervalk, Sterrenwever, Kristalorakel, Schemerwijze). Nest touch (Advancement4) = step 2,
  crystal touch (Advancement3) = step 3.
- **Gear tooltip** (`GearVergelijk` + `client/SpecClient`): in every Vadsrijk gear tooltip "Vergeleken met je <slot>:", per
  stat ▲ green / ▼ red, damage per hit for weapons, and a verdict (beter / slechter / ongeveer even goed). Weighted: weapon
  damage x3, the class main stat x2, others x1, relative to the bigger value (min. reference 8, Vads 60), threshold 3 %.
  Weapons only compare with a same-class weapon in the main hand; rings with the weaker of the two; empty slot = "nieuw vak".
- **Balance**: `SpecBalans.extra/factor` model per spec and step (same constants as the code). `dpsTabelKlopt` checks the
  two specs per class within 10 % at steps 1-3, every step adds something, each spec >= +5 %. Current ratios: ridder 1.01 /
  1.00 / 1.01, jager 0.96 / 0.96 / 0.97, magier 1.02 / 0.97 / 1.01.

## Migration (old saves, `Specs.migreer`, once, on entering the dimension; `spec_v` = 2)
- Old steps (adv1 lv 10, adv2 lv 20, adv3 lv 30, adv4 lv 50) -> new: old 2+ = new 1, old 3+ = new 2 (old 4 = new 2; nobody
  had step 3 yet). Old step 1 only: new step 1 when the class is lv 17+, else nothing (the spec17 quest comes at 17).
- New step >= 1 without a spec: the spec choice is open (free, anywhere). Steps 2/3 wait for the spec.
- The tree was re-laid: every bought node is refunded once (all points back). Chat message `spec.migratie`.
- Quests: the old adv10_/adv20_/z2_adv30_/z3_adv50_ quests are gone from the quest files (an active one simply disappears
  from the log; Questlog only lists defined quests). The new quests of the steps a save already has are marked done
  (`SpecRegistry.questsAlGedaan`), so nothing is offered twice.

## Shared-file changes (all marked U-SPECS)
- `VadsrijkFeature.register`: last line `SpecRegistry.register`. `VadsrijkClient.init`: `SpecClient.init` (wraps
  `KernPayloads.sieradenOntvanger`).
- klassen (own slice): Advancement (model), Boom (respec free), Bomen (4 columns, `nietUitgesloten`), KlasseBalie (spec
  actions + state), BoomScherm (columns, spec button, badges), KlassenClient (spec screen), SkillInfo (adv entries removed),
  KlassenRegistry (old lv 10/20 message off; dev level args up to Levels.MAX), RidderMetBoom/Pluisjager/Vadsmagier/Basisaanval
  (node checks -> `Specs.heeft`).
- zone2/systemen/Advancement3 + zone3/systemen/Advancement4: checks -> `Specs.heeft`; their lv-30/50 chat messages off;
  Advancement4.STAP = 3, nest touch on step 2. Their gametests rewritten for the new model (advancementStap3/4, boomRijen
  counts, the ridder requirement chains).
- npc/VadsrijkNpcGameTests.klasseInTracker: quest id adv10_ -> spec17_.
- Generators: vadsrijk_npc.py (spec17 quests, Gids text, FTB list), vadsrijk_z2_npc.py (lv-30 quests removed),
  vadsrijk_z3_npc.py (z3_adv35), vadsrijk_t_z5verhaal.py (z5_adv58). New: tools/features/vadsrijk_u_specs.py (in FEATURES
  before vadsrijk_t_vfx) + tools/lang/en/c300_vr_u_specs.json. Regenerated with the standalone `_direct()` runs (lang json
  reordered by those runs; only the old quest texts were removed).
- Removed stale advancement json: vr_adv10_*, vr_adv20_*, vr_z2_adv30_*, vr_z3_adv50_*.

## Decisions I made myself
- Step 3 at class lv 58 (not 60): the finale quest is lv 59 and the cap is 60; 58 leaves a level of room.
- Step 2 gate = the werf quest (z3_hq08_werf, the first main-line boss) though its bosses are lv 38-40: the "first boss" in the
  story comes a bit after 35, so in practice step 2 lands around lv 37-38 (class lv 35 is the minimum).
- Reuzenkussen keeps the kussenzwaard (no new two-handed weapon model): the style is in the effects. A dedicated 2-hand pillow
  is an open point (see below).
- "In a fight" = hit or got hit in the last 10 s (own tracker, no kern change).
- The jager regen pair no longer excludes each other.

## Open points
- Not checked in a client yet (no AutoCheck run; the user tests from t-int). Look at the K screen layout (4 columns, 8 rows),
  the spec screen and the tooltips.
- A real two-handed "reuzenkussen" weapon model / animation; a parry *animation* for Tegenstoot.
- Zone 4 has no proper class NPC; the Duinwachter has an extra option. Maybe a real ordetempel NPC in Lampionhaven later.
- The wiki/site class pages still describe the old lv 10/20/30/50 model (wiki generators not touched).

## Resume
`runGameTestServer -Pgt=VadsrijkSpecs`, `-Pgt=VadsrijkKlassen`, `-Pgt=VadsrijkZ2Systemen`, `-Pgt=VadsrijkZ3Systemen`.
Dev: `/guhs vadsrijk spec ...` (see SPIEK_U_SPECS.md).
