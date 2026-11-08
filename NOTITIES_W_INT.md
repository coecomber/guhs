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
