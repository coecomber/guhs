# NOTITIES U-DUBBEL (round U, 2026-11-06)

Bug from the playtest (FEEDBACK_RONDE4): Kristalwacht Splinter (NPC `z2_wacht_dok`) stood 2x on one spot; 3 Kruinroofvogels in
their arena. Branch `110-u-dubbel` from eaee06d08.

## Root causes (checked against the 26.1.2 sources: PersistentEntitySectionManager / ServerLevel)

1. **Ghosts of unsaved entities.** `storeChunkSections` filters `EntityAccess::shouldBeSaved` and only unloads (setRemoved) the
   entities it saves. An entity that overrides `shouldBeSaved()` to false (22 Vadsrijk classes: RijkGuh NPCs, airships + boards,
   kites, Ouwe Dobber, MikaSpion, Grauwe Droom, figuranten, pets, critters, Vahoegnir...) is NEVER removed when its chunk
   unloads: it stays hidden in its entity section in memory and comes back as soon as the chunk is visible again. Whatever the
   manager spawned in the meantime stands next to it. (Note: `EntityType.Builder.noSave()` types are fine: they are still
   "shouldBeSaved" and get unloaded; only the method override causes ghosts.)
2. **The load window.** `isLoaded/hasChunkAt` is true as soon as the chunk is FULL, but its entities are only visible later:
   saved ones load asynchronously from disk (status PENDING), and the visibility switch (HIDDEN -> TRACKED/TICKING) is scheduled
   on the main-thread executor. A manager that looks in that window (`getEntity(uuid)` / `getEntitiesOfClass` both only see
   visible entities) concludes "missing" and spawns a twin. Arenas: boss saved -> every reload of the arena chunk with a player
   nearby could add one (3 Kruinroofvogels = 2 reloads; flying in creative makes this frequent). Npcs: the ghost (1) + this
   window (once-a-second keeper landing in it) = a second Splinter.
3. **No uniqueness.** Nothing tied a spawned boss/NPC to "the one of X"; `Arenas.zoek` returned the first boss in the box and
   never removed extras; a flying boss outside the box was not found at all.

Not the cause: the dev client + integrated server (one server, one set of statics); several slice managers spawning the same NPC
(all NPC defs go through the one `Npcs` keeper, keyed by id); arena reset (it discards first).

## What I built (package `feature/vadsrijk/dubbel/`)

- `Dubbel`:
  - `klaar(sl, pos)` = `areEntitiesActuallyLoadedAndTicking(chunk)`: the test a manager must use before "missing -> spawn".
  - `merk(e, key)` puts tag `guhs_vr_uniek=<key>` (saved with the entity). `sleutel(e)` reads it; for entities saved before
    U-DUBBEL it derives `arena:<id>` (boss of a registered arena) or `spawner:<id>` (mob of a spawner with max 1 = area leaders).
  - Join guard (EntityJoinLevelEvent, LOWEST): per level one entity per key. A copy loaded from disk while the key is alive is
    refused; a FRESH spawn replaces (discards) the old one (the manager decided the old one is gone, e.g. a hidden leftover).
  - Ghost sweeper (EntityLeaveLevelEvent + ServerTickEvent.Post): an unsaved Vadsrijk (or keyed) entity that goes hidden without
    being removed is discarded at the end of the tick if still hidden. Not when a player rides it.
  - Every 10 s: keyed doubles tidied in every level (`opruimen(sl,false)`).
  - `scan(sl)`: keyed groups (strict) + heuristic groups (NPC guh with the same npc id; named non-mob Vadsrijk entity with same
    type+name within 2 blocks).
- `DubbelRegistry`: events + `/guhs vadsrijk dubbel scan | opruimen | stats`.
- `DubbelGameTests`: `eenPerSleutel` (key rules, disk copy refused, fresh replaces, legacy arena key, scan + opruimen) and
  `arenaHerladenEenBaas` (a test arena far out in the Vadsrijk, 3x3 chunks forced/unforced 4 times until really unloaded;
  exactly one boss after every load, an untagged disk copy is refused, an unsaved guh does not come back as a ghost).

## Shared-file changes (all marked `U-DUBBEL`)

- `VadsrijkFeature`: one register line.
- `kern/baas/Arenas`: `klaar` gate before judging/spawning; boss gets key `arena:<id>`; `zoek` also via the key (flying bosses
  outside the box) and discards extra bosses of one arena (not those with a target); add-only `afmelden(id)` + `sleutel(id)`.
