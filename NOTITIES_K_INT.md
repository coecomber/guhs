# NOTITIES_K_INT: Vadsrijk zone 3 "De Schemerwortel", integration (branch 110-k-int, worktree guhs_vadsrijk/k-int)

Base: 05196f3cc (110-j-int, the round-3 integration = the user's test client). One merge commit per branch. Never gradle in
j-int / h-melee / g-int / f-int / w-poc. build/moddev copied from j-int (artifacts only). Scratch: scratchpad/kint (logs,
lang3.py = 3-way lang merge, lang3m.py = the same with a synthetic base for criss-cross merges).

## Merged
| branch | head | notes |
|---|---|---|
| 110-z3-systemen | ca18781a8 | VadsrijkFeature payload lines: both; lang 3-way |
| 110-z3-wereld | d06c93935 | WereldLucht landmarks: z3-wereld's (tree 91, Hoogvads 116 behind it, volcano 148) instead of j-int's 75/105/140; i-lore's moon/finger constants kept |
| 110-z3-wezens | c9dffda70 | FEATURES: both; lang 3-way |
| 110-z3-npc | 53ca0312a | criss-cross (bases ca18781a8 + 5bbc7af29): lang via lang3m.py (base = ab6d952ac + systemen + voltooiing). Zone-2 cliffhanger: i-lore's version kept (Z2NpcPlekken comment, lang `z2_hq12_hartsplinter.klaar.*`), npc's generator change to vadsrijk_z2_npc.py was identical/absent |
| 110-z3-bazen | 0d1cbd618 | Vahoegje animation add/add: bazen's (theirs). One pet registration (bazen's `PET_VAHOEGJE`; npc's `VAHOEGJE_PET` points at it, npc's texture `textures/vadsrijk/zone3/vahoegje.png` + its generator writes removed). Pet texts = bazen's. Rijdieren.RIJDEN_IN_ARENA merged cleanly with i-mounts |

## Cross-cutting decisions (commit 166214cb8, class `zone3/npc/Z3Koppelingen`)
- **Opening:** z3-wereld's film (`Z3Reizen.opening`) is THE opening (`Z3Reizen.EERSTE_AANKOMST = null`, set explicitly at
  server start). hq01's "aangekomen" comes from standing in the Honingtak area, so it works with the wereld film. npc's
  `Z3Verhaal.opening` only runs if Z3Reizen were absent (never now).
- **Zone-2 cliffhanger east:** i-lore's (in j-int) kept; z3-npc's duplicate dropped in the merge.
- **Travel:** `luchtschipper` (De Haven) already offers `schemerwortel` (z3-wereld did it). hq01 starts at Kapitein Poolster
  (`z2_kapitein`, Kandijkom).
- **Bazen <-> npc:** `Z3BazenApi.MOMENT` -> `NpcApi.meld`: `baas:<id>` -> `z3_<id>:verslagen` (nestwachter -> opgevrolijkt,
  vahoegnir via its own moment), `piepvel_weg` -> `z3:flash_f1`, `nestwachter_gered`, `vahoegnir_opgevrolijkt` ->
  `z3_vahoegnir:opgevrolijkt`, `troonzaal_klaar` -> the bloom (`zone3_klaar`, `z3:finale`). Binding `z3_vahoegnir` ->
  `Z3BazenApi.startVahoegnir(p, false)`. `HEEFT_SPLINTERS` = hq14 active/done (or creative).
- **Flashes / finale:** bazen's scenes are the real ones (F1 arm, Vahoegnir ending + hatching + beam, F2 throne room). npc
  waits for them: F1 is skipped when the player saw `z3:f1_arm`; the npc finale is marked seen on `vahoegnir_opgevrolijkt` and
  the bloom waits for `troonzaal_klaar` (max 90 s); hq15's cliffhanger skips its own F2 when `z3:f2_troonzaal` was seen. The
  npc versions remain as a fallback for a player who was not in the room (bazen shows its scenes to players within 40-45).
- **Bleu's egg:** a decoy (prop in his room); the npc egg scene now starts with "Het ei bij Baron Bleu? Nep! Een kaaswiel met
  roze verf..." (`vadsrijk.z3npc.scene.ei.nep`).
- **Mika-imker:** NOT built as a boss (time). The secret quest ends in a lore scene: all 5 clues -> binding `z3_imker` (flag
  `z3_imker_open`) -> walk into `mika_imker_raatzaal` -> 6-line Praat scene (a Mika in a too-big beekeeper suit "tasting
  honey for the report", the smoke pot dies, he flees) -> `z3_imker:verslagen`. Book: imker counts by the quest.
- **Small bindings:** `z3_ring_aan` (a ring of flames round the trunk at the ring's height, per player), `z3_ketel_uit`
  (blossom puff), `z3_terugkleur` (a pink wave from the nest down the tree, ~6 s, per player).
- **Coordinates:** Z3WezensPlekken / Z3NpcPlekken / Z3BazenPlekken read `Z3WereldPlekken` directly (no reflection; the old
  numbers are only used for ids wereld lacks). All 43 bazen ids exist in wereld. `Z3Lagen.GEBIEDEN` is built from
  `Z3WereldPlekken.GEBIEDEN` (+ `z3_ring`), so areas follow the map. `Z3Wind.KOLOMMEN` was already pointed there by wereld.
- **Layered paths:** NOT registered in kern's 2D `Spawners.registreerPad` (wezens checks per height).
- **Book (zone-3 page):** lookouts (`Z3WereldPlekken.UITKIJKEN`) + campfires (`KAMPVUREN`) added; boss ids checked against
  bazen's registry (`vr_z3_baas_roquefort|kruinroofvogel|piepvel|bleu|vahoegnir` all exist; schimmelkolos dropped (a prop
  entity), imker = quest only). `ZoneBoeken.gebiedId` is 3D now (layered zone-3 areas), catch-all name `z3_ring` for zone 3.
- **Altars:** `bouw3` places `guhs:vr_z3_baas_altaar` at `werf_vahoeg_altaar` (on a 3x3 plank pad beside the werf entrance)
  and `hart_vahoeg_altaar` (was a chiseled stone placeholder).
- Generator fix: `vadsrijk_z3_npc.py` `_mijn_sleutel` dropped z3-wereld's `vadsrijk.npc.reis.kies.z3_kapitein` on a standalone
  run -> `vadsrijk.npc.reis.*` excluded.

## Log
- Compile after the 4 first merges: OK. After bazen + glue: OK.

## Resume point
(updated below)
