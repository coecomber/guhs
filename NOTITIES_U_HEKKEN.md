# NOTITIES_U_HEKKEN: connected fences, walls, bars, chains, panes and garlands (round U)

## USAGE OF THE SHARED PASS (for u-deco / u-overgang / u-z5, read this first)

- **`tools/vadsrijk/verbind.py`** is the central connection pass. It runs on the WRITTEN tiles, with a 4-block halo
  from the neighbour tiles.
  - **You do not have to call it:** `maak_kaart.py` runs it at the end of every run (`--zone 1..5|alle`, also
    `--alleen`) on every tile written during that run.
  - `zone3/maak3.py`, `zone4/maak4.py` and `zone5/maak5.py`, when started directly, run it themselves.
- **In your generator, just write the block.**
  - `minecraft:spruce_fence`, `guhs:vlaggetjes[axis=x]`, `minecraft:iron_chain` are all fine: the pass sets
    north/east/south/west, wall up/low/tall, chain axis and garland axis.
  - Diagonal steps in a line of 3 or more get a corner filled (on the void side first), and a 1-up step gets the lower
    post raised.
- **By hand:**
  - `python tools/vadsrijk/verbind.py --alleen 21,-9;22,-9` (or without `--alleen` = all tiles);
  - on a volume of your own: `verbind.verbind_vol(vol, pal, id_fn, rand=0)`.
- **Check:** `python tools/vadsrijk/audit/verbindcheck.py` writes `AUDIT_VERBIND.md`. It is part of `maak_kaart --audit`
  and must be about 0.
- **Rules for future generators:** `tools/vadsrijk/LEESMIJ_BOUWREGELS.md`.
- **Merging:**
  - `maak_kaart.py`: `main()` became a thin wrapper around `_main()` plus the pass. The walkability audit moved to after
    the pass.
  - `maak3/4/5.py`: 3 lines in `__main__`.
  - All changes are marked `U-HEKKEN`.

## What and why

- **The root cause:** `KaartFeature` places the tiles with `setKnownShape(true)`, so Minecraft never recomputes
  connection states. Many generators wrote fences without states (`minecraft:spruce_fence` = all sides false), wrote
  garlands with the wrong axis, or drew rings with diagonal steps. The result was loose posts.
- **The fix:**
  - The pass runs over the written tiles, which covers every zone and every future generator. It is idempotent and
    rewrites only tiles that change.
  - It runs in parallel rounds. A worker writes `<tile>.verbind`, and the main process renames after each round, so
    nobody reads a half-written neighbour.
  - Tiles whose shared edge changed are redone in the next round.

## Built

| Part | What |
|---|---|
| `tools/vadsrijk/verbind.py` | `verbind_vol` (the core on a volume), `naloop`/`na`/`na_sinds` (the pass over tiles, parallel, halo 4) |
| `tools/vadsrijk/audit/verbindcheck.py` | Writes `AUDIT_VERBIND.md`, with the kinds `los`/`fout`/`as`/`gat`. It is hooked into `maak_kaart --audit`. |
| `tools/vadsrijk/LEESMIJ_BOUWREGELS.md` | The rule for all future generators. |
| `kamp/portaaltempel_wereld.py` | The separate portaaltempel.nbt now also goes through `verbind_vol`. |

**Java: `feature/vadsrijk/verbind/Verbind.java`.**
- What it offers:
  - `vorm(level, pos, state)` = `Block.updateFromNeighbourShapes` for fences, panes, bars, walls and gates;
  - `buren(level, pos)`;
  - `zet(level, pos, state, flags)`;
  - `gebied(level, a, b)`.
- Where it is used (all small, marked `U-HEKKEN`):
  - `bazen/Herstel.zet` + `terug` (flag 16, so the neighbours are re-shaped by hand);
  - `bazen/BazenBouw.zet`;
  - `zone2/bazen/Z2Bouw.zet`;
  - `zone5/wezens/Z5Bouw.zet`/`eigen`;
  - `karakter/RustendeRidders.zet`;
  - `puzzels/PuzzelStempel.stempel`: a `gebied` pass over the puzzle box.