- `kern/mob/Spawners`: `klaar` instead of `isLoaded` for the spawner centre and for each spawn spot.
- `npc/Npcs`: `klaar` gate in the keeper; NPC guh gets key `npc:<id>`; `levend` adopts the keyed one; add-only `sleutel(id)`.
  (Covers zone 1-5 + Ambacht NPCs: they all register through `Npcs.voegToe`.)
- `luchtschip/Luchtschepen`: `klaar` instead of `hasChunkAt` (ships + departure boards); keys `luchtschip:<lijn>:<k>`,
  `luchtschip_bord:<halte>:<i>`.
- `zone4/dieren/Z4DierenRegistry`: keys `z4:ouwe_dobber`, `z4:vlieger:<i>` (already gated on entity ticking).
- `zone5/wereld/Z5Bewoners`, `zone5/wolkwal/WolkwalBeweging`: `klaar` gate; keys `z5:bewoner:<i>`, `z5:wolkwal:<id>`.
- `vriendjes/RozeDraakBeheer`: `klaar` gate; nest dragon key `draak:nest`.
- `karakter/RustendeRidders`: `klaar` gate before the statues are wiped + rebuilt (saved armour stands were not back yet).
- `huisdieren/Huisdieren`: key `huisdier:<player>` (one pet per player); `economie/Rijdieren`: key `rijdier:<player>`.
- `bazen/mika/MikaSpion`, `zone2/bazen/boor/Boormeester` (loose named bosses): `klaar` gate; keys `baas:mika_spion`,
  `baas:boormeester`; Boormeester's search discards an old twin.

## Audit zones 1-5 (unique spawns)

| What | Manager | Status |
|---|---|---|
| All NPCs z1-z5 + Ambacht + kampwachten/Kristalwacht | `Npcs` keeper | fixed (gate + key + sweeper) |
| Arena bosses z1 Klodderreus/Koning Klodder/Opperzwam, z2 Galmwolf/Spiegelzucht, z3 9 arenas (Kruinroofvogel...), z4 Zeeslurf/Wrak, z5 bosses, Mika-imker | `Arenas` | fixed (gate + key + extras removed) |
| Area leaders z3 (spawner max 1) | `Spawners` | fixed (gate + derived key) |
| Pool mobs incl. Sparringridder, elites | `Spawners` | gate; the existing over-cap trim already removes extras |
| Mika-spion, Boormeester | own `zoek` | fixed |
| Vahoegnir | own dedup (`alleVahoegnirs`) | already OK + sweeper |
| Ouwe Dobber, kites | Z4DierenRegistry | keys + sweeper (was already gated on entity ticking) |
| Airships + departure boards | Luchtschepen | fixed |
| Z5Bewoners (incl. Wolkwacht knights), Wolkwal pieces | own maps | fixed |
| Pets, mounts | per player | keyed |
| Nest dragon | RozeDraakBeheer | fixed |
| Grauwe Droom, Karavaan, critters, Z3Leven riders, figuranten | own | ghosts handled by the sweeper; not keyed (not unique per spot / short-lived) |
| Arena respawn boards (holograms) | Arenas/Getallen | already OK (disk copies cancelled by tag in KernEvents) |

## Tests

- `runGameTestServer -Pgt=DubbelGameTests`: **all 3 required tests passed** (2026-11-06).
- The gametest server has NO Vadsrijk dimension, so `arenaHerladenEenBaas` falls back to a small keeper in the test level that
  follows the same rules as `Arenas` (klaar gate, find by key, spawn + key); with the dimension it uses kern's real Arenas. The
  run logged "load window seen 32 time(s)" over 3 reload rounds: the window where the blocks are loaded but the entities are
  not is real and frequent (that is where the old code spawned the twin). Ghost check (unsaved guh gone after unload) passed.

## Open points

- Grauwe Droom, Karavaan, critters and Z3Leven riders are not keyed (the sweeper removes their ghosts; their managers keep
  references). Key them if a double shows up there.
- A boss whose chunk is not entity-ticking (beyond the simulation distance) is not spawned until a player is closer; NPC spots
  between simulation distance and 96 blocks likewise. Intended (that is exactly the window), but noted.
