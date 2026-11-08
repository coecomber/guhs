# AUDIT_VERBIND - connecting blocks in the Vadsrijk tiles

**GENERATED** by `tools/vadsrijk/audit/verbindcheck.py` (runs with `maak_kaart --audit`). Counts what the connection pass `tools/vadsrijk/verbind.py` would still change; after a regen everything must be ~0. Rules: `tools/vadsrijk/LEESMIJ_BOUWREGELS.md`.

Tiles: 1308. Total open: **8**.

| kind | count | meaning |
|---|---|---|
| los | 0 | fence/wall/pane misses a connection to a connectable neighbour (loose post) |
| fout | 8 | wrong state the other way (connected to nothing, wall height/post) |
| as | 0 | chain/rope/garland with the wrong axis |
| gat | 0 | diagonal or 1-up step in a line not bridged |

## Per block

| kind | block | count |
|---|---|---|
| fout | `guhs:wolkmarmer_gepolijst_muur` | 8 |

## Examples: fout

| x y z | now | should be |
|---|---|---|
| 2639 234 -550 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2649 234 -550 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2659 234 -550 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2669 234 -550 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2639 234 -490 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2649 234 -490 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2659 234 -490 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |
| 2669 234 -490 | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]` | `guhs:wolkmarmer_gepolijst_muur[east=tall,north=none,south=none,up=false,waterlogged=false,west=tall]` |

## Worst tiles

| tile | open |
|---|---|
| 41,-9 | 4 |
| 41,-8 | 4 |
