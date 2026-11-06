# NOTITIES_U_Z6WEZENS: zone 6 "Het Zijdeland" combat (lv 60-70) + the Bultguh

Branch `110-u-z6wezens` (worktree `guhs_vadsrijk/u-z6wezens`, from eaee06d08 + merge `110-u-dubbel`).
Plan: `guhs1100-avontuur/ZONE6_PLAN.md` (§5 creatures, §5.1 field bosses, §6 dungeon, §7 cave elite, §9 loot/mounts/pets/music;
the user decisions at the end: Bultguh 2 seats, no raid limits, cap 70).
Packages `feature/vadsrijk/zone6/wezens/` (+ `mob/`, `baas/`, `loot/`, `client/`) and `feature/vadsrijk/zone6/bultguh/`.
Ids `guhs:vr_z6_mob_*` (creatures + gear), `guhs:vr_z6_<wapen>`, `guhs:vr_z6_kristal_*`, `guhs:vr_z6_baas_*` (bosses, objects, named
items, trophies), `guhs:vr_eco_bultguh`, loot tables `z6wezens:*`, arenas `z6wezens:*`, lang `vadsrijk.z6wezens.*`.
Generator `tools/features/vadsrijk_u_z6wezens.py` (in FEATURES before `vadsrijk_t_vfx`; standalone:
`PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6wezens.py`). Cheat sheet: `SPIEK_U_Z6WEZENS.md` (Dutch).
Gametests: `VadsrijkZ6WezensGameTests` (`-Pgt=VadsrijkZ6Wezens`).

## What was built
### Creatures (`mob/`, base `Z6Wezen` on zone 1's RijkWezen)
Three endings (`Z6Wezen.Afloop`): OPVROLIJKEN (pink plof, bright `_blij` texture, happy hops; the grey star-salt crust flakes off),
MIKA (smoke + a grumpy line), VERSLAAN (constructs burst into glittering sand). 10 of 14 kinds are cheered up (sweet zone).
| kind | lv / area | mechanic (all telegraphed) |
|---|---|---|
| Zandvlooi (VERSLAAN) | 60 duinen | hides in the sand (no nameplate) until you are within 5, then leaps every ~3 s |
| Fennekguh | 60-61 duinen | hears you at 16 (aggro), alerts its pair; ears up + red strip = pounce |
| Springpiep | 61 duinen | red circle = sand kick (hit + 1.5 s darkness), then bounces away |
| Duinkruiper | 62 duinen | red cone = tail sting + Plakkerig |
| Dadelaapje | 62-63 palmenkloven | throws dates (red circle); a hit may pinch 1 % coins (max 60 brons), all given back on the cheer-up |
| Gierguh (flyer) | 63 palmenkloven | dives on the LOWEST player (red strip), then sits 2 s |
| Spiegelslang (VERSLAAN) | 64 zoutvlakte | 2 mirage copies (1 Vads, never hurt, burst on touch, 15 s); lunge strip |
| Zoutkreeft | 64-65 zoutvlakte | salt crust: front/sides x0.35, BEHIND x1.5 ("Zwakke plek!"); claw circle |
| Zandgeestje (VERSLAAN, flyer) | 65 zoutvlakte, NIGHT | purple circle = pulls in + dizzy; drops Sterrenstof; settles in the morning |
| Ruïnewachter | 66-67 ruinedal | statue (no nameplate) until within 6; big slam circle |
| Mika-Lampendover (MIKA) | 60-68 everywhere | red cone snuffer: darkness + removes Vahoeg/Snelpootjes |
| Mika-Ringdrager (MIKA) | 66-70 ruinedal/oase | purple circle r 9, then a drain beam 3 s (heals him 60 %); walk out to break it |
| Gedoofd sterretje (flyer) | 68-70 moederoase | 3 small red circles (star noise) |
| De Zoutslang (VERSLAAN, cave elite) | 66 Zingende Zoutgrot | always elite; dives into the rock (immune 2.5 s), pops out at the noisiest (sprinting / last hitter) player along a red strip |
- Leaders (spawner `z6wezens:leider_*`, elite, 5 min): Oude Oorschelp (fennekguh 63), Dadelbaas Pitje (65), Kapitein Korstje (67),
  De Oudste Wachter (69), Ringdrager Brie (70). 10 % Gouden Veer.
- Spawners (`Z6WezensSpawners`): 16 open spots (2 kinds each), the night spot (only Zandgeestjes, VERBODEN by day), the dungeon rooms
  (cave mode), the cave elite, the 5 leaders: ~70 alive max. No spawns in Rozenkloof, Saffraanpoort, the caravan camp, the tower foot.

