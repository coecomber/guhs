# NOTITIES_Z3_BAZEN: zone 3 "De Schemerwortel", slice "bazen" (Guhs 1.10, Het Vadsrijk)

Branch `110-z3-bazen`, worktree `guhs_vadsrijk/z3-bazen`, from ab6d952ac. Package `feature/vadsrijk/zone3/bazen`, ids `vr_z3_baas_*`, lang
`vadsrijk.z3bazen.*`. Generators: `tools/features/vadsrijk_z3_bazen.py` (texts + build hook, in FEATURES) and
`tools/features/vadsrijk_z3_bazen_modellen.py` (all art: models, animations, textures, blocks, icons; Blockbench copies in
`blockbench/vadsrijk/z3/`). Registered with one line each in `VadsrijkFeature` and `client/VadsrijkClient`.
Dev command root: `/guhs vadsrijk z3bazen ...` (tp, bouw, fase, vahoegnir [herinnering], vahoeg werf|hart, zegel, kerkers
reset|loods|ringen, loot, items, herstel). Kern's `/guhs vadsrijk baas reset z3bazen:<id>` works too.

## The engine
`Z3Baas` extends zone 2's `Z2Baas` (so every boss fights from the first second: all players in the arena are targets, also the
ones at range; its own rotation; hits from outside do nothing; a leash; an empty arena resets). Zone 3 adds:
- `mech(p, fractie)`: mechanic hits are a fraction of the player's max Vads, capped at 60 %: careless = faint after 2-3 mistakes,
  never a one-shot. `raak()` = a normal hit through the pipeline.
- `immuun()` / `vadsVloer()`: a boss waits at a threshold until its mechanic is done (cages, splinters, balloons), with a line
  to the hitter. `schadeFactor()`: armour plates, feeding roots, the lantern-light rule.
- `taak()/later()/rol()/inslag()/stapel()`: per-tick tasks, rolling things in red lanes, red circles, yellow stack circles.
- enrage timer (`enrageTicks`, default 7 min) with pulses; the beleving phase moments come from kern (lang keys
  `vadsrijk.kern.beleving.fase.vr_z3_baas_<id>.<n>`), the victory moment + chest ceremony from kern too.
- `Z3Vast`: held players (talons, root, Rijpingskamer): click/shift yourself loose, a teammate sneaking next to you helps, the
  boss's own damage threshold (bird) frees you; else a timed end (a drop with slow falling, never fall damage).
- `Z3Doel`: hittable / right-clickable mechanic objects (cage locks, balloons, feeding roots, deck cannons, kettles, the
  Schimmelkolos); `Z3Figurant`: any GeckoLib model as a prop (the arm, mice, egg, hatchling, throne-room cast);
  `Z3Hulpje`: adds (Mika-dekmatrozen, schimmelbolletjes, grauwe kloddertjes that roll to the egg; those get the zone-1 pink plof).
- Colours: red damage, pink safe, purple mechanic, yellow stack.

## The bosses (arena ids `z3bazen:<id>`)
- **De Kruinroofvogel** (lv 38, Kale Kruin, respawn 25 min, optional). Dives (red strips, aimed at players at range), wing gust
  (purple), fase 2 (70 %) Grijpklauw (carried up; 4 % damage frees, or 8 clicks), fase 3 (35 %) **De Achtervolging**: he flies
  ~200 blocks to `kruinroofvogel_zit_1|2` along a pink trail; while he flees, **mounts are allowed in his arena**
  (economie hook `Rijdieren.RIJDEN_IN_ARENA`, see below); whoever reaches the perch within ~32 s gets 10 s Vahoeg. The fight
  continues at the perch (bar sent by Z3Baas to fighters outside the arena). Pet "kruinroofvogel".
- **Kaaszeppelinwerf**: **Takelmeester Taleggio** (lv 38; red = platform falls / pink = safe rigging, knife lobs, rope leap;
  fase 2 hangs high, ranged hit him, melee pulls a rope lever when he swings over it -> falls, 5 s stunned, +50 % damage),
  **Bombardier Brokkel** (lv 39; cheese-wheel lanes, bomb lobs, 4 armour plates = 40 % damage, 2 deck cannons: right-click
  fires where you look, a hit = a plate off, reload faster solo; faster per plate), **Kapitein Roquefort** (lv 40, end boss;
  boarding charge, deckhands; fase 2 the deck tilts (shake + sliding) + 4 balloon segments with purple markers (floor 33 %
  until popped); fase 3 the zeppelin sinks: jump to the wreck within 6 s, the deck hurts 30 %/s; flees under his umbrella).
  Rooms (`Z3Kerkers`): Gieterij fondue lanes, Bewapeningsloods (3 levers/3 cannons, lever i turns cannon i and i+1;
  solution 1,0,2 presses), a safety net under the yard.
