# HERVAT V-INT (integration round U, branch 110-v-int): paused 2026-11-06 ~17:35

The user's test client is still t-int (never run gradle there). This tree becomes the new test client.

## Merged (in this order, all with --no-ff, compileJava green after each)
| slice | head |
|---|---|
| u-hekken | 1de7ce906 |
| u-overgang | 25bcbcd1b (verbind.py = latest u-hekken) |
| u-deco | 57dba63b5 |
| u-dubbel | 1272eb16b |
| u-zichtbaar | 31510d84c |
| u-karakter | 2fb6aeba7 |
| u-luchtschip | 721ecff6e |
| u-ruiters | 80895692a |
| u-specs | 20acf28f7 (89 lang keys it dropped by accident were restored in c043130d4) |
| u-hud | 061a44dce |
| u-tabs | db5771432 |
| u-toetsen | 3bd81d203 |
| u-audio2 | 5e8e108cf |
| u-ambacht2 | 4c880181d |
| u-gilden | f165d0af2 |
| u-huizen | eef01fd48 |
| u-grotten | 0db78a33d |
| u-z5 | 8ea405aac |
| u-z5binnen | 57fe33c52 |
| u-tapijt | 07a359bbc |
| u-z6muziek | e3f98d3b5 |
| u-z6verhaal | a8eec423c |
| u-z6wezens | a1114de75 |
| u-z5tuin | 9b995da0d |
| u-grootte | 90689737b |

## Glue built here (package feature/vadsrijk/vint, generator tools/features/vadsrijk_v_int.py, all marked V-INT)
- **Key H:** opens the Vadsrijk-menu only in the Vadsrijk; elsewhere H stays einde2's power (two conflict contexts, `VIntClient.Context`).
- **Menu tabs:**
  - Knuffelbond (115) and Mijn huis (117);
  - sub-tabs "HUD aanpassen" (under Instellingen) and "Specialisatie" (under Skillboom);
  - the menu key is in u-toetsen's panel.
- **Specs:** the spec line in the Karakter tab (`KARAKTER_EXTRA`) and in the party frames (`SPEC_BRON`).
- **Puzzles:** a cave puzzle counts once (in the "Grotten" category) and is listed in the Puzzels tab.
- **u-dubbel:** the scan leaves out the riders and the whole Knuffelkwartier (`Dubbel.NIET_HEURISTISCH`).
- **Housing:** street lamps are the guhlantaarnpaal. The castle .nbt go through the central verbind pass (0 changes).
- **Mount drops (user rule):** 10 % per player per kill/clear, with a pity of 15 per character per source (`RijdierBuit`).
  - Sources: the Opperzwam's Zwamzwijntje, Spiegelzucht's Kristal-Pluimguh, the Vahoeg gold Pluimguh.
  - Dev: `/guhs vadsrijk loot kansen|pech|rol`.
  - Gametest `VIntGameTests`.
- **Carpet:** no generic "Spatie = hup" tip on the carpet's first ride.
- **Z3Baas** registers T-VFX's controller, so the zone-3/4 bosses now play vfx_*.
- **Zone 6:**
  - the beacon stinger (Z6Puzzels);
  - line E in luchtschip.Poorten;
  - the skill trees got rows 8/9 (lv 62/66) and the gaps in rows 6-7 filled, so 69 points are a real choice (`SpecBoom.zijdeRijen`, names in vadsrijk_v_int.py);
  - the z6verhaal main-line test takes the compass from the real dungeon.
- **Map fixes:**
  - the tail-fluke platforms sit on the real skin;
  - the tail path no longer puts a railing across the junction, so the 4 z5w tail spots are reachable;
  - the hat-deck rims stay open next to diagonal bridge lanes (hoeden.7);
  - cave lamp posts no longer make 1-wide nooks.

## Map state
- Full regen of zones 1-5 (`--geen-cache --audit`), then zone 5 again with u-z5binnen + u-z5tuin. The generated files are committed in this tree.
- Checks:
  - audit: kuil 4 (= baseline; the 3 Sporenwoud pits are gone);
  - verbindcheck: fout 8 (wall `up` flag on the Raadszaal, cosmetic);
  - grotcheck: 0 in all 5 caves;
  - loopcheck3: PAD 397, VAL 1 (baseline);
  - loopcheck4: 0; poicheck3/4: 0;
  - loopcheck5: every POI reachable, VAL 475, LEEG 660 (the open tail fluke; the Wolkvangnet catches you);
  - the cave boxes z2/z5: 0;
  - luchtschip_check: A-D OK;
  - kaartPakControle: 0 differences.
