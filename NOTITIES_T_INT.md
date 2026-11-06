# NOTITIES_T_INT (night round T integration, branch 110-t-int, from abb8a027c = the final r-int)

User-facing documents: `EINDRAPPORT_NACHT.md` (Dutch, per slice + open points), `SPIEKBRIEF.md`, `TESTEN_VADSRIJK.md`.

## Merge order (all with --no-ff, one commit each)
t-wereld, t-dialoog, t-geluid, t-models, t-vfx, t-z4boot, t-z4wezens, s-site; then t-ambacht, t-z5wezens, t-rijdier, t-items,
t-z4wereld, t-z5wereld, t-puzzels, t-z5verhaal, t-opslag, t-kleding, t-luchtschip, t-karakter, t-z4verhaal, t-imker, t-wolkwal,
t-z4dieren, t-droom.

## How conflicts were resolved
- Lang json: key union keeping each line's raw text (no re-escaping churn). Deletions only when ours still had the base value.
  A criss-cross virtual base (t-droom) is read line by line.
- VadsrijkFeature / VadsrijkClient / WereldRegistry / FEATURES: both sides. `vadsrijk_t_vfx` is moved to the END after every merge.
- Manual merges:
  - t-rijdier vs t-z4boot (the boat on the new species API);
  - t-z5wereld vs t-z4wereld (VadsrijkBiomen: zone 5 by height first, then zone 4, then zone 3; maak_kaart zone 4 before 5);
  - t-z4verhaal vs t-z5verhaal (travel map: zone4 -> lampionkust + hoogvads, route D, both quest files, both guide lists).

## Glue written here (all marked `T-INT`)
- **Klodderreus VFX:** `anims_klodderreus()` + vfx_* on root, `KlodderGeo` vfx controller, `BaasAnims.controllers` on GeoAnimatable,
  `BaasAnims.speel` -> KlodderGeo.
- **Boat:** `RijdierSoort.ZEILBOOTJE` (Eigenschap.WATER); EcoSpul skips water species (BootSpul owns entity + icon);
  `Uiterlijk.zonderGarderobe`. BootZee: POI prefix `boot_dok_`, the current = plan4.STROOM; `Z4Stroming.EIGEN`. Roles
  `z4_zeilboot_geef` / `z4_zeilboot`.
- **Ambacht:**
  - `opslag/AmbachtTas` (MateriaalTas.BRON);
  - hiscores through `WebExport.EXTRA_HISCORES` (same folder and format, no uuid) + 10 site tabs;
  - `HoogvadsGilde` (teachers -> workshops, role `werkplaats` -> smithy);
  - zone-4 fishing groups + `NpcApi.meld(z4_vis, gevangen)`.
- **Opslag:**
  - one boss cast bar: `Gietbalk.baas` -> `Vfx.cast`; `RijkHud.BOVEN_EXTRA` includes `CastBalk.hoogte()`;
  - the drankjesriem on `RijkHud.plek("drankjesriem")` + cooldowns in `OpslagHud.EXTRA`;
  - bankier `z5_gouddraad`.
- **Karakter:**
  - `Opslag.KARAKTER` = slot+1, `Opslag.sluit` on VOOR_WISSEL;
  - accountBreed `economie.garderobe` + `boot`;
  - `KarakterRegistry.ZACHT` (z5_proefarena) + scenes never retire an Eénvads knight.
- **Luchtschip:**
  - ZOEKERS + Z4/Z5WereldPlekken + `zone4Haven` (pier spot = anchor - UIT);
  - Opstijgpier faces east;
  - routes A/B/D re-laid (`tools/vadsrijk/luchtschip_check.py [--fix]`).
- **Zone 4 story:**
  - `Z4VerhaalPlekken.ALIAS` (68 story ids -> real POIs);
  - no fallback areas;
  - `z4_schildpad` id;
  - Oma Dobber texts.
- **Zone 5:**
  - whale volumes -20;
  - z5 combat areas re-asserted after the districts;
  - fin platform height = top of the lowest solid run;
  - the Grauwe Droom plays the Vahoegnir theme.
- **Rewards:**
  - z4_s14 -> Lampion Pluimguh, z4_s15 -> Duinhupper;
  - kleding `boek:zone4:100/50` for the lantern wings/hairpin;
  - no gliding in z4/z5 dungeons + the Droomwolk.
- **Levels:** `Levels.MAX = MAX_GENERIEK = 60`; LV_CAP 60 in vadsrijk_t_z4verhaal.py and vadsrijk_t_z5verhaal.py; z5 VORIGE = z4_hq07_opstijgpier.
- **Keys:** drankjesriem Alt+1/2/3 (KeyModifier.ALT, IN_GAME); Y Ambachten, I Knapzak, O Kledingkast.
- **Generators:**
  - NL/EN order fixed in 12 `build(h)` functions (make_v2.lang is key, en, nl);
  - all night generators re-run in FEATURES order;
  - `maak_modellen.py --sparring-over` AFTER t-z5wezens' generator.
- **Map:**
  - zone 4 drops its anchored block airship;
  - kite finish moved twice (now reachable);
  - audit.py `TEGEL_FILTER` (audit4 works again);
  - `Barrieres` z4 = (2184, -491, 426, 270);
  - Z3Lucht fades out before zone 4.
- **Other:**
  - blokuitleg for the 19 `vr_pz_*` blocks + 2 older ones;
  - t-imker's portrait resolver only;
  - FTB CHAPTER_VERSION 63.

## Resume
See the "Hervatten" section of EINDRAPPORT_NACHT.md. The generated map files are committed in this tree (it is the test client).
