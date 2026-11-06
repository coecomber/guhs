# NOTITIES U-TAPIJT (round U, agent U-TAPIJT, branch 110-u-tapijt, from eaee06d08)

The flying carpet ("Vliegend tapijt"), the zone-6 mount (ZONE6_PLAN hq06: Tapijtweefster Knoopje weaves it). Zone 6 isn't built, so
it comes with an unlock hook + dev commands. Feedback round 4: it must feel floaty and glidy, very different from the other mounts.

Package **`feature/vadsrijk/tapijt/`** (+ `client/`), mixin `mixin/client/TapijtZitMixin`. Generator **`tools/features/vadsrijk_u_tapijt.py`**
(one line in FEATURES, after `vadsrijk_t_vfx`; standalone: `PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_tapijt.py`).
Cheat sheet: `SPIEK_U_TAPIJT.md`.

## What is built
- **Species** `RijdierSoort.TAPIJT(0.36, 0.40, 0, 0.30, "quest", 2.0f, 0.5f, Eigenschap.GLIJDT)` (new trait `GLIJDT`, `glijdt()`). The
  entity is registered by EcoSpul's loop like every mount (`vr_eco_tapijt`), with `TapijtEntity::new` as factory for `glijdt()`
  species; saddle/icon item `vr_item_zadel_tapijt` (a rolled-up carpet) comes from the same loop.
