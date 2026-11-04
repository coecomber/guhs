# NOTITIES_Z3_SYSTEMEN: zone 3 "De Schemerwortel", slice systemen (branch 110-z3-systemen)

## Kern: layered areas (EARLY COMMIT 206551a31, other slices can merge/cherry-pick it)
- `kern/Gebieden.Gebied` has an optional `Laag` (y-range and/or cylinder/ring around a centre). The old 8-argument constructor
  still works (no layer). Build with `Gebied.cilinder(id, cx, cz, rMin, rMax, minY, maxY, lo, hi, stad)` or
  `new Gebied(...).metY(minY, maxY).metRing(cx, cz, rMin, rMax)`.
- Lookups: `Gebieden.op(x, y, z)` / `Gebieden.op(BlockPos)` = full (layers). `Gebieden.op(x, z)` (2D) skips layered areas, so in
  zone 3 it returns the catch-all `z3_ring`. All existing callers that have a y now use the 3D lookup.
- `zone3/systemen/Z3Lagen.GEBIEDEN`: the zone-3 plan, copied from z3-wereld's POI_Z3.md v0 (same ids/order) + `z3_ring`.
  `Z3Lagen.kern(id, cilinder, a, b, c, d, minY, maxY, lo, hi, stad)` converts z3-wereld's `Gebied3` fields.
  `Z3Lagen.registreer()` puts them in front of zone 1/2 (called by Z3SystemenRegistry).
- Biomes: `VadsrijkBiomen.index(x, y, z)` (the biome source is 3D in zone 3): honingtak, kruinzee, takkenbanen, schorsstad
  (+ stamspiraal), wortelvelden, wortelduister, zeppelinwerf, hart, schemerwortel (rest of the box). "Het Vadsrijk - <naam>".
- `kern/Vaardigheden.NA_GEBRUIK` (player, skill) after a successful skill use.
- Merged 110-i-audio (fast-forward) so `StadsMuziek.stadOp(x, y, z)` exists and the client player uses it.
