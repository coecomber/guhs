# NOTITIES Y-VITRINE (round Y, 2026-11-10)

Branch `110-y-vitrine` from 65cd32467 (110-x-int). Slice: the **Vadsrijk-vitrine**, one trophy block for the survival base that shows the owner's Vadsrijk character live. The cheat sheet is in `SPIEK_Y_VITRINE.md`.

## For y-ftb / integration: the reward
- **Item id: `guhs:vr_vitrine`** (stack size 1, epic rarity). Wire it as the reward of the FTB quest **"Word rekruut van De Knuffelorde"** (chapter "Het Vadsrijk"): `{"type":"item","item":{"id":"guhs:vr_vitrine","count":1}}`.
- There is no recipe on purpose. Other ways to get it: creative tab Guhs, `/give @s guhs:vr_vitrine`, `/guhs vadsrijk vitrine geef` (op).
- The FTB claim rule (never claim inside Het Vadsrijk) belongs to y-ftb/y-scheiding. The block refuses to be placed there anyway.

## What I built
Package `feature/vadsrijk/vitrine/`:
- **`Vitrine`**: registry (block, item, block entity, creative tab), all logic and the dev command.
  - `magHier(level)`: everywhere except Het Vadsrijk and Het Knuffelkwartier (the housing dimension of the Vadsrijk).
  - `foto(player)`: the snapshot. It holds: name, active class + its spec, generic level, class level, zones done (bit mask), raid clears (`extra("z6raid").clears`), date of the first completed zone, and the statue (mount/pet + colour + look).
  - `samenvatting(...)`: the chat lines on click.
  - `bordRegels(...)` / `badges(...)`: the panel lines, in common code so the gametests can check them.
- **`VitrineBlock`**: a BaseEntityBlock with FACING, shape 1..15 × 0..16, glass sound and light level 6 (the subtle glow).
  - `getStateForPlacement` returns null outside `magHier` and shows a message above the hotbar.
  - `setPlacedBy` makes the placer the owner.
  - Clicking sends a chat summary to whoever clicks. With a block in hand you place that block against the case as usual; with any other item (or an empty hand) you get the summary.
- **`VitrineBlockEntity`**: owner uuid + snapshot + the zone gate it was last synced with.
  - Saved with the chunk; that is the offline cache.
  - Synced with the vanilla BE update packet.
  - Server ticker: every 100 ticks, staggered by position, it re-reads the owner only if he is online. It compares and sends only on a change. A changed gate (`ZonePoort.open()`) is also noticed while the owner is offline.
- **`client/VitrineRenderer`** (BER), cheap:
  - The statue is a client-only copy of the mount/pet entity (pattern from `BlubKnuffelRenderer`). It is made once per vitrine and rebuilt only when the statue in the snapshot changes. It is scaled to fit the glass (0.56 high / 0.62 wide) and turns slowly. No shadow, no name tag.
  - Fallback: the Knuffelorde emblem. This is an item model only (`guhs:vr_vitrine_embleem`), drawn from a paper stack with the `item_model` component, so no extra item is registered.
  - Panel: billboard text above the case, only within 14 blocks. Lines are rebuilt only when the snapshot or gate changes.