- zweefcheck: z1 115 / z2 20 / z4 26 / z5 144. These are Zwamzoet wall lanterns and Hoogvads lampion garlands, not fixed (old/intended).
- **IMPORTANT:** run maak_kaart / kaartpak.py with `JAVA_HOME="$APPDATA/PrismLauncher/java/java-runtime-delta"`, otherwise the kaartpak step fails (system JDK 14).
- Rider routes were regenerated (routes.py) after the zone 1-5 regen.

## Tests so far
- Glue batch: 38/38.
- Broad suite (Vadsrijk, Z2-Z5, Dubbel, Geluid, ...): 3 failures.
  - opperzwamFasen and grotSpawnerVindtDeVloer are known flakes.
  - Z4Verhaal "travel map lampionkust" is FIXED (it was the dropped lang key).
- VadsrijkZ6Wezens.kerker failed once in a batch and passed alone (flake).
- Last run (`kaartPakControle` + Grootte, Wereld, Specs, Klassen, Z6Verhaal, Luchtschip, Z5Licht, Z5Wereld): only specBoomIndeling failed ("outside the grid"). It is fixed (93c0e7f3f) but NOT yet re-run.

## Still to merge / to do (in this order)
1. Re-run `-Pgt=VadsrijkSpecs,VadsrijkKlassen,Z6Verhaal,VInt,VadsrijkTRijdier`.
2. **u-z6wereld:** the lead says when it is done.
   - Merge it, then `maak_kaart --zone 6` (or alle; zone 6 after 5).
   - Then `routes.py .`, the checkers, and the kaartpak.
   - Kwartiermeester/Bondenbord ids `kk_poort_<stad>` / `bond_bord_<stad>` are handled generically (prefixes). Check them in the world.
3. **u-z6raid** (1dcbf2527), after z6wereld:
   - `RijkHud.standaardPlek` case "z6raid";
   - halter: `Rijdieren.geef(p, "bultguh:sterrennacht")` does not exist. Give the Bultguh + `Aankleden.geef(p, "vacht:bultguh:sterrennacht")`, through `RijdierBuit` (10 % + pity, new source);
   - bond xp on a clear: `Z6RaidHaken.OPGEVROLIJKT` -> u-gilden BondXp;
   - play `vr.z6raid.muziek.wachtzaal` in the Wachtzaal and `.opgevrolijkt` at Ringeling's defeat;
   - check that Spiegelvizier = Meester Glans in z6verhaal;
   - the raid interior .nbt must load through the structure manager (u-grootte: no .nbt in the jar).
4. **u-questrobot** and **u-site**, when the lead says so.
5. **u-krimp** LAST (audio/texture re-encode) or re-run its script on the final tree.
6. FEATURES: `vadsrijk_t_vfx` stays last.
   - The helper `tools/vadsrijk/vint_docs_wip/merge.sh <slice>` does the lang union, both-sides on VadsrijkFeature/VadsrijkClient/FEATURES and the t_vfx-last fix. Copy it to a scratch folder before use; it cd's to v-int.
   - LESSON: the lang union must not re-add keys that a slice deleted. langunie.py handles this now.
7. **Phase 2 rest:**
   - the full gametest suite (rerun flakes alone);
   - an AutoCheck walk if RAM allows;
   - run/ from t-int/run without saves/logs (run-client.bat is updated already).
8. **Phase 3 docs (Dutch):**
   - SPIEKBRIEF.md: build it with `python tools/vadsrijk/vint_docs_wip/maak_spiek.py . <(git show eaee06d08:SPIEKBRIEF.md) kop.md zones.md feedback.md`. The parts are in vint_docs_wip; still to write: kop.md (start + key table incl. H, `;`, Alt+1/2/3), the zone-6 block in zones.md, and the loot table (all sources + 10 %/pity + `/guhs vadsrijk loot kansen`);
   - TESTEN_VADSRIJK.md: put testen_u.md after "Nieuw in de nachtronde T";
   - EINDRAPPORT_RONDE_U.md.
   - Remove tools/vadsrijk/vint_docs_wip at the end.

## Known open issues
- Not seen in a client: the menu, tabs, the H context, the spec rows 8/9 screen height, the castles, the housing lamps.
- u-specs' standalone generator runs can drop other generators' keys. Never run them `_direct` on this tree without the union check.
- zweefcheck findings, as above.
