# NOTITIES_U_LEVEN_RITME (round U, branch 110-u-leven-ritme from 110-v-int 3349c1d52)

FEEDBACK_RONDE4 V7 "levendigheid": day/night rhythm in the towns, weather with charm, reactions to your emotes (knuffelmeter), a
town newspaper/notice board. Package `nl.juiced.guhs.feature.vadsrijk.ritme` (+ `.client`), generator
`tools/features/vadsrijk_u_leven_ritme.py` (lang prefix `vadsrijk.uritme.`), payload `guhs:vr_uritme_forceer`, entity tag
`guhs_vr_uritme`. Test steps: SPIEK_U_LEVEN_RITME.md. Not overlapping u-leven-stad (critters, street life, playgrounds, terraces):
this slice spawns no street guhs, only sleepers in beds, 2 night-market stall guhs and owls.

## What was built
- **`Steden`**: the 9 towns (knuffelkamp, zwamzoet, haven, kandijkom, honingtak, lampionhaven, hoogvads, rozenkloof,
  saffraanpoort). Each has:
  - a box/ellipse with a y range that works on the client too (kern `Gebieden` only gets the zone 2-6 areas at server start);
  - its kern area ids;
  - a zone number;
  - anchors for the board (next to the town's `questbord`, or a square) and the night market. The anchors are read from the
    generated `*WereldPlekken.ALLE` maps by POI name.
  - **Zone 6** (`z6_rozenkloof`/`z6_saffraanpoort`, POIs `rozenkloof_plein`, `saffraanpoort_plein`, `saffraanpoort_bazaar`) is read
    by reflection from `zone6.wereld.Z6WereldPlekken`, with the u-z6wereld numbers as fallback. It works before and after that
    merge. Zone 6 box = U-Z6WERELD's tile box (`Steden.zone`).
- **`Rooster`** (the schedule): night = time of day 13000..23000, or forced by `/guhs vadsrijk ritme nacht|dag|auto`.
  - A shopkeeper (`WINKELROLLEN`: winkel, smid, markt, stal, kleermaker, juwelier, wapenwinkel, bank, honingthee, honingkraam,
    chocomelk, z4_*soep/limonade, z6_tapijtwinkel) whose role is bound, and that is NOT in any quest (giver, turn-in, objective,
    speaker, tracker spot, objective source), NOT a travel NPC (`QuestMotor.REIZEN`) and has NO `QuestMotor.EXTRA` choices, walks
    to the nearest free bed (40 blocks).
  - It sleeps there with the guh SLAPEN loop emote. kern/npc's own RijkGuh is only moved, with `RijkGuh.rust` so Npcs' keeper
    doesn't snap it home.
  - Without a bed it dozes at its stall.
  - At its spot: a "Gesloten, morgen weer open!" text, a bell display and an Interaction click box. The bell runs
    `NpcRollen.doe(rol)`, so the same service opens.
  - Clicking the sleeping shopkeeper does the same, through `QuestMotor.KLIK_VOOR`.
  - Dawn: it wakes with a yawn and walks or snaps home.
  - The 24/7 shopkeepers yawn now and then at night.
- **`Bedden`**: finds bed heads per loaded chunk (palette `maybeHas` first) and caches them. Claims are by id, so no bed is shared.
- **`Nachtleven`**:
  - up to 6 sleepers per player in town beds within 40 blocks. They are RijkGuhs without an NPC id, named "z Z z", sometimes in a
    nightcap or pyjamas. A third of the beds stay empty. They never spawn within 6 blocks of you. A click gives a snore line.
  - 3 owls per occupied town: the existing `vogels` guh-uiltje, perched on roof tops. Gone at dawn.
- **`Nachtmarkt`** (Zwamzoet, Kandijkom, Lampionhaven, Saffraanpoort):
  - a ring of 8 big and 16 small hanging lampions (Guhs `lampion_roze/geel/mint`, BlockDisplays, full bright) 4 blocks above the
    anchor;
  - a "Nachtmarkt" text;
  - 2 stall guhs on free cells near the anchor: Lotje Lampion sells a snoepje (10 brons, 60 s Snelpootjes), Opa Sterrenkoek sells a
    koekje (5 brons, a fortune and +30 % Vads).
  - Only while a player is within 64 blocks at night.
- **`WeerKlok`** (common, deterministic per day + zone, no packets):
  - showers in zone 2 (snow), 3 (blossom), 4 (rain), 5 (cloud parade) and 6 (sand swirl) on 2 of 3 days (zone 6: 3 of 4);
  - a rainbow after the zone 2-4 showers;
  - zone 4's evening lampion glow every evening;
  - morning mist around sunrise every other day.
  - Zone 1 keeps fix-sfeer's pink shower + rainbow (not duplicated).
- **client `WeerBeeld`**:
  - particles on a budget: per tick at most 14 snow, 10 blossom or 16 rain particles, about 4 glow motes, about 4 cloud
    particles and 8 sand particles, times the slider. Particles fall only where the sky is open above. There is a soft rain sound.
  - fog colour and distance for the mist, the lampion glow and the rain (LOW priority, on top of fix-sfeer's area fog);
  - fireflies at night in the towns.
  - Slider "Weer met charme" (0-100, default 70) in Vadsrijk-menu > Instellingen (`config/guhs-vadsrijk-ritme.toml`). It is
    multiplied by the sfeer level (VOL 1, ZACHT 0.45, UIT 0).
- **Town lights at dusk**: fix-sfeer's `Lichten` (the glow-layer wave with halos and horn) now also scans the other towns and
  starts each town's wave from its own square, through the hook `SfeerClient.U_STADPLEIN`. Honingtak is excluded because it has
  its own zone-3 evening glow. No blocks are toggled; this is the allowed glow-layer / emissive-prop option.
- **`Knuffelmeter`** (U-HUD hook `Emotes.NA_EMOTE`): up to 4 GuhEntities within 7 blocks react with a small delay each:
  - zwaai: wave back;
  - knuffel: the nearest within 3.5 blocks hugs back with hearts (and U-HUD's "you hug the air" is then skipped); the others get
    hearts;
  - dans: dance along;
  - juich: VAHOEG;
  - lach: ROLLEN;
  - hartje: VERLEGEN (blush) with hearts;
  - slaap: yawn;
  - wijs: look where you point.
  - Sleeping guhs sleep on.
  - Counting: friendly emotes count once per guh per day per town (key `npc:<id>` or the UUID); tame pets don't count.
  - Rewards per town: 5 = 25 brons, 15 = 100 brons, 25 = 200 brons + the first time title **Knuffelvriend**.
  - Rewards in total: 50 = HEART_GLASSES by `economie.Post`, 120 = title **Grote Knuffelaar** + 500 brons.
  - The titles are real `feature.titels.Titels` (Guhdex tab, behind your name).
  - Shown in the Karakter tab (`MenuServer.KARAKTER_EXTRA`, 2 lines) and on every newspaper.
  - Data: `SpelerData.extra("uritme")`, so per character.
- **`Pluizenblaadje`**: a board per town (8 towns, not Knuffelkamp) next to its quest board: a birch sign display at 1.4x, a title, a
  sub line and an Interaction. A click opens the talking screen (Praat) with pages:
  - the headline;
  - a town item (6 per town);
  - a general item (16, filled with a real citizen, an object and a number);
  - yesterday: the top hugger, the slimes cheered up in the zone (`Haken.VERSLAGEN`, in memory), or a quiet-day line;
  - the real forecast from WeerKlok (time + tomorrow's mist);
  - your knuffelmeter;
  - 2 chores.
  - Chores are ZOEK / BEZORG / HAAL. They are deterministic per town and day (the same for everybody), with progress per player in
    `extra("uritme").klus`, and reset when the day changes.
  - Only citizens with role null and no quest mention take part, so a chore click (`QuestMotor.KLIK_VOOR`, registered before the
    sleepy shopkeeper's) can never take over quest business.
  - A ZOEK spot sparkles only for that player, with "Warm" and "Heel warm" hints.
  - Reward: 20-50 brons + a knuffelmeter point.
  - Kept separate from the Bestelbord (crafting orders) and the Premiebord (bounties): no items, no materials, no mobs.
- Every spawned entity carries `guhs_vr_uritme`: a reload cancels the saved leftovers (`EntityJoinLevelEvent`, like kern's
  `Getallen.TAG`), and the owners respawn them while a player is near.

## Decisions I made myself
- **Quest guarantee = "quest NPCs never move".** Any NPC id mentioned anywhere in any quest stays 24/7. This is over-inclusive on
  purpose. The quest robot's assumption (clicking the live NPC at its spot) therefore holds; see the gametest.
- **No vanilla server rain.** Vanilla weather is dimension-wide (zone 6 desert rain), so the weather is client visuals from a
  shared timetable. The server uses the same timetable for the newspaper forecast.
- Rainbow only after the zone 2-4 showers (not after cloud or sand moments).
- Night market vendors and sleepers are RijkGuhs without an NPC id (never saved, invulnerable, not pushable). Their clicks are
  caught in `PlayerInteractEvent.EntityInteract` (HIGH).
- Board placed on a free floor cell 2-4 blocks from the quest board. Lampions hang 4 blocks above the market anchor. Nothing is
  built as blocks, so walkability and the connected-blocks rule are not affected.
- The knuffelmeter title does NOT replace the HUD class title (`Spelers.TITEL_BRON`): it is a normal Guhs title the player picks.

## Shared-file changes (add-only, marked `U-LEVEN-RITME`)
- `VadsrijkFeature` (+2: register + payloads), `client/VadsrijkClient` (+1), `tools/features/__init__.py` (+1).
- `npc/RijkGuh`: `public static Predicate<RijkGuh> STIL` (default false). When it is true, tick skips waving/idle emotes and
  SlenterGoal won't start.
- `hud/Emotes`: `public static final List<BiPredicate<ServerPlayer, Emote>> NA_EMOTE`, called after an emote starts. A true answer
  ("a guh hugged back") skips the "knuffel.niemand" line.
- `wereld/sfeer/client/SfeerClient`: hooks `U_REGENBOOG` (DoubleSupplier) and `U_STADPLEIN` (interface StadPlein).
- `wereld/sfeer/client/SfeerLucht`: rainbow = max(zone 1, `U_REGENBOOG`).
- `wereld/sfeer/client/Lichten`: in `interessant`, `scan`, and `golf` (horn from the other town's square, no temple beacon horn
  there).
- `menu/client/InstellingenPaneel`: +1 slider row "weer".
- lang nl_nl/en_us: +219 keys (`vadsrijk.uritme.*`, `gui.guhs.titels.naam|hint.uritme_*`, `vadsrijk.menu.instellingen.weer(.uitleg)`).

## Tests
- `runGameTestServer -Pgt=VadsrijkURitme` (batch `vadsrijk_u_ritme`), 4 tests:
  - `roosterBlokkeertNooitQuests`
  - `knuffelmeter`
  - `krantReset`
  - `stedenEnWeer`
- No client run (no AutoCheck), so the visuals are NOT checked by me. Look first at:
  - the sleeper position on a bed (`Bedden.Bed.x/y/z`);
  - the lampion ring height;
  - the board sign scale and rotation;
  - the particle amounts at 70;
  - the fog strength of the mist and the glow.

## Open points / resume
- Lights are a glow layer (fix-sfeer's choice). Real dark lamps by day would need generator exports.
- Zone 6 board and market anchors use reflection with fallback; check them after the u-z6wereld merge with
  `/guhs vadsrijk ritme status` in Saffraanpoort/Rozenkloof.
- The newspaper's "yesterday" counters live in memory, so after a server restart it shows a quiet-day line until the next day.
- u-leven-stad's street guhs (if they are GuhEntities) automatically react to emotes and count for the knuffelmeter in towns.