### Field bosses (`baas/`, on zone 3's Z3Baas via `Z6Baas`)
- **Koningin Wormelien** (lv 63, by DAY; arena `z6wezens:wormelien` r 26, 20 min): dive (immune, a ripple follows someone; standing
  still = sand fountain; bursts up under the target), sand wave lane (Plakkerig); 60 % Duinspuug (cast bar, 3 pink shelters, the
  rest buried 35 %); 30 % the salt crust (Z3Doel `vr_z6_baas_zoutkorst` riding high on her back: immune until it breaks, then 6 s
  stun + 10 s x1.5). Cheered up: pink, sleeps in the sand.
- **De Zoutkolos** (lv 66, at NIGHT; `z6wezens:zoutkolos` r 22, 20 min): crack lines (4-6 red strips, cast bar); 60 % singing
  crystals (Z3Doel `zingkristal`, every 3 s a purple ring 1.2-4 + dizzy; break them); 30 % glowing heart: a hit from a player
  LOOKING at the heart (dot > 0.9, < 16 blocks) x2, with any item whose id contains `sterrenlens` x2.5. Construct: falls apart.
- Day/night gate in the arena factory; `Z6Bazen.forceerWormelien/forceerZoutkolos` (dev + story) spawn once at any time.

### The dungeon: De Verzonken Sterrenwacht (`Z6Bouw` + 3 bosses)
- Own rooms underground (floor y 30, x 3940..4061, z -983..-959): Ingangshal, gang, round Ringenzaal (r 10), gang, Kaartenzaal
  (star maps, bookshelves), gang, square Vuurschalenzaal (21 x 21), gang, the Koepel (r 12, lapis + glowstone star sky up to 14
  high). Double shell (no water/cave leaks), flat floors, 5-wide corridors, lights in every ceiling and wall band. Built ONCE and
  kept (marker block under the arrival spot); built on demand by the hatch, the story start, the dev tp, or when a player is found
  inside without rooms. Hatches: right-click near `z6w_sterrenwacht_ingang` = down; the ladders at the Ingangshal west wall and
  the Koepel east wall = up (to the surface by heightmap). Grey hints within 3 blocks.
- **De Astrolabiumwachter** (lv 66, construct): hits do 25 % unless the rings are ALIGNED (gold pillar + "NU!", 5 s every ~11 s);
  ring sweep = a red band per ring with 2 pink gaps (1.4 s, 30 %); a ring more every 25 % (max 4, bones `ring_1..4`); brass beam.
- **Kootje Glimmertje** (lv 67, Mika): 4 fire bowls (Z3Doel `vuurschaal`, stand 1/0, bone `vlam`); he snuffs one every ~15 s
  (purple strip); right-click a dark bowl twice to relight; 2 out = Darkness, 3 out = grey telegraphs, 4 out = immune; 50 % two out
  + 2 Lampendovers; snuffer slam (cast bar) + smoke bomb (blind).
- **De Verzonken Sterrenguh** (lv 68, victim): 4 telescopes (Z3Doel `telescoop`, stand 0-3 = N/E/S/W, bone `kijker` turned per
  stand); right-click = a quarter turn; all four aimed = heart lit, x2 for 10 s, then a star shock (red ring, safe close) and they
  are knocked askew; x0.35 otherwise. Star noise rain (more per phase), despair wave from phase 2. Cheered up: gives the
  Sterrenkompas (`Z6WezensApi.KOMPAS` + item `guhs:vr_z6_sterrenkompas`).
- Each boss has its own kern chest (`Z6BazenLoot.kist`).

### Loot (`loot/`, `baas/Z6BazenLoot`, `baas/Z6BazenEffecten`)
- Sets (`Z6Sets`; pieces count in the armour slots AND the gloves/cape/armband slots):
  - Duinwandelaar (6, Ridder/Jager): 2 Verdediging +14 & Vads +80 / 4 Schade tegen bazen +8 % / 6 Loopsnelheid +8 %.
  - Duinzijde (6, Magier): 2 Wijsheid +10 / 4 Heal-kracht +12 / 6 Elementschade +6 %.
  - Karavaanwacht (4, field bosses, Episch: wanten, cape, armband, laarzen): 2 Vads +120 / 4 Schade tegen bazen +10 %.
  - Sterrenwacht (6, dungeon, Episch): 2 Kritiek +4 % / 4 Cooldownreductie +6 % / 6 every 20 s the next hit +40 % ("Sterretje!").
