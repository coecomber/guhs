# NOTITIES W-SCHEPEN (round W)

Branch `110-w-schepen` from 0f00119f2 (110-v-int). Goal: every ship, boat and airship in zones 1-6 can be boarded by a survival
player on foot. User reports: Kapitein Hemelhoog's platform (fences on the boarding edge, ship ~6 blocks off behind them), the
Oosttaksteiger ship too far to click, "check all ships". Round-U leftovers: line 0 off, zone-3 harbour masts without a stand spot.

## Inventory (all boarding points)

| What | Where | How you board | Before | After |
|---|---|---|---|---|
| Line 0 Kampsteiger | kamp dock, north arm | airship | line inactive (no POIs) | tip (-229,87,1008), east |
| Line 0 De Haven | bay pier, west tip | airship | inactive (the old spot was the mooring tower plaza) | tip (30,65,470), west |
| Line A Noordklifhaven | havens.pier, zone 1 | airship | stand spot 4 inside the platform, ship over the tip fence + mast flag | tip (150,127,-657) |
| Line A Kandijkom-dok | zone 2 dok | airship | stand spot 20 inside the dock, ship over the dock | tip (704,91,-880) |
| Line B Galmdok | havens.pier, zone 2 | airship | as Noordklifhaven | tip (984,117,-856) |
| Line B Weststeiger | zone 3 round platform | airship | stand spot = landing, ship lay ON the mast | tip (1186,167,-602) |
| Line C Oosttaksteiger | zone 3 round platform | airship | idem (the user's "boot te ver") | tip (1622,167,-485) |
| Line C Lampionhaven | zone 4 luchthaven | airship | ship 12 blocks off the open west edge (anchor rule) | tip (1791,71,-470) |
| Line D Opstijgpier (Hemelhoog) | zone 4 round platform | airship | ship 6 off, behind the hekje rim | tip (2524,73,-565), rim open |
| Line D Walvisrughaven | zone 5 pier 2 | airship | ship over 8 blocks of pier + rails, gate at the end | tip (2630,217,-352), gate gone |
| Line E Staarthaven | zone 5 tail | airship | **no pier at all** (placeholder inside the tail) | tip (2966,210,-520), new |
| Line E Rozenkloof | zone 6 Luchthaven | airship | wrong direction (180: ship over the pier/tower), POI snapped onto the railing, gate | tip (3398,142,-740), south |
| 11 zone-4 boat docks | boot_dok_* | own boat (X key, auto-mount) | OK | OK (water within 1-2 blocks) |
| Bells (kamp, De Haven, Kandijkom, Honingtak) | | open the travel map | not a vessel | unchanged |
| Gondola zone 2 | bell | scene | not a vessel | unchanged |

## What I did

**Docking rule (Java, `luchtschip/Lijnen.java`):** a harbour POI `haven_*_vertrek` = the LAST stand cell at the open tip of its
pier. `Haven.UIT` 9 → **10**: the spinning propellers (9.5 behind the ship's middle in the model) end exactly on the tip's outer
edge, the hull stern lies 0.5 beyond it, the stern click box (9.75) starts on the tip itself (click distance 0.67 from the eye on
the tip). The POI lookup order: zone-4/5/6 generated POIs first, the old T-INT zone-4 "anchor minus UIT" rule only as fallback
(zone 6's POI class was not in ZOEKERS: added).

**Boarding on foot (new `luchtschip/Loopplank.java`):**
- walk on: stepping off the tip towards a docked ship = `instappen` (closed line: the Poorten explanation, you stay);
- a right-click on a pier block within 4 of the tip boards (hooked in `Luchtschepen.pierKlik`, event cancelled when boarded);
- safety net: dropping off an open tip (no ship, or refused) puts you back on the tip (no fall damage) with
  "Het luchtschip ligt hier nog niet aan. Volgende vertrek over m:ss". Only over open air (a pier that runs on is just walked on).
- Real lines only in Het Vadsrijk, test lines anywhere. Runs every tick from `Luchtschepen.tick` (cheap: players × 12 tips).

**Line 0:** both POIs now exist → `Lijn.actief()` is true. KAMPSTEIGER / HAVEN_ZWAMZOET reserves + direction (De Haven 0 → 90).
The old via points still clear the terrain (luchtschip_check OK).

**Line E:** zone 5 builds the Staarthaven (`bouw5.staarthaven`): the tail path's east end between the fluke bridges, railing
removed, POI `haven_z5_z6_vertrek`. `Z6VerhaalPlekken.STAARTHAVEN` now reads that POI (the quest tracker / film trigger used the
old placeholder inside the tail). Rozenkloof `Z6Lijn.ROZENKLOOF` richting 180 → 0.

**Zone-3 masts:** `dok_mast` / `oosttaksteiger_mast` / `haven_z3_*_mast` are now a stand spot 2 cells inland of the mast foot
(they were inside the log; `z3_uitkijk_mast` NPC place uses dok_mast). Removed from the poicheck3/loopcheck3 exemption lists.

## Generator files touched (integration: regenerate these zones)
- `tools/vadsrijk/havens.py` (shared by zone 1 Noordklifhaven + zone 2 Galmdok): tip edge open (|lateral| ≤ 3), vertrek = tip, mast flag flies outward.
- `tools/vadsrijk/wereld/bouwsels.py` (zone 1): bay pier POI `haven_z1_kamp_vertrek`, no swim ladder at the bay pier's tip; new `lijn0_kampsteiger` (carves the arm's tip railing, the flag's top row, one ladder rung; POI `haven_kamp_z1_vertrek`).
- `tools/vadsrijk/maak_kaart.py`: one call `bouwsels.lijn0_kampsteiger(...)` after `kamp_arena` (marked W-SCHEPEN).
- `tools/vadsrijk/zone2/bouw2.py`: Kandijkom dock south tip open, POI `dok_instap`, `haven_z2_z1_vertrek` = it.
- `tools/vadsrijk/zone3/plan3.py` (`steiger_tip`, `*_instap`, `*_mast` stand spot, vertrek = tip), `zone3/bouw3.py` (rim open at the tip), `zone3/poicheck3.py`, `zone3/loopcheck3.py` (exemptions).
- `tools/vadsrijk/zone4/plan4.py` (POIs `haven_z4_z3_vertrek`, `haven_z4_z5_vertrek`, anchors), `zone4/kit/eilanden.py` (Opstijgpier east rim open).
- `tools/vadsrijk/zone5/bouw5.py` (pier 2: no gate, masts lower; `staarthaven`), `zone5/plan5.py` (vertrek = pier-2 tip, anchors).
- `tools/vadsrijk/zone6/kit/rozenkloof.py` (no gate, mast lower, anchor), `zone6/plan6.py` (GEEN_SNAP for the pier spots: the snapper put them on the railing).

Verified locally by regenerating only the harbour tiles (zone 1 `2,-11;-4,15;0,7`, zone 2 `15,-14;10,-14;11,-14`, zone 3
`18,-10;25,-8`, zone 4 `39,-9`, zone 5 `41,-6;46,-9`, zone 6 `53,-12`); generated outputs (tiles, *WereldPlekken.java, POI_*.md)
are NOT committed. **Note:** the Java expects the new POIs (`haven_kamp_z1_vertrek`, `haven_z1_kamp_vertrek`,
`haven_z4_z3_vertrek`, `haven_z4_z5_vertrek`, `haven_z5_z6_vertrek`): without the regen, line 0 stays inactive and lines C/D/E
fall back to the old spots, and the gametest `havensOpDeKaart` fails. After the regen, run `schepen_check.py` (must be 0).

## Shared-file changes (all marked W-SCHEPEN)
- `luchtschip/Lijnen.java` (UIT, reserves, ZOEKERS order), `luchtschip/Luchtschepen.java` (3 hooks: tick, pierKlik, vergeet).
- `zone6/verhaal/Z6Lijn.java` (richting), `zone6/verhaal/Z6VerhaalPlekken.java` (STAARTHAVEN from the zone-5 POI).
- `indeling/IndelingRegistry.java` (fallback harbour spots = the tips).
- `tools/features/__init__.py` (+`vadsrijk_w_schepen`), lang nl/en (+1 key, 2 texts overridden: pier hint, "mag").
- New: `luchtschip/Loopplank.java`, `luchtschip/VadsrijkSchepenGameTests.java`, `tools/features/vadsrijk_w_schepen.py`,
  `tools/vadsrijk/schepen_check.py`.

## Checks
- `python tools/vadsrijk/schepen_check.py [--kaart] [--alleen ...]`: per airship harbour STAND, BEREIK (flood fill on foot),
  RAND (fence/wall/block on the boarding edge), KIER (gap pier edge → ship), KLIK (nearest click box ≤ 3.0 from an eye on a
  reachable cell, line of sight), RUIMTE (every model cube of the docked ship, incl. balloon/fins/props, vs blocks/fences/ladders),
  LANDING (veiligeLanding replica lands on the tip), BARRIERE (tip inside Barrieres.VRIJ); per boat dock STAND/BEREIK/WATER;
  line 0 inactive = error. Exit 1 on any error. Result on the regenerated tiles: **23 boarding points, 0 errors**.
- `luchtschip_check.py`: now reads UIT, includes line E (Z6Lijn), uses the POI-first order; within 30 blocks of a dock 4 free
  blocks are enough (low piers over water). All 6 lines OK.
- Gametests `-Pgt=VadsrijkSchepen,VadsrijkLuchtschip` (with the locally regenerated POIs): **11/11 passed** (new: `havensOpDeKaart`, `loopplank`).

## Decisions
- Tip open, no fence: the user asked for it ("waar je erop zou moeten kunnen lopen"); the safety net replaces the railing.
- UIT 10 instead of moving every POI half a block: one number, nothing clips.
- De Haven end of line 0 = the bay pier tip, not the mooring tower (no clear airspace at stand height there; the old block ship
  and the bell/captain at the tower stay as decoration + travel map).
- Masts/flags near tips were lowered/flipped instead of moved (smallest visual change).

## Open points
- Not seen in a client (no client runs this round): check the stern/props against the tip in the dev client per SPIEK.
- Walking on the deck from the stern side: the cabin sits at the stern, the deck strip starts in front of it (U-LUCHTSCHIP's
  deck clamp); boarding puts you on a free deck place as before.
- The old anchored block airships (Kampsteiger south of the jetty, Kandijkom east, De Haven tower) are still there as decoration.
