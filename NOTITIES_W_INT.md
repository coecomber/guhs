# NOTITIES W-INT (round W integration, branch 110-w-int from 0f00119f2)

## Phase 1 (merges + fixes)
- Merged `--no-ff`, in order: w-modellen bf25ee013, w-leven 856804e9a, w-spel 12eeac6ef, w-grot 25571ca89, w-balans 7308e3946,
  w-schepen e4c6ec984, w-engels 1c980a696.
  - `stadsleven/Beestjes.java` + `Straat.java`: git merged them cleanly; checked by hand. W-MODELLEN's BlubLook (model, texture,
    anims, scale) and W-LEVEN's split cap (`blobMax`/`muisMax`/`volgendeIsBlob`), pet distance and town mouse factor are all in.
  - lang conflicts (w-schepen, w-engels): both sides appended at the end of the file. Resolved with a key-wise 3-way JSON merge
    (no key that a slice removed comes back). en_us is rebuilt by apply_en afterwards anyway.
  - `tools/features/__init__.py`: `'vadsrijk_w_grot'` added right after `'vadsrijk_u_grotkerker'`.
- Resources: the full `make_resources.py` is NOT committed (same as earlier integrations: on 26.1 it rewrites about 1,800
  unrelated files and crashes in `fix_compass_needles`). Only the round-W slice generators were run standalone:
  wezens, u_leven_stad, u_questlopen, w_grot, t_z4verhaal, u_z6verhaal (+ u_z6wereld after it, because the standalone z6verhaal
  drops `vadsrijk.kern.gebied.z6_*`), w_schepen, t_puzzels. Png byte drift (krimp) was reverted.
  Then `make_ftbquests.py` (needs the 1.21.1 client jar copied to build/moddev/artifacts, see NOTITIES_W_ENGELS) and the output
  was committed.
- The 20 missing texts (nl via the owning generator, then en):
  - `biome.guhs.knuffelkwartier`: vadsrijk_u_huizen;
  - `item.guhs.vr_eco_*`: vadsrijk_economie. On 26.1 a block item has its own item.* name. That generator is stale (it would undo
    later Rijfluitje/X-key text edits), so only its 4 new keys were put into nl_nl;
  - `vadsrijk.bazen.effect.z5baas_*`: vadsrijk_t_z5wezens. `z5droom_*`: vadsrijk_t_droom. These items have NO extra mechanic in
    Java, so the "Effect:" line states their real bonus stats, rather than inventing an effect.
  - Side effect: the z5 area names now come from their generator again (e.g. "De Wolkrand: de Noordvin" instead of w-spel's
    hand-added "De Noordelijke Wolkrand").
- English top-up: `tools/lang/en/c415_vr_aanvulling.json` (123 keys, registered in chunks.json), 2 airship texts with changed
  Dutch edited in c412, 2 keys allowlisted (`Dungeon: %s`, `Type: %s · zone %s`). Then apply_en, `engelscheck` OK, `--merk`.
  `taalcheck`: 2452 keys, 0 missing.
- Flash F4 (Z6VerhaalPlekken CAMERA "f4"): the first half now starts on the Sterrendovende Toren (built zone-6 terrain), seen from
  the east, and the look sweeps west to Hoogvads as before (instead of looking at empty sky at x 4900).
- run/ copied from v-int without saves/ and logs/.

## Tests (phase 1)
- compileJava OK.
- `runGameTestServer -Pgt=` TModels, VadsrijkWezens, Stadsleven, WLeven, Z5Bewoners, VadsrijkTaal, SceneLaden, VadsrijkQuestLopen,
  VadsrijkZit, VadsrijkDungeon, VadsrijkGrotKerker, VadsrijkGrotten, Z4Verhaal, Z6Verhaal, VadsrijkQuestRobot, VadsrijkSchepen,
  VadsrijkLuchtschip, Taal, Taalsleutels: 68 tests, 67 passed. The only failure is `havensOpDeKaart` (expected until the map
  regen: line 0 POIs).

## Phase 2 (after the PC restart, 2026-11-09)
- slots/ was empty. Focused rerun `-Pgt=VadsrijkParty,VadsrijkUHud,VadsrijkZ6Raid,VadsrijkMenu,VadsrijkKern,VadsrijkDungeon,
  VadsrijkDungeonNep,TaalsleutelsGameTests`: **36/36** (incl. the fixed `nepVultRijEnStart`).
- Merged 110-w-bereik (5851f7a93) as 4421c85e8. Conflicts:
  - `zone4/kit/eilanden.py` Opstijgpier ring: BOTH openings kept (W-SCHEPEN east tip where the airship docks + W-BEREIK west
    where the pier walkway comes in).
  - en_us.json: ours; the 2 new luik keys get English in the top-up (apply_en rebuilds en_us anyway).
  - Auto-merged and checked: bouw3 (W-BEREIK lift_kapel stop + W-SCHEPEN steiger tip rim are different spots), bouw5 (staarttrap,
    hofdeuren_vrij, trap_af landing next to W-SCHEPEN pier 2 + staarthaven), bouw2, maak_kaart (vrijpad after verbind + bereikcheck
    in --audit next to lijn0_kampsteiger).