- **Movement** (`TapijtFysica`, pure; `TapijtEntity.travel` runs it on the riding client = prediction):
  - own momentum (vx, vz) apart from the yaw; the yaw follows the view (`draai_volg`, max `draai_max` deg/tick);
  - W: push along the yaw that fades near the top speed (`versnelling` x top x (1 - v_along/top)): 7 % of top in tick 1, ~50 % in
    0.5 s, 95 % in 2 s; no W: x`uitglijden` per tick (0.984: ~45 blocks of glide from top speed); S: brakes `remmen` x top per tick,
    standing still backs off at `achteruit`; A/D: sideways push (`zijwaarts`);
  - drift: the momentum vector is rotated toward the yaw by `grip` (W held) / `grip_glijden` (gliding) of the angle per tick, only
    under 110 degrees (bigger: W/S just push against it); speed clamped to top;
  - hover: ground = the highest of the footprint (middle + 4 corners at 0.75) and a point 6 ticks ahead along the momentum (so it
    rises before a step), scanning 40 blocks down from carpet+1; fluids (water, lava) count as ground. A spring to `zweefhoogte`
    with capped climb (`omhoog_max`, `omhoog_acc`) and a gentle capped sink (`zwaarte`, `val_max`); nothing below = glide down at
    `val_max`. Never above hover + `hop_max` (hop = space, only within 0.3 of the hover height, cooldown 12 ticks, strength =
    the mount's jump stat x `hop_kracht`). Low ceiling: hover height = room - 1.6 (rider), min 0.15. maxUpStep 1.0. Bumping a wall
    kills that axis of the momentum (x0.2).
  - looks: roll from the turn rate (`kanteling` deg per deg/tick, max `kanteling_max`) + sideways drift, pitch from the speed
    change; computed on every client from what it sees (also for other players' carpets); a bob of 0.045 in the renderer.
- **Server**: `isFlyingVehicle() = true` (no "floating too long" kick, like the boat). Every 10 ticks `TapijtEntity.controle`:
  moved more than top x `server_marge` x 10 + 1 blocks = put back to the last check (+ ClientboundMoveVehiclePacket); more than
  hover + hop_max + 1.25 above the footprint ground while not sinking = put down to the hover height. Logged (every 5th time).
- **Tuning**: `TapijtBalans` (21 knobs, sane ranges) = section **`[tapijt]` in `config/guhs-vadsrijk-balans.toml`** (KlasseBalans).
  `/guhs vadsrijk tapijt tune <sleutel> <waarde>` (saved) and `/guhs vadsrijk balans tapijt ...` both work. A version counter: the
  server sends the numbers to every player (payload `vr_tapijt_balans`) on join and within a second of a change, so the predicting
  client uses the server's numbers.
- **Looks**, on the three mount look axes, but carpet-only (`Uiterlijk.zonderGarderobe(TAPIJT)`: no spots/hats on a carpet;
  species-specific PATROON/TUIG variants, which the data model already allowed):
  - VACHT "Weefsel": naturel (Rozensaffraan), turkoois, granaat (stal 20z), zandroos (8 veren), nacht (lv 10), karavaan
    (`Bron.api("z6_eerste_karavaan")`): a border with a zigzag, a lattice/lozenge/stars field, a medallion with a tiny guh face;
  - PATROON "Kwastjes": goud (start), roze, turkoois (stal 5z), wit (3 veren), regenboog (`Bron.api("z6_kwastjes")`): overlays
    `vr_rijpatroon_tapijt_kwast_*` that only paint the tassels (drawn untinted by UiterlijkLaag);
  - TUIG "Kussen": roze (start), saffraan, turkoois (stal 4z), paars (4 veren): tack items `vr_rijtuig_kussen_*`, drawn on the
    "midden" bone (anchor put in `UiterlijkLaag.ANKERS` from TapijtClient).
  - The unlock puts on the pink cushion + gold tassels. Axis labels per species via lang `vadsrijk.rijdier.as.<as>.tapijt`.
- **Model** (generator): 28 x 44 px carpet, 1 px thick, five pieces chained from the middle (voor2-voor1-midden-achter1-achter2),
  8 tassels per short end, guh ears on the four corners (pink inside). Animations idle / zweef / snel = a travelling ripple.
- **Rider**: cross-legged (mixin on `PlayerModel.setupAnim` TAIL, only when the render state has `TapijtClient.ZIT`), tilts and
  bobs with the carpet (RenderLivingEvent Pre/Post, same rotations as the carpet renderer). Seat: `zit` 0.30, 0.1 toward the back.
- **N screen**: the carpet is the 10th mount; rows shrink to fit (`rh`, 18 px at 10 mounts) so the list stays above the hint/button;
  under the speed/jump bars an extra line "Zweeft 1.2 blok hoog · glijdt ~45 blokken uit" (`TapijtClient.statRegels`).
- **Unlock API**: `nl.juiced.guhs.feature.vadsrijk.tapijt.Tapijt.geef(ServerPlayer)` (quest flag `tapijt_gekregen`, tip), NPC role
  **`z6_tapijt_geef`** (NpcRollen), `Tapijt.rijd(p)`. Reward hooks for zone 6: `Aankleden.geef(p, "patroon:tapijt:kwast_regenboog")`
  (side quest 4 "De verloren tapijtkwastjes"), `Aankleden.geef(p, "vacht:tapijt:karavaan")` (Zoutgrot chest).

## Decisions I made myself
- Steering = the view (like the land mounts), A/D = sideways push (not turning like the boat): the drift is the point, and the view
  steering makes "turn the carpet while it slides the old way" easy.
- Speed 360 % (between Pluimguh 350 and Guhwolf 370), small hop (0.40, ~1.2 blocks of jump stat; the hop is capped by hop_max).
- Sideways drift and A/D push without W decay with `uitglijden`, so nothing drifts forever.
- Lava counts as ground (it hovers over it; the rider never touches it). It hovers over leaves/fences (their collision top).
- Banking sign follows the sled (model +x = right); if it looks wrong in the client: `tune kanteling -3.5`.
- Tassels on the PATROON axis, cushion on the TUIG axis: no new axis needed, the panel and storage work unchanged.

## Shared-file changes (all marked `U-TAPIJT`)
- `economie/RijdierSoort`: value `TAPIJT` (last), trait `GLIJDT`, `glijdt()`.
- `economie/EcoSpul`: `rijdier(...)` takes the entity factory; the loop passes `TapijtEntity::new` for `glijdt()` species.
- `economie/client/EconomieClient`: renderer loop registers `TapijtRenderer` for `glijdt()` species.
- `economie/client/RijdierScherm`: list rows adapt to the count (`rh`), the carpet's stat line hook.
- `rijdier/Uiterlijk`: one call `Tapijt.registreerUiterlijk()` at the end of the static block (looks must exist before the tack items).
- `rijdier/client/AankleedPaneel`: per-species axis label (`vadsrijk.rijdier.as.<as>.<soort>` if it exists), a symbol for `kwast_*`.
- `rijdier/RijdierExtraRegistry.TuigItem`: right-click finds the variant's real key (species-specific tack: `tuig:tapijt:<id>`).
- `rijdier/Aankleden.geef`: the chat line for a species' own pattern/tack uses `nieuw.patroon_van` / `nieuw.tuig_van` (was "vacht").
- `klassen/KlasseBalans`: section `[tapijt]` (5 small lines + file comment).
- `VadsrijkFeature` (register + payloads), `client/VadsrijkClient` (init): 1 line each. `guhs.mixins.json`: `client.TapijtZitMixin`.
- `tools/features/__init__.py`: `'vadsrijk_u_tapijt'` appended. Lang nl_nl/en_us: only new keys (37).

## Tests
`VadsrijkTapijtGameTests` (batch `vadsrijk_tapijt`): versnellen (curve, smooth, never over top), uitglijdenEnRemmen (no instant stop,
long glide, glide number of the N screen, S brakes faster, backwards, sideways), drift (yaw turns max draai_max, momentum keeps the
old way, W aligns faster than gliding, aligns fully after 4 s, look-round keeps the speed), zweven (rise without overshoot, ledge:
capped gentle sink + soft landing, no ground: val_max, hop only near hover and never above the cap even holding space, ceilings),
server (teSnel / teHoog), zweeftOverDeGrond (a real carpet floats down slowly onto its hover height over stone), zweeftOverWater
(a carpet called in the water rises out and hovers over it), krijgenEnLooks (unlock,
start looks, 6/5/4 looks, every file + item, own looks only, wearing). Run: `-Pgt=VadsrijkTapijt,VadsrijkTRijdier,VadsrijkRijdieren`.
Results (2026-11-06): `runGameTestServer -Pgt=VadsrijkTapijt,VadsrijkTRijdier,VadsrijkRijdieren`: **all 20 passed**.
(Lesson: the "empty" template is 5x4x5; entities placed outside it get DISCARDED when the neighbouring test is set up.)
Also new: a carpet ignores damage without an attacker (water, cactus, walls), like the boat; only a real hit scares it.

## Open / for later
- Zone 6 itself: Knoopje's NPC with role `z6_tapijt_geef`; the two `Bron.api` rewards; carpets at stalls/the Karavanserai (deco).
- Not seen in a client by me: banking direction, cross-legged legs, cushion anchor (`TapijtClient.init`: bone "midden", y 1.2, z 1.6),
  ripple strength, corner ears. The user judges looks in the dev client.
- The first ride also shows the generic "Spatie = een grote hup" mount tip (once per player, from Rijdieren.roep) before the carpet tip.
