# NOTITIES_T_KLEDING (night round T, 2026-11-05/06)

Branch `110-t-kleding` from dbf82899d. Package `nl.juiced.guhs.feature.vadsrijk.kleding` (+ `.client`), generator
`tools/features/vadsrijk_t_kleding.py`. Design: VOORSTEL_ROADMAP §4.2/§4.3/§4.4 + R3/R4/R5 (§13). Cheat sheet: `SPIEK_T_KLEDING.md`.

## What is built
- **Cosmetica** (catalogue, common code): 10 wings (3 from t-puzzels), 8 cosmetic capes (3 from t-puzzels), 7 head accessories, 5 name colours, 8 dyes, 7 cheer-up
  effects, 5 trails, 3 auras. Id = `<soort>:<naam>` = unlock key. Each has a source spec (see `Bronnen`). `Cosmetica.registreer`
  lets another slice add one (its own lang/model).
- **KledingData** = the ACCOUNT store: overworld SavedData `guhs:vr_kleding`, one CompoundTag per player UUID. Not in
  `SpelerData` (that becomes per character slot). Swap point for t-karakter: `KledingData.BRON` (interface `kast(p)` / `bewaard(p)`).
- **Kledingkast** (server API): unlock (`ontgrendel`, `geheim`, `geefBron`), choices (`kies`, `kiesLook`), transmog looks
  (collected from bag + worn armour every 10 s and on `Haken.LOOT`; key = slot|equipment asset → an item id), what other clients
  see (`uiterlijk`, broadcast on change + login), the screen data and actions.
- **Bronnen**: checked sources every 10 s per player in the Vadsrijk (start, binnen, vlag:<npc flag>, boek:<zone>:<pct>,
  level:<n>, veren:<n>, kills:<n>, alleboeken); "later:..." never unlocks; direct ones come from hooks.
- **Zweven** (server) + `KledingClient.zweef` (client): the zone-3 Pluimguh glide on foot. Hold space in the air: sink 0.12 b/t
  (longer glide 0.065), forward ~walking (faster glide 0.34 b/t). Never up. Tricks on a NEW space press in the air, once per
  flight: double jump (0.48 up) / air dash (0.95 forward). Soft landing = `LivingFallEvent` cancelled + pink poof. Server: only
  trusts a glide from someone with gliding + wings, in the Vadsrijk, outside arenas (`Arenas.op`) and dungeon areas
  (`GEEN_ZWEEF_GEBIEDEN` = z2_spiegelburcht, z3_zeppelinwerf, z3_hart; `Zweven.VERBODEN` for zone 4/5 slices), not in a scene;
  keeps fall distance 0 + `Knuffelsprong.beschermTot` for the landing. Place rules synced to the client (payload `Staat`) on change.
- **Plofjes**: last hitter per mob via `Schade.NA_KLAP`; on `Haken.VERSLAGEN` the last hitter's cheer-up effect is sent to clients
  within 48 blocks; every helper +1 on the account kill counter; Vahoeg bosses (`Z3Baas.vahoeg()`): first one = Knuffelregen,
  3 % (end boss 8 %) Schemervleugels with a server broadcast.
- **Vahoegnir**: `Z3BazenApi.MOMENT` "vahoegnir_opgevrolijkt" → Vahoegveertjes (+ gliding), put on at once, chat line, title
  card + tip (waits until the scene is over). Fallback for old saves: the source also accepts the `zone3_klaar` flag.
- **Client**: key O (`key.guhs.vr_kledingkast`, category Vadsrijk, works at home too), collection tab "Kledingkast" (order 40),
  `KledingScherm` (EcoScherm style: live 3D preview, 9 categories, rows with Kies/Aan/locked + source tooltips, 2 effect sliders),
  `KledingLaag` (render layer + render-state modifier: transmog in the Vadsrijk, cosmetic cape via the skin's cape texture, wings +
  accessory as tinted item models), `Effecten` (trail/aura/cheer-up particles, all client-side from synced looks; budget 48
  particles/tick, 32 blocks; sliders Anderen/Eigen ALLES/MINDER/UIT in `config/guhs_kleding.properties`; arena: trails thinned,
  auras off), name colour via `PlayerEvent.NameFormat` (HIGH, before the titles feature appends its title).
- **NPCs**: no new NPC entities: the existing hub tailors (`kleermaker` Zwamzoet, `z2_breister` Kandijkom, `z3_kleermaker`) get an
  extra dialogue choice "Naar de Kledingkast" through `QuestMotor.EXTRA` (public API, no npc file touched).
- **Dev**: `/guhs vadsrijk kleding kast|alles|geef|neem|kies|wis|check|lijst|zweef|plof|kills|geheim|vahoegnir`; player command
  `/guhs vadsrijk kledingkast` (no permission).
- **Gametests**: `VadsrijkTKledingGameTests` (batch vadsrijk_t_kleding, `-Pgt=VadsrijkTKleding`).

## Decisions made alone
- **Wings render without GeckoLib geo.** A GeckoLib render layer needs a GeoRenderer parent; the player renderer is vanilla. The
  wings are baked item models (cuboid feathers, Python-generated, 3 poses per wing) on a vanilla `RenderLayer`: folded / spread /
  flap; static beyond 24 blocks (LOD). Cheap, tintable, no extra entity. Real GeckoLib animation = a later swap in `KledingLaag`.
- **Gliding needs the Vahoegveertjes** (the story unlock) AND any wings on. Tricks that don't need gliding (double jump, dash, soft
  landing) work with those wings even before the unlock. Every wing type glides once gliding is unlocked.