- The Z5 proefarena fence ring is now 4-connected: an outer corner at each diagonal step.
- Gametest: `VadsrijkVerbindGameTests`, 2 tests, run with `-Pgt=VadsrijkVerbind`.

## Decisions made myself

**Walls:**
- `tall` under a block with a full bottom face, or under a wall connected that way.
- `up` follows vanilla `shouldRaisePost`. Simplification: anything standing on a straight wall raises the post
  (lantern, torch, ...). In vanilla that is "covers the post test", and for practically every block it gives the same
  result.

**Chains:** the axis follows the chain neighbours. Without neighbours, the axis stays as written.

**Garlands** (vlaggetjes, feestslingers): the axis follows the garland neighbours. Without neighbours, the axis follows
the side where there are blocks (the posts).

**Gap fill:**
- **Diagonal steps:** a corner is filled only for lines (the pair has at least 1 other line neighbour, so diagonal lamp
  posts stay separate).
- **The corner is NEVER placed on a standable floor** (planks, slab, stair, ...), so it never ends up on a path. With
  floor on both sides, the diagonal step stays.
- **Doorways stay open:** a 1-wide opening between two line pieces is never closed.
- **Steps of 1 up:** the lower post is raised by one. This only stacks on an existing post, so it costs no walking room.

**Hollowed blocks:** these are never next to a fence (a fence is not opaque), so "air" next to a line piece is really air.

**Pass over the written tiles instead of inside each generator:** this covers every zone and every future generator with
one hook, and tile borders get correct neighbours.

**maak_kaart:** the walkability audit (`audit.py`) now runs AFTER the pass, at the end of `main()` (for `--zone 1`/`alle`).

## Shared-file changes

All of these are marked `U-HEKKEN`:
- `maak_kaart.py`;
- `zone3/maak3.py`, `zone4/maak4.py`, `zone5/maak5.py` (`__main__` only);
- `kamp/portaaltempel_wereld.py`;
- the 6 Java placement helpers listed above.

## Pitfalls noted

- **Shared scratchpad:** sibling agents share the scratchpad folder. A `gt.log` there was overwritten by u-karakter's
  gradle output, so use a sub-folder per slice.
- **Config cache:** `.gradle/configuration-cache` may need deleting when the gametest log shows another worktree's
  `cwd`.
- **Tile lists starting with '-':** `verbind.na` passes `--alleen=...`, because argparse saw the tile list `-1,1` as an
  option. Fixed in a215bb28a.

## Results (full regen `maak_kaart --zone alle --geen-cache --audit`, 2026-11-06 ~13:50)

**The pass:** 187 tiles rewritten, about 19.7k blocks changed.

**verbindcheck:**
- before (t-int tiles): 24,136 open (los 14,684, fout 4,973, as 145, gat 4,334);
- after: **0**. A second run of the pass changes 0, so it is idempotent.

**Zone 3, measured on a fresh maak3:**

| Check | Without the pass | With the pass |
|---|---|---|
| loopcheck3 PAD | 396 | 396 |
| loopcheck3 VAL | 1 | 1 |
| poicheck3 | 0/430 | 0/430 |

This is after the fix for u-deco's report: the fills used to land on the Rijpad and at the lift door `lift_kapel_boven`.

**Walkability audit (AUDIT_VADSRIJK) against t-int:**
- POI, stap, water, boom and gras are equal or better.
- **Open point:** pits went from 4 to 5. The new one is the hat platform at `-374 103 82` (`hoedenpad_netwerk.hoeden.7` / `hoedenpad_wezen_4`).
  - In t-int that platform was ALREADY "niet bereikbaar vanaf het netwerk" (the hat path has no real entrance). Its fence
    ring had diagonal gaps you could squeeze through.
  - The pass closes those gaps over the void, so the audit now calls it "je komt er niet meer weg".
  - Fix belongs in the hat-path generator (`wereld/hoedenpaden.py`): give the platform a real entrance in the ring. I
    did not touch it.

**Gametest:** `-Pgt=VadsrijkVerbind`: all required tests passed (13:36).

**Not committed:** the generated tiles, `AUDIT_VADSRIJK.md`, `veilige_plekken.json` and `AUDIT_VERBIND.md` (integration
regenerates them).