- Secrets w-bereik could not reach (commit 9095c8f71), each checked against the real tiles first:
  - z1 `holle_boom` / knuffel_8: sat on a giant mushroom CAP 36 up (ground = mycelium y 71). Now on the forest floor under the
    cap (`bouwsels.grond_onder_hoed`).
  - z1 `hoedenschat`: the narrow unlit plank walk is diagonal; its 1-wide cells only touched diagonally and the rails of both
    sides closed every corner. `hoedenpaden.brug(smal=True)` now fills one corner cell at every diagonal-only contact.
  - z2 `galmgrot` / `ijsgrot`: the tunnel floor sat 3 (galm) / 7 (ijs) above the ground west of the mouth. `grotje()` builds a
    3-wide snow step ramp down (1 per block, head room carved); the `_ingang` POI moves to the ramp foot.
  - z2 `boorgaten` clue: stood on top of an ice/snow overhang above the avalanche foot; offset (4,3) -> (6,2) = the walkable foot.
  - z3 `kuiken_3`: that side of pond 3 is the crown's steep leaf edge (snapped down to y 213). Now on the walkable rim next to
    honingvijver_3 (angle a+1.25).
  - z3 `uitkijk_takpunt`: leaves hung 1-2 over the moss/path walk onto the lookout platform. New last zone-3 pass
    `takpunt_vrij` clears leaves/vines in the 3 cells above non-leaf floor blocks within 9 of the lookout.
  - z4 `vliegerspul_1/_3`: Java literals (no map POI) on the kite rocks; moved onto the path right below (find radius 3).
- Regen (full, `maak_kaart --audit --geen-cache`, zone 4 before zone 5 by the built-in order), 20 min.
- **verbindcheck regression found + fixed:** w-bereik's `vrijpad` runs AFTER the connection pass and its "floating rail" rule
  deleted the diagonal corner pieces verbind fills into railings over the void -> los 458 / fout 342 / gat 489. Fixes:
  - `vrijpad.py`: a floating piece that touches a SUPPORTED line piece (3x3x3) is a railing corner: kept (the kathedraal
    balustrade floats as a whole line, so it still goes);
  - `maak_kaart.py`: a second `verbind.na_sinds` after vrijpad (converges: the 2nd pass changes ~1 block).
  - Result: verbindcheck **2** (los 1, gat 1; v-int baseline 8). vrijpad keeps peeling one hanging chain stack by 3-5 blocks per
    run (chain -> hanging lantern rule): harmless, not chased.
- More secret fixes after the first bereikcheck:
  - z2 `ijsgrot`: the ramp idea failed there (the cave sits on a steep lake bank, ground 30 below). The ramp is now galm-only;
    the ice cave gets a 1x1 roof hole with a scaffolding column (the dome top is walkable).
  - z1 `geheim_tempelkamer`: sealed behind cracked bricks and NO code opens it: a 2-high crumbled gap in the cracked wall.
  - z2 `uitkijk_galmtop` (U-OVERGANG viewpoint needle): the slope buried the lowest treads of its spiral. `ijsnaald` now carves
    3 cells of head room over every tread first, then lays the treads.
  - z3: `takpunt_vrij` also clears flowers left standing on removed leaves (zweefcheck z3 back to 0).
- Checker notes: `poicheck6` and `loopcheck6` read `_cache/plekken_z6.json` = the UNSNAPPED plan spots (37 / 30 hits at y 70);
  the published Z6WereldPlekken (snapped) checks 0 bad with poicheck6's own `staat()`. loopcheck3 VAL +1 = a sealed 1-cell pocket
  in the werf tower (1677,214,-805), no way in.
- Full suite 407 tests, 6 red: 2 real (schadegetallen, huisdierNaamVanSpeler expected Dutch; the test server loads en_us, which is
  real English since w-engels: tests now accept nl or en), 4 order-dependent (pass alone: Z2Bazen 6/6, Stadsleven 5/5, Z5Droom 7/7,
  Ruiters 3/3). Quest robot 41/19/24/23/24/25, 0 stuck.
- **Client start crash found by the AutoCheck run** (gametests never load the client): w-party registered the SERVER config and the
  CLIENT config under the same file name `guhs-vadsrijk-nepspelers.toml` -> "Detected config file conflict". The client config is
  now `guhs-vadsrijk-nepspelers-client.toml`.
- Site: 110-w-int merged into 110-u-site, regen (runtime dump copied from w-int's run/), preview gh-pages aa19bf3e8 (200, noindex).
