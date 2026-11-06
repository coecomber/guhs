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
