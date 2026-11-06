# NOTITIES_T_DROOM — zone 5, finale phase DROOM: de Grauwe Droom van Moeder Wolkwal

Branch `110-t-droom` (worktree `guhs_vadsrijk/t-droom`), from `110-t-z5wezens` + merge of `110-t-z5verhaal` (conflicts: FEATURES
and the two lang files, resolved as the union). Package `feature/vadsrijk/zone5/droom/` (+ `client/`), generator
`tools/features/vadsrijk_t_droom.py` (in FEATURES; standalone: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_droom.py`).
Cheat sheet: `SPIEK_T_DROOM.md`. Gametests: `Z5DroomGameTests` (`-Pgt=Z5DroomGameTests`).

## What is built
- **The hook:** `Z5DroomRegistry.aanmelden()` = `Z5Finale.registreer(Fase.DROOM, p -> Z5Droom.start(p, false))` at common setup.
  t-z5wezens' `Z5Bazen.start(p, "droom")` still answers false, but the registered start is asked first, so it never matters.
  The boss id `guhs:vr_z5_baas_grauwe_droom` (= `Z5Finale.BAAS_DROOM`, the one in the Verkenningsboek) contains "droom", so the
  story's own kill hook also counts it; the mechanic objects deliberately have NO "droom" in their ids (`vr_z5_baas_wiegnoot`,
  `vr_z5_baas_grauwe_rank`).
- **The place: de Droomwolk** (`Z5Droom.PLEK`, `w("z5d_droomwolk", 3070, 272, -520)`): a round cloud floor r 17 (white wool + light
  grey flecks, a puffy rim) in the open sky east of the tail fluke, built by `Z5Droom.bouw` ONLY into air, once per server start.
  You "sink into the whale's dream" (the story's own line: "Je zakt omlaag, de walvis in, haar droom in"): title card, teleport,
  slow falling, the boss intro 1.5 s later. Fall net: falling off during the fight puts you back on the south edge; after the
  fight, stepping off = waking up. Own kern area `z5d_droom` ("De Droom van Moeder Wolkwal", lv 60, not a town).
- **The boss `GrauweDroom`** (extends t-z5wezens' `Z5Baas` → zone 3's `Z3Baas` engine; a LOOSE boss: `losseStraal` 26, no kern
  Arena, not saved): lv 60, Water, ontvadst, HP 17000 + 560/lv (~50 600 solo at 60; +75 % per extra player via kern scaling).
  It swims slow rounds (r 9) over the cloud, cries softly, stops while stunned.
  - Grauwe golf (red strip 4 wide, 1.5 s: 25 % + Plakkerig), Slaapwolk (red circle under everyone: 22 %), Staartzwiep (light hit
    when you swim too close).
  - **Grauwe ranken** (every 25 s; 3, or 4 in fase 3): Z3Doel tendrils (HP 1.5 % of the boss, min 300), each grips the nearest
    fighter (4 % + Plakkerig every 2 s) and makes the dream 15 % tougher (min x0.55). Cut them.
  - **Fase 2 (65 %) "De droom draait om"**: the cloud floor turns night-grey/black (real block swap of OUR floor blocks only:
    the inverted colours), 4 drifting pink dream clouds (Telegraaf ROZE) are safe, the grey mist takes 7 % every 2 s elsewhere,
    20 s, again every ~60 s.
  - **Het achterstevoren slaapliedje** (signature, fase 2+, every ~45/35 s): 4 note stones (Z3Doel, colours pink/yellow/light
    blue/lilac, flute pitches) light up in the RIGHT order; then right-click them in that order within 6+2n s (anyone; wrong =
    start over, no harm). Right = getroost: VERDOOFD 8 s + x1.5 damage 10 s. Too late = Nachtmerrie: 30 % on all, heals 4 %.
    4 notes, 5 in fase 3.
  - **Fase 3 (30 %) "Samen neuriën"**: holds at 30 % (`vadsVloer`) until the hum meter (300) is full: +1 per tick per fighter in
    the yellow circle (r 4, middle; max 4 counted). Full = "Moeder Wolkwal neuriet mee", lilac texture, x1.25 damage for the rest.
  - Enrage: kern/Z3Baas default (7 min). The end: pink texture, hearts, the real lullaby (24 notes) on a flute, drifts up.
- **Rewards** (`DroomLoot`): chest = `z5wezens:baas` roll + 3 named Legendaries (Wiegeliedstaf/Magiër/Roze, Zwaard van de Zoete
  Droom/Ridder/Roze, Slaapsterrenboog/Jager/Water; 12 % each) + Moeder Wolkwals Slaapmuts (Guhddelijk, HOOFD, 3 %, server
  broadcast) + trophy "Een Fijne Droom (in een potje)". First time: a sure named Legendary + the trophy. Pet **Droomwalvisje**
  (kern G-PETS, first kill; the boss model in pink at 0.16 with t-z5wezens' shared pet animation).
- **The end flow:** every helper → `Z5Finale.droomOpgevrolijkt(p)`, a replay flag (`extra("z5droom")`), and a wake-up: when
  their chest is opened (or after 90 s; no chest = 90 s too, or step off the cloud) they wake at the middle blowhole
  (`Z5VerhaalPlekken.SPUITGAT` + 3 x, first free spot upward). The Wolkwaldroom itself has no exit, so we do not go back there.
- **Replay ("herinnering")**: after the story's dream, sneak 2 s on the middle blowhole → the dream again (loot/pet farming).
- **Title card + intro:** kern's boss intro (camera + "De Grauwe Droom / De nare droom van Moeder Wolkwal") + the z2/z3 tip
  cards (`vadsrijk.z2bazen.tip.grauwe_droom`, `vadsrijk.z3bazen.tip.grauwe_droom`) + our phase cards.
- **Music hook:** `GrauweDroom.muziek()` = `guhs:vr.z5droom.muziek.grauwe_droom` (no sound asset yet; t-geluid can fill it, or
  key on the entity type `guhs:vr_z5_baas_grauwe_droom`).
- **VFX:** only kern `Telegraaf` (red strip/circles, pink clouds, yellow hum circle, note-coloured circles) + vanilla particles; t-vfx
  decals can replace them at integration.
- **Models/textures** (generated): `geckolib/.../vadsrijk/zone5/droom/{grauwe_droom,wiegnoot,grauwe_rank}`, textures
  `textures/vadsrijk/zone5/droom/` (grauwe_droom + _lila + _roze + _gloed, wiegnoot_0..3 + _gloed, grauwe_rank + _gloed), item icons
  `textures/item/vadsrijk/zone5/droom/`.
- **Dev command** `/guhs vadsrijk droom start|verhaal|tp|wakker|bouw|fase <2|3>|wiegelied|ranken|omkeer|neurie|win|reset|loot [lv]|info`.

## Decisions made alone
- The dream is a SEPARATE place (sky cloud), not the real Wolkwaldroom: t-z5wezens' spy arena sits in that room, and an
  overlapping kern arena without a boss sends "no boss bar" to everyone inside every 5 ticks (bar/music flicker). The story text
  already says you sink into her dream, so a dream cloud fits. Placeholder spot east of the tail (sky, y 272); t-z5wereld can
  publish `z5d_droomwolk` to move it.
- Loose boss (no kern Arena): one shared fight for everyone on the cloud; a second party that arrives joins the running fight.
- Waking up at the middle blowhole (the room has no exit); chests stay on the cloud and you wake once yours is opened.
- No gore, no killing: tendrils are "cut" (pink puff), the boss is cheered up; Guh tone texts.
- Stat names in plain words (Wijsheid, Healkracht, Kracht, Vads, Behendigheid, Kritiek, Verdediging, Loopsnelheid).
- Phase thresholds 65 % / 30 %, numbers are first guesses (balance open).

## Shared-file changes (add-only, marked `// T-DROOM`)
| file | change |
|---|---|
| `VadsrijkFeature.register` | one line `zone5.droom.Z5DroomRegistry.register(modBus)` (after T-Z5WEZENS) |
| `client/VadsrijkClient.init` | one line `zone5.droom.client.Z5DroomClient.init(modBus)` |
| `tools/features/__init__.py` | `vadsrijk_t_droom` in FEATURES (+ the merge union of t-z5wezens + t-z5verhaal) |
| lang nl_nl / en_us | ~60 generated keys (`vadsrijk.z5droom.*`, entity/item names, loot names/flavour, pet, area, the two boss-tip keys) |
No kern edits. Reused read-only: t-z5wezens `Z5Baas`, `Z5WezensPlekken.wereld`, zone 3 `Z3Baas`/`Z3Doel`, t-z5verhaal `Z5Finale`,
`Z5VerhaalPlekken.SPUITGAT`, t-z5wezens' generator helpers (`schilder`, `_bewaar`) and pet animation.

## Open points
- Looks unseen in a client (the user judges in the dev client).
- Balance (HP, fractions, cooldowns, hum meter 300) first guesses.
- Real inverted-colour shader (now: the floor blocks swap to grey/black + grey dust) → t-vfx could add a screen tint.
- The Slaapmuts has no own worn look (plain harness look of kern, `harnasEigenschappen(HOOFD, null)`).
- The cloud is never removed (stays as a little cloud in the sky after the first dream).
- No AutoCheck client run.

## Resume point / tests
See the bottom (test results) and `git log -1`.