- Jewellery: Saffraanring + Woestijnroosring (both rings: fit ring 1 and ring 2), Rozenketting, Sterrenoorbel, Zoutkristalarmband.
- 6 weapons, one per SPECIALISATION style (u-specs' specs), tooltip "Gemaakt voor: <spec>" (`Z6Loot.SPEC`): Rozenkromzwaard
  (Schildridder), Dadelpalmknots (Reuzenkussen), Duinvalkboog (Scherpschutter), Karavaankruisboog (Strikkenzetter), Saffraanstaf
  (Sterrenwever), Oasefluit (Zegenzanger). ~6 % above zone 5.
- Crystals (zone-2 gem system): Rozenkwarts (Kritiek 3), Sterrenzout (Bazenschade 3); drops like zone 4 (8 / 35 % / boss always).
- 10 named boss items with an effect (list in `Z6BazenEffecten`), 3 trophies, Gouden Veren (field bosses 2 on the first kill then
  25 % for 1; the dungeon end boss 1 every time; leaders 10 %), Sterrenstof (Zandgeestjes 1-2, bosses 3-6).
- Pets (8): Fennekje (1 % Fennekguh), Springpiepje (1 % Springpiep), Sterretje (2 % Gedoofd sterretje), Dadelaapje (0.5 % / quest),
  Bobbeltje (story), Wormelientje, Zoutklontje, Sterrenguhtje (first kills).
- Tooltip comparison arrows: u-specs' `GearVergelijk` handles every RijkItem, nothing extra needed.

### The BULTGUH (`zone6/bultguh/Bultguh` + 1 enum line)
- `RijdierSoort.BULTGUH(0.33, 0.60, 0, 1.95, "quest", 1.6, 2.5, TANDEM, ZANDLOPER)`: 330 %, +30 % on sand, 2 seats.
- Tandem: a non-owner right-clicks the mount while the owner rides it = sits behind (z +0.85); only the owner steers
  (`RijdierEntity.eigenRuiter`); owner off = the mount poofs and the passenger lands. A hit on either rider scares it (as before).
- Model: two humps, the saddle between them (bone `zadel`), a long neck (`nek`), head + guh ears, a striped blanket with tassels
  (`kwastjes`, they sway), little bells round the neck (`belletjes`, they swing). Coats: naturel (zandroze), saffraan, wolkwit,
  kaneel (stal 18 zilver each), sterrennacht (`Bron.api("z6_raid_sterrennacht")` for u-z6raid). Patterns and tack fit
  (anchors in `Z6WezensClient`), plus the zone-6 tack **Wormelien-zadeldeken** (first kill of Wormelien).
- Unlock: `Bultguh.geef(p, vacht)` from u-z6verhaal's stal quest (see `guhs_vadsrijk/POI_VERZOEKEN_Z6_WEZENS.md`); source text
  `vadsrijk.rijdier.bron.api.z6_stal`.

### Music hooks (`Z6Muziek`)
Every boss names `guhs:vr.geluid.baas.<thema>` via `muziek()`; until u-audio2 adds the theme to `GeluidPlan.BAAS_THEMAS` a stand-in
plays (no silence). Wanted themes (for `tools/vadsrijk/geluid/stukken.py`, "oed, qanun, darbuka, ney, guh-zoet; leitmotif = Oma
Dadel's caravan song"):
| theme id | boss | stand-in now | suggested spec |
|---|---|---|---|
| `wormelien` | Koningin Wormelien | zeeslurf | D minor -> major end, bpm 100, oud (kalimba lead) + darbuka ostinato, low "slurp" contrabass |
| `zoutkolos` | De Zoutkolos | brokkel | E phrygian, bpm 96, celesta/glass harmonica "singing salt" over taiko + low brass |
| `sterrenwacht` | De Astrolabiumwachter | spiegelzucht | A dorian, bpm 112, music-box clockwork arps + qanun, ticking hihat |
| `glimmertje` | Kootje Glimmertje | mika_spion | G minor sneaky polka, bpm 120, bassoon + pizzicato, candle "puff" hits |
| `verzonken_sterrenguh` | De Verzonken Sterrenguh | vahoegnir | F lydian, bpm 76, slow ney flute + choir pad, the caravan song in the last phase |
Mapping in game: `/guhs vadsrijk z6wezens muziek`. The phase crossfade/fanfare of u-audio2 work as soon as the theme exists.

### Places (`Z6WezensPlekken`)
All placeholders `w("<poi id>", x, y, z)`; `zone6.wereld.Z6WereldPlekken.plek(id)` wins by reflection. Ids + the volume to keep
free: `guhs_vadsrijk/POI_VERZOEKEN_Z6_WEZENS.md`. u-z6wereld's POI_Z6.md did not exist yet when I finished.

### Dev command `/guhs vadsrijk z6wezens ...`
spawn, alle, tp, baas, fase, mechaniek (duik, duinspuug, korst, zoutgolf, kristallen, zing, ringzwaai, lijn, doof, hart, spiegel,
slangduik, ringpot), bouw kerker|arenas, set, sieraden, wapens, kristallen, items, huisdieren, sterrenstof, loot, kist, bultguh,
plekken, spawners, muziek, opruimen.

## Decisions I made myself
- 13 plan creatures + the Zoutslang as the 14th kind (a normal elite creature, not an arena boss, in cave mode). If u-grotten adds a
  zone-6 cave with its own elite spawner, use `Z6WezensRegistry.ZOUTSLANG` and drop my `z6wezens:grot_zoutslang` spawner.
- Constructs crumble instead of being cheered up (Spiegelslang, Zandgeestje, Zandvlooien, Zoutslang), as in the plan's last column.
- The dungeon interior is this slice's own underground rooms (like zone 5's Buikgangen), entered by a hatch; the surface ruin is
  the map's. The rooms stay (not undone at restart like Herstel builds).
- Sterrenwacht set = 6 pieces (incl. gloves + cape), Karavaanwacht = 4 (gloves, cape, armband, boots) - the new slots are in the sets.
- Named items: my own names/effects. The spec weapons name the u-specs specs in their tooltip (no hard dependency).
- Tandem: anyone may hop on behind (no party check: harmless, and friends outside a party just work).
- Pet Bobbeltje is registered here (a baby Bultguh model) but only the story gives it.
- The Sterrenlens: matched by item id substring, so u-z6verhaal can name its item freely.

## Shared-file changes (all marked U-Z6WEZENS)
- `VadsrijkFeature.register`: `Z6WezensRegistry.register(modBus)` (after Z5DroomRegistry, before ItemsRegistry).
- `client/VadsrijkClient.init`: `Z6WezensClient.init(modBus)`.
- `economie/RijdierSoort`: + `BULTGUH` (after ZEILBOOTJE; u-tapijt appends TAPIJT at the same spot: a trivial merge, keep both),
  + `Eigenschap.TANDEM, ZANDLOPER`, + `zitplaatsen()`.
- `economie/RijdierEntity`: `eigenRuiter()` (the owner steers), `getControllingPassenger` -> owner, `getRiddenSpeed` (+30 % on sand),
  `opZand()`, the second seat in `getPassengerAttachmentPoint`, `canAddPassenger` (seats), `mobInteract` (hop on behind), tick/hurt
  use the owner. Behaviour of every other mount unchanged (1 seat: first passenger = owner).
- `tools/features/__init__.py`: + `vadsrijk_u_z6wezens` (before `vadsrijk_t_vfx`).
- Lang nl_nl/en_us: generated keys only (420). t-vfx's generic boss poses merged into my `vr_z6_baas_*` animation files.
- Merged `110-u-dubbel` (as asked): bosses spawn through kern `Arenas` (key `arena:<id>`), leaders through `Spawners` (derived key).

## Open points
- Looks are generated and unseen in a client (the user judges in the dev client): every creature, the 5 bosses, the objects, the
  Bultguh + tassels/bells + anchors (`Z6WezensClient`: KOP on `hoofd`, NEK on `nek`, RUG on `zadel` - offsets are guesses), the
  telescope turn direction (bone `kijker`, -90 deg per stand), the wormelien tack.
- Balance (HP, fractions, cooldowns, drop chances) first guesses. Level cap is still 60 in this tree: xp is clamped like zone 5.
- The dungeon has no door gating (rooms in a row); the bosses' arenas are the rooms themselves.
- The 5 boss themes need rendering by u-audio2 (table above). No AutoCheck client run.
- u-z6verhaal: wire `Bultguh.geef`, `Z6Bazen.start`, the KOMPAS hook and the quest pets (see the request file).

## Test results (2026-11-06)
- `runGameTestServer -Pgt=VadsrijkZ6Wezens,VadsrijkTRijdier,VadsrijkRijdieren`: all T-RIJDIER (5) and Rijdieren (6) tests passed with
  the RijdierEntity/RijdierSoort changes; 1 of mine failed: the kerker test fought three bosses with one player and zone 3's
  "one boss fight at a time" rule (Z3Toegang) kept the Sterrenguh from fighting. The Sterrenguh got its own test.
- Rerun `-Pgt=VadsrijkZ6Wezens`: **All 10 required tests passed** (wezens, zout, loot, wormelien, zoutkolos, kerker, sterrenguh,
  bultguh, muziek, + the batch split).
- No AutoCheck client run (the looks need the user's eyes in the dev client).