- **Graaf Piepvel** (lv 42, the Pelszaal, respawn 10 min, the central boss). Fase 1 Capezwier (rotating red cone), fluff balls in
  lanes, "deftige entree" leap at the furthest player. Fase 2 (70 %) vads shield fed by the 4 hanging cages (grey light lines);
  break the 4 locks -> -25 % shield each, the mice run to his cape and a cape panel goes (no time pressure). Fase 3 (40 %) he blows
  out the 6 lanterns (Darkness for fighters, he is invisible except his eyes away from a lit post, -50 % damage away from the
  light), relight posts by right-click, he blows at a lit post (purple: stand in it to save the flame). Floor 10 % until the big
  cage by the throne is opened (right-click): all mice storm his cape, he falls. Enrage 6 min = the hall freezes.
  Then **flash F1** (`Z3Scenes.armFlash`, no clicks): "Mijn cape!", the tower creaks, a long grey arm with five long fingers
  reaches in from `flash_f1_kwastgat`, grabs him ("Nee nee nee, Keizer...") and pulls him out; floep. The mice get their fur back
  and squeak off. Camera scene the first time per player (`z3:f1_arm`). Pet "piepje" (mini pieppiepmuisje).
- **Hart van de Boom**: **De Wortelwurger** (lv 49; heartbeat = 1.5 s dark every 4 s, the 4 feeding roots are only open during
  the beat, each living root +20 % DR + heals; grabs from the wall (held), slam, spore lobs), **De Oude Nestwachter** (lv 49;
  year-ring waves: stand on pink rings; shoulder charge shoves you to the edge; at the end he turns pink, bows, stays 20 s:
  moment "nestwachter_gered"), **Baron Bleu + Schimmelkolos** (lv 50, end boss; fase 1 on the walking kettle (melee hits the
  kettle = hits him), mould fields burned by lit lantern posts, the kettle sprays posts out; fase 2 he fights himself (lunges,
  Rijpingskamer = held); fase 3 the arm reaches for the egg: yellow stack circle, solo 1 player can block it but takes 50 %;
  flees in his escape zeppelin). Rooms: Aderloop (don't move in the dark), Schimmelkwekerij (3 kettles: right-click x3 each,
  spores), Ringenzaal (knob i turns ring i and i+1, solution 1,2,4 presses).
- **Vahoegnir** (lv 50, the old nest). Not a respawning boss: `Z3BazenApi.startVahoegnir(p, herinnering)` (hq14 after the ride, or
  the Droomsteen). 75/50/25 %: he lands and is immune until a splinter goes back (3 s in the pink circle under his chest;
  `Z3BazenApi.HEEFT_SPLINTERS`), texture `vahoegnir_<n>` goes grey -> pink. Fase 2 black fluff + grey kloddertjes rolling to the
  egg (heal him), fase 3 crystal howl (hide behind the 6 nest branches, they crack then break), fase 4 he circles and dives at
  the egg 3x (yellow stack circle). Then `Z3Scenes.vahoegnirEinde`: splinters 4+5, pink, the chests/victory, the egg hatches
  (Vahoegje, pet "vahoegje"), he giggles, the pink beam toward Hoogvads (east), he flies off along it, then **flash F2**
  (`Z3Scenes.troonzaal`, ~20 s, no clicks, first time per player and not for a "herinnering"): a small grey throne room built
  for the scene 26 blocks above the nest (removed after), Piepvel kneeling in his shirt, Hofschrijver Krabbel's report, the long
  fingers tapping, "Dan... reik... ik... verder.", "Wordt vervolgd...".

