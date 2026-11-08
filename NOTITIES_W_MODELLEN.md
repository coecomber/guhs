# NOTITIES_W_MODELLEN (round W, branch 110-w-modellen, from 0f00119f2)

Goal (user, release requirement): "de blobs weer de leukere oude look". The Grauwe Klodderreus and the other square-ish slime
mobs back to their old square models; the blue town critters on the existing blob model (the Blub-guh of the dream and the
tamable survival Blubs). Gameplay, hitboxes, names and spawn counts untouched.

## What happened (git history)
- **2026-11-03 `6cf5c068f` (110-vr-wezens):** zone-1 creatures made by `tools/features/vadsrijk_wezens.py`: square slime models
  (one 12x10x12 block + a 10x1.5x10 cap + two guh ears). These are the "old" models.
- **2026-11-05 `e906b09c6` (t-models, round T, decision R5 "hybride"):** model kit v2 (`vadsrijk_t_modelkit.py`): `assets()` built
  all of zone 1 (and zones 2-3) with `stijl(2)`: rounded boxes, a jiggle cap on a new bone `wiebel`, v2 painting. This is when
  the slimes became round.
- **2026-11-05 `d3745cd78` (t-models):** the Grauwe Klodderreus got its own big round GeckoLib model (`tmodels/grauwe_klodderreus`,
  `KlodderGeo` + `KlodderGeoRenderer`); before that he (and the proefklodders/kloddertjes) used the **vanilla slime model**
  (`kern/client/KlodderRenderer`: grey `vadsrijk/kern/grauwe_klodder.png`, pink `entity/guh_slime.png` after the plof). That
  vanilla cube is the "meer vierkante variant" the user means. `KernClient` was switched to `KlodderGeoRenderer::klein/::reus`.
