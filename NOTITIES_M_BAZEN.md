# NOTITIES_M_BAZEN: zone-3 boss playtest fixes (branch 110-m-bazen, from e5ab2935e = 110-k-int)

Everything goes by POI id / arena id (`Z3WereldPlekken` via `Z3BazenPlekken.w`), no new hard-coded coordinates: survives the zone-3
map revamp.

## 1. Graaf Piepvel's cape = mouse pelts
- `tools/features/vadsrijk_z3_bazen_modellen.py`: `_pels()` paints a patchwork of flattened pieppiepmuisje winter coats (the `piep`
  mouse look: plum/grey fur, cream belly, big round pink-lined ears, pink nose + paws, curly pink tail), one pelt per patch, patch
  grounds darker so each pelt reads, cream running stitches on every seam. `_muisjes_aan_cape()` adds 3D ears (pink inside) on the
  outside of cape_1/2/3 + the collar, and pink tails trailing behind the train / out of the side hems. The ears/tails live in the
  panel bones, so a panel the mice gnaw off in fase 2 takes its ears/tails with it. Sock variant unchanged.
- Regenerated: `vr_z3_baas_piepvel.geo.json`, `piepvel.png`, the Blockbench copy. Software render (wiki_renders):
  scratchpad `mbazen/piepvel_render.png` (front, 3/4, side, back with 2 panels hidden). Not seen in a client.

## 2. Kaaszeppelinwerf intro theft
Cause: the 3 deck arenas overlapped (kern arenas were cylinders |dy| < 24; Brokkel/Roquefort/Taleggio are 26-29 apart horizontally,
radii 15/19/15, decks 15 apart in height). Walking at a deck edge = in two arenas = two intros/fights.
- **kern (small, backwards compatible):** `Arena` got a height window `onder/boven` (old 7-arg constructor = 24/24), `met()`,
  `overlapt()`. `VadsrijkBaas.magIntro(p)` (default true); `Arenas.tick` only counts a player as inside (intro, bar, scaling) when
  the boss says yes, and asks again next tick.
- **Layout:** dungeon arenas (werf + hart) get a 6-below/14-above window; `Z3BazenPlekken.indeel` makes all zone-3 arenas disjoint
  (split halfway between floors when >= 6 apart in height, else shrink both radii to a 2-block gap, min r 8, logged). Result now:
  Roquefort y 105..118, Brokkel 120..140, Taleggio 135..155 (the wreck stays inside Roquefort's). `Z3BazenRegistry.ARENAS` = the
  registered layout.
- **`Z3Toegang`:** one fight at a time (intro = you're in that boss's fight while it lives and you're in its arena + 10 s grace;
  other zone-3 bosses don't greet/target you or take your hits, overlay "Je bent nog in gevecht met ...") and the dungeon order
  (Taleggio -> Brokkel -> Roquefort, Wortelwurger -> Nestwachter -> Bleu: the next one wakes when the previous is beaten by you within
  60 min, or is down right now, or you are creative; overlay "X laat zich nog niet zien. Versla eerst Y."). In-memory (a restart =
  the "down right now" rule still works through ArenaData's respawn timers).
- POI_VERZOEKEN_Z3.md: section m-bazen asks the revamp for r1+r2+4 spacing (or 16 height) and the walking order = fight order.

## 3. Hart van de Boom: no boss at `tp hart`
Not quest-gated and not a bug: `tp hart` = `hart_poort` in the Hartslag-zaal (y 13); the bosses are in the ring at floor 50, r 95
round the trunk, ~150 blocks walk (root passage up, Aderloop, Kwekerij gate, Ringenzaal gate). Made clear:
- At the gate (r 10, once per 5 min): chat with the route + the three bosses; without the story (`Z3BazenApi.HART_VERHAAL`, wired in
  `Z3Koppelingen` = hq13 active/done or creative) also "De wortels slapen nog... Kom terug als Wortelwacht Gloei je naar het Hart
  stuurt..." (fighting is allowed, the story just doesn't count yet: hq13's goals are `meld` goals).
- `tp hart` prints the boss list + coordinates; `tp <baas>` opens the order; new `/guhs vadsrijk z3bazen spawn <baas>` (all 9;
  resets the arena now = full HP, tp to its board, order opened); `/guhs vadsrijk z3bazen arenas` lists every arena (window,
  awake/empty, predecessor).

## 4. Vahoegnir "Hij is er al" but nowhere
The old command only called him (he lands only while the nest is loaded) and never took you there.
- `vahoegnir [herinnering]`: calls him if needed and ALWAYS takes you to `nest_rand`; if he exists it prints where he is (pos, fase,
  Vads, "ZONDER arena" for an orphan). `vahoegnir waar`, `vahoegnir reset` (discards every Vahoegnir in the loaded level incl.
  orphans, clears a pending landing, nest branches restored, arena reset).
- `Vahoegnir.bewaak` (every 20 ticks): wandered > r+30 from the nest, > 30 below / 40 above it, near the void, or stuck in blocks
  (not while flying) = back to the first free spot above the nest centre (logged).

## Tests
`runGameTestServer -Pgt=VadsrijkZ3Toegang` (new class `VadsrijkZ3ToegangGameTests`, 4 tests): arenas disjoint + layout rule,
intro isolation + order + creative + dev override, Hart gate hint + spawn order, Vahoegnir dev tp / back to nest / reset (in the real
Vadsrijk dimension when the test server has it). Result: see the log below.

## Docs
SPIEKBRIEF.md section **Z4b. Bazen-testgids**.

## Log
- compileJava OK (slot script, GUHS_SLOTS=3; build/moddev/artifacts copied from k-int).
- Run 1 `-Pgt=VadsrijkZ3Toegang,VadsrijkZ3Bazen`: 9/10; `vahoegnirSplinters` failed because the new nest watch moved the test's
  Vahoegnir (spawned in the test level) to the nest coordinates. Fix: the watch only runs in the Vadsrijk level.
- Run 2: **10/10 passed**. The Vahoegnir test took its no-Vadsrijk-dimension branch (orphan found + reset); the real-nest path (dev
  tp, landing, back to the nest) is compiled but not exercised by a test on this test server.
- No AutoCheck (memory); the cape is judged from the software render only.

## Open
- The user should look at the cape in the dev client (Pelszaal, `spawn piepvel`).
- The Vahoegnir nest path in a real world: `vahoegnir`, `vahoegnir waar`, `vahoegnir reset`.
- Integration: kern `Arena` now has 9 components (old constructor kept); anyone deconstructing the record would need to update.
