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
- A line of at least 3 pieces with a diagonal step gets the corner filled, on the outside of the curve.
- The pass NEVER puts a piece on a standable floor (a walkway): the corner goes over the void. A ground-level ring with
  floor on both sides keeps its diagonal step.
- A 1-wide doorway between two line pieces is never closed.
- A step of 1 up gets the lower post raised by one block.
- Two lone posts are never joined.

**Sturdy faces:** these come from the block models. The worktree assets are read first, then the asset jar. A new mod
block with a full-cube model is handled automatically.

### What a generator author must do

1. **Nothing special for tiles.** Write the block in any state (`minecraft:oak_fence` is fine) and the pass sets the
   states. Writing correct states yourself is fine too: the pass recomputes them anyway.
2. **Draw lines orthogonally (4-connected).** The pass can only bridge diagonal steps over the void. A fence ring on the
   ground must be drawn 4-connected by the generator itself.
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

## 2. Walking lines stay free, and everything must be reachable on foot (W-BEREIK, round W 2026-11-08)

**The rule (W_BRIEF walkability standard):** a survival player (1.8 high) must reach every NPC, quest spot, secret and arena on
foot: 2 air blocks per cell, 3 air above the lower floor for a 1-block step up, drops of at most 3 (any depth into water), no
lanterns/chains/rails in the walking space, no balustrades floating in the air, no wooden door against a wall (a fake door is an
IRON door).

**The shared pass** `tools/vadsrijk/vrijpad.py` runs in `maak_kaart.py` right after the connection pass (`vrijpad.na_sinds`), on
every tile written in that run. It removes lanterns/chains hanging into the walking space (the chain above becomes a hanging
lantern), lanterns/chains standing on stair steps or in open walkways, loose chains on floors and floating fences/walls/bars over
walkable floors. By hand: `python tools/vadsrijk/vrijpad.py [--alleen tx,tz;...]` (idempotent).

**The check** `python tools/vadsrijk/bereikcheck.py` (~3 min, ~2-3 GB; also part of `maak_kaart --audit`) floods the whole map from
the start spawn (airship lines, lifts, hatches, gondola as edges) and writes `BEREIK_RAPPORT.md`: per zone the reached / total
counts (main quest, side quest, NPC, secrets, arenas, spawners, puzzles, other spots) and every unreached target with the nearest
reached cell, arenas without an exit, airship boarding distances, one-way areas, doors, deco on walking lines.
- `--zone N` limits the details to one zone, `--punt x,y,z` explains one spot, `--strikt` exits 1 when a main-quest target or an
  NPC is not reached (for CI / integration).
- The targets come from the game code: `tools/vadsrijk/bereik_doelen.json`, dumped by the gametest
  `gradle_slot.sh <tree> runGameTestServer -Pgt=BereikDoelenGameTests` (every `*Plekken` field, NPCs, arenas, spawners,
  airship lines, puzzles, teleport links). Rerun it after changing NPC/POI/quest/link code, then rerun the check.
- Release bar: main-quest targets and NPCs 100 %, side quests and secrets ~99 %; explain every exception in the NOTITIES.