## Vahoeg mode (light: no lockouts, no leaderboards)
`VahoegModus`: right-click the Vahoeg altar (`vr_z3_baas_altaar`, place it at `werf_vahoeg_altaar` / `hart_vahoeg_altaar`) with a
Vahoegzegel in your bags (z3-systemen's `Vahoeg.heeftZegel/neemZegel`, lv 50 via `Vahoeg.magVahoeg`; item `vr_z3_vahoegzegel`):
for 30 min (or until the end boss falls) the bosses of that dungeon spawn as Vahoeg (lv 52, x1.5 HP, x1.25 damage, 1 extra
mechanic each, the week seal by ISO week: Donker / Haastig / Splitsend / Taai). Vahoeg bosses give 1-3 Gouden Veren
(`GoudenVeren.geef`), the end boss rolls `GoudenVeren.rolGoudenPluimguh` (the 2 %); the 25-feather guarantee is systemen's
(`GoudenVeren.ruil` at Veertje). Normal dungeon bosses drop a Vahoegzegel 20 % of the time. Boss xp = `Z3Xp.baas(level)`.
**Merged `110-z3-systemen` (ca18781a8) into this branch** for these APIs.

## Loot (`Z3BazenLoot`, effects `Z3BazenEffecten`)
Named Legendary (lv 38-50): Roofvogelklauw, Kruinverenmantel (Kruinroofvogel), Takelmeesters Touwknots (Taleggio), Brokkels
Lontstaf (Brokkel), Roquefortsabel, De Paraplu van de Kapitein (Roquefort), Kooisleutel, Cape van gebreide sokken (Piepvel),
Wortelwurgers Hartwortel (Wortelwurger), Knoest z'n Lantaarnstaf (Nestwachter), Schimmelbreker (Bleu). Guhddelijk: Monocle van
Baron Bleu (2 %), De Eierwarmer (Vahoegnir, 3 %, KETTING). First kill = a sure Legendary + the trophy. 9 trophies (not gebonden).
Pets via `Huisdieren.registreer`: kruinroofvogel, piepje, vahoegje.

## For other slices / integration
- **economie (changed, small)**: `Rijdieren.RIJDEN_IN_ARENA` (list of `Predicate<Vec3>`) + `Rijdieren.inArenaVerbod(x,y,z)`
  replace the three `Arenas.op(...) != null` checks (toets, afReden, HUD). Backwards compatible; bazen adds the chase predicate.
- **npc**: `Z3BazenApi.MOMENT` (BiConsumer<ServerPlayer,String>): "piepvel_weg" (after F1: hartsplinter 4, Piepje out of the
  bag), "nestwachter_gered" (splinter 5, the NPC), "vahoegnir_splinter_1..3", "vahoegnir_opgevrolijkt", "vahoegje",
  "hoogvads", "troonzaal_klaar" (zone3_klaar / z3_hq15), and "baas:<id>" for every zone-3 boss beaten.
  `Z3BazenApi.startVahoegnir(p, herinnering)`, `Z3BazenApi.HEEFT_SPLINTERS`. The plan put flash F1/F2 in the npc slice; the
  caller gave them to bazen: they live in `Z3Scenes` (npc only needs the moments). `flash_f2_kijkpunt` (Nestkapel) is unused:
  the F2 set is built above the nest so its chunks are loaded where the players are.
- **systemen**: merged; bazen uses Vahoeg / GoudenVeren / Z3Xp / Z3Spul.VAHOEGZEGEL (no own seal/feather items).
- **wereld**: POI requests in `POI_VERZOEKEN_Z3.md` (section bazen). The altars must be PLACED by wereld (block
  `guhs:vr_z3_baas_altaar` at the two altar POIs). Gates are 3x4 planes of `vr_z3_baas_poort` along x, only into air.
- **wiki**: blokuitleg entries added for lantaarnpaal, altaar, hendel, ringknop.

## Open choices / not done
- Balance numbers are first guesses (HP, fractions, lock/balloon/root HP, cooldowns).
- The Mika-imker (secret boss, plan §5) is NOT built (not in the caller's list); nor the dungeon time trial (the user dropped
  the scoreboard).
- The respawned Graaf keeps the mouse-pelt cape (the mechanic needs the cages); `piepvel_sokken.png` exists for a later
  "sock cape" variant; his intro line already changes ("Nog steeds koud hier...").
- Bleu's egg is a prop in his own room (the real egg POI `ei` is in the Nestkamer, 95 blocks away).
- `/guhs vadsrijk z3bazen bouw` makes stand-in discs (only into air) for testing before the world merge.

## Resume point (2026-11-04)
- Commits: 6c991c035 (everything), 2fa468679 (merge 110-z3-systemen), f6025413b (systemen APIs, tests). No push.
- Gametests `runGameTestServer -Pgt=VadsrijkZ3Bazen`: **all passed** (brokkel: target at range + rotation + 40 % armour + cannon
  knocks a plate off + reload + no hits from outside; roofvogel: grab freed by clicks/help, riding allowed in his arena only
  while he flees, immune in flight, reset ends the chase; piepvel: 4 cages + 6 posts, shield holds until the 4th lock, cape
  panels go, dark = all posts out and -50 %, a lit post = full damage, 10 % floor, big cage, defeat, the arm removes him;
  vahoegnir: lands at 75 %, immune until 3 s in the pink circle, splinter 1 back; puzzles + Vahoeg form).
  Test notes: bosses got a `testFilter` (Z3Baas.strijders) and the test floors force-load their chunks.
- No AutoCheck run (memory); the look of all models is unverified in a client (the user judges in the dev client).