- **2026-11-06 `58685c360` (t-int):** zone 4 regenerated with kit v2. `8a57580d0` (u-z6wezens) and z6raid: built with kit v2 from the start.
- **2026-11-06 `447070480` (u-krimp):** pngs recompressed only (no look change).
- **Round U (u-leven-stad):** the town critters (`stadsleven/Beestjes.Diertje`, ~30 % blobs, and the Wandelaar's pet blob) used
  the zone-1 **klodder** model (by then the round kit-v2 one) with `textures/vadsrijk/stadsleven/blauwe_klodder.png` (the klodder
  texture recoloured light blue). They were never Blubs. The Blub-guh (`feature/blub`, entity `guhs:blubguh`, model
  `geckolib/models/entity/blubguh.geo.json`) is the blob of Guhdromenland and the tamable survival blob; it has 5 soorten, 3 tints
  and a Blub-Guh-Meter that changes its face/ears.

## Table: mob -> current model (0f00119f2) -> old model/commit -> action
| mob (entity) | model at 0f00119f2 | old model | action |
|---|---|---|---|
| Grauwe Klodderreus (`guhs:vr_klodderreus`) | `tmodels/grauwe_klodderreus` (round, GeckoLib, d3745cd78) | vanilla slime (KlodderRenderer, `grauwe_klodder.png` / `guh_slime.png`), before d3745cd78 | **reverted**: KernClient -> `KlodderRenderer::new` |
| Proefklodder / proefkloddertje (`guhs:vr_proefklodder`, `vr_proefkloddertje`; also the kloddertjes the giant spits) | zone-1 klodder model (round) via KlodderGeoRenderer::klein | vanilla slime (KlodderRenderer) | **reverted**: KlodderRenderer |
| Klodder (`guhs:vr_mob_klodder`) | `wezens/klodder` kit v2 (e906b09c6) | square, 6cf5c068f | **reverted** (geo/anim/png = e906b09c6^, byte/pixel identical to the fixed generator) |
| Kloddertje (`guhs:vr_mob_kloddertje`) | klodder model + kloddertje.png (v2) | square klodder + old kloddertje.png | **reverted** (textures) |
| Bubbelklodder | kit v2 | square, 6cf5c068f | **reverted** |
| Stuiterklodder | kit v2 | square | **reverted** |
| Moederklodder | kit v2 | square | **reverted** |
| Druipslijm | kit v2 | square | **reverted** |
| Kristalklodder (+ gloed) | kit v2 | square | **reverted** |
| Kampklodder (`guhs:vr_mob_kampklodder`, the friendly camp blob) | kit v2 | square | **reverted** |
| Elites of all the above | `_elite` textures from v2 | old `_elite` | **reverted** with the rest |
| Town blob (Diertje ~30 %, stadsleven) | round klodder model + `blauwe_klodder.png` (u-leven-stad) | none (new in round U) | **now the Blub-guh model** (blue soort, random tint 0-2, meter stage p..55), `BlubLook.java` |
| Wandelaar's pet blob (stadsleven) | same as above, scale 0.42 | none | **Blub-guh model**, scale 0.5 |
| Koning Klodder, Element-klodders, Kloddertje (z3 boss), KK-klodder (`vr_baas_*`) | own boss models (fa5f70af0 / 6c991c035) | never changed by T | untouched (still valid) |
| Huisdier Klodderreus (pet) | `vr_baas_kk_klodder` + `vr_huisdier_klodderreus.png` | never changed | untouched |
| Zone-1 non-slimes: hoedjesbol, glimzwam, sporenpuffer, zwamwachter, tempelridder, sporenpriester, mosgeest, knaag | kit v2 | square (6cf5c068f) | **kept** (not slimes); revert = one line, see below |
| Zones 2-3 (all, e.g. ijspuffer, schimmelbolletje) | kit v2 (e906b09c6) | square (z2/z3 commits) | **kept** (no slimes there) |
| Zone 4 (mistmopje, lampionkwal, stekelpluis, ...) | kit v2 (t-int 58685c360) | square (454d08684) | **kept** (no slimes) |
| Zone 5 / zone 6 / raid (bobbeltje, zoutklontje, warmpje, ...) | kit v2 from the start | none (new) | **kept, listed** (never had an old model) |
| Grauwe Klodderreus round model files (`tmodels/grauwe_klodderreus.*`, KlodderGeo, KlodderGeoRenderer) | in use | - | **kept in the tree, unused** by the renderer (TModels showroom + test still read them; the server still sends KlodderGeo trigger anims, harmless) |

## What I changed
- `tools/features/vadsrijk_wezens.py`: the SLIJM loop builds `model_slijm` inside `with stijl(1):` (rest of zone 1 stays v2).
  Regenerated output = the pre-T files exactly (geo + anim byte-identical, all 26 pngs pixel-identical); committed the pre-T
  files (`git checkout e906b09c6^ -- ...`), so integration may regenerate safely.
- `kern/client/KernClient.java` (shared, `// W-MODELLEN`): the 3 slime renderers back to `KlodderRenderer::new`.
- `stadsleven/BlubLook.java` (new, own file): model/anim `blubguh`, textures `textures/entity/blubguh/blauw_<tint>_<stap>.png`,
  idle/blij/walk, scale 0.6 (pet 0.5), the hidden bones.
- `stadsleven/Beestjes.java`: Diertje blob uses BlubLook (random texture), hop anim -> `walk`; the old BLOB_MODEL/TEX/SCHAAL
  constants removed. **BLOB_KANS (share of blobs) and all counts untouched** (w-leven's area).
- `stadsleven/Straat.java`: the pet blob row uses BlubLook; the little hop offset also for the Blub (its walk anim is named "walk").
- `stadsleven/client/StadslevenClient.java` (`// W-MODELLEN`): `PropRenderer.adjustModelBonesForRender` hides the Blub bones a
  blue Blub at stage 0-3 does not show (other soorten's bits, lijf_zacht, guh ears, strik, kleding).
- `tools/features/vadsrijk_u_leven_stad.py`: no longer writes `blauwe_klodder.png`; the png is deleted (nothing uses it).
- `tools/wiki_site/vadsrijk/renders.py`: klodderreus render from `tools/wiki_site/vadsrijk/vanilla_slime.geo.json` (new: the
  vanilla slime model as geo for the offline renderer) + `grauwe_klodder.png`; GELEEND paths starting with `tools/` are repo-relative.
  Renders regenerated for baas_klodderreus + the 7 zone-1 slime renders (wezen_*klodder*, druipslijm, kloddertje); index.json
  only updated for those (the run also re-rendered 7 unrelated stale renders: restored, not my area).
- `tmodels/TModelsGameTests.java`: the slimes must NOT have `wiebel` any more, their textures + the KlodderRenderer textures must
  exist; new test `stadsblobIsDeBlub` (bones, idle/blij/walk, every texture BlubLook can pick).

## Checked
- Boss variants (Koning Klodder, kloddertje z3, KK-klodder, pet klodderreus): own models, untouched, still valid.
- Bestiary/Verkenningsboek/menus: icons are items or live entity renders (follow the renderers); nothing points at removed assets.
- `blauwe_klodder` and `vadsrijk/wezens/klodder` are referenced nowhere else now (grep) except KlodderGeoRenderer (unused).
- Wiki renders source: every slime id resolves (renders.vind), klodderreus now the vanilla cube.

## Not done / open
- The blue Blub's soft glow pass (BlubClient.GloedLaag) is not drawn on town blobs (plain prop renderer). Fine IMO; say if wanted.
- Zone-1 non-slimes and zones 2-6 stay on kit v2. If the user also wants e.g. hoedjesbol/knaag square again: wrap their builder
  in `with stijl(1):` in `vadsrijk_wezens._assets` the same way and restore from `e906b09c6^` (same recipe).
- The round giant's files (`tmodels/`) stay as dead-ish weight (~450 KB textures); delete in a later cleanup if the user is happy.
- `tools/krimp_manifest.json` still has the hashes of the v2 pngs: krimp will just re-process the restored (smaller) pngs next run.
- No client run (brief): the look must be checked in the dev client (SPIEK_W_MODELLEN.md).

## Tests
`runGameTestServer -Pgt=TModelsGameTests,VadsrijkWezensGameTests,StadslevenGameTests`: **All 14 required tests passed** (compileJava covers the client classes too).
