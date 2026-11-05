# NOTITIES T-LUCHTSCHIP (night round T)

Branch `110-t-luchtschip` from dbf82899d. Sources: WERELDINDELING.md §4 (airship lines) + §5 (sight lines), ZONE4_PLAN W-1..W-3.

## What is built

Package `feature/vadsrijk/luchtschip/` (+ `client/`), generator `tools/features/vadsrijk_t_luchtschip.py`.

| File | What |
|---|---|
| `Lijnen` | THE data class: all harbours (POI ids `haven_<van>_<naar>_vertrek`, placeholder coordinates, open-air direction, zone token) and lines 0/A/B/C/D (control points, phase), luxury price, the zone-4-finale flag, the POI lookup chain `ZOEKERS`. |
| `Dienstregeling` | Pure timetable + path maths, the same on server and client: period 3 min, flight 60-90 s (length / 8 bl/s, clamped), two ships per line, Catmull-Rom path resampled by arc length, ease in/out, the ship turns round on the last 14 % so it lies at the pier with its bow out. The way back = the points reversed, shifted 22 blocks sideways in the middle. |
| `LuchtschipEntity` | GeckoLib ship (`vr_luchtschip`, scale 4: ~18 x 7 blocks deck, balloon ~20 blocks long). It is a timetable puppet: server and client each compute its position from the game time (the client uses a soft clock that follows the server's time), so there are no position packets and the flight is smooth. 16 standing places on the deck (`PLEKKEN`), passengers turn with the ship and can look around. Never saved (`noSave`). |
| `VertrekbordEntity` | One text line of a departure board (invisible, name tag only, NoopRenderer). 3 per harbour. |
| `Luchtschepen` | Server manager: spawn a ship while a player is within 320 blocks of its timetable position, remove it beyond 420 (only without passengers). It also keeps far ships on time (`volg` when not entity-ticking), the boards (within 56 blocks, rewritten every second), the action bar (near a pier: next departure; on board: departure/landing countdown), boarding (`instappen` + gates `SLOT`), departure/landing (`faseWissel`), edge cases, the luxury ticket (`luxe`) and dev status. |
| `Tijd` | Dev-only clock shift per line (`vertrek`), synced to clients; the two payloads. |
| `Zicht` | Own view distance for the Vadsrijk ServerLevel (`ServerChunkCache#setViewDistance` on that level only), config `config/guhs-vadsrijk-zicht.properties` (`zichtafstand`, default 20, 0 = off, max 32), re-applied every second, client radius packet on login/dimension change/respawn and when it changes. |
| `LuchtschipRegistry` | Entity types, events, dev commands `/guhs vadsrijk luchtschip ...`, payloads. |
| `client/LuchtschipClient` | Renderers (GeckoLib ship with glowmask, NoopRenderer for the board lines), AfterSky hook, payload receiver. |
| `client/Schimmen` | The far silhouettes at their REAL position and size: quads with the real world corners, scaled towards the camera onto the sky (uniform scaling about the eye keeps direction + apparent size exact), drawn after the sky and before the terrain, faded out when the thing is within the render distance, tinted by time of day. Boom, Splinterpieken, Kaaszeppelinwerf (round: a camera-facing billboard), Moeder Wolkwal + Hoogvads (long: a side quad along x + a front quad, each faded by the view angle). |
| `VadsrijkLuchtschipGameTests` | 5 tests: timetable/paths of all real lines, a real test-line flight (board, locked in, no damage, no sneaking off, landing on the other pier, tag cleared), logout in flight, luxury ticket, view distance. |

Assets (generator): `geckolib/models|animations/entity/vr_luchtschip.*`, `textures/entity/vr_luchtschip(.png|_glowmask.png)`,
`textures/vadsrijk/luchtschip/schim_{boom,pieken,werf,hoogvads_zij,hoogvads_voor}.png`, 40 lang keys (nl + real en).

## Decisions I made myself
- **Ship technique:** an entity puppet (like the existing balloon/Zuka ship), not a moving block structure. Players are passengers standing on the deck (`shouldRiderSit false`), up to 16. There is no collision deck: it is robust in multiplayer, with no desync and no falling through. You board by right-clicking the ship while it lies at the pier, and sneak to get off while it is still there.
- **Timetable from the game time** (deterministic), not a server-side state machine. A restart is therefore "free": the ships are simply at the right place again. The ship entities are only there near players (cheap).
- **Two ships per line**, departing from both ends at the same moment every 3 min and passing each other halfway with a 22-block offset. Docked time per ship = 3 min - flight (90-120 s boarding).
- **Line 0** (Kampsteiger <-> Haven Zwamzoet) is in the data, but it only flies in the world once its own POIs
  `haven_kamp_z1_vertrek` / `haven_z1_kamp_vertrek` exist (`Lijn.actief()`). Both old docks carry the anchored block airship of
  the scene flight, and a real ship there would collide with it. Dev `rij 0` works anyway. The old bell flight stays (it is the
  luxury map now).
- **Barrier exemption (t-wereld `Barrieres.TAG_VRIJ`):** the ship entity carries the tag `guhs_luchtschip`.
- **Departure board position:** t-wereld's POI `<haven>_bord` when it exists, else above the pier spot.
- **Gates (heenweg):**
  - 0 = `WereldReizen.MAG_VLIEGEN`;
  - A = `QuestMotor.reisSlot(p,"kandijkom")`;
  - B = `Z3Reizen.slot`;
  - C = flag `zone3_klaar`;
  - D = flag `zone4_klaar` (`Lijnen.VLAG_Z4_FINALE`; t-z4verhaal should set it at the finale).
  - The way back is always open, and creative ignores the gates.
- **Arrival bookkeeping:**
  - zone 2: the first story quest `z2_hq01_splinterpieken` is accepted (as the captain's flight did);
  - zone 3: `Z3Reizen.GEZIEN_EERSTE` is set (counts as reached for the travel map). The zone-3 opening film does NOT play on a line-B arrival (Z3Reizen.opening is package-private; open point).
- **Luxury ticket:** 400 brons (`Lijnen.LUXE_PRIJS`), charged in the travel map and in the captains' direct travel choices. The first luxury flight to each place is free (so the story's first trips stay free). Tests (`VLIEG_TEST`) fly free.
- **Edge cases:**
  - logout in flight: put on the destination pier before the save (at the pier: that pier);
  - server stop/crash: the destination is in persistent data `guhs_vr_luchtschip_naar`, and you land there on login;
  - death: the tag is forgotten;
  - damage on board: cancelled (except `/kill`/void);
  - thrown off another way (teleport, ship removed): at the ship = landed at the destination, far away = slow falling.
- **The ship lies with its middle 9 blocks out from the pier POI** (deck at the POI's height, bow pointing to the open air). t-wereld: build the pier so there is ~16 blocks of free air in the harbour's direction (see `Lijnen` directions: Noordklif N, Kandijkom-dok S, Galmdok E, Weststeiger W, Oosttak E, Lampionhaven W, Opstijgpier N, Walvisrug S, Kampsteiger E, Haven Zwamzoet S).
- **View distance default 20** (32 is possible with `zicht 32`; 32 = ~4200 chunks per player, heavy for the live server, so not the default).
- **The silhouettes are drawn in the sky layer** (behind all terrain). The "only while not loaded" rule is approximated by the distance vs the effective render distance (cheap, no chunk queries).

## Shared-file changes (all marked `T-LUCHTSCHIP`)
- `VadsrijkFeature.java`: +1 line in `register`, +1 in `payloads`.
- `client/VadsrijkClient.java`: +1 line `LuchtschipClient.init`.
- `npc/Reiskaart.java` (`kies`): luxury ticket check before `QuestMotor.vliegNaar`.
- `npc/QuestMotor.java` (travel option branch): the same check before `vliegNaar`.
- `wereld/client/WereldLucht.java`: skip fixed-azimuth landmarks 0-2 (spires, Hoogvads, tree) while `Schimmen.vervangt` (the volcano stays).
- `zone3/wereld/client/Z3Lucht.java`: skip the fixed Hoogvads billboard while `Schimmen.vervangt`.
- `tools/features/__init__.py`: +`vadsrijk_t_luchtschip` in FEATURES.
- `lang/nl_nl.json`, `lang/en_us.json`: +40 keys (generator).

## For integration / other agents
- **t-wereld / t-z4wereld:**
  - add the POIs `haven_<van>_<naar>_vertrek` to the generated POI classes; they are found automatically in WereldPlekken/Z2/Z3.
  - For a zone-4/5 POI class add one line: `Lijnen.ZOEKERS.add(Z4WereldPlekken::plek)`.
  - Line 0's ids are `haven_kamp_z1_vertrek` / `haven_z1_kamp_vertrek`: either add those as POIs, or let integration point the placeholders at `kampsteiger` / the existing haven POI.
- **t-z4verhaal:** set the quest flag `zone4_klaar` at the finale (or replace `Luchtschepen.SLOT.put("z4_z5", ...)`).
- **Other slices:** replace `Luchtschepen.AANKOMST` for their own first-arrival scene.
- **When the control points change** (better routes once the harbours are built): edit `Lijnen` (the `via` lists). The gametest checks 60-90 s, no jumps > 0.75/tick, and never below y 60.

## Resume point / status
See the bottom of this file (filled in at the end of the session).

## Open points
- The zone-3 opening film is not played on the first line-B arrival (only the flag is set).
- Line D flies always (ambient), even before the finale; only boarding is gated. If the story wants no ship before the finale: hide it in `Luchtschepen.schepen` for line D until a world flag.
- Routes are hand-drawn over the planned terrain. They do not check collisions with the real map (the kloof, the werf). Check them in the client once t-wereld's map is in: `/guhs vadsrijk luchtschip rij <lijn>`.
- No deck collision: you cannot walk around on the deck, you stand in one of 16 places.
- The board text is a name tag (visible within 64 blocks, always facing you), not a real sign.
