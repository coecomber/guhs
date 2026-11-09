# AUDIT_VERBIND - connecting blocks in the Vadsrijk tiles

**GENERATED** by `tools/vadsrijk/audit/verbindcheck.py` (runs with `maak_kaart --audit`). Counts what the connection pass `tools/vadsrijk/verbind.py` would still change; after a regen everything must be ~0. Rules: `tools/vadsrijk/LEESMIJ_BOUWREGELS.md`.

Tiles: 1308. Total open: **0**.

| kind | count | meaning |
|---|---|---|
| los | 0 | fence/wall/pane misses a connection to a connectable neighbour (loose post) |
| fout | 0 | wrong state the other way (connected to nothing, wall height/post) |
| as | 0 | chain/rope/garland with the wrong axis |
| gat | 0 | diagonal or 1-up step in a line not bridged |