### Decisions
- **"Zone completed"** = every MAIN quest (`Quest.hoofd()`) of that zone handed in (`ZoneBoeken.zoneVan`; the raid's `z6raid*` quests do not count).
- **The statue** is picked in this order:
  1. the active mount (`Rijdieren.actief`), if it is a land mount;
  2. else the owned land mount with the highest level;
  3. else the active pet (`Huisdieren.actief`);
  4. else the emblem.

  The Zeilbootje (water) is skipped because it has its own boat entity. Pluimguh colour and coat/pattern/tack (`Aankleden.uiterlijk`) are copied.
- **Class line** = the ACTIVE class (`SpelerData.klasse`) + its spec + the GENERIC level (the HUD level).
- **"Date of the first completed zone"**: quests store no timestamp. The date is kept in the player data slice `extra("vitrine").eerste` (ms) and set the first time the game notices a completed zone.
  - The game checks in two places: every snapshot, and a cheap check every 600 ticks for online players WITHOUT a date (only zone 1 is checked there).
  - Players who finished zone 1 before this feature get the date of the first check after the update. Integration can live with that or backfill it.
- **Zone gate:** the gate travels with the block entity (`open`).
  - Zones above it show as "?" badges on the panel and as "? Zone n: binnenkort" in the summary, never by name. That holds even if an op has completed them.
  - The raid line only appears at gate 6.
  - Raising the gate re-syncs every loaded vitrine within 5 s.
- **Display only:** the snapshot contains no items and no inventory. Nothing can be taken out or put in. A vitrine without an owner (from /setblock) is claimed by the first click.
- Anyone can break it (normal block, drops itself, hardness 1.5). Base protection is the server's claim system.

### Dev command (op)
`/guhs vadsrijk vitrine` [status], `geef`, `zone <1-6>` (hands in every main quest of that zone, as `zones zone1klaar` does), `raid <n>`, `ververs` (every loaded vitrine now), `wisdatum`.

## Assets + texts
- Generator `tools/features/vadsrijk_y_vitrine.py` (registered in `tools/features/__init__.py` after `vadsrijk_x_zone1`). It produces:
  - the block model (`render_type` translucent): pedestal, gold rim, velvet stand, gold posts, glass sides + top, front plaque;
  - 5 block textures (`textures/block/vadsrijk/vitrine/`) and the 32×32 emblem (`textures/item/vadsrijk/vitrine/embleem.png`);
  - the blockstate, the item models and the loot table;
  - 21 texts.
- Lang: Dutch + English written into nl_nl/en_us. English overlay chunk **`tools/lang/en/c460_vr_y_vitrine.json`** (+ a chunk entry in `tools/lang/chunks.json`, prefixes `vadsrijk.vitrine.` and `block.guhs.vr_vitrine`). `apply_en.py --no-ftb`: 0 still Dutch.
- Not done: the `tools/vadsrijk/engels_bron.json` hashes (engelscheck). Integration refreshes them with the other slices.
- Item name via `useBlockDescriptionPrefix` (block.guhs.vr_vitrine), tooltip `block.guhs.vr_vitrine.tip/.tip2`.

## Shared-file changes (all one line, marked Y-VITRINE)
- `feature/vadsrijk/VadsrijkFeature.java`: `Vitrine.register(modBus)` after ZonePoortRegistry.
- `feature/vadsrijk/client/VadsrijkClient.java`: `VitrineClient.init(modBus)` after QuestLopenClient.
- `tools/features/__init__.py`: `'vadsrijk_y_vitrine'` after `'vadsrijk_x_zone1'`.
- `tools/lang/chunks.json`: one appended chunk `c460_vr_y_vitrine`.
- `src/main/resources/assets/guhs/lang/nl_nl.json` + `en_us.json`: +21 keys each.
- Wiki (`blokuitleg.py`, "every interactive block needs an entry"): NOT added. It is the Guhs wiki tool, not in this tree's scope. Integration: add `vr_vitrine` there ("Klik: overzicht van de Vadsrijk-held van de eigenaar").

## Tests
`runGameTestServer -Pgt=VadsrijkVitrine`, class `vitrine/VadsrijkVitrineGameTests` (4 tests):
- `alleenBuitenHetVadsrijk`: placing with the item works in the overworld (the placer is the owner, the first snapshot comes right away). It is refused "in Het Vadsrijk": the gametest server has NO Vadsrijk dimension, so `Vitrine.TEST_RIJK` lets the test level stand in, and the full BlockItem.place path is refused with no block placed. When the real Vadsrijk/Kwartier levels exist, `magHier` is asserted false for them.
- `fotoVolgtVoortgang`: level 20 + zone 1 + Knuffelhert + 2 raid clears end up in the snapshot. Zone 2 is added; the date stays that of the first zone; no change means nothing is re-sent; at gate 6 the summary names the zones and shows the raid.
- `offlineCache`: offline the snapshot stays (and survives save/load); the summary says "laatste keer"; back online it catches up.
- `poortEenGeenSpoilers`: gate 1, even with everything done: zones 2-6 are never named on the panel or in the chat, five "?" badges, no raid line. Raising the gate to 6 with the owner offline re-syncs and shows the raid line.

Result 2026-11-10: **All 5 required tests passed** (4 vitrine tests + the always-on one); log `guhs_vadsrijk/y-vitrine-gt2.log`. Run 1 failed on two test mistakes, now fixed: the quest rewards give xp, so the level is set after the quests; and there is no Vadsrijk dimension in the test server.

## Open points
- The FTB reward wiring itself (y-ftb / integration): item id above.
- Client look (the user judges it in the dev client):
  - glass translucency against the statue;
  - the statue size per mount (it fits the bounding box; GeckoLib models that stick out of their box may poke through the glass);
  - panel height with 5 lines.
- Wiki blokuitleg entry and the engels_bron hashes (integration).