- **Double jump** was explicitly on the R4 list, so a single small hop (~1.4 blocks) is allowed; nothing ever keeps you rising.
- **Gouden Veren**: "25 Gouden Veren" = collected 25 (the counter, or already traded for the gold Pluimguh); feathers are NOT spent
  (Z3Data.zetVeren is package-private; spending would make the gold Pluimguh and the wings fight over the same feathers).
- **Transmog only in the Vadsrijk** and only over a piece you really wear (WoW rule). At home your armour is your armour.
- **Dye** tints wings + head accessory (item-model dye tint); capes are painted textures; armour dye not done (the Vadsrijk
  equipment assets are not dyeable layers; changing them = other slices' assets).
- **Starters** (earned by just being there): Confetti, dye Roze + Wit, Guh-oortjes (first time in the Vadsrijk).
- **Secrets** built here: "zweefvlucht" (one glide of 100+ blocks down) → Pluisvleugeltjes; "kleermakers" (open the wardrobe at all
  three tailors) → Reiscape. `Kledingkast.geheim(p, id)` is the hook for t-puzzels.
- **Key O** (free in the T brief list). The wardrobe also works at home (cosmetics go home).

## Shared-file changes (add-only, marked T-KLEDING)
- `feature/vadsrijk/VadsrijkFeature.java`: `KledingRegistry.register(modBus)` + `KledingRegistry.payloads(registrar)`.
- `feature/vadsrijk/client/VadsrijkClient.java`: `KledingClient.init(modBus)`.
- `tools/features/__init__.py`: `'vadsrijk_t_kleding'` in FEATURES.
- `assets/guhs/lang/nl_nl.json` + `en_us.json`: 174 keys `vadsrijk.kleding.*` + `key.guhs.vr_kledingkast` (via the generator).
- Uses (no edits): `QuestMotor.EXTRA`, `Z3BazenApi.MOMENT`, `Haken.VERSLAGEN/LOOT/BINNEN`, `Schade.NA_KLAP`,
  `Knuffelsprong.beschermTot`, `Arenas.op`, `Gebieden.op`, `Titels.behaald/actief/kies`, `CollectieTabs`, `EcoScherm`.

## For integration / other slices
- **t-karakter**: point `KledingData.BRON` at the account store (keep the tag layout). Cosmetics are account-wide by design.
- **t-items** (gear cape slot): our cosmetic cape replaces the drawn cape (skin cape texture); wings hide the cape. If t-items draws
  its stat cape another way (own layer), it should skip drawing when `KledingCache.kaap(uuid) != null || KledingCache.vleugel(uuid) != null`.
- **t-puzzels**: `Kledingkast.geheim(p, "<id>")` + add a `Cosmetica.registreer(new Ding("kaap:x", KAAP, "geheim:<id>", null, 0, 0))`.
- **zone 4/5**: `Zweven.VERBODEN.add(p -> ...)` for dungeons; the Lampionvlieger + Lampion-haarspeld unlock by themselves once a
  `zone4` Verkenningsboek exists (change their source from `later:zone4` to `boek:zone4:100` / `boek:zone4:25`).
- **t-vfx**: our particles are vanilla; if the VFX kit offers nicer ones, swap them in `Effecten`.

## Resume point / open
See the bottom of this file (updated at the end of the run).

## t-puzzels coupling (added at the lead's request)
- 3 wings + 3 capes with source `puzzel:<t-puzzels id>`: `vleugel:sterrenstof` (pz_vleugels_sterrenstof, double jump),
  `vleugel:poollicht` (pz_vleugels_poollicht, longer glide), `vleugel:lantaarnmot` (pz_vleugels_lantaarnmot, soft landing),
  `kaap:sneeuwvlok` (pz_cape_sneeuwvlok), `kaap:honingraat` (pz_cape_honingraat), `kaap:mos` (pz_cape_mos). Names hidden ("???") until unlocked.
- Soft dependency by reflection (compiles without 110-t-puzzels): `KledingRegistry.puzzels()` adds a listener to
  `Puzzels.ONTGRENDEL_HAAK` at start-up; the 10 s check pass also asks `PuzzelSpeler.ontgrendeld(p, id)` (covers older unlocks).
  Integration may replace both with direct calls. Dev: `/guhs vadsrijk kleding puzzel <pz_id>`.

## Wing rendering, as it ended
- First version posed two halves per frame in code; in the AutoCheck client only one half showed. Now each wing has three
  BAKED pair models (`_dicht` folded 45 deg back, `_open` 22.5 deg, `_op` tips up 22.5 deg); gliding alternates open/op every
  4 ticks (= the flap), LOD > 24 blocks: no flap. Verified in the AutoCheck client (screenshots run/screenshots/autocheck/tk_*.png):
  both halves visible, capes, ears on the head, the wardrobe screen. Looks are for the user to judge in the dev client.

## Tests
- `runGameTestServer -Pgt=VadsrijkTKleding`: 8/8 green (see the final report for the last run).
- AutoCheck script: `tools/autocheck/vadsrijk_t_kleding.txt` (screen, every wing, capes, hats, two cheer-up effects).

## Open / not done
- Real gliding was NOT tested in a client (AutoCheck can't hold space): the client logic is the Pluimguh glide on foot; check in
  the dev client: jump off something in the Vadsrijk and hold space; double jump / dash = tap space again in the air.
- No new NPC entities (hub tailors got the choice); no armour dye (assets not dyeable); the lantern kite + hairpin wait for a
  zone-4 Verkenningsboek (`later:zone4`).
- Transmog only draws over a piece really worn (by design) and only in the Vadsrijk.
- The "Njeg!" letters are dust particles; readable but rough. The effects are vanilla particles (t-vfx could swap them).
- en_us texts are real English; NL is the main text.

## Resume point
All committed on 110-t-kleding. Next for integration: wire `KledingData.BRON` to t-karakter's account store; t-items cape: see above.
