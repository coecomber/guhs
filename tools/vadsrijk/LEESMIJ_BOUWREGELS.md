# LEESMIJ_BOUWREGELS: rules for every Vadsrijk map generator

These rules hold for every generator that writes map tiles (`src/main/resources/data/guhs/structure/vadsrijk/tegels/`),
for zones 1 to 5 and every future zone or kit.

## 1. Connecting blocks are always written connected (user rule, U-HEKKEN 2026-11-06)

**The rule:** fences, walls, iron bars, glass panes, chains and Guhs line blocks (vlaggetjes, feestslingers, vadstouw,
guh fences, mod walls, `*_ruit`, `*_tralies`) must render in the game as connected lines, never as loose posts.

**Why the game does not fix it:** `KaartFeature` places the tiles with `setKnownShape(true)`. Minecraft then takes every
written blockstate literally and never recomputes north/east/south/west. A fence written as `minecraft:spruce_fence`
(all sides false) stays a loose post.

**How it is guaranteed:** the shared connection pass `tools/vadsrijk/verbind.py` runs automatically after the tiles are
written:
- `maak_kaart.py` (every `--zone`, also `--alleen`) runs it once at the end on every tile written during that run.
- `zone3/maak3.py`, `zone4/maak4.py` and `zone5/maak5.py`, when run directly, run it themselves (`verbind.na_sinds`).
- It works on the written tiles with a 4-block halo from the neighbour tiles, so tile borders are correct too.
- It is idempotent, so a second run changes nothing.

**What it does:** it uses the vanilla rules.

| Block kind | Connects to |
|---|---|
| fences | fences of the same family (`#wooden_fences` / other `#fences`), fence gates in line, sturdy full faces (never leaves, barrier, pumpkin, melon or shulker boxes) |
| walls | walls, panes/bars, gates in line, sturdy faces |

Walls also get their side heights (`low`, or `tall` under a covering block) and `up` the way `WallBlock` computes them.

| Block kind | Connects to / gets |
|---|---|
| panes and bars | panes/bars, walls, sturdy faces |
| chains and `vadstouw` | the axis along their chain neighbours |
| `vlaggetjes` and `feestslingers` | the horizontal axis along their garland, or between the posts they hang from |

**Gaps:**
- A line of at least 3 pieces with a diagonal step gets the corner filled, on the outside of the curve and preferably
  over the void, so the walkway stays free.
- A step of 1 up gets the lower post raised by one block.
- Two lone posts are never joined.

**Sturdy faces:** these come from the block models. The worktree assets are read first, then the asset jar. A new mod
block with a full-cube model is handled automatically.

### What a generator author must do

1. **Nothing special for tiles.** Write the block in any state (`minecraft:oak_fence` is fine) and the pass sets the
   states. Writing correct states yourself is fine too: the pass recomputes them anyway.
2. **Draw lines orthogonally where you can.** A ring or curve is fine because the pass fills the diagonal steps. But
   decide which side is the walkway: the pass fills the side without floor first.
3. **New connecting mod block?**
   - Fences go in `data/minecraft/tags/block/fences.json`, plus `wooden_fences.json` if it is wood.
   - Walls go in `walls.json`.
   - Panes end in `_ruit` or `_tralies`.
   - Chain-like blocks and garlands go in `Regels.soort` in `verbind.py`.
   - Gates go in `fence_gates.json`.
4. **Something you write outside the tile folder** (a separate `.nbt` structure, or a scene used by a renderer): run
   `verbind.verbind_vol(vol, pal, id_fn)` on its volume before writing. The kit scenes already call
   `S.fix_connections()`.
5. **Blocks placed at runtime by Java** (puzzles, halls, camp pieces) must use the connected state:
   - `Block.updateFromNeighbourShapes(state, level, pos)` before `setBlock`, or flag 3 with shape updates;
   - helper: `nl.juiced.guhs.feature.vadsrijk.kern.wereld.Verbind.zet(level, pos, state)`.

### Check

`python tools/vadsrijk/audit/verbindcheck.py` (also part of `maak_kaart --audit`) writes `AUDIT_VERBIND.md`. It counts:
- `los`: a block misses a connection;
- `fout`: a wrong state;
- `as`: a wrong axis;
- `gat`: an unbridged step.

After a regen, all four must be about 0.

Run the pass by hand with `python tools/vadsrijk/verbind.py [--alleen tx,tz;...]`.
